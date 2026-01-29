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
import java.math.RoundingMode;

/**
 * Rule Engine node for calculating Water Saturation (Sw) using Archie equation.
 *
 * Formula: Sw = (a * Rw / (φ^m * Rt))^(1/n)
 *
 * Where:
 * - a = Tortuosity factor (typically 1.0)
 * - Rw = Formation water resistivity (ohm-m)
 * - φ = Porosity (fraction 0-1)
 * - m = Cementation exponent (typically 2.0)
 * - Rt = True formation resistivity (ohm-m)
 * - n = Saturation exponent (typically 2.0)
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rv sw calculation",
        configClazz = RvSwCalculationNodeConfiguration.class,
        nodeDescription = "Calculate Water Saturation (Sw) using Archie equation",
        nodeDetails = "Calculates water saturation using the Archie equation:<br/>" +
                "Sw = (a × Rw / (φ^m × Rt))^(1/n)<br/><br/>" +
                "Default Archie parameters:<br/>" +
                "- a (tortuosity) = 1.0<br/>" +
                "- m (cementation) = 2.0<br/>" +
                "- n (saturation) = 2.0<br/><br/>" +
                "Parameters can be overridden via configuration or read from message body.",
        configDirective = "tbActionNodeRvSwConfig",
        icon = "water_drop",
        docUrl = "https://nexus.thingsboard.io/docs/rv-module/sw-calculation"
)
public class RvSwCalculationNode implements TbNode {

    private RvSwCalculationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, RvSwCalculationNodeConfiguration.class);
        log.info("RvSwCalculationNode initialized with Archie params: a={}, m={}, n={}",
                config.getTortuosityFactor(), config.getCementationExponent(), config.getSaturationExponent());
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

            // Extract required input values
            double porosity = extractDouble(body, config.getPorosityField(), "Porosity");
            double rw = extractDouble(body, config.getRwField(), "Rw");
            double rt = extractDouble(body, config.getRtField(), "Rt");

            // Get Archie parameters (from message or defaults)
            double a = getArchieParam(body, config.getTortuosityFactorField(), config.getTortuosityFactor(), 1.0);
            double m = getArchieParam(body, config.getCementationExponentField(), config.getCementationExponent(), 2.0);
            double n = getArchieParam(body, config.getSaturationExponentField(), config.getSaturationExponent(), 2.0);

            // Validate inputs
            if (porosity <= 0 || porosity > 1) {
                throw new RuntimeException("Porosity must be between 0 and 1, got: " + porosity);
            }
            if (rw <= 0) {
                throw new RuntimeException("Rw must be positive, got: " + rw);
            }
            if (rt <= 0) {
                throw new RuntimeException("Rt must be positive, got: " + rt);
            }

            // Calculate Sw using Archie equation
            double sw = calculateSwArchie(porosity, rw, rt, a, m, n);

            log.debug("Sw calculated: {} for entity {} (porosity={}, Rw={}, Rt={}, a={}, m={}, n={})",
                    sw, msg.getOriginator(), porosity, rw, rt, a, m, n);

            // Round result
            BigDecimal result = BigDecimal.valueOf(sw)
                    .setScale(config.getResultPrecision(), RoundingMode.HALF_UP);

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
                        ctx.tellFailure(msg, new RuntimeException("Failed to save Sw result", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                ctx.tellSuccess(updatedMsg);
            }

        } catch (Exception e) {
            log.error("Error calculating Sw: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private double calculateSwArchie(double porosity, double rw, double rt, double a, double m, double n) {
        // φ^m
        double phiPowM = Math.pow(porosity, m);

        // a * Rw / (φ^m * Rt)
        double ratio = a * rw / (phiPowM * rt);

        // Sw = ratio^(1/n)
        double sw = Math.pow(ratio, 1.0 / n);

        // Clamp to [0, 1]
        return Math.max(0.0, Math.min(1.0, sw));
    }

    private double getArchieParam(ObjectNode body, String fieldName, Double configValue, double defaultValue) {
        // First try to read from message body
        if (fieldName != null && !fieldName.isEmpty()) {
            JsonNode node = body.get(fieldName);
            if (node != null && node.isNumber()) {
                return node.asDouble();
            }
        }
        // Then use config value
        if (configValue != null) {
            return configValue;
        }
        // Finally use default
        return defaultValue;
    }

    private TbMsg updateMessage(TbMsg msg, ObjectNode body, BigDecimal result) {
        body.put(config.getOutputField(), result.doubleValue());

        TbMsg.TbMsgBuilder builder = msg.transform()
                .data(JacksonUtil.toString(body));

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

    private double extractDouble(ObjectNode body, String fieldName, String displayName) {
        JsonNode node = body.get(fieldName);
        if (node == null || node.isNull()) {
            throw new RuntimeException(displayName + " field '" + fieldName + "' not found in message body");
        }
        if (!node.isNumber()) {
            throw new RuntimeException(displayName + " field '" + fieldName + "' must be a number");
        }
        return node.asDouble();
    }
}
