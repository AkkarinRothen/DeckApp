---

### Sprint 44D — Modo Sesión e Integraciones (19 de abril de 2026)

**FEATURE — HexMapSessionScreen: tracking de exploración en vivo**
- **`HexMapSessionScreen`**: canvas en modo SESSION con fog of war activo. Header con día actual y actividades usadas. FAB "+" para nuevo día.
- **`SessionTileSheet`** (`ModalBottomSheet`): chips de estado (Explorado/Reconocido/Mapeado), botones de acción con coste de actividades, notas de jugador visibles post-exploración, POIs con botones "Tirar tabla" y "Encuentro".
- **`StatusChip`**: chip visual con ícono de check cuando el estado está activo.
- **`HexMapSessionViewModel`**: auto-vincula el mapa a la sesión activa al abrir (`LinkHexMapToSessionUseCase`), gestiona día actual con `StartNewHexDayUseCase`, delega acciones a `ExploreHexUseCase`, `ReconnoiterHexUseCase`, `MapHexUseCase`.
- **Integración Sessions**: `SessionRepository.getActiveSession()` inyectado, mapa se vincula automáticamente.
- **Integración Tables**: `RollTableUseCase` invocado desde POI con `tableId`; resultado mostrado en `AlertDialog` con nombre de tabla y texto resuelto.
- **Integración Encounters**: desde POI navega a `EncounterEditorRoute(encounterId)` o a `EncounterListRoute` si no hay encuentro vinculado.
- **Punto de entrada**: "Mapas Hex" agregado al dropdown de `LibraryScreen` (icono `Layers`).
- Fix: smart cast en `poi.tableId`/`poi.encounterId` resuelto con variables locales (propiedad de módulo externo).

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — sprint 44 completo, feature hexploration operativo.

---

### Sprint 44C — Pantallas de Lista y Editor de Hexploración (19 de abril de 2026)

**FEATURE — HexMapListScreen y HexMapEditorScreen funcionales**
- **`HexMapListScreen`**: lista de mapas con `LazyColumn`, `Card` clickable, botón borrar con `AlertDialog` de confirmación, FAB para crear nuevo mapa. Estado vacío con mensaje guía.
- **`CreateHexMapDialog`**: campos de nombre, columnas y filas (3–40) con preview del total de hexes. Creación inmediata navega al editor.
- **`HexMapEditorScreen`**: `HexGridCanvas` en modo DESIGN ocupa el espacio disponible, barra de terreno en la parte inferior, FAB "Iniciar sesión" con navegación a `HexMapSessionRoute`.
- **`TerrainBrushToolbar`**: `LazyRow` con 8 pinceles predefinidos (abierto, difícil, muy difícil, infranqueable, agua, llanura, bosque, montaña). Tap aplica el terreno directamente al hex.
- **`TileDetailSheet`** (`ModalBottomSheet`): long press abre notas DM/jugadores (editables + guardado), lista de POIs del hex con botón borrar, botón agregar POI.
- **`AddPoiDialog`**: nombre, tipo (dropdown con todos los `PoiType`), descripción opcional.
- **`HexMapEditorViewModel`**: `SavedStateHandle["mapId"]` (patrón del proyecto, sin dependencia circular a `:app`). Brush activo aplicado en tap, tile seleccionado en long press.
- Rutas `HexMapListRoute`, `HexMapEditorRoute(mapId)`, `HexMapSessionRoute(mapId)` registradas en `Screen.kt` y `NavGraph.kt`.
- Fix: eliminado `toRoute<HexMapEditorRoute>()` del ViewModel (causaría dependencia circular `:feature → :app`).

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 407 tasks, 16 executed.

---

### Sprint 44B — HexGridCanvas (19 de abril de 2026)

**FEATURE — Canvas de hexágonos con lazy rendering y modos Diseño/Sesión**
- Nuevo módulo `:feature:hexploration` registrado en `settings.gradle.kts` y `app/build.gradle.kts`.
- **`HexGridCanvas`** (`components/HexGridCanvas.kt`): Composable Canvas con coordenadas axiales flat-top.
- **Lazy rendering / viewport culling**: en cada frame se filtran los tiles cuyo centro cae fuera del viewport expandido (+2 hex de margen). Sin cap de tamaño — mapas de cualquier dimensión son viables.
- **Pan + pinch-zoom**: `rememberTransformableState` con zoom entre 0.3× y 4×.
- **Tap / Long press**: `detectTapGestures` convierte coordenadas de pantalla → mundo → axial con `axialRound` para hit-testing preciso.
- **Modo DESIGN**: muestra terreno, indica coste con puntos blancos en tiles difíciles (cost ≥2).
- **Modo SESSION**: fog of war oscuro en tiles no explorados; tint blanco en reconocidos; shimmer dorado en mapeados.
- **POI overlay**: círculo de color según `PoiType` (8 colores semánticos) visible en diseño o cuando el hex está explorado.
- Hex stroke dorado + grosor 3px en tile seleccionado.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 406 tasks, 25 executed.

---

### Sprint 44A — Dominio y Base de Datos de Hexploración (19 de abril de 2026)

**FEATURE — Capa de dominio completa para el feature Hexploración**
- **Modelos** (`core:model`): `HexMap`, `HexTile`, `HexPoi`, `HexDay`, `HexActivityEntry`, `HexActivityType`, `HexStyle`, `PoiType`.
- **Repository interface** (`core:domain`): `HexRepository` con métodos Flow + suspend para mapas, tiles, POIs y días.
- **11 UseCases** en `core:domain/usecase/hex/`: `GetHexMapsUseCase`, `GetHexMapWithTilesUseCase`, `CreateHexMapUseCase` (genera grid de tiles automáticamente), `UpdateHexTileUseCase`, `ExploreHexUseCase`, `ReconnoiterHexUseCase` (costo según `terrainCost`), `MapHexUseCase`, `AddHexPoiUseCase`, `DeleteHexPoiUseCase`, `StartNewHexDayUseCase`, `LogHexActivityUseCase`, `LinkHexMapToSessionUseCase`, `DeleteHexMapUseCase`.
- **Room** (`core:data`): `HexEntities.kt` (4 entidades con FK CASCADE), `HexDao.kt`, `HexMappers.kt` (toDomain/toEntity), `HexRepositoryImpl.kt`.
- **Migración 29→30**: 4 nuevas tablas (`hex_maps`, `hex_tiles`, `hex_pois`, `hex_days`) con índices e integridad referencial.
- **DI**: `HexDao` y `HexRepositoryImpl` registrados en `DataModule.kt`.
- `HexActivityEntry` y `HexActivityType` anotados con `@Serializable` para persistencia JSON en `HexDayEntity.activitiesLog`.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 384 tasks, 71 executed.

---

### Sprint 43 — Resultado de Tirada Inline en Tarjetas de Tabla (18 de abril de 2026)

**FEATURE — Quick Roll con resultado visible sin abrir la tabla**
- Al presionar TIRAR en una tarjeta de tabla, el resultado aparece directamente en la propia tarjeta sin navegar a ningún detalle.
- `TablesUiState` extendido con `quickRollResults: Map<Long, TableRollResult>` — cada tabla mantiene su último resultado independientemente.
- `TablesViewModel.rollTable()` ahora también actualiza `quickRollResults[tableId]`.
- `TablesViewModel.clearQuickRoll(tableId)` elimina el resultado de una tabla específica.
- **`TableGridItem` (biblioteca standalone)**: modo dual con `AnimatedContent (fadeIn/fadeOut)`:
  - *Vista normal*: nombre, descripción, tags y botón TIRAR.
  - *Vista resultado*: fondo secundario, badge con el número de dado, texto resuelto y botón TIRAR DE NUEVO + × para cerrar.
- **`TableListItem` (vista sesión)**: resultado inline expandido debajo del nombre. El dado cambia a icono `Refresh` y el pin se sustituye por `×` mientras hay resultado activo.
- Grid en `TablesTab` refactorizado para usar `TableGridItem` compartido (eliminado el duplicado local más simple).

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — sin errores en feature:tables, feature:draw ni app.

---

### Sprint 42 — Fix BOM en Archivos de Packs (18 de abril de 2026)

**FIX — BOM causaba fallo al instalar todos los packs de tablas**
- Los 5 archivos de packs de tablas (`tables_adventure.json`, `tables_combat_magic.json`, `tables_names.json`, `tables_social.json`, `tables_world.json`) tenían BOM UTF-8 (`EF BB BF`) al inicio.
- `kotlinx.serialization` no tolera BOM: lanzaba `Unexpected JSON token at offset 0: Expected '{', but had '"'` al intentar instalar cualquier pack de tablas.
- El error era silencioso en `loadBundledTablesIfEmpty()` (atrapado por `catch`) pero visible en el nuevo diálogo de packs.
- **Fix 1 (fuente)**: BOM eliminado de los 5 archivos JSON de assets.
- **Fix 2 (defensivo)**: Agregado `.trimStart('\uFEFF')` al leer cualquier asset de pack en `TablesViewModel` y `ReferenceListViewModel`, para tolerar futuros archivos con BOM.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — APK generado con archivos de assets corregidos.

---

### Sprint 41 — Pack Manager en Pantalla de Tablas (18 de abril de 2026)

**FEATURE — Pack Manager nativo en Tablas**
- Eliminada la dependencia de navegar a Referencias para gestionar packs de tablas.
- `TablesViewModel` ahora inyecta `InstallStarterPackUseCase` y `RemoveStarterPackUseCase`, y expone `installTablePack()` / `removeTablePack()` / `setShowPackDialog()`.
- `observeInstalledPacks()`: colecta `getInstalledPackNames()` en tiempo real y filtra solo los packs `tables_*.json`, construyendo la lista de `TablePackInfo` reactiva.
- `TablesUiState` extendido con `showPackDialog` y `availableTablePacks`.
- Nuevo composable `TablePackDialog` en `TableLibraryScreen.kt` (visibilidad `internal`) con estados Instalado / Disponible y acciones Instalar / Actualizar / Quitar.
- `TableLibraryScreen`: botón de pack en TopBar (icono `AutoAwesome`); botón de estado vacío ahora abre el diálogo local.
- `TablesTab` (usado en sesiones): nuevo `SmallFloatingActionButton` terciario con `AutoAwesome`; diálogo de packs también disponible en el contexto de sesión; estado vacío corregido.
- `onExplorePacks` eliminado de `TablesTab` y `TableLibraryScreen` — era un callback de navegación a Referencias que ya no tiene sentido.
- NavGraph y `SessionScreen` actualizados para no pasar el callback obsoleto.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `:feature:tables` + `:feature:draw` + `:app` — sin errores.

---

### Sprint 40 — Corrección de Packs de Tablas Aleatorias (18 de abril de 2026)

**FIX — Pack `tables_loot.json` vacío**
- El archivo `tables_loot.json` existía pero contenía únicamente arrays vacíos — cero tablas y cero entradas.
- Reescrito con **15 tablas completas** y **152 entradas** organizadas en 4 categorías:
  - *Botín*: CR 0-4, CR 5-10, CR 11-16, CR 17+, Gemas, Arte/Antigüedades, Bandidos, Criatura
  - *Magia*: Pociones, Pergaminos, Objetos Mágicos Menores/Moderados/Mayores
  - *Equipamiento*: Armamento Especial
  - *Comercio*: Mercancías Valiosas
- Rango de IDs: tablas 20000–20014, entradas 20000–21409 (sin colisión con otros packs).

**FIX — `subTableId` siempre nulo en `importStarterPack`**
- Bug en `ReferenceRepositoryImpl`: al insertar entradas de tablas aleatorias desde un pack, `subTableId` se fijaba en `null` hardcodeado, ignorando el valor `dto.subTableId`.
- Corregido: ahora se remapea correctamente usando `randomTableIdMap[dto.subTableId]`, preservando las referencias entre sub-tablas dentro del mismo pack.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `:core:data:compileDebugKotlin` — Corrección compilada sin errores.

---

### Sprint 39 — Starter Packs Dinámicos y Resiliencia de Datos (18 de abril de 2026)

**CONTENT — Sistema de Starter Packs**
- **NEW**: Implementado sistema de **Ecosistema de Paquetes**. La app ahora escanea dinámicamente la carpeta `assets/starter_packs` para detectar nuevos contenidos JSON sin necesidad de actualizaciones de código.
- **DATA**: Implementada la **Migración 27 a 28**, añadiendo la columna `sourcePack` a tablas de referencia, reglas y tablas aleatorias para rastrear el origen del contenido.
- **INTELLIGENCE**: Lógica de "Instalación Inteligente" que evita duplicados. El sistema ahora "adopta" y actualiza registros antiguos (huérfanos) basándose en el nombre, unificando la base de datos automáticamente.
- **UX**: Nuevo diálogo de gestión de packs con estados visuales (Instalado/Disponible) y opciones para **Actualizar** o **Quitar** paquetes específicos.
- **EXPANSION**: Creados y expandidos packs exhaustivos para **D&D 5e (2024)** (Maestrías, Estados, Visión) y **Twilight 2000 4th Ed** (Tablas aleatorias de Clima y Encuentros, reglas de Stress).

**STABILITY — Resiliencia y Parches**
- **FIX**: Implementada lógica de **Autocuración de Esquema** en la Migración 28. Detecta y recrea automáticamente la tabla `npcs` si se encuentra en un estado inconsistente (`columns={}`), resolviendo fallos críticos de Room.
- **BACKUP**: `BackupDto` actualizado con valores por defecto y soporte para `sourcePack`, mejorando la compatibilidad con archivos JSON externos simplificados.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — Verificada la integridad de la migración v28 y el motor de escaneo dinámico de assets.

---

### Sprint 38 — Finalización Módulo de Referencias (18 de abril de 2026)

**REFERENCE — Tablas de Consulta y Reglas (Sprint 29-37)**
- **NEW**: Implementado módulo `:feature:reference` para centralizar tablas de reglas, equipo y mecánicas.
- **NEW**: Motor de importación tri-modal: **CSV/TSV**, **Markdown** y **OCR inteligente vía Gemini Vision**.
- **UX**: Grid de consulta interactivo con soporte para `CellExpandDialog` para lectura cómoda de celdas extensas.
- **INTEGRATION**: Integración profunda en `SessionScreen` con un nuevo tab de Referencias pre-filtrado por los sistemas de juego de la sesión activa.
- **CONFIG**: Implementado `GameSystemsSelector` y `SessionConfigSheet` para gestionar dinámicamente los sistemas (D&D 5e, Pathfinder 2e, etc) durante la partida.

**BACKUP — Integración de Referencias**
- **UPDATE**: `FullBackupDto` extendido para incluir todas las tablas de referencia y reglas del sistema.
- **DATA**: Implementada la migración de base de datos **26 a 27** con soporte para FTS4 en tablas de referencia y reglas para búsquedas ultra-rápidas.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — Verificada la integridad de los nuevos flujos de importación y la consistencia del backup.

---

### Sprint 28 — Seguridad Total: Backup & Restore en la Nube (17 de abril de 2026)

**BACKUP — Arquitectura de Persistencia (Portable)**
- **NEW**: Implementado motor de serialización usando `kotlinx.serialization`. Convierte toda la base de datos (mazos, tablas, encuentros, notas) en un archivo JSON agnóstico del schema interno de Room.
- **NEW**: Implementado `BackupRepository` con soporte para transacciones masivas (`withTransaction`), asegurando que la restauración sea atómica y segura.
- **NEW**: `FileRepository` extendido para manejar empaquetado/desempaquetado de ZIPs de biblioteca completa, incluyendo rutas relativas para todas las imágenes.

**UI — Gestión de Biblioteca (Settings)**
- **NEW**: Añadida sección "Copia de Seguridad" en Ajustes.
- **CLOUD**: Integración con Android SAF (Storage Access Framework). El DM ahora puede guardar su backup directamente en **Google Drive, Dropbox o Almacenamiento Local**.
- **SAFETY**: Implementado diálogo de confirmación destructiva antes de restaurar, alertando al usuario sobre el reemplazo total de datos.
- **PROGRESS**: Feedback visual mediante `CircularProgressIndicator` y Snackbars informativos durante todo el proceso de empaquetado y restauración.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — Verificada la integridad de la base de datos tras la inyección del nuevo `BackupDao`.

---

### Sprint 27 — Refactor Core: Importación Robusta y Motor de Combate (17 de abril de 2026)

**IMPORT — Arquitectura y Resiliencia (Sprint 1-3)**
- **NEW**: Implementado `PdfLayoutProcessor` para desacoplar la lógica de extracción de PDFs del UseCase principal. Optimizado para ejecución en `Dispatchers.IO`.
- **ARCHITECTURE**: Centralización total de enums (`DeckImportSource`, `TableImportSource`, `PdfLayoutMode`) en `core:model` para eliminar dependencias circulares y comparaciones de strings.
- **SAFETY**: Implementado **Rollback Automático** en `ImportDeckUseCase`. En caso de fallo, se eliminan los registros parciales de la DB y las imágenes físicas para evitar "datos basura".
- **INTELLIGENCE**: Refactorizado `FilenameParser` para ignorar ruido de escaneo ("Scan_", "IMG_", "Copia de") y soportar múltiples delimitadores (_, -, espacios) con normalización a *Title Case*.
- **VALIDATION**: Añadida validación proactiva de manifiesto en importación de ZIPs para asegurar la integridad antes de procesar.

**ENCOUNTERS — Motor de Combate Determinista (Sprint 1-2)**
- **NEW**: Implementado `CalculateInitiativeOrderUseCase`. Establece un orden de combate inmutable basado en reglas TTRPG (Iniciativa > Bono > Nombre), eliminando saltos visuales en el tracker.
- **LOGIC**: Refactorizado `NextTurnUseCase` para usar el `sortOrder` persistido. Añadido clamping de seguridad para el índice de turno ante borrados accidentales de criaturas.
- **NARRATIVE**: Implementado `ToggleConditionUseCase` con registro automático en el `CombatLog` usando lenguaje natural (ej: "Orco ahora está Aturdido").
- **AUTOMATION**: `ApplyDamageUseCase` ahora automatiza la narrativa del log, registrando caídas inconscientes y recuperaciones de consciencia de forma enriquecida.
- **CLEANUP**: Implementado `CleanupCombatUseCase` para eliminaciones seguras de participantes con recalculo inmediato del orden de iniciativa.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 515 tareas. Verificada la integridad de los nuevos UseCases y su correcta inyección en `SessionViewModel`.

---

### Sprint 26 — Configuración Centralizada, Estabilidad Visual y Reordenamiento (17 de abril de 2026)

**UX/UI — Reordenamiento Persistente (C-1 Extension)**
- **NEW**: Implementado sistema de **Drag & Drop** para la Biblioteca de Mazos y la Biblioteca de Tablas.
- **PERSISTENCE**: Añadido campo `sortOrder` a la base de datos (Entidades `CardStack` y `RandomTable`).
- **MIGRATION**: Implementada la `MIGRATION_25_26` para añadir soporte de ordenamiento sin pérdida de datos.
- **UX**: Integrado `sh.calvin.reorderable` con feedback háptico y visual (elevación 8dp) para una experiencia premium.
- **CONTROL**: Añadido modo "Ordenar" en TopAppBar para evitar movimientos accidentales durante la navegación.

### C-1 — Reordenar recursos y cartas (drag & drop) 🔴 ★★
Actualmente las cartas se muestran en orden `sortOrder`.

**Implementación:** `LazyVerticalGrid` con `sh.calvin.reorderable`.

**Estado:**
- [x] **Biblioteca (Mazos y Tablas)**: Implementado reordenamiento persistente (Sprint 26).
- [ ] **DeckDetail (Cartas individuales)**: Pendiente (Sprint 27).

**UX/UI — Centro de Mando del Mazo (C-2)**
- **NEW**: Implementada la `DeckConfigSheet` centralizada. Consolidada toda la configuración (Metadatos, Visuales, Reglas y Tags) en un panel de control único y premium.
- **FEATURE**: Edición en tiempo real de **Nombre** y **Descripción** del mazo con persistencia inmediata en `CardRepository`.
- **VISUAL**: Integrado el selector de **Imagen de Portada** (Cover) para personalizar la apariencia en la biblioteca.
- **CLEANUP**: Eliminado el diálogo `AddTagDialog` redundante; la gestión de etiquetas ahora es parte integral de la configuración centralizada mediante `FlowRow` y chips interactivos.
- **SAFETY**: Añadida la "Zona de Peligro" con acciones claras de **Archivar** y **Eliminar Mazos** (con diálogo de confirmación).

**VISUAL — Refactor 3D de Mecánica Boca Abajo (B-2/B-3)**
- **NEW**: Implementada rotación 3D real (`graphicsLayer`) con sombreado dinámico basado en el ángulo de giro.
- **ANIMATION**: Migrado a `Animatable` con **Spring** (fricción ajustada) para una respuesta táctil elástica y natural.
- **UX**: Las cartas robadas boca abajo ahora muestran el reverso del mazo correspondiente, manteniendo la intriga para el DM.

**FIX — Estabilización de Importación Markdown**
- **PARSER**: Mejorado el `MarkdownTableParser` con una regex robusta para soportar delimitadores complejos, espacios variables y múltiples columnas sin pérdida de datos.
- **BUG**: Corregido mapeo en `TableImportViewModel` que ignoraba el modo de importación MARKDOWN, permitiendo ahora el flujo completo desde archivos `.md`.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `:feature:deck:compileDebugKotlin` — Verificada integridad de mappers, inyección de dependencias y recursos visuales.

---

### Sprint 18 — Biblioteca de NPCs y Criaturas (17 de abril de 2026)

**ARCHITECTURE — Nuevo Módulo `:feature:npcs`**
- **NEW**: Implementado el módulo independiente `:feature:npcs` para la gestión de la biblioteca persistente.
- **DATA**: Migración de base de datos a la versión **24**. Añadidas tablas `npcs` y `npc_tags`.
- **DATA**: `EncounterCreatureEntity` extendido con `npcId` e `imagePath` para permitir la vinculación con la biblioteca.
- **LOGIC**: Implementados use cases para CRUD de NPCs y guardado local de avatares en `/npcs/`.

**FEATURE — Biblioteca de NPCs Premium**
- **UI**: `NpcListScreen` — Vista de grid visual con previsualización de stats (HP, AC, Iniciativa) y badges de avatars.
- **UI**: `NpcEditorScreen` — Editor completo con selector de imágenes, campos numéricos y soporte **Markdown** para lore y notas.
- **UX**: Sistema de filtrado por Tags para organizar criaturas por tipo, entorno o alineamiento.

**INTEGRATION — Flujo de Trabajo del DM**
- **NEW**: Integrado selector de NPCs en el `EncounterEditorScreen`. Permite añadir múltiples criaturas a un encuentro usando NPCs de la biblioteca como plantillas.
- **UI**: El `CombatTab` ahora muestra los **Avatares** de las criaturas vinculadas, mejorando drásticamente el feedback visual durante el rastreo de iniciativa.
- **INTEGRATION**: Registro en el `NavGraph` principal y acceso desde el menú de la `LibraryScreen`.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 337 tareas. Verificada integridad de mappers, migraciones y Hilt/Dagger.

---

### Sprint 17 — Combat Tracker e Integración de Encuentros (17 de abril de 2026)

**FEATURE — Combat Tracker Premium**
- **NEW**: Implementado `CombatTab` dinámico en `SessionScreen`. Solo aparece cuando hay un encuentro activo.
- **NEW**: Soporte para **Participantes Temporales (PJs)**. El DM puede añadir jugadores a la iniciativa con un solo tap sin persistirlos en la biblioteca de encuentros.
- **UX**: Visualización "Bloodied" (Sangriento) automática cuando una criatura cae por debajo del 50% de HP.
- **UX**: Animaciones de reordenamiento automático en la lista de iniciativa (`animateItem`) y escala visual para el turno activo.
- **LOGIC**: Mejorado `NextTurnUseCase` (ahora en `SessionViewModel`) para ciclar correctamente entre criaturas y jugadores temporales.

**INTEGRATION — Automatización de Notas**
- **NEW**: Al finalizar un combate, se genera un resumen detallado (Rondas, Supervivientes) que se anexa automáticamente a las Notas del DM de la sesión.
- **LOGIC**: `ApplyDamageUseCase` gestiona automáticamente la condición `UNCONSCIOUS` al llegar a 0 HP y la retira al curar.

---

### Sprint 16 — Notas por carta y Portabilidad (ZIP Export v2) (17 de abril de 2026)

**ARCHITECTURE — Componentes Globales**
- **MOVE**: Refactorizado `MarkdownToolbar` y movido a `:core:ui`.
- **FEATURE**: Notas por carta completas con autoguardado y vista previa.
- **INTEGRATION**: Exportación e Importación enriquecida mediante `deck_manifest.json`.

---

### Sprint 15 — Notas de DM: Editor Markdown y UX de Guardado (17 de abril de 2026)

**FEATURE — Editor de Notas Pro**
- **NEW**: Implementada `MarkdownToolbar` con acciones rápidas: Bold, Italic, Link, Listas, Headers (H1/H2), Checkboxes y Código.
- **INTEGRATION**: `NotesTab` refactorizado para usar `TextFieldValue`. Esto permite insertar etiquetas Markdown exactamente en la posición del cursor o envolver el texto seleccionado.
- **UX**: Mejorada la jerarquía visual del editor con contenedores `Surface` redondeados, bordes suaves y tipografía optimizada.

**FEATURE — Feedback de Autoguardado**
- **UX**: Añadido indicador visual de "Guardando…" (CircularProgress + Texto) en la cabecera de la pestaña de notas.
- **UX**: Añadido icono de verificación (Check) una vez persistido el cambio para dar tranquilidad al DM.
- **STATE**: Nueva propiedad `isSavingNotes` en `SessionUiState` controlada desde el ViewModel durante la persistencia en `SessionRepository`.

**FEATURE — Pulido de Notas Rápidas (Quick Notes)**
- **UX**: Rediseñado el `QuickNoteDialog` con un look más premium y mejor feedback ("Se añadirá con un timestamp...").
- **UX**: Foco automático en el editor al abrir el diálogo para permitir escritura inmediata sin toques extra.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** — Verificado que el autoguardado con debounce (800ms) funciona correctamente con la nueva estructura de `TextFieldValue`.

---

### Sprint 24 — Gemini Vision Mode: Path Multimodal para Reconocimiento de Tablas (17 de abril de 2026)

**INTEGRATION — Gemini Vision Mode (Multimodal)**
- **NEW**: Añadido `recognizeTableFromImage(bitmap, apiKey)` a `AiTableRepository` y su implementación en `GeminiAiRepository`. Envía el bitmap recortado directamente a Gemini como entrada multimodal, bypassando ML Kit OCR por completo.
- **NEW**: `RecognizeTableFromImageUseCase` en `:core:domain` — orquesta la llamada al repositorio de IA con visión.
- **NEW**: `TableImportViewModel.recognizeWithVision()` — invoca el use case, popula las entradas y navega al paso MAPPING directamente desde RECOGNITION.
- **UI**: Paso RECOGNITION ahora ofrece dos botones: "OCR" (flujo existente) e "IA Vision" (nuevo path). El botón Vision muestra `CircularProgressIndicator` durante el procesamiento.
- **STATE**: Añadido `isVisionProcessing: Boolean` a `TableImportUiState`.

**UPDATE — Modelo Gemini actualizado a 2.0 Flash**
- Cambiado `gemini-1.5-flash-latest` → `gemini-2.0-flash` en `GeminiAiRepository` para ambos endpoints (texto e imagen).
- `apiVersion` actualizado a `v1beta` (requerido por Gemini 2.0).
- Extraídas constantes `MODEL_NAME` y `API_VERSION` en companion object.
- Refactorizado el parsing de respuesta a función privada `parseResponse()` para eliminar duplicación.

**ARCHITECTURE — Backward compatible**
- El path OCR original no fue modificado. Usuarios sin API key configurada ven el botón Vision pero el error se gestiona mediante el diálogo de error existente.

**INTEGRATION — Build**
- Pendiente verificación con `assembleDebug`.

---

### Sprint 23 — Importación Inteligente: Markdown y Gemini AI (17 de abril de 2026)

**FEATURE — Soporte Nativo de Tablas Markdown (`.md`)**
- **NEW**: Implementado `MarkdownTableParser` para reconocer la sintaxis estándar de tablas `| Header |`.
- **INTEGRATION**: El selector de archivos ahora permite elegir archivos `.md` y los procesa extrayendo las filas de datos ignorando los delimitadores de formato.

**FEATURE — Transcripción Inteligente con Gemini AI**
- **INTEGRATION**: Añadido el SDK de Google AI (`generativeai`).
- **NEW**: Botón "Varita Mágica" en `TableReviewView` que envía el texto (especialmente tras OCR) a Gemini 1.5 Flash.
- **AI**: La IA reconstruye y limpia los datos de la tabla, corrigiendo errores de OCR y re-estructurando columnas desordenadas en un formato JSON estructurado.
- **CONFIG**: Implementada la gestión segura de la API Key en la pantalla de Ajustes, persistida localmente.

**ARCHITECTURE — Settings & Configuration**
- **NEW**: `SettingsRepository` centraliza la gestión de la API Key de Gemini y la calidad de imagen JPEG.
- **FIX**: Resincronizados los ViewModels para usar el nuevo repositorio en lugar de acceso directo a SharedPreferences.

**FIX — Limpieza de Warnings y Estabilización**
- **DEPRECATION**: Reemplazados iconos `List` y `ViewList` por sus versiones `AutoMirrored` para compatibilidad futura y RTL.
- **OPT-IN**: Añadido `@OptIn(FlowPreview::class)` en `LibraryViewModel` para silenciar avisos del operador `debounce`.
- **KAPT**: Configurado `kapt.languageVersion = 1.9` en `gradle.properties` para resolver el aviso de fallback en Kotlin 2.0.
- **BUILD**: Corregidos errores de referencias no resueltas e inyección de dependencias en `TableImportViewModel`.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 313 tareas, sin warnings de código.

---

### Sprint 22 — Estabilización del Build y Refactor de Módulos (16 de abril de 2026)

**FIX — Solución a error de inyección de Dagger Hilt (Missing Binding)**
- **PROBLEM**: El build fallaba con un error de inyección en `TableRepositoryImpl` porque `TableBundleDao` no tenía un método `@Provides` en el módulo de Dagger.
- **SOLUTION**: Añadido `@Provides fun provideTableBundleDao(db: DeckAppDatabase)` a `DatabaseModule` en `DataModule.kt`.
- **FIX**: Detectada y corregida la omisión de `MIGRATION_19_20` en el `databaseBuilder` de Room, asegurando la consistencia entre la versión de la base de datos (20) y las migraciones registradas.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 445 tareas, 21 ejecutadas, sin errores.

---

### Sprint 22 — Estabilización del Build y Refactor de Módulos (16 de abril de 2026)

**ARCHITECTURE — Solución Crítica: KSP Internal Compiler Error**
- **PROBLEM**: El módulo `:feature:tables` sufría un error interno de KSP (NullPointerException) que bloqueaba completamente el build, sin causa aparente en el código fuente.
- **SOLUTION**: Migrado el procesamiento de Hilt en `:feature:tables` de **KSP a Kapt**. Aunque KSP es el estándar moderno, Kapt ofrece la estabilidad necesaria para este módulo específico mientras se investiga la causa raíz del fallo en el compilador.
- **CONSOLIDATION**: Fusionados `TableLibraryViewModel` y `TablesViewModel` en una única unidad lógica. Eliminado `TableImportViewModel` del módulo `:feature:tables` para reducir la complejidad y los puntos de fallo de inyección.

**FIX — Solución al Crash de Inicio (Database Schema Mismatch)**
- **PROBLEM**: El app crasheaba instantáneamente al arrancar debido a un fallo en la validación de Room.
- **CAUSE**: La migración 18→19 creaba un índice para la tabla `recent_files`, pero la entidad `@Entity RecentFileRecord` no lo tenía definido en su código Kotlin.
- **SOLUTION**: Añadido `indices = [Index(value = ["uri"], unique = true)]` a `RecentFileRecord`. Alineados todos los índices manuales con las definiciones de las entidades de Room para asegurar la integridad del esquema.

**FIX — Solución al Crash en Importación (SecurityException)**
- **PROBLEM**: Crash al seleccionar un PDF desde la navegación por carpetas (`No persistable permission grants found`).
- **CAUSE**: Se intentaba llamar a `takePersistableUriPermission` sobre un URI de documento hijo que ya heredaba permisos del árbol (tree URI) de la carpeta madre, lo cual no es permitido por Android.
- **SOLUTION**: Implementado un bloque `try-catch` en `ImportViewModel.onPdfSelected` para manejar de forma segura los permisos de URIs que ya poseen acceso delegado.

**UX/OCR — Mejoras en el Reconocimiento de Tablas**
- **FIX**: Eliminado el residuo de caracteres separadores (como puntos, dos puntos o guiones) que el OCR mantenía en el texto de las filas tras extraer el número de el rango. Mejorada la robustez de la limpieza en `AnalyzeTableImageUseCase`.

**BUILD — Soporte de Alineación 16 KB (Android 15+)**
- **FIX**: Resuelta la advertencia de `Android 16 KB Alignment` en `libmlkit_google_ocr_pipeline.so`.
- **UPDATE**: ML Kit Text Recognition actualizado a `16.0.1`.
- **WORKAROUND**: Añadida dependencia `androidx.camera:camera-core:1.4.2` para forzar utilidades nativas alineadas.
- **CONFIG**: Habilitado `useLegacyPackaging = true` en el bloque de `packaging` para asegurar la correcta alineación de librerías JNI.

**FEAT — Importación Proactiva de Tablas**
- **NEW**: Implementada la navegación por el dispositivo para importación de tablas.
- **NEW**: Soporte para miniaturas de PDFs en el listado de archivos encontrados tras seleccionar una carpeta (Tree URI).
- **UX**: Conectada la interfaz de `SourceSelectionView` con `ActivityResultLauncher` para una experiencia nativa y fluida.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 325 tareas, sin errores.

---

### Sprint 21 — Gestión de Recursos y UX de Nombramiento (15 de abril de 2026)

**FEATURE — Filtrado de Tablas por Sesión**
- `TablesViewModel` ahora soporta `setSession(id)`. Si hay una sesión activa, el tab de tablas solo muestra las tablas "asignadas" por defecto.
- Añadido un interruptor ("Mostrando todas") para navegar rápidamente por la biblioteca completa sin salir del contexto de la sesión.

**UI/UX — Renombrado Prominente en Importación**
- **PROBLEM**: Los usuarios a veces olvidaban cambiar el nombre predeterminado al importar recursos nuevos.
- **SOLUTION**: Los campos de Nombre (Mazos/Tablas) se han movido a la parte superior del paso final de configuración, envuelvos en una `Surface` resaltada (primaryContainer) para asegurar su visibilidad inmediata.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 462 tareas, sin errores.

---

**ARCHITECTURE — Refactor de MazosTab → DeckWorkspace**
- **PROBLEM**: Con múltiples mazos activos, la mano era una única fila horizontal de scroll infinito sin distinción visual entre recursos de diferente origen.
- **SOLUTION**: Reemplazado `MazosTab` (scroll horizontal) por `DeckWorkspace` (lista vertical de clusters), donde cada mazo de la sesión tiene su propia caja Bento colapsable.
- **IMPACT**: Eliminado el scroll horizontal; las cartas se organizan en `FlowRow` que envuelve automáticamente a múltiples filas.

**FEATURE — DeckClusterItem**
- Nuevo componente `DeckClusterItem`: header colapsable (nombre del mazo + "N en mano · M disponibles" + icono ExpandMore/Less), body animado con `AnimatedVisibility`.
- Cuando el cluster no tiene cartas: slot vacío con call-to-action "Selecciona y pulsa ROBAR" que activa el mazo al tocarlo, eliminando la necesidad de la `DeckBar` separada.

**FEATURE — CompactCardItem**
- Nueva variante de carta de 120dp (vs 160dp en SwipeToDiscardCard) para acomodar más cartas por pantalla sin scroll.
- Mantiene swipe-to-discard, tap para detalle, alpha de feedback de arrastre, e indicador de tabla enlazada.

**ARCHITECTURE — SessionViewModel / SessionUiState**
- Nuevos campos: `collapsedDeckIds: Set<Long>` (efímero, no persistido), `handByDeck: Map<Long, List<Card>>` (derivado reactivamente de `hand`).
- Nuevas funciones: `toggleDeckCollapse(stackId)`, `setLastInteractedDeck(stackId)`.

**INTEGRATION — Build**
- `@file:OptIn(ExperimentalLayoutApi::class)` para `FlowRow` (API experimental de Compose).
- **BUILD SUCCESSFUL** `assembleDebug` — 409 tareas, sin errores.

---

### Sprint 20 — Reconocimiento de Tablas: Parsers y Pulido (15 de abril de 2026)

**FEATURE — Multi-columna configurable en `CsvTableParser`**
- Nuevo campo `additionalColumns: List<Int>` en `ParseConfig`. Las columnas adicionales se concatenan al texto principal con ` — `.
- `ParsePreview` expone `columnCount` para que la UI pueda renderizar un selector de columnas.

**FIX — `inferRollFormula` detecta 2d6, 3d6 y otras combinaciones multi-dado**
- Nueva sobrecarga `inferRollFormula(minValue, maxValue)` que reconoce 2d4 (2–8), 2d6 (2–12), 2d10 (2–20) y 3d6 (3–18).
- `ImportTableUseCase.inferFormula()` ahora pasa tanto min como max.
- La sobrecarga de 1 argumento se mantiene por compatibilidad (asume min=1).

**FEATURE — Soporte formato Roll20 en `JsonTableParser`**
- Detecta objetos con campo `"items"` (formato `RollableTable` de Roll20).
- Convierte `weight` en rangos acumulativos: item de weight=3 ocupa 3 resultados consecutivos.

**ARCHITECTURE — `TableParseException` sellada**
- Nuevo archivo `TableParseException.kt` en `:core:domain` con casos: `InvalidJson`, `UnknownFormat`, `EmptyResult`.
- `JsonTableParser` relanza `InvalidJson` y `UnknownFormat` en lugar de `IllegalArgumentException`.
- `ImportTableUseCase` lanza `EmptyResult` cuando JSON o texto plano no producen entradas.

**FIX — Gutters con umbral de votos adaptativo**
- Tablas con menos de 10 líneas usan `minVotes = 1` en `detectColumns`. Las tablas grandes mantienen el 15%.
- Evita descartar columnas opcionales en tablas pequeñas con datos dispersos.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 443 tareas, sin errores.

---

### Sprint 19 — Reconocimiento de Tablas: Robustez del Pipeline (15 de abril de 2026)

**FIX — `groupIntoLines` por centro de línea acumulado**
- **PROBLEM**: La tolerancia usaba la altura del bloque *anterior*, no de la línea en construcción. Un bloque con altura atípica (ej. un dígito grande) desplazaba el threshold para todos los siguientes.
- **SOLUTION**: Se mantienen `lineCenterY` y `lineAvgHeight` como medias incrementales. Cada bloque nuevo se compara contra el centro actual de la línea, no contra el último bloque visto.

**FIX — Modo percentil en `splitLinesIntoTables`**
- Pre-escaneo de todas las líneas para detectar si `maxRoll >= 90` (tabla 01–100).
- En modo percentil, `resetThreshold = 10` en lugar de 2 para evitar partir en la transición "91-00" → "1".

**FEATURE — DPI adaptativo en `loadPdfPage`**
- `renderPageAdaptiveDpi()`: primer render a 800px, mide altura promedio de bloques OCR, luego elige resolución final: 1800px (texto denso), 1200px (normal), 900px (texto grande).
- Evita cargar bitmaps de alta resolución innecesarios en tablas con tipografía grande.

**ARCHITECTURE — `OcrRepositoryImpl` a `@ActivityRetainedScoped`**
- Cambiado de `@Singleton` a `@ActivityRetainedScoped` para liberar el reconocedor ML Kit al salir del flujo de importación.
- **FIX de build**: Extraído a `OcrModule` propio con `@InstallIn(ActivityRetainedComponent::class)`, ya que el scope no es compatible con `SingletonComponent`.

**FEATURE — Stitching con dos modos (`StitchingMode`)**
- Nuevo enum `StitchingMode`: `CONTINUE_RANGES` (desplaza rolls para continuar la secuencia) y `APPEND` (respeta rolls del OCR, solo ajusta `sortOrder`).
- `updateStitchingMode()` expuesto en el ViewModel para que la UI ofrezca el toggle al activar stitching.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 443 tareas, sin errores.

---

### Sprint 18 — Reconocimiento de Tablas: Heurísticas, Confianza y Parsers (15 de abril de 2026)

**FEATURE — Confianza OCR propagada hasta el UiState**
- `buildEntries` calcula `minConfidence` por fila (mínimo de los bloques que la componen).
- `AnalysisResult.lowConfidenceIndices` → `ImportResult.lowConfidenceIndices` → `TableImportUiState.lowConfidenceIndices`.
- Umbral: `CONFIDENCE_THRESHOLD = 0.6f` en `AnalyzeTableImageUseCase`. La UI de revisión puede resaltar estas entradas (implementación visual pendiente).

**FIX — Clustering con promedios globales**
- `clusterBlocks` ahora calcula `avgWidth` y `avgHeight` sobre todos los bloques antes del BFS. Elimina el problema donde un bloque pequeño (ej. "1") generaba un threshold mínimo, impidiendo conectar bloques cercanos.

**FIX — `suggestTablePoints` excluye encabezados y pies de página**
- Filtro: bloques en el 5% superior/inferior de la imagen y textos de ≤3 caracteres se ignoran al calcular el bounding box sugerido.

**FIX — Bug crítico: `expectedTableCount=1` partía la tabla en múltiples**
- **PROBLEM**: `splitLinesIntoTables` aplicaba la heurística de reinicio incluso cuando el usuario declaraba explícitamente 1 tabla.
- **SOLUTION**: Si `expectedCount == 1`, la función devuelve todas las líneas como un único grupo sin evaluar reinicios.

**FEATURE — Detección de formato CSV/JSON por contenido**
- `onFileSelected` asigna `ImportMode.NONE` cuando la extensión no es `.csv`/`.json`.
- `loadFileText` resuelve el modo tras leer el contenido: detecta JSON por prefijo `{`/`[`, CSV por presencia de delimitadores.

**FIX — ENUM_PATTERN ampliado en PlainTextTableParser**
- Cubre ahora `[1-5] texto`, tabulador como separador, y espacio simple sin símbolo de puntuación.

**FIX — Header detection mejorada en CsvTableParser**
- Reconoce keywords TTRPG ("roll", "result", "rango", "encounter", etc.) como señal positiva de cabecera.
- Valida con `RangeParser` antes de aplicar la heurística de dígitos, evitando clasificar `"1d6"` como dato.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 443 tareas, 28 ejecutadas, sin errores.

---

### Sprint 17 — Reconocimiento de Tablas: Fundamentos del Pipeline OCR (15 de abril de 2026)

**ARCHITECTURE — Jerarquía OCR: TextLine como unidad base**
- **PROBLEM**: `OcrRepositoryImpl` iteraba `TextBlock` y concatenaba líneas con `\n`, forzando a `AnalyzeTableImageUseCase` a re-separar lo que ML Kit ya tenía separado con `splitMultiLineBlocks`.
- **SOLUTION**: Refactorizado `OcrRepositoryImpl` para iterar `block.lines` (nivel `TextLine`). Cada línea produce un `OcrBlock` independiente con bounding box propio.
- **IMPACT**: `splitMultiLineBlocks` pasa a ser safety net en lugar de paso crítico. Bounding boxes más precisos mejoran `groupIntoLines` y `detectColumns`.

**ARCHITECTURE — `OcrBlock.confidence` real + `RectModel` Float**
- `confidence` ahora es el promedio de `TextElement.confidence` por línea en vez de `1.0f` hardcodeado. Habilita filtrado de entradas inciertas en sprints futuros.
- `RectModel` migrado de `Int` a `Float` para preservar precisión al escalar bitmaps. Eliminadas conversiones `.toFloat()` redundantes en `detectColumns`.

**ARCHITECTURE — `OcrRepository` devuelve `Result<List<OcrBlock>>`**
- **PROBLEM**: Fallo silencioso: `catch (e: Exception) { emptyList() }` hacía indistinguible un error de ML Kit de una imagen sin texto.
- **SOLUTION**: Firma cambiada a `Result<List<OcrBlock>>`. Nuevo `OcrException` en `:core:domain`. `ImportTableUseCase.getRawBlocks` usa `getOrThrow()` — el ViewModel ya tenía `try-catch` y distingue el tipo de error.

**FEATURE — Filtro de ruido de separadores**
- `AnalyzeTableImageUseCase` descarta bloques cuyo texto es solo guiones, barras o subrayados (`"———"`, `"_____"`, `"| | |"`). Estos son líneas de tabla que ML Kit convierte en texto y rompían el clustering.

**FIX — Rangos percentiles `00 = 100`**
- `RangeParser` ahora normaliza `"00"` → `100`. `"96-00"` se parsea como `ParsedRange(96, 100)`. Convención estándar en tablas OSR y D&D Basic.

**FIX — BOM UTF-8 en CSV de Excel**
- `CsvTableParser.preview()` y `parse()` aplican `trimStart('\uFEFF')`. El primer campo ya no aparece como `"﻿Roll"` en archivos exportados desde Excel.

---

### Sprint 11 — OCR Avanzado: Segmentación Multilínea y Control por Tabla (15 de abril de 2026)

**FEATURE — Motor OCR Mejorado (`AnalyzeTableImageUseCase`)**
- Implementada `splitMultiLineBlocks`: divide bloques OCR que fusionan filas múltiples (común en layouts con zebra-striping), recuperando números de Roll ocultos.
- Parámetro `expectedTableCount` para que el usuario indique cuántas tablas esperar, ajustando la agresividad del clustering espacial.

**UI/UX — Edición Manual en ReviewStep**
- `BasicTextField` editable para `minRoll` / `maxRoll` directamente en la lista de entradas.
- Botones `ExpandLess`/`ExpandMore` en `EntryRow` para reordenar filas manualmente tras la detección.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — Sin regresión en flujos existentes de CSV, JSON y Texto.

---

### Sprint 16 — Estabilización Crítica: Persistencia y Motor de PDF (15 de abril de 2026)


**ARCHITECTURE — Migración a PdfRenderer Nativo**
- **PROBLEM**: El uso de `pdfium-android` causaba `NoClassDefFoundError: ArrayMap` en dispositivos modernos debido a dependencias obsoletas de Android Support superpuestas con AndroidX.
- **SOLUTION**: Refactorizado `FileRepositoryImpl` para utilizar la API nativa `android.graphics.pdf.PdfRenderer`.
- **IMPACT**: Eliminación de dependencias externas pesadas (`pdfium`, `legacy-support`) y mejora de la estabilidad en los flujos de importación de PDF.

**ARCHITECTURE — Consolidación de Integridad Referencial (MIGRATION 11→12→13)**
- **PROBLEM**: Crone de `SQLiteConstraintException` y fallo de arranque `IllegalStateException` indicando que faltaba una ruta de migración de la versión 11 a la 13.
- **SOLUTION**: Actualizado el esquema Room (versión 13) para incluir `@ForeignKey` con `onDelete = CASCADE` en todas las tablas dependientes.
- **FIX**: Corregida la omisión de `MIGRATION_11_12` y `MIGRATION_12_13` en el `Room.databaseBuilder` dentro de `DataModule.kt`.
- **FIX**: Corregida discrepancia de esquema en `session_deck_refs` (índice faltante) y typo `AUTOINT` en scripts de migración previos.

**INTEGRATION — Build y Calidad**
- **PENDING**: Verificación de flujos de importación de PDF con el nuevo motor nativo y validación de borrado masivo de recursos.

---

### Sprint 11 — Detección de Múltiples Tablas e Importación por Lotes (15 de abril de 2026)

**FEATURE — Clustering Espacial en OCR**
- Implementada la lógica de **clustering espacial** en `AnalyzeTableImageUseCase` para separar múltiples tablas en una misma imagen/página.
- El sistema utiliza un algoritmo de componentes conectados para agrupar bloques de texto próximos, permitiendo detectar tablas independientes.
- Añadida heurística para la **detección automática de títulos** de tablas basada en el contexto del cluster.

**UI/UX — Review Multi-tabla**
- Rediseñada la pantalla `TableImportScreen` para incluir un **selector de pestañas** (`ScrollableTabRow`) cuando se detectan múltiples tablas.
- Permite al usuario revisar, editar y guardar cada tabla de forma independiente dentro del mismo flujo de importación.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — Verificada la compatibilidad de la nueva lógica de OCR con los flujos existentes de CSV y Texto.

---

### Sprint 10 — Estabilización y Gestión de Deuda Técnica (15 de abril de 2026)

**PROBLEM/SOLUTION — Bloqueos de Base de Datos y PDF**
- **DB (MIGRATION 10→11)**: Corregid el fallo de arranque por índices faltantes en `session_deck_refs` y `session_table_refs`. Implementada migración manual para preservar datos.
- **PDFium (AndroidX)**: Solucionado el error `NoClassDefFoundError: ArrayMap` mediante la inclusión de `androidx.legacy:legacy-support-v4` como puente de compatibilidad.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** — El proyecto vuelve a ser completamente estable.

---

### Sprint 9 — Integración de Cartas y Tablas (15 de abril de 2026)

**ARCHITECTURE — Vínculo de Dominio y Persistencia (MIGRATION 8→9)**
- Implementada la migración de base de datos **8 a 9** para añadir la columna `linkedTableId` a la tabla `cards`.
- Actualizado el modelo de dominio `Card` y los `Mappers` para soportar la asociación opcional con tablas aleatorias.
- Incrementada la versión de la DB a **9** y registrada la migración en `DataModule`.

**FEATURE — Selector de Tablas en Editor de Cartas**
- Añadida la sección **"Acción Vinculada"** en `CardEditorScreen`.
- Implementado un selector modal que carga dinámicamente todas las tablas disponibles de la biblioteca.
- Permite vincular, cambiar o eliminar la asociación de una tabla con la carta de forma persistente.

**FEATURE — Acción Rápida en Sesión (Direct Roll)**
- Las cartas en la `SessionScreen` ahora detectan si tienen una tabla vinculada.
- Añadido un **trigger visual** (icono de dado) en la esquina superior de la carta (cuando está revelada).
- Al pulsar el icono, se dispara `tablesViewModel.rollTable(tableId)`, mostrando el resultado instantáneamente en el panel de detalles.
- Solucionado el problema de *Smart Cast* en propiedades multi-módulo mediante captura en variables locales.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — Verificada la compatibilidad de las migraciones y la integridad de los nuevos componentes UI.
- Actualizada la documentación funcional en `walkthrough.md`.

---

### Sprint 8 — Sistema de Tablas Aleatorias (Fase Avanzada) (15 de abril de 2026)

**ARCHITECTURE — Corrección de Perspectiva (4 Puntos)**
- Implementada la lógica de **Homografía** usando `android.graphics.Matrix.setPolyToPoly`.
- Permite transformar cuadriláteros irregulares (fotos de manuales físicos) en bitmaps rectulares planos para un OCR óptimo.
- UI: Reemplazado `cropRect` por lista de 4 puntos (`Offset`) arrastrables de forma independiente con líneas conectoras y área de recorte visual.

**FEATURE — Soporte Multipage (Stitching) y Flujo Continuo**
- Añadido `isStitchingMode` a `TableImportViewModel`.
- Botón "Añadir página" permite concatenar resultados de diferentes recortes o páginas en una sola tabla lógica antes de guardar.
- El sistema ajusta automáticamente el `sortOrder` y la integridad de los dados al añadir nuevas filas.

**FEATURE — Detección Automática de Columnas y Heurística de Datos**
- El motor de análisis detecta la estructura horizontal de la tabla buscando alineaciones de bloques (gutters) en el eje X.
- Separación automática de múltiples columnas usando el delimitador `|` (ej: `1-10 | Poción | 50 gp`).
- Merge multilínea inteligente: las descripciones largas se unen solo a su columna correspondiente basándose en su posición horizontal.

**FEATURE — Auto-detección de Bordes (Sugerencia OCR)**
- Escaneo silencioso al abrir una página: utiliza los resultados de OCR iniciales para calcular el *bounding box* del contenido textual.
- La UI posiciona automáticamente las 4 esquinas rodeando la tabla detectada para minimizar el ajuste manual.

**INTEGRATION — Estabilidad y Verificación**
- **BUILD SUCCESSFUL** — verificado post-implementación de todas las heurísticas avanzadas.
- Actualizada documentación técnica en `walkthrough.md`.

---

### Sprint 7 — Completar Sistema de Tablas (15 de abril de 2026)

**FEATURE — FAB "TIRAR" real en SessionScreen (tab Tablas)**
- `TablesViewModel` inyectado en `SessionScreen` via `hiltViewModel()` — mismo NavBackStackEntry, misma instancia compartida con `TablesTab`.
- FAB página 2 (Tablas): si `activeTable != null` → tira esa tabla y el resultado aparece en `TableDetailSheet` ya abierto. Si no hay tabla activa → Snackbar "Abre una tabla para usar TIRAR".
- `SessionFab` actualizado: acepta `hasActiveTable`, muestra FAB atenuado (α 0.5) y sublabel "tabla activa" para feedback visual.

**FEATURE — Export de tablas a JSON**
- `ExportTableUseCase` (object en `:core:domain`): serializa `RandomTable` → formato DeckApp JSON v1 (compatible con `JsonTableParser`). Escapa correctamente `\`, `"`, `\n`.
- `TablesViewModel.getExportJson()`: construye JSON desde `activeTable` (ya en memoria con entries completos).
- `TableDetailSheet`: nuevo parámetro `onExport`, nuevo botón de Share en la cabecera.
- `TablesTab`: `rememberLauncherForActivityResult(CreateDocument)` → el usuario elige destino → se escribe el JSON via `ContentResolver`.

**FEATURE — WEIGHTED mode en TableEditorScreen**
- Campo "Fórmula" deshabilitado cuando `rollMode == WEIGHTED` (la fórmula se ignora en ese modo).
- Nota informativa: "Modo Peso: la fórmula se ignora. El resultado se elige por probabilidad relativa."
- `RollTableUseCase.pickEntry` ya manejaba WEIGHTED correctamente — solo faltaba el feedback visual en el editor.

---

### Sprint 6 — Sistema de Importación de Tablas (14 de abril de 2026)

**ARCHITECTURE — Motor de importación multi-fuente**
- `ImportTableUseCase` en `:core:domain` — orquestador universal: OCR, CSV, JSON, texto plano.
- `AnalyzeTableImageUseCase` — heurística de parsing OCR: agrupación por Y, detección de rango/texto, manejo de entradas multilínea.
- `RenderPdfPageUseCase` — envuelve `FileRepository.renderPdfPageToBitmap` para previsualización en el flujo de importación.
- `CsvTableParser` — auto-detección de delimitador, preview de columnas, `ParseConfig` para mapeo manual.
- `JsonTableParser` — compatibilidad con Foundry VTT `RollTable` y formato DeckApp JSON.
- `PlainTextTableParser` — detección de patrones `N. texto`, `- [rango] texto` y listas simples.
- `RangeParser` — motor centralizado: parseo, validación de integridad (huecos/solapamientos), inferencia de fórmula de dado.
- `ReadTextFromUriUseCase` — lectura de texto desde URI SAF vía `FileRepository.readTextFromUri`.

**FEATURE — TableImportScreen (flujo de 5 pasos)**
- Paso 1 (`SOURCE_SELECTION`): 4 cards de origen — Imagen, PDF, Archivo, Pegar texto.
- Paso 2 (`CROP`): canvas interactivo con rectángulo de recorte arrastrable sobre el PDF/imagen; navegación de páginas PDF.
- Paso 3 (`FILE_PREVIEW`): texto raw editable — continuar para CSV configura columnas, para texto plano analiza directamente.
- Paso 4 (`MAPPING`): selección de columna Rango / Texto para CSV con muestra visual de filas.
- Paso 5 (`REVIEW`): entradas editables inline, advertencias de huecos/solapamientos, nombre y categoría de tabla; checkmark en TopBar guarda en Room.

**FEATURE — TableImportViewModel**
- Flujo completo: selección → carga → análisis → revisión → guardado.
- `loadFileText` usa `ReadTextFromUriUseCase` para leer archivos CSV/JSON/TXT desde URI SAF.
- `saveTable()` construye `RandomTable` desde el estado de revisión y persiste via `TableRepository`.
- `savedSuccessfully: Boolean` en `TableImportUiState` — señal para que la pantalla navegue de vuelta.

**INTEGRATION — FileRepository**
- `readTextFromUri(uri: Uri): String` añadido a interfaz e implementado en `FileRepositoryImpl` via `ContentResolver`.

**INTEGRATION — NavGraph**
- `TableImportRoute` ya estaba registrado; `onImportFinished` ahora observado via `LaunchedEffect(savedSuccessfully)`.
- `BUILD SUCCESSFUL` — pendiente de verificación post-refactor.

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
- [x] **Sprint 7: Configuración de Mazos (2026-04-14)**
    - [x] Implementado `DeckConfigSheet` (ModalBottomSheet) para configurar mazos.
    - [x] Añadido soporte para **Robo Boca Abajo** (B-2): Las cartas se inicializan en la cara de dorso al ser robadas.
    - [x] Añadido soporte para **Imagen de Dorso Global** (B-3).
    - [x] Añadida configuración de **Aspect Ratio** y **Draw Mode** desde la UI del mazo.
    - [x] Migración Room v5 → v6 con campos `backImagePath` y `drawFaceDown` en `CardStackEntity`.

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

## Sprint 12: Gestión Avanzada de Mazos y Mejoras de OCR (2026-04-15)

### Objetivos Completados
- [x] **Duplicar Mazo:** Implementada la clonación completa de mazos, incluyendo la copia física de las imágenes en el almacenamiento interno para evitar dependencias entre copias.
- [x] **Fusionar Mazos:** Capacidad de migrar cartas de un mazo origen a uno destino, remapeando automáticamente las rutas de archivos de imagen.
- [x] **Mejora de Heurística OCR:** Corregido el error de "fusión de filas". El sistema ahora detecta rangos incluso si están concatenados con el texto descriptivo y separa los bloques automáticamente.
- [x] **Estabilidad:** Verificado con build completo (`BUILD SUCCESSFUL`).

### Cambios Técnicos
- **DuplicateDeckUseCase:** Ahora retorna el ID del nuevo mazo para permitir navegación inmediata después de la copia.
- **RangeParser:** Refactorizado para devolver la longitud consumida del string, permitiendo a `AnalyzeTableImageUseCase` dividir bloques de OCR que contienen múltiples datos (rango + descripción).
- **Tolerancia Vertical:** Se aumentó un 15% (de 0.6 a 0.7) en la agrupación de líneas para manejar mejor escaneos de manuales con tipografía variable.

## Sprint 13: Archivado y Restauración de Mazos (2026-04-15)

### Objetivos Completados
- [x] **Visualización de Archivo:** Los mazos archivados ahora muestran un filtro de escala de grises y un icono de "Caja" en el centro de la portada para una distinción inmediata.
- [x] **Modo Solo Lectura:** Al entrar en el detalle de un mazo archivado, se deshabilitan las funciones de edición (Añadir cartas, Tags, Configuración, Duplicación y Fusión) para preservar su estado.
- [x] **Gestión de Sesión:** Deshabilitado el botón de "Añadir a sesión" para mazos archivados desde la biblioteca.
- [x] **Estabilidad:** Build verificado y sin errores.

### Cambios Técnicos
- **DeckCoverCard:** Integración de `ColorFilter.colorMatrix` para el efecto de desactivación visual sin perder la miniatura.
- **DeckDetailScreen:** Implementación de lógica condicional sobre `uiState.deck.isArchived` para restringir el acceso a componentes mutables.
- **LibraryViewModel:** Centralización del estado de visibilidad (Activos vs Archivados) desacoplado de la lógica de persistencia.

---

### Sprint 14: OCR Pro - Revisión Dual y Sensibilidad (15 de abril de 2026)

**FEATURE — Interfaz de Revisión Dual (OCR)**
- Implementado el componente `ZoomableImage` con soporte para gestos (Zoom/Pan/Reset) para previsualización interactiva.
- Rediseño de `ReviewStep` en `TableImportScreen` con layout vertical dividido para validación directa visual vs datos.
- Persistencia de `croppedBitmap` en el `TableImportUiState` para asegurar que la referencia visual se mantenga durante todo el flujo.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — Verificada la integración del nuevo visor interactivo.

---

### Sprint 15: Tablas Recursivas (Sub-tablas) (15 de abril de 2026)

**ARCHITECTURE — Motor de Tirada Recursivo**
- Actualizado `RollTableUseCase` para soportar `subTableId` con recursividad real (hasta 5 niveles).
- Implementada lógica de concatenación de resultados (`Resultado A → Resultado B`) preservando el flujo de sesión.
- Corrección de *Smart Casts* multi-módulo mediante captura en variables locales.

**FEATURE — Table Picker & Editor UX**
- Implementado `TablePickerDialog` en el editor para vincular entradas a cualquier tabla de la biblioteca.
- Nuevo diseño de `EntryRow` con botones de Enlace/Desenlace e indicadores visuales de recursividad.
- Cargada toda la biblioteca de tablas en `TableEditorViewModel` permitiendo búsquedas rápidas durante la vinculación.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — Todas las referencias circulares y de sintaxis resueltas.
