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
Training Service

Handles ML model training operations.
Uses the training pipeline modules for actual model training.
"""
import logging
from datetime import date, datetime, timedelta
from typing import Dict, List, Optional, Any
from uuid import UUID, uuid4

import httpx

from config import settings
from models.schemas import (
    TrainingJobResponse,
    TrainingJobStatus,
    TrainingProgressUpdate
)
from training.failure_trainer import FailurePredictionTrainer
from training.anomaly_trainer import AnomalyDetectionTrainer
from training.data_loader import TrainingDataLoader

logger = logging.getLogger(__name__)


class TrainingService:
    """Service for training ML models."""

    def __init__(self):
        self._jobs: Dict[UUID, TrainingJobResponse] = {}
        self._cancelled_jobs: set = set()

    async def create_job(
        self,
        tenant_id: UUID,
        model_name: str,
        data_start_date: date,
        data_end_date: date,
        hyperparameters: Optional[Dict[str, Any]] = None
    ) -> TrainingJobResponse:
        """
        Create a new training job.

        Args:
            tenant_id: Tenant ID
            model_name: Name of the model to train
            data_start_date: Start date for training data
            data_end_date: End date for training data
            hyperparameters: Optional hyperparameters override

        Returns:
            TrainingJobResponse with job details
        """
        job_id = uuid4()

        job = TrainingJobResponse(
            job_id=job_id,
            tenant_id=tenant_id,
            model_name=model_name,
            status=TrainingJobStatus.PENDING,
            data_start_date=data_start_date,
            data_end_date=data_end_date,
            hyperparameters=hyperparameters or self._get_default_hyperparameters(model_name),
            progress_percent=0,
            created_time=datetime.now()
        )

        self._jobs[job_id] = job

        # Notify Java backend
        await self._notify_job_created(job)

        logger.info(f"Created training job {job_id} for model {model_name}")
        return job

    async def get_job(self, job_id: UUID) -> Optional[TrainingJobResponse]:
        """Get training job by ID."""
        return self._jobs.get(job_id)

    async def cancel_job(self, job_id: UUID) -> bool:
        """Cancel a training job."""
        job = self._jobs.get(job_id)
        if not job or job.status in [TrainingJobStatus.COMPLETED, TrainingJobStatus.FAILED]:
            return False

        self._cancelled_jobs.add(job_id)
        job.status = TrainingJobStatus.CANCELLED
        job.completed_time = datetime.now()

        # Notify Java backend
        await self._notify_job_cancelled(job_id)

        logger.info(f"Cancelled training job {job_id}")
        return True

    async def run_training(self, job_id: UUID, tenant_id: UUID):
        """
        Run the training job (background task).

        Uses the appropriate trainer based on model_name:
        - failure_prediction: Uses FailurePredictionTrainer (LSTM)
        - anomaly_detection: Uses AnomalyDetectionTrainer (Isolation Forest)
        """
        job = self._jobs.get(job_id)
        if not job:
            logger.error(f"Job {job_id} not found")
            return

        try:
            # Start training
            job.status = TrainingJobStatus.RUNNING
            job.started_time = datetime.now()
            await self._update_job_progress(job_id, 0, "Initializing")

            # Check for cancellation
            if job_id in self._cancelled_jobs:
                return

            # Create progress callback
            async def progress_callback(percent, step, **kwargs):
                if job_id in self._cancelled_jobs:
                    raise InterruptedError("Job cancelled")
                await self._update_job_progress(
                    job_id, percent, step,
                    kwargs.get("current_epoch"),
                    kwargs.get("total_epochs")
                )

            # Select trainer based on model name
            start_date = datetime.combine(job.data_start_date, datetime.min.time())
            end_date = datetime.combine(job.data_end_date, datetime.max.time())

            if job.model_name == "failure_prediction":
                trainer = FailurePredictionTrainer(model_name=job.model_name)
                result = trainer.train(
                    tenant_id=tenant_id,
                    start_date=start_date,
                    end_date=end_date,
                    lift_system_type="ESP",  # TODO: Get from config
                    hyperparameters=job.hyperparameters,
                    progress_callback=progress_callback
                )
            elif job.model_name == "anomaly_detection":
                trainer = AnomalyDetectionTrainer(model_name=job.model_name)
                result = trainer.train(
                    tenant_id=tenant_id,
                    start_date=start_date,
                    end_date=end_date,
                    lift_system_type="ESP",  # TODO: Get from config
                    hyperparameters=job.hyperparameters,
                    progress_callback=progress_callback
                )
            else:
                raise ValueError(f"Unknown model type: {job.model_name}")

            # Update job with results
            metrics = result.get("metrics", {})
            model_id = uuid4()  # TODO: Get from result

            job.status = TrainingJobStatus.COMPLETED
            job.progress_percent = 100
            job.completed_time = datetime.now()
            job.result_model_id = model_id
            job.accuracy = metrics.get("accuracy")
            job.precision = metrics.get("precision")
            job.recall = metrics.get("recall")
            job.f1_score = metrics.get("f1_score")

            await self._notify_job_completed(job_id, model_id)
            logger.info(f"Training job {job_id} completed with model {model_id}")

        except InterruptedError:
            logger.info(f"Training job {job_id} was cancelled")
            job.status = TrainingJobStatus.CANCELLED
            job.completed_time = datetime.now()

        except Exception as e:
            logger.error(f"Training job {job_id} failed: {e}", exc_info=True)
            job.status = TrainingJobStatus.FAILED
            job.completed_time = datetime.now()
            job.error_message = str(e)
            await self._notify_job_failed(job_id, str(e))

    async def trigger_failure_model_training(
        self,
        tenant_id: UUID,
        days_of_data: int = 365
    ) -> TrainingJobResponse:
        """Trigger training for failure prediction model."""
        end_date = date.today()
        start_date = end_date - timedelta(days=days_of_data)

        return await self.create_job(
            tenant_id=tenant_id,
            model_name="failure_prediction",
            data_start_date=start_date,
            data_end_date=end_date,
            hyperparameters={
                "lstm_units": 64,
                "dropout_rate": 0.2,
                "learning_rate": 0.001,
                "epochs": 50,
                "batch_size": 32,
                "early_stop_enabled": True
            }
        )

    async def trigger_anomaly_model_training(
        self,
        tenant_id: UUID,
        days_of_data: int = 90
    ) -> TrainingJobResponse:
        """Trigger training for anomaly detection model."""
        end_date = date.today()
        start_date = end_date - timedelta(days=days_of_data)

        return await self.create_job(
            tenant_id=tenant_id,
            model_name="anomaly_detection",
            data_start_date=start_date,
            data_end_date=end_date,
            hyperparameters={
                "n_estimators": 100,
                "contamination": 0.1,
                "max_samples": "auto"
            }
        )

    def _get_default_hyperparameters(self, model_name: str) -> Dict[str, Any]:
        """Get default hyperparameters for a model type."""
        defaults = {
            "failure_prediction": {
                "lstm_units": 64,
                "dropout_rate": 0.2,
                "learning_rate": 0.001,
                "epochs": 50,
                "batch_size": 32,
                "early_stop_enabled": True
            },
            "anomaly_detection": {
                "n_estimators": 100,
                "contamination": 0.1,
                "max_samples": "auto"
            },
            "health_score": {
                "weights": {
                    "pump_health": 0.3,
                    "motor_health": 0.25,
                    "production_efficiency": 0.25,
                    "maintenance_compliance": 0.2
                }
            }
        }
        return defaults.get(model_name, {})

    async def _update_job_progress(
        self,
        job_id: UUID,
        progress: int,
        step: str,
        epoch: Optional[int] = None,
        total_epochs: Optional[int] = None
    ):
        """Update job progress and notify Java backend."""
        job = self._jobs.get(job_id)
        if job:
            job.progress_percent = progress
            job.current_step = step
            job.current_epoch = epoch
            job.total_epochs = total_epochs

        # Notify Java backend
        try:
            async with httpx.AsyncClient() as client:
                await client.put(
                    f"{settings.nexus_api_url}/api/nexus/po/ml/training/jobs/{job_id}/progress",
                    params={
                        "progressPercent": progress,
                        "currentStep": step,
                        "currentEpoch": epoch,
                        "totalEpochs": total_epochs
                    },
                    timeout=settings.nexus_api_timeout
                )
        except Exception as e:
            logger.warning(f"Failed to update progress: {e}")

    async def _fetch_training_data(
        self,
        tenant_id: UUID,
        start_date: date,
        end_date: date
    ) -> Any:
        """Fetch historical data for training."""
        # TODO: Implement data fetching from ThingsBoard/database
        logger.info(f"Fetching training data from {start_date} to {end_date}")
        return {"mock": "data"}

    async def _prepare_features(
        self,
        data: Any,
        model_name: str
    ) -> tuple:
        """Prepare features for training."""
        # TODO: Implement feature engineering
        logger.info(f"Preparing features for {model_name}")
        return (None, None)

    async def _train_model(
        self,
        features: Any,
        labels: Any,
        model_name: str,
        hyperparameters: Dict[str, Any],
        job_id: UUID
    ) -> tuple:
        """Train the model."""
        # TODO: Implement actual training
        logger.info(f"Training {model_name} model")

        # Simulate training progress
        import asyncio
        epochs = hyperparameters.get("epochs", 10)
        for epoch in range(epochs):
            if job_id in self._cancelled_jobs:
                break
            await self._update_job_progress(
                job_id,
                45 + int(35 * (epoch + 1) / epochs),
                "Training",
                epoch + 1,
                epochs
            )
            await asyncio.sleep(0.1)  # Simulate training time

        # Mock metrics
        metrics = {
            "accuracy": 0.92,
            "precision": 0.89,
            "recall": 0.91,
            "f1_score": 0.90,
            "auc_roc": 0.94
        }

        return (None, metrics)

    async def _save_model(
        self,
        tenant_id: UUID,
        model: Any,
        model_name: str,
        metrics: Dict[str, float]
    ) -> UUID:
        """Save model to MLflow and register with Java backend."""
        # TODO: Implement MLflow integration
        model_id = uuid4()
        logger.info(f"Saved model {model_id} to MLflow")
        return model_id

    async def _notify_job_created(self, job: TrainingJobResponse):
        """Notify Java backend of job creation."""
        pass  # TODO: Implement

    async def _notify_job_cancelled(self, job_id: UUID):
        """Notify Java backend of job cancellation."""
        try:
            async with httpx.AsyncClient() as client:
                await client.post(
                    f"{settings.nexus_api_url}/api/nexus/po/ml/training/jobs/{job_id}/cancel",
                    timeout=settings.nexus_api_timeout
                )
        except Exception as e:
            logger.warning(f"Failed to notify cancellation: {e}")

    async def _notify_job_completed(self, job_id: UUID, model_id: UUID):
        """Notify Java backend of job completion."""
        try:
            async with httpx.AsyncClient() as client:
                await client.post(
                    f"{settings.nexus_api_url}/api/nexus/po/ml/training/jobs/{job_id}/complete",
                    params={"resultModelId": str(model_id)},
                    timeout=settings.nexus_api_timeout
                )
        except Exception as e:
            logger.warning(f"Failed to notify completion: {e}")

    async def _notify_job_failed(self, job_id: UUID, error: str):
        """Notify Java backend of job failure."""
        try:
            async with httpx.AsyncClient() as client:
                await client.post(
                    f"{settings.nexus_api_url}/api/nexus/po/ml/training/jobs/{job_id}/fail",
                    params={"errorMessage": error},
                    timeout=settings.nexus_api_timeout
                )
        except Exception as e:
            logger.warning(f"Failed to notify failure: {e}")
