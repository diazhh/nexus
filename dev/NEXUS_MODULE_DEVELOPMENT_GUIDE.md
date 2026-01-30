# NEXUS Module Development Guide

Guía para desarrollar nuevos módulos en la plataforma NEXUS, evitando errores comunes de compilación.

## 1. Backend Java (Spring Boot)

### 1.1 Modelos y DTOs
- **Consistencia de tipos**: Si el modelo usa `String` para un campo (ej: `runNumber`), el DTO puede usar `Integer`, pero el servicio debe hacer la conversión explícita.
- **Nombres de propiedades**: Los DTOs deben coincidir exactamente con los builders de Lombok. Verificar que las propiedades existan antes de usarlas.
- **Convención**: Usar sufijos claros para unidades (`Ft`, `Ppg`, `Psi`, `Hours`, `Min`, `Percent`).

### 1.2 Servicios
- **Firmas de métodos**: Antes de llamar un método del servicio desde el controlador, verificar:
  - Nombre exacto del método (`getRunKpis` vs `calculateRunKpis`)
  - Parámetros requeridos (¿necesita `PageLink`?)
  - Tipo de retorno (`PageData<T>` vs `List<T>`)

- **PageLink**: Los métodos que consultan listas paginadas requieren `PageLink`:
  ```java
  PageLink pageLink = new PageLink(1000, 0);
  service.getByParent(parentId, pageLink);
  ```

### 1.3 Controladores
- **Alineación con servicios**: Los métodos del controlador deben llamar a métodos que existan en el servicio.
- **Propiedades de DTOs en summaries**: Al construir Maps de resumen, usar solo propiedades que existen en el DTO.

### 1.4 Rule Engine Nodes
- **Tipos de colecciones para telemetría**:
  ```java
  // Atributos
  List<AttributeKvEntry> entries = new ArrayList<>();
  entries.add(new BaseAttributeKvEntry(ts, new DoubleDataEntry(key, value)));

  // Telemetría
  List<TsKvEntry> entries = new ArrayList<>();
  entries.add(new BasicTsKvEntry(ts, new DoubleDataEntry(key, value)));
  ```

---

## 2. Frontend Angular

### 2.1 Nombres de Clases de Componentes
- **Convención de prefijo**: Usar `Dr` (PascalCase), NO `DR` (mayúsculas).
  ```typescript
  // ✓ Correcto
  export class DrBhasListComponent

  // ✗ Incorrecto
  export class DRBhasListComponent
  ```

### 2.2 Modelos TypeScript
- **Verificar propiedades**: Antes de usar una propiedad en el template, verificar que existe en el modelo.
- **Nombres exactos**: `wobKlbs` ≠ `wobKlb`, `c1MethanePpm` ≠ `c1Ppm`

### 2.3 Servicios HTTP
- **Verificar firmas**: Los servicios pueden tener métodos con diferentes parámetros:
  ```typescript
  // Dos métodos diferentes
  getMudLogsByRun(runId: string, pageLink: PageLink): Observable<PageData<DRMudLog>>
  getMudLogsByDepthRange(runId: string, startDepth: number, endDepth: number): Observable<DRMudLog[]>
  ```

### 2.4 Templates HTML
- **Propiedades del modelo**: Solo usar propiedades que existen en el modelo TypeScript.
- **Conditional rendering**: Usar `*ngIf` para propiedades opcionales antes de acceder a sub-propiedades.

### 2.5 Imports en Módulos
- **Declaraciones**: Todo componente usado debe estar en `declarations` y `exports` del módulo.
- **Rutas**: Verificar que el componente importado en routing coincide con el nombre de la clase.

---

## 3. Archivos de Configuración

### 3.1 Headers de Licencia
Todo archivo nuevo debe incluir el header Apache 2.0:

**TypeScript/Java:**
```
/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */
```

**HTML:**
```html
<!--
    Copyright © 2016-2026 The Thingsboard Authors

    Licensed under the Apache License, Version 2.0 (the "License");
    ...
-->
```

**SCSS:**
```scss
/**
 * Copyright © 2016-2026 The Thingsboard Authors
 * ...
 */
```

### 3.2 Menu System
- Agregar `MenuId` en el enum de `menu.models.ts`
- Agregar entrada en `menuSectionMap`
- Agregar a los menús de `SYS_ADMIN` y/o `TENANT_ADMIN`

### 3.3 Traducciones
- Agregar todas las claves en `locale.constant-en_US.json`

---

## 4. Checklist Pre-Compilación

### Backend
- [ ] DTOs tienen todas las propiedades usadas por el servicio
- [ ] Servicios tienen todos los métodos llamados por controladores
- [ ] Tipos de parámetros coinciden (UUID vs Entity, PageLink requerido)
- [ ] Imports de `TsKvEntry`, `AttributeKvEntry` correctos en Rule Nodes

### Frontend
- [ ] Nombres de clases usan prefijo correcto (Dr, no DR)
- [ ] Propiedades en templates existen en modelos
- [ ] Servicios HTTP tienen métodos con firmas correctas
- [ ] Componentes declarados en módulo y rutas

### General
- [ ] Headers de licencia en todos los archivos nuevos
- [ ] Traducciones agregadas
- [ ] Entradas de menú configuradas

---

## 5. Comandos de Verificación

```bash
# Compilar solo frontend
cd ui-ngx && npm run build

# Compilar módulo específico
mvn compile -pl common/dr-module -am

# Compilar todo sin tests
mvn clean install -DskipTests

# Verificar licencias
mvn license:check
```

---

## 6. Errores Comunes y Soluciones

| Error | Causa | Solución |
|-------|-------|----------|
| `cannot find symbol: method X()` | Método no existe en servicio | Verificar nombre exacto del método |
| `incompatible types: List<A> cannot be converted to List<B>` | Tipo de colección incorrecto | Usar tipo correcto (TsKvEntry, AttributeKvEntry) |
| `Property 'X' does not exist on type 'Y'` | Propiedad no existe en modelo | Verificar modelo TypeScript |
| `NG8001: 'X' is not a known element` | Componente no declarado | Agregar a declarations del módulo |
| `Missing header` | Sin licencia Apache | Agregar header de licencia |
