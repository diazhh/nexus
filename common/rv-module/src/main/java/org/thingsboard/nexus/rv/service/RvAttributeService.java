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
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Servicio para gestionar atributos de Assets del módulo de Yacimientos.
 * Proporciona operaciones CRUD sobre atributos SERVER_SCOPE y SHARED_SCOPE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RvAttributeService {

    private final AttributesService attributesService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Guarda atributos SERVER_SCOPE para un Asset de yacimiento.
     */
    public void saveServerAttributes(UUID assetId, Map<String, Object> attributes) {
        log.debug("Saving SERVER_SCOPE attributes for asset {}: {}", assetId, attributes.keySet());
        saveAttributes(assetId, attributes, AttributeScope.SERVER_SCOPE);
    }

    /**
     * Guarda atributos SHARED_SCOPE para un Asset de yacimiento.
     */
    public void saveSharedAttributes(UUID assetId, Map<String, Object> attributes) {
        log.debug("Saving SHARED_SCOPE attributes for asset {}: {}", assetId, attributes.keySet());
        saveAttributes(assetId, attributes, AttributeScope.SHARED_SCOPE);
    }

    /**
     * Obtiene todos los atributos SERVER_SCOPE de un Asset.
     */
    public List<AttributeKvEntry> getServerAttributes(UUID assetId) {
        try {
            return attributesService.findAll(null, new AssetId(assetId), AttributeScope.SERVER_SCOPE).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting SERVER_SCOPE attributes for asset {}: {}", assetId, e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get server attributes", e);
        }
    }

    /**
     * Obtiene atributos específicos por sus claves.
     */
    public List<AttributeKvEntry> getServerAttributes(UUID assetId, List<String> keys) {
        try {
            return attributesService.find(null, new AssetId(assetId), AttributeScope.SERVER_SCOPE, keys).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting attributes {} for asset {}: {}", keys, assetId, e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get attributes", e);
        }
    }

    /**
     * Elimina atributos por sus claves.
     */
    public void deleteAttributes(UUID assetId, AttributeScope scope, List<String> keys) {
        try {
            attributesService.removeAll(null, new AssetId(assetId), scope, keys);
            log.debug("Deleted attributes {} from asset {} in scope {}", keys, assetId, scope);
        } catch (Exception e) {
            log.error("Error deleting attributes for asset {}: {}", assetId, e.getMessage());
            throw new RuntimeException("Failed to delete attributes", e);
        }
    }

    private void saveAttributes(UUID assetId, Map<String, Object> attributes, AttributeScope scope) {
        try {
            List<AttributeKvEntry> kvEntries = new ArrayList<>();

            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                AttributeKvEntry kvEntry = createKvEntry(entry.getKey(), entry.getValue());
                if (kvEntry != null) {
                    kvEntries.add(kvEntry);
                }
            }

            if (!kvEntries.isEmpty()) {
                attributesService.save(null, new AssetId(assetId), scope, kvEntries);
                log.debug("Saved {} attributes for asset {} in scope {}", kvEntries.size(), assetId, scope);
            }
        } catch (Exception e) {
            log.error("Error saving attributes for asset {}: {}", assetId, e.getMessage(), e);
            throw new RuntimeException("Failed to save attributes", e);
        }
    }

    private AttributeKvEntry createKvEntry(String key, Object value) {
        if (value == null) {
            return null;
        }

        long ts = System.currentTimeMillis();

        if (value instanceof String) {
            return new BaseAttributeKvEntry(new StringDataEntry(key, (String) value), ts);
        } else if (value instanceof Integer) {
            return new BaseAttributeKvEntry(new LongDataEntry(key, ((Integer) value).longValue()), ts);
        } else if (value instanceof Long) {
            return new BaseAttributeKvEntry(new LongDataEntry(key, (Long) value), ts);
        } else if (value instanceof Double) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, (Double) value), ts);
        } else if (value instanceof Float) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, ((Float) value).doubleValue()), ts);
        } else if (value instanceof BigDecimal) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, ((BigDecimal) value).doubleValue()), ts);
        } else if (value instanceof Boolean) {
            return new BaseAttributeKvEntry(new BooleanDataEntry(key, (Boolean) value), ts);
        } else if (value instanceof JsonNode) {
            return new BaseAttributeKvEntry(new JsonDataEntry(key, ((JsonNode) value).toString()), ts);
        } else {
            // Para objetos complejos, serializar a JSON
            try {
                JsonNode jsonNode = objectMapper.valueToTree(value);
                return new BaseAttributeKvEntry(new JsonDataEntry(key, jsonNode.toString()), ts);
            } catch (Exception e) {
                return new BaseAttributeKvEntry(new StringDataEntry(key, value.toString()), ts);
            }
        }
    }
}
