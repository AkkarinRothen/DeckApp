# Planificación: Mejoras en Reconocimiento de Tablas y QOL

Este documento detalla la hoja de ruta para optimizar el flujo de importación de tablas, enfocándose en la precisión del OCR y en la experiencia de usuario (QOL).

## 1. Mejoras Estructurales (Reconocimiento)

### A. Corrección de Perspectiva (De-skew)
- **Objetivo:** Permitir importar fotos de libros físicos con curvatura o ángulo.
- **Implementación:**
    - Reemplazar el `Rect` de recorte actual por un polígono de **4 puntos independientes**.
    - Utilizar `Matrix.setPolyToPoly` para transformar el área seleccionada en un bitmap rectangular "plano" antes de procesar el OCR.
    - **QOL:** Añadir auto-detección de bordes sugerida (usando contornos de OpenCV o similar si está disponible, o simplemente heurísticas de líneas largas).

### B. Soporte para Stitching (Tablas Multi-página)
- **Objetivo:** Unir fragmentos de una misma tabla repartidos en varias páginas de un PDF.
- **Flujo:**
    - Al terminar el Paso 5 (Mapping), añadir un botón "Continuar en otra página".
    - El ViewModel debe mantener un `DraftTable` acumulativo.
    - Las nuevas filas detectadas se anexan al final del borrador actual.

### C. Auditoría y Sanación de Rangos (Range Healing)
- **Objetivo:** Garantizar que las tablas sean válidas (sin huecos ni solapamientos) automáticamente.
- **Heurística:**
    - Si se detecta un hueco (ej: Fila A termina en 10, Fila B empieza en 12), ofrecer un botón de **"Auto-reparar"**.
    - La reparación ajustará todos los `minRoll` y `maxRoll` posteriores para mantener la secuencia lógica.

---

## 2. Calidad de Vida (UX/QOL)

### A. Estabilidad de Edición
- **Range Healing & Validation Jump:** Botón para reparar la secuencia y navegación rápida al siguiente error de validación.
- **Sequence Booster:** Botón "+" que inserta una fila pre-poblando el siguiente rango lógico.
- **Drag-to-Reorder:** Reordenamiento táctil con re-cálculo automático de rangos.

### B. Visualización e Inspección
- **Lupa de Contexto (Contextual Magnifier):** Mini-recorte de la imagen original al enfocar cada fila.
- **Interactive Snippet:** Posibilidad de ver el "entorno" de la fila original en un panel lateral.

### C. Automatización de Tareas Tediosas
- **Noise Stripper:** Limpieza masiva de basura OCR (puntos suspensivos, números de página, símbolos).
- **Auto-Tagging por IA:** Gemini sugiere etiquetas y categoría basándose en el contenido.
- **Inherit Metadata:** Herencia automática del nombre del archivo PDF como etiqueta de origen.
- **Clipboard Watcher:** Detección automática de tablas al abrir la pantalla de importación.

### D. Verificación y Continuidad
- **Test Drive (Simulación):** Realizar tiradas de prueba en vivo antes de guardar para validar la distribución.
- **Sub-table Placeholder:** Crear marcadores para tablas referenciadas que aún no existen.

---

## 3. Generación Proactiva (AI Creator)
- **Objetivo:** Permitir crear tablas desde cero mediante lenguaje natural.
- **Flujo:** "Crea una tabla de 1d20 encuentros en un pantano sombrío" -> Gemini genera el esquema -> Revisión -> Guardar.

---

## 4. Hoja de Roadmap Propuesto

### A. Enlaces y Tiradas Anidadas (Nested Rolls)
- **Objetivo:** Que una tabla pueda invocar a otra o disparar acciones.
- **Implementación:**
    - Definir sintaxis de enlace: `[[@Tabla:Nombre]]` o `[[@Mazo:Nombre]]`.
    - **Reconocimiento:** Durante la importación, detectar estos patrones y sugerir el `subTableId`.
    - **Ejecución:** En la vista de resultado de tirada, mostrar botones interactivos para "Tirar en tabla vinculada" o "Robar carta".

### B. Gestión de Paquetes (Bundles) y Origen
- **Objetivo:** Organizar tablas por libro o aventura automáticamente.
- **Flujo:**
    - Capturar el metadato `sourceFileName` durante la importación.
    - Opción de "Crear Paquete" al importar un lote (ej: "Dungeon Master Guide - Tablas").
    - Permitir filtrar la biblioteca global por paquete.

### B. Interacción en Sesión y Narrativa
- **AI Flavoring:** Integrar con Gemini para narrar resultados de tablas basados en el contexto de la sesión.
- **Modos de Tirada Avanzados:** Añadir modo "Sin Repetición" (Deck Mode) para tablas que se agotan.
- **Quick-Scratch Tables:** Widget lateral para crear tablas efímeras de 1d4/1d6 al vuelo durante el juego.

### C. Conectividad y Mapas
- **Compartición por QR/Link:** Sistema de exportación e importación rápida entre dispositivos mediante códigos QR.
- **Vínculos Geográficos:** Capacidad de asociar tablas a localizaciones en el mapa de Hexploración para disparar encuentros automáticos.

---

## 4. Biblioteca Global y Discovery
- **Objetivo:** Una base de datos de tablas accesible fuera de las sesiones.
- **Vista:** `TableLibraryScreen` con diseño tipo Bento/Grid.
- **Búsqueda:** Indexar el contenido de las entradas para permitir búsqueda de texto completo (ej: buscar "Veneno" y encontrar todas las tablas que lo incluyen).

---

## 5. Hoja de Roadmap Propuesto

1.  **Fase 1 (Estabilidad):** Implementar el "Smart Range Healing" y la "Fusión de Filas".
2.  **Fase 2 (Visual):** Implementar la "Lupa de Contexto" y el "Meta-data recognition".
3.  **Fase 3 (Geometría):** Implementar la "Corrección de Perspectiva" (4 puntos).
4.  **Fase 4 (Avanzado):** Soporte multi-página (Stitching).
5.  **Fase 5 (Lotes):** Reconocimiento de tablas paralelas y edición masiva.
6.  **Fase 6 (Integración):** Enlaces `@Tabla` / `@Mazo` y automatización.
7.  **Fase 7 (Gestión):** Biblioteca Global y Table Bundles.
8.  **Fase 8 (Creación):** Generación de tablas desde cero con IA.
9.  **Fase 9 (Narrativa):** AI Flavoring y modos de tirada avanzada.
10. **Fase 10 (Social):** Compartición por QR y Deep Links.
11. **Fase 11 (Mundo):** Integración con Mapas y Tablas de Servilleta.
