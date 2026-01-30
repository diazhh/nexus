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
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.NexusModuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.nexus.NexusModule;
import org.thingsboard.server.common.data.nexus.TenantModule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.nexus.NexusModuleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.*;

/**
 * REST Controller for NEXUS Module Management.
 * Handles module definitions and tenant-module assignments.
 */
@Tag(name = "NEXUS Module Management", description = "APIs for managing NEXUS modules and tenant assignments")
@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/nexus/module")
public class NexusModuleController extends BaseController {

    private final NexusModuleService moduleService;

    // ========================
    // Module Management (SysAdmin)
    // ========================

    @Operation(summary = "Get all modules", description = "Returns a page of all available modules. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping
    public PageData<NexusModule> getModules(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Text search term") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(moduleService.findAllModules(pageLink));
    }

    @Operation(summary = "Get module by ID", description = "Returns a single module by its ID. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module found"),
        @ApiResponse(responseCode = "404", description = "Module not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping("/{moduleId}")
    public NexusModule getModuleById(
            @Parameter(description = "Module ID", required = true) @PathVariable String moduleId
    ) throws ThingsboardException {
        checkParameter("moduleId", moduleId);
        NexusModuleId id = new NexusModuleId(toUUID(moduleId));
        return checkNotNull(moduleService.findModuleById(id));
    }

    @Operation(summary = "Get module by key", description = "Returns a single module by its unique key (CT, RV, DR, etc.). " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module found"),
        @ApiResponse(responseCode = "404", description = "Module not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping("/key/{moduleKey}")
    public NexusModule getModuleByKey(
            @Parameter(description = "Module key (e.g., CT, RV, DR)", required = true) @PathVariable String moduleKey
    ) throws ThingsboardException {
        checkParameter("moduleKey", moduleKey);
        return checkNotNull(moduleService.findModuleByKey(moduleKey).orElse(null));
    }

    @Operation(summary = "Get available modules", description = "Returns all modules that can be assigned to tenants. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping("/available")
    public List<NexusModule> getAvailableModules() throws ThingsboardException {
        return moduleService.findAvailableModules();
    }

    @Operation(summary = "Create or update module", description = "Creates a new module or updates an existing one. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping
    public NexusModule saveModule(
            @Parameter(description = "A JSON value representing the Module", required = true) @RequestBody NexusModule module
    ) throws ThingsboardException {
        return checkNotNull(moduleService.saveModule(module));
    }

    @Operation(summary = "Delete module", description = "Deletes a module by its ID. Cannot delete modules in use by tenants. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Module not found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "400", description = "Module is in use")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @DeleteMapping("/{moduleId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteModule(
            @Parameter(description = "Module ID", required = true) @PathVariable String moduleId
    ) throws ThingsboardException {
        checkParameter("moduleId", moduleId);
        NexusModuleId id = new NexusModuleId(toUUID(moduleId));
        moduleService.deleteModule(id);
    }

    // ========================
    // Tenant Module Assignment (SysAdmin)
    // ========================

    @Operation(summary = "Get tenant modules", description = "Returns all modules assigned to a specific tenant. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping("/tenant/{tenantId}")
    public List<TenantModule> getTenantModules(
            @Parameter(description = "Tenant ID", required = true) @PathVariable String tenantId
    ) throws ThingsboardException {
        checkParameter("tenantId", tenantId);
        TenantId tid = TenantId.fromUUID(toUUID(tenantId));
        return moduleService.findModulesByTenantId(tid);
    }

    @Operation(summary = "Get active tenant modules", description = "Returns only active modules for a specific tenant. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping("/tenant/{tenantId}/active")
    public List<TenantModule> getActiveTenantModules(
            @Parameter(description = "Tenant ID", required = true) @PathVariable String tenantId
    ) throws ThingsboardException {
        checkParameter("tenantId", tenantId);
        TenantId tid = TenantId.fromUUID(toUUID(tenantId));
        return moduleService.findActiveModulesByTenantId(tid);
    }

    @Operation(summary = "Assign module to tenant", description = "Assigns a module to a tenant, enabling access. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Module already assigned"),
        @ApiResponse(responseCode = "404", description = "Module or tenant not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping("/tenant/{tenantId}/assign/{moduleId}")
    public TenantModule assignModuleToTenant(
            @Parameter(description = "Tenant ID", required = true) @PathVariable String tenantId,
            @Parameter(description = "Module ID", required = true) @PathVariable String moduleId
    ) throws ThingsboardException {
        checkParameter("tenantId", tenantId);
        checkParameter("moduleId", moduleId);

        TenantId tid = TenantId.fromUUID(toUUID(tenantId));
        NexusModuleId mid = new NexusModuleId(toUUID(moduleId));
        SecurityUser currentUser = getCurrentUser();

        return checkNotNull(moduleService.assignModuleToTenant(tid, mid, currentUser.getId()));
    }

    @Operation(summary = "Unassign module from tenant", description = "Removes module assignment from a tenant. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module unassigned successfully"),
        @ApiResponse(responseCode = "404", description = "Assignment not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @DeleteMapping("/tenant/{tenantId}/unassign/{moduleId}")
    @ResponseStatus(HttpStatus.OK)
    public void unassignModuleFromTenant(
            @Parameter(description = "Tenant ID", required = true) @PathVariable String tenantId,
            @Parameter(description = "Module ID", required = true) @PathVariable String moduleId
    ) throws ThingsboardException {
        checkParameter("tenantId", tenantId);
        checkParameter("moduleId", moduleId);

        TenantId tid = TenantId.fromUUID(toUUID(tenantId));
        NexusModuleId mid = new NexusModuleId(toUUID(moduleId));

        moduleService.unassignModuleFromTenant(tid, mid);
    }

    @Operation(summary = "Activate module for tenant", description = "Reactivates a previously deactivated module for a tenant. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module activated successfully"),
        @ApiResponse(responseCode = "404", description = "Assignment not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping("/tenant/{tenantId}/activate/{moduleId}")
    public TenantModule activateModuleForTenant(
            @Parameter(description = "Tenant ID", required = true) @PathVariable String tenantId,
            @Parameter(description = "Module ID", required = true) @PathVariable String moduleId
    ) throws ThingsboardException {
        checkParameter("tenantId", tenantId);
        checkParameter("moduleId", moduleId);

        TenantId tid = TenantId.fromUUID(toUUID(tenantId));
        NexusModuleId mid = new NexusModuleId(toUUID(moduleId));

        return checkNotNull(moduleService.activateModuleForTenant(tid, mid));
    }

    @Operation(summary = "Deactivate module for tenant", description = "Deactivates a module for a tenant without removing the assignment. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Assignment not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PostMapping("/tenant/{tenantId}/deactivate/{moduleId}")
    public TenantModule deactivateModuleForTenant(
            @Parameter(description = "Tenant ID", required = true) @PathVariable String tenantId,
            @Parameter(description = "Module ID", required = true) @PathVariable String moduleId
    ) throws ThingsboardException {
        checkParameter("tenantId", tenantId);
        checkParameter("moduleId", moduleId);

        TenantId tid = TenantId.fromUUID(toUUID(tenantId));
        NexusModuleId mid = new NexusModuleId(toUUID(moduleId));

        return checkNotNull(moduleService.deactivateModuleForTenant(tid, mid));
    }

    @Operation(summary = "Count tenants using module", description = "Returns the number of tenants using a specific module. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @GetMapping("/{moduleId}/tenants/count")
    public long countTenantsUsingModule(
            @Parameter(description = "Module ID", required = true) @PathVariable String moduleId
    ) throws ThingsboardException {
        checkParameter("moduleId", moduleId);
        NexusModuleId id = new NexusModuleId(toUUID(moduleId));
        return moduleService.countTenantsUsingModule(id);
    }

    // ========================
    // Current User APIs (TenantAdmin)
    // ========================

    @Operation(summary = "Get my modules", description = "Returns all active modules for the current tenant. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/my")
    public List<TenantModule> getMyModules() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        return moduleService.findActiveModulesByTenantId(currentUser.getTenantId());
    }

    @Operation(summary = "Get my module keys", description = "Returns the set of active module keys for the current tenant. Used for menu filtering. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/my/keys")
    public Set<String> getMyModuleKeys() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        return moduleService.getActiveModuleKeysForTenant(currentUser.getTenantId());
    }

    @Operation(summary = "Check module access", description = "Checks if the current tenant has access to a specific module. " + TENANT_AUTHORITY_PARAGRAPH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/my/access/{moduleKey}")
    public boolean hasModuleAccess(
            @Parameter(description = "Module key (e.g., CT, RV, DR)", required = true) @PathVariable String moduleKey
    ) throws ThingsboardException {
        checkParameter("moduleKey", moduleKey);
        SecurityUser currentUser = getCurrentUser();
        return moduleService.hasTenantModuleAccess(currentUser.getTenantId(), moduleKey);
    }
}
