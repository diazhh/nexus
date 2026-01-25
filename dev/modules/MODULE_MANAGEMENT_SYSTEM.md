# Sistema de Gestión de Módulos (Module Management System)

## Visión General

El Sistema de Gestión de Módulos controla qué módulos operativos están disponibles para cada tenant, gestiona permisos, licencias, configuraciones y la activación/desactivación de funcionalidades modulares en Nexus.

## Conceptos Clave

### Módulo
Paquete completo de funcionalidad que incluye:
- Backend (APIs, servicios, entidades)
- Frontend (componentes, rutas, menús)
- Base de datos (esquemas, migraciones)
- Configuraciones (plantillas, reglas, dashboards)

### Activación de Módulo
Proceso de habilitar un módulo para un tenant específico, incluyendo:
- Verificación de licencia/permisos
- Instalación de componentes necesarios
- Configuración inicial
- Activación de menús y rutas

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    Admin Portal                              │
│  - Module Catalog                                            │
│  - Tenant Module Management                                  │
│  - License Management                                        │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Module Manager Service                    │
│  - Module Registry                                           │
│  - Activation Engine                                         │
│  - Permission Manager                                        │
│  - Configuration Manager                                     │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Module Loader (Runtime)                   │
│  - Dynamic Menu Builder                                      │
│  - Route Guard                                               │
│  - Feature Toggle                                            │
└─────────────────────────────────────────────────────────────┘
```

## Modelo de Datos

### Tabla: nx_modules

Catálogo de módulos disponibles en el sistema.

```sql
CREATE TABLE nx_modules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_key VARCHAR(100) NOT NULL UNIQUE, -- Identificador único (ej: 'coiled-tubing')
    module_name VARCHAR(255) NOT NULL,
    description TEXT,
    version VARCHAR(20) NOT NULL,
    category VARCHAR(50), -- 'OPERATIONS', 'ANALYTICS', 'REPORTING', etc.
    icon VARCHAR(100), -- Nombre del icono material
    author VARCHAR(255),
    requires_license BOOLEAN DEFAULT TRUE,
    min_tb_version VARCHAR(20), -- Versión mínima de TB/Nexus requerida
    dependencies JSONB, -- Array de module_keys de los que depende
    capabilities JSONB, -- Capacidades que proporciona el módulo
    configuration_schema JSONB, -- JSON Schema para configuración
    is_system_module BOOLEAN DEFAULT FALSE,
    is_available BOOLEAN DEFAULT TRUE,
    metadata JSONB,
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    CONSTRAINT uk_module_key UNIQUE (module_key)
);

CREATE INDEX idx_nx_modules_key ON nx_modules(module_key);
CREATE INDEX idx_nx_modules_category ON nx_modules(category);
CREATE INDEX idx_nx_modules_available ON nx_modules(is_available);
```

### Tabla: nx_tenant_modules

Relación de módulos activados por tenant.

```sql
CREATE TABLE nx_tenant_modules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    module_id UUID NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    activation_date BIGINT NOT NULL,
    deactivation_date BIGINT,
    license_key VARCHAR(500), -- Para módulos licenciados
    license_expiry_date BIGINT,
    configuration JSONB, -- Configuración específica del módulo para este tenant
    usage_limits JSONB, -- Límites de uso (ej: max_users, max_devices)
    activated_by UUID, -- Usuario que activó el módulo
    metadata JSONB,
    CONSTRAINT fk_tm_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_tm_module FOREIGN KEY (module_id) REFERENCES nx_modules(id) ON DELETE CASCADE,
    CONSTRAINT uk_tenant_module UNIQUE (tenant_id, module_id)
);

CREATE INDEX idx_nx_tenant_modules_tenant ON nx_tenant_modules(tenant_id);
CREATE INDEX idx_nx_tenant_modules_module ON nx_tenant_modules(module_id);
CREATE INDEX idx_nx_tenant_modules_active ON nx_tenant_modules(is_active);
```

### Tabla: nx_module_permissions

Permisos específicos de cada módulo.

```sql
CREATE TABLE nx_module_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_id UUID NOT NULL,
    permission_key VARCHAR(200) NOT NULL, -- Ej: 'CT_UNITS_VIEW', 'CT_REELS_EDIT'
    permission_name VARCHAR(255) NOT NULL,
    description TEXT,
    permission_group VARCHAR(100), -- Agrupación lógica (ej: 'UNITS', 'REELS')
    is_default BOOLEAN DEFAULT FALSE, -- Si se otorga por defecto al activar módulo
    metadata JSONB,
    CONSTRAINT fk_mp_module FOREIGN KEY (module_id) REFERENCES nx_modules(id) ON DELETE CASCADE,
    CONSTRAINT uk_module_permission UNIQUE (module_id, permission_key)
);

CREATE INDEX idx_nx_module_permissions_module ON nx_module_permissions(module_id);
CREATE INDEX idx_nx_module_permissions_key ON nx_module_permissions(permission_key);
```

### Tabla: nx_role_module_permissions

Relación entre roles y permisos de módulos.

```sql
CREATE TABLE nx_role_module_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID NOT NULL,
    module_permission_id UUID NOT NULL,
    granted BOOLEAN DEFAULT TRUE,
    granted_by UUID,
    granted_time BIGINT NOT NULL,
    CONSTRAINT fk_rmp_role FOREIGN KEY (role_id) REFERENCES nx_roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rmp_permission FOREIGN KEY (module_permission_id) REFERENCES nx_module_permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_module_permission UNIQUE (role_id, module_permission_id)
);

CREATE INDEX idx_nx_role_module_permissions_role ON nx_role_module_permissions(role_id);
CREATE INDEX idx_nx_role_module_permissions_permission ON nx_role_module_permissions(module_permission_id);
```

### Tabla: nx_module_menus

Definición de menús proporcionados por cada módulo.

```sql
CREATE TABLE nx_module_menus (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_id UUID NOT NULL,
    menu_key VARCHAR(100) NOT NULL,
    parent_menu_key VARCHAR(100), -- Para submenús
    menu_label VARCHAR(255) NOT NULL,
    menu_icon VARCHAR(100),
    route_path VARCHAR(500),
    order_index INTEGER DEFAULT 0,
    required_permission VARCHAR(200), -- Permiso necesario para ver el menú
    is_visible BOOLEAN DEFAULT TRUE,
    metadata JSONB,
    CONSTRAINT fk_mm_module FOREIGN KEY (module_id) REFERENCES nx_modules(id) ON DELETE CASCADE,
    CONSTRAINT uk_module_menu UNIQUE (module_id, menu_key)
);

CREATE INDEX idx_nx_module_menus_module ON nx_module_menus(module_id);
CREATE INDEX idx_nx_module_menus_parent ON nx_module_menus(parent_menu_key);
```

### Tabla: nx_module_routes

Definición de rutas proporcionadas por cada módulo.

```sql
CREATE TABLE nx_module_routes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_id UUID NOT NULL,
    route_path VARCHAR(500) NOT NULL,
    component_name VARCHAR(255) NOT NULL,
    required_permission VARCHAR(200),
    metadata JSONB,
    CONSTRAINT fk_mr_module FOREIGN KEY (module_id) REFERENCES nx_modules(id) ON DELETE CASCADE,
    CONSTRAINT uk_module_route UNIQUE (module_id, route_path)
);

CREATE INDEX idx_nx_module_routes_module ON nx_module_routes(module_id);
CREATE INDEX idx_nx_module_routes_path ON nx_module_routes(route_path);
```

### Tabla: nx_module_activation_log

Registro de activaciones/desactivaciones de módulos.

```sql
CREATE TABLE nx_module_activation_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    module_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL, -- 'ACTIVATED', 'DEACTIVATED', 'CONFIGURED'
    performed_by UUID NOT NULL,
    details JSONB,
    timestamp BIGINT NOT NULL,
    CONSTRAINT fk_mal_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_mal_module FOREIGN KEY (module_id) REFERENCES nx_modules(id) ON DELETE CASCADE
);

CREATE INDEX idx_nx_module_activation_log_tenant ON nx_module_activation_log(tenant_id);
CREATE INDEX idx_nx_module_activation_log_module ON nx_module_activation_log(module_id);
CREATE INDEX idx_nx_module_activation_log_timestamp ON nx_module_activation_log(timestamp);
```

## Modelo Java

### Entity: NexusModule

```java
@Entity
@Table(name = "nx_modules")
public class NexusModule {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "module_key", unique = true, nullable = false)
    private String moduleKey;
    
    @Column(name = "module_name", nullable = false)
    private String moduleName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "version", nullable = false)
    private String version;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "icon")
    private String icon;
    
    @Column(name = "author")
    private String author;
    
    @Column(name = "requires_license")
    private boolean requiresLicense;
    
    @Column(name = "min_tb_version")
    private String minTbVersion;
    
    @Type(type = "jsonb")
    @Column(name = "dependencies", columnDefinition = "jsonb")
    private JsonNode dependencies;
    
    @Type(type = "jsonb")
    @Column(name = "capabilities", columnDefinition = "jsonb")
    private JsonNode capabilities;
    
    @Type(type = "jsonb")
    @Column(name = "configuration_schema", columnDefinition = "jsonb")
    private JsonNode configurationSchema;
    
    @Column(name = "is_system_module")
    private boolean systemModule;
    
    @Column(name = "is_available")
    private boolean available;
    
    @Type(type = "jsonb")
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;
    
    @Column(name = "created_time", nullable = false)
    private long createdTime;
    
    @Column(name = "updated_time")
    private Long updatedTime;
    
    // Getters y Setters
}
```

### Entity: TenantModule

```java
@Entity
@Table(name = "nx_tenant_modules")
public class TenantModule {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "module_id", nullable = false)
    private UUID moduleId;
    
    @Column(name = "is_active")
    private boolean active;
    
    @Column(name = "activation_date", nullable = false)
    private long activationDate;
    
    @Column(name = "deactivation_date")
    private Long deactivationDate;
    
    @Column(name = "license_key")
    private String licenseKey;
    
    @Column(name = "license_expiry_date")
    private Long licenseExpiryDate;
    
    @Type(type = "jsonb")
    @Column(name = "configuration", columnDefinition = "jsonb")
    private JsonNode configuration;
    
    @Type(type = "jsonb")
    @Column(name = "usage_limits", columnDefinition = "jsonb")
    private JsonNode usageLimits;
    
    @Column(name = "activated_by")
    private UUID activatedBy;
    
    @Type(type = "jsonb")
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;
    
    // Getters y Setters
}
```

### Service: ModuleManagementService

```java
@Service
public class ModuleManagementService {
    
    @Autowired
    private NexusModuleRepository moduleRepository;
    
    @Autowired
    private TenantModuleRepository tenantModuleRepository;
    
    @Autowired
    private ModulePermissionRepository permissionRepository;
    
    @Autowired
    private RoleModulePermissionRepository rolePermissionRepository;
    
    @Autowired
    private ModuleMenuRepository menuRepository;
    
    @Autowired
    private ModuleActivationLogRepository activationLogRepository;
    
    /**
     * Registra un nuevo módulo en el sistema
     */
    @Transactional
    public NexusModule registerModule(ModuleRegistrationRequest request) {
        
        // Validar que no existe
        if (moduleRepository.existsByModuleKey(request.getModuleKey())) {
            throw new IllegalArgumentException("Module already registered: " + request.getModuleKey());
        }
        
        // Validar dependencias
        if (request.getDependencies() != null && !request.getDependencies().isEmpty()) {
            validateDependencies(request.getDependencies());
        }
        
        NexusModule module = new NexusModule();
        module.setId(UUID.randomUUID());
        module.setModuleKey(request.getModuleKey());
        module.setModuleName(request.getModuleName());
        module.setDescription(request.getDescription());
        module.setVersion(request.getVersion());
        module.setCategory(request.getCategory());
        module.setIcon(request.getIcon());
        module.setAuthor(request.getAuthor());
        module.setRequiresLicense(request.isRequiresLicense());
        module.setAvailable(true);
        module.setCreatedTime(System.currentTimeMillis());
        
        module = moduleRepository.save(module);
        
        // Registrar permisos del módulo
        if (request.getPermissions() != null) {
            for (ModulePermissionRequest permReq : request.getPermissions()) {
                registerModulePermission(module.getId(), permReq);
            }
        }
        
        // Registrar menús del módulo
        if (request.getMenus() != null) {
            for (ModuleMenuRequest menuReq : request.getMenus()) {
                registerModuleMenu(module.getId(), menuReq);
            }
        }
        
        return module;
    }
    
    /**
     * Activa un módulo para un tenant
     */
    @Transactional
    public TenantModule activateModuleForTenant(ModuleActivationRequest request) {
        
        UUID tenantId = request.getTenantId();
        UUID moduleId = request.getModuleId();
        
        // Validar que el módulo existe y está disponible
        NexusModule module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new NotFoundException("Module not found"));
        
        if (!module.isAvailable()) {
            throw new IllegalStateException("Module is not available");
        }
        
        // Validar que no está ya activado
        Optional<TenantModule> existing = tenantModuleRepository
            .findByTenantIdAndModuleId(tenantId, moduleId);
        
        if (existing.isPresent() && existing.get().isActive()) {
            throw new IllegalStateException("Module already activated for this tenant");
        }
        
        // Validar dependencias
        if (module.getDependencies() != null) {
            validateTenantDependencies(tenantId, module.getDependencies());
        }
        
        // Validar licencia si es necesario
        if (module.isRequiresLicense()) {
            validateLicense(request.getLicenseKey(), module.getModuleKey());
        }
        
        // Crear o actualizar registro
        TenantModule tenantModule = existing.orElse(new TenantModule());
        tenantModule.setId(tenantModule.getId() != null ? tenantModule.getId() : UUID.randomUUID());
        tenantModule.setTenantId(tenantId);
        tenantModule.setModuleId(moduleId);
        tenantModule.setActive(true);
        tenantModule.setActivationDate(System.currentTimeMillis());
        tenantModule.setDeactivationDate(null);
        
        if (module.isRequiresLicense()) {
            tenantModule.setLicenseKey(request.getLicenseKey());
            tenantModule.setLicenseExpiryDate(request.getLicenseExpiryDate());
        }
        
        tenantModule.setConfiguration(request.getConfiguration());
        tenantModule.setActivatedBy(request.getActivatedBy());
        
        tenantModule = tenantModuleRepository.save(tenantModule);
        
        // Ejecutar post-activación
        executePostActivation(tenantId, module);
        
        // Registrar log
        logModuleActivation(tenantId, moduleId, "ACTIVATED", request.getActivatedBy());
        
        return tenantModule;
    }
    
    /**
     * Desactiva un módulo para un tenant
     */
    @Transactional
    public void deactivateModuleForTenant(UUID tenantId, UUID moduleId, UUID deactivatedBy) {
        
        TenantModule tenantModule = tenantModuleRepository
            .findByTenantIdAndModuleId(tenantId, moduleId)
            .orElseThrow(() -> new NotFoundException("Module not activated for this tenant"));
        
        if (!tenantModule.isActive()) {
            throw new IllegalStateException("Module is already deactivated");
        }
        
        tenantModule.setActive(false);
        tenantModule.setDeactivationDate(System.currentTimeMillis());
        
        tenantModuleRepository.save(tenantModule);
        
        // Ejecutar post-desactivación
        executePostDeactivation(tenantId, moduleId);
        
        // Registrar log
        logModuleActivation(tenantId, moduleId, "DEACTIVATED", deactivatedBy);
    }
    
    /**
     * Obtiene módulos activos para un tenant
     */
    public List<ModuleInfo> getActiveTenantModules(UUID tenantId) {
        
        List<TenantModule> tenantModules = tenantModuleRepository
            .findByTenantIdAndActive(tenantId, true);
        
        List<ModuleInfo> result = new ArrayList<>();
        
        for (TenantModule tm : tenantModules) {
            NexusModule module = moduleRepository.findById(tm.getModuleId())
                .orElse(null);
            
            if (module != null) {
                ModuleInfo info = new ModuleInfo();
                info.setModule(module);
                info.setTenantModule(tm);
                info.setPermissions(getModulePermissions(module.getId()));
                info.setMenus(getModuleMenus(module.getId()));
                result.add(info);
            }
        }
        
        return result;
    }
    
    /**
     * Verifica si un tenant tiene acceso a un módulo
     */
    public boolean hasModuleAccess(UUID tenantId, String moduleKey) {
        
        NexusModule module = moduleRepository.findByModuleKey(moduleKey)
            .orElse(null);
        
        if (module == null || !module.isAvailable()) {
            return false;
        }
        
        Optional<TenantModule> tenantModule = tenantModuleRepository
            .findByTenantIdAndModuleId(tenantId, module.getId());
        
        if (!tenantModule.isPresent() || !tenantModule.get().isActive()) {
            return false;
        }
        
        // Verificar expiración de licencia si aplica
        if (module.isRequiresLicense()) {
            Long expiryDate = tenantModule.get().getLicenseExpiryDate();
            if (expiryDate != null && expiryDate < System.currentTimeMillis()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Obtiene permisos de módulo para un usuario
     */
    public Set<String> getUserModulePermissions(UUID userId, UUID tenantId) {
        
        Set<String> permissions = new HashSet<>();
        
        // Obtener roles del usuario
        List<UUID> userRoleIds = getUserRoleIds(userId);
        
        if (userRoleIds.isEmpty()) {
            return permissions;
        }
        
        // Obtener módulos activos del tenant
        List<TenantModule> activeModules = tenantModuleRepository
            .findByTenantIdAndActive(tenantId, true);
        
        for (TenantModule tm : activeModules) {
            // Obtener permisos del módulo
            List<ModulePermission> modulePermissions = permissionRepository
                .findByModuleId(tm.getModuleId());
            
            for (ModulePermission mp : modulePermissions) {
                // Verificar si algún rol del usuario tiene este permiso
                boolean hasPermission = rolePermissionRepository
                    .existsByRoleIdInAndModulePermissionIdAndGranted(
                        userRoleIds, mp.getId(), true);
                
                if (hasPermission) {
                    permissions.add(mp.getPermissionKey());
                }
            }
        }
        
        return permissions;
    }
    
    /**
     * Ejecuta acciones post-activación
     */
    private void executePostActivation(UUID tenantId, NexusModule module) {
        
        log.info("Executing post-activation for module {} on tenant {}", 
                 module.getModuleKey(), tenantId);
        
        // Aquí se pueden ejecutar acciones como:
        // - Crear assets/devices iniciales
        // - Instalar rule chains
        // - Crear dashboards
        // - Inicializar configuraciones
        
        // Ejemplo: Instalar plantillas del módulo
        installModuleTemplates(tenantId, module.getId());
        
        // Instalar rule chains
        installModuleRuleChains(tenantId, module.getId());
    }
    
    /**
     * Ejecuta acciones post-desactivación
     */
    private void executePostDeactivation(UUID tenantId, UUID moduleId) {
        
        log.info("Executing post-deactivation for module {} on tenant {}", 
                 moduleId, tenantId);
        
        // Nota: Generalmente NO se eliminan datos al desactivar
        // Solo se deshabilita el acceso a funcionalidades
    }
}
```

## API REST

### Endpoints

```java
@RestController
@RequestMapping("/api/nexus/modules")
public class ModuleManagementController {
    
    @Autowired
    private ModuleManagementService moduleService;
    
    /**
     * GET /api/nexus/modules/catalog
     * Obtener catálogo de módulos disponibles
     */
    @GetMapping("/catalog")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public List<NexusModule> getModuleCatalog(
            @RequestParam(required = false) String category) {
        
        if (category != null) {
            return moduleService.getAvailableModulesByCategory(category);
        }
        return moduleService.getAllAvailableModules();
    }
    
    /**
     * POST /api/nexus/modules/activate
     * Activar módulo para el tenant actual
     */
    @PostMapping("/activate")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public TenantModule activateModule(@RequestBody ModuleActivationRequest request) {
        
        UUID tenantId = getCurrentUser().getTenantId().getId();
        UUID userId = getCurrentUser().getId().getId();
        
        request.setTenantId(tenantId);
        request.setActivatedBy(userId);
        
        return moduleService.activateModuleForTenant(request);
    }
    
    /**
     * POST /api/nexus/modules/{moduleId}/deactivate
     * Desactivar módulo
     */
    @PostMapping("/{moduleId}/deactivate")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public void deactivateModule(@PathVariable UUID moduleId) {
        
        UUID tenantId = getCurrentUser().getTenantId().getId();
        UUID userId = getCurrentUser().getId().getId();
        
        moduleService.deactivateModuleForTenant(tenantId, moduleId, userId);
    }
    
    /**
     * GET /api/nexus/modules/active
     * Obtener módulos activos del tenant
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<ModuleInfo> getActiveModules() {
        
        UUID tenantId = getCurrentUser().getTenantId().getId();
        return moduleService.getActiveTenantModules(tenantId);
    }
    
    /**
     * GET /api/nexus/modules/check-access/{moduleKey}
     * Verificar acceso a módulo
     */
    @GetMapping("/check-access/{moduleKey}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public ModuleAccessResponse checkModuleAccess(@PathVariable String moduleKey) {
        
        UUID tenantId = getCurrentUser().getTenantId().getId();
        boolean hasAccess = moduleService.hasModuleAccess(tenantId, moduleKey);
        
        return new ModuleAccessResponse(moduleKey, hasAccess);
    }
    
    /**
     * GET /api/nexus/modules/permissions
     * Obtener permisos de módulos del usuario actual
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public Set<String> getUserModulePermissions() {
        
        UUID userId = getCurrentUser().getId().getId();
        UUID tenantId = getCurrentUser().getTenantId().getId();
        
        return moduleService.getUserModulePermissions(userId, tenantId);
    }
    
    /**
     * GET /api/nexus/modules/{moduleId}/configuration
     * Obtener configuración del módulo
     */
    @GetMapping("/{moduleId}/configuration")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public JsonNode getModuleConfiguration(@PathVariable UUID moduleId) {
        
        UUID tenantId = getCurrentUser().getTenantId().getId();
        return moduleService.getModuleConfiguration(tenantId, moduleId);
    }
    
    /**
     * PUT /api/nexus/modules/{moduleId}/configuration
     * Actualizar configuración del módulo
     */
    @PutMapping("/{moduleId}/configuration")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public void updateModuleConfiguration(
            @PathVariable UUID moduleId,
            @RequestBody JsonNode configuration) {
        
        UUID tenantId = getCurrentUser().getTenantId().getId();
        moduleService.updateModuleConfiguration(tenantId, moduleId, configuration);
    }
}
```

## Frontend (Angular)

### Service: ModuleService

```typescript
@Injectable({
  providedIn: 'root'
})
export class ModuleService {
  
  constructor(private http: HttpClient) {}
  
  getModuleCatalog(category?: string): Observable<NexusModule[]> {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    
    return this.http.get<NexusModule[]>('/api/nexus/modules/catalog', { params });
  }
  
  activateModule(request: ModuleActivationRequest): Observable<TenantModule> {
    return this.http.post<TenantModule>('/api/nexus/modules/activate', request);
  }
  
  deactivateModule(moduleId: string): Observable<void> {
    return this.http.post<void>(`/api/nexus/modules/${moduleId}/deactivate`, {});
  }
  
  getActiveModules(): Observable<ModuleInfo[]> {
    return this.http.get<ModuleInfo[]>('/api/nexus/modules/active');
  }
  
  checkModuleAccess(moduleKey: string): Observable<ModuleAccessResponse> {
    return this.http.get<ModuleAccessResponse>(`/api/nexus/modules/check-access/${moduleKey}`);
  }
  
  getUserModulePermissions(): Observable<Set<string>> {
    return this.http.get<Set<string>>('/api/nexus/modules/permissions');
  }
}
```

### Guard: ModuleAccessGuard

```typescript
@Injectable({
  providedIn: 'root'
})
export class ModuleAccessGuard implements CanActivate {
  
  constructor(
    private moduleService: ModuleService,
    private router: Router
  ) {}
  
  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    const moduleKey = route.data['moduleKey'];
    
    if (!moduleKey) {
      return of(true); // No requiere módulo específico
    }
    
    return this.moduleService.checkModuleAccess(moduleKey).pipe(
      map(response => {
        if (!response.hasAccess) {
          this.router.navigate(['/access-denied']);
          return false;
        }
        return true;
      }),
      catchError(() => {
        this.router.navigate(['/error']);
        return of(false);
      })
    );
  }
}
```

### Component: Module Manager

```typescript
@Component({
  selector: 'tb-module-manager',
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Gestión de Módulos</mat-card-title>
      </mat-card-header>
      
      <mat-card-content>
        <mat-tab-group>
          <!-- Tab: Módulos Disponibles -->
          <mat-tab label="Catálogo">
            <div class="modules-grid">
              <mat-card *ngFor="let module of availableModules" class="module-card">
                <mat-card-header>
                  <mat-icon mat-card-avatar>{{module.icon}}</mat-icon>
                  <mat-card-title>{{module.moduleName}}</mat-card-title>
                  <mat-card-subtitle>v{{module.version}}</mat-card-subtitle>
                </mat-card-header>
                
                <mat-card-content>
                  <p>{{module.description}}</p>
                  
                  <div class="module-meta">
                    <span class="category">{{module.category}}</span>
                    <span class="license" *ngIf="module.requiresLicense">
                      <mat-icon>lock</mat-icon> Requiere Licencia
                    </span>
                  </div>
                </mat-card-content>
                
                <mat-card-actions>
                  <button mat-raised-button color="primary"
                          *ngIf="!isModuleActive(module.id)"
                          (click)="activateModule(module)">
                    Activar
                  </button>
                  <button mat-stroked-button 
                          *ngIf="isModuleActive(module.id)">
                    <mat-icon>check</mat-icon> Activado
                  </button>
                  <button mat-button (click)="viewModuleDetails(module)">
                    Detalles
                  </button>
                </mat-card-actions>
              </mat-card>
            </div>
          </mat-tab>
          
          <!-- Tab: Módulos Activos -->
          <mat-tab label="Activos">
            <mat-list>
              <mat-list-item *ngFor="let info of activeModules">
                <mat-icon matListItemIcon>{{info.module.icon}}</mat-icon>
                <h3 matListItemTitle>{{info.module.moduleName}}</h3>
                <p matListItemLine>
                  Activado: {{info.tenantModule.activationDate | date}}
                </p>
                <button mat-icon-button matListItemMeta 
                        (click)="configureModule(info)">
                  <mat-icon>settings</mat-icon>
                </button>
                <button mat-icon-button matListItemMeta 
                        (click)="deactivateModule(info)"
                        color="warn">
                  <mat-icon>power_off</mat-icon>
                </button>
              </mat-list-item>
            </mat-list>
          </mat-tab>
        </mat-tab-group>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .modules-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 16px;
      margin-top: 16px;
    }
    
    .module-card {
      height: 100%;
    }
    
    .module-meta {
      display: flex;
      gap: 12px;
      margin-top: 12px;
    }
    
    .category {
      background: #e0e0e0;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
    }
  `]
})
export class ModuleManagerComponent implements OnInit {
  
  availableModules: NexusModule[] = [];
  activeModules: ModuleInfo[] = [];
  
  constructor(
    private moduleService: ModuleService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}
  
  ngOnInit() {
    this.loadModuleCatalog();
    this.loadActiveModules();
  }
  
  loadModuleCatalog() {
    this.moduleService.getModuleCatalog().subscribe(modules => {
      this.availableModules = modules;
    });
  }
  
  loadActiveModules() {
    this.moduleService.getActiveModules().subscribe(modules => {
      this.activeModules = modules;
    });
  }
  
  isModuleActive(moduleId: string): boolean {
    return this.activeModules.some(m => m.module.id === moduleId);
  }
  
  activateModule(module: NexusModule) {
    const dialogRef = this.dialog.open(ModuleActivationDialogComponent, {
      data: { module }
    });
    
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.moduleService.activateModule({
          moduleId: module.id,
          licenseKey: result.licenseKey,
          configuration: result.configuration
        }).subscribe(() => {
          this.snackBar.open('Módulo activado correctamente', 'Cerrar', {
            duration: 3000
          });
          this.loadActiveModules();
        });
      }
    });
  }
  
  deactivateModule(info: ModuleInfo) {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Desactivar Módulo',
        message: `¿Está seguro de desactivar el módulo ${info.module.moduleName}?`
      }
    });
    
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.moduleService.deactivateModule(info.module.id).subscribe(() => {
          this.snackBar.open('Módulo desactivado', 'Cerrar', {
            duration: 3000
          });
          this.loadActiveModules();
        });
      }
    });
  }
}
```

## Registro de Módulos en Startup

### ModuleInitializer

```java
@Component
public class ModuleInitializer implements ApplicationRunner {
    
    @Autowired
    private ModuleManagementService moduleService;
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing Nexus modules...");
        
        // Registrar módulos del sistema
        registerSystemModules();
        
        log.info("Module initialization completed");
    }
    
    private void registerSystemModules() {
        
        // Módulo Core (siempre activado)
        registerCoreModule();
        
        // Módulo Coiled Tubing
        registerCoiledTubingModule();
        
        // Otros módulos...
    }
    
    private void registerCoiledTubingModule() {
        
        if (moduleService.moduleExists("coiled-tubing")) {
            log.info("Coiled Tubing module already registered");
            return;
        }
        
        ModuleRegistrationRequest request = new ModuleRegistrationRequest();
        request.setModuleKey("coiled-tubing");
        request.setModuleName("Coiled Tubing");
        request.setDescription("Gestión de operaciones de Coiled Tubing");
        request.setVersion("1.0.0");
        request.setCategory("OPERATIONS");
        request.setIcon("settings_input_antenna");
        request.setAuthor("Nexus Team");
        request.setRequiresLicense(true);
        
        // Permisos
        List<ModulePermissionRequest> permissions = Arrays.asList(
            new ModulePermissionRequest("CT_UNITS_VIEW", "Ver Unidades", "UNITS", true),
            new ModulePermissionRequest("CT_UNITS_EDIT", "Editar Unidades", "UNITS", false),
            new ModulePermissionRequest("CT_REELS_VIEW", "Ver Reels", "REELS", true),
            new ModulePermissionRequest("CT_REELS_EDIT", "Editar Reels", "REELS", false),
            new ModulePermissionRequest("CT_JOBS_VIEW", "Ver Trabajos", "JOBS", true),
            new ModulePermissionRequest("CT_JOBS_MANAGE", "Gestionar Trabajos", "JOBS", false)
        );
        request.setPermissions(permissions);
        
        // Menús
        List<ModuleMenuRequest> menus = Arrays.asList(
            new ModuleMenuRequest("ct_main", null, "Coiled Tubing", "settings_input_antenna", 
                                 null, 100, null),
            new ModuleMenuRequest("ct_units", "ct_main", "Unidades", null, 
                                 "/modules/coiled-tubing/units", 1, "CT_UNITS_VIEW"),
            new ModuleMenuRequest("ct_reels", "ct_main", "Reels", null, 
                                 "/modules/coiled-tubing/reels", 2, "CT_REELS_VIEW"),
            new ModuleMenuRequest("ct_jobs", "ct_main", "Trabajos", null, 
                                 "/modules/coiled-tubing/jobs", 3, "CT_JOBS_VIEW"),
            new ModuleMenuRequest("ct_reports", "ct_main", "Reportes", null, 
                                 "/modules/coiled-tubing/reports", 4, "CT_UNITS_VIEW")
        );
        request.setMenus(menus);
        
        moduleService.registerModule(request);
        
        log.info("Coiled Tubing module registered successfully");
    }
}
```

## Ejemplo de Uso

### Activar Módulo CT para un Tenant

```java
ModuleActivationRequest request = new ModuleActivationRequest();
request.setModuleId(coiledTubingModuleId);
request.setLicenseKey("NEXUS-CT-XXXXX-XXXXX");
request.setLicenseExpiryDate(System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)); // 1 año

// Configuración inicial
ObjectNode config = objectMapper.createObjectNode();
config.put("max_units", 10);
config.put("max_reels", 50);
config.put("enable_fatigue_calculation", true);
request.setConfiguration(config);

TenantModule activated = moduleService.activateModuleForTenant(request);
```

### Verificar Acceso desde Component

```typescript
@Component({...})
export class CoiledTubingComponent implements OnInit {
  
  hasEditPermission = false;
  
  constructor(private moduleService: ModuleService) {}
  
  ngOnInit() {
    this.moduleService.getUserModulePermissions().subscribe(permissions => {
      this.hasEditPermission = permissions.has('CT_UNITS_EDIT');
    });
  }
}
```

## Ventajas del Sistema

1. **Modularidad**: Activar solo lo necesario por tenant
2. **Control de Licencias**: Gestión centralizada de licencias
3. **Seguridad**: Permisos granulares por módulo
4. **Escalabilidad**: Agregar nuevos módulos sin modificar core
5. **Flexibilidad**: Configuración personalizada por tenant

## Próximos Pasos

1. Implementar esquema de base de datos
2. Desarrollar servicios backend
3. Crear componentes frontend
4. Implementar sistema de licencias
5. Crear módulo ejemplo (Coiled Tubing)
6. Pruebas de activación/desactivación
