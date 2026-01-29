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

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RvService } from '@core/http/rv/rv.service';

@Component({
  selector: 'tb-rv-calculator',
  templateUrl: './rv-calculator.component.html',
  styleUrls: ['./rv-calculator.component.scss']
})
export class RvCalculatorComponent implements OnInit {

  selectedTabIndex = 0;

  // OOIP Calculator
  ooipForm: FormGroup;
  ooipResult: number = null;

  // Sw Archie Calculator
  archieForm: FormGroup;
  archieResult: number = null;

  // Pb Standing Calculator
  pbForm: FormGroup;
  pbResult: number = null;

  // Viscosity Calculator
  viscosityForm: FormGroup;
  viscosityResult: number = null;

  isCalculating = false;

  constructor(
    private fb: FormBuilder,
    private rvService: RvService
  ) {}

  ngOnInit(): void {
    this.initForms();
  }

  initForms(): void {
    // OOIP Form
    this.ooipForm = this.fb.group({
      areaAcres: [null, [Validators.required, Validators.min(0)]],
      thicknessM: [null, [Validators.required, Validators.min(0)]],
      porosity: [0.2, [Validators.required, Validators.min(0), Validators.max(1)]],
      waterSat: [0.3, [Validators.required, Validators.min(0), Validators.max(1)]],
      boRbStb: [1.2, [Validators.required, Validators.min(0.9)]]
    });

    // Archie Form
    this.archieForm = this.fb.group({
      porosity: [0.2, [Validators.required, Validators.min(0), Validators.max(1)]],
      rw: [0.1, [Validators.required, Validators.min(0)]],
      rt: [10, [Validators.required, Validators.min(0)]],
      a: [1.0],
      m: [2.0],
      n: [2.0]
    });

    // Pb Standing Form
    this.pbForm = this.fb.group({
      rs: [500, [Validators.required, Validators.min(0)]],
      gasGravity: [0.8, [Validators.required, Validators.min(0.5), Validators.max(1.5)]],
      temperature: [180, [Validators.required, Validators.min(60)]],
      apiGravity: [28, [Validators.required, Validators.min(0), Validators.max(70)]]
    });

    // Viscosity Form
    this.viscosityForm = this.fb.group({
      apiGravity: [28, [Validators.required, Validators.min(0), Validators.max(70)]],
      temperature: [180, [Validators.required, Validators.min(60)]]
    });
  }

  calculateOOIP(): void {
    if (this.ooipForm.valid) {
      this.isCalculating = true;
      this.rvService.calculateOOIPVolumetric(this.ooipForm.value).subscribe({
        next: (result) => {
          this.ooipResult = result.ooip;
          this.isCalculating = false;
        },
        error: () => this.isCalculating = false
      });
    }
  }

  calculateArchie(): void {
    if (this.archieForm.valid) {
      this.isCalculating = true;
      this.rvService.calculateSwArchie(this.archieForm.value).subscribe({
        next: (result) => {
          this.archieResult = result.sw;
          this.isCalculating = false;
        },
        error: () => this.isCalculating = false
      });
    }
  }

  calculatePb(): void {
    if (this.pbForm.valid) {
      this.isCalculating = true;
      this.rvService.calculatePbStanding(this.pbForm.value).subscribe({
        next: (result) => {
          this.pbResult = result.pb;
          this.isCalculating = false;
        },
        error: () => this.isCalculating = false
      });
    }
  }

  calculateViscosity(): void {
    if (this.viscosityForm.valid) {
      this.isCalculating = true;
      this.rvService.calculateViscosityBeggsRobinson(this.viscosityForm.value).subscribe({
        next: (result) => {
          this.viscosityResult = result.viscosity;
          this.isCalculating = false;
        },
        error: () => this.isCalculating = false
      });
    }
  }
}
