# Guía de Pruebas con cURL - Sistema de Roles y Permisos

**Fecha:** 24 de enero de 2026  
**ThingsBoard Version:** 4.3.0-RC

---

## 📋 Credenciales de Prueba

### Usuario Administrador del Sistema
```
Usuario: sysadmin@thingsboard.org
Password: sysadmin
```

### Usuario Administrador del Tenant
```
Usuario: tenant@thingsboard.org
Password: tenant
```

### Usuario Customer
```
Usuario: customer@thingsboard.org
Password: customer
```

---

## 🔐 Autenticación

### 1. Obtener Token JWT

```bash
# Autenticar como Tenant Admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tenant@thingsboard.org",
    "password": "tenant"
  }' | jq

# Guardar token en variable
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant@thingsboard.org","password":"tenant"}' 2>/dev/null | jq -r '.token')

echo "Token: $TOKEN"
```

### 2. Verificar Token

```bash
# Obtener información del usuario actual
curl -X GET http://localhost:8080/api/auth/user \
  -H "X-Authorization: Bearer $TOKEN" | jq
```

---

## 👥 Endpoints de Usuarios

### Listar Usuarios del Tenant

```bash
curl -X GET "http://localhost:8080/api/users?pageSize=10&page=0" \
  -H "X-Authorization: Bearer $TOKEN" | jq
```

### Obtener Usuario por ID

```bash
USER_ID="7ff7bf70-f953-11f0-b547-43802839fc7e"
curl -X GET "http://localhost:8080/api/user/$USER_ID" \
  -H "X-Authorization: Bearer $TOKEN" | jq
```

### Crear Usuario

```bash
curl -X POST http://localhost:8080/api/user \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "authority": "TENANT_ADMIN",
    "firstName": "Nuevo",
    "lastName": "Usuario"
  }' | jq
```

---

## 🔒 Endpoints de Roles

### 1. Listar Roles

```bash
curl -X GET "http://localhost:8080/api/role?pageSize=10&page=0" \
  -H "X-Authorization: Bearer $TOKEN" | jq
```

### 2. Crear Rol

```bash
curl -X POST http://localhost:8080/api/role \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Operador",
    "description": "Rol para operadores del sistema",
    "isSystem": false
  }' | jq
```

**Respuesta esperada:**
```json
{
  "id": {
    "entityType": "ROLE",
    "id": "uuid-del-rol"
  },
  "name": "Operador",
  "description": "Rol para operadores del sistema",
  "isSystem": false,
  "version": 1
}
```

### 3. Obtener Rol por ID

```bash
ROLE_ID="7abf5020-f955-11f0-b743-3de84f84abcb"
curl -X GET "http://localhost:8080/api/role/$ROLE_ID" \
  -H "X-Authorization: Bearer $TOKEN" | jq
```

### 4. Actualizar Rol

```bash
ROLE_ID="7abf5020-f955-11f0-b743-3de84f84abcb"
curl -X POST http://localhost:8080/api/role \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": {
      "entityType": "ROLE",
      "id": "'$ROLE_ID'"
    },
    "name": "Operador Actualizado",
    "description": "Descripción actualizada",
    "isSystem": false,
    "version": 1
  }' | jq
```

### 5. Eliminar Rol

```bash
ROLE_ID="7abf5020-f955-11f0-b743-3de84f84abcb"
curl -X DELETE "http://localhost:8080/api/role/$ROLE_ID" \
  -H "X-Authorization: Bearer $TOKEN"
```

---

## 🔑 Endpoints de Permisos

### 1. Obtener Permisos de un Rol

```bash
ROLE_ID="7abf5020-f955-11f0-b743-3de84f84abcb"
curl -X GET "http://localhost:8080/api/role/$ROLE_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN" | jq
```

### 2. Actualizar Permisos de un Rol (Reemplaza todos)

```bash
ROLE_ID="7abf5020-f955-11f0-b743-3de84f84abcb"
curl -X PUT "http://localhost:8080/api/role/$ROLE_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "resource": "DEVICE",
      "operation": "READ"
    },
    {
      "resource": "DEVICE",
      "operation": "WRITE"
    },
    {
      "resource": "DASHBOARD",
      "operation": "READ"
    }
  ]' | jq
```

### 3. Agregar Permisos a un Rol (Sin eliminar existentes)

```bash
ROLE_ID="7abf5020-f955-11f0-b743-3de84f84abcb"
curl -X POST "http://localhost:8080/api/role/$ROLE_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "resource": "ASSET",
      "operation": "READ"
    }
  ]' | jq
```

### 4. Eliminar Permisos de un Rol

```bash
ROLE_ID="7abf5020-f955-11f0-b743-3de84f84abcb"
curl -X DELETE "http://localhost:8080/api/role/$ROLE_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "resource": "DEVICE",
      "operation": "WRITE"
    }
  ]' | jq
```

### 5. Obtener Recursos Disponibles

```bash
curl -X GET "http://localhost:8080/api/role/resources" \
  -H "X-Authorization: Bearer $TOKEN" | jq
```

**Respuesta esperada:**
```json
[
  "ALL", "DEVICE", "ASSET", "DASHBOARD", "USER", "CUSTOMER",
  "ALARM", "RULE_CHAIN", "ENTITY_VIEW", "WIDGET_TYPE",
  "WIDGETS_BUNDLE", "TENANT", "DEVICE_PROFILE", "ASSET_PROFILE",
  "TB_RESOURCE", "OTA_PACKAGE", "EDGE", "RPC", "QUEUE",
  "NOTIFICATION", "OAUTH2_CLIENT", "DOMAIN", "MOBILE_APP",
  "ADMIN_SETTINGS", "AI_MODEL", "API_KEY", "ROLE"
]
```

### 6. Obtener Operaciones Disponibles

```bash
curl -X GET "http://localhost:8080/api/role/operations" \
  -H "X-Authorization: Bearer $TOKEN" | jq
```

**Respuesta esperada:**
```json
[
  "ALL", "CREATE", "READ", "WRITE", "DELETE", "RPC_CALL",
  "READ_CREDENTIALS", "WRITE_CREDENTIALS", "READ_ATTRIBUTES",
  "WRITE_ATTRIBUTES", "READ_TELEMETRY", "CLAIM_DEVICES"
]
```

---

## 👤 Gestión de Usuarios con Roles

### 1. Obtener Usuarios por Rol

```bash
ROLE_ID="7abf5020-f955-11f0-b743-3de84f84abcb"
curl -X GET "http://localhost:8080/api/users/role/$ROLE_ID?pageSize=10&page=0" \
  -H "X-Authorization: Bearer $TOKEN" | jq
```

### 2. Cambiar Rol de un Usuario

```bash
USER_ID="7ff7bf70-f953-11f0-b547-43802839fc7e"
ROLE_ID="7abf5020-f955-11f0-b743-3de84f84abcb"
curl -X PUT "http://localhost:8080/api/user/$USER_ID/role/$ROLE_ID" \
  -H "X-Authorization: Bearer $TOKEN"
```

---

## 🧪 Scripts de Prueba Completos

### Script 1: Crear Rol con Permisos

```bash
#!/bin/bash

# Autenticar
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant@thingsboard.org","password":"tenant"}' | jq -r '.token')

echo "Token obtenido: ${TOKEN:0:50}..."

# Crear rol
echo -e "\n1. Creando rol..."
ROLE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/role \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Operador de Dispositivos",
    "description": "Puede leer y escribir dispositivos",
    "isSystem": false
  }')

echo "$ROLE_RESPONSE" | jq

ROLE_ID=$(echo "$ROLE_RESPONSE" | jq -r '.id.id')
echo "Rol creado con ID: $ROLE_ID"

# Agregar permisos
echo -e "\n2. Agregando permisos..."
curl -s -X PUT "http://localhost:8080/api/role/$ROLE_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"resource": "DEVICE", "operation": "READ"},
    {"resource": "DEVICE", "operation": "WRITE"},
    {"resource": "DASHBOARD", "operation": "READ"}
  ]' | jq

# Verificar permisos
echo -e "\n3. Verificando permisos..."
curl -s -X GET "http://localhost:8080/api/role/$ROLE_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN" | jq

echo -e "\n✅ Rol creado exitosamente con permisos"
```

### Script 2: Listar Todo

```bash
#!/bin/bash

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant@thingsboard.org","password":"tenant"}' | jq -r '.token')

echo "=== ROLES ==="
curl -s -X GET "http://localhost:8080/api/role?pageSize=100&page=0" \
  -H "X-Authorization: Bearer $TOKEN" | jq '.data[] | {id: .id.id, name: .name, description: .description}'

echo -e "\n=== USUARIOS ==="
curl -s -X GET "http://localhost:8080/api/users?pageSize=100&page=0" \
  -H "X-Authorization: Bearer $TOKEN" | jq '.data[] | {id: .id.id, email: .email, authority: .authority, roleId: .roleId}'

echo -e "\n=== RECURSOS DISPONIBLES ==="
curl -s -X GET "http://localhost:8080/api/role/resources" \
  -H "X-Authorization: Bearer $TOKEN" | jq

echo -e "\n=== OPERACIONES DISPONIBLES ==="
curl -s -X GET "http://localhost:8080/api/role/operations" \
  -H "X-Authorization: Bearer $TOKEN" | jq
```

---

## 🐛 Troubleshooting

### Error 403 - Forbidden
```json
{
  "status": 403,
  "message": "You don't have permission to perform this operation!"
}
```
**Solución:** Verifica que estés autenticado como `TENANT_ADMIN`

### Error 400 - Cache not found
```json
{
  "status": 400,
  "message": "Cannot find cache named 'rolePermissions'"
}
```
**Solución:** Reinicia el backend después de agregar la configuración del caché

### Error 401 - Unauthorized
```json
{
  "status": 401,
  "message": "Authentication failed"
}
```
**Solución:** Token expirado, obtén uno nuevo

---

## 📊 Códigos de Respuesta HTTP

| Código | Significado |
|--------|-------------|
| 200 | OK - Operación exitosa |
| 201 | Created - Recurso creado |
| 400 | Bad Request - Datos inválidos |
| 401 | Unauthorized - No autenticado |
| 403 | Forbidden - Sin permisos |
| 404 | Not Found - Recurso no encontrado |
| 500 | Internal Server Error - Error del servidor |

---

## 🔗 URLs Importantes

- **Backend API:** http://localhost:8080/api
- **Frontend:** http://localhost:4200
- **Swagger UI:** http://localhost:8080/swagger-ui.html (si está habilitado)

---

**Nota:** Todos los ejemplos asumen que el backend está corriendo en `localhost:8080` y el frontend en `localhost:4200`.
