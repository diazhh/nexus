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
package org.thingsboard.nexus.po.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.po.config.PoModuleConfiguration;
import org.thingsboard.nexus.po.dto.OptimizationType;
import org.thingsboard.nexus.po.dto.RecommendationDto;
import org.thingsboard.nexus.po.dto.RecommendationStatus;
import org.thingsboard.nexus.po.exception.PoEntityNotFoundException;
import org.thingsboard.nexus.po.model.PoRecommendation;
import org.thingsboard.nexus.po.repository.PoRecommendationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing optimization recommendations.
 * Handles the full lifecycle: create -> approve/reject -> execute
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoRecommendationService {

    private final PoRecommendationRepository repository;
    private final PoAssetService assetService;
    private final PoModuleConfiguration config;

    /**
     * Creates a new recommendation.
     */
    @Transactional
    public RecommendationDto createRecommendation(RecommendationDto dto) {
        log.info("Creating recommendation for asset: {}, type: {}", dto.getAssetId(), dto.getType());

        PoRecommendation entity = mapToEntity(dto);
        entity.setId(UUID.randomUUID());
        entity.setStatus(RecommendationStatus.PENDING);
        entity.setCreatedTime(System.currentTimeMillis());

        // Set expiry time if enabled
        if (config.isRecommendationAutoExpiry()) {
            long expiryMs = config.getRecommendationExpiryHours() * 60 * 60 * 1000L;
            entity.setExpiryTime(entity.getCreatedTime() + expiryMs);
        }

        // Get asset name
        entity.setAssetType(assetService.getAssetType(dto.getAssetId()));
        String assetName = assetService.getAssetName(dto.getAssetId());

        PoRecommendation saved = repository.save(entity);
        log.info("Recommendation created: id={}, asset={}", saved.getId(), assetName);

        RecommendationDto result = mapToDto(saved);
        result.setAssetName(assetName);
        return result;
    }

    /**
     * Gets a recommendation by ID.
     */
    public Optional<RecommendationDto> getRecommendation(UUID id) {
        return repository.findById(id).map(this::mapToDto);
    }

    /**
     * Gets recommendations by tenant with pagination.
     */
    public Page<RecommendationDto> getRecommendations(UUID tenantId, int page, int size) {
        Page<PoRecommendation> entities = repository.findByTenantIdOrderByCreatedTimeDesc(
                tenantId, PageRequest.of(page, size));
        return mapToPage(entities, page, size);
    }

    /**
     * Gets recommendations by status.
     */
    public Page<RecommendationDto> getRecommendationsByStatus(UUID tenantId, RecommendationStatus status, int page, int size) {
        Page<PoRecommendation> entities = repository.findByTenantIdAndStatusOrderByCreatedTimeDesc(
                tenantId, status, PageRequest.of(page, size));
        return mapToPage(entities, page, size);
    }

    /**
     * Gets pending recommendations for a tenant.
     */
    public List<RecommendationDto> getPendingRecommendations(UUID tenantId) {
        return repository.findByTenantIdAndStatusOrderByPriorityAscCreatedTimeDesc(tenantId, RecommendationStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Gets pending recommendations for an asset.
     */
    public List<RecommendationDto> getPendingRecommendationsForAsset(UUID assetId) {
        return repository.findByAssetIdAndStatusOrderByPriorityAscCreatedTimeDesc(assetId, RecommendationStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Gets recommendations by optimization type.
     */
    public Page<RecommendationDto> getRecommendationsByType(UUID tenantId, OptimizationType type, int page, int size) {
        Page<PoRecommendation> entities = repository.findByTenantIdAndOptimizationTypeOrderByCreatedTimeDesc(
                tenantId, type, PageRequest.of(page, size));
        return mapToPage(entities, page, size);
    }

    /**
     * Approves a recommendation.
     */
    @Transactional
    public RecommendationDto approveRecommendation(UUID id, UUID userId, String notes) {
        PoRecommendation entity = repository.findById(id)
                .orElseThrow(() -> new PoEntityNotFoundException("Recommendation", id));

        if (entity.getStatus() != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Can only approve PENDING recommendations. Current status: " + entity.getStatus());
        }

        entity.setStatus(RecommendationStatus.APPROVED);
        entity.setApprovedBy(userId);
        entity.setApprovedTime(System.currentTimeMillis());
        if (notes != null) {
            entity.setNotes(notes);
        }

        PoRecommendation saved = repository.save(entity);
        log.info("Recommendation approved: id={}, by={}", id, userId);

        return mapToDto(saved);
    }

    /**
     * Rejects a recommendation.
     */
    @Transactional
    public RecommendationDto rejectRecommendation(UUID id, UUID userId, String reason) {
        PoRecommendation entity = repository.findById(id)
                .orElseThrow(() -> new PoEntityNotFoundException("Recommendation", id));

        if (entity.getStatus() != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Can only reject PENDING recommendations. Current status: " + entity.getStatus());
        }

        entity.setStatus(RecommendationStatus.REJECTED);
        entity.setApprovedBy(userId); // Using approvedBy for the rejector
        entity.setApprovedTime(System.currentTimeMillis());
        entity.setRejectionReason(reason);

        PoRecommendation saved = repository.save(entity);
        log.info("Recommendation rejected: id={}, by={}, reason={}", id, userId, reason);

        return mapToDto(saved);
    }

    /**
     * Marks a recommendation as executed.
     */
    @Transactional
    public RecommendationDto executeRecommendation(UUID id, UUID userId) {
        PoRecommendation entity = repository.findById(id)
                .orElseThrow(() -> new PoEntityNotFoundException("Recommendation", id));

        if (entity.getStatus() != RecommendationStatus.APPROVED) {
            throw new IllegalStateException("Can only execute APPROVED recommendations. Current status: " + entity.getStatus());
        }

        entity.setStatus(RecommendationStatus.EXECUTED);
        entity.setExecutedBy(userId);
        entity.setExecutedTime(System.currentTimeMillis());

        PoRecommendation saved = repository.save(entity);
        log.info("Recommendation executed: id={}, by={}", id, userId);

        return mapToDto(saved);
    }

    /**
     * Marks a recommendation as failed.
     */
    @Transactional
    public RecommendationDto failRecommendation(UUID id, String errorMessage) {
        PoRecommendation entity = repository.findById(id)
                .orElseThrow(() -> new PoEntityNotFoundException("Recommendation", id));

        entity.setStatus(RecommendationStatus.FAILED);
        entity.setNotes(errorMessage);

        PoRecommendation saved = repository.save(entity);
        log.warn("Recommendation failed: id={}, error={}", id, errorMessage);

        return mapToDto(saved);
    }

    /**
     * Cancels a recommendation.
     */
    @Transactional
    public RecommendationDto cancelRecommendation(UUID id, UUID userId, String reason) {
        PoRecommendation entity = repository.findById(id)
                .orElseThrow(() -> new PoEntityNotFoundException("Recommendation", id));

        if (entity.getStatus() == RecommendationStatus.EXECUTED) {
            throw new IllegalStateException("Cannot cancel EXECUTED recommendations");
        }

        entity.setStatus(RecommendationStatus.CANCELLED);
        entity.setRejectionReason(reason);

        PoRecommendation saved = repository.save(entity);
        log.info("Recommendation cancelled: id={}, by={}", id, userId);

        return mapToDto(saved);
    }

    /**
     * Counts pending recommendations for a tenant.
     */
    public long countPending(UUID tenantId) {
        return repository.countPendingByTenant(tenantId);
    }

    /**
     * Counts recommendations by status.
     */
    public long countByStatus(UUID tenantId, RecommendationStatus status) {
        return repository.countByTenantIdAndStatus(tenantId, status);
    }

    /**
     * Calculates acceptance rate.
     */
    public Double getAcceptanceRate(UUID tenantId) {
        return repository.calculateAcceptanceRate(tenantId);
    }

    /**
     * Expires old pending recommendations (scheduled task).
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void expireOldRecommendations() {
        if (!config.isRecommendationAutoExpiry()) {
            return;
        }

        int expired = repository.expireOldRecommendations(System.currentTimeMillis());
        if (expired > 0) {
            log.info("Expired {} old recommendations", expired);
        }
    }

    // Mapping methods

    private PoRecommendation mapToEntity(RecommendationDto dto) {
        return PoRecommendation.builder()
                .id(dto.getId())
                .tenantId(dto.getTenantId())
                .assetId(dto.getAssetId())
                .assetType(dto.getAssetType())
                .optimizationType(dto.getType())
                .status(dto.getStatus())
                .priority(dto.getPriority())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .currentValue(dto.getCurrentValue())
                .recommendedValue(dto.getRecommendedValue())
                .unit(dto.getUnit())
                .expectedProductionIncrease(dto.getExpectedProductionIncrease())
                .expectedProductionIncreasePercent(dto.getExpectedProductionIncreasePercent())
                .expectedCostSavings(dto.getExpectedCostSavings())
                .expectedEfficiencyImprovement(dto.getExpectedEfficiencyImprovement())
                .confidence(dto.getConfidence())
                .parameters(dto.getParameters())
                .optimizationResultId(dto.getOptimizationResultId())
                .createdBy(dto.getCreatedBy())
                .approvedBy(dto.getApprovedBy())
                .executedBy(dto.getExecutedBy())
                .rejectionReason(dto.getRejectionReason())
                .createdTime(dto.getCreatedTime())
                .approvedTime(dto.getApprovedTime())
                .executedTime(dto.getExecutedTime())
                .expiryTime(dto.getExpiryTime())
                .notes(dto.getNotes())
                .build();
    }

    private RecommendationDto mapToDto(PoRecommendation entity) {
        return RecommendationDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .assetId(entity.getAssetId())
                .assetType(entity.getAssetType())
                .type(entity.getOptimizationType())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .currentValue(entity.getCurrentValue())
                .recommendedValue(entity.getRecommendedValue())
                .unit(entity.getUnit())
                .expectedProductionIncrease(entity.getExpectedProductionIncrease())
                .expectedProductionIncreasePercent(entity.getExpectedProductionIncreasePercent())
                .expectedCostSavings(entity.getExpectedCostSavings())
                .expectedEfficiencyImprovement(entity.getExpectedEfficiencyImprovement())
                .confidence(entity.getConfidence())
                .parameters(entity.getParameters())
                .optimizationResultId(entity.getOptimizationResultId())
                .createdBy(entity.getCreatedBy())
                .approvedBy(entity.getApprovedBy())
                .executedBy(entity.getExecutedBy())
                .rejectionReason(entity.getRejectionReason())
                .createdTime(entity.getCreatedTime())
                .approvedTime(entity.getApprovedTime())
                .executedTime(entity.getExecutedTime())
                .expiryTime(entity.getExpiryTime())
                .notes(entity.getNotes())
                .assetName(assetService.getAssetName(entity.getAssetId()))
                .build();
    }

    private Page<RecommendationDto> mapToPage(Page<PoRecommendation> entities, int page, int size) {
        List<RecommendationDto> dtos = entities.getContent().stream()
                .map(this::mapToDto)
                .toList();
        return new PageImpl<>(dtos, PageRequest.of(page, size), entities.getTotalElements());
    }
}
