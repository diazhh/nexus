# Guía de Implementación - Sistema de Roles y Permisos

## Inicio Rápido

### Prerrequisitos

**Herramientas Requeridas:**
- JDK 11+
- Maven 3.6+
- Node.js 14+
- PostgreSQL 12+
- Git
- IDE (IntelliJ IDEA recomendado para backend, VS Code para frontend)

**Conocimientos Técnicos:**
- Java, Spring Boot, Spring Security
- TypeScript, Angular 14+
- SQL, PostgreSQL
- REST APIs
- Testing (JUnit, Jasmine)

### Setup del Ambiente

```bash
# Clonar repositorio
git clone https://github.com/your-org/thingsboard.git
cd thingsboard

# Checkout rama de desarrollo
git checkout -b feature/roles-and-permissions develop

# Configurar base de datos
createdb thingsboard_dev
psql thingsboard_dev < dao/src/main/resources/sql/schema-entities.sql

# Compilar backend
mvn clean install -DskipTests

# Instalar dependencias frontend
cd ui-ngx
yarn install

# Iniciar backend
cd ../application
mvn spring-boot:run

# Iniciar frontend (en otra terminal)
cd ../ui-ngx
yarn start
```

---

## Guía de Implementación por Sprint

### Sprint 1-2: Base de Datos y Modelos

#### Paso 1: Crear Esquemas SQL

**Archivo:** `dao/src/main/resources/sql/schema-roles.sql`

```sql
-- Crear tabla role
CREATE TABLE IF NOT EXISTS role (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    tenant_id UUID,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1024),
    is_system BOOLEAN DEFAULT FALSE,
    additional_info VARCHAR,
    version BIGINT DEFAULT 1,
    CONSTRAINT role_pkey PRIMARY KEY (id),
    CONSTRAINT role_tenant_name_unq UNIQUE (tenant_id, name),
    CONSTRAINT fk_role_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

-- Crear tabla role_permission
CREATE TABLE IF NOT EXISTS role_permission (
    id UUID NOT NULL,
    role_id UUID NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    CONSTRAINT role_permission_pkey PRIMARY KEY (id),
    CONSTRAINT role_permission_role_resource_op_unq 
        UNIQUE (role_id, resource_type, operation),
    CONSTRAINT fk_role_permission_role 
        FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

-- Índices
CREATE INDEX idx_role_tenant_id ON role(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_role_permission_role_id ON role_permission(role_id);
CREATE INDEX idx_role_permission_composite ON role_permission(role_id, resource_type, operation);
```

**Ejecutar:**
```bash
psql thingsboard_dev < dao/src/main/resources/sql/schema-roles.sql
```

#### Paso 2: Crear Entidades Java

**Archivo:** `common/data/src/main/java/org/thingsboard/server/common/data/security/Role.java`

```java
package org.thingsboard.server.common.data.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseDataWithAdditionalInfo<RoleId> implements HasTenantId {
    
    private static final long serialVersionUID = 1L;
    
    private TenantId tenantId;
    
    @NoXss
    @Length(fieldName = "name", min = 1, max = 255)
    private String name;
    
    @NoXss
    @Length(fieldName = "description", max = 1024)
    private String description;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean isSystem;
    
    private Long version;
    
    public Role() {
        super();
    }
    
    public Role(RoleId id) {
        super(id);
    }
    
    public Role(Role role) {
        super(role);
        this.tenantId = role.getTenantId();
        this.name = role.getName();
        this.description = role.getDescription();
        this.isSystem = role.isSystem();
        this.version = role.getVersion();
    }
    
    public boolean isSystemRole() {
        return isSystem;
    }
}
```

**Archivo:** `common/data/src/main/java/org/thingsboard/server/common/data/security/RolePermission.java`

```java
package org.thingsboard.server.common.data.security;

import lombok.Data;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RolePermissionId;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.io.Serializable;

@Data
public class RolePermission implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private RolePermissionId id;
    private RoleId roleId;
    private Resource resource;
    private Operation operation;
    
    public RolePermission() {
    }
    
    public RolePermission(RoleId roleId, Resource resource, Operation operation) {
        this.roleId = roleId;
        this.resource = resource;
        this.operation = operation;
    }
    
    public boolean allowsOperation(Operation op) {
        return this.operation == Operation.ALL || this.operation == op;
    }
}
```

**Archivo:** `common/data/src/main/java/org/thingsboard/server/common/data/id/RoleId.java`

```java
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class RoleId extends UUIDBased implements EntityId {
    
    private static final long serialVersionUID = 1L;
    
    @JsonCreator
    public RoleId(@JsonProperty("id") UUID id) {
        super(id);
    }
    
    public static RoleId fromString(String roleId) {
        return new RoleId(UUID.fromString(roleId));
    }
    
    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }
}
```

#### Paso 3: Crear DAOs

**Archivo:** `dao/src/main/java/org/thingsboard/server/dao/role/RoleDao.java`

```java
package org.thingsboard.server.dao.role;

import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.UUID;

public interface RoleDao extends Dao<Role>, TenantEntityDao<Role> {
    
    Role save(TenantId tenantId, Role role);
    
    PageData<Role> findByTenantId(UUID tenantId, PageLink pageLink);
    
    Role findByTenantIdAndName(TenantId tenantId, String name);
    
    List<Role> findSystemRoles();
    
    void deleteByTenantId(TenantId tenantId);
}
```

**Implementación JPA:**

**Archivo:** `dao/src/main/java/org/thingsboard/server/dao/sql/role/RoleEntity.java`

```java
package org.thingsboard.server.dao.sql.role;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.*;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.ROLE_TABLE_NAME)
public class RoleEntity extends BaseSqlEntity<Role> {
    
    @Column(name = ModelConstants.ROLE_TENANT_ID_PROPERTY)
    private UUID tenantId;
    
    @Column(name = ModelConstants.ROLE_NAME_PROPERTY)
    private String name;
    
    @Column(name = ModelConstants.ROLE_DESCRIPTION_PROPERTY)
    private String description;
    
    @Column(name = ModelConstants.ROLE_IS_SYSTEM_PROPERTY)
    private boolean isSystem;
    
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private String additionalInfo;
    
    @Version
    @Column(name = ModelConstants.VERSION_PROPERTY)
    private Long version;
    
    public RoleEntity() {
        super();
    }
    
    public RoleEntity(Role role) {
        if (role.getId() != null) {
            this.setUuid(role.getId().getId());
        }
        this.setCreatedTime(role.getCreatedTime());
        if (role.getTenantId() != null) {
            this.tenantId = role.getTenantId().getId();
        }
        this.name = role.getName();
        this.description = role.getDescription();
        this.isSystem = role.isSystem();
        this.additionalInfo = role.getAdditionalInfoString();
        this.version = role.getVersion();
    }
    
    @Override
    public Role toData() {
        Role role = new Role(new RoleId(this.getUuid()));
        role.setCreatedTime(this.getCreatedTime());
        if (this.tenantId != null) {
            role.setTenantId(new TenantId(this.tenantId));
        }
        role.setName(this.name);
        role.setDescription(this.description);
        role.setSystem(this.isSystem);
        role.setAdditionalInfo(this.getAdditionalInfoData());
        role.setVersion(this.version);
        return role;
    }
}
```

#### Paso 4: Tests Unitarios

**Archivo:** `dao/src/test/java/org/thingsboard/server/dao/role/RoleDaoTest.java`

```java
package org.thingsboard.server.dao.role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RoleDaoTest {
    
    @Autowired
    private RoleDao roleDao;
    
    private TenantId tenantId;
    
    @BeforeEach
    void setUp() {
        tenantId = new TenantId(UUID.randomUUID());
    }
    
    @AfterEach
    void tearDown() {
        roleDao.deleteByTenantId(tenantId);
    }
    
    @Test
    void testSaveRole() {
        Role role = createTestRole("Test Role");
        Role saved = roleDao.save(tenantId, role);
        
        assertNotNull(saved.getId());
        assertEquals("Test Role", saved.getName());
    }
    
    @Test
    void testFindByTenantId() {
        createTestRole("Role 1");
        createTestRole("Role 2");
        
        PageData<Role> roles = roleDao.findByTenantId(
            tenantId.getId(), 
            new PageLink(10, 0)
        );
        
        assertEquals(2, roles.getData().size());
    }
    
    private Role createTestRole(String name) {
        Role role = new Role();
        role.setName(name);
        role.setTenantId(tenantId);
        role.setDescription("Test description");
        return roleDao.save(tenantId, role);
    }
}
```

**Ejecutar tests:**
```bash
mvn test -Dtest=RoleDaoTest
```

---

### Sprint 3-4: Backend Services

#### Paso 1: Implementar RoleService

**Archivo:** `application/src/main/java/org/thingsboard/server/service/role/RoleService.java`

```java
package org.thingsboard.server.service.role;

import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.common.data.security.RolePermission;

import java.util.List;
import java.util.Set;

public interface RoleService {
    
    Role saveRole(Role role);
    
    Role findRoleById(TenantId tenantId, RoleId roleId);
    
    PageData<Role> findRolesByTenantId(TenantId tenantId, PageLink pageLink);
    
    void deleteRole(TenantId tenantId, RoleId roleId);
    
    void updateRolePermissions(RoleId roleId, Set<RolePermission> permissions);
    
    Set<RolePermission> getRolePermissions(RoleId roleId);
    
    List<Role> getSystemRoles();
    
    void createDefaultTenantRoles(TenantId tenantId);
}
```

**Implementación:**

**Archivo:** `application/src/main/java/org/thingsboard/server/service/role/DefaultRoleService.java`

```java
package org.thingsboard.server.service.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.role.RolePermissionDao;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultRoleService implements RoleService {
    
    private final RoleDao roleDao;
    private final RolePermissionDao rolePermissionDao;
    private final UserService userService;
    
    @Override
    @Transactional
    public Role saveRole(Role role) {
        log.trace("Saving role {}", role);
        
        validateRole(role);
        
        if (role.getId() != null && role.isSystemRole()) {
            throw new DataValidationException("Cannot modify system role");
        }
        
        return roleDao.save(role.getTenantId(), role);
    }
    
    @Override
    public Role findRoleById(TenantId tenantId, RoleId roleId) {
        log.trace("Finding role by id {}", roleId);
        return roleDao.findById(tenantId, roleId.getId());
    }
    
    @Override
    public PageData<Role> findRolesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Finding roles by tenantId {}", tenantId);
        return roleDao.findByTenantId(tenantId.getId(), pageLink);
    }
    
    @Override
    @Transactional
    public void deleteRole(TenantId tenantId, RoleId roleId) {
        log.trace("Deleting role {}", roleId);
        
        Role role = findRoleById(tenantId, roleId);
        if (role == null) {
            throw new DataValidationException("Role not found");
        }
        
        if (role.isSystemRole()) {
            throw new DataValidationException("Cannot delete system role");
        }
        
        long userCount = userService.countUsersByRoleId(roleId);
        if (userCount > 0) {
            throw new DataValidationException(
                "Cannot delete role: " + userCount + " users are still assigned"
            );
        }
        
        roleDao.removeById(tenantId, roleId.getId());
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "rolePermissions", key = "#roleId")
    public void updateRolePermissions(RoleId roleId, Set<RolePermission> permissions) {
        log.trace("Updating permissions for role {}", roleId);
        
        rolePermissionDao.deleteByRoleId(roleId);
        
        if (!permissions.isEmpty()) {
            List<RolePermission> permissionList = new ArrayList<>(permissions);
            permissionList.forEach(p -> p.setRoleId(roleId));
            rolePermissionDao.saveAll(permissionList);
        }
    }
    
    @Override
    @Cacheable(value = "rolePermissions", key = "#roleId")
    public Set<RolePermission> getRolePermissions(RoleId roleId) {
        log.trace("Getting permissions for role {}", roleId);
        List<RolePermission> permissions = rolePermissionDao.findByRoleId(roleId);
        return new HashSet<>(permissions);
    }
    
    @Override
    public List<Role> getSystemRoles() {
        return roleDao.findSystemRoles();
    }
    
    @Override
    @Transactional
    public void createDefaultTenantRoles(TenantId tenantId) {
        log.trace("Creating default roles for tenant {}", tenantId);
        
        createTenantAdminRole(tenantId);
        createCustomerUserRole(tenantId);
    }
    
    private void createTenantAdminRole(TenantId tenantId) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName("Tenant Administrator");
        role.setDescription("Full access to tenant resources");
        role.setSystem(true);
        
        Role saved = roleDao.save(tenantId, role);
        
        // Add all permissions
        Set<RolePermission> permissions = new HashSet<>();
        for (Resource resource : Resource.values()) {
            permissions.add(new RolePermission(saved.getId(), resource, Operation.ALL));
        }
        updateRolePermissions(saved.getId(), permissions);
    }
    
    private void createCustomerUserRole(TenantId tenantId) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName("Customer User");
        role.setDescription("Limited access for customer users");
        role.setSystem(true);
        
        Role saved = roleDao.save(tenantId, role);
        
        // Add limited permissions
        Set<RolePermission> permissions = new HashSet<>();
        permissions.add(new RolePermission(saved.getId(), Resource.DEVICE, Operation.READ));
        permissions.add(new RolePermission(saved.getId(), Resource.ASSET, Operation.READ));
        permissions.add(new RolePermission(saved.getId(), Resource.DASHBOARD, Operation.READ));
        permissions.add(new RolePermission(saved.getId(), Resource.ALARM, Operation.READ));
        permissions.add(new RolePermission(saved.getId(), Resource.ALARM, Operation.WRITE));
        
        updateRolePermissions(saved.getId(), permissions);
    }
    
    private void validateRole(Role role) {
        if (role.getName() == null || role.getName().trim().isEmpty()) {
            throw new DataValidationException("Role name is required");
        }
        
        if (role.getTenantId() == null) {
            throw new DataValidationException("Tenant ID is required");
        }
        
        // Check for duplicate names
        Role existing = roleDao.findByTenantIdAndName(role.getTenantId(), role.getName());
        if (existing != null && !existing.getId().equals(role.getId())) {
            throw new DataValidationException("Role with name '" + role.getName() + "' already exists");
        }
    }
}
```

#### Paso 2: Cache Configuration

**Archivo:** `application/src/main/java/org/thingsboard/server/config/CacheConfig.java`

Agregar configuración de cache:

```java
@Bean
public CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();
    cacheManager.setCaches(Arrays.asList(
        new ConcurrentMapCache("rolePermissions"),
        // ... otros caches
    ));
    return cacheManager;
}
```

---

### Sprint 5-6: REST APIs

#### Implementar RoleController

**Archivo:** `application/src/main/java/org/thingsboard/server/controller/RoleController.java`

```java
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.role.RoleService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api/role")
@RequiredArgsConstructor
@Tag(name = "Role Management")
public class RoleController extends BaseController {
    
    private final RoleService roleService;
    
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/{roleId}")
    public Role getRoleById(@PathVariable String roleId) throws ThingsboardException {
        checkParameter("roleId", roleId);
        RoleId id = new RoleId(toUUID(roleId));
        return checkNotNull(roleService.findRoleById(getCurrentUser().getTenantId(), id));
    }
    
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping
    public PageData<Role> getRoles(
            @Parameter(description = "Page size") @RequestParam int pageSize,
            @Parameter(description = "Page number") @RequestParam int page,
            @Parameter(description = "Text search") @RequestParam(required = false) String textSearch,
            @Parameter(description = "Sort property") @RequestParam(required = false) String sortProperty,
            @Parameter(description = "Sort order") @RequestParam(required = false) String sortOrder
    ) {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return roleService.findRolesByTenantId(getCurrentUser().getTenantId(), pageLink);
    }
    
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping
    public Role saveRole(@RequestBody Role role) throws ThingsboardException {
        role.setTenantId(getCurrentUser().getTenantId());
        return roleService.saveRole(role);
    }
    
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping("/{roleId}")
    public void deleteRole(@PathVariable String roleId) throws ThingsboardException {
        checkParameter("roleId", roleId);
        RoleId id = new RoleId(toUUID(roleId));
        roleService.deleteRole(getCurrentUser().getTenantId(), id);
    }
    
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping("/{roleId}/permissions")
    public void updatePermissions(
            @PathVariable String roleId,
            @RequestBody Set<RolePermission> permissions
    ) throws ThingsboardException {
        checkParameter("roleId", roleId);
        RoleId id = new RoleId(toUUID(roleId));
        roleService.updateRolePermissions(id, permissions);
    }
    
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/{roleId}/permissions")
    public Set<RolePermission> getPermissions(@PathVariable String roleId) throws ThingsboardException {
        checkParameter("roleId", roleId);
        RoleId id = new RoleId(toUUID(roleId));
        return roleService.getRolePermissions(id);
    }
    
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/resources")
    public List<Resource> getAvailableResources() {
        return Arrays.asList(Resource.values());
    }
    
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/operations")
    public List<Operation> getAvailableOperations() {
        return Arrays.asList(Operation.values());
    }
}
```

**Probar API:**
```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant@thingsboard.org","password":"tenant"}' \
  | jq -r '.token')

# Crear rol
curl -X POST http://localhost:8080/api/role \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Role","description":"Testing"}'
```

---

## Checklist de Implementación

### Backend
- [ ] Esquemas SQL creados y ejecutados
- [ ] Entidades Java implementadas (Role, RolePermission)
- [ ] IDs creados (RoleId, RolePermissionId)
- [ ] DAOs implementados (RoleDao, RolePermissionDao)
- [ ] Services implementados (RoleService)
- [ ] Controllers implementados (RoleController)
- [ ] Tests unitarios >= 80% coverage
- [ ] Tests de integración funcionando
- [ ] Cache configurado
- [ ] Documentación Swagger actualizada

### Frontend
- [ ] Modelos TypeScript creados
- [ ] Servicios Angular implementados
- [ ] Componentes UI creados
- [ ] Routing configurado
- [ ] Traducción i18n agregada
- [ ] Tests unitarios >= 75% coverage
- [ ] Tests E2E de flujos principales

### Base de Datos
- [ ] Migraciones creadas y testeadas
- [ ] Índices agregados
- [ ] Triggers implementados
- [ ] Scripts de rollback creados

---

**Versión:** 1.0  
**Última Actualización:** 23 Enero 2026
