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
@Table(name = "template_instances")
public class TemplateInstanceEntity extends BaseSqlEntity<TemplateInstanceEntity> {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "template_version", nullable = false, length = 20)
    private String templateVersion;

    @Column(name = "root_asset_id", nullable = false)
    private UUID rootAssetId;

    @Type(JsonBinaryType.class)
    @Column(name = "instance_variables", columnDefinition = "jsonb", nullable = false)
    private String instanceVariables;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    public TemplateInstanceEntity() {
        super();
    }

    @Override
    public TemplateInstanceEntity toData() {
        return this;
    }
}
