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
import { HasUUID } from '@shared/models/id/has-uuid';

/**
 * Transformation types for mapping rules
 */
export enum TransformationType {
  DIRECT = 'DIRECT',
  SCALE = 'SCALE',
  FORMULA = 'FORMULA',
  LOOKUP = 'LOOKUP',
  UNIT_CONVERSION = 'UNIT_CONVERSION'
}

/**
 * Distribution modes for data source configs
 */
export enum DistributionMode {
  DIRECT = 'DIRECT',
  MAPPED = 'MAPPED'
}

/**
 * Transformation type display names
 */
export const TransformationTypeTranslationMap = new Map<TransformationType, string>([
  [TransformationType.DIRECT, 'data-mapping.transformation-type.direct'],
  [TransformationType.SCALE, 'data-mapping.transformation-type.scale'],
  [TransformationType.FORMULA, 'data-mapping.transformation-type.formula'],
  [TransformationType.LOOKUP, 'data-mapping.transformation-type.lookup'],
  [TransformationType.UNIT_CONVERSION, 'data-mapping.transformation-type.unit-conversion']
]);

/**
 * Distribution mode display names
 */
export const DistributionModeTranslationMap = new Map<DistributionMode, string>([
  [DistributionMode.DIRECT, 'data-mapping.distribution-mode.direct'],
  [DistributionMode.MAPPED, 'data-mapping.distribution-mode.mapped']
]);

/**
 * Mapping Template - Reusable template for data mapping configurations
 */
export interface MappingTemplate extends BaseData<HasUUID> {
  tenantId: string;
  moduleKey: string;
  name: string;
  description?: string;
  targetAssetType?: string;
  distributionMode: DistributionMode;
  isDefault: boolean;
  active: boolean;
  additionalInfo?: any;
  createdTime: number;
}

/**
 * Mapping Template Rule - Individual mapping rule within a template
 */
export interface MappingTemplateRule extends BaseData<HasUUID> {
  templateId: HasUUID;
  sourceKey: string;
  targetKey: string;
  transformationType: TransformationType;
  transformationConfig?: any;
  unitSource?: string;
  unitTarget?: string;
  description?: string;
  priority: number;
  active: boolean;
  createdTime: number;
}

/**
 * Data Source Config - Applied mapping configuration for a Device→Asset pair
 */
export interface DataSourceConfig extends BaseData<HasUUID> {
  tenantId: string;
  deviceId: HasUUID;
  targetAssetId: HasUUID;
  moduleKey: string;
  targetAssetType?: string;
  distributionMode: DistributionMode;
  active: boolean;
  additionalInfo?: any;
  createdTime: number;
}

/**
 * Data Mapping Rule - Applied rule for a Data Source Config
 */
export interface DataMappingRule extends BaseData<HasUUID> {
  dataSourceConfigId: HasUUID;
  sourceKey: string;
  targetKey: string;
  transformationType: TransformationType;
  transformationConfig?: any;
  unitSource?: string;
  unitTarget?: string;
  description?: string;
  priority: number;
  active: boolean;
  createdTime: number;
}

/**
 * Request to create a new mapping template
 */
export interface CreateMappingTemplateRequest {
  moduleKey: string;
  name: string;
  description?: string;
  targetAssetType?: string;
  distributionMode?: DistributionMode;
  isDefault?: boolean;
  active?: boolean;
}

/**
 * Request to update a mapping template
 */
export interface UpdateMappingTemplateRequest {
  name?: string;
  description?: string;
  targetAssetType?: string;
  distributionMode?: DistributionMode;
  isDefault?: boolean;
  active?: boolean;
}

/**
 * Request to create a mapping template rule
 */
export interface CreateMappingTemplateRuleRequest {
  templateId: string;
  sourceKey: string;
  targetKey: string;
  transformationType?: TransformationType;
  transformationConfig?: any;
  unitSource?: string;
  unitTarget?: string;
  description?: string;
  priority?: number;
  active?: boolean;
}

/**
 * Request to apply a template to Device→Asset
 */
export interface ApplyTemplateRequest {
  templateId: string;
  deviceId: string;
  targetAssetId: string;
}

/**
 * Transformation config for SCALE type
 */
export interface ScaleTransformationConfig {
  factor: number;
  offset?: number;
}

/**
 * Transformation config for FORMULA type
 */
export interface FormulaTransformationConfig {
  formula: string;
  variables?: string[];
}

/**
 * Transformation config for LOOKUP type
 */
export interface LookupTransformationConfig {
  lookupTable: { [key: string]: any };
  defaultValue?: any;
}

/**
 * Transformation config for UNIT_CONVERSION type
 */
export interface UnitConversionTransformationConfig {
  fromUnit: string;
  toUnit: string;
  conversionFactor?: number;
}

/**
 * Extended mapping template with rule count
 */
export interface MappingTemplateInfo extends MappingTemplate {
  ruleCount?: number;
}

/**
 * Extended data source config with device and asset names
 */
export interface DataSourceConfigInfo extends DataSourceConfig {
  deviceName?: string;
  assetName?: string;
  templateName?: string;
}

/**
 * Module keys for data mapping
 */
export type DataMappingModuleKey = 'CT' | 'DR' | 'RV' | string;

/**
 * Common telemetry keys per module
 */
export const CommonTelemetryKeys: { [module: string]: string[] } = {
  CT: [
    'hydraulic.pressure',
    'hydraulic.temperature',
    'hydraulic.flowRate',
    'injector.speed',
    'injector.tension',
    'injector.depth',
    'reel.pressure',
    'reel.tension',
    'powerPack.engineRpm',
    'powerPack.fuelLevel'
  ],
  DR: [
    'drilling.wobKlbs',
    'drilling.rpm',
    'drilling.torqueFtLbs',
    'drilling.ropFtHr',
    'hydraulics.sppPsi',
    'hydraulics.flowRateGpm',
    'hydraulics.mudWeightPpg',
    'directional.inclinationDeg',
    'directional.azimuthDeg'
  ],
  RV: [
    'production.oilRateBopd',
    'production.gasRateMscfd',
    'production.waterRateBwpd',
    'production.waterCutPct',
    'production.gorScfStb',
    'pressures.thpPsi',
    'pressures.chpPsi',
    'pressures.flowingBhpPsi'
  ]
};
