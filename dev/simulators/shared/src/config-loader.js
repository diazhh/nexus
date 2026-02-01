/*
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";
/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Configuration loader for simulators
 */
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.ConfigLoader = void 0;
const fs = __importStar(require("fs"));
const path = __importStar(require("path"));
const yaml = __importStar(require("yaml"));
class ConfigLoader {
    /**
     * Load configuration from YAML file
     */
    static loadFromFile(filePath) {
        try {
            const absolutePath = path.isAbsolute(filePath)
                ? filePath
                : path.resolve(process.cwd(), filePath);
            if (!fs.existsSync(absolutePath)) {
                throw new Error(`Config file not found: ${absolutePath}`);
            }
            const fileContent = fs.readFileSync(absolutePath, 'utf8');
            const config = yaml.parse(fileContent);
            console.log(`✅ Loaded config from: ${absolutePath}`);
            return config;
        }
        catch (error) {
            console.error(`❌ Failed to load config:`, error);
            throw error;
        }
    }
    /**
     * Load configuration from environment or file
     */
    static load() {
        const configFile = process.env.CONFIG_FILE || './config.yaml';
        return this.loadFromFile(configFile);
    }
    /**
     * Get MQTT broker URL from config or environment
     */
    static getMqttBroker(config) {
        return process.env.MQTT_BROKER || config?.mqtt?.broker_url || 'tcp://localhost:1883';
    }
}
exports.ConfigLoader = ConfigLoader;
//# sourceMappingURL=config-loader.js.map