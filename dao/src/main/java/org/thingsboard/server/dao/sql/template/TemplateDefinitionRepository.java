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
package org.thingsboard.server.dao.sql.template;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sql.TemplateDefinitionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateDefinitionRepository extends JpaRepository<TemplateDefinitionEntity, UUID> {

    Optional<TemplateDefinitionEntity> findByTemplateCode(String templateCode);

    List<TemplateDefinitionEntity> findByModuleCodeAndTenantIdAndIsActive(
            String moduleCode, UUID tenantId, Boolean isActive);

    List<TemplateDefinitionEntity> findByModuleCodeAndEntityTypeAndTenantIdAndIsActive(
            String moduleCode, String entityType, UUID tenantId, Boolean isActive);

    List<TemplateDefinitionEntity> findByTenantIdAndIsActive(UUID tenantId, Boolean isActive);

    long countByTenantId(UUID tenantId);
}
