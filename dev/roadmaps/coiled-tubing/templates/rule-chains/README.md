# Rule Chains - Módulo Coiled Tubing

Este directorio contiene las definiciones de Rule Chains para el módulo de Coiled Tubing.

## Rule Chains Disponibles

### 1. ct-fatigue-calculation.json

**Propósito**: Cálculo en tiempo real de fatiga acumulada en reels de tubería continua.

**Flujo**:
1. **Input** - Recibe telemetría de assets
2. **Filter Reel Telemetry** - Filtra solo telemetría de reels CT
3. **Get Reel Attributes** - Obtiene atributos del reel (geometría, material, fatiga actual)
4. **Calculate Fatigue** - Aplica algoritmo de Palmgren-Miner para calcular fatiga
5. **Save Fatigue Log** - Guarda registro en base de datos vía REST API
6. **Update Reel Attributes** - Actualiza atributos del reel con nueva fatiga
7. **Check Fatigue Level** - Evalúa nivel de fatiga
8. **Create Critical/High Alarm** - Genera alarmas según umbral
9. **Clear Alarms** - Limpia alarmas cuando fatiga es normal

**Algoritmo**:
- Calcula esfuerzos: circunferencial (hoop), axial, flexión
- Combina con Von Mises stress
- Aplica curva S-N del material
- Acumula daño según regla de Palmgren-Miner
- Aplica factores de corrección (corrosión, soldadura, temperatura)

**Umbrales de Alarma**:
- **CRITICAL**: Fatiga ≥ 95%
- **HIGH**: Fatiga ≥ 80%
- **NORMAL**: Fatiga < 80%

**Parámetros de Entrada** (telemetría):
- `pressure` - Presión interna (psi)
- `tension` - Tensión axial (lbf)
- `temperature` - Temperatura (°F)
- `direction` - Dirección de movimiento ('IN', 'OUT', 'STOPPED')

**Atributos Requeridos** (del reel):
- `tubing_od_inch` - Diámetro externo
- `tubing_id_inch` - Diámetro interno
- `material_grade` - Grado del material (QT-800, QT-900, QT-1000)
- `typical_gooseneck_radius_inch` - Radio del gooseneck
- `reel_core_diameter_inch` - Diámetro del núcleo del reel
- `accumulated_fatigue_percent` - Fatiga acumulada actual
- `total_cycles` - Total de ciclos realizados
- `corrosion_environment` - Ambiente corrosivo
- `weld_stress_concentration_factor` - Factor de concentración de estrés en soldaduras

**Salida**:
- Registro en tabla `ct_fatigue_log`
- Atributos actualizados en el reel
- Alarmas generadas si aplica

## Instalación

### Opción 1: Importar vía UI de ThingsBoard

1. Ir a **Rule Chains** en ThingsBoard
2. Click en **+** (Add Rule Chain)
3. Click en **Import Rule Chain**
4. Seleccionar el archivo JSON
5. Configurar como Root Rule Chain si es necesario

### Opción 2: Importar vía REST API

```bash
curl -X POST http://localhost:8080/api/ruleChain \
  -H "Content-Type: application/json" \
  -H "X-Authorization: Bearer $JWT_TOKEN" \
  -d @ct-fatigue-calculation.json
```

## Configuración

### 1. Configurar Endpoint REST

Editar el nodo "Save Fatigue Log" y actualizar la URL si el backend no está en localhost:

```json
{
  "restEndpointUrlPattern": "http://YOUR_SERVER:8080/api/nexus/ct/fatigue/log"
}
```

### 2. Asignar a Assets de Reels

Para que la Rule Chain procese telemetría de reels:

1. Ir a **Assets** → Seleccionar un reel CT
2. En **Relations**, agregar relación al Root Rule Chain
3. O configurar la Rule Chain como Root para todo el tenant

### 3. Configurar Atributos del Reel

Asegurarse de que cada reel tenga los atributos requeridos configurados en **Server Attributes**.

## Testing

### Enviar Telemetría de Prueba

```bash
# Obtener access token del reel
REEL_TOKEN="YOUR_REEL_ACCESS_TOKEN"

# Enviar telemetría
curl -X POST http://localhost:8080/api/v1/$REEL_TOKEN/telemetry \
  -H "Content-Type: application/json" \
  -d '{
    "pressure": 15000,
    "tension": 25000,
    "temperature": 85,
    "direction": "IN"
  }'
```

### Verificar Resultados

1. **Logs de Fatiga**: Consultar tabla `ct_fatigue_log`
2. **Atributos del Reel**: Ver `accumulated_fatigue_percent` en Server Attributes
3. **Alarmas**: Revisar sección de Alarms del reel
4. **Debug**: Habilitar Debug Mode en los nodos de la Rule Chain

## Mantenimiento

### Ajustar Propiedades de Materiales

Editar el nodo "Calculate Fatigue" y modificar el objeto `MATERIAL_PROPERTIES`:

```javascript
var MATERIAL_PROPERTIES = {
    'QT-800': {E: 30e6, A: 1e15, m: 3.5, yield: 80000},
    'QT-900': {E: 30e6, A: 8e14, m: 3.3, yield: 90000},
    'CUSTOM': {E: 29e6, A: 1.2e15, m: 3.7, yield: 85000}
};
```

### Ajustar Factores de Corrosión

```javascript
var CORROSION_FACTORS = {
    'SWEET': 1.0,
    'MILDLY_SOUR': 1.2,
    'SOUR': 1.5,
    'HIGHLY_CORROSIVE': 2.0,
    'CUSTOM_ENV': 1.8
};
```

### Modificar Umbrales de Alarma

Editar el script del nodo "Check Fatigue Level":

```javascript
if (msg.accumulatedFatiguePercent >= 90) {  // Cambiar de 95 a 90
    return ['Critical'];
}
```

## Troubleshooting

### Problema: No se generan logs de fatiga

**Solución**:
1. Verificar que el backend esté corriendo
2. Verificar URL del endpoint REST
3. Revisar logs del nodo "Save Fatigue Log"
4. Verificar que el reel tenga todos los atributos requeridos

### Problema: Cálculos incorrectos

**Solución**:
1. Habilitar Debug Mode en "Calculate Fatigue"
2. Verificar valores de atributos del reel
3. Revisar propiedades del material
4. Validar telemetría de entrada

### Problema: Alarmas no se generan

**Solución**:
1. Verificar umbrales en "Check Fatigue Level"
2. Revisar configuración de nodos de alarma
3. Verificar que `propagate: true` esté configurado
4. Revisar relaciones del asset

## Referencias

- **Documentación**: `/dev/roadmaps/coiled-tubing/analytics/FATIGUE_CALCULATION.md`
- **API**: `/dev/roadmaps/coiled-tubing/api/API_DOCUMENTATION.md`
- **ThingsBoard Rule Engine**: https://thingsboard.io/docs/user-guide/rule-engine-2-0/overview/

---

**Versión**: 1.0.0  
**Última Actualización**: Enero 2026
