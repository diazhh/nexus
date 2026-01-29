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
package org.thingsboard.nexus.rv.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO representing a Completion entity.
 * Maps to a ThingsBoard Asset of type "rv_completion" with SERVER_SCOPE attributes.
 *
 * A Completion represents the mechanical configuration of how a well is
 * connected to the reservoir (perforations, screens, etc.).
 *
 * Hierarchy: Well -> Completion
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RvCompletionDto {

    // Asset identity
    private UUID assetId;
    private UUID tenantId;
    private String name;
    private String label;

    // Hierarchy
    private UUID wellAssetId;
    private UUID zoneAssetId;            // Target zone

    // Completion identification
    private String completionCode;
    private Integer completionNumber;    // 1, 2, 3 for multiple completions
    private Long completionDate;
    private String completionType;       // OPENHOLE, CASED_PERFORATED, GRAVEL_PACK, FRAC_PACK, SLOTTED_LINER

    // Interval details
    private BigDecimal topDepthMdM;
    private BigDecimal bottomDepthMdM;
    private BigDecimal topDepthTvdM;
    private BigDecimal bottomDepthTvdM;
    private BigDecimal intervalLengthM;

    // Perforation details (if cased-perforated)
    private Integer perforationShotsPerFoot;
    private BigDecimal perforationPhasingDeg;
    private BigDecimal perforationDiameterIn;
    private BigDecimal perforationPenetrationIn;
    private Integer totalPerforations;

    // Screen/Liner details (if applicable)
    private BigDecimal screenSlotSizeIn;
    private String screenType;           // WIRE_WRAPPED, PREMIUM, MESH

    // Gravel pack details (if applicable)
    private String gravelSize;
    private BigDecimal gravelPackLength;

    // Tubing configuration
    private BigDecimal tubingIdIn;
    private BigDecimal tubingOdIn;
    private BigDecimal tubingDepthM;
    private String tubingMaterial;

    // Production data
    private BigDecimal productivityIndexBpdPsi;
    private BigDecimal skinFactor;
    private BigDecimal flowEfficiency;
    private BigDecimal currentContributionPercent;

    // Stimulation history
    private Boolean hasBeenAcidized;
    private Boolean hasBeenFractured;
    private Long lastStimulationDate;
    private String lastStimulationType;

    // Status
    private String completionStatus;     // ACTIVE, SHUT_IN, SQUEEZED, ABANDONED
    private Boolean isIsolated;
    private String isolationMethod;      // PACKER, PLUG, CEMENT

    // Artificial lift (if applicable)
    private String liftMethod;           // NATURAL, ESP, SRP, GAS_LIFT, PCP
    private BigDecimal liftEquipmentDepthM;

    // For ESP
    private String espModel;
    private BigDecimal espFrequencyHz;
    private BigDecimal espStages;

    // For Gas Lift
    private Integer gasLiftValveCount;
    private BigDecimal operatingValveDepthM;
    private BigDecimal gasInjectionRateMscfd;

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "rv_completion";
    public static final String ATTR_COMPLETION_CODE = "completion_code";
    public static final String ATTR_COMPLETION_NUMBER = "completion_number";
    public static final String ATTR_COMPLETION_DATE = "completion_date";
    public static final String ATTR_COMPLETION_TYPE = "completion_type";
    public static final String ATTR_TOP_DEPTH_MD_M = "top_depth_md_m";
    public static final String ATTR_BOTTOM_DEPTH_MD_M = "bottom_depth_md_m";
    public static final String ATTR_PERFORATION_SPF = "perforation_shots_per_foot";
    public static final String ATTR_TUBING_ID_IN = "tubing_id_in";
    public static final String ATTR_PRODUCTIVITY_INDEX = "productivity_index_bpd_psi";
    public static final String ATTR_SKIN_FACTOR = "skin_factor";
    public static final String ATTR_COMPLETION_STATUS = "completion_status";
    public static final String ATTR_LIFT_METHOD = "lift_method";
}
