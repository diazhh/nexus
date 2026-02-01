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
 * Drilling Model with MSE and ECD calculations
 */

import { NoiseGenerator } from '@simulators/shared';

export interface FormationProperties {
  name: string;
  topDepth: number;          // ft
  bottomDepth: number;       // ft
  compressiveStrength: number; // psi (UCS)
  drillability: number;      // 0-1 scale (1 = easy to drill)
  porePressureGradient: number; // psi/ft
  fractureGradient: number;  // psi/ft
}

export interface DrillingParameters {
  // Controllable parameters
  wob: number;              // Weight On Bit (lbf)
  rpm: number;              // Rotary speed (RPM)
  flowRate: number;         // Pump rate (gpm)
  mudWeight: number;        // Mud density (ppg)

  // Derived parameters
  rop: number;              // Rate of Penetration (ft/hr)
  torque: number;           // Torque (ft-lbf)
  standpipePressure: number; // psi
  hookLoad: number;         // lbf

  // Calculated metrics
  mse: number;              // Mechanical Specific Energy (ksi)
  ecd: number;              // Equivalent Circulating Density (ppg)

  // Current state
  depth: number;            // Current depth (ft)
  formation: FormationProperties;
}

export interface BitGeometry {
  diameter: number;         // inches
  area: number;             // in² (calculated)
  nozzles: number;          // Number of nozzles
  tfa: number;              // Total Flow Area (in²)
}

export interface WellGeometry {
  holeSize: number;         // inches
  drillpipeOD: number;      // inches
  drillpipeID: number;      // inches
  annularArea: number;      // in² (calculated)
}

/**
 * Drilling model with MSE, ECD, and parameter correlation
 */
export class DrillingModel {
  private noise = new NoiseGenerator();
  private bit: BitGeometry;
  private well: WellGeometry;
  private formations: FormationProperties[];
  private currentDepth = 0;

  constructor(
    bit: BitGeometry,
    well: WellGeometry,
    formations: FormationProperties[]
  ) {
    this.bit = bit;
    this.well = well;
    this.formations = formations.sort((a, b) => a.topDepth - b.topDepth);

    // Calculate derived geometry
    this.bit.area = Math.PI * Math.pow(this.bit.diameter / 2, 2);
    this.well.annularArea = Math.PI * (
      Math.pow(this.well.holeSize / 2, 2) -
      Math.pow(this.well.drillpipeOD / 2, 2)
    );
  }

  /**
   * Calculate Rate of Penetration based on formation and parameters
   *
   * Simplified Bourgoyne & Young model:
   * ROP ∝ (WOB/diameter)^a * RPM^b * (1/UCS)^c
   */
  private calculateROP(wob: number, rpm: number, formation: FormationProperties): number {
    // Normalize parameters
    const wobPerInch = wob / this.bit.diameter; // lbf/in
    const normalizedUCS = formation.compressiveStrength / 10000; // Normalize to ~1

    // Empirical coefficients
    const a = 0.6;  // WOB exponent
    const b = 0.4;  // RPM exponent

    // Base ROP from drilling efficiency
    const baseROP = 100 * formation.drillability *
                    Math.pow(wobPerInch / 1000, a) *
                    Math.pow(rpm / 100, b) *
                    (1 / normalizedUCS);

    // Add realistic variation
    return this.noise.realistic(baseROP, 0.15, 0.02);
  }

  /**
   * Calculate Torque based on WOB and formation
   *
   * T = μ * WOB * (diameter/2)
   * where μ is coefficient of friction
   */
  private calculateTorque(wob: number, formation: FormationProperties): number {
    // Coefficient of friction (varies with formation)
    const mu = 0.3 + (1 - formation.drillability) * 0.4; // 0.3-0.7

    const torque = mu * wob * (this.bit.diameter / 2) / 12; // Convert to ft-lbf

    return this.noise.realistic(torque, 0.1, 0.01);
  }

  /**
   * Calculate Mechanical Specific Energy (MSE)
   *
   * MSE = (WOB/A) + (120*π*RPM*T)/(ROP*A)
   *
   * Where:
   *   WOB = Weight on bit (lbf)
   *   A = Bit area (in²)
   *   RPM = Rotations per minute
   *   T = Torque (ft-lbf)
   *   ROP = Rate of penetration (ft/hr)
   *
   * Lower MSE = more efficient drilling
   * MSE ≈ UCS for optimal drilling
   */
  private calculateMSE(wob: number, rpm: number, torque: number, rop: number): number {
    if (rop <= 0) return 0;

    const wobComponent = wob / this.bit.area;
    const torqueComponent = (120 * Math.PI * rpm * torque) / (rop * this.bit.area);

    const mse = (wobComponent + torqueComponent) / 1000; // Convert to ksi

    return Math.max(0, mse);
  }

  /**
   * Calculate Standpipe Pressure (SPP)
   *
   * SPP = ΔP_bit + ΔP_drillstring + ΔP_annular
   */
  private calculateStandpipePressure(flowRate: number, mudWeight: number, depth: number): number {
    // Simplified pressure loss calculations

    // Bit pressure drop (largest component)
    const velocity = flowRate / (60 * this.bit.tfa); // ft/s
    const bitPressureDrop = 0.052 * mudWeight * Math.pow(velocity, 2) / 1097; // psi

    // Drillstring friction
    const drillstringPressureDrop = 0.000015 * Math.pow(flowRate, 1.8) * depth / 1000;

    // Annular friction
    const annularPressureDrop = 0.00001 * Math.pow(flowRate, 1.8) * depth / 1000;

    const spp = bitPressureDrop + drillstringPressureDrop + annularPressureDrop;

    return this.noise.realistic(spp, 0.05);
  }

  /**
   * Calculate Equivalent Circulating Density (ECD)
   *
   * ECD = MW + (ΔP_annular / (0.052 * TVD))
   *
   * ECD must stay between pore pressure and fracture pressure
   */
  private calculateECD(
    mudWeight: number,
    flowRate: number,
    depth: number,
    formation: FormationProperties
  ): number {
    // Annular pressure loss
    const annularPressureDrop = 0.00001 * Math.pow(flowRate, 1.8) * depth / 1000;

    // ECD calculation
    const ecd = mudWeight + (annularPressureDrop / (0.052 * depth));

    return this.noise.realistic(ecd, 0.02);
  }

  /**
   * Get current formation at depth
   */
  private getCurrentFormation(depth: number): FormationProperties {
    for (const formation of this.formations) {
      if (depth >= formation.topDepth && depth < formation.bottomDepth) {
        return formation;
      }
    }
    // Return last formation if beyond all
    return this.formations[this.formations.length - 1];
  }

  /**
   * Update drilling parameters and advance depth
   *
   * @param wob - Weight on bit (lbf)
   * @param rpm - Rotary speed (RPM)
   * @param flowRate - Pump rate (gpm)
   * @param mudWeight - Mud density (ppg)
   * @param deltaTime - Time step (seconds)
   * @returns Updated drilling parameters
   */
  update(
    wob: number,
    rpm: number,
    flowRate: number,
    mudWeight: number,
    deltaTime: number
  ): DrillingParameters {
    // Get current formation
    const formation = this.getCurrentFormation(this.currentDepth);

    // Calculate ROP
    const rop = this.calculateROP(wob, rpm, formation);

    // Advance depth
    const depthIncrement = (rop / 3600) * deltaTime; // Convert ft/hr to ft/s
    this.currentDepth += depthIncrement;

    // Calculate torque
    const torque = this.calculateTorque(wob, formation);

    // Calculate MSE
    const mse = this.calculateMSE(wob, rpm, torque, rop);

    // Calculate pressures
    const standpipePressure = this.calculateStandpipePressure(flowRate, mudWeight, this.currentDepth);

    // Calculate ECD
    const ecd = this.calculateECD(mudWeight, flowRate, this.currentDepth, formation);

    // Calculate hook load (simplified)
    const stringWeight = this.currentDepth * 15; // ~15 lbf/ft for drillstring
    const buoyancyFactor = 1 - (mudWeight / 65.5); // Buoyancy effect
    const hookLoad = stringWeight * buoyancyFactor;

    return {
      wob: this.noise.realistic(wob, 0.05),
      rpm: this.noise.realistic(rpm, 0.03),
      flowRate: this.noise.realistic(flowRate, 0.05),
      mudWeight: this.noise.realistic(mudWeight, 0.01),
      rop: Math.max(0, rop),
      torque: Math.max(0, torque),
      standpipePressure: Math.max(0, standpipePressure),
      hookLoad: Math.max(0, hookLoad),
      mse: Math.max(0, mse),
      ecd: Math.max(mudWeight, ecd),
      depth: this.currentDepth,
      formation
    };
  }

  /**
   * Check for drilling problems
   */
  detectProblems(params: DrillingParameters): string[] {
    const problems: string[] = [];

    // MSE too high (inefficient drilling)
    const optimalMSE = params.formation.compressiveStrength / 1000; // ksi
    if (params.mse > optimalMSE * 2) {
      problems.push('HIGH_MSE');
    }

    // ECD approaching fracture gradient
    const fractureECD = params.formation.fractureGradient / 0.052;
    if (params.ecd > fractureECD * 0.95) {
      problems.push('HIGH_ECD');
    }

    // ECD below pore pressure (underbalanced)
    const porePressureECD = params.formation.porePressureGradient / 0.052;
    if (params.ecd < porePressureECD * 1.05) {
      problems.push('LOW_ECD');
    }

    // Stick-slip (torque oscillations - simplified detection)
    if (params.rpm < 60 && params.torque > 5000) {
      problems.push('STICK_SLIP');
    }

    // Low ROP
    if (params.rop < 10) {
      problems.push('LOW_ROP');
    }

    return problems;
  }

  /**
   * Get current depth
   */
  getDepth(): number {
    return this.currentDepth;
  }

  /**
   * Set depth (for initialization)
   */
  setDepth(depth: number): void {
    this.currentDepth = Math.max(0, depth);
  }

  /**
   * Get bit geometry
   */
  getBit(): BitGeometry {
    return { ...this.bit };
  }

  /**
   * Get well geometry
   */
  getWell(): WellGeometry {
    return { ...this.well };
  }
}
