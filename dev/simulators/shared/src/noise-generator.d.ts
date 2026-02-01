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
export declare class NoiseGenerator {
    /**
     * Generate Gaussian (normal distribution) noise
     * Uses Box-Muller transform
     *
     * @param mean - Mean value
     * @param stdDev - Standard deviation
     * @returns Random value from normal distribution
     */
    gaussian(mean: number, stdDev: number): number;
    /**
     * Add uniform random noise within a range
     *
     * @param value - Base value
     * @param range - +/- range for noise
     * @returns Value with uniform noise added
     */
    uniform(value: number, range: number): number;
    /**
     * Simulate sensor drift over time
     *
     * @param baseValue - Original sensor value
     * @param time - Time elapsed (any unit)
     * @param driftRate - Drift per time unit
     * @returns Value with drift applied
     */
    drift(baseValue: number, time: number, driftRate: number): number;
    /**
     * Add random spikes (outliers) with given probability
     *
     * @param value - Base value
     * @param probability - Probability of spike (0-1)
     * @param magnitude - Spike magnitude
     * @returns Value with possible spike
     */
    randomSpike(value: number, probability: number, magnitude: number): number;
    /**
     * Generate value with realistic sensor noise (combination of effects)
     *
     * @param baseValue - Base value
     * @param noisePercent - Noise as percentage of base value (e.g., 0.05 = 5%)
     * @param spikeProb - Probability of spike
     * @returns Realistic noisy value
     */
    realistic(baseValue: number, noisePercent?: number, spikeProb?: number): number;
    /**
     * Smooth transition between two values
     *
     * @param current - Current value
     * @param target - Target value
     * @param alpha - Smoothing factor (0-1), higher = faster transition
     * @returns Smoothed value
     */
    smooth(current: number, target: number, alpha?: number): number;
    /**
     * Clamp value between min and max
     */
    clamp(value: number, min: number, max: number): number;
    /**
     * Generate periodic variation (sine wave)
     *
     * @param time - Time value
     * @param amplitude - Wave amplitude
     * @param period - Wave period
     * @param offset - Vertical offset
     * @returns Periodic value
     */
    periodic(time: number, amplitude: number, period: number, offset?: number): number;
}
//# sourceMappingURL=noise-generator.d.ts.map