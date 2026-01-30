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
package org.thingsboard.nexus.dr.service;

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
import org.thingsboard.server.dao.attributes.AttributesService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing attributes on Drilling Assets
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrAttributeService {

    private final AttributesService attributesService;

    /**
     * Save server-scope attributes for an entity
     */
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

    /**
     * Save shared-scope attributes for an entity
     */
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
     * Create an AttributeKvEntry from a key-value pair
     */
    private AttributeKvEntry createKvEntry(String key, Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return new BaseAttributeKvEntry(new StringDataEntry(key, (String) value), System.currentTimeMillis());
        } else if (value instanceof Integer) {
            return new BaseAttributeKvEntry(new LongDataEntry(key, ((Integer) value).longValue()), System.currentTimeMillis());
        } else if (value instanceof Long) {
            return new BaseAttributeKvEntry(new LongDataEntry(key, (Long) value), System.currentTimeMillis());
        } else if (value instanceof Double) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, (Double) value), System.currentTimeMillis());
        } else if (value instanceof Float) {
            return new BaseAttributeKvEntry(new DoubleDataEntry(key, ((Float) value).doubleValue()), System.currentTimeMillis());
        } else if (value instanceof Boolean) {
            return new BaseAttributeKvEntry(new BooleanDataEntry(key, (Boolean) value), System.currentTimeMillis());
        } else {
            return new BaseAttributeKvEntry(new StringDataEntry(key, value.toString()), System.currentTimeMillis());
        }
    }
}
