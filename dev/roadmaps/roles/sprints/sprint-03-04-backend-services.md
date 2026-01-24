# Sprint 3-4: Backend Services (2 semanas)

## Objetivos del Sprint

Implementar la capa de servicios para gestión de roles y permisos, incluyendo validación de permisos y cache.

## User Stories

### US-7: Como desarrollador, necesito RoleService implementado
**Prioridad:** Alta  
**Puntos:** 13

**Criterios de Aceptación:**
- [x] Interface `RoleService` definida con todos los métodos
- [x] `DefaultRoleService` implementado
- [x] Validaciones de negocio aplicadas
- [x] Transacciones configuradas correctamente
- [x] Tests unitarios >= 85%

**Tareas:**
1. Crear interface `RoleService.java`
2. Crear `DefaultRoleService.java`
3. Implementar `saveRole()` con validaciones
4. Implementar `findRoleById()`
5. Implementar `findRolesByTenantId()` con paginación
6. Implementar `deleteRole()` con validaciones
7. Implementar `updateRolePermissions()`
8. Implementar `getRolePermissions()` con cache
9. Implementar `createDefaultTenantRoles()`
10. Agregar validación de roles del sistema
11. Agregar validación de usuarios asignados
12. Crear `RoleServiceTest.java`
13. Implementar tests con mocks
14. Verificar coverage >= 85%

**Estimación:** 5 días

---

### US-8: Como desarrollador, necesito sistema de validación de permisos
**Prioridad:** Crítica  
**Puntos:** 13

**Criterios de Aceptación:**
- [x] `RoleBasedPermissionChecker` implementado
- [x] Cache de permisos configurado
- [x] Validación de permisos funcional
- [x] Fallback a authority legacy funciona
- [x] Tests >= 90%

**Tareas:**
1. Crear `RoleBasedPermissionChecker.java`
2. Implementar `hasPermission(user, resource, operation)`
3. Implementar lógica de matching de permisos
4. Configurar cache Caffeine/Redis
5. Implementar fallback a legacy authority
6. Manejar Operation.ALL correctamente
7. Manejar Resource.ALL correctamente
8. Optimizar performance de validación
9. Crear `RoleBasedPermissionCheckerTest.java`
10. Testear cache hits/misses
11. Testear edge cases (null, empty)
12. Benchmark de performance

**Estimación:** 5 días

---

### US-9: Como desarrollador, necesito modificar UserService
**Prioridad:** Alta  
**Puntos:** 8

**Criterios de Aceptación:**
- [x] Método `saveUserWithRole()` implementado
- [x] Método `countUsersByRoleId()` implementado
- [x] Método `findUsersByRoleId()` implementado
- [x] Validaciones de rol agregadas
- [x] Tests actualizados

**Tareas:**
1. Modificar `UserService.java` interface
2. Agregar método `saveUserWithRole()`
3. Agregar método `countUsersByRoleId()`
4. Agregar método `findUsersByRoleId()` con paginación
5. Modificar `saveUser()` para soportar roleId
6. Validar que roleId pertenezca al tenant
7. Actualizar queries en UserDao
8. Modificar `DefaultUserService.java`
9. Actualizar `UserServiceTest.java`
10. Agregar tests para nuevos métodos

**Estimación:** 3 días

---

### US-10: Como sistema, necesito invalidación de cache
**Prioridad:** Alta  
**Puntos:** 5

**Criterios de Aceptación:**
- [x] Cache se invalida al actualizar permisos
- [x] Cache se invalida al eliminar rol
- [x] Cache distribuido (Redis) opcional funciona
- [x] Eventos de invalidación publicados
- [x] Tests de invalidación

**Tareas:**
1. Configurar `@CacheEvict` en métodos críticos
2. Implementar evento `RolePermissionsChangedEvent`
3. Crear listener de eventos
4. Configurar Redis cache manager (opcional)
5. Implementar fallback a local cache
6. Crear tests de invalidación
7. Verificar invalidación entre instancias (Redis)

**Estimación:** 2 días

---

### US-11: Como desarrollador, necesito migrar SecurityUser
**Prioridad:** Alta  
**Puntos:** 8

**Criterios de Aceptación:**
- [x] Campo `roleId` agregado a SecurityUser
- [x] Campo `permissions` agregado
- [x] Método `getAuthority()` compatible con legacy
- [x] Método `hasPermission()` implementado
- [x] Tests actualizados

**Tareas:**
1. Modificar clase `SecurityUser`
2. Agregar campo `RoleId roleId`
3. Agregar campo `Set<RolePermission> permissions`
4. Modificar método `getAuthority()` para derivar de rol
5. Agregar método `hasPermission(resource, operation)`
6. Mantener retrocompatibilidad con authority
7. Actualizar constructores
8. Modificar `UserPrincipal` si necesario
9. Actualizar `SecurityUserTest.java`
10. Testear todos los escenarios (con rol, sin rol, legacy)

**Estimación:** 3 días

---

## Tareas Técnicas

### T-4: Configurar Cache Manager
**Prioridad:** Alta  
**Estimación:** 4 horas

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("rolePermissions");
        manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .recordStats());
        return manager;
    }
}
```

---

### T-5: Agregar Métricas de Performance
**Prioridad:** Media  
**Estimación:** 3 horas

```java
@Component
public class RoleMetrics {
    
    @Autowired
    private MeterRegistry registry;
    
    public void recordPermissionCheck(boolean cached, long durationMs) {
        Timer.builder("role.permission.check")
            .tag("cached", String.valueOf(cached))
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

---

### T-6: Crear Tenant Roles Trigger
**Prioridad:** Media  
**Estimación:** 2 horas

```sql
CREATE OR REPLACE FUNCTION create_default_tenant_roles()
RETURNS TRIGGER AS $$
BEGIN
    -- Crear roles default para nuevo tenant
    PERFORM create_tenant_roles(NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tenant_roles_trigger
    AFTER INSERT ON tenant
    FOR EACH ROW
    EXECUTE FUNCTION create_default_tenant_roles();
```

---

## Tests

### Tests Unitarios

**RoleServiceTest.java**
```java
@Test
void testSaveRole() {
    Role role = createTestRole();
    when(roleDao.save(any(), any())).thenReturn(role);
    
    Role saved = roleService.saveRole(role);
    
    assertNotNull(saved);
    verify(roleDao, times(1)).save(any(), any());
}

@Test
void testDeleteRoleWithUsers() {
    when(userService.countUsersByRoleId(any())).thenReturn(5L);
    
    assertThrows(DataValidationException.class, () -> {
        roleService.deleteRole(tenantId, roleId);
    });
}

@Test
void testDeleteSystemRole() {
    Role systemRole = createSystemRole();
    when(roleDao.findById(any(), any())).thenReturn(systemRole);
    
    assertThrows(DataValidationException.class, () -> {
        roleService.deleteRole(tenantId, roleId);
    });
}
```

**RoleBasedPermissionCheckerTest.java**
```java
@Test
void testSysAdminHasAllPermissions() {
    SecurityUser sysAdmin = createSysAdmin();
    
    assertTrue(checker.hasPermission(sysAdmin, Resource.DEVICE, Operation.DELETE));
}

@Test
void testUserWithPermission() {
    Set<RolePermission> perms = Set.of(
        new RolePermission(roleId, Resource.DEVICE, Operation.READ)
    );
    when(cache.get(any())).thenReturn(perms);
    
    assertTrue(checker.hasPermission(user, Resource.DEVICE, Operation.READ));
}

@Test
void testUserWithoutPermission() {
    when(cache.get(any())).thenReturn(Collections.emptySet());
    
    assertFalse(checker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
}

@Test
void testPermissionWithAllOperation() {
    Set<RolePermission> perms = Set.of(
        new RolePermission(roleId, Resource.DEVICE, Operation.ALL)
    );
    when(cache.get(any())).thenReturn(perms);
    
    assertTrue(checker.hasPermission(user, Resource.DEVICE, Operation.READ));
    assertTrue(checker.hasPermission(user, Resource.DEVICE, Operation.WRITE));
    assertTrue(checker.hasPermission(user, Resource.DEVICE, Operation.DELETE));
}
```

**Coverage Target:** >= 85%

---

### Tests de Integración

**RoleServiceIntegrationTest.java**
```java
@SpringBootTest
@Transactional
public class RoleServiceIntegrationTest {
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private UserService userService;
    
    @Test
    void testFullRoleLifecycle() {
        // Create
        Role role = new Role();
        role.setName("Integration Test Role");
        role.setTenantId(tenantId);
        
        Role created = roleService.saveRole(role);
        assertNotNull(created.getId());
        
        // Update permissions
        Set<RolePermission> perms = Set.of(
            new RolePermission(created.getId(), Resource.DEVICE, Operation.READ)
        );
        roleService.updateRolePermissions(created.getId(), perms);
        
        // Verify permissions
        Set<RolePermission> loaded = roleService.getRolePermissions(created.getId());
        assertEquals(1, loaded.size());
        
        // Delete
        roleService.deleteRole(tenantId, created.getId());
        
        // Verify deleted
        assertNull(roleService.findRoleById(tenantId, created.getId()));
    }
}
```

**Coverage Target:** >= 80%

---

## Performance Benchmarks

### Targets
| Operación | Target | Critical |
|-----------|--------|----------|
| Permission check (cached) | < 1ms | < 5ms |
| Permission check (uncached) | < 10ms | < 50ms |
| Save role | < 100ms | < 500ms |
| Update permissions | < 200ms | < 1s |
| Cache hit rate | > 95% | > 90% |

### Test con JMH

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class PermissionCheckBenchmark {
    
    @Benchmark
    public boolean testCachedPermissionCheck(BenchmarkState state) {
        return state.checker.hasPermission(
            state.user, 
            Resource.DEVICE, 
            Operation.READ
        );
    }
}
```

---

## Definición de Hecho (DoD)

- [x] Código implementado siguiendo estándares
- [x] Tests unitarios >= 85% coverage
- [x] Tests de integración >= 80% coverage
- [ ] Todos los tests pasan en CI/CD
- [ ] Performance benchmarks cumplen targets
- [x] Cache configurado y funcionando
- [x] Documentación JavaDoc completa
- [ ] Code review aprobado
- [ ] Sin code smells en SonarQube
- [x] Logs apropiados agregados

---

## Riesgos

### Riesgo 1: Performance de cache
**Mitigación:** Benchmarking temprano, ajustar tamaño y TTL según necesidad

### Riesgo 2: Complejidad de validación de permisos
**Mitigación:** Tests exhaustivos de edge cases, documentación clara

### Riesgo 3: Retrocompatibilidad con authority
**Mitigación:** Suite completa de regression tests

---

## Entregables

- [x] `RoleService` y `BaseRoleService`
- [x] `RoleBasedPermissionChecker`
- [x] `SecurityUser` modificado
- [x] `UserService` modificado
- [x] Cache configuration
- [x] Suite de tests completa
- [x] Documentación JavaDoc
- [ ] Performance benchmarks

---

**Sprint Goal:** Tener servicios backend completos y funcionales con validación de permisos.

**Velocity Estimada:** 47 puntos  
**Capacity del Equipo:** 2 desarrolladores × 10 días = 20 días-persona
