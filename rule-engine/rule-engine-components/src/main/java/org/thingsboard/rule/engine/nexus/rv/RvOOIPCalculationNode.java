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
package org.thingsboard.rule.engine.nexus.rv;

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
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Rule Engine node for calculating Original Oil In Place (OOIP).
 *
 * Formula: OOIP = 7758 * A * h * φ * (1 - Sw) / Bo
 *
 * Where:
 * - A = Area (acres)
 * - h = Net pay thickness (meters, converted to feet internally)
 * - φ = Porosity (fraction 0-1)
 * - Sw = Water saturation (fraction 0-1)
 * - Bo = Formation volume factor (rb/stb)
 *
 * Result: OOIP in STB (Stock Tank Barrels)
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rv ooip calculation",
        configClazz = RvOOIPCalculationNodeConfiguration.class,
        nodeDescription = "Calculate Original Oil In Place (OOIP) using volumetric method",
        nodeDetails = "Calculates OOIP using the formula: OOIP = 7758 * A * h * φ * (1 - Sw) / Bo.<br/><br/>" +
                "Input parameters are read from the message body using configurable field names.<br/>" +
                "The result can be saved to message body, metadata, attributes, or time-series.<br/><br/>" +
                "Units: Area in acres, thickness in meters (converted to feet), porosity and Sw as fractions (0-1), Bo in rb/stb.<br/>" +
                "Result: OOIP in STB.",
        configDirective = "tbActionNodeRvOOIPConfig",
        icon = "calculate",
        docUrl = "https://nexus.thingsboard.io/docs/rv-module/ooip-calculation"
)
public class RvOOIPCalculationNode implements TbNode {

    private static final BigDecimal CONSTANT_7758 = new BigDecimal("7758");
    private static final BigDecimal METERS_TO_FEET = new BigDecimal("3.28084");
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private RvOOIPCalculationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, RvOOIPCalculationNodeConfiguration.class);
        log.info("RvOOIPCalculationNode initialized with config: {}", config);
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

            // Extract input values
            BigDecimal areaAcres = extractBigDecimal(body, config.getAreaAcresField(), "Area");
            BigDecimal thicknessM = extractBigDecimal(body, config.getThicknessMField(), "Thickness");
            BigDecimal porosity = extractBigDecimal(body, config.getPorosityField(), "Porosity");
            BigDecimal waterSat = extractBigDecimal(body, config.getWaterSaturationField(), "Water Saturation");
            BigDecimal bo = extractBigDecimal(body, config.getBoField(), "Bo");

            // Validate inputs
            validateRange("Porosity", porosity, BigDecimal.ZERO, BigDecimal.ONE);
            validateRange("Water Saturation", waterSat, BigDecimal.ZERO, BigDecimal.ONE);
            validatePositive("Bo", bo);
            validatePositive("Area", areaAcres);
            validatePositive("Thickness", thicknessM);

            // Calculate OOIP
            BigDecimal ooip = calculateOOIP(areaAcres, thicknessM, porosity, waterSat, bo);

            log.debug("OOIP calculated: {} STB for entity {}", ooip, msg.getOriginator());

            // Round result
            BigDecimal result = ooip.setScale(config.getResultPrecision(), RoundingMode.HALF_UP);

            // Update message and/or save to database
            TbMsg updatedMsg = updateMessage(msg, body, result);

            if (config.isSaveAsAttribute() || config.isSaveAsTelemetry()) {
                ListenableFuture<Void> saveFuture = saveToDatabase(ctx, msg, result);
                Futures.addCallback(saveFuture, new com.google.common.util.concurrent.FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        ctx.tellSuccess(updatedMsg);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ctx.tellFailure(msg, new RuntimeException("Failed to save OOIP result", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                ctx.tellSuccess(updatedMsg);
            }

        } catch (Exception e) {
            log.error("Error calculating OOIP: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private BigDecimal calculateOOIP(BigDecimal areaAcres, BigDecimal thicknessM,
                                      BigDecimal porosity, BigDecimal waterSat, BigDecimal bo) {
        // Convert meters to feet
        BigDecimal thicknessFt = thicknessM.multiply(METERS_TO_FEET, MC);

        // Oil saturation = 1 - Sw
        BigDecimal oilSat = BigDecimal.ONE.subtract(waterSat);

        // OOIP = 7758 * A * h * φ * (1 - Sw) / Bo
        return CONSTANT_7758
                .multiply(areaAcres, MC)
                .multiply(thicknessFt, MC)
                .multiply(porosity, MC)
                .multiply(oilSat, MC)
                .divide(bo, MC);
    }

    private TbMsg updateMessage(TbMsg msg, ObjectNode body, BigDecimal result) {
        // Add to message body
        body.put(config.getOutputField(), result.doubleValue());

        TbMsg.TbMsgBuilder builder = msg.transform()
                .data(JacksonUtil.toString(body));

        // Add to metadata if configured
        if (config.isAddToMetadata()) {
            var md = msg.getMetaData();
            md.putValue(config.getOutputField(), result.toPlainString());
            builder.metaData(md);
        }

        return builder.build();
    }

    private ListenableFuture<Void> saveToDatabase(TbContext ctx, TbMsg msg, BigDecimal result) {
        if (config.isSaveAsAttribute()) {
            return saveAsAttribute(ctx, msg, result);
        } else if (config.isSaveAsTelemetry()) {
            return saveAsTelemetry(ctx, msg, result);
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> saveAsAttribute(TbContext ctx, TbMsg msg, BigDecimal result) {
        AttributeScope scope = AttributeScope.valueOf(config.getAttributeScope());
        DoubleDataEntry kvEntry = new DoubleDataEntry(config.getOutputField(), result.doubleValue());

        SettableFuture<Void> future = SettableFuture.create();
        ctx.getTelemetryService().saveAttributes(AttributesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .scope(scope)
                .entry(kvEntry)
                .future(future)
                .build());
        return future;
    }

    private ListenableFuture<Void> saveAsTelemetry(TbContext ctx, TbMsg msg, BigDecimal result) {
        BasicTsKvEntry tsEntry = new BasicTsKvEntry(
                System.currentTimeMillis(),
                new DoubleDataEntry(config.getOutputField(), result.doubleValue())
        );

        SettableFuture<Void> future = SettableFuture.create();
        ctx.getTelemetryService().saveTimeseries(TimeseriesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .entry(tsEntry)
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

    private void validateRange(String name, BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new RuntimeException(String.format("%s must be between %s and %s, got %s", name, min, max, value));
        }
    }

    private void validatePositive(String name, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(String.format("%s must be positive, got %s", name, value));
        }
    }
}
