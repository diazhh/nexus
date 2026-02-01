///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  MappingTemplate,
  MappingTemplateRule,
  DataSourceConfig,
  DataMappingModuleKey
} from '@shared/models/data-mapping.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';

@Injectable({
  providedIn: 'root'
})
export class DataMappingService {

  private readonly BASE_URL = '/api/nexus/dataMapping';

  constructor(private http: HttpClient) {}

  // ========================
  // Mapping Templates
  // ========================

  /**
   * Get all mapping templates with pagination
   */
  getMappingTemplates(pageLink: PageLink, config?: RequestConfig): Observable<PageData<MappingTemplate>> {
    return this.http.get<PageData<MappingTemplate>>(
      `${this.BASE_URL}/templates${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get mapping templates by module key with pagination
   */
  getMappingTemplatesByModule(moduleKey: DataMappingModuleKey, pageLink: PageLink, config?: RequestConfig): Observable<PageData<MappingTemplate>> {
    return this.http.get<PageData<MappingTemplate>>(
      `${this.BASE_URL}/templates/module/${moduleKey}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get active mapping templates by module key
   */
  getActiveMappingTemplatesByModule(moduleKey: DataMappingModuleKey, config?: RequestConfig): Observable<MappingTemplate[]> {
    return this.http.get<MappingTemplate[]>(
      `${this.BASE_URL}/templates/module/${moduleKey}/active`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get mapping template by ID
   */
  getMappingTemplateById(templateId: string, config?: RequestConfig): Observable<MappingTemplate> {
    return this.http.get<MappingTemplate>(
      `${this.BASE_URL}/template/${templateId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Save mapping template (create or update)
   */
  saveMappingTemplate(template: MappingTemplate, config?: RequestConfig): Observable<MappingTemplate> {
    return this.http.post<MappingTemplate>(
      `${this.BASE_URL}/template`,
      template,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Delete mapping template
   */
  deleteMappingTemplate(templateId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE_URL}/template/${templateId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Duplicate mapping template with new name
   */
  duplicateMappingTemplate(templateId: string, newName: string, config?: RequestConfig): Observable<MappingTemplate> {
    return this.http.post<MappingTemplate>(
      `${this.BASE_URL}/template/${templateId}/duplicate?newName=${encodeURIComponent(newName)}`,
      null,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ========================
  // Mapping Template Rules
  // ========================

  /**
   * Get all rules for a template
   */
  getMappingTemplateRules(templateId: string, config?: RequestConfig): Observable<MappingTemplateRule[]> {
    return this.http.get<MappingTemplateRule[]>(
      `${this.BASE_URL}/template/${templateId}/rules`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get mapping template rule by ID
   */
  getMappingTemplateRuleById(ruleId: string, config?: RequestConfig): Observable<MappingTemplateRule> {
    return this.http.get<MappingTemplateRule>(
      `${this.BASE_URL}/rule/${ruleId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Save mapping template rule (create or update)
   */
  saveMappingTemplateRule(rule: MappingTemplateRule, config?: RequestConfig): Observable<MappingTemplateRule> {
    return this.http.post<MappingTemplateRule>(
      `${this.BASE_URL}/rule`,
      rule,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Bulk create mapping template rules
   */
  saveMappingTemplateRules(templateId: string, rules: MappingTemplateRule[], config?: RequestConfig): Observable<MappingTemplateRule[]> {
    return this.http.post<MappingTemplateRule[]>(
      `${this.BASE_URL}/template/${templateId}/rules/bulk`,
      rules,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Delete mapping template rule
   */
  deleteMappingTemplateRule(ruleId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE_URL}/rule/${ruleId}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Count rules for a template
   */
  countMappingTemplateRules(templateId: string, config?: RequestConfig): Observable<number> {
    return this.http.get<number>(
      `${this.BASE_URL}/template/${templateId}/rules/count`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ========================
  // Template Application
  // ========================

  /**
   * Apply mapping template to create DataSourceConfig + DataMappingRules
   */
  applyMappingTemplate(templateId: string, deviceId: string, targetAssetId: string, config?: RequestConfig): Observable<DataSourceConfig> {
    return this.http.post<DataSourceConfig>(
      `${this.BASE_URL}/template/${templateId}/apply?deviceId=${deviceId}&targetAssetId=${targetAssetId}`,
      null,
      defaultHttpOptionsFromConfig(config)
    );
  }

  // ========================
  // Data Source Configurations
  // ========================

  /**
   * Get all data source configurations with pagination
   */
  getDataSources(pageLink: PageLink, config?: RequestConfig): Observable<PageData<DataSourceConfig>> {
    return this.http.get<PageData<DataSourceConfig>>(
      `${this.BASE_URL}/dataSources${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get data source configurations by module key with pagination
   */
  getDataSourcesByModule(moduleKey: DataMappingModuleKey, pageLink: PageLink, config?: RequestConfig): Observable<PageData<DataSourceConfig>> {
    return this.http.get<PageData<DataSourceConfig>>(
      `${this.BASE_URL}/dataSources${pageLink.toQuery()}&moduleKey=${moduleKey}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Get data source configuration by ID
   */
  getDataSourceById(id: string, config?: RequestConfig): Observable<DataSourceConfig> {
    return this.http.get<DataSourceConfig>(
      `${this.BASE_URL}/dataSource/${id}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  /**
   * Delete data source configuration
   */
  deleteDataSource(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE_URL}/dataSource/${id}`,
      defaultHttpOptionsFromConfig(config)
    );
  }
}
