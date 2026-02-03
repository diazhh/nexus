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
import java.util.UUID;

/**
 * DTO for catalog items in the Reservoir Module.
 * Catalogs store enumeration values like well types, lithologies, formations, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RvCatalogDto {

    private UUID id;

    @NotNull(message = "El ID del tenant es requerido")
    private UUID tenantId;

    /**
     * Type of catalog (e.g., WELL_TYPE, LITHOLOGY, FORMATION_VE, FAJA_REGION)
     */
    @NotBlank(message = "El tipo de catálogo es requerido")
    @Size(max = 100, message = "El tipo de catálogo no debe exceder 100 caracteres")
    private String catalogType;

    /**
     * Unique code within the catalog type (e.g., PRODUCER, SANDSTONE)
     */
    @NotBlank(message = "El código es requerido")
    @Size(max = 50, message = "El código no debe exceder 50 caracteres")
    private String code;

    /**
     * Display name
     */
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    private String name;

    /**
     * Optional description
     */
    @Size(max = 255, message = "La descripción no debe exceder 255 caracteres")
    private String description;

    /**
     * Additional metadata as JSON (e.g., icons, ranges, formulas)
     */
    private JsonNode metadata;

    /**
     * Sort order for display
     */
    @PositiveOrZero(message = "El orden de clasificación debe ser >= 0")
    private Integer sortOrder;

    /**
     * Whether this catalog item is active
     */
    private Boolean isActive;

    private Long createdTime;
    private Long updatedTime;

    // ========================================
    // Catalog Types Constants
    // ========================================

    public static final String TYPE_WELL_TYPE = "WELL_TYPE";
    public static final String TYPE_LITHOLOGY = "LITHOLOGY";
    public static final String TYPE_DRIVE_MECHANISM = "DRIVE_MECHANISM";
    public static final String TYPE_FORMATION_VE = "FORMATION_VE";
    public static final String TYPE_FAJA_REGION = "FAJA_REGION";
    public static final String TYPE_COMPLETION_TYPE = "COMPLETION_TYPE";
    public static final String TYPE_ARTIFICIAL_LIFT = "ARTIFICIAL_LIFT";
    public static final String TYPE_IPR_METHOD = "IPR_METHOD";
    public static final String TYPE_DECLINE_TYPE = "DECLINE_TYPE";
}
