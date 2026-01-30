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
import org.thingsboard.server.common.data.id.MappingTemplateId;
import org.thingsboard.server.common.data.id.MappingTemplateRuleId;
import org.thingsboard.server.common.data.nexus.MappingTemplateRule;
import org.thingsboard.server.common.data.nexus.TransformationType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

/**
 * JPA Entity for nx_mapping_template_rule table.
 * Defines mapping rules within a template.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "nx_mapping_template_rule")
public final class MappingTemplateRuleEntity extends BaseSqlEntity<MappingTemplateRule> {

    @Column(name = "template_id", columnDefinition = "uuid", nullable = false)
    private UUID templateId;

    @Column(name = "source_key", nullable = false)
    private String sourceKey;

    @Column(name = "target_key", nullable = false)
    private String targetKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "transformation_type")
    private TransformationType transformationType;

    @Convert(converter = JsonConverter.class)
    @Column(name = "transformation_config")
    private JsonNode transformationConfig;

    @Column(name = "unit_source")
    private String unitSource;

    @Column(name = "unit_target")
    private String unitTarget;

    @Column(name = "description")
    private String description;

    @Column(name = "priority")
    private int priority;

    @Column(name = "is_active")
    private boolean isActive;

    public MappingTemplateRuleEntity() {
        super();
    }

    public MappingTemplateRuleEntity(MappingTemplateRule rule) {
        super(rule);
        this.templateId = getUuid(rule.getTemplateId());
        this.sourceKey = rule.getSourceKey();
        this.targetKey = rule.getTargetKey();
        this.transformationType = rule.getTransformationType();
        this.transformationConfig = rule.getTransformationConfig();
        this.unitSource = rule.getUnitSource();
        this.unitTarget = rule.getUnitTarget();
        this.description = rule.getDescription();
        this.priority = rule.getPriority();
        this.isActive = rule.isActive();
    }

    @Override
    public MappingTemplateRule toData() {
        MappingTemplateRule rule = new MappingTemplateRule(new MappingTemplateRuleId(id));
        rule.setCreatedTime(createdTime);
        rule.setTemplateId(getEntityId(templateId, MappingTemplateId::new));
        rule.setSourceKey(sourceKey);
        rule.setTargetKey(targetKey);
        rule.setTransformationType(transformationType);
        rule.setTransformationConfig(transformationConfig);
        rule.setUnitSource(unitSource);
        rule.setUnitTarget(unitTarget);
        rule.setDescription(description);
        rule.setPriority(priority);
        rule.setActive(isActive);
        return rule;
    }
}
