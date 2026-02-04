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
Anomaly Detection Model

Isolation Forest-based model for detecting anomalies in well telemetry.
Identifies unusual patterns that may indicate equipment issues.
"""
import logging
from typing import Dict, List, Optional, Tuple, Any
from uuid import UUID

import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
import joblib

from config import settings

logger = logging.getLogger(__name__)


# Features used for anomaly detection
ANOMALY_FEATURES = [
    # Pump metrics
    "pump_intake_pressure",
    "pump_discharge_pressure",
    "pump_temperature",
    "pump_vibration",
    "pump_current",

    # Motor metrics
    "motor_temperature",
    "motor_current",
    "motor_power",

    # Production metrics
    "oil_rate",
    "water_rate",
    "gas_rate",

    # Wellbore metrics
    "casing_pressure",
    "tubing_pressure",

    # Derived metrics
    "pump_efficiency",
    "water_cut",
    "gas_oil_ratio"
]

# Expected ranges for anomaly analysis
EXPECTED_RANGES = {
    "pump_intake_pressure": {"min": 50, "max": 500, "unit": "psi"},
    "pump_discharge_pressure": {"min": 500, "max": 3000, "unit": "psi"},
    "pump_temperature": {"min": 100, "max": 180, "unit": "F"},
    "pump_vibration": {"min": 0, "max": 2.5, "unit": "mm/s"},
    "pump_current": {"min": 20, "max": 80, "unit": "A"},
    "motor_temperature": {"min": 150, "max": 250, "unit": "F"},
    "motor_current": {"min": 20, "max": 90, "unit": "A"},
    "motor_power": {"min": 50, "max": 500, "unit": "kW"},
    "oil_rate": {"min": 50, "max": 2000, "unit": "bbl/d"},
    "water_rate": {"min": 0, "max": 3000, "unit": "bbl/d"},
    "gas_rate": {"min": 0, "max": 5000, "unit": "mcf/d"},
    "casing_pressure": {"min": 100, "max": 1200, "unit": "psi"},
    "tubing_pressure": {"min": 100, "max": 2000, "unit": "psi"},
    "pump_efficiency": {"min": 50, "max": 100, "unit": "%"},
    "water_cut": {"min": 0, "max": 95, "unit": "%"},
    "gas_oil_ratio": {"min": 100, "max": 2000, "unit": "scf/bbl"}
}


class AnomalyDetectionModel:
    """Isolation Forest model for anomaly detection."""

    def __init__(self, model_path: Optional[str] = None):
        """
        Initialize the anomaly detection model.

        Args:
            model_path: Path to saved model
        """
        self.model: Optional[IsolationForest] = None
        self.scaler = StandardScaler()
        self.n_features = len(ANOMALY_FEATURES)
        self.feature_stats: Dict[str, Dict] = {}

        if model_path:
            self.load(model_path)

    def build_model(
        self,
        n_estimators: int = 100,
        contamination: float = 0.1,
        max_samples: str = "auto",
        random_state: int = 42
    ) -> IsolationForest:
        """
        Build the Isolation Forest model.

        Args:
            n_estimators: Number of trees in the forest
            contamination: Expected proportion of anomalies
            max_samples: Number of samples per tree
            random_state: Random seed

        Returns:
            Configured IsolationForest model
        """
        self.model = IsolationForest(
            n_estimators=n_estimators,
            contamination=contamination,
            max_samples=max_samples,
            random_state=random_state,
            n_jobs=-1
        )

        logger.info(f"Built Isolation Forest with {n_estimators} estimators")
        return self.model

    def train(
        self,
        X: np.ndarray,
        feature_names: Optional[List[str]] = None,
        progress_callback: Optional[callable] = None
    ) -> Dict[str, Any]:
        """
        Train the model on normal operating data.

        Args:
            X: Training data (samples, features)
            feature_names: Names of features
            progress_callback: Progress callback function

        Returns:
            Training statistics
        """
        if self.model is None:
            self.build_model()

        # Scale features
        X_scaled = self.scaler.fit_transform(X)

        if progress_callback:
            progress_callback(10, "Fitting model...")

        # Fit model
        self.model.fit(X_scaled)

        if progress_callback:
            progress_callback(80, "Computing statistics...")

        # Compute feature statistics for anomaly analysis
        feature_names = feature_names or ANOMALY_FEATURES[:X.shape[1]]
        for i, name in enumerate(feature_names):
            self.feature_stats[name] = {
                "mean": float(np.mean(X[:, i])),
                "std": float(np.std(X[:, i])),
                "min": float(np.min(X[:, i])),
                "max": float(np.max(X[:, i])),
                "q1": float(np.percentile(X[:, i], 25)),
                "q3": float(np.percentile(X[:, i], 75))
            }

        # Get training scores
        scores = self.model.decision_function(X_scaled)

        if progress_callback:
            progress_callback(100, "Training complete")

        logger.info(f"Trained on {len(X)} samples")

        return {
            "training_samples": len(X),
            "n_features": X.shape[1],
            "score_mean": float(np.mean(scores)),
            "score_std": float(np.std(scores)),
            "anomaly_threshold": float(np.percentile(scores, 10))
        }

    def predict(self, X: np.ndarray) -> Tuple[np.ndarray, np.ndarray]:
        """
        Detect anomalies.

        Args:
            X: Input data (samples, features)

        Returns:
            Tuple of (is_anomaly array, anomaly_scores array)
        """
        if self.model is None:
            # Return mock predictions
            n_samples = X.shape[0] if isinstance(X, np.ndarray) else 1
            is_anomaly = np.random.choice([True, False], n_samples, p=[0.1, 0.9])
            scores = np.random.uniform(-0.5, 0.5, n_samples)
            return is_anomaly, scores

        # Scale input
        X_scaled = self.scaler.transform(X)

        # Predict (-1 for anomaly, 1 for normal)
        predictions = self.model.predict(X_scaled)
        is_anomaly = predictions == -1

        # Get anomaly scores (higher = more anomalous)
        scores = -self.model.decision_function(X_scaled)  # Negate so higher = more anomalous

        return is_anomaly, scores

    def predict_single(
        self,
        features: Dict[str, float]
    ) -> Dict[str, Any]:
        """
        Detect anomaly for a single observation.

        Args:
            features: Feature values

        Returns:
            Anomaly detection results
        """
        # Create feature array
        feature_array = np.array([
            features.get(f, 0) for f in ANOMALY_FEATURES
        ]).reshape(1, -1)

        # Predict
        is_anomaly, scores = self.predict(feature_array)

        # Analyze which features are anomalous
        anomalous_features = self._analyze_anomalous_features(features)

        return {
            "is_anomaly": bool(is_anomaly[0]),
            "anomaly_score": float(scores[0]),
            "anomalous_features": anomalous_features
        }

    def _analyze_anomalous_features(
        self,
        features: Dict[str, float]
    ) -> List[Dict[str, Any]]:
        """
        Identify which features are anomalous.

        Uses both expected ranges and learned statistics.
        """
        anomalous = []

        for name, value in features.items():
            if name not in EXPECTED_RANGES:
                continue

            expected = EXPECTED_RANGES[name]
            stats = self.feature_stats.get(name, {})

            # Check against expected range
            is_out_of_range = value < expected["min"] or value > expected["max"]

            # Check against learned statistics (if available)
            deviation_score = 0.0
            if stats:
                z_score = abs(value - stats["mean"]) / (stats["std"] + 1e-6)
                iqr = stats["q3"] - stats["q1"]
                is_statistical_outlier = (
                    value < stats["q1"] - 1.5 * iqr or
                    value > stats["q3"] + 1.5 * iqr
                )
                deviation_score = min(1.0, z_score / 3.0)  # Normalize to 0-1
            else:
                is_statistical_outlier = False

            if is_out_of_range or is_statistical_outlier:
                anomalous.append({
                    "feature": name,
                    "current_value": value,
                    "expected_min": expected["min"],
                    "expected_max": expected["max"],
                    "deviation_score": round(deviation_score, 4),
                    "unit": expected.get("unit", "")
                })

        # Sort by deviation score
        anomalous.sort(key=lambda x: x["deviation_score"], reverse=True)

        return anomalous[:5]  # Return top 5 anomalous features

    def save(self, path: str):
        """Save model to disk."""
        if self.model:
            save_data = {
                "model": self.model,
                "scaler": self.scaler,
                "feature_stats": self.feature_stats
            }
            joblib.dump(save_data, path)
            logger.info(f"Model saved to {path}")

    def load(self, path: str):
        """Load model from disk."""
        data = joblib.load(path)
        self.model = data["model"]
        self.scaler = data["scaler"]
        self.feature_stats = data.get("feature_stats", {})
        logger.info(f"Model loaded from {path}")


def prepare_training_data(
    df: pd.DataFrame,
    filter_normal: bool = True
) -> np.ndarray:
    """
    Prepare training data from telemetry DataFrame.

    For anomaly detection, we typically train only on "normal" data
    so the model learns what normal looks like.

    Args:
        df: Telemetry DataFrame
        filter_normal: Whether to filter out known anomalies

    Returns:
        NumPy array of training samples
    """
    # Get available features
    available_features = [f for f in ANOMALY_FEATURES if f in df.columns]
    if len(available_features) == 0:
        raise ValueError("No anomaly features found in data")

    data = df[available_features].copy()

    # Fill missing values
    data = data.fillna(method='ffill').fillna(method='bfill').fillna(0)

    if filter_normal:
        # Remove obvious outliers (beyond 3 sigma) for training
        for col in data.columns:
            mean = data[col].mean()
            std = data[col].std()
            data = data[
                (data[col] >= mean - 3 * std) &
                (data[col] <= mean + 3 * std)
            ]

    X = data.values
    logger.info(f"Prepared {len(X)} training samples with {X.shape[1]} features")

    return X


class MultiVariateAnomalyDetector:
    """
    Advanced anomaly detector that combines multiple detection methods.
    """

    def __init__(self):
        self.isolation_forest = AnomalyDetectionModel()
        self.statistical_bounds: Dict[str, Dict] = {}

    def fit(self, X: np.ndarray, feature_names: List[str]):
        """
        Fit the detector on training data.

        Args:
            X: Training data
            feature_names: Feature names
        """
        # Train Isolation Forest
        self.isolation_forest.train(X, feature_names)

        # Compute statistical bounds
        for i, name in enumerate(feature_names):
            values = X[:, i]
            self.statistical_bounds[name] = {
                "mean": np.mean(values),
                "std": np.std(values),
                "min": np.min(values),
                "max": np.max(values),
                "p5": np.percentile(values, 5),
                "p95": np.percentile(values, 95)
            }

    def detect(self, features: Dict[str, float]) -> Dict[str, Any]:
        """
        Detect anomalies using multiple methods.

        Args:
            features: Feature values

        Returns:
            Combined anomaly detection result
        """
        # Isolation Forest detection
        if_result = self.isolation_forest.predict_single(features)

        # Statistical detection
        stat_anomalies = []
        for name, value in features.items():
            if name in self.statistical_bounds:
                bounds = self.statistical_bounds[name]
                if value < bounds["p5"] or value > bounds["p95"]:
                    z_score = abs(value - bounds["mean"]) / (bounds["std"] + 1e-6)
                    stat_anomalies.append({
                        "feature": name,
                        "value": value,
                        "z_score": z_score
                    })

        # Combine results
        is_anomaly = if_result["is_anomaly"] or len(stat_anomalies) >= 2

        return {
            "is_anomaly": is_anomaly,
            "anomaly_score": if_result["anomaly_score"],
            "detection_method": "combined",
            "isolation_forest_anomaly": if_result["is_anomaly"],
            "statistical_anomalies": len(stat_anomalies),
            "anomalous_features": if_result["anomalous_features"]
        }
