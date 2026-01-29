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
 * DTO for Seismic Survey assets in the Reservoir Module.
 * Represents 2D/3D seismic acquisition and interpretation data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RvSeismicSurveyDto {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;

    // Association
    private UUID fieldId;       // Field this survey covers
    private UUID reservoirId;   // Optional: specific reservoir

    // Survey Information
    private String surveyType;      // 2D, 3D, 4D, VSP
    private String acquisitionDate;
    private String processingDate;
    private String contractor;      // Acquisition company
    private String processingCompany;

    // Coverage
    private BigDecimal areaKm2;         // Survey area in km²
    private BigDecimal totalLineKm;     // Total line km (for 2D)
    private Integer totalCdpCount;      // Number of CDP points
    private BigDecimal binSizeInline;   // Inline bin size (m)
    private BigDecimal binSizeCrossline;// Crossline bin size (m)

    // Quality Metrics
    private Integer foldCoverage;       // Fold coverage
    private BigDecimal signalToNoiseRatio;
    private String qualityRating;       // EXCELLENT, GOOD, FAIR, POOR

    // Processing Information
    private String processingSequence;  // Migration type, filters, etc.
    private String velocityModelUsed;
    private Boolean preStackDepthMigration;

    // Interpretation
    private Boolean interpreted;
    private String interpreterName;
    private String interpretationDate;
    private Integer horizonsInterpreted;
    private Integer faultsInterpreted;

    // Venezuela-specific
    private String fajaRegion;          // If applicable: BOYACA, JUNIN, etc.
    private Boolean heavyOilImaging;    // Special processing for heavy oil

    // Coordinate system
    private String coordinateSystem;    // UTM zone, datum
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
