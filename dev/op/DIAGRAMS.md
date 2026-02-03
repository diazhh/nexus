# DIAGRAMAS DE ARQUITECTURA - PF & PO Modules

**Proyecto**: Nexus Production Facilities & Optimization
**Versión**: 1.0
**Fecha**: 2026-02-03
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

### 1.1 Arquitectura de Capas (Mermaid)

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
    
    subgraph "Service Layer"
        WellSvc[Well Service]
        TeleSvc[Telemetry Service]
        AlarmSvc[Alarm Service]
        OptSvc[Optimization Service]
        MLSvc[ML Service]
    end
    
    subgraph "Data Layer"
        PG[(PostgreSQL)]
        TS[(TimescaleDB)]
        Redis[(Redis Cache)]
        Kafka[Kafka Streams]
    end
    
    subgraph "Integration Layer"
        SCADA[SCADA/DCS]
        Historian[Historian]
        ERP[ERP System]
    end
    
    UI --> PF
    UI --> PO
    Mobile --> PF
    
    PF --> WellSvc
    PF --> TeleSvc
    PF --> AlarmSvc
    PO --> OptSvc
    PO --> MLSvc
    PO --> TeleSvc
    
    RV --> PF
    PF --> PO
    
    WellSvc --> PG
    TeleSvc --> TS
    TeleSvc --> Kafka
    AlarmSvc --> PG
    OptSvc --> PG
    MLSvc --> Redis
    
    TeleSvc --> SCADA
    TeleSvc --> Historian
    OptSvc --> ERP
    
    style PF fill:#4A90E2
    style PO fill:#7ED321
    style RV fill:#F5A623
```

### 1.2 Arquitectura de Módulos PF y PO

```mermaid
graph LR
    subgraph "RV Module"
        IPR[IPR Curves]
        PVT[PVT Properties]
        Decline[Decline Curves]
    end
    
    subgraph "PF Module - Production Facilities"
        Wells[Wells Management]
        Wellpads[Wellpads]
        FlowStations[Flow Stations]
        Telemetry[Telemetry Processing]
        Alarms[Alarm System]
        SCADA_Int[SCADA Integration]
    end
    
    subgraph "PO Module - Production Optimization"
        EspOpt[ESP Optimizer]
        GasOpt[Gas Lift Optimizer]
        DilOpt[Diluent Optimizer]
        MLModels[ML Models]
        HealthScore[Health Score]
        Recommendations[Recommendation Engine]
    end
    
    IPR --> EspOpt
    PVT --> EspOpt
    Decline --> HealthScore
    
    Wells --> Telemetry
    Telemetry --> Alarms
    Telemetry --> EspOpt
    Telemetry --> MLModels
    Telemetry --> HealthScore
    
    EspOpt --> Recommendations
    GasOpt --> Recommendations
    DilOpt --> Recommendations
    MLModels --> Recommendations
    
    Recommendations --> SCADA_Int
    
    style PF Module fill:#E3F2FD
    style PO Module fill:#E8F5E9
    style RV Module fill:#FFF3E0
```

---

## 2. Modelo de Datos (ERD) {#erd}

### 2.1 Entidades Principales PF Module

```mermaid
erDiagram
    PF_WELL ||--o{ PF_TELEMETRY : "generates"
    PF_WELL ||--o{ PF_ALARM : "has"
    PF_WELL }o--|| PF_WELLPAD : "belongs to"
    PF_WELL ||--o| PF_ESP_SYSTEM : "has"
    PF_WELL ||--o| PF_PCP_SYSTEM : "has"
    PF_WELL ||--o| PF_GAS_LIFT_SYSTEM : "has"
    PF_WELL }o--|| RV_WELL : "references"
    PF_WELLPAD }o--|| PF_FLOW_STATION : "connected to"
    
    PF_WELL {
        uuid id PK
        uuid tenant_id
        string name
        string api_number UK
        uuid wellpad_id FK
        uuid rv_well_id FK
        enum status
        enum lift_system_type
        double latitude
        double longitude
        double measured_depth_ft
        bigint created_time
    }
    
    PF_WELLPAD {
        uuid id PK
        uuid tenant_id
        string name
        string code UK
        uuid flow_station_id FK
        double latitude
        double longitude
        int capacity_wells
        int current_well_count
    }
    
    PF_ESP_SYSTEM {
        uuid id PK
        uuid well_id FK
        string pump_model
        int stages
        double rated_flow_bpd
        double motor_hp
        int motor_voltage
        double frequency_hz
        double max_motor_temp_f
    }
    
    PF_TELEMETRY {
        timestamptz time PK
        uuid entity_id PK
        string key PK
        double value_numeric
        string value_string
        double quality_score
    }
    
    PF_ALARM {
        uuid id PK
        uuid tenant_id
        uuid entity_id FK
        enum severity
        enum status
        string message
        bigint start_time
        bigint end_time
    }
```

### 2.2 Entidades Principales PO Module

```mermaid
erDiagram
    PF_WELL ||--o{ PO_RECOMMENDATION : "has"
    PF_WELL ||--|| PO_HEALTH_SCORE : "has"
    PF_WELL ||--o{ PO_KPI : "has"
    PO_RECOMMENDATION ||--o| PO_SETPOINT_CHANGE : "executes"
    
    PO_RECOMMENDATION {
        uuid id PK
        uuid well_id FK
        string type
        double current_value
        double recommended_value
        double expected_benefit
        double confidence
        enum priority
        enum status
        bigint created_time
        bigint expires_at
    }
    
    PO_HEALTH_SCORE {
        uuid id PK
        uuid well_id FK
        int score
        enum trend
        jsonb component_scores
        double failure_probability
        int estimated_days_to_failure
        bigint calculated_at
    }
    
    PO_KPI {
        uuid id PK
        uuid entity_id FK
        date date
        double production_bpd
        double efficiency_percent
        double uptime_percent
        double deferment_bpd
        double lifting_cost_per_bbl
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

### 3.1 Flujo de Telemetría (MQTT → Storage)

```mermaid
sequenceDiagram
    participant Device as IoT Device<br/>(PLC/RTU)
    participant MQTT as MQTT Broker
    participant App as PF Module
    participant Validator as Data Validator
    participant Kafka as Kafka Topic
    participant TS as TimescaleDB
    participant Alarm as Alarm Service
    participant WS as WebSocket
    participant UI as Frontend
    
    Device->>MQTT: Publish telemetry<br/>topic: v1/devices/{id}/telemetry
    MQTT->>App: Subscribe & receive
    App->>Validator: Validate data quality
    Validator-->>App: Quality score: 0.98
    
    alt Quality >= 0.7
        App->>Kafka: Publish to pf.telemetry.validated
        App->>TS: Batch insert (100 records)
        TS-->>App: Stored
        App->>Alarm: Evaluate against limits
        
        alt Limit violated
            Alarm->>Alarm: Create/update alarm
            Alarm->>WS: Push alarm notification
        end
        
        App->>WS: Push telemetry update
        WS->>UI: Real-time data
    else Quality < 0.7
        App->>Kafka: Send to DLQ<br/>(pf.telemetry.dlq)
    end
```

### 3.2 Flujo de Optimización ESP

```mermaid
sequenceDiagram
    participant Scheduler as Cron Scheduler
    participant Engine as Recommendation Engine
    participant EspOpt as ESP Optimizer
    participant PF as PF Module<br/>(Telemetry)
    participant RV as RV Module<br/>(IPR/PVT)
    participant Simulator as Impact Simulator
    participant DB as Database
    participant UI as Frontend
    
    Scheduler->>Engine: Daily at 7 AM
    Engine->>Engine: Get all active ESP wells
    
    loop For each well
        Engine->>EspOpt: optimizeFrequency(wellId)
        EspOpt->>PF: Get current telemetry
        PF-->>EspOpt: Current state
        EspOpt->>RV: Get IPR & PVT
        RV-->>EspOpt: Reservoir data
        
        EspOpt->>EspOpt: Evaluate safety<br/>Calculate efficiency<br/>Identify opportunity
        
        alt Opportunity found
            EspOpt->>Simulator: Simulate frequency change
            Simulator-->>EspOpt: Impact estimation
            EspOpt-->>Engine: Optimization result
            Engine->>DB: Save recommendation
        else No action needed
            EspOpt-->>Engine: No recommendation
        end
    end
    
    Engine->>Engine: Prioritize by benefit
    Engine->>UI: Notify new recommendations
```

### 3.3 Flujo de Predicción de Fallas

```mermaid
sequenceDiagram
    participant Scheduler as Scheduler
    participant PredService as Prediction Service
    participant PF as PF Module
    participant MLService as ML Service<br/>(Python/Flask)
    participant Model as LSTM Model
    participant DB as Database
    participant Notification as Notification Service
    
    Scheduler->>PredService: Every 6 hours
    PredService->>PredService: Get wells to evaluate
    
    loop For each well
        PredService->>PF: Get last 7 days telemetry
        PF-->>PredService: Hourly data (168 points)
        
        PredService->>MLService: POST /predict/esp-failure
        Note over PredService,MLService: {<br/>  wellId,<br/>  telemetry: [...]<br/>}
        
        MLService->>MLService: Feature engineering
        MLService->>Model: Predict
        Model-->>MLService: Failure probability
        
        alt Probability > 0.5
            MLService->>MLService: Estimate days to failure
            MLService-->>PredService: High risk result
            PredService->>DB: Update health score
            PredService->>Notification: Send alert
            Notification->>Notification: Email supervisor
        else Probability <= 0.5
            MLService-->>PredService: Low risk
            PredService->>DB: Update health score
        end
    end
```

### 3.4 Flujo de Ejecución de Recomendación

```mermaid
sequenceDiagram
    participant User as Engineer
    participant UI as Frontend
    participant API as PO API
    participant Approval as Approval Workflow
    participant Safety as Safety Interlock
    participant Controller as Setpoint Controller
    participant SCADA as SCADA System
    participant Monitor as Monitor Service
    
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
    
    Controller->>Monitor: Start monitoring (30s)
    
    loop Wait 30 seconds
        Monitor->>SCADA: Read actual value
        SCADA-->>Monitor: Current value
    end
    
    alt Value reached target
        Monitor-->>Controller: Success
        Controller->>API: Setpoint changed successfully
        API->>UI: Show success
    else Value didn't reach target
        Monitor-->>Controller: Failed
        Controller->>SCADA: Rollback to previous value
        Controller->>API: Setpoint change failed
        API->>UI: Show error + rollback
    end
```

---

## 4. Secuencias de Operación {#secuencias}

### 4.1 Creación de Pozo (PlantUML)

```plantuml
@startuml
actor Engineer
participant "Frontend" as UI
participant "PfWellController" as Controller
participant "PfWellService" as Service
participant "PfWellDao" as DAO
database "PostgreSQL" as DB

Engineer -> UI: Create new well
UI -> UI: Fill form
UI -> Controller: POST /api/nexus/pf/wells
activate Controller

Controller -> Controller: Validate DTO
Controller -> Service: createWell(well)
activate Service

Service -> Service: Validate business rules
Service -> Service: Check duplicates
Service -> DAO: save(well)
activate DAO

DAO -> DB: INSERT INTO pf.well
DB --> DAO: Well created
DAO --> Service: PfWell entity
deactivate DAO

Service -> Service: Create in ThingsBoard<br/>as Asset
Service --> Controller: PfWell
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
participant "EspOptimizer" as Optimizer
participant "ImpactSimulator" as Simulator
participant "Database" as DB
participant "NotificationService" as Notif

== Daily Cycle at 7:00 AM ==

Cron -> Engine: @Scheduled trigger
activate Engine

Engine -> Engine: Get all active wells
loop For each well
    Engine -> Optimizer: optimizeFrequency(wellId)
    activate Optimizer
    
    Optimizer -> Optimizer: 1. Get current state
    Optimizer -> Optimizer: 2. Evaluate safety
    Optimizer -> Optimizer: 3. Calculate efficiency
    Optimizer -> Optimizer: 4. Identify opportunity
    
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
            TB1[ThingsBoard Node 1<br/>PF + PO Modules]
            TB2[ThingsBoard Node 2<br/>PF + PO Modules]
            TB3[ThingsBoard Node 3<br/>PF + PO Modules]
            ML1[ML Service Pod<br/>Python/Flask]
            ML2[ML Service Pod<br/>Python/Flask]
        end
        
        subgraph "Data Services"
            PG[(PostgreSQL<br/>Primary)]
            PGS[(PostgreSQL<br/>Standby)]
            TS[(TimescaleDB<br/>Time-series)]
            Redis1[(Redis Master)]
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
        App1[App Server 1<br/>10.0.1.10]
        App2[App Server 2<br/>10.0.1.11]
        App3[App Server 3<br/>10.0.1.12]
    end
    
    subgraph "Data Tier - Private Subnet"
        DB1[(Database Master<br/>10.0.2.10)]
        DB2[(Database Standby<br/>10.0.2.11)]
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

### 6.2 Estados de Alarma

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: Limit violated
    
    ACTIVE --> ACKNOWLEDGED: User acknowledges
    ACTIVE --> CLEARED: Condition resolved
    
    ACKNOWLEDGED --> CLEARED: Condition resolved
    ACKNOWLEDGED --> ESCALATED: No action (30 min)
    
    ESCALATED --> CLEARED: Condition resolved
    
    CLEARED --> [*]
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
        AlarmList[Alarm List Component]
        
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
    
    subgraph "Shared Services"
        WellSvc[PfWellService]
        TeleSvc[PfTelemetryService]
        AlarmSvc[PfAlarmService]
        RecSvc[PoRecommendationService]
        KpiSvc[PoKpiService]
    end
    
    subgraph "State Management (NgRx)"
        Store[Store]
        Actions[Actions]
        Reducers[Reducers]
        Effects[Effects]
    end
    
    WellList --> WellSvc
    WellDetail --> WellSvc
    WellDetail --> TeleSvc
    AlarmList --> AlarmSvc
    
    RecList --> RecSvc
    KpiDash --> KpiSvc
    
    WellSvc --> Effects
    TeleSvc --> Effects
    Effects --> Store
    Store --> Reducers
    
    style WellDetail fill:#E3F2FD
    style RecDetail fill:#E8F5E9
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

**Documento completado con 14 diagramas visuales**

