# Diseño UI/UX - Módulo Coiled Tubing

## Principios de Diseño

### 1. Claridad Operacional
- Información crítica siempre visible
- Jerarquía visual clara
- Estados del sistema evidentes
- Acciones primarias destacadas

### 2. Eficiencia
- Flujos de trabajo optimizados
- Accesos rápidos a funciones comunes
- Shortcuts de teclado
- Reducción de clics necesarios

### 3. Consistencia
- Seguir Material Design guidelines
- Paleta de colores corporativa
- Iconografía uniforme
- Patrones de interacción predecibles

### 4. Responsividad
- Diseño mobile-first para dashboards móviles
- Adaptación a diferentes resoluciones
- Touch-friendly para tablets en campo
- Desktop-optimized para centro de control

## Paleta de Colores

### Colores Principales
```scss
$primary-color: #1976D2;      // Azul ThingsBoard
$secondary-color: #424242;    // Gris oscuro
$accent-color: #FF9800;       // Naranja (alertas)
$success-color: #4CAF50;      // Verde (OK)
$warning-color: #FFC107;      // Amarillo (advertencia)
$danger-color: #F44336;       // Rojo (crítico)
$info-color: #2196F3;         // Azul claro (info)
```

### Colores de Estado Operacional
```scss
$status-operational: #4CAF50;  // Verde - Operando
$status-standby: #2196F3;      // Azul - Standby
$status-maintenance: #FF9800;  // Naranja - Mantenimiento
$status-offline: #9E9E9E;      // Gris - Offline
$status-alarm: #F44336;        // Rojo - Alarma activa
```

### Colores de Fatiga
```scss
$fatigue-excellent: #4CAF50;   // 0-40% fatiga
$fatigue-good: #8BC34A;        // 40-60% fatiga
$fatigue-moderate: #FFC107;    // 60-80% fatiga
$fatigue-high: #FF9800;        // 80-95% fatiga
$fatigue-critical: #F44336;    // 95-100% fatiga
```

## Componentes UI Principales

### 1. Dashboard de Operaciones en Tiempo Real

#### Layout Principal
```
┌─────────────────────────────────────────────────────────────┐
│  Header: CT-UNIT-001 | WELL-XYZ | JOB-456 | OPERATIONAL    │
│  Inicio: 08:30 | Duración: 02:45 | Operador: Juan Pérez     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐  ┌────────────────────────────────┐  │
│  │  DEPTH TRACKER  │  │    REAL-TIME PARAMETERS        │  │
│  │                 │  │                                 │  │
│  │   [Gauge 3D]    │  │  Presión:  ████████░  4200 PSI │  │
│  │                 │  │  Tensión:  ██████░░░  22000 lbf│  │
│  │  Depth: 2456 m  │  │  Velocidad:████░░░░░  15.5 m/m │  │
│  │  Target: 3200 m │  │  Tasa Bomb:███████░░  120 l/m  │  │
│  │                 │  │                                 │  │
│  │  ▼ Running In   │  │  Temp Hyd:  ████░░░░  65°C     │  │
│  │                 │  │  Profundidad: 2456.8 m         │  │
│  └─────────────────┘  └────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │           DEPTH vs TIME CHART                       │  │
│  │  3500m ┐                                            │  │
│  │        │              ╱─────                        │  │
│  │  2500m ┤         ╱────                              │  │
│  │        │    ╱────                                   │  │
│  │  1500m ┤────                                        │  │
│  │        │                                            │  │
│  │     0m └────────────────────────────────────────→  │  │
│  │         08:30  09:30  10:30  11:30  12:30   Time   │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐      │
│  │ HYDRAULIC    │ │  INJECTION   │ │  CONTROL     │      │
│  │  System      │ │   System     │ │   System     │      │
│  │              │ │              │ │              │      │
│  │ ✓ Pressure OK│ │ ✓ Speed OK   │ │ ✓ Connected  │      │
│  │ ✓ Temp OK    │ │ ✓ Tension OK │ │ ⚠ 2 Warnings │      │
│  │ ✓ Flow OK    │ │ ✓ Depth OK   │ │ ✓ Mode: Auto │      │
│  └──────────────┘ └──────────────┘ └──────────────┘      │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  ACTIVE ALARMS                                      │  │
│  │  ⚠ 11:15 - Hydraulic temp approaching limit (63°C) │  │
│  │  ⚠ 10:45 - Reel fatigue > 80% - Monitor closely    │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  [PAUSE JOB] [EMERGENCY STOP] [ADD EVENT] [GENERATE RPT]  │
└─────────────────────────────────────────────────────────────┘
```

#### Componentes Específicos

**A. Depth Tracker (Gauge 3D)**
```typescript
interface DepthTrackerConfig {
  currentDepth: number;
  targetDepth: number;
  wellDepth: number;
  direction: 'IN' | 'OUT' | 'STOPPED';
  showTubing: boolean;
  showBHA: boolean;
  showFormations?: boolean;
}
```

Visualización:
- Gauge circular 3D animado
- Indicador de dirección (flechas)
- Porcentaje de profundidad alcanzada
- Tiempo estimado a profundidad objetivo
- Representación visual de la tubería en el pozo

**B. Real-Time Parameters Panel**
- Barras de progreso con límites configurables
- Código de colores según rangos
- Valores numéricos precisos
- Tendencia (↑↓→)
- Sparklines para visualización de tendencia

**C. Depth vs Time Chart**
- Chart.js interactivo
- Zoom y pan
- Tooltips informativos
- Marcadores de eventos
- Fases del trabajo en diferentes colores
- Exportable a imagen

**D. System Status Cards**
```html
<mat-card class="system-status-card" [class.status-ok]="hydraulicOk" 
          [class.status-warning]="hydraulicWarning">
  <mat-card-header>
    <mat-card-title>
      <mat-icon>settings_input_component</mat-icon>
      Hydraulic System
    </mat-card-title>
  </mat-card-header>
  <mat-card-content>
    <div class="status-item">
      <mat-icon class="status-ok">check_circle</mat-icon>
      <span>Pressure OK</span>
      <span class="value">4200 PSI</span>
    </div>
    <div class="status-item">
      <mat-icon class="status-warning">warning</mat-icon>
      <span>Temp Approaching Limit</span>
      <span class="value">65°C</span>
    </div>
  </mat-card-content>
</mat-card>
```

### 2. Fleet Management Dashboard

```
┌─────────────────────────────────────────────────────────────┐
│  FLEET OVERVIEW                          [Filters ▼] [⚙]   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │           FLEET MAP VIEW                            │  │
│  │                                                     │  │
│  │    🗺️  [Interactive Map]                           │  │
│  │                                                     │  │
│  │    📍 CT-001 (Working) - Campo Norte               │  │
│  │    📍 CT-002 (Standby) - Base                      │  │
│  │    📍 CT-003 (Maintenance) - Workshop              │  │
│  │    📍 CT-004 (Working) - Pad A                     │  │
│  │    📍 CT-005 (Transit) - En ruta a Campo Sur       │  │
│  │                                                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ WORKING  │  │ STANDBY  │  │ MAINT.   │  │ OFFLINE  │  │
│  │    2     │  │    1     │  │    1     │  │    1     │  │
│  │   40%    │  │   20%    │  │   20%    │  │   20%    │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  FLEET UTILIZATION (Last 30 Days)                   │  │
│  │  100%┐                                              │  │
│  │      │ ████████████████████████████████████████    │  │
│  │   75%┤ ████████████████████████████████████        │  │
│  │      │ ████████████████████████████                │  │
│  │   50%┤ ████████████████████                        │  │
│  │      │                                              │  │
│  │    0%└───────────────────────────────────────────→ │  │
│  │       Week1  Week2  Week3  Week4                    │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐│
│  │ UNIT     STATUS      LOCATION    HOURS   NEXT MAINT.  ││
│  ├───────────────────────────────────────────────────────┤│
│  │ CT-001  🟢 Working   Pad A       1,250    50 hrs      ││
│  │ CT-002  🔵 Standby   Base          985   115 hrs      ││
│  │ CT-003  🟠 Maint.    Workshop    1,420    In Progress ││
│  │ CT-004  🟢 Working   Campo Norte 1,105   195 hrs      ││
│  │ CT-005  ⚪ Offline   Base        1,680   Overdue      ││
│  └───────────────────────────────────────────────────────┘│
│                                                             │
│  [VIEW DETAILS] [SCHEDULE JOB] [MAINTENANCE PLANNER]       │
└─────────────────────────────────────────────────────────────┘
```

### 3. Gestión de Reels - Vista de Lista

```
┌─────────────────────────────────────────────────────────────┐
│  REEL INVENTORY                    🔍 Search  [+NEW REEL]  │
├─────────────────────────────────────────────────────────────┤
│  Filters: [All Sizes ▼] [All Status ▼] [Sort: Fatiga ▼]   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ REEL-R001                           STATUS: Available │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │ Size: 2" OD  |  Length: 5,000 m  |  Material: QT-800 │ │
│  │                                                       │ │
│  │ Fatigue: ████████░░  82%  ⚠ HIGH                     │ │
│  │ Life Remaining: ~900 cycles                           │ │
│  │                                                       │ │
│  │ Total Jobs: 45  |  Total Meters: 125,000 m           │ │
│  │ Last Used: 2026-01-20 | Last Inspection: 2026-01-15  │ │
│  │                                                       │ │
│  │ [VIEW DETAILS] [SCHEDULE INSPECTION] [ASSIGN TO JOB] │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ REEL-R002                          STATUS: In Use     │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │ Size: 1.75" OD | Length: 6,000 m | Material: QT-700  │ │
│  │                                                       │ │
│  │ Fatigue: ████░░░░░░  45%  ✓ GOOD                     │ │
│  │ Life Remaining: ~2,750 cycles                         │ │
│  │                                                       │ │
│  │ Currently on: CT-UNIT-003 | Job: JOB-789             │ │
│  │ Total Jobs: 28  |  Total Meters: 85,000 m            │ │
│  │                                                       │ │
│  │ [VIEW LIVE DATA] [VIEW DETAILS]                      │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4. Reel Details - Fatigue Lifecycle View

```
┌─────────────────────────────────────────────────────────────┐
│  ← Back to Reels    REEL-R001 DETAILS                      │
├─────────────────────────────────────────────────────────────┤
│  Tabs: [Overview] [Fatigue Analysis] [History] [Inspections]│
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  FATIGUE LIFECYCLE VISUALIZATION                    │  │
│  │                                                     │  │
│  │   100% ┤                                    ╱──── Critical│
│  │        │                               ╱────              │
│  │    80% ┤                          ╱────        ⚠ Current │
│  │        │                     ╱────                       │
│  │    60% ┤                ╱────                            │
│  │        │           ╱────                                 │
│  │    40% ┤      ╱────                                      │
│  │        │ ╱────                                           │
│  │    20% ┼────                                             │
│  │        │                                                 │
│  │     0% └──────────────────────────────────────→         │
│  │         0    500  1000  1500  2000  2500  Cycles        │
│  │                                                          │
│  │  Current: 82% (1,850 cycles)                            │
│  │  Projected Retirement: ~2,300 cycles (~450 cycles left) │
│  │                                                          │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  FATIGUE BREAKDOWN BY FACTOR                        │  │
│  │                                                     │  │
│  │  Bending Stress (Reel):     ████████░░  35%        │  │
│  │  Bending Stress (Gooseneck):███████░░░  30%        │  │
│  │  Axial Tension:             ████░░░░░░  15%        │  │
│  │  Internal Pressure:         ███░░░░░░░  12%        │  │
│  │  Corrosion Factor:          ███░░░░░░░  10%        │  │
│  │                                                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  RECENT JOBS IMPACT                                 │  │
│  ├─────────────────────────────────────────────────────┤  │
│  │  Job        Date        Cycles   Fatigue Added      │  │
│  │  JOB-788    2026-01-20    12       1.2%            │  │
│  │  JOB-765    2026-01-15     8       0.8%            │  │
│  │  JOB-743    2026-01-10    15       1.5%            │  │
│  │  JOB-721    2026-01-05    10       1.0%            │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  [SCHEDULE INSPECTION] [GENERATE LIFECYCLE REPORT]         │
└─────────────────────────────────────────────────────────────┘
```

### 5. Job Planning Interface

```
┌─────────────────────────────────────────────────────────────┐
│  CREATE NEW JOB                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Step 1 of 4: Job Information                              │
│  ━━━━━━━━━━━━━░░░░░░░░░░░░░░░░░░░░░░                      │
│                                                             │
│  Job Type: [Well Cleanout        ▼]                        │
│  Priority:  ⚪ Low  ⚪ Medium  🔘 High  ⚪ Critical          │
│                                                             │
│  Well Information:                                          │
│    Well Name:    [WELL-XYZ-001        ]                    │
│    Field:        [Campo Norte         ]                    │
│    Client:       [ACME Oil Corp       ]                    │
│    Depth (MD):   [3,250] m                                 │
│    Target Zone:  [2,800 - 3,200] m                         │
│                                                             │
│  Planned Start:  [2026-01-25] 📅  [08:00] 🕐              │
│  Est. Duration:  [8] hours                                 │
│                                                             │
│  Description:                                               │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ Cleanout operation to remove sand fill from          │  │
│  │ production tubing. Expected 150m of fill.             │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  [CANCEL]                         [NEXT: Select Resources →]│
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  CREATE NEW JOB                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Step 2 of 4: Resource Assignment                          │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━░░░░░░░░░░░░                    │
│                                                             │
│  CT Unit Selection:                                         │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ ⚪ CT-001  Available  Base         1,250 hrs         │  │
│  │ ⚪ CT-002  Available  Campo Norte    985 hrs         │  │
│  │ 🔘 CT-003  Available  Pad A       1,105 hrs  ✓ SELECTED││
│  │ ⚪ CT-004  In Use     Campo Sur   1,420 hrs         │  │
│  │ ⚪ CT-005  Maintenance Workshop   1,680 hrs         │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  Reel Selection (for CT-003):                              │
│  Recommended: 2" OD, 6000m minimum                         │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ ⚪ REEL-R001  2" OD  5,000m  82% fatigue  ⚠ High    │  │
│  │ 🔘 REEL-R002  2" OD  6,000m  45% fatigue  ✓ Good   │  │
│  │ ⚪ REEL-R003  1.75" OD  6,500m  38% fatigue  ✓ Good│  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  Personnel Assignment:                                      │
│    Operator:    [Juan Pérez          ▼]                   │
│    Supervisor:  [Carlos Mendez       ▼]                   │
│    Engineer:    [María González      ▼]                   │
│                                                             │
│  [← BACK]                            [NEXT: Configure BHA →]│
└─────────────────────────────────────────────────────────────┘
```

### 6. Analytics Dashboard

```
┌─────────────────────────────────────────────────────────────┐
│  COILED TUBING ANALYTICS        Period: [Last 90 Days ▼]  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │  JOBS    │  │  NPT     │  │ SUCCESS  │  │ REVENUE  │  │
│  │  COMPLETED│  │  AVERAGE │  │   RATE   │  │  (USD)   │  │
│  │          │  │          │  │          │  │          │  │
│  │   127    │  │  2.5 hrs │  │  96.8%   │  │  $1.2M   │  │
│  │  ↑ 15%   │  │  ↓ 8%    │  │  ↑ 2%    │  │  ↑ 22%   │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  JOBS BY TYPE (Last 90 Days)                        │  │
│  │                                                     │  │
│  │  Well Cleanout     ████████████████  42 (33%)      │  │
│  │  Acid Stimulation  ███████████       28 (22%)      │  │
│  │  Nitrogen Lifting  ████████          20 (16%)      │  │
│  │  Milling           ██████            15 (12%)      │  │
│  │  Cement Squeeze    █████             13 (10%)      │  │
│  │  Other             ███                9  (7%)      │  │
│  │                                                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  FLEET PERFORMANCE COMPARISON                       │  │
│  │                                                     │  │
│  │  Unit    Jobs  Util%  NPT   Avg.Duration  Rating   │  │
│  │  CT-001   32   85%   2.1hr    6.5hr     ★★★★★     │  │
│  │  CT-002   28   78%   2.8hr    7.2hr     ★★★★☆     │  │
│  │  CT-003   35   92%   1.9hr    6.1hr     ★★★★★     │  │
│  │  CT-004   22   65%   3.2hr    7.8hr     ★★★☆☆     │  │
│  │  CT-005   10   42%   4.1hr    8.5hr     ★★☆☆☆     │  │
│  │                                                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  PREDICTIVE INSIGHTS                                │  │
│  │                                                     │  │
│  │  🔮 REEL-R001 will reach 95% fatigue in ~3 weeks   │  │
│  │  🔮 CT-005 requires major maintenance (overdue)     │  │
│  │  🔮 Peak demand expected in 2 weeks (historical)    │  │
│  │  🔮 Recommend scheduling CT-002 preventive maint.   │  │
│  │                                                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  [EXPORT DATA] [GENERATE REPORT] [CONFIGURE METRICS]       │
└─────────────────────────────────────────────────────────────┘
```

## Componentes Reutilizables

### 1. Status Badge Component
```typescript
@Component({
  selector: 'ct-status-badge',
  template: `
    <span class="status-badge" [ngClass]="statusClass">
      <mat-icon *ngIf="icon">{{icon}}</mat-icon>
      {{label}}
    </span>
  `
})
export class CTStatusBadgeComponent {
  @Input() status: UnitStatus;
  
  get statusClass(): string {
    return `status-${this.status.toLowerCase()}`;
  }
  
  get icon(): string {
    const icons = {
      'OPERATIONAL': 'check_circle',
      'STANDBY': 'pause_circle',
      'MAINTENANCE': 'build',
      'OFFLINE': 'cancel',
      'ALARM': 'warning'
    };
    return icons[this.status];
  }
  
  get label(): string {
    return this.status.replace('_', ' ');
  }
}
```

### 2. Fatigue Gauge Component
```typescript
@Component({
  selector: 'ct-fatigue-gauge',
  template: `
    <div class="fatigue-gauge">
      <svg viewBox="0 0 200 200">
        <!-- Gauge background -->
        <circle cx="100" cy="100" r="80" 
                fill="none" stroke="#e0e0e0" stroke-width="20"/>
        
        <!-- Fatigue arc -->
        <circle cx="100" cy="100" r="80" 
                fill="none" [attr.stroke]="gaugeColor" stroke-width="20"
                [attr.stroke-dasharray]="dashArray"
                [attr.stroke-dashoffset]="dashOffset"
                transform="rotate(-90 100 100)"/>
        
        <!-- Center text -->
        <text x="100" y="100" text-anchor="middle" 
              font-size="32" font-weight="bold">
          {{fatigue}}%
        </text>
        <text x="100" y="125" text-anchor="middle" 
              font-size="14" fill="#666">
          Fatigue
        </text>
      </svg>
      
      <div class="gauge-label">
        <mat-icon [style.color]="gaugeColor">{{statusIcon}}</mat-icon>
        {{statusLabel}}
      </div>
    </div>
  `
})
export class CTFatigueGaugeComponent {
  @Input() fatigue: number; // 0-100
  
  get gaugeColor(): string {
    if (this.fatigue < 40) return '#4CAF50';
    if (this.fatigue < 60) return '#8BC34A';
    if (this.fatigue < 80) return '#FFC107';
    if (this.fatigue < 95) return '#FF9800';
    return '#F44336';
  }
  
  get statusIcon(): string {
    if (this.fatigue < 80) return 'check_circle';
    if (this.fatigue < 95) return 'warning';
    return 'error';
  }
  
  get statusLabel(): string {
    if (this.fatigue < 40) return 'Excellent';
    if (this.fatigue < 60) return 'Good';
    if (this.fatigue < 80) return 'Moderate';
    if (this.fatigue < 95) return 'High';
    return 'Critical';
  }
  
  get dashArray(): string {
    const circumference = 2 * Math.PI * 80;
    return `${circumference} ${circumference}`;
  }
  
  get dashOffset(): number {
    const circumference = 2 * Math.PI * 80;
    return circumference - (this.fatigue / 100) * circumference;
  }
}
```

### 3. Real-Time Parameter Widget
```typescript
@Component({
  selector: 'ct-realtime-parameter',
  template: `
    <div class="parameter-widget">
      <div class="parameter-header">
        <mat-icon>{{icon}}</mat-icon>
        <span class="parameter-name">{{label}}</span>
      </div>
      
      <div class="parameter-value">
        <span class="value" [ngClass]="valueClass">{{value}}</span>
        <span class="unit">{{unit}}</span>
        <mat-icon class="trend-icon">{{trendIcon}}</mat-icon>
      </div>
      
      <div class="parameter-bar">
        <div class="bar-background">
          <div class="bar-fill" [style.width.%]="percentage"
               [ngClass]="barClass"></div>
          <div class="bar-limit" [style.left.%]="limitPercentage"></div>
        </div>
        <div class="bar-labels">
          <span>{{min}}</span>
          <span>{{max}}</span>
        </div>
      </div>
      
      <div class="parameter-sparkline">
        <canvas #sparklineCanvas></canvas>
      </div>
    </div>
  `
})
export class CTRealtimeParameterComponent implements OnInit, OnDestroy {
  @Input() label: string;
  @Input() value: number;
  @Input() unit: string;
  @Input() min: number = 0;
  @Input() max: number = 100;
  @Input() limit: number;
  @Input() icon: string;
  @Input() history: number[] = [];
  
  @ViewChild('sparklineCanvas') sparklineCanvas: ElementRef;
  
  get percentage(): number {
    return ((this.value - this.min) / (this.max - this.min)) * 100;
  }
  
  get limitPercentage(): number {
    return ((this.limit - this.min) / (this.max - this.min)) * 100;
  }
  
  get valueClass(): string {
    if (this.value > this.limit) return 'value-alarm';
    if (this.value > this.limit * 0.9) return 'value-warning';
    return 'value-normal';
  }
  
  get barClass(): string {
    if (this.value > this.limit) return 'bar-alarm';
    if (this.value > this.limit * 0.9) return 'bar-warning';
    return 'bar-normal';
  }
  
  get trendIcon(): string {
    if (this.history.length < 2) return 'trending_flat';
    const recent = this.history.slice(-5);
    const avg = recent.reduce((a, b) => a + b) / recent.length;
    if (this.value > avg * 1.05) return 'trending_up';
    if (this.value < avg * 0.95) return 'trending_down';
    return 'trending_flat';
  }
  
  ngOnInit() {
    this.renderSparkline();
  }
  
  renderSparkline() {
    // Implementación de sparkline con Chart.js
  }
}
```

## Responsive Design

### Breakpoints
```scss
$breakpoint-mobile: 600px;
$breakpoint-tablet: 960px;
$breakpoint-desktop: 1280px;
$breakpoint-large: 1920px;
```

### Mobile Layout (< 600px)
- Stack vertical de todos los componentes
- Dashboards simplificados
- Touch-optimized controls (min 44px touch targets)
- Bottom navigation bar
- Swipe gestures para navegación

### Tablet Layout (600px - 960px)
- Grid 2 columnas
- Side navigation colapsable
- Dashboards optimizados
- Gestos táctiles + teclado

### Desktop Layout (> 960px)
- Layouts completos multi-columna
- Sidebar fijo
- Shortcuts de teclado
- Hover states
- Drag & drop habilitado

## Accesibilidad

### WCAG 2.1 Level AA Compliance
- Contraste de color mínimo 4.5:1
- Navegación completa por teclado
- ARIA labels en todos los componentes
- Screen reader support
- Focus indicators visibles
- Alt text en imágenes e iconos

### Keyboard Shortcuts
```
Ctrl + N      : Nuevo trabajo
Ctrl + S      : Guardar
Ctrl + F      : Buscar
Ctrl + D      : Dashboard
Esc           : Cerrar modal
Tab           : Navegación
Enter         : Confirmar
Space         : Seleccionar
Arrow Keys    : Navegación en listas
```

## Animaciones y Transiciones

### Principios
- Duración: 200-300ms para microinteracciones
- Easing: cubic-bezier(0.4, 0.0, 0.2, 1)
- Reducir movimiento si está configurado (prefers-reduced-motion)

### Ejemplos
```scss
.card-enter {
  animation: slideInUp 300ms cubic-bezier(0.4, 0.0, 0.2, 1);
}

.gauge-update {
  transition: stroke-dashoffset 500ms ease-out;
}

.alarm-pulse {
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
```

## Testing UI/UX

### Pruebas Requeridas
1. Usability testing con operadores reales
2. A/B testing de layouts alternativos
3. Performance testing (60fps mínimo)
4. Cross-browser testing
5. Mobile device testing
6. Accessibility audit

### Métricas de Éxito
- Time to complete task < 30 segundos
- Error rate < 5%
- User satisfaction score > 4/5
- Load time < 2 segundos
- 60 FPS en animaciones

---

**Versión**: 1.0.0  
**Última Actualización**: Enero 2026
