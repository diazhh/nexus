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
package org.thingsboard.nexus.ct.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.nexus.ct.model.CTUnit;
import org.thingsboard.nexus.ct.model.UnitStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CTUnitRepository extends JpaRepository<CTUnit, UUID> {

    Optional<CTUnit> findByUnitCode(String unitCode);

    Page<CTUnit> findByTenantId(UUID tenantId, Pageable pageable);

    List<CTUnit> findByTenantIdAndOperationalStatus(UUID tenantId, UnitStatus status);

    @Query("SELECT u FROM CTUnit u WHERE u.tenantId = :tenantId " +
           "AND (:status IS NULL OR u.operationalStatus = :status) " +
           "AND (:location IS NULL OR LOWER(u.currentLocation) LIKE LOWER(CONCAT('%', :location, '%')))")
    Page<CTUnit> findByFilters(
        @Param("tenantId") UUID tenantId,
        @Param("status") UnitStatus status,
        @Param("location") String location,
        Pageable pageable
    );

    @Query("SELECT u FROM CTUnit u WHERE u.tenantId = :tenantId " +
           "AND u.operationalStatus IN :statuses")
    List<CTUnit> findByTenantIdAndStatuses(
        @Param("tenantId") UUID tenantId,
        @Param("statuses") List<UnitStatus> statuses
    );

    @Query("SELECT COUNT(u) FROM CTUnit u WHERE u.tenantId = :tenantId " +
           "AND u.operationalStatus = :status")
    long countByTenantIdAndStatus(
        @Param("tenantId") UUID tenantId,
        @Param("status") UnitStatus status
    );

    boolean existsByUnitCode(String unitCode);
}
