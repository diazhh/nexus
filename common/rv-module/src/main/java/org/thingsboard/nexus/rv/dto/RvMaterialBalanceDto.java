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

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 255, message = "El nombre no debe exceder 255 caracteres")
    private String name;

    @Size(max = 255, message = "La descripción no debe exceder 255 caracteres")
    private String description;

    // Association
    @NotNull(message = "El ID del yacimiento es requerido")
    private UUID reservoirId;

    private UUID fieldId;

    // Reservoir Type
    @Size(max = 50, message = "El tipo de yacimiento no debe exceder 50 caracteres")
    private String reservoirType;  // OIL, GAS, GAS_CONDENSATE, VOLATILE_OIL

    // Initial Conditions
    @Positive(message = "La presión inicial debe ser > 0")
    private BigDecimal initialPressure;      // Pi (psia)

    @PositiveOrZero(message = "La presión de burbuja debe ser >= 0")
    private BigDecimal bubblePointPressure;  // Pb (psia)

    @Positive(message = "La temperatura inicial debe ser > 0")
    private BigDecimal initialTemperature;   // Ti (°F)

    @DecimalMin(value = "0.0", message = "La saturación de agua inicial debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La saturación de agua inicial debe ser <= 1")
    private BigDecimal initialWaterSaturation; // Swi (fraction)

    @DecimalMin(value = "0.0", message = "La saturación de agua connata debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La saturación de agua connata debe ser <= 1")
    private BigDecimal connateWaterSaturation; // Swc (fraction)

    // Initial PVT Properties
    @Positive(message = "El Bo inicial debe ser > 0")
    private BigDecimal initialBo;            // Boi (bbl/STB)

    @Positive(message = "El Bg inicial debe ser > 0")
    private BigDecimal initialBg;            // Bgi (cf/scf)

    @PositiveOrZero(message = "El Rs inicial debe ser >= 0")
    private BigDecimal initialRs;            // Rsi (scf/STB)

    @Positive(message = "El Bw inicial debe ser > 0")
    private BigDecimal initialBw;            // Bwi (bbl/STB)

    // Compressibilities
    @PositiveOrZero(message = "La compresibilidad del aceite debe ser >= 0")
    private BigDecimal oilCompressibility;   // co (1/psi)

    @PositiveOrZero(message = "La compresibilidad del agua debe ser >= 0")
    private BigDecimal waterCompressibility; // cw (1/psi)

    @PositiveOrZero(message = "La compresibilidad de la roca debe ser >= 0")
    private BigDecimal rockCompressibility;  // cf (1/psi)

    // Aquifer Parameters (for water drive)
    private Boolean hasAquiferSupport;

    @Size(max = 100, message = "El modelo de acuífero no debe exceder 100 caracteres")
    private String aquiferModel;             // INFINITE_LINEAR, RADIAL_FINITE, VAN_EVERDINGEN_HURST, FETKOVICH, CARTER_TRACY

    @PositiveOrZero(message = "El volumen del acuífero debe ser >= 0")
    private BigDecimal aquiferVolume;        // Aquifer volume (MMbbl or Bcf)

    @PositiveOrZero(message = "La permeabilidad del acuífero debe ser >= 0")
    private BigDecimal aquiferPermeability;  // k (md)

    @DecimalMin(value = "0.0", message = "La porosidad del acuífero debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La porosidad del acuífero debe ser <= 1")
    private BigDecimal aquiferPorosity;      // φ (fraction)

    @PositiveOrZero(message = "El espesor del acuífero debe ser >= 0")
    private BigDecimal aquiferThickness;     // h (ft)

    @PositiveOrZero(message = "El radio del acuífero debe ser >= 0")
    private BigDecimal aquiferRadius;        // ra (ft) - outer radius

    @PositiveOrZero(message = "El radio del yacimiento debe ser >= 0")
    private BigDecimal reservoirRadius;      // re (ft) - inner radius

    @DecimalMin(value = "0.0", message = "El ángulo del acuífero debe ser >= 0")
    @DecimalMax(value = "360.0", message = "El ángulo del acuífero debe ser <= 360")
    private BigDecimal aquiferAngle;         // θ (degrees) - encroachment angle

    // Gas Cap Parameters
    private Boolean hasGasCap;

    @PositiveOrZero(message = "La relación del casquete de gas debe ser >= 0")
    private BigDecimal gasCapRatio;          // m = GBgi/(NBoi) ratio

    // Calculated OOIP/OGIP
    @PositiveOrZero(message = "El OOIP calculado debe ser >= 0")
    private BigDecimal calculatedOOIP;       // N (MMSTB)

    @PositiveOrZero(message = "El OGIP calculado debe ser >= 0")
    private BigDecimal calculatedOGIP;       // G (Bcf)

    @PositiveOrZero(message = "El volumen del casquete de gas debe ser >= 0")
    private BigDecimal calculatedGasCapVolume; // GBgi (MMbbl)

    // Drive Mechanism Analysis
    @Size(max = 50, message = "El mecanismo de empuje primario no debe exceder 50 caracteres")
    private String primaryDriveMechanism;    // SOLUTION_GAS, GAS_CAP, WATER_DRIVE, COMPACTION, GRAVITY, COMBINATION

    @DecimalMin(value = "0.0", message = "El índice de empuje por gas en solución debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El índice de empuje por gas en solución debe ser <= 1")
    private BigDecimal solutionGasDriveIndex;  // DDI - Depletion Drive Index

    @DecimalMin(value = "0.0", message = "El índice de empuje por casquete de gas debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El índice de empuje por casquete de gas debe ser <= 1")
    private BigDecimal gasCapDriveIndex;       // SDI - Segregation Drive Index

    @DecimalMin(value = "0.0", message = "El índice de empuje por agua debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El índice de empuje por agua debe ser <= 1")
    private BigDecimal waterDriveIndex;        // WDI - Water Drive Index

    @DecimalMin(value = "0.0", message = "El índice de empuje por compactación debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El índice de empuje por compactación debe ser <= 1")
    private BigDecimal compactionDriveIndex;   // CDI - Compaction Drive Index

    // Regression Results (Havlena-Odeh)
    private BigDecimal regressionSlope;
    private BigDecimal regressionIntercept;

    @DecimalMin(value = "0.0", message = "El coeficiente R² de regresión debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El coeficiente R² de regresión debe ser <= 1")
    private BigDecimal regressionR2;         // Coefficient of determination

    @Size(max = 50, message = "El tipo de gráfica no debe exceder 50 caracteres")
    private String plotType;                 // F_vs_Eo, F_vs_EoEg, F_Eo_vs_EwEf

    // Analysis Quality
    @Size(max = 50, message = "La calidad del análisis no debe exceder 50 caracteres")
    private String analysisQuality;          // EXCELLENT, GOOD, FAIR, POOR

    @Size(max = 500, message = "Las notas del análisis no deben exceder 500 caracteres")
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
