# Documentación de APIs - Módulo Coiled Tubing

## Base URL

```
https://nexus-server/api/nexus/ct
```

## Autenticación

Todas las APIs requieren autenticación JWT:

```http
Authorization: Bearer <jwt_token>
```

## Endpoints Principales

### 1. Units (Unidades CT)

#### GET /units
Lista todas las unidades CT del tenant.

**Query Parameters**:
- `status` (opcional): Filtrar por estado operacional
- `location` (opcional): Filtrar por ubicación
- `page` (opcional, default: 0): Número de página
- `pageSize` (opcional, default: 20): Tamaño de página

**Response 200**:
```json
{
  "data": [
    {
      "id": "uuid",
      "unit_code": "CT-001",
      "unit_name": "CT Unit 001 - Standard",
      "operational_status": "OPERATIONAL",
      "current_location": "Pad A - Campo Norte",
      "manufacturer": "NOV",
      "model": "XL-500",
      "total_operational_hours": 1250.5,
      "current_reel_id": "reel-uuid",
      "next_maintenance_due_hours": 249.5
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

#### GET /units/{id}
Obtiene detalles completos de una unidad específica.

**Response 200**:
```json
{
  "id": "uuid",
  "unit_code": "CT-001",
  "unit_name": "CT Unit 001 - Standard",
  "asset_id": "tb-asset-uuid",
  "operational_status": "OPERATIONAL",
  "current_location": "Pad A",
  "latitude": 40.7128,
  "longitude": -74.0060,
  
  "specifications": {
    "manufacturer": "NOV",
    "model": "XL-500",
    "serial_number": "SN-2024-001",
    "year_manufactured": 2024,
    "max_pressure_psi": 5500,
    "max_tension_lbf": 35000,
    "max_speed_ft_min": 30
  },
  
  "systems": {
    "hydraulic_system_id": "uuid",
    "injection_system_id": "uuid",
    "control_system_id": "uuid",
    "power_pack_id": "uuid"
  },
  
  "operational_data": {
    "total_operational_hours": 1250.5,
    "total_jobs_completed": 32,
    "total_meters_deployed": 98500.0
  },
  
  "current_reel": {
    "reel_id": "uuid",
    "reel_code": "REEL-R001",
    "coupled_date": 1705881600000,
    "fatigue_percent": 82.3
  },
  
  "maintenance": {
    "last_maintenance_date": 1705363200000,
    "hours_since_maintenance": 250.5,
    "next_maintenance_due_hours": 249.5,
    "maintenance_overdue": false
  }
}
```

#### POST /units
Crea una nueva unidad CT desde una plantilla.

**Request Body**:
```json
{
  "template_id": "ct-unit-standard-v1",
  "unit_code": "CT-006",
  "unit_name": "CT Unit 006",
  "manufacturer": "NOV",
  "model": "XL-600",
  "serial_number": "SN-2026-006",
  "year_manufactured": 2026,
  "max_pressure_psi": 6000,
  "max_tension_lbf": 40000,
  "location": "Base - Workshop"
}
```

**Response 201**:
```json
{
  "id": "new-uuid",
  "unit_code": "CT-006",
  "asset_id": "tb-asset-uuid",
  "message": "Unit created successfully with 6 related assets"
}
```

#### PUT /units/{id}
Actualiza una unidad existente.

#### DELETE /units/{id}
Elimina una unidad (soft delete).

#### POST /units/{id}/assign-reel
Acopla un reel a una unidad.

**Request Body**:
```json
{
  "reel_id": "reel-uuid"
}
```

**Response 200**:
```json
{
  "message": "Reel REEL-R002 successfully assigned to unit CT-001",
  "coupled_date": 1706486400000
}
```

#### POST /units/{id}/detach-reel
Desacopla el reel actual de una unidad.

**Response 200**:
```json
{
  "message": "Reel REEL-R002 successfully detached from unit CT-001"
}
```

#### GET /units/{id}/history
Obtiene el historial de trabajos de una unidad.

**Query Parameters**:
- `from_date` (opcional): Fecha inicio (epoch ms)
- `to_date` (opcional): Fecha fin (epoch ms)
- `page`, `pageSize`

### 2. Reels (Carretes)

#### GET /reels
Lista todos los reels del tenant.

**Query Parameters**:
- `status`: 'AVAILABLE', 'IN_USE', 'INSPECTION', 'RETIRED'
- `size_od_inch`: Filtrar por tamaño
- `fatigue_min`, `fatigue_max`: Rango de fatiga
- `page`, `pageSize`

**Response 200**:
```json
{
  "data": [
    {
      "id": "uuid",
      "reel_code": "REEL-R001",
      "tubing_od_inch": 2.0,
      "total_length_ft": 5000,
      "material_grade": "QT-800",
      "status": "AVAILABLE",
      "accumulated_fatigue_percent": 82.3,
      "estimated_remaining_cycles": 850,
      "fatigue_level": "HIGH",
      "total_jobs_used": 45,
      "current_unit_code": null
    }
  ],
  "totalElements": 12
}
```

#### GET /reels/{id}
Detalles completos de un reel.

**Response 200**:
```json
{
  "id": "uuid",
  "reel_code": "REEL-R001",
  "reel_name": "Reel 001 - 2 inch QT-800",
  "asset_id": "tb-asset-uuid",
  "status": "AVAILABLE",
  
  "specifications": {
    "tubing_od_inch": 2.0,
    "tubing_id_inch": 1.75,
    "wall_thickness_inch": 0.125,
    "total_length_ft": 5000,
    "material_grade": "QT-800",
    "material_yield_strength_psi": 110000,
    "youngs_modulus_psi": 30000000
  },
  
  "fatigue": {
    "accumulated_fatigue_percent": 82.3,
    "total_cycles": 1850,
    "estimated_remaining_cycles": 850,
    "fatigue_level": "HIGH",
    "calculation_method": "PALMGREN_MINER"
  },
  
  "condition": {
    "has_corrosion": false,
    "has_mechanical_damage": false,
    "ovality_percent": 0.8,
    "wall_thickness_loss_percent": 2.1
  },
  
  "operational_data": {
    "total_jobs_used": 45,
    "total_meters_deployed": 125000,
    "total_hours_in_use": 380.5
  },
  
  "inspection": {
    "last_inspection_date": 1705363200000,
    "last_inspection_type": "MAGNETIC_PARTICLE",
    "last_inspection_result": "PASSED",
    "next_inspection_due_date": 1713139200000
  },
  
  "current_assignment": {
    "unit_id": null,
    "unit_code": null,
    "coupled_date": null
  }
}
```

#### POST /reels
Crea un nuevo reel.

#### PUT /reels/{id}
Actualiza un reel.

#### GET /reels/{id}/fatigue-history
Historial de cálculos de fatiga.

**Query Parameters**:
- `from_date`, `to_date`
- `limit` (default: 100)

**Response 200**:
```json
{
  "data": [
    {
      "calculation_time": 1706486400000,
      "job_id": "job-uuid",
      "job_number": "JOB-789",
      "cycle_number": 1851,
      "pressure_psi": 4200,
      "tension_lbf": 22000,
      "von_mises_stress_psi": 45250,
      "fatigue_increment_percent": 0.054,
      "fatigue_after_percent": 82.3
    }
  ]
}
```

#### GET /reels/{id}/lifecycle-report
Genera reporte de lifecycle del reel.

**Response 200** (PDF):
```
Binary PDF data
```

### 3. Jobs (Trabajos)

#### GET /jobs
Lista trabajos con filtros.

**Query Parameters**:
- `status`: 'PLANNED', 'IN_PROGRESS', 'COMPLETED', etc.
- `job_type`: 'CLEANOUT', 'STIMULATION', etc.
- `unit_id`: Filtrar por unidad
- `from_date`, `to_date`
- `page`, `pageSize`

**Response 200**:
```json
{
  "data": [
    {
      "id": "uuid",
      "job_number": "JOB-789",
      "job_name": "Cleanout WELL-XYZ-001",
      "job_type": "WELL_CLEANOUT",
      "status": "IN_PROGRESS",
      "well_name": "WELL-XYZ-001",
      "unit_code": "CT-003",
      "reel_code": "REEL-R002",
      "actual_start_date": 1706486400000,
      "estimated_duration_hours": 8.0,
      "elapsed_hours": 2.75,
      "current_phase": "RUNNING_IN",
      "max_actual_depth_ft": 2456.8,
      "target_depth_ft": 3200
    }
  ]
}
```

#### GET /jobs/{id}
Detalles completos del trabajo.

**Response 200**:
```json
{
  "id": "uuid",
  "job_number": "JOB-789",
  "job_type": "WELL_CLEANOUT",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  
  "well_info": {
    "well_id": "well-uuid",
    "well_name": "WELL-XYZ-001",
    "field_name": "Campo Norte",
    "client_name": "ACME Oil Corp",
    "well_depth_md_ft": 3250,
    "target_depth_from_ft": 2800,
    "target_depth_to_ft": 3200
  },
  
  "resources": {
    "unit_id": "unit-uuid",
    "unit_code": "CT-003",
    "reel_id": "reel-uuid",
    "reel_code": "REEL-R002",
    "operator_name": "Juan Pérez",
    "supervisor_name": "Carlos Mendez"
  },
  
  "planning": {
    "planned_start_date": 1706486400000,
    "estimated_duration_hours": 8.0,
    "max_planned_pressure_psi": 5000,
    "max_planned_tension_lbf": 25000
  },
  
  "execution": {
    "actual_start_date": 1706486400000,
    "current_phase": "RUNNING_IN",
    "elapsed_hours": 2.75,
    "max_actual_depth_ft": 2456.8,
    "max_actual_pressure_psi": 4200,
    "max_actual_tension_lbf": 22000
  },
  
  "telemetry_live": {
    "depth_ft": 2456.8,
    "pressure_psi": 4200,
    "tension_lbf": 22000,
    "speed_ft_min": 15.5,
    "direction": "IN"
  }
}
```

#### POST /jobs
Crea un nuevo trabajo.

**Request Body**:
```json
{
  "job_name": "Cleanout WELL-ABC",
  "job_type": "WELL_CLEANOUT",
  "priority": "HIGH",
  "well_name": "WELL-ABC-001",
  "field_name": "Campo Sur",
  "client_name": "Client XYZ",
  "well_depth_md_ft": 3500,
  "target_depth_from_ft": 3000,
  "target_depth_to_ft": 3400,
  "unit_id": "unit-uuid",
  "reel_id": "reel-uuid",
  "operator_user_id": "user-uuid",
  "planned_start_date": 1707091200000,
  "estimated_duration_hours": 10,
  "max_planned_pressure_psi": 5000,
  "description": "Clean sand fill from production tubing"
}
```

**Response 201**:
```json
{
  "id": "new-uuid",
  "job_number": "JOB-790",
  "status": "PLANNED",
  "message": "Job created successfully"
}
```

#### PUT /jobs/{id}
Actualiza un trabajo.

#### POST /jobs/{id}/start
Inicia un trabajo.

**Response 200**:
```json
{
  "job_id": "uuid",
  "status": "IN_PROGRESS",
  "actual_start_date": 1706572800000,
  "message": "Job started successfully"
}
```

#### POST /jobs/{id}/pause
Pausa un trabajo en progreso.

**Request Body**:
```json
{
  "reason": "Weather delay"
}
```

#### POST /jobs/{id}/complete
Finaliza un trabajo.

**Request Body**:
```json
{
  "job_success": true,
  "objectives_achieved": true,
  "notes": "Job completed successfully without incidents"
}
```

**Response 200**:
```json
{
  "job_id": "uuid",
  "status": "COMPLETED",
  "actual_end_date": 1706608800000,
  "actual_duration_hours": 7.5,
  "report_generated": true,
  "report_path": "/reports/JOB-789-summary.pdf"
}
```

#### POST /jobs/{id}/events
Registra un evento durante el trabajo.

**Request Body**:
```json
{
  "event_type": "ALARM",
  "event_category": "EQUIPMENT",
  "severity": "WARNING",
  "event_title": "High Hydraulic Temperature",
  "event_description": "Hydraulic temperature reached 63°C",
  "depth_at_event_ft": 2456.8,
  "action_taken": "Reduced injection speed to 12 ft/min"
}
```

#### GET /jobs/{id}/events
Lista eventos del trabajo.

#### GET /jobs/{id}/phases
Fases del trabajo.

#### GET /jobs/{id}/report
Genera reporte del trabajo (PDF).

### 4. Simulation (Simulación)

#### POST /simulation/job
Simula un trabajo antes de ejecutarlo.

**Request Body**:
```json
{
  "well": {
    "name": "WELL-TEST",
    "depth_md_ft": 3200,
    "depth_tvd_ft": 3200,
    "wellbore_diameter_inch": 6.0,
    "fluid_density_ppg": 8.6,
    "deviation_profile": [
      {"depth_ft": 0, "inclination_deg": 0},
      {"depth_ft": 3200, "inclination_deg": 0}
    ]
  },
  "job_type": "WELL_CLEANOUT",
  "equipment": {
    "unit_id": "unit-uuid",
    "reel_id": "reel-uuid",
    "tubing_od_inch": 2.0,
    "tubing_id_inch": 1.75
  },
  "operations": {
    "target_depth_ft": 3000,
    "max_running_speed_ft_min": 20,
    "pump_rate_bpm": 5
  }
}
```

**Response 200**:
```json
{
  "feasibility": {
    "is_feasible": true,
    "limiting_factors": [],
    "warnings": [
      "Hookload will reach 85% of unit capacity at target depth"
    ]
  },
  
  "predictions": {
    "max_hookload_lbf": 29750,
    "max_pressure_surface_psi": 4850,
    "estimated_duration_hours": 7.5,
    "phase_durations": [
      {"phase": "RIGGING_UP", "duration_hours": 1.0},
      {"phase": "RUNNING_IN", "duration_hours": 2.5},
      {"phase": "ON_DEPTH", "duration_hours": 2.0},
      {"phase": "PULLING_OUT", "duration_hours": 1.5},
      {"phase": "RIGGING_DOWN", "duration_hours": 0.5}
    ]
  },
  
  "forces": {
    "depth_ft": [0, 500, 1000, 1500, 2000, 2500, 3000],
    "hookload_lbf": [0, 4200, 8500, 13000, 17800, 23500, 29750]
  },
  
  "fatigue": {
    "estimated_cycles": 10,
    "estimated_fatigue_consumption_percent": 1.2,
    "new_accumulated_fatigue_percent": 83.5
  },
  
  "risks": [
    {
      "severity": "MEDIUM",
      "type": "HIGH_TENSION",
      "description": "Hookload reaches 85% of unit capacity",
      "recommendation": "Monitor tension closely or consider heavier unit"
    }
  ],
  
  "recommendations": [
    "Job is feasible with current equipment",
    "Monitor hydraulic temperature during high tension phases",
    "Consider reducing speed to 15 ft/min below 2500 ft"
  ]
}
```

### 5. Maintenance (Mantenimiento)

#### GET /maintenance
Lista registros de mantenimiento.

#### POST /maintenance
Crea registro de mantenimiento.

#### PUT /maintenance/{id}
Actualiza registro.

#### POST /maintenance/{id}/complete
Completa un mantenimiento.

### 6. Analytics (Analítica)

#### GET /analytics/fleet-utilization
Utilización de flota.

**Query Parameters**:
- `from_date`, `to_date`
- `group_by`: 'day', 'week', 'month'

**Response 200**:
```json
{
  "period": {
    "from": 1704067200000,
    "to": 1706745600000
  },
  "units": [
    {
      "unit_code": "CT-001",
      "total_hours": 180,
      "productive_hours": 165,
      "npt_hours": 15,
      "utilization_percent": 75.0,
      "jobs_completed": 8
    }
  ],
  "fleet_totals": {
    "total_units": 5,
    "avg_utilization_percent": 68.5,
    "total_jobs": 42,
    "total_productive_hours": 720
  }
}
```

#### GET /analytics/fatigue-summary
Resumen de fatiga de reels.

#### GET /analytics/job-performance
Performance de trabajos.

**Response 200**:
```json
{
  "summary": {
    "total_jobs": 127,
    "success_rate_percent": 96.8,
    "avg_duration_hours": 6.8,
    "avg_npt_hours": 2.5
  },
  "by_type": [
    {
      "job_type": "WELL_CLEANOUT",
      "count": 42,
      "success_rate": 98.0,
      "avg_duration": 6.5
    }
  ]
}
```

### 7. Reports (Reportes)

#### GET /reports/job-summary/{jobId}
Genera reporte resumen de trabajo (PDF).

#### GET /reports/reel-lifecycle/{reelId}
Reporte de lifecycle de reel (PDF).

#### GET /reports/fleet-utilization
Reporte de utilización de flota (Excel/PDF).

**Query Parameters**:
- `from_date`, `to_date`
- `format`: 'pdf' o 'excel'

## WebSocket API

### Real-Time Job Telemetry

**Endpoint**: `wss://nexus-server/api/ws/ct/jobs/{jobId}/telemetry`

**Message Format**:
```json
{
  "timestamp": 1706486400000,
  "depth_ft": 2456.8,
  "pressure_psi": 4200,
  "tension_lbf": 22000,
  "speed_ft_min": 15.5,
  "direction": "IN",
  "hydraulic_temp_f": 65,
  "pump_rate_bpm": 5.2
}
```

**Client Example**:
```typescript
const ws = new WebSocket('wss://nexus-server/api/ws/ct/jobs/job-uuid/telemetry');

ws.onmessage = (event) => {
  const telemetry = JSON.parse(event.data);
  updateDashboard(telemetry);
};
```

## Códigos de Error

### 400 Bad Request
```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid job type",
  "details": {
    "field": "job_type",
    "value": "INVALID_TYPE"
  }
}
```

### 404 Not Found
```json
{
  "error": "NOT_FOUND",
  "message": "Unit with ID 'abc-123' not found"
}
```

### 409 Conflict
```json
{
  "error": "CONFLICT",
  "message": "Reel is already assigned to another unit",
  "details": {
    "reel_id": "reel-uuid",
    "current_unit_id": "unit-uuid"
  }
}
```

### 422 Unprocessable Entity
```json
{
  "error": "UNPROCESSABLE_ENTITY",
  "message": "Cannot start job: unit is in maintenance",
  "details": {
    "unit_status": "MAINTENANCE"
  }
}
```

## Rate Limiting

- **Lectura**: 100 requests/minuto
- **Escritura**: 50 requests/minuto
- **Simulación**: 10 requests/minuto

**Headers**:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1706486460
```

## Paginación

Todas las APIs de listado soportan paginación:

**Request**:
```
GET /api/nexus/ct/units?page=0&pageSize=20&sort=unit_code,asc
```

**Response Headers**:
```
X-Total-Count: 127
X-Page-Number: 0
X-Page-Size: 20
X-Total-Pages: 7
```

## Versionado

La API usa versionado en header:

```
API-Version: 1.0
```

---

**Versión**: 1.0.0  
**Última Actualización**: Enero 2026
