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
import org.thingsboard.server.common.data.id.DataAggregationId;
import org.thingsboard.server.common.data.nexus.AggregationType;
import org.thingsboard.server.common.data.nexus.DataAggregation;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

/**
 * JPA Entity for nx_data_aggregation table.
 * Defines aggregation rules from child to parent assets.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "nx_data_aggregation")
public final class DataAggregationEntity extends BaseSqlEntity<DataAggregation> {

    @Column(name = "tenant_id", columnDefinition = "uuid", nullable = false)
    private UUID tenantId;

    @Column(name = "module_key", nullable = false)
    private String moduleKey;

    @Column(name = "aggregation_name", nullable = false)
    private String aggregationName;

    @Column(name = "source_asset_type", nullable = false)
    private String sourceAssetType;

    @Column(name = "target_asset_type", nullable = false)
    private String targetAssetType;

    @Column(name = "source_key", nullable = false)
    private String sourceKey;

    @Column(name = "target_key", nullable = false)
    private String targetKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type", nullable = false)
    private AggregationType aggregationType;

    @Column(name = "aggregation_window")
    private long aggregationWindow;

    @Column(name = "filter_expression")
    private String filterExpression;

    @Column(name = "is_active")
    private boolean isActive;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    public DataAggregationEntity() {
        super();
    }

    public DataAggregationEntity(DataAggregation aggregation) {
        super(aggregation);
        this.tenantId = getTenantUuid(aggregation.getTenantId());
        this.moduleKey = aggregation.getModuleKey();
        this.aggregationName = aggregation.getName();
        this.sourceAssetType = aggregation.getSourceAssetType();
        this.targetAssetType = aggregation.getTargetAssetType();
        this.sourceKey = aggregation.getSourceKey();
        this.targetKey = aggregation.getTargetKey();
        this.aggregationType = aggregation.getAggregationType();
        this.aggregationWindow = aggregation.getAggregationWindow();
        this.filterExpression = aggregation.getFilterExpression();
        this.isActive = aggregation.isActive();
        this.additionalInfo = aggregation.getAdditionalInfo();
    }

    @Override
    public DataAggregation toData() {
        DataAggregation aggregation = new DataAggregation(new DataAggregationId(id));
        aggregation.setCreatedTime(createdTime);
        aggregation.setTenantId(getTenantId(tenantId));
        aggregation.setModuleKey(moduleKey);
        aggregation.setName(aggregationName);
        aggregation.setSourceAssetType(sourceAssetType);
        aggregation.setTargetAssetType(targetAssetType);
        aggregation.setSourceKey(sourceKey);
        aggregation.setTargetKey(targetKey);
        aggregation.setAggregationType(aggregationType);
        aggregation.setAggregationWindow(aggregationWindow);
        aggregation.setFilterExpression(filterExpression);
        aggregation.setActive(isActive);
        aggregation.setAdditionalInfo(additionalInfo);
        return aggregation;
    }
}
