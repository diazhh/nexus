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
package org.thingsboard.nexus.ct.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class CTAttributeService {

    private final AttributesService attributesService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveServerAttributes(UUID entityId, Map<String, Object> attributes) {
        log.debug("Saving server attributes for entity {}: {}", entityId, attributes);
        
        try {
            List<AttributeKvEntry> kvEntries = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                AttributeKvEntry kvEntry = createKvEntry(entry.getKey(), entry.getValue());
                if (kvEntry != null) {
                    kvEntries.add(kvEntry);
                }
            }
            
            if (!kvEntries.isEmpty()) {
                attributesService.save(null, new AssetId(entityId), AttributeScope.SERVER_SCOPE, kvEntries);
                log.debug("Saved {} attributes for entity {}", kvEntries.size(), entityId);
            }
        } catch (Exception e) {
            log.error("Error saving attributes for entity {}: {}", entityId, e.getMessage(), e);
            throw new RuntimeException("Failed to save attributes", e);
        }
    }

    public void saveSharedAttributes(UUID entityId, Map<String, Object> attributes) {
        log.debug("Saving shared attributes for entity {}: {}", entityId, attributes);
        
        try {
            List<AttributeKvEntry> kvEntries = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                AttributeKvEntry kvEntry = createKvEntry(entry.getKey(), entry.getValue());
                if (kvEntry != null) {
                    kvEntries.add(kvEntry);
                }
            }
            
            if (!kvEntries.isEmpty()) {
                attributesService.save(null, new AssetId(entityId), AttributeScope.SHARED_SCOPE, kvEntries);
                log.debug("Saved {} shared attributes for entity {}", kvEntries.size(), entityId);
            }
        } catch (Exception e) {
            log.error("Error saving shared attributes for entity {}: {}", entityId, e.getMessage(), e);
            throw new RuntimeException("Failed to save shared attributes", e);
        }
    }

    /**
     * Save a single server attribute
     */
    public void saveServerAttribute(UUID entityId, String key, Object value) {
        Map<String, Object> attributes = Map.of(key, value);
        saveServerAttributes(entityId, attributes);
    }

    /**
     * Get all SERVER_SCOPE attributes for an asset
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
     * Get specific attributes by keys
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
     * Delete attributes by keys
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
            // For complex objects, serialize to JSON
            try {
                JsonNode jsonNode = objectMapper.valueToTree(value);
                return new BaseAttributeKvEntry(new JsonDataEntry(key, jsonNode.toString()), ts);
            } catch (Exception e) {
                return new BaseAttributeKvEntry(new StringDataEntry(key, value.toString()), ts);
            }
        }
    }
}
