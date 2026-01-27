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
package org.thingsboard.server.common.data.template;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.TemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
public class Template extends BaseData<TemplateId> implements HasName, HasTenantId {

    private static final long serialVersionUID = 1L;

    private TenantId tenantId;
    
    @NoXss
    @Length(fieldName = "name")
    private String name;
    
    private transient JsonNode configuration;

    @Getter
    @Setter
    private byte[] configurationBytes;

    public Template() {
        super();
    }

    public Template(TemplateId id) {
        super(id);
    }

    public Template(Template template) {
        super(template);
        this.tenantId = template.getTenantId();
        this.name = template.getName();
        this.configuration = template.getConfiguration();
        this.configurationBytes = template.getConfigurationBytes();
    }

    @Schema(description = "JSON object with tenant id", accessMode = Schema.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Schema(description = "Unique template name", example = "My Template")
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Schema(description = "JSON object with template configuration", 
            implementation = com.fasterxml.jackson.databind.JsonNode.class)
    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }
}
