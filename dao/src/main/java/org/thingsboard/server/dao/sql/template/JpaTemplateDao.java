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
package org.thingsboard.server.dao.sql.template;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.template.Template;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TemplateEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.template.TemplateDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

/**
 * JPA implementation of TemplateDao.
 */
@Component
@RequiredArgsConstructor
@SqlDao
public class JpaTemplateDao extends JpaAbstractDao<TemplateEntity, Template> implements TemplateDao {

    private final TemplateRepository templateRepository;

    @Override
    protected Class<TemplateEntity> getEntityClass() {
        return TemplateEntity.class;
    }

    @Override
    protected JpaRepository<TemplateEntity, UUID> getRepository() {
        return templateRepository;
    }

    @Override
    public PageData<Template> findTemplatesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(templateRepository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Template findByTenantIdAndName(UUID tenantId, String name) {
        TemplateEntity entity = templateRepository.findByTenantIdAndName(tenantId, name);
        return entity != null ? entity.toData() : null;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TEMPLATE;
    }
}
