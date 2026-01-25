# Cálculo de Fatiga - Módulo Coiled Tubing

## Introducción

El cálculo de fatiga es crítico para garantizar la seguridad operacional y maximizar la vida útil de los reels de tubería continua. Este documento describe la metodología, algoritmos y implementación del sistema de cálculo de fatiga en tiempo real.

## Fundamentos Teóricos

### Fatiga en Tubería Continua

La tubería de coiled tubing experimenta fatiga debido a:

1. **Flexión cíclica**: Cuando pasa por el gooseneck y se enrolla/desenrolla del reel
2. **Presión interna**: Durante operaciones de bombeo
3. **Tensión axial**: Durante despliegue y retracción
4. **Temperatura**: Afecta las propiedades del material
5. **Corrosión**: Ambiente corrosivo reduce la vida útil

### Regla de Palmgren-Miner

La acumulación de daño por fatiga se calcula usando la **regla de Palmgren-Miner**:

```
D = Σ(ni / Ni)

Donde:
D  = Daño acumulado (cuando D ≥ 1.0, se predice falla)
ni = Número de ciclos aplicados al nivel de estrés i
Ni = Número de ciclos hasta falla al nivel de estrés i
```

## Modelo de Cálculo

### Paso 1: Cálculo de Esfuerzos

#### A. Esfuerzo Circunferencial (Hoop Stress)

Causado por la presión interna:

```
σh = (P × Di) / (2 × t)

Donde:
σh = Esfuerzo circunferencial (psi)
P  = Presión interna (psi)
Di = Diámetro interno (pulgadas)
t  = Espesor de pared (pulgadas)
```

#### B. Esfuerzo Axial

Causado por la tensión en la tubería:

```
σa = F / A

Donde:
σa = Esfuerzo axial (psi)
F  = Fuerza de tensión (lbf)
A  = Área de sección transversal (in²)

A = π/4 × (Do² - Di²)
```

#### C. Esfuerzo por Flexión

Causado por el radio de curvatura (gooseneck, reel):

```
σb = (E × Do/2) / R

Donde:
σb = Esfuerzo por flexión (psi)
E  = Módulo de elasticidad del material (psi)
Do = Diámetro externo (pulgadas)
R  = Radio de curvatura (pulgadas)
```

### Paso 2: Esfuerzo Equivalente (Von Mises)

Para combinar los tres esfuerzos principales:

```
σvm = √(σh² + σa² + σb² - σh×σa - σh×σb - σa×σb)

Donde:
σvm = Esfuerzo equivalente de Von Mises (psi)
```

### Paso 3: Curva S-N del Material

La curva S-N relaciona el nivel de esfuerzo con el número de ciclos hasta la falla:

```
N = A × σvm^(-m)

Donde:
N = Número de ciclos hasta falla
A = Constante del material
m = Pendiente de la curva S-N (típicamente 3-5)
σvm = Esfuerzo de Von Mises
```

Para QT-800 (material común):
- A ≈ 1.5 × 10^15
- m ≈ 3.5

### Paso 4: Factores de Corrección

#### Factor de Corrosión (fc)

```
fc = 1.0 + (0.5 × severity)

Donde severity:
- NONE: 0.0
- H2S:  0.3
- CO2:  0.2
- MIXED: 0.5
```

#### Factor de Soldadura (fw)

```
fw = Kt

Donde Kt = Factor de concentración de estrés:
- Sin soldaduras: 1.0
- Soldaduras de alta calidad: 1.2
- Soldaduras estándar: 1.5
```

#### Factor de Temperatura (ft)

```
ft = 1.0 + ((T - 70) / 1000)

Donde:
T = Temperatura (°F)
70°F = Temperatura de referencia
```

### Paso 5: Incremento de Fatiga

```
ΔD = (1 / N) × fc × fw × ft

Donde:
ΔD = Incremento de daño por un ciclo
```

### Paso 6: Fatiga Acumulada

```
Dtotal = Dtotal_anterior + ΔD

Fatiga% = Dtotal × 100
```

## Implementación en Rule Chain

### Nodo JavaScript: Fatigue Calculator

```javascript
/**
 * Nodo de ThingsBoard Rule Engine para cálculo de fatiga
 * Se ejecuta cada vez que llega telemetría relevante
 */

// Constantes de materiales
const MATERIAL_PROPERTIES = {
    'QT-700': { A: 1.2e15, m: 3.3, E: 30e6 },
    'QT-800': { A: 1.5e15, m: 3.5, E: 30e6 },
    'QT-900': { A: 1.8e15, m: 3.7, E: 30e6 }
};

// Factores de corrosión
const CORROSION_FACTORS = {
    'NONE': 1.0,
    'H2S': 1.3,
    'CO2': 1.2,
    'MIXED': 1.5
};

function calculateFatigue(msg, metadata, msgType) {
    
    // 1. Extraer telemetría actual
    var telemetry = msg;
    var pressure = telemetry.pressure || 0;          // psi
    var tension = telemetry.tension || 0;            // lbf
    var temperature = telemetry.temperature || 70;   // °F
    var direction = telemetry.direction || 'STOPPED';
    
    // 2. Obtener atributos del reel desde metadata
    var reelId = metadata.reelId;
    var reelAttributes = getReelAttributes(reelId);
    
    if (!reelAttributes) {
        return {msg: msg, metadata: metadata, msgType: msgType};
    }
    
    // Geometría de la tubería
    var Do = reelAttributes.tubing_od_inch;
    var Di = reelAttributes.tubing_id_inch;
    var t = (Do - Di) / 2;
    var materialGrade = reelAttributes.material_grade;
    
    // Radio de curvatura (depende si está en reel o gooseneck)
    var radius = direction !== 'STOPPED' 
        ? reelAttributes.typical_gooseneck_radius_inch 
        : reelAttributes.reel_core_diameter_inch / 2;
    
    // Propiedades del material
    var matProps = MATERIAL_PROPERTIES[materialGrade];
    if (!matProps) {
        matProps = MATERIAL_PROPERTIES['QT-800']; // Default
    }
    
    // Fatiga acumulada actual
    var currentFatigue = reelAttributes.accumulated_fatigue_percent || 0;
    var totalCycles = reelAttributes.total_cycles || 0;
    
    // Factores
    var corrosionFactor = CORROSION_FACTORS[reelAttributes.corrosion_environment] || 1.0;
    var weldFactor = reelAttributes.weld_stress_concentration_factor || 1.0;
    
    // 3. Calcular esfuerzos
    
    // Esfuerzo circunferencial (hoop stress)
    var sigma_h = (pressure * Di) / (2 * t);
    
    // Esfuerzo axial
    var area = Math.PI / 4 * (Math.pow(Do, 2) - Math.pow(Di, 2));
    var sigma_a = tension / area;
    
    // Esfuerzo por flexión
    var sigma_b = (matProps.E * (Do / 2)) / radius;
    
    // 4. Esfuerzo equivalente de Von Mises
    var sigma_vm = Math.sqrt(
        Math.pow(sigma_h, 2) + 
        Math.pow(sigma_a, 2) + 
        Math.pow(sigma_b, 2) - 
        sigma_h * sigma_a - 
        sigma_h * sigma_b - 
        sigma_a * sigma_b
    );
    
    // Verificar que el esfuerzo sea significativo
    if (sigma_vm < 1000) {
        // Esfuerzo muy bajo, no calcular fatiga
        return {msg: msg, metadata: metadata, msgType: msgType};
    }
    
    // 5. Ciclos hasta falla (curva S-N)
    var N = matProps.A * Math.pow(sigma_vm, -matProps.m);
    
    // Limitar N a un valor razonable
    N = Math.max(N, 1);
    N = Math.min(N, 1e9);
    
    // 6. Factor de temperatura
    var tempFactor = 1.0 + ((temperature - 70) / 1000);
    
    // 7. Incremento de fatiga
    var fatigueIncrement = (1 / N) * corrosionFactor * weldFactor * tempFactor;
    
    // Solo incrementar si hay movimiento
    var shouldIncrement = (direction === 'IN' || direction === 'OUT');
    
    if (!shouldIncrement) {
        fatigueIncrement = 0;
    }
    
    // 8. Nueva fatiga acumulada
    var newFatiguePercent = currentFatigue + (fatigueIncrement * 100);
    newFatiguePercent = Math.min(newFatiguePercent, 100); // Cap at 100%
    
    // 9. Ciclos restantes estimados
    var avgFatiguePerCycle = newFatiguePercent / Math.max(totalCycles + 1, 1);
    var remainingCycles = Math.floor((100 - newFatiguePercent) / Math.max(avgFatiguePerCycle, 0.001));
    
    // 10. Guardar registro de fatiga
    var fatigueLog = {
        reelId: reelId,
        timestamp: Date.now(),
        
        // Parámetros del ciclo
        pressure_psi: Math.round(pressure),
        tension_lbf: Math.round(tension),
        bend_radius_inch: radius,
        temperature_f: Math.round(temperature),
        
        // Esfuerzos calculados
        hoop_stress_psi: Math.round(sigma_h),
        axial_stress_psi: Math.round(sigma_a),
        bending_stress_psi: Math.round(sigma_b),
        von_mises_stress_psi: Math.round(sigma_vm),
        
        // Fatiga
        cycles_to_failure: Math.round(N),
        fatigue_increment_percent: fatigueIncrement * 100,
        fatigue_before_percent: currentFatigue,
        fatigue_after_percent: newFatiguePercent,
        
        // Factores
        corrosion_factor: corrosionFactor,
        weld_factor: weldFactor,
        temperature_factor: tempFactor,
        
        // Metadata
        calculation_method: 'PALMGREN_MINER',
        cycle_type: direction
    };
    
    // Guardar en base de datos (llamada a servicio Java)
    saveFatigueLog(fatigueLog);
    
    // 11. Actualizar atributos del reel
    updateReelAttributes(reelId, {
        accumulated_fatigue_percent: newFatiguePercent,
        total_cycles: totalCycles + (shouldIncrement ? 1 : 0),
        estimated_remaining_cycles: remainingCycles
    });
    
    // 12. Generar alarmas si es necesario
    if (newFatiguePercent >= 95) {
        createAlarm(reelId, 'CT_FATIGUE_CRITICAL', 
                   'Critical fatigue level: ' + newFatiguePercent.toFixed(2) + '%',
                   'CRITICAL');
    } else if (newFatiguePercent >= 80) {
        createAlarm(reelId, 'CT_FATIGUE_HIGH',
                   'High fatigue level: ' + newFatiguePercent.toFixed(2) + '%',
                   'MAJOR');
    } else {
        clearAlarm(reelId, 'CT_FATIGUE_CRITICAL');
        clearAlarm(reelId, 'CT_FATIGUE_HIGH');
    }
    
    // 13. Enviar resultado al siguiente nodo
    var result = {
        fatigue_percent: newFatiguePercent,
        fatigue_increment: fatigueIncrement * 100,
        remaining_cycles: remainingCycles,
        von_mises_stress: sigma_vm,
        fatigue_status: getFatigueStatus(newFatiguePercent)
    };
    
    return {
        msg: result, 
        metadata: metadata, 
        msgType: msgType
    };
}

function getFatigueStatus(fatiguePercent) {
    if (fatiguePercent < 40) return 'EXCELLENT';
    if (fatiguePercent < 60) return 'GOOD';
    if (fatiguePercent < 80) return 'MODERATE';
    if (fatiguePercent < 95) return 'HIGH';
    return 'CRITICAL';
}

// Funciones auxiliares (implementadas en Java/TB)
function getReelAttributes(reelId) {
    // Obtener atributos del asset reel
    // Implementado como llamada al servicio de atributos de TB
}

function saveFatigueLog(logData) {
    // Guardar en ct_fatigue_log
    // Implementado como llamada REST a servicio Java
}

function updateReelAttributes(reelId, updates) {
    // Actualizar atributos del reel
}

function createAlarm(entityId, alarmType, message, severity) {
    // Crear alarma en TB
}

function clearAlarm(entityId, alarmType) {
    // Limpiar alarma en TB
}

// Ejecutar cálculo
return calculateFatigue(msg, metadata, msgType);
```

## Validación y Calibración

### Datos de Prueba

Validar el algoritmo con datos conocidos:

```javascript
// Test Case 1: Ciclo típico de cleanout
var testCase1 = {
    pressure: 3000,      // psi
    tension: 15000,      // lbf
    temperature: 75,     // °F
    Do: 2.0,            // inch
    Di: 1.75,           // inch
    radius: 72,         // inch (gooseneck)
    material: 'QT-800',
    corrosion: 'NONE'
};

// Resultado esperado: ~0.001% de fatiga por ciclo
```

### Comparación con Modelos Comerciales

Validar contra software comercial:
- CIRCA™ (Baker Hughes)
- CYCLE™ (Baker Hughes)
- FACT (Medco North)

### Ajuste de Constantes

Las constantes A y m pueden requerir ajuste basado en:
- Datos históricos de fallas
- Resultados de laboratorio
- Experiencia operacional

## Optimizaciones de Performance

### 1. Cálculo Condicional

Solo calcular fatiga cuando:
- Hay movimiento de tubería (direction != 'STOPPED')
- Los valores han cambiado significativamente
- Ha pasado un tiempo mínimo (ej: 5 segundos)

```javascript
// Throttle de cálculos
var lastCalculation = metadata.lastFatigueCalcTime || 0;
var now = Date.now();
if (now - lastCalculation < 5000) { // 5 segundos
    return {msg: msg, metadata: metadata, msgType: msgType};
}
metadata.lastFatigueCalcTime = now;
```

### 2. Batch Processing

Para trabajos largos, acumular múltiples ciclos y calcular en batch:

```javascript
// Acumular datos cada minuto en lugar de cada segundo
```

### 3. Caching de Atributos

Cachear atributos del reel para evitar múltiples lecturas:

```javascript
var reelCache = {}; // En memoria del nodo
if (!reelCache[reelId]) {
    reelCache[reelId] = getReelAttributes(reelId);
}
```

## Reportes de Fatiga

### Reporte Diario de Fatiga

Generado automáticamente al finalizar cada trabajo:

```
FATIGUE ANALYSIS REPORT
Job: JOB-789
Reel: REEL-R001 (2" QT-800)
Date: 2026-01-24

SUMMARY:
- Fatigue before job: 78.5%
- Fatigue after job: 82.3%
- Fatigue consumed: 3.8%
- Cycles performed: 12

STATISTICS:
- Max Von Mises Stress: 45,250 psi
- Avg Von Mises Stress: 38,100 psi
- Min Cycles to Failure (worst case): 2,450

ESTIMATED REMAINING LIFE:
- Remaining fatigue capacity: 17.7%
- Estimated remaining cycles: 850
- Projected retirement date: 2026-03-15

RECOMMENDATIONS:
⚠ Reel approaching high fatigue level (>80%)
⚠ Schedule inspection within next 30 days
⚠ Consider retiring reel after ~850 more cycles
✓ Current condition acceptable for continued use
```

### Gráfico de Evolución de Fatiga

Mostrar en dashboard:
- Fatiga acumulada vs tiempo
- Fatiga acumulada vs ciclos
- Tasa de consumo de fatiga
- Proyección a retiro

## Machine Learning (Futuro)

### Predicción Mejorada

Usar ML para:
- Predecir vida útil más precisamente
- Identificar patrones de uso problemáticos
- Detectar anomalías en consumo de fatiga
- Optimizar parámetros operacionales

### Modelo Propuesto

```python
# Ejemplo conceptual
from sklearn.ensemble import RandomForestRegressor

# Features
X = [
    'pressure', 'tension', 'temperature', 
    'speed', 'depth', 'job_type',
    'material_grade', 'corrosion_environment',
    'operational_hours'
]

# Target
y = 'fatigue_increment_actual'

# Entrenar modelo
model = RandomForestRegressor()
model.fit(X_train, y_train)

# Predecir
predicted_fatigue = model.predict(X_current)
```

## Referencias

### Estándares Técnicos
- API Specification 5ST: Specification for Coiled Tubing
- NACE MR0175: Petroleum and Natural Gas Industries
- ASME B31.3: Process Piping

### Literatura Científica
- Palmgren, A. (1924). "Die Lebensdauer von Kugellagern"
- Miner, M. A. (1945). "Cumulative Damage in Fatigue"
- Newman, K. (1998). "Fatigue Life Prediction of Coiled Tubing"

### Software Comercial
- Baker Hughes CIRCA™ Suite
- Schlumberger ACTive™ Services
- Halliburton RealSense™

---

**Versión**: 1.0.0  
**Última Actualización**: Enero 2026
