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
Failure Prediction Model Trainer

Trains LSTM-based models for predicting equipment failures.
Uses MLflow for experiment tracking and model registry.
"""
import logging
import os
from datetime import datetime
from typing import Dict, List, Optional, Tuple, Any
from uuid import UUID, uuid4

import numpy as np
import pandas as pd
import mlflow
import mlflow.keras
from sklearn.model_selection import train_test_split
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    confusion_matrix,
    classification_report
)

from config import settings
from training.data_loader import TrainingDataLoader

logger = logging.getLogger(__name__)

# Conditional TensorFlow import
try:
    import tensorflow as tf
    from tensorflow import keras
    from tensorflow.keras.models import Sequential
    from tensorflow.keras.layers import LSTM, Dense, Dropout, BatchNormalization
    from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau, ModelCheckpoint
    from tensorflow.keras.optimizers import Adam
    TF_AVAILABLE = True
except ImportError:
    TF_AVAILABLE = False
    logger.warning("TensorFlow not available. Training will not work.")


class FailurePredictionTrainer:
    """
    Trains LSTM models for failure prediction.

    Features:
    - Configurable hyperparameters
    - MLflow experiment tracking
    - Early stopping and learning rate scheduling
    - Feature importance calculation
    - Model versioning
    """

    def __init__(
        self,
        experiment_name: str = None,
        model_name: str = "failure_prediction"
    ):
        self.experiment_name = experiment_name or settings.mlflow_experiment_name
        self.model_name = model_name
        self.data_loader = TrainingDataLoader(use_api=False)

        # Setup MLflow
        mlflow.set_tracking_uri(settings.mlflow_tracking_uri)
        mlflow.set_experiment(self.experiment_name)

        self.model = None
        self.feature_columns: List[str] = []
        self.feature_stats: Dict[str, Dict] = {}

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
        Train a failure prediction model.

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
        if not TF_AVAILABLE:
            raise RuntimeError("TensorFlow is required for training")

        # Default hyperparameters
        hp = {
            "sequence_length": 24,
            "lstm_units_1": 64,
            "lstm_units_2": 32,
            "dropout_rate": 0.3,
            "learning_rate": 0.001,
            "batch_size": 32,
            "epochs": 100,
            "early_stopping_patience": 10,
            "validation_split": 0.2,
            "test_split": 0.15
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
                mlflow.log_param("data_start_date", start_date.isoformat())
                mlflow.log_param("data_end_date", end_date.isoformat())

                # Step 1: Load data
                if progress_callback:
                    progress_callback(10, "Loading training data")

                # Use synthetic data for now (replace with real data loader in production)
                features_df, labels_df = self.data_loader.create_synthetic_data(
                    num_wells=20,
                    days=180,
                    samples_per_day=96,
                    failure_rate=0.15
                )

                mlflow.log_metric("total_samples", len(features_df))
                mlflow.log_metric("num_wells", features_df["well_id"].nunique())

                # Step 2: Prepare sequences
                if progress_callback:
                    progress_callback(20, "Preparing sequences")

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

                X, y = self.data_loader.prepare_sequences(
                    features_df,
                    sequence_length=hp["sequence_length"],
                    feature_columns=self.feature_columns,
                    target_column="failure_within_14_days"
                )

                mlflow.log_metric("sequence_count", len(X))
                mlflow.log_metric("positive_ratio", float(y.mean()))

                # Step 3: Split data
                X_train, X_test, y_train, y_test = train_test_split(
                    X, y, test_size=hp["test_split"], random_state=42, stratify=y
                )

                mlflow.log_metric("train_samples", len(X_train))
                mlflow.log_metric("test_samples", len(X_test))

                # Step 4: Build model
                if progress_callback:
                    progress_callback(30, "Building model")

                self.model = self._build_model(
                    input_shape=(hp["sequence_length"], len(self.feature_columns)),
                    lstm_units_1=hp["lstm_units_1"],
                    lstm_units_2=hp["lstm_units_2"],
                    dropout_rate=hp["dropout_rate"],
                    learning_rate=hp["learning_rate"]
                )

                # Step 5: Train model
                if progress_callback:
                    progress_callback(40, "Training model")

                callbacks = [
                    EarlyStopping(
                        monitor="val_loss",
                        patience=hp["early_stopping_patience"],
                        restore_best_weights=True
                    ),
                    ReduceLROnPlateau(
                        monitor="val_loss",
                        factor=0.5,
                        patience=5,
                        min_lr=0.0001
                    )
                ]

                # Custom callback for progress updates
                class ProgressCallback(keras.callbacks.Callback):
                    def __init__(self, total_epochs, callback_fn):
                        super().__init__()
                        self.total_epochs = total_epochs
                        self.callback_fn = callback_fn

                    def on_epoch_end(self, epoch, logs=None):
                        if self.callback_fn:
                            progress = 40 + int((epoch / self.total_epochs) * 40)
                            self.callback_fn(
                                progress,
                                f"Training epoch {epoch + 1}/{self.total_epochs}",
                                current_epoch=epoch + 1,
                                total_epochs=self.total_epochs
                            )

                if progress_callback:
                    callbacks.append(ProgressCallback(hp["epochs"], progress_callback))

                history = self.model.fit(
                    X_train, y_train,
                    batch_size=hp["batch_size"],
                    epochs=hp["epochs"],
                    validation_split=hp["validation_split"],
                    callbacks=callbacks,
                    verbose=1
                )

                # Log training metrics
                for metric_name, values in history.history.items():
                    for epoch, value in enumerate(values):
                        mlflow.log_metric(f"train_{metric_name}", value, step=epoch)

                # Step 6: Evaluate model
                if progress_callback:
                    progress_callback(85, "Evaluating model")

                y_pred_proba = self.model.predict(X_test)
                y_pred = (y_pred_proba > 0.5).astype(int).flatten()

                metrics = {
                    "accuracy": accuracy_score(y_test, y_pred),
                    "precision": precision_score(y_test, y_pred, zero_division=0),
                    "recall": recall_score(y_test, y_pred, zero_division=0),
                    "f1_score": f1_score(y_test, y_pred, zero_division=0),
                    "auc_roc": roc_auc_score(y_test, y_pred_proba) if y_test.sum() > 0 else 0
                }

                for name, value in metrics.items():
                    mlflow.log_metric(name, value)

                # Log confusion matrix
                cm = confusion_matrix(y_test, y_pred)
                mlflow.log_text(str(cm), "confusion_matrix.txt")

                # Log classification report
                report = classification_report(y_test, y_pred)
                mlflow.log_text(report, "classification_report.txt")

                # Step 7: Calculate feature importance
                if progress_callback:
                    progress_callback(90, "Calculating feature importance")

                feature_importance = self._calculate_feature_importance(X_test, y_test)
                mlflow.log_dict(feature_importance, "feature_importance.json")

                # Step 8: Save model
                if progress_callback:
                    progress_callback(95, "Saving model")

                # Save to MLflow
                mlflow.keras.log_model(
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
                self.model.save(os.path.join(model_dir, "model.keras"))

                # Save feature stats
                self._save_feature_stats(features_df)

                if progress_callback:
                    progress_callback(100, "Training complete")

                return {
                    "status": "success",
                    "run_id": run_id,
                    "model_path": model_dir,
                    "metrics": metrics,
                    "feature_importance": feature_importance,
                    "training_samples": len(X_train),
                    "test_samples": len(X_test),
                    "epochs_trained": len(history.history["loss"])
                }

            except Exception as e:
                logger.error(f"Training failed: {e}")
                mlflow.log_param("error", str(e))
                raise

    def _build_model(
        self,
        input_shape: Tuple[int, int],
        lstm_units_1: int,
        lstm_units_2: int,
        dropout_rate: float,
        learning_rate: float
    ) -> keras.Model:
        """Build LSTM model architecture."""
        model = Sequential([
            LSTM(lstm_units_1, return_sequences=True, input_shape=input_shape),
            BatchNormalization(),
            Dropout(dropout_rate),

            LSTM(lstm_units_2, return_sequences=False),
            BatchNormalization(),
            Dropout(dropout_rate),

            Dense(32, activation="relu"),
            Dropout(dropout_rate / 2),

            Dense(1, activation="sigmoid")
        ])

        model.compile(
            optimizer=Adam(learning_rate=learning_rate),
            loss="binary_crossentropy",
            metrics=["accuracy", keras.metrics.AUC(name="auc")]
        )

        logger.info(f"Model built with {model.count_params():,} parameters")
        return model

    def _calculate_feature_importance(
        self,
        X_test: np.ndarray,
        y_test: np.ndarray
    ) -> Dict[str, float]:
        """
        Calculate feature importance using permutation importance.
        """
        base_score = self.model.evaluate(X_test, y_test, verbose=0)[0]
        importance = {}

        for i, feature in enumerate(self.feature_columns):
            # Permute feature
            X_permuted = X_test.copy()
            np.random.shuffle(X_permuted[:, :, i])

            # Calculate score drop
            permuted_score = self.model.evaluate(X_permuted, y_test, verbose=0)[0]
            importance[feature] = float(permuted_score - base_score)

        # Normalize
        total = sum(abs(v) for v in importance.values())
        if total > 0:
            importance = {k: abs(v) / total for k, v in importance.items()}

        # Sort by importance
        importance = dict(sorted(importance.items(), key=lambda x: x[1], reverse=True))
        return importance

    def _save_feature_stats(self, df: pd.DataFrame):
        """Save feature statistics for normalization during inference."""
        self.feature_stats = {}

        for col in self.feature_columns:
            if col in df.columns:
                self.feature_stats[col] = {
                    "mean": float(df[col].mean()),
                    "std": float(df[col].std()),
                    "min": float(df[col].min()),
                    "max": float(df[col].max())
                }

    def load_model(self, model_path: str = None, run_id: str = None):
        """Load a trained model."""
        if run_id:
            # Load from MLflow
            model_uri = f"runs:/{run_id}/model"
            self.model = mlflow.keras.load_model(model_uri)
        elif model_path:
            # Load from local path
            self.model = keras.models.load_model(model_path)
        else:
            raise ValueError("Either model_path or run_id must be provided")

        logger.info("Model loaded successfully")


# Training API endpoint handler
async def run_training_job(
    tenant_id: UUID,
    job_id: UUID,
    model_name: str,
    start_date: datetime,
    end_date: datetime,
    lift_system_type: str = "ESP",
    hyperparameters: Dict = None
) -> Dict:
    """
    Run a training job with progress tracking.

    This function is called from the training API endpoint.
    """
    from integrations.nexus_client import NexusApiClient
    nexus = NexusApiClient()

    trainer = FailurePredictionTrainer(model_name=model_name)

    async def progress_callback(percent, step, **kwargs):
        await nexus.update_training_progress(
            tenant_id=tenant_id,
            job_id=job_id,
            progress_percent=percent,
            current_step=step,
            current_epoch=kwargs.get("current_epoch"),
            total_epochs=kwargs.get("total_epochs")
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
            "modelType": "FAILURE_PREDICTION",
            "liftSystemType": lift_system_type,
            "version": datetime.now().strftime("%Y%m%d.%H%M%S"),
            "status": "ACTIVE",
            "accuracy": result["metrics"]["accuracy"],
            "precision": result["metrics"]["precision"],
            "recall": result["metrics"]["recall"],
            "f1Score": result["metrics"]["f1_score"],
            "aucRoc": result["metrics"]["auc_roc"],
            "trainingSamples": result["training_samples"],
            "featureImportance": result["feature_importance"],
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
