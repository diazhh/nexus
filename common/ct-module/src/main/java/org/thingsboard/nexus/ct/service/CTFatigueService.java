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
package org.thingsboard.nexus.ct.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.ct.dto.CTFatigueLogDto;
import org.thingsboard.nexus.ct.dto.CTReelDto;
import org.thingsboard.nexus.ct.model.CTFatigueLog;
import org.thingsboard.nexus.ct.repository.CTFatigueLogRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CTFatigueService {

    private final CTFatigueLogRepository fatigueLogRepository;
    private final CTReelService reelService;

    @Async
    @Transactional
    public void logFatigueCalculation(CTFatigueLogDto logDto) {
        log.info("Logging fatigue calculation for reel: {}", logDto.getReelId());

        CTFatigueLog fatigueLog = CTFatigueLog.builder()
            .tenantId(logDto.getTenantId())
            .reelId(logDto.getReelId())
            .jobId(logDto.getJobId())
            .timestamp(logDto.getTimestamp())
            .cycleNumber(logDto.getCycleNumber())
            .pressurePsi(logDto.getPressurePsi())
            .tensionLbf(logDto.getTensionLbf())
            .bendRadiusIn(logDto.getBendRadiusIn())
            .temperatureF(logDto.getTemperatureF())
            .hoopStressPsi(logDto.getHoopStressPsi())
            .axialStressPsi(logDto.getAxialStressPsi())
            .bendingStressPsi(logDto.getBendingStressPsi())
            .vonMisesStressPsi(logDto.getVonMisesStressPsi())
            .cyclesToFailure(logDto.getCyclesToFailure())
            .fatigueIncrement(logDto.getFatigueIncrement())
            .accumulatedFatiguePercent(logDto.getAccumulatedFatiguePercent())
            .corrosionFactor(logDto.getCorrosionFactor())
            .weldFactor(logDto.getWeldFactor())
            .temperatureFactor(logDto.getTemperatureFactor())
            .calculationMethod(logDto.getCalculationMethod())
            .notes(logDto.getNotes())
            .build();

        fatigueLogRepository.save(fatigueLog);

        // Actualizar fatiga acumulada en el reel
        updateReelAccumulatedFatigue(logDto.getReelId(), logDto.getAccumulatedFatiguePercent());

        log.info("Fatigue calculation logged successfully. Accumulated fatigue: {}%", 
                 logDto.getAccumulatedFatiguePercent());
    }

    @Transactional
    public void updateReelAccumulatedFatigue(UUID reelId, BigDecimal accumulatedFatigue) {
        CTReelDto reel = reelService.getById(reelId);
        reelService.updateFatigue(reelId, accumulatedFatigue, reel.getTotalCycles());
        log.debug("Updated reel {} accumulated fatigue to {}%", reelId, accumulatedFatigue);
    }

    @Transactional(readOnly = true)
    public List<CTFatigueLogDto> getFatigueHistory(UUID reelId, Long fromTime, Long toTime) {
        log.info("Retrieving fatigue history for reel: {} from {} to {}", reelId, fromTime, toTime);

        List<CTFatigueLog> logs;
        if (fromTime != null && toTime != null) {
            logs = fatigueLogRepository.findByReelIdAndTimeRange(reelId, fromTime, toTime);
        } else {
            logs = fatigueLogRepository.findByReelIdOrderByTimestampDesc(reelId);
        }

        return logs.stream()
            .map(CTFatigueLogDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CTFatigueLogDto> getFatigueHistoryPaged(UUID reelId, int page, int pageSize) {
        log.info("Retrieving paged fatigue history for reel: {} (page: {}, size: {})", 
                 reelId, page, pageSize);

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<CTFatigueLog> logs = fatigueLogRepository.findByReelIdOrderByTimestampDesc(reelId, pageable);

        return logs.map(CTFatigueLogDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public CTFatigueLogDto getLatestFatigueLog(UUID reelId) {
        log.info("Retrieving latest fatigue log for reel: {}", reelId);

        CTFatigueLog latestLog = fatigueLogRepository.findLatestByReelId(reelId);
        return CTFatigueLogDto.fromEntity(latestLog);
    }

    @Transactional(readOnly = true)
    public List<CTFatigueLogDto> getJobFatigueHistory(UUID jobId) {
        log.info("Retrieving fatigue history for job: {}", jobId);

        List<CTFatigueLog> logs = fatigueLogRepository.findByJobIdOrderByTimestampAsc(jobId);
        return logs.stream()
            .map(CTFatigueLogDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CTFatigueLogDto> getHighFatigueReels(UUID tenantId, BigDecimal threshold) {
        log.info("Retrieving reels with fatigue >= {}% for tenant: {}", threshold, tenantId);

        List<CTFatigueLog> logs = fatigueLogRepository.findHighFatigueReels(tenantId, threshold);
        return logs.stream()
            .map(CTFatigueLogDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long getTotalCyclesCount(UUID reelId) {
        return fatigueLogRepository.countByReelId(reelId);
    }
}
