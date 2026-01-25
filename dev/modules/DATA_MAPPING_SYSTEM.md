# Sistema de Mapeo de Datos (Data Mapping System)

## Visión General

El Sistema de Mapeo de Datos gestiona la distribución y transformación de datos desde fuentes externas hacia los gemelos digitales en ThingsBoard. Permite mapear variables de sistemas SCADA, PLCs, sensores y otras fuentes a múltiples assets y devices, aplicando transformaciones y validaciones.

## Problemática

En un gemelo digital complejo (ej: Unidad CT con 10 assets relacionados), los datos llegan de una fuente única pero deben distribuirse a múltiples entidades:

```
Fuente de Datos SCADA
├── pressure_hydraulic_1 → Asset "Sistema Hidráulico" → telemetry "pressure"
├── pressure_hydraulic_2 → Asset "Sistema Hidráulico" → telemetry "pressure_backup"
├── speed_injection → Asset "Sistema Inyección" → telemetry "speed"
├── tension_reel → Asset "Reel" → telemetry "tension"
├── depth_current → Asset "Sistema Inyección" → telemetry "depth"
└── temp_hydraulic → Asset "Sistema Hidráulico" → telemetry "temperature"
```

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    Fuentes de Datos                          │
│  - MQTT                                                      │
│  - HTTP/REST                                                 │
│  - OPC-UA                                                    │
│  - Modbus                                                    │
│  - Bases de Datos                                            │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Data Ingestion Layer                      │
│  - Protocol Adapters                                         │
│  - Data Normalizer                                           │
│  - Buffer & Queue                                            │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Mapping Engine (Rule Chain)               │
│  - Mapping Resolver                                          │
│  - Data Transformer                                          │
│  - Validator                                                 │
│  - Router                                                    │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    ThingsBoard Entities                      │
│  - Assets (múltiples)                                        │
│  - Devices (múltiples)                                       │
│  - Telemetry & Attributes                                    │
└─────────────────────────────────────────────────────────────┘
```

## Modelo de Datos

### Tabla: nx_data_sources

Define las fuentes de datos disponibles.

```sql
CREATE TABLE nx_data_sources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    module_name VARCHAR(100) NOT NULL,
    source_type VARCHAR(50) NOT NULL, -- 'MQTT', 'HTTP', 'OPC_UA', 'MODBUS', 'DATABASE'
    protocol_config JSONB NOT NULL, -- Configuración específica del protocolo
    is_active BOOLEAN DEFAULT TRUE,
    connection_status VARCHAR(20), -- 'CONNECTED', 'DISCONNECTED', 'ERROR'
    last_data_time BIGINT,
    metadata JSONB,
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    CONSTRAINT fk_ds_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT uk_data_source_name UNIQUE (tenant_id, module_name, name)
);

CREATE INDEX idx_nx_data_sources_tenant ON nx_data_sources(tenant_id);
CREATE INDEX idx_nx_data_sources_module ON nx_data_sources(module_name);
CREATE INDEX idx_nx_data_sources_active ON nx_data_sources(is_active);
```

### Tabla: nx_mapping_configurations

Configuraciones de mapeo de datos.

```sql
CREATE TABLE nx_mapping_configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    module_name VARCHAR(100) NOT NULL,
    data_source_id UUID NOT NULL,
    template_instance_id UUID, -- Referencia a instancia de template (opcional)
    mapping_type VARCHAR(50) NOT NULL, -- 'TELEMETRY', 'ATTRIBUTE', 'BOTH'
    is_active BOOLEAN DEFAULT TRUE,
    version VARCHAR(20) DEFAULT '1.0.0',
    metadata JSONB,
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    CONSTRAINT fk_mc_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_mc_data_source FOREIGN KEY (data_source_id) REFERENCES nx_data_sources(id) ON DELETE CASCADE,
    CONSTRAINT fk_mc_template_instance FOREIGN KEY (template_instance_id) REFERENCES nx_template_instances(id) ON DELETE CASCADE
);

CREATE INDEX idx_nx_mapping_configs_tenant ON nx_mapping_configurations(tenant_id);
CREATE INDEX idx_nx_mapping_configs_source ON nx_mapping_configurations(data_source_id);
CREATE INDEX idx_nx_mapping_configs_instance ON nx_mapping_configurations(template_instance_id);
```

### Tabla: nx_mapping_rules

Reglas individuales de mapeo.

```sql
CREATE TABLE nx_mapping_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mapping_config_id UUID NOT NULL,
    source_field VARCHAR(255) NOT NULL, -- Campo en la fuente de datos
    target_entity_id UUID NOT NULL, -- Asset o Device destino
    target_entity_type VARCHAR(20) NOT NULL, -- 'ASSET', 'DEVICE'
    target_field VARCHAR(255) NOT NULL, -- Telemetría o atributo destino
    target_scope VARCHAR(20), -- Para atributos: 'SERVER_SCOPE', 'SHARED_SCOPE', 'CLIENT_SCOPE'
    data_type VARCHAR(50) NOT NULL, -- 'STRING', 'LONG', 'DOUBLE', 'BOOLEAN', 'JSON'
    transformation_script TEXT, -- Script de transformación (JavaScript)
    validation_rules JSONB, -- Reglas de validación
    default_value TEXT, -- Valor por defecto si falta el dato
    is_required BOOLEAN DEFAULT FALSE,
    order_index INTEGER DEFAULT 0, -- Orden de procesamiento
    metadata JSONB,
    CONSTRAINT fk_mr_mapping_config FOREIGN KEY (mapping_config_id) REFERENCES nx_mapping_configurations(id) ON DELETE CASCADE
);

CREATE INDEX idx_nx_mapping_rules_config ON nx_mapping_rules(mapping_config_id);
CREATE INDEX idx_nx_mapping_rules_entity ON nx_mapping_rules(target_entity_id);
CREATE INDEX idx_nx_mapping_rules_source ON nx_mapping_rules(source_field);
```

### Tabla: nx_transformation_functions

Funciones de transformación reutilizables.

```sql
CREATE TABLE nx_transformation_functions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    module_name VARCHAR(100),
    function_type VARCHAR(50) NOT NULL, -- 'JAVASCRIPT', 'EXPRESSION', 'LOOKUP'
    function_body TEXT NOT NULL,
    input_params JSONB, -- Definición de parámetros de entrada
    output_type VARCHAR(50), -- Tipo de dato de salida
    is_system_function BOOLEAN DEFAULT FALSE,
    metadata JSONB,
    created_time BIGINT NOT NULL,
    CONSTRAINT fk_tf_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT uk_transformation_function UNIQUE (tenant_id, name)
);

CREATE INDEX idx_nx_transformation_functions_tenant ON nx_transformation_functions(tenant_id);
CREATE INDEX idx_nx_transformation_functions_module ON nx_transformation_functions(module_name);
```

### Tabla: nx_mapping_logs

Logs de procesamiento de datos (opcional, para debugging).

```sql
CREATE TABLE nx_mapping_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    mapping_config_id UUID NOT NULL,
    data_source_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'SUCCESS', 'ERROR', 'PARTIAL'
    records_processed INTEGER DEFAULT 0,
    records_failed INTEGER DEFAULT 0,
    error_details TEXT,
    processing_time_ms BIGINT,
    timestamp BIGINT NOT NULL,
    CONSTRAINT fk_ml_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ml_mapping_config FOREIGN KEY (mapping_config_id) REFERENCES nx_mapping_configurations(id) ON DELETE CASCADE
);

CREATE INDEX idx_nx_mapping_logs_tenant ON nx_mapping_logs(tenant_id);
CREATE INDEX idx_nx_mapping_logs_config ON nx_mapping_logs(mapping_config_id);
CREATE INDEX idx_nx_mapping_logs_timestamp ON nx_mapping_logs(timestamp);

-- Partition por tiempo para optimizar
CREATE TABLE nx_mapping_logs_partitioned (LIKE nx_mapping_logs INCLUDING ALL)
PARTITION BY RANGE (timestamp);
```

## Modelo Java

### Entity: DataSource

```java
@Entity
@Table(name = "nx_data_sources")
public class DataSource {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "module_name", nullable = false)
    private String moduleName;
    
    @Column(name = "source_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DataSourceType sourceType;
    
    @Type(type = "jsonb")
    @Column(name = "protocol_config", columnDefinition = "jsonb", nullable = false)
    private JsonNode protocolConfig;
    
    @Column(name = "is_active")
    private boolean active;
    
    @Column(name = "connection_status")
    @Enumerated(EnumType.STRING)
    private ConnectionStatus connectionStatus;
    
    @Column(name = "last_data_time")
    private Long lastDataTime;
    
    @Type(type = "jsonb")
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;
    
    @Column(name = "created_time", nullable = false)
    private long createdTime;
    
    @Column(name = "updated_time")
    private Long updatedTime;
    
    // Getters y Setters
}
```

### Entity: MappingConfiguration

```java
@Entity
@Table(name = "nx_mapping_configurations")
public class MappingConfiguration {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "module_name", nullable = false)
    private String moduleName;
    
    @Column(name = "data_source_id", nullable = false)
    private UUID dataSourceId;
    
    @Column(name = "template_instance_id")
    private UUID templateInstanceId;
    
    @Column(name = "mapping_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MappingType mappingType;
    
    @Column(name = "is_active")
    private boolean active;
    
    @Column(name = "version")
    private String version;
    
    @Type(type = "jsonb")
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;
    
    @Column(name = "created_time", nullable = false)
    private long createdTime;
    
    @Column(name = "updated_time")
    private Long updatedTime;
    
    // Getters y Setters
}
```

### Service: DataMappingService

```java
@Service
public class DataMappingService {
    
    @Autowired
    private MappingConfigurationRepository mappingConfigRepo;
    
    @Autowired
    private MappingRuleRepository mappingRuleRepo;
    
    @Autowired
    private TelemetryService telemetryService;
    
    @Autowired
    private AttributesService attributesService;
    
    @Autowired
    private TransformationService transformationService;
    
    /**
     * Procesa datos desde una fuente y los distribuye según el mapeo
     */
    @Transactional
    public MappingResult processData(UUID dataSourceId, Map<String, Object> sourceData) {
        
        // Obtener configuraciones de mapeo activas para esta fuente
        List<MappingConfiguration> configs = mappingConfigRepo
            .findByDataSourceIdAndActive(dataSourceId, true);
        
        if (configs.isEmpty()) {
            log.warn("No active mapping configurations found for data source: {}", dataSourceId);
            return MappingResult.noMappings();
        }
        
        MappingResult result = new MappingResult();
        
        for (MappingConfiguration config : configs) {
            try {
                processMappingConfiguration(config, sourceData, result);
            } catch (Exception e) {
                log.error("Error processing mapping config {}: {}", config.getId(), e.getMessage());
                result.addError(config.getId(), e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Procesa una configuración de mapeo específica
     */
    private void processMappingConfiguration(
            MappingConfiguration config,
            Map<String, Object> sourceData,
            MappingResult result) {
        
        // Obtener reglas de mapeo ordenadas
        List<MappingRule> rules = mappingRuleRepo
            .findByMappingConfigIdOrderByOrderIndex(config.getId());
        
        // Agrupar reglas por entidad destino para optimizar escrituras
        Map<EntityId, List<MappingRule>> rulesByEntity = rules.stream()
            .collect(Collectors.groupingBy(rule -> 
                new EntityId(rule.getTargetEntityType(), rule.getTargetEntityId())
            ));
        
        // Procesar cada entidad
        for (Map.Entry<EntityId, List<MappingRule>> entry : rulesByEntity.entrySet()) {
            EntityId entityId = entry.getKey();
            List<MappingRule> entityRules = entry.getValue();
            
            processEntityMappings(entityId, entityRules, sourceData, result);
        }
    }
    
    /**
     * Procesa mapeos para una entidad específica
     */
    private void processEntityMappings(
            EntityId entityId,
            List<MappingRule> rules,
            Map<String, Object> sourceData,
            MappingResult result) {
        
        List<AttributeKvEntry> attributes = new ArrayList<>();
        List<TsKvEntry> telemetries = new ArrayList<>();
        long timestamp = System.currentTimeMillis();
        
        for (MappingRule rule : rules) {
            try {
                // Obtener valor de la fuente
                Object sourceValue = getSourceValue(sourceData, rule.getSourceField());
                
                // Validar si es requerido
                if (sourceValue == null) {
                    if (rule.isRequired()) {
                        result.addError(entityId, "Required field missing: " + rule.getSourceField());
                        continue;
                    } else if (rule.getDefaultValue() != null) {
                        sourceValue = rule.getDefaultValue();
                    } else {
                        continue; // Skip optional missing fields
                    }
                }
                
                // Aplicar transformación si existe
                if (rule.getTransformationScript() != null) {
                    sourceValue = transformationService.transform(
                        sourceValue, 
                        rule.getTransformationScript()
                    );
                }
                
                // Validar valor
                if (rule.getValidationRules() != null) {
                    ValidationResult validation = validateValue(sourceValue, rule.getValidationRules());
                    if (!validation.isValid()) {
                        result.addError(entityId, "Validation failed: " + validation.getMessage());
                        continue;
                    }
                }
                
                // Convertir a tipo de dato correcto
                Object typedValue = convertToDataType(sourceValue, rule.getDataType());
                
                // Agregar a lista de atributos o telemetrías
                if (rule.getTargetScope() != null) {
                    // Es un atributo
                    attributes.add(new AttributeKvEntry(
                        timestamp,
                        createKvEntry(rule.getTargetField(), typedValue, rule.getDataType())
                    ));
                } else {
                    // Es telemetría
                    telemetries.add(new BasicTsKvEntry(
                        timestamp,
                        createKvEntry(rule.getTargetField(), typedValue, rule.getDataType())
                    ));
                }
                
                result.addSuccess(entityId, rule.getSourceField());
                
            } catch (Exception e) {
                log.error("Error processing rule {} for entity {}: {}", 
                         rule.getId(), entityId, e.getMessage());
                result.addError(entityId, "Rule " + rule.getSourceField() + ": " + e.getMessage());
            }
        }
        
        // Guardar atributos
        if (!attributes.isEmpty()) {
            attributesService.save(entityId, rules.get(0).getTargetScope(), attributes);
        }
        
        // Guardar telemetrías
        if (!telemetries.isEmpty()) {
            telemetryService.saveAndNotify(
                TenantId.fromUUID(getCurrentTenantId()),
                entityId,
                telemetries,
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        log.debug("Telemetry saved for entity: {}", entityId);
                    }
                    
                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Failed to save telemetry for entity: {}", entityId, t);
                    }
                }
            );
        }
    }
    
    /**
     * Crea una configuración de mapeo desde una instancia de template
     */
    @Transactional
    public MappingConfiguration createMappingFromTemplate(
            UUID templateInstanceId,
            UUID dataSourceId,
            Map<String, String> fieldMappings) {
        
        TemplateInstance instance = templateInstanceRepository.findById(templateInstanceId)
            .orElseThrow(() -> new NotFoundException("Template instance not found"));
        
        // Crear configuración de mapeo
        MappingConfiguration config = new MappingConfiguration();
        config.setId(UUID.randomUUID());
        config.setTenantId(instance.getTenantId());
        config.setName("Mapping for " + instance.getInstanceName());
        config.setDataSourceId(dataSourceId);
        config.setTemplateInstanceId(templateInstanceId);
        config.setMappingType(MappingType.BOTH);
        config.setActive(true);
        config.setCreatedTime(System.currentTimeMillis());
        
        config = mappingConfigRepo.save(config);
        
        // Obtener estructura del template
        NexusTemplate template = templateRepository.findById(instance.getTemplateId())
            .orElseThrow(() -> new NotFoundException("Template not found"));
        
        List<TemplateNode> nodes = templateNodeRepository.findByTemplateId(template.getId());
        
        // Obtener mapeo de node_key -> entity_id desde metadata de instancia
        Map<String, UUID> nodeToEntityMap = extractNodeToEntityMap(instance.getMetadata());
        
        int orderIndex = 0;
        
        // Crear reglas de mapeo basadas en fieldMappings
        for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
            String sourceField = mapping.getKey();
            String targetSpec = mapping.getValue(); // Formato: "node_key.field_name"
            
            String[] parts = targetSpec.split("\\.");
            if (parts.length != 2) {
                log.warn("Invalid target spec: {}", targetSpec);
                continue;
            }
            
            String nodeKey = parts[0];
            String targetField = parts[1];
            
            UUID entityId = nodeToEntityMap.get(nodeKey);
            if (entityId == null) {
                log.warn("Entity not found for node key: {}", nodeKey);
                continue;
            }
            
            // Determinar tipo de entidad y si es atributo o telemetría
            TemplateNode node = nodes.stream()
                .filter(n -> n.getNodeKey().equals(nodeKey))
                .findFirst()
                .orElse(null);
            
            if (node == null) continue;
            
            // Buscar si el campo es telemetría o atributo en el template
            List<TemplateAttribute> attributes = templateAttributeRepository
                .findByTemplateIdAndNodeKey(template.getId(), nodeKey);
            
            List<TemplateTelemetry> telemetries = templateTelemetryRepository
                .findByTemplateIdAndNodeKey(template.getId(), nodeKey);
            
            boolean isTelemetry = telemetries.stream()
                .anyMatch(t -> t.getTelemetryKey().equals(targetField));
            
            TemplateAttribute attribute = attributes.stream()
                .filter(a -> a.getAttributeKey().equals(targetField))
                .findFirst()
                .orElse(null);
            
            MappingRule rule = new MappingRule();
            rule.setId(UUID.randomUUID());
            rule.setMappingConfigId(config.getId());
            rule.setSourceField(sourceField);
            rule.setTargetEntityId(entityId);
            rule.setTargetEntityType(node.getNodeType());
            rule.setTargetField(targetField);
            rule.setOrderIndex(orderIndex++);
            
            if (isTelemetry) {
                TemplateTelemetry telemetry = telemetries.stream()
                    .filter(t -> t.getTelemetryKey().equals(targetField))
                    .findFirst()
                    .orElse(null);
                
                if (telemetry != null) {
                    rule.setDataType(telemetry.getTelemetryType());
                }
            } else if (attribute != null) {
                rule.setTargetScope(attribute.getAttributeScope());
                rule.setDataType(attribute.getAttributeType());
                rule.setRequired(attribute.isRequired());
            }
            
            mappingRuleRepo.save(rule);
        }
        
        return config;
    }
}
```

## Rule Chain para Procesamiento

ThingsBoard Rule Chain que procesa datos y aplica mapeos:

```json
{
  "name": "Nexus Data Mapping Chain",
  "nodes": [
    {
      "type": "org.thingsboard.rule.engine.filter.TbMsgTypeFilter",
      "name": "Filter POST_TELEMETRY",
      "configuration": {
        "messageTypes": ["POST_TELEMETRY_REQUEST"]
      }
    },
    {
      "type": "org.thingsboard.rule.engine.transform.TbTransformMsgNode",
      "name": "Extract Data Source ID",
      "configuration": {
        "jsScript": "var dataSourceId = metadata.dataSourceId;\nif (!dataSourceId) {\n  return {msg: msg, metadata: metadata, msgType: msgType};\n}\nmetadata.processedDataSourceId = dataSourceId;\nreturn {msg: msg, metadata: metadata, msgType: msgType};"
      }
    },
    {
      "type": "org.thingsboard.rule.engine.rest.TbRestApiCallNode",
      "name": "Call Mapping Service",
      "configuration": {
        "restEndpointUrlPattern": "http://localhost:8080/api/nexus/data-mapping/process/${dataSourceId}",
        "requestMethod": "POST",
        "headers": {
          "Content-Type": "application/json"
        },
        "useSimpleClientHttpFactory": false,
        "readTimeoutMs": 5000
      }
    },
    {
      "type": "org.thingsboard.rule.engine.action.TbLogNode",
      "name": "Log Success",
      "configuration": {
        "jsScript": "return 'Mapping processed successfully for data source: ' + metadata.dataSourceId;"
      }
    },
    {
      "type": "org.thingsboard.rule.engine.action.TbLogNode",
      "name": "Log Error",
      "configuration": {
        "jsScript": "return 'Mapping processing failed: ' + msg;"
      }
    }
  ],
  "connections": [
    {
      "fromIndex": 0,
      "toIndex": 1,
      "type": "True"
    },
    {
      "fromIndex": 1,
      "toIndex": 2,
      "type": "Success"
    },
    {
      "fromIndex": 2,
      "toIndex": 3,
      "type": "Success"
    },
    {
      "fromIndex": 2,
      "toIndex": 4,
      "type": "Failure"
    }
  ]
}
```

## API REST

### Endpoints

```java
@RestController
@RequestMapping("/api/nexus/data-mapping")
public class DataMappingController {
    
    @Autowired
    private DataMappingService mappingService;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    /**
     * POST /api/nexus/data-mapping/sources
     * Crear fuente de datos
     */
    @PostMapping("/sources")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public DataSource createDataSource(@RequestBody DataSourceRequest request) {
        return dataSourceService.createDataSource(request);
    }
    
    /**
     * GET /api/nexus/data-mapping/sources
     * Listar fuentes de datos
     */
    @GetMapping("/sources")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<DataSource> listDataSources(
            @RequestParam(required = false) String moduleName,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") int page) {
        
        UUID tenantId = getCurrentUser().getTenantId().getId();
        PageLink pageLink = new PageLink(pageSize, page);
        
        return dataSourceService.findByTenantIdAndModule(tenantId, moduleName, pageLink);
    }
    
    /**
     * POST /api/nexus/data-mapping/configurations
     * Crear configuración de mapeo
     */
    @PostMapping("/configurations")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public MappingConfiguration createMappingConfiguration(
            @RequestBody MappingConfigurationRequest request) {
        return mappingService.createMappingConfiguration(request);
    }
    
    /**
     * POST /api/nexus/data-mapping/configurations/from-template
     * Crear mapeo desde template
     */
    @PostMapping("/configurations/from-template")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public MappingConfiguration createMappingFromTemplate(
            @RequestBody MappingFromTemplateRequest request) {
        return mappingService.createMappingFromTemplate(
            request.getTemplateInstanceId(),
            request.getDataSourceId(),
            request.getFieldMappings()
        );
    }
    
    /**
     * POST /api/nexus/data-mapping/process/{dataSourceId}
     * Procesar datos (llamado desde Rule Chain)
     */
    @PostMapping("/process/{dataSourceId}")
    public MappingResult processData(
            @PathVariable UUID dataSourceId,
            @RequestBody Map<String, Object> sourceData) {
        return mappingService.processData(dataSourceId, sourceData);
    }
    
    /**
     * GET /api/nexus/data-mapping/configurations/{configId}/preview
     * Preview de cómo se mapearán los datos
     */
    @PostMapping("/configurations/{configId}/preview")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public MappingPreview previewMapping(
            @PathVariable UUID configId,
            @RequestBody Map<String, Object> sampleData) {
        return mappingService.previewMapping(configId, sampleData);
    }
}
```

## Frontend (Angular)

### Component: Data Mapping Editor

```typescript
@Component({
  selector: 'tb-data-mapping-editor',
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Editor de Mapeo de Datos</mat-card-title>
      </mat-card-header>
      
      <mat-card-content>
        <!-- Selección de Fuente de Datos -->
        <mat-form-field>
          <mat-label>Fuente de Datos</mat-label>
          <mat-select [(ngModel)]="selectedDataSource" (selectionChange)="onDataSourceChange()">
            <mat-option *ngFor="let ds of dataSources" [value]="ds">
              {{ds.name}} ({{ds.sourceType}})
            </mat-option>
          </mat-select>
        </mat-form-field>
        
        <!-- Tabla de Mapeo -->
        <table mat-table [dataSource]="mappingRules" class="mapping-table">
          
          <!-- Campo Origen -->
          <ng-container matColumnDef="sourceField">
            <th mat-header-cell *matHeaderCellDef>Campo Origen</th>
            <td mat-cell *matCellDef="let rule">
              <mat-form-field>
                <input matInput [(ngModel)]="rule.sourceField" 
                       placeholder="ej: pressure_1">
              </mat-form-field>
            </td>
          </ng-container>
          
          <!-- Entidad Destino -->
          <ng-container matColumnDef="targetEntity">
            <th mat-header-cell *matHeaderCellDef>Entidad Destino</th>
            <td mat-cell *matCellDef="let rule">
              <tb-entity-select 
                [(ngModel)]="rule.targetEntityId"
                [entityTypes]="['ASSET', 'DEVICE']">
              </tb-entity-select>
            </td>
          </ng-container>
          
          <!-- Campo Destino -->
          <ng-container matColumnDef="targetField">
            <th mat-header-cell *matHeaderCellDef>Campo Destino</th>
            <td mat-cell *matCellDef="let rule">
              <mat-form-field>
                <input matInput [(ngModel)]="rule.targetField" 
                       placeholder="ej: pressure">
              </mat-form-field>
            </td>
          </ng-container>
          
          <!-- Transformación -->
          <ng-container matColumnDef="transformation">
            <th mat-header-cell *matHeaderCellDef>Transformación</th>
            <td mat-cell *matCellDef="let rule">
              <button mat-icon-button (click)="editTransformation(rule)">
                <mat-icon>code</mat-icon>
              </button>
            </td>
          </ng-container>
          
          <!-- Acciones -->
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Acciones</th>
            <td mat-cell *matCellDef="let rule; let i = index">
              <button mat-icon-button (click)="removeRule(i)">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>
          
          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
        
        <button mat-button (click)="addRule()">
          <mat-icon>add</mat-icon> Agregar Regla
        </button>
      </mat-card-content>
      
      <mat-card-actions>
        <button mat-raised-button color="primary" (click)="save()">Guardar</button>
        <button mat-button (click)="preview()">Vista Previa</button>
      </mat-card-actions>
    </mat-card>
  `
})
export class DataMappingEditorComponent implements OnInit {
  
  dataSources: DataSource[] = [];
  selectedDataSource: DataSource;
  mappingRules: MappingRule[] = [];
  
  displayedColumns = ['sourceField', 'targetEntity', 'targetField', 'transformation', 'actions'];
  
  constructor(
    private mappingService: DataMappingService,
    private dataSourceService: DataSourceService
  ) {}
  
  ngOnInit() {
    this.loadDataSources();
  }
  
  loadDataSources() {
    this.dataSourceService.list().subscribe(data => {
      this.dataSources = data.data;
    });
  }
  
  addRule() {
    this.mappingRules.push({
      sourceField: '',
      targetEntityId: null,
      targetField: '',
      transformationScript: null
    } as MappingRule);
  }
  
  removeRule(index: number) {
    this.mappingRules.splice(index, 1);
  }
  
  save() {
    const config: MappingConfigurationRequest = {
      name: `Mapping for ${this.selectedDataSource.name}`,
      dataSourceId: this.selectedDataSource.id,
      rules: this.mappingRules
    };
    
    this.mappingService.createConfiguration(config).subscribe(result => {
      // Mostrar notificación de éxito
      console.log('Mapping saved:', result);
    });
  }
  
  preview() {
    // Abrir dialog con vista previa
  }
}
```

## Ejemplo Completo: Unidad CT

### Configuración de Fuente MQTT

```json
{
  "name": "CT Unit 001 SCADA",
  "sourceType": "MQTT",
  "moduleName": "coiled-tubing",
  "protocolConfig": {
    "brokerUrl": "tcp://scada.oilfield.com:1883",
    "topic": "ct/unit001/telemetry",
    "username": "nexus",
    "password": "***",
    "qos": 1
  }
}
```

### Datos de Ejemplo MQTT

```json
{
  "timestamp": 1706112000000,
  "hyd_pressure_1": 4500.5,
  "hyd_pressure_2": 4505.2,
  "hyd_temp": 65.3,
  "inj_speed": 15.2,
  "inj_tension": 25000,
  "inj_depth": 2456.8,
  "reel_weight": 15000,
  "reel_length_used": 2450,
  "control_status": "RUNNING",
  "control_mode": "AUTO"
}
```

### Configuración de Mapeo

```json
{
  "dataSourceId": "uuid-of-data-source",
  "templateInstanceId": "uuid-of-ct-unit-instance",
  "fieldMappings": {
    "hyd_pressure_1": "hydraulic_system.pressure",
    "hyd_pressure_2": "hydraulic_system.pressure_backup",
    "hyd_temp": "hydraulic_system.temperature",
    "inj_speed": "injection_system.speed",
    "inj_tension": "injection_system.tension",
    "inj_depth": "injection_system.depth",
    "reel_weight": "reel.weight",
    "reel_length_used": "reel.length_used",
    "control_status": "control_system.status",
    "control_mode": "control_system.mode"
  }
}
```

## Transformaciones Comunes

### Conversión de Unidades

```javascript
// PSI a Bar
function psiToBar(value) {
  return value * 0.0689476;
}

// Fahrenheit a Celsius
function fToC(value) {
  return (value - 32) * 5/9;
}
```

### Cálculos Derivados

```javascript
// Calcular fatiga basado en tensión y ciclos
function calculateFatigue(tension, cycles) {
  const maxTension = 30000; // kN
  const fatigueRatio = tension / maxTension;
  return cycles * fatigueRatio;
}
```

## Ventajas del Sistema

1. **Flexibilidad**: Mapear cualquier fuente a cualquier gemelo digital
2. **Reutilización**: Transformaciones y validaciones reutilizables
3. **Trazabilidad**: Logs de procesamiento para debugging
4. **Escalabilidad**: Procesa múltiples fuentes en paralelo
5. **Mantenibilidad**: Configuraciones independientes por módulo

## Próximos Pasos

1. Implementar esquema de base de datos
2. Desarrollar servicios backend
3. Crear Rule Chain de procesamiento
4. Desarrollar componentes frontend
5. Crear configuraciones para módulo CT
6. Pruebas con datos reales
