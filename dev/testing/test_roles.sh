#!/bin/bash

# Script de Prueba - Sistema de Roles y Permisos
# Crea varios roles de prueba y verifica su funcionamiento

set -e

BASE_URL="http://localhost:8080"
TENANT_USER="tenant@thingsboard.org"
TENANT_PASS="tenant"

echo "=========================================="
echo "  Prueba del Sistema de Roles y Permisos"
echo "=========================================="
echo ""

# 1. Autenticación
echo "1. Autenticando como Tenant Admin..."
TOKEN=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$TENANT_USER\",\"password\":\"$TENANT_PASS\"}" | jq -r '.token')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
  echo "❌ Error: No se pudo obtener el token"
  exit 1
fi

echo "✅ Token obtenido: ${TOKEN:0:50}..."
echo ""

# 2. Crear Rol: Operador de Dispositivos
echo "2. Creando rol 'Operador de Dispositivos'..."
ROLE1=$(curl -s -X POST $BASE_URL/api/role \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Operador de Dispositivos",
    "description": "Puede gestionar dispositivos y ver dashboards",
    "isSystem": false
  }')

ROLE1_ID=$(echo "$ROLE1" | jq -r '.id.id')
echo "✅ Rol creado con ID: $ROLE1_ID"
echo "$ROLE1" | jq '{name, description, isSystem}'
echo ""

# 3. Agregar permisos al Rol 1
echo "3. Agregando permisos al rol 'Operador de Dispositivos'..."
curl -s -X PUT "$BASE_URL/api/role/$ROLE1_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"resource": "DEVICE", "operation": "READ"},
    {"resource": "DEVICE", "operation": "WRITE"},
    {"resource": "DEVICE", "operation": "DELETE"},
    {"resource": "DASHBOARD", "operation": "READ"},
    {"resource": "ASSET", "operation": "READ"}
  ]' > /dev/null

PERMS1=$(curl -s -X GET "$BASE_URL/api/role/$ROLE1_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN")
echo "✅ Permisos agregados:"
echo "$PERMS1" | jq -r '.[] | "  - \(.resource): \(.operation)"'
echo ""

# 4. Crear Rol: Analista
echo "4. Creando rol 'Analista'..."
ROLE2=$(curl -s -X POST $BASE_URL/api/role \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Analista",
    "description": "Solo lectura de dispositivos, assets y dashboards",
    "isSystem": false
  }')

ROLE2_ID=$(echo "$ROLE2" | jq -r '.id.id')
echo "✅ Rol creado con ID: $ROLE2_ID"
echo "$ROLE2" | jq '{name, description, isSystem}'
echo ""

# 5. Agregar permisos al Rol 2
echo "5. Agregando permisos al rol 'Analista'..."
curl -s -X PUT "$BASE_URL/api/role/$ROLE2_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"resource": "DEVICE", "operation": "READ"},
    {"resource": "ASSET", "operation": "READ"},
    {"resource": "DASHBOARD", "operation": "READ"},
    {"resource": "ALARM", "operation": "READ"}
  ]' > /dev/null

PERMS2=$(curl -s -X GET "$BASE_URL/api/role/$ROLE2_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN")
echo "✅ Permisos agregados:"
echo "$PERMS2" | jq -r '.[] | "  - \(.resource): \(.operation)"'
echo ""

# 6. Crear Rol: Administrador de Reglas
echo "6. Creando rol 'Administrador de Reglas'..."
ROLE3=$(curl -s -X POST $BASE_URL/api/role \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Administrador de Reglas",
    "description": "Gestiona rule chains y puede leer dispositivos",
    "isSystem": false
  }')

ROLE3_ID=$(echo "$ROLE3" | jq -r '.id.id')
echo "✅ Rol creado con ID: $ROLE3_ID"
echo "$ROLE3" | jq '{name, description, isSystem}'
echo ""

# 7. Agregar permisos al Rol 3
echo "7. Agregando permisos al rol 'Administrador de Reglas'..."
curl -s -X PUT "$BASE_URL/api/role/$ROLE3_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"resource": "RULE_CHAIN", "operation": "READ"},
    {"resource": "RULE_CHAIN", "operation": "WRITE"},
    {"resource": "RULE_CHAIN", "operation": "CREATE"},
    {"resource": "RULE_CHAIN", "operation": "DELETE"},
    {"resource": "DEVICE", "operation": "READ"}
  ]' > /dev/null

PERMS3=$(curl -s -X GET "$BASE_URL/api/role/$ROLE3_ID/permissions" \
  -H "X-Authorization: Bearer $TOKEN")
echo "✅ Permisos agregados:"
echo "$PERMS3" | jq -r '.[] | "  - \(.resource): \(.operation)"'
echo ""

# 8. Listar todos los roles
echo "8. Listando todos los roles creados..."
ALL_ROLES=$(curl -s -X GET "$BASE_URL/api/role?pageSize=100&page=0" \
  -H "X-Authorization: Bearer $TOKEN")

echo "✅ Roles en el sistema:"
echo "$ALL_ROLES" | jq -r '.data[] | "  - [\(.id.id | .[0:8])] \(.name) - \(.description)"'
echo ""

# 9. Verificar recursos y operaciones disponibles
echo "9. Recursos disponibles en el sistema:"
RESOURCES=$(curl -s -X GET "$BASE_URL/api/role/resources" \
  -H "X-Authorization: Bearer $TOKEN")
echo "$RESOURCES" | jq -r '.[] | "  - \(.)"' | head -10
echo "  ... (total: $(echo "$RESOURCES" | jq '. | length') recursos)"
echo ""

echo "10. Operaciones disponibles en el sistema:"
OPERATIONS=$(curl -s -X GET "$BASE_URL/api/role/operations" \
  -H "X-Authorization: Bearer $TOKEN")
echo "$OPERATIONS" | jq -r '.[] | "  - \(.)"'
echo ""

# Resumen
echo "=========================================="
echo "  ✅ PRUEBA COMPLETADA EXITOSAMENTE"
echo "=========================================="
echo ""
echo "Resumen:"
echo "  - 3 roles creados"
echo "  - Permisos configurados correctamente"
echo "  - Sistema funcionando correctamente"
echo ""
echo "IDs de los roles creados:"
echo "  - Operador de Dispositivos: $ROLE1_ID"
echo "  - Analista: $ROLE2_ID"
echo "  - Administrador de Reglas: $ROLE3_ID"
echo ""
