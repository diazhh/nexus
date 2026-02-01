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
 * Palmgren-Miner Fatigue Damage Model for Coiled Tubing
 */

export interface MaterialProperties {
  grade: 'CT80' | 'CT90' | 'CT100' | 'CT110';  // Yield strength in ksi
  yieldStrength: number;     // psi
  ultimateStrength: number;  // psi
  fatigueExponent: number;   // S-N curve exponent (m), typically 3-5
  fatigueCoefficient: number; // S-N curve coefficient (C)
}

export interface StressCycle {
  stress: number;           // psi
  cycles: number;           // Number of cycles at this stress
  timestamp: Date;          // When this cycle occurred
}

export interface FatigueState {
  totalCycles: number;      // Total number of cycles
  cumulativeDamage: number; // D = Σ(ni/Ni), 0-1 scale
  remainingLife: number;    // Percentage (0-100%)
  stressHistory: StressCycle[];
  predictedFailureCycles: number; // Estimated cycles until failure
}

/**
 * Palmgren-Miner cumulative damage model for coiled tubing fatigue
 */
export class FatigueModel {
  private material: MaterialProperties;
  private stressHistory: StressCycle[] = [];
  private totalCycles = 0;
  private cumulativeDamage = 0;

  // Material presets
  private static readonly MATERIALS: Record<string, MaterialProperties> = {
    CT80: {
      grade: 'CT80',
      yieldStrength: 80000,      // 80 ksi
      ultimateStrength: 95000,   // 95 ksi
      fatigueExponent: 4.0,      // Typical for steel
      fatigueCoefficient: 1e15   // Calibrated for CT
    },
    CT90: {
      grade: 'CT90',
      yieldStrength: 90000,
      ultimateStrength: 105000,
      fatigueExponent: 4.2,
      fatigueCoefficient: 1.2e15
    },
    CT100: {
      grade: 'CT100',
      yieldStrength: 100000,
      ultimateStrength: 115000,
      fatigueExponent: 4.5,
      fatigueCoefficient: 1.5e15
    },
    CT110: {
      grade: 'CT110',
      yieldStrength: 110000,
      ultimateStrength: 125000,
      fatigueExponent: 4.8,
      fatigueCoefficient: 1.8e15
    }
  };

  constructor(materialGrade: 'CT80' | 'CT90' | 'CT100' | 'CT110' = 'CT80') {
    this.material = FatigueModel.MATERIALS[materialGrade];
  }

  /**
   * Calculate cycles to failure at given stress level using S-N curve
   *
   * S-N Curve: N = C / σ^m
   * Where:
   *   N = cycles to failure
   *   C = fatigue coefficient (material constant)
   *   σ = stress amplitude (psi)
   *   m = fatigue exponent (typically 3-5 for steel)
   */
  private cyclestoFailure(stress: number): number {
    if (stress <= 0) {
      return Infinity; // No stress = no fatigue
    }

    // Ensure stress doesn't exceed ultimate strength
    const effectiveStress = Math.min(stress, this.material.ultimateStrength * 0.95);

    // S-N equation
    const N = this.material.fatigueCoefficient / Math.pow(effectiveStress, this.material.fatigueExponent);

    return Math.max(1, N); // Minimum 1 cycle
  }

  /**
   * Add stress cycles to fatigue history
   *
   * @param stress - Stress level (psi)
   * @param cycles - Number of cycles at this stress
   * @param timestamp - When this occurred
   */
  addCycles(stress: number, cycles: number, timestamp: Date = new Date()): void {
    if (cycles <= 0 || stress < 0) {
      return;
    }

    // Calculate damage for these cycles: d = n / N
    const cyclestoFail = this.cyclestoFailure(stress);
    const damage = cycles / cyclestoFail;

    // Update cumulative damage (Miner's Rule)
    this.cumulativeDamage += damage;
    this.totalCycles += cycles;

    // Add to history
    this.stressHistory.push({
      stress,
      cycles,
      timestamp
    });

    // Keep only recent history (last 1000 entries)
    if (this.stressHistory.length > 1000) {
      this.stressHistory.shift();
    }
  }

  /**
   * Get current fatigue state
   */
  getState(): FatigueState {
    // Remaining life = (1 - D) * 100%
    const remainingLife = Math.max(0, (1 - this.cumulativeDamage) * 100);

    // Predict cycles until failure based on current damage rate
    let predictedFailureCycles = Infinity;
    if (this.cumulativeDamage > 0 && this.totalCycles > 0) {
      const damageRate = this.cumulativeDamage / this.totalCycles;
      const remainingDamage = 1.0 - this.cumulativeDamage;
      predictedFailureCycles = this.totalCycles + (remainingDamage / damageRate);
    }

    return {
      totalCycles: this.totalCycles,
      cumulativeDamage: this.cumulativeDamage,
      remainingLife,
      stressHistory: [...this.stressHistory], // Copy
      predictedFailureCycles
    };
  }

  /**
   * Check if coiled tubing has failed (D ≥ 1.0)
   */
  hasFailed(): boolean {
    return this.cumulativeDamage >= 1.0;
  }

  /**
   * Get material properties
   */
  getMaterial(): MaterialProperties {
    return { ...this.material };
  }

  /**
   * Calculate stress from tension and geometry
   *
   * @param tension - Axial tension (lbf)
   * @param outerDiameter - OD in inches
   * @param wallThickness - Wall thickness in inches
   * @returns Stress in psi
   */
  static calculateStress(tension: number, outerDiameter: number, wallThickness: number): number {
    // Cross-sectional area of pipe
    const outerRadius = outerDiameter / 2;
    const innerRadius = outerRadius - wallThickness;
    const area = Math.PI * (outerRadius * outerRadius - innerRadius * innerRadius);

    if (area <= 0) {
      return 0;
    }

    // Stress = Force / Area
    return Math.abs(tension) / area;
  }

  /**
   * Reset fatigue model (for new string or inspection)
   */
  reset(): void {
    this.stressHistory = [];
    this.totalCycles = 0;
    this.cumulativeDamage = 0;
  }

  /**
   * Get fatigue life percentage at given stress
   * (for planning operations)
   */
  static getLifeAtStress(stress: number, cycles: number, materialGrade: 'CT80' | 'CT90' | 'CT100' | 'CT110' = 'CT80'): number {
    const model = new FatigueModel(materialGrade);
    const N = model.cyclestoFailure(stress);
    const damage = cycles / N;
    return Math.max(0, (1 - damage) * 100);
  }
}
