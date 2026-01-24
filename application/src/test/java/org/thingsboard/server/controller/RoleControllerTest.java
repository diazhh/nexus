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

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RoleControllerTest extends AbstractControllerTest {

    static final TypeReference<PageData<Role>> PAGE_DATA_ROLE_TYPE_REFERENCE = new TypeReference<>() {};

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("Role Test Tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("roletest@thingsboard.org");
        tenantAdmin.setFirstName("Role");
        tenantAdmin.setLastName("Test");
        tenantAdmin = createUserAndLogin(tenantAdmin, "password");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        if (savedTenant != null) {
            deleteTenant(savedTenant.getId());
        }
    }

    @Test
    public void testSaveRole() throws Exception {
        Role role = new Role();
        role.setName("Test Role");
        role.setDescription("Test role description");
        role.setTenantId(savedTenant.getId());

        Role savedRole = doPost("/api/role", role, Role.class);

        Assert.assertNotNull(savedRole);
        Assert.assertNotNull(savedRole.getId());
        Assert.assertEquals(role.getName(), savedRole.getName());
        Assert.assertEquals(role.getDescription(), savedRole.getDescription());
        Assert.assertEquals(savedTenant.getId(), savedRole.getTenantId());
    }

    @Test
    public void testGetRoleById() throws Exception {
        Role role = createRole("Get Role Test");

        Role foundRole = doGet("/api/role/" + role.getId().getId().toString(), Role.class);

        Assert.assertNotNull(foundRole);
        Assert.assertEquals(role.getId(), foundRole.getId());
        Assert.assertEquals(role.getName(), foundRole.getName());
    }

    @Test
    public void testGetRoles() throws Exception {
        createRole("Role 1");
        createRole("Role 2");
        createRole("Role 3");

        PageData<Role> pageData = doGetTyped("/api/role?pageSize=10&page=0", PAGE_DATA_ROLE_TYPE_REFERENCE);

        Assert.assertNotNull(pageData);
        Assert.assertNotNull(pageData.getData());
        Assert.assertTrue(pageData.getData().size() >= 3);
    }

    @Test
    public void testUpdateRole() throws Exception {
        Role role = createRole("Original Name");

        role.setName("Updated Name");
        role.setDescription("Updated description");

        Role updatedRole = doPost("/api/role", role, Role.class);

        Assert.assertNotNull(updatedRole);
        Assert.assertEquals(role.getId(), updatedRole.getId());
        Assert.assertEquals("Updated Name", updatedRole.getName());
        Assert.assertEquals("Updated description", updatedRole.getDescription());
    }

    @Test
    public void testDeleteRole() throws Exception {
        Role role = createRole("Role to Delete");

        doDelete("/api/role/" + role.getId().getId().toString())
            .andExpect(status().isOk());

        doGet("/api/role/" + role.getId().getId().toString())
            .andExpect(status().isNotFound());
    }

    @Test
    public void testGetRolePermissions() throws Exception {
        Role role = createRole("Permissions Test Role");
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(role.getId(), Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(role.getId(), Resource.DEVICE, Operation.WRITE));
        
        doPut("/api/role/" + role.getId().getId().toString() + "/permissions", permissions)
            .andExpect(status().isOk());

        Set<RolePermission> retrievedPermissions = doGetTyped(
            "/api/role/" + role.getId().getId().toString() + "/permissions",
            new TypeReference<Set<RolePermission>>() {}
        );

        Assert.assertNotNull(retrievedPermissions);
        Assert.assertEquals(2, retrievedPermissions.size());
    }

    @Test
    public void testUpdateRolePermissions() throws Exception {
        Role role = createRole("Update Permissions Role");

        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(role.getId(), Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(role.getId(), Resource.ASSET, Operation.READ));

        doPut("/api/role/" + role.getId().getId().toString() + "/permissions", permissions)
            .andExpect(status().isOk());

        Set<RolePermission> retrievedPermissions = doGetTyped(
            "/api/role/" + role.getId().getId().toString() + "/permissions",
            new TypeReference<Set<RolePermission>>() {}
        );

        Assert.assertEquals(2, retrievedPermissions.size());
    }

    @Test
    public void testAddRolePermissions() throws Exception {
        Role role = createRole("Add Permissions Role");

        Set<RolePermission> initialPermissions = new HashSet<>();
        initialPermissions.add(new RolePermission(role.getId(), Resource.DEVICE, Operation.READ));
        
        doPut("/api/role/" + role.getId().getId().toString() + "/permissions", initialPermissions)
            .andExpect(status().isOk());

        Set<RolePermission> additionalPermissions = new HashSet<>();
        additionalPermissions.add(new RolePermission(role.getId(), Resource.ASSET, Operation.READ));

        doPost("/api/role/" + role.getId().getId().toString() + "/permissions", additionalPermissions)
            .andExpect(status().isOk());

        Set<RolePermission> allPermissions = doGetTyped(
            "/api/role/" + role.getId().getId().toString() + "/permissions",
            new TypeReference<Set<RolePermission>>() {}
        );

        Assert.assertEquals(2, allPermissions.size());
    }

    @Test
    public void testUpdateRolePermissionsRemoval() throws Exception {
        Role role = createRole("Remove Permissions Role");

        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(role.getId(), Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(role.getId(), Resource.ASSET, Operation.READ));
        
        doPut("/api/role/" + role.getId().getId().toString() + "/permissions", permissions)
            .andExpect(status().isOk());

        Set<RolePermission> retrievedPermissions = doGetTyped(
            "/api/role/" + role.getId().getId().toString() + "/permissions",
            new TypeReference<Set<RolePermission>>() {}
        );
        Assert.assertEquals(2, retrievedPermissions.size());

        Set<RolePermission> updatedPermissions = new HashSet<>();
        updatedPermissions.add(new RolePermission(role.getId(), Resource.DEVICE, Operation.READ));
        
        doPut("/api/role/" + role.getId().getId().toString() + "/permissions", updatedPermissions)
            .andExpect(status().isOk());

        Set<RolePermission> remainingPermissions = doGetTyped(
            "/api/role/" + role.getId().getId().toString() + "/permissions",
            new TypeReference<Set<RolePermission>>() {}
        );

        Assert.assertEquals(1, remainingPermissions.size());
    }

    @Test
    public void testGetAvailableResources() throws Exception {
        List<String> resources = doGetTyped("/api/role/resources", new TypeReference<List<String>>() {});

        Assert.assertNotNull(resources);
        Assert.assertTrue(resources.size() > 0);
        Assert.assertTrue(resources.contains("DEVICE"));
        Assert.assertTrue(resources.contains("ASSET"));
    }

    @Test
    public void testGetAvailableOperations() throws Exception {
        List<String> operations = doGetTyped("/api/role/operations", new TypeReference<List<String>>() {});

        Assert.assertNotNull(operations);
        Assert.assertTrue(operations.size() > 0);
        Assert.assertTrue(operations.contains("READ"));
        Assert.assertTrue(operations.contains("WRITE"));
    }

    @Test
    public void testSaveRoleWithDuplicateName() throws Exception {
        Role role1 = createRole("Duplicate Name");

        Role role2 = new Role();
        role2.setName("Duplicate Name");
        role2.setTenantId(savedTenant.getId());

        doPost("/api/role", role2)
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteRoleInUse() throws Exception {
        Role role = createRole("Role In Use");
        
        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(savedTenant.getId());
        user.setEmail("roleuser@thingsboard.org");
        user.setFirstName("Role");
        user.setLastName("User");
        user.setRoleId(role.getId());
        
        User savedUser = doPost("/api/user?sendActivationMail=false", user, User.class);
        Assert.assertNotNull(savedUser);

        doDelete("/api/role/" + role.getId().getId().toString())
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetRolesByTextSearch() throws Exception {
        createRole("Search Test Alpha");
        createRole("Search Test Beta");
        createRole("Different Name");

        PageData<Role> pageData = doGetTyped(
            "/api/role?pageSize=10&page=0&textSearch=Search Test",
            PAGE_DATA_ROLE_TYPE_REFERENCE
        );

        Assert.assertNotNull(pageData);
        Assert.assertTrue(pageData.getData().size() >= 2);
    }

    private Role createRole(String name) throws Exception {
        Role role = new Role();
        role.setName(name);
        role.setDescription("Test role: " + name);
        role.setTenantId(savedTenant.getId());
        return doPost("/api/role", role, Role.class);
    }
}
