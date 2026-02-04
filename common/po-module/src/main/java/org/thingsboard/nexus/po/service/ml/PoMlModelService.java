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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.po.dto.ml.MlModelStatus;
import org.thingsboard.nexus.po.dto.ml.MlModelType;
import org.thingsboard.nexus.po.dto.ml.PoMlModelDto;
import org.thingsboard.nexus.po.exception.PoEntityNotFoundException;
import org.thingsboard.nexus.po.model.PoMlModel;
import org.thingsboard.nexus.po.repository.PoMlModelRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing ML models.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PoMlModelService {

    private final PoMlModelRepository modelRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get all models for a tenant with pagination.
     */
    public Page<PoMlModelDto> getModels(UUID tenantId, Pageable pageable) {
        return modelRepository.findByTenantId(tenantId, pageable)
                .map(this::toDto);
    }

    /**
     * Get all models for a tenant (no pagination).
     */
    public List<PoMlModelDto> getAllModels(UUID tenantId) {
        return modelRepository.findByTenantIdOrderByCreatedTimeDesc(tenantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get model by ID.
     */
    public PoMlModelDto getModel(UUID modelId) {
        PoMlModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new PoEntityNotFoundException("MlModel", modelId));
        return toDto(model);
    }

    /**
     * Get active models for a tenant.
     */
    public List<PoMlModelDto> getActiveModels(UUID tenantId) {
        return modelRepository.findActiveModels(tenantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get models by type.
     */
    public List<PoMlModelDto> getModelsByType(UUID tenantId, MlModelType modelType) {
        return modelRepository.findByTenantIdAndModelType(tenantId, modelType)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Deploy a model (set as active).
     * Archives any previously active model of the same type.
     */
    @Transactional
    public PoMlModelDto deployModel(UUID modelId) {
        PoMlModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new PoEntityNotFoundException("MlModel", modelId));

        // Archive any existing active model of same type
        modelRepository.findActiveModelByType(model.getTenantId(), model.getModelType())
                .ifPresent(activeModel -> {
                    activeModel.setStatus(MlModelStatus.ARCHIVED);
                    modelRepository.save(activeModel);
                    log.info("Archived model: {} v{}", activeModel.getName(), activeModel.getVersion());
                });

        // Set new model as active
        model.setStatus(MlModelStatus.ACTIVE);
        PoMlModel saved = modelRepository.save(model);
        log.info("Deployed model: {} v{}", model.getName(), model.getVersion());

        return toDto(saved);
    }

    /**
     * Archive a model.
     */
    @Transactional
    public PoMlModelDto archiveModel(UUID modelId) {
        PoMlModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new PoEntityNotFoundException("MlModel", modelId));

        model.setStatus(MlModelStatus.ARCHIVED);
        PoMlModel saved = modelRepository.save(model);
        log.info("Archived model: {} v{}", model.getName(), model.getVersion());

        return toDto(saved);
    }

    /**
     * Delete a model.
     */
    @Transactional
    public void deleteModel(UUID modelId) {
        PoMlModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new PoEntityNotFoundException("MlModel", modelId));

        if (model.getStatus() == MlModelStatus.ACTIVE) {
            throw new IllegalStateException("Cannot delete active model. Archive it first.");
        }

        modelRepository.delete(model);
        log.info("Deleted model: {} v{}", model.getName(), model.getVersion());
    }

    /**
     * Create a new model entry (called by training service).
     */
    @Transactional
    public PoMlModel createModel(UUID tenantId, String name, MlModelType modelType, String version, UUID createdBy) {
        PoMlModel model = PoMlModel.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name(name)
                .modelType(modelType)
                .version(version)
                .status(MlModelStatus.TRAINING)
                .createdTime(System.currentTimeMillis())
                .createdBy(createdBy)
                .build();

        return modelRepository.save(model);
    }

    /**
     * Update model after training completes.
     */
    @Transactional
    public void updateTrainingResults(UUID modelId, PoMlModelDto results) {
        PoMlModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new PoEntityNotFoundException("MlModel", modelId));

        model.setAccuracy(results.getAccuracy());
        model.setPrecisionScore(results.getPrecisionScore());
        model.setRecall(results.getRecall());
        model.setF1Score(results.getF1Score());
        model.setAucRoc(results.getAucRoc());
        model.setTrainingEndTime(System.currentTimeMillis());
        model.setTrainingSamples(results.getTrainingSamples());
        model.setFailureEvents(results.getFailureEvents());
        model.setWellsCount(results.getWellsCount());
        model.setModelPath(results.getModelPath());
        model.setMlflowRunId(results.getMlflowRunId());

        if (results.getHyperparameters() != null) {
            model.setHyperparameters(results.getHyperparameters());
        }
        if (results.getFeatureImportance() != null) {
            model.setFeatureImportance(objectMapper.valueToTree(results.getFeatureImportance()));
        }

        model.setStatus(MlModelStatus.ARCHIVED); // Not active until deployed
        modelRepository.save(model);
    }

    /**
     * Mark model as failed.
     */
    @Transactional
    public void markModelFailed(UUID modelId) {
        PoMlModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new PoEntityNotFoundException("MlModel", modelId));

        model.setStatus(MlModelStatus.FAILED);
        modelRepository.save(model);
    }

    private PoMlModelDto toDto(PoMlModel model) {
        Map<String, Double> featureImportance = new HashMap<>();
        if (model.getFeatureImportance() != null) {
            model.getFeatureImportance().fields().forEachRemaining(entry ->
                    featureImportance.put(entry.getKey(), entry.getValue().asDouble()));
        }

        return PoMlModelDto.builder()
                .id(model.getId())
                .tenantId(model.getTenantId())
                .name(model.getName())
                .modelType(model.getModelType())
                .liftSystemType(model.getLiftSystemType())
                .version(model.getVersion())
                .status(model.getStatus())
                .accuracy(model.getAccuracy())
                .precisionScore(model.getPrecisionScore())
                .recall(model.getRecall())
                .f1Score(model.getF1Score())
                .aucRoc(model.getAucRoc())
                .trainingStartTime(model.getTrainingStartTime())
                .trainingEndTime(model.getTrainingEndTime())
                .trainingSamples(model.getTrainingSamples())
                .failureEvents(model.getFailureEvents())
                .wellsCount(model.getWellsCount())
                .hyperparameters(model.getHyperparameters())
                .featureImportance(featureImportance)
                .modelPath(model.getModelPath())
                .mlflowRunId(model.getMlflowRunId())
                .createdTime(model.getCreatedTime())
                .createdBy(model.getCreatedBy())
                .build();
    }
}
