# Ideas de Mejoras y Nuevas Funciones

Este documento sirve para registrar ideas de funcionalidades que podrían integrarse en el futuro para mejorar la experiencia de uso en TTRPGs.

## 1. Mejoras en la Importación
- [ ] **Rango de páginas (Full Range):** Selección de `inicio-fin` (ej: "10-50") para ignorar contenido no deseado en cualquier parte del PDF.
- [ ] **Dorso Único (Generic Back):** Opción para asignar una imagen fija o de una galería de texturas como dorso global.
- [ ] **Plantillas de Importación:** Guardar configuraciones de grilla/márgenes para reutilizarlas en PDFs del mismo autor.
- [ ] **Importación Masiva (Batch):** Procesamiento de múltiples archivos PDF/Carpetas en cola usando plantillas.
- [ ] **Auto-Crop Inteligente:** Uso de OpenCV para detectar bordes de cartas automáticamente en PDFs irregulares.
- [ ] **Corrección de Rotación:** Botones de giro 90° en la vista previa para corregir la orientación antes de recortar.
- [ ] **OCR de Títulos:** Detección automática de títulos durante el recorte para evitar nombres genéricos.
- [ ] **Auto-Tagging:** Aplicar etiquetas automáticas basadas en nombres de carpetas o metadatos del PDF.

### Estructura y Estabilidad (Área 1)
- [ ] **Importación Atómica:** Sistema de confirmación final para evitar mazos incompletos tras un cierre inesperado.
- [ ] **WorkManager Integration:** Procesamiento en segundo plano para importaciones grandes.
- [ ] **Gestión de Memoria Pro:** Pool de bitmaps y reciclaje agresivo para evitar cierres por falta de RAM (OOM).
- [ ] **Validación de Espacio:** Comprobación de almacenamiento disponible antes de iniciar la copia de archivos.
- [ ] **Garbage Collector de Archivos:** Limpieza automática de imágenes "huérfanas" no vinculadas a la base de datos.
- [ ] **Soporte DPI Adaptativo:** Ajuste inteligente de la resolución de renderizado según el tamaño de la carta.
- [ ] **Deduplicación por Hash (SHA-256):** Evitar archivos duplicados en disco si varias cartas comparten la misma imagen.
- [ ] **Indexación Room FTS:** Búsqueda de texto completo optimizada para bibliotecas de miles de cartas.
- [ ] **Sistema de Miniaturas (.webp):** Generación automática de versiones ligeras para scroll fluido en la biblioteca.
- [ ] **Tests de Regresión de PDF:** Suite de pruebas automáticas para asegurar que nuevos cambios no rompan modos de importación previos.
- [ ] **Verificador de Integridad SAF:** Chequeo al inicio para detectar y reparar URIs de carpetas externas inaccesibles.

## 12. Estabilidad de Datos y Sesiones (Core)
- [ ] **Snapshots de Sesión:** Fotos de estado cada X eventos para carga instantánea de sesiones largas.
- [ ] **Registro Maestro de Assets:** Tabla centralizada para control de integridad de archivos físicos.
- [ ] **Herramienta de Autodiagnóstico:** Sistema de "Auto-reparación" de la base de datos y archivos.
- [ ] **Viewport Virtualization:** Optimización de renderizado para mesas de juego con cientos de elementos.
- [ ] **Shadow Saving (Notas de DM):** Escritura segura con archivos temporales para evitar pérdida de texto.
- [ ] **Monitor de Salud de RAM:** Alerta proactiva al usuario si la biblioteca está agotando los recursos del sistema.

## 2. Organización y Biblioteca
- [ ] **Etiquetado por mazo (Tags):** Categorizar mazos (Bestiario, Hechizos, Encuentros) para filtrado rápido.
- [ ] **Buscador Global:** Buscar una carta por nombre entre todos los mazos de la biblioteca.

## 3. Funciones de Juego (Session Mode)
- [ ] **Gestión de Descarte:** Pila de descarte visual para ver qué cartas salieron y poder re-mezclarlas.
- [ ] **Marcadores sobre la carta:** Tokens temporales (vida, estados, "agotado").
- [ ] **Mano del Jugador:** Zona de cartas privadas antes de bajarlas a la mesa virtual.
- [ ] **Timer de Sesión:** Mostrar el tiempo transcurrido de la partida.

## 4. Edición y Personalización
- [ ] **Editor Markdown:** Soporte para negritas, listas y tablas en las descripciones de las cartas.
- [ ] **Notas por carta:** Campo de notas privadas del DM vinculadas a una carta específica.

## 5. Exportación
- [ ] **Exportar a PDF (Re-Layout):** Crear un PDF optimizado para impresión A4 seleccionando cartas de distintos mazos.
- [ ] **Backup Completo:** Exportar toda la biblioteca en un ZIP (imágenes + base de datos).

## 6. Funciones Inteligentes (IA y Automatización)
- [ ] **Dados embebidos:** Detección de patrones tipo `[2d10+5]` en el texto y tirada automática al tocar.
- [ ] **Generador de descripciones (LLM):** Creación de lore o descripciones para cartas que solo tienen imagen/nombre.
- [ ] **Traducción en tiempo real:** Uso de OCR + Traducción para superponer texto en español sobre imágenes en inglés.

## 7. Mecánicas Especializadas
- [ ] **Modo "Tarot / Oráculo":** Soporte para cartas invertidas con significados alternativos.
- [ ] **Pilas de Botín (Loot Tables):** Sistema de pesos/probabilidades para que algunas cartas sean más raras que otras.
- [ ] **Relojes de Progreso:** Componentes interactivos sobre cartas para trackear progreso de peligros o tareas.

## 8. Integración Digital y VTT
- [ ] **Webhook de Discord:** Envío automático de cartas y resultados de tablas a canales de Discord.
- [ ] **Exportación a VTT:** Generación de formatos compatibles con Foundry VTT o Roll20.
- [ ] **Servidor Web Local:** Vista de la "Mesa" vía navegador para jugadores en la misma red.

## 9. Exploración y Worldbuilding
- [ ] **Modo Mapa (Hexcrawl):** Grilla para colocar cartas boca abajo y representar exploración de territorio.
- [ ] **Generador de Semillas:** Combinación automática de cartas de Lugar/NPC/Conflicto.

## 10. Gestión de Combate
- [ ] **Mazo de Iniciativa:** Sistema de turnos basado en cartas con soporte para efectos especiales.
- [ ] **Vínculo de Estados:** Arrastrar condiciones sobre cartas de personajes/criaturas.

## 11. Accesibilidad y Estética
- [ ] **Comandos de Voz:** Acciones básicas (robar, barajar) mediante voz para DMs con manos ocupadas.
- [ ] **Temas de Mesa:** Texturas personalizables para el fondo del Workspace (madera, piedra, etc.).

---
*Documento actualizado el 16 de abril de 2026*

## Propuesta de Hoja de Ruta: Organización y Biblioteca (Q2 2026)

Basado en la arquitectura actual, se propone la siguiente secuencia de implementación:

### Fase 1: Motor de Búsqueda de Alto Rendimiento (FTS)
- [ ] **Migración Room FTS4:** Crear tablas virtuales paralelas para indexar nombres de cartas, contenido de caras y entradas de tablas.
- [ ] **Búsqueda Global Dinámica:** Integrar resultados en la barra de búsqueda actual, mostrando coincidencias de cartas y tablas en tiempo real con contexto de ubicación.
- [ ] **Resaltado de Términos:** Mostrar fragmentos de texto donde se encontró la coincidencia.

### Fase 2: El "Baúl" Unificado (Colecciones M:N)
- [ ] **Entidad 'Collection':** Sistema flexible donde un recurso puede pertenecer a múltiples grupos (Campaña, Tipo, etc.).
- [ ] **Iconografía y Color:** Personalización de cada colección con iconos (Cofre, Mapa, Libro) y paletas de colores.
- [ ] **Navegación por Pestañas:** Rediseñar la pantalla principal para alternar entre "Mazos", "Tablas" y "Colecciones".
- [ ] **Acciones Masivas Mejoradas:** Mover varios ítems entre colecciones con drag-and-drop.

### Fase 3: Optimización Visual
- [ ] **Miniaturas WebP:** Pipeline automático para generar vistas previas de baja resolución durante la importación.
- [ ] **Filtros Avanzados:** Combinación lógica de etiquetas (AND/OR) y filtros por "Último uso".

### Fase 4: Integración y Estabilidad
- [ ] **OCR de Títulos:** Integrar la detección automática de nombres durante el recorte para poblar el índice de búsqueda desde el inicio.
- [ ] **Backup por Colección:** Permitir exportar una colección completa (Campaña) como un solo archivo.
