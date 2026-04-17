# Plan de Importación de Tablas desde PDF e Imágenes

Este documento detalla la planificación para la nueva funcionalidad de importación de tablas aleatorias a partir de fuentes visuales (PDF, JPG, PNG).

## Objetivo
Permitir a los usuarios convertir tablas impresas o en PDF (comunes en manuales de TTRPG) en objetos `RandomTable` dentro de la aplicación, minimizando la carga manual de datos.

---

## Flujo de Usuario Propuesto

1. **Selección de Fuente**: 
   - El usuario elige "Importar Tabla".
   - Selecciona un archivo PDF o una imagen de su galería.
2. **Selección de Área (Crop)**:
   - Se muestra la página del PDF o la imagen.
   - El usuario dibuja un rectángulo sobre el área que contiene la tabla (esto ayuda al OCR a no confundirse con texto circundante).
3. **Procesamiento (OCR)**:
   - La aplicación procesa el área seleccionada usando **Google ML Kit Text Recognition**.
   - Se extraen los "bloques" de texto y sus coordenadas.
4. **Mapeo de Columnas**:
   - La app intenta detectar automáticamente:
     - **Columna de Rango**: (e.g., "1", "2-4", "5-10").
     - **Columna de Contenido**: El texto del encuentro o resultado.
   - El usuario puede corregir qué bloque de texto corresponde a qué columna.
5. **Revisión y Edición**:
   - Se muestra una tabla editable con los datos extraídos.
   - El usuario puede corregir errores de lectura del OCR.
6. **Guardado**:
   - Se crea la `RandomTable` en la base de datos Room.

---

## Componentes Técnicos

### 1. Dependencias Nuevas
- `com.google.mlkit:text-recognition:16.0.1` (o versión actual).

### 2. Módulos Involucrados
- `:core:domain`: Nuevos casos de uso como `AnalyzeTableImageUseCase`.
- `:core:data`: Implementación del OCR en `FileRepository` o un nuevo `OcrRepository`.
- `:feature:import`: Extensión de la lógica de importación actual.
- `:feature:tables`: Nuevo `TableImportScreen`.

## Formatos de Referencia (Biblioteca del Usuario)

Tras analizar la biblioteca proporcionada (`Biblioteca\Tablas y Referencias`), se identifican los siguientes patrones comunes que la herramienta debe manejar:

1. **Tabla de 2 Columnas (Estándar)**:
   - `[Rango | Descripción]`. 
   - Es el formato más común (e.g., *The Book of Random Tables*).
   - El OCR debe distinguir entre el número a la izquierda y el texto a la derecha.

2. **Tablas de 3+ Columnas (Complejas)**:
   - `[d20 | Nombre | Cantidad | Comportamiento]`.
   - Común en tablas de encuentros (e.g., *The Mother of All Encounter Tables*).
   - El usuario debe poder elegir qué columna se convierte en el "Texto" de la entrada.

3. **Listas Enumeradas (Simples)**:
   - `1. Item A`, `2. Item B`... (e.g., *100 Books to Find...*).
   - Sin bordes de tabla, pero con un patrón de índice implícito.

4. **Entradas Multilínea**:
   - Una descripción larga que ocupa varias filas visuales bajo el mismo número de rango.
   - Requiere una heurística de "agrupación por proximidad" (si una fila no tiene número de rango, probablemente pertenece a la anterior).

---

## Lógica de Parsing (Heurística Avanzada)

Para convertir los bloques detectados por ML Kit en una estructura de datos:

1. **Alineación Vertical**: 
   - Agrupar bloques de texto que comparten un `y_center` similar (con un margen de error del 5-10% de la altura del bloque).
2. **Detección de Columnas**:
   - Analizar los espacios vacíos horizontales para determinar los límites de las columnas.
   - Si se detecta un patrón numérico (`\d+`, `\d+-\d+`) consistentemente en la izquierda, se marca como columna de `RollRange`.
3. **Mapeo de Datos**:
   - `minRoll` / `maxRoll` -> Extraídos del texto del primer bloque de cada fila.
   - `text` -> Todo el texto restante de la fila, concatenado (si hay múltiples columnas de contenido).
4. **Validación de Fórmula**:
   - La app calculará automáticamente la fórmula sugerida (e.g., si el máximo es 100, sugerir `1d100` o `%`).

---

## Desafíos Técnicos
- **Tablas Multilínea**: Entradas de tabla que ocupan varias líneas físicas pero son un solo registro.
- **Calidad de Imagen**: PDFs escaneados o fotos de libros pueden tener distorsión.
- **Formato de Dados**: Detectar si la tabla usa 1d6, 1d20, etc., basándose en los rangos detectados.

---

## Otros Métodos de Importación Comunes

Además del OCR, la aplicación soportará los siguientes formatos para facilitar la migración desde otras herramientas:

1. **Importación CSV / DSV**:
   - Soporte para coma (`,`), punto y coma (`;`) y el estándar de la industria TTRPG: **Pipe (`|`)**.
   - Auto-detección de delimitadores basada en la frecuencia de caracteres.
   - Soporte para cabeceras (ignorarlas o usarlas para configurar columnas).

2. **Importación JSON (Estándar VTT)**:
   - Compatibilidad con el formato de **Foundry VTT** (`RollTable`).
   - Mapeo de campos: `results` -> `entries`, `weight` -> `weight`, `range` -> `minRoll/maxRoll`.
   - Soporte para tablas anidadas si el JSON contiene referencias.

3. **Texto Plano (Paste / Markdown)**:
   - Modo "Lista Simple": Cada línea es una entrada. La app asigna los rangos automáticamente (1, 2, 3...).
   - Modo "Markup": Detectar patrones como `1. Item` o `- [1-10] Item`.

---

## Organización y Gestión de la Biblioteca

Para que el usuario pueda manejar cientos de tablas importadas, implementaremos:

1. **Sistema de Etiquetas (Tags)**:
   - Reusar la entidad `TagEntity` existente.
   - Crear `RandomTableTagCrossRef` para permitir múltiples etiquetas por tabla.
   - Filtro por tags en la lista de tablas.

2. **Marcado de Favoritos (Pinning)**:
   - Campo `isPinned` en `RandomTableEntity`.
   - Las tablas fijadas aparecerán siempre al principio de la lista, con un distintivo visual (ícono de chincheta).

3. **Gestión de Origen (Sources)**:
   - Agrupar tablas por archivo de origen (e.g., "Todas las tablas del PDF 'Manual del Jugador'").

---

## Integración con el Ecosistema de Mazo

Para que las tablas no sean una isla, las vincularemos con la funcionalidad principal de la app:

1. **Vínculo Tabla → Carta**:
   - Una entrada de tabla puede tener una acción de "Robar Carta de [Mazo]".
   - Útil para tablas de tesoro que te llevan a cartas de ítem físicas en la app.

2. **IA de Limpieza (Opcional)**:
   - Procesar el texto del OCR para corregir saltos de línea mal puestos o errores ortográficos comunes en escaneos de libros antiguos.
   - Generación de tablas mediante prompts (e.g., "Genera una tabla de 10 rumores de taberna").

---

## Experiencia "Premium" de Tirada

1. **Feedback Háptico y Sonoro**: Diferentes vibraciones y sonidos según el tipo de dado (d20 pesado vs d4 ligero).
2. **Animación de "Tumbling"**: Los resultados no aparecen de golpe, sino que muestran valores aleatorios rápidos antes de detenerse (efecto tragamonedas/dado rodando).
3. **Historial Exportable**: Poder compartir el log de tiradas de una sesión como un reporte de aventura.
4. **Modo Pantalla Completa**: Para usar la tablet como un "Oracle" dedicado en el centro de la mesa.
