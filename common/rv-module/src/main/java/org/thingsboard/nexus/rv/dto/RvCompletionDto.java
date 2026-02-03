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

import jakarta.validation.constraints.*;
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

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    private String name;

    private String label;

    // Hierarchy
    @NotNull(message = "El pozo (well) es requerido")
    private UUID wellAssetId;

    private UUID zoneAssetId;            // Target zone

    // Completion identification
    @Size(max = 50, message = "El código de completación no debe exceder 50 caracteres")
    private String completionCode;

    @Positive(message = "El número de completación debe ser > 0")
    private Integer completionNumber;    // 1, 2, 3 for multiple completions

    private Long completionDate;

    @Size(max = 50, message = "El tipo de completación no debe exceder 50 caracteres")
    private String completionType;       // OPENHOLE, CASED_PERFORATED, GRAVEL_PACK, FRAC_PACK, SLOTTED_LINER

    // Interval details
    @PositiveOrZero(message = "La profundidad superior MD debe ser >= 0")
    private BigDecimal topDepthMdM;

    @PositiveOrZero(message = "La profundidad inferior MD debe ser >= 0")
    private BigDecimal bottomDepthMdM;

    @PositiveOrZero(message = "La profundidad superior TVD debe ser >= 0")
    private BigDecimal topDepthTvdM;

    @PositiveOrZero(message = "La profundidad inferior TVD debe ser >= 0")
    private BigDecimal bottomDepthTvdM;

    @PositiveOrZero(message = "La longitud del intervalo debe ser >= 0")
    private BigDecimal intervalLengthM;

    // Perforation details (if cased-perforated)
    @PositiveOrZero(message = "Los disparos por pie deben ser >= 0")
    private Integer perforationShotsPerFoot;

    @DecimalMin(value = "0.0", message = "El faseo debe ser >= 0")
    @DecimalMax(value = "360.0", message = "El faseo debe ser <= 360")
    private BigDecimal perforationPhasingDeg;

    @Positive(message = "El diámetro de perforación debe ser > 0")
    private BigDecimal perforationDiameterIn;

    @Positive(message = "La penetración debe ser > 0")
    private BigDecimal perforationPenetrationIn;

    @PositiveOrZero(message = "El total de perforaciones debe ser >= 0")
    private Integer totalPerforations;

    // Screen/Liner details (if applicable)
    private BigDecimal screenSlotSizeIn;
    private String screenType;           // WIRE_WRAPPED, PREMIUM, MESH

    // Gravel pack details (if applicable)
    private String gravelSize;
    private BigDecimal gravelPackLength;

    // Tubing configuration
    @Positive(message = "El ID del tubing debe ser > 0")
    private BigDecimal tubingIdIn;

    @Positive(message = "El OD del tubing debe ser > 0")
    private BigDecimal tubingOdIn;

    @PositiveOrZero(message = "La profundidad del tubing debe ser >= 0")
    private BigDecimal tubingDepthM;

    @Size(max = 50, message = "El material del tubing no debe exceder 50 caracteres")
    private String tubingMaterial;

    // Production data
    @PositiveOrZero(message = "El índice de productividad debe ser >= 0")
    private BigDecimal productivityIndexBpdPsi;

    private BigDecimal skinFactor;

    @DecimalMin(value = "0.0", message = "La eficiencia de flujo debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La eficiencia de flujo debe ser <= 1")
    private BigDecimal flowEfficiency;

    @DecimalMin(value = "0.0", message = "La contribución debe ser >= 0")
    @DecimalMax(value = "100.0", message = "La contribución debe ser <= 100")
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
    public static final String ASSET_TYPE = "RV_COMPLETION";
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
