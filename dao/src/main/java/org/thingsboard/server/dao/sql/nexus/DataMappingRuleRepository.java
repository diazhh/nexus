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
import org.thingsboard.server.dao.model.sql.DataMappingRuleEntity;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for DataMappingRule entities.
 */
public interface DataMappingRuleRepository extends JpaRepository<DataMappingRuleEntity, UUID> {

    /**
     * Find all rules for a data source configuration
     */
    @Query("SELECT r FROM DataMappingRuleEntity r WHERE r.dataSourceConfigId = :dataSourceConfigId ORDER BY r.priority")
    List<DataMappingRuleEntity> findByDataSourceConfigId(@Param("dataSourceConfigId") UUID dataSourceConfigId);

    /**
     * Find active rules for a data source configuration (ordered by priority)
     */
    @Query("SELECT r FROM DataMappingRuleEntity r WHERE r.dataSourceConfigId = :dataSourceConfigId AND r.isActive = true ORDER BY r.priority")
    List<DataMappingRuleEntity> findActiveByDataSourceConfigId(@Param("dataSourceConfigId") UUID dataSourceConfigId);

    /**
     * Find rules by source key
     */
    @Query("SELECT r FROM DataMappingRuleEntity r WHERE r.dataSourceConfigId = :dataSourceConfigId AND r.sourceKey = :sourceKey ORDER BY r.priority")
    List<DataMappingRuleEntity> findByDataSourceConfigIdAndSourceKey(@Param("dataSourceConfigId") UUID dataSourceConfigId, @Param("sourceKey") String sourceKey);

    /**
     * Delete all rules for a data source configuration
     */
    @Modifying
    @Query("DELETE FROM DataMappingRuleEntity r WHERE r.dataSourceConfigId = :dataSourceConfigId")
    void deleteByDataSourceConfigId(@Param("dataSourceConfigId") UUID dataSourceConfigId);
}
