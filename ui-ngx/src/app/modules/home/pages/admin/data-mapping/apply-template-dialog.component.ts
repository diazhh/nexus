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

import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DataMappingService } from '@core/http/data-mapping.service';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { MappingTemplate, DataSourceConfig } from '@shared/models/data-mapping.models';
import { Device } from '@shared/models/device.models';
import { Asset } from '@shared/models/asset.models';
import { PageLink } from '@shared/models/page/page-link';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, switchMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

export interface ApplyTemplateDialogData {
  template?: MappingTemplate;
  moduleKey?: string;
}

@Component({
  selector: 'tb-apply-template-dialog',
  templateUrl: './apply-template-dialog.component.html',
  styleUrls: ['./apply-template-dialog.component.scss']
})
export class ApplyTemplateDialogComponent implements OnInit, OnDestroy {

  applyForm: FormGroup;
  currentStep = 1;
  totalSteps = 3;
  isApplying = false;

  // Step 1: Device selection
  devices: Device[] = [];
  filteredDevices: Device[] = [];
  isLoadingDevices = false;

  // Step 2: Asset selection
  assets: Asset[] = [];
  filteredAssets: Asset[] = [];
  isLoadingAssets = false;

  // Step 3: Template selection
  templates: MappingTemplate[] = [];
  isLoadingTemplates = false;

  private deviceSearch$ = new Subject<string>();
  private assetSearch$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private dialogRef: MatDialogRef<ApplyTemplateDialogComponent, DataSourceConfig>,
    @Inject(MAT_DIALOG_DATA) public data: ApplyTemplateDialogData,
    private fb: FormBuilder,
    private store: Store<AppState>,
    private dataMappingService: DataMappingService,
    private deviceService: DeviceService,
    private assetService: AssetService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.setupSearchStreams();
    this.loadInitialData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    this.applyForm = this.fb.group({
      deviceId: [null, Validators.required],
      deviceSearch: [''],
      assetId: [null, Validators.required],
      assetSearch: [''],
      templateId: [this.data.template?.id?.id || null, Validators.required]
    });
  }

  private setupSearchStreams(): void {
    this.deviceSearch$.pipe(
      debounceTime(300),
      takeUntil(this.destroy$)
    ).subscribe(search => {
      this.filterDevices(search);
    });

    this.assetSearch$.pipe(
      debounceTime(300),
      takeUntil(this.destroy$)
    ).subscribe(search => {
      this.filterAssets(search);
    });
  }

  private loadInitialData(): void {
    this.loadDevices();
    this.loadAssets();
    if (!this.data.template) {
      this.loadTemplates();
    } else {
      this.templates = [this.data.template];
    }
  }

  private loadDevices(): void {
    this.isLoadingDevices = true;
    const pageLink = new PageLink(100);
    this.deviceService.getTenantDeviceInfos(pageLink)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (pageData) => {
          this.devices = pageData.data;
          this.filteredDevices = this.devices;
          this.isLoadingDevices = false;
        },
        error: () => {
          this.isLoadingDevices = false;
        }
      });
  }

  private loadAssets(): void {
    this.isLoadingAssets = true;
    const pageLink = new PageLink(100);
    this.assetService.getTenantAssetInfos(pageLink)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (pageData) => {
          this.assets = pageData.data;
          this.filteredAssets = this.assets;
          this.isLoadingAssets = false;
        },
        error: () => {
          this.isLoadingAssets = false;
        }
      });
  }

  private loadTemplates(): void {
    this.isLoadingTemplates = true;
    const moduleKey = this.data.moduleKey;
    const pageLink = new PageLink(100);

    const request$ = moduleKey
      ? this.dataMappingService.getMappingTemplatesByModule(moduleKey, pageLink)
      : this.dataMappingService.getMappingTemplates(pageLink);

    request$.pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (pageData) => {
          this.templates = pageData.data.filter(t => t.active);
          this.isLoadingTemplates = false;
        },
        error: () => {
          this.isLoadingTemplates = false;
        }
      });
  }

  onDeviceSearchChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.deviceSearch$.next(value);
  }

  onAssetSearchChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.assetSearch$.next(value);
  }

  private filterDevices(search: string): void {
    if (!search) {
      this.filteredDevices = this.devices;
    } else {
      const searchLower = search.toLowerCase();
      this.filteredDevices = this.devices.filter(d =>
        d.name.toLowerCase().includes(searchLower) ||
        d.label?.toLowerCase().includes(searchLower)
      );
    }
  }

  private filterAssets(search: string): void {
    if (!search) {
      this.filteredAssets = this.assets;
    } else {
      const searchLower = search.toLowerCase();
      this.filteredAssets = this.assets.filter(a =>
        a.name.toLowerCase().includes(searchLower) ||
        a.label?.toLowerCase().includes(searchLower)
      );
    }
  }

  selectDevice(device: Device): void {
    this.applyForm.patchValue({ deviceId: device.id.id });
  }

  selectAsset(asset: Asset): void {
    this.applyForm.patchValue({ assetId: asset.id.id });
  }

  selectTemplate(template: MappingTemplate): void {
    this.applyForm.patchValue({ templateId: template.id.id });
  }

  isDeviceSelected(device: Device): boolean {
    return this.applyForm.get('deviceId')?.value === device.id.id;
  }

  isAssetSelected(asset: Asset): boolean {
    return this.applyForm.get('assetId')?.value === asset.id.id;
  }

  isTemplateSelected(template: MappingTemplate): boolean {
    return this.applyForm.get('templateId')?.value === template.id.id;
  }

  getSelectedDevice(): Device | undefined {
    const deviceId = this.applyForm.get('deviceId')?.value;
    return this.devices.find(d => d.id.id === deviceId);
  }

  getSelectedAsset(): Asset | undefined {
    const assetId = this.applyForm.get('assetId')?.value;
    return this.assets.find(a => a.id.id === assetId);
  }

  getSelectedTemplate(): MappingTemplate | undefined {
    const templateId = this.applyForm.get('templateId')?.value;
    return this.templates.find(t => t.id.id === templateId);
  }

  canProceedToNextStep(): boolean {
    switch (this.currentStep) {
      case 1:
        return !!this.applyForm.get('deviceId')?.value;
      case 2:
        return !!this.applyForm.get('assetId')?.value;
      case 3:
        return !!this.applyForm.get('templateId')?.value;
      default:
        return false;
    }
  }

  nextStep(): void {
    if (this.currentStep < this.totalSteps && this.canProceedToNextStep()) {
      this.currentStep++;
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  apply(): void {
    if (this.applyForm.invalid) {
      return;
    }

    this.isApplying = true;
    const { deviceId, assetId, templateId } = this.applyForm.value;

    this.dataMappingService.applyMappingTemplate(templateId, deviceId, assetId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (dataSourceConfig) => {
          this.isApplying = false;
          this.dialogRef.close(dataSourceConfig);
        },
        error: () => {
          this.isApplying = false;
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
