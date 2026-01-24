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
package org.thingsboard.server.service.security.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.RolePermission;
import org.thingsboard.server.common.data.security.permission.Operation;
import org.thingsboard.server.common.data.security.permission.Resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SecurityUser extends User {

    private static final long serialVersionUID = -797397440703066079L;

    private Collection<GrantedAuthority> authorities;
    @Getter @Setter
    private boolean enabled;
    @Getter @Setter
    private UserPrincipal userPrincipal;
    @Getter @Setter
    private String sessionId = UUID.randomUUID().toString();
    @Getter @Setter
    private Set<RolePermission> permissions = new HashSet<>();

    public SecurityUser() {
        super();
    }

    public SecurityUser(UserId id) {
        super(id);
    }

    public SecurityUser(User user, boolean enabled, UserPrincipal userPrincipal) {
        super(user);
        this.enabled = enabled;
        this.userPrincipal = userPrincipal;
    }

    public SecurityUser(User user, boolean enabled, UserPrincipal userPrincipal, Set<RolePermission> permissions) {
        super(user);
        this.enabled = enabled;
        this.userPrincipal = userPrincipal;
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }

    public Collection<GrantedAuthority> getAuthorities() {
        if (authorities == null) {
            authorities = Stream.of(SecurityUser.this.getAuthority())
                    .map(authority -> new SimpleGrantedAuthority(authority.name()))
                    .collect(Collectors.toList());
        }
        return authorities;
    }

    public boolean hasPermission(Resource resource, Operation operation) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        for (RolePermission permission : permissions) {
            if (matchesResource(permission.getResource(), resource) && 
                matchesOperation(permission.getOperation(), operation)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyPermission(Resource resource) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        return permissions.stream()
                .anyMatch(p -> matchesResource(p.getResource(), resource));
    }

    private boolean matchesResource(Resource permissionResource, Resource requestedResource) {
        if (permissionResource == Resource.ALL) {
            return true;
        }
        return permissionResource == requestedResource;
    }

    private boolean matchesOperation(Operation permissionOperation, Operation requestedOperation) {
        if (permissionOperation == Operation.ALL) {
            return true;
        }
        return permissionOperation == requestedOperation;
    }

}
