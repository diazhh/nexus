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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DataSourceConfigId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.nexus.DataSourceConfig;
import org.thingsboard.server.common.data.nexus.DistributionMode;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

/**
 * JPA Entity for nx_data_source_config table.
 * Configures the relationship between a device and Digital Twin assets.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "nx_data_source_config")
public final class DataSourceConfigEntity extends BaseSqlEntity<DataSourceConfig> {

    @Column(name = "tenant_id", columnDefinition = "uuid", nullable = false)
    private UUID tenantId;

    @Column(name = "device_id", columnDefinition = "uuid", nullable = false)
    private UUID deviceId;

    @Column(name = "module_key", nullable = false)
    private String moduleKey;

    @Column(name = "target_asset_id", columnDefinition = "uuid")
    private UUID targetAssetId;

    @Column(name = "target_asset_type")
    private String targetAssetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_mode")
    private DistributionMode distributionMode;

    @Convert(converter = JsonConverter.class)
    @Column(name = "mapping_config")
    private JsonNode mappingConfig;

    @Column(name = "is_active")
    private boolean isActive;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    public DataSourceConfigEntity() {
        super();
    }

    public DataSourceConfigEntity(DataSourceConfig config) {
        super(config);
        this.tenantId = getTenantUuid(config.getTenantId());
        this.deviceId = getUuid(config.getDeviceId());
        this.moduleKey = config.getModuleKey();
        this.targetAssetId = getUuid(config.getTargetAssetId());
        this.targetAssetType = config.getTargetAssetType();
        this.distributionMode = config.getDistributionMode();
        this.mappingConfig = config.getMappingConfig();
        this.isActive = config.isActive();
        this.additionalInfo = config.getAdditionalInfo();
    }

    @Override
    public DataSourceConfig toData() {
        DataSourceConfig config = new DataSourceConfig(new DataSourceConfigId(id));
        config.setCreatedTime(createdTime);
        config.setTenantId(getTenantId(tenantId));
        config.setDeviceId(getEntityId(deviceId, DeviceId::new));
        config.setModuleKey(moduleKey);
        config.setTargetAssetId(getEntityId(targetAssetId, AssetId::new));
        config.setTargetAssetType(targetAssetType);
        config.setDistributionMode(distributionMode);
        config.setMappingConfig(mappingConfig);
        config.setActive(isActive);
        config.setAdditionalInfo(additionalInfo);
        return config;
    }
}
