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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.template.TemplateDefinitionDto;
import org.thingsboard.server.common.data.template.TemplateInstanceDto;
import org.thingsboard.server.common.data.template.TemplateVersionDto;
import org.thingsboard.server.dao.model.sql.TemplateDefinitionEntity;
import org.thingsboard.server.dao.model.sql.TemplateInstanceEntity;
import org.thingsboard.server.dao.model.sql.TemplateVersionEntity;
import org.thingsboard.server.dao.template.TemplateDefinitionDaoService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JpaTemplateDefinitionDaoService implements TemplateDefinitionDaoService {

    private final TemplateDefinitionRepository templateDefinitionRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateInstanceRepository templateInstanceRepository;

    // --- Template Definition ---

    @Override
    public TemplateDefinitionDto saveTemplateDefinition(TemplateDefinitionDto dto) {
        TemplateDefinitionEntity entity = toEntity(dto);
        TemplateDefinitionEntity saved = templateDefinitionRepository.save(entity);
        return toDto(saved);
    }

    @Override
    public Optional<TemplateDefinitionDto> findById(UUID id) {
        return templateDefinitionRepository.findById(id).map(this::toDto);
    }

    @Override
    public Optional<TemplateDefinitionDto> findByTemplateCode(String templateCode) {
        return templateDefinitionRepository.findByTemplateCode(templateCode).map(this::toDto);
    }

    @Override
    public List<TemplateDefinitionDto> findByModuleAndTenant(String moduleCode, UUID tenantId, Boolean isActive) {
        return templateDefinitionRepository.findByModuleCodeAndTenantIdAndIsActive(moduleCode, tenantId, isActive)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<TemplateDefinitionDto> findByModuleAndEntityTypeAndTenant(String moduleCode, String entityType, UUID tenantId, Boolean isActive) {
        return templateDefinitionRepository.findByModuleCodeAndEntityTypeAndTenantIdAndIsActive(moduleCode, entityType, tenantId, isActive)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<TemplateDefinitionDto> findByTenantAndActive(UUID tenantId, Boolean isActive) {
        return templateDefinitionRepository.findByTenantIdAndIsActive(tenantId, isActive)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public long countByTenant(UUID tenantId) {
        return templateDefinitionRepository.countByTenantId(tenantId);
    }

    @Override
    public void deleteById(UUID id) {
        templateDefinitionRepository.deleteById(id);
    }

    // --- Template Version ---

    @Override
    public TemplateVersionDto saveVersion(TemplateVersionDto dto) {
        TemplateVersionEntity entity = toVersionEntity(dto);
        TemplateVersionEntity saved = templateVersionRepository.save(entity);
        return toVersionDto(saved);
    }

    @Override
    public List<TemplateVersionDto> findVersionsByTemplateId(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByCreatedTimeDesc(templateId)
                .stream().map(this::toVersionDto).collect(Collectors.toList());
    }

    @Override
    public long countVersionsByTemplateId(UUID templateId) {
        return templateVersionRepository.countByTemplateId(templateId);
    }

    @Override
    public void deleteVersionsByTemplateId(UUID templateId) {
        templateVersionRepository.findByTemplateIdOrderByCreatedTimeDesc(templateId)
                .forEach(templateVersionRepository::delete);
    }

    // --- Template Instance ---

    @Override
    public TemplateInstanceDto saveInstance(TemplateInstanceDto dto) {
        TemplateInstanceEntity entity = toInstanceEntity(dto);
        TemplateInstanceEntity saved = templateInstanceRepository.save(entity);
        return toInstanceDto(saved);
    }

    @Override
    public List<TemplateInstanceDto> findInstancesByTemplateId(UUID templateId) {
        return templateInstanceRepository.findByTemplateIdOrderByCreatedTimeDesc(templateId)
                .stream().map(this::toInstanceDto).collect(Collectors.toList());
    }

    @Override
    public List<TemplateInstanceDto> findInstancesByTenant(UUID tenantId) {
        return templateInstanceRepository.findByTenantIdOrderByCreatedTimeDesc(tenantId)
                .stream().map(this::toInstanceDto).collect(Collectors.toList());
    }

    @Override
    public long countInstancesByTemplateId(UUID templateId) {
        return templateInstanceRepository.countByTemplateId(templateId);
    }

    // --- Conversion methods ---

    private TemplateDefinitionDto toDto(TemplateDefinitionEntity entity) {
        return TemplateDefinitionDto.builder()
                .id(entity.getId())
                .templateCode(entity.getTemplateCode())
                .templateName(entity.getTemplateName())
                .description(entity.getDescription())
                .moduleCode(entity.getModuleCode())
                .entityType(entity.getEntityType())
                .category(entity.getCategory())
                .version(entity.getVersion())
                .isActive(entity.getIsActive())
                .templateStructureJson(entity.getTemplateStructure())
                .requiredVariablesJson(entity.getRequiredVariables())
                .createdBy(entity.getCreatedBy())
                .createdTime(entity.getCreatedTime())
                .updatedBy(entity.getUpdatedBy())
                .updatedTime(entity.getUpdatedTime())
                .tenantId(entity.getTenantId())
                .build();
    }

    private TemplateDefinitionEntity toEntity(TemplateDefinitionDto dto) {
        TemplateDefinitionEntity entity = new TemplateDefinitionEntity();
        entity.setId(dto.getId());
        entity.setTemplateCode(dto.getTemplateCode());
        entity.setTemplateName(dto.getTemplateName());
        entity.setDescription(dto.getDescription());
        entity.setModuleCode(dto.getModuleCode());
        entity.setEntityType(dto.getEntityType());
        entity.setCategory(dto.getCategory());
        entity.setVersion(dto.getVersion());
        entity.setIsActive(dto.getIsActive());
        entity.setTemplateStructure(dto.getTemplateStructureJson());
        entity.setRequiredVariables(dto.getRequiredVariablesJson());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedTime(dto.getCreatedTime());
        entity.setUpdatedBy(dto.getUpdatedBy());
        entity.setUpdatedTime(dto.getUpdatedTime());
        entity.setTenantId(dto.getTenantId());
        return entity;
    }

    private TemplateVersionDto toVersionDto(TemplateVersionEntity entity) {
        return TemplateVersionDto.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .version(entity.getVersion())
                .templateStructure(entity.getTemplateStructure())
                .changeDescription(entity.getChangeDescription())
                .createdBy(entity.getCreatedBy())
                .createdTime(entity.getCreatedTime())
                .build();
    }

    private TemplateVersionEntity toVersionEntity(TemplateVersionDto dto) {
        TemplateVersionEntity entity = new TemplateVersionEntity();
        entity.setId(dto.getId());
        entity.setTemplateId(dto.getTemplateId());
        entity.setVersion(dto.getVersion());
        entity.setTemplateStructure(dto.getTemplateStructure());
        entity.setChangeDescription(dto.getChangeDescription());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedTime(dto.getCreatedTime());
        return entity;
    }

    private TemplateInstanceDto toInstanceDto(TemplateInstanceEntity entity) {
        return TemplateInstanceDto.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .templateVersion(entity.getTemplateVersion())
                .rootAssetId(entity.getRootAssetId())
                .instanceVariables(entity.getInstanceVariables())
                .createdBy(entity.getCreatedBy())
                .createdTime(entity.getCreatedTime())
                .tenantId(entity.getTenantId())
                .build();
    }

    private TemplateInstanceEntity toInstanceEntity(TemplateInstanceDto dto) {
        TemplateInstanceEntity entity = new TemplateInstanceEntity();
        entity.setId(dto.getId());
        entity.setTemplateId(dto.getTemplateId());
        entity.setTemplateVersion(dto.getTemplateVersion());
        entity.setRootAssetId(dto.getRootAssetId());
        entity.setInstanceVariables(dto.getInstanceVariables());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedTime(dto.getCreatedTime());
        entity.setTenantId(dto.getTenantId());
        return entity;
    }
}
