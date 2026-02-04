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
ML Models Package
"""
from . import schemas
from .failure_prediction import FailurePredictionModel, prepare_training_data as prepare_failure_data
from .anomaly_detection import AnomalyDetectionModel, prepare_training_data as prepare_anomaly_data

__all__ = [
    "schemas",
    "FailurePredictionModel",
    "AnomalyDetectionModel",
    "prepare_failure_data",
    "prepare_anomaly_data"
]
