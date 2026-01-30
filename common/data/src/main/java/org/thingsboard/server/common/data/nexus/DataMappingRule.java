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
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.DataMappingRuleId;
import org.thingsboard.server.common.data.id.DataSourceConfigId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Defines how telemetry data is transformed and distributed from source to target.
 * Each rule maps a source telemetry key to a target key with optional transformation.
 */
@Schema
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DataMappingRule extends BaseData<DataMappingRuleId> {

    private static final long serialVersionUID = 1L;

    private DataSourceConfigId dataSourceConfigId;

    @NoXss
    @Length(fieldName = "sourceKey", min = 1, max = 255)
    private String sourceKey;

    @NoXss
    @Length(fieldName = "targetKey", min = 1, max = 255)
    private String targetKey;

    @NoXss
    @Length(fieldName = "targetAssetRelation", max = 100)
    private String targetAssetRelation;

    @NoXss
    @Length(fieldName = "targetAssetType", max = 100)
    private String targetAssetType;

    private TransformationType transformationType;

    private JsonNode transformationConfig;

    private int priority;

    private boolean active;

    public DataMappingRule() {
        super();
        this.transformationType = TransformationType.DIRECT;
        this.priority = 0;
        this.active = true;
    }

    public DataMappingRule(DataMappingRuleId id) {
        super(id);
    }

    public DataMappingRule(DataMappingRule rule) {
        super(rule);
        this.dataSourceConfigId = rule.getDataSourceConfigId();
        this.sourceKey = rule.getSourceKey();
        this.targetKey = rule.getTargetKey();
        this.targetAssetRelation = rule.getTargetAssetRelation();
        this.targetAssetType = rule.getTargetAssetType();
        this.transformationType = rule.getTransformationType();
        this.transformationConfig = rule.getTransformationConfig();
        this.priority = rule.getPriority();
        this.active = rule.isActive();
    }

    @Schema(description = "JSON object with the Data Mapping Rule Id. " +
            "Specify this field to update the rule. " +
            "Referencing non-existing Id will cause error. " +
            "Omit this field to create new rule.")
    @Override
    public DataMappingRuleId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with the Data Source Config Id")
    public DataSourceConfigId getDataSourceConfigId() {
        return dataSourceConfigId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Source telemetry key", example = "temp_1")
    public String getSourceKey() {
        return sourceKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Target telemetry key", example = "temperature")
    public String getTargetKey() {
        return targetKey;
    }

    @Schema(description = "Relation to target asset", example = "Contains")
    public String getTargetAssetRelation() {
        return targetAssetRelation;
    }

    @Schema(description = "Type of target asset", example = "CT_UNIT")
    public String getTargetAssetType() {
        return targetAssetType;
    }

    @Schema(description = "Transformation type", example = "DIRECT")
    public TransformationType getTransformationType() {
        return transformationType;
    }

    @Schema(description = "JSON configuration for transformation")
    public JsonNode getTransformationConfig() {
        return transformationConfig;
    }

    @Schema(description = "Priority order for rule execution", example = "0")
    public int getPriority() {
        return priority;
    }

    @Schema(description = "Active status flag", example = "true")
    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "DataMappingRule [id=" + id +
                ", dataSourceConfigId=" + dataSourceConfigId +
                ", sourceKey=" + sourceKey +
                ", targetKey=" + targetKey +
                ", transformationType=" + transformationType +
                ", priority=" + priority +
                ", active=" + active +
                ", createdTime=" + createdTime + "]";
    }
}
