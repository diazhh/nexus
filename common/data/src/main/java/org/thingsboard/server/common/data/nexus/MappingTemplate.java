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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.MappingTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Reusable mapping template for configuring data distribution from devices to Digital Twin assets.
 * Templates are organized by module (CT, DR, RV) and can be applied to multiple Device→Asset pairs.
 */
@Schema
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MappingTemplate extends BaseDataWithAdditionalInfo<MappingTemplateId> implements HasTenantId {

    private static final long serialVersionUID = 1L;

    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "moduleKey", min = 1, max = 50)
    private String moduleKey;

    @NoXss
    @Length(fieldName = "name", min = 1, max = 255)
    private String name;

    @NoXss
    @Length(fieldName = "description", max = 1000)
    private String description;

    @NoXss
    @Length(fieldName = "targetAssetType", max = 100)
    private String targetAssetType;

    private DistributionMode distributionMode;

    private boolean isDefault;

    private boolean active;

    public MappingTemplate() {
        super();
        this.distributionMode = DistributionMode.MAPPED;
        this.isDefault = false;
        this.active = true;
    }

    public MappingTemplate(MappingTemplateId id) {
        super(id);
    }

    public MappingTemplate(MappingTemplate template) {
        super(template);
        this.tenantId = template.getTenantId();
        this.moduleKey = template.getModuleKey();
        this.name = template.getName();
        this.description = template.getDescription();
        this.targetAssetType = template.getTargetAssetType();
        this.distributionMode = template.getDistributionMode();
        this.isDefault = template.isDefault();
        this.active = template.isActive();
    }

    @Schema(description = "JSON object with the Mapping Template Id. " +
            "Specify this field to update the template. " +
            "Referencing non-existing Id will cause error. " +
            "Omit this field to create new template.")
    @Override
    public MappingTemplateId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Module key identifier (CT, DR, RV)", example = "CT")
    public String getModuleKey() {
        return moduleKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Template name", example = "CT Standard Mapping")
    public String getName() {
        return name;
    }

    @Schema(description = "Template description", example = "Standard mapping for CT units from SCADA systems")
    public String getDescription() {
        return description;
    }

    @Schema(description = "Target asset type", example = "CT_UNIT")
    public String getTargetAssetType() {
        return targetAssetType;
    }

    @Schema(description = "Distribution mode", example = "MAPPED")
    public DistributionMode getDistributionMode() {
        return distributionMode;
    }

    @Schema(description = "Whether this is the default template for the module", example = "true")
    public boolean isDefault() {
        return isDefault;
    }

    @Schema(description = "Active status flag", example = "true")
    public boolean isActive() {
        return active;
    }

    @Schema(description = "Additional parameters", implementation = JsonNode.class)
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    @Override
    public String toString() {
        return "MappingTemplate [id=" + id +
                ", tenantId=" + tenantId +
                ", moduleKey=" + moduleKey +
                ", name=" + name +
                ", targetAssetType=" + targetAssetType +
                ", distributionMode=" + distributionMode +
                ", isDefault=" + isDefault +
                ", active=" + active +
                ", createdTime=" + createdTime + "]";
    }
}
