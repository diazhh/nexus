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
package org.thingsboard.nexus.pf.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.pf.dto.TelemetryDataDto;
import org.thingsboard.nexus.pf.service.PfTelemetryService;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MQTT Connector Service for SCADA/field device integration.
 * Manages MQTT connections and processes incoming telemetry.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MqttConnectorService {

    private final PfTelemetryService telemetryService;
    private final ObjectMapper objectMapper;

    // Active MQTT clients by connection ID
    private final Map<UUID, MqttClient> activeClients = new ConcurrentHashMap<>();

    // Connection configurations
    private final Map<UUID, MqttConnectionConfig> configurations = new ConcurrentHashMap<>();

    // Connection status
    private final Map<UUID, ConnectionStatus> connectionStatus = new ConcurrentHashMap<>();

    // Metrics
    private final Map<UUID, ConnectionMetrics> metrics = new ConcurrentHashMap<>();

    // Scheduler for reconnection attempts
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Pattern to extract entity ID from topic
    // Default pattern: v1/devices/{entityId}/telemetry
    private static final Pattern DEFAULT_TOPIC_PATTERN = Pattern.compile("v1/devices/([^/]+)/telemetry");

    /**
     * Establishes MQTT connection based on configuration.
     */
    public void connect(MqttConnectionConfig config) {
        UUID connectionId = config.getId();

        if (activeClients.containsKey(connectionId)) {
            log.warn("Connection {} already exists, disconnecting first", connectionId);
            disconnect(connectionId);
        }

        configurations.put(connectionId, config);
        connectionStatus.put(connectionId, ConnectionStatus.CONNECTING);
        metrics.put(connectionId, new ConnectionMetrics());

        try {
            String brokerUrl = buildBrokerUrl(config);
            MqttConnectOptions options = buildConnectOptions(config);

            MqttClient client = new MqttClient(brokerUrl, config.getClientId(), new MemoryPersistence());

            // Set callback for message handling
            client.setCallback(new MqttMessageCallback(connectionId, config));

            // Connect
            client.connect(options);
            activeClients.put(connectionId, client);

            // Subscribe to topic
            client.subscribe(config.getTopicPattern(), config.getQos());

            connectionStatus.put(connectionId, ConnectionStatus.CONNECTED);
            log.info("MQTT connection established: {} to {}", config.getName(), brokerUrl);

        } catch (MqttException e) {
            log.error("Failed to connect to MQTT broker: {}", config.getName(), e);
            connectionStatus.put(connectionId, ConnectionStatus.ERROR);

            if (config.isAutoReconnect()) {
                scheduleReconnect(config);
            }
        }
    }

    /**
     * Disconnects MQTT connection.
     */
    public void disconnect(UUID connectionId) {
        MqttClient client = activeClients.remove(connectionId);
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
                connectionStatus.put(connectionId, ConnectionStatus.DISCONNECTED);
                log.info("MQTT connection disconnected: {}", connectionId);
            } catch (MqttException e) {
                log.error("Error disconnecting MQTT client: {}", connectionId, e);
            }
        }
    }

    /**
     * Disconnects all active connections.
     */
    @PreDestroy
    public void disconnectAll() {
        log.info("Disconnecting all MQTT connections...");
        new ArrayList<>(activeClients.keySet()).forEach(this::disconnect);
        scheduler.shutdown();
    }

    /**
     * Gets connection status.
     */
    public ConnectionStatus getConnectionStatus(UUID connectionId) {
        return connectionStatus.getOrDefault(connectionId, ConnectionStatus.UNKNOWN);
    }

    /**
     * Gets all active connections.
     */
    public List<MqttConnectionInfo> getActiveConnections() {
        List<MqttConnectionInfo> connections = new ArrayList<>();
        for (Map.Entry<UUID, MqttConnectionConfig> entry : configurations.entrySet()) {
            UUID id = entry.getKey();
            MqttConnectionConfig config = entry.getValue();
            ConnectionMetrics m = metrics.getOrDefault(id, new ConnectionMetrics());

            connections.add(MqttConnectionInfo.builder()
                    .connectionId(id)
                    .name(config.getName())
                    .brokerHost(config.getBrokerHost())
                    .status(connectionStatus.getOrDefault(id, ConnectionStatus.UNKNOWN))
                    .messagesReceived(m.messagesReceived)
                    .messagesProcessed(m.messagesProcessed)
                    .errors(m.errors)
                    .lastMessageTime(m.lastMessageTime)
                    .build());
        }
        return connections;
    }

    /**
     * Tests connection to broker.
     */
    public boolean testConnection(MqttConnectionConfig config) {
        try {
            String brokerUrl = buildBrokerUrl(config);
            MqttConnectOptions options = buildConnectOptions(config);
            options.setConnectionTimeout(5); // Short timeout for test

            MqttClient testClient = new MqttClient(brokerUrl,
                    "test-" + UUID.randomUUID().toString().substring(0, 8),
                    new MemoryPersistence());

            testClient.connect(options);
            testClient.disconnect();
            testClient.close();

            return true;
        } catch (MqttException e) {
            log.warn("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    // Helper methods

    private String buildBrokerUrl(MqttConnectionConfig config) {
        String protocol = config.isSslEnabled() ? "ssl" : "tcp";
        return String.format("%s://%s:%d", protocol, config.getBrokerHost(), config.getBrokerPort());
    }

    private MqttConnectOptions buildConnectOptions(MqttConnectionConfig config) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(config.isCleanSession());
        options.setConnectionTimeout(config.getConnectionTimeoutSeconds());
        options.setKeepAliveInterval(config.getKeepAliveSeconds());
        options.setAutomaticReconnect(false); // We handle reconnect ourselves

        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            options.setUserName(config.getUsername());
        }
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            options.setPassword(config.getPassword().toCharArray());
        }

        return options;
    }

    private void scheduleReconnect(MqttConnectionConfig config) {
        int delay = Math.min(config.getMaxReconnectDelaySeconds(), 10); // Start with 10 seconds
        scheduler.schedule(() -> {
            log.info("Attempting reconnect for: {}", config.getName());
            connect(config);
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * Processes incoming MQTT message.
     */
    private void processMessage(UUID connectionId, String topic, MqttMessage message) {
        ConnectionMetrics m = metrics.get(connectionId);
        if (m != null) {
            m.messagesReceived++;
            m.lastMessageTime = System.currentTimeMillis();
        }

        try {
            MqttConnectionConfig config = configurations.get(connectionId);

            // Parse topic to extract entity ID
            UUID entityId = extractEntityIdFromTopic(topic);
            if (entityId == null) {
                log.warn("Could not extract entity ID from topic: {}", topic);
                return;
            }

            // Parse payload
            String payload = new String(message.getPayload());
            JsonNode jsonPayload = objectMapper.readTree(payload);

            // Extract timestamp
            Long timestamp = jsonPayload.has("ts") ?
                    jsonPayload.get("ts").asLong() : System.currentTimeMillis();

            // Extract values
            Map<String, Object> values = new HashMap<>();
            JsonNode valuesNode = jsonPayload.has("values") ?
                    jsonPayload.get("values") : jsonPayload;

            valuesNode.fields().forEachRemaining(field -> {
                String key = field.getKey();
                if (!"ts".equals(key)) {
                    JsonNode valueNode = field.getValue();
                    if (valueNode.isNumber()) {
                        values.put(key, valueNode.doubleValue());
                    } else if (valueNode.isBoolean()) {
                        values.put(key, valueNode.booleanValue());
                    } else {
                        values.put(key, valueNode.asText());
                    }
                }
            });

            // Create telemetry DTO
            TelemetryDataDto telemetryData = TelemetryDataDto.builder()
                    .entityId(entityId)
                    .timestamp(timestamp)
                    .values(values)
                    .build();

            // Process through telemetry service
            telemetryService.processTelemetry(config.getTenantId(), telemetryData);

            if (m != null) {
                m.messagesProcessed++;
            }

            log.debug("Processed MQTT message: topic={}, entityId={}, keys={}",
                    topic, entityId, values.keySet());

        } catch (Exception e) {
            log.error("Error processing MQTT message from topic {}: {}", topic, e.getMessage());
            if (m != null) {
                m.errors++;
            }
        }
    }

    private UUID extractEntityIdFromTopic(String topic) {
        // Try default pattern first
        Matcher matcher = DEFAULT_TOPIC_PATTERN.matcher(topic);
        if (matcher.find()) {
            String idStr = matcher.group(1);
            try {
                return UUID.fromString(idStr);
            } catch (IllegalArgumentException e) {
                // Not a UUID, might be a device name - would need lookup
                log.debug("Entity ID in topic is not a UUID: {}", idStr);
            }
        }

        // Try to extract from last segment before "telemetry"
        String[] segments = topic.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if ("telemetry".equals(segments[i + 1])) {
                try {
                    return UUID.fromString(segments[i]);
                } catch (IllegalArgumentException e) {
                    // Not a UUID
                }
            }
        }

        return null;
    }

    /**
     * MQTT callback handler for a specific connection.
     */
    private class MqttMessageCallback implements MqttCallbackExtended {

        private final UUID connectionId;
        private final MqttConnectionConfig config;

        MqttMessageCallback(UUID connectionId, MqttConnectionConfig config) {
            this.connectionId = connectionId;
            this.config = config;
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            connectionStatus.put(connectionId, ConnectionStatus.CONNECTED);
            log.info("MQTT connection complete: {} (reconnect={})", config.getName(), reconnect);
        }

        @Override
        public void connectionLost(Throwable cause) {
            connectionStatus.put(connectionId, ConnectionStatus.DISCONNECTED);
            log.warn("MQTT connection lost: {} - {}", config.getName(), cause.getMessage());

            if (config.isAutoReconnect()) {
                scheduleReconnect(config);
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            processMessage(connectionId, topic, message);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // Not used for subscriptions
        }
    }

    /**
     * Connection status enum.
     */
    public enum ConnectionStatus {
        UNKNOWN,
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        ERROR
    }

    /**
     * Connection metrics.
     */
    private static class ConnectionMetrics {
        long messagesReceived = 0;
        long messagesProcessed = 0;
        long errors = 0;
        Long lastMessageTime = null;
    }

    /**
     * Connection info DTO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MqttConnectionInfo {
        private UUID connectionId;
        private String name;
        private String brokerHost;
        private ConnectionStatus status;
        private long messagesReceived;
        private long messagesProcessed;
        private long errors;
        private Long lastMessageTime;
    }
}
