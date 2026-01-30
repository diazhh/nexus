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
package org.thingsboard.server.dao.nexus;

import org.thingsboard.server.common.data.id.NexusModuleId;
import org.thingsboard.server.common.data.nexus.NexusModule;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO interface for NexusModule operations.
 */
public interface NexusModuleDao extends Dao<NexusModule> {

    /**
     * Save or update a module
     */
    NexusModule save(NexusModule module);

    /**
     * Find module by ID
     */
    NexusModule findById(UUID moduleId);

    /**
     * Find module by unique key
     */
    Optional<NexusModule> findByModuleKey(String moduleKey);

    /**
     * Find all available modules
     */
    List<NexusModule> findAvailableModules();

    /**
     * Find all system modules
     */
    List<NexusModule> findSystemModules();

    /**
     * Find modules by category
     */
    List<NexusModule> findByCategory(String category);

    /**
     * Find all modules with pagination
     */
    PageData<NexusModule> findAllModules(PageLink pageLink);

    /**
     * Find modules by IDs
     */
    List<NexusModule> findByIds(List<UUID> moduleIds);

    /**
     * Count available modules
     */
    long countAvailableModules();

    /**
     * Check if module key exists
     */
    boolean existsByModuleKey(String moduleKey);

    /**
     * Delete module by ID
     */
    boolean removeById(UUID moduleId);
}
