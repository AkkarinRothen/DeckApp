# Encuentros y Combat Tracker — Plan de Implementación

> **Propósito:** Planificar el sistema de encuentros y tracker de combate.
> Es el Tab 3 dinámico de `SessionScreen` (`hasActiveEncounter = true`) y una sección
> dentro del hub "Preparar" (Fase 3-B de `TOOLKIT_DESIGN.md`).
>
> **Sprint objetivo:** 16 — ver `ROADMAP.md` para contexto de secuencia.

---

## 1. Visión de Producto

El DM necesita gestionar combates en tiempo real sin salir de la sesión:
- Ver quién actúa en cada turno (iniciativa)
- Aplicar daño y curación con un tap
- Registrar condiciones (envenenado, aturdido, etc.)
- Preparar encuentros antes de la partida con fichas de criaturas reutilizables

El tracker **no es un VTT completo** — es una herramienta de asistencia rápida para el DM.
La fuente de verdad sigue siendo la imaginación y los dados físicos.

---

## 2. Modelo de Dominio

```kotlin
// :core:model — Encounter.kt

data class Encounter(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val creatures: List<EncounterCreature> = emptyList(),
    val linkedSessionId: Long? = null,   // null = encuentro de biblioteca, non-null = activo en sesión
    val isActive: Boolean = false,
    val currentRound: Int = 0,
    val currentTurnIndex: Int = 0,       // índice en la lista ordenada por iniciativa
    val createdAt: Long = System.currentTimeMillis()
)

data class EncounterCreature(
    val id: Long = 0,
    val encounterId: Long,
    val name: String,
    val maxHp: Int,
    val currentHp: Int,
    val armorClass: Int = 10,
    val initiativeBonus: Int = 0,
    val initiativeRoll: Int? = null,     // null = sin tirar todavía
    val conditions: Set<Condition> = emptySet(),
    val notes: String = "",              // estados especiales, hechizos activos
    val sortOrder: Int = 0               // orden visual antes de tirar iniciativa
)

enum class Condition {
    BLINDED, CHARMED, DEAFENED, EXHAUSTED, FRIGHTENED,
    GRAPPLED, INCAPACITATED, INVISIBLE, PARALYZED,
    PETRIFIED, POISONED, PRONE, RESTRAINED, STUNNED, UNCONSCIOUS
}
```

**Notas de diseño:**
- `initiativeRoll` es el valor del dado (1d20 + bonus). El orden de combate se
  calcula en runtime como `initiativeRoll + initiativeBonus` — sin persistir el total
  para permitir retirar si cambia el bonus.
- `conditions` es un `Set<Condition>` serializado como JSON en Room (`@TypeConverter`).
- La lista de criaturas se ordena por `initiativeRoll DESC` una vez tirada;
  antes de tirar se ordena por `sortOrder`.

---

## 3. Room — Nuevas Entidades

```kotlin
// EncounterEntity
@Entity(tableName = "encounters")
data class EncounterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val linkedSessionId: Long?,
    val isActive: Boolean,
    val currentRound: Int,
    val currentTurnIndex: Int,
    val createdAt: Long
)

// EncounterCreatureEntity
@Entity(
    tableName = "encounter_creatures",
    foreignKeys = [ForeignKey(
        entity = EncounterEntity::class,
        parentColumns = ["id"],
        childColumns = ["encounterId"],
        onDelete = CASCADE
    )],
    indices = [Index("encounterId")]
)
data class EncounterCreatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encounterId: Long,
    val name: String,
    val maxHp: Int,
    val currentHp: Int,
    val armorClass: Int,
    val initiativeBonus: Int,
    val initiativeRoll: Int?,
    val conditionsJson: String,   // JSON de Set<Condition>
    val notes: String,
    val sortOrder: Int
)
```

**Room version:** bump a v9 con `MIGRATION_8_9` (`CREATE TABLE encounters` + `CREATE TABLE encounter_creatures`).

---

## 4. Nuevo Módulo — `:feature:encounters`

```
:feature:encounters/
  src/main/kotlin/com/deckapp/feature/encounters/
    EncounterListScreen.kt         — biblioteca de encuentros preparados
    EncounterEditorScreen.kt       — crear/editar un encuentro
    components/
      CreatureRow.kt               — fila de criatura con HP bar y controles
      InitiativeOrderList.kt       — lista reordenable durante combate
      ConditionChips.kt            — chips de condiciones activas
    EncounterListViewModel.kt
    EncounterEditorViewModel.kt
    di/
      EncountersModule.kt
```

---

## 5. Pantallas

### 5.1 EncounterListScreen (Preparación)

Accesible desde el hub "Preparar" (futuro) y desde el overflow de `SessionScreen`.

```
┌──────────────────────────────────────────┐
│  Encuentros                        [+]   │
├──────────────────────────────────────────┤
│  Buscar encuentros...                    │
├──────────────────────────────────────────┤
│  ┌────────────────────────────────────┐  │
│  │ Guardia del Castillo               │  │
│  │ 3 criaturas · AC 14                │  │
│  │                    [▶ INICIAR]     │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │ Encuentro de Lobos en el Bosque    │  │
│  │ 4 criaturas · CR estimado 3        │  │
│  │                    [▶ INICIAR]     │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

- Tap en tarjeta → `EncounterEditorScreen` para ver/editar
- Tap en "Iniciar" → copia el encuentro a la sesión activa y abre el tab Combate
- Si no hay sesión activa → dialog "¿Iniciar nueva sesión primero?"

### 5.2 EncounterEditorScreen (Preparación)

```
┌──────────────────────────────────────────┐
│  ← Guardia del Castillo          [Tirar] │
├──────────────────────────────────────────┤
│  Nombre: [_____________]                 │
│  Descripción: [___________________]      │
├──────────────────────────────────────────┤
│  CRIATURAS                         [+]   │
│  ┌────────────────────────────────────┐  │
│  │ Guardia 1   HP: [52] AC: [14]      │  │
│  │ Bono Init: [+2]    [×]             │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │ Guardia 2   HP: [52] AC: [14]      │  │
│  │ Bono Init: [+2]    [×]             │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │ Capitán     HP: [78] AC: [16]      │  │
│  │ Bono Init: [+3]    [×]             │  │
│  └────────────────────────────────────┘  │
├──────────────────────────────────────────┤
│                [💾 GUARDAR]              │
└──────────────────────────────────────────┘
```

- "Tirar" → genera `initiativeRoll` (1d20) para cada criatura y guarda
- Añadir criatura: campos inline (nombre, HP max, AC, bono de iniciativa)
- Long-press en criatura → reordenar (antes de tirar iniciativa)

### 5.3 CombatTab (Tab 3 de SessionScreen, dinámico)

Visible solo cuando `SessionUiState.hasActiveEncounter = true`.

```
┌──────────────────────────────────────────┐
│  ⚔ Combate — Ronda 3          [Fin]      │
├──────────────────────────────────────────┤
│  ► Capitán ——————————— 78/78  AC 16      │  ← turno activo (resaltado)
│    Condiciones: ninguna                  │
│    [−5] [−1] [+1] [+5] [⚕ Curar] [💀]  │  ← controles de HP
├──────────────────────────────────────────┤
│    Guardia 1 ———————— 28/52  AC 14       │  ← HP bar roja
│    [☠ Envenenado]                        │
│    [−5] [−1] [+1] [+5] [⚕] [💀]         │
├──────────────────────────────────────────┤
│    Guardia 2 ———————— 52/52  AC 14       │
│    [−5] [−1] [+1] [+5] [⚕] [💀]         │
├──────────────────────────────────────────┤
│  Jugadores (sin HP, solo turno)          │
│  · Thalindra (Init 18) ✓                 │
│  · Durgin (Init 12) ✓                    │
├──────────────────────────────────────────┤
│            [ ► SIGUIENTE TURNO ]        │
└──────────────────────────────────────────┘
```

**Controles de HP:**
- Botones `[−5]`, `[−1]`, `[+1]`, `[+5]` — ajuste rápido
- Tap en el número de HP → `AlertDialog` con teclado numérico para valor exacto
- `[⚕ Curar]` — restaura HP completo
- `[💀]` — marca criatura como inconsciente (HP = 0, añade condición `UNCONSCIOUS`)

**Condiciones:**
- Tap en criatura → expande panel con `ConditionChips`
- Cada chip = condición activa; tap para activar/desactivar
- Color: verde = activo, gris = inactivo

**Iniciativa:**
- La lista está ordenada por `initiativeRoll + initiativeBonus` descendente
- Jugadores: solo nombre e iniciativa, sin HP (el DM no gestiona la vida de los PJs)
- El turno activo se resalta con borde primary + animación sutil

---

## 6. Domain — UseCases

```kotlin
// :core:domain/usecase/encounter/

class StartEncounterUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    // Copia el encuentro de biblioteca a la sesión (deep copy con nuevos IDs)
    // Tira iniciativa automáticamente (1d20 + bonus) para cada criatura
    // Activa hasActiveEncounter en SessionRepository
    suspend operator fun invoke(encounterId: Long, sessionId: Long): Encounter
}

class ApplyDamageUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    // Aplica delta de HP (negativo = daño, positivo = curación), clampea 0..maxHp
    suspend operator fun invoke(creatureId: Long, delta: Int)
}

class NextTurnUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    // Avanza currentTurnIndex; si llega al final → incrementa currentRound, vuelve a 0
    suspend operator fun invoke(encounterId: Long)
}

class EndEncounterUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository,
    private val sessionRepository: SessionRepository
) {
    // Marca encuentro como inactivo, desactiva hasActiveEncounter en la sesión
    suspend operator fun invoke(encounterId: Long, sessionId: Long)
}

class ToggleConditionUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    suspend operator fun invoke(creatureId: Long, condition: Condition)
}
```

---

## 7. EncounterRepository

```kotlin
// :core:domain/repository/EncounterRepository.kt
interface EncounterRepository {
    fun getAllEncounters(): Flow<List<Encounter>>
    fun getActiveEncounter(sessionId: Long): Flow<Encounter?>
    suspend fun saveEncounter(encounter: Encounter): Long
    suspend fun updateCreature(creature: EncounterCreature)
    suspend fun deleteEncounter(encounterId: Long)
    suspend fun startEncounterInSession(encounterId: Long, sessionId: Long): Encounter
    suspend fun endEncounter(encounterId: Long)
}
```

---

## 8. Integración con SessionScreen

### SessionUiState — cambios
```kotlin
data class SessionUiState(
    // ... campos existentes ...
    val hasActiveEncounter: Boolean = false,   // ya existe como stub
    val activeEncounter: Encounter? = null     // NUEVO
)
```

### SessionViewModel — cambios
```kotlin
fun startEncounter(encounterId: Long) {
    viewModelScope.launch {
        val encounter = startEncounterUseCase(encounterId, session.id)
        _uiState.update { it.copy(hasActiveEncounter = true, activeEncounter = encounter) }
    }
}
```

### CombatTab — composable en SessionScreen
```kotlin
// Dentro del HorizontalPager, page index 3 (dinámico)
if (uiState.hasActiveEncounter) {
    CombatTab(
        encounter = uiState.activeEncounter!!,
        onApplyDamage = { creatureId, delta -> viewModel.applyDamage(creatureId, delta) },
        onNextTurn = { viewModel.nextTurn() },
        onToggleCondition = { creatureId, cond -> viewModel.toggleCondition(creatureId, cond) },
        onEndEncounter = { viewModel.endEncounter() }
    )
}
```

El Tab 3 en el `TabRow` aparece/desaparece con `AnimatedVisibility` según `hasActiveEncounter`.

---

## 9. Jugadores en la Iniciativa

El DM puede añadir los PJs a la iniciativa manualmente (no se sincronizan con ningún perfil de personaje — en Fase 1 son solo nombres + valor de iniciativa).

**Modelo — PlayerInitiative (temporal, no persiste entre encuentros):**
```kotlin
data class PlayerInitiative(
    val name: String,
    val initiativeTotal: Int   // el DM ingresa el total directamente
)
```

Vive en `CombatTabViewModel` como `StateFlow<List<PlayerInitiative>>`, no en Room.
Se resetea al terminar el encuentro.

**UI:** Botón "Añadir jugador" en el `CombatTab` → dialog con nombre + campo numérico de iniciativa.

---

## 10. Roadmap de Implementación

### Sprint 16 — Encuentros Fase 1
```
Día 1:
  · Modelo Encounter + EncounterCreature en :core:model
  · Room: EncounterEntity, EncounterCreatureEntity, MIGRATION_8_9
  · EncounterRepository (interface) + EncounterRepositoryImpl
  · UseCases: StartEncounterUseCase, ApplyDamageUseCase, NextTurnUseCase, EndEncounterUseCase

Día 2:
  · :feature:encounters módulo Gradle
  · EncounterEditorScreen: formulario + lista de criaturas inline
  · EncounterEditorViewModel

Día 3:
  · CombatTab: lista de iniciativa, controles de HP, condiciones
  · EncounterListScreen: lista + botón Iniciar
  · Integración en SessionScreen (Tab 3 dinámico, SessionViewModel)

Día 4:
  · ConditionChips composable
  · Jugadores en iniciativa (PlayerInitiative temporal)
  · EncountersModule (Hilt)
  · Rutas en NavGraph.kt: EncounterListRoute, EncounterEditorRoute
  · BUILD SUCCESSFUL + smoke test
```

### Sprint 17 — Encuentros Fase 2 (mejoras, mismo sprint que NPCs)
```
  · Link bidireccional Encuentro ↔ NPC (NpcEncounterCrossRef)
  · "Añadir desde biblioteca de NPCs" en EncounterEditorScreen
  · Historial de encuentros por sesión (mostrar en SessionHistoryScreen)
  · Timer de ronda (opcional — configurable en Settings)
  · Plantillas de criatura guardables (reusar "Guardia AC14 HP52" en múltiples encuentros)
```

---

## 11. Decisiones de Producto Pendientes

| # | Pregunta | Opciones | Recomendación |
|---|----------|---------|---------------|
| E-1 | **¿El DM tira iniciativa manualmente o la app tira por él?** | (a) App tira 1d20+bonus automáticamente. (b) El DM ingresa el resultado. (c) Ambos — botón "Tirar auto" + campo editable. | Opción (c): auto con posibilidad de override |
| E-2 | **¿Los PJs tienen perfil con stats o solo nombre + iniciativa?** | (a) Solo nombre + iniciativa total. (b) PJ completo (AC, HP, clase). | Opción (a) en Fase 1 — la app es herramienta del DM, no de los jugadores |
| E-3 | **¿El tab Combate desaparece al terminar o queda como historial?** | (a) Desaparece inmediatamente. (b) Se queda como "Último combate" hasta comenzar otro. | Opción (b) — el DM puede necesitar revisar el estado final post-combate |
| E-4 | **¿Los encontres pertenecen a una campaña o son globales?** | (a) Globales (cualquier sesión los puede usar). (b) Organizados por campaña. | Opción (a) en Fase 1; campaña en Fase 3 cuando se implemente agrupación |
| E-5 | **¿HP de criaturas se resetea al re-usar el encuentro?** | Siempre debe resetearse al iniciar desde biblioteca. En la sesión activa los cambios persisten. | Al hacer `startEncounterInSession`, copiar con `currentHp = maxHp` siempre |

---

## 12. Criterios de Calidad

- Los controles de HP (`[−5]`, `[+1]`, etc.) deben tener **48dp de touch target** mínimo
- El avance de turno (`SIGUIENTE TURNO`) debe actualizar la UI en < 100ms (operación local)
- El damage no debe permitir valores negativos de HP (clamp a 0)
- Máximo 20 criaturas por encuentro en Fase 1 (limitación de UI — más de 20 necesitaría scroll)
- `ConditionChips` debe mostrar el nombre completo de la condición en su tooltip (long-press)
- Soporte dark theme completo — HP bars en rojo sobre fondo oscuro deben ser legibles

---

*Creado: 15 de abril de 2026*  
*Relacionado: `TOOLKIT_DESIGN.md` §Encuentros, `ROADMAP.md` Sprint 16, `DM_NOTES.md` §vínculos*
