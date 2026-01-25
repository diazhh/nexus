# Metodología de Desarrollo de Módulos para Nexus/ThingsBoard

## 📋 Propósito
Este documento establece las **convenciones y mejores prácticas** para desarrollar nuevos módulos en la plataforma Nexus IoT basada en ThingsBoard, garantizando que el código compile correctamente y siga los estándares del proyecto.

---

## 🏗️ Estructura de Módulos en ThingsBoard

### Ubicación de Módulos
Los módulos personalizados se colocan en:
```
/common/<nombre-modulo>/
```

Ejemplo del módulo Coiled Tubing:
```
/common/ct-module/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/thingsboard/nexus/ct/
│   │   │       ├── model/           # Entidades JPA
│   │   │       ├── dto/             # Data Transfer Objects
│   │   │       ├── repository/      # Repositorios JPA
│   │   │       ├── service/         # Lógica de negocio
│   │   │       ├── controller/      # REST Controllers
│   │   │       └── exception/       # Excepciones personalizadas
│   │   └── resources/
│   │       └── application.yml      # Configuración del módulo
│   └── test/
└── README.md
```

---

## 📦 Configuración Maven (pom.xml)

### 1. Parent POM
Todo módulo en `/common/` debe heredar del POM padre `common`:

```xml
<parent>
    <groupId>org.thingsboard</groupId>
    <artifactId>common</artifactId>
    <version>4.3.0-RC</version>
</parent>

<groupId>org.thingsboard.common</groupId>
<artifactId>nombre-modulo</artifactId>
<packaging>jar</packaging>
```

### 2. Dependencias Esenciales
```xml
<dependencies>
    <!-- Core ThingsBoard -->
    <dependency>
        <groupId>org.thingsboard.common</groupId>
        <artifactId>data</artifactId>
    </dependency>
    <dependency>
        <groupId>org.thingsboard.common</groupId>
        <artifactId>dao-api</artifactId>
    </dependency>
    
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

### 3. Configuración del Compiler Plugin
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 4. Registrar el Módulo en common/pom.xml
```xml
<modules>
    <!-- ... módulos existentes ... -->
    <module>nombre-modulo</module>
</modules>
```

---

## 💻 Convenciones de Código

### 1. Servicios (@Service)

#### ❌ INCORRECTO - Métodos void
```java
@Service
public class MiService {
    
    @Transactional
    public void update(UUID id, Entity entity) {
        // ... lógica ...
        repository.save(entity);
    }
}
```

#### ✅ CORRECTO - Métodos que retornan DTOs
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MiService {
    
    private final MiRepository repository;
    
    @Transactional
    public MiDto update(UUID id, Entity entity) {
        // ... lógica ...
        Entity savedEntity = repository.save(entity);
        return MiDto.fromEntity(savedEntity);
    }
}
```

**Razón**: Los controladores necesitan retornar la entidad actualizada al cliente.

### 2. Logging

#### ❌ INCORRECTO - Doble declaración de logger
```java
@Slf4j
public class MiService {
    private static final Logger log = LoggerFactory.getLogger(MiService.class);
    // ...
}
```

#### ✅ CORRECTO - Solo usar @Slf4j
```java
@Slf4j
public class MiService {
    // La anotación @Slf4j crea automáticamente el campo 'log'
    
    public void miMetodo() {
        log.info("Mensaje de log");
    }
}
```

**Razón**: `@Slf4j` de Lombok ya crea el campo `log`. Declararlo manualmente causa conflictos.

### 3. Controladores REST

#### ✅ Estructura Correcta
```java
@RestController
@RequestMapping("/api/mi-modulo/entidades")
@RequiredArgsConstructor
@Slf4j
public class MiController {
    
    private final MiService service;
    
    @GetMapping("/{id}")
    public ResponseEntity<MiDto> getById(@PathVariable UUID id) {
        MiDto dto = service.getById(id);
        return ResponseEntity.ok(dto);
    }
    
    @PostMapping
    public ResponseEntity<MiDto> create(@Valid @RequestBody Entity entity) {
        MiDto created = service.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MiDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody Entity entity) {
        MiDto updated = service.update(id, entity);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 4. DTOs (Data Transfer Objects)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiDto {
    private UUID id;
    private UUID tenantId;
    private String nombre;
    private Long createdTime;
    private Long updatedTime;
    
    public static MiDto fromEntity(MiEntity entity) {
        return MiDto.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .nombre(entity.getNombre())
            .createdTime(entity.getCreatedTime())
            .updatedTime(entity.getUpdatedTime())
            .build();
    }
}
```

### 5. Entidades JPA

```java
@Entity
@Table(name = "mi_tabla")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;
    
    @Column(name = "created_time", nullable = false)
    private Long createdTime;
    
    @Column(name = "updated_time")
    private Long updatedTime;
    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdTime == null) {
            createdTime = System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedTime = System.currentTimeMillis();
    }
}
```

### 6. Repositorios JPA

```java
@Repository
public interface MiRepository extends JpaRepository<MiEntity, UUID> {
    
    Optional<MiEntity> findByTenantIdAndNombre(UUID tenantId, String nombre);
    
    Page<MiEntity> findByTenantId(UUID tenantId, Pageable pageable);
    
    boolean existsByNombre(String nombre);
    
    long countByTenantIdAndStatus(UUID tenantId, Status status);
}
```

### 7. Excepciones Personalizadas

```java
public class MiEntityNotFoundException extends RuntimeException {
    public MiEntityNotFoundException(String entityType, String id) {
        super(String.format("%s not found with id: %s", entityType, id));
    }
}

public class MiBusinessException extends RuntimeException {
    public MiBusinessException(String message) {
        super(message);
    }
}
```

---

## ⚠️ Errores Comunes a Evitar

### 1. ❌ NO Duplicar Métodos con Diferentes Firmas
```java
// INCORRECTO - Causa error de compilación
public void assignReel(UUID unitId, UUID reelId) { }
public ReelDto assignReel(UUID unitId, UUID reelId) { }  // Duplicado!
```

### 2. ❌ NO Declarar Logger Manualmente con @Slf4j
```java
// INCORRECTO
@Slf4j
public class MiClase {
    private static final Logger log = LoggerFactory.getLogger(MiClase.class);  // Conflicto!
}
```

### 3. ❌ NO Retornar void en Servicios de Actualización
```java
// INCORRECTO
@Transactional
public void update(UUID id, Entity entity) {
    repository.save(entity);
    // No retorna la entidad actualizada!
}
```

### 4. ❌ NO Olvidar @Transactional
```java
// INCORRECTO - Operaciones sin transacción
public MiDto create(Entity entity) {
    return repository.save(entity);  // Sin @Transactional!
}
```

### 5. ❌ NO Usar Types Ambiguos en Enums
```java
// INCORRECTO
public enum Status {
    ACTIVE, INACTIVE;  // Sin valores claros
}

// CORRECTO
@Enumerated(EnumType.STRING)
private Status status;
```

---

## 🔍 Patrón de Servicio Completo (Ejemplo Real)

```java
package org.thingsboard.nexus.ct.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CTUnitService {
    
    private final CTUnitRepository unitRepository;
    private final CTReelRepository reelRepository;
    
    // MÉTODOS DE LECTURA (readOnly = true)
    @Transactional(readOnly = true)
    public CTUnitDto getById(UUID id) {
        log.debug("Getting CT Unit by id: {}", id);
        CTUnit unit = unitRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", id.toString()));
        return CTUnitDto.fromEntity(unit);
    }
    
    @Transactional(readOnly = true)
    public Page<CTUnitDto> getByTenant(UUID tenantId, Pageable pageable) {
        log.debug("Getting CT Units for tenant: {}", tenantId);
        Page<CTUnit> units = unitRepository.findByTenantId(tenantId, pageable);
        return units.map(CTUnitDto::fromEntity);
    }
    
    // MÉTODOS DE ESCRITURA (retornan DTO actualizado)
    @Transactional
    public CTUnitDto create(CTUnit unit) {
        log.info("Creating new CT Unit: {}", unit.getUnitCode());
        
        // Validaciones
        if (unitRepository.existsByUnitCode(unit.getUnitCode())) {
            throw new CTBusinessException("Unit code already exists: " + unit.getUnitCode());
        }
        
        // Timestamps
        if (unit.getCreatedTime() == null) {
            unit.setCreatedTime(System.currentTimeMillis());
        }
        
        // Guardar y retornar DTO
        CTUnit savedUnit = unitRepository.save(unit);
        log.info("CT Unit created successfully: {}", savedUnit.getId());
        return CTUnitDto.fromEntity(savedUnit);
    }
    
    @Transactional
    public CTUnitDto update(UUID id, CTUnit updatedUnit) {
        log.info("Updating CT Unit: {}", id);
        
        // Buscar entidad existente
        CTUnit existingUnit = unitRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", id.toString()));
        
        // Validaciones de negocio
        if (!existingUnit.getUnitCode().equals(updatedUnit.getUnitCode()) &&
            unitRepository.existsByUnitCode(updatedUnit.getUnitCode())) {
            throw new CTBusinessException("Unit code already exists");
        }
        
        // Actualizar campos
        existingUnit.setUnitName(updatedUnit.getUnitName());
        existingUnit.setUpdatedTime(System.currentTimeMillis());
        
        // Guardar y retornar DTO
        CTUnit savedUnit = unitRepository.save(existingUnit);
        log.info("CT Unit updated successfully: {}", savedUnit.getId());
        return CTUnitDto.fromEntity(savedUnit);
    }
    
    @Transactional
    public void delete(UUID id) {
        log.info("Deleting CT Unit: {}", id);
        
        CTUnit unit = unitRepository.findById(id)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", id.toString()));
        
        // Validaciones antes de borrar
        if (unit.getCurrentReelId() != null) {
            throw new CTBusinessException("Cannot delete unit with assigned reel");
        }
        
        unitRepository.delete(unit);
        log.info("CT Unit deleted successfully: {}", id);
    }
    
    // OPERACIONES ESPECIALIZADAS (retornan DTO)
    @Transactional
    public CTUnitDto assignReel(UUID unitId, UUID reelId) {
        log.info("Assigning reel {} to unit {}", reelId, unitId);
        
        // Buscar entidades
        CTUnit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", unitId.toString()));
        CTReel reel = reelRepository.findById(reelId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Reel", reelId.toString()));
        
        // Validaciones de negocio
        if (unit.getCurrentReelId() != null) {
            throw new CTBusinessException("Unit already has a reel assigned");
        }
        if (!reel.getStatus().equals(ReelStatus.AVAILABLE)) {
            throw new CTBusinessException("Reel is not available");
        }
        
        // Actualizar ambas entidades
        unit.setCurrentReelId(reelId);
        unit.setReelCoupledDate(System.currentTimeMillis());
        CTUnit savedUnit = unitRepository.save(unit);
        
        reel.setStatus(ReelStatus.IN_USE);
        reel.setCurrentUnitId(unitId);
        reelRepository.save(reel);
        
        log.info("Reel assigned successfully");
        return CTUnitDto.fromEntity(savedUnit);  // SIEMPRE retornar DTO
    }
}
```

---

## 🎯 Reglas Críticas de Implementación

### ✅ Regla #1: Servicios Retornan DTOs
**Todos los métodos de servicio que modifican datos DEBEN retornar el DTO actualizado.**

```java
// ✅ CORRECTO
@Transactional
public EntityDto create(Entity entity) {
    Entity saved = repository.save(entity);
    return EntityDto.fromEntity(saved);
}

// ❌ INCORRECTO
@Transactional
public void create(Entity entity) {
    repository.save(entity);
}
```

### ✅ Regla #2: No Duplicar Métodos
**Un método con la misma firma NO puede existir dos veces, incluso con diferentes tipos de retorno.**

```java
// ❌ ERROR DE COMPILACIÓN
public void assignReel(UUID unitId, UUID reelId) { }
public ReelDto assignReel(UUID unitId, UUID reelId) { }  // DUPLICADO!

// ✅ CORRECTO - Solo una versión
public ReelDto assignReel(UUID unitId, UUID reelId) { }
```

### ✅ Regla #3: @Slf4j es Suficiente
**No declarar logger manualmente cuando se usa @Slf4j.**

```java
// ❌ INCORRECTO
@Slf4j
public class MiClase {
    private static final Logger log = LoggerFactory.getLogger(MiClase.class);
}

// ✅ CORRECTO
@Slf4j
public class MiClase {
    // @Slf4j ya provee el campo 'log'
}
```

### ✅ Regla #4: Usar @Transactional Correctamente
```java
// Lectura
@Transactional(readOnly = true)
public EntityDto getById(UUID id) { }

// Escritura
@Transactional
public EntityDto create(Entity entity) { }
```

### ✅ Regla #5: Validar Antes de Guardar
```java
@Transactional
public EntityDto create(Entity entity) {
    // 1. Validaciones de negocio
    if (repository.existsByCode(entity.getCode())) {
        throw new BusinessException("Code already exists");
    }
    
    // 2. Timestamps
    if (entity.getCreatedTime() == null) {
        entity.setCreatedTime(System.currentTimeMillis());
    }
    
    // 3. Guardar
    Entity saved = repository.save(entity);
    
    // 4. Retornar DTO
    return EntityDto.fromEntity(saved);
}
```

---

## 🔧 Checklist de Desarrollo

### Antes de Compilar
- [ ] Todas las clases Service tienen `@Service`, `@RequiredArgsConstructor`, `@Slf4j`
- [ ] No hay declaraciones manuales de `Logger` con `@Slf4j`
- [ ] Métodos de escritura retornan DTOs, no `void`
- [ ] No hay métodos duplicados (misma firma)
- [ ] Todos los métodos tienen `@Transactional` apropiado
- [ ] DTOs tienen método estático `fromEntity()`
- [ ] Entidades JPA tienen `@PrePersist` y `@PreUpdate` para timestamps
- [ ] Repositorios extienden `JpaRepository<Entity, UUID>`
- [ ] Controladores usan `ResponseEntity<Dto>` en respuestas

### Validación de Compilación
```bash
# 1. Compilar solo el módulo
mvn clean install -DskipTests -pl common/mi-modulo -am

# 2. Compilar proyecto completo
mvn clean install -DskipTests

# 3. Verificar JAR generado
ls -lh common/mi-modulo/target/*.jar
```

---

## 📚 Referencia de Módulos Core de TB

### Ejemplos para Estudiar
- **`common/data/`**: Modelos de datos base
- **`common/dao-api/`**: Interfaces de DAO
- **`dao/`**: Implementaciones de servicios (BaseAssetService, BaseDeviceService)
- **`application/controller/`**: Controladores REST (AssetController, DeviceController)

### Comando para Buscar Ejemplos
```bash
# Buscar servicios similares
find dao/src/main/java -name "*Service.java" | head -10

# Buscar controladores similares
find application/src/main/java -name "*Controller.java" | head -10

# Ver ejemplo de Asset
cat dao/src/main/java/org/thingsboard/server/dao/asset/BaseAssetService.java
```

---

## 🚀 Flujo de Desarrollo Recomendado

### Fase 1: Planificación
1. Definir entidades del dominio
2. Diseñar API REST
3. Crear esquema de base de datos

### Fase 2: Backend Core
1. **Crear módulo Maven**
   ```bash
   mkdir -p common/mi-modulo/src/main/java/org/thingsboard/nexus/mimodulo/{model,dto,repository,service,controller,exception}
   ```

2. **Configurar pom.xml** (usar template arriba)

3. **Crear entidades JPA** (en `model/`)

4. **Crear DTOs** (en `dto/`)

5. **Crear repositorios** (en `repository/`)

6. **Crear servicios** (en `service/`)
   - TODOS los métodos de escritura retornan DTOs
   - NO duplicar métodos
   - Usar solo `@Slf4j`

7. **Crear controladores** (en `controller/`)

8. **Registrar módulo** en `common/pom.xml`

### Fase 3: Validación
```bash
# Compilar módulo
mvn clean install -DskipTests -pl common/mi-modulo -am

# Compilar proyecto completo
mvn clean install -DskipTests
```

### Fase 4: Testing
```bash
# Ejecutar tests del módulo
mvn test -pl common/mi-modulo
```

---

## 🛠️ Troubleshooting

### Error: "method X is already defined"
**Causa**: Método duplicado con misma firma  
**Solución**: Eliminar una de las definiciones, mantener la que retorna DTO

### Error: "Field 'log' already exists"
**Causa**: Logger declarado manualmente con `@Slf4j`  
**Solución**: Eliminar declaración manual de logger

### Error: "incompatible types: void cannot be converted to Dto"
**Causa**: Método del servicio retorna `void` pero controller espera `Dto`  
**Solución**: Cambiar firma del método para retornar `Dto`

### Error: "BUILD FAILURE" en ui-ngx con archivos SVG
**Causa**: Recursos generados corruptos o enlaces simbólicos rotos  
**Solución**: 
```bash
rm -rf ui-ngx/target/generated-resources
mvn clean install -DskipTests
```

### Error: Module not found
**Causa**: Módulo no registrado en parent POM  
**Solución**: Agregar `<module>mi-modulo</module>` en `common/pom.xml`

---

## 📊 Comparación: Antes vs Después

### ❌ Implementación Incorrecta (Causa Errores)
```java
@Service
@Slf4j
public class CTUnitService {
    private static final Logger log = LoggerFactory.getLogger(CTUnitService.class);  // Duplicado!
    
    @Transactional
    public void assignReel(UUID unitId, UUID reelId) {  // void!
        // ...
        unitRepository.save(unit);
    }
    
    @Transactional
    public CTUnitDto assignReel(UUID unitId, UUID reelId) {  // DUPLICADO!
        // ...
        return CTUnitDto.fromEntity(savedUnit);
    }
}
```

### ✅ Implementación Correcta (Compila Sin Errores)
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CTUnitService {
    // NO declarar logger manualmente
    
    private final CTUnitRepository unitRepository;
    
    @Transactional
    public CTUnitDto assignReel(UUID unitId, UUID reelId) {
        log.info("Assigning reel {} to unit {}", reelId, unitId);
        
        CTUnit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new CTEntityNotFoundException("CT Unit", unitId.toString()));
        
        // Lógica de negocio
        unit.setCurrentReelId(reelId);
        CTUnit savedUnit = unitRepository.save(unit);
        
        // SIEMPRE retornar DTO
        return CTUnitDto.fromEntity(savedUnit);
    }
}
```

---

## 📖 Caso de Estudio: Corrección del Módulo Coiled Tubing

### Problema Original
```
[ERROR] method assignReel(UUID,UUID) is already defined in class CTUnitService
[ERROR] method detachReel(UUID) is already defined in class CTUnitService
[ERROR] incompatible types: void cannot be converted to CTUnitDto
```

### Solución Aplicada
1. **Identificar métodos duplicados** en `CTUnitService.java`
2. **Eliminar versiones void** (líneas 157-209)
3. **Mantener versiones que retornan DTOs** (líneas 248-302)
4. **Limpiar recursos generados** del UI: `rm -rf ui-ngx/target/generated-resources`
5. **Recompilar**: `mvn clean install -DskipTests`

### Resultado
```
[INFO] Coiled Tubing Module ............................... SUCCESS [  0.576 s]
[INFO] BUILD SUCCESS
```

---

## 🎓 Lecciones Aprendidas

### 1. Seguir Convenciones de ThingsBoard
- Los servicios en TB **siempre retornan DTOs** para operaciones de escritura
- Los controladores necesitan devolver la entidad actualizada al cliente
- No reinventar patrones, seguir los ejemplos de `dao/` y `application/controller/`

### 2. Evitar Refactorizaciones Innecesarias
- Si un método ya existe y funciona, no crear versiones alternativas
- Mantener una sola firma por método

### 3. Usar Herramientas de Lombok Correctamente
- `@Slf4j` provee el logger automáticamente
- `@RequiredArgsConstructor` inyecta dependencias finales
- No mezclar anotaciones con código manual equivalente

### 4. Validar Antes de Integrar
```bash
# Compilar módulo aislado primero
mvn clean install -DskipTests -pl common/mi-modulo -am

# Solo si funciona, compilar todo
mvn clean install -DskipTests
```

---

## 📝 Template de Módulo Nuevo

### Estructura Inicial
```bash
cd /home/diazhh/dev/nexus
mkdir -p common/mi-modulo/src/main/java/org/thingsboard/nexus/mimodulo/{model,dto,repository,service,controller,exception}
mkdir -p common/mi-modulo/src/main/resources
mkdir -p common/mi-modulo/src/test/java
```

### pom.xml Base
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.thingsboard</groupId>
        <artifactId>common</artifactId>
        <version>4.3.0-RC</version>
    </parent>

    <groupId>org.thingsboard.common</groupId>
    <artifactId>mi-modulo</artifactId>
    <packaging>jar</packaging>
    <name>Mi Módulo</name>
    
    <!-- Copiar dependencias y plugins de ct-module/pom.xml -->
</project>
```

### Registrar en common/pom.xml
```xml
<modules>
    <!-- ... -->
    <module>mi-modulo</module>
</modules>
```

---

## ✅ Verificación Final

### Comandos de Validación
```bash
# 1. Compilar módulo específico
mvn clean install -DskipTests -pl common/mi-modulo -am

# 2. Verificar JAR generado
ls -lh common/mi-modulo/target/*.jar

# 3. Compilar proyecto completo
mvn clean install -DskipTests

# 4. Buscar errores específicos
mvn clean install -DskipTests 2>&1 | grep -i "error\|failure"
```

### Indicadores de Éxito
```
[INFO] Mi Módulo .......................................... SUCCESS
[INFO] BUILD SUCCESS
```

---

## 🔗 Referencias

- **Código ThingsBoard Core**: `/dao/src/main/java/org/thingsboard/server/dao/`
- **Controladores TB**: `/application/src/main/java/org/thingsboard/server/controller/`
- **Módulo Coiled Tubing**: `/common/ct-module/` (ejemplo de referencia)
- **Documentación Maven**: https://maven.apache.org/guides/
- **Spring Boot**: https://spring.io/projects/spring-boot
- **Lombok**: https://projectlombok.org/features/

---

**Autor**: Nexus Development Team  
**Fecha**: Enero 2026  
**Versión**: 1.0.0  
**Última actualización**: Después de resolver errores de compilación del módulo CT
