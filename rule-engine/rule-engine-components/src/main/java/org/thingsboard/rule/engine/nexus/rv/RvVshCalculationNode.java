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
 * Rule Engine node for calculating Shale Volume (Vsh) from Gamma Ray logs.
 *
 * Supports multiple methods:
 * - LINEAR: Vsh = IGR
 * - LARIONOV_TERTIARY: Vsh = 0.083 * (2^(3.7*IGR) - 1)
 * - LARIONOV_OLDER: Vsh = 0.33 * (2^(2*IGR) - 1)
 * - STEIBER: Vsh = IGR / (3 - 2*IGR)
 * - CLAVIER: Vsh = 1.7 - sqrt(3.38 - (IGR + 0.7)^2)
 *
 * Where IGR = (GRlog - GRclean) / (GRshale - GRclean)
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rv vsh calculation",
        configClazz = RvVshCalculationNodeConfiguration.class,
        nodeDescription = "Calculate Shale Volume (Vsh) from Gamma Ray logs",
        nodeDetails = "Calculates shale volume using various methods including Larionov (Tertiary/Older), " +
                "Steiber, and Clavier equations.<br/><br/>" +
                "First calculates the Gamma Ray Index (IGR) = (GRlog - GRclean) / (GRshale - GRclean), " +
                "then applies the selected non-linear correction.<br/><br/>" +
                "Larionov Tertiary is recommended for Venezuelan formations.",
        configDirective = "tbActionNodeRvVshConfig",
        icon = "terrain",
        docUrl = "https://nexus.thingsboard.io/docs/rv-module/vsh-calculation"
)
public class RvVshCalculationNode implements TbNode {

    private RvVshCalculationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, RvVshCalculationNodeConfiguration.class);
        log.info("RvVshCalculationNode initialized with method: {}", config.getMethod());
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
            double grLog = extractDouble(body, config.getGrLogField(), "GR Log");
            double grClean = extractDouble(body, config.getGrCleanField(), "GR Clean");
            double grShale = extractDouble(body, config.getGrShaleField(), "GR Shale");

            // Validate inputs
            if (grShale <= grClean) {
                throw new RuntimeException("GR Shale must be greater than GR Clean");
            }

            // Calculate IGR (Gamma Ray Index)
            double igr = (grLog - grClean) / (grShale - grClean);
            igr = Math.max(0.0, Math.min(1.0, igr)); // Clamp to [0, 1]

            // Calculate Vsh based on selected method
            double vsh = calculateVsh(igr, config.getMethod());

            log.debug("Vsh calculated: {} using method {} for entity {}",
                    vsh, config.getMethod(), msg.getOriginator());

            // Round result
            BigDecimal result = BigDecimal.valueOf(vsh)
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
                        ctx.tellFailure(msg, new RuntimeException("Failed to save Vsh result", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                ctx.tellSuccess(updatedMsg);
            }

        } catch (Exception e) {
            log.error("Error calculating Vsh: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private double calculateVsh(double igr, RvVshCalculationNodeConfiguration.VshMethod method) {
        double vsh;
        switch (method) {
            case LINEAR:
                vsh = igr;
                break;
            case LARIONOV_TERTIARY:
                // Vsh = 0.083 * (2^(3.7*IGR) - 1)
                vsh = 0.083 * (Math.pow(2, 3.7 * igr) - 1);
                break;
            case LARIONOV_OLDER:
                // Vsh = 0.33 * (2^(2*IGR) - 1)
                vsh = 0.33 * (Math.pow(2, 2 * igr) - 1);
                break;
            case STEIBER:
                // Vsh = IGR / (3 - 2*IGR)
                vsh = igr / (3 - 2 * igr);
                break;
            case CLAVIER:
                // Vsh = 1.7 - sqrt(3.38 - (IGR + 0.7)^2)
                double term = 3.38 - Math.pow(igr + 0.7, 2);
                vsh = (term > 0) ? 1.7 - Math.sqrt(term) : 1.0;
                break;
            default:
                throw new RuntimeException("Unknown Vsh method: " + method);
        }

        // Clamp result to [0, 1]
        return Math.max(0.0, Math.min(1.0, vsh));
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
