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
import java.util.List;
import java.util.UUID;

/**
 * DTO for Material Balance Study in the Reservoir Module.
 * Implements the Havlena-Odeh method for OOIP/OGIP estimation
 * and drive mechanism identification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RvMaterialBalanceDto {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;

    // Association
    private UUID reservoirId;
    private UUID fieldId;

    // Reservoir Type
    private String reservoirType;  // OIL, GAS, GAS_CONDENSATE, VOLATILE_OIL

    // Initial Conditions
    private BigDecimal initialPressure;      // Pi (psia)
    private BigDecimal bubblePointPressure;  // Pb (psia)
    private BigDecimal initialTemperature;   // Ti (°F)
    private BigDecimal initialWaterSaturation; // Swi (fraction)
    private BigDecimal connateWaterSaturation; // Swc (fraction)

    // Initial PVT Properties
    private BigDecimal initialBo;            // Boi (bbl/STB)
    private BigDecimal initialBg;            // Bgi (cf/scf)
    private BigDecimal initialRs;            // Rsi (scf/STB)
    private BigDecimal initialBw;            // Bwi (bbl/STB)

    // Compressibilities
    private BigDecimal oilCompressibility;   // co (1/psi)
    private BigDecimal waterCompressibility; // cw (1/psi)
    private BigDecimal rockCompressibility;  // cf (1/psi)

    // Aquifer Parameters (for water drive)
    private Boolean hasAquiferSupport;
    private String aquiferModel;             // INFINITE_LINEAR, RADIAL_FINITE, VAN_EVERDINGEN_HURST, FETKOVICH, CARTER_TRACY
    private BigDecimal aquiferVolume;        // Aquifer volume (MMbbl or Bcf)
    private BigDecimal aquiferPermeability;  // k (md)
    private BigDecimal aquiferPorosity;      // φ (fraction)
    private BigDecimal aquiferThickness;     // h (ft)
    private BigDecimal aquiferRadius;        // ra (ft) - outer radius
    private BigDecimal reservoirRadius;      // re (ft) - inner radius
    private BigDecimal aquiferAngle;         // θ (degrees) - encroachment angle

    // Gas Cap Parameters
    private Boolean hasGasCap;
    private BigDecimal gasCapRatio;          // m = GBgi/(NBoi) ratio

    // Calculated OOIP/OGIP
    private BigDecimal calculatedOOIP;       // N (MMSTB)
    private BigDecimal calculatedOGIP;       // G (Bcf)
    private BigDecimal calculatedGasCapVolume; // GBgi (MMbbl)

    // Drive Mechanism Analysis
    private String primaryDriveMechanism;    // SOLUTION_GAS, GAS_CAP, WATER_DRIVE, COMPACTION, GRAVITY, COMBINATION
    private BigDecimal solutionGasDriveIndex;  // DDI - Depletion Drive Index
    private BigDecimal gasCapDriveIndex;       // SDI - Segregation Drive Index
    private BigDecimal waterDriveIndex;        // WDI - Water Drive Index
    private BigDecimal compactionDriveIndex;   // CDI - Compaction Drive Index

    // Regression Results (Havlena-Odeh)
    private BigDecimal regressionSlope;
    private BigDecimal regressionIntercept;
    private BigDecimal regressionR2;         // Coefficient of determination
    private String plotType;                 // F_vs_Eo, F_vs_EoEg, F_Eo_vs_EwEf

    // Analysis Quality
    private String analysisQuality;          // EXCELLENT, GOOD, FAIR, POOR
    private String analysisNotes;

    // Study Information
    private String analyst;
    private String analysisDate;
    private String softwareUsed;

    // Time Series Data Points (for plotting)
    private List<MaterialBalanceDataPoint> dataPoints;

    // Extended metadata (JSON)
    private JsonNode metadata;

    // Timestamps
    private Long createdTime;
    private Long updatedTime;

    /**
     * Individual data point for material balance analysis
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialBalanceDataPoint {
        private String date;
        private BigDecimal pressure;         // P (psia)
        private BigDecimal cumulativeOilProduction;   // Np (MMSTB)
        private BigDecimal cumulativeGasProduction;   // Gp (Bcf)
        private BigDecimal cumulativeWaterProduction; // Wp (MMbbl)
        private BigDecimal cumulativeWaterInjection;  // Wi (MMbbl)
        private BigDecimal cumulativeGasInjection;    // Gi (Bcf)

        // PVT at this pressure
        private BigDecimal Bo;               // Oil FVF (bbl/STB)
        private BigDecimal Bg;               // Gas FVF (cf/scf)
        private BigDecimal Rs;               // Solution GOR (scf/STB)
        private BigDecimal Bw;               // Water FVF (bbl/STB)

        // Calculated MBE terms
        private BigDecimal F;                // Underground withdrawal (bbl)
        private BigDecimal Eo;               // Oil expansion (bbl/STB)
        private BigDecimal Eg;               // Gas cap expansion (bbl/scf)
        private BigDecimal Efw;              // Formation and water expansion (bbl/STB)
        private BigDecimal We;               // Water influx (bbl)

        // For Havlena-Odeh plot
        private BigDecimal xAxis;            // Depending on plot type
        private BigDecimal yAxis;            // F or F/Eo
    }

    // Reservoir Type Constants
    public static final String TYPE_OIL = "OIL";
    public static final String TYPE_GAS = "GAS";
    public static final String TYPE_GAS_CONDENSATE = "GAS_CONDENSATE";
    public static final String TYPE_VOLATILE_OIL = "VOLATILE_OIL";

    // Drive Mechanism Constants
    public static final String DRIVE_SOLUTION_GAS = "SOLUTION_GAS";
    public static final String DRIVE_GAS_CAP = "GAS_CAP";
    public static final String DRIVE_WATER = "WATER_DRIVE";
    public static final String DRIVE_COMPACTION = "COMPACTION";
    public static final String DRIVE_GRAVITY = "GRAVITY";
    public static final String DRIVE_COMBINATION = "COMBINATION";

    // Aquifer Model Constants
    public static final String AQUIFER_INFINITE_LINEAR = "INFINITE_LINEAR";
    public static final String AQUIFER_RADIAL_FINITE = "RADIAL_FINITE";
    public static final String AQUIFER_VAN_EVERDINGEN_HURST = "VAN_EVERDINGEN_HURST";
    public static final String AQUIFER_FETKOVICH = "FETKOVICH";
    public static final String AQUIFER_CARTER_TRACY = "CARTER_TRACY";

    // Plot Type Constants
    public static final String PLOT_F_VS_EO = "F_vs_Eo";
    public static final String PLOT_F_VS_EO_EG = "F_vs_EoEg";
    public static final String PLOT_F_EO_VS_EW_EF = "F_Eo_vs_EwEf";
}
