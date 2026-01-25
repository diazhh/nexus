# Módulo Coiled Tubing - Caso de Estudio Completo

## Visión General

El módulo de Coiled Tubing es el primer módulo operativo de Nexus y sirve como referencia para implementar módulos adicionales. Este módulo gestiona operaciones de Coiled Tubing en campos petroleros mediante gemelos digitales de unidades, reels y trabajos.

## Objetivos del Módulo

1. Gestionar inventario de unidades CT y reels
2. Monitorear operaciones en tiempo real
3. Calcular fatiga de tubería
4. Planificar y registrar trabajos
5. Generar reportes operacionales
6. Gestionar alarmas y mantenimiento

## Conceptos del Dominio

### Unidad de Coiled Tubing (CT Unit)
Equipo móvil que despliega tubería continua en pozos petroleros. Incluye:
- Sistema hidráulico
- Sistema de inyección
- Sistema de control
- Sensores integrados

### Reel (Carrete)
Carrete que contiene la tubería continua. Características:
- Longitud total de tubería
- Diámetro de tubería
- Material y grado
- Ciclos de fatiga acumulados
- Puede acoplarse/desacoplarse de unidades

### Trabajo (Job)
Operación realizada con una unidad CT en un pozo específico:
- Tipo de operación
- Pozo objetivo
- Recursos asignados
- Parámetros operacionales
- Registros de ejecución

## Arquitectura del Módulo

### Estructura de Directorios

```
/dev/modules/coiled-tubing/
├── README.md                           # Este archivo
├── ARCHITECTURE.md                     # Arquitectura detallada
├── IMPLEMENTATION_GUIDE.md             # Guía paso a paso
├── database/
│   ├── schema.sql                      # Esquema completo
│   ├── migrations/
│   │   ├── V1__initial_schema.sql
│   │   └── V2__add_fatigue_tracking.sql
│   └── seed-data.sql                   # Datos de prueba
├── templates/
│   ├── ct-unit-standard.json           # Plantilla unidad estándar
│   ├── ct-unit-heavy-duty.json         # Plantilla unidad heavy-duty
│   ├── reel-standard.json              # Plantilla reel estándar
│   └── rule-chains/
│       ├── fatigue-calculation.json
│       └── maintenance-alerts.json
├── data-mapping/
│   ├── scada-mapping-example.json
│   └── opc-ua-mapping-example.json
├── backend/
│   ├── src/main/java/
│   │   └── org/thingsboard/nexus/modules/ct/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── model/
│   │       └── dto/
│   └── pom.xml
├── frontend/
│   ├── components/
│   │   ├── units/
│   │   ├── reels/
│   │   ├── jobs/
│   │   └── shared/
│   ├── services/
│   ├── models/
│   └── ct-routing.module.ts
├── dashboards/
│   ├── unit-monitoring.json
│   ├── fleet-overview.json
│   └── job-execution.json
└── reports/
    ├── templates/
    └── generators/
```

## Gemelo Digital: Unidad CT

### Estructura Jerárquica

```
CT-UNIT-001 (Asset Root)
├── Tipo: CT_UNIT
├── Atributos:
│   ├── manufacturer: "NOV"
│   ├── model: "XL-500"
│   ├── serial_number: "SN-2024-001"
│   ├── year: 2024
│   ├── max_pressure: 5500 PSI
│   ├── location: "Pad A - Campo X"
│   └── status: "OPERATIONAL"
├── Telemetrías:
│   ├── operational_hours: 1250.5
│   ├── availability: 98.5%
│   └── last_maintenance: timestamp
│
├── HYDRAULIC-SYSTEM (Asset Child)
│   ├── Tipo: CT_HYDRAULIC_SYSTEM
│   ├── Atributos:
│   │   ├── pump_type: "Triple Pump"
│   │   ├── max_pressure: 5500 PSI
│   │   ├── capacity_liters: 600
│   │   └── oil_type: "ISO VG 46"
│   ├── Telemetrías:
│   │   ├── pressure: 4200 PSI
│   │   ├── temperature: 65°C
│   │   ├── flow_rate: 120 L/min
│   │   └── oil_level: 85%
│   └── Relación: "Contains"
│
├── INJECTION-SYSTEM (Asset Child)
│   ├── Tipo: CT_INJECTION_SYSTEM
│   ├── Atributos:
│   │   ├── max_speed: 30 m/min
│   │   ├── max_tension: 35000 kN
│   │   └── gripper_type: "Dual Chain"
│   ├── Telemetrías:
│   │   ├── speed: 15.5 m/min
│   │   ├── tension: 22000 kN
│   │   ├── depth: 2456.8 m
│   │   ├── direction: "IN" | "OUT" | "STOPPED"
│   │   └── gripper_pressure: 180 bar
│   └── Relación: "Contains"
│
├── CONTROL-SYSTEM (Asset Child)
│   ├── Tipo: CT_CONTROL_SYSTEM
│   ├── Atributos:
│   │   ├── plc_model: "Siemens S7-1500"
│   │   ├── software_version: "v2.3.1"
│   │   └── hmi_type: "Touch Screen 15inch"
│   ├── Telemetrías:
│   │   ├── connection_status: "CONNECTED"
│   │   ├── active_alarms: 0
│   │   ├── operation_mode: "AUTO" | "MANUAL"
│   │   └── cpu_usage: 45%
│   └── Relación: "Contains"
│
├── REEL-ATTACHED (Asset - Relación Dinámica)
│   ├── Tipo: CT_REEL
│   ├── Atributos:
│   │   ├── reel_id: "REEL-042"
│   │   ├── total_length: 5000 m
│   │   ├── tubing_diameter: 2.375 inch
│   │   ├── tubing_grade: "80ksi"
│   │   ├── manufacture_date: "2023-06-15"
│   │   └── fatigue_cycles: 450
│   ├── Telemetrías:
│   │   ├── length_used: 2456.8 m
│   │   ├── length_remaining: 2543.2 m
│   │   ├── weight: 12500 kg
│   │   ├── fatigue_percentage: 35.2%
│   │   └── estimated_remaining_life: 780 hours
│   └── Relación: "Uses" (puede cambiar dinámicamente)
│
└── SENSORS (Devices)
    ├── PRESSURE-SENSOR-001 (Device)
    │   ├── Profile: "Analog Sensor"
    │   └── Telemetría: pressure_raw
    │
    ├── TENSION-SENSOR-001 (Device)
    │   ├── Profile: "Load Cell"
    │   └── Telemetría: tension_raw
    │
    └── DEPTH-SENSOR-001 (Device)
        ├── Profile: "Encoder"
        └── Telemetría: depth_raw
```

## Modelo de Datos

### Tablas Específicas del Módulo

#### ct_units
```sql
CREATE TABLE ct_units (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    asset_id UUID NOT NULL UNIQUE, -- Referencia al Asset raíz en TB
    unit_code VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    year INTEGER,
    max_pressure DOUBLE PRECISION,
    max_injection_speed DOUBLE PRECISION,
    status VARCHAR(20) DEFAULT 'AVAILABLE', -- AVAILABLE, IN_USE, MAINTENANCE, OUT_OF_SERVICE
    current_location VARCHAR(255),
    current_job_id UUID,
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    CONSTRAINT fk_ct_units_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_units_asset FOREIGN KEY (asset_id) REFERENCES asset(id) ON DELETE CASCADE,
    CONSTRAINT uk_ct_unit_code UNIQUE (tenant_id, unit_code)
);

CREATE INDEX idx_ct_units_tenant ON ct_units(tenant_id);
CREATE INDEX idx_ct_units_status ON ct_units(status);
CREATE INDEX idx_ct_units_job ON ct_units(current_job_id);
```

#### ct_reels
```sql
CREATE TABLE ct_reels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    asset_id UUID NOT NULL UNIQUE,
    reel_code VARCHAR(50) NOT NULL,
    total_length DOUBLE PRECISION NOT NULL,
    tubing_diameter DOUBLE PRECISION NOT NULL,
    tubing_grade VARCHAR(50),
    material VARCHAR(100),
    manufacture_date DATE,
    fatigue_cycles INTEGER DEFAULT 0,
    max_fatigue_cycles INTEGER DEFAULT 1000,
    status VARCHAR(20) DEFAULT 'AVAILABLE', -- AVAILABLE, ATTACHED, MAINTENANCE, RETIRED
    current_unit_id UUID,
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    CONSTRAINT fk_ct_reels_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_reels_asset FOREIGN KEY (asset_id) REFERENCES asset(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_reels_unit FOREIGN KEY (current_unit_id) REFERENCES ct_units(id) ON DELETE SET NULL,
    CONSTRAINT uk_ct_reel_code UNIQUE (tenant_id, reel_code)
);

CREATE INDEX idx_ct_reels_tenant ON ct_reels(tenant_id);
CREATE INDEX idx_ct_reels_status ON ct_reels(status);
CREATE INDEX idx_ct_reels_unit ON ct_reels(current_unit_id);
```

#### ct_jobs
```sql
CREATE TABLE ct_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    job_number VARCHAR(50) NOT NULL,
    job_type VARCHAR(50) NOT NULL, -- CLEANOUT, FISHING, PERFORATION, etc.
    unit_id UUID NOT NULL,
    reel_id UUID NOT NULL,
    well_name VARCHAR(255),
    well_location VARCHAR(255),
    customer VARCHAR(255),
    planned_start_date BIGINT,
    actual_start_date BIGINT,
    planned_end_date BIGINT,
    actual_end_date BIGINT,
    status VARCHAR(20) DEFAULT 'PLANNED', -- PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
    max_depth_planned DOUBLE PRECISION,
    max_depth_achieved DOUBLE PRECISION,
    total_cycles INTEGER DEFAULT 0,
    notes TEXT,
    created_by UUID,
    created_time BIGINT NOT NULL,
    updated_time BIGINT,
    CONSTRAINT fk_ct_jobs_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_jobs_unit FOREIGN KEY (unit_id) REFERENCES ct_units(id),
    CONSTRAINT fk_ct_jobs_reel FOREIGN KEY (reel_id) REFERENCES ct_reels(id),
    CONSTRAINT uk_ct_job_number UNIQUE (tenant_id, job_number)
);

CREATE INDEX idx_ct_jobs_tenant ON ct_jobs(tenant_id);
CREATE INDEX idx_ct_jobs_unit ON ct_jobs(unit_id);
CREATE INDEX idx_ct_jobs_status ON ct_jobs(status);
CREATE INDEX idx_ct_jobs_dates ON ct_jobs(actual_start_date, actual_end_date);
```

#### ct_job_events
```sql
CREATE TABLE ct_job_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID NOT NULL,
    event_time BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- START, STOP, INCIDENT, MILESTONE
    event_description TEXT,
    depth_at_event DOUBLE PRECISION,
    metadata JSONB,
    recorded_by UUID,
    CONSTRAINT fk_ct_job_events_job FOREIGN KEY (job_id) REFERENCES ct_jobs(id) ON DELETE CASCADE
);

CREATE INDEX idx_ct_job_events_job ON ct_job_events(job_id);
CREATE INDEX idx_ct_job_events_time ON ct_job_events(event_time);
```

#### ct_fatigue_records
```sql
CREATE TABLE ct_fatigue_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reel_id UUID NOT NULL,
    job_id UUID,
    record_time BIGINT NOT NULL,
    cycles_added INTEGER NOT NULL,
    max_tension_recorded DOUBLE PRECISION,
    avg_tension DOUBLE PRECISION,
    total_cycles_after INTEGER NOT NULL,
    fatigue_percentage DOUBLE PRECISION,
    remaining_life_hours DOUBLE PRECISION,
    metadata JSONB,
    CONSTRAINT fk_ct_fatigue_reel FOREIGN KEY (reel_id) REFERENCES ct_reels(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_fatigue_job FOREIGN KEY (job_id) REFERENCES ct_jobs(id) ON DELETE SET NULL
);

CREATE INDEX idx_ct_fatigue_reel ON ct_fatigue_records(reel_id);
CREATE INDEX idx_ct_fatigue_time ON ct_fatigue_records(record_time);
```

## Funcionalidades Principales

### 1. Gestión de Unidades

**Crear Unidad desde Plantilla**
- Usuario selecciona plantilla de unidad (Standard, Heavy-Duty)
- Completa formulario con datos específicos
- Sistema crea gemelo digital completo
- Registra unidad en tabla `ct_units`

**Operaciones:**
- Listar unidades con filtros
- Ver detalles y monitoreo en tiempo real
- Editar información
- Cambiar estado (mantenimiento, fuera de servicio)
- Asignar a trabajos

### 2. Gestión de Reels

**Crear Reel**
- Registrar nuevo reel con especificaciones
- Crear asset en TB
- Inicializar tracking de fatiga

**Operaciones:**
- Listar reels disponibles
- Acoplar/desacoplar de unidades
- Seguimiento de fatiga
- Historial de uso
- Programar retiro

### 3. Gestión de Trabajos

**Planificar Trabajo**
- Crear job con información del cliente y pozo
- Asignar unidad y reel
- Establecer fechas y parámetros

**Ejecutar Trabajo**
- Iniciar job (cambiar estado a IN_PROGRESS)
- Monitorear en tiempo real
- Registrar eventos
- Calcular fatiga en tiempo real

**Finalizar Trabajo**
- Registrar profundidad alcanzada
- Actualizar ciclos de fatiga
- Generar reporte automático
- Liberar recursos

### 4. Cálculo de Fatiga

**Algoritmo de Fatiga (Rule Engine)**

```javascript
// Nodo de Rule Chain: Calculate Fatigue
var tension = parseFloat(msg.tension);
var speed = parseFloat(msg.speed);
var depth = parseFloat(msg.depth);

// Obtener información del reel desde atributos
var maxTension = parseFloat(metadata.reel_max_tension || 35000);
var currentCycles = parseInt(metadata.reel_current_cycles || 0);
var maxCycles = parseInt(metadata.reel_max_cycles || 1000);

// Calcular ratio de tensión
var tensionRatio = tension / maxTension;

// Incrementar ciclos si tensión > 70% del máximo
if (tensionRatio > 0.7) {
    currentCycles += 1;
    
    // Calcular porcentaje de fatiga
    var fatiguePercentage = (currentCycles / maxCycles) * 100;
    
    // Estimar vida restante (horas)
    var remainingCycles = maxCycles - currentCycles;
    var avgCyclesPerHour = 10; // Ajustable
    var remainingLifeHours = remainingCycles / avgCyclesPerHour;
    
    // Actualizar atributos del reel
    var updates = {
        reel_current_cycles: currentCycles,
        reel_fatigue_percentage: fatiguePercentage,
        reel_remaining_life_hours: remainingLifeHours
    };
    
    // Guardar telemetría
    msg.fatigue_percentage = fatiguePercentage;
    msg.remaining_life_hours = remainingLifeHours;
    msg.current_cycles = currentCycles;
    
    // Generar alarma si fatiga > 80%
    if (fatiguePercentage > 80) {
        metadata.alarmType = "HIGH_FATIGUE";
        metadata.alarmSeverity = "WARNING";
    }
    
    if (fatiguePercentage > 95) {
        metadata.alarmType = "CRITICAL_FATIGUE";
        metadata.alarmSeverity = "CRITICAL";
    }
}

return {msg: msg, metadata: metadata, msgType: msgType};
```

### 5. Sistema de Alarmas

**Alarmas Configuradas:**
- `CT_HIGH_PRESSURE`: Presión hidráulica > 95% max
- `CT_HIGH_TEMPERATURE`: Temperatura > 80°C
- `CT_OVERSPEED`: Velocidad > max permitida
- `CT_HIGH_TENSION`: Tensión > 90% max
- `CT_HIGH_FATIGUE`: Fatiga > 80%
- `CT_CRITICAL_FATIGUE`: Fatiga > 95%
- `CT_CONNECTION_LOST`: Pérdida de comunicación
- `CT_MAINTENANCE_DUE`: Mantenimiento programado

### 6. Dashboards

#### Dashboard: Unit Monitoring
- Estado actual de la unidad
- Telemetrías en tiempo real (gauges, charts)
- Alarmas activas
- Información del trabajo actual
- Datos del reel acoplado
- Historial de operación (24h)

#### Dashboard: Fleet Overview
- Mapa con ubicación de unidades
- Tabla resumen de todas las unidades
- Indicadores de disponibilidad
- Trabajos activos
- Próximos mantenimientos

#### Dashboard: Job Execution
- Información del trabajo
- Gráficas de profundidad vs tiempo
- Parámetros operacionales
- Eventos registrados
- Cálculo de fatiga en tiempo real

### 7. Reportes

#### Reporte: Job Summary
- Información general del trabajo
- Equipo utilizado
- Tiempo total de operación
- Profundidad alcanzada
- Ciclos de fatiga agregados
- Eventos significativos
- Eficiencia operacional

#### Reporte: Reel Lifecycle
- Historial completo del reel
- Todos los trabajos realizados
- Evolución de fatiga
- Proyección de vida útil
- Recomendaciones de retiro

#### Reporte: Fleet Utilization
- Utilización de unidades (%)
- Horas operacionales vs disponibles
- Trabajos completados
- Revenue por unidad
- Análisis de downtime

## Permisos del Módulo

```
CT_MODULE_ACCESS          # Acceso básico al módulo
CT_UNITS_VIEW             # Ver unidades
CT_UNITS_CREATE           # Crear unidades
CT_UNITS_EDIT             # Editar unidades
CT_UNITS_DELETE           # Eliminar unidades
CT_REELS_VIEW             # Ver reels
CT_REELS_CREATE           # Crear reels
CT_REELS_EDIT             # Editar reels
CT_REELS_DELETE           # Eliminar reels
CT_REELS_ATTACH           # Acoplar/desacoplar reels
CT_JOBS_VIEW              # Ver trabajos
CT_JOBS_CREATE            # Crear trabajos
CT_JOBS_MANAGE            # Gestionar trabajos (iniciar, finalizar)
CT_JOBS_DELETE            # Eliminar trabajos
CT_REPORTS_VIEW           # Ver reportes
CT_REPORTS_GENERATE       # Generar reportes
CT_CONFIGURATION_MANAGE   # Gestionar configuración del módulo
```

## Menús del Módulo

```
Coiled Tubing (Icono: settings_input_antenna)
├── Dashboard (route: /modules/ct/dashboard) [CT_MODULE_ACCESS]
├── Unidades (route: /modules/ct/units) [CT_UNITS_VIEW]
│   ├── Lista de Unidades
│   ├── Crear Unidad [CT_UNITS_CREATE]
│   └── Monitoreo en Vivo
├── Reels (route: /modules/ct/reels) [CT_REELS_VIEW]
│   ├── Lista de Reels
│   ├── Crear Reel [CT_REELS_CREATE]
│   └── Tracking de Fatiga
├── Trabajos (route: /modules/ct/jobs) [CT_JOBS_VIEW]
│   ├── Lista de Trabajos
│   ├── Planificar Trabajo [CT_JOBS_CREATE]
│   ├── Trabajos Activos
│   └── Historial
├── Reportes (route: /modules/ct/reports) [CT_REPORTS_VIEW]
│   ├── Reportes de Trabajos
│   ├── Reportes de Flota
│   └── Análisis de Fatiga
└── Configuración (route: /modules/ct/config) [CT_CONFIGURATION_MANAGE]
    ├── Plantillas
    ├── Parámetros de Fatiga
    └── Integraciones
```

## Guía de Implementación

Ver documentos detallados:
- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - Arquitectura técnica completa
- **[IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md)** - Guía paso a paso
- **[TESTING_GUIDE.md](./TESTING_GUIDE.md)** - Estrategia de pruebas

## Próximos Pasos

1. Revisar y aprobar diseño del módulo
2. Implementar esquema de base de datos
3. Desarrollar backend (Spring Boot)
4. Crear plantillas de gemelos digitales
5. Desarrollar frontend (Angular)
6. Implementar Rule Chains
7. Crear dashboards
8. Desarrollar generadores de reportes
9. Pruebas de integración
10. Documentación de usuario
11. Capacitación
12. Deploy a producción

## Lecciones Aprendidas

Este módulo servirá como referencia para módulos futuros. Documentar:
- Patrones de diseño exitosos
- Problemas encontrados y soluciones
- Optimizaciones aplicadas
- Mejores prácticas
- Feedback de usuarios

## Soporte y Mantenimiento

- **Versión actual:** 1.0.0
- **Equipo responsable:** Nexus Development Team
- **Documentación:** `/dev/modules/coiled-tubing/`
- **Issue tracker:** [Link al sistema de tickets]
- **Changelog:** [CHANGELOG.md](./CHANGELOG.md)
