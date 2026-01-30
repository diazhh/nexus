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
import org.thingsboard.server.dao.model.sql.MappingTemplateEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for MappingTemplate entities.
 */
public interface MappingTemplateRepository extends JpaRepository<MappingTemplateEntity, UUID> {

    /**
     * Find templates for a tenant with pagination and search
     */
    @Query("SELECT t FROM MappingTemplateEntity t WHERE t.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(t.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(t.moduleKey, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(t.description, CONCAT('%', :searchText, '%')) = true)")
    Page<MappingTemplateEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    /**
     * Find templates by module key
     */
    @Query("SELECT t FROM MappingTemplateEntity t WHERE t.tenantId = :tenantId AND t.moduleKey = :moduleKey ORDER BY t.name")
    List<MappingTemplateEntity> findByTenantIdAndModuleKey(@Param("tenantId") UUID tenantId, @Param("moduleKey") String moduleKey);

    /**
     * Find templates by module key with pagination
     */
    @Query("SELECT t FROM MappingTemplateEntity t WHERE t.tenantId = :tenantId AND t.moduleKey = :moduleKey " +
            "AND (:searchText IS NULL OR ilike(t.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(t.description, CONCAT('%', :searchText, '%')) = true)")
    Page<MappingTemplateEntity> findByTenantIdAndModuleKey(@Param("tenantId") UUID tenantId,
                                                           @Param("moduleKey") String moduleKey,
                                                           @Param("searchText") String searchText,
                                                           Pageable pageable);

    /**
     * Find the default template for a module
     */
    @Query("SELECT t FROM MappingTemplateEntity t WHERE t.tenantId = :tenantId AND t.moduleKey = :moduleKey AND t.isDefault = true")
    Optional<MappingTemplateEntity> findDefaultByTenantIdAndModuleKey(@Param("tenantId") UUID tenantId, @Param("moduleKey") String moduleKey);

    /**
     * Find active templates for a tenant
     */
    @Query("SELECT t FROM MappingTemplateEntity t WHERE t.tenantId = :tenantId AND t.isActive = true ORDER BY t.moduleKey, t.name")
    List<MappingTemplateEntity> findActiveByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Find active templates by module key
     */
    @Query("SELECT t FROM MappingTemplateEntity t WHERE t.tenantId = :tenantId AND t.moduleKey = :moduleKey AND t.isActive = true ORDER BY t.name")
    List<MappingTemplateEntity> findActiveByTenantIdAndModuleKey(@Param("tenantId") UUID tenantId, @Param("moduleKey") String moduleKey);

    /**
     * Find template by name within a module
     */
    @Query("SELECT t FROM MappingTemplateEntity t WHERE t.tenantId = :tenantId AND t.moduleKey = :moduleKey AND t.name = :name")
    Optional<MappingTemplateEntity> findByTenantIdAndModuleKeyAndName(@Param("tenantId") UUID tenantId, @Param("moduleKey") String moduleKey, @Param("name") String name);

    /**
     * Check if a template name already exists within a module
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM MappingTemplateEntity t WHERE t.tenantId = :tenantId AND t.moduleKey = :moduleKey AND t.name = :name")
    boolean existsByTenantIdAndModuleKeyAndName(@Param("tenantId") UUID tenantId, @Param("moduleKey") String moduleKey, @Param("name") String name);

    /**
     * Delete all templates for a tenant
     */
    @Modifying
    @Query("DELETE FROM MappingTemplateEntity t WHERE t.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);
}
