# Master Plan: Table Recognition & Management (Data Studio)

Este documento detalla la hoja de ruta extendida para convertir Deckapp en la herramienta definitiva de gestión de datos para TTRPG.

## Fase A: Estabilidad y Precisión (Completado/En Proceso)
1.  **Image Preprocessor:** Grayscale, Contrast, Sharpening. [Hecho]
2.  **OCR UI:** Zoom, Pan, Overlay de confianza. [Hecho]
3.  **Normalización:** Corrección de caracteres 1/I/l, 0/O. [Hecho]
4.  **Exportación:** CSV y Markdown. [Hecho]

## Fase B: Organización y UX Industrial (Sprints 23-26)
5.  **Navegación Pro:** Header de progreso, back-stack inteligente y navegación unificada.
6.  **Biblioteca 2.0:** Vista de cuadrícula, Sticky Headers y organización por `TableBundle`.
7.  **Operaciones en Lote:** Multi-selección para etiquetas, borrado y exportación masiva.
8.  **Toolbox Contextual:** Menú de herramientas para duplicar, invertir y re-ajustar OCR.

## Fase C: El "Data Studio" (Sprints 27-30)
9.  **Editor de Celdas Avanzado:** Concatenar columnas, Split inteligente y búsqueda Regex.
10. **Motor de Sugerencias:** Detección de huecos en rangos de dados y auto-corrección mediante glosario local.
11. **Links Dinámicos:** Detección de referencias a otras tablas dentro del texto.

## Fase D: Integración y Ecosistema (Fase Final)
12. **Contextual Widgets:** Tablas flotantes sobre la vista de combate.
13. **Multi-Roll & Grouping:** Tiradas múltiples con sumatorios o listas agrupadas.
14. **Deckapp Protocol:** Importación directa desde URLs y formato de intercambio .dat.

---

## Próximos Pasos Inmediatos:
1. Implementar la entidad `TableBundle` en la base de datos.
2. Refactorizar la Biblioteca para soportar la vista de cuadrícula.
3. Añadir el botón "Invertir Rangos" como primera herramienta del Toolbox.
