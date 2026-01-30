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
import org.thingsboard.server.common.data.id.MappingTemplateId;
import org.thingsboard.server.common.data.id.MappingTemplateRuleId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Defines a single mapping rule within a MappingTemplate.
 * Each rule maps a source telemetry key to a target key with optional transformation.
 */
@Schema
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MappingTemplateRule extends BaseData<MappingTemplateRuleId> {

    private static final long serialVersionUID = 1L;

    private MappingTemplateId templateId;

    @NoXss
    @Length(fieldName = "sourceKey", min = 1, max = 255)
    private String sourceKey;

    @NoXss
    @Length(fieldName = "targetKey", min = 1, max = 255)
    private String targetKey;

    private TransformationType transformationType;

    private JsonNode transformationConfig;

    @NoXss
    @Length(fieldName = "unitSource", max = 50)
    private String unitSource;

    @NoXss
    @Length(fieldName = "unitTarget", max = 50)
    private String unitTarget;

    @NoXss
    @Length(fieldName = "description", max = 500)
    private String description;

    private int priority;

    private boolean active;

    public MappingTemplateRule() {
        super();
        this.transformationType = TransformationType.DIRECT;
        this.priority = 0;
        this.active = true;
    }

    public MappingTemplateRule(MappingTemplateRuleId id) {
        super(id);
    }

    public MappingTemplateRule(MappingTemplateRule rule) {
        super(rule);
        this.templateId = rule.getTemplateId();
        this.sourceKey = rule.getSourceKey();
        this.targetKey = rule.getTargetKey();
        this.transformationType = rule.getTransformationType();
        this.transformationConfig = rule.getTransformationConfig();
        this.unitSource = rule.getUnitSource();
        this.unitTarget = rule.getUnitTarget();
        this.description = rule.getDescription();
        this.priority = rule.getPriority();
        this.active = rule.isActive();
    }

    @Schema(description = "JSON object with the Mapping Template Rule Id. " +
            "Specify this field to update the rule. " +
            "Referencing non-existing Id will cause error. " +
            "Omit this field to create new rule.")
    @Override
    public MappingTemplateRuleId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with the Mapping Template Id")
    public MappingTemplateId getTemplateId() {
        return templateId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Source telemetry key from device", example = "hyd_pressure_1")
    public String getSourceKey() {
        return sourceKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Target telemetry key for asset", example = "pressure")
    public String getTargetKey() {
        return targetKey;
    }

    @Schema(description = "Transformation type", example = "DIRECT")
    public TransformationType getTransformationType() {
        return transformationType;
    }

    @Schema(description = "JSON configuration for transformation (e.g., {factor: 1.0, offset: 0})")
    public JsonNode getTransformationConfig() {
        return transformationConfig;
    }

    @Schema(description = "Source unit of measurement", example = "PSI")
    public String getUnitSource() {
        return unitSource;
    }

    @Schema(description = "Target unit of measurement", example = "PSI")
    public String getUnitTarget() {
        return unitTarget;
    }

    @Schema(description = "Rule description", example = "Hydraulic pressure mapping")
    public String getDescription() {
        return description;
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
        return "MappingTemplateRule [id=" + id +
                ", templateId=" + templateId +
                ", sourceKey=" + sourceKey +
                ", targetKey=" + targetKey +
                ", transformationType=" + transformationType +
                ", unitSource=" + unitSource +
                ", unitTarget=" + unitTarget +
                ", priority=" + priority +
                ", active=" + active +
                ", createdTime=" + createdTime + "]";
    }
}
