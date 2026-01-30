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
package org.thingsboard.nexus.dr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.nexus.dr.model.DrBha;
import org.thingsboard.nexus.dr.model.enums.BhaType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Bottom Hole Assembly (BHA) entities
 */
@Repository
public interface DrBhaRepository extends JpaRepository<DrBha, UUID> {

    /**
     * Find BHA by number
     */
    Optional<DrBha> findByTenantIdAndBhaNumber(UUID tenantId, String bhaNumber);

    /**
     * Find BHA by asset ID
     */
    Optional<DrBha> findByAssetId(UUID assetId);

    /**
     * Find all BHAs for a tenant
     */
    Page<DrBha> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find all BHAs for a tenant (list)
     */
    List<DrBha> findByTenantId(UUID tenantId);

    /**
     * Find BHAs by tenant and status
     */
    List<DrBha> findByTenantIdAndStatus(UUID tenantId, String status);

    /**
     * Find BHAs by tenant and type
     */
    List<DrBha> findByTenantIdAndBhaType(UUID tenantId, BhaType bhaType);

    /**
     * Find available BHAs (not currently in use)
     */
    @Query("SELECT b FROM DrBha b WHERE b.tenantId = :tenantId AND b.status = 'AVAILABLE'")
    List<DrBha> findAvailableBhas(@Param("tenantId") UUID tenantId);

    /**
     * Find directional BHAs
     */
    @Query("SELECT b FROM DrBha b WHERE b.tenantId = :tenantId AND b.isDirectional = true AND b.status = 'AVAILABLE'")
    List<DrBha> findAvailableDirectionalBhas(@Param("tenantId") UUID tenantId);

    /**
     * Find BHAs with filters
     */
    @Query("SELECT b FROM DrBha b WHERE b.tenantId = :tenantId " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (:bhaType IS NULL OR b.bhaType = :bhaType) " +
           "AND (:isDirectional IS NULL OR b.isDirectional = :isDirectional) " +
           "AND (:bitSizeIn IS NULL OR b.bitSizeIn = :bitSizeIn)")
    Page<DrBha> findByFilters(
        @Param("tenantId") UUID tenantId,
        @Param("status") String status,
        @Param("bhaType") BhaType bhaType,
        @Param("isDirectional") Boolean isDirectional,
        @Param("bitSizeIn") java.math.BigDecimal bitSizeIn,
        Pageable pageable
    );

    /**
     * Find BHAs by bit size
     */
    @Query("SELECT b FROM DrBha b WHERE b.tenantId = :tenantId AND b.bitSizeIn = :bitSizeIn")
    List<DrBha> findByTenantIdAndBitSize(
        @Param("tenantId") UUID tenantId,
        @Param("bitSizeIn") java.math.BigDecimal bitSizeIn
    );

    /**
     * Count BHAs by tenant and status
     */
    @Query("SELECT COUNT(b) FROM DrBha b WHERE b.tenantId = :tenantId AND b.status = :status")
    long countByTenantIdAndStatus(
        @Param("tenantId") UUID tenantId,
        @Param("status") String status
    );

    /**
     * Count BHAs by tenant and type
     */
    @Query("SELECT COUNT(b) FROM DrBha b WHERE b.tenantId = :tenantId AND b.bhaType = :bhaType")
    long countByTenantIdAndBhaType(
        @Param("tenantId") UUID tenantId,
        @Param("bhaType") BhaType bhaType
    );

    /**
     * Check if BHA number exists
     */
    boolean existsByTenantIdAndBhaNumber(UUID tenantId, String bhaNumber);

    /**
     * Check if BHA number exists for another BHA
     */
    @Query("SELECT COUNT(b) > 0 FROM DrBha b WHERE b.tenantId = :tenantId AND b.bhaNumber = :bhaNumber AND b.id != :excludeId")
    boolean existsByTenantIdAndBhaNumberAndIdNot(
        @Param("tenantId") UUID tenantId,
        @Param("bhaNumber") String bhaNumber,
        @Param("excludeId") UUID excludeId
    );

    /**
     * Find BHAs with high footage (for replacement consideration)
     */
    @Query("SELECT b FROM DrBha b WHERE b.tenantId = :tenantId AND b.totalFootageDrilled > :footageThreshold")
    List<DrBha> findBhasWithHighFootage(
        @Param("tenantId") UUID tenantId,
        @Param("footageThreshold") java.math.BigDecimal footageThreshold
    );

    /**
     * Find BHAs by motor manufacturer
     */
    @Query("SELECT b FROM DrBha b WHERE b.tenantId = :tenantId " +
           "AND b.bhaType IN ('MOTOR', 'HYBRID') " +
           "AND LOWER(b.motorManufacturer) LIKE LOWER(CONCAT('%', :manufacturer, '%'))")
    List<DrBha> findByMotorManufacturer(
        @Param("tenantId") UUID tenantId,
        @Param("manufacturer") String manufacturer
    );
}
