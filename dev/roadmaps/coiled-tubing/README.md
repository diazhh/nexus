# Módulo Coiled Tubing - Nexus IoT Platform

## Visión General

El **Módulo de Coiled Tubing** es un sistema integral de gestión, monitoreo y análisis para operaciones de tubería continua (Coiled Tubing) en la industria petrolera. Este módulo proporciona capacidades profesionales de nivel empresarial comparables con las mejores soluciones del mercado (CIRCA™, CYCLE™, JobMaster™).

## Características Principales

### 🎯 Gestión de Activos
- **Unidades CT**: Gestión completa de flota de unidades de coiled tubing
- **Reels (Carretes)**: Tracking de tubería continua con historial completo
- **Equipos Auxiliares**: Inyectores, sistemas hidráulicos, sistemas de control
- **Herramientas**: BHA (Bottom Hole Assembly), sensores downhole

### 📊 Monitoreo en Tiempo Real
- Dashboard operacional con datos en vivo
- Visualización de parámetros críticos (presión, tensión, profundidad, velocidad)
- Alertas y alarmas configurables
- Estado de equipos y sistemas
- Telemetría downhole en tiempo real

### 📈 Análisis y Cálculos
- **Cálculo de Fatiga**: Algoritmo avanzado basado en ciclos de trabajo
- **Predicción de Vida Útil**: Modelo predictivo para reels
- **Simulación de Trabajos**: Pre-planificación de operaciones
- **Análisis de Performance**: KPIs operacionales y de flota
- **Optimización**: Recomendaciones basadas en datos históricos

### 📋 Gestión de Trabajos
- Planificación y programación de trabajos
- Asignación de recursos (unidades, reels, personal)
- Registro de operaciones en tiempo real
- Reportes post-trabajo automatizados
- Historial completo de trabajos por unidad/reel/pozo

### 📑 Reportería y Analytics
- **Reportes Operacionales**: Job summary, daily reports, NPT analysis
- **Reportes de Flota**: Utilización, disponibilidad, performance
- **Reportes de Fatiga**: Lifecycle de reels, consumo de vida útil
- **Analytics Predictivo**: Tendencias, patrones, predicciones
- **Dashboards Ejecutivos**: KPIs del negocio

### 🔧 Mantenimiento
- Programación de mantenimiento preventivo
- Gestión de órdenes de trabajo
- Historial de mantenimiento por asset
- Alertas de mantenimiento basadas en uso
- Tracking de repuestos y componentes

## Capacidades Técnicas del Módulo

### Arquitectura de Gemelos Digitales

Cada **Unidad de Coiled Tubing** se modela como un gemelo digital complejo compuesto por múltiples assets relacionados:

```
CT-UNIT-001 (Asset Root)
├── Hydraulic System (Asset)
│   ├── Atributos: max_pressure, pump_type, oil_capacity
│   └── Telemetrías: pressure, temperature, flow_rate, oil_level
├── Injection System (Asset)
│   ├── Atributos: max_speed, max_tension, gripper_type
│   └── Telemetrías: speed, tension, depth, direction
├── Control System (Asset)
│   ├── Atributos: software_version, plc_model
│   └── Telemetrías: connection_status, alarms, mode
├── Power Pack (Asset)
│   ├── Atributos: engine_model, power_rating
│   └── Telemetrías: rpm, fuel_level, engine_temp
├── Gooseneck (Asset)
│   ├── Atributos: radius, min_bend_radius
│   └── Telemetrías: angle, wear_status
├── Reel System (Asset)
│   ├── Relación dinámica con Reel (acoplable/desacoplable)
│   └── Telemetrías: reel_rotation, tension_control
└── Sensors & Devices
    ├── Pressure Sensors (Devices)
    ├── Depth Measurement (Devices)
    ├── Weight Indicator (Devices)
    └── Telemetry System (Devices)
```

### Reels como Assets Dinámicos

Los **Reels** son assets independientes que pueden:
- Acoplarse a diferentes unidades CT
- Mantener su historial de uso
- Acumular fatiga independientemente
- Transferirse entre ubicaciones

### Sistema de Plantillas (Templates)

El módulo incluye plantillas predefinidas para crear instancias completas:

1. **CT Unit Templates**
   - Standard CT Unit (hasta 3.5" OD)
   - Heavy Duty CT Unit (hasta 4.5" OD)
   - Ultra Heavy Duty CT Unit (5"+ OD)

2. **Reel Templates**
   - Standard Reel (1" - 2")
   - Large Diameter Reel (2.375" - 3.5")
   - Specialty Reel (material especial, alta presión)

3. **BHA Templates**
   - Limpieza (jetting, scraping)
   - Perforación (drilling, milling)
   - Cementación (cementing tools)
   - Estimulación (fracturing, acidizing)

## Tipos de Operaciones Soportadas

### Operaciones de Intervención
- **Well Cleanout**: Limpieza de pozos
- **Nitrogen Lifting**: Levantamiento con nitrógeno
- **Acid Stimulation**: Estimulación ácida
- **Cement Squeeze**: Squeeze de cemento
- **Fishing Operations**: Operaciones de pesca
- **Milling**: Fresado de herramientas

### Operaciones de Completación
- **Through-Tubing Services**: Servicios a través de tubería
- **Straddle Packer Operations**: Operaciones con packers
- **Perforating**: Perforación
- **Bridge Plug Setting**: Instalación de bridge plugs

### Operaciones de Producción
- **Velocity String Deployment**: Instalación de velocity strings
- **Scale Removal**: Remoción de incrustaciones
- **Paraffin Treatment**: Tratamiento de parafina
- **Sand Cleanout**: Limpieza de arena

## Integraciones con ThingsBoard

### Assets & Devices
- Uso de tipos de asset customizados (`CT_UNIT`, `CT_REEL`, `CT_INJECTOR`, etc.)
- Jerarquías multi-nivel con relaciones dinámicas
- Atributos compartidos y específicos por tipo

### Rule Engine
- **Fatigue Calculation Chain**: Cálculo continuo de fatiga
- **Maintenance Alerts Chain**: Generación de alertas de mantenimiento
- **Performance Monitoring Chain**: Monitoreo de KPIs
- **Data Validation Chain**: Validación de datos SCADA
- **Alarm Propagation Chain**: Propagación de alarmas

### Alarmas Configuradas
- `CT_FATIGUE_HIGH`: Fatiga > 80%
- `CT_FATIGUE_CRITICAL`: Fatiga > 95%
- `CT_PRESSURE_EXCEEDED`: Presión sobre límite
- `CT_TENSION_EXCEEDED`: Tensión sobre límite
- `CT_MAINTENANCE_DUE`: Mantenimiento vencido
- `CT_REEL_LIFE_LOW`: Vida útil < 20%
- `CT_CONNECTION_LOST`: Pérdida de comunicación
- `CT_EMERGENCY_STOP`: Paro de emergencia

### Dashboards

#### 1. Real-Time Operations Dashboard
**Propósito**: Monitoreo en vivo durante trabajos activos

**Widgets**:
- Depth tracker con visualización gráfica
- Gauge de presión en tiempo real
- Indicador de tensión/carga
- Velocidad de inyección/retracción
- Estado de sistemas (hidráulico, control, power)
- Mapa de ubicación de unidad
- Timeline de eventos del trabajo
- Alarmas activas
- Parámetros downhole (si hay telemetría)

#### 2. Fleet Management Dashboard
**Propósito**: Visión general de toda la flota

**Widgets**:
- Mapa con ubicación de todas las unidades
- Estado operacional (trabajando, standby, mantenimiento, transit)
- Utilización de flota (%)
- Gráfico de distribución de trabajos
- Próximos mantenimientos
- Top units por horas trabajadas
- Disponibilidad promedio
- NPT (Non-Productive Time) por unidad

#### 3. Reel Lifecycle Dashboard
**Propósito**: Gestión de inventario de reels

**Widgets**:
- Lista de reels con status
- Gráfico de fatiga acumulada por reel
- Vida útil restante (%)
- Historial de trabajos por reel
- Últimas inspecciones
- Reels disponibles vs. en uso
- Proyección de reemplazo
- Costo por metro de tubería

#### 4. Job Execution Dashboard
**Propósito**: Vista detallada de trabajo en ejecución

**Widgets**:
- Job header (pozo, cliente, tipo de trabajo)
- Timeline detallado de fases
- Depth vs. Time chart
- Pump rate & pressure chart
- Tratamiento químico tracking
- Personnel on location
- Equipment status checklist
- Real-time notes/comments
- Cálculo de costo en vivo

#### 5. Analytics & Performance Dashboard
**Propósito**: Análisis histórico y tendencias

**Widgets**:
- Jobs completed (trend mensual)
- Average job duration por tipo
- Success rate de operaciones
- Fatigue consumption rate
- Maintenance cost tracking
- Revenue per unit
- Comparativa de performance entre unidades
- Predicciones de demanda

## Reportes Estándar

### Operacionales
1. **Daily Operations Report**: Resumen diario de actividades
2. **Job Summary Report**: Detalle completo post-trabajo
3. **NPT Analysis Report**: Análisis de tiempos no productivos
4. **Stuck Pipe Incident Report**: Reporte de incidentes

### Gestión de Flota
5. **Fleet Utilization Report**: Utilización mensual/trimestral
6. **Equipment Availability Report**: Disponibilidad de equipos
7. **Maintenance Summary Report**: Resumen de mantenimientos
8. **Performance Benchmarking Report**: Comparación de unidades

### Técnicos
9. **Reel Lifecycle Report**: Estado de reels
10. **Fatigue Analysis Report**: Análisis detallado de fatiga
11. **Pressure Testing Report**: Reportes de pruebas de presión
12. **Inspection Report**: Reportes de inspección

### Ejecutivos
13. **Executive Summary**: KPIs del negocio
14. **Revenue Report**: Análisis de ingresos
15. **Cost Analysis Report**: Análisis de costos operacionales
16. **Client Performance Report**: Performance por cliente

## Modelo de Datos Simplificado

### Tablas Principales
- `ct_units`: Unidades de coiled tubing
- `ct_reels`: Carretes de tubería
- `ct_jobs`: Trabajos/operaciones
- `ct_job_phases`: Fases de cada trabajo
- `ct_job_events`: Eventos durante trabajos
- `ct_fatigue_log`: Log de cálculos de fatiga
- `ct_maintenance`: Registros de mantenimiento
- `ct_inspections`: Inspecciones de equipos
- `ct_bha_configs`: Configuraciones de BHA
- `ct_personnel`: Personal asignado
- `ct_well_data`: Datos de pozos intervenidos

## Sistema de Permisos

### Roles del Módulo
- **CT_OPERATOR**: Operador de unidad
- **CT_SUPERVISOR**: Supervisor de operaciones
- **CT_ENGINEER**: Ingeniero de coiled tubing
- **CT_MANAGER**: Manager de flota
- **CT_ADMIN**: Administrador del módulo
- **CT_VIEWER**: Solo lectura

### Permisos Granulares
- `CT_VIEW_UNITS`: Ver unidades
- `CT_MANAGE_UNITS`: Gestionar unidades
- `CT_VIEW_REELS`: Ver reels
- `CT_MANAGE_REELS`: Gestionar reels
- `CT_VIEW_JOBS`: Ver trabajos
- `CT_CREATE_JOBS`: Crear trabajos
- `CT_EXECUTE_JOBS`: Ejecutar trabajos
- `CT_APPROVE_JOBS`: Aprobar trabajos
- `CT_VIEW_ANALYTICS`: Ver analytics
- `CT_GENERATE_REPORTS`: Generar reportes
- `CT_MANAGE_MAINTENANCE`: Gestionar mantenimiento
- `CT_ADMIN_MODULE`: Administrar módulo completo

## Estructura de Menús

```
📋 Coiled Tubing
├── 🏠 Dashboard
│   ├── Real-Time Operations
│   ├── Fleet Overview
│   └── Analytics
├── 🚛 Unidades
│   ├── Lista de Unidades
│   ├── Nueva Unidad (desde template)
│   ├── Configuración de Unidades
│   └── Historial por Unidad
├── 🎞️ Reels
│   ├── Inventario de Reels
│   ├── Nuevo Reel
│   ├── Fatiga y Lifecycle
│   └── Inspecciones
├── 📋 Trabajos
│   ├── Trabajos Activos
│   ├── Planificar Trabajo
│   ├── Historial de Trabajos
│   └── Job Templates
├── 🔧 Mantenimiento
│   ├── Programación
│   ├── Órdenes de Trabajo
│   ├── Historial
│   └── Repuestos
├── 📊 Analytics
│   ├── Performance
│   ├── Fatiga Analysis
│   ├── Utilización
│   └── Tendencias
├── 📑 Reportes
│   ├── Operacionales
│   ├── Flota
│   ├── Técnicos
│   └── Ejecutivos
└── ⚙️ Configuración
    ├── Plantillas
    ├── Alarmas
    ├── Parámetros
    └── Integraciones
```

## Flujo de Trabajo Típico

### Creación de Nueva Unidad CT
1. Seleccionar plantilla de unidad (Standard/Heavy/Ultra)
2. Especificar datos de la unidad (serial, fabricante, año)
3. Sistema crea automáticamente todos los assets relacionados
4. Configurar atributos específicos
5. Asignar ubicación inicial
6. Activar en el sistema

### Planificación de Trabajo
1. Crear nuevo trabajo
2. Seleccionar pozo objetivo
3. Asignar unidad CT disponible
4. Seleccionar/acoplar reel adecuado
5. Configurar BHA según tipo de trabajo
6. Definir parámetros operacionales
7. Asignar personal
8. Aprobar trabajo

### Ejecución de Trabajo
1. Iniciar job en dashboard operacional
2. Monitoreo en tiempo real de parámetros
3. Registro automático de eventos
4. Cálculo continuo de fatiga
5. Alertas automáticas ante anomalías
6. Captura de depth vs. time
7. Registro de tratamientos/químicos
8. Finalización y cierre de trabajo

### Post-Trabajo
1. Generación automática de Job Summary Report
2. Actualización de fatiga en reel
3. Actualización de horas en unidad
4. Evaluación de necesidad de mantenimiento
5. Desacople de reel (si corresponde)
6. Cambio de status de unidad
7. Análisis de performance

## Tecnologías y Estándares

### Backend
- **Lenguaje**: Java 17+
- **Framework**: Spring Boot 3.x
- **ORM**: JPA/Hibernate
- **Database**: PostgreSQL 14+
- **Timeseries**: ThingsBoard Timeseries DB

### Frontend
- **Framework**: Angular 18+
- **UI Components**: Angular Material, PrimeNG
- **Charts**: Chart.js, Plotly.js, D3.js
- **Maps**: Leaflet, OpenLayers
- **Real-time**: WebSocket, STOMP

### Algoritmos
- **Fatiga**: Modelo de acumulación de daño (Palmgren-Miner)
- **Simulación**: Análisis de fuerzas y torque
- **Predicción**: Machine Learning (opcional, futuro)

## Documentación Relacionada

- [Arquitectura Técnica](./ARCHITECTURE.md)
- [Diseño de UI/UX](./UI_UX_DESIGN.md)
- [Esquema de Base de Datos](./database/SCHEMA.md)
- [API Documentation](./api/API_DOCUMENTATION.md)
- [Cálculo de Fatiga](./analytics/FATIGUE_CALCULATION.md)
- [Guía de Implementación](./IMPLEMENTATION_GUIDE.md)
- [Simulación de Trabajos](./analytics/JOB_SIMULATION.md)
- [Integración SCADA](./DATA_INTEGRATION.md)

## Próximos Pasos

1. ✅ Revisión de documentación
2. ⏳ Diseño detallado de base de datos
3. ⏳ Desarrollo de backend (APIs, servicios)
4. ⏳ Implementación de Rule Chains
5. ⏳ Desarrollo de componentes frontend
6. ⏳ Creación de dashboards
7. ⏳ Implementación de reportes
8. ⏳ Pruebas de integración
9. ⏳ Capacitación de usuarios
10. ⏳ Despliegue en producción

## Contacto y Soporte

Para consultas sobre este módulo:
- **Equipo de Desarrollo**: Nexus Development Team
- **Documentación**: `/dev/roadmaps/coiled-tubing/`
- **Issue Tracking**: Sistema de tickets interno

---

**Versión**: 1.0.0  
**Última Actualización**: Enero 2026  
**Estado**: En Diseño
