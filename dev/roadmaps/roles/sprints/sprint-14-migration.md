# Sprint 14: Migración y Retrocompatibilidad (1 semana)

## Objetivos del Sprint

Crear scripts de migración para usuarios existentes y asegurar retrocompatibilidad con el sistema legacy.

## User Stories

### US-27: Scripts de Migración SQL
**Prioridad:** Crítica | **Puntos:** 13

**Criterios de Aceptación:**
- [ ] Script de migración completo creado
- [ ] Todos los usuarios migrados correctamente
- [ ] Roles del sistema creados por tenant
- [ ] Permisos asignados correctamente
- [ ] Validación post-migración exitosa

**Tareas:**
1. Crear `upgrade_to_roles_system.sql`
2. Implementar creación de roles del sistema
3. Implementar asignación de permisos default
4. Mapear usuarios existentes a roles basado en authority
5. Actualizar `tb_user.role_id`
6. Crear queries de validación
7. Probar en base de datos de staging
8. Crear reporte de migración
9. Documentar proceso de migración

**Estimación:** 3 días

---

### US-28: Rollback Plan
**Prioridad:** Alta | **Puntos:** 5

**Criterios de Aceptación:**
- [ ] Script de rollback creado
- [ ] Rollback probado en staging
- [ ] Datos restaurados correctamente
- [ ] Documentación de rollback completa

**Tareas:**
1. Crear `rollback_roles_system.sql`
2. Remover columna `role_id` de `tb_user`
3. Restaurar `authority` a NOT NULL
4. Eliminar tablas `role` y `role_permission`
5. Probar rollback en staging
6. Documentar pasos de rollback

**Estimación:** 1 día

---

### US-29: Código de Retrocompatibilidad
**Prioridad:** Alta | **Puntos:** 8

**Criterios de Aceptación:**
- [ ] Sistema legacy funciona sin cambios
- [ ] Fallback a authority implementado
- [ ] Tests de retrocompatibilidad pasan
- [ ] Performance no degradada

**Tareas:**
1. Implementar fallback en `RoleBasedPermissionChecker`
2. Mantener validación por authority
3. Asegurar que usuarios sin rol funcionen
4. Crear `LegacyAuthCompatibilityTest.java`
5. Testear flujos legacy completos
6. Documentar comportamiento de fallback

**Estimación:** 2 días

---

## Scripts SQL

### upgrade_to_roles_system.sql
```sql
-- Paso 1: Crear roles del sistema
INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
SELECT 
    gen_random_uuid(),
    EXTRACT(EPOCH FROM NOW()) * 1000,
    t.id,
    'Tenant Administrator',
    'Full access to tenant resources',
    TRUE,
    1
FROM tenant t
WHERE NOT EXISTS (
    SELECT 1 FROM role r 
    WHERE r.name = 'Tenant Administrator' AND r.tenant_id = t.id
);

-- Paso 2: Asignar permisos a Tenant Administrator
INSERT INTO role_permission (id, role_id, resource_type, operation)
SELECT 
    gen_random_uuid(),
    r.id,
    'ALL',
    'ALL'
FROM role r
WHERE r.name = 'Tenant Administrator' AND r.is_system = TRUE;

-- Paso 3: Migrar usuarios
UPDATE tb_user u
SET role_id = (
    CASE 
        WHEN u.authority = 'TENANT_ADMIN' THEN
            (SELECT id FROM role 
             WHERE name = 'Tenant Administrator' 
             AND tenant_id = u.tenant_id 
             LIMIT 1)
        WHEN u.authority = 'CUSTOMER_USER' THEN
            (SELECT id FROM role 
             WHERE name = 'Customer User' 
             AND tenant_id = u.tenant_id 
             LIMIT 1)
    END
)
WHERE u.role_id IS NULL;

-- Paso 4: Validación
DO $$
DECLARE
    v_users_without_role INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_users_without_role
    FROM tb_user
    WHERE role_id IS NULL AND authority NOT IN ('SYS_ADMIN', 'REFRESH_TOKEN');
    
    IF v_users_without_role > 0 THEN
        RAISE WARNING 'Atención: % usuarios sin rol asignado', v_users_without_role;
    ELSE
        RAISE NOTICE 'Migración completada: todos los usuarios tienen rol asignado';
    END IF;
END $$;
```

---

## Tests de Migración

### MigrationTest.java
```java
@SpringBootTest
@Sql(scripts = "/sql/migration-test-data.sql")
public class MigrationTest {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RoleService roleService;
    
    @Test
    void testMigrationScript() throws Exception {
        // Execute migration script
        ScriptUtils.executeSqlScript(
            dataSource.getConnection(),
            new ClassPathResource("sql/upgrade_to_roles_system.sql")
        );
        
        // Verify all users have roles
        List<User> users = userService.findAllUsers();
        for (User user : users) {
            if (user.getAuthority() != Authority.SYS_ADMIN) {
                assertNotNull(user.getRoleId(), 
                    "User " + user.getEmail() + " should have role assigned");
            }
        }
    }
    
    @Test
    void testRollbackScript() throws Exception {
        // Execute migration
        ScriptUtils.executeSqlScript(
            dataSource.getConnection(),
            new ClassPathResource("sql/upgrade_to_roles_system.sql")
        );
        
        // Execute rollback
        ScriptUtils.executeSqlScript(
            dataSource.getConnection(),
            new ClassPathResource("sql/rollback_roles_system.sql")
        );
        
        // Verify system is back to original state
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.getMetaData().getTables(null, null, "role", null);
            assertFalse(rs.next(), "Table 'role' should not exist after rollback");
        }
    }
}
```

---

## Validación Post-Migración

### Checklist de Validación
```bash
#!/bin/bash
# validate_migration.sh

echo "=== Validación de Migración ==="

# 1. Verificar usuarios sin rol
USERS_WITHOUT_ROLE=$(psql $DB_URL -t -c "
    SELECT COUNT(*) FROM tb_user 
    WHERE role_id IS NULL 
    AND authority NOT IN ('SYS_ADMIN', 'REFRESH_TOKEN');
")

echo "Usuarios sin rol: $USERS_WITHOUT_ROLE"
if [ "$USERS_WITHOUT_ROLE" -gt 0 ]; then
    echo "❌ FALLO: Hay usuarios sin rol asignado"
    exit 1
fi

# 2. Verificar roles del sistema
SYSTEM_ROLES=$(psql $DB_URL -t -c "
    SELECT COUNT(*) FROM role WHERE is_system = TRUE;
")

echo "Roles del sistema: $SYSTEM_ROLES"
if [ "$SYSTEM_ROLES" -lt 1 ]; then
    echo "❌ FALLO: No hay roles del sistema"
    exit 1
fi

# 3. Verificar permisos
ROLES_WITHOUT_PERMISSIONS=$(psql $DB_URL -t -c "
    SELECT COUNT(*) FROM role r
    WHERE r.is_system = TRUE
    AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id);
")

echo "Roles sin permisos: $ROLES_WITHOUT_PERMISSIONS"
if [ "$ROLES_WITHOUT_PERMISSIONS" -gt 0 ]; then
    echo "⚠️ ADVERTENCIA: Roles del sistema sin permisos"
fi

# 4. Verificar integridad referencial
ORPHAN_PERMISSIONS=$(psql $DB_URL -t -c "
    SELECT COUNT(*) FROM role_permission rp
    WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.id = rp.role_id);
")

if [ "$ORPHAN_PERMISSIONS" -gt 0 ]; then
    echo "❌ FALLO: Permisos huérfanos encontrados"
    exit 1
fi

echo "✅ Migración validada correctamente"
```

---

## Definición de Hecho

- [ ] Scripts de migración creados y probados
- [ ] Rollback script validado
- [ ] Migración exitosa en staging
- [ ] Validaciones post-migración pasan
- [ ] Código de retrocompatibilidad testeado
- [ ] Documentación completa
- [ ] Plan de rollback documentado
- [ ] Aprobación de DBA

---

**Sprint Goal:** Sistema migrado y con rollback plan validado.

**Velocity Estimada:** 21 puntos
