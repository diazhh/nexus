# Esquema de Base de Datos - Sistema de Roles y Permisos

## Diagrama de Entidades

```
┌─────────────────┐
│    tb_user      │
├─────────────────┤
│ id (PK)         │───┐
│ tenant_id       │   │
│ customer_id     │   │
│ email           │   │
│ authority       │   │  (legacy)
│ role_id (FK) ───┼───┼──────────┐
│ ...             │   │          │
└─────────────────┘   │          │
                      │          │
                      │          ▼
                      │    ┌─────────────────┐
                      │    │      role       │
                      │    ├─────────────────┤
                      │    │ id (PK)         │◄────┐
                      │    │ tenant_id (FK)  │     │
                      │    │ name            │     │
                      │    │ description     │     │
                      │    │ is_system       │     │
                      │    │ created_time    │     │
                      │    │ version         │     │
                      │    └─────────────────┘     │
                      │                            │
                      │                            │
                      │    ┌─────────────────────┐ │
                      │    │  role_permission    │ │
                      │    ├─────────────────────┤ │
                      │    │ id (PK)             │ │
                      │    │ role_id (FK) ───────┼─┘
                      │    │ resource_type       │
                      │    │ operation           │
                      │    └─────────────────────┘
                      │
                      ▼
              ┌─────────────┐
              │   tenant    │
              ├─────────────┤
              │ id (PK)     │
              │ ...         │
              └─────────────┘
```

## Definición de Tablas

### Tabla: `role`

Almacena los roles personalizados por tenant y roles del sistema.

```sql
CREATE TABLE IF NOT EXISTS role (
    -- Primary Key
    id UUID NOT NULL,
    
    -- Audit fields
    created_time BIGINT NOT NULL,
    
    -- Tenant relationship (NULL para roles del sistema)
    tenant_id UUID,
    
    -- Role information
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1024),
    
    -- System flag (roles no editables)
    is_system BOOLEAN DEFAULT FALSE,
    
    -- Additional metadata
    additional_info VARCHAR,
    
    -- Version for optimistic locking
    version BIGINT DEFAULT 1,
    
    -- Constraints
    CONSTRAINT role_pkey PRIMARY KEY (id),
    CONSTRAINT role_tenant_name_unq UNIQUE (tenant_id, name),
    CONSTRAINT fk_role_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_role_tenant_id ON role(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_role_is_system ON role(is_system) WHERE is_system = TRUE;
CREATE INDEX idx_role_name ON role(name);
```

**Campos:**
- `id`: UUID único del rol
- `created_time`: Timestamp de creación (epoch millis)
- `tenant_id`: FK a tenant (NULL = rol del sistema global)
- `name`: Nombre del rol (único por tenant)
- `description`: Descripción opcional del rol
- `is_system`: Flag para roles del sistema (no editables)
- `additional_info`: JSON para metadata adicional
- `version`: Versión para optimistic locking

**Restricciones:**
- PK: `id`
- UNIQUE: `(tenant_id, name)` - nombre único por tenant
- FK: `tenant_id` → `tenant(id)` ON DELETE CASCADE

**Índices:**
- `idx_role_tenant_id`: Para búsquedas por tenant
- `idx_role_is_system`: Para roles del sistema
- `idx_role_name`: Para búsquedas por nombre

---

### Tabla: `role_permission`

Almacena los permisos asignados a cada rol (matriz Resource × Operation).

```sql
CREATE TABLE IF NOT EXISTS role_permission (
    -- Primary Key
    id UUID NOT NULL,
    
    -- Role relationship
    role_id UUID NOT NULL,
    
    -- Permission definition
    resource_type VARCHAR(64) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    
    -- Constraints
    CONSTRAINT role_permission_pkey PRIMARY KEY (id),
    CONSTRAINT role_permission_role_resource_op_unq 
        UNIQUE (role_id, resource_type, operation),
    CONSTRAINT fk_role_permission_role 
        FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_role_permission_role_id ON role_permission(role_id);
CREATE INDEX idx_role_permission_resource ON role_permission(resource_type);
CREATE INDEX idx_role_permission_composite ON role_permission(role_id, resource_type, operation);
```

**Campos:**
- `id`: UUID único del permiso
- `role_id`: FK al rol que tiene este permiso
- `resource_type`: Tipo de recurso (DEVICE, ASSET, DASHBOARD, etc.)
- `operation`: Operación permitida (READ, WRITE, DELETE, ALL, etc.)

**Restricciones:**
- PK: `id`
- UNIQUE: `(role_id, resource_type, operation)` - un permiso único por combinación
- FK: `role_id` → `role(id)` ON DELETE CASCADE

**Índices:**
- `idx_role_permission_role_id`: Para cargar todos los permisos de un rol
- `idx_role_permission_resource`: Para queries por tipo de recurso
- `idx_role_permission_composite`: Para validación rápida de permisos

---

### Modificación: `tb_user`

Agregar campo `role_id` para vincular usuarios con roles.

```sql
-- Agregar nueva columna role_id
ALTER TABLE tb_user ADD COLUMN role_id UUID;

-- Agregar foreign key constraint
ALTER TABLE tb_user ADD CONSTRAINT fk_user_role 
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE SET NULL;

-- Hacer authority nullable para permitir usuarios basados en roles
ALTER TABLE tb_user ALTER COLUMN authority DROP NOT NULL;

-- Index for performance
CREATE INDEX idx_user_role_id ON tb_user(role_id) WHERE role_id IS NOT NULL;
```

**Cambios:**
- **Nuevo campo:** `role_id UUID` - FK al rol asignado
- **Modificación:** `authority` ahora es nullable
- **FK:** `role_id` → `role(id)` ON DELETE SET NULL
- **Índice:** `idx_user_role_id` para búsquedas por rol

**Comportamiento:**
- Si `role_id` IS NOT NULL → usar permisos del rol
- Si `role_id` IS NULL → usar authority legacy
- ON DELETE SET NULL → si se elimina rol, usuario queda sin rol (debe reasignarse)

---

## Valores de Enums

### Resource Types (resource_type)

```sql
-- Valores permitidos para resource_type
'ADMIN_SETTINGS'
'ALARM'
'DEVICE'
'ASSET'
'CUSTOMER'
'DASHBOARD'
'ENTITY_VIEW'
'TENANT'
'RULE_CHAIN'
'USER'
'WIDGETS_BUNDLE'
'WIDGET_TYPE'
'OAUTH2_CLIENT'
'DOMAIN'
'MOBILE_APP'
'MOBILE_APP_BUNDLE'
'OAUTH2_CONFIGURATION_TEMPLATE'
'TENANT_PROFILE'
'DEVICE_PROFILE'
'ASSET_PROFILE'
'API_USAGE_STATE'
'TB_RESOURCE'
'OTA_PACKAGE'
'EDGE'
'RPC'
'QUEUE'
'VERSION_CONTROL'
'NOTIFICATION'
'MOBILE_APP_SETTINGS'
'JOB'
'AI_MODEL'
'API_KEY'
```

### Operations (operation)

```sql
-- Valores permitidos para operation
'ALL'              -- Todos los permisos sobre el recurso
'CREATE'           -- Crear nueva entidad
'READ'             -- Leer/consultar entidad
'WRITE'            -- Modificar entidad existente
'DELETE'           -- Eliminar entidad
'ASSIGN_TO_CUSTOMER'
'UNASSIGN_FROM_CUSTOMER'
'RPC_CALL'
'READ_CREDENTIALS'
'WRITE_CREDENTIALS'
'READ_ATTRIBUTES'
'WRITE_ATTRIBUTES'
'READ_TELEMETRY'
'WRITE_TELEMETRY'
'CLAIM_DEVICES'
'ASSIGN_TO_TENANT'
'READ_CALCULATED_FIELD'
'WRITE_CALCULATED_FIELD'
```

---

## Datos Iniciales (Seeds)

### Roles del Sistema

```sql
-- Role: System Administrator (Global)
INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
VALUES (
    'e8a8e8e0-1234-4321-8765-000000000001',
    EXTRACT(EPOCH FROM NOW()) * 1000,
    NULL,
    'System Administrator',
    'Full system access across all tenants',
    TRUE,
    1
);

-- Permissions: System Administrator - ALL
INSERT INTO role_permission (id, role_id, resource_type, operation)
VALUES (
    gen_random_uuid(),
    'e8a8e8e0-1234-4321-8765-000000000001',
    'ALL',
    'ALL'
);

-- Function to create default tenant roles
CREATE OR REPLACE FUNCTION create_default_tenant_roles(p_tenant_id UUID)
RETURNS VOID AS $$
BEGIN
    -- Tenant Administrator Role
    INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
    VALUES (
        gen_random_uuid(),
        EXTRACT(EPOCH FROM NOW()) * 1000,
        p_tenant_id,
        'Tenant Administrator',
        'Full access to tenant resources',
        TRUE,
        1
    );
    
    -- Customer User Role
    INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
    VALUES (
        gen_random_uuid(),
        EXTRACT(EPOCH FROM NOW()) * 1000,
        p_tenant_id,
        'Customer User',
        'Limited access for customer users',
        TRUE,
        1
    );
    
    -- Device Manager Role (Custom example)
    INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
    VALUES (
        gen_random_uuid(),
        EXTRACT(EPOCH FROM NOW()) * 1000,
        p_tenant_id,
        'Device Manager',
        'Manage devices and view dashboards',
        FALSE,
        1
    );
END;
$$ LANGUAGE plpgsql;

-- Trigger to create default roles when new tenant is created
CREATE OR REPLACE FUNCTION trigger_create_default_roles()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM create_default_tenant_roles(NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tenant_roles_trigger
    AFTER INSERT ON tenant
    FOR EACH ROW
    EXECUTE FUNCTION trigger_create_default_roles();
```

### Permisos para Tenant Administrator

```sql
-- Script para agregar permisos completos a Tenant Administrator
INSERT INTO role_permission (id, role_id, resource_type, operation)
SELECT 
    gen_random_uuid(),
    r.id,
    res.resource_type,
    'ALL'
FROM role r
CROSS JOIN (
    VALUES 
        ('DEVICE'), ('ASSET'), ('DASHBOARD'), ('USER'), 
        ('CUSTOMER'), ('RULE_CHAIN'), ('ENTITY_VIEW'),
        ('WIDGETS_BUNDLE'), ('WIDGET_TYPE'), ('DEVICE_PROFILE'),
        ('ASSET_PROFILE'), ('TB_RESOURCE'), ('OTA_PACKAGE'),
        ('EDGE'), ('RPC'), ('ALARM'), ('NOTIFICATION'),
        ('OAUTH2_CLIENT'), ('MOBILE_APP'), ('JOB'), ('AI_MODEL')
) AS res(resource_type)
WHERE r.name = 'Tenant Administrator' AND r.is_system = TRUE;
```

### Permisos para Customer User

```sql
-- Script para agregar permisos limitados a Customer User
INSERT INTO role_permission (id, role_id, resource_type, operation)
SELECT 
    gen_random_uuid(),
    r.id,
    perm.resource_type,
    perm.operation
FROM role r
CROSS JOIN (
    VALUES 
        ('DEVICE', 'READ'),
        ('DEVICE', 'READ_CREDENTIALS'),
        ('DEVICE', 'CLAIM_DEVICES'),
        ('ASSET', 'READ'),
        ('DASHBOARD', 'READ'),
        ('ENTITY_VIEW', 'READ'),
        ('ALARM', 'READ'),
        ('ALARM', 'WRITE'),
        ('RPC', 'RPC_CALL')
) AS perm(resource_type, operation)
WHERE r.name = 'Customer User' AND r.is_system = TRUE;
```

---

## Script de Migración

### Migración de Usuarios Existentes

```sql
-- upgrade_3.7.0_to_3.8.0.sql

-- Paso 1: Crear tablas nuevas
\i create_role_tables.sql

-- Paso 2: Crear roles del sistema
INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
VALUES 
    ('e8a8e8e0-1234-4321-8765-000000000001',
     EXTRACT(EPOCH FROM NOW()) * 1000,
     NULL,
     'System Administrator',
     'Full system access',
     TRUE, 1);

-- Paso 3: Crear roles por tenant
INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
SELECT 
    gen_random_uuid(),
    EXTRACT(EPOCH FROM NOW()) * 1000,
    t.id,
    'Tenant Administrator',
    'Full tenant access',
    TRUE,
    1
FROM tenant t
WHERE NOT EXISTS (
    SELECT 1 FROM role r 
    WHERE r.name = 'Tenant Administrator' AND r.tenant_id = t.id
);

INSERT INTO role (id, created_time, tenant_id, name, description, is_system, version)
SELECT 
    gen_random_uuid(),
    EXTRACT(EPOCH FROM NOW()) * 1000,
    t.id,
    'Customer User',
    'Customer access',
    TRUE,
    1
FROM tenant t
WHERE NOT EXISTS (
    SELECT 1 FROM role r 
    WHERE r.name = 'Customer User' AND r.tenant_id = t.id
);

-- Paso 4: Agregar permisos a roles del sistema
\i insert_system_role_permissions.sql

-- Paso 5: Modificar tabla tb_user
ALTER TABLE tb_user ADD COLUMN role_id UUID;
ALTER TABLE tb_user ALTER COLUMN authority DROP NOT NULL;

-- Paso 6: Asignar roles a usuarios existentes basado en authority
UPDATE tb_user u
SET role_id = (
    CASE 
        WHEN u.authority = 'SYS_ADMIN' THEN 
            (SELECT id FROM role 
             WHERE name = 'System Administrator' AND tenant_id IS NULL 
             LIMIT 1)
        WHEN u.authority = 'TENANT_ADMIN' THEN
            (SELECT id FROM role 
             WHERE name = 'Tenant Administrator' AND tenant_id = u.tenant_id 
             LIMIT 1)
        WHEN u.authority = 'CUSTOMER_USER' THEN
            (SELECT id FROM role 
             WHERE name = 'Customer User' AND tenant_id = u.tenant_id 
             LIMIT 1)
    END
)
WHERE u.role_id IS NULL AND u.authority IN ('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER');

-- Paso 7: Agregar constraints
ALTER TABLE tb_user ADD CONSTRAINT fk_user_role 
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE SET NULL;

-- Paso 8: Crear índices
CREATE INDEX idx_user_role_id ON tb_user(role_id) WHERE role_id IS NOT NULL;
CREATE INDEX idx_role_tenant_id ON role(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_role_permission_role_id ON role_permission(role_id);
CREATE INDEX idx_role_permission_composite ON role_permission(role_id, resource_type, operation);

-- Paso 9: Verificación
DO $$
DECLARE
    v_users_without_role INTEGER;
    v_roles_without_permissions INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_users_without_role
    FROM tb_user
    WHERE role_id IS NULL AND authority IS NULL;
    
    IF v_users_without_role > 0 THEN
        RAISE WARNING 'Atención: % usuarios sin rol asignado', v_users_without_role;
    END IF;
    
    SELECT COUNT(*) INTO v_roles_without_permissions
    FROM role r
    WHERE r.is_system = TRUE
    AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id);
    
    IF v_roles_without_permissions > 0 THEN
        RAISE WARNING 'Atención: % roles del sistema sin permisos', v_roles_without_permissions;
    END IF;
    
    RAISE NOTICE 'Migración completada exitosamente';
END $$;
```

### Rollback Script

```sql
-- rollback_3.8.0_to_3.7.0.sql

-- Remover constraints
ALTER TABLE tb_user DROP CONSTRAINT IF EXISTS fk_user_role;

-- Remover columna role_id
ALTER TABLE tb_user DROP COLUMN IF EXISTS role_id;

-- Hacer authority NOT NULL nuevamente
UPDATE tb_user SET authority = 'CUSTOMER_USER' WHERE authority IS NULL;
ALTER TABLE tb_user ALTER COLUMN authority SET NOT NULL;

-- Eliminar tablas
DROP TABLE IF EXISTS role_permission CASCADE;
DROP TABLE IF EXISTS role CASCADE;

-- Eliminar funciones
DROP FUNCTION IF EXISTS create_default_tenant_roles(UUID);
DROP FUNCTION IF EXISTS trigger_create_default_roles();
DROP TRIGGER IF EXISTS tenant_roles_trigger ON tenant;

RAISE NOTICE 'Rollback completado';
```

---

## Consideraciones de Performance

### Índices Recomendados

```sql
-- Índices para búsquedas frecuentes
CREATE INDEX CONCURRENTLY idx_role_tenant_id ON role(tenant_id) 
    WHERE tenant_id IS NOT NULL;

CREATE INDEX CONCURRENTLY idx_role_is_system ON role(is_system) 
    WHERE is_system = TRUE;

CREATE INDEX CONCURRENTLY idx_role_name_lower ON role(LOWER(name));

CREATE INDEX CONCURRENTLY idx_user_role_id ON tb_user(role_id) 
    WHERE role_id IS NOT NULL;

CREATE INDEX CONCURRENTLY idx_role_permission_composite 
    ON role_permission(role_id, resource_type, operation);

-- Índice para búsqueda de usuarios por tenant sin customer
CREATE INDEX CONCURRENTLY idx_user_tenant_no_customer 
    ON tb_user(tenant_id) 
    WHERE customer_id IS NULL;
```

### Particionamiento (Futuro)

Para tenants con muchos roles (>10,000), considerar particionamiento:

```sql
-- Ejemplo de particionamiento por tenant_id (no implementar aún)
CREATE TABLE role_partitioned (
    LIKE role INCLUDING ALL
) PARTITION BY HASH (tenant_id);

CREATE TABLE role_p0 PARTITION OF role_partitioned
    FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE role_p1 PARTITION OF role_partitioned
    FOR VALUES WITH (MODULUS 4, REMAINDER 1);
-- etc.
```

### Estadísticas

```sql
-- Mantener estadísticas actualizadas
ANALYZE role;
ANALYZE role_permission;
ANALYZE tb_user;
```

---

## Restricciones y Validaciones

### Check Constraints

```sql
-- Asegurar que nombres no estén vacíos
ALTER TABLE role ADD CONSTRAINT role_name_not_empty 
    CHECK (LENGTH(TRIM(name)) > 0);

-- Asegurar que resource_type sea válido
ALTER TABLE role_permission ADD CONSTRAINT role_permission_valid_resource
    CHECK (resource_type IN (
        'ADMIN_SETTINGS', 'ALARM', 'DEVICE', 'ASSET', 'CUSTOMER',
        'DASHBOARD', 'ENTITY_VIEW', 'TENANT', 'RULE_CHAIN', 'USER',
        'WIDGETS_BUNDLE', 'WIDGET_TYPE', 'OAUTH2_CLIENT', 'DOMAIN',
        'MOBILE_APP', 'MOBILE_APP_BUNDLE', 'TENANT_PROFILE',
        'DEVICE_PROFILE', 'ASSET_PROFILE', 'TB_RESOURCE',
        'OTA_PACKAGE', 'EDGE', 'RPC', 'QUEUE', 'NOTIFICATION',
        'JOB', 'AI_MODEL', 'API_KEY', 'ALL'
    ));

-- Asegurar que operation sea válida
ALTER TABLE role_permission ADD CONSTRAINT role_permission_valid_operation
    CHECK (operation IN (
        'ALL', 'CREATE', 'READ', 'WRITE', 'DELETE',
        'ASSIGN_TO_CUSTOMER', 'UNASSIGN_FROM_CUSTOMER',
        'RPC_CALL', 'READ_CREDENTIALS', 'WRITE_CREDENTIALS',
        'READ_ATTRIBUTES', 'WRITE_ATTRIBUTES',
        'READ_TELEMETRY', 'WRITE_TELEMETRY',
        'CLAIM_DEVICES', 'ASSIGN_TO_TENANT'
    ));
```

### Triggers de Validación

```sql
-- Trigger para prevenir eliminación de roles con usuarios
CREATE OR REPLACE FUNCTION prevent_role_deletion_with_users()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM tb_user WHERE role_id = OLD.id) THEN
        RAISE EXCEPTION 'Cannot delete role: users are still assigned to this role';
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER role_deletion_check
    BEFORE DELETE ON role
    FOR EACH ROW
    EXECUTE FUNCTION prevent_role_deletion_with_users();

-- Trigger para prevenir modificación de roles del sistema
CREATE OR REPLACE FUNCTION prevent_system_role_modification()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.is_system = TRUE THEN
        RAISE EXCEPTION 'Cannot modify system role';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER system_role_modification_check
    BEFORE UPDATE ON role
    FOR EACH ROW
    WHEN (OLD.is_system = TRUE)
    EXECUTE FUNCTION prevent_system_role_modification();
```

---

## Queries de Ejemplo

### Obtener todos los permisos de un usuario

```sql
SELECT 
    rp.resource_type,
    rp.operation
FROM tb_user u
JOIN role r ON u.role_id = r.id
JOIN role_permission rp ON r.id = rp.role_id
WHERE u.id = :user_id;
```

### Verificar si usuario tiene permiso específico

```sql
SELECT EXISTS (
    SELECT 1
    FROM tb_user u
    JOIN role r ON u.role_id = r.id
    JOIN role_permission rp ON r.id = rp.role_id
    WHERE u.id = :user_id
    AND rp.resource_type IN (:resource_type, 'ALL')
    AND rp.operation IN (:operation, 'ALL')
) AS has_permission;
```

### Listar usuarios por rol

```sql
SELECT 
    u.id,
    u.email,
    u.first_name,
    u.last_name,
    r.name as role_name
FROM tb_user u
JOIN role r ON u.role_id = r.id
WHERE r.id = :role_id
ORDER BY u.email;
```

### Contar usuarios por rol en un tenant

```sql
SELECT 
    r.name,
    COUNT(u.id) as user_count
FROM role r
LEFT JOIN tb_user u ON r.id = u.role_id
WHERE r.tenant_id = :tenant_id
GROUP BY r.id, r.name
ORDER BY user_count DESC;
```

---

**Versión:** 1.0  
**Última Actualización:** 23 Enero 2026
