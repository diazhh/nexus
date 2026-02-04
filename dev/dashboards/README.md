# ThingsBoard Dashboard Configurations

Este directorio contiene las configuraciones JSON de dashboards para los módulos PF (Production Facilities) y PO (Production Optimization).

## Dashboards Disponibles

### Production Facilities (PF)

| Dashboard | Archivo | Descripción |
|-----------|---------|-------------|
| Well Monitoring | `pf-well-monitoring-dashboard.json` | Monitoreo en tiempo real de pozos, telemetría ESP/PCP/Gas Lift/Rod Pump |
| Alarms & Events | `pf-alarms-dashboard.json` | Gestión de alarmas, histórico, análisis por severidad y tipo |

### Production Optimization (PO)

| Dashboard | Archivo | Descripción |
|-----------|---------|-------------|
| Health Score | `po-health-dashboard.json` | Dashboard de health scores, predicción de fallas, degradación de componentes |
| Recommendations | `po-recommendations-dashboard.json` | Gestión de recomendaciones de optimización, aprobación, ejecución |

## Cómo Importar

### Opción 1: Import via UI

1. Navegar a **Dashboards** en ThingsBoard
2. Click en el botón **+** (Add Dashboard)
3. Seleccionar **Import Dashboard**
4. Seleccionar el archivo JSON correspondiente
5. Click **Import**

### Opción 2: Import via REST API

```bash
# Autenticarse
TOKEN=$(curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"tenant@thingsboard.org","password":"tenant"}' \
  | jq -r '.token')

# Importar dashboard
curl -X POST "http://localhost:8080/api/dashboard" \
  -H "Content-Type: application/json" \
  -H "X-Authorization: Bearer $TOKEN" \
  -d @pf-well-monitoring-dashboard.json
```

## Requisitos Previos

Antes de importar los dashboards, asegúrate de tener configurados:

### 1. Asset Types

```
PF_WELL          - Production Wells
PF_WELLPAD       - Wellpads
PF_FLOW_STATION  - Flow Stations
PO_RECOMMENDATION - Optimization Recommendations
```

### 2. Telemetry Keys (ts_kv)

**Producción:**
- `production_bpd` - Oil production (barrels/day)
- `water_bpd` - Water production (barrels/day)
- `gas_mcfd` - Gas production (thousand cubic feet/day)
- `water_cut_percent` - Water cut percentage

**ESP:**
- `frequency_hz` - Pump frequency (Hz)
- `current_amps` - Motor current (Amps)
- `motor_temp_f` - Motor temperature (F)
- `intake_pressure_psi` - Intake pressure (PSI)
- `discharge_pressure_psi` - Discharge pressure (PSI)
- `vibration_ips` - Vibration (inches/second)

**PCP:**
- `speed_rpm` - Pump speed (RPM)
- `torque_ftlb` - Torque (ft-lb)
- `polished_rod_load_lbs` - Polished rod load (lbs)

**Gas Lift:**
- `injection_rate_mcfd` - Gas injection rate (Mcfd)
- `injection_pressure_psi` - Injection pressure (PSI)
- `casing_pressure_psi` - Casing pressure (PSI)

**Health Scores:**
- `health_score` - Overall health (0-100)
- `mechanical_score` - Mechanical health (0-100)
- `electrical_score` - Electrical health (0-100)
- `production_score` - Production health (0-100)
- `failure_probability` - Failure probability (0-1)

### 3. Attributes

**Well Attributes:**
- `name` - Well name
- `apiNumber` - API well number
- `fieldName` - Field name
- `wellpadName` - Wellpad name
- `liftSystemType` - Lift system type (ESP, PCP, GAS_LIFT, ROD_PUMP)
- `status` - Well status (PRODUCING, SHUT_IN, WORKOVER, etc.)
- `latitude` / `longitude` - Location

## Widgets Utilizados

Los dashboards utilizan widgets estándar de ThingsBoard:

- **cards**: `entities_table`, `attributes_card`, `value_card`, `simple_card`
- **charts**: `basic_timeseries`, `doughnut`, `bar`
- **analogue_gauges**: `radial_100`
- **alarm_widgets**: `alarms_table`
- **maps**: `openstreetmap`
- **input_widgets**: `command_button`

## Personalización

### Cambiar colores

Los colores están definidos usando códigos hexadecimales en cada widget. Para cambiarlos:

```json
"colorNeedle": "#1976d2"  // Cambiar a color deseado
```

### Cambiar umbrales

Los umbrales en gauges se definen en `highlights`:

```json
"highlights": [
  {"from": 0, "to": 250, "color": "#4caf50"},   // Verde - Normal
  {"from": 250, "to": 320, "color": "#ffc107"}, // Amarillo - Warning
  {"from": 320, "to": 400, "color": "#d32f2f"}  // Rojo - Critical
]
```

### Agregar más wells

Los Entity Aliases filtran por tipo de asset. Para incluir más wells:

1. Crear assets de tipo `PF_WELL` en ThingsBoard
2. Configurar telemetría y atributos según la especificación
3. Los dashboards los mostrarán automáticamente

## Estados del Dashboard

Cada dashboard tiene múltiples estados (vistas):

### PF Well Monitoring
- `default` - Overview de todos los pozos
- `well_detail` - Detalle de un pozo específico

### PF Alarms
- `default` - Alarmas activas
- `history` - Histórico de alarmas
- `alarm_detail` - Detalle de una alarma

### PO Health
- `default` - Overview de health de la flota
- `at_risk` - Wells en riesgo
- `well_health_detail` - Detalle de health de un pozo

### PO Recommendations
- `default` - Recomendaciones pendientes
- `executed` - Recomendaciones ejecutadas
- `recommendation_detail` - Detalle de una recomendación

---

**Creado**: 2026-02-04
**Versión**: 1.0
**Compatible con**: ThingsBoard 3.5+
