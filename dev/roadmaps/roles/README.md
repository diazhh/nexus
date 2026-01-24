# Sistema de Roles y Permisos - ThingsBoard CE

## Índice de Documentación

Esta carpeta contiene toda la documentación, roadmaps y especificaciones para la implementación del **Sistema Mejorado de Roles y Permisos** en ThingsBoard CE, similar al que existe en la versión PE.

### Documentos Principales

1. **[ROADMAP.md](./ROADMAP.md)** - Plan general del proyecto, fases y cronograma
2. **[TECHNICAL_SPEC.md](./TECHNICAL_SPEC.md)** - Especificación técnica completa
3. **[TESTING_PLAN.md](./TESTING_PLAN.md)** - Plan de pruebas unitarias e integración
4. **[DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md)** - Esquema de base de datos y migraciones
5. **[API_SPECIFICATION.md](./API_SPECIFICATION.md)** - Especificación de APIs REST
6. **[IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md)** - Guía paso a paso de implementación
7. **[USE_CASES.md](./USE_CASES.md)** - Casos de uso y ejemplos prácticos

### Tareas por Sprint

La carpeta `sprints/` contiene el desglose detallado de tareas para cada sprint:

- **[Sprint 01-02: Base de Datos](./sprints/sprint-01-02-database.md)**
- **[Sprint 03-04: Backend Services](./sprints/sprint-03-04-backend-services.md)**
- **[Sprint 05-06: REST APIs](./sprints/sprint-05-06-rest-apis.md)**
- **[Sprint 07-08: Frontend Services](./sprints/sprint-07-08-frontend-services.md)**
- **[Sprint 09-11: Frontend UI Roles](./sprints/sprint-09-11-frontend-ui-roles.md)**
- **[Sprint 12-13: Frontend UI Users](./sprints/sprint-12-13-frontend-ui-users.md)**
- **[Sprint 14: Migración](./sprints/sprint-14-migration.md)**
- **[Sprint 15: Testing E2E](./sprints/sprint-15-testing.md)**
- **[Sprint 16: Release](./sprints/sprint-16-release.md)**

### Referencias de Código

La carpeta `code-examples/` contiene ejemplos de código y templates:

- **Backend**: Ejemplos de entidades, DAOs, servicios y controladores
- **Frontend**: Componentes Angular, servicios y templates
- **Tests**: Ejemplos de tests unitarios e integración

### Diagramas

La carpeta `diagrams/` contiene diagramas de arquitectura y flujos:

- Diagrama de base de datos (ERD)
- Diagrama de clases
- Diagrama de flujo de autenticación/autorización
- Diagrama de componentes frontend

## Objetivo del Proyecto

Implementar un sistema completo de **roles y permisos configurables** que permita:

✅ Crear roles personalizados por tenant  
✅ Asignar permisos granulares (Resource × Operation)  
✅ Crear usuarios directamente sin customer obligatorio  
✅ Gestionar permisos desde una interfaz web intuitiva  
✅ Mantener retrocompatibilidad con el sistema actual  

## Inicio Rápido

1. Lee el [ROADMAP.md](./ROADMAP.md) para entender el alcance completo
2. Revisa la [TECHNICAL_SPEC.md](./TECHNICAL_SPEC.md) para detalles técnicos
3. Consulta el [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) para comenzar a implementar
4. Sigue las tareas del sprint actual en la carpeta `sprints/`

## Estado del Proyecto

- **Fase Actual**: Planificación y Diseño
- **Próximo Hito**: Sprint 1 - Base de Datos y Modelos
- **Estimación Total**: 16 sprints (32 semanas)

## Contacto y Soporte

Para preguntas sobre la implementación, consulta la documentación o contacta al equipo de desarrollo.
