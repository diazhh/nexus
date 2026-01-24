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

import { BaseData } from './base-data';
import { RoleId } from './id/role-id';
import { RolePermissionId } from './id/role-permission-id';
import { TenantId } from './id/tenant-id';
import { HasTenantId } from '@shared/models/entity.models';

export interface Role extends BaseData<RoleId>, HasTenantId {
  tenantId: TenantId;
  name: string;
  description: string;
  isSystem: boolean;
  version: number;
  additionalInfo?: any;
}

export interface RolePermission {
  id?: RolePermissionId;
  roleId: RoleId;
  resource: Resource;
  operation: Operation;
}

export enum Resource {
  ALL = 'ALL',
  DEVICE = 'DEVICE',
  ASSET = 'ASSET',
  DASHBOARD = 'DASHBOARD',
  USER = 'USER',
  CUSTOMER = 'CUSTOMER',
  ALARM = 'ALARM',
  RULE_CHAIN = 'RULE_CHAIN',
  ENTITY_VIEW = 'ENTITY_VIEW',
  WIDGET_TYPE = 'WIDGET_TYPE',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  TENANT = 'TENANT',
  TENANT_PROFILE = 'TENANT_PROFILE',
  DEVICE_PROFILE = 'DEVICE_PROFILE',
  ASSET_PROFILE = 'ASSET_PROFILE',
  TB_RESOURCE = 'TB_RESOURCE',
  OTA_PACKAGE = 'OTA_PACKAGE',
  EDGE = 'EDGE',
  RPC = 'RPC',
  QUEUE = 'QUEUE',
  NOTIFICATION = 'NOTIFICATION',
  NOTIFICATION_TARGET = 'NOTIFICATION_TARGET',
  NOTIFICATION_TEMPLATE = 'NOTIFICATION_TEMPLATE',
  NOTIFICATION_RULE = 'NOTIFICATION_RULE',
  OAUTH2_CLIENT = 'OAUTH2_CLIENT',
  DOMAIN = 'DOMAIN',
  MOBILE_APP = 'MOBILE_APP',
  ADMIN_SETTINGS = 'ADMIN_SETTINGS',
  AI_MODEL = 'AI_MODEL',
  API_KEY = 'API_KEY',
  ROLE = 'ROLE'
}

export enum Operation {
  ALL = 'ALL',
  CREATE = 'CREATE',
  READ = 'READ',
  WRITE = 'WRITE',
  DELETE = 'DELETE',
  RPC_CALL = 'RPC_CALL',
  READ_CREDENTIALS = 'READ_CREDENTIALS',
  WRITE_CREDENTIALS = 'WRITE_CREDENTIALS',
  READ_ATTRIBUTES = 'READ_ATTRIBUTES',
  WRITE_ATTRIBUTES = 'WRITE_ATTRIBUTES',
  READ_TELEMETRY = 'READ_TELEMETRY',
  CLAIM_DEVICES = 'CLAIM_DEVICES'
}

export const resourceTranslationMap = new Map<Resource, string>([
  [Resource.ALL, 'role.resource.all'],
  [Resource.DEVICE, 'role.resource.device'],
  [Resource.ASSET, 'role.resource.asset'],
  [Resource.DASHBOARD, 'role.resource.dashboard'],
  [Resource.USER, 'role.resource.user'],
  [Resource.CUSTOMER, 'role.resource.customer'],
  [Resource.ALARM, 'role.resource.alarm'],
  [Resource.RULE_CHAIN, 'role.resource.rule-chain'],
  [Resource.ENTITY_VIEW, 'role.resource.entity-view'],
  [Resource.WIDGET_TYPE, 'role.resource.widget-type'],
  [Resource.WIDGETS_BUNDLE, 'role.resource.widgets-bundle'],
  [Resource.TENANT, 'role.resource.tenant'],
  [Resource.TENANT_PROFILE, 'role.resource.tenant-profile'],
  [Resource.DEVICE_PROFILE, 'role.resource.device-profile'],
  [Resource.ASSET_PROFILE, 'role.resource.asset-profile'],
  [Resource.TB_RESOURCE, 'role.resource.tb-resource'],
  [Resource.OTA_PACKAGE, 'role.resource.ota-package'],
  [Resource.EDGE, 'role.resource.edge'],
  [Resource.RPC, 'role.resource.rpc'],
  [Resource.QUEUE, 'role.resource.queue'],
  [Resource.NOTIFICATION, 'role.resource.notification'],
  [Resource.NOTIFICATION_TARGET, 'role.resource.notification-target'],
  [Resource.NOTIFICATION_TEMPLATE, 'role.resource.notification-template'],
  [Resource.NOTIFICATION_RULE, 'role.resource.notification-rule'],
  [Resource.OAUTH2_CLIENT, 'role.resource.oauth2-client'],
  [Resource.DOMAIN, 'role.resource.domain'],
  [Resource.MOBILE_APP, 'role.resource.mobile-app'],
  [Resource.ADMIN_SETTINGS, 'role.resource.admin-settings'],
  [Resource.AI_MODEL, 'role.resource.ai-model'],
  [Resource.API_KEY, 'role.resource.api-key'],
  [Resource.ROLE, 'role.resource.role']
]);

export const operationTranslationMap = new Map<Operation, string>([
  [Operation.ALL, 'role.operation.all'],
  [Operation.CREATE, 'role.operation.create'],
  [Operation.READ, 'role.operation.read'],
  [Operation.WRITE, 'role.operation.write'],
  [Operation.DELETE, 'role.operation.delete'],
  [Operation.RPC_CALL, 'role.operation.rpc-call'],
  [Operation.READ_CREDENTIALS, 'role.operation.read-credentials'],
  [Operation.WRITE_CREDENTIALS, 'role.operation.write-credentials'],
  [Operation.READ_ATTRIBUTES, 'role.operation.read-attributes'],
  [Operation.WRITE_ATTRIBUTES, 'role.operation.write-attributes'],
  [Operation.READ_TELEMETRY, 'role.operation.read-telemetry'],
  [Operation.CLAIM_DEVICES, 'role.operation.claim-devices']
]);
