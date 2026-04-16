# Planificador de Sesiones y Wiki de Campaña — Plan de Implementación

> **Propósito:** Planificar el hub de preparación pre-partida del DM.
> Dos herramientas complementarias: el **Planificador** (qué va a pasar en la sesión)
> y la **Wiki** (qué existe en el mundo de la campaña).
>
> **Sprint objetivo:** 18 — ver `ROADMAP.md`, `NAVIGATION.md` para contexto de navegación.
> Depende de `:feature:encounters` (Sprint 16) y `:feature:npcs` (Sprint 17).

---

## 1. Visión de Producto

### Planificador de Sesiones
El DM prepara sesiones como una secuencia de **escenas**. Cada escena puede tener
un encuentro vinculado, notas libres y mazos sugeridos. Durante la partida, el DM
puede marcar escenas como completadas desde `SessionScreen`.

> "Es como un guión flexible, no un script rígido."

### Wiki de Campaña
Base de conocimiento del mundo del juego: lugares, facciones, lore, cronología, objetos.
Markdown con resolución de `[[links]]` entre entradas. Buscable full-text.

> "Es el compendio del DM — todo lo que el mundo es, fue o podría ser."

---

## 2. Modelo de Dominio

### Planificador

```kotlin
// :core:model — SessionPlan.kt

data class SessionPlan(
    val id: Long = 0,
    val name: String,                      // "Sesión 12: La Boda de Argynvost"
    val estimatedDate: Long? = null,
    val status: PlanStatus = PlanStatus.PLANNED,
    val scenes: List<Scene> = emptyList(),
    val linkedDeckIds: List<Long> = emptyList(),
    val dmNotes: String = "",              // notas generales del plan (Markdown)
    val linkedSessionId: Long? = null,     // si se inició la sesión real, vínculo aquí
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class PlanStatus {
    PLANNED,      // preparada, no iniciada
    IN_PROGRESS,  // sesión en curso
    COMPLETED,    // sesión terminada
    ABANDONED     // cancelada
}

data class Scene(
    val id: Long = 0,
    val planId: Long,
    val title: String,
    val description: String = "",          // Markdown — contexto de la escena
    val linkedEncounterId: Long? = null,   // encuentro preparado para esta escena
    val linkedNpcIds: List<Long> = emptyList(), // NPCs que aparecen en esta escena
    val isCompleted: Boolean = false,      // marcado durante la partida
    val sortOrder: Int = 0
)
```

### Wiki

```kotlin
// :core:model — WikiEntry.kt

data class WikiEntry(
    val id: Long = 0,
    val title: String,
    val category: WikiCategory,
    val content: String,                   // Markdown con soporte [[NombreEntrada]]
    val tags: List<String> = emptyList(),  // igual que NPC: strings libres
    val linkedEntryIds: List<Long> = emptyList(), // vínculos resueltos de [[links]]
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class WikiCategory {
    LUGAR,        // ciudades, mazmorras, regiones
    FACCION,      // facciones, organizaciones, cultos
    PERSONAJE,    // si el NPC es muy detallado — complementa :feature:npcs
    LORE,         // historia del mundo, mitos, leyendas
    CRONOLOGIA,   // eventos ordenados en el tiempo
    OBJETO,       // ítems mágicos, artefactos, reliquias
    OTRO          // cualquier cosa que no encaje
}
```

---

## 3. Room — Nuevas Entidades

### Planificador

```kotlin
@Entity(tableName = "session_plans")
data class SessionPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val estimatedDate: Long?,
    val status: String,                  // PlanStatus.name
    val linkedDeckIdsJson: String,       // JSON array de Long
    val dmNotes: String,
    val linkedSessionId: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "scenes",
    foreignKeys = [ForeignKey(SessionPlanEntity::class, ["id"], ["planId"], onDelete = CASCADE)],
    indices = [Index("planId")]
)
data class SceneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val title: String,
    val description: String,
    val linkedEncounterId: Long?,
    val linkedNpcIdsJson: String,        // JSON array de Long
    val isCompleted: Boolean,
    val sortOrder: Int
)
```

### Wiki

```kotlin
@Entity(tableName = "wiki_entries")
data class WikiEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,               // WikiCategory.name
    val content: String,
    val tagsJson: String,
    val linkedEntryIdsJson: String,     // JSON array de Long (IDs resueltos de [[links]])
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

**Room version:** bump a v11. `MIGRATION_10_11`:
```sql
CREATE TABLE session_plans (...);
CREATE TABLE scenes (...);
CREATE TABLE wiki_entries (...);
CREATE INDEX index_scenes_planId ON scenes(planId);
```

---

## 4. Domain — Repositorios y UseCases

```kotlin
// SessionPlanRepository
interface SessionPlanRepository {
    fun getAllPlans(): Flow<List<SessionPlan>>
    fun getPlansByStatus(status: PlanStatus): Flow<List<SessionPlan>>
    suspend fun getPlanById(id: Long): SessionPlan?
    suspend fun savePlan(plan: SessionPlan): Long
    suspend fun updateSceneCompleted(sceneId: Long, completed: Boolean)
    suspend fun deletePlan(id: Long)
    suspend fun linkPlanToSession(planId: Long, sessionId: Long)
}

// WikiRepository
interface WikiRepository {
    fun getAllEntries(): Flow<List<WikiEntry>>
    fun getEntriesByCategory(category: WikiCategory): Flow<List<WikiEntry>>
    fun searchEntries(query: String): Flow<List<WikiEntry>>
    suspend fun getEntryByTitle(title: String): WikiEntry?
    suspend fun saveEntry(entry: WikiEntry): Long
    suspend fun deleteEntry(id: Long)
}
```

```kotlin
// UseCases clave

class SaveSessionPlanUseCase @Inject constructor(private val repo: SessionPlanRepository) {
    suspend operator fun invoke(plan: SessionPlan): Long =
        repo.savePlan(plan.copy(updatedAt = System.currentTimeMillis()))
}

class MarkSceneCompletedUseCase @Inject constructor(private val repo: SessionPlanRepository) {
    suspend operator fun invoke(sceneId: Long, completed: Boolean) =
        repo.updateSceneCompleted(sceneId, completed)
}

class ResolveWikiLinksUseCase @Inject constructor(private val repo: WikiRepository) {
    // Parsea [[NombreEntrada]] en el contenido y devuelve el texto
    // con los IDs de entradas encontradas para navegación
    suspend operator fun invoke(content: String): ResolvedContent
}

data class ResolvedContent(
    val rawContent: String,
    val links: Map<String, Long?>  // "NombreEntrada" → id (null si no existe todavía)
)
```

---

## 5. Nuevos Módulos

```
:feature:planner/
  src/main/kotlin/com/deckapp/feature/planner/
    SessionPlanListScreen.kt     — lista de planes con status
    SessionPlanEditorScreen.kt   — editor de plan y escenas
    components/
      SceneCard.kt               — tarjeta de escena draggable (reordenable)
      SceneEditorRow.kt          — inline editor de una escena
      PlanStatusBadge.kt         — badge de color por PlanStatus
    SessionPlanListViewModel.kt
    SessionPlanEditorViewModel.kt
    di/PlannerModule.kt

:feature:wiki/
  src/main/kotlin/com/deckapp/feature/wiki/
    WikiListScreen.kt            — lista filtrable por categoría
    WikiEntryScreen.kt           — vista Markdown + links resueltos
    WikiEditorScreen.kt          — editor Markdown con toolbar básica
    components/
      WikiLinkSpan.kt            — span clickeable para [[links]]
      CategoryChip.kt            — chip de categoría con color
      WikiToolbar.kt             — toolbar de formato sobre el teclado
    WikiListViewModel.kt
    WikiEntryViewModel.kt
    WikiEditorViewModel.kt
    di/WikiModule.kt
```

---

## 6. Pantallas — Planificador

### 6.1 SessionPlanListScreen

```
┌──────────────────────────────────────────┐
│  Sesiones planificadas                   │
├──────────────────────────────────────────┤
│  [En curso] [Planificadas] [Completadas] │  ← chips de filtro
├──────────────────────────────────────────┤
│  ┌────────────────────────────────────┐  │
│  │ ▶ EN CURSO                        │  │
│  │ Sesión 12: La Boda de Argynvost   │  │
│  │ 4 escenas · 1 completada          │  │
│  │ Hoy, 21:00                        │  │
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ 📅 PLANIFICADA                    │  │
│  │ Sesión 13: El Cementerio          │  │
│  │ 3 escenas · sin fecha             │  │
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ ✅ COMPLETADA                     │  │
│  │ Sesión 11: El Puente de Tser      │  │
│  │ 5 escenas · 23 de marzo           │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

### 6.2 SessionPlanEditorScreen

```
┌──────────────────────────────────────────┐
│  ← Sesión 12: La Boda de Argynvost  [💾] │
├──────────────────────────────────────────┤
│  Nombre: [___________________________]   │
│  Fecha:  [📅 Sin fecha]                  │
│  Estado: [▶ En curso ▼]                  │
│                                          │
│  NOTAS GENERALES                         │
│  [Los jugadores llegaron con el          │
│   arma de Argynvost. Cuidado con...]     │
│                                          │
│  ESCENAS                          [+]    │
│  ┌────────────────────────────────────┐  │
│  │ ✅ 1. Llegada al Castillo          │  │  ← completada
│  │    Sin encuentro vinculado         │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │ ▷ 2. El Banquete          [↕ drag] │  │  ← activa
│  │    ⚔ Guardia del Castillo (3 crit) │  │
│  │    👤 Strahd · Ireena              │  │
│  │    [ver notas]                     │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │ ○ 3. La Cripta                     │  │  ← pendiente
│  │    Sin encuentro vinculado         │  │
│  └────────────────────────────────────┘  │
│                                          │
│  MAZOS SUGERIDOS                  [+]    │
│  [Tarokka] [Nord Games — Monsters]       │
└──────────────────────────────────────────┘
```

- Tap en escena → expand inline con descripción + botones Vincular Encuentro / Vincular NPCs
- Long-press + drag → reordenar escenas (`sh.calvin.reorderable`)
- Checkbox en cada escena → `MarkSceneCompletedUseCase` (usable también desde SessionScreen)

---

## 7. Pantallas — Wiki

### 7.1 WikiListScreen

```
┌──────────────────────────────────────────┐
│  Wiki de Campaña                         │
├──────────────────────────────────────────┤
│  🔍 Buscar...                            │
│  [Todos][Lugar][Facción][Lore][Objeto]   │  ← chips de categoría
├──────────────────────────────────────────┤
│  📌 FIJADOS                              │
│  · Barovia — Lugar                       │
│  · El Pacto Oscuro — Lore                │
│                                          │
│  LUGAR                                   │
│  · Barovia ────────────────────── 2.1k ↗ │
│  · Castillo Ravenloft ─────────── 1.8k ↗ │
│  · Vallaki ────────────────────── 980  ↗ │
│                                          │
│  LORE                                    │
│  · El Pacto Oscuro ────────────── 3.2k ↗ │
│  · Historia de Tatyana ─────────── 600 ↗ │
│                                          │
│  OBJETO                                  │
│  · Símbolo Solar de Argynvost ──── 240 ↗ │
└──────────────────────────────────────────┘
```

Los números (2.1k, etc.) son el tamaño aproximado del contenido en caracteres — da
noción de qué entradas son más detalladas sin abrir cada una.

### 7.2 WikiEntryScreen

```
┌──────────────────────────────────────────┐
│  ← Barovia                    [✏ Editar] │
│     Lugar · 📌                           │
├──────────────────────────────────────────┤
│  ## Barovia                              │
│                                          │
│  Barovia es un dominio de terror         │
│  gobernado por [[Strahd von Zarovich]].  │  ← link clickeable
│  La bruma lo rodea permanentemente...    │
│                                          │
│  ### Localidades                         │
│  - [[Castillo Ravenloft]] — al este      │
│  - [[Vallaki]] — la ciudad amurallada    │
│  - El Pueblo de Barovia — al sur         │
│                                          │
│  ### Historia                            │
│  El dominio existió antes de que         │
│  [[El Pacto Oscuro]] atrapara a Strahd.  │
│                                          │
├──────────────────────────────────────────┤
│  Tags: barovia · dominio · principal     │
│  Actualizado: hace 2 días                │
└──────────────────────────────────────────┘
```

Los `[[links]]` son spans con color `primary` y underline. Tap → navega a la
`WikiEntryScreen` del entry vinculado. Si el entry no existe aún, el link aparece en
`error` color con opción "Crear ahora" al tocar.

### 7.3 WikiEditorScreen

```
┌──────────────────────────────────────────┐
│  ← Editar: Barovia             [💾 Guardar]│
├──────────────────────────────────────────┤
│  Título:    [Barovia____________]        │
│  Categoría: [Lugar ▼]                    │
│  Tags:      [barovia, dominio, principal]│
├──────────────────────────────────────────┤
│  [B] [I] [H1] [H2] [—] [Lista] [[Link]] │  ← toolbar Markdown
├──────────────────────────────────────────┤
│  ## Barovia                              │
│                                          │
│  Barovia es un dominio de terror         │  ← TextField Markdown
│  gobernado por [[Strahd von Zarovich]]. │
│  La bruma lo rodea...                    │
│                                          │
│  (continúa)                              │
└──────────────────────────────────────────┘
```

La toolbar Markdown opera sobre `TextFieldValue` insertando sintaxis en la posición
del cursor:
- `[B]` → envuelve selección en `**...**`
- `[I]` → envuelve en `*...*`
- `[H1]` / `[H2]` → prepend `# ` / `## ` a la línea actual
- `[—]` → inserta `---\n`
- `[Lista]` → prepend `- ` a la línea actual
- `[[Link]]` → abre `WikiLinkPickerDialog` para buscar una entrada existente e insertar `[[Título]]`

---

## 8. Resolución de Wiki Links

El sistema de `[[links]]` necesita:

1. **En el editor:** autocompletado al tipear `[[` — aparece un dropdown con entradas
   cuyo título empieza por lo que el DM escribe
2. **En la vista:** los `[[NombreEntrada]]` se convierten en `AnnotatedString` con spans
   clickeables usando `Markwon` o un renderizador custom
3. **Al guardar:** `ResolveWikiLinksUseCase` parsea el contenido, busca entradas por título
   y actualiza `linkedEntryIds` en la entidad

```kotlin
// Regex para detectar links
val WIKI_LINK_REGEX = Regex("""\[\[([^\]]+)]]""")

// En WikiEntryViewModel
private suspend fun resolveLinks(content: String): List<Long> {
    return WIKI_LINK_REGEX.findAll(content)
        .mapNotNull { wikiRepository.getEntryByTitle(it.groupValues[1])?.id }
        .toList()
}
```

**Renderización:** usar `Markwon` para el Markdown base + un plugin custom para los
`[[links]]`. Markwon soporta `AbstractMarkwonPlugin` para registrar spans personalizados.

---

## 9. Integración con el Resto del Sistema

### Desde SessionScreen (durante partida)
- En el overflow (⋮) de SessionScreen → "Ver plan de sesión"
- Si hay un `SessionPlan` con `status = IN_PROGRESS`, muestra las escenas pendientes
  en un `ModalBottomSheet` con checkboxes
- El DM marca escenas completadas sin salir del tab activo

### Desde EncounterEditorScreen
- "Añadir a escena": picker de `SessionPlan` y `Scene` para crear el vínculo

### Desde NpcDetailScreen
- Sección "Aparece en escenas": lista de `Scene.title` donde el NPC está vinculado
- Tap → navega a `SessionPlanEditorScreen` con la escena expandida

### Desde WikiEntryScreen
- "Relacionado con": chips de NPCs cuya `motivation` o `secret` menciona esta entrada
  (calculado en runtime por texto, no por vínculo explícito — búsqueda simple)

---

## 10. Roadmap de Implementación

### Sprint 18 — Planificador + Wiki Fase 1
```
Día 1:
  · Modelos SessionPlan, Scene, WikiEntry, WikiCategory, PlanStatus en :core:model
  · Room: 3 entidades, MIGRATION_10_11
  · SessionPlanRepository + WikiRepository (interfaces + impl)
  · UseCases: SaveSessionPlanUseCase, MarkSceneCompletedUseCase, ResolveWikiLinksUseCase

Día 2:
  · :feature:planner módulo Gradle
  · SessionPlanListScreen + SessionPlanListViewModel
  · SessionPlanEditorScreen con lista de escenas (sin drag & drop todavía)

Día 3:
  · :feature:wiki módulo Gradle
  · WikiListScreen + WikiListViewModel (filtros por categoría)
  · WikiEntryScreen con renderizado Markdown (Markwon) + [[links]] como spans

Día 4:
  · WikiEditorScreen con toolbar Markdown básica (B, I, H1, H2, Lista)
  · WikiLinkPickerDialog: buscar entradas existentes al insertar [[
  · Integración en PrepareScreen (tabs Sesiones y Wiki activos)
  · Rutas NavGraph: SessionPlanListRoute, SessionPlanEditorRoute, WikiListRoute, WikiEntryRoute
  · BUILD SUCCESSFUL + smoke test
```

### Sprint 19 — Mejoras
```
  · Drag & drop de escenas en SessionPlanEditorScreen (sh.calvin.reorderable)
  · Autocompletado [[link]] en WikiEditorScreen (dropdown sobre el teclado)
  · Acceso al plan de sesión desde SessionScreen (BottomSheet de escenas)
  · Vínculos NPC → Escena (NpcDetailScreen muestra en qué escenas aparece)
  · Vínculos bidireccionales WikiEntry ↔ NPC
  · Export del plan de sesión como Markdown / PDF
```

---

## 11. Decisiones de Producto Pendientes

| # | Pregunta | Opciones | Recomendación |
|---|----------|---------|---------------|
| PW-1 | **¿Wiki y Notas de DM son el mismo sistema?** | (a) Separados: Wiki = enciclopedia del mundo; Notas = apuntes de sesión. (b) Unificados. | Separados — Wiki es curada y permanente; Notas son efímeras y de sesión |
| PW-2 | **¿Los [[links]] de Wiki apuntan también a NPCs?** | (a) Solo a otras WikiEntries. (b) También a NPCs y Encuentros. | Fase 1: solo WikiEntries. Fase 2: expandir a NPCs/Encuentros |
| PW-3 | **¿El Planificador de Sesiones reemplaza `Session` o es un documento aparte?** | La `Session` real (Room) es el log de eventos. `SessionPlan` es el guión previo. | Son entidades distintas: `SessionPlan.linkedSessionId` las vincula opcionalmente |
| PW-4 | **¿Las escenas tienen orden fijo o el DM las puede saltear?** | El orden es sugerido, no obligatorio. El DM puede completar la escena 3 antes que la 2. | Checkboxes independientes — no hay orden de completado forzado |
| PW-5 | **¿La Wiki tiene control de versiones (historial de ediciones)?** | (a) No (simpler). (b) Sí, guardando snapshots del content. | No en Fase 1 — añadir `updatedAt` es suficiente; el historial completo es Fase 3 |

---

## 12. Criterios de Calidad

- Los `[[links]]` en `WikiEntryScreen` deben ser distinguibles del texto normal
  sin ser agresivos — usar `primary` color con underline sutil
- El editor Markdown no debe causar lag al escribir — `TextFieldValue` local con sync
  al ViewModel solo en debounce de 500ms (igual que notas)
- La búsqueda en WikiListScreen debe funcionar sin esperar — `stateIn` con
  `WhileSubscribed(5000)` y debounce de 300ms en el query
- `SceneCard` con drag handle: touch target del handle ≥ 48dp
- `WikiEntryScreen` debe manejar entradas largas (10.000+ caracteres) sin lag —
  usar `LazyColumn` con chunks del contenido si Markwon tiene límites de performance

---

*Creado: 15 de abril de 2026*  
*Relacionado: `TOOLKIT_DESIGN.md` §Planificador + §Wiki, `ROADMAP.md` Sprint 18, `NAVIGATION.md` §PrepareScreen, `ENCOUNTERS.md`, `NPCS.md`*
