# RV Simulator - Guía Detallada de Uso

## Introducción

El RV (Reservoir) Simulator genera telemetría realista de producción de pozos petroleros usando curvas de declinación de Arps, el método estándar de la industria para predecir producción de reservorios.

## Fundamentos Teóricos

### Curvas de Declinación de Arps

J.J. Arps (1945) desarrolló ecuaciones empíricas para modelar la declinación de producción en pozos:

#### 1. Declinación Exponencial (b = 0)

```
q(t) = qi * exp(-Di * t)
Np(t) = (qi / Di) * (1 - exp(-Di * t))
```

**Características**:
- Declinación más rápida y constante en porcentaje
- Típica de pozos de shale/tight oil
- EUR finito

**Cuándo usar**:
- Pozos no convencionales (Eagle Ford, Bakken, Permian)
- Producción con drive de expansión de roca
- Primeros 2-3 años de producción

**Parámetros típicos**:
- qi: 800-2000 bbl/day
- Di: 0.30-0.60 /year (30-60% decline anual)

#### 2. Declinación Hiperbólica (0 < b < 1)

```
q(t) = qi / (1 + b*Di*t)^(1/b)
Np(t) = (qi^b / (Di*(1-b))) * (qi^(1-b) - q(t)^(1-b))
```

**Características**:
- Declinación moderada que disminuye con el tiempo
- Más común en reservorios convencionales
- b-factor determina curvatura

**Cuándo usar**:
- Reservorios convencionales con drive natural
- Producción con presión de mantenimiento parcial
- Vida útil de 5-15 años

**Parámetros típicos**:
- qi: 300-1500 bbl/day
- Di: 0.20-0.50 /year
- b: 0.3-0.8 (típico: 0.5-0.7)

#### 3. Declinación Armónica (b = 1)

```
q(t) = qi / (1 + Di*t)
Np(t) = (qi / Di) * ln(1 + Di*t)
```

**Características**:
- Declinación más lenta
- EUR teóricamente infinito
- Poco común en la práctica

**Cuándo usar**:
- Pozos con waterflood o pressure maintenance
- Producción asistida (waterflooding, gas injection)
- Vida útil > 20 años

**Parámetros típicos**:
- qi: 200-1000 bbl/day
- Di: 0.15-0.35 /year

## Configuración Paso a Paso

### 1. Preparar ThingsBoard

#### Crear Devices (fuentes de datos)

```bash
# Para cada pozo, crear un Device en ThingsBoard:
1. Navegar a: Devices → + (Add Device)
2. Nombre: "Eagle Ford 1H"
3. Device Profile: "default"
4. Guardar

5. Click en el device → Credentials
6. Copiar "Access Token": ej. "A1B2C3D4E5F6G7H8"
```

#### Crear Assets (digital twins)

```bash
1. Navegar a: Assets → + (Add Asset)
2. Nombre: "Eagle Ford 1H Digital Twin"
3. Asset Profile: "Well" o "default"
4. Guardar
```

### 2. Configurar el Simulador

#### Archivo config.yaml

```yaml
mqtt:
  broker_url: "tcp://localhost:1883"      # Tu ThingsBoard MQTT
  reconnect_period_ms: 5000

simulation:
  rate_hz: 0.1                            # 1 dato cada 10 segundos
  realtime_factor: 365.0                  # 1 año simulado = 1 día real

wells:
  # Ejemplo 1: Pozo de Shale (Exponential)
  - deviceToken: "A1B2C3D4E5F6G7H8"       # Token de ThingsBoard
    wellName: "Eagle Ford 1H"
    declineType: "exponential"
    initialRate: 1200.0                    # bbl/day inicial
    declineRate: 0.35                      # 35% decline anual
    startDate: "2024-01-01T00:00:00Z"

  # Ejemplo 2: Pozo Convencional (Hyperbolic)
  - deviceToken: "B2C3D4E5F6G7H8I9"
    wellName: "Permian 2H"
    declineType: "hyperbolic"
    initialRate: 800.0
    declineRate: 0.45
    hyperbolicExponent: 0.6                # b-factor
    startDate: "2023-06-15T00:00:00Z"

  # Ejemplo 3: Pozo con Waterflood (Harmonic)
  - deviceToken: "C3D4E5F6G7H8I9J0"
    wellName: "Bakken 3H"
    declineType: "harmonic"
    initialRate: 600.0
    declineRate: 0.25
    startDate: "2022-03-20T00:00:00Z"
```

### 3. Ejecutar el Simulador

#### Opción A: Node.js Local

```bash
# 1. Instalar dependencias (primera vez)
cd /path/to/nexus/dev/simulators
npm install

# 2. Build
npm run build

# 3. Configurar
cd rv-simulator
cp config.example.yaml config.yaml
nano config.yaml  # Editar con tus tokens

# 4. Ejecutar
npm start

# Modo desarrollo (con auto-reload)
npm run dev
```

#### Opción B: Docker

```bash
# 1. Build imagen
cd /path/to/nexus/dev/simulators
docker-compose build rv-simulator

# 2. Configurar
cd rv-simulator
cp config.example.yaml config.yaml
nano config.yaml  # Editar con tus tokens

# 3. Ejecutar
cd ..
docker-compose up rv-simulator

# Background
docker-compose up -d rv-simulator

# Ver logs
docker-compose logs -f rv-simulator
```

### 4. Configurar Data Mapping en NEXUS

#### Via UI de ThingsBoard

```bash
1. Navegar a: Data Mapping → Data Sources

2. Click "Apply Template"

3. Completar formulario:
   - Source Device: "Eagle Ford 1H"
   - Target Asset: "Eagle Ford 1H Digital Twin"
   - Module: "RV - Reservoir"
   - Template: Seleccionar template RV disponible

4. Click "Apply"

5. Sistema crea automáticamente:
   - DataSourceConfig (enlace Device → Asset)
   - DataMappingRules (transformaciones de telemetría)
```

#### Mapping Rules Creados

| Source Key (Device) | Target Key (Asset) | Transform |
|---------------------|-------------------|-----------|
| oil_rate | production_rate | Direct |
| cumulative_oil | cumulative_production | Direct |
| water_cut | water_cut_percentage | Direct |
| reservoir_pressure | formation_pressure | Direct |
| liquid_rate | total_liquid_rate | Direct |
| water_rate | water_production_rate | Direct |

### 5. Verificar Flujo de Datos

#### En Device (datos crudos del simulador)

```bash
1. ThingsBoard → Devices → "Eagle Ford 1H"
2. Tab: "Latest Telemetry"
3. Deberías ver:
   - oil_rate: 1156.32
   - cumulative_oil: 11650.00
   - water_cut: 0.00
   - reservoir_pressure: 3988.45
   - liquid_rate: 1156.32
   - water_rate: 0.00
   - days_on_production: 10
   - timestamp: 2024-01-11T...
```

#### En Asset (datos mapeados/transformados)

```bash
1. ThingsBoard → Assets → "Eagle Ford 1H Digital Twin"
2. Tab: "Latest Telemetry"
3. Deberías ver:
   - production_rate: 1156.32
   - cumulative_production: 11650.00
   - water_cut_percentage: 0.00
   - formation_pressure: 3988.45
   - total_liquid_rate: 1156.32
   - water_production_rate: 0.00
```

## Salida del Simulador

### Console Output

```
═══════════════════════════════════════════════════
   NEXUS RV Simulator - Reservoir Production
═══════════════════════════════════════════════════

📊 Initializing 3 well decline models...

  ✅ Eagle Ford 1H:
     Type: exponential
     qi: 1200.00 bbl/day
     Di: 0.3500 /year
     EUR: 1251429 bbl
     Start: 2024-01-01T00:00:00Z

  ✅ Permian 2H:
     Type: hyperbolic
     qi: 800.00 bbl/day
     Di: 0.4500 /year
     b: 0.600
     EUR: 2847563 bbl
     Start: 2023-06-15T00:00:00Z

  ✅ Bakken 3H:
     Type: harmonic
     qi: 600.00 bbl/day
     Di: 0.2500 /year
     EUR: ∞ bbl
     Start: 2022-03-20T00:00:00Z

🔌 Connecting MQTT clients...

  ✅ Eagle Ford 1H connected
  ✅ Permian 2H connected
  ✅ Bakken 3H connected

✅ All MQTT clients connected

🚀 Starting RV Simulator:
   Rate: 0.1 Hz
   Interval: 10000 ms
   Realtime Factor: 365x

✅ Simulator started. Press Ctrl+C to stop.

📈 Eagle Ford 1H [Day 10]: Oil=1156.32 bbl/day, Cum=11650 bbl, WC=0.00%, P=3988 psi
📈 Permian 2H [Day 197]: Oil=615.45 bbl/day, Cum=95842 bbl, WC=0.00%, P=3456 psi
📈 Bakken 3H [Day 1050]: Oil=385.21 bbl/day, Cum=567890 bbl, WC=48.32%, P=2987 psi
...
```

## Fenómenos Simulados

### 1. Declinación de Producción

El simulador calcula la tasa de producción según la ecuación de Arps correspondiente:

```typescript
// Ejemplo: Exponential decline
const t = daysSinceStart / 365.25;  // Convertir a años
const rate = qi * Math.exp(-Di * t);
```

### 2. Water Breakthrough (Irrupción de Agua)

Después de ~2 años, el agua comienza a aparecer:

```typescript
// Aumento logístico de water cut
const waterBreakthrough = 2; // años
if (t > waterBreakthrough) {
  const timeSince = t - waterBreakthrough;
  waterCut = 1 / (1 + Math.exp(-0.5 * timeSince)) * 0.8; // Max 80%
}
```

**Comportamiento**:
- Años 0-2: Water cut = 0%
- Año 3: Water cut ≈ 20%
- Año 5: Water cut ≈ 50%
- Año 10: Water cut ≈ 75%

### 3. Declinación de Presión

La presión del reservorio declina más lento que la producción:

```typescript
const initialPressure = 4000; // psi
const pressureDeclineRate = 0.3; // 30% de la tasa de producción
const pressure = initialPressure * Math.exp(-Di * t * pressureDeclineRate);
```

**Comportamiento**:
- Año 0: 4000 psi
- Año 5: ~3300 psi (exponential con Di=0.35)
- Año 10: ~2700 psi

### 4. Ruido Realista

Cada valor incluye variabilidad estocástica:

```typescript
// ±5% ruido Gaussiano
const noisyRate = gaussian(rate, rate * 0.05);

// Spikes aleatorios (1% probabilidad)
if (random() < 0.01) {
  noisyRate += random(-stdDev*3, +stdDev*3);
}
```

## Casos de Uso

### Caso 1: Evaluación Rápida de Template

**Objetivo**: Verificar que el mapping template funciona correctamente

**Configuración**:
```yaml
simulation:
  rate_hz: 1.0              # 1 Hz (datos cada segundo)
  realtime_factor: 8760.0   # 1 año = 1 hora real

wells:
  - deviceToken: "TEST_TOKEN"
    wellName: "Test Well"
    declineType: "exponential"
    initialRate: 1000.0
    declineRate: 0.40
    startDate: "2024-01-01T00:00:00Z"
```

**Resultado**: En 1 hora real verás 10 años de producción simulada

### Caso 2: Entrenamiento y Demo

**Objetivo**: Mostrar comportamiento de pozos a largo plazo

**Configuración**:
```yaml
simulation:
  rate_hz: 0.1
  realtime_factor: 1825.0   # 5 años = 1 día real

wells:
  - # Exponential (shale)
  - # Hyperbolic (conventional)
  - # Harmonic (waterflooded)
```

**Resultado**: Demostrar diferencias entre tipos de declinación

### Caso 3: Desarrollo de Dashboards

**Objetivo**: Generar datos para diseño de visualizaciones

**Configuración**:
```yaml
simulation:
  rate_hz: 0.5              # 0.5 Hz (cada 2 segundos)
  realtime_factor: 365.0    # 1 año = 1 día real

wells:
  # Múltiples pozos con diferentes características
  - # Young well (high rate, steep decline)
  - # Mature well (low rate, slow decline)
  - # Watered out well (high water cut)
```

**Resultado**: Datos variados para testing de widgets

## Solución de Problemas

### Problema: Simulador no conecta a MQTT

**Síntomas**:
```
❌ MQTT connection error: connect ECONNREFUSED 127.0.0.1:1883
```

**Soluciones**:
1. Verificar ThingsBoard está corriendo: `docker ps | grep thingsboard`
2. Verificar puerto MQTT abierto: `netstat -an | grep 1883`
3. Probar conexión: `telnet localhost 1883`
4. Revisar broker_url en config.yaml

### Problema: No hay datos en Device

**Síntomas**:
- Simulador muestra "published"
- Latest Telemetry vacío en Device

**Soluciones**:
1. Verificar access token correcto
2. Revisar logs de ThingsBoard: `docker logs thingsboard`
3. Verificar Device existe y está activo
4. Probar con MQTT client manual:
   ```bash
   mosquitto_pub -h localhost -t v1/devices/me/telemetry \
     -u "YOUR_TOKEN" -m '{"test":123}'
   ```

### Problema: Datos en Device pero no en Asset

**Síntomas**:
- Device tiene Latest Telemetry
- Asset está vacío

**Soluciones**:
1. Verificar DataSourceConfig existe:
   - UI → Data Mapping → Data Sources
   - Buscar el Device
2. Verificar MappingRules creadas:
   - Debe haber reglas para cada key
3. Verificar Rule Chain activo:
   - UI → Rule Chains
   - Buscar "Root Rule Chain" o similar
   - Verificar tiene TbNexusDataDistributionNode
4. Revisar logs del nodo

### Problema: Water cut siempre en 0%

**Explicación**: Water breakthrough ocurre después de 2 años simulados

**Solución**:
```yaml
# Acelerar simulación para ver water cut
simulation:
  realtime_factor: 1825.0   # 5 años = 1 día

# O modificar startDate a fecha antigua
wells:
  - startDate: "2020-01-01T00:00:00Z"  # 4+ años atrás
```

### Problema: Valores negativos o infinitos

**Síntomas**:
```
oil_rate: NaN
reservoir_pressure: -1234.56
```

**Soluciones**:
1. Verificar parámetros válidos:
   - qi > 0
   - Di > 0
   - 0 < b < 1 (para hyperbolic)
2. Revisar startDate no es futuro
3. Verificar realtime_factor razonable (< 10000)

## Performance y Escalabilidad

### Recursos por Pozo

- **Memoria**: ~5-10 MB
- **CPU**: < 1% @ 1 Hz
- **Red**: ~500 bytes/mensaje
  - 0.1 Hz: 50 B/s
  - 1.0 Hz: 500 B/s

### Límites Recomendados

| Escenario | Pozos | Rate Hz | Realtime Factor |
|-----------|-------|---------|-----------------|
| Testing | 1-5 | 1.0 | 1000-5000 |
| Demo | 5-20 | 0.1 | 100-500 |
| Producción | 20-100 | 0.05-0.1 | 1-100 |

### Múltiples Instancias

Para > 100 pozos, ejecutar múltiples contenedores:

```bash
# Container 1: Pozos 1-50
docker-compose up rv-simulator-1

# Container 2: Pozos 51-100
docker-compose up rv-simulator-2
```

## Referencias

1. **Arps, J.J. (1945)**: "Analysis of Decline Curves", *Transactions of the AIME*
2. **SPE 0918-0228-JPT**: "Decline Curve Analysis Using Type Curves"
3. **ThingsBoard Docs**: https://thingsboard.io/docs/mqtt-over-tcp/
4. **NEXUS Data Mapping**: Ver documentación del proyecto

## Contacto y Soporte

Para problemas o preguntas:
1. Revisar esta guía
2. Consultar [README.md](README.md)
3. Revisar logs del simulador
4. Abrir issue en el repositorio
