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
 * Job State Machine for Coiled Tubing Operations
 */

import { NoiseGenerator } from '@simulators/shared';

export type JobPhase = 'RIH' | 'POOH' | 'Work' | 'Idle';

export interface JobState {
  phase: JobPhase;
  depth: number;              // Current depth (ft)
  tension: number;            // Current tension (lbf)
  pressure: number;           // Surface pressure (psi)
  flowRate: number;           // Pump rate (bbl/min)
  phaseStartTime: Date;       // When current phase started
  phaseElapsedSeconds: number; // Seconds in current phase
  totalRunTime: number;       // Total job time (seconds)
}

export interface JobPhaseConfig {
  targetDepth: number;        // Maximum depth (ft)
  pipeWeight: number;         // Coiled tubing weight (lbf/ft)
  workPressure: number;       // Working pressure during job (psi)
  workFlowRate: number;       // Flow rate during work (bbl/min)
  rihSpeed: number;           // Run-in-hole speed (ft/min)
  poohSpeed: number;          // Pull-out speed (ft/min)
  workDuration: number;       // Time to spend working (seconds)
}

/**
 * State machine for coiled tubing job phases
 */
export class JobStateMachine {
  private noise = new NoiseGenerator();
  private config: JobPhaseConfig;
  private currentState: JobState;

  constructor(config: JobPhaseConfig) {
    this.config = config;
    this.currentState = {
      phase: 'Idle',
      depth: 0,
      tension: 0,
      pressure: 0,
      flowRate: 0,
      phaseStartTime: new Date(),
      phaseElapsedSeconds: 0,
      totalRunTime: 0
    };
  }

  /**
   * Update state based on elapsed time
   *
   * @param deltaSeconds - Time elapsed since last update
   * @returns Current job state
   */
  update(deltaSeconds: number): JobState {
    this.currentState.phaseElapsedSeconds += deltaSeconds;
    this.currentState.totalRunTime += deltaSeconds;

    // Update state based on current phase
    switch (this.currentState.phase) {
      case 'Idle':
        this.updateIdlePhase();
        break;

      case 'RIH':
        this.updateRIHPhase(deltaSeconds);
        break;

      case 'Work':
        this.updateWorkPhase();
        break;

      case 'POOH':
        this.updatePOOHPhase(deltaSeconds);
        break;
    }

    return this.getState();
  }

  /**
   * Idle phase: At surface, no activity
   */
  private updateIdlePhase(): void {
    this.currentState.depth = 0;
    this.currentState.tension = this.noise.realistic(0, 0.1, 0);
    this.currentState.pressure = this.noise.realistic(0, 0.1, 0);
    this.currentState.flowRate = 0;

    // Transition to RIH after some time
    if (this.currentState.phaseElapsedSeconds > 60) { // 1 minute idle
      this.transitionTo('RIH');
    }
  }

  /**
   * Run In Hole: Descending into wellbore
   */
  private updateRIHPhase(deltaSeconds: number): void {
    // Increase depth at RIH speed
    const depthIncrement = (this.config.rihSpeed / 60) * deltaSeconds; // Convert ft/min to ft/s
    this.currentState.depth = Math.min(
      this.currentState.depth + depthIncrement,
      this.config.targetDepth
    );

    // Tension increases with depth (weight of string in hole)
    // Tension = weight_per_foot * depth + overpull
    const stringWeight = this.config.pipeWeight * this.currentState.depth;
    const overpull = 1000; // Extra tension for friction
    const baseTension = stringWeight + overpull;
    this.currentState.tension = this.noise.realistic(baseTension, 0.05);

    // Light circulation during RIH
    this.currentState.pressure = this.noise.realistic(500, 0.1);
    this.currentState.flowRate = this.noise.realistic(2.0, 0.1);

    // Reached target depth - transition to Work
    if (this.currentState.depth >= this.config.targetDepth * 0.99) {
      this.currentState.depth = this.config.targetDepth;
      this.transitionTo('Work');
    }
  }

  /**
   * Work phase: Operating at depth
   */
  private updateWorkPhase(): void {
    // Maintain constant depth
    this.currentState.depth = this.config.targetDepth;

    // Tension fluctuates during work (pumping, circulation)
    const stringWeight = this.config.pipeWeight * this.currentState.depth;
    const workTension = stringWeight + 2000; // Higher overpull during work
    this.currentState.tension = this.noise.realistic(workTension, 0.08, 0.02);

    // Working pressure and flow
    this.currentState.pressure = this.noise.realistic(this.config.workPressure, 0.05, 0.01);
    this.currentState.flowRate = this.noise.realistic(this.config.workFlowRate, 0.05);

    // Transition to POOH after work duration
    if (this.currentState.phaseElapsedSeconds > this.config.workDuration) {
      this.transitionTo('POOH');
    }
  }

  /**
   * Pull Out Of Hole: Extracting from wellbore
   */
  private updatePOOHPhase(deltaSeconds: number): void {
    // Decrease depth at POOH speed
    const depthDecrement = (this.config.poohSpeed / 60) * deltaSeconds;
    this.currentState.depth = Math.max(
      this.currentState.depth - depthDecrement,
      0
    );

    // Tension decreases with depth
    const stringWeight = this.config.pipeWeight * this.currentState.depth;
    const overpull = 800; // Less overpull pulling out
    const baseTension = stringWeight + overpull;
    this.currentState.tension = this.noise.realistic(baseTension, 0.05);

    // Reduced circulation during POOH
    this.currentState.pressure = this.noise.realistic(300, 0.1);
    this.currentState.flowRate = this.noise.realistic(1.5, 0.1);

    // Reached surface - transition to Idle
    if (this.currentState.depth <= 10) { // Within 10 ft of surface
      this.currentState.depth = 0;
      this.transitionTo('Idle');
    }
  }

  /**
   * Transition to new phase
   */
  private transitionTo(newPhase: JobPhase): void {
    console.log(`🔄 Phase transition: ${this.currentState.phase} → ${newPhase}`);
    this.currentState.phase = newPhase;
    this.currentState.phaseStartTime = new Date();
    this.currentState.phaseElapsedSeconds = 0;
  }

  /**
   * Get current state (with realistic noise)
   */
  getState(): JobState {
    return {
      ...this.currentState,
      // Add small noise to all values
      depth: Math.max(0, this.currentState.depth),
      tension: Math.max(0, this.currentState.tension),
      pressure: Math.max(0, this.currentState.pressure),
      flowRate: Math.max(0, this.currentState.flowRate)
    };
  }

  /**
   * Calculate current stress on pipe
   *
   * @param outerDiameter - OD in inches
   * @param wallThickness - Wall thickness in inches
   * @returns Stress in psi
   */
  getStress(outerDiameter: number, wallThickness: number): number {
    const outerRadius = outerDiameter / 2;
    const innerRadius = outerRadius - wallThickness;
    const area = Math.PI * (outerRadius * outerRadius - innerRadius * innerRadius);

    if (area <= 0) return 0;

    return this.currentState.tension / area;
  }

  /**
   * Force immediate phase change (for testing/manual control)
   */
  setPhase(phase: JobPhase): void {
    this.transitionTo(phase);
  }

  /**
   * Get configuration
   */
  getConfig(): JobPhaseConfig {
    return { ...this.config };
  }

  /**
   * Calculate estimated cycles for current job
   */
  getEstimatedCycles(): number {
    // One cycle = RIH + Work + POOH
    // Approximate based on depth changes
    const rihTime = (this.config.targetDepth / this.config.rihSpeed) * 60; // seconds
    const poohTime = (this.config.targetDepth / this.config.poohSpeed) * 60;
    const totalCycleTime = rihTime + this.config.workDuration + poohTime;

    // Each trip is one fatigue cycle
    return Math.floor(this.currentState.totalRunTime / totalCycleTime);
  }
}
