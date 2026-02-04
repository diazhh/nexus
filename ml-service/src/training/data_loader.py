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
Training Data Loader

Fetches and prepares historical telemetry data for ML model training.
Supports fetching from:
- ThingsBoard REST API
- PostgreSQL ts_kv tables directly
"""
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple, Any
from uuid import UUID

import numpy as np
import pandas as pd
from sqlalchemy import text

from config import settings
from data.database import get_db_context
from integrations.thingsboard_client import (
    ThingsBoardClient,
    get_telemetry_keys_for_lift_type,
    ESP_TELEMETRY_KEYS
)

logger = logging.getLogger(__name__)


class TrainingDataLoader:
    """
    Loads and prepares training data for ML models.

    Can fetch data from:
    1. ThingsBoard REST API (preferred for small datasets)
    2. PostgreSQL ts_kv directly (for large datasets)
    """

    def __init__(self, use_api: bool = True):
        """
        Initialize the data loader.

        Args:
            use_api: If True, use ThingsBoard API. If False, query database directly.
        """
        self.use_api = use_api
        self.tb_client = ThingsBoardClient() if use_api else None

    async def load_training_data(
        self,
        tenant_id: UUID,
        start_date: datetime,
        end_date: datetime,
        lift_system_type: str = "ESP",
        include_failure_labels: bool = True,
        min_samples_per_well: int = 1000
    ) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """
        Load training data for all wells.

        Args:
            tenant_id: Tenant ID
            start_date: Start of training period
            end_date: End of training period
            lift_system_type: Type of lift system to train for
            include_failure_labels: Whether to include failure event labels
            min_samples_per_well: Minimum samples required per well

        Returns:
            Tuple of (features_df, labels_df)
        """
        logger.info(f"Loading training data from {start_date} to {end_date}")

        if self.use_api:
            return await self._load_via_api(
                tenant_id, start_date, end_date,
                lift_system_type, include_failure_labels, min_samples_per_well
            )
        else:
            return await self._load_via_database(
                tenant_id, start_date, end_date,
                lift_system_type, include_failure_labels, min_samples_per_well
            )

    async def _load_via_api(
        self,
        tenant_id: UUID,
        start_date: datetime,
        end_date: datetime,
        lift_system_type: str,
        include_failure_labels: bool,
        min_samples_per_well: int
    ) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """Load data via ThingsBoard API."""
        await self.tb_client.login()

        # Get all wells
        wells = await self.tb_client.get_wells()
        logger.info(f"Found {len(wells)} wells")

        # Filter by lift system type
        wells = [w for w in wells if self._get_well_lift_type(w) == lift_system_type]
        logger.info(f"Found {len(wells)} {lift_system_type} wells")

        if not wells:
            return pd.DataFrame(), pd.DataFrame()

        # Get telemetry keys
        telemetry_keys = get_telemetry_keys_for_lift_type(lift_system_type)

        # Load data for each well
        all_features = []
        all_labels = []

        for well in wells:
            well_id = well["id"]["id"]
            well_name = well["name"]

            try:
                # Fetch telemetry
                df = await self.tb_client.get_historical_telemetry_df(
                    asset_id=well_id,
                    keys=telemetry_keys,
                    start_date=start_date,
                    end_date=end_date,
                    interval_minutes=15  # 15-minute aggregation
                )

                if len(df) < min_samples_per_well:
                    logger.warning(f"Well {well_name}: insufficient data ({len(df)} samples)")
                    continue

                # Add well identifier
                df["well_id"] = well_id
                df["well_name"] = well_name

                # Get failure labels if needed
                if include_failure_labels:
                    labels = await self._get_failure_labels(well_id, start_date, end_date)
                    df = self._merge_labels(df, labels)
                    all_labels.append(df[["well_id", "failure_within_14_days"]])

                all_features.append(df)
                logger.info(f"Well {well_name}: loaded {len(df)} samples")

            except Exception as e:
                logger.error(f"Failed to load data for well {well_name}: {e}")
                continue

        if not all_features:
            return pd.DataFrame(), pd.DataFrame()

        features_df = pd.concat(all_features, ignore_index=True)
        labels_df = pd.concat(all_labels, ignore_index=True) if all_labels else pd.DataFrame()

        logger.info(f"Total training samples: {len(features_df)}")
        return features_df, labels_df

    async def _load_via_database(
        self,
        tenant_id: UUID,
        start_date: datetime,
        end_date: datetime,
        lift_system_type: str,
        include_failure_labels: bool,
        min_samples_per_well: int
    ) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """Load data directly from PostgreSQL ts_kv tables."""
        start_ts = int(start_date.timestamp() * 1000)
        end_ts = int(end_date.timestamp() * 1000)

        telemetry_keys = get_telemetry_keys_for_lift_type(lift_system_type)
        keys_str = ",".join([f"'{k}'" for k in telemetry_keys])

        query = f"""
        WITH well_assets AS (
            SELECT a.id, a.name
            FROM asset a
            JOIN attribute_kv attr ON a.id = attr.entity_id
            WHERE a.tenant_id = :tenant_id
              AND a.type = 'WELL'
              AND attr.attribute_key = 'liftSystemType'
              AND attr.str_v = :lift_system_type
        )
        SELECT
            w.id as well_id,
            w.name as well_name,
            kv.ts,
            kv.key,
            COALESCE(kv.dbl_v, kv.long_v) as value
        FROM well_assets w
        JOIN ts_kv kv ON w.id = kv.entity_id
        WHERE kv.ts BETWEEN :start_ts AND :end_ts
          AND kv.key IN ({keys_str})
        ORDER BY w.id, kv.ts, kv.key
        """

        with get_db_context() as db:
            result = db.execute(
                text(query),
                {
                    "tenant_id": str(tenant_id),
                    "lift_system_type": lift_system_type,
                    "start_ts": start_ts,
                    "end_ts": end_ts
                }
            )
            rows = result.fetchall()

        if not rows:
            return pd.DataFrame(), pd.DataFrame()

        # Pivot the data
        df = pd.DataFrame(rows, columns=["well_id", "well_name", "ts", "key", "value"])
        df["timestamp"] = pd.to_datetime(df["ts"], unit="ms")

        # Pivot to get one column per telemetry key
        pivot_df = df.pivot_table(
            index=["well_id", "well_name", "timestamp"],
            columns="key",
            values="value",
            aggfunc="mean"
        ).reset_index()

        # Flatten column names
        pivot_df.columns = [col if isinstance(col, str) else col[0] for col in pivot_df.columns]

        logger.info(f"Loaded {len(pivot_df)} samples from database")

        # Get labels if needed
        labels_df = pd.DataFrame()
        if include_failure_labels:
            # Query failure events from alarms
            labels_df = await self._get_failure_labels_from_db(
                tenant_id, start_date, end_date
            )
            pivot_df = self._merge_labels(pivot_df, labels_df)

        return pivot_df, labels_df

    async def _get_failure_labels(
        self,
        well_id: str,
        start_date: datetime,
        end_date: datetime
    ) -> pd.DataFrame:
        """
        Get failure event labels from ThingsBoard alarms.

        Creates binary labels: 1 if failure occurred within 14 days, 0 otherwise.
        """
        await self.tb_client._get_token()

        start_ts = int(start_date.timestamp() * 1000)
        end_ts = int(end_date.timestamp() * 1000)

        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(
                    f"{self.tb_client.base_url}/api/alarm/ASSET/{well_id}",
                    headers=self.tb_client._headers(),
                    params={
                        "startTime": start_ts,
                        "endTime": end_ts,
                        "alarmType": "EQUIPMENT_FAILURE",
                        "pageSize": 1000
                    }
                )
                response.raise_for_status()
                alarms = response.json().get("data", [])

            # Extract failure timestamps
            failure_times = [
                datetime.fromtimestamp(a["createdTime"] / 1000)
                for a in alarms
                if a.get("severity") in ["CRITICAL", "MAJOR"]
            ]

            return pd.DataFrame({
                "well_id": [well_id] * len(failure_times),
                "failure_time": failure_times
            })
        except Exception as e:
            logger.warning(f"Could not fetch failure labels: {e}")
            return pd.DataFrame()

    async def _get_failure_labels_from_db(
        self,
        tenant_id: UUID,
        start_date: datetime,
        end_date: datetime
    ) -> pd.DataFrame:
        """Get failure labels from database alarms."""
        start_ts = int(start_date.timestamp() * 1000)
        end_ts = int(end_date.timestamp() * 1000)

        query = """
        SELECT
            originator_id as well_id,
            created_time as failure_ts
        FROM alarm
        WHERE tenant_id = :tenant_id
          AND originator_type = 'ASSET'
          AND type = 'EQUIPMENT_FAILURE'
          AND severity IN ('CRITICAL', 'MAJOR')
          AND created_time BETWEEN :start_ts AND :end_ts
        """

        with get_db_context() as db:
            result = db.execute(
                text(query),
                {
                    "tenant_id": str(tenant_id),
                    "start_ts": start_ts,
                    "end_ts": end_ts
                }
            )
            rows = result.fetchall()

        if not rows:
            return pd.DataFrame()

        df = pd.DataFrame(rows, columns=["well_id", "failure_ts"])
        df["failure_time"] = pd.to_datetime(df["failure_ts"], unit="ms")
        return df

    def _merge_labels(
        self,
        features_df: pd.DataFrame,
        labels_df: pd.DataFrame,
        prediction_horizon_days: int = 14
    ) -> pd.DataFrame:
        """
        Merge failure labels into features dataframe.

        Creates binary label: 1 if failure within prediction_horizon_days, 0 otherwise.
        """
        if labels_df.empty:
            features_df["failure_within_14_days"] = 0
            return features_df

        # For each row, check if there's a failure within the horizon
        def check_failure(row):
            well_failures = labels_df[labels_df["well_id"] == row["well_id"]]
            if well_failures.empty:
                return 0

            row_time = row["timestamp"] if "timestamp" in row.index else row.name
            horizon_end = row_time + timedelta(days=prediction_horizon_days)

            for _, failure in well_failures.iterrows():
                if row_time <= failure["failure_time"] <= horizon_end:
                    return 1
            return 0

        features_df["failure_within_14_days"] = features_df.apply(check_failure, axis=1)
        return features_df

    def _get_well_lift_type(self, well: Dict) -> str:
        """Extract lift system type from well asset."""
        # Check additional info
        add_info = well.get("additionalInfo", {}) or {}
        lift_type = add_info.get("liftSystemType", "")

        if not lift_type:
            # Try to infer from name
            name = well.get("name", "").upper()
            if "ESP" in name:
                return "ESP"
            elif "PCP" in name:
                return "PCP"
            elif "BEAM" in name or "ROD" in name:
                return "ROD_PUMP"
            elif "GAS" in name:
                return "GAS_LIFT"

        return lift_type or "ESP"

    def prepare_sequences(
        self,
        df: pd.DataFrame,
        sequence_length: int = 24,
        feature_columns: List[str] = None,
        target_column: str = "failure_within_14_days"
    ) -> Tuple[np.ndarray, np.ndarray]:
        """
        Prepare sequences for LSTM training.

        Args:
            df: DataFrame with features
            sequence_length: Number of timesteps per sequence
            feature_columns: Columns to use as features
            target_column: Column to use as target

        Returns:
            Tuple of (X, y) arrays
        """
        if feature_columns is None:
            feature_columns = [c for c in df.columns if c not in
                             ["well_id", "well_name", "timestamp", target_column]]

        # Fill missing values
        df_filled = df[feature_columns].fillna(method="ffill").fillna(0)

        # Normalize features
        mean = df_filled.mean()
        std = df_filled.std().replace(0, 1)
        df_normalized = (df_filled - mean) / std

        X_list = []
        y_list = []

        # Create sequences per well
        for well_id in df["well_id"].unique():
            well_data = df[df["well_id"] == well_id]
            well_features = df_normalized.loc[well_data.index].values
            well_targets = df.loc[well_data.index, target_column].values

            for i in range(len(well_features) - sequence_length):
                X_list.append(well_features[i:i + sequence_length])
                y_list.append(well_targets[i + sequence_length])

        X = np.array(X_list)
        y = np.array(y_list)

        logger.info(f"Created {len(X)} sequences with shape {X.shape}")
        return X, y

    def create_synthetic_data(
        self,
        num_wells: int = 10,
        days: int = 180,
        samples_per_day: int = 96,  # 15-minute intervals
        failure_rate: float = 0.1
    ) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """
        Create synthetic training data for testing.

        Args:
            num_wells: Number of wells to simulate
            days: Number of days of data
            samples_per_day: Samples per day
            failure_rate: Probability of failure event

        Returns:
            Tuple of (features_df, labels_df)
        """
        logger.info(f"Creating synthetic data: {num_wells} wells, {days} days")

        np.random.seed(42)
        total_samples = days * samples_per_day

        all_data = []
        all_failures = []

        for well_idx in range(num_wells):
            well_id = f"well-{well_idx:03d}"

            # Base values with some well-specific variation
            base_intake_pressure = 1200 + np.random.normal(0, 100)
            base_discharge_pressure = 3500 + np.random.normal(0, 200)
            base_motor_current = 45 + np.random.normal(0, 5)
            base_motor_temp = 170 + np.random.normal(0, 10)
            base_flow_rate = 800 + np.random.normal(0, 100)

            # Generate time series
            timestamps = pd.date_range(
                start=datetime.now() - timedelta(days=days),
                periods=total_samples,
                freq="15min"
            )

            # Random failure events
            failure_indices = []
            if np.random.random() < failure_rate:
                num_failures = np.random.randint(1, 4)
                failure_indices = np.random.choice(
                    range(total_samples // 4, total_samples),
                    size=num_failures,
                    replace=False
                )

            # Generate features with degradation before failures
            data = []
            for i, ts in enumerate(timestamps):
                # Check if approaching failure
                days_to_failure = float('inf')
                for fi in failure_indices:
                    if fi > i:
                        days_to_failure = min(days_to_failure, (fi - i) / samples_per_day)

                # Add degradation signal
                degradation = 0
                if days_to_failure < 14:
                    degradation = (14 - days_to_failure) / 14

                # Generate features
                row = {
                    "timestamp": ts,
                    "well_id": well_id,
                    "well_name": f"Test Well {well_idx + 1}",
                    "pump_intake_pressure_psi": base_intake_pressure + np.random.normal(0, 20) - degradation * 100,
                    "pump_discharge_pressure_psi": base_discharge_pressure + np.random.normal(0, 50) - degradation * 200,
                    "motor_current_amps": base_motor_current + np.random.normal(0, 2) + degradation * 15,
                    "motor_temperature_f": base_motor_temp + np.random.normal(0, 5) + degradation * 30,
                    "vibration_in_s": 0.1 + np.random.exponential(0.02) + degradation * 0.3,
                    "flow_rate_bpd": base_flow_rate + np.random.normal(0, 30) - degradation * 200,
                    "frequency_hz": 60 + np.random.normal(0, 1),
                    "gas_oil_ratio": 500 + np.random.normal(0, 50),
                    "water_cut_percent": 30 + np.random.normal(0, 5),
                    "failure_within_14_days": 1 if days_to_failure <= 14 else 0
                }
                data.append(row)

            well_df = pd.DataFrame(data)
            all_data.append(well_df)

            # Record failure events
            for fi in failure_indices:
                all_failures.append({
                    "well_id": well_id,
                    "failure_time": timestamps[fi]
                })

        features_df = pd.concat(all_data, ignore_index=True)
        labels_df = pd.DataFrame(all_failures)

        positive_rate = features_df["failure_within_14_days"].mean()
        logger.info(f"Synthetic data: {len(features_df)} samples, {positive_rate:.2%} positive rate")

        return features_df, labels_df
