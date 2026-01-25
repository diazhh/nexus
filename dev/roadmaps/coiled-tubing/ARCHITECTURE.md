# Arquitectura Técnica - Módulo Coiled Tubing

## Visión General de la Arquitectura

El módulo de Coiled Tubing sigue la arquitectura modular de Nexus, integrándose completamente con ThingsBoard mientras proporciona funcionalidad específica del dominio.

---

## 🎯 Patrones de Código y Convenciones

> **CRÍTICO**: Este módulo DEBE seguir las convenciones de ThingsBoard para garantizar compilación exitosa.
> 
> **Guía Completa**: `@/home/diazhh/dev/nexus/dev/METODOLOGIA_DESARROLLO_MODULOS.md`

### Patrón de Servicio (Service Layer)
```java
@Service
@RequiredArgsConstructor  // Inyección de dependencias automática
@Slf4j                    // Logger automático (NO declarar manualmente)
public class CTUnitService {
    
    private final CTUnitRepository unitRepository;
    private final CTReelRepository reelRepository;
    
    // Métodos de lectura: @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public CTUnitDto getById(UUID id) {
        CTUnit unit = unitRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", id.toString()));
        return CTUnitDto.fromEntity(unit);
    }
    
    // Métodos de escritura: @Transactional + RETORNAR DTO
    @Transactional
    public CTUnitDto create(CTUnit unit) {
        // Validaciones
        if (unitRepository.existsByUnitCode(unit.getUnitCode())) {
            throw new CTBusinessException("Unit code already exists");
        }
        
        // Guardar
        CTUnit saved = unitRepository.save(unit);
        
        // SIEMPRE retornar DTO actualizado
        return CTUnitDto.fromEntity(saved);
    }
}
```

### Patrón de Controlador (REST Controller)
```java
@RestController
@RequestMapping("/api/ct/units")
@RequiredArgsConstructor  // NO @Autowired
@Slf4j                    // NO declarar logger manualmente
public class CTUnitController {
    
    private final CTUnitService unitService;
    
    @GetMapping("/{id}")
    public ResponseEntity<CTUnitDto> getById(@PathVariable UUID id) {
        CTUnitDto dto = unitService.getById(id);
        return ResponseEntity.ok(dto);
    }
    
    @PostMapping
    public ResponseEntity<CTUnitDto> create(@Valid @RequestBody CTUnit unit) {
        CTUnitDto created = unitService.create(unit);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

### ⚠️ Errores Comunes Resueltos

| Error | Causa | Solución |
|-------|-------|----------|
| `method X already defined` | Método duplicado con misma firma | Eliminar duplicado, mantener versión que retorna DTO |
| `Field 'log' already exists` | Logger declarado con `@Slf4j` activo | Eliminar declaración manual |
| `void cannot be converted to Dto` | Servicio retorna void, controller espera Dto | Cambiar firma del servicio para retornar Dto |

---

## Arquitectura de Capas

```
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Dashboards  │  │ UI Components│  │   Reports    │          │
│  │   Angular    │  │   Angular    │  │   PDF/Excel  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE APLICACIÓN                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ CT Services  │  │ Job Manager  │  │Analytics Eng.│          │
│  │    Java      │  │    Java      │  │    Java      │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │REST APIs     │  │WebSocket API │  │Event Handlers│          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE LÓGICA DE NEGOCIO                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │Template Eng. │  │Data Mapping  │  │Rule Engine   │          │
│  │              │  │   Service    │  │  Integration │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │Fatigue Calc. │  │Job Simulation│  │Alarm Manager │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE THINGSBOARD CORE                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │Assets/Devices│  │  Rule Chains │  │   Alarms     │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Telemetry DB │  │ Attributes   │  │ Dashboards   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE PERSISTENCIA                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ PostgreSQL   │  │  Timeseries  │  │    Cache     │          │
│  │  (Nexus DB)  │  │   (Cassandra)│  │   (Redis)    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE INTEGRACIÓN                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ SCADA/OPC-UA │  │     MQTT     │  │  REST APIs   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

## Componentes Principales

### 1. Gestión de Unidades CT (CT Unit Management)

**Responsabilidades**:
- CRUD de unidades CT
- Instanciación desde templates
- Gestión de relaciones entre assets
- Tracking de ubicación y estado
- Historial de trabajos por unidad

**Componentes**:
```java
// Backend Services
CTUnitService
├── createFromTemplate(templateId, unitData)
├── updateUnitStatus(unitId, status)
├── assignReel(unitId, reelId)
├── detachReel(unitId)
├── getUnitDetails(unitId)
├── getUnitHistory(unitId)
└── calculateUtilization(unitId, period)

// Controllers
CTUnitController
├── POST   /api/nexus/ct/units
├── GET    /api/nexus/ct/units/{id}
├── PUT    /api/nexus/ct/units/{id}
├── DELETE /api/nexus/ct/units/{id}
├── POST   /api/nexus/ct/units/{id}/assign-reel
├── POST   /api/nexus/ct/units/{id}/detach-reel
└── GET    /api/nexus/ct/units/{id}/history
```

### 2. Gestión de Reels (Reel Management)

**Responsabilidades**:
- CRUD de reels
- Tracking de fatiga acumulada
- Cálculo de vida útil restante
- Historial de uso
- Gestión de inspecciones

**Componentes**:
```java
// Backend Services
CTReelService
├── createReel(reelData)
├── updateFatigue(reelId, fatigueData)
├── calculateRemainingLife(reelId)
├── getReelHistory(reelId)
├── scheduleInspection(reelId, date)
└── getAvailableReels(specifications)

// Fatigue Calculator
FatigueCalculator
├── calculateCycleFatigue(cycles, pressure, tension)
├── accumulateFatigue(reelId, jobData)
├── predictLifespan(reelId, usagePattern)
└── generateFatigueReport(reelId)
```

### 3. Gestión de Trabajos (Job Management)

**Responsabilidades**:
- Planificación de trabajos
- Asignación de recursos
- Ejecución y monitoreo
- Registro de eventos
- Generación de reportes

**Componentes**:
```java
// Backend Services
CTJobService
├── createJob(jobData)
├── startJob(jobId)
├── updateJobStatus(jobId, status)
├── recordEvent(jobId, event)
├── pauseJob(jobId, reason)
├── completeJob(jobId)
└── generateJobReport(jobId)

// Job Execution Engine
JobExecutionEngine
├── initializeJob(jobId)
├── monitorRealTimeData(jobId)
├── validateParameters(jobId)
├── triggerAlarms(jobId, condition)
└── finalizeJob(jobId)
```

### 4. Motor de Cálculo de Fatiga (Fatigue Engine)

**Responsabilidades**:
- Cálculo continuo de fatiga
- Modelo de acumulación de daño
- Predicción de vida útil
- Generación de alertas

**Algoritmo Base**:
```
Fatiga Acumulada = Σ (n_i / N_i)

Donde:
- n_i = número de ciclos a nivel de estrés i
- N_i = número de ciclos hasta falla a nivel i
- Σ = suma acumulativa (regla de Palmgren-Miner)

Factores considerados:
- Presión interna
- Tensión axial
- Radio de curvatura (gooseneck, reel)
- Temperatura
- Factores de corrosión
- Factores de concentración de estrés (soldaduras)
```

**Implementación**:
```java
// Rule Chain Node: Fatigue Calculation
function calculateFatigue(msg, metadata, msgType) {
    var telemetry = msg;
    var reelId = metadata.reelId;
    
    // Obtener datos históricos del reel
    var reelData = getReelData(reelId);
    var currentFatigue = reelData.accumulatedFatigue;
    
    // Parámetros del ciclo actual
    var pressure = telemetry.pressure;
    var tension = telemetry.tension;
    var gooseNeckRadius = reelData.gooseNeckRadius;
    var reelRadius = reelData.reelRadius;
    var tubingOD = reelData.tubingOD;
    var tubingID = reelData.tubingID;
    var material = reelData.materialGrade;
    
    // Cálculo de estrés
    var hoopStress = (pressure * tubingID) / (2 * (tubingOD - tubingID));
    var axialStress = tension / (Math.PI * (tubingOD^2 - tubingID^2) / 4);
    var bendingStress = (tubingOD / 2) * (E / gooseNeckRadius);
    
    // Estrés combinado (Von Mises)
    var vonMisesStress = Math.sqrt(
        hoopStress^2 + axialStress^2 + bendingStress^2 - 
        hoopStress*axialStress - hoopStress*bendingStress - 
        axialStress*bendingStress
    );
    
    // Número de ciclos hasta falla (curva S-N del material)
    var N = calculateCyclesToFailure(vonMisesStress, material);
    
    // Incremento de fatiga (1 ciclo)
    var fatigueIncrement = 1 / N;
    
    // Aplicar factores de corrección
    fatigueIncrement *= getCorrosionFactor(reelData.environment);
    fatigueIncrement *= getWeldFactor(reelData.hasWelds);
    
    // Actualizar fatiga acumulada
    var newFatigue = currentFatigue + fatigueIncrement;
    
    // Guardar y generar alarmas si es necesario
    saveAccumulatedFatigue(reelId, newFatigue);
    
    if (newFatigue > 0.8) {
        createAlarm(reelId, "CT_FATIGUE_HIGH", newFatigue);
    }
    if (newFatigue > 0.95) {
        createAlarm(reelId, "CT_FATIGUE_CRITICAL", newFatigue);
    }
    
    return {msg: {fatigue: newFatigue}, metadata: metadata, msgType: msgType};
}
```

### 5. Sistema de Mapeo de Datos (Data Mapping)

**Responsabilidades**:
- Mapeo de variables SCADA a assets
- Transformación de datos
- Validación de rangos
- Distribución a múltiples assets

**Configuración de Mapeo**:
```json
{
  "source": {
    "type": "OPC_UA",
    "endpoint": "opc.tcp://scada-server:4840",
    "nodeId": "ns=2;s=CT_UNIT_001"
  },
  "mappings": [
    {
      "sourceVariable": "HYD_PRESSURE_1",
      "targetAsset": "CT-UNIT-001-HYDRAULIC",
      "targetTelemetry": "pressure",
      "transformation": "value * 6.89476",
      "unit": "PSI_to_kPa",
      "validation": {
        "min": 0,
        "max": 40000,
        "alarm_threshold": 35000
      }
    },
    {
      "sourceVariable": "INJ_SPEED",
      "targetAsset": "CT-UNIT-001-INJECTION",
      "targetTelemetry": "speed",
      "transformation": "value * 0.3048",
      "unit": "ft_min_to_m_min"
    },
    {
      "sourceVariable": "INJ_DEPTH",
      "targetAsset": "CT-UNIT-001-INJECTION",
      "targetTelemetry": "depth",
      "transformation": "value * 0.3048",
      "unit": "ft_to_m"
    },
    {
      "sourceVariable": "INJ_TENSION",
      "targetAsset": "CT-UNIT-001-INJECTION",
      "targetTelemetry": "tension",
      "transformation": "value * 4.44822",
      "unit": "lbf_to_N"
    },
    {
      "sourceVariable": "REEL_ROTATION",
      "targetAsset": "CT-REEL-R456",
      "targetTelemetry": "rotation_speed",
      "transformation": "value"
    }
  ]
}
```

### 6. Sistema de Plantillas (Template System)

**Plantilla de Unidad CT Estándar**:
```json
{
  "template_id": "ct-unit-standard-v1",
  "template_name": "CT Unit Standard",
  "template_type": "COMPOSITE",
  "category": "CT_UNIT",
  "nodes": [
    {
      "node_key": "root",
      "node_name": "{{unit_name}}",
      "node_type": "ASSET",
      "asset_type": "CT_UNIT",
      "is_root": true,
      "attributes": {
        "manufacturer": "{{manufacturer}}",
        "model": "{{model}}",
        "serial_number": "{{serial_number}}",
        "year": "{{year}}",
        "max_pressure_rating": "{{max_pressure}}",
        "location": "{{location}}"
      },
      "telemetries": [
        "operational_hours",
        "availability_percentage",
        "current_status"
      ]
    },
    {
      "node_key": "hydraulic_system",
      "node_name": "{{unit_name}}-HYDRAULIC",
      "node_type": "ASSET",
      "asset_type": "CT_HYDRAULIC_SYSTEM",
      "parent_node_key": "root",
      "attributes": {
        "pump_type": "Triple Pump",
        "max_pressure": "{{max_pressure}}",
        "oil_capacity_liters": 600,
        "oil_type": "ISO VG 46"
      },
      "telemetries": [
        "pressure",
        "temperature",
        "flow_rate",
        "oil_level"
      ]
    },
    {
      "node_key": "injection_system",
      "node_name": "{{unit_name}}-INJECTION",
      "node_type": "ASSET",
      "asset_type": "CT_INJECTION_SYSTEM",
      "parent_node_key": "root",
      "attributes": {
        "max_speed_m_min": 30,
        "max_tension_kN": 350,
        "gripper_type": "Dual Chain"
      },
      "telemetries": [
        "speed",
        "tension",
        "depth",
        "direction",
        "gripper_pressure"
      ]
    },
    {
      "node_key": "control_system",
      "node_name": "{{unit_name}}-CONTROL",
      "node_type": "ASSET",
      "asset_type": "CT_CONTROL_SYSTEM",
      "parent_node_key": "root",
      "attributes": {
        "software_version": "v2.5",
        "plc_model": "Siemens S7-1500",
        "hmi_type": "Touch Panel"
      },
      "telemetries": [
        "connection_status",
        "active_alarms",
        "operation_mode"
      ]
    },
    {
      "node_key": "power_pack",
      "node_name": "{{unit_name}}-POWER",
      "node_type": "ASSET",
      "asset_type": "CT_POWER_PACK",
      "parent_node_key": "root",
      "attributes": {
        "engine_model": "Caterpillar C15",
        "power_rating_hp": 550,
        "fuel_capacity_gal": 300
      },
      "telemetries": [
        "engine_rpm",
        "fuel_level",
        "engine_temperature",
        "oil_pressure"
      ]
    },
    {
      "node_key": "gooseneck",
      "node_name": "{{unit_name}}-GOOSENECK",
      "node_type": "ASSET",
      "asset_type": "CT_GOOSENECK",
      "parent_node_key": "root",
      "attributes": {
        "radius_m": 1.8,
        "adjustable": true,
        "min_bend_radius": 48
      },
      "telemetries": [
        "angle",
        "wear_status"
      ]
    }
  ],
  "relations": [
    {
      "from": "root",
      "to": "hydraulic_system",
      "type": "Contains"
    },
    {
      "from": "root",
      "to": "injection_system",
      "type": "Contains"
    },
    {
      "from": "root",
      "to": "control_system",
      "type": "Contains"
    },
    {
      "from": "root",
      "to": "power_pack",
      "type": "Contains"
    },
    {
      "from": "root",
      "to": "gooseneck",
      "type": "Contains"
    }
  ]
}
```

## Rule Chains del Módulo

### 1. CT Fatigue Calculation Chain

```
[Input Node: Telemetry]
         ↓
[Filter: Reel Telemetry] → [Discard if not reel]
         ↓
[Script: Calculate Fatigue] (algoritmo descrito arriba)
         ↓
[Save Timeseries: fatigue_accumulated]
         ↓
[Switch: Fatigue Level]
    ├─→ [fatigue > 0.95] → [Create Alarm: CRITICAL]
    ├─→ [fatigue > 0.80] → [Create Alarm: HIGH]
    └─→ [else] → [Clear Alarm]
```

### 2. CT Maintenance Alert Chain

```
[Input: Operational Hours Update]
         ↓
[Get Attribute: last_maintenance_hours]
         ↓
[Script: Calculate Hours Since Maintenance]
         ↓
[Switch: Hours Threshold]
    ├─→ [hours > 500] → [Create Alarm: MAINTENANCE_OVERDUE]
    ├─→ [hours > 400] → [Create Alarm: MAINTENANCE_DUE]
    └─→ [else] → [No Action]
```

### 3. CT Job Event Processing Chain

```
[Input: Job Event]
         ↓
[Enrich: Job Data]
         ↓
[Script: Process Event] (guardar en ct_job_events)
         ↓
[Switch: Event Type]
    ├─→ [START] → [Update Job Status] → [Notify Operators]
    ├─→ [PHASE_CHANGE] → [Log Phase] → [Update Dashboard]
    ├─→ [ALARM] → [Create Alarm] → [Send Alert]
    ├─→ [COMPLETE] → [Finalize Job] → [Generate Report]
    └─→ [OTHER] → [Log Event]
```

### 4. CT Data Validation Chain

```
[Input: SCADA Data]
         ↓
[Data Mapper] (aplica transformaciones)
         ↓
[Validator]
    ├─→ [Out of Range] → [Create Alarm: INVALID_DATA]
    ├─→ [Stale Data] → [Create Alarm: CONNECTION_LOST]
    └─→ [Valid] → [Route to Asset]
                      ↓
                [Save Telemetry]
```

## Integración con Sistemas Externos

### SCADA/OPC-UA Integration

```java
@Service
public class CTScadaIntegrationService {
    
    @Autowired
    private OpcUaClient opcUaClient;
    
    @Autowired
    private DataMappingService mappingService;
    
    @Scheduled(fixedRate = 1000) // 1 segundo
    public void pollScadaData() {
        List<CTUnit> activeUnits = getActiveUnits();
        
        for (CTUnit unit : activeUnits) {
            DataMapping mapping = mappingService.getMapping(unit.getId());
            
            // Leer datos del SCADA
            Map<String, Object> scadaData = opcUaClient.readNodes(
                mapping.getSourceNodeIds()
            );
            
            // Aplicar mapeo y transformaciones
            Map<EntityId, Map<String, Object>> telemetryByAsset = 
                mappingService.mapToAssets(scadaData, mapping);
            
            // Enviar a ThingsBoard
            for (Map.Entry<EntityId, Map<String, Object>> entry : 
                 telemetryByAsset.entrySet()) {
                
                telemetryService.saveAndNotify(
                    entry.getKey(),
                    entry.getValue(),
                    System.currentTimeMillis()
                );
            }
        }
    }
}
```

### Real-Time Data Streaming (WebSocket)

```typescript
// Frontend Service
@Injectable()
export class CTRealtimeService {
  private ws: WebSocketSubject<any>;
  
  connectToJob(jobId: string): Observable<JobTelemetry> {
    this.ws = webSocket({
      url: `wss://nexus-server/api/ws/ct/jobs/${jobId}/telemetry`,
      deserializer: (e) => JSON.parse(e.data)
    });
    
    return this.ws.asObservable();
  }
  
  subscribeToUnit(unitId: string): Observable<UnitTelemetry> {
    return this.ws.multiplex(
      () => ({ subscribe: unitId }),
      () => ({ unsubscribe: unitId }),
      (message) => message.entityId === unitId
    );
  }
}
```

## Patrones de Diseño Utilizados

### 1. Template Pattern
Para la creación de unidades y reels desde plantillas predefinidas.

### 2. Observer Pattern
Para notificaciones en tiempo real de cambios en telemetrías y alarmas.

### 3. Strategy Pattern
Para diferentes algoritmos de cálculo de fatiga según material y condiciones.

### 4. Factory Pattern
Para crear instancias de jobs, units, reels con configuraciones específicas.

### 5. Repository Pattern
Para acceso a datos y abstracción de la capa de persistencia.

## Consideraciones de Performance

### Caching Strategy
```java
@Cacheable(value = "ct-units", key = "#unitId")
public CTUnit getUnitById(UUID unitId) {
    return unitRepository.findById(unitId);
}

@CacheEvict(value = "ct-units", key = "#unit.id")
public CTUnit updateUnit(CTUnit unit) {
    return unitRepository.save(unit);
}
```

### Optimización de Queries
```java
// Fetch join para evitar N+1 queries
@Query("SELECT u FROM CTUnit u " +
       "LEFT JOIN FETCH u.hydraulicSystem " +
       "LEFT JOIN FETCH u.injectionSystem " +
       "WHERE u.id = :unitId")
CTUnit findByIdWithSystems(@Param("unitId") UUID unitId);
```

### Indexación de Base de Datos
```sql
CREATE INDEX idx_ct_jobs_status_date ON ct_jobs(status, start_date);
CREATE INDEX idx_ct_fatigue_log_reel_time ON ct_fatigue_log(reel_id, timestamp);
CREATE INDEX idx_ct_job_events_job_time ON ct_job_events(job_id, event_time);
```

## Seguridad

### Autenticación y Autorización
- JWT tokens para APIs
- Role-based access control (RBAC)
- Permisos granulares por recurso
- Auditoría de acciones críticas

### Protección de Datos
- Encriptación de datos sensibles
- HTTPS para todas las comunicaciones
- Rate limiting en APIs
- Validación de inputs

## Escalabilidad

### Horizontal Scaling
- Stateless services
- Load balancing
- Shared cache (Redis)
- Message queue para procesamiento asíncrono

### Vertical Scaling
- Optimización de queries
- Connection pooling
- Lazy loading de relaciones
- Paginación de resultados

## Monitoreo y Logging

```java
@Slf4j
@Service
public class CTJobService {
    
    public JobResult executeJob(UUID jobId) {
        log.info("Starting job execution: {}", jobId);
        
        try {
            // Lógica de ejecución
            log.debug("Job {} - Phase: initialization", jobId);
            
            // ...
            
            log.info("Job {} completed successfully", jobId);
            return JobResult.success();
            
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage(), e);
            return JobResult.failure(e);
        }
    }
}
```

## Próximos Pasos

1. Implementación de servicios backend
2. Desarrollo de Rule Chains
3. Creación de componentes frontend
4. Pruebas de integración
5. Optimización de performance
6. Documentación de APIs

---

**Versión**: 1.0.0  
**Última Actualización**: Enero 2026
