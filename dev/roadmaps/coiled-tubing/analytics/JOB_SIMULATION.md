# Simulación de Trabajos - Módulo Coiled Tubing

## Visión General

El simulador de trabajos permite pre-planificar operaciones de coiled tubing, predecir parámetros operacionales, identificar riesgos potenciales y optimizar recursos antes de la ejecución en campo.

## Objetivos del Simulador

1. **Validación de Factibilidad**: Verificar si un trabajo es técnicamente posible
2. **Optimización de Parámetros**: Determinar presión, tensión y velocidad óptimas
3. **Predicción de Tiempos**: Estimar duración de cada fase del trabajo
4. **Análisis de Riesgos**: Identificar puntos críticos (stuck pipe, exceso de tensión)
5. **Selección de Equipos**: Determinar unidad, reel y BHA adecuados
6. **Consumo de Fatiga**: Predecir impacto en la vida útil del reel

## Modelo de Simulación

### Inputs del Simulador

```typescript
interface JobSimulationInput {
  // Datos del pozo
  well: {
    name: string;
    depth_md_ft: number;
    depth_tvd_ft: number;
    inclination_profile: DeviationPoint[];
    wellbore_diameter_inch: number;
    fluid_density_ppg: number;
    temperature_profile: TemperaturePoint[];
  };
  
  // Tipo de trabajo
  jobType: 'CLEANOUT' | 'STIMULATION' | 'MILLING' | 'NITROGEN_LIFTING';
  
  // Equipos seleccionados
  equipment: {
    unit_id: string;
    reel_id: string;
    tubing_od_inch: number;
    tubing_id_inch: number;
    bha_configuration: BHAConfig;
  };
  
  // Parámetros operacionales planeados
  operations: {
    target_depth_ft: number;
    max_running_speed_ft_min: number;
    pump_rate_bpm?: number;
    treatment_pressure_psi?: number;
    nitrogen_rate_scf?: number;
  };
  
  // Condiciones del sitio
  site: {
    elevation_ft: number;
    ambient_temp_f: number;
  };
}
```

### Outputs del Simulador

```typescript
interface JobSimulationResult {
  // Factibilidad
  feasibility: {
    is_feasible: boolean;
    limiting_factors: string[];
    warnings: string[];
  };
  
  // Parámetros operacionales predichos
  predictions: {
    max_hookload_lbf: number;
    max_pressure_surface_psi: number;
    max_pressure_downhole_psi: number;
    estimated_duration_hours: number;
    phase_durations: PhaseDuration[];
  };
  
  // Análisis de fuerzas
  forces: {
    depth_ft: number[];
    hookload_lbf: number[];
    friction_force_lbf: number[];
    buckling_margin: number[];
  };
  
  // Análisis hidráulico
  hydraulics: {
    depth_ft: number[];
    pressure_psi: number[];
    friction_pressure_psi: number[];
    velocity_ft_sec: number[];
  };
  
  // Fatiga estimada
  fatigue: {
    estimated_cycles: number;
    estimated_fatigue_consumption_percent: number;
    new_accumulated_fatigue_percent: number;
  };
  
  // Riesgos identificados
  risks: Risk[];
  
  // Recomendaciones
  recommendations: string[];
}
```

## Algoritmos de Simulación

### 1. Análisis de Fuerzas

#### A. Peso en Fluido (Buoyant Weight)

```
Wb = W × (1 - ρf / ρs)

Donde:
Wb = Peso en fluido (lbf/ft)
W  = Peso en aire (lbf/ft)
ρf = Densidad del fluido (ppg)
ρs = Densidad del acero (~65 ppg)
```

#### B. Fricción (Modelo de Amontons-Coulomb)

```
Ff = μ × N

Donde:
Ff = Fuerza de fricción (lbf/ft)
μ  = Coeficiente de fricción (típicamente 0.2-0.3)
N  = Fuerza normal (lbf/ft)

Para sección inclinada:
N = Wb × sin(α) × ΔL

α  = Ángulo de inclinación
ΔL = Longitud del segmento
```

#### C. Hookload (Carga en Superficie)

**Running In (bajando)**:
```
HL = ∫[0 to D] (Wb - Ff) dL

Donde:
HL = Hookload (lbf)
D  = Profundidad (ft)
```

**Pulling Out (subiendo)**:
```
HL = ∫[0 to D] (Wb + Ff) dL
```

### 2. Análisis Hidráulico

#### A. Pérdida de Presión por Fricción (Annulus)

```
ΔPf = (f × ρ × L × v²) / (2 × g × Dh)

Donde:
ΔPf = Pérdida de presión por fricción (psi)
f   = Factor de fricción de Darcy
ρ   = Densidad del fluido (ppg)
L   = Longitud (ft)
v   = Velocidad (ft/sec)
g   = Gravedad (32.2 ft/sec²)
Dh  = Diámetro hidráulico (inch)
```

#### B. Velocidad Anular

```
va = Q / Aa

Donde:
va = Velocidad anular (ft/sec)
Q  = Caudal (bpm) × 0.000972 (conversión a ft³/sec)
Aa = Área anular (in²)

Aa = π/4 × (Dwb² - Dct²)
```

#### C. Presión en Fondo (Bottomhole Pressure)

```
Pbh = Ps + Ph + ΔPf

Donde:
Pbh = Presión en fondo (psi)
Ps  = Presión de superficie (psi)
Ph  = Presión hidrostática (psi)
ΔPf = Pérdida por fricción (psi)

Ph = 0.052 × ρ × D
```

### 3. Análisis de Pandeo (Buckling)

#### Criterio de Pandeo Helicoidal

```
Fcr = 2.83 × √(EI × W)

Donde:
Fcr = Carga crítica de pandeo (lbf)
E   = Módulo de Young (psi)
I   = Momento de inercia (in⁴)
W   = Peso por unidad de longitud (lbf/ft)

I = π/64 × (Do⁴ - Di⁴)
```

**Margen de seguridad**:
```
Safety Margin = (Fcr - Factual) / Fcr × 100%

Si Safety Margin < 20%: Alto riesgo de pandeo
```

### 4. Predicción de Tiempos

#### Tiempo de Despliegue (Running In)

```
t_running = D / v_avg

Donde:
t_running = Tiempo de bajada (min)
D         = Profundidad objetivo (ft)
v_avg     = Velocidad promedio (ft/min)
```

Ajustes por condiciones:
- Reducir velocidad en secciones desviadas
- Paradas por conexiones de herramientas
- Velocidad reducida cerca del fondo

#### Tiempo de Tratamiento

```
t_treatment = V_total / Q

Donde:
t_treatment = Tiempo de tratamiento (min)
V_total     = Volumen total a bombear (bbl)
Q           = Tasa de bombeo (bpm)
```

#### Tiempo Total Estimado

```
t_total = t_rig_up + t_running + t_on_depth + t_treatment + t_pulling + t_rig_down

Donde cada fase incluye:
- Tiempo productivo
- Tiempo de conexiones
- Factor de contingencia (10-15%)
```

## Implementación del Simulador

### Servicio Java

```java
@Service
public class CTJobSimulationService {
    
    @Autowired
    private CTUnitService unitService;
    
    @Autowired
    private CTReelService reelService;
    
    /**
     * Ejecuta simulación completa de un trabajo
     */
    public JobSimulationResult simulateJob(JobSimulationInput input) {
        
        // 1. Validar inputs
        validateInput(input);
        
        // 2. Obtener datos de equipos
        CTUnit unit = unitService.getById(input.getEquipment().getUnitId());
        CTReel reel = reelService.getById(input.getEquipment().getReelId());
        
        // 3. Construir perfil del pozo
        WellProfile wellProfile = buildWellProfile(input.getWell());
        
        // 4. Análisis de fuerzas
        ForceAnalysis forces = analyzeForces(wellProfile, input, reel);
        
        // 5. Análisis hidráulico
        HydraulicAnalysis hydraulics = analyzeHydraulics(wellProfile, input);
        
        // 6. Análisis de pandeo
        BucklingAnalysis buckling = analyzeBuckling(forces, reel);
        
        // 7. Predicción de fatiga
        FatigueEstimation fatigue = estimateFatigue(forces, reel, input);
        
        // 8. Análisis de riesgos
        List<Risk> risks = identifyRisks(forces, hydraulics, buckling, unit, reel);
        
        // 9. Verificar factibilidad
        FeasibilityResult feasibility = checkFeasibility(
            forces, hydraulics, buckling, unit, reel, risks
        );
        
        // 10. Generar recomendaciones
        List<String> recommendations = generateRecommendations(
            feasibility, risks, forces, hydraulics
        );
        
        // 11. Construir resultado
        return JobSimulationResult.builder()
            .feasibility(feasibility)
            .predictions(buildPredictions(forces, hydraulics, input))
            .forces(forces)
            .hydraulics(hydraulics)
            .fatigue(fatigue)
            .risks(risks)
            .recommendations(recommendations)
            .build();
    }
    
    /**
     * Análisis de fuerzas a lo largo del pozo
     */
    private ForceAnalysis analyzeForces(
        WellProfile well, 
        JobSimulationInput input,
        CTReel reel
    ) {
        List<Double> depths = new ArrayList<>();
        List<Double> hookloads = new ArrayList<>();
        List<Double> frictionForces = new ArrayList<>();
        
        double tubingOD = input.getEquipment().getTubingOdInch();
        double tubingID = input.getEquipment().getTubingIdInch();
        
        // Peso de tubería por pie
        double area = Math.PI / 4 * (Math.pow(tubingOD, 2) - Math.pow(tubingID, 2));
        double weightPerFt = area * 490 / 12; // lb/ft (acero)
        
        // Peso en fluido
        double fluidDensity = input.getWell().getFluidDensityPpg();
        double buoyancyFactor = 1 - (fluidDensity / 65.0);
        double buoyantWeight = weightPerFt * buoyancyFactor;
        
        // Coeficiente de fricción
        double frictionCoef = 0.25;
        
        // Iterar por segmentos del pozo
        double cumulativeHookload = 0;
        
        for (WellSegment segment : well.getSegments()) {
            double depth = segment.getDepthMdFt();
            double inclination = segment.getInclinationDeg();
            double length = segment.getLengthFt();
            
            // Componente de peso
            double weightComponent = buoyantWeight * length;
            
            // Fuerza de fricción
            double normalForce = weightComponent * Math.sin(Math.toRadians(inclination));
            double frictionForce = frictionCoef * normalForce;
            
            // Hookload acumulado (running in - bajando)
            cumulativeHookload += weightComponent - frictionForce;
            
            depths.add(depth);
            hookloads.add(cumulativeHookload);
            frictionForces.add(frictionForce);
        }
        
        return ForceAnalysis.builder()
            .depthFt(depths)
            .hookloadLbf(hookloads)
            .frictionForceLbf(frictionForces)
            .build();
    }
    
    /**
     * Análisis hidráulico
     */
    private HydraulicAnalysis analyzeHydraulics(
        WellProfile well,
        JobSimulationInput input
    ) {
        // Implementación similar para presiones
        // ...
    }
    
    /**
     * Identificación de riesgos
     */
    private List<Risk> identifyRisks(
        ForceAnalysis forces,
        HydraulicAnalysis hydraulics,
        BucklingAnalysis buckling,
        CTUnit unit,
        CTReel reel
    ) {
        List<Risk> risks = new ArrayList<>();
        
        // 1. Verificar si excede capacidad de la unidad
        double maxHookload = forces.getHookloadLbf().stream()
            .max(Double::compare).orElse(0.0);
        
        if (maxHookload > unit.getMaxTensionLbf() * 0.9) {
            risks.add(Risk.builder()
                .severity("HIGH")
                .type("TENSION_EXCEEDED")
                .description("Max hookload (" + maxHookload + " lbf) exceeds 90% of unit capacity")
                .recommendation("Consider using heavier duty unit or reduce target depth")
                .build());
        }
        
        // 2. Verificar presión
        double maxPressure = hydraulics.getPressurePsi().stream()
            .max(Double::compare).orElse(0.0);
        
        if (maxPressure > reel.getMaterialYieldStrengthPsi() * 0.8) {
            risks.add(Risk.builder()
                .severity("CRITICAL")
                .type("PRESSURE_EXCEEDED")
                .description("Max pressure exceeds 80% of tubing yield strength")
                .recommendation("Reduce pump rate or use higher grade tubing")
                .build());
        }
        
        // 3. Verificar margen de pandeo
        double minBucklingMargin = buckling.getBucklingMargin().stream()
            .min(Double::compare).orElse(100.0);
        
        if (minBucklingMargin < 20) {
            risks.add(Risk.builder()
                .severity("MEDIUM")
                .type("BUCKLING_RISK")
                .description("Low buckling margin detected at depth")
                .recommendation("Increase WOB or reduce injection speed")
                .build());
        }
        
        return risks;
    }
}
```

### Frontend: Simulador Interactivo

```typescript
@Component({
  selector: 'ct-job-simulator',
  template: `
    <div class="simulator-container">
      <mat-stepper linear #stepper>
        
        <!-- Step 1: Job Configuration -->
        <mat-step [stepControl]="jobConfigForm">
          <form [formGroup]="jobConfigForm">
            <ng-template matStepLabel>Job Configuration</ng-template>
            
            <mat-form-field>
              <mat-label>Job Type</mat-label>
              <mat-select formControlName="jobType">
                <mat-option value="CLEANOUT">Well Cleanout</mat-option>
                <mat-option value="STIMULATION">Acid Stimulation</mat-option>
                <mat-option value="MILLING">Milling</mat-option>
              </mat-select>
            </mat-form-field>
            
            <mat-form-field>
              <mat-label>Well</mat-label>
              <input matInput formControlName="wellName" placeholder="WELL-001">
            </mat-form-field>
            
            <mat-form-field>
              <mat-label>Target Depth (ft MD)</mat-label>
              <input matInput type="number" formControlName="targetDepth">
            </mat-form-field>
            
            <div>
              <button mat-button matStepperNext>Next</button>
            </div>
          </form>
        </mat-step>
        
        <!-- Step 2: Equipment Selection -->
        <mat-step [stepControl]="equipmentForm">
          <form [formGroup]="equipmentForm">
            <ng-template matStepLabel>Select Equipment</ng-template>
            
            <ct-unit-selector formControlName="unitId"></ct-unit-selector>
            <ct-reel-selector formControlName="reelId"></ct-reel-selector>
            
            <div>
              <button mat-button matStepperPrevious>Back</button>
              <button mat-button matStepperNext>Next</button>
            </div>
          </form>
        </mat-step>
        
        <!-- Step 3: Parameters -->
        <mat-step [stepControl]="parametersForm">
          <form [formGroup]="parametersForm">
            <ng-template matStepLabel>Operational Parameters</ng-template>
            
            <mat-form-field>
              <mat-label>Max Speed (ft/min)</mat-label>
              <input matInput type="number" formControlName="maxSpeed">
            </mat-form-field>
            
            <mat-form-field>
              <mat-label>Pump Rate (bpm)</mat-label>
              <input matInput type="number" formControlName="pumpRate">
            </mat-form-field>
            
            <div>
              <button mat-button matStepperPrevious>Back</button>
              <button mat-button (click)="runSimulation()" color="primary">
                Run Simulation
              </button>
            </div>
          </form>
        </mat-step>
        
        <!-- Step 4: Results -->
        <mat-step>
          <ng-template matStepLabel>Results</ng-template>
          
          <div *ngIf="simulationResult">
            <!-- Feasibility -->
            <mat-card>
              <mat-card-title>Feasibility</mat-card-title>
              <mat-card-content>
                <div [class.feasible]="simulationResult.feasibility.is_feasible"
                     [class.not-feasible]="!simulationResult.feasibility.is_feasible">
                  <mat-icon>{{simulationResult.feasibility.is_feasible ? 'check_circle' : 'error'}}</mat-icon>
                  {{simulationResult.feasibility.is_feasible ? 'FEASIBLE' : 'NOT FEASIBLE'}}
                </div>
                
                <div class="warnings" *ngIf="simulationResult.feasibility.warnings.length > 0">
                  <h4>Warnings:</h4>
                  <ul>
                    <li *ngFor="let warning of simulationResult.feasibility.warnings">
                      {{warning}}
                    </li>
                  </ul>
                </div>
              </mat-card-content>
            </mat-card>
            
            <!-- Force Analysis Chart -->
            <mat-card>
              <mat-card-title>Force Analysis</mat-card-title>
              <mat-card-content>
                <canvas #forceChart></canvas>
              </mat-card-content>
            </mat-card>
            
            <!-- Hydraulic Analysis Chart -->
            <mat-card>
              <mat-card-title>Hydraulic Analysis</mat-card-title>
              <mat-card-content>
                <canvas #hydraulicChart></canvas>
              </mat-card-content>
            </mat-card>
            
            <!-- Fatigue Estimation -->
            <mat-card>
              <mat-card-title>Fatigue Impact</mat-card-title>
              <mat-card-content>
                <p>Estimated Cycles: {{simulationResult.fatigue.estimated_cycles}}</p>
                <p>Fatigue Consumption: {{simulationResult.fatigue.estimated_fatigue_consumption_percent}}%</p>
                <p>New Accumulated Fatigue: {{simulationResult.fatigue.new_accumulated_fatigue_percent}}%</p>
              </mat-card-content>
            </mat-card>
            
            <!-- Risks -->
            <mat-card *ngIf="simulationResult.risks.length > 0">
              <mat-card-title>Identified Risks</mat-card-title>
              <mat-card-content>
                <mat-list>
                  <mat-list-item *ngFor="let risk of simulationResult.risks">
                    <mat-icon [style.color]="getRiskColor(risk.severity)">
                      warning
                    </mat-icon>
                    <div mat-line>{{risk.type}}</div>
                    <div mat-line>{{risk.description}}</div>
                    <div mat-line><em>{{risk.recommendation}}</em></div>
                  </mat-list-item>
                </mat-list>
              </mat-card-content>
            </mat-card>
            
            <!-- Recommendations -->
            <mat-card>
              <mat-card-title>Recommendations</mat-card-title>
              <mat-card-content>
                <ul>
                  <li *ngFor="let rec of simulationResult.recommendations">
                    {{rec}}
                  </li>
                </ul>
              </mat-card-content>
            </mat-card>
            
            <div class="actions">
              <button mat-raised-button color="primary" 
                      (click)="createJobFromSimulation()"
                      [disabled]="!simulationResult.feasibility.is_feasible">
                Create Job
              </button>
              <button mat-button (click)="exportSimulation()">
                Export Report
              </button>
              <button mat-button matStepperPrevious>
                Modify Parameters
              </button>
            </div>
          </div>
        </mat-step>
      </mat-stepper>
    </div>
  `
})
export class CTJobSimulatorComponent {
  
  simulationResult: JobSimulationResult;
  
  runSimulation() {
    const input = this.buildSimulationInput();
    
    this.simulationService.simulate(input).subscribe(
      result => {
        this.simulationResult = result;
        this.renderCharts();
      }
    );
  }
  
  renderCharts() {
    this.renderForceChart();
    this.renderHydraulicChart();
  }
}
```

## Casos de Uso

### Caso 1: Cleanout Simple

```yaml
Input:
  Well: WELL-001
  Depth: 3000 ft vertical
  Job Type: CLEANOUT
  Unit: CT-001 (Standard)
  Reel: REEL-R001 (2" OD, QT-800)
  
Expected Output:
  Max Hookload: ~8,500 lbf
  Max Pressure: ~2,500 psi
  Duration: ~4.5 hours
  Fatigue: ~1.2%
  Feasibility: YES
```

### Caso 2: Estimulación Compleja

```yaml
Input:
  Well: WELL-XYZ (Horizontal 5000 ft MD, 3000 ft TVD)
  Job Type: ACID_STIMULATION
  Volume: 50 bbl
  Rate: 5 bpm
  
Expected Output:
  Max Hookload: ~18,000 lbf
  Max Pressure: ~4,800 psi
  Duration: ~8 hours
  Risks: [BUCKLING_IN_HORIZONTAL, HIGH_FRICTION]
  Recommendations: [Use lighter fluid, Reduce injection speed]
```

## Validación del Simulador

### Comparación con Datos Reales

Comparar predicciones con jobs ejecutados:

```sql
SELECT 
    j.job_number,
    j.max_actual_pressure_psi as actual_pressure,
    s.predicted_max_pressure_psi as simulated_pressure,
    ABS(j.max_actual_pressure_psi - s.predicted_max_pressure_psi) as error_psi,
    ABS(j.max_actual_pressure_psi - s.predicted_max_pressure_psi) / j.max_actual_pressure_psi * 100 as error_percent
FROM ct_jobs j
INNER JOIN ct_job_simulations s ON s.job_id = j.id
WHERE j.status = 'COMPLETED';
```

Target: Error < 10% en el 90% de los casos

## Mejoras Futuras

1. **Modelo 3D Interactivo**: Visualización 3D del pozo y tubería
2. **Optimización Automática**: Algoritmo que ajusta parámetros para minimizar riesgos
3. **Monte Carlo**: Análisis probabilístico con incertidumbre
4. **Machine Learning**: Mejorar predicciones con datos históricos
5. **Real-Time Calibration**: Ajustar simulación durante ejecución del trabajo

---

**Versión**: 1.0.0  
**Última Actualización**: Enero 2026
