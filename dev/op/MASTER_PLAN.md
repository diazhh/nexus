# MASTER PLAN - Production Facilities & Optimization Modules

**Proyecto**: Nexus PF & PO Modules
**Versión**: 2.0 (Arquitectura ThingsBoard Core)
**Fecha**: Febrero 2026
**Estado**: Plan de Diseño
**Preparado para**: Hector Diaz

---

## 📋 Tabla de Contenidos

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Visión y Objetivos](#visión-y-objetivos)
3. [Contexto del Negocio](#contexto-del-negocio)
4. [Arquitectura de Solución](#arquitectura-de-solución)
5. [Alcance del Proyecto](#alcance-del-proyecto)
6. [Módulos a Implementar](#módulos-a-implementar)
7. [Integración con Módulos Existentes](#integración-con-módulos-existentes)
8. [Estrategia de Implementación](#estrategia-de-implementación)
9. [Plan de Recursos](#plan-de-recursos)
10. [Gestión de Riesgos](#gestión-de-riesgos)
11. [Plan de Calidad](#plan-de-calidad)
12. [Métricas de Éxito](#métricas-de-éxito)
13. [Governance](#governance)

---

## 1. Resumen Ejecutivo

### 1.1 Problema a Resolver

Las operaciones de producción petrolera enfrentan desafíos críticos:

- **Falta de visibilidad en tiempo real** de equipos de levantamiento artificial (ESP, PCP, Gas Lift)
- **Operación subóptima** de pozos productores (producción 15-30% por debajo del potencial)
- **Fallas imprevistas** de equipos costosos (ESP $150K-300K por falla)
- **Decisiones reactivas** en lugar de predictivas
- **Datos dispersos** en múltiples sistemas sin integración

### 1.2 Solución Propuesta

Implementar dos módulos complementarios en la plataforma Nexus:

**Módulo PF (Production Facilities)**
- Sistema de monitoreo en tiempo real de infraestructura de superficie
- Integración con SCADA para telemetría continua
- Visualización de pozos, macollas y estaciones de flujo
- Gestión de alarmas y eventos

**Módulo PO (Production Optimization)**
- Optimización automática de parámetros operacionales
- Predicción de fallas con Machine Learning
- Recomendaciones inteligentes para maximizar producción
- KPIs de producción y económicos

### 1.3 Decisión Arquitectónica Clave

> **Arquitectura ThingsBoard Core**: Los módulos PF y PO utilizan la infraestructura nativa de
> ThingsBoard (Assets, Attributes, ts_kv, Alarm System, Rule Engine) en lugar de crear tablas
> custom duplicadas. Esto sigue el patrón exitoso de los módulos CT y RV, reduciendo código
> duplicado y aprovechando las capacidades probadas de TB.

### 1.4 Beneficios Esperados

| Categoría | Beneficio | Impacto Estimado |
|-----------|-----------|------------------|
| **Producción** | Incremento de producción | +3% a +8% |
| **Costos** | Reducción de costos de levantamiento | -10% a -20% |
| **Equipos** | Extensión de vida útil de equipos | +20% a +40% |
| **Downtime** | Reducción de fallas no programadas | -30% a -50% |
| **Eficiencia** | Automatización de decisiones rutinarias | 70% más rápido |
| **ROI** | Retorno de inversión | 300%+ en 18 meses |

### 1.5 Inversión Estimada

| Concepto | Monto (USD) |
|----------|-------------|
| Desarrollo de Software | $800K - $1.2M |
| Infraestructura (Hardware/Cloud) | $150K - $250K |
| Licencias de Software | $50K - $100K |
| Capacitación | $30K - $50K |
| Contingencia (15%) | $155K - $240K |
| **TOTAL** | **$1.185M - $1.840M** |

**ROI esperado**: $3.5M - $5.5M en ahorros y producción adicional a 3 años

---

## 2. Visión y Objetivos

### 2.1 Visión

> "Crear una plataforma inteligente y autónoma que permita a los operadores de producción petrolera maximizar la extracción de hidrocarburos de manera segura, eficiente y sostenible, mediante monitoreo en tiempo real y optimización automática basada en Machine Learning."

### 2.2 Objetivos Estratégicos

#### Objetivo 1: Digitalización Completa de Operaciones de Producción
- Integrar 100% de los pozos productores en el sistema
- Capturar telemetría en tiempo real de todos los equipos críticos
- Eliminar monitoreo manual y hojas de cálculo

#### Objetivo 2: Optimización Operacional
- Implementar optimizadores automáticos para ESP, PCP y Gas Lift
- Reducir tiempo de respuesta a problemas de horas a minutos
- Automatizar decisiones de rutina (60% de ajustes operacionales)

#### Objetivo 3: Mantenimiento Predictivo
- Predecir fallas de equipos con 85%+ de precisión
- Reducir costos de mantenimiento reactivo en 40%
- Aumentar disponibilidad de equipos (uptime) a 95%+

#### Objetivo 4: Mejora Continua
- Establecer ciclo de retroalimentación para mejorar modelos
- Capturar conocimiento operacional en sistema experto
- Reducir dependencia de expertos individuales

### 2.3 Objetivos SMART

| Objetivo | Específico | Medible | Alcanzable | Relevante | Tiempo |
|----------|-----------|---------|------------|-----------|--------|
| **Incremento de Producción** | Aumentar producción de campo | +5% promedio | Basado en benchmarks | Alto impacto económico | 12 meses |
| **Reducción de Downtime** | Disminuir tiempo fuera de servicio | -35% | Predicción temprana | Continuidad operacional | 18 meses |
| **Eficiencia Energética** | Reducir consumo energético | -12% | Optimización de frecuencias | Sostenibilidad | 15 meses |
| **ROI** | Retorno de inversión | 300%+ | Casos de éxito industria | Justificación financiera | 24 meses |

---

## 3. Contexto del Negocio

### 3.1 Situación Actual

**Módulos Existentes en Nexus:**

| Módulo | Estado | Funcionalidad |
|--------|--------|---------------|
| **RV** (Yacimientos) | ✅ Implementado | Caracterización de reservorios, IPR, declinación |
| **DR** (Drilling) | ✅ Implementado | Monitoreo de perforación, MSE, well control |
| **CT** (Coiled Tubing) | ✅ Implementado | Gestión de trabajos CT, fatiga analysis |
| **PF** (Production Facilities) | ❌ No existe | **A implementar** |
| **PO** (Production Optimization) | ❌ No existe | **A implementar** |

**Brecha Identificada:**
- No existe sistema para monitorear producción de superficie
- No hay optimización de levantamiento artificial
- Falta integración entre caracterización de yacimientos (RV) y operaciones diarias

### 3.2 Casos de Uso del Negocio

#### Caso de Uso 1: Operador de Control de Producción
**Actor**: Operador de sala de control
**Objetivo**: Monitorear estado de 50+ pozos simultáneamente
**Flujo**:
1. Abre dashboard de campo en módulo PF
2. Ve mapa con todos los pozos y su código de color (verde/amarillo/rojo)
3. Identifica pozo con alarma crítica
4. Hace clic en pozo para ver detalles
5. Revisa tendencias de temperatura de motor ESP
6. Ve recomendación del módulo PO: "Reducir frecuencia de 52 a 48 Hz"
7. Aplica cambio con un clic
8. Sistema envía comando a SCADA
9. Monitorea respuesta en tiempo real
10. Alarma se resuelve, pozo vuelve a verde

**Valor**: Tiempo de respuesta reducido de 2 horas a 5 minutos

#### Caso de Uso 2: Ingeniero de Producción
**Actor**: Ingeniero de producción
**Objetivo**: Optimizar distribución de gas lift entre 8 pozos
**Flujo**:
1. Abre optimizador de gas lift en módulo PO
2. Sistema muestra distribución actual: 12.5 MMscfd
3. Ejecuta optimización
4. Sistema calcula distribución óptima usando curvas de respuesta
5. Muestra incremento potencial: +165 bpd (valor: $12,375/día)
6. Ingeniero aprueba cambios
7. Sistema ajusta válvulas de inyección automáticamente
8. Monitorea resultados en siguientes 24 horas
9. Sistema aprende y mejora predicciones

**Valor**: $4.5M adicionales por año en un campo de 8 pozos

#### Caso de Uso 3: Gerente de Mantenimiento
**Actor**: Gerente de mantenimiento
**Objetivo**: Planificar intervenciones de pozos
**Flujo**:
1. Revisa dashboard de Health Score en módulo PO
2. Identifica 3 pozos con score < 50
3. Hace clic en ESP-023 (score: 42, tendencia descendente)
4. Sistema muestra predicción: "Falla estimada en 15-20 días"
5. Problema detectado: Aislamiento degradándose
6. Sistema sugiere: "Programar pulling en próximas 2 semanas"
7. Gerente crea orden de trabajo
8. Sistema estima costo de intervención: $180K
9. Compara con costo de falla catastrófica: $300K + producción diferida
10. Aprueba intervención preventiva

**Valor**: Ahorro de $120K + evitar 15 días de downtime

### 3.3 Mercado Objetivo

**Segmentos de Clientes:**

1. **Operadores de Campos Maduros** (Prioridad 1)
   - Campos con 10+ años de producción
   - Declinación natural pronunciada
   - Necesidad de optimización para mantener producción

2. **Operadores de Crudos Pesados** (Prioridad 1)
   - Alto uso de diluentes
   - Sistemas de levantamiento costosos (ESP, PCP)
   - Problemas frecuentes de operación

3. **Operadores con ESP Fleet** (Prioridad 2)
   - Alto costo de fallas (run life < 1 año)
   - Necesidad de optimización de frecuencias
   - Problemas de gas lock y temperatura

4. **Operadores con Gas Lift** (Prioridad 2)
   - Gas limitado
   - Necesidad de distribución óptima
   - Múltiples pozos compitiendo por gas

---

## 4. Arquitectura de Solución

### 4.1 Arquitectura Conceptual

```
┌─────────────────────────────────────────────────────────────────────┐
│                          NEXUS PLATFORM                             │
│                     (ThingsBoard 4.3.0 Extended)                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────────  PRESENTATION LAYER  ──────────────────────┐ │
│  │                                                                 │ │
│  │  Angular 18 Frontend + ThingsBoard Dashboards                  │ │
│  │  ├─ PF Dashboards (Wellpads, Flow Stations, Wells)            │ │
│  │  ├─ PO Dashboards (Optimizers, Recommendations, KPIs)         │ │
│  │  ├─ Mobile App (Field Operations)                             │ │
│  │  └─ Reporting & Analytics                                     │ │
│  │                                                                 │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                   ↕                                  │
│  ┌────────────────────  APPLICATION LAYER  ────────────────────────┐ │
│  │                                                                 │ │
│  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐      │ │
│  │  │  PF Module    │  │  PO Module    │  │  RV Module    │      │ │
│  │  │  (Facilities) │  │ (Optimization)│  │ (Reservoirs)  │      │ │
│  │  │               │  │               │  │               │      │ │
│  │  │ • Monitoring  │←─│ • Optimizers  │←─│ • IPR/PVT     │      │ │
│  │  │ • TB Assets   │  │ • ML Models   │  │ • Decline     │      │ │
│  │  │ • TB Alarms   │  │ • Recommends  │  │ • MatBalance  │      │ │
│  │  │ • Rule Engine │  │ • Health Score│  │               │      │ │
│  │  └───────────────┘  └───────────────┘  └───────────────┘      │ │
│  │                                                                 │ │
│  │  ┌───────────────┐  ┌───────────────┐                          │ │
│  │  │  DR Module    │  │  CT Module    │                          │ │
│  │  │  (Drilling)   │  │(Coiled Tubing)│                          │ │
│  │  └───────────────┘  └───────────────┘                          │ │
│  │                                                                 │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                   ↕                                  │
│  ┌────────────────────  SERVICE LAYER  ─────────────────────────────┐│
│  │                                                                 │ │
│  │  Wrapper Services (Spring Boot 3.4)                            │ │
│  │  ├─ PfAssetService (wraps TB AssetService)                     │ │
│  │  ├─ PfAttributeService (wraps TB AttributesService)            │ │
│  │  ├─ PfTelemetryService (wraps TB TelemetryService)             │ │
│  │  ├─ PoOptimizationService, PoPredictionService                 │ │
│  │  ├─ Integration Services (SCADA, ERP, CMMS)                    │ │
│  │  └─ Custom Rule Nodes (PfDataQualityNode, PfAlarmNode)         │ │
│  │                                                                 │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                   ↕                                  │
│  ┌────────────────────  DATA LAYER (ThingsBoard Core)  ────────────┐│
│  │                                                                 │ │
│  │  ┌──────────────────────────────────────────────────────┐      │ │
│  │  │  ThingsBoard Core Tables (PostgreSQL)                │      │ │
│  │  │  • asset (pf_well, pf_wellpad, pf_esp_system, etc.) │      │ │
│  │  │  • attribute_kv (SERVER_SCOPE attributes)            │      │ │
│  │  │  • ts_kv, ts_kv_latest (Time-series telemetry)      │      │ │
│  │  │  • alarm (TB Alarm System)                           │      │ │
│  │  │  • relation (Contains, BelongsTo, HasSystem)        │      │ │
│  │  └──────────────────────────────────────────────────────┘      │ │
│  │                                                                 │ │
│  │  ┌──────────────────────────────────────────────────────┐      │ │
│  │  │  Custom Tables (ONLY 2 - for complex workflows)      │      │ │
│  │  │  • pf_optimization_result (versioned ML results)     │      │ │
│  │  │  • pf_recommendation (state machine workflow)        │      │ │
│  │  └──────────────────────────────────────────────────────┘      │ │
│  │                                                                 │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │ │
│  │  │    Redis     │  │    Kafka     │  │   RocksDB    │          │ │
│  │  │   (Cache)    │  │  (Messaging) │  │(State Store) │          │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘          │ │
│  │                                                                 │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                   ↕                                  │
│  ┌────────────────────  INTEGRATION LAYER  ─────────────────────────┐│
│  │                                                                 │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │ │
│  │  │  SCADA   │  │  Historian│  │   ERP    │  │  CMMS    │        │ │
│  │  │  (OPC-UA)│  │  (PI/PHD) │  │  (SAP)   │  │(Maximo)  │        │ │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │ │
│  │                                                                 │ │
│  │  Protocols: MQTT, Modbus, HTTP/REST, gRPC                      │ │
│  │                                                                 │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Arquitectura de Datos (ThingsBoard Core)

```
┌─────────────────────────────────────────────────────────────────────┐
│              DATA ARCHITECTURE (ThingsBoard Core)                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  THINGSBOARD CORE TABLES (Public Schema)                           │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                                                             │   │
│  │  asset                                                      │   │
│  │  • type: pf_well, pf_wellpad, pf_flow_station, etc.        │   │
│  │  • id, tenant_id, name, label, additional_info             │   │
│  │                                                             │   │
│  │  attribute_kv (SERVER_SCOPE)                               │   │
│  │  • Well properties: api_number, status, latitude, longitude│   │
│  │  • ESP specs: motor_hp, stages, frequency                  │   │
│  │  • Health scores: health_score, failure_probability        │   │
│  │  • Operational limits: high_pressure, low_pressure         │   │
│  │                                                             │   │
│  │  ts_kv, ts_kv_latest (Time-Series)                         │   │
│  │  • Real-time telemetry: pressure, temperature, frequency   │   │
│  │  • Native TB time-series optimizations                     │   │
│  │  • Partitioning handled by TB automatically                │   │
│  │                                                             │   │
│  │  alarm (TB Alarm System)                                   │   │
│  │  • Configured via Asset Profiles + Alarm Rules             │   │
│  │  • Severities: CRITICAL, MAJOR, WARNING, INDETERMINATE     │   │
│  │  • Auto-propagation to parent assets                       │   │
│  │                                                             │   │
│  │  relation                                                   │   │
│  │  • Wellpad Contains Wells                                  │   │
│  │  • Well HasSystem ESP/PCP/GasLift                          │   │
│  │  • FlowStation ProcessesFrom Wellpad                       │   │
│  │                                                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  CUSTOM TABLES (Only 2 - for complex workflows)                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                                                             │   │
│  │  pf_optimization_result                                     │   │
│  │  • Versioned ML/optimization results                       │   │
│  │  • Complex queries for ML training                         │   │
│  │  • Historical analysis requirements                        │   │
│  │  • Fields: id, tenant_id, well_asset_id (UUID ref to TB),  │   │
│  │    optimization_type, parameters (JSON), results (JSON)    │   │
│  │                                                             │   │
│  │  pf_recommendation                                          │   │
│  │  • State machine: PENDING → APPROVED → EXECUTED            │   │
│  │  • Approval workflow with audit trail                      │   │
│  │  • Fields: id, tenant_id, well_asset_id, type, status,     │   │
│  │    parameters, approved_by, executed_time                  │   │
│  │                                                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  CACHE LAYER                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Redis (In-Memory)                                          │   │
│  │  • Latest attribute values (from TB attribute_kv)          │   │
│  │  • Active alarms (from TB alarm table)                     │   │
│  │  • Session data                                            │   │
│  │  • TTL: 5 minutos                                          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  STREAM PROCESSING                                                 │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Apache Kafka                                               │   │
│  │  • Topic: pf.telemetry.raw                                 │   │
│  │  • Topic: po.recommendations                               │   │
│  │  • Topic: po.setpoint-changes                              │   │
│  │  • Retention: 7 días                                       │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  COLD DATA (Historical)                                            │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  S3 / Object Storage                                        │   │
│  │  • Telemetry export from ts_kv (>30 días)                  │   │
│  │  • Parquet format for ML training                          │   │
│  │  • Partitioned: /year/month/day/                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.3 Patrones de Arquitectura

#### Patrón 1: ThingsBoard Rule Engine Processing
```
Device → TB MQTT Transport → Rule Engine:
    ├── Message Type Switch
    ├── PfDataQualityNode (custom) → quality_score as attribute
    ├── PfAlarmEvaluationNode (custom) → TB Alarm System
    └── Save Timeseries → ts_kv (native)
```

#### Patrón 2: Wrapper Services Pattern
```
Controller → PfWellService → PfAssetService → TB AssetService
                          → PfAttributeService → TB AttributesService
                          → PfTelemetryService → TB TelemetryService
```

#### Patrón 3: Assets vs Custom Tables Decision
```
┌─────────────────────────────────────────────────────────────┐
│  ¿Necesita versionado histórico?                           │
│       ↓ NO                           ↓ YES                 │
│  ┌─────────────────┐          ┌─────────────────────┐      │
│  │ ¿Tiene workflow │          │ Tabla Custom        │      │
│  │ de estados?     │          │ (pf_optimization_   │      │
│  │       ↓ NO      │          │  result)            │      │
│  │ ┌─────────────┐ │          └─────────────────────┘      │
│  │ │ TB Asset +  │ │                                        │
│  │ │ Attributes  │ │                                        │
│  │ └─────────────┘ │                                        │
│  │       ↓ YES     │                                        │
│  │ ┌─────────────┐ │                                        │
│  │ │ Tabla Custom│ │                                        │
│  │ │(pf_recommend│ │                                        │
│  │ │ ation)      │ │                                        │
│  │ └─────────────┘ │                                        │
│  └─────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
```

### 4.4 Por qué ThingsBoard Core vs Tablas Custom

| Aspecto | TB Core | Tablas Custom |
|---------|---------|---------------|
| **Time-to-market** | ✅ Rápido | ❌ Lento |
| **Mantenimiento** | ✅ Bajo (TB mantiene) | ❌ Alto |
| **Escalabilidad** | ✅ Probada | ⚠️ Por implementar |
| **UI/Dashboards** | ✅ Incluidos | ❌ Desarrollar |
| **Alarmas** | ✅ Sistema completo | ❌ Implementar |
| **Queries temporales** | ✅ Optimizados | ❌ Implementar |
| **WebSocket real-time** | ✅ Nativo | ❌ Implementar |
| **Multi-tenancy** | ✅ Nativo | ⚠️ Implementar |

---

## 5. Alcance del Proyecto

### 5.1 En Alcance (In Scope)

#### Módulo PF - Production Facilities

**✅ Gestión de Activos (como TB Assets)**
- Pozos productores (asset type: pf_well)
- Macollas/Wellpads (asset type: pf_wellpad)
- Estaciones de flujo (asset type: pf_flow_station)
- Separadores (asset type: pf_separator)
- Tanques de almacenamiento (asset type: pf_tank)
- Líneas de recolección (asset type: pf_pipeline)

**✅ Sistemas de Levantamiento Artificial (como TB Assets)**
- ESP (asset type: pf_esp_system)
- PCP (asset type: pf_pcp_system)
- Gas Lift (asset type: pf_gas_lift_system)
- Rod Pump (asset type: pf_rod_pump_system)
- Jet Pump (asset type: pf_jet_pump_system)
- Inyección de diluentes (asset type: pf_diluent_system)

**✅ Telemetría en Tiempo Real**
- Integración SCADA via TB MQTT Transport
- Procesamiento via TB Rule Engine
- Validación de calidad via custom Rule Nodes
- Almacenamiento en ts_kv nativo

**✅ Monitoreo y Visualización**
- TB Dashboards nativos + Custom Angular components
- Vista de macolla/wellpad
- Vista de estación de flujo
- Vista de pozo individual
- Tendencias en tiempo real
- Mapas geográficos

**✅ Alarmas y Eventos**
- TB Alarm System con Asset Profiles
- Alarm Rules configurables
- Clasificación por severity (CRITICAL/MAJOR/WARNING)
- Notificaciones (email, SMS via TB Notification)
- Alarm propagation a parent assets

#### Módulo PO - Production Optimization

**✅ Optimizadores**
- Optimizador de frecuencia ESP
- Optimizador de RPM PCP
- Optimizador de distribución de gas lift
- Optimizador de inyección de diluentes
- Optimizador de nivel de fluido

**✅ Analytics Avanzado**
- Predicción de fallas con Machine Learning
- Detección de anomalías
- Health Score de equipos (stored as TB Attributes)
- Análisis de causa raíz
- Benchmarking de pozos

**✅ Recomendaciones (tabla custom: pf_recommendation)**
- Generación automática de recomendaciones
- Simulación de impacto
- Flujo de aprobación con estado
- Tracking de efectividad
- Aprendizaje continuo

**✅ KPIs (stored as TB Attributes)**
- KPIs de producción (uptime, efficiency, deferment)
- KPIs de equipos (run life, MTBF, MTTR)
- KPIs económicos (lifting cost, energy cost, ROI)

**✅ Control (Fase 5)**
- Envío de setpoints via TB RPC
- Control en lazo cerrado
- Rollback automático si falla

#### Integración

**✅ Integración con Módulos Existentes**
- Módulo RV: Recibir IPR, PVT, caracterización
- Módulo RV: Enviar producción real, datos de declinación

**✅ Integración con Sistemas Externos**
- SCADA/DCS via TB MQTT Transport
- Historian (PI, PHD) - lectura histórica
- ERP (SAP) - precios, costos, inventarios
- CMMS (Maximo) - órdenes de trabajo

### 5.2 Fuera de Alcance (Out of Scope)

❌ **No Incluido en Este Proyecto:**

- Simulador de yacimientos (ya existe en RV Module)
- Drilling optimization (ya existe en DR Module)
- Coiled tubing management (ya existe en CT Module)
- Artificial Intelligence de propósito general (solo ML específico para producción)
- Reemplazo de sistemas SCADA existentes (solo integración)
- Hardware de campo (sensores, RTUs, PLCs)
- Migración de datos históricos (solo forward desde go-live)
- Mobile app nativa (solo responsive web)
- Blockchain / cryptocurrency features
- Social media integration

❌ **Pospuesto para Fases Futuras:**

- Optimización de redes de recolección (network optimization)
- Simulación hidráulica de líneas
- Optimización de planta de tratamiento
- Optimización de separación
- Gestión de agua producida
- Monitoreo ambiental (flaring, emissions)
- HSE (Health, Safety, Environment) module

### 5.3 Supuestos (Assumptions)

1. **Infraestructura SCADA Existente**
   - Los campos ya tienen sistemas SCADA operativos
   - Protocolos estándar disponibles (MQTT, Modbus)
   - RTUs/PLCs con conectividad de red

2. **Datos Disponibles**
   - Telemetría de pozos disponible con frecuencia mínima de 1 minuto
   - Datos históricos de al menos 6 meses para entrenar modelos ML
   - Metadata de pozos y equipos disponible

3. **Recursos Humanos**
   - Equipo de desarrollo disponible según plan
   - Production engineers disponibles como SMEs
   - Personal de IT para soporte de infraestructura

4. **Tecnología**
   - Stack actual (Spring Boot, Angular, PostgreSQL) es adecuado
   - ThingsBoard Core (Assets, Attributes, ts_kv) es suficiente
   - ThingsBoard 4.3.0 es estable y no requiere upgrade

5. **Acceso y Permisos**
   - Acceso a sistemas SCADA para lectura/escritura
   - Credenciales para sistemas externos (ERP, CMMS)
   - Permisos para desplegar en producción

### 5.4 Restricciones (Constraints)

#### Restricciones Técnicas

1. **Performance**
   - Latencia máxima de procesamiento de telemetría: 1 segundo
   - Latencia máxima de API REST: 200ms (p95)
   - Sistema debe manejar 100+ pozos con telemetría de 1 segundo

2. **Disponibilidad**
   - Uptime requerido: 99.5% (43 horas de downtime/año máximo)
   - Disaster recovery: RPO < 1 hora, RTO < 4 horas

3. **Seguridad**
   - Multi-tenant isolation obligatoria (via TB native)
   - Encriptación de datos en tránsito y en reposo
   - Audit logging de todas las operaciones críticas

4. **Compatibilidad**
   - Debe funcionar con ThingsBoard 4.3.0 (no se puede cambiar versión)
   - Debe seguir arquitectura de módulos existentes (RV, DR, CT)
   - Debe usar TB Core para datos que TB maneja bien

#### Restricciones de Negocio

1. **Presupuesto**
   - Budget máximo: $1.8M
   - No se puede exceder sin aprobación ejecutiva

2. **Tiempo**
   - Go-live de Fase 1 (PF Module): 4 meses máximo
   - Proyecto completo: 21 meses máximo

3. **Recursos**
   - Equipo limitado a 10 personas
   - Production engineers disponibles solo 20% de su tiempo

#### Restricciones Operacionales

1. **Despliegue**
   - Despliegues solo permitidos en ventanas de mantenimiento
   - No se puede interrumpir operaciones de producción
   - Rollback plan obligatorio

2. **Capacitación**
   - Máximo 2 días de capacitación por usuario
   - Materiales en español
   - Sistema debe ser intuitivo (learn by doing)

---

## 6. Módulos a Implementar

### 6.1 Módulo PF (Production Facilities)

**Objetivo**: Monitoreo en tiempo real de infraestructura de producción de superficie

**Arquitectura**: ThingsBoard Core (Assets, Attributes, ts_kv, Alarm System)

**Componentes Principales**:

#### 6.1.1 Asset Types (TB Assets)
```
PF Module Asset Types:
├── pf_well            - Pozo productor
├── pf_wellpad         - Macolla/Cluster
├── pf_flow_station    - Estación de flujo
├── pf_separator       - Separador
├── pf_tank            - Tanque
└── pf_pipeline        - Tubería
```

#### 6.1.2 Lift System Asset Types
```
Lift System Asset Types:
├── pf_esp_system      - Sistema ESP
├── pf_pcp_system      - Sistema PCP
├── pf_gas_lift_system - Sistema Gas Lift
├── pf_rod_pump_system - Sistema Rod Pump
└── pf_jet_pump_system - Sistema Jet Pump
```

#### 6.1.3 DTOs y Wrapper Services
```
org.thingsboard.nexus.pf
├── dto/
│   ├── PfWellDto.java (ASSET_TYPE = "pf_well", ATTR_* constants)
│   ├── PfWellpadDto.java
│   ├── PfFlowStationDto.java
│   └── PfEspSystemDto.java
├── service/
│   ├── PfAssetService.java (wraps TB AssetService)
│   ├── PfAttributeService.java (wraps TB AttributesService)
│   ├── PfTelemetryService.java (wraps TB TelemetryService)
│   ├── PfWellService.java (business logic)
│   └── PfWellpadService.java
└── controller/
    ├── PfWellController.java
    └── PfWellpadController.java
```

#### 6.1.4 Custom Rule Nodes (Rule Engine)
```
org.thingsboard.nexus.pf.rule
├── PfDataQualityNode.java
│   ├── Range validation
│   ├── Rate of change validation
│   ├── Quality score calculation
│   └── Saves quality_score as SERVER_SCOPE attribute
├── PfAlarmEvaluationNode.java
│   ├── Complex business rule evaluation
│   └── Creates TB Alarms via AlarmService
└── PfTelemetryEnrichmentNode.java
    └── Adds calculated fields to telemetry
```

#### 6.1.5 Asset Profiles (TB Alarm System)
```
Asset Profile "pf_well":
├── Default Rule Chain: "PF Telemetry Processing"
├── Alarm Rules:
│   ├── high_pressure_alarm (severity: CRITICAL)
│   ├── low_pressure_alarm (severity: WARNING)
│   ├── high_temperature_alarm (severity: CRITICAL)
│   └── vibration_alarm (severity: MAJOR)
└── Alarm propagation: to parent wellpad
```

#### 6.1.6 Frontend Components (Angular + TB Dashboards)
```
ui-ngx/src/app/modules/home/pages/pf/
├── wellpad-list/
├── wellpad-dashboard/
├── flow-station-dashboard/
├── well-detail/
├── well-trend/
└── alarm-list/

TB Dashboard Templates:
├── PF Well Overview
├── PF Wellpad Overview
├── PF Alarm Console
└── PF Production Summary
```

**Entregables Fase 1 (PF Base)**:
- [ ] DTOs con ASSET_TYPE y ATTR_* constants
- [ ] Wrapper Services (PfAssetService, PfAttributeService)
- [ ] Business Services (PfWellService, PfWellpadService)
- [ ] Custom Rule Nodes para Rule Engine
- [ ] Asset Profiles con Alarm Rules
- [ ] TB Dashboard templates
- [ ] REST Controllers

**Entregables Fase 2 (Lift Systems)**:
- [ ] Lift System DTOs y Services
- [ ] Specialized variables per system type
- [ ] Dashboards especializados por tipo
- [ ] Alarm rules per lift system type

### 6.2 Módulo PO (Production Optimization)

**Objetivo**: Optimización inteligente de operaciones de producción

**Arquitectura**: TB Core para Health Scores + Custom Tables para Workflows

**Componentes Principales**:

#### 6.2.1 Optimization Engines
```
org.thingsboard.nexus.po.optimizer
├── EspFrequencyOptimizer
│   ├── FrequencyCalculator
│   ├── EfficiencyAnalyzer
│   └── SafetyChecker
├── GasLiftAllocator
│   ├── ResponseCurveBuilder
│   ├── AllocationOptimizer
│   └── SensitivityAnalyzer
├── DiluentOptimizer
│   ├── ViscosityModel
│   ├── EconomicOptimizer
│   └── InventoryManager
└── PcpRpmController
    ├── FluidLevelController
    ├── TorqueMonitor
    └── WearPredictor
```

#### 6.2.2 Machine Learning
```
org.thingsboard.nexus.po.ml
├── FailurePredictionModel
│   ├── EspFailureModel
│   ├── PcpFailureModel
│   └── ModelTrainer
├── AnomalyDetector
│   ├── IsolationForest
│   ├── LSTM
│   └── AutoEncoder
└── ForecastingModel
    ├── ProductionForecast
    └── DeclineForecast
```

#### 6.2.3 Health Score (TB Attributes)
```
Health Score stored as SERVER_SCOPE Attributes on well assets:
├── health_score (BigDecimal 0-100)
├── health_trend (String: IMPROVING, STABLE, DEGRADING)
├── failure_probability (BigDecimal 0-1)
├── next_failure_estimate (Long timestamp)
└── contributing_factors (JSON array)

Service Pattern:
```java
public void saveHealthScore(UUID wellAssetId, HealthScoreDto score) {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put(ATTR_HEALTH_SCORE, score.getScore());
    attrs.put(ATTR_FAILURE_PROBABILITY, score.getFailureProbability());
    poAttributeService.saveServerAttributes(wellAssetId, attrs);
}
```
```

#### 6.2.4 Recommendation Engine (Custom Table)
```
org.thingsboard.nexus.po.recommendation
├── RecommendationService
├── ImpactSimulator (simular impacto)
├── ApprovalWorkflow
├── EffectivenessTracker
└── LearningService (feedback loop)

Custom Table: pf_recommendation
├── id (UUID)
├── tenant_id (UUID)
├── well_asset_id (UUID → TB Asset)
├── type (String)
├── status (PENDING, APPROVED, EXECUTED, REJECTED)
├── parameters (JSONB)
├── approved_by (UUID)
├── approved_time (Long)
├── executed_time (Long)
└── effectiveness_score (BigDecimal)
```

#### 6.2.5 Optimization Results (Custom Table)
```
Custom Table: pf_optimization_result
├── id (UUID)
├── tenant_id (UUID)
├── well_asset_id (UUID → TB Asset)
├── optimization_type (String)
├── version (Integer)
├── input_parameters (JSONB)
├── results (JSONB)
├── timestamp (Long)
└── model_version (String)
```

#### 6.2.6 KPI Calculator (TB Attributes)
```
KPIs stored as SERVER_SCOPE Attributes:
├── uptime_percent
├── efficiency_percent
├── deferment_bpd
├── run_life_days
├── mtbf_hours
├── mttr_hours
├── lifting_cost_per_bbl
├── energy_cost_per_bbl
└── roi_percent

Service Pattern:
```java
public void calculateAndSaveKpis(UUID wellAssetId) {
    KpiDto kpis = kpiCalculator.calculate(wellAssetId);
    Map<String, Object> attrs = kpiToAttributeMap(kpis);
    poAttributeService.saveServerAttributes(wellAssetId, attrs);
}
```
```

#### 6.2.7 Frontend Components (Angular)
```
ui-ngx/src/app/modules/home/pages/po/
├── optimizer-dashboard/
├── esp-optimizer/
├── gas-lift-optimizer/
├── diluent-optimizer/
├── recommendation-list/
├── health-score-dashboard/
├── kpi-dashboard/
├── prediction-dashboard/
└── analytics-reports/
```

**Entregables Fase 3 (PO Base)**:
- [ ] PoAssetService, PoAttributeService (wrappers)
- [ ] Optimizador ESP (frecuencia)
- [ ] Optimizador Gas Lift (distribución)
- [ ] Sistema de recomendaciones (pf_recommendation table)
- [ ] Health Score calculation + TB Attributes storage
- [ ] Dashboard de optimización
- [ ] Flujo de aprobación

**Entregables Fase 4 (Advanced Analytics)**:
- [ ] Modelos ML para predicción de fallas
- [ ] Detector de anomalías
- [ ] pf_optimization_result table
- [ ] KPI dashboards
- [ ] Análisis de causa raíz

**Entregables Fase 5 (Automation)**:
- [ ] Control en lazo cerrado via TB RPC
- [ ] Integración bidireccional con SCADA
- [ ] Auto-aprendizaje de modelos
- [ ] Rollback automático

---

## 7. Integración con Módulos Existentes

### 7.1 Integración con Módulo RV (Yacimientos)

**Dirección: RV → PF/PO**

| Dato de RV | Uso en PF/PO | Frecuencia |
|------------|--------------|------------|
| IPR (Inflow Performance) | Setpoint objetivo de producción | Mensual |
| Propiedades PVT (API, viscosidad, GOR) | Parámetros de modelos de optimización | Mensual |
| Presión de yacimiento | Límites operacionales | Mensual |
| Curva de declinación | Benchmark de eficiencia | Mensual |
| Tipo de fluido (oil/gas) | Configuración de sistema de levantamiento | Una vez |

**API de Integración**:
```
GET /api/nexus/rv/reservoirs/{id}/ipr → para obtener IPR
GET /api/nexus/rv/wells/{id}/pvt → para obtener PVT
GET /api/nexus/rv/wells/{id}/decline → para obtener curva de declinación
```

**Dirección: PF/PO → RV**

| Dato de PF/PO | Uso en RV | Frecuencia |
|---------------|-----------|------------|
| Producción diaria real (oil, gas, water) | Actualizar curva de declinación | Diario |
| BSW (Basic Sediment & Water) | Análisis de corte de agua | Diario |
| GOR medido | Comparar con GOR esperado | Diario |
| Presión de fondo (calculada) | Validar IPR | Semanal |
| Uptime/Downtime | Factor de disponibilidad | Mensual |

**API de Integración**:
```
POST /api/nexus/rv/wells/{id}/production → enviar datos de producción diaria
POST /api/nexus/rv/wells/{id}/pressure-survey → enviar presión de fondo
```

**Ejemplo de Flujo**:
```
1. PO Module calcula producción óptima de pozo ABC-123
2. PO consulta IPR de RV: "Potencial = 600 bpd a 2000 psi Pwf"
3. PO considera PVT de RV: "API 28°, viscosidad 15 cp"
4. PO optimiza frecuencia ESP para alcanzar 580 bpd (97% del potencial)
5. PF monitorea producción real: 575 bpd
6. PF envía a RV datos diarios
7. RV actualiza curva de declinación con datos reales
```

### 7.2 Integración con Módulo DR (Drilling)

**No hay integración directa**, pero:

Cuando un pozo termina de perforarse en DR Module:
1. DR crea entidad de pozo completado
2. Workflow manual: Operador "promueve" pozo de DR a PF
3. PF crea nuevo TB Asset tipo `pf_well` con referencia a pozo de DR
4. Se configura sistema de levantamiento como TB Asset relacionado
5. Se inicia monitoreo en PF

### 7.3 Integración con Módulo CT (Coiled Tubing)

**No hay integración directa**, pero:

Cuando se planea un trabajo de CT en un pozo productor:
1. CT Module consulta estado de pozo en PF
2. PF indica si pozo está produciendo o parado (via TB Attribute)
3. Durante trabajo CT, pozo se marca como "Under Workover" en PF
4. Alarmas de PF se silencian temporalmente
5. Al finalizar trabajo CT, pozo vuelve a estado productivo en PF

---

## 8. Estrategia de Implementación

### 8.1 Metodología: Agile con Fases Incrementales

**Framework**: Scrum con sprints de 2 semanas

**Fases del Proyecto**:
- **Fase 0**: Planning & Setup (1 mes)
- **Fase 1**: PF Module Base (3-4 meses, 6-8 sprints)
- **Fase 2**: Lift Systems (2-3 meses, 4-6 sprints)
- **Fase 3**: PO Module Base (3-4 meses, 6-8 sprints)
- **Fase 4**: Advanced Analytics (4-6 meses, 8-12 sprints)
- **Fase 5**: Automation (3-4 meses, 6-8 sprints)

**Duración Total**: 16-22 meses (incluyendo planning)

### 8.2 Estrategia de Release

**Modelo: Continuous Delivery con Feature Flags**

Environments:
1. **Development** (dev)
   - Despliegue automático en cada commit a `develop`
   - Usado por developers para pruebas

2. **Staging** (stg)
   - Despliegue automático cuando se completa sprint
   - Usado para UAT (User Acceptance Testing)
   - Réplica exacta de producción

3. **Production** (prod)
   - Despliegue manual al finalizar cada fase
   - Requiere aprobación de Product Owner
   - Feature flags para activar funcionalidades gradualmente

**Release Strategy**:
- **Alpha** (interno): Despliegue a campo piloto (5-10 pozos)
- **Beta** (limitado): Despliegue a campo completo (50+ pozos)
- **GA** (General Availability): Despliegue a todos los clientes

### 8.3 Piloto y Rollout

**Fase 1 Piloto (PF Module)**:
```
Semana 1-2:   Despliegue en ambiente de pruebas
Semana 3-4:   Configuración de 5 pozos como TB Assets
Semana 5-8:   Operación paralela con sistema legacy
Semana 9-12:  Validación de datos, ajustes
Semana 13:    Go-live oficial de piloto
Semana 14-16: Monitoreo y soporte intensivo
```

**Criterios de Éxito del Piloto**:
- ✅ 100% de telemetría capturada via TB ts_kv
- ✅ Latencia < 1 segundo en procesamiento
- ✅ 0 downtime del sistema
- ✅ Satisfacción de usuarios > 80%
- ✅ Todas las alarmas críticas detectadas via TB Alarm System

**Rollout a Producción**:
```
Mes 1: Campo piloto (5-10 pozos como TB Assets)
Mes 2: Expansión a 25 pozos
Mes 3: Expansión a 50 pozos
Mes 4: Expansión a 100+ pozos (campo completo)
```

### 8.4 Gestión de Cambios

**Change Management Process**:

1. **Preparación**
   - Identificar stakeholders
   - Evaluar impacto del cambio
   - Desarrollar plan de comunicación

2. **Capacitación**
   - Materiales de capacitación (videos, manuales)
   - Sesiones hands-on
   - Sandbox para práctica

3. **Comunicación**
   - Kickoff meeting
   - Demos mensuales
   - Newsletter semanal durante rollout

4. **Soporte**
   - Helpdesk 24/7 durante primeras 2 semanas
   - Champions en cada turno
   - Documentación en wiki interna

---

## 9. Plan de Recursos

### 9.1 Equipo de Desarrollo

#### Backend Team

| Rol | Cantidad | Perfil | Responsabilidad | Dedicación |
|-----|----------|--------|-----------------|------------|
| **Tech Lead Backend** | 1 | Senior Java Developer, 8+ años | Arquitectura, code review, mentoring | 100% |
| **Backend Developer 1** | 1 | Java/Spring Boot, 5+ años | Módulo PF: DTOs, Services, Rule Nodes | 100% |
| **Backend Developer 2** | 1 | Java/Spring Boot, 5+ años | Módulo PO: optimizadores, KPIs | 100% |
| **Data Engineer** | 1 | PostgreSQL, Kafka | TB integrations, performance tuning | 80% |
| **ML Engineer** | 1 | Python, TensorFlow, scikit-learn | Modelos predictivos, anomaly detection | 60% (Fase 4) |

#### Frontend Team

| Rol | Cantidad | Perfil | Responsabilidad | Dedicación |
|-----|----------|--------|-----------------|------------|
| **Tech Lead Frontend** | 1 | Angular, 7+ años | Arquitectura frontend, code review | 100% |
| **Frontend Developer 1** | 1 | Angular, TypeScript, 4+ años | Módulo PF: dashboards, monitoreo | 100% |
| **Frontend Developer 2** | 1 | Angular, TypeScript, 4+ años | Módulo PO: optimizadores, analytics | 100% (desde Fase 3) |
| **UX/UI Designer** | 1 | Figma, diseño de sistemas | Wireframes, UI components, UX research | 50% |

#### DevOps & Infrastructure

| Rol | Cantidad | Perfil | Responsabilidad | Dedicación |
|-----|----------|--------|-----------------|------------|
| **DevOps Engineer** | 1 | Docker, Kubernetes, CI/CD | Pipelines, monitoring, deployments | 80% |
| **DBA** | 1 | PostgreSQL tuning | Database optimization, backups | 40% |

#### QA & Testing

| Rol | Cantidad | Perfil | Responsabilidad | Dedicación |
|-----|----------|--------|-----------------|------------|
| **QA Lead** | 1 | Test automation, Selenium | Test strategy, automation framework | 100% |
| **QA Engineer** | 1 | Manual + automation testing | Test execution, bug reporting | 100% |

#### Domain Experts (SMEs)

| Rol | Cantidad | Perfil | Responsabilidad | Dedicación |
|-----|----------|--------|-----------------|------------|
| **Production Engineer** | 1 | 10+ años en producción, ESP/PCP | Requirements, validation, UAT | 20% |
| **Petroleum Engineer** | 1 | Optimización de producción | Algoritmos de optimización, validación | 20% |
| **SCADA Expert** | 1 | OPC-UA, Modbus, sistemas de control | Integración SCADA, troubleshooting | 30% |

#### Management

| Rol | Cantidad | Perfil | Responsabilidad | Dedicación |
|-----|----------|--------|-----------------|------------|
| **Product Owner** | 1 | PMP, experiencia en O&G | Priorización, roadmap, stakeholders | 100% |
| **Scrum Master** | 1 | Certified Scrum Master | Facilitar ceremonias, remover impedimentos | 50% |
| **Project Manager** | 1 | PMP, 5+ años proyectos de software | Timeline, budget, riesgos, reporting | 100% |

**Total Team Size**: 16 personas (12 FTE aproximadamente)

### 9.2 Costo de Personal

| Rol | Cantidad | Rate (USD/mes) | Meses | Total |
|-----|----------|----------------|-------|-------|
| Tech Leads (2) | 2 | $12,000 | 18 | $432,000 |
| Senior Developers (3) | 3 | $10,000 | 18 | $540,000 |
| Mid-level Developers (3) | 3 | $7,000 | 14 | $294,000 |
| ML Engineer | 1 | $11,000 | 8 | $88,000 |
| DevOps + DBA | 2 | $8,000 | 18 | $288,000 |
| QA Team (2) | 2 | $6,000 | 16 | $192,000 |
| UX/UI Designer | 1 | $6,000 | 12 | $72,000 |
| Domain Experts (3) | 3 | $4,000 | 18 | $216,000 |
| Management (3) | 3 | $8,000 | 18 | $432,000 |
| **TOTAL PERSONAL** | | | | **$2,554,000** |

### 9.3 Otros Costos

| Concepto | Costo (USD) |
|----------|-------------|
| **Infraestructura Cloud** | |
| - Servers (Dev/Stg/Prod) | $80,000 |
| - Storage (PostgreSQL, S3) | $40,000 |
| - Networking & CDN | $20,000 |
| **Licencias de Software** | |
| - IDE Licenses (IntelliJ, WebStorm) | $10,000 |
| - Monitoring Tools (Grafana, DataDog) | $30,000 |
| - Testing Tools (Selenium Grid) | $15,000 |
| **Capacitación** | |
| - Training materials development | $20,000 |
| - On-site training sessions | $30,000 |
| **Viajes** | |
| - Site visits para UAT | $40,000 |
| - Conference attendance | $10,000 |
| **Contingencia (15%)** | $383,100 |
| **TOTAL OTROS** | **$678,100** |

### 9.4 Costo Total del Proyecto

| Categoría | Costo (USD) |
|-----------|-------------|
| Personal | $2,554,000 |
| Infraestructura & Licencias | $195,000 |
| Capacitación | $50,000 |
| Viajes | $50,000 |
| Contingencia | $383,100 |
| **TOTAL PROYECTO** | **$3,232,100** |

**Nota**: Este es el costo total de desarrollo. El costo operativo anual post-implementación se estima en $400K-$600K/año (mantenimiento, hosting, soporte).

---

## 10. Gestión de Riesgos

### 10.1 Matriz de Riesgos

| ID | Riesgo | Probabilidad | Impacto | Severidad | Mitigación |
|----|--------|--------------|---------|-----------|------------|
| R01 | Datos de SCADA inconsistentes o faltantes | Alta | Alto | 🔴 CRÍTICO | Implementar PfDataQualityNode robusto, tener plan B con datos estimados |
| R02 | Retraso en integración SCADA (protocolos propietarios) | Media | Alto | 🟠 ALTO | Iniciar integración temprano (Fase 0), usar TB MQTT Transport |
| R03 | Modelos ML no alcanzan accuracy esperado | Media | Medio | 🟡 MEDIO | Empezar con heurísticas, mejorar gradualmente con ML |
| R04 | Resistencia al cambio de operadores | Alta | Medio | 🟡 MEDIO | Change management robusto, involucrar operadores desde día 1 |
| R05 | Performance issues con 100+ pozos | Baja | Alto | 🟠 ALTO | Aprovechar TB native ts_kv optimizations, load testing |
| R06 | Pérdida de personal clave (brain drain) | Media | Alto | 🟠 ALTO | Documentación exhaustiva, knowledge sharing, redundancia en roles |
| R07 | Scope creep (nuevos requerimientos) | Alta | Medio | 🟡 MEDIO | Gestión estricta de cambios, Product Owner fuerte |
| R08 | Problemas de compatibilidad con ThingsBoard | Baja | Alto | 🟠 ALTO | Seguir patrón de CT/RV modules, usar TB APIs estándar |
| R09 | Downtime de producción durante despliegue | Baja | Crítico | 🔴 CRÍTICO | Despliegues en ventanas de mantenimiento, rollback plan |
| R10 | Budget overrun | Media | Medio | 🟡 MEDIO | Tracking semanal de costos, contingencia del 15% |

### 10.2 Plan de Mitigación Detallado

#### R01: Datos de SCADA Inconsistentes
**Mitigación**:
1. **Detección Temprana**:
   - Implementar PfDataQualityNode en Rule Engine desde Fase 1
   - Quality score saved as TB Attribute
   - Alertas cuando calidad de datos < 90%

2. **Estrategias de Manejo**:
   ```
   PfDataQualityNode logic:
   if (data_quality < 90%) {
       save_quality_score_attribute();
       route_to_review_queue();
   }

   if (data_missing > 5_minutes) {
       use_last_known_value_from_ts_kv_latest();
       mark_as_estimated();
   }

   if (data_missing > 30_minutes) {
       disable_optimization();
       create_tb_alarm(severity=MAJOR);
   }
   ```

3. **Plan B**:
   - Mantener operación en modo "monitoring only"
   - Deshabilitar optimización automática si datos no confiables

#### R04: Resistencia al Cambio
**Mitigación**:
1. **Fase de Preparación**:
   - Kickoff meeting explicando beneficios
   - Identificar "champions" en cada turno
   - Encuestas de expectativas

2. **Involucramiento Temprano**:
   - Operadores participan en UAT desde Fase 1
   - Feedback incorporado en cada sprint
   - Demo sessions cada 2 semanas

3. **Capacitación Gradual**:
   - Semana 1: Conceptos básicos
   - Semana 2: Hands-on en sandbox
   - Semana 3: Shadow operations (sistema en paralelo)
   - Semana 4: Go-live con soporte intensivo

4. **Incentivos**:
   - Reconocimiento a early adopters
   - KPIs del sistema incluidos en bonus de operadores

#### R06: Pérdida de Personal Clave
**Mitigación**:
1. **Documentación**:
   - Architecture Decision Records (ADR) para cada decisión importante
   - Code bien comentado
   - Wiki interna con runbooks

2. **Knowledge Sharing**:
   - Pair programming obligatorio
   - Code review de 100% del código
   - Tech talks quincenales

3. **Redundancia**:
   - Mínimo 2 personas por área crítica
   - Rotación de responsabilidades cada 3 meses

4. **Retención**:
   - Salarios competitivos
   - Plan de carrera claro
   - Bonos por hitos del proyecto

### 10.3 Contingencia por Fase

| Fase | Contingencia de Tiempo | Contingencia de Budget |
|------|----------------------|------------------------|
| Fase 0 (Planning) | +1 semana | +$20K |
| Fase 1 (PF Base) | +2 semanas | +$80K |
| Fase 2 (Lift Systems) | +1 semana | +$50K |
| Fase 3 (PO Base) | +2 semanas | +$80K |
| Fase 4 (Analytics) | +3 semanas | +$100K |
| Fase 5 (Automation) | +2 semanas | +$70K |

---

## 11. Plan de Calidad

### 11.1 Estrategia de Testing

#### Unit Testing
- **Coverage Target**: 80%+
- **Framework**: JUnit 5, Mockito
- **Ejecutado en**: Cada commit (CI pipeline)

#### Integration Testing
- **Coverage Target**: 60%+
- **Framework**: Spring Boot Test, TestContainers
- **Ejecutado en**: Cada PR merge

#### E2E Testing
- **Coverage**: Flujos críticos (happy path + 2 alternativas)
- **Framework**: Protractor / Cypress
- **Ejecutado en**: Nightly builds

#### Performance Testing
- **Herramienta**: JMeter, Gatling
- **Escenarios**:
  - 100 pozos con telemetría de 1 segundo
  - 1000 usuarios concurrentes en dashboards
  - Simulación de 1 semana de datos (stress test)
- **Métricas**:
  - Latencia API p95 < 200ms
  - Procesamiento telemetría < 1 segundo
  - CPU usage < 70%
  - Memory usage < 80%

#### Security Testing
- **SAST** (Static Analysis): SonarQube
- **DAST** (Dynamic Analysis): OWASP ZAP
- **Dependency Check**: Snyk
- **Penetration Testing**: Al finalizar Fase 3 y Fase 5

#### UAT (User Acceptance Testing)
- **Participantes**: 5-10 operadores + 2 ingenieros de producción
- **Duración**: 2 semanas al final de cada fase
- **Criterios de aceptación**: Definidos en cada User Story

### 11.2 Estándares de Código

#### Java Backend
```
- Estilo: Google Java Style Guide
- Formatter: google-java-format
- Linter: Checkstyle
- Complejidad ciclomática: < 10
- Método máximo: 50 líneas
- Clase máxima: 500 líneas
```

#### TypeScript Frontend
```
- Estilo: Angular Style Guide
- Formatter: Prettier
- Linter: ESLint + TSLint
- Componente máximo: 300 líneas
- No any types (usar tipos estrictos)
```

#### SQL
```
- Todas las queries preparadas (evitar SQL injection)
- Índices en columnas de búsqueda
- EXPLAIN ANALYZE para queries > 100ms
```

### 11.3 Definition of Done (DoD)

Una historia de usuario está DONE cuando:
- [ ] Código escrito y commiteado
- [ ] Unit tests escritos (coverage > 80%)
- [ ] Integration tests para API
- [ ] Code review aprobado por 2 personas
- [ ] Sin code smells críticos en SonarQube
- [ ] Documentación técnica actualizada
- [ ] API documentada en Swagger/OpenAPI
- [ ] Deployed a ambiente de Staging
- [ ] UAT completado por Product Owner
- [ ] No hay bugs críticos o high pendientes

### 11.4 CI/CD Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                      CI/CD PIPELINE                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. COMMIT to develop                                       │
│     ↓                                                       │
│  2. BUILD                                                   │
│     - Maven build                                           │
│     - npm build                                             │
│     ↓                                                       │
│  3. UNIT TESTS                                              │
│     - JUnit                                                 │
│     - Jest (Angular)                                        │
│     ↓                                                       │
│  4. STATIC ANALYSIS                                         │
│     - SonarQube scan                                        │
│     - Security scan (Snyk)                                  │
│     ↓                                                       │
│  5. INTEGRATION TESTS                                       │
│     - Spring Boot Test                                      │
│     - TestContainers                                        │
│     ↓                                                       │
│  6. BUILD DOCKER IMAGE                                      │
│     - docker build                                          │
│     - push to registry                                      │
│     ↓                                                       │
│  7. DEPLOY to DEV                                           │
│     - kubectl apply                                         │
│     - Health check                                          │
│     ↓                                                       │
│  8. E2E TESTS (nightly)                                     │
│     - Cypress tests                                         │
│     ↓                                                       │
│  9. DEPLOY to STAGING (on sprint completion)                │
│     - Manual approval                                       │
│     - kubectl apply                                         │
│     ↓                                                       │
│  10. UAT in STAGING                                         │
│      - 2 weeks testing                                      │
│      ↓                                                       │
│  11. DEPLOY to PRODUCTION (on phase completion)             │
│      - Manual approval (Product Owner + CTO)                │
│      - kubectl apply                                        │
│      - Gradual rollout (canary deployment)                  │
│      - Monitoring & alerting                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 12. Métricas de Éxito

### 12.1 KPIs Técnicos

| KPI | Target | Medición |
|-----|--------|----------|
| **System Uptime** | 99.5%+ | Monitoring 24/7 con Grafana |
| **API Latency (p95)** | < 200ms | Application Performance Monitoring |
| **Telemetry Processing** | < 1 segundo | TB Rule Engine metrics |
| **Test Coverage** | > 80% | SonarQube |
| **Code Quality (SonarQube)** | A rating | SonarQube scan |
| **Critical Bugs** | 0 in production | Jira dashboard |
| **Security Vulnerabilities** | 0 critical | Snyk scan |

### 12.2 KPIs Funcionales

| KPI | Baseline | Target | Medición |
|-----|----------|--------|----------|
| **Production Increase** | 0% | +5% | Daily production reports |
| **Equipment Uptime** | 88% | 95%+ | PO Module KPI dashboard |
| **Downtime Reduction** | - | -35% | Comparison vs. historical data |
| **Failure Prediction Accuracy** | - | 85%+ | ML model metrics |
| **Lifting Cost Reduction** | $0 | -15% | Economic KPI dashboard |
| **Energy Efficiency** | 0% | +12% | kWh/bbl tracking |

### 12.3 KPIs de Adopción

| KPI | Target | Medición |
|-----|--------|----------|
| **User Adoption Rate** | 90%+ | Active users / Total users |
| **Daily Active Users** | 80%+ | Login analytics |
| **User Satisfaction (NPS)** | > 50 | Quarterly surveys |
| **Training Completion** | 100% | LMS tracking |
| **Support Tickets** | < 10/week after 3 months | Helpdesk system |

### 12.4 KPIs de Negocio (ROI)

| Concepto | Año 1 | Año 2 | Año 3 |
|----------|-------|-------|-------|
| **Inversión** | $3.2M | $0.5M | $0.5M |
| **Ahorro Operativo** | $1.5M | $2.0M | $2.2M |
| **Producción Adicional (valorizada)** | $2.0M | $2.5M | $2.5M |
| **Beneficio Neto** | $0.3M | $4.0M | $4.2M |
| **ROI Acumulado** | 9% | 115% | 243% |

**Break-even**: Mes 14

---

## 13. Governance

### 13.1 Estructura de Gobierno

```
┌─────────────────────────────────────────────────────────────┐
│                   STEERING COMMITTEE                        │
│                  (Monthly meetings)                         │
│                                                             │
│  Members:                                                   │
│  - CTO (Chair)                                              │
│  - VP of Operations                                         │
│  - CFO                                                      │
│  - Product Owner                                            │
│                                                             │
│  Responsibilities:                                          │
│  - Approve budget and timeline changes                      │
│  - Resolve escalated issues                                 │
│  - Review project status                                    │
│  - Strategic decisions                                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   PRODUCT OWNER                             │
│                  (Daily involvement)                        │
│                                                             │
│  Responsibilities:                                          │
│  - Define and prioritize backlog                            │
│  - Accept/reject deliverables                               │
│  - Stakeholder communication                                │
│  - Business decisions                                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  PROJECT MANAGER                            │
│                  (Daily involvement)                        │
│                                                             │
│  Responsibilities:                                          │
│  - Track timeline and budget                                │
│  - Risk management                                          │
│  - Resource allocation                                      │
│  - Status reporting                                         │
│  - Vendor management                                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
         ▼             ▼             ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ Tech Lead   │ │ Scrum       │ │  QA Lead    │
│ Backend     │ │ Master      │ │             │
└─────────────┘ └─────────────┘ └─────────────┘
```

### 13.2 Reuniones de Gobierno

#### Daily Standup (15 min)
- **Frecuencia**: Diario, 9:00 AM
- **Participantes**: Dev team + Scrum Master
- **Agenda**:
  - ¿Qué hice ayer?
  - ¿Qué haré hoy?
  - ¿Hay impedimentos?

#### Sprint Planning (4 horas)
- **Frecuencia**: Cada 2 semanas, inicio de sprint
- **Participantes**: Dev team + Product Owner + Scrum Master
- **Agenda**:
  - Review de backlog
  - Selección de user stories
  - Story sizing (planning poker)
  - Definition of sprint goal

#### Sprint Review / Demo (2 horas)
- **Frecuencia**: Cada 2 semanas, fin de sprint
- **Participantes**: Dev team + stakeholders
- **Agenda**:
  - Demo de funcionalidades completadas
  - Feedback de stakeholders
  - Actualización de roadmap

#### Sprint Retrospective (1.5 horas)
- **Frecuencia**: Cada 2 semanas, después de review
- **Participantes**: Dev team + Scrum Master
- **Agenda**:
  - ¿Qué fue bien?
  - ¿Qué podemos mejorar?
  - Action items para próximo sprint

#### Technical Architecture Review (2 horas)
- **Frecuencia**: Mensual
- **Participantes**: Tech Leads + Architects + Senior Developers
- **Agenda**:
  - Decisiones de arquitectura
  - Technical debt review
  - Performance review
  - Security review

#### Steering Committee Meeting (1 hora)
- **Frecuencia**: Mensual
- **Participantes**: Steering Committee + Product Owner + PM
- **Agenda**:
  - Status report (RAG: Red/Amber/Green)
  - Budget review
  - Risk review
  - Strategic decisions

### 13.3 Reportes

#### Weekly Status Report
**Audiencia**: Product Owner, PM
**Formato**: Email
**Contenido**:
- Progreso del sprint actual (%)
- User stories completadas vs. planeadas
- Impedimentos
- Próximos hitos

#### Monthly Executive Report
**Audiencia**: Steering Committee
**Formato**: PowerPoint (10-15 slides)
**Contenido**:
- Executive summary (RAG status)
- Milestones completados
- Budget vs. actual
- Riesgos y mitigaciones
- Próximos meses (roadmap)
- Decisiones requeridas

#### Phase Completion Report
**Audiencia**: Todos los stakeholders
**Formato**: Documento (20-30 páginas)
**Contenido**:
- Objetivos de la fase
- Entregables completados
- Métricas de calidad
- Lecciones aprendidas
- Plan para siguiente fase

### 13.4 Gestión de Cambios (Change Requests)

**Proceso para Cambio de Scope**:

1. **Solicitud**:
   - Stakeholder completa Change Request Form
   - Incluye: descripción, justificación, urgencia

2. **Evaluación**:
   - Product Owner + Tech Leads evalúan
   - Estiman impacto en timeline y budget
   - Clasifican: low/medium/high impact

3. **Aprobación**:
   - Low impact: Product Owner aprueba
   - Medium impact: Product Owner + PM aprueban
   - High impact: Steering Committee aprueba

4. **Implementación**:
   - Se agrega al backlog con prioridad adecuada
   - Se actualiza roadmap
   - Se comunica a team

**SLA de Change Requests**:
- Respuesta inicial: 2 días hábiles
- Evaluación completa: 5 días hábiles
- Decisión: 10 días hábiles (high impact), 3 días (low/medium)

---

## 14. Apéndices

### 14.1 Glosario

| Término | Definición |
|---------|------------|
| **ESP** | Electric Submersible Pump - Bomba electrosumergible |
| **PCP** | Progressing Cavity Pump - Bomba de cavidad progresiva |
| **Gas Lift** | Sistema de levantamiento por inyección de gas |
| **Macolla** | Cluster de pozos / Wellpad |
| **PIP** | Pump Intake Pressure - Presión de entrada de bomba |
| **IPR** | Inflow Performance Relationship - Curva de potencial del pozo |
| **SCADA** | Supervisory Control and Data Acquisition |
| **OPC-UA** | Open Platform Communications Unified Architecture |
| **MTBF** | Mean Time Between Failures |
| **MTTR** | Mean Time To Repair |
| **TB Asset** | ThingsBoard Asset - Entidad core de TB para representar equipos |
| **TB Attribute** | ThingsBoard Attribute - Propiedades de assets (SERVER_SCOPE) |
| **ts_kv** | ThingsBoard time-series key-value tables |
| **Rule Engine** | Motor de procesamiento de mensajes de ThingsBoard |

### 14.2 Referencias

1. Documento Original de Optimización: `/nexus/.claude/optimizacion.md`
2. ThingsBoard Documentation: https://thingsboard.io/docs/
3. Módulo RV: `/nexus/common/rv-module/`
4. Módulo DR: `/nexus/common/dr-module/`
5. Módulo CT: `/nexus/common/ct-module/`
6. CTAssetService (patrón de referencia): `/common/ct-module/src/main/java/org/thingsboard/nexus/ct/service/CTAssetService.java`
7. CTAttributeService (patrón de referencia): `/common/ct-module/src/main/java/org/thingsboard/nexus/ct/service/CTAttributeService.java`

### 14.3 Contactos

- **Product Owner**: Hector Diaz - hector.diaz@nexus.com
- **CTO**: TBD
- **VP Operations**: TBD

---

**Fin del Master Plan v2.0**

**Próximos Pasos**:
1. Review con Steering Committee → Semana del 10 Feb 2026
2. Aprobación de budget → 20 Feb 2026
3. Inicio de contratación de equipo → 25 Feb 2026
4. Kickoff oficial del proyecto → 10 Mar 2026

---

*Este documento es confidencial y propiedad de Nexus. No distribuir sin autorización.*
