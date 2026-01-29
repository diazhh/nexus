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
package org.thingsboard.nexus.rv.exception;

import java.util.UUID;

/**
 * Exception thrown when a Reservoir entity is not found.
 */
public class RvEntityNotFoundException extends RvException {

    private final String entityType;
    private final UUID entityId;

    public RvEntityNotFoundException(String message) {
        super(message);
        this.entityType = "Unknown";
        this.entityId = null;
    }

    public RvEntityNotFoundException(String entityType, UUID entityId) {
        super(String.format("%s not found with ID: %s", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public RvEntityNotFoundException(String entityType, String identifier) {
        super(String.format("%s not found: %s", entityType, identifier));
        this.entityType = entityType;
        this.entityId = null;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }
}
