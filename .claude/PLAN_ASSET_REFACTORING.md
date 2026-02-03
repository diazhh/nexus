# Plan de Refactorización: Migración a Arquitectura de Assets

## Objetivo

Migrar los módulos **DR (Drilling)** y **CT (Coiled Tubing)** para usar la misma arquitectura basada en assets que usa el módulo **RV (Reservoir)**, donde cada activo físico es un gemelo digital representado como un Asset de ThingsBoard.

---

## Filosofía de Diseño

### ✅ Debe ser Asset (Gemelo Digital)
- Activos físicos que tienen identidad, ubicación, estado
- Equipos que pueden tener telemetría en tiempo real
- Elementos que forman parte de una jerarquía de activos
- Componentes que necesitan plantillas (templates)

### ✅ Debe ser Tabla
- Datos transaccionales (jobs, operaciones)
- Registros históricos/logs (surveys, mud logs, fatigue logs)
- Eventos con timestamps
- Datos que requieren queries SQL complejas con aggregaciones

---

## Análisis del Módulo DR (Drilling)

### Estado Actual

| Entidad | Tipo Actual | Tabla | Asset |
|---------|-------------|-------|-------|
| Rig | Híbrido | `dr_rigs` | ✅ asset_id + child assets |
| BHA | Híbrido | `dr_bhas` | ✅ asset_id |
| Run | Tabla | `dr_runs` | ❌ Solo refs |
| Survey | Tabla | `dr_directional_surveys` | ❌ |
| Mud Log | Tabla | `dr_mud_logs` | ❌ |

### Cambios Propuestos

| Entidad | Acción | Justificación |
|---------|--------|---------------|
| **Rig** | 🔄 Eliminar tabla `dr_rigs` | Es un activo físico → 100% Asset |
| **BHA** | 🔄 Eliminar tabla `dr_bhas` | Es un activo físico → 100% Asset |
| **Run** | ✅ Mantener tabla | Es una operación transaccional |
| **Survey** | ✅ Mantener tabla | Son registros históricos de medición |
| **Mud Log** | ✅ Mantener tabla | Son registros históricos geológicos |

### Tareas DR

#### Fase 1: Preparación
- [ ] 1.1 Crear DTOs para `DrRigDto` y `DrBhaDto` (similar a RV)
- [ ] 1.2 Definir tipos de asset: `DR_RIG`, `DR_BHA`
- [ ] 1.3 Mapear columnas de tablas actuales a atributos de asset

#### Fase 2: Servicios
- [ ] 2.1 Crear `DrAssetService` (basado en RvAssetService)
- [ ] 2.2 Crear `DrAttributeService` (basado en RvAttributeService)
- [ ] 2.3 Refactorizar `DrRigService` para usar assets en lugar de JPA
- [ ] 2.4 Refactorizar `DrBhaService` para usar assets en lugar de JPA

#### Fase 3: Repositorios
- [ ] 3.1 Eliminar `DrRigRepository` (ya no se necesita)
- [ ] 3.2 Eliminar `DrBhaRepository` (ya no se necesita)
- [ ] 3.3 Actualizar `DrRunRepository` para referenciar assets por ID

#### Fase 4: Controladores
- [ ] 4.1 Actualizar `DrRigController` para usar DTOs y AssetService
- [ ] 4.2 Actualizar `DrBhaController` para usar DTOs y AssetService

#### Fase 5: Migración de Datos
- [ ] 5.1 Crear script de migración: `dr_rigs` → assets + attributes
- [ ] 5.2 Crear script de migración: `dr_bhas` → assets + attributes
- [ ] 5.3 Actualizar FKs en `dr_runs` para apuntar a asset IDs

#### Fase 6: Limpieza
- [ ] 6.1 Eliminar entidades JPA: `DrRig.java`, `DrBha.java`
- [ ] 6.2 Eliminar tabla `dr_rigs` del schema
- [ ] 6.3 Eliminar tabla `dr_bhas` del schema
- [ ] 6.4 Actualizar tests

---

## Análisis del Módulo CT (Coiled Tubing)

### Estado Actual

| Entidad | Tipo Actual | Tabla | Asset |
|---------|-------------|-------|-------|
| Unit | Híbrido | `ct_units` | ✅ asset_id + child assets |
| Reel | Híbrido | `ct_reels` | ✅ asset_id |
| Job | Tabla | `ct_jobs` | ❌ |
| Job Phase | Tabla | `ct_job_phases` | ❌ |
| Job Event | Tabla | `ct_job_events` | ❌ |
| Fatigue Log | Tabla | `ct_fatigue_log` | ❌ |

### Cambios Propuestos

| Entidad | Acción | Justificación |
|---------|--------|---------------|
| **Unit** | 🔄 Eliminar tabla `ct_units` | Es un activo físico → 100% Asset |
| **Reel** | 🔄 Eliminar tabla `ct_reels` | Es un activo físico → 100% Asset |
| **Job** | ✅ Mantener tabla | Es una operación transaccional |
| **Job Phase** | ✅ Mantener tabla | Son registros de fases de operación |
| **Job Event** | ✅ Mantener tabla | Son eventos/incidentes |
| **Fatigue Log** | ✅ Mantener tabla | Son cálculos históricos de fatiga |

### Tareas CT

#### Fase 1: Preparación
- [ ] 1.1 Crear DTOs para `CtUnitDto` y `CtReelDto` (similar a RV)
- [ ] 1.2 Definir tipos de asset: `CT_UNIT`, `CT_REEL`
- [ ] 1.3 Mapear columnas de tablas actuales a atributos de asset

#### Fase 2: Servicios
- [ ] 2.1 Crear `CtAssetService` (basado en RvAssetService)
- [ ] 2.2 Actualizar `CTAttributeService` existente
- [ ] 2.3 Refactorizar `CTUnitService` para usar assets en lugar de JPA
- [ ] 2.4 Refactorizar `CTReelService` para usar assets en lugar de JPA

#### Fase 3: Repositorios
- [ ] 3.1 Eliminar `CTUnitRepository` (ya no se necesita)
- [ ] 3.2 Eliminar `CTReelRepository` (ya no se necesita)
- [ ] 3.3 Actualizar `CTJobRepository` para referenciar assets por ID
- [ ] 3.4 Actualizar `CTFatigueLogRepository` para referenciar reel como asset ID

#### Fase 4: Controladores
- [ ] 4.1 Actualizar `CTUnitController` para usar DTOs y AssetService
- [ ] 4.2 Actualizar `CTReelController` para usar DTOs y AssetService

#### Fase 5: Migración de Datos
- [ ] 5.1 Crear script de migración: `ct_units` → assets + attributes
- [ ] 5.2 Crear script de migración: `ct_reels` → assets + attributes
- [ ] 5.3 Actualizar FKs en `ct_jobs` para apuntar a asset IDs
- [ ] 5.4 Actualizar FKs en `ct_fatigue_log` para apuntar a asset IDs

#### Fase 6: Limpieza
- [ ] 6.1 Eliminar entidades JPA: `CTUnit.java`, `CTReel.java`
- [ ] 6.2 Eliminar tabla `ct_units` del schema
- [ ] 6.3 Eliminar tabla `ct_reels` del schema
- [ ] 6.4 Actualizar tests

---

## Arquitectura Final

### Módulo DR

```
Assets (ThingsBoard core)
├── DR_RIG (asset type)
│   ├── Atributos: rig_code, rig_name, operational_status, specs...
│   └── Child Assets: drawworks, top_drive, mud_pumps, bop...
│
└── DR_BHA (asset type)
    └── Atributos: bha_number, bit_info, motor_info, components...

Tablas (Nexus DR)
├── dr_runs (referencia assets por UUID)
├── dr_directional_surveys (FK a dr_runs)
└── dr_mud_logs (FK a dr_runs)
```

### Módulo CT

```
Assets (ThingsBoard core)
├── CT_UNIT (asset type)
│   ├── Atributos: unit_code, operational_status, specs...
│   └── Child Assets: hydraulic_system, injection_system, control_system...
│
└── CT_REEL (asset type)
    └── Atributos: reel_code, tubing_specs, fatigue_data...

Tablas (Nexus CT)
├── ct_jobs (referencia assets por UUID)
├── ct_job_phases (FK a ct_jobs)
├── ct_job_events (FK a ct_jobs)
└── ct_fatigue_log (referencia reel asset por UUID)
```

---

## Mapeo de Atributos

### DR_RIG Attributes

| Columna Actual | Atributo Asset | Tipo |
|----------------|----------------|------|
| rig_code | rig_code | STRING |
| rig_name | rig_name | STRING |
| rig_type | rig_type | STRING |
| operational_status | operational_status | STRING |
| contractor | contractor | STRING |
| manufacturer | manufacturer | STRING |
| model | model | STRING |
| year_built | year_built | LONG |
| max_hookload_lbs | max_hookload_lbs | DOUBLE |
| max_rotary_torque_ft_lbs | max_rotary_torque_ft_lbs | DOUBLE |
| max_depth_capability_ft | max_depth_capability_ft | DOUBLE |
| current_location | current_location | STRING |
| latitude | latitude | DOUBLE |
| longitude | longitude | DOUBLE |
| total_wells_drilled | total_wells_drilled | LONG |
| total_footage_drilled_ft | total_footage_drilled_ft | DOUBLE |
| total_npt_hours | total_npt_hours | DOUBLE |
| total_operational_hours | total_operational_hours | DOUBLE |
| metadata | metadata | JSON |

### DR_BHA Attributes

| Columna Actual | Atributo Asset | Tipo |
|----------------|----------------|------|
| bha_number | bha_number | STRING |
| bha_type | bha_type | STRING |
| is_directional | is_directional | BOOLEAN |
| bit_serial | bit_serial | STRING |
| bit_type | bit_type | STRING |
| bit_size_in | bit_size_in | DOUBLE |
| bit_iadc_code | bit_iadc_code | STRING |
| bit_manufacturer | bit_manufacturer | STRING |
| motor_* | motor_* | VARIOUS |
| rss_* | rss_* | VARIOUS |
| status | status | STRING |
| total_footage_drilled | total_footage_drilled | DOUBLE |
| components_json | components | JSON |

### CT_UNIT Attributes

| Columna Actual | Atributo Asset | Tipo |
|----------------|----------------|------|
| unit_code | unit_code | STRING |
| operational_status | operational_status | STRING |
| max_pressure_psi | max_pressure_psi | DOUBLE |
| max_tension_lbf | max_tension_lbf | DOUBLE |
| max_speed_ft_min | max_speed_ft_min | DOUBLE |
| total_operational_hours | total_operational_hours | DOUBLE |
| total_jobs_completed | total_jobs_completed | LONG |
| total_meters_deployed | total_meters_deployed | DOUBLE |
| current_location | current_location | STRING |
| latitude | latitude | DOUBLE |
| longitude | longitude | DOUBLE |
| maintenance_* | maintenance_* | VARIOUS |
| certification_* | certification_* | VARIOUS |
| current_reel_id | current_reel_asset_id | STRING (UUID) |

### CT_REEL Attributes

| Columna Actual | Atributo Asset | Tipo |
|----------------|----------------|------|
| reel_code | reel_code | STRING |
| tubing_od_inch | tubing_od_inch | DOUBLE |
| tubing_id_inch | tubing_id_inch | DOUBLE |
| wall_thickness_inch | wall_thickness_inch | DOUBLE |
| total_length_ft | total_length_ft | DOUBLE |
| material_grade | material_grade | STRING |
| material_yield_strength_psi | material_yield_strength_psi | DOUBLE |
| material_tensile_strength_psi | material_tensile_strength_psi | DOUBLE |
| youngs_modulus_psi | youngs_modulus_psi | DOUBLE |
| accumulated_fatigue_percent | accumulated_fatigue_percent | DOUBLE |
| total_cycles | total_cycles | LONG |
| estimated_remaining_cycles | estimated_remaining_cycles | LONG |
| status | status | STRING |
| current_unit_id | current_unit_asset_id | STRING (UUID) |
| inspection_* | inspection_* | VARIOUS |
| condition_* | condition_* | VARIOUS |

---

## Templates Requeridos

### Templates DR
1. **DR_RIG_TEMPLATE** - Template para crear rigs con child assets
2. **DR_BHA_TEMPLATE** - Template para crear BHAs

### Templates CT
1. **CT_UNIT_TEMPLATE** - Template para crear unidades CT con child assets
2. **CT_REEL_TEMPLATE** - Template para crear reels

---

## Orden de Ejecución Recomendado

1. **Semana 1**: Preparación DR (Fases 1-2)
2. **Semana 2**: Repositorios y Controladores DR (Fases 3-4)
3. **Semana 3**: Migración y Limpieza DR (Fases 5-6)
4. **Semana 4**: Preparación CT (Fases 1-2)
5. **Semana 5**: Repositorios y Controladores CT (Fases 3-4)
6. **Semana 6**: Migración y Limpieza CT (Fases 5-6)

---

## Riesgos y Mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Pérdida de datos en migración | Crear backups antes de migrar; scripts reversibles |
| Performance de queries con assets | Usar índices en attribute_kv; cache en memoria |
| Compatibilidad con código existente | Mantener interfaces de servicio; cambiar solo implementación |
| Referencias cruzadas entre módulos | dr_runs y ct_jobs siguen referenciando por UUID |

---

## Archivos a Modificar/Eliminar

### Módulo DR

**Eliminar:**
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/model/DrRig.java`
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/model/DrBha.java`
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/repository/DrRigRepository.java`
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/repository/DrBhaRepository.java`

**Crear:**
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/dto/DrRigDto.java`
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/dto/DrBhaDto.java`
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/service/DrAssetService.java`

**Modificar:**
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/service/DrRigService.java`
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/service/DrBhaService.java`
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/controller/DrRigController.java`
- `common/dr-module/src/main/java/org/thingsboard/nexus/dr/controller/DrBhaController.java`
- `dao/src/main/resources/sql/schema-dr.sql` (eliminar tablas dr_rigs, dr_bhas)

### Módulo CT

**Eliminar:**
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/model/CTUnit.java`
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/model/CTReel.java`
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/repository/CTUnitRepository.java`
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/repository/CTReelRepository.java`

**Crear:**
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/dto/CtUnitDto.java`
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/dto/CtReelDto.java`
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/service/CtAssetService.java`

**Modificar:**
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/service/CTUnitService.java`
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/service/CTReelService.java`
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/controller/CTUnitController.java`
- `common/ct-module/src/main/java/org/thingsboard/nexus/ct/controller/CTReelController.java`
- `common/ct-module/src/main/resources/sql/migrations/V1__initial_ct_schema.sql` (eliminar tablas)

---

## Pruebas de Validación

### Credenciales de Prueba
- **Usuario Tenant Default**: `tenant@thingsboard.org` / `tenant`
- **URL Base**: `http://localhost:8080`

### Pruebas DR Module

#### 1. Autenticación
```bash
# Obtener token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant@thingsboard.org","password":"tenant"}'
```

#### 2. Crear Asset Profile para DR_RIG
```bash
# Crear perfil de asset para rigs
curl -X POST http://localhost:8080/api/assetProfile \
  -H "Content-Type: application/json" \
  -H "X-Authorization: Bearer {TOKEN}" \
  -d '{
    "name": "DR_RIG",
    "description": "Drilling Rig Profile",
    "defaultQueueName": "Main"
  }'
```

#### 3. Crear Asset Profile para DR_BHA
```bash
# Crear perfil de asset para BHAs
curl -X POST http://localhost:8080/api/assetProfile \
  -H "Content-Type: application/json" \
  -H "X-Authorization: Bearer {TOKEN}" \
  -d '{
    "name": "DR_BHA",
    "description": "Bottom Hole Assembly Profile",
    "defaultQueueName": "Main"
  }'
```

#### 4. Probar API de Rigs
```bash
# Crear un rig
curl -X POST http://localhost:8080/api/nexus/dr/rigs \
  -H "Content-Type: application/json" \
  -H "X-Authorization: Bearer {TOKEN}" \
  -d '{
    "rigCode": "RIG-001",
    "rigName": "Test Rig 1",
    "rigType": "LAND",
    "operationalStatus": "STANDBY"
  }'

# Listar rigs
curl -X GET "http://localhost:8080/api/nexus/dr/rigs?pageSize=10&page=0" \
  -H "X-Authorization: Bearer {TOKEN}"

# Obtener rig por ID
curl -X GET http://localhost:8080/api/nexus/dr/rigs/{assetId} \
  -H "X-Authorization: Bearer {TOKEN}"
```

#### 5. Probar API de BHAs
```bash
# Crear un BHA
curl -X POST http://localhost:8080/api/nexus/dr/bhas \
  -H "Content-Type: application/json" \
  -H "X-Authorization: Bearer {TOKEN}" \
  -d '{
    "bhaNumber": "BHA-001",
    "bhaType": "ROTARY",
    "bitSizeIn": 8.5,
    "bitType": "PDC"
  }'

# Listar BHAs
curl -X GET "http://localhost:8080/api/nexus/dr/bhas?pageSize=10&page=0" \
  -H "X-Authorization: Bearer {TOKEN}"
```

### Pruebas CT Module

#### 1. Crear Asset Profile para CT_UNIT
```bash
curl -X POST http://localhost:8080/api/assetProfile \
  -H "Content-Type: application/json" \
  -H "X-Authorization: Bearer {TOKEN}" \
  -d '{
    "name": "CT_UNIT",
    "description": "Coiled Tubing Unit Profile",
    "defaultQueueName": "Main"
  }'
```

#### 2. Crear Asset Profile para CT_REEL
```bash
curl -X POST http://localhost:8080/api/assetProfile \
  -H "Content-Type: application/json" \
  -H "X-Authorization: Bearer {TOKEN}" \
  -d '{
    "name": "CT_REEL",
    "description": "Coiled Tubing Reel Profile",
    "defaultQueueName": "Main"
  }'
```

#### 3. Probar API de Units
```bash
# Crear una unidad CT
curl -X POST http://localhost:8080/api/nexus/ct/units \
  -H "Content-Type: application/json" \
  -H "X-Authorization: Bearer {TOKEN}" \
  -d '{
    "unitCode": "CTU-001",
    "operationalStatus": "OPERATIONAL",
    "maxPressurePsi": 15000,
    "maxTensionLbf": 100000
  }'

# Listar units
curl -X GET "http://localhost:8080/api/nexus/ct/units?pageSize=10&page=0" \
  -H "X-Authorization: Bearer {TOKEN}"
```

#### 4. Probar API de Reels
```bash
# Crear un reel
curl -X POST http://localhost:8080/api/nexus/ct/reels \
  -H "Content-Type: application/json" \
  -H "X-Authorization: Bearer {TOKEN}" \
  -d '{
    "reelCode": "REEL-001",
    "tubingOdInch": 2.0,
    "tubingIdInch": 1.75,
    "totalLengthFt": 25000,
    "materialGrade": "CT-80"
  }'

# Listar reels
curl -X GET "http://localhost:8080/api/nexus/ct/reels?pageSize=10&page=0" \
  -H "X-Authorization: Bearer {TOKEN}"
```

### Verificación en Base de Datos

```sql
-- Verificar assets creados
SELECT id, name, type, label FROM asset WHERE type IN ('DR_RIG', 'DR_BHA', 'CT_UNIT', 'CT_REEL');

-- Verificar atributos
SELECT e.entity_id, e.attribute_key, e.str_value, e.long_value, e.dbl_value
FROM attribute_kv e
JOIN asset a ON e.entity_id = a.id
WHERE a.type IN ('DR_RIG', 'DR_BHA', 'CT_UNIT', 'CT_REEL');

-- Verificar que las tablas viejas ya no existen (después de migración)
-- SELECT * FROM dr_rigs; -- Debe fallar
-- SELECT * FROM dr_bhas; -- Debe fallar
-- SELECT * FROM ct_units; -- Debe fallar
-- SELECT * FROM ct_reels; -- Debe fallar
```

---

## Checklist de Validación Final

- [ ] Todos los rigs se crean como assets tipo `DR_RIG`
- [ ] Todos los BHAs se crean como assets tipo `DR_BHA`
- [ ] Todos los units se crean como assets tipo `CT_UNIT`
- [ ] Todos los reels se crean como assets tipo `CT_REEL`
- [ ] Templates funcionan correctamente para cada tipo
- [ ] dr_runs referencia correctamente a asset IDs
- [ ] ct_jobs referencia correctamente a asset IDs
- [ ] ct_fatigue_log referencia correctamente a reel asset ID
- [ ] APIs mantienen compatibilidad hacia atrás
- [ ] Tests pasan correctamente
- [ ] Datos migrados correctamente
