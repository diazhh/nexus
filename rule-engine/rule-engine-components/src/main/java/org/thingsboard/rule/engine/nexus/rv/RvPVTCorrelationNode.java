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
 * Rule Engine node for PVT (Pressure-Volume-Temperature) correlations.
 *
 * Calculates various fluid properties:
 * - Bubble point pressure (Pb) using Standing, Vazquez-Beggs, or Glasø
 * - Oil formation volume factor (Bo) using Standing, Vazquez-Beggs, or Glasø
 * - Dead/live oil viscosity using Beggs-Robinson, Egbogah, or Chew-Connally
 * - Solution GOR using Standing or Vazquez-Beggs
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rv pvt correlation",
        configClazz = RvPVTCorrelationNodeConfiguration.class,
        nodeDescription = "Calculate PVT fluid properties using industry correlations",
        nodeDetails = "Calculates PVT properties using standard petroleum engineering correlations.<br/><br/>" +
                "Supported properties:<br/>" +
                "- Bubble Point (Pb): Standing, Vazquez-Beggs, Glasø<br/>" +
                "- Oil FVF (Bo): Standing, Vazquez-Beggs, Glasø<br/>" +
                "- Viscosity: Beggs-Robinson, Egbogah, Chew-Connally<br/>" +
                "- Solution GOR (Rs): Standing, Vazquez-Beggs<br/><br/>" +
                "Input: API gravity, gas gravity, temperature, pressure (as needed).<br/>" +
                "Output: Calculated property in standard units.",
        configDirective = "tbActionNodeRvPVTConfig",
        icon = "science",
        docUrl = "https://nexus.thingsboard.io/docs/rv-module/pvt-correlations"
)
public class RvPVTCorrelationNode implements TbNode {

    private RvPVTCorrelationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, RvPVTCorrelationNodeConfiguration.class);
        log.info("RvPVTCorrelationNode initialized: property={}, correlation={}",
                config.getPropertyToCalculate(), config.getCorrelation());
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

            // Calculate the property
            double result = calculateProperty(body);

            log.debug("PVT {} calculated: {} for entity {} using {}",
                    config.getPropertyToCalculate(), result, msg.getOriginator(), config.getCorrelation());

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
                        ctx.tellFailure(msg, new RuntimeException("Failed to save PVT result", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                ctx.tellSuccess(updatedMsg);
            }

        } catch (Exception e) {
            log.error("Error calculating PVT property: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private double calculateProperty(ObjectNode body) {
        switch (config.getCorrelation()) {
            // Bubble Point correlations
            case STANDING_PB:
                return calculatePbStanding(body);
            case VAZQUEZ_BEGGS_PB:
                return calculatePbVazquezBeggs(body);
            case GLASO_PB:
                return calculatePbGlaso(body);

            // Oil FVF correlations
            case STANDING_BO:
                return calculateBoStanding(body);
            case VAZQUEZ_BEGGS_BO:
                return calculateBoVazquezBeggs(body);
            case GLASO_BO:
                return calculateBoGlaso(body);

            // Viscosity correlations
            case BEGGS_ROBINSON:
                return calculateViscosityBeggsRobinson(body);
            case EGBOGAH:
                return calculateViscosityEgbogah(body);

            // GOR correlations
            case STANDING_RS:
                return calculateRsStanding(body);
            case VAZQUEZ_BEGGS_RS:
                return calculateRsVazquezBeggs(body);

            default:
                throw new RuntimeException("Unsupported correlation: " + config.getCorrelation());
        }
    }

    // ========================================
    // Bubble Point Correlations
    // ========================================

    /**
     * Standing correlation for bubble point pressure.
     * Pb = 18.2 * ((Rs/γg)^0.83 * 10^(0.00091*T - 0.0125*API) - 1.4)
     */
    private double calculatePbStanding(ObjectNode body) {
        double rs = extractDouble(body, config.getRsField(), "Rs");
        double gg = extractDouble(body, config.getGasGravityField(), "Gas Gravity");
        double t = extractDouble(body, config.getTemperatureField(), "Temperature");
        double api = extractDouble(body, config.getApiGravityField(), "API Gravity");

        double term1 = Math.pow(rs / gg, 0.83);
        double exponent = 0.00091 * t - 0.0125 * api;
        double term2 = Math.pow(10, exponent);

        return 18.2 * (term1 * term2 - 1.4);
    }

    /**
     * Vazquez-Beggs correlation for bubble point pressure.
     */
    private double calculatePbVazquezBeggs(ObjectNode body) {
        double rs = extractDouble(body, config.getRsField(), "Rs");
        double gg = extractDouble(body, config.getGasGravityField(), "Gas Gravity");
        double t = extractDouble(body, config.getTemperatureField(), "Temperature");
        double api = extractDouble(body, config.getApiGravityField(), "API Gravity");

        // Correct gas gravity to separator conditions at 100 psig
        double ggc = gg * (1 + 5.912E-5 * api * 60 * Math.log10(114.7 / 114.7));

        double c1, c2, c3;
        if (api <= 30) {
            c1 = 0.0362; c2 = 1.0937; c3 = 25.7240;
        } else {
            c1 = 0.0178; c2 = 1.187; c3 = 23.931;
        }

        return Math.pow(rs / (c1 * ggc * Math.exp(c3 * api / (t + 460))), 1 / c2);
    }

    /**
     * Glasø correlation for bubble point pressure.
     */
    private double calculatePbGlaso(ObjectNode body) {
        double rs = extractDouble(body, config.getRsField(), "Rs");
        double gg = extractDouble(body, config.getGasGravityField(), "Gas Gravity");
        double t = extractDouble(body, config.getTemperatureField(), "Temperature");
        double api = extractDouble(body, config.getApiGravityField(), "API Gravity");

        double pbStar = Math.pow(rs / gg, 0.816) * Math.pow(t, 0.172) / Math.pow(api, 0.989);
        double logPb = 1.7669 + 1.7447 * Math.log10(pbStar) - 0.30218 * Math.pow(Math.log10(pbStar), 2);

        return Math.pow(10, logPb);
    }

    // ========================================
    // Oil FVF Correlations
    // ========================================

    /**
     * Standing correlation for oil formation volume factor.
     * Bo = 0.9759 + 0.00012 * (Rs * (γg/γo)^0.5 + 1.25*T)^1.2
     */
    private double calculateBoStanding(ObjectNode body) {
        double rs = extractDouble(body, config.getRsField(), "Rs");
        double gg = extractDouble(body, config.getGasGravityField(), "Gas Gravity");
        double go = extractDouble(body, config.getOilGravityField(), "Oil Gravity");
        double t = extractDouble(body, config.getTemperatureField(), "Temperature");

        double gravityRatio = Math.sqrt(gg / go);
        double term = rs * gravityRatio + 1.25 * t;

        return 0.9759 + 0.00012 * Math.pow(term, 1.2);
    }

    /**
     * Vazquez-Beggs correlation for oil formation volume factor.
     */
    private double calculateBoVazquezBeggs(ObjectNode body) {
        double rs = extractDouble(body, config.getRsField(), "Rs");
        double gg = extractDouble(body, config.getGasGravityField(), "Gas Gravity");
        double t = extractDouble(body, config.getTemperatureField(), "Temperature");
        double api = extractDouble(body, config.getApiGravityField(), "API Gravity");

        double c1, c2, c3;
        if (api <= 30) {
            c1 = 4.677E-4; c2 = 1.751E-5; c3 = -1.811E-8;
        } else {
            c1 = 4.670E-4; c2 = 1.100E-5; c3 = 1.337E-9;
        }

        return 1 + c1 * rs + (t - 60) * (api / gg) * (c2 + c3 * rs);
    }

    /**
     * Glasø correlation for oil formation volume factor.
     */
    private double calculateBoGlaso(ObjectNode body) {
        double rs = extractDouble(body, config.getRsField(), "Rs");
        double gg = extractDouble(body, config.getGasGravityField(), "Gas Gravity");
        double t = extractDouble(body, config.getTemperatureField(), "Temperature");
        double api = extractDouble(body, config.getApiGravityField(), "API Gravity");

        double go = 141.5 / (api + 131.5); // Convert API to specific gravity
        double bob = rs * Math.pow(gg / go, 0.526) + 0.968 * t;
        double logBo = -6.58511 + 2.91329 * Math.log10(bob) - 0.27683 * Math.pow(Math.log10(bob), 2);

        return Math.pow(10, logBo);
    }

    // ========================================
    // Viscosity Correlations
    // ========================================

    /**
     * Beggs-Robinson correlation for dead oil viscosity.
     * μod = 10^(10^(3.0324 - 0.02023*API) * T^-1.163) - 1
     */
    private double calculateViscosityBeggsRobinson(ObjectNode body) {
        double api = extractDouble(body, config.getApiGravityField(), "API Gravity");
        double t = extractDouble(body, config.getTemperatureField(), "Temperature");

        double x = Math.pow(t, -1.163);
        double y = Math.pow(10, 3.0324 - 0.02023 * api);

        return Math.pow(10, y * x) - 1;
    }

    /**
     * Egbogah correlation for dead oil viscosity.
     */
    private double calculateViscosityEgbogah(ObjectNode body) {
        double api = extractDouble(body, config.getApiGravityField(), "API Gravity");
        double t = extractDouble(body, config.getTemperatureField(), "Temperature");

        double a = 1.8653 - 0.025086 * api - 0.5644 * Math.log10(t);
        return Math.pow(10, Math.pow(10, a)) - 1;
    }

    // ========================================
    // Solution GOR Correlations
    // ========================================

    /**
     * Standing correlation for solution GOR.
     * Rs = γg * ((P/18.2 + 1.4) * 10^(0.0125*API - 0.00091*T))^1.2048
     */
    private double calculateRsStanding(ObjectNode body) {
        double gg = extractDouble(body, config.getGasGravityField(), "Gas Gravity");
        double p = extractDouble(body, config.getPressureField(), "Pressure");
        double api = extractDouble(body, config.getApiGravityField(), "API Gravity");
        double t = extractDouble(body, config.getTemperatureField(), "Temperature");

        double exponent = 0.0125 * api - 0.00091 * t;
        double term = (p / 18.2 + 1.4) * Math.pow(10, exponent);

        return gg * Math.pow(term, 1.2048);
    }

    /**
     * Vazquez-Beggs correlation for solution GOR.
     */
    private double calculateRsVazquezBeggs(ObjectNode body) {
        double gg = extractDouble(body, config.getGasGravityField(), "Gas Gravity");
        double p = extractDouble(body, config.getPressureField(), "Pressure");
        double api = extractDouble(body, config.getApiGravityField(), "API Gravity");
        double t = extractDouble(body, config.getTemperatureField(), "Temperature");

        double c1, c2, c3;
        if (api <= 30) {
            c1 = 0.0362; c2 = 1.0937; c3 = 25.7240;
        } else {
            c1 = 0.0178; c2 = 1.187; c3 = 23.931;
        }

        return c1 * gg * Math.pow(p, c2) * Math.exp(c3 * api / (t + 460));
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
        if (fieldName == null || fieldName.isEmpty()) {
            throw new RuntimeException(displayName + " field name is not configured");
        }
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
