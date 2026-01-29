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
 * Rule Engine node for calculating Inflow Performance Relationship (IPR).
 *
 * Supports multiple methods:
 * - VOGEL: q = qmax * (1 - 0.2*(Pwf/Pr) - 0.8*(Pwf/Pr)^2) - for saturated reservoirs
 * - DARCY: q = J * (Pr - Pwf) - for undersaturated reservoirs (linear)
 * - FETKOVICH: q = C * (Pr^2 - Pwf^2)^n - for gas wells
 * - JONES: Quadratic form accounting for turbulence
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rv ipr calculation",
        configClazz = RvIPRCalculationNodeConfiguration.class,
        nodeDescription = "Calculate oil/gas flow rate using IPR methods",
        nodeDetails = "Calculates production rate using Inflow Performance Relationship equations.<br/><br/>" +
                "Supported methods:<br/>" +
                "- VOGEL: For saturated reservoirs (below bubble point)<br/>" +
                "- DARCY: Linear IPR for undersaturated reservoirs<br/>" +
                "- FETKOVICH: For gas wells<br/>" +
                "- JONES: Quadratic IPR accounting for turbulence<br/><br/>" +
                "Input: Reservoir pressure, flowing pressure, and method-specific parameters.<br/>" +
                "Output: Production rate (bpd or Mscfd depending on context).",
        configDirective = "tbActionNodeRvIPRConfig",
        icon = "trending_up",
        docUrl = "https://nexus.thingsboard.io/docs/rv-module/ipr-calculation"
)
public class RvIPRCalculationNode implements TbNode {

    private RvIPRCalculationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, RvIPRCalculationNodeConfiguration.class);
        log.info("RvIPRCalculationNode initialized with method: {}", config.getMethod());
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

            // Extract common input values
            double pr = extractDouble(body, config.getReservoirPressureField(), "Reservoir Pressure");
            double pwf = extractDouble(body, config.getFlowingPressureField(), "Flowing Pressure");

            // Validate common inputs
            if (pr <= 0) {
                throw new RuntimeException("Reservoir pressure must be positive");
            }
            if (pwf < 0) {
                throw new RuntimeException("Flowing pressure cannot be negative");
            }
            if (pwf > pr) {
                throw new RuntimeException("Flowing pressure cannot exceed reservoir pressure");
            }

            // Calculate flow rate based on method
            double flowRate = calculateIPR(body, pr, pwf, config.getMethod());

            log.debug("IPR flow rate calculated: {} using method {} for entity {}",
                    flowRate, config.getMethod(), msg.getOriginator());

            // Round result
            BigDecimal result = BigDecimal.valueOf(flowRate)
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
                        ctx.tellFailure(msg, new RuntimeException("Failed to save IPR result", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                ctx.tellSuccess(updatedMsg);
            }

        } catch (Exception e) {
            log.error("Error calculating IPR: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private double calculateIPR(ObjectNode body, double pr, double pwf,
                                 RvIPRCalculationNodeConfiguration.IprMethod method) {
        switch (method) {
            case VOGEL:
                return calculateVogel(body, pr, pwf);
            case DARCY:
                return calculateDarcy(body, pr, pwf);
            case FETKOVICH:
                return calculateFetkovich(body, pr, pwf);
            case JONES:
                return calculateJones(body, pr, pwf);
            default:
                throw new RuntimeException("Unknown IPR method: " + method);
        }
    }

    /**
     * Vogel equation for saturated reservoirs.
     * q = qmax * (1 - 0.2*(Pwf/Pr) - 0.8*(Pwf/Pr)^2)
     */
    private double calculateVogel(ObjectNode body, double pr, double pwf) {
        double qmax = extractDouble(body, config.getQmaxField(), "Qmax");
        if (qmax <= 0) {
            throw new RuntimeException("Qmax must be positive");
        }

        double ratio = pwf / pr;
        return qmax * (1 - 0.2 * ratio - 0.8 * ratio * ratio);
    }

    /**
     * Darcy/Linear IPR for undersaturated reservoirs.
     * q = J * (Pr - Pwf)
     */
    private double calculateDarcy(ObjectNode body, double pr, double pwf) {
        double j = extractDouble(body, config.getProductivityIndexField(), "Productivity Index");
        if (j <= 0) {
            throw new RuntimeException("Productivity Index must be positive");
        }

        return j * (pr - pwf);
    }

    /**
     * Fetkovich equation for gas wells.
     * q = C * (Pr^2 - Pwf^2)^n
     */
    private double calculateFetkovich(ObjectNode body, double pr, double pwf) {
        double c = extractDouble(body, config.getCField(), "C coefficient");
        double n = extractDoubleWithDefault(body, config.getNField(), 1.0);

        if (c <= 0) {
            throw new RuntimeException("C coefficient must be positive");
        }
        if (n < 0.5 || n > 1.0) {
            log.warn("Fetkovich n exponent {} is outside typical range (0.5-1.0)", n);
        }

        double pressureTerm = pr * pr - pwf * pwf;
        return c * Math.pow(pressureTerm, n);
    }

    /**
     * Jones equation (quadratic IPR with turbulence).
     * (Pr - Pwf) = a*q + b*q^2
     * Solving for q: q = (-a + sqrt(a^2 + 4*b*(Pr-Pwf))) / (2*b)
     */
    private double calculateJones(ObjectNode body, double pr, double pwf) {
        double a = extractDouble(body, config.getACoeffField(), "a coefficient");
        double b = extractDouble(body, config.getBCoeffField(), "b coefficient");

        if (b <= 0) {
            throw new RuntimeException("b coefficient must be positive");
        }

        double drawdown = pr - pwf;
        double discriminant = a * a + 4 * b * drawdown;

        if (discriminant < 0) {
            throw new RuntimeException("Invalid Jones equation parameters (negative discriminant)");
        }

        return (-a + Math.sqrt(discriminant)) / (2 * b);
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

    private double extractDoubleWithDefault(ObjectNode body, String fieldName, double defaultValue) {
        if (fieldName == null || fieldName.isEmpty()) {
            return defaultValue;
        }
        JsonNode node = body.get(fieldName);
        if (node == null || node.isNull() || !node.isNumber()) {
            return defaultValue;
        }
        return node.asDouble();
    }
}
