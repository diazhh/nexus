# NEXUS RV Simulator - Reservoir Production

Telemetry simulator for oil and gas reservoir production data using Arps decline curve analysis.

## Features

- **Arps Decline Curves**: Exponential, Hyperbolic, and Harmonic models
- **Realistic Production Data**:
  - Oil production rate (bbl/day)
  - Cumulative production (bbl)
  - Water cut percentage
  - Reservoir pressure (psi)
  - Liquid rate and water rate
- **Multiple Wells**: Simulate multiple wells with different decline characteristics
- **Time Acceleration**: Run simulations faster than real-time
- **ThingsBoard Integration**: MQTT v1 protocol support

## Quick Start

### 1. Install Dependencies

From the simulators root directory:

```bash
npm install
```

### 2. Configure Wells

Copy the example configuration:

```bash
cd rv-simulator
cp config.example.yaml config.yaml
```

Edit `config.yaml` with your well parameters and ThingsBoard device tokens.

### 3. Create ThingsBoard Devices

For each well in your config:

1. Go to ThingsBoard → Devices → Add Device
2. Name it (e.g., "Eagle Ford 1H")
3. Copy the access token from Credentials tab
4. Paste into `config.yaml` as `deviceToken`

### 4. Run Simulator

Development mode (with auto-reload):
```bash
npm run dev
```

Production mode:
```bash
npm run build
npm start
```

## Configuration

### MQTT Settings

```yaml
mqtt:
  broker_url: "tcp://localhost:1883"  # ThingsBoard MQTT endpoint
  reconnect_period_ms: 5000           # Auto-reconnect interval
```

### Simulation Settings

```yaml
simulation:
  rate_hz: 0.1              # Data frequency (0.1 = every 10 seconds)
  realtime_factor: 365.0    # Time acceleration (365 = 1 year per day)
```

### Well Configuration

```yaml
wells:
  - deviceToken: "YOUR_DEVICE_TOKEN"
    wellName: "Well Name"
    declineType: "exponential"  # exponential | hyperbolic | harmonic
    initialRate: 1200.0         # Initial production (bbl/day)
    declineRate: 0.35           # Annual decline rate (0.35 = 35%)
    hyperbolicExponent: 0.6     # Only for hyperbolic type (0-1)
    startDate: "2024-01-01T00:00:00Z"
```

## Decline Curve Models

### Exponential Decline

**Equation**: `q(t) = qi * exp(-Di * t)`

**Characteristics**:
- Steepest decline
- Finite EUR (Estimated Ultimate Recovery)
- Typical for tight oil/gas and unconventional wells

**Parameters**:
- `qi`: 500-2000 bbl/day
- `Di`: 0.30-0.60 /year (30-60% annual decline)

### Hyperbolic Decline

**Equation**: `q(t) = qi / (1 + b*Di*t)^(1/b)`

**Characteristics**:
- Most common in conventional reservoirs
- Moderate decline rate
- `b=0` is exponential, `b=1` is harmonic

**Parameters**:
- `qi`: 300-1500 bbl/day
- `Di`: 0.20-0.50 /year
- `b`: 0.3-0.8 (typical: 0.5-0.7)

### Harmonic Decline

**Equation**: `q(t) = qi / (1 + Di*t)`

**Characteristics**:
- Slowest decline
- Infinite theoretical EUR
- Typical for pressure-maintained reservoirs

**Parameters**:
- `qi`: 200-1000 bbl/day
- `Di`: 0.15-0.35 /year

## Generated Telemetry

Each data point includes:

| Key | Description | Units |
|-----|-------------|-------|
| `oil_rate` | Current oil production rate | bbl/day |
| `cumulative_oil` | Total oil produced | bbl |
| `water_cut` | Water fraction | % |
| `reservoir_pressure` | Formation pressure | psi |
| `liquid_rate` | Total fluid rate (oil + water) | bbl/day |
| `water_rate` | Water production rate | bbl/day |
| `days_on_production` | Days since start | days |
| `timestamp` | Simulation time | ISO 8601 |

## Integration with NEXUS Data Mapping

1. **Create Device** in ThingsBoard (represents physical well)
2. **Get device token** and add to `config.yaml`
3. **Start simulator** - telemetry flows to Device
4. **Create Asset** in ThingsBoard (digital twin)
5. **Apply Mapping Template** via Data Sources UI:
   - Select source Device
   - Select target Asset
   - Choose RV mapping template
6. **Rules auto-create** to map `oil_rate` → `production_rate`, etc.
7. **Data flows**: Device → Rule Chain → Asset (digital twin)

## Example Output

```
═══════════════════════════════════════════════════
   NEXUS RV Simulator - Reservoir Production
═══════════════════════════════════════════════════

📊 Initializing 4 well decline models...

  ✅ Eagle Ford 1H:
     Type: exponential
     qi: 1200.00 bbl/day
     Di: 0.3500 /year
     EUR: 1251429 bbl
     Start: 2024-01-01T00:00:00Z

🔌 Connecting MQTT clients...
  ✅ Eagle Ford 1H connected

🚀 Starting RV Simulator:
   Rate: 0.1 Hz
   Interval: 10000 ms
   Realtime Factor: 365x

📈 Eagle Ford 1H [Day 10]: Oil=1156.32 bbl/day, Cum=11650 bbl, WC=0.00%, P=3988 psi
📈 Eagle Ford 1H [Day 20]: Oil=1114.85 bbl/day, Cum=22950 bbl, WC=0.00%, P=3976 psi
```

## Performance

- **Memory**: ~50MB per well
- **CPU**: <5% on modern hardware
- **Network**: ~500 bytes/message at 0.1 Hz = ~50 B/s per well

## Troubleshooting

**Connection errors**:
- Verify ThingsBoard MQTT is running on port 1883
- Check device tokens are correct
- Ensure firewall allows MQTT traffic

**No data in ThingsBoard**:
- Verify device exists and is active
- Check Latest Telemetry tab in Device
- Review simulator console for errors

**Unrealistic data**:
- Adjust `realtime_factor` for slower simulation
- Verify `startDate` is appropriate
- Check decline parameters match well type

## License

Apache License 2.0 - Copyright © 2016-2026 The Thingsboard Authors
