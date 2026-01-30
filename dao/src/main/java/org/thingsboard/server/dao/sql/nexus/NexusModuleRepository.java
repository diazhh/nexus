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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.NexusModuleEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for NexusModule entities.
 */
public interface NexusModuleRepository extends JpaRepository<NexusModuleEntity, UUID> {

    /**
     * Find module by unique key
     */
    Optional<NexusModuleEntity> findByModuleKey(String moduleKey);

    /**
     * Find all available modules (can be assigned to tenants)
     */
    @Query("SELECT m FROM NexusModuleEntity m WHERE m.isAvailable = true ORDER BY m.displayOrder")
    List<NexusModuleEntity> findAvailableModules();

    /**
     * Find all system modules
     */
    @Query("SELECT m FROM NexusModuleEntity m WHERE m.isSystemModule = true ORDER BY m.displayOrder")
    List<NexusModuleEntity> findSystemModules();

    /**
     * Find modules by category
     */
    @Query("SELECT m FROM NexusModuleEntity m WHERE m.category = :category AND m.isAvailable = true ORDER BY m.displayOrder")
    List<NexusModuleEntity> findByCategory(@Param("category") String category);

    /**
     * Find modules with pagination and search
     */
    @Query("SELECT m FROM NexusModuleEntity m WHERE " +
            "(:searchText IS NULL OR ilike(m.moduleName, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(m.moduleKey, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(m.description, CONCAT('%', :searchText, '%')) = true)")
    Page<NexusModuleEntity> findAllModules(@Param("searchText") String searchText, Pageable pageable);

    /**
     * Find modules by IDs
     */
    @Query("SELECT m FROM NexusModuleEntity m WHERE m.id IN :moduleIds")
    List<NexusModuleEntity> findByIdIn(@Param("moduleIds") List<UUID> moduleIds);

    /**
     * Count available modules
     */
    @Query("SELECT COUNT(m) FROM NexusModuleEntity m WHERE m.isAvailable = true")
    long countAvailableModules();

    /**
     * Check if module key exists
     */
    boolean existsByModuleKey(String moduleKey);
}
