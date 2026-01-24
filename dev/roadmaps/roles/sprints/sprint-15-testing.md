# Sprint 15: Testing End-to-End (1 semana)

## Objetivos del Sprint

Ejecutar suite completa de tests E2E, performance y seguridad para validar el sistema completo.

## User Stories

### US-30: Tests End-to-End con Protractor
**Prioridad:** Crítica | **Puntos:** 13

**Criterios de Aceptación:**
- [ ] Suite E2E completa implementada
- [ ] Todos los flujos principales cubiertos
- [ ] Tests pasan al 100%
- [ ] Screenshots de fallos capturados

**Tareas:**
1. Crear `role-management.e2e-spec.ts`
2. Test: Crear rol
3. Test: Configurar permisos
4. Test: Asignar rol a usuario
5. Test: Validar accesos del usuario
6. Test: Modificar rol
7. Test: Eliminar rol
8. Crear `user-with-roles.e2e-spec.ts`
9. Test: Crear usuario con rol
10. Test: Cambiar rol de usuario
11. Test: Usuario sin permisos recibe 403
12. Ejecutar suite completa
13. Documentar resultados

**Estimación:** 3 días

---

### US-31: Performance Testing
**Prioridad:** Alta | **Puntos:** 5

**Criterios de Aceptación:**
- [ ] Load tests con JMeter ejecutados
- [ ] Targets de performance cumplidos
- [ ] Reporte de performance generado
- [ ] Optimizaciones aplicadas si necesario

**Tareas:**
1. Crear plan de test JMeter
2. Test: 1000 usuarios concurrentes
3. Test: 10,000 validaciones de permisos
4. Medir response times (P95, P99)
5. Medir cache hit rate
6. Analizar bottlenecks
7. Optimizar si necesario
8. Generar reporte de performance

**Estimación:** 2 días

---

### US-32: Security Testing
**Prioridad:** Alta | **Puntos:** 3

**Criterios de Aceptación:**
- [ ] Tests de seguridad ejecutados
- [ ] No hay vulnerabilidades críticas
- [ ] Autorizaciones validadas
- [ ] OWASP Top 10 verificado

**Tareas:**
1. Test: SQL Injection prevention
2. Test: XSS prevention
3. Test: CSRF protection
4. Test: Privilege escalation prevention
5. Test: Session hijacking prevention
6. Ejecutar OWASP ZAP scan
7. Revisar resultados
8. Documentar findings

**Estimación:** 1 día

---

## Suite E2E Completa

### role-management.e2e-spec.ts
```typescript
describe('Role Management E2E', () => {
  let page: RoleManagementPage;
  let loginPage: LoginPage;
  
  beforeAll(async () => {
    page = new RoleManagementPage();
    loginPage = new LoginPage();
    await loginPage.navigateTo();
    await loginPage.loginAsTenantAdmin();
  });
  
  it('should complete full role lifecycle', async () => {
    // Navigate to roles
    await page.navigateTo();
    expect(await page.getPageTitle()).toBe('Roles');
    
    // Create role
    await page.clickAddButton();
    await page.fillRoleName('E2E Test Role');
    await page.fillRoleDescription('Created in E2E test');
    await page.clickSaveButton();
    
    expect(await page.getSuccessMessage()).toContain('Role created');
    expect(await page.getRoleByName('E2E Test Role')).toBeTruthy();
    
    // Configure permissions
    await page.clickRoleByName('E2E Test Role');
    await page.clickManagePermissions();
    
    await page.togglePermission('DEVICE', 'READ');
    await page.togglePermission('DEVICE', 'WRITE');
    await page.togglePermission('ASSET', 'READ');
    await page.togglePermission('DASHBOARD', 'READ');
    
    await page.clickSavePermissions();
    expect(await page.getSuccessMessage()).toContain('Permissions updated');
    
    // Create user with role
    await page.navigateToUsers();
    await page.clickAddUserButton();
    
    await page.fillUserEmail('e2e-test@example.com');
    await page.fillUserFirstName('E2E');
    await page.fillUserLastName('Test');
    await page.selectRole('E2E Test Role');
    await page.checkCreateAsTenantUser();
    
    await page.clickSaveUser();
    expect(await page.getSuccessMessage()).toContain('User created');
    
    // Verify user permissions
    await loginPage.logout();
    await loginPage.login('e2e-test@example.com', 'password');
    
    // Should have access to devices
    await page.navigateToDevices();
    expect(await page.isPageAccessible()).toBeTruthy();
    
    // Should NOT have access to users
    await page.navigateToUsers();
    expect(await page.getErrorMessage()).toContain('Access denied');
    
    // Cleanup
    await loginPage.logout();
    await loginPage.loginAsTenantAdmin();
    await page.navigateToRoles();
    await page.deleteRoleByName('E2E Test Role');
    await page.confirmDeletion();
  });
});
```

---

## Performance Testing con JMeter

### permission-check.jmx
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.comments">
        Permission Check Load Test
      </stringProp>
      <elementProp name="TestPlan.user_defined_variables">
        <collectionProp name="Arguments.arguments">
          <elementProp name="USERS" elementType="Argument">
            <stringProp name="Argument.value">1000</stringProp>
          </elementProp>
          <elementProp name="RAMP_UP" elementType="Argument">
            <stringProp name="Argument.value">60</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    
    <ThreadGroup>
      <stringProp name="ThreadGroup.num_threads">${USERS}</stringProp>
      <stringProp name="ThreadGroup.ramp_time">${RAMP_UP}</stringProp>
      <stringProp name="ThreadGroup.loops">100</stringProp>
      
      <HTTPSamplerProxy>
        <stringProp name="HTTPSampler.path">/api/device</stringProp>
        <stringProp name="HTTPSampler.method">GET</stringProp>
      </HTTPSamplerProxy>
    </ThreadGroup>
  </hashTree>
</jmeterTestPlan>
```

### Targets de Performance
| Métrica | Target | Crítico |
|---------|--------|---------|
| Response Time P95 | < 200ms | < 500ms |
| Permission Check P95 | < 10ms | < 50ms |
| Error Rate | < 0.1% | < 1% |
| Throughput | > 1000 req/s | > 500 req/s |
| Cache Hit Rate | > 95% | > 90% |

---

## Regression Testing

### Checklist de Regression
- [ ] Login con usuario legacy (sin rol) funciona
- [ ] Tenant Admin mantiene todos los permisos
- [ ] Customer User mantiene permisos limitados
- [ ] APIs existentes responden correctamente
- [ ] UI legacy sin cambios funciona
- [ ] Performance no degradada
- [ ] Datos existentes intactos

---

## Bug Tracking

### Severidad de Bugs
- **Crítico:** Sistema no funciona, pérdida de datos
- **Alto:** Feature principal no funciona
- **Medio:** Feature secundario con workaround
- **Bajo:** Cosmético, documentación

### Criterios de Éxito
- **Cero bugs críticos** antes de release
- **< 5 bugs altos** antes de release
- **Bugs medios/bajos** documentados para siguiente sprint

---

## Definición de Hecho

- [ ] Suite E2E completa ejecutada
- [ ] 100% de tests E2E pasan
- [ ] Performance tests ejecutados
- [ ] Targets de performance cumplidos
- [ ] Security tests ejecutados
- [ ] Cero vulnerabilidades críticas
- [ ] Regression tests pasan
- [ ] Bugs documentados en tracker
- [ ] Reporte de testing generado
- [ ] Sign-off de QA obtenido

---

**Sprint Goal:** Sistema completamente testeado y validado para producción.

**Velocity Estimada:** 21 puntos
