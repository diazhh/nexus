# Sistema de Menús Dinámicos (Dynamic Menu System)

## Visión General

El Sistema de Menús Dinámicos genera automáticamente la estructura de menús de la aplicación basándose en los módulos activos y permisos del usuario. Esto permite que la interfaz se adapte dinámicamente a las capacidades disponibles para cada tenant y usuario.

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    Angular App Init                          │
│  - Menu Service Bootstrap                                    │
│  - Permission Service Init                                   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Menu Builder Service                      │
│  - Fetch Active Modules                                      │
│  - Fetch User Permissions                                    │
│  - Build Menu Tree                                           │
│  - Apply Visibility Rules                                    │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Menu Component                            │
│  - Render Menu Tree                                          │
│  - Handle Navigation                                         │
│  - Update on Permission Change                               │
└─────────────────────────────────────────────────────────────┘
```

## Estructura de Menú

### Jerarquía de Menús

```
Core (Siempre visible)
├── Home
├── Assets
├── Devices
├── Dashboards
└── Usuarios y Roles (según permisos)

Módulos (Según activación)
├── Coiled Tubing
│   ├── Unidades
│   ├── Reels
│   ├── Trabajos
│   └── Reportes
├── [Otro Módulo]
│   ├── Submenu 1
│   └── Submenu 2
└── ...

Configuración (según permisos)
├── Módulos
├── Sistema
└── Integraciones
```

## Backend API

### Endpoint: Get User Menu

```java
@RestController
@RequestMapping("/api/nexus/menu")
public class MenuController {
    
    @Autowired
    private MenuBuilderService menuBuilderService;
    
    /**
     * GET /api/nexus/menu
     * Obtiene el menú completo para el usuario actual
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public MenuStructure getUserMenu() {
        
        UUID userId = getCurrentUser().getId().getId();
        UUID tenantId = getCurrentUser().getTenantId().getId();
        Authority authority = getCurrentUser().getAuthority();
        
        return menuBuilderService.buildMenuForUser(userId, tenantId, authority);
    }
    
    /**
     * GET /api/nexus/menu/modules
     * Obtiene solo menús de módulos
     */
    @GetMapping("/modules")
    @PreAuthorize("isAuthenticated()")
    public List<MenuSection> getModuleMenus() {
        
        UUID userId = getCurrentUser().getId().getId();
        UUID tenantId = getCurrentUser().getTenantId().getId();
        
        return menuBuilderService.buildModuleMenus(userId, tenantId);
    }
}
```

### Service: MenuBuilderService

```java
@Service
public class MenuBuilderService {
    
    @Autowired
    private ModuleManagementService moduleService;
    
    @Autowired
    private ModuleMenuRepository menuRepository;
    
    @Autowired
    private RolePermissionService rolePermissionService;
    
    /**
     * Construye el menú completo para un usuario
     */
    public MenuStructure buildMenuForUser(UUID userId, UUID tenantId, Authority authority) {
        
        MenuStructure structure = new MenuStructure();
        
        // 1. Agregar menús core (siempre visibles según authority)
        structure.setCoreMenus(buildCoreMenus(authority));
        
        // 2. Obtener permisos del usuario
        Set<String> userPermissions = moduleService.getUserModulePermissions(userId, tenantId);
        
        // 3. Obtener módulos activos
        List<ModuleInfo> activeModules = moduleService.getActiveTenantModules(tenantId);
        
        // 4. Construir menús de módulos
        List<MenuSection> moduleSections = new ArrayList<>();
        
        for (ModuleInfo moduleInfo : activeModules) {
            List<ModuleMenu> moduleMenus = menuRepository
                .findByModuleIdAndVisible(moduleInfo.getModule().getId(), true);
            
            MenuSection section = buildModuleMenuSection(
                moduleInfo.getModule(),
                moduleMenus,
                userPermissions
            );
            
            if (section != null && !section.getItems().isEmpty()) {
                moduleSections.add(section);
            }
        }
        
        structure.setModuleSections(moduleSections);
        
        // 5. Agregar menús de configuración
        if (hasConfigurationAccess(authority, userPermissions)) {
            structure.setConfigurationMenus(buildConfigurationMenus(authority, userPermissions));
        }
        
        return structure;
    }
    
    /**
     * Construye una sección de menú para un módulo
     */
    private MenuSection buildModuleMenuSection(
            NexusModule module,
            List<ModuleMenu> menus,
            Set<String> userPermissions) {
        
        // Construir árbol de menús
        Map<String, List<ModuleMenu>> menuTree = menus.stream()
            .collect(Collectors.groupingBy(
                m -> m.getParentMenuKey() != null ? m.getParentMenuKey() : "ROOT"
            ));
        
        // Obtener menús raíz (sin parent)
        List<ModuleMenu> rootMenus = menuTree.getOrDefault("ROOT", Collections.emptyList());
        
        if (rootMenus.isEmpty()) {
            return null;
        }
        
        MenuSection section = new MenuSection();
        section.setModuleKey(module.getModuleKey());
        section.setModuleName(module.getModuleName());
        section.setIcon(module.getIcon());
        
        List<MenuItem> items = new ArrayList<>();
        
        for (ModuleMenu rootMenu : rootMenus) {
            MenuItem item = buildMenuItem(rootMenu, menuTree, userPermissions);
            if (item != null) {
                items.add(item);
            }
        }
        
        section.setItems(items);
        
        return section;
    }
    
    /**
     * Construye un item de menú recursivamente
     */
    private MenuItem buildMenuItem(
            ModuleMenu menu,
            Map<String, List<ModuleMenu>> menuTree,
            Set<String> userPermissions) {
        
        // Verificar permiso requerido
        if (menu.getRequiredPermission() != null) {
            if (!userPermissions.contains(menu.getRequiredPermission())) {
                return null; // Usuario no tiene permiso
            }
        }
        
        MenuItem item = new MenuItem();
        item.setKey(menu.getMenuKey());
        item.setLabel(menu.getMenuLabel());
        item.setIcon(menu.getMenuIcon());
        item.setRoute(menu.getRoutePath());
        item.setOrderIndex(menu.getOrderIndex());
        
        // Construir hijos
        List<ModuleMenu> children = menuTree.get(menu.getMenuKey());
        if (children != null && !children.isEmpty()) {
            List<MenuItem> childItems = new ArrayList<>();
            for (ModuleMenu child : children) {
                MenuItem childItem = buildMenuItem(child, menuTree, userPermissions);
                if (childItem != null) {
                    childItems.add(childItem);
                }
            }
            
            // Ordenar hijos
            childItems.sort(Comparator.comparing(MenuItem::getOrderIndex));
            item.setChildren(childItems);
        }
        
        return item;
    }
    
    /**
     * Construye menús core del sistema
     */
    private List<MenuItem> buildCoreMenus(Authority authority) {
        
        List<MenuItem> coreMenus = new ArrayList<>();
        
        // Home (todos)
        coreMenus.add(MenuItem.builder()
            .key("home")
            .label("Inicio")
            .icon("home")
            .route("/home")
            .orderIndex(0)
            .build());
        
        // Assets (todos)
        coreMenus.add(MenuItem.builder()
            .key("assets")
            .label("Assets")
            .icon("domain")
            .route("/assets")
            .orderIndex(10)
            .build());
        
        // Devices (todos)
        coreMenus.add(MenuItem.builder()
            .key("devices")
            .label("Devices")
            .icon("devices_other")
            .route("/devices")
            .orderIndex(20)
            .build());
        
        // Dashboards (todos)
        coreMenus.add(MenuItem.builder()
            .key("dashboards")
            .label("Dashboards")
            .icon("dashboard")
            .route("/dashboards")
            .orderIndex(30)
            .build());
        
        // Usuarios (según authority)
        if (authority == Authority.TENANT_ADMIN) {
            MenuItem usersMenu = MenuItem.builder()
                .key("users")
                .label("Usuarios y Roles")
                .icon("people")
                .orderIndex(40)
                .build();
            
            List<MenuItem> userChildren = new ArrayList<>();
            userChildren.add(MenuItem.builder()
                .key("users_list")
                .label("Usuarios")
                .route("/users")
                .orderIndex(1)
                .build());
            
            userChildren.add(MenuItem.builder()
                .key("roles_list")
                .label("Roles")
                .route("/roles")
                .orderIndex(2)
                .build());
            
            usersMenu.setChildren(userChildren);
            coreMenus.add(usersMenu);
        }
        
        return coreMenus;
    }
    
    /**
     * Construye menús de configuración
     */
    private List<MenuItem> buildConfigurationMenus(
            Authority authority,
            Set<String> userPermissions) {
        
        if (authority != Authority.TENANT_ADMIN) {
            return Collections.emptyList();
        }
        
        List<MenuItem> configMenus = new ArrayList<>();
        
        // Gestión de módulos
        configMenus.add(MenuItem.builder()
            .key("config_modules")
            .label("Módulos")
            .icon("extension")
            .route("/configuration/modules")
            .orderIndex(1)
            .build());
        
        // Configuración del sistema
        configMenus.add(MenuItem.builder()
            .key("config_system")
            .label("Sistema")
            .icon("settings")
            .route("/configuration/system")
            .orderIndex(2)
            .build());
        
        return configMenus;
    }
}
```

## Frontend (Angular)

### Models

```typescript
export interface MenuStructure {
  coreMenus: MenuItem[];
  moduleSections: MenuSection[];
  configurationMenus: MenuItem[];
}

export interface MenuSection {
  moduleKey: string;
  moduleName: string;
  icon: string;
  items: MenuItem[];
}

export interface MenuItem {
  key: string;
  label: string;
  icon?: string;
  route?: string;
  children?: MenuItem[];
  orderIndex: number;
  badge?: MenuBadge;
}

export interface MenuBadge {
  value: string | number;
  color?: 'primary' | 'accent' | 'warn';
}
```

### Service: MenuService

```typescript
@Injectable({
  providedIn: 'root'
})
export class MenuService {
  
  private menuStructure$ = new BehaviorSubject<MenuStructure | null>(null);
  private loading$ = new BehaviorSubject<boolean>(false);
  
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    // Cargar menú cuando el usuario se autentica
    this.authService.authState$.pipe(
      filter(state => state.isAuthenticated)
    ).subscribe(() => {
      this.loadMenu();
    });
  }
  
  getMenuStructure(): Observable<MenuStructure | null> {
    return this.menuStructure$.asObservable();
  }
  
  isLoading(): Observable<boolean> {
    return this.loading$.asObservable();
  }
  
  loadMenu(): void {
    this.loading$.next(true);
    
    this.http.get<MenuStructure>('/api/nexus/menu').pipe(
      catchError(error => {
        console.error('Error loading menu:', error);
        return of(null);
      }),
      finalize(() => this.loading$.next(false))
    ).subscribe(menu => {
      this.menuStructure$.next(menu);
    });
  }
  
  reloadMenu(): void {
    this.loadMenu();
  }
  
  hasMenuItem(key: string): Observable<boolean> {
    return this.menuStructure$.pipe(
      map(structure => {
        if (!structure) return false;
        return this.findMenuItemByKey(structure, key) !== null;
      })
    );
  }
  
  private findMenuItemByKey(structure: MenuStructure, key: string): MenuItem | null {
    // Buscar en core menus
    let found = this.searchInItems(structure.coreMenus, key);
    if (found) return found;
    
    // Buscar en module sections
    for (const section of structure.moduleSections) {
      found = this.searchInItems(section.items, key);
      if (found) return found;
    }
    
    // Buscar en configuration menus
    found = this.searchInItems(structure.configurationMenus, key);
    if (found) return found;
    
    return null;
  }
  
  private searchInItems(items: MenuItem[], key: string): MenuItem | null {
    for (const item of items) {
      if (item.key === key) return item;
      
      if (item.children) {
        const found = this.searchInItems(item.children, key);
        if (found) return found;
      }
    }
    return null;
  }
}
```

### Component: Dynamic Menu

```typescript
@Component({
  selector: 'tb-dynamic-menu',
  template: `
    <mat-nav-list *ngIf="menuStructure$ | async as menu" class="nexus-menu">
      
      <!-- Core Menus -->
      <div class="menu-section">
        <ng-container *ngFor="let item of menu.coreMenus">
          <tb-menu-item [item]="item" [level]="0"></tb-menu-item>
        </ng-container>
      </div>
      
      <!-- Module Sections -->
      <mat-divider *ngIf="menu.moduleSections.length > 0"></mat-divider>
      
      <div class="menu-section" *ngFor="let section of menu.moduleSections">
        <div class="section-header">
          <mat-icon>{{section.icon}}</mat-icon>
          <span>{{section.moduleName}}</span>
        </div>
        
        <ng-container *ngFor="let item of section.items">
          <tb-menu-item [item]="item" [level]="0"></tb-menu-item>
        </ng-container>
      </div>
      
      <!-- Configuration Menus -->
      <mat-divider *ngIf="menu.configurationMenus.length > 0"></mat-divider>
      
      <div class="menu-section">
        <div class="section-header">
          <mat-icon>settings</mat-icon>
          <span>Configuración</span>
        </div>
        
        <ng-container *ngFor="let item of menu.configurationMenus">
          <tb-menu-item [item]="item" [level]="0"></tb-menu-item>
        </ng-container>
      </div>
      
    </mat-nav-list>
    
    <div *ngIf="(loading$ | async)" class="loading-indicator">
      <mat-spinner diameter="40"></mat-spinner>
    </div>
  `,
  styles: [`
    .nexus-menu {
      padding: 0;
    }
    
    .menu-section {
      padding: 8px 0;
    }
    
    .section-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      color: rgba(0, 0, 0, 0.54);
      font-size: 14px;
      font-weight: 500;
      text-transform: uppercase;
    }
    
    .loading-indicator {
      display: flex;
      justify-content: center;
      padding: 24px;
    }
  `]
})
export class DynamicMenuComponent implements OnInit {
  
  menuStructure$: Observable<MenuStructure | null>;
  loading$: Observable<boolean>;
  
  constructor(private menuService: MenuService) {
    this.menuStructure$ = this.menuService.getMenuStructure();
    this.loading$ = this.menuService.isLoading();
  }
  
  ngOnInit() {
    // El menú ya se carga automáticamente en el servicio
  }
}
```

### Component: Menu Item (Recursivo)

```typescript
@Component({
  selector: 'tb-menu-item',
  template: `
    <div class="menu-item-wrapper">
      <!-- Item con hijos (expandible) -->
      <mat-list-item *ngIf="item.children && item.children.length > 0"
                     (click)="toggleExpanded()"
                     [class.active]="isActive()"
                     [style.padding-left.px]="level * 16">
        <mat-icon matListItemIcon *ngIf="item.icon">{{item.icon}}</mat-icon>
        <span matListItemTitle>{{item.label}}</span>
        <mat-icon matListItemMeta class="expand-icon" 
                  [class.expanded]="expanded">
          expand_more
        </mat-icon>
        <span matListItemMeta *ngIf="item.badge" 
              class="menu-badge"
              [class]="'badge-' + (item.badge.color || 'primary')">
          {{item.badge.value}}
        </span>
      </mat-list-item>
      
      <!-- Item sin hijos (navegable) -->
      <mat-list-item *ngIf="!item.children || item.children.length === 0"
                     [routerLink]="item.route"
                     routerLinkActive="active"
                     [style.padding-left.px]="level * 16">
        <mat-icon matListItemIcon *ngIf="item.icon">{{item.icon}}</mat-icon>
        <span matListItemTitle>{{item.label}}</span>
        <span matListItemMeta *ngIf="item.badge" 
              class="menu-badge"
              [class]="'badge-' + (item.badge.color || 'primary')">
          {{item.badge.value}}
        </span>
      </mat-list-item>
      
      <!-- Hijos (recursivo) -->
      <div class="menu-children" *ngIf="expanded && item.children">
        <tb-menu-item *ngFor="let child of item.children"
                      [item]="child"
                      [level]="level + 1">
        </tb-menu-item>
      </div>
    </div>
  `,
  styles: [`
    .menu-item-wrapper {
      width: 100%;
    }
    
    mat-list-item {
      cursor: pointer;
      height: 48px;
      transition: background-color 0.2s;
    }
    
    mat-list-item:hover {
      background-color: rgba(0, 0, 0, 0.04);
    }
    
    mat-list-item.active {
      background-color: rgba(63, 81, 181, 0.12);
      color: #3f51b5;
    }
    
    .expand-icon {
      transition: transform 0.2s;
    }
    
    .expand-icon.expanded {
      transform: rotate(180deg);
    }
    
    .menu-children {
      overflow: hidden;
    }
    
    .menu-badge {
      background-color: #f44336;
      color: white;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
      min-width: 20px;
      text-align: center;
    }
    
    .menu-badge.badge-primary {
      background-color: #3f51b5;
    }
    
    .menu-badge.badge-accent {
      background-color: #ff4081;
    }
    
    .menu-badge.badge-warn {
      background-color: #f44336;
    }
  `]
})
export class MenuItemComponent implements OnInit {
  
  @Input() item: MenuItem;
  @Input() level: number = 0;
  
  expanded: boolean = false;
  
  constructor(private router: Router) {}
  
  ngOnInit() {
    // Expandir si algún hijo está activo
    if (this.item.children) {
      this.expanded = this.hasActiveChild();
    }
  }
  
  toggleExpanded() {
    this.expanded = !this.expanded;
  }
  
  isActive(): boolean {
    if (this.item.route) {
      return this.router.isActive(this.item.route, false);
    }
    return this.hasActiveChild();
  }
  
  private hasActiveChild(): boolean {
    if (!this.item.children) return false;
    
    return this.item.children.some(child => {
      if (child.route && this.router.isActive(child.route, false)) {
        return true;
      }
      if (child.children) {
        return this.checkChildrenActive(child.children);
      }
      return false;
    });
  }
  
  private checkChildrenActive(children: MenuItem[]): boolean {
    return children.some(child => {
      if (child.route && this.router.isActive(child.route, false)) {
        return true;
      }
      if (child.children) {
        return this.checkChildrenActive(child.children);
      }
      return false;
    });
  }
}
```

### Module: Dynamic Menu Module

```typescript
@NgModule({
  declarations: [
    DynamicMenuComponent,
    MenuItemComponent
  ],
  imports: [
    CommonModule,
    MatListModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    RouterModule
  ],
  exports: [
    DynamicMenuComponent
  ],
  providers: [
    MenuService
  ]
})
export class DynamicMenuModule {}
```

### Integración en Layout Principal

```typescript
@Component({
  selector: 'tb-main-layout',
  template: `
    <mat-sidenav-container>
      <mat-sidenav mode="side" opened>
        <div class="sidenav-header">
          <img src="assets/logo.png" alt="Nexus">
          <h2>Nexus Platform</h2>
        </div>
        
        <!-- Menú Dinámico -->
        <tb-dynamic-menu></tb-dynamic-menu>
      </mat-sidenav>
      
      <mat-sidenav-content>
        <router-outlet></router-outlet>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `
})
export class MainLayoutComponent {}
```

## Actualización del Menú en Runtime

### Cuando se Activa un Módulo

```typescript
@Component({...})
export class ModuleManagerComponent {
  
  constructor(
    private moduleService: ModuleService,
    private menuService: MenuService
  ) {}
  
  activateModule(module: NexusModule) {
    this.moduleService.activateModule({...}).subscribe(() => {
      // Recargar menú para reflejar nuevo módulo
      this.menuService.reloadMenu();
      
      this.snackBar.open('Módulo activado. Menú actualizado.', 'Cerrar');
    });
  }
}
```

### Cuando Cambian Permisos

```typescript
@Injectable()
export class PermissionChangeListener {
  
  constructor(
    private menuService: MenuService,
    private websocketService: WebSocketService
  ) {
    // Escuchar cambios de permisos via WebSocket
    this.websocketService.subscribe('/user/queue/permissions', () => {
      this.menuService.reloadMenu();
    });
  }
}
```

## Caché y Optimización

### MenuService con Caché

```typescript
@Injectable({
  providedIn: 'root'
})
export class MenuService {
  
  private readonly CACHE_KEY = 'nexus_menu_cache';
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5 minutos
  
  loadMenu(): void {
    // Intentar cargar desde caché
    const cached = this.loadFromCache();
    if (cached) {
      this.menuStructure$.next(cached);
      return;
    }
    
    // Cargar desde servidor
    this.loading$.next(true);
    
    this.http.get<MenuStructure>('/api/nexus/menu').subscribe(menu => {
      this.menuStructure$.next(menu);
      this.saveToCache(menu);
      this.loading$.next(false);
    });
  }
  
  private loadFromCache(): MenuStructure | null {
    try {
      const cached = localStorage.getItem(this.CACHE_KEY);
      if (!cached) return null;
      
      const data = JSON.parse(cached);
      const timestamp = data.timestamp;
      
      if (Date.now() - timestamp > this.CACHE_DURATION) {
        localStorage.removeItem(this.CACHE_KEY);
        return null;
      }
      
      return data.menu;
    } catch (e) {
      return null;
    }
  }
  
  private saveToCache(menu: MenuStructure): void {
    try {
      const data = {
        menu: menu,
        timestamp: Date.now()
      };
      localStorage.setItem(this.CACHE_KEY, JSON.stringify(data));
    } catch (e) {
      // Ignorar errores de storage
    }
  }
}
```

## Testing

### Unit Test: MenuBuilderService

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class MenuBuilderServiceTest {
    
    @Autowired
    private MenuBuilderService menuBuilderService;
    
    @MockBean
    private ModuleManagementService moduleService;
    
    @Test
    public void testBuildMenuForTenantAdmin() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        Set<String> permissions = Set.of("CT_UNITS_VIEW", "CT_REELS_VIEW");
        when(moduleService.getUserModulePermissions(userId, tenantId))
            .thenReturn(permissions);
        
        // Act
        MenuStructure menu = menuBuilderService.buildMenuForUser(
            userId, tenantId, Authority.TENANT_ADMIN);
        
        // Assert
        assertNotNull(menu);
        assertFalse(menu.getCoreMenus().isEmpty());
        assertTrue(menu.getCoreMenus().stream()
            .anyMatch(m -> m.getKey().equals("home")));
    }
}
```

## Ventajas del Sistema

1. **Flexibilidad**: Menús se adaptan automáticamente a módulos activos
2. **Seguridad**: Solo muestra opciones con permisos adecuados
3. **Usabilidad**: Interfaz limpia sin opciones innecesarias
4. **Mantenibilidad**: Nuevos módulos agregan menús sin modificar código
5. **Performance**: Caché optimiza carga de menús

## Próximos Pasos

1. Implementar backend completo
2. Desarrollar componentes Angular
3. Integrar con sistema de módulos
4. Agregar badges dinámicos (notificaciones)
5. Implementar búsqueda en menú
6. Pruebas de integración
