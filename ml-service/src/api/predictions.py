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
Prediction API Endpoints
"""
import logging
from typing import List, Optional
from uuid import UUID

from fastapi import APIRouter, HTTPException, Header, BackgroundTasks
from pydantic import BaseModel, Field

from services.prediction_service import PredictionService
from models.schemas import (
    PredictionRequest,
    PredictionResponse,
    BatchPredictionRequest,
    BatchPredictionResponse,
    WellPredictionSummary
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/predictions")

# Initialize service
prediction_service = PredictionService()


@router.post("/failure", response_model=PredictionResponse)
async def predict_failure(
    request: PredictionRequest,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id")
):
    """
    Predict failure probability for a well.

    Uses LSTM-based model to predict:
    - Failure probability (0-1)
    - Estimated days to failure
    - Contributing factors
    """
    try:
        result = await prediction_service.predict_failure(
            tenant_id=x_tenant_id,
            well_asset_id=request.well_asset_id,
            features=request.features
        )
        return result
    except Exception as e:
        logger.error(f"Failure prediction error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/anomaly", response_model=PredictionResponse)
async def detect_anomaly(
    request: PredictionRequest,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id")
):
    """
    Detect anomalies in well telemetry.

    Uses Isolation Forest model to detect:
    - Whether current readings are anomalous
    - Anomaly score
    - Which features are anomalous
    """
    try:
        result = await prediction_service.detect_anomaly(
            tenant_id=x_tenant_id,
            well_asset_id=request.well_asset_id,
            features=request.features
        )
        return result
    except Exception as e:
        logger.error(f"Anomaly detection error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/health-score", response_model=PredictionResponse)
async def calculate_health_score(
    request: PredictionRequest,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id")
):
    """
    Calculate health score for a well.

    Combines multiple metrics to produce:
    - Health score (0-100)
    - Health level (CRITICAL, POOR, FAIR, GOOD, EXCELLENT)
    - Health trend (IMPROVING, STABLE, DEGRADING)
    - Component scores breakdown
    """
    try:
        result = await prediction_service.calculate_health_score(
            tenant_id=x_tenant_id,
            well_asset_id=request.well_asset_id,
            features=request.features
        )
        return result
    except Exception as e:
        logger.error(f"Health score error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/batch", response_model=BatchPredictionResponse)
async def batch_predict(
    request: BatchPredictionRequest,
    background_tasks: BackgroundTasks,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id")
):
    """
    Run predictions for multiple wells.

    Processes predictions in batches and stores results.
    Returns job ID for tracking progress.
    """
    try:
        job_id = await prediction_service.start_batch_prediction(
            tenant_id=x_tenant_id,
            well_ids=request.well_ids,
            prediction_types=request.prediction_types
        )

        # Run in background
        background_tasks.add_task(
            prediction_service.run_batch_prediction,
            job_id,
            x_tenant_id,
            request.well_ids,
            request.prediction_types
        )

        return BatchPredictionResponse(
            job_id=job_id,
            status="started",
            total_wells=len(request.well_ids)
        )
    except Exception as e:
        logger.error(f"Batch prediction error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/batch/{job_id}", response_model=BatchPredictionResponse)
async def get_batch_status(job_id: UUID):
    """
    Get status of a batch prediction job.
    """
    try:
        status = await prediction_service.get_batch_status(job_id)
        if status is None:
            raise HTTPException(status_code=404, detail="Job not found")
        return status
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Batch status error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/summary/{well_asset_id}", response_model=WellPredictionSummary)
async def get_well_summary(
    well_asset_id: UUID,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id")
):
    """
    Get prediction summary for a specific well.

    Combines latest predictions from all models.
    """
    try:
        summary = await prediction_service.get_well_summary(
            tenant_id=x_tenant_id,
            well_asset_id=well_asset_id
        )
        if summary is None:
            raise HTTPException(status_code=404, detail="Well not found")
        return summary
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Well summary error: {e}")
        raise HTTPException(status_code=500, detail=str(e))
