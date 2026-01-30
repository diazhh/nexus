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
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule Engine node for processing directional survey data using Minimum Curvature Method.
 *
 * Calculates:
 * - TVD (True Vertical Depth)
 * - Northing and Easting coordinates
 * - Dog Leg Severity (DLS)
 * - Vertical Section
 * - Closure Distance and Direction
 *
 * The Minimum Curvature Method assumes the wellbore follows the smoothest possible
 * path between two survey stations. It uses a ratio factor (RF) to account for wellbore curvature.
 *
 * Output routes:
 * - "Success" for normal surveys
 * - "High DLS" when DLS exceeds threshold (indicates high curvature stress)
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "dr survey processing",
        configClazz = DrSurveyProcessingNodeConfiguration.class,
        nodeDescription = "Process directional survey data using Minimum Curvature Method",
        nodeDetails = "Calculates wellbore trajectory using the industry-standard Minimum Curvature Method:<br/>" +
                "- TVD (True Vertical Depth)<br/>" +
                "- Northing and Easting coordinates<br/>" +
                "- Dog Leg Severity (DLS)<br/>" +
                "- Vertical Section<br/>" +
                "- Closure Distance and Direction<br/><br/>" +
                "Routes to 'High DLS' when dogleg severity exceeds threshold.",
        configDirective = "tbActionNodeDrSurveyConfig",
        icon = "explore",
        docUrl = "https://nexus.thingsboard.io/docs/dr-module/survey-processing",
        relationTypes = {"Success", "High DLS", "Failure"}
)
public class DrSurveyProcessingNode implements TbNode {

    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private DrSurveyProcessingNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, DrSurveyProcessingNodeConfiguration.class);
        log.info("DrSurveyProcessingNode initialized with config: {}", config);
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

            // Extract current survey
            BigDecimal mdFt = extractBigDecimal(body, config.getMdFtField(), "MD");
            BigDecimal incDeg = extractBigDecimal(body, config.getInclinationDegField(), "Inclination");
            BigDecimal azDeg = extractBigDecimal(body, config.getAzimuthDegField(), "Azimuth");

            // Extract previous survey
            BigDecimal prevMdFt = getOptionalBigDecimal(body, config.getPrevMdFtField());
            BigDecimal prevIncDeg = getOptionalBigDecimal(body, config.getPrevInclinationDegField());
            BigDecimal prevAzDeg = getOptionalBigDecimal(body, config.getPrevAzimuthDegField());
            BigDecimal prevTvdFt = getOptionalBigDecimal(body, config.getPrevTvdFtField());
            BigDecimal prevNorthingFt = getOptionalBigDecimal(body, config.getPrevNorthingFtField());
            BigDecimal prevEastingFt = getOptionalBigDecimal(body, config.getPrevEastingFtField());

            SurveyResult result;

            // If no previous survey, this is tie-in point (surface location)
            if (prevMdFt == null || prevTvdFt == null) {
                result = calculateTieInSurvey(mdFt, incDeg, azDeg);
            } else {
                result = calculateMinimumCurvature(
                        prevMdFt, prevIncDeg, prevAzDeg, prevTvdFt, prevNorthingFt, prevEastingFt,
                        mdFt, incDeg, azDeg
                );
            }

            log.debug("Survey processed: TVD={}, N={}, E={}, DLS={} for entity {}",
                    result.tvdFt, result.northingFt, result.eastingFt, result.dlsDegPer100ft, msg.getOriginator());

            // Update message
            TbMsg updatedMsg = updateMessage(msg, body, result);

            // Determine if DLS is high
            boolean isHighDls = config.isEnableDlsAlert() &&
                    result.dlsDegPer100ft.doubleValue() > config.getDlsThresholdDegPer100ft();

            // Save and route
            if (config.isSaveAsAttribute() || config.isSaveAsTelemetry()) {
                ListenableFuture<Void> saveFuture = saveToDatabase(ctx, msg, result);
                Futures.addCallback(saveFuture, new com.google.common.util.concurrent.FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        routeMessage(ctx, updatedMsg, isHighDls);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ctx.tellFailure(msg, new RuntimeException("Failed to save survey result", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                routeMessage(ctx, updatedMsg, isHighDls);
            }

        } catch (Exception e) {
            log.error("Error processing survey: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private SurveyResult calculateTieInSurvey(BigDecimal mdFt, BigDecimal incDeg, BigDecimal azDeg) {
        // For tie-in point, assume vertical from surface
        // TVD = MD * cos(Inc)
        double incRad = Math.toRadians(incDeg.doubleValue());
        BigDecimal tvdFt = mdFt.multiply(BigDecimal.valueOf(Math.cos(incRad)), MC);

        return new SurveyResult(
                tvdFt.setScale(config.getResultPrecision(), RoundingMode.HALF_UP),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    private SurveyResult calculateMinimumCurvature(
            BigDecimal prevMdFt, BigDecimal prevIncDeg, BigDecimal prevAzDeg,
            BigDecimal prevTvdFt, BigDecimal prevNorthingFt, BigDecimal prevEastingFt,
            BigDecimal mdFt, BigDecimal incDeg, BigDecimal azDeg) {

        // Convert to radians
        double inc1Rad = Math.toRadians(prevIncDeg.doubleValue());
        double az1Rad = Math.toRadians(prevAzDeg.doubleValue());
        double inc2Rad = Math.toRadians(incDeg.doubleValue());
        double az2Rad = Math.toRadians(azDeg.doubleValue());

        // Course length
        BigDecimal courseLength = mdFt.subtract(prevMdFt, MC);

        // Calculate dogleg angle (radians)
        double cosInc1 = Math.cos(inc1Rad);
        double sinInc1 = Math.sin(inc1Rad);
        double cosInc2 = Math.cos(inc2Rad);
        double sinInc2 = Math.sin(inc2Rad);
        double cosAzDiff = Math.cos(az2Rad - az1Rad);

        double cosDogleg = cosInc1 * cosInc2 + sinInc1 * sinInc2 * cosAzDiff;
        // Clamp to [-1, 1] to avoid NaN from acos
        cosDogleg = Math.max(-1.0, Math.min(1.0, cosDogleg));
        double doglegRad = Math.acos(cosDogleg);

        // Ratio Factor (RF) for minimum curvature
        double rf;
        if (doglegRad < 0.0001) {
            rf = 1.0; // Nearly straight section
        } else {
            rf = 2.0 / doglegRad * Math.tan(doglegRad / 2.0);
        }

        // Calculate increments
        double halfCourse = courseLength.doubleValue() / 2.0;

        double deltaTvd = halfCourse * rf * (cosInc1 + cosInc2);
        double deltaNorthing = halfCourse * rf * (sinInc1 * Math.cos(az1Rad) + sinInc2 * Math.cos(az2Rad));
        double deltaEasting = halfCourse * rf * (sinInc1 * Math.sin(az1Rad) + sinInc2 * Math.sin(az2Rad));

        // Calculate new position
        BigDecimal tvdFt = prevTvdFt.add(BigDecimal.valueOf(deltaTvd), MC);
        BigDecimal northingFt = (prevNorthingFt != null ? prevNorthingFt : BigDecimal.ZERO)
                .add(BigDecimal.valueOf(deltaNorthing), MC);
        BigDecimal eastingFt = (prevEastingFt != null ? prevEastingFt : BigDecimal.ZERO)
                .add(BigDecimal.valueOf(deltaEasting), MC);

        // Calculate DLS (degrees per 100 ft)
        double doglegDeg = Math.toDegrees(doglegRad);
        BigDecimal dlsDegPer100ft = BigDecimal.ZERO;
        if (courseLength.compareTo(BigDecimal.ZERO) > 0) {
            dlsDegPer100ft = BigDecimal.valueOf(doglegDeg)
                    .multiply(HUNDRED, MC)
                    .divide(courseLength, MC);
        }

        // Calculate vertical section
        double vsAzRad = Math.toRadians(config.getVerticalSectionAzimuthDeg());
        double verticalSection = northingFt.doubleValue() * Math.cos(vsAzRad)
                + eastingFt.doubleValue() * Math.sin(vsAzRad);

        // Calculate closure
        double closureDistance = Math.sqrt(
                northingFt.doubleValue() * northingFt.doubleValue() +
                        eastingFt.doubleValue() * eastingFt.doubleValue()
        );
        double closureDirection = Math.toDegrees(Math.atan2(eastingFt.doubleValue(), northingFt.doubleValue()));
        if (closureDirection < 0) {
            closureDirection += 360;
        }

        return new SurveyResult(
                tvdFt.setScale(config.getResultPrecision(), RoundingMode.HALF_UP),
                northingFt.setScale(config.getResultPrecision(), RoundingMode.HALF_UP),
                eastingFt.setScale(config.getResultPrecision(), RoundingMode.HALF_UP),
                dlsDegPer100ft.setScale(config.getResultPrecision(), RoundingMode.HALF_UP),
                BigDecimal.valueOf(verticalSection).setScale(config.getResultPrecision(), RoundingMode.HALF_UP),
                BigDecimal.valueOf(closureDistance).setScale(config.getResultPrecision(), RoundingMode.HALF_UP),
                BigDecimal.valueOf(closureDirection).setScale(config.getResultPrecision(), RoundingMode.HALF_UP)
        );
    }

    private void routeMessage(TbContext ctx, TbMsg msg, boolean isHighDls) {
        if (isHighDls) {
            ctx.tellNext(msg, "High DLS");
        } else {
            ctx.tellSuccess(msg);
        }
    }

    private TbMsg updateMessage(TbMsg msg, ObjectNode body, SurveyResult result) {
        body.put(config.getOutputTvdFtField(), result.tvdFt.doubleValue());
        body.put(config.getOutputNorthingFtField(), result.northingFt.doubleValue());
        body.put(config.getOutputEastingFtField(), result.eastingFt.doubleValue());
        body.put(config.getOutputDlsDegPer100ftField(), result.dlsDegPer100ft.doubleValue());
        body.put(config.getOutputVerticalSectionFtField(), result.verticalSectionFt.doubleValue());
        body.put(config.getOutputClosureDistanceFtField(), result.closureDistanceFt.doubleValue());
        body.put(config.getOutputClosureDirectionDegField(), result.closureDirectionDeg.doubleValue());

        TbMsg.TbMsgBuilder builder = msg.transform()
                .data(JacksonUtil.toString(body));

        if (config.isAddToMetadata()) {
            var md = msg.getMetaData();
            md.putValue(config.getOutputTvdFtField(), result.tvdFt.toPlainString());
            md.putValue(config.getOutputDlsDegPer100ftField(), result.dlsDegPer100ft.toPlainString());
            builder.metaData(md);
        }

        return builder.build();
    }

    private ListenableFuture<Void> saveToDatabase(TbContext ctx, TbMsg msg, SurveyResult result) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        if (config.isSaveAsAttribute()) {
            futures.add(saveAsAttribute(ctx, msg, result));
        }
        if (config.isSaveAsTelemetry()) {
            futures.add(saveAsTelemetry(ctx, msg, result));
        }

        return Futures.transform(Futures.allAsList(futures), list -> null, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Void> saveAsAttribute(TbContext ctx, TbMsg msg, SurveyResult result) {
        AttributeScope scope = AttributeScope.valueOf(config.getAttributeScope());

        List<AttributeKvEntry> entries = new ArrayList<>();
        long ts = System.currentTimeMillis();
        entries.add(new BaseAttributeKvEntry(ts, new DoubleDataEntry(config.getOutputTvdFtField(), result.tvdFt.doubleValue())));
        entries.add(new BaseAttributeKvEntry(ts, new DoubleDataEntry(config.getOutputNorthingFtField(), result.northingFt.doubleValue())));
        entries.add(new BaseAttributeKvEntry(ts, new DoubleDataEntry(config.getOutputEastingFtField(), result.eastingFt.doubleValue())));
        entries.add(new BaseAttributeKvEntry(ts, new DoubleDataEntry(config.getOutputDlsDegPer100ftField(), result.dlsDegPer100ft.doubleValue())));

        SettableFuture<Void> future = SettableFuture.create();
        ctx.getTelemetryService().saveAttributes(AttributesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .scope(scope)
                .entries(entries)
                .future(future)
                .build());
        return future;
    }

    private ListenableFuture<Void> saveAsTelemetry(TbContext ctx, TbMsg msg, SurveyResult result) {
        long ts = System.currentTimeMillis();

        List<TsKvEntry> entries = new ArrayList<>();
        entries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(config.getOutputTvdFtField(), result.tvdFt.doubleValue())));
        entries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(config.getOutputNorthingFtField(), result.northingFt.doubleValue())));
        entries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(config.getOutputEastingFtField(), result.eastingFt.doubleValue())));
        entries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(config.getOutputDlsDegPer100ftField(), result.dlsDegPer100ft.doubleValue())));
        entries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(config.getOutputVerticalSectionFtField(), result.verticalSectionFt.doubleValue())));
        entries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(config.getOutputClosureDistanceFtField(), result.closureDistanceFt.doubleValue())));
        entries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(config.getOutputClosureDirectionDegField(), result.closureDirectionDeg.doubleValue())));

        SettableFuture<Void> future = SettableFuture.create();
        ctx.getTelemetryService().saveTimeseries(TimeseriesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .entries(entries)
                .future(future)
                .build());
        return future;
    }

    private BigDecimal extractBigDecimal(ObjectNode body, String fieldName, String displayName) {
        JsonNode node = body.get(fieldName);
        if (node == null || node.isNull()) {
            throw new RuntimeException(displayName + " field '" + fieldName + "' not found in message body");
        }
        if (!node.isNumber()) {
            throw new RuntimeException(displayName + " field '" + fieldName + "' must be a number");
        }
        return BigDecimal.valueOf(node.asDouble());
    }

    private BigDecimal getOptionalBigDecimal(ObjectNode body, String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        JsonNode node = body.get(fieldName);
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return BigDecimal.valueOf(node.asDouble());
    }

    private static class SurveyResult {
        final BigDecimal tvdFt;
        final BigDecimal northingFt;
        final BigDecimal eastingFt;
        final BigDecimal dlsDegPer100ft;
        final BigDecimal verticalSectionFt;
        final BigDecimal closureDistanceFt;
        final BigDecimal closureDirectionDeg;

        SurveyResult(BigDecimal tvdFt, BigDecimal northingFt, BigDecimal eastingFt,
                     BigDecimal dlsDegPer100ft, BigDecimal verticalSectionFt,
                     BigDecimal closureDistanceFt, BigDecimal closureDirectionDeg) {
            this.tvdFt = tvdFt;
            this.northingFt = northingFt;
            this.eastingFt = eastingFt;
            this.dlsDegPer100ft = dlsDegPer100ft;
            this.verticalSectionFt = verticalSectionFt;
            this.closureDistanceFt = closureDistanceFt;
            this.closureDirectionDeg = closureDirectionDeg;
        }
    }
}
