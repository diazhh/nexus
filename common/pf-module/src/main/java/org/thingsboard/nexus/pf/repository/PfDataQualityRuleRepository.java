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
package org.thingsboard.nexus.pf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.nexus.pf.model.PfDataQualityRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Data Quality Rules.
 */
@Repository
public interface PfDataQualityRuleRepository extends JpaRepository<PfDataQualityRule, UUID> {

    /**
     * Find rule by variable key (global rule)
     */
    Optional<PfDataQualityRule> findByVariableKeyAndEntityTypeIsNullAndEntityIdIsNullAndEnabledTrue(String variableKey);

    /**
     * Find rule by variable key and entity type
     */
    Optional<PfDataQualityRule> findByVariableKeyAndEntityTypeAndEntityIdIsNullAndEnabledTrue(
            String variableKey, String entityType);

    /**
     * Find rule by variable key and entity ID
     */
    Optional<PfDataQualityRule> findByVariableKeyAndEntityIdAndEnabledTrue(String variableKey, UUID entityId);

    /**
     * Find all rules for a variable (most specific to least specific)
     */
    @Query("SELECT r FROM PfDataQualityRule r WHERE r.variableKey = :variableKey AND r.enabled = true " +
            "AND (r.entityId = :entityId OR (r.entityType = :entityType AND r.entityId IS NULL) " +
            "OR (r.entityType IS NULL AND r.entityId IS NULL)) " +
            "ORDER BY CASE WHEN r.entityId IS NOT NULL THEN 1 " +
            "WHEN r.entityType IS NOT NULL THEN 2 ELSE 3 END")
    List<PfDataQualityRule> findApplicableRules(
            @Param("variableKey") String variableKey,
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId);

    /**
     * Find all rules for an entity type
     */
    List<PfDataQualityRule> findByEntityTypeAndEnabledTrue(String entityType);

    /**
     * Find all rules for a specific entity
     */
    List<PfDataQualityRule> findByEntityIdAndEnabledTrue(UUID entityId);

    /**
     * Find all global rules (no entity type or ID)
     */
    List<PfDataQualityRule> findByEntityTypeIsNullAndEntityIdIsNullAndEnabledTrue();

    /**
     * Find all enabled rules
     */
    List<PfDataQualityRule> findByEnabledTrue();

    /**
     * Check if rule exists for variable
     */
    boolean existsByVariableKeyAndEntityTypeAndEntityId(String variableKey, String entityType, UUID entityId);
}
