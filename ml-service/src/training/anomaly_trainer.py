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
Anomaly Detection Model Trainer

Trains Isolation Forest models for detecting anomalies in well telemetry.
Uses MLflow for experiment tracking and model registry.
"""
import logging
import os
from datetime import datetime
from typing import Dict, List, Optional, Tuple, Any
from uuid import UUID
import json

import numpy as np
import pandas as pd
import mlflow
import mlflow.sklearn
import joblib
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split

from config import settings
from training.data_loader import TrainingDataLoader

logger = logging.getLogger(__name__)


class AnomalyDetectionTrainer:
    """
    Trains Isolation Forest models for anomaly detection.

    Features:
    - Configurable contamination rate
    - MLflow experiment tracking
    - Feature statistics calculation
    - Threshold tuning
    """

    def __init__(
        self,
        experiment_name: str = None,
        model_name: str = "anomaly_detection"
    ):
        self.experiment_name = experiment_name or settings.mlflow_experiment_name
        self.model_name = model_name
        self.data_loader = TrainingDataLoader(use_api=False)

        # Setup MLflow
        mlflow.set_tracking_uri(settings.mlflow_tracking_uri)
        mlflow.set_experiment(self.experiment_name)

        self.model: Optional[IsolationForest] = None
        self.scaler: Optional[StandardScaler] = None
        self.feature_columns: List[str] = []
        self.feature_stats: Dict[str, Dict] = {}
        self.thresholds: Dict[str, float] = {}

    def train(
        self,
        tenant_id: UUID,
        start_date: datetime,
        end_date: datetime,
        lift_system_type: str = "ESP",
        hyperparameters: Dict = None,
        progress_callback: callable = None
    ) -> Dict[str, Any]:
        """
        Train an anomaly detection model.

        Args:
            tenant_id: Tenant ID
            start_date: Training data start date
            end_date: Training data end date
            lift_system_type: Lift system type
            hyperparameters: Training hyperparameters
            progress_callback: Callback for progress updates

        Returns:
            Dictionary with training results
        """
        # Default hyperparameters
        hp = {
            "n_estimators": 100,
            "max_samples": "auto",
            "contamination": 0.05,  # 5% expected anomalies
            "max_features": 1.0,
            "bootstrap": False,
            "n_jobs": -1,
            "random_state": 42
        }
        if hyperparameters:
            hp.update(hyperparameters)

        run_id = None

        with mlflow.start_run(run_name=f"{self.model_name}_{lift_system_type}_{datetime.now().strftime('%Y%m%d_%H%M%S')}") as run:
            run_id = run.info.run_id

            try:
                # Log parameters
                mlflow.log_params(hp)
                mlflow.log_param("lift_system_type", lift_system_type)
                mlflow.log_param("tenant_id", str(tenant_id))

                # Step 1: Load data
                if progress_callback:
                    progress_callback(10, "Loading training data")

                # Use synthetic data for now
                features_df, _ = self.data_loader.create_synthetic_data(
                    num_wells=20,
                    days=90,
                    samples_per_day=96,
                    failure_rate=0.0  # Use normal operation data
                )

                mlflow.log_metric("total_samples", len(features_df))
                mlflow.log_metric("num_wells", features_df["well_id"].nunique())

                # Step 2: Prepare features
                if progress_callback:
                    progress_callback(20, "Preparing features")

                self.feature_columns = [
                    "pump_intake_pressure_psi",
                    "pump_discharge_pressure_psi",
                    "motor_current_amps",
                    "motor_temperature_f",
                    "vibration_in_s",
                    "flow_rate_bpd",
                    "frequency_hz",
                    "gas_oil_ratio",
                    "water_cut_percent"
                ]

                X = features_df[self.feature_columns].values

                # Handle missing values
                X = np.nan_to_num(X, nan=0.0)

                # Step 3: Scale features
                if progress_callback:
                    progress_callback(30, "Scaling features")

                self.scaler = StandardScaler()
                X_scaled = self.scaler.fit_transform(X)

                # Calculate feature statistics
                self._calculate_feature_stats(features_df)

                # Step 4: Split data
                X_train, X_test = train_test_split(
                    X_scaled, test_size=0.2, random_state=42
                )

                mlflow.log_metric("train_samples", len(X_train))
                mlflow.log_metric("test_samples", len(X_test))

                # Step 5: Train model
                if progress_callback:
                    progress_callback(50, "Training Isolation Forest")

                self.model = IsolationForest(
                    n_estimators=hp["n_estimators"],
                    max_samples=hp["max_samples"],
                    contamination=hp["contamination"],
                    max_features=hp["max_features"],
                    bootstrap=hp["bootstrap"],
                    n_jobs=hp["n_jobs"],
                    random_state=hp["random_state"]
                )

                self.model.fit(X_train)

                # Step 6: Evaluate
                if progress_callback:
                    progress_callback(70, "Evaluating model")

                # Get anomaly scores
                train_scores = self.model.decision_function(X_train)
                test_scores = self.model.decision_function(X_test)

                # Predictions (-1 = anomaly, 1 = normal)
                train_pred = self.model.predict(X_train)
                test_pred = self.model.predict(X_test)

                # Calculate metrics
                train_anomaly_rate = (train_pred == -1).mean()
                test_anomaly_rate = (test_pred == -1).mean()

                metrics = {
                    "train_anomaly_rate": float(train_anomaly_rate),
                    "test_anomaly_rate": float(test_anomaly_rate),
                    "score_mean": float(test_scores.mean()),
                    "score_std": float(test_scores.std()),
                    "score_min": float(test_scores.min()),
                    "score_max": float(test_scores.max()),
                    "threshold": float(self.model.offset_)
                }

                for name, value in metrics.items():
                    mlflow.log_metric(name, value)

                # Step 7: Calculate per-feature thresholds
                if progress_callback:
                    progress_callback(80, "Calculating feature thresholds")

                self._calculate_feature_thresholds(features_df)
                mlflow.log_dict(self.thresholds, "feature_thresholds.json")
                mlflow.log_dict(self.feature_stats, "feature_stats.json")

                # Step 8: Save model
                if progress_callback:
                    progress_callback(90, "Saving model")

                # Save to MLflow
                mlflow.sklearn.log_model(
                    self.model,
                    "model",
                    registered_model_name=f"{self.model_name}_{lift_system_type}"
                )

                # Save locally
                model_dir = os.path.join(
                    settings.model_storage_path,
                    self.model_name,
                    lift_system_type,
                    datetime.now().strftime("%Y%m%d_%H%M%S")
                )
                os.makedirs(model_dir, exist_ok=True)

                # Save model and scaler
                joblib.dump(self.model, os.path.join(model_dir, "model.joblib"))
                joblib.dump(self.scaler, os.path.join(model_dir, "scaler.joblib"))

                # Save metadata
                metadata = {
                    "feature_columns": self.feature_columns,
                    "feature_stats": self.feature_stats,
                    "thresholds": self.thresholds,
                    "hyperparameters": hp
                }
                with open(os.path.join(model_dir, "metadata.json"), "w") as f:
                    json.dump(metadata, f, indent=2)

                if progress_callback:
                    progress_callback(100, "Training complete")

                return {
                    "status": "success",
                    "run_id": run_id,
                    "model_path": model_dir,
                    "metrics": metrics,
                    "feature_stats": self.feature_stats,
                    "thresholds": self.thresholds,
                    "training_samples": len(X_train),
                    "test_samples": len(X_test)
                }

            except Exception as e:
                logger.error(f"Training failed: {e}")
                mlflow.log_param("error", str(e))
                raise

    def _calculate_feature_stats(self, df: pd.DataFrame):
        """Calculate feature statistics for anomaly detection."""
        self.feature_stats = {}

        for col in self.feature_columns:
            if col in df.columns:
                values = df[col].dropna()
                self.feature_stats[col] = {
                    "mean": float(values.mean()),
                    "std": float(values.std()),
                    "min": float(values.min()),
                    "max": float(values.max()),
                    "p05": float(values.quantile(0.05)),
                    "p25": float(values.quantile(0.25)),
                    "p50": float(values.quantile(0.50)),
                    "p75": float(values.quantile(0.75)),
                    "p95": float(values.quantile(0.95)),
                    "iqr": float(values.quantile(0.75) - values.quantile(0.25))
                }

    def _calculate_feature_thresholds(self, df: pd.DataFrame):
        """
        Calculate per-feature anomaly thresholds.

        Uses IQR method: anomaly if value < Q1 - 1.5*IQR or > Q3 + 1.5*IQR
        """
        self.thresholds = {}

        for col in self.feature_columns:
            if col in self.feature_stats:
                stats = self.feature_stats[col]
                iqr = stats["iqr"]
                q1 = stats["p25"]
                q3 = stats["p75"]

                self.thresholds[col] = {
                    "lower": float(q1 - 1.5 * iqr),
                    "upper": float(q3 + 1.5 * iqr),
                    "lower_extreme": float(q1 - 3.0 * iqr),
                    "upper_extreme": float(q3 + 3.0 * iqr)
                }

    def load_model(self, model_path: str = None, run_id: str = None):
        """Load a trained model."""
        if run_id:
            # Load from MLflow
            model_uri = f"runs:/{run_id}/model"
            self.model = mlflow.sklearn.load_model(model_uri)
        elif model_path:
            # Load from local path
            self.model = joblib.load(os.path.join(model_path, "model.joblib"))
            self.scaler = joblib.load(os.path.join(model_path, "scaler.joblib"))

            # Load metadata
            metadata_path = os.path.join(model_path, "metadata.json")
            if os.path.exists(metadata_path):
                with open(metadata_path, "r") as f:
                    metadata = json.load(f)
                    self.feature_columns = metadata.get("feature_columns", [])
                    self.feature_stats = metadata.get("feature_stats", {})
                    self.thresholds = metadata.get("thresholds", {})
        else:
            raise ValueError("Either model_path or run_id must be provided")

        logger.info("Model loaded successfully")

    def predict(self, features: Dict[str, float]) -> Dict[str, Any]:
        """
        Predict if features are anomalous.

        Args:
            features: Dictionary of feature values

        Returns:
            Dictionary with anomaly detection results
        """
        if self.model is None:
            raise RuntimeError("Model not loaded")

        # Prepare features
        X = np.array([[features.get(col, 0) for col in self.feature_columns]])

        # Scale
        if self.scaler:
            X_scaled = self.scaler.transform(X)
        else:
            X_scaled = X

        # Predict
        anomaly_score = self.model.decision_function(X_scaled)[0]
        prediction = self.model.predict(X_scaled)[0]
        is_anomaly = prediction == -1

        # Check individual feature thresholds
        anomalous_features = []
        for col in self.feature_columns:
            value = features.get(col)
            if value is not None and col in self.thresholds:
                thresh = self.thresholds[col]
                if value < thresh["lower"] or value > thresh["upper"]:
                    stats = self.feature_stats.get(col, {})
                    anomalous_features.append({
                        "feature": col,
                        "value": value,
                        "expected_min": thresh["lower"],
                        "expected_max": thresh["upper"],
                        "deviation": abs(value - stats.get("mean", value)) / max(stats.get("std", 1), 0.001)
                    })

        return {
            "is_anomaly": is_anomaly,
            "anomaly_score": float(anomaly_score),
            "anomalous_features": anomalous_features
        }


# Training API endpoint handler
async def run_anomaly_training_job(
    tenant_id: UUID,
    job_id: UUID,
    model_name: str,
    start_date: datetime,
    end_date: datetime,
    lift_system_type: str = "ESP",
    hyperparameters: Dict = None
) -> Dict:
    """
    Run an anomaly detection training job with progress tracking.
    """
    from integrations.nexus_client import NexusApiClient
    nexus = NexusApiClient()

    trainer = AnomalyDetectionTrainer(model_name=model_name)

    async def progress_callback(percent, step, **kwargs):
        await nexus.update_training_progress(
            tenant_id=tenant_id,
            job_id=job_id,
            progress_percent=percent,
            current_step=step
        )

    try:
        result = trainer.train(
            tenant_id=tenant_id,
            start_date=start_date,
            end_date=end_date,
            lift_system_type=lift_system_type,
            hyperparameters=hyperparameters,
            progress_callback=progress_callback
        )

        # Save model to database
        model_data = {
            "name": model_name,
            "modelType": "ANOMALY_DETECTION",
            "liftSystemType": lift_system_type,
            "version": datetime.now().strftime("%Y%m%d.%H%M%S"),
            "status": "ACTIVE",
            "trainingSamples": result["training_samples"],
            "modelPath": result["model_path"],
            "mlflowRunId": result["run_id"],
            "hyperparameters": hyperparameters or {}
        }

        saved_model = await nexus.save_model(tenant_id, model_data)

        if saved_model:
            await nexus.complete_training_job(
                tenant_id=tenant_id,
                job_id=job_id,
                model_id=UUID(saved_model["id"]),
                metrics=result["metrics"]
            )

        return result

    except Exception as e:
        await nexus.fail_training_job(
            tenant_id=tenant_id,
            job_id=job_id,
            error_message=str(e)
        )
        raise
