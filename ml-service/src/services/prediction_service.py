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
Prediction Service

Handles all prediction operations including failure prediction,
anomaly detection, and health score calculation.
"""
import logging
from datetime import datetime
from typing import Dict, List, Optional, Any
from uuid import UUID, uuid4

import numpy as np

from config import settings
from models.schemas import (
    PredictionResponse,
    BatchPredictionResponse,
    WellPredictionSummary,
    PredictionType,
    HealthLevel,
    HealthTrend,
    ContributingFactor,
    AnomalousFeature
)

logger = logging.getLogger(__name__)


class PredictionService:
    """Service for making ML predictions."""

    def __init__(self):
        self.failure_model = None
        self.anomaly_model = None
        self._batch_jobs: Dict[UUID, BatchPredictionResponse] = {}

    async def predict_failure(
        self,
        tenant_id: UUID,
        well_asset_id: UUID,
        features: Optional[Dict[str, float]] = None
    ) -> PredictionResponse:
        """
        Predict failure probability for a well.

        Args:
            tenant_id: Tenant ID
            well_asset_id: Well asset ID
            features: Optional pre-computed features

        Returns:
            PredictionResponse with failure prediction
        """
        logger.info(f"Predicting failure for well {well_asset_id}")

        # TODO: Fetch telemetry data if features not provided
        if features is None:
            features = await self._fetch_well_features(tenant_id, well_asset_id)

        # TODO: Load and use actual model
        # For now, return mock prediction
        probability = np.random.uniform(0.1, 0.9)
        days_to_failure = int(30 * (1 - probability)) if probability > 0.5 else None

        contributing_factors = []
        if probability > 0.5:
            contributing_factors = [
                ContributingFactor(
                    feature="pump_temperature",
                    current_value=185.5,
                    threshold=180.0,
                    impact="HIGH",
                    trend="UP",
                    unit="F"
                ),
                ContributingFactor(
                    feature="vibration_level",
                    current_value=2.8,
                    threshold=2.5,
                    impact="MEDIUM",
                    trend="UP",
                    unit="mm/s"
                )
            ]

        prediction_id = uuid4()

        # TODO: Save prediction to database via Java API
        await self._save_prediction(
            tenant_id=tenant_id,
            prediction_id=prediction_id,
            well_asset_id=well_asset_id,
            prediction_type=PredictionType.FAILURE,
            probability=probability,
            days_to_failure=days_to_failure,
            contributing_factors=contributing_factors
        )

        return PredictionResponse(
            prediction_id=prediction_id,
            well_asset_id=well_asset_id,
            prediction_type=PredictionType.FAILURE,
            prediction_time=datetime.now(),
            failure_probability=round(probability, 4),
            days_to_failure=days_to_failure,
            confidence=round(np.random.uniform(0.7, 0.95), 4),
            contributing_factors=contributing_factors
        )

    async def detect_anomaly(
        self,
        tenant_id: UUID,
        well_asset_id: UUID,
        features: Optional[Dict[str, float]] = None
    ) -> PredictionResponse:
        """
        Detect anomalies in well telemetry.

        Args:
            tenant_id: Tenant ID
            well_asset_id: Well asset ID
            features: Optional pre-computed features

        Returns:
            PredictionResponse with anomaly detection result
        """
        logger.info(f"Detecting anomaly for well {well_asset_id}")

        # TODO: Fetch telemetry data if features not provided
        if features is None:
            features = await self._fetch_well_features(tenant_id, well_asset_id)

        # TODO: Load and use actual Isolation Forest model
        # For now, return mock prediction
        anomaly_score = np.random.uniform(-0.5, 0.5)
        is_anomaly = anomaly_score > settings.anomaly_score_threshold - 1.0

        anomalous_features = []
        if is_anomaly:
            anomalous_features = [
                AnomalousFeature(
                    feature="casing_pressure",
                    current_value=1250.0,
                    expected_min=800.0,
                    expected_max=1100.0,
                    deviation_score=0.85,
                    unit="psi"
                )
            ]

        prediction_id = uuid4()

        return PredictionResponse(
            prediction_id=prediction_id,
            well_asset_id=well_asset_id,
            prediction_type=PredictionType.ANOMALY,
            prediction_time=datetime.now(),
            is_anomaly=is_anomaly,
            anomaly_score=round(anomaly_score, 4),
            anomalous_features=anomalous_features
        )

    async def calculate_health_score(
        self,
        tenant_id: UUID,
        well_asset_id: UUID,
        features: Optional[Dict[str, float]] = None
    ) -> PredictionResponse:
        """
        Calculate health score for a well.

        Args:
            tenant_id: Tenant ID
            well_asset_id: Well asset ID
            features: Optional pre-computed features

        Returns:
            PredictionResponse with health score
        """
        logger.info(f"Calculating health score for well {well_asset_id}")

        # TODO: Implement actual health score calculation
        # For now, return mock score
        health_score = np.random.randint(20, 95)

        # Determine health level
        if health_score < settings.health_critical_threshold:
            health_level = HealthLevel.CRITICAL
        elif health_score < settings.health_poor_threshold:
            health_level = HealthLevel.POOR
        elif health_score < settings.health_fair_threshold:
            health_level = HealthLevel.FAIR
        elif health_score < settings.health_good_threshold:
            health_level = HealthLevel.GOOD
        else:
            health_level = HealthLevel.EXCELLENT

        # Mock trend
        trends = [HealthTrend.IMPROVING, HealthTrend.STABLE, HealthTrend.DEGRADING]
        health_trend = trends[np.random.randint(0, 3)]

        # Component scores
        component_scores = {
            "pump": np.random.randint(30, 100),
            "motor": np.random.randint(40, 100),
            "tubing": np.random.randint(50, 100),
            "casing": np.random.randint(60, 100)
        }

        prediction_id = uuid4()

        return PredictionResponse(
            prediction_id=prediction_id,
            well_asset_id=well_asset_id,
            prediction_type=PredictionType.HEALTH_SCORE,
            prediction_time=datetime.now(),
            health_score=health_score,
            health_level=health_level,
            health_trend=health_trend,
            component_scores=component_scores
        )

    async def start_batch_prediction(
        self,
        tenant_id: UUID,
        well_ids: List[UUID],
        prediction_types: List[PredictionType]
    ) -> UUID:
        """Start a batch prediction job."""
        job_id = uuid4()
        self._batch_jobs[job_id] = BatchPredictionResponse(
            job_id=job_id,
            status="started",
            total_wells=len(well_ids),
            processed_wells=0,
            failed_wells=0,
            progress_percent=0
        )
        return job_id

    async def run_batch_prediction(
        self,
        job_id: UUID,
        tenant_id: UUID,
        well_ids: List[UUID],
        prediction_types: List[PredictionType]
    ):
        """Run batch predictions (background task)."""
        job = self._batch_jobs.get(job_id)
        if not job:
            return

        job.status = "running"
        results = []

        for i, well_id in enumerate(well_ids):
            try:
                for pred_type in prediction_types:
                    if pred_type == PredictionType.FAILURE:
                        result = await self.predict_failure(tenant_id, well_id)
                    elif pred_type == PredictionType.ANOMALY:
                        result = await self.detect_anomaly(tenant_id, well_id)
                    else:
                        result = await self.calculate_health_score(tenant_id, well_id)
                    results.append(result)

                job.processed_wells += 1
            except Exception as e:
                logger.error(f"Batch prediction failed for well {well_id}: {e}")
                job.failed_wells += 1

            job.progress_percent = int((i + 1) / len(well_ids) * 100)

        job.status = "completed"
        job.results = results

    async def get_batch_status(self, job_id: UUID) -> Optional[BatchPredictionResponse]:
        """Get status of a batch prediction job."""
        return self._batch_jobs.get(job_id)

    async def get_well_summary(
        self,
        tenant_id: UUID,
        well_asset_id: UUID
    ) -> Optional[WellPredictionSummary]:
        """Get prediction summary for a well."""
        # TODO: Fetch from database
        return WellPredictionSummary(
            well_asset_id=well_asset_id,
            well_name="Mock Well",
            lift_system_type="ESP",
            health_score=75,
            health_level=HealthLevel.GOOD,
            health_trend=HealthTrend.STABLE,
            failure_probability=0.35,
            days_to_failure=None,
            has_anomaly=False,
            primary_issue=None,
            last_updated=datetime.now()
        )

    async def _fetch_well_features(
        self,
        tenant_id: UUID,
        well_asset_id: UUID
    ) -> Dict[str, float]:
        """Fetch features for a well from telemetry data."""
        # TODO: Implement actual data fetching from ThingsBoard
        return {}

    async def _save_prediction(
        self,
        tenant_id: UUID,
        prediction_id: UUID,
        well_asset_id: UUID,
        prediction_type: PredictionType,
        **kwargs
    ):
        """Save prediction to database via Java API."""
        # TODO: Implement API call to Java backend
        pass
