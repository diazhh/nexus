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

import * as fs from 'fs';
import * as path from 'path';
import * as yaml from 'yaml';

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

export class ConfigLoader {
  /**
   * Load configuration from YAML file
   */
  static loadFromFile(filePath: string): SimulatorConfig {
    try {
      const absolutePath = path.isAbsolute(filePath)
        ? filePath
        : path.resolve(process.cwd(), filePath);

      if (!fs.existsSync(absolutePath)) {
        throw new Error(`Config file not found: ${absolutePath}`);
      }

      const fileContent = fs.readFileSync(absolutePath, 'utf8');
      const config = yaml.parse(fileContent) as SimulatorConfig;

      console.log(`✅ Loaded config from: ${absolutePath}`);
      return config;
    } catch (error) {
      console.error(`❌ Failed to load config:`, error);
      throw error;
    }
  }

  /**
   * Load configuration from environment or file
   */
  static load(): SimulatorConfig {
    const configFile = process.env.CONFIG_FILE || './config.yaml';
    return this.loadFromFile(configFile);
  }

  /**
   * Get MQTT broker URL from config or environment
   */
  static getMqttBroker(config?: SimulatorConfig): string {
    return process.env.MQTT_BROKER || config?.mqtt?.broker_url || 'tcp://localhost:1883';
  }
}
