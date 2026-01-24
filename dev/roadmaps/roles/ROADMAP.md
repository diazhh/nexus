# ROADMAP - Sistema de Roles y Permisos

## Visión General

Transformar ThingsBoard CE con un sistema de roles y permisos configurables similar a la versión PE, permitiendo a los administradores de tenant crear y gestionar roles personalizados con permisos granulares.

## Objetivos Principales

### 🎯 Objetivo 1: Roles Personalizables
Permitir a los tenant admins crear roles custom con nombres y descripciones personalizadas, no limitarse a los 3 roles hardcodeados actuales (SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER).

### 🎯 Objetivo 2: Permisos Granulares
Implementar sistema de permisos basado en matriz Resource × Operation, permitiendo control fino sobre qué puede hacer cada rol (ej: leer devices pero no crearlos).

### 🎯 Objetivo 3: Usuarios Independientes
Permitir crear usuarios de tenant directamente sin requerir asignación a un customer, útil para usuarios administrativos o de soporte.

### 🎯 Objetivo 4: UI de Gestión
Crear interfaz web completa para:
- Listar, crear, editar y eliminar roles
- Configurar permisos mediante matriz visual
- Asignar roles a usuarios
- Crear usuarios directamente con rol

### 🎯 Objetivo 5: Retrocompatibilidad
Mantener funcionamiento del sistema actual mientras se migra gradualmente al nuevo sistema, sin romper funcionalidad existente.

## Alcance del Proyecto

### ✅ Incluido

**Backend:**
- Nuevas entidades: Role, RolePermission
- Servicios: RoleService, modificaciones a UserService
- Controllers: RoleController, modificaciones a UserController
- Sistema de validación de permisos dinámico
- Cache de permisos para performance
- Scripts de migración SQL
- Tests unitarios e integración

**Frontend:**
- Módulo completo de gestión de roles
- Componentes de UI para CRUD de roles
- Matriz visual de permisos
- Integración con gestión de usuarios
- Selector de roles en creación de usuarios
- Tests de componentes

**Base de Datos:**
- Tabla `role` para almacenar roles
- Tabla `role_permission` para permisos
- Modificación de `tb_user` para incluir `role_id`
- Índices para performance
- Scripts de migración de datos existentes

**Documentación:**
- Documentación técnica completa
- Guías de usuario
- Ejemplos de uso
- Release notes

### ❌ No Incluido (Futuras Fases)

- Permisos a nivel de entidad individual (ej: acceso solo a device X)
- Grupos de usuarios
- Herencia de roles
- Permisos temporales o con expiración
- Integración con sistemas externos de IAM
- Multi-tenancy de roles (compartir roles entre tenants)

## Fases de Implementación

### Fase 1: Fundamentos (Sprints 1-4, 8 semanas)

**Objetivos:**
- Crear estructura de base de datos
- Implementar modelos y DAOs
- Servicios core de roles y permisos
- Sistema básico de validación

**Entregables:**
- Tablas `role` y `role_permission` creadas
- Entidades Java Role, RolePermission
- RoleDao, RolePermissionDao funcionales
- RoleService implementado
- Tests unitarios >= 80% coverage

**Criterios de Éxito:**
- [ ] Puedo crear un rol via código
- [ ] Puedo asignar permisos a un rol
- [ ] Puedo asignar rol a un usuario
- [ ] Sistema valida permisos correctamente
- [ ] Tests pasan en CI/CD

### Fase 2: APIs REST (Sprints 5-6, 4 semanas)

**Objetivos:**
- Exponer funcionalidad vía REST API
- Implementar validaciones de seguridad
- Documentar APIs con Swagger

**Entregables:**
- RoleController completo
- Endpoints en UserController para gestión de roles
- Anotación @RequirePermission funcional
- Documentación Swagger actualizada
- Tests de integración de APIs

**Criterios de Éxito:**
- [ ] Puedo hacer CRUD de roles vía API
- [ ] Puedo asignar/modificar permisos vía API
- [ ] Puedo crear usuarios con rol vía API
- [ ] APIs validadas con Postman/curl
- [ ] Respuestas cumplen estándares REST

### Fase 3: Frontend Base (Sprints 7-8, 4 semanas)

**Objetivos:**
- Crear modelos TypeScript
- Implementar servicios Angular
- Preparar base para componentes UI

**Entregables:**
- Modelos role.models.ts
- RoleService Angular
- Modificaciones a UserService
- Tests de servicios Angular
- Interceptores HTTP configurados

**Criterios de Éxito:**
- [ ] RoleService puede consumir APIs
- [ ] Modelos TypeScript correctamente tipados
- [ ] Manejo de errores implementado
- [ ] Tests unitarios de servicios >= 80%
- [ ] Integración con AuthService

### Fase 4: UI de Roles (Sprints 9-11, 6 semanas)

**Objetivos:**
- Crear interfaz completa de gestión de roles
- Implementar matriz de permisos
- Integrar con routing y navegación

**Entregables:**
- Componente de lista de roles
- Formulario de creación/edición de rol
- Dialog de matriz de permisos
- Routing configurado
- Tests de componentes
- Traducción i18n

**Criterios de Éxito:**
- [ ] Puedo ver lista de roles en UI
- [ ] Puedo crear rol nuevo
- [ ] Puedo editar rol existente
- [ ] Puedo configurar permisos visualmente
- [ ] Puedo eliminar rol (validando dependencias)
- [ ] UI responsive y accesible

### Fase 5: UI de Usuarios Mejorada (Sprints 12-13, 4 semanas)

**Objetivos:**
- Modificar gestión de usuarios para soportar roles
- Permitir creación de usuarios sin customer
- Integrar selector de roles

**Entregables:**
- AddUserDialog modificado con roles
- Componente selector de roles
- Lista de usuarios del tenant
- Funcionalidad de cambio de rol
- Tests de componentes

**Criterios de Éxito:**
- [ ] Puedo crear usuario seleccionando rol
- [ ] Puedo crear usuario sin customer
- [ ] Puedo cambiar rol de usuario existente
- [ ] Veo rol de usuario en lista
- [ ] Validaciones funcionan correctamente

### Fase 6: Migración y Estabilización (Sprints 14-16, 6 semanas)

**Objetivos:**
- Migrar datos existentes
- Asegurar retrocompatibilidad
- Testing exhaustivo
- Documentación final

**Entregables:**
- Scripts SQL de migración
- Código de retrocompatibilidad
- Feature flags implementados
- Suite completa de tests E2E
- Documentación de usuario
- Release notes

**Criterios de Éxito:**
- [ ] Migración de datos 100% exitosa
- [ ] Sistema legacy funciona sin cambios
- [ ] Tests E2E pasan al 100%
- [ ] Performance igual o mejor
- [ ] Documentación completa y revisada
- [ ] Ready for production

## Cronograma

```
Mes 1-2   : Sprints 1-4  - Fase 1: Fundamentos
Mes 3     : Sprints 5-6  - Fase 2: APIs REST
Mes 4     : Sprints 7-8  - Fase 3: Frontend Base
Mes 5-6   : Sprints 9-11 - Fase 4: UI de Roles
Mes 7     : Sprints 12-13- Fase 5: UI de Usuarios
Mes 8     : Sprints 14-16- Fase 6: Migración y Release
```

**Duración Total:** 8 meses (32 semanas)  
**Esfuerzo Estimado:** 2-3 desarrolladores full-time  

## Recursos Necesarios

### Equipo

- **1 Backend Developer** (Java/Spring Boot)
- **1 Frontend Developer** (Angular/TypeScript)
- **1 Full-Stack Developer** (compartido entre backend y frontend)
- **1 QA Engineer** (a partir del Sprint 10)
- **1 Tech Lead** (revisión y arquitectura, 50% dedicación)

### Infraestructura

- Ambiente de desarrollo local
- Ambiente de staging para testing
- Pipeline CI/CD configurado
- Base de datos PostgreSQL de desarrollo
- Herramientas de testing (Jest, Jasmine, Mockito)

### Conocimientos Técnicos Requeridos

- Java 11+, Spring Boot, Spring Security
- PostgreSQL, SQL avanzado
- Angular 14+, TypeScript, RxJS
- REST API design
- Unit testing y Integration testing
- Git, Maven/Gradle

## Riesgos y Mitigación

### Riesgo 1: Complejidad de Migración
**Probabilidad:** Media  
**Impacto:** Alto  
**Mitigación:**
- Crear scripts de migración exhaustivamente testeados
- Implementar rollback automático
- Fase piloto con subset de datos
- Feature flag para activar/desactivar nuevo sistema

### Riesgo 2: Performance en Validación de Permisos
**Probabilidad:** Media  
**Impacto:** Medio  
**Mitigación:**
- Implementar cache de permisos (Redis/Caffeine)
- Optimizar queries con índices apropiados
- Load testing desde Sprint 10
- Monitoreo de performance en producción

### Riesgo 3: Resistencia al Cambio de Usuarios
**Probabilidad:** Baja  
**Impacto:** Medio  
**Mitigación:**
- Documentación clara de usuario
- Videos tutoriales
- Migración gradual opcional
- Soporte dedicado post-release

### Riesgo 4: Bugs en Retrocompatibilidad
**Probabilidad:** Media  
**Impacto:** Alto  
**Mitigación:**
- Suite completa de regression tests
- Testing manual de flujos legacy
- Beta testing con usuarios seleccionados
- Plan de rollback documentado

### Riesgo 5: Scope Creep
**Probabilidad:** Alta  
**Impacto:** Medio  
**Mitigación:**
- Definición clara de alcance (ver sección "No Incluido")
- Change request process formal
- Priorización rigurosa de features
- Comunicación constante con stakeholders

## Métricas de Éxito

### Métricas Técnicas

- **Code Coverage:** >= 80% para backend, >= 75% para frontend
- **Performance:** Tiempo de validación de permisos < 10ms (P95)
- **Disponibilidad:** >= 99.9% durante migración
- **Error Rate:** < 0.1% en APIs de roles
- **Response Time:** APIs < 200ms (P95)

### Métricas de Negocio

- **Adopción:** >= 70% de tenants usando nuevo sistema en 3 meses post-release
- **Creación de Roles:** Promedio >= 3 roles custom por tenant
- **Satisfacción:** NPS >= 8/10 en encuesta post-implementación
- **Tickets de Soporte:** Reducción >= 30% en tickets relacionados a permisos
- **Time to Market:** Nuevos roles creados en < 5 minutos

### Métricas de Calidad

- **Bugs Críticos:** 0 en producción primer mes
- **Bugs Severos:** < 5 en producción primer mes
- **Time to Resolution:** < 24h para bugs críticos
- **Test Pass Rate:** >= 98% en CI/CD
- **Documentation Coverage:** 100% de APIs documentadas

## Dependencias

### Dependencias Externas

- ThingsBoard CE versión base estable (3.7+)
- PostgreSQL 12+
- Angular 14+
- Spring Boot 2.7+

### Dependencias Internas

- Sistema de autenticación actual debe estar estable
- APIs de User y Customer deben ser estables
- Frontend framework y componentes base actualizados

## Entregables Finales

### Código

- [ ] Código backend en rama `feature/roles-and-permissions`
- [ ] Código frontend en misma rama
- [ ] Scripts SQL de migración versionados
- [ ] Tests >= 80% coverage

### Documentación

- [ ] Especificación técnica completa
- [ ] API documentation (Swagger)
- [ ] Guía de usuario final
- [ ] Guía de administrador
- [ ] Release notes detallados
- [ ] Troubleshooting guide

### Testing

- [ ] Suite de tests unitarios
- [ ] Suite de tests de integración
- [ ] Suite de tests E2E
- [ ] Plan de testing manual
- [ ] Resultados de load testing

### Deployment

- [ ] Scripts de migración probados
- [ ] Feature flags configurados
- [ ] Rollback plan documentado
- [ ] Monitoring y alertas configurados
- [ ] Runbook operacional

## Siguientes Pasos

1. **Semana 1:** Revisión y aprobación de roadmap con stakeholders
2. **Semana 2:** Setup de ambientes y herramientas
3. **Semana 3:** Kick-off Sprint 1 - Diseño detallado de base de datos
4. **Semana 4:** Implementación inicial de tablas y entidades

## Aprobaciones Requeridas

- [ ] Tech Lead / Arquitecto de Software
- [ ] Product Owner / Manager
- [ ] QA Lead
- [ ] DevOps / Infraestructura
- [ ] Stakeholders de Negocio

---

**Versión:** 1.0  
**Fecha:** Enero 2026  
**Autor:** Equipo de Desarrollo ThingsBoard  
**Última Actualización:** 23 Enero 2026
