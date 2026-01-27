# Sistema de Plantillas (Template System) - Especificación Completa

## 📋 Visión General

El Sistema de Plantillas es un **componente core transversal** de la plataforma Nexus que permite crear, gestionar y utilizar plantillas para la creación automatizada de jerarquías de assets en ThingsBoard.

### Características Principales

✅ **CRUD Completo de Plantillas** - Interfaz visual para crear/editar/eliminar plantillas  
✅ **Multi-Módulo** - Soporta plantillas para CT, Well Testing, y futuros módulos  
✅ **Multi-Tipo** - Cada módulo puede tener múltiples tipos (Units, Reels, Wells, etc.)  
✅ **Jerarquías Complejas** - Define múltiples assets con relaciones padre-hijo  
✅ **Atributos Tipados** - Especifica tipo de dato para cada atributo (string, number, boolean, date)  
✅ **Telemetrías Configurables** - Define qué telemetrías tendrá cada asset  
✅ **Relaciones Tipadas** - Define tipo de relación entre assets (Contains, Manages, Uses, etc.)  
✅ **Variables de Plantilla** - Usa `{{variable}}` para valores dinámicos  
✅ **Versionado** - Mantiene historial de versiones de plantillas  
✅ **Validación** - Valida estructura antes de guardar  

---

## 🗄️ Modelo de Datos

### Tabla: `template_definitions`

```sql
CREATE TABLE template_definitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_code VARCHAR(100) UNIQUE NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Clasificación
    module_code VARCHAR(50) NOT NULL,  -- 'CT', 'WELL_TESTING', etc.
    entity_type VARCHAR(50) NOT NULL,  -- 'UNIT', 'REEL', 'WELL', etc.
    category VARCHAR(100),              -- Subcategoría opcional
    
    -- Versión
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    is_active BOOLEAN DEFAULT true,
    
    -- Definición de la plantilla (JSON)
    template_structure JSONB NOT NULL,
    
    -- Variables requeridas
    required_variables JSONB NOT NULL,  -- ['unitCode', 'unitName', 'manufacturer', ...]
    
    -- Metadata
    created_by UUID NOT NULL,
    created_time TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by UUID,
    updated_time TIMESTAMP,
    
    -- Tenant (para multi-tenancy)
    tenant_id UUID NOT NULL,
    
    CONSTRAINT fk_template_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_template_module_type ON template_definitions(module_code, entity_type);
CREATE INDEX idx_template_tenant ON template_definitions(tenant_id);
CREATE INDEX idx_template_active ON template_definitions(is_active) WHERE is_active = true;
```

### Tabla: `template_versions` (Historial)

```sql
CREATE TABLE template_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    version VARCHAR(20) NOT NULL,
    template_structure JSONB NOT NULL,
    change_description TEXT,
    created_by UUID NOT NULL,
    created_time TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_version_template FOREIGN KEY (template_id) 
        REFERENCES template_definitions(id) ON DELETE CASCADE,
    CONSTRAINT uk_template_version UNIQUE (template_id, version)
);
```

### Tabla: `template_instances` (Registro de Instancias Creadas)

```sql
CREATE TABLE template_instances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    template_version VARCHAR(20) NOT NULL,
    
    -- Asset raíz creado
    root_asset_id UUID NOT NULL,
    
    -- Variables usadas en la instanciación
    instance_variables JSONB NOT NULL,
    
    -- Metadata
    created_by UUID NOT NULL,
    created_time TIMESTAMP NOT NULL DEFAULT NOW(),
    tenant_id UUID NOT NULL,
    
    CONSTRAINT fk_instance_template FOREIGN KEY (template_id) 
        REFERENCES template_definitions(id) ON DELETE RESTRICT
);

CREATE INDEX idx_instance_template ON template_instances(template_id);
CREATE INDEX idx_instance_root_asset ON template_instances(root_asset_id);
```

---

## 📐 Estructura JSON de una Plantilla

### Formato: `template_structure`

```json
{
  "nodes": [
    {
      "nodeKey": "root",
      "nodeName": "{{unitCode}} - {{unitName}}",
      "nodeType": "ASSET",
      "assetType": "CT_UNIT",
      "isRoot": true,
      "attributes": [
        {
          "key": "unitCode",
          "label": "Unit Code",
          "dataType": "STRING",
          "value": "{{unitCode}}",
          "isRequired": true,
          "isServerAttribute": true
        },
        {
          "key": "manufacturer",
          "label": "Manufacturer",
          "dataType": "STRING",
          "value": "{{manufacturer}}",
          "isRequired": false,
          "isServerAttribute": true
        },
        {
          "key": "maxPressurePsi",
          "label": "Max Pressure (PSI)",
          "dataType": "LONG",
          "value": 15000,
          "isRequired": true,
          "isServerAttribute": true
        },
        {
          "key": "commissionDate",
          "label": "Commission Date",
          "dataType": "DATE",
          "value": "{{commissionDate}}",
          "isRequired": false,
          "isServerAttribute": true
        },
        {
          "key": "isOperational",
          "label": "Is Operational",
          "dataType": "BOOLEAN",
          "value": true,
          "isRequired": true,
          "isServerAttribute": true
        }
      ],
      "telemetries": [
        {
          "key": "currentPressurePsi",
          "label": "Current Pressure (PSI)",
          "dataType": "DOUBLE",
          "unit": "psi"
        },
        {
          "key": "operatingHours",
          "label": "Operating Hours",
          "dataType": "LONG",
          "unit": "hours"
        },
        {
          "key": "engineRpm",
          "label": "Engine RPM",
          "dataType": "LONG",
          "unit": "rpm"
        }
      ]
    },
    {
      "nodeKey": "hydraulic_system",
      "nodeName": "{{unitCode}} - Hydraulic System",
      "nodeType": "ASSET",
      "assetType": "CT_HYDRAULIC_SYSTEM",
      "isRoot": false,
      "parentNodeKey": "root",
      "relationType": "Contains",
      "attributes": [
        {
          "key": "pumpCapacityGpm",
          "label": "Pump Capacity (GPM)",
          "dataType": "LONG",
          "value": 120,
          "isRequired": true,
          "isServerAttribute": true
        },
        {
          "key": "maxPressurePsi",
          "label": "Max Pressure (PSI)",
          "dataType": "LONG",
          "value": 5000,
          "isRequired": true,
          "isServerAttribute": true
        }
      ],
      "telemetries": [
        {
          "key": "pumpPressurePsi",
          "label": "Pump Pressure (PSI)",
          "dataType": "DOUBLE",
          "unit": "psi"
        },
        {
          "key": "oilTempF",
          "label": "Oil Temperature (°F)",
          "dataType": "DOUBLE",
          "unit": "°F"
        }
      ]
    },
    {
      "nodeKey": "injection_system",
      "nodeName": "{{unitCode}} - Injection Head",
      "nodeType": "ASSET",
      "assetType": "CT_INJECTION_SYSTEM",
      "isRoot": false,
      "parentNodeKey": "root",
      "relationType": "Contains",
      "attributes": [
        {
          "key": "injectorType",
          "label": "Injector Type",
          "dataType": "STRING",
          "value": "CHAIN_DRIVE",
          "isRequired": true,
          "isServerAttribute": true
        },
        {
          "key": "maxSpeedFtMin",
          "label": "Max Speed (ft/min)",
          "dataType": "LONG",
          "value": 200,
          "isRequired": true,
          "isServerAttribute": true
        }
      ],
      "telemetries": [
        {
          "key": "speedFtMin",
          "label": "Speed (ft/min)",
          "dataType": "DOUBLE",
          "unit": "ft/min"
        },
        {
          "key": "gripForceLbf",
          "label": "Grip Force (lbf)",
          "dataType": "DOUBLE",
          "unit": "lbf"
        }
      ]
    }
  ],
  "relations": [
    {
      "fromNodeKey": "root",
      "toNodeKey": "hydraulic_system",
      "relationType": "Contains",
      "relationTypeGroup": "COMMON"
    },
    {
      "fromNodeKey": "root",
      "toNodeKey": "injection_system",
      "relationType": "Contains",
      "relationTypeGroup": "COMMON"
    }
  ]
}
```

### Formato: `required_variables`

```json
[
  {
    "name": "unitCode",
    "label": "Unit Code",
    "dataType": "STRING",
    "description": "Unique identifier for the unit",
    "isRequired": true,
    "validationPattern": "^CT-[0-9]{3}$",
    "example": "CT-001"
  },
  {
    "name": "unitName",
    "label": "Unit Name",
    "dataType": "STRING",
    "description": "Display name for the unit",
    "isRequired": true,
    "maxLength": 255,
    "example": "Coiled Tubing Unit Alpha"
  },
  {
    "name": "manufacturer",
    "label": "Manufacturer",
    "dataType": "STRING",
    "description": "Equipment manufacturer",
    "isRequired": false,
    "example": "NOV"
  },
  {
    "name": "commissionDate",
    "label": "Commission Date",
    "dataType": "DATE",
    "description": "Date when unit was commissioned",
    "isRequired": false,
    "example": "2024-01-15"
  }
]
```

---

## 🏗️ Arquitectura Backend

### DTOs

```java
// TemplateDefinitionDto.java
@Data
@Builder
public class TemplateDefinitionDto {
    private UUID id;
    private String templateCode;
    private String templateName;
    private String description;
    
    private String moduleCode;
    private String entityType;
    private String category;
    
    private String version;
    private Boolean isActive;
    
    private TemplateStructure templateStructure;
    private List<TemplateVariable> requiredVariables;
    
    private UUID createdBy;
    private Long createdTime;
    private UUID updatedBy;
    private Long updatedTime;
    
    private UUID tenantId;
}

// TemplateStructure.java
@Data
public class TemplateStructure {
    private List<TemplateNode> nodes;
    private List<TemplateRelation> relations;
}

// TemplateNode.java
@Data
public class TemplateNode {
    private String nodeKey;
    private String nodeName;
    private String nodeType;  // ASSET, DEVICE
    private String assetType;
    private Boolean isRoot;
    private String parentNodeKey;
    private String relationType;
    
    private List<TemplateAttribute> attributes;
    private List<TemplateTelemetry> telemetries;
}

// TemplateAttribute.java
@Data
public class TemplateAttribute {
    private String key;
    private String label;
    private String dataType;  // STRING, LONG, DOUBLE, BOOLEAN, DATE, JSON
    private Object value;
    private Boolean isRequired;
    private Boolean isServerAttribute;
}

// TemplateTelemetry.java
@Data
public class TemplateTelemetry {
    private String key;
    private String label;
    private String dataType;
    private String unit;
}

// TemplateVariable.java
@Data
public class TemplateVariable {
    private String name;
    private String label;
    private String dataType;
    private String description;
    private Boolean isRequired;
    private String validationPattern;
    private Integer maxLength;
    private String example;
}

// CreateFromTemplateRequest.java
@Data
public class CreateFromTemplateRequest {
    private UUID templateId;
    private Map<String, Object> variables;  // Variable name -> value
}

// TemplateInstanceResult.java
@Data
public class TemplateInstanceResult {
    private UUID instanceId;
    private UUID rootAssetId;
    private List<UUID> createdAssetIds;
    private Map<String, UUID> nodeKeyToAssetIdMap;
}
```

### Service: `TemplateService`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {
    
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository versionRepository;
    private final TemplateInstanceRepository instanceRepository;
    private final AssetService assetService;
    private final AttributesService attributesService;
    private final RelationService relationService;
    
    // ==================== CRUD ====================
    
    @Transactional
    public TemplateDefinitionDto create(CreateTemplateRequest request, UUID tenantId, UUID userId) {
        log.info("Creating template: {} for module: {}", request.getTemplateName(), request.getModuleCode());
        
        // Validar estructura
        validateTemplateStructure(request.getTemplateStructure());
        
        // Crear entidad
        TemplateDefinition template = TemplateDefinition.builder()
            .templateCode(generateTemplateCode(request))
            .templateName(request.getTemplateName())
            .description(request.getDescription())
            .moduleCode(request.getModuleCode())
            .entityType(request.getEntityType())
            .category(request.getCategory())
            .version("1.0.0")
            .isActive(true)
            .templateStructure(request.getTemplateStructure())
            .requiredVariables(request.getRequiredVariables())
            .createdBy(userId)
            .createdTime(System.currentTimeMillis())
            .tenantId(tenantId)
            .build();
        
        template = templateRepository.save(template);
        
        // Guardar primera versión
        saveVersion(template, "Initial version", userId);
        
        return TemplateDefinitionDto.fromEntity(template);
    }
    
    @Transactional
    public TemplateDefinitionDto update(UUID templateId, UpdateTemplateRequest request, UUID userId) {
        log.info("Updating template: {}", templateId);
        
        TemplateDefinition template = templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        // Validar nueva estructura
        if (request.getTemplateStructure() != null) {
            validateTemplateStructure(request.getTemplateStructure());
        }
        
        // Actualizar campos
        if (request.getTemplateName() != null) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getTemplateStructure() != null) {
            template.setTemplateStructure(request.getTemplateStructure());
            // Incrementar versión
            template.setVersion(incrementVersion(template.getVersion()));
        }
        if (request.getRequiredVariables() != null) {
            template.setRequiredVariables(request.getRequiredVariables());
        }
        
        template.setUpdatedBy(userId);
        template.setUpdatedTime(System.currentTimeMillis());
        
        template = templateRepository.save(template);
        
        // Guardar nueva versión si cambió la estructura
        if (request.getTemplateStructure() != null) {
            saveVersion(template, request.getChangeDescription(), userId);
        }
        
        return TemplateDefinitionDto.fromEntity(template);
    }
    
    @Transactional(readOnly = true)
    public TemplateDefinitionDto getById(UUID templateId) {
        TemplateDefinition template = templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        return TemplateDefinitionDto.fromEntity(template);
    }
    
    @Transactional(readOnly = true)
    public List<TemplateDefinitionDto> listByModule(String moduleCode, String entityType, UUID tenantId) {
        List<TemplateDefinition> templates;
        
        if (entityType != null) {
            templates = templateRepository.findByModuleCodeAndEntityTypeAndTenantIdAndIsActive(
                moduleCode, entityType, tenantId, true
            );
        } else {
            templates = templateRepository.findByModuleCodeAndTenantIdAndIsActive(
                moduleCode, tenantId, true
            );
        }
        
        return templates.stream()
            .map(TemplateDefinitionDto::fromEntity)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public void delete(UUID templateId) {
        log.info("Deleting template: {}", templateId);
        
        // Verificar que no tenga instancias activas
        long instanceCount = instanceRepository.countByTemplateId(templateId);
        if (instanceCount > 0) {
            throw new BusinessException(
                "Cannot delete template with existing instances. Found " + instanceCount + " instances."
            );
        }
        
        templateRepository.deleteById(templateId);
    }
    
    // ==================== INSTANCIACIÓN ====================
    
    @Transactional
    public TemplateInstanceResult instantiate(UUID templateId, Map<String, Object> variables, 
                                              UUID tenantId, UUID userId) {
        log.info("Instantiating template: {} with variables: {}", templateId, variables.keySet());
        
        // Cargar plantilla
        TemplateDefinition template = templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        if (!template.getIsActive()) {
            throw new BusinessException("Template is not active");
        }
        
        // Validar variables requeridas
        validateRequiredVariables(template.getRequiredVariables(), variables);
        
        // Procesar plantilla
        TemplateStructure structure = template.getTemplateStructure();
        Map<String, UUID> nodeKeyToAssetId = new HashMap<>();
        List<UUID> createdAssetIds = new ArrayList<>();
        
        // 1. Crear todos los assets
        for (TemplateNode node : structure.getNodes()) {
            Asset asset = createAssetFromNode(node, variables, tenantId);
            asset = assetService.saveAsset(asset);
            
            nodeKeyToAssetId.put(node.getNodeKey(), asset.getId().getId());
            createdAssetIds.add(asset.getId().getId());
            
            // Establecer atributos
            setAssetAttributes(asset.getId(), node.getAttributes(), variables);
            
            log.debug("Created asset: {} with ID: {}", asset.getName(), asset.getId());
        }
        
        // 2. Crear relaciones
        for (TemplateRelation relation : structure.getRelations()) {
            UUID fromAssetId = nodeKeyToAssetId.get(relation.getFromNodeKey());
            UUID toAssetId = nodeKeyToAssetId.get(relation.getToNodeKey());
            
            if (fromAssetId == null || toAssetId == null) {
                log.warn("Skipping relation - asset not found: {} -> {}", 
                    relation.getFromNodeKey(), relation.getToNodeKey());
                continue;
            }
            
            EntityRelation entityRelation = new EntityRelation();
            entityRelation.setFrom(new AssetId(fromAssetId));
            entityRelation.setTo(new AssetId(toAssetId));
            entityRelation.setType(relation.getRelationType());
            entityRelation.setTypeGroup(RelationTypeGroup.valueOf(relation.getRelationTypeGroup()));
            
            relationService.saveRelation(TenantId.fromUUID(tenantId), entityRelation);
            
            log.debug("Created relation: {} -[{}]-> {}", 
                relation.getFromNodeKey(), relation.getRelationType(), relation.getToNodeKey());
        }
        
        // 3. Obtener root asset
        UUID rootAssetId = structure.getNodes().stream()
            .filter(TemplateNode::getIsRoot)
            .findFirst()
            .map(node -> nodeKeyToAssetId.get(node.getNodeKey()))
            .orElseThrow(() -> new BusinessException("No root node found in template"));
        
        // 4. Registrar instancia
        TemplateInstance instance = TemplateInstance.builder()
            .templateId(templateId)
            .templateVersion(template.getVersion())
            .rootAssetId(rootAssetId)
            .instanceVariables(variables)
            .createdBy(userId)
            .createdTime(System.currentTimeMillis())
            .tenantId(tenantId)
            .build();
        
        instance = instanceRepository.save(instance);
        
        return TemplateInstanceResult.builder()
            .instanceId(instance.getId())
            .rootAssetId(rootAssetId)
            .createdAssetIds(createdAssetIds)
            .nodeKeyToAssetIdMap(nodeKeyToAssetId)
            .build();
    }
    
    // ==================== HELPERS ====================
    
    private void validateTemplateStructure(TemplateStructure structure) {
        // Validar que existe al menos un nodo root
        long rootCount = structure.getNodes().stream()
            .filter(TemplateNode::getIsRoot)
            .count();
        
        if (rootCount == 0) {
            throw new ValidationException("Template must have at least one root node");
        }
        
        if (rootCount > 1) {
            throw new ValidationException("Template can only have one root node");
        }
        
        // Validar que todos los parentNodeKey existen
        Set<String> nodeKeys = structure.getNodes().stream()
            .map(TemplateNode::getNodeKey)
            .collect(Collectors.toSet());
        
        for (TemplateNode node : structure.getNodes()) {
            if (!node.getIsRoot() && node.getParentNodeKey() != null) {
                if (!nodeKeys.contains(node.getParentNodeKey())) {
                    throw new ValidationException(
                        "Parent node key not found: " + node.getParentNodeKey()
                    );
                }
            }
        }
        
        // Validar relaciones
        for (TemplateRelation relation : structure.getRelations()) {
            if (!nodeKeys.contains(relation.getFromNodeKey())) {
                throw new ValidationException("From node key not found: " + relation.getFromNodeKey());
            }
            if (!nodeKeys.contains(relation.getToNodeKey())) {
                throw new ValidationException("To node key not found: " + relation.getToNodeKey());
            }
        }
    }
    
    private void validateRequiredVariables(List<TemplateVariable> required, Map<String, Object> provided) {
        for (TemplateVariable var : required) {
            if (var.getIsRequired() && !provided.containsKey(var.getName())) {
                throw new ValidationException("Required variable missing: " + var.getName());
            }
            
            // Validar tipo de dato
            if (provided.containsKey(var.getName())) {
                Object value = provided.get(var.getName());
                validateVariableType(var, value);
            }
        }
    }
    
    private Asset createAssetFromNode(TemplateNode node, Map<String, Object> variables, UUID tenantId) {
        Asset asset = new Asset();
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setName(replaceVariables(node.getNodeName(), variables));
        asset.setType(node.getAssetType());
        asset.setLabel(replaceVariables(node.getNodeName(), variables));
        
        return asset;
    }
    
    private String replaceVariables(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return result;
    }
    
    private void setAssetAttributes(AssetId assetId, List<TemplateAttribute> attributes, 
                                   Map<String, Object> variables) {
        List<AttributeKvEntry> kvEntries = new ArrayList<>();
        
        for (TemplateAttribute attr : attributes) {
            Object value = attr.getValue();
            
            // Reemplazar variables si es string
            if (value instanceof String) {
                value = replaceVariables((String) value, variables);
            }
            
            // Convertir a AttributeKvEntry según tipo
            AttributeKvEntry kvEntry = createAttributeKvEntry(attr.getKey(), value, attr.getDataType());
            kvEntries.add(kvEntry);
        }
        
        // Guardar atributos
        attributesService.save(assetId.getId(), AttributeScope.SERVER_SCOPE, kvEntries);
    }
}
```

### Controller: `TemplateController`

```java
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Slf4j
public class TemplateController {
    
    private final TemplateService templateService;
    
    @PostMapping
    @PreAuthorize("hasAuthority('TEMPLATE_CREATE')")
    public ResponseEntity<TemplateDefinitionDto> create(
        @RequestBody @Valid CreateTemplateRequest request
    ) {
        UUID tenantId = getCurrentUser().getTenantId().getId();
        UUID userId = getCurrentUser().getId().getId();
        
        TemplateDefinitionDto template = templateService.create(request, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }
    
    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('TEMPLATE_UPDATE')")
    public ResponseEntity<TemplateDefinitionDto> update(
        @PathVariable UUID templateId,
        @RequestBody @Valid UpdateTemplateRequest request
    ) {
        UUID userId = getCurrentUser().getId().getId();
        TemplateDefinitionDto template = templateService.update(templateId, request, userId);
        return ResponseEntity.ok(template);
    }
    
    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('TEMPLATE_READ')")
    public ResponseEntity<TemplateDefinitionDto> getById(@PathVariable UUID templateId) {
        TemplateDefinitionDto template = templateService.getById(templateId);
        return ResponseEntity.ok(template);
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('TEMPLATE_READ')")
    public ResponseEntity<List<TemplateDefinitionDto>> list(
        @RequestParam String moduleCode,
        @RequestParam(required = false) String entityType
    ) {
        UUID tenantId = getCurrentUser().getTenantId().getId();
        List<TemplateDefinitionDto> templates = templateService.listByModule(
            moduleCode, entityType, tenantId
        );
        return ResponseEntity.ok(templates);
    }
    
    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAuthority('TEMPLATE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID templateId) {
        templateService.delete(templateId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{templateId}/instantiate")
    @PreAuthorize("hasAuthority('TEMPLATE_USE')")
    public ResponseEntity<TemplateInstanceResult> instantiate(
        @PathVariable UUID templateId,
        @RequestBody Map<String, Object> variables
    ) {
        UUID tenantId = getCurrentUser().getTenantId().getId();
        UUID userId = getCurrentUser().getId().getId();
        
        TemplateInstanceResult result = templateService.instantiate(
            templateId, variables, tenantId, userId
        );
        return ResponseEntity.ok(result);
    }
}
```

---

## 🎨 Interfaz de Usuario

### Vista: Lista de Plantillas

```
┌─────────────────────────────────────────────────────────────────┐
│ 📋 Template Management                          [+ New Template] │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│ Module: [CT ▼]  Type: [All ▼]  Status: [Active ▼]  [🔍 Search] │
│                                                                   │
│ ┌───────────────────────────────────────────────────────────┐   │
│ │ Template Name        │ Module │ Type │ Version │ Actions  │   │
│ ├───────────────────────────────────────────────────────────┤   │
│ │ Standard CT Unit     │ CT     │ UNIT │ 2.1.0   │ ⚙️ 📋 🗑️ │   │
│ │ Heavy Duty CT Unit   │ CT     │ UNIT │ 1.5.0   │ ⚙️ 📋 🗑️ │   │
│ │ Standard CT Reel     │ CT     │ REEL │ 1.2.0   │ ⚙️ 📋 🗑️ │   │
│ │ Large Diameter Reel  │ CT     │ REEL │ 1.0.0   │ ⚙️ 📋 🗑️ │   │
│ └───────────────────────────────────────────────────────────┘   │
│                                                                   │
│ Showing 4 of 4 templates                        [1] 2 3 Next >  │
└─────────────────────────────────────────────────────────────────┘
```

### Vista: Editor de Plantilla (Crear/Editar)

```
┌─────────────────────────────────────────────────────────────────┐
│ ✏️ Template Editor - Standard CT Unit                    [Save] │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│ ┌─ Basic Information ────────────────────────────────────────┐  │
│ │ Template Code: [ct-unit-standard-v1_____________]          │  │
│ │ Template Name: [Standard CT Unit________________]          │  │
│ │ Description:   [Standard Coiled Tubing Unit for...____]    │  │
│ │                                                             │  │
│ │ Module:        [CT ▼]                                      │  │
│ │ Entity Type:   [UNIT ▼]                                    │  │
│ │ Category:      [Standard Equipment______________]          │  │
│ └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│ ┌─ Required Variables ───────────────────────────────────────┐  │
│ │ [+ Add Variable]                                            │  │
│ │                                                             │  │
│ │ ┌─ Variable: unitCode ─────────────────────────────────┐   │  │
│ │ │ Name:       unitCode                                  │   │  │
│ │ │ Label:      Unit Code                                 │   │  │
│ │ │ Data Type:  [STRING ▼]                               │   │  │
│ │ │ Required:   [✓]                                       │   │  │
│ │ │ Pattern:    ^CT-[0-9]{3}$                            │   │  │
│ │ │ Example:    CT-001                                    │   │  │
│ │ └───────────────────────────────────────────────────────┘   │  │
│ │                                                             │  │
│ │ ┌─ Variable: unitName ─────────────────────────────────┐   │  │
│ │ │ Name:       unitName                                  │   │  │
│ │ │ Label:      Unit Name                                 │   │  │
│ │ │ Data Type:  [STRING ▼]                               │   │  │
│ │ │ Required:   [✓]                                       │   │  │
│ │ │ Max Length: 255                                       │   │  │
│ │ └───────────────────────────────────────────────────────┘   │  │
│ └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│ ┌─ Asset Hierarchy ──────────────────────────────────────────┐  │
│ │ [+ Add Asset]                                               │  │
│ │                                                             │  │
│ │ 📦 CT Unit (root)                                          │  │
│ │   ├─ 🔧 Hydraulic System                                   │  │
│ │   ├─ ⚙️ Injection System                                   │  │
│ │   ├─ 🖥️ Control System                                     │  │
│ │   └─ ⚡ Power Pack                                         │  │
│ │                                                             │  │
│ │ [Edit Selected] [Delete Selected]                          │  │
│ └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│ [Cancel]                                              [Save]     │
└─────────────────────────────────────────────────────────────────┘
```

### Vista: Editor de Asset (dentro del editor de plantilla)

```
┌─────────────────────────────────────────────────────────────────┐
│ 📦 Edit Asset Node - CT Unit                            [Close] │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│ ┌─ Node Configuration ───────────────────────────────────────┐  │
│ │ Node Key:    [root_____________________]                   │  │
│ │ Node Name:   [{{unitCode}} - {{unitName}}]                 │  │
│ │ Node Type:   [ASSET ▼]                                     │  │
│ │ Asset Type:  [CT_UNIT_________________]                    │  │
│ │ Is Root:     [✓]                                           │  │
│ │ Parent Node: [None (Root)______________]                   │  │
│ │ Relation:    [Contains ▼]                                  │  │
│ └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│ ┌─ Attributes ────────────────────────────────────────────────┐  │
│ │ [+ Add Attribute]                                           │  │
│ │                                                             │  │
│ │ ┌─────────────────────────────────────────────────────┐    │  │
│ │ │ Key          │ Label        │ Type   │ Value  │ Req │    │  │
│ │ ├─────────────────────────────────────────────────────┤    │  │
│ │ │ unitCode     │ Unit Code    │ STRING │ {{...}}│ ✓  │    │  │
│ │ │ manufacturer │ Manufacturer │ STRING │ {{...}}│    │    │  │
│ │ │ maxPressure  │ Max Pressure │ LONG   │ 15000  │ ✓  │    │  │
│ │ │ isOperational│ Operational  │ BOOLEAN│ true   │ ✓  │    │  │
│ │ └─────────────────────────────────────────────────────┘    │  │
│ └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│ ┌─ Telemetries ──────────────────────────────────────────────┐  │
│ │ [+ Add Telemetry]                                           │  │
│ │                                                             │  │
│ │ ┌─────────────────────────────────────────────────────┐    │  │
│ │ │ Key              │ Label            │ Type   │ Unit │    │  │
│ │ ├─────────────────────────────────────────────────────┤    │  │
│ │ │ currentPressure  │ Current Pressure │ DOUBLE │ psi  │    │  │
│ │ │ operatingHours   │ Operating Hours  │ LONG   │ hrs  │    │  │
│ │ │ engineRpm        │ Engine RPM       │ LONG   │ rpm  │    │  │
│ │ └─────────────────────────────────────────────────────┘    │  │
│ └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│ [Cancel]                                              [Save]     │
└─────────────────────────────────────────────────────────────────┘
```

### Vista: Usar Plantilla (Crear desde Plantilla)

```
┌─────────────────────────────────────────────────────────────────┐
│ 🎯 Create from Template                                 [Create] │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│ ┌─ Select Template ──────────────────────────────────────────┐  │
│ │ Template: [Standard CT Unit ▼]                             │  │
│ │                                                             │  │
│ │ Description: Standard Coiled Tubing Unit for tubing up to  │  │
│ │              3.5" OD with 15,000 PSI max pressure          │  │
│ │                                                             │  │
│ │ Will create:                                                │  │
│ │   • 1 CT Unit asset                                         │  │
│ │   • 4 subsystem assets (Hydraulic, Injection, Control,     │  │
│ │     Power)                                                  │  │
│ │   • 4 Contains relations                                    │  │
│ └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│ ┌─ Fill Variables ───────────────────────────────────────────┐  │
│ │ * Unit Code:       [CT-001_________________]               │  │
│ │ * Unit Name:       [Coiled Tubing Unit Alpha]              │  │
│ │   Manufacturer:    [NOV____________________]               │  │
│ │   Commission Date: [📅 2024-01-15__________]               │  │
│ └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│ ┌─ Preview ──────────────────────────────────────────────────┐  │
│ │ Assets to be created:                                       │  │
│ │                                                             │  │
│ │ 📦 CT-001 - Coiled Tubing Unit Alpha                       │  │
│ │   ├─ 🔧 CT-001 - Hydraulic System                          │  │
│ │   ├─ ⚙️ CT-001 - Injection Head                            │  │
│ │   ├─ 🖥️ CT-001 - Control System                            │  │
│ │   └─ ⚡ CT-001 - Power Pack                                │  │
│ └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│ [Cancel]                                              [Create]   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Permisos

```sql
-- Permisos para el sistema de plantillas
INSERT INTO permission (id, name, description, resource_type, operation) VALUES
(uuid_generate_v4(), 'TEMPLATE_CREATE', 'Create templates', 'TEMPLATE', 'CREATE'),
(uuid_generate_v4(), 'TEMPLATE_READ', 'View templates', 'TEMPLATE', 'READ'),
(uuid_generate_v4(), 'TEMPLATE_UPDATE', 'Edit templates', 'TEMPLATE', 'UPDATE'),
(uuid_generate_v4(), 'TEMPLATE_DELETE', 'Delete templates', 'TEMPLATE', 'DELETE'),
(uuid_generate_v4(), 'TEMPLATE_USE', 'Use templates to create assets', 'TEMPLATE', 'EXECUTE');
```

---

## 📊 Flujo de Uso

### 1. Administrador Crea Plantilla

1. Navega a **Template Management**
2. Click en **[+ New Template]**
3. Completa información básica (nombre, módulo, tipo)
4. Define variables requeridas (unitCode, unitName, etc.)
5. Construye jerarquía de assets:
   - Agrega asset raíz (CT Unit)
   - Agrega subsistemas (Hydraulic, Injection, etc.)
   - Define atributos para cada asset
   - Define telemetrías para cada asset
6. Guarda plantilla

### 2. Usuario Crea Unidad desde Plantilla

1. En módulo CT, click en **[Create from Template]**
2. Selecciona plantilla "Standard CT Unit"
3. Completa variables requeridas:
   - Unit Code: CT-001
   - Unit Name: Coiled Tubing Unit Alpha
   - Manufacturer: NOV
4. Revisa preview de assets a crear
5. Click en **[Create]**
6. Sistema crea automáticamente:
   - 5 assets en ThingsBoard
   - Atributos configurados
   - Relaciones establecidas
   - Registro en ct_units

---

## ✅ Ventajas de este Enfoque

✅ **Flexibilidad Total** - Administradores pueden crear/modificar plantillas sin código  
✅ **Multi-Módulo** - Mismo sistema sirve para CT, Well Testing, y futuros módulos  
✅ **Versionado** - Historial completo de cambios en plantillas  
✅ **Validación** - Estructura validada antes de guardar  
✅ **Reutilizable** - Una plantilla puede usarse infinitas veces  
✅ **Trazabilidad** - Registro de qué plantilla creó qué assets  
✅ **Escalable** - Soporta jerarquías complejas de cualquier tamaño  

---

## 🚀 Próximos Pasos de Implementación

1. **Fase 1: Backend Core** (1 semana)
   - Crear tablas de BD
   - Implementar entidades JPA
   - Implementar DTOs
   - Implementar TemplateService (CRUD)
   - Implementar TemplateController

2. **Fase 2: Instanciación** (1 semana)
   - Implementar lógica de instanciación
   - Crear assets en ThingsBoard
   - Establecer relaciones
   - Configurar atributos y telemetrías

3. **Fase 3: Frontend** (2 semanas)
   - Lista de plantillas
   - Editor de plantillas (crear/editar)
   - Editor de assets (jerarquía)
   - Formulario "Crear desde plantilla"
   - Preview de assets a crear

4. **Fase 4: Testing** (1 semana)
   - Unit tests
   - Integration tests
   - E2E tests

---

**Documento creado**: Enero 2026  
**Versión**: 1.0.0  
**Estado**: Especificación Completa
