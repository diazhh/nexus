# PO MODULE SPECIFICATION - Production Optimization

**Módulo**: Production Optimization (PO)
**Versión**: 1.0
**Fecha**: 2026-02-03
**Propósito**: Optimización inteligente de operaciones de producción con Machine Learning

---

## 📋 Tabla de Contenidos

1. [Overview](#overview)
2. [Arquitectura del Módulo](#arquitectura)
3. [Optimizadores](#optimizadores)
4. [Machine Learning](#machine-learning)
5. [Sistema de Recomendaciones](#recomendaciones)
6. [KPIs y Analytics](#kpis)
7. [Control y Automatización](#control)
8. [APIs REST](#apis)

---

## 1. Overview {#overview}

### 1.1 Propósito

El módulo PO proporciona optimización inteligente de:
- Parámetros operacionales de levantamiento artificial
- Distribución de recursos limitados (gas, diluentes)
- Predicción de fallas de equipos
- Recomendaciones automatizadas
- Control en lazo cerrado (fase avanzada)

### 1.2 Dependencias

```
PO Module
    ↓ Requiere datos de
PF Module (Telemetría, Estados, Alarmas)
    ↓ Usa caracterización de
RV Module (IPR, PVT, Propiedades)
```

### 1.3 Responsabilidades

✅ **Lo que hace el módulo PO**:
- Calcular setpoints óptimos para ESP, PCP, Gas Lift
- Predecir fallas de equipos con ML
- Detectar anomalías en tiempo real
- Calcular Health Score de equipos
- Generar recomendaciones con simulación de impacto
- Controlar cambios de setpoints (con aprobación)

❌ **Lo que NO hace**:
- Monitoreo en tiempo real (eso es PF Module)
- Captura de telemetría (eso es PF Module)
- Gestión de alarmas básicas (eso es PF Module)

---

## 2. Arquitectura del Módulo {#arquitectura}

### 2.1 Estructura de Paquetes

```
org.thingsboard.server
├── common
│   └── data
│       └── po
│           ├── entity
│           │   ├── PoRecommendation.java
│           │   ├── PoOptimization.java
│           │   ├── PoHealthScore.java
│           │   ├── PoKpi.java
│           │   └── PoSetpointChange.java
│           ├── dto
│           │   ├── PoRecommendationDto.java
│           │   ├── OptimizationResultDto.java
│           │   ├── HealthScoreDto.java
│           │   └── KpiDto.java
│           └── ml
│               ├── ModelMetrics.java
│               ├── PredictionResult.java
│               └── FeatureVector.java
│
├── dao
│   └── po
│       ├── PoRecommendationDao.java
│       ├── PoHealthScoreDao.java
│       └── PoKpiDao.java
│
└── service
    └── po
        ├── optimizer
        │   ├── EspFrequencyOptimizer.java
        │   ├── GasLiftAllocator.java
        │   ├── DiluentOptimizer.java
        │   └── PcpRpmController.java
        ├── ml
        │   ├── FailurePredictionService.java
        │   ├── AnomalyDetectionService.java
        │   ├── ModelTrainingService.java
        │   └── FeatureEngineeringService.java
        ├── analytics
        │   ├── HealthScoreCalculator.java
        │   ├── RootCauseAnalyzer.java
        │   └── BenchmarkingService.java
        ├── recommendation
        │   ├── RecommendationEngine.java
        │   ├── ImpactSimulator.java
        │   ├── ApprovalWorkflow.java
        │   └── EffectivenessTracker.java
        └── control
            ├── SetpointController.java
            ├── ClosedLoopController.java
            └── SafetyInterlockService.java
```

### 2.2 Arquitectura de Componentes

```
┌─────────────────────────────────────────────────────────────┐
│                     PO MODULE                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         RECOMMENDATION ENGINE                        │  │
│  │  • Generate recommendations                          │  │
│  │  • Simulate impact                                   │  │
│  │  • Prioritize by value                               │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                     │
│         ┌─────────────┼─────────────┐                      │
│         │             │             │                      │
│         ▼             ▼             ▼                      │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐                │
│  │ OPTIMIZER │ │    ML     │ │ ANALYTICS │                │
│  │  ENGINE   │ │  MODELS   │ │  ENGINE   │                │
│  │           │ │           │ │           │                │
│  │ • ESP     │ │• Failure  │ │• Health   │                │
│  │ • PCP     │ │  Predict  │ │  Score    │                │
│  │ • Gas Lift│ │• Anomaly  │ │• Root     │                │
│  │ • Diluent │ │  Detect   │ │  Cause    │                │
│  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘                │
│        │             │             │                      │
│        └─────────────┼─────────────┘                      │
│                      │                                     │
│                      ▼                                     │
│           ┌──────────────────────┐                         │
│           │   DATA ACCESS        │                         │
│           │   PF Module APIs     │                         │
│           │   RV Module APIs     │                         │
│           └──────────────────────┘                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Optimizadores {#optimizadores}

### 3.1 ESP Frequency Optimizer

#### Objetivo
Encontrar la frecuencia óptima que maximice producción sin comprometer vida útil del equipo.

#### Modelo de Optimización

```java
@Service
@Slf4j
public class EspFrequencyOptimizer {

    @Autowired
    private PfWellService wellService;

    @Autowired
    private PfTelemetryService telemetryService;

    @Autowired
    private RvWellService rvWellService;

    /**
     * Optimize ESP frequency for a well
     */
    public OptimizationResult optimizeFrequency(UUID wellId) {

        // 1. Get current state
        PfWell well = wellService.getWell(wellId).orElseThrow();
        PfEspSystem esp = well.getEspSystem();
        TelemetryData currentTelemetry = telemetryService.getLatest(wellId);

        // 2. Get reservoir data from RV
        RvWell rvWell = rvWellService.getWell(well.getRvWellId()).orElseThrow();
        IprCurve ipr = rvWell.getIprCurve();
        PvtProperties pvt = rvWell.getPvtProperties();

        // 3. Evaluate current state
        StateEvaluation currentState = evaluateState(currentTelemetry, esp);

        if (!currentState.isWithinSafeLimits()) {
            // PROTECTION MODE: reduce frequency
            return generateProtectionRecommendation(currentState);
        }

        // 4. Calculate efficiency
        double currentEfficiency = calculateEfficiency(
            currentTelemetry.getProduction(),
            ipr.getPotentialAtPwf(currentTelemetry.getPip()),
            currentTelemetry.getFrequency()
        );

        // 5. Determine opportunity
        OptimizationOpportunity opportunity =
            identifyOpportunity(currentEfficiency, currentState);

        if (opportunity.getType() == OpportunityType.INCREASE_FREQUENCY) {
            // Try higher frequency
            double proposedFreq = currentTelemetry.getFrequency() + 2.0;
            return simulateFrequencyChange(wellId, proposedFreq, ipr, pvt, esp);

        } else if (opportunity.getType() == OpportunityType.DECREASE_FREQUENCY) {
            // Reduce frequency (possible gas interference)
            double proposedFreq = currentTelemetry.getFrequency() - 3.0;
            return simulateFrequencyChange(wellId, proposedFreq, ipr, pvt, esp);

        } else {
            // No action needed
            return OptimizationResult.noAction("Well operating optimally");
        }
    }

    /**
     * Simulate impact of frequency change
     */
    private OptimizationResult simulateFrequencyChange(
            UUID wellId, double newFrequency, IprCurve ipr,
            PvtProperties pvt, PfEspSystem esp) {

        // Get pump curve for this ESP
        PumpCurve pumpCurve = esp.getPumpCurve();

        // Calculate new operating point using nodal analysis
        NodalAnalysisResult nodal = NodalAnalysis.solve(
            ipr,
            pumpCurve,
            newFrequency,
            esp.getStages(),
            pvt
        );

        // Estimate production
        double estimatedProduction = nodal.getFlowRate();

        // Estimate motor temperature
        double estimatedTemp = estimateMotorTemperature(
            newFrequency,
            nodal.getPumpLoad(),
            pvt.getFluidTemperature()
        );

        // Calculate economic benefit
        double currentProduction = getCurrentProduction(wellId);
        double productionGain = estimatedProduction - currentProduction;
        double revenueGain = productionGain * getOilPrice(); // $/day

        // Calculate energy cost increase
        double energyCostIncrease = calculateEnergyCost(newFrequency)
                                  - calculateEnergyCost(getCurrentFrequency(wellId));

        double netBenefit = revenueGain - energyCostIncrease;

        // Build result
        return OptimizationResult.builder()
            .wellId(wellId)
            .optimizationType("ESP_FREQUENCY")
            .currentValue(getCurrentFrequency(wellId))
            .recommendedValue(newFrequency)
            .estimatedProduction(estimatedProduction)
            .productionGain(productionGain)
            .estimatedMotorTemp(estimatedTemp)
            .netBenefit(netBenefit)
            .confidence(calculateConfidence(nodal))
            .risks(identifyRisks(estimatedTemp, nodal))
            .recommendedActions(generateActions(newFrequency))
            .build();
    }

    /**
     * Evaluate current state
     */
    private StateEvaluation evaluateState(TelemetryData telemetry, PfEspSystem esp) {
        StateEvaluation eval = new StateEvaluation();

        // Check temperature
        if (telemetry.getMotorTempF() > esp.getMaxMotorTempF()) {
            eval.addViolation("MOTOR_TEMP_HIGH",
                String.format("Motor temp %.1f°F exceeds limit %.1f°F",
                    telemetry.getMotorTempF(), esp.getMaxMotorTempF()));
        }

        // Check current
        double currentPercent = telemetry.getCurrentAmps() / esp.getMaxCurrentAmps() * 100;
        if (currentPercent > 110) {
            eval.addViolation("CURRENT_HIGH",
                String.format("Motor current %.1f%% of nominal", currentPercent));
        } else if (currentPercent < 60) {
            eval.addViolation("CURRENT_LOW",
                String.format("Motor current %.1f%% (possible gas)", currentPercent));
        }

        // Check PIP
        if (telemetry.getPipPsi() < esp.getMinPipPsi()) {
            eval.addViolation("PIP_LOW",
                String.format("PIP %.1f psi below minimum %.1f psi (gas lock risk)",
                    telemetry.getPipPsi(), esp.getMinPipPsi()));
        }

        // Check vibration
        if (telemetry.getVibrationG() > esp.getMaxVibrationG()) {
            eval.addViolation("VIBRATION_HIGH",
                String.format("Vibration %.2fg exceeds limit %.2fg",
                    telemetry.getVibrationG(), esp.getMaxVibrationG()));
        }

        return eval;
    }
}
```

#### Lógica de Decisión

```
┌─────────────────────────────────────────────────────────────┐
│            ESP FREQUENCY OPTIMIZATION LOGIC                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Step 1: Evaluate Safety                                   │
│  ────────────────────────                                  │
│  IF temperature > 280°F  → PROTECTION MODE                 │
│  IF current > 110% rated → PROTECTION MODE                 │
│  IF PIP < 150 psi        → PROTECTION MODE                 │
│  IF vibration > 1.0g     → PROTECTION MODE                 │
│                                                             │
│  Step 2: Calculate Efficiency                              │
│  ─────────────────────────                                 │
│  Efficiency = Actual Production / Potential Production     │
│                                                             │
│  Step 3: Identify Opportunity                              │
│  ──────────────────────────                                │
│  IF efficiency > 90% AND stable conditions:                │
│      → Consider INCREASE frequency (+2-5 Hz)               │
│                                                             │
│  IF efficiency < 70%:                                      │
│      → Consider DECREASE frequency (possible gas)          │
│                                                             │
│  IF 70% < efficiency < 90%:                                │
│      → Maintain and monitor                                │
│                                                             │
│  Step 4: Simulate Impact                                   │
│  ────────────────────────                                  │
│  • Run nodal analysis with new frequency                   │
│  • Estimate new production                                 │
│  • Estimate new motor temperature                          │
│  • Calculate net economic benefit                          │
│                                                             │
│  Step 5: Risk Assessment                                   │
│  ────────────────────────                                  │
│  • Check if estimated temp stays < 280°F                   │
│  • Check if current stays in range                         │
│  • Calculate confidence based on model accuracy            │
│                                                             │
│  Step 6: Generate Recommendation                           │
│  ──────────────────────────────                            │
│  • Recommended frequency                                   │
│  • Expected benefits                                       │
│  • Confidence level                                        │
│  • Associated risks                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Gas Lift Allocator

#### Objetivo
Distribuir gas de inyección limitado entre N pozos para maximizar producción total del campo.

#### Algoritmo de Optimización

```java
@Service
@Slf4j
public class GasLiftAllocator {

    /**
     * Optimize gas lift allocation across wells
     */
    public GasLiftAllocationResult optimizeAllocation(
            List<UUID> wellIds, double totalGasAvailable) {

        // 1. Get current allocation
        Map<UUID, Double> currentAllocation = getCurrentAllocation(wellIds);

        // 2. Get response curves for each well
        Map<UUID, GasLiftResponseCurve> responseCurves = new HashMap<>();
        for (UUID wellId : wellIds) {
            responseCurves.put(wellId, buildResponseCurve(wellId));
        }

        // 3. Run optimization algorithm
        Map<UUID, Double> optimalAllocation = optimizeUsingMarginalAnalysis(
            responseCurves,
            totalGasAvailable
        );

        // 4. Calculate impact
        double currentProduction = calculateTotalProduction(
            wellIds, currentAllocation, responseCurves);

        double optimalProduction = calculateTotalProduction(
            wellIds, optimalAllocation, responseCurves);

        double productionGain = optimalProduction - currentProduction;

        // 5. Build result
        return GasLiftAllocationResult.builder()
            .currentAllocation(currentAllocation)
            .optimalAllocation(optimalAllocation)
            .currentProduction(currentProduction)
            .optimalProduction(optimalProduction)
            .productionGain(productionGain)
            .valueGain(productionGain * getOilPrice())
            .wellChanges(calculateWellChanges(currentAllocation, optimalAllocation))
            .build();
    }

    /**
     * Optimize using marginal analysis (greedy algorithm)
     *
     * Algorithm:
     * 1. Start with zero gas allocation
     * 2. Iteratively allocate gas increment to well with highest marginal gain
     * 3. Repeat until all gas is allocated
     */
    private Map<UUID, Double> optimizeUsingMarginalAnalysis(
            Map<UUID, GasLiftResponseCurve> curves,
            double totalGas) {

        Map<UUID, Double> allocation = new HashMap<>();
        curves.keySet().forEach(wellId -> allocation.put(wellId, 0.0));

        double gasIncrement = 0.1; // MMscfd
        double remainingGas = totalGas;

        while (remainingGas >= gasIncrement) {
            // Find well with highest marginal gain
            UUID bestWell = null;
            double bestGain = 0.0;

            for (Map.Entry<UUID, GasLiftResponseCurve> entry : curves.entrySet()) {
                UUID wellId = entry.getKey();
                GasLiftResponseCurve curve = entry.getValue();

                double currentGas = allocation.get(wellId);
                double currentProd = curve.getProduction(currentGas);
                double newProd = curve.getProduction(currentGas + gasIncrement);

                double marginalGain = newProd - currentProd;

                if (marginalGain > bestGain) {
                    bestGain = marginalGain;
                    bestWell = wellId;
                }
            }

            if (bestWell != null) {
                allocation.put(bestWell, allocation.get(bestWell) + gasIncrement);
                remainingGas -= gasIncrement;
            } else {
                break; // No more gains possible
            }
        }

        return allocation;
    }

    /**
     * Build response curve for a well
     */
    private GasLiftResponseCurve buildResponseCurve(UUID wellId) {
        // Get historical data
        List<ProductionTest> tests = getProductionTests(wellId);

        // Fit curve (polynomial or piecewise linear)
        // Q_oil = f(Q_gas_injected)

        return CurveFitting.fitGasLiftCurve(tests);
    }
}
```

### 3.3 Diluent Optimizer

#### Objetivo
Minimizar uso de diluente manteniendo producción objetivo (crudos pesados).

```java
@Service
public class DiluentOptimizer {

    /**
     * Optimize diluent injection rate
     */
    public DiluentOptimizationResult optimize(UUID wellId) {

        // Get well data
        PfWell well = wellService.getWell(wellId).orElseThrow();
        RvWell rvWell = rvWellService.getWell(well.getRvWellId()).orElseThrow();

        // Get current state
        double currentDiluentRate = getCurrentDiluentRate(wellId);
        double currentProduction = getCurrentProduction(wellId);
        double currentViscosity = getCurrentViscosity(wellId);

        // Get reservoir properties
        double heavyOilViscosity = rvWell.getPvt().getViscosityCp();
        double heavyOilApi = rvWell.getPvt().getApiGravity();

        // Get diluent properties
        double diluentViscosity = getDiluentViscosity();
        double diluentApi = getDiluentApi();

        // Get economic data
        double heavyOilPrice = getHeavyOilPrice();
        double diluentCost = getDiluentCost();
        double blendPrice = getBlendPrice(heavyOilApi, diluentApi, currentDiluentRate);

        // Optimization: minimize diluent subject to constraints
        OptimizationProblem problem = OptimizationProblem.builder()
            .objectiveFunction(diluentRate -> {
                // Minimize: diluent_cost - blend_penalty
                double diluentExpense = diluentRate * diluentCost;
                double blendValue = calculateBlendValue(diluentRate);
                return diluentExpense - blendValue; // minimize this
            })
            .addConstraint("production", diluentRate -> {
                // Production must be >= 95% of current
                double estimatedProd = estimateProduction(wellId, diluentRate);
                return estimatedProd >= currentProduction * 0.95;
            })
            .addConstraint("viscosity", diluentRate -> {
                // Blend viscosity must be pumpable (<500 cp)
                double blendViscosity = calculateBlendViscosity(
                    heavyOilViscosity, diluentViscosity, diluentRate);
                return blendViscosity <= 500;
            })
            .addConstraint("api", diluentRate -> {
                // Blend API must meet minimum spec (> 18°)
                double blendApi = calculateBlendApi(
                    heavyOilApi, diluentApi, diluentRate);
                return blendApi >= 18.0;
            })
            .lowerBound(0.0)
            .upperBound(currentDiluentRate * 2.0)
            .build();

        // Solve
        double optimalDiluentRate = NumericalOptimizer.solve(problem);

        // Calculate savings
        double savings = (currentDiluentRate - optimalDiluentRate) * diluentCost;

        return DiluentOptimizationResult.builder()
            .wellId(wellId)
            .currentRate(currentDiluentRate)
            .optimalRate(optimalDiluentRate)
            .savings(savings)
            .build();
    }
}
```

---

## 4. Machine Learning {#machine-learning}

### 4.1 Failure Prediction Model

#### Arquitectura del Modelo

```python
# ml-service/models/esp_failure_predictor.py

import tensorflow as tf
from tensorflow.keras import layers, Model
import numpy as np

class EspFailurePredictor:
    """
    Predicts ESP failure probability using LSTM neural network

    Input: Time series of 7 days (168 hours) with 10 features
    Output: Binary classification (fail in next 14 days: yes/no)
    """

    def __init__(self):
        self.model = None
        self.feature_scaler = None
        self.lookback_hours = 168  # 7 days
        self.features = [
            'frequency_hz',
            'current_amps',
            'motor_temp_f',
            'intake_temp_f',
            'pip_psi',
            'discharge_pressure_psi',
            'vibration_g',
            'production_bpd',
            'bsw_percent',
            'gor_scf_stb'
        ]

    def build_model(self):
        """Build LSTM model architecture"""

        input_layer = layers.Input(shape=(self.lookback_hours, len(self.features)))

        # First LSTM layer
        x = layers.LSTM(64, return_sequences=True)(input_layer)
        x = layers.Dropout(0.2)(x)

        # Second LSTM layer
        x = layers.LSTM(32, return_sequences=False)(x)
        x = layers.Dropout(0.2)(x)

        # Dense layers
        x = layers.Dense(16, activation='relu')(x)
        x = layers.Dropout(0.1)(x)

        # Output layer
        output = layers.Dense(1, activation='sigmoid')(x)

        model = Model(inputs=input_layer, outputs=output)

        model.compile(
            optimizer='adam',
            loss='binary_crossentropy',
            metrics=['accuracy', 'precision', 'recall', 'AUC']
        )

        self.model = model
        return model

    def train(self, X_train, y_train, X_val, y_val, epochs=50):
        """Train the model"""

        # Class weights (handle imbalanced dataset)
        class_weight = {
            0: 1.0,  # No failure
            1: 10.0  # Failure (rare event, weight more)
        }

        # Callbacks
        early_stop = tf.keras.callbacks.EarlyStopping(
            monitor='val_loss',
            patience=10,
            restore_best_weights=True
        )

        reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=5,
            min_lr=0.00001
        )

        # Train
        history = self.model.fit(
            X_train, y_train,
            validation_data=(X_val, y_val),
            epochs=epochs,
            batch_size=32,
            class_weight=class_weight,
            callbacks=[early_stop, reduce_lr],
            verbose=1
        )

        return history

    def predict(self, X):
        """Predict failure probability"""

        # X shape: (n_samples, lookback_hours, n_features)
        probabilities = self.model.predict(X)

        return probabilities

    def predict_well(self, well_id, telemetry_data):
        """
        Predict failure for a specific well

        Args:
            well_id: Well UUID
            telemetry_data: DataFrame with last 7 days of hourly data

        Returns:
            dict with failure_probability, days_to_failure, confidence
        """

        # Prepare features
        X = self.prepare_features(telemetry_data)

        # Predict
        probability = float(self.model.predict(X)[0][0])

        # Estimate days to failure
        if probability > 0.5:
            days_to_failure = self.estimate_days_to_failure(probability, telemetry_data)
        else:
            days_to_failure = None

        # Calculate confidence
        confidence = self.calculate_confidence(X, telemetry_data)

        return {
            'well_id': well_id,
            'failure_probability': probability,
            'days_to_failure': days_to_failure,
            'confidence': confidence,
            'timestamp': pd.Timestamp.now().isoformat()
        }

    def estimate_days_to_failure(self, probability, telemetry_data):
        """
        Estimate days until failure based on probability and trend

        Uses exponential decay model:
        days = k * ln(1/p) where p = probability, k = constant
        """

        if probability < 0.5:
            return None

        # Base estimate from probability
        k = 20  # constant calibrated from historical data
        base_estimate = k * np.log(1.0 / probability)

        # Adjust based on trend
        temp_trend = self.calculate_trend(telemetry_data['motor_temp_f'])
        vib_trend = self.calculate_trend(telemetry_data['vibration_g'])

        if temp_trend > 0.1 or vib_trend > 0.05:
            # Accelerating degradation
            adjustment = 0.7
        else:
            adjustment = 1.0

        days = base_estimate * adjustment

        return max(1, min(30, int(days)))  # Clamp to [1, 30] days
```

#### Feature Engineering

```python
# ml-service/training/feature_engineering.py

class FeatureEngineer:
    """
    Extract features for ML models
    """

    @staticmethod
    def extract_statistical_features(telemetry_df):
        """
        Extract statistical features from time series
        """
        features = {}

        for column in telemetry_df.columns:
            if column == 'timestamp':
                continue

            values = telemetry_df[column].values

            features[f'{column}_mean'] = np.mean(values)
            features[f'{column}_std'] = np.std(values)
            features[f'{column}_min'] = np.min(values)
            features[f'{column}_max'] = np.max(values)
            features[f'{column}_range'] = np.ptp(values)

            # Trend (slope of linear regression)
            x = np.arange(len(values))
            slope, _ = np.polyfit(x, values, 1)
            features[f'{column}_trend'] = slope

            # Volatility (coefficient of variation)
            if features[f'{column}_mean'] != 0:
                features[f'{column}_cv'] = features[f'{column}_std'] / features[f'{column}_mean']
            else:
                features[f'{column}_cv'] = 0.0

        return features

    @staticmethod
    def extract_domain_features(telemetry_df):
        """
        Extract domain-specific features for ESP
        """
        features = {}

        # Motor loading indicator
        current = telemetry_df['current_amps'].values
        rated_current = 50.0  # nominal
        features['motor_loading_percent'] = np.mean(current / rated_current * 100)

        # Temperature differential
        motor_temp = telemetry_df['motor_temp_f'].values
        intake_temp = telemetry_df['intake_temp_f'].values
        features['temp_differential_f'] = np.mean(motor_temp - intake_temp)

        # Specific energy (kWh/bbl)
        power_kw = telemetry_df['power_kw'].values if 'power_kw' in telemetry_df else None
        production = telemetry_df['production_bpd'].values
        if power_kw is not None:
            features['specific_energy_kwh_per_bbl'] = np.mean(power_kw * 24 / production)

        # Efficiency indicator
        frequency = telemetry_df['frequency_hz'].values
        features['frequency_utilization_percent'] = np.mean(frequency / 60.0 * 100)

        return features
```

### 4.2 Anomaly Detection

```python
# ml-service/models/anomaly_detector.py

from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
import numpy as np

class AnomalyDetector:
    """
    Detect anomalies in ESP telemetry using Isolation Forest
    """

    def __init__(self, contamination=0.05):
        self.model = IsolationForest(
            n_estimators=100,
            contamination=contamination,
            random_state=42
        )
        self.scaler = StandardScaler()
        self.feature_names = None

    def train(self, normal_data):
        """
        Train on normal operating data

        Args:
            normal_data: DataFrame with features from normal operation
        """

        # Store feature names
        self.feature_names = normal_data.columns.tolist()

        # Scale features
        X_scaled = self.scaler.fit_transform(normal_data)

        # Train
        self.model.fit(X_scaled)

    def predict(self, new_data):
        """
        Predict if new data points are anomalies

        Returns:
            -1 for anomaly, 1 for normal
        """

        X_scaled = self.scaler.transform(new_data)
        predictions = self.model.predict(X_scaled)

        # Get anomaly scores
        scores = self.model.score_samples(X_scaled)

        return predictions, scores

    def detect_anomaly(self, telemetry_data):
        """
        Detect anomaly in real-time telemetry
        """

        # Extract features
        features = self.extract_features(telemetry_data)

        # Predict
        prediction, score = self.predict([features])

        is_anomaly = prediction[0] == -1
        anomaly_score = -score[0]  # Convert to positive score

        # Identify which features are anomalous
        if is_anomaly:
            anomalous_features = self.identify_anomalous_features(
                features, telemetry_data)
        else:
            anomalous_features = []

        return {
            'is_anomaly': bool(is_anomaly),
            'anomaly_score': float(anomaly_score),
            'anomalous_features': anomalous_features,
            'timestamp': pd.Timestamp.now().isoformat()
        }
```

---

## 5. Sistema de Recomendaciones {#recomendaciones}

### 5.1 Recommendation Engine

```java
@Service
@Slf4j
public class RecommendationEngine {

    @Autowired
    private EspFrequencyOptimizer espOptimizer;

    @Autowired
    private GasLiftAllocator gasLiftAllocator;

    @Autowired
    private DiluentOptimizer diluentOptimizer;

    @Autowired
    private ImpactSimulator impactSimulator;

    @Autowired
    private PoRecommendationDao recommendationDao;

    /**
     * Generate recommendations for all wells
     */
    @Scheduled(cron = "0 0 7 * * *")  // Daily at 7 AM
    public void generateDailyRecommendations() {

        log.info("Starting daily recommendation generation");

        // Get all active wells
        List<PfWell> wells = wellService.findAllActive();

        List<PoRecommendation> recommendations = new ArrayList<>();

        for (PfWell well : wells) {
            try {
                // Generate recommendations based on lift system type
                List<PoRecommendation> wellRecs =
                    generateRecommendationsForWell(well);

                recommendations.addAll(wellRecs);

            } catch (Exception e) {
                log.error("Error generating recommendations for well {}",
                    well.getId(), e);
            }
        }

        // Prioritize by economic value
        recommendations.sort(Comparator.comparing(
            PoRecommendation::getNetBenefit).reversed());

        // Save top recommendations
        recommendations.stream()
            .limit(50)
            .forEach(recommendationDao::save);

        log.info("Generated {} recommendations", recommendations.size());
    }

    /**
     * Generate recommendations for a specific well
     */
    public List<PoRecommendation> generateRecommendationsForWell(PfWell well) {

        List<PoRecommendation> recommendations = new ArrayList<>();

        // Based on lift system type
        switch (well.getLiftSystemType()) {
            case ESP:
                OptimizationResult espResult = espOptimizer.optimizeFrequency(well.getId());
                if (espResult.hasActionRequired()) {
                    recommendations.add(buildRecommendation(well, espResult));
                }
                break;

            case GAS_LIFT:
                // Gas lift is optimized at field level, not individual well
                break;

            case PCP:
                // PCP RPM optimization
                OptimizationResult pcpResult = pcpOptimizer.optimizeRpm(well.getId());
                if (pcpResult.hasActionRequired()) {
                    recommendations.add(buildRecommendation(well, pcpResult));
                }
                break;

            case DILUENT_INJECTION:
                DiluentOptimizationResult diluentResult =
                    diluentOptimizer.optimize(well.getId());
                if (diluentResult.hasSavings()) {
                    recommendations.add(buildRecommendation(well, diluentResult));
                }
                break;
        }

        return recommendations;
    }

    /**
     * Build recommendation entity from optimization result
     */
    private PoRecommendation buildRecommendation(
            PfWell well, OptimizationResult result) {

        // Simulate impact
        ImpactSimulation simulation = impactSimulator.simulate(well, result);

        return PoRecommendation.builder()
            .wellId(well.getId())
            .wellName(well.getName())
            .recommendationType(result.getOptimizationType())
            .currentValue(result.getCurrentValue())
            .recommendedValue(result.getRecommendedValue())
            .expectedBenefit(result.getNetBenefit())
            .confidence(result.getConfidence())
            .priority(calculatePriority(result))
            .status(RecommendationStatus.PENDING)
            .simulation(simulation)
            .risks(result.getRisks())
            .recommendedActions(result.getRecommendedActions())
            .createdTime(System.currentTimeMillis())
            .expiresAt(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))
            .build();
    }
}
```

### 5.2 Approval Workflow

```java
@Service
public class ApprovalWorkflow {

    /**
     * Determine approval level required based on risk
     */
    public ApprovalLevel getRequiredApprovalLevel(PoRecommendation recommendation) {

        double benefitPerDay = recommendation.getExpectedBenefit();
        double confidence = recommendation.getConfidence();
        List<String> risks = recommendation.getRisks();

        // Level 1: Automatic (low risk, proven algorithm)
        if (benefitPerDay < 500 &&
            confidence > 0.9 &&
            risks.isEmpty() &&
            isProvenWell(recommendation.getWellId())) {

            return ApprovalLevel.AUTOMATIC;
        }

        // Level 2: Supervisor approval
        if (benefitPerDay < 2000 &&
            confidence > 0.75 &&
            risks.size() <= 1) {

            return ApprovalLevel.SUPERVISOR;
        }

        // Level 3: Engineer approval
        if (benefitPerDay < 10000 &&
            confidence > 0.6) {

            return ApprovalLevel.ENGINEER;
        }

        // Level 4: Manager approval
        return ApprovalLevel.MANAGER;
    }

    /**
     * Submit recommendation for approval
     */
    public void submitForApproval(UUID recommendationId, UUID userId) {

        PoRecommendation rec = recommendationDao.findById(recommendationId)
            .orElseThrow();

        ApprovalLevel required = getRequiredApprovalLevel(rec);

        rec.setStatus(RecommendationStatus.PENDING_APPROVAL);
        rec.setRequiredApprovalLevel(required);
        rec.setSubmittedBy(userId);
        rec.setSubmittedAt(System.currentTimeMillis());

        recommendationDao.save(rec);

        // Notify approver
        notificationService.notifyApprovalRequired(rec, required);
    }

    /**
     * Approve recommendation
     */
    public void approve(UUID recommendationId, UUID approverId, String comments) {

        PoRecommendation rec = recommendationDao.findById(recommendationId)
            .orElseThrow();

        // Verify approver has authority
        if (!hasApprovalAuthority(approverId, rec.getRequiredApprovalLevel())) {
            throw new InsufficientAuthorityException();
        }

        rec.setStatus(RecommendationStatus.APPROVED);
        rec.setApprovedBy(approverId);
        rec.setApprovedAt(System.currentTimeMillis());
        rec.setApprovalComments(comments);

        recommendationDao.save(rec);

        // Execute recommendation
        setpointController.executeRecommendation(rec);
    }
}
```

---

## 6. KPIs y Analytics {#kpis}

### 6.1 KPI Calculator

```java
@Service
public class ProductionKpiCalculator {

    /**
     * Calculate production KPIs for a well
     */
    public ProductionKpi calculateKpis(UUID wellId, LocalDate date) {

        // Get production data for the day
        ProductionData prodData = productionService.getForDate(wellId, date);

        // Get potential from RV module
        double potential = rvService.getPotentialProduction(wellId, date);

        // Calculate metrics
        double efficiency = prodData.getOilBpd() / potential * 100.0;

        double uptime = prodData.getHoursProducing() / 24.0 * 100.0;

        double deferment = Math.max(0, potential - prodData.getOilBpd());

        return ProductionKpi.builder()
            .wellId(wellId)
            .date(date)
            .oilProductionBpd(prodData.getOilBpd())
            .gasProductionMcfd(prodData.getGasMcfd())
            .waterProductionBpd(prodData.getWaterBpd())
            .potential(potential)
            .efficiency(efficiency)
            .uptime(uptime)
            .deferment(deferment)
            .bsw(prodData.getBsw())
            .gor(prodData.getGor())
            .build();
    }

    /**
     * Calculate field-level KPIs
     */
    public FieldKpi calculateFieldKpis(UUID fieldId, LocalDate date) {

        List<PfWell> wells = wellService.findByField(fieldId);

        double totalProduction = 0;
        double totalPotential = 0;
        double totalDeferment = 0;
        int totalWells = wells.size();
        int producingWells = 0;

        for (PfWell well : wells) {
            ProductionKpi wellKpi = calculateKpis(well.getId(), date);

            totalProduction += wellKpi.getOilProductionBpd();
            totalPotential += wellKpi.getPotential();
            totalDeferment += wellKpi.getDeferment();

            if (well.getStatus() == WellStatus.PRODUCING) {
                producingWells++;
            }
        }

        double fieldEfficiency = totalProduction / totalPotential * 100.0;
        double availability = (double) producingWells / totalWells * 100.0;

        return FieldKpi.builder()
            .fieldId(fieldId)
            .date(date)
            .totalProduction(totalProduction)
            .totalPotential(totalPotential)
            .totalDeferment(totalDeferment)
            .efficiency(fieldEfficiency)
            .availability(availability)
            .wellCount(totalWells)
            .producingWells(producingWells)
            .build();
    }
}
```

---

## 7. Control y Automatización {#control}

### 7.1 Setpoint Controller

```java
@Service
@Slf4j
public class SetpointController {

    @Autowired
    private ScadaIntegrationService scadaService;

    @Autowired
    private SafetyInterlockService safetyService;

    /**
     * Execute recommendation by sending setpoint to SCADA
     */
    public SetpointChangeResult executeRecommendation(PoRecommendation recommendation) {

        // Safety checks
        if (!safetyService.isChangeAllowed(recommendation)) {
            throw new SafetyInterlockException("Safety interlock prevents change");
        }

        // Send to SCADA
        try {
            ScadaCommand command = buildScadaCommand(recommendation);

            scadaService.sendCommand(command);

            log.info("Setpoint change sent to SCADA: well={}, type={}, value={}",
                recommendation.getWellId(),
                recommendation.getRecommendationType(),
                recommendation.getRecommendedValue());

            // Monitor response
            return monitorSetpointChange(recommendation);

        } catch (Exception e) {
            log.error("Failed to execute recommendation", e);
            throw new SetpointChangeException("Failed to send command to SCADA", e);
        }
    }

    /**
     * Monitor setpoint change and verify response
     */
    private SetpointChangeResult monitorSetpointChange(PoRecommendation recommendation) {

        UUID wellId = recommendation.getWellId();
        String variable = getVariableName(recommendation.getRecommendationType());
        double targetValue = recommendation.getRecommendedValue();

        // Wait for change to take effect (30 seconds)
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify new value
        TelemetryData telemetry = telemetryService.getLatest(wellId);
        double actualValue = telemetry.getValue(variable);

        boolean success = Math.abs(actualValue - targetValue) < 0.5;

        if (!success) {
            log.warn("Setpoint change failed: expected={}, actual={}",
                targetValue, actualValue);

            // Rollback
            rollbackSetpoint(recommendation);
        }

        return SetpointChangeResult.builder()
            .recommendationId(recommendation.getId())
            .success(success)
            .targetValue(targetValue)
            .actualValue(actualValue)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
```

---

## 8. APIs REST {#apis}

### 8.1 Recommendations API

```
Base URL: /api/nexus/po
```

#### Get Recommendations
```http
GET /recommendations?
    status=PENDING&
    priority=HIGH&
    sort=expectedBenefit,desc

Response: 200 OK
{
  "data": [
    {
      "id": "rec-uuid",
      "wellId": "well-uuid",
      "wellName": "WELL-ABC-123",
      "type": "ESP_FREQUENCY",
      "currentValue": 52.0,
      "recommendedValue": 48.0,
      "expectedBenefit": 350.50,
      "confidence": 0.92,
      "priority": "HIGH",
      "status": "PENDING",
      "risks": [
        "Production may decrease by 30 bpd temporarily"
      ],
      "actions": [
        "Reduce frequency to 48 Hz",
        "Monitor motor temperature for 4 hours",
        "If temperature doesn't decrease, schedule pulling"
      ],
      "createdAt": 1704816000000,
      "expiresAt": 1705420800000
    }
  ],
  "totalElements": 15,
  "hasNext": false
}
```

#### Approve Recommendation
```http
POST /recommendations/{id}/approve
Authorization: Bearer {jwt_token}

Request Body:
{
  "comments": "Approved. Temperature is critical.",
  "scheduleAt": null  // Execute immediately
}

Response: 200 OK
{
  "id": "rec-uuid",
  "status": "APPROVED",
  "approvedBy": "user-uuid",
  "approvedAt": 1704816120000,
  "executionScheduled": true
}
```

### 8.2 KPIs API

```http
GET /wells/{wellId}/kpis?
    from=2026-02-01&
    to=2026-02-28

Response: 200 OK
{
  "wellId": "well-uuid",
  "period": "2026-02",
  "kpis": {
    "avgProduction": 456.5,
    "avgEfficiency": 87.3,
    "uptime": 94.2,
    "totalDeferment": 342.8,
    "liftingCost": 8.45,
    "energyCost": 3.25
  },
  "trend": "IMPROVING",
  "comparison": {
    "vsLastMonth": +5.2,
    "vsLastYear": +12.8
  }
}
```

### 8.3 Health Score API

```http
GET /wells/{wellId}/health-score

Response: 200 OK
{
  "wellId": "well-uuid",
  "wellName": "WELL-ABC-123",
  "healthScore": 72,
  "trend": "DECLINING",
  "components": {
    "motor": {
      "score": 65,
      "issues": ["Temperature trending up", "Aislamiento degradando"]
    },
    "pump": {
      "score": 80,
      "issues": []
    },
    "cable": {
      "score": 75,
      "issues": ["Age: 18 months"]
    }
  },
  "predictedFailure": {
    "probability": 0.35,
    "estimatedDays": 45,
    "confidence": 0.82
  },
  "recommendations": [
    "Schedule pulling in next 30-60 days",
    "Reduce frequency to extend life"
  ]
}
```

---

**Documento PO_MODULE_SPEC.md completo.**

**Próximo**: DATA_MODEL.md con ERD completo
