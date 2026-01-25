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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Nodo personalizado de ThingsBoard para simulación de trabajos de Coiled Tubing.
 * 
 * Este nodo:
 * 1. Recibe parámetros de un trabajo planificado
 * 2. Simula el despliegue de tubería en el pozo
 * 3. Calcula fuerzas (hookload, fricción, pandeo)
 * 4. Calcula presiones hidráulicas
 * 5. Estima tiempos de operación
 * 6. Predice fatiga acumulada
 * 7. Identifica riesgos potenciales
 * 
 * Uso: Llamar antes de ejecutar un trabajo para validar factibilidad
 */
@Slf4j
public class CTJobSimulationNode {

    private static final double GRAVITY = 32.174; // ft/s²
    private static final double PI = Math.PI;
    
    /**
     * Simula un trabajo de coiled tubing
     */
    public SimulationResult simulate(JobParameters params) {
        try {
            log.info("Starting job simulation for well: {}", params.getWellName());
            
            SimulationResult result = new SimulationResult();
            result.setJobId(params.getJobId());
            result.setWellName(params.getWellName());
            
            // 1. Validar factibilidad básica
            FeasibilityCheck feasibility = checkFeasibility(params);
            result.setFeasibility(feasibility);
            
            if (!feasibility.isFeasible()) {
                log.warn("Job not feasible: {}", feasibility.getLimitingFactors());
                return result;
            }
            
            // 2. Calcular análisis de fuerzas
            ForceAnalysis forces = calculateForces(params);
            result.setForces(forces);
            
            // 3. Calcular análisis hidráulico
            HydraulicAnalysis hydraulics = calculateHydraulics(params);
            result.setHydraulics(hydraulics);
            
            // 4. Estimar tiempos
            TimeEstimation times = estimateTimes(params, forces);
            result.setTimes(times);
            
            // 5. Predecir fatiga
            FatiguePrediction fatigue = predictFatigue(params, forces);
            result.setFatigue(fatigue);
            
            // 6. Identificar riesgos
            List<Risk> risks = identifyRisks(params, forces, hydraulics);
            result.setRisks(risks);
            
            log.info("Simulation completed. Feasible: {}, Max hookload: {} lbf, Duration: {} hrs",
                     feasibility.isFeasible(), 
                     forces.getMaxHookload(),
                     times.getTotalDurationHours());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error simulating job: {}", e.getMessage(), e);
            return SimulationResult.error(e.getMessage());
        }
    }
    
    /**
     * Verifica si el trabajo es factible
     */
    private FeasibilityCheck checkFeasibility(JobParameters params) {
        FeasibilityCheck check = new FeasibilityCheck();
        check.setFeasible(true);
        List<String> limitingFactors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Verificar profundidad vs longitud de tubería
        if (params.getTargetDepthFt() > params.getTubingLengthFt()) {
            limitingFactors.add("Target depth exceeds tubing length");
            check.setFeasible(false);
        }
        
        // Verificar capacidad de presión
        double maxPressure = params.getMaxPressurePsi();
        if (maxPressure > params.getUnitMaxPressurePsi()) {
            limitingFactors.add("Required pressure exceeds unit capacity");
            check.setFeasible(false);
        }
        
        // Verificar diámetro de pozo
        if (params.getTubingOdInch() >= params.getWellboreDiameterInch()) {
            limitingFactors.add("Tubing OD too large for wellbore");
            check.setFeasible(false);
        }
        
        // Advertencias
        if (params.getTargetDepthFt() > 20000) {
            warnings.add("Deep well - expect high friction forces");
        }
        
        if (params.getMaxInclinationDeg() > 60) {
            warnings.add("High deviation - risk of buckling");
        }
        
        check.setLimitingFactors(limitingFactors);
        check.setWarnings(warnings);
        
        return check;
    }
    
    /**
     * Calcula fuerzas a lo largo del pozo
     */
    private ForceAnalysis calculateForces(JobParameters params) {
        ForceAnalysis analysis = new ForceAnalysis();
        
        int steps = 100;
        double depthIncrement = params.getTargetDepthFt() / steps;
        
        List<Double> depths = new ArrayList<>();
        List<Double> hookloads = new ArrayList<>();
        List<Double> frictions = new ArrayList<>();
        List<Double> bucklingMargins = new ArrayList<>();
        
        // Propiedades de la tubería
        double Do = params.getTubingOdInch();
        double Di = params.getTubingIdInch();
        double area = PI / 4.0 * (Math.pow(Do, 2) - Math.pow(Di, 2));
        double weight = area * 490.0 / 144.0; // lbf/ft (acero)
        
        double maxHookload = 0;
        
        for (int i = 0; i <= steps; i++) {
            double depth = i * depthIncrement;
            
            // Peso de tubería
            double tubingWeight = weight * depth;
            
            // Fricción (simplificado - modelo de Johancsik)
            double frictionCoef = 0.25;
            double normalForce = tubingWeight * Math.sin(Math.toRadians(params.getMaxInclinationDeg()));
            double frictionForce = frictionCoef * normalForce;
            
            // Hookload (running in)
            double hookload = tubingWeight + frictionForce;
            
            // Margen de pandeo
            double criticalBucklingForce = calculateBucklingForce(params, depth);
            double bucklingMargin = (criticalBucklingForce - hookload) / criticalBucklingForce * 100;
            
            depths.add(depth);
            hookloads.add(hookload);
            frictions.add(frictionForce);
            bucklingMargins.add(Math.max(bucklingMargin, 0));
            
            if (hookload > maxHookload) {
                maxHookload = hookload;
            }
        }
        
        analysis.setDepths(depths);
        analysis.setHookloads(hookloads);
        analysis.setFrictions(frictions);
        analysis.setBucklingMargins(bucklingMargins);
        analysis.setMaxHookload(maxHookload);
        
        return analysis;
    }
    
    /**
     * Calcula presiones hidráulicas
     */
    private HydraulicAnalysis calculateHydraulics(JobParameters params) {
        HydraulicAnalysis analysis = new HydraulicAnalysis();
        
        if (params.getPumpRateBpm() == null || params.getPumpRateBpm() == 0) {
            // No hay bombeo
            return analysis;
        }
        
        int steps = 100;
        double depthIncrement = params.getTargetDepthFt() / steps;
        
        List<Double> depths = new ArrayList<>();
        List<Double> pressures = new ArrayList<>();
        List<Double> frictionPressures = new ArrayList<>();
        List<Double> velocities = new ArrayList<>();
        
        double Do = params.getTubingOdInch();
        double Di = params.getTubingIdInch();
        double area = PI / 4.0 * Math.pow(Di / 12.0, 2); // ft²
        
        double flowRate = params.getPumpRateBpm() * 0.0238; // bpm to ft³/s
        double velocity = flowRate / area; // ft/s
        
        double maxPressure = 0;
        
        for (int i = 0; i <= steps; i++) {
            double depth = i * depthIncrement;
            
            // Presión hidrostática
            double hydrostaticPressure = params.getFluidDensityPpg() * 0.052 * depth;
            
            // Presión de fricción (ecuación de Darcy-Weisbach simplificada)
            double frictionFactor = 0.02;
            double frictionPressure = frictionFactor * (depth / (Di / 12.0)) * 
                                     (params.getFluidDensityPpg() * 8.33 * Math.pow(velocity, 2)) / 
                                     (2 * GRAVITY);
            
            double totalPressure = hydrostaticPressure + frictionPressure;
            
            depths.add(depth);
            pressures.add(totalPressure);
            frictionPressures.add(frictionPressure);
            velocities.add(velocity);
            
            if (totalPressure > maxPressure) {
                maxPressure = totalPressure;
            }
        }
        
        analysis.setDepths(depths);
        analysis.setPressures(pressures);
        analysis.setFrictionPressures(frictionPressures);
        analysis.setVelocities(velocities);
        analysis.setMaxPressure(maxPressure);
        
        return analysis;
    }
    
    /**
     * Estima tiempos de operación
     */
    private TimeEstimation estimateTimes(JobParameters params, ForceAnalysis forces) {
        TimeEstimation times = new TimeEstimation();
        
        // Tiempo de rigging (setup)
        double riggingTimeHours = 2.0;
        
        // Tiempo de running in
        double runningInSpeed = params.getMaxRunningSpeedFtMin();
        double runningInTimeHours = params.getTargetDepthFt() / (runningInSpeed * 60.0);
        
        // Tiempo en profundidad (tratamiento)
        double onDepthTimeHours = params.getEstimatedTreatmentHours() != null ? 
                                  params.getEstimatedTreatmentHours() : 1.0;
        
        // Tiempo de pulling out (más rápido que running in)
        double pullingOutTimeHours = runningInTimeHours * 0.7;
        
        // Tiempo de rigging down
        double riggingDownTimeHours = 1.5;
        
        double totalHours = riggingTimeHours + runningInTimeHours + onDepthTimeHours + 
                           pullingOutTimeHours + riggingDownTimeHours;
        
        times.setRiggingUpHours(riggingTimeHours);
        times.setRunningInHours(runningInTimeHours);
        times.setOnDepthHours(onDepthTimeHours);
        times.setPullingOutHours(pullingOutTimeHours);
        times.setRiggingDownHours(riggingDownTimeHours);
        times.setTotalDurationHours(totalHours);
        
        return times;
    }
    
    /**
     * Predice fatiga acumulada durante el trabajo
     */
    private FatiguePrediction predictFatigue(JobParameters params, ForceAnalysis forces) {
        FatiguePrediction prediction = new FatiguePrediction();
        
        // Ciclos estimados (running in + pulling out)
        int estimatedCycles = 2;
        
        // Esfuerzo promedio (simplificado)
        double avgStress = forces.getMaxHookload() / 
                          (PI / 4.0 * (Math.pow(params.getTubingOdInch(), 2) - 
                                      Math.pow(params.getTubingIdInch(), 2)));
        
        // Fatiga por ciclo (muy simplificado)
        double fatiguePerCycle = 0.01; // 1% por ciclo (placeholder)
        
        double estimatedFatigue = fatiguePerCycle * estimatedCycles;
        
        prediction.setEstimatedCycles(estimatedCycles);
        prediction.setEstimatedFatiguePercent(estimatedFatigue);
        prediction.setAverageStressPsi(avgStress);
        
        return prediction;
    }
    
    /**
     * Identifica riesgos potenciales
     */
    private List<Risk> identifyRisks(JobParameters params, ForceAnalysis forces, 
                                     HydraulicAnalysis hydraulics) {
        List<Risk> risks = new ArrayList<>();
        
        // Riesgo de exceso de tensión
        if (forces.getMaxHookload() > params.getUnitMaxTensionLbf() * 0.9) {
            risks.add(new Risk("HIGH", "TENSION_LIMIT", 
                              "Hookload near unit capacity: " + 
                              Math.round(forces.getMaxHookload()) + " lbf"));
        }
        
        // Riesgo de pandeo
        boolean hasBucklingRisk = forces.getBucklingMargins().stream()
                                        .anyMatch(margin -> margin < 20);
        if (hasBucklingRisk) {
            risks.add(new Risk("MEDIUM", "BUCKLING", 
                              "Low buckling margin in deviated sections"));
        }
        
        // Riesgo de exceso de presión
        if (hydraulics.getMaxPressure() != null && 
            hydraulics.getMaxPressure() > params.getUnitMaxPressurePsi() * 0.9) {
            risks.add(new Risk("HIGH", "PRESSURE_LIMIT", 
                              "Pressure near unit capacity: " + 
                              Math.round(hydraulics.getMaxPressure()) + " psi"));
        }
        
        // Riesgo de stuck pipe
        if (params.getMaxInclinationDeg() > 45 && params.getTargetDepthFt() > 15000) {
            risks.add(new Risk("MEDIUM", "STUCK_PIPE", 
                              "High deviation and depth increase stuck pipe risk"));
        }
        
        return risks;
    }
    
    private double calculateBucklingForce(JobParameters params, double depth) {
        // Fuerza crítica de pandeo (ecuación de Euler simplificada)
        double E = 30e6; // psi
        double I = PI / 64.0 * (Math.pow(params.getTubingOdInch(), 4) - 
                                Math.pow(params.getTubingIdInch(), 4));
        double L = depth * 12.0; // pulgadas
        
        return (Math.pow(PI, 2) * E * I) / Math.pow(L, 2);
    }
    
    // Clases auxiliares
    
    public static class JobParameters {
        private UUID jobId;
        private String wellName;
        private double targetDepthFt;
        private double wellboreDiameterInch;
        private double maxInclinationDeg;
        private double tubingOdInch;
        private double tubingIdInch;
        private double tubingLengthFt;
        private double fluidDensityPpg = 8.33;
        private Double pumpRateBpm;
        private double maxPressurePsi;
        private double maxRunningSpeedFtMin = 60.0;
        private double unitMaxPressurePsi;
        private double unitMaxTensionLbf;
        private Double estimatedTreatmentHours;
        
        // Getters y setters
        public UUID getJobId() { return jobId; }
        public void setJobId(UUID jobId) { this.jobId = jobId; }
        public String getWellName() { return wellName; }
        public void setWellName(String wellName) { this.wellName = wellName; }
        public double getTargetDepthFt() { return targetDepthFt; }
        public void setTargetDepthFt(double targetDepthFt) { this.targetDepthFt = targetDepthFt; }
        public double getWellboreDiameterInch() { return wellboreDiameterInch; }
        public void setWellboreDiameterInch(double wellboreDiameterInch) { 
            this.wellboreDiameterInch = wellboreDiameterInch; 
        }
        public double getMaxInclinationDeg() { return maxInclinationDeg; }
        public void setMaxInclinationDeg(double maxInclinationDeg) { 
            this.maxInclinationDeg = maxInclinationDeg; 
        }
        public double getTubingOdInch() { return tubingOdInch; }
        public void setTubingOdInch(double tubingOdInch) { this.tubingOdInch = tubingOdInch; }
        public double getTubingIdInch() { return tubingIdInch; }
        public void setTubingIdInch(double tubingIdInch) { this.tubingIdInch = tubingIdInch; }
        public double getTubingLengthFt() { return tubingLengthFt; }
        public void setTubingLengthFt(double tubingLengthFt) { this.tubingLengthFt = tubingLengthFt; }
        public double getFluidDensityPpg() { return fluidDensityPpg; }
        public void setFluidDensityPpg(double fluidDensityPpg) { this.fluidDensityPpg = fluidDensityPpg; }
        public Double getPumpRateBpm() { return pumpRateBpm; }
        public void setPumpRateBpm(Double pumpRateBpm) { this.pumpRateBpm = pumpRateBpm; }
        public double getMaxPressurePsi() { return maxPressurePsi; }
        public void setMaxPressurePsi(double maxPressurePsi) { this.maxPressurePsi = maxPressurePsi; }
        public double getMaxRunningSpeedFtMin() { return maxRunningSpeedFtMin; }
        public void setMaxRunningSpeedFtMin(double maxRunningSpeedFtMin) { 
            this.maxRunningSpeedFtMin = maxRunningSpeedFtMin; 
        }
        public double getUnitMaxPressurePsi() { return unitMaxPressurePsi; }
        public void setUnitMaxPressurePsi(double unitMaxPressurePsi) { 
            this.unitMaxPressurePsi = unitMaxPressurePsi; 
        }
        public double getUnitMaxTensionLbf() { return unitMaxTensionLbf; }
        public void setUnitMaxTensionLbf(double unitMaxTensionLbf) { 
            this.unitMaxTensionLbf = unitMaxTensionLbf; 
        }
        public Double getEstimatedTreatmentHours() { return estimatedTreatmentHours; }
        public void setEstimatedTreatmentHours(Double estimatedTreatmentHours) { 
            this.estimatedTreatmentHours = estimatedTreatmentHours; 
        }
    }
    
    public static class SimulationResult {
        private UUID jobId;
        private String wellName;
        private FeasibilityCheck feasibility;
        private ForceAnalysis forces;
        private HydraulicAnalysis hydraulics;
        private TimeEstimation times;
        private FatiguePrediction fatigue;
        private List<Risk> risks;
        private boolean success = true;
        private String errorMessage;
        
        public static SimulationResult error(String message) {
            SimulationResult result = new SimulationResult();
            result.setSuccess(false);
            result.setErrorMessage(message);
            return result;
        }
        
        // Getters y setters
        public UUID getJobId() { return jobId; }
        public void setJobId(UUID jobId) { this.jobId = jobId; }
        public String getWellName() { return wellName; }
        public void setWellName(String wellName) { this.wellName = wellName; }
        public FeasibilityCheck getFeasibility() { return feasibility; }
        public void setFeasibility(FeasibilityCheck feasibility) { this.feasibility = feasibility; }
        public ForceAnalysis getForces() { return forces; }
        public void setForces(ForceAnalysis forces) { this.forces = forces; }
        public HydraulicAnalysis getHydraulics() { return hydraulics; }
        public void setHydraulics(HydraulicAnalysis hydraulics) { this.hydraulics = hydraulics; }
        public TimeEstimation getTimes() { return times; }
        public void setTimes(TimeEstimation times) { this.times = times; }
        public FatiguePrediction getFatigue() { return fatigue; }
        public void setFatigue(FatiguePrediction fatigue) { this.fatigue = fatigue; }
        public List<Risk> getRisks() { return risks; }
        public void setRisks(List<Risk> risks) { this.risks = risks; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class FeasibilityCheck {
        private boolean feasible;
        private List<String> limitingFactors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
        public boolean isFeasible() { return feasible; }
        public void setFeasible(boolean feasible) { this.feasible = feasible; }
        public List<String> getLimitingFactors() { return limitingFactors; }
        public void setLimitingFactors(List<String> limitingFactors) { 
            this.limitingFactors = limitingFactors; 
        }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
    
    public static class ForceAnalysis {
        private List<Double> depths;
        private List<Double> hookloads;
        private List<Double> frictions;
        private List<Double> bucklingMargins;
        private double maxHookload;
        
        public List<Double> getDepths() { return depths; }
        public void setDepths(List<Double> depths) { this.depths = depths; }
        public List<Double> getHookloads() { return hookloads; }
        public void setHookloads(List<Double> hookloads) { this.hookloads = hookloads; }
        public List<Double> getFrictions() { return frictions; }
        public void setFrictions(List<Double> frictions) { this.frictions = frictions; }
        public List<Double> getBucklingMargins() { return bucklingMargins; }
        public void setBucklingMargins(List<Double> bucklingMargins) { 
            this.bucklingMargins = bucklingMargins; 
        }
        public double getMaxHookload() { return maxHookload; }
        public void setMaxHookload(double maxHookload) { this.maxHookload = maxHookload; }
    }
    
    public static class HydraulicAnalysis {
        private List<Double> depths;
        private List<Double> pressures;
        private List<Double> frictionPressures;
        private List<Double> velocities;
        private Double maxPressure;
        
        public List<Double> getDepths() { return depths; }
        public void setDepths(List<Double> depths) { this.depths = depths; }
        public List<Double> getPressures() { return pressures; }
        public void setPressures(List<Double> pressures) { this.pressures = pressures; }
        public List<Double> getFrictionPressures() { return frictionPressures; }
        public void setFrictionPressures(List<Double> frictionPressures) { 
            this.frictionPressures = frictionPressures; 
        }
        public List<Double> getVelocities() { return velocities; }
        public void setVelocities(List<Double> velocities) { this.velocities = velocities; }
        public Double getMaxPressure() { return maxPressure; }
        public void setMaxPressure(Double maxPressure) { this.maxPressure = maxPressure; }
    }
    
    public static class TimeEstimation {
        private double riggingUpHours;
        private double runningInHours;
        private double onDepthHours;
        private double pullingOutHours;
        private double riggingDownHours;
        private double totalDurationHours;
        
        public double getRiggingUpHours() { return riggingUpHours; }
        public void setRiggingUpHours(double riggingUpHours) { this.riggingUpHours = riggingUpHours; }
        public double getRunningInHours() { return runningInHours; }
        public void setRunningInHours(double runningInHours) { this.runningInHours = runningInHours; }
        public double getOnDepthHours() { return onDepthHours; }
        public void setOnDepthHours(double onDepthHours) { this.onDepthHours = onDepthHours; }
        public double getPullingOutHours() { return pullingOutHours; }
        public void setPullingOutHours(double pullingOutHours) { this.pullingOutHours = pullingOutHours; }
        public double getRiggingDownHours() { return riggingDownHours; }
        public void setRiggingDownHours(double riggingDownHours) { 
            this.riggingDownHours = riggingDownHours; 
        }
        public double getTotalDurationHours() { return totalDurationHours; }
        public void setTotalDurationHours(double totalDurationHours) { 
            this.totalDurationHours = totalDurationHours; 
        }
    }
    
    public static class FatiguePrediction {
        private int estimatedCycles;
        private double estimatedFatiguePercent;
        private double averageStressPsi;
        
        public int getEstimatedCycles() { return estimatedCycles; }
        public void setEstimatedCycles(int estimatedCycles) { this.estimatedCycles = estimatedCycles; }
        public double getEstimatedFatiguePercent() { return estimatedFatiguePercent; }
        public void setEstimatedFatiguePercent(double estimatedFatiguePercent) { 
            this.estimatedFatiguePercent = estimatedFatiguePercent; 
        }
        public double getAverageStressPsi() { return averageStressPsi; }
        public void setAverageStressPsi(double averageStressPsi) { 
            this.averageStressPsi = averageStressPsi; 
        }
    }
    
    public static class Risk {
        private String severity;
        private String type;
        private String description;
        
        public Risk(String severity, String type, String description) {
            this.severity = severity;
            this.type = type;
            this.description = description;
        }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
