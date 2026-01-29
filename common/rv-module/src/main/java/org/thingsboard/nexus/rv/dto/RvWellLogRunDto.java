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
 * DTO for Well Log Run assets in the Reservoir Module.
 * Represents a single wireline or LWD logging run in a well.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RvWellLogRunDto {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;

    // Association
    private UUID wellId;            // Parent well
    private UUID completionId;      // If associated with specific completion

    // Run Information
    private Integer runNumber;      // Run sequence number
    private String runDate;         // Date of logging run
    private String loggingCompany;  // Service company
    private String loggingEngineer;

    // Logging Type
    private String loggingType;     // WIRELINE, LWD, MWD, MEMORY
    private String toolString;      // Tool combination used
    private String loggingUnit;     // Unit identifier

    // Depth Information (all in meters)
    private BigDecimal topDepthMd;      // Top logged depth (MD)
    private BigDecimal bottomDepthMd;   // Bottom logged depth (MD)
    private BigDecimal topDepthTvd;     // Top logged depth (TVD)
    private BigDecimal bottomDepthTvd;  // Bottom logged depth (TVD)
    private BigDecimal intervalLogged;  // Total interval logged (m)

    // Depth Reference
    private String depthReference;      // KB, RT, GL, MSL
    private BigDecimal depthReferenceElevation; // Elevation of reference (m)

    // Hole Conditions
    private BigDecimal holeSize;        // Hole diameter (inches)
    private String mudType;             // OBM, WBM, SBM, AIR
    private BigDecimal mudWeight;       // Mud weight (ppg)
    private BigDecimal maxTemp;         // Maximum temperature (°F)
    private BigDecimal staticTemp;      // Static temperature (°F)

    // Curves Acquired
    private List<String> curvesAcquired; // GR, RHOB, NPHI, RT, etc.
    private Integer curveCount;
    private BigDecimal sampleRate;      // Sampling rate (ft or m)

    // Quality
    private String overallQuality;      // EXCELLENT, GOOD, FAIR, POOR
    private Boolean repeatSectionRun;
    private BigDecimal repeatSectionDepth;
    private String qualityIssues;       // Known quality problems

    // Environmental Corrections
    private Boolean boreholeCorrectionsApplied;
    private Boolean mudFilterateCorrected;
    private Boolean temperatureCorrected;

    // Interpretation Status
    private Boolean interpreted;
    private String interpreter;
    private String interpretationDate;

    // Calculated Parameters (from interpretation)
    private BigDecimal avgPorosity;
    private BigDecimal avgWaterSat;
    private BigDecimal avgVshale;
    private BigDecimal netPayThickness;
    private BigDecimal grossThickness;
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
