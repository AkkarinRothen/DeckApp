# Navegación — Rediseño de BottomNav y Arquitectura de Rutas

> **Propósito:** Planificar la evolución de la navegación desde el BottomNav de 4 ítems actual
> hacia la arquitectura multi-hub que soporta la suite completa de herramientas del DM.
>
> **Prerequisito de:** Sprints 16–18 (Encuentros, NPCs, Planificador, Wiki).
> Consultar `TOOLKIT_DESIGN.md` para la visión macro de navegación.

---

## 1. Estado Actual

```
BottomNav (4 ítems):
[ Biblioteca ]  [ Sesión ]  [ ◉ ROBAR ]  [ Ajustes ]
      1              2       FAB central       4
```

**Rutas existentes en `NavGraph.kt`:**
```
LibraryRoute
DeckDetailRoute(deckId)
CardEditorRoute(deckId, cardId)
CardViewRoute(cardId, sessionId)
ImportRoute
TableImportRoute
SessionSetupRoute
SessionRoute(sessionId)
SessionHistoryRoute(sessionId)
TablesListRoute
TableEditorRoute(tableId)
SettingsRoute
```

**Problema:** No hay lugar en la navegación para:
- Biblioteca de Encuentros
- Lista de NPCs
- Planificador de Sesiones
- Wiki de Campaña

Opciones: (a) añadir un 5º ítem al BottomNav, (b) agrupar todo en un hub "Preparar",
(c) drawer lateral.

---

## 2. Arquitectura Propuesta

### BottomNav — 5 ítems (con FAB central)

```
[ Biblioteca ]  [ Preparar ]  [ ◉ ACCIÓN ]  [ Sesión ]  [ Ajustes ]
      1               2        FAB centro         3             4
```

> **Nota:** El FAB central cambia de etiqueta e icono según la pantalla activa.
> El mandato de CLAUDE.md dice "exactamente 4 ítems" — esta es una **decisión de producto
> que requiere aprobación** (ver §6 Decisiones Pendientes). Se documenta aquí como propuesta.

**Alternativa conservadora — mantener 4 ítems con sub-navegación:**
```
[ Biblioteca ]  [ Sesión ]  [ ◉ ACCIÓN ]  [ Preparar ]  → reemplaza Ajustes
```
Ajustes pasa a vivir en el overflow (⋮) de cada pantalla + en Preparar.

**Alternativa minimalista — drawer lateral:**
```
[ Biblioteca ]  [ Sesión ]  [ ◉ ACCIÓN ]  [ Ajustes ]   ← sin cambio
  + Drawer desde hamburger en TopAppBar con acceso a Preparar
```

**Recomendación:** Hub "Preparar" como 4º ítem, reemplazando Ajustes.
Ajustes accesible desde el overflow del TopAppBar y desde dentro de Preparar.
Mantiene 4 ítems + FAB (respeta el mandato de CLAUDE.md).

---

## 3. Hub "Preparar" — Navegación Interna

El hub Preparar no es una pantalla única: es un `NavHost` anidado con sub-tabs.

```
┌─────────────────────────────────────────┐
│  Preparar                               │
├──────────┬──────────┬──────────┬────────┤
│ Encuentros│  NPCs   │ Sesiones │  Wiki  │  ← sub-tabs (TabRow)
├──────────┴──────────┴──────────┴────────┤
│                                         │
│   [contenido del sub-tab activo]        │
│                                         │
└─────────────────────────────────────────┘
```

Los sub-tabs son:
| Index | Label | Pantalla | Sprint |
|-------|-------|----------|--------|
| 0 | Encuentros | `EncounterListScreen` | 16 |
| 1 | NPCs | `NpcListScreen` | 17 |
| 2 | Sesiones | `SessionPlanListScreen` | 18 |
| 3 | Wiki | `WikiListScreen` | 18 |

El FAB en el hub Preparar es contextual por sub-tab:
- Encuentros → "Nuevo encuentro"
- NPCs → "Nuevo NPC"
- Sesiones → "Nueva sesión planificada"
- Wiki → "Nueva entrada"

---

## 4. NavGraph Actualizado

### Nuevas rutas a añadir

```kotlin
// :app/navigation/Screen.kt

// Hub Preparar
@Serializable object PrepareRoute
@Serializable data class EncounterListRoute(val sessionId: Long? = null)
@Serializable data class EncounterEditorRoute(val encounterId: Long = -1L)
@Serializable object NpcListRoute
@Serializable data class NpcDetailRoute(val npcId: Long)
@Serializable data class NpcEditorRoute(val npcId: Long = -1L)
@Serializable object SessionPlanListRoute
@Serializable data class SessionPlanEditorRoute(val planId: Long = -1L)
@Serializable object WikiListRoute
@Serializable data class WikiEntryRoute(val entryId: Long = -1L)
```

### BottomNav — nuevo ítem Preparar

```kotlin
// MainActivity.kt o NavGraph.kt
NavigationBarItem(
    selected = currentDestination?.hasRoute(PrepareRoute::class) == true ||
               currentDestination?.hasRoute(EncounterListRoute::class) == true ||
               currentDestination?.hasRoute(NpcListRoute::class) == true,
    onClick = { navController.navigate(PrepareRoute) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }},
    icon = { Icon(Icons.Default.AutoStories, contentDescription = null) },
    label = { Text("Preparar") }
)
```

### PrepareScreen — orquestador del hub

```kotlin
@Composable
fun PrepareScreen(
    onNavigateToEncounterEditor: (Long) -> Unit,
    onNavigateToNpcEditor: (Long) -> Unit,
    onNavigateToSessionPlanEditor: (Long) -> Unit,
    onNavigateToWikiEntry: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val tabs = listOf("Encuentros", "NPCs", "Sesiones", "Wiki")
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preparar") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, null)
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB contextual según pagerState.currentPage
            PrepareScreenFab(currentTab = pagerState.currentPage, ...)
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, label ->
                    Tab(selected = pagerState.currentPage == index,
                        onClick = { /* scroll to page */ },
                        text = { Text(label) })
                }
            }
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> EncounterListScreen(onNavigateToEditor = onNavigateToEncounterEditor)
                    1 -> NpcListScreen(onNavigateToNpcEditor = onNavigateToNpcEditor)
                    2 -> SessionPlanListScreen(onNavigateToEditor = onNavigateToSessionPlanEditor)
                    3 -> WikiListScreen(onNavigateToEntry = onNavigateToWikiEntry)
                }
            }
        }
    }
}
```

---

## 5. FAB Contextual — Comportamiento Completo

| Pantalla activa | FAB label | FAB icono | Acción |
|----------------|-----------|-----------|--------|
| LibraryScreen | Importar | `Add` | navega a ImportRoute |
| SessionScreen / tab Mazos | ROBAR | dado | `viewModel.drawCard()` |
| SessionScreen / tab Tablas | TIRAR | d20 | `viewModel.rollActiveTable()` |
| SessionScreen / tab Notas | NOTA | `Edit` | `viewModel.startQuickNote()` |
| SessionScreen / tab Combate | SIGUIENTE | `SkipNext` | `viewModel.nextTurn()` |
| PrepareScreen / Encuentros | Nuevo encuentro | `Add` | navega a EncounterEditorRoute(-1) |
| PrepareScreen / NPCs | Nuevo NPC | `PersonAdd` | navega a NpcEditorRoute(-1) |
| PrepareScreen / Sesiones | Nueva sesión | `Add` | navega a SessionPlanEditorRoute(-1) |
| PrepareScreen / Wiki | Nueva entrada | `Add` | navega a WikiEntryRoute(-1) |
| SettingsRoute | — | — | sin FAB |

---

## 6. Ajustes — Nueva Ubicación

Con Preparar reemplazando el BottomNav item de Ajustes:

- **Opción A (recomendada):** Ícono de engranaje en el `TopAppBar` de `PrepareScreen`
- **Opción B:** Ruta `SettingsRoute` sigue siendo un destino de nivel superior accesible
  desde el overflow (⋮) de `LibraryScreen` y `SessionScreen`
- **Opción C:** Ambas — ícono en PrepareScreen + overflow en las otras pantallas

La ruta `SettingsRoute` no desaparece del NavGraph, solo deja de estar en el BottomNav.

---

## 7. Deep Links y State Restoration

Con `saveState = true` y `restoreState = true` en cada `navigate()` del BottomNav,
el estado de cada hub se preserva al cambiar de tab.

**Ejemplo:** el DM está en PrepareScreen/NPCs buscando "Strahd", cambia a SessionScreen
para robar una carta, vuelve a Preparar — la búsqueda "Strahd" sigue activa.

Esto ya funciona con el patrón de Navigation Compose estándar; solo hay que asegurarse
de que todos los ViewModels que manejan estado de búsqueda/filtro sean `@HiltViewModel`
con scope de `NavBackStackEntry` correcto.

---

## 8. Módulo `:feature:prepare`

Para no sobrecargar `:app`, el hub Preparar vive en su propio módulo:

```
:feature:prepare/
  src/main/kotlin/com/deckapp/feature/prepare/
    PrepareScreen.kt       — orquestador de tabs (HorizontalPager)
    PrepareViewModel.kt    — estado mínimo: currentTab
    PrepareScreenFab.kt    — FAB contextual
```

Este módulo depende de `:feature:encounters`, `:feature:npcs`, `:feature:planner`,
`:feature:wiki` (a medida que se implementan).

---

## 9. Roadmap de Implementación

### Sprint 16 (junto con Encuentros)
```
  · Añadir PrepareRoute a NavGraph.kt
  · :feature:prepare módulo con PrepareScreen stub (solo tab Encuentros activo)
  · Reemplazar ítem Ajustes en BottomNav → Preparar (Ajustes al overflow del TopAppBar)
  · PrepareScreenFab contextual (solo Encuentros por ahora)
```

### Sprint 17 (junto con NPCs)
```
  · Activar tab NPCs en PrepareScreen
  · NpcListScreen integrada como page 1 del HorizontalPager
```

### Sprint 18 (Planificador + Wiki)
```
  · Activar tabs Sesiones y Wiki en PrepareScreen
  · SessionPlanListScreen y WikiListScreen como pages 2 y 3
```

---

## 10. Decisiones de Producto Pendientes

| # | Pregunta | Opciones | Recomendación |
|---|----------|---------|---------------|
| NAV-1 | **¿5 ítems en BottomNav o reemplazar Ajustes?** | (a) 5 ítems — rompe mandato CLAUDE.md. (b) Reemplazar Ajustes (Preparar en su lugar). (c) Drawer lateral. | Opción (b): Preparar reemplaza Ajustes; Ajustes al overflow |
| NAV-2 | **¿El label del BottomNav de Preparar cambia con el sub-tab activo?** | (a) Siempre "Preparar". (b) Cambia a "Encuentros" / "NPCs" / etc. | Opción (a): siempre "Preparar" — menos sorpresa para el usuario |
| NAV-3 | **¿SessionHistoryScreen está en Sesión o en Preparar?** | Historial de sesiones pasadas podría ir en Preparar/Sesiones. | Mantener en `:feature:session` como pantalla secundaria de la sesión — no en Preparar |
| NAV-4 | **¿Configuración de mazo (DrawMode, dorso) es ruta o BottomSheet?** | (a) Ruta `DeckConfigRoute`. (b) `ModalBottomSheet` desde DeckDetailScreen. | Opción (b): BottomSheet — evita una ruta extra para una operación contextual |

---

*Creado: 15 de abril de 2026*  
*Relacionado: `TOOLKIT_DESIGN.md` §Navegación, `ROADMAP.md` Sprints 16–18, `ENCOUNTERS.md`, `NPCS.md`*
