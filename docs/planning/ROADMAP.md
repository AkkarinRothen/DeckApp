# DeckApp TTRPG — Roadmap de Mejoras

> **Documento vivo.** Registrar aquí ideas, mejoras pendientes y decisiones de dirección antes de
> llevarlas a código. Ver `DEVLOG.md` para el historial de lo ya implementado.
>
> **Escala de impacto:** 🔴 Alto — 🟡 Medio — 🟢 Bajo  
> **Escala de esfuerzo:** ★★★ Grande (>1 sprint) — ★★ Medio (~1 sprint) — ★ Pequeño (<1 día)

---

## Estado actual del proyecto (15 de abril de 2026)

### ✅ Implementado y funcional
- Import desde carpeta SAF (imágenes → cartas, auto-detect de metadata de filename)
- Import desde ZIP y Export a ZIP
- PDF import con **PdfiumAndroid** (migrado en Sprint 11 — 4 modos de layout, grilla configurable, auto-trim)
- 8 modos de contenido de carta completos (renderizado + edición)
- CardEditorScreen multi-cara con picker de imagen y zonas editables
- SessionScreen: workspace multi-tab (`HorizontalPager`) — Mazos / Tablas / Notas / Combate (dinámico)
- Robo, descarte, undo, night mode, haptic, wake lock, multi-mazo
- Peek (ver tope sin robar), Browse mode desde sesión
- Tags en mazos + búsqueda y filtro en Biblioteca
- **Duplicar mazo** (Sprint 12 — `DuplicateDeckUseCase`)
- **Fusionar mazos** (Sprint 12 — `MergeDecksUseCase` + dialog de selección de destino)
- **Archivar/restaurar mazos** (Sprint 13 — toggle en Biblioteca con chip "Archivados")
- Borrar y renombrar sesiones
- `:feature:settings` con uso de almacenamiento por mazo + **calidad JPEG configurable** (Sprint 11)
- Room: **migraciones escritas** v1→v8 — sin `fallbackToDestructiveMigration` en prod (Sprint 11)
- Tags cross-ref: **índices en `card_tags` y `card_stack_tags`** (Sprint 11)
- **Sistema de Tablas Aleatorias completo** — `:feature:tables`, 9 tablas bundled, modos RANGE/WEIGHTED
- **Import de tablas** — OCR (ML Kit), CSV, JSON (Foundry VTT), texto plano; flujo de 5 pasos
- **Homografía + auto-detección de bordes** en import OCR; stitching multi-página (Sprint 8)
- **Export de tablas** a JSON compatible con formato DeckApp / Foundry VTT
- `RangeParser`, `CsvTableParser`, `JsonTableParser`, `PlainTextTableParser` — motor de parsing robusto

### ⚠️ Incompleto o con deuda técnica
- `SessionHistoryScreen` — implementada la navegación, pendiente el contenido real (timeline de DrawEvents)
- Notas de DM — `Session.dmNotes` existe en el modelo, la UI está pendiente (ver `DM_NOTES.md`)
- Tab "Notas" en SessionScreen — stub vacío (sprint pendiente)
- `Card.dmNotes` — no existe todavía en el modelo
- Exportar resumen de sesión (notas + tiradas + cartas) — Fase 2

---

## A — Deuda Técnica

| # | Ítem | Impacto | Esfuerzo | Estado |
|---|------|---------|----------|--------|
| A-1 | Migrar PDF rendering de `PdfRenderer` nativo a **PdfiumAndroid** | 🔴 | ★★ | ✅ Hecho (Sprint 11) |
| A-2 | Room: strategy de migración real (quitar `fallbackToDestructiveMigration`) | 🔴 | ★★ | ✅ Hecho (Sprint 11) — migraciones v1→v8 escritas |
| A-3 | Índices en tablas cross-ref de tags (`card_tags`, `card_stack_tags`) | 🟡 | ★ | ✅ Hecho (Sprint 11) |
| A-4 | Compresión de imagen configurable en import | 🟡 | ★ | ✅ Hecho (Sprint 11) — Settings: Alta/Media/Baja |
| A-5 | Manejo de errores visible en Import (fallo de archivo individual) | 🟡 | ★ | ✅ Hecho (Sprint 11) — `failedFiles` en `ImportUiState` |
| A-6 | Export ZIP de mazo desde `DeckDetailScreen` overflow | 🟡 | ★ | ⏳ UseCase existe, falta el botón en la UI |
| A-7 | Progress bar de almacenamiento por mazo en `SettingsScreen` | 🟢 | ★ | ⏳ Pendiente |

---

## B — Features Pendientes del Plan Original

### B-1 — Historial de sesión (SessionHistoryScreen) 🔴 ★★
La pantalla `SessionHistoryScreen` existe en el módulo pero es un stub. El `DrawEvent` log
ya contiene toda la información necesaria.

**Comportamiento esperado:**
- Accesible desde el listado de sesiones → tap en sesión finalizada
- Timeline vertical con íconos por acción (DRAW / DISCARD / FLIP / RESET…)
- Cada ítem: hora, nombre de la carta, imagen thumbnail, acción realizada
- Filtros: por mazo, por tipo de acción
- Estadísticas al final: total de cartas robadas, descartadas, tiempo de sesión

---

### B-2 — Modo boca abajo al robar (Face Down Draw) 🔴 ★
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

### B-3 — Imagen de dorso por mazo 🔴 ★
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

### B-5 — Spread layout Tarokka (posiciones nombradas) 🟡 ★★★
Layout visual de una tirada de Tarokka (Cruz de 5 posiciones) en `SessionScreen`.

**Comportamiento esperado:**
- Modo "Tirada" accesible desde el overflow de sesión cuando el mazo activo es Tarokka
- Canvas con 5 posiciones nombradas: Pasado, Presente, Futuro, Obstáculo, Resultado
- Al robar, la carta se coloca en la siguiente posición libre
- Cada posición tiene etiqueta y puede mostrarse boca abajo (revelar individualmente)
- Vista de resumen de la tirada completa

**Alcance Fase 3** — requiere diseño UI propio.

---

### B-6 — Clonar sesión (reusar configuración) 🟡 ★
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
- [ ] **DeckDetail (Cartas individuales)**: Pendiente (Sprint 27).

---

### C-2 — Panel de configuración del mazo en DeckDetail 🔴 ★★
Actualmente no hay forma de cambiar el `DrawMode`, el `defaultContentMode`, ni la imagen de dorso
de un mazo existente desde la UI.

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

---

### C-4 — Snackbar con opción "Deshacer borrado" de mazo/carta 🟡 ★
Actualmente el borrado es inmediato e irreversible (con confirmación, pero sin recuperación).

**Comportamiento esperado:**
- Al borrar un mazo: mostrar Snackbar con botón "Deshacer" (5 segundos)
- Si el DM toca "Deshacer" antes del timeout: restaurar el mazo y sus imágenes
- Si el timeout expira: confirmar la eliminación definitiva

**Implementación:** diferir la eliminación real al cierre del Snackbar, cancelar el Job si
el DM pulsa deshacer.

---

### C-5 — Animación de barajar en SessionScreen 🟢 ★
Visual feedback cuando el DM baraja el mazo (shuffle).

**Comportamiento esperado:**
- Al llamar a "Barajar mazo": animación de 2-3 cartas del mazo superpuestas que se mezclan
- Opcional: sonido de barajar (AudioManager o SoundPool, silenciable)

---

### C-6 — Compartir imagen de una carta 🟢 ★
Desde `CardViewScreen`, opción de compartir la imagen de la cara activa como JPEG.

**Comportamiento esperado:**
- Botón de Share en el TopAppBar de CardViewScreen
- Usa `FileProvider` + `Intent.ACTION_SEND` con `image/jpeg`
- Comparte la imagen sin metadata del DM

---

## D — Features Nuevas (no estaban en el plan original)

### D-1 — Notas de DM 🔴 ★★★
`Session.dmNotes: String?` existe en el modelo. Plan completo en `DM_NOTES.md`.
Roadmap en 3 fases: notas por sesión → notas por carta + Quick Notes → diario de campaña completo.

---

### D-2 — Modo "Carta aleatoria" con filtro de palo 🟡 ★
Robar una carta aleatoria de un palo específico sin cambiar el DrawMode global.

**Comportamiento esperado:**
- En el overflow del mazo en `SessionScreen`: "Robar al azar por palo…"
- BottomSheet con chips de palos disponibles en el mazo seleccionado
- Roba una carta aleatoria del palo elegido que esté disponible

---

### D-3 — Timer de sesión 🟡 ★
Mostrar el tiempo transcurrido desde el inicio de la sesión activa.

**Comportamiento esperado:**
- En el TopAppBar de `SessionScreen`: texto sutil "2h 14m" junto al nombre
- Calculado como `System.currentTimeMillis() - session.createdAt`
- Se actualiza cada minuto (no en tiempo real)

---

### D-4 — Backup y restauración de la biblioteca 🔴 ★★
Crear una copia de seguridad completa de todos los mazos importados.

**Comportamiento esperado:**
- En Settings: "Crear backup" → exporta un ZIP grande con todas las imágenes + el schema de Room como JSON
- "Restaurar backup" → importa el ZIP y recrea toda la biblioteca
- Compatible entre instalaciones/dispositivos

**Dependencia:** requiere serialización del modelo de dominio a JSON (kotlinx.serialization ya en el proyecto).

---

### D-5 — Widget de Android para sesión activa 🟢 ★★★
Glance widget (API de widgets de Compose para Android) en la pantalla de inicio.

**Contenido del widget:**
- Nombre de la sesión activa
- Contadores de cartas disponibles
- Botón "ROBAR" que abre la app directamente en `SessionScreen`
- Se oculta cuando no hay sesión activa

**Aplica a Fase 3.** Requiere `androidx.glance:glance-appwidget`.

---

## E — Decisiones de Producto Pendientes

| # | Pregunta | Contexto | Opciones |
|---|----------|----------|---------|
| E-1 | **¿Sincronización en la nube?** | Supabase ya conocido del proyecto TDAPP. Permitiría continuar en tableta + teléfono. | (a) No, storage local siempre. (b) Supabase opt-in en Fase 3. (c) Backup/restore manual (D-4) como sustituto. |
| E-2 | **¿Nombre definitivo de la app?** | "DeckApp" es genérico. El package es `com.deckapp.ttrpg`. | Opciones a considerar: *Arcana*, *Vault* (TTRPG Vault), *TableDeck*, *Grimoire*, *Spellbook*... |
| E-3 | **¿Target mínimo API 26 o subir a API 28?** | API 26 = Android 8.0 (2017). API 28 = Android 9.0 (2018). | Subir a 28 permite `NotificationChannel` simplificado y mejoras de seguridad en file access. |
| E-4 | **¿Soporte de orientación horizontal en SessionScreen?** | En tablets o teléfonos en landscape, la mano horizontal queda muy estrecha verticalmente. | (a) Forzar portrait siempre (simple). (b) Layout adaptativo (complejo). (c) Config por sesión. |
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

## Próximos Sprints

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

*Última actualización: 17 de abril de 2026*
