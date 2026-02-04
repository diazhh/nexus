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
package org.thingsboard.nexus.pf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing hierarchical relationships between PF assets.
 * Handles parent-child relationships and specific PF domain relations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfHierarchyService {

    private final RelationService relationService;

    // Relation types for PF module
    public static final String RELATION_CONTAINS = "Contains";
    public static final String RELATION_BELONGS_TO = "BelongsTo";
    public static final String RELATION_MONITORED_BY = "MonitoredBy";
    public static final String RELATION_CONNECTED_TO = "ConnectedTo";
    public static final String RELATION_INSTALLED_IN = "InstalledIn";

    /**
     * Creates a parent-child relationship (Contains).
     */
    public void setParentChild(UUID tenantId, UUID parentId, UUID childId) {
        log.debug("Setting parent-child: {} -> {}", parentId, childId);

        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(parentId));
        relation.setTo(new AssetId(childId));
        relation.setType(RELATION_CONTAINS);
        relation.setTypeGroup(RelationTypeGroup.COMMON);

        relationService.saveRelation(TenantId.fromUUID(tenantId), relation);
    }

    /**
     * Gets the parent of an asset.
     */
    public UUID getParent(UUID tenantId, UUID childId) {
        try {
            List<EntityRelation> relations = relationService.findByToAndType(
                    TenantId.fromUUID(tenantId),
                    new AssetId(childId),
                    RELATION_CONTAINS,
                    RelationTypeGroup.COMMON
            );

            if (!relations.isEmpty()) {
                return relations.get(0).getFrom().getId();
            }
        } catch (Exception e) {
            log.error("Error getting parent for {}: {}", childId, e.getMessage());
        }
        return null;
    }

    /**
     * Gets all children of an asset.
     */
    public List<UUID> getChildren(UUID tenantId, UUID parentId) {
        try {
            List<EntityRelation> relations = relationService.findByFromAndType(
                    TenantId.fromUUID(tenantId),
                    new AssetId(parentId),
                    RELATION_CONTAINS,
                    RelationTypeGroup.COMMON
            );

            return relations.stream()
                    .map(r -> r.getTo().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting children for {}: {}", parentId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Creates an "InstalledIn" relation (e.g., ESP installed in well).
     */
    public void createInstalledInRelation(UUID tenantId, UUID equipmentId, UUID wellId) {
        log.debug("Creating InstalledIn relation: {} -> {}", equipmentId, wellId);

        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(equipmentId));
        relation.setTo(new AssetId(wellId));
        relation.setType(RELATION_INSTALLED_IN);
        relation.setTypeGroup(RelationTypeGroup.COMMON);

        relationService.saveRelation(TenantId.fromUUID(tenantId), relation);
    }

    /**
     * Creates a "ConnectedTo" relation (e.g., pipeline connections).
     */
    public void createConnectedToRelation(UUID tenantId, UUID fromId, UUID toId) {
        log.debug("Creating ConnectedTo relation: {} -> {}", fromId, toId);

        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(fromId));
        relation.setTo(new AssetId(toId));
        relation.setType(RELATION_CONNECTED_TO);
        relation.setTypeGroup(RelationTypeGroup.COMMON);

        relationService.saveRelation(TenantId.fromUUID(tenantId), relation);
    }

    /**
     * Removes parent-child relationship.
     */
    public void removeParentChild(UUID tenantId, UUID parentId, UUID childId) {
        log.debug("Removing parent-child: {} -> {}", parentId, childId);

        relationService.deleteRelation(
                TenantId.fromUUID(tenantId),
                new AssetId(parentId),
                new AssetId(childId),
                RELATION_CONTAINS,
                RelationTypeGroup.COMMON
        );
    }

    /**
     * Removes all relations for an asset.
     */
    public void removeAllRelations(UUID tenantId, UUID assetId) {
        log.debug("Removing all relations for: {}", assetId);

        relationService.deleteEntityRelations(
                TenantId.fromUUID(tenantId),
                new AssetId(assetId)
        );
    }

    /**
     * Gets equipment installed in a well.
     */
    public List<UUID> getEquipmentInWell(UUID tenantId, UUID wellId) {
        try {
            List<EntityRelation> relations = relationService.findByToAndType(
                    TenantId.fromUUID(tenantId),
                    new AssetId(wellId),
                    RELATION_INSTALLED_IN,
                    RelationTypeGroup.COMMON
            );

            return relations.stream()
                    .map(r -> r.getFrom().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting equipment for well {}: {}", wellId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
