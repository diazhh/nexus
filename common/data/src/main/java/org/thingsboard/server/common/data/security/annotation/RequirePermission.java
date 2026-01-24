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
package org.thingsboard.server.common.data.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require specific permissions for method execution.
 * Can be applied to methods to enforce role-based permission checks.
 * 
 * Example usage:
 * <pre>
 * {@code
 * @RequirePermission(resource = "DEVICE", operation = "READ")
 * public Device getDevice(DeviceId deviceId) {
 *     // method implementation
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * The resource type that the permission applies to.
     * Must match one of the values in org.thingsboard.server.service.security.permission.Resource
     */
    String resource();
    
    /**
     * The operation type that the permission applies to.
     * Must match one of the values in org.thingsboard.server.service.security.permission.Operation
     */
    String operation();
    
    /**
     * Optional message to include in the exception if permission is denied.
     */
    String message() default "Access denied: insufficient permissions";
}
