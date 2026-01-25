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
package org.thingsboard.nexus.ct.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.ct.dto.CTFatigueLogDto;
import org.thingsboard.nexus.ct.service.CTFatigueService;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nexus/ct/fatigue")
@RequiredArgsConstructor
@Slf4j
public class CTFatigueController {

    private final CTFatigueService fatigueService;

    @PostMapping("/log")
    public ResponseEntity<Void> logFatigueCalculation(@Valid @RequestBody CTFatigueLogDto logDto) {
        log.info("Received fatigue calculation log for reel: {}", logDto.getReelId());
        fatigueService.logFatigueCalculation(logDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/reel/{reelId}/history")
    public ResponseEntity<List<CTFatigueLogDto>> getFatigueHistory(
            @PathVariable UUID reelId,
            @RequestParam(required = false) Long fromTime,
            @RequestParam(required = false) Long toTime) {
        log.info("Getting fatigue history for reel: {}", reelId);
        List<CTFatigueLogDto> history = fatigueService.getFatigueHistory(reelId, fromTime, toTime);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/reel/{reelId}/history/paged")
    public ResponseEntity<Page<CTFatigueLogDto>> getFatigueHistoryPaged(
            @PathVariable UUID reelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("Getting paged fatigue history for reel: {} (page: {}, size: {})", reelId, page, pageSize);
        Page<CTFatigueLogDto> history = fatigueService.getFatigueHistoryPaged(reelId, page, pageSize);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/reel/{reelId}/latest")
    public ResponseEntity<CTFatigueLogDto> getLatestFatigueLog(@PathVariable UUID reelId) {
        log.info("Getting latest fatigue log for reel: {}", reelId);
        CTFatigueLogDto latestLog = fatigueService.getLatestFatigueLog(reelId);
        return ResponseEntity.ok(latestLog);
    }

    @GetMapping("/job/{jobId}/history")
    public ResponseEntity<List<CTFatigueLogDto>> getJobFatigueHistory(@PathVariable UUID jobId) {
        log.info("Getting fatigue history for job: {}", jobId);
        List<CTFatigueLogDto> history = fatigueService.getJobFatigueHistory(jobId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/reel/{reelId}/cycles")
    public ResponseEntity<Long> getTotalCycles(@PathVariable UUID reelId) {
        log.info("Getting total cycles count for reel: {}", reelId);
        Long totalCycles = fatigueService.getTotalCyclesCount(reelId);
        return ResponseEntity.ok(totalCycles);
    }

    @GetMapping("/high-fatigue")
    public ResponseEntity<List<CTFatigueLogDto>> getHighFatigueReels(
            @RequestParam UUID tenantId,
            @RequestParam(defaultValue = "80.0") BigDecimal threshold) {
        log.info("Getting high fatigue reels for tenant: {} with threshold: {}%", tenantId, threshold);
        List<CTFatigueLogDto> highFatigueReels = fatigueService.getHighFatigueReels(tenantId, threshold);
        return ResponseEntity.ok(highFatigueReels);
    }
}
