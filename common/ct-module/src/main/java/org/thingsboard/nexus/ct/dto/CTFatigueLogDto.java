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
package org.thingsboard.nexus.ct.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.nexus.ct.model.CTFatigueLog;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CTFatigueLogDto {

    private UUID id;
    private UUID tenantId;
    private UUID reelId;
    private UUID jobId;
    private Long timestamp;
    private Integer cycleNumber;

    // Parámetros operacionales
    private BigDecimal pressurePsi;
    private BigDecimal tensionLbf;
    private BigDecimal bendRadiusIn;
    private BigDecimal temperatureF;

    // Esfuerzos calculados
    private BigDecimal hoopStressPsi;
    private BigDecimal axialStressPsi;
    private BigDecimal bendingStressPsi;
    private BigDecimal vonMisesStressPsi;

    // Cálculo de fatiga
    private Long cyclesToFailure;
    private BigDecimal fatigueIncrement;
    private BigDecimal accumulatedFatiguePercent;

    // Factores de corrección
    private BigDecimal corrosionFactor;
    private BigDecimal weldFactor;
    private BigDecimal temperatureFactor;

    // Metadata
    private String calculationMethod;
    private String notes;
    private Long createdTime;

    public static CTFatigueLogDto fromEntity(CTFatigueLog entity) {
        if (entity == null) {
            return null;
        }

        return CTFatigueLogDto.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .reelId(entity.getReelId())
            .jobId(entity.getJobId())
            .timestamp(entity.getTimestamp())
            .cycleNumber(entity.getCycleNumber())
            .pressurePsi(entity.getPressurePsi())
            .tensionLbf(entity.getTensionLbf())
            .bendRadiusIn(entity.getBendRadiusIn())
            .temperatureF(entity.getTemperatureF())
            .hoopStressPsi(entity.getHoopStressPsi())
            .axialStressPsi(entity.getAxialStressPsi())
            .bendingStressPsi(entity.getBendingStressPsi())
            .vonMisesStressPsi(entity.getVonMisesStressPsi())
            .cyclesToFailure(entity.getCyclesToFailure())
            .fatigueIncrement(entity.getFatigueIncrement())
            .accumulatedFatiguePercent(entity.getAccumulatedFatiguePercent())
            .corrosionFactor(entity.getCorrosionFactor())
            .weldFactor(entity.getWeldFactor())
            .temperatureFactor(entity.getTemperatureFactor())
            .calculationMethod(entity.getCalculationMethod())
            .notes(entity.getNotes())
            .createdTime(entity.getCreatedTime())
            .build();
    }
}
