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
import org.thingsboard.server.common.data.id.DataMappingRuleId;
import org.thingsboard.server.common.data.id.DataSourceConfigId;
import org.thingsboard.server.common.data.nexus.DataMappingRule;
import org.thingsboard.server.common.data.nexus.TransformationType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

/**
 * JPA Entity for nx_data_mapping_rule table.
 * Defines how telemetry data is transformed and distributed.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "nx_data_mapping_rule")
public final class DataMappingRuleEntity extends BaseSqlEntity<DataMappingRule> {

    @Column(name = "data_source_config_id", columnDefinition = "uuid", nullable = false)
    private UUID dataSourceConfigId;

    @Column(name = "source_key", nullable = false)
    private String sourceKey;

    @Column(name = "target_key", nullable = false)
    private String targetKey;

    @Column(name = "target_asset_relation")
    private String targetAssetRelation;

    @Column(name = "target_asset_type")
    private String targetAssetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transformation_type")
    private TransformationType transformationType;

    @Convert(converter = JsonConverter.class)
    @Column(name = "transformation_config")
    private JsonNode transformationConfig;

    @Column(name = "priority")
    private int priority;

    @Column(name = "is_active")
    private boolean isActive;

    public DataMappingRuleEntity() {
        super();
    }

    public DataMappingRuleEntity(DataMappingRule rule) {
        super(rule);
        this.dataSourceConfigId = getUuid(rule.getDataSourceConfigId());
        this.sourceKey = rule.getSourceKey();
        this.targetKey = rule.getTargetKey();
        this.targetAssetRelation = rule.getTargetAssetRelation();
        this.targetAssetType = rule.getTargetAssetType();
        this.transformationType = rule.getTransformationType();
        this.transformationConfig = rule.getTransformationConfig();
        this.priority = rule.getPriority();
        this.isActive = rule.isActive();
    }

    @Override
    public DataMappingRule toData() {
        DataMappingRule rule = new DataMappingRule(new DataMappingRuleId(id));
        rule.setCreatedTime(createdTime);
        rule.setDataSourceConfigId(getEntityId(dataSourceConfigId, DataSourceConfigId::new));
        rule.setSourceKey(sourceKey);
        rule.setTargetKey(targetKey);
        rule.setTargetAssetRelation(targetAssetRelation);
        rule.setTargetAssetType(targetAssetType);
        rule.setTransformationType(transformationType);
        rule.setTransformationConfig(transformationConfig);
        rule.setPriority(priority);
        rule.setActive(isActive);
        return rule;
    }
}
