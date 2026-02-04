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
Training API Endpoints
"""
import logging
from typing import List, Optional
from uuid import UUID

from fastapi import APIRouter, HTTPException, Header, BackgroundTasks
from pydantic import BaseModel, Field

from services.training_service import TrainingService
from models.schemas import (
    TrainingRequest,
    TrainingJobResponse,
    TrainingProgressUpdate
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/training")

# Initialize service
training_service = TrainingService()


@router.post("/start", response_model=TrainingJobResponse)
async def start_training(
    request: TrainingRequest,
    background_tasks: BackgroundTasks,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id")
):
    """
    Start a new model training job.

    Creates a training job and starts it in the background.
    """
    try:
        job = await training_service.create_job(
            tenant_id=x_tenant_id,
            model_name=request.model_name,
            data_start_date=request.data_start_date,
            data_end_date=request.data_end_date,
            hyperparameters=request.hyperparameters
        )

        # Start training in background
        background_tasks.add_task(
            training_service.run_training,
            job.job_id,
            x_tenant_id
        )

        return job
    except Exception as e:
        logger.error(f"Training start error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/jobs/{job_id}", response_model=TrainingJobResponse)
async def get_job_status(job_id: UUID):
    """
    Get status of a training job.
    """
    try:
        job = await training_service.get_job(job_id)
        if job is None:
            raise HTTPException(status_code=404, detail="Job not found")
        return job
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Job status error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/jobs/{job_id}/cancel")
async def cancel_job(job_id: UUID):
    """
    Cancel a running training job.
    """
    try:
        success = await training_service.cancel_job(job_id)
        if not success:
            raise HTTPException(status_code=404, detail="Job not found or already completed")
        return {"message": "Job cancelled"}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Job cancel error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/retrain/failure-prediction")
async def retrain_failure_model(
    background_tasks: BackgroundTasks,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id"),
    days_of_data: int = 365
):
    """
    Trigger retraining of the failure prediction model.

    Uses historical data to train a new version of the model.
    """
    try:
        job = await training_service.trigger_failure_model_training(
            tenant_id=x_tenant_id,
            days_of_data=days_of_data
        )

        background_tasks.add_task(
            training_service.run_training,
            job.job_id,
            x_tenant_id
        )

        return {"job_id": str(job.job_id), "message": "Training started"}
    except Exception as e:
        logger.error(f"Retrain error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/retrain/anomaly-detection")
async def retrain_anomaly_model(
    background_tasks: BackgroundTasks,
    x_tenant_id: UUID = Header(..., alias="X-Tenant-Id"),
    days_of_data: int = 90
):
    """
    Trigger retraining of the anomaly detection model.

    Uses recent data to calibrate the model for current operating conditions.
    """
    try:
        job = await training_service.trigger_anomaly_model_training(
            tenant_id=x_tenant_id,
            days_of_data=days_of_data
        )

        background_tasks.add_task(
            training_service.run_training,
            job.job_id,
            x_tenant_id
        )

        return {"job_id": str(job.job_id), "message": "Training started"}
    except Exception as e:
        logger.error(f"Retrain error: {e}")
        raise HTTPException(status_code=500, detail=str(e))
