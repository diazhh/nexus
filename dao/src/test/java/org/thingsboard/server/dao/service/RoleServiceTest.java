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
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.role.RoleService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@DaoSqlTest
public class RoleServiceTest extends AbstractServiceTest {

    @Autowired
    private RoleService roleService;

    @Test
    public void testSaveRole() {
        Role role = createTestRole("Test Role", "Test role description");
        
        Role savedRole = roleService.saveRole(role);
        
        Assert.assertNotNull(savedRole);
        Assert.assertNotNull(savedRole.getId());
        Assert.assertEquals(role.getName(), savedRole.getName());
        Assert.assertEquals(role.getDescription(), savedRole.getDescription());
        Assert.assertEquals(tenantId, savedRole.getTenantId());
        Assert.assertFalse(savedRole.isSystemRole());
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testFindRoleById() {
        Role role = createTestRole("Find Test Role", "Description");
        Role savedRole = roleService.saveRole(role);
        
        Role foundRole = roleService.findRoleById(tenantId, savedRole.getId());
        
        Assert.assertNotNull(foundRole);
        Assert.assertEquals(savedRole.getId(), foundRole.getId());
        Assert.assertEquals(savedRole.getName(), foundRole.getName());
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testFindRoleByTenantIdAndName() {
        Role role = createTestRole("Unique Name Role", "Description");
        Role savedRole = roleService.saveRole(role);
        
        Role foundRole = roleService.findRoleByTenantIdAndName(tenantId, "Unique Name Role");
        
        Assert.assertNotNull(foundRole);
        Assert.assertEquals(savedRole.getId(), foundRole.getId());
        Assert.assertEquals("Unique Name Role", foundRole.getName());
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testUpdateRole() {
        Role role = createTestRole("Original Name", "Original description");
        Role savedRole = roleService.saveRole(role);
        
        savedRole.setName("Updated Name");
        savedRole.setDescription("Updated description");
        Role updatedRole = roleService.saveRole(savedRole);
        
        Assert.assertEquals("Updated Name", updatedRole.getName());
        Assert.assertEquals("Updated description", updatedRole.getDescription());
        Assert.assertEquals(savedRole.getId(), updatedRole.getId());
        
        roleService.deleteRole(tenantId, updatedRole.getId());
    }

    @Test
    public void testDeleteRole() {
        Role role = createTestRole("Delete Test Role", "Description");
        Role savedRole = roleService.saveRole(role);
        RoleId roleId = savedRole.getId();
        
        roleService.deleteRole(tenantId, roleId);
        
        Role deletedRole = roleService.findRoleById(tenantId, roleId);
        Assert.assertNull(deletedRole);
    }

    @Test(expected = DataValidationException.class)
    public void testDeleteSystemRole() {
        Role systemRole = createTestRole("System Role", "System role");
        systemRole.setSystem(true);
        Role savedRole = roleService.saveRole(systemRole);
        
        try {
            roleService.deleteRole(tenantId, savedRole.getId());
        } finally {
            savedRole.setSystem(false);
            roleService.saveRole(savedRole);
            roleService.deleteRole(tenantId, savedRole.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDuplicateRoleName() {
        Role role1 = createTestRole("Duplicate Name", "First role");
        Role savedRole1 = roleService.saveRole(role1);
        
        try {
            Role role2 = createTestRole("Duplicate Name", "Second role");
            roleService.saveRole(role2);
        } finally {
            roleService.deleteRole(tenantId, savedRole1.getId());
        }
    }

    @Test
    public void testFindRolesByTenantId() {
        Role role1 = createTestRole("Role 1", "Description 1");
        Role role2 = createTestRole("Role 2", "Description 2");
        Role role3 = createTestRole("Role 3", "Description 3");
        
        Role savedRole1 = roleService.saveRole(role1);
        Role savedRole2 = roleService.saveRole(role2);
        Role savedRole3 = roleService.saveRole(role3);
        
        PageLink pageLink = new PageLink(10, 0);
        PageData<Role> roles = roleService.findRolesByTenantId(tenantId, pageLink);
        
        Assert.assertNotNull(roles);
        Assert.assertTrue(roles.getData().size() >= 3);
        
        roleService.deleteRole(tenantId, savedRole1.getId());
        roleService.deleteRole(tenantId, savedRole2.getId());
        roleService.deleteRole(tenantId, savedRole3.getId());
    }

    @Test
    public void testGetRolePermissions() {
        Role role = createTestRole("Permissions Test Role", "Description");
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.WRITE));
        permissions.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.READ));
        
        roleService.updateRolePermissions(savedRole.getId(), permissions);
        
        Set<RolePermission> retrievedPermissions = roleService.getRolePermissions(savedRole.getId());
        
        Assert.assertNotNull(retrievedPermissions);
        Assert.assertEquals(3, retrievedPermissions.size());
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testUpdateRolePermissions() {
        Role role = createTestRole("Update Permissions Role", "Description");
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> initialPermissions = new HashSet<>();
        initialPermissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), initialPermissions);
        
        Set<RolePermission> updatedPermissions = new HashSet<>();
        updatedPermissions.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.WRITE));
        updatedPermissions.add(new RolePermission(savedRole.getId(), Resource.DASHBOARD, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), updatedPermissions);
        
        Set<RolePermission> retrievedPermissions = roleService.getRolePermissions(savedRole.getId());
        
        Assert.assertEquals(2, retrievedPermissions.size());
        Assert.assertTrue(retrievedPermissions.stream()
                .anyMatch(p -> p.getResource() == Resource.ASSET && p.getOperation() == Operation.WRITE));
        Assert.assertTrue(retrievedPermissions.stream()
                .anyMatch(p -> p.getResource() == Resource.DASHBOARD && p.getOperation() == Operation.READ));
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testAddRolePermissions() {
        Role role = createTestRole("Add Permissions Role", "Description");
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> initialPermissions = new HashSet<>();
        initialPermissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), initialPermissions);
        
        Set<RolePermission> additionalPermissions = new HashSet<>();
        additionalPermissions.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.READ));
        roleService.addRolePermissions(savedRole.getId(), additionalPermissions);
        
        Set<RolePermission> retrievedPermissions = roleService.getRolePermissions(savedRole.getId());
        
        Assert.assertEquals(2, retrievedPermissions.size());
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testRemoveRolePermissions() {
        Role role = createTestRole("Remove Permissions Role", "Description");
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.READ));
        permissions.add(new RolePermission(savedRole.getId(), Resource.DASHBOARD, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), permissions);
        
        Set<RolePermission> permissionsToRemove = new HashSet<>();
        permissionsToRemove.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.READ));
        roleService.removeRolePermissions(savedRole.getId(), permissionsToRemove);
        
        Set<RolePermission> retrievedPermissions = roleService.getRolePermissions(savedRole.getId());
        
        Assert.assertEquals(2, retrievedPermissions.size());
        Assert.assertFalse(retrievedPermissions.stream()
                .anyMatch(p -> p.getResource() == Resource.ASSET));
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testCreateDefaultTenantRoles() {
        TenantId newTenantId = TenantId.fromUUID(UUID.randomUUID());
        
        roleService.createDefaultTenantRoles(newTenantId);
        
        PageLink pageLink = new PageLink(10, 0);
        PageData<Role> roles = roleService.findRolesByTenantId(newTenantId, pageLink);
        
        Assert.assertNotNull(roles);
        Assert.assertTrue(roles.getData().size() >= 2);
        
        boolean hasTenantAdmin = roles.getData().stream()
                .anyMatch(r -> r.getName().equals("Tenant Administrator"));
        boolean hasCustomerUser = roles.getData().stream()
                .anyMatch(r -> r.getName().equals("Customer User"));
        
        Assert.assertTrue(hasTenantAdmin);
        Assert.assertTrue(hasCustomerUser);
        
        roleService.deleteRolesByTenantId(newTenantId);
    }

    @Test
    public void testRolePermissionsWithAllOperation() {
        Role role = createTestRole("All Operations Role", "Description");
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.ALL));
        roleService.updateRolePermissions(savedRole.getId(), permissions);
        
        Set<RolePermission> retrievedPermissions = roleService.getRolePermissions(savedRole.getId());
        
        Assert.assertEquals(1, retrievedPermissions.size());
        RolePermission permission = retrievedPermissions.iterator().next();
        Assert.assertEquals(Operation.ALL, permission.getOperation());
        Assert.assertEquals(Resource.DEVICE, permission.getResource());
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testRolePermissionsWithAllResource() {
        Role role = createTestRole("All Resources Role", "Description");
        Role savedRole = roleService.saveRole(role);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(savedRole.getId(), Resource.ALL, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), permissions);
        
        Set<RolePermission> retrievedPermissions = roleService.getRolePermissions(savedRole.getId());
        
        Assert.assertEquals(1, retrievedPermissions.size());
        RolePermission permission = retrievedPermissions.iterator().next();
        Assert.assertEquals(Resource.ALL, permission.getResource());
        Assert.assertEquals(Operation.READ, permission.getOperation());
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveRoleWithNullName() {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setDescription("Role without name");
        
        roleService.saveRole(role);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveRoleWithEmptyName() {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName("");
        role.setDescription("Role with empty name");
        
        roleService.saveRole(role);
    }

    @Test
    public void testFindRolesByTenantIdWithPagination() {
        for (int i = 0; i < 15; i++) {
            Role role = createTestRole("Pagination Role " + i, "Description " + i);
            roleService.saveRole(role);
        }
        
        PageLink pageLink = new PageLink(5, 0);
        PageData<Role> firstPage = roleService.findRolesByTenantId(tenantId, pageLink);
        
        Assert.assertNotNull(firstPage);
        Assert.assertTrue(firstPage.getData().size() <= 5);
        Assert.assertTrue(firstPage.hasNext());
        
        PageLink nextPageLink = pageLink.nextPageLink();
        PageData<Role> secondPage = roleService.findRolesByTenantId(tenantId, nextPageLink);
        
        Assert.assertNotNull(secondPage);
        Assert.assertTrue(secondPage.getData().size() > 0);
        
        roleService.deleteRolesByTenantId(tenantId);
    }

    @Test
    public void testCountUsersByRoleId() {
        Role role = createTestRole("Count Users Role", "Description");
        Role savedRole = roleService.saveRole(role);
        
        long count = roleService.countUsersByRoleId(savedRole.getId());
        
        Assert.assertEquals(0, count);
        
        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testDeleteRolesByTenantId() {
        Role role1 = createTestRole("Tenant Delete Role 1", "Description");
        Role role2 = createTestRole("Tenant Delete Role 2", "Description");
        
        roleService.saveRole(role1);
        roleService.saveRole(role2);
        
        roleService.deleteRolesByTenantId(tenantId);
        
        PageLink pageLink = new PageLink(10, 0);
        PageData<Role> roles = roleService.findRolesByTenantId(tenantId, pageLink);
        
        Assert.assertEquals(0, roles.getData().size());
    }

    private Role createTestRole(String name, String description) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName(name);
        role.setDescription(description);
        role.setSystem(false);
        return role;
    }
}
