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
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.NexusModuleId;
import org.thingsboard.server.common.data.nexus.NexusModule;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

/**
 * JPA Entity for nx_module table.
 * Represents a NEXUS module that can be assigned to tenants.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "nx_module")
public final class NexusModuleEntity extends BaseSqlEntity<NexusModule> {

    @Column(name = "module_key", unique = true, nullable = false)
    private String moduleKey;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "description")
    private String description;

    @Column(name = "version")
    private String version;

    @Column(name = "category")
    private String category;

    @Column(name = "icon")
    private String icon;

    @Column(name = "route_path")
    private String routePath;

    @Column(name = "is_system_module")
    private boolean isSystemModule;

    @Column(name = "is_available")
    private boolean isAvailable;

    @Column(name = "display_order")
    private int displayOrder;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    public NexusModuleEntity() {
        super();
    }

    public NexusModuleEntity(NexusModule module) {
        super(module);
        this.moduleKey = module.getModuleKey();
        this.moduleName = module.getName();
        this.description = module.getDescription();
        this.version = module.getVersion();
        this.category = module.getCategory();
        this.icon = module.getIcon();
        this.routePath = module.getRoutePath();
        this.isSystemModule = module.isSystemModule();
        this.isAvailable = module.isAvailable();
        this.displayOrder = module.getDisplayOrder();
        this.additionalInfo = module.getAdditionalInfo();
    }

    @Override
    public NexusModule toData() {
        NexusModule module = new NexusModule(new NexusModuleId(id));
        module.setCreatedTime(createdTime);
        module.setModuleKey(moduleKey);
        module.setName(moduleName);
        module.setDescription(description);
        module.setVersion(version);
        module.setCategory(category);
        module.setIcon(icon);
        module.setRoutePath(routePath);
        module.setSystemModule(isSystemModule);
        module.setAvailable(isAvailable);
        module.setDisplayOrder(displayOrder);
        module.setAdditionalInfo(additionalInfo);
        return module;
    }
}
