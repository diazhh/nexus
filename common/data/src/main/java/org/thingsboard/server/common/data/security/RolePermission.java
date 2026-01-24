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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RolePermissionId;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;

import java.io.Serializable;

@Schema
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "ID of the role permission")
    private RolePermissionId id;
    
    @Schema(description = "ID of the role this permission belongs to", requiredMode = Schema.RequiredMode.REQUIRED)
    private RoleId roleId;
    
    @Schema(description = "Resource type", example = "DEVICE", requiredMode = Schema.RequiredMode.REQUIRED)
    private Resource resource;
    
    @Schema(description = "Operation type", example = "READ", requiredMode = Schema.RequiredMode.REQUIRED)
    private Operation operation;
    
    public RolePermission(RoleId roleId, Resource resource, Operation operation) {
        this.roleId = roleId;
        this.resource = resource;
        this.operation = operation;
    }
    
    public boolean allowsOperation(Operation op) {
        return this.operation == Operation.ALL || this.operation == op;
    }
    
    public boolean allowsResource(Resource res) {
        return this.resource == Resource.ALL || this.resource == res;
    }
    
    @Override
    public String toString() {
        return "RolePermission{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", resource=" + resource +
                ", operation=" + operation +
                '}';
    }
}
