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
Failure Prediction Model

LSTM-based model for predicting equipment failures in oil wells.
Uses time-series sensor data to predict:
- Probability of failure
- Estimated days to failure
- Contributing factors
"""
import logging
from datetime import datetime
from typing import Dict, List, Optional, Tuple, Any
from uuid import UUID

import numpy as np
import pandas as pd
from sklearn.preprocessing import StandardScaler, MinMaxScaler
from sklearn.model_selection import train_test_split

try:
    import tensorflow as tf
    from tensorflow import keras
    from keras.models import Sequential, Model, load_model
    from keras.layers import LSTM, Dense, Dropout, BatchNormalization, Input
    from keras.callbacks import EarlyStopping, ModelCheckpoint, ReduceLROnPlateau
    from keras.optimizers import Adam
    HAS_TENSORFLOW = True
except ImportError:
    HAS_TENSORFLOW = False
    logging.warning("TensorFlow not available, failure prediction model will use mock predictions")

from config import settings

logger = logging.getLogger(__name__)


# Features used for failure prediction
SENSOR_FEATURES = [
    # Pump features
    "pump_intake_pressure",
    "pump_discharge_pressure",
    "pump_temperature",
    "pump_vibration",
    "pump_current",
    "pump_speed",

    # Motor features
    "motor_temperature",
    "motor_current",
    "motor_voltage",
    "motor_power",

    # Production features
    "oil_rate",
    "water_rate",
    "gas_rate",
    "fluid_level",

    # Wellbore features
    "casing_pressure",
    "tubing_pressure",
    "wellhead_temperature",

    # Derived features
    "pump_efficiency",
    "gas_oil_ratio",
    "water_cut"
]

# Feature thresholds for contributing factor analysis
FEATURE_THRESHOLDS = {
    "pump_temperature": {"max": 180, "critical": 200, "unit": "F"},
    "pump_vibration": {"max": 2.5, "critical": 3.5, "unit": "mm/s"},
    "motor_temperature": {"max": 250, "critical": 280, "unit": "F"},
    "motor_current": {"max": 90, "critical": 100, "unit": "A"},
    "pump_efficiency": {"min": 50, "critical": 40, "unit": "%"},
    "casing_pressure": {"max": 1200, "critical": 1400, "unit": "psi"},
}


class FailurePredictionModel:
    """LSTM model for failure prediction."""

    def __init__(self, model_path: Optional[str] = None):
        """
        Initialize the failure prediction model.

        Args:
            model_path: Path to saved model weights
        """
        self.model: Optional[Model] = None
        self.scaler = StandardScaler()
        self.sequence_length = settings.sequence_length
        self.n_features = len(SENSOR_FEATURES)

        if model_path and HAS_TENSORFLOW:
            self.load(model_path)

    def build_model(
        self,
        lstm_units: int = 64,
        dropout_rate: float = 0.2,
        learning_rate: float = 0.001
    ) -> Model:
        """
        Build the LSTM model architecture.

        Args:
            lstm_units: Number of LSTM units
            dropout_rate: Dropout rate for regularization
            learning_rate: Learning rate for optimizer

        Returns:
            Compiled Keras model
        """
        if not HAS_TENSORFLOW:
            raise RuntimeError("TensorFlow is required for model training")

        model = Sequential([
            # Input layer
            Input(shape=(self.sequence_length, self.n_features)),

            # First LSTM layer
            LSTM(lstm_units, return_sequences=True),
            BatchNormalization(),
            Dropout(dropout_rate),

            # Second LSTM layer
            LSTM(lstm_units // 2, return_sequences=False),
            BatchNormalization(),
            Dropout(dropout_rate),

            # Dense layers
            Dense(32, activation='relu'),
            Dropout(dropout_rate / 2),

            # Output: failure probability
            Dense(1, activation='sigmoid')
        ])

        model.compile(
            optimizer=Adam(learning_rate=learning_rate),
            loss='binary_crossentropy',
            metrics=['accuracy', 'AUC', 'Precision', 'Recall']
        )

        self.model = model
        logger.info(f"Built LSTM model with {lstm_units} units")
        return model

    def train(
        self,
        X: np.ndarray,
        y: np.ndarray,
        epochs: int = 50,
        batch_size: int = 32,
        validation_split: float = 0.2,
        early_stop_enabled: bool = True,
        callbacks: Optional[List] = None,
        progress_callback: Optional[callable] = None
    ) -> Dict[str, Any]:
        """
        Train the model.

        Args:
            X: Training sequences (samples, sequence_length, features)
            y: Labels (0 = no failure, 1 = failure)
            epochs: Number of training epochs
            batch_size: Training batch size
            validation_split: Fraction of data for validation
            early_stop_enabled: Whether to use early stopping
            callbacks: Additional Keras callbacks
            progress_callback: Function to call with progress updates

        Returns:
            Training history and metrics
        """
        if not HAS_TENSORFLOW:
            raise RuntimeError("TensorFlow is required for model training")

        if self.model is None:
            self.build_model()

        # Split data
        X_train, X_val, y_train, y_val = train_test_split(
            X, y, test_size=validation_split, random_state=42, stratify=y
        )

        # Scale features
        X_train_scaled = self._scale_sequences(X_train, fit=True)
        X_val_scaled = self._scale_sequences(X_val)

        # Setup callbacks
        callback_list = callbacks or []

        if early_stop_enabled:
            callback_list.append(EarlyStopping(
                monitor='val_loss',
                patience=10,
                restore_best_weights=True
            ))

        callback_list.append(ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=5,
            min_lr=1e-6
        ))

        # Custom progress callback
        if progress_callback:
            class ProgressCallback(keras.callbacks.Callback):
                def on_epoch_end(self, epoch, logs=None):
                    progress_callback(epoch + 1, epochs, logs)

            callback_list.append(ProgressCallback())

        # Train
        logger.info(f"Training on {len(X_train)} samples, validating on {len(X_val)}")
        history = self.model.fit(
            X_train_scaled, y_train,
            validation_data=(X_val_scaled, y_val),
            epochs=epochs,
            batch_size=batch_size,
            callbacks=callback_list,
            verbose=1
        )

        # Evaluate
        val_metrics = self.model.evaluate(X_val_scaled, y_val, verbose=0)
        metrics = dict(zip(self.model.metrics_names, val_metrics))

        logger.info(f"Training completed. Val Loss: {metrics['loss']:.4f}, Val Accuracy: {metrics['accuracy']:.4f}")

        return {
            "history": history.history,
            "metrics": metrics,
            "training_samples": len(X_train),
            "validation_samples": len(X_val)
        }

    def predict(self, X: np.ndarray) -> Tuple[np.ndarray, np.ndarray]:
        """
        Make predictions.

        Args:
            X: Input sequences (samples, sequence_length, features)

        Returns:
            Tuple of (probabilities, days_to_failure estimates)
        """
        if not HAS_TENSORFLOW or self.model is None:
            # Return mock predictions
            n_samples = X.shape[0] if isinstance(X, np.ndarray) else 1
            probabilities = np.random.uniform(0.1, 0.9, n_samples)
            days_to_failure = np.where(
                probabilities > 0.5,
                (30 * (1 - probabilities)).astype(int),
                np.full(n_samples, -1)
            )
            return probabilities, days_to_failure

        # Scale input
        X_scaled = self._scale_sequences(X)

        # Predict
        probabilities = self.model.predict(X_scaled, verbose=0).flatten()

        # Estimate days to failure based on probability
        # Higher probability = fewer days
        days_to_failure = np.where(
            probabilities > 0.5,
            (30 * (1 - probabilities)).astype(int),
            np.full(len(probabilities), -1)  # -1 means not imminent
        )

        return probabilities, days_to_failure

    def predict_single(
        self,
        features: Dict[str, float],
        historical_data: Optional[pd.DataFrame] = None
    ) -> Dict[str, Any]:
        """
        Make prediction for a single well.

        Args:
            features: Current feature values
            historical_data: Historical time series data

        Returns:
            Prediction results with probability, days to failure, and contributing factors
        """
        # If we have historical data, create sequence
        if historical_data is not None and len(historical_data) >= self.sequence_length:
            sequence = self._create_sequence(historical_data)
        else:
            # Create sequence from current features (repeated)
            feature_array = np.array([features.get(f, 0) for f in SENSOR_FEATURES])
            sequence = np.tile(feature_array, (self.sequence_length, 1))

        sequence = sequence.reshape(1, self.sequence_length, self.n_features)

        # Predict
        probability, days = self.predict(sequence)

        # Analyze contributing factors
        contributing_factors = self._analyze_contributing_factors(features)

        return {
            "probability": float(probability[0]),
            "days_to_failure": int(days[0]) if days[0] > 0 else None,
            "contributing_factors": contributing_factors,
            "confidence": self._calculate_confidence(probability[0], contributing_factors)
        }

    def _analyze_contributing_factors(
        self,
        features: Dict[str, float]
    ) -> List[Dict[str, Any]]:
        """Analyze which features are contributing to failure risk."""
        factors = []

        for feature, value in features.items():
            if feature not in FEATURE_THRESHOLDS:
                continue

            threshold = FEATURE_THRESHOLDS[feature]
            impact = None
            trend = "STABLE"

            if "max" in threshold:
                if value > threshold.get("critical", threshold["max"] * 1.1):
                    impact = "HIGH"
                    trend = "UP"
                elif value > threshold["max"]:
                    impact = "MEDIUM"
                    trend = "UP"
                elif value > threshold["max"] * 0.9:
                    impact = "LOW"

            if "min" in threshold:
                if value < threshold.get("critical", threshold["min"] * 0.9):
                    impact = "HIGH"
                    trend = "DOWN"
                elif value < threshold["min"]:
                    impact = "MEDIUM"
                    trend = "DOWN"
                elif value < threshold["min"] * 1.1:
                    impact = "LOW"

            if impact:
                factors.append({
                    "feature": feature,
                    "current_value": value,
                    "threshold": threshold.get("max", threshold.get("min")),
                    "impact": impact,
                    "trend": trend,
                    "unit": threshold.get("unit", "")
                })

        # Sort by impact severity
        impact_order = {"HIGH": 0, "MEDIUM": 1, "LOW": 2}
        factors.sort(key=lambda x: impact_order.get(x["impact"], 3))

        return factors[:5]  # Return top 5 factors

    def _calculate_confidence(
        self,
        probability: float,
        contributing_factors: List[Dict]
    ) -> float:
        """Calculate prediction confidence based on probability and factors."""
        # Base confidence from probability certainty
        base_confidence = 2 * abs(probability - 0.5)

        # Boost confidence if contributing factors align with prediction
        if probability > 0.5 and len(contributing_factors) > 0:
            factor_boost = min(0.2, len(contributing_factors) * 0.05)
            base_confidence = min(1.0, base_confidence + factor_boost)
        elif probability < 0.5 and len(contributing_factors) == 0:
            base_confidence = min(1.0, base_confidence + 0.1)

        return round(base_confidence, 4)

    def _scale_sequences(
        self,
        X: np.ndarray,
        fit: bool = False
    ) -> np.ndarray:
        """Scale feature sequences."""
        original_shape = X.shape
        X_flat = X.reshape(-1, self.n_features)

        if fit:
            X_scaled = self.scaler.fit_transform(X_flat)
        else:
            X_scaled = self.scaler.transform(X_flat)

        return X_scaled.reshape(original_shape)

    def _create_sequence(self, df: pd.DataFrame) -> np.ndarray:
        """Create a sequence from DataFrame."""
        # Ensure we have the required features
        available_features = [f for f in SENSOR_FEATURES if f in df.columns]

        # Get the last sequence_length rows
        data = df[available_features].tail(self.sequence_length).values

        # Pad missing features with zeros
        if len(available_features) < self.n_features:
            padding = np.zeros((data.shape[0], self.n_features - len(available_features)))
            data = np.concatenate([data, padding], axis=1)

        return data

    def save(self, path: str):
        """Save model to disk."""
        if self.model and HAS_TENSORFLOW:
            self.model.save(path)
            logger.info(f"Model saved to {path}")

    def load(self, path: str):
        """Load model from disk."""
        if HAS_TENSORFLOW:
            self.model = load_model(path)
            logger.info(f"Model loaded from {path}")


def prepare_training_data(
    df: pd.DataFrame,
    failure_events: pd.DataFrame,
    sequence_length: int = 24,
    failure_window_hours: int = 168  # 7 days
) -> Tuple[np.ndarray, np.ndarray]:
    """
    Prepare training data from historical telemetry and failure events.

    Args:
        df: Telemetry DataFrame with timestamp index
        failure_events: DataFrame with failure event timestamps
        sequence_length: Number of time steps per sequence
        failure_window_hours: Hours before failure to label as positive

    Returns:
        Tuple of (X sequences, y labels)
    """
    sequences = []
    labels = []

    # Ensure we have the required features
    available_features = [f for f in SENSOR_FEATURES if f in df.columns]
    if len(available_features) == 0:
        raise ValueError("No sensor features found in data")

    df = df[available_features].copy()

    # Fill missing values
    df = df.fillna(method='ffill').fillna(method='bfill').fillna(0)

    # Convert failure events to set of timestamps
    failure_times = set(failure_events['timestamp'].tolist()) if 'timestamp' in failure_events.columns else set()

    # Create sequences
    for i in range(len(df) - sequence_length):
        sequence = df.iloc[i:i + sequence_length].values

        # Check if there's a failure within the window after this sequence
        end_time = df.index[i + sequence_length]
        window_end = end_time + pd.Timedelta(hours=failure_window_hours)

        is_failure = any(
            end_time <= t <= window_end
            for t in failure_times
        )

        sequences.append(sequence)
        labels.append(1 if is_failure else 0)

    X = np.array(sequences)
    y = np.array(labels)

    logger.info(f"Prepared {len(X)} sequences, {sum(y)} positive ({100*sum(y)/len(y):.1f}%)")

    return X, y
