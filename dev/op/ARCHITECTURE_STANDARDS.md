# NEXUS Architecture Standards

**Version**: 1.0
**Date**: 2026-02-04
**Status**: DRAFT - Pending Approval

Este documento define los estándares de arquitectura unificados para todos los módulos Nexus.

---

## 1. Visión General de Módulos

### 1.1 Módulos Nexus Existentes

| Módulo | Propósito | Asset Prefix |
|--------|-----------|--------------|
| **PF** | Production Facilities - Monitoreo de pozos e infraestructura | `pf_` |
| **PO** | Production Optimization - Optimización de sistemas de levantamiento | `pf_` (usa assets de PF) |
| **DR** | Drilling - Gestión de perforación | `dr_` |
| **CT** | Coiled Tubing - Operaciones de tubing flexible | `ct_` |
| **RV** | Reservoir - Yacimientos y análisis de reservorios | `rv_` |

### 1.2 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          THINGSBOARD CORE                               │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │
│  │AssetService │ │Timeseries   │ │Attributes   │ │AlarmService │       │
│  │             │ │Service      │ │Service      │ │             │       │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └──────┬──────┘       │
│         │               │               │               │               │
│  ┌──────┴───────────────┴───────────────┴───────────────┴──────┐       │
│  │              TB WebSocket API (/api/ws)                      │       │
│  │         TbWebSocketHandler + SubscriptionServices            │       │
│  └──────────────────────────────────────────────────────────────┘       │
│         │               │               │               │               │
│  ┌──────┴───────────────┴───────────────┴───────────────┴──────┐       │
│  │              TB Dashboard System (Widgets)                   │       │
│  └──────────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
     ┌──────────────────────────────┼──────────────────────────────┐
     │                              │                              │
┌────┴────┐  ┌────┴────┐  ┌────┴────┐  ┌────┴────┐  ┌────┴────┐
│   PF    │  │   PO    │  │   DR    │  │   CT    │  │   RV    │
│ Module  │  │ Module  │  │ Module  │  │ Module  │  │ Module  │
│         │  │         │  │         │  │         │  │         │
│Wrapper  │  │Wrapper  │  │Wrapper  │  │Wrapper  │  │Wrapper  │
│Services │  │Services │  │Services │  │Services │  │Services │
└─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘
```

---

## 2. Estructura de Paquetes (ESTÁNDAR)

Todos los módulos DEBEN seguir esta estructura:

```
common/{module}-module/src/main/java/org/thingsboard/nexus/{module}/
├── config/
│   └── {Module}ModuleConfiguration.java
├── controller/
│   └── {Module}{Entity}Controller.java
├── dto/
│   ├── {Module}{Entity}Dto.java
│   └── enums/                          # Si hay más de 3 enums
│       └── {Status|Type}Enum.java
├── exception/
│   ├── {Module}Exception.java          # Base exception
│   ├── {Module}EntityNotFoundException.java
│   ├── {Module}BusinessException.java  # Para errores de negocio
│   └── {Module}ExceptionHandler.java   # @RestControllerAdvice
├── model/                              # SOLO si hay entidades JPA
│   └── {Module}{Entity}.java
├── repository/                         # SOLO si hay entidades JPA
│   └── {Module}{Entity}Repository.java
└── service/
    ├── {Module}AssetService.java       # Wrapper de TB AssetService
    ├── {Module}AttributeService.java   # Wrapper de TB AttributesService
    └── {Module}{Domain}Service.java    # Servicios de dominio
```

### 2.1 Archivos PROHIBIDOS

Los siguientes patrones NO deben existir en módulos Nexus:

| Patrón | Razón | Alternativa |
|--------|-------|-------------|
| `{Module}WebSocketHandler.java` | TB ya tiene WebSocket | Usar `/api/ws` de TB |
| `{Module}WebSocketConfig.java` | Duplicación | Usar TB WebSocket |
| Custom notification via WS | TB Notification System | Usar TB Notifications |

---

## 3. Convenciones de Naming

### 3.1 Prefijos de Módulo

| Módulo | Prefijo Java | Prefijo Asset | Ejemplo |
|--------|--------------|---------------|---------|
| PF | `Pf` | `pf_` | `PfWellDto`, `pf_well` |
| PO | `Po` | `pf_` | `PoHealthScoreDto` |
| DR | `Dr` | `dr_` | `DrRigDto`, `dr_rig` |
| CT | `Ct` | `ct_` | `CtJobDto`, `ct_unit` |
| RV | `Rv` | `rv_` | `RvWellDto`, `rv_well` |

> **NOTA**: CT actualmente usa `CT` (mayúsculas). DEBE migrarse a `Ct` para consistencia.

### 3.2 Sufijos Estándar

| Tipo | Sufijo | Ejemplo |
|------|--------|---------|
| Data Transfer Object | `Dto` | `PfWellDto` |
| Service | `Service` | `PfWellService` |
| Controller | `Controller` | `PfWellController` |
| Repository | `Repository` | `PoRecommendationRepository` |
| Exception | `Exception` | `PfEntityNotFoundException` |
| Configuration | `Configuration` | `PfModuleConfiguration` |

### 3.3 Asset Types

```java
// Constantes DEBEN definirse en el DTO correspondiente
public class PfWellDto {
    public static final String ASSET_TYPE = "pf_well";
    public static final String ATTR_API_NUMBER = "api_number";
    public static final String ATTR_STATUS = "status";
    // ...
}
```

---

## 4. Uso de ThingsBoard Core

### 4.1 Servicios a Usar (OBLIGATORIO)

| Necesidad | Servicio TB | NO crear |
|-----------|-------------|----------|
| Crear/Modificar Assets | `AssetService` | Tablas custom de entidades |
| Guardar Atributos | `AttributesService` | Tablas de configuración |
| Guardar Telemetría | `TimeseriesService` | Tablas de telemetría |
| Crear Alarmas | `AlarmService` | Sistema de alarmas custom |
| Relaciones | `RelationService` | Tablas de relaciones |
| WebSocket Real-time | `TbWebSocketHandler` | WebSocket custom |
| Notificaciones | TB Notification System | Sistema de notificaciones |
| Dashboards | TB Dashboard System | Dashboards Angular custom |

### 4.2 Wrapper Services Pattern

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PfAssetService {

    private final AssetService assetService;  // TB Core

    /**
     * Crea un Asset usando TB Core.
     * NUNCA crear tablas custom para entidades base.
     */
    public Asset createAsset(UUID tenantId, String assetType, String name, String label) {
        Asset asset = new Asset();
        asset.setTenantId(TenantId.fromUUID(tenantId));  // SIEMPRE usar fromUUID()
        asset.setType(assetType);
        asset.setName(name);
        asset.setLabel(label);
        return assetService.saveAsset(asset);
    }
}
```

### 4.3 Instanciación de IDs (ESTÁNDAR)

```java
// ✅ CORRECTO
TenantId tenantId = TenantId.fromUUID(uuid);
AssetId assetId = new AssetId(uuid);

// ❌ INCORRECTO
TenantId tenantId = new TenantId(uuid);  // No usar constructor directo
```

### 4.4 Cuándo Crear Tablas Custom

SOLO crear tablas custom cuando:

1. ✅ Se requiere **versionado histórico** (ej: `po_optimization_result`)
2. ✅ Hay **workflow de estados** complejo (ej: `po_recommendation`)
3. ✅ Se necesitan **JOINs SQL complejos** para ML/Analytics
4. ✅ **Datos de cálculo** que no son entidades de dominio

❌ NO crear tablas custom para:
- Entidades de dominio (Wells, Rigs, etc.) → Usar Assets
- Configuración → Usar Attributes
- Telemetría → Usar ts_kv
- Alarmas → Usar TB Alarm System

---

## 5. Manejo de Excepciones

### 5.1 Jerarquía de Excepciones (ESTÁNDAR)

```java
// Base exception del módulo
public class PfException extends RuntimeException {
    public PfException(String message) { super(message); }
    public PfException(String message, Throwable cause) { super(message, cause); }
}

// Para entidades no encontradas
public class PfEntityNotFoundException extends PfException {
    public PfEntityNotFoundException(String entityType, UUID id) {
        super(String.format("%s not found with id: %s", entityType, id));
    }
}

// Para errores de lógica de negocio
public class PfBusinessException extends PfException {
    public PfBusinessException(String message) { super(message); }
}

// Para errores de validación
public class PfValidationException extends PfException {
    private final Map<String, String> fieldErrors;
    // ...
}
```

### 5.2 Exception Handler (ESTÁNDAR)

```java
@RestControllerAdvice(basePackages = "org.thingsboard.nexus.pf")
@Slf4j
public class PfExceptionHandler {

    @ExceptionHandler(PfEntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PfEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(PfBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(PfBusinessException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse("BUSINESS_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(PfValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(PfValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage(), ex.getFieldErrors()));
    }
}
```

---

## 6. Configuración de Módulos

### 6.1 Estructura de Configuration

```java
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "nexus.pf")
@Data
public class PfModuleConfiguration {

    // Feature flags
    private boolean telemetryProcessingEnabled = true;
    private boolean alarmEvaluationEnabled = true;

    // Timeouts
    private int defaultTimeoutMs = 5000;

    // Batch processing
    private int batchSize = 100;

    @Bean
    public RestTemplate pfRestTemplate() {
        return new RestTemplate();
    }
}
```

### 6.2 Property Prefixes (ESTÁNDAR)

| Módulo | Prefix |
|--------|--------|
| PF | `nexus.pf` |
| PO | `nexus.po` |
| DR | `nexus.dr` |
| CT | `nexus.ct` |
| RV | `nexus.rv` |

---

## 7. Testing

### 7.1 Requerimientos Mínimos

| Tipo | Cobertura Mínima |
|------|------------------|
| Unit Tests | 80% en Services |
| Integration Tests | Controllers principales |
| Naming | `{Class}Test.java` |

### 7.2 Estructura de Tests

```
src/test/java/org/thingsboard/nexus/{module}/
├── service/
│   ├── {Module}AssetServiceTest.java
│   └── {Module}{Domain}ServiceTest.java
└── controller/
    └── {Module}{Entity}ControllerTest.java
```

### 7.3 Patrón de Test con Mockito

```java
@ExtendWith(MockitoExtension.class)
class PfWellServiceTest {

    @Mock
    private PfAssetService pfAssetService;

    @Mock
    private PfAttributeService pfAttributeService;

    @InjectMocks
    private PfWellService wellService;

    @Test
    @DisplayName("Should create well successfully")
    void shouldCreateWellSuccessfully() {
        // Given
        PfWellDto dto = createTestWellDto();
        when(pfAssetService.createAsset(any(), any(), any(), any()))
            .thenReturn(createMockAsset());

        // When
        PfWellDto result = wellService.createWell(tenantId, dto);

        // Then
        assertNotNull(result);
        verify(pfAssetService).createAsset(any(), eq("pf_well"), any(), any());
    }
}
```

---

## 8. WebSocket y Real-Time

### 8.1 Estrategia Unificada

```
┌─────────────────────────────────────────────────────────────┐
│                    TB WebSocket (/api/ws)                   │
│                                                             │
│  Soporta:                                                   │
│  - Telemetry subscriptions (entityId + keys)               │
│  - Attribute subscriptions (entityId + keys)               │
│  - Alarm subscriptions (entityId o tenantId)               │
│  - Entity data subscriptions (queries)                      │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Código a ELIMINAR

Los siguientes archivos deben eliminarse por ser redundantes:

```
common/pf-module/src/main/java/org/thingsboard/nexus/pf/websocket/
├── PfWebSocketConfig.java      # ELIMINAR
└── PfWebSocketHandler.java     # ELIMINAR
```

### 8.3 Cómo Usar TB WebSocket desde Frontend

```typescript
// Angular - Suscripción a telemetría de pozo
this.telemetryService.subscribeToEntityTelemetry(
  wellEntityId,
  ['frequency_hz', 'current_a', 'temperature_f']
).subscribe(update => {
  this.latestTelemetry = update;
});

// Suscripción a alarmas
this.alarmService.subscribeToEntityAlarms(wellEntityId)
  .subscribe(alarm => {
    this.activeAlarms.push(alarm);
  });
```

---

## 9. Dashboards - Arquitectura Unificada

### 9.1 Estrategia Principal

**Nexus usará dashboards CUSTOM (Angular) pero REUTILIZARÁ los servicios de TB.**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    DASHBOARDS NEXUS (Custom Angular)                    │
│                                                                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │
│  │ PF Dashboard│ │ PO Dashboard│ │ DR Dashboard│ │ RV Dashboard│       │
│  │  (Custom)   │ │  (Custom)   │ │  (Custom)   │ │  (Custom)   │       │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └──────┬──────┘       │
│         │               │               │               │               │
│  ┌──────┴───────────────┴───────────────┴───────────────┴──────┐       │
│  │             CAPA DE SERVICIOS ANGULAR                        │       │
│  │  ┌────────────────────────────────────────────────────────┐ │       │
│  │  │ Servicios Nexus HTTP (CRUD)                            │ │       │
│  │  │ - PfWellService, PoOptimizationService, DrRigService   │ │       │
│  │  └────────────────────────────────────────────────────────┘ │       │
│  │  ┌────────────────────────────────────────────────────────┐ │       │
│  │  │ Servicios TB REUTILIZADOS (Real-time + Data)           │ │       │
│  │  │ - TelemetryWebsocketService (WebSocket)                │ │       │
│  │  │ - AttributeService (HTTP)                              │ │       │
│  │  │ - EntityDataSubscription (Subscriptions)               │ │       │
│  │  └────────────────────────────────────────────────────────┘ │       │
│  └──────────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          THINGSBOARD CORE                               │
│                                                                         │
│  Backend: /api/ws (WebSocket) + /api/plugins/telemetry/* (HTTP)        │
└─────────────────────────────────────────────────────────────────────────┘
```

### 9.2 Servicios de TB a Reutilizar en Frontend

#### WebSocket (Real-time)

| Servicio TB | Ubicación | Uso en Nexus |
|-------------|-----------|--------------|
| `TelemetryWebsocketService` | `@core/ws/telemetry-websocket.service.ts` | Telemetría en tiempo real |
| `EntityDataSubscription` | `@core/api/entity-data-subscription.ts` | Suscripciones complejas |
| `WebsocketService` | `@core/ws/websocket.service.ts` | Base class |

#### HTTP (Data Access)

| Servicio TB | Ubicación | Uso en Nexus |
|-------------|-----------|--------------|
| `AttributeService` | `@core/http/attribute.service.ts` | Leer/escribir atributos |
| `AssetService` | `@core/http/asset.service.ts` | CRUD de assets |
| `AlarmService` | `@core/http/alarm.service.ts` | Gestión de alarmas |

### 9.3 Ejemplo de Implementación Correcta

#### ❌ INCORRECTO (Polling cada 30s - como DR actual)

```typescript
// dr-realtime-dashboard.component.ts - INEFICIENTE
ngOnInit() {
  // Polling cada 30 segundos - MAL
  interval(30000).pipe(
    switchMap(() => this.rigService.getRigsByStatus(...))
  ).subscribe(rigs => {
    this.activeRigs = rigs;
  });
}
```

#### ✅ CORRECTO (WebSocket de TB)

```typescript
// pf-well-dashboard.component.ts - EFICIENTE
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { TelemetrySubscriber, EntityDataCmd } from '@shared/models/telemetry/telemetry.models';

@Component({...})
export class PfWellDashboardComponent implements OnInit, OnDestroy {

  private telemetrySubscriber: TelemetrySubscriber;

  constructor(
    private telemetryWsService: TelemetryWebsocketService,
    private pfWellService: PfWellService  // Servicio Nexus para CRUD
  ) {}

  ngOnInit() {
    // 1. Cargar datos iniciales via HTTP (servicio Nexus)
    this.pfWellService.getWell(this.wellId).subscribe(well => {
      this.well = well;
      // 2. Suscribirse a telemetría en tiempo real (servicio TB)
      this.subscribeToTelemetry();
    });
  }

  private subscribeToTelemetry() {
    // Usar el servicio de TB para WebSocket
    const entityDataCmd = new EntityDataCmd();
    entityDataCmd.entityFilter = {
      type: 'singleEntity',
      singleEntity: { entityType: 'ASSET', id: this.wellId }
    };
    entityDataCmd.latestCmd = {
      keys: [
        { type: 'timeSeries', key: 'frequency_hz' },
        { type: 'timeSeries', key: 'current_a' },
        { type: 'timeSeries', key: 'temperature_f' }
      ]
    };

    this.telemetrySubscriber = new TelemetrySubscriber([entityDataCmd]);
    this.telemetrySubscriber.onEntityData = (update) => {
      // Actualizar UI con datos en tiempo real
      this.updateTelemetryDisplay(update);
    };

    this.telemetryWsService.subscribe(this.telemetrySubscriber);
  }

  ngOnDestroy() {
    if (this.telemetrySubscriber) {
      this.telemetryWsService.unsubscribe(this.telemetrySubscriber);
    }
  }
}
```

### 9.4 Estructura de Servicios Frontend para Nexus

```
ui-ngx/src/app/core/http/
├── pf/                          # Servicios HTTP Nexus PF
│   ├── pf-well.service.ts       # CRUD de pozos
│   ├── pf-wellpad.service.ts    # CRUD de macollas
│   └── pf-esp.service.ts        # CRUD de sistemas ESP
├── po/                          # Servicios HTTP Nexus PO
│   ├── po-optimization.service.ts
│   └── po-health.service.ts
├── dr/                          # Servicios HTTP Nexus DR (ya existen)
│   ├── dr-rig.service.ts
│   └── dr-run.service.ts
└── [TB Core services]           # REUTILIZAR estos
    ├── attribute.service.ts     # ← Usar para atributos
    ├── asset.service.ts         # ← Usar para assets genéricos
    └── alarm.service.ts         # ← Usar para alarmas

ui-ngx/src/app/core/ws/          # REUTILIZAR estos (NO crear custom)
├── telemetry-websocket.service.ts  # ← Suscripciones real-time
├── websocket.service.ts            # ← Base WebSocket
└── notification-websocket.service.ts
```

### 9.5 Modelos Compartidos a Usar

```typescript
// Importar modelos de TB en lugar de crear propios

// Telemetría
import {
  TelemetrySubscriber,
  EntityDataCmd,
  AttributeData,
  TimeseriesData
} from '@shared/models/telemetry/telemetry.models';

// Queries
import {
  EntityFilter,
  EntityData,
  EntityKey,
  EntityKeyType
} from '@shared/models/query/query.models';

// Tiempo
import {
  SubscriptionTimewindow,
  AggregationType
} from '@shared/models/time/time.models';

// Página
import {
  PageData,
  PageLink
} from '@shared/models/page/page-data';
```

### 9.6 Checklist para Nuevos Dashboards

- [ ] Usar componentes Angular custom (NO TB Dashboard widgets)
- [ ] Crear servicio HTTP en `@core/http/{module}/` para operaciones CRUD
- [ ] Reutilizar `TelemetryWebsocketService` para datos en tiempo real
- [ ] Reutilizar `AttributeService` para leer/escribir atributos
- [ ] Reutilizar modelos de `@shared/models/` en lugar de crear propios
- [ ] NO crear WebSocket custom (usar `/api/ws` de TB)
- [ ] NO usar polling (interval) para datos que cambian frecuentemente

---

## 10. Checklist de Compliance

### Para nuevos módulos:

- [ ] Estructura de paquetes sigue estándar (Sección 2)
- [ ] Naming sigue convenciones (Sección 3)
- [ ] Usa TB Core services via wrappers (Sección 4)
- [ ] Excepciones siguen jerarquía (Sección 5)
- [ ] Configuration con prefix correcto (Sección 6)
- [ ] Tests con 80% coverage en services (Sección 7)
- [ ] NO tiene WebSocket custom (Sección 8)
- [ ] Dashboards usan TB System (Sección 9)

### Para módulos existentes:

| Módulo | Compliance | Acciones Pendientes |
|--------|------------|---------------------|
| PF | 🟡 85% | Eliminar WebSocket custom |
| PO | 🟢 95% | - |
| DR | 🔴 60% | Agregar tests, estandarizar naming |
| CT | 🔴 55% | Agregar tests, migrar `CT` → `Ct` |
| RV | 🟢 90% | - |

---

## 11. Cambios Requeridos

### 11.1 Acciones Inmediatas (CRÍTICAS)

1. **Eliminar WebSocket de PF**
   ```bash
   rm common/pf-module/src/main/java/.../websocket/PfWebSocketConfig.java
   rm common/pf-module/src/main/java/.../websocket/PfWebSocketHandler.java
   ```

2. **Refactorizar PfNotificationService**
   - Eliminar dependencia de PfWebSocketHandler
   - Usar TB Notification System para Email/SMS

3. **Crear Tests para DR y CT**
   - Mínimo: AssetService, principales Domain Services

### 11.2 Acciones a Mediano Plazo

1. **Migrar CT naming** de `CT` a `Ct`
2. **Estandarizar excepciones** en todos los módulos
3. **Documentar Asset Types** en archivo centralizado

---

## Apéndice A: Asset Types Registry

```java
// Propuesta: crear archivo centralizado
public final class NexusAssetTypes {

    // PF Module
    public static final String PF_WELL = "pf_well";
    public static final String PF_WELLPAD = "pf_wellpad";
    public static final String PF_FLOW_STATION = "pf_flow_station";
    public static final String PF_ESP_SYSTEM = "pf_esp_system";
    public static final String PF_PCP_SYSTEM = "pf_pcp_system";
    public static final String PF_GAS_LIFT_SYSTEM = "pf_gas_lift_system";
    public static final String PF_ROD_PUMP_SYSTEM = "pf_rod_pump_system";

    // DR Module
    public static final String DR_RIG = "dr_rig";
    public static final String DR_BHA = "dr_bha";

    // CT Module
    public static final String CT_UNIT = "ct_unit";
    public static final String CT_REEL = "ct_reel";

    // RV Module
    public static final String RV_BASIN = "rv_basin";
    public static final String RV_FIELD = "rv_field";
    public static final String RV_RESERVOIR = "rv_reservoir";
    public static final String RV_ZONE = "rv_zone";
    public static final String RV_WELL = "rv_well";
    // ... etc
}
```

---

**Documento creado**: 2026-02-04
**Autor**: Architecture Team
**Próxima revisión**: Después de implementar cambios críticos
