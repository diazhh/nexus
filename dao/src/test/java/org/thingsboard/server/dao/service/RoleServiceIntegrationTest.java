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
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.role.RoleBasedPermissionChecker;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.user.UserService;

import java.util.HashSet;
import java.util.Set;

@DaoSqlTest
@Transactional
public class RoleServiceIntegrationTest extends AbstractServiceTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleBasedPermissionChecker permissionChecker;

    @Test
    public void testFullRoleLifecycle() {
        Role role = new Role();
        role.setName("Integration Test Role");
        role.setDescription("Role for integration testing");
        role.setTenantId(tenantId);
        role.setSystem(false);
        
        Role created = roleService.saveRole(role);
        Assert.assertNotNull(created.getId());
        Assert.assertEquals("Integration Test Role", created.getName());
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(created.getId(), Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(created.getId(), Resource.ASSET, Operation.WRITE));
        roleService.updateRolePermissions(created.getId(), permissions);
        
        Set<RolePermission> loaded = roleService.getRolePermissions(created.getId());
        Assert.assertEquals(2, loaded.size());
        
        created.setDescription("Updated description");
        Role updated = roleService.saveRole(created);
        Assert.assertEquals("Updated description", updated.getDescription());
        
        roleService.deleteRole(tenantId, created.getId());
        
        Role deleted = roleService.findRoleById(tenantId, created.getId());
        Assert.assertNull(deleted);
    }

    @Test
    public void testRoleWithUserAssignment() {
        Role role = new Role();
        role.setName("User Assignment Role");
        role.setDescription("Role for user assignment test");
        role.setTenantId(tenantId);
        role.setSystem(false);
        
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.WRITE));
        roleService.updateRolePermissions(savedRole.getId(), permissions);
        
        User user = new User();
        user.setEmail("roletest@example.com");
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setRoleId(savedRole.getId());
        
        User savedUser = userService.saveUser(tenantId, user);
        Assert.assertNotNull(savedUser);
        Assert.assertEquals(savedRole.getId(), savedUser.getRoleId());
        
        Assert.assertTrue(permissionChecker.hasPermission(savedUser, Resource.DEVICE, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(savedUser, Resource.DEVICE, Operation.WRITE));
        Assert.assertFalse(permissionChecker.hasPermission(savedUser, Resource.DEVICE, Operation.DELETE));
        
        userService.deleteUser(tenantId, savedUser);
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testPermissionUpdateAndCacheInvalidation() {
        Role role = new Role();
        role.setName("Cache Test Role");
        role.setDescription("Role for cache invalidation test");
        role.setTenantId(tenantId);
        role.setSystem(false);
        
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> initialPermissions = new HashSet<>();
        initialPermissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), initialPermissions);
        
        Set<RolePermission> loaded1 = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(1, loaded1.size());
        
        Set<RolePermission> updatedPermissions = new HashSet<>();
        updatedPermissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        updatedPermissions.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.WRITE));
        roleService.updateRolePermissions(savedRole.getId(), updatedPermissions);
        
        Set<RolePermission> loaded2 = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(2, loaded2.size());
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testAddAndRemovePermissions() {
        Role role = new Role();
        role.setName("Add Remove Permissions Role");
        role.setDescription("Role for add/remove permissions test");
        role.setTenantId(tenantId);
        role.setSystem(false);
        
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> initialPermissions = new HashSet<>();
        initialPermissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), initialPermissions);
        
        Set<RolePermission> additionalPermissions = new HashSet<>();
        additionalPermissions.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.READ));
        additionalPermissions.add(new RolePermission(savedRole.getId(), Resource.DASHBOARD, Operation.READ));
        roleService.addRolePermissions(savedRole.getId(), additionalPermissions);
        
        Set<RolePermission> afterAdd = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(3, afterAdd.size());
        
        Set<RolePermission> permissionsToRemove = new HashSet<>();
        permissionsToRemove.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.READ));
        roleService.removeRolePermissions(savedRole.getId(), permissionsToRemove);
        
        Set<RolePermission> afterRemove = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(2, afterRemove.size());
        Assert.assertTrue(afterRemove.stream()
                .anyMatch(p -> p.getResource() == Resource.DEVICE && p.getOperation() == Operation.READ));
        Assert.assertTrue(afterRemove.stream()
                .anyMatch(p -> p.getResource() == Resource.DASHBOARD && p.getOperation() == Operation.READ));
        Assert.assertFalse(afterRemove.stream()
                .anyMatch(p -> p.getResource() == Resource.ASSET));
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testMultipleRolesWithDifferentPermissions() {
        Role adminRole = new Role();
        adminRole.setName("Admin Role");
        adminRole.setDescription("Admin role with full permissions");
        adminRole.setTenantId(tenantId);
        adminRole.setSystem(false);
        Role savedAdminRole = roleService.saveRole(adminRole);
        
        Set<RolePermission> adminPermissions = new HashSet<>();
        adminPermissions.add(new RolePermission(savedAdminRole.getId(), Resource.ALL, Operation.ALL));
        roleService.updateRolePermissions(savedAdminRole.getId(), adminPermissions);
        
        Role viewerRole = new Role();
        viewerRole.setName("Viewer Role");
        viewerRole.setDescription("Viewer role with read-only permissions");
        viewerRole.setTenantId(tenantId);
        viewerRole.setSystem(false);
        Role savedViewerRole = roleService.saveRole(viewerRole);
        
        Set<RolePermission> viewerPermissions = new HashSet<>();
        viewerPermissions.add(new RolePermission(savedViewerRole.getId(), Resource.DEVICE, Operation.READ));
        viewerPermissions.add(new RolePermission(savedViewerRole.getId(), Resource.ASSET, Operation.READ));
        roleService.updateRolePermissions(savedViewerRole.getId(), viewerPermissions);
        
        User adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setAuthority(Authority.TENANT_ADMIN);
        adminUser.setTenantId(tenantId);
        adminUser.setRoleId(savedAdminRole.getId());
        User savedAdminUser = userService.saveUser(tenantId, adminUser);
        
        User viewerUser = new User();
        viewerUser.setEmail("viewer@example.com");
        viewerUser.setAuthority(Authority.TENANT_ADMIN);
        viewerUser.setTenantId(tenantId);
        viewerUser.setRoleId(savedViewerRole.getId());
        User savedViewerUser = userService.saveUser(tenantId, viewerUser);
        
        Assert.assertTrue(permissionChecker.hasPermission(savedAdminUser, Resource.DEVICE, Operation.DELETE));
        Assert.assertTrue(permissionChecker.hasPermission(savedAdminUser, Resource.ASSET, Operation.WRITE));
        
        Assert.assertTrue(permissionChecker.hasPermission(savedViewerUser, Resource.DEVICE, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(savedViewerUser, Resource.DEVICE, Operation.WRITE));
        Assert.assertFalse(permissionChecker.hasPermission(savedViewerUser, Resource.DASHBOARD, Operation.READ));
        
        userService.deleteUser(tenantId, savedAdminUser);
        userService.deleteUser(tenantId, savedViewerUser);
        roleService.deleteRole(tenantId, savedAdminRole.getId());
        roleService.deleteRole(tenantId, savedViewerRole.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testDeleteRoleWithAssignedUsers() {
        Role role = new Role();
        role.setName("Role With Users");
        role.setDescription("Role that has assigned users");
        role.setTenantId(tenantId);
        role.setSystem(false);
        
        Role savedRole = roleService.saveRole(role);
        
        User user = new User();
        user.setEmail("assigned@example.com");
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setRoleId(savedRole.getId());
        User savedUser = userService.saveUser(tenantId, user);
        
        try {
            roleService.deleteRole(tenantId, savedRole.getId());
        } finally {
            userService.deleteUser(tenantId, savedUser);
            roleService.deleteRole(tenantId, savedRole.getId());
        }
    }

    @Test
    public void testDefaultTenantRolesCreation() {
        roleService.createDefaultTenantRoles(tenantId);
        
        PageLink pageLink = new PageLink(10, 0);
        PageData<Role> roles = roleService.findRolesByTenantId(tenantId, pageLink);
        
        Assert.assertNotNull(roles);
        Assert.assertTrue(roles.getData().size() >= 2);
        
        Role tenantAdminRole = roles.getData().stream()
                .filter(r -> r.getName().equals("Tenant Administrator"))
                .findFirst()
                .orElse(null);
        Assert.assertNotNull(tenantAdminRole);
        
        Set<RolePermission> adminPermissions = roleService.getRolePermissions(tenantAdminRole.getId());
        Assert.assertFalse(adminPermissions.isEmpty());
        Assert.assertTrue(adminPermissions.stream()
                .anyMatch(p -> p.getResource() == Resource.ALL && p.getOperation() == Operation.ALL));
        
        Role customerUserRole = roles.getData().stream()
                .filter(r -> r.getName().equals("Customer User"))
                .findFirst()
                .orElse(null);
        Assert.assertNotNull(customerUserRole);
        
        Set<RolePermission> customerPermissions = roleService.getRolePermissions(customerUserRole.getId());
        Assert.assertFalse(customerPermissions.isEmpty());
        Assert.assertTrue(customerPermissions.stream()
                .anyMatch(p -> p.getResource() == Resource.DEVICE && p.getOperation() == Operation.READ));
    }

    @Test
    public void testRolePermissionsWithAllOperations() {
        Role role = new Role();
        role.setName("All Operations Role");
        role.setDescription("Role with ALL operations");
        role.setTenantId(tenantId);
        role.setSystem(false);
        
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.ALL));
        roleService.updateRolePermissions(savedRole.getId(), permissions);
        
        User user = new User();
        user.setEmail("allops@example.com");
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setRoleId(savedRole.getId());
        User savedUser = userService.saveUser(tenantId, user);
        
        Assert.assertTrue(permissionChecker.hasPermission(savedUser, Resource.DEVICE, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(savedUser, Resource.DEVICE, Operation.WRITE));
        Assert.assertTrue(permissionChecker.hasPermission(savedUser, Resource.DEVICE, Operation.DELETE));
        Assert.assertTrue(permissionChecker.hasPermission(savedUser, Resource.DEVICE, Operation.RPC_CALL));
        
        Assert.assertFalse(permissionChecker.hasPermission(savedUser, Resource.ASSET, Operation.READ));
        
        userService.deleteUser(tenantId, savedUser);
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testRolePermissionsWithAllResources() {
        Role role = new Role();
        role.setName("All Resources Role");
        role.setDescription("Role with ALL resources");
        role.setTenantId(tenantId);
        role.setSystem(false);
        
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(savedRole.getId(), Resource.ALL, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), permissions);
        
        User user = new User();
        user.setEmail("allres@example.com");
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setRoleId(savedRole.getId());
        User savedUser = userService.saveUser(tenantId, user);
        
        Assert.assertTrue(permissionChecker.hasPermission(savedUser, Resource.DEVICE, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(savedUser, Resource.ASSET, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(savedUser, Resource.DASHBOARD, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(savedUser, Resource.RULE_CHAIN, Operation.READ));
        
        Assert.assertFalse(permissionChecker.hasPermission(savedUser, Resource.DEVICE, Operation.WRITE));
        
        userService.deleteUser(tenantId, savedUser);
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testPaginationWithMultipleRoles() {
        for (int i = 0; i < 25; i++) {
            Role role = new Role();
            role.setName("Pagination Role " + i);
            role.setDescription("Description " + i);
            role.setTenantId(tenantId);
            role.setSystem(false);
            roleService.saveRole(role);
        }
        
        PageLink firstPageLink = new PageLink(10, 0);
        PageData<Role> firstPage = roleService.findRolesByTenantId(tenantId, firstPageLink);
        
        Assert.assertNotNull(firstPage);
        Assert.assertTrue(firstPage.getData().size() <= 10);
        Assert.assertTrue(firstPage.hasNext());
        
        PageLink secondPageLink = firstPageLink.nextPageLink();
        PageData<Role> secondPage = roleService.findRolesByTenantId(tenantId, secondPageLink);
        
        Assert.assertNotNull(secondPage);
        Assert.assertTrue(secondPage.getData().size() > 0);
        
        roleService.deleteRolesByTenantId(tenantId);
    }
}
