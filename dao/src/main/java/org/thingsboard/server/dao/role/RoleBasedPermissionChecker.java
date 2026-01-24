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
package org.thingsboard.server.dao.role;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;

import java.util.Set;

@Component
@Slf4j
public class RoleBasedPermissionChecker {

    @Autowired
    private RoleService roleService;

    public boolean hasPermission(User user, Resource resource, Operation operation) {
        if (user == null) {
            log.debug("User is null, denying permission");
            return false;
        }

        if (user.getAuthority() == Authority.SYS_ADMIN) {
            log.trace("User is SYS_ADMIN, granting all permissions");
            return true;
        }

        RoleId roleId = user.getRoleId();
        if (roleId == null) {
            log.trace("User has no role, falling back to legacy authority check");
            return checkLegacyAuthority(user, resource, operation);
        }

        Set<RolePermission> permissions = getRolePermissionsCached(roleId);
        
        if (permissions == null || permissions.isEmpty()) {
            log.debug("No permissions found for role [{}]", roleId);
            return false;
        }

        boolean hasPermission = checkPermissions(permissions, resource, operation);
        
        log.trace("Permission check for user [{}], resource [{}], operation [{}]: {}", 
                user.getId(), resource, operation, hasPermission);
        
        return hasPermission;
    }

    @Cacheable(cacheNames = "rolePermissions", key = "#roleId")
    protected Set<RolePermission> getRolePermissionsCached(RoleId roleId) {
        return roleService.getRolePermissions(roleId);
    }

    private boolean checkPermissions(Set<RolePermission> permissions, Resource resource, Operation operation) {
        for (RolePermission permission : permissions) {
            if (matchesResource(permission.getResource(), resource) && 
                matchesOperation(permission.getOperation(), operation)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesResource(Resource permissionResource, Resource requestedResource) {
        if (permissionResource == Resource.ALL) {
            return true;
        }
        return permissionResource == requestedResource;
    }

    private boolean matchesOperation(Operation permissionOperation, Operation requestedOperation) {
        if (permissionOperation == Operation.ALL) {
            return true;
        }
        return permissionOperation == requestedOperation;
    }

    private boolean checkLegacyAuthority(User user, Resource resource, Operation operation) {
        Authority authority = user.getAuthority();
        
        if (authority == null) {
            return false;
        }

        switch (authority) {
            case SYS_ADMIN:
                return true;
            case TENANT_ADMIN:
                return checkTenantAdminPermissions(resource, operation);
            case CUSTOMER_USER:
                return checkCustomerUserPermissions(resource, operation);
            default:
                return false;
        }
    }

    private boolean checkTenantAdminPermissions(Resource resource, Operation operation) {
        switch (resource) {
            case TENANT:
            case TENANT_PROFILE:
                return operation == Operation.READ;
            case USER:
            case CUSTOMER:
            case DEVICE:
            case ASSET:
            case DASHBOARD:
            case RULE_CHAIN:
            case ENTITY_VIEW:
            case ALARM:
            case WIDGET_TYPE:
            case WIDGETS_BUNDLE:
            case TB_RESOURCE:
            case OTA_PACKAGE:
            case EDGE:
            case RPC:
            case QUEUE:
                return true;
            default:
                return false;
        }
    }

    private boolean checkCustomerUserPermissions(Resource resource, Operation operation) {
        switch (resource) {
            case DEVICE:
            case ASSET:
            case DASHBOARD:
            case ENTITY_VIEW:
            case ALARM:
                return operation == Operation.READ || operation == Operation.RPC_CALL;
            default:
                return false;
        }
    }

    public boolean hasAnyPermission(User user, Resource resource) {
        if (user == null) {
            return false;
        }

        if (user.getAuthority() == Authority.SYS_ADMIN) {
            return true;
        }

        RoleId roleId = user.getRoleId();
        if (roleId == null) {
            return checkLegacyAuthority(user, resource, Operation.READ);
        }

        Set<RolePermission> permissions = getRolePermissionsCached(roleId);
        
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        return permissions.stream()
                .anyMatch(p -> matchesResource(p.getResource(), resource));
    }

    public boolean hasAllPermissions(User user, Resource resource) {
        if (user == null) {
            return false;
        }

        if (user.getAuthority() == Authority.SYS_ADMIN) {
            return true;
        }

        RoleId roleId = user.getRoleId();
        if (roleId == null) {
            return false;
        }

        Set<RolePermission> permissions = getRolePermissionsCached(roleId);
        
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        return permissions.stream()
                .anyMatch(p -> matchesResource(p.getResource(), resource) && 
                              p.getOperation() == Operation.ALL);
    }
}
