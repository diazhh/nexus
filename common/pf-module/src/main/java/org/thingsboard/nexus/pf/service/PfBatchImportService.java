/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.nexus.pf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.nexus.pf.dto.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for batch importing wells and related entities from CSV files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PfBatchImportService {

    private final PfWellService wellService;
    private final PfWellpadService wellpadService;
    private final PfEspSystemService espSystemService;
    private final PfPcpSystemService pcpSystemService;
    private final PfGasLiftSystemService gasLiftSystemService;
    private final PfRodPumpSystemService rodPumpSystemService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Imports wells from CSV file.
     */
    public ImportResult importWells(UUID tenantId, MultipartFile file) {
        ImportResult result = new ImportResult();
        result.setStartTime(System.currentTimeMillis());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreEmptyLines()
                    .withTrim()
                    .parse(reader);

            for (CSVRecord record : parser) {
                result.totalRecords++;

                try {
                    PfWellDto wellDto = parseWellRecord(tenantId, record);
                    validateWellDto(wellDto, result, record.getRecordNumber());

                    if (result.getErrors().stream()
                            .noneMatch(e -> e.getRecordNumber() == record.getRecordNumber())) {
                        wellService.createWell(tenantId, wellDto);
                        result.successCount++;
                    }

                } catch (Exception e) {
                    result.failureCount++;
                    result.addError(record.getRecordNumber(),
                            "Error processing record: " + e.getMessage());
                    log.warn("Error importing well record {}: {}", record.getRecordNumber(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error reading CSV file: {}", e.getMessage());
            result.addError(0, "Error reading file: " + e.getMessage());
        }

        result.setEndTime(System.currentTimeMillis());
        return result;
    }

    /**
     * Imports wells from CSV file asynchronously.
     */
    @Async
    public CompletableFuture<ImportResult> importWellsAsync(UUID tenantId, MultipartFile file) {
        return CompletableFuture.completedFuture(importWells(tenantId, file));
    }

    /**
     * Imports ESP systems from CSV file.
     */
    public ImportResult importEspSystems(UUID tenantId, MultipartFile file) {
        ImportResult result = new ImportResult();
        result.setStartTime(System.currentTimeMillis());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreEmptyLines()
                    .withTrim()
                    .parse(reader);

            for (CSVRecord record : parser) {
                result.totalRecords++;

                try {
                    PfEspSystemDto dto = parseEspRecord(tenantId, record);
                    espSystemService.createEspSystem(tenantId, dto);
                    result.successCount++;

                } catch (Exception e) {
                    result.failureCount++;
                    result.addError(record.getRecordNumber(),
                            "Error processing ESP record: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            result.addError(0, "Error reading file: " + e.getMessage());
        }

        result.setEndTime(System.currentTimeMillis());
        return result;
    }

    /**
     * Validates CSV file format before import.
     */
    public ValidationResult validateCsvFile(MultipartFile file, String entityType) {
        ValidationResult validation = new ValidationResult();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            Set<String> headers = parser.getHeaderMap().keySet();
            Set<String> requiredHeaders = getRequiredHeaders(entityType);

            // Check for missing required headers
            Set<String> missingHeaders = new HashSet<>(requiredHeaders);
            missingHeaders.removeAll(headers);

            if (!missingHeaders.isEmpty()) {
                validation.setValid(false);
                validation.setMissingHeaders(new ArrayList<>(missingHeaders));
            } else {
                validation.setValid(true);
            }

            validation.setFoundHeaders(new ArrayList<>(headers));
            validation.setRecordCount((int) parser.stream().count());

        } catch (Exception e) {
            validation.setValid(false);
            validation.setError("Error reading file: " + e.getMessage());
        }

        return validation;
    }

    /**
     * Generates a CSV template for the specified entity type.
     */
    public String generateCsvTemplate(String entityType) {
        Set<String> headers = getAllHeaders(entityType);
        return String.join(",", headers);
    }

    // Parsing methods

    private PfWellDto parseWellRecord(UUID tenantId, CSVRecord record) {
        PfWellDto dto = new PfWellDto();

        dto.setName(getRequiredValue(record, "name"));
        dto.setApiNumber(getValue(record, "api_number"));
        dto.setStatus(parseEnum(getValue(record, "status"), WellStatus.class, WellStatus.PRODUCING));
        dto.setLiftSystemType(parseEnum(getValue(record, "lift_system_type"),
                LiftSystemType.class, LiftSystemType.NATURAL_FLOW));

        dto.setLatitude(parseBigDecimal(getValue(record, "latitude")));
        dto.setLongitude(parseBigDecimal(getValue(record, "longitude")));
        dto.setMeasuredDepthFt(parseBigDecimal(getValue(record, "measured_depth_ft")));
        dto.setTrueVerticalDepthFt(parseBigDecimal(getValue(record, "true_vertical_depth_ft")));

        dto.setSpudDate(parseDate(getValue(record, "spud_date")));
        dto.setFirstProductionDate(parseDate(getValue(record, "first_production_date")));
        dto.setCurrentProductionBpd(parseBigDecimal(getValue(record, "current_production_bpd")));

        // Wellpad reference by name - would need lookup to get UUID
        String wellpadName = getValue(record, "wellpad_name");
        if (wellpadName != null && !wellpadName.isEmpty()) {
            // In a real implementation, lookup wellpad by name to get ID
            log.debug("Wellpad name specified: {} - would need lookup", wellpadName);
        }

        return dto;
    }

    private PfEspSystemDto parseEspRecord(UUID tenantId, CSVRecord record) {
        PfEspSystemDto dto = new PfEspSystemDto();

        // Well reference
        String wellName = getRequiredValue(record, "well_name");
        // Would need to lookup well by name to get ID

        dto.setPumpModel(getValue(record, "pump_model"));
        dto.setPumpSerialNumber(getValue(record, "pump_serial_number"));
        dto.setStages(parseInt(getValue(record, "stages")));
        dto.setRatedHeadFt(parseBigDecimal(getValue(record, "rated_head_ft")));
        dto.setRatedFlowBpd(parseBigDecimal(getValue(record, "rated_flow_bpd")));
        dto.setMotorHp(parseBigDecimal(getValue(record, "motor_hp")));
        dto.setMotorVoltage(parseInt(getValue(record, "motor_voltage")));
        dto.setFrequencyHz(parseBigDecimal(getValue(record, "frequency_hz")));
        dto.setSettingDepthFt(parseBigDecimal(getValue(record, "setting_depth_ft")));
        dto.setInstallationDate(parseDate(getValue(record, "installation_date")));

        // Operational limits
        dto.setMinFrequencyHz(parseBigDecimal(getValue(record, "min_frequency_hz")));
        dto.setMaxFrequencyHz(parseBigDecimal(getValue(record, "max_frequency_hz")));
        dto.setMinCurrentAmps(parseBigDecimal(getValue(record, "min_current_amps")));
        dto.setMaxCurrentAmps(parseBigDecimal(getValue(record, "max_current_amps")));
        dto.setMaxMotorTempF(parseBigDecimal(getValue(record, "max_motor_temp_f")));
        dto.setMinPipPsi(parseBigDecimal(getValue(record, "min_pip_psi")));
        dto.setMaxVibrationG(parseBigDecimal(getValue(record, "max_vibration_g")));

        return dto;
    }

    private void validateWellDto(PfWellDto dto, ImportResult result, long recordNumber) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            result.addError(recordNumber, "Well name is required");
            result.failureCount++;
        }

        BigDecimal minLat = BigDecimal.valueOf(-90);
        BigDecimal maxLat = BigDecimal.valueOf(90);
        if (dto.getLatitude() != null &&
                (dto.getLatitude().compareTo(minLat) < 0 || dto.getLatitude().compareTo(maxLat) > 0)) {
            result.addWarning(recordNumber, "Invalid latitude value: " + dto.getLatitude());
        }

        BigDecimal minLon = BigDecimal.valueOf(-180);
        BigDecimal maxLon = BigDecimal.valueOf(180);
        if (dto.getLongitude() != null &&
                (dto.getLongitude().compareTo(minLon) < 0 || dto.getLongitude().compareTo(maxLon) > 0)) {
            result.addWarning(recordNumber, "Invalid longitude value: " + dto.getLongitude());
        }

        if (dto.getMeasuredDepthFt() != null && dto.getMeasuredDepthFt().compareTo(BigDecimal.ZERO) < 0) {
            result.addWarning(recordNumber, "Measured depth should be positive");
        }
    }

    // Helper methods

    private String getValue(CSVRecord record, String header) {
        try {
            return record.isMapped(header) ? record.get(header) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getRequiredValue(CSVRecord record, String header) {
        String value = getValue(record, header);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required field '" + header + "' is missing");
        }
        return value;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass, T defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private Set<String> getRequiredHeaders(String entityType) {
        return switch (entityType.toLowerCase()) {
            case "well" -> Set.of("name");
            case "esp" -> Set.of("well_name", "pump_model");
            case "wellpad" -> Set.of("name", "code");
            default -> Set.of();
        };
    }

    private Set<String> getAllHeaders(String entityType) {
        return switch (entityType.toLowerCase()) {
            case "well" -> new LinkedHashSet<>(Arrays.asList(
                    "name", "api_number", "wellpad_name", "status", "lift_system_type",
                    "latitude", "longitude", "measured_depth_ft", "true_vertical_depth_ft",
                    "spud_date", "first_production_date", "current_production_bpd"
            ));
            case "esp" -> new LinkedHashSet<>(Arrays.asList(
                    "well_name", "pump_model", "pump_serial_number", "stages",
                    "rated_head_ft", "rated_flow_bpd", "motor_hp", "motor_voltage",
                    "frequency_hz", "setting_depth_ft", "installation_date",
                    "min_frequency_hz", "max_frequency_hz", "min_current_amps",
                    "max_current_amps", "max_motor_temp_f", "min_pip_psi", "max_vibration_g"
            ));
            case "wellpad" -> new LinkedHashSet<>(Arrays.asList(
                    "name", "code", "flow_station_name", "latitude", "longitude",
                    "capacity_wells", "commissioning_date", "operational_status"
            ));
            default -> new LinkedHashSet<>();
        };
    }

    // Result classes

    @lombok.Data
    public static class ImportResult {
        private long startTime;
        private long endTime;
        private int totalRecords;
        private int successCount;
        private int failureCount;
        private List<ImportError> errors = new ArrayList<>();
        private List<ImportWarning> warnings = new ArrayList<>();

        public void addError(long recordNumber, String message) {
            errors.add(new ImportError(recordNumber, message));
        }

        public void addWarning(long recordNumber, String message) {
            warnings.add(new ImportWarning(recordNumber, message));
        }

        public long getDurationMs() {
            return endTime - startTime;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ImportError {
        private long recordNumber;
        private String message;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ImportWarning {
        private long recordNumber;
        private String message;
    }

    @lombok.Data
    public static class ValidationResult {
        private boolean valid;
        private int recordCount;
        private List<String> foundHeaders;
        private List<String> missingHeaders;
        private String error;
    }
}
