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
package org.thingsboard.nexus.dr.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.thingsboard.nexus.dr.model.enums.LithologyType;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing a Mud Log entry.
 * Mud logs record geological and drilling data at specific depths.
 */
@Entity
@Table(name = "dr_mud_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrMudLog {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Reference to the drilling run
     */
    @Column(name = "run_id", nullable = false)
    private UUID runId;

    /**
     * Reference to the well (from rv-module)
     */
    @Column(name = "well_id", nullable = false)
    private UUID wellId;

    // --- Depth Information ---

    /**
     * Measured Depth of sample in feet
     */
    @Column(name = "md_ft", nullable = false, precision = 10, scale = 2)
    private BigDecimal mdFt;

    /**
     * Measured Depth of sample top in feet
     */
    @Column(name = "md_top_ft", precision = 10, scale = 2)
    private BigDecimal mdTopFt;

    /**
     * Measured Depth of sample bottom in feet
     */
    @Column(name = "md_bottom_ft", precision = 10, scale = 2)
    private BigDecimal mdBottomFt;

    /**
     * True Vertical Depth in feet
     */
    @Column(name = "tvd_ft", precision = 10, scale = 2)
    private BigDecimal tvdFt;

    // --- Lithology Information ---

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_lithology", length = 50)
    private LithologyType primaryLithology;

    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_lithology", length = 50)
    private LithologyType secondaryLithology;

    /**
     * Percentage of primary lithology (0-100)
     */
    @Column(name = "primary_lithology_percent", precision = 5, scale = 2)
    private BigDecimal primaryLithologyPercent;

    /**
     * Lithology description from geologist
     */
    @Column(name = "lithology_description", columnDefinition = "TEXT")
    private String lithologyDescription;

    /**
     * Rock color
     */
    @Column(name = "color", length = 100)
    private String color;

    /**
     * Grain size (Fine, Medium, Coarse, etc.)
     */
    @Column(name = "grain_size", length = 50)
    private String grainSize;

    /**
     * Rock hardness
     */
    @Column(name = "hardness", length = 50)
    private String hardness;

    /**
     * Porosity type
     */
    @Column(name = "porosity_type", length = 100)
    private String porosityType;

    /**
     * Visual porosity estimate percentage
     */
    @Column(name = "porosity_percent", precision = 5, scale = 2)
    private BigDecimal porosityPercent;

    /**
     * Cement type
     */
    @Column(name = "cement_type", length = 100)
    private String cite;

    /**
     * Sorting (well sorted, poorly sorted, etc.)
     */
    @Column(name = "sorting", length = 50)
    private String sorting;

    /**
     * Roundness of grains
     */
    @Column(name = "roundness", length = 50)
    private String roundness;

    // --- Gas and Show Information ---

    /**
     * Total gas reading in units
     */
    @Column(name = "total_gas_units", precision = 10, scale = 2)
    private BigDecimal totalGasUnits;

    /**
     * Background gas level
     */
    @Column(name = "background_gas_units", precision = 10, scale = 2)
    private BigDecimal backgroundGasUnits;

    /**
     * Connection gas level
     */
    @Column(name = "connection_gas_units", precision = 10, scale = 2)
    private BigDecimal connectionGasUnits;

    /**
     * Trip gas level
     */
    @Column(name = "trip_gas_units", precision = 10, scale = 2)
    private BigDecimal tripGasUnits;

    /**
     * Methane (C1) percentage
     */
    @Column(name = "c1_percent", precision = 6, scale = 3)
    private BigDecimal c1Percent;

    /**
     * Ethane (C2) percentage
     */
    @Column(name = "c2_percent", precision = 6, scale = 3)
    private BigDecimal c2Percent;

    /**
     * Propane (C3) percentage
     */
    @Column(name = "c3_percent", precision = 6, scale = 3)
    private BigDecimal c3Percent;

    /**
     * Isobutane (iC4) percentage
     */
    @Column(name = "ic4_percent", precision = 6, scale = 3)
    private BigDecimal ic4Percent;

    /**
     * Normal butane (nC4) percentage
     */
    @Column(name = "nc4_percent", precision = 6, scale = 3)
    private BigDecimal nc4Percent;

    /**
     * Isopentane (iC5) percentage
     */
    @Column(name = "ic5_percent", precision = 6, scale = 3)
    private BigDecimal ic5Percent;

    /**
     * Normal pentane (nC5) percentage
     */
    @Column(name = "nc5_percent", precision = 6, scale = 3)
    private BigDecimal nc5Percent;

    // --- Hydrocarbon Shows ---

    /**
     * Oil show type (fluorescence, cut, stain, etc.)
     */
    @Column(name = "oil_show_type", length = 100)
    private String oilShowType;

    /**
     * Oil show intensity (None, Trace, Fair, Good, Excellent)
     */
    @Column(name = "oil_show_intensity", length = 50)
    private String oilShowIntensity;

    /**
     * Fluorescence color
     */
    @Column(name = "fluorescence_color", length = 100)
    private String fluorescenceColor;

    /**
     * Fluorescence intensity percentage
     */
    @Column(name = "fluorescence_percent", precision = 5, scale = 2)
    private BigDecimal fluorescencePercent;

    /**
     * Cut fluorescence description
     */
    @Column(name = "cut_description", length = 255)
    private String cutDescription;

    /**
     * Stain description
     */
    @Column(name = "stain_description", length = 255)
    private String stainDescription;

    // --- Drilling Parameters at this depth ---

    /**
     * Rate of Penetration at this depth (ft/hr)
     */
    @Column(name = "rop_ft_hr", precision = 8, scale = 2)
    private BigDecimal ropFtHr;

    /**
     * Weight on Bit (klbs)
     */
    @Column(name = "wob_klbs", precision = 6, scale = 2)
    private BigDecimal wobKlbs;

    /**
     * Rotary RPM
     */
    @Column(name = "rpm", precision = 6, scale = 2)
    private BigDecimal rpm;

    /**
     * Torque (ft-lbs)
     */
    @Column(name = "torque_ft_lbs", precision = 8, scale = 2)
    private BigDecimal torqueFtLbs;

    /**
     * Pump pressure (psi)
     */
    @Column(name = "pump_pressure_psi", precision = 8, scale = 2)
    private BigDecimal pumpPressurePsi;

    /**
     * Flow rate (gpm)
     */
    @Column(name = "flow_rate_gpm", precision = 8, scale = 2)
    private BigDecimal flowRateGpm;

    // --- Mud Properties ---

    /**
     * Mud weight in (ppg)
     */
    @Column(name = "mud_weight_ppg", precision = 6, scale = 2)
    private BigDecimal mudWeightPpg;

    /**
     * Mud weight out (ppg)
     */
    @Column(name = "mud_weight_out_ppg", precision = 6, scale = 2)
    private BigDecimal mudWeightOutPpg;

    /**
     * Mud viscosity (sec/qt)
     */
    @Column(name = "mud_viscosity", precision = 6, scale = 2)
    private BigDecimal mudViscosity;

    /**
     * Mud temperature in (°F)
     */
    @Column(name = "mud_temp_in_f", precision = 6, scale = 2)
    private BigDecimal mudTempInF;

    /**
     * Mud temperature out (°F)
     */
    @Column(name = "mud_temp_out_f", precision = 6, scale = 2)
    private BigDecimal mudTempOutF;

    /**
     * Chlorides (ppm)
     */
    @Column(name = "chlorides_ppm", precision = 10, scale = 2)
    private BigDecimal chloridesPpm;

    // --- Sample Information ---

    /**
     * Sample type (cuttings, core, sidewall core)
     */
    @Column(name = "sample_type", length = 50)
    private String sampleType;

    /**
     * Sample number/identifier
     */
    @Column(name = "sample_number", length = 50)
    private String sampleNumber;

    /**
     * Lag time in minutes
     */
    @Column(name = "lag_time_minutes", precision = 6, scale = 2)
    private BigDecimal lagTimeMinutes;

    /**
     * Timestamp when sample was collected
     */
    @Column(name = "sample_time", nullable = false)
    private Long sampleTime;

    // --- Formation Information ---

    /**
     * Formation name
     */
    @Column(name = "formation_name", length = 100)
    private String formationName;

    /**
     * Formation top depth
     */
    @Column(name = "formation_top_ft", precision = 10, scale = 2)
    private BigDecimal formationTopFt;

    /**
     * Geological age/period
     */
    @Column(name = "geological_age", length = 100)
    private String geologicalAge;

    // --- Raw Data ---

    @Type(JsonBinaryType.class)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private JsonNode rawData;

    // --- Metadata ---

    /**
     * Geologist name who logged the sample
     */
    @Column(name = "logged_by", length = 100)
    private String loggedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    @Column(name = "updated_time")
    private Long updatedTime;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdTime == null) {
            createdTime = System.currentTimeMillis();
        }
        if (sampleTime == null) {
            sampleTime = System.currentTimeMillis();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedTime = System.currentTimeMillis();
    }
}
