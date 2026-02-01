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
    private UUID tenantId;
    private String name;
    private String label;

    // Hierarchy
    private UUID fieldAssetId;

    // Reservoir identification
    private String code;
    private String formationName;
    private String geologicAge;          // MIOCENE, OLIGOCENE, EOCENE, CRETACEOUS

    // Static properties (petrophysics)
    private BigDecimal averagePorosityFrac;    // φ (0-1)
    private BigDecimal averagePermeabilityMd;  // k (millidarcys)
    private BigDecimal netPayThicknessM;       // h (meters)
    private BigDecimal grossThicknessM;
    private BigDecimal netToGrossRatio;
    private BigDecimal averageSwFrac;          // Sw inicial (0-1)
    private BigDecimal averageVshFrac;         // Vsh (0-1)
    private BigDecimal areaKm2;

    // Rock properties
    private String lithology;                  // SANDSTONE, LIMESTONE, DOLOMITE, SHALE
    private BigDecimal rockCompressibilityCp;  // cf (1/psi)
    private BigDecimal grainDensityGcc;        // ρg (g/cc)

    // Initial conditions
    private BigDecimal initialPressurePsi;     // Pi
    private BigDecimal currentPressurePsi;     // P actual
    private BigDecimal bubblePointPressurePsi; // Pb
    private BigDecimal temperatureF;           // T
    private BigDecimal datumDepthM;            // Datum TVDss

    // Fluid properties (from PVT study)
    private String fluidType;                  // OIL, GAS, CONDENSATE, HEAVY_OIL, EXTRA_HEAVY_OIL
    private BigDecimal apiGravity;
    private BigDecimal oilViscosityCp;         // μo @ reservoir conditions
    private BigDecimal gasOilRatioScfStb;      // GOR
    private BigDecimal formationVolFactorBo;   // Bo
    private BigDecimal waterSaturationIrreducible; // Swirr

    // Volumetrics (OOIP)
    private BigDecimal ooipStb;                // Original Oil In Place
    private BigDecimal ogipScf;                // Original Gas In Place
    private BigDecimal recoveryFactorFrac;     // RF (0-1)
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
