# DIAGRAMAS DE ARQUITECTURA - PF & PO Modules

**Proyecto**: Nexus Production Facilities & Optimization
**Versión**: 2.0
**Fecha**: 2026-02-03
**Arquitectura**: ThingsBoard Core (Assets, Attributes, ts_kv, Alarm System)
**Herramientas**: Mermaid, PlantUML

---

## 📋 Índice

1. [Arquitectura General](#arquitectura-general)
2. [Modelo de Datos (ERD)](#erd)
3. [Flujos de Datos](#flujos)
4. [Secuencias de Operación](#secuencias)
5. [Despliegue](#despliegue)

---

## 1. Arquitectura General {#arquitectura-general}

### 1.1 Arquitectura de Capas - ThingsBoard Core (Mermaid)

```mermaid
graph TB
    subgraph "Presentation Layer"
        UI[Angular Frontend]
        Mobile[Mobile App]
    end

    subgraph "Application Layer"
        PF[PF Module<br/>Facilities]
        PO[PO Module<br/>Optimization]
        RV[RV Module<br/>Reservoirs]
        DR[DR Module<br/>Drilling]
        CT[CT Module<br/>Coiled Tubing]
    end

    subgraph "ThingsBoard Core Services"
        AssetSvc[Asset Service]
        AttrSvc[Attributes Service]
        TsSvc[Telemetry Service]
        AlarmSvc[TB Alarm Service]
        RelSvc[Relation Service]
        RuleEngine[Rule Engine]
    end

    subgraph "ThingsBoard Data Layer"
        PG[(PostgreSQL<br/>asset, attribute_kv<br/>relation, alarm)]
        TS[(ts_kv Tables<br/>Time-series)]
        Redis[(Redis Cache)]
        Kafka[Kafka Streams]
    end

    subgraph "Custom Tables (Only)"
        CustomPG[(PostgreSQL<br/>pf_optimization_result<br/>pf_recommendation)]
    end

    subgraph "Integration Layer"
        SCADA[SCADA/DCS]
        Historian[Historian]
        ERP[ERP System]
    end

    UI --> PF
    UI --> PO
    Mobile --> PF

    PF --> AssetSvc
    PF --> AttrSvc
    PF --> TsSvc
    PF --> AlarmSvc
    PO --> AssetSvc
    PO --> AttrSvc
    PO --> CustomPG

    RV --> PF
    PF --> PO

    AssetSvc --> PG
    AttrSvc --> PG
    TsSvc --> TS
    TsSvc --> Kafka
    AlarmSvc --> PG
    RelSvc --> PG
    RuleEngine --> TsSvc
    RuleEngine --> AlarmSvc

    TsSvc --> SCADA
    TsSvc --> Historian
    PO --> ERP

    style PF fill:#4A90E2
    style PO fill:#7ED321
    style RV fill:#F5A623
    style RuleEngine fill:#9B59B6
```

### 1.2 Arquitectura de Módulos PF y PO - ThingsBoard Native

```mermaid
graph LR
    subgraph "RV Module"
        IPR[IPR Curves]
        PVT[PVT Properties]
        Decline[Decline Curves]
    end

    subgraph "PF Module - ThingsBoard Assets"
        Wells[pf_well Assets]
        Wellpads[pf_wellpad Assets]
        FlowStations[pf_flow_station Assets]
        EspSystems[pf_esp_system Assets]
        RuleNodes[Rule Nodes<br/>PfDataQualityNode<br/>PfAlarmEvaluationNode]
        TBAlarms[TB Alarm System]
    end

    subgraph "PO Module - Optimization"
        EspOpt[ESP Optimizer]
        GasOpt[Gas Lift Optimizer]
        DilOpt[Diluent Optimizer]
        MLModels[ML Models]
        HealthScore[Health Score<br/>via Attributes]
        Recommendations[pf_recommendation<br/>Custom Table]
    end

    IPR --> EspOpt
    PVT --> EspOpt
    Decline --> HealthScore

    Wells --> RuleNodes
    RuleNodes --> TBAlarms
    RuleNodes --> EspOpt
    RuleNodes --> MLModels
    RuleNodes --> HealthScore

    EspOpt --> Recommendations
    GasOpt --> Recommendations
    DilOpt --> Recommendations
    MLModels --> Recommendations

    style PF Module fill:#E3F2FD
    style PO Module fill:#E8F5E9
    style RV Module fill:#FFF3E0
```

### 1.3 Arquitectura de Servicios Wrapper

```mermaid
graph TB
    subgraph "PF Module Services"
        PfAssetSvc[PfAssetService<br/>Wrapper]
        PfAttrSvc[PfAttributeService<br/>Wrapper]
        PfWellSvc[PfWellService]
        PfAlarmSvc[PfAlarmService<br/>Wrapper]
    end

    subgraph "ThingsBoard Core Services"
        TBAssetSvc[TB AssetService]
        TBAttrSvc[TB AttributesService]
        TBAlarmSvc[TB AlarmService]
        TBTsSvc[TB TelemetryService]
    end

    subgraph "ThingsBoard Data Layer"
        AssetTbl[(asset table)]
        AttrTbl[(attribute_kv table)]
        AlarmTbl[(alarm table)]
        TsKvTbl[(ts_kv tables)]
    end

    PfAssetSvc --> TBAssetSvc
    PfAttrSvc --> TBAttrSvc
    PfAlarmSvc --> TBAlarmSvc
    PfWellSvc --> PfAssetSvc
    PfWellSvc --> PfAttrSvc

    TBAssetSvc --> AssetTbl
    TBAttrSvc --> AttrTbl
    TBAlarmSvc --> AlarmTbl
    TBTsSvc --> TsKvTbl

    style PfAssetSvc fill:#4A90E2
    style PfAttrSvc fill:#4A90E2
    style PfWellSvc fill:#4A90E2
    style PfAlarmSvc fill:#4A90E2
```

---

## 2. Modelo de Datos (ERD) {#erd}

### 2.1 Arquitectura de Datos - ThingsBoard Core

Este diagrama muestra cómo el módulo PF utiliza las tablas core de ThingsBoard en lugar de tablas custom.

```mermaid
erDiagram
    TB_ASSET ||--o{ TB_ATTRIBUTE_KV : "has attributes"
    TB_ASSET ||--o{ TB_TS_KV : "has telemetry"
    TB_ASSET ||--o{ TB_ALARM : "has alarms"
    TB_ASSET ||--o{ TB_RELATION : "relates to"
    TB_ASSET ||--o{ PF_OPTIMIZATION_RESULT : "optimizations for"
    TB_ASSET ||--o{ PF_RECOMMENDATION : "recommendations for"

    TB_ASSET {
        uuid id PK
        uuid tenant_id FK
        uuid customer_id FK
        string name
        string type "pf_well, pf_wellpad, etc"
        string label
        jsonb additional_info
        bigint created_time
    }

    TB_ATTRIBUTE_KV {
        uuid entity_id FK
        string attribute_type "SERVER_SCOPE"
        string attribute_key "api_number, status, etc"
        long last_update_ts
        boolean bool_v
        string str_v
        long long_v
        double dbl_v
        string json_v
    }

    TB_TS_KV {
        uuid entity_id FK
        string key "temperature, pressure, etc"
        bigint ts
        boolean bool_v
        string str_v
        long long_v
        double dbl_v
        string json_v
    }

    TB_ALARM {
        uuid id PK
        uuid tenant_id FK
        uuid originator_id FK
        string type
        string severity "CRITICAL, MAJOR, etc"
        string status "ACTIVE, CLEARED, etc"
        bigint start_ts
        bigint end_ts
        bigint ack_ts
        bigint clear_ts
        jsonb details
    }

    TB_RELATION {
        uuid from_id FK
        string from_type
        uuid to_id FK
        string to_type
        string relation_type "Contains, BelongsTo"
        string relation_type_group
    }

    PF_OPTIMIZATION_RESULT {
        uuid id PK
        uuid tenant_id FK
        uuid well_id FK "Referencia a TB Asset"
        string optimization_type
        jsonb input_parameters
        jsonb output_parameters
        double improvement_percent
        bigint calculated_at
    }

    PF_RECOMMENDATION {
        uuid id PK
        uuid tenant_id FK
        uuid well_id FK "Referencia a TB Asset"
        string type
        double current_value
        double recommended_value
        double expected_benefit
        double confidence
        string priority
        string status
        bigint created_time
        bigint expires_at
    }
```

### 2.2 Asset Types del Módulo PF

```mermaid
graph TB
    subgraph "Asset Type: pf_wellpad"
        WP[pf_wellpad Asset]
        WP_ATTR["Attributes:
        - code (string)
        - latitude (double)
        - longitude (double)
        - capacity_wells (long)
        - current_well_count (long)
        - status (string)"]
    end

    subgraph "Asset Type: pf_well"
        WELL[pf_well Asset]
        WELL_ATTR["Attributes:
        - api_number (string)
        - lift_system_type (string)
        - measured_depth_ft (double)
        - status (string)
        - rv_well_id (string)"]
    end

    subgraph "Asset Type: pf_esp_system"
        ESP[pf_esp_system Asset]
        ESP_ATTR["Attributes:
        - pump_model (string)
        - stages (long)
        - rated_flow_bpd (double)
        - motor_hp (double)
        - motor_voltage (long)
        - frequency_hz (double)
        - max_motor_temp_f (double)"]
    end

    subgraph "Asset Type: pf_flow_station"
        FS[pf_flow_station Asset]
        FS_ATTR["Attributes:
        - code (string)
        - capacity_bpd (double)
        - latitude (double)
        - longitude (double)"]
    end

    WP --> |"Contains"| WELL
    WELL --> |"HasSystem"| ESP
    WP --> |"BelongsTo"| FS

    WP --- WP_ATTR
    WELL --- WELL_ATTR
    ESP --- ESP_ATTR
    FS --- FS_ATTR

    style WP fill:#E3F2FD
    style WELL fill:#E3F2FD
    style ESP fill:#E3F2FD
    style FS fill:#E3F2FD
```

### 2.3 Comparación: Arquitectura Vieja vs Nueva

```mermaid
graph LR
    subgraph "VIEJA - Custom Tables (Eliminada)"
        OLD_WELL[(pf.well table)]
        OLD_PAD[(pf.wellpad table)]
        OLD_TEL[(pf.telemetry<br/>TimescaleDB)]
        OLD_ALARM[(pf.alarm table)]
        OLD_LIMIT[(pf.operational_limit)]
    end

    subgraph "NUEVA - ThingsBoard Core"
        NEW_ASSET[(TB asset table<br/>type: pf_well, pf_wellpad)]
        NEW_ATTR[(TB attribute_kv<br/>SERVER_SCOPE)]
        NEW_TS[(TB ts_kv tables<br/>Native time-series)]
        NEW_ALARM[(TB alarm table<br/>Alarm System)]
        NEW_PROFILE[Asset Profiles<br/>Alarm Rules]
    end

    OLD_WELL -.->|migrated to| NEW_ASSET
    OLD_PAD -.->|migrated to| NEW_ASSET
    OLD_TEL -.->|migrated to| NEW_TS
    OLD_ALARM -.->|migrated to| NEW_ALARM
    OLD_LIMIT -.->|migrated to| NEW_PROFILE

    style OLD_WELL fill:#FFCDD2
    style OLD_PAD fill:#FFCDD2
    style OLD_TEL fill:#FFCDD2
    style OLD_ALARM fill:#FFCDD2
    style OLD_LIMIT fill:#FFCDD2
    style NEW_ASSET fill:#C8E6C9
    style NEW_ATTR fill:#C8E6C9
    style NEW_TS fill:#C8E6C9
    style NEW_ALARM fill:#C8E6C9
    style NEW_PROFILE fill:#C8E6C9
```

### 2.4 Entidades PO Module (Optimización)

```mermaid
erDiagram
    TB_ASSET ||--o{ PF_RECOMMENDATION : "has"
    TB_ASSET ||--o{ PF_OPTIMIZATION_RESULT : "has"
    TB_ASSET ||--|| TB_ATTRIBUTE_KV : "health_score stored as attribute"
    PF_RECOMMENDATION ||--o| PO_SETPOINT_CHANGE : "executes"

    TB_ASSET {
        uuid id PK
        string type "pf_well"
        string name
    }

    TB_ATTRIBUTE_KV {
        uuid entity_id FK
        string attribute_key "health_score, failure_probability, etc"
        long long_v "score value"
        string json_v "component_scores JSON"
    }

    PF_RECOMMENDATION {
        uuid id PK
        uuid tenant_id FK
        uuid well_id FK "TB Asset ID"
        string type
        double current_value
        double recommended_value
        double expected_benefit
        double confidence
        string priority
        string status
        bigint created_time
        bigint expires_at
    }

    PF_OPTIMIZATION_RESULT {
        uuid id PK
        uuid tenant_id FK
        uuid well_id FK "TB Asset ID"
        string optimization_type
        jsonb input_parameters
        jsonb output_parameters
        double improvement_percent
        bigint calculated_at
    }

    PO_SETPOINT_CHANGE {
        uuid id PK
        uuid recommendation_id FK
        double target_value
        double actual_value
        boolean success
        bigint executed_at
    }
```

---

## 3. Flujos de Datos {#flujos}

### 3.1 Flujo de Telemetría - ThingsBoard Rule Engine

```mermaid
sequenceDiagram
    participant Device as IoT Device<br/>(PLC/RTU)
    participant MQTT as TB MQTT Transport
    participant RuleEngine as Rule Engine
    participant QualityNode as PfDataQualityNode<br/>(Custom Rule Node)
    participant AlarmNode as PfAlarmEvaluationNode<br/>(Custom Rule Node)
    participant TsKv as ts_kv Tables
    participant AlarmSvc as TB Alarm Service
    participant WS as WebSocket<br/>(Subscriptions)
    participant UI as Frontend

    Device->>MQTT: Publish telemetry<br/>topic: v1/devices/{token}/telemetry
    MQTT->>RuleEngine: Post Telemetry Message

    RuleEngine->>QualityNode: Validate & enrich data
    QualityNode->>QualityNode: Calculate quality score<br/>Apply unit conversions

    alt Quality >= 0.7
        QualityNode->>TsKv: Save to ts_kv<br/>(TB native storage)
        QualityNode->>AlarmNode: Forward to alarm evaluation

        AlarmNode->>AlarmNode: Check against<br/>Asset Profile rules

        alt Limit violated
            AlarmNode->>AlarmSvc: Create/Update TB Alarm
            AlarmSvc->>WS: Push alarm notification
        end

        RuleEngine->>WS: Push telemetry update
        WS->>UI: Real-time data
    else Quality < 0.7
        QualityNode->>RuleEngine: Route to failure chain
        RuleEngine->>RuleEngine: Log to DLQ topic
    end
```

### 3.2 Flujo de Creación de Asset (Well)

```mermaid
sequenceDiagram
    participant User as Engineer
    participant UI as Frontend
    participant API as PfWellController
    participant WellSvc as PfWellService
    participant AssetSvc as PfAssetService<br/>(Wrapper)
    participant AttrSvc as PfAttributeService<br/>(Wrapper)
    participant TBAsset as TB AssetService
    participant TBAttr as TB AttributesService
    participant DB as PostgreSQL

    User->>UI: Create new well form
    UI->>API: POST /api/nexus/pf/wells
    API->>WellSvc: createWell(PfWellDto)

    WellSvc->>AssetSvc: createAsset(tenantId, "pf_well", name)
    AssetSvc->>TBAsset: save(Asset)
    TBAsset->>DB: INSERT INTO asset
    DB-->>TBAsset: Asset created
    TBAsset-->>AssetSvc: Asset with ID
    AssetSvc-->>WellSvc: Asset

    WellSvc->>WellSvc: Build attribute map from DTO
    WellSvc->>AttrSvc: saveServerAttributes(assetId, attributeMap)
    AttrSvc->>TBAttr: save(entityId, SERVER_SCOPE, attributes)
    TBAttr->>DB: INSERT INTO attribute_kv
    DB-->>TBAttr: Attributes saved

    WellSvc-->>API: PfWellDto with assetId
    API-->>UI: 201 Created + Well DTO
    UI-->>User: Show success
```

### 3.3 Flujo de Optimización ESP

```mermaid
sequenceDiagram
    participant Scheduler as Cron Scheduler
    participant Engine as Recommendation Engine
    participant EspOpt as ESP Optimizer
    participant AssetSvc as PfAssetService
    participant AttrSvc as PfAttributeService
    participant TsSvc as TB TelemetryService
    participant RV as RV Module<br/>(IPR/PVT)
    participant Simulator as Impact Simulator
    participant DB as PostgreSQL<br/>(pf_recommendation)
    participant UI as Frontend

    Scheduler->>Engine: Daily at 7 AM
    Engine->>AssetSvc: getAssetsByType("pf_well")
    AssetSvc-->>Engine: List of well assets

    loop For each well asset
        Engine->>EspOpt: optimizeFrequency(wellAssetId)
        EspOpt->>TsSvc: getTimeseries(assetId, keys, timeRange)
        TsSvc-->>EspOpt: Current telemetry from ts_kv
        EspOpt->>AttrSvc: getServerAttributes(assetId)
        AttrSvc-->>EspOpt: Well configuration attributes
        EspOpt->>RV: Get IPR & PVT
        RV-->>EspOpt: Reservoir data

        EspOpt->>EspOpt: Evaluate safety<br/>Calculate efficiency<br/>Identify opportunity

        alt Opportunity found
            EspOpt->>Simulator: Simulate frequency change
            Simulator-->>EspOpt: Impact estimation
            EspOpt-->>Engine: Optimization result
            Engine->>DB: INSERT INTO pf_recommendation
        else No action needed
            EspOpt-->>Engine: No recommendation
        end
    end

    Engine->>Engine: Prioritize by benefit
    Engine->>UI: Notify new recommendations
```

### 3.4 Flujo de Predicción de Fallas

```mermaid
sequenceDiagram
    participant Scheduler as Scheduler
    participant PredService as Prediction Service
    participant AssetSvc as PfAssetService
    participant TsSvc as TB TelemetryService
    participant MLService as ML Service<br/>(Python/Flask)
    participant Model as LSTM Model
    participant AttrSvc as PfAttributeService
    participant AlarmSvc as TB AlarmService

    Scheduler->>PredService: Every 6 hours
    PredService->>AssetSvc: getAssetsByType("pf_well")

    loop For each well
        PredService->>TsSvc: getTimeseries(wellId, 7 days)
        TsSvc-->>PredService: Hourly data (168 points)

        PredService->>MLService: POST /predict/esp-failure
        Note over PredService,MLService: {<br/>  wellId,<br/>  telemetry: [...]<br/>}

        MLService->>MLService: Feature engineering
        MLService->>Model: Predict
        Model-->>MLService: Failure probability

        alt Probability > 0.5
            MLService->>MLService: Estimate days to failure
            MLService-->>PredService: High risk result

            PredService->>AttrSvc: saveServerAttribute(wellId,<br/>"health_score", score)
            PredService->>AttrSvc: saveServerAttribute(wellId,<br/>"failure_probability", prob)

            PredService->>AlarmSvc: createAlarm(wellId,<br/>type: "ESP_FAILURE_PREDICTED",<br/>severity: WARNING)
        else Probability <= 0.5
            MLService-->>PredService: Low risk
            PredService->>AttrSvc: Update health_score attribute
        end
    end
```

### 3.5 Flujo de Ejecución de Recomendación

```mermaid
sequenceDiagram
    participant User as Engineer
    participant UI as Frontend
    participant API as PO API
    participant Approval as Approval Workflow
    participant Safety as Safety Interlock
    participant Controller as Setpoint Controller
    participant SCADA as SCADA System
    participant AttrSvc as PfAttributeService
    participant AlarmSvc as TB AlarmService

    User->>UI: Review recommendation
    User->>UI: Click "Approve & Execute"
    UI->>API: POST /recommendations/{id}/approve

    API->>Approval: Check approval authority
    Approval-->>API: User authorized

    API->>Safety: Check safety interlocks
    Safety-->>API: Change allowed

    API->>Controller: Execute recommendation
    Controller->>SCADA: Send setpoint command<br/>OPC-UA Write
    SCADA-->>Controller: Command acknowledged

    Controller->>Controller: Monitor for 30 seconds

    loop Wait 30 seconds
        Controller->>SCADA: Read actual value
        SCADA-->>Controller: Current value
    end

    alt Value reached target
        Controller-->>API: Setpoint changed successfully
        API->>AttrSvc: Update well attributes<br/>(new setpoint values)
        API->>AlarmSvc: Clear any related alarms
        API->>UI: Show success
    else Value didn't reach target
        Controller->>SCADA: Rollback to previous value
        Controller-->>API: Setpoint change failed
        API->>AlarmSvc: Create SETPOINT_CHANGE_FAILED alarm
        API->>UI: Show error + rollback
    end
```

---

## 4. Secuencias de Operación {#secuencias}

### 4.1 Creación de Pozo - ThingsBoard Assets (PlantUML)

```plantuml
@startuml
actor Engineer
participant "Frontend" as UI
participant "PfWellController" as Controller
participant "PfWellService" as Service
participant "PfAssetService" as AssetSvc
participant "PfAttributeService" as AttrSvc
participant "TB AssetService" as TBAsset
participant "TB AttributesService" as TBAttr
database "PostgreSQL" as DB

Engineer -> UI: Create new well
UI -> UI: Fill form
UI -> Controller: POST /api/nexus/pf/wells
activate Controller

Controller -> Controller: Validate PfWellDto
Controller -> Service: createWell(dto)
activate Service

Service -> Service: Validate business rules
Service -> AssetSvc: createAsset(tenantId, "pf_well", name)
activate AssetSvc

AssetSvc -> TBAsset: save(Asset)
TBAsset -> DB: INSERT INTO asset
DB --> TBAsset: Asset with UUID
TBAsset --> AssetSvc: Asset
AssetSvc --> Service: Asset
deactivate AssetSvc

note over Service: Build attribute map:\n- api_number\n- status\n- lift_system_type\n- measured_depth_ft

Service -> AttrSvc: saveServerAttributes(assetId, attrMap)
activate AttrSvc

AttrSvc -> TBAttr: save(entityId, SERVER_SCOPE, entries)
TBAttr -> DB: INSERT INTO attribute_kv
DB --> TBAttr: Attributes saved
TBAttr --> AttrSvc: Success
AttrSvc --> Service: Success
deactivate AttrSvc

Service --> Controller: PfWellDto with assetId
deactivate Service

Controller --> UI: 201 Created + Well DTO
deactivate Controller
UI --> Engineer: Show success message

@enduml
```

### 4.2 Ciclo de Optimización Diario (PlantUML)

```plantuml
@startuml
participant "Scheduler" as Cron
participant "RecommendationEngine" as Engine
participant "PfAssetService" as AssetSvc
participant "EspOptimizer" as Optimizer
participant "TB TelemetryService" as TsSvc
participant "ImpactSimulator" as Simulator
participant "pf_recommendation" as DB
participant "NotificationService" as Notif

== Daily Cycle at 7:00 AM ==

Cron -> Engine: @Scheduled trigger
activate Engine

Engine -> AssetSvc: getAssetsByType("pf_well")
AssetSvc --> Engine: List<Asset> wells

loop For each well asset
    Engine -> Optimizer: optimizeFrequency(wellAssetId)
    activate Optimizer

    Optimizer -> TsSvc: getTimeseries(assetId, keys, range)
    TsSvc --> Optimizer: Current telemetry from ts_kv

    Optimizer -> Optimizer: 1. Evaluate safety
    Optimizer -> Optimizer: 2. Calculate efficiency
    Optimizer -> Optimizer: 3. Identify opportunity

    alt Opportunity found
        Optimizer -> Simulator: simulateImpact()
        activate Simulator
        Simulator --> Optimizer: ImpactResult
        deactivate Simulator

        Optimizer --> Engine: OptimizationResult
    else No opportunity
        Optimizer --> Engine: null
    end
    deactivate Optimizer
end

Engine -> Engine: Prioritize by benefit
Engine -> DB: Save top 50 recommendations
Engine -> Notif: Notify engineers
deactivate Engine

@enduml
```

### 4.3 Procesamiento de Alarmas - ThingsBoard Alarm System (PlantUML)

```plantuml
@startuml
participant "Rule Engine" as RE
participant "PfAlarmEvaluationNode" as AlarmNode
participant "Asset Profile" as Profile
participant "TB AlarmService" as AlarmSvc
participant "WebSocket" as WS
participant "Frontend" as UI

RE -> AlarmNode: TbMsg with telemetry
activate AlarmNode

AlarmNode -> Profile: Get alarm rules for asset type
Profile --> AlarmNode: List<AlarmRule>

loop For each alarm rule
    AlarmNode -> AlarmNode: Evaluate condition\n(e.g., motor_temp > 300)

    alt Condition violated
        AlarmNode -> AlarmSvc: createOrUpdateAlarm(\n  originatorId: assetId,\n  type: "HIGH_MOTOR_TEMP",\n  severity: MAJOR)
        AlarmSvc --> AlarmNode: TB Alarm created

        AlarmNode -> WS: Push alarm notification
        WS -> UI: Real-time alarm update
    else Condition cleared
        AlarmNode -> AlarmSvc: clearAlarm(assetId, type)
        AlarmSvc --> AlarmNode: Alarm cleared
    end
end

deactivate AlarmNode

@enduml
```

---

## 5. Despliegue {#despliegue}

### 5.1 Arquitectura de Despliegue (Kubernetes)

```mermaid
graph TB
    subgraph "Kubernetes Cluster"
        subgraph "Ingress"
            LB[Load Balancer<br/>NGINX Ingress]
        end

        subgraph "Application Pods"
            TB1[ThingsBoard Node 1<br/>PF + PO + CT + RV Modules]
            TB2[ThingsBoard Node 2<br/>PF + PO + CT + RV Modules]
            TB3[ThingsBoard Node 3<br/>PF + PO + CT + RV Modules]
            ML1[ML Service Pod<br/>Python/Flask]
            ML2[ML Service Pod<br/>Python/Flask]
        end

        subgraph "ThingsBoard Data Services"
            PG[(PostgreSQL<br/>asset, attribute_kv<br/>alarm, relation<br/>+ pf_recommendation)]
            PGS[(PostgreSQL<br/>Standby)]
            TS[(ts_kv partitions<br/>Time-series data)]
            Redis1[(Redis Master<br/>Cache + Sessions)]
            Redis2[(Redis Replica)]
            Kafka1[Kafka Broker 1]
            Kafka2[Kafka Broker 2]
            Kafka3[Kafka Broker 3]
        end

        subgraph "Monitoring"
            Prom[Prometheus]
            Graf[Grafana]
        end
    end

    subgraph "External Systems"
        SCADA[SCADA/DCS]
        Devices[IoT Devices]
    end

    LB --> TB1
    LB --> TB2
    LB --> TB3

    TB1 --> ML1
    TB2 --> ML2
    TB3 --> ML1

    TB1 --> PG
    TB2 --> PG
    TB3 --> PG
    PG -.-> PGS

    TB1 --> TS
    TB2 --> TS
    TB3 --> TS

    TB1 --> Redis1
    TB2 --> Redis1
    TB3 --> Redis1
    Redis1 -.-> Redis2

    TB1 --> Kafka1
    TB2 --> Kafka2
    TB3 --> Kafka3

    Prom --> TB1
    Prom --> TB2
    Prom --> TB3
    Graf --> Prom

    Devices --> SCADA
    SCADA --> LB

    style TB1 fill:#4A90E2
    style TB2 fill:#4A90E2
    style TB3 fill:#4A90E2
    style ML1 fill:#7ED321
    style ML2 fill:#7ED321
```

### 5.2 Topología de Red

```mermaid
graph TB
    subgraph "DMZ"
        LB[Load Balancer]
        Firewall[Firewall]
    end

    subgraph "Application Tier - Private Subnet"
        App1[ThingsBoard Node 1<br/>10.0.1.10]
        App2[ThingsBoard Node 2<br/>10.0.1.11]
        App3[ThingsBoard Node 3<br/>10.0.1.12]
    end

    subgraph "Data Tier - Private Subnet"
        DB1[(PostgreSQL Master<br/>TB Core Tables<br/>10.0.2.10)]
        DB2[(PostgreSQL Standby<br/>10.0.2.11)]
        Cache[(Redis<br/>10.0.2.20)]
        MQ1[Kafka 1<br/>10.0.2.30]
        MQ2[Kafka 2<br/>10.0.2.31]
    end

    subgraph "Field Network"
        SCADA[SCADA Server<br/>192.168.100.10]
        RTU1[RTU Wellpad 1<br/>192.168.100.20]
        RTU2[RTU Wellpad 2<br/>192.168.100.21]
    end

    Internet((Internet)) --> LB
    LB --> Firewall
    Firewall --> App1
    Firewall --> App2
    Firewall --> App3

    App1 --> DB1
    App2 --> DB1
    App3 --> DB1
    DB1 -.Replication.-> DB2

    App1 --> Cache
    App2 --> Cache
    App3 --> Cache

    App1 --> MQ1
    App2 --> MQ2
    App3 --> MQ1

    SCADA --> Firewall
    RTU1 --> SCADA
    RTU2 --> SCADA
```

---

## 6. Diagramas de Estado

### 6.1 Estados de Recomendación

```mermaid
stateDiagram-v2
    [*] --> PENDING: Created

    PENDING --> PENDING_APPROVAL: Submit for approval
    PENDING --> REJECTED: Reject
    PENDING --> EXPIRED: Timeout (7 days)

    PENDING_APPROVAL --> APPROVED: Approve
    PENDING_APPROVAL --> REJECTED: Reject

    APPROVED --> EXECUTING: Execute

    EXECUTING --> COMPLETED: Success
    EXECUTING --> FAILED: Error
    EXECUTING --> ROLLED_BACK: Rollback

    FAILED --> PENDING: Retry

    COMPLETED --> [*]
    REJECTED --> [*]
    EXPIRED --> [*]
    ROLLED_BACK --> [*]
```

### 6.2 Estados de Alarma - ThingsBoard Alarm System

```mermaid
stateDiagram-v2
    [*] --> ACTIVE_UNACK: Condition violated<br/>(TB creates alarm)

    ACTIVE_UNACK --> ACTIVE_ACK: User acknowledges<br/>(via TB API)
    ACTIVE_UNACK --> CLEARED_UNACK: Condition resolved<br/>(Rule Node clears)

    ACTIVE_ACK --> CLEARED_ACK: Condition resolved

    CLEARED_UNACK --> CLEARED_ACK: User acknowledges

    CLEARED_ACK --> [*]: Alarm lifecycle complete

    note right of ACTIVE_UNACK
        TB Alarm Status: ACTIVE
        TB Alarm Ack: false
    end note

    note right of ACTIVE_ACK
        TB Alarm Status: ACTIVE
        TB Alarm Ack: true
    end note

    note right of CLEARED_ACK
        TB Alarm Status: CLEARED
        TB Alarm Ack: true
    end note
```

### 6.3 Ciclo de Vida de Asset

```mermaid
stateDiagram-v2
    [*] --> PLANNED: Asset created<br/>in planning phase

    PLANNED --> INSTALLING: Installation starts
    PLANNED --> CANCELLED: Project cancelled

    INSTALLING --> ACTIVE: Commissioning complete
    INSTALLING --> CANCELLED: Installation cancelled

    ACTIVE --> MAINTENANCE: Maintenance required
    ACTIVE --> SHUTDOWN: Temporary shutdown
    ACTIVE --> DECOMMISSIONED: End of life

    MAINTENANCE --> ACTIVE: Maintenance complete
    MAINTENANCE --> DECOMMISSIONED: Cannot repair

    SHUTDOWN --> ACTIVE: Restart
    SHUTDOWN --> DECOMMISSIONED: Permanent shutdown

    CANCELLED --> [*]
    DECOMMISSIONED --> [*]

    note right of ACTIVE
        Status stored as
        SERVER_SCOPE attribute
        in attribute_kv table
    end note
```

---

## 7. Componentes Frontend

### 7.1 Arquitectura de Componentes Angular

```mermaid
graph TB
    subgraph "PF Module Components"
        WellList[Well List Component]
        WellDetail[Well Detail Component]
        WellpadDash[Wellpad Dashboard]
        FlowStation[Flow Station Dashboard]
        AlarmList[TB Alarm Widget]

        WellList --> WellDetail
        WellpadDash --> WellDetail
    end

    subgraph "PO Module Components"
        OptDash[Optimization Dashboard]
        RecList[Recommendation List]
        RecDetail[Recommendation Detail]
        KpiDash[KPI Dashboard]
        HealthDash[Health Score Dashboard]

        OptDash --> RecList
        RecList --> RecDetail
    end

    subgraph "TB Core Services (Angular)"
        AssetSvc[AssetService<br/>TB Angular SDK]
        AttrSvc[AttributeService<br/>TB Angular SDK]
        AlarmSvc[AlarmService<br/>TB Angular SDK]
        TeleSvc[TelemetryService<br/>TB Angular SDK]
    end

    subgraph "PF/PO Custom Services"
        WellSvc[PfWellService]
        RecSvc[PoRecommendationService]
        KpiSvc[PoKpiService]
    end

    WellList --> WellSvc
    WellDetail --> WellSvc
    WellDetail --> TeleSvc
    AlarmList --> AlarmSvc

    RecList --> RecSvc
    KpiDash --> KpiSvc

    WellSvc --> AssetSvc
    WellSvc --> AttrSvc

    style WellDetail fill:#E3F2FD
    style RecDetail fill:#E8F5E9
```

### 7.2 Integración con ThingsBoard Widgets

```mermaid
graph TB
    subgraph "ThingsBoard Dashboard"
        DB[Dashboard]

        subgraph "Standard TB Widgets"
            AlarmTable[Alarm Table Widget]
            TsChart[Time-series Chart]
            EntityTable[Entity Table Widget]
            Map[Map Widget]
        end

        subgraph "Custom PF Widgets"
            WellStatus[Well Status Widget]
            EspDiagram[ESP Diagram Widget]
            ProductionGauge[Production Gauge]
        end

        subgraph "Custom PO Widgets"
            RecWidget[Recommendations Widget]
            HealthWidget[Health Score Widget]
            KpiWidget[KPI Summary Widget]
        end
    end

    subgraph "Data Sources"
        Assets[(TB Assets<br/>pf_well, pf_wellpad)]
        Attrs[(TB Attributes<br/>SERVER_SCOPE)]
        TsKv[(ts_kv<br/>Telemetry)]
        Alarms[(TB Alarms)]
        CustomTbl[(pf_recommendation<br/>pf_optimization_result)]
    end

    AlarmTable --> Alarms
    TsChart --> TsKv
    EntityTable --> Assets
    Map --> Assets

    WellStatus --> Attrs
    EspDiagram --> TsKv
    ProductionGauge --> TsKv

    RecWidget --> CustomTbl
    HealthWidget --> Attrs
    KpiWidget --> TsKv

    style AlarmTable fill:#FFF9C4
    style TsChart fill:#FFF9C4
    style EntityTable fill:#FFF9C4
```

---

## 8. Diagrama de Decisión Arquitectónica

### 8.1 ¿Cuándo usar Tabla Custom vs ThingsBoard Core?

```mermaid
flowchart TD
    START[Nueva entidad/dato] --> Q1{¿Es un objeto<br/>del dominio físico?}

    Q1 -->|Sí: Pozo, Equipo,<br/>Instalación| TB_ASSET[Usar TB Asset<br/>+ Attributes]

    Q1 -->|No| Q2{¿Es telemetría<br/>time-series?}

    Q2 -->|Sí: Temperatura,<br/>Presión, Flujo| TB_TS[Usar ts_kv<br/>TB Native]

    Q2 -->|No| Q3{¿Es una alarma<br/>o alerta?}

    Q3 -->|Sí: Límite excedido,<br/>Falla detectada| TB_ALARM[Usar TB Alarm<br/>System]

    Q3 -->|No| Q4{¿Tiene ciclo de vida<br/>complejo con workflow?}

    Q4 -->|Sí: Aprobaciones,<br/>Estados múltiples| CUSTOM[Tabla Custom<br/>Justificada]

    Q4 -->|No| Q5{¿Requiere queries<br/>analíticos complejos?}

    Q5 -->|Sí: Agregaciones,<br/>JOINs complejos| CUSTOM

    Q5 -->|No| TB_ASSET

    TB_ASSET --> EXAMPLES_ASSET[Ejemplos:<br/>pf_well, pf_esp_system<br/>ct_unit, rv_basin]

    TB_TS --> EXAMPLES_TS[Ejemplos:<br/>motor_temp, wellhead_pressure<br/>flow_rate_bpd]

    TB_ALARM --> EXAMPLES_ALARM[Ejemplos:<br/>HIGH_MOTOR_TEMP<br/>LOW_PRODUCTION<br/>ESP_FAILURE_PREDICTED]

    CUSTOM --> EXAMPLES_CUSTOM[Ejemplos:<br/>pf_recommendation<br/>pf_optimization_result<br/>ct_job, ct_fatigue_log]

    style TB_ASSET fill:#C8E6C9
    style TB_TS fill:#C8E6C9
    style TB_ALARM fill:#C8E6C9
    style CUSTOM fill:#FFECB3
```

---

## Uso de los Diagramas

### Renderizar Diagramas Mermaid
Los diagramas Mermaid se renderizan automáticamente en:
- GitHub
- GitLab
- VS Code (con extensión Mermaid)
- Confluence (con plugin)

### Renderizar Diagramas PlantUML
Para renderizar PlantUML:
```bash
# Instalar PlantUML
brew install plantuml

# Generar imágenes
plantuml DIAGRAMS.md
```

O usar servicios online:
- https://www.plantuml.com/plantuml/
- https://plantuml-editor.kkeisuke.com/

---

**Documento actualizado a arquitectura ThingsBoard Core con 20 diagramas visuales**

