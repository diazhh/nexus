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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.concurrent.ExecutionException;

/**
 * Rule Engine Node for Material Balance Equation calculations.
 *
 * Supports calculation of:
 * - F (Underground withdrawal)
 * - Eo (Oil expansion)
 * - Eg (Gas cap expansion)
 * - Efw (Formation and water expansion)
 * - OOIP estimation via Havlena-Odeh method
 * - Drive mechanism indices (DDI, SDI, WDI, CDI)
 */
@Slf4j
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "rv material balance",
        configClazz = RvMaterialBalanceNodeConfiguration.class,
        nodeDescription = "Calculates Material Balance Equation terms and OOIP",
        nodeDetails = "Performs Material Balance calculations using the Havlena-Odeh method. " +
                "Calculates underground withdrawal (F), expansion terms (Eo, Eg, Efw), and estimates OOIP. " +
                "Supports drive mechanism analysis (Depletion, Gas Cap, Water, Compaction drives). " +
                "Input parameters can come from message body, metadata, or attributes.",
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbEnrichmentNodeRvMaterialBalanceConfig",
        icon = "calculate"
)
public class RvMaterialBalanceNode implements TbNode {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 8;

    private RvMaterialBalanceNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, RvMaterialBalanceNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        try {
            ObjectNode msgData = (ObjectNode) mapper.readTree(msg.getData());

            BigDecimal result;
            String resultKey = config.getResultKey();

            switch (config.getCalculationType()) {
                case "F":
                    result = calculateF(msgData);
                    break;
                case "Eo":
                    result = calculateEo(msgData);
                    break;
                case "Eg":
                    result = calculateEg(msgData);
                    break;
                case "Efw":
                    result = calculateEfw(msgData);
                    break;
                case "OOIP_SIMPLE":
                    result = calculateOOIPSimple(msgData);
                    break;
                case "DDI":
                    result = calculateDDI(msgData);
                    break;
                case "SDI":
                    result = calculateSDI(msgData);
                    break;
                case "WDI":
                    result = calculateWDI(msgData);
                    break;
                case "CDI":
                    result = calculateCDI(msgData);
                    break;
                default:
                    throw new TbNodeException("Unknown calculation type: " + config.getCalculationType());
            }

            // Add result to message
            msgData.put(resultKey, result.doubleValue());
            TbMsg newMsg = ctx.transformMsg(msg, msg.getMetaData(), msgData.toString());

            ctx.tellSuccess(newMsg);
            log.debug("Material Balance calculation completed: {} = {}", resultKey, result);

        } catch (Exception e) {
            log.error("Error in Material Balance calculation: {}", e.getMessage());
            ctx.tellFailure(msg, e);
        }
    }

    /**
     * Calculate F - Underground withdrawal
     * F = Np*Bo + (Gp - Np*Rs)*Bg + Wp*Bw - Wi*Bw - Gi*Bg
     */
    private BigDecimal calculateF(ObjectNode data) {
        BigDecimal Np = getDecimalValue(data, config.getNpKey());
        BigDecimal Gp = getDecimalValue(data, config.getGpKey());
        BigDecimal Wp = getDecimalValue(data, config.getWpKey(), BigDecimal.ZERO);
        BigDecimal Wi = getDecimalValue(data, config.getWiKey(), BigDecimal.ZERO);
        BigDecimal Gi = getDecimalValue(data, config.getGiKey(), BigDecimal.ZERO);
        BigDecimal Bo = getDecimalValue(data, config.getBoKey());
        BigDecimal Bg = getDecimalValue(data, config.getBgKey());
        BigDecimal Rs = getDecimalValue(data, config.getRsKey());
        BigDecimal Bw = getDecimalValue(data, config.getBwKey(), BigDecimal.ONE);

        // F = Np*Bo + (Gp - Np*Rs)*Bg + Wp*Bw - Wi*Bw - Gi*Bg
        BigDecimal F = Np.multiply(Bo)
                .add(Gp.subtract(Np.multiply(Rs)).multiply(Bg))
                .add(Wp.multiply(Bw))
                .subtract(Wi.multiply(Bw))
                .subtract(Gi.multiply(Bg));

        return F.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Eo - Oil expansion term
     * Eo = (Bo - Boi) + (Rsi - Rs)*Bg
     */
    private BigDecimal calculateEo(ObjectNode data) {
        BigDecimal Bo = getDecimalValue(data, config.getBoKey());
        BigDecimal Boi = getDecimalValue(data, config.getBoiKey());
        BigDecimal Rs = getDecimalValue(data, config.getRsKey());
        BigDecimal Rsi = getDecimalValue(data, config.getRsiKey());
        BigDecimal Bg = getDecimalValue(data, config.getBgKey());

        // Eo = (Bo - Boi) + (Rsi - Rs)*Bg
        BigDecimal Eo = Bo.subtract(Boi)
                .add(Rsi.subtract(Rs).multiply(Bg));

        return Eo.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Eg - Gas cap expansion term
     * Eg = Boi * (Bg/Bgi - 1)
     */
    private BigDecimal calculateEg(ObjectNode data) {
        BigDecimal Boi = getDecimalValue(data, config.getBoiKey());
        BigDecimal Bg = getDecimalValue(data, config.getBgKey());
        BigDecimal Bgi = getDecimalValue(data, config.getBgiKey());

        if (Bgi.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Eg = Boi * (Bg/Bgi - 1)
        BigDecimal Eg = Boi.multiply(Bg.divide(Bgi, MC).subtract(BigDecimal.ONE));

        return Eg.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Efw - Formation and water expansion term
     * Efw = (1 + m)*Boi*[(cw*Swi + cf)/(1-Swi)]*ΔP
     */
    private BigDecimal calculateEfw(ObjectNode data) {
        BigDecimal Boi = getDecimalValue(data, config.getBoiKey());
        BigDecimal m = getDecimalValue(data, config.getMKey(), BigDecimal.ZERO);
        BigDecimal cw = getDecimalValue(data, config.getCwKey());
        BigDecimal cf = getDecimalValue(data, config.getCfKey());
        BigDecimal Swi = getDecimalValue(data, config.getSwiKey());
        BigDecimal Pi = getDecimalValue(data, config.getPiKey());
        BigDecimal P = getDecimalValue(data, config.getPKey());

        BigDecimal deltaP = Pi.subtract(P);
        BigDecimal denominator = BigDecimal.ONE.subtract(Swi);

        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Efw = (1 + m)*Boi*[(cw*Swi + cf)/(1-Swi)]*ΔP
        BigDecimal numerator = cw.multiply(Swi).add(cf);
        BigDecimal Efw = BigDecimal.ONE.add(m)
                .multiply(Boi)
                .multiply(numerator.divide(denominator, MC))
                .multiply(deltaP);

        return Efw.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate OOIP using simple material balance (no gas cap, no water influx)
     * N = F / (Eo + Efw)
     */
    private BigDecimal calculateOOIPSimple(ObjectNode data) {
        BigDecimal F = calculateF(data);
        BigDecimal Eo = calculateEo(data);
        BigDecimal Efw = calculateEfw(data);

        BigDecimal denominator = Eo.add(Efw);
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Cannot calculate OOIP: (Eo + Efw) = 0");
        }

        BigDecimal N = F.divide(denominator, MC);
        return N.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate DDI - Depletion Drive Index
     * DDI = N * Eo / F
     */
    private BigDecimal calculateDDI(ObjectNode data) {
        BigDecimal N = getDecimalValue(data, config.getNKey());
        BigDecimal Eo = calculateEo(data);
        BigDecimal F = calculateF(data);

        if (F.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal DDI = N.multiply(Eo).divide(F, MC);
        return DDI.min(BigDecimal.ONE).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate SDI - Segregation (Gas Cap) Drive Index
     * SDI = N * m * Eg / F
     */
    private BigDecimal calculateSDI(ObjectNode data) {
        BigDecimal N = getDecimalValue(data, config.getNKey());
        BigDecimal m = getDecimalValue(data, config.getMKey(), BigDecimal.ZERO);
        BigDecimal Eg = calculateEg(data);
        BigDecimal F = calculateF(data);

        if (F.compareTo(BigDecimal.ZERO) == 0 || m.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal SDI = N.multiply(m).multiply(Eg).divide(F, MC);
        return SDI.min(BigDecimal.ONE).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate WDI - Water Drive Index
     * WDI = We / F (or estimated as 1 - DDI - SDI - CDI)
     */
    private BigDecimal calculateWDI(ObjectNode data) {
        BigDecimal DDI = calculateDDI(data);
        BigDecimal SDI = calculateSDI(data);
        BigDecimal CDI = calculateCDI(data);

        BigDecimal WDI = BigDecimal.ONE.subtract(DDI).subtract(SDI).subtract(CDI);
        return WDI.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate CDI - Compaction Drive Index
     * CDI = N * Efw / F
     */
    private BigDecimal calculateCDI(ObjectNode data) {
        BigDecimal N = getDecimalValue(data, config.getNKey());
        BigDecimal Efw = calculateEfw(data);
        BigDecimal F = calculateF(data);

        if (F.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal CDI = N.multiply(Efw).divide(F, MC);
        return CDI.min(BigDecimal.ONE).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal getDecimalValue(ObjectNode data, String key) {
        return getDecimalValue(data, key, null);
    }

    private BigDecimal getDecimalValue(ObjectNode data, String key, BigDecimal defaultValue) {
        if (key == null || key.isEmpty()) {
            if (defaultValue != null) return defaultValue;
            throw new IllegalArgumentException("Missing required parameter key");
        }

        JsonNode node = data.get(key);
        if (node == null || node.isNull()) {
            if (defaultValue != null) return defaultValue;
            throw new IllegalArgumentException("Missing required value for: " + key);
        }

        return new BigDecimal(node.asText());
    }

    @Override
    public void destroy() {
        // No resources to clean up
    }
}
