# DeckApp TTRPG — Planning Document

> **Documento vivo.** Actualizar con cada decisión de producto o arquitectura relevante.
> Ver `DEVLOG.md` para el historial detallado de cambios y `CLAUDE.md` para los mandatos técnicos.

---

## Visión del Proyecto

App Android nativa como caja de herramientas para Directores de Juego (DMs) de TTRPG.
**Fase 1:** Gestión y uso de mazos de cartas digitales durante partidas.
**Long-term:** Suite completa de herramientas para DMs (iniciativa, tablas de encuentros, etc.).

**Referencia funcional:** Foundry VTT Card System (v9+) + módulos Monarch, Complete Card Management,
Card Hand Mini Toolbar. Productos de referencia de contenido: Tarot, Story Engine Deck, Gloomhaven,
Arkham Horror LCG, Nord Games, Deck of Many, Tarokka (Curse of Strahd).

---

## Stack Tecnológico

| Capa            | Tecnología                          | Notas                                    |
|-----------------|-------------------------------------|------------------------------------------|
| Lenguaje        | Kotlin                              |                                          |
| UI              | Jetpack Compose + Material Design 3 | Dark theme por defecto                   |
| DI              | Hilt                                |                                          |
| DB local        | Room                                | Event log como fuente de verdad          |
| Arquitectura    | Clean Arch (MVVM + UseCases)        | Igual que TDAPP                          |
| Navegación      | Navigation Compose                  | Centralizado en :app NavGraph.kt         |
| Imágenes        | Coil                                |                                          |
| Markdown        | Markwon                             | Opcional — complementa imágenes          |
| PDF rendering   | PdfiumAndroid (barteksc)            | Para import de PDFs                      |
| PDF crop        | Kotlin puro + Bitmap API            | Recorte matemático, sin OpenCV en Fase 1 |
| OpenCV (Fase 2) | zynkware/Document-Scanning-SDK      | Auto-detección de grilla en PDFs         |

---

## Modelo de Dominio

### Tipos de Pila (CardStack)

| Tipo   | Descripción                                  | Visibilidad |
|--------|----------------------------------------------|-------------|
| `DECK` | Fuente principal. Baraja, reparte, resetea.  | DM          |
| `HAND` | Cartas en mano durante la sesión activa.     | Privada      |
| `PILE` | Descarte / cartas jugadas.                   | Pública      |

### Modos de Contenido de Carta (CardContentMode)

Basado en el análisis de productos del mercado TTRPG:

| Modo               | Producto de referencia      | Descripción                                          |
|--------------------|-----------------------------|------------------------------------------------------|
| `IMAGE_ONLY`       | Deck of Illusions, art puro | Solo imagen. Sin texto estructurado.                 |
| `IMAGE_WITH_TEXT`  | Nord Games, Paul Weber      | Imagen + título + cuerpo Markdown.                   |
| `REVERSIBLE`       | Tarot, Oracle decks         | Misma imagen, 2 textos: derecho e invertido (180°).  |
| `TOP_BOTTOM_SPLIT` | Gloomhaven ability cards    | Una cara con zona superior e inferior (2 acciones).  |
| `LEFT_RIGHT_SPLIT` | Variantes flip              | Una cara dividida izquierda/derecha.                 |
| `FOUR_EDGE_CUES`   | Story Engine Deck           | 4 pistas, una por orientación (0°/90°/180°/270°).    |
| `FOUR_QUADRANT`    | Compases narrativos         | 4 zonas en cuadrícula 2×2.                           |
| `DOUBLE_SIDED_FULL`| Arkham Horror LCG           | Ambas caras con contenido completo e independiente.  |

### Estructuras Core

```kotlin
enum class CardContentMode {
    IMAGE_ONLY, IMAGE_WITH_TEXT, REVERSIBLE,
    TOP_BOTTOM_SPLIT, LEFT_RIGHT_SPLIT,
    FOUR_EDGE_CUES, FOUR_QUADRANT, DOUBLE_SIDED_FULL
}

data class ContentZone(val text: String, val imagePath: String?)

data class CardFace(
    val name: String,               // "Frente", "Dorso", "Upright"...
    val imagePath: String?,         // imagen de fondo de la cara
    val contentMode: CardContentMode,
    val zones: List<ContentZone>,
    // REVERSIBLE: zones[0]=upright, zones[1]=reversed
    // TOP_BOTTOM: zones[0]=top, zones[1]=bottom
    // FOUR_EDGE:  zones[0..3] = N/E/S/W
    val reversedImagePath: String? = null
)

data class Card(
    val id: Long, val stackId: Long, val originDeckId: Long,
    val title: String, val suit: String?, val value: Int?,
    val faces: List<CardFace>,
    val currentFaceIndex: Int = 0,
    val currentRotation: Int = 0,  // 0/90/180/270
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

// Event log — append-only, fuente de verdad de sesión
data class DrawEvent(
    val id: Long, val sessionId: Long, val cardId: Long,
    val action: DrawAction, // DRAW, DISCARD, PASS, FLIP, ROTATE, REVERSE, RESET
    val timestamp: Long
)
```

### Flujo de estado de una carta

```
DECK (isDrawn=false)
    │  DM pulsa ROBAR (TOP / BOTTOM / RANDOM)
    ▼
HAND (isDrawn=true, sessionId=X)
    │  swipe izq / tap Descartar     │  tap Pasar
    ▼                                ▼
PILE (stackType=PILE)           PASADA (a jugador nombrado)
    │  "Barajar de vuelta"
    ▼
DECK (isDrawn=false, reinserción en fondo o aleatoria)
    │  "Resetear Mazo" (post-sesión)
    ▼
DECK (TODAS isDrawn=false, orden original restaurado)
```

---

## Estructura de Módulos Gradle

```
:app                    → Orquestador, NavGraph.kt, MainActivity
:core:model             → Todos los modelos de dominio + enums
:core:domain            → Interfaces de repo + UseCases
:core:data              → Room entities, DAOs, mappers, RepositoryImpl
:core:ui                → DeckAppTheme, CardThumbnail, componentes compartidos
:feature:library        → LibraryScreen + ViewModel
:feature:deck           → DeckDetailScreen + CardEditorScreen + CardViewScreen + VMs
:feature:draw           → SessionScreen + PileScreen + VMs
:feature:import         → ImportScreen + PDF processor + WorkManager Worker + VM
:feature:session        → SessionSetupScreen + SessionHistoryScreen + VMs
```

---

## Navegación

```
Bottom Nav (4 ítems):
[ Biblioteca ]  [ Sesión ]  [ ◉ ROBAR ]  [ Ajustes ]
      1              2       FAB central       4

Sin sesión activa → FAB = "Nueva Sesión"
Con sesión activa → FAB = "Robar carta"
```

---

## Pantallas — Resumen de Features

### LibraryScreen
- Cuadrícula de mazos (2 col default), toggle lista
- Buscar por nombre/tag; filtrar por sistema/sesión asignada/archivado
- Tap → DeckDetailScreen | Long-press → sheet (Añadir a sesión, Duplicar, Archivar, Eliminar)
- Multi-selección (batch archivar / añadir a sesión)
- FAB → ImportScreen

### DeckDetailScreen
- Cover image colapsable; cuadrícula de cartas reordenable
- Filtrar por palo; ordenar por índice/palo/valor/nombre
- Tap carta → CardViewScreen | Long-press → Editar/Eliminar/Mover
- Panel de config: DrawMode, dibujar boca arriba/abajo, defaultContentMode, imagen de dorso
- Overflow: Duplicar, Fusionar, Dividir por palo, Exportar ZIP, Archivar

### SessionScreen ← pantalla principal de juego
```
┌─────────────────────────────────┐
│ [Sesión]              [⋮ menú]  │
│ [Tarokka 42/54]  [Cond. 12/20]  │  ← Tabs de mazo
├─────────────────────────────────┤
│   [carta]  [carta]  [carta] ··· │  ← MANO (scroll horizontal)
│   swipe izq=descartar           │    tap=CardViewScreen
├─────────────────────────────────┤
│   ──────── ▲ Descarte (3) ───── │  ← PILE (tray colapsable)
└─────────────────────────────────┘
             [ ◉ ROBAR ]
```
- Persistencia: DrawEvent escrito en Room ANTES de animar
- Wake lock, undo (1 nivel), Night Mode overlay, haptic en robo

### CardViewScreen — rendering por ContentMode
| Modo               | Rendering                                                         |
|--------------------|------------------------------------------------------------------|
| `IMAGE_ONLY`       | Imagen full-screen, pinch-zoom                                    |
| `IMAGE_WITH_TEXT`  | Imagen + bottom sheet con Markdown                               |
| `REVERSIBLE`       | Toggle [↑ Derecho / ↓ Invertido] + texto correspondiente         |
| `TOP_BOTTOM_SPLIT` | Línea central divisoria; tap zona → expande                      |
| `FOUR_EDGE_CUES`   | Botón ↻ cicla entre las 4 pistas orientadas                      |
| `FOUR_QUADRANT`    | Overlay 2×2; tap cuadrante → expande                             |
| `DOUBLE_SIDED_FULL`| Tap → flip animation completa entre caras                        |

Acciones: [ Descartar ] [ Mantener ] [ Pasar ] [ Editar ]
Modo "Mostrar a jugador": full-screen, brillo máximo, sin UI de DM

### ImportScreen — Fuentes soportadas
- **Carpeta de imágenes** (SAF folder picker): subcarpetas → palos, auto-detect nombre de filename
- **PDF** (PdfiumAndroid): 4 modos de layout configurables con preview overlay
- **ZIP archive** (Fase 2)
- **Google Drive** (Fase 3)

### PDF Import — 4 modos de layout
```
Modo A: páginas alternas (frente / dorso / frente / dorso...)
Modo B: lado a lado en la misma página (┤frente│dorso├)
Modo C: grilla N×M por página (print-and-play estándar)
Modo D: primera mitad = frentes, segunda mitad = dorsos
```
Preview interactivo con grilla overlay antes de confirmar.
Procesamiento en background (WorkManager).

---

## Assets Disponibles como Demo

**Ubicación:** `I:\TTRPG\Visuales\Decks\The Ultimate Trove - Assets (Decks)\`

| Colección            | Contenido                                       |
|----------------------|-------------------------------------------------|
| Paul Weber           | Magic Items, Monsters, Spells, Equipment, NPCs  |
| Nord Games           | Critical Decks, Treasure Decks, Monster Decks   |
| Deck of Many         | Animated Spells, Items, Encounters              |
| Tarokka              | Tarots para Curse of Strahd                     |
| Pathfinder           | Cartas de sistema Pathfinder                    |
| Deck of Illusions    | Arte puro, criatures e ilusiones                |
| Card Backs           | Dorsos (No Logo, With Logo, Weber Logo)         |

~4,059 archivos, 160 carpetas.
**Estrategia:** SAF folder picker; imágenes copiadas a almacenamiento interno en import.

---

## Fases del Proyecto

### Fase 1 — MVP
Criterio: DM importa carpeta o PDF, crea sesión, roba cartas, descarta, resetea.

- [x] Setup proyecto Android Studio multi-módulo
- [x] Room schema v1: CardStack, Card, CardFace, ContentZone, DrawEvent, Session
- [x] `CardContentMode` enum en Room (guardado como String)
- [x] ImportScreen: SAF folder picker → Decks en Room
- [ ] Import desde PDF (PdfiumAndroid) — modos A/B/C/D con config de grilla + preview
- [x] Selector de `defaultContentMode` al confirmar import
- [x] LibraryScreen: cuadrícula de mazos
- [x] DeckDetailScreen: ver cartas del mazo
- [x] SessionScreen: FAB ROBAR + MANO horizontal + tray PILE
- [x] CardViewScreen: `IMAGE_ONLY`, `IMAGE_WITH_TEXT`
- [x] CardViewScreen: `REVERSIBLE` — toggle derecho/invertido
- [x] CardViewScreen: `TOP_BOTTOM_SPLIT` — zonas expandibles
- [x] Discard swipe + Reset deck
- [x] Screen wake lock en sesión
- [x] Dark theme por defecto

### Fase 2 — Contenido Enriquecido
- [x] CardViewScreen: `FOUR_EDGE_CUES`, `FOUR_QUADRANT`, `DOUBLE_SIDED_FULL`
- [x] Markdown rendering (Markwon) en todas las zonas
- [x] CardEditorScreen completo (selector de modo, editor de zonas, multi-face)
- [ ] Auto-detección de grilla PDF (OpenCV — zynkware/Document-Scanning-Android-SDK)
- [x] Undo última acción (DrawEvent log reversal)
- [x] Night Mode overlay (sesión con poca luz)
- [ ] Deal: repartir N cartas a jugadores nombrados (Pospuesto - DM Solo)
- [ ] Peek: ver top del mazo sin robar
- [ ] Modo "Mostrar a jugador" (full-screen carta, brillo máximo)
- [x] Tags + búsqueda/filtro de mazos
- [ ] Fusionar / Duplicar mazos
- [ ] Gestión de almacenamiento (Settings)
- [x] Import desde ZIP
- [ ] Historial de sesión + Archivo

### Fase 3 — DM Toolkit Expansion
- [ ] Spread layout Tarokka (posiciones nombradas en cruz de 5 cartas)
- [ ] Integración de iniciativa por carta (ordenar hand por card.value)
- [ ] Deck of Many Things enforcement mode
- [ ] Composite virtual deck (robo desde múltiples mazos mezclados)
- [ ] Clonar sesión (reusar configuración para sesión siguiente)
- [x] Exportar mazo como ZIP
- [ ] Google Drive import
- [ ] Agrupación por campaña
- [ ] TBD según necesidades descubiertas en sesión

---

## Decisiones Pendientes

- [ ] ¿Sincronización en la nube? (Supabase como en TDAPP sería natural en Fase 3)
- [ ] Target API Android mínima (propuesta: API 26 / Android 8.0)
- [ ] Nombre definitivo de la app
