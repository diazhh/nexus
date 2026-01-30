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
package org.thingsboard.server.dao.sql.nexus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.MappingTemplateRuleEntity;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for MappingTemplateRule entities.
 */
public interface MappingTemplateRuleRepository extends JpaRepository<MappingTemplateRuleEntity, UUID> {

    /**
     * Find all rules for a template
     */
    @Query("SELECT r FROM MappingTemplateRuleEntity r WHERE r.templateId = :templateId ORDER BY r.priority, r.sourceKey")
    List<MappingTemplateRuleEntity> findByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Find active rules for a template (ordered by priority)
     */
    @Query("SELECT r FROM MappingTemplateRuleEntity r WHERE r.templateId = :templateId AND r.isActive = true ORDER BY r.priority, r.sourceKey")
    List<MappingTemplateRuleEntity> findActiveByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Find rules by source key within a template
     */
    @Query("SELECT r FROM MappingTemplateRuleEntity r WHERE r.templateId = :templateId AND r.sourceKey = :sourceKey ORDER BY r.priority")
    List<MappingTemplateRuleEntity> findByTemplateIdAndSourceKey(@Param("templateId") UUID templateId, @Param("sourceKey") String sourceKey);

    /**
     * Check if a source key already exists within a template
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM MappingTemplateRuleEntity r WHERE r.templateId = :templateId AND r.sourceKey = :sourceKey")
    boolean existsByTemplateIdAndSourceKey(@Param("templateId") UUID templateId, @Param("sourceKey") String sourceKey);

    /**
     * Delete all rules for a template
     */
    @Modifying
    @Query("DELETE FROM MappingTemplateRuleEntity r WHERE r.templateId = :templateId")
    void deleteByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Count rules for a template
     */
    @Query("SELECT COUNT(r) FROM MappingTemplateRuleEntity r WHERE r.templateId = :templateId")
    long countByTemplateId(@Param("templateId") UUID templateId);
}
