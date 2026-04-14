# DeckApp TTRPG — Roadmap de Mejoras

> **Documento vivo.** Registrar aquí ideas, mejoras pendientes y decisiones de dirección antes de
> llevarlas a código. Ver `DEVLOG.md` para el historial de lo ya implementado.
>
> **Escala de impacto:** 🔴 Alto — 🟡 Medio — 🟢 Bajo  
> **Escala de esfuerzo:** ★★★ Grande (>1 sprint) — ★★ Medio (~1 sprint) — ★ Pequeño (<1 día)

---

## Estado actual del proyecto (14 de abril de 2026)

### ✅ Implementado y funcional
- Import desde carpeta SAF (imágenes → cartas, auto-detect de metadata de filename)
- Import desde ZIP y Export a ZIP
- PDF import con native PdfRenderer (4 modos de layout, grilla configurable, auto-trim de bordes)
- 8 modos de contenido de carta completos (renderizado + edición)
- CardEditorScreen multi-cara con picker de imagen y zonas editables
- SessionScreen: robo, descarte, undo, night mode, haptic, wake lock, multi-mazo
- Peek (ver tope sin robar), Deal (repartir N cartas), Mostrar a jugador (full-screen, brillo máximo)
- Tags en mazos + búsqueda y filtro en Biblioteca
- Duplicar mazo, Browse mode desde sesión
- Borrar y renombrar sesiones
- `:feature:settings` con uso de almacenamiento por mazo

### ⚠️ Incompleto o con deuda técnica
- PDF import usa `android.graphics.pdf.PdfRenderer` nativo en lugar de `PdfiumAndroid`
  (este último soporta más variantes de PDF y está ya en el plan)
- Room usa `fallbackToDestructiveMigration()` — correcto solo para dev, hay que migrar correctamente antes de release
- Tags cross-ref sin índice → full scan en updates (acceptables ahora, hay que resolverlo antes de mazos grandes)

---

## A — Deuda Técnica

| # | Ítem | Impacto | Esfuerzo | Notas |
|---|------|---------|----------|-------|
| A-1 | Migrar PDF rendering de `PdfRenderer` nativo a **PdfiumAndroid** | 🔴 | ★★ | El nativo falla con PDFs complejos (font embedding, transparencias). PdfiumAndroid ya está en el plan y en el `settings.gradle.kts` como jitpack dependency |
| A-2 | Room: strategy de migración real (quitar `fallbackToDestructiveMigration`) | 🔴 | ★★ | Antes del primer release a producción. Definir migraciones escritas o `AutoMigration`. Requiere configurar `room.schemaLocation` |
| A-3 | Índices en tablas cross-ref de tags (`card_tags`, `card_stack_tags`) | 🟡 | ★ | Actualmente full scan. Añadir `@Index` en las entities para `tagId` |
| A-4 | Compresión de imagen configurable en import | 🟡 | ★ | Actualmente JPEG quality=90 fijo. Exponer en Settings: Alta (95) / Media (85) / Baja (70) |
| A-5 | Manejo de errores visible en Import (fallo de archivo individual) | 🟡 | ★ | Si falla copiar una imagen concreta, hoy se silencia. Mostrar lista de archivos fallidos al final del import |

---

## B — Features Pendientes del Plan Original

### B-1 — Historial de sesión (SessionHistoryScreen) 🔴 ★★
La pantalla `SessionHistoryScreen` existe en el módulo pero es un stub. El `DrawEvent` log
ya contiene toda la información necesaria.

**Comportamiento esperado:**
- Accesible desde el listado de sesiones → tap en sesión finalizada
- Timeline vertical con íconos por acción (DRAW / DISCARD / FLIP / RESET…)
- Cada ítem: hora, nombre de la carta, imagen thumbnail, acción realizada
- Filtros: por mazo, por tipo de acción
- Estadísticas al final: total de cartas robadas, descartadas, tiempo de sesión

---

### B-2 — Modo boca abajo al robar (Face Down Draw) 🔴 ★
Robar una carta pero mostrarla tapada (se ve el dorso) hasta que el DM la toca para revelarla.
Muy común en mecánicas de sorpresa/misterio.

**Comportamiento esperado:**
- Toggle en la configuración del mazo: "Robar boca abajo / boca arriba"
- La carta en la mano muestra el dorso (imagen de dorso del mazo o imagen genérica)
- Tap en la carta en la mano → `FLIP` event → se revela el frente con animación
- `CardViewScreen` desde una carta boca abajo muestra directamente el dorso

**Detalles técnicos:**
- `Card.isRevealed: Boolean` (nuevo campo, Room migration necesaria)
- O reusar `currentFaceIndex`: si el dorso es `faces[1]`, empezar con `currentFaceIndex=1`
- Imagen de dorso configurable por mazo: `CardStack.backImagePath: String?`

---

### B-3 — Imagen de dorso por mazo 🔴 ★
Actualmente no hay forma de asignar una imagen de dorso global a un mazo.
Es esencial para el modo Face Down y para la presentación visual general.

**Comportamiento esperado:**
- En `DeckDetailScreen` (o pantalla de config del mazo): picker de imagen para el dorso
- El dorso se muestra en el `CardThumbnail` cuando `card.isDrawn = false` y se selecciona
- En `SessionScreen`, cartas boca abajo muestran este dorso

**Detalles técnicos:**
- `CardStack.backImagePath: String?` ya existe en el modelo pero no está expuesto en la UI
- Conectar en `DeckDetailScreen` via un `IconButton` o en un panel de configuración del mazo

---

### B-4 — Archivar mazos 🟡 ★
Ocultar mazos de la Biblioteca sin borrarlos. Útil para mazos de campañas anteriores.

**Comportamiento esperado:**
- Opción "Archivar" en el menú contextual del mazo (⋮)
- Biblioteca muestra solo mazos activos por defecto
- Toggle en filtros para mostrar archivados también
- El mazo archivado conserva todas sus cartas

**Detalles técnicos:**
- `CardStack.isArchived: Boolean` (nuevo campo, Room migration)
- `CardStackDao.getActiveDecks()` con `WHERE isArchived = 0`

---

### B-5 — Spread layout Tarokka (posiciones nombradas) 🟡 ★★★
Layout visual de una tirada de Tarokka (Cruz de 5 posiciones) en `SessionScreen`.

**Comportamiento esperado:**
- Modo "Tirada" accesible desde el overflow de sesión cuando el mazo activo es Tarokka
- Canvas con 5 posiciones nombradas: Pasado, Presente, Futuro, Obstáculo, Resultado
- Al robar, la carta se coloca en la siguiente posición libre
- Cada posición tiene etiqueta y puede mostrarse boca abajo (revelar individualmente)
- Vista de resumen de la tirada completa

**Alcance Fase 3** — requiere diseño UI propio.

---

### B-6 — Clonar sesión (reusar configuración) 🟡 ★
Crear una nueva sesión con los mismos mazos que una sesión anterior.
Muy útil para campañas con sesiones recurrentes.

**Comportamiento esperado:**
- Botón "Repetir sesión" en el historial de sesiones finalizadas
- Crea una nueva sesión con el mismo nombre + fecha + los mismos mazos
- Los mazos se resetean automáticamente al estado original

---

### B-7 — Auto-detección de grilla en PDF (OpenCV) 🟢 ★★★
Detectar automáticamente la grilla de corte en un PDF sin que el usuario tenga que
especificar el número de columnas y filas.

Librería: `zynkware/Document-Scanning-Android-SDK` (Kotlin, OpenCV lightweight)

**Aplica a Fase 2 tardía.** El flujo manual con preview funciona bien y cubre la mayoría de casos.

---

## C — Mejoras de UX

### C-1 — Reordenar cartas en DeckDetail (drag & drop) 🔴 ★★
Actualmente las cartas se muestran en orden `sortOrder` pero no se puede reordenar desde la UI.

**Comportamiento esperado:**
- Modo edición: botón "Reordenar" en el TopAppBar activa handles de drag
- Long-press en una carta entra en modo drag; arrastrar para reposicionar
- Al soltar: actualiza `sortOrder` de todas las cartas afectadas en Room

**Implementación:** `LazyVerticalGrid` de Compose no tiene soporte nativo de drag-and-drop.
Opciones:
1. `ReorderableItem` de la librería `sh.calvin.reorderable:reorderable` (Compose-first, activo)
2. Cambiar a `LazyColumn` para DeckDetail y usar la misma librería

---

### C-2 — Panel de configuración del mazo en DeckDetail 🔴 ★★
Actualmente no hay forma de cambiar el `DrawMode`, el `defaultContentMode`, ni la imagen de dorso
de un mazo existente desde la UI.

**Comportamiento esperado:**
- Nuevo ítem "Configurar mazo" en el DropdownMenu de DeckDetailScreen
- BottomSheet o pantalla nueva con:
  - Nombre del mazo (editable)
  - Descripción (editable, Markdown)
  - `defaultContentMode`: selector de 8 opciones
  - `DrawMode`: selector RANDOM / TOP / BOTTOM
  - Imagen de dorso: picker de imagen (ver B-3)
  - "Mostrar contador de cartas" toggle

---

### C-3 — Filtrar cartas por palo en DeckDetail 🟡 ★
Para mazos como Tarokka donde hay palos (Oros, Copas, Espadas, Bastos) o los mazos de
Paul Weber donde hay categorías (Monsters, Items, Spells).

**Comportamiento esperado:**
- Chip row bajo el header con los palos del mazo (extraídos de `card.suit`)
- Tap en chip → filtra la cuadrícula al palo seleccionado
- Chip "Todos" siempre presente

---

### C-4 — Snackbar con opción "Deshacer borrado" de mazo/carta 🟡 ★
Actualmente el borrado es inmediato e irreversible (con confirmación, pero sin recuperación).

**Comportamiento esperado:**
- Al borrar un mazo: mostrar Snackbar con botón "Deshacer" (5 segundos)
- Si el DM toca "Deshacer" antes del timeout: restaurar el mazo y sus imágenes
- Si el timeout expira: confirmar la eliminación definitiva

**Implementación:** diferir la eliminación real al cierre del Snackbar, cancelar el Job si
el DM pulsa deshacer.

---

### C-5 — Animación de barajar en SessionScreen 🟢 ★
Visual feedback cuando el DM baraja el mazo (shuffle).

**Comportamiento esperado:**
- Al llamar a "Barajar mazo": animación de 2-3 cartas del mazo superpuestas que se mezclan
- Opcional: sonido de barajar (AudioManager o SoundPool, silenciable)

---

### C-6 — Compartir imagen de una carta 🟢 ★
Desde `CardViewScreen`, opción de compartir la imagen de la cara activa como JPEG.

**Comportamiento esperado:**
- Botón de Share en el TopAppBar de CardViewScreen
- Usa `FileProvider` + `Intent.ACTION_SEND` con `image/jpeg`
- Comparte la imagen sin metadata del DM

---

## D — Features Nuevas (no estaban en el plan original)

### D-1 — Notas de DM por sesión 🔴 ★
Un campo de texto libre (Markdown) por sesión donde el DM puede tomar apuntes rápidos.

**Comportamiento esperado:**
- Ítem "Notas de sesión" en el overflow de `SessionScreen`
- BottomSheet con `TextField` Markdown + botón Guardar
- Las notas se muestran en el historial de sesión (ver B-1)
- `Session.dmNotes: String?` (nuevo campo)

---

### D-2 — Modo "Carta aleatoria" con filtro de palo 🟡 ★
Robar una carta aleatoria de un palo específico sin cambiar el DrawMode global.

**Comportamiento esperado:**
- En el overflow del mazo en `SessionScreen`: "Robar al azar por palo…"
- BottomSheet con chips de palos disponibles en el mazo seleccionado
- Roba una carta aleatoria del palo elegido que esté disponible

---

### D-3 — Timer de sesión 🟡 ★
Mostrar el tiempo transcurrido desde el inicio de la sesión activa.

**Comportamiento esperado:**
- En el TopAppBar de `SessionScreen`: texto sutil "2h 14m" junto al nombre
- Calculado como `System.currentTimeMillis() - session.createdAt`
- Se actualiza cada minuto (no en tiempo real)

---

### D-4 — Backup y restauración de la biblioteca 🔴 ★★
Crear una copia de seguridad completa de todos los mazos importados.

**Comportamiento esperado:**
- En Settings: "Crear backup" → exporta un ZIP grande con todas las imágenes + el schema de Room como JSON
- "Restaurar backup" → importa el ZIP y recrea toda la biblioteca
- Compatible entre instalaciones/dispositivos

**Dependencia:** requiere serialización del modelo de dominio a JSON (kotlinx.serialization ya en el proyecto).

---

### D-5 — Widget de Android para sesión activa 🟢 ★★★
Glance widget (API de widgets de Compose para Android) en la pantalla de inicio.

**Contenido del widget:**
- Nombre de la sesión activa
- Contadores de cartas disponibles
- Botón "ROBAR" que abre la app directamente en `SessionScreen`
- Se oculta cuando no hay sesión activa

**Aplica a Fase 3.** Requiere `androidx.glance:glance-appwidget`.

---

## E — Decisiones de Producto Pendientes

| # | Pregunta | Contexto | Opciones |
|---|----------|----------|---------|
| E-1 | **¿Sincronización en la nube?** | Supabase ya conocido del proyecto TDAPP. Permitiría continuar en tableta + teléfono. | (a) No, storage local siempre. (b) Supabase opt-in en Fase 3. (c) Backup/restore manual (D-4) como sustituto. |
| E-2 | **¿Nombre definitivo de la app?** | "DeckApp" es genérico. El package es `com.deckapp.ttrpg`. | Opciones a considerar: *Arcana*, *Vault* (TTRPG Vault), *TableDeck*, *Grimoire*, *Spellbook*... |
| E-3 | **¿Target mínimo API 26 o subir a API 28?** | API 26 = Android 8.0 (2017). API 28 = Android 9.0 (2018). | Subir a 28 permite `NotificationChannel` simplificado y mejoras de seguridad en file access. |
| E-4 | **¿Soporte de orientación horizontal en SessionScreen?** | En tablets o teléfonos en landscape, la mano horizontal queda muy estrecha verticalmente. | (a) Forzar portrait siempre (simple). (b) Layout adaptativo (complejo). (c) Config por sesión. |
| E-5 | **¿Procesamiento de PDF en foreground Worker o en coroutine?** | Actualmente el import se describe como WorkManager pero el codigo usa coroutines directas. | Resolver antes de mazos grandes (>100 páginas) donde el import puede tardar varios minutos. |

---

## Sprints Sugeridos (ordenados por impacto)

```
Sprint 4 — Configuración y pulido de mazos
  · C-2  Panel de config del mazo (DrawMode, defaultContentMode, imagen de dorso)
  · B-3  Imagen de dorso por mazo (exponer en UI)
  · B-2  Modo boca abajo al robar (Face Down Draw)
  · C-3  Filtrar cartas por palo en DeckDetail
  · A-3  Índices en cross-ref de tags

Sprint 5 — Historial y notas
  · B-1  SessionHistoryScreen (timeline de DrawEvents)
  · D-1  Notas de DM por sesión
  · B-6  Clonar sesión
  · D-3  Timer de sesión

Sprint 6 — PDF real + deuda técnica
  · A-1  Migrar a PdfiumAndroid
  · A-2  Room: migraciones escritas (quitar fallbackToDestructiveMigration)
  · A-4  Compresión de imagen configurable
  · A-5  Errores visibles en import

Sprint 7 — UX avanzada
  · C-1  Drag & drop para reordenar cartas
  · B-4  Archivar mazos
  · C-4  Snackbar con "Deshacer borrado"
  · D-4  Backup y restauración

Sprint 8 — Features de mesa (Fase 3)
  · B-5  Spread layout Tarokka
  · D-2  Robar al azar por palo
  · B-7  Auto-detección de grilla PDF (OpenCV)
  · D-5  Widget de Android
```

---

*Última actualización: 14 de abril de 2026*
