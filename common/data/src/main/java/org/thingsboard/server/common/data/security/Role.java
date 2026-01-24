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
package org.thingsboard.server.common.data.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseDataWithAdditionalInfo<RoleId> implements HasTenantId, HasName, HasVersion {
    
    private static final long serialVersionUID = 1L;
    
    private TenantId tenantId;
    
    @NoXss
    @Length(fieldName = "name", min = 1, max = 255)
    private String name;
    
    @NoXss
    @Length(fieldName = "description", max = 1024)
    private String description;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean isSystem;
    
    private Long version;
    
    public Role() {
        super();
    }
    
    public Role(RoleId id) {
        super(id);
    }
    
    public Role(Role role) {
        super(role);
        this.tenantId = role.getTenantId();
        this.name = role.getName();
        this.description = role.getDescription();
        this.isSystem = role.isSystem();
        this.version = role.getVersion();
    }
    
    @Schema(description = "JSON object with the Role Id. " +
            "Specify this field to update the role. " +
            "Referencing non-existing Role Id will cause error. " +
            "Omit this field to create new role.")
    @Override
    public RoleId getId() {
        return super.getId();
    }
    
    @Schema(description = "Timestamp of the role creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }
    
    @Schema(description = "JSON object with the Tenant Id. System roles have null tenant id.", accessMode = Schema.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }
    
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique role name", example = "Device Manager")
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Schema(description = "Description of the role", example = "Manages devices and views dashboards")
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Schema(description = "System flag indicates if role is predefined and cannot be deleted", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    public boolean isSystem() {
        return isSystem;
    }
    
    public void setSystem(boolean system) {
        isSystem = system;
    }
    
    @Schema(description = "Version of the role for optimistic locking", example = "1")
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    @Schema(description = "Additional parameters of the role", implementation = com.fasterxml.jackson.databind.JsonNode.class)
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }
    
    public boolean isSystemRole() {
        return isSystem || tenantId == null;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Role [id=");
        builder.append(id);
        builder.append(", tenantId=");
        builder.append(tenantId);
        builder.append(", name=");
        builder.append(name);
        builder.append(", description=");
        builder.append(description);
        builder.append(", isSystem=");
        builder.append(isSystem);
        builder.append(", version=");
        builder.append(version);
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append("]");
        return builder.toString();
    }
}
