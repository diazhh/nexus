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
package org.thingsboard.server.dao.role;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Role;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

@Component
@Slf4j
public class RoleValidator extends DataValidator<Role> {

    @Autowired
    private TenantService tenantService;

    @Override
    protected void validateDataImpl(TenantId tenantId, Role role) {
        if (StringUtils.isEmpty(role.getName())) {
            throw new DataValidationException("Role name should be specified!");
        }
        
        if (role.getName().length() > 255) {
            throw new DataValidationException("Role name length should not exceed 255 characters!");
        }
        
        if (role.getDescription() != null && role.getDescription().length() > 1024) {
            throw new DataValidationException("Role description length should not exceed 1024 characters!");
        }
        
        if (role.getTenantId() == null) {
            throw new DataValidationException("Role should be assigned to tenant!");
        }
        
        if (!role.getTenantId().isNullUid()) {
            if (!tenantService.tenantExists(role.getTenantId())) {
                throw new DataValidationException("Role is referencing non-existing tenant!");
            }
        }
    }

    @Override
    protected void validateCreate(TenantId tenantId, Role role) {
        if (role.isSystem()) {
            throw new DataValidationException("System roles cannot be created via API!");
        }
    }

    @Override
    protected Role validateUpdate(TenantId tenantId, Role role) {
        Role existingRole = roleDao.findById(tenantId, role.getUuidId());
        if (existingRole == null) {
            throw new DataValidationException("Unable to update non-existent role!");
        }
        
        if (existingRole.isSystem()) {
            throw new DataValidationException("System roles cannot be modified!");
        }
        
        if (!existingRole.getTenantId().equals(role.getTenantId())) {
            throw new DataValidationException("Role tenant cannot be changed!");
        }
        
        return existingRole;
    }

    @Autowired
    private RoleDao roleDao;
}
