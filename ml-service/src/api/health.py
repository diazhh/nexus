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
Health Check Endpoints
"""
import logging
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Dict, Any, Optional
import httpx

from config import settings
from data.database import check_database_connection, verify_ml_tables

logger = logging.getLogger(__name__)
router = APIRouter()


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    service: str
    version: str
    components: Dict[str, Any]


async def check_mlflow_connection() -> dict:
    """Check MLflow tracking server connectivity."""
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(f"{settings.mlflow_tracking_uri}/health")
            if response.status_code == 200:
                return {
                    "status": "healthy",
                    "uri": settings.mlflow_tracking_uri
                }
            else:
                return {
                    "status": "degraded",
                    "uri": settings.mlflow_tracking_uri,
                    "http_status": response.status_code
                }
    except httpx.ConnectError:
        return {
            "status": "unhealthy",
            "uri": settings.mlflow_tracking_uri,
            "error": "Connection refused"
        }
    except Exception as e:
        logger.warning(f"MLflow health check failed: {e}")
        return {
            "status": "unknown",
            "uri": settings.mlflow_tracking_uri,
            "error": str(e)
        }


def get_model_status() -> dict:
    """Get status of loaded ML models."""
    # Import here to avoid circular dependency
    from main import failure_model, anomaly_model

    models_status = {
        "failure_prediction": {
            "status": "loaded" if failure_model and failure_model.model else "not_loaded",
            "trained": failure_model.is_trained if failure_model else False
        },
        "anomaly_detection": {
            "status": "loaded" if anomaly_model and anomaly_model.model else "not_loaded",
            "trained": anomaly_model.is_trained if anomaly_model else False
        },
        "health_score": {
            "status": "not_implemented"
        }
    }

    return models_status


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Check service health.
    Returns status of all components including database, MLflow, and models.
    """
    # Check database
    db_status = check_database_connection()

    # Check MLflow
    mlflow_status = await check_mlflow_connection()

    # Check models
    try:
        models_status = get_model_status()
    except Exception as e:
        logger.warning(f"Could not get model status: {e}")
        models_status = {
            "failure_prediction": {"status": "unknown"},
            "anomaly_detection": {"status": "unknown"},
            "health_score": {"status": "unknown"}
        }

    # Check ML tables
    tables_status = verify_ml_tables()

    components = {
        "api": {"status": "healthy"},
        "database": db_status,
        "mlflow": mlflow_status,
        "ml_tables": tables_status,
        "models": models_status
    }

    # Determine overall status
    critical_components = [db_status.get("status")]
    if any(s == "unhealthy" for s in critical_components):
        overall_status = "unhealthy"
    elif any(s == "degraded" for s in critical_components):
        overall_status = "degraded"
    else:
        overall_status = "healthy"

    return HealthResponse(
        status=overall_status,
        service=settings.service_name,
        version=settings.service_version,
        components=components
    )


@router.get("/health/ready")
async def readiness_check():
    """
    Kubernetes readiness probe.
    Returns 200 if service is ready to accept traffic.

    Service is ready when:
    - Database is connected
    - API is responding
    """
    db_status = check_database_connection()

    if db_status.get("status") == "healthy":
        return {"ready": True, "database": "connected"}

    # Return 503 if not ready
    from fastapi import HTTPException
    raise HTTPException(
        status_code=503,
        detail={
            "ready": False,
            "database": db_status.get("status"),
            "error": db_status.get("error")
        }
    )


@router.get("/health/live")
async def liveness_check():
    """
    Kubernetes liveness probe.
    Returns 200 if service is alive.

    Simple check - if we can respond, we're alive.
    """
    return {"alive": True}


@router.get("/health/detailed")
async def detailed_health():
    """
    Detailed health check with all component information.
    """
    health = await health_check()

    # Add additional details
    from main import kafka_consumer

    kafka_status = {
        "enabled": settings.kafka_enabled,
        "bootstrap_servers": settings.kafka_bootstrap_servers,
        "topic": settings.kafka_telemetry_topic,
        "stats": kafka_consumer.get_stats() if kafka_consumer else None
    }

    return {
        **health.model_dump(),
        "kafka": kafka_status,
        "configuration": {
            "api_host": settings.api_host,
            "api_port": settings.api_port,
            "debug": settings.debug,
            "db_host": settings.db_host,
            "db_name": settings.db_name
        }
    }
