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
import org.thingsboard.server.common.data.nexus.MappingTemplateRule;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

/**
 * DAO interface for MappingTemplateRule operations.
 */
public interface MappingTemplateRuleDao extends Dao<MappingTemplateRule> {

    /**
     * Save or update a mapping template rule
     */
    MappingTemplateRule save(TenantId tenantId, MappingTemplateRule rule);

    /**
     * Find rule by ID
     */
    MappingTemplateRule findById(UUID ruleId);

    /**
     * Find all rules for a template
     */
    List<MappingTemplateRule> findByTemplateId(UUID templateId);

    /**
     * Find active rules for a template (ordered by priority)
     */
    List<MappingTemplateRule> findActiveByTemplateId(UUID templateId);

    /**
     * Find rules by source key within a template
     */
    List<MappingTemplateRule> findBySourceKey(UUID templateId, String sourceKey);

    /**
     * Check if a source key already exists within a template
     */
    boolean existsBySourceKey(UUID templateId, String sourceKey);

    /**
     * Delete rule by ID
     */
    boolean removeById(UUID ruleId);

    /**
     * Delete all rules for a template
     */
    void deleteByTemplateId(UUID templateId);

    /**
     * Count rules for a template
     */
    long countByTemplateId(UUID templateId);
}
