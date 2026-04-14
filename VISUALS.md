# DeckApp TTRPG — Planificación Visual

> Documento de diseño para mejoras de visualización de cartas y barajas.
> Ver `ROADMAP.md` para el contexto completo del proyecto.

---

## Diagnóstico del estado actual

### CardThumbnail (`:core:ui`)
- `ContentScale.Crop` recorta la imagen sin respetar el contenido de la carta
- El ancho es fijo (`height × 0.714f` = proporción estándar 2.5"×3.5"), pero no hay soporte para otros formatos (Tarot, landscape, cuadrado)
- Sin indicador visual de cuántas caras tiene la carta
- Sin indicador del modo de contenido (`IMAGE_ONLY` vs `TOP_BOTTOM_SPLIT` vs `REVERSIBLE`, etc.)
- El título se renderiza fuera de la imagen (ocupa espacio extra)

### CardViewScreen (`:feature:deck`)
- Para `DOUBLE_SIDED_FULL`: el flip es correcto, pero el botón "Voltear carta (cara X/Y)" es puramente textual; no hay tira de miniaturas de caras
- Para `FOUR_EDGE_CUES`: solo se ve UNA cue activa a la vez; no hay modo referencia con las 4 visibles simultáneamente
- Para `TOP_BOTTOM_SPLIT`: ambas zonas se muestran como columnas iguales a la derecha de la imagen, no como extensiones visuales de las zonas de la carta
- La imagen ocupa `weight(1f)` y el panel de texto ocupa altura variable — en modos con mucho texto la imagen queda muy pequeña
- No hay indicadores de posición (dots) cuando hay múltiples caras

### Sesión — cartas en mano
- Sin indicador visual de si la carta tiene cara activa vs cara oculta (boca abajo)
- Sin indicador del modo de contenido de la carta en mano
- La proporción de la miniatura en mano (`height = 160.dp`) no refleja el formato real del mazo

### Biblioteca — cobertura de mazos
- Cover con `ContentScale.Crop` puede recortar imágenes de portada
- No hay indicación visual del número de cartas del mazo
- No se diferencia visualmente un mazo con cartas bi-cara de uno simple

---

## Problema principal: "Varios lados activos"

Hay tres situaciones distintas donde el DM necesita ver múltiples lados simultáneamente:

### Situación 1 — Referencia completa de una carta multi-cara
El DM quiere revisar TODAS las caras de una carta de una vez, sin tener que flipear.
Ejemplo: antes de una sesión, chequeando qué dice el frente y dorso de una carta de Arkham Horror.

### Situación 2 — Orientación y pistas activas (FOUR_EDGE_CUES)
Story Engine Deck: la carta se coloca en la mesa y la pista activa es la más cercana al jugador.
El DM necesita ver las 4 pistas en disposición de brújula para decidir cómo orientar la carta.

### Situación 3 — Acciones de turno (TOP_BOTTOM_SPLIT)
Gloomhaven: SIEMPRE se ven las dos acciones simultáneamente. El jugador elige cuál ejecutar.
El split NO es secuencial; ambas zonas son activas al mismo tiempo.

---

## Mejoras propuestas

---

### V-1 — Aspect ratio configurable por mazo

**Problema:** Todas las cartas usan la misma proporción (2.5"×3.5" = 0.714). Las cartas de Tarot son más altas (2.75"×4.75" ≈ 0.579), y algunos mazos usan formato cuadrado o landscape.

**Nuevo campo:** `CardStack.aspectRatio: CardAspectRatio`

```kotlin
enum class CardAspectRatio(val widthRatio: Float, val heightRatio: Float) {
    STANDARD(2.5f, 3.5f),    // 0.714 — naipes estándar, Magic, Pokémon
    TAROT(2.75f, 4.75f),     // 0.579 — Tarot, Oracle decks
    MINI(1.75f, 2.5f),       // 0.700 — cartas mini
    SQUARE(1f, 1f),          // 1.000 — cartas cuadradas
    LANDSCAPE(3.5f, 2.5f)    // 1.400 — cartas horizontales (algunos mazos de rol)
}
```

**Impacto:**
- `CardThumbnail`: usa `aspectRatio` del mazo en vez de ancho fijo
- `DeckDetailScreen`: pasa el aspectRatio a las miniaturas
- `SessionScreen`: cartas en mano reflejan el formato real
- Selector de aspecto en el panel de config del mazo (ROADMAP C-2)

**Esfuerzo:** ★ | Room migration necesaria para nuevo campo.

---

### V-2 — ContentScale.Fit en CardThumbnail (con letterbox)

**Problema:** `ContentScale.Crop` recorta la imagen. Para cartas con texto en los bordes (Gloomhaven, Nord Games), el recorte puede ocultar información clave.

**Propuesta:** Cambiar a `ContentScale.Fit` con fondo `surfaceVariant` como letterbox.
Añadir parámetro `cropMode: Boolean = false` para contextos donde el crop es preferible (cover de mazo en la biblioteca).

```
Antes (Crop):       Después (Fit + letterbox):
┌──────────┐        ┌──────────┐
│████████  │        │░░░░░░░░░░│  ← letterbox (surfaceVariant)
│████████  │        │  ██████  │
│████████  │        │  ██████  │
│████████  │        │  ██████  │
└──────────┘        │░░░░░░░░░░│
                    └──────────┘
```

**Esfuerzo:** ★ — cambio puntual en `CardThumbnail.kt`.

---

### V-3 — Face indicator dots en CardThumbnail

**Problema:** No hay forma de saber si una carta tiene múltiples caras al mirarla en la grilla o en la mano.

**Propuesta:** Indicador de puntos superpuesto en el borde inferior de la imagen cuando `card.faces.size > 1`.

```
┌──────────┐
│          │
│  imagen  │
│          │
│    •◉•   │  ← dots overlay (•=inactivo, ◉=activo)
└──────────┘
```

**Implementación:**
- Overlay `Row` de `Box` circulares dentro del `Box` de imagen
- Tamaño: 5dp filled / 4dp unfilled, spacing 4dp
- Color: `onSurface.copy(alpha = 0.9f)` sobre fondo semitransparente
- Solo visible si `card.faces.size > 1`
- Refleja `card.currentFaceIndex`

**Esfuerzo:** ★

---

### V-4 — Modo de contenido badge en CardThumbnail (opcional)

**Problema:** Visualmente no se distingue una carta `IMAGE_ONLY` de una `TOP_BOTTOM_SPLIT` o `REVERSIBLE` en la grilla.

**Propuesta:** Chip pequeño en la esquina superior derecha con ícono del modo.

| Modo | Ícono |
|------|-------|
| `IMAGE_ONLY` | — (sin badge) |
| `IMAGE_WITH_TEXT` | `Notes` |
| `REVERSIBLE` | `SwapVert` |
| `TOP_BOTTOM_SPLIT` | `HorizontalRule` |
| `LEFT_RIGHT_SPLIT` | `VerticalAlignCenter` |
| `FOUR_EDGE_CUES` | `Explore` (brújula) |
| `FOUR_QUADRANT` | `GridView` |
| `DOUBLE_SIDED_FULL` | `FlipCameraAndroid` |

**Implementación:** chip pequeño 20×20dp, fondo `primaryContainer.copy(alpha = 0.85f)`.
Parámetro `showModeBadge: Boolean = false` para no contaminarlo por defecto.

**Esfuerzo:** ★

---

### V-5 — Tira de miniaturas de caras en CardViewScreen

**Problema:** Cuando una carta tiene múltiples caras, el DM solo ve el botón textual "Voltear carta (cara X/Y)". No hay forma rápida de ver qué hay en las otras caras.

**Propuesta:** Tira horizontal de miniaturas de caras, visible cuando `card.faces.size > 1`, reemplazando el botón de texto.

```
┌─────────────────────────────────────┐
│                                     │
│           [imagen cara activa]      │  weight(1f)
│                                     │
├─────────────────────────────────────┤
│  [▓▓] [░░] [░░]    ← tira de caras │  60dp height
│  ↑                                  │
│  cara activa (borde primary)        │
└─────────────────────────────────────┘
```

**Implementación:**
- `LazyRow` de `FaceThumbnailChip` (miniaturas 40×56dp con borde si es la cara activa)
- Tap en miniatura → `viewModel.jumpToFace(index)` (nuevo método, actualiza `currentFaceIndex`)
- Animación de slide entre caras como ya existe para `DOUBLE_SIDED_FULL`
- La tira es fija (no reemplaza al panel de texto/zonas)

**Esfuerzo:** ★★

---

### V-6 — Modo referencia: todas las caras en columna

**Problema:** Para preparar una sesión, el DM puede querer revisar todas las caras de una carta de forma continua sin tener que flipear una por una.

**Propuesta:** Toggle "Ver todas las caras" en el TopAppBar de `CardViewScreen` (ícono `ViewStream`).

Cuando está activo:

```
┌─────────────────────────────────────┐
│  [←]  Nombre  [⊞ Ver todas] [👁]   │  TopAppBar
├─────────────────────────────────────┤
│  CARA 1: Frente                     │  label
│  ┌─────────────────────────────┐   │
│  │       imagen cara 1         │   │  aspect ratio correcto
│  └─────────────────────────────┘   │
│  [texto / zonas de cara 1]          │
│                                     │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │  divider
│                                     │
│  CARA 2: Dorso                      │  label
│  ┌─────────────────────────────┐   │
│  │       imagen cara 2         │   │
│  └─────────────────────────────┘   │
│  [texto / zonas de cara 2]          │
└─────────────────────────────────────┘
         (scroll vertical)
```

**Implementación:**
- `showAllFaces: Boolean` como estado local en `CardViewScreen`
- Cuando activo: `LazyColumn` con un bloque por cara
- Cada bloque reutiliza `CardFaceImage` + el panel de zonas correcto para su modo
- Solo disponible si `card.faces.size > 1`

**Esfuerzo:** ★★

---

### V-7 — Vista brújula para FOUR_EDGE_CUES

**Problema actual:** Solo se ve una cue a la vez (la que corresponde a la rotación activa). Para el DM es difícil decidir cómo orientar la carta sin ver las 4 cues.

**Propuesta:** Vista de brújula que muestra las 4 cues alrededor de la imagen central.

```
           ┌─────────────┐
           │  [Cue Norte] │
           └──────┬──────┘
                  │
┌──────────┐ ┌───┴────┐ ┌──────────┐
│[Cue Oeste]│ │ imagen │ │[Cue Este] │
└──────────┘ └───┬────┘ └──────────┘
                  │
           ┌──────┴──────┐
           │  [Cue Sur]   │
           └─────────────┘
```

Activación: botón toggle "Vista brújula" / "Vista normal" en el TopAppBar (ícono `Explore`).

**Implementación:**
- Nueva composable `FourEdgeCompassLayout` en `CardViewScreen`
- Layout con `Box` + posicionamiento por `Alignment` (TopCenter, BottomCenter, CenterStart, CenterEnd)
- La imagen central ocupa ~50% del ancho
- Cada cue es un `Surface` card con `MarkdownText`
- El texto de la cue activa (según rotación) se resalta con color `primary`
- Tap en una cue → llama a `viewModel.setRotation(degrees)` para activarla

**Esfuerzo:** ★★

---

### V-8 — TOP_BOTTOM_SPLIT: layout expandido

**Problema actual:** Las dos zonas se muestran como dos columnas igual de pequeñas debajo de la imagen. Esto funciona para textos cortos pero no refleja visualmente la naturaleza de la carta (zona superior vs inferior).

**Propuesta:** Layout expandido que extiende visualmente la carta:

```
┌─────────────────────────────────────┐
│  ┌─────────────────────────────┐   │
│  │    zona superior (imagen)   │   │  mitad superior de la imagen
│  │    [título acción top]      │   │
│  └─────────────────────────────┘   │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │  divider (línea central)
│  ┌─────────────────────────────┐   │
│  │    zona inferior (imagen)   │   │  mitad inferior de la imagen
│  │    [título acción bottom]   │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

Alternativa más simple: hacer cada zona `expandible` con tap:
- Estado comprimido: `Row` de los dos textos como está hoy
- Estado expandido (tap en zona): la zona elegida se abre en una `BottomSheet` con la imagen recortada + texto completo

**Esfuerzo:** ★★ (layout expandido) / ★ (opción expandible con BottomSheet)

---

### V-9 — Deck cover como mosaico de cartas

**Problema actual:** La portada del mazo es una sola imagen (la primera carta importada).
Para mazos sin portada explícita, la imagen de la primera carta puede no ser representativa.

**Propuesta:** Opción de portada automática en mosaico 2×2:

```
┌──────────────────┐
│  ┌────┐ ┌────┐  │
│  │ C1 │ │ C2 │  │
│  └────┘ └────┘  │
│  ┌────┐ ┌────┐  │
│  │ C3 │ │ C4 │  │
│  └────┘ └────┘  │
└──────────────────┘
```

**Implementación:**
- `CardStack.coverMode: CoverMode = CoverMode.FIRST_CARD`
  - `FIRST_CARD`: comportamiento actual
  - `MOSAIC`: usa las primeras 4 cartas
  - `CUSTOM`: imagen elegida manualmente (si `coverImagePath != null`)
- `MosaicCoverImage` composable: `Box` 2×2 con `AsyncImage` usando `ContentScale.Crop`
- Disponible en el panel de config del mazo (ROADMAP C-2)

**Esfuerzo:** ★★

---

### V-10 — Stack visual de mazo en Biblioteca

**Problema actual:** Los mazos en la grilla de Biblioteca se ven como cards planas. No hay sensación de profundidad o de "pila de cartas".

**Propuesta:** Efecto de apilado visual con 2-3 cartas ligeramente desplazadas y rotadas detrás de la portada.

```
   ╱──────────╲
  ╱  ╱───────╲ ╲    ← carta 3 (más atrás, más rotada)
 ╱  ╱  ╱─────┐ ╲╲   ← carta 2
│  │  │      │  ││
│  │  │ cover│  ││   ← portada (frente)
│  │  │      │  ││
 ╲  ╲  ╲─────┘ ╱╱
  ╲  ╲───────╱ ╱
   ╲──────────╱
```

**Implementación:**
- `DeckCoverCard` en `:feature:library`: `Box` con 3 `Surface` apiladas
- Surface[2]: rotación -4°, translationX -6dp, translationY 4dp, alpha 0.5f
- Surface[1]: rotación -2°, translationX -3dp, translationY 2dp, alpha 0.7f
- Surface[0]: carta principal, sin transformación
- Solo el efecto de sombra de Surface[2] y [1]; no se carga imagen en ellas (rendimiento)
- Un toggle en Settings puede desactivar el efecto para usuarios que prefieren lo plano

**Esfuerzo:** ★

---

### V-11 — Contador de cartas badge en el cover del mazo

**Problema:** Para saber cuántas cartas tiene un mazo hay que entrar a `DeckDetailScreen`.

**Propuesta:** Badge numérico en la esquina superior derecha de cada cover de mazo en la Biblioteca.

```
┌──────────────────┐
│  ┌───┐           │
│  │52 │           │  ← badge con total de cartas
│  └───┘  imagen   │
│          cover   │
└──────────────────┘
```

**Implementación:**
- `LibraryViewModel` carga el count junto al stack (ya disponible via `cardDao.getCardsForStack`)
- Badge: `Surface` pequeño (24dp) con `BadgeDefaults` de Material3 en `topEnd` del `Box`

**Esfuerzo:** ★

---

## Resumen por pantalla

### LibraryScreen
| Mejora | Sprint sugerido |
|--------|----------------|
| V-9 Mosaico de portada | Sprint 4 |
| V-10 Stack visual de mazos | Sprint 4 |
| V-11 Badge de contador | Sprint 4 |

### DeckDetailScreen
| Mejora | Sprint sugerido |
|--------|----------------|
| V-1 Aspect ratio por mazo | Sprint 4 |
| V-2 ContentScale.Fit | Sprint 4 |
| V-3 Face indicator dots | Sprint 4 |
| V-4 Mode badge | Sprint 5 |

### CardViewScreen
| Mejora | Sprint sugerido |
|--------|----------------|
| V-5 Tira de miniaturas de caras | Sprint 5 |
| V-6 Modo referencia (todas las caras) | Sprint 5 |
| V-7 Vista brújula (FOUR_EDGE_CUES) | Sprint 5 |
| V-8 TOP_BOTTOM_SPLIT expandido | Sprint 5 |

### SessionScreen (cartas en mano)
| Mejora | Sprint sugerido |
|--------|----------------|
| V-1 Aspect ratio correcto | Sprint 4 |
| V-2 ContentScale.Fit | Sprint 4 |
| V-3 Face indicator dots | Sprint 4 |

---

## Decisiones de diseño a tomar

| # | Pregunta | Opciones |
|---|----------|---------|
| D-V1 | **¿ContentScale.Crop o Fit como default en thumbnails?** | Crop = se ven más ricas visualmente pero recorta. Fit = toda la carta visible pero con letterbox. Quizás Crop en biblioteca y Fit en sesión. |
| D-V2 | **¿Badge de modo en todas las vistas o solo en DeckDetail?** | Solo donde el DM está en modo "gestión" (DeckDetail) tiene sentido. En Sesión contaminaría. |
| D-V3 | **¿Vista brújula de FOUR_EDGE_CUES reemplaza o complementa la vista normal?** | Toggle: el DM elige cuál prefiere. La vista normal sigue siendo la default. |
| D-V4 | **¿El mosaico 2×2 es opt-in o el nuevo default para mazos sin portada explícita?** | Si no hay `coverImagePath`, el mosaico sería el default automático. Más informativo que la imagen de la primera carta. |
| D-V5 | **¿Efecto de stack en Biblioteca afecta el rendimiento en mazos grandes?** | El efecto es CSS-level (solo transform), no carga imágenes adicionales. Debería ser seguro. |

---

*Última actualización: 14 de abril de 2026*
