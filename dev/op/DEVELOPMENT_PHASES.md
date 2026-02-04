# DEVELOPMENT PHASES - Detailed Implementation Plan

**Proyecto**: Nexus PF & PO Modules
**Versión**: 2.1 (Arquitectura ThingsBoard Core)
**Fecha**: 2026-02-03
**Última Actualización**: 2026-02-03

Este documento detalla el plan de desarrollo por fases, con user stories, tasks técnicos, y criterios de aceptación específicos para cada sprint.

> **Nota de Arquitectura v2.0**: Los módulos PF y PO utilizan la arquitectura ThingsBoard Core
> (Assets, Attributes, ts_kv, Alarm System) en lugar de tablas custom. Solo se crean tablas
> específicas para datos que requieren workflows complejos (po_optimization_result, po_recommendation).

---

## 🚀 ESTADO DE IMPLEMENTACIÓN ACTUAL

### Módulo PF (Production Facilities) - ~70% Backend

| Componente | Estado | Archivos | Notas |
|------------|--------|----------|-------|
| DTOs (Well, Wellpad, ESP, PCP, GasLift, RodPump) | ✅ | 14 | Completo |
| Enums (WellStatus, LiftSystemType, etc.) | ✅ | 5 | Completo |
| Wrapper Services (PfAssetService, PfAttributeService) | ✅ | 2 | Completo |
| Domain Services (Well, Wellpad, FlowStation, ESP, PCP, GasLift, RodPump) | ✅ | 10 | Completo |
| Telemetry & Alarm Services | ✅ | 5 | Usando TB Core |
| Controllers REST | ✅ | 12 | /api/nexus/pf/* |
| MQTT Integration | ⚠️ | 2 | Básico |
| WebSocket | ⚠️ | 2 | Básico |
| Unit Tests | ❌ | 0 | Pendiente |

**Total: 56 archivos Java, ~11,329 LOC**

### Módulo PO (Production Optimization) - ~60% Backend

| Componente | Estado | Archivos | Notas |
|------------|--------|----------|-------|
| DTOs (HealthScore, Recommendation, Optimization, ESP, GasLift, KPI) | ✅ | 8 | Completo |
| Enums (RecommendationStatus, OptimizationType, HealthLevel) | ✅ | 3 | Completo |
| JPA Entities (PoOptimizationResult, PoRecommendation) | ✅ | 2 | Tablas custom |
| Repositories | ✅ | 2 | Spring Data JPA |
| Wrapper Services (PoAssetService, PoAttributeService) | ✅ | 2 | Completo |
| Domain Services (Health, Recommendation, Optimization) | ✅ | 4 | Completo |
| ESP Frequency Optimizer | ✅ | 1 | Algoritmo funcional |
| Gas Lift Allocator | ⚠️ | 0 | DTO ready, falta algoritmo |
| Controllers REST | ✅ | 3 | /api/nexus/po/* |
| Exception Handling | ✅ | 4 | Completo |
| Unit Tests | ❌ | 0 | Pendiente |

**Total: 27 archivos Java, ~4,481 LOC**

---

## 📋 Índice

1. [Fase 0: Planning & Setup](#fase-0)
2. [Fase 1: PF Module Base](#fase-1)
3. [Fase 2: Lift Systems](#fase-2)
4. [Fase 3: PO Module Base](#fase-3)
5. [Fase 4: Advanced Analytics](#fase-4)
6. [Fase 5: Automation](#fase-5)

---

## FASE 0: Planning & Setup (1 mes) {#fase-0}

**Duración**: 1 Feb - 10 Mar 2026
**Team**: PM, Tech Leads (2), Architect
**Budget**: $150K

### Objetivos
- Finalizar diseño técnico detallado
- Setup completo de infraestructura
- Contratación y onboarding de equipo
- Procurement de herramientas

### Deliverables Checklist

#### Week 1-2: Documentation & Architecture
- [x] Crear toda la documentación en `/dev`
  - [x] MASTER_PLAN.md
  - [x] ROADMAP.md
  - [x] TECHNICAL_STACK.md
  - [x] DEVELOPMENT_PHASES.md
  - [ ] PF_MODULE_SPEC.md
  - [ ] PO_MODULE_SPEC.md
  - [ ] DATA_MODEL.md
  - [ ] API_SPECIFICATION.md
  - [ ] INTEGRATION_ARCHITECTURE.md

- [ ] Architecture review con CTO
  - [ ] Validar decisiones de tecnología
  - [ ] Aprobar patrones de arquitectura (ThingsBoard Core)
  - [ ] Definir non-functional requirements

- [ ] Budget approval por Steering Committee
  - [ ] Presentar business case
  - [ ] Aprobar $3.2M investment
  - [ ] Definir approval process para gastos

#### Week 3-4: Infrastructure Setup
- [ ] **Environments Setup**
  ```bash
  # Dev Environment
  - EC2/VM: 2 × t3.xlarge (backend)
  - ThingsBoard: PostgreSQL + ts_kv tables
  - ElastiCache Redis: cache.t3.micro
  - MSK Kafka: kafka.m5.large × 2

  # Staging Environment
  - Similar to dev, isolated network

  # Production Environment
  - Auto-scaling group: 3-10 instances
  - PostgreSQL Multi-AZ (ThingsBoard DB)
  - Redis cluster (3 nodes)
  - Kafka cluster (3 brokers)
  ```

- [ ] **CI/CD Pipeline**
  - [ ] GitHub repo creado: `nexus-pf-po-modules`
  - [ ] GitHub Actions workflows configurados
  - [ ] Automated testing pipeline
  - [ ] Docker registry setup
  - [ ] Kubernetes cluster setup (EKS/AKS/GKE)

- [ ] **ThingsBoard Configuration**
  - [ ] ThingsBoard instalado y configurado
  - [ ] Asset Types definidos (pf_well, pf_wellpad, etc.)
  - [ ] Asset Profiles con Alarm Rules configurados
  - [ ] Rule Chains creadas para PF/PO
  - [ ] Backup policy definido (daily, 30 days retention)

- [ ] **Kafka**
  - [ ] Topics creados:
    ```
    pf.telemetry.raw (partitions: 10, replication: 3)
    pf.telemetry.validated (partitions: 10, replication: 3)
    po.recommendations (partitions: 5, replication: 3)
    ```
  - [ ] Consumer groups configurados
  - [ ] Monitoring con Kafka Manager

- [ ] **Monitoring & Observability**
  - [ ] Grafana instalado
  - [ ] Prometheus configurado
  - [ ] Dashboards base creados
  - [ ] Alerting rules configurados
  - [ ] Log aggregation (ELK o Loki)

- [ ] **Code Quality Tools**
  - [ ] SonarQube server setup
  - [ ] Quality gates configurados
  - [ ] Code coverage threshold: 80%

#### Week 5: Team & Kickoff
- [ ] **Hiring Completed** (al menos 60%)
  - [ ] 1 Backend Developer hired
  - [ ] 1 Frontend Developer hired
  - [ ] 1 Data Engineer hired
  - [ ] 1 QA Engineer hired

- [ ] **Onboarding**
  - [ ] Access to all systems granted
  - [ ] Development environment setup guide
  - [ ] Codebase walkthrough session
  - [ ] Architecture presentation (ThingsBoard Core pattern)

- [ ] **Kickoff Meeting**
  - [ ] Agenda preparada
  - [ ] Stakeholders invitados
  - [ ] Project vision presentada
  - [ ] Q&A session

- [ ] **Sprint 0 Activities**
  - [ ] Backlog creado en Jira/Linear (100+ stories)
  - [ ] Top 50 stories priorizadas
  - [ ] Story sizing session (planning poker)
  - [ ] Sprint 1 planning completado
  - [ ] Code standards document
  - [ ] Git branching strategy definida

### Exit Criteria Phase 0
- ✅ Toda la documentación aprobada
- ✅ Infraestructura operacional (dev + staging)
- ✅ CI/CD pipeline funcional (green build)
- ✅ Al menos 60% del team contratado
- ✅ Backlog con 50+ stories ready
- ✅ Sprint 1 ready to start

---

## FASE 1: PF Module Base (4 meses) {#fase-1}

**Duración**: 11 Mar - 31 Jul 2026 (10 sprints)
**Team**: 8 personas
**Budget**: $600K
**Go-Live**: Alpha pilot con 5 pozos

### Sprint 1-2: DTOs, Wrapper Services & Rule Chain Foundation

#### Sprint 1 (Mar 11-24)

**Sprint Goal**: Crear fundamentos del módulo PF usando ThingsBoard Core

**User Stories**:

**[PF-001] Crear PfWellDto y PfAssetService**
```
Como developer
Quiero crear DTOs y Wrapper Services para pozos
Para poder gestionar pozos productores usando TB Assets

Acceptance Criteria:
- PfWellDto creado con constantes ASSET_TYPE y ATTR_*
- PfAssetService como wrapper de TB AssetService
- PfAttributeService como wrapper de TB AttributesService
- Relación con Wellpad via TB Relations (Contains)
- Relación con RvWell via TB Relations (BelongsTo)
- Status enum stored as SERVER_SCOPE attribute

DTOs Pattern:
```java
@Data
@Builder
public class PfWellDto {
    public static final String ASSET_TYPE = "pf_well";
    public static final String ATTR_API_NUMBER = "api_number";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_LATITUDE = "latitude";
    public static final String ATTR_LONGITUDE = "longitude";

    private UUID assetId;  // TB Asset ID
    private String name;
    private String apiNumber;
    private WellStatus status;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
```

Technical Tasks:
1. Create PfWellDto.java with ASSET_TYPE and ATTR_* constants
2. Create PfAssetService.java (wrapper for TB AssetService)
3. Create PfAttributeService.java (wrapper for TB AttributesService)
4. Create Asset Profile "pf_well" in ThingsBoard
5. Write unit tests for wrapper services
6. Integration test for CRUD operations

Estimation: 5 story points
Assigned to: Backend Dev 1
```

**[PF-002] Crear Validación y Mappers**
```
Como developer
Quiero tener validación y mappers para DTOs
Para convertir entre DTOs y datos de TB Assets/Attributes

Acceptance Criteria:
- Validación con Jakarta Validation en DTOs
- Mapper methods en PfWellService para Asset+Attributes → PfWellDto
- Unit tests para mappers

Technical Tasks:
1. Add validation annotations to PfWellDto (@NotNull, @Size, etc)
2. Create buildAttributeMap() method in PfWellService
3. Create mapToDto() method to convert Asset+Attributes to DTO
4. Write unit tests for mapping logic

Estimation: 3 story points
Assigned to: Backend Dev 1
```

**[PF-003] Implementar PfWellService con lógica de negocio**
```
Como developer
Quiero servicios para operaciones CRUD usando TB Core
Para encapsular lógica de negocio

Service Pattern:
```java
@Service
@RequiredArgsConstructor
public class PfWellService {
    private final PfAssetService pfAssetService;
    private final PfAttributeService pfAttributeService;
    private final RelationService relationService;

    public PfWellDto createWell(UUID tenantId, PfWellDto dto) {
        Asset asset = pfAssetService.createAsset(tenantId,
            PfWellDto.ASSET_TYPE, dto.getName());
        dto.setAssetId(asset.getId().getId());
        pfAttributeService.saveServerAttributes(dto.getAssetId(),
            buildAttributeMap(dto));
        return dto;
    }

    public PfWellDto findById(UUID wellAssetId) {
        Asset asset = pfAssetService.getAssetById(wellAssetId);
        List<AttributeKvEntry> attrs = pfAttributeService
            .getServerAttributes(wellAssetId);
        return mapToDto(asset, attrs);
    }
}
```

Acceptance Criteria:
- PfWellService implementation using wrapper services
- Métodos: create, findById, findByTenant, update, delete
- Validación de business rules
- Manejo de TB Relations
- Unit tests con Mockito

Technical Tasks:
1. Create PfWellService interface
2. Implement PfWellService using wrapper services
3. Add business validation logic
4. Implement relation management (wellpad contains well)
5. Write unit tests (coverage > 80%)

Estimation: 5 story points
Assigned to: Backend Dev 1
```

**[PF-004] Crear REST Controllers**
```
Como API consumer
Quiero endpoints REST para gestionar pozos
Para poder integrar con frontend

Acceptance Criteria:
- PfWellController creado
- Endpoints: POST /wells, GET /wells/{id}, GET /wells, PUT /wells/{id}, DELETE /wells/{id}
- Response con DTOs
- HTTP status codes correctos
- Exception handling
- Swagger documentation

Technical Tasks:
1. Create PfWellController
2. Implement CRUD endpoints using PfWellService
3. Add @ApiOperation annotations
4. Create ControllerAdvice for exception handling
5. Integration tests para cada endpoint

Estimation: 8 story points
Assigned to: Backend Dev 1
```

**[PF-005] Configurar Rule Chain para PF Telemetry**
```
Como data engineer
Quiero Rule Chain configurada para procesar telemetría PF
Para optimizar flujo de datos usando infraestructura TB nativa

Architecture:
```
MQTT Device → TB Transport → Rule Chain:
  ├── Message Type Switch
  ├── PfDataQualityNode (custom)
  ├── PfAlarmEvaluationNode (custom) → TB Alarm System
  └── Save Timeseries (ts_kv native)
```

Acceptance Criteria:
- Rule Chain "PF Telemetry Processing" creada
- Custom Rule Node PfDataQualityNode implementado
- Datos fluyen a ts_kv nativo de TB
- Latency < 1 segundo end-to-end
- Performance test: insert 10K rows/sec

Technical Tasks:
1. Create Rule Chain "PF Telemetry Processing" in TB
2. Implement PfDataQualityNode (custom Rule Node)
3. Configure Save Timeseries node
4. Configure relations to PfAlarmEvaluationNode
5. Performance testing
6. Create Grafana dashboard for Rule Chain metrics

Estimation: 8 story points
Assigned to: Data Engineer
```

**Definition of Done Sprint 1**:
- [ ] All stories completed and deployed to dev
- [ ] Code coverage > 80%
- [ ] Code review approved by Tech Lead
- [ ] SonarQube quality gate passed
- [ ] Integration tests passing
- [ ] Swagger documentation updated
- [ ] Demo prepared for Sprint Review

#### Sprint 2 (Mar 25 - Apr 7)

**Sprint Goal**: Completar DTOs y servicios para entidades relacionadas

**User Stories**:

**[PF-006] Crear PfWellpadDto y PfWellpadService**
```
Como developer
Quiero DTOs y servicios para Wellpads
Para gestionar macollas como TB Assets

Acceptance Criteria:
- PfWellpadDto con ASSET_TYPE = "pf_wellpad"
- PfWellpadService usando wrapper services
- TB Relation: Wellpad Contains Wells
- Geographic coordinates como SERVER_SCOPE attributes
- Capacity attributes

DTO Pattern:
```java
@Data @Builder
public class PfWellpadDto {
    public static final String ASSET_TYPE = "pf_wellpad";
    public static final String ATTR_LATITUDE = "latitude";
    public static final String ATTR_LONGITUDE = "longitude";
    public static final String ATTR_CAPACITY = "capacity";

    private UUID assetId;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer capacity;
}
```

Technical Tasks:
1. Create PfWellpadDto.java
2. Create PfWellpadService.java
3. Configure TB Relation "Contains" for wellpad-wells
4. Write unit and integration tests
```

**[PF-007] Crear PfFlowStationDto y PfFlowStationService**
```
Como developer
Quiero DTOs y servicios para Flow Stations
Para gestionar estaciones de flujo como TB Assets

Acceptance Criteria:
- PfFlowStationDto con ASSET_TYPE = "pf_flow_station"
- Processing capacity como attribute
- Equipment list como JSON attribute
- TB Relations para conectar con wellpads

Technical Tasks:
1. Create PfFlowStationDto.java
2. Create PfFlowStationService.java
3. Configure TB Relations
4. Write unit and integration tests
```

**[PF-008] Batch import de pozos**
```
Como operador
Quiero importar pozos desde CSV
Para poblar sistema con datos existentes

Acceptance Criteria:
- CSV import functionality usando batch Asset creation
- Validación y error handling
- Bulk attribute save optimization
- Progress reporting

Technical Tasks:
1. Create PfImportService with batch processing
2. Implement CSV parser with validation
3. Batch create assets and attributes
4. Create error report generation
5. Integration tests
```

**[PF-009] Configuración de Alarm Rules en Asset Profiles**
```
Como operador
Quiero límites operacionales configurados en Asset Profiles
Para recibir alarmas automáticas via TB Alarm System

Architecture (NO custom table):
```
Asset Profile "pf_well":
  └── Alarm Rules:
      ├── High Pressure (severity: CRITICAL)
      ├── Low Pressure (severity: WARNING)
      ├── High Temperature (severity: CRITICAL)
      └── Vibration Alert (severity: MAJOR)
```

Acceptance Criteria:
- Asset Profile "pf_well" con Alarm Rules
- Por variable, por severity
- Threshold values configurable via TB UI
- Alarmas generadas automáticamente por TB Alarm System

Technical Tasks:
1. Configure Asset Profile "pf_well" in TB
2. Add Alarm Rules for operational limits
3. Configure severity levels (CRITICAL, MAJOR, WARNING)
4. Test alarm generation with sample telemetry
5. Create documentation for operators
```

**Definition of Done Sprint 2**:
- [ ] All CRUD DTOs/Services complete (Well, Wellpad, FlowStation)
- [ ] Batch import functional
- [ ] Asset Profiles with Alarm Rules configured
- [ ] API documentation complete
- [ ] Load test: 100 concurrent requests

### Sprint 3-4: SCADA Integration & Telemetry via Rule Engine

#### Sprint 3 (Apr 8-21)

**Sprint Goal**: Integrar con MQTT y procesar telemetría via Rule Engine

**[PF-010] MQTT Integration via TB Transport**
```
Como sistema
Quiero conectarme al broker MQTT usando TB Transport
Para recibir telemetría en tiempo real

Architecture:
```
Field MQTT Broker → TB MQTT Transport → Rule Engine → ts_kv
```

Acceptance Criteria:
- TB MQTT Transport configurado
- Device provisioning para pozos
- Connection segura TLS/SSL
- QoS 1 (at least once)
- Monitoring via TB dashboards

Technical Tasks:
1. Configure TB MQTT Transport
2. Create device provisioning scripts
3. Configure TLS certificates
4. Create monitoring dashboard
5. Integration test con mock broker
6. Add metrics (connected, messages_received, errors)

Estimation: 13 story points
Assigned to: Backend Dev 1 + Data Engineer
```

**[PF-011] Topic Configuration per Well**
```
Como operador
Quiero que cada pozo tenga su topic MQTT
Para separar streams de telemetría

Topic Structure (TB format):
v1/devices/{deviceToken}/telemetry

Payload Format:
{
  "ts": 1704816000000,
  "values": {
    "frequency": 52.3,
    "current": 45.2,
    "temperature_motor": 295,
    "pip": 156,
    "discharge_pressure": 1850
  }
}

Acceptance Criteria:
- Topic pattern via TB device token
- Payload validation in Rule Chain
- Automatic device-to-asset relation
```

**[PF-012] Custom Rule Nodes for PF Processing**
```
Como sistema
Quiero Rule Nodes custom para procesamiento PF
Para aplicar lógica específica del dominio

Custom Rule Nodes:
1. PfDataQualityNode - Validación de calidad
2. PfAlarmEvaluationNode - Evaluación de alarmas por reglas de negocio
3. PfTelemetryEnrichmentNode - Enriquecimiento de datos

Acceptance Criteria:
- 3 custom Rule Nodes implementados
- Integrados en Rule Chain "PF Telemetry Processing"
- Datos almacenados en ts_kv nativo
- Latency < 1 segundo end-to-end
- Error handling con logging

Technical Tasks:
1. Create PfDataQualityNode (extends TbAbstractRuleNode)
2. Create PfAlarmEvaluationNode
3. Create PfTelemetryEnrichmentNode
4. Register nodes in TB plugin system
5. Configure Rule Chain
6. Performance testing

Estimation: 13 story points
```

**[PF-013] Data Quality Validator Node**
```
Como sistema
Quiero validar calidad de datos en Rule Chain
Para descartar datos erróneos

PfDataQualityNode Implementation:
```java
@RuleNode(
    type = ComponentType.FILTER,
    name = "PF Data Quality Validator"
)
public class PfDataQualityNode extends TbAbstractRuleNode {
    // Range validation (ej: temperature 0-500°F)
    // Rate of change validation
    // Missing data detection
    // Quality score calculation
}
```

Quality Score as Attribute:
- Stored as SERVER_SCOPE attribute "data_quality_score"
- 1.0 = perfect data, 0.0 = invalid

Acceptance Criteria:
- DataQualityNode implementado
- Quality score saved as attribute
- Low quality data routed to review queue
- Dashboard de data quality

Estimation: 8 story points
```

#### Sprint 4 (Apr 22 - May 5)

**[PF-014] Real-time Dashboard Backend**
```
Como operador
Quiero ver datos en tiempo real
Para monitorear pozos

Architecture (using TB native):
```
ts_kv → TB WebSocket API → Frontend Dashboard
```

Acceptance Criteria:
- TB WebSocket subscriptions configuradas
- Latest values via attribute subscriptions
- Telemetry history via ts_kv queries
- Performance optimized

Technical Tasks:
1. Configure TB WebSocket subscriptions
2. Create subscription helper services
3. Implement caching strategy
4. Performance testing
```

**[PF-015] Telemetry Query API**
```
Como integrador
Quiero API para consultar telemetría histórica
Para análisis y reportes

Endpoints (via TB API extension):
- GET /api/pf/wells/{id}/telemetry/latest
- GET /api/pf/wells/{id}/telemetry?from=&to=&keys=
- GET /api/pf/wells/{id}/telemetry/aggregated?interval=&agg=

Acceptance Criteria:
- Endpoints usando TB TelemetryService
- Aggregation queries (avg, min, max)
- Performance optimized
```

**Definition of Done Sprint 3-4**:
- [ ] MQTT integration via TB Transport functional
- [ ] Telemetry flowing to ts_kv
- [ ] Custom Rule Nodes operativos
- [ ] Data quality validation working
- [ ] Latency < 1 segundo medida
- [ ] Load test: 100 pozos @ 1 msg/sec
- [ ] Dashboard backend ready

### Sprint 5-6: Alarm System via TB Alarms

#### Sprint 5 (May 6-19)

**[PF-016] Alarm Rules in Asset Profiles**
```
Como operador
Quiero alarmas configuradas en Asset Profiles
Para recibir notificaciones automáticas

Architecture (NO custom table):
```
TB Asset Profile "pf_well":
  └── Alarm Rules (JSON config):
      {
        "highPressure": {
          "condition": "pressure > threshold",
          "severity": "CRITICAL",
          "propagate": true
        }
      }
```

Acceptance Criteria:
- Asset Profiles con Alarm Rules configurados
- Severities: CRITICAL, MAJOR, WARNING, INDETERMINATE
- Alarm propagation to parent assets
- Alarm lifecycle management via TB
```

**[PF-017] Custom Alarm Evaluation Node**
```
Como sistema
Quiero evaluación de alarmas con lógica de negocio
Para generar alarmas contextuales

PfAlarmEvaluationNode:
```java
@RuleNode(type = ComponentType.ACTION, name = "PF Alarm Evaluator")
public class PfAlarmEvaluationNode extends TbAbstractRuleNode {
    // Evaluates business rules
    // Creates TB Alarms via AlarmService
    // Supports complex conditions
}
```

Acceptance Criteria:
- Custom alarm evaluation logic
- Integration with TB Alarm System
- Support for multi-condition alarms
```

**[PF-018] Alarm Notification Service**
```
Como operador
Quiero recibir notificaciones de alarmas
Para responder rápidamente

Architecture:
```
TB Alarm System → Rule Chain → Notification Targets:
  ├── Email (via TB notification)
  ├── SMS (via TB notification)
  └── WebSocket (real-time UI)
```

Acceptance Criteria:
- Email notifications configuradas
- SMS notifications configuradas
- Real-time updates via WebSocket
- Notification templates
```

### Sprint 7-8: Frontend Dashboards

**[PF-019] TB Dashboard Templates**
```
Como operador
Quiero dashboards preconstruidos
Para monitorear operaciones

TB Dashboards:
1. Well Overview - Single well monitoring
2. Wellpad Overview - Multiple wells
3. Alarm Console - Active alarms
4. Production Summary - KPIs

Acceptance Criteria:
- 4 dashboard templates creados
- Widgets configurados
- Real-time data subscriptions
- Mobile-responsive
```

### Sprint 9-10: Alpha Pilot

**[PF-020] Pilot Deployment**
```
Como equipo
Quiero desplegar en piloto
Para validar con datos reales

Pilot Scope:
- 5 pozos en producción
- 1 wellpad
- Full telemetry integration
- Alarm monitoring

Acceptance Criteria:
- 5 wells configured as TB Assets
- Telemetry flowing via Rule Chain
- Alarms generating correctly
- User training completed
```

---

## FASE 2: Lift Systems (3 meses) {#fase-2}

**Duración**: 1 Ago - 31 Oct 2026
**Focus**: ESP, PCP, Gas Lift, Rod Pump como TB Assets

### Key Stories

**[PF-021] ESP System as TB Asset**
```
DTOs Pattern:
```java
@Data @Builder
public class PfEspSystemDto {
    public static final String ASSET_TYPE = "pf_esp_system";
    public static final String ATTR_MOTOR_HP = "motor_hp";
    public static final String ATTR_STAGES = "stages";
    public static final String ATTR_FREQUENCY = "frequency";
    // ...
}
```

Relation: Well HasSystem ESP
Telemetry: Stored in ts_kv
Attributes: Motor specs, performance params
```

**[PF-022] PCP System as TB Asset**
- Similar pattern to ESP
- ASSET_TYPE = "pf_pcp_system"
- Specific attributes for PCP

**[PF-023] Gas Lift System as TB Asset**
- ASSET_TYPE = "pf_gas_lift_system"
- Injection parameters as attributes

**[PF-024] Rod Pump System as TB Asset**
- ASSET_TYPE = "pf_rod_pump_system"
- Dynamometer data via telemetry

---

## FASE 3: PO Module Base (4 meses) {#fase-3}

**Duración**: 1 Nov 2026 - 28 Feb 2027
**Focus**: Optimización usando TB Core + Tablas Custom justificadas

### Architecture Decision

```
PO Module Storage:
├── TB Core (for majority of data):
│   ├── Health Scores → SERVER_SCOPE attributes on well assets
│   ├── Optimization Status → SERVER_SCOPE attributes
│   └── Telemetry Analysis → ts_kv
│
└── Custom Tables (ONLY for complex workflows):
    ├── pf_optimization_result (versioned results)
    └── pf_recommendation (workflow states)
```

### Key Stories

**[PO-001] PoAssetService y PoAttributeService**
```
Como developer
Quiero wrapper services para PO
Para acceder a TB Core de forma consistente

Services:
```java
@Service
public class PoAssetService {
    // Wrapper for TB AssetService
    // Access to pf_well, pf_esp_system assets
}

@Service
public class PoAttributeService {
    // Save/read health scores as attributes
    // Save/read optimization status as attributes
}
```
```

**[PO-002] Health Score as TB Attributes**
```
Como sistema
Quiero health scores guardados como atributos
Para consulta rápida sin tablas custom

Pattern (NO custom table):
```java
public static final String ATTR_HEALTH_SCORE = "health_score";
public static final String ATTR_FAILURE_PROBABILITY = "failure_probability";

public void saveHealthScore(UUID wellAssetId, HealthScoreDto score) {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put(ATTR_HEALTH_SCORE, score.getScore());
    attrs.put(ATTR_FAILURE_PROBABILITY, score.getFailureProbability());
    poAttributeService.saveServerAttributes(wellAssetId, attrs);
}
```
```

**[PO-003] Custom Table: pf_optimization_result**
```
Como sistema
Quiero tabla custom para resultados de optimización
Porque requiere versionado y queries complejos

Justification:
- Multiple versions per well
- Complex queries for ML training
- Historical analysis requirements

Entity:
```java
@Entity
@Table(name = "pf_optimization_result")
public class PfOptimizationResult {
    @Id private UUID id;
    private UUID tenantId;
    private UUID wellAssetId;  // Reference to TB Asset
    private String optimizationType;
    private JsonNode parameters;
    private JsonNode results;
    private Long timestamp;
}
```
```

**[PO-004] Custom Table: pf_recommendation**
```
Como sistema
Quiero tabla custom para recomendaciones
Porque tiene workflow de estados

Justification:
- State machine: PENDING → APPROVED → EXECUTED
- Audit trail required
- Complex business logic

Entity:
```java
@Entity
@Table(name = "pf_recommendation")
public class PfRecommendation {
    @Id private UUID id;
    private UUID tenantId;
    private UUID wellAssetId;  // Reference to TB Asset
    private String type;
    private String status;  // PENDING, APPROVED, EXECUTED, REJECTED
    private JsonNode parameters;
    private UUID approvedBy;
    private Long approvedTime;
}
```
```

---

## FASE 4: Advanced Analytics (6 meses) {#fase-4}

**Duración**: 1 Mar - 31 Ago 2027
**Focus**: ML/AI con datos de TB Core

### Key Stories

**[PO-010] ML Pipeline with TB Data**
```
Architecture:
```
ts_kv (TB) → Kafka Export → ML Platform → Results:
  ├── Health Score → attribute_kv (TB)
  └── Predictions → pf_optimization_result (custom)
```
```

**[PO-011] Predictive Failure Models**
- Read telemetry from ts_kv
- Train models externally
- Store predictions as attributes

---

## FASE 5: Automation (3 meses) {#fase-5}

**Duración**: 1 Sep - 30 Nov 2027
**Focus**: Closed-loop control via Rule Engine

### Key Stories

**[PO-020] Automated Setpoint Optimization**
```
Architecture:
```
TB Rule Chain:
  ├── Read current telemetry
  ├── PfOptimizationNode (custom)
  ├── Approval workflow (if required)
  └── Send RPC to device
```
```

---

## 📊 Velocity Tracking

### Expected Velocity per Sprint
- **Sprints 1-3**: 25-30 story points (ramping up)
- **Sprints 4-10**: 35-40 story points (steady state)
- **Sprints 11+**: 40-45 story points (team matured)

### Sprint Burndown Template
```
Day 1:  100% remaining
Day 3:   85% remaining
Day 5:   70% remaining
Day 7:   50% remaining
Day 9:   30% remaining
Day 10:  10% remaining
Day 11:   0% remaining (buffer)
```

---

## 🔄 Sprint Ceremony Templates

### Daily Standup (15 min)
```
Format:
- What I did yesterday
- What I'll do today
- Any blockers

Rules:
- Max 2 min per person
- Parking lot para discusiones largas
- Scrum Master tracks impediments
```

### Sprint Planning (4 hours)
```
Part 1 (2 hours): What
- PO presents top priority stories
- Team asks clarifying questions
- Team commits to sprint goal

Part 2 (2 hours): How
- Team breaks down stories into tasks
- Estimate hours per task
- Assign tasks to team members
- Identify dependencies
```

### Sprint Review / Demo (2 hours)
```
Agenda:
- Sprint goal recap
- Demo of completed features
- Feedback from stakeholders
- Review of metrics (velocity, quality)
- Acceptance of deliverables
```

### Sprint Retrospective (1.5 hours)
```
Format: Start/Stop/Continue

Start doing:
- [Things we should start]

Stop doing:
- [Things that didn't work]

Continue doing:
- [Things that worked well]

Action items for next sprint
```

---

## 📈 Progress Tracking Dashboard

### Weekly Metrics
| Metric | Week 1 | Week 2 | Week 3 | Week 4 |
|--------|--------|--------|--------|--------|
| Stories Completed | | | | |
| Story Points Completed | | | | |
| Velocity | | | | |
| Bugs Found | | | | |
| Bugs Fixed | | | | |
| Code Coverage % | | | | |
| SonarQube Issues | | | | |

---

## Decisión Arquitectónica: Por qué ThingsBoard Core

### Beneficios de usar TB Core vs Tablas Custom

| Aspecto | TB Core | Tablas Custom |
|---------|---------|---------------|
| **Time-to-market** | Rápido | Lento |
| **Mantenimiento** | Bajo | Alto |
| **Escalabilidad** | Probada | Por implementar |
| **UI/Dashboards** | Incluidos | Desarrollar |
| **Alarmas** | Sistema completo | Implementar |
| **Queries temporales** | Optimizados | Implementar |

### Cuándo usar Tablas Custom

Solo crear tablas custom cuando:
1. ✅ Se requiere versionado histórico (pf_optimization_result)
2. ✅ Hay workflow de estados complejo (pf_recommendation)
3. ✅ Se necesitan JOINs SQL complejos para ML
4. ❌ NO para datos que TB ya maneja bien (telemetría, atributos, alarmas)

---

**Document Maintenance**:
- Update after each sprint
- Add lessons learned
- Track blockers and resolutions

**Last Updated**: 2026-02-03
**Next Review**: After Sprint 1 completion
