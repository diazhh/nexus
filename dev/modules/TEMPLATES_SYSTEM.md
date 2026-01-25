# Sistema de Plantillas (Templates System)

## Visión General

El Sistema de Plantillas permite crear configuraciones predefinidas de gemelos digitales complejos que pueden instanciarse con un solo clic. Esto es fundamental para estandarizar la creación de assets y devices en los módulos operativos.

## Arquitectura

### Componentes

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Angular)                        │
│  - Template Selector UI                                      │
│  - Template Creator/Editor                                   │
│  - Instance Creator Form                                     │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Backend (Java Spring)                     │
│  - Template Service                                          │
│  - Instance Service                                          │
│  - Validation Service                                        │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Base de Datos (PostgreSQL)                │
│  - nx_templates                                              │
│  - nx_template_nodes                                         │
│  - nx_template_relations                                     │
│  - nx_template_attributes                                    │
│  - nx_template_telemetries                                   │
└─────────────────────────────────────────────────────────────┘
```

## Modelo de Datos

### Tabla: nx_templates

Almacena las definiciones de plantillas.

```sql
CREATE TABLE nx_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    module_name VARCHAR(100) NOT NULL,
    template_type VARCHAR(50) NOT NULL, -- 'ASSET', 'DEVICE', 'COMPOSITE'
    category VARCHAR(100), -- Para filtrado (ej: 'CT_UNIT', 'CT_REEL')
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    is_system_template BOOLEAN DEFAULT FALSE, -- Plantillas del sistema no editables
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB, -- Información adicional customizable
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT uk_template_name UNIQUE (tenant_id, module_name, name, version)
);

CREATE INDEX idx_nx_templates_tenant ON nx_templates(tenant_id);
CREATE INDEX idx_nx_templates_module ON nx_templates(module_name);
CREATE INDEX idx_nx_templates_category ON nx_templates(category);
CREATE INDEX idx_nx_templates_active ON nx_templates(is_active);
```

### Tabla: nx_template_nodes

Define los nodos (assets/devices) que componen la plantilla.

```sql
CREATE TABLE nx_template_nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    node_key VARCHAR(100) NOT NULL, -- Identificador único dentro de la plantilla
    node_name VARCHAR(255) NOT NULL, -- Nombre del nodo
    node_type VARCHAR(50) NOT NULL, -- 'ASSET', 'DEVICE'
    asset_type VARCHAR(100), -- Tipo de asset en TB (si node_type='ASSET')
    device_profile_id UUID, -- Profile del device (si node_type='DEVICE')
    description TEXT,
    is_root BOOLEAN DEFAULT FALSE, -- Nodo raíz de la jerarquía
    parent_node_key VARCHAR(100), -- Referencia al nodo padre
    level INTEGER DEFAULT 0, -- Nivel en la jerarquía
    metadata JSONB,
    CONSTRAINT fk_template FOREIGN KEY (template_id) REFERENCES nx_templates(id) ON DELETE CASCADE,
    CONSTRAINT uk_node_key UNIQUE (template_id, node_key)
);

CREATE INDEX idx_nx_template_nodes_template ON nx_template_nodes(template_id);
CREATE INDEX idx_nx_template_nodes_parent ON nx_template_nodes(parent_node_key);
```

### Tabla: nx_template_relations

Define las relaciones entre nodos de la plantilla.

```sql
CREATE TABLE nx_template_relations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    from_node_key VARCHAR(100) NOT NULL,
    to_node_key VARCHAR(100) NOT NULL,
    relation_type VARCHAR(100) NOT NULL, -- 'Contains', 'Manages', 'Uses', etc.
    is_dynamic BOOLEAN DEFAULT FALSE, -- Si la relación puede cambiar en runtime
    metadata JSONB,
    CONSTRAINT fk_template_rel FOREIGN KEY (template_id) REFERENCES nx_templates(id) ON DELETE CASCADE,
    CONSTRAINT uk_template_relation UNIQUE (template_id, from_node_key, to_node_key, relation_type)
);

CREATE INDEX idx_nx_template_relations_template ON nx_template_relations(template_id);
CREATE INDEX idx_nx_template_relations_from ON nx_template_relations(from_node_key);
CREATE INDEX idx_nx_template_relations_to ON nx_template_relations(to_node_key);
```

### Tabla: nx_template_attributes

Define atributos predefinidos para cada nodo.

```sql
CREATE TABLE nx_template_attributes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    node_key VARCHAR(100) NOT NULL,
    attribute_scope VARCHAR(20) NOT NULL, -- 'SERVER_SCOPE', 'SHARED_SCOPE', 'CLIENT_SCOPE'
    attribute_key VARCHAR(255) NOT NULL,
    attribute_type VARCHAR(50) NOT NULL, -- 'STRING', 'LONG', 'DOUBLE', 'BOOLEAN', 'JSON'
    default_value TEXT,
    is_required BOOLEAN DEFAULT FALSE,
    is_editable BOOLEAN DEFAULT TRUE, -- Si puede editarse al crear instancia
    validation_rules JSONB, -- Reglas de validación JSON Schema
    description TEXT,
    metadata JSONB,
    CONSTRAINT fk_template_attr FOREIGN KEY (template_id) REFERENCES nx_templates(id) ON DELETE CASCADE
);

CREATE INDEX idx_nx_template_attributes_template ON nx_template_attributes(template_id);
CREATE INDEX idx_nx_template_attributes_node ON nx_template_attributes(node_key);
```

### Tabla: nx_template_telemetries

Define telemetrías esperadas para cada nodo.

```sql
CREATE TABLE nx_template_telemetries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    node_key VARCHAR(100) NOT NULL,
    telemetry_key VARCHAR(255) NOT NULL,
    telemetry_type VARCHAR(50) NOT NULL, -- 'DOUBLE', 'LONG', 'STRING', 'BOOLEAN'
    unit VARCHAR(50), -- Unidad de medida
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    description TEXT,
    metadata JSONB,
    CONSTRAINT fk_template_telem FOREIGN KEY (template_id) REFERENCES nx_templates(id) ON DELETE CASCADE
);

CREATE INDEX idx_nx_template_telemetries_template ON nx_template_telemetries(template_id);
CREATE INDEX idx_nx_template_telemetries_node ON nx_template_telemetries(node_key);
```

### Tabla: nx_template_instances

Registro de instancias creadas desde plantillas.

```sql
CREATE TABLE nx_template_instances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    instance_name VARCHAR(255) NOT NULL,
    root_entity_id UUID NOT NULL, -- ID del asset/device raíz creado
    root_entity_type VARCHAR(20) NOT NULL, -- 'ASSET', 'DEVICE'
    created_by UUID,
    created_time BIGINT NOT NULL,
    metadata JSONB, -- Mapeo de node_key -> entity_id
    CONSTRAINT fk_template_inst FOREIGN KEY (template_id) REFERENCES nx_templates(id),
    CONSTRAINT fk_tenant_inst FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_nx_template_instances_template ON nx_template_instances(template_id);
CREATE INDEX idx_nx_template_instances_tenant ON nx_template_instances(tenant_id);
CREATE INDEX idx_nx_template_instances_root ON nx_template_instances(root_entity_id);
```

## Modelo Java (Backend)

### Entity: NexusTemplate

```java
@Entity
@Table(name = "nx_templates")
public class NexusTemplate {
    
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
    
    @Column(name = "template_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TemplateType templateType;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "version", nullable = false)
    private String version;
    
    @Column(name = "is_system_template")
    private boolean systemTemplate;
    
    @Column(name = "is_active")
    private boolean active;
    
    @Type(type = "jsonb")
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "created_time", nullable = false)
    private long createdTime;
    
    @Column(name = "updated_time")
    private Long updatedTime;
    
    // Getters y Setters
}
```

### Service: TemplateService

```java
@Service
public class TemplateService {
    
    @Autowired
    private NexusTemplateRepository templateRepository;
    
    @Autowired
    private TemplateNodeRepository nodeRepository;
    
    @Autowired
    private TemplateRelationRepository relationRepository;
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private DeviceService deviceService;
    
    /**
     * Crea una nueva plantilla
     */
    @Transactional
    public NexusTemplate createTemplate(TemplateCreateRequest request) {
        // Validar request
        validateTemplateRequest(request);
        
        // Crear template
        NexusTemplate template = new NexusTemplate();
        template.setId(UUID.randomUUID());
        template.setTenantId(request.getTenantId());
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setModuleName(request.getModuleName());
        template.setTemplateType(request.getTemplateType());
        template.setCategory(request.getCategory());
        template.setVersion("1.0.0");
        template.setActive(true);
        template.setCreatedTime(System.currentTimeMillis());
        
        template = templateRepository.save(template);
        
        // Crear nodos
        for (TemplateNodeRequest nodeReq : request.getNodes()) {
            createTemplateNode(template.getId(), nodeReq);
        }
        
        // Crear relaciones
        for (TemplateRelationRequest relReq : request.getRelations()) {
            createTemplateRelation(template.getId(), relReq);
        }
        
        return template;
    }
    
    /**
     * Instancia una plantilla creando los assets/devices reales
     */
    @Transactional
    public TemplateInstance instantiateTemplate(UUID templateId, TemplateInstanceRequest request) {
        NexusTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new NotFoundException("Template not found"));
        
        if (!template.isActive()) {
            throw new IllegalStateException("Template is not active");
        }
        
        // Obtener todos los nodos de la plantilla
        List<TemplateNode> nodes = nodeRepository.findByTemplateId(templateId);
        
        // Mapa para trackear node_key -> entity_id creado
        Map<String, EntityId> createdEntities = new HashMap<>();
        
        // Ordenar nodos por nivel (crear primero los padres)
        nodes.sort(Comparator.comparing(TemplateNode::getLevel));
        
        // Crear cada nodo
        for (TemplateNode node : nodes) {
            EntityId entityId = createEntityFromNode(template, node, request, createdEntities);
            createdEntities.put(node.getNodeKey(), entityId);
        }
        
        // Crear relaciones
        List<TemplateRelation> relations = relationRepository.findByTemplateId(templateId);
        for (TemplateRelation relation : relations) {
            EntityId fromEntity = createdEntities.get(relation.getFromNodeKey());
            EntityId toEntity = createdEntities.get(relation.getToNodeKey());
            
            createRelation(fromEntity, toEntity, relation.getRelationType());
        }
        
        // Registrar instancia
        TemplateInstance instance = new TemplateInstance();
        instance.setTemplateId(templateId);
        instance.setTenantId(request.getTenantId());
        instance.setInstanceName(request.getInstanceName());
        
        // El root entity es el nodo marcado como root
        TemplateNode rootNode = nodes.stream()
            .filter(TemplateNode::isRoot)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No root node found"));
        
        EntityId rootEntity = createdEntities.get(rootNode.getNodeKey());
        instance.setRootEntityId(rootEntity.getId());
        instance.setRootEntityType(rootEntity.getEntityType());
        instance.setCreatedTime(System.currentTimeMillis());
        
        // Guardar mapeo de node_key -> entity_id en metadata
        ObjectNode metadata = objectMapper.createObjectNode();
        for (Map.Entry<String, EntityId> entry : createdEntities.entrySet()) {
            metadata.put(entry.getKey(), entry.getValue().toString());
        }
        instance.setMetadata(metadata);
        
        return instanceRepository.save(instance);
    }
    
    /**
     * Crea un asset o device desde un nodo de plantilla
     */
    private EntityId createEntityFromNode(
            NexusTemplate template,
            TemplateNode node,
            TemplateInstanceRequest request,
            Map<String, EntityId> createdEntities) {
        
        String entityName = buildEntityName(node, request);
        
        if (node.getNodeType() == NodeType.ASSET) {
            // Crear Asset
            Asset asset = new Asset();
            asset.setTenantId(new TenantId(request.getTenantId()));
            asset.setName(entityName);
            asset.setType(node.getAssetType());
            asset.setLabel(node.getDescription());
            
            // Aplicar atributos del template
            List<TemplateAttribute> attributes = attributeRepository
                .findByTemplateIdAndNodeKey(template.getId(), node.getNodeKey());
            
            for (TemplateAttribute attr : attributes) {
                String value = request.getAttributeOverrides()
                    .getOrDefault(node.getNodeKey() + "." + attr.getAttributeKey(), 
                                 attr.getDefaultValue());
                
                assetService.saveEntityAttributes(
                    asset.getId(),
                    attr.getAttributeScope(),
                    Collections.singletonList(new AttributeKvEntry(
                        System.currentTimeMillis(),
                        new StringDataEntry(attr.getAttributeKey(), value)
                    ))
                );
            }
            
            asset = assetService.saveAsset(asset);
            
            // Establecer relación con padre si existe
            if (node.getParentNodeKey() != null) {
                EntityId parentEntity = createdEntities.get(node.getParentNodeKey());
                createRelation(parentEntity, asset.getId(), "Contains");
            }
            
            return asset.getId();
            
        } else {
            // Crear Device
            Device device = new Device();
            device.setTenantId(new TenantId(request.getTenantId()));
            device.setName(entityName);
            device.setType(node.getAssetType()); // O device type
            device.setLabel(node.getDescription());
            
            if (node.getDeviceProfileId() != null) {
                device.setDeviceProfileId(new DeviceProfileId(node.getDeviceProfileId()));
            }
            
            device = deviceService.saveDevice(device);
            
            // Establecer relación con padre si existe
            if (node.getParentNodeKey() != null) {
                EntityId parentEntity = createdEntities.get(node.getParentNodeKey());
                createRelation(parentEntity, device.getId(), "Contains");
            }
            
            return device.getId();
        }
    }
    
    /**
     * Lista plantillas filtradas por módulo y categoría
     */
    public PageData<NexusTemplate> findTemplates(
            UUID tenantId,
            String moduleName,
            String category,
            PageLink pageLink) {
        
        return templateRepository.findByTenantIdAndModuleNameAndCategory(
            tenantId, moduleName, category, pageLink);
    }
}
```

## API REST

### Endpoints

```java
@RestController
@RequestMapping("/api/nexus/templates")
public class TemplateController {
    
    @Autowired
    private TemplateService templateService;
    
    /**
     * POST /api/nexus/templates
     * Crear nueva plantilla
     */
    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public NexusTemplate createTemplate(@RequestBody TemplateCreateRequest request) {
        return templateService.createTemplate(request);
    }
    
    /**
     * GET /api/nexus/templates/{templateId}
     * Obtener plantilla por ID
     */
    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public NexusTemplate getTemplate(@PathVariable UUID templateId) {
        return templateService.findById(templateId);
    }
    
    /**
     * GET /api/nexus/templates
     * Listar plantillas con filtros
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<NexusTemplate> listTemplates(
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") int page) {
        
        UUID tenantId = getCurrentUser().getTenantId().getId();
        PageLink pageLink = new PageLink(pageSize, page);
        
        return templateService.findTemplates(tenantId, moduleName, category, pageLink);
    }
    
    /**
     * POST /api/nexus/templates/{templateId}/instantiate
     * Crear instancia desde plantilla
     */
    @PostMapping("/{templateId}/instantiate")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public TemplateInstance instantiateTemplate(
            @PathVariable UUID templateId,
            @RequestBody TemplateInstanceRequest request) {
        
        request.setTenantId(getCurrentUser().getTenantId().getId());
        return templateService.instantiateTemplate(templateId, request);
    }
    
    /**
     * GET /api/nexus/templates/{templateId}/structure
     * Obtener estructura completa de la plantilla
     */
    @GetMapping("/{templateId}/structure")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public TemplateStructure getTemplateStructure(@PathVariable UUID templateId) {
        return templateService.getTemplateStructure(templateId);
    }
}
```

## Frontend (Angular)

### Service: TemplateService

```typescript
@Injectable({
  providedIn: 'root'
})
export class TemplateService {
  
  constructor(private http: HttpClient) {}
  
  createTemplate(request: TemplateCreateRequest): Observable<NexusTemplate> {
    return this.http.post<NexusTemplate>('/api/nexus/templates', request);
  }
  
  getTemplate(templateId: string): Observable<NexusTemplate> {
    return this.http.get<NexusTemplate>(`/api/nexus/templates/${templateId}`);
  }
  
  listTemplates(params: TemplateQueryParams): Observable<PageData<NexusTemplate>> {
    let httpParams = new HttpParams();
    if (params.moduleName) httpParams = httpParams.set('moduleName', params.moduleName);
    if (params.category) httpParams = httpParams.set('category', params.category);
    httpParams = httpParams.set('pageSize', params.pageSize.toString());
    httpParams = httpParams.set('page', params.page.toString());
    
    return this.http.get<PageData<NexusTemplate>>('/api/nexus/templates', { params: httpParams });
  }
  
  instantiateTemplate(templateId: string, request: TemplateInstanceRequest): Observable<TemplateInstance> {
    return this.http.post<TemplateInstance>(
      `/api/nexus/templates/${templateId}/instantiate`,
      request
    );
  }
  
  getTemplateStructure(templateId: string): Observable<TemplateStructure> {
    return this.http.get<TemplateStructure>(`/api/nexus/templates/${templateId}/structure`);
  }
}
```

### Component: Template Selector

```typescript
@Component({
  selector: 'tb-template-selector',
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Seleccionar Plantilla</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <mat-form-field>
          <mat-label>Categoría</mat-label>
          <mat-select [(ngModel)]="selectedCategory" (selectionChange)="loadTemplates()">
            <mat-option *ngFor="let cat of categories" [value]="cat.value">
              {{cat.label}}
            </mat-option>
          </mat-select>
        </mat-form-field>
        
        <mat-list>
          <mat-list-item *ngFor="let template of templates" 
                         (click)="selectTemplate(template)"
                         [class.selected]="selectedTemplate?.id === template.id">
            <h3 matLine>{{template.name}}</h3>
            <p matLine>{{template.description}}</p>
            <span matLine class="version">v{{template.version}}</span>
          </mat-list-item>
        </mat-list>
      </mat-card-content>
      <mat-card-actions>
        <button mat-raised-button color="primary" 
                [disabled]="!selectedTemplate"
                (click)="onCreateInstance()">
          Crear Instancia
        </button>
      </mat-card-actions>
    </mat-card>
  `
})
export class TemplateSelectorComponent implements OnInit {
  
  @Input() moduleName: string;
  @Output() templateSelected = new EventEmitter<NexusTemplate>();
  
  templates: NexusTemplate[] = [];
  categories: any[] = [];
  selectedCategory: string;
  selectedTemplate: NexusTemplate;
  
  constructor(private templateService: TemplateService) {}
  
  ngOnInit() {
    this.loadCategories();
    this.loadTemplates();
  }
  
  loadTemplates() {
    this.templateService.listTemplates({
      moduleName: this.moduleName,
      category: this.selectedCategory,
      pageSize: 100,
      page: 0
    }).subscribe(data => {
      this.templates = data.data;
    });
  }
  
  selectTemplate(template: NexusTemplate) {
    this.selectedTemplate = template;
  }
  
  onCreateInstance() {
    this.templateSelected.emit(this.selectedTemplate);
  }
}
```

## Ejemplo de Uso: Plantilla de Unidad CT

### Definición JSON

```json
{
  "name": "Unidad CT Estándar",
  "description": "Plantilla estándar para unidad de Coiled Tubing",
  "moduleName": "coiled-tubing",
  "templateType": "COMPOSITE",
  "category": "CT_UNIT",
  "nodes": [
    {
      "nodeKey": "root",
      "nodeName": "{instance_name}",
      "nodeType": "ASSET",
      "assetType": "CT_UNIT",
      "isRoot": true,
      "level": 0,
      "attributes": [
        {
          "scope": "SERVER_SCOPE",
          "key": "manufacturer",
          "type": "STRING",
          "defaultValue": "",
          "isRequired": true,
          "isEditable": true
        },
        {
          "scope": "SERVER_SCOPE",
          "key": "model",
          "type": "STRING",
          "isRequired": true
        },
        {
          "scope": "SERVER_SCOPE",
          "key": "serial_number",
          "type": "STRING",
          "isRequired": true
        }
      ]
    },
    {
      "nodeKey": "hydraulic_system",
      "nodeName": "Sistema Hidráulico",
      "nodeType": "ASSET",
      "assetType": "CT_HYDRAULIC_SYSTEM",
      "parentNodeKey": "root",
      "level": 1,
      "attributes": [
        {
          "scope": "SERVER_SCOPE",
          "key": "max_pressure",
          "type": "DOUBLE",
          "defaultValue": "5000"
        },
        {
          "scope": "SERVER_SCOPE",
          "key": "capacity_liters",
          "type": "DOUBLE",
          "defaultValue": "500"
        }
      ],
      "telemetries": [
        {
          "key": "current_pressure",
          "type": "DOUBLE",
          "unit": "PSI",
          "minValue": 0,
          "maxValue": 6000
        },
        {
          "key": "temperature",
          "type": "DOUBLE",
          "unit": "°C"
        }
      ]
    },
    {
      "nodeKey": "injection_system",
      "nodeName": "Sistema de Inyección",
      "nodeType": "ASSET",
      "assetType": "CT_INJECTION_SYSTEM",
      "parentNodeKey": "root",
      "level": 1,
      "telemetries": [
        {
          "key": "speed",
          "type": "DOUBLE",
          "unit": "m/min"
        },
        {
          "key": "tension",
          "type": "DOUBLE",
          "unit": "kN"
        },
        {
          "key": "depth",
          "type": "DOUBLE",
          "unit": "m"
        }
      ]
    },
    {
      "nodeKey": "control_system",
      "nodeName": "Sistema de Control",
      "nodeType": "ASSET",
      "assetType": "CT_CONTROL_SYSTEM",
      "parentNodeKey": "root",
      "level": 1
    }
  ],
  "relations": [
    {
      "fromNodeKey": "root",
      "toNodeKey": "hydraulic_system",
      "relationType": "Contains"
    },
    {
      "fromNodeKey": "root",
      "toNodeKey": "injection_system",
      "relationType": "Contains"
    },
    {
      "fromNodeKey": "root",
      "toNodeKey": "control_system",
      "relationType": "Contains"
    }
  ]
}
```

### Instanciar Plantilla

```typescript
const instanceRequest: TemplateInstanceRequest = {
  instanceName: "CT-UNIT-001",
  attributeOverrides: {
    "root.manufacturer": "NOV",
    "root.model": "XL-500",
    "root.serial_number": "SN-2024-001",
    "hydraulic_system.max_pressure": "5500",
    "hydraulic_system.capacity_liters": "600"
  }
};

this.templateService.instantiateTemplate(templateId, instanceRequest)
  .subscribe(instance => {
    console.log('Instancia creada:', instance);
    // Navegar a la vista de la unidad creada
    this.router.navigate(['/modules/coiled-tubing/units', instance.rootEntityId]);
  });
```

## Ventajas del Sistema

1. **Estandarización**: Garantiza que todas las instancias sigan la misma estructura
2. **Rapidez**: Crear gemelos digitales complejos en segundos
3. **Consistencia**: Todos los assets tienen los atributos y telemetrías correctos
4. **Mantenibilidad**: Actualizar plantillas afecta a nuevas instancias
5. **Documentación**: Las plantillas sirven como documentación de la estructura

## Próximos Pasos

1. Implementar esquema de base de datos
2. Desarrollar servicios backend
3. Crear componentes frontend
4. Crear plantillas para módulo Coiled Tubing
5. Pruebas de integración
