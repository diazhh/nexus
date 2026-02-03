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

import { Injectable, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import {
  RvBasin,
  RvField,
  RvReservoir,
  RvWell,
  RvZone,
  RvCompletion,
  RvPvtStudy,
  RvIprModel,
  RvDeclineAnalysis,
  RvWellLog,
  RvMaterialBalance,
  RvCore,
  RvFault,
  RvSeismicSurvey
} from '@shared/models/rv/rv.models';

/**
 * Service for exporting RV module data to CSV format.
 */
@Injectable({
  providedIn: 'root'
})
export class RvExportService {

  constructor(
    @Inject(DOCUMENT) private document: Document
  ) {}

  // ================================
  // CSV Export Methods
  // ================================

  exportZonesToCsv(zones: RvZone[], filename = 'rv_zones'): void {
    const headers = [
      'Nombre', 'Codigo', 'Yacimiento', 'Prof. Tope MD (m)', 'Prof. Base MD (m)',
      'Espesor Bruto (m)', 'Espesor Neto (m)', 'NTG', 'Porosidad (%)',
      'Permeabilidad (mD)', 'Sw (%)', 'Estado', 'Litologia'
    ];

    const rows = zones.map(z => [
      z.name || '',
      z.code || '',
      z.reservoirAssetId || '',
      z.topDepthMdM?.toFixed(2) || '',
      z.bottomDepthMdM?.toFixed(2) || '',
      z.grossThicknessM?.toFixed(2) || '',
      z.netPayThicknessM?.toFixed(2) || '',
      z.netToGrossRatio?.toFixed(3) || '',
      z.porosityFrac ? (z.porosityFrac * 100).toFixed(1) : '',
      z.permeabilityMd?.toFixed(2) || '',
      z.waterSaturationFrac ? (z.waterSaturationFrac * 100).toFixed(1) : '',
      z.zoneStatus || '',
      z.lithology || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportReservoirsToCsv(reservoirs: RvReservoir[], filename = 'rv_reservoirs'): void {
    const headers = [
      'Nombre', 'Codigo', 'Campo', 'Formacion', 'Tipo Yacimiento',
      'Porosidad Prom. (%)', 'Permeabilidad Prom. (mD)', 'Espesor Neto (m)',
      'Presion Inicial (psi)', 'Temperatura (F)', 'API', 'OOIP (MMbbl)', 'Litologia'
    ];

    const rows = reservoirs.map(r => [
      r.name || '',
      r.code || '',
      r.fieldAssetId || '',
      r.formationName || '',
      r.reservoirType || '',
      r.avgPorosityFrac ? (r.avgPorosityFrac * 100).toFixed(1) : '',
      r.avgPermeabilityMd?.toFixed(2) || '',
      r.netPayThicknessM?.toFixed(2) || '',
      r.initialReservoirPressurePsi?.toFixed(0) || '',
      r.reservoirTemperatureF?.toFixed(1) || '',
      r.apiGravity?.toFixed(1) || '',
      r.ooipMmbbl?.toFixed(2) || '',
      r.lithology || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportWellsToCsv(wells: RvWell[], filename = 'rv_wells'): void {
    const headers = [
      'Nombre', 'UWI', 'API Number', 'Campo', 'Yacimiento', 'Tipo',
      'Estado', 'Categoria', 'Profundidad MD (m)', 'Profundidad TVD (m)',
      'Latitud', 'Longitud', 'Fecha Spud', 'Levantamiento Artificial'
    ];

    const rows = wells.map(w => [
      w.name || '',
      w.uwi || '',
      w.apiNumber || '',
      w.fieldAssetId || '',
      w.reservoirAssetId || '',
      w.wellType || '',
      w.wellStatus || '',
      w.wellCategory || '',
      w.totalDepthMdM?.toFixed(2) || '',
      w.totalDepthTvdM?.toFixed(2) || '',
      w.surfaceLatitude?.toFixed(6) || '',
      w.surfaceLongitude?.toFixed(6) || '',
      w.spudDate ? new Date(w.spudDate).toISOString().split('T')[0] : '',
      w.artificialLiftType || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportCompletionsToCsv(completions: RvCompletion[], filename = 'rv_completions'): void {
    const headers = [
      'Nombre', 'Numero', 'Pozo', 'Zona', 'Tipo', 'Estado',
      'Prof. Tope Perf (m)', 'Prof. Base Perf (m)', 'Fecha Completacion',
      'Metodo Levantamiento', 'Densidad Disparo (spf)'
    ];

    const rows = completions.map(c => [
      c.name || '',
      c.completionNumber?.toString() || '',
      c.wellAssetId || '',
      c.zoneAssetId || '',
      c.completionType || '',
      c.completionStatus || '',
      c.topPerforationMdM?.toFixed(2) || '',
      c.bottomPerforationMdM?.toFixed(2) || '',
      c.completionDate ? new Date(c.completionDate).toISOString().split('T')[0] : '',
      c.liftMethod || '',
      c.perforationDensitySpf?.toFixed(1) || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportPvtStudiesToCsv(studies: RvPvtStudy[], filename = 'rv_pvt_studies'): void {
    const headers = [
      'Nombre', 'Codigo', 'Yacimiento', 'Fecha Muestra', 'Tipo Muestra',
      'Presion Burbuja (psi)', 'Rs (scf/stb)', 'Bo @ Pb', 'API',
      'Viscosidad @ Pb (cp)', 'Compresibilidad (1/psi)'
    ];

    const rows = studies.map(s => [
      s.name || '',
      s.studyCode || '',
      s.reservoirAssetId || '',
      s.sampleDate ? new Date(s.sampleDate).toISOString().split('T')[0] : '',
      s.sampleType || '',
      s.bubblePointPressurePsi?.toFixed(0) || '',
      s.solutionGorAtPbScfStb?.toFixed(0) || '',
      s.oilFvfAtPbRbStb?.toFixed(4) || '',
      s.apiGravity?.toFixed(1) || '',
      s.oilViscosityAtPbCp?.toFixed(3) || '',
      s.oilCompressibility?.toExponential(3) || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportDeclineAnalysesToCsv(analyses: RvDeclineAnalysis[], filename = 'rv_decline_analyses'): void {
    const headers = [
      'Nombre', 'Codigo', 'Pozo', 'Yacimiento', 'Tipo Declinacion',
      'Qi (bopd)', 'Di (%/year)', 'b', 'EUR (bbl)', 'R2', 'Calidad Ajuste'
    ];

    const rows = analyses.map(a => [
      a.name || '',
      a.analysisCode || '',
      a.wellAssetId || '',
      a.reservoirAssetId || '',
      a.declineType || '',
      a.qiBopd?.toFixed(1) || '',
      a.diPerYear?.toFixed(2) || '',
      a.bExponent?.toFixed(3) || '',
      a.eurBbl?.toFixed(0) || '',
      a.r2Coefficient?.toFixed(4) || '',
      a.fitQuality || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportIprModelsToCsv(models: RvIprModel[], filename = 'rv_ipr_models'): void {
    const headers = [
      'Nombre', 'Codigo', 'Pozo', 'Metodo', 'Presion Yacimiento (psi)',
      'Presion Burbuja (psi)', 'Qmax (bopd)', 'PI (bpd/psi)',
      'Calidad Modelo', 'Fecha Test'
    ];

    const rows = models.map(m => [
      m.name || '',
      m.modelCode || '',
      m.wellAssetId || '',
      m.iprMethod || '',
      m.reservoirPressurePsi?.toFixed(0) || '',
      m.bubblePointPressurePsi?.toFixed(0) || '',
      m.qmaxBopd?.toFixed(1) || '',
      m.productivityIndexBpdPsi?.toFixed(3) || '',
      m.modelQuality || '',
      m.testDate ? new Date(m.testDate).toISOString().split('T')[0] : ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportWellLogsToCsv(logs: RvWellLog[], filename = 'rv_well_logs'): void {
    const headers = [
      'Nombre', 'Run', 'Pozo', 'Tipo Registro', 'Compania',
      'Fecha', 'Prof. Tope (m)', 'Prof. Base (m)', 'Curvas Disponibles'
    ];

    const rows = logs.map(l => [
      l.name || '',
      l.runNumber?.toString() || '',
      l.wellAssetId || '',
      l.logType || '',
      l.serviceCompany || '',
      l.logDate ? new Date(l.logDate).toISOString().split('T')[0] : '',
      l.topDepthMdM?.toFixed(2) || '',
      l.bottomDepthMdM?.toFixed(2) || '',
      l.availableCurves?.join(', ') || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportMaterialBalancesToCsv(studies: RvMaterialBalance[], filename = 'rv_material_balances'): void {
    const headers = [
      'Nombre', 'Codigo', 'Yacimiento', 'Metodo', 'OOIP Calculado (STB)',
      'Mecanismo Empuje', 'Pi (psi)', 'Pb (psi)', 'Ti (F)',
      'Boi', 'Swi (%)', 'R2', 'Calidad'
    ];

    const rows = studies.map(s => [
      s.name || '',
      s.studyCode || '',
      s.reservoirAssetId || '',
      s.analysisMethod || '',
      s.calculatedOoipStb?.toFixed(0) || '',
      s.driveMechanism || '',
      s.initialPressurePsi?.toFixed(0) || '',
      s.bubblePointPressurePsi?.toFixed(0) || '',
      s.initialTemperatureF?.toFixed(1) || '',
      s.initialOilFvfRbStb?.toFixed(4) || '',
      s.initialWaterSatFrac ? (s.initialWaterSatFrac * 100).toFixed(1) : '',
      s.r2Coefficient?.toFixed(4) || '',
      s.studyQuality || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportCoresToCsv(cores: RvCore[], filename = 'rv_cores'): void {
    const headers = [
      'Nombre', 'Codigo', 'Pozo', 'Tipo', 'Fecha',
      'Prof. Tope (m)', 'Prof. Base (m)', 'Recuperacion (%)',
      'Porosidad (%)', 'Permeabilidad (mD)', 'Litologia'
    ];

    const rows = cores.map(c => [
      c.name || '',
      c.coreCode || '',
      c.wellAssetId || '',
      c.coreType || '',
      c.coreDate ? new Date(c.coreDate).toISOString().split('T')[0] : '',
      c.topDepthMdM?.toFixed(2) || '',
      c.bottomDepthMdM?.toFixed(2) || '',
      c.recoveryPercent?.toFixed(1) || '',
      c.porosityFrac ? (c.porosityFrac * 100).toFixed(1) : '',
      c.permeabilityMd?.toFixed(2) || '',
      c.lithologyDescription || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportFaultsToCsv(faults: RvFault[], filename = 'rv_faults'): void {
    const headers = [
      'Nombre', 'Codigo', 'Campo', 'Tipo', 'Rumbo (deg)',
      'Buzamiento (deg)', 'Longitud (m)', 'Salto (m)', 'Sellante'
    ];

    const rows = faults.map(f => [
      f.name || '',
      f.faultCode || '',
      f.fieldAssetId || '',
      f.faultType || '',
      f.strikeDeg?.toFixed(1) || '',
      f.dipDeg?.toFixed(1) || '',
      f.lengthM?.toFixed(0) || '',
      f.throwM?.toFixed(1) || '',
      f.isSealing ? 'Si' : 'No'
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportSeismicSurveysToCsv(surveys: RvSeismicSurvey[], filename = 'rv_seismic_surveys'): void {
    const headers = [
      'Nombre', 'Codigo', 'Campo', 'Tipo', 'Compania',
      'Fecha Adquisicion', 'Area (km2)', 'Longitud Linea (km)',
      'Interpretado'
    ];

    const rows = surveys.map(s => [
      s.name || '',
      s.surveyCode || '',
      s.fieldAssetId || '',
      s.surveyType || '',
      s.acquisitionCompany || '',
      s.acquisitionDate ? new Date(s.acquisitionDate).toISOString().split('T')[0] : '',
      s.areaCoveredKm2?.toFixed(2) || '',
      s.totalLineLengthKm?.toFixed(2) || '',
      s.isInterpreted ? 'Si' : 'No'
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportFieldsToCsv(fields: RvField[], filename = 'rv_fields'): void {
    const headers = [
      'Nombre', 'Codigo', 'Cuenca', 'Operador',
      'Area (km2)', 'Fecha Descubrimiento', 'Estado', 'Tipo'
    ];

    const rows = fields.map(f => [
      f.name || '',
      f.code || '',
      f.basinAssetId || '',
      f.operatorName || '',
      f.areaKm2?.toFixed(2) || '',
      f.discoveryDate ? new Date(f.discoveryDate).toISOString().split('T')[0] : '',
      f.fieldStatus || '',
      f.fieldType || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  exportBasinsToCsv(basins: RvBasin[], filename = 'rv_basins'): void {
    const headers = [
      'Nombre', 'Codigo', 'Pais', 'Estado/Provincia',
      'Area (km2)', 'Tipo', 'Edad Geologica', 'Sistema Petrolero'
    ];

    const rows = basins.map(b => [
      b.name || '',
      b.code || '',
      b.country || '',
      b.state || '',
      b.areaKm2?.toFixed(0) || '',
      b.basinType || '',
      b.geologicAge || '',
      b.petroleumSystem || ''
    ]);

    this.downloadCsv(headers, rows, filename);
  }

  // ================================
  // Generic Export Methods
  // ================================

  /**
   * Export any array of objects to CSV
   */
  exportToCsv<T>(data: T[], columns: {key: keyof T, header: string, formatter?: (val: any) => string}[], filename: string): void {
    const headers = columns.map(c => c.header);
    const rows = data.map(item =>
      columns.map(col => {
        const val = item[col.key];
        if (col.formatter) {
          return col.formatter(val);
        }
        return val !== null && val !== undefined ? String(val) : '';
      })
    );
    this.downloadCsv(headers, rows, filename);
  }

  // ================================
  // Private Methods
  // ================================

  private downloadCsv(headers: string[], rows: (string | number)[][], filename: string): void {
    const csvContent = this.generateCsvContent(headers, rows);
    this.downloadFile(csvContent, `${filename}.csv`, 'text/csv;charset=utf-8;');
  }

  private generateCsvContent(headers: string[], rows: (string | number)[][]): string {
    const processCell = (cell: string | number): string => {
      let cellStr = String(cell);
      // Escape double quotes
      cellStr = cellStr.replace(/"/g, '""');
      // Quote cells with special characters
      if (cellStr.search(/[",;\n]/g) >= 0) {
        cellStr = `"${cellStr}"`;
      }
      return cellStr;
    };

    const headerLine = headers.map(h => processCell(h)).join(';');
    const dataLines = rows.map(row => row.map(cell => processCell(cell)).join(';'));

    // Add BOM for Excel UTF-8 support
    return '\ufeff' + headerLine + '\n' + dataLines.join('\n');
  }

  private downloadFile(content: string, filename: string, mimeType: string): void {
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);

    const a = this.document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();

    setTimeout(() => URL.revokeObjectURL(url), 100);
  }
}
