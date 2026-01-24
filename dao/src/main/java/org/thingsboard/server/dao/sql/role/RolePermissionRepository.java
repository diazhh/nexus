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
package org.thingsboard.server.dao.sql.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.RolePermissionEntity;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {

    List<RolePermissionEntity> findByRoleId(UUID roleId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RolePermissionEntity rp WHERE rp.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") UUID roleId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RolePermissionEntity rp WHERE rp.roleId = :roleId " +
            "AND rp.resource = :resource AND rp.operation = :operation")
    void deleteByRoleIdAndResourceAndOperation(@Param("roleId") UUID roleId,
                                                @Param("resource") String resource,
                                                @Param("operation") String operation);
}
