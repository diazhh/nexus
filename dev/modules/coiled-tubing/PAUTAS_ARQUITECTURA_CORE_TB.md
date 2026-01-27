# Pautas de Arquitectura Core ThingsBoard para Módulos

## 🚨 Problemas Identificados en la Compilación

### Análisis de Errores de Símbolos

Los errores de compilación actuales revelan **violaciones críticas** de la arquitectura modular de ThingsBoard:

```
[ERROR] cannot find symbol: class TwoFactorAuthService
[ERROR] cannot find symbol: class EntityCountCmd
[ERROR] cannot find symbol: class DeviceStateService
[ERROR] cannot find symbol: class TelemetrySubscriptionService
[ERROR] cannot find symbol: class InstallScripts
[ERROR] cannot find symbol: class ChangePasswordRequest
[ERROR] cannot find symbol: class SecurityUser
[ERROR] cannot find symbol: class EntitiesVersionControlService
```

**Causa raíz:** El módulo CT está importando y usando clases internas del core de ThingsBoard que:
1. **No están en el classpath del módulo**
2. **No deberían ser accesibles desde módulos externos**
3. **Violan el principio de separación de capas**

---

## 📋 Pautas Obligatorias para Módulos

### 1. **Separación de Capas Estricta**

```
┌─────────────────────────────────────────┐
│   MÓDULO CT (common/ct-module)          │  ← Tu código aquí
│   - Controllers                          │
│   - Services                             │
│   - Repositories                         │
│   - DTOs                                 │
└─────────────────────────────────────────┘
              ↓ SOLO puede usar ↓
┌─────────────────────────────────────────┐
│   THINGSBOARD PUBLIC APIs                │
│   - Asset API                            │
│   - Device API                           │
│   - Telemetry API                        │
│   - Attribute API                        │
│   - Relation API                         │
│   - Rule Engine API                      │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│   THINGSBOARD CORE (NO ACCESIBLE)       │  ← NO importar desde aquí
│   - TwoFactorAuthService                 │
│   - DeviceStateService                   │
│   - SecurityUser                         │
│   - InstallScripts                       │
│   - Servicios internos                   │
└─────────────────────────────────────────┘
```

### 2. **Dependencias Permitidas en pom.xml**

```xml
<!-- ✅ CORRECTO: Dependencias públicas de TB -->
<dependencies>
    <!-- APIs públicas de ThingsBoard -->
    <dependency>
        <groupId>org.thingsboard</groupId>
        <artifactId>common-data</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <dependency>
        <groupId>org.thingsboard</groupId>
        <artifactId>dao</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- Spring Boot y JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
</dependencies>

<!-- ❌ INCORRECTO: NO incluir módulos internos -->
<!-- NO HACER ESTO:
<dependency>
    <groupId>org.thingsboard</groupId>
    <artifactId>application</artifactId>  ← Contiene clases internas
</dependency>
-->
```

### 3. **Imports Permitidos vs Prohibidos**

#### ✅ **PERMITIDO:**
```java
// APIs públicas de ThingsBoard
import org.thingsboard.server.common.data.Asset;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;

// DAOs públicos
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

// Spring Framework
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
```

#### ❌ **PROHIBIDO:**
```java
// Servicios internos del core
import org.thingsboard.server.service.security.auth.TwoFactorAuthService;  // ❌
import org.thingsboard.server.service.state.DeviceStateService;            // ❌
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService; // ❌
import org.thingsboard.server.service.install.InstallScripts;              // ❌
import org.thingsboard.server.service.security.model.SecurityUser;         // ❌
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService; // ❌

// Clases de aplicación
import org.thingsboard.server.controller.*;  // ❌
import org.thingsboard.server.actors.*;      // ❌
```

### 4. **Patrón de Acceso a Assets y Devices**

#### ✅ **Forma Correcta:**
```java
@Service
@RequiredArgsConstructor
public class CTUnitService {
    
    // Inyectar servicios públicos de TB
    private final AssetService assetService;
    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    private final RelationService relationService;
    
    // Tu repositorio JPA para tablas custom
    private final CTUnitRepository ctUnitRepository;
    
    public CTUnitDto createUnit(TenantId tenantId, CreateCTUnitRequest request) {
        // 1. Crear asset en ThingsBoard usando API pública
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(request.getUnitName());
        asset.setType("CT_UNIT");
        asset.setLabel(request.getUnitCode());
        
        Asset savedAsset = assetService.saveAsset(asset);
        
        // 2. Guardar en tu tabla custom
        CTUnit unit = new CTUnit();
        unit.setTenantId(tenantId.getId());
        unit.setAssetId(savedAsset.getId().getId());
        unit.setUnitCode(request.getUnitCode());
        unit.setManufacturer(request.getManufacturer());
        
        CTUnit savedUnit = ctUnitRepository.save(unit);
        
        // 3. Agregar atributos usando API pública
        List<AttributeKvEntry> attributes = new ArrayList<>();
        attributes.add(new BaseAttributeKvEntry(
            System.currentTimeMillis(),
            new StringDataEntry("manufacturer", request.getManufacturer())
        ));
        
        attributesService.save(
            tenantId,
            savedAsset.getId(),
            AttributeScope.SERVER_SCOPE,
            attributes
        ).get();
        
        return toDto(savedUnit, savedAsset);
    }
}
```

### 5. **Estructura de Paquetes del Módulo**

```
common/ct-module/
├── src/main/java/org/thingsboard/nexus/ct/
│   ├── controller/           # REST Controllers
│   │   ├── CTUnitController.java
│   │   ├── CTReelController.java
│   │   └── CTJobController.java
│   ├── service/              # Lógica de negocio
│   │   ├── CTUnitService.java
│   │   ├── CTReelService.java
│   │   └── CTTemplateService.java
│   ├── repository/           # JPA Repositories
│   │   ├── CTUnitRepository.java
│   │   └── CTReelRepository.java
│   ├── model/                # Entidades JPA
│   │   ├── CTUnit.java
│   │   └── CTReel.java
│   ├── dto/                  # Data Transfer Objects
│   │   ├── CTUnitDto.java
│   │   └── CreateCTUnitRequest.java
│   └── config/               # Configuración Spring
│       └── CTModuleConfiguration.java
└── src/main/resources/
    └── application-ct.yml
```

### 6. **Configuración de Spring Boot**

```java
@Configuration
@ComponentScan(basePackages = "org.thingsboard.nexus.ct")
@EnableJpaRepositories(basePackages = "org.thingsboard.nexus.ct.repository")
@EntityScan(basePackages = "org.thingsboard.nexus.ct.model")
public class CTModuleConfiguration {
    
    // Beans específicos del módulo
    
    @Bean
    public CTTemplateService ctTemplateService(
            AssetService assetService,
            AttributesService attributesService) {
        return new CTTemplateService(assetService, attributesService);
    }
}
```

### 7. **Manejo de Tests**

#### ✅ **Tests Correctos:**
```java
@SpringBootTest
@ActiveProfiles("test")
public class CTUnitServiceTest {
    
    @Autowired
    private CTUnitService ctUnitService;
    
    @MockBean  // Mock de servicios de TB
    private AssetService assetService;
    
    @MockBean
    private AttributesService attributesService;
    
    @Test
    public void testCreateUnit() {
        // Arrange
        TenantId tenantId = new TenantId(UUID.randomUUID());
        CreateCTUnitRequest request = new CreateCTUnitRequest();
        request.setUnitCode("UNIT-001");
        
        Asset mockAsset = new Asset();
        mockAsset.setId(new AssetId(UUID.randomUUID()));
        
        when(assetService.saveAsset(any())).thenReturn(mockAsset);
        
        // Act
        CTUnitDto result = ctUnitService.createUnit(tenantId, request);
        
        // Assert
        assertNotNull(result);
        assertEquals("UNIT-001", result.getUnitCode());
        verify(assetService, times(1)).saveAsset(any());
    }
}
```

#### ❌ **NO hacer:**
```java
// NO importar clases internas en tests
@Autowired
private TwoFactorAuthService twoFactorAuthService;  // ❌

@Autowired
private DeviceStateService deviceStateService;      // ❌
```

---

## 🔧 Solución a los Errores Actuales

### Paso 1: Limpiar Dependencias en pom.xml

```bash
# Revisar y eliminar dependencias a módulos internos
cd /home/diazhh/dev/nexus/common/ct-module
vim pom.xml
```

Asegurar que **SOLO** tenga:
- `common-data`
- `dao`
- Spring Boot starters
- Lombok
- Testing frameworks

### Paso 2: Eliminar Imports Prohibidos

```bash
# Buscar imports problemáticos
grep -r "import org.thingsboard.server.service" src/
grep -r "import org.thingsboard.server.controller" src/
grep -r "import org.thingsboard.server.actors" src/
```

Reemplazar con APIs públicas equivalentes.

### Paso 3: Refactorizar Tests

Los tests en `application/src/test/` que fallan **NO son parte del módulo CT**. Son tests del core de TB que están fallando por otros motivos.

**Acción:** Enfocarse solo en tests dentro de `common/ct-module/src/test/`

### Paso 4: Verificar Compilación del Módulo

```bash
# Compilar SOLO el módulo CT
cd /home/diazhh/dev/nexus/common/ct-module
mvn clean compile

# Si compila exitosamente, el módulo está bien estructurado
```

---

## 📚 Checklist de Validación

Antes de compilar, verificar:

- [ ] **pom.xml** solo tiene dependencias públicas de TB
- [ ] **Ningún import** de `org.thingsboard.server.service.*` (excepto DAOs)
- [ ] **Ningún import** de `org.thingsboard.server.controller.*`
- [ ] **Ningún import** de `org.thingsboard.server.actors.*`
- [ ] **Servicios** solo usan `AssetService`, `DeviceService`, `AttributesService`, etc.
- [ ] **Tests** mockean servicios de TB, no los importan directamente
- [ ] **Entidades JPA** están en paquete del módulo, no en core TB
- [ ] **Controllers** usan `@RestController` y rutas bajo `/api/nexus/modules/ct/`

---

## 🎯 Principios Clave

### 1. **Módulo = Extensión, NO Fork**
Tu módulo extiende ThingsBoard, no lo modifica. Usa APIs públicas.

### 2. **Gemelos Digitales = Assets + Tablas Custom**
- Assets/Devices en TB para telemetría y atributos
- Tablas JPA custom para metadatos específicos del módulo

### 3. **Servicios de TB = Dependencias Inyectadas**
No instancies servicios de TB manualmente, inyéctalos vía Spring.

### 4. **Tests = Mocks de TB**
Mockea servicios de TB en tests, no uses implementaciones reales.

### 5. **Compilación Independiente**
El módulo debe compilar sin necesitar el módulo `application` de TB.

---

## 📖 Referencias

- **ThingsBoard Architecture:** https://thingsboard.io/docs/reference/architecture/
- **Custom Modules Guide:** Ver `/dev/modules/MODULAR_ARCHITECTURE_GUIDE.md`
- **DAO Layer:** `org.thingsboard.server.dao.*`
- **Common Data:** `org.thingsboard.server.common.data.*`

---

## ⚠️ Errores Comunes a Evitar

1. **Importar clases de `application` module**
2. **Usar `SecurityUser` directamente** (usar `TenantId` y `UserId`)
3. **Acceder a servicios internos** (usar DAOs públicos)
4. **Modificar código del core** (extender, no modificar)
5. **Tests que dependen de toda la aplicación** (usar mocks)

---

## ✅ Ejemplo Completo: Crear Asset con Relaciones

```java
@Service
@RequiredArgsConstructor
public class CTTemplateService {
    
    private final AssetService assetService;
    private final RelationService relationService;
    private final AttributesService attributesService;
    
    public Asset createUnitFromTemplate(TenantId tenantId, String templateId) {
        // 1. Crear asset raíz
        Asset rootAsset = new Asset();
        rootAsset.setTenantId(tenantId);
        rootAsset.setName("CT Unit 001");
        rootAsset.setType("CT_UNIT");
        Asset savedRoot = assetService.saveAsset(rootAsset);
        
        // 2. Crear sub-assets
        Asset hydraulicSystem = new Asset();
        hydraulicSystem.setTenantId(tenantId);
        hydraulicSystem.setName("Hydraulic System");
        hydraulicSystem.setType("CT_HYDRAULIC_SYSTEM");
        Asset savedHydraulic = assetService.saveAsset(hydraulicSystem);
        
        // 3. Crear relación
        EntityRelation relation = new EntityRelation();
        relation.setFrom(savedRoot.getId());
        relation.setTo(savedHydraulic.getId());
        relation.setType("Contains");
        relationService.saveRelation(tenantId, relation);
        
        // 4. Agregar atributos
        List<AttributeKvEntry> attrs = Arrays.asList(
            new BaseAttributeKvEntry(
                System.currentTimeMillis(),
                new LongDataEntry("max_pressure", 5500L)
            )
        );
        attributesService.save(
            tenantId,
            savedHydraulic.getId(),
            AttributeScope.SERVER_SCOPE,
            attrs
        ).get();
        
        return savedRoot;
    }
}
```

---

## 🚀 Próximos Pasos

1. **Auditar** todos los archivos del módulo CT
2. **Eliminar** imports prohibidos
3. **Refactorizar** servicios para usar solo APIs públicas
4. **Actualizar** tests con mocks apropiados
5. **Compilar** módulo CT de forma independiente
6. **Documentar** patrones exitosos en este archivo
