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
package org.thingsboard.nexus.ct.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CTReportRequest {
    
    private ReportType reportType;
    private ReportFormat format;
    private Long startDate;
    private Long endDate;
    private UUID entityId;
    private UUID tenantId;
    
    public enum ReportType {
        JOB_SUMMARY,
        REEL_LIFECYCLE,
        FLEET_UTILIZATION,
        FATIGUE_ANALYSIS,
        MAINTENANCE_SCHEDULE
    }
    
    public enum ReportFormat {
        PDF,
        EXCEL,
        CSV
    }
}
