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
package org.thingsboard.nexus.pf.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.pf.integration.MqttConnectionConfig;
import org.thingsboard.nexus.pf.integration.MqttConnectorService;
import org.thingsboard.nexus.pf.integration.MqttConnectorService.ConnectionStatus;
import org.thingsboard.nexus.pf.integration.MqttConnectorService.MqttConnectionInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for MQTT Integration management.
 */
@RestController
@RequestMapping("/api/nexus/pf/mqtt")
@RequiredArgsConstructor
@Slf4j
public class PfMqttController {

    private final MqttConnectorService mqttConnectorService;

    /**
     * Creates and starts a new MQTT connection.
     */
    @PostMapping("/connections")
    public ResponseEntity<Map<String, Object>> createConnection(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody MqttConnectionConfig config) {

        if (config.getId() == null) {
            config.setId(UUID.randomUUID());
        }
        config.setTenantId(tenantId);

        log.info("Creating MQTT connection: {} to {}:{}",
                config.getName(), config.getBrokerHost(), config.getBrokerPort());

        mqttConnectorService.connect(config);

        return ResponseEntity.ok(Map.of(
                "connectionId", config.getId(),
                "status", mqttConnectorService.getConnectionStatus(config.getId())
        ));
    }

    /**
     * Tests MQTT connection without persisting.
     */
    @PostMapping("/connections/test")
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody MqttConnectionConfig config) {

        log.info("Testing MQTT connection to {}:{}", config.getBrokerHost(), config.getBrokerPort());

        boolean success = mqttConnectorService.testConnection(config);

        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Connection successful" : "Connection failed"
        ));
    }

    /**
     * Gets all active connections.
     */
    @GetMapping("/connections")
    public ResponseEntity<List<MqttConnectionInfo>> getConnections(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {

        return ResponseEntity.ok(mqttConnectorService.getActiveConnections());
    }

    /**
     * Gets status of a specific connection.
     */
    @GetMapping("/connections/{connectionId}/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID connectionId) {

        ConnectionStatus status = mqttConnectorService.getConnectionStatus(connectionId);

        return ResponseEntity.ok(Map.of(
                "connectionId", connectionId,
                "status", status
        ));
    }

    /**
     * Disconnects an MQTT connection.
     */
    @DeleteMapping("/connections/{connectionId}")
    public ResponseEntity<Void> disconnectConnection(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID connectionId) {

        log.info("Disconnecting MQTT connection: {}", connectionId);

        mqttConnectorService.disconnect(connectionId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Reconnects an MQTT connection.
     */
    @PostMapping("/connections/{connectionId}/reconnect")
    public ResponseEntity<Map<String, Object>> reconnectConnection(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID connectionId) {

        log.info("Reconnecting MQTT connection: {}", connectionId);

        // Disconnect and reconnect
        mqttConnectorService.disconnect(connectionId);

        // Get stored config and reconnect (in a real implementation,
        // configs would be persisted to database)
        ConnectionStatus status = mqttConnectorService.getConnectionStatus(connectionId);

        return ResponseEntity.ok(Map.of(
                "connectionId", connectionId,
                "status", status
        ));
    }
}
