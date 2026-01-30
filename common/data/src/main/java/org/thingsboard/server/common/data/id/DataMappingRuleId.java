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
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema
public class DataMappingRuleId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public DataMappingRuleId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static DataMappingRuleId fromString(String dataMappingRuleId) {
        return new DataMappingRuleId(UUID.fromString(dataMappingRuleId));
    }

    @Schema(description = "ID of the data mapping rule, time-based UUID v1", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    @Override
    public UUID getId() {
        return super.getId();
    }

    @Schema(description = "Type of the entity", example = "DATA_MAPPING_RULE")
    @Override
    public EntityType getEntityType() {
        return EntityType.DATA_MAPPING_RULE;
    }
}
