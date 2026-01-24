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
package org.thingsboard.server.service.security.permission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.annotation.RequirePermission;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;
import org.thingsboard.server.dao.role.RoleBasedPermissionChecker;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.lang.reflect.Method;

/**
 * Aspect that intercepts methods annotated with @RequirePermission
 * and validates that the current user has the required permission.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final RoleBasedPermissionChecker permissionChecker;

    /**
     * Intercepts methods annotated with @RequirePermission and checks permissions.
     * 
     * @param joinPoint the join point
     * @return the result of the method execution
     * @throws Throwable if the method throws an exception or permission is denied
     */
    @Around("@annotation(org.thingsboard.server.common.data.security.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        if (annotation == null) {
            log.warn("@RequirePermission annotation not found on method: {}", method.getName());
            return joinPoint.proceed();
        }

        SecurityUser currentUser = getCurrentUser();
        if (currentUser == null) {
            log.warn("No authenticated user found for permission check on method: {}", method.getName());
            throw new ThingsboardException("Authentication required", ThingsboardErrorCode.AUTHENTICATION);
        }

        String resourceStr = annotation.resource();
        String operationStr = annotation.operation();

        Resource resource;
        Operation operation;

        try {
            resource = Resource.valueOf(resourceStr);
            operation = Operation.valueOf(operationStr);
        } catch (IllegalArgumentException e) {
            log.error("Invalid resource '{}' or operation '{}' in @RequirePermission annotation on method: {}", 
                resourceStr, operationStr, method.getName());
            throw new ThingsboardException("Invalid permission configuration", ThingsboardErrorCode.GENERAL);
        }

        boolean hasPermission = permissionChecker.hasPermission(currentUser, resource, operation);

        if (!hasPermission) {
            log.warn("User {} does not have permission {} on resource {} for method: {}", 
                currentUser.getEmail(), operation, resource, method.getName());
            throw new ThingsboardException(annotation.message(), ThingsboardErrorCode.PERMISSION_DENIED);
        }

        log.debug("Permission check passed for user {} on resource {} with operation {} for method: {}", 
            currentUser.getEmail(), resource, operation, method.getName());

        return joinPoint.proceed();
    }

    /**
     * Gets the current authenticated user from the security context.
     * 
     * @return the current SecurityUser or null if not authenticated
     */
    private SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        }
        return null;
    }
}
