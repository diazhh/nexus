# Especificación Técnica - Sistema de Roles y Permisos

## Arquitectura del Sistema

### Visión General

El sistema de roles y permisos se implementa como una extensión del modelo de autorización existente en ThingsBoard CE, agregando capacidades de configuración granular sin romper la retrocompatibilidad.

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (Angular)                      │
├─────────────────────────────────────────────────────────────┤
│  RoleModule  │  UserModule  │  PermissionDirectives        │
└─────────────────────────────────────────────────────────────┘
                              ↓ REST API
┌─────────────────────────────────────────────────────────────┐
│                   Backend (Spring Boot)                      │
├─────────────────────────────────────────────────────────────┤
│  RoleController │ UserController │ @RequirePermission       │
├─────────────────────────────────────────────────────────────┤
│  RoleService    │ UserService    │ PermissionChecker        │
├─────────────────────────────────────────────────────────────┤
│  RoleDao        │ UserDao        │ RolePermissionDao        │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  Base de Datos (PostgreSQL)                  │
├─────────────────────────────────────────────────────────────┤
│  role  │  role_permission  │  tb_user  │  tenant            │
└─────────────────────────────────────────────────────────────┘
```

---

## Componentes Backend

### 1. Modelo de Datos

#### Role Entity
```java
@Entity
@Table(name = "role")
public class RoleEntity {
    @Id
    private UUID id;
    
    @Column(name = "tenant_id")
    private UUID tenantId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_system")
    private boolean isSystem;
    
    @Column(name = "created_time")
    private long createdTime;
    
    @Version
    @Column(name = "version")
    private Long version;
}
```

**Índices:**
- PK: `id`
- UNIQUE: `(tenant_id, name)`
- INDEX: `tenant_id`, `is_system`

#### RolePermission Entity
```java
@Entity
@Table(name = "role_permission")
public class RolePermissionEntity {
    @Id
    private UUID id;
    
    @Column(name = "role_id", nullable = false)
    private UUID roleId;
    
    @Column(name = "resource_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Resource resource;
    
    @Column(name = "operation", nullable = false)
    @Enumerated(EnumType.STRING)
    private Operation operation;
}
```

**Índices:**
- PK: `id`
- UNIQUE: `(role_id, resource_type, operation)`
- INDEX: `role_id`
- COMPOSITE: `(role_id, resource_type, operation)`

#### User Modification
```java
@Entity
@Table(name = "tb_user")
public class UserEntity {
    // Campos existentes...
    
    @Column(name = "authority")
    private String authority; // Ahora nullable
    
    @Column(name = "role_id")
    private UUID roleId; // Nuevo campo
}
```

---

### 2. Data Access Layer

#### RoleDao Interface
```java
public interface RoleDao extends Dao<Role>, TenantEntityDao<Role> {
    Role save(TenantId tenantId, Role role);
    PageData<Role> findByTenantId(UUID tenantId, PageLink pageLink);
    Role findByTenantIdAndName(TenantId tenantId, String name);
    List<Role> findSystemRoles();
    void deleteByTenantId(TenantId tenantId);
}
```

**Queries Principales:**
```sql
-- findByTenantId
SELECT * FROM role 
WHERE tenant_id = ? 
ORDER BY name 
LIMIT ? OFFSET ?;

-- findByTenantIdAndName
SELECT * FROM role 
WHERE tenant_id = ? AND name = ?;

-- findSystemRoles
SELECT * FROM role 
WHERE is_system = true;
```

#### RolePermissionDao Interface
```java
public interface RolePermissionDao extends Dao<RolePermission> {
    List<RolePermission> findByRoleId(RoleId roleId);
    void deleteByRoleId(RoleId roleId);
    void saveAll(List<RolePermission> permissions);
}
```

**Queries Principales:**
```sql
-- findByRoleId
SELECT * FROM role_permission 
WHERE role_id = ?;

-- deleteByRoleId
DELETE FROM role_permission 
WHERE role_id = ?;

-- Check permission
SELECT EXISTS (
    SELECT 1 FROM role_permission 
    WHERE role_id = ? 
    AND resource_type IN (?, 'ALL')
    AND operation IN (?, 'ALL')
);
```

---

### 3. Service Layer

#### RoleService
```java
@Service
public class DefaultRoleService implements RoleService {
    
    private final RoleDao roleDao;
    private final RolePermissionDao permissionDao;
    private final CacheManager cacheManager;
    
    @Transactional
    public Role saveRole(Role role) {
        validateRole(role);
        return roleDao.save(role.getTenantId(), role);
    }
    
    @Transactional
    @CacheEvict(value = "rolePermissions", key = "#roleId")
    public void updateRolePermissions(RoleId roleId, Set<RolePermission> permissions) {
        permissionDao.deleteByRoleId(roleId);
        permissionDao.saveAll(new ArrayList<>(permissions));
    }
    
    @Cacheable(value = "rolePermissions", key = "#roleId")
    public Set<RolePermission> getRolePermissions(RoleId roleId) {
        return new HashSet<>(permissionDao.findByRoleId(roleId));
    }
}
```

**Validaciones:**
- Nombre no vacío
- Nombre único por tenant
- No modificar roles del sistema
- No eliminar roles con usuarios asignados

---

### 4. Permission Checking System

#### RoleBasedPermissionChecker
```java
@Component
public class RoleBasedPermissionChecker {
    
    @Autowired
    private Cache<RoleId, Set<RolePermission>> permissionCache;
    
    public boolean hasPermission(SecurityUser user, Resource resource, Operation operation) {
        // SYS_ADMIN siempre tiene acceso
        if (user.getAuthority() == Authority.SYS_ADMIN) {
            return true;
        }
        
        RoleId roleId = user.getRoleId();
        if (roleId == null) {
            // Fallback a legacy authority
            return checkLegacyPermission(user, resource, operation);
        }
        
        Set<RolePermission> permissions = getPermissionsFromCache(roleId);
        return permissions.stream()
            .anyMatch(p -> matchesPermission(p, resource, operation));
    }
    
    private boolean matchesPermission(RolePermission perm, Resource resource, Operation operation) {
        boolean resourceMatch = perm.getResource() == resource || perm.getResource() == Resource.ALL;
        boolean operationMatch = perm.getOperation() == operation || perm.getOperation() == Operation.ALL;
        return resourceMatch && operationMatch;
    }
}
```

**Cache Strategy:**
- **L1 Cache:** Caffeine (in-memory, local)
- **L2 Cache:** Redis (distributed, opcional)
- **TTL:** 1 hora
- **Invalidación:** Al actualizar permisos del rol

---

### 5. Aspect-Oriented Permission Validation

#### @RequirePermission Annotation
```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    Resource resource();
    Operation[] operations() default {};
}
```

#### PermissionAspect
```java
@Aspect
@Component
public class PermissionAspect {
    
    @Autowired
    private RoleBasedPermissionChecker permissionChecker;
    
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) 
            throws Throwable {
        
        SecurityUser user = SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal();
        
        Resource resource = requirePermission.resource();
        Operation[] operations = requirePermission.operations();
        
        if (operations.length == 0) {
            operations = new Operation[]{Operation.ALL};
        }
        
        boolean hasPermission = Arrays.stream(operations)
            .anyMatch(op -> permissionChecker.hasPermission(user, resource, op));
        
        if (!hasPermission) {
            throw new ThingsboardException(
                "Access denied to resource: " + resource,
                ThingsboardErrorCode.PERMISSION_DENIED
            );
        }
        
        return joinPoint.proceed();
    }
}
```

**Uso:**
```java
@RestController
public class DeviceController {
    
    @GetMapping("/api/device/{deviceId}")
    @RequirePermission(resource = Resource.DEVICE, operations = {Operation.READ})
    public Device getDevice(@PathVariable UUID deviceId) {
        // Método protegido
    }
    
    @PostMapping("/api/device")
    @RequirePermission(resource = Resource.DEVICE, operations = {Operation.CREATE})
    public Device createDevice(@RequestBody Device device) {
        // Método protegido
    }
}
```

---

### 6. Controller Layer

#### RoleController
```java
@RestController
@RequestMapping("/api/role")
@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
public class RoleController extends BaseController {
    
    @GetMapping
    public PageData<Role> getRoles(
        @RequestParam int pageSize,
        @RequestParam int page
    ) {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page);
        return roleService.findRolesByTenantId(tenantId, pageLink);
    }
    
    @PostMapping
    public Role saveRole(@RequestBody Role role) {
        role.setTenantId(getCurrentUser().getTenantId());
        return roleService.saveRole(role);
    }
    
    @PostMapping("/{roleId}/permissions")
    public void updatePermissions(
        @PathVariable UUID roleId,
        @RequestBody Set<RolePermission> permissions
    ) {
        roleService.updateRolePermissions(new RoleId(roleId), permissions);
    }
}
```

**Error Handling:**
```java
@ExceptionHandler(DataValidationException.class)
public ResponseEntity<ErrorResponse> handleValidationError(DataValidationException ex) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(ex.getMessage(), 2));
}
```

---

## Componentes Frontend

### 1. Models TypeScript

```typescript
// role.models.ts
export interface Role extends BaseData<RoleId> {
  tenantId: TenantId;
  name: string;
  description: string;
  isSystem: boolean;
  version: number;
}

export interface RolePermission {
  id?: RolePermissionId;
  roleId: RoleId;
  resource: Resource;
  operation: Operation;
}

export enum Resource {
  DEVICE = 'DEVICE',
  ASSET = 'ASSET',
  DASHBOARD = 'DASHBOARD',
  // ...
}

export enum Operation {
  ALL = 'ALL',
  CREATE = 'CREATE',
  READ = 'READ',
  WRITE = 'WRITE',
  DELETE = 'DELETE',
  // ...
}
```

### 2. Services Angular

```typescript
@Injectable({ providedIn: 'root' })
export class RoleService {
  
  constructor(private http: HttpClient) {}
  
  getRoles(pageLink: PageLink): Observable<PageData<Role>> {
    return this.http.get<PageData<Role>>('/api/role', {
      params: pageLink.toQuery()
    });
  }
  
  saveRole(role: Role): Observable<Role> {
    return this.http.post<Role>('/api/role', role);
  }
  
  updatePermissions(roleId: string, permissions: RolePermission[]): Observable<void> {
    return this.http.post<void>(`/api/role/${roleId}/permissions`, permissions);
  }
  
  getPermissions(roleId: string): Observable<RolePermission[]> {
    return this.http.get<RolePermission[]>(`/api/role/${roleId}/permissions`);
  }
}
```

### 3. Components

#### RoleComponent
```typescript
@Component({
  selector: 'tb-role',
  templateUrl: './role.component.html'
})
export class RoleComponent extends EntityComponent<Role> {
  
  buildForm(entity: Role): FormGroup {
    return this.fb.group({
      name: [entity?.name || '', [Validators.required, Validators.maxLength(255)]],
      description: [entity?.description || '', Validators.maxLength(1024)]
    });
  }
  
  save() {
    if (this.entityForm.valid) {
      const role: Role = {
        ...this.entity,
        ...this.entityForm.value
      };
      
      this.roleService.saveRole(role).subscribe(
        saved => this.onSaveSuccess(saved),
        error => this.onSaveError(error)
      );
    }
  }
}
```

#### RolePermissionsDialogComponent
```typescript
@Component({
  selector: 'tb-role-permissions-dialog',
  templateUrl: './role-permissions-dialog.component.html'
})
export class RolePermissionsDialogComponent implements OnInit {
  
  resources: Resource[];
  operations: Operation[];
  permissionsMatrix: Map<Resource, Set<Operation>> = new Map();
  
  ngOnInit() {
    this.loadResources();
    this.loadOperations();
    this.loadRolePermissions();
  }
  
  togglePermission(resource: Resource, operation: Operation) {
    if (!this.permissionsMatrix.has(resource)) {
      this.permissionsMatrix.set(resource, new Set());
    }
    
    const ops = this.permissionsMatrix.get(resource);
    if (ops.has(operation)) {
      ops.delete(operation);
    } else {
      ops.add(operation);
    }
  }
  
  save() {
    const permissions = this.matrixToPermissionsList();
    this.roleService.updatePermissions(this.roleId, permissions).subscribe(
      () => this.dialogRef.close(true),
      error => this.handleError(error)
    );
  }
  
  private matrixToPermissionsList(): RolePermission[] {
    const permissions: RolePermission[] = [];
    this.permissionsMatrix.forEach((operations, resource) => {
      operations.forEach(operation => {
        permissions.push({ resource, operation } as RolePermission);
      });
    });
    return permissions;
  }
}
```

### 4. Routing

```typescript
const routes: Routes = [
  {
    path: 'roles',
    component: RolesComponent,
    canActivate: [AuthGuard],
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        label: 'role.roles',
        icon: 'verified_user'
      }
    }
  }
];
```

### 5. Guards

```typescript
@Injectable()
export class PermissionGuard implements CanActivate {
  
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}
  
  canActivate(route: ActivatedRouteSnapshot): boolean {
    const user = this.authService.getCurrentUser();
    const requiredResource = route.data.requiredResource;
    const requiredOperation = route.data.requiredOperation;
    
    if (this.hasPermission(user, requiredResource, requiredOperation)) {
      return true;
    }
    
    this.router.navigate(['/access-denied']);
    return false;
  }
  
  private hasPermission(user: AuthUser, resource: Resource, operation: Operation): boolean {
    // Implementación de validación en cliente (no segura, solo UX)
    return user.permissions?.some(p => 
      p.resource === resource && 
      (p.operation === operation || p.operation === Operation.ALL)
    );
  }
}
```

---

## Sistema de Cache

### Arquitectura de Cache

```
┌──────────────┐
│  Controller  │
└──────┬───────┘
       │
       ▼
┌──────────────┐     Cache Miss     ┌──────────────┐
│  Service     │ ──────────────────> │  Caffeine    │
│ @Cacheable   │ <────────────────── │   Cache      │
└──────┬───────┘     Cache Hit       └──────────────┘
       │                                     │
       │ DB Query                            │ Evict on Update
       ▼                                     ▼
┌──────────────┐                      ┌──────────────┐
│  Repository  │                      │  Redis       │
└──────┬───────┘                      │  (Optional)  │
       │                              └──────────────┘
       ▼
┌──────────────┐
│  PostgreSQL  │
└──────────────┘
```

### Configuración

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("rolePermissions");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .recordStats());
        return cacheManager;
    }
}
```

### Estrategia de Invalidación

**Eventos que invalidan cache:**
1. Update role permissions
2. Delete role
3. Update role (si cambia is_system)

**Implementación:**
```java
@CacheEvict(value = "rolePermissions", key = "#roleId")
public void updateRolePermissions(RoleId roleId, Set<RolePermission> permissions) {
    // ...
}

@CacheEvict(value = "rolePermissions", key = "#roleId")
public void deleteRole(TenantId tenantId, RoleId roleId) {
    // ...
}
```

---

## Performance Considerations

### Database Optimization

**Índices Críticos:**
```sql
CREATE INDEX idx_role_permission_composite 
ON role_permission(role_id, resource_type, operation);

CREATE INDEX idx_user_role_id 
ON tb_user(role_id) WHERE role_id IS NOT NULL;

CREATE INDEX idx_role_tenant_id 
ON role(tenant_id) WHERE tenant_id IS NOT NULL;
```

**Query Optimization:**
```sql
-- Usar EXISTS en vez de COUNT para verificaciones
SELECT EXISTS (
    SELECT 1 FROM role_permission 
    WHERE role_id = ? 
    AND resource_type IN (?, 'ALL')
    AND operation IN (?, 'ALL')
    LIMIT 1
);

-- Evitar N+1 queries con joins
SELECT u.*, r.name as role_name
FROM tb_user u
LEFT JOIN role r ON u.role_id = r.id
WHERE u.tenant_id = ?;
```

### Benchmarks Esperados

| Operación | Target | Medida |
|-----------|--------|--------|
| Permission check (cached) | < 1ms | P95 |
| Permission check (uncached) | < 10ms | P95 |
| Get role | < 50ms | P95 |
| Save role | < 100ms | P95 |
| Update permissions | < 200ms | P95 |
| List roles (10 items) | < 100ms | P95 |

---

## Security Considerations

### 1. Validación en Múltiples Capas

```
Frontend (UX only)
    ↓
API Gateway (@PreAuthorize)
    ↓
Service Layer (Business logic)
    ↓
Aspect (@RequirePermission)
    ↓
Database (FK constraints)
```

### 2. Principio de Menor Privilegio

- Roles del sistema no editables
- Validación estricta de tenant_id
- No permitir escalar privilegios
- Auditoría de todos los cambios

### 3. Prevención de Ataques

**SQL Injection:**
- Usar prepared statements
- Validar todos los inputs
- Usar JPA/Hibernate correctamente

**Privilege Escalation:**
```java
public Role saveRole(Role role) {
    // Forzar tenant_id del usuario actual
    role.setTenantId(getCurrentUser().getTenantId());
    
    // Prevenir modificación de is_system
    if (role.getId() != null) {
        Role existing = roleDao.findById(role.getId());
        role.setSystem(existing.isSystem());
    }
    
    return roleDao.save(role);
}
```

**Session Hijacking:**
- Invalidar sesiones al cambiar rol
- Tokens JWT con expiración corta
- Refresh tokens seguros

---

## Monitoring y Observabilidad

### Métricas a Monitorear

```java
@Component
public class RoleMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordPermissionCheck(boolean cacheHit, long durationMs) {
        Timer.builder("role.permission.check")
            .tag("cache", cacheHit ? "hit" : "miss")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordRoleOperation(String operation, boolean success) {
        Counter.builder("role.operation")
            .tag("operation", operation)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }
}
```

### Logs Importantes

```java
log.info("Role created: roleId={}, name={}, tenantId={}", 
    role.getId(), role.getName(), role.getTenantId());

log.warn("Permission denied: userId={}, resource={}, operation={}", 
    userId, resource, operation);

log.error("Role deletion failed: roleId={}, reason={}", 
    roleId, exception.getMessage());
```

### Alertas

- Cache hit rate < 90%
- Permission check P95 > 50ms
- Failed permission checks > 100/min
- Role CRUD errors > 10/min

---

## Deployment Strategy

### Rolling Update

1. Deploy backend con feature flag OFF
2. Run database migrations
3. Verify migration success
4. Enable feature flag gradualmente (10%, 50%, 100%)
5. Monitor metrics
6. Deploy frontend

### Rollback Plan

```sql
-- Rollback script
ALTER TABLE tb_user DROP COLUMN role_id;
DROP TABLE role_permission CASCADE;
DROP TABLE role CASCADE;
```

### Feature Flag

```properties
# application.yml
features:
  roles-system:
    enabled: ${ROLES_SYSTEM_ENABLED:false}
    rollout-percentage: ${ROLES_ROLLOUT:0}
```

```java
@ConditionalOnProperty(name = "features.roles-system.enabled", havingValue = "true")
@Configuration
public class RolesSystemConfig {
    // Configuración del nuevo sistema
}
```

---

**Versión:** 1.0  
**Última Actualización:** 23 Enero 2026
