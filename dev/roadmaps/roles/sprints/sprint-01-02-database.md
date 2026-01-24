# Sprint 1-2: Base de Datos y Modelos (2 semanas)

## Objetivos del Sprint

Crear la estructura de base de datos y los modelos de datos para el sistema de roles y permisos.

## User Stories

### US-1: Como DBA, necesito crear las tablas de roles
**Prioridad:** Alta  
**Puntos:** 5

**Criterios de Aceptación:**
- [x] Tabla `role` creada con todos los campos requeridos
- [x] Constraints e índices aplicados
- [x] Foreign keys configuradas correctamente
- [x] Script SQL ejecuta sin errores

**Tareas:**
1. Diseñar esquema de tabla `role`
2. Crear script SQL `create_role_table.sql`
3. Agregar índices de performance
4. Agregar constraints de validación
5. Probar en base de datos de desarrollo
6. Documentar estructura en README

**Estimación:** 2 días

---

### US-2: Como DBA, necesito crear la tabla de permisos
**Prioridad:** Alta  
**Puntos:** 5

**Criterios de Aceptación:**
- [x] Tabla `role_permission` creada
- [x] Relación con `role` configurada (CASCADE)
- [x] Unique constraint en (role_id, resource_type, operation)
- [x] Índices para performance

**Tareas:**
1. Diseñar esquema de tabla `role_permission`
2. Crear script SQL `create_role_permission_table.sql`
3. Configurar foreign keys con CASCADE
4. Agregar índices composite
5. Probar inserciones y deletes
6. Validar cascade funciona correctamente

**Estimación:** 1 día

---

### US-3: Como desarrollador backend, necesito las entidades Java
**Prioridad:** Alta  
**Puntos:** 8

**Criterios de Aceptación:**
- [x] Clase `Role` implementada con todos los campos
- [x] Clase `RolePermission` implementada
- [x] IDs creados (`RoleId`, `RolePermissionId`)
- [x] Validaciones agregadas (@NotNull, @Length)
- [ ] Tests unitarios >= 90%

**Tareas:**
1. Crear `Role.java` en common/data
2. Crear `RolePermission.java` en common/data
3. Crear `RoleId.java` en common/data/id
4. Crear `RolePermissionId.java` en common/data/id
5. Agregar validaciones de bean validation
6. Implementar equals/hashCode
7. Crear `RoleTest.java`
8. Crear `RolePermissionTest.java`
9. Ejecutar tests y verificar coverage

**Estimación:** 3 días

**Archivos a Crear:**
```
common/data/src/main/java/org/thingsboard/server/common/data/security/
├── Role.java
└── RolePermission.java

common/data/src/main/java/org/thingsboard/server/common/data/id/
├── RoleId.java
└── RolePermissionId.java

common/data/src/test/java/org/thingsboard/server/common/data/security/
├── RoleTest.java
└── RolePermissionTest.java
```

---

### US-4: Como desarrollador backend, necesito los DAOs
**Prioridad:** Alta  
**Puntos:** 13

**Criterios de Aceptación:**
- [x] Interface `RoleDao` definida
- [x] Interface `RolePermissionDao` definida
- [x] Implementación JPA de ambos DAOs
- [x] Queries optimizadas con índices
- [ ] Tests de integración >= 85%

**Tareas:**
1. Crear interface `RoleDao.java`
2. Crear `RoleEntity.java` (JPA entity)
3. Crear `JpaRoleDao.java` (implementación)
4. Crear `RoleRepository.java` (Spring Data)
5. Crear interface `RolePermissionDao.java`
6. Crear `RolePermissionEntity.java`
7. Crear `JpaRolePermissionDao.java`
8. Crear `RolePermissionRepository.java`
9. Implementar queries custom si necesario
10. Crear `RoleDaoTest.java` (integration test)
11. Crear `RolePermissionDaoTest.java`
12. Setup TestContainers para tests
13. Verificar coverage >= 85%

**Estimación:** 4 días

**Archivos a Crear:**
```
dao/src/main/java/org/thingsboard/server/dao/role/
├── RoleDao.java
├── RolePermissionDao.java
└── BaseRoleDao.java (opcional)

dao/src/main/java/org/thingsboard/server/dao/sql/role/
├── RoleEntity.java
├── RolePermissionEntity.java
├── JpaRoleDao.java
├── JpaRolePermissionDao.java
├── RoleRepository.java
└── RolePermissionRepository.java

dao/src/test/java/org/thingsboard/server/dao/role/
├── RoleDaoTest.java
└── RolePermissionDaoTest.java
```

---

### US-5: Como DBA, necesito modificar tabla de usuarios
**Prioridad:** Alta  
**Puntos:** 3

**Criterios de Aceptación:**
- [x] Campo `role_id` agregado a `tb_user`
- [x] Foreign key a `role` configurada
- [x] Campo `authority` ahora nullable
- [x] Índice en `role_id` agregado
- [x] Script de migración creado

**Tareas:**
1. Crear script `alter_user_table.sql`
2. Agregar columna `role_id UUID`
3. Hacer `authority` nullable
4. Agregar FK constraint
5. Crear índice `idx_user_role_id`
6. Probar en DB de desarrollo
7. Crear script de rollback
8. Documentar cambios

**Estimación:** 1 día

---

### US-6: Como desarrollador, necesito datos de prueba
**Prioridad:** Media  
**Puntos:** 3

**Criterios de Aceptación:**
- [ ] Script de seed data creado
- [ ] Roles del sistema creados (SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER)
- [ ] Permisos asignados a roles del sistema
- [ ] Script ejecuta sin errores

**Tareas:**
1. Crear `seed_system_roles.sql`
2. Insertar rol "System Administrator"
3. Insertar rol "Tenant Administrator" (por tenant)
4. Insertar rol "Customer User" (por tenant)
5. Crear función `create_default_tenant_roles()`
6. Insertar permisos para cada rol
7. Crear trigger para nuevos tenants
8. Probar con tenant de prueba

**Estimación:** 2 días

---

## Tareas Técnicas

### T-1: Configurar ModelConstants
**Prioridad:** Alta  
**Estimación:** 2 horas

Agregar constantes para nuevas tablas:
```java
public static final String ROLE_TABLE_NAME = "role";
public static final String ROLE_PERMISSION_TABLE_NAME = "role_permission";
public static final String ROLE_NAME_PROPERTY = "name";
// etc.
```

---

### T-2: Actualizar EntityType Enum
**Prioridad:** Alta  
**Estimación:** 1 hora

Agregar `ROLE` a enum EntityType:
```java
public enum EntityType {
    // ... existentes
    ROLE,
    ROLE_PERMISSION
}
```

---

### T-3: Configurar Liquibase/Flyway (opcional)
**Prioridad:** Baja  
**Estimación:** 4 horas

Si se usa migración automática, configurar:
- Crear changelog para roles
- Versionar cambios
- Configurar rollback

---

## Tests

### Tests Unitarios

**RoleTest.java**
- [x] testRoleCreation
- [x] testRoleValidation
- [x] testRoleEquality
- [x] testRoleWithNullTenant
- [x] testRoleIsSystemFlag

**RolePermissionTest.java**
- [x] testPermissionCreation
- [x] testPermissionWithAllOperation
- [x] testPermissionMatching

### Tests de Integración

**RoleDaoTest.java**
- [x] testSaveRole
- [x] testFindByTenantId
- [x] testFindByTenantIdAndName
- [x] testFindSystemRoles
- [x] testDeleteRole
- [x] testUpdateRole
- [x] testUniqueConstraintViolation

**RolePermissionDaoTest.java**
- [x] testSavePermission
- [x] testFindByRoleId
- [x] testDeleteByRoleId
- [x] testSaveAllPermissions
- [x] testCascadeDelete

**Coverage Target:** >= 85%

---

## Definición de Hecho (DoD)

- [x] Código implementado y revisado
- [ ] Tests unitarios >= 90% coverage
- [ ] Tests de integración >= 85% coverage
- [ ] Todos los tests pasan
- [x] Código sigue estándares del proyecto
- [x] Documentación actualizada
- [x] Scripts SQL validados en DB
- [ ] Code review aprobado
- [x] No hay warnings en compilación
- [ ] Commits con mensajes descriptivos

---

## Riesgos e Impedimentos

### Riesgo 1: Conflictos con esquema existente
**Mitigación:** Revisar esquema actual antes de empezar, coordinar con DBAs

### Riesgo 2: Performance de índices
**Mitigación:** Probar con dataset realista, ajustar índices según sea necesario

### Riesgo 3: Problemas de migración
**Mitigación:** Tener rollback script listo, probar en staging primero

---

## Reuniones y Ceremonias

### Sprint Planning
- **Fecha:** Día 1
- **Duración:** 2 horas
- **Agenda:** Revisar user stories, estimar puntos, asignar tareas

### Daily Standups
- **Horario:** 9:00 AM diario
- **Duración:** 15 minutos
- **Formato:** ¿Qué hice? ¿Qué haré? ¿Blockers?

### Sprint Review
- **Fecha:** Último día
- **Duración:** 1 hora
- **Demo:** Mostrar tablas creadas, tests pasando

### Sprint Retrospective
- **Fecha:** Último día
- **Duración:** 1 hora
- **Formato:** Start, Stop, Continue

---

## Recursos

### Documentación
- [DATABASE_SCHEMA.md](../DATABASE_SCHEMA.md)
- [TECHNICAL_SPEC.md](../TECHNICAL_SPEC.md)

### Herramientas
- PostgreSQL 12+
- IntelliJ IDEA
- DBeaver para DB management
- TestContainers para integration tests

### Referencias
- Spring Data JPA documentation
- PostgreSQL Index documentation
- ThingsBoard DAO patterns

---

## Entregables

- [x] Scripts SQL en `dao/src/main/resources/sql/`
- [x] Entidades Java en `common/data/`
- [x] DAOs en `dao/src/main/java/`
- [ ] Tests en `dao/src/test/java/`
- [x] Documentación actualizada
- [ ] README con instrucciones de setup

---

**Sprint Goal:** Tener la estructura de datos completa y funcional para soportar roles y permisos.

**Velocity Estimada:** 37 puntos  
**Capacity del Equipo:** 2 desarrolladores × 10 días = 20 días-persona
