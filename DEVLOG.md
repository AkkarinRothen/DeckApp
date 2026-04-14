# DeckApp TTRPG — Dev Log

> **Formato de entradas:** `### Descripción (DD de mes de AAAA)`
> Categorías: DECISION | ARCHITECTURE | INTEGRATION | PROBLEM | SOLUTION | PENDING

---

### Sprint 5 — Tablas Aleatorias (14 de abril de 2026)

**ARCHITECTURE — Sistema de tablas aleatorias completo**
- Módulo `:feature:tables` creado desde cero (3 screens, 2 ViewModels, AndroidManifest).
- `AppDatabase` bumpeada a versión 4. Nuevas entidades: `random_tables`, `table_entries`, `table_roll_results`.
- `RandomTable`, `TableEntry`, `TableRollResult`, `TableRollMode` añadidos a `:core:model`.
- `TableRepository` (interface) en `:core:domain`. `TableRepositoryImpl` en `:core:data`.
- `RollTableUseCase` en `:core:domain` — evaluación de dados, resolución de texto inline, sub-tablas 1 nivel.
- `DataModule` actualizado: `RandomTableDao`, `TableRollResultDao`, binding de `TableRepository`.

**FEATURE — RollTableUseCase**
- Regex de fórmulas de dado: `(\d+)d(\d+)([+\-]\d+)?` — soporta `1d6`, `2d8+3`, `1d100`.
- Dados inline en texto: `[1d4+1]` → evaluado y embebido en el resultado final.
- Sub-tabla `@NombreTabla` → lookup por nombre + tirada recursiva (máx. 5 niveles).
- Modos RANGE (por rango) y WEIGHTED (por peso relativo).
- Persiste cada `TableRollResult` en Room antes de retornar.

**FEATURE — TablesTab (tab 1 de SessionScreen)**
- Lista filtrable por categoría (chips) y búsqueda de texto.
- Botón 🎲 en cada item → tirada rápida sin abrir BottomSheet.
- Tap en item → `TableDetailSheet` con resultado animado + historial de los últimos 5 tiros en la sesión.
- FAB para crear nueva tabla (navega a `TableEditorScreen`).

**FEATURE — TableDetailSheet**
- `ModalBottomSheet` con dado animado (valor en cuadrado primary), resultado resuelto.
- Botones "Tirar de nuevo" / "Cerrar".
- Historial por sesión: row compacto con valor de dado, texto y hora.

**FEATURE — TableEditorScreen + TableEditorViewModel**
- Crear y editar tablas: nombre, categoría, descripción, fórmula, modo (RANGE/WEIGHTED).
- Lista editable de entradas con campos min/max (RANGE) o peso (WEIGHTED) + texto.
- Vista previa de tirada desde el editor — guarda temporalmente y tira.
- FAB "Guardar" — valida nombre no vacío antes de persistir.

**INTEGRATION — Bundled tables**
- 9 tablas predefinidas en `app/src/main/assets/tables/bundled_tables.json`.
- `TablesViewModel` las carga al primer lanzamiento si `countBuiltInTables() == 0`.
- Tablas: Encuentros Bosque/Mazmorra, Nombres (humanos M/F + élfico), Clima, Botín Goblin, Crítico/Pifia.

**INTEGRATION — SessionScreen + NavGraph**
- `SessionTabPlaceholder(label = "Tablas")` reemplazado por `TablesTab(sessionId, onCreateTable)`.
- Parámetro `onCreateTable: () -> Unit = {}` añadido a `SessionScreen`.
- Rutas `TablesListRoute` y `TableEditorRoute(tableId: Long = -1L)` añadidas a `Screen.kt` y `NavGraph.kt`.
- `:feature:draw` añadido como dependencia de `:feature:tables` en `build.gradle.kts`.
- **BUILD SUCCESSFUL** en 3m 33s — 407 tareas, sin errores.

---

### Sprint 4 — Visuales: CardAspectRatio, ContentScale.Fit, dots y badge (14 de abril de 2026)

**ARCHITECTURE — Sistema de proporciones de carta (CardAspectRatio)**
- Nuevo enum `CardAspectRatio` en `:core:model`: STANDARD / TAROT / MINI / SQUARE / LANDSCAPE.
- `CardStack.aspectRatio` añadido con default STANDARD. Columna `aspectRatio` en `CardStackEntity`.
- `AppDatabase` bumpeada a versión 3. Mappers actualizados con `runCatching` para migración segura.

**FEATURE — CardThumbnail rediseñado**
- `ContentScale.Fit` reemplaza `Crop` — imagen completa visible, sin recorte.
- Fondo `surfaceVariant` como letterbox cuando la imagen no ocupa todo el espacio.
- Parámetro `aspectRatio: CardAspectRatio` controla el ancho proporcionalmente al alto.
- Dots de caras: `Row` de círculos en borde inferior cuando `card.faces.size > 1`.
  Cara activa = punto grande primary, otras = puntos pequeños semitransparentes.
- Parámetro `showFaceDots: Boolean = true`.

**FEATURE — Badge de conteo en Biblioteca**
- `getTotalCardCount(stackId): Flow<Int>` añadido en `CardDao`, `CardRepository`, `CardRepositoryImpl`.
- `LibraryUiState.deckCardCounts: Map<Long, Int>` — flow combinado de counts por mazo.
- `DeckCoverCard` recibe `cardCount: Int` y muestra `BadgedBox` en la portada.
- `LibraryScreen` pasa `deckCardCounts[deck.id]` a cada `DeckCoverCard`.

**INTEGRATION — DeckDetailScreen + SessionScreen**
- `DeckDetailScreen`: `CardThumbnail` recibe `deck.aspectRatio`.
- `SessionViewModel`: `deckAspectRatios: Map<Long, CardAspectRatio>` cargado del combine existente.
- `SessionScreen`: `SwipeToDiscardCard` recibe `aspectRatio` → pasa a `CardThumbnail`.
- **BUILD SUCCESSFUL** en 48s — sin errores.

---

### SessionScreen refactorizada a workspace multi-tab (14 de abril de 2026)

**ARCHITECTURE — HorizontalPager + TabRow en SessionScreen**
- `SessionScreen` pasa de pantalla monolítica a workspace con `HorizontalPager` de 3 tabs:
  - Tab 0 — Mazos: todo el comportamiento anterior preservado (mano, DeckBar, PileTray)
  - Tab 1 — Tablas: placeholder (`SessionTabPlaceholder`) → se rellena en Sprint 5
  - Tab 2 — Notas: placeholder → Sprint 6
  - Tab 3 — Combate: aparece solo cuando `hasActiveEncounter = true` → Sprint 7
- `MazosTab` extraído como composable privado con la mano, DeckBar y PileTray.
- `SessionFab` extraído como función privada — FAB contextual por tab (ROBAR / TIRAR / NOTA).
- Night Mode overlay elevado a nivel de `Box` raíz — cubre todos los tabs uniformemente.
- `activeTab: Int` y `hasActiveEncounter: Boolean` añadidos a `SessionUiState`.
- `setActiveTab()`, `rollActiveTable()` (stub) y `startQuickNote()` (stub) en `SessionViewModel`.
- Sincronización bidireccional `PagerState ↔ ViewModel` via `LaunchedEffect`.
- **BUILD SUCCESSFUL** — sin errores de compilación, solo warning de `ExperimentalCoroutinesApi` preexistente.

---

### Inicio del Proyecto — Planificación Fundacional (13 de abril de 2026)

**DECISION — Arranque del proyecto**
- App Android nativa para gestión de mazos de cartas TTRPG, orientada a DMs.
- Carpeta base del proyecto: `I:\TTRPG\Visuales\Decks\Deckapp`
- UI en español.
- Vision a largo plazo: suite completa de herramientas para DMs. Fase 1 = mazos.
- Referencia funcional: Foundry VTT Card System (v9+) + módulos Monarch, CCM, Card Hand Mini Toolbar.

**ARCHITECTURE — Stack y estructura de módulos**
- Arquitectura: Multi-módulo Gradle + Clean Architecture (UseCases), igual que TDAPP.
- Módulos: `:core:model`, `:core:domain`, `:core:data`, `:core:ui`,
  `:feature:library`, `:feature:deck`, `:feature:draw`, `:feature:import`, `:feature:session`.
- DI: Hilt. UI: Jetpack Compose + Material 3. DB: Room.
- Event log (`DrawEvent`, append-only) como fuente de verdad de sesión.
  Habilita undo, historial y crash recovery sin código adicional.
- Session-scoped draw state: la sesión tiene copia de trabajo; el deck en biblioteca siempre está limpio.

**DECISION — Modelo de contenido de carta (CardContentMode)**
- Investigación de productos del mercado TTRPG identificó 8 patrones estructurales:
  `IMAGE_ONLY`, `IMAGE_WITH_TEXT`, `REVERSIBLE` (Tarot/Oracle), `TOP_BOTTOM_SPLIT` (Gloomhaven),
  `LEFT_RIGHT_SPLIT`, `FOUR_EDGE_CUES` (Story Engine Deck), `FOUR_QUADRANT`, `DOUBLE_SIDED_FULL` (Arkham Horror LCG).
- Cada mazo tiene `defaultContentMode`; cada carta puede sobreescribir el suyo.
- `CardFace.zones: List<ContentZone>` — número e interpretación de zonas depende del modo.
- `Card.currentRotation: Int` (0/90/180/270) controla qué zona es activa en modos orientación-dependiente.

**DECISION — Import desde PDF**
- Librería PDF: `barteksc/PdfiumAndroid` (wrapper de PDFium, motor de Chrome).
  Más robusto que el `PdfRenderer` nativo de Android.
- Recorte: Kotlin puro + `Bitmap.createBitmap()` para los 4 modos de layout (sin OpenCV en Fase 1).
- 4 modos de layout soportados:
  - Modo A: páginas alternas (frente / dorso / frente / dorso...)
  - Modo B: lado a lado en la misma página
  - Modo C: grilla N×M por página (print-and-play estándar)
  - Modo D: primera mitad PDF = frentes, segunda = dorsos
- UI: preview interactivo con grilla overlay antes de confirmar import.
- Procesamiento en background (WorkManager) — sobrevive a bloqueo de pantalla.
- OpenCV (`zynkware/Document-Scanning-Android-SDK`) reservado para Fase 2: auto-detección de grilla.

**DECISION — Almacenamiento de imágenes**
- Usar SAF (Storage Access Framework) con `takePersistableUriPermission` para acceso a carpetas.
- Imágenes copiadas a almacenamiento interno de la app en el momento del import.
- No guardar paths absolutos externos (se rompen con cambios de SD card o permisos del OS).

**DECISION — Assets de demo**
- `I:\TTRPG\Visuales\Decks\The Ultimate Trove - Assets (Decks)\` (~4,059 archivos, 160 carpetas)
  usado como biblioteca inicial de mazos demo.
- Contiene: Paul Weber, Nord Games, Deck of Many, Tarokka, Pathfinder, Deck of Illusions.

---

---

### Scaffold completo + Primer build exitoso (14 de abril de 2026)

**ARCHITECTURE — Scaffold de todos los módulos**
- Creados todos los feature modules: `:feature:deck`, `:feature:draw`, `:feature:import`, `:feature:session`.
- Cada módulo tiene `build.gradle.kts`, Screen(s), ViewModel(s) iniciales.
- `SessionScreen` implementa wake lock vía `DisposableEffect` + `FLAG_KEEP_SCREEN_ON`.
- `ImportScreen` implementa flujo de 4 fases con preview de PDF (PdfiumAndroid).
- `CardViewScreen` adapta rendering según `CardContentMode` (reversible, FOUR_EDGE_CUES, flip).
- `CardEditorScreen` tiene selector de modo + editores de zonas dinámicos por modo.

**SOLUTION — Errores de build resueltos en iteración**
- `UndoLastActionUseCase`: sintaxis `kotlinx.coroutines.flow.first(it)` → `.first()` con import correcto.
- `android:Theme.Material.NoTitleBar` no existe → corregido a `android:Theme.Material.NoActionBar`.
- PdfiumAndroid v1.9.0 trae `com.android.support:support-compat:26.1.0` que conflictuaba con `androidx.core` → añadido `exclude(group = "com.android.support")`.
- Recursos `mipmap/ic_launcher` faltaban → creados como adaptive icons XML (válidos desde minSdk 26).
- NavGraph pasaba parámetros directos a screens que usan SavedStateHandle → eliminados.
- `DrawCardUseCase.invoke` parámetro `drawMode` llamado como `mode` en SessionViewModel → corregido.
- `SessionViewModel` llamaba `getDrawnCardsForSession()` inexistente → añadido `getDrawnCards()` a CardRepository.
- `ImportDeckUseCase` no existía → creado stub en `:core:domain` con `Result<Long>` como retorno.
- `onCardClick` en SessionScreen era `(Long, Long) -> Unit` pero NavGraph pasaba `(Long) -> Unit` → corregido.

**DECISION — Workflow de desarrollo**
- Ejecutar `:app:assembleDebug` al final de cada sprint para detectar errores antes de la siguiente iteración.

---

---

### Import desde carpeta SAF — Implementación real (14 de abril de 2026)

**SOLUTION — Import de carpeta funcional end-to-end**
- `FileRepository` añadido a `:core:domain` — interfaz para operaciones de archivo (lista imágenes SAF, copia a storage interno, elimina imágenes de mazo).
- `FileRepositoryImpl` en `:core:data` — usa `DocumentsContract` para listar archivos en árbol SAF, `ContentResolver.openInputStream` para copiar. Almacena en `{filesDir}/decks/{deckId}/{fileName}`.
- `FilenameParser` en `:core:domain:util` — parsea `NNN_titulo[_palo].ext` → `CardMetadata(title, value?, suit?)`. Fase 1: el título no puede contener guiones bajos (son separadores de palo).
- `ImportDeckUseCase` actualizado: crea el `CardStack`, procesa la carpeta, crea un `Card + CardFace` por imagen, actualiza la portada del mazo con la primera imagen.
- `DataModule` actualizado: binding `FileRepository → FileRepositoryImpl`.

**ARCHITECTURE — Separación de responsabilidades en import**
- `:core:domain` no tiene `Context` — `FileRepository` es la abstracción limpia.
- `:feature:import` sigue manejando: SAF picker, preview PDF, progreso de UI.
- `:core:domain` maneja: lógica de negocio (qué se importa, cómo se modela).
- `:core:data` maneja: I/O de archivos, queries de Room.
- **PDF import**: queda como TODO Fase 2. `ImportDeckUseCase` reconoce `source="PDF"` pero no procesa. El rendering (PdfiumAndroid) y el recorte (Bitmap.createBitmap) se implementarán en Fase 2.

**DECISION — Convención de nombre de archivo**
- `NNN_titulo[_palo].ext` — Fase 1: sin guiones bajos en el título.
- Ejemplos: `047_dragon.png` → value=47, title="Dragon"; `047_dragon_fuego.png` → suit="Fuego".
- Limitación conocida: títulos con múltiples palabras (`magic_missile`) se interpretan como titulo + palo.

---

### Library, DeckDetail, CardView — Coil file path + Gradle wrapper (14 de abril de 2026)

**SOLUTION — Imágenes internas cargando correctamente con Coil**
- `AsyncImage(model = stringPath)` no funcionaba para rutas absolutas internas (`/data/data/.../files/decks/...`): Coil interpreta los strings como URLs.
- Fix: `AsyncImage(model = File(path))` en `DeckCoverCard`, `CardThumbnail`, `CardViewScreen` y `DeckDetailScreen`.
- Para evitar el warning de smart cast nullable en propiedades de data class, se asigna a una `val` local antes del `null check`.

**SOLUTION — Gradle wrapper funcional desde terminal**
- El proyecto no tenía `gradlew`/`gradlew.bat` ni `gradle-wrapper.jar` (no se habían generado en el scaffold manual).
- Se copió `gradle-wrapper.jar` del proyecto `audio_book_player` (compatible — el jar bootstrapper es agnóstico a la versión; descarga Gradle 8.13 según `gradle-wrapper.properties`).
- Se copiaron los scripts `gradlew` y `gradlew.bat` del mismo proyecto.
- Resultado: `./gradlew :app:assembleDebug` funciona desde la terminal del proyecto.

---

### Fase 1 completa — Loop de juego funcional (14 de abril de 2026)

**SOLUTION — Pile de descarte reactiva**
- Nuevo query en `CardDao.getPiledCards(sessionId)`: cartas con evento DISCARD en esta sesión, después del último RESET, que actualmente tienen `isDrawn = 0`. Query SQL reactivo (Flow) — la pila se actualiza automáticamente.
- `CardRepository` e impl actualizados con `getPiledCards`.
- `SessionViewModel` ahora pobla `uiState.pile` desde este Flow.

**SOLUTION — Reset de mazo**
- `SessionViewModel.resetDeck()`: descarta la mano, llama `cardRepository.resetDeck(stackId)`, registra `DrawAction.RESET` en el event log. El evento RESET es el anchor temporal que limpia la pile en el query SQL.
- Agregado al DropdownMenu de SessionScreen.

**SOLUTION — SessionScreen mejorado**
- `SwipeToDiscardCard`: agrega `graphicsLayer { translationX = offsetX }` — la carta se desliza visualmente hacia la izquierda al hacer swipe. `clickable` para tap a CardViewScreen.
- Haptic fuerte al robar: `LaunchedEffect(uiState.hand.size)` detecta aumento en la mano y dispara `VibrationEffect.createOneShot(80, DEFAULT_AMPLITUDE)`.
- PileTray header completo es clickeable (mejor touch target).

**SOLUTION — CardViewScreen TOP_BOTTOM_SPLIT**
- Extraído `TopBottomZonePanel`: muestra zonas[0] y zonas[1] en dos cards lado a lado (Row), cada una con su etiqueta "Superior" / "Inferior".
- El zone panel ahora usa un `when(face.contentMode)` para despachar el renderizado correcto por modo. `TOP_BOTTOM_SPLIT` va a `TopBottomZonePanel`, el resto al panel genérico.

**Pendiente conocido — Warnings de Room**
- `tagId` en tablas cross-ref no tiene índice → full scan en updates de tags. Aceptable para Fase 1, agregar índices en Fase 2.
- `exportSchema = true` en `DeckAppDatabase` sin `room.schemaLocation` configurado → no genera el archivo JSON de schema. Resolver en Fase 2 aplicando el plugin `androidx.room`.

---

### Fase 2-A — Markwon + Modos de contenido completos (14 de abril de 2026)

**SOLUTION — Markdown rendering con Markwon**
- `MarkdownText` composable en `:core:ui` — wrapper de `AndroidView(TextView)` para Markwon.
- Respeta el color del tema (Material3 `onSurface`) y el tamaño de fuente del `TextStyle` recibido.
- Aplicado en todos los paneles de texto de `CardViewScreen`: `GenericZonePanel`, `TopBottomZonePanel`, `FourQuadrantZonePanel`.

**SOLUTION — FOUR_QUADRANT (Story Engine style)**
- `FourQuadrantOverlay`: overlay 2×2 con `VerticalDivider` + `HorizontalDivider` sobre la imagen de la carta.
- Tap en cuadrante → resalta con indicador de punto y fondo semitransparente.
- `FourQuadrantZonePanel`: muestra el texto del cuadrante seleccionado, con hint "Toca un cuadrante" cuando ninguno está activo.
- Índices: 0=NO, 1=NE, 2=SO, 3=SE.
- Estado local en Screen (`selectedQuadrant`): se resetea al cambiar de carta o cara vía `LaunchedEffect`.

**SOLUTION — DOUBLE_SIDED_FULL (Arkham Horror LCG style)**
- `AnimatedContent(targetState = card.currentFaceIndex)` en el área de imagen con slide horizontal.
- Dirección del slide sigue la lógica: cara siguiente → slide desde la derecha; cara anterior → desde la izquierda.
- El botón "Voltear carta" ya existía — ahora tiene animación.

**ARCHITECTURE — CardViewScreen refactorizada**
- Extraídos componentes privados: `CardFaceImage`, `FourQuadrantOverlay`, `QuadrantCell`, `FourQuadrantZonePanel`, `TopBottomZonePanel`, `GenericZonePanel`.
- El panel de zonas usa `when(face.contentMode)` limpio: `TOP_BOTTOM_SPLIT`, `FOUR_QUADRANT`, y `else` (todos los demás modos).

---

### Sesión multi-mazo — DeckBar + badge en mano (14 de abril de 2026)

**DECISION — Scope multi-mazo**
- La app es exclusivamente para el DM. No hay jugadores en la UI.
- Sistemas como Plot Deck o Tarokka requieren múltiples mazos activos simultáneamente.
- `SessionSetupScreen` ya soportaba multi-mazo en el modelo; el gap era la UI de juego.

**ARCHITECTURE — Selección de mazo activo**
- `SessionUiState` nuevos campos: `selectedDeckId: Long?`, `deckCardCounts: Map<Long, Int>`, `deckNames: Map<Long, String>`.
- `deckCardCounts` es reactivo: usa `flatMapLatest` sobre `getDecksForSession` → `combine` de `getAvailableCount(stackId)` por mazo → mapa stackId→count en tiempo real.
- `CardRepository.getAvailableCount(stackId): Flow<Int>` — nuevo método usando DAO ya existente.
- Auto-selección del primer mazo al cargar la sesión (si `selectedDeckId == null`).

**SOLUTION — DeckBar composable**
- Aparece solo si `deckRefs.size > 1` (sesiones de 1 mazo no lo necesitan).
- `LazyRow` de `FilterChip`s; cada chip muestra nombre + "N disponibles".
- El chip seleccionado se resalta con `primaryContainer`.
- Tap en chip → `viewModel.selectDeck(stackId)`.

**SOLUTION — FAB ROBAR multi-mazo**
- `drawCard()` usa `selectedDeckId` en lugar de `deckRefs.firstOrNull()`.
- Con múltiples mazos, el FAB muestra el nombre del mazo activo debajo de "ROBAR".
- `resetDeck()` ahora resetea **todos** los mazos de la sesión (no solo el primero).

**SOLUTION — Badge de mazo en cartas de la mano**
- En sesiones multi-mazo: `card.stackId` se cruza con `deckNames` para mostrar un badge.
- Overlay `Surface` con `primaryContainer.copy(alpha = 0.9f)` en la base de cada carta.
- En sesiones de 1 mazo: badge oculto (no agrega ruido visual innecesario).

**SOLUTION — TopBar dinámica**
- Con 1 mazo: subtítulo "N en mano" (comportamiento anterior).
- Con múltiples mazos: subtítulo "N en mano  ·  Mazo1 X  ·  Mazo2 Y  ···" (resumen rápido de disponibles).

---

### Tags + búsqueda/filtro en biblioteca (14 de abril de 2026)

**SOLUTION — Búsqueda por nombre en LibraryScreen**
- `LibraryUiState` migrado a `MutableStateFlow` con `searchQuery`, `selectedTagIds`, `allTags`.
- `filteredDecks` como propiedad derivada en el data class — filtrado por nombre (contiene, ignoreCase) y por tags (cualquier tag del mazo está en el set seleccionado).
- `OutlinedTextField` con ícono Search + botón Clear cuando hay texto.
- Estado "Sin resultados" con botón "Limpiar filtros".

**SOLUTION — Filtro por tags como chips en LibraryScreen**
- `LazyRow` de `FilterChip`s debajo del buscador, visible si hay tags.
- `toggleTagFilter(tagId)` actualiza `selectedTagIds: Set<Long>` con toggle on/off.
- Los chips solo aparecen cuando la biblioteca tiene al menos un tag creado.

**SOLUTION — Gestión de tags en DeckDetailScreen**
- Sección de tags con scroll horizontal (encima del hint y del HorizontalDivider).
- `InputChip` por cada tag con botón × (`combinedClickable(onClick = { removeTag(tagId) })`).
- `AssistChip` "+ Tag" abre `AddTagDialog` (AlertDialog con OutlinedTextField).
- `DeckDetailViewModel.addTag(name)`: crea el tag via `saveTag()`, lo añade al deck via `saveStack(deck.copy(tags = ...))`.
- `DeckDetailViewModel.removeTag(tagId)`: filtra tags y llama `saveStack(deck.copy(...))`.
- Tags duplicados ignorados (comparación insensible a mayúsculas).

**PROBLEM/SOLUTION — Crash al borrar mazo (FOREIGN KEY constraint failed)**
- `session_deck_refs.stackId` FK a `card_stacks` no tenía `ON DELETE CASCADE`.
- Al borrar un mazo referenciado en una sesión, SQLite lanzaba `SQLiteConstraintException`.
- Fix: `onDelete = ForeignKey.CASCADE` en `SessionDeckRefEntity.stackId`.
- DB version bumped 1 → 2 con `fallbackToDestructiveMigration()` (dev only).

---

### Borrado de mazos y cartas (14 de abril de 2026)

**DECISION — Features excluidas del scope DM-solo**
- Deal (repartir a jugadores), Pass (pasar carta a jugador), Peek multiusuario: fuera del roadmap.
- La app es exclusivamente para el DM. No hay modelo de jugador en Fase 1/2.

**SOLUTION — Borrado de mazos desde LibraryScreen**
- `DeckCoverCard` recibe `onDelete: (() -> Unit)? = null`. Cuando está presente, muestra botón ⋮ en la fila inferior del card que abre un `DropdownMenu` con "Eliminar mazo" en rojo.
- `LibraryScreen` gestiona `confirmDeleteDeck: CardStack?` como estado local. Cuando no es null, muestra un `AlertDialog` de confirmación.
- `LibraryViewModel.deleteDeck(id)`: primero borra imágenes internas via `fileRepository.deleteImagesForDeck(id)`, luego `cardRepository.deleteStack(id)`. Room elimina en cascada cartas, caras y cross-refs de tags.

**SOLUTION — Borrado de cartas desde DeckDetailScreen**
- `CardThumbnail` envuelto en `Modifier.combinedClickable(onClick=..., onLongClick=...)`.
- Long-press abre `AlertDialog` de confirmación con nombre de la carta.
- `DeckDetailViewModel.deleteCard()` ya existía — solo se conectó a la UI.
- Hint de instrucción visible bajo el contador de cartas: "Mantén presionada una carta para eliminarla".

---

### Fase 2-C — Undo completo + Night Mode (14 de abril de 2026)

**SOLUTION — Undo superficialmente completo**
- `UndoLastActionUseCase` ahora revierte también `DrawAction.DISCARD`: llama `updateCardDrawnState(cardId, isDrawn = true)`, devolviendo la carta a la mano.
- Antes solo revertía DRAW, FLIP, REVERSE, ROTATE. PASS y RESET siguen siendo no revertibles (demasiado estado involucrado).

**SOLUTION — canUndo reactivo en UI**
- `SessionUiState.canUndo: Boolean` derivado de observar `getEventsForSession(sessionId).map { it.isNotEmpty() }`.
- El botón Undo en `SessionScreen` usa `enabled = uiState.canUndo` — deshabilitado cuando no hay nada que deshacer.
- `SessionUiState.snackbarMessage: String?` muestra "Acción deshecha" o "Nada que deshacer" via `SnackbarHost`.

**SOLUTION — Night Mode overlay**
- `SessionUiState.nightMode: Boolean` + `toggleNightMode()` en `SessionViewModel`.
- Toggle desde el `DropdownMenu` de la TopBar con texto dinámico ("Modo nocturno" / "Desactivar modo nocturno").
- Overlay: `Box` con `Color.Black.copy(alpha = 0.55f)` sobre el área de la mano.
- Tap sobre el overlay desactiva el modo nocturno directamente (acceso rápido).
- El tray de descarte queda fuera del overlay (acceso siempre visible).

---

### Fase 2-B — CardEditorScreen completo (14 de abril de 2026)

**SOLUTION — CardEditorScreen funcional end-to-end**
- `CardEditorViewModel` actualizado: inyecta `FileRepository` para copiar imágenes a storage interno.
- Nuevo método `pickFaceImage(faceIndex, uri)`: copia la imagen seleccionada via `GetContent` a `{filesDir}/decks/{deckId}/face_{n}_{timestamp}.jpg`, actualiza `face.imagePath`.
- Nuevo método `updateFaceName(faceIndex, name)`: permite renombrar caras desde la UI.
- Nuevo método `removeFace(faceIndex)`: elimina la cara (guarda siempre mínimo 1) y ajusta `selectedFaceIndex`.
- `isPickingImage: Boolean` en `CardEditorUiState` — muestra `CircularProgressIndicator` en TopBar mientras se copia.

**ARCHITECTURE — Separación imagen vs estado**
- El picker de imagen (`rememberLauncherForActivityResult(GetContent)`) vive en la Screen.
- `pendingPickFaceIndex` como estado local temporal entre el lanzamiento del picker y su callback.
- La copia real de archivos ocurre en el ViewModel via `FileRepository` (no en la Screen).

**SOLUTION — FaceImagePicker composable**
- Si la cara tiene imagen: muestra `AsyncImage` 160dp de alto + dos FAB flotantes ("Cambiar" / "×" para quitar).
- Si no tiene imagen: `OutlinedButton` "Seleccionar imagen".
- `zoneLabelsFor(mode)` función pura extraída para devolver las etiquetas de zona según el modo.

**SOLUTION — Eliminar cara con botón × en tab**
- Botón `IconButton` (18dp) con `Icons.Default.Close` (14dp) integrado dentro del texto del `Tab`.
- Solo visible cuando `faces.size > 1`.

---

### Sprint 3 — Importación y Exportación Completa (ZIP) (14 de abril de 2026)

**SOLUTION — Importación desde ZIP**
- `FileRepository.unzipToTemp(zipUri)`: descomprime archivos en un directorio único dentro de `cacheDir`. Filtra solo extensiones de imagen soportadas.
- `ImportDeckUseCase` actualizado para manejar la fuente `ZIP`: descomprime y luego usa la lógica compartida de importación de imágenes.
- `ImportScreen` & `ImportViewModel`: nueva opción "Archivo ZIP" en la fase de selección.

**SOLUTION — Exportación a ZIP**
- `FileRepository.zipDeckDirectory(deckId, outputUri)`: empaqueta todas las imágenes del directorio interno del mazo en un archivo ZIP directamente en el URI de destino (SAF).
- `ExportDeckToZipUseCase` creado en `:core:domain`.
- `DeckDetailScreen`: ítem "Exportar ZIP" en el menú de opciones.
- `ActivityResultContracts.CreateDocument` integrado para que el usuario elija dónde guardar el archivo exportado.
- Feedback vía `SnackbarHost` para informar éxito o error en la exportación.

**ARCHITECTURE — Reutilización de lógica de importación**
- Refactorizada la lógica de importación de imágenes en `ImportDeckUseCase` hacia el método privado `importFromImageList`, permitiendo que tanto carpetas como archivos ZIP sigan el mismo flujo de parseo de metadatos y copia interna.

---

### Cierre de Sprint 3 — Build estable y Sincronización (14 de abril de 2026)

**INTEGRATION — Build exitoso de fin de sprint**
- Ejecutado `:app:assembleDebug` con éxito. Generado `app-debug.apk` funcional con todas las features de Fase 1 y Fase 2-A/B/C.
- Verificación de regresiones en import ZIP y export ZIP (ambos operativos).
- El event log (`DrawEvent`) persiste correctamente todas las acciones de la sesión multi-mazo.

**ARCHITECTURE — Sincronización de documentación**
- `PLANNING.md` y `DEVLOG.md` actualizados para reflejar el progreso real.
- Fase 1 marcada como 95% completada (solo resta el procesamiento real de PDF).
- Fase 2 marcada como 60% completada.
- Feature de Exportación ZIP adelantada de Fase 3 a Fase 2 (completada).

---

## 📋 Pendientes Iniciales

- [x] Crear proyecto Android Studio con estructura multi-módulo Gradle
- [x] Definir Room schema v1:
      `CardStackEntity`, `CardEntity`, `CardFaceEntity`, `ContentZoneEntity`,
      `DrawEventEntity`, `SessionEntity`, `TagEntity`
- [x] Agregar dependencia `PdfiumAndroid` a `:feature:import`
- [x] Implementar `ImportDeckFromFolderUseCase` en `:core:domain`
- [ ] Implementar `ImportDeckFromPdfUseCase` con los 4 modos de layout
- [x] Smoke test: `LibraryScreen` con mazo hardcodeado
- [x] Verificar SAF + `takePersistableUriPermission` en Android 11+
      (los assets están en almacenamiento externo — requiere folder picker)
- [x] Definir convención de auto-detect desde filename:
      propuesta: `NNN_nombre[_palo].ext` → value=NNN, name="Nombre", suit="Palo"
- [x] Evaluar si Markwon funciona dentro de Compose (Fase 2 pero elegir librería ahora)
- [x] Definir nombre definitivo de la app
- [x] Decidir target API mínima (propuesta: API 26 / Android 8.0)
- [x] **Sprint 6: Notas de DM e Historial de Sesión (2026-04-14)**
    - [x] Implementado Tab de Notas en SessionScreen con auto-guardado y preview Markdown.
    - [x] Implementado `SessionHistoryScreen` con timeline vertical y estadísticas.
    - [x] Añadido timer de sesión en el TopBar de la sesión activa.
    - [x] Migración Room v4 → v5 con campo `dmNotes` en `SessionEntity`.
    - [x] Añadido `DrawAction.PEEK` para registrar revisiones del tope del mazo.

---

## 💡 Decisiones de Diseño Anotadas

### Por qué Event Log en lugar de estado mutable
Room guarda el estado actual de las cartas, pero el historial de la sesión se construye
sobre un event log append-only (`DrawEvent`). Ventajas: undo gratis, historial completo,
crash recovery sin lógica adicional. El estado actual es una proyección del log.

### Por qué PdfiumAndroid y no PdfRenderer nativo
`android.graphics.pdf.PdfRenderer` (API 21+) tiene limitaciones severas: solo ARGB_8888,
no soporta PDFs cifrados ni anotaciones, rendimiento limitado en páginas grandes.
`PdfiumAndroid` usa el mismo motor que Chrome (PDFium) y es mucho más robusto con
los PDFs comerciales de mazos TTRPG (Nord Games, Paul Weber, etc. a veces usan features
avanzadas de PDF).

### Por qué Kotlin puro para el recorte (sin OpenCV en Fase 1)
El recorte de cartas en los 4 modos de PDF import es matemático: se divide la página en
rectángulos iguales usando las dimensiones conocidas. No requiere detección de bordes.
Esto mantiene el APK ~40MB más liviano. OpenCV se agrega en Fase 2 solo para el feature
opcional de auto-detección cuando el usuario no conoce el layout del PDF.
