# DeckApp TTRPG — Visión: Suite de Herramientas para Máster

> Expansión de la visión del proyecto más allá de los mazos.
> La app evoluciona de "gestor de cartas" a "caja de herramientas completa para el DM".
>
> Basado en las prioridades del usuario:
> - **En sesión:** Tablas aleatorias + Notas rápidas (además de mazos)
> - **Preparación:** Planificador de sesiones + Encuentros + NPCs + Wiki de campaña
> - **UX preferida:** Sesión expandida con tabs — todo en una sola pantalla

---

## Principio de diseño central

> **El máster no debería necesitar salir de la pantalla de sesión para acceder a ninguna herramienta.**

Todas las herramientas de uso en mesa viven en una sola pantalla con tabs.
Las herramientas de preparación viven en una sección separada, accesible desde el bottom nav.

---

## Nueva arquitectura de navegación

### Bottom Nav rediseñado (4 ítems + FAB)

```
[ Biblioteca ]  [ Sesión ]  [ ◉ ACCIÓN ]  [ Preparar ]  [ Ajustes ]
      1              2        FAB centro         3              4
```

| Tab | Contenido | FAB en este contexto |
|-----|-----------|---------------------|
| **Biblioteca** | Mazos importados (igual que hoy) | "Importar mazo" |
| **Sesión** | Workspace multi-tab (ver abajo) | Contextual por tab activo |
| **Preparar** | Hub de preparación (ver abajo) | "Nueva X" según sub-pantalla |
| **Ajustes** | Almacenamiento, preferencias | — |

---

## Sesión expandida — Workspace multi-tab

La pantalla de sesión pasa a tener **tabs horizontales** para cambiar de herramienta
sin salir de la sesión activa.

```
┌─────────────────────────────────────────┐
│  Sesión: La Maldición de Strahd   [⋮]  │  ← TopAppBar con nombre y overflow
├───────────┬──────────┬──────────────────┤
│  🃏 Mazos │ 🎲 Tablas│  📝 Notas        │  ← Tabs
├───────────┴──────────┴──────────────────┤
│                                         │
│      [contenido del tab activo]         │
│                                         │
│                                         │
├─────────────────────────────────────────┤
│              [ ◉ ACCIÓN ]               │  ← FAB cambia según tab
└─────────────────────────────────────────┘
```

### Tab 1 — Mazos (comportamiento actual preservado)
- Mano de cartas con swipe para descartar
- Tray de descarte colapsable
- FAB = "ROBAR"

### Tab 2 — Tablas aleatorias (nueva feature)
```
┌─────────────────────────────────────────┐
│  🎲 Tablas                              │
├─────────────────────────────────────────┤
│  Búsqueda y filtro por categoría...     │
│  [Encuentros] [Nombres] [Clima] [Botín] │  ← chips
├─────────────────────────────────────────┤
│  ┌──────────────────────────────────┐   │
│  │ Encuentro Aleatorio Bosque       │   │
│  │ 1d12 · 12 entradas               │   │  ← card de tabla
│  │ Último resultado: "3 Lobos"      │   │
│  └──────────────────────────────────┘   │
│  ┌──────────────────────────────────┐   │
│  │ Nombres Élficos                  │   │
│  │ 1d20 · 20 entradas               │   │
│  └──────────────────────────────────┘   │
├─────────────────────────────────────────┤
│              [ ◉ TIRAR ]                │  ← tira la tabla seleccionada
└─────────────────────────────────────────┘
```

Al tocar una tabla → se abre `TableDetailSheet` (BottomSheet):
```
┌─────────────────────────────────────────┐
│  Encuentro Aleatorio Bosque         [×] │
├─────────────────────────────────────────┤
│  ┌───────────────────────────────────┐  │
│  │  🎲  Resultado: 7                 │  │  ← dado animado
│  │                                   │  │
│  │  "Una manada de 1d4+1 lobos       │  │  ← resultado destacado
│  │   ataca desde el flanquero        │  │
│  │   oeste del camino."              │  │
│  └───────────────────────────────────┘  │
│                                         │
│  Historial de esta sesión:              │
│  · 3 → "3 Lobos" (hace 12 min)         │
│  · 11 → "Bandido solitario"             │
│                                         │
│  [ Tirar de nuevo ]   [ Cerrar ]        │
└─────────────────────────────────────────┘
```

### Tab 3 — Notas rápidas (nueva feature)
```
┌─────────────────────────────────────────┐
│  📝 Notas de sesión                     │
├─────────────────────────────────────────┤
│  ┌──────────────────────────────────┐   │
│  │ 📌 Los jugadores saben del mapa  │   │  ← nota fijada
│  └──────────────────────────────────┘   │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │ Ireena dijo que no va a Vallaki  │   │  ← nota libre (Markdown)
│  │ hace 5 min                       │   │
│  └──────────────────────────────────┘   │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │ HP Strahd: 144 → 87              │   │
│  └──────────────────────────────────┘   │
├─────────────────────────────────────────┤
│          [ ◉ + NOTA RÁPIDA ]            │  ← abre campo de texto inline
└─────────────────────────────────────────┘
```

Tocar el FAB expande un `TextField` en la parte inferior con guardado automático al cerrar.
Las notas son Markdown pero el input por defecto es texto plano (la vista previa se activa con toggle).

---

## Preparar — Hub de preparación

```
┌─────────────────────────────────────────┐
│  Preparar                               │
├──────────┬──────────┬───────────────────┤
│   Sesión │Encuentros│ NPCs  │   Wiki    │  ← sub-tabs de preparación
├──────────┴──────────┴───────────────────┤
│                                         │
│      [contenido del sub-tab]            │
│                                         │
└─────────────────────────────────────────┘
```

### Sub-tab 1 — Planificador de sesiones
Lista de sesiones planificadas. Cada sesión tiene:
- Nombre y fecha estimada
- Lista de escenas (texto + escena de encuentro opcional)
- Mazos vinculados
- Estado: Preparada / En curso / Completada

```
Sesión 12: "La boda de Argynvost"
├── Escena 1: Llegada al castillo
├── Escena 2: El banquete [ENCUENTRO: Strahd] →
├── Escena 3: La cripta
└── Notas: Los jugadores tienen el símbolo solar
```

### Sub-tab 2 — Encuentros
Encounters preparados que el DM puede lanzar desde la sesión.

```
┌──────────────────────────────────────┐
│ Guardia del Castillo                 │
│ 3 criaturas · CR 3 total             │
│ ─────────────────────────────────── │
│ • Guardia (HP 52)   [    ❤ 52/52  ] │
│ • Guardia (HP 52)   [    ❤ 52/52  ] │
│ • Capitán (HP 78)   [    ❤ 78/78  ] │
│                          [▶ INICIAR] │
└──────────────────────────────────────┘
```

Al tocar "Iniciar" → el encuentro pasa a la sesión activa como un widget en el tab Mazos
(o en un tab nuevo "Combate" que aparece solo cuando hay combate activo).

### Sub-tab 3 — NPCs
Fichas rápidas. Cada NPC tiene: nombre, raza, rol, descripción breve, motivación.
Buscable. Tap → ficha completa con links a encuentros donde aparece.

### Sub-tab 4 — Wiki de campaña
Notas organizadas por categoría: **Lugares / Facciones / Lore / Cronología / Objetos**

Editor Markdown. Cada entrada puede referenciar a otras (`[[Nombre de entrada]]` estilo wiki).
Buscable full-text.

---

## Modelo de dominio expandido

### Tablas Aleatorias

```kotlin
data class RandomTable(
    val id: Long,
    val name: String,
    val description: String,
    val category: String,       // "Encuentros", "Nombres", "Clima", etc.
    val rollFormula: String,    // "1d6", "1d20", "2d6+3"
    val entries: List<TableEntry>,
    val tags: List<Tag>,
    val createdAt: Long
)

data class TableEntry(
    val id: Long,
    val tableId: Long,
    val minRoll: Int,
    val maxRoll: Int,           // para rangos: "5-7 → Lobos"
    val text: String,           // Markdown, puede tener inline dice: "1d4+1 Lobos"
    val subTableId: Long? = null // tabla anidada opcional
)

data class TableRollResult(
    val id: Long,
    val tableId: Long,
    val sessionId: Long?,
    val rollValue: Int,
    val resultText: String,
    val timestamp: Long
)
```

### Notas de Sesión

```kotlin
data class SessionNote(
    val id: Long,
    val sessionId: Long,
    val content: String,        // Markdown
    val isPinned: Boolean = false,
    val timestamp: Long
)
```

### Encuentros

```kotlin
data class Encounter(
    val id: Long,
    val name: String,
    val description: String,
    val creatures: List<EncounterCreature>,
    val linkedSessionId: Long? = null,
    val isActive: Boolean = false,
    val createdAt: Long
)

data class EncounterCreature(
    val id: Long,
    val encounterId: Long,
    val name: String,
    val maxHp: Int,
    val currentHp: Int,
    val armorClass: Int,
    val initiativeBonus: Int,
    val notes: String,          // estados, condiciones
    val sortOrder: Int
)
```

### NPCs

```kotlin
data class Npc(
    val id: Long,
    val name: String,
    val race: String?,
    val role: String?,          // "Tabernero", "Agente de Strahd", etc.
    val description: String,    // Markdown — apariencia, personalidad
    val motivation: String,
    val secret: String?,        // info solo para el DM
    val linkedEncounterIds: List<Long>,
    val tags: List<Tag>,
    val createdAt: Long
)
```

### Planificador de Sesiones

```kotlin
data class SessionPlan(
    val id: Long,
    val name: String,
    val estimatedDate: Long?,
    val status: PlanStatus,     // PLANNED, IN_PROGRESS, COMPLETED
    val scenes: List<Scene>,
    val linkedDeckIds: List<Long>,
    val dmNotes: String,
    val createdAt: Long
)

enum class PlanStatus { PLANNED, IN_PROGRESS, COMPLETED }

data class Scene(
    val id: Long,
    val planId: Long,
    val title: String,
    val description: String,
    val linkedEncounterId: Long?,
    val sortOrder: Int
)
```

### Wiki de Campaña

```kotlin
data class WikiEntry(
    val id: Long,
    val title: String,
    val category: WikiCategory,
    val content: String,        // Markdown con soporte [[links]]
    val tags: List<String>,
    val linkedEntryIds: List<Long>,
    val createdAt: Long,
    val updatedAt: Long
)

enum class WikiCategory {
    LUGAR, FACCION, PERSONAJE, LORE, CRONOLOGIA, OBJETO, OTRO
}
```

---

## Nuevos módulos Gradle

```
:feature:tables     → TablesScreen + TableDetailSheet + ViewModel
:feature:notes      → SessionNotesTab + ViewModel (ligero, vive dentro de Sesión)
:feature:encounters → EncounterListScreen + EncounterEditorScreen + ActiveEncounterWidget
:feature:npcs       → NpcListScreen + NpcDetailScreen + NpcEditorScreen
:feature:planner    → SessionPlanListScreen + SessionPlanEditorScreen
:feature:wiki       → WikiListScreen + WikiEntryScreen + WikiEditorScreen
```

Las notas son tan ligeras que podrían vivir dentro de `:feature:draw` (SessionScreen),
pero por el mandato de módulos separados van en `:feature:notes`.

---

## Fases de implementación

### Fase 3-A — Sesión expandida + Tablas (próximos 2 sprints)
**Prioridad máxima:** lo que el máster usa EN la mesa.

```
Sprint 5 — Tablas aleatorias
  · Modelo RandomTable + TableEntry + TableRollResult en :core:model
  · Room schema: tablas nuevas (bump DB version)
  · :feature:tables: TablesTab dentro de SessionScreen
  · Crear tablas manualmente (editor básico)
  · Roll con animación de dado, historial de tiradas por sesión
  · Categorías predefinidas importables (Encuentros, Nombres, Clima)
  · FAB contextual en tab Tablas = "TIRAR"

Sprint 6 — Notas de sesión + Tabs en SessionScreen
  · Refactorizar SessionScreen a pager con tabs (Mazos / Tablas / Notas)
  · Modelo SessionNote en :core:model
  · :feature:notes: tab ligero dentro de la sesión
  · FAB contextual en tab Notas = "+ NOTA RÁPIDA"
  · Notas fijables (isPinned)
  · Las notas aparecen en el historial de sesión (Sprint 7)
```

### Fase 3-B — Preparación (3 sprints)

```
Sprint 7 — Encuentros + Tracker de combate básico
  · Modelo Encounter + EncounterCreature en :core:model
  · :feature:encounters: lista + editor + tracker de HP inline
  · Botón "Iniciar combate" desde Preparar → crea widget en tab Combate de la sesión
  · Tab "Combate" aparece dinámicamente solo cuando hay encuentro activo

Sprint 8 — NPCs
  · Modelo Npc en :core:model
  · :feature:npcs: lista + ficha + editor
  · Buscable desde dentro de la sesión (link desde notas)
  · Link bidireccional NPC ↔ Encuentro

Sprint 9 — Planificador + Wiki
  · Modelos SessionPlan + Scene + WikiEntry en :core:model
  · :feature:planner: lista de sesiones planificadas + editor de escenas
  · :feature:wiki: lista + editor Markdown + resolución de [[links]]
  · Integración: desde una Scene del planificador se puede abrir el Encuentro vinculado
```

### Fase 4 — Integración profunda

```
  · Importar tablas desde formato CSV o JSON (comunidad)
  · Exportar resumen de sesión (notas + tiradas + cartas robadas) como PDF
  · Vinculación cruzada: mazo → NPC, tabla → encuentro, wiki → session plan
  · Búsqueda global (mazos + tablas + NPCs + wiki con un solo input)
  · Timeline de campaña que integra sesiones completadas
```

---

## Rediseño de navegación — detalle técnico

### NavGraph actualizado

**Nuevas rutas:**
```kotlin
@Serializable object PrepareRoute
@Serializable object TablesRoute
@Serializable data class TableDetailRoute(val tableId: Long)
@Serializable data class EncounterListRoute(val sessionId: Long? = null)
@Serializable data class EncounterEditorRoute(val encounterId: Long = -1L)
@Serializable object NpcListRoute
@Serializable data class NpcDetailRoute(val npcId: Long)
@Serializable object PlannerRoute
@Serializable object WikiRoute
@Serializable data class WikiEntryRoute(val entryId: Long)
```

**SessionScreen** pasa a ser un `HorizontalPager` o `TabRow`:
- Tab 0: Mazos (código actual de SessionScreen)
- Tab 1: Tablas (`:feature:tables`)
- Tab 2: Notas (`:feature:notes`)
- Tab 3: Combate (`:feature:encounters`, solo cuando `hasActiveEncounter`)

**FAB contextual en SessionScreen:**
```kotlin
val fabLabel = when (currentTab) {
    0 -> "ROBAR"
    1 -> "TIRAR"
    2 -> "NOTA"
    3 -> "INICIAR RONDA"
    else -> "ACCIÓN"
}
```

### Bottom Nav — cambios en NavGraph.kt

```kotlin
// Nuevo ítem:
NavigationBarItem(
    selected = currentDestination?.hasRoute(PrepareRoute::class) == true,
    onClick = { navController.navigate(PrepareRoute) { ... } },
    icon = { Icon(Icons.Default.AutoStories, null) },
    label = { Text("Preparar") }
)
```

---

## Decisiones de diseño clave

| # | Decisión | Elección | Razón |
|---|----------|---------|-------|
| D-1 | ¿SessionScreen como pager o tabs? | **TabRow + animateScrollToPage** | Swipe entre tabs es natural en mobile, tabs dan visibilidad de lo disponible |
| D-2 | ¿Notas en `:feature:notes` o dentro de `:feature:draw`? | **`:feature:notes`** separado | Mandato de módulos del CLAUDE.md; las notas también pueden usarse fuera de sesión |
| D-3 | ¿Encounter tracker como tab fijo o dinámico? | **Dinámico** — aparece solo cuando hay combate activo | Reduce clutter; el combate no siempre está activo |
| D-4 | ¿Wiki como Markdown libre o con estructura? | **Markdown + categorías** — estructura mínima | El Markdown ya está integrado (Markwon); las categorías permiten filtrar |
| D-5 | ¿Tablas predefinidas incluidas en el APK? | **Sí, un pack básico** — Encuentros, Nombres, Clima (editables) | Reduce fricción inicial; el DM puede editar o agregar las suyas |

---

## Impacto en arquitectura existente

| Módulo existente | Cambio necesario |
|-----------------|-----------------|
| `:core:model` | Añadir 8 nuevos data classes + 3 enums |
| `:core:data` | Room schema bump, nuevos DAOs, nuevas entities |
| `:core:domain` | Nuevos UseCases por feature (RollTableUseCase, SaveNoteUseCase, etc.) |
| `:feature:draw` | SessionScreen refactorizada como HorizontalPager con 3+ tabs |
| `:app` NavGraph | 8+ rutas nuevas, nuevo ítem en bottom nav |

---

*Última actualización: 14 de abril de 2026*
