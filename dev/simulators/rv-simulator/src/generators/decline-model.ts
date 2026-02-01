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
 * Arps Decline Curve Models for Reservoir Production
 */

import { NoiseGenerator } from '@simulators/shared';

export type DeclineType = 'exponential' | 'hyperbolic' | 'harmonic';

export interface DeclineParams {
  type: DeclineType;
  qi: number;           // Initial production rate (bbl/day or MCF/day)
  Di: number;           // Initial decline rate (1/year)
  b?: number;           // Hyperbolic exponent (0-1), only for hyperbolic
  startDate: Date;      // Production start date
}

export interface ProductionData {
  time: Date;
  daysSinceStart: number;
  productionRate: number;      // Current rate (bbl/day or MCF/day)
  cumulativeProduction: number; // Total produced (bbl or MCF)
  reservePressure: number;      // Simulated pressure (psi)
  waterCut: number;             // Water fraction (0-1)
}

export class DeclineModel {
  private noise = new NoiseGenerator();
  private params: DeclineParams;
  private cumulative = 0;

  constructor(params: DeclineParams) {
    this.validateParams(params);
    this.params = params;
  }

  /**
   * Calculate production rate at given time using Arps equations
   */
  getProductionRate(currentDate: Date): number {
    const t = this.getDaysElapsed(currentDate) / 365.25; // Convert to years

    let rate: number;

    switch (this.params.type) {
      case 'exponential':
        // q(t) = qi * exp(-Di * t)
        rate = this.params.qi * Math.exp(-this.params.Di * t);
        break;

      case 'hyperbolic':
        // q(t) = qi / (1 + b * Di * t)^(1/b)
        const b = this.params.b || 0.5;
        rate = this.params.qi / Math.pow(1 + b * this.params.Di * t, 1 / b);
        break;

      case 'harmonic':
        // q(t) = qi / (1 + Di * t)
        rate = this.params.qi / (1 + this.params.Di * t);
        break;
    }

    return Math.max(0, rate);
  }

  /**
   * Calculate cumulative production up to given time
   */
  getCumulativeProduction(currentDate: Date): number {
    const t = this.getDaysElapsed(currentDate) / 365.25; // Years

    let cumulative: number;

    switch (this.params.type) {
      case 'exponential':
        // Np(t) = (qi / Di) * (1 - exp(-Di * t))
        cumulative = (this.params.qi / this.params.Di) * (1 - Math.exp(-this.params.Di * t));
        break;

      case 'hyperbolic':
        const b = this.params.b || 0.5;
        if (b === 1) {
          // Harmonic case
          cumulative = (this.params.qi / this.params.Di) * Math.log(1 + this.params.Di * t);
        } else {
          // Np(t) = (qi^b / (Di * (1-b))) * (qi^(1-b) - q(t)^(1-b))
          const qt = this.getProductionRate(currentDate);
          cumulative = (Math.pow(this.params.qi, b) / (this.params.Di * (1 - b))) *
                       (Math.pow(this.params.qi, 1 - b) - Math.pow(qt, 1 - b));
        }
        break;

      case 'harmonic':
        // Np(t) = (qi / Di) * ln(1 + Di * t)
        cumulative = (this.params.qi / this.params.Di) * Math.log(1 + this.params.Di * t);
        break;
    }

    return Math.max(0, cumulative * 365.25); // Convert to daily units
  }

  /**
   * Generate complete production data with realistic noise and correlations
   */
  generateProductionData(currentDate: Date): ProductionData {
    const daysSinceStart = this.getDaysElapsed(currentDate);
    const t = daysSinceStart / 365.25;

    // Base production rate from decline curve
    const baseRate = this.getProductionRate(currentDate);

    // Add realistic noise (±5% variability)
    const productionRate = this.noise.realistic(baseRate, 0.05);

    // Cumulative production
    const cumulativeProduction = this.getCumulativeProduction(currentDate);

    // Simulate reservoir pressure decline (correlated with production)
    const initialPressure = 4000; // psi
    const pressureDeclineRate = 0.3; // Pressure declines slower than production
    const reservePressure = initialPressure * Math.exp(-this.params.Di * t * pressureDeclineRate);
    const noisyPressure = this.noise.realistic(reservePressure, 0.02);

    // Simulate water cut increase over time (water breakthrough)
    const waterBreakthrough = 2; // Years until water appears
    let waterCut = 0;
    if (t > waterBreakthrough) {
      // Water cut increases logistically after breakthrough
      const timeSinceBreakthrough = t - waterBreakthrough;
      waterCut = 1 / (1 + Math.exp(-0.5 * timeSinceBreakthrough)) * 0.8; // Max 80% water
      waterCut = this.noise.clamp(waterCut + this.noise.gaussian(0, 1) * 0.02, 0, 1);
    }

    return {
      time: currentDate,
      daysSinceStart,
      productionRate: Math.max(0, productionRate),
      cumulativeProduction,
      reservePressure: this.noise.clamp(noisyPressure, 0, initialPressure),
      waterCut: this.noise.clamp(waterCut, 0, 1)
    };
  }

  /**
   * Get days elapsed since production start
   */
  private getDaysElapsed(currentDate: Date): number {
    const elapsed = currentDate.getTime() - this.params.startDate.getTime();
    return Math.max(0, elapsed / (1000 * 60 * 60 * 24));
  }

  /**
   * Validate decline parameters
   */
  private validateParams(params: DeclineParams): void {
    if (params.qi <= 0) {
      throw new Error('Initial production rate (qi) must be positive');
    }
    if (params.Di <= 0) {
      throw new Error('Decline rate (Di) must be positive');
    }
    if (params.type === 'hyperbolic') {
      if (params.b === undefined || params.b < 0 || params.b > 1) {
        throw new Error('Hyperbolic exponent (b) must be between 0 and 1');
      }
    }
  }

  /**
   * Get estimated ultimate recovery (EUR)
   */
  getEUR(): number {
    const t = 30; // Project 30 years into future

    switch (this.params.type) {
      case 'exponential':
        return (this.params.qi / this.params.Di) * 365.25;

      case 'hyperbolic':
        const b = this.params.b || 0.5;
        if (b === 1) {
          return Infinity; // Harmonic decline theoretically produces forever
        }
        const qFinal = this.params.qi / Math.pow(1 + b * this.params.Di * t, 1 / b);
        return (Math.pow(this.params.qi, b) / (this.params.Di * (1 - b))) *
               (Math.pow(this.params.qi, 1 - b) - Math.pow(qFinal, 1 - b)) * 365.25;

      case 'harmonic':
        return Infinity;
    }
  }
}
