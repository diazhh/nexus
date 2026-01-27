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

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "template_definitions")
public class TemplateDefinitionEntity extends BaseSqlEntity<TemplateDefinitionEntity> {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "template_code", unique = true, nullable = false, length = 100)
    private String templateCode;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "module_code", nullable = false, length = 50)
    private String moduleCode;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "is_active")
    private Boolean isActive;

    @Type(JsonBinaryType.class)
    @Column(name = "template_structure", columnDefinition = "jsonb", nullable = false)
    private String templateStructure;

    @Type(JsonBinaryType.class)
    @Column(name = "required_variables", columnDefinition = "jsonb", nullable = false)
    private String requiredVariables;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_time")
    private Long updatedTime;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    public TemplateDefinitionEntity() {
        super();
    }

    @Override
    public TemplateDefinitionEntity toData() {
        return this;
    }
}
