# Sprint 16: Release y Documentación (1 semana)

## Objetivos del Sprint

Preparar documentación final, release notes y desplegar sistema a producción.

## User Stories

### US-33: Documentación de Usuario Final
**Prioridad:** Alta | **Puntos:** 8

**Criterios de Aceptación:**
- [ ] Guía de usuario completa
- [ ] Screenshots y ejemplos visuales
- [ ] Paso a paso de flujos principales
- [ ] FAQs incluidas

**Tareas:**
1. Crear `USER_GUIDE.md`
2. Documentar cómo crear roles
3. Documentar cómo asignar permisos
4. Documentar cómo crear usuarios con roles
5. Agregar screenshots de UI
6. Documentar casos comunes de uso
7. Crear sección de troubleshooting
8. Crear FAQs
9. Revisar con UX writer

**Estimación:** 2 días

---

### US-34: Release Notes
**Prioridad:** Alta | **Puntos:** 3

**Criterios de Aceptación:**
- [ ] Release notes completos
- [ ] Breaking changes documentados
- [ ] Migration guide incluido
- [ ] Known issues listados

**Tareas:**
1. Crear `RELEASE_NOTES.md`
2. Listar features nuevas
3. Documentar breaking changes
4. Documentar migration path
5. Listar known issues y workarounds
6. Agregar upgrade instructions
7. Revisar con product manager

**Estimación:** 1 día

---

### US-35: Despliegue a Producción
**Prioridad:** Crítica | **Puntos:** 5

**Criterios de Aceptación:**
- [ ] Sistema desplegado en producción
- [ ] Migración ejecutada exitosamente
- [ ] Monitoreo configurado
- [ ] Rollback plan listo

**Tareas:**
1. Preparar ambiente de producción
2. Backup completo de base de datos
3. Ejecutar scripts de migración
4. Desplegar nuevo código
5. Ejecutar smoke tests
6. Verificar métricas de performance
7. Habilitar monitoreo y alertas
8. Documentar proceso de despliegue

**Estimación:** 2 días

---

## Documentación de Usuario

### USER_GUIDE.md - Estructura

```markdown
# Guía de Usuario - Sistema de Roles y Permisos

## Introducción
El nuevo sistema de roles permite a los administradores de tenant...

## Gestión de Roles

### Crear un Rol Nuevo
1. Navegar a "Roles" en el menú de administración
2. Hacer clic en "Agregar Rol"
3. Completar nombre y descripción
4. Guardar

[Screenshot del formulario]

### Configurar Permisos
1. Seleccionar rol de la lista
2. Hacer clic en "Gestionar Permisos"
3. Marcar permisos deseados en la matriz
4. Guardar cambios

[Screenshot de matriz de permisos]

### Roles del Sistema
Los siguientes roles vienen preconfigurados:
- **Tenant Administrator**: Acceso completo
- **Customer User**: Acceso limitado
...

## Gestión de Usuarios

### Crear Usuario con Rol
1. Navegar a "Usuarios"
2. Hacer clic en "Agregar Usuario"
3. Completar datos del usuario
4. Seleccionar rol
5. Marcar "Crear como usuario de tenant" si no requiere customer
6. Guardar

### Cambiar Rol de Usuario
1. Seleccionar usuario de la lista
2. Hacer clic en icono de "Cambiar Rol"
3. Seleccionar nuevo rol
4. Confirmar cambio

## Casos de Uso Comunes

### Caso 1: Device Manager
Crear un rol para usuarios que solo gestionan dispositivos...

### Caso 2: Dashboard Viewer
Crear un rol read-only para visualización...

## Troubleshooting

### Problema: Usuario no puede acceder a recurso
**Solución**: Verificar que el rol tenga los permisos necesarios...

## FAQs

**P: ¿Puedo eliminar un rol del sistema?**
R: No, los roles marcados como "sistema" no pueden eliminarse...

**P: ¿Qué pasa si elimino un rol con usuarios asignados?**
R: El sistema previene la eliminación y solicita reasignar usuarios...
```

---

## Release Notes

### RELEASE_NOTES_v3.8.0.md

```markdown
# ThingsBoard CE v3.8.0 - Enhanced Roles and Permissions

**Release Date:** Febrero 2026

## 🎉 Nuevas Funcionalidades

### Sistema de Roles y Permisos Configurables
- Creación de roles personalizados por tenant
- Configuración granular de permisos (Resource × Operation)
- Usuarios sin customer requerido
- Interfaz visual de matriz de permisos
- Cache de permisos para alta performance

## 🔄 Breaking Changes

### Backend
- Campo `authority` en `tb_user` ahora es nullable
- Nuevo campo `role_id` en `tb_user`
- Nuevas tablas: `role`, `role_permission`

### Frontend
- Nuevo módulo `RoleModule`
- Modificaciones en `AddUserDialog`
- Nueva ruta `/roles`

### API
- Nuevos endpoints: `/api/role/*`
- Modificaciones en `/api/user/*`

## 📦 Migration Guide

### Requisitos Previos
- PostgreSQL 12+
- Backup completo de base de datos
- Downtime estimado: 10-15 minutos

### Pasos de Migración
1. Detener aplicación
2. Backup de base de datos
3. Ejecutar `upgrade_to_roles_system.sql`
4. Verificar migración exitosa
5. Desplegar nuevo código
6. Iniciar aplicación
7. Validar funcionamiento

### Rollback
Si algo falla, ejecutar `rollback_roles_system.sql`

## ⚠️ Known Issues

### Issue #1: Performance en tenants con muchos usuarios
**Workaround**: Incrementar cache size en configuración

### Issue #2: Traducción incompleta en algunos idiomas
**Status**: En progreso para próximo release

## 🔧 Mejoras de Performance
- Cache de permisos con hit rate > 95%
- Permission check < 10ms (P95)
- Índices optimizados en base de datos

## 📚 Documentación
- [User Guide](./USER_GUIDE.md)
- [API Specification](./API_SPECIFICATION.md)
- [Migration Guide](./DATABASE_SCHEMA.md)

## 🙏 Agradecimientos
Gracias al equipo de desarrollo y todos los beta testers.
```

---

## Checklist de Release

### Pre-Release
- [ ] Todos los tests pasan
- [ ] Code freeze 48h antes
- [ ] Release notes finalizados
- [ ] Documentación completa
- [ ] Changelog actualizado
- [ ] Version bump en pom.xml/package.json
- [ ] Tag de git creado
- [ ] Aprobación de stakeholders

### Release Day
- [ ] Comunicación a usuarios (24h antes)
- [ ] Backup de producción
- [ ] Ventana de mantenimiento programada
- [ ] Scripts de migración listos
- [ ] Rollback plan validado
- [ ] Equipo en standby

### Despliegue
- [ ] Detener aplicación
- [ ] Ejecutar backup
- [ ] Ejecutar migración
- [ ] Desplegar código
- [ ] Ejecutar smoke tests
- [ ] Validar métricas
- [ ] Activar monitoreo

### Post-Release
- [ ] Comunicar éxito de despliegue
- [ ] Monitorear por 24h
- [ ] Recolectar feedback
- [ ] Documentar issues
- [ ] Celebrar 🎉

---

## Monitoreo Post-Release

### Métricas a Vigilar (Primeras 24h)
```
Dashboard: Roles System Health

┌─ Permission Checks ────────┐  ┌─ API Performance ──────────┐
│ Rate: 1,234/sec            │  │ P95: 45ms                  │
│ Cache Hit: 97.3%           │  │ P99: 120ms                 │
│ Errors: 0.02%              │  │ Error Rate: 0.01%          │
└────────────────────────────┘  └────────────────────────────┘

┌─ Database ─────────────────┐  ┌─ Users ────────────────────┐
│ Query Time: 12ms           │  │ New Roles: 45              │
│ Connections: 42/100        │  │ Permissions Changed: 123   │
│ Lock Wait: 0ms             │  │ Failed Logins: 2           │
└────────────────────────────┘  └────────────────────────────┘
```

### Alertas Críticas
- Permission check P95 > 50ms
- Error rate > 1%
- Cache hit rate < 90%
- Database connection pool > 80%

---

## Plan de Soporte

### Semana 1 Post-Release
- **Cobertura**: 24/7
- **Equipo**: 2 devs on-call
- **SLA**: Response < 1h para críticos

### Semana 2-4 Post-Release
- **Cobertura**: Horario laboral + on-call
- **Equipo**: 1 dev on-call
- **SLA**: Response < 4h para críticos

### Mes 2+ Post-Release
- **Cobertura**: Horario laboral
- **Soporte**: Regular support team

---

## Definición de Hecho

- [ ] Documentación de usuario completa
- [ ] Release notes publicados
- [ ] Sistema desplegado en producción
- [ ] Migración exitosa verificada
- [ ] Monitoreo activo
- [ ] No hay incidentes críticos
- [ ] Feedback inicial positivo
- [ ] Equipo de soporte capacitado
- [ ] Post-mortem programado (1 semana)

---

**Sprint Goal:** Sistema en producción con documentación completa y soporte activo.

**Velocity Estimada:** 13 puntos
