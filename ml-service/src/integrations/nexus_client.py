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
Nexus Java Backend API Client

Provides integration with the Nexus Java backend for:
- Saving ML predictions
- Getting ML configuration
- Managing training jobs
"""
import logging
from typing import Dict, List, Optional, Any
from uuid import UUID
from datetime import datetime

import httpx

from config import settings

logger = logging.getLogger(__name__)


class NexusApiClient:
    """Client for Nexus Java Backend REST API."""

    def __init__(self, base_url: str = None, timeout: int = None):
        self.base_url = base_url or settings.nexus_api_url
        self.timeout = timeout or settings.nexus_api_timeout

    def _headers(self, tenant_id: UUID) -> Dict[str, str]:
        """Get request headers."""
        return {
            "Content-Type": "application/json",
            "X-Tenant-Id": str(tenant_id)
        }

    async def get_ml_config(self, tenant_id: UUID) -> Optional[Dict]:
        """Get ML configuration for a tenant."""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/api/nexus/po/ml/config",
                    headers=self._headers(tenant_id)
                )
                if response.status_code == 404:
                    return None
                response.raise_for_status()
                return response.json()
        except Exception as e:
            logger.error(f"Failed to get ML config: {e}")
            return None

    async def save_ml_config(
        self,
        tenant_id: UUID,
        config: Dict
    ) -> Optional[Dict]:
        """Save ML configuration."""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/api/nexus/po/ml/config",
                    headers=self._headers(tenant_id),
                    json=config
                )
                response.raise_for_status()
                return response.json()
        except Exception as e:
            logger.error(f"Failed to save ML config: {e}")
            return None

    async def save_prediction(
        self,
        tenant_id: UUID,
        prediction: Dict
    ) -> Optional[Dict]:
        """
        Save a prediction result to the database.

        Args:
            tenant_id: Tenant ID
            prediction: Prediction data including:
                - wellAssetId: Well asset ID
                - predictionType: FAILURE, ANOMALY, or HEALTH_SCORE
                - probability: Failure probability (0-1)
                - daysToFailure: Estimated days to failure
                - isAnomaly: Whether anomaly detected
                - anomalyScore: Anomaly score
                - healthScore: Health score (0-100)
                - healthLevel: CRITICAL, POOR, FAIR, GOOD, EXCELLENT
                - contributingFactors: List of factors
                - anomalousFeatures: List of anomalous features

        Returns:
            Saved prediction object
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/api/nexus/po/ml/predictions",
                    headers=self._headers(tenant_id),
                    json=prediction
                )
                response.raise_for_status()
                return response.json()
        except Exception as e:
            logger.error(f"Failed to save prediction: {e}")
            return None

    async def get_predictions(
        self,
        tenant_id: UUID,
        well_asset_id: UUID = None,
        prediction_type: str = None,
        limit: int = 100
    ) -> List[Dict]:
        """Get predictions from the database."""
        try:
            params = {"limit": limit}
            if well_asset_id:
                params["wellAssetId"] = str(well_asset_id)
            if prediction_type:
                params["predictionType"] = prediction_type

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/api/nexus/po/ml/predictions",
                    headers=self._headers(tenant_id),
                    params=params
                )
                response.raise_for_status()
                return response.json()
        except Exception as e:
            logger.error(f"Failed to get predictions: {e}")
            return []

    async def get_high_risk_wells(
        self,
        tenant_id: UUID,
        threshold: float = 0.7
    ) -> List[Dict]:
        """Get wells with high failure probability."""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/api/nexus/po/ml/predictions/high-risk",
                    headers=self._headers(tenant_id),
                    params={"threshold": threshold}
                )
                response.raise_for_status()
                return response.json()
        except Exception as e:
            logger.error(f"Failed to get high risk wells: {e}")
            return []

    async def create_training_job(
        self,
        tenant_id: UUID,
        model_name: str,
        data_start_date: str,
        data_end_date: str,
        hyperparameters: Dict = None
    ) -> Optional[Dict]:
        """Create a new training job."""
        try:
            job_data = {
                "modelName": model_name,
                "dataStartDate": data_start_date,
                "dataEndDate": data_end_date,
                "hyperparameters": hyperparameters or {}
            }

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/api/nexus/po/ml/training/jobs",
                    headers=self._headers(tenant_id),
                    json=job_data
                )
                response.raise_for_status()
                return response.json()
        except Exception as e:
            logger.error(f"Failed to create training job: {e}")
            return None

    async def update_training_progress(
        self,
        tenant_id: UUID,
        job_id: UUID,
        progress_percent: int,
        current_step: str = None,
        current_epoch: int = None,
        total_epochs: int = None
    ) -> bool:
        """Update training job progress."""
        try:
            update_data = {
                "progressPercent": progress_percent
            }
            if current_step:
                update_data["currentStep"] = current_step
            if current_epoch is not None:
                update_data["currentEpoch"] = current_epoch
            if total_epochs is not None:
                update_data["totalEpochs"] = total_epochs

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.put(
                    f"{self.base_url}/api/nexus/po/ml/training/jobs/{job_id}/progress",
                    headers=self._headers(tenant_id),
                    json=update_data
                )
                response.raise_for_status()
                return True
        except Exception as e:
            logger.error(f"Failed to update training progress: {e}")
            return False

    async def complete_training_job(
        self,
        tenant_id: UUID,
        job_id: UUID,
        model_id: UUID,
        metrics: Dict = None
    ) -> bool:
        """Mark training job as completed."""
        try:
            complete_data = {
                "resultModelId": str(model_id),
                "metrics": metrics or {}
            }

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.put(
                    f"{self.base_url}/api/nexus/po/ml/training/jobs/{job_id}/complete",
                    headers=self._headers(tenant_id),
                    json=complete_data
                )
                response.raise_for_status()
                return True
        except Exception as e:
            logger.error(f"Failed to complete training job: {e}")
            return False

    async def fail_training_job(
        self,
        tenant_id: UUID,
        job_id: UUID,
        error_message: str
    ) -> bool:
        """Mark training job as failed."""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.put(
                    f"{self.base_url}/api/nexus/po/ml/training/jobs/{job_id}/fail",
                    headers=self._headers(tenant_id),
                    json={"errorMessage": error_message}
                )
                response.raise_for_status()
                return True
        except Exception as e:
            logger.error(f"Failed to fail training job: {e}")
            return False

    async def save_model(
        self,
        tenant_id: UUID,
        model_data: Dict
    ) -> Optional[Dict]:
        """
        Save a trained model to the registry.

        Args:
            tenant_id: Tenant ID
            model_data: Model information including:
                - name: Model name
                - modelType: FAILURE_PREDICTION, ANOMALY_DETECTION, HEALTH_SCORE
                - liftSystemType: ESP, PCP, ROD_PUMP, GAS_LIFT
                - version: Version string
                - accuracy, precision, recall, f1Score, aucRoc: Metrics
                - hyperparameters: Training hyperparameters
                - featureImportance: Feature importance map
                - modelPath: Path to model files
                - mlflowRunId: MLflow run ID

        Returns:
            Saved model object
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/api/nexus/po/ml/models",
                    headers=self._headers(tenant_id),
                    json=model_data
                )
                response.raise_for_status()
                return response.json()
        except Exception as e:
            logger.error(f"Failed to save model: {e}")
            return None

    async def get_active_model(
        self,
        tenant_id: UUID,
        model_type: str,
        lift_system_type: str = None
    ) -> Optional[Dict]:
        """Get the active model for a type."""
        try:
            params = {"modelType": model_type}
            if lift_system_type:
                params["liftSystemType"] = lift_system_type

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.get(
                    f"{self.base_url}/api/nexus/po/ml/models/active",
                    headers=self._headers(tenant_id),
                    params=params
                )
                if response.status_code == 404:
                    return None
                response.raise_for_status()
                return response.json()
        except Exception as e:
            logger.error(f"Failed to get active model: {e}")
            return None

    async def health_check(self) -> bool:
        """Check if the Nexus API is available."""
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{self.base_url}/api/health")
                return response.status_code == 200
        except Exception:
            return False
