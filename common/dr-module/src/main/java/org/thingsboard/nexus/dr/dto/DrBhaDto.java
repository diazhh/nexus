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
package org.thingsboard.nexus.dr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.nexus.dr.model.DrBha;
import org.thingsboard.nexus.dr.model.enums.BhaType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Bottom Hole Assembly (BHA)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrBhaDto {

    private UUID id;
    private UUID tenantId;
    private String bhaNumber;
    private UUID assetId;

    // BHA Type and Configuration
    private BhaType bhaType;
    private Boolean isDirectional;

    // Bit Information
    private String bitSerial;
    private String bitType;
    private BigDecimal bitSizeIn;
    private String bitIadcCode;
    private String bitManufacturer;
    private String bitModel;
    private BigDecimal bitTfaSqIn;
    private String bitNozzles;

    // BHA Dimensions
    private BigDecimal totalLengthFt;
    private BigDecimal totalWeightLbs;

    // Motor Information
    private String motorManufacturer;
    private String motorModel;
    private BigDecimal motorOdIn;
    private BigDecimal motorBendAngleDeg;
    private String motorLobeConfiguration;

    // RSS Information
    private String rssManufacturer;
    private String rssModel;
    private String rssType;

    // Status and Tracking
    private String status;
    private BigDecimal totalFootageDrilled;
    private BigDecimal totalHoursOnBottom;
    private Integer totalRuns;

    // Components
    private JsonNode componentsJson;

    // Dull Grading
    private String bitDullInner;
    private String bitDullOuter;
    private String bitDullChar;
    private String bitDullLocation;
    private String bitBearingCondition;
    private String bitGaugeCondition;
    private String bitReasonPulled;

    // Metadata
    private String notes;
    private JsonNode metadata;

    private Long createdTime;
    private Long updatedTime;

    /**
     * Create DTO from entity
     */
    public static DrBhaDto fromEntity(DrBha entity) {
        if (entity == null) {
            return null;
        }

        DrBhaDto dto = new DrBhaDto();
        dto.id = entity.getId();
        dto.tenantId = entity.getTenantId();
        dto.bhaNumber = entity.getBhaNumber();
        dto.assetId = entity.getAssetId();

        // BHA Type
        dto.bhaType = entity.getBhaType();
        dto.isDirectional = entity.getIsDirectional();

        // Bit Information
        dto.bitSerial = entity.getBitSerial();
        dto.bitType = entity.getBitType();
        dto.bitSizeIn = entity.getBitSizeIn();
        dto.bitIadcCode = entity.getBitIadcCode();
        dto.bitManufacturer = entity.getBitManufacturer();
        dto.bitModel = entity.getBitModel();
        dto.bitTfaSqIn = entity.getBitTfaSqIn();
        dto.bitNozzles = entity.getBitNozzles();

        // Dimensions
        dto.totalLengthFt = entity.getTotalLengthFt();
        dto.totalWeightLbs = entity.getTotalWeightLbs();

        // Motor Information
        dto.motorManufacturer = entity.getMotorManufacturer();
        dto.motorModel = entity.getMotorModel();
        dto.motorOdIn = entity.getMotorOdIn();
        dto.motorBendAngleDeg = entity.getMotorBendAngleDeg();
        dto.motorLobeConfiguration = entity.getMotorLobeConfiguration();

        // RSS Information
        dto.rssManufacturer = entity.getRssManufacturer();
        dto.rssModel = entity.getRssModel();
        dto.rssType = entity.getRssType();

        // Status and Tracking
        dto.status = entity.getStatus();
        dto.totalFootageDrilled = entity.getTotalFootageDrilled();
        dto.totalHoursOnBottom = entity.getTotalHoursOnBottom();
        dto.totalRuns = entity.getTotalRuns();

        // Components
        dto.componentsJson = entity.getComponentsJson();

        // Dull Grading
        dto.bitDullInner = entity.getBitDullInner();
        dto.bitDullOuter = entity.getBitDullOuter();
        dto.bitDullChar = entity.getBitDullChar();
        dto.bitDullLocation = entity.getBitDullLocation();
        dto.bitBearingCondition = entity.getBitBearingCondition();
        dto.bitGaugeCondition = entity.getBitGaugeCondition();
        dto.bitReasonPulled = entity.getBitReasonPulled();

        // Metadata
        dto.notes = entity.getNotes();
        dto.metadata = entity.getMetadata();

        dto.createdTime = entity.getCreatedTime();
        dto.updatedTime = entity.getUpdatedTime();

        return dto;
    }

    /**
     * Convert DTO to entity for persistence
     */
    public DrBha toEntity() {
        DrBha entity = new DrBha();
        entity.setId(this.id);
        entity.setTenantId(this.tenantId);
        entity.setBhaNumber(this.bhaNumber);
        entity.setAssetId(this.assetId);

        // BHA Type
        entity.setBhaType(this.bhaType);
        entity.setIsDirectional(this.isDirectional);

        // Bit Information
        entity.setBitSerial(this.bitSerial);
        entity.setBitType(this.bitType);
        entity.setBitSizeIn(this.bitSizeIn);
        entity.setBitIadcCode(this.bitIadcCode);
        entity.setBitManufacturer(this.bitManufacturer);
        entity.setBitModel(this.bitModel);
        entity.setBitTfaSqIn(this.bitTfaSqIn);
        entity.setBitNozzles(this.bitNozzles);

        // Dimensions
        entity.setTotalLengthFt(this.totalLengthFt);
        entity.setTotalWeightLbs(this.totalWeightLbs);

        // Motor Information
        entity.setMotorManufacturer(this.motorManufacturer);
        entity.setMotorModel(this.motorModel);
        entity.setMotorOdIn(this.motorOdIn);
        entity.setMotorBendAngleDeg(this.motorBendAngleDeg);
        entity.setMotorLobeConfiguration(this.motorLobeConfiguration);

        // RSS Information
        entity.setRssManufacturer(this.rssManufacturer);
        entity.setRssModel(this.rssModel);
        entity.setRssType(this.rssType);

        // Status and Tracking
        entity.setStatus(this.status);
        entity.setTotalFootageDrilled(this.totalFootageDrilled);
        entity.setTotalHoursOnBottom(this.totalHoursOnBottom);
        entity.setTotalRuns(this.totalRuns);

        // Components
        entity.setComponentsJson(this.componentsJson);

        // Dull Grading
        entity.setBitDullInner(this.bitDullInner);
        entity.setBitDullOuter(this.bitDullOuter);
        entity.setBitDullChar(this.bitDullChar);
        entity.setBitDullLocation(this.bitDullLocation);
        entity.setBitBearingCondition(this.bitBearingCondition);
        entity.setBitGaugeCondition(this.bitGaugeCondition);
        entity.setBitReasonPulled(this.bitReasonPulled);

        // Metadata
        entity.setNotes(this.notes);
        entity.setMetadata(this.metadata);

        return entity;
    }
}
