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
package org.thingsboard.nexus.pf.exception;

import java.util.UUID;

/**
 * Exception thrown when a PF entity is not found
 */
public class PfEntityNotFoundException extends PfException {

    public PfEntityNotFoundException(String entityType, UUID entityId) {
        super(String.format("%s not found with ID: %s", entityType, entityId));
    }

    public PfEntityNotFoundException(String message) {
        super(message);
    }
}
