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
package org.thingsboard.rule.engine.nexus;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * Configuration for NEXUS Data Distribution Node.
 *
 * This node distributes telemetry data from devices (RTUs, PLCs, sensors)
 * to their corresponding Digital Twin assets based on configured mapping rules.
 */
@Data
public class TbNexusDataDistributionNodeConfiguration implements NodeConfiguration<TbNexusDataDistributionNodeConfiguration> {

    /**
     * Module key to filter configurations.
     * Use "AUTO" to automatically detect the module from the device's data source configuration.
     */
    private String moduleKey;

    /**
     * Distribution mode:
     * - DIRECT: Send data directly to the configured target asset
     * - MAPPED: Transform data according to mapping rules before distribution
     * - HIERARCHICAL: Distribute to asset hierarchy (target + related assets)
     */
    private String distributionMode;

    /**
     * Error handling strategy:
     * - SKIP: Skip failed distributions silently
     * - FAIL: Fail the entire message on first error
     * - LOG_AND_CONTINUE: Log errors but continue processing remaining keys
     */
    private String errorHandling;

    /**
     * Whether to log distribution operations for auditing.
     */
    private boolean enableAuditLog;

    /**
     * Whether to propagate data to parent assets in the hierarchy.
     */
    private boolean propagateToParents;

    /**
     * Whether to use server timestamp instead of message timestamp.
     */
    private boolean useServerTs;

    /**
     * Default TTL for telemetry data in seconds (0 = use tenant profile default).
     */
    private long defaultTtlSeconds;

    @Override
    public TbNexusDataDistributionNodeConfiguration defaultConfiguration() {
        TbNexusDataDistributionNodeConfiguration config = new TbNexusDataDistributionNodeConfiguration();
        config.setModuleKey("AUTO");
        config.setDistributionMode("MAPPED");
        config.setErrorHandling("LOG_AND_CONTINUE");
        config.setEnableAuditLog(true);
        config.setPropagateToParents(false);
        config.setUseServerTs(false);
        config.setDefaultTtlSeconds(0);
        return config;
    }
}
