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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.ct.rule.CTJobSimulationNode.JobParameters;
import org.thingsboard.nexus.ct.rule.CTJobSimulationNode.SimulationResult;
import org.thingsboard.nexus.ct.service.CTSimulationService;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/nexus/ct/simulation")
@RequiredArgsConstructor
@Slf4j
public class CTSimulationController {

    private final CTSimulationService simulationService;

    @PostMapping("/job/{jobId}")
    public ResponseEntity<SimulationResult> simulateJob(@PathVariable UUID jobId) {
        log.info("Received simulation request for job: {}", jobId);
        SimulationResult result = simulationService.simulateJob(jobId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/custom")
    public ResponseEntity<SimulationResult> simulateCustomJob(
            @Valid @RequestBody JobParameters params) {
        log.info("Received custom simulation request for well: {}", params.getWellName());
        SimulationResult result = simulationService.simulateCustomJob(params);
        return ResponseEntity.ok(result);
    }
}
