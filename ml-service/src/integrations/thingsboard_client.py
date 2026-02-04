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
ThingsBoard REST API Client

Provides integration with ThingsBoard for:
- Fetching historical telemetry data
- Managing assets and devices
- Importing rule chains
- Creating alarms
"""
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
from uuid import UUID

import httpx
import pandas as pd

from config import settings

logger = logging.getLogger(__name__)


class ThingsBoardClient:
    """Client for ThingsBoard REST API."""

    def __init__(
        self,
        base_url: str = None,
        username: str = None,
        password: str = None
    ):
        self.base_url = base_url or settings.thingsboard_url
        self.username = username or settings.thingsboard_username
        self.password = password or settings.thingsboard_password
        self._token: Optional[str] = None
        self._refresh_token: Optional[str] = None

    async def _get_token(self) -> str:
        """Get authentication token."""
        if self._token:
            return self._token

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/api/auth/login",
                json={
                    "username": self.username,
                    "password": self.password
                }
            )
            response.raise_for_status()
            data = response.json()
            self._token = data["token"]
            self._refresh_token = data.get("refreshToken")
            return self._token

    def _headers(self) -> Dict[str, str]:
        """Get request headers with auth token."""
        return {
            "Content-Type": "application/json",
            "X-Authorization": f"Bearer {self._token}"
        }

    async def login(self) -> bool:
        """Authenticate with ThingsBoard."""
        try:
            await self._get_token()
            logger.info("Successfully authenticated with ThingsBoard")
            return True
        except Exception as e:
            logger.error(f"ThingsBoard authentication failed: {e}")
            return False

    async def get_tenant_assets(
        self,
        asset_type: str = None,
        page_size: int = 100,
        page: int = 0
    ) -> List[Dict]:
        """
        Get assets for the current tenant.

        Args:
            asset_type: Filter by asset type (e.g., "WELL", "ESP")
            page_size: Number of results per page
            page: Page number

        Returns:
            List of asset dictionaries
        """
        await self._get_token()

        params = {
            "pageSize": page_size,
            "page": page
        }
        if asset_type:
            params["type"] = asset_type

        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/api/tenant/assets",
                headers=self._headers(),
                params=params
            )
            response.raise_for_status()
            data = response.json()
            return data.get("data", [])

    async def get_wells(self, page_size: int = 100) -> List[Dict]:
        """Get all well assets."""
        wells = []
        page = 0

        while True:
            batch = await self.get_tenant_assets(
                asset_type="WELL",
                page_size=page_size,
                page=page
            )
            if not batch:
                break
            wells.extend(batch)
            if len(batch) < page_size:
                break
            page += 1

        logger.info(f"Found {len(wells)} well assets")
        return wells

    async def get_asset_telemetry(
        self,
        asset_id: str,
        keys: List[str],
        start_ts: int,
        end_ts: int,
        interval: int = 0,
        agg: str = "NONE",
        limit: int = 100000
    ) -> Dict[str, List[Dict]]:
        """
        Get telemetry timeseries for an asset.

        Args:
            asset_id: Asset ID (UUID string)
            keys: List of telemetry keys to fetch
            start_ts: Start timestamp (milliseconds)
            end_ts: End timestamp (milliseconds)
            interval: Aggregation interval in ms (0 = no aggregation)
            agg: Aggregation type (NONE, AVG, MIN, MAX, SUM, COUNT)
            limit: Maximum number of data points per key

        Returns:
            Dictionary mapping keys to list of {ts, value} dicts
        """
        await self._get_token()

        params = {
            "keys": ",".join(keys),
            "startTs": start_ts,
            "endTs": end_ts,
            "limit": limit
        }

        if interval > 0:
            params["interval"] = interval
            params["agg"] = agg

        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.get(
                f"{self.base_url}/api/plugins/telemetry/ASSET/{asset_id}/values/timeseries",
                headers=self._headers(),
                params=params
            )
            response.raise_for_status()
            return response.json()

    async def get_historical_telemetry_df(
        self,
        asset_id: str,
        keys: List[str],
        start_date: datetime,
        end_date: datetime,
        interval_minutes: int = 5
    ) -> pd.DataFrame:
        """
        Get historical telemetry as a pandas DataFrame.

        Args:
            asset_id: Asset ID
            keys: Telemetry keys to fetch
            start_date: Start date
            end_date: End date
            interval_minutes: Aggregation interval

        Returns:
            DataFrame with timestamp index and columns for each key
        """
        start_ts = int(start_date.timestamp() * 1000)
        end_ts = int(end_date.timestamp() * 1000)
        interval_ms = interval_minutes * 60 * 1000

        telemetry = await self.get_asset_telemetry(
            asset_id=asset_id,
            keys=keys,
            start_ts=start_ts,
            end_ts=end_ts,
            interval=interval_ms,
            agg="AVG"
        )

        # Convert to DataFrame
        if not telemetry:
            return pd.DataFrame()

        # Get all timestamps
        all_timestamps = set()
        for key_data in telemetry.values():
            for point in key_data:
                all_timestamps.add(point["ts"])

        # Build DataFrame
        timestamps = sorted(all_timestamps)
        data = {"timestamp": [datetime.fromtimestamp(ts / 1000) for ts in timestamps]}

        for key in keys:
            key_values = {p["ts"]: p["value"] for p in telemetry.get(key, [])}
            data[key] = [key_values.get(ts) for ts in timestamps]

        df = pd.DataFrame(data)
        df.set_index("timestamp", inplace=True)

        # Convert to numeric
        for col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce")

        return df

    async def get_asset_attributes(
        self,
        asset_id: str,
        scope: str = "SERVER_SCOPE"
    ) -> Dict[str, Any]:
        """Get asset attributes."""
        await self._get_token()

        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/api/plugins/telemetry/ASSET/{asset_id}/values/attributes/{scope}",
                headers=self._headers()
            )
            response.raise_for_status()
            attrs = response.json()
            return {attr["key"]: attr["value"] for attr in attrs}

    async def save_asset_attributes(
        self,
        asset_id: str,
        attributes: Dict[str, Any],
        scope: str = "SERVER_SCOPE"
    ) -> bool:
        """Save attributes to an asset."""
        await self._get_token()

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/api/plugins/telemetry/ASSET/{asset_id}/attributes/{scope}",
                headers=self._headers(),
                json=attributes
            )
            response.raise_for_status()
            return True

    async def create_alarm(
        self,
        asset_id: str,
        alarm_type: str,
        severity: str,
        details: Dict[str, Any] = None
    ) -> Dict:
        """
        Create an alarm on an asset.

        Args:
            asset_id: Asset ID
            alarm_type: Type of alarm (e.g., "PREDICTED_FAILURE")
            severity: Severity level (CRITICAL, MAJOR, MINOR, WARNING, INDETERMINATE)
            details: Additional alarm details

        Returns:
            Created alarm object
        """
        await self._get_token()

        alarm_data = {
            "originator": {
                "entityType": "ASSET",
                "id": asset_id
            },
            "type": alarm_type,
            "severity": severity,
            "status": "ACTIVE_UNACK",
            "details": details or {}
        }

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/api/alarm",
                headers=self._headers(),
                json=alarm_data
            )
            response.raise_for_status()
            return response.json()

    async def import_rule_chain(self, rule_chain_json: Dict) -> Dict:
        """
        Import a rule chain from JSON.

        Args:
            rule_chain_json: Rule chain configuration

        Returns:
            Imported rule chain object
        """
        await self._get_token()

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/api/ruleChain/import",
                headers=self._headers(),
                json=rule_chain_json
            )
            response.raise_for_status()
            return response.json()

    async def get_rule_chains(self) -> List[Dict]:
        """Get all rule chains."""
        await self._get_token()

        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/api/ruleChains",
                headers=self._headers(),
                params={"pageSize": 100, "page": 0}
            )
            response.raise_for_status()
            return response.json().get("data", [])


# Telemetry keys for different lift system types
ESP_TELEMETRY_KEYS = [
    "pump_intake_pressure_psi",
    "pump_discharge_pressure_psi",
    "pump_intake_temperature_f",
    "motor_current_amps",
    "motor_voltage_v",
    "motor_temperature_f",
    "motor_winding_temperature_f",
    "vibration_x_in_s",
    "vibration_y_in_s",
    "frequency_hz",
    "flow_rate_bpd",
    "gas_oil_ratio",
    "water_cut_percent",
    "casing_pressure_psi",
    "tubing_pressure_psi",
    "wellhead_pressure_psi",
    "wellhead_temperature_f",
    "liquid_level_ft",
    "power_consumption_kw"
]

PCP_TELEMETRY_KEYS = [
    "torque_ft_lb",
    "rpm",
    "pump_speed_rpm",
    "motor_current_amps",
    "motor_temperature_f",
    "flow_rate_bpd",
    "pump_intake_pressure_psi",
    "casing_pressure_psi",
    "tubing_pressure_psi",
    "water_cut_percent",
    "gas_oil_ratio"
]

ROD_PUMP_TELEMETRY_KEYS = [
    "spm",
    "stroke_length_in",
    "peak_polished_rod_load_lb",
    "min_polished_rod_load_lb",
    "motor_current_amps",
    "flow_rate_bpd",
    "pump_fillage_percent",
    "casing_pressure_psi",
    "tubing_pressure_psi",
    "water_cut_percent",
    "gas_oil_ratio"
]

GAS_LIFT_TELEMETRY_KEYS = [
    "gas_injection_rate_mcfd",
    "gas_injection_pressure_psi",
    "tubing_pressure_psi",
    "casing_pressure_psi",
    "flow_rate_bpd",
    "water_cut_percent",
    "gas_oil_ratio",
    "wellhead_temperature_f"
]


def get_telemetry_keys_for_lift_type(lift_type: str) -> List[str]:
    """Get the appropriate telemetry keys for a lift system type."""
    keys_map = {
        "ESP": ESP_TELEMETRY_KEYS,
        "PCP": PCP_TELEMETRY_KEYS,
        "ROD_PUMP": ROD_PUMP_TELEMETRY_KEYS,
        "BEAM": ROD_PUMP_TELEMETRY_KEYS,
        "GAS_LIFT": GAS_LIFT_TELEMETRY_KEYS
    }
    return keys_map.get(lift_type.upper(), ESP_TELEMETRY_KEYS)
