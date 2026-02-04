/*
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
package org.thingsboard.nexus.po.service;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Wrapper service for ThingsBoard AttributesService.
 * Provides simplified access to asset attributes for the PO module.
 *
 * Health scores, optimization status, and other PO-specific data are stored
 * as SERVER_SCOPE attributes on assets.
 */
@Service("poAttributeService")
@RequiredArgsConstructor
@Slf4j
public class PoAttributeService {

    private final AttributesService tbAttributesService;

    /**
     * Saves SERVER_SCOPE attributes for an asset.
     */
    public void saveServerAttributes(UUID assetId, Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        EntityId entityId = new AssetId(assetId);
        List<AttributeKvEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            AttributeKvEntry kvEntry = createAttributeEntry(entry.getKey(), entry.getValue());
            if (kvEntry != null) {
                entries.add(kvEntry);
            }
        }

        if (!entries.isEmpty()) {
            try {
                ListenableFuture<?> future = tbAttributesService.save(
                        TenantId.SYS_TENANT_ID,
                        entityId,
                        AttributeScope.SERVER_SCOPE,
                        entries
                );
                future.get(); // Wait for completion
                log.debug("Saved {} attributes for asset {}", entries.size(), assetId);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error saving attributes for asset {}: {}", assetId, e.getMessage());
                throw new RuntimeException("Failed to save attributes", e);
            }
        }
    }

    /**
     * Saves a single SERVER_SCOPE attribute.
     */
    public void saveServerAttribute(UUID assetId, String key, Object value) {
        saveServerAttributes(assetId, Collections.singletonMap(key, value));
    }

    /**
     * Gets all SERVER_SCOPE attributes for an asset.
     */
    public List<AttributeKvEntry> getServerAttributes(UUID assetId) {
        EntityId entityId = new AssetId(assetId);
        try {
            ListenableFuture<List<AttributeKvEntry>> future = tbAttributesService.findAll(
                    TenantId.SYS_TENANT_ID,
                    entityId,
                    AttributeScope.SERVER_SCOPE
            );
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting attributes for asset {}: {}", assetId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets specific SERVER_SCOPE attributes by keys.
     */
    public List<AttributeKvEntry> getServerAttributes(UUID assetId, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        EntityId entityId = new AssetId(assetId);
        try {
            ListenableFuture<List<AttributeKvEntry>> future = tbAttributesService.find(
                    TenantId.SYS_TENANT_ID,
                    entityId,
                    AttributeScope.SERVER_SCOPE,
                    keys
            );
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting attributes for asset {}: {}", assetId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets a single attribute value as Optional.
     */
    public Optional<AttributeKvEntry> getAttribute(UUID assetId, String key) {
        List<AttributeKvEntry> entries = getServerAttributes(assetId, Collections.singletonList(key));
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(0));
    }

    /**
     * Gets a double attribute value.
     */
    public Optional<Double> getDoubleAttribute(UUID assetId, String key) {
        return getAttribute(assetId, key)
                .flatMap(AttributeKvEntry::getDoubleValue);
    }

    /**
     * Gets a string attribute value.
     */
    public Optional<String> getStringAttribute(UUID assetId, String key) {
        return getAttribute(assetId, key)
                .map(AttributeKvEntry::getValueAsString);
    }

    /**
     * Gets a long attribute value.
     */
    public Optional<Long> getLongAttribute(UUID assetId, String key) {
        return getAttribute(assetId, key)
                .flatMap(AttributeKvEntry::getLongValue);
    }

    /**
     * Gets a boolean attribute value.
     */
    public Optional<Boolean> getBooleanAttribute(UUID assetId, String key) {
        return getAttribute(assetId, key)
                .flatMap(AttributeKvEntry::getBooleanValue);
    }

    /**
     * Gets attributes as a map.
     */
    public Map<String, Object> getAttributesAsMap(UUID assetId) {
        List<AttributeKvEntry> entries = getServerAttributes(assetId);
        Map<String, Object> map = new HashMap<>();

        for (AttributeKvEntry entry : entries) {
            Object value = entry.getValue();
            if (value != null) {
                map.put(entry.getKey(), value);
            }
        }

        return map;
    }

    /**
     * Gets specific attributes as a map.
     */
    public Map<String, Object> getAttributesAsMap(UUID assetId, List<String> keys) {
        List<AttributeKvEntry> entries = getServerAttributes(assetId, keys);
        Map<String, Object> map = new HashMap<>();

        for (AttributeKvEntry entry : entries) {
            Object value = entry.getValue();
            if (value != null) {
                map.put(entry.getKey(), value);
            }
        }

        return map;
    }

    /**
     * Removes attributes by keys.
     */
    public void removeAttributes(UUID assetId, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        EntityId entityId = new AssetId(assetId);
        try {
            ListenableFuture<?> future = tbAttributesService.removeAll(
                    TenantId.SYS_TENANT_ID,
                    entityId,
                    AttributeScope.SERVER_SCOPE,
                    keys
            );
            future.get();
            log.debug("Removed {} attributes from asset {}", keys.size(), assetId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error removing attributes from asset {}: {}", assetId, e.getMessage());
        }
    }

    // Helper method to create AttributeKvEntry from key-value pair
    private AttributeKvEntry createAttributeEntry(String key, Object value) {
        if (value == null) {
            return null;
        }

        long ts = System.currentTimeMillis();

        if (value instanceof Double || value instanceof Float) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, ((Number) value).doubleValue()), ts);
        } else if (value instanceof Number) {
            return new BaseAttributeKvEntry(new LongDataEntry(key, ((Number) value).longValue()), ts);
        } else if (value instanceof Boolean) {
            return new BaseAttributeKvEntry(new BooleanDataEntry(key, (Boolean) value), ts);
        } else if (value instanceof String) {
            String strValue = (String) value;
            // Check if it's JSON
            if ((strValue.startsWith("{") && strValue.endsWith("}")) ||
                    (strValue.startsWith("[") && strValue.endsWith("]"))) {
                return new BaseAttributeKvEntry(new JsonDataEntry(key, strValue), ts);
            }
            return new BaseAttributeKvEntry(new StringDataEntry(key, strValue), ts);
        } else {
            // Convert to string
            return new BaseAttributeKvEntry(new StringDataEntry(key, value.toString()), ts);
        }
    }
}
