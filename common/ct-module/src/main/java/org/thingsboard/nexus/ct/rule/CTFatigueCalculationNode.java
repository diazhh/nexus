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
package org.thingsboard.nexus.ct.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.nexus.ct.dto.CTFatigueLogDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nodo personalizado de ThingsBoard para cálculo de fatiga en reels de Coiled Tubing.
 * 
 * Este nodo:
 * 1. Recibe telemetría de reels (presión, tensión, temperatura, dirección)
 * 2. Obtiene atributos del reel (geometría, material, fatiga actual)
 * 3. Calcula fatiga usando algoritmo de Palmgren-Miner
 * 4. Guarda el registro en base de datos
 * 5. Actualiza atributos del reel
 * 6. Genera alarmas según umbrales
 * 
 * Uso: Asignar este nodo a la Rule Chain de assets tipo CT_REEL
 */
@Slf4j
public class CTFatigueCalculationNode {

    private static final RestTemplate restTemplate = new RestTemplate();
    
    // Propiedades de materiales (módulo de elasticidad, constantes S-N)
    private static final Map<String, MaterialProperties> MATERIALS = new HashMap<>();
    
    static {
        MATERIALS.put("QT-800", new MaterialProperties(30e6, 1e15, 3.5, 80000));
        MATERIALS.put("QT-900", new MaterialProperties(30e6, 8e14, 3.3, 90000));
        MATERIALS.put("QT-1000", new MaterialProperties(30e6, 5e14, 3.0, 100000));
    }
    
    // Factores de corrosión
    private static final Map<String, Double> CORROSION_FACTORS = new HashMap<>();
    
    static {
        CORROSION_FACTORS.put("SWEET", 1.0);
        CORROSION_FACTORS.put("MILDLY_SOUR", 1.2);
        CORROSION_FACTORS.put("SOUR", 1.5);
        CORROSION_FACTORS.put("HIGHLY_CORROSIVE", 2.0);
    }

    /**
     * Procesa telemetría del reel y calcula fatiga
     */
    public FatigueCalculationResult calculate(TelemetryData telemetry, ReelAttributes attributes) {
        try {
            log.debug("Calculating fatigue for reel: {}", attributes.getReelId());
            
            // 1. Validar datos de entrada
            if (!isValidTelemetry(telemetry)) {
                log.warn("Invalid telemetry data, skipping calculation");
                return FatigueCalculationResult.skip("Invalid telemetry");
            }
            
            // 2. Obtener propiedades del material
            MaterialProperties material = MATERIALS.getOrDefault(
                attributes.getMaterialGrade(), 
                MATERIALS.get("QT-800")
            );
            
            // 3. Calcular geometría
            double Do = attributes.getTubingOdInch();
            double Di = attributes.getTubingIdInch();
            double t = (Do - Di) / 2.0;
            double area = Math.PI / 4.0 * (Math.pow(Do, 2) - Math.pow(Di, 2));
            
            // 4. Determinar radio de curvatura
            double radius = telemetry.getDirection().equals("STOPPED")
                ? attributes.getReelCoreDiameterInch() / 2.0
                : attributes.getTypicalGooseneckRadiusInch();
            
            // 5. Calcular esfuerzos
            double sigma_h = (telemetry.getPressure() * Di) / (2.0 * t);
            double sigma_a = telemetry.getTension() / area;
            double sigma_b = (material.getE() * (Do / 2.0)) / radius;
            
            // 6. Esfuerzo equivalente Von Mises
            double sigma_vm = Math.sqrt(
                Math.pow(sigma_h, 2) + 
                Math.pow(sigma_a, 2) + 
                Math.pow(sigma_b, 2) - 
                sigma_h * sigma_a - 
                sigma_h * sigma_b - 
                sigma_a * sigma_b
            );
            
            // 7. Verificar si el esfuerzo es significativo
            if (sigma_vm < 1000) {
                log.debug("Stress too low ({} psi), skipping fatigue calculation", sigma_vm);
                return FatigueCalculationResult.skip("Stress below threshold");
            }
            
            // 8. Ciclos hasta falla (curva S-N)
            double N = material.getA() * Math.pow(sigma_vm, -material.getM());
            N = Math.max(Math.min(N, 1e9), 1.0);
            
            // 9. Factores de corrección
            double corrosionFactor = CORROSION_FACTORS.getOrDefault(
                attributes.getCorrosionEnvironment(), 
                1.0
            );
            double weldFactor = attributes.getWeldStressConcentrationFactor();
            double tempFactor = 1.0 + ((telemetry.getTemperature() - 70.0) / 1000.0);
            
            // 10. Incremento de fatiga
            double fatigueIncrement = (1.0 / N) * corrosionFactor * weldFactor * tempFactor;
            
            // Solo incrementar si hay movimiento
            boolean shouldIncrement = telemetry.getDirection().equals("IN") || 
                                     telemetry.getDirection().equals("OUT");
            
            if (!shouldIncrement) {
                fatigueIncrement = 0.0;
            }
            
            // 11. Nueva fatiga acumulada
            double currentFatigue = attributes.getAccumulatedFatiguePercent();
            double newFatiguePercent = Math.min(currentFatigue + (fatigueIncrement * 100.0), 100.0);
            int newTotalCycles = attributes.getTotalCycles() + (shouldIncrement ? 1 : 0);
            
            // 12. Ciclos restantes estimados
            double avgFatiguePerCycle = newFatiguePercent / Math.max(newTotalCycles, 1);
            int remainingCycles = (int) Math.floor((100.0 - newFatiguePercent) / Math.max(avgFatiguePerCycle, 0.001));
            
            // 13. Construir resultado
            FatigueCalculationResult result = new FatigueCalculationResult();
            result.setSuccess(true);
            result.setReelId(attributes.getReelId());
            result.setTenantId(attributes.getTenantId());
            result.setTimestamp(System.currentTimeMillis());
            result.setCycleNumber(newTotalCycles);
            
            // Parámetros operacionales
            result.setPressurePsi(round(telemetry.getPressure(), 2));
            result.setTensionLbf(round(telemetry.getTension(), 2));
            result.setBendRadiusIn(round(radius, 2));
            result.setTemperatureF(round(telemetry.getTemperature(), 2));
            
            // Esfuerzos calculados
            result.setHoopStressPsi(round(sigma_h, 0));
            result.setAxialStressPsi(round(sigma_a, 0));
            result.setBendingStressPsi(round(sigma_b, 0));
            result.setVonMisesStressPsi(round(sigma_vm, 0));
            
            // Fatiga
            result.setCyclesToFailure((long) Math.round(N));
            result.setFatigueIncrement(round(fatigueIncrement, 10));
            result.setAccumulatedFatiguePercent(round(newFatiguePercent, 3));
            
            // Factores
            result.setCorrosionFactor(round(corrosionFactor, 3));
            result.setWeldFactor(round(weldFactor, 3));
            result.setTemperatureFactor(round(tempFactor, 3));
            
            // Metadata
            result.setCalculationMethod("PALMGREN_MINER");
            result.setNotes("Direction: " + telemetry.getDirection());
            
            // Estado de fatiga
            result.setNewFatiguePercent(newFatiguePercent);
            result.setNewTotalCycles(newTotalCycles);
            result.setRemainingCycles(remainingCycles);
            result.setFatigueStatus(getFatigueStatus(newFatiguePercent));
            
            log.info("Fatigue calculated for reel {}: {}% (increment: {}%)", 
                     attributes.getReelId(), 
                     round(newFatiguePercent, 2), 
                     round(fatigueIncrement * 100, 4));
            
            return result;
            
        } catch (Exception e) {
            log.error("Error calculating fatigue for reel {}: {}", 
                     attributes.getReelId(), e.getMessage(), e);
            return FatigueCalculationResult.error(e.getMessage());
        }
    }
    
    /**
     * Guarda el registro de fatiga en la base de datos
     */
    public void saveFatigueLog(FatigueCalculationResult result, String backendUrl) {
        try {
            CTFatigueLogDto logDto = CTFatigueLogDto.builder()
                .tenantId(result.getTenantId())
                .reelId(result.getReelId())
                .timestamp(result.getTimestamp())
                .cycleNumber(result.getCycleNumber())
                .pressurePsi(BigDecimal.valueOf(result.getPressurePsi()))
                .tensionLbf(BigDecimal.valueOf(result.getTensionLbf()))
                .bendRadiusIn(BigDecimal.valueOf(result.getBendRadiusIn()))
                .temperatureF(BigDecimal.valueOf(result.getTemperatureF()))
                .hoopStressPsi(BigDecimal.valueOf(result.getHoopStressPsi()))
                .axialStressPsi(BigDecimal.valueOf(result.getAxialStressPsi()))
                .bendingStressPsi(BigDecimal.valueOf(result.getBendingStressPsi()))
                .vonMisesStressPsi(BigDecimal.valueOf(result.getVonMisesStressPsi()))
                .cyclesToFailure(result.getCyclesToFailure())
                .fatigueIncrement(BigDecimal.valueOf(result.getFatigueIncrement()))
                .accumulatedFatiguePercent(BigDecimal.valueOf(result.getAccumulatedFatiguePercent()))
                .corrosionFactor(BigDecimal.valueOf(result.getCorrosionFactor()))
                .weldFactor(BigDecimal.valueOf(result.getWeldFactor()))
                .temperatureFactor(BigDecimal.valueOf(result.getTemperatureFactor()))
                .calculationMethod(result.getCalculationMethod())
                .notes(result.getNotes())
                .build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CTFatigueLogDto> request = new HttpEntity<>(logDto, headers);
            
            String url = backendUrl + "/api/nexus/ct/fatigue/log";
            restTemplate.postForEntity(url, request, Void.class);
            
            log.debug("Fatigue log saved successfully for reel: {}", result.getReelId());
            
        } catch (Exception e) {
            log.error("Error saving fatigue log: {}", e.getMessage(), e);
        }
    }
    
    private boolean isValidTelemetry(TelemetryData telemetry) {
        return telemetry != null && 
               telemetry.getPressure() >= 0 && 
               telemetry.getTension() >= 0 &&
               telemetry.getDirection() != null;
    }
    
    private String getFatigueStatus(double fatiguePercent) {
        if (fatiguePercent >= 95.0) return "CRITICAL";
        if (fatiguePercent >= 80.0) return "HIGH";
        return "NORMAL";
    }
    
    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    
    // Clases auxiliares
    
    public static class TelemetryData {
        private double pressure;
        private double tension;
        private double temperature = 70.0;
        private String direction = "STOPPED";
        
        public double getPressure() { return pressure; }
        public void setPressure(double pressure) { this.pressure = pressure; }
        public double getTension() { return tension; }
        public void setTension(double tension) { this.tension = tension; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
    }
    
    public static class ReelAttributes {
        private UUID reelId;
        private UUID tenantId;
        private double tubingOdInch = 2.375;
        private double tubingIdInch = 1.995;
        private String materialGrade = "QT-800";
        private double typicalGooseneckRadiusInch = 72.0;
        private double reelCoreDiameterInch = 96.0;
        private double accumulatedFatiguePercent = 0.0;
        private int totalCycles = 0;
        private String corrosionEnvironment = "SWEET";
        private double weldStressConcentrationFactor = 1.0;
        
        // Getters y setters
        public UUID getReelId() { return reelId; }
        public void setReelId(UUID reelId) { this.reelId = reelId; }
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        public double getTubingOdInch() { return tubingOdInch; }
        public void setTubingOdInch(double tubingOdInch) { this.tubingOdInch = tubingOdInch; }
        public double getTubingIdInch() { return tubingIdInch; }
        public void setTubingIdInch(double tubingIdInch) { this.tubingIdInch = tubingIdInch; }
        public String getMaterialGrade() { return materialGrade; }
        public void setMaterialGrade(String materialGrade) { this.materialGrade = materialGrade; }
        public double getTypicalGooseneckRadiusInch() { return typicalGooseneckRadiusInch; }
        public void setTypicalGooseneckRadiusInch(double typicalGooseneckRadiusInch) { 
            this.typicalGooseneckRadiusInch = typicalGooseneckRadiusInch; 
        }
        public double getReelCoreDiameterInch() { return reelCoreDiameterInch; }
        public void setReelCoreDiameterInch(double reelCoreDiameterInch) { 
            this.reelCoreDiameterInch = reelCoreDiameterInch; 
        }
        public double getAccumulatedFatiguePercent() { return accumulatedFatiguePercent; }
        public void setAccumulatedFatiguePercent(double accumulatedFatiguePercent) { 
            this.accumulatedFatiguePercent = accumulatedFatiguePercent; 
        }
        public int getTotalCycles() { return totalCycles; }
        public void setTotalCycles(int totalCycles) { this.totalCycles = totalCycles; }
        public String getCorrosionEnvironment() { return corrosionEnvironment; }
        public void setCorrosionEnvironment(String corrosionEnvironment) { 
            this.corrosionEnvironment = corrosionEnvironment; 
        }
        public double getWeldStressConcentrationFactor() { return weldStressConcentrationFactor; }
        public void setWeldStressConcentrationFactor(double weldStressConcentrationFactor) { 
            this.weldStressConcentrationFactor = weldStressConcentrationFactor; 
        }
    }
    
    public static class FatigueCalculationResult {
        private boolean success;
        private String errorMessage;
        private UUID reelId;
        private UUID tenantId;
        private Long timestamp;
        private Integer cycleNumber;
        private double pressurePsi;
        private double tensionLbf;
        private double bendRadiusIn;
        private double temperatureF;
        private double hoopStressPsi;
        private double axialStressPsi;
        private double bendingStressPsi;
        private double vonMisesStressPsi;
        private Long cyclesToFailure;
        private double fatigueIncrement;
        private double accumulatedFatiguePercent;
        private double corrosionFactor;
        private double weldFactor;
        private double temperatureFactor;
        private String calculationMethod;
        private String notes;
        private double newFatiguePercent;
        private int newTotalCycles;
        private int remainingCycles;
        private String fatigueStatus;
        
        public static FatigueCalculationResult skip(String reason) {
            FatigueCalculationResult result = new FatigueCalculationResult();
            result.setSuccess(false);
            result.setErrorMessage("Skipped: " + reason);
            return result;
        }
        
        public static FatigueCalculationResult error(String message) {
            FatigueCalculationResult result = new FatigueCalculationResult();
            result.setSuccess(false);
            result.setErrorMessage(message);
            return result;
        }
        
        // Getters y setters completos
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public UUID getReelId() { return reelId; }
        public void setReelId(UUID reelId) { this.reelId = reelId; }
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        public Integer getCycleNumber() { return cycleNumber; }
        public void setCycleNumber(Integer cycleNumber) { this.cycleNumber = cycleNumber; }
        public double getPressurePsi() { return pressurePsi; }
        public void setPressurePsi(double pressurePsi) { this.pressurePsi = pressurePsi; }
        public double getTensionLbf() { return tensionLbf; }
        public void setTensionLbf(double tensionLbf) { this.tensionLbf = tensionLbf; }
        public double getBendRadiusIn() { return bendRadiusIn; }
        public void setBendRadiusIn(double bendRadiusIn) { this.bendRadiusIn = bendRadiusIn; }
        public double getTemperatureF() { return temperatureF; }
        public void setTemperatureF(double temperatureF) { this.temperatureF = temperatureF; }
        public double getHoopStressPsi() { return hoopStressPsi; }
        public void setHoopStressPsi(double hoopStressPsi) { this.hoopStressPsi = hoopStressPsi; }
        public double getAxialStressPsi() { return axialStressPsi; }
        public void setAxialStressPsi(double axialStressPsi) { this.axialStressPsi = axialStressPsi; }
        public double getBendingStressPsi() { return bendingStressPsi; }
        public void setBendingStressPsi(double bendingStressPsi) { this.bendingStressPsi = bendingStressPsi; }
        public double getVonMisesStressPsi() { return vonMisesStressPsi; }
        public void setVonMisesStressPsi(double vonMisesStressPsi) { this.vonMisesStressPsi = vonMisesStressPsi; }
        public Long getCyclesToFailure() { return cyclesToFailure; }
        public void setCyclesToFailure(Long cyclesToFailure) { this.cyclesToFailure = cyclesToFailure; }
        public double getFatigueIncrement() { return fatigueIncrement; }
        public void setFatigueIncrement(double fatigueIncrement) { this.fatigueIncrement = fatigueIncrement; }
        public double getAccumulatedFatiguePercent() { return accumulatedFatiguePercent; }
        public void setAccumulatedFatiguePercent(double accumulatedFatiguePercent) { 
            this.accumulatedFatiguePercent = accumulatedFatiguePercent; 
        }
        public double getCorrosionFactor() { return corrosionFactor; }
        public void setCorrosionFactor(double corrosionFactor) { this.corrosionFactor = corrosionFactor; }
        public double getWeldFactor() { return weldFactor; }
        public void setWeldFactor(double weldFactor) { this.weldFactor = weldFactor; }
        public double getTemperatureFactor() { return temperatureFactor; }
        public void setTemperatureFactor(double temperatureFactor) { this.temperatureFactor = temperatureFactor; }
        public String getCalculationMethod() { return calculationMethod; }
        public void setCalculationMethod(String calculationMethod) { this.calculationMethod = calculationMethod; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public double getNewFatiguePercent() { return newFatiguePercent; }
        public void setNewFatiguePercent(double newFatiguePercent) { this.newFatiguePercent = newFatiguePercent; }
        public int getNewTotalCycles() { return newTotalCycles; }
        public void setNewTotalCycles(int newTotalCycles) { this.newTotalCycles = newTotalCycles; }
        public int getRemainingCycles() { return remainingCycles; }
        public void setRemainingCycles(int remainingCycles) { this.remainingCycles = remainingCycles; }
        public String getFatigueStatus() { return fatigueStatus; }
        public void setFatigueStatus(String fatigueStatus) { this.fatigueStatus = fatigueStatus; }
    }
    
    private static class MaterialProperties {
        private final double E;    // Módulo de elasticidad (psi)
        private final double A;    // Constante de curva S-N
        private final double m;    // Exponente de curva S-N
        private final double yield; // Límite elástico (psi)
        
        public MaterialProperties(double E, double A, double m, double yield) {
            this.E = E;
            this.A = A;
            this.m = m;
            this.yield = yield;
        }
        
        public double getE() { return E; }
        public double getA() { return A; }
        public double getM() { return m; }
        public double getYield() { return yield; }
    }
}
