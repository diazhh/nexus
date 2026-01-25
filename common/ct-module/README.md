# Coiled Tubing Module - Implementation Progress

## Module Information
- **Module Name**: ct-module (Coiled Tubing Module)
- **Version**: 1.0.0-SNAPSHOT
- **Package**: org.thingsboard.nexus.ct

## Implementation Status

### ✅ Fase 0: Preparación (COMPLETADA)
- [x] Estructura de directorios creada
- [x] pom.xml configurado
- [x] Script de migración V1__initial_ct_schema.sql
- [x] 6 tablas principales: ct_units, ct_reels, ct_jobs, ct_job_phases, ct_job_events, ct_fatigue_log

### ✅ Fase 1.1: Entidades JPA (COMPLETADA)
- [x] Enums: UnitStatus, ReelStatus, JobStatus
- [x] CTUnit.java (entity)
- [x] CTReel.java (entity)
- [x] CTJob.java (entity)

### ✅ Fase 1.2: Repositorios JPA (COMPLETADA)
- [x] CTUnitRepository
- [x] CTReelRepository
- [x] CTJobRepository

### ✅ Fase 1.3: DTOs (COMPLETADA)
- [x] CTUnitDto
- [x] CTReelDto
- [x] CTJobDto

### ✅ Fase 1.4: Servicios de Negocio (COMPLETADA)
- [x] CTUnitService
- [x] CTReelService
- [x] CTJobService
- [x] Exception handling (CTException, CTEntityNotFoundException, CTBusinessException)

### ✅ Fase 1.5: Controllers REST (COMPLETADA)
- [x] CTUnitController
- [x] CTReelController
- [x] CTJobController

### ⏳ Fase 2: Rule Engine (PENDIENTE)
- [ ] Fatigue calculation rule chain
- [ ] Integration with ThingsBoard

### ⏳ Fase 3: Frontend (PENDIENTE)
- [ ] Angular module
- [ ] Components
- [ ] Services

## Database Schema

### Tables Created
1. **ct_units**: CT unit management
2. **ct_reels**: Reel inventory and lifecycle
3. **ct_jobs**: Job planning and execution
4. **ct_job_phases**: Job phase tracking
5. **ct_job_events**: Event logging during jobs
6. **ct_fatigue_log**: Fatigue calculation history

## Dependencies

### Backend
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- Lombok
- Jackson (JSON)

### Frontend (Pending)
- Angular 18+
- Angular Material
- RxJS

## Next Steps

1. ✅ ~~Complete business services (CTUnitService, CTReelService, CTJobService)~~
2. ✅ ~~Implement REST controllers~~
3. ✅ ~~Add validation and exception handling~~
4. Create unit tests
5. Implement fatigue calculation rule chain
6. Develop frontend components
7. Integration with application layer
8. Run `mvn license:format` after changes (comando a ejecutar siempre)

## Maven Configuration

El módulo está integrado en el reactor de Maven del proyecto:
- Agregado en `common/pom.xml` como `<module>ct-module</module>`
- GroupId: `org.thingsboard.common`
- ArtifactId: `ct-module`
- Version: `4.3.0-RC`

**Comando para aplicar headers de licencia:**
```bash
mvn license:format -pl common/ct-module
```

## API Endpoints

### CT Units
- `GET /api/ct/units/{id}` - Obtener unidad por ID
- `GET /api/ct/units/code/{unitCode}` - Obtener unidad por código
- `GET /api/ct/units/tenant/{tenantId}` - Listar unidades por tenant
- `GET /api/ct/units/tenant/{tenantId}/filter` - Filtrar unidades
- `POST /api/ct/units` - Crear nueva unidad
- `PUT /api/ct/units/{id}` - Actualizar unidad
- `PUT /api/ct/units/{id}/status` - Actualizar status de unidad
- `POST /api/ct/units/{unitId}/reel/{reelId}` - Asignar reel a unidad
- `DELETE /api/ct/units/{unitId}/reel` - Desacoplar reel de unidad
- `DELETE /api/ct/units/{id}` - Eliminar unidad

### CT Reels
- `GET /api/ct/reels/{id}` - Obtener reel por ID
- `GET /api/ct/reels/code/{reelCode}` - Obtener reel por código
- `GET /api/ct/reels/tenant/{tenantId}` - Listar reels por tenant
- `GET /api/ct/reels/tenant/{tenantId}/filter` - Filtrar reels
- `GET /api/ct/reels/tenant/{tenantId}/available` - Obtener reels disponibles
- `GET /api/ct/reels/tenant/{tenantId}/high-fatigue` - Obtener reels con alta fatiga
- `POST /api/ct/reels` - Crear nuevo reel
- `PUT /api/ct/reels/{id}` - Actualizar reel
- `PUT /api/ct/reels/{id}/status` - Actualizar status de reel
- `PUT /api/ct/reels/{id}/fatigue` - Actualizar fatiga de reel
- `PUT /api/ct/reels/{id}/inspection` - Registrar inspección de reel
- `PUT /api/ct/reels/{id}/retire` - Retirar reel
- `DELETE /api/ct/reels/{id}` - Eliminar reel

### CT Jobs
- `GET /api/ct/jobs/{id}` - Obtener trabajo por ID
- `GET /api/ct/jobs/number/{jobNumber}` - Obtener trabajo por número
- `GET /api/ct/jobs/tenant/{tenantId}` - Listar trabajos por tenant
- `GET /api/ct/jobs/tenant/{tenantId}/filter` - Filtrar trabajos
- `GET /api/ct/jobs/tenant/{tenantId}/active` - Obtener trabajos activos
- `GET /api/ct/jobs/tenant/{tenantId}/unit/{unitId}` - Trabajos por unidad
- `GET /api/ct/jobs/tenant/{tenantId}/reel/{reelId}` - Trabajos por reel
- `POST /api/ct/jobs` - Crear nuevo trabajo
- `PUT /api/ct/jobs/{id}` - Actualizar trabajo
- `POST /api/ct/jobs/{id}/start` - Iniciar trabajo
- `POST /api/ct/jobs/{id}/pause` - Pausar trabajo
- `POST /api/ct/jobs/{id}/resume` - Reanudar trabajo
- `POST /api/ct/jobs/{id}/complete` - Completar trabajo
- `POST /api/ct/jobs/{id}/cancel` - Cancelar trabajo
- `POST /api/ct/jobs/{id}/approve` - Aprobar trabajo
- `DELETE /api/ct/jobs/{id}` - Eliminar trabajo

## Metodologías Correctas de Implementación

### 1. Entidades JPA (Basado en DeviceEntity, AiModelEntity)

**Anotaciones correctas:**
```java
@Entity
@Table(name = "table_name")
@Data  // o @Getter @Setter
@EqualsAndHashCode(callSuper = true)  // si extiende BaseEntity
public class CTEntity extends BaseVersionedEntity<CTModel> {
    // Campos con anotaciones Jakarta
}
```

**Imports correctos:**
- `jakarta.persistence.*` (NO javax.persistence)
- `org.hibernate.annotations.Type`
- `io.hypersistence.utils.hibernate.type.json.JsonBinaryType`

**Campos UUID:**
```java
@Column(name = "tenant_id", columnDefinition = "uuid")
private UUID tenantId;
```

**Campos JSONB:**
```java
@Type(JsonBinaryType.class)
@Column(name = "metadata", columnDefinition = "jsonb")
private JsonNode metadata;
```

**Campos Enum:**
```java
@Enumerated(EnumType.STRING)
@Column(name = "status")
private Status status;
```

### 2. DTOs (Basado en DeviceDto, AssetDto)

**Estructura correcta:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CTDto {
    // Campos públicos o con getters/setters generados por Lombok
    
    public static CTDto fromEntity(CTEntity entity) {
        return CTDto.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            // ... usar getters de la entidad
            .build();
    }
}
```

**IMPORTANTE:** 
- Usar `entity.getField()` NO `entity.field` en fromEntity()
- Lombok genera getters automáticamente con @Data
- Usar @Builder para construcción fluida

### 3. Servicios (Basado en DeviceServiceImpl)

**Estructura correcta:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CTService {
    private final CTRepository repository;
    
    @Transactional(readOnly = true)
    public CTDto getById(UUID id) {
        log.debug("Getting entity by id: {}", id);
        CTEntity entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Entity", id.toString()));
        return CTDto.fromEntity(entity);
    }
    
    @Transactional
    public CTDto create(CTEntity entity) {
        log.info("Creating entity: {}", entity);
        // Validaciones
        CTEntity saved = repository.save(entity);
        return CTDto.fromEntity(saved);
    }
}
```

**IMPORTANTE:**
- Usar `@Transactional(readOnly = true)` para lecturas
- Usar `@Transactional` para escrituras
- Siempre retornar DTOs, no entidades
- Log con slf4j

### 4. Repositorios (Basado en DeviceRepository)

**Estructura correcta:**
```java
public interface CTRepository extends JpaRepository<CTEntity, UUID> {
    Optional<CTEntity> findByCode(String code);
    
    Page<CTEntity> findByTenantId(UUID tenantId, Pageable pageable);
    
    @Query("SELECT e FROM CTEntity e WHERE e.tenantId = :tenantId AND e.status = :status")
    List<CTEntity> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, 
                                           @Param("status") Status status);
}
```

### 5. Controllers (Basado en DeviceController)

**Estructura correcta:**
```java
@RestController
@RequestMapping("/api/ct")
@RequiredArgsConstructor
@Slf4j
public class CTController {
    private final CTService service;
    
    @GetMapping("/{id}")
    public ResponseEntity<CTDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }
    
    @PostMapping
    public ResponseEntity<CTDto> create(@Valid @RequestBody CTEntity entity) {
        return ResponseEntity.ok(service.create(entity));
    }
}
```

### 6. Validación Jakarta

**Imports correctos:**
- `jakarta.validation.Valid` (NO javax.validation)
- `jakarta.validation.constraints.*`

### 7. Dependencias Maven

**Requeridas en pom.xml:**
```xml
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 8. Errores Comunes a Evitar

❌ **NO HACER:**
- Usar `javax.persistence.*` → Usar `jakarta.persistence.*`
- Usar `entity.field` en DTOs → Usar `entity.getField()`
- Acceder a campos privados directamente → Usar getters
- Usar `@Type(type = "jsonb")` → Usar `@Type(JsonBinaryType.class)`
- Retornar void en servicios → Retornar DTOs
- Olvidar `@Transactional` en métodos de escritura

✅ **SÍ HACER:**
- Usar Jakarta EE 9+ (jakarta.*)
- Usar Lombok para generar getters/setters
- Usar @Builder en DTOs
- Usar @Transactional apropiadamente
- Validar entradas con @Valid
- Logging con slf4j
- Retornar DTOs desde servicios

## Notes

- All entities use UUID as primary key
- Timestamps are stored as epoch milliseconds (BIGINT)
- JSON data stored in JSONB columns
- All tables have tenant_id for multi-tenancy support
- License headers applied using `mvn license:format`
- Module integrated in Maven reactor at `common/ct-module`
