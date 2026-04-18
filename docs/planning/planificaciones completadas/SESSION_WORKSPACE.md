# Plan: Workspace de Sesión - Clusters de Mazos (Bento Style)
*Creado: 15 de abril de 2026 | Sprint objetivo: 14.5 (encaja entre Sprint 14-Notas y Sprint 16-Encuentros)*

---

Este plan detalla la reestructuración del **Tab Mazos** dentro de `SessionScreen` para maximizar la organización visual y minimizar el scroll. Las cartas se agruparán automáticamente por mazo de origen en contenedores independientes.

> [!NOTE]
> Este plan aplica únicamente al Tab 0 (Mazos) de `SessionScreen`.
> El `HorizontalPager` multi-tab ya está implementado desde el Refactor del Sprint 4.
> No hay cambios en los módulos de Tablas, Notas ni Combate.

---

## Contexto y Motivación

### Problema actual
```
MazosTab actual:
┌────────────────────────────────────────────────────────────────→ scroll horizontal
│  [Carta 1]  [Carta 2]  [Carta 3]  [Carta 4]  [Carta 5]  [Carta 6]...
│  (mezcla de todos los mazos, sin organización visual)
```

Los DMs con múltiples mazos activos (ej: Monstruos + Hechizos + Encuentros) no pueden saber de un vistazo qué cartas pertenecen a qué mazo. Con 5+ cartas robadas, requiere scroll horizontal para ver todas.

### Solución propuesta: Workspace Bento
```
MazosTab rediseñado:
┌─────────────────────────────────────────────────────┐
│🎴 MONSTRUOS (2 en mano · 14 disponibles)       [▲] │ ← Header colapsable
│ ┌─────────┐ ┌─────────┐                             │
│ │ Dragón  │ │ Goblin  │                             │ ← FlowRow de cartas
│ └─────────┘ └─────────┘                             │
├─────────────────────────────────────────────────────┤
│🃏 HECHIZOS (0 en mano · 20 disponibles)        [▲] │ ← Slot vacío (sigue visible)
│ ┌─────────────────────────────────────────────────┐ │
│ │         Pulsa ROBAR para sacar una carta        │ │ ← Call-to-action
│ └─────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────┤
│🗂️ ENCUENTROS (1 en mano · 8 disponibles)       [▼] │ ← Cluster colapsado
└─────────────────────────────────────────────────────┘
           [ ◉ ROBAR DE MONSTRUOS ]  ← FAB contextual al cluster activo
```

---

## Decisiones de Diseño

| Decisión | Elección | Razonamiento |
|----------|----------|--------------|
| **Mazos vacíos** | Siempre visibles, con call-to-action | El slot es el punto de entrada para "Robar de este mazo" — elimina la DeckBar separada |
| **Colapso** | Manual con ícono de flecha | Predecible; el DM controla el espacio |
| **FAB** | Contextual al cluster tocado por último | Reduce la DeckBar a un historial de acción, sin necesidad de chips de selección |
| **Mesa Central** | NO en este sprint (Fase 2) | Añade complejidad; primero validar los clusters simples |
| **Tamaño de cartas** | 120dp alto (vs 160dp actual) | 25% más compacto para que quepan 2-3 cartas por fila |

---

## Archivos a Modificar

| Archivo | Cambio |
|---------|--------|
| `feature/draw/src/.../SessionViewModel.kt` | Añadir `collapsedDeckIds: Set<Long>`, `lastInteractedDeckId: Long?` a `SessionUiState`; función `toggleDeckCollapse()`, `setLastInteractedDeck()` |
| `feature/draw/src/.../SessionScreen.kt` | Refactorizar `MazosTab` → `DeckWorkspace`; implementar `DeckClusterItem`; adaptar FAB |
| `docs/planning/ROADMAP.md` | Añadir este sprint al historial de sprints y ajustar numeración |

**Sin cambios en:** Room schema, migraciones, DAOs, repositorios, modelos de dominio.

---

## Cambios en SessionUiState

```kotlin
data class SessionUiState(
    // ... campos existentes sin cambio ...

    // NUEVO: Set de IDs de cluster colapsados por el usuario
    val collapsedDeckIds: Set<Long> = emptySet(),

    // NUEVO: Para que el FAB sepa de qué mazo robar (antes era selectedDeckId)
    // null = usa el primero de deckRefs
    val lastInteractedDeckId: Long? = null,
)
```

> [!NOTE]
> `selectedDeckId` actual + `lastInteractedDeckId` nuevo son equivalentes.
> Se puede renombrar en lugar de añadir un campo nuevo para evitar duplicados.

---

## Cambios en SessionViewModel

```kotlin
/** Alterna el estado expandido/colapsado de un cluster de mazo. */
fun toggleDeckCollapse(stackId: Long) {
    _uiState.update { state ->
        val current = state.collapsedDeckIds
        val updated = if (stackId in current) current - stackId else current + stackId
        state.copy(collapsedDeckIds = updated)
    }
}

/** Registra el último cluster con el que interactuó el DM (para el FAB). */
fun setLastInteractedDeck(stackId: Long) {
    _uiState.update { it.copy(lastInteractedDeckId = stackId, selectedDeckId = stackId) }
}
```

---

## Componentes de UI

### DeckWorkspace (reemplaza MazosTab)
```
DeckWorkspace(
    deckRefs: List<SessionDeckRef>,
    handByDeck: Map<Long, List<Card>>,   ← hand.groupBy { it.stackId }
    collapsedDeckIds: Set<Long>,
    ...
) {
    LazyColumn {
        items(deckRefs) { ref ->
            DeckClusterItem(
                ref = ref,
                cards = handByDeck[ref.stackId] ?: emptyList(),
                isCollapsed = ref.stackId in collapsedDeckIds,
                onToggleCollapse = { onToggleCollapse(ref.stackId) },
                onCardTap = { ... },
                onCardDiscard = { ... },
                onInteract = { onSetLastInteracted(ref.stackId) },
            )
        }
    }
}
```

### DeckClusterItem
```
DeckClusterItem:
┌─ Surface con borde sutil ─────────────────────────────────────┐
│ Row (Header):                                                  │
│   [ícono de mazo o color de tag]  Nombre del Mazo             │
│   Texto sutil: "N en mano · M disponibles"                    │
│   Spacer(weight 1f)                                           │
│   IconButton(ExpandMore/ExpandLess)                           │
├───────────────────────────────────────────────────────────────┤
│ AnimatedVisibility(!isCollapsed):                             │
│   if (cards.isEmpty):                                         │
│     EmptyClusterHint: "Toca ROBAR para sacar una carta"       │
│   else:                                                       │
│     FlowRow (wrap automático) con cartas en CompactCardItem   │
└───────────────────────────────────────────────────────────────┘
```

### CompactCardItem (versión reducida de SwipeToDiscardCard)
- Alto: **120dp** (vs 160dp actual)
- Mantiene: swipe para descartar, tap para ver detalle, alpha si boca abajo
- Elimina: deckBadge (ya no necesario, visualmente dentro del cluster)

### FAB adaptado
```kotlin
// En SessionFab: el subtítulo muestra el mazo activo cuando hay >1 mazo
// El FAB sigue diciendo "ROBAR" pero su acción usa lastInteractedDeckId
val deckName = deckNames[lastInteractedDeckId ?: deckRefs.first().stackId]
LargeFloatingActionButton(...) {
    Text("ROBAR")
    if (deckRefs.size > 1) Text(deckName, style = labelSmall)
}
```

---

## Plan de Ejecución (Pasos en orden)

- [ ] **1. SessionUiState**: Añadir `collapsedDeckIds` y confirmar si `lastInteractedDeckId` es un rename de `selectedDeckId`
- [ ] **2. SessionViewModel**: Añadir `toggleDeckCollapse()` y `setLastInteractedDeck()`
- [ ] **3. CompactCardItem**: Extraer de `SwipeToDiscardCard` con altura de 120dp
- [ ] **4. DeckClusterItem**: Implementar con `AnimatedVisibility` para colapso
- [ ] **5. DeckWorkspace**: Reemplazar el body de `MazosTab` con el nuevo composable
- [ ] **6. FAB**: Ajustar el subtítulo para usar `lastInteractedDeckId`
- [ ] **7. Build**: `./gradlew assembleDebug`
- [ ] **8. DEVLOG + ROADMAP**: Registrar el sprint

---

## Integración con el Roadmap

Este workspace mejora directamente dos áreas del roadmap:

- **C-2 (Panel de config del mazo)**: Los headers del cluster son el punto natural para añadir un menú contextual por mazo (⁝) con acciones de "Configurar", "Barajar de vuelta", etc.
- **B-5 (Spread layout Tarokka)**: La "Mesa Central" descartada de este sprint se convertirá en la base del spread de Tarokka en Fase 3, donde las posiciones nombradas serían las celdas del layout.

### Propuesta de numeración
Este sprint encaja entre los ya planificados:

```
Sprint 14  — Notas de DM (Tab Notas en SessionScreen)   [PLANIFICADO]
Sprint 14.5— Workspace Bento (este plan)                 [NUEVO]
Sprint 15  — Notas por carta + gestión almacenamiento    [PLANIFICADO]
Sprint 16  — Encuentros y Combat Tracker                 [PLANIFICADO]
```

---

## Plan de Verificación

1. **Un solo mazo**: La UI no debe verse más compleja que la actual — un solo cluster que ocupa toda la pantalla, sin la DeckBar de chips separada.
2. **Tres mazos, sin cartas**: Los tres clusters aparecen con el call-to-action vacío. FAB dice "ROBAR · [Mazo 1]".
3. **Robar de Mazo 2**: La carta aparece en el cluster de Mazo 2. FAB cambia a "ROBAR · [Mazo 2]".
4. **Colapsar Mazo 1**: El cluster se minimiza al header. El espacio libre es ocupado por los clusters inferiores.
5. **5+ cartas en un cluster (FlowRow)**: Se reorganizan en dos filas automáticamente sin scroll horizontal.
6. **Undo**: La última carta vuelve al cluster correcto (no cambia de posición).
7. **Sin regresión**: Swipe para descartar, reveal, peek, noche, modo noche — todo funciona igual.

---

*Última actualización: 15 de abril de 2026*
