# Módulo `:feature:reference` — Tablas de Consulta y Reglas de Sistema

> **Instrucción para Claude Code**: A medida que avancés en la implementación, marcá cada tarea completada cambiando `- [ ]` por `- [x]`. Al terminar cada sprint, agregá el emoji ✅ al título del sprint.

## Objetivo General
Proveer al DM de un repositorio centralizado de tablas de consulta (ej: efectos de condiciones, precios de equipo, encuentros aleatorios estáticos) y reglas del sistema (resúmenes de mecánicas), accesibles rápidamente mediante búsqueda global o un tab dedicado dentro de la sesión activa, filtrado por el sistema de juego de la sesión.

---

## Sprint 29 — Refactor de Modelos y Casos de Uso ✅

> **Objetivo**: Base lógica y modelos de dominio definidos.

- [x] Modelos en `:core:model`:
  - `ReferenceTable` (id, name, columns, rows, gameSystem, category, isPinned)
  - `ReferenceColumn` (header, widthWeight)
  - `ReferenceRow` (cells: List<String>)
  - `SystemRule` (id, title, content, gameSystem, category, isPinned)
- [x] Repositorios (Interfaces) en `:core:domain`:
  - `ReferenceRepository`
  - `AiReferenceRepository` (para OCR de tablas con Gemini)
- [x] Casos de Uso (Stubs/Impl base):
  - `GetAllReferenceTablesUseCase`
  - `GetReferenceTableWithRowsUseCase`
  - `SaveReferenceTableUseCase`
  - `DeleteReferenceTableUseCase`
  - `GetAllSystemRulesUseCase`
  - `SaveSystemRuleUseCase`
  - `SearchReferencesUseCase` (búsqueda unificada)
- [x] `MarkdownTableParser.kt` ya existía para tablas aleatorias — se agregó `parseReference(text): ImportPreviewData` al mismo archivo (evita duplicar la clase)
- [x] Crear `RecognizeReferenceTableFromImageUseCase.kt` (inyecta `AiReferenceRepository`)
- [x] Crear `UpdateSessionGameSystemsUseCase.kt`
  - Nota: también se agregó `updateGameSystems(sessionId, systems)` a `SessionRepository.kt`

### Verificación

- [x] `./gradlew :core:model:compileDebugKotlin`
- [x] `./gradlew :core:domain:compileDebugKotlin`
- [x] `./gradlew :core:domain:test`

---

## Sprint 30 — Persistencia Room (v26 → v27) ✅

> **Objetivo**: DB migrada, DAOs funcionando, repositorio implementado. Al terminar, `:core:data:compileDebugKotlin` sin errores y Room genera el schema v27.

### Entidades en `Entities.kt`

- [x] Agregar `ReferenceTableEntity` con `columnsJson: String`
- [x] Agregar `ReferenceRowEntity` con FK CASCADE a `reference_tables` y `cellsJson: String`
- [x] Agregar `ReferenceTableTagCrossRef` (cross-ref con `TagEntity`)
- [x] Agregar `SystemRuleEntity`
- [x] Agregar `SystemRuleTagCrossRef`
- [x] Agregar `ReferenceTableWithRows` (relation helper Room)
- [x] Actualizar `SessionEntity`: agregar `gameSystemsJson: String = "[\"General\"]"`

### FTS en `SearchEntities.kt`

- [x] Agregar `ReferenceTableFtsEntity` (`@Fts4(contentEntity = ReferenceTableEntity::class)`)
- [x] Agregar `SystemRuleFtsEntity` (`@Fts4(contentEntity = SystemRuleEntity::class)`)

### Mappers en `Mappers.kt`

- [x] `ReferenceTableEntity.toDomain(rows, tags)` y `ReferenceTable.toEntity()`
- [x] `ReferenceRowEntity.toDomain()` y `ReferenceRow.toEntity()`
- [x] `SystemRuleEntity.toDomain(tags)` y `SystemRule.toEntity()`
- [x] Mapper para `SessionEntity` ↔ `Session` (agregar `gameSystems` usando la instancia `json` existente de línea 8)
- [x] Mappers `toBackupDto()` para las 3 entidades principales

### DAOs en `ReferenceDao.kt`

- [x] `ReferenceTableDao`:
  - `getAllTablesWithRows(): Flow<List<ReferenceTableWithRows>>`
  - `getTableWithRows(id): ReferenceTableWithRows?`
  - `searchTablesFullText(query): Flow<List<ReferenceTableEntity>>` (FTS JOIN)
  - `searchRowsInTable(tableId, query): Flow<List<ReferenceRowEntity>>` (LIKE sobre cellsJson)
  - `getDistinctSystems(): Flow<List<String>>`
  - `insertTable()`, `insertRows()`, `deleteTable()`, `deleteRowsForTable()`, `updatePinned()`
  - `addTagRef()`, `removeTagRef()`, `getTagsForTable(tableId)`
  - Métodos sync para backup: `getAllTablesSync()`, `getAllRowsSync()`, `getAllTagRefsSync()`
- [x] `SystemRuleDao` (misma estructura adaptada a reglas)

### Migración `MIGRATION_26_27` en `Migrations.kt`

- [x] Crear tablas: `reference_tables`, `reference_rows`, `reference_table_tags`, `system_rules`, `system_rule_tags`
- [x] Crear índices en FKs
- [x] Crear tablas FTS: `CREATE VIRTUAL TABLE reference_tables_fts USING fts4(...)`
- [x] Crear tablas FTS: `CREATE VIRTUAL TABLE system_rules_fts USING fts4(...)`
- [x] `ALTER TABLE sessions ADD COLUMN gameSystemsJson TEXT NOT NULL DEFAULT '["General"]'`

### `DeckAppDatabase.kt`

- [x] Cambiar `version = 27`
- [x] Agregar las 9 entidades nuevas al `@Database(entities = [...])`
- [x] Agregar `abstract fun referenceTableDao(): ReferenceTableDao`
- [x] Agregar `abstract fun systemRuleDao(): SystemRuleDao`
- [x] Registrar `MIGRATION_26_27` en `companion object`

### `ReferenceRepositoryImpl.kt` (nuevo)

- [x] Implementar todos los métodos de `ReferenceRepository`
- [x] `getDistinctSystems()` combina sistemas de tablas y reglas con `combine()` + `distinct()`

### `GeminiAiRepository.kt`

- [x] Hacer que implemente `AiReferenceRepository` además de `AiTableRepository`
- [x] Implementar `recognizeReferenceTableFromImage()` con prompt JSON que preserva todas las columnas

### `DataModule.kt`

- [x] `@Provides fun provideReferenceTableDao(db): ReferenceTableDao`
- [x] `@Provides fun provideSystemRuleDao(db): SystemRuleDao`
- [x] `@Binds abstract fun bindReferenceRepository(impl): ReferenceRepository`
- [x] `@Binds abstract fun bindAiReferenceRepository(impl: GeminiAiRepository): AiReferenceRepository`
- [x] Registrar `MIGRATION_26_27` en el `databaseBuilder`

### Verificación

- [x] `./gradlew :core:data:compileDebugKotlin`
- [x] Verificar que Room generó el schema JSON v27 en `schemas/`

---

## Sprint 31 — Scaffold del módulo + componentes simples ✅

> **Objetivo**: Módulo creado y compilando. Componentes sin estado listos para usar en pantallas.

### Scaffold

- [x] 
 Crear estructura de directorios `feature/reference/src/...`
- [x] 
 Crear `build.gradle.kts` con `ksp` (NO kapt), dependencias: `:core:model`, `:core:domain`, `:core:ui`, Compose BOM, Hilt, Navigation
- [x] 
 Agregar `include(":feature:reference")` en `settings.gradle.kts`
- [x] 
 `./gradlew :feature:reference:compileDebugKotlin` (módulo vacío compila)

### Componentes puros (sin ViewModel)

- [x] 
 `SystemFilterBar.kt` — `LazyRow` de `FilterChip` por gameSystem + chip "Todos"
- [x] 
 `GameSystemsSelector.kt` en `:core:ui/components/` — `FlowRow` de `FilterChip` + botón "+" con `AlertDialog` para sistema custom
- [x] 
 `ReferenceTableCard.kt` — card con nombre, badge sistema, categoría, col count, fil count, badge pin
- [x] 
 `SystemRuleCard.kt` — card con título, badge sistema, categoría, preview markdown 2 líneas (reutiliza `MarkdownText`)
- [x] 
 `CellExpandDialog.kt` — `AlertDialog` que muestra texto completo de una celda (`título = columnHeader — firstCellValue`)

### ViewModels base

- [x] 
 `ReferenceTabViewModel.kt` con `ReferenceTabUiState`:
  ```kotlin
  data class ReferenceTabUiState(
      val referenceTables: List<ReferenceTable> = emptyList(),
      val systemRules: List<SystemRule> = emptyList(),
      val activeSystemFilters: Set<String> = emptySet(),
      val searchQuery: String = "",
      val isSearchActive: Boolean = false,
      val isLoading: Boolean = false
  )
  ```

### Verificación

- [x] 
 `./gradlew :feature:reference:compileDebugKotlin`

---

## Sprint 32 — Grid de consulta y QuickViewSheet ✅

> **Objetivo**: El componente central de consulta en sesión está funcionando. Se puede abrir una tabla y ver sus filas con scroll y tap-to-expand.

### `ReferenceTableGrid.kt`

- [x] 
 `LazyColumn` con `stickyHeader { HeaderRow(...) }`
- [x] 
 `HeaderRow`: `Row` con `Text` por columna, `Modifier.weight(column.widthWeight)`, fondo `primaryContainer`
- [x] 
 `DataRow`: `Row` con `Text(maxLines=2, overflow=Ellipsis)` + `VerticalDivider` entre celdas
- [x] 
 Filas alternas: `surface` / `surfaceVariant`
- [x] 
 Tap en celda → muestra `CellExpandDialog` con texto completo
- [x] 
 Soporte para scroll horizontal si hay > 3 columnas

### `ReferenceQuickViewSheet.kt`

- [x] 
 `ModalBottomSheet(skipPartiallyExpanded = true)` — abre al 90%
- [x] 
 Cabecera: nombre de tabla + `AssistChip` de sistema + `AssistChip` de categoría
- [x] 
 Cuerpo: `ReferenceTableGrid` con los datos de la tabla
- [x] 
 Footer: `Row` con `[Cerrar]` y `[Editar →]`
- [x] 
 `[Editar →]` navega a `ReferenceTableEditorRoute(tableId)`

### `ReferenceSpeedDial.kt`

- [x] 
 FAB principal que al tap despliega dos mini-FABs: "📋 Nueva tabla" y "📝 Nueva regla"
- [x] 
 Animación de expansión con `AnimatedVisibility`
- [x] 
 Al elegir: navega a editor con `prefilledSystem = session.gameSystems.first()`

### Verificación

- [x] 
 Preview en emulador: crear tabla manual → abrir QuickViewSheet → scroll → tap celda → ver dialog

---

## Sprint 33 — Flujo de importación ✅

> **Objetivo**: El DM puede importar datos desde CSV, Markdown pegado o imagen. Todos los flujos convergen en `ImportPreviewSheet`.

### `ImportSourceSheet.kt`

- [x] 
 `ModalBottomSheet` con 3 opciones como `ListItem`:
  - "📄 Archivo CSV/TSV" → lanza SAF file picker (`application/csv`, `text/plain`, `text/*`)
  - "📝 Pegar Markdown" → abre `AlertDialog` con `OutlinedTextField` multiline
  - "📷 Desde imagen" → lanza `ActivityResultContracts.GetContent("image/*")`
- [x] 
 Resultado de cada opción → llama a `ViewModel.prepareImport(source, data)`

### `ImportPreviewSheet.kt`

- [x] 
 `ModalBottomSheet` a ~80% de pantalla
- [x] 
 Estado: `isLoading` (para OCR), `importPreviewData: ImportPreviewData?`, `error: String?`
- [x] 
 Cuando `isLoading = true`: `CircularProgressIndicator` centrado
- [x] 
 Cuando hay datos:
  - Headers editables: `LazyRow` de `OutlinedTextField` (uno por columna)
  - Mini-grid de primeras 5 filas (no editable)
  - Si la tabla ya tiene filas: `RadioGroup` "Reemplazar / Añadir"
  - `[Cancelar]` y `[Importar N filas]` en footer
- [x] 
 Cuando hay error: `Text` de error + botón "Reintentar"
- [x] 
 Al confirmar: `ViewModel.applyImport(previewData, replaceExisting)`

### En `ReferenceTableEditorViewModel.kt`

- [x] 
 `fun prepareImportFromCsv(rawText: String)` → `CsvTableParser.parseAllRows()`
- [x] 
 `fun prepareImportFromMarkdown(rawText: String)` → `MarkdownTableParser.parse()`
- [x] 
 `fun prepareImportFromImage(bitmap: Bitmap)` → `RecognizeReferenceTableFromImageUseCase` (coroutine con loading)
- [x] 
 `fun applyImport(data: ImportPreviewData, replace: Boolean)` → actualiza `_uiState.rows` y `_uiState.columns`
- [x] 
 Si `replace = true`: limpia columnas y filas actuales; si `replace = false` y columnas no coinciden → `AlertDialog` de aviso

### Verificación

- [x] 
 Test manual: importar CSV de 3 columnas → ver preview → confirmar → ver filas en el editor
- [x] 
 Test manual: pegar tabla markdown → ver preview → confirmar
- [x] 
 Test manual: importar imagen (requiere API key Gemini)

---

## Sprint 34 — Editor de reglas ✅

> **Objetivo**: El DM puede crear y editar reglas de sistema con markdown. Sigue el patrón de `NpcEditorScreen`.

### `RuleEditorViewModel.kt`

- [x] 
 `RuleEditorUiState`: `title`, `content`, `gameSystem`, `category`, `isPinned`, `isPreviewMode`, `isSaving`, `error`
- [x] 
 `fun loadRule(ruleId: Long)` — si `ruleId == -1L`, estado vacío con `prefilledSystem`
- [x] 
 `fun save()` → `SaveSystemRuleUseCase` → navegar atrás al éxito
- [x] 
 `fun togglePreviewMode()`
- [x] 
 Validación en tiempo real: botón guardar deshabilitado si título vacío

### `RuleEditorScreen.kt`

- [x] 
 `TopAppBar`: título "Nueva regla" / "Editar regla" + `IconButton` guardar (checkmark, deshabilitado si título vacío)
- [x] 
 `OutlinedTextField`: Título *
- [x] 
 Fila con dos `ExposedDropdownMenuBox`:
  - Sistema de juego (texto libre + sugerencias de `getDistinctSystems()`)
  - Categoría (`DEFAULT_CATEGORIES` + "Personalizada..." → `OutlinedTextField` adicional)
- [x] 
 Toggle `[Editar] / [👁 Vista previa]` como `SegmentedButton`
- [x] 
 En modo Editar: `MarkdownToolbar` de `:core:ui` + `OutlinedTextField` multiline para content
- [x] 
 En modo Vista previa: `MarkdownText` de `:core:ui`
- [x] 
 `LazyColumn` con `imePadding()` para que el teclado no tape el editor

### Verificación

- [x] 
 Crear regla "Agarre" con markdown → guardar → aparece en lista
- [x] 
 Editar regla existente → cambios persisten
- [x] 
 Toggle editar/preview funciona

---

## Sprint 35 — Editor de tablas de referencia ✅

> **Objetivo**: El DM puede crear tablas con columnas personalizadas, editar filas inline e importar desde múltiples fuentes.

### `ReferenceTableEditorViewModel.kt`

- [x] 
 `ReferenceTableEditorUiState`:
  ```kotlin
  data class ReferenceTableEditorUiState(
      val name: String = "",
      val description: String = "",
      val gameSystem: String = "General",
      val category: String = "General",
      val columns: List<ReferenceColumn> = emptyList(),
      val rows: List<ReferenceRow> = emptyList(),
      val isPinned: Boolean = false,
      val isSaving: Boolean = false,
      val isImportLoading: Boolean = false,
      val importError: String? = null,
      val error: String? = null
  )
  ```
- [x] 
 `fun loadTable(tableId: Long)` — carga tabla existente o inicia vacía
- [x] 
 `fun addColumn()`, `fun removeColumn(index)`, `fun updateColumnHeader(index, header)`, `fun updateColumnWidth(index, weight)`
- [x] 
 `fun addRow()`, `fun removeRow(index)`, `fun updateCell(rowIndex, colIndex, value)`
- [x] 
 Al agregar/eliminar columna: ajustar automáticamente `cells` de todas las filas (rellenar con "" o truncar)
- [x] 
 `fun save()` → `SaveReferenceTableUseCase`
- [x] 
 `fun prepareImportFromCsv/Markdown/Image(...)` y `fun applyImport(data, replace)`

### `ReferenceTableEditorScreen.kt`

- [x] 
 `TopAppBar`: "Nueva tabla" / "Editar tabla" + botón guardar
- [x] 
 Sección metadatos: nombre, sistema, categoría, descripción
- [x] 
 Sección COLUMNAS:
  - Por cada columna: `OutlinedTextField` header + `SingleChoiceSegmentedButtonRow` [Peq|Med|Gde] + `IconButton` eliminar
  - `TextButton` "+ Añadir columna"
- [x] 
 Sección FILAS con botón "↑ Importar" en el header de sección:
  - Header fijo (no editable, fondo `primaryContainer`)
  - Por cada fila: `Row` de `BasicTextField` por celda + `IconButton` eliminar
  - Scroll horizontal si columnas > 3
  - `TextButton` "+ Añadir fila"
- [x] 
 `ImportSourceSheet` controlado por `showImportSourceSheet`
- [x] 
 `ImportPreviewSheet` controlado por `importPreviewData != null || isImportLoading`

### Verificación

- [x] 
 Crear tabla con 3 columnas y 5 filas manualmente → guardar → aparece en lista
- [x] 
 Añadir y eliminar columna → filas se ajustan automáticamente
- [x] 
 Importar desde CSV → preview → confirmar → filas aparecen en el editor
- [x] 
 Editar tabla existente → cambios persisten

---

## Sprint 36 — Pantalla de biblioteca (standalone) ✅

> **Objetivo**: El DM puede gestionar todas sus tablas y reglas desde un punto central fuera de sesiones.

### `ReferenceListViewModel.kt`

- [x] 
 `ReferenceListUiState`: tablas, reglas, sistemas disponibles, filtro activo, query de búsqueda, página activa (Tablas/Reglas)
- [x] 
 Búsqueda con debounce (300ms) usando `flatMapLatest` sobre `searchQuery`
- [x] 
 `fun toggleSystemFilter(system)`, `fun setPage(page)`, `fun deleteTable(id)`, `fun deleteRule(id)`
- [x] 
 `fun addTagToTable(tableId, tagId)`, `fun addTagToRule(ruleId, tagId)` (para long-press)

### `ReferenceListScreen.kt`

- [x] 
 `TopAppBar`: "Referencias" + `IconButton` búsqueda + `IconButton` menú (⋮)
- [x] 
 `SystemFilterBar` con sistemas disponibles
- [x] 
 `TabRow` interno [Tablas | Reglas]
- [x] 
 En tab Tablas: `LazyColumn` con `stickyHeader` por categoría, items de `ReferenceTableCard`
  - Tap → `ReferenceQuickViewSheet`
  - Long press → `DropdownMenu` [Editar | Duplicar | Asignar etiqueta | Eliminar]
  - Eliminar → `AlertDialog` de confirmación
- [x] 
 En tab Reglas: igual con `SystemRuleCard`
  - Tap → navega a `RuleEditorRoute(ruleId)` en modo lectura
- [x] 
 `ExtendedFAB` que cambia label según la tab activa: "Nueva tabla" / "Nueva regla"

### Verificación

- [x] 
 Navegar desde Library → Referencias → ver tablas y reglas agrupadas
- [x] 
 Buscar "Condiciones" → filtra resultados
- [x] 
 Long press → Asignar etiqueta → tag aparece en el item
- [x] 
 Eliminar tabla → confirmación → item desaparece

---

## Sprint 37 — Integración en SessionScreen y app ✅

> **Objetivo**: El tab de Referencia está disponible en sesiones activas. Los sistemas de juego se configuran al crear y durante una sesión.

### Rutas en `Screen.kt`

- [x] 
 `@Serializable object ReferenceListRoute`
- [x] 
 `@Serializable data class ReferenceTableEditorRoute(val tableId: Long = -1L, val prefilledSystem: String = "")`
- [x] 
 `@Serializable data class RuleEditorRoute(val ruleId: Long = -1L, val prefilledSystem: String = "")`

### `NavGraph.kt`

- [x] 
 `composable<ReferenceListRoute> { ReferenceListScreen(...) }`
- [x] 
 `composable<ReferenceTableEditorRoute> { ReferenceTableEditorScreen(...) }`
- [x] 
 `composable<RuleEditorRoute> { RuleEditorScreen(...) }`

### `SessionScreen.kt`

- [x] 
 Verificar si `TabRow` ya es `ScrollableTabRow`; si no, cambiarlo (6 tabs pueden desbordar)
- [x] 
 Aumentar `tabCount` en 1 (verificar lógica de combate activo)
- [x] 
 Agregar tab "Ref." en posición 3
- [x] 
 Agregar `case 3` en `HorizontalPager`: `ReferenceTab(sessionGameSystems = uiState.session.gameSystems, ...)`
- [x] 
 Actualizar todos los `when(page)` que referencien Notas (3→4) y Combate (4→5)
- [x] 
 Actualizar comentario de `SessionUiState.activeTab`
- [x] 
 Inyectar `referenceTabViewModel: ReferenceTabViewModel = hiltViewModel()`

### `SessionViewModel.kt`

- [x] 
 Agregar `fun updateGameSystems(systems: List<String>)` → `UpdateSessionGameSystemsUseCase`
- [x] 
 Asegurar que `SessionUiState.session.gameSystems` se propaga desde el repositorio

### `GameSystemsSelector` en sesiones

- [x] 
 Agregar sección "Sistemas de juego" en `NewSessionScreen`/`NewSessionSheet` (`:feature:session`)
  - Usar `GameSystemsSelector` de `:core:ui`
  - Sugerencias de `getDistinctSystems()`; si vacío, solo botón "+" con hint
  - Default si no se selecciona nada: `["General"]`
- [x] 
 Agregar sección "Sistemas de juego" en `SessionConfigSheet`
  - Cambios persisten inmediatamente vía `SessionViewModel.updateGameSystems()`
  - Re-filtra el tab de Referencia en tiempo real

### `LibraryScreen.kt`

- [x] 
 Agregar punto de entrada a `ReferenceListRoute` (igual que `onWikiClick`)

### Verificación

- [x] 
 Build completo: `./gradlew :app:assembleDebug`
- [x] 
 Crear sesión con sistemas "D&D 5e" + "Pathfinder 2e" → abrir sesión → tab "Ref." pre-filtrado
- [x] 
 Cambiar sistemas desde SessionConfigSheet → tab re-filtra en tiempo real
- [x] 
 SpeedDial FAB → "Nueva tabla" → sistema pre-cargado → guardar → aparece en el tab
- [x] 
 Navegar Library → Referencias → funciona como pantalla standalone

---

## Sprint 38 — Backup, build final y test end-to-end ✅

> **Objetivo**: Los datos del módulo se incluyen en el backup. El sistema completo pasa una verificación manual.

### `BackupDto.kt` en `:core:model/backup/`

- [x] Actualizar `SessionBackupDto`: agregar `val gameSystemsJson: String = "[\"General\"]"` (con default)
- [x] Agregar 5 nuevos campos a `FullBackupDto` (todos con `= emptyList()`):
  ```kotlin
  val referenceTables: List<ReferenceTableBackupDto> = emptyList()
  val referenceRows: List<ReferenceRowBackupDto> = emptyList()
  val referenceTableTags: List<ReferenceTableTagBackupDto> = emptyList()
  val systemRules: List<SystemRuleBackupDto> = emptyList()
  val systemRuleTags: List<SystemRuleTagBackupDto> = emptyList()
  ```
- [x] Agregar 5 nuevos DTOs `@Serializable`:
  - `ReferenceTableBackupDto` (id, name, description, gameSystem, category, columnsJson, isPinned, sortOrder, createdAt)
  - `ReferenceRowBackupDto` (id, tableId, cellsJson, sortOrder)
  - `ReferenceTableTagBackupDto` (tableId, tagId)
  - `SystemRuleBackupDto` (id, title, content, gameSystem, category, isPinned, sortOrder, lastUpdated)
  - `SystemRuleTagBackupDto` (ruleId, tagId)

### DAOs — métodos sync para backup

- [x] Agregar a `ReferenceTableDao`: `getAllTablesSync(): List<ReferenceTableEntity>`, `getAllRowsSync(): List<ReferenceRowEntity>`, `getAllTagRefsSync(): List<ReferenceTableTagCrossRef>`
- [x] Agregar a `SystemRuleDao`: `getAllRulesSync(): List<SystemRuleEntity>`, `getAllTagRefsSync(): List<SystemRuleTagCrossRef>`

### `BackupRepositoryImpl.kt`

- [x] En `createBackup()` dentro de `withTransaction`:
  ```kotlin
  referenceTables = referenceTableDao.getAllTablesSync().map { it.toBackupDto() },
  referenceRows = referenceTableDao.getAllRowsSync().map { it.toBackupDto() },
  referenceTableTags = referenceTableDao.getAllTagRefsSync().map { ReferenceTableTagBackupDto(it.tableId, it.tagId) },
  systemRules = systemRuleDao.getAllRulesSync().map { it.toBackupDto() },
  systemRuleTags = systemRuleDao.getAllTagRefsSync().map { SystemRuleTagBackupDto(it.ruleId, it.tagId) }
  ```
- [x] En `restoreBackup()`: insertar tablas, filas, tag refs, reglas, tag refs (en ese orden — respetar FKs)
- [x] Inyectar `referenceTableDao` y `systemRuleDao` en `BackupRepositoryImpl`

### Build y test final

- [x] 
 `./gradlew :app:assembleDebug` — sin errores
- [x] 
 `./gradlew :core:domain:test` — sin errores
- [x] 
 **Test manual en emulador**:
  - [x] 
 Crear tabla "Condiciones D&D 5e" con 3 columnas y 5 filas
  - [x] 
 Abrir sesión → tab "Ref." → abrir QuickViewSheet → tap celda → ver texto completo
  - [x] 
 Crear regla "Agarre" con markdown → verificar preview
  - [x] 
 Importar tabla desde CSV con 3 columnas
  - [x] 
 Importar tabla desde texto Markdown pegado (`| col | col |`)
  - [x] 
 Importar desde imagen (requiere API key Gemini configurada en Ajustes)
  - [x] 
 Crear sesión con sistemas "D&D 5e" + "Pathfinder 2e" → tab pre-filtra correctamente
  - [x] 
 Cambiar sistemas desde SessionConfigSheet → re-filtrado en tiempo real
  - [x] 
 Exportar backup → desinstalar app → reinstalar → importar backup → verificar datos completos
- [x] 
 Actualizar `DEVLOG.md` con entrada del sprint

---

## Registro de desviaciones

> Anotar acá cualquier cambio respecto al plan original, con fecha y motivo.

| Fecha | Sprint | Desviación | Motivo |
|-------|--------|-----------|--------|
| — | — | — | — |
