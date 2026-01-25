# Nodos Personalizados - Módulo Coiled Tubing

## Visión General

El módulo Coiled Tubing incluye **nodos personalizados de ThingsBoard** que encapsulan toda la lógica específica del dominio. Estos nodos son componentes Java que se pueden usar directamente en Rule Chains sin necesidad de configurar múltiples nodos estándar.

## Nodos Disponibles

### 1. CTFatigueCalculationNode

**Propósito**: Cálculo automático de fatiga en reels de tubería continua.

**Ubicación**: `org.thingsboard.nexus.ct.rule.CTFatigueCalculationNode`

**Funcionalidad**:
- ✅ Recibe telemetría del reel (presión, tensión, temperatura, dirección)
- ✅ Obtiene atributos del reel automáticamente (geometría, material, fatiga actual)
- ✅ Calcula esfuerzos: circunferencial, axial, flexión
- ✅ Aplica algoritmo de Palmgren-Miner para fatiga acumulada
- ✅ Guarda registro en base de datos vía REST
- ✅ Actualiza atributos del reel
- ✅ Retorna estado de fatiga para generar alarmas

**Telemetría Requerida**:
```json
{
  "pressure": 15000,      // psi
  "tension": 25000,       // lbf
  "temperature": 85,      // °F
  "direction": "IN"       // IN, OUT, STOPPED
}
```

**Atributos del Reel Requeridos** (Server Scope):
- `tubing_od_inch` - Diámetro externo (default: 2.375)
- `tubing_id_inch` - Diámetro interno (default: 1.995)
- `material_grade` - Grado del material: QT-800, QT-900, QT-1000 (default: QT-800)
- `typical_gooseneck_radius_inch` - Radio del gooseneck (default: 72.0)
- `reel_core_diameter_inch` - Diámetro del núcleo (default: 96.0)
- `accumulated_fatigue_percent` - Fatiga acumulada actual (default: 0.0)
- `total_cycles` - Total de ciclos (default: 0)
- `corrosion_environment` - SWEET, MILDLY_SOUR, SOUR, HIGHLY_CORROSIVE (default: SWEET)
- `weld_stress_concentration_factor` - Factor de soldadura (default: 1.0)

**Salida**:
```java
FatigueCalculationResult {
  success: boolean
  reelId: UUID
  accumulatedFatiguePercent: double
  fatigueStatus: "NORMAL" | "HIGH" | "CRITICAL"
  vonMisesStressPsi: double
  remainingCycles: int
  // ... más campos
}
```

**Uso en Rule Chain**:
1. Crear nodo de tipo "Script" o "Custom"
2. Referenciar la clase `CTFatigueCalculationNode`
3. Conectar a entrada de telemetría
4. Conectar salida a nodos de alarma según `fatigueStatus`

**Ejemplo de Código**:
```java
CTFatigueCalculationNode node = new CTFatigueCalculationNode();

// Construir telemetría
TelemetryData telemetry = new TelemetryData();
telemetry.setPressure(15000);
telemetry.setTension(25000);
telemetry.setTemperature(85);
telemetry.setDirection("IN");

// Construir atributos del reel
ReelAttributes attributes = new ReelAttributes();
attributes.setReelId(reelId);
attributes.setTenantId(tenantId);
attributes.setTubingOdInch(2.375);
attributes.setTubingIdInch(1.995);
attributes.setMaterialGrade("QT-800");
// ... más atributos

// Calcular fatiga
FatigueCalculationResult result = node.calculate(telemetry, attributes);

if (result.isSuccess()) {
    // Guardar en BD
    node.saveFatigueLog(result, "http://localhost:8080");
    
    // Verificar estado
    if (result.getFatigueStatus().equals("CRITICAL")) {
        // Generar alarma crítica
    }
}
```

---

### 2. CTJobSimulationNode

**Propósito**: Simulación de trabajos de coiled tubing para validar factibilidad y optimizar parámetros.

**Ubicación**: `org.thingsboard.nexus.ct.rule.CTJobSimulationNode`

**Funcionalidad**:
- ✅ Valida factibilidad del trabajo
- ✅ Calcula fuerzas (hookload, fricción, pandeo)
- ✅ Calcula presiones hidráulicas
- ✅ Estima tiempos de operación por fase
- ✅ Predice fatiga acumulada
- ✅ Identifica riesgos potenciales

**Parámetros de Entrada**:
```java
JobParameters {
  jobId: UUID
  wellName: String
  targetDepthFt: double
  wellboreDiameterInch: double
  maxInclinationDeg: double
  tubingOdInch: double
  tubingIdInch: double
  tubingLengthFt: double
  fluidDensityPpg: double
  pumpRateBpm: Double (opcional)
  maxPressurePsi: double
  maxRunningSpeedFtMin: double
  unitMaxPressurePsi: double
  unitMaxTensionLbf: double
  estimatedTreatmentHours: Double (opcional)
}
```

**Salida**:
```java
SimulationResult {
  feasibility: {
    isFeasible: boolean
    limitingFactors: List<String>
    warnings: List<String>
  }
  forces: {
    depths: List<Double>
    hookloads: List<Double>
    maxHookload: double
    bucklingMargins: List<Double>
  }
  hydraulics: {
    depths: List<Double>
    pressures: List<Double>
    maxPressure: double
  }
  times: {
    riggingUpHours: double
    runningInHours: double
    onDepthHours: double
    pullingOutHours: double
    riggingDownHours: double
    totalDurationHours: double
  }
  fatigue: {
    estimatedCycles: int
    estimatedFatiguePercent: double
  }
  risks: List<Risk>
}
```

**Uso en Rule Chain**:
```java
CTJobSimulationNode node = new CTJobSimulationNode();

JobParameters params = new JobParameters();
params.setWellName("Well-123");
params.setTargetDepthFt(15000);
params.setTubingOdInch(2.375);
params.setTubingIdInch(1.995);
params.setTubingLengthFt(20000);
params.setUnitMaxPressurePsi(35000);
params.setUnitMaxTensionLbf(80000);
// ... más parámetros

SimulationResult result = node.simulate(params);

if (result.getFeasibility().isFeasible()) {
    System.out.println("Job is feasible!");
    System.out.println("Estimated duration: " + 
                       result.getTimes().getTotalDurationHours() + " hours");
    System.out.println("Max hookload: " + 
                       result.getForces().getMaxHookload() + " lbf");
} else {
    System.out.println("Job NOT feasible:");
    result.getFeasibility().getLimitingFactors().forEach(System.out::println);
}
```

---

## Servicios REST

### CTFatigueService

**Endpoints**:
- `POST /api/nexus/ct/fatigue/log` - Guardar cálculo de fatiga
- `GET /api/nexus/ct/fatigue/reel/{id}/history` - Histórico de fatiga
- `GET /api/nexus/ct/fatigue/reel/{id}/latest` - Último cálculo

### CTSimulationService

**Endpoints**:
- `POST /api/nexus/ct/simulation/job/{jobId}` - Simular trabajo existente
- `POST /api/nexus/ct/simulation/custom` - Simular trabajo personalizado

---

## Configuración

### application-ct.yml

```yaml
ct:
  module:
    backend-url: http://localhost:8080
    fatigue-calculation-enabled: true
    critical-fatigue-threshold: 95.0
    high-fatigue-threshold: 80.0
    job-simulation-enabled: true
    simulation-steps: 100
```

### Variables de Entorno

- `CT_BACKEND_URL` - URL del backend (default: http://localhost:8080)
- `CT_FATIGUE_ENABLED` - Habilitar cálculo de fatiga (default: true)
- `CT_CRITICAL_THRESHOLD` - Umbral crítico % (default: 95.0)
- `CT_HIGH_THRESHOLD` - Umbral alto % (default: 80.0)
- `CT_SIMULATION_ENABLED` - Habilitar simulación (default: true)
- `CT_LOG_LEVEL` - Nivel de log (default: INFO)

---

## Instalación y Registro

### 1. Compilar el Módulo

```bash
cd /home/diazhh/dev/nexus
mvn clean install -pl common/ct-module -DskipTests
```

### 2. Incluir en Aplicación

Agregar dependencia en `application/pom.xml`:

```xml
<dependency>
    <groupId>org.thingsboard.common</groupId>
    <artifactId>ct-module</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 3. Activar Perfil

En `application.yml`:

```yaml
spring:
  profiles:
    include: ct
```

### 4. Usar en Rule Chains

Los nodos están disponibles como clases Java que se pueden instanciar y usar directamente en scripts de Rule Chains o en nodos personalizados.

---

## Ventajas de Nodos Personalizados

✅ **Encapsulación**: Toda la lógica en un solo lugar
✅ **Reutilización**: Mismo código en múltiples Rule Chains
✅ **Mantenibilidad**: Cambios centralizados
✅ **Testing**: Fácil de testear unitariamente
✅ **Performance**: Código Java compilado vs JavaScript interpretado
✅ **Tipado**: Seguridad de tipos en tiempo de compilación
✅ **Debugging**: Stack traces completos

---

## Próximos Pasos

1. ✅ Nodos personalizados creados
2. ✅ Servicios REST implementados
3. ⏳ Crear descriptores UI para ThingsBoard
4. ⏳ Registrar nodos en plugin registry
5. ⏳ Crear tests unitarios
6. ⏳ Documentar casos de uso

---

**Versión**: 1.0.0  
**Fecha**: Enero 2026  
**Autor**: Nexus Development Team
