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
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing attributes of PF assets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfAttributeService {

    private final AttributesService attributesService;

    /**
     * Saves server-scope attributes for an asset.
     */
    public void saveServerAttributes(UUID assetId, Map<String, Object> attributes) {
        log.debug("Saving {} attributes for asset {}", attributes.size(), assetId);

        List<AttributeKvEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Object> attr : attributes.entrySet()) {
            String key = attr.getKey();
            Object value = attr.getValue();

            if (value == null) {
                continue;
            }

            AttributeKvEntry entry = createAttributeEntry(key, value);
            if (entry != null) {
                entries.add(entry);
            }
        }

        if (!entries.isEmpty()) {
            EntityId entityId = new AssetId(assetId);
            try {
                attributesService.save(null, entityId, AttributeScope.SERVER_SCOPE, entries).get();
                log.debug("Saved {} attributes successfully", entries.size());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error saving attributes for asset {}: {}", assetId, e.getMessage());
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to save attributes", e);
            }
        }
    }

    /**
     * Gets all server-scope attributes for an asset.
     */
    public List<AttributeKvEntry> getServerAttributes(UUID assetId) {
        EntityId entityId = new AssetId(assetId);
        try {
            return attributesService.findAll(null, entityId, AttributeScope.SERVER_SCOPE).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting attributes for asset {}: {}", assetId, e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get attributes", e);
        }
    }

    /**
     * Gets specific attributes by keys.
     */
    public List<AttributeKvEntry> getServerAttributes(UUID assetId, List<String> keys) {
        EntityId entityId = new AssetId(assetId);
        try {
            return attributesService.find(null, entityId, AttributeScope.SERVER_SCOPE, keys).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting attributes {} for asset {}: {}", keys, assetId, e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get attributes", e);
        }
    }

    /**
     * Deletes attributes by keys.
     */
    public void deleteAttributes(UUID assetId, List<String> keys) {
        EntityId entityId = new AssetId(assetId);
        try {
            attributesService.removeAll(null, entityId, AttributeScope.SERVER_SCOPE, keys).get();
            log.debug("Deleted attributes {} for asset {}", keys, assetId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting attributes for asset {}: {}", assetId, e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to delete attributes", e);
        }
    }

    /**
     * Creates an AttributeKvEntry from a key-value pair.
     */
    private AttributeKvEntry createAttributeEntry(String key, Object value) {
        long ts = System.currentTimeMillis();

        if (value instanceof String) {
            return new BaseAttributeKvEntry(new StringDataEntry(key, (String) value), ts);
        } else if (value instanceof Boolean) {
            return new BaseAttributeKvEntry(new BooleanDataEntry(key, (Boolean) value), ts);
        } else if (value instanceof Long) {
            return new BaseAttributeKvEntry(new LongDataEntry(key, (Long) value), ts);
        } else if (value instanceof Integer) {
            return new BaseAttributeKvEntry(new LongDataEntry(key, ((Integer) value).longValue()), ts);
        } else if (value instanceof Double) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, (Double) value), ts);
        } else if (value instanceof Float) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, ((Float) value).doubleValue()), ts);
        } else if (value instanceof BigDecimal) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, ((BigDecimal) value).doubleValue()), ts);
        } else {
            // Convert to string for other types
            return new BaseAttributeKvEntry(new StringDataEntry(key, value.toString()), ts);
        }
    }
}
