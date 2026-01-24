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
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;
import org.thingsboard.server.dao.role.RoleBasedPermissionChecker;
import org.thingsboard.server.dao.role.RoleService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class RoleBasedPermissionCheckerTest {

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleBasedPermissionChecker permissionChecker;

    private TenantId tenantId;
    private RoleId roleId;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        roleId = new RoleId(UUID.randomUUID());
    }

    @Test
    public void testSysAdminHasAllPermissions() {
        User sysAdmin = createUser(Authority.SYS_ADMIN, null);
        
        Assert.assertTrue(permissionChecker.hasPermission(sysAdmin, Resource.DEVICE, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(sysAdmin, Resource.DEVICE, Operation.WRITE));
        Assert.assertTrue(permissionChecker.hasPermission(sysAdmin, Resource.DEVICE, Operation.DELETE));
        Assert.assertTrue(permissionChecker.hasPermission(sysAdmin, Resource.TENANT, Operation.DELETE));
        Assert.assertTrue(permissionChecker.hasPermission(sysAdmin, Resource.ALL, Operation.ALL));
    }

    @Test
    public void testUserWithNullReturnsFalse() {
        Assert.assertFalse(permissionChecker.hasPermission(null, Resource.DEVICE, Operation.READ));
    }

    @Test
    public void testUserWithPermission() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.READ));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
    }

    @Test
    public void testUserWithoutPermission() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(Collections.emptySet());
        
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
    }

    @Test
    public void testUserWithDifferentPermission() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.READ));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.ASSET, Operation.READ));
    }

    @Test
    public void testPermissionWithAllOperation() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.ALL));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.DELETE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.RPC_CALL));
    }

    @Test
    public void testPermissionWithAllResource() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.ALL, Operation.READ));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.ASSET, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DASHBOARD, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
    }

    @Test
    public void testPermissionWithAllResourceAndAllOperation() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.ALL, Operation.ALL));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.ASSET, Operation.DELETE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DASHBOARD, Operation.RPC_CALL));
    }

    @Test
    public void testMultiplePermissions() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.WRITE));
        permissions.add(new RolePermission(roleId, Resource.ASSET, Operation.READ));
        permissions.add(new RolePermission(roleId, Resource.DASHBOARD, Operation.ALL));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.DELETE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.ASSET, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.ASSET, Operation.WRITE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DASHBOARD, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DASHBOARD, Operation.WRITE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DASHBOARD, Operation.DELETE));
    }

    @Test
    public void testLegacyAuthorityFallbackForTenantAdmin() {
        User user = createUser(Authority.TENANT_ADMIN, null);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.ASSET, Operation.DELETE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DASHBOARD, Operation.ALL));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.TENANT, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.TENANT, Operation.WRITE));
    }

    @Test
    public void testLegacyAuthorityFallbackForCustomerUser() {
        User user = createUser(Authority.CUSTOMER_USER, null);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.DELETE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.ASSET, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.ASSET, Operation.WRITE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DASHBOARD, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.USER, Operation.READ));
    }

    @Test
    public void testLegacyAuthorityFallbackWithNullAuthority() {
        User user = createUser(null, null);
        
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.ASSET, Operation.WRITE));
    }

    @Test
    public void testUserWithNoRoleAndNoAuthority() {
        User user = createUser(null, null);
        
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
    }

    @Test
    public void testUserWithEmptyPermissions() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(Collections.emptySet());
        
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.ASSET, Operation.WRITE));
    }

    @Test
    public void testUserWithNullPermissions() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(null);
        
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
    }

    @Test
    public void testHasAnyPermissionWithSysAdmin() {
        User sysAdmin = createUser(Authority.SYS_ADMIN, null);
        
        Assert.assertTrue(permissionChecker.hasAnyPermission(sysAdmin, Resource.DEVICE));
        Assert.assertTrue(permissionChecker.hasAnyPermission(sysAdmin, Resource.ALL));
    }

    @Test
    public void testHasAnyPermissionWithPermissions() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(roleId, Resource.ASSET, Operation.WRITE));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertTrue(permissionChecker.hasAnyPermission(user, Resource.DEVICE));
        Assert.assertTrue(permissionChecker.hasAnyPermission(user, Resource.ASSET));
        Assert.assertFalse(permissionChecker.hasAnyPermission(user, Resource.DASHBOARD));
    }

    @Test
    public void testHasAnyPermissionWithNoPermissions() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(Collections.emptySet());
        
        Assert.assertFalse(permissionChecker.hasAnyPermission(user, Resource.DEVICE));
    }

    @Test
    public void testHasAnyPermissionWithNullUser() {
        Assert.assertFalse(permissionChecker.hasAnyPermission(null, Resource.DEVICE));
    }

    @Test
    public void testHasAllPermissionsWithSysAdmin() {
        User sysAdmin = createUser(Authority.SYS_ADMIN, null);
        
        Assert.assertTrue(permissionChecker.hasAllPermissions(sysAdmin, Resource.DEVICE));
        Assert.assertTrue(permissionChecker.hasAllPermissions(sysAdmin, Resource.ALL));
    }

    @Test
    public void testHasAllPermissionsWithAllOperation() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.ALL));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertTrue(permissionChecker.hasAllPermissions(user, Resource.DEVICE));
        Assert.assertFalse(permissionChecker.hasAllPermissions(user, Resource.ASSET));
    }

    @Test
    public void testHasAllPermissionsWithSpecificOperation() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.WRITE));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertFalse(permissionChecker.hasAllPermissions(user, Resource.DEVICE));
    }

    @Test
    public void testHasAllPermissionsWithNoRole() {
        User user = createUser(Authority.TENANT_ADMIN, null);
        
        Assert.assertFalse(permissionChecker.hasAllPermissions(user, Resource.DEVICE));
    }

    @Test
    public void testHasAllPermissionsWithNullUser() {
        Assert.assertFalse(permissionChecker.hasAllPermissions(null, Resource.DEVICE));
    }

    @Test
    public void testRpcCallPermission() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.RPC_CALL));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.RPC_CALL));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
    }

    @Test
    public void testLegacyCustomerUserRpcCall() {
        User user = createUser(Authority.CUSTOMER_USER, null);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.RPC_CALL));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.ASSET, Operation.RPC_CALL));
    }

    @Test
    public void testComplexPermissionScenario() {
        User user = createUser(Authority.TENANT_ADMIN, roleId);
        
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(roleId, Resource.DEVICE, Operation.WRITE));
        permissions.add(new RolePermission(roleId, Resource.ASSET, Operation.ALL));
        permissions.add(new RolePermission(roleId, Resource.ALL, Operation.READ));
        
        when(roleService.getRolePermissions(any(RoleId.class))).thenReturn(permissions);
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DEVICE, Operation.DELETE));
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.ASSET, Operation.READ));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.ASSET, Operation.WRITE));
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.ASSET, Operation.DELETE));
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.DASHBOARD, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.DASHBOARD, Operation.WRITE));
        
        Assert.assertTrue(permissionChecker.hasPermission(user, Resource.RULE_CHAIN, Operation.READ));
        Assert.assertFalse(permissionChecker.hasPermission(user, Resource.RULE_CHAIN, Operation.DELETE));
    }

    private User createUser(Authority authority, RoleId roleId) {
        User user = new User();
        user.setId(new UserId(UUID.randomUUID()));
        user.setTenantId(tenantId);
        user.setEmail("test@example.com");
        user.setAuthority(authority);
        user.setRoleId(roleId);
        return user;
    }
}
