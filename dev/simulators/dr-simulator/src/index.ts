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
 * NEXUS DR (Drilling) Telemetry Simulator
 */

import { MqttClient, ConfigLoader, SimulatorConfig } from '@simulators/shared';
import { DrillingModel, FormationProperties, BitGeometry, WellGeometry } from './models/drilling-model';

interface DrillingRigConfig {
  deviceToken: string;
  rigName: string;

  // Bit geometry
  bitDiameter: number;      // inches
  bitNozzles: number;
  bitTFA: number;           // Total Flow Area (in²)

  // Well geometry
  holeSize: number;         // inches
  drillpipeOD: number;      // inches
  drillpipeID: number;      // inches

  // Operating parameters
  targetWOB: number;        // lbf
  targetRPM: number;        // RPM
  targetFlowRate: number;   // gpm
  mudWeight: number;        // ppg

  // Formations to drill through
  formations: FormationProperties[];

  // Starting depth
  startDepth: number;       // ft
}

interface DrSimulatorConfig extends SimulatorConfig {
  rigs: DrillingRigConfig[];
}

class DrSimulator {
  private config: DrSimulatorConfig;
  private mqttClients: Map<string, MqttClient> = new Map();
  private drillingModels: Map<string, DrillingModel> = new Map();
  private simulationTimer: NodeJS.Timeout | null = null;
  private running = false;
  private lastUpdateTime = new Date();

  constructor(config: DrSimulatorConfig) {
    this.config = config;
    this.initializeRigs();
  }

  /**
   * Initialize drilling models for all rigs
   */
  private initializeRigs(): void {
    console.log(`\n⚙️  Initializing ${this.config.rigs.length} drilling rigs...\n`);

    for (const rig of this.config.rigs) {
      // Create bit geometry
      const bit: BitGeometry = {
        diameter: rig.bitDiameter,
        area: 0, // Will be calculated by model
        nozzles: rig.bitNozzles,
        tfa: rig.bitTFA
      };

      // Create well geometry
      const well: WellGeometry = {
        holeSize: rig.holeSize,
        drillpipeOD: rig.drillpipeOD,
        drillpipeID: rig.drillpipeID,
        annularArea: 0 // Will be calculated by model
      };

      // Initialize drilling model
      const model = new DrillingModel(bit, well, rig.formations);
      model.setDepth(rig.startDepth);
      this.drillingModels.set(rig.rigName, model);

      console.log(`  ✅ ${rig.rigName}:`);
      console.log(`     Bit: ${rig.bitDiameter}" PDC, ${rig.bitNozzles} nozzles`);
      console.log(`     Hole: ${rig.holeSize}" x ${rig.drillpipeOD}" DP`);
      console.log(`     Target: ${rig.targetWOB} lbf WOB, ${rig.targetRPM} RPM`);
      console.log(`     Mud Weight: ${rig.mudWeight} ppg`);
      console.log(`     Formations: ${rig.formations.length} layers`);
      rig.formations.forEach(f => {
        console.log(`       - ${f.name}: ${f.topDepth}-${f.bottomDepth} ft (${f.compressiveStrength} psi UCS)`);
      });
      console.log();
    }
  }

  /**
   * Connect all MQTT clients
   */
  async connect(): Promise<void> {
    console.log('🔌 Connecting MQTT clients...\n');

    const brokerUrl = ConfigLoader.getMqttBroker(this.config);

    const connectionPromises = this.config.rigs.map(async (rig) => {
      const client = new MqttClient({
        brokerUrl,
        accessToken: rig.deviceToken,
        reconnectPeriod: this.config.mqtt.reconnect_period_ms,
        clientId: `dr-sim-${rig.rigName.replace(/\s+/g, '-').toLowerCase()}`
      });

      await client.connect();
      this.mqttClients.set(rig.rigName, client);
      console.log(`  ✅ ${rig.rigName} connected`);
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
    this.lastUpdateTime = new Date();
    const intervalMs = 1000 / this.config.simulation.rate_hz;

    console.log(`🚀 Starting DR Simulator:`);
    console.log(`   Rate: ${this.config.simulation.rate_hz} Hz`);
    console.log(`   Interval: ${intervalMs} ms`);
    console.log(`   Realtime Factor: ${this.config.simulation.realtime_factor}x\n`);

    this.simulationTimer = setInterval(() => {
      const now = new Date();
      const realDeltaMs = now.getTime() - this.lastUpdateTime.getTime();
      this.lastUpdateTime = now;

      // Apply realtime factor
      const simulatedDeltaSeconds = (realDeltaMs / 1000) * this.config.simulation.realtime_factor;

      // Update and publish for all rigs
      for (const rig of this.config.rigs) {
        this.updateRig(rig, simulatedDeltaSeconds);
      }
    }, intervalMs);

    console.log('✅ Simulator started. Press Ctrl+C to stop.\n');
  }

  /**
   * Update single drilling rig and publish telemetry
   */
  private updateRig(rig: DrillingRigConfig, deltaSeconds: number): void {
    const model = this.drillingModels.get(rig.rigName);
    const client = this.mqttClients.get(rig.rigName);

    if (!model || !client) {
      console.error(`❌ Missing components for ${rig.rigName}`);
      return;
    }

    if (!client.isConnected()) {
      console.warn(`⚠️  ${rig.rigName} not connected, skipping...`);
      return;
    }

    // Add some parameter variation (±10%)
    const wob = rig.targetWOB * (0.9 + Math.random() * 0.2);
    const rpm = rig.targetRPM * (0.95 + Math.random() * 0.1);
    const flowRate = rig.targetFlowRate * (0.95 + Math.random() * 0.1);

    // Update drilling model
    const params = model.update(wob, rpm, flowRate, rig.mudWeight, deltaSeconds);

    // Detect problems
    const problems = model.detectProblems(params);

    // Build telemetry payload
    const telemetry = {
      // Controllable parameters
      weight_on_bit: parseFloat(params.wob.toFixed(2)),              // lbf
      rotary_speed: parseFloat(params.rpm.toFixed(2)),               // RPM
      flow_rate: parseFloat(params.flowRate.toFixed(2)),             // gpm
      mud_weight: parseFloat(params.mudWeight.toFixed(2)),           // ppg

      // Measured parameters
      rate_of_penetration: parseFloat(params.rop.toFixed(2)),        // ft/hr
      torque: parseFloat(params.torque.toFixed(2)),                  // ft-lbf
      standpipe_pressure: parseFloat(params.standpipePressure.toFixed(2)), // psi
      hook_load: parseFloat(params.hookLoad.toFixed(2)),             // lbf

      // Calculated metrics
      mechanical_specific_energy: parseFloat(params.mse.toFixed(3)), // ksi
      equivalent_circulating_density: parseFloat(params.ecd.toFixed(2)), // ppg

      // Depth and formation
      depth: parseFloat(params.depth.toFixed(2)),                    // ft
      formation_name: params.formation.name,
      formation_ucs: parseFloat(params.formation.compressiveStrength.toFixed(0)), // psi
      formation_drillability: parseFloat(params.formation.drillability.toFixed(3)),

      // Pressure window
      pore_pressure_gradient: parseFloat(params.formation.porePressureGradient.toFixed(3)), // psi/ft
      fracture_gradient: parseFloat(params.formation.fractureGradient.toFixed(3)),          // psi/ft

      // Problems
      has_problems: problems.length > 0,
      problems: problems.join(','),

      // Timestamp
      timestamp: new Date().toISOString()
    };

    // Publish to ThingsBoard
    client.publishTelemetry(telemetry);

    // Log periodically
    const shouldLog = Math.floor(params.depth) % 100 < 1; // Every ~100 ft

    if (shouldLog) {
      this.logRigStatus(rig.rigName, params.depth, params.rop, params.mse,
                       params.formation.name, problems);
    }

    // Warn on problems
    if (problems.length > 0 && Math.random() < 0.1) { // 10% chance to log problem
      console.warn(`⚠️  ${rig.rigName}: ${problems.join(', ')}`);
    }
  }

  /**
   * Log rig status
   */
  private logRigStatus(
    rigName: string,
    depth: number,
    rop: number,
    mse: number,
    formation: string,
    problems: string[]
  ): void {
    const problemStr = problems.length > 0 ? ` ⚠️ ${problems.join(',')}` : '';
    console.log(
      `🔩 ${rigName}:` +
      ` Depth=${depth.toFixed(0)} ft,` +
      ` ROP=${rop.toFixed(1)} ft/hr,` +
      ` MSE=${mse.toFixed(1)} ksi,` +
      ` Formation=${formation}${problemStr}`
    );
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
  console.log('   NEXUS DR Simulator - Drilling Operations');
  console.log('═══════════════════════════════════════════════════\n');

  try {
    // Load configuration
    const config = ConfigLoader.load() as DrSimulatorConfig;

    // Validate configuration
    if (!config.rigs || config.rigs.length === 0) {
      throw new Error('No drilling rigs configured in config file');
    }

    // Create and start simulator
    const simulator = new DrSimulator(config);
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

export { DrSimulator, DrSimulatorConfig, DrillingRigConfig };
