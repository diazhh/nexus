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

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * Configuration for the Material Balance Node.
 */
@Data
public class RvMaterialBalanceNodeConfiguration implements NodeConfiguration<RvMaterialBalanceNodeConfiguration> {

    /**
     * Type of calculation to perform:
     * - F: Underground withdrawal
     * - Eo: Oil expansion
     * - Eg: Gas cap expansion
     * - Efw: Formation and water expansion
     * - OOIP_SIMPLE: Simple OOIP calculation (no gas cap, no water influx)
     * - DDI: Depletion Drive Index
     * - SDI: Segregation (Gas Cap) Drive Index
     * - WDI: Water Drive Index
     * - CDI: Compaction Drive Index
     */
    private String calculationType;

    // Result configuration
    private String resultKey;

    // Production data keys
    private String npKey;      // Cumulative oil production (Np)
    private String gpKey;      // Cumulative gas production (Gp)
    private String wpKey;      // Cumulative water production (Wp)
    private String wiKey;      // Cumulative water injection (Wi)
    private String giKey;      // Cumulative gas injection (Gi)

    // Current PVT keys
    private String boKey;      // Current oil FVF (Bo)
    private String bgKey;      // Current gas FVF (Bg)
    private String rsKey;      // Current solution GOR (Rs)
    private String bwKey;      // Current water FVF (Bw)
    private String pKey;       // Current pressure (P)

    // Initial condition keys
    private String boiKey;     // Initial oil FVF (Boi)
    private String bgiKey;     // Initial gas FVF (Bgi)
    private String rsiKey;     // Initial solution GOR (Rsi)
    private String piKey;      // Initial pressure (Pi)
    private String swiKey;     // Initial water saturation (Swi)

    // Compressibility keys
    private String cwKey;      // Water compressibility (cw)
    private String cfKey;      // Formation compressibility (cf)

    // Gas cap ratio
    private String mKey;       // Gas cap ratio (m = GBgi/NBoi)

    // OOIP for drive index calculations
    private String nKey;       // Original oil in place (N)

    @Override
    public RvMaterialBalanceNodeConfiguration defaultConfiguration() {
        RvMaterialBalanceNodeConfiguration config = new RvMaterialBalanceNodeConfiguration();
        config.setCalculationType("F");
        config.setResultKey("F");

        // Production data
        config.setNpKey("Np");
        config.setGpKey("Gp");
        config.setWpKey("Wp");
        config.setWiKey("Wi");
        config.setGiKey("Gi");

        // Current PVT
        config.setBoKey("Bo");
        config.setBgKey("Bg");
        config.setRsKey("Rs");
        config.setBwKey("Bw");
        config.setPKey("P");

        // Initial conditions
        config.setBoiKey("Boi");
        config.setBgiKey("Bgi");
        config.setRsiKey("Rsi");
        config.setPiKey("Pi");
        config.setSwiKey("Swi");

        // Compressibilities
        config.setCwKey("cw");
        config.setCfKey("cf");

        // Gas cap and OOIP
        config.setMKey("m");
        config.setNKey("N");

        return config;
    }
}
