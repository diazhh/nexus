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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.*;

@Tag(name = "Role Management", description = "APIs for managing roles and permissions")
@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/role")
public class RoleController extends BaseController {

    private final RoleService roleService;

    @Operation(summary = "Get role by ID", description = "Returns a single role by its ID. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role found"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/{roleId}")
    public Role getRoleById(@Parameter(description = "Role ID", required = true) @PathVariable String roleId) throws ThingsboardException {
        checkParameter("roleId", roleId);
        RoleId id = new RoleId(toUUID(roleId));
        SecurityUser currentUser = getCurrentUser();
        return checkNotNull(roleService.findRoleById(currentUser.getTenantId(), id));
    }

    @Operation(summary = "Get roles", description = "Returns a page of roles for the current tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping
    public PageData<Role> getRoles(
        @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
        @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
        @Parameter(description = "Text search term") @RequestParam(required = false) String textSearch,
        @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
        @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(roleService.findRolesByTenantId(currentUser.getTenantId(), pageLink));
    }

    @Operation(summary = "Create or update role", description = "Creates a new role or updates an existing one. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping
    public Role saveRole(@Parameter(description = "A JSON value representing the Role", required = true) @RequestBody Role role) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        role.setTenantId(currentUser.getTenantId());
        return checkNotNull(roleService.saveRole(role));
    }

    @Operation(summary = "Delete role", description = "Deletes a role by its ID. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "400", description = "Role is in use")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteRole(@Parameter(description = "Role ID", required = true) @PathVariable String roleId) throws ThingsboardException {
        checkParameter("roleId", roleId);
        RoleId id = new RoleId(toUUID(roleId));
        SecurityUser currentUser = getCurrentUser();
        
        long userCount = roleService.countUsersByRoleId(id);
        if (userCount > 0) {
            throw new ThingsboardException("Cannot delete role that is assigned to " + userCount + " user(s)", 
                org.thingsboard.server.common.data.exception.ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        
        roleService.deleteRole(currentUser.getTenantId(), id);
    }

    @Operation(summary = "Get role permissions", description = "Returns all permissions assigned to a role. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/{roleId}/permissions")
    public Set<RolePermission> getPermissions(@Parameter(description = "Role ID", required = true) @PathVariable String roleId) throws ThingsboardException {
        checkParameter("roleId", roleId);
        RoleId id = new RoleId(toUUID(roleId));
        SecurityUser currentUser = getCurrentUser();
        checkNotNull(roleService.findRoleById(currentUser.getTenantId(), id));
        return roleService.getRolePermissions(id);
    }

    @Operation(summary = "Update role permissions", description = "Replaces all permissions for a role. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permissions updated successfully"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "400", description = "Invalid permissions")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PutMapping("/{roleId}/permissions")
    @ResponseStatus(HttpStatus.OK)
    public void updatePermissions(
        @Parameter(description = "Role ID", required = true) @PathVariable String roleId,
        @Parameter(description = "Set of permissions", required = true) @RequestBody Set<RolePermission> permissions
    ) throws ThingsboardException {
        checkParameter("roleId", roleId);
        RoleId id = new RoleId(toUUID(roleId));
        SecurityUser currentUser = getCurrentUser();
        checkNotNull(roleService.findRoleById(currentUser.getTenantId(), id));
        
        validatePermissions(permissions);
        roleService.updateRolePermissions(id, permissions);
    }

    @Operation(summary = "Add role permissions", description = "Adds new permissions to a role without removing existing ones. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permissions added successfully"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "400", description = "Invalid permissions")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/{roleId}/permissions")
    @ResponseStatus(HttpStatus.OK)
    public void addPermissions(
        @Parameter(description = "Role ID", required = true) @PathVariable String roleId,
        @Parameter(description = "Set of permissions to add", required = true) @RequestBody Set<RolePermission> permissions
    ) throws ThingsboardException {
        checkParameter("roleId", roleId);
        RoleId id = new RoleId(toUUID(roleId));
        SecurityUser currentUser = getCurrentUser();
        checkNotNull(roleService.findRoleById(currentUser.getTenantId(), id));
        
        validatePermissions(permissions);
        roleService.addRolePermissions(id, permissions);
    }

    @Operation(summary = "Remove role permissions", description = "Removes specific permissions from a role. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permissions removed successfully"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{roleId}/permissions")
    @ResponseStatus(HttpStatus.OK)
    public void removePermissions(
        @Parameter(description = "Role ID", required = true) @PathVariable String roleId,
        @Parameter(description = "Set of permissions to remove", required = true) @RequestBody Set<RolePermission> permissions
    ) throws ThingsboardException {
        checkParameter("roleId", roleId);
        RoleId id = new RoleId(toUUID(roleId));
        SecurityUser currentUser = getCurrentUser();
        checkNotNull(roleService.findRoleById(currentUser.getTenantId(), id));
        
        roleService.removeRolePermissions(id, permissions);
    }

    @Operation(summary = "Get available resources", description = "Returns all available resource types that can be used in permissions. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/resources")
    public List<String> getAvailableResources() {
        return Arrays.stream(Resource.values())
            .map(Enum::name)
            .collect(Collectors.toList());
    }

    @Operation(summary = "Get available operations", description = "Returns all available operation types that can be used in permissions. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/operations")
    public List<String> getAvailableOperations() {
        return Arrays.stream(org.thingsboard.server.service.security.permission.Operation.values())
            .map(Enum::name)
            .collect(Collectors.toList());
    }

    private void validatePermissions(Set<RolePermission> permissions) throws ThingsboardException {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        
        for (RolePermission permission : permissions) {
            if (permission.getResource() == null || permission.getOperation() == null) {
                throw new ThingsboardException("Permission must have both resource and operation", 
                    org.thingsboard.server.common.data.exception.ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            
            try {
                Resource.valueOf(permission.getResource().name());
                org.thingsboard.server.service.security.permission.Operation.valueOf(permission.getOperation().name());
            } catch (IllegalArgumentException e) {
                throw new ThingsboardException("Invalid resource or operation in permission", 
                    org.thingsboard.server.common.data.exception.ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        }
    }
}
