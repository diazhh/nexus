# NEXUS Telemetry Simulators

IoT telemetry simulators for petroleum operations, designed to work with ThingsBoard and NEXUS Data Mapping system.

## Overview

The NEXUS simulators generate realistic telemetry data for different petroleum modules:

- **RV (Reservoir)** - Production decline curves, water cut, pressure ✅ **Implemented**
- **CT (Coiled Tubing)** - Fatigue analysis, job states, depth/tension 🚧 Planned
- **DR (Drilling)** - MSE, ECD, correlated drilling parameters 🚧 Planned

## Architecture

```
┌─────────────┐
│  Simulators │
│  (Node.js)  │
└──────┬──────┘
       │ MQTT v1 Protocol
       │ v1/devices/me/telemetry
       ▼
┌─────────────┐        ┌──────────────┐
│   Device    │───────▶│    Asset     │
│ (Data Source│  Rule  │ (Digital Twin│
└─────────────┘  Chain └──────────────┘
       ▲                      ▲
       │                      │
   ThingsBoard        DataSourceConfig
                      + MappingRules
```

### Data Flow

1. **Simulator** generates telemetry → publishes to MQTT
2. **Device** receives telemetry via MQTT
3. **Rule Chain** (TbNexusDataDistributionNode) processes data
4. **DataMappingRules** transform keys (sourceKey → targetKey)
5. **Asset** receives mapped telemetry as digital twin

## Project Structure

```
simulators/
├── shared/                 # Shared utilities
│   ├── src/
│   │   ├── mqtt-client.ts     # ThingsBoard MQTT client
│   │   ├── noise-generator.ts # Statistical noise generation
│   │   ├── event-scheduler.ts # Timed event system
│   │   └── config-loader.ts   # YAML configuration
│   └── package.json
│
├── rv-simulator/          # Reservoir module ✅
│   ├── src/
│   │   ├── generators/
│   │   │   └── decline-model.ts  # Arps decline curves
│   │   └── index.ts              # Main simulator
│   ├── config.example.yaml
│   └── package.json
│
├── ct-simulator/          # Coiled Tubing 🚧
├── dr-simulator/          # Drilling 🚧
│
├── Dockerfile
├── docker-compose.yaml
└── package.json          # NPM workspace root
```

## Quick Start

### Prerequisites

- Node.js 20+ or Docker
- ThingsBoard instance with MQTT enabled
- Basic understanding of MQTT and ThingsBoard

### Option 1: Run with Docker (Recommended)

```bash
# 1. Build images
docker-compose build

# 2. Configure your simulator (copy and edit config.yaml)
cd rv-simulator
cp config.example.yaml config.yaml
# Edit config.yaml with your device tokens

# 3. Set MQTT broker (optional - default uses host.docker.internal)
cp .env.example .env
# Edit .env if needed

# 4. Start simulator
docker-compose up rv-simulator

# Or run in background
docker-compose up -d rv-simulator

# 5. View logs
docker-compose logs -f rv-simulator
```

### Option 2: Run with Node.js

```bash
# 1. Install dependencies
npm install

# 2. Configure simulator
cd rv-simulator
cp config.example.yaml config.yaml
# Edit config.yaml with your device tokens

# 3. Build
npm run build

# 4. Run
npm start

# Or development mode (with auto-reload)
npm run dev
```

## Configuration

### Environment Variables

Create a `.env` file:

```bash
# MQTT Broker URL
MQTT_BROKER=tcp://your-thingsboard-host:1883

# Or for local testing
MQTT_BROKER=tcp://localhost:1883
```

### Simulator Config (YAML)

Each simulator has a `config.yaml` file:

```yaml
mqtt:
  broker_url: "tcp://localhost:1883"
  reconnect_period_ms: 5000

simulation:
  rate_hz: 0.1              # Data frequency (Hz)
  realtime_factor: 365.0    # Time acceleration

# Simulator-specific config...
# See individual simulator READMEs
```

## ThingsBoard Integration

### Setup Steps

1. **Create Devices** (one per simulated entity)
   - Go to ThingsBoard → Devices → Add Device
   - Example: "Eagle Ford 1H" for a well
   - Copy access token from Credentials tab

2. **Configure Simulator**
   - Add device token to `config.yaml`
   - Set broker URL to your ThingsBoard MQTT endpoint

3. **Start Simulator**
   - Telemetry flows to Device automatically

4. **Create Assets** (digital twins)
   - Go to ThingsBoard → Assets → Add Asset
   - Example: "Eagle Ford Well 1H DT" for digital twin

5. **Apply Mapping Template** (via NEXUS UI)
   - Navigate to Data Mapping → Data Sources
   - Click "Apply Template"
   - Select source Device and target Asset
   - Choose appropriate module template (RV, CT, or DR)

6. **Verify Data Flow**
   - Device receives raw telemetry
   - Asset receives mapped/transformed telemetry
   - Check Latest Telemetry tabs on both

### Data Mapping Example

```
Device Telemetry          Asset Telemetry
─────────────────         ───────────────
oil_rate         ────▶    production_rate
cumulative_oil   ────▶    cumulative_production
water_cut        ────▶    water_cut_percentage
reservoir_pressure ───▶    formation_pressure
```

## Available Simulators

### ✅ RV Simulator - Reservoir Production

Simulates oil/gas well production using Arps decline curves.

**Features**:
- Exponential, hyperbolic, and harmonic decline models
- Production rate, cumulative production
- Water cut progression
- Reservoir pressure decline

**See**: [rv-simulator/README.md](rv-simulator/README.md)

### 🚧 CT Simulator - Coiled Tubing (Planned)

Will simulate coiled tubing operations.

**Planned Features**:
- Palmgren-Miner fatigue calculation
- Job state machine (RIH, POOH, Work, Idle)
- Depth, tension, pressure correlation
- Cumulative damage tracking

### 🚧 DR Simulator - Drilling (Planned)

Will simulate drilling operations.

**Planned Features**:
- MSE (Mechanical Specific Energy) calculation
- ECD (Equivalent Circulating Density)
- Correlated parameters (WOB, ROP, RPM, torque)
- Formation change simulation

## Development

### Adding a New Simulator

1. Create simulator directory:
   ```bash
   mkdir my-simulator
   cd my-simulator
   ```

2. Create `package.json`:
   ```json
   {
     "name": "@simulators/my-simulator",
     "dependencies": {
       "@simulators/shared": "workspace:*"
     }
   }
   ```

3. Add to workspace in root `package.json`:
   ```json
   {
     "workspaces": [
       "shared",
       "rv-simulator",
       "my-simulator"
     ]
   }
   ```

4. Implement simulator using shared modules:
   ```typescript
   import { MqttClient, NoiseGenerator } from '@simulators/shared';
   ```

5. Add to `Dockerfile` and `docker-compose.yaml`

### Building

```bash
# Build all
npm run build

# Build specific simulator
npm run build -w rv-simulator

# Clean all
npm run clean
```

### Testing Locally

Use the included Mosquitto broker:

```bash
# Uncomment mqtt-broker service in docker-compose.yaml

# Start broker and simulator
docker-compose up mqtt-broker rv-simulator

# Broker will be available at:
# - MQTT: localhost:1883
# - WebSocket: localhost:9001
```

## Troubleshooting

### Simulator won't connect

- Verify MQTT broker is running
- Check broker URL in config.yaml or .env
- Ensure firewall allows port 1883
- Verify device tokens are correct

### No data in ThingsBoard

- Check Device → Latest Telemetry tab
- Verify simulator console shows "published" messages
- Check ThingsBoard rule chain is active
- Ensure device credentials are correct

### Data in Device but not Asset

- Verify DataSourceConfig exists (Data Sources UI)
- Check MappingRules are created
- Ensure rule chain includes TbNexusDataDistributionNode
- Check rule chain logs for errors

### Docker build fails

- Ensure Node.js 20+ base image is available
- Check Dockerfile uncomments match implemented simulators
- Verify all package.json files are valid
- Clear Docker cache: `docker-compose build --no-cache`

## Performance

### Resource Usage (per simulator)

- **Memory**: ~50MB
- **CPU**: <5% on modern hardware
- **Network**: ~500 bytes/message
  - At 0.1 Hz: ~50 B/s
  - At 1 Hz: ~500 B/s

### Scaling

- Run multiple simulator instances
- Use separate containers per simulator type
- Configure different rate_hz for different modules
- Use realtime_factor to compress simulation time

## License

Apache License 2.0

Copyright © 2016-2026 The Thingsboard Authors

## Support

For issues, questions, or contributions:

1. Check simulator-specific READMEs
2. Review ThingsBoard documentation
3. Check NEXUS Data Mapping documentation
4. Open an issue with:
   - Simulator type and version
   - Configuration (redact tokens)
   - Error messages or logs
   - Expected vs actual behavior
