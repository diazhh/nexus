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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DataSourceConfigId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.UUID;

/**
 * Configures the relationship between a device (data source) and Digital Twin assets.
 * This entity defines how telemetry from a device should be distributed to target assets.
 */
@Schema
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DataSourceConfig extends BaseDataWithAdditionalInfo<DataSourceConfigId> implements HasTenantId {

    private static final long serialVersionUID = 1L;

    private TenantId tenantId;
    private DeviceId deviceId;

    @NoXss
    @Length(fieldName = "moduleKey", min = 1, max = 50)
    private String moduleKey;

    private AssetId targetAssetId;

    @NoXss
    @Length(fieldName = "targetAssetType", max = 100)
    private String targetAssetType;

    private DistributionMode distributionMode;

    private JsonNode mappingConfig;

    private boolean active;

    public DataSourceConfig() {
        super();
        this.distributionMode = DistributionMode.MAPPED;
        this.active = true;
    }

    public DataSourceConfig(DataSourceConfigId id) {
        super(id);
    }

    public DataSourceConfig(DataSourceConfig config) {
        super(config);
        this.tenantId = config.getTenantId();
        this.deviceId = config.getDeviceId();
        this.moduleKey = config.getModuleKey();
        this.targetAssetId = config.getTargetAssetId();
        this.targetAssetType = config.getTargetAssetType();
        this.distributionMode = config.getDistributionMode();
        this.mappingConfig = config.getMappingConfig();
        this.active = config.isActive();
    }

    @Schema(description = "JSON object with the Data Source Config Id. " +
            "Specify this field to update the configuration. " +
            "Referencing non-existing Id will cause error. " +
            "Omit this field to create new configuration.")
    @Override
    public DataSourceConfigId getId() {
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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with the Device Id of the data source")
    public DeviceId getDeviceId() {
        return deviceId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Module key identifier", example = "CT")
    public String getModuleKey() {
        return moduleKey;
    }

    @Schema(description = "JSON object with target Asset Id (optional)")
    public AssetId getTargetAssetId() {
        return targetAssetId;
    }

    @Schema(description = "Target asset type", example = "CT_UNIT")
    public String getTargetAssetType() {
        return targetAssetType;
    }

    @Schema(description = "Distribution mode", example = "MAPPED")
    public DistributionMode getDistributionMode() {
        return distributionMode;
    }

    @Schema(description = "JSON configuration for data mapping")
    public JsonNode getMappingConfig() {
        return mappingConfig;
    }

    @Schema(description = "Active status flag", example = "true")
    public boolean isActive() {
        return active;
    }

    @Schema(description = "Additional parameters", implementation = JsonNode.class)
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    @Override
    public String toString() {
        return "DataSourceConfig [id=" + id +
                ", tenantId=" + tenantId +
                ", deviceId=" + deviceId +
                ", moduleKey=" + moduleKey +
                ", targetAssetId=" + targetAssetId +
                ", targetAssetType=" + targetAssetType +
                ", distributionMode=" + distributionMode +
                ", active=" + active +
                ", createdTime=" + createdTime + "]";
    }
}
