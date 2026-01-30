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

import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

/**
 * Represents a NEXUS module that can be assigned to tenants.
 * Modules include CT (Corte de Tubing), RV (Reparación de Válvulas), DR (Drilling Rigs), etc.
 */
export interface NexusModule extends BaseData<NexusModuleId> {
  moduleKey: string;
  name: string;
  description?: string;
  version: string;
  category?: string;
  icon?: string;
  routePath?: string;
  systemModule: boolean;
  available: boolean;
  displayOrder: number;
  additionalInfo?: any;
}

export interface NexusModuleId extends EntityId {
  entityType: EntityType.NEXUS_MODULE;
}

/**
 * Represents the assignment of a NexusModule to a specific Tenant.
 */
export interface TenantModule extends BaseData<TenantModuleId> {
  tenantId: TenantModuleIdRef;
  moduleId: NexusModuleIdRef;
  active: boolean;
  activationDate: number;
  deactivationDate?: number;
  activatedBy?: UserIdRef;
  configuration?: any;
  additionalInfo?: any;
}

export interface TenantModuleId extends EntityId {
  entityType: EntityType.TENANT_MODULE;
}

export interface TenantModuleIdRef {
  id: string;
}

export interface NexusModuleIdRef {
  id: string;
}

export interface UserIdRef {
  id: string;
}

/**
 * Extended TenantModule with module details for UI display
 */
export interface TenantModuleInfo extends TenantModule {
  module?: NexusModule;
}

/**
 * Module keys used in the system
 */
export type ModuleKey = 'CT' | 'RV' | 'DR' | string;

/**
 * Module categories
 */
export type ModuleCategory = 'OPERATIONS' | 'ADMINISTRATION' | 'ANALYTICS' | string;

/**
 * Request to assign modules to a tenant
 */
export interface AssignModulesRequest {
  moduleIds: string[];
}

/**
 * Module access check response
 */
export interface ModuleAccessResponse {
  hasAccess: boolean;
  moduleKey: string;
}
