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
package org.thingsboard.server.dao.template;

import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.template.Template;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

/**
 * DAO interface for Template entity.
 */
public interface TemplateDao extends Dao<Template> {

    /**
     * Find templates by tenant id.
     *
     * @param tenantId the tenant id
     * @param pageLink the page link
     * @return the page data of templates
     */
    PageData<Template> findTemplatesByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find template by tenant id and name.
     *
     * @param tenantId the tenant id
     * @param name the template name
     * @return the template
     */
    Template findByTenantIdAndName(UUID tenantId, String name);
}
