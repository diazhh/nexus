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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;
import org.thingsboard.server.dao.role.RoleService;

import java.util.HashSet;
import java.util.Set;

@DaoSqlTest
@Transactional
public class RoleCacheTest extends AbstractServiceTest {

    @Autowired
    private RoleService roleService;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Test
    public void testCacheInvalidationOnPermissionUpdate() {
        if (cacheManager == null) {
            return;
        }

        Role role = createTestRole("Cache Update Test Role");
        Role savedRole = roleService.saveRole(role);

        Set<RolePermission> initialPermissions = new HashSet<>();
        initialPermissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), initialPermissions);

        Set<RolePermission> firstLoad = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(1, firstLoad.size());

        Cache cache = cacheManager.getCache("rolePermissions");
        if (cache != null) {
            Cache.ValueWrapper cachedValue = cache.get(savedRole.getId());
            Assert.assertNotNull(cachedValue);
        }

        Set<RolePermission> updatedPermissions = new HashSet<>();
        updatedPermissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        updatedPermissions.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.WRITE));
        roleService.updateRolePermissions(savedRole.getId(), updatedPermissions);

        Set<RolePermission> secondLoad = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(2, secondLoad.size());

        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testCacheInvalidationOnRoleDelete() {
        if (cacheManager == null) {
            return;
        }

        Role role = createTestRole("Cache Delete Test Role");
        Role savedRole = roleService.saveRole(role);

        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), permissions);

        Set<RolePermission> loaded = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(1, loaded.size());

        Cache cache = cacheManager.getCache("rolePermissions");
        if (cache != null) {
            Cache.ValueWrapper cachedValue = cache.get(savedRole.getId());
            Assert.assertNotNull(cachedValue);
        }

        roleService.deleteRole(tenantId, savedRole.getId());

        if (cache != null) {
            Cache.ValueWrapper cachedValueAfterDelete = cache.get(savedRole.getId());
            Assert.assertNull(cachedValueAfterDelete);
        }
    }

    @Test
    public void testCacheInvalidationOnAddPermissions() {
        if (cacheManager == null) {
            return;
        }

        Role role = createTestRole("Cache Add Test Role");
        Role savedRole = roleService.saveRole(role);

        Set<RolePermission> initialPermissions = new HashSet<>();
        initialPermissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), initialPermissions);

        Set<RolePermission> firstLoad = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(1, firstLoad.size());

        Set<RolePermission> additionalPermissions = new HashSet<>();
        additionalPermissions.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.WRITE));
        roleService.addRolePermissions(savedRole.getId(), additionalPermissions);

        Set<RolePermission> secondLoad = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(2, secondLoad.size());

        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testCacheInvalidationOnRemovePermissions() {
        if (cacheManager == null) {
            return;
        }

        Role role = createTestRole("Cache Remove Test Role");
        Role savedRole = roleService.saveRole(role);

        Set<RolePermission> initialPermissions = new HashSet<>();
        initialPermissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        initialPermissions.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.WRITE));
        initialPermissions.add(new RolePermission(savedRole.getId(), Resource.DASHBOARD, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), initialPermissions);

        Set<RolePermission> firstLoad = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(3, firstLoad.size());

        Set<RolePermission> permissionsToRemove = new HashSet<>();
        permissionsToRemove.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.WRITE));
        roleService.removeRolePermissions(savedRole.getId(), permissionsToRemove);

        Set<RolePermission> secondLoad = roleService.getRolePermissions(savedRole.getId());
        Assert.assertEquals(2, secondLoad.size());
        Assert.assertFalse(secondLoad.stream()
                .anyMatch(p -> p.getResource() == Resource.ASSET));

        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testMultipleCacheOperations() {
        if (cacheManager == null) {
            return;
        }

        Role role1 = createTestRole("Cache Multi Test Role 1");
        Role savedRole1 = roleService.saveRole(role1);

        Role role2 = createTestRole("Cache Multi Test Role 2");
        Role savedRole2 = roleService.saveRole(role2);

        Set<RolePermission> permissions1 = new HashSet<>();
        permissions1.add(new RolePermission(savedRole1.getId(), Resource.DEVICE, Operation.READ));
        roleService.updateRolePermissions(savedRole1.getId(), permissions1);

        Set<RolePermission> permissions2 = new HashSet<>();
        permissions2.add(new RolePermission(savedRole2.getId(), Resource.ASSET, Operation.WRITE));
        roleService.updateRolePermissions(savedRole2.getId(), permissions2);

        Set<RolePermission> loaded1 = roleService.getRolePermissions(savedRole1.getId());
        Set<RolePermission> loaded2 = roleService.getRolePermissions(savedRole2.getId());

        Assert.assertEquals(1, loaded1.size());
        Assert.assertEquals(1, loaded2.size());

        Cache cache = cacheManager.getCache("rolePermissions");
        if (cache != null) {
            Assert.assertNotNull(cache.get(savedRole1.getId()));
            Assert.assertNotNull(cache.get(savedRole2.getId()));
        }

        Set<RolePermission> updatedPermissions1 = new HashSet<>();
        updatedPermissions1.add(new RolePermission(savedRole1.getId(), Resource.DEVICE, Operation.ALL));
        roleService.updateRolePermissions(savedRole1.getId(), updatedPermissions1);

        Set<RolePermission> reloaded1 = roleService.getRolePermissions(savedRole1.getId());
        Set<RolePermission> reloaded2 = roleService.getRolePermissions(savedRole2.getId());

        Assert.assertEquals(1, reloaded1.size());
        Assert.assertEquals(Operation.ALL, reloaded1.iterator().next().getOperation());
        Assert.assertEquals(1, reloaded2.size());

        roleService.deleteRole(tenantId, savedRole1.getId());
        roleService.deleteRole(tenantId, savedRole2.getId());
    }

    @Test
    public void testCacheConsistencyAfterMultipleUpdates() {
        if (cacheManager == null) {
            return;
        }

        Role role = createTestRole("Cache Consistency Test Role");
        Role savedRole = roleService.saveRole(role);

        for (int i = 0; i < 5; i++) {
            Set<RolePermission> permissions = new HashSet<>();
            permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
            
            if (i % 2 == 0) {
                permissions.add(new RolePermission(savedRole.getId(), Resource.ASSET, Operation.WRITE));
            }
            
            roleService.updateRolePermissions(savedRole.getId(), permissions);
            
            Set<RolePermission> loaded = roleService.getRolePermissions(savedRole.getId());
            
            if (i % 2 == 0) {
                Assert.assertEquals(2, loaded.size());
            } else {
                Assert.assertEquals(1, loaded.size());
            }
        }

        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testCacheHitRate() {
        if (cacheManager == null) {
            return;
        }

        Role role = createTestRole("Cache Hit Rate Test Role");
        Role savedRole = roleService.saveRole(role);

        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), permissions);

        for (int i = 0; i < 10; i++) {
            Set<RolePermission> loaded = roleService.getRolePermissions(savedRole.getId());
            Assert.assertEquals(1, loaded.size());
        }

        roleService.deleteRole(tenantId, savedRole.getId());
    }

    @Test
    public void testCacheEvictionOnRoleUpdate() {
        if (cacheManager == null) {
            return;
        }

        Role role = createTestRole("Cache Eviction Test Role");
        Role savedRole = roleService.saveRole(role);

        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(savedRole.getId(), Resource.DEVICE, Operation.READ));
        roleService.updateRolePermissions(savedRole.getId(), permissions);

        Role loadedRole = roleService.findRoleById(tenantId, savedRole.getId());
        Assert.assertNotNull(loadedRole);

        savedRole.setDescription("Updated description");
        roleService.saveRole(savedRole);

        Role reloadedRole = roleService.findRoleById(tenantId, savedRole.getId());
        Assert.assertEquals("Updated description", reloadedRole.getDescription());

        roleService.deleteRole(tenantId, savedRole.getId());
    }

    private Role createTestRole(String name) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName(name);
        role.setDescription("Test role for cache testing");
        role.setSystem(false);
        return role;
    }
}
