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
 * NEXUS CT (Coiled Tubing) Telemetry Simulator
 */

import { MqttClient, ConfigLoader, SimulatorConfig } from '@simulators/shared';
import { FatigueModel, MaterialProperties } from './models/fatigue-model';
import { JobStateMachine, JobPhaseConfig, JobPhase } from './models/job-state-machine';

interface CtUnitConfig {
  deviceToken: string;
  unitName: string;
  material: 'CT80' | 'CT90' | 'CT100' | 'CT110';
  outerDiameter: number;      // inches
  wallThickness: number;      // inches
  targetDepth: number;        // ft
  pipeWeight: number;         // lbf/ft
  workPressure: number;       // psi
  workFlowRate: number;       // bbl/min
  rihSpeed: number;           // ft/min
  poohSpeed: number;          // ft/min
  workDuration: number;       // seconds at depth
}

interface CtSimulatorConfig extends SimulatorConfig {
  units: CtUnitConfig[];
}

class CtSimulator {
  private config: CtSimulatorConfig;
  private mqttClients: Map<string, MqttClient> = new Map();
  private stateMachines: Map<string, JobStateMachine> = new Map();
  private fatigueModels: Map<string, FatigueModel> = new Map();
  private simulationTimer: NodeJS.Timeout | null = null;
  private running = false;
  private lastUpdateTime = new Date();

  constructor(config: CtSimulatorConfig) {
    this.config = config;
    this.initializeUnits();
  }

  /**
   * Initialize state machines and fatigue models for all units
   */
  private initializeUnits(): void {
    console.log(`\n🔧 Initializing ${this.config.units.length} CT units...\n`);

    for (const unit of this.config.units) {
      // Create job phase config
      const jobConfig: JobPhaseConfig = {
        targetDepth: unit.targetDepth,
        pipeWeight: unit.pipeWeight,
        workPressure: unit.workPressure,
        workFlowRate: unit.workFlowRate,
        rihSpeed: unit.rihSpeed,
        poohSpeed: unit.poohSpeed,
        workDuration: unit.workDuration
      };

      // Initialize state machine
      const stateMachine = new JobStateMachine(jobConfig);
      this.stateMachines.set(unit.unitName, stateMachine);

      // Initialize fatigue model
      const fatigueModel = new FatigueModel(unit.material);
      this.fatigueModels.set(unit.unitName, fatigueModel);

      const material = fatigueModel.getMaterial();
      console.log(`  ✅ ${unit.unitName}:`);
      console.log(`     Material: ${material.grade} (${material.yieldStrength / 1000} ksi)`);
      console.log(`     OD: ${unit.outerDiameter}" x ${unit.wallThickness}" WT`);
      console.log(`     Target Depth: ${unit.targetDepth} ft`);
      console.log(`     RIH/POOH Speed: ${unit.rihSpeed}/${unit.poohSpeed} ft/min`);
      console.log(`     Work Time: ${unit.workDuration / 60} min\n`);
    }
  }

  /**
   * Connect all MQTT clients
   */
  async connect(): Promise<void> {
    console.log('🔌 Connecting MQTT clients...\n');

    const brokerUrl = ConfigLoader.getMqttBroker(this.config);

    const connectionPromises = this.config.units.map(async (unit) => {
      const client = new MqttClient({
        brokerUrl,
        accessToken: unit.deviceToken,
        reconnectPeriod: this.config.mqtt.reconnect_period_ms,
        clientId: `ct-sim-${unit.unitName.replace(/\s+/g, '-').toLowerCase()}`
      });

      await client.connect();
      this.mqttClients.set(unit.unitName, client);
      console.log(`  ✅ ${unit.unitName} connected`);
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

    console.log(`🚀 Starting CT Simulator:`);
    console.log(`   Rate: ${this.config.simulation.rate_hz} Hz`);
    console.log(`   Interval: ${intervalMs} ms`);
    console.log(`   Realtime Factor: ${this.config.simulation.realtime_factor}x\n`);

    this.simulationTimer = setInterval(() => {
      const now = new Date();
      const realDeltaMs = now.getTime() - this.lastUpdateTime.getTime();
      this.lastUpdateTime = now;

      // Apply realtime factor
      const simulatedDeltaSeconds = (realDeltaMs / 1000) * this.config.simulation.realtime_factor;

      // Update and publish for all units
      for (const unit of this.config.units) {
        this.updateUnit(unit, simulatedDeltaSeconds);
      }
    }, intervalMs);

    console.log('✅ Simulator started. Press Ctrl+C to stop.\n');
  }

  /**
   * Update single CT unit and publish telemetry
   */
  private updateUnit(unit: CtUnitConfig, deltaSeconds: number): void {
    const stateMachine = this.stateMachines.get(unit.unitName);
    const fatigueModel = this.fatigueModels.get(unit.unitName);
    const client = this.mqttClients.get(unit.unitName);

    if (!stateMachine || !fatigueModel || !client) {
      console.error(`❌ Missing components for ${unit.unitName}`);
      return;
    }

    if (!client.isConnected()) {
      console.warn(`⚠️  ${unit.unitName} not connected, skipping...`);
      return;
    }

    // Update job state
    const jobState = stateMachine.update(deltaSeconds);

    // Calculate stress
    const stress = stateMachine.getStress(unit.outerDiameter, unit.wallThickness);

    // Update fatigue (add cycles based on stress changes)
    // Simplified: One cycle per trip (RIH + Work + POOH)
    const estimatedCycles = stateMachine.getEstimatedCycles();
    if (estimatedCycles > fatigueModel.getState().totalCycles) {
      // New cycle detected
      const cyclesAdded = estimatedCycles - fatigueModel.getState().totalCycles;
      fatigueModel.addCycles(stress, cyclesAdded, new Date());
    }

    const fatigueState = fatigueModel.getState();

    // Build telemetry payload
    const telemetry = {
      // Job state
      job_phase: jobState.phase,
      depth: parseFloat(jobState.depth.toFixed(2)),                    // ft
      tension: parseFloat(jobState.tension.toFixed(2)),                // lbf
      surface_pressure: parseFloat(jobState.pressure.toFixed(2)),      // psi
      flow_rate: parseFloat(jobState.flowRate.toFixed(3)),             // bbl/min

      // Calculated stress
      axial_stress: parseFloat(stress.toFixed(2)),                     // psi

      // Fatigue metrics
      total_cycles: fatigueState.totalCycles,
      cumulative_damage: parseFloat(fatigueState.cumulativeDamage.toFixed(6)),
      remaining_life: parseFloat(fatigueState.remainingLife.toFixed(2)), // %
      predicted_cycles_to_failure: Math.round(fatigueState.predictedFailureCycles),

      // Time tracking
      phase_elapsed_time: Math.round(jobState.phaseElapsedSeconds),    // s
      total_run_time: Math.round(jobState.totalRunTime),               // s
      timestamp: new Date().toISOString()
    };

    // Publish to ThingsBoard
    client.publishTelemetry(telemetry);

    // Log periodically and on phase changes
    const shouldLog =
      jobState.phaseElapsedSeconds < deltaSeconds * 2 || // Just changed phase
      Math.floor(jobState.phaseElapsedSeconds) % 30 === 0; // Every 30s

    if (shouldLog) {
      this.logUnitStatus(unit.unitName, jobState.phase, jobState.depth,
                        jobState.tension, fatigueState.remainingLife);
    }

    // Warn if fatigue critical
    if (fatigueState.remainingLife < 20 && fatigueState.remainingLife > 19.5) {
      console.warn(`⚠️  ${unit.unitName}: FATIGUE WARNING - ${fatigueState.remainingLife.toFixed(1)}% life remaining`);
    }

    if (fatigueModel.hasFailed()) {
      console.error(`❌ ${unit.unitName}: FATIGUE FAILURE - Replace CT string!`);
    }
  }

  /**
   * Log unit status
   */
  private logUnitStatus(unitName: string, phase: JobPhase, depth: number,
                       tension: number, remainingLife: number): void {
    const phaseEmojis: Record<JobPhase, string> = {
      'Idle': '⏸️ ',
      'RIH': '⬇️ ',
      'Work': '🔧',
      'POOH': '⬆️ '
    };

    console.log(
      `${phaseEmojis[phase]} ${unitName} [${phase}]:` +
      ` Depth=${depth.toFixed(0)} ft,` +
      ` Tension=${tension.toFixed(0)} lbf,` +
      ` Life=${remainingLife.toFixed(1)}%`
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
  console.log('   NEXUS CT Simulator - Coiled Tubing Operations');
  console.log('═══════════════════════════════════════════════════\n');

  try {
    // Load configuration
    const config = ConfigLoader.load() as CtSimulatorConfig;

    // Validate configuration
    if (!config.units || config.units.length === 0) {
      throw new Error('No CT units configured in config file');
    }

    // Create and start simulator
    const simulator = new CtSimulator(config);
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

export { CtSimulator, CtSimulatorConfig, CtUnitConfig };
