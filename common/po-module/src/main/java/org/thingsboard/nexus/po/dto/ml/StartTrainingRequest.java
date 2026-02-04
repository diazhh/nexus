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
package org.thingsboard.nexus.po.dto.ml;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for starting a training job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartTrainingRequest {

    @NotBlank(message = "Model name is required")
    private String modelName;

    @NotNull(message = "Data start date is required")
    private LocalDate dataStartDate;

    @NotNull(message = "Data end date is required")
    private LocalDate dataEndDate;

    /**
     * Optional hyperparameters to override defaults.
     * Keys may include: lstmUnits, dropoutRate, learningRate, epochs, batchSize, earlyStopEnabled
     */
    private Map<String, Object> hyperparameters;
}
