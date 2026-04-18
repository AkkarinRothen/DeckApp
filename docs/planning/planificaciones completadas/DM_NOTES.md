# DM Notes — Plan de Integración y Funciones

> **Propósito:** Planificar la evolución del sistema de notas del DM desde el campo `Session.dmNotes`
> actual hasta un sistema completo de gestión de información narrativa y de mesa.
>
> **Relación con el roadmap:** Expande D-1 del `ROADMAP.md` (Notas de DM por sesión).
> Consultar también `PLAN_GENERAL.md` para la visión macro del proyecto.

---

## 1. Estado Actual

| Elemento | Estado |
|----------|--------|
| `Session.dmNotes: String?` | ✅ En el modelo de dominio |
| `SessionEntity.dmNotes` | ✅ En Room (column sin migración pendiente) |
| UI para editar `dmNotes` | ❌ No implementada |
| Visualización en historial | ❌ Pendiente (SessionHistoryScreen es stub) |
| Notas por carta | ❌ No existe en el modelo |
| Diario de campaña | ❌ No existe |

---

## 2. Visión de Producto

El DM necesita capturar información **en contexto y sin fricción** durante la partida.
Las notas deben ser:

- **Rápidas de crear** — accesibles desde SessionScreen con un gesto
- **Ricas en contexto** — vinculadas a cartas, sesiones y elementos de campaña
- **Portables** — exportables a Markdown o texto plano
- **No invasivas** — nunca interrumpir el flujo de la partida

---

## 3. Funciones Planificadas

### 3.1 Notas por Sesión (Fase 1 — Sprint 14–15)

**Descripción:** Texto libre vinculado a una `Session`. Ya existe el campo, falta la UI.

**Flujo esperado:**
- Icono de bloc de notas en el TopAppBar de `SessionScreen`
- Toca → BottomSheet con `OutlinedTextField` multilínea (scroll interno)
- Guardado automático al cerrar (no requiere botón Guardar explícito)
- Las notas aparecen en `SessionHistoryScreen` debajo del timeline de eventos
- Preview truncado a 3 líneas con botón "Ver completo"

**Detalles técnicos:**
- `SessionViewModel.updateDmNotes(text: String)` — debounce de 500ms antes de persistir
- Persistir en Room mediante `SessionDao.updateDmNotes(sessionId, text)`
- Soportar Markdown básico en visualización (librería recomendada: `noties/Markwon`)
- El BottomSheet usa `ModalBottomSheet` de Material3

**Estimación:** ★ (< 1 día)

---

### 3.2 Notas por Carta (Fase 1 — Sprint 15)

**Descripción:** Campo de notas libre adjunto a cada `Card` individual.
Útil para anotaciones de roleplay: "Esta carta representa a Lord Strahd", "Jugador la recibió en sesión 3".

**Modelo:**
```kotlin
data class Card(
    // ... campos actuales ...
    val dmNotes: String? = null   // NUEVO
)
```

**Migración Room:** `ALTER TABLE cards ADD COLUMN dmNotes TEXT`

**Flujo esperado:**
- En `CardViewScreen`: botón de nota (icono lápiz pequeño) en el TopAppBar
- Abre BottomSheet con TextField multilínea
- Guardado al cerrar el sheet (debounce)
- En `DeckDetailScreen`: badge sutil (punto) en cartas que tienen notas

**Estimación:** ★★ (1 día)

---

### 3.3 Notas Rápidas (Quick Notes) (Fase 1 — Sprint 15)

**Descripción:** Captura ultra-rápida de texto durante la sesión sin abrir ningún editor completo.

**Flujo esperado:**
- FAB secundario en `SessionScreen` (o elemento en el overflow): "Nota rápida"
- Dialog minimalista: TextField de una línea + botón Añadir
- La nota se agrega al `dmNotes` de la sesión actual con un timestamp: `[21:34] texto...`
- Historial de notas rápidas visible en el BottomSheet de Notas por Sesión

**Estimación:** ★ (pocas horas)

---

### 3.4 Notas de Campaña / Diario (Fase 2)

**Descripción:** Sistema de notas independiente de sesiones individuales. Permite organizar
información de campaña: NPCs, lugares, secretos, arcos narrativos.

**Nuevo módulo:** `:feature:notes`

**Modelo de dominio:**
```kotlin
data class CampaignNote(
    val id: Long = 0,
    val title: String,
    val body: String,           // Markdown
    val category: NoteCategory, // NPC / LUGAR / ITEM / SECRETO / LIBRE
    val tags: List<String> = emptyList(),
    val linkedCardIds: List<Long> = emptyList(),    // vínculos a cartas
    val linkedSessionIds: List<Long> = emptyList(), // vínculos a sesiones
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class NoteCategory { NPC, LUGAR, ITEM, SECRETO, EVENTO, LIBRE }
```

**Pantallas:**
- `CampaignNotesScreen` — listado con búsqueda, filtro por categoría, chip de fijados
- `NoteEditorScreen` — editor Markdown con toolbar básica (negrita, cursiva, encabezados)
- Tab en BottomNav o item en el drawer (decisión de producto pendiente — ver §7)

**Room:**
- `CampaignNoteEntity` + `NoteTagCrossRef`
- `NoteCardCrossRef` para vínculos bidireccionales nota ↔ carta

**Estimación:** ★★★ (2–3 sprints)

---

### 3.5 Editor Markdown con Toolbar (Fase 2)

**Descripción:** Editor enriquecido para notas de campaña. No un procesador de texto completo,
sino una barra de formato contextual sobre el teclado.

**Toolbar (InlineContent sobre el teclado):**
```
[B] [I] [H1] [H2] [—] [Lista] [Tabla] [Link]
```

**Implementación:**
- Opción A: `Markwon` (display) + formato manual al insertar texto con `TextFieldValue`
- Opción B: `compose-richtext` / `halilibo/compose-richtext` (editor Compose nativo)
- Opción C: `WebView` con editor HTML (desaconsejado, pesado para uso en sesión)

**Recomendación:** Opción A es la más liviana. `Markwon` solo renderiza; la escritura es
un `OutlinedTextField` normal con botones que insertan sintaxis Markdown en la posición del cursor.

**Estimación:** ★★ (~1 sprint integrado en 3.4)

---

### 3.6 OCR de Notas Manuscritas (Fase 2 — extensión del feature de tablas)

**Descripción:** Tomar una foto de notas manuscritas y convertirlas a texto en una nota de campaña.

**Flujo esperado:**
- En `NoteEditorScreen`: botón "Importar de imagen"
- Lanza el crop tool de `TableImportScreen` (reusar el componente)
- ML Kit Text Recognition procesa la imagen
- El texto OCR se inserta en el editor en la posición del cursor
- El DM puede corregir antes de guardar

**Integración:** Reusar `ImportTableUseCase.getRawBlocks()` para obtener el texto crudo,
pero sin el paso de `AnalyzeTableImageUseCase` (no interesa la estructura de tabla).

**Estimación:** ★★ (reutilización de infraestructura OCR existente)

---

### 3.7 Vínculos Carta → Nota (Fase 2)

**Descripción:** Desde `CardViewScreen`, abrir la nota de campaña relacionada con esa carta.

**Flujo esperado:**
- En `CardViewScreen`: icono de nota en el TopAppBar si la carta tiene nota vinculada
- Tap → abre `NoteEditorScreen` con la nota correspondiente
- Desde `NoteEditorScreen`: sección "Cartas vinculadas" con thumbnails
- Tap en thumbnail → abre `CardViewScreen`

**Modelo:** `Card.dmNotes` (§3.2) para notas in-place; `NoteCardCrossRef` para vínculos
a `CampaignNote` (son dos sistemas distintos que coexisten).

**Estimación:** ★ (una vez que §3.2 y §3.4 estén implementados)

---

### 3.8 Export de Notas (Fase 2)

**Descripción:** Exportar notas a formatos externos para uso fuera de la app.

**Formatos:**
| Formato | Casos de uso |
|---------|-------------|
| `.md` (Markdown) | Obsidian, Notion, cualquier editor de texto |
| `.txt` (texto plano) | Compatibilidad máxima |
| `.pdf` (impresión) | Preparación de sesión en papel |
| ZIP con todo | Backup completo de notas de campaña |

**Implementación:**
- `ExportNotesUseCase(noteIds, format)` en `:core:domain`
- Para PDF: `PdfDocument` de Android o `iText` (licencia Apache)
- Compartir via `FileProvider` + `Intent.ACTION_SEND`

**Estimación:** ★★

---

### 3.9 Búsqueda Global (Fase 3)

**Descripción:** Una búsqueda que cruce sesiones, notas, cartas y tablas aleatorias.

**Flujo esperado:**
- Icono de búsqueda en la BottomNav o un SearchBar persistente
- Resultados agrupados por tipo: "Cartas (3)", "Notas (5)", "Tablas (1)"
- Tap en resultado navega al elemento correspondiente

**Implementación:**
- Room FTS (Full Text Search) — `@Fts4` en `CampaignNoteEntity` y `CardEntity`
- `SearchRepository` que agrega resultados de múltiples DAOs

**Estimación:** ★★★

---

## 4. Integraciones con Módulos Existentes

| Módulo | Integración |
|--------|-------------|
| `:feature:session` | `SessionViewModel` con notas por sesión (§3.1, §3.3) |
| `:feature:deck` | `CardViewModel` con notas por carta (§3.2, §3.7) |
| `:feature:tables` | Reusar OCR pipeline para notas manuscritas (§3.6) |
| `:feature:settings` | Estadísticas de notas en uso de almacenamiento |
| `:core:domain` | `CampaignNote`, `NoteRepository`, `ExportNotesUseCase` |
| `:core:data` | Room entities/DAOs para `CampaignNote` |
| `:core:ui` | `MarkdownText` composable (Markwon wrapper) compartido |
| NavGraph (`:app`) | Nueva ruta `notes/{noteId}` y tab en BottomNav (decisión §7) |

---

## 5. Roadmap de Implementación

### Fase 1 — Notas básicas integradas (Sprint 14–16)
```
Sprint 14 — Notas por sesión
  · [x] SessionScreen: BottomSheet de notas (§3.1)
  · [x] SessionViewModel.updateDmNotes() con debounce
  · [x] SessionHistoryScreen: mostrar dmNotes de sesiones finalizadas
  · [x] Notas rápidas (Quick Notes) con timestamp automático (§3.3)

Sprint 15 — Notas por carta
  · [x] Room migration: cards.dmNotes
  · [x] CardViewScreen: BottomSheet de nota (§3.2)
  · [x] DeckDetailScreen: badge visual en cartas con nota
  · [x] CardViewModel.updateCardNotes()

Sprint 16 — Integración con export
  · [x] Incluir dmNotes en ZIP export de mazos
  · [x] Mostrar notas en backup general (D-4 de ROADMAP)
```

### Fase 2 — Diario de campaña (Sprint 17–20)
```
Sprint 17 — Modelo y persistencia
  · Nuevo módulo :feature:notes
  · CampaignNote model + Room entities + NoteRepository
  · CampaignNotesScreen con lista y filtros básicos

Sprint 18 — Editor Markdown
  · NoteEditorScreen con toolbar de formato (§3.5)
  · Integrar Markwon para preview en tiempo real
  · Categorías y tags de notas

Sprint 19 — Vínculos y OCR
  · NoteCardCrossRef (§3.7)
  · OCR de notas manuscritas (§3.6)
  · Deep links internos nota ↔ carta ↔ sesión

Sprint 20 — Export y backup
  · ExportNotesUseCase: .md, .txt (§3.8)
  · Integración con el backup global (D-4)
```

### Fase 3 — Búsqueda global y widgets (Sprint 21+)
```
  · Room FTS en notas y cartas
  · SearchScreen global (§3.9)
  · Widget de nota rápida en home screen (extensión de D-5)
```

---

## 6. Dependencias Técnicas

| Librería | Propósito | Estado |
|---------|-----------|--------|
| `noties/Markwon` (v4) | Render de Markdown en Compose | Por añadir |
| ML Kit Text Recognition | OCR (§3.6) | Ya integrado en `:feature:tables` |
| `kotlinx.serialization` | Export JSON / backup | Ya en el proyecto |
| Room FTS (`@Fts4`) | Búsqueda global (§3.9) | Por añadir en Fase 3 |

**Agregar en `feature/notes/build.gradle.kts` (cuando se cree el módulo):**
```kotlin
implementation("io.noties.markwon:core:4.6.2")
implementation("io.noties.markwon:editor:4.6.2")   // toolbar helper
implementation("io.noties.markwon:ext-tables:4.6.2")
```

---

## 7. Decisiones de Producto Pendientes

| # | Pregunta | Opciones | Recomendación |
|---|----------|---------|---------------|
| N-1 | **¿Dónde aparecen las notas de campaña en la nav?** | (a) 5º ítem en BottomNav — rompe la regla de 4 ítems. (b) Dentro del overflow de `SessionScreen`. (c) Tab dentro de un futuro "Panel DM" (drawer lateral). | Opción (c) — drawer lateral emergente en `SessionScreen` a largo plazo; opción (b) como solución de Fase 1 |
| N-2 | **¿Markdown o texto enriquecido (HTML)?** | Markdown es portable y liviano. HTML permite más formato pero es más pesado. | Markdown — el DM lo escribe en plain text y se renderiza con Markwon |
| N-3 | **¿Notas por carta reemplazan o complementan `Card.subtitle`?** | `subtitle` es visible en la UI de juego; `dmNotes` es privado al DM. Son campos distintos. | Complementan — `subtitle` = visible en mesa; `dmNotes` = notas privadas del DM |
| N-4 | **¿Las notas de sesión se incluyen en el ZIP de export del mazo?** | Las notas son de sesión, no del mazo en sí. | No — van en el backup general (D-4), no en el ZIP por mazo |
| N-5 | **¿Las notas manuscritas OCR son destructivas (reemplazan) o aditivas (se añaden)?** | Reemplazar puede borrar notas existentes. Añadir es siempre seguro. | Siempre aditivas — el OCR se inserta en la posición del cursor |

---

## 8. Criterios de Calidad

- El BottomSheet de notas debe abrirse en < 200ms (sin animación lenta)
- El debounce de guardado no debe generar writes a Room con frecuencia > 1/500ms
- El campo `dmNotes` en Room debe usar `@ColumnInfo(name = "dm_notes")` para consistencia con el naming convention del schema
- El editor Markdown no debe causar `recomposition` completa en cada keystroke — usar `remember { mutableStateOf() }` local y sync al ViewModel solo en el debounce
- Todas las pantallas de notas deben soportar dark theme (default ON del proyecto)

---

*Creado: 15 de abril de 2026*  
*Relacionado: `ROADMAP.md` §D-1, `PLAN_GENERAL.md`, `ADVANCED_TABLES.md` (OCR reutilizado en §3.6)*
