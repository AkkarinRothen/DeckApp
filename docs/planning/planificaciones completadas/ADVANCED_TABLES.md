# Plan: Sistema de Tablas Aleatorias (Fase Avanzada)

Este plan detalla las mejoras estructurales y funcionales para llevar el sistema de tablas aleatorias a un nivel profesional.

## Estado de Implementación
- [ ] Biblioteca Global de Tablas (`TableLibraryScreen`)
- [ ] Vinculación Tabla → Mazo (Draw action)
- [ ] Mapeo Manual de OCR (Interactive UI)
- [ ] Importación por Lotes (Multi-table detection)
- [ ] Soporte para CSV con delimitadores dinámicos

## Mejoras de Reconocimiento (OCR & Heuristics)

> [!IMPORTANT]
> El objetivo es reducir el tiempo de edición manual post-importación en un 80%.

### 1. Pre-procesamiento de Imagen
- **Recorte Libre y Adaptable:** Implementación de manejadores táctiles en esquinas y bordes.
- **Corrección de Perspectiva (4 Puntos):** Implementación de `setPolyToPoly` (Homografía) con **Auto-detección sugerida**: el sistema pre-escanea la página y posiciona las 4 esquinas rodeando la tabla detectada automáticamente.
- **Multipage Table Support (Stitching):** Flujo de importación incremental para unir fragmentos de tablas.
- **Flujo de Importación Continua:** Mantener el documento abierto tras guardar para permitir la selección de una segunda o tercera tabla diferente en el mismo archivo.

### 2. Heurísticas de Estructura
- **Multiline Merging:** Si un bloque no empieza con un rango numérico/dado, se une inteligentemente a la fila superior si la proximidad Y es menor al 150% del interlineado.
- **Column Detection (X-Axis):** Agrupar bloques por coordenadas X para auto-detectar columnas (ej. Rango | Resultado | Descripción).
- **Sequence Audit:** Validación automática de rangos (detectar huecos o solapamientos en los números).

### 3. Parsing de Contenido
- **Reference Detection:** Reconocer patrones `@Mazo` o `@Tabla` en los resultados para generar acciones automáticas.
- **Dice Expression Parser:** Detectar si una columna contiene fórmulas (1d6 + 2) para sugerir el `rollFormula` global.

## Cambios Propuestos

### Componente: Core & Domain
- **[MODIFY]** `AnalyzeTableImageUseCase.kt`: Implementar lógica de agrupación por columnas y limpieza multilínea.
- **[MODIFY]** `RandomTable.kt`: Añadir metas para `suggestedFormula` y enlaces a recursos.

### Componente: Feature Tables
- **[NEW]** `TableLibraryScreen.kt`: Gestión global fuera de la sesión.
- **[MODIFY]** `TableImportScreen.kt`: Añadir paso de "Ajuste de Perspectiva" y "Definición de Columnas".
- **[MODIFY]** `TableImportViewModel.kt`: Soporte para estados de mapeo manual post-OCR.

## Verificación
- Validar con fotos de manuales de D&D y Pathfinder (tablas complejas de 3+ columnas).
- Test de estrés con tablas de 100+ filas.
- Comprobar que el "merging" no rompe tablas de una sola columna.
