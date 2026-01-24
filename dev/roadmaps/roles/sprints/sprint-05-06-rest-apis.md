# Sprint 5-6: REST APIs (2 semanas)

## Objetivos del Sprint

Exponer la funcionalidad de roles y permisos mediante APIs REST seguras y bien documentadas.

## User Stories

### US-12: Como frontend developer, necesito endpoints de Roles
**Prioridad:** Alta | **Puntos:** 13

**Criterios de Aceptación:**
- [x] GET, POST, DELETE `/api/role` implementados
- [x] Paginación funcional
- [x] Validaciones de seguridad aplicadas
- [x] Respuestas con códigos HTTP correctos

**Tareas:**
1. Crear `RoleController.java`
2. Implementar `@GetMapping` para lista paginada
3. Implementar `@GetMapping("/{id}")` para detalle
4. Implementar `@PostMapping` para crear/actualizar
5. Implementar `@DeleteMapping("/{id}")`
6. Agregar `@PreAuthorize` en todos los métodos
7. Implementar manejo de errores
8. Validar permisos de tenant
9. Crear tests de controller con MockMvc
10. Documentar con Swagger annotations

**Estimación:** 3 días

---

### US-13: Como frontend developer, necesito endpoints de Permisos
**Prioridad:** Alta | **Puntos:** 8

**Criterios de Aceptación:**
- [x] GET/POST `/api/role/{id}/permissions` implementados
- [x] GET `/api/role/resources` y `/api/role/operations`
- [x] Validación de permisos válidos

**Tareas:**
1. Implementar `getPermissions(roleId)`
2. Implementar `updatePermissions(roleId, permissions)`
3. Implementar `getAvailableResources()`
4. Implementar `getAvailableOperations()`
5. Validar que resource/operation sean válidos
6. Crear tests de endpoints
7. Documentar en Swagger

**Estimación:** 2 días

---

### US-14: Como desarrollador, necesito @RequirePermission annotation
**Prioridad:** Alta | **Puntos:** 13

**Criterios de Aceptación:**
- [x] Annotation `@RequirePermission` creada
- [x] Aspect para interceptar métodos implementado
- [x] Integración con RoleBasedPermissionChecker
- [x] Tests de aspect >= 85%

**Tareas:**
1. Crear annotation `@RequirePermission.java`
2. Crear `PermissionAspect.java`
3. Implementar `@Around` advice
4. Integrar con SecurityContext
5. Llamar a PermissionChecker
6. Lanzar ThingsboardException si no autorizado
7. Configurar aspect en Spring context
8. Crear `PermissionAspectTest.java`
9. Testear con controllers reales
10. Documentar uso de annotation

**Estimación:** 3 días

---

### US-15: Como frontend developer, necesito endpoints de Usuarios modificados
**Prioridad:** Alta | **Puntos:** 8

**Criterios de Aceptación:**
- [x] POST `/api/user/tenant?roleId=X` implementado
- [x] GET `/api/user/tenant/users?roleId=X` con filtro
- [x] PUT `/api/user/{id}/role/{roleId}` implementado

**Tareas:**
1. Modificar `UserController.java`
2. Agregar método `createTenantUser(user, roleId)`
3. Agregar método `getTenantUsers(pageLink, roleId)`
4. Agregar método `changeUserRole(userId, roleId)`
5. Validar roleId pertenece al tenant
6. Actualizar tests de UserController
7. Documentar nuevos endpoints

**Estimación:** 2 días

---

## Tests de API

### Integration Tests con RestAssured

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class RoleApiIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    private String authToken;
    
    @BeforeEach
    void setUp() {
        authToken = loginAsTenantAdmin();
    }
    
    @Test
    void testCreateRole() {
        Role role = new Role();
        role.setName("API Test Role");
        
        given()
            .port(port)
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(role)
        .when()
            .post("/api/role")
        .then()
            .statusCode(200)
            .body("name", equalTo("API Test Role"))
            .body("id.id", notNullValue());
    }
    
    @Test
    void testGetRoles() {
        given()
            .port(port)
            .header("Authorization", "Bearer " + authToken)
            .queryParam("pageSize", 10)
            .queryParam("page", 0)
        .when()
            .get("/api/role")
        .then()
            .statusCode(200)
            .body("data", notNullValue())
            .body("totalElements", greaterThanOrEqualTo(0));
    }
    
    @Test
    void testUnauthorizedAccess() {
        given()
            .port(port)
            .contentType(ContentType.JSON)
        .when()
            .get("/api/role")
        .then()
            .statusCode(401);
    }
}
```

**Coverage Target:** >= 80%

---

## Documentación Swagger

```java
@Tag(name = "Role Management", description = "APIs for managing roles and permissions")
@RestController
@RequestMapping("/api/role")
public class RoleController {
    
    @Operation(
        summary = "Get role by ID",
        description = "Returns a single role by its ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role found"),
        @ApiResponse(responseCode = "404", description = "Role not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{roleId}")
    public Role getRoleById(
        @Parameter(description = "Role ID") @PathVariable String roleId
    ) {
        // Implementation
    }
}
```

---

## Definición de Hecho

- [x] Todos los endpoints implementados
- [x] Tests de integración >= 80%
- [x] Documentación Swagger completa
- [ ] Postman collection creada
- [x] Security validada
- [x] Error handling robusto
- [ ] Code review aprobado

---

**Sprint Goal:** APIs REST completas y documentadas para roles y permisos.

**Velocity Estimada:** 34 puntos
