# Análisis de Implementación Actual - Módulo Coiled Tubing

**Fecha**: 26 de Enero, 2026  
**Estado**: Análisis Completo de Componentes Faltantes

---

## 🔍 Resumen Ejecutivo

El módulo CT tiene implementada la **capa de datos básica** (entidades, repositorios, DTOs) pero **FALTA COMPLETAMENTE** el sistema de **Gemelos Digitales** y **Plantillas** que era el requisito fundamental del diseño original.

### Problema Principal Identificado

Los botones de "Crear" en el frontend **no funcionan** porque:
1. ❌ **NO existe integración con ThingsBoard Assets** (gemelos digitales)
2. ❌ **NO existe sistema de plantillas** para crear instancias
3. ❌ **NO hay creación automática de jerarquías de assets**
4. ❌ Los servicios solo guardan registros en tablas SQL sin crear los assets correspondientes

---

## 📊 Estado de Implementación por Componente

### ✅ Componentes Implementados (30%)

#### 1. Base de Datos
- ✅ Tablas SQL creadas (`ct_units`, `ct_reels`, `ct_jobs`, `ct_fatigue_log`)
- ✅ Migraciones ejecutadas correctamente
- ✅ Índices y constraints configurados

#### 2. Backend - Capa de Datos
- ✅ Entidades JPA: `CTUnit`, `CTReel`, `CTJob`, `CTFatigueLog`
- ✅ Repositorios: `CTUnitRepository`, `CTReelRepository`, `CTJobRepository`
- ✅ DTOs: `CTUnitDto`, `CTReelDto`, `CTJobDto`

#### 3. Backend - Servicios Básicos
- ✅ `CTUnitService` - CRUD básico (sin assets)
- ✅ `CTReelService` - CRUD básico (sin assets)
- ✅ `CTJobService` - CRUD básico (sin assets)
- ✅ `CTFatigueService` - Cálculo de fatiga
- ✅ `CTSimulationService` - Simulación de trabajos

#### 4. Backend - Controllers REST
- ✅ `CTUnitController` - Endpoints básicos
- ✅ `CTReelController` - Endpoints básicos
- ✅ `CTJobController` - Endpoints básicos

#### 5. Frontend - Componentes UI
- ✅ Listas: `ct-units-list`, `ct-reels-list`, `ct-jobs-list`
- ✅ Detalles: `ct-unit-details`, `ct-reel-details`, `ct-job-details`
- ✅ Routing configurado
- ✅ Servicios HTTP: `CTUnitService`, `CTReelService`, `CTJobService`

#### 6. Rule Engine
- ✅ `CTFatigueCalculationNode` - Nodo personalizado
- ✅ `CTJobSimulationNode` - Nodo personalizado

---

## ❌ Componentes FALTANTES (70%)

### 🚨 CRÍTICO: Sistema de Gemelos Digitales

#### Problema
El roadmap especifica claramente que **cada unidad CT, reel y componente debe ser un Asset de ThingsBoard** con:
- Atributos compartidos y específicos
- Telemetrías en tiempo real
- Relaciones jerárquicas entre assets
- Dashboards asociados

#### Lo que FALTA

##### 1. **Integración con AssetService de ThingsBoard**
```java
// ❌ NO EXISTE en CTUnitService.java
@Autowired
private AssetService assetService;  // FALTA

@Transactional
public CTUnitDto createFromTemplate(UUID tenantId, CreateUnitRequest request) {
    // 1. Crear asset principal en ThingsBoard
    Asset unitAsset = new Asset();
    unitAsset.setType("CT_UNIT");
    unitAsset.setName(request.getUnitName());
    unitAsset.setTenantId(new TenantId(tenantId));
    unitAsset = assetService.saveAsset(unitAsset);  // FALTA
    
    // 2. Crear sub-assets (hydraulic, injection, control, power, gooseneck)
    // FALTA COMPLETAMENTE
    
    // 3. Crear relaciones entre assets
    // FALTA COMPLETAMENTE
    
    // 4. Configurar atributos iniciales
    // FALTA COMPLETAMENTE
    
    // 5. Guardar registro en ct_units con assetId
    CTUnit unit = new CTUnit();
    unit.setAssetId(unitAsset.getId().getId());  // FALTA
    // ...
}
```

**Estado Actual**: Los servicios solo hacen `repository.save()` sin crear assets.

##### 2. **Sistema de Plantillas (Templates)**

Según el roadmap, debe existir:

```
templates/
├── ct-unit-standard.json          ❌ NO EXISTE
├── ct-unit-heavy-duty.json        ❌ NO EXISTE
├── ct-unit-ultra-heavy.json       ❌ NO EXISTE
├── reel-standard.json             ❌ NO EXISTE
├── reel-large-diameter.json       ❌ NO EXISTE
└── bha-templates/                 ❌ NO EXISTE
    ├── cleaning.json
    ├── drilling.json
    └── cementing.json
```

**Estructura de Plantilla Esperada**:
```json
{
  "templateId": "ct-unit-standard-v1",
  "name": "Standard CT Unit",
  "description": "CT Unit for tubing up to 3.5\" OD",
  "version": "1.0.0",
  "assetHierarchy": {
    "root": {
      "type": "CT_UNIT",
      "name": "{{unit_name}}",
      "attributes": {
        "manufacturer": "{{manufacturer}}",
        "model": "{{model}}",
        "maxPressurePsi": 15000,
        "maxTensionLbf": 40000
      },
      "children": [
        {
          "type": "CT_HYDRAULIC_SYSTEM",
          "name": "{{unit_name}} - Hydraulic System",
          "relation": "Contains",
          "attributes": {
            "pumpCapacityGpm": 120,
            "maxPressurePsi": 5000
          }
        },
        {
          "type": "CT_INJECTION_SYSTEM",
          "name": "{{unit_name}} - Injection Head",
          "relation": "Contains",
          "attributes": {
            "maxSpeedFtMin": 200,
            "gripForce": 40000
          }
        }
      ]
    }
  }
}
```

##### 3. **TemplateService - NO EXISTE**

```java
// ❌ FALTA COMPLETAMENTE
@Service
public class CTTemplateService {
    
    private final AssetService assetService;
    private final RelationService relationService;
    
    /**
     * Instancia una plantilla creando todos los assets y relaciones
     */
    public TemplateInstanceResult instantiate(
        String templateId, 
        Map<String, Object> variables
    ) {
        // 1. Cargar plantilla desde JSON
        // 2. Reemplazar variables
        // 3. Crear asset raíz
        // 4. Crear assets hijos recursivamente
        // 5. Crear relaciones
        // 6. Configurar atributos
        // 7. Retornar IDs de assets creados
    }
    
    public List<CTTemplate> getAvailableTemplates(String category) {
        // Listar plantillas disponibles
    }
}
```

##### 4. **Gestión de Plantillas en Frontend - NO EXISTE**

```typescript
// ❌ FALTA COMPLETAMENTE
// ui-ngx/src/app/modules/home/pages/ct/ct-template-selector-dialog.component.ts

@Component({
  selector: 'ct-template-selector-dialog',
  template: `
    <h2>Select CT Unit Template</h2>
    <mat-radio-group [(ngModel)]="selectedTemplate">
      <mat-radio-button *ngFor="let template of templates" [value]="template">
        <h3>{{template.name}}</h3>
        <p>{{template.description}}</p>
        <ul>
          <li>Max Pressure: {{template.specs.maxPressure}} PSI</li>
          <li>Max Tension: {{template.specs.maxTension}} LBF</li>
        </ul>
      </mat-radio-button>
    </mat-radio-group>
  `
})
export class CTTemplateSelectorDialogComponent {
  templates: CTTemplate[] = [];
  selectedTemplate: CTTemplate;
  
  // FALTA IMPLEMENTACIÓN
}
```

##### 5. **Diálogos de Creación con Plantillas - INCOMPLETOS**

**Estado Actual** (`ct-units-list.component.ts:133`):
```typescript
createUnit() {
  // TODO: Open create unit dialog
  console.log('Create unit dialog');  // ❌ NO HACE NADA
}
```

**Lo que DEBERÍA hacer**:
```typescript
createUnit() {
  // 1. Abrir diálogo de selección de plantilla
  const dialogRef = this.dialog.open(CTTemplateSelectorDialogComponent, {
    data: { category: 'ct-unit' }
  });
  
  dialogRef.afterClosed().subscribe(template => {
    if (template) {
      // 2. Abrir formulario con campos de la plantilla
      this.openCreateUnitForm(template);
    }
  });
}

openCreateUnitForm(template: CTTemplate) {
  const dialogRef = this.dialog.open(CTUnitFormDialogComponent, {
    data: { 
      template: template,
      mode: 'create'
    }
  });
  
  dialogRef.afterClosed().subscribe(result => {
    if (result) {
      // 3. Llamar a createFromTemplate en el backend
      this.unitService.createFromTemplate(result).subscribe({
        next: (unit) => {
          // Asset creado exitosamente
          this.loadUnits();
        }
      });
    }
  });
}
```

##### 6. **Endpoints de Plantillas - NO EXISTEN**

```java
// ❌ FALTA en CTUnitController.java
@GetMapping("/templates")
public ResponseEntity<List<CTTemplateDto>> getAvailableTemplates() {
    // Retornar plantillas disponibles
}

@PostMapping("/from-template")
public ResponseEntity<CTUnitDto> createFromTemplate(
    @RequestBody CreateFromTemplateRequest request
) {
    // request.templateId
    // request.variables (unitCode, unitName, manufacturer, etc.)
    CTUnitDto unit = unitService.createFromTemplate(tenantId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(unit);
}
```

---

### 🔧 Componentes de Integración FALTANTES

#### 7. **Sincronización Bidireccional Asset ↔ Tabla SQL**

**Problema**: Actualmente hay dos "mundos" desconectados:
- Tablas SQL (`ct_units`, `ct_reels`) - Implementado
- Assets de ThingsBoard - NO implementado

**Solución Requerida**:
```java
@Service
public class CTAssetSyncService {
    
    /**
     * Sincroniza cambios de asset a tabla SQL
     */
    @EventListener
    public void onAssetUpdated(AssetUpdatedEvent event) {
        if (event.getAsset().getType().equals("CT_UNIT")) {
            // Actualizar ct_units
        }
    }
    
    /**
     * Sincroniza cambios de tabla SQL a asset
     */
    public void syncUnitToAsset(UUID unitId) {
        CTUnit unit = unitRepository.findById(unitId).get();
        Asset asset = assetService.findAssetById(new AssetId(unit.getAssetId()));
        
        // Actualizar atributos del asset
        asset.setLabel(unit.getUnitName());
        // ...
        assetService.saveAsset(asset);
    }
}
```

#### 8. **Gestión de Relaciones entre Assets**

```java
// ❌ FALTA
@Service
public class CTAssetRelationService {
    
    private final RelationService relationService;
    
    /**
     * Asigna reel a unidad creando relación en ThingsBoard
     */
    public void assignReelToUnit(UUID unitAssetId, UUID reelAssetId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(unitAssetId));
        relation.setTo(new AssetId(reelAssetId));
        relation.setType("Uses");
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        
        relationService.saveRelation(relation);
    }
    
    /**
     * Obtiene jerarquía completa de assets de una unidad
     */
    public CTAssetHierarchy getUnitHierarchy(UUID unitAssetId) {
        // Retornar árbol de assets relacionados
    }
}
```

#### 9. **Configuración de Atributos y Telemetrías**

```java
// ❌ FALTA
@Service
public class CTAttributeService {
    
    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    
    /**
     * Configura atributos iniciales de una unidad
     */
    public void initializeUnitAttributes(UUID assetId, CTUnit unit) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("manufacturer", unit.getManufacturer());
        attributes.put("model", unit.getModel());
        attributes.put("serialNumber", unit.getSerialNumber());
        attributes.put("maxPressurePsi", unit.getMaxPressurePsi());
        attributes.put("maxTensionLbf", unit.getMaxTensionLbf());
        
        attributesService.save(
            new AssetId(assetId),
            AttributeScope.SERVER_SCOPE,
            attributes
        );
    }
    
    /**
     * Guarda telemetría en tiempo real
     */
    public void saveTelemetry(UUID assetId, Map<String, Object> telemetry) {
        timeseriesService.save(
            new AssetId(assetId),
            new BasicTsKvEntry(System.currentTimeMillis(), telemetry)
        );
    }
}
```

---

### 📋 Componentes de Gestión FALTANTES

#### 10. **Administración de Plantillas - Frontend**

```
❌ NO EXISTE: ui-ngx/src/app/modules/home/pages/ct/templates/
├── ct-template-list.component.ts
├── ct-template-editor.component.ts
└── ct-template-preview.component.ts
```

#### 11. **Importación/Exportación de Plantillas**

```java
// ❌ FALTA
@RestController
@RequestMapping("/api/nexus/ct/templates")
public class CTTemplateController {
    
    @PostMapping("/import")
    public ResponseEntity<CTTemplateDto> importTemplate(
        @RequestParam("file") MultipartFile file
    ) {
        // Importar plantilla desde JSON
    }
    
    @GetMapping("/{id}/export")
    public ResponseEntity<Resource> exportTemplate(@PathVariable UUID id) {
        // Exportar plantilla a JSON
    }
}
```

#### 12. **Validación de Plantillas**

```java
// ❌ FALTA
@Service
public class CTTemplateValidationService {
    
    public ValidationResult validateTemplate(CTTemplate template) {
        // Validar estructura JSON
        // Validar tipos de assets
        // Validar atributos requeridos
        // Validar relaciones
    }
}
```

---

### 🎯 Dashboards y Visualización FALTANTES

#### 13. **Dashboards Automáticos por Asset**

Según roadmap, cada unidad CT debe tener:
- ❌ Real-Time Operations Dashboard
- ❌ Fleet Management Dashboard
- ❌ Reel Lifecycle Dashboard
- ❌ Job Execution Dashboard
- ❌ Analytics & Performance Dashboard

**Estos dashboards deben crearse automáticamente** al instanciar una plantilla.

#### 14. **Widgets Personalizados**

```
❌ NO EXISTEN widgets específicos de CT:
- Depth Tracker Widget
- Pressure Gauge Widget
- Tension Indicator Widget
- Fatigue Progress Widget
- BHA Configuration Widget
```

---

### 📊 Reportes y Analytics FALTANTES

#### 15. **Sistema de Reportes**

```
❌ NO EXISTE: common/ct-module/src/main/java/org/thingsboard/nexus/ct/report/
├── generators/
│   ├── DailyOperationsReportGenerator.java
│   ├── JobSummaryReportGenerator.java
│   └── FatigueAnalysisReportGenerator.java
└── templates/
    ├── daily-operations.jrxml
    └── job-summary.jrxml
```

---

## 🎯 Plan de Acción Recomendado

### Fase 1: Sistema de Plantillas (CRÍTICO)
**Prioridad**: 🔴 ALTA  
**Tiempo Estimado**: 2-3 semanas

1. **Crear CTTemplateService**
   - Cargar plantillas desde JSON
   - Instanciar plantillas creando assets
   - Gestionar variables y sustituciones

2. **Crear plantillas JSON base**
   - `ct-unit-standard.json`
   - `ct-unit-heavy-duty.json`
   - `reel-standard.json`
   - `reel-large-diameter.json`

3. **Integrar con AssetService**
   - Crear assets en ThingsBoard
   - Configurar atributos iniciales
   - Crear relaciones jerárquicas

### Fase 2: Integración con Gemelos Digitales
**Prioridad**: 🔴 ALTA  
**Tiempo Estimado**: 2 semanas

1. **Modificar servicios existentes**
   - `CTUnitService.createFromTemplate()`
   - `CTReelService.createFromTemplate()`
   - Sincronización asset ↔ tabla SQL

2. **Crear CTAssetSyncService**
   - Listeners de eventos de assets
   - Sincronización bidireccional

3. **Crear CTAttributeService**
   - Gestión de atributos
   - Gestión de telemetrías

### Fase 3: Frontend - Diálogos de Creación
**Prioridad**: 🟡 MEDIA  
**Tiempo Estimado**: 1 semana

1. **CTTemplateSelectorDialogComponent**
   - Mostrar plantillas disponibles
   - Preview de especificaciones

2. **Modificar diálogos de creación**
   - Integrar selector de plantillas
   - Formularios dinámicos según plantilla

3. **Implementar botones de acción**
   - `createUnit()` - Funcional
   - `createReel()` - Funcional
   - `createJob()` - Funcional

### Fase 4: Dashboards Automáticos
**Prioridad**: 🟡 MEDIA  
**Tiempo Estimado**: 2 semanas

1. **Dashboard Templates**
   - Crear plantillas de dashboards
   - Asociar a tipos de assets

2. **Creación automática**
   - Al instanciar plantilla, crear dashboard
   - Configurar widgets con asset IDs

### Fase 5: Reportes
**Prioridad**: 🟢 BAJA  
**Tiempo Estimado**: 1-2 semanas

1. **Generadores de reportes**
2. **Plantillas JRXML**
3. **Endpoints de exportación**

---

## 📝 Conclusiones

### Lo que SÍ funciona
✅ Listado de unidades/reels/jobs (si existen en BD)  
✅ Navegación entre páginas  
✅ Filtros y búsqueda  
✅ Cálculo de fatiga  
✅ Simulación de trabajos  

### Lo que NO funciona
❌ **Crear nuevas unidades** (botón no hace nada)  
❌ **Crear nuevos reels** (botón no hace nada)  
❌ **Crear nuevos jobs** (botón no hace nada)  
❌ **Gemelos digitales** (no se crean assets)  
❌ **Plantillas** (no existen)  
❌ **Dashboards automáticos** (no se crean)  
❌ **Jerarquías de assets** (no se crean relaciones)  

### Impacto en el Usuario
El usuario puede **VER** datos si los inserta manualmente en la BD, pero **NO PUEDE CREAR** nada desde la interfaz porque falta todo el sistema de plantillas y gemelos digitales que era el core del diseño.

---

## 🔗 Referencias

- `/home/diazhh/dev/nexus/dev/roadmaps/coiled-tubing/IMPLEMENTATION_GUIDE.md` - Líneas 318-354 (createFromTemplate)
- `/home/diazhh/dev/nexus/dev/roadmaps/coiled-tubing/README.md` - Líneas 91-110 (Sistema de Plantillas)
- `/home/diazhh/dev/nexus/dev/roadmaps/coiled-tubing/ARCHITECTURE.md` - Arquitectura de gemelos digitales
