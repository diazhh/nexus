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
 * DTO representing a Reservoir (Yacimiento) entity.
 * Maps to a ThingsBoard Asset of type "rv_reservoir" with SERVER_SCOPE attributes.
 *
 * Hierarchy: Basin -> Field -> Reservoir -> Zone
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RvReservoirDto {

    // Asset identity
    private UUID assetId;

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    private String name;

    private String label;

    // Hierarchy
    @NotNull(message = "El campo (field) es requerido")
    private UUID fieldAssetId;

    // Reservoir identification
    @Size(max = 50, message = "El código no debe exceder 50 caracteres")
    private String code;

    @Size(max = 100, message = "El nombre de formación no debe exceder 100 caracteres")
    private String formationName;
    private String geologicAge;          // MIOCENE, OLIGOCENE, EOCENE, CRETACEOUS

    // Static properties (petrophysics)
    @DecimalMin(value = "0.0", message = "La porosidad debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La porosidad debe ser <= 1")
    private BigDecimal averagePorosityFrac;    // φ (0-1)

    @PositiveOrZero(message = "La permeabilidad debe ser >= 0")
    private BigDecimal averagePermeabilityMd;  // k (millidarcys)

    @PositiveOrZero(message = "El espesor neto debe ser >= 0")
    private BigDecimal netPayThicknessM;       // h (meters)

    @PositiveOrZero(message = "El espesor bruto debe ser >= 0")
    private BigDecimal grossThicknessM;

    @DecimalMin(value = "0.0", message = "La relación net-to-gross debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La relación net-to-gross debe ser <= 1")
    private BigDecimal netToGrossRatio;

    @DecimalMin(value = "0.0", message = "La saturación de agua debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La saturación de agua debe ser <= 1")
    private BigDecimal averageSwFrac;          // Sw inicial (0-1)

    @DecimalMin(value = "0.0", message = "El volumen de arcilla debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El volumen de arcilla debe ser <= 1")
    private BigDecimal averageVshFrac;         // Vsh (0-1)

    @PositiveOrZero(message = "El área debe ser >= 0")
    private BigDecimal areaKm2;

    // Rock properties
    @Size(max = 50, message = "La litología no debe exceder 50 caracteres")
    private String lithology;                  // SANDSTONE, LIMESTONE, DOLOMITE, SHALE

    @PositiveOrZero(message = "La compresibilidad de la roca debe ser >= 0")
    private BigDecimal rockCompressibilityCp;  // cf (1/psi)

    @Positive(message = "La densidad del grano debe ser > 0")
    private BigDecimal grainDensityGcc;        // ρg (g/cc)

    // Initial conditions
    @PositiveOrZero(message = "La presión inicial debe ser >= 0")
    private BigDecimal initialPressurePsi;     // Pi

    @PositiveOrZero(message = "La presión actual debe ser >= 0")
    private BigDecimal currentPressurePsi;     // P actual

    @PositiveOrZero(message = "La presión de burbuja debe ser >= 0")
    private BigDecimal bubblePointPressurePsi; // Pb

    @Positive(message = "La temperatura debe ser > 0")
    private BigDecimal temperatureF;           // T

    private BigDecimal datumDepthM;            // Datum TVDss

    // Fluid properties (from PVT study)
    @Size(max = 50, message = "El tipo de fluido no debe exceder 50 caracteres")
    private String fluidType;                  // OIL, GAS, CONDENSATE, HEAVY_OIL, EXTRA_HEAVY_OIL

    @PositiveOrZero(message = "La gravedad API debe ser >= 0")
    @DecimalMax(value = "100.0", message = "La gravedad API debe ser <= 100")
    private BigDecimal apiGravity;

    @PositiveOrZero(message = "La viscosidad debe ser >= 0")
    private BigDecimal oilViscosityCp;         // μo @ reservoir conditions

    @PositiveOrZero(message = "El GOR debe ser >= 0")
    private BigDecimal gasOilRatioScfStb;      // GOR

    @Positive(message = "El factor volumétrico Bo debe ser > 0")
    private BigDecimal formationVolFactorBo;   // Bo

    @DecimalMin(value = "0.0", message = "La saturación irreducible debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La saturación irreducible debe ser <= 1")
    private BigDecimal waterSaturationIrreducible; // Swirr

    // Volumetrics (OOIP)
    @PositiveOrZero(message = "El OOIP debe ser >= 0")
    private BigDecimal ooipStb;                // Original Oil In Place

    @PositiveOrZero(message = "El OGIP debe ser >= 0")
    private BigDecimal ogipScf;                // Original Gas In Place

    @DecimalMin(value = "0.0", message = "El factor de recuperación debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El factor de recuperación debe ser <= 1")
    private BigDecimal recoveryFactorFrac;     // RF (0-1)

    @PositiveOrZero(message = "Las reservas recuperables deben ser >= 0")
    private BigDecimal recoverableReservesStb;

    // Drive mechanism
    private String primaryDriveMechanism;      // SOLUTION_GAS, WATER_DRIVE, GAS_CAP, GRAVITY_DRAINAGE, COMPACTION

    // Current production status (telemetry references)
    private BigDecimal currentOilRateBopd;
    private BigDecimal cumulativeProductionStb;
    private BigDecimal currentWaterCutFrac;
    private BigDecimal currentGorScfStb;

    // Aquifer (if water drive)
    private Boolean hasAquifer;
    private String aquiferModel;               // FETKOVICH, CARTER_TRACY, VAN_EVERDINGEN_HURST
    private BigDecimal aquiferVolumeFactor;

    // Venezuela-specific
    private Boolean isFoamyOil;
    private BigDecimal foamyOilFactor;
    private BigDecimal criticalGasSaturation;

    // Geomechanics reference
    private UUID geomechanicalModelAssetId;

    // Metadata
    private JsonNode additionalInfo;
    private Long createdTime;
    private Long updatedTime;

    // Constants for attribute keys
    public static final String ASSET_TYPE = "RV_RESERVOIR";
    public static final String ATTR_CODE = "code";
    public static final String ATTR_FORMATION_NAME = "formation_name";
    public static final String ATTR_GEOLOGIC_AGE = "geologic_age";
    public static final String ATTR_AVG_POROSITY_FRAC = "average_porosity_frac";
    public static final String ATTR_AVG_PERMEABILITY_MD = "average_permeability_md";
    public static final String ATTR_NET_PAY_THICKNESS_M = "net_pay_thickness_m";
    public static final String ATTR_INITIAL_PRESSURE_PSI = "initial_pressure_psi";
    public static final String ATTR_CURRENT_PRESSURE_PSI = "current_pressure_psi";
    public static final String ATTR_BUBBLE_POINT_PSI = "bubble_point_pressure_psi";
    public static final String ATTR_TEMPERATURE_F = "temperature_f";
    public static final String ATTR_FLUID_TYPE = "fluid_type";
    public static final String ATTR_API_GRAVITY = "api_gravity";
    public static final String ATTR_OOIP_STB = "ooip_stb";
    public static final String ATTR_RECOVERY_FACTOR = "recovery_factor_frac";
    public static final String ATTR_PRIMARY_DRIVE = "primary_drive_mechanism";
    public static final String ATTR_IS_FOAMY_OIL = "is_foamy_oil";
}
