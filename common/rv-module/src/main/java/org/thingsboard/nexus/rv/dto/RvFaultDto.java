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
 * DTO for Fault assets in the Reservoir Module.
 * Represents geological faults identified in seismic interpretation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RvFaultDto {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;

    // Association
    private UUID fieldId;
    private UUID reservoirId;
    private UUID seismicSurveyId;   // Source seismic survey

    // Fault Classification
    private String faultType;       // NORMAL, REVERSE, STRIKE_SLIP, LISTRIC, GROWTH
    private String faultSystem;     // Name of fault system if part of larger system
    private Integer faultOrder;     // Order within fault system (1=main, 2=secondary)

    // Geometry
    private BigDecimal strikeAzimuth;   // Strike direction (degrees from N)
    private BigDecimal dipAngle;        // Dip angle (degrees)
    private String dipDirection;        // N, NE, E, SE, S, SW, W, NW
    private BigDecimal lengthKm;        // Fault trace length (km)
    private BigDecimal throwM;          // Vertical throw (m)
    private BigDecimal heaveM;          // Horizontal displacement (m)

    // Depth Range
    private BigDecimal topDepthM;       // Shallowest point (m TVD)
    private BigDecimal baseDepthM;      // Deepest point (m TVD)

    // Sealing Properties
    private String sealingPotential;    // SEALING, PARTIALLY_SEALING, NON_SEALING
    private BigDecimal shaleSmeaerFactor;   // SGR - Shale Gouge Ratio
    private BigDecimal claySmearPotential;  // CSP
    private BigDecimal faultRockPermeability; // mD

    // Activity
    private Boolean activelyGrowing;    // For growth faults
    private String lastActivityAge;     // Geological age of last movement

    // Interpretation Confidence
    private String interpretationConfidence; // HIGH, MEDIUM, LOW
    private String dataSource;          // SEISMIC, WELL_CORRELATION, BOTH

    // Juxtaposition Analysis
    private Boolean juxtapositionAnalysisDone;
    private String juxtaposedFormations;    // Formations across fault

    // Compartmentalization
    private Boolean compartmentalizing;     // Does it compartmentalize reservoir?
    private String compartments;            // Names of compartments created

    // Well Penetrations
    private Integer wellsPenetratingFault;
    private String penetratingWells;        // Comma-separated well names

    // Venezuela-specific
    private Boolean fajaRelatedStructure;
    private String tectonicPhase;           // EXTENSIONAL, COMPRESSIONAL, WRENCH

    // Coordinates (fault polygon/polyline as GeoJSON)
    private JsonNode faultGeometry;         // GeoJSON LineString or Polygon
    private String coordinateSystem;

    // Extended metadata
    private JsonNode metadata;

    // Timestamps
    private Long createdTime;
    private Long updatedTime;

    // Fault Type Constants
    public static final String FAULT_NORMAL = "NORMAL";
    public static final String FAULT_REVERSE = "REVERSE";
    public static final String FAULT_STRIKE_SLIP = "STRIKE_SLIP";
    public static final String FAULT_LISTRIC = "LISTRIC";
    public static final String FAULT_GROWTH = "GROWTH";
    public static final String FAULT_THRUST = "THRUST";
}
