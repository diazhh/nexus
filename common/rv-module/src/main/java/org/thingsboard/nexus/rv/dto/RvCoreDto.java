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

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Core assets in the Reservoir Module.
 * Represents a core sample taken from a well with associated analysis data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RvCoreDto {

    private UUID id;

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 255, message = "El nombre no debe exceder 255 caracteres")
    private String name;

    @Size(max = 255, message = "La descripción no debe exceder 255 caracteres")
    private String description;

    // Association
    @NotNull(message = "El ID del pozo es requerido")
    private UUID wellId;            // Parent well

    private UUID reservoirId;       // Reservoir interval

    // Core Information
    @PositiveOrZero(message = "El número de núcleo debe ser >= 0")
    private Integer coreNumber;     // Core sequence number in well

    @Size(max = 50, message = "La fecha de núcleo no debe exceder 50 caracteres")
    private String coreDate;        // Date core was cut

    @Size(max = 50, message = "El tipo de núcleo no debe exceder 50 caracteres")
    private String coreType;        // CONVENTIONAL, SIDEWALL, ROTARY_SIDEWALL

    // Depth Information (in meters)
    @PositiveOrZero(message = "La profundidad superior MD debe ser >= 0")
    private BigDecimal topDepthMd;      // Top depth (MD)

    @PositiveOrZero(message = "La profundidad inferior MD debe ser >= 0")
    private BigDecimal bottomDepthMd;   // Bottom depth (MD)

    @PositiveOrZero(message = "La profundidad superior TVD debe ser >= 0")
    private BigDecimal topDepthTvd;     // Top depth (TVD)

    @PositiveOrZero(message = "La profundidad inferior TVD debe ser >= 0")
    private BigDecimal bottomDepthTvd;  // Bottom depth (TVD)

    @PositiveOrZero(message = "La longitud cortada debe ser >= 0")
    private BigDecimal cutLength;       // Total cut length (m)

    @PositiveOrZero(message = "La longitud recuperada debe ser >= 0")
    private BigDecimal recoveredLength; // Recovered length (m)

    @DecimalMin(value = "0.0", message = "La recuperación debe ser >= 0")
    @DecimalMax(value = "100.0", message = "La recuperación debe ser <= 100")
    private BigDecimal recovery;        // Recovery percentage

    // Formation
    @Size(max = 100, message = "El nombre de formación no debe exceder 100 caracteres")
    private String formationName;

    @Size(max = 50, message = "La litología primaria no debe exceder 50 caracteres")
    private String lithologyPrimary;    // Main lithology

    @Size(max = 50, message = "La litología secundaria no debe exceder 50 caracteres")
    private String lithologySecondary;  // Secondary lithology

    @Size(max = 50, message = "El tamaño de grano no debe exceder 50 caracteres")
    private String grainSize;           // FINE, MEDIUM, COARSE, etc.

    @Size(max = 50, message = "El sorting no debe exceder 50 caracteres")
    private String sorting;             // WELL_SORTED, MODERATELY_SORTED, POORLY_SORTED

    @Size(max = 50, message = "La cementación no debe exceder 50 caracteres")
    private String cementation;         // UNCONSOLIDATED, WEAKLY, MODERATELY, STRONGLY

    // Visual Description
    @Size(max = 50, message = "El oil show no debe exceder 50 caracteres")
    private String oilShow;             // NONE, STAIN, BLEEDING, SATURATED

    @Size(max = 50, message = "La fluorescencia no debe exceder 50 caracteres")
    private String fluorescence;        // NONE, SPOTTY, PATCHY, UNIFORM

    @Size(max = 50, message = "El olor del aceite no debe exceder 50 caracteres")
    private String oilOdor;             // NONE, SLIGHT, MODERATE, STRONG

    @Size(max = 50, message = "El color del aceite no debe exceder 50 caracteres")
    private String oilColor;

    // Routine Core Analysis (RCA) Results
    private Boolean rcaCompleted;

    @Size(max = 100, message = "El laboratorio RCA no debe exceder 100 caracteres")
    private String rcaLaboratory;

    @Size(max = 50, message = "La fecha RCA no debe exceder 50 caracteres")
    private String rcaDate;

    @DecimalMin(value = "0.0", message = "La porosidad promedio debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La porosidad promedio debe ser <= 1")
    private BigDecimal avgPorosity;         // Average porosity (fraction)

    @PositiveOrZero(message = "La permeabilidad horizontal debe ser >= 0")
    private BigDecimal avgPermeabilityH;    // Horizontal permeability (mD)

    @PositiveOrZero(message = "La permeabilidad vertical debe ser >= 0")
    private BigDecimal avgPermeabilityV;    // Vertical permeability (mD)

    @Positive(message = "La densidad del grano debe ser > 0")
    private BigDecimal avgGrainDensity;     // Grain density (g/cc)

    @DecimalMin(value = "0.0", message = "La saturación de agua debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La saturación de agua debe ser <= 1")
    private BigDecimal avgWaterSat;         // Residual water saturation

    // Special Core Analysis (SCAL) Results
    private Boolean scalCompleted;

    @Size(max = 100, message = "El laboratorio SCAL no debe exceder 100 caracteres")
    private String scalLaboratory;

    @Size(max = 50, message = "La fecha SCAL no debe exceder 50 caracteres")
    private String scalDate;

    @PositiveOrZero(message = "La presión capilar de entrada debe ser >= 0")
    private BigDecimal capillaryPressureEntry;  // Psi

    @DecimalMin(value = "0.0", message = "La saturación de agua irreducible debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La saturación de agua irreducible debe ser <= 1")
    private BigDecimal irreducibleWaterSat;     // Swirr (fraction)

    @DecimalMin(value = "0.0", message = "La saturación de aceite residual debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La saturación de aceite residual debe ser <= 1")
    private BigDecimal residualOilSat;          // Sor (fraction)

    @DecimalMin(value = "0.0", message = "El endpoint de perm relativa al agua debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El endpoint de perm relativa al agua debe ser <= 1")
    private BigDecimal relPermEndpointWater;    // krw at Sor

    @DecimalMin(value = "0.0", message = "El endpoint de perm relativa al aceite debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El endpoint de perm relativa al aceite debe ser <= 1")
    private BigDecimal relPermEndpointOil;      // kro at Swirr

    @DecimalMin(value = "-1.0", message = "El índice de mojabilidad debe ser >= -1")
    @DecimalMax(value = "1.0", message = "El índice de mojabilidad debe ser <= 1")
    private BigDecimal wettability;             // Amott-Harvey index

    @Size(max = 50, message = "La clase de mojabilidad no debe exceder 50 caracteres")
    private String wettabilityClass;            // WATER_WET, OIL_WET, MIXED_WET

    // Geomechanical Properties
    private Boolean geomechCompleted;

    @PositiveOrZero(message = "El módulo de Young debe ser >= 0")
    private BigDecimal youngModulus;        // GPa

    @DecimalMin(value = "0.0", message = "La relación de Poisson debe ser >= 0")
    @DecimalMax(value = "0.5", message = "La relación de Poisson debe ser <= 0.5")
    private BigDecimal poissonRatio;

    @PositiveOrZero(message = "La resistencia uniaxial debe ser >= 0")
    private BigDecimal uniaxialStrength;    // MPa

    @DecimalMin(value = "0.0", message = "El ángulo de fricción debe ser >= 0")
    @DecimalMax(value = "90.0", message = "El ángulo de fricción debe ser <= 90")
    private BigDecimal frictionAngle;       // degrees

    // Storage Location
    @Size(max = 255, message = "La ubicación de almacenamiento no debe exceder 255 caracteres")
    private String storageLocation;

    @Size(max = 100, message = "Los números de caja no deben exceder 100 caracteres")
    private String boxNumbers;              // Core box numbers

    private Boolean photographed;
    private Boolean ctScanned;

    // Venezuela-specific
    private Boolean heavyOilCore;

    @DecimalMin(value = "0.0", message = "El contenido de arena debe ser >= 0")
    @DecimalMax(value = "100.0", message = "El contenido de arena debe ser <= 100")
    private BigDecimal sandContent;         // For CHOPS evaluation

    private Boolean foamyOilTest;

    @PositiveOrZero(message = "El factor de aceite espumoso debe ser >= 0")
    private BigDecimal foamyOilFactor;

    // File References
    private String corePhotosPath;
    private String coreReportPath;
    private String rcaReportPath;
    private String scalReportPath;

    // Extended metadata
    private JsonNode metadata;

    // Timestamps
    private Long createdTime;
    private Long updatedTime;

    // Core Type Constants
    public static final String CORE_CONVENTIONAL = "CONVENTIONAL";
    public static final String CORE_SIDEWALL = "SIDEWALL";
    public static final String CORE_ROTARY_SIDEWALL = "ROTARY_SIDEWALL";
    public static final String CORE_ORIENTED = "ORIENTED";

    // Oil Show Constants
    public static final String SHOW_NONE = "NONE";
    public static final String SHOW_STAIN = "STAIN";
    public static final String SHOW_BLEEDING = "BLEEDING";
    public static final String SHOW_SATURATED = "SATURATED";
}
