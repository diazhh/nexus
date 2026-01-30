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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.DataAggregationEntity;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for DataAggregation entities.
 */
public interface DataAggregationRepository extends JpaRepository<DataAggregationEntity, UUID> {

    /**
     * Find aggregations for a tenant with pagination and search
     */
    @Query("SELECT a FROM DataAggregationEntity a WHERE a.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(a.aggregationName, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(a.moduleKey, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(a.sourceAssetType, CONCAT('%', :searchText, '%')) = true)")
    Page<DataAggregationEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    /**
     * Find aggregations by module key
     */
    @Query("SELECT a FROM DataAggregationEntity a WHERE a.tenantId = :tenantId AND a.moduleKey = :moduleKey")
    List<DataAggregationEntity> findByTenantIdAndModuleKey(@Param("tenantId") UUID tenantId, @Param("moduleKey") String moduleKey);

    /**
     * Find active aggregations by module key
     */
    @Query("SELECT a FROM DataAggregationEntity a WHERE a.tenantId = :tenantId AND a.moduleKey = :moduleKey AND a.isActive = true")
    List<DataAggregationEntity> findActiveByTenantIdAndModuleKey(@Param("tenantId") UUID tenantId, @Param("moduleKey") String moduleKey);

    /**
     * Find aggregations by source asset type
     */
    @Query("SELECT a FROM DataAggregationEntity a WHERE a.tenantId = :tenantId AND a.sourceAssetType = :sourceAssetType")
    List<DataAggregationEntity> findByTenantIdAndSourceAssetType(@Param("tenantId") UUID tenantId, @Param("sourceAssetType") String sourceAssetType);

    /**
     * Find active aggregations by source asset type
     */
    @Query("SELECT a FROM DataAggregationEntity a WHERE a.tenantId = :tenantId AND a.sourceAssetType = :sourceAssetType AND a.isActive = true")
    List<DataAggregationEntity> findActiveByTenantIdAndSourceAssetType(@Param("tenantId") UUID tenantId, @Param("sourceAssetType") String sourceAssetType);

    /**
     * Check if aggregation name exists for tenant
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM DataAggregationEntity a WHERE a.tenantId = :tenantId AND a.aggregationName = :name")
    boolean existsByTenantIdAndAggregationName(@Param("tenantId") UUID tenantId, @Param("name") String name);

    /**
     * Delete all aggregations for a tenant
     */
    @Modifying
    @Query("DELETE FROM DataAggregationEntity a WHERE a.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);
}
