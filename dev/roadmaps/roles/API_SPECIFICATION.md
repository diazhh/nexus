# API REST - Sistema de Roles y Permisos

## Especificación de APIs

### Base URL
```
http://localhost:8080/api
```

### Autenticación
Todas las APIs requieren autenticación JWT mediante header:
```
Authorization: Bearer <JWT_TOKEN>
```

---

## Endpoints de Roles

### 1. Listar Roles

**GET** `/api/role`

Obtiene lista paginada de roles del tenant actual.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Query Parameters:**
- `pageSize` (int, opcional): Tamaño de página (default: 10, max: 100)
- `page` (int, opcional): Número de página (default: 0)
- `textSearch` (string, opcional): Búsqueda por nombre o descripción
- `sortProperty` (string, opcional): Campo para ordenar (default: name)
- `sortOrder` (string, opcional): ASC o DESC (default: ASC)

**Response 200 OK:**
```json
{
  "data": [
    {
      "id": {
        "id": "784f394c-42b6-435a-983c-b7beff2784f9"
      },
      "createdTime": 1705968234000,
      "tenantId": {
        "id": "13814000-1dd2-11b2-8080-808080808080"
      },
      "name": "Device Manager",
      "description": "Manages devices and views dashboards",
      "isSystem": false,
      "additionalInfo": null,
      "version": 1
    }
  ],
  "totalPages": 5,
  "totalElements": 42,
  "hasNext": true
}
```

**Ejemplo cURL:**
```bash
curl -X GET "http://localhost:8080/api/role?pageSize=10&page=0" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

### 2. Obtener Role por ID

**GET** `/api/role/{roleId}`

Obtiene detalles de un rol específico.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Path Parameters:**
- `roleId` (UUID): ID del rol

**Response 200 OK:**
```json
{
  "id": {
    "id": "784f394c-42b6-435a-983c-b7beff2784f9"
  },
  "createdTime": 1705968234000,
  "tenantId": {
    "id": "13814000-1dd2-11b2-8080-808080808080"
  },
  "name": "Device Manager",
  "description": "Manages devices and views dashboards",
  "isSystem": false,
  "version": 1
}
```

**Response 404 Not Found:**
```json
{
  "status": 404,
  "message": "Role not found",
  "errorCode": 31,
  "timestamp": 1705968234000
}
```

**Ejemplo cURL:**
```bash
curl -X GET "http://localhost:8080/api/role/784f394c-42b6-435a-983c-b7beff2784f9" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

### 3. Crear o Actualizar Role

**POST** `/api/role`

Crea un nuevo rol o actualiza uno existente.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Request Body:**
```json
{
  "id": {
    "id": "784f394c-42b6-435a-983c-b7beff2784f9"
  },
  "name": "Device Manager",
  "description": "Manages devices and views dashboards"
}
```

**Validaciones:**
- `name`: requerido, longitud 1-255, único por tenant
- `description`: opcional, longitud max 1024
- No se puede modificar `isSystem=true`

**Response 200 OK:**
```json
{
  "id": {
    "id": "784f394c-42b6-435a-983c-b7beff2784f9"
  },
  "createdTime": 1705968234000,
  "tenantId": {
    "id": "13814000-1dd2-11b2-8080-808080808080"
  },
  "name": "Device Manager",
  "description": "Manages devices and views dashboards",
  "isSystem": false,
  "version": 1
}
```

**Response 400 Bad Request:**
```json
{
  "status": 400,
  "message": "Role name is required",
  "errorCode": 2,
  "timestamp": 1705968234000
}
```

**Response 409 Conflict:**
```json
{
  "status": 409,
  "message": "Role with name 'Device Manager' already exists",
  "errorCode": 4,
  "timestamp": 1705968234000
}
```

**Ejemplo cURL (Crear):**
```bash
curl -X POST "http://localhost:8080/api/role" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Device Manager",
    "description": "Manages devices and views dashboards"
  }'
```

**Ejemplo cURL (Actualizar):**
```bash
curl -X POST "http://localhost:8080/api/role" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": {"id": "784f394c-42b6-435a-983c-b7beff2784f9"},
    "name": "Device Manager",
    "description": "Updated description",
    "version": 1
  }'
```

---

### 4. Eliminar Role

**DELETE** `/api/role/{roleId}`

Elimina un rol del sistema.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Path Parameters:**
- `roleId` (UUID): ID del rol a eliminar

**Validaciones:**
- No se pueden eliminar roles con `isSystem=true`
- No se pueden eliminar roles con usuarios asignados

**Response 200 OK:**
```json
{
  "message": "Role deleted successfully"
}
```

**Response 400 Bad Request:**
```json
{
  "status": 400,
  "message": "Cannot delete system role",
  "errorCode": 2,
  "timestamp": 1705968234000
}
```

**Response 409 Conflict:**
```json
{
  "status": 409,
  "message": "Cannot delete role: 5 users are still assigned to this role",
  "errorCode": 4,
  "timestamp": 1705968234000
}
```

**Ejemplo cURL:**
```bash
curl -X DELETE "http://localhost:8080/api/role/784f394c-42b6-435a-983c-b7beff2784f9" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

### 5. Obtener Permisos de Role

**GET** `/api/role/{roleId}/permissions`

Obtiene todos los permisos asignados a un rol.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Path Parameters:**
- `roleId` (UUID): ID del rol

**Response 200 OK:**
```json
[
  {
    "id": {
      "id": "550e8400-e29b-41d4-a716-446655440001"
    },
    "roleId": {
      "id": "784f394c-42b6-435a-983c-b7beff2784f9"
    },
    "resource": "DEVICE",
    "operation": "READ"
  },
  {
    "id": {
      "id": "550e8400-e29b-41d4-a716-446655440002"
    },
    "roleId": {
      "id": "784f394c-42b6-435a-983c-b7beff2784f9"
    },
    "resource": "DEVICE",
    "operation": "WRITE"
  },
  {
    "id": {
      "id": "550e8400-e29b-41d4-a716-446655440003"
    },
    "roleId": {
      "id": "784f394c-42b6-435a-983c-b7beff2784f9"
    },
    "resource": "DASHBOARD",
    "operation": "READ"
  }
]
```

**Ejemplo cURL:**
```bash
curl -X GET "http://localhost:8080/api/role/784f394c-42b6-435a-983c-b7beff2784f9/permissions" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

### 6. Actualizar Permisos de Role

**POST** `/api/role/{roleId}/permissions`

Reemplaza todos los permisos de un rol con los especificados.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Path Parameters:**
- `roleId` (UUID): ID del rol

**Request Body:**
```json
[
  {
    "resource": "DEVICE",
    "operation": "ALL"
  },
  {
    "resource": "ASSET",
    "operation": "READ"
  },
  {
    "resource": "DASHBOARD",
    "operation": "READ"
  },
  {
    "resource": "ALARM",
    "operation": "READ"
  },
  {
    "resource": "ALARM",
    "operation": "WRITE"
  }
]
```

**Validaciones:**
- `resource`: debe ser un valor válido del enum Resource
- `operation`: debe ser un valor válido del enum Operation
- No duplicados (resource + operation)

**Response 200 OK:**
```json
{
  "message": "Permissions updated successfully",
  "count": 5
}
```

**Response 400 Bad Request:**
```json
{
  "status": 400,
  "message": "Invalid resource type: INVALID_RESOURCE",
  "errorCode": 2,
  "timestamp": 1705968234000
}
```

**Ejemplo cURL:**
```bash
curl -X POST "http://localhost:8080/api/role/784f394c-42b6-435a-983c-b7beff2784f9/permissions" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"resource": "DEVICE", "operation": "ALL"},
    {"resource": "ASSET", "operation": "READ"},
    {"resource": "DASHBOARD", "operation": "READ"}
  ]'
```

---

### 7. Obtener Recursos Disponibles

**GET** `/api/role/resources`

Obtiene lista de todos los tipos de recursos disponibles.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Response 200 OK:**
```json
[
  "ADMIN_SETTINGS",
  "ALARM",
  "DEVICE",
  "ASSET",
  "CUSTOMER",
  "DASHBOARD",
  "ENTITY_VIEW",
  "TENANT",
  "RULE_CHAIN",
  "USER",
  "WIDGETS_BUNDLE",
  "WIDGET_TYPE",
  "DEVICE_PROFILE",
  "ASSET_PROFILE",
  "TB_RESOURCE",
  "OTA_PACKAGE",
  "EDGE",
  "RPC",
  "NOTIFICATION",
  "JOB",
  "AI_MODEL",
  "API_KEY"
]
```

**Ejemplo cURL:**
```bash
curl -X GET "http://localhost:8080/api/role/resources" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

### 8. Obtener Operaciones Disponibles

**GET** `/api/role/operations`

Obtiene lista de todas las operaciones disponibles.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Response 200 OK:**
```json
[
  "ALL",
  "CREATE",
  "READ",
  "WRITE",
  "DELETE",
  "ASSIGN_TO_CUSTOMER",
  "UNASSIGN_FROM_CUSTOMER",
  "RPC_CALL",
  "READ_CREDENTIALS",
  "WRITE_CREDENTIALS",
  "READ_ATTRIBUTES",
  "WRITE_ATTRIBUTES",
  "READ_TELEMETRY",
  "WRITE_TELEMETRY",
  "CLAIM_DEVICES"
]
```

**Ejemplo cURL:**
```bash
curl -X GET "http://localhost:8080/api/role/operations" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

## Endpoints de Usuarios (Modificados)

### 9. Crear Usuario de Tenant con Role

**POST** `/api/user/tenant`

Crea un usuario directamente en el tenant (sin customer requerido) con rol asignado.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Query Parameters:**
- `roleId` (UUID, requerido): ID del rol a asignar
- `sendActivationMail` (boolean, opcional): Enviar email de activación (default: true)

**Request Body:**
```json
{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "additionalInfo": {
    "description": "Device manager for IoT devices",
    "lang": "es_ES"
  }
}
```

**Response 200 OK:**
```json
{
  "id": {
    "id": "1e0d9c60-2b6a-11ec-8d3d-0242ac130003"
  },
  "createdTime": 1705968234000,
  "tenantId": {
    "id": "13814000-1dd2-11b2-8080-808080808080"
  },
  "customerId": null,
  "email": "user@example.com",
  "authority": "TENANT_ADMIN",
  "roleId": {
    "id": "784f394c-42b6-435a-983c-b7beff2784f9"
  },
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "additionalInfo": {
    "description": "Device manager for IoT devices",
    "lang": "es_ES",
    "userCredentialsEnabled": true,
    "userActivated": false
  }
}
```

**Response 409 Conflict:**
```json
{
  "status": 409,
  "message": "User with email 'user@example.com' already exists",
  "errorCode": 4,
  "timestamp": 1705968234000
}
```

**Ejemplo cURL:**
```bash
curl -X POST "http://localhost:8080/api/user/tenant?roleId=784f394c-42b6-435a-983c-b7beff2784f9&sendActivationMail=true" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

---

### 10. Listar Usuarios del Tenant

**GET** `/api/user/tenant/users`

Obtiene lista paginada de todos los usuarios del tenant (incluye usuarios sin customer).

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Query Parameters:**
- `pageSize` (int, opcional): Tamaño de página (default: 10)
- `page` (int, opcional): Número de página (default: 0)
- `textSearch` (string, opcional): Búsqueda por email o nombre
- `roleId` (UUID, opcional): Filtrar por rol específico

**Response 200 OK:**
```json
{
  "data": [
    {
      "id": {
        "id": "1e0d9c60-2b6a-11ec-8d3d-0242ac130003"
      },
      "email": "user@example.com",
      "authority": "TENANT_ADMIN",
      "roleId": {
        "id": "784f394c-42b6-435a-983c-b7beff2784f9"
      },
      "firstName": "John",
      "lastName": "Doe",
      "customerId": null
    }
  ],
  "totalPages": 1,
  "totalElements": 15,
  "hasNext": false
}
```

**Ejemplo cURL:**
```bash
curl -X GET "http://localhost:8080/api/user/tenant/users?pageSize=20&page=0" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

### 11. Cambiar Role de Usuario

**PUT** `/api/user/{userId}/role/{roleId}`

Actualiza el rol asignado a un usuario.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Path Parameters:**
- `userId` (UUID): ID del usuario
- `roleId` (UUID): ID del nuevo rol

**Response 200 OK:**
```json
{
  "id": {
    "id": "1e0d9c60-2b6a-11ec-8d3d-0242ac130003"
  },
  "email": "user@example.com",
  "roleId": {
    "id": "784f394c-42b6-435a-983c-b7beff2784f9"
  },
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response 400 Bad Request:**
```json
{
  "status": 400,
  "message": "Cannot assign role from different tenant",
  "errorCode": 2,
  "timestamp": 1705968234000
}
```

**Ejemplo cURL:**
```bash
curl -X PUT "http://localhost:8080/api/user/1e0d9c60-2b6a-11ec-8d3d-0242ac130003/role/784f394c-42b6-435a-983c-b7beff2784f9" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

### 12. Obtener Usuarios por Role

**GET** `/api/role/{roleId}/users`

Obtiene lista de usuarios asignados a un rol específico.

**Autorización:** `TENANT_ADMIN`, `SYS_ADMIN`

**Path Parameters:**
- `roleId` (UUID): ID del rol

**Query Parameters:**
- `pageSize` (int, opcional): Tamaño de página
- `page` (int, opcional): Número de página

**Response 200 OK:**
```json
{
  "data": [
    {
      "id": {
        "id": "1e0d9c60-2b6a-11ec-8d3d-0242ac130003"
      },
      "email": "user1@example.com",
      "firstName": "John",
      "lastName": "Doe"
    },
    {
      "id": {
        "id": "2e0d9c60-2b6a-11ec-8d3d-0242ac130004"
      },
      "email": "user2@example.com",
      "firstName": "Jane",
      "lastName": "Smith"
    }
  ],
  "totalPages": 1,
  "totalElements": 2,
  "hasNext": false
}
```

**Ejemplo cURL:**
```bash
curl -X GET "http://localhost:8080/api/role/784f394c-42b6-435a-983c-b7beff2784f9/users" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

## Códigos de Error

| Código | Descripción |
|--------|-------------|
| 2 | BAD_REQUEST_PARAMS - Parámetros inválidos |
| 3 | AUTHENTICATION - Error de autenticación |
| 4 | ITEM_NOT_FOUND - Recurso no encontrado |
| 5 | PERMISSION_DENIED - Sin permisos |
| 31 | GENERAL - Error general |

---

## Ejemplos de Uso Completos

### Crear Role con Permisos

```bash
# 1. Crear rol
ROLE_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/role" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dashboard Viewer",
    "description": "Read-only access to dashboards"
  }')

ROLE_ID=$(echo $ROLE_RESPONSE | jq -r '.id.id')

# 2. Asignar permisos
curl -X POST "http://localhost:8080/api/role/$ROLE_ID/permissions" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"resource": "DASHBOARD", "operation": "READ"},
    {"resource": "DEVICE", "operation": "READ"},
    {"resource": "ASSET", "operation": "READ"}
  ]'

# 3. Crear usuario con este rol
curl -X POST "http://localhost:8080/api/user/tenant?roleId=$ROLE_ID" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "viewer@example.com",
    "firstName": "Dashboard",
    "lastName": "Viewer"
  }'
```

### Clonar Permisos de un Role a Otro

```bash
# 1. Obtener permisos del rol origen
SOURCE_PERMS=$(curl -s -X GET "http://localhost:8080/api/role/$SOURCE_ROLE_ID/permissions" \
  -H "Authorization: Bearer $JWT_TOKEN")

# 2. Aplicar al rol destino
curl -X POST "http://localhost:8080/api/role/$TARGET_ROLE_ID/permissions" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$SOURCE_PERMS"
```

---

**Versión:** 1.0  
**Última Actualización:** 23 Enero 2026
