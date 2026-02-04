/**
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
package org.thingsboard.nexus.pf.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Configuration for MQTT connection to SCADA/field devices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttConnectionConfig {

    /**
     * Unique identifier for this connection
     */
    private UUID id;

    /**
     * Connection name (descriptive)
     */
    private String name;

    /**
     * MQTT broker host
     */
    private String brokerHost;

    /**
     * MQTT broker port (default: 1883, SSL: 8883)
     */
    @Builder.Default
    private int brokerPort = 1883;

    /**
     * Client ID for MQTT connection
     */
    private String clientId;

    /**
     * Username for authentication
     */
    private String username;

    /**
     * Password for authentication
     */
    private String password;

    /**
     * Enable SSL/TLS
     */
    @Builder.Default
    private boolean sslEnabled = false;

    /**
     * Topic pattern for subscription (supports wildcards: + and #)
     * Example: "v1/devices/+/telemetry"
     */
    private String topicPattern;

    /**
     * QoS level (0, 1, or 2)
     */
    @Builder.Default
    private int qos = 1;

    /**
     * Keep alive interval in seconds
     */
    @Builder.Default
    private int keepAliveSeconds = 60;

    /**
     * Connection timeout in seconds
     */
    @Builder.Default
    private int connectionTimeoutSeconds = 30;

    /**
     * Auto reconnect on disconnect
     */
    @Builder.Default
    private boolean autoReconnect = true;

    /**
     * Maximum reconnect delay in seconds
     */
    @Builder.Default
    private int maxReconnectDelaySeconds = 60;

    /**
     * Clean session flag
     */
    @Builder.Default
    private boolean cleanSession = true;

    /**
     * Associated tenant ID
     */
    private UUID tenantId;

    /**
     * Whether this connection is enabled
     */
    @Builder.Default
    private boolean enabled = true;
}
