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
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.nexus.NexusModule;
import org.thingsboard.server.dao.nexus.NexusModuleService;

/**
 * Initializes NEXUS modules on application startup.
 * Registers or updates module definitions in the database.
 */
@Component
@Slf4j
public class NexusModuleInitializer {

    @Autowired
    private NexusModuleService moduleService;

    /**
     * Register all NEXUS modules on application startup.
     * This runs after the application is fully initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100) // Run after other initializers
    public void initializeModules() {
        log.info("Initializing NEXUS modules...");

        try {
            // Register CT Module (Corte de Tubing)
            registerCtModule();

            // Register RV Module (Reparación de Válvulas)
            registerRvModule();

            // Register DR Module (Drilling Rigs)
            registerDrModule();

            log.info("NEXUS modules initialization completed successfully");
        } catch (Exception e) {
            log.error("Error initializing NEXUS modules", e);
        }
    }

    private void registerCtModule() {
        log.debug("Registering CT module (Corte de Tubing)");
        NexusModule module = moduleService.registerModule(
                "CT",                                    // moduleKey
                "Corte de Tubing",                       // name
                "Módulo para gestión de operaciones de corte de tubing. " +
                        "Incluye unidades, órdenes de trabajo, personal y KPIs.",  // description
                "OPERATIONS",                            // category
                "content_cut",                           // icon
                "/ct",                                   // routePath
                false,                                   // isSystem
                1                                        // displayOrder
        );
        log.info("CT module registered: {}", module.getId());
    }

    private void registerRvModule() {
        log.debug("Registering RV module (Reparación de Válvulas)");
        NexusModule module = moduleService.registerModule(
                "RV",                                    // moduleKey
                "Reparación de Válvulas",                // name
                "Módulo para gestión de reparación de válvulas de pozos. " +
                        "Incluye pozos, sets de válvulas, servicios y seguimiento.", // description
                "OPERATIONS",                            // category
                "build",                                 // icon
                "/rv",                                   // routePath
                false,                                   // isSystem
                2                                        // displayOrder
        );
        log.info("RV module registered: {}", module.getId());
    }

    private void registerDrModule() {
        log.debug("Registering DR module (Drilling Rigs)");
        NexusModule module = moduleService.registerModule(
                "DR",                                    // moduleKey
                "Drilling Rigs",                         // name
                "Módulo para gestión de equipos de perforación. " +
                        "Incluye rigs, pozos, BHAs, parámetros de perforación y KPIs.", // description
                "OPERATIONS",                            // category
                "precision_manufacturing",               // icon
                "/dr",                                   // routePath
                false,                                   // isSystem
                3                                        // displayOrder
        );
        log.info("DR module registered: {}", module.getId());
    }
}
