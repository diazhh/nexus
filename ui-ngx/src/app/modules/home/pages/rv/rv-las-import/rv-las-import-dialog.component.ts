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

import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { RvLasParserService, LasFile, LasCurveInfo } from '@core/http/rv/rv-las-parser.service';
import { RvService } from '@core/http/rv/rv.service';
import { PageLink } from '@shared/models/page/page-link';
import { RvWell, RvWellLog } from '@shared/models/rv/rv.models';

export interface RvLasImportDialogData {
  tenantId: string;
}

export interface RvLasImportResult {
  wellLogRunId: string;
  curvesImported: number;
  dataPointsImported: number;
}

@Component({
  selector: 'tb-rv-las-import-dialog',
  templateUrl: './rv-las-import-dialog.component.html',
  styleUrls: ['./rv-las-import-dialog.component.scss']
})
export class RvLasImportDialogComponent implements OnInit {

  importForm: FormGroup;
  wells: RvWell[] = [];
  lasFile: LasFile | null = null;
  validationErrors: string[] = [];
  isLoading = false;
  step = 1; // 1: Select file, 2: Review & Configure, 3: Import

  // File info display
  fileName = '';
  fileSize = '';

  // Curve selection
  selectedCurves: Set<string> = new Set();

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<RvLasImportDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RvLasImportDialogData,
    private lasParser: RvLasParserService,
    private rvService: RvService
  ) {
    this.importForm = this.fb.group({
      wellId: ['', Validators.required],
      logRunName: [''],
      logType: ['COMPOSITE'],
      overwriteExisting: [false]
    });
  }

  ngOnInit(): void {
    this.loadWells();
  }

  loadWells(): void {
    this.rvService.getWells(this.data.tenantId, new PageLink(1000, 0)).subscribe({
      next: (result) => {
        this.wells = result.data;
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.fileName = file.name;
    this.fileSize = this.formatFileSize(file.size);

    const reader = new FileReader();
    reader.onload = () => {
      try {
        const content = reader.result as string;
        this.lasFile = this.lasParser.parseLasFile(content);

        // Validate the file
        const validation = this.lasParser.validateLasFile(this.lasFile);
        this.validationErrors = validation.errors;

        // Set default log run name from file
        if (this.lasFile.wellInfo.wellName) {
          this.importForm.patchValue({
            logRunName: `${this.lasFile.wellInfo.wellName} - ${this.fileName}`
          });
        } else {
          this.importForm.patchValue({
            logRunName: this.fileName.replace('.las', '').replace('.LAS', '')
          });
        }

        // Select all curves by default
        this.lasFile.curves.forEach(c => this.selectedCurves.add(c.mnemonic));

        if (validation.valid) {
          this.step = 2;
        }
      } catch (error) {
        this.validationErrors = ['Error al parsear el archivo LAS: ' + (error as Error).message];
      }
    };
    reader.readAsText(file);
  }

  toggleCurve(mnemonic: string): void {
    if (this.selectedCurves.has(mnemonic)) {
      this.selectedCurves.delete(mnemonic);
    } else {
      this.selectedCurves.add(mnemonic);
    }
  }

  selectAllCurves(): void {
    this.lasFile?.curves.forEach(c => this.selectedCurves.add(c.mnemonic));
  }

  deselectAllCurves(): void {
    this.selectedCurves.clear();
  }

  getCurveIcon(curve: LasCurveInfo): string {
    const mnemonic = curve.mnemonic.toUpperCase();
    if (mnemonic.includes('DEPT') || mnemonic.includes('DEPTH') || mnemonic === 'MD') {
      return 'straighten';
    }
    if (mnemonic.includes('GR') || mnemonic === 'SGR' || mnemonic === 'CGR') {
      return 'signal_cellular_alt';
    }
    if (mnemonic.includes('RES') || mnemonic.includes('ILD') || mnemonic.includes('ILM') || mnemonic.includes('LLD')) {
      return 'electrical_services';
    }
    if (mnemonic.includes('NPHI') || mnemonic.includes('NEU') || mnemonic.includes('TNPH')) {
      return 'scatter_plot';
    }
    if (mnemonic.includes('RHOB') || mnemonic.includes('DEN') || mnemonic.includes('DRHO')) {
      return 'density_medium';
    }
    if (mnemonic.includes('DT') || mnemonic.includes('SONIC') || mnemonic.includes('AC')) {
      return 'graphic_eq';
    }
    if (mnemonic.includes('CALI') || mnemonic.includes('CAL')) {
      return 'lens_blur';
    }
    if (mnemonic.includes('SP')) {
      return 'bolt';
    }
    return 'show_chart';
  }

  getCurveDescription(curve: LasCurveInfo): string {
    const mnemonic = curve.mnemonic.toUpperCase();
    const descriptions: { [key: string]: string } = {
      'DEPT': 'Profundidad Medida',
      'DEPTH': 'Profundidad Medida',
      'MD': 'Profundidad Medida (MD)',
      'TVD': 'Profundidad Verdadera (TVD)',
      'GR': 'Rayos Gamma',
      'SGR': 'Rayos Gamma Espectral',
      'CGR': 'Rayos Gamma Corregido',
      'ILD': 'Resistividad Inductiva Profunda',
      'ILM': 'Resistividad Inductiva Media',
      'LLD': 'Resistividad Laterolog Profunda',
      'LLS': 'Resistividad Laterolog Somera',
      'MSFL': 'Resistividad Micro-esférica',
      'NPHI': 'Porosidad Neutrón',
      'TNPH': 'Porosidad Neutrón Térmica',
      'RHOB': 'Densidad Bulk',
      'DRHO': 'Corrección de Densidad',
      'DT': 'Tiempo de Tránsito Sónico',
      'DTC': 'Tiempo de Tránsito Compresional',
      'DTS': 'Tiempo de Tránsito de Cizalla',
      'CALI': 'Calibre del Pozo',
      'CAL': 'Calibre',
      'SP': 'Potencial Espontáneo',
      'PEF': 'Factor Fotoeléctrico',
      'PHIN': 'Porosidad Neutrón Efectiva',
      'PHID': 'Porosidad Densidad'
    };

    return descriptions[mnemonic] || curve.description || mnemonic;
  }

  getDataPointCount(curve: LasCurveInfo): number {
    if (!this.lasFile) return 0;
    const data = this.lasFile.data.get(curve.mnemonic);
    return data ? data.filter(v => !isNaN(v)).length : 0;
  }

  getDepthRange(): string {
    if (!this.lasFile) return '';
    const depths = this.lasParser.getDepthArray(this.lasFile);
    if (depths.length === 0) return '';

    const validDepths = depths.filter(d => !isNaN(d));
    const min = Math.min(...validDepths);
    const max = Math.max(...validDepths);
    const unit = this.lasFile.wellInfo.depthUnit || 'm';

    return `${min.toFixed(1)} - ${max.toFixed(1)} ${unit}`;
  }

  onImport(): void {
    if (!this.importForm.valid || !this.lasFile || this.selectedCurves.size === 0) return;

    this.isLoading = true;
    this.step = 3;

    // Prepare import data
    const formValues = this.importForm.value;
    const depths = this.lasParser.getDepthArray(this.lasFile);

    const curvesData = Array.from(this.selectedCurves)
      .filter(mnemonic => !mnemonic.includes('DEPT') && mnemonic !== 'MD')
      .map(mnemonic => {
        const curveInfo = this.lasFile!.curves.find(c => c.mnemonic === mnemonic)!;
        const data = this.lasFile!.data.get(mnemonic) || [];
        return {
          curveName: mnemonic,
          unit: curveInfo.unit,
          description: curveInfo.description,
          data: depths.map((depth, i) => ({
            depth,
            value: data[i]
          })).filter(d => !isNaN(d.depth) && !isNaN(d.value))
        };
      });

    // Create well log
    const wellLog: RvWellLog = {
      wellAssetId: formValues.wellId,
      name: formValues.logRunName || this.fileName,
      logType: formValues.logType,
      topDepthMdM: Math.min(...depths.filter(d => !isNaN(d))),
      bottomDepthMdM: Math.max(...depths.filter(d => !isNaN(d))),
      serviceCompany: this.lasFile!.wellInfo.serviceCompany,
      dataFormat: 'LAS',
      availableCurves: Array.from(this.selectedCurves),
      additionalInfo: {
        curves: curvesData,
        sourceFile: this.fileName,
        lasVersion: this.lasFile!.header.version
      }
    };

    // Call API to save
    this.rvService.createWellLog(this.data.tenantId, wellLog).subscribe({
      next: (result) => {
        this.isLoading = false;
        const importResult: RvLasImportResult = {
          wellLogRunId: result.assetId,
          curvesImported: curvesData.length,
          dataPointsImported: curvesData.reduce((sum, c) => sum + c.data.length, 0)
        };
        this.dialogRef.close(importResult);
      },
      error: (error) => {
        this.isLoading = false;
        this.validationErrors = ['Error al importar: ' + (error.message || 'Error desconocido')];
        this.step = 2;
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  private formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }
}
