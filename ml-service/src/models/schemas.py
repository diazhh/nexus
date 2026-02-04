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
Pydantic Schemas for API Models
"""
from datetime import date, datetime
from enum import Enum
from typing import Dict, List, Optional, Any
from uuid import UUID

from pydantic import BaseModel, Field


# Enums
class PredictionType(str, Enum):
    FAILURE = "FAILURE"
    ANOMALY = "ANOMALY"
    HEALTH_SCORE = "HEALTH_SCORE"


class HealthLevel(str, Enum):
    CRITICAL = "CRITICAL"
    POOR = "POOR"
    FAIR = "FAIR"
    GOOD = "GOOD"
    EXCELLENT = "EXCELLENT"


class HealthTrend(str, Enum):
    IMPROVING = "IMPROVING"
    STABLE = "STABLE"
    DEGRADING = "DEGRADING"


class TrainingJobStatus(str, Enum):
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


# Request Models
class PredictionRequest(BaseModel):
    """Request for a single prediction."""
    well_asset_id: UUID
    features: Optional[Dict[str, float]] = None


class BatchPredictionRequest(BaseModel):
    """Request for batch predictions."""
    well_ids: List[UUID]
    prediction_types: List[PredictionType] = [
        PredictionType.FAILURE,
        PredictionType.ANOMALY,
        PredictionType.HEALTH_SCORE
    ]


class TrainingRequest(BaseModel):
    """Request to start training."""
    model_name: str
    data_start_date: date
    data_end_date: date
    hyperparameters: Optional[Dict[str, Any]] = None


class TrainingProgressUpdate(BaseModel):
    """Training progress update."""
    progress_percent: int = Field(ge=0, le=100)
    current_epoch: Optional[int] = None
    total_epochs: Optional[int] = None
    current_step: Optional[str] = None
    metrics: Optional[Dict[str, float]] = None


# Response Models
class ContributingFactor(BaseModel):
    """A factor contributing to a prediction."""
    feature: str
    current_value: float
    threshold: Optional[float] = None
    impact: str  # HIGH, MEDIUM, LOW
    trend: Optional[str] = None  # UP, DOWN, STABLE
    unit: Optional[str] = None


class AnomalousFeature(BaseModel):
    """An anomalous feature detected."""
    feature: str
    current_value: float
    expected_min: float
    expected_max: float
    deviation_score: float
    unit: Optional[str] = None


class PredictionResponse(BaseModel):
    """Response for a prediction request."""
    prediction_id: UUID
    well_asset_id: UUID
    prediction_type: PredictionType
    prediction_time: datetime

    # Failure prediction fields
    failure_probability: Optional[float] = None
    days_to_failure: Optional[int] = None
    confidence: Optional[float] = None
    contributing_factors: Optional[List[ContributingFactor]] = None

    # Anomaly detection fields
    is_anomaly: Optional[bool] = None
    anomaly_score: Optional[float] = None
    anomalous_features: Optional[List[AnomalousFeature]] = None

    # Health score fields
    health_score: Optional[int] = None
    health_level: Optional[HealthLevel] = None
    health_trend: Optional[HealthTrend] = None
    component_scores: Optional[Dict[str, int]] = None


class BatchPredictionResponse(BaseModel):
    """Response for batch prediction request."""
    job_id: UUID
    status: str
    total_wells: int
    processed_wells: int = 0
    failed_wells: int = 0
    progress_percent: int = 0
    results: Optional[List[PredictionResponse]] = None


class WellPredictionSummary(BaseModel):
    """Summary of all predictions for a well."""
    well_asset_id: UUID
    well_name: Optional[str] = None
    lift_system_type: Optional[str] = None

    # Health
    health_score: Optional[int] = None
    health_level: Optional[HealthLevel] = None
    health_trend: Optional[HealthTrend] = None

    # Failure
    failure_probability: Optional[float] = None
    days_to_failure: Optional[int] = None

    # Anomaly
    has_anomaly: Optional[bool] = None

    # Primary issue
    primary_issue: Optional[str] = None

    last_updated: Optional[datetime] = None


class TrainingJobResponse(BaseModel):
    """Response for training job."""
    job_id: UUID
    tenant_id: UUID
    model_name: str
    status: TrainingJobStatus

    # Configuration
    data_start_date: date
    data_end_date: date
    hyperparameters: Optional[Dict[str, Any]] = None

    # Progress
    progress_percent: int = 0
    current_epoch: Optional[int] = None
    total_epochs: Optional[int] = None
    current_step: Optional[str] = None

    # Results
    result_model_id: Optional[UUID] = None
    error_message: Optional[str] = None

    # Metrics (for completed training)
    accuracy: Optional[float] = None
    precision: Optional[float] = None
    recall: Optional[float] = None
    f1_score: Optional[float] = None

    # Timestamps
    started_time: Optional[datetime] = None
    completed_time: Optional[datetime] = None
    created_time: datetime


class ModelInfo(BaseModel):
    """Information about a trained model."""
    model_id: UUID
    name: str
    version: str
    model_type: str
    status: str

    # Metrics
    accuracy: Optional[float] = None
    precision: Optional[float] = None
    recall: Optional[float] = None
    f1_score: Optional[float] = None
    auc_roc: Optional[float] = None

    # Feature importance
    feature_importance: Optional[Dict[str, float]] = None

    # Training info
    training_samples: Optional[int] = None
    training_time_seconds: Optional[int] = None

    created_time: datetime
