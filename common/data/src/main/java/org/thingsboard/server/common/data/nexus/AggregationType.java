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
 * Enum representing the type of aggregation applied to data from child to parent assets.
 */
public enum AggregationType {
    /**
     * Sum - add all values together
     */
    SUM,

    /**
     * Average - calculate the arithmetic mean
     */
    AVG,

    /**
     * Minimum - find the smallest value
     */
    MIN,

    /**
     * Maximum - find the largest value
     */
    MAX,

    /**
     * Count - count the number of values
     */
    COUNT
}
