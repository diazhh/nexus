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
package org.thingsboard.nexus.rv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.thingsboard.nexus.rv.dto.RvCatalogDto;
import org.thingsboard.nexus.rv.exception.RvEntityNotFoundException;
import org.thingsboard.server.common.data.CacheConstants;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing RV catalog data.
 * Provides access to enumeration values like well types, lithologies, formations, etc.
 *
 * Uses Spring Cache to improve performance for frequently accessed catalog data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = CacheConstants.RV_CATALOGS_CACHE)
public class RvCatalogService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String SELECT_BASE = """
        SELECT id, tenant_id, catalog_type, code, name, description,
               metadata, sort_order, is_active, created_time, updated_time
        FROM rv_catalogs
        """;

    /**
     * Get all catalog items of a specific type for a tenant.
     * Results are cached with key: "type-{tenantId}-{catalogType}"
     */
    @Cacheable(key = "'type-' + #tenantId + '-' + #catalogType")
    public List<RvCatalogDto> getCatalogsByType(UUID tenantId, String catalogType) {
        log.debug("Getting catalogs: tenantId={}, type={}", tenantId, catalogType);

        String sql = SELECT_BASE + """
            WHERE tenant_id = ? AND catalog_type = ? AND is_active = true
            ORDER BY sort_order, name
            """;

        return jdbcTemplate.query(sql, new RvCatalogRowMapper(), tenantId, catalogType);
    }

    /**
     * Get a specific catalog item by type and code.
     * Results are cached with key: "code-{tenantId}-{catalogType}-{code}"
     */
    @Cacheable(key = "'code-' + #tenantId + '-' + #catalogType + '-' + #code")
    public Optional<RvCatalogDto> getCatalogByCode(UUID tenantId, String catalogType, String code) {
        log.debug("Getting catalog: tenantId={}, type={}, code={}", tenantId, catalogType, code);

        String sql = SELECT_BASE + """
            WHERE tenant_id = ? AND catalog_type = ? AND code = ? AND is_active = true
            """;

        List<RvCatalogDto> results = jdbcTemplate.query(sql, new RvCatalogRowMapper(), tenantId, catalogType, code);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Get a catalog item by ID.
     * Results are cached with key: "id-{id}"
     */
    @Cacheable(key = "'id-' + #id")
    public Optional<RvCatalogDto> getCatalogById(UUID id) {
        log.debug("Getting catalog by ID: {}", id);

        String sql = SELECT_BASE + "WHERE id = ?";

        List<RvCatalogDto> results = jdbcTemplate.query(sql, new RvCatalogRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Get all available catalog types for a tenant.
     * Results are cached with key: "types-{tenantId}"
     */
    @Cacheable(key = "'types-' + #tenantId")
    public List<String> getAvailableCatalogTypes(UUID tenantId) {
        log.debug("Getting available catalog types for tenant: {}", tenantId);

        String sql = """
            SELECT DISTINCT catalog_type FROM rv_catalogs
            WHERE tenant_id = ? AND is_active = true
            ORDER BY catalog_type
            """;

        return jdbcTemplate.queryForList(sql, String.class, tenantId);
    }

    /**
     * Create a new catalog item.
     * Evicts all cache entries to ensure fresh data.
     */
    @CacheEvict(allEntries = true)
    public RvCatalogDto createCatalog(RvCatalogDto catalog) {
        log.info("Creating catalog: type={}, code={}", catalog.getCatalogType(), catalog.getCode());

        UUID id = UUID.randomUUID();
        long now = System.currentTimeMillis();

        String sql = """
            INSERT INTO rv_catalogs (id, tenant_id, catalog_type, code, name, description,
                                     metadata, sort_order, is_active, created_time)
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
            """;

        String metadataJson = null;
        if (catalog.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(catalog.getMetadata());
            } catch (Exception e) {
                log.warn("Failed to serialize metadata", e);
            }
        }

        jdbcTemplate.update(sql,
                id,
                catalog.getTenantId(),
                catalog.getCatalogType(),
                catalog.getCode(),
                catalog.getName(),
                catalog.getDescription(),
                metadataJson,
                catalog.getSortOrder() != null ? catalog.getSortOrder() : 0,
                catalog.getIsActive() != null ? catalog.getIsActive() : true,
                now
        );

        catalog.setId(id);
        catalog.setCreatedTime(now);
        return catalog;
    }

    /**
     * Update an existing catalog item.
     * Evicts all cache entries to ensure fresh data.
     */
    @CacheEvict(allEntries = true)
    public RvCatalogDto updateCatalog(RvCatalogDto catalog) {
        log.info("Updating catalog: id={}", catalog.getId());

        if (catalog.getId() == null) {
            throw new IllegalArgumentException("Catalog ID is required for update");
        }

        long now = System.currentTimeMillis();

        String sql = """
            UPDATE rv_catalogs SET
                name = ?, description = ?, metadata = ?::jsonb,
                sort_order = ?, is_active = ?, updated_time = ?
            WHERE id = ?
            """;

        String metadataJson = null;
        if (catalog.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(catalog.getMetadata());
            } catch (Exception e) {
                log.warn("Failed to serialize metadata", e);
            }
        }

        int updated = jdbcTemplate.update(sql,
                catalog.getName(),
                catalog.getDescription(),
                metadataJson,
                catalog.getSortOrder(),
                catalog.getIsActive(),
                now,
                catalog.getId()
        );

        if (updated == 0) {
            throw new RvEntityNotFoundException("Catalog not found: " + catalog.getId());
        }

        catalog.setUpdatedTime(now);
        return catalog;
    }

    /**
     * Delete a catalog item (soft delete - sets is_active to false).
     * Evicts all cache entries to ensure fresh data.
     */
    @CacheEvict(allEntries = true)
    public void deleteCatalog(UUID id) {
        log.warn("Deleting catalog: id={}", id);

        String sql = "UPDATE rv_catalogs SET is_active = false, updated_time = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql, System.currentTimeMillis(), id);

        if (updated == 0) {
            throw new RvEntityNotFoundException("Catalog not found: " + id);
        }
    }

    /**
     * Search catalogs by name (partial match).
     */
    public List<RvCatalogDto> searchCatalogs(UUID tenantId, String catalogType, String searchText) {
        log.debug("Searching catalogs: tenantId={}, type={}, search={}", tenantId, catalogType, searchText);

        String sql = SELECT_BASE + """
            WHERE tenant_id = ? AND catalog_type = ? AND is_active = true
            AND (LOWER(name) LIKE LOWER(?) OR LOWER(code) LIKE LOWER(?))
            ORDER BY sort_order, name
            """;

        String pattern = "%" + searchText + "%";
        return jdbcTemplate.query(sql, new RvCatalogRowMapper(), tenantId, catalogType, pattern, pattern);
    }

    // ========================================
    // Convenience methods for specific catalog types
    // ========================================

    public List<RvCatalogDto> getWellTypes(UUID tenantId) {
        return getCatalogsByType(tenantId, RvCatalogDto.TYPE_WELL_TYPE);
    }

    public List<RvCatalogDto> getLithologies(UUID tenantId) {
        return getCatalogsByType(tenantId, RvCatalogDto.TYPE_LITHOLOGY);
    }

    public List<RvCatalogDto> getDriveMechanisms(UUID tenantId) {
        return getCatalogsByType(tenantId, RvCatalogDto.TYPE_DRIVE_MECHANISM);
    }

    public List<RvCatalogDto> getVenezuelanFormations(UUID tenantId) {
        return getCatalogsByType(tenantId, RvCatalogDto.TYPE_FORMATION_VE);
    }

    public List<RvCatalogDto> getFajaRegions(UUID tenantId) {
        return getCatalogsByType(tenantId, RvCatalogDto.TYPE_FAJA_REGION);
    }

    public List<RvCatalogDto> getCompletionTypes(UUID tenantId) {
        return getCatalogsByType(tenantId, RvCatalogDto.TYPE_COMPLETION_TYPE);
    }

    public List<RvCatalogDto> getArtificialLiftMethods(UUID tenantId) {
        return getCatalogsByType(tenantId, RvCatalogDto.TYPE_ARTIFICIAL_LIFT);
    }

    public List<RvCatalogDto> getIprMethods(UUID tenantId) {
        return getCatalogsByType(tenantId, RvCatalogDto.TYPE_IPR_METHOD);
    }

    public List<RvCatalogDto> getDeclineTypes(UUID tenantId) {
        return getCatalogsByType(tenantId, RvCatalogDto.TYPE_DECLINE_TYPE);
    }

    // ========================================
    // Row Mapper
    // ========================================

    private class RvCatalogRowMapper implements RowMapper<RvCatalogDto> {
        @Override
        public RvCatalogDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            RvCatalogDto dto = new RvCatalogDto();
            dto.setId(UUID.fromString(rs.getString("id")));
            dto.setTenantId(UUID.fromString(rs.getString("tenant_id")));
            dto.setCatalogType(rs.getString("catalog_type"));
            dto.setCode(rs.getString("code"));
            dto.setName(rs.getString("name"));
            dto.setDescription(rs.getString("description"));
            dto.setSortOrder(rs.getInt("sort_order"));
            dto.setIsActive(rs.getBoolean("is_active"));
            dto.setCreatedTime(rs.getLong("created_time"));

            Long updatedTime = rs.getLong("updated_time");
            if (!rs.wasNull()) {
                dto.setUpdatedTime(updatedTime);
            }

            // Parse metadata JSON
            String metadataStr = rs.getString("metadata");
            if (metadataStr != null) {
                try {
                    dto.setMetadata(objectMapper.readTree(metadataStr));
                } catch (Exception e) {
                    log.warn("Failed to parse metadata JSON", e);
                }
            }

            return dto;
        }
    }
}
