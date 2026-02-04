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
Nexus ML Service - Main Application

Real-time ML predictions for Production Optimization:
- Failure Prediction (LSTM)
- Anomaly Detection (Isolation Forest)
- Health Scoring

Consumes telemetry from Kafka for real-time processing.
"""
import logging
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import settings
from api import predictions, training, health
from services.kafka_consumer import TelemetryKafkaConsumer
from models.failure_prediction import FailurePredictionModel
from models.anomaly_detection import AnomalyDetectionModel

# Configure logging
logging.basicConfig(
    level=logging.DEBUG if settings.debug else logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Global instances
kafka_consumer: Optional[TelemetryKafkaConsumer] = None
failure_model: Optional[FailurePredictionModel] = None
anomaly_model: Optional[AnomalyDetectionModel] = None


async def load_models():
    """Load ML models into memory."""
    global failure_model, anomaly_model

    logger.info("Loading ML models...")

    # Load failure prediction model
    try:
        failure_model = FailurePredictionModel()
        # TODO: Load pre-trained weights if available
        # failure_model.load(f"{settings.model_storage_path}/failure_prediction/latest")
        logger.info("Failure prediction model loaded")
    except Exception as e:
        logger.warning(f"Could not load failure model: {e}")
        failure_model = FailurePredictionModel()

    # Load anomaly detection model
    try:
        anomaly_model = AnomalyDetectionModel()
        # TODO: Load pre-trained model if available
        # anomaly_model.load(f"{settings.model_storage_path}/anomaly_detection/latest")
        logger.info("Anomaly detection model loaded")
    except Exception as e:
        logger.warning(f"Could not load anomaly model: {e}")
        anomaly_model = AnomalyDetectionModel()


async def start_kafka_consumer():
    """Start the Kafka consumer for real-time telemetry."""
    global kafka_consumer

    if not settings.kafka_enabled:
        logger.info("Kafka consumer disabled")
        return

    try:
        kafka_consumer = TelemetryKafkaConsumer(
            failure_model=failure_model,
            anomaly_model=anomaly_model
        )
        await kafka_consumer.start()
        logger.info(f"Kafka consumer started, listening to {settings.kafka_telemetry_topic}")
    except Exception as e:
        logger.error(f"Failed to start Kafka consumer: {e}")
        logger.warning("Running without real-time Kafka processing")


async def stop_kafka_consumer():
    """Stop the Kafka consumer."""
    global kafka_consumer

    if kafka_consumer:
        await kafka_consumer.stop()
        logger.info("Kafka consumer stopped")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events."""
    logger.info(f"Starting {settings.service_name} v{settings.service_version}")
    logger.info(f"MLflow tracking URI: {settings.mlflow_tracking_uri}")
    logger.info(f"Nexus API URL: {settings.nexus_api_url}")
    logger.info(f"Kafka enabled: {settings.kafka_enabled}")

    # Startup
    await load_models()
    await start_kafka_consumer()

    yield

    # Shutdown
    logger.info("Shutting down ML service")
    await stop_kafka_consumer()


app = FastAPI(
    title="Nexus ML Service",
    description="Machine Learning service for Production Optimization - Failure Prediction, Anomaly Detection, Health Scoring",
    version=settings.service_version,
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(health.router, prefix=settings.api_prefix, tags=["Health"])
app.include_router(predictions.router, prefix=settings.api_prefix, tags=["Predictions"])
app.include_router(training.router, prefix=settings.api_prefix, tags=["Training"])


@app.get("/")
async def root():
    """Root endpoint."""
    kafka_stats = kafka_consumer.get_stats() if kafka_consumer else {"enabled": False}

    return {
        "service": settings.service_name,
        "version": settings.service_version,
        "status": "running",
        "kafka": kafka_stats
    }


@app.get("/api/v1/kafka/stats")
async def kafka_stats():
    """Get Kafka consumer statistics."""
    if not kafka_consumer:
        return {"enabled": False}
    return kafka_consumer.get_stats()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.api_host,
        port=settings.api_port,
        reload=settings.debug
    )
