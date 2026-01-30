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
 * Rule Engine node for calculating Mechanical Specific Energy (MSE).
 *
 * Formula: MSE = (480 * T * N) / (D² * ROP) + (4 * WOB) / (π * D²)
 *
 * Where:
 * - WOB = Weight on Bit (klbs)
 * - N = RPM (rotations per minute)
 * - T = Torque (ft-lbs)
 * - D = Bit Diameter (inches)
 * - ROP = Rate of Penetration (ft/hr)
 *
 * Result: MSE in psi
 *
 * Output routes:
 * - "Success" for normal calculation
 * - "High MSE" when MSE exceeds configured threshold (indicates drilling inefficiency)
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "dr mse calculation",
        configClazz = DrMseCalculationNodeConfiguration.class,
        nodeDescription = "Calculate Mechanical Specific Energy (MSE) for drilling optimization",
        nodeDetails = "Calculates MSE using the formula: MSE = (480 * T * N) / (D² * ROP) + (4 * WOB) / (π * D²).<br/><br/>" +
                "MSE represents the energy required to destroy a unit volume of rock. Lower MSE indicates better drilling efficiency.<br/>" +
                "Typical MSE values: 20,000-40,000 psi for efficient drilling, >80,000 psi indicates problems.<br/><br/>" +
                "Routes message to 'High MSE' when threshold exceeded for alerting.",
        configDirective = "tbActionNodeDrMseConfig",
        icon = "speed",
        docUrl = "https://nexus.thingsboard.io/docs/dr-module/mse-calculation",
        relationTypes = {"Success", "High MSE", "Failure"}
)
public class DrMseCalculationNode implements TbNode {

    private static final BigDecimal CONSTANT_480 = new BigDecimal("480");
    private static final BigDecimal CONSTANT_4 = new BigDecimal("4");
    private static final BigDecimal PI = new BigDecimal("3.14159265359");
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private DrMseCalculationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, DrMseCalculationNodeConfiguration.class);
        log.info("DrMseCalculationNode initialized with config: {}", config);
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
            BigDecimal wobKlbs = extractBigDecimal(body, config.getWobKlbsField(), "WOB");
            BigDecimal rpm = extractBigDecimal(body, config.getRpmField(), "RPM");
            BigDecimal torqueFtLbs = extractBigDecimal(body, config.getTorqueFtLbsField(), "Torque");
            BigDecimal ropFtHr = extractBigDecimal(body, config.getRopFtHrField(), "ROP");
            BigDecimal bitDiameterIn = extractBigDecimal(body, config.getBitDiameterInField(), "Bit Diameter");

            // Validate inputs
            validatePositive("WOB", wobKlbs);
            validatePositive("RPM", rpm);
            validatePositive("Torque", torqueFtLbs);
            validatePositive("ROP", ropFtHr);
            validatePositive("Bit Diameter", bitDiameterIn);

            // Calculate MSE
            BigDecimal mse = calculateMse(wobKlbs, rpm, torqueFtLbs, ropFtHr, bitDiameterIn);

            log.debug("MSE calculated: {} psi for entity {}", mse, msg.getOriginator());

            // Round result
            BigDecimal result = mse.setScale(config.getResultPrecision(), RoundingMode.HALF_UP);

            // Update message
            TbMsg updatedMsg = updateMessage(msg, body, result);

            // Determine if MSE is high (drilling inefficiency)
            boolean isHighMse = config.isEnableMseAlert() &&
                    result.doubleValue() > config.getMseThresholdPsi();

            // Save and route
            if (config.isSaveAsAttribute() || config.isSaveAsTelemetry()) {
                ListenableFuture<Void> saveFuture = saveToDatabase(ctx, msg, result);
                Futures.addCallback(saveFuture, new com.google.common.util.concurrent.FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        routeMessage(ctx, updatedMsg, isHighMse);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ctx.tellFailure(msg, new RuntimeException("Failed to save MSE result", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                routeMessage(ctx, updatedMsg, isHighMse);
            }

        } catch (Exception e) {
            log.error("Error calculating MSE: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private void routeMessage(TbContext ctx, TbMsg msg, boolean isHighMse) {
        if (isHighMse) {
            ctx.tellNext(msg, "High MSE");
        } else {
            ctx.tellSuccess(msg);
        }
    }

    /**
     * Calculate MSE using Teale's equation.
     * MSE = (480 * T * N) / (D² * ROP) + (4 * WOB) / (π * D²)
     */
    private BigDecimal calculateMse(BigDecimal wobKlbs, BigDecimal rpm, BigDecimal torqueFtLbs,
                                     BigDecimal ropFtHr, BigDecimal bitDiameterIn) {
        // Convert WOB from klbs to lbs
        BigDecimal wobLbs = wobKlbs.multiply(new BigDecimal("1000"), MC);

        // Calculate bit area: π * D² / 4 -> D² = (D² * π / 4) -> area
        BigDecimal dSquared = bitDiameterIn.multiply(bitDiameterIn, MC);

        // Rotary component: (480 * T * N) / (D² * ROP)
        BigDecimal rotaryNumerator = CONSTANT_480.multiply(torqueFtLbs, MC).multiply(rpm, MC);
        BigDecimal rotaryDenominator = dSquared.multiply(ropFtHr, MC);
        BigDecimal rotaryComponent = rotaryNumerator.divide(rotaryDenominator, MC);

        // WOB component: (4 * WOB) / (π * D²)
        BigDecimal wobNumerator = CONSTANT_4.multiply(wobLbs, MC);
        BigDecimal wobDenominator = PI.multiply(dSquared, MC);
        BigDecimal wobComponent = wobNumerator.divide(wobDenominator, MC);

        return rotaryComponent.add(wobComponent, MC);
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
}
