///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Configuration loader for simulators
 */
export interface MqttConfig {
    broker_url: string;
    reconnect_period_ms?: number;
}
export interface SimulationConfig {
    rate_hz: number;
    realtime_factor: number;
}
export interface SimulatorConfig {
    mqtt: MqttConfig;
    simulation: SimulationConfig;
    [key: string]: any;
}
export declare class ConfigLoader {
    /**
     * Load configuration from YAML file
     */
    static loadFromFile(filePath: string): SimulatorConfig;
    /**
     * Load configuration from environment or file
     */
    static load(): SimulatorConfig;
    /**
     * Get MQTT broker URL from config or environment
     */
    static getMqttBroker(config?: SimulatorConfig): string;
}
//# sourceMappingURL=config-loader.d.ts.map