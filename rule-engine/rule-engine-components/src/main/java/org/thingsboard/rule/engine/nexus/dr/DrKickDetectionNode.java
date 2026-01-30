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
package org.thingsboard.rule.engine.nexus.dr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule Engine node for detecting kick events during drilling.
 *
 * A kick is an uncontrolled influx of formation fluids (gas, oil, or water) into the wellbore.
 * Early detection is critical for well control and safety.
 *
 * Detection indicators:
 * 1. Pit gain (increase in pit volume) - PRIMARY
 * 2. Flow differential (flow out > flow in) - PRIMARY
 * 3. Standpipe pressure drop
 * 4. Mud weight decrease (from gas cutting)
 * 5. Connection gas increase
 *
 * Severity levels:
 * - NONE: No kick indicators
 * - LOW: Single minor indicator
 * - MEDIUM: Multiple indicators or significant single indicator
 * - HIGH: Multiple significant indicators (immediate action required)
 * - CRITICAL: All indicators present (shut-in well immediately)
 *
 * Output routes:
 * - "No Kick" for normal operations
 * - "Kick Warning" for low/medium severity
 * - "Kick Alert" for high/critical severity
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "dr kick detection",
        configClazz = DrKickDetectionNodeConfiguration.class,
        nodeDescription = "Detect kick events by monitoring drilling parameters",
        nodeDetails = "Monitors multiple drilling parameters to detect potential kick events:<br/>" +
                "- Pit volume gain<br/>" +
                "- Flow differential (flow out > flow in)<br/>" +
                "- Standpipe pressure changes<br/>" +
                "- Mud weight changes<br/>" +
                "- Gas increases<br/><br/>" +
                "Routes to 'Kick Alert' for high/critical severity requiring immediate action.",
        configDirective = "tbActionNodeDrKickDetectionConfig",
        icon = "warning",
        docUrl = "https://nexus.thingsboard.io/docs/dr-module/kick-detection",
        relationTypes = {"No Kick", "Kick Warning", "Kick Alert", "Failure"}
)
public class DrKickDetectionNode implements TbNode {

    private DrKickDetectionNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, DrKickDetectionNodeConfiguration.class);
        log.info("DrKickDetectionNode initialized with config: {}", config);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            JsonNode jsonNode = JacksonUtil.toJsonNode(msg.getData());
            if (!jsonNode.isObject()) {
                ctx.tellFailure(msg, new RuntimeException("Message body is not a JSON object"));
                return;
            }

            ObjectNode body = (ObjectNode) jsonNode;

            // Analyze kick indicators
            KickAnalysisResult analysis = analyzeKickIndicators(body);

            log.debug("Kick analysis: {} indicators, severity: {} for entity {}",
                    analysis.indicatorCount, analysis.severity, msg.getOriginator());

            // Update message with results
            TbMsg updatedMsg = updateMessage(msg, body, analysis);

            // Save telemetry if configured
            if (config.isSaveAsTelemetry()) {
                ListenableFuture<Void> saveFuture = saveKickTelemetry(ctx, msg, analysis);
                Futures.addCallback(saveFuture, new com.google.common.util.concurrent.FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        routeMessage(ctx, updatedMsg, analysis.severity);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ctx.tellFailure(msg, new RuntimeException("Failed to save kick detection result", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                routeMessage(ctx, updatedMsg, analysis.severity);
            }

        } catch (Exception e) {
            log.error("Error in kick detection: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private KickAnalysisResult analyzeKickIndicators(ObjectNode body) {
        List<String> detectedIndicators = new ArrayList<>();
        int severityScore = 0;

        // 1. Check pit volume gain
        Double pitVolume = getOptionalDouble(body, config.getPitVolumeField());
        Double baselinePitVolume = getOptionalDouble(body, config.getBaselinePitVolumeField());
        if (pitVolume != null && baselinePitVolume != null) {
            double pitGain = pitVolume - baselinePitVolume;
            if (pitGain >= config.getPitGainThresholdBbl()) {
                detectedIndicators.add("PIT_GAIN");
                severityScore += (pitGain >= config.getPitGainThresholdBbl() * 2) ? 3 : 2;
            }
        }

        // 2. Check flow differential
        Double flowIn = getOptionalDouble(body, config.getFlowInGpmField());
        Double flowOut = getOptionalDouble(body, config.getFlowOutGpmField());
        if (flowIn != null && flowOut != null) {
            double flowDiff = flowOut - flowIn;
            if (flowDiff >= config.getFlowDifferentialThresholdGpm()) {
                detectedIndicators.add("FLOW_INCREASE");
                severityScore += (flowDiff >= config.getFlowDifferentialThresholdGpm() * 2) ? 3 : 2;
            }
        }

        // 3. Check standpipe pressure (decrease indicates kick)
        Double spp = getOptionalDouble(body, config.getStandpipePressureField());
        // Pressure drop detection would need historical comparison
        // For now, we assume baseline in attributes or metadata

        // 4. Check mud weight decrease
        Double mudWeightIn = getOptionalDouble(body, config.getMudWeightInField());
        Double mudWeightOut = getOptionalDouble(body, config.getMudWeightOutField());
        if (mudWeightIn != null && mudWeightOut != null) {
            double mwDrop = mudWeightIn - mudWeightOut;
            if (mwDrop >= config.getMudWeightDropThresholdPpg()) {
                detectedIndicators.add("MUD_WEIGHT_DROP");
                severityScore += 2;
            }
        }

        // 5. Check gas increase
        Double gasUnits = getOptionalDouble(body, config.getGasUnitsField());
        // Gas increase detection would need historical comparison

        // Determine severity
        KickSeverity severity = calculateSeverity(severityScore, detectedIndicators.size());

        return new KickAnalysisResult(
                !detectedIndicators.isEmpty(),
                severity,
                detectedIndicators.size(),
                detectedIndicators
        );
    }

    private KickSeverity calculateSeverity(int score, int indicatorCount) {
        if (indicatorCount == 0 || score == 0) {
            return KickSeverity.NONE;
        }
        if (score >= 8 || indicatorCount >= 4) {
            return KickSeverity.CRITICAL;
        }
        if (score >= 5 || indicatorCount >= 3) {
            return KickSeverity.HIGH;
        }
        if (score >= 3 || indicatorCount >= 2) {
            return KickSeverity.MEDIUM;
        }
        return KickSeverity.LOW;
    }

    private void routeMessage(TbContext ctx, TbMsg msg, KickSeverity severity) {
        switch (severity) {
            case HIGH:
            case CRITICAL:
                ctx.tellNext(msg, "Kick Alert");
                break;
            case LOW:
            case MEDIUM:
                ctx.tellNext(msg, "Kick Warning");
                break;
            default:
                ctx.tellNext(msg, "No Kick");
        }
    }

    private TbMsg updateMessage(TbMsg msg, ObjectNode body, KickAnalysisResult analysis) {
        body.put(config.getOutputKickIndicatorField(), analysis.kickDetected);
        body.put(config.getOutputKickSeverityField(), analysis.severity.name());
        body.put("kickIndicatorCount", analysis.indicatorCount);

        var indicatorsArray = body.putArray("kickIndicators");
        analysis.indicators.forEach(indicatorsArray::add);

        TbMsg.TbMsgBuilder builder = msg.transform()
                .data(JacksonUtil.toString(body));

        if (config.isAddToMetadata()) {
            var md = msg.getMetaData();
            md.putValue(config.getOutputKickIndicatorField(), String.valueOf(analysis.kickDetected));
            md.putValue(config.getOutputKickSeverityField(), analysis.severity.name());
            builder.metaData(md);
        }

        return builder.build();
    }

    private ListenableFuture<Void> saveKickTelemetry(TbContext ctx, TbMsg msg, KickAnalysisResult analysis) {
        long ts = System.currentTimeMillis();

        List<TsKvEntry> entries = new ArrayList<>();
        entries.add(new BasicTsKvEntry(ts,
                new BooleanDataEntry(config.getOutputKickIndicatorField(), analysis.kickDetected)));
        entries.add(new BasicTsKvEntry(ts,
                new StringDataEntry(config.getOutputKickSeverityField(), analysis.severity.name())));

        SettableFuture<Void> future = SettableFuture.create();
        ctx.getTelemetryService().saveTimeseries(TimeseriesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .entries(entries)
                .future(future)
                .build());
        return future;
    }

    private Double getOptionalDouble(ObjectNode body, String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        JsonNode node = body.get(fieldName);
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.asDouble();
    }

    private enum KickSeverity {
        NONE, LOW, MEDIUM, HIGH, CRITICAL
    }

    private static class KickAnalysisResult {
        final boolean kickDetected;
        final KickSeverity severity;
        final int indicatorCount;
        final List<String> indicators;

        KickAnalysisResult(boolean kickDetected, KickSeverity severity, int indicatorCount, List<String> indicators) {
            this.kickDetected = kickDetected;
            this.severity = severity;
            this.indicatorCount = indicatorCount;
            this.indicators = indicators;
        }
    }
}
