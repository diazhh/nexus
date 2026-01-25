# Guía de Arquitectura Modular de Nexus

## Visión General

Nexus es una plataforma IoT modular para operaciones petroleras construida sobre ThingsBoard. El sistema permite agregar módulos operativos de forma incremental, cada uno proporcionando funcionalidad específica para diferentes aspectos de las operaciones.

## Conceptos Fundamentales

### Gemelos Digitales
- Cada elemento físico tiene su representación digital en TB como Asset o Device
- Los gemelos digitales pueden tener relaciones dinámicas entre sí
- Cada gemelo tiene atributos (configuración) y telemetrías (datos en tiempo real)

### Módulos
- Un módulo es un paquete completo de funcionalidad para un dominio específico
- Incluye: configuración de assets, reglas, dashboards, menús, interfaces, reportes
- Los módulos se activan por tenant según sus necesidades

### Plantillas (Templates)
- Configuraciones predefinidas para crear gemelos digitales complejos
- Definen estructura de assets, relaciones, atributos y telemetrías
- Permiten crear instancias completas con un solo clic

## Arquitectura de Capas

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                      │
│  - Interfaces de Usuario Customizadas                       │
│  - Dashboards                                                │
│  - Menús Modulares                                           │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE LÓGICA DE NEGOCIO                 │
│  - Gestión de Módulos                                        │
│  - Motor de Plantillas                                       │
│  - Sistema de Mapeo de Datos                                 │
│  - Procesamiento de Reglas                                   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE THINGSBOARD CORE                  │
│  - Assets & Devices                                          │
│  - Rule Engine                                               │
│  - Alarmas                                                   │
│  - Telemetría & Atributos                                    │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PERSISTENCIA                      │
│  - Base de Datos PostgreSQL (Extensiones Nexus)             │
│  - Timeseries Database                                       │
└─────────────────────────────────────────────────────────────┘
```

## Estructura de un Módulo

Cada módulo debe seguir esta estructura estándar:

```
/dev/modules/
  ├── [module-name]/
  │   ├── README.md                    # Descripción general del módulo
  │   ├── ARCHITECTURE.md              # Arquitectura técnica
  │   ├── IMPLEMENTATION_GUIDE.md      # Guía de implementación paso a paso
  │   ├── database/
  │   │   ├── schema.sql              # Esquema de tablas específicas
  │   │   ├── migrations/             # Scripts de migración
  │   │   └── seed-data.sql           # Datos iniciales
  │   ├── templates/
  │   │   ├── asset-templates.json    # Plantillas de assets
  │   │   ├── device-templates.json   # Plantillas de devices
  │   │   └── rule-chains.json        # Cadenas de reglas
  │   ├── data-mapping/
  │   │   ├── mapping-schemas.json    # Esquemas de mapeo
  │   │   └── transformations.json    # Transformaciones de datos
  │   ├── ui/
  │   │   ├── components/             # Componentes Angular
  │   │   ├── services/               # Servicios
  │   │   ├── models/                 # Modelos TypeScript
  │   │   └── routes.ts               # Configuración de rutas
  │   ├── dashboards/
  │   │   └── *.json                  # Definiciones de dashboards
  │   ├── reports/
  │   │   ├── templates/              # Plantillas de reportes
  │   │   └── generators/             # Generadores de reportes
  │   └── api/
  │       ├── controllers/            # REST Controllers Java
  │       ├── services/               # Servicios Java
  │       └── models/                 # DTOs y Entities
```

## Componentes Principales

### 1. Sistema de Plantillas (Templates System)
Ver: [TEMPLATES_SYSTEM.md](./TEMPLATES_SYSTEM.md)

Permite definir y crear configuraciones complejas de gemelos digitales:
- Plantillas de Assets
- Plantillas de Devices
- Relaciones entre entidades
- Atributos predefinidos
- Telemetrías esperadas

### 2. Sistema de Mapeo de Datos (Data Mapping System)
Ver: [DATA_MAPPING_SYSTEM.md](./DATA_MAPPING_SYSTEM.md)

Gestiona la distribución de datos desde fuentes externas a gemelos digitales:
- Definición de fuentes de datos
- Mapeo de variables a atributos/telemetrías
- Transformaciones y validaciones
- Distribución a múltiples assets

### 3. Sistema de Gestión de Módulos (Module Management System)
Ver: [MODULE_MANAGEMENT_SYSTEM.md](./MODULE_MANAGEMENT_SYSTEM.md)

Controla la activación y configuración de módulos por tenant:
- Activación/desactivación de módulos
- Configuración de permisos
- Gestión de menús dinámicos
- Versionado de módulos

### 4. Sistema de Menús Dinámicos
Ver: [DYNAMIC_MENU_SYSTEM.md](./DYNAMIC_MENU_SYSTEM.md)

Genera menús basados en módulos activos y permisos:
- Menús por módulo
- Control de acceso por rol
- Jerarquía de menús
- Personalización por tenant

## Ejemplo: Módulo Coiled Tubing (CT)

### Componentes del Módulo CT

1. **Gestión de Reels**
   - Crear/editar/eliminar reels
   - Tracking de uso y fatiga
   - Historial de trabajos

2. **Gestión de Unidades**
   - Crear unidades desde plantillas
   - Acoplar/desacoplar reels
   - Monitoreo en tiempo real

3. **Gestión de Trabajos**
   - Planificación de trabajos
   - Asignación de recursos
   - Registro de operaciones

4. **Cálculos y Reglas**
   - Cálculo de fatiga (Rule Engine)
   - Alertas de mantenimiento
   - Optimización de operaciones

### Gemelo Digital de Unidad CT

```
Unidad CT (Asset Raíz)
├── Sistema Hidráulico (Asset)
│   ├── Atributos: presión_max, capacidad, fabricante
│   └── Telemetrías: presión_actual, temperatura, flujo
├── Sistema de Inyección (Asset)
│   ├── Atributos: velocidad_max, diámetro
│   └── Telemetrías: velocidad_actual, tensión, profundidad
├── Reel Acoplado (Asset - Relación dinámica)
│   ├── Atributos: longitud_total, diámetro_tubing, ciclos_fatiga
│   └── Telemetrías: longitud_usada, peso, temperatura
├── Sistema de Control (Asset)
│   ├── Atributos: versión_software, modelo_plc
│   └── Telemetrías: estado_conexión, alarmas, modo_operación
└── Sensores (Devices)
    ├── Sensor Presión (Device)
    ├── Sensor Tensión (Device)
    └── Sensor Profundidad (Device)
```

## Flujo de Implementación de un Módulo

### Fase 1: Análisis y Diseño
1. Definir requisitos del módulo
2. Identificar entidades del gemelo digital
3. Diseñar estructura de assets y relaciones
4. Definir atributos y telemetrías
5. Identificar reglas de negocio

### Fase 2: Base de Datos
1. Diseñar esquema de tablas específicas
2. Crear scripts de migración
3. Definir datos semilla (seed data)
4. Implementar scripts SQL

### Fase 3: Backend (Java)
1. Crear entidades JPA
2. Implementar repositorios
3. Crear servicios de negocio
4. Desarrollar REST Controllers
5. Implementar Rule Chains

### Fase 4: Sistema de Plantillas
1. Definir plantillas JSON
2. Implementar lógica de instanciación
3. Crear validaciones
4. Integrar con TB Core

### Fase 5: Sistema de Mapeo
1. Definir esquemas de mapeo
2. Implementar transformaciones
3. Crear procesadores de datos
4. Integrar con Rule Engine

### Fase 6: Frontend (Angular)
1. Crear componentes de UI
2. Implementar servicios HTTP
3. Crear formularios de gestión
4. Desarrollar dashboards
5. Integrar con sistema de menús

### Fase 7: Pruebas e Integración
1. Pruebas unitarias
2. Pruebas de integración
3. Pruebas de UI
4. Validación con usuarios
5. Documentación

### Fase 8: Despliegue
1. Preparar paquete de módulo
2. Crear scripts de instalación
3. Documentar proceso de activación
4. Capacitación

## Convenciones y Estándares

### Nomenclatura
- **Módulos**: kebab-case (ej: `coiled-tubing`)
- **Assets**: PascalCase (ej: `UnidadCT001`)
- **Atributos**: snake_case (ej: `presion_maxima`)
- **Telemetrías**: snake_case (ej: `temperatura_actual`)
- **Tablas DB**: snake_case con prefijo módulo (ej: `ct_units`, `ct_reels`)

### Prefijos
Cada módulo debe usar un prefijo único:
- Coiled Tubing: `ct_`
- Otro módulo ejemplo: `mod_`

### Permisos
Estructura de permisos por módulo:
- `MODULE_[NAME]_VIEW`: Ver datos del módulo
- `MODULE_[NAME]_EDIT`: Editar configuraciones
- `MODULE_[NAME]_ADMIN`: Administrar módulo completo
- `MODULE_[NAME]_REPORTS`: Acceso a reportes

### APIs
Estructura de endpoints:
```
/api/nexus/modules/[module-name]/[resource]
```

Ejemplo:
```
/api/nexus/modules/coiled-tubing/units
/api/nexus/modules/coiled-tubing/reels
/api/nexus/modules/coiled-tubing/jobs
```

## Integración con ThingsBoard Core

### Assets y Devices
- Usar tipos de asset customizados por módulo
- Mantener jerarquías claras
- Documentar relaciones

### Rule Engine
- Crear Rule Chains específicas por módulo
- Nombrar nodos claramente
- Documentar lógica de reglas

### Alarmas
- Usar severidades estándar de TB
- Prefijo en nombre de alarmas (ej: `CT_FATIGUE_HIGH`)
- Configurar propagación adecuadamente

### Dashboards
- Seguir guías de diseño de Nexus
- Usar widgets estándar cuando sea posible
- Documentar dashboards customizados

## Roadmap de Implementación

### Sprint 0: Infraestructura Base
- [x] Sistema de Roles y Usuarios
- [ ] Sistema de Plantillas (Templates)
- [ ] Sistema de Mapeo de Datos
- [ ] Sistema de Gestión de Módulos
- [ ] Sistema de Menús Dinámicos

### Sprint 1-N: Módulos Operativos
Cada módulo seguirá el ciclo completo de implementación.

Módulos planificados:
1. Coiled Tubing
2. [Agregar según necesidades]

## Referencias

- [Templates System](./TEMPLATES_SYSTEM.md)
- [Data Mapping System](./DATA_MAPPING_SYSTEM.md)
- [Module Management System](./MODULE_MANAGEMENT_SYSTEM.md)
- [Dynamic Menu System](./DYNAMIC_MENU_SYSTEM.md)
- [Coiled Tubing Module](./coiled-tubing/README.md)

## Próximos Pasos

1. Revisar y aprobar esta guía
2. Implementar sistemas base (Templates, Mapping, Module Management)
3. Crear módulo piloto (Coiled Tubing)
4. Documentar lecciones aprendidas
5. Escalar a otros módulos
