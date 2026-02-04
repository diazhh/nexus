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
package org.thingsboard.nexus.pf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.pf.dto.AlarmSeverity;
import org.thingsboard.nexus.pf.dto.PfAlarmDto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for sending notifications about alarms and system events.
 * Supports multiple channels: Email, SMS, Webhook.
 *
 * Note: Real-time WebSocket notifications are handled by ThingsBoard's native
 * TelemetryWebsocketService. Nexus dashboards should use TB's WebSocket API
 * at /api/ws for real-time data subscriptions.
 */
@Service
@Slf4j
public class PfNotificationService {

    // Notification configurations by tenant
    private final Map<UUID, NotificationConfig> tenantConfigs = new ConcurrentHashMap<>();

    // Notification history (for deduplication)
    private final Map<String, Long> notificationHistory = new ConcurrentHashMap<>();

    // Cooldown period in milliseconds (prevent alarm flooding)
    private static final long COOLDOWN_PERIOD_MS = 300000; // 5 minutes

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    /**
     * Sends notification for an alarm.
     */
    @Async
    public void notifyAlarm(PfAlarmDto alarm) {
        // Check cooldown to prevent notification flooding
        String alarmKey = alarm.getEntityId() + ":" + alarm.getAlarmType();
        Long lastNotification = notificationHistory.get(alarmKey);

        if (lastNotification != null &&
                System.currentTimeMillis() - lastNotification < COOLDOWN_PERIOD_MS) {
            log.debug("Skipping notification for {} (cooldown period)", alarmKey);
            return;
        }

        notificationHistory.put(alarmKey, System.currentTimeMillis());

        // Get notification config for tenant
        NotificationConfig config = tenantConfigs.getOrDefault(
                alarm.getTenantId(), NotificationConfig.defaultConfig());

        // Determine which channels to use based on severity
        Set<NotificationChannel> channels = getChannelsForSeverity(config, alarm.getSeverity());

        for (NotificationChannel channel : channels) {
            try {
                switch (channel) {
                    case EMAIL -> sendEmailNotification(alarm, config);
                    case SMS -> sendSmsNotification(alarm, config);
                    case WEBHOOK -> sendWebhookNotification(alarm, config);
                }
            } catch (Exception e) {
                log.error("Failed to send {} notification for alarm {}: {}",
                        channel, alarm.getId(), e.getMessage());
            }
        }
    }

    /**
     * Sends a general notification (not alarm-specific).
     */
    @Async
    public void sendNotification(UUID tenantId, NotificationMessage message) {
        NotificationConfig config = tenantConfigs.getOrDefault(tenantId, NotificationConfig.defaultConfig());

        for (NotificationChannel channel : message.getChannels()) {
            try {
                switch (channel) {
                    case EMAIL -> sendEmail(config, message.getSubject(), message.getBody(), message.getRecipients());
                    case SMS -> sendSms(config, message.getBody(), message.getRecipients());
                    case WEBHOOK -> sendWebhook(config, message.toMap());
                }
            } catch (Exception e) {
                log.error("Failed to send {} notification: {}", channel, e.getMessage());
            }
        }
    }

    /**
     * Sets notification configuration for a tenant.
     */
    public void setTenantConfig(UUID tenantId, NotificationConfig config) {
        tenantConfigs.put(tenantId, config);
        log.info("Updated notification config for tenant {}", tenantId);
    }

    /**
     * Gets notification configuration for a tenant.
     */
    public NotificationConfig getTenantConfig(UUID tenantId) {
        return tenantConfigs.getOrDefault(tenantId, NotificationConfig.defaultConfig());
    }

    // Channel implementations

    private void sendEmailNotification(PfAlarmDto alarm, NotificationConfig config) {
        if (config.getEmailRecipients() == null || config.getEmailRecipients().isEmpty()) {
            return;
        }

        String subject = buildEmailSubject(alarm);
        String body = buildEmailBody(alarm);

        sendEmail(config, subject, body, config.getEmailRecipients());
    }

    private void sendEmail(NotificationConfig config, String subject, String body, List<String> recipients) {
        // In a real implementation, use JavaMailSender or email service
        log.info("Sending email: subject='{}', recipients={}", subject, recipients);

        // Example integration with email service:
        // mailSender.send(message -> {
        //     message.setTo(recipients.toArray(new String[0]));
        //     message.setSubject(subject);
        //     message.setText(body);
        // });
    }

    private void sendSmsNotification(PfAlarmDto alarm, NotificationConfig config) {
        if (config.getSmsRecipients() == null || config.getSmsRecipients().isEmpty()) {
            return;
        }

        String message = buildSmsMessage(alarm);
        sendSms(config, message, config.getSmsRecipients());
    }

    private void sendSms(NotificationConfig config, String message, List<String> recipients) {
        // In a real implementation, use Twilio or SMS gateway
        log.info("Sending SMS: message='{}', recipients={}", message, recipients);

        // Example integration with Twilio:
        // for (String recipient : recipients) {
        //     twilioClient.messages.create(
        //         phoneNumber(recipient),
        //         phoneNumber(config.getSmsFromNumber()),
        //         message
        //     );
        // }
    }

    private void sendWebhookNotification(PfAlarmDto alarm, NotificationConfig config) {
        if (config.getWebhookUrl() == null || config.getWebhookUrl().isEmpty()) {
            return;
        }

        Map<String, Object> payload = buildWebhookPayload(alarm);
        sendWebhook(config, payload);
    }

    private void sendWebhook(NotificationConfig config, Map<String, Object> payload) {
        // In a real implementation, use RestTemplate or WebClient
        log.info("Sending webhook to {}: payload={}", config.getWebhookUrl(), payload);

        // Example integration:
        // restTemplate.postForEntity(config.getWebhookUrl(), payload, Void.class);
    }

    // Message building methods

    private String buildEmailSubject(PfAlarmDto alarm) {
        return String.format("[%s] %s - %s",
                alarm.getSeverity(),
                alarm.getAlarmType().replace("_", " "),
                alarm.getEntityName() != null ? alarm.getEntityName() : alarm.getEntityId());
    }

    private String buildEmailBody(PfAlarmDto alarm) {
        StringBuilder sb = new StringBuilder();

        sb.append("ALARM NOTIFICATION\n");
        sb.append("==================\n\n");

        sb.append("Severity: ").append(alarm.getSeverity()).append("\n");
        sb.append("Type: ").append(alarm.getAlarmType()).append("\n");
        sb.append("Entity: ").append(alarm.getEntityName() != null ?
                alarm.getEntityName() : alarm.getEntityId()).append("\n");
        sb.append("Time: ").append(formatTimestamp(alarm.getStartTime())).append("\n");
        sb.append("\n");

        sb.append("Message:\n");
        sb.append(alarm.getMessage()).append("\n");
        sb.append("\n");

        if (alarm.getRecommendedActions() != null && !alarm.getRecommendedActions().isEmpty()) {
            sb.append("Recommended Actions:\n");
            for (String action : alarm.getRecommendedActions()) {
                sb.append("  • ").append(action).append("\n");
            }
        }

        sb.append("\n---\n");
        sb.append("This is an automated notification from Nexus PF Module.\n");

        return sb.toString();
    }

    private String buildSmsMessage(PfAlarmDto alarm) {
        return String.format("[%s] %s: %s",
                alarm.getSeverity().toString().charAt(0),
                alarm.getEntityName() != null ? alarm.getEntityName() : "Entity",
                truncate(alarm.getMessage(), 100));
    }

    private Map<String, Object> buildWebhookPayload(PfAlarmDto alarm) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "ALARM");
        payload.put("alarmId", alarm.getId());
        payload.put("entityId", alarm.getEntityId());
        payload.put("entityName", alarm.getEntityName());
        payload.put("alarmType", alarm.getAlarmType());
        payload.put("severity", alarm.getSeverity());
        payload.put("status", alarm.getStatus());
        payload.put("message", alarm.getMessage());
        payload.put("startTime", alarm.getStartTime());
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }

    private Set<NotificationChannel> getChannelsForSeverity(NotificationConfig config,
                                                             AlarmSeverity severity) {
        return switch (severity) {
            case CRITICAL -> config.getCriticalChannels();
            case HIGH -> config.getHighChannels();
            case MEDIUM -> config.getMediumChannels();
            case LOW, INFO -> config.getLowChannels();
        };
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return "N/A";
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DATE_FORMATTER);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    // Inner classes

    public enum NotificationChannel {
        EMAIL,
        SMS,
        WEBHOOK
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationConfig {
        private List<String> emailRecipients;
        private List<String> smsRecipients;
        private String webhookUrl;
        private String webhookSecret;
        private String smsFromNumber;

        @lombok.Builder.Default
        private Set<NotificationChannel> criticalChannels =
                EnumSet.of(NotificationChannel.EMAIL, NotificationChannel.SMS);

        @lombok.Builder.Default
        private Set<NotificationChannel> highChannels =
                EnumSet.of(NotificationChannel.EMAIL);

        @lombok.Builder.Default
        private Set<NotificationChannel> mediumChannels =
                EnumSet.noneOf(NotificationChannel.class);

        @lombok.Builder.Default
        private Set<NotificationChannel> lowChannels =
                EnumSet.noneOf(NotificationChannel.class);

        public static NotificationConfig defaultConfig() {
            return NotificationConfig.builder().build();
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationMessage {
        private String subject;
        private String body;
        private List<String> recipients;
        private Set<NotificationChannel> channels;
        private Map<String, Object> metadata;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("subject", subject);
            map.put("body", body);
            map.put("timestamp", System.currentTimeMillis());
            if (metadata != null) {
                map.putAll(metadata);
            }
            return map;
        }
    }
}
