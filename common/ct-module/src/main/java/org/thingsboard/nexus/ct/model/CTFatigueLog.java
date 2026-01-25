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
package org.thingsboard.nexus.ct.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ct_fatigue_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CTFatigueLog {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "reel_id", nullable = false)
    private UUID reelId;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "cycle_number")
    private Integer cycleNumber;

    // Parámetros operacionales del ciclo
    @Column(name = "pressure_psi", precision = 10, scale = 2)
    private BigDecimal pressurePsi;

    @Column(name = "tension_lbf", precision = 10, scale = 2)
    private BigDecimal tensionLbf;

    @Column(name = "bend_radius_in", precision = 8, scale = 2)
    private BigDecimal bendRadiusIn;

    @Column(name = "temperature_f", precision = 6, scale = 2)
    private BigDecimal temperatureF;

    // Esfuerzos calculados
    @Column(name = "hoop_stress_psi", precision = 12, scale = 2)
    private BigDecimal hoopStressPsi;

    @Column(name = "axial_stress_psi", precision = 12, scale = 2)
    private BigDecimal axialStressPsi;

    @Column(name = "bending_stress_psi", precision = 12, scale = 2)
    private BigDecimal bendingStressPsi;

    @Column(name = "von_mises_stress_psi", precision = 12, scale = 2)
    private BigDecimal vonMisesStressPsi;

    // Cálculo de fatiga
    @Column(name = "cycles_to_failure")
    private Long cyclesToFailure;

    @Column(name = "fatigue_increment", precision = 12, scale = 10)
    private BigDecimal fatigueIncrement;

    @Column(name = "accumulated_fatigue_percent", precision = 6, scale = 3)
    private BigDecimal accumulatedFatiguePercent;

    // Factores de corrección aplicados
    @Column(name = "corrosion_factor", precision = 4, scale = 3)
    private BigDecimal corrosionFactor;

    @Column(name = "weld_factor", precision = 4, scale = 3)
    private BigDecimal weldFactor;

    @Column(name = "temperature_factor", precision = 4, scale = 3)
    private BigDecimal temperatureFactor;

    // Metadata
    @Column(name = "calculation_method", length = 50)
    private String calculationMethod;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdTime == null) {
            createdTime = System.currentTimeMillis();
        }
    }
}
