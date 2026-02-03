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
 * DTO for Well Log Run assets in the Reservoir Module.
 * Represents a single wireline or LWD logging run in a well.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RvWellLogRunDto {

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

    private UUID completionId;      // If associated with specific completion

    // Run Information
    @PositiveOrZero(message = "El número de corrida debe ser >= 0")
    private Integer runNumber;      // Run sequence number

    @Size(max = 50, message = "La fecha de corrida no debe exceder 50 caracteres")
    private String runDate;         // Date of logging run

    @Size(max = 100, message = "La compañía de registros no debe exceder 100 caracteres")
    private String loggingCompany;  // Service company

    @Size(max = 100, message = "El ingeniero de registros no debe exceder 100 caracteres")
    private String loggingEngineer;

    // Logging Type
    @Size(max = 50, message = "El tipo de registro no debe exceder 50 caracteres")
    private String loggingType;     // WIRELINE, LWD, MWD, MEMORY

    @Size(max = 255, message = "La sarta de herramientas no debe exceder 255 caracteres")
    private String toolString;      // Tool combination used

    @Size(max = 50, message = "La unidad de registro no debe exceder 50 caracteres")
    private String loggingUnit;     // Unit identifier

    // Depth Information (all in meters)
    @PositiveOrZero(message = "La profundidad superior MD debe ser >= 0")
    private BigDecimal topDepthMd;      // Top logged depth (MD)

    @PositiveOrZero(message = "La profundidad inferior MD debe ser >= 0")
    private BigDecimal bottomDepthMd;   // Bottom logged depth (MD)

    @PositiveOrZero(message = "La profundidad superior TVD debe ser >= 0")
    private BigDecimal topDepthTvd;     // Top logged depth (TVD)

    @PositiveOrZero(message = "La profundidad inferior TVD debe ser >= 0")
    private BigDecimal bottomDepthTvd;  // Bottom logged depth (TVD)

    @PositiveOrZero(message = "El intervalo registrado debe ser >= 0")
    private BigDecimal intervalLogged;  // Total interval logged (m)

    // Depth Reference
    @Size(max = 10, message = "La referencia de profundidad no debe exceder 10 caracteres")
    private String depthReference;      // KB, RT, GL, MSL

    private BigDecimal depthReferenceElevation; // Elevation of reference (m)

    // Hole Conditions
    @Positive(message = "El tamaño del hoyo debe ser > 0")
    private BigDecimal holeSize;        // Hole diameter (inches)

    @Size(max = 50, message = "El tipo de lodo no debe exceder 50 caracteres")
    private String mudType;             // OBM, WBM, SBM, AIR

    @Positive(message = "El peso del lodo debe ser > 0")
    private BigDecimal mudWeight;       // Mud weight (ppg)

    @Positive(message = "La temperatura máxima debe ser > 0")
    private BigDecimal maxTemp;         // Maximum temperature (°F)

    @Positive(message = "La temperatura estática debe ser > 0")
    private BigDecimal staticTemp;      // Static temperature (°F)

    // Curves Acquired
    private List<String> curvesAcquired; // GR, RHOB, NPHI, RT, etc.

    @PositiveOrZero(message = "El conteo de curvas debe ser >= 0")
    private Integer curveCount;

    @Positive(message = "La tasa de muestreo debe ser > 0")
    private BigDecimal sampleRate;      // Sampling rate (ft or m)

    // Quality
    @Size(max = 50, message = "La calidad general no debe exceder 50 caracteres")
    private String overallQuality;      // EXCELLENT, GOOD, FAIR, POOR

    private Boolean repeatSectionRun;

    @PositiveOrZero(message = "La profundidad de sección repetida debe ser >= 0")
    private BigDecimal repeatSectionDepth;

    @Size(max = 500, message = "Los problemas de calidad no deben exceder 500 caracteres")
    private String qualityIssues;       // Known quality problems

    // Environmental Corrections
    private Boolean boreholeCorrectionsApplied;
    private Boolean mudFilterateCorrected;
    private Boolean temperatureCorrected;

    // Interpretation Status
    private Boolean interpreted;

    @Size(max = 100, message = "El intérprete no debe exceder 100 caracteres")
    private String interpreter;

    @Size(max = 50, message = "La fecha de interpretación no debe exceder 50 caracteres")
    private String interpretationDate;

    // Calculated Parameters (from interpretation)
    @DecimalMin(value = "0.0", message = "La porosidad promedio debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La porosidad promedio debe ser <= 1")
    private BigDecimal avgPorosity;

    @DecimalMin(value = "0.0", message = "La saturación de agua promedio debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La saturación de agua promedio debe ser <= 1")
    private BigDecimal avgWaterSat;

    @DecimalMin(value = "0.0", message = "El volumen de arcilla promedio debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El volumen de arcilla promedio debe ser <= 1")
    private BigDecimal avgVshale;

    @PositiveOrZero(message = "El espesor neto petrolífero debe ser >= 0")
    private BigDecimal netPayThickness;

    @PositiveOrZero(message = "El espesor bruto debe ser >= 0")
    private BigDecimal grossThickness;

    @DecimalMin(value = "0.0", message = "La relación net-to-gross debe ser >= 0")
    @DecimalMax(value = "1.0", message = "La relación net-to-gross debe ser <= 1")
    private BigDecimal netToGross;

    // File References
    private String lasFilePath;         // LAS file path
    private String dlisFilePath;        // DLIS file path
    private String pdfReportPath;       // PDF log report

    // Venezuela-specific
    private Boolean fajaFormation;      // Logged through Faja formations
    private Boolean heavyOilZone;       // Encountered heavy oil

    // Extended metadata
    private JsonNode metadata;

    // Timestamps
    private Long createdTime;
    private Long updatedTime;

    // Logging Type Constants
    public static final String LOG_WIRELINE = "WIRELINE";
    public static final String LOG_LWD = "LWD";
    public static final String LOG_MWD = "MWD";
    public static final String LOG_MEMORY = "MEMORY";
    public static final String LOG_PIPE_CONVEYED = "PIPE_CONVEYED";

    // Common Curve Names
    public static final String CURVE_GR = "GR";         // Gamma Ray
    public static final String CURVE_RHOB = "RHOB";     // Bulk Density
    public static final String CURVE_NPHI = "NPHI";     // Neutron Porosity
    public static final String CURVE_RT = "RT";         // Deep Resistivity
    public static final String CURVE_RXOD = "RXOD";     // Medium Resistivity
    public static final String CURVE_RS = "RS";         // Shallow Resistivity
    public static final String CURVE_DT = "DT";         // Sonic
    public static final String CURVE_SP = "SP";         // Spontaneous Potential
    public static final String CURVE_CALI = "CALI";     // Caliper
    public static final String CURVE_PEF = "PEF";       // Photoelectric Factor
}
