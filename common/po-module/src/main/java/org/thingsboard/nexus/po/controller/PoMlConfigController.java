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
package org.thingsboard.nexus.po.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.nexus.po.dto.ml.PoMlConfigDto;
import org.thingsboard.nexus.po.service.ml.PoMlConfigService;

import java.util.UUID;

/**
 * REST Controller for ML configuration management.
 */
@RestController
@RequestMapping("/api/nexus/po/ml/config")
@RequiredArgsConstructor
@Slf4j
public class PoMlConfigController {

    private final PoMlConfigService configService;

    /**
     * Get ML configuration for tenant.
     */
    @GetMapping
    public ResponseEntity<PoMlConfigDto> getConfig(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("Getting ML config for tenant: {}", tenantId);
        return ResponseEntity.ok(configService.getConfig(tenantId));
    }

    /**
     * Save ML configuration for tenant.
     */
    @PostMapping
    public ResponseEntity<PoMlConfigDto> saveConfig(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody PoMlConfigDto config) {
        log.info("Saving ML config for tenant: {}", tenantId);
        return ResponseEntity.ok(configService.saveConfig(tenantId, config));
    }

    /**
     * Reset ML configuration to defaults.
     */
    @PostMapping("/reset")
    public ResponseEntity<PoMlConfigDto> resetConfig(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("Resetting ML config to defaults for tenant: {}", tenantId);
        return ResponseEntity.ok(configService.resetToDefaults(tenantId));
    }
}
