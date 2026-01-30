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
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.NexusModuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantModuleId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.nexus.TenantModule;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

/**
 * JPA Entity for nx_tenant_module table.
 * Represents the assignment of a module to a specific tenant.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "nx_tenant_module")
public final class TenantModuleEntity extends BaseSqlEntity<TenantModule> {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "activation_date", nullable = false)
    private long activationDate;

    @Column(name = "deactivation_date")
    private Long deactivationDate;

    @Column(name = "activated_by")
    private UUID activatedBy;

    @Convert(converter = JsonConverter.class)
    @Column(name = "configuration")
    private JsonNode configuration;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    public TenantModuleEntity() {
        super();
    }

    public TenantModuleEntity(TenantModule tenantModule) {
        super(tenantModule);
        if (tenantModule.getTenantId() != null) {
            this.tenantId = tenantModule.getTenantId().getId();
        }
        if (tenantModule.getModuleId() != null) {
            this.moduleId = tenantModule.getModuleId().getId();
        }
        this.isActive = tenantModule.isActive();
        this.activationDate = tenantModule.getActivationDate();
        this.deactivationDate = tenantModule.getDeactivationDate();
        if (tenantModule.getActivatedBy() != null) {
            this.activatedBy = tenantModule.getActivatedBy().getId();
        }
        this.configuration = tenantModule.getConfiguration();
        this.additionalInfo = tenantModule.getAdditionalInfo();
    }

    @Override
    public TenantModule toData() {
        TenantModule tenantModule = new TenantModule(new TenantModuleId(id));
        tenantModule.setCreatedTime(createdTime);
        if (tenantId != null) {
            tenantModule.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (moduleId != null) {
            tenantModule.setModuleId(new NexusModuleId(moduleId));
        }
        tenantModule.setActive(isActive);
        tenantModule.setActivationDate(activationDate);
        tenantModule.setDeactivationDate(deactivationDate);
        if (activatedBy != null) {
            tenantModule.setActivatedBy(new UserId(activatedBy));
        }
        tenantModule.setConfiguration(configuration);
        tenantModule.setAdditionalInfo(additionalInfo);
        return tenantModule;
    }
}
