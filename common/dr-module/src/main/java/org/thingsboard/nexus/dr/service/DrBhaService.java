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
package org.thingsboard.nexus.dr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.dr.dto.DrBhaDto;
import org.thingsboard.nexus.dr.exception.DrBusinessException;
import org.thingsboard.nexus.dr.exception.DrEntityNotFoundException;
import org.thingsboard.nexus.dr.model.DrBha;
import org.thingsboard.nexus.dr.model.enums.BhaType;
import org.thingsboard.nexus.dr.repository.DrBhaRepository;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.template.CreateFromTemplateRequest;
import org.thingsboard.server.common.data.template.TemplateInstanceResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Bottom Hole Assemblies (BHAs)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrBhaService {

    private final DrBhaRepository bhaRepository;
    private final DrTemplateService templateService;

    // --- Query Operations ---

    @Transactional(readOnly = true)
    public DrBhaDto getById(UUID id) {
        log.debug("Getting BHA by id: {}", id);
        DrBha bha = bhaRepository.findById(id)
                .orElseThrow(() -> new DrEntityNotFoundException("BHA", id.toString()));
        return DrBhaDto.fromEntity(bha);
    }

    @Transactional(readOnly = true)
    public DrBhaDto getByNumber(UUID tenantId, String bhaNumber) {
        log.debug("Getting BHA by number: {}", bhaNumber);
        DrBha bha = bhaRepository.findByTenantIdAndBhaNumber(tenantId, bhaNumber)
                .orElseThrow(() -> new DrEntityNotFoundException("BHA", bhaNumber));
        return DrBhaDto.fromEntity(bha);
    }

    @Transactional(readOnly = true)
    public DrBhaDto getByAssetId(UUID assetId) {
        log.debug("Getting BHA by asset id: {}", assetId);
        DrBha bha = bhaRepository.findByAssetId(assetId)
                .orElseThrow(() -> new DrEntityNotFoundException("BHA", "asset:" + assetId));
        return DrBhaDto.fromEntity(bha);
    }

    @Transactional(readOnly = true)
    public Page<DrBhaDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting BHAs for tenant: {}", tenantId);
        Page<DrBha> bhas = bhaRepository.findByTenantId(tenantId, pageable);
        return bhas.map(DrBhaDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<DrBhaDto> getAllByTenant(UUID tenantId) {
        log.debug("Getting all BHAs for tenant: {}", tenantId);
        List<DrBha> bhas = bhaRepository.findByTenantId(tenantId);
        return bhas.stream().map(DrBhaDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DrBhaDto> getByFilters(UUID tenantId, String status, BhaType bhaType,
                                       Boolean isDirectional, BigDecimal bitSizeIn, Pageable pageable) {
        log.debug("Getting BHAs with filters - tenant: {}, status: {}, type: {}",
                tenantId, status, bhaType);
        Page<DrBha> bhas = bhaRepository.findByFilters(tenantId, status, bhaType, isDirectional, bitSizeIn, pageable);
        return bhas.map(DrBhaDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<DrBhaDto> getAvailableBhas(UUID tenantId) {
        log.debug("Getting available BHAs for tenant: {}", tenantId);
        List<DrBha> bhas = bhaRepository.findAvailableBhas(tenantId);
        return bhas.stream().map(DrBhaDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrBhaDto> getAvailableDirectionalBhas(UUID tenantId) {
        log.debug("Getting available directional BHAs for tenant: {}", tenantId);
        List<DrBha> bhas = bhaRepository.findAvailableDirectionalBhas(tenantId);
        return bhas.stream().map(DrBhaDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrBhaDto> getByStatus(UUID tenantId, String status) {
        log.debug("Getting BHAs by status - tenant: {}, status: {}", tenantId, status);
        List<DrBha> bhas = bhaRepository.findByTenantIdAndStatus(tenantId, status);
        return bhas.stream().map(DrBhaDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrBhaDto> getByType(UUID tenantId, BhaType bhaType) {
        log.debug("Getting BHAs by type - tenant: {}, type: {}", tenantId, bhaType);
        List<DrBha> bhas = bhaRepository.findByTenantIdAndBhaType(tenantId, bhaType);
        return bhas.stream().map(DrBhaDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrBhaDto> getByBitSize(UUID tenantId, BigDecimal bitSizeIn) {
        log.debug("Getting BHAs by bit size - tenant: {}, size: {}", tenantId, bitSizeIn);
        List<DrBha> bhas = bhaRepository.findByTenantIdAndBitSize(tenantId, bitSizeIn);
        return bhas.stream().map(DrBhaDto::fromEntity).collect(Collectors.toList());
    }

    // --- Create Operations ---

    @Transactional
    public DrBhaDto createFromTemplate(UUID tenantId, CreateFromTemplateRequest request) {
        return createFromTemplate(tenantId, request.getTemplateId(), request.getVariables(), tenantId);
    }

    @Transactional
    public DrBhaDto createFromTemplate(UUID tenantId, UUID templateId, Map<String, Object> variables, UUID createdBy) {
        log.info("Creating BHA from template {} for tenant {}", templateId, tenantId);

        String bhaNumber = (String) variables.get("bhaNumber");
        if (bhaNumber == null || bhaNumber.isEmpty()) {
            throw new DrBusinessException("BHA number is required");
        }

        if (bhaRepository.existsByTenantIdAndBhaNumber(tenantId, bhaNumber)) {
            throw new DrBusinessException("BHA number already exists: " + bhaNumber);
        }

        // Instantiate template to create digital twin asset
        TemplateInstanceResult instanceResult = templateService.instantiateTemplate(
                new TenantId(tenantId),
                templateId,
                variables,
                createdBy
        );

        // Create the BHA entity with reference to created asset
        DrBha bha = new DrBha();
        bha.setId(UUID.randomUUID());
        bha.setTenantId(tenantId);
        bha.setAssetId(instanceResult.getRootAssetId());
        bha.setBhaNumber(bhaNumber);
        bha.setCreatedBy(createdBy);
        bha.setCreatedTime(System.currentTimeMillis());

        // Set BHA specifications from variables
        String bhaTypeStr = (String) variables.get("bhaType");
        if (bhaTypeStr != null) {
            bha.setBhaType(BhaType.valueOf(bhaTypeStr));
        }

        Object isDirectional = variables.get("isDirectional");
        if (isDirectional != null) {
            bha.setIsDirectional(isDirectional instanceof Boolean ? (Boolean) isDirectional :
                    Boolean.parseBoolean(isDirectional.toString()));
        }

        // Bit information
        bha.setBitSerial((String) variables.get("bitSerial"));
        bha.setBitType((String) variables.get("bitType"));
        bha.setBitIadcCode((String) variables.get("bitIadcCode"));
        bha.setBitManufacturer((String) variables.get("bitManufacturer"));
        bha.setBitModel((String) variables.get("bitModel"));
        bha.setBitNozzles((String) variables.get("bitNozzles"));

        Object bitSizeIn = variables.get("bitSizeIn");
        if (bitSizeIn != null) {
            bha.setBitSizeIn(bitSizeIn instanceof BigDecimal ? (BigDecimal) bitSizeIn :
                    BigDecimal.valueOf(Double.parseDouble(bitSizeIn.toString())));
        }

        Object bitTfaSqIn = variables.get("bitTfaSqIn");
        if (bitTfaSqIn != null) {
            bha.setBitTfaSqIn(bitTfaSqIn instanceof BigDecimal ? (BigDecimal) bitTfaSqIn :
                    BigDecimal.valueOf(Double.parseDouble(bitTfaSqIn.toString())));
        }

        // Motor information
        bha.setMotorManufacturer((String) variables.get("motorManufacturer"));
        bha.setMotorModel((String) variables.get("motorModel"));
        bha.setMotorLobeConfiguration((String) variables.get("motorLobeConfiguration"));

        Object motorOdIn = variables.get("motorOdIn");
        if (motorOdIn != null) {
            bha.setMotorOdIn(motorOdIn instanceof BigDecimal ? (BigDecimal) motorOdIn :
                    BigDecimal.valueOf(Double.parseDouble(motorOdIn.toString())));
        }

        Object motorBendAngleDeg = variables.get("motorBendAngleDeg");
        if (motorBendAngleDeg != null) {
            bha.setMotorBendAngleDeg(motorBendAngleDeg instanceof BigDecimal ? (BigDecimal) motorBendAngleDeg :
                    BigDecimal.valueOf(Double.parseDouble(motorBendAngleDeg.toString())));
        }

        bha.setStatus("AVAILABLE");

        DrBha savedBha = bhaRepository.save(bha);
        log.info("BHA created from template: {} with asset ID: {}", savedBha.getId(), savedBha.getAssetId());

        return DrBhaDto.fromEntity(savedBha);
    }

    @Transactional
    public DrBhaDto create(DrBha bha) {
        log.info("Creating new BHA: {}", bha.getBhaNumber());

        if (bhaRepository.existsByTenantIdAndBhaNumber(bha.getTenantId(), bha.getBhaNumber())) {
            throw new DrBusinessException("BHA number already exists: " + bha.getBhaNumber());
        }

        if (bha.getCreatedTime() == null) {
            bha.setCreatedTime(System.currentTimeMillis());
        }

        DrBha savedBha = bhaRepository.save(bha);
        log.info("BHA created: {}", savedBha.getId());

        return DrBhaDto.fromEntity(savedBha);
    }

    // --- Update Operations ---

    @Transactional
    public DrBhaDto update(UUID id, DrBha updatedBha) {
        log.info("Updating BHA: {}", id);

        DrBha existingBha = bhaRepository.findById(id)
                .orElseThrow(() -> new DrEntityNotFoundException("BHA", id.toString()));

        // Check for duplicate BHA number
        if (!existingBha.getBhaNumber().equals(updatedBha.getBhaNumber()) &&
            bhaRepository.existsByTenantIdAndBhaNumberAndIdNot(existingBha.getTenantId(), updatedBha.getBhaNumber(), id)) {
            throw new DrBusinessException("BHA number already exists: " + updatedBha.getBhaNumber());
        }

        // Update fields
        existingBha.setBhaNumber(updatedBha.getBhaNumber());
        existingBha.setBhaType(updatedBha.getBhaType());
        existingBha.setIsDirectional(updatedBha.getIsDirectional());

        // Bit information
        existingBha.setBitSerial(updatedBha.getBitSerial());
        existingBha.setBitType(updatedBha.getBitType());
        existingBha.setBitSizeIn(updatedBha.getBitSizeIn());
        existingBha.setBitIadcCode(updatedBha.getBitIadcCode());
        existingBha.setBitManufacturer(updatedBha.getBitManufacturer());
        existingBha.setBitModel(updatedBha.getBitModel());
        existingBha.setBitTfaSqIn(updatedBha.getBitTfaSqIn());
        existingBha.setBitNozzles(updatedBha.getBitNozzles());

        // Dimensions
        existingBha.setTotalLengthFt(updatedBha.getTotalLengthFt());
        existingBha.setTotalWeightLbs(updatedBha.getTotalWeightLbs());

        // Motor information
        existingBha.setMotorManufacturer(updatedBha.getMotorManufacturer());
        existingBha.setMotorModel(updatedBha.getMotorModel());
        existingBha.setMotorOdIn(updatedBha.getMotorOdIn());
        existingBha.setMotorBendAngleDeg(updatedBha.getMotorBendAngleDeg());
        existingBha.setMotorLobeConfiguration(updatedBha.getMotorLobeConfiguration());

        // RSS information
        existingBha.setRssManufacturer(updatedBha.getRssManufacturer());
        existingBha.setRssModel(updatedBha.getRssModel());
        existingBha.setRssType(updatedBha.getRssType());

        // Components
        existingBha.setComponentsJson(updatedBha.getComponentsJson());

        // Metadata
        existingBha.setNotes(updatedBha.getNotes());
        existingBha.setMetadata(updatedBha.getMetadata());
        existingBha.setUpdatedTime(System.currentTimeMillis());

        DrBha savedBha = bhaRepository.save(existingBha);
        log.info("BHA updated: {}", savedBha.getId());

        return DrBhaDto.fromEntity(savedBha);
    }

    @Transactional
    public DrBhaDto updateStatus(UUID bhaId, String newStatus) {
        log.info("Updating BHA {} status to {}", bhaId, newStatus);

        DrBha bha = bhaRepository.findById(bhaId)
                .orElseThrow(() -> new DrEntityNotFoundException("BHA", bhaId.toString()));

        bha.setStatus(newStatus);
        bha.setUpdatedTime(System.currentTimeMillis());

        DrBha savedBha = bhaRepository.save(bha);
        return DrBhaDto.fromEntity(savedBha);
    }

    @Transactional
    public DrBhaDto recordDullGrade(UUID bhaId, String dullInner, String dullOuter, String dullChar,
                                    String dullLocation, String bearingCondition, String gaugeCondition,
                                    String reasonPulled) {
        log.info("Recording dull grade for BHA: {}", bhaId);

        DrBha bha = bhaRepository.findById(bhaId)
                .orElseThrow(() -> new DrEntityNotFoundException("BHA", bhaId.toString()));

        bha.setBitDullInner(dullInner);
        bha.setBitDullOuter(dullOuter);
        bha.setBitDullChar(dullChar);
        bha.setBitDullLocation(dullLocation);
        bha.setBitBearingCondition(bearingCondition);
        bha.setBitGaugeCondition(gaugeCondition);
        bha.setBitReasonPulled(reasonPulled);
        bha.setUpdatedTime(System.currentTimeMillis());

        DrBha savedBha = bhaRepository.save(bha);
        log.info("Dull grade recorded for BHA: {}", bhaId);

        return DrBhaDto.fromEntity(savedBha);
    }

    @Transactional
    public DrBhaDto updateStatistics(UUID bhaId, BigDecimal footageDrilled, BigDecimal hoursOnBottom) {
        log.info("Updating statistics for BHA: {}", bhaId);

        DrBha bha = bhaRepository.findById(bhaId)
                .orElseThrow(() -> new DrEntityNotFoundException("BHA", bhaId.toString()));

        if (footageDrilled != null) {
            BigDecimal currentFootage = bha.getTotalFootageDrilled() != null ? bha.getTotalFootageDrilled() : BigDecimal.ZERO;
            bha.setTotalFootageDrilled(currentFootage.add(footageDrilled));
        }

        if (hoursOnBottom != null) {
            BigDecimal currentHours = bha.getTotalHoursOnBottom() != null ? bha.getTotalHoursOnBottom() : BigDecimal.ZERO;
            bha.setTotalHoursOnBottom(currentHours.add(hoursOnBottom));
        }

        bha.setUpdatedTime(System.currentTimeMillis());

        DrBha savedBha = bhaRepository.save(bha);
        return DrBhaDto.fromEntity(savedBha);
    }

    @Transactional
    public DrBhaDto incrementRunCount(UUID bhaId) {
        log.info("Incrementing run count for BHA: {}", bhaId);

        DrBha bha = bhaRepository.findById(bhaId)
                .orElseThrow(() -> new DrEntityNotFoundException("BHA", bhaId.toString()));

        int currentRuns = bha.getTotalRuns() != null ? bha.getTotalRuns() : 0;
        bha.setTotalRuns(currentRuns + 1);
        bha.setUpdatedTime(System.currentTimeMillis());

        DrBha savedBha = bhaRepository.save(bha);
        return DrBhaDto.fromEntity(savedBha);
    }

    // --- Delete Operations ---

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting BHA: {}", id);

        DrBha bha = bhaRepository.findById(id)
                .orElseThrow(() -> new DrEntityNotFoundException("BHA", id.toString()));

        if ("IN_USE".equals(bha.getStatus())) {
            throw new DrBusinessException("Cannot delete BHA that is currently in use");
        }

        bhaRepository.delete(bha);
        log.info("BHA deleted: {}", id);
    }

    // --- Statistics ---

    @Transactional(readOnly = true)
    public long countByStatus(UUID tenantId, String status) {
        return bhaRepository.countByTenantIdAndStatus(tenantId, status);
    }

    @Transactional(readOnly = true)
    public long countByType(UUID tenantId, BhaType bhaType) {
        return bhaRepository.countByTenantIdAndBhaType(tenantId, bhaType);
    }
}
