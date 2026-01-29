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
 * Rule Engine node for production decline analysis using Arps equations.
 *
 * Decline Types:
 * - EXPONENTIAL (b=0): q(t) = qi * e^(-Di*t)
 * - HYPERBOLIC (0<b<1): q(t) = qi / (1 + b*Di*t)^(1/b)
 * - HARMONIC (b=1): q(t) = qi / (1 + Di*t)
 *
 * Calculation Types:
 * - RATE_AT_TIME: Calculate production rate at time t
 * - CUMULATIVE_AT_TIME: Calculate cumulative production at time t
 * - TIME_TO_RATE: Calculate time to reach a given rate
 * - EUR: Calculate Estimated Ultimate Recovery (with economic limit)
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rv decline analysis",
        configClazz = RvDeclineAnalysisNodeConfiguration.class,
        nodeDescription = "Calculate production decline using Arps equations",
        nodeDetails = "Performs decline curve analysis using Arps decline equations.<br/><br/>" +
                "Decline Types:<br/>" +
                "- EXPONENTIAL: Constant percentage decline (b=0)<br/>" +
                "- HYPERBOLIC: Variable decline rate (0<b<1)<br/>" +
                "- HARMONIC: Declining percentage decline (b=1)<br/><br/>" +
                "Calculation Types:<br/>" +
                "- RATE_AT_TIME: Forecast rate at future time<br/>" +
                "- CUMULATIVE_AT_TIME: Calculate Np at time t<br/>" +
                "- TIME_TO_RATE: Time to reach abandonment rate<br/>" +
                "- EUR: Estimated Ultimate Recovery",
        configDirective = "tbActionNodeRvDeclineConfig",
        icon = "show_chart",
        docUrl = "https://nexus.thingsboard.io/docs/rv-module/decline-analysis"
)
public class RvDeclineAnalysisNode implements TbNode {

    private RvDeclineAnalysisNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, RvDeclineAnalysisNodeConfiguration.class);
        log.info("RvDeclineAnalysisNode initialized with type: {}, calculation: {}",
                config.getDeclineType(), config.getCalculationType());
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
            double qi = extractDouble(body, config.getInitialRateField(), "Initial Rate");
            double di = extractDouble(body, config.getDeclineRateField(), "Decline Rate");
            double t = extractDouble(body, config.getTimeField(), "Time");

            // Get b exponent based on decline type
            double b = getBExponent(body);

            // Validate inputs
            if (qi <= 0) {
                throw new RuntimeException("Initial rate must be positive");
            }
            if (di <= 0) {
                throw new RuntimeException("Decline rate must be positive");
            }
            if (t < 0) {
                throw new RuntimeException("Time cannot be negative");
            }

            // Calculate based on type
            double result = calculate(qi, di, b, t);

            log.debug("Decline analysis result: {} for entity {} (qi={}, Di={}, b={}, t={})",
                    result, msg.getOriginator(), qi, di, b, t);

            // Round result
            BigDecimal resultBd = BigDecimal.valueOf(result)
                    .setScale(config.getResultPrecision(), RoundingMode.HALF_UP);

            // Update message and/or save to database
            TbMsg updatedMsg = updateMessage(msg, body, resultBd);

            if (config.isSaveAsAttribute() || config.isSaveAsTelemetry()) {
                ListenableFuture<Void> saveFuture = saveToDatabase(ctx, msg, resultBd);
                Futures.addCallback(saveFuture, new com.google.common.util.concurrent.FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        ctx.tellSuccess(updatedMsg);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ctx.tellFailure(msg, new RuntimeException("Failed to save decline analysis result", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                ctx.tellSuccess(updatedMsg);
            }

        } catch (Exception e) {
            log.error("Error in decline analysis: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private double getBExponent(ObjectNode body) {
        switch (config.getDeclineType()) {
            case EXPONENTIAL:
                return 0.0;
            case HARMONIC:
                return 1.0;
            case HYPERBOLIC:
                // Try to read from message, fall back to default
                if (config.getBExponentField() != null) {
                    JsonNode node = body.get(config.getBExponentField());
                    if (node != null && node.isNumber()) {
                        double b = node.asDouble();
                        if (b <= 0 || b >= 1) {
                            log.warn("Hyperbolic b={} outside range (0,1), using default", b);
                            return config.getDefaultBExponent();
                        }
                        return b;
                    }
                }
                return config.getDefaultBExponent();
            default:
                return 0.5;
        }
    }

    private double calculate(double qi, double di, double b, double t) {
        switch (config.getCalculationType()) {
            case RATE_AT_TIME:
                return calculateRateAtTime(qi, di, b, t);
            case CUMULATIVE_AT_TIME:
                return calculateCumulativeAtTime(qi, di, b, t);
            case TIME_TO_RATE:
                // In this case, 't' field is used as target rate
                return calculateTimeToRate(qi, di, b, t);
            case EUR:
                // In this case, 't' field is used as economic limit rate
                return calculateEUR(qi, di, b, t);
            default:
                throw new RuntimeException("Unknown calculation type: " + config.getCalculationType());
        }
    }

    /**
     * Calculate rate at time t.
     */
    private double calculateRateAtTime(double qi, double di, double b, double t) {
        if (b == 0) {
            // Exponential: q(t) = qi * e^(-Di*t)
            return qi * Math.exp(-di * t);
        } else if (b == 1) {
            // Harmonic: q(t) = qi / (1 + Di*t)
            return qi / (1 + di * t);
        } else {
            // Hyperbolic: q(t) = qi / (1 + b*Di*t)^(1/b)
            return qi / Math.pow(1 + b * di * t, 1 / b);
        }
    }

    /**
     * Calculate cumulative production at time t.
     */
    private double calculateCumulativeAtTime(double qi, double di, double b, double t) {
        if (b == 0) {
            // Exponential: Np = (qi/Di) * (1 - e^(-Di*t))
            return (qi / di) * (1 - Math.exp(-di * t));
        } else if (b == 1) {
            // Harmonic: Np = (qi/Di) * ln(1 + Di*t)
            return (qi / di) * Math.log(1 + di * t);
        } else {
            // Hyperbolic: Np = qi / ((1-b)*Di) * (1 - (q/qi)^(1-b))
            double qt = calculateRateAtTime(qi, di, b, t);
            return (qi / ((1 - b) * di)) * (1 - Math.pow(qt / qi, 1 - b));
        }
    }

    /**
     * Calculate time to reach target rate.
     */
    private double calculateTimeToRate(double qi, double di, double b, double targetRate) {
        if (targetRate <= 0 || targetRate >= qi) {
            throw new RuntimeException("Target rate must be between 0 and initial rate");
        }

        if (b == 0) {
            // Exponential: t = -ln(q/qi) / Di
            return -Math.log(targetRate / qi) / di;
        } else if (b == 1) {
            // Harmonic: t = (qi/q - 1) / Di
            return (qi / targetRate - 1) / di;
        } else {
            // Hyperbolic: t = ((qi/q)^b - 1) / (b*Di)
            return (Math.pow(qi / targetRate, b) - 1) / (b * di);
        }
    }

    /**
     * Calculate Estimated Ultimate Recovery (EUR) to economic limit.
     */
    private double calculateEUR(double qi, double di, double b, double economicLimit) {
        double tLimit = calculateTimeToRate(qi, di, b, economicLimit);
        return calculateCumulativeAtTime(qi, di, b, tLimit);
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
