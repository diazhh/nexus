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
 * MQTT Client for ThingsBoard telemetry publishing
 */
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.MqttClient = void 0;
const mqtt_1 = __importDefault(require("mqtt"));
class MqttClient {
    client = null;
    config;
    connected = false;
    constructor(config) {
        this.config = {
            reconnectPeriod: 5000,
            keepalive: 60,
            ...config
        };
    }
    async connect() {
        return new Promise((resolve, reject) => {
            console.log(`Connecting to MQTT broker: ${this.config.brokerUrl}`);
            this.client = mqtt_1.default.connect(this.config.brokerUrl, {
                username: this.config.accessToken,
                clientId: this.config.clientId || `nexus-sim-${Date.now()}`,
                reconnectPeriod: this.config.reconnectPeriod,
                keepalive: this.config.keepalive,
                clean: true
            });
            this.client.on('connect', () => {
                this.connected = true;
                console.log('✅ MQTT connected successfully');
                resolve();
            });
            this.client.on('error', (err) => {
                console.error('❌ MQTT connection error:', err.message);
                reject(err);
            });
            this.client.on('reconnect', () => {
                console.log('🔄 MQTT reconnecting...');
            });
            this.client.on('offline', () => {
                this.connected = false;
                console.warn('⚠️  MQTT offline');
            });
            this.client.on('close', () => {
                this.connected = false;
                console.log('MQTT connection closed');
            });
        });
    }
    publishTelemetry(data) {
        if (!this.client || !this.connected) {
            console.warn('⚠️  Cannot publish: MQTT not connected');
            return;
        }
        const topic = 'v1/devices/me/telemetry';
        const payload = JSON.stringify(data);
        this.client.publish(topic, payload, { qos: 1 }, (err) => {
            if (err) {
                console.error('❌ Failed to publish telemetry:', err.message);
            }
        });
    }
    publishAttributes(data) {
        if (!this.client || !this.connected) {
            console.warn('⚠️  Cannot publish: MQTT not connected');
            return;
        }
        const topic = 'v1/devices/me/attributes';
        const payload = JSON.stringify(data);
        this.client.publish(topic, payload, { qos: 1 }, (err) => {
            if (err) {
                console.error('❌ Failed to publish attributes:', err.message);
            }
        });
    }
    isConnected() {
        return this.connected;
    }
    async disconnect() {
        return new Promise((resolve) => {
            if (this.client) {
                this.client.end(false, {}, () => {
                    console.log('MQTT disconnected');
                    resolve();
                });
            }
            else {
                resolve();
            }
        });
    }
}
exports.MqttClient = MqttClient;
//# sourceMappingURL=mqtt-client.js.map