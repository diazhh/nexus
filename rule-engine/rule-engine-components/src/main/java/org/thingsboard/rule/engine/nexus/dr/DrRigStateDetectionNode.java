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
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule Engine node for detecting rig state from real-time drilling data.
 *
 * Detects the following rig states:
 * - DRILLING_ROTATING: On bottom, rotating, with WOB
 * - DRILLING_SLIDING: On bottom, not rotating, with WOB (directional drilling)
 * - CIRCULATING: Pumps on, not drilling
 * - REAMING: Rotating while tripping
 * - BACK_REAMING: Rotating while tripping out
 * - TRIPPING_IN: Running pipe in hole
 * - TRIPPING_OUT: Pulling pipe out of hole
 * - CONNECTION: Making/breaking connection
 * - IN_SLIPS: Pipe stationary in slips
 * - STATIC: No activity (idle)
 *
 * Routes message to the detected state for conditional processing.
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "dr rig state detection",
        configClazz = DrRigStateDetectionNodeConfiguration.class,
        nodeDescription = "Detect rig activity state from real-time drilling parameters",
        nodeDetails = "Analyzes drilling parameters to determine current rig state:<br/>" +
                "- DRILLING_ROTATING: Rotary drilling<br/>" +
                "- DRILLING_SLIDING: Slide drilling (directional)<br/>" +
                "- CIRCULATING: Circulating without drilling<br/>" +
                "- REAMING/BACK_REAMING: Conditioning hole<br/>" +
                "- TRIPPING_IN/OUT: Pipe movement<br/>" +
                "- CONNECTION: Making connection<br/>" +
                "- IN_SLIPS/STATIC: Stationary<br/><br/>" +
                "Routes message to detected state name for activity-based processing.",
        configDirective = "tbActionNodeDrRigStateConfig",
        icon = "build",
        docUrl = "https://nexus.thingsboard.io/docs/dr-module/rig-state-detection",
        relationTypes = {"DRILLING_ROTATING", "DRILLING_SLIDING", "CIRCULATING", "REAMING",
                "BACK_REAMING", "TRIPPING_IN", "TRIPPING_OUT", "CONNECTION", "IN_SLIPS", "STATIC", "Failure"}
)
public class DrRigStateDetectionNode implements TbNode {

    private DrRigStateDetectionNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, DrRigStateDetectionNodeConfiguration.class);
        log.info("DrRigStateDetectionNode initialized with config: {}", config);
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

            // Extract drilling parameters
            DrillingParameters params = extractParameters(body);

            // Detect rig state
            RigState state = detectRigState(params);

            log.debug("Rig state detected: {} for entity {}", state, msg.getOriginator());

            // Update message
            TbMsg updatedMsg = updateMessage(msg, body, state);

            // Save and route
            if (config.isSaveAsAttribute() || config.isSaveAsTelemetry()) {
                ListenableFuture<Void> saveFuture = saveToDatabase(ctx, msg, state);
                Futures.addCallback(saveFuture, new com.google.common.util.concurrent.FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        ctx.tellNext(updatedMsg, state.name());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ctx.tellFailure(msg, new RuntimeException("Failed to save rig state", t));
                    }
                }, ctx.getDbCallbackExecutor());
            } else {
                ctx.tellNext(updatedMsg, state.name());
            }

        } catch (Exception e) {
            log.error("Error detecting rig state: {}", e.getMessage(), e);
            ctx.tellFailure(msg, e);
        }
    }

    private DrillingParameters extractParameters(ObjectNode body) {
        return new DrillingParameters(
                getOptionalDouble(body, config.getBitDepthFtField()),
                getOptionalDouble(body, config.getHoleDepthFtField()),
                getOptionalDouble(body, config.getBlockPositionFtField()),
                getOptionalDouble(body, config.getHookLoadKlbsField()),
                getOptionalDouble(body, config.getWobKlbsField()),
                getOptionalDouble(body, config.getRpmField()),
                getOptionalDouble(body, config.getSppPsiField()),
                getOptionalDouble(body, config.getFlowRateGpmField()),
                getOptionalDouble(body, config.getTorqueFtLbsField()),
                getOptionalDouble(body, config.getRopFtHrField())
        );
    }

    private RigState detectRigState(DrillingParameters p) {
        // Check if we have minimum required data
        if (p.bitDepth == null && p.holeDepth == null) {
            return RigState.STATIC;
        }

        boolean isOnBottom = isOnBottom(p);
        boolean isRotating = isRotating(p);
        boolean isCirculating = isCirculating(p);
        boolean hasWob = hasWeightOnBit(p);
        boolean pipeMovingDown = isPipeMovingDown(p);
        boolean pipeMovingUp = isPipeMovingUp(p);
        boolean inSlips = isInSlips(p);

        // Decision tree for rig state
        if (isOnBottom) {
            if (isRotating && hasWob) {
                return RigState.DRILLING_ROTATING;
            }
            if (!isRotating && hasWob && isCirculating) {
                return RigState.DRILLING_SLIDING;
            }
            if (isCirculating && !hasWob) {
                return RigState.CIRCULATING;
            }
        }

        // Not on bottom
        if (pipeMovingDown) {
            if (isRotating) {
                return RigState.REAMING;
            }
            return RigState.TRIPPING_IN;
        }

        if (pipeMovingUp) {
            if (isRotating) {
                return RigState.BACK_REAMING;
            }
            return RigState.TRIPPING_OUT;
        }

        // Pipe stationary
        if (inSlips) {
            if (isCirculating) {
                return RigState.CONNECTION;
            }
            return RigState.IN_SLIPS;
        }

        // Circulating off bottom
        if (isCirculating && !isOnBottom) {
            return RigState.CIRCULATING;
        }

        return RigState.STATIC;
    }

    private boolean isOnBottom(DrillingParameters p) {
        if (p.bitDepth != null && p.holeDepth != null) {
            return (p.holeDepth - p.bitDepth) <= config.getOnBottomThresholdFt();
        }
        return false;
    }

    private boolean isRotating(DrillingParameters p) {
        if (p.rpm != null) {
            return p.rpm >= config.getMinRpmForRotating();
        }
        return false;
    }

    private boolean isCirculating(DrillingParameters p) {
        if (p.flowRate != null) {
            return p.flowRate >= config.getMinFlowForCirculating();
        }
        if (p.spp != null) {
            return p.spp > 100; // Some pressure indicates pumps on
        }
        return false;
    }

    private boolean hasWeightOnBit(DrillingParameters p) {
        if (p.wob != null) {
            return p.wob >= config.getMinWobForDrilling();
        }
        return false;
    }

    private boolean isPipeMovingDown(DrillingParameters p) {
        // Would typically compare current vs previous block position
        // For now, use ROP as proxy
        if (p.rop != null && p.rop > 0) {
            return true;
        }
        return false;
    }

    private boolean isPipeMovingUp(DrillingParameters p) {
        // Would typically compare current vs previous block position
        // Negative ROP would indicate pulling out
        if (p.rop != null && p.rop < 0) {
            return true;
        }
        return false;
    }

    private boolean isInSlips(DrillingParameters p) {
        if (p.blockPosition != null) {
            return p.blockPosition <= config.getSlipPositionThresholdFt();
        }
        return false;
    }

    private TbMsg updateMessage(TbMsg msg, ObjectNode body, RigState state) {
        body.put(config.getOutputRigStateField(), state.name());
        body.put(config.getOutputRigStateCodeField(), state.getCode());
        body.put(config.getOutputActivityField(), state.getActivity());

        if (config.isTrackStateChanges()) {
            body.put(config.getStateChangeTimeField(), System.currentTimeMillis());
        }

        TbMsg.TbMsgBuilder builder = msg.transform()
                .data(JacksonUtil.toString(body));

        if (config.isAddToMetadata()) {
            var md = msg.getMetaData();
            md.putValue(config.getOutputRigStateField(), state.name());
            md.putValue(config.getOutputRigStateCodeField(), String.valueOf(state.getCode()));
            builder.metaData(md);
        }

        return builder.build();
    }

    private ListenableFuture<Void> saveToDatabase(TbContext ctx, TbMsg msg, RigState state) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();

        if (config.isSaveAsAttribute()) {
            futures.add(saveAsAttribute(ctx, msg, state));
        }
        if (config.isSaveAsTelemetry()) {
            futures.add(saveAsTelemetry(ctx, msg, state));
        }

        return Futures.transform(Futures.allAsList(futures), list -> null, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Void> saveAsAttribute(TbContext ctx, TbMsg msg, RigState state) {
        AttributeScope scope = AttributeScope.valueOf(config.getAttributeScope());

        SettableFuture<Void> future = SettableFuture.create();
        ctx.getTelemetryService().saveAttributes(AttributesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .scope(scope)
                .entry(new StringDataEntry(config.getOutputRigStateField(), state.name()))
                .future(future)
                .build());
        return future;
    }

    private ListenableFuture<Void> saveAsTelemetry(TbContext ctx, TbMsg msg, RigState state) {
        long ts = System.currentTimeMillis();

        SettableFuture<Void> future = SettableFuture.create();
        ctx.getTelemetryService().saveTimeseries(TimeseriesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .entry(new BasicTsKvEntry(ts, new StringDataEntry(config.getOutputRigStateField(), state.name())))
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

    private enum RigState {
        DRILLING_ROTATING(1, "Drilling"),
        DRILLING_SLIDING(2, "Drilling"),
        CIRCULATING(3, "Circulating"),
        REAMING(4, "Reaming"),
        BACK_REAMING(5, "Reaming"),
        TRIPPING_IN(6, "Tripping"),
        TRIPPING_OUT(7, "Tripping"),
        CONNECTION(8, "Connection"),
        IN_SLIPS(9, "In Slips"),
        STATIC(0, "Static");

        private final int code;
        private final String activity;

        RigState(int code, String activity) {
            this.code = code;
            this.activity = activity;
        }

        public int getCode() {
            return code;
        }

        public String getActivity() {
            return activity;
        }
    }

    private static class DrillingParameters {
        final Double bitDepth;
        final Double holeDepth;
        final Double blockPosition;
        final Double hookLoad;
        final Double wob;
        final Double rpm;
        final Double spp;
        final Double flowRate;
        final Double torque;
        final Double rop;

        DrillingParameters(Double bitDepth, Double holeDepth, Double blockPosition, Double hookLoad,
                           Double wob, Double rpm, Double spp, Double flowRate, Double torque, Double rop) {
            this.bitDepth = bitDepth;
            this.holeDepth = holeDepth;
            this.blockPosition = blockPosition;
            this.hookLoad = hookLoad;
            this.wob = wob;
            this.rpm = rpm;
            this.spp = spp;
            this.flowRate = flowRate;
            this.torque = torque;
            this.rop = rop;
        }
    }
}
