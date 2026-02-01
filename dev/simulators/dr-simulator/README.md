# NEXUS DR Simulator - Drilling Operations

Telemetry simulator for drilling operations with MSE (Mechanical Specific Energy) and ECD (Equivalent Circulating Density) calculations.

## Features

- **MSE Calculation**: Drilling efficiency metric
- **ECD Calculation**: Dynamic pressure management
- **Realistic ROP**: Based on Bourgoyne & Young model
- **Formation Modeling**: Multiple geological layers
- **Problem Detection**: HIGH_MSE, HIGH/LOW_ECD, STICK_SLIP, LOW_ROP
- **Correlated Parameters**: WOB, RPM, torque, pressure
- **ThingsBoard Integration**: MQTT v1 protocol

## Quick Start

### 1. Configure Rigs

```bash
cd dr-simulator
cp config.example.yaml config.yaml
nano config.yaml  # Edit with device tokens and parameters
```

### 2. Run Simulator

Development:
```bash
npm run dev
```

Production:
```bash
npm run build
npm start
```

## Key Concepts

### MSE - Mechanical Specific Energy

MSE measures drilling efficiency - how much energy is required to remove a unit volume of rock.

**Formula**:
```
MSE = (WOB/A) + (120*π*RPM*T)/(ROP*A)

Where:
  WOB = Weight on bit (lbf)
  A   = Bit area (in²)
  RPM = Rotations per minute
  T   = Torque (ft-lbf)
  ROP = Rate of penetration (ft/hr)
```

**Interpretation**:
- **Optimal**: MSE ≈ Formation UCS (compressive strength)
- **High MSE**: Inefficient drilling
  - Dull bit
  - Wrong parameters
  - Bit balling
- **Low MSE**: Very efficient (rare)

**Units**: ksi (thousands of psi)

### ECD - Equivalent Circulating Density

ECD is the effective mud weight experienced by the formation while circulating, accounting for friction losses.

**Formula**:
```
ECD = MW + (ΔP_annular / (0.052 * TVD))

Where:
  MW = Static mud weight (ppg)
  ΔP_annular = Annular friction pressure (psi)
  TVD = True vertical depth (ft)
```

**Pressure Window**:
```
Pore Pressure < ECD < Fracture Pressure
```

**Problems**:
- **ECD too low**: Kick risk (formation fluids enter wellbore)
- **ECD too high**: Lost circulation (fracture formation)

**Units**: ppg (pounds per gallon)

### ROP - Rate of Penetration

How fast the bit drills through formation.

**Simplified Bourgoyne & Young**:
```
ROP ∝ (WOB/diameter)^a * RPM^b * (1/UCS)^c * drillability
```

**Factors**:
- ↑ WOB → ↑ ROP (within limits)
- ↑ RPM → ↑ ROP
- ↑ Formation hardness → ↓ ROP
- ↑ Drillability → ↑ ROP

**Units**: ft/hr

## Configuration

### Rig Parameters

```yaml
rigs:
  - deviceToken: "YOUR_TOKEN"
    rigName: "Rig 1"

    # Bit
    bitDiameter: 12.25        # inches (12-1/4")
    bitNozzles: 5
    bitTFA: 0.85              # Total Flow Area

    # Well
    holeSize: 12.25
    drillpipeOD: 5.0
    drillpipeID: 4.276

    # Operating params
    targetWOB: 30000          # lbf (30 klbf)
    targetRPM: 120
    targetFlowRate: 600       # gpm
    mudWeight: 10.5           # ppg

    # Start depth
    startDepth: 5000          # ft

    # Formations (see below)
```

### Formation Modeling

```yaml
formations:
  - name: "Shale"
    topDepth: 0
    bottomDepth: 8000
    compressiveStrength: 8000    # psi (UCS)
    drillability: 0.7            # 0-1 scale
    porePressureGradient: 0.465  # psi/ft
    fractureGradient: 0.85       # psi/ft

  - name: "Limestone"
    topDepth: 8000
    bottomDepth: 10000
    compressiveStrength: 18000   # Harder
    drillability: 0.4
    porePressureGradient: 0.465
    fractureGradient: 0.75
```

### Common Formation Properties

| Rock Type | UCS (psi) | Drillability | Typical ROP |
|-----------|-----------|--------------|-------------|
| Salt | 1,000-3,000 | 0.9-1.0 | 100-300 ft/hr |
| Shale | 5,000-10,000 | 0.6-0.8 | 50-150 ft/hr |
| Sandstone | 8,000-15,000 | 0.5-0.7 | 40-100 ft/hr |
| Limestone | 15,000-25,000 | 0.3-0.5 | 20-60 ft/hr |
| Dolomite | 20,000-30,000 | 0.2-0.4 | 15-40 ft/hr |
| Granite | 25,000-40,000 | 0.1-0.2 | 5-20 ft/hr |

### Pressure Gradients

| Type | Gradient (psi/ft) | Description |
|------|-------------------|-------------|
| Normal Pore | 0.433-0.465 | Hydrostatic (water) |
| Overpressure | 0.500-0.800 | Abnormal pressure |
| Fracture | 0.70-1.00 | Formation breakdown |

## Generated Telemetry

| Key | Description | Units |
|-----|-------------|-------|
| `weight_on_bit` | WOB | lbf |
| `rotary_speed` | RPM | RPM |
| `flow_rate` | Pump rate | gpm |
| `mud_weight` | Mud density | ppg |
| `rate_of_penetration` | ROP | ft/hr |
| `torque` | Bit torque | ft-lbf |
| `standpipe_pressure` | SPP | psi |
| `hook_load` | String weight | lbf |
| `mechanical_specific_energy` | MSE | ksi |
| `equivalent_circulating_density` | ECD | ppg |
| `depth` | Current depth | ft |
| `formation_name` | Current formation | string |
| `formation_ucs` | Rock strength | psi |
| `formation_drillability` | Drillability | 0-1 |
| `pore_pressure_gradient` | Pore pressure | psi/ft |
| `fracture_gradient` | Fracture pressure | psi/ft |
| `has_problems` | Problem flag | boolean |
| `problems` | Problem list | string |

## Problem Detection

### HIGH_MSE

**Meaning**: Drilling inefficiently

**Causes**:
- Dull bit
- Wrong WOB/RPM combination
- Bit balling (cuttings stuck to bit)
- Hydraulics issues

**Actions**:
- Check MSE vs formation UCS
- Adjust WOB or RPM
- Improve hydraulics
- Consider bit change

### HIGH_ECD

**Meaning**: ECD approaching fracture gradient

**Causes**:
- High flow rate
- High mud weight
- Narrow annular clearance
- High viscosity mud

**Actions**:
- Reduce flow rate
- Reduce mud weight (if safe)
- Check for tight spots
- Monitor for losses

### LOW_ECD

**Meaning**: ECD below pore pressure (underbalanced)

**Causes**:
- Low mud weight
- Low flow rate
- Pressure depletion

**Actions**:
- Increase mud weight
- Monitor for kicks
- Check flow-check procedures

### STICK_SLIP

**Meaning**: Torque oscillations, inefficient drilling

**Causes**:
- Low RPM
- High torque
- Formation characteristics
- BHA design

**Actions**:
- Increase RPM
- Reduce WOB
- Improve hydraulics
- Add downhole motor

### LOW_ROP

**Meaning**: Drilling too slowly

**Causes**:
- Hard formation
- Low WOB
- Dull bit
- Low RPM

**Actions**:
- Increase WOB (if MSE allows)
- Increase RPM
- Check bit condition
- Verify formation

## Example Output

```
═══════════════════════════════════════════════════
   NEXUS DR Simulator - Drilling Operations
═══════════════════════════════════════════════════

⚙️  Initializing 2 drilling rigs...

  ✅ Rig 1:
     Bit: 12.25" PDC, 5 nozzles
     Hole: 12.25" x 5" DP
     Target: 30000 lbf WOB, 120 RPM
     Mud Weight: 10.5 ppg
     Formations: 4 layers
       - Shale: 0-8000 ft (8000 psi UCS)
       - Sandstone: 8000-10500 ft (12000 psi UCS)
       - Limestone: 10500-13000 ft (18000 psi UCS)
       - Target Shale: 13000-15000 ft (10000 psi UCS)

🔌 Connecting MQTT clients...
  ✅ Rig 1 connected

🚀 Starting DR Simulator:
   Rate: 0.5 Hz
   Realtime Factor: 100x

🔩 Rig 1: Depth=5100 ft, ROP=85.3 ft/hr, MSE=7.8 ksi, Formation=Shale
🔩 Rig 1: Depth=5200 ft, ROP=87.1 ft/hr, MSE=7.5 ksi, Formation=Shale
⚠️  Rig 1: HIGH_MSE
🔩 Rig 1: Depth=8100 ft, ROP=62.4 ft/hr, MSE=11.2 ksi, Formation=Sandstone
```

## Use Cases

### 1. MSE Monitoring

Track drilling efficiency across formations:

```yaml
simulation:
  realtime_factor: 50  # Fast drilling

rigs:
  - targetWOB: 30000
    targetRPM: 120
    # MSE will vary with formations
```

### 2. Pressure Management

Monitor ECD in narrow pressure window:

```yaml
formations:
  - name: "Overpressured Shale"
    porePressureGradient: 0.650   # High pore pressure
    fractureGradient: 0.750       # Low fracture pressure
    # Narrow window: requires careful ECD control
```

### 3. Formation Comparison

Compare drilling performance in different rocks:

```yaml
formations:
  - name: "Soft Shale"
    compressiveStrength: 5000
    drillability: 0.8
  - name: "Hard Limestone"
    compressiveStrength: 20000
    drillability: 0.3
```

## Integration with NEXUS Data Mapping

1. **Create Device** for each rig
2. **Get device token** and add to config.yaml
3. **Start simulator** - telemetry flows to Device
4. **Create Asset** (rig digital twin)
5. **Apply Mapping Template**:
   - Select rig Device
   - Select rig Asset
   - Choose DR mapping template
6. **Data flows**: Device → Rule Chain → Asset

### Example Mapping

| Source Key (Device) | Target Key (Asset) |
|---------------------|-------------------|
| weight_on_bit | drilling_wob |
| rate_of_penetration | current_rop |
| mechanical_specific_energy | drilling_mse |
| equivalent_circulating_density | circulating_ecd |
| depth | current_depth |
| formation_name | current_formation |

## Troubleshooting

### MSE always high

- Check formation `compressiveStrength` is realistic
- Verify `targetWOB` and `targetRPM` are appropriate
- Increase `drillability` if needed

### ROP too low

- Increase `targetWOB` or `targetRPM`
- Increase formation `drillability`
- Reduce `compressiveStrength`

### No formation changes

- Verify `depth` is advancing
- Check formation depth ranges don't overlap
- Ensure `topDepth` < `bottomDepth`

### ECD problems not triggering

- Make pressure window narrower:
  - Increase `porePressureGradient`
  - Decrease `fractureGradient`
- Increase `flowRate` to raise ECD

## Performance

- **Memory**: ~15 MB per rig
- **CPU**: <5% on modern hardware
- **Network**: ~900 bytes/message at 0.5 Hz = ~450 B/s per rig

## License

Apache License 2.0 - Copyright © 2016-2026 The Thingsboard Authors
