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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DistributionLogId;
import org.thingsboard.server.common.data.nexus.DistributionLog;
import org.thingsboard.server.common.data.nexus.DistributionStatus;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

/**
 * JPA Entity for nx_distribution_log table.
 * Audit log for data distribution operations.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "nx_distribution_log")
public final class DistributionLogEntity extends BaseSqlEntity<DistributionLog> {

    @Column(name = "tenant_id", columnDefinition = "uuid", nullable = false)
    private UUID tenantId;

    @Column(name = "device_id", columnDefinition = "uuid", nullable = false)
    private UUID deviceId;

    @Column(name = "target_asset_id", columnDefinition = "uuid")
    private UUID targetAssetId;

    @Column(name = "module_key")
    private String moduleKey;

    @Column(name = "operation_type", nullable = false)
    private String operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DistributionStatus status;

    @Column(name = "keys_processed")
    private int keysProcessed;

    @Column(name = "error_message", length = 4096)
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private long processingTimeMs;

    public DistributionLogEntity() {
        super();
    }

    public DistributionLogEntity(DistributionLog log) {
        super(log);
        this.tenantId = getTenantUuid(log.getTenantId());
        this.deviceId = getUuid(log.getDeviceId());
        this.targetAssetId = getUuid(log.getTargetAssetId());
        this.moduleKey = log.getModuleKey();
        this.operationType = log.getOperationType();
        this.status = log.getStatus();
        this.keysProcessed = log.getKeysProcessed();
        this.errorMessage = log.getErrorMessage();
        this.processingTimeMs = log.getProcessingTimeMs();
    }

    @Override
    public DistributionLog toData() {
        DistributionLog log = new DistributionLog(new DistributionLogId(id));
        log.setCreatedTime(createdTime);
        log.setTenantId(getTenantId(tenantId));
        log.setDeviceId(getEntityId(deviceId, DeviceId::new));
        log.setTargetAssetId(getEntityId(targetAssetId, AssetId::new));
        log.setModuleKey(moduleKey);
        log.setOperationType(operationType);
        log.setStatus(status);
        log.setKeysProcessed(keysProcessed);
        log.setErrorMessage(errorMessage);
        log.setProcessingTimeMs(processingTimeMs);
        return log;
    }
}
