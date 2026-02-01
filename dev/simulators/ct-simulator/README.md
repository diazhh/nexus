# NEXUS CT Simulator - Coiled Tubing Operations

Telemetry simulator for coiled tubing operations with Palmgren-Miner fatigue analysis and automated job state machine.

## Features

- **Palmgren-Miner Fatigue Model**: Cumulative damage tracking
- **Job State Machine**: Automatic phase transitions (Idle → RIH → Work → POOH)
- **Realistic Telemetry**:
  - Depth, tension, pressure, flow rate
  - Axial stress calculations
  - Fatigue metrics (cycles, damage, remaining life)
- **Multiple Material Grades**: CT80, CT90, CT100, CT110
- **Multiple CT Units**: Simulate fleet operations
- **ThingsBoard Integration**: MQTT v1 protocol support

## Quick Start

### 1. Configure CT Units

```bash
cd ct-simulator
cp config.example.yaml config.yaml
nano config.yaml  # Edit with your device tokens and parameters
```

### 2. Run Simulator

Development mode:
```bash
npm run dev
```

Production mode:
```bash
npm run build
npm start
```

## Job Phases

The simulator automatically cycles through these phases:

### 1. Idle (⏸️)
- **Duration**: 60 seconds
- **Depth**: 0 ft (surface)
- **Tension**: ~0 lbf
- **Pressure**: ~0 psi
- **Next**: Transitions to RIH

### 2. RIH - Run In Hole (⬇️)
- **Duration**: Depends on target depth and RIH speed
- **Depth**: Increases from 0 to target depth
- **Tension**: Increases with depth (string weight + overpull)
- **Pressure**: Light circulation (~500 psi)
- **Speed**: Configured `rihSpeed` (ft/min)
- **Next**: Transitions to Work when target depth reached

### 3. Work (🔧)
- **Duration**: Configured `workDuration` (seconds)
- **Depth**: Constant at target depth
- **Tension**: String weight + work overpull
- **Pressure**: Working pressure (2000-7000 psi)
- **Flow**: Active pumping
- **Next**: Transitions to POOH after work duration

### 4. POOH - Pull Out Of Hole (⬆️)
- **Duration**: Depends on depth and POOH speed
- **Depth**: Decreases from target to surface
- **Tension**: Decreases with depth
- **Pressure**: Reduced circulation (~300 psi)
- **Speed**: Configured `poohSpeed` (ft/min)
- **Next**: Returns to Idle when at surface

## Fatigue Model

### Palmgren-Miner Rule

Cumulative damage calculation:

```
D = Σ(ni / Ni)

Where:
  D  = Cumulative damage (0-1, failure at D ≥ 1.0)
  ni = Number of cycles at stress level i
  Ni = Cycles to failure at stress level i
```

### S-N Curve

Cycles to failure based on stress:

```
N = C / σ^m

Where:
  N = Cycles to failure
  C = Fatigue coefficient (material constant)
  σ = Stress amplitude (psi)
  m = Fatigue exponent (3-5 for steel)
```

### Material Properties

| Grade | Yield (ksi) | Ultimate (ksi) | Fatigue Exp (m) |
|-------|-------------|----------------|-----------------|
| CT80  | 80          | 95             | 4.0             |
| CT90  | 90          | 105            | 4.2             |
| CT100 | 100         | 115            | 4.5             |
| CT110 | 110         | 125            | 4.8             |

### Stress Calculation

```
σ = F / A

Where:
  σ = Axial stress (psi)
  F = Tension force (lbf)
  A = Cross-sectional area (in²)

A = π * (ro² - ri²)
  ro = Outer radius
  ri = Inner radius
```

## Configuration

### CT Unit Parameters

```yaml
units:
  - deviceToken: "YOUR_TOKEN"
    unitName: "CT Unit 1"
    material: "CT80"           # Material grade
    outerDiameter: 2.0         # inches
    wallThickness: 0.156       # inches
    targetDepth: 12000         # feet
    pipeWeight: 1.08           # lbf/ft
    workPressure: 3000         # psi
    workFlowRate: 4.0          # bbl/min
    rihSpeed: 120              # ft/min
    poohSpeed: 150             # ft/min
    workDuration: 1800         # seconds (30 min)
```

### Common CT Sizes

| OD (in) | WT (in) | Weight (lbf/ft) | Typical Use |
|---------|---------|-----------------|-------------|
| 1.25    | 0.109   | 0.51            | Light duty  |
| 1.50    | 0.125   | 0.68            | Standard    |
| 1.75    | 0.134   | 0.82            | Medium      |
| 2.00    | 0.156   | 1.08            | Common      |
| 2.375   | 0.175   | 1.70            | Heavy duty  |
| 2.875   | 0.203   | 2.40            | Very heavy  |

### Simulation Speed

```yaml
simulation:
  rate_hz: 0.2              # Data every 5 seconds
  realtime_factor: 60.0     # 1 sim hour = 1 real minute
```

**Time Scaling Examples**:
- `realtime_factor: 1` = Real-time (1:1)
- `realtime_factor: 60` = 1 hour in 1 minute
- `realtime_factor: 3600` = 1 hour in 1 second

## Generated Telemetry

| Key | Description | Units |
|-----|-------------|-------|
| `job_phase` | Current phase | Idle/RIH/Work/POOH |
| `depth` | Current depth | ft |
| `tension` | Axial tension | lbf |
| `surface_pressure` | Surface pressure | psi |
| `flow_rate` | Pump rate | bbl/min |
| `axial_stress` | Calculated stress | psi |
| `total_cycles` | Fatigue cycles | count |
| `cumulative_damage` | Miner's D value | 0-1 |
| `remaining_life` | % life left | % |
| `predicted_cycles_to_failure` | Estimated cycles | count |
| `phase_elapsed_time` | Time in phase | seconds |
| `total_run_time` | Total job time | seconds |

## Example Output

```
═══════════════════════════════════════════════════
   NEXUS CT Simulator - Coiled Tubing Operations
═══════════════════════════════════════════════════

🔧 Initializing 2 CT units...

  ✅ CT Unit 1:
     Material: CT80 (80 ksi)
     OD: 2" x 0.156" WT
     Target Depth: 12000 ft
     RIH/POOH Speed: 120/150 ft/min
     Work Time: 30 min

  ✅ CT Unit 2:
     Material: CT110 (110 ksi)
     OD: 2.375" x 0.175" WT
     Target Depth: 18000 ft
     RIH/POOH Speed: 100/120 ft/min
     Work Time: 60 min

🔌 Connecting MQTT clients...
  ✅ CT Unit 1 connected
  ✅ CT Unit 2 connected

🚀 Starting CT Simulator:
   Rate: 0.2 Hz
   Interval: 5000 ms
   Realtime Factor: 60x

⏸️  CT Unit 1 [Idle]: Depth=0 ft, Tension=0 lbf, Life=100.0%
⬇️  CT Unit 1 [RIH]: Depth=2400 ft, Tension=2950 lbf, Life=100.0%
⬇️  CT Unit 1 [RIH]: Depth=7200 ft, Tension=7890 lbf, Life=100.0%
🔧 CT Unit 1 [Work]: Depth=12000 ft, Tension=15120 lbf, Life=99.8%
⬆️  CT Unit 1 [POOH]: Depth=8500 ft, Tension=9980 lbf, Life=99.7%
⏸️  CT Unit 1 [Idle]: Depth=0 ft, Tension=0 lbf, Life=99.6%
```

## Use Cases

### 1. Fatigue Life Monitoring

Track cumulative damage across multiple trips:

```yaml
simulation:
  realtime_factor: 3600    # Fast simulation
units:
  - material: "CT80"
    targetDepth: 15000
    # ... will show fatigue progression over many cycles
```

### 2. Material Comparison

Compare different CT grades:

```yaml
units:
  - unitName: "CT80 Unit"
    material: "CT80"
    targetDepth: 12000

  - unitName: "CT110 Unit"
    material: "CT110"
    targetDepth: 12000    # Same depth, different material
```

### 3. Operational Planning

Test different job parameters:

```yaml
units:
  # Fast, shallow jobs
  - targetDepth: 8000
    rihSpeed: 150
    workDuration: 900

  # Slow, deep jobs
  - targetDepth: 20000
    rihSpeed: 80
    workDuration: 7200
```

## Integration with NEXUS Data Mapping

1. **Create Device** for each CT unit in ThingsBoard
2. **Get device token** and add to config.yaml
3. **Start simulator** - telemetry flows to Device
4. **Create Asset** (CT unit digital twin)
5. **Apply Mapping Template** via Data Sources UI:
   - Select CT unit Device
   - Select CT unit Asset
   - Choose CT mapping template
6. **Data flows**: Device → Rule Chain → Asset

### Example Mapping

| Source Key (Device) | Target Key (Asset) |
|---------------------|-------------------|
| job_phase | current_phase |
| depth | measured_depth |
| tension | string_tension |
| surface_pressure | pump_pressure |
| cumulative_damage | fatigue_damage |
| remaining_life | string_life_remaining |

## Troubleshooting

### Fatigue accumulates too fast

- Reduce `realtime_factor` for more realistic timing
- Check material grade matches actual CT string
- Verify `outerDiameter` and `wallThickness` are correct

### Unit stays in one phase

- Check `targetDepth` is reasonable
- Verify `rihSpeed` and `poohSpeed` > 0
- Ensure `workDuration` > 0

### Tension values unrealistic

- Verify `pipeWeight` matches CT size
- Check `targetDepth` is reasonable
- Material density should be ~490 lbf/ft³ for steel

### No phase transitions

- Increase `realtime_factor` to speed up simulation
- Check console for state transition messages
- Verify simulation is actually running

## Performance

- **Memory**: ~10-20 MB per CT unit
- **CPU**: <5% on modern hardware
- **Network**: ~800 bytes/message at 0.2 Hz = ~160 B/s per unit

## License

Apache License 2.0 - Copyright © 2016-2026 The Thingsboard Authors
