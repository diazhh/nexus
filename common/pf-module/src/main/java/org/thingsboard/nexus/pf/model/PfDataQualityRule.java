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
package org.thingsboard.nexus.pf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA Entity for Data Quality Rules.
 * Stored in pf.data_quality_rule table.
 */
@Entity
@Table(name = "data_quality_rule", schema = "pf",
        uniqueConstraints = @UniqueConstraint(columnNames = {"variable_key", "entity_type", "entity_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfDataQualityRule {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "variable_key", nullable = false)
    private String variableKey;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "physical_min", precision = 18, scale = 4)
    private BigDecimal physicalMin;

    @Column(name = "physical_max", precision = 18, scale = 4)
    private BigDecimal physicalMax;

    @Column(name = "expected_min", precision = 18, scale = 4)
    private BigDecimal expectedMin;

    @Column(name = "expected_max", precision = 18, scale = 4)
    private BigDecimal expectedMax;

    @Column(name = "max_rate_of_change_percent_per_minute", precision = 10, scale = 4)
    private BigDecimal maxRateOfChangePercentPerMinute;

    @Column(name = "max_absolute_change_per_minute", precision = 18, scale = 4)
    private BigDecimal maxAbsoluteChangePerMinute;

    @Column(name = "outlier_sigma_threshold", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal outlierSigmaThreshold = BigDecimal.valueOf(3.0);

    @Column(name = "min_samples_for_statistics")
    @Builder.Default
    private Integer minSamplesForStatistics = 30;

    @Column(name = "unit")
    private String unit;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_time", nullable = false)
    private Long createdTime;

    @Column(name = "updated_time")
    private Long updatedTime;

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) {
            createdTime = System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = System.currentTimeMillis();
    }
}
