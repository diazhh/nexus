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
package org.thingsboard.server.common.data.nexus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DistributionLogId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Audit log entry for data distribution operations.
 * Records the status and details of each distribution operation.
 */
@Schema
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DistributionLog extends BaseData<DistributionLogId> implements HasTenantId {

    private static final long serialVersionUID = 1L;

    private TenantId tenantId;
    private DeviceId deviceId;
    private AssetId targetAssetId;

    @NoXss
    @Length(fieldName = "moduleKey", max = 50)
    private String moduleKey;

    @NoXss
    @Length(fieldName = "operationType", min = 1, max = 50)
    private String operationType;

    private DistributionStatus status;

    private int keysProcessed;

    @NoXss
    @Length(fieldName = "errorMessage", max = 4096)
    private String errorMessage;

    private long processingTimeMs;

    public DistributionLog() {
        super();
        this.keysProcessed = 0;
        this.processingTimeMs = 0;
    }

    public DistributionLog(DistributionLogId id) {
        super(id);
    }

    public DistributionLog(DistributionLog log) {
        super(log);
        this.tenantId = log.getTenantId();
        this.deviceId = log.getDeviceId();
        this.targetAssetId = log.getTargetAssetId();
        this.moduleKey = log.getModuleKey();
        this.operationType = log.getOperationType();
        this.status = log.getStatus();
        this.keysProcessed = log.getKeysProcessed();
        this.errorMessage = log.getErrorMessage();
        this.processingTimeMs = log.getProcessingTimeMs();
    }

    @Schema(description = "JSON object with the Distribution Log Id", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public DistributionLogId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(description = "JSON object with the Device Id of the data source")
    public DeviceId getDeviceId() {
        return deviceId;
    }

    @Schema(description = "JSON object with target Asset Id")
    public AssetId getTargetAssetId() {
        return targetAssetId;
    }

    @Schema(description = "Module key identifier", example = "CT")
    public String getModuleKey() {
        return moduleKey;
    }

    @Schema(description = "Operation type", example = "DISTRIBUTE")
    public String getOperationType() {
        return operationType;
    }

    @Schema(description = "Distribution status", example = "SUCCESS")
    public DistributionStatus getStatus() {
        return status;
    }

    @Schema(description = "Number of telemetry keys processed", example = "5")
    public int getKeysProcessed() {
        return keysProcessed;
    }

    @Schema(description = "Error message if distribution failed")
    public String getErrorMessage() {
        return errorMessage;
    }

    @Schema(description = "Processing time in milliseconds", example = "125")
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    @Override
    public String toString() {
        return "DistributionLog [id=" + id +
                ", tenantId=" + tenantId +
                ", deviceId=" + deviceId +
                ", targetAssetId=" + targetAssetId +
                ", moduleKey=" + moduleKey +
                ", operationType=" + operationType +
                ", status=" + status +
                ", keysProcessed=" + keysProcessed +
                ", processingTimeMs=" + processingTimeMs +
                ", createdTime=" + createdTime + "]";
    }
}
