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
package org.thingsboard.nexus.rv.exception;

/**
 * Exception thrown when reservoir calculation errors occur.
 * This includes errors in PVT calculations, volumetric calculations,
 * material balance analysis, decline analysis, and other
 * petroleum engineering calculations.
 */
public class RvCalculationException extends RvException {

    private final String calculationType;
    private final String errorCode;

    public RvCalculationException(String message) {
        super(message);
        this.calculationType = "GENERAL";
        this.errorCode = "RV_CALC_ERROR";
    }

    public RvCalculationException(String calculationType, String message) {
        super(message);
        this.calculationType = calculationType;
        this.errorCode = "RV_CALC_" + calculationType.toUpperCase() + "_ERROR";
    }

    public RvCalculationException(String message, Throwable cause) {
        super(message, cause);
        this.calculationType = "GENERAL";
        this.errorCode = "RV_CALC_ERROR";
    }

    public RvCalculationException(String calculationType, String message, Throwable cause) {
        super(message, cause);
        this.calculationType = calculationType;
        this.errorCode = "RV_CALC_" + calculationType.toUpperCase() + "_ERROR";
    }

    public String getCalculationType() {
        return calculationType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // Common calculation types
    public static final String CALC_PVT = "PVT";
    public static final String CALC_OOIP = "OOIP";
    public static final String CALC_OGIP = "OGIP";
    public static final String CALC_MATERIAL_BALANCE = "MATERIAL_BALANCE";
    public static final String CALC_DECLINE = "DECLINE";
    public static final String CALC_IPR = "IPR";
    public static final String CALC_VSH = "VSH";
    public static final String CALC_SW = "SW";
    public static final String CALC_REGRESSION = "REGRESSION";
}
