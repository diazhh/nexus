# TECHNICAL STACK - PF & PO Modules

**Proyecto**: Nexus Production Facilities & Optimization
**Versión**: 2.0
**Fecha**: 2026-02-03
**Arquitectura**: ThingsBoard Core (Assets, Attributes, ts_kv, Alarm System)

---

## 📋 Stack Overview

El stack tecnológico está basado en la arquitectura existente de **Nexus (ThingsBoard 4.3.0 Extended)** para mantener consistencia y aprovechar la infraestructura actual.

> **Decisión Arquitectónica Clave**: Los módulos PF y PO utilizan **tablas core de ThingsBoard** (asset, attribute_kv, ts_kv, alarm) en lugar de tablas custom, siguiendo el patrón establecido por los módulos CT (Coiled Tubing) y RV (Yacimientos).

```
┌─────────────────────────────────────────────────────────────────┐
│                       TECHNOLOGY STACK                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  FRONTEND                                                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Angular 18.2.13 + TypeScript 5.5.4                      │  │
│  │  ├─ Angular Material 18.2.14                             │  │
│  │  ├─ NgRx (State Management)                              │  │
│  │  ├─ RxJS 7.8 (Reactive Programming)                      │  │
│  │  ├─ ECharts 5.4 (Visualizations)                         │  │
│  │  ├─ Leaflet 1.9 (Maps)                                   │  │
│  │  └─ Tailwind CSS 3.3 (Styling)                           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  BACKEND                                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Java 17 + Spring Boot 3.4.10                            │  │
│  │  ├─ ThingsBoard Service Wrappers                         │  │
│  │  ├─ Spring Security 6                                    │  │
│  │  ├─ Spring WebFlux (Reactive)                            │  │
│  │  ├─ Spring Cloud (Microservices)                         │  │
│  │  └─ Lombok (Boilerplate reduction)                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  DATA LAYER (ThingsBoard Core)                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  PostgreSQL 14+ (TB Core Tables)                         │  │
│  │  ├─ asset (Asset Types: pf_well, pf_wellpad, etc.)      │  │
│  │  ├─ attribute_kv (SERVER_SCOPE attributes)              │  │
│  │  ├─ ts_kv, ts_kv_latest (Time-series telemetry)        │  │
│  │  ├─ alarm (TB Alarm System)                              │  │
│  │  └─ relation (Asset hierarchies)                         │  │
│  │  Custom Tables (Solo 2):                                 │  │
│  │  ├─ pf_optimization_result                               │  │
│  │  └─ pf_recommendation                                    │  │
│  │  Redis 7.0 (Cache)                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  MESSAGING & STREAMING                                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Apache Kafka 3.3                                        │  │
│  │  TB Rule Engine (Stream Processing)                      │  │
│  │  Custom Rule Nodes (PfDataQualityNode, etc.)            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  IoT PROTOCOLS (ThingsBoard Native)                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  MQTT (TB MQTT Transport)                                │  │
│  │  OPC-UA (TB Gateway)                                     │  │
│  │  Modbus (TB Gateway)                                     │  │
│  │  HTTP/REST (TB HTTP Transport)                           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  MACHINE LEARNING                                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Python 3.11                                             │  │
│  │  ├─ TensorFlow 2.15 / PyTorch 2.1                       │  │
│  │  ├─ scikit-learn 1.4                                    │  │
│  │  ├─ pandas 2.1, numpy 1.26                              │  │
│  │  ├─ Flask 3.0 (ML API)                                  │  │
│  │  └─ MLflow (Model Management)                            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  DEVOPS & INFRASTRUCTURE                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Docker 24.0 + Docker Compose                           │  │
│  │  Kubernetes 1.28 (Orchestration)                         │  │
│  │  GitHub Actions / Jenkins (CI/CD)                        │  │
│  │  Terraform (Infrastructure as Code)                      │  │
│  │  Grafana + Prometheus (Monitoring)                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎨 Frontend Stack

### Core Framework
```json
{
  "framework": "Angular 18.2.13",
  "language": "TypeScript 5.5.4",
  "build_tool": "Angular CLI + Webpack",
  "package_manager": "npm 10.2"
}
```

### UI Libraries

#### Angular Material 18.2.14
**Uso**: Componentes UI base
```typescript
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule } from '@angular/material/dialog';
```

**Componentes Principales**:
- Buttons, Inputs, Selects
- Tables con sorting/pagination
- Dialogs modales
- Snackbars (notificaciones)
- Menus y toolbars

#### ECharts 5.4.3
**Uso**: Gráficas y visualizaciones de datos
```typescript
import * as echarts from 'echarts';
```

**Tipos de gráficas**:
- Line charts (tendencias de telemetría)
- Bar charts (producción por pozo)
- Pie charts (distribución de sistemas de levantamiento)
- Gauge charts (Health Score)
- Scatter plots (eficiencia vs. producción)

#### Leaflet 1.9.4
**Uso**: Mapas geográficos
```typescript
import * as L from 'leaflet';
```

**Features**:
- Mapa de campo con pozos
- Markers con código de color por estado
- Clustering de pozos
- Heatmaps de producción
- Líneas de recolección

#### NgRx (State Management)
**Uso**: Gestión de estado global
```typescript
import { Store } from '@ngrx/store';
import { createAction, createReducer } from '@ngrx/store';
```

**Estado Gestionado**:
- Estado de pozos (wellState)
- Alarmas activas (alarmState) - via TB Alarm Service
- Recomendaciones (recommendationState)
- Configuración de usuario (userPreferencesState)

### Build & Dev Tools

```json
{
  "webpack": "5.89",
  "postcss": "8.4",
  "tailwindcss": "3.3",
  "prettier": "3.0",
  "eslint": "8.50",
  "jest": "29.7"
}
```

---

## ⚙️ Backend Stack

### Core Framework

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.10</version>
</dependency>
```

**Java Version**: 17 (LTS)

### Spring Modules

#### Wrapper Services Pattern (ThingsBoard Core)

> **IMPORTANTE**: Los módulos PF y PO **NO** usan Spring Data JPA directamente para las entidades del dominio. En su lugar, usan **Wrapper Services** que encapsulan los servicios core de ThingsBoard.

**Patrón Correcto - Wrapper Service**:
```java
@Service
@RequiredArgsConstructor
public class PfAssetService {

    private final AssetService assetService;  // TB Core Service

    public static final String TYPE_WELL = "pf_well";
    public static final String TYPE_WELLPAD = "pf_wellpad";

    public Asset createAsset(UUID tenantId, String type, String name) {
        Asset asset = new Asset();
        asset.setTenantId(new TenantId(tenantId));
        asset.setType(type);
        asset.setName(name);
        return assetService.saveAsset(asset);
    }

    public List<Asset> getAssetsByType(UUID tenantId, String type) {
        return assetService.findAssetsByTenantIdAndType(
            new TenantId(tenantId), type, new PageLink(1000)
        ).getData();
    }
}
```

**DTO Pattern con Constantes**:
```java
@Data
@Builder
public class PfWellDto {
    // Asset Type constant
    public static final String ASSET_TYPE = "pf_well";

    // Attribute key constants
    public static final String ATTR_API_NUMBER = "api_number";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_LIFT_SYSTEM_TYPE = "lift_system_type";
    public static final String ATTR_MEASURED_DEPTH_FT = "measured_depth_ft";

    // DTO fields
    private UUID assetId;  // TB Asset ID
    private String name;
    private String apiNumber;
    private WellStatus status;
    private LiftSystemType liftSystemType;
    private Double measuredDepthFt;

    // Convert to attribute map
    public Map<String, Object> toAttributeMap() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ATTR_API_NUMBER, apiNumber);
        attrs.put(ATTR_STATUS, status.name());
        attrs.put(ATTR_LIFT_SYSTEM_TYPE, liftSystemType.name());
        attrs.put(ATTR_MEASURED_DEPTH_FT, measuredDepthFt);
        return attrs;
    }
}
```

#### Spring Security
**Uso**: Autenticación y autorización
```java
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
public PfWellDto createWell(PfWellDto well) { }
```

#### Spring WebFlux (Reactive)
**Uso**: APIs reactivas para telemetría en tiempo real via TB WebSocket
```java
@GetMapping(value = "/telemetry/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<TelemetryData> streamTelemetry(@PathParam String assetId) {
    // Uses TB TelemetryService internally
    return telemetryService.getRealtimeStream(new AssetId(UUID.fromString(assetId)));
}
```

### Key Dependencies

```xml
<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
</dependency>

<!-- Kafka (via TB Rule Engine) -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
    <version>3.1.0</version>
</dependency>

<!-- JSON Processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.3</version>
</dependency>

<!-- ThingsBoard Common (for service access) -->
<dependency>
    <groupId>org.thingsboard</groupId>
    <artifactId>common-data</artifactId>
</dependency>
```

---

## 🗄️ Data Layer

### Arquitectura de Datos - ThingsBoard Core

> **Decisión Arquitectónica**: El módulo PF/PO utiliza las **tablas core de ThingsBoard** para almacenar datos, NO tablas custom en un schema separado. Esto garantiza consistencia con otros módulos (CT, RV) y aprovecha las capacidades nativas de TB.

```
┌─────────────────────────────────────────────────────────────────┐
│                    DATA LAYER ARCHITECTURE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ThingsBoard Core Tables (Public Schema)                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  asset                                                    │  │
│  │  ├─ type: pf_well, pf_wellpad, pf_esp_system, etc.       │  │
│  │  ├─ tenant_id, customer_id                               │  │
│  │  └─ additional_info (JSONB)                              │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │  attribute_kv (SERVER_SCOPE)                              │  │
│  │  ├─ entity_id → asset.id                                 │  │
│  │  ├─ attribute_key: api_number, status, etc.              │  │
│  │  └─ str_v, long_v, dbl_v, json_v                         │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │  ts_kv, ts_kv_latest (Time-series)                        │  │
│  │  ├─ entity_id → asset.id                                 │  │
│  │  ├─ key: motor_temp, wellhead_pressure, etc.             │  │
│  │  └─ ts, dbl_v (or str_v, bool_v, json_v)                 │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │  alarm                                                    │  │
│  │  ├─ originator_id → asset.id                             │  │
│  │  ├─ type: HIGH_MOTOR_TEMP, LOW_PRODUCTION, etc.          │  │
│  │  └─ severity, status, start_ts, ack_ts, clear_ts         │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │  relation                                                 │  │
│  │  ├─ from_id, to_id                                       │  │
│  │  └─ relation_type: Contains, BelongsTo, HasSystem        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Custom Tables (SOLO para datos que no encajan en TB Core)     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  pf_optimization_result                                   │  │
│  │  ├─ Resultados de optimización con workflow complejo     │  │
│  │  └─ JOINs analíticos, agregaciones históricas            │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │  pf_recommendation                                        │  │
│  │  ├─ Recomendaciones con ciclo de vida                    │  │
│  │  └─ Estados: PENDING → APPROVED → EXECUTING → COMPLETED  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### PostgreSQL 14+ - ThingsBoard Core Tables

**NO SE CREAN SCHEMAS CUSTOM (pf.*, po.*)**. Los módulos usan las tablas existentes de ThingsBoard:

```sql
-- TABLA: asset (ThingsBoard Core)
-- Se usa el campo 'type' para identificar assets del módulo PF
-- Ejemplo de consulta para obtener pozos:
SELECT * FROM asset WHERE type = 'pf_well' AND tenant_id = :tenantId;

-- TABLA: attribute_kv (ThingsBoard Core)
-- Almacena propiedades de los assets como atributos SERVER_SCOPE
-- Ejemplo de consulta para obtener atributos de un pozo:
SELECT * FROM attribute_kv
WHERE entity_id = :assetId
  AND attribute_type = 'SERVER_SCOPE';

-- TABLA: ts_kv (ThingsBoard Core)
-- Almacena telemetría time-series (nativo TB, particionado)
-- Ejemplo de consulta para obtener telemetría:
SELECT ts, key, dbl_v FROM ts_kv
WHERE entity_id = :assetId
  AND key IN ('motor_temp', 'current_amps', 'wellhead_pressure')
  AND ts BETWEEN :startTs AND :endTs
ORDER BY ts DESC;

-- TABLA: alarm (ThingsBoard Core)
-- Almacena alarmas usando el TB Alarm System
-- Ejemplo de consulta para alarmas activas:
SELECT * FROM alarm
WHERE originator_id = :assetId
  AND status = 'ACTIVE_UNACK'
ORDER BY start_ts DESC;
```

### Custom Tables (Solo 2)

**Justificación**: Estas tablas custom son necesarias porque:
1. Tienen ciclo de vida con workflow de aprobación
2. Requieren queries analíticos complejos (JOINs, agregaciones)
3. No encajan en el modelo Asset/Attribute de ThingsBoard

```sql
-- Tabla Custom: pf_optimization_result
-- Almacena resultados de optimización con análisis complejo
CREATE TABLE pf_optimization_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    well_id UUID NOT NULL,  -- Referencia a TB Asset (pf_well)
    optimization_type VARCHAR(50) NOT NULL,
    input_parameters JSONB,
    output_parameters JSONB,
    improvement_percent DOUBLE PRECISION,
    energy_savings_kwh DOUBLE PRECISION,
    production_increase_bpd DOUBLE PRECISION,
    calculated_at BIGINT NOT NULL,
    created_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

CREATE INDEX idx_opt_result_tenant ON pf_optimization_result(tenant_id);
CREATE INDEX idx_opt_result_well ON pf_optimization_result(well_id);
CREATE INDEX idx_opt_result_type ON pf_optimization_result(optimization_type);
CREATE INDEX idx_opt_result_time ON pf_optimization_result(calculated_at);

-- Tabla Custom: pf_recommendation
-- Almacena recomendaciones con workflow de aprobación
CREATE TABLE pf_recommendation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    well_id UUID NOT NULL,  -- Referencia a TB Asset (pf_well)
    type VARCHAR(50) NOT NULL,
    current_value DOUBLE PRECISION,
    recommended_value DOUBLE PRECISION,
    expected_benefit_bpd DOUBLE PRECISION,
    expected_savings_usd DOUBLE PRECISION,
    confidence DOUBLE PRECISION,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_by UUID,
    approved_by UUID,
    executed_by UUID,
    created_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    approved_time BIGINT,
    executed_time BIGINT,
    expires_at BIGINT,
    notes TEXT
);

CREATE INDEX idx_rec_tenant ON pf_recommendation(tenant_id);
CREATE INDEX idx_rec_well ON pf_recommendation(well_id);
CREATE INDEX idx_rec_status ON pf_recommendation(status);
CREATE INDEX idx_rec_priority ON pf_recommendation(priority);
```

### Time-Series: ts_kv (ThingsBoard Native)

> **NO SE USA TimescaleDB CUSTOM**. ThingsBoard ya proporciona almacenamiento time-series optimizado con las tablas `ts_kv` y `ts_kv_latest`.

**ThingsBoard Time-Series Features**:
- Particionamiento automático por tiempo
- Políticas de retención configurables
- Agregaciones nativas (AVG, MIN, MAX, SUM, COUNT)
- API de telemetría con subscripciones WebSocket

**Consulta de Telemetría via TB Service**:
```java
@Service
@RequiredArgsConstructor
public class PfTelemetryService {

    private final TimeseriesService timeseriesService;  // TB Core Service

    public List<TsKvEntry> getLatestTelemetry(UUID assetId, List<String> keys) {
        return timeseriesService.findLatest(
            TenantId.SYS_TENANT_ID,
            new AssetId(assetId),
            keys
        ).get();
    }

    public List<TsKvEntry> getTelemetryHistory(UUID assetId, List<String> keys,
                                                long startTs, long endTs) {
        return timeseriesService.findAll(
            TenantId.SYS_TENANT_ID,
            new AssetId(assetId),
            new BaseReadTsKvQuery(keys, startTs, endTs, 0, 10000, Aggregation.NONE)
        ).get();
    }
}
```

### Redis 7.0
**Uso**: Cache de datos frecuentes

**Data Structures**:
```
# Current well status (from TB attributes)
well:{assetId}:status → JSON string
TTL: 5 minutes

# Latest telemetry (from ts_kv_latest)
well:{assetId}:telemetry:latest → Hash
TTL: 1 minute

# Active alarms (from TB alarm table)
alarms:active:{tenantId} → Sorted Set (by timestamp)
TTL: 30 seconds (refresh from TB)

# Session data
session:{sessionId} → Hash
TTL: 30 minutes
```

**Configuration**:
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    jedis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

---

## 📨 Messaging & Streaming

### ThingsBoard Rule Engine

> **Procesamiento de Telemetría**: Los módulos PF/PO usan el **TB Rule Engine** para procesar telemetría, NO Kafka Streams custom.

**Rule Chain: PF Root**
```
Device → [Message Type Switch]
              ↓
    [Post Telemetry] → [PfDataQualityNode] → [Save Timeseries]
              ↓                   ↓
    [PfAlarmEvaluationNode] → [Create/Clear Alarm]
              ↓
    [Route to PO Module] → [PfOptimizationTriggerNode]
```

**Custom Rule Nodes**:

```java
@RuleNode(
    type = ComponentType.FILTER,
    name = "PF Data Quality",
    nodeDescription = "Validates and enriches PF telemetry data"
)
public class PfDataQualityNode implements TbNode {

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        JsonNode data = JacksonUtil.toJsonNode(msg.getData());

        // Calculate quality score
        double qualityScore = calculateQuality(data);

        if (qualityScore >= 0.7) {
            // Enrich with quality metadata
            ObjectNode enriched = JacksonUtil.newObjectNode();
            enriched.setAll((ObjectNode) data);
            enriched.put("quality_score", qualityScore);

            TbMsg newMsg = TbMsg.transformMsgData(msg, enriched.toString());
            ctx.tellSuccess(newMsg);
        } else {
            ctx.tellFailure(msg, new RuntimeException("Low quality data: " + qualityScore));
        }
    }
}
```

### Apache Kafka 3.3

**Topics** (via TB Rule Engine):
```
tb_rule_engine.main             - Main rule engine queue
tb_rule_engine.notifications    - Notification queue
tb_core.transport.api.requests  - Transport API requests
```

**Custom Topics** (solo para PO optimization):
```
nexus.pf.telemetry.enriched     - Telemetría enriquecida para ML
nexus.po.recommendations        - Recomendaciones generadas
nexus.po.setpoint-changes       - Cambios de setpoint
```

**Configuration**:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: nexus-po-consumer
      auto-offset-reset: earliest
    producer:
      acks: all
      retries: 3
```

---

## 🌐 IoT Protocols

### ThingsBoard Native Transports

> Los módulos PF/PO utilizan los **transportes nativos de ThingsBoard** para comunicación con dispositivos, NO implementaciones custom.

### MQTT (TB MQTT Transport)
**Uso**: Comunicación con devices de campo

**Topic Structure** (TB Standard):
```
v1/devices/me/telemetry         - Device → Platform (telemetry)
v1/devices/me/attributes        - Device → Platform (attributes)
v1/devices/me/rpc/request/+     - Platform → Device (RPC)
v1/devices/me/rpc/response/+    - Device → Platform (RPC response)
```

**QoS**: QoS 1 (at least once delivery)

### OPC-UA (TB Gateway)
**Uso**: Integración con PLCs y SCADA

**Configuration** (tb-gateway.yaml):
```yaml
connectors:
  - name: SCADA OPC-UA
    type: opcua
    configuration:
      server:
        url: opc.tcp://scada-server:4840/nexus/pf
        security: None
      mapping:
        - deviceNodePattern: ns=2;s=Well\.*
          deviceNamePattern: ${Well.name}
          attributes:
            - key: frequency
              path: ns=2;s=Well.${deviceName}.Frequency
          timeseries:
            - key: motor_temp
              path: ns=2;s=Well.${deviceName}.MotorTemp
            - key: current_amps
              path: ns=2;s=Well.${deviceName}.Current
```

### Modbus (TB Gateway)
**Uso**: RTUs y PLCs legacy

**Configuration**:
```yaml
connectors:
  - name: RTU Modbus
    type: modbus
    configuration:
      master:
        port: 502
        slaves:
          - host: 192.168.1.100
            unitId: 1
            timeseries:
              - key: wellhead_pressure
                address: 40001
                type: float
              - key: casing_pressure
                address: 40003
                type: float
```

---

## 🤖 Machine Learning Stack

### Python 3.11

**ML Service** (Microservicio separado):
```
ml-service/
├── models/
│   ├── esp_failure_predictor.py
│   ├── anomaly_detector.py
│   └── production_forecaster.py
├── training/
│   ├── train_esp_model.py
│   └── evaluate_model.py
├── api/
│   └── app.py (Flask REST API)
└── requirements.txt
```

### Key Libraries

```txt
# requirements.txt
tensorflow==2.15.0
torch==2.1.0
scikit-learn==1.4.0
pandas==2.1.4
numpy==1.26.3
scipy==1.11.4
flask==3.0.0
mlflow==2.9.2
joblib==1.3.2
```

### TensorFlow / PyTorch
**Uso**: Deep learning para predicción de fallas

**Arquitectura típica (LSTM para ESP failure)**:
```python
import tensorflow as tf
from tensorflow.keras import layers

model = tf.keras.Sequential([
    layers.LSTM(64, return_sequences=True, input_shape=(timesteps, features)),
    layers.Dropout(0.2),
    layers.LSTM(32, return_sequences=False),
    layers.Dropout(0.2),
    layers.Dense(16, activation='relu'),
    layers.Dense(1, activation='sigmoid')  # Binary: fail / no fail
])

model.compile(
    optimizer='adam',
    loss='binary_crossentropy',
    metrics=['accuracy', 'precision', 'recall']
)
```

### scikit-learn
**Uso**: Modelos tradicionales ML

**Anomaly Detection (Isolation Forest)**:
```python
from sklearn.ensemble import IsolationForest

model = IsolationForest(
    n_estimators=100,
    contamination=0.05,  # 5% expected anomalies
    random_state=42
)

model.fit(X_train)
predictions = model.predict(X_test)  # -1 = anomaly, 1 = normal
```

### MLflow
**Uso**: Model management y tracking

```python
import mlflow
import mlflow.tensorflow

with mlflow.start_run():
    mlflow.log_param("lstm_units", 64)
    mlflow.log_param("dropout_rate", 0.2)

    # Training
    history = model.fit(X_train, y_train, epochs=50)

    # Log metrics
    mlflow.log_metric("accuracy", history.history['accuracy'][-1])
    mlflow.log_metric("val_accuracy", history.history['val_accuracy'][-1])

    # Save model
    mlflow.tensorflow.log_model(model, "esp_failure_model")
```

### Flask API
**Uso**: Exponer modelos ML como REST API

```python
from flask import Flask, request, jsonify
import mlflow.tensorflow

app = Flask(__name__)

# Load model
model = mlflow.tensorflow.load_model("models:/esp_failure_model/production")

@app.route('/predict/esp-failure', methods=['POST'])
def predict_esp_failure():
    data = request.json
    features = extract_features(data['telemetry'])

    prediction = model.predict(features)
    probability = float(prediction[0][0])

    return jsonify({
        'well_id': data['well_id'],  # TB Asset ID
        'failure_probability': probability,
        'days_to_failure': estimate_days(probability),
        'confidence': calculate_confidence(features)
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

---

## 🐳 DevOps & Infrastructure

### Docker

**Dockerfile (Backend)**:
```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/nexus-pf-*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Dockerfile (ML Service)**:
```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5000

CMD ["python", "api/app.py"]
```

**Docker Compose** (Development):
```yaml
version: '3.8'

services:
  # ThingsBoard handles PostgreSQL, Redis, Kafka internally
  # For dev, we can use TB docker-compose

  thingsboard:
    image: thingsboard/tb-postgres:latest
    ports:
      - "8080:8080"
      - "1883:1883"  # MQTT
      - "5683:5683"  # CoAP
    environment:
      TB_QUEUE_TYPE: in-memory
    volumes:
      - tb_data:/data
      - tb_logs:/var/log/thingsboard

  nexus-pf-module:
    build: ./pf-module
    ports:
      - "8081:8081"
    depends_on:
      - thingsboard
    environment:
      TB_URL: http://thingsboard:8080
      TB_USERNAME: tenant@thingsboard.org
      TB_PASSWORD: tenant

  ml-service:
    build: ./ml-service
    ports:
      - "5000:5000"
    volumes:
      - ml_models:/app/models

volumes:
  tb_data:
  tb_logs:
  ml_models:
```

### Kubernetes

**Deployment** (Backend):
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nexus-pf-backend
  namespace: nexus
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nexus-pf-backend
  template:
    metadata:
      labels:
        app: nexus-pf-backend
    spec:
      containers:
      - name: backend
        image: nexus-registry/pf-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: TB_URL
          valueFrom:
            configMapKeyRef:
              name: nexus-config
              key: thingsboard-url
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

**Service**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: nexus-pf-backend-svc
  namespace: nexus
spec:
  type: LoadBalancer
  selector:
    app: nexus-pf-backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

### CI/CD (GitHub Actions)

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Build with Maven
      run: mvn clean install -DskipTests

    - name: Run Unit Tests
      run: mvn test

    - name: Run Integration Tests
      run: mvn verify -Pintegration-tests

    - name: SonarQube Scan
      run: mvn sonar:sonar -Dsonar.projectKey=nexus-pf

    - name: Build Docker Image
      run: docker build -t nexus/pf-backend:${{ github.sha }} .

    - name: Push to Registry
      run: docker push nexus/pf-backend:${{ github.sha }}

    - name: Deploy to Dev
      if: github.ref == 'refs/heads/develop'
      run: kubectl set image deployment/nexus-pf-backend backend=nexus/pf-backend:${{ github.sha }} -n nexus-dev
```

### Monitoring (Grafana + Prometheus)

**Spring Boot Actuator** (metrics exposure):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Prometheus Scrape Config**:
```yaml
scrape_configs:
  - job_name: 'nexus-pf-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['nexus-pf-backend-svc:8080']
```

**Grafana Dashboards**:
- JVM metrics (heap, CPU, threads)
- HTTP request metrics (rate, latency, errors)
- TB Asset counts by type
- TB Alarm statistics
- Custom business metrics (pozos monitoreados, alarmas activas)

---

## 🔧 Development Tools

### IDEs
- **IntelliJ IDEA Ultimate 2023.3** (Backend)
- **WebStorm 2023.3** (Frontend)
- **PyCharm Professional 2023.3** (ML Service)

### Code Quality
- **SonarQube 10.3** (Static analysis)
- **Checkstyle** (Java style)
- **ESLint + Prettier** (TypeScript style)
- **Black** (Python formatter)

### Testing
- **JUnit 5** (Unit tests Java)
- **Mockito** (Mocking)
- **TestContainers** (Integration tests with TB)
- **Jest** (Unit tests TypeScript)
- **Cypress** (E2E tests)
- **pytest** (Python tests)

### API Documentation
- **Swagger / OpenAPI 3.0**
- **Springdoc OpenAPI**

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Nexus PF/PO API")
                .version("2.0")
                .description("Production Facilities & Optimization APIs\n\n" +
                    "Uses ThingsBoard Core for data storage (Assets, Attributes, ts_kv, Alarms)"));
    }
}
```

Acceso: `http://localhost:8080/swagger-ui.html`

---

## 📊 Technology Decision Matrix

| Decisión | Opciones Consideradas | Opción Elegida | Justificación |
|----------|----------------------|----------------|---------------|
| **Backend Language** | Java, Python, Go | Java 17 | Consistencia con ThingsBoard, ecosystem maduro |
| **Frontend Framework** | Angular, React, Vue | Angular 18 | Ya usado en Nexus, TypeScript nativo |
| **Entity Storage** | Custom Tables, TB Assets | **TB Assets** | Consistencia con CT/RV, evita duplicación |
| **Properties Storage** | DB Columns, TB Attributes | **TB Attributes** | Flexible, no requiere migrations |
| **Time-Series DB** | TimescaleDB custom, TB ts_kv | **TB ts_kv** | Ya optimizado, API nativa, particionado |
| **Alarm System** | Custom table, TB Alarms | **TB Alarms** | Dashboard integration, API completa |
| **Message Queue** | Kafka, RabbitMQ | Kafka (via TB) | Ya integrado en ThingsBoard |
| **Cache** | Redis, Memcached | Redis | Data structures ricas, pub/sub |
| **ML Framework** | TensorFlow, PyTorch | TensorFlow + scikit-learn | TF para DL, sklearn para traditional ML |
| **Containerization** | Docker, Podman | Docker | Estándar de industria |
| **Orchestration** | Kubernetes, Docker Swarm | Kubernetes | Estándar para producción |
| **CI/CD** | GitHub Actions, Jenkins | GitHub Actions | Integración nativa con GitHub |

---

## 🔐 Security Considerations

### Authentication & Authorization
- **JWT** tokens (siguiendo estándar ThingsBoard)
- **Role-Based Access Control (RBAC)**
- **Multi-tenant isolation** via TB TenantId

### Data Encryption
- **TLS 1.3** para todas las comunicaciones
- **Encryption at rest** para base de datos (PostgreSQL native)
- **Secrets management** con Kubernetes Secrets o Vault

### API Security
- **Rate limiting** (100 requests/min por usuario)
- **Input validation** con Jakarta Validation
- **SQL injection prevention** con prepared statements (TB Services)
- **XSS prevention** con sanitización de inputs

---

## 📦 Third-Party Services

| Servicio | Provider | Uso | Costo Estimado |
|----------|----------|-----|----------------|
| **Cloud Hosting** | AWS / Azure / GCP | Compute, Storage | $5K-8K/mes |
| **Monitoring** | DataDog / New Relic | APM, Logs | $2K/mes |
| **Error Tracking** | Sentry | Error monitoring | $500/mes |
| **Email Service** | SendGrid | Notificaciones | $200/mes |
| **SMS Service** | Twilio | Alertas SMS | $500/mes |

---

## 🚀 Performance Targets

| Métrica | Target | Medición |
|---------|--------|----------|
| **API Latency (p95)** | < 200ms | Prometheus |
| **Telemetry Processing** | < 1 seg | TB Rule Engine metrics |
| **TB API Query Time** | < 100ms (p95) | TB metrics |
| **Frontend Page Load** | < 2 seg | Lighthouse |
| **Concurrent Users** | 1000+ | Load testing (JMeter) |
| **Throughput** | 10K req/sec | Gatling |

---

## 📚 Referencias

- [ThingsBoard Documentation](https://thingsboard.io/docs/)
- [ThingsBoard API Reference](https://thingsboard.io/docs/reference/rest-api/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Angular Documentation](https://angular.io/docs)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [TensorFlow Guide](https://www.tensorflow.org/guide)
- [Kubernetes Documentation](https://kubernetes.io/docs/home/)

---

**Última Actualización**: 2026-02-03
**Arquitectura**: ThingsBoard Core (Assets, Attributes, ts_kv, Alarm System)
**Mantenedor**: Tech Lead Backend / Architect
