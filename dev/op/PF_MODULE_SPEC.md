# PF MODULE SPECIFICATION - Production Facilities

**Módulo**: Production Facilities (PF)
**Versión**: 2.0
**Fecha**: 2026-02-03
**Propósito**: Monitoreo en tiempo real de infraestructura de producción de superficie

---

## Tabla de Contenidos

1. [Overview](#overview)
2. [Decisión Arquitectónica](#decision-arquitectonica)
3. [Arquitectura del Módulo](#arquitectura)
4. [Modelo de Datos](#modelo-de-datos)
5. [Servicios](#servicios)
6. [APIs REST](#apis-rest)
7. [Procesamiento de Telemetría](#telemetria)
8. [Sistema de Alarmas](#alarmas)
9. [Frontend Components](#frontend)

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

**Lo que hace el módulo PF**:
- Capturar telemetría desde devices de campo vía MQTT/OPC-UA/Modbus
- Validar calidad de datos mediante Rule Nodes personalizados
- Almacenar series temporales en tablas ts_kv de ThingsBoard
- Detectar alarmas usando TB Alarm System con Asset Profiles
- Proporcionar dashboards de monitoreo
- Gestionar configuración de activos de producción como TB Assets

**Lo que NO hace**:
- Optimización (eso es PO Module)
- Predicción de fallas con ML (eso es PO Module)
- Caracterización de yacimientos (eso es RV Module)

---

## 2. Decisión Arquitectónica {#decision-arquitectonica}

### 2.1 Patrón: Assets + Attributes (ThingsBoard Core)

El módulo PF utiliza el sistema nativo de Assets y Attributes de ThingsBoard en lugar de entidades JPA personalizadas. Este patrón es consistente con los módulos CT (Coiled Tubing) y RV (Yacimientos).

### 2.2 Comparación de Enfoques

| Aspecto | Tablas Custom (NO usar) | TB Assets + Attributes (USAR) |
|---------|-------------------------|-------------------------------|
| **Multi-tenancy** | Implementar manualmente | Built-in via TenantId |
| **Relaciones** | FKs personalizadas | TB Relation system |
| **Telemetría** | TimescaleDB custom | ts_kv nativo |
| **Alarmas** | Tabla pf.alarm custom | TB Alarm System |
| **UI Integration** | Dashboards custom | TB Dashboard widgets nativos |
| **Permisos** | RBAC personalizado | TB permission system |
| **Mantenimiento** | Alto - migraciones de schema | Bajo - solo atributos |

### 2.3 Cuándo Usar Tablas Custom

Solo para datos que NO encajan en el modelo Asset/Attribute:
- `pf_optimization_result` - Resultados de cálculos de optimización
- `pf_recommendation` - Recomendaciones con workflow de aprobación

### 2.4 Referencia de Patrón

Este módulo sigue el mismo patrón establecido por:
- **CT Module**: `CTAssetService`, `CTAttributeService`, tablas custom solo para `ct_jobs` y `ct_fatigue_log`
- **RV Module**: `RvAssetService`, `RvAttributeService`, sin tablas custom

---

## 3. Arquitectura del Módulo {#arquitectura}

### 3.1 Estructura de Paquetes

```
org.thingsboard.nexus.pf
├── config
│   └── PfModuleConfiguration.java
├── dto
│   ├── PfWellDto.java
│   ├── PfWellpadDto.java
│   ├── PfFlowStationDto.java
│   ├── PfEspSystemDto.java
│   ├── PfPcpSystemDto.java
│   ├── PfGasLiftSystemDto.java
│   ├── WellStatus.java
│   ├── LiftSystemType.java
│   └── OperationalStatus.java
├── service
│   ├── PfAssetService.java           # Wrapper sobre TB AssetService
│   ├── PfAttributeService.java       # Wrapper sobre TB AttributesService
│   ├── PfHierarchyService.java       # Gestión de relaciones parent-child
│   ├── PfWellService.java            # Lógica de negocio de pozos
│   ├── PfWellpadService.java
│   ├── PfFlowStationService.java
│   ├── PfEspSystemService.java
│   ├── PfTelemetryService.java       # Wrapper sobre TB TelemetryService
│   ├── PfAlarmService.java           # Wrapper sobre TB AlarmService
│   └── PfDataQualityService.java
├── controller
│   ├── PfWellController.java
│   ├── PfWellpadController.java
│   ├── PfFlowStationController.java
│   ├── PfTelemetryController.java
│   └── PfAlarmController.java
├── model (SOLO para tablas custom)
│   ├── PfOptimizationResult.java
│   └── PfRecommendation.java
├── repository (SOLO para tablas custom)
│   ├── PfOptimizationResultRepository.java
│   └── PfRecommendationRepository.java
├── rule (Rule Nodes personalizados)
│   ├── PfDataQualityNode.java
│   └── PfAlarmEvaluationNode.java
└── exception
    ├── PfException.java
    ├── PfEntityNotFoundException.java
    └── PfBusinessException.java
```

### 3.2 Dependencias Maven

```xml
<!-- PF Module Dependencies -->
<dependencies>
    <!-- ThingsBoard Core -->
    <dependency>
        <groupId>org.thingsboard</groupId>
        <artifactId>common</artifactId>
    </dependency>

    <!-- ThingsBoard DAO -->
    <dependency>
        <groupId>org.thingsboard</groupId>
        <artifactId>dao</artifactId>
    </dependency>

    <!-- MQTT Client (para integración SCADA) -->
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

    <!-- NO se requiere TimescaleDB - usamos ts_kv de ThingsBoard -->
</dependencies>
```

---

## 4. Modelo de Datos {#modelo-de-datos}

### 4.1 Estrategia de Almacenamiento

El módulo PF utiliza el modelo de datos nativo de ThingsBoard:

| Tipo de Dato | Ubicación | Descripción |
|--------------|-----------|-------------|
| **Metadatos de pozo** | `asset` + `attribute_kv` | Asset type = `pf_well`, atributos en SERVER_SCOPE |
| **Metadatos de macolla** | `asset` + `attribute_kv` | Asset type = `pf_wellpad` |
| **Config. sistema ESP** | `asset` + `attribute_kv` | Asset type = `pf_esp_system` |
| **Jerarquías** | `relation` | Relaciones parent-child (Wellpad → Wells) |
| **Telemetría real-time** | `ts_kv`, `ts_kv_dictionary` | Series temporales nativas de TB |
| **Alarmas** | `alarm` | Sistema de alarmas de ThingsBoard |
| **Resultados optimización** | `pf_optimization_result` | Tabla custom |
| **Recomendaciones** | `pf_recommendation` | Tabla custom |

### 4.2 Asset Types

```
pf_well            - Pozo productor
pf_wellpad         - Macolla/Cluster de pozos
pf_flow_station    - Estación de flujo/separación
pf_separator       - Separador
pf_tank            - Tanque de almacenamiento
pf_pipeline        - Tubería de recolección
pf_esp_system      - Sistema de levantamiento ESP
pf_pcp_system      - Sistema de levantamiento PCP
pf_gas_lift_system - Sistema Gas Lift
pf_rod_pump_system - Sistema de bombeo mecánico
```

### 4.3 DTOs con Constantes de Atributos

#### PfWellDto (Pozo Productor)

```java
/**
 * DTO para Pozo Productor - usa ThingsBoard Assets + Attributes
 * Sigue el patrón establecido por CT y RV modules
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfWellDto {

    // Asset type constant para ThingsBoard
    public static final String ASSET_TYPE = "pf_well";

    // Attribute key constants (almacenados en TB attribute_kv)
    public static final String ATTR_API_NUMBER = "api_number";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_LIFT_SYSTEM_TYPE = "lift_system_type";
    public static final String ATTR_LATITUDE = "latitude";
    public static final String ATTR_LONGITUDE = "longitude";
    public static final String ATTR_MEASURED_DEPTH_FT = "measured_depth_ft";
    public static final String ATTR_TRUE_VERTICAL_DEPTH_FT = "true_vertical_depth_ft";
    public static final String ATTR_SPUD_DATE = "spud_date";
    public static final String ATTR_FIRST_PRODUCTION_DATE = "first_production_date";
    public static final String ATTR_CURRENT_PRODUCTION_BPD = "current_production_bpd";
    public static final String ATTR_RV_WELL_ID = "rv_well_id";
    public static final String ATTR_WELLPAD_ID = "wellpad_id";

    // Asset ID en ThingsBoard (del Asset entity)
    private UUID assetId;

    @NotNull
    private UUID tenantId;

    @NotBlank
    @Size(min = 2, max = 255)
    private String name;

    // Almacenados como SERVER_SCOPE attributes
    private String apiNumber;
    private WellStatus status;
    private LiftSystemType liftSystemType;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal measuredDepthFt;
    private BigDecimal trueVerticalDepthFt;
    private LocalDate spudDate;
    private LocalDate firstProductionDate;
    private BigDecimal currentProductionBpd;

    // Referencias (vía TB Relations o atributos)
    private UUID wellpadId;
    private UUID rvWellId;

    // Audit fields (del TB Asset)
    private Long createdTime;
    private Long updatedTime;

    /**
     * Construye mapa de atributos para guardar en ThingsBoard
     */
    public Map<String, Object> toAttributeMap() {
        Map<String, Object> attrs = new HashMap<>();
        if (apiNumber != null) attrs.put(ATTR_API_NUMBER, apiNumber);
        if (status != null) attrs.put(ATTR_STATUS, status.name());
        if (liftSystemType != null) attrs.put(ATTR_LIFT_SYSTEM_TYPE, liftSystemType.name());
        if (latitude != null) attrs.put(ATTR_LATITUDE, latitude.doubleValue());
        if (longitude != null) attrs.put(ATTR_LONGITUDE, longitude.doubleValue());
        if (measuredDepthFt != null) attrs.put(ATTR_MEASURED_DEPTH_FT, measuredDepthFt.doubleValue());
        if (trueVerticalDepthFt != null) attrs.put(ATTR_TRUE_VERTICAL_DEPTH_FT, trueVerticalDepthFt.doubleValue());
        if (spudDate != null) attrs.put(ATTR_SPUD_DATE, spudDate.toString());
        if (firstProductionDate != null) attrs.put(ATTR_FIRST_PRODUCTION_DATE, firstProductionDate.toString());
        if (currentProductionBpd != null) attrs.put(ATTR_CURRENT_PRODUCTION_BPD, currentProductionBpd.doubleValue());
        if (rvWellId != null) attrs.put(ATTR_RV_WELL_ID, rvWellId.toString());
        if (wellpadId != null) attrs.put(ATTR_WELLPAD_ID, wellpadId.toString());
        return attrs;
    }
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

#### PfWellpadDto (Macolla/Cluster)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfWellpadDto {

    public static final String ASSET_TYPE = "pf_wellpad";

    // Attribute key constants
    public static final String ATTR_CODE = "code";
    public static final String ATTR_LATITUDE = "latitude";
    public static final String ATTR_LONGITUDE = "longitude";
    public static final String ATTR_CAPACITY_WELLS = "capacity_wells";
    public static final String ATTR_CURRENT_WELL_COUNT = "current_well_count";
    public static final String ATTR_TOTAL_PRODUCTION_BPD = "total_production_bpd";
    public static final String ATTR_COMMISSIONING_DATE = "commissioning_date";
    public static final String ATTR_OPERATIONAL_STATUS = "operational_status";
    public static final String ATTR_FLOW_STATION_ID = "flow_station_id";

    private UUID assetId;
    private UUID tenantId;

    @NotBlank
    private String name;

    private String code;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer capacityWells;
    private Integer currentWellCount;
    private BigDecimal totalProductionBpd;
    private LocalDate commissioningDate;
    private OperationalStatus operationalStatus;
    private UUID flowStationId;

    private Long createdTime;
    private Long updatedTime;
}
```

#### PfEspSystemDto (Sistema ESP)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfEspSystemDto {

    public static final String ASSET_TYPE = "pf_esp_system";

    // Attribute key constants
    public static final String ATTR_WELL_ID = "well_id";
    public static final String ATTR_PUMP_MODEL = "pump_model";
    public static final String ATTR_PUMP_SERIAL_NUMBER = "pump_serial_number";
    public static final String ATTR_STAGES = "stages";
    public static final String ATTR_RATED_HEAD_FT = "rated_head_ft";
    public static final String ATTR_RATED_FLOW_BPD = "rated_flow_bpd";
    public static final String ATTR_MOTOR_HP = "motor_hp";
    public static final String ATTR_MOTOR_VOLTAGE = "motor_voltage";
    public static final String ATTR_FREQUENCY_HZ = "frequency_hz";
    public static final String ATTR_SETTING_DEPTH_FT = "setting_depth_ft";
    public static final String ATTR_INSTALLATION_DATE = "installation_date";
    public static final String ATTR_LAST_PULLING_DATE = "last_pulling_date";
    public static final String ATTR_RUN_LIFE_DAYS = "run_life_days";

    // Operational Limits (también como atributos)
    public static final String ATTR_MIN_FREQUENCY_HZ = "min_frequency_hz";
    public static final String ATTR_MAX_FREQUENCY_HZ = "max_frequency_hz";
    public static final String ATTR_MIN_CURRENT_AMPS = "min_current_amps";
    public static final String ATTR_MAX_CURRENT_AMPS = "max_current_amps";
    public static final String ATTR_MAX_MOTOR_TEMP_F = "max_motor_temp_f";
    public static final String ATTR_MIN_PIP_PSI = "min_pip_psi";
    public static final String ATTR_MAX_VIBRATION_G = "max_vibration_g";

    private UUID assetId;
    private UUID tenantId;
    private String name;

    private UUID wellId;
    private String pumpModel;
    private String pumpSerialNumber;
    private Integer stages;
    private Double ratedHeadFt;
    private Double ratedFlowBpd;
    private Double motorHp;
    private Integer motorVoltage;
    private Double frequencyHz;
    private Double settingDepthFt;
    private LocalDate installationDate;
    private LocalDate lastPullingDate;
    private Integer runLifeDays;

    // Operational Limits
    private Double minFrequencyHz;
    private Double maxFrequencyHz;
    private Double minCurrentAmps;
    private Double maxCurrentAmps;
    private Double maxMotorTempF;
    private Double minPipPsi;
    private Double maxVibrationG;
}
```

### 4.4 Tablas Custom (Solo para datos específicos del dominio)

```sql
-- El módulo PF usa tablas core de ThingsBoard:
-- - asset: Almacena pf_well, pf_wellpad, pf_flow_station, pf_esp_system assets
-- - attribute_kv: Almacena SERVER_SCOPE attributes para cada asset
-- - ts_kv, ts_kv_dictionary: Almacena telemetría time-series
-- - alarm: Almacena alarmas via TB Alarm System
-- - relation: Almacena jerarquías parent-child

-- Tablas custom SOLO para datos específicos del módulo
-- (similar a como CT tiene ct_jobs y ct_fatigue_log)

-- Resultados de optimización (historial de cálculos)
CREATE TABLE IF NOT EXISTS pf_optimization_result (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    well_id UUID NOT NULL,              -- Referencia a TB Asset ID
    optimization_type VARCHAR(50) NOT NULL,
    current_value DOUBLE PRECISION,
    recommended_value DOUBLE PRECISION,
    expected_benefit DOUBLE PRECISION,
    confidence DOUBLE PRECISION,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    risks JSONB,
    recommended_actions JSONB,
    simulation_result JSONB,
    created_time BIGINT NOT NULL,
    executed_time BIGINT,
    created_by UUID,
    executed_by UUID,
    notes TEXT,
    CONSTRAINT fk_opt_result_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE INDEX idx_pf_opt_result_tenant ON pf_optimization_result(tenant_id);
CREATE INDEX idx_pf_opt_result_well ON pf_optimization_result(well_id);
CREATE INDEX idx_pf_opt_result_status ON pf_optimization_result(status);
CREATE INDEX idx_pf_opt_result_type ON pf_optimization_result(optimization_type);

-- Recomendaciones del sistema (con workflow de aprobación)
CREATE TABLE IF NOT EXISTS pf_recommendation (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    well_id UUID NOT NULL,              -- Referencia a TB Asset ID
    recommendation_type VARCHAR(100) NOT NULL,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    description TEXT,
    expected_benefit_usd DOUBLE PRECISION,
    confidence DOUBLE PRECISION,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    required_approval_level VARCHAR(50),
    submitted_by UUID,
    submitted_at BIGINT,
    approved_by UUID,
    approved_at BIGINT,
    approval_comments TEXT,
    executed_at BIGINT,
    effectiveness_score DOUBLE PRECISION,
    metadata JSONB,
    created_time BIGINT NOT NULL,
    expires_at BIGINT,
    CONSTRAINT fk_rec_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE INDEX idx_pf_rec_tenant ON pf_recommendation(tenant_id);
CREATE INDEX idx_pf_rec_well ON pf_recommendation(well_id);
CREATE INDEX idx_pf_rec_status ON pf_recommendation(status);
CREATE INDEX idx_pf_rec_priority ON pf_recommendation(priority);
```

---

## 5. Servicios {#servicios}

### 5.1 PfAssetService (Wrapper sobre TB AssetService)

```java
/**
 * Service wrapper sobre ThingsBoard AssetService para el módulo PF.
 * Sigue el patrón establecido por CTAssetService y RvAssetService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfAssetService {

    private final AssetService assetService;

    // Asset type constants del módulo PF
    public static final String TYPE_WELL = "pf_well";
    public static final String TYPE_WELLPAD = "pf_wellpad";
    public static final String TYPE_FLOW_STATION = "pf_flow_station";
    public static final String TYPE_SEPARATOR = "pf_separator";
    public static final String TYPE_TANK = "pf_tank";
    public static final String TYPE_PIPELINE = "pf_pipeline";
    public static final String TYPE_ESP_SYSTEM = "pf_esp_system";
    public static final String TYPE_PCP_SYSTEM = "pf_pcp_system";
    public static final String TYPE_GAS_LIFT_SYSTEM = "pf_gas_lift_system";
    public static final String TYPE_ROD_PUMP_SYSTEM = "pf_rod_pump_system";

    /**
     * Crea un nuevo asset
     */
    public Asset createAsset(UUID tenantId, String assetType, String name, String label) {
        log.info("Creating PF asset: type={}, name={}", assetType, name);

        Asset asset = new Asset();
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setType(assetType);
        asset.setName(name);
        asset.setLabel(label != null ? label : name);

        Asset savedAsset = assetService.saveAsset(asset);
        log.debug("Created asset with ID: {}", savedAsset.getId());

        return savedAsset;
    }

    /**
     * Obtiene asset por ID
     */
    public Optional<Asset> getAssetById(UUID assetId) {
        try {
            Asset asset = assetService.findAssetById(null, new AssetId(assetId));
            return Optional.ofNullable(asset);
        } catch (Exception e) {
            log.error("Error getting asset {}: {}", assetId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Obtiene assets por tipo con paginación
     */
    public Page<Asset> getAssetsByType(UUID tenantId, String assetType, int page, int size) {
        log.debug("Getting assets: tenantId={}, type={}, page={}, size={}",
                  tenantId, assetType, page, size);

        PageLink pageLink = new PageLink(size, page);
        PageData<Asset> pageData = assetService.findAssetsByTenantIdAndType(
            TenantId.fromUUID(tenantId), assetType, pageLink);

        return new PageImpl<>(
            pageData.getData(),
            PageRequest.of(page, size),
            pageData.getTotalElements()
        );
    }

    /**
     * Busca assets por nombre
     */
    public Page<Asset> searchAssetsByName(UUID tenantId, String assetType,
                                          String searchText, int page, int size) {
        PageLink pageLink = new PageLink(size, page, searchText);
        PageData<Asset> pageData = assetService.findAssetsByTenantIdAndType(
            TenantId.fromUUID(tenantId), assetType, pageLink);

        return new PageImpl<>(
            pageData.getData(),
            PageRequest.of(page, size),
            pageData.getTotalElements()
        );
    }

    /**
     * Actualiza un asset
     */
    public Asset updateAsset(Asset asset) {
        log.info("Updating asset: {}", asset.getId());
        return assetService.saveAsset(asset);
    }

    /**
     * Elimina un asset
     */
    public void deleteAsset(UUID tenantId, UUID assetId) {
        log.warn("Deleting asset: {}", assetId);
        assetService.deleteAsset(TenantId.fromUUID(tenantId), new AssetId(assetId));
    }

    /**
     * Verifica si un asset existe
     */
    public boolean existsById(UUID assetId) {
        return getAssetById(assetId).isPresent();
    }

    /**
     * Cuenta assets por tipo
     */
    public long countByType(UUID tenantId, String assetType) {
        PageLink pageLink = new PageLink(1, 0);
        PageData<Asset> pageData = assetService.findAssetsByTenantIdAndType(
            TenantId.fromUUID(tenantId), assetType, pageLink);
        return pageData.getTotalElements();
    }

    /**
     * Obtiene todos los tipos de asset del módulo PF
     */
    public List<String> getAllPfAssetTypes() {
        return List.of(
            TYPE_WELL, TYPE_WELLPAD, TYPE_FLOW_STATION, TYPE_SEPARATOR,
            TYPE_TANK, TYPE_PIPELINE, TYPE_ESP_SYSTEM, TYPE_PCP_SYSTEM,
            TYPE_GAS_LIFT_SYSTEM, TYPE_ROD_PUMP_SYSTEM
        );
    }

    /**
     * Valida que el tipo sea válido para PF
     */
    public boolean isValidPfAssetType(String assetType) {
        return getAllPfAssetTypes().contains(assetType);
    }
}
```

### 5.2 PfAttributeService (Wrapper sobre TB AttributesService)

```java
/**
 * Service wrapper sobre ThingsBoard AttributesService para el módulo PF.
 * Maneja SERVER_SCOPE y SHARED_SCOPE attributes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfAttributeService {

    private final AttributesService attributesService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Guarda atributos SERVER_SCOPE
     */
    public void saveServerAttributes(UUID entityId, Map<String, Object> attributes) {
        log.debug("Saving server attributes for entity {}: {} keys",
                  entityId, attributes.size());

        try {
            List<AttributeKvEntry> kvEntries = new ArrayList<>();

            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                AttributeKvEntry kvEntry = createKvEntry(entry.getKey(), entry.getValue());
                if (kvEntry != null) {
                    kvEntries.add(kvEntry);
                }
            }

            if (!kvEntries.isEmpty()) {
                attributesService.save(null, new AssetId(entityId),
                                      AttributeScope.SERVER_SCOPE, kvEntries);
            }
        } catch (Exception e) {
            log.error("Error saving attributes for entity {}: {}", entityId, e.getMessage());
            throw new RuntimeException("Failed to save attributes", e);
        }
    }

    /**
     * Guarda atributos SHARED_SCOPE (visibles por devices)
     */
    public void saveSharedAttributes(UUID entityId, Map<String, Object> attributes) {
        try {
            List<AttributeKvEntry> kvEntries = attributes.entrySet().stream()
                .map(e -> createKvEntry(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            if (!kvEntries.isEmpty()) {
                attributesService.save(null, new AssetId(entityId),
                                      AttributeScope.SHARED_SCOPE, kvEntries);
            }
        } catch (Exception e) {
            log.error("Error saving shared attributes: {}", e.getMessage());
            throw new RuntimeException("Failed to save shared attributes", e);
        }
    }

    /**
     * Obtiene todos los atributos SERVER_SCOPE
     */
    public List<AttributeKvEntry> getServerAttributes(UUID assetId) {
        try {
            return attributesService.findAll(null, new AssetId(assetId),
                                            AttributeScope.SERVER_SCOPE).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting attributes for asset {}: {}", assetId, e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get attributes", e);
        }
    }

    /**
     * Obtiene atributos específicos por keys
     */
    public List<AttributeKvEntry> getServerAttributes(UUID assetId, List<String> keys) {
        try {
            return attributesService.find(null, new AssetId(assetId),
                                         AttributeScope.SERVER_SCOPE, keys).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get attributes", e);
        }
    }

    /**
     * Elimina atributos
     */
    public void deleteAttributes(UUID assetId, AttributeScope scope, List<String> keys) {
        attributesService.removeAll(null, new AssetId(assetId), scope, keys);
    }

    private AttributeKvEntry createKvEntry(String key, Object value) {
        if (value == null) return null;

        long ts = System.currentTimeMillis();

        if (value instanceof String) {
            return new BaseAttributeKvEntry(new StringDataEntry(key, (String) value), ts);
        } else if (value instanceof Integer) {
            return new BaseAttributeKvEntry(new LongDataEntry(key, ((Integer) value).longValue()), ts);
        } else if (value instanceof Long) {
            return new BaseAttributeKvEntry(new LongDataEntry(key, (Long) value), ts);
        } else if (value instanceof Double) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, (Double) value), ts);
        } else if (value instanceof Float) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, ((Float) value).doubleValue()), ts);
        } else if (value instanceof BigDecimal) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, ((BigDecimal) value).doubleValue()), ts);
        } else if (value instanceof Boolean) {
            return new BaseAttributeKvEntry(new BooleanDataEntry(key, (Boolean) value), ts);
        } else if (value instanceof JsonNode) {
            return new BaseAttributeKvEntry(new JsonDataEntry(key, ((JsonNode) value).toString()), ts);
        } else {
            // Objetos complejos -> JSON
            try {
                JsonNode jsonNode = objectMapper.valueToTree(value);
                return new BaseAttributeKvEntry(new JsonDataEntry(key, jsonNode.toString()), ts);
            } catch (Exception e) {
                return new BaseAttributeKvEntry(new StringDataEntry(key, value.toString()), ts);
            }
        }
    }
}
```

### 5.3 PfWellService (Lógica de Negocio)

```java
/**
 * Servicio de lógica de negocio para pozos.
 * Usa PfAssetService y PfAttributeService para persistencia.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfWellService {

    private final PfAssetService pfAssetService;
    private final PfAttributeService pfAttributeService;
    private final PfHierarchyService pfHierarchyService;

    /**
     * Crea un nuevo pozo
     */
    @Transactional
    public PfWellDto createWell(UUID tenantId, PfWellDto dto) {
        log.info("Creating well: {}", dto.getName());

        // 1. Crear ThingsBoard Asset
        Asset asset = pfAssetService.createAsset(
            tenantId,
            PfWellDto.ASSET_TYPE,
            dto.getName(),
            dto.getName()
        );

        dto.setAssetId(asset.getId().getId());
        dto.setTenantId(tenantId);
        dto.setCreatedTime(asset.getCreatedTime());

        // 2. Guardar atributos
        Map<String, Object> attrs = dto.toAttributeMap();
        if (!attrs.isEmpty()) {
            pfAttributeService.saveServerAttributes(dto.getAssetId(), attrs);
        }

        // 3. Crear relación jerárquica si hay wellpad
        if (dto.getWellpadId() != null) {
            pfHierarchyService.setParentChild(
                tenantId,
                dto.getWellpadId(),  // parent
                dto.getAssetId()     // child
            );
        }

        log.info("Well created: id={}, name={}", dto.getAssetId(), dto.getName());
        return dto;
    }

    /**
     * Obtiene un pozo por ID
     */
    public Optional<PfWellDto> getWellById(UUID tenantId, UUID wellId) {
        return pfAssetService.getAssetById(wellId)
            .filter(asset -> PfWellDto.ASSET_TYPE.equals(asset.getType()))
            .map(asset -> buildWellDto(asset));
    }

    /**
     * Lista pozos con paginación
     */
    public Page<PfWellDto> getWells(UUID tenantId, int page, int size) {
        Page<Asset> assets = pfAssetService.getAssetsByType(
            tenantId, PfWellDto.ASSET_TYPE, page, size);

        return assets.map(this::buildWellDto);
    }

    /**
     * Lista pozos por wellpad
     */
    public List<PfWellDto> getWellsByWellpad(UUID tenantId, UUID wellpadId) {
        // Usar TB Relations para encontrar hijos
        List<UUID> childIds = pfHierarchyService.getChildren(wellpadId, PfWellDto.ASSET_TYPE);

        return childIds.stream()
            .map(id -> getWellById(tenantId, id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * Actualiza un pozo
     */
    @Transactional
    public PfWellDto updateWell(UUID tenantId, UUID wellId, PfWellDto dto) {
        Asset asset = pfAssetService.getAssetById(wellId)
            .orElseThrow(() -> new PfEntityNotFoundException("Well not found: " + wellId));

        // Actualizar nombre si cambió
        if (dto.getName() != null && !dto.getName().equals(asset.getName())) {
            asset.setName(dto.getName());
            asset.setLabel(dto.getName());
            pfAssetService.updateAsset(asset);
        }

        // Actualizar atributos
        dto.setAssetId(wellId);
        Map<String, Object> attrs = dto.toAttributeMap();
        if (!attrs.isEmpty()) {
            pfAttributeService.saveServerAttributes(wellId, attrs);
        }

        dto.setUpdatedTime(System.currentTimeMillis());
        return dto;
    }

    /**
     * Actualiza el estado del pozo
     */
    public void updateWellStatus(UUID wellId, WellStatus newStatus) {
        pfAttributeService.saveServerAttributes(wellId,
            Map.of(PfWellDto.ATTR_STATUS, newStatus.name()));
        log.info("Well {} status updated to {}", wellId, newStatus);
    }

    /**
     * Elimina un pozo
     */
    @Transactional
    public void deleteWell(UUID tenantId, UUID wellId) {
        log.warn("Deleting well: {}", wellId);

        // Eliminar relaciones primero
        pfHierarchyService.removeAllRelations(wellId);

        // Eliminar asset
        pfAssetService.deleteAsset(tenantId, wellId);
    }

    private PfWellDto buildWellDto(Asset asset) {
        PfWellDto dto = PfWellDto.builder()
            .assetId(asset.getId().getId())
            .tenantId(asset.getTenantId().getId())
            .name(asset.getName())
            .createdTime(asset.getCreatedTime())
            .build();

        // Cargar atributos
        List<AttributeKvEntry> attrs = pfAttributeService.getServerAttributes(asset.getId().getId());

        for (AttributeKvEntry attr : attrs) {
            switch (attr.getKey()) {
                case PfWellDto.ATTR_API_NUMBER:
                    dto.setApiNumber(attr.getValueAsString());
                    break;
                case PfWellDto.ATTR_STATUS:
                    dto.setStatus(WellStatus.valueOf(attr.getValueAsString()));
                    break;
                case PfWellDto.ATTR_LIFT_SYSTEM_TYPE:
                    dto.setLiftSystemType(LiftSystemType.valueOf(attr.getValueAsString()));
                    break;
                case PfWellDto.ATTR_LATITUDE:
                    attr.getDoubleValue().ifPresent(v -> dto.setLatitude(BigDecimal.valueOf(v)));
                    break;
                case PfWellDto.ATTR_LONGITUDE:
                    attr.getDoubleValue().ifPresent(v -> dto.setLongitude(BigDecimal.valueOf(v)));
                    break;
                case PfWellDto.ATTR_WELLPAD_ID:
                    dto.setWellpadId(UUID.fromString(attr.getValueAsString()));
                    break;
                // ... otros atributos
            }
        }

        return dto;
    }
}
```

### 5.4 PfAlarmService (Wrapper sobre TB Alarm System)

```java
/**
 * Servicio de alarmas usando ThingsBoard Alarm System.
 * NO usa tabla custom - usa la tabla 'alarm' de ThingsBoard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfAlarmService {

    private final AlarmService alarmService;  // TB AlarmService
    private final ObjectMapper objectMapper;

    /**
     * Crea o actualiza una alarma
     */
    public Alarm createAlarm(UUID tenantId, UUID assetId,
                            String alarmType, AlarmSeverity severity, String message) {

        Alarm alarm = new Alarm();
        alarm.setTenantId(TenantId.fromUUID(tenantId));
        alarm.setOriginator(new AssetId(assetId));
        alarm.setType(alarmType);
        alarm.setSeverity(mapSeverity(severity));
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarm.setStartTs(System.currentTimeMillis());

        // Detalles adicionales en JSON
        ObjectNode details = objectMapper.createObjectNode();
        details.put("message", message);
        details.put("module", "PF");
        alarm.setDetails(details);

        Alarm savedAlarm = alarmService.createOrUpdateAlarm(alarm);
        log.info("Alarm created: type={}, severity={}, asset={}",
                 alarmType, severity, assetId);

        return savedAlarm;
    }

    /**
     * Limpia una alarma
     */
    public void clearAlarm(UUID alarmId) {
        alarmService.clearAlarm(null, new AlarmId(alarmId),
                               System.currentTimeMillis(), null);
        log.info("Alarm cleared: {}", alarmId);
    }

    /**
     * Reconoce una alarma
     */
    public void acknowledgeAlarm(UUID alarmId) {
        alarmService.ackAlarm(null, new AlarmId(alarmId), System.currentTimeMillis());
        log.info("Alarm acknowledged: {}", alarmId);
    }

    /**
     * Obtiene alarmas activas para un asset
     */
    public List<AlarmInfo> getActiveAlarms(UUID tenantId, UUID assetId) {
        AlarmQuery query = AlarmQuery.builder()
            .affectedEntityId(new AssetId(assetId))
            .status(AlarmSearchStatus.ACTIVE)
            .build();

        PageLink pageLink = new PageLink(100);
        PageData<AlarmInfo> alarms = alarmService.findAlarms(
            TenantId.fromUUID(tenantId), query, pageLink);

        return alarms.getData();
    }

    /**
     * Obtiene alarmas activas por severidad
     */
    public List<AlarmInfo> getActiveAlarmsBySeverity(UUID tenantId,
                                                      List<AlarmSeverity> severities) {
        // Implementar filtrado por severidad
        return getActiveAlarms(tenantId, null).stream()
            .filter(a -> severities.contains(mapFromTbSeverity(a.getSeverity())))
            .collect(Collectors.toList());
    }

    private org.thingsboard.server.common.data.alarm.AlarmSeverity mapSeverity(AlarmSeverity severity) {
        return switch (severity) {
            case CRITICAL -> org.thingsboard.server.common.data.alarm.AlarmSeverity.CRITICAL;
            case HIGH -> org.thingsboard.server.common.data.alarm.AlarmSeverity.MAJOR;
            case MEDIUM -> org.thingsboard.server.common.data.alarm.AlarmSeverity.MINOR;
            case LOW -> org.thingsboard.server.common.data.alarm.AlarmSeverity.WARNING;
            default -> org.thingsboard.server.common.data.alarm.AlarmSeverity.INDETERMINATE;
        };
    }
}
```

---

## 6. APIs REST {#apis-rest}

### 6.1 Well Management API

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
  "assetId": "tb-asset-uuid",
  "tenantId": "tenant-uuid",
  "name": "WELL-ABC-123",
  "apiNumber": "42-123-45678",
  "wellpadId": "uuid-of-wellpad",
  "status": "PRODUCING",
  "liftSystemType": "ESP",
  "latitude": 10.12345,
  "longitude": -65.67890,
  "measuredDepthFt": 8500,
  "trueVerticalDepthFt": 8200,
  "firstProductionDate": "2020-01-15",
  "createdTime": 1704816000000
}
```

#### Get Well by ID
```http
GET /wells/{wellId}
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "assetId": "well-uuid",
  "tenantId": "tenant-uuid",
  "name": "WELL-ABC-123",
  "wellpadId": "wellpad-uuid",
  "status": "PRODUCING",
  "currentProductionBpd": 456.5,
  ...
}
```

#### Get Wells by Wellpad
```http
GET /wells?wellpadId={wellpadId}&page=0&size=20
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "content": [
    { "assetId": "...", "name": "WELL-001", ... },
    { "assetId": "...", "name": "WELL-002", ... }
  ],
  "totalElements": 12,
  "totalPages": 1,
  "number": 0
}
```

### 6.2 Telemetry API

La telemetría se accede mediante las APIs nativas de ThingsBoard o wrappers del módulo:

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
  }
}
```

#### Query Historical Telemetry
```http
GET /wells/{wellId}/telemetry?from=1704729600000&to=1704816000000&keys=frequency_hz,temperature_motor_f
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "entityId": "well-uuid",
  "data": [
    { "ts": 1704729600000, "frequency_hz": 52.0, "temperature_motor_f": 275 },
    { "ts": 1704729660000, "frequency_hz": 52.1, "temperature_motor_f": 276 },
    ...
  ]
}
```

### 6.3 Alarms API

Usa ThingsBoard Alarm API nativa o endpoints del módulo:

#### Get Active Alarms
```http
GET /alarms/active?entityId={wellId}&severity=CRITICAL,HIGH
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "data": [
    {
      "id": "alarm-uuid",
      "originator": { "id": "well-uuid", "entityType": "ASSET" },
      "type": "MOTOR_TEMPERATURE_HIGH",
      "severity": "CRITICAL",
      "status": "ACTIVE_UNACK",
      "startTs": 1704816000000,
      "details": {
        "message": "Motor temperature 295°F exceeds limit 280°F",
        "module": "PF"
      }
    }
  ]
}
```

---

## 7. Procesamiento de Telemetría {#telemetria}

### 7.1 Arquitectura de Telemetría

El módulo PF usa el sistema de telemetría nativo de ThingsBoard:

```
┌──────────┐     ┌──────────┐     ┌──────────────┐     ┌─────────────┐
│  Device  │────▶│   MQTT   │────▶│ TB Transport │────▶│ Rule Engine │
│ (PLC/RTU)│     │  Broker  │     │   Layer      │     │             │
└──────────┘     └──────────┘     └──────────────┘     └──────┬──────┘
                                                               │
                                                               ▼
                                                    ┌─────────────────────┐
                                                    │  PfDataQualityNode  │
                                                    │  (Rule Node Custom) │
                                                    └──────────┬──────────┘
                                                               │
                                    ┌──────────────────────────┼──────────────────────────┐
                                    ▼                          ▼                          ▼
                             ┌────────────┐           ┌─────────────────┐         ┌────────────┐
                             │   ts_kv    │           │ PfAlarmEvalNode │         │  WebSocket │
                             │  (native)  │           │  (Rule Node)    │         │    Push    │
                             └────────────┘           └────────┬────────┘         └────────────┘
                                                               │
                                                               ▼
                                                      ┌────────────────┐
                                                      │ TB Alarm System│
                                                      └────────────────┘
```

### 7.2 Almacenamiento

| Tabla ThingsBoard | Uso |
|-------------------|-----|
| `ts_kv` | Series temporales principales |
| `ts_kv_dictionary` | Diccionario de keys de telemetría |
| `ts_kv_latest` | Cache de últimos valores |

**NO se usa TimescaleDB custom** - ThingsBoard ya tiene optimizaciones para time-series.

### 7.3 PfDataQualityNode (Rule Node Custom)

```java
/**
 * Rule Node personalizado para validación de calidad de datos.
 * Se integra en el Rule Chain de ThingsBoard.
 */
@Slf4j
@RuleNode(
    type = ComponentType.FILTER,
    name = "PF Data Quality Validator",
    configClazz = PfDataQualityNodeConfiguration.class,
    nodeDescription = "Validates telemetry data quality for PF module",
    nodeDetails = "Checks range, rate of change, and completeness"
)
public class PfDataQualityNode implements TbNode {

    private PfDataQualityNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) {
        this.config = TbNodeUtils.convert(configuration, PfDataQualityNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            JsonNode data = mapper.readTree(msg.getData());

            double qualityScore = validateQuality(data);

            if (qualityScore >= config.getMinQualityThreshold()) {
                // Agregar quality_score al mensaje
                ObjectNode enrichedData = (ObjectNode) data;
                enrichedData.put("quality_score", qualityScore);

                TbMsg newMsg = TbMsg.transformMsg(msg, msg.getType(),
                                                  msg.getOriginator(),
                                                  msg.getMetaData(),
                                                  enrichedData.toString());
                ctx.tellNext(newMsg, "True");
            } else {
                log.warn("Low quality data rejected: score={}", qualityScore);
                ctx.tellNext(msg, "False");
            }
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }

    private double validateQuality(JsonNode data) {
        double score = 1.0;

        // Validación de rango
        score *= validateRange(data);

        // Validación de rate of change
        score *= validateRateOfChange(data);

        // Validación de completeness
        score *= validateCompleteness(data);

        return score;
    }

    // ... implementación de validaciones
}
```

### 7.4 Consulta de Telemetría

```java
/**
 * Servicio para consultar telemetría usando TB TelemetryService
 */
@Service
@RequiredArgsConstructor
public class PfTelemetryService {

    private final TimeseriesService timeseriesService;

    /**
     * Obtiene últimos valores de telemetría
     */
    public List<TsKvEntry> getLatestTelemetry(UUID assetId, List<String> keys) {
        try {
            return timeseriesService.findLatest(null, new AssetId(assetId), keys).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get latest telemetry", e);
        }
    }

    /**
     * Obtiene telemetría histórica
     */
    public List<TsKvEntry> getHistoricalTelemetry(UUID assetId, List<String> keys,
                                                   long startTs, long endTs) {
        try {
            List<ReadTsKvQuery> queries = keys.stream()
                .map(key -> new BaseReadTsKvQuery(key, startTs, endTs, 0, 10000, Aggregation.NONE))
                .collect(Collectors.toList());

            return timeseriesService.findAll(null, new AssetId(assetId), queries).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get historical telemetry", e);
        }
    }

    /**
     * Obtiene telemetría agregada
     */
    public List<TsKvEntry> getAggregatedTelemetry(UUID assetId, String key,
                                                   long startTs, long endTs,
                                                   Aggregation aggregation,
                                                   long interval) {
        try {
            ReadTsKvQuery query = new BaseReadTsKvQuery(key, startTs, endTs,
                                                        interval, 1000, aggregation);
            return timeseriesService.findAll(null, new AssetId(assetId),
                                            List.of(query)).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get aggregated telemetry", e);
        }
    }
}
```

---

## 8. Sistema de Alarmas {#alarmas}

### 8.1 Arquitectura de Alarmas

El módulo PF usa ThingsBoard Alarm System nativo:

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Telemetry     │────▶│  Asset Profile   │────▶│  TB Alarm       │
│   (ts_kv)       │     │  (Alarm Rules)   │     │  System         │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                          │
                              ┌────────────────────────────┼────────────────────────────┐
                              ▼                            ▼                            ▼
                       ┌────────────┐              ┌────────────┐              ┌────────────┐
                       │   alarm    │              │ WebSocket  │              │   Email/   │
                       │   table    │              │   Push     │              │   SMS      │
                       └────────────┘              └────────────┘              └────────────┘
```

### 8.2 Tipos de Alarmas

| Alarm Type | Description | Severity |
|------------|-------------|----------|
| `MOTOR_TEMPERATURE_HIGH` | Temperatura de motor alta | CRITICAL/MAJOR |
| `MOTOR_TEMPERATURE_LOW` | Temperatura de motor baja | MINOR |
| `CURRENT_HIGH` | Corriente alta | CRITICAL/MAJOR |
| `CURRENT_LOW` | Corriente baja (posible gas) | MAJOR |
| `PIP_LOW` | Presión de intake baja | CRITICAL |
| `VIBRATION_HIGH` | Vibración alta | CRITICAL |
| `COMMUNICATION_LOST` | Pérdida de comunicación | MAJOR |
| `DATA_QUALITY_LOW` | Calidad de datos baja | MINOR |

### 8.3 Configuración via Asset Profiles

Las alarmas se configuran en Asset Profiles de ThingsBoard:

```json
{
  "alarmRules": [
    {
      "id": "motor_temp_high",
      "alarmType": "MOTOR_TEMPERATURE_HIGH",
      "createRules": {
        "CRITICAL": {
          "condition": {
            "condition": [{
              "key": {"type": "TIME_SERIES", "key": "temperature_motor_f"},
              "valueType": "NUMERIC",
              "predicate": {
                "type": "NUMERIC",
                "operation": "GREATER",
                "value": {"defaultValue": 280}
              }
            }]
          },
          "alarmDetails": "Motor temperature ${temperature_motor_f}°F exceeds critical limit 280°F"
        },
        "MAJOR": {
          "condition": {
            "condition": [{
              "key": {"type": "TIME_SERIES", "key": "temperature_motor_f"},
              "valueType": "NUMERIC",
              "predicate": {
                "type": "NUMERIC",
                "operation": "GREATER",
                "value": {"defaultValue": 260}
              }
            }]
          },
          "alarmDetails": "Motor temperature ${temperature_motor_f}°F exceeds warning limit 260°F"
        }
      },
      "clearRule": {
        "condition": {
          "condition": [{
            "key": {"type": "TIME_SERIES", "key": "temperature_motor_f"},
            "valueType": "NUMERIC",
            "predicate": {
              "type": "NUMERIC",
              "operation": "LESS",
              "value": {"defaultValue": 250}
            }
          }]
        }
      }
    },
    {
      "id": "pip_low",
      "alarmType": "PIP_LOW",
      "createRules": {
        "CRITICAL": {
          "condition": {
            "condition": [{
              "key": {"type": "TIME_SERIES", "key": "pip_psi"},
              "valueType": "NUMERIC",
              "predicate": {
                "type": "NUMERIC",
                "operation": "LESS",
                "value": {"defaultValue": 150}
              }
            }]
          },
          "alarmDetails": "Pump Intake Pressure ${pip_psi} psi below minimum 150 psi - Gas lock risk"
        }
      },
      "clearRule": {
        "condition": {
          "condition": [{
            "key": {"type": "TIME_SERIES", "key": "pip_psi"},
            "valueType": "NUMERIC",
            "predicate": {
              "type": "NUMERIC",
              "operation": "GREATER",
              "value": {"defaultValue": 180}
            }
          }]
        }
      }
    }
  ]
}
```

### 8.4 Workflow de Alarmas

```
┌─────────────┐
│ Telemetría  │
│  Recibida   │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ Asset Profile   │
│ Alarm Rules     │
│ (automático TB) │
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
             │ TB crea/     │
             │ actualiza    │
             │ Alarm        │
             └──────┬───────┘
                    │
              ┌─────┴─────┐
              │           │
              ▼           ▼
        ┌─────────┐ ┌─────────────┐
        │ alarm   │ │ Notification│
        │ table   │ │ Rule Chain  │
        └─────────┘ └─────────────┘
```

---

## 9. Frontend Components {#frontend}

### 9.1 Component Structure

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

### 9.2 Well Dashboard Component

```typescript
@Component({
  selector: 'tb-pf-well-detail',
  templateUrl: './well-detail.component.html',
  styleUrls: ['./well-detail.component.scss']
})
export class PfWellDetailComponent implements OnInit, OnDestroy {

  wellId: string;
  well: PfWellDto;
  latestTelemetry: Map<string, TsValue>;
  activeAlarms: AlarmInfo[] = [];

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private pfWellService: PfWellService,
    private telemetryWsService: TelemetryWebsocketService,
    private alarmService: AlarmService
  ) {}

  ngOnInit() {
    this.wellId = this.route.snapshot.params['wellId'];
    this.loadWellData();
    this.subscribeToTelemetry();
    this.subscribeToAlarms();
  }

  loadWellData() {
    this.pfWellService.getWell(this.wellId).subscribe(well => {
      this.well = well;
    });
  }

  subscribeToTelemetry() {
    // Usar TB TelemetryWebsocketService para real-time
    const entityId = { entityType: EntityType.ASSET, id: this.wellId };

    this.telemetryWsService.subscribe(entityId,
      ['frequency_hz', 'current_amps', 'temperature_motor_f', 'pip_psi', 'vibration_g']
    ).pipe(
      takeUntil(this.destroy$)
    ).subscribe(data => {
      this.latestTelemetry = data;
    });
  }

  subscribeToAlarms() {
    // Usar TB AlarmService para alarmas
    this.alarmService.getAlarms(
      { entityType: EntityType.ASSET, id: this.wellId },
      { status: AlarmSearchStatus.ACTIVE }
    ).pipe(
      takeUntil(this.destroy$)
    ).subscribe(alarms => {
      this.activeAlarms = alarms.data;
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

---

**Documento actualizado para usar arquitectura ThingsBoard Core.**
**Versión 2.0 - Alineado con patrones de CT y RV modules.**
