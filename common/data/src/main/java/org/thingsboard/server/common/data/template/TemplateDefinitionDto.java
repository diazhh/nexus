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
package org.thingsboard.server.common.data.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDefinitionDto {
    private UUID id;
    private String templateCode;
    private String templateName;
    private String description;

    private String moduleCode;
    private String entityType;
    private String category;

    private String version;
    private Boolean isActive;

    private TemplateStructure templateStructure;
    private List<TemplateVariable> requiredVariables;

    @JsonIgnore
    private String templateStructureJson;
    @JsonIgnore
    private String requiredVariablesJson;

    private UUID createdBy;
    private Long createdTime;
    private UUID updatedBy;
    private Long updatedTime;

    private UUID tenantId;
}
