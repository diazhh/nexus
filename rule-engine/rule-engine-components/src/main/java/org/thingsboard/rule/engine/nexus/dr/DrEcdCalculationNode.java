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
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Rule Engine node for calculating Equivalent Circulating Density (ECD).
 *
 * Formula: ECD = MW + (APL / (0.052 * TVD))
 *
 * Where:
 * - MW = Mud Weight (ppg)
 * - APL = Annular Pressure Loss (psi)
 * - TVD = True Vertical Depth (ft)
 *
 * Result: ECD in ppg
 *
 * Output routes:
 * - "Success" for normal ECD
 * - "High ECD" when ECD exceeds high threshold (risk of fracturing formation)
 * - "Low ECD" when ECD below low threshold (risk of kick)
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "dr ecd calculation",
        configClazz = DrEcdCalculationNodeConfiguration.class,
        nodeDescription = "Calculate Equivalent Circulating Density (ECD) for wellbore pressure management",
        nodeDetails = "Calculates ECD using the formula: ECD = MW + (APL / (0.052 * TVD)).<br/><br/>" +
                "ECD is critical for maintaining wellbore stability. It must stay within the drilling window:<br/>" +
                "- Above pore pressure (to prevent kicks)<br/>" +
                "- Below fracture pressure (to prevent lost circulation)<br/><br/>" +
                "Routes message to 'High ECD' or 'Low ECD' for threshold violations.",
        configDirective = "tbActionNodeDrEcdConfig",
        icon = "compress",
        docUrl = "https://nexus.thingsboard.io/docs/dr-module/ecd-calculation",
        relationTypes = {"Success", "High ECD", "Low ECD", "Failure"}
)
public class DrEcdCalculationNode implements TbNode {

    private static final BigDecimal CONSTANT_0052 = new BigDecimal("0.052");
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private DrEcdCalculationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, DrEcdCalculationNodeConfiguration.class);
        log.info("DrEcdCalculationNode initialized with config: {}", config);
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
            BigDecimal mudWeightPpg = extractBigDecimal(body, config.getMudWeightPpgField(), "Mud Weight");
            BigDecimal annularPressureLossPsi = extractBigDecimal(body, config.getAnnularPressureLossPsiField(), "Annular Pressure Loss");
            BigDecimal tvdFt = extractBigDecimal(body, config.getTvdFtField(), "TVD");

            // Validate inputs
            validatePositive("Mud Weight", mudWeightPpg);
            validateNonNegative("Annular Pressure Loss", annularPressureLossPsi);
            validatePositive("TVD", tvdFt);

            // Calculate ECD
            BigDecimal ecd = calculateEcd(mudWeightPpg, annularPressureLossPsi, tvdFt);

            log.debug("ECD calculated: {} ppg for entity {}", ecd, msg.getOriginator());

            // Round result
            BigDecimal result = ecd.setScale(config.getResultPrecision(), RoundingMode.HALF_UP);

            // Update message
            TbMsg updatedMsg = updateMessage(msg, body, result);

            // Determine routing
            String routeType = determineRoute(result.doubleValue());

            // Save and route
            if (config.isSaveAsAttribute() || config.isSaveAsTelemetry()) {
                ListenableFuture<Void> saveFuture = saveToDatabase(ctx, msg, result);
                String finalRouteType = routeType;
                Futures.addCallback(saveFuture, new com.google.common.util.concurrent.FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        routeMessage(ctx, updatedMsg, finalRouteType);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ctx.tellFailure(msg, new RuntimeException("Failed to save ECD result", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                routeMessage(ctx, updatedMsg, routeType);
            }

        } catch (Exception e) {
            log.error("Error calculating ECD: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private String determineRoute(double ecdValue) {
        if (config.isEnableEcdHighAlert() && ecdValue > config.getEcdHighThresholdPpg()) {
            return "High ECD";
        }
        if (config.isEnableEcdLowAlert() && ecdValue < config.getEcdLowThresholdPpg()) {
            return "Low ECD";
        }
        return "Success";
    }

    private void routeMessage(TbContext ctx, TbMsg msg, String routeType) {
        switch (routeType) {
            case "High ECD":
                ctx.tellNext(msg, "High ECD");
                break;
            case "Low ECD":
                ctx.tellNext(msg, "Low ECD");
                break;
            default:
                ctx.tellSuccess(msg);
        }
    }

    /**
     * Calculate ECD.
     * ECD = MW + (APL / (0.052 * TVD))
     */
    private BigDecimal calculateEcd(BigDecimal mudWeightPpg, BigDecimal annularPressureLossPsi, BigDecimal tvdFt) {
        // Pressure gradient constant: 0.052 psi/ft/ppg
        BigDecimal denominator = CONSTANT_0052.multiply(tvdFt, MC);
        BigDecimal pressureComponent = annularPressureLossPsi.divide(denominator, MC);
        return mudWeightPpg.add(pressureComponent, MC);
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

    private void validatePositive(String name, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(String.format("%s must be positive, got %s", name, value));
        }
    }

    private void validateNonNegative(String name, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(String.format("%s must be non-negative, got %s", name, value));
        }
    }
}
