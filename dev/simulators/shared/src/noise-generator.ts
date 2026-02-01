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
 * Noise Generator for realistic sensor data simulation
 */

export class NoiseGenerator {
  /**
   * Generate Gaussian (normal distribution) noise
   * Uses Box-Muller transform
   *
   * @param mean - Mean value
   * @param stdDev - Standard deviation
   * @returns Random value from normal distribution
   */
  gaussian(mean: number, stdDev: number): number {
    const u1 = Math.random();
    const u2 = Math.random();

    // Box-Muller transform
    const z0 = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);

    return mean + z0 * stdDev;
  }

  /**
   * Add uniform random noise within a range
   *
   * @param value - Base value
   * @param range - +/- range for noise
   * @returns Value with uniform noise added
   */
  uniform(value: number, range: number): number {
    return value + (Math.random() - 0.5) * 2 * range;
  }

  /**
   * Simulate sensor drift over time
   *
   * @param baseValue - Original sensor value
   * @param time - Time elapsed (any unit)
   * @param driftRate - Drift per time unit
   * @returns Value with drift applied
   */
  drift(baseValue: number, time: number, driftRate: number): number {
    return baseValue + (time * driftRate);
  }

  /**
   * Add random spikes (outliers) with given probability
   *
   * @param value - Base value
   * @param probability - Probability of spike (0-1)
   * @param magnitude - Spike magnitude
   * @returns Value with possible spike
   */
  randomSpike(value: number, probability: number, magnitude: number): number {
    if (Math.random() < probability) {
      const direction = Math.random() > 0.5 ? 1 : -1;
      return value + direction * magnitude;
    }
    return value;
  }

  /**
   * Generate value with realistic sensor noise (combination of effects)
   *
   * @param baseValue - Base value
   * @param noisePercent - Noise as percentage of base value (e.g., 0.05 = 5%)
   * @param spikeProb - Probability of spike
   * @returns Realistic noisy value
   */
  realistic(baseValue: number, noisePercent: number = 0.05, spikeProb: number = 0.01): number {
    const stdDev = Math.abs(baseValue) * noisePercent;
    let value = this.gaussian(baseValue, stdDev);
    value = this.randomSpike(value, spikeProb, stdDev * 3);
    return value;
  }

  /**
   * Smooth transition between two values
   *
   * @param current - Current value
   * @param target - Target value
   * @param alpha - Smoothing factor (0-1), higher = faster transition
   * @returns Smoothed value
   */
  smooth(current: number, target: number, alpha: number = 0.1): number {
    return current + alpha * (target - current);
  }

  /**
   * Clamp value between min and max
   */
  clamp(value: number, min: number, max: number): number {
    return Math.max(min, Math.min(max, value));
  }

  /**
   * Generate periodic variation (sine wave)
   *
   * @param time - Time value
   * @param amplitude - Wave amplitude
   * @param period - Wave period
   * @param offset - Vertical offset
   * @returns Periodic value
   */
  periodic(time: number, amplitude: number, period: number, offset: number = 0): number {
    return offset + amplitude * Math.sin(2 * Math.PI * time / period);
  }
}
