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

/**
 * Reservoir Module (rv-module) TypeScript Models
 * Corresponds to backend DTOs in org.thingsboard.nexus.rv.dto
 */

// ==============================================
// BASE INTERFACE
// ==============================================

export interface RvBaseEntity {
  id?: string;         // Alias for assetId for convenience
  assetId?: string;
  tenantId?: string;
  name: string;
  label?: string;
  createdTime?: number;
  updatedTime?: number;
}

// ==============================================
// BASIN (Cuenca)
// ==============================================

export interface RvBasin extends RvBaseEntity {
  code?: string;
  basinType?: string;  // FORELAND, RIFT, PASSIVE_MARGIN, BACK_ARC, INTRACRATONIC
  country?: string;
  state?: string;
  areaKm2?: number;
  geologicAge?: string;
  tectonicSetting?: string;
  petroleumSystem?: string;
  additionalInfo?: any;
}

// ==============================================
// FIELD (Campo)
// ==============================================

export interface RvField extends RvBaseEntity {
  basinAssetId?: string;
  code?: string;
  operatorName?: string;
  discoveryDate?: number;
  productionStartDate?: number;
  fieldStatus?: string;  // EXPLORATION, DEVELOPMENT, PRODUCING, MATURE, ABANDONED
  fieldType?: string;    // OIL, GAS, CONDENSATE, MIXED
  onshoreOffshore?: string;  // ONSHORE, OFFSHORE, MIXED
  areaKm2?: number;
  centerLatitude?: number;
  centerLongitude?: number;
  totalWells?: number;
  producingWells?: number;
  injectionWells?: number;
  abandonedWells?: number;
  ooipMmbbl?: number;
  ogipBcf?: number;
  recoveryFactorPercent?: number;
  cumulativeOilProductionMmbbl?: number;
  cumulativeGasProductionBcf?: number;
  currentOilRateBopd?: number;
  currentGasRateMmscfd?: number;
  additionalInfo?: any;
}

// ==============================================
// RESERVOIR (Yacimiento)
// ==============================================

export interface RvReservoir extends RvBaseEntity {
  fieldAssetId?: string;
  code?: string;
  formationName?: string;
  lithology?: string;        // SANDSTONE, LIMESTONE, DOLOMITE, SHALE, CARBONATE, MIXED
  reservoirType?: string;    // CONVENTIONAL, UNCONVENTIONAL, TIGHT, SHALE, HEAVY_OIL, FOAMY
  driveType?: string;        // WATER_DRIVE, GAS_CAP, SOLUTION_GAS, GRAVITY, COMBINATION
  depositionalEnv?: string;
  geologicAge?: string;
  structureType?: string;    // ANTICLINAL, FAULTED, STRATIGRAPHIC, COMBINATION
  trapType?: string;
  areaAcres?: number;
  grossThicknessM?: number;
  netPayThicknessM?: number;
  avgPorosityFrac?: number;
  avgPermeabilityMd?: number;
  avgWaterSatFrac?: number;
  initialReservoirPressurePsi?: number;
  currentReservoirPressurePsi?: number;
  reservoirTemperatureF?: number;
  bubblePointPressurePsi?: number;
  oilFvfRbStb?: number;
  oilViscosityCp?: number;
  apiGravity?: number;
  gasOilRatioScfStb?: number;
  ooipMmbbl?: number;
  originalReservesMmbbl?: number;
  remainingReservesMmbbl?: number;
  recoveryFactorPercent?: number;
  cumulativeProductionMmbbl?: number;
  hasFoamyOil?: boolean;
  foamyCriticalGasSaturation?: number;
  requiresDiluent?: boolean;
  diluentType?: string;
  additionalInfo?: any;
}

// ==============================================
// ZONE (Zona/Formacion)
// ==============================================

export interface RvZone extends RvBaseEntity {
  reservoirAssetId?: string;
  code?: string;
  zoneNumber?: number;
  topDepthMdM?: number;
  bottomDepthMdM?: number;
  topDepthTvdM?: number;
  bottomDepthTvdM?: number;
  grossThicknessM?: number;
  netPayThicknessM?: number;
  netToGrossRatio?: number;
  porosityFrac?: number;
  permeabilityMd?: number;
  waterSaturationFrac?: number;
  shaleFrac?: number;
  lithology?: string;
  fluidType?: string;     // OIL, GAS, WATER, OIL_GAS
  zoneStatus?: string;    // PRODUCTIVE, DEPLETED, WATER_OUT, BEHIND_PIPE
  isPerforated?: boolean;
  additionalInfo?: any;
}

// ==============================================
// WELL (Pozo)
// ==============================================

export interface RvWell extends RvBaseEntity {
  reservoirAssetId?: string;
  fieldAssetId?: string;
  uwi?: string;
  apiNumber?: string;
  wellType?: string;           // PRODUCER, INJECTOR, OBSERVATION, DISPOSAL
  wellStatus?: string;         // DRILLING, COMPLETING, PRODUCING, SHUT_IN, ABANDONED
  wellCategory?: string;       // VERTICAL, DEVIATED, HORIZONTAL, MULTILATERAL
  operatorName?: string;
  spudDate?: number;
  completionDate?: number;
  firstProductionDate?: number;
  surfaceLatitude?: number;
  surfaceLongitude?: number;
  bottomholeLatitude?: number;
  bottomholeLongitude?: number;
  totalDepthMdM?: number;
  totalDepthTvdM?: number;
  kickoffPointM?: number;
  casingDiameterIn?: number;
  tubingDiameterIn?: number;
  currentPressurePsi?: number;
  currentRateBopd?: number;
  currentGasRateMscfd?: number;
  currentWaterCutPercent?: number;
  cumulativeOilBbl?: number;
  cumulativeGasMcf?: number;
  cumulativeWaterBbl?: number;
  productivityIndexBpdPsi?: number;
  skinFactor?: number;
  artificialLiftType?: string;
  drillingJobAssetId?: string;    // Integration with CT module
  productionUnitAssetId?: string; // Integration with Production module
  additionalInfo?: any;
}

// ==============================================
// COMPLETION (Completacion)
// ==============================================

export interface RvCompletion extends RvBaseEntity {
  wellAssetId?: string;
  zoneAssetId?: string;
  completionNumber?: number;
  completionType?: string;   // OPENHOLE, CASED_PERFORATED, GRAVEL_PACK, FRAC_PACK, SLOTTED_LINER
  completionDate?: number;
  completionStatus?: string; // ACTIVE, INACTIVE, ISOLATED, ABANDONED
  topPerforationMdM?: number;
  bottomPerforationMdM?: number;
  perforationIntervalM?: number;
  perforationDensitySpf?: number;
  shotPhasing?: number;
  perforationDiameterIn?: number;
  totalShots?: number;
  openShots?: number;
  liftMethod?: string;       // NATURAL, ESP, SRP, GAS_LIFT, JET_PUMP, PCP
  liftDepthM?: number;
  liftCapacityBopd?: number;
  lastStimulationType?: string;
  lastStimulationDate?: number;
  skinBeforeStim?: number;
  skinAfterStim?: number;
  currentRateBopd?: number;
  currentPiPbdPsi?: number;
  additionalInfo?: any;
}

// ==============================================
// PVT STUDY (Estudio PVT)
// ==============================================

export interface RvPvtStudy extends RvBaseEntity {
  reservoirAssetId?: string;
  wellAssetId?: string;
  studyCode?: string;
  sampleDate?: number;
  laboratoryName?: string;
  sampleType?: string;       // BOTTOMHOLE, RECOMBINED, SEPARATOR
  sampleDepthM?: number;
  samplePressurePsi?: number;
  sampleTemperatureF?: number;
  apiGravity?: number;
  specificGravityOil?: number;
  stockTankOilViscosityCp?: number;
  bubblePointPressurePsi?: number;
  bubblePointPressurePsia?: number;  // Alias for bubblePointPressurePsi
  initialPressurePsia?: number;
  reservoirTemperatureF?: number;
  solutionGorAtPbScfStb?: number;
  solutionGorScfStb?: number;        // Alias for solutionGorAtPbScfStb
  oilFvfAtPbRbStb?: number;
  boAtPbRbStb?: number;              // Alias for oilFvfAtPbRbStb
  bobRbStb?: number;                 // Oil FVF at bubble point
  oilViscosityAtPbCp?: number;
  deadOilViscosityCp?: number;
  gasSpecificGravity?: number;
  gasGravity?: number;               // Alias for gasSpecificGravity
  gasZFactorAtPb?: number;
  oilCompressibility?: number;
  waterSalinity?: number;
  waterFvfAtReservoirRbStb?: number;
  usesCorrelations?: boolean;
  pbCorrelation?: string;     // STANDING, VASQUEZ_BEGGS, GLASO
  boCorrelation?: string;
  viscosityCorrelation?: string;
  hasFoamyBehavior?: boolean;
  pseudoBubblePointPsi?: number;
  pseudoBubblePoint?: number;        // Alias for pseudoBubblePointPsi
  foamyOilFactor?: number;
  foamCriticalGasSaturation?: number;
  differentialLiberationData?: any;
  separatorTestData?: any;
  viscosityData?: any;
  additionalInfo?: any;
}

// ==============================================
// IPR MODEL (Modelo IPR)
// ==============================================

export interface RvIprModel extends RvBaseEntity {
  wellAssetId?: string;
  completionAssetId?: string;
  modelCode?: string;
  analysisDate?: number;
  iprMethod?: string;         // VOGEL, DARCY, FETKOVICH, JONES, COMPOSITE
  reservoirPressurePsi?: number;
  bubblePointPressurePsi?: number;
  reservoirTemperatureF?: number;
  isBelowBubblePoint?: boolean;
  testRateBopd?: number;
  testPwfPsi?: number;
  testDate?: number;
  testType?: string;          // PRODUCTION_TEST, BUILDUP, DST
  productivityIndexBpdPsi?: number;
  productivityIndexAbovePb?: number;
  productivityIndexBelowPb?: number;
  qmaxBopd?: number;
  vogelCoefficient?: number;
  fetkovichC?: number;
  fetkovichN?: number;
  jonesA?: number;
  jonesB?: number;
  iprCurveData?: any;
  currentPwfPsi?: number;
  currentRateBopd?: number;
  operatingEfficiencyPercent?: number;
  skinFactor?: number;
  damageRatio?: number;
  flowEfficiency?: number;
  idealQmaxBopd?: number;
  r2Coefficient?: number;
  modelQuality?: string;      // EXCELLENT, GOOD, FAIR, POOR
  additionalInfo?: any;
}

// ==============================================
// DECLINE ANALYSIS (Analisis de Declinacion)
// ==============================================

export interface RvDeclineAnalysis extends RvBaseEntity {
  wellAssetId?: string;
  reservoirAssetId?: string;
  analysisCode?: string;
  analysisDate?: number;
  dataStartDate?: number;
  dataEndDate?: number;
  declineType?: string;       // EXPONENTIAL, HYPERBOLIC, HARMONIC
  declinePhase?: string;      // PRIMARY, SECONDARY, TERTIARY
  qiBopd?: number;
  diPerYear?: number;
  diPerMonth?: number;
  bExponent?: number;
  dMinPerYear?: number;
  transitionTime?: number;
  eurBbl?: number;
  remainingReservesBbl?: number;
  cumulativeProductionBbl?: number;
  recoveryFactorPercent?: number;
  forecastEndDate?: number;
  economicLimitBopd?: number;
  forecastEurBbl?: number;
  remainingLifeMonths?: number;
  rate1YearBopd?: number;
  rate3YearsBopd?: number;
  rate5YearsBopd?: number;
  rate10YearsBopd?: number;
  cumulative1YearBbl?: number;
  cumulative3YearsBbl?: number;
  cumulative5YearsBbl?: number;
  r2Coefficient?: number;
  standardError?: number;
  fitQuality?: string;        // EXCELLENT, GOOD, FAIR, POOR
  dataPointsUsed?: number;
  historicalData?: any;
  forecastData?: any;
  eurExponentialBbl?: number;
  eurHyperbolicBbl?: number;
  eurHarmonicBbl?: number;
  additionalInfo?: any;
}

// ==============================================
// ENUMS FOR UI
// ==============================================

export enum RvBasinType {
  FORELAND = 'FORELAND',
  RIFT = 'RIFT',
  PASSIVE_MARGIN = 'PASSIVE_MARGIN',
  BACK_ARC = 'BACK_ARC',
  INTRACRATONIC = 'INTRACRATONIC'
}

export enum RvFieldStatus {
  EXPLORATION = 'EXPLORATION',
  DEVELOPMENT = 'DEVELOPMENT',
  PRODUCING = 'PRODUCING',
  MATURE = 'MATURE',
  ABANDONED = 'ABANDONED'
}

export enum RvFieldType {
  OIL = 'OIL',
  GAS = 'GAS',
  CONDENSATE = 'CONDENSATE',
  MIXED = 'MIXED'
}

export enum RvLithology {
  SANDSTONE = 'SANDSTONE',
  LIMESTONE = 'LIMESTONE',
  DOLOMITE = 'DOLOMITE',
  SHALE = 'SHALE',
  CARBONATE = 'CARBONATE',
  MIXED = 'MIXED'
}

export enum RvReservoirType {
  CONVENTIONAL = 'CONVENTIONAL',
  UNCONVENTIONAL = 'UNCONVENTIONAL',
  TIGHT = 'TIGHT',
  SHALE = 'SHALE',
  HEAVY_OIL = 'HEAVY_OIL',
  FOAMY = 'FOAMY'
}

export enum RvDriveType {
  WATER_DRIVE = 'WATER_DRIVE',
  GAS_CAP = 'GAS_CAP',
  SOLUTION_GAS = 'SOLUTION_GAS',
  GRAVITY = 'GRAVITY',
  COMBINATION = 'COMBINATION'
}

export enum RvWellType {
  PRODUCER = 'PRODUCER',
  INJECTOR = 'INJECTOR',
  OBSERVATION = 'OBSERVATION',
  DISPOSAL = 'DISPOSAL'
}

export enum RvWellStatus {
  DRILLING = 'DRILLING',
  COMPLETING = 'COMPLETING',
  PRODUCING = 'PRODUCING',
  SHUT_IN = 'SHUT_IN',
  ABANDONED = 'ABANDONED'
}

export enum RvWellCategory {
  VERTICAL = 'VERTICAL',
  DEVIATED = 'DEVIATED',
  HORIZONTAL = 'HORIZONTAL',
  MULTILATERAL = 'MULTILATERAL'
}

export enum RvCompletionType {
  OPENHOLE = 'OPENHOLE',
  CASED_PERFORATED = 'CASED_PERFORATED',
  GRAVEL_PACK = 'GRAVEL_PACK',
  FRAC_PACK = 'FRAC_PACK',
  SLOTTED_LINER = 'SLOTTED_LINER'
}

export enum RvLiftMethod {
  NATURAL = 'NATURAL',
  ESP = 'ESP',
  SRP = 'SRP',
  GAS_LIFT = 'GAS_LIFT',
  JET_PUMP = 'JET_PUMP',
  PCP = 'PCP'
}

export enum RvIprMethod {
  VOGEL = 'VOGEL',
  DARCY = 'DARCY',
  FETKOVICH = 'FETKOVICH',
  JONES = 'JONES',
  COMPOSITE = 'COMPOSITE'
}

export enum RvDeclineType {
  EXPONENTIAL = 'EXPONENTIAL',
  HYPERBOLIC = 'HYPERBOLIC',
  HARMONIC = 'HARMONIC'
}

export enum RvPvtCorrelation {
  STANDING = 'STANDING',
  VASQUEZ_BEGGS = 'VASQUEZ_BEGGS',
  GLASO = 'GLASO',
  BEGGS_ROBINSON = 'BEGGS_ROBINSON'
}

// ==============================================
// HELPER FUNCTIONS
// ==============================================

export function getFieldStatusColor(status: string): string {
  switch (status) {
    case RvFieldStatus.PRODUCING: return 'primary';
    case RvFieldStatus.DEVELOPMENT: return 'accent';
    case RvFieldStatus.EXPLORATION: return 'warn';
    case RvFieldStatus.MATURE: return '';
    case RvFieldStatus.ABANDONED: return '';
    default: return '';
  }
}

export function getWellStatusIcon(status: string): string {
  switch (status) {
    case RvWellStatus.PRODUCING: return 'play_circle';
    case RvWellStatus.DRILLING: return 'build';
    case RvWellStatus.COMPLETING: return 'engineering';
    case RvWellStatus.SHUT_IN: return 'pause_circle';
    case RvWellStatus.ABANDONED: return 'cancel';
    default: return 'help';
  }
}

export function formatOilRate(rateBopd: number): string {
  if (rateBopd >= 1000) {
    return (rateBopd / 1000).toFixed(1) + ' Mbopd';
  }
  return rateBopd.toFixed(0) + ' bopd';
}

export function formatVolume(volumeBbl: number): string {
  if (volumeBbl >= 1000000) {
    return (volumeBbl / 1000000).toFixed(2) + ' MMbbl';
  } else if (volumeBbl >= 1000) {
    return (volumeBbl / 1000).toFixed(1) + ' Mbbl';
  }
  return volumeBbl.toFixed(0) + ' bbl';
}
