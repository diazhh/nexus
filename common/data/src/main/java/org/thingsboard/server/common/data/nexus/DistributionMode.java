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
package org.thingsboard.server.common.data.nexus;

/**
 * Enum representing the distribution mode for data from devices to Digital Twins.
 */
public enum DistributionMode {
    /**
     * Direct distribution - data goes directly to target asset without transformation
     */
    DIRECT,

    /**
     * Mapped distribution - data is transformed according to mapping rules
     */
    MAPPED,

    /**
     * Hierarchical distribution - data is distributed to asset hierarchy
     */
    HIERARCHICAL
}
