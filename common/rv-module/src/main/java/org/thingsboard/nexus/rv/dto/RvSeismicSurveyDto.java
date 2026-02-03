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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Seismic Survey assets in the Reservoir Module.
 * Represents 2D/3D seismic acquisition and interpretation data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RvSeismicSurveyDto {

    private UUID id;

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 255, message = "El nombre no debe exceder 255 caracteres")
    private String name;

    @Size(max = 255, message = "La descripción no debe exceder 255 caracteres")
    private String description;

    // Association
    @NotNull(message = "El ID del campo es requerido")
    private UUID fieldId;       // Field this survey covers

    private UUID reservoirId;   // Optional: specific reservoir

    // Survey Information
    @Size(max = 50, message = "El tipo de levantamiento no debe exceder 50 caracteres")
    private String surveyType;      // 2D, 3D, 4D, VSP

    @Size(max = 50, message = "La fecha de adquisición no debe exceder 50 caracteres")
    private String acquisitionDate;

    @Size(max = 50, message = "La fecha de procesamiento no debe exceder 50 caracteres")
    private String processingDate;

    @Size(max = 100, message = "El contratista no debe exceder 100 caracteres")
    private String contractor;      // Acquisition company

    @Size(max = 100, message = "La compañía de procesamiento no debe exceder 100 caracteres")
    private String processingCompany;

    // Coverage
    @PositiveOrZero(message = "El área debe ser >= 0")
    private BigDecimal areaKm2;         // Survey area in km²

    @PositiveOrZero(message = "La longitud total de línea debe ser >= 0")
    private BigDecimal totalLineKm;     // Total line km (for 2D)

    @PositiveOrZero(message = "El conteo total de CDP debe ser >= 0")
    private Integer totalCdpCount;      // Number of CDP points

    @PositiveOrZero(message = "El tamaño de bin inline debe ser >= 0")
    private BigDecimal binSizeInline;   // Inline bin size (m)

    @PositiveOrZero(message = "El tamaño de bin crossline debe ser >= 0")
    private BigDecimal binSizeCrossline;// Crossline bin size (m)

    // Quality Metrics
    @PositiveOrZero(message = "La cobertura fold debe ser >= 0")
    private Integer foldCoverage;       // Fold coverage

    @PositiveOrZero(message = "La relación señal/ruido debe ser >= 0")
    private BigDecimal signalToNoiseRatio;

    @Size(max = 50, message = "La calificación de calidad no debe exceder 50 caracteres")
    private String qualityRating;       // EXCELLENT, GOOD, FAIR, POOR

    // Processing Information
    @Size(max = 255, message = "La secuencia de procesamiento no debe exceder 255 caracteres")
    private String processingSequence;  // Migration type, filters, etc.

    @Size(max = 100, message = "El modelo de velocidad usado no debe exceder 100 caracteres")
    private String velocityModelUsed;

    private Boolean preStackDepthMigration;

    // Interpretation
    private Boolean interpreted;

    @Size(max = 100, message = "El nombre del intérprete no debe exceder 100 caracteres")
    private String interpreterName;

    @Size(max = 50, message = "La fecha de interpretación no debe exceder 50 caracteres")
    private String interpretationDate;

    @PositiveOrZero(message = "Los horizontes interpretados deben ser >= 0")
    private Integer horizonsInterpreted;

    @PositiveOrZero(message = "Las fallas interpretadas deben ser >= 0")
    private Integer faultsInterpreted;

    // Venezuela-specific
    @Size(max = 50, message = "La región de la Faja no debe exceder 50 caracteres")
    private String fajaRegion;          // If applicable: BOYACA, JUNIN, etc.

    private Boolean heavyOilImaging;    // Special processing for heavy oil

    // Coordinate system
    @Size(max = 100, message = "El sistema de coordenadas no debe exceder 100 caracteres")
    private String coordinateSystem;    // UTM zone, datum

    @Size(max = 50, message = "El datum usado no debe exceder 50 caracteres")
    private String datumUsed;

    // File references
    private String segyFilePath;
    private String navigationFilePath;
    private String velocityFilePath;

    // Extended metadata (JSON)
    private JsonNode metadata;

    // Timestamps
    private Long createdTime;
    private Long updatedTime;

    // Survey Type Constants
    public static final String SURVEY_2D = "2D";
    public static final String SURVEY_3D = "3D";
    public static final String SURVEY_4D = "4D";
    public static final String SURVEY_VSP = "VSP";
    public static final String SURVEY_WALKAWAY = "WALKAWAY_VSP";
}
