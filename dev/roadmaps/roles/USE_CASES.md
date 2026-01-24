# Casos de Uso - Sistema de Roles y Permisos

## Casos de Uso Principales

### UC-01: Administrador Crea Rol Personalizado

**Actor:** Tenant Administrator

**Precondiciones:**
- Usuario autenticado como Tenant Admin
- Acceso al módulo de Roles

**Flujo Principal:**
1. Admin navega a sección "Roles" en menú de administración
2. Hace clic en botón "Agregar Rol"
3. Completa formulario:
   - Nombre: "Device Manager"
   - Descripción: "Gestiona dispositivos IoT y ve dashboards"
4. Hace clic en "Guardar"
5. Sistema crea el rol y muestra mensaje de confirmación
6. Admin es redirigido a la lista de roles

**Postcondiciones:**
- Nuevo rol creado en base de datos
- Rol visible en lista de roles del tenant
- Rol listo para asignar permisos

**Flujos Alternativos:**
- 3a. Nombre ya existe: Sistema muestra error "Rol con ese nombre ya existe"
- 3b. Nombre vacío: Sistema muestra error "Nombre es requerido"

---

### UC-02: Configurar Permisos de Rol

**Actor:** Tenant Administrator

**Precondiciones:**
- Rol "Device Manager" creado
- Usuario autenticado como Tenant Admin

**Flujo Principal:**
1. Admin selecciona rol "Device Manager" de la lista
2. Hace clic en "Gestionar Permisos"
3. Sistema muestra matriz de permisos (Recursos × Operaciones)
4. Admin marca permisos deseados:
   - DEVICE: READ, WRITE, DELETE
   - DASHBOARD: READ
   - ALARM: READ, WRITE
   - ASSET: READ
5. Hace clic en "Guardar Permisos"
6. Sistema actualiza permisos y muestra confirmación

**Postcondiciones:**
- Permisos guardados en tabla `role_permission`
- Cache de permisos invalidado
- Usuarios con este rol obtienen nuevos permisos inmediatamente

**Ejemplo Visual:**

```
Matriz de Permisos:
┌─────────────┬────────┬─────────┬─────────┬─────────┐
│ Recurso     │ READ   │ WRITE   │ DELETE  │ ALL     │
├─────────────┼────────┼─────────┼─────────┼─────────┤
│ DEVICE      │   ☑    │    ☑    │    ☑    │    ☐    │
│ ASSET       │   ☑    │    ☐    │    ☐    │    ☐    │
│ DASHBOARD   │   ☑    │    ☐    │    ☐    │    ☐    │
│ ALARM       │   ☑    │    ☑    │    ☐    │    ☐    │
│ USER        │   ☐    │    ☐    │    ☐    │    ☐    │
└─────────────┴────────┴─────────┴─────────┴─────────┘
```

---

### UC-03: Crear Usuario con Rol

**Actor:** Tenant Administrator

**Precondiciones:**
- Al menos un rol custom disponible
- Usuario autenticado como Tenant Admin

**Flujo Principal:**
1. Admin navega a "Usuarios"
2. Hace clic en "Agregar Usuario"
3. Completa formulario:
   - Email: "john.doe@company.com"
   - Nombre: "John"
   - Apellido: "Doe"
   - Selecciona rol: "Device Manager"
   - Marca checkbox: "Crear como usuario de tenant" (sin customer)
4. Selecciona método de activación: "Enviar email de activación"
5. Hace clic en "Guardar"
6. Sistema crea usuario y envía email
7. Admin ve mensaje de confirmación

**Postcondiciones:**
- Usuario creado en base de datos con `role_id` asignado
- `customer_id` = NULL (usuario de tenant)
- Email de activación enviado
- Usuario aparece en lista de usuarios del tenant

**Flujos Alternativos:**
- 3a. Email duplicado: Sistema muestra error
- 4a. Método "Mostrar link de activación": Sistema muestra dialog con link
- 5a. Sin rol seleccionado: Sistema muestra error "Rol es requerido"

---

### UC-04: Usuario Accede al Sistema con Permisos

**Actor:** Device Manager User

**Precondiciones:**
- Usuario creado con rol "Device Manager"
- Usuario activado y con credenciales

**Flujo Principal:**
1. Usuario inicia sesión con email y password
2. Sistema valida credenciales
3. Sistema carga usuario y su rol
4. Sistema carga permisos del rol desde cache
5. Usuario ve dashboard principal
6. Usuario navega a "Devices"
7. Sistema valida permiso DEVICE:READ
8. Usuario puede ver lista de dispositivos
9. Usuario hace clic en "Agregar Device"
10. Sistema valida permiso DEVICE:CREATE
11. Usuario puede crear nuevo dispositivo

**Postcondiciones:**
- Usuario autenticado
- Permisos cargados en sesión
- Accesos permitidos funcionan correctamente

**Flujos Alternativos:**
- 10a. Usuario sin permiso CREATE: Sistema muestra error 403 Forbidden
- Usuario intenta acceder a "Users": Sistema oculta opción en menú

---

### UC-05: Validación de Permisos en Backend

**Actor:** Sistema (automático)

**Precondiciones:**
- Usuario autenticado con rol asignado
- Request entrante a API protegida

**Flujo Principal:**
1. Request llega a `GET /api/device/{deviceId}`
2. Controlador tiene anotación `@RequirePermission(resource=DEVICE, operation=READ)`
3. Aspect intercepta la llamada
4. Aspect obtiene SecurityUser del contexto
5. Aspect consulta RoleBasedPermissionChecker
6. Checker verifica en cache si rol tiene DEVICE:READ
7. Cache hit: permiso encontrado
8. Aspect permite ejecución del método
9. Controlador procesa request y retorna device

**Postcondiciones:**
- Request procesado exitosamente
- Auditoría registrada (opcional)

**Flujos Alternativos:**
- 6a. Cache miss: Checker consulta base de datos, actualiza cache
- 7a. Permiso no encontrado: Aspect lanza ThingsboardException(403)
- 9a. Usuario intenta `DELETE /api/device/{deviceId}` sin permiso: Error 403

**Diagrama de Secuencia:**
```
Usuario -> API -> Aspect -> PermissionChecker -> Cache -> DB
   |         |       |            |                |      |
   |-------->|       |            |                |      |
   |         |------>|            |                |      |
   |         |       |----------->|                |      |
   |         |       |            |--------------->|      |
   |         |       |            |<---------------|      |
   |         |       |<-----------|                |      |
   |         |<------|            |                |      |
   |<--------|       |            |                |      |
```

---

### UC-06: Cambiar Rol de Usuario Existente

**Actor:** Tenant Administrator

**Precondiciones:**
- Usuario "john.doe@company.com" existe con rol "Device Manager"
- Rol "Dashboard Viewer" disponible

**Flujo Principal:**
1. Admin navega a lista de usuarios
2. Busca usuario "john.doe@company.com"
3. Hace clic en editar usuario
4. Cambia rol de "Device Manager" a "Dashboard Viewer"
5. Hace clic en "Guardar"
6. Sistema actualiza `tb_user.role_id`
7. Sistema invalida sesiones activas del usuario
8. Sistema muestra confirmación

**Postcondiciones:**
- Usuario tiene nuevo rol asignado
- Sesiones activas invalidadas (usuario debe re-login)
- Próximo login tendrá nuevos permisos

**Flujos Alternativos:**
- 4a. Rol pertenece a otro tenant: Sistema muestra error
- 7a. Usuario tiene sesión activa: Sistema fuerza logout

---

### UC-07: Eliminar Rol con Validación

**Actor:** Tenant Administrator

**Precondiciones:**
- Rol "Device Manager" existe
- 3 usuarios asignados al rol

**Flujo Principal:**
1. Admin selecciona rol "Device Manager"
2. Hace clic en "Eliminar"
3. Sistema valida si hay usuarios con este rol
4. Sistema muestra diálogo:
   "No se puede eliminar: 3 usuarios tienen este rol asignado.
    Por favor reasigne estos usuarios a otro rol primero."
5. Admin cancela operación

**Postcondiciones:**
- Rol no eliminado
- Usuarios mantienen asignación

**Flujos Alternativos:**
- 3a. Sin usuarios asignados: Sistema muestra confirmación
- 3b. Admin confirma: Sistema elimina rol y permisos asociados (CASCADE)
- 3c. Rol es del sistema (isSystem=true): Sistema muestra error "No se pueden eliminar roles del sistema"

---

### UC-08: Migración de Usuarios Legacy

**Actor:** Sistema (automático durante upgrade)

**Precondiciones:**
- Sistema en versión 3.7.0
- Usuarios existentes con campo `authority`

**Flujo Principal:**
1. DBA ejecuta script de migración `upgrade_to_roles.sql`
2. Script crea tablas `role` y `role_permission`
3. Script crea roles del sistema por tenant
4. Script asigna permisos a roles del sistema
5. Script mapea usuarios a roles basado en authority:
   - SYS_ADMIN → "System Administrator"
   - TENANT_ADMIN → "Tenant Administrator"  
   - CUSTOMER_USER → "Customer User"
6. Script actualiza `tb_user.role_id`
7. Script valida que todos los usuarios tengan rol
8. Script muestra reporte de migración

**Postcondiciones:**
- Todos los usuarios tienen `role_id` asignado
- Campo `authority` mantenido para retrocompatibilidad
- Sistema funcional con nuevo modelo

**Validación:**
```sql
SELECT 
    COUNT(*) as total_users,
    COUNT(role_id) as users_with_role,
    COUNT(*) - COUNT(role_id) as users_without_role
FROM tb_user;
```

---

### UC-09: Clonar Rol Existente

**Actor:** Tenant Administrator

**Precondiciones:**
- Rol "Device Manager" existe con permisos configurados

**Flujo Principal:**
1. Admin selecciona rol "Device Manager"
2. Hace clic en "Clonar Rol"
3. Sistema abre diálogo con datos pre-llenados:
   - Nombre: "Device Manager (Copy)"
   - Descripción: copiada del original
   - Permisos: copiados del original
4. Admin modifica nombre a "Senior Device Manager"
5. Admin agrega permisos adicionales:
   - USER: READ
   - CUSTOMER: READ
6. Hace clic en "Guardar"
7. Sistema crea nuevo rol con permisos

**Postcondiciones:**
- Nuevo rol creado independiente del original
- Permisos copiados y extendidos
- Disponible para asignar a usuarios

---

### UC-10: Auditar Cambios en Roles y Permisos

**Actor:** Auditor / Administrator

**Precondiciones:**
- Sistema con auditoría habilitada
- Cambios realizados en roles

**Flujo Principal:**
1. Admin navega a "Audit Logs"
2. Filtra por entidad: "ROLE"
3. Sistema muestra eventos:
   - "2026-01-23 10:30 - Admin created role 'Device Manager'"
   - "2026-01-23 10:35 - Admin updated permissions for 'Device Manager'"
   - "2026-01-23 11:00 - Admin assigned role to user john@example.com"
4. Admin selecciona evento
5. Sistema muestra detalles JSON:
   ```json
   {
     "actionType": "UPDATED",
     "entityType": "ROLE_PERMISSION",
     "entityId": "784f394c-42b6-435a-983c-b7beff2784f9",
     "actionData": {
       "added": [
         {"resource": "USER", "operation": "READ"}
       ],
       "removed": [
         {"resource": "ASSET", "operation": "WRITE"}
       ]
     }
   }
   ```

**Postcondiciones:**
- Historial completo de cambios visible
- Trazabilidad de modificaciones
- Cumplimiento de auditoría

---

## Casos de Uso por Rol

### Tenant Administrator

1. ✅ Crear, editar, eliminar roles custom
2. ✅ Configurar permisos granulares
3. ✅ Crear usuarios con roles
4. ✅ Cambiar roles de usuarios
5. ✅ Ver audit logs de cambios
6. ✅ Clonar roles existentes
7. ✅ Exportar/importar configuración de roles (futuro)

### Customer User

1. ❌ No puede crear roles
2. ❌ No puede modificar permisos
3. ✅ Puede ver su propio perfil
4. ✅ Accede según permisos de su rol

### System Administrator

1. ✅ Todo lo de Tenant Admin
2. ✅ Crear roles globales del sistema
3. ✅ Ver roles de todos los tenants
4. ✅ Gestionar tenants y sus roles

---

## Escenarios de Integración

### Escenario 1: Onboarding de Nuevo Cliente

**Contexto:** Nueva empresa se registra como tenant

**Flujo:**
1. Sistema crea tenant
2. Trigger automático crea roles por default:
   - "Tenant Administrator"
   - "Customer User"
   - "Device Manager" (custom example)
3. Se asignan permisos a cada rol
4. Admin crea primer usuario con rol "Tenant Administrator"
5. Tenant Admin crea más usuarios según necesidad

**Resultado:** Tenant operativo con estructura de roles lista

---

### Escenario 2: Migración de Cliente Legacy

**Contexto:** Cliente existente migra a nuevo sistema de roles

**Flujo:**
1. DBA ejecuta script de migración en ambiente de staging
2. Validación: todos los usuarios tienen rol asignado
3. Testing: usuarios pueden acceder según permisos esperados
4. Rollout a producción en ventana de mantenimiento
5. Monitoring post-despliegue
6. Tenant Admin puede empezar a crear roles custom

**Resultado:** Migración sin downtime, usuarios mantienen accesos

---

### Escenario 3: Reorganización de Permisos

**Contexto:** Empresa reorganiza estructura de accesos

**Flujo:**
1. Admin crea nuevos roles:
   - "IoT Engineer" (devices y assets full)
   - "Business Analyst" (dashboards y alarms read-only)
   - "Operations Manager" (devices read, alarms write)
2. Admin configura permisos de cada rol
3. Admin reasigna usuarios en batch o individualmente
4. Usuarios reciben notificación de cambio de rol
5. Próximo login aplica nuevos permisos

**Resultado:** Nueva estructura de permisos aplicada gradualmente

---

## Casos de Uso Técnicos

### Caso Técnico 1: Cache de Permisos

**Escenario:** Usuario realiza 1000 requests en 1 minuto

**Sin Cache:**
- 1000 queries a BD para validar permisos
- ~50ms por query = 50 segundos total
- DB overload

**Con Cache:**
- 1 query a BD (cache miss inicial)
- 999 hits desde Redis/Caffeine
- ~1ms por hit = 1 segundo total
- Performance mejorada 50x

**Implementación:**
```java
@Cacheable(value = "rolePermissions", key = "#roleId")
public Set<RolePermission> getRolePermissions(RoleId roleId) {
    return permissionDao.findByRoleId(roleId);
}

@CacheEvict(value = "rolePermissions", key = "#roleId")
public void updateRolePermissions(RoleId roleId, Set<RolePermission> permissions) {
    permissionDao.deleteByRoleId(roleId);
    permissionDao.saveAll(permissions);
}
```

---

### Caso Técnico 2: Invalidación de Sesiones

**Escenario:** Admin cambia rol de usuario con sesión activa

**Problema:** Usuario mantiene permisos antiguos hasta re-login

**Solución:**
1. Al cambiar rol, publicar evento `UserRoleChangedEvent`
2. Listener invalida sesiones del usuario en Redis
3. Próximo request del usuario recibe 401 Unauthorized
4. Frontend detecta 401, redirige a login
5. Usuario re-autentica con nuevos permisos

**Código:**
```java
@EventListener
public void onUserRoleChanged(UserRoleChangedEvent event) {
    sessionService.invalidateUserSessions(event.getUserId());
}
```

---

### Caso Técnico 3: Permisos Jerárquicos

**Escenario:** Operation.ALL incluye todas las operaciones

**Validación:**
```java
public boolean hasPermission(Resource resource, Operation operation) {
    return permissions.stream()
        .anyMatch(p -> 
            p.getResource() == resource && 
            (p.getOperation() == operation || p.getOperation() == Operation.ALL)
        );
}
```

**Ejemplo:**
- Permiso: DEVICE:ALL
- Valida: DEVICE:READ ✅
- Valida: DEVICE:WRITE ✅
- Valida: DEVICE:DELETE ✅
- Valida: ASSET:READ ❌

---

## Matriz de Casos de Uso vs Features

| Caso de Uso | Sprint | Prioridad | Status |
|-------------|--------|-----------|--------|
| UC-01: Crear Rol | 9-11 | Alta | Planificado |
| UC-02: Configurar Permisos | 9-11 | Alta | Planificado |
| UC-03: Crear Usuario con Rol | 12-13 | Alta | Planificado |
| UC-04: Acceso con Permisos | 5-6 | Alta | Planificado |
| UC-05: Validación Backend | 3-4 | Crítica | Planificado |
| UC-06: Cambiar Rol | 12-13 | Media | Planificado |
| UC-07: Eliminar Rol | 9-11 | Media | Planificado |
| UC-08: Migración Legacy | 14 | Crítica | Planificado |
| UC-09: Clonar Rol | Post-MVP | Baja | Backlog |
| UC-10: Auditar Cambios | 14-15 | Media | Planificado |

---

## Checklist de Aceptación

### Para cada Caso de Uso

- [ ] Happy path implementado y testeado
- [ ] Flujos alternativos cubiertos
- [ ] Validaciones en frontend y backend
- [ ] Mensajes de error claros
- [ ] Auditoría registrada
- [ ] Tests unitarios >= 80%
- [ ] Tests E2E del flujo completo
- [ ] Documentación actualizada
- [ ] Performance validada
- [ ] Accesibilidad verificada

---

**Versión:** 1.0  
**Última Actualización:** 23 Enero 2026
