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
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.DataAggregationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Defines aggregation rules from child assets to parent assets.
 * Used to roll up telemetry data in asset hierarchies.
 */
@Schema
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DataAggregation extends BaseDataWithAdditionalInfo<DataAggregationId> implements HasTenantId, HasName {

    private static final long serialVersionUID = 1L;

    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "moduleKey", min = 1, max = 50)
    private String moduleKey;

    @NoXss
    @Length(fieldName = "name", min = 1, max = 255)
    private String name;

    @NoXss
    @Length(fieldName = "sourceAssetType", min = 1, max = 100)
    private String sourceAssetType;

    @NoXss
    @Length(fieldName = "targetAssetType", min = 1, max = 100)
    private String targetAssetType;

    @NoXss
    @Length(fieldName = "sourceKey", min = 1, max = 255)
    private String sourceKey;

    @NoXss
    @Length(fieldName = "targetKey", min = 1, max = 255)
    private String targetKey;

    private AggregationType aggregationType;

    private long aggregationWindow;

    @NoXss
    @Length(fieldName = "filterExpression", max = 1024)
    private String filterExpression;

    private boolean active;

    public DataAggregation() {
        super();
        this.aggregationType = AggregationType.SUM;
        this.aggregationWindow = 60000; // 1 minute default
        this.active = true;
    }

    public DataAggregation(DataAggregationId id) {
        super(id);
    }

    public DataAggregation(DataAggregation aggregation) {
        super(aggregation);
        this.tenantId = aggregation.getTenantId();
        this.moduleKey = aggregation.getModuleKey();
        this.name = aggregation.getName();
        this.sourceAssetType = aggregation.getSourceAssetType();
        this.targetAssetType = aggregation.getTargetAssetType();
        this.sourceKey = aggregation.getSourceKey();
        this.targetKey = aggregation.getTargetKey();
        this.aggregationType = aggregation.getAggregationType();
        this.aggregationWindow = aggregation.getAggregationWindow();
        this.filterExpression = aggregation.getFilterExpression();
        this.active = aggregation.isActive();
    }

    @Schema(description = "JSON object with the Data Aggregation Id. " +
            "Specify this field to update the aggregation. " +
            "Referencing non-existing Id will cause error. " +
            "Omit this field to create new aggregation.")
    @Override
    public DataAggregationId getId() {
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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Module key identifier", example = "CT")
    public String getModuleKey() {
        return moduleKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Aggregation name", example = "Active Units Count")
    @Override
    public String getName() {
        return name;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Source asset type (child)", example = "CT_UNIT")
    public String getSourceAssetType() {
        return sourceAssetType;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Target asset type (parent)", example = "CT_LOCATION")
    public String getTargetAssetType() {
        return targetAssetType;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Source telemetry key to aggregate", example = "status")
    public String getSourceKey() {
        return sourceKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Target telemetry key for result", example = "active_units_count")
    public String getTargetKey() {
        return targetKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Aggregation type", example = "COUNT")
    public AggregationType getAggregationType() {
        return aggregationType;
    }

    @Schema(description = "Aggregation window in milliseconds", example = "300000")
    public long getAggregationWindow() {
        return aggregationWindow;
    }

    @Schema(description = "Optional filter expression for aggregation")
    public String getFilterExpression() {
        return filterExpression;
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
        return "DataAggregation [id=" + id +
                ", tenantId=" + tenantId +
                ", moduleKey=" + moduleKey +
                ", name=" + name +
                ", sourceAssetType=" + sourceAssetType +
                ", targetAssetType=" + targetAssetType +
                ", sourceKey=" + sourceKey +
                ", targetKey=" + targetKey +
                ", aggregationType=" + aggregationType +
                ", aggregationWindow=" + aggregationWindow +
                ", active=" + active +
                ", createdTime=" + createdTime + "]";
    }
}
