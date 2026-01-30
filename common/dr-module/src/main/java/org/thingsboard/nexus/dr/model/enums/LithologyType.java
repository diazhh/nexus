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
package org.thingsboard.nexus.dr.model.enums;

/**
 * Enumeration of lithology (rock) types for mud logging.
 */
public enum LithologyType {
    // Clastic Sedimentary Rocks
    SANDSTONE,
    SILTSTONE,
    SHALE,
    CLAYSTONE,
    MUDSTONE,
    CONGLOMERATE,
    BRECCIA,

    // Carbonate Rocks
    LIMESTONE,
    DOLOMITE,
    CHALK,
    MARL,

    // Evaporites
    SALT,
    ANHYDRITE,
    GYPSUM,
    HALITE,

    // Igneous Rocks
    GRANITE,
    BASALT,
    VOLCANIC_ASH,
    TUFF,

    // Metamorphic Rocks
    SLATE,
    SCHIST,
    QUARTZITE,
    MARBLE,
    GNEISS,

    // Coal and Organic
    COAL,
    LIGNITE,

    // Other
    CHERT,
    DIATOMITE,
    BENTONITE,
    PYRITE,
    UNKNOWN,
    OTHER
}
