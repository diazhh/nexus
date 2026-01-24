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
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "role")
public final class RoleEntity extends BaseVersionedEntity<Role> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_system")
    private boolean isSystem;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    public RoleEntity() {
        super();
    }

    public RoleEntity(Role role) {
        super(role);
        if (role.getTenantId() != null) {
            this.tenantId = role.getTenantId().getId();
        }
        this.name = role.getName();
        this.description = role.getDescription();
        this.isSystem = role.isSystem();
        this.additionalInfo = role.getAdditionalInfo();
    }

    @Override
    public Role toData() {
        Role role = new Role(new RoleId(id));
        role.setCreatedTime(createdTime);
        if (tenantId != null) {
            role.setTenantId(new TenantId(tenantId));
        }
        role.setName(name);
        role.setDescription(description);
        role.setSystem(isSystem);
        role.setAdditionalInfo(additionalInfo);
        role.setVersion(version);
        return role;
    }
}
