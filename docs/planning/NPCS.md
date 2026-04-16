# NPCs — Plan de Implementación

> **Propósito:** Planificar el sistema de fichas de NPCs (Personajes No Jugadores).
> Los NPCs son el "quién" detrás de encuentros, lugares y arcos narrativos.
> Se integran con `:feature:encounters` (Sprint 16) y anticipan la Wiki de campaña (Sprint 18).
>
> **Sprint objetivo:** 17 — ver `ROADMAP.md` y `ENCOUNTERS.md` para contexto de secuencia.

---

## 1. Visión de Producto

El DM necesita acceder a información de un NPC **en segundos durante la partida**:
- Nombre, apariencia y rol de un vistazo
- Motivación y secreto del personaje (solo visible para el DM)
- Estado actual: ¿está vivo? ¿aliado o enemigo? ¿dónde está ahora?
- Vínculo rápido al encuentro donde aparece

Los NPCs también se usan en **preparación**: escribir fichas antes de la sesión,
organizarlas por campaña, buscarlas mientras se escribe la aventura.

El sistema **no reemplaza un editor de personajes completo** (Dndbeyond, Improved Initiative).
Es una agenda de contactos narrativa, rápida y contextual.

---

## 2. Modelo de Dominio

```kotlin
// :core:model — Npc.kt

data class Npc(
    val id: Long = 0,
    val name: String,
    val race: String? = null,           // "Humano", "Elfo", "Vampiro", ...
    val role: String? = null,           // "Tabernero", "Agente de Strahd", "Guardián"
    val appearance: String = "",        // descripción breve de apariencia (Markdown)
    val personality: String = "",       // rasgos de personalidad visibles
    val motivation: String = "",        // qué quiere / qué teme
    val secret: String? = null,         // info solo para el DM (nunca se muestra en modo jugador)
    val currentStatus: NpcStatus = NpcStatus.ALIVE,
    val currentLocation: String? = null, // texto libre: "Castillo de Ravenloft, 3er piso"
    val portraitImagePath: String? = null, // imagen de personaje opcional
    val tags: List<String> = emptyList(),
    val linkedEncounterIds: List<Long> = emptyList(),
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class NpcStatus {
    ALIVE,       // vivo / activo
    DEAD,        // muerto
    MISSING,     // desaparecido / paradero desconocido
    ALLY,        // aliado de los jugadores
    ENEMY,       // enemigo declarado
    NEUTRAL      // neutral (default hasta que cambie)
}
```

**Decisión de diseño:** `tags` es `List<String>` (texto libre) en lugar de `List<Tag>`.
Los tags de NPC son narrativos ("villano", "strahd", "vallaki") y no necesitan el sistema
de `Tag` de entidades de Room del dominio de mazos. Se serializan como JSON string.

---

## 3. Room — Nueva Entidad

```kotlin
@Entity(tableName = "npcs")
data class NpcEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val race: String?,
    val role: String?,
    val appearance: String,
    val personality: String,
    val motivation: String,
    val secret: String?,
    val currentStatus: String,          // NpcStatus.name
    val currentLocation: String?,
    val portraitImagePath: String?,
    val tagsJson: String,               // JSON array de strings
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

// Cross-ref NPC ↔ Encuentro (muchos a muchos)
@Entity(
    tableName = "npc_encounter_refs",
    primaryKeys = ["npcId", "encounterId"],
    foreignKeys = [
        ForeignKey(NpcEntity::class, ["id"], ["npcId"], onDelete = CASCADE),
        ForeignKey(EncounterEntity::class, ["id"], ["encounterId"], onDelete = CASCADE)
    ],
    indices = [Index("npcId"), Index("encounterId")]
)
data class NpcEncounterRefEntity(
    val npcId: Long,
    val encounterId: Long
)
```

**Room version:** bump a v10. `MIGRATION_9_10`:
```sql
CREATE TABLE npcs (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, ...);
CREATE TABLE npc_encounter_refs (npcId INTEGER NOT NULL, encounterId INTEGER NOT NULL, PRIMARY KEY(npcId, encounterId));
CREATE INDEX index_npc_encounter_refs_npcId ON npc_encounter_refs(npcId);
CREATE INDEX index_npc_encounter_refs_encounterId ON npc_encounter_refs(encounterId);
```

---

## 4. Domain — Repository y UseCases

```kotlin
// :core:domain/repository/NpcRepository.kt
interface NpcRepository {
    fun getAllNpcs(): Flow<List<Npc>>
    fun getNpcsByStatus(status: NpcStatus): Flow<List<Npc>>
    fun getNpcsForEncounter(encounterId: Long): Flow<List<Npc>>
    fun searchNpcs(query: String): Flow<List<Npc>>
    suspend fun getNpcById(id: Long): Npc?
    suspend fun saveNpc(npc: Npc): Long
    suspend fun deleteNpc(id: Long)
    suspend fun linkToEncounter(npcId: Long, encounterId: Long)
    suspend fun unlinkFromEncounter(npcId: Long, encounterId: Long)
}
```

```kotlin
// UseCases — :core:domain/usecase/npc/

class SaveNpcUseCase @Inject constructor(private val repo: NpcRepository) {
    suspend operator fun invoke(npc: Npc): Long = repo.saveNpc(npc.copy(updatedAt = System.currentTimeMillis()))
}

class SearchNpcsUseCase @Inject constructor(private val repo: NpcRepository) {
    // Combina búsqueda por nombre, raza, rol y tags
    operator fun invoke(query: String): Flow<List<Npc>> = repo.searchNpcs(query)
}

class LinkNpcToEncounterUseCase @Inject constructor(private val repo: NpcRepository) {
    suspend operator fun invoke(npcId: Long, encounterId: Long) = repo.linkToEncounter(npcId, encounterId)
}
```

---

## 5. Nuevo Módulo — `:feature:npcs`

```
:feature:npcs/
  src/main/kotlin/com/deckapp/feature/npcs/
    NpcListScreen.kt           — lista buscable de todos los NPCs
    NpcDetailScreen.kt         — ficha completa de un NPC (read-only con botón Editar)
    NpcEditorScreen.kt         — crear y editar NPC
    components/
      NpcCard.kt               — tarjeta compacta para listados
      NpcStatusBadge.kt        — badge de color por NpcStatus
      NpcPortrait.kt           — imagen de retrato con fallback de iniciales
      EncounterLinkChips.kt    — chips de encuentros vinculados
    NpcListViewModel.kt
    NpcDetailViewModel.kt
    NpcEditorViewModel.kt
    di/
      NpcsModule.kt
```

---

## 6. Pantallas

### 6.1 NpcListScreen

Accesible desde el hub "Preparar" y desde el picker dentro de `EncounterEditorScreen`.

```
┌──────────────────────────────────────────┐
│  NPCs                              [+]   │
├──────────────────────────────────────────┤
│  🔍 Buscar NPCs...                       │
│  [Todos] [Aliados] [Enemigos] [Muertos]  │  ← chips de filtro por NpcStatus
├──────────────────────────────────────────┤
│  📌 FIJADOS                             │
│  ┌────────────────────────────────────┐  │
│  │ 🖼  Strahd von Zarovich            │  │
│  │     Vampiro · Señor del Dominio    │  │
│  │     [👿 ENEMIGO]  Castillo Ravenloft│  │
│  └────────────────────────────────────┘  │
│                                          │
│  TODOS                                   │
│  ┌────────────────────────────────────┐  │
│  │ 🖼  Ireena Kolyana                 │  │
│  │     Humana · Objetivo de Strahd    │  │
│  │     [🤝 ALIADA]   Barovia          │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │ 👤 Ismark el Menor                 │  │  ← sin retrato: iniciales
│  │     Humano · Hermano de Ireena     │  │
│  │     [😐 NEUTRAL]  Barovia          │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

- Tap → `NpcDetailScreen`
- Long-press → menu contextual: Editar, Duplicar, Cambiar estado, Eliminar
- Chip de filtro "Muertos" muestra NPCs con `NpcStatus.DEAD` con opacidad reducida
- Sección "Fijados" encima del listado cuando hay `isPinned = true`

### 6.2 NpcDetailScreen

```
┌──────────────────────────────────────────┐
│  ← Strahd von Zarovich        [✏ Editar] │
├──────────────────────────────────────────┤
│  ┌──────────┐  Vampiro                   │
│  │  [foto]  │  Señor del Dominio Barovia  │
│  │          │  [👿 ENEMIGO]              │
│  └──────────┘  📍 Castillo Ravenloft     │
├──────────────────────────────────────────┤
│  APARIENCIA                              │
│  Alto, pálido, capa negra. Ojos rojos    │
│  cuando se alimenta. Anillo de sello.    │
├──────────────────────────────────────────┤
│  PERSONALIDAD                            │
│  Controlador, poético, obsesionado con   │
│  Tatyana. Trata a los PJs como juguetes. │
├──────────────────────────────────────────┤
│  MOTIVACIÓN                              │
│  Quiere a Ireena como reencarnación de   │
│  Tatyana. Teme perder el control de su   │
│  dominio.                                │
├──────────────────────────────────────────┤
│  🔒 SECRETO (solo DM)                   │
│  Es un títere del Oscuro Poder. No puede │
│  salir de Barovia aunque quiera.         │
├──────────────────────────────────────────┤
│  ENCUENTROS VINCULADOS                   │
│  [Cena en el Castillo] [Final del Arco]  │
├──────────────────────────────────────────┤
│  Tags: villano · vampiro · jefe-final    │
└──────────────────────────────────────────┘
```

- La sección "Secreto" se muestra siempre (es pantalla de DM, no hay modo jugador aquí)
- Tap en chip de encuentro → navega a `EncounterEditorScreen` del encuentro vinculado
- El retrato es editable desde aquí (SAF image picker, no solo desde el editor)

### 6.3 NpcEditorScreen

```
┌──────────────────────────────────────────┐
│  ← Nuevo NPC                   [💾 Guardar]│
├──────────────────────────────────────────┤
│  [+ Añadir retrato]                      │
│                                          │
│  Nombre *           [_______________]    │
│  Raza               [_______________]    │
│  Rol                [_______________]    │
│                                          │
│  Estado:                                 │
│  [Vivo ▼]  Ubicación: [___________]     │
│                                          │
│  APARIENCIA                              │
│  [____________________________________]  │
│  [____________________________________]  │
│                                          │
│  PERSONALIDAD                            │
│  [____________________________________]  │
│                                          │
│  MOTIVACIÓN                              │
│  [____________________________________]  │
│                                          │
│  SECRETO (solo DM) 🔒                    │
│  [____________________________________]  │
│                                          │
│  Tags (separados por coma)               │
│  [____________________________________]  │
│                                          │
│  VINCULAR A ENCUENTROS                   │
│  [+ Seleccionar encuentro]               │
│  [× Guardia del Castillo]                │
└──────────────────────────────────────────┘
```

- Sección "Vincular a encuentros": picker de encuentros existentes (lista con búsqueda)
- Los campos de texto largo (apariencia, personalidad, motivación, secreto) son
  `OutlinedTextField` multilínea — soporte básico de Markdown en la vista (Markwon)
- Retrato: SAF image picker → copia a almacenamiento interno (igual que imágenes de cartas)
- Campos obligatorios: solo Nombre

---

## 7. NPC Picker — Reutilizable en Encuentros

Cuando el DM está editando un encuentro y quiere añadir una criatura ya definida como NPC:

```kotlin
// Composable reutilizable en :core:ui
@Composable
fun NpcPickerBottomSheet(
    isVisible: Boolean,
    onNpcSelected: (Npc) -> Unit,
    onDismiss: () -> Unit
)
```

Flujo en `EncounterEditorScreen`:
- Botón "Añadir desde NPCs" → abre `NpcPickerBottomSheet`
- Al seleccionar un NPC → se crea una `EncounterCreature` con los datos del NPC
  (`name`, `armorClass` si estuviera disponible, etc.) y se añade al encuentro
- También se crea automáticamente el vínculo en `NpcEncounterRefEntity`

---

## 8. Acceso desde SessionScreen (durante partida)

Durante la sesión, el DM puede necesitar consultar la ficha de un NPC rápidamente.
No hay un tab dedicado a NPCs en SessionScreen — se accede desde el tab Notas:

**Tab Notas → sección "NPCs activos":**
- Lista compacta de NPCs vinculados a cualquier encuentro activo o sesión actual
- Tap → `NpcDetailScreen` en modo overlay (sin salir de la sesión)
- Este acceso no requiere ningún cambio en el modelo — es solo un query adicional:
  `getNpcsForEncounter(activeEncounterId)`

---

## 9. Integración con el Módulo de Encuentros

### EncounterEditorScreen — añadir NPC como criatura
```kotlin
// En EncounterEditorViewModel
fun addNpcAsCreature(npc: Npc) {
    val creature = EncounterCreature(
        encounterId = currentEncounterId,
        name = npc.name,
        maxHp = 0,     // el DM ajusta manualmente (no hay stats en el NPC)
        currentHp = 0,
        sortOrder = creatures.size
    )
    addCreature(creature)
    linkNpcToEncounterUseCase(npc.id, currentEncounterId)
}
```

### CombatTab — indicador de NPC
En el `CombatTab`, las criaturas que tienen un NPC vinculado muestran un ícono de persona.
Tap en el ícono → navega a `NpcDetailScreen` del NPC en overlay.

---

## 10. Búsqueda y Filtros

| Filtro | Implementación |
|--------|----------------|
| Texto libre | `WHERE name LIKE %query% OR role LIKE %query%` |
| Estado | `WHERE currentStatus = 'ALLY'` (enum como String) |
| Encounter | JOIN con `npc_encounter_refs` |
| Tags | `WHERE tagsJson LIKE %tag%` (simple, no FTS todavía) |
| Fijados primero | `ORDER BY isPinned DESC, updatedAt DESC` |

**Fase 2:** Migrar búsqueda de NPCs a Room FTS (`@Fts4`) cuando se implemente
la búsqueda global (Sprint 21+). Por ahora `LIKE` es suficiente para volúmenes de < 200 NPCs.

---

## 11. Roadmap de Implementación

### Sprint 17 — NPCs Fase 1 (junto con Encuentros Fase 2)
```
Día 1:
  · Modelo Npc + NpcStatus en :core:model
  · Room: NpcEntity, NpcEncounterRefEntity, MIGRATION_9_10
  · NpcRepository (interface) + NpcRepositoryImpl
  · UseCases: SaveNpcUseCase, SearchNpcsUseCase, LinkNpcToEncounterUseCase

Día 2:
  · :feature:npcs módulo Gradle
  · NpcEditorScreen + NpcEditorViewModel
  · NpcPortrait composable (imagen SAF o fallback de iniciales con CircleAvatar)

Día 3:
  · NpcListScreen + NpcListViewModel (con filtros de estado)
  · NpcDetailScreen + NpcDetailViewModel
  · NpcStatusBadge composable con colores por estado

Día 4:
  · NpcPickerBottomSheet en :core:ui
  · Integración en EncounterEditorScreen (botón "Añadir desde NPCs")
  · Acceso desde tab Notas en SessionScreen (lista "NPCs activos")
  · NpcsModule (Hilt) + rutas NavGraph
  · BUILD SUCCESSFUL + smoke test

También en Sprint 17 (Encuentros Fase 2):
  · Link bidireccional NPC ↔ Encuentro en EncounterEditorScreen
  · Indicador de NPC en CombatTab
  · Historial de encuentros en SessionHistoryScreen
```

### Sprint 18+ — NPCs Fase 2
```
  · Retrato desde cámara (además de galería)
  · Importar NPC desde un statblock de PDF (OCR — extensión del sistema de tablas)
  · Relaciones entre NPCs: "Strahd conoce a Ireena", con tipo (aliado/enemigo/conocido)
  · Ficha de NPC exportable como PDF/imagen para mostrar a jugadores
  · Vínculo NPC → WikiEntry cuando se implemente la Wiki (Sprint 18)
```

---

## 12. Decisiones de Producto Pendientes

| # | Pregunta | Opciones | Recomendación |
|---|----------|---------|---------------|
| N-1 | **¿Stats de D&D (AC, HP fijo, velocidad)?** | (a) Solo narrativo (sin stats). (b) Stats completos. (c) Stats opcionales (AC + HP como sugerencia). | Opción (c): AC y HP como campos opcionales que se copian al crear la criatura del encuentro |
| N-2 | **¿El retrato se muestra a los jugadores?** | La app es solo del DM. No hay modo jugador para NPCs. | No hay modo jugador en Fase 1 — el secreto y retrato son siempre visibles para el DM |
| N-3 | **¿Tags de NPC separados del sistema de Tags de mazos?** | Reusar el `Tag` de dominio existente vs strings libres. | Strings libres en Fase 1 — los NPCs tienen tags narrativos que no comparten dominio con mazos |
| N-4 | **¿Cómo se organiza la lista con 100+ NPCs?** | Secciones alfabéticas / agrupación por rol / agrupación por campaña. | Secciones alfabéticas en Fase 1; por campaña cuando se implemente agrupación (Fase 3) |
| N-5 | **¿Los NPCs "muertos" se ocultan o se muestran con estilo diferente?** | (a) Ocultos por defecto (chip para mostrar). (b) Visibles con opacidad reducida + badge ☠. | Opción (b): visibles pero con opacidad 0.5 y badge ☠ — el historial narrativo importa |

---

## 13. Criterios de Calidad

- `NpcPortrait` sin imagen debe mostrar un `CircleAvatar` con las iniciales del nombre
  (ej. "SZ" para "Strahd von Zarovich") en color `primaryContainer`
- El campo "Secreto" debe tener un ícono de candado y un texto diferente de color
  para que el DM sepa visualmente que esa info es privada
- La búsqueda debe responder en < 200ms para listas de hasta 200 NPCs (LIKE es suficiente)
- `NpcStatus.DEAD` debe mostrarse con `MaterialTheme.colorScheme.error` en el badge
- El editor debe hacer autosave implícito al salir (igual que las notas) — sin botón
  "¿Descartar cambios?" molesto
- Soporte dark theme completo — los retratos deben tener border sutil para distinguirse del fondo

---

*Creado: 15 de abril de 2026*  
*Relacionado: `ENCOUNTERS.md` §9 (vínculo NPC↔Encuentro), `TOOLKIT_DESIGN.md` §NPCs, `ROADMAP.md` Sprint 17*
