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
package org.thingsboard.server.dao.template;

import org.thingsboard.server.common.data.template.TemplateDefinitionDto;
import org.thingsboard.server.common.data.template.TemplateInstanceDto;
import org.thingsboard.server.common.data.template.TemplateVersionDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateDefinitionDaoService {

    TemplateDefinitionDto saveTemplateDefinition(TemplateDefinitionDto dto);

    Optional<TemplateDefinitionDto> findById(UUID id);

    Optional<TemplateDefinitionDto> findByTemplateCode(String templateCode);

    List<TemplateDefinitionDto> findByModuleAndTenant(String moduleCode, UUID tenantId, Boolean isActive);

    List<TemplateDefinitionDto> findByModuleAndEntityTypeAndTenant(String moduleCode, String entityType, UUID tenantId, Boolean isActive);

    List<TemplateDefinitionDto> findByTenantAndActive(UUID tenantId, Boolean isActive);

    long countByTenant(UUID tenantId);

    void deleteById(UUID id);

    // Version operations
    TemplateVersionDto saveVersion(TemplateVersionDto dto);

    List<TemplateVersionDto> findVersionsByTemplateId(UUID templateId);

    long countVersionsByTemplateId(UUID templateId);

    void deleteVersionsByTemplateId(UUID templateId);

    // Instance operations
    TemplateInstanceDto saveInstance(TemplateInstanceDto dto);

    List<TemplateInstanceDto> findInstancesByTemplateId(UUID templateId);

    List<TemplateInstanceDto> findInstancesByTenant(UUID tenantId);

    long countInstancesByTemplateId(UUID templateId);
}
