# PO MODULE SPECIFICATION - Production Optimization

**Módulo**: Production Optimization (PO)
**Versión**: 2.0
**Fecha**: 2026-02-03
**Propósito**: Optimización inteligente de operaciones de producción con Machine Learning
**Arquitectura**: ThingsBoard Core (Assets, Attributes) + Custom Tables para Recommendations

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
PF Module (TB Assets, Attributes, ts_kv, TB Alarms)
    ↓ Usa caracterización de
RV Module (IPR, PVT, Propiedades como TB Attributes)
```

### 1.3 Arquitectura de Datos

> **Decisión Arquitectónica**: El módulo PO utiliza:
> - **ThingsBoard Assets** (via PF Module): Para identificar pozos y equipos
> - **ThingsBoard Attributes** (via PF Module): Para configuración y estado
> - **ThingsBoard ts_kv**: Para telemetría (consumida via TB API)
> - **ThingsBoard Alarms**: Para alarmas (via TB Alarm System)
> - **Custom Tables**: SOLO para `pf_recommendation` y `pf_optimization_result`
> - **Health Score**: Almacenado como SERVER_SCOPE Attribute en el Asset del pozo

### 1.4 Responsabilidades

✅ **Lo que hace el módulo PO**:
- Calcular setpoints óptimos para ESP, PCP, Gas Lift
- Predecir fallas de equipos con ML
- Detectar anomalías en tiempo real
- Calcular Health Score de equipos (guardado como TB Attribute)
- Generar recomendaciones con simulación de impacto
- Controlar cambios de setpoints (con aprobación)

❌ **Lo que NO hace**:
- Monitoreo en tiempo real (eso es PF Module)
- Captura de telemetría (eso es PF Module)
- Gestión de alarmas básicas (eso es PF Module via TB Alarm System)

---

## 2. Arquitectura del Módulo {#arquitectura}

### 2.1 Estructura de Paquetes

```
org.thingsboard.nexus.po
├── dto
│   ├── PoRecommendationDto.java
│   ├── OptimizationResultDto.java
│   ├── HealthScoreDto.java
│   └── KpiDto.java
│
├── model                              # SOLO para tablas custom
│   ├── PfRecommendation.java          # @Entity - pf_recommendation
│   └── PfOptimizationResult.java      # @Entity - pf_optimization_result
│
├── repository                         # SOLO JPA repos para tablas custom
│   ├── PfRecommendationRepository.java
│   └── PfOptimizationResultRepository.java
│
├── service
│   ├── PoAssetService.java            # Wrapper para TB AssetService
│   ├── PoAttributeService.java        # Wrapper para TB AttributesService
│   ├── PoTelemetryService.java        # Wrapper para TB TelemetryService
│   │
│   ├── optimizer
│   │   ├── EspFrequencyOptimizer.java
│   │   ├── GasLiftAllocator.java
│   │   ├── DiluentOptimizer.java
│   │   └── PcpRpmController.java
│   │
│   ├── ml
│   │   ├── FailurePredictionService.java
│   │   ├── AnomalyDetectionService.java
│   │   ├── ModelTrainingService.java
│   │   └── FeatureEngineeringService.java
│   │
│   ├── analytics
│   │   ├── HealthScoreCalculator.java
│   │   ├── RootCauseAnalyzer.java
│   │   └── BenchmarkingService.java
│   │
│   ├── recommendation
│   │   ├── RecommendationEngine.java
│   │   ├── ImpactSimulator.java
│   │   ├── ApprovalWorkflow.java
│   │   └── EffectivenessTracker.java
│   │
│   └── control
│       ├── SetpointController.java
│       ├── ClosedLoopController.java
│       └── SafetyInterlockService.java
│
└── controller
    ├── RecommendationController.java
    ├── OptimizationController.java
    └── AnalyticsController.java
```

### 2.2 Arquitectura de Componentes

```
┌─────────────────────────────────────────────────────────────────┐
│                     PO MODULE                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │         RECOMMENDATION ENGINE                             │   │
│  │  • Generate recommendations                               │   │
│  │  • Simulate impact                                        │   │
│  │  • Prioritize by value                                    │   │
│  │  • Store in pf_recommendation table                       │   │
│  └────────────────────┬─────────────────────────────────────┘   │
│                       │                                          │
│         ┌─────────────┼─────────────┐                           │
│         │             │             │                           │
│         ▼             ▼             ▼                           │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐                     │
│  │ OPTIMIZER │ │    ML     │ │ ANALYTICS │                     │
│  │  ENGINE   │ │  MODELS   │ │  ENGINE   │                     │
│  │           │ │           │ │           │                     │
│  │ • ESP     │ │• Failure  │ │• Health   │                     │
│  │ • PCP     │ │  Predict  │ │  Score    │                     │
│  │ • Gas Lift│ │• Anomaly  │ │• Root     │                     │
│  │ • Diluent │ │  Detect   │ │  Cause    │                     │
│  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘                     │
│        │             │             │                            │
│        └─────────────┼─────────────┘                            │
│                      │                                          │
│                      ▼                                          │
│           ┌──────────────────────┐                              │
│           │   THINGSBOARD CORE   │                              │
│           │   via PF Module APIs │                              │
│           │                      │                              │
│           │ • PfAssetService     │                              │
│           │ • PfAttributeService │                              │
│           │ • TB TelemetryService│                              │
│           │ • TB AlarmService    │                              │
│           └──────────────────────┘                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 Wrapper Services para ThingsBoard

```java
/**
 * PO Module wrapper for accessing PF Assets via TB Core
 */
@Service
@RequiredArgsConstructor
public class PoAssetService {

    private final AssetService assetService;  // TB Core Service

    /**
     * Get all wells by type from PF Module
     */
    public List<Asset> getWellsByType(UUID tenantId, String liftSystemType) {
        // Use PF asset types
        List<Asset> allWells = assetService.findAssetsByTenantIdAndType(
            new TenantId(tenantId),
            PfWellDto.ASSET_TYPE,  // "pf_well"
            new PageLink(10000)
        ).getData();

        // Filter by lift system type attribute if needed
        return allWells.stream()
            .filter(w -> matchesLiftSystemType(w.getId().getId(), liftSystemType))
            .collect(Collectors.toList());
    }

    /**
     * Get asset by ID
     */
    public Optional<Asset> getAssetById(UUID assetId) {
        try {
            return Optional.of(assetService.findAssetById(
                TenantId.SYS_TENANT_ID,
                new AssetId(assetId)
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

/**
 * PO Module wrapper for TB Attributes (Health Score, etc.)
 */
@Service
@RequiredArgsConstructor
public class PoAttributeService {

    private final AttributesService attributesService;  // TB Core Service
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Health Score attribute keys
    public static final String ATTR_HEALTH_SCORE = "health_score";
    public static final String ATTR_HEALTH_TREND = "health_trend";
    public static final String ATTR_FAILURE_PROBABILITY = "failure_probability";
    public static final String ATTR_ESTIMATED_DAYS_TO_FAILURE = "estimated_days_to_failure";
    public static final String ATTR_COMPONENT_SCORES = "component_scores";

    /**
     * Save health score as SERVER_SCOPE attribute on well asset
     */
    public void saveHealthScore(UUID wellAssetId, HealthScoreDto healthScore) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ATTR_HEALTH_SCORE, healthScore.getScore());
        attributes.put(ATTR_HEALTH_TREND, healthScore.getTrend().name());
        attributes.put(ATTR_FAILURE_PROBABILITY, healthScore.getFailureProbability());
        attributes.put(ATTR_ESTIMATED_DAYS_TO_FAILURE, healthScore.getEstimatedDaysToFailure());

        // Component scores as JSON
        try {
            String componentScoresJson = objectMapper.writeValueAsString(healthScore.getComponentScores());
            attributes.put(ATTR_COMPONENT_SCORES, componentScoresJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize component scores", e);
        }

        saveServerAttributes(wellAssetId, attributes);
    }

    /**
     * Get health score from well asset attributes
     */
    public Optional<HealthScoreDto> getHealthScore(UUID wellAssetId) {
        List<AttributeKvEntry> attrs = getServerAttributes(wellAssetId, List.of(
            ATTR_HEALTH_SCORE,
            ATTR_HEALTH_TREND,
            ATTR_FAILURE_PROBABILITY,
            ATTR_ESTIMATED_DAYS_TO_FAILURE,
            ATTR_COMPONENT_SCORES
        ));

        if (attrs.isEmpty()) {
            return Optional.empty();
        }

        HealthScoreDto.HealthScoreDtoBuilder builder = HealthScoreDto.builder();
        builder.assetId(wellAssetId);

        for (AttributeKvEntry attr : attrs) {
            switch (attr.getKey()) {
                case ATTR_HEALTH_SCORE:
                    builder.score(attr.getLongValue().orElse(0L).intValue());
                    break;
                case ATTR_HEALTH_TREND:
                    builder.trend(HealthTrend.valueOf(attr.getStrValue().orElse("STABLE")));
                    break;
                case ATTR_FAILURE_PROBABILITY:
                    builder.failureProbability(attr.getDoubleValue().orElse(0.0));
                    break;
                case ATTR_ESTIMATED_DAYS_TO_FAILURE:
                    builder.estimatedDaysToFailure(attr.getLongValue().orElse(null));
                    break;
                case ATTR_COMPONENT_SCORES:
                    // Parse JSON
                    try {
                        String json = attr.getStrValue().orElse("{}");
                        Map<String, ComponentScore> scores = objectMapper.readValue(
                            json, new TypeReference<>() {});
                        builder.componentScores(scores);
                    } catch (Exception e) {
                        log.error("Failed to parse component scores", e);
                    }
                    break;
            }
        }

        return Optional.of(builder.build());
    }

    private void saveServerAttributes(UUID entityId, Map<String, Object> attributes) {
        List<AttributeKvEntry> kvEntries = new ArrayList<>();
        long ts = System.currentTimeMillis();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            AttributeKvEntry kvEntry = createKvEntry(entry.getKey(), entry.getValue(), ts);
            if (kvEntry != null) {
                kvEntries.add(kvEntry);
            }
        }

        if (!kvEntries.isEmpty()) {
            attributesService.save(
                TenantId.SYS_TENANT_ID,
                new AssetId(entityId),
                AttributeScope.SERVER_SCOPE,
                kvEntries
            );
        }
    }

    private List<AttributeKvEntry> getServerAttributes(UUID assetId, List<String> keys) {
        try {
            return attributesService.find(
                TenantId.SYS_TENANT_ID,
                new AssetId(assetId),
                AttributeScope.SERVER_SCOPE,
                keys
            ).get();
        } catch (Exception e) {
            log.error("Error getting attributes for asset {}", assetId, e);
            return List.of();
        }
    }

    // ... createKvEntry implementation similar to PF module
}
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
    private PoAssetService assetService;

    @Autowired
    private PoAttributeService attributeService;

    @Autowired
    private TimeseriesService telemetryService;  // TB Core Service

    @Autowired
    private RvWellService rvWellService;

    /**
     * Optimize ESP frequency for a well
     */
    public OptimizationResult optimizeFrequency(UUID wellAssetId) {

        // 1. Get well asset and attributes from TB
        Asset wellAsset = assetService.getAssetById(wellAssetId)
            .orElseThrow(() -> new EntityNotFoundException("Well not found"));

        Map<String, Object> wellAttrs = attributeService.getServerAttributesAsMap(wellAssetId);
        String rvWellId = (String) wellAttrs.get(PfWellDto.ATTR_RV_WELL_ID);

        // 2. Get ESP system attributes
        List<Asset> espSystems = assetService.getRelatedAssets(
            wellAssetId, "HasSystem", PfEspSystemDto.ASSET_TYPE);

        if (espSystems.isEmpty()) {
            return OptimizationResult.noAction("No ESP system found");
        }

        Asset espAsset = espSystems.get(0);
        Map<String, Object> espAttrs = attributeService.getServerAttributesAsMap(
            espAsset.getId().getId());

        // 3. Get current telemetry from ts_kv
        TelemetryData currentTelemetry = getLatestTelemetry(wellAssetId);

        // 4. Get reservoir data from RV module (also TB-based)
        RvWellDto rvWell = rvWellService.getWellById(UUID.fromString(rvWellId))
            .orElseThrow();
        IprCurve ipr = rvWellService.getIprCurve(rvWell.getAssetId());
        PvtProperties pvt = rvWellService.getPvtProperties(rvWell.getAssetId());

        // 5. Evaluate current state
        StateEvaluation currentState = evaluateState(currentTelemetry, espAttrs);

        if (!currentState.isWithinSafeLimits()) {
            // PROTECTION MODE: reduce frequency
            return generateProtectionRecommendation(currentState);
        }

        // 6. Calculate efficiency
        double currentEfficiency = calculateEfficiency(
            currentTelemetry.getProduction(),
            ipr.getPotentialAtPwf(currentTelemetry.getPip()),
            currentTelemetry.getFrequency()
        );

        // 7. Determine opportunity
        OptimizationOpportunity opportunity =
            identifyOpportunity(currentEfficiency, currentState);

        if (opportunity.getType() == OpportunityType.INCREASE_FREQUENCY) {
            double proposedFreq = currentTelemetry.getFrequency() + 2.0;
            return simulateFrequencyChange(wellAssetId, proposedFreq, ipr, pvt, espAttrs);

        } else if (opportunity.getType() == OpportunityType.DECREASE_FREQUENCY) {
            double proposedFreq = currentTelemetry.getFrequency() - 3.0;
            return simulateFrequencyChange(wellAssetId, proposedFreq, ipr, pvt, espAttrs);

        } else {
            return OptimizationResult.noAction("Well operating optimally");
        }
    }

    /**
     * Get latest telemetry from TB ts_kv
     */
    private TelemetryData getLatestTelemetry(UUID assetId) {
        List<String> keys = List.of(
            "frequency_hz", "current_amps", "motor_temp_f",
            "pip_psi", "vibration_g", "production_bpd"
        );

        try {
            List<TsKvEntry> entries = telemetryService.findLatest(
                TenantId.SYS_TENANT_ID,
                new AssetId(assetId),
                keys
            ).get();

            return TelemetryData.fromTsKvEntries(entries);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get telemetry", e);
        }
    }

    /**
     * Evaluate current state
     */
    private StateEvaluation evaluateState(TelemetryData telemetry, Map<String, Object> espAttrs) {
        StateEvaluation eval = new StateEvaluation();

        Double maxMotorTemp = (Double) espAttrs.get(PfEspSystemDto.ATTR_MAX_MOTOR_TEMP_F);
        Double maxCurrentAmps = (Double) espAttrs.get(PfEspSystemDto.ATTR_MAX_CURRENT_AMPS);
        Double minPipPsi = (Double) espAttrs.get(PfEspSystemDto.ATTR_MIN_PIP_PSI);
        Double maxVibrationG = (Double) espAttrs.get(PfEspSystemDto.ATTR_MAX_VIBRATION_G);

        // Check temperature
        if (maxMotorTemp != null && telemetry.getMotorTempF() > maxMotorTemp) {
            eval.addViolation("MOTOR_TEMP_HIGH",
                String.format("Motor temp %.1f°F exceeds limit %.1f°F",
                    telemetry.getMotorTempF(), maxMotorTemp));
        }

        // Check current
        if (maxCurrentAmps != null) {
            double currentPercent = telemetry.getCurrentAmps() / maxCurrentAmps * 100;
            if (currentPercent > 110) {
                eval.addViolation("CURRENT_HIGH",
                    String.format("Motor current %.1f%% of nominal", currentPercent));
            } else if (currentPercent < 60) {
                eval.addViolation("CURRENT_LOW",
                    String.format("Motor current %.1f%% (possible gas)", currentPercent));
            }
        }

        // Check PIP
        if (minPipPsi != null && telemetry.getPipPsi() < minPipPsi) {
            eval.addViolation("PIP_LOW",
                String.format("PIP %.1f psi below minimum %.1f psi (gas lock risk)",
                    telemetry.getPipPsi(), minPipPsi));
        }

        // Check vibration
        if (maxVibrationG != null && telemetry.getVibrationG() > maxVibrationG) {
            eval.addViolation("VIBRATION_HIGH",
                String.format("Vibration %.2fg exceeds limit %.2fg",
                    telemetry.getVibrationG(), maxVibrationG));
        }

        return eval;
    }
}
```

#### Lógica de Decisión

```
┌─────────────────────────────────────────────────────────────┐
│            ESP FREQUENCY OPTIMIZATION LOGIC                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Step 1: Get Data from ThingsBoard                          │
│  ────────────────────────────────                           │
│  • Well Asset from TB asset table (type: pf_well)           │
│  • Well Attributes from TB attribute_kv (SERVER_SCOPE)      │
│  • ESP System via TB relation (HasSystem → pf_esp_system)   │
│  • Current Telemetry from TB ts_kv                          │
│                                                              │
│  Step 2: Evaluate Safety                                    │
│  ────────────────────────                                   │
│  IF temperature > max_motor_temp_f  → PROTECTION MODE       │
│  IF current > 110% rated            → PROTECTION MODE       │
│  IF PIP < min_pip_psi              → PROTECTION MODE        │
│  IF vibration > max_vibration_g    → PROTECTION MODE        │
│                                                              │
│  Step 3: Calculate Efficiency                               │
│  ─────────────────────────                                  │
│  Efficiency = Actual Production / Potential Production      │
│                                                              │
│  Step 4: Identify Opportunity                               │
│  ──────────────────────────                                 │
│  IF efficiency > 90% AND stable conditions:                 │
│      → Consider INCREASE frequency (+2-5 Hz)                │
│                                                              │
│  IF efficiency < 70%:                                       │
│      → Consider DECREASE frequency (possible gas)           │
│                                                              │
│  IF 70% < efficiency < 90%:                                 │
│      → Maintain and monitor                                 │
│                                                              │
│  Step 5: Simulate Impact                                    │
│  ────────────────────────                                   │
│  • Run nodal analysis with new frequency                    │
│  • Estimate new production                                  │
│  • Estimate new motor temperature                           │
│  • Calculate net economic benefit                           │
│                                                              │
│  Step 6: Save Recommendation                                │
│  ──────────────────────────                                 │
│  → Save to pf_recommendation table (custom)                 │
│  → Include well_id referencing TB Asset UUID                │
│                                                              │
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

    @Autowired
    private PoAssetService assetService;

    @Autowired
    private TimeseriesService telemetryService;

    /**
     * Optimize gas lift allocation across wells
     */
    public GasLiftAllocationResult optimizeAllocation(
            UUID tenantId, double totalGasAvailable) {

        // 1. Get all gas lift wells from TB
        List<Asset> gasLiftWells = assetService.getWellsByType(tenantId, "GAS_LIFT");

        List<UUID> wellIds = gasLiftWells.stream()
            .map(a -> a.getId().getId())
            .collect(Collectors.toList());

        // 2. Get current allocation from TB attributes
        Map<UUID, Double> currentAllocation = getCurrentAllocation(wellIds);

        // 3. Get response curves for each well
        Map<UUID, GasLiftResponseCurve> responseCurves = new HashMap<>();
        for (UUID wellId : wellIds) {
            responseCurves.put(wellId, buildResponseCurve(wellId));
        }

        // 4. Run optimization algorithm
        Map<UUID, Double> optimalAllocation = optimizeUsingMarginalAnalysis(
            responseCurves,
            totalGasAvailable
        );

        // 5. Calculate impact
        double currentProduction = calculateTotalProduction(
            wellIds, currentAllocation, responseCurves);

        double optimalProduction = calculateTotalProduction(
            wellIds, optimalAllocation, responseCurves);

        double productionGain = optimalProduction - currentProduction;

        // 6. Build result
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
     */
    private Map<UUID, Double> optimizeUsingMarginalAnalysis(
            Map<UUID, GasLiftResponseCurve> curves,
            double totalGas) {

        Map<UUID, Double> allocation = new HashMap<>();
        curves.keySet().forEach(wellId -> allocation.put(wellId, 0.0));

        double gasIncrement = 0.1; // MMscfd
        double remainingGas = totalGas;

        while (remainingGas >= gasIncrement) {
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
                break;
            }
        }

        return allocation;
    }
}
```

### 3.3 Diluent Optimizer

```java
@Service
public class DiluentOptimizer {

    @Autowired
    private PoAssetService assetService;

    @Autowired
    private PoAttributeService attributeService;

    @Autowired
    private TimeseriesService telemetryService;

    /**
     * Optimize diluent injection rate
     */
    public DiluentOptimizationResult optimize(UUID wellAssetId) {

        // Get well asset from TB
        Asset wellAsset = assetService.getAssetById(wellAssetId).orElseThrow();

        // Get RV well reference from attributes
        Map<String, Object> wellAttrs = attributeService.getServerAttributesAsMap(wellAssetId);
        String rvWellId = (String) wellAttrs.get(PfWellDto.ATTR_RV_WELL_ID);

        // Get current telemetry from ts_kv
        double currentDiluentRate = getLatestTelemetryValue(wellAssetId, "diluent_rate_bpd");
        double currentProduction = getLatestTelemetryValue(wellAssetId, "production_bpd");
        double currentViscosity = getLatestTelemetryValue(wellAssetId, "viscosity_cp");

        // Get reservoir properties from RV (also TB-based)
        RvWellDto rvWell = rvWellService.getWellById(UUID.fromString(rvWellId)).orElseThrow();
        PvtProperties pvt = rvWellService.getPvtProperties(rvWell.getAssetId());

        double heavyOilViscosity = pvt.getViscosityCp();
        double heavyOilApi = pvt.getApiGravity();

        // Get economic data
        double diluentCost = getDiluentCost();

        // Optimization: minimize diluent subject to constraints
        OptimizationProblem problem = OptimizationProblem.builder()
            .objectiveFunction(diluentRate -> {
                double diluentExpense = diluentRate * diluentCost;
                double blendValue = calculateBlendValue(diluentRate);
                return diluentExpense - blendValue;
            })
            .addConstraint("production", diluentRate -> {
                double estimatedProd = estimateProduction(wellAssetId, diluentRate);
                return estimatedProd >= currentProduction * 0.95;
            })
            .addConstraint("viscosity", diluentRate -> {
                double blendViscosity = calculateBlendViscosity(
                    heavyOilViscosity, getDiluentViscosity(), diluentRate);
                return blendViscosity <= 500;
            })
            .addConstraint("api", diluentRate -> {
                double blendApi = calculateBlendApi(
                    heavyOilApi, getDiluentApi(), diluentRate);
                return blendApi >= 18.0;
            })
            .lowerBound(0.0)
            .upperBound(currentDiluentRate * 2.0)
            .build();

        double optimalDiluentRate = NumericalOptimizer.solve(problem);
        double savings = (currentDiluentRate - optimalDiluentRate) * diluentCost;

        return DiluentOptimizationResult.builder()
            .wellId(wellAssetId)
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

    Data Source: ThingsBoard ts_kv via REST API
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

        x = layers.LSTM(64, return_sequences=True)(input_layer)
        x = layers.Dropout(0.2)(x)
        x = layers.LSTM(32, return_sequences=False)(x)
        x = layers.Dropout(0.2)(x)
        x = layers.Dense(16, activation='relu')(x)
        x = layers.Dropout(0.1)(x)

        output = layers.Dense(1, activation='sigmoid')(x)

        model = Model(inputs=input_layer, outputs=output)

        model.compile(
            optimizer='adam',
            loss='binary_crossentropy',
            metrics=['accuracy', 'precision', 'recall', 'AUC']
        )

        self.model = model
        return model

    def predict_well(self, well_asset_id, tb_client):
        """
        Predict failure for a specific well

        Args:
            well_asset_id: TB Asset UUID for the well (pf_well type)
            tb_client: ThingsBoard REST API client

        Returns:
            dict with failure_probability, days_to_failure, confidence
        """

        # Fetch telemetry from ThingsBoard ts_kv
        telemetry_data = self.fetch_telemetry_from_tb(
            tb_client, well_asset_id, self.lookback_hours)

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
            'well_asset_id': str(well_asset_id),
            'failure_probability': probability,
            'days_to_failure': days_to_failure,
            'confidence': confidence,
            'timestamp': pd.Timestamp.now().isoformat()
        }

    def fetch_telemetry_from_tb(self, tb_client, asset_id, hours):
        """
        Fetch telemetry from ThingsBoard ts_kv tables via REST API

        GET /api/plugins/telemetry/{entityType}/{entityId}/values/timeseries
        """
        end_ts = int(time.time() * 1000)
        start_ts = end_ts - (hours * 3600 * 1000)

        response = tb_client.get_timeseries(
            entity_type='ASSET',
            entity_id=str(asset_id),
            keys=','.join(self.features),
            start_ts=start_ts,
            end_ts=end_ts,
            interval=3600000,  # 1 hour
            agg='AVG'
        )

        return self._response_to_dataframe(response)
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

    Data Source: ThingsBoard ts_kv via REST API
    """

    def __init__(self, contamination=0.05):
        self.model = IsolationForest(
            n_estimators=100,
            contamination=contamination,
            random_state=42
        )
        self.scaler = StandardScaler()
        self.feature_names = None

    def detect_anomaly(self, well_asset_id, tb_client):
        """
        Detect anomaly in real-time telemetry from ThingsBoard

        Args:
            well_asset_id: TB Asset UUID
            tb_client: ThingsBoard REST API client
        """

        # Fetch latest telemetry from TB ts_kv_latest
        telemetry = tb_client.get_latest_telemetry(
            entity_type='ASSET',
            entity_id=str(well_asset_id),
            keys=self.feature_names
        )

        # Extract features
        features = self.extract_features(telemetry)

        # Predict
        prediction, score = self.predict([features])

        is_anomaly = prediction[0] == -1
        anomaly_score = -score[0]

        # Identify which features are anomalous
        if is_anomaly:
            anomalous_features = self.identify_anomalous_features(features)
        else:
            anomalous_features = []

        return {
            'well_asset_id': str(well_asset_id),
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
    private PfRecommendationRepository recommendationRepository;  // Custom table

    @Autowired
    private PoAssetService assetService;

    @Autowired
    private PoAttributeService attributeService;

    /**
     * Generate recommendations for all wells
     */
    @Scheduled(cron = "0 0 7 * * *")  // Daily at 7 AM
    public void generateDailyRecommendations(UUID tenantId) {

        log.info("Starting daily recommendation generation");

        // Get all active wells from TB
        List<Asset> wells = assetService.getActiveWells(tenantId);

        List<PfRecommendation> recommendations = new ArrayList<>();

        for (Asset wellAsset : wells) {
            try {
                UUID wellAssetId = wellAsset.getId().getId();

                // Get lift system type from TB attribute
                Map<String, Object> attrs = attributeService.getServerAttributesAsMap(wellAssetId);
                String liftSystemType = (String) attrs.get(PfWellDto.ATTR_LIFT_SYSTEM_TYPE);

                List<PfRecommendation> wellRecs =
                    generateRecommendationsForWell(wellAssetId, liftSystemType);

                recommendations.addAll(wellRecs);

            } catch (Exception e) {
                log.error("Error generating recommendations for well {}",
                    wellAsset.getName(), e);
            }
        }

        // Prioritize by economic value
        recommendations.sort(Comparator.comparing(
            PfRecommendation::getExpectedBenefitBpd).reversed());

        // Save top recommendations to custom table
        recommendations.stream()
            .limit(50)
            .forEach(recommendationRepository::save);

        log.info("Generated {} recommendations", recommendations.size());
    }

    /**
     * Generate recommendations for a specific well
     */
    public List<PfRecommendation> generateRecommendationsForWell(
            UUID wellAssetId, String liftSystemType) {

        List<PfRecommendation> recommendations = new ArrayList<>();

        switch (liftSystemType) {
            case "ESP":
                OptimizationResult espResult = espOptimizer.optimizeFrequency(wellAssetId);
                if (espResult.hasActionRequired()) {
                    recommendations.add(buildRecommendation(wellAssetId, espResult));
                }
                break;

            case "GAS_LIFT":
                // Gas lift is optimized at field level
                break;

            case "PCP":
                OptimizationResult pcpResult = pcpOptimizer.optimizeRpm(wellAssetId);
                if (pcpResult.hasActionRequired()) {
                    recommendations.add(buildRecommendation(wellAssetId, pcpResult));
                }
                break;

            case "DILUENT_INJECTION":
                DiluentOptimizationResult diluentResult =
                    diluentOptimizer.optimize(wellAssetId);
                if (diluentResult.hasSavings()) {
                    recommendations.add(buildRecommendation(wellAssetId, diluentResult));
                }
                break;
        }

        return recommendations;
    }

    /**
     * Build recommendation entity for custom table
     */
    private PfRecommendation buildRecommendation(
            UUID wellAssetId, OptimizationResult result) {

        return PfRecommendation.builder()
            .tenantId(getTenantId())
            .wellId(wellAssetId)  // Reference to TB Asset UUID
            .type(result.getOptimizationType())
            .currentValue(result.getCurrentValue())
            .recommendedValue(result.getRecommendedValue())
            .expectedBenefitBpd(result.getProductionGain())
            .expectedSavingsUsd(result.getNetBenefit())
            .confidence(result.getConfidence())
            .priority(calculatePriority(result))
            .status("PENDING")
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

    @Autowired
    private PfRecommendationRepository recommendationRepository;

    @Autowired
    private SetpointController setpointController;

    @Autowired
    private NotificationService notificationService;

    /**
     * Determine approval level required based on risk
     */
    public ApprovalLevel getRequiredApprovalLevel(PfRecommendation recommendation) {

        double benefitPerDay = recommendation.getExpectedSavingsUsd();
        double confidence = recommendation.getConfidence();

        // Level 1: Automatic
        if (benefitPerDay < 500 && confidence > 0.9) {
            return ApprovalLevel.AUTOMATIC;
        }

        // Level 2: Supervisor approval
        if (benefitPerDay < 2000 && confidence > 0.75) {
            return ApprovalLevel.SUPERVISOR;
        }

        // Level 3: Engineer approval
        if (benefitPerDay < 10000 && confidence > 0.6) {
            return ApprovalLevel.ENGINEER;
        }

        // Level 4: Manager approval
        return ApprovalLevel.MANAGER;
    }

    /**
     * Approve recommendation
     */
    public void approve(UUID recommendationId, UUID approverId, String comments) {

        PfRecommendation rec = recommendationRepository.findById(recommendationId)
            .orElseThrow();

        rec.setStatus("APPROVED");
        rec.setApprovedBy(approverId);
        rec.setApprovedTime(System.currentTimeMillis());

        recommendationRepository.save(rec);

        // Execute recommendation
        setpointController.executeRecommendation(rec);
    }
}
```

---

## 6. KPIs y Analytics {#kpis}

### 6.1 Health Score Calculator

Health Score is stored as a **ThingsBoard Attribute** on the well asset, not in a custom table.

```java
@Service
public class HealthScoreCalculator {

    @Autowired
    private PoAssetService assetService;

    @Autowired
    private PoAttributeService attributeService;

    @Autowired
    private TimeseriesService telemetryService;

    @Autowired
    private FailurePredictionService predictionService;

    /**
     * Calculate and save health score for a well
     * Stores result as TB Attributes on the well asset
     */
    public HealthScoreDto calculateAndSave(UUID wellAssetId) {

        // Get related ESP system
        List<Asset> espSystems = assetService.getRelatedAssets(
            wellAssetId, "HasSystem", PfEspSystemDto.ASSET_TYPE);

        if (espSystems.isEmpty()) {
            return null;
        }

        UUID espAssetId = espSystems.get(0).getId().getId();

        // Calculate component scores
        Map<String, ComponentScore> componentScores = new HashMap<>();

        // Motor score
        ComponentScore motorScore = calculateMotorScore(wellAssetId, espAssetId);
        componentScores.put("motor", motorScore);

        // Pump score
        ComponentScore pumpScore = calculatePumpScore(wellAssetId, espAssetId);
        componentScores.put("pump", pumpScore);

        // Cable score
        ComponentScore cableScore = calculateCableScore(wellAssetId, espAssetId);
        componentScores.put("cable", cableScore);

        // Overall score (weighted average)
        int overallScore = (int) (
            motorScore.getScore() * 0.4 +
            pumpScore.getScore() * 0.35 +
            cableScore.getScore() * 0.25
        );

        // Get failure prediction from ML service
        PredictionResult prediction = predictionService.predict(wellAssetId);

        // Determine trend
        HealthTrend trend = determineTrend(wellAssetId, overallScore);

        // Build DTO
        HealthScoreDto healthScore = HealthScoreDto.builder()
            .assetId(wellAssetId)
            .score(overallScore)
            .trend(trend)
            .componentScores(componentScores)
            .failureProbability(prediction.getProbability())
            .estimatedDaysToFailure(prediction.getDaysToFailure())
            .calculatedAt(System.currentTimeMillis())
            .build();

        // Save as TB Attributes on the well asset
        attributeService.saveHealthScore(wellAssetId, healthScore);

        return healthScore;
    }

    /**
     * Get health score from TB Attributes
     */
    public Optional<HealthScoreDto> getHealthScore(UUID wellAssetId) {
        return attributeService.getHealthScore(wellAssetId);
    }

    private ComponentScore calculateMotorScore(UUID wellAssetId, UUID espAssetId) {
        // Get motor telemetry from ts_kv
        List<TsKvEntry> telemetry = getLatestTelemetry(espAssetId,
            List.of("motor_temp_f", "current_amps", "motor_vibration_g"));

        // Get ESP attributes for limits
        Map<String, Object> espAttrs = attributeService.getServerAttributesAsMap(espAssetId);

        double maxTemp = (Double) espAttrs.getOrDefault(PfEspSystemDto.ATTR_MAX_MOTOR_TEMP_F, 300.0);
        double currentTemp = getTelemetryValue(telemetry, "motor_temp_f");

        // Calculate score based on proximity to limits
        double tempScore = 100 - ((currentTemp / maxTemp) * 100);

        List<String> issues = new ArrayList<>();
        if (currentTemp > maxTemp * 0.9) {
            issues.add("Temperature approaching limit");
        }

        return ComponentScore.builder()
            .score((int) Math.max(0, Math.min(100, tempScore)))
            .issues(issues)
            .build();
    }
}
```

### 6.2 KPI Calculator

```java
@Service
public class ProductionKpiCalculator {

    @Autowired
    private PoAssetService assetService;

    @Autowired
    private TimeseriesService telemetryService;

    @Autowired
    private RvWellService rvWellService;

    /**
     * Calculate production KPIs for a well
     * Data comes from TB ts_kv and RV module (also TB-based)
     */
    public ProductionKpi calculateKpis(UUID wellAssetId, LocalDate date) {

        // Get production data from ts_kv
        ProductionData prodData = getProductionForDate(wellAssetId, date);

        // Get potential from RV module (which also uses TB Assets/Attributes)
        Map<String, Object> wellAttrs = attributeService.getServerAttributesAsMap(wellAssetId);
        String rvWellId = (String) wellAttrs.get(PfWellDto.ATTR_RV_WELL_ID);

        double potential = rvWellService.getPotentialProduction(
            UUID.fromString(rvWellId), date);

        // Calculate metrics
        double efficiency = prodData.getOilBpd() / potential * 100.0;
        double uptime = prodData.getHoursProducing() / 24.0 * 100.0;
        double deferment = Math.max(0, potential - prodData.getOilBpd());

        return ProductionKpi.builder()
            .wellAssetId(wellAssetId)
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

    private ProductionData getProductionForDate(UUID assetId, LocalDate date) {
        long startTs = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long endTs = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        List<TsKvEntry> telemetry = telemetryService.findAll(
            TenantId.SYS_TENANT_ID,
            new AssetId(assetId),
            new BaseReadTsKvQuery(
                List.of("production_bpd", "gas_mcfd", "water_bpd", "bsw", "gor"),
                startTs, endTs, 0, 100, Aggregation.AVG
            )
        ).join();

        return ProductionData.fromTelemetry(telemetry);
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

    @Autowired
    private PoAttributeService attributeService;

    @Autowired
    private AlarmService alarmService;  // TB Alarm Service

    /**
     * Execute recommendation by sending setpoint to SCADA
     */
    public SetpointChangeResult executeRecommendation(PfRecommendation recommendation) {

        UUID wellAssetId = recommendation.getWellId();

        // Safety checks
        if (!safetyService.isChangeAllowed(recommendation)) {
            // Create TB Alarm
            alarmService.createAlarm(
                new AssetId(wellAssetId),
                "SETPOINT_CHANGE_BLOCKED",
                AlarmSeverity.WARNING,
                "Safety interlock prevents change"
            );
            throw new SafetyInterlockException("Safety interlock prevents change");
        }

        // Send to SCADA
        try {
            ScadaCommand command = buildScadaCommand(recommendation);
            scadaService.sendCommand(command);

            log.info("Setpoint change sent to SCADA: well={}, type={}, value={}",
                wellAssetId,
                recommendation.getType(),
                recommendation.getRecommendedValue());

            // Monitor response
            return monitorSetpointChange(recommendation);

        } catch (Exception e) {
            log.error("Failed to execute recommendation", e);

            // Create TB Alarm for failure
            alarmService.createAlarm(
                new AssetId(wellAssetId),
                "SETPOINT_CHANGE_FAILED",
                AlarmSeverity.MAJOR,
                "Failed to send command to SCADA: " + e.getMessage()
            );

            throw new SetpointChangeException("Failed to send command to SCADA", e);
        }
    }

    /**
     * Monitor setpoint change and verify response
     */
    private SetpointChangeResult monitorSetpointChange(PfRecommendation recommendation) {

        UUID wellAssetId = recommendation.getWellId();
        String variable = getVariableName(recommendation.getType());
        double targetValue = recommendation.getRecommendedValue();

        // Wait for change to take effect (30 seconds)
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify new value from TB telemetry
        double actualValue = getLatestTelemetryValue(wellAssetId, variable);

        boolean success = Math.abs(actualValue - targetValue) < 0.5;

        if (!success) {
            log.warn("Setpoint change failed: expected={}, actual={}",
                targetValue, actualValue);

            // Rollback
            rollbackSetpoint(recommendation);

            // Create TB Alarm
            alarmService.createAlarm(
                new AssetId(wellAssetId),
                "SETPOINT_VERIFICATION_FAILED",
                AlarmSeverity.MAJOR,
                String.format("Expected %s=%f, actual=%f", variable, targetValue, actualValue)
            );
        } else {
            // Clear any related alarms
            alarmService.clearAlarm(
                new AssetId(wellAssetId),
                "SETPOINT_CHANGE_FAILED"
            );
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
      "wellAssetId": "well-asset-uuid",   // TB Asset UUID
      "wellName": "WELL-ABC-123",
      "type": "ESP_FREQUENCY",
      "currentValue": 52.0,
      "recommendedValue": 48.0,
      "expectedBenefitBpd": 35.5,
      "expectedSavingsUsd": 350.50,
      "confidence": 0.92,
      "priority": "HIGH",
      "status": "PENDING",
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
  "scheduleAt": null
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

### 8.2 Health Score API

Health Score is retrieved from **TB Attributes**, not a custom table.

```http
GET /wells/{wellAssetId}/health-score

Response: 200 OK
{
  "wellAssetId": "well-asset-uuid",   // TB Asset UUID
  "wellName": "WELL-ABC-123",
  "healthScore": 72,
  "trend": "DECLINING",
  "components": {
    "motor": {
      "score": 65,
      "issues": ["Temperature trending up"]
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
  "dataSource": "TB Attributes (SERVER_SCOPE)"
}
```

### 8.3 KPIs API

```http
GET /wells/{wellAssetId}/kpis?
    from=2026-02-01&
    to=2026-02-28

Response: 200 OK
{
  "wellAssetId": "well-asset-uuid",
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
  "dataSource": "TB ts_kv"
}
```

---

## 9. Modelo de Datos

### 9.1 Custom Tables (Solo 2)

```sql
-- pf_recommendation: Workflow de aprobación complejo
CREATE TABLE pf_recommendation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    well_id UUID NOT NULL,  -- Referencia a TB Asset (type: pf_well)
    type VARCHAR(50) NOT NULL,
    current_value DOUBLE PRECISION,
    recommended_value DOUBLE PRECISION,
    expected_benefit_bpd DOUBLE PRECISION,
    expected_savings_usd DOUBLE PRECISION,
    confidence DOUBLE PRECISION,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_by UUID,
    approved_by UUID,
    executed_by UUID,
    created_time BIGINT NOT NULL,
    approved_time BIGINT,
    executed_time BIGINT,
    expires_at BIGINT,
    notes TEXT
);

-- pf_optimization_result: Histórico de análisis complejos
CREATE TABLE pf_optimization_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    well_id UUID NOT NULL,  -- Referencia a TB Asset
    optimization_type VARCHAR(50) NOT NULL,
    input_parameters JSONB,
    output_parameters JSONB,
    improvement_percent DOUBLE PRECISION,
    calculated_at BIGINT NOT NULL,
    created_time BIGINT NOT NULL
);
```

### 9.2 ThingsBoard Data (Via Core Tables)

| Dato | Almacenamiento | Acceso |
|------|----------------|--------|
| Well Entity | TB `asset` (type: pf_well) | `AssetService.findAssetById()` |
| Well Config | TB `attribute_kv` (SERVER_SCOPE) | `AttributesService.find()` |
| Telemetry | TB `ts_kv` | `TelemetryService.findAll()` |
| Health Score | TB `attribute_kv` (SERVER_SCOPE) | `AttributesService.find()` |
| Alarms | TB `alarm` | `AlarmService.findAlarms()` |
| Relationships | TB `relation` | `RelationService.findByFrom()` |

---

**Documento PO_MODULE_SPEC.md v2.0 - Arquitectura ThingsBoard Core**
