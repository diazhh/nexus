# Guía de Implementación - Módulo Coiled Tubing

## Visión General

Esta guía proporciona instrucciones paso a paso para implementar el módulo de Coiled Tubing en la plataforma Nexus IoT, siguiendo la arquitectura modular documentada.

---

## ⚠️ METODOLOGÍA Y CONVENCIONES CRÍTICAS

> **IMPORTANTE**: Consultar `@/home/diazhh/dev/nexus/dev/METODOLOGIA_DESARROLLO_MODULOS.md` para guía completa sobre desarrollo de módulos en ThingsBoard.

### Reglas de Oro para Evitar Errores de Compilación

#### 1. ✅ Servicios SIEMPRE Retornan DTOs
```java
// ❌ INCORRECTO - Causa errores en controllers
@Transactional
public void assignReel(UUID unitId, UUID reelId) {
    repository.save(entity);
}

// ✅ CORRECTO - Controller puede devolver respuesta
@Transactional
public CTUnitDto assignReel(UUID unitId, UUID reelId) {
    CTUnit saved = repository.save(entity);
    return CTUnitDto.fromEntity(saved);
}
```

#### 2. ✅ NO Duplicar Métodos
```java
// ❌ ERROR DE COMPILACIÓN
public void assignReel(UUID a, UUID b) { }
public ReelDto assignReel(UUID a, UUID b) { }  // Mismo nombre y parámetros!

// ✅ CORRECTO - Un solo método
public ReelDto assignReel(UUID a, UUID b) { }
```

#### 3. ✅ @Slf4j es Suficiente - NO Declarar Logger Manual
```java
// ❌ INCORRECTO - Causa warning "Field 'log' already exists"
@Slf4j
public class MiService {
    private static final Logger log = LoggerFactory.getLogger(MiService.class);
}

// ✅ CORRECTO - @Slf4j ya provee 'log'
@Slf4j
public class MiService {
    // Ya tienes acceso a 'log' automáticamente
    public void miMetodo() {
        log.info("Mi mensaje");
    }
}
```

#### 4. ✅ Usar @RequiredArgsConstructor para Inyección
```java
// ✅ CORRECTO
@Service
@RequiredArgsConstructor
@Slf4j
public class CTUnitService {
    private final CTUnitRepository repository;  // Inyectado automáticamente
    // No necesitas @Autowired
}
```

#### 5. ✅ Usar @Transactional Apropiadamente
```java
// Lectura
@Transactional(readOnly = true)
public CTUnitDto getById(UUID id) { }

// Escritura (create, update, delete)
@Transactional
public CTUnitDto create(CTUnit unit) { }
```

---

## Pre-requisitos

### Infraestructura Base Requerida

✅ **Sistemas Core de Nexus**:
- Sistema de Roles y Usuarios (implementado)
- Sistema de Plantillas (Templates System)
- Sistema de Mapeo de Datos (Data Mapping System)
- Sistema de Gestión de Módulos (Module Management System)
- Sistema de Menús Dinámicos (Dynamic Menu System)

### Tecnologías

- **Backend**: Java 17+, Spring Boot 3.x
- **Frontend**: Angular 18+
- **Base de Datos**: PostgreSQL 14+
- **Timeseries**: Cassandra o PostgreSQL Timescale
- **Cache**: Redis
- **Rule Engine**: ThingsBoard Rule Engine
- **Build**: Maven 3.9+

## Estado de Implementación

- ✅ **Fase 0**: Preparación (COMPLETADA)
- ✅ **Fase 1**: Backend Core (COMPLETADA)
- ✅ **Fase 2**: Rule Engine & Fatiga (COMPLETADA)
  - ✅ Nodos personalizados implementados
  - ✅ CTFatigueCalculationNode completo
  - ✅ CTJobSimulationNode completo
  - ✅ Servicios de fatiga y simulación
  - ✅ Migraciones de base de datos
  - ✅ Datos de ejemplo
- ✅ **Fase 3**: Frontend Components (100% COMPLETADA)
- ✅ **Fase 4**: Dashboards (100% COMPLETADA)
- ✅ **Fase 5**: Sistema de Reportes (100% COMPLETADA)
- ⏳ **Fase 6**: Testing & QA (PENDIENTE)
- ⏳ **Fase 7**: Integración SCADA (PENDIENTE)
- ⏳ **Fase 8**: Deployment (PENDIENTE)

## Roadmap de Implementación

### Fase 0: Preparación (Semana 1)

**Objetivo**: Configurar ambiente y estructura del proyecto

#### 0.1 Setup de Proyecto

```bash
# Crear estructura de directorios del módulo
mkdir -p common/ct-module/src/main/java/org/thingsboard/nexus/ct
mkdir -p common/ct-module/src/main/resources
mkdir -p ui-ngx/src/app/modules/ct

# Estructura Java
cd common/ct-module/src/main/java/org/thingsboard/nexus/ct
mkdir -p {controller,service,repository,model,dto,config}
```

#### 0.2 Configuración de Base de Datos

```sql
-- Ejecutar migraciones
-- /dev/roadmaps/coiled-tubing/database/migrations/

-- V1__initial_ct_schema.sql
-- Crear todas las tablas principales

-- V2__add_ct_indexes.sql
-- Agregar índices

-- V3__add_ct_views_functions.sql
-- Crear vistas y funciones
```

#### 0.3 Configuración de Módulo en Nexus

```sql
-- Registrar módulo en nx_modules
INSERT INTO nx_modules (
    id, module_key, module_name, version, category, 
    requires_license, capabilities, is_available
) VALUES (
    uuid_generate_v4(),
    'coiled-tubing',
    'Coiled Tubing Operations',
    '1.0.0',
    'OPERATIONS',
    true,
    '["real_time_monitoring", "fatigue_calculation", "job_simulation", "fleet_management"]',
    true
);
```

**Entregables**:
- ✅ Estructura de proyecto creada
- ✅ Base de datos migrada
- ✅ Módulo registrado en Nexus

---

### Fase 1: Backend Core (Semanas 2-4)

**Objetivo**: Implementar servicios backend fundamentales

#### 1.1 Modelos y Entidades JPA (Semana 2)

**Archivo**: `common/ct-module/src/main/java/org/thingsboard/nexus/ct/model/`

```java
// CTUnit.java
@Entity
@Table(name = "ct_units")
@Data
@EqualsAndHashCode(callSuper = true)
public class CTUnit extends BaseEntity {
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "unit_code", unique = true, nullable = false)
    private String unitCode;
    
    @Column(name = "unit_name", nullable = false)
    private String unitName;
    
    @Column(name = "asset_id", nullable = false)
    private UUID assetId;
    
    @Column(name = "manufacturer")
    private String manufacturer;
    
    @Column(name = "model")
    private String model;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false)
    private UnitStatus operationalStatus;
    
    @Column(name = "current_location")
    private String currentLocation;
    
    @Column(name = "total_operational_hours")
    private Double totalOperationalHours;
    
    @Column(name = "current_reel_id")
    private UUID currentReelId;
    
    // Getters, setters, constructors
}

// Crear entidades para:
// - CTReel.java
// - CTJob.java
// - CTJobPhase.java
// - CTJobEvent.java
// - CTFatigueLog.java
// - CTMaintenance.java
// - CTInspection.java
// - CTBHAConfiguration.java
// - CTPersonnelAssignment.java
```

#### 1.2 Repositorios (Semana 2)

**Archivo**: `common/ct-module/src/main/java/org/thingsboard/nexus/ct/repository/`

```java
// CTUnitRepository.java
@Repository
public interface CTUnitRepository extends JpaRepository<CTUnit, UUID> {
    
    Optional<CTUnit> findByUnitCode(String unitCode);
    
    List<CTUnit> findByTenantId(UUID tenantId);
    
    List<CTUnit> findByOperationalStatus(UnitStatus status);
    
    @Query("SELECT u FROM CTUnit u WHERE u.tenantId = :tenantId " +
           "AND u.operationalStatus IN :statuses")
    List<CTUnit> findByTenantIdAndStatuses(
        @Param("tenantId") UUID tenantId,
        @Param("statuses") List<UnitStatus> statuses
    );
    
    @Query("SELECT u FROM CTUnit u " +
           "LEFT JOIN FETCH u.currentReel " +
           "WHERE u.id = :id")
    Optional<CTUnit> findByIdWithReel(@Param("id") UUID id);
}

// Crear repositorios para todas las entidades
```

#### 1.3 DTOs (Semana 2)

**Archivo**: `common/ct-module/src/main/java/org/thingsboard/nexus/ct/dto/`

```java
// CTUnitDto.java
@Data
@Builder
public class CTUnitDto {
    private UUID id;
    private String unitCode;
    private String unitName;
    private UnitStatus operationalStatus;
    private String currentLocation;
    private Double totalOperationalHours;
    private Integer totalJobsCompleted;
    private CTReelSummaryDto currentReel;
    private MaintenanceInfoDto maintenance;
    
    public static CTUnitDto fromEntity(CTUnit entity) {
        // Conversión
    }
}

// Crear DTOs para todas las entidades
```

#### 1.4 Servicios de Negocio (Semanas 3-4)

**Archivo**: `common/ct-module/src/main/java/org/thingsboard/nexus/ct/service/`

```java
// CTUnitService.java
@Service
@Slf4j
public class CTUnitService {
    
    @Autowired
    private CTUnitRepository unitRepository;
    
    @Autowired
    private TemplateService templateService;
    
    @Autowired
    private AssetService assetService;
    
    @Transactional
    public CTUnitDto createFromTemplate(UUID tenantId, CreateUnitRequest request) {
        log.info("Creating CT Unit from template: {}", request.getTemplateId());
        
        // 1. Validar template existe
        Template template = templateService.getById(request.getTemplateId());
        
        // 2. Instanciar template (crea assets en TB)
        TemplateInstanceResult instance = templateService.instantiate(
            template, 
            buildTemplateVariables(request)
        );
        
        // 3. Crear registro en ct_units
        CTUnit unit = CTUnit.builder()
            .tenantId(tenantId)
            .unitCode(request.getUnitCode())
            .unitName(request.getUnitName())
            .assetId(instance.getRootAssetId())
            .manufacturer(request.getManufacturer())
            .model(request.getModel())
            .operationalStatus(UnitStatus.STANDBY)
            .currentLocation(request.getLocation())
            .totalOperationalHours(0.0)
            .build();
        
        unit = unitRepository.save(unit);
        
        log.info("CT Unit created: {}", unit.getUnitCode());
        
        return CTUnitDto.fromEntity(unit);
    }
    
    public CTUnitDto getById(UUID id) {
        CTUnit unit = unitRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));
        return CTUnitDto.fromEntity(unit);
    }
    
    public List<CTUnitDto> getByTenant(UUID tenantId, UnitFilters filters) {
        // Implementar lógica de filtrado
    }
    
    @Transactional
    public void assignReel(UUID unitId, UUID reelId) {
        log.info("Assigning reel {} to unit {}", reelId, unitId);
        
        CTUnit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new EntityNotFoundException("Unit not found"));
        
        CTReel reel = reelService.getById(reelId);
        
        // Validaciones
        if (reel.getStatus() != ReelStatus.AVAILABLE) {
            throw new BusinessException("Reel is not available");
        }
        
        if (unit.getCurrentReelId() != null) {
            throw new BusinessException("Unit already has a reel assigned");
        }
        
        // Asignar
        unit.setCurrentReelId(reelId);
        unit.setReelCoupledDate(System.currentTimeMillis());
        unitRepository.save(unit);
        
        reel.setStatus(ReelStatus.IN_USE);
        reel.setCurrentUnitId(unitId);
        reelService.update(reel);
        
        // Crear relación en TB entre assets
        assetService.createRelation(
            unit.getAssetId(),
            reel.getAssetId(),
            "Uses"
        );
        
        log.info("Reel assigned successfully");
    }
    
    @Transactional
    public void detachReel(UUID unitId) {
        // Implementar lógica de desacople
    }
}

// Implementar servicios:
// - CTReelService
// - CTJobService
// - CTFatigueCalculationService
// - CTSimulationService
// - CTMaintenanceService
// - CTReportService
```

#### 1.5 Controllers REST (Semana 4)

**Archivo**: `common/ct-module/src/main/java/org/thingsboard/nexus/ct/controller/`

```java
// CTUnitController.java
@RestController
@RequestMapping("/api/nexus/ct/units")
@RequiredArgsConstructor
@Slf4j
public class CTUnitController {
    
    private final CTUnitService unitService;
    
    @GetMapping
    @PreAuthorize("hasAuthority('CT_VIEW_UNITS')")
    public ResponseEntity<PageData<CTUnitDto>> getUnits(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String location,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        UUID tenantId = getCurrentUser().getTenantId().getId();
        
        UnitFilters filters = UnitFilters.builder()
            .status(status)
            .location(location)
            .build();
        
        PageData<CTUnitDto> units = unitService.getByTenant(
            tenantId, filters, page, pageSize
        );
        
        return ResponseEntity.ok(units);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CT_VIEW_UNITS')")
    public ResponseEntity<CTUnitDto> getUnit(@PathVariable UUID id) {
        CTUnitDto unit = unitService.getById(id);
        return ResponseEntity.ok(unit);
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('CT_MANAGE_UNITS')")
    public ResponseEntity<CTUnitDto> createUnit(
        @RequestBody @Valid CreateUnitRequest request
    ) {
        UUID tenantId = getCurrentUser().getTenantId().getId();
        CTUnitDto unit = unitService.createFromTemplate(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(unit);
    }
    
    @PostMapping("/{id}/assign-reel")
    @PreAuthorize("hasAuthority('CT_MANAGE_UNITS')")
    public ResponseEntity<MessageResponse> assignReel(
        @PathVariable UUID id,
        @RequestBody AssignReelRequest request
    ) {
        unitService.assignReel(id, request.getReelId());
        return ResponseEntity.ok(new MessageResponse("Reel assigned successfully"));
    }
}

// Crear controllers para todas las entidades
```

**Entregables Fase 1**:
- ✅ Todas las entidades JPA (CTUnit, CTReel, CTJob)
- ✅ Todos los repositorios (3 repositorios)
- ✅ Todos los servicios de negocio (3 servicios)
- ✅ Todos los REST controllers (3 controllers)
- ✅ Validaciones y manejo de errores
- ⏳ Tests unitarios (cobertura > 80%) - Pendiente

---

### Fase 2: Rule Engine & Fatiga (Semana 5)

**Objetivo**: Implementar cálculo de fatiga en tiempo real

#### 2.1 Rule Chain: Fatigue Calculation

**Archivo**: `templates/rule-chains/ct-fatigue-calculation.json`

```json
{
  "name": "CT Fatigue Calculation",
  "type": "CORE",
  "configuration": {
    "nodes": [
      {
        "id": "input",
        "type": "RULE_CHAIN_INPUT",
        "name": "Input",
        "configuration": {}
      },
      {
        "id": "filter-reel-telemetry",
        "type": "SCRIPT_FILTER",
        "name": "Filter Reel Telemetry",
        "configuration": {
          "jsScript": "return metadata.entityType === 'ASSET' && metadata.assetType === 'CT_REEL';"
        }
      },
      {
        "id": "calculate-fatigue",
        "type": "SCRIPT_TRANSFORM",
        "name": "Calculate Fatigue",
        "configuration": {
          "jsScript": "<!-- Script completo del cálculo de fatiga -->"
        }
      },
      {
        "id": "save-fatigue-log",
        "type": "REST_API_CALL",
        "name": "Save Fatigue Log",
        "configuration": {
          "restEndpointUrl": "http://localhost:8080/api/nexus/ct/fatigue/log",
          "requestMethod": "POST"
        }
      },
      {
        "id": "update-reel-attributes",
        "type": "SAVE_ATTRIBUTES",
        "name": "Update Reel Attributes",
        "configuration": {
          "scope": "SERVER_SCOPE"
        }
      },
      {
        "id": "check-alarm-threshold",
        "type": "SCRIPT_SWITCH",
        "name": "Check Alarm Threshold",
        "configuration": {
          "jsScript": "if (msg.fatigue_percent >= 95) return ['critical']; else if (msg.fatigue_percent >= 80) return ['high']; else return ['normal'];"
        }
      },
      {
        "id": "create-alarm-critical",
        "type": "CREATE_ALARM",
        "name": "Create Critical Alarm",
        "configuration": {
          "alarmType": "CT_FATIGUE_CRITICAL",
          "severity": "CRITICAL"
        }
      },
      {
        "id": "create-alarm-high",
        "type": "CREATE_ALARM",
        "name": "Create High Alarm",
        "configuration": {
          "alarmType": "CT_FATIGUE_HIGH",
          "severity": "MAJOR"
        }
      },
      {
        "id": "clear-alarms",
        "type": "CLEAR_ALARM",
        "name": "Clear Alarms",
        "configuration": {
          "alarmType": "CT_FATIGUE_*"
        }
      }
    ],
    "connections": [
      {"from": "input", "to": "filter-reel-telemetry", "type": "Success"},
      {"from": "filter-reel-telemetry", "to": "calculate-fatigue", "type": "True"},
      {"from": "calculate-fatigue", "to": "save-fatigue-log", "type": "Success"},
      {"from": "calculate-fatigue", "to": "update-reel-attributes", "type": "Success"},
      {"from": "update-reel-attributes", "to": "check-alarm-threshold", "type": "Success"},
      {"from": "check-alarm-threshold", "to": "create-alarm-critical", "type": "critical"},
      {"from": "check-alarm-threshold", "to": "create-alarm-high", "type": "high"},
      {"from": "check-alarm-threshold", "to": "clear-alarms", "type": "normal"}
    ]
  }
}
```

#### 2.2 Servicio de Fatiga Backend

```java
@Service
public class CTFatigueService {
    
    @Async
    public void logFatigueCalculation(FatigueLogDto log) {
        // Guardar en ct_fatigue_log
        // Actualizar ct_reels.accumulated_fatigue_percent
    }
    
    public FatigueHistoryDto getFatigueHistory(UUID reelId, Long fromDate, Long toDate) {
        // Consultar histórico
    }
}
```

**Entregables Fase 2**: ✅ COMPLETADA
- ✅ Entidad CTFatigueLog creada (123 líneas)
- ✅ Repositorio CTFatigueLogRepository implementado
- ✅ DTO CTFatigueLogDto con conversión
- ✅ Servicio CTFatigueService con métodos async
- ✅ Controller CTFatigueController con 7 endpoints REST
- ✅ Nodo personalizado CTFatigueCalculationNode (450+ líneas)
- ✅ Nodo personalizado CTJobSimulationNode (600+ líneas)
- ✅ Servicio CTSimulationService implementado
- ✅ Controller CTSimulationController con 2 endpoints
- ✅ Configuración del módulo (CTModuleConfiguration)
- ✅ application-ct.yml con variables de entorno
- ✅ Migraciones SQL (V1 schema + V2 seed data)
- ✅ Documentación completa de nodos personalizados
- ⏳ Validación con datos de prueba - Pendiente
- ⏳ Tests unitarios - Pendiente

---

### Fase 3: Frontend - Components Core (Semanas 6-7) - ✅ 100% COMPLETADO

**Objetivo**: Implementar componentes Angular principales e integrar con menú principal

**Estado Actual**:
- ✅ Modelos TypeScript (4 archivos)
- ✅ Servicios HTTP Angular (4 archivos)
- ✅ Componentes de Lista (3 componentes × 3 archivos = 9 archivos)
- ✅ Diálogo de Simulación (3 archivos)
- ✅ Diálogo de Historial de Fatiga (3 archivos)
- ✅ Módulo CT con routing actualizado (2 archivos)
- ✅ Componentes de Detalle (3 componentes × 3 archivos = 9 archivos)
  - CTUnitDetailsComponent (125 líneas TS + 220 HTML + 280 SCSS)
  - CTReelDetailsComponent (135 líneas TS + 260 HTML + 240 SCSS)
  - CTJobDetailsComponent (190 líneas TS + 280 HTML + 230 SCSS)
- ✅ Diálogos CRUD (3 componentes × 3 archivos = 9 archivos)
  - CTUnitFormDialogComponent (175 líneas TS + 185 HTML + 105 SCSS)
  - CTReelFormDialogComponent (165 líneas TS + 175 HTML + 105 SCSS)
  - CTJobFormDialogComponent (185 líneas TS + 195 HTML + 105 SCSS)
- ✅ Módulo CT actualizado con 11 componentes totales
- ✅ **Integración con Menú Principal** ← COMPLETADO
  - MenuId enum actualizado (4 nuevas entradas)
  - menuSectionMap configurado con iconos y rutas
  - Menú agregado para SYS_ADMIN y TENANT_ADMIN
  - Routing actualizado con autorización y breadcrumbs
  - CTModule integrado en home-pages.module.ts
  - 52 traducciones agregadas en locale.constant-en_US.json

#### 3.1 Módulo CT Angular

**Archivo**: `ui-ngx/src/app/modules/ct/ct.module.ts`

```typescript
@NgModule({
  declarations: [
    CTUnitsComponent,
    CTUnitDetailsComponent,
    CTReelsComponent,
    CTReelDetailsComponent,
    CTJobsComponent,
    CTJobDetailsComponent,
    CTDashboardComponent,
    // ... otros componentes
  ],
  imports: [
    CommonModule,
    CTRoutingModule,
    SharedModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule
  ]
})
export class CTModule { }
```

#### 3.2 Servicios Angular

**Archivo**: `ui-ngx/src/app/modules/ct/services/ct-unit.service.ts`

```typescript
@Injectable({
  providedIn: 'root'
})
export class CTUnitService {
  
  private baseUrl = '/api/nexus/ct/units';
  
  constructor(private http: HttpClient) {}
  
  getUnits(params?: CTUnitQueryParams): Observable<PageData<CTUnit>> {
    return this.http.get<PageData<CTUnit>>(this.baseUrl, { params });
  }
  
  getUnit(id: string): Observable<CTUnit> {
    return this.http.get<CTUnit>(`${this.baseUrl}/${id}`);
  }
  
  createUnit(request: CreateUnitRequest): Observable<CTUnit> {
    return this.http.post<CTUnit>(this.baseUrl, request);
  }
  
  assignReel(unitId: string, reelId: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/${unitId}/assign-reel`, { reelId });
  }
}

// ✅ COMPLETADO:
// - CTUnitService (8 métodos)
// - CTReelService (7 métodos)
// - CTJobService (10 métodos)
// - CTSimulationService (2 métodos)
```

#### 3.3 Componentes de Lista ✅ COMPLETADO

**Archivos**: 
- `ui-ngx/src/app/modules/home/pages/ct/ct-units-list.component.ts` (175 líneas)
- `ui-ngx/src/app/modules/home/pages/ct/ct-reels-list.component.ts` (180 líneas)
- `ui-ngx/src/app/modules/home/pages/ct/ct-jobs-list.component.ts` (220 líneas)

**Características Implementadas**:
- Tablas con Material Design y paginación
- Filtros por estado y búsqueda por texto
- Acciones contextuales por fila
- Indicadores visuales con colores
- Navegación a detalles
- Integración con diálogos de simulación y fatiga
- Estados de carga y error
- Responsive design

**Ejemplo de implementación**:

```typescript
@Component({
  selector: 'tb-ct-units',
  templateUrl: './ct-units.component.html',
  styleUrls: ['./ct-units.component.scss']
})
export class CTUnitsComponent implements OnInit {
  
  units$: Observable<PageData<CTUnit>>;
  displayedColumns = ['unitCode', 'status', 'location', 'hours', 'reel', 'actions'];
  
  constructor(
    private unitService: CTUnitService,
    private dialog: MatDialog
  ) {}
  
  ngOnInit() {
    this.loadUnits();
  }
  
  loadUnits() {
    this.units$ = this.unitService.getUnits();
  }
  
  createUnit() {
    this.dialog.open(CreateUnitDialogComponent, {
      width: '600px'
    }).afterClosed().subscribe(result => {
      if (result) {
        this.loadUnits();
      }
    });
  }
  
  viewDetails(unit: CTUnit) {
    this.router.navigate(['/ct/units', unit.id]);
  }
}
```

**Template**: `ct-units.component.html`

```html
<div class="ct-units-container">
  <mat-toolbar>
    <span>CT Units</span>
    <span class="spacer"></span>
    <button mat-raised-button color="primary" (click)="createUnit()">
      <mat-icon>add</mat-icon>
      New Unit
    </button>
  </mat-toolbar>
  
  <mat-table [dataSource]="units$ | async" class="units-table">
    <ng-container matColumnDef="unitCode">
      <mat-header-cell *matHeaderCellDef>Unit Code</mat-header-cell>
      <mat-cell *matCellDef="let unit">{{unit.unitCode}}</mat-cell>
    </ng-container>
    
    <ng-container matColumnDef="status">
      <mat-header-cell *matHeaderCellDef>Status</mat-header-cell>
      <mat-cell *matCellDef="let unit">
        <ct-status-badge [status]="unit.operationalStatus"></ct-status-badge>
      </mat-cell>
    </ng-container>
    
    <!-- Más columnas -->
    
    <mat-header-row *matHeaderRowDef="displayedColumns"></mat-header-row>
    <mat-row *matRowDef="let row; columns: displayedColumns;"
             (click)="viewDetails(row)"></mat-row>
  </mat-table>
</div>
```

#### 3.4 Componentes de Detalle ✅ COMPLETADO

**Archivos**: 
- `ui-ngx/src/app/modules/home/pages/ct/ct-unit-details.component.ts` (125 líneas)
- `ui-ngx/src/app/modules/home/pages/ct/ct-reel-details.component.ts` (135 líneas)
- `ui-ngx/src/app/modules/home/pages/ct/ct-job-details.component.ts` (190 líneas)

**Características Implementadas**:
- Vistas detalladas completas para Units, Reels y Jobs
- Tarjetas de resumen con métricas clave (4 cards por componente)
- Grid de información con múltiples secciones
- Navegación bidireccional (lista ↔ detalle)
- Acciones contextuales según estado
- Integración con diálogos existentes
- Estados de carga y error
- Responsive design con grid adaptativo

**Funcionalidades Específicas**:
- **Units**: Assign/Detach Reel, Recent Jobs
- **Reels**: Fatigue visualization, History dialog, Retire action
- **Jobs**: Progress tracking, Conditional actions (Start/Complete/Cancel), Simulation

**Entregables Fase 3**:
- ✅ Modelos TypeScript (4 archivos)
- ✅ Servicios HTTP (4 archivos)
- ✅ Componentes de lista (Units, Reels, Jobs) - 9 archivos
- ✅ Diálogo de simulación (3 archivos)
- ✅ Diálogo de historial de fatiga (3 archivos)
- ✅ Módulo CT con routing actualizado (2 archivos)
- ✅ **Componentes de detalle (Units, Reels, Jobs) - 9 archivos** ← NUEVO
- ⏳ Formularios de creación/edición (0/3)
- ⏳ Integración con menú principal (0%)

**Total Archivos Creados**: 34 archivos (~6,900 líneas de código)

---

### Fase 4: Dashboards (Semana 8) - ✅ 100% COMPLETADO

**Objetivo**: Implementar dashboards operacionales propios del módulo CT

**Estado Actual**: ✅ COMPLETADO - 3 dashboards Angular implementados

#### 4.1 Real-Time Operations Dashboard ✅

**Archivo**: `ui-ngx/src/app/modules/home/pages/ct/ct-realtime-dashboard.component.{ts,html,scss}`

**Características Implementadas**:
- ✅ Métricas en tiempo real (Active Jobs, Active Units, Total Depth, Critical Alarms)
- ✅ Tabla de jobs activos con progreso
- ✅ Auto-refresh cada 5 segundos
- ✅ Navegación a detalles de jobs
- ✅ Indicadores visuales con colores dinámicos
- ✅ Barras de progreso por job

#### 4.2 Fleet Management Dashboard ✅

**Archivo**: `ui-ngx/src/app/modules/home/pages/ct/ct-fleet-dashboard.component.{ts,html,scss}`

**Características Implementadas**:
- ✅ Métricas de flota (Total Units, Operational, Maintenance, Utilization Rate)
- ✅ Tabla completa de estado de unidades
- ✅ Filtros por estado operacional
- ✅ Indicadores de utilización con colores
- ✅ Navegación a detalles de unidades

#### 4.3 Analytics Dashboard ✅

**Archivo**: `ui-ngx/src/app/modules/home/pages/ct/ct-analytics-dashboard.component.{ts,html,scss}`

**Características Implementadas**:
- ✅ Métricas analíticas (Total Jobs, Completed, Avg Duration, Success Rate)
- ✅ Distribución de jobs por tipo con gráfico de barras
- ✅ Tabla de jobs recientes
- ✅ Cálculo automático de KPIs
- ✅ Estadísticas de performance

**Entregables Fase 4**:
- ✅ 3 dashboards operacionales (9 archivos: 3 TS + 3 HTML + 3 SCSS)
- ✅ Componentes Angular con auto-refresh
- ✅ Integración con servicios HTTP del módulo CT
- ✅ Routing configurado (3 rutas nuevas)
- ✅ MenuId agregados (4 nuevos)
- ✅ Responsive design con Material Design
- ✅ ~1,200 líneas de código

**Arquitectura**:
- Dashboards propios del módulo (no dashboards de ThingsBoard)
- Uso de infraestructura ThingsBoard para suscripciones
- Componentes standalone con auto-refresh
- Integración completa con módulo CT existente

---

### Fase 5: Sistema de Reportes (Semanas 9-10) - ✅ 100% COMPLETADO

**Objetivo**: Implementar sistema completo de generación de reportes operacionales

**Estado Actual**: ✅ COMPLETADO - 5 tipos de reportes implementados

#### 5.1 Simulador de Trabajos ✅

**Archivo**: `ui-ngx/src/app/modules/home/pages/ct/ct-job-simulation-dialog.component.{ts,html,scss}`

**Estado**: ✅ Ya implementado en Fase 3
- Simulación interactiva de trabajos
- Configuración de parámetros operacionales
- Visualización de resultados

#### 5.2 Sistema de Reportes ✅

**Backend Implementado**:
- `CTReportService.java` (450 líneas) - Servicio de generación
- `CTReportController.java` (115 líneas) - 6 endpoints REST
- `CTReportRequest.java` - DTO de request
- `CTReportResponse.java` - DTO de response

**Frontend Implementado**:
- `ct-report.service.ts` (95 líneas) - Servicio Angular
- `ct-reports.component.{ts,html,scss}` (230 líneas) - Componente UI

**Tipos de Reportes Implementados**:

1. **Job Summary Report**
   - Resumen completo de trabajos
   - Campos: número, tipo, pozo, estado, prioridad, fechas, duración
   - Formato: CSV, TXT

2. **Reel Lifecycle Report**
   - Ciclo de vida de reels
   - Campos: código, estado, material, dimensiones, fatiga, ciclos
   - Cálculo de vida útil restante

3. **Fleet Utilization Report**
   - Rendimiento de flota
   - Campos: unidades, horas, trabajos completados, utilización
   - Cálculo automático de tasas

4. **Fatigue Analysis Report**
   - Análisis crítico de fatiga
   - Filtro: reels con fatiga >= 80%
   - Recomendaciones automáticas

5. **Maintenance Schedule Report**
   - Programación de mantenimiento
   - Basado en ciclo de 500 horas
   - Priorización automática

**Características Implementadas**:
- ✅ Generación en formato CSV y texto plano
- ✅ 6 endpoints REST con autorización
- ✅ Componente UI con grid responsive
- ✅ Descarga automática de archivos
- ✅ Selector de formato
- ✅ Loading overlay
- ✅ Cálculos automáticos de métricas
- ✅ Integración completa con módulo CT

**Entregables Fase 5**:
- ✅ Simulador funcional (ya implementado)
- ✅ 5 tipos de reportes operacionales
- ✅ Backend completo (servicio + controller + DTOs)
- ✅ Frontend completo (servicio + componente)
- ✅ Integración con routing y menú
- ✅ Formatos CSV y TXT
- ✅ ~930 líneas de código
- ⏳ Dependencia Spring Security pendiente en pom.xml
- ⏳ Tests unitarios pendientes

---

### Fase 6: Testing & QA (Semana 11) - ⏳ EN PROGRESO

**Objetivo**: Implementar suite completa de tests para garantizar calidad y estabilidad del módulo

**Estado Actual**: ⏳ EN PROGRESO - Implementando tests unitarios y de integración

#### 6.1 Tests Unitarios Backend

**Objetivo**: Cobertura > 80% en servicios y controllers

**Tests a Implementar**:

1. **CTUnitServiceTest** - Tests para servicio de unidades
   - Crear unidad desde template
   - Obtener unidad por ID
   - Listar unidades por tenant
   - Asignar/desacoplar reel
   - Actualizar estado operacional
   - Validaciones de negocio

2. **CTReelServiceTest** - Tests para servicio de reels
   - Crear reel desde template
   - Obtener reel por ID
   - Listar reels por tenant
   - Actualizar fatiga
   - Retirar reel
   - Validaciones de ciclo de vida

3. **CTJobServiceTest** - Tests para servicio de trabajos
   - Crear job
   - Iniciar/completar/cancelar job
   - Actualizar progreso
   - Obtener jobs activos
   - Validaciones de estado

4. **CTFatigueServiceTest** - Tests para servicio de fatiga
   - Calcular fatiga
   - Registrar log de fatiga
   - Obtener historial
   - Validaciones de umbrales

5. **CTReportServiceTest** - Tests para servicio de reportes
   - Generar cada tipo de reporte
   - Validar formato CSV/TXT
   - Validar contenido de reportes

**Controllers a Testear**:
- CTUnitController (8 endpoints)
- CTReelController (7 endpoints)
- CTJobController (10 endpoints)
- CTFatigueController (7 endpoints)
- CTReportController (6 endpoints)

#### 6.2 Tests Unitarios Frontend

**Objetivo**: Cobertura > 70% en servicios y componentes Angular

**Tests a Implementar**:

1. **CTUnitService.spec.ts** - Tests para servicio HTTP
   - Verificar llamadas HTTP correctas
   - Manejo de errores
   - Transformación de datos

2. **CTReelService.spec.ts** - Tests para servicio HTTP
   - Verificar endpoints
   - Validar parámetros

3. **CTJobService.spec.ts** - Tests para servicio HTTP
   - Verificar operaciones CRUD
   - Validar filtros

4. **Componentes principales**:
   - CTUnitsListComponent.spec.ts
   - CTReelsListComponent.spec.ts
   - CTJobsListComponent.spec.ts

#### 6.3 Tests de Integración

**Objetivo**: Validar flujo completo de operaciones

**Tests a Implementar**:

1. **API REST Integration Tests**
   - Flujo completo de creación de unidad
   - Flujo de asignación de reel a unidad
   - Flujo de creación y ejecución de job
   - Flujo de cálculo de fatiga

2. **Database Integration Tests**
   - Validar persistencia de datos
   - Validar transacciones
   - Validar constraints e índices

3. **Rule Engine Tests**
   - Validar CTFatigueCalculationNode
   - Validar CTJobSimulationNode
   - Validar flujo de alarmas

#### 6.4 Tests E2E (Opcional)

- Flujos de usuario completos con Cypress/Playwright
- Validación de UI/UX

**Entregables Fase 6**:
- ✅ Tests unitarios backend (cobertura > 80%)
- ✅ Tests unitarios frontend (cobertura > 70%)
- ✅ Tests de integración para APIs REST
- ✅ Tests de integración para base de datos
- ✅ Documentación de estrategia de testing
- ✅ CI/CD pipeline con tests automatizados
- ⏳ Tests E2E (opcional)

---

### Fase 7: Integración SCADA (Semana 12)

#### 7.1 Configurar Data Mapping

Crear configuraciones de mapeo para conectar SCADA a assets.

#### 7.2 Testar Integración

Probar flujo completo de datos desde SCADA → TB → Fatigue Calculation.

**Entregables Fase 7**:
- ✅ Integración SCADA funcional
- ✅ Mapeo de datos configurado

---

### Fase 8: Documentación & Despliegue (Semana 13)

#### 8.1 Documentación Técnica

- API documentation (Swagger)
- Guías de usuario
- Manuales de operación

#### 8.2 Capacitación

- Sesiones de training
- Videos tutoriales
- FAQs

#### 8.3 Despliegue

```bash
# Build backend
mvn clean package -DskipTests

# Build frontend
cd ui-ngx
npm run build:prod

# Deploy
./deploy.sh production
```

**Entregables Fase 8**:
- ✅ Documentación completa
- ✅ Training realizado
- ✅ Módulo en producción

---

## Checklist de Verificación

### Backend
- [ ] Todas las tablas creadas
- [ ] Todas las entidades JPA funcionando
- [ ] Todos los repositorios implementados
- [ ] Todos los servicios implementados
- [ ] Todos los controllers REST implementados
- [ ] Validaciones en todos los endpoints
- [ ] Manejo de errores robusto
- [ ] Tests unitarios > 80% cobertura
- [ ] Tests de integración funcionando

### Rule Engine
- [ ] Fatigue calculation chain implementada
- [ ] Maintenance alerts chain implementada
- [ ] Data validation chain implementada
- [ ] Alarm propagation funcionando
- [ ] Performance validado (< 100ms por cálculo)

### Frontend
- [ ] Todos los componentes implementados
- [ ] Todos los servicios implementados
- [ ] Routing configurado
- [ ] Menús integrados con Dynamic Menu System
- [ ] Permisos aplicados correctamente
- [ ] UI responsive
- [ ] Accesibilidad WCAG 2.1 AA
- [ ] Tests E2E pasando

### Dashboards
- [ ] Real-time operations dashboard
- [ ] Fleet management dashboard
- [ ] Analytics dashboard
- [ ] Reel lifecycle dashboard
- [ ] Job execution dashboard

### Integración
- [ ] SCADA integration configurada
- [ ] Data mapping funcionando
- [ ] Template system integrado
- [ ] Module management integrado
- [ ] Real-time telemetry funcionando

### Documentación
- [ ] API documentation completa
- [ ] User guides escritos
- [ ] Technical documentation actualizada
- [ ] Training materials preparados

### Performance
- [ ] Load testing completado
- [ ] Optimizaciones aplicadas
- [ ] Caching configurado
- [ ] Índices de BD optimizados

### Seguridad
- [ ] Autenticación funcionando
- [ ] Autorización por roles
- [ ] Permisos granulares
- [ ] Auditoría de acciones
- [ ] Datos sensibles encriptados

## Troubleshooting Común

### Problema: Rule Chain no calcula fatiga

**Síntomas**: No se crean registros en `ct_fatigue_log`

**Solución**:
1. Verificar que la rule chain está asignada al root rule chain
2. Verificar que los metadatos incluyen `reelId`
3. Revisar logs del rule engine: `/var/log/thingsboard/thingsboard.log`

### Problema: Frontend no carga datos

**Síntomas**: Lista vacía en componentes

**Solución**:
1. Verificar que el backend está corriendo
2. Verificar permisos del usuario
3. Revisar Network tab en DevTools
4. Verificar que el módulo está activado para el tenant

### Problema: Performance lento en dashboards

**Solución**:
1. Optimizar queries con índices
2. Implementar caching en Redis
3. Reducir frecuencia de polling
4. Usar WebSockets para real-time data

## Soporte

Para ayuda:
- **Documentación**: `/dev/roadmaps/coiled-tubing/`
- **Issues**: Sistema de tickets interno
- **Slack**: #nexus-ct-module

---

**Versión**: 1.0.0  
**Última Actualización**: Enero 2026
