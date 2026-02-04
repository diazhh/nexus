#
# Copyright © 2016-2026 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
Kafka Consumer for Real-Time Telemetry Processing

Consumes telemetry data from ThingsBoard Kafka topics and triggers
ML predictions in real-time.
"""
import asyncio
import json
import logging
from datetime import datetime
from typing import Dict, List, Optional, Any, Set
from uuid import UUID

from aiokafka import AIOKafkaConsumer
from aiokafka.errors import KafkaError

from config import settings
from models.failure_prediction import FailurePredictionModel
from models.anomaly_detection import AnomalyDetectionModel

logger = logging.getLogger(__name__)


class TelemetryKafkaConsumer:
    """
    Kafka consumer for processing well telemetry in real-time.

    Subscribes to ThingsBoard telemetry topics and triggers
    predictions when new data arrives.
    """

    def __init__(
        self,
        failure_model: Optional[FailurePredictionModel] = None,
        anomaly_model: Optional[AnomalyDetectionModel] = None
    ):
        """
        Initialize the Kafka consumer.

        Args:
            failure_model: Pre-loaded failure prediction model
            anomaly_model: Pre-loaded anomaly detection model
        """
        self.consumer: Optional[AIOKafkaConsumer] = None
        self.failure_model = failure_model or FailurePredictionModel()
        self.anomaly_model = anomaly_model or AnomalyDetectionModel()

        self._running = False
        self._processed_count = 0
        self._error_count = 0

        # Buffer for accumulating telemetry per well
        self._telemetry_buffer: Dict[str, List[Dict]] = {}
        self._buffer_size = settings.sequence_length

        # Wells being monitored (from configuration)
        self._monitored_wells: Set[str] = set()

    async def start(self):
        """Start the Kafka consumer."""
        if self._running:
            logger.warning("Consumer already running")
            return

        try:
            self.consumer = AIOKafkaConsumer(
                settings.kafka_telemetry_topic,
                bootstrap_servers=settings.kafka_bootstrap_servers,
                group_id=settings.kafka_consumer_group,
                auto_offset_reset='latest',
                enable_auto_commit=True,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                key_deserializer=lambda k: k.decode('utf-8') if k else None
            )

            await self.consumer.start()
            self._running = True

            logger.info(f"Kafka consumer started, subscribed to {settings.kafka_telemetry_topic}")

            # Start processing loop
            asyncio.create_task(self._consume_loop())

        except Exception as e:
            logger.error(f"Failed to start Kafka consumer: {e}")
            raise

    async def stop(self):
        """Stop the Kafka consumer."""
        self._running = False

        if self.consumer:
            await self.consumer.stop()
            self.consumer = None

        logger.info("Kafka consumer stopped")

    async def _consume_loop(self):
        """Main consumption loop."""
        try:
            async for message in self.consumer:
                if not self._running:
                    break

                try:
                    await self._process_message(message)
                    self._processed_count += 1

                except Exception as e:
                    logger.error(f"Error processing message: {e}")
                    self._error_count += 1

        except KafkaError as e:
            logger.error(f"Kafka error in consume loop: {e}")
        except Exception as e:
            logger.error(f"Unexpected error in consume loop: {e}")
        finally:
            self._running = False

    async def _process_message(self, message):
        """
        Process a single Kafka message.

        ThingsBoard telemetry message format:
        {
            "entityId": "device-uuid",
            "entityType": "DEVICE",
            "tenantId": "tenant-uuid",
            "ts": 1234567890000,
            "data": {
                "temperature": 150.5,
                "pressure": 1200,
                ...
            }
        }
        """
        key = message.key
        value = message.value

        if not value:
            return

        # Extract telemetry data
        entity_id = value.get('entityId')
        tenant_id = value.get('tenantId')
        timestamp = value.get('ts', int(datetime.now().timestamp() * 1000))
        data = value.get('data', {})

        if not entity_id or not data:
            return

        # Check if this is a monitored well
        if self._monitored_wells and entity_id not in self._monitored_wells:
            return

        # Add to buffer
        await self._add_to_buffer(entity_id, tenant_id, timestamp, data)

        # Check if we should run predictions
        if len(self._telemetry_buffer.get(entity_id, [])) >= self._buffer_size:
            await self._run_predictions(entity_id, tenant_id)

    async def _add_to_buffer(
        self,
        entity_id: str,
        tenant_id: str,
        timestamp: int,
        data: Dict[str, Any]
    ):
        """Add telemetry data to the buffer."""
        if entity_id not in self._telemetry_buffer:
            self._telemetry_buffer[entity_id] = []

        self._telemetry_buffer[entity_id].append({
            'tenant_id': tenant_id,
            'timestamp': timestamp,
            'data': data
        })

        # Keep only the most recent data points
        max_buffer = self._buffer_size * 2
        if len(self._telemetry_buffer[entity_id]) > max_buffer:
            self._telemetry_buffer[entity_id] = self._telemetry_buffer[entity_id][-max_buffer:]

    async def _run_predictions(self, entity_id: str, tenant_id: str):
        """
        Run ML predictions for a well.

        Args:
            entity_id: Well/device entity ID
            tenant_id: Tenant ID
        """
        buffer = self._telemetry_buffer.get(entity_id, [])
        if len(buffer) < self._buffer_size:
            return

        # Get the latest data point for current features
        latest = buffer[-1]['data']

        try:
            # Run anomaly detection (fast, run on every update)
            anomaly_result = self.anomaly_model.predict_single(latest)

            if anomaly_result['is_anomaly']:
                logger.warning(f"Anomaly detected for {entity_id}: score={anomaly_result['anomaly_score']:.3f}")
                await self._save_anomaly_prediction(
                    tenant_id=tenant_id,
                    well_asset_id=entity_id,
                    result=anomaly_result
                )

            # Run failure prediction (if anomaly detected or periodically)
            if anomaly_result['is_anomaly'] or self._should_run_failure_prediction(entity_id):
                failure_result = self.failure_model.predict_single(latest)

                if failure_result['probability'] > settings.failure_probability_threshold:
                    logger.warning(
                        f"High failure risk for {entity_id}: "
                        f"probability={failure_result['probability']:.3f}, "
                        f"days_to_failure={failure_result.get('days_to_failure')}"
                    )
                    await self._save_failure_prediction(
                        tenant_id=tenant_id,
                        well_asset_id=entity_id,
                        result=failure_result
                    )

        except Exception as e:
            logger.error(f"Prediction error for {entity_id}: {e}")

    def _should_run_failure_prediction(self, entity_id: str) -> bool:
        """
        Determine if we should run failure prediction.

        Failure prediction is more expensive, so we don't run it on every update.
        """
        # Run every N messages
        return self._processed_count % 10 == 0

    async def _save_anomaly_prediction(
        self,
        tenant_id: str,
        well_asset_id: str,
        result: Dict[str, Any]
    ):
        """Save anomaly prediction to the backend."""
        # TODO: Call Java backend API to save prediction
        prediction_data = {
            'tenantId': tenant_id,
            'wellAssetId': well_asset_id,
            'predictionType': 'ANOMALY',
            'isAnomaly': result['is_anomaly'],
            'anomalyScore': result['anomaly_score'],
            'anomalousFeatures': result.get('anomalous_features', [])
        }

        logger.debug(f"Saving anomaly prediction: {prediction_data}")
        # await self._post_to_backend('/api/nexus/po/ml/predictions/internal', prediction_data)

    async def _save_failure_prediction(
        self,
        tenant_id: str,
        well_asset_id: str,
        result: Dict[str, Any]
    ):
        """Save failure prediction to the backend."""
        # TODO: Call Java backend API to save prediction
        prediction_data = {
            'tenantId': tenant_id,
            'wellAssetId': well_asset_id,
            'predictionType': 'FAILURE',
            'probability': result['probability'],
            'daysToFailure': result.get('days_to_failure'),
            'confidence': result.get('confidence'),
            'contributingFactors': result.get('contributing_factors', [])
        }

        logger.debug(f"Saving failure prediction: {prediction_data}")
        # await self._post_to_backend('/api/nexus/po/ml/predictions/internal', prediction_data)

    def set_monitored_wells(self, well_ids: Set[str]):
        """Set the list of wells to monitor."""
        self._monitored_wells = well_ids
        logger.info(f"Monitoring {len(well_ids)} wells")

    def get_stats(self) -> Dict[str, Any]:
        """Get consumer statistics."""
        return {
            'running': self._running,
            'processed_count': self._processed_count,
            'error_count': self._error_count,
            'buffered_wells': len(self._telemetry_buffer),
            'monitored_wells': len(self._monitored_wells)
        }


class MultiTopicConsumer:
    """
    Consumer that subscribes to multiple Kafka topics for different data types.
    """

    def __init__(self):
        self.consumers: Dict[str, TelemetryKafkaConsumer] = {}

    async def start_all(self):
        """Start all consumers."""
        # Telemetry consumer
        telemetry_consumer = TelemetryKafkaConsumer()
        await telemetry_consumer.start()
        self.consumers['telemetry'] = telemetry_consumer

        # Could add more consumers for different topics:
        # - Alarm events
        # - Maintenance records
        # etc.

    async def stop_all(self):
        """Stop all consumers."""
        for name, consumer in self.consumers.items():
            logger.info(f"Stopping {name} consumer")
            await consumer.stop()

    def get_all_stats(self) -> Dict[str, Dict]:
        """Get stats from all consumers."""
        return {
            name: consumer.get_stats()
            for name, consumer in self.consumers.items()
        }
