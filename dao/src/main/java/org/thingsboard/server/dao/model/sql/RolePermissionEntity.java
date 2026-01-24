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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RolePermissionId;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;
import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

@Data
@Entity
@Table(name = "role_permission")
public final class RolePermissionEntity implements ToData<RolePermission> {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "role_id")
    private UUID roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation")
    private Operation operation;

    public RolePermissionEntity() {
        super();
    }

    public RolePermissionEntity(RolePermission permission) {
        if (permission.getId() != null) {
            this.id = permission.getId().getId();
        } else {
            this.id = UUID.randomUUID();
        }
        this.roleId = permission.getRoleId().getId();
        this.resource = permission.getResource();
        this.operation = permission.getOperation();
    }

    @Override
    public RolePermission toData() {
        RolePermission permission = new RolePermission();
        permission.setId(new RolePermissionId(id));
        permission.setRoleId(new RoleId(roleId));
        permission.setResource(resource);
        permission.setOperation(operation);
        return permission;
    }
}
