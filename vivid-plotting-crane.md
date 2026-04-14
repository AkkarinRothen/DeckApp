# Plan: Sprint Tablas Aleatorias (Sprint 5) + Sprint Visual (Sprint 4)

---

# Plan 0 — Refactor SessionScreen a Multi-Tab (HorizontalPager)

## Context

SessionScreen es actualmente una pantalla monolítica con un Scaffold, FAB fijo "ROBAR",
y una `DeckBar` con FilterChips para seleccionar el mazo activo. El Sprint 5 (Tablas) y
el Sprint 6 (Notas) requieren que SessionScreen sea un workspace de tabs.

Estado actual detectado:
- `SessionScreen.kt` — firma: `onCardClick`, `onSessionEnd`, `onBrowseDeck`
- `SessionUiState` — campos: `session`, `deckRefs`, `hand`, `pile`, `selectedDeckId`, `nightMode`, etc.
- `SessionViewModel` — gestiona todo el estado, sin tabs todavía
- `DeckBar` (líneas 346–395) — barra de chips de mazo → se mueve DENTRO del Tab 0
- FAB actual (líneas 242–265) — hardcodeado a "ROBAR" / `viewModel.drawCard()`
- No hay `HorizontalPager` ni `TabRow` en ningún archivo del módulo

---

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `feature/draw/src/.../SessionViewModel.kt` | Añadir `activeTab` a `SessionUiState` + `setActiveTab()` |
| `feature/draw/src/.../SessionScreen.kt` | Refactorizar cuerpo a `TabRow + HorizontalPager`, FAB contextual |
| `feature/draw/build.gradle.kts` | Añadir `foundation-pager` si no está en el BOM |
| `app/src/.../navigation/NavGraph.kt` | Sin cambio de rutas — `SessionRoute` se mantiene igual |

---

## Diseño de tabs

```
Tab index │ Label       │ Ícono      │ Contenido
──────────┼─────────────┼────────────┼──────────────────────────────────────────
    0     │ Mazos       │ 🃏         │ Contenido ACTUAL de SessionScreen (mano, DeckBar, PileTray)
    1     │ Tablas      │ 🎲         │ TablesTab (stub vacío por ahora, se rellena en Sprint 5)
    2     │ Notas       │ 📝         │ NotesTab (stub vacío por ahora, se rellena en Sprint 6)
    3*    │ Combate     │ ⚔️         │ CombatTab — solo visible si hasActiveEncounter = true (Sprint 7)
```

Tab 3 es dinámico: aparece/desaparece dependiendo de `hasActiveEncounter` en `SessionUiState`.

---

## Cambios en SessionUiState y SessionViewModel

### SessionUiState — campos nuevos
```kotlin
data class SessionUiState(
    // ... campos existentes sin cambio ...
    val activeTab: Int = 0,                      // tab activo (0=Mazos, 1=Tablas, 2=Notas)
    val hasActiveEncounter: Boolean = false,      // habilita/deshabilita Tab 3 (Sprint 7)
)
```

### SessionViewModel — método nuevo
```kotlin
fun setActiveTab(tab: Int) {
    _uiState.update { it.copy(activeTab = tab) }
}
```

> **Nota:** `PagerState` vive en la composición (es UI state, no business state), pero el
> índice del tab activo se duplica en ViewModel para que el FAB sepa qué acción ejecutar.
> Se usa `LaunchedEffect(pagerState.currentPage)` para sincronizar ViewModel → Compose
> y `LaunchedEffect(uiState.activeTab)` para sincronizar Compose → ViewModel.

---

## Estructura del Scaffold refactorizado

```kotlin
@Composable
fun SessionScreen(
    onCardClick: (cardId: Long, sessionId: Long) -> Unit,
    onSessionEnd: () -> Unit,
    onBrowseDeck: (deckId: Long) -> Unit = {},
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { if (uiState.hasActiveEncounter) 4 else 3 })

    // Sync pager → ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setActiveTab(pagerState.currentPage)
    }
    // Sync ViewModel → pager (para navegación programática futura)
    LaunchedEffect(uiState.activeTab) {
        if (pagerState.currentPage != uiState.activeTab)
            pagerState.animateScrollToPage(uiState.activeTab)
    }

    Scaffold(
        topBar = { SessionTopBar(...) },           // sin cambio
        floatingActionButton = { SessionFab(uiState, pagerState, viewModel) },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Column {
            // TabRow
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(icon = { Icon(Icons.Filled.Style, ...) },
                    text = { Text("Mazos") }, ...)
                Tab(icon = { Icon(Icons.Filled.Casino, ...) },
                    text = { Text("Tablas") }, ...)
                Tab(icon = { Icon(Icons.Filled.Notes, ...) },
                    text = { Text("Notas") }, ...)
                if (uiState.hasActiveEncounter)
                    Tab(icon = { Icon(Icons.Filled.Swords, ...) },
                        text = { Text("Combate") }, ...)
            }
            // Pager
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> MazosTab(uiState, viewModel, onCardClick)
                    1 -> TablesTabStub()      // placeholder Sprint 5
                    2 -> NotesTabStub()       // placeholder Sprint 6
                    3 -> CombatTabStub()      // placeholder Sprint 7
                }
            }
        }
        // Night mode overlay (se mantiene igual, sobre todo el contenido)
        if (uiState.nightMode) { NightModeOverlay() }
    }
}
```

---

## FAB contextual

```kotlin
@Composable
private fun SessionFab(
    uiState: SessionUiState,
    pagerState: PagerState,
    viewModel: SessionViewModel
) {
    val (label, action) = when (pagerState.currentPage) {
        0 -> "ROBAR" to { viewModel.drawCard() }
        1 -> "TIRAR" to { viewModel.rollActiveTable() }  // stub por ahora
        2 -> "NOTA"  to { viewModel.startQuickNote() }   // stub por ahora
        3 -> "ACCIÓN" to {}                              // stub Sprint 7
        else -> "ROBAR" to { viewModel.drawCard() }
    }
    LargeFloatingActionButton(
        onClick = action,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, ...)
            // Subtítulo de mazo solo en tab 0
            if (pagerState.currentPage == 0 && uiState.deckRefs.size > 1) {
                Text(text = selectedDeckName, ...)
            }
        }
    }
}
```

---

## MazosTab — extracción del contenido actual

El contenido del cuerpo actual de SessionScreen (hand horizontal, `DeckBar`, `PileTray`)
se extrae a un composable `MazosTab`:

```kotlin
@Composable
private fun MazosTab(
    uiState: SessionUiState,
    viewModel: SessionViewModel,
    onCardClick: (cardId: Long, sessionId: Long) -> Unit
) {
    // Todo el contenido actual del body de SessionScreen:
    // - DeckBar (FilterChips de mazo activo) — ya existía, solo se mueve aquí
    // - LazyRow de cartas en mano con SwipeToDiscardCard
    // - PileTray (descarte colapsable)
}
```

---

## Stubs para tabs vacíos (Sprint 5/6 los rellenan)

```kotlin
@Composable
private fun TablesTabStub() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Tablas — Próximamente", style = MaterialTheme.typography.bodyLarge,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable  
private fun NotesTabStub() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Notas — Próximamente", ...)
    }
}
```

---

## Pasos de ejecución

1. **`SessionUiState`** — añadir `activeTab: Int = 0` y `hasActiveEncounter: Boolean = false`
2. **`SessionViewModel`** — añadir `setActiveTab(tab: Int)` y stubs vacíos `rollActiveTable()` / `startQuickNote()`
3. **`SessionScreen`** — crear `SessionFab` como función privada; crear `MazosTab` extrayendo cuerpo actual; añadir `TabRow + HorizontalPager`; sincronizar `pagerState` ↔ ViewModel vía `LaunchedEffect`
4. **`build.gradle.kts`** de `:feature:draw` — verificar que `androidx.compose.foundation` incluye pager (es parte del BOM estándar desde Compose 1.4)
5. **Build** — `./gradlew assembleDebug`
6. **DEVLOG** — registrar refactor

---

## Verificación

1. Tab 0 (Mazos): comportamiento idéntico al actual — robar, descartar, undo, noche
2. Swipe de tab 0 a tab 1: FAB cambia de "ROBAR" a "TIRAR"
3. Swipe a tab 2: FAB cambia a "NOTA"
4. Tab 3 (Combate) NO aparece cuando `hasActiveEncounter = false`
5. Screen wake lock sigue activo en todos los tabs
6. Night mode overlay visible sobre todos los tabs
7. Sin regresión en TopAppBar overflow (barajar, resetear, etc.)
8. `selectedDeckId` se conserva al cambiar de tab y volver a Mazos

---

# Plan A — Sprint 5: Tablas Aleatorias

## Context

El DM necesita acceder a tablas aleatorias rápidamente durante la sesión sin interrumpir
el flujo de juego. Basado en investigación de Chartopia, ENWorld, D&D Beyond forums y Roll20:
los DMs priorizan **mínimos clicks**, **anidado de tablas** (3+ niveles), **dados inline**,
**historial de tiradas por sesión**, y **modo offline** garantizado.

La implementación vive principalmente en `:feature:tables` (nueva) y requiere nuevas entidades
en Room + modelos en `:core:model`.

---

## Archivos críticos a crear / modificar

| Archivo | Acción |
|---------|--------|
| `core/model/src/.../RandomTable.kt` | CREAR — modelos de dominio |
| `core/data/src/.../db/Entities.kt` | MODIFICAR — nuevas entities Room |
| `core/data/src/.../db/Daos.kt` | MODIFICAR — DAOs de tablas |
| `core/data/src/.../db/AppDatabase.kt` | MODIFICAR — bump DB version, nuevas tablas |
| `core/domain/src/.../repository/TableRepository.kt` | CREAR — interfaz |
| `core/data/src/.../repository/TableRepositoryImpl.kt` | CREAR — implementación |
| `core/domain/src/.../usecase/RollTableUseCase.kt` | CREAR — lógica de tirada |
| `feature/tables/` | CREAR módulo completo |
| `i:\TTRPG\Visuales\Decks\Deckapp\TABLES.md` | CREAR — doc de diseño permanente |
| `app/src/.../navigation/NavGraph.kt` | MODIFICAR — rutas nuevas |
| `app/build.gradle.kts` | MODIFICAR — incluir `:feature:tables` |

---

## Modelo de dominio detallado

```kotlin
// :core:model — RandomTable.kt

data class RandomTable(
    val id: Long,
    val name: String,
    val description: String,
    val category: String,        // "Encuentros", "Nombres", "Clima", "Botín", etc.
    val rollFormula: String,     // "1d6", "1d20", "2d6", "1d100"
    val entries: List<TableEntry>,
    val tags: List<Tag>,
    val isPublic: Boolean = false,  // para export/import comunitario futuro
    val createdAt: Long,
    val updatedAt: Long
)

data class TableEntry(
    val id: Long,
    val tableId: Long,
    val minRoll: Int,            // rangos: minRoll=5, maxRoll=7 → "5-7 → Lobos"
    val maxRoll: Int,
    val weight: Int = 1,         // peso relativo para RANDOM mode (no range mode)
    val text: String,            // Markdown con inline dice: "Una manada de [1d4+1] lobos"
    val subTableRef: String? = null,   // "@NombreDeTabla" — referencia a sub-tabla por nombre
    val subTableId: Long? = null,      // foreign key resuelto de subTableRef
    val sortOrder: Int
)

data class TableRollResult(
    val id: Long,
    val tableId: Long,
    val sessionId: Long?,        // null = tirada fuera de sesión
    val rollValue: Int,          // resultado del dado
    val resolvedText: String,    // texto final con dados inline ya evaluados
    val subResults: List<TableRollResult> = emptyList(),  // resultados de sub-tablas
    val timestamp: Long
)

// Modos de tirada
enum class TableRollMode {
    RANGE,    // resultado por rango (1-4 / 5-8 / 9-12) — modelo clásico D&D
    WEIGHTED, // entradas sin rango, probabilidad por weight relativo
    SEQUENTIAL // próxima entrada en orden, útil para encuentros predefinidos
}
```

---

## Sintaxis de entradas (formato de texto enriquecido)

```
Dado inline:     "Aparecen [1d4+1] bandidos"   → evalúa el dado y embebe el resultado
Sub-tabla:       "@Nombres Élficos"             → tira la tabla referenciada e inserta el resultado
Sub-tabla con prefijo: "El líder se llama @Nombres Élficos (Masculino)"
Condicional:     "[if: repeat → re-roll]"       → para evitar duplicados (futuro Sprint 6)
```

Reglas de parsing (en `RollTableUseCase`):
- `\[(\d+)d(\d+)([+-]\d+)?\]` → evaluar expresión dado
- `@([\w\s]+)` → buscar tabla por nombre y tirar recursivamente (máximo 5 niveles)

---

## Pasos de ejecución — Sprint 5

### 1. TABLES.md — Documento de diseño permanente
Crear `i:\TTRPG\Visuales\Decks\Deckapp\TABLES.md` con el diseño completo incluyendo
wireframes de UI, sintaxis de entradas, formato JSON de import/export, y roadmap de features
de tablas más allá del Sprint 5.

### 2. Modelos de dominio en `:core:model`
- `RandomTable.kt` — data classes + `TableRollMode` enum

### 3. Room entities y DAOs
Nuevas entities: `RandomTableEntity`, `TableEntryEntity`, `TableRollResultEntity`
Nuevos DAOs: `RandomTableDao`, `TableRollResultDao`
Bump `@Database(version = N)` en `AppDatabase.kt`

```kotlin
// DAOs mínimos necesarios:
@Dao interface RandomTableDao {
    @Query("SELECT * FROM random_tables ORDER BY name ASC")
    fun getAllTables(): Flow<List<RandomTableEntity>>
    
    @Query("SELECT * FROM random_tables WHERE category = :category ORDER BY name ASC")
    fun getTablesByCategory(category: String): Flow<List<RandomTableEntity>>
    
    @Transaction
    @Query("SELECT * FROM random_tables WHERE id = :id")
    suspend fun getTableWithEntries(id: Long): TableWithEntries?
    
    @Insert suspend fun insertTable(table: RandomTableEntity): Long
    @Insert suspend fun insertEntries(entries: List<TableEntryEntity>)
    @Update suspend fun updateTable(table: RandomTableEntity)
    @Delete suspend fun deleteTable(table: RandomTableEntity)
    
    @Query("SELECT DISTINCT category FROM random_tables ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>
}

@Dao interface TableRollResultDao {
    @Query("SELECT * FROM table_roll_results WHERE sessionId = :sessionId AND tableId = :tableId ORDER BY timestamp DESC")
    fun getRollHistory(sessionId: Long, tableId: Long): Flow<List<TableRollResultEntity>>
    
    @Insert suspend fun insertResult(result: TableRollResultEntity): Long
    @Query("DELETE FROM table_roll_results WHERE sessionId = :sessionId")
    suspend fun clearSessionHistory(sessionId: Long)
}
```

### 4. Repository + Use Cases
`TableRepository` interface en `:core:domain`:
```kotlin
interface TableRepository {
    fun getAllTables(): Flow<List<RandomTable>>
    fun getTablesByCategory(category: String): Flow<List<RandomTable>>
    suspend fun getTableWithEntries(id: Long): RandomTable?
    suspend fun saveTable(table: RandomTable): Long
    suspend fun deleteTable(tableId: Long)
    fun getCategories(): Flow<List<String>>
}
```

`RollTableUseCase` en `:core:domain` — lógica completa:
```kotlin
class RollTableUseCase @Inject constructor(
    private val tableRepository: TableRepository,
    private val rollResultRepository: TableRollResultRepository
) {
    suspend operator fun invoke(
        tableId: Long,
        sessionId: Long?,
        depth: Int = 0  // para evitar loops infinitos
    ): TableRollResult {
        if (depth > 5) return TableRollResult(..., resolvedText = "[máx. anidado alcanzado]")
        val table = tableRepository.getTableWithEntries(tableId) ?: error("Table $tableId not found")
        val rollValue = evaluateDice(table.rollFormula)
        val entry = table.entries.firstOrNull { rollValue in it.minRoll..it.maxRoll }
            ?: table.entries.random()
        val resolvedText = resolveInlineDice(entry.text)
        val subResult = entry.subTableId?.let { invoke(it, sessionId, depth + 1) }
        val finalText = if (subResult != null) resolveSubTable(resolvedText, subResult.resolvedText)
                        else resolvedText
        val result = TableRollResult(
            id = 0, tableId = tableId, sessionId = sessionId,
            rollValue = rollValue, resolvedText = finalText,
            subResults = listOfNotNull(subResult), timestamp = System.currentTimeMillis()
        )
        rollResultRepository.insertResult(result)
        return result
    }
    
    private fun evaluateDice(formula: String): Int { /* parsear "1d20", "2d6+3" */ }
    private fun resolveInlineDice(text: String): String { /* reemplazar [1d4+1] en texto */ }
}
```

### 5. `:feature:tables` — nuevo módulo Gradle

Pantallas y componentes:
- `TablesTab.kt` — composable que vive dentro del `HorizontalPager` de `SessionScreen`
- `TableListScreen.kt` — lista completa de tablas (desde Preparar)
- `TableEditorScreen.kt` — crear/editar tabla con sus entradas
- `TableDetailSheet.kt` — BottomSheet de tirada activa
- `TablesViewModel.kt`
- `TableEditorViewModel.kt`

**`TablesTab.kt`** (vive en SessionScreen):
```
┌─────────────────────────────────────────┐
│  Buscar tablas...           [Filtros ▾] │
│  [Encuentros] [Nombres] [Clima] [Todo]  │  ← chips de categoría
├─────────────────────────────────────────┤
│  Encuentro Aleatorio Bosque        [🎲] │  ← tap en 🎲 → tirada inmediata
│  1d12 · 12 entradas                     │  ← tap en card → TableDetailSheet
│  "3 Lobos" hace 5 min                   │  ← último resultado de esta sesión
├─────────────────────────────────────────┤
│  Nombres Élficos                   [🎲] │
│  1d20 · 20 entradas                     │
├─────────────────────────────────────────┤
│  Clima Montañoso                   [🎲] │
│  1d6 · 6 entradas                       │
└─────────────────────────────────────────┘
         [ ◉ TIRAR TABLA ACTIVA ]
```

**`TableDetailSheet.kt`** (BottomSheet de resultado):
```
┌─────────────────────────────────────────┐
│  Encuentro Aleatorio Bosque         [×] │
│  ─────────────────────────────────────  │
│       🎲  Resultado: 7                  │  ← dado con animación de roll
│                                         │
│  "Una manada de 3 lobos ataca          │  ← texto con dados inline ya resueltos
│   desde el flanco oeste."              │
│                                         │
│  Historial en esta sesión:             │
│  · 7 → "3 lobos" (hace 2 min)          │
│  · 3 → "Bandido solitario" (hace 15m)  │
│  · 11 → "Emboscada élfica"             │
│                                         │
│  [ 🔄 Tirar de nuevo ]  [ Cerrar ]     │
└─────────────────────────────────────────┘
```

**`TableEditorScreen.kt`** (crear/editar):
```
Nombre de la tabla: [___________________]
Categoría:          [Encuentros       ▾]
Fórmula de dado:    [1d12]
Descripción:        [___________________]

Entradas:
┌────┬────┬──────────────────────────────────────────┐
│  1 │ 1  │ Un guardabosques solitario               │
├────┼────┼──────────────────────────────────────────┤
│  2 │ 3  │ [1d4+1] ciervos pastando                 │  ← editable inline
├────┼────┼──────────────────────────────────────────┤
│  4 │ 6  │ Bandidos (@Nombres Bandidos)              │  ← sub-tabla inline
├────┼────┼──────────────────────────────────────────┤
│  7 │ 9  │ Una manada de [1d4+1] lobos              │
└────┴────┴──────────────────────────────────────────┘
  minRoll · maxRoll · texto (soporta [dados] y @referencias)

[ + Añadir entrada ]

[ Guardar ]  [ Vista previa de tirada ]
```

### 6. Tablas predefinidas incluidas en el APK (bundled assets)

Incluir en `assets/tables/` un JSON con tablas clásicas para que la app sea útil
sin configuración previa. Importar automáticamente en el primer lanzamiento.

**Tablas sugeridas para el bundle inicial:**
- Encuentros en Bosque (1d12)
- Encuentros en Mazmorra (1d20)
- Nombres Humanos Masculinos (1d20)
- Nombres Humanos Femeninos (1d20)
- Nombres Élficos (1d20)
- Clima (1d6)
- Botín de Goblin (1d8)
- Resultado de Crítico (1d10)
- Resultado de Pifio (1d10)

### 7. Formato JSON para import/export de packs

```json
{
  "version": 1,
  "pack": {
    "name": "Encuentros del Bosque Oscuro",
    "author": "DM Community",
    "description": "Pack de encuentros para campañas de bosque sombrío",
    "tables": [
      {
        "name": "Encuentro Aleatorio Bosque",
        "category": "Encuentros",
        "rollFormula": "1d12",
        "description": "Tirar al entrar en el bosque o cada hora de viaje",
        "entries": [
          { "minRoll": 1, "maxRoll": 2, "text": "Sin encuentro" },
          { "minRoll": 3, "maxRoll": 4, "text": "[1d4+1] ciervos" },
          { "minRoll": 5, "maxRoll": 7, "text": "Una manada de [1d4+1] lobos" },
          { "minRoll": 8, "maxRoll": 9, "text": "Bandido solitario (@Nombres Humanos Masculinos)" },
          { "minRoll": 10, "maxRoll": 11, "text": "[1d3] trolls de bosque" },
          { "minRoll": 12, "maxRoll": 12, "text": "Un dragón verde juvenil" }
        ]
      }
    ]
  }
}
```

### 8. NavGraph — nuevas rutas

```kotlin
@Serializable object TablesListRoute
@Serializable data class TableEditorRoute(val tableId: Long = -1L)  // -1 = nueva tabla
```

### 9. Integración en SessionScreen

`SessionScreen` pasa a ser `HorizontalPager` con 3 tabs (Mazos · Tablas · Notas).
- El estado del pager se eleva al `SessionViewModel`
- `activeTab: Int` en `SessionUiState`
- FAB contextual: tab 0 → "ROBAR", tab 1 → "TIRAR", tab 2 → "NOTA"

### 10. Build y DEVLOG

Ejecutar `./gradlew assembleDebug` al terminar. Actualizar `DEVLOG.md`.

---

## Features del Sprint 5 (MVP) vs. posteriores

| Feature | Sprint |
|---------|--------|
| Crear/editar tablas con rangos | 5 |
| Tirada con animación | 5 |
| Inline dice en texto de entrada | 5 |
| Sub-tabla reference (@NombreTabla) — 1 nivel | 5 |
| Historial por sesión | 5 |
| Categorías y búsqueda | 5 |
| Tablas predefinidas bundled | 5 |
| Import/Export JSON pack | 5 |
| Sub-tablas anidadas (3+ niveles) | 6 |
| Tablas ponderadas (weight sin rango) | 6 |
| Tablas secuenciales (SEQUENTIAL mode) | 6 |
| Conditional logic ([if: repeat → re-roll]) | 7 |
| Variable passing entre tablas | 7 |
| Import CSV formato comunidad | 7 |
| UI drag & drop para reordenar entradas | 7 |

---

## Verificación

1. Build limpio sin errores: `./gradlew assembleDebug`
2. Crear una tabla manualmente con entradas de rango y dado inline
3. Tirar la tabla → resultado mostrado en BottomSheet con texto resuelto
4. Tirar de nuevo → historial muestra ambos resultados
5. Crear tabla con `@OtraTabla` en una entrada → sub-tabla se tira y resultado se embebe
6. Cambiar de tab en SessionScreen (Mazos → Tablas → Notas) sin perder estado
7. FAB cambia de "ROBAR" a "TIRAR" según el tab activo
8. Sin regresión en tab de Mazos (comportamiento actual preservado)

---

# Plan B — Sprint Visual (Sprint 4): Thumbnails y Cartas

## Context

Mejorar la visualización de cartas y barajas en DeckApp TTRPG. **Este sprint precede al Sprint 5.**
Decisiones del usuario: **ContentScale.Fit en toda la app**, prioridad en thumbnails y aspect ratio.
Documento de diseño detallado: `i:\TTRPG\Visuales\Decks\Deckapp\VISUALS.md`

### Scope de este sprint
- **V-1** `CardAspectRatio` enum configurable por mazo (STANDARD / TAROT / MINI / SQUARE / LANDSCAPE)
- **V-2** `ContentScale.Fit` + letterbox en `CardThumbnail` (elimina recorte)
- **V-3** Dots indicadores de caras activas en `CardThumbnail` (solo si `faces.size > 1`)
- **V-11** Badge contador de cartas en covers de la Biblioteca

---

## Archivos críticos

| Archivo | Cambio |
|---------|--------|
| `core/model/src/.../CardAspectRatio.kt` | **CREAR** enum |
| `core/model/src/.../CardStack.kt` | Añadir `aspectRatio: CardAspectRatio` |
| `core/data/src/.../db/Entities.kt` | Columna `aspectRatio` en `CardStackEntity` |
| `core/data/src/.../db/AppDatabase.kt` | Bump DB version |
| `core/data/src/.../db/Daos.kt` | `getTotalCardCount(stackId): Flow<Int>` |
| `core/domain/src/.../repository/CardRepository.kt` | `getTotalCardCount` |
| `core/data/src/.../repository/CardRepositoryImpl.kt` | Implementar |
| `core/ui/src/.../components/CardThumbnail.kt` | Fit + letterbox + dots + aspectRatio param |
| `feature/library/src/.../LibraryViewModel.kt` | `deckCardCounts: Map<Long, Int>` |
| `feature/library/src/.../LibraryScreen.kt` | Badge en DeckCoverCard |
| `feature/deck/src/.../DeckDetailScreen.kt` | Pasar `aspectRatio` a thumbnails |
| `feature/draw/src/.../SessionScreen.kt` | `aspectRatio` en SwipeToDiscardCard |

---

## Pasos de ejecución

### 1. `CardAspectRatio.kt` — nuevo enum en `:core:model`
```kotlin
enum class CardAspectRatio(val ratio: Float) {
    STANDARD(2.5f / 3.5f),  // 0.714 — naipes estándar
    TAROT(2.75f / 4.75f),   // 0.579 — Tarot, Oracle
    MINI(1.75f / 2.5f),     // 0.700
    SQUARE(1f),
    LANDSCAPE(3.5f / 2.5f)  // 1.400
}
```

### 2. `CardStack.kt` — añadir campo
```kotlin
val aspectRatio: CardAspectRatio = CardAspectRatio.STANDARD
```

### 3. `CardStackEntity` + mappers — añadir columna
```kotlin
@ColumnInfo(name = "aspectRatio") val aspectRatio: String = "STANDARD"
```
Mappers: `aspectRatio = CardAspectRatio.valueOf(entity.aspectRatio)` / `.name`

### 4. `AppDatabase.kt` — bump version
Incrementar `version` en `@Database`. `fallbackToDestructiveMigration()` ya activo.

### 5. `CardDao` — query de count total
```kotlin
@Query("SELECT COUNT(*) FROM cards WHERE stackId = :stackId")
fun getTotalCardCount(stackId: Long): Flow<Int>
```
Propagar en `CardRepository` interface + `CardRepositoryImpl`.

### 6. `CardThumbnail.kt` — rediseño completo
Parámetros nuevos:
- `aspectRatio: CardAspectRatio = CardAspectRatio.STANDARD`
- `showFaceDots: Boolean = true`

Cambios:
- Ancho: `height * aspectRatio.ratio`
- Imagen: `ContentScale.Fit` + `background(surfaceVariant)` como letterbox
- Dots overlay (cuando `faces.size > 1 && showFaceDots`):
  ```
  Row centrado en borde inferior de la imagen:
  ● = cara activa (5dp, primary)
  ○ = otras caras (4dp, onSurface.copy(0.4f))
  Fondo semitransparente detrás del row de dots
  ```

### 7. `LibraryViewModel.kt` — card counts
Añadir a `LibraryUiState`: `deckCardCounts: Map<Long, Int> = emptyMap()`

Cargar con `flatMapLatest` + `combine`:
```kotlin
getAllDecks().flatMapLatest { decks ->
    if (decks.isEmpty()) flowOf(emptyMap())
    else combine(decks.map { d -> getTotalCardCount(d.id).map { d.id to it } }) { pairs ->
        pairs.toMap()
    }
}
```
Combinar con el flow existente de `uiState`.

### 8. `LibraryScreen.kt` — badge en DeckCoverCard
`DeckCoverCard` recibe `cardCount: Int`:
- `BadgedBox` de Material3 envuelve el cover image
- Badge texto: `"$cardCount"`, style `labelSmall`

### 9. `DeckDetailScreen.kt` — pasar aspectRatio
```kotlin
CardThumbnail(card = card, aspectRatio = deck.aspectRatio ?: CardAspectRatio.STANDARD, ...)
```

### 10. `SessionScreen.kt` — aspectRatio en mano
- `SessionUiState` nuevo campo: `activeDeckAspectRatio: CardAspectRatio`
  (cargado desde el `CardStack` del mazo seleccionado)
- `SwipeToDiscardCard` recibe `aspectRatio` → pasa a `CardThumbnail`

### 11. Build
`./gradlew assembleDebug`

---

## Verificación
1. Build limpio sin errores
2. Biblioteca: badge numérico visible en cada cover de mazo
3. DeckDetail: cartas Tarot más altas que naipes estándar (si el mazo tiene `TAROT`)
4. DeckDetail: cartas con 2+ caras muestran dots; las de 1 cara no
5. Sesión: cartas en mano sin recorte, carta completa visible con letterbox
6. Sin regresión en import ni en CardViewScreen

---

# Plan original: DeckApp TTRPG — Documentos Iniciales

## Context (histórico)

App Android nativa como herramienta para DMs de TTRPG — gestión de mazos de cartas digitales.
- Assets en: `I:\TTRPG\Visuales\Decks\The Ultimate Trove - Assets (Decks)\` (~4,059 archivos, 160 carpetas)
- Referencia funcional: Foundry VTT Card System (v9+) + módulos Monarch, CCM, Card Hand Mini Toolbar
- Arquitectura de referencia: TDAPP (multi-módulo Gradle, Clean Architecture, Hilt, Room, Compose)

---

## Decisiones Tomadas
- [x] UI en **español**
- [x] Assets de The Ultimate Trove como biblioteca inicial (lectura directa vía SAF)
- [x] Arquitectura: multi-módulo + Clean Architecture, igual que TDAPP
- [x] Stack: Kotlin, Jetpack Compose, Room, Hilt, Navigation Compose, Material 3, Coil, Markwon
- [x] Modelo de dominio basado en Foundry VTT: Deck / Hand / Pile + multi-face cards
- [x] **Event log** como fuente de verdad para sesiones (append-only `DrawEvent` table)
- [x] **Session-scoped draw state**: las sesiones tienen copia de trabajo; el deck en library siempre está limpio
- [x] **Content URIs** (SAF) con persistencia de grant, imágenes copiadas a almacenamiento interno en import

## Decisiones Pendientes
- [ ] ¿Sincronización en la nube? (Supabase como en TDAPP natural para Fase 3)
- [ ] Target API Android mínima (propuesta: API 26)
- [ ] Nombre definitivo de la app

---

## Modelo de Dominio

### Tipos de pila
| Tipo        | Descripción                                | Visibilidad |
|-------------|-------------------------------------------|-------------|
| `DECK`      | Fuente principal. Baraja, reparte, resetea | DM (pública) |
| `HAND`      | Cartas en mano durante la sesión          | Privada      |
| `PILE`      | Descarte / cartas jugadas                 | Pública      |

---

### Modos de contenido de carta (CardContentMode)

Investigando productos del mercado (Tarot, Story Engine, Gloomhaven, Arkham Horror LCG,
Nord Games, Deck of Many, etc.) se identificaron **8 patrones estructurales** de contenido:

| Modo                  | Ejemplo de producto          | Descripción                                                    |
|-----------------------|------------------------------|----------------------------------------------------------------|
| `IMAGE_ONLY`          | Deck of Illusions, imágenes  | Solo imagen. Sin texto estructurado.                           |
| `IMAGE_WITH_TEXT`     | Nord Games, Paul Weber       | Imagen + título + cuerpo Markdown.                             |
| `REVERSIBLE`          | Tarot, Oracle decks          | Misma imagen, 2 textos: derecho e invertido (giro 180°).       |
| `TOP_BOTTOM_SPLIT`    | Gloomhaven ability cards     | Una cara dividida en zona superior e inferior (2 acciones).    |
| `LEFT_RIGHT_SPLIT`    | Algunos flip books, variante | Una cara dividida izquierda/derecha.                           |
| `FOUR_EDGE_CUES`      | Story Engine Deck            | 4 textos de pista, uno por orientación (0°/90°/180°/270°).    |
| `FOUR_QUADRANT`       | Compases narrativos, variante| 4 zonas en cuadrícula 2×2, una por esquina.                   |
| `DOUBLE_SIDED_FULL`   | Arkham Horror LCG            | Ambas caras con contenido completo e independiente.            |

**Detalle de los casos más relevantes para TTRPG:**

```
REVERSIBLE (Tarot/Oracle):
┌──────────────────────┐     ┌──────────────────────┐
│  [imagen central]    │ giro│  [misma imagen ↕]    │
│                      │ 180°│                      │
│  Upright text:       │ ──→ │  Reversed text:      │
│  "Transformación,    │     │  "Resistencia al     │
│   cambio positivo"   │     │   cambio, bloqueo"   │
└──────────────────────┘     └──────────────────────┘

TOP_BOTTOM_SPLIT (Gloomhaven):
┌──────────────────────┐
│  ZONA SUPERIOR       │  → Acción A (ej: "Mover 3")
│  [imagen top]        │
│  texto acción top    │
├──────────────────────┤
│  ZONA INFERIOR       │  → Acción B (ej: "Atacar 2")
│  [imagen bottom]     │
│  texto acción bottom │
└──────────────────────┘

FOUR_EDGE_CUES (Story Engine):
         [Cue Norte: "Traición"]
              ↑
[Cue Oeste]←  IMAGEN  →[Cue Este]
              ↓
         [Cue Sur: "Alianza"]

La pista activa = la que queda más cerca del lector
según la orientación en que se coloca la carta.
Agent/Anchor/Aspect: 4 cues. Engine/Conflict: 2 cues (N/S).
```

---

### Modelos core actualizados

```kotlin
enum class StackType { DECK, HAND, PILE }
enum class DrawMode { TOP, BOTTOM, RANDOM }

// Modo de contenido — define cómo se renderizan las zonas de la carta
enum class CardContentMode {
    IMAGE_ONLY,
    IMAGE_WITH_TEXT,
    REVERSIBLE,          // upright + reversed text, misma imagen
    TOP_BOTTOM_SPLIT,    // 2 ContentZones: [0]=top, [1]=bottom
    LEFT_RIGHT_SPLIT,    // 2 ContentZones: [0]=left, [1]=right
    FOUR_EDGE_CUES,      // 4 ContentZones: [0]=N, [1]=E, [2]=S, [3]=W
    FOUR_QUADRANT,       // 4 ContentZones: [0]=NW, [1]=NE, [2]=SW, [3]=SE
    DOUBLE_SIDED_FULL    // faces[0] y faces[1] son independientes y completas
}

// Zona de contenido dentro de una cara
data class ContentZone(
    val text: String,          // Markdown
    val imagePath: String?     // imagen de zona (opcional, e.g. en top/bottom split)
)

data class CardFace(
    val name: String,          // "Frente", "Dorso", "Upright", etc.
    val imagePath: String?,    // imagen de fondo de la cara completa
    val contentMode: CardContentMode,
    val zones: List<ContentZone>,
    // Para REVERSIBLE: zones[0] = upright, zones[1] = reversed
    // Para TOP_BOTTOM: zones[0] = top, zones[1] = bottom
    // Para FOUR_EDGE: zones[0..3] = N/E/S/W cues
    val reversedImagePath: String? = null  // imagen alternativa al invertir (raro, opcional)
)

data class Card(
    val id: Long, val stackId: Long,
    val originDeckId: Long,       // para Reset
    val title: String, val suit: String?, val value: Int?,
    val faces: List<CardFace>,    // mínimo 1 cara (dorso es opcional)
    val currentFaceIndex: Int = 0,
    val currentRotation: Int = 0, // 0/90/180/270 — para FOUR_EDGE y REVERSIBLE
    val isReversed: Boolean = false,
    val isDrawn: Boolean = false,
    val sortOrder: Int, val tags: List<Tag>
)

data class CardStack(
    val id: Long, val name: String, val type: StackType,
    val description: String, val coverImagePath: String?,
    val sourceFolderPath: String?,
    val defaultContentMode: CardContentMode = CardContentMode.IMAGE_ONLY,
    val displayCount: Boolean = true,
    val tags: List<Tag>, val createdAt: Long
)

// Event log — fuente de verdad de sesión (append-only)
data class DrawEvent(
    val id: Long, val sessionId: Long, val cardId: Long,
    val action: DrawAction, // DRAW, DISCARD, PASS, FLIP, ROTATE, REVERSE, RESET
    val timestamp: Long
)
```

---

### UI por modo de contenido

**CardViewScreen debe adaptar su rendering según `contentMode`:**

| Modo               | Rendering en CardViewScreen                                       |
|--------------------|------------------------------------------------------------------|
| `IMAGE_ONLY`       | Imagen full-screen, pinch-zoom, sin panel de texto               |
| `IMAGE_WITH_TEXT`  | Imagen + bottom sheet con título y Markdown                      |
| `REVERSIBLE`       | Imagen + toggle [↑ Derecho / ↓ Invertido] + texto correspondiente|
| `TOP_BOTTOM_SPLIT` | Imagen dividida con línea central; tap zona top/bottom → expande |
| `FOUR_EDGE_CUES`   | Imagen con indicador de rotación; botón ↻ cicla entre las 4 cues |
| `FOUR_QUADRANT`    | Imagen con overlay 2×2; tap cuadrante → expande ese contenido    |
| `DOUBLE_SIDED_FULL`| Tap imagen → flip animation completa entre ambas caras           |

**CardEditorScreen — selector de modo:**
```
¿Cómo está estructurado el contenido de esta carta?
○ Solo imagen
○ Imagen + texto libre
● Reversible (derecho / invertido)     ← activa campos "Texto derecho" + "Texto invertido"
○ División superior/inferior           ← activa 2 editores de zona
○ 4 pistas de orientación (Story Engine style)  ← activa 4 campos de pista
○ 4 cuadrantes
○ Dos caras completas e independientes
```

**Deck-level default:**
Cada `CardStack` tiene un `defaultContentMode` que se aplica a todas sus cartas al importar.
El DM puede sobreescribir por carta individual desde CardEditorScreen.

### Flujo de estados de una carta
```
DECK (isDrawn=false)
    │  DM pulsa DRAW (TOP/BOTTOM/RANDOM)
    ▼
HAND (isDrawn=true, stackType=HAND, sessionId=X)
    │  swipe izq → discard      │  "Pass" action
    ▼                            ▼
PILE (stackType=PILE)        PASADA (label=nombre jugador)
    │  "Shuffle Pile Back"
    ▼
DECK (isDrawn=false, reinserción en fondo o aleatoria)
    │  "Reset Deck" (post-sesión)
    ▼
DECK (TODAS isDrawn=false, orden original restaurado via originDeckId)
```

---

## Estructura de Módulos Gradle

```
:app                    — Orquestador, NavGraph.kt, MainActivity
:core:model             — CardStack, Card, CardFace, Tag, DrawEvent, enums
:core:domain            — Interfaces repo + UseCases:
                          DrawCardUseCase, ShuffleDeckUseCase, ResetDeckUseCase
                          DealCardsUseCase, PassCardUseCase, ImportDeckUseCase
                          CreateSessionUseCase, UndoLastActionUseCase...
:core:data              — Room entities, DAOs, mappers, RepositoryImpl
:core:ui                — DeckAppTheme, componentes compartidos (CardThumbnail, etc.)
:feature:library        — LibraryScreen + ViewModel
:feature:deck           — DeckDetailScreen + CardEditorScreen + CardViewScreen + VMs
:feature:draw           — SessionScreen + PileScreen + VMs
:feature:import         — ImportScreen + background Worker + ViewModel
:feature:session        — SessionSetupScreen + SessionHistoryScreen + VMs
```

---

## Navegación (Bottom Nav — 4 ítems)

```
[ Biblioteca ]  [ Sesión ]  [ ◉ ROBAR ]  [ Ajustes ]
      1              2       (FAB center)       4
```
El FAB central es el elemento más importante — siempre accesible con el pulgar.
Sin sesión activa → FAB = "Nueva Sesión". Con sesión activa → FAB = "Robar carta".

---

## Pantallas y Features Completas

### LibraryScreen
- Cuadrícula de mazos (2 col default) con toggle a lista
- Empty state + CTA importar
- Buscar por nombre/tag, filtrar por sistema/sesión asignada/archivado
- Tap mazo → DeckDetailScreen | Long-press → sheet contextual (Añadir a sesión, Duplicar, Archivar, Eliminar)
- Multi-selección (long-press para entrar): batch archivar/añadir a sesión
- FAB → ImportScreen

### DeckDetailScreen
- Cover image colapsable en scroll
- Cuadrícula de cartas reordenable (drag handle en modo edición)
- Filtrar por palo (chip row), ordenar por índice/palo/valor/nombre
- Tap carta → CardViewScreen | Long-press carta → Editar/Eliminar/Mover
- "+" al final de la cuadrícula → CardEditorScreen (carta nueva)
- Panel de configuración: DrawMode, dibujar boca arriba/abajo, imagen de dorso, auto-barajar en reset, notas DM
- Overflow: Duplicar, Fusionar con otro mazo, Dividir por palo, Exportar ZIP, Archivar

### SessionScreen ← **pantalla principal durante el juego**
```
┌─────────────────────────────────┐
│ [Nombre sesión]      [⋮ menú]   │  ← Top bar (informativo, no interactivo)
│ [Tarokka 42/54] [Cond. 12/20]   │  ← Tabs de mazo con contador
├─────────────────────────────────┤
│                                 │
│   [carta] [carta] [carta] ···   │  ← MANO (scroll horizontal)
│    swipe izq=descartar          │
│    tap=CardViewScreen           │
│                                 │
├─────────────────────────────────┤
│    ────── ▲ Descarte (3) ────── │  ← Tray colapsable (PILE)
└─────────────────────────────────┘
              [ ◉ ROBAR ]          ← FAB grande, centro inferior
```
- **Persistencia**: cada acción = evento en Room ANTES de animar
- **Wake lock**: pantalla no se apaga mientras SessionScreen está activa
- **Undo**: deshacer última acción (nivel 1, via DrawEvent log)
- **Night Mode**: overlay semitransparente oscuro, toggle rápido con 1 tap
- **Haptic**: pulso fuerte en cada robo de carta
- **Overflow**: Descartar mano completa, Barajar mazo, Resetear mazo, Resetear todo, Notas de sesión, Finalizar sesión

### CardViewScreen
- Imagen full-screen, pinch-to-zoom, tap para voltear (animación flip)
- Bottom sheet: nombre + palo/valor arriba, markdown scrollable
- Acciones: [ Descartar ] [ Mantener ] [ Pasar ] [ Editar ]
- **Modo "Mostrar a jugador"**: full-screen, brillo máximo, sin UI de DM, orientación bloqueada

### CardEditorScreen
- Tabs de caras (Face 1, Face 2, + Añadir cara)
- Por cara: image picker + crop, editor Markdown con toolbar mínima + preview toggle
- Metadatos: nombre (requerido), palo, valor, tags
- Guardar / Descartar cambios / Duplicar carta / Eliminar carta

### ImportScreen
- Phase 1: Selección de fuente (Browse Files / Select Folder / ZIP / **PDF**)
- Phase 2A *(carpeta)*: Tabla de análisis (carpeta → mazo, subcarpetas → palos, thumbnails, toggle incluir/excluir)
- Phase 2B *(PDF)*: Configuración del modo de extracción (ver sección PDF Import abajo)
- Auto-detect de nombre/valor desde filename (`047_dragon.png` → value=47, name="Dragon")
- Phase 3: Confirmación + progreso (Background Worker, survives screen lock)
- Phase 4: Éxito + vista previa de las cartas extraídas con opción de descartar individuales
- CTAs: "Ir a Biblioteca" / "Añadir a Sesión"

---

## PDF Import — Modos de Extracción

### Contexto
Los PDFs de mazos TTRPG (print-and-play, Nord Games, etc.) usan layouts distintos.
La app debe detectar o preguntar el modo antes de procesar.

### Modos soportados

```
MODO A — Página alternada (front/back por páginas)
  Página 1 = Frente carta 1
  Página 2 = Dorso carta 1
  Página 3 = Frente carta 2
  Página 4 = Dorso carta 2  ...
  → Emparejar páginas de 2 en 2

MODO B — Lado a lado en la misma página
  ┌──────────┬──────────┐
  │  FRENTE  │  DORSO   │
  └──────────┴──────────┘
  → Cortar imagen por la mitad vertical
  → Variante: dorso a la izquierda, frente a la derecha

MODO C — Grilla N×M por página (print-and-play estándar)
  ┌────┬────┬────┐
  │ F1 │ F2 │ F3 │
  ├────┼────┼────┤
  │ F4 │ F5 │ F6 │
  ├────┼────┼────┤
  │ F7 │ F8 │ F9 │
  └────┴────┴────┘
  → Dividir en N×M rectángulos iguales
  → Grilla de dorsos en página(s) separadas (emparejamiento por índice)
  → O grilla mixta: columnas impares = frentes, columnas pares = dorsos

MODO D — Primera mitad del PDF = frentes, segunda mitad = dorsos
  Páginas 1..N = frentes,  páginas N+1..2N = dorsos
  → Emparejar por índice (frente[i] ↔ dorso[N+i])
```

### Librería de renderizado PDF: PdfiumAndroid
- Repositorio: `barteksc/PdfiumAndroid`
- Wrapper Android del motor PDFium de Google (el mismo que usa Chrome)
- Renderiza páginas a `Bitmap` con calidad ARGB_8888
- Más completo que el `PdfRenderer` nativo (que no soporta todas las variantes de PDF)
- Agregar a `:feature:import` como dependencia

### Librería de recorte: Kotlin puro + Bitmap API (sin OpenCV)
Para los modos A, B, C y D, el recorte es **matemático** (no requiere detección de bordes):
- Se conoce la dimensión de la página (en píxeles al renderizar con PdfiumAndroid)
- Se aplica `Bitmap.createBitmap(src, x, y, width, height)` para recortar cada carta
- Sin dependencia de OpenCV → APK más liviano

OpenCV reservado para Fase 2 feature opcional: **auto-detección de grilla**
cuando el usuario no conoce el layout (detecta bordes de corte impresos).
Referencia: `zynkware/Document-Scanning-Android-SDK` (Kotlin, lightweight OpenCV).

### Flujo de UI para import PDF

```
1. Usuario selecciona PDF via SAF file picker
2. App renderiza primera página como preview (thumbnail)
3. Pantalla de configuración:
   ┌────────────────────────────────────────────────────┐
   │  Modo de layout:                                   │
   │  ○ Página alternada (frente / dorso / frente...)   │
   │  ○ Lado a lado (frente | dorso en misma página)    │
   │  ● Grilla N×M          [3] cols × [3] filas        │
   │  ○ Primera mitad = frentes, segunda = dorsos       │
   │                                                    │
   │  [Preview de pág. 1 con grilla overlay superpuesta]│
   │                                                    │
   │  Orientación del dorso:                            │
   │  ● Mismo dorso para todas las cartas               │
   │  ○ Dorso individual (definido por el layout)       │
   │                                                    │
   │  Recorte de márgenes: [auto] [manual: T R B L]     │
   └────────────────────────────────────────────────────┘
4. Preview de las primeras 4-6 cartas extraídas
5. Usuario ajusta si el recorte no es correcto
6. Confirmar → procesamiento en background (WorkManager)
7. Resultado: galería de cartas extraídas, puede descartar
   cartas individuales antes de crear el mazo definitivo
```

### Nota sobre Markdown
El Markdown en las cartas es **opcional** y complementario a las imágenes.
- Importación desde carpeta de imágenes → `bodyMarkdown = ""` (imagen sola es suficiente)
- Importación desde PDF → igual, imagen extraída como cara, markdown vacío
- El DM puede añadir texto Markdown manualmente desde CardEditorScreen después de importar
- No hay vinculación automática imagen↔.md obligatoria; es una feature de Fase 2

---

## Features Gap Analysis (no estaban en el plan original)

| Gap | Descripción | Fase |
|-----|-------------|------|
| Screen Wake Lock | `FLAG_KEEP_SCREEN_ON` en SessionScreen | 1 |
| Event log (DrawEvent) | Append-only → habilita undo + historial + crash recovery | 1 |
| Undo última acción | 1 nivel, via event log reversal | 2 |
| Night Mode overlay | Dimming + tinte cálido sobre imágenes de cartas | 2 |
| Modo "Mostrar a jugador" | Full-screen carta, sin UI DM, brillo máximo | 2 |
| Modo Browse/Referencia | Ver mazo sin modificar estado de robo | 2 |
| Spread Tarokka | Layout de posiciones nombradas (Cruz de 5 cartas) | 3 |
| Integración iniciativa | Ordenar mano por carta.value para turno de combate | 3 |
| Deck of Many Things mode | Enforcement de carta-única por jugador | 3 |
| Set Aside area | Zona temporal en sesión, distinta de mano y pila | 3 |
| Auto-detect metadata import | Parsear `NNN_nombre_palo.png` → campos Card | 2 |
| Gestión almacenamiento | Estimado de uso, optimizar imágenes de dorso | 2 |

---

## Fases del Proyecto

### Fase 1 — MVP (loop completo funcional)
Criterio: DM importa carpeta o PDF, crea sesión, roba cartas, descarta, resetea.

- [ ] Setup proyecto Android Studio multi-módulo
- [ ] Room schema v1: CardStack, Card, CardFace, ContentZone, DrawEvent, Session
- [ ] `CardContentMode` enum soportado en Room (guardado como String)
- [ ] ImportScreen: SAF folder picker → crear Decks en Room (`IMAGE_ONLY` por defecto)
- [ ] **Import desde PDF** (PdfiumAndroid) — modos A/B/C/D con config de grilla
- [ ] Preview de cartas extraídas + selector de `defaultContentMode` del mazo antes de confirmar
- [ ] LibraryScreen: cuadrícula de mazos
- [ ] DeckDetailScreen: ver cartas del mazo
- [ ] SessionScreen con FAB ROBAR + MANO + tray PILE
- [ ] CardViewScreen: rendering básico — `IMAGE_ONLY` y `IMAGE_WITH_TEXT`
- [ ] CardViewScreen: `REVERSIBLE` — toggle derecho/invertido con texto correspondiente
- [ ] CardViewScreen: `TOP_BOTTOM_SPLIT` — zonas expandibles
- [ ] Discard swipe + Reset deck
- [ ] Screen wake lock en sesión
- [ ] Dark theme por defecto

### Fase 2 — Contenido enriquecido
- [ ] CardViewScreen: `FOUR_EDGE_CUES` — rotación cíclica de pistas (Story Engine style)
- [ ] CardViewScreen: `FOUR_QUADRANT` — overlay 2×2 tappable
- [ ] CardViewScreen: `DOUBLE_SIDED_FULL` — flip completo entre ambas caras
- [ ] Markdown rendering (Markwon) en todas las zonas de texto
- [ ] CardEditorScreen completo (selector de modo, editor de zonas, multi-face)
- [ ] Auto-detección de grilla en PDF import (OpenCV — `zynkware/Document-Scanning-Android-SDK`)
- [ ] Undo última acción
- [ ] Night Mode overlay
- [ ] Deal (N cartas a jugadores nombrados)
- [ ] Peek (ver top del mazo sin robar)
- [ ] Modo "Mostrar a jugador"
- [ ] Tags + búsqueda/filtro de mazos
- [ ] Fusionar / Duplicar mazos
- [ ] Gestión de almacenamiento (Settings)
- [ ] Import desde ZIP
- [ ] Historial de sesión + Archive

### Fase 3 — DM Toolkit Expansion
- [ ] Spread layout (Tarokka)
- [ ] Integración de iniciativa por carta
- [ ] Deck of Many Things enforcement mode
- [ ] Composite virtual deck (robo de múltiples mazos como uno solo)
- [ ] Clonar sesión (reusar configuración)
- [ ] Exportar mazo como ZIP
- [ ] Google Drive import
- [ ] Agrupación por campaña
- [ ] TBD según necesidades en sesión

---

## Assets Disponibles
**Ubicación:** `I:\TTRPG\Visuales\Decks\The Ultimate Trove - Assets (Decks)\`
Contenido: Paul Weber (Magic Items, Monsters, Spells, Equipment), Nord Games (Critical, Treasure, Monster),
Deck of Many, Tarokka, Pathfinder, Deck of Illusions. ~4,059 archivos, 160 carpetas.
Estrategia: SAF folder picker apunta a esta carpeta; imágenes se copian a storage interno en import.

---

## Archivos a crear

1. `I:\TTRPG\Visuales\Decks\Deckapp\CLAUDE.md` — Mandatos de arquitectura
2. `I:\TTRPG\Visuales\Decks\Deckapp\PLANNING.md` — Documento vivo de producto
3. `I:\TTRPG\Visuales\Decks\Deckapp\DEVLOG.md` — Bitácora formato TDAPP

---

## Pasos de ejecución

1. Crear `CLAUDE.md`
2. Crear `PLANNING.md`
3. Crear `DEVLOG.md`
4. Guardar memorias del proyecto y usuario en `C:\Users\Giise\.claude\projects\i--TTRPG\memory\`

---

## Verificación
- Los tres archivos existen en `Deckapp\` y son legibles
- `CLAUDE.md` define mandatos alineados con TDAPP
- `PLANNING.md` incluye modelo de dominio, flows, pantallas, fases
- `DEVLOG.md` sigue formato TDAPP con primeros pendientes técnicos
