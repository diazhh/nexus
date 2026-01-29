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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar jerarquías entre Assets del módulo de Yacimientos.
 * Implementa relaciones padre-hijo usando ThingsBoard Relations.
 *
 * Jerarquía típica:
 * Basin -> Field -> Reservoir -> Zone -> Well -> Completion
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvHierarchyService {

    private final RelationService relationService;

    // Tipos de relación estándar
    public static final String RELATION_CONTAINS = "Contains";
    public static final String RELATION_BELONGS_TO = "BelongsTo";
    public static final String RELATION_PRODUCES_FROM = "ProducesFrom";
    public static final String RELATION_INJECTS_TO = "InjectsTo";
    public static final String RELATION_CHARACTERIZED_BY = "CharacterizedBy";
    public static final String RELATION_MONITORED_BY = "MonitoredBy";
    public static final String RELATION_ADJACENT_TO = "AdjacentTo";
    public static final String RELATION_CROSSES = "Crosses";

    /**
     * Crea una relación "Contains" (padre contiene hijo).
     * Ej: Basin contains Field, Field contains Reservoir
     */
    public void createContainsRelation(UUID tenantId, UUID parentAssetId, UUID childAssetId) {
        log.debug("Creating CONTAINS relation: {} -> {}", parentAssetId, childAssetId);
        createRelation(tenantId, parentAssetId, childAssetId, RELATION_CONTAINS);
    }

    /**
     * Crea una relación "BelongsTo" (hijo pertenece a padre).
     * Inversa de Contains.
     */
    public void createBelongsToRelation(UUID tenantId, UUID childAssetId, UUID parentAssetId) {
        log.debug("Creating BELONGS_TO relation: {} -> {}", childAssetId, parentAssetId);
        createRelation(tenantId, childAssetId, parentAssetId, RELATION_BELONGS_TO);
    }

    /**
     * Crea una relación "ProducesFrom" (pozo produce de zona/yacimiento).
     */
    public void createProducesFromRelation(UUID tenantId, UUID wellAssetId, UUID reservoirAssetId) {
        log.debug("Creating PRODUCES_FROM relation: {} -> {}", wellAssetId, reservoirAssetId);
        createRelation(tenantId, wellAssetId, reservoirAssetId, RELATION_PRODUCES_FROM);
    }

    /**
     * Crea una relación "InjectsTo" (pozo inyector inyecta a zona).
     */
    public void createInjectsToRelation(UUID tenantId, UUID injectorWellId, UUID reservoirAssetId) {
        log.debug("Creating INJECTS_TO relation: {} -> {}", injectorWellId, reservoirAssetId);
        createRelation(tenantId, injectorWellId, reservoirAssetId, RELATION_INJECTS_TO);
    }

    /**
     * Crea una relación "CharacterizedBy" (entidad caracterizada por estudio).
     * Ej: Reservoir CharacterizedBy PVT_Study
     */
    public void createCharacterizedByRelation(UUID tenantId, UUID entityAssetId, UUID studyAssetId) {
        log.debug("Creating CHARACTERIZED_BY relation: {} -> {}", entityAssetId, studyAssetId);
        createRelation(tenantId, entityAssetId, studyAssetId, RELATION_CHARACTERIZED_BY);
    }

    /**
     * Crea una relación "AdjacentTo" (entidades adyacentes).
     * Ej: Zone AdjacentTo Zone, Fault AdjacentTo Reservoir
     */
    public void createAdjacentToRelation(UUID tenantId, UUID assetId1, UUID assetId2) {
        log.debug("Creating ADJACENT_TO relation: {} <-> {}", assetId1, assetId2);
        createRelation(tenantId, assetId1, assetId2, RELATION_ADJACENT_TO);
        // Relación bidireccional
        createRelation(tenantId, assetId2, assetId1, RELATION_ADJACENT_TO);
    }

    /**
     * Crea una relación "Crosses" (falla cruza yacimiento).
     */
    public void createCrossesRelation(UUID tenantId, UUID faultAssetId, UUID reservoirAssetId) {
        log.debug("Creating CROSSES relation: {} -> {}", faultAssetId, reservoirAssetId);
        createRelation(tenantId, faultAssetId, reservoirAssetId, RELATION_CROSSES);
    }

    /**
     * Establece jerarquía completa padre-hijo con ambas relaciones.
     */
    public void setParentChild(UUID tenantId, UUID parentAssetId, UUID childAssetId) {
        createContainsRelation(tenantId, parentAssetId, childAssetId);
        createBelongsToRelation(tenantId, childAssetId, parentAssetId);
    }

    /**
     * Obtiene todos los hijos directos de un Asset (relación Contains).
     */
    public List<UUID> getChildren(UUID tenantId, UUID parentAssetId) {
        List<EntityRelation> relations = relationService.findByFromAndType(
            TenantId.fromUUID(tenantId),
            new AssetId(parentAssetId),
            RELATION_CONTAINS,
            RelationTypeGroup.COMMON
        );

        return relations.stream()
            .map(r -> r.getTo().getId())
            .collect(Collectors.toList());
    }

    /**
     * Obtiene el padre de un Asset (relación BelongsTo).
     */
    public UUID getParent(UUID tenantId, UUID childAssetId) {
        List<EntityRelation> relations = relationService.findByFromAndType(
            TenantId.fromUUID(tenantId),
            new AssetId(childAssetId),
            RELATION_BELONGS_TO,
            RelationTypeGroup.COMMON
        );

        return relations.isEmpty() ? null : relations.get(0).getTo().getId();
    }

    /**
     * Obtiene todos los Assets relacionados por un tipo específico.
     * @param tenantId ID del tenant
     * @param assetId ID del asset origen
     * @param relationType Tipo de relación a buscar
     * @return Lista de IDs de entidades relacionadas
     */
    public List<UUID> getRelatedAssets(UUID tenantId, UUID assetId, String relationType) {
        List<EntityRelation> relations = relationService.findByFromAndType(
            TenantId.fromUUID(tenantId),
            new AssetId(assetId),
            relationType,
            RelationTypeGroup.COMMON
        );

        return relations.stream()
            .map(r -> r.getTo().getId())
            .collect(Collectors.toList());
    }

    /**
     * Alias para getRelatedAssets - obtiene entidades relacionadas por tipo de relación.
     * @param tenantId ID del tenant
     * @param entityId ID de la entidad origen
     * @param relationType Tipo de relación a buscar
     * @return Lista de IDs de entidades relacionadas
     */
    public List<UUID> getRelatedEntities(UUID tenantId, UUID entityId, String relationType) {
        return getRelatedAssets(tenantId, entityId, relationType);
    }

    /**
     * Elimina una relación específica.
     */
    public void deleteRelation(UUID tenantId, UUID fromAssetId, UUID toAssetId, String relationType) {
        log.debug("Deleting relation: {} -[{}]-> {}", fromAssetId, relationType, toAssetId);

        relationService.deleteRelation(
            TenantId.fromUUID(tenantId),
            new AssetId(fromAssetId),
            new AssetId(toAssetId),
            relationType,
            RelationTypeGroup.COMMON
        );
    }

    /**
     * Elimina todas las relaciones de un Asset.
     */
    public void deleteAllRelations(UUID tenantId, UUID assetId) {
        log.warn("Deleting all relations for asset: {}", assetId);
        relationService.deleteEntityRelations(TenantId.fromUUID(tenantId), new AssetId(assetId));
    }

    /**
     * Verifica si existe una relación entre dos Assets.
     */
    public boolean relationExists(UUID tenantId, UUID fromAssetId, UUID toAssetId, String relationType) {
        try {
            EntityRelation relation = relationService.getRelation(
                TenantId.fromUUID(tenantId),
                new AssetId(fromAssetId),
                new AssetId(toAssetId),
                relationType,
                RelationTypeGroup.COMMON
            );
            return relation != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Crea una relación genérica entre dos Assets.
     */
    public void createRelation(UUID tenantId, UUID fromAssetId, UUID toAssetId, String relationType) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(fromAssetId));
        relation.setTo(new AssetId(toAssetId));
        relation.setType(relationType);
        relation.setTypeGroup(RelationTypeGroup.COMMON);

        relationService.saveRelation(TenantId.fromUUID(tenantId), relation);
    }
}
