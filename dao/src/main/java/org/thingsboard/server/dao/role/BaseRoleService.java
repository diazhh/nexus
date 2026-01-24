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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("RoleDaoService")
@Slf4j
public class BaseRoleService extends AbstractCachedEntityService<RoleCacheKey, Role, RoleCacheEvictEvent> implements RoleService {

    public static final String INCORRECT_ROLE_ID = "Incorrect roleId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String ROLE_NAME_REQUIRED = "Role name is required";
    public static final String SYSTEM_ROLE_DELETE_ERROR = "System roles cannot be deleted";
    public static final String ROLE_IN_USE_ERROR = "Role is assigned to %d users and cannot be deleted";
    public static final String ROLE_NAME_ALREADY_EXISTS = "Role with name '%s' already exists";

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private RolePermissionDao rolePermissionDao;

    @Autowired
    private DataValidator<Role> roleValidator;

    @Autowired
    private EntityCountService countService;

    @Autowired
    private JpaExecutorService executor;

    @TransactionalEventListener(classes = RoleCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(RoleCacheEvictEvent event) {
        cache.evict(new RoleCacheKey(event.tenantId(), event.roleId()));
    }

    @Override
    public Role findRoleById(TenantId tenantId, RoleId roleId) {
        log.trace("Executing findRoleById [{}]", roleId);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        return cache.getAndPutInTransaction(new RoleCacheKey(tenantId, roleId),
                () -> roleDao.findById(tenantId, roleId.getId()), true);
    }

    @Override
    public ListenableFuture<Role> findRoleByIdAsync(TenantId tenantId, RoleId roleId) {
        log.trace("Executing findRoleByIdAsync [{}]", roleId);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        return executor.submit(() -> findRoleById(tenantId, roleId));
    }

    @Override
    public Role findRoleByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findRoleByTenantIdAndName [{}] [{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(name, n -> ROLE_NAME_REQUIRED);
        Optional<Role> roleOpt = roleDao.findRoleByTenantIdAndName(tenantId.getId(), name);
        return roleOpt.orElse(null);
    }

    @Override
    @Transactional
    public Role saveRole(Role role) {
        return saveRole(role, true);
    }

    @Override
    @Transactional
    public Role saveRole(Role role, boolean doValidate) {
        log.trace("Executing saveRole [{}]", role);
        
        if (doValidate) {
            roleValidator.validate(role, r -> r.getTenantId());
            
            if (role.getId() == null) {
                Role existingRole = findRoleByTenantIdAndName(role.getTenantId(), role.getName());
                if (existingRole != null) {
                    throw new DataValidationException(String.format(ROLE_NAME_ALREADY_EXISTS, role.getName()));
                }
            } else {
                Role existingRole = findRoleByTenantIdAndName(role.getTenantId(), role.getName());
                if (existingRole != null && !existingRole.getId().equals(role.getId())) {
                    throw new DataValidationException(String.format(ROLE_NAME_ALREADY_EXISTS, role.getName()));
                }
            }
        }

        Role savedRole = roleDao.save(role.getTenantId(), role);
        
        publishEvictEvent(new RoleCacheEvictEvent(savedRole.getTenantId(), savedRole.getId()));
        
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(savedRole.getTenantId())
                .entityId(savedRole.getId())
                .entity(savedRole)
                .created(role.getId() == null)
                .build());
        
        countService.publishCountEntityEvictEvent(savedRole.getTenantId(), EntityType.ROLE);
        
        return savedRole;
    }

    @Override
    @Transactional
    public void deleteRole(TenantId tenantId, RoleId roleId) {
        log.trace("Executing deleteRole [{}]", roleId);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        
        Role role = findRoleById(tenantId, roleId);
        if (role == null) {
            throw new DataValidationException(INCORRECT_ROLE_ID + roleId);
        }
        
        if (role.isSystemRole()) {
            throw new DataValidationException(SYSTEM_ROLE_DELETE_ERROR);
        }
        
        long usersCount = countUsersByRoleId(roleId);
        if (usersCount > 0) {
            throw new DataValidationException(String.format(ROLE_IN_USE_ERROR, usersCount));
        }
        
        rolePermissionDao.deleteByRoleId(roleId.getId());
        
        roleDao.removeById(tenantId, roleId.getId());
        
        publishEvictEvent(new RoleCacheEvictEvent(tenantId, roleId));
        
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(roleId)
                .build());
        
        countService.publishCountEntityEvictEvent(tenantId, EntityType.ROLE);
    }

    @Override
    public PageData<Role> findRolesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findRolesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return roleDao.findRolesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<Role> findSystemRoles() {
        log.trace("Executing findSystemRoles");
        return roleDao.findSystemRoles();
    }

    @Override
    public List<Role> findRolesByTenantIdAndIds(TenantId tenantId, List<RoleId> roleIds) {
        log.trace("Executing findRolesByTenantIdAndIds, tenantId [{}], roleIds [{}]", tenantId, roleIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return roleDao.findRolesByTenantIdAndIds(tenantId.getId(), 
                roleIds.stream().map(RoleId::getId).collect(Collectors.toList()));
    }

    @Override
    @Cacheable(cacheNames = "rolePermissions", key = "#roleId")
    public Set<RolePermission> getRolePermissions(RoleId roleId) {
        log.trace("Executing getRolePermissions [{}]", roleId);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        List<RolePermission> permissions = rolePermissionDao.findByRoleId(roleId.getId());
        return new HashSet<>(permissions);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "rolePermissions", key = "#roleId")
    public void updateRolePermissions(RoleId roleId, Set<RolePermission> permissions) {
        log.trace("Executing updateRolePermissions [{}]", roleId);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        
        rolePermissionDao.deleteByRoleId(roleId.getId());
        
        if (permissions != null && !permissions.isEmpty()) {
            List<RolePermission> permissionList = permissions.stream()
                    .peek(p -> p.setRoleId(roleId))
                    .collect(Collectors.toList());
            rolePermissionDao.save(roleId.getId(), permissionList);
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "rolePermissions", key = "#roleId")
    public void addRolePermissions(RoleId roleId, Set<RolePermission> permissions) {
        log.trace("Executing addRolePermissions [{}]", roleId);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        
        if (permissions != null && !permissions.isEmpty()) {
            List<RolePermission> permissionList = permissions.stream()
                    .peek(p -> p.setRoleId(roleId))
                    .collect(Collectors.toList());
            rolePermissionDao.save(roleId.getId(), permissionList);
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "rolePermissions", key = "#roleId")
    public void removeRolePermissions(RoleId roleId, Set<RolePermission> permissions) {
        log.trace("Executing removeRolePermissions [{}]", roleId);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        
        if (permissions != null && !permissions.isEmpty()) {
            rolePermissionDao.deleteByRoleIdAndPermissions(roleId.getId(), 
                    permissions.stream().collect(Collectors.toList()));
        }
    }

    @Override
    @Transactional
    public void createDefaultTenantRoles(TenantId tenantId) {
        log.trace("Executing createDefaultTenantRoles [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        
        Role tenantAdmin = new Role();
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setName("Tenant Administrator");
        tenantAdmin.setDescription("Full access to tenant resources");
        tenantAdmin.setSystem(false);
        Role savedTenantAdmin = saveRole(tenantAdmin, false);
        
        Set<RolePermission> adminPermissions = new HashSet<>();
        adminPermissions.add(new RolePermission(savedTenantAdmin.getId(), Resource.ALL, Operation.ALL));
        updateRolePermissions(savedTenantAdmin.getId(), adminPermissions);
        
        Role customerUser = new Role();
        customerUser.setTenantId(tenantId);
        customerUser.setName("Customer User");
        customerUser.setDescription("Limited access to assigned resources");
        customerUser.setSystem(false);
        Role savedCustomerUser = saveRole(customerUser, false);
        
        Set<RolePermission> customerPermissions = new HashSet<>();
        customerPermissions.add(new RolePermission(savedCustomerUser.getId(), Resource.DEVICE, Operation.READ));
        customerPermissions.add(new RolePermission(savedCustomerUser.getId(), Resource.ASSET, Operation.READ));
        customerPermissions.add(new RolePermission(savedCustomerUser.getId(), Resource.DASHBOARD, Operation.READ));
        updateRolePermissions(savedCustomerUser.getId(), customerPermissions);
    }

    @Override
    public long countUsersByRoleId(RoleId roleId) {
        log.trace("Executing countUsersByRoleId [{}]", roleId);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        return roleDao.countUsersByRoleId(roleId.getId());
    }

    @Override
    @Transactional
    public void deleteRolesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteRolesByTenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        
        tenantRolesRemover.removeEntities(tenantId, tenantId);
    }

    private final PaginatedRemover<TenantId, Role> tenantRolesRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<Role> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return roleDao.findRolesByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Role entity) {
            deleteRole(tenantId, entity.getId());
        }
    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findRoleById(tenantId, new RoleId(entityId.getId())));
    }

    @Override
    public com.google.common.util.concurrent.FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return com.google.common.util.concurrent.FluentFuture.from(findRoleByIdAsync(tenantId, new RoleId(entityId.getId())))
                .transform(Optional::ofNullable, com.google.common.util.concurrent.MoreExecutors.directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return roleDao.countByTenantId(tenantId);
    }

    @Override
    public boolean hasPermission(RoleId roleId, String resource, String operation) {
        log.trace("Executing hasPermission [{}] [{}] [{}]", roleId, resource, operation);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        
        Set<RolePermission> permissions = getRolePermissions(roleId);
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        
        try {
            Resource resourceEnum = Resource.valueOf(resource);
            Operation operationEnum = Operation.valueOf(operation);
            
            for (RolePermission permission : permissions) {
                if ((permission.getResource() == Resource.ALL || permission.getResource() == resourceEnum) &&
                    (permission.getOperation() == Operation.ALL || permission.getOperation() == operationEnum)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid resource or operation: {} {}", resource, operation);
            return false;
        }
        
        return false;
    }
}
