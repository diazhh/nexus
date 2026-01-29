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
 * Exception thrown for business rule violations in Reservoir Module.
 */
public class RvBusinessException extends RvException {

    private final String errorCode;

    public RvBusinessException(String message) {
        super(message);
        this.errorCode = "RV_BUSINESS_ERROR";
    }

    public RvBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RvBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "RV_BUSINESS_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }

    // Common error codes
    public static final String INVALID_HIERARCHY = "RV_INVALID_HIERARCHY";
    public static final String INSUFFICIENT_DATA = "RV_INSUFFICIENT_DATA";
    public static final String CALCULATION_ERROR = "RV_CALCULATION_ERROR";
    public static final String INVALID_FLUID_TYPE = "RV_INVALID_FLUID_TYPE";
    public static final String PRESSURE_OUT_OF_RANGE = "RV_PRESSURE_OUT_OF_RANGE";
    public static final String INVALID_PVT_DATA = "RV_INVALID_PVT_DATA";
}
