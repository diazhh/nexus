# Resumen de Sprints - Sistema de Roles y Permisos

## Cronograma General

| Sprint | Semanas | Fase | Objetivo |
|--------|---------|------|----------|
| 1-2 | 1-2 | Base de Datos | Estructura de datos y modelos |
| 3-4 | 3-4 | Backend Services | Servicios y validación de permisos |
| 5-6 | 5-6 | REST APIs | Exposición de funcionalidad via API |
| 7-8 | 7-8 | Frontend Base | Modelos y servicios Angular |
| 9-11 | 9-13 | UI Roles | Interfaz de gestión de roles |
| 12-13 | 14-15 | UI Usuarios | Gestión de usuarios mejorada |
| 14 | 16 | Migración | Scripts y retrocompatibilidad |
| 15 | 17 | Testing E2E | Pruebas exhaustivas |
| 16 | 18 | Release | Documentación y despliegue |

**Duración Total:** 18 semanas (4.5 meses)  
**Equipo:** 2-3 desarrolladores full-time

---

## Sprint 1-2: Base de Datos y Modelos
**Estado:** Planificado  
**Puntos:** 37

### Entregables Principales
- Esquemas SQL de tablas `role` y `role_permission`
- Entidades Java (Role, RolePermission)
- DAOs implementados con JPA
- Modificación de tabla `tb_user`
- Tests >= 85% coverage

### Hitos
- [ ] Base de datos funcional
- [ ] DAOs completamente testeados
- [ ] Datos de seed creados

**[Ver detalles →](./sprint-01-02-database.md)**

---

## Sprint 3-4: Backend Services
**Estado:** Planificado  
**Puntos:** 47

### Entregables Principales
- RoleService implementado
- RoleBasedPermissionChecker con cache
- UserService modificado
- SecurityUser actualizado
- Performance benchmarks

### Hitos
- [ ] Servicios funcionales
- [ ] Cache operativo
- [ ] Performance targets cumplidos

**[Ver detalles →](./sprint-03-04-backend-services.md)**

---

## Sprint 5-6: REST APIs
**Estado:** Planificado  
**Puntos:** 34

### Entregables Principales
- RoleController completo
- UserController modificado
- Anotación @RequirePermission
- Documentación Swagger
- Tests de integración de APIs

### Hitos
- [ ] Todos los endpoints implementados
- [ ] Validación de seguridad funcional
- [ ] APIs documentadas

**[Ver detalles →](./sprint-05-06-rest-apis.md)**

---

## Sprint 7-8: Frontend Base
**Estado:** Planificado  
**Puntos:** 29

### Entregables Principales
- Modelos TypeScript (role.models.ts)
- RoleService Angular
- UserService modificado
- Interceptores HTTP
- Tests de servicios >= 80%

### Hitos
- [ ] Servicios frontend funcionales
- [ ] Integración con backend exitosa
- [ ] Manejo de errores robusto

**[Ver detalles →](./sprint-07-08-frontend-services.md)**

---

## Sprint 9-11: Frontend UI Roles
**Estado:** Planificado  
**Puntos:** 55

### Entregables Principales
- Módulo RoleModule completo
- Componente de lista de roles
- Formulario CRUD de roles
- Dialog de matriz de permisos
- Routing y navegación
- Traducción i18n completa

### Hitos
- [ ] UI de roles completamente funcional
- [ ] Matriz de permisos visual
- [ ] Integración con routing

**[Ver detalles →](./sprint-09-11-frontend-ui-roles.md)**

---

## Sprint 12-13: Frontend UI Usuarios
**Estado:** Planificado  
**Puntos:** 34

### Entregables Principales
- AddUserDialog modificado
- Selector de roles
- Lista de usuarios del tenant
- Funcionalidad de cambio de rol
- Tests de componentes

### Hitos
- [ ] Creación de usuarios con rol funcional
- [ ] Usuarios sin customer soportado
- [ ] Cambio de rol implementado

**[Ver detalles →](./sprint-12-13-frontend-ui-users.md)**

---

## Sprint 14: Migración y Retrocompatibilidad
**Estado:** Planificado  
**Puntos:** 21

### Entregables Principales
- Scripts de migración SQL
- Código de retrocompatibilidad
- Feature flags
- Plan de rollback
- Documentación de migración

### Hitos
- [ ] Migración testeada en staging
- [ ] Rollback validado
- [ ] Datos migrados correctamente

**[Ver detalles →](./sprint-14-migration.md)**

---

## Sprint 15: Testing End-to-End
**Estado:** Planificado  
**Puntos:** 21

### Entregables Principales
- Suite E2E completa
- Tests de performance
- Tests de seguridad
- Regression tests
- Reporte de bugs

### Hitos
- [ ] E2E tests al 100%
- [ ] Cero bugs críticos
- [ ] Performance validada

**[Ver detalles →](./sprint-15-testing.md)**

---

## Sprint 16: Release y Documentación
**Estado:** Planificado  
**Puntos:** 13

### Entregables Principales
- Documentación de usuario final
- Release notes
- Guías de administración
- Videos tutoriales
- Plan de soporte

### Hitos
- [ ] Documentación completa
- [ ] Release notes publicados
- [ ] Sistema en producción

**[Ver detalles →](./sprint-16-release.md)**

---

## Métricas de Progreso

### Velocity por Sprint
```
Sprint 1-2:  37 puntos
Sprint 3-4:  47 puntos
Sprint 5-6:  34 puntos
Sprint 7-8:  29 puntos
Sprint 9-11: 55 puntos
Sprint 12-13: 34 puntos
Sprint 14:   21 puntos
Sprint 15:   21 puntos
Sprint 16:   13 puntos
──────────────────────
TOTAL:      291 puntos
```

### Distribución de Esfuerzo
- **Backend:** 40% (Sprints 1-6)
- **Frontend:** 35% (Sprints 7-13)
- **Testing/QA:** 15% (Sprint 15)
- **Migración/Release:** 10% (Sprints 14, 16)

---

## Dependencias entre Sprints

```
Sprint 1-2 (DB)
    ↓
Sprint 3-4 (Services)
    ↓
Sprint 5-6 (APIs)
    ↓
Sprint 7-8 (Frontend Base)
    ↓
Sprint 9-11 (UI Roles) ←→ Sprint 12-13 (UI Users)
    ↓
Sprint 14 (Migration)
    ↓
Sprint 15 (Testing)
    ↓
Sprint 16 (Release)
```

**Crítico:** Sprints 1-6 son secuenciales y bloqueantes.  
**Paralelo:** Sprints 9-11 y 12-13 pueden ejecutarse parcialmente en paralelo.

---

## Recursos por Sprint

### Sprints 1-6 (Backend)
- **Requerido:** 1 Backend Developer senior
- **Opcional:** 1 DBA para consultas
- **Herramientas:** Java, Spring Boot, PostgreSQL

### Sprints 7-13 (Frontend)
- **Requerido:** 1 Frontend Developer senior
- **Opcional:** 1 UX Designer para matriz de permisos
- **Herramientas:** Angular, TypeScript, Material

### Sprint 14 (Migración)
- **Requerido:** 1 Full-stack + 1 DBA
- **Herramientas:** SQL, Scripts de migración

### Sprint 15 (Testing)
- **Requerido:** 1 QA Engineer + 1 Developer
- **Herramientas:** Protractor, JMeter, Postman

### Sprint 16 (Release)
- **Requerido:** Tech Writer + Product Manager
- **Herramientas:** Markdown, Video editor

---

## Checklist de Inicio de Sprint

Antes de comenzar cada sprint:

- [ ] Backlog del sprint refinado
- [ ] User stories estimadas
- [ ] Tareas asignadas
- [ ] Ambiente de desarrollo listo
- [ ] Dependencias resueltas
- [ ] Sprint goal definido
- [ ] DoD acordado
- [ ] Reunión de planning completada

---

## Checklist de Fin de Sprint

Al finalizar cada sprint:

- [ ] Todos los tests pasan
- [ ] Code review completado
- [ ] Documentación actualizada
- [ ] Demo preparada
- [ ] Sprint review realizado
- [ ] Retrospectiva completada
- [ ] Velocidad registrada
- [ ] Backlog actualizado

---

## Riesgos Generales

### Alto Impacto
1. **Complejidad de migración** → Mitigation: Scripts exhaustivos, rollback plan
2. **Performance de permisos** → Mitigation: Cache, benchmarking continuo
3. **Scope creep** → Mitigation: Change control estricto

### Medio Impacto
1. **Bugs en retrocompatibilidad** → Mitigation: Regression tests
2. **Resistencia al cambio** → Mitigation: Documentación, capacitación
3. **Disponibilidad de recursos** → Mitigation: Plan de contingencia

### Bajo Impacto
1. **Problemas de UI/UX** → Mitigation: Feedback temprano
2. **Issues de i18n** → Mitigation: Revisión de traducciones

---

**Última Actualización:** 23 Enero 2026  
**Próximo Review:** Inicio de Sprint 1
