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

export interface TemplateVariable {
  name: string;
  label: string;
  dataType: string;
  description: string;
  isRequired: boolean;
  validationPattern?: string;
  maxLength?: number;
  example?: string;
}

export interface TemplateDefinition {
  id: string;
  templateCode: string;
  templateName: string;
  description: string;
  moduleCode: string;
  entityType: string;
  category: string;
  version: string;
  isActive: boolean;
  templateStructure: any;
  requiredVariables: TemplateVariable[];
  createdBy: string;
  createdTime: number;
  updatedBy?: string;
  updatedTime?: number;
  tenantId: string;
}

export interface CreateTemplateRequest {
  templateName: string;
  description: string;
  moduleCode: string;
  entityType: string;
  category: string;
  templateStructure: any;
  requiredVariables: TemplateVariable[];
}

export interface UpdateTemplateRequest {
  templateName?: string;
  description?: string;
  templateStructure?: any;
  requiredVariables?: TemplateVariable[];
  changeDescription?: string;
}

export interface CreateFromTemplateRequest {
  templateId: string;
  variables: { [key: string]: any };
}

/**
 * Backward-compatible alias for TemplateDefinition.
 * @deprecated Use TemplateDefinition instead.
 */
export type CTTemplate = TemplateDefinition;

@Injectable({
  providedIn: 'root'
})
export class CTTemplateService {

  private readonly baseUrl = '/api/nexus/templates';

  constructor(private http: HttpClient) {}

  // --- CRUD Operations ---

  createTemplate(request: CreateTemplateRequest, tenantId: string, userId: string): Observable<TemplateDefinition> {
    return this.http.post<TemplateDefinition>(this.baseUrl, request, {
      params: { tenantId, userId }
    });
  }

  updateTemplate(templateId: string, request: UpdateTemplateRequest, userId: string): Observable<TemplateDefinition> {
    return this.http.put<TemplateDefinition>(`${this.baseUrl}/${templateId}`, request, {
      params: { userId }
    });
  }

  getTemplateById(templateId: string): Observable<TemplateDefinition> {
    return this.http.get<TemplateDefinition>(`${this.baseUrl}/${templateId}`);
  }

  getTemplateByCode(templateCode: string): Observable<TemplateDefinition> {
    return this.http.get<TemplateDefinition>(`${this.baseUrl}/code/${templateCode}`);
  }

  getTemplatesByModule(moduleCode: string, tenantId: string): Observable<TemplateDefinition[]> {
    return this.http.get<TemplateDefinition[]>(`${this.baseUrl}/module/${moduleCode}`, {
      params: { tenantId }
    });
  }

  getTemplatesByModuleAndType(moduleCode: string, entityType: string, tenantId: string): Observable<TemplateDefinition[]> {
    return this.http.get<TemplateDefinition[]>(`${this.baseUrl}/module/${moduleCode}/type/${entityType}`, {
      params: { tenantId }
    });
  }

  getTemplatesByTenant(tenantId: string): Observable<TemplateDefinition[]> {
    return this.http.get<TemplateDefinition[]>(`${this.baseUrl}/tenant/${tenantId}`);
  }

  deactivateTemplate(templateId: string, userId: string): Observable<TemplateDefinition> {
    return this.http.put<TemplateDefinition>(`${this.baseUrl}/${templateId}/deactivate`, null, {
      params: { userId }
    });
  }

  activateTemplate(templateId: string, userId: string): Observable<TemplateDefinition> {
    return this.http.put<TemplateDefinition>(`${this.baseUrl}/${templateId}/activate`, null, {
      params: { userId }
    });
  }

  deleteTemplate(templateId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${templateId}`);
  }

  // --- Query Operations ---

  getTemplateVersions(templateId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${templateId}/versions`);
  }

  getTemplateInstances(templateId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${templateId}/instances`);
  }

  getInstancesByTenant(tenantId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/tenant/${tenantId}/instances`);
  }

  countTemplates(tenantId: string): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/tenant/${tenantId}/count`);
  }

  countInstances(templateId: string): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/${templateId}/instances/count`);
  }

  // --- Backward-compatible convenience methods ---

  getUnitTemplates(tenantId: string): Observable<TemplateDefinition[]> {
    return this.getTemplatesByModuleAndType('CT', 'CT_UNIT', tenantId);
  }

  getReelTemplates(tenantId: string): Observable<TemplateDefinition[]> {
    return this.getTemplatesByModuleAndType('CT', 'CT_REEL', tenantId);
  }
}
