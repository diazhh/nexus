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
package org.thingsboard.server.common.data.nexus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.NexusModuleId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Represents a NEXUS module that can be assigned to tenants.
 * Modules include CT (Corte de Tubing), RV (Reparación de Válvulas), DR (Drilling Rigs), etc.
 */
@Schema
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class NexusModule extends BaseDataWithAdditionalInfo<NexusModuleId> implements HasName {

    private static final long serialVersionUID = 1L;

    @NoXss
    @Length(fieldName = "moduleKey", min = 1, max = 100)
    private String moduleKey;

    @NoXss
    @Length(fieldName = "name", min = 1, max = 255)
    private String name;

    @NoXss
    @Length(fieldName = "description", max = 1024)
    private String description;

    @NoXss
    @Length(fieldName = "version", max = 20)
    private String version;

    @NoXss
    @Length(fieldName = "category", max = 50)
    private String category;

    @NoXss
    @Length(fieldName = "icon", max = 100)
    private String icon;

    @NoXss
    @Length(fieldName = "routePath", max = 255)
    private String routePath;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean isSystemModule;

    private boolean isAvailable;

    private int displayOrder;

    public NexusModule() {
        super();
        this.version = "1.0.0";
        this.isAvailable = true;
        this.displayOrder = 0;
    }

    public NexusModule(NexusModuleId id) {
        super(id);
    }

    public NexusModule(NexusModule module) {
        super(module);
        this.moduleKey = module.getModuleKey();
        this.name = module.getName();
        this.description = module.getDescription();
        this.version = module.getVersion();
        this.category = module.getCategory();
        this.icon = module.getIcon();
        this.routePath = module.getRoutePath();
        this.isSystemModule = module.isSystemModule();
        this.isAvailable = module.isAvailable();
        this.displayOrder = module.getDisplayOrder();
    }

    @Schema(description = "JSON object with the Module Id. " +
            "Specify this field to update the module. " +
            "Referencing non-existing Module Id will cause error. " +
            "Omit this field to create new module.")
    @Override
    public NexusModuleId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the module creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique module key identifier", example = "CT")
    public String getModuleKey() {
        return moduleKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Module display name", example = "Corte de Tubing")
    @Override
    public String getName() {
        return name;
    }

    @Schema(description = "Module description", example = "Module for managing tubing cutting operations")
    public String getDescription() {
        return description;
    }

    @Schema(description = "Module version", example = "1.0.0")
    public String getVersion() {
        return version;
    }

    @Schema(description = "Module category", example = "OPERATIONS")
    public String getCategory() {
        return category;
    }

    @Schema(description = "Module icon name", example = "content_cut")
    public String getIcon() {
        return icon;
    }

    @Schema(description = "Module route path in UI", example = "/ct")
    public String getRoutePath() {
        return routePath;
    }

    @Schema(description = "System flag indicates if module is core and cannot be disabled", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    public boolean isSystemModule() {
        return isSystemModule;
    }

    @Schema(description = "Availability flag indicates if module can be assigned to tenants", example = "true")
    public boolean isAvailable() {
        return isAvailable;
    }

    @Schema(description = "Display order in UI menus", example = "1")
    public int getDisplayOrder() {
        return displayOrder;
    }

    @Schema(description = "Additional parameters of the module", implementation = JsonNode.class)
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NexusModule [id=");
        builder.append(id);
        builder.append(", moduleKey=");
        builder.append(moduleKey);
        builder.append(", name=");
        builder.append(name);
        builder.append(", description=");
        builder.append(description);
        builder.append(", version=");
        builder.append(version);
        builder.append(", category=");
        builder.append(category);
        builder.append(", isSystemModule=");
        builder.append(isSystemModule);
        builder.append(", isAvailable=");
        builder.append(isAvailable);
        builder.append(", displayOrder=");
        builder.append(displayOrder);
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append("]");
        return builder.toString();
    }
}
