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
ML Service Configuration
"""
import os
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Application settings."""

    # Service Info
    service_name: str = "nexus-ml-service"
    service_version: str = "1.0.0"
    debug: bool = False

    # API Settings
    api_host: str = "0.0.0.0"
    api_port: int = 8090
    api_prefix: str = "/api/v1"

    # Database
    db_host: str = "localhost"
    db_port: int = 5432
    db_name: str = "nexus"
    db_user: str = "postgres"
    db_password: str = "postgres"

    @property
    def database_url(self) -> str:
        return f"postgresql://{self.db_user}:{self.db_password}@{self.db_host}:{self.db_port}/{self.db_name}"

    # Java Backend API
    nexus_api_url: str = "http://localhost:8080"
    nexus_api_timeout: int = 30

    # ThingsBoard API
    thingsboard_url: str = "http://localhost:8080"
    thingsboard_username: str = "tenant@thingsboard.org"
    thingsboard_password: str = "tenant"

    # Kafka Settings
    kafka_enabled: bool = True
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group: str = "nexus-ml-service"
    kafka_telemetry_topic: str = "tb.rule-engine.telemetry"
    kafka_alarm_topic: str = "tb.rule-engine.alarms"
    kafka_auto_offset_reset: str = "latest"

    # MLflow
    mlflow_tracking_uri: str = "http://localhost:5000"
    mlflow_experiment_name: str = "nexus-po-models"

    # Model Settings
    model_storage_path: str = "/var/lib/nexus/ml-models"
    default_prediction_batch_size: int = 100

    # Training Settings
    max_concurrent_training_jobs: int = 2
    training_timeout_hours: int = 4

    # Feature Engineering
    sequence_length: int = 24  # Hours of historical data for LSTM
    feature_window_hours: int = 168  # 1 week for feature calculation

    # Prediction Thresholds
    failure_probability_threshold: float = 0.7
    anomaly_score_threshold: float = 0.8
    health_critical_threshold: int = 30
    health_poor_threshold: int = 50
    health_fair_threshold: int = 70
    health_good_threshold: int = 85

    class Config:
        env_prefix = "ML_"
        env_file = ".env"
        extra = "ignore"


settings = Settings()
