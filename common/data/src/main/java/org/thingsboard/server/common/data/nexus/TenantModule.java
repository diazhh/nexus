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
import org.thingsboard.server.common.data.id.NexusModuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantModuleId;
import org.thingsboard.server.common.data.id.UserId;

/**
 * Represents the assignment of a NexusModule to a specific Tenant.
 * Controls which modules each tenant can access.
 */
@Schema
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TenantModule extends BaseDataWithAdditionalInfo<TenantModuleId> implements HasTenantId {

    private static final long serialVersionUID = 1L;

    private TenantId tenantId;

    private NexusModuleId moduleId;

    private boolean isActive;

    private long activationDate;

    private Long deactivationDate;

    private UserId activatedBy;

    private JsonNode configuration;

    public TenantModule() {
        super();
        this.isActive = true;
        this.activationDate = System.currentTimeMillis();
    }

    public TenantModule(TenantModuleId id) {
        super(id);
    }

    public TenantModule(TenantModule tenantModule) {
        super(tenantModule);
        this.tenantId = tenantModule.getTenantId();
        this.moduleId = tenantModule.getModuleId();
        this.isActive = tenantModule.isActive();
        this.activationDate = tenantModule.getActivationDate();
        this.deactivationDate = tenantModule.getDeactivationDate();
        this.activatedBy = tenantModule.getActivatedBy();
        this.configuration = tenantModule.getConfiguration();
    }

    @Schema(description = "JSON object with the Tenant Module Id. " +
            "Specify this field to update the assignment. " +
            "Referencing non-existing Id will cause error. " +
            "Omit this field to create new assignment.")
    @Override
    public TenantModuleId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the assignment creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with the Tenant Id")
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with the Module Id")
    public NexusModuleId getModuleId() {
        return moduleId;
    }

    @Schema(description = "Active flag indicates if the module is currently accessible by the tenant", example = "true")
    public boolean isActive() {
        return isActive;
    }

    @Schema(description = "Timestamp when the module was activated for this tenant, in milliseconds", example = "1609459200000")
    public long getActivationDate() {
        return activationDate;
    }

    @Schema(description = "Timestamp when the module was deactivated for this tenant, in milliseconds", example = "1609459200000")
    public Long getDeactivationDate() {
        return deactivationDate;
    }

    @Schema(description = "User Id who activated/assigned the module to this tenant")
    public UserId getActivatedBy() {
        return activatedBy;
    }

    @Schema(description = "Tenant-specific configuration for the module", implementation = JsonNode.class)
    public JsonNode getConfiguration() {
        return configuration;
    }

    @Schema(description = "Additional parameters of the tenant module assignment", implementation = JsonNode.class)
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    /**
     * Deactivates the module for this tenant
     */
    public void deactivate() {
        this.isActive = false;
        this.deactivationDate = System.currentTimeMillis();
    }

    /**
     * Reactivates the module for this tenant
     */
    public void reactivate() {
        this.isActive = true;
        this.deactivationDate = null;
        this.activationDate = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TenantModule [id=");
        builder.append(id);
        builder.append(", tenantId=");
        builder.append(tenantId);
        builder.append(", moduleId=");
        builder.append(moduleId);
        builder.append(", isActive=");
        builder.append(isActive);
        builder.append(", activationDate=");
        builder.append(activationDate);
        builder.append(", deactivationDate=");
        builder.append(deactivationDate);
        builder.append(", activatedBy=");
        builder.append(activatedBy);
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append("]");
        return builder.toString();
    }
}
