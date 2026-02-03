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
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 255, message = "El nombre no debe exceder 255 caracteres")
    private String name;

    @Size(max = 255, message = "La descripción no debe exceder 255 caracteres")
    private String description;

    // Association
    private UUID fieldId;
    private UUID reservoirId;
    private UUID seismicSurveyId;   // Source seismic survey

    // Fault Classification
    @Size(max = 50, message = "El tipo de falla no debe exceder 50 caracteres")
    private String faultType;       // NORMAL, REVERSE, STRIKE_SLIP, LISTRIC, GROWTH

    @Size(max = 100, message = "El sistema de fallas no debe exceder 100 caracteres")
    private String faultSystem;     // Name of fault system if part of larger system

    @PositiveOrZero(message = "El orden de la falla debe ser >= 0")
    private Integer faultOrder;     // Order within fault system (1=main, 2=secondary)

    // Geometry
    @DecimalMin(value = "0.0", message = "El azimut de rumbo debe ser >= 0")
    @DecimalMax(value = "360.0", message = "El azimut de rumbo debe ser <= 360")
    private BigDecimal strikeAzimuth;   // Strike direction (degrees from N)

    @DecimalMin(value = "0.0", message = "El ángulo de buzamiento debe ser >= 0")
    @DecimalMax(value = "90.0", message = "El ángulo de buzamiento debe ser <= 90")
    private BigDecimal dipAngle;        // Dip angle (degrees)

    @Size(max = 10, message = "La dirección de buzamiento no debe exceder 10 caracteres")
    private String dipDirection;        // N, NE, E, SE, S, SW, W, NW

    @PositiveOrZero(message = "La longitud de la falla debe ser >= 0")
    private BigDecimal lengthKm;        // Fault trace length (km)

    @PositiveOrZero(message = "El salto vertical debe ser >= 0")
    private BigDecimal throwM;          // Vertical throw (m)

    @PositiveOrZero(message = "El desplazamiento horizontal debe ser >= 0")
    private BigDecimal heaveM;          // Horizontal displacement (m)

    // Depth Range
    @PositiveOrZero(message = "La profundidad superior debe ser >= 0")
    private BigDecimal topDepthM;       // Shallowest point (m TVD)

    @PositiveOrZero(message = "La profundidad de base debe ser >= 0")
    private BigDecimal baseDepthM;      // Deepest point (m TVD)

    // Sealing Properties
    @Size(max = 50, message = "El potencial de sello no debe exceder 50 caracteres")
    private String sealingPotential;    // SEALING, PARTIALLY_SEALING, NON_SEALING

    @DecimalMin(value = "0.0", message = "El factor SGR debe ser >= 0")
    @DecimalMax(value = "1.0", message = "El factor SGR debe ser <= 1")
    private BigDecimal shaleSmeaerFactor;   // SGR - Shale Gouge Ratio

    @PositiveOrZero(message = "El potencial CSP debe ser >= 0")
    private BigDecimal claySmearPotential;  // CSP

    @PositiveOrZero(message = "La permeabilidad de la roca de falla debe ser >= 0")
    private BigDecimal faultRockPermeability; // mD

    // Activity
    private Boolean activelyGrowing;    // For growth faults

    @Size(max = 100, message = "La edad de última actividad no debe exceder 100 caracteres")
    private String lastActivityAge;     // Geological age of last movement

    // Interpretation Confidence
    @Size(max = 50, message = "La confianza de interpretación no debe exceder 50 caracteres")
    private String interpretationConfidence; // HIGH, MEDIUM, LOW

    @Size(max = 50, message = "La fuente de datos no debe exceder 50 caracteres")
    private String dataSource;          // SEISMIC, WELL_CORRELATION, BOTH

    // Juxtaposition Analysis
    private Boolean juxtapositionAnalysisDone;

    @Size(max = 255, message = "Las formaciones yuxtapuestas no deben exceder 255 caracteres")
    private String juxtaposedFormations;    // Formations across fault

    // Compartmentalization
    private Boolean compartmentalizing;     // Does it compartmentalize reservoir?

    @Size(max = 255, message = "Los compartimientos no deben exceder 255 caracteres")
    private String compartments;            // Names of compartments created

    // Well Penetrations
    @PositiveOrZero(message = "El número de pozos que penetran la falla debe ser >= 0")
    private Integer wellsPenetratingFault;

    @Size(max = 500, message = "Los pozos penetrantes no deben exceder 500 caracteres")
    private String penetratingWells;        // Comma-separated well names

    // Venezuela-specific
    private Boolean fajaRelatedStructure;

    @Size(max = 50, message = "La fase tectónica no debe exceder 50 caracteres")
    private String tectonicPhase;           // EXTENSIONAL, COMPRESSIONAL, WRENCH

    // Coordinates (fault polygon/polyline as GeoJSON)
    private JsonNode faultGeometry;         // GeoJSON LineString or Polygon

    @Size(max = 100, message = "El sistema de coordenadas no debe exceder 100 caracteres")
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
