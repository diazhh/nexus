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
package org.thingsboard.nexus.po.service.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.po.dto.ml.PoMlConfigDto;
import org.thingsboard.nexus.po.model.PoMlConfig;
import org.thingsboard.nexus.po.repository.PoMlConfigRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing ML configuration per tenant.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PoMlConfigService {

    private final PoMlConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    // Default anomaly features
    private static final List<String> DEFAULT_ANOMALY_FEATURES = Arrays.asList(
            "motor_temp_f", "vibration_g", "current_amps", "intake_pressure_psi"
    );

    /**
     * Get ML configuration for tenant.
     * Creates default config if not exists.
     */
    @Transactional
    public PoMlConfigDto getConfig(UUID tenantId) {
        PoMlConfig config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultConfig(tenantId));
        return toDto(config);
    }

    /**
     * Save ML configuration.
     */
    @Transactional
    public PoMlConfigDto saveConfig(UUID tenantId, PoMlConfigDto dto) {
        PoMlConfig config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    PoMlConfig newConfig = new PoMlConfig();
                    newConfig.setId(UUID.randomUUID());
                    newConfig.setTenantId(tenantId);
                    newConfig.setCreatedTime(System.currentTimeMillis());
                    return newConfig;
                });

        updateFromDto(config, dto);
        config.setUpdatedTime(System.currentTimeMillis());

        PoMlConfig saved = configRepository.save(config);
        log.info("Saved ML config for tenant: {}", tenantId);
        return toDto(saved);
    }

    /**
     * Reset configuration to defaults.
     */
    @Transactional
    public PoMlConfigDto resetToDefaults(UUID tenantId) {
        configRepository.deleteByTenantId(tenantId);
        PoMlConfig config = createDefaultConfig(tenantId);
        log.info("Reset ML config to defaults for tenant: {}", tenantId);
        return toDto(config);
    }

    /**
     * Create default configuration for a new tenant.
     */
    private PoMlConfig createDefaultConfig(UUID tenantId) {
        PoMlConfig config = PoMlConfig.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .failureAlertThreshold(new BigDecimal("60.00"))
                .predictionHorizonDays(14)
                .analysisFrequencyHours(1)
                .lookbackDays(7)
                .anomalyContamination(new BigDecimal("0.0500"))
                .anomalyWindowHours(24)
                .anomalyFeatures(createAnomalyFeaturesJson(DEFAULT_ANOMALY_FEATURES))
                .healthWeightMechanical(new BigDecimal("0.40"))
                .healthWeightElectrical(new BigDecimal("0.35"))
                .healthWeightProduction(new BigDecimal("0.15"))
                .healthWeightThermal(new BigDecimal("0.10"))
                .healthThresholdHealthy(80)
                .healthThresholdWarning(60)
                .healthThresholdAtRisk(40)
                .autoEmailEnabled(true)
                .autoEmailThreshold(new BigDecimal("70.00"))
                .autoAlarmEnabled(true)
                .autoWorkOrderEnabled(false)
                .autoPushNotificationEnabled(false)
                .createdTime(System.currentTimeMillis())
                .build();

        return configRepository.save(config);
    }

    private JsonNode createAnomalyFeaturesJson(List<String> features) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        features.forEach(arrayNode::add);
        return arrayNode;
    }

    private void updateFromDto(PoMlConfig config, PoMlConfigDto dto) {
        if (dto.getFailureAlertThreshold() != null) {
            config.setFailureAlertThreshold(dto.getFailureAlertThreshold());
        }
        if (dto.getPredictionHorizonDays() != null) {
            config.setPredictionHorizonDays(dto.getPredictionHorizonDays());
        }
        if (dto.getAnalysisFrequencyHours() != null) {
            config.setAnalysisFrequencyHours(dto.getAnalysisFrequencyHours());
        }
        if (dto.getLookbackDays() != null) {
            config.setLookbackDays(dto.getLookbackDays());
        }
        if (dto.getAnomalyContamination() != null) {
            config.setAnomalyContamination(dto.getAnomalyContamination());
        }
        if (dto.getAnomalyWindowHours() != null) {
            config.setAnomalyWindowHours(dto.getAnomalyWindowHours());
        }
        if (dto.getAnomalyFeatures() != null) {
            config.setAnomalyFeatures(createAnomalyFeaturesJson(dto.getAnomalyFeatures()));
        }
        if (dto.getHealthWeightMechanical() != null) {
            config.setHealthWeightMechanical(dto.getHealthWeightMechanical());
        }
        if (dto.getHealthWeightElectrical() != null) {
            config.setHealthWeightElectrical(dto.getHealthWeightElectrical());
        }
        if (dto.getHealthWeightProduction() != null) {
            config.setHealthWeightProduction(dto.getHealthWeightProduction());
        }
        if (dto.getHealthWeightThermal() != null) {
            config.setHealthWeightThermal(dto.getHealthWeightThermal());
        }
        if (dto.getHealthThresholdHealthy() != null) {
            config.setHealthThresholdHealthy(dto.getHealthThresholdHealthy());
        }
        if (dto.getHealthThresholdWarning() != null) {
            config.setHealthThresholdWarning(dto.getHealthThresholdWarning());
        }
        if (dto.getHealthThresholdAtRisk() != null) {
            config.setHealthThresholdAtRisk(dto.getHealthThresholdAtRisk());
        }
        if (dto.getAutoEmailEnabled() != null) {
            config.setAutoEmailEnabled(dto.getAutoEmailEnabled());
        }
        if (dto.getAutoEmailThreshold() != null) {
            config.setAutoEmailThreshold(dto.getAutoEmailThreshold());
        }
        if (dto.getAutoAlarmEnabled() != null) {
            config.setAutoAlarmEnabled(dto.getAutoAlarmEnabled());
        }
        if (dto.getAutoWorkOrderEnabled() != null) {
            config.setAutoWorkOrderEnabled(dto.getAutoWorkOrderEnabled());
        }
        if (dto.getAutoPushNotificationEnabled() != null) {
            config.setAutoPushNotificationEnabled(dto.getAutoPushNotificationEnabled());
        }
    }

    private PoMlConfigDto toDto(PoMlConfig config) {
        List<String> anomalyFeatures = new ArrayList<>();
        if (config.getAnomalyFeatures() != null && config.getAnomalyFeatures().isArray()) {
            config.getAnomalyFeatures().forEach(node -> anomalyFeatures.add(node.asText()));
        }

        return PoMlConfigDto.builder()
                .id(config.getId())
                .tenantId(config.getTenantId())
                .failureAlertThreshold(config.getFailureAlertThreshold())
                .predictionHorizonDays(config.getPredictionHorizonDays())
                .analysisFrequencyHours(config.getAnalysisFrequencyHours())
                .lookbackDays(config.getLookbackDays())
                .anomalyContamination(config.getAnomalyContamination())
                .anomalyWindowHours(config.getAnomalyWindowHours())
                .anomalyFeatures(anomalyFeatures)
                .healthWeightMechanical(config.getHealthWeightMechanical())
                .healthWeightElectrical(config.getHealthWeightElectrical())
                .healthWeightProduction(config.getHealthWeightProduction())
                .healthWeightThermal(config.getHealthWeightThermal())
                .healthThresholdHealthy(config.getHealthThresholdHealthy())
                .healthThresholdWarning(config.getHealthThresholdWarning())
                .healthThresholdAtRisk(config.getHealthThresholdAtRisk())
                .autoEmailEnabled(config.getAutoEmailEnabled())
                .autoEmailThreshold(config.getAutoEmailThreshold())
                .autoAlarmEnabled(config.getAutoAlarmEnabled())
                .autoWorkOrderEnabled(config.getAutoWorkOrderEnabled())
                .autoPushNotificationEnabled(config.getAutoPushNotificationEnabled())
                .createdTime(config.getCreatedTime())
                .updatedTime(config.getUpdatedTime())
                .build();
    }
}
