# PF MODULE SPECIFICATION - Production Facilities

**Módulo**: Production Facilities (PF)
**Versión**: 1.0
**Fecha**: 2026-02-03
**Propósito**: Monitoreo en tiempo real de infraestructura de producción de superficie

---

## 📋 Tabla de Contenidos

1. [Overview](#overview)
2. [Arquitectura del Módulo](#arquitectura)
3. [Modelo de Datos](#modelo-de-datos)
4. [Servicios](#servicios)
5. [APIs REST](#apis-rest)
6. [Procesamiento de Telemetría](#telemetria)
7. [Sistema de Alarmas](#alarmas)
8. [Frontend Components](#frontend)

---

## 1. Overview {#overview}

### 1.1 Propósito

El módulo PF proporciona monitoreo en tiempo real de:
- Pozos productores
- Macollas/Wellpads
- Estaciones de flujo
- Sistemas de levantamiento artificial (ESP, PCP, Gas Lift, Rod Pump)
- Telemetría SCADA
- Alarmas y eventos operacionales

### 1.2 Responsabilidades

✅ **Lo que hace el módulo PF**:
- Capturar telemetría desde devices de campo vía MQTT/OPC-UA/Modbus
- Validar calidad de datos
- Almacenar series temporales en TimescaleDB
- Detectar alarmas por límites y rate of change
- Proporcionar dashboards de monitoreo
- Gestionar configuración de activos de producción

❌ **Lo que NO hace**:
- Optimización (eso es PO Module)
- Predicción de fallas con ML (eso es PO Module)
- Caracterización de yacimientos (eso es RV Module)

---

## 2. Arquitectura del Módulo {#arquitectura}

### 2.1 Estructura de Paquetes

```
org.thingsboard.server
├── common
│   └── data
│       └── pf
│           ├── entity
│           │   ├── PfWell.java
│           │   ├── PfWellpad.java
│           │   ├── PfFlowStation.java
│           │   ├── PfSeparator.java
│           │   ├── PfTank.java
│           │   ├── PfPipeline.java
│           │   └── liftsystem
│           │       ├── PfEspSystem.java
│           │       ├── PfPcpSystem.java
│           │       ├── PfGasLiftSystem.java
│           │       └── PfRodPumpSystem.java
│           ├── dto
│           │   ├── PfWellDto.java
│           │   ├── PfWellpadDto.java
│           │   └── ...
│           ├── telemetry
│           │   ├── TelemetryData.java
│           │   ├── TelemetryKey.java
│           │   └── DataQuality.java
│           └── alarm
│               ├── PfAlarm.java
│               ├── AlarmSeverity.java
│               └── AlarmType.java
│
├── dao
│   └── pf
│       ├── PfWellDao.java
│       ├── PfWellpadDao.java
│       ├── PfTelemetryDao.java
│       └── PfAlarmDao.java
│
└── service
    └── pf
        ├── asset
        │   ├── PfWellService.java
        │   ├── PfWellServiceImpl.java
        │   ├── PfWellpadService.java
        │   └── ...
        ├── telemetry
        │   ├── TelemetryProcessor.java
        │   ├── DataQualityValidator.java
        │   ├── TelemetryAggregator.java
        │   └── TelemetryQueryService.java
        ├── integration
        │   ├── ScadaIntegrationService.java
        │   ├── MqttConnector.java
        │   ├── OpcUaConnector.java
        │   └── ModbusConnector.java
        └── alarm
            ├── AlarmService.java
            ├── AlarmEvaluator.java
            ├── AlarmClassifier.java
            └── NotificationService.java
```

### 2.2 Dependencias Maven

```xml
<!-- PF Module Dependencies -->
<dependencies>
    <!-- ThingsBoard Core -->
    <dependency>
        <groupId>org.thingsboard</groupId>
        <artifactId>common</artifactId>
    </dependency>

    <!-- MQTT Client -->
    <dependency>
        <groupId>org.eclipse.paho</groupId>
        <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
        <version>1.2.5</version>
    </dependency>

    <!-- OPC-UA Client -->
    <dependency>
        <groupId>org.eclipse.milo</groupId>
        <artifactId>sdk-client</artifactId>
        <version>0.6.11</version>
    </dependency>

    <!-- Modbus -->
    <dependency>
        <groupId>com.infiniteautomation</groupId>
        <artifactId>modbus4j</artifactId>
        <version>3.0.6</version>
    </dependency>

    <!-- TimescaleDB -->
    <dependency>
        <groupId>com.timescale</groupId>
        <artifactId>timescaledb-jdbc</artifactId>
        <version>2.11.0</version>
    </dependency>
</dependencies>
```

---

## 3. Modelo de Datos {#modelo-de-datos}

### 3.1 Entidades Principales

#### PfWell (Pozo Productor)

```java
@Entity
@Table(name = "well", schema = "pf")
@Data
@EqualsAndHashCode(callSuper = true)
public class PfWell extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Well name cannot be blank")
    private String name;

    @Column(name = "api_number", unique = true, length = 50)
    private String apiNumber; // API Well Number (identificador único en industria)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wellpad_id")
    private PfWellpad wellpad;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rv_well_id")
    private RvWell rvWell; // Referencia a módulo RV

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WellStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "lift_system_type")
    private LiftSystemType liftSystemType;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "measured_depth_ft")
    private Double measuredDepthFt;

    @Column(name = "true_vertical_depth_ft")
    private Double trueVerticalDepthFt;

    @Column(name = "spud_date")
    private LocalDate spudDate;

    @Column(name = "first_production_date")
    private LocalDate firstProductionDate;

    @Column(name = "current_production_bpd")
    private Double currentProductionBpd;

    @Column(name = "additional_info", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    private JsonNode additionalInfo;

    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    @Column(name = "updated_time")
    private Long updatedTime;

    // Getters, Setters, Builder
}

public enum WellStatus {
    PRODUCING,        // En producción normal
    SHUT_IN,          // Cerrado temporalmente
    UNDER_WORKOVER,   // En mantenimiento
    ABANDONED,        // Abandonado
    SUSPENDED,        // Suspendido
    INACTIVE          // Inactivo
}

public enum LiftSystemType {
    ESP,              // Electric Submersible Pump
    PCP,              // Progressing Cavity Pump
    GAS_LIFT,         // Gas Lift
    ROD_PUMP,         // Bombeo Mecánico
    JET_PUMP,         // Jet Pump
    PLUNGER_LIFT,     // Plunger Lift
    NATURAL_FLOW,     // Flujo natural
    DILUENT_INJECTION // Inyección de diluentes
}
```

#### PfWellpad (Macolla/Cluster)

```java
@Entity
@Table(name = "wellpad", schema = "pf")
@Data
@EqualsAndHashCode(callSuper = true)
public class PfWellpad extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", unique = true)
    private String code;

    @OneToMany(mappedBy = "wellpad", cascade = CascadeType.ALL)
    private List<PfWell> wells = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "flow_station_id")
    private PfFlowStation flowStation;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "capacity_wells")
    private Integer capacityWells; // Número máximo de pozos

    @Column(name = "current_well_count")
    private Integer currentWellCount;

    @Column(name = "total_production_bpd")
    private Double totalProductionBpd;

    @Column(name = "commissioning_date")
    private LocalDate commissioningDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status")
    private OperationalStatus operationalStatus;
}
```

#### PfEspSystem (Sistema ESP)

```java
@Entity
@Table(name = "esp_system", schema = "pf")
@Data
public class PfEspSystem extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "well_id", nullable = false)
    private PfWell well;

    @Column(name = "pump_model")
    private String pumpModel;

    @Column(name = "pump_serial_number")
    private String pumpSerialNumber;

    @Column(name = "stages")
    private Integer stages;

    @Column(name = "rated_head_ft")
    private Double ratedHeadFt;

    @Column(name = "rated_flow_bpd")
    private Double ratedFlowBpd;

    @Column(name = "motor_hp")
    private Double motorHp;

    @Column(name = "motor_voltage")
    private Integer motorVoltage;

    @Column(name = "frequency_hz")
    private Double frequencyHz;

    @Column(name = "setting_depth_ft")
    private Double settingDepthFt;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "last_pulling_date")
    private LocalDate lastPullingDate;

    @Column(name = "run_life_days")
    private Integer runLifeDays;

    // Operational Limits
    @Column(name = "min_frequency_hz")
    private Double minFrequencyHz;

    @Column(name = "max_frequency_hz")
    private Double maxFrequencyHz;

    @Column(name = "min_current_amps")
    private Double minCurrentAmps;

    @Column(name = "max_current_amps")
    private Double maxCurrentAmps;

    @Column(name = "max_motor_temp_f")
    private Double maxMotorTempF;

    @Column(name = "min_pip_psi")
    private Double minPipPsi;

    @Column(name = "max_vibration_g")
    private Double maxVibrationG;
}
```

### 3.2 Tablas de Base de Datos

```sql
-- Schema creation
CREATE SCHEMA IF NOT EXISTS pf;

-- Well table
CREATE TABLE pf.well (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    api_number VARCHAR(50) UNIQUE,
    wellpad_id UUID,
    rv_well_id UUID,
    status VARCHAR(50) NOT NULL,
    lift_system_type VARCHAR(50),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    measured_depth_ft DOUBLE PRECISION,
    true_vertical_depth_ft DOUBLE PRECISION,
    spud_date DATE,
    first_production_date DATE,
    current_production_bpd DOUBLE PRECISION,
    additional_info JSONB,
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    CONSTRAINT fk_wellpad FOREIGN KEY (wellpad_id) REFERENCES pf.wellpad(id),
    CONSTRAINT fk_rv_well FOREIGN KEY (rv_well_id) REFERENCES rv.well(id)
);

CREATE INDEX idx_well_tenant ON pf.well(tenant_id);
CREATE INDEX idx_well_wellpad ON pf.well(wellpad_id);
CREATE INDEX idx_well_status ON pf.well(status);
CREATE INDEX idx_well_api_number ON pf.well(api_number);

-- Wellpad table
CREATE TABLE pf.wellpad (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE,
    flow_station_id UUID,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    capacity_wells INTEGER,
    current_well_count INTEGER,
    total_production_bpd DOUBLE PRECISION,
    commissioning_date DATE,
    operational_status VARCHAR(50),
    created_time BIGINT NOT NULL,
    updated_time BIGINT
);

-- Telemetry table (TimescaleDB hypertable)
CREATE TABLE pf.telemetry (
    time TIMESTAMPTZ NOT NULL,
    entity_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    key VARCHAR(255) NOT NULL,
    value_numeric DOUBLE PRECISION,
    value_string TEXT,
    value_boolean BOOLEAN,
    quality_score DOUBLE PRECISION DEFAULT 1.0,
    PRIMARY KEY (time, entity_id, key)
);

-- Convert to hypertable
SELECT create_hypertable('pf.telemetry', 'time',
    chunk_time_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);

-- Create indexes
CREATE INDEX idx_telemetry_entity ON pf.telemetry(entity_id, time DESC);
CREATE INDEX idx_telemetry_key ON pf.telemetry(key, time DESC);

-- Compression policy (compress data older than 7 days)
ALTER TABLE pf.telemetry SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'entity_id, key'
);

SELECT add_compression_policy('pf.telemetry', INTERVAL '7 days');

-- Retention policy (delete data older than 30 days)
SELECT add_retention_policy('pf.telemetry', INTERVAL '30 days');

-- Continuous aggregate for 1-minute data
CREATE MATERIALIZED VIEW pf.telemetry_1min
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', time) AS bucket,
    entity_id,
    key,
    AVG(value_numeric) AS avg_value,
    MAX(value_numeric) AS max_value,
    MIN(value_numeric) AS min_value,
    COUNT(*) AS sample_count
FROM pf.telemetry
WHERE value_numeric IS NOT NULL
GROUP BY bucket, entity_id, key;

-- Refresh policy for continuous aggregate
SELECT add_continuous_aggregate_policy('pf.telemetry_1min',
    start_offset => INTERVAL '1 hour',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute');

-- Operational Limits table
CREATE TABLE pf.operational_limit (
    id UUID PRIMARY KEY,
    entity_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    variable_key VARCHAR(255) NOT NULL,
    high_high_limit DOUBLE PRECISION,
    high_limit DOUBLE PRECISION,
    low_limit DOUBLE PRECISION,
    low_low_limit DOUBLE PRECISION,
    rate_of_change_limit DOUBLE PRECISION,
    deadband DOUBLE PRECISION DEFAULT 0.5,
    enabled BOOLEAN DEFAULT TRUE,
    created_time BIGINT NOT NULL,
    UNIQUE(entity_id, variable_key)
);

-- Alarms table
CREATE TABLE pf.alarm (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    entity_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    alarm_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    details JSONB,
    start_time BIGINT NOT NULL,
    end_time BIGINT,
    acknowledged_time BIGINT,
    acknowledged_by UUID,
    cleared_time BIGINT
);

CREATE INDEX idx_alarm_entity ON pf.alarm(entity_id, start_time DESC);
CREATE INDEX idx_alarm_status ON pf.alarm(status, severity);
CREATE INDEX idx_alarm_tenant ON pf.alarm(tenant_id, start_time DESC);
```

---

## 4. Servicios {#servicios}

### 4.1 PfWellService

```java
public interface PfWellService {

    /**
     * Create a new well
     */
    PfWell createWell(PfWell well);

    /**
     * Update existing well
     */
    PfWell updateWell(PfWell well);

    /**
     * Find well by ID
     */
    Optional<PfWell> findWellById(UUID tenantId, UUID wellId);

    /**
     * Find all wells for tenant
     */
    PageData<PfWell> findWellsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find wells by wellpad
     */
    List<PfWell> findWellsByWellpad(UUID wellpadId);

    /**
     * Find wells by status
     */
    List<PfWell> findWellsByStatus(UUID tenantId, WellStatus status);

    /**
     * Delete well
     */
    void deleteWell(UUID tenantId, UUID wellId);

    /**
     * Update well status
     */
    void updateWellStatus(UUID wellId, WellStatus newStatus);

    /**
     * Batch import wells from CSV
     */
    ImportResult importWellsFromCsv(UUID tenantId, MultipartFile file);
}
```

### 4.2 TelemetryProcessor

```java
@Service
@Slf4j
public class TelemetryProcessor {

    @Autowired
    private DataQualityValidator dataQualityValidator;

    @Autowired
    private TelemetryDao telemetryDao;

    @Autowired
    private KafkaTemplate<String, TelemetryData> kafkaTemplate;

    /**
     * Process telemetry from MQTT message
     */
    public void processTelemetry(String topic, byte[] payload) {
        try {
            // Parse payload
            TelemetryMessage message = parsePayload(payload);

            // Validate data quality
            double qualityScore = dataQualityValidator.validate(message);

            if (qualityScore >= 0.7) {
                // Store in TimescaleDB
                telemetryDao.save(message, qualityScore);

                // Publish to Kafka for downstream processing
                kafkaTemplate.send("pf.telemetry.validated", message);

                log.debug("Telemetry processed: entity={}, keys={}, quality={}",
                    message.getEntityId(), message.getKeys(), qualityScore);
            } else {
                log.warn("Low quality telemetry rejected: entity={}, quality={}",
                    message.getEntityId(), qualityScore);

                // Send to dead letter queue for review
                kafkaTemplate.send("pf.telemetry.dlq", message);
            }

        } catch (Exception e) {
            log.error("Error processing telemetry from topic {}", topic, e);
        }
    }

    /**
     * Batch insert telemetry (optimization for high-throughput)
     */
    @Async
    public CompletableFuture<Void> processTelemetryBatch(List<TelemetryMessage> batch) {
        try {
            // Validate all
            Map<TelemetryMessage, Double> validatedBatch = batch.stream()
                .collect(Collectors.toMap(
                    msg -> msg,
                    dataQualityValidator::validate
                ));

            // Filter by quality
            List<TelemetryMessage> goodData = validatedBatch.entrySet().stream()
                .filter(entry -> entry.getValue() >= 0.7)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            // Batch insert
            telemetryDao.saveBatch(goodData);

            log.info("Batch processed: total={}, accepted={}", batch.size(), goodData.size());

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Error in batch processing", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

### 4.3 AlarmService

```java
@Service
@Slf4j
public class AlarmService {

    @Autowired
    private PfAlarmDao alarmDao;

    @Autowired
    private OperationalLimitDao limitDao;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AlarmClassifier alarmClassifier;

    /**
     * Evaluate telemetry against operational limits
     */
    public void evaluateTelemetry(UUID entityId, String key, double value) {
        // Get operational limits for this variable
        Optional<OperationalLimit> limitOpt = limitDao.findByEntityAndKey(entityId, key);

        if (limitOpt.isEmpty()) {
            return; // No limits configured
        }

        OperationalLimit limit = limitOpt.get();

        // Check against limits
        AlarmSeverity severity = null;
        String message = null;

        if (value >= limit.getHighHighLimit()) {
            severity = AlarmSeverity.CRITICAL;
            message = String.format("%s is critically high: %.2f (limit: %.2f)",
                key, value, limit.getHighHighLimit());

        } else if (value >= limit.getHighLimit()) {
            severity = AlarmSeverity.HIGH;
            message = String.format("%s is high: %.2f (limit: %.2f)",
                key, value, limit.getHighLimit());

        } else if (value <= limit.getLowLowLimit()) {
            severity = AlarmSeverity.CRITICAL;
            message = String.format("%s is critically low: %.2f (limit: %.2f)",
                key, value, limit.getLowLowLimit());

        } else if (value <= limit.getLowLimit()) {
            severity = AlarmSeverity.HIGH;
            message = String.format("%s is low: %.2f (limit: %.2f)",
                key, value, limit.getLowLimit());
        }

        if (severity != null) {
            createAlarm(entityId, key, severity, message, value);
        } else {
            // Clear alarm if exists
            clearAlarm(entityId, key);
        }
    }

    /**
     * Create alarm
     */
    private void createAlarm(UUID entityId, String key, AlarmSeverity severity,
                            String message, double value) {

        // Check if alarm already exists and is active
        Optional<PfAlarm> existingAlarm = alarmDao.findActiveAlarm(entityId, key);

        if (existingAlarm.isPresent()) {
            // Update existing alarm
            PfAlarm alarm = existingAlarm.get();
            alarm.setMessage(message);
            alarm.setSeverity(severity);
            alarm.setUpdatedTime(System.currentTimeMillis());
            alarmDao.save(alarm);

            log.debug("Updated alarm: entity={}, key={}, severity={}",
                entityId, key, severity);

        } else {
            // Create new alarm
            PfAlarm alarm = PfAlarm.builder()
                .entityId(entityId)
                .alarmType("LIMIT_VIOLATION_" + key.toUpperCase())
                .severity(severity)
                .status(AlarmStatus.ACTIVE)
                .message(message)
                .details(buildAlarmDetails(key, value))
                .startTime(System.currentTimeMillis())
                .build();

            // Classify alarm (add tags, priority, recommended actions)
            alarmClassifier.classify(alarm);

            alarmDao.save(alarm);

            log.info("Created alarm: entity={}, key={}, severity={}",
                entityId, key, severity);

            // Send notifications if critical
            if (severity == AlarmSeverity.CRITICAL) {
                notificationService.notifyAlarm(alarm);
            }
        }
    }
}
```

---

## 5. APIs REST {#apis-rest}

### 5.1 Well Management API

```
Base URL: /api/nexus/pf
```

#### Create Well
```http
POST /wells
Content-Type: application/json
Authorization: Bearer {jwt_token}

Request Body:
{
  "name": "WELL-ABC-123",
  "apiNumber": "42-123-45678",
  "wellpadId": "uuid-of-wellpad",
  "status": "PRODUCING",
  "liftSystemType": "ESP",
  "latitude": 10.12345,
  "longitude": -65.67890,
  "measuredDepthFt": 8500,
  "trueVerticalDepthFt": 8200,
  "firstProductionDate": "2020-01-15"
}

Response: 201 Created
{
  "id": "generated-uuid",
  "name": "WELL-ABC-123",
  ...
  "createdTime": 1704816000000
}
```

#### Get Well by ID
```http
GET /wells/{wellId}
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "id": "well-uuid",
  "name": "WELL-ABC-123",
  "wellpad": {
    "id": "wellpad-uuid",
    "name": "MACOLLA-NORTE-01"
  },
  "status": "PRODUCING",
  "currentProductionBpd": 456.5,
  ...
}
```

#### Get Wells by Wellpad
```http
GET /wells?wellpadId={wellpadId}
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "data": [
    { "id": "...", "name": "WELL-001", ... },
    { "id": "...", "name": "WELL-002", ... }
  ],
  "totalElements": 12,
  "hasNext": false
}
```

### 5.2 Telemetry API

#### Get Latest Telemetry
```http
GET /wells/{wellId}/telemetry/latest
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "entityId": "well-uuid",
  "timestamp": 1704816000000,
  "values": {
    "frequency_hz": 52.3,
    "current_amps": 45.2,
    "temperature_motor_f": 285,
    "pip_psi": 156,
    "discharge_pressure_psi": 1850,
    "vibration_g": 0.45,
    "production_bpd": 456
  },
  "quality": 0.98
}
```

#### Query Historical Telemetry
```http
GET /wells/{wellId}/telemetry?
    from=1704729600000&
    to=1704816000000&
    keys=frequency_hz,temperature_motor_f&
    interval=1min

Response: 200 OK
{
  "entityId": "well-uuid",
  "from": 1704729600000,
  "to": 1704816000000,
  "data": [
    {
      "ts": 1704729600000,
      "frequency_hz": 52.0,
      "temperature_motor_f": 275
    },
    {
      "ts": 1704729660000,
      "frequency_hz": 52.1,
      "temperature_motor_f": 276
    },
    ...
  ],
  "aggregation": "1min"
}
```

### 5.3 Alarms API

#### Get Active Alarms
```http
GET /alarms/active?
    entityId={wellId}&
    severity=CRITICAL,HIGH

Response: 200 OK
{
  "data": [
    {
      "id": "alarm-uuid",
      "entityId": "well-uuid",
      "entityName": "WELL-ABC-123",
      "alarmType": "LIMIT_VIOLATION_TEMPERATURE_MOTOR_F",
      "severity": "CRITICAL",
      "status": "ACTIVE",
      "message": "temperature_motor_f is critically high: 295.00 (limit: 280.00)",
      "startTime": 1704816000000,
      "recommendedActions": [
        "Reduce frequency to 48 Hz",
        "Check cooling fluid flow",
        "Schedule pulling if temperature persists"
      ]
    }
  ]
}
```

#### Acknowledge Alarm
```http
POST /alarms/{alarmId}/acknowledge
Authorization: Bearer {jwt_token}

Request Body:
{
  "comment": "Reduced frequency to 48 Hz, monitoring temperature"
}

Response: 200 OK
{
  "id": "alarm-uuid",
  "status": "ACKNOWLEDGED",
  "acknowledgedTime": 1704816120000,
  "acknowledgedBy": "user-uuid"
}
```

---

## 6. Procesamiento de Telemetría {#telemetria}

### 6.1 Pipeline de Telemetría

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  Device  │────▶│   MQTT   │────▶│  Kafka   │────▶│   App    │
│ (PLC/RTU)│     │  Broker  │     │  Topic   │     │ Consumer │
└──────────┘     └──────────┘     └──────────┘     └────┬─────┘
                                                         │
                                                         ▼
                                                  ┌──────────────┐
                                                  │  Validation  │
                                                  │  & Quality   │
                                                  └──────┬───────┘
                                                         │
                                    ┌────────────────────┼────────────────────┐
                                    ▼                    ▼                    ▼
                             ┌────────────┐      ┌────────────┐      ┌────────────┐
                             │TimescaleDB │      │   Alarm    │      │  WebSocket │
                             │  Storage   │      │  Evaluator │      │   Push     │
                             └────────────┘      └────────────┘      └────────────┘
```

### 6.2 Data Quality Validator

```java
@Component
public class DataQualityValidator {

    /**
     * Validate telemetry data quality
     * Returns quality score 0.0 (invalid) to 1.0 (perfect)
     */
    public double validate(TelemetryMessage message) {
        double score = 1.0;

        // Rule 1: Range validation
        score *= validateRange(message);

        // Rule 2: Rate of change validation
        score *= validateRateOfChange(message);

        // Rule 3: Missing data check
        score *= validateCompleteness(message);

        // Rule 4: Outlier detection
        score *= validateOutliers(message);

        return score;
    }

    private double validateRange(TelemetryMessage message) {
        // Check if values are within expected physical ranges
        for (Map.Entry<String, Double> entry : message.getValues().entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();

            PhysicalRange range = getRangeForKey(key);
            if (range != null && !range.contains(value)) {
                return 0.5; // Suspicious data
            }
        }
        return 1.0;
    }

    private double validateRateOfChange(TelemetryMessage message) {
        // Compare with previous value
        TelemetryMessage previous = getPreviousMessage(message.getEntityId());

        if (previous != null) {
            long timeDiff = message.getTimestamp() - previous.getTimestamp();
            double timeDiffMinutes = timeDiff / 60000.0;

            for (String key : message.getValues().keySet()) {
                Double currentValue = message.getValue(key);
                Double previousValue = previous.getValue(key);

                if (currentValue != null && previousValue != null) {
                    double percentChange = Math.abs(
                        (currentValue - previousValue) / previousValue * 100
                    );

                    // Max 10% change per minute
                    if (percentChange / timeDiffMinutes > 10) {
                        return 0.7; // Suspicious rapid change
                    }
                }
            }
        }

        return 1.0;
    }
}
```

---

## 7. Sistema de Alarmas {#alarmas}

### 7.1 Tipos de Alarmas

| Alarm Type | Description | Severity |
|------------|-------------|----------|
| `LIMIT_VIOLATION_HIGH_HIGH` | Variable excede high-high limit | CRITICAL |
| `LIMIT_VIOLATION_HIGH` | Variable excede high limit | HIGH |
| `LIMIT_VIOLATION_LOW` | Variable debajo de low limit | HIGH |
| `LIMIT_VIOLATION_LOW_LOW` | Variable debajo de low-low limit | CRITICAL |
| `RATE_OF_CHANGE` | Cambio muy rápido | HIGH |
| `COMMUNICATION_LOST` | Pérdida de comunicación con device | HIGH |
| `DATA_QUALITY_LOW` | Calidad de datos baja | MEDIUM |
| `EQUIPMENT_FAILURE` | Falla detectada de equipo | CRITICAL |

### 7.2 Workflow de Alarmas

```
┌─────────────┐
│ Telemetry   │
│  Received   │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ Evaluate        │
│ Against Limits  │
└──────┬──────────┘
       │
       ├──────────────┐
       │              │
       ▼              ▼
┌─────────┐    ┌─────────┐
│  Normal │    │ Violated│
└─────────┘    └────┬────┘
                    │
                    ▼
             ┌──────────────┐
             │ Create/Update│
             │    Alarm     │
             └──────┬───────┘
                    │
                    ▼
             ┌──────────────┐
             │   Classify   │
             │ (auto-tagging)│
             └──────┬───────┘
                    │
              ┌─────┴─────┐
              │           │
              ▼           ▼
        ┌─────────┐ ┌─────────┐
        │  Save   │ │ Notify  │
        │   DB    │ │ (if critical)│
        └─────────┘ └─────────┘
```

---

## 8. Frontend Components {#frontend}

### 8.1 Component Structure

```
ui-ngx/src/app/modules/home/pages/pf/
├── wellpad-list/
│   ├── wellpad-list.component.ts
│   ├── wellpad-list.component.html
│   └── wellpad-list.component.scss
├── wellpad-dashboard/
│   ├── wellpad-dashboard.component.ts
│   ├── wellpad-dashboard.component.html
│   └── wellpad-dashboard.component.scss
├── well-detail/
│   ├── well-detail.component.ts
│   ├── well-detail.component.html
│   ├── well-detail.component.scss
│   └── components/
│       ├── well-info-card/
│       ├── well-diagram/
│       ├── well-trend-chart/
│       └── well-actions/
├── flow-station-dashboard/
├── alarm-list/
└── shared/
    ├── services/
    │   ├── pf-well.service.ts
    │   ├── pf-telemetry.service.ts
    │   └── pf-alarm.service.ts
    ├── models/
    │   ├── pf-well.model.ts
    │   ├── pf-telemetry.model.ts
    │   └── pf-alarm.model.ts
    └── components/
        ├── status-indicator/
        ├── production-gauge/
        └── trend-sparkline/
```

### 8.2 Well Dashboard Component

```typescript
@Component({
  selector: 'tb-well-detail-dashboard',
  templateUrl: './well-detail.component.html',
  styleUrls: ['./well-detail.component.scss']
})
export class WellDetailComponent implements OnInit, OnDestroy {

  wellId: string;
  well: PfWell;
  latestTelemetry: TelemetryData;
  activeAlarms: PfAlarm[] = [];

  private destroy$ = new Subject<void>();
  private telemetrySubscription: Subscription;

  constructor(
    private route: ActivatedRoute,
    private wellService: PfWellService,
    private telemetryService: PfTelemetryService,
    private alarmService: PfAlarmService,
    private wsService: WebSocketService
  ) {}

  ngOnInit() {
    this.wellId = this.route.snapshot.params['wellId'];
    this.loadWellData();
    this.subscribeToRealtimeTelemetry();
    this.loadActiveAlarms();
  }

  loadWellData() {
    this.wellService.getWell(this.wellId).subscribe(well => {
      this.well = well;
    });
  }

  subscribeToRealtimeTelemetry() {
    // WebSocket subscription for real-time updates
    this.telemetrySubscription = this.wsService
      .subscribeTelemetry(this.wellId)
      .pipe(takeUntil(this.destroy$))
      .subscribe(telemetry => {
        this.latestTelemetry = telemetry;
        this.updateCharts(telemetry);
      });
  }

  loadActiveAlarms() {
    this.alarmService.getActiveAlarms(this.wellId).subscribe(alarms => {
      this.activeAlarms = alarms;
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

---

**Documento completo. Siguiente: PO_MODULE_SPEC.md**
