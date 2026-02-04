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
package org.thingsboard.nexus.pf.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.thingsboard.nexus.pf.dto.TelemetryDataDto;
import org.thingsboard.nexus.pf.dto.PfAlarmDto;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket handler for real-time telemetry and alarm updates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PfWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    // Sessions by session ID
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // Subscriptions: entityId -> Set of session IDs
    private final Map<UUID, Set<String>> telemetrySubscriptions = new ConcurrentHashMap<>();

    // Alarm subscriptions: tenantId -> Set of session IDs
    private final Map<UUID, Set<String>> alarmSubscriptions = new ConcurrentHashMap<>();

    // Session metadata
    private final Map<String, SessionMetadata> sessionMetadata = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        sessionMetadata.put(session.getId(), new SessionMetadata());
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);

        // Remove from all subscriptions
        telemetrySubscriptions.values().forEach(subs -> subs.remove(sessionId));
        alarmSubscriptions.values().forEach(subs -> subs.remove(sessionId));
        sessionMetadata.remove(sessionId);

        log.info("WebSocket connection closed: {} (status: {})", sessionId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            WebSocketCommand command = objectMapper.readValue(message.getPayload(), WebSocketCommand.class);
            handleCommand(session, command);
        } catch (Exception e) {
            log.error("Error processing WebSocket message: {}", e.getMessage());
            sendError(session, "Invalid message format");
        }
    }

    /**
     * Handles incoming WebSocket commands.
     */
    private void handleCommand(WebSocketSession session, WebSocketCommand command) {
        switch (command.getType()) {
            case "SUBSCRIBE_TELEMETRY" -> subscribeTelemetry(session, command);
            case "UNSUBSCRIBE_TELEMETRY" -> unsubscribeTelemetry(session, command);
            case "SUBSCRIBE_ALARMS" -> subscribeAlarms(session, command);
            case "UNSUBSCRIBE_ALARMS" -> unsubscribeAlarms(session, command);
            case "PING" -> sendPong(session);
            default -> sendError(session, "Unknown command type: " + command.getType());
        }
    }

    private void subscribeTelemetry(WebSocketSession session, WebSocketCommand command) {
        if (command.getEntityIds() == null || command.getEntityIds().isEmpty()) {
            sendError(session, "entityIds required for telemetry subscription");
            return;
        }

        for (UUID entityId : command.getEntityIds()) {
            telemetrySubscriptions.computeIfAbsent(entityId, k -> ConcurrentHashMap.newKeySet())
                    .add(session.getId());
        }

        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata != null) {
            metadata.subscribedEntities.addAll(command.getEntityIds());
        }

        sendAck(session, "Subscribed to telemetry for " + command.getEntityIds().size() + " entities");
        log.debug("Session {} subscribed to telemetry for entities: {}",
                session.getId(), command.getEntityIds());
    }

    private void unsubscribeTelemetry(WebSocketSession session, WebSocketCommand command) {
        if (command.getEntityIds() != null) {
            for (UUID entityId : command.getEntityIds()) {
                Set<String> subs = telemetrySubscriptions.get(entityId);
                if (subs != null) {
                    subs.remove(session.getId());
                }
            }

            SessionMetadata metadata = sessionMetadata.get(session.getId());
            if (metadata != null) {
                metadata.subscribedEntities.removeAll(command.getEntityIds());
            }
        }

        sendAck(session, "Unsubscribed from telemetry");
    }

    private void subscribeAlarms(WebSocketSession session, WebSocketCommand command) {
        UUID tenantId = command.getTenantId();
        if (tenantId == null) {
            sendError(session, "tenantId required for alarm subscription");
            return;
        }

        alarmSubscriptions.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet())
                .add(session.getId());

        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata != null) {
            metadata.tenantId = tenantId;
            metadata.subscribedToAlarms = true;
        }

        sendAck(session, "Subscribed to alarms");
        log.debug("Session {} subscribed to alarms for tenant: {}", session.getId(), tenantId);
    }

    private void unsubscribeAlarms(WebSocketSession session, WebSocketCommand command) {
        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata != null && metadata.tenantId != null) {
            Set<String> subs = alarmSubscriptions.get(metadata.tenantId);
            if (subs != null) {
                subs.remove(session.getId());
            }
            metadata.subscribedToAlarms = false;
        }

        sendAck(session, "Unsubscribed from alarms");
    }

    /**
     * Broadcasts telemetry update to subscribed clients.
     */
    public void broadcastTelemetry(TelemetryDataDto telemetry) {
        Set<String> subscribedSessions = telemetrySubscriptions.get(telemetry.getEntityId());
        if (subscribedSessions == null || subscribedSessions.isEmpty()) {
            return;
        }

        WebSocketMessage message = WebSocketMessage.builder()
                .type("TELEMETRY_UPDATE")
                .entityId(telemetry.getEntityId())
                .timestamp(telemetry.getTimestamp())
                .data(telemetry.getValues())
                .build();

        broadcast(subscribedSessions, message);
    }

    /**
     * Broadcasts alarm to subscribed clients.
     */
    public void broadcastAlarm(PfAlarmDto alarm) {
        // Get sessions subscribed to this tenant's alarms
        Set<String> subscribedSessions = alarmSubscriptions.get(alarm.getTenantId());
        if (subscribedSessions == null || subscribedSessions.isEmpty()) {
            return;
        }

        WebSocketMessage message = WebSocketMessage.builder()
                .type("ALARM_" + alarm.getStatus())
                .entityId(alarm.getEntityId())
                .timestamp(alarm.getStartTime())
                .data(Map.of(
                        "alarmId", alarm.getId(),
                        "alarmType", alarm.getAlarmType(),
                        "severity", alarm.getSeverity(),
                        "status", alarm.getStatus(),
                        "message", alarm.getMessage()
                ))
                .build();

        broadcast(subscribedSessions, message);
    }

    /**
     * Broadcasts data quality alert.
     */
    public void broadcastDataQualityAlert(UUID entityId, double qualityScore, List<String> issues) {
        Set<String> subscribedSessions = telemetrySubscriptions.get(entityId);
        if (subscribedSessions == null || subscribedSessions.isEmpty()) {
            return;
        }

        WebSocketMessage message = WebSocketMessage.builder()
                .type("DATA_QUALITY_ALERT")
                .entityId(entityId)
                .timestamp(System.currentTimeMillis())
                .data(Map.of(
                        "qualityScore", qualityScore,
                        "issues", issues
                ))
                .build();

        broadcast(subscribedSessions, message);
    }

    private void broadcast(Set<String> sessionIds, WebSocketMessage message) {
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error serializing WebSocket message: {}", e.getMessage());
            return;
        }

        TextMessage textMessage = new TextMessage(jsonMessage);

        List<String> failedSessions = new ArrayList<>();

        for (String sessionId : sessionIds) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.warn("Failed to send message to session {}: {}", sessionId, e.getMessage());
                    failedSessions.add(sessionId);
                }
            } else {
                failedSessions.add(sessionId);
            }
        }

        // Clean up failed sessions
        sessionIds.removeAll(failedSessions);
    }

    private void sendAck(WebSocketSession session, String message) {
        sendMessage(session, WebSocketMessage.builder()
                .type("ACK")
                .timestamp(System.currentTimeMillis())
                .data(Map.of("message", message))
                .build());
    }

    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, WebSocketMessage.builder()
                .type("ERROR")
                .timestamp(System.currentTimeMillis())
                .data(Map.of("error", error))
                .build());
    }

    private void sendPong(WebSocketSession session) {
        sendMessage(session, WebSocketMessage.builder()
                .type("PONG")
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("Error sending WebSocket message: {}", e.getMessage());
        }
    }

    /**
     * Gets statistics about WebSocket connections.
     */
    public WebSocketStats getStats() {
        return WebSocketStats.builder()
                .activeSessions(sessions.size())
                .telemetrySubscriptions(telemetrySubscriptions.values().stream()
                        .mapToInt(Set::size).sum())
                .alarmSubscriptions(alarmSubscriptions.values().stream()
                        .mapToInt(Set::size).sum())
                .uniqueEntitiesSubscribed(telemetrySubscriptions.size())
                .build();
    }

    // Inner classes

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WebSocketCommand {
        private String type;
        private UUID tenantId;
        private List<UUID> entityIds;
        private Map<String, Object> params;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WebSocketMessage {
        private String type;
        private UUID entityId;
        private Long timestamp;
        private Object data;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WebSocketStats {
        private int activeSessions;
        private int telemetrySubscriptions;
        private int alarmSubscriptions;
        private int uniqueEntitiesSubscribed;
    }

    private static class SessionMetadata {
        UUID tenantId;
        Set<UUID> subscribedEntities = ConcurrentHashMap.newKeySet();
        boolean subscribedToAlarms = false;
    }
}
