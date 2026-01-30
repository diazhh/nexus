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
package org.thingsboard.server.dao.nexus;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.nexus.MappingTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO interface for MappingTemplate operations.
 */
public interface MappingTemplateDao extends Dao<MappingTemplate> {

    /**
     * Save or update a mapping template
     */
    MappingTemplate save(TenantId tenantId, MappingTemplate template);

    /**
     * Find template by ID
     */
    MappingTemplate findById(UUID templateId);

    /**
     * Find all templates for a tenant with pagination
     */
    PageData<MappingTemplate> findByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find templates by module key
     */
    List<MappingTemplate> findByModuleKey(UUID tenantId, String moduleKey);

    /**
     * Find templates by module key with pagination
     */
    PageData<MappingTemplate> findByModuleKey(UUID tenantId, String moduleKey, PageLink pageLink);

    /**
     * Find the default template for a module
     */
    Optional<MappingTemplate> findDefaultByModuleKey(UUID tenantId, String moduleKey);

    /**
     * Find active templates for a tenant
     */
    List<MappingTemplate> findActiveByTenantId(UUID tenantId);

    /**
     * Find active templates by module key
     */
    List<MappingTemplate> findActiveByModuleKey(UUID tenantId, String moduleKey);

    /**
     * Find template by name within a module
     */
    Optional<MappingTemplate> findByName(UUID tenantId, String moduleKey, String name);

    /**
     * Check if a template name already exists within a module
     */
    boolean existsByName(UUID tenantId, String moduleKey, String name);

    /**
     * Delete template by ID
     */
    boolean removeById(UUID templateId);

    /**
     * Delete all templates for a tenant
     */
    void deleteByTenantId(UUID tenantId);
}
