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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.po.dto.ml.PoMlPredictionDto;
import org.thingsboard.nexus.po.dto.ml.PredictionType;
import org.thingsboard.nexus.po.dto.ml.WellPredictionSummaryDto;
import org.thingsboard.nexus.po.exception.PoEntityNotFoundException;
import org.thingsboard.nexus.po.model.PoMlPrediction;
import org.thingsboard.nexus.po.repository.PoMlPredictionRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing ML predictions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PoMlPredictionService {

    private final PoMlPredictionRepository predictionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get the latest prediction for a well by type.
     */
    public PoMlPredictionDto getLatestPrediction(UUID wellAssetId, PredictionType predictionType) {
        return predictionRepository.findLatestByWellAndType(wellAssetId, predictionType)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Get the latest prediction by ID.
     */
    public PoMlPredictionDto getPrediction(UUID predictionId) {
        PoMlPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new PoEntityNotFoundException("MlPrediction", predictionId));
        return toDto(prediction);
    }

    /**
     * Get all predictions for a well.
     */
    public List<PoMlPredictionDto> getWellPredictions(UUID wellAssetId) {
        return predictionRepository.findByWellAssetIdOrderByCreatedTimeDesc(wellAssetId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get high-risk wells for a tenant.
     */
    public List<WellPredictionSummaryDto> getHighRiskWells(UUID tenantId, BigDecimal threshold) {
        List<PoMlPrediction> predictions = predictionRepository.findHighRiskWells(tenantId, threshold);
        return predictions.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get wells with anomalies for a tenant.
     */
    public List<WellPredictionSummaryDto> getAnomalousWells(UUID tenantId) {
        List<PoMlPrediction> predictions = predictionRepository.findAnomalousWells(tenantId);
        return predictions.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get latest failure predictions for all wells of a tenant.
     */
    public List<WellPredictionSummaryDto> getFailurePredictionSummaries(UUID tenantId) {
        List<PoMlPrediction> predictions = predictionRepository.findLatestPredictions(tenantId, PredictionType.FAILURE.name());
        return predictions.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get latest health scores for all wells of a tenant.
     */
    public List<WellPredictionSummaryDto> getHealthScoreSummaries(UUID tenantId) {
        List<PoMlPrediction> predictions = predictionRepository.findLatestPredictions(tenantId, PredictionType.HEALTH_SCORE.name());
        return predictions.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get prediction history for a well.
     */
    public List<PoMlPredictionDto> getPredictionHistory(UUID wellAssetId, PredictionType predictionType,
                                                         Long startTime, Long endTime) {
        return predictionRepository.findHistory(wellAssetId, predictionType, startTime, endTime)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Save a new prediction (called by ML Python service).
     */
    @Transactional
    public PoMlPrediction savePrediction(PoMlPredictionDto dto) {
        PoMlPrediction prediction = PoMlPrediction.builder()
                .id(UUID.randomUUID())
                .tenantId(dto.getTenantId())
                .wellAssetId(dto.getWellAssetId())
                .modelId(dto.getModelId())
                .predictionType(dto.getPredictionType())
                .createdTime(System.currentTimeMillis())
                .probability(dto.getProbability())
                .daysToFailure(dto.getDaysToFailure())
                .healthScore(dto.getHealthScore())
                .healthLevel(dto.getHealthLevel())
                .healthTrend(dto.getHealthTrend())
                .isAnomaly(dto.getIsAnomaly() != null ? dto.getIsAnomaly() : false)
                .anomalyScore(dto.getAnomalyScore())
                .confidence(dto.getConfidence())
                .build();

        // Set JSON fields
        if (dto.getContributingFactors() != null) {
            prediction.setContributingFactors(objectMapper.valueToTree(dto.getContributingFactors()));
        }
        if (dto.getAnomalousFeatures() != null) {
            prediction.setAnomalousFeatures(objectMapper.valueToTree(dto.getAnomalousFeatures()));
        }
        if (dto.getComponentScores() != null) {
            prediction.setComponentScores(objectMapper.valueToTree(dto.getComponentScores()));
        }

        PoMlPrediction saved = predictionRepository.save(prediction);
        log.debug("Saved prediction {} for well {} type {}", saved.getId(), dto.getWellAssetId(), dto.getPredictionType());

        return saved;
    }

    /**
     * Acknowledge a prediction.
     */
    @Transactional
    public PoMlPredictionDto acknowledgePrediction(UUID predictionId, UUID userId) {
        PoMlPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new PoEntityNotFoundException("MlPrediction", predictionId));

        prediction.setAcknowledged(true);
        prediction.setAcknowledgedBy(userId);
        prediction.setAcknowledgedTime(System.currentTimeMillis());

        return toDto(predictionRepository.save(prediction));
    }

    /**
     * Dismiss a prediction.
     */
    @Transactional
    public PoMlPredictionDto dismissPrediction(UUID predictionId, UUID userId, String reason) {
        PoMlPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new PoEntityNotFoundException("MlPrediction", predictionId));

        prediction.setDismissed(true);
        prediction.setDismissedBy(userId);
        prediction.setDismissReason(reason);

        return toDto(predictionRepository.save(prediction));
    }

    /**
     * Count pending actions.
     */
    public long countPendingActions(UUID tenantId, BigDecimal threshold) {
        return predictionRepository.countPendingActions(tenantId, threshold);
    }

    /**
     * Delete old predictions (for cleanup scheduler).
     */
    @Transactional
    public void deleteOldPredictions(UUID tenantId, long cutoffTime) {
        predictionRepository.deleteOldPredictions(tenantId, cutoffTime);
        log.info("Deleted old predictions for tenant {} before {}", tenantId, cutoffTime);
    }

    private WellPredictionSummaryDto toSummaryDto(PoMlPrediction prediction) {
        WellPredictionSummaryDto.WellPredictionSummaryDtoBuilder builder = WellPredictionSummaryDto.builder()
                .wellAssetId(prediction.getWellAssetId())
                .lastUpdated(prediction.getCreatedTime());

        // Set fields based on prediction type
        if (prediction.getPredictionType() == PredictionType.FAILURE) {
            builder.failureProbability(prediction.getProbability());
            builder.daysToFailure(prediction.getDaysToFailure());
            // Extract primary issue from contributing factors
            if (prediction.getContributingFactors() != null) {
                List<PoMlPredictionDto.ContributingFactorDto> factors = parseContributingFactors(prediction.getContributingFactors());
                if (!factors.isEmpty()) {
                    builder.primaryIssue(factors.get(0).getFeature());
                }
            }
        }

        if (prediction.getHealthScore() != null) {
            builder.healthScore(prediction.getHealthScore());
            builder.healthLevel(prediction.getHealthLevel());
            builder.healthTrend(prediction.getHealthTrend());
        }

        if (prediction.getPredictionType() == PredictionType.ANOMALY) {
            builder.hasAnomaly(prediction.getIsAnomaly());
        }

        return builder.build();
    }

    private PoMlPredictionDto toDto(PoMlPrediction prediction) {
        PoMlPredictionDto.PoMlPredictionDtoBuilder builder = PoMlPredictionDto.builder()
                .id(prediction.getId())
                .tenantId(prediction.getTenantId())
                .wellAssetId(prediction.getWellAssetId())
                .modelId(prediction.getModelId())
                .predictionType(prediction.getPredictionType())
                .createdTime(prediction.getCreatedTime())
                .probability(prediction.getProbability())
                .daysToFailure(prediction.getDaysToFailure())
                .healthScore(prediction.getHealthScore())
                .healthLevel(prediction.getHealthLevel())
                .healthTrend(prediction.getHealthTrend())
                .isAnomaly(prediction.getIsAnomaly())
                .anomalyScore(prediction.getAnomalyScore())
                .confidence(prediction.getConfidence())
                .alarmCreated(prediction.getAlarmCreated())
                .alarmId(prediction.getAlarmId())
                .notificationSent(prediction.getNotificationSent())
                .workOrderCreated(prediction.getWorkOrderCreated())
                .workOrderId(prediction.getWorkOrderId())
                .acknowledged(prediction.getAcknowledged())
                .dismissed(prediction.getDismissed())
                .dismissReason(prediction.getDismissReason());

        // Parse JSON fields
        if (prediction.getContributingFactors() != null) {
            builder.contributingFactors(parseContributingFactors(prediction.getContributingFactors()));
        }
        if (prediction.getAnomalousFeatures() != null) {
            builder.anomalousFeatures(parseAnomalousFeatures(prediction.getAnomalousFeatures()));
        }
        if (prediction.getComponentScores() != null) {
            builder.componentScores(parseComponentScores(prediction.getComponentScores()));
        }

        return builder.build();
    }

    private List<PoMlPredictionDto.ContributingFactorDto> parseContributingFactors(Object json) {
        try {
            return objectMapper.convertValue(json, new TypeReference<List<PoMlPredictionDto.ContributingFactorDto>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse contributing factors", e);
            return Collections.emptyList();
        }
    }

    private List<PoMlPredictionDto.AnomalousFeatureDto> parseAnomalousFeatures(Object json) {
        try {
            return objectMapper.convertValue(json, new TypeReference<List<PoMlPredictionDto.AnomalousFeatureDto>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse anomalous features", e);
            return Collections.emptyList();
        }
    }

    private Map<String, Integer> parseComponentScores(Object json) {
        try {
            return objectMapper.convertValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse component scores", e);
            return Collections.emptyMap();
        }
    }
}
