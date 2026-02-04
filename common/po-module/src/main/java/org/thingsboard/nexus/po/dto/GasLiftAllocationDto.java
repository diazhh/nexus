/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.nexus.po.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Gas Lift allocation optimization across multiple wells.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GasLiftAllocationDto {

    /**
     * Optimization result ID
     */
    private UUID optimizationId;

    /**
     * Field/wellpad ID
     */
    private UUID fieldAssetId;

    /**
     * Field name
     */
    private String fieldName;

    /**
     * Total available gas for injection (MSCF/day)
     */
    private BigDecimal totalAvailableGas;

    /**
     * Current total gas injection rate (MSCF/day)
     */
    private BigDecimal currentTotalGasRate;

    /**
     * Optimized total gas injection rate (MSCF/day)
     */
    private BigDecimal optimizedTotalGasRate;

    /**
     * Current total production (BPD)
     */
    private BigDecimal currentTotalProduction;

    /**
     * Expected total production after optimization (BPD)
     */
    private BigDecimal expectedTotalProduction;

    /**
     * Expected total production increase (BPD)
     */
    private BigDecimal expectedProductionIncrease;

    /**
     * Expected production increase (%)
     */
    private BigDecimal expectedProductionIncreasePercent;

    /**
     * Gas utilization efficiency improvement (%)
     */
    private BigDecimal efficiencyImprovement;

    /**
     * Individual well allocations
     */
    private List<WellAllocation> wellAllocations;

    /**
     * Timestamp of optimization
     */
    private Long timestamp;

    /**
     * Confidence score
     */
    private Double confidence;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WellAllocation {
        /**
         * Well asset ID
         */
        private UUID wellAssetId;

        /**
         * Well name
         */
        private String wellName;

        /**
         * Current gas injection rate (MSCF/day)
         */
        private BigDecimal currentGasRate;

        /**
         * Recommended gas injection rate (MSCF/day)
         */
        private BigDecimal recommendedGasRate;

        /**
         * Gas rate change (MSCF/day)
         */
        private BigDecimal gasRateChange;

        /**
         * Current production (BPD)
         */
        private BigDecimal currentProduction;

        /**
         * Expected production (BPD)
         */
        private BigDecimal expectedProduction;

        /**
         * Expected production increase (BPD)
         */
        private BigDecimal expectedProductionIncrease;

        /**
         * Marginal oil rate (BPD per MSCF/day)
         */
        private BigDecimal marginalOilRate;

        /**
         * Gas-oil ratio (SCF/BBL)
         */
        private BigDecimal gasOilRatio;

        /**
         * Priority rank (1 = highest priority for gas)
         */
        private Integer priorityRank;

        /**
         * Is this well at minimum gas rate?
         */
        private Boolean atMinimum;

        /**
         * Is this well at maximum gas rate?
         */
        private Boolean atMaximum;
    }
}
