///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * NEXUS RV (Reservoir) Telemetry Simulator
 */

import { MqttClient, ConfigLoader, SimulatorConfig } from '@simulators/shared';
import { DeclineModel, DeclineParams, DeclineType } from './generators/decline-model';

interface WellConfig {
  deviceToken: string;
  wellName: string;
  declineType: DeclineType;
  initialRate: number;        // bbl/day or MCF/day
  declineRate: number;         // 1/year
  hyperbolicExponent?: number; // Only for hyperbolic
  startDate: string;           // ISO date string
}

interface RvSimulatorConfig extends SimulatorConfig {
  wells: WellConfig[];
}

class RvSimulator {
  private config: RvSimulatorConfig;
  private mqttClients: Map<string, MqttClient> = new Map();
  private declineModels: Map<string, DeclineModel> = new Map();
  private simulationTimer: NodeJS.Timeout | null = null;
  private running = false;

  constructor(config: RvSimulatorConfig) {
    this.config = config;
    this.initializeModels();
  }

  /**
   * Initialize decline models for all wells
   */
  private initializeModels(): void {
    console.log(`\n📊 Initializing ${this.config.wells.length} well decline models...\n`);

    for (const well of this.config.wells) {
      const params: DeclineParams = {
        type: well.declineType,
        qi: well.initialRate,
        Di: well.declineRate,
        b: well.hyperbolicExponent,
        startDate: new Date(well.startDate)
      };

      const model = new DeclineModel(params);
      this.declineModels.set(well.wellName, model);

      const eur = model.getEUR();
      console.log(`  ✅ ${well.wellName}:`);
      console.log(`     Type: ${well.declineType}`);
      console.log(`     qi: ${well.initialRate.toFixed(2)} bbl/day`);
      console.log(`     Di: ${well.declineRate.toFixed(4)} /year`);
      if (well.hyperbolicExponent !== undefined) {
        console.log(`     b: ${well.hyperbolicExponent.toFixed(3)}`);
      }
      console.log(`     EUR: ${eur === Infinity ? '∞' : eur.toFixed(0)} bbl`);
      console.log(`     Start: ${well.startDate}\n`);
    }
  }

  /**
   * Connect all MQTT clients
   */
  async connect(): Promise<void> {
    console.log('🔌 Connecting MQTT clients...\n');

    const brokerUrl = ConfigLoader.getMqttBroker(this.config);

    const connectionPromises = this.config.wells.map(async (well) => {
      const client = new MqttClient({
        brokerUrl,
        accessToken: well.deviceToken,
        reconnectPeriod: this.config.mqtt.reconnect_period_ms,
        clientId: `rv-sim-${well.wellName.replace(/\s+/g, '-').toLowerCase()}`
      });

      await client.connect();
      this.mqttClients.set(well.wellName, client);
      console.log(`  ✅ ${well.wellName} connected`);
    });

    await Promise.all(connectionPromises);
    console.log('\n✅ All MQTT clients connected\n');
  }

  /**
   * Start simulation loop
   */
  start(): void {
    if (this.running) {
      console.warn('⚠️  Simulator already running');
      return;
    }

    this.running = true;
    const intervalMs = 1000 / this.config.simulation.rate_hz;

    console.log(`🚀 Starting RV Simulator:`);
    console.log(`   Rate: ${this.config.simulation.rate_hz} Hz`);
    console.log(`   Interval: ${intervalMs} ms`);
    console.log(`   Realtime Factor: ${this.config.simulation.realtime_factor}x\n`);

    let simulatedTime = new Date();

    this.simulationTimer = setInterval(() => {
      // Advance simulated time
      const deltaMs = intervalMs * this.config.simulation.realtime_factor;
      simulatedTime = new Date(simulatedTime.getTime() + deltaMs);

      // Generate and publish telemetry for all wells
      for (const well of this.config.wells) {
        this.publishWellData(well, simulatedTime);
      }
    }, intervalMs);

    console.log('✅ Simulator started. Press Ctrl+C to stop.\n');
  }

  /**
   * Publish telemetry for a single well
   */
  private publishWellData(well: WellConfig, currentTime: Date): void {
    const model = this.declineModels.get(well.wellName);
    const client = this.mqttClients.get(well.wellName);

    if (!model || !client) {
      console.error(`❌ Model or client not found for ${well.wellName}`);
      return;
    }

    if (!client.isConnected()) {
      console.warn(`⚠️  ${well.wellName} not connected, skipping...`);
      return;
    }

    // Generate production data
    const data = model.generateProductionData(currentTime);

    // Build telemetry payload
    const telemetry = {
      // Production metrics
      oil_rate: parseFloat(data.productionRate.toFixed(2)),          // bbl/day
      cumulative_oil: parseFloat(data.cumulativeProduction.toFixed(2)), // bbl
      water_cut: parseFloat((data.waterCut * 100).toFixed(2)),       // percentage

      // Reservoir metrics
      reservoir_pressure: parseFloat(data.reservePressure.toFixed(2)), // psi

      // Calculated metrics
      liquid_rate: parseFloat((data.productionRate / (1 - data.waterCut)).toFixed(2)), // bbl/day
      water_rate: parseFloat((data.productionRate * data.waterCut / (1 - data.waterCut)).toFixed(2)), // bbl/day

      // Time tracking
      days_on_production: data.daysSinceStart,
      timestamp: currentTime.toISOString()
    };

    // Publish to ThingsBoard
    client.publishTelemetry(telemetry);

    // Log periodically (every 10 data points)
    if (data.daysSinceStart % 10 === 0) {
      console.log(`📈 ${well.wellName} [Day ${data.daysSinceStart}]:` +
                  ` Oil=${telemetry.oil_rate} bbl/day,` +
                  ` Cum=${telemetry.cumulative_oil} bbl,` +
                  ` WC=${telemetry.water_cut}%,` +
                  ` P=${telemetry.reservoir_pressure} psi`);
    }
  }

  /**
   * Stop simulation
   */
  stop(): void {
    if (!this.running) {
      return;
    }

    console.log('\n🛑 Stopping simulator...');

    if (this.simulationTimer) {
      clearInterval(this.simulationTimer);
      this.simulationTimer = null;
    }

    this.running = false;
    console.log('✅ Simulator stopped');
  }

  /**
   * Disconnect all MQTT clients
   */
  async disconnect(): Promise<void> {
    console.log('🔌 Disconnecting MQTT clients...');

    const disconnectPromises = Array.from(this.mqttClients.values()).map(
      client => client.disconnect()
    );

    await Promise.all(disconnectPromises);
    this.mqttClients.clear();
    console.log('✅ All clients disconnected');
  }
}

// ========================
// Main Entry Point
// ========================

async function main() {
  console.log('═══════════════════════════════════════════════════');
  console.log('   NEXUS RV Simulator - Reservoir Production');
  console.log('═══════════════════════════════════════════════════\n');

  try {
    // Load configuration
    const config = ConfigLoader.load() as RvSimulatorConfig;

    // Validate configuration
    if (!config.wells || config.wells.length === 0) {
      throw new Error('No wells configured in config file');
    }

    // Create and start simulator
    const simulator = new RvSimulator(config);
    await simulator.connect();
    simulator.start();

    // Handle graceful shutdown
    const shutdown = async () => {
      console.log('\n\n📋 Shutdown signal received');
      simulator.stop();
      await simulator.disconnect();
      console.log('👋 Goodbye!\n');
      process.exit(0);
    };

    process.on('SIGINT', shutdown);
    process.on('SIGTERM', shutdown);

  } catch (error) {
    console.error('❌ Fatal error:', error);
    process.exit(1);
  }
}

// Start the simulator
if (require.main === module) {
  main();
}

export { RvSimulator, RvSimulatorConfig, WellConfig };
