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

/**
 * LAS File Header Information
 */
export interface LasHeader {
  version: string;
  wrap: boolean;
  delimiter?: string;
}

/**
 * Well Information from LAS file
 */
export interface LasWellInfo {
  wellName?: string;
  uwi?: string;
  company?: string;
  field?: string;
  location?: string;
  province?: string;
  country?: string;
  serviceCompany?: string;
  date?: string;
  startDepth?: number;
  stopDepth?: number;
  step?: number;
  nullValue: number;
  depthUnit?: string;
}

/**
 * Curve definition from LAS file
 */
export interface LasCurveInfo {
  mnemonic: string;
  unit: string;
  apiCode?: string;
  description: string;
}

/**
 * Parsed LAS file data
 */
export interface LasFile {
  header: LasHeader;
  wellInfo: LasWellInfo;
  curves: LasCurveInfo[];
  data: Map<string, number[]>;
  parameters?: Map<string, { value: string; unit: string; description: string }>;
}

/**
 * LAS Parser Service
 *
 * Parses LAS 2.0 and LAS 3.0 format files commonly used in the
 * oil and gas industry for well log data exchange.
 *
 * Reference: https://www.cwls.org/products/#las
 */
@Injectable({
  providedIn: 'root'
})
export class RvLasParserService {

  private readonly NULL_VALUES = [-999.25, -999, -9999, -9999.25, -999.2500];

  /**
   * Parse a LAS file content
   */
  parseLasFile(content: string): LasFile {
    const lines = content.split(/\r?\n/);
    const lasFile: LasFile = {
      header: { version: '2.0', wrap: false },
      wellInfo: { nullValue: -999.25 },
      curves: [],
      data: new Map(),
      parameters: new Map()
    };

    let currentSection = '';
    let dataStarted = false;
    const dataLines: string[] = [];

    for (const line of lines) {
      const trimmedLine = line.trim();

      // Skip empty lines and comments
      if (!trimmedLine || trimmedLine.startsWith('#')) continue;

      // Check for section headers
      if (trimmedLine.startsWith('~')) {
        currentSection = this.getSectionType(trimmedLine);
        if (currentSection === 'ASCII' || currentSection === 'DATA') {
          dataStarted = true;
        }
        continue;
      }

      // Process based on current section
      if (dataStarted) {
        dataLines.push(trimmedLine);
      } else {
        switch (currentSection) {
          case 'VERSION':
            this.parseVersionLine(trimmedLine, lasFile.header);
            break;
          case 'WELL':
            this.parseWellLine(trimmedLine, lasFile.wellInfo);
            break;
          case 'CURVE':
            const curveInfo = this.parseCurveLine(trimmedLine);
            if (curveInfo) {
              lasFile.curves.push(curveInfo);
              lasFile.data.set(curveInfo.mnemonic, []);
            }
            break;
          case 'PARAMETER':
            this.parseParameterLine(trimmedLine, lasFile.parameters!);
            break;
        }
      }
    }

    // Parse data section
    if (dataLines.length > 0) {
      this.parseDataSection(dataLines, lasFile);
    }

    return lasFile;
  }

  private getSectionType(line: string): string {
    const sectionChar = line.charAt(1).toUpperCase();
    switch (sectionChar) {
      case 'V': return 'VERSION';
      case 'W': return 'WELL';
      case 'C': return 'CURVE';
      case 'P': return 'PARAMETER';
      case 'O': return 'OTHER';
      case 'A': return 'ASCII';
      default: return 'UNKNOWN';
    }
  }

  private parseVersionLine(line: string, header: LasHeader): void {
    const parts = this.parseInfoLine(line);
    if (!parts) return;

    const mnemonic = parts.mnemonic.toUpperCase();
    const value = parts.value;

    if (mnemonic === 'VERS') {
      header.version = value;
    } else if (mnemonic === 'WRAP') {
      header.wrap = value.toUpperCase() === 'YES';
    } else if (mnemonic === 'DLM') {
      header.delimiter = value;
    }
  }

  private parseWellLine(line: string, wellInfo: LasWellInfo): void {
    const parts = this.parseInfoLine(line);
    if (!parts) return;

    const mnemonic = parts.mnemonic.toUpperCase();
    const value = parts.value;
    const unit = parts.unit;

    switch (mnemonic) {
      case 'STRT':
        wellInfo.startDepth = parseFloat(value);
        wellInfo.depthUnit = unit;
        break;
      case 'STOP':
        wellInfo.stopDepth = parseFloat(value);
        break;
      case 'STEP':
        wellInfo.step = parseFloat(value);
        break;
      case 'NULL':
        wellInfo.nullValue = parseFloat(value);
        break;
      case 'COMP':
        wellInfo.company = value;
        break;
      case 'WELL':
        wellInfo.wellName = value;
        break;
      case 'UWI':
        wellInfo.uwi = value;
        break;
      case 'FLD':
        wellInfo.field = value;
        break;
      case 'LOC':
        wellInfo.location = value;
        break;
      case 'PROV':
      case 'STAT':
        wellInfo.province = value;
        break;
      case 'CTRY':
      case 'CNTY':
        wellInfo.country = value;
        break;
      case 'SRVC':
        wellInfo.serviceCompany = value;
        break;
      case 'DATE':
        wellInfo.date = value;
        break;
    }
  }

  private parseCurveLine(line: string): LasCurveInfo | null {
    const parts = this.parseInfoLine(line);
    if (!parts) return null;

    return {
      mnemonic: parts.mnemonic.toUpperCase(),
      unit: parts.unit,
      apiCode: parts.apiCode,
      description: parts.description
    };
  }

  private parseParameterLine(line: string, parameters: Map<string, { value: string; unit: string; description: string }>): void {
    const parts = this.parseInfoLine(line);
    if (!parts) return;

    parameters.set(parts.mnemonic.toUpperCase(), {
      value: parts.value,
      unit: parts.unit,
      description: parts.description
    });
  }

  /**
   * Parse a single info line in LAS format
   * Format: MNEMONIC.UNIT VALUE : DESCRIPTION
   * or: MNEMONIC.UNIT API_CODE : DESCRIPTION
   */
  private parseInfoLine(line: string): { mnemonic: string; unit: string; value: string; apiCode?: string; description: string } | null {
    // Find the colon that separates value from description
    const colonIndex = line.indexOf(':');
    if (colonIndex === -1) return null;

    const leftPart = line.substring(0, colonIndex).trim();
    const description = line.substring(colonIndex + 1).trim();

    // Parse the left part: MNEMONIC.UNIT VALUE
    const dotIndex = leftPart.indexOf('.');
    if (dotIndex === -1) return null;

    const mnemonic = leftPart.substring(0, dotIndex).trim();

    // Find where unit ends and value begins (first space after dot)
    const afterDot = leftPart.substring(dotIndex + 1);
    const spaceIndex = afterDot.search(/\s/);

    let unit = '';
    let value = '';

    if (spaceIndex === -1) {
      unit = afterDot.trim();
    } else {
      unit = afterDot.substring(0, spaceIndex).trim();
      value = afterDot.substring(spaceIndex).trim();
    }

    return { mnemonic, unit, value, description };
  }

  private parseDataSection(dataLines: string[], lasFile: LasFile): void {
    const curveNames = lasFile.curves.map(c => c.mnemonic);
    const nullValue = lasFile.wellInfo.nullValue;

    for (const line of dataLines) {
      // Skip lines that look like comments or section headers
      if (line.startsWith('~') || line.startsWith('#')) continue;

      // Split by whitespace
      const values = line.trim().split(/\s+/).map(v => parseFloat(v));

      if (values.length !== curveNames.length) {
        // Handle wrapped data or skip malformed lines
        continue;
      }

      for (let i = 0; i < curveNames.length; i++) {
        const curveName = curveNames[i];
        const curveData = lasFile.data.get(curveName);

        if (curveData) {
          const value = values[i];
          // Check for null values
          if (this.isNullValue(value, nullValue)) {
            curveData.push(NaN);
          } else {
            curveData.push(value);
          }
        }
      }
    }
  }

  private isNullValue(value: number, nullValue: number): boolean {
    if (isNaN(value)) return true;
    if (value === nullValue) return true;
    // Check common null values
    return this.NULL_VALUES.some(nv => Math.abs(value - nv) < 0.01);
  }

  /**
   * Get depth array from parsed LAS file
   */
  getDepthArray(lasFile: LasFile): number[] {
    // First curve is usually DEPT or DEPTH
    const depthCurve = lasFile.curves.find(c =>
      c.mnemonic.includes('DEPT') || c.mnemonic.includes('DEPTH') || c.mnemonic === 'MD'
    );

    if (depthCurve) {
      return lasFile.data.get(depthCurve.mnemonic) || [];
    }

    // If no depth curve found, use first curve
    if (lasFile.curves.length > 0) {
      return lasFile.data.get(lasFile.curves[0].mnemonic) || [];
    }

    return [];
  }

  /**
   * Get curve data by mnemonic
   */
  getCurveData(lasFile: LasFile, mnemonic: string): number[] {
    return lasFile.data.get(mnemonic.toUpperCase()) || [];
  }

  /**
   * Validate LAS file structure
   */
  validateLasFile(lasFile: LasFile): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (lasFile.curves.length === 0) {
      errors.push('No curves defined in file');
    }

    if (!lasFile.wellInfo.wellName && !lasFile.wellInfo.uwi) {
      errors.push('Well name or UWI not specified');
    }

    const depthCurve = lasFile.curves.find(c =>
      c.mnemonic.includes('DEPT') || c.mnemonic.includes('DEPTH') || c.mnemonic === 'MD'
    );
    if (!depthCurve) {
      errors.push('No depth curve found');
    }

    // Check that all curves have data
    for (const curve of lasFile.curves) {
      const data = lasFile.data.get(curve.mnemonic);
      if (!data || data.length === 0) {
        errors.push(`Curve ${curve.mnemonic} has no data`);
      }
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }
}
