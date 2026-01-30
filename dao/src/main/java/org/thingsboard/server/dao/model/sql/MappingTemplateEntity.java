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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.MappingTemplateId;
import org.thingsboard.server.common.data.nexus.DistributionMode;
import org.thingsboard.server.common.data.nexus.MappingTemplate;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

/**
 * JPA Entity for nx_mapping_template table.
 * Reusable mapping templates for configuring data distribution.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "nx_mapping_template")
public final class MappingTemplateEntity extends BaseSqlEntity<MappingTemplate> {

    @Column(name = "tenant_id", columnDefinition = "uuid", nullable = false)
    private UUID tenantId;

    @Column(name = "module_key", nullable = false)
    private String moduleKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "target_asset_type")
    private String targetAssetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_mode")
    private DistributionMode distributionMode;

    @Column(name = "is_default")
    private boolean isDefault;

    @Column(name = "is_active")
    private boolean isActive;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    public MappingTemplateEntity() {
        super();
    }

    public MappingTemplateEntity(MappingTemplate template) {
        super(template);
        this.tenantId = getTenantUuid(template.getTenantId());
        this.moduleKey = template.getModuleKey();
        this.name = template.getName();
        this.description = template.getDescription();
        this.targetAssetType = template.getTargetAssetType();
        this.distributionMode = template.getDistributionMode();
        this.isDefault = template.isDefault();
        this.isActive = template.isActive();
        this.additionalInfo = template.getAdditionalInfo();
    }

    @Override
    public MappingTemplate toData() {
        MappingTemplate template = new MappingTemplate(new MappingTemplateId(id));
        template.setCreatedTime(createdTime);
        template.setTenantId(getTenantId(tenantId));
        template.setModuleKey(moduleKey);
        template.setName(name);
        template.setDescription(description);
        template.setTargetAssetType(targetAssetType);
        template.setDistributionMode(distributionMode);
        template.setDefault(isDefault);
        template.setActive(isActive);
        template.setAdditionalInfo(additionalInfo);
        return template;
    }
}
