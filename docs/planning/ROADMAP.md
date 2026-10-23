# DeckApp TTRPG — Roadmap de Mejoras

> **Documento vivo.** Registrar aquí ideas, mejoras pendientes y decisiones de dirección antes de
> llevarlas a código. Ver `DEVLOG.md` para el historial de lo ya implementado.
>
> **Escala de impacto:** 🔴 Alto — 🟡 Medio — 🟢 Bajo  
> **Escala de esfuerzo:** ★★★ Grande (>1 sprint) — ★★ Medio (~1 sprint) — ★ Pequeño (<1 día)

---

## Estado actual del proyecto (22 de octubre de 2026)

### ✅ Implementado y funcional (Actualizado Octubre 2026)
- **Playmat 2D Híbrido:** Superficies múltiples, colocación/rotación/volteo de cartas, tokens con HP y anillos de color, relojes de progreso, hilos narrativos, cuadrículas tácticas y battlemaps.
- **Mesa de sesión adaptable:** El layout se organiza por ancho disponible para ventanas compactas, medianas y expandidas; paneles y navegación se ajustan sin cambiar el estado del tablero.
- **Widget de sesión:** Glance permite vincular cada instancia a una sesión activa y abrirla directamente. Si no hay sesión activa, el widget puede quedar instalado y enlaza con la lista de sesiones.
- **Bitácora de Sesión en Vivo (`SessionLogEntry`) & Notas Rápidas (`SessionNote`):** Room v49 con persistencia reactiva, captura automática de eventos de dados, cartas, tokens, oráculos y relojes; drawer lateral con editor Markdown y atajos de un toque.
- **Ergonomía de Mesa:** Modo Zen inmersivo (1-tap toggle) y Toolbar superior categórica (Mundo, Entidades, Mesa).
- Import desde carpeta SAF (imágenes → cartas, auto-detect de metadata de filename), ZIP (import/export), y PDF import con **PdfiumAndroid** (4 modos de layout, grilla configurable, auto-trim).
- 8 modos de contenido de carta completos (renderizado + edición), CardEditorScreen multi-cara.
- SessionScreen multi-tab con dados 3D, oráculos Mythic y generador Choir.
- Room con migraciones blindadas sin pérdida destructiva (v1 → v49).
- Búsqueda global de sesión y biblioteca, catálogo de tablas bundled e importación OCR/CSV/JSON.
- **Biblioteca de NPCs:** búsqueda por nombre/descripción y filtros por NPC/monstruo y etiquetas, con estado sin coincidencias y limpieza de filtros.
- **Actualizador Android:** consulta opcional de GitHub Releases, descarga con progreso y apertura del instalador del sistema; la publicación aún requiere un release APK con firma estable.

### 🎯 Plan Priorizado de UX y Mesa (Auditoría 17/10/2026 - En Ejecución)
- **Fase 1 (P0):** Hacer usable la mesa en todos los tamaños (Layout adaptable vertical/horizontal, franja Slim adaptativa de contexto ~36dp sin solapar contenido, y selector de herramientas unificado por categorías).
- **Fase 2 (P0):** Cerrar rutas incompletas y estados vacíos (Combate sin encuentro con acciones rápidas inline, Hexploración sin mapa con vínculo directo, limpieza de referencias 'null' en historial).
- **Fase 3 (P1):** Unificar consulta y registro (Ficha de PNJ Bento con combate interactivo arriba y lore renderizado abajo, visores consistentes).
- **Fase 4 (P1):** Preparación y protección de datos (Export ZIP de mazo desde detalle, rango de páginas en importador PDF, validación de backup/restauración).
- **Fase 5 (P2):** Expansiones opcionales (VTT compartido local, exportación a Foundry, tiradas avanzadas).

---

## A — Deuda Técnica

| # | Ítem | Impacto | Esfuerzo | Estado |
|---|------|---------|----------|--------|
| A-1 | Migrar PDF rendering de `PdfRenderer` nativo a **PdfiumAndroid** | 🔴 | ★★ | ✅ Hecho (Sprint 11) |
| A-2 | Room: strategy de migración real (quitar `fallbackToDestructiveMigration`) | 🔴 | ★★ | ✅ Hecho (Sprint 11) — migraciones v1→v8 escritas |
| A-3 | Índices en tablas cross-ref de tags (`card_tags`, `card_stack_tags`) | 🟡 | ★ | ✅ Hecho (Sprint 11) |
| A-4 | Compresión de imagen configurable en import | 🟡 | ★ | ✅ Hecho (Sprint 11) — Settings: Alta/Media/Baja |
| A-5 | Manejo de errores visible en Import (fallo de archivo individual) | 🟡 | ★ | ✅ Hecho (Sprint 11) — `failedFiles` en `ImportUiState` |
| A-6 | Export ZIP de mazo desde `DeckDetailScreen` overflow | 🟡 | ★ | ✅ Hecho — exportación de ZIP/JSON desde el menú de detalle |
| A-7 | Progress bar de almacenamiento por mazo en `SettingsScreen` | 🟢 | ★ | ✅ Hecho — desglose de almacenamiento por mazo en Ajustes |

---

## B — Features Pendientes del Plan Original

### B-1 — Historial de sesión (SessionHistoryScreen) ✅ Hecho
La pantalla implementa una línea de tiempo de eventos, filtros y estadísticas; se registran acciones de juego y eventos adicionales de sesión.

**Comportamiento esperado:**
- Accesible desde el listado de sesiones → tap en sesión finalizada
- Timeline vertical con íconos por acción (DRAW / DISCARD / FLIP / RESET…)
- Cada ítem: hora, nombre de la carta, imagen thumbnail, acción realizada
- Filtros: por mazo, por tipo de acción
- Estadísticas al final: total de cartas robadas, descartadas, tiempo de sesión

---

### B-2 — Modo boca abajo al robar (Face Down Draw) ✅ Hecho
Robar una carta pero mostrarla tapada (se ve el dorso) hasta que el DM la toca para revelarla.
Muy común en mecánicas de sorpresa/misterio.

**Comportamiento esperado:**
- Toggle en la configuración del mazo: "Robar boca abajo / boca arriba"
- La carta en la mano muestra el dorso (imagen de dorso del mazo o imagen genérica)
- Tap en la carta en la mano → `FLIP` event → se revela el frente con animación
- `CardViewScreen` desde una carta boca abajo muestra directamente el dorso

✅ **Hecho (Sprint 26)** — Implementado con rotación 3D y Spring animation.

**Detalles técnicos:**
- `Card.isRevealed: Boolean` (nuevo campo, Room migration necesaria)
- O reusar `currentFaceIndex`: si el dorso es `faces[1]`, empezar con `currentFaceIndex=1`
- Imagen de dorso configurable por mazo: `CardStack.backImagePath: String?`

---

### B-3 — Imagen de dorso por mazo ✅ Hecho
Actualmente no hay forma de asignar una imagen de dorso global a un mazo.
Es esencial para el modo Face Down y para la presentación visual general.

**Comportamiento esperado:**
- En `DeckDetailScreen` (o pantalla de config del mazo): picker de imagen para el dorso
- El dorso se muestra en el `CardThumbnail` cuando `card.isDrawn = false` y se selecciona
- En `SessionScreen`, cartas boca abajo muestran este dorso

✅ **Hecho (Sprint 26)** — Integrado en la `DeckConfigSheet`.

**Detalles técnicos:**
- `CardStack.backImagePath: String?` ya existe en el modelo pero no está expuesto en la UI
- Conectar en `DeckDetailScreen` via un `IconButton` o en un panel de configuración del mazo

---

### B-4 — Archivar mazos ✅ Hecho (Sprint 13)
`CardStack.isArchived`, migración v7→v8, chip "Archivados" en Biblioteca, `getArchivedDecks()` en DAO/repo.

---

### B-5 — Spread layout Tarokka (posiciones nombradas) ✅ Hecho (octubre 2026)
Layout visual de una tirada de Tarokka (Cruz de 5 posiciones) en `SessionScreen`.

**Comportamiento esperado:**
- Modo "Tirada" accesible desde el overflow de sesión cuando el mazo activo es Tarokka
- Canvas con 5 posiciones nombradas: Pasado, Presente, Futuro, Obstáculo, Resultado
- Al robar, la carta se coloca en la siguiente posición libre
- Cada posición tiene etiqueta y puede mostrarse boca abajo (revelar individualmente)
- Vista de resumen de la tirada completa

**Hecho:** el bloque de mazo Tarokka muestra las cinco posiciones, permite robar una carta por vez, revelar individualmente y empezar otra tirada con confirmación. La asignación se conserva en la memoria de la sesión.

---

### B-6 — Clonar sesión (reusar configuración) ✅ Hecho
Crear una nueva sesión con los mismos mazos que una sesión anterior.
Muy útil para campañas con sesiones recurrentes.

**Comportamiento esperado:**
- Botón "Repetir sesión" en el historial de sesiones finalizadas
- Crea una nueva sesión con el mismo nombre + fecha + los mismos mazos
- Los mazos se resetean automáticamente al estado original

---

### B-7 — Auto-detección de grilla en PDF (OpenCV) 🟢 ★★★
Detectar automáticamente la grilla de corte en un PDF sin que el usuario tenga que
especificar el número de columnas y filas.

Librería: `zynkware/Document-Scanning-Android-SDK` (Kotlin, OpenCV lightweight)

**Aplica a Fase 2 tardía.** El flujo manual con preview funciona bien y cubre la mayoría de casos.

---

## C — Mejoras de UX

### C-1 — Reordenar recursos y cartas (drag & drop) 🔴 ★★
Actualmente las cartas se muestran en orden `sortOrder`.

**Implementación:** `LazyVerticalGrid` con `sh.calvin.reorderable`.

**Estado:**
- [x] **Biblioteca (Mazos y Tablas)**: Implementado reordenamiento persistente (Sprint 26).
- [x] **DeckDetail (Cartas individuales)**: Reordenamiento persistente con arrastre y controles de paso; modo dedicado para evitar movimientos accidentales.

---

### C-2 — Panel de configuración del mazo en DeckDetail ✅ Hecho
La `DeckConfigSheet` permite configurar el modo de robo, el contenido predeterminado, el dorso y otros ajustes del mazo.

**Comportamiento esperado:**
- Nuevo ítem "Configurar mazo" en el DropdownMenu de DeckDetailScreen
- BottomSheet o pantalla nueva con:
  - Nombre del mazo (editable)
  - Descripción (editable, Markdown)
  - `defaultContentMode`: selector de 8 opciones
  - `DrawMode`: selector RANDOM / TOP / BOTTOM
  - Imagen de dorso: picker de imagen (ver B-3)
  - "Mostrar contador de cartas" toggle

✅ **Hecho (Sprint 26)** — Implementada la `DeckConfigSheet` premium.

---

### C-3 — Filtrar cartas por palo en DeckDetail 🟡 ★
Para mazos como Tarokka donde hay palos (Oros, Copas, Espadas, Bastos) o los mazos de
Paul Weber donde hay categorías (Monsters, Items, Spells).

**Comportamiento esperado:**
- Chip row bajo el header con los palos del mazo (extraídos de `card.suit`)
- Tap en chip → filtra la cuadrícula al palo seleccionado
- Chip "Todos" siempre presente

✅ **Hecho** — DeckDetail genera los filtros desde los palos presentes y permite volver a “Todos”. El reordenamiento se desactiva mientras hay un filtro aplicado.

---

### C-4 — Snackbar con opción "Deshacer borrado" de mazo/carta 🟡 ★
El borrado requiere confirmación y ahora ofrece una ventana breve para recuperarlo.

**Comportamiento esperado:**
- Al borrar un mazo: mostrar Snackbar con botón "Deshacer" (5 segundos)
- Si el DM toca "Deshacer" antes del timeout: cancelar la eliminación y conservar el mazo, sus cartas e imágenes
- Si el timeout expira: confirmar la eliminación definitiva

**Implementación:** diferir la eliminación real al cierre del Snackbar, cancelar el Job si
el DM pulsa deshacer.

✅ **Hecho** — Carta o mazo queda pendiente durante cinco segundos; el borrado de archivos y datos solo se ejecuta al vencer el plazo. “Deshacer” cancela la operación.

---

### C-5 — Animación de barajar en SessionScreen 🟢 ★
Visual feedback cuando el DM baraja el mazo (shuffle).

**Comportamiento esperado:**
- Al llamar a "Barajar mazo": animación de 2-3 cartas del mazo superpuestas que se mezclan
- Opcional: sonido de barajar (AudioManager o SoundPool, silenciable)

✅ **Hecho** — Los controles grandes del bloque de mazo animan tres cartas durante el barajado, la recuperación del descarte y el reinicio. El sonido queda opcional y no se incorporó.

---

### C-6 — Compartir imagen de una carta 🟢 ★
Desde `CardViewScreen`, opción de compartir la imagen de la cara activa como JPEG.

**Comportamiento esperado:**
- Botón de Share en el TopAppBar de CardViewScreen
- Usa `FileProvider` + `Intent.ACTION_SEND` con `image/jpeg`
- Comparte la imagen sin metadata del DM

✅ **Hecho** — CardView comparte la imagen de la cara activa como JPEG rasterizado mediante el selector del sistema. La opción se desactiva con la carta oculta o si la cara no tiene imagen.

### C-7 — Búsqueda y filtros en biblioteca de NPCs ✅ Hecho (octubre 2026)
La biblioteca permite buscar por nombre o descripción y filtrar entre NPCs y monstruos, además de filtrar por etiquetas presentes. Cuando no hay coincidencias, ofrece limpiar los filtros.

### C-8 — Búsqueda global de cartas ✅ Hecho (octubre 2026)
El buscador global consulta cartas mediante el índice FTS existente, abre el resultado directamente y aplica debounce/cancelación para evitar consultas repetidas mientras se escribe. Los resultados están limitados para mantener fluida la vista.

### C-9 — Importación por lotes de PDFs ✅ Hecho (octubre 2026)
Permite seleccionar varios PDFs, revisar una previsualización del primero y aplicar el mismo layout a todos. Cada archivo crea su propio mazo; se procesa secuencialmente, limita el rango de páginas al tamaño de cada PDF y reporta fallos individuales.

---

## D — Features Nuevas (no estaban en el plan original)

### D-1 — Notas de DM ✅ Hecho
Notas por sesión, notas por carta, Quick Notes y diario de campaña están implementados.

---

### D-2 — Modo "Carta aleatoria" con filtro de palo ✅ Hecho (octubre 2026)
Robar una carta aleatoria de un palo específico sin cambiar el DrawMode global.

**Hecho:** acción "Robar por palo" en el bloque de cada mazo y selector con los palos que aún tienen cartas. El robo comparte la lógica normal de orientación, estado boca abajo e historial, y no altera el modo de robo del mazo.

---

### D-3 — Timer de sesión ✅ Hecho
Mostrar el tiempo transcurrido desde el inicio de la sesión activa.

**Comportamiento esperado:**
- En el TopAppBar de `SessionScreen`: texto sutil "2h 14m" junto al nombre
- Calculado como `System.currentTimeMillis() - session.createdAt`
- Se actualiza cada minuto (no en tiempo real)

---

### D-4 — Backup y restauración de la biblioteca ✅ Hecho
Crear una copia de seguridad completa de todos los mazos importados.

**Comportamiento esperado:**
- En Settings: "Crear backup" → exporta un ZIP grande con todas las imágenes + el schema de Room como JSON
- "Restaurar backup" → importa el ZIP y recrea toda la biblioteca
- Compatible entre instalaciones/dispositivos

**Dependencia:** requiere serialización del modelo de dominio a JSON (kotlinx.serialization ya en el proyecto).

---

### D-5 — Widget de Android para sesión activa ✅ Hecho (octubre 2026)
Glance widget configurable por instancia, con acceso a la sesión elegida y actualización del nombre/estado mientras la app está abierta.

**Contenido del widget:**
- Nombre de la sesión activa
- Tap para abrir la sesión enlazada y acción para cambiarla
- Estado vacío que permite abrir la lista de sesiones

**Hecho:** cada widget permite elegir una sesión activa. Si la sesión termina o se elimina, ofrece vincular otra; si se configura sin sesiones activas, puede abrir la lista de sesiones.

### D-6 — Layout adaptable de la mesa por ancho ✅ Hecho (octubre 2026)
La pantalla de sesión distingue ventanas compactas, medianas y expandidas por ancho disponible. Los layouts medianos usan una columna de herramientas más estrecha y los paneles compactos aprovechan mejor ventanas de poca altura.

---

## E — Decisiones de Producto Pendientes

| # | Pregunta | Contexto | Opciones |
|---|----------|----------|---------|
| E-1 | **¿Sincronización en la nube?** | Supabase ya conocido del proyecto TDAPP. Permitiría continuar en tableta + teléfono. | (a) No, storage local siempre. (b) Supabase opt-in en Fase 3. (c) Backup/restore manual (D-4) como sustituto. |
| E-2 | **¿Nombre definitivo de la app?** | "DeckApp" es genérico. El package es `com.deckapp.ttrpg`. | Opciones a considerar: *Arcana*, *Vault* (TTRPG Vault), *TableDeck*, *Grimoire*, *Spellbook*... |
| E-3 | **¿Target mínimo API 26 o subir a API 28?** | API 26 = Android 8.0 (2017). API 28 = Android 9.0 (2018). | Subir a 28 permite `NotificationChannel` simplificado y mejoras de seguridad en file access. |
| E-4 | **¿Soporte de orientación horizontal en SessionScreen?** | ✅ Resuelto con layout adaptable por ancho disponible, sin guardar una disposición distinta por orientación. |
| E-5 | **¿Procesamiento de PDF en foreground Worker o en coroutine?** | Actualmente el import se describe como WorkManager pero el codigo usa coroutines directas. | Resolver antes de mazos grandes (>100 páginas) donde el import puede tardar varios minutos. |

---

## Sprints Realizados

| Sprint | Contenido principal | Estado |
|--------|---------------------|--------|
| 1–14.5 | Core architecture, Tables, PDF, Multi-deck, Workspace Bento | ✅ |
| 15 | Notas de DM — Tab Notas real, Markdown toolbar, Quick Notes polish | ✅ |
| 16 | Notas por carta + Export ZIP enriquecido (manifiesto JSON) | ✅ |
| 1–3 | Setup multi-módulo, Room schema, LibraryScreen, DeckDetail, SessionScreen base | ✅ |
| 4 | CardAspectRatio, ContentScale.Fit, dots de caras, badge de conteo | ✅ |
| — | Refactor SessionScreen → HorizontalPager (Mazos/Tablas/Notas/Combate) | ✅ |
| 5 | Sistema de Tablas Aleatorias completo + 9 tablas bundled | ✅ |
| 6 | Import de tablas — OCR, CSV, JSON, PlainText; flujo de 5 pasos | ✅ |
| 7 | FAB TIRAR real, export tablas JSON, WEIGHTED mode UI | ✅ |
| 8 | Homografía, stitching multi-página, detección automática de bordes | ✅ |
| 9 | RangeParser robusto, CsvTableParser, JsonTableParser, PlainTextTableParser | ✅ |
| 10 | Tags para tablas, `isPinned`, TableLibraryScreen, gestión en caliente | ✅ |
| 11 | OCR avanzado: `splitMultiLineBlocks`, `expectedTableCount`, edición manual de rangos | ✅ |
| 12 | Duplicar mazo, fusionar mazos (UseCase + dialog UI) | ✅ |
| 13 | Archivar/restaurar mazos, Room v8, chip Archivados en Biblioteca | ✅ |
| 14 | Deuda técnica: PdfRenderer nativo, migraciones Room v13, índices tags | ✅ |
| 14.5 | Workspace Bento: DeckClusterItem, DeckWorkspace, CompactCardItem, FlowRow | ✅ |
| 26 | Configuración Centralizada (C-2), Boca Abajo 3D (B-2) e Importación MD | ✅ |

---

## Planes de sprint originales (histórico; revisar antes de reutilizar)

> La planificación que sigue se redactó en abril de 2026 y contiene funciones que desde entonces se completaron. El backlog vigente es el estado por sección de este documento y `docs/planning/IDEAS_MEJORAS.md`.

Sprint 17 — Encuentros y Combat Tracker (17 de abril de 2026) ✅
  · Modelo Encounter + EncounterCreature en :core:model
  · :feature:encounters: lista + editor + tracker de HP / Iniciativa
  · Integración de PJs temporales en iniciativa
  · Tab Combate en SessionScreen (dinámico)
  · Resumen automático de combate en Notas de DM

Sprint 18 — NPCs
  · Modelo Npc + :feature:npcs
  · Lista + ficha + editor
  · Link bidireccional NPC ↔ Encuentro

Sprint 19 — Planificador de sesiones + Wiki
  · SessionPlan, Scene, WikiEntry en :core:model
  · :feature:planner + :feature:wiki
  · Editor Markdown con resolución de [[links]]

Sprint 20 — Backup & Restauración
  · Export ZIP de toda la biblioteca + schema JSON (D-4)
  · Restore: importar ZIP y recrear la biblioteca

Sprint 21 — UX avanzada
  · C-1  Drag & drop para reordenar cartas en DeckDetail
  · C-2  Panel de config del mazo (DrawMode, dorso, contentMode)
  · B-2  Modo boca abajo al robar (Face Down Draw)
  · C-4  Snackbar con "Deshacer borrado"

Sprint 22+ — Fase 3
  · B-5  Spread layout Tarokka (posiciones nombradas) → Mesa Central del Workspace
  · B-7  Auto-detección de grilla PDF (OpenCV)
  · D-5  Widget de Android (Glance)
  · Búsqueda global con Room FTS

Sprint 23 — Refinamiento de Importación PDF 🔴 ★
  · Rango de páginas: Selección de inicio-fin para descartar anexos
  · Dorso Genérico: Asignar imagen de respaldo global al importar
  · Rotación Manual: Botones de giro en la vista previa

Sprint 24 — Inteligencia y Automatización 🟡 ★★
  · Dice Parser: Detección de [XdY] y tirada con un tap
  · Loot Weights: Sistema de probabilidades de robo por carta
  · Traducción Visual: Capa de texto sobre imagen (OCR)

Sprint 25 — Mecánicas Narrativas 🟢 ★★★
  · Relojes de Progreso: Overlays interactivos sobre cartas
  · Modo Oráculo: Soporte para cartas invertidas
```

---

*Última actualización: 22 de octubre de 2026*
