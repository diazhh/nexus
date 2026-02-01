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
 * MQTT Client for ThingsBoard telemetry publishing
 */
export interface MqttClientConfig {
    brokerUrl: string;
    accessToken: string;
    reconnectPeriod?: number;
    keepalive?: number;
    clientId?: string;
}
export declare class MqttClient {
    private client;
    private config;
    private connected;
    constructor(config: MqttClientConfig);
    connect(): Promise<void>;
    publishTelemetry(data: Record<string, any>): void;
    publishAttributes(data: Record<string, any>): void;
    isConnected(): boolean;
    disconnect(): Promise<void>;
}
//# sourceMappingURL=mqtt-client.d.ts.map