### Separación individual de cartas y presets PnP 2×3 para The Story Engine y Deck of Worlds (23 de octubre de 2026)

**Categoría:** `FEATURE / STORY ENGINE / EXPANSIONS / PNP IMPORT / OCR PIPELINE / UX`

- **Regeneración de las 13 Expansiones en Cartas Individuales Auténticas:**
  - Se corrigió la extracción previa donde cada página completa (hoja de 6 cartas) se trataba como una única carta, dejando mazos con solo 10 cartas gigantes en lugar de cartas separadas.
  - Se implementó un algoritmo de corte 2×3 preciso por celda (650×650 px a 200 DPI, WebP optimizado ~40 KB) extrayendo las 6 cartas por pliego con emparejamiento dúplex invertido horizontalmente para los reversos (`back_col = 1 - col`).
  - **Paquete `story_engine_deck_of_worlds_expansions.json` (9 Mazos, 540 Cartas, 540 Caras, 1 Regla de Mundo):**
    - 3 Expansiones Cartográficas (Ártico, Costas, Desiertos): 60 cartas cada una (12 Regiones, 12 Puntos de Interés, 12 Apelativos, 6 Orígenes, 12 Atributos, 6 Acontecimientos).
    - 3 Expansiones Creativas: Ganchos de Aventura (60 cartas), Llaves de Cultura (60 cartas), Fragmentos de Lore (30 Opus + 30 Florecimientos).
    - 3 Expansiones de Género (Fantasía, Horror, Ciencia Ficción): 60 cartas cada una divididas en los 6 palos oficiales de Deck of Worlds.
  - **Paquete `new_story_engine.json` (4 Mazos, 300 Cartas, 300 Caras, 1 Regla de Narrativa):**
    - Potenciadores / Boosters: 120 cartas (24 Agentes, 24 Motores, 24 Anclas, 24 Conflictos, 24 Aspectos).
    - Trasfondos / Backstories: 60 cartas (30 Motores de Trasfondo, 30 Conflictos de Trasfondo).
    - Misterios / Mystery: 60 cartas (Agentes, Motores, Anclas, Conflictos, Aspectos).
    - Objetos Únicos / Unique Items: 60 cartas (12 Anclas, 48 Aspectos).
- **Extracción de Pistas con OCR y Localización al Español:**
  - Detección neuronal de pistas según el modo nativo de cada tipo de carta: `IMAGE_WITH_TEXT` para Regiones, `TOP_BOTTOM_SPLIT` para Puntos de Interés/Advents/Agentes/Motores/Anclas/Conflictos, y `FOUR_EDGE_CUES` (4 orientaciones cardinales) para Apelativos/Orígenes/Atributos/Aspectos/Ganchos.
  - Normalización de las pistas en `zonesJson` con texto traducido y mapeado a `List<ContentZone>`.
- **Soporte de Importación Dúplex en la App (`PdfLayoutProcessor` e `ImportViewModel`):**
  - Añadido preset `"Story Engine 2×3 PnP"` a `PDF_PRESET_TEMPLATES` en el importador de PDFs para hojas de 6 cartas cuadradas alternadas.
  - Modificado `PdfLayoutProcessor` para reflejar automáticamente la columna del dorso (`gridCols - 1 - col`) en modos `ALTERNATING_PAGES` y `FIRST_HALF_FRONTS`, garantizando que al importar cualquier PDF PnP dúplex los frentes y dorsos coincidan a la perfección.
  - Añadida prueba unitaria en `PdfLayoutProcessorTest` validando el reflejo de columnas dúplex.
- **Verificación:** Pruebas unitarias de `:core:domain:testDebugUnitTest` pasadas con éxito.

### Refactor interno del playmat (23 de octubre de 2026)

**Categoría:** `REFACTOR / PLAYMAT / COMPOSE`

- Se mantuvieron `PlaymatCanvasView` y sus dos firmas públicas, incluida la sobrecarga obsoleta. El composable principal conserva estado, gestos y callbacks.
- Se extrajo la escena espacial a `PlaymatSceneLayer`, manteniendo el `GraphicsLayer` de snapshot alrededor del fondo, mapa, grilla, plantillas y elementos de mesa.
- Se extrajeron controles flotantes a `PlaymatCanvasOverlays` y diálogos a `PlaymatDialogsHost`. Las capas reciben un snapshot inmutable de UI y eventos que escriben en el estado del orquestador.
- Verificación: `:feature:draw:compileDebugKotlin` y `:feature:draw:testDebugUnitTest` finalizaron correctamente. No se realizó revisión manual en dispositivo porque `adb` no está disponible en el entorno actual.

### Widget de sesión y mesa adaptable (22 de octubre de 2026)

**Categoría:** `FEATURE / UX / ANDROID WIDGET / RESPONSIVE`

- Añadido widget Android con Glance configurable por instancia: el usuario vincula cada widget con una sesión activa y puede mantener widgets distintos para campañas distintas.
- El widget abre la sesión vinculada; no ejecuta tiradas ni altera la partida. Si la sesión termina o se elimina, permite elegir otra. Si se configura sin sesiones activas, queda instalado en estado vacío y ofrece abrir la lista de sesiones.
- El estado de cada instancia se guarda en preferencias de Glance, no requiere migración de Room. La app actualiza los widgets cuando el flujo de sesiones cambia.
- `SessionScreen` ahora calcula layout compacto, mediano o expandido con el ancho disponible. Las ventanas medianas usan herramientas laterales de menor ancho; las hojas compactas aprovechan ventanas bajas.
- Actualizado `docs/planning/ROADMAP.md` para corregir estados ya completados y registrar esta tanda.
- Verificación: `:app:compileDebugKotlin` completó correctamente. Revisión visual e instalación en dispositivo quedan pendientes hasta que un Android/AVD esté conectado.

### Robo por palo y tirada Tarokka (22 de octubre de 2026)

**Categoría:** `FEATURE / UX / SESSION / DECKS / TAROKKA`

- Añadido el selector "Robar por palo" a los bloques de mazo. Solo muestra palos con cartas disponibles; el robo conserva la lógica normal de carta boca abajo y orientación, registra el evento y no modifica el modo global del mazo.
- Añadida la tirada Tarokka de cinco posiciones (Pasado, Presente, Futuro, Obstáculo y Resultado), con robo individual secuencial, revelado individual y reinicio confirmado. La asignación de cartas se conserva en la memoria persistente del tablero.
- Actualizado el roadmap para marcar como hechos Tarokka, clonación de sesión, notas de DM, temporizador y backup/restauración.
- Verificación: `:feature:draw:compileDebugKotlin`, `:feature:draw:testDebugUnitTest`, pruebas específicas de `DrawCardBySuitTest` y `BoardMemoryCodecTest`, y `:app:assembleDebug` finalizaron correctamente. Se revisó en el Motorola conectado una sesión existente, que no tenía bloques de mazo. El dispositivo se desconectó antes de poder instalar la compilación final y revisar los controles de cartas en pantalla.

### Feedback de barajado y compartir imágenes de cartas (22 de octubre de 2026)

**Categoría:** `FEATURE / UX / DECKS / SHARING / ANIMATION`

- El bloque grande de mazo muestra tres cartas animadas al barajar, recuperar descartes o confirmar un reinicio.
- CardView agrega compartir para la cara activa. Rasteriza la imagen a JPEG en caché y usa FileProvider con el selector del sistema; no incluye notas ni metadatos originales. Se desactiva si la carta está oculta o la cara no tiene imagen.
- `:feature:draw:compileDebugKotlin` y `:feature:deck:compileDebugKotlin` compilaron correctamente con fallback del compilador Kotlin tras la desconexión de su daemon; `:app:assembleDebug` finalizó correctamente.

### Recuperación de borrado de cartas y mazos (22 de octubre de 2026)

**Categoría:** `FEATURE / UX / DECKS / UNDO`

- El borrado de cartas y mazos queda pendiente durante cinco segundos y se ofrece la acción “Deshacer” en una notificación visible.
- Si se deshace, los datos e imágenes se conservan; al vencer el plazo se completa la eliminación. El mazo solo vuelve al listado al cancelar antes de eliminarlo.
- Confirmado que el reordenamiento persistente de cartas y el filtro por palo ya estaban disponibles; se actualizaron sus estados en el roadmap.
- `:feature:deck:compileDebugKotlin --offline --no-configuration-cache --max-workers=1` y `:app:assembleDebug --offline --no-configuration-cache --max-workers=1`: compilaciones correctas.

### Organización y navegación del tablero de sesión (22 de octubre de 2026)

**Categoría:** `FEATURE / UX / SESSION / BOARD MEMORY`

- Añadido un resumen del tablero con conteos por grupo, aviso para grupos vacíos y navegación directa entre vistas y carpetas.
- Añadidas sugerencias para revisar bloques duplicados, sin recurso vinculado o sin uso registrado durante 30 días. Quitar un bloque desde las sugerencias requiere confirmación y no elimina el recurso de la biblioteca.
- Persistidos los bloques, grupos, actividad y modo del tablero por sesión; se conservan los valores predeterminados al leer memorias antiguas.
- Añadidas plantillas personalizadas reutilizables entre sesiones y un historial de hasta 20 cambios del tablero con acción para deshacer.
- Incorporados los modos Preparar y Partida; el modo Partida oculta los controles de edición y conserva la disposición del tablero.
- Verificación: `:feature:draw:compileDebugKotlin --offline`, `:feature:draw:testDebugUnitTest --offline` y `:app:assembleDebug --offline` completaron correctamente. Las tres pruebas nuevas de `BoardMemoryCodecTest` pasaron; la suite completa de `:core:data:testDebugUnitTest` tuvo una falla en `ChoirOfFleshPackGenerator` por un archivo no encontrado. Se limpió y recompiló `feature:settings` tras detectar artefactos incrementales obsoletos. Instalado en el dispositivo conectado y revisados el resumen, el acceso a la sesión y el modo Partida; la fila de controles se desplaza horizontalmente en móvil.

### Mejora de mesa: bloques de cartas y mazos de sesión (21 de octubre de 2026)

**Categoría:** `FEATURE / UX`

- Los bloques de cartas muestran mano, mazo y descarte por separado; el robo y la revelación tienen acciones visibles en tamaños compactos y grandes.
- Cada carta en mano ofrece un control explícito para descartarla. El carrusel ya no descarta al deslizar horizontalmente.
- Las imágenes de frente y dorso se ajustan al espacio de la carta sin recortar sus bordes.
- La vista de mazos muestra los nombres completos al expandir, un estado de mano vacía, el recuento de descartes y la acción de recuperarlos cuando corresponde.
- Verificación: `:feature:draw:compileDebugKotlin --offline` completó correctamente.

### Implementación Plan Priorizado de Mejoras UX: Fase 4 — Preparación, Biblioteca y Protección de Datos (21 de octubre de 2026)

**Categoría:** `BACKUP / EXPORT / IMPORT / PDF / TEMPLATES / STORAGE INTEGRITY / RELEASE`

- **Exportador Ligero y Completo de Mazos (`DeckDetailScreen.kt`, `DeckDetailViewModel.kt`, `ExportDeckToJsonUseCase.kt`):**
  - Incorporado botón directo de compartir en la `TopAppBar` del detalle del mazo que despliega un diálogo modal de exportación (`DeckExportDialog`).
  - Opciones de exportación:
    - `[📦 ZIP Completo (con arte)]`: Archivo ZIP autónomo que incluye todas las imágenes y un manifiesto `deck_manifest.json` completo con tags, valores, palos y notas del director.
    - `[📄 JSON Ligero (solo datos)]`: Manifiesto ligero en JSON exportable por SAF (`writeTextToUri`), ideal para compartir configuraciones de mazo por mensajería o respaldo rápido sin duplicar megabytes de imágenes.

- **Inspección Previa de Backup e Integridad (`SettingsScreen.kt`, `SettingsViewModel.kt`, `FileRepositoryImpl.kt`):**
  - Creado `inspectBackupZip` en `FileRepositoryImpl`: lee y parsea `manifest.json` en stream (`ZipInputStream`) sin necesidad de descomprimir archivos en disco.
  - Diálogo modal de inspección previa en `SettingsScreen`: muestra versión del backup, fecha de creación, tamaño comprimido y un desglose de entidades (mazos, cartas, tablas, PNJs, wiki, sesiones, mapas).
  - Verificación de almacenamiento: consulta `usableSpace` vía `StatFs` y compara el espacio disponible frente al requerido, impidiendo restauraciones incompletas si el almacenamiento es insuficiente.

- **Importador de PDF Potenciado con Rango de Páginas y Plantillas (`PdfLayoutProcessor.kt`, `ImportDeckUseCase.kt`, `ImportViewModel.kt`, `ImportScreen.kt`):**
  - *Rango de Páginas Ergonómico:* Selector `Desde pág.` a `Hasta pág.` con botón `[Todas]` para restablecer a `1..total` y validación de rangos inválidos (`start > end`). El procesamiento acota la extracción únicamente a las páginas deseadas en los 4 modos de layout.
  - *Plantillas Rápidas de Rejilla:* Chips horizontales de 1 toque con preconfiguraciones listas:
    - `3×3 Alternado`: Print & play estándar (páginas impares frentes, pares dorsos).
    - `3×3 Solo Frente`: 9 cartas por página, 1 cara.
    - `Side-by-Side PNJ`: 50/50 mitad izquierda arte y mitad derecha texto.
    - `1×1 Página Completa`: 1 carta grande por página.
    - `1ra Mitad / 2da Mitad`: Primera mitad frentes, segunda mitad dorsos.
  - *Estimación Dinámica y Chequeo de Espacio:* Cálculo en tiempo real de cartas estimadas y advertencia visual si el espacio libre en el dispositivo es bajo.
  - *Suite de Pruebas Unitarias:* Creada la suite `PdfLayoutProcessorTest` en `:core:domain`, validando al 100% la extracción por rangos y bordes seguros.

- **Verificación Técnica y Compilación Release:**
  - 100% de tests unitarios aprobados en `:core:domain:testDebugUnitTest` y `:feature:draw:testDebugUnitTest`.
  - Compilación Release completada (`:app:assembleRelease`, APK de 154.9 MB).

---

### Implementación Plan Priorizado de Mejoras UX: Fase 3 — Unificar Consulta y Registro en Partida (21 de octubre de 2026)

**Categoría:** `UX / NPC / BENTO / DICE / SESSION HISTORY / AUTOSAVE / DEBOUNCE / MODAL BOTTOMSHEET / RELEASE`

- **Ficha de PNJ Interactiva Bento (`NpcDetailSheet.kt`):**
  - *Bloque Bento de Salud Dinámica:* Barra visual de vida reactiva animada con cambio de color según el estado (verde > 50%, ámbar > 20%, rojo ≤ 20%).
  - *Ajuste rápido de HP con 1 toque:* Controles táctiles `-5`, `-1`, `+1`, `+5` con persistencia automática en base de datos vía `onUpdateHp` (`viewModel.updateNpcHp`).
  - *Tiradas de 1 toque integradas a la sesión:*
    - PNJ d20 / Genérico: Botón directo `[Tirar Inic]` que evalúa `1d20 + iniciativa` y lo envía al registro de tiradas de la sesión.
    - Choir of Flesh Criatura: Tarjetas Bento para Ataque Estándar y Ataque Especial con botón directo de dado `[Tirar 🎲]` que evalúa la tirada en la mesa.
    - Choir of Flesh Inquebrantable: Insignias de atributos interactivas (`FUE`, `DES`, `CON`, `INT`, `VOL`, `PRE`) táctiles que tiran `1d20 + mod` al tocarlas.
  - *Pestañas limpias:* Separación fluida entre estadísticas mecánicas inmediatas, Descripción libre de marcas internas y Notas / Reglas.

- **Visor Consistente de Reglas y Tablas en ModalBottomSheet (`WikiQuickPreviewDialog.kt`):**
  - Transformado el diálogo flotante clásico en un `ModalBottomSheet` interactivo deslizable, homogéneo con `TableDetailSheet` y `NpcDetailSheet`.
  - Botón de 1 toque `[Enviar al Diario / Notas de Sesión]` (`onSendToNotes`) que inserta la regla o artículo directamente en la bitácora activa sin abandonar el contexto de la mesa.
  - Soporte de copia rápida con feedback visual y vista de imagen táctil ampliable.

- **Historial de Sesión Reorganizado con Filtros Rápidos (`SessionHistoryScreen.kt` & `SessionHistoryViewModel.kt`):**
  - Sustituida la sobrecarga de menús desplegables por una barra horizontal compacta de chips de filtro: `[Todos | 🎲 Dados y Tablas | 🃏 Cartas | ⚔️ Combate | ⏰ Relojes]`.
  - Integración de `SessionLogRepository`: el historial unifica ahora eventos de cartas (`DrawEvent`), tablas aleatorias (`TableRollResult`) y eventos de bitácora (`SessionLogEntry`: dados, oráculos/Mythic, relojes y combate).
  - Algoritmo de desduplicación inteligente: descarta eventos redundantes o duplicados concurrentes (<500ms) entre tiradas de tablas y entradas de log.
  - Eliminadas por completo las etiquetas con valores `null`.

- **Recursos de Sesión Priorizados (`SessionBoardBlockPickerSheet.kt`):**
  - Las tablas y recursos vinculados a la sesión aparecen arriba en la lista con insignia visual distintiva `⭐ En sesión`, seguidos por el resto de la biblioteca clasificada.

- **Notas de Sesión con Autoguardado Continuo y Control de Teclado (`PlaymatSessionDrawer.kt`):**
  - Implementado debounce de 400ms con coroutines para autoguardado suave y reactivo, eliminando escrituras innecesarias en base de datos en cada tecla.
  - Indicador visual dinámico en la cabecera: `Guardando...` / `Guardado ✓` para máxima tranquilidad del director de juego.
  - Botón de cierre rápido de teclado (`KeyboardHide`) para despejar la vista táctil en orientación vertical u horizontal.
  - Guardado forzado garantizado en `DisposableEffect.onDispose` ante rotación o cierre del cajón.

- **Verificación Técnica y Despliegue:**
  - 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest`.
  - Compilación Release completada (`:app:assembleRelease`, APK de 154 MB).
  - Instalación y despliegue exitoso vía ADB en el dispositivo físico `ZE222B5GXX` (`com.deckapp.ttrpg` ejecutando con éxito, PID 26044).

---

### Implementación Plan Priorizado de Mejoras UX: Fases 1 y 2 (21 de octubre de 2026)

**Categoría:** `UX / LAYOUT / LANDSCAPE / HUD / EMPTY STATES / COMBAT / HEXPLORATION / WIKI / LOGGING / RELEASE`

- **Fase 1 — Layout Adaptable y Despeje de Canvas en Móvil y Tablet:**
  - *Landscape sin obstáculos:* Se eliminaron los dos rieles laterales que asfixiaban la pantalla y comprimían el área de juego. El margen izquierdo quedó 100% despejado para el lienzo central (`centralWorkspaceContent`).
  - *Riel Unificado a la Derecha (`SidebarNavigationRail.kt`):* Se unificaron todas las herramientas (Mesa, Tablas, Combate, Notas, Diario, PNJs, Wiki, Plan, Reglas, Mythic) en un único riel scrollable de 56dp anclado al margen derecho con espaciado optimizado para pantallas bajas de teléfonos.
  - *BentoSidebar Unificado:* Al abrir cualquier herramienta (proceda de herramientas primarias o de lore), se abre un único panel lateral a la derecha con ancho elástico (`280.dp - 340.dp`), alternando sin fricción ni superposiciones y cerrándose limpiamente con un solo toque.
  - *Franja Slim Adaptativa de Contexto (`SessionTopHudCapsule.kt`):* Altura fija no invasiva de ~36dp con chips dinámicos de contexto activo (Escena actual con acceso directo a selector, Combate con indicador de turno, Relojes de tensión en alerta, Hilos de Mythic). El detalle extendido se abre en un `ModalBottomSheet` para no tapar los bloques ni el canvas central.
  - *Selector Categorizado en Portrait (`SessionBottomBar.kt`):* Sustituida la barra horizontal desbordante por 3 categorías limpias: `Mesa`, `Lore` y `Combate` con menús desplegables e indicadores visuales de estado activo.

- **Fase 2 — Estados Vacíos Accionables y Corrección de Referencias:**
  - *Combate sin encuentro activo (`CombatEmptyState.kt`):* Permite iniciar Combate Rápido inmediato con 1 toque O cargar de forma directa cualquiera de los encuentros preparados de la campaña con badge de criaturas y botón de inicio.
  - *Hexploración sin mapa (`SessionScreen.kt`):* Incorporadas dos acciones directas sin requerir salir a ajustes: `[🗺️ Crear Mapa]` (genera e inicializa un mapa 10x10 vinculado al instante) y chips desplazables horizontalmente para vincular mapas preexistentes con 1 toque.
  - *Wiki y Lore (`WikiTab.kt` y `WikiViewModel.kt`):* Añadido botón de acción directa `[➕ Crear Primer Artículo]` en el estado vacío y botón '+' persistente en la barra de búsqueda para redactar nuevos artículos con soporte de hipervínculos `[[Título]]` mediante un diálogo modal ágil.
  - *Historial y Exportación de Log (`SessionHistoryScreen.kt` y `SessionCombatDelegate.kt`):* Corregido el bug de presentación donde eventos sin carta imprimían `Carta #null` o `Carta null`. Ahora se formatea de manera limpia omitiendo campos nulos o vacíos.

- **Verificación Técnica y Despliegue:**
  - 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest`.
  - Compilación Release no-dev (`:app:assembleRelease`) completada con éxito (`BUILD SUCCESSFUL`).
  - Instalación y despliegue exitoso vía ADB WiFi en el dispositivo físico (`com.deckapp.ttrpg` PID activo 13532).

---

### Refactorización Arquitectónica de Sesión: Extracción de NpcSessionUiState (20 de octubre de 2026)

**Categoría:** `REFACTOR / ARCHITECTURE / SESSION / NPCS / VOICES / PERMANENT PLAYERS / STATE MANAGEMENT / CLEAN CODE`

- **Extracción y Desacoplamiento de Personajes No Jugadores y Jugadores Permanentes (`NpcSessionUiState`):**
  - Creado `com.deckapp.feature.draw.model.state.NpcSessionUiState` para encapsular las 4 variables de NPCs, audio de voz y jugadores permanentes en el tablero (`npcs`, `playingNpcVoicePath`, `npcCount`, `permanentPlayers`).
  - Incorporado `val npc: NpcSessionUiState = NpcSessionUiState()` en `SessionUiState`.
  - Preservados getters delegados retrocompatibles (`npcs`, `playingNpcVoicePath`, `npcCount`, `permanentPlayers`) para garantizar cero roturas en llamadas de UI (`SessionScreen`, `BoardItemRenderer`, `CombatBlockContent`, `BoardBlockCard`, `PlanTab`, etc.).
  - Actualizadas las mutaciones de recuperación de biblioteca de NPCs, reproducción/detención de audio de voz y guardado/borrado de jugadores permanentes en `SessionViewModel`.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest` (`ChoirGuideDelegateTest` con nuevo test de sincronización `npc substate encapsulation and getter synchronization`).
  - Compilación e integración verificada con éxito en `:app:assembleDebug` (`BUILD SUCCESSFUL` en 5m 34s).

---

### Refactorización Arquitectónica de Sesión: Extracción de ResourcesSessionUiState (20 de octubre de 2026)

**Categoría:** `REFACTOR / ARCHITECTURE / SESSION / RESOURCES / RANDOM TABLES / STARTER PACKS / STATE MANAGEMENT / CLEAN CODE`

- **Extracción y Desacoplamiento de Recursos y Tablas de Sesión (`ResourcesSessionUiState`):**
  - Creado `com.deckapp.feature.draw.model.state.ResourcesSessionUiState` para encapsular las 4 variables de gestión de tablas y catálogo (`allTables`, `tablesInSession`, `availableStarterPacks`, `availableSystems`).
  - Incorporado `val resources: ResourcesSessionUiState = ResourcesSessionUiState()` en `SessionUiState`.
  - Preservados getters delegados retrocompatibles (`allTables`, `tablesInSession`, `availableStarterPacks`, `availableSystems`) para garantizar paridad y cero roturas en los 30+ composables y selectores (`ResourceManagerDialog`, `SessionBoardTab`, `SessionBoardBlockPickerSheet`, `SessionDialogsHost`, etc.).
  - Actualizados los flujos de inicialización y recolección reactiva en `SessionViewModel` (asignación de tablas del sistema, tablas de sesión y paquetes iniciales).
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest` (`ChoirGuideDelegateTest` con nuevo test de sincronización `resources substate encapsulation and getter synchronization`).
  - Compilación e integración verificada con éxito en `:app:assembleDebug` (`BUILD SUCCESSFUL` en 3m 1s).

---

### Refactorización Arquitectónica de Sesión: Extracción de GuidesSessionUiState (20 de octubre de 2026)

**Categoría:** `REFACTOR / ARCHITECTURE / SESSION / GUIDES / CHOIR / STORY ENGINE / MICROSETTINGS / STATE MANAGEMENT / CLEAN CODE`

- **Extracción y Desacoplamiento de Asistentes y Guías de Sesión (`GuidesSessionUiState`):**
  - Creado `com.deckapp.feature.draw.model.state.GuidesSessionUiState` para encapsular las 7 variables de guías interactivas y asistentes de creación:
    - Guía de procedimientos Choir (`choirGuideSourcePage`, `choirGuideTableId`, `choirGuideError`).
    - Diseñador de microsettings y clúster de Deck of Worlds (`activeMicrosetting`, `editingMicrosettingBoardItemId`).
    - Guía interactiva paso a paso The Story Engine & Deck of Worlds (`storyEngineGuideTab`, `storyEngineGuideStepIndex`).
  - Incorporado `val guides: GuidesSessionUiState = GuidesSessionUiState()` dentro del estado global `SessionUiState`.
  - Preservados getters delegados retrocompatibles (`choirGuideSourcePage`, `choirGuideTableId`, `choirGuideError`, `activeMicrosetting`, `editingMicrosettingBoardItemId`, `storyEngineGuideTab`, `storyEngineGuideStepIndex`) para asegurar cero roturas y paridad absoluta en composables y pantallas.
  - Corregido el smart-cast de `choirGuideError` en `ChoirProcedureGuide.kt` mediante acceso seguro (`?.let`).
- **Helpers Atómicos de Mutación en Delegados (`updateGuidesState`):**
  - Implementado helper `updateGuidesState` en `ChoirGuideDelegate`, `MicrosettingDelegate` y `StoryEngineGuideDelegate`.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest` (`StoryEngineGuideDelegateTest` con nuevo test de sincronización `guides substate encapsulation and getter synchronization`, `ChoirGuideDelegateTest`, etc.).
  - Compilación e integración completa verificada con éxito en `:app:assembleDebug` (`BUILD SUCCESSFUL` en 2m 55s).

---

### Refactorización Arquitectónica de Sesión: Extracción de CombatSessionUiState y SessionCombatDelegate (20 de octubre de 2026)

**Categoría:** `REFACTOR / ARCHITECTURE / SESSION / COMBAT / INITIATIVE / ENCOUNTERS / STATE MANAGEMENT / CLEAN CODE`

- **Extracción y Desacoplamiento de Estado de Combate e Iniciativa (`CombatSessionUiState`):**
  - Creado `com.deckapp.feature.draw.model.state.CombatSessionUiState` para encapsular las 5 variables del sistema táctico y tracker de encuentros (`hasActiveEncounter`, `activeEncounter`, `combatLog`, `playerParticipants`, `isCombatHudExpanded`).
  - Incorporado `val combat: CombatSessionUiState = CombatSessionUiState()` dentro del estado global `SessionUiState`.
  - Preservados getters delegados retrocompatibles (`hasActiveEncounter`, `activeEncounter`, `combatLog`, `playerParticipants`, `isCombatHudExpanded`) para garantizar cero roturas en llamadas de UI, paneles tácticos (`TacticalSidePanel`, `CombatBlockContent`, `SessionScreen`) y tests.
- **Helper Atómico de Mutación en Delegado (`SessionCombatDelegate.updateCombatState`):**
  - Implementado helper `updateCombatState { transform(it.combat) }` en `SessionCombatDelegate`, aislando las transiciones de participantes de iniciativa, expansión del HUD y cierre de encuentros.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest` (`SessionCombatDelegateTest` con nuevo test de sincronización y encapsulamiento `combatSessionUiState encapsulation and synchronization with legacy getters`).

---

### Refactorización Arquitectónica de Sesión: Extracción de SpotlightUiState y SessionSpotlightDelegate (20 de octubre de 2026)

**Categoría:** `REFACTOR / ARCHITECTURE / SESSION / SPOTLIGHT / OMNIBAR / STATE MANAGEMENT / CLEAN CODE`

- **Extracción y Desacoplamiento de Estado de Búsqueda Omnibar (`SpotlightUiState`):**
  - Creado `com.deckapp.feature.draw.model.state.SpotlightUiState` para encapsular las 7 variables del Omnibar / Spotlight (`showDialog`, `query`, `selectedCategory`, `results`, `selectedItem`, `lastRollResult`, `isSearching`).
  - Incorporado `val spotlight: SpotlightUiState = SpotlightUiState()` en `SessionUiState`.
  - Preservados getters delegados retrocompatibles (`showSpotlightDialog`, `spotlightQuery`, `spotlightCategory`, `spotlightResults`, `selectedSpotlightItem`, `spotlightLastRollResult`, `isSearchingSpotlight`) para garantizar compatibilidad total sin roturas en composables ni en llamadas externas.
- **Helper Atómico de Mutación en Delegado (`SessionSpotlightDelegate.updateSpotlightState`):**
  - Implementado helper `updateSpotlightState { transform(it.spotlight) }` en `SessionSpotlightDelegate`, unificando las transiciones de apertura, cierre, ejecución de query y selección de resultados en el Omnibar.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest` (`SessionSpotlightDelegateTest` con nuevo test de sincronización y encapsulamiento `spotlightUiState encapsulation and synchronization with legacy getters`).
  - Compilación e integración completa verificada con éxito en `:app:assembleDebug` (`BUILD SUCCESSFUL`).

---

### Refactorización Arquitectónica de Sesión: Extracción de SessionModalsUiState (20 de octubre de 2026)

**Categoría:** `REFACTOR / ARCHITECTURE / SESSION / MODALS / DIALOGS / STATE MANAGEMENT / CLEAN CODE`

- **Extracción y Desacoplamiento de Estado de Diálogos (`SessionModalsUiState`):**
  - Creado `com.deckapp.feature.draw.model.state.SessionModalsUiState` para encapsular la visibilidad y datos transitorios de las 19 ventanas modales, diálogos y alertas de la sesión (`showQuickNoteDialog`, `showResourceManager`, `showSessionConfig`, `showDeckPicker`, `showStarterPackDialog`, `showQuickSearch`, `showHotbarActionDialog`, `editingHotbarAction`, `showSceneFramingDialog`, `showClockEditDialog`, `editingClock`, `clockImpactAlert`, `showThreadEditDialog`, `showMythicCharacterEditDialog`, `editingMythicThread`, `editingMythicCharacter`, `showChoirGuide`, `showMicrosettingBuilder`, `showStoryEngineGuide`).
  - Incorporado `val modals: SessionModalsUiState = SessionModalsUiState()` dentro del estado global `SessionUiState`.
  - Preservados getters delegados retrocompatibles para las 19 propiedades, garantizando compatibilidad absoluta con `SessionDialogsHost`, componentes de UI y tests unitarios sin rotura alguna.
- **Helpers y Sincronización en Delegados:**
  - Implementado helper `updateModalsState` en `SessionClocksDelegate` y `SessionMythicDelegate`.
  - Migradas las mutaciones de apertura, edición y cierre de diálogos en `SessionViewModel`, `SessionClocksDelegate`, `SessionMythicDelegate`, `ChoirGuideDelegate`, `MicrosettingDelegate` y `StoryEngineGuideDelegate`.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest` (`SessionClocksDelegateTest` con nuevo test de sincronización y encapsulamiento `sessionModalsUiState encapsulation and synchronization with legacy getters`, `SessionMythicDelegateTest`, `ChoirGuideDelegateTest`, etc.).
  - Compilación e integración verificada en `:app:assembleDebug`.

---

### Refactorización Arquitectónica de Sesión: Extracción de SoloMythicUiState y SessionMythicDelegate (20 de octubre de 2026)


**Categoría:** `REFACTOR / ARCHITECTURE / SESSION / MYTHIC / SOLO JOURNALING / STATE MANAGEMENT / CLEAN CODE`

- **Extracción y Desacoplamiento de Estado Mítico y Solitario (`SoloMythicUiState`):**
  - Creado `com.deckapp.feature.draw.model.state.SoloMythicUiState` para aislar las variables del Oráculo de Mítica y el flujo Solo Journaling (`chaos`, `probability`, `lastResult`, `history`, `lastEvent`, `isSoloSceneBusy`, `isSendingInspiration`, `lastRandomMythicPick`).
  - Incorporado `val soloMythic: SoloMythicUiState = SoloMythicUiState()` en `SessionUiState`.
  - Preservados getters delegados retrocompatibles (`boardMythicChaos`, `boardMythicProbability`, `boardMythicLastResult`, `boardMythicHistory`, `boardMythicLastEvent`, `isSoloSceneBusy`, `isSendingInspiration`, `lastRandomMythicPick`) para paridad sin roturas con UI y vistas de Bento Board (`SessionBoardPagerContent`, `BoardCentralTool`, etc.).
- **Helper Atómico de Mutación en Delegado (`SessionMythicDelegate.updateMythicState`):**
  - Implementado helper `updateMythicState { transform(it.soloMythic) }` en `SessionMythicDelegate`.
  - Migradas las mutaciones de tiradas de destino (Fate Checks), eventos aleatorios, bloqueos de escenas en solitario, sorteo aleatorio de hilos/personajes e inspiración en el diario.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest` (`SessionMythicDelegateTest` con nuevo test de sincronización y encapsulamiento `soloMythicUiState encapsulation and synchronization with legacy getters`).

---

### Refactorización Arquitectónica de Sesión: Extracción de DecksUiState y SessionDeckDelegate (20 de octubre de 2026)


**Categoría:** `REFACTOR / ARCHITECTURE / SESSION / DECKS / STATE MANAGEMENT / CLEAN CODE`

- **Extracción y Desacoplamiento de Estado de Mazos (`DecksUiState`):**
  - Creado `com.deckapp.feature.draw.model.state.DecksUiState` para encapsular las 14 variables de mazos, manos, descartes y previsualización (`deckRefs`, `deckNames`, `deckCardCounts`, `selectedDeckId`, `hand`, `pile`, `peekCard`, `deckAspectRatios`, `deckBackImages`, `collapsedDeckIds`, `handByDeck`, `allDecks`, `isShuffling`, `showDrawBySuitSheet`, `availableSuitsForDraw`).
  - Incorporado `val decks: DecksUiState = DecksUiState()` en `SessionUiState`.
  - Preservados getters delegados retrocompatibles (`hand`, `pile`, `selectedDeckId`, `allDecks`, `handByDeck`, etc.) para garantizar paridad y cero roturas con llamadas o extensiones existentes.
- **Helper Atómico de Mutación en Delegado (`SessionDeckDelegate.updateDeckState`):**
  - Implementado helper `updateDeckState { transform(it.decks) }` en `SessionDeckDelegate`, unificando la mutación de estado de cartas, selección de mazos, visibilidad de hojas de palos y colapso de mazos.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest` (`PlaymatDelegateTest`, `SessionDeckDelegateTest`, `StoryEngineGuideDelegateTest`).
  - Compilación exitosa en `:app:assembleDebug`.

---

### Refactorización Arquitectónica de Sesión: Extracción de PlaymatUiState y Contrato PlaymatActions (20 de octubre de 2026)


**Categoría:** `REFACTOR / ARCHITECTURE / SESSION / PLAYMAT / STATE MANAGEMENT / CLEAN CODE / COMPOSABLE`

- **Extracción y Desacoplamiento de Estado (`PlaymatUiState`):**
  - Creado `com.deckapp.feature.draw.model.state.PlaymatUiState` para aislar las variables del Tapete 2D (`selectedCardId`, `showHandTray`, `isRulerActive`).
  - Incorporado `val playmat: PlaymatUiState = PlaymatUiState()` dentro del estado global de sesión (`SessionUiState`).
  - Preservados getters delegados retrocompatibles (`selectedPlaymatCardId`, `showPlaymatHandTray`, `activePlaymatSurface`) para garantizar cero roturas en código existente y tests heredados.
- **Helper Atómico de Mutación en Delegado (`PlaymatDelegate.updatePlaymatState`):**
  - Implementado helper `updatePlaymatState { transform(it.playmat) }` en `PlaymatDelegate`, simplificando operaciones de selección y visibilidad de bandejas.
- **Eliminación del Prop-Drilling de 40+ Callbacks (`PlaymatActions`):**
  - Creado el contrato unificado `PlaymatActions` en `model.state` agrupando todas las acciones de cartas, superficies, cámaras, físicas de mazos, tokens, relojes, hilos y battlemaps.
  - Creada la función de extensión `PlaymatDelegate.toActions(...)` que mapea de forma limpia y tipada todas las llamadas al delegado.
  - Reducción en `SessionScreen.kt` de más de 55 líneas de paso manual de lambdas sueltas a un bloque conciso y memoizado `remember(viewModel.playmatDelegate) { viewModel.playmatDelegate.toActions(...) }`.
  - Mantenida sobrecarga `@Deprecated` en `PlaymatCanvasView` para retrocompatibilidad total con llamadas o previews existentes.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `PlaymatDelegateTest` incluyendo la nueva prueba `playmatUiState_encapsulationAndToActionsBridge`.

---

### Fondos de Escena, Battlemaps y Cuadrícula Táctica con Snap y Regla en el Tapete (Playmat Fase 7) (20 de octubre de 2026)

**Categoría:** `FEATURE / PLAYMAT / BATTLEMAPS / TACTICAL GRID / MAGNETIC SNAP / DUAL RULER / UX / COMPOSABLE`

- **Soporte Dual de Battlemaps y Fondos de Escena (`PlaymatBattlemapLayer`):**
  - **Texturas atmosféricas procedurales sin peso añadido**: Implementación de 4 presets geométricos y degradados vectoriales en Canvas:
    - *Mazmorra de Piedra (`DUNGEON_STONE`)*: Losas de roca fría, relieve y juntas oscuras con viñeta.
    - *Taberna Medieval (`MEDIEVAL_TAVERN`)*: Tablones de roble cálido y vetas de madera rústica.
    - *Bosque Místico (`MYSTIC_FOREST`)*: Fondo musgoso y motas de polen/esporas mágicas luminiscentes.
    - *Mapa de Pergamino Antiguo (`PARCHMENT_MAP`)*: Tinte sepia envejecido con manchas sutiles y rosa de los vientos estilizada.
  - **Importación de Battlemaps personalizados**: Selector nativo de galería (`GetContent` / `image/*`) con renderizado eficiente vía Coil (`AsyncImage`), persistencia de URI y escalado/posicionamiento 2D continuo.
  - **Panel de control de escena (`PlaymatTacticalSettingsDialog`)**:
    - Ajuste de opacidad (20% a 100%) para balancear visibilidad de cartas/tokens con el mapa.
    - Factor de escala de fondo (0.5x a 3.0x).
    - Bloqueo de mapa (`isBackgroundLocked`) para evitar desplazamientos accidentales al manipular miniaturas.
- **Cuadrícula Táctica Híbrida (`PlaymatGridLayer`):**
  - Renderizado de alta fidelidad acoplado al Pan & Zoom de la cámara 2D en tiempo real.
  - **Modos de celda**:
    - *Cuadrada clásica D&D/Pathfinder*: Escala estándar de 5 pies por casilla.
    - *Hexagonal táctica*: Para hexploración, wargames o movimiento diagonal orgánico.
  - **Estilo visual**: *Líneas tenues* o *Puntos minimalistas* con tinte semitransparente que no interfiere con el arte del mapa.
  - **Calibrador de celda**: Slider táctil de 32dp a 96dp (por defecto 48dp) con previsualización en vivo.
- **Snapping Magnético Universal a Casilla (`PlaymatDelegate.calculateSnappedPosition`):**
  - Al soltar tokens de personajes (`PlaymatTokenItem`), relojes de progreso (`PlaymatClockItem`) o cartas, las coordenadas se alinean con precisión matemática al centro de la casilla o hexágono más cercano cuando el snap está activado.
  - Prioridad inteligente: respeta ranuras de plantillas temáticas (*Deck of Worlds*, *Story Engine*) si el elemento se suelta dentro de su radio magnético prioritario, aplicando el snap de cuadrícula táctica en el espacio restante.
- **Doble Regla de Medición Táctica (`PlaymatMeasurementRuler`):**
  - **Cinta elástica automática en arrastre de tokens**: Al mover cualquier token en el tapete, se despliega una línea elástica punteada en color cian neón (`#00E5FF`) desde el punto de origen hasta la posición actual del dedo, con halos de origen y destino y una píldora flotante con la distancia en tiempo real: ej. `"30 ft (6 c)"` calculada a razón de 5 pies por casilla.
  - **Herramienta de Regla Libre**: Nuevo botón de regla (`Straighten`) en `PlaymatToolbar`. Al activarse, despliega un banner guía en la parte superior y permite medir cualquier distancia y línea de visión (LoS) entre dos puntos del tapete mediante un trazo táctil sin mover las piezas de la mesa.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `PlaymatDelegateTest` (`battlemap_setPresetAndCustomImageTransform` y `tacticalGrid_configurationAndSnapping`).
  - Compilación e integración verificada en `:app:assembleDebug`.

---

### Volteo 3D de Cartas Boca Abajo y Relojes de Progreso en el Tapete (Playmat Fase 6) (20 de octubre de 2026)

**Categoría:** `FEATURE / PLAYMAT / PROGRESS CLOCKS / 3D CARD FLIP / NARRATIVE THREADS / BLADES IN THE DARK / UX / COMPOSABLE`

- **Volteo Físico 3D de Cartas Boca Abajo (`PlaymatCardItem`):**
  - Implementación de animación tridimensional sobre el eje Y (`rotationY`) a 120 FPS con resorte elástico (`Spring.StiffnessMediumLow`) y perspectiva de cámara (`cameraDistance = 14f * density`).
  - Al voltear una carta mediante el menú radial contextual o cambio de estado, la carta rota suavemente de 0° a 180°, alternando en el ecuador de los 90° entre su anverso ilustrado y su reverso físico (con contra-rotación interna de 180° para prevenir efecto espejo).
  - Permite preparar encuentros misteriosos, trampas, tesoros ocultos o tiradas de oráculo sin revelar antes del momento dramático.
- **Relojes de Progreso Interactivos (`PlaymatClockItem`):**
  - Nuevo elemento espacial en el tapete inspirado en *Blades in the Dark*, PBTA y TTRPGs modernos.
  - Renderizado geométrico vectorial mediante Canvas con cuñas radiales en 4, 6 u 8 segmentos de tensión, eje central metálico y disco de cuarzo oscuro.
  - Interacción táctil fluida a 120 FPS:
    - *Toque directo*: cicla y avanza el progreso del reloj.
    - *Menú contextual flotante*: acciones rápidas para `+1 Progreso`, `-1 Progreso`, `Vaciar Reloj`, `Conectar Hilo`, `Retirar` y `Cerrar`.
    - *Física de arrastre*: movimiento desacoplado a 120 FPS sin fricción ni lag.
- **Diálogo Creador de Relojes (`PlaymatClockCreationDialog`):**
  - Botón directo en la barra flotante del tapete (`PlaymatToolbar`) con icono temático de reloj de arena.
  - Selección de título con sugerencias rápidas (*Alerta de Guardias*, *Tiempo de Escape*, *Ritual Oscuro*, *Detección Sigilosa*).
  - Selector de complejidad (4, 6 u 8 cuñas) y paleta de 6 tintes semánticos (Peligro rojo, Alerta ámbar, Magia violeta, Tiempo azul, Progreso verde, Neutral).
- **Conectividad Universal Total de Hilos:**
  - Los relojes se integran transparentemente al sistema de conectores elásticos con el endpoint `"clock_$id"`.
  - Permite enlazar: **Carta ↔ Reloj** (ej. pista que avanza una cuenta regresiva), **Token ↔ Reloj** (ej. guardia que activa una alarma) y **Reloj ↔ Reloj**.
  - Poda automática de hilos si se retira el reloj de la mesa.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `PlaymatDelegateTest` (`cardFlip_toggleIsFaceUp` y `progressClocks_crudAndUniversalConnections`).
  - APK debug generado y compilado con éxito.

---

### Tokens de Personajes (PJ / PNJ) y Conexiones Universales en el Tapete (Playmat Fase 5) (20 de octubre de 2026)

**Categoría:** `FEATURE / PLAYMAT / CHARACTER TOKENS / NPCS / NARRATIVE THREADS / UNIVERSAL CONNECTORS / UX / COMPOSABLE`

- **Tokens Circulares de Personajes TTRPG (`PlaymatTokenItem`):**
  - Cada token representa a un personaje (PJ o PNJ) sobre el tapete con disco circular de 64dp, sombreado dinámico, anillo de facción en relieve (Verde Aliado `#4CAF50`, Ámbar Neutral `#FFB300`, Rojo Enemigo `#E53935`), avatar fotográfico o inicial tipográfica, micro-barra de PV de 2 colores y etiqueta inferior con nombre y valor numérico de vida.
  - Arrastre desacoplado ultra fluido a 120 FPS sin bloqueos ni lag.
  - Menú radial contextual con acceso directo a: *Conectar Hilo*, *Ajustar PV*, *Cambiar Facción*, *Retirar de la Mesa* y *Cerrar*.
- **Selector de Tokens Mixto (`PlaymatTokenPickerDialog`):**
  - Botón directo `+ Token` en la barra de herramientas del tapete (`PlaymatToolbar`).
  - Pestaña de **NPCs de Campaña**: despliega la lista de personajes registrados en la sesión (`uiState.npcs`) para colocarlos en la mesa con un toque preservando sus PV y avatar.
  - Pestaña de **Token Rápido**: formulario ligero para crear tokens ad-hoc (nombre, PV máx, facción) al vuelo.
- **Micro-Gestión de Salud (`PlaymatTokenHpDialog`):**
  - Diálogo rápido con modificadores numéricos (`-5`, `-1`, `+1`, `+5`) para reflejar daño y curación al instante en combate o narrativa.
- **Conectividad Universal de Hilos Narrativos:**
  - `PlaymatConnection` generalizado mediante `fromEndpointId` y `toEndpointId` con soporte de prefijos universales (`"card_"` y `"token_"`).
  - Permite trazar hilos entre cualquier combinación de elementos: **Carta ↔ Carta**, **Carta ↔ Token**, **Token ↔ Token** o **Token ↔ Carta**.
  - Poda automática de hilos si se retira un token o se descarta/devuelve una carta de la mesa.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `PlaymatDelegateTest` (`characterTokens_crudAndUniversalConnections`).
  - Desplegado y verificado en dispositivo físico Motorola One Fusion (`ZE222B5GXX`).

---

### Hilos Narrativos y Conectores de Investigación en el Tapete (Playmat Fase 4) (19 de octubre de 2026)

**Categoría:** `FEATURE / PLAYMAT / NARRATIVE THREADS / CONNECTORS / INVESTIGATION BOARD / UX / COMPOSABLE`

- **Hilos Narrativos y Conectores Físicos entre Cartas (`PlaymatConnectionLine`):**
  - Permite enlazar cualquier par de cartas en el tapete 2D con un estilo visual de tablón de investigación / pistas detectivescas o relaciones causa-efecto de *The Story Engine*.
  - Renderizado de cuerda elástica con curvatura Bézier (`quadraticTo`) y ligera comba por gravedad que se tensa, estira y reposiciona suavemente en tiempo real a 120 FPS al arrastrar cualquiera de las dos cartas.
  - Sombra proyectada sobre el fieltro/madera de la mesa para dar sensación de profundidad física.
  - Chinchetas/pins en ambos extremos con textura metálica, relieve de color temático y brillo especular.
- **Flujo de Conexión Táctil Ergonómico:**
  - Acción rápida **"Conectar Hilo"** en el menú contextual radial de la carta seleccionada.
  - Al activarse, la carta origen se ilumina con un halo/borde distintivo en rojo (`#E53935`) y se despliega un banner inferior con la indicación *"Toca la segunda carta para tender el hilo"* y opción de cancelación.
  - Tocar la carta destino traza la conexión de inmediato.
- **Píldora Interactiva y Diálogo de Edición (`PlaymatConnectionEditorDialog`):**
  - Cada hilo cuenta con una píldora flotante en su punto central medio que muestra la relación (ej. *"Sospechoso de"*, *"Antagonista"*, *"Causa / Origen"*, *"Ubicado en"*, *"Aliado con"*, *"Pista de"*).
  - Tocar la píldora abre un diálogo para modificar la etiqueta de texto con un toque, seleccionar entre una paleta de 6 colores semánticos (Rojo, Oro, Cian, Púrpura, Verde, Neutral) o cortar el hilo con el botón de papelera.
- **Limpieza y Poda Automática del Ciclo de Vida:**
  - Al descartar o devolver cualquiera de las cartas conectadas a la mano privada, los hilos asociados se eliminan automáticamente para evitar referencias huérfanas en la mesa.
  - Prevención de duplicados en ambas direcciones.
- **Verificación Técnica:**
  - 100% de tests unitarios aprobados en `PlaymatDelegateTest` (`narrativeConnections_createUpdateAndAutoPruneOnDiscard`).
  - Compilado e instalado en el dispositivo físico Motorola (`ZE222B5GXX`) con ejecución activa.

---

### Mazos Físicos 3D, Pilas de Descarte y Lanzador de Dados en el Tapete (Playmat Fase 3) (19 de octubre de 2026)

**Categoría:** `FEATURE / PLAYMAT / PHYSICAL DECKS / DICE ROLLER / GESTURES / UX / COMPOSABLE`

- **Mazos Físicos 3D en el Tapete (`PlaymatDeckItem`):**
  - Cada mazo activo en la sesión se representa físicamente sobre la superficie con efecto de volumen 3D escalonado (1 a 4 capas de sombras y bordes según cartas restantes).
  - Píldora superior con el nombre del mazo y badge con el conteo de cartas disponibles.
  - Textura auténtica del reverso del mazo o degradado táctil de cuero si no posee imagen.
  - **Interacción Dual de Robo con Feedback Háptico:**
    - *Toque directo*: roba una carta de forma inmediata y la coloca boca arriba al lado del mazo.
    - *Arrastre táctil desde el mazo*: arrastrar el dedo desde el mazo extrae una carta flotante con vibración háptica (`HapticFeedbackConstants.LONG_PRESS` / `performHapticFeedback`) y permite soltarla en cualquier coordenada del tapete o acoplarla por snapping magnético a una ranura de plantilla.
  - Mazos completamente reposicionables en el espacio 2D mediante arrastre de su cabecera.
- **Pilas de Descarte Físicas con Retorno Rápido (`PlaymatDiscardPileItem`):**
  - Cada mazo cuenta con su correspondiente pila de descarte sobre el tapete.
  - Estado vacío representado con ranura punteada tenue y botón de retorno inactivo.
  - Al descartar cartas, muestra la última carta descartada boca arriba con una inclinación física natural (-2.5°), badge numérico de cartas descartadas y botón rápido de barajado (🔄 *Reshuffle*) que devuelve todas las cartas al mazo principal y registra el evento en el log.
  - Coordenadas de descarte reposicionables libremente en la mesa.
- **Lanzador Flotante de Dados Integrado (`PlaymatDiceOverlay`):**
  - Nuevo botón de dados (`Casino`) en la barra de herramientas del tapete que despliega una botonera flotante compacta y ergonómica.
  - Soporte instantáneo de dados poliédricos estándar de rol: `d4`, `d6`, `d8`, `d10`, `d12`, `d20`, `d100`.
  - Animación de tirada y muestra de fórmula y resultado.
  - Botón **"Fijar a Mesa"**: materializa el resultado numérico del dado directamente sobre el tapete como una ficha circular (`PlaymatTableChip`) con color temático según el dado.
- **Persistencia de Superficie (`PlaymatDeckPlacement`):**
  - Estructura `PlaymatDeckPlacement(stackId, x, y, discardX, discardY, isLocked)` guardada en cada `PlaymatSurface`.
  - Disposición automática inteligente al margen izquierdo para sesiones existentes o nuevos mazos añadidos a la mesa.
- **Verificación Técnica:**
  - Nuevas pruebas unitarias en `PlaymatDelegateTest` verificando movimiento y persistencia de mazos y descartes (100% de tests aprobados).
  - Compilación e instalación directa en dispositivo físico Motorola con verificación visual.

---

### Optimización de Arrastre Ultrarrápido, Snapping Magnético y Exportación en el Tapete (Playmat Fase 2) (19 de octubre de 2026)

**Categoría:** `PERFORMANCE / PLAYMAT / GESTURES / UX / COMPOSABLE / ANIMATIONS / EXPORT`

- **Arrastre Desacoplado y Ultrafluido a 120 FPS (`PlaymatCardItem`):**
  - **Diagnóstico y solución del stuttering:** Se identificó que la llamada a `onMoveCard` en cada micro-delta del puntero disparaba la codificación JSON y escritura síncrona en disco (`SharedPreferences.apply()`) en el hilo de UI, provocando que las cartas se congelaran o apenas se movieran.
  - **Decoupling gesture engine:** Se implementó `localX` y `localY` gestionados localmente durante el gesto, delegando la persistencia a `onMove` únicamente al liberar el dedo (`onDragEnd`).
  - **Eliminación de colisión de eventos táctiles:** Se sustituyó `.clickable` por `awaitEachGesture` / `awaitTouchSlopOrCancellation` en un único bloque de puntero, permitiendo detección inmediata y sin retrasos de toques limpios para selección y arrastre instantáneo sin conflicto de touch slop.
- **Snapping Magnético a Plantillas Temáticas (`TemplateSlotDef`):**
  - Al arrastrar una carta cerca de una ranura de plantilla (*Deck of Worlds*, *Story Engine*, *Tarot*, *Oráculo*) a menos de 60dp, la carta proyecta un halo de acoplamiento magnético en color cian neón (`#00E5FF`) y la silueta de la ranura se ilumina.
  - Al soltar la carta dentro del radio magnético, se ajusta con precisión matemática a las coordenadas de la ranura y asocia su `slotTag`.
- **Banner de Lectura Rápida de Pistas Activas (`Active Cue Quick-Read`):**
  - Renderizado automático de una barra inferior translúcida con el texto de la pista activa (`activeZone.text`) o título, permitiendo leer el significado narrativo inmediatamente al rotar sin tener que hacer zoom excesivo ni abrir el visor de detalles.
- **Rotación Física Suave con Elasticidad (`Spring`):**
  - Se sustituyó la rotación instantánea por `animateFloatAsState` con `spring(stiffness = Spring.StiffnessMediumLow)` renderizada vía GPU en `graphicsLayer`, otorgando una sensación táctil de carta física sobre paño.
- **Acción Radial "A Mapa Táctico" con Diálogo de Conversión:**
  - En la rueda contextual radial se añadió la acción *A Mapa Táctico*: despliega un diálogo selector para convertir la carta seleccionada en un **Token / Miniatura de Combate** (con la ilustración, PV y CA en la grilla) o en un **Pin de Exploración / PDI** (chincheta de mapa con las pistas y notas de la carta).
- **Exportación y Compartido de Snapshot del Tapete (`PlaymatSnapshotHelper`):**
  - Integrado botón de cámara/compartir en `PlaymatToolbar` que captura la superficie de la mesa mediante `rememberGraphicsLayer()` y lanza el selector nativo de compartir de Android (`Intent.ACTION_SEND`) utilizando `FileProvider` (`${applicationId}.fileprovider`).
- **Verificación Técnica:**
  - Compilación limpia de todos los módulos y 100% de tests unitarios aprobados en `:feature:draw:testDebugUnitTest`.
  - Instalado y ejecutado con éxito en dispositivo físico Motorola conectado vía ADB.

---

### Vista Principal de Tapete de Cartas Espacial 2D — Playmat (19 de octubre de 2026)

**Categoría:** `FEATURE / PLAYMAT / WORKSPACE / 2D CANVAS / UX / STORY ENGINE / SESSION`

- **Nueva Vista Central Dedicada (`SessionViewMode.PLAYMAT`):**
  - Añadido el modo "Tapete" al selector de vistas principales en `SessionTopBar` (junto a *Mesa*, *Táctico*, *Mapa* y *Mythic*), disponible tanto en el menú desplegable móvil como en el conmutador de iconos en pantallas anchas.
  - Implementado `PlaymatCanvasView` con Pan & Zoom continuo 2D multitáctil (pellizco con 2 dedos y arrastre de fondo) con límites de escala ergonómicos (0.4x a 2.5x).
- **Mesa de Juego 2D Interactiva con Arrastre y Orientación Física:**
  - Las cartas se colocan sobre la superficie con coordenadas libres `(x, y)`, elevación, sombra y rotación (`rotationDegrees: 0°, 90°, 180°, 270°`).
  - Detección táctil individual: arrastre directo de cartas sobre el lienzo con actualización reactiva en `SessionUiState`.
  - Soporte de cara visible (`isFaceUp`): renderizado del arte/zonas frontales o dorso temático oficial de mazo.
- **Menú Radial Contextual (`PlaymatRadialMenu`):**
  - Al tocar cualquier carta en el tapete, se despliega una rueda de micro-acciones flotante:
    - 🔄 **Girar 90°**: Rota la orientación activa al siguiente borde.
    - 👁️ **Voltear**: Alterna entre frente y dorso de la carta.
    - 📥 **Mandar a Mano**: Devuelve la carta a la reserva privada del jugador.
    - 🗑️ **Descartar**: Envía la carta a la pila de descarte con feedback visual.
    - 🔍 **Detalle**: Abre el visor ampliado a pantalla completa.
- **Bandeja Inferior Desplegable de la Mano (`PlaymatHandTray`):**
  - Dock inferior acoplable que filtra automáticamente las cartas que están en mano y aún no han sido bajadas a la mesa.
  - Permite desplegar cartas al tapete mediante el botón "A la Mesa" con auto-posicionamiento en el centro de visión actual.
- **Barra de Herramientas Flotante (`PlaymatToolbar`):**
  - Selector y gestor de múltiples mesas/superficies por sesión (`PlaymatSurface`).
  - Selector de temas visuales de fondo: *Fieltro Verde*, *Fieltro Rojo*, *Madera Rústica*, *Pergamino Antiguo* y *Dark Mode Minimal* con microrejilla de puntos.
  - Selector de plantillas y moldes temáticos (`PlaymatTemplateType`): *Mesa Libre*, *Deck of Worlds: Microsetting (1+5)* con ranuras cardinales punteadas (*Región, Hito, Nombres, Orígenes, Atributos, Crisis*), *Story Engine: Premisa (4 cartas)*, *Tarot (3 cartas)* y *Oráculo en Cruz*.
  - Acciones rápidas: Notas adhesivas (*Sticky Notes*), centrado de cámara y robo directo al tapete (*Quick Draw*).
- **Delegado y Persistencia Reactiva (`PlaymatDelegate`):**
  - Lógica desacoplada en `PlaymatDelegate` con integración a `BoardSessionMemory` en `SettingsRepository`, garantizando guardado persistente entre sesiones sin necesidad de migraciones destructivas de base de datos.
  - Cobertura completa de pruebas unitarias en `PlaymatDelegateTest` (100% aprobadas).

---

### Guía Interactiva y Asistente de Procedimiento de Story Engine y Deck of Worlds en Sesión (19 de octubre de 2026)

**Categoría:** `FEATURE / STORY ENGINE / SESSION / UX / WORKFLOW / PARITY`

- **Barra de Procedimiento Contextual (`StoryEngineProcedureBar`):**
  - Se activa automáticamente bajo la barra superior de la sesión cuando la sesión actual tiene los sistemas "Story Engine" o "Deck of Worlds".
  - Muestra un banner contextual distintivo en tono terracota/ámbar con acceso rápido a la fase actual y botones para abrir la guía paso a paso y el generador de microsettings.
  - Coexiste armoniosamente con el asistente de *Choir of Flesh* sin conflictos visuales ni funcionales.
- **Asistente Modal de Procedimiento (`StoryEngineProcedureGuide`):**
  - Implementado como Bottom Sheet modal con 3 pestañas temáticas:
    1. *Deck of Worlds*: 6 fases de construcción de microsettings (Región, Punto de Interés, Nombres, Orígenes, Atributos y Acontecimientos).
    2. *New Story Engine*: 4 fases de formulación narrativa (Potenciador, Trasfondo, Misterio y Objeto Único).
    3. *Expansiones*: Matriz y guía de integración para expansiones cartográficas, creativas y temáticas de género.
  - Cada fase incluye:
    - Chip de progreso e indicador numérico.
    - Descripción del objetivo creativo y preguntas disparadoras de inspiración en español.
    - Consejo de experto/pro-tip para el Director de Juego / Narrador Solo.
    - Botón de acción **Robar Carta**: busca el mazo correspondiente en la sesión (o en la biblioteca si no está añadido a la sesión y lo vincula automáticamente), roba una carta a la mesa y muestra un Snackbar de confirmación.
    - Botón de acción **Rotar Pista**: cicla la orientación activa hacia el borde inferior (0° / Sur) para revelar la siguiente opción narrativa (1d4 a 90° en 4 bordes, 1d2 a 180° en 2 bordes).
    - Botón de acceso directo para abrir el modal `MicrosettingBuilderDialog`.
- **Arquitectura y Delegados (`StoryEngineGuideDelegate`):**
  - Control de estado desacoplado dentro de `SessionViewModel` mediante `StoryEngineGuideDelegate`, gestionando selección de pestañas, avance de pasos, extracción y rotación de cartas asociadas.
  - Pruebas unitarias completas implementadas en `StoryEngineGuideDelegateTest` con MockK y Coroutines Test.
- **Paridad con Desktop:**
  - Especificación arquitectónica y de interfaz documentada en `docs/planning/DESKTOP_PARITY_STORY_ENGINE.md` (Sección 8: Procedimiento y Guía en Sesión).

---

### Incorporación de las 13 Expansiones de The Story Engine y Deck of Worlds (19 de octubre de 2026)

**Categoría:** `FEATURE / STORY ENGINE / EXPANSIONS / PACKAGING / OCR PIPELINE / PARITY`

- **Extracción, Rasterización y OCR de Alta Precisión para 13 Expansiones Oficiales:**
  - Se procesaron las 13 carpetas y PDFs de expansiones originales (Print and Play HQ Cards) mediante PyMuPDF y RapidOCR ONNX.
  - Se identificó la alternancia de páginas par/impar (páginas impares como frentes de carta con pistas, páginas pares como reversos oficiales temáticos), garantizando que las cartas se extraigan con sus reversos auténticos.
- **Empaquetado en 2 Paquetes Maestros Oficiales (`FullBackupDto`):**
  - **Paquete 1: `story_engine_deck_of_worlds_expansions.json` (9 Mazos, 90 Cartas, 90 Caras, 1 Regla de Mundo):**
    - *Expansiones Cartográficas (`TOP_BOTTOM_SPLIT`)*: Ártico (10 cartas), Costas (10 cartas), Desiertos (10 cartas).
    - *Expansiones Creativas (`FOUR_EDGE_CUES`)*: Ganchos de Aventura (10 cartas), Llaves de Cultura (10 cartas), Fragmentos de Lore (10 cartas).
    - *Expansiones de Género (`TOP_BOTTOM_SPLIT`)*: Fantasía (10 cartas), Horror (10 cartas), Ciencia Ficción (10 cartas).
    - Incluye regla de compendio: *Deck of Worlds: Guía de Expansiones Cartográficas y de Género*.
  - **Paquete 2: `new_story_engine.json` (4 Mazos, 50 Cartas, 50 Caras, 1 Regla de Narrativa):**
    - *Potenciadores / Boosters* (20 cartas, `TOP_BOTTOM_SPLIT`).
    - *Trasfondos / Backstories* (10 cartas, `FOUR_EDGE_CUES`).
    - *Misterios / Mystery* (10 cartas, `TOP_BOTTOM_SPLIT`).
    - *Objetos Únicos / Unique Items* (10 cartas, `FOUR_EDGE_CUES`).
    - Incluye regla de compendio: *New Story Engine: Cómo Funciona el Motor de Historias*.
- **Limpieza de OCR y Traducción al Español:**
  - Filtrado de marcas de agua, copyright, URLs y numeraciones marginales.
  - Normalización de las pistas narrativas en `zonesJson` con traducciones al español de biomas, sitios, roles y ganchos.
  - Corrección arquitectónica: conformación estricta de `zonesJson` a `List<ContentZone>` (`[{"text": "..."}]`) para asegurar la compatibilidad con el selector de brújula/dados y la rotación física en Compose.
- **Sincronización en ViewModels y Mappers:**
  - Actualizados `TablesViewModel`, `SessionViewModel`, `ReferenceTabViewModel`, `ReferenceListViewModel` y `TablesTab` para nombres de visualización limpios.
  - En `ReferenceRepositoryImpl`, protección de estados activos (`preserveStoryCards`) extendida a ambos paquetes de expansión para no sobreescribir cartas en juego de los usuarios.
- **Paridad con Desktop:**
  - Actualizado `docs/planning/DESKTOP_PARITY_STORY_ENGINE.md` con la especificación completa de ambos paquetes maestros.

---

### Configuración Story Engine: Deck of Worlds — Traducción al Español (704 Pistas) y Orientación Activa al Borde Inferior (19 de octubre de 2026)

**Categoría:** `FEATURE / STORY ENGINE / LOCALIZATION / UX / PARITY`

- **Traducción Íntegra al Español de las 240 Cartas y 704 Zonas Narrativas:**
  - Se implementó un pipeline de extracción OCR mediante red neuronal (RapidOCR ONNX) para recuperar y alinear con precisión las opciones impresas en cada cara de carta.
  - Se tradujeron al español el 100% de las pistas y ganchos narrativos (`zonesJson`) en los 6 sub-mazos oficiales:
    - *Regiones* (32 cartas, 32 biomas/terrenos en español).
    - *Puntos de Interés* (48 cartas, 96 estructuras y sitios geográficos).
    - *Nombres y Apelativos* (48 cartas, 192 epítetos, prefijos y sufijos).
    - *Orígenes e Historia* (32 cartas, 128 mitos de creación, catástrofes y hechos históricos).
    - *Atributos y Cultura* (48 cartas, 192 rasgos culturales, anomalías y costumbres locales).
    - *Acontecimientos y Ganchos* (32 cartas, 64 crisis inminentes, dilemas y detonantes de aventura).
  - Títulos enriquecidos temáticamente en formato: `Categoría #XX: [Pistas en español]` (ej: *Región #01: Bosque(s)*, *Punto de Interés #01: Taller / Árbol ancestral*, *Acontecimiento #01: Bajo asedio / Fauna anómala*).
- **Convención de Orientación Activa al Borde Inferior (Sur / 0°):**
  - La opción activa y seleccionada de cada carta queda orientada en posición de lectura derecha en el **borde inferior** (Sur / Abajo), facilitando la lectura directa en la mano del jugador y tapete de juego.
  - En cartas de 4 opciones (`FOUR_EDGE_CUES`):
    - `0°`: Sur (lectura derecha en borde inferior, badge `S`).
    - `90°`: Oeste (rotada 90° al borde inferior, badge `O`).
    - `180°`: Norte (rotada 180° al borde inferior, badge `N`).
    - `270°`: Este (rotada 270° al borde inferior, badge `E`).
  - En cartas de 2 opciones (`TOP_BOTTOM_SPLIT`):
    - `0°`: Opción Inferior (badge `1`).
    - `180°`: Opción Superior rotada al borde inferior (badge `2`).
  - En cartas centradas (`IMAGE_WITH_TEXT`):
    - `0°`: Opción central.
- **Sincronización en UI, Modelos y Base de Datos:**
  - Actualizado `CardViewScreen.kt`: chips de estado flotante, etiquetas del selector radial de brújula (`Sur`, `Oeste`, `Norte`, `Este` e `Inferior`, `Superior`), y paneles de zonas (`GenericZonePanel`, `TopBottomZonePanel`).
  - Actualizado `CardItem.kt`: badges de orientación en cartas de mano/mesa ajustados a `S`, `O`, `N`, `E` y `1`, `2`.
  - Actualizado `CardEditorScreen.kt`: nomenclatura de zonas sincronizada con los grados y orientaciones cardinales.
  - Actualizado `ReferenceRepositoryImpl.kt`: soporte para actualización reactiva de títulos y zonas pobladas al reinstalar o refrescar el paquete de inicio sin perder estados de juego.
- **Paridad de Escritorio (`RULE[user_global]`):**
  - Actualizado `docs/planning/DESKTOP_PARITY_STORY_ENGINE.md` documentando la arquitectura de datos, convenciones cardinales al borde inferior y rebalanceo de sub-mazos para réplica exacta en TDAPP-desktop.

---

### Mejoras de Bloques de Mazos en Mesa, Iconografía Vectorial y Validación Story Engine (18 de octubre de 2026)

**Categoría:** `FEATURE / UX / VECTOR ASSETS / HARDWARE QA`

- **Rediseño y Ergonomía de Bloques de Mazos en Mesa (`DeckBlockContent.kt`):**
  - **Estructura en 3 zonas visuales:** Cabecera con contadores precisos (*N en mano · N en mazo · N descartadas*), botón de acción principal (*Robar*, deshabilitado con estado *"Agotado"* al vaciarse el mazo) y carrusel de cartas en mano.
  - **Separación semántica de barajado:** Se implementó `shuffleDeckOnly` en `SessionDeckDelegate` y `SessionViewModel` para permitir barajar las cartas restantes disponibles en el mazo sin forzar la recuperación de descartes.
  - **Diálogos de confirmación:** Protección contra acciones destructivas masivas para *"Descartar mano"* y *"Reiniciar mazo"*, mostrando con precisión el impacto de cartas afectadas.
  - **Navegación al Visor de Cartas:** Cableado completo del callback `onCardClick` a través de `SessionToolConnector`, `BoardCentralTool`, `SessionBoardTab`, `SessionBoardPagerContent`, `BoardItemUiContext`, `BoardBlockCard` y `BoardBlockCardAux` hacia `DeckBlockContent`.
- **Iconografía Vectorial & Launcher Adaptable:**
  - Sustitución de glifos/emojis hardcodeados por símbolos vectoriales dedicados en `DeckIcons.kt` (mapas, dados, cartas, compendio, diario, explorador).
  - Lanzador de Android modernizado con soporte para capas adaptable y monocroma temática.
- **Validación en Dispositivo Físico (Motorola One Fusion):**
  - Verificada en hardware real la integración de cartas *The Story Engine: Deck of Worlds* con selector de opciones dinámico (modos 1, 2 y 4 pistas con tiradas de dado d2/d4 y brújula de orientación).
  - Corregida la colisión entre el `ModalBottomSheet` de selección de mazos y el diálogo de instalación de paquetes de inicio en `SessionViewModel`.

---

### Gestión Dinámica de Mazos en Sesión Activa & Selector de Paquetes de Inicio (18 de octubre de 2026)

**Categoría:** `FEATURE / TABLETOP UX / ARCHITECTURE`

Se implementó el sistema completo para añadir, conmutar y gestionar mazos en una sesión de juego ya iniciada sin perder el progreso ni salir de la mesa:
- **Selector Modal de Mazos (`SessionDeckPickerSheet`):**
  - Búsqueda en tiempo real por título o etiquetas del mazo.
  - Vista previa de miniatura con `toCoilModel()`, total de cartas disponibles y etiqueta de sistema principal.
  - Indicador dinámico de estado (activo/inactivo) con switch de conmutación directa.
  - Badge y alerta de seguridad: si un mazo contiene cartas activas en la mesa o mano del jugador y se desactiva, se despliega un diálogo de confirmación informando que las cartas pasarán a la pila de descarte para evitar inconsistencias en el estado.
  - Acceso directo a Paquetes de Inicio (`Starter Packs`): botón rápido en la cabecera del sheet para instalar colecciones de cartas (e.g. *The Story Engine: Deck of Worlds*, *Choir of Flesh*) sin interrumpir la sesión.
- **Acceso Directo desde la Barra Lateral de Mazos (`DeckWorkspace`):**
  - Cabecera con conteo de mazos en juego (`Mazos en juego (N)`) y botón rápido *"Gestionar"*.
  - Estado vacío interactivo cuando no hay mazos asignados con botón de acción destacado: *"Añadir Mazos a la Mesa"*.
- **Acceso desde Configuración de Sesión (`SessionConfigSheet`):**
  - Sección *"Mazos en la Mesa"* con tarjeta resumen, conteo de mazos activos y botón de edición/gestión que abre `SessionDeckPickerSheet`.
- **Integración y Diálogos en `SessionViewModel` & `SessionDialogsHost`:**
  - Coordinación de estados `showDeckPicker` y `showStarterPackDialog`.
  - Escaneo dinámico de assets de paquetes de inicio (`context.assets.list("starter_packs")`) e instalación/desinstalación reactiva.

---

### The Story Engine: Deck of Worlds — Mazos Instalables y Motor de Microsetting para Mesa (18 de octubre de 2026)

**Categoría:** `FEATURE / CONTENT / TABLETOP UX`

Se integró completamente la colección oficial de cartas de creación de mundos *The Story Engine: Deck of Worlds* como paquete de inicio (`starter pack`) instalable y motor interactivo para la mesa de juego:
- **Extracción de Assets y Conversión Vectorial/WebP (246 cartas):**
  - Procesados todos los PDFs oficiales (`The Story Engine_ Deck of Worlds_All [2022-06-04]`).
  - Extraídas y recortadas las 240 cartas (40 cartas por cada uno de los 6 sub-mazos) y los 6 reversos temáticos con proporciones de borde exactas en formato WebP de alta densidad optimizado para móviles en `starter_packs/deck_of_worlds/cards/`.
  - Sub-mazos indexados:
    - *Región (Region Cards)*: 40 cartas (fondo verde/dorado) que definen la geografía y bioma.
    - *Hito / Punto de Interés (Landmark Cards)*: 40 cartas (fondo azul) para estructuras naturales o artificiales.
    - *Nombre / Identidad (Namesake Cards)*: 40 cartas (fondo naranja/ámbar) con etimologías y títulos.
    - *Origen / Historia (Origin Cards)*: 40 cartas (fondo violeta/púrpura) de trasfondo y mitología.
    - *Atributo / Rasgo (Attribute Cards)*: 40 cartas (fondo turquesa/cian) con características distintivas.
    - *Acontecimiento / Crisis (Advent Cards)*: 40 cartas (fondo rojo/carmesí) de conflictos y eventos en curso.
- **Starter Pack JSON Oficial (`story_engine_deck_of_worlds.json`):**
  - Definidos los 6 mazos con metadatos completos, reglas de orientación con pistas en 4 bordes (`FOUR_EDGE_CUES`), 4 opciones de sugerencia narrativa por carta en los ángulos 0°, 90°, 180° y 270°, y reglas de microsetting.
- **Modelo de Dominio de Microsettings (`Microsetting.kt` & `SessionBoardItemType.MICROSETTING`):**
  - Nuevas estructuras de datos `MicrosettingCluster`, `MicrosettingCardItem`, `MicrosettingSlotType` y serializador `MicrosettingCodec`.
  - Agregado el nuevo bloque de mesa `MICROSETTING` al tablero de sesión, permitiendo fijar constelaciones de cartas de mundo vivas en el tapete.
- **Generador Interactivo de Microsettings (`MicrosettingBuilderDialog`):**
  - Generación de constelaciones completas con 1 toque (robo sincronizado de los 6 mazos).
  - Selector interactivo tipo brújula para rotar y activar cada una de las 4 pistas de borde en 90°, 180°, 270° o 0°.
  - Redibujado individual por slot para reemplazar cartas específicas conservando el resto del micro-escenario.
  - Síntesis y formateo del hook narrativo listo para copiar o insertar directamente en el Diario/Notas del Director.
- **Bloque de Mesa en el Tablero de Sesión (`MicrosettingBlockContent` & `MicrosettingDelegate`):**
  - Renderizado responsivo para teléfonos y tablets con miniaturas de cartas rotadas según la orientación elegida.
  - Badges de slot con colores distintivos por mazo, controles in-situ de rotación y re-robo, y botón de exportación al diario.
- **Herramienta Central y Selectores de Bloques:**
  - Integrada la opción en el selector central y en el sheet de añadir bloques (`SessionBoardBlockPickerSheet`).

---



**Categoría:** `FEATURE / CONTENT / UX`

Se integraron de forma nativa los mapas tácticos oficiales y la biblioteca ampliada de tablas del sistema *Choir of Flesh*:
- **Mapas Tácticos Oficiales en Alta Definición (WebP):**
  - *Cuadrícula de Incursión (8 Nodos):* Renderizado en `starter_packs/choir_of_flesh/maps/incursion_grid.webp` (1684×1191 px). Viene pre-cargado con 9 pines de dungeon (Punto de Partida y Habitaciones 1 a 8), equipados con roles interactivos (`Encuentro`, `Criatura`, `Botín`) conectados a las tablas del sistema.
  - *Cuadrícula de Combate Individual (5×5):* `combat_grid.webp` calibrado al píxel exacto con celdas de 185 px, desfase `(X: 132, Y: 426)`, cuadrícula cuadrada con escala de 5 pies y rosa de los vientos.
  - *Combate Multijugador (10×5):* `multiplayer_combat_grid.webp` calibrado a 185 px por celda, desfase `(X: 267, Y: 439)`, 10 columnas por 5 filas.
- **Soporte de Mapas y Pines en el Motor de Starter Packs (`ReferenceRepositoryImpl`):**
  - `importStarterPack()` ahora importa y remapea mapas tácticos y pines con remapeo automático de IDs de tablas y serialización JSON de `tableLinks`.
  - Desinstalación limpia (`removeStarterPack`): elimina de forma segura las plantillas de mapas y pines asociados al paquete.
- **Carga de Mapas del Sistema a la Sesión (`TacticalMapSelectorSheet` & `TacticalMapDelegate`):**
  - En sesiones con sistema *Choir of Flesh*, el selector de mapas muestra el botón rápido *"Mapas Choir"* o *"Cargar Mapas de Choir of Flesh"*, clonando instantáneamente las 3 plantillas y sus pines interactivos al mapa activo de la sesión.
- **Conexión Asistente de Incursión (`ChoirIncursionDialog`):**
  - Añadido botón con icono de mapa en la cabecera del asistente para saltar de inmediato al modo Táctico con la cuadrícula de incursión.
- **Expansión de Tablas Oficiales en el Starter Pack (`choir_of_flesh.json`):**
  - Añadidas 6 tablas esenciales extraídas de los manuales y hojas de ayuda (`Tables Aid`):
    - *Tabla de Botín (D100 Looting)* (ID: -2217)
    - *Tabla de Rapiña (D20 Scavenging)* (ID: -2218)
    - *Eventos de Asentamiento / Refugio (D100)* (ID: -2219)
    - *El Precio del Coro (D100)* (ID: -2220)
    - *Dones de los Deshechos (D20)* (ID: -2221)
    - *Complicaciones de Escena (D6)* (ID: -2222)
  - El paquete ahora cuenta con 22 tablas aleatorias oficiales y 315 entradas.

---

### Pines × Tablas 2.0 — Vínculos Múltiples, Roles y Tiradas Rápidas en Mapa Táctico (18 de octubre de 2026)

**Categoría:** `FEATURE / ARCHITECTURE`

Se completó la implementación integral acordada para la conexión entre el sistema de pines del mapa táctico y el motor de tablas aleatorias:
- **Modelo y Base de Datos (Room v48):**
  - Incorporada entidad de dominio `PinTableLink(tableId, label)` para asignar múltiples tablas a un único pin con etiquetas de rol contextuales ("Botín", "Encuentro", "Secreto", "Evento", "Trampa").
  - Migración Room no destructiva `MIGRATION_47_48` que añade `tableLinksJson TEXT NOT NULL DEFAULT '[]'` a `tactical_map_pins`, conservando compatibilidad retroactiva completa con el campo legacy `targetTableId`.
  - Mappers y exportación/restauración de backups (`BackupRepositoryImpl`) sincronizados con el nuevo esquema.
- **Hoja Detalle de Pin (`TacticalPinSheet`):**
  - Reemplazada la tarjeta de tabla única por un contenedor de tablas de la sala (`pin.effectiveTableLinks`).
  - Cada fila muestra su chip de rol ("Botín", etc.), nombre de tabla y botón "Tirar" independiente de 1 toque.
  - Historial de descubrimientos particionado por tabla (`pin.discoveriesForTable(tableId)`), permitiendo eliminar descubrimientos individuales sin afectar a las otras tablas vinculadas.
- **Diálogo de Configuración de Pin (`TacticalPinDialog`):**
  - Soporte para agregar múltiples tablas desde el catálogo de la campaña.
  - Selector de chips de rol inmediato ("Botín", "Encuentro", "Secreto", "Evento", "Trampa") por tabla vinculada y eliminación rápida con un tap.
- **Tirada Rápida In-Situ sobre el Canvas (`TacticalFloatingPinPanel` & `TacticalMapView`):**
  - Si el pin tiene una tabla: botón directo "🎲 Tirar en [Rol] Nombre".
  - Si tiene múltiples tablas: contenedor compacto de tirada rápida con botones directos para cada tabla sin necesidad de abrir el diálogo modal completo.
- **Dungeon Key (`TacticalSidePanel`):**
  - Cada tarjeta de sala/pin en el índice lateral ahora incluye un badge de estado: total de tablas vinculadas y progreso de tiradas registradas (ej. `🎲 2 tablas • 1/2 tiradas`).
- **Registro Automático en Diario de Sesión (`TacticalMapDelegate`):**
  - Cada tirada ejecutada desde un pin o panel flotante genera de inmediato una entrada con timestamp, título del pin, rol, nombre de tabla y resultado en `dmNotes` y en el historial de sesión.
- **Validación:**
  - Tests unitarios de `:core:domain` completados con éxito (60 tareas).
  - Módulos `:core:model`, `:core:data` y `:feature:draw` compilados sin errores.
  - APK Debug de desarrollo (`:app:assembleDebug`) generado satisfactoriamente en 4m 2s.

---

### Choir of Flesh — guía de procedimientos (18 de octubre de 2026)

Se integraron las seis hojas de procedimientos como WebP sin conexión y una guía contextual para exploración exterior, preparación y exploración de incursiones y pruebas de Angustia. La guía conserva el paso, ofrece tirada física o de la app, registra la confirmación en el diario y permite volver a la secuencia tras una interrupción. El resto de los procedimientos está disponible como índice de referencia. Se ocultaron en la lectura y edición del diario las marcas internas que evitan registros duplicados. Detalle y límites en [docs/ux/choir-procedimientos-2026-10-18.md](docs/ux/choir-procedimientos-2026-10-18.md). Validación: 73 pruebas de dominio, 116 de interfaz, APK de desarrollo compilado e instalado y recorrido físico en Motorola One Fusion.

### Rediseño UX Mesa de Juego — Implementación Integral Entregas 1 a 6 (17 de octubre de 2026)

**Auditoría Física y Optimización de Pantalla (Motorola One Fusion & Landscape):**
- **Entrega 1: Recuperación de Espacio Vertical y Cabecera Compacta:**
  - *Retiro de NavigationBar global en sesión activa:* En [NavGraph.kt](file:///i:/TTRPG/Visuales/Decks/Deckapp/app/src/main/kotlin/com/deckapp/navigation/NavGraph.kt), se añadió `SessionRoute::class` a la condición `hideBottomBar`, recuperando ~70dp de espacio vertical útil para el tablero y el workspace de juego.
  - *Cabecera compacta adaptable (`SessionTopBar.kt`):* Selector de modo responsivo. En pantallas compactas (< 500dp como móviles en vertical 360-400dp), los 4 botones de modo se unifican en un pill dropdown (`[ :: Mesa ▼ ]`, `[ ⊞ Táctico ▼ ]`, etc.) con checkmark indicador. En pantallas anchas/horizontal se preserva la fila de iconos. Las acciones secundarias se agruparon en el menú overflow `MoreVert`, reduciendo padding táctil de 48dp a 32-36dp para Undo, Spotlight y Dados.
  - *Contrato de dimensiones en BentoSidebar (`BentoSidebar.kt`):* Se eliminó el `Modifier.fillMaxSize()` raíz que forzaba overlay de pantalla completa; el `scrim` oscurecido ahora es opcional (`showOverlay = false` por defecto en sidebars empotrados) y se aplicaron dimensiones elásticas en horizontal (`widthIn(min = 260.dp, max = 320.dp)`) asegurando que el canvas central mantenga >= 450dp de ancho utilizable.

- **Entrega 2: Rutas y Salidas a Estados Vacíos:**
  - *Tracker de Combate sin encuentro:* Se eliminó la hoja inferior vacía. Ahora [SessionScreen.kt](file:///i:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/SessionScreen.kt) muestra `CombatEmptyState` con ilustración, explicación guiada y el botón directo de 1 toque `⚡ Iniciar Combate Rápido`, que invoca `startQuickCombat()` en [SessionViewModel.kt](file:///i:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/SessionViewModel.kt) inicializando de inmediato un encuentro vinculado con seguimiento de rondas.
  - *Hexploración sin mapa vinculado:* El bloque de tablero en [BoardBlockMeta.kt](file:///i:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/BoardBlockMeta.kt) ahora evalúa `linkedHexMapId != null` para mostrar honestamente "Sin mapa vinculado" en vez de "Mapa activo". Además, en modo Hexploración se renderiza un estado vacío con botón interactivo `⚙ Vincular Mapa en Ajustes`.
  - *Pestaña de Referencias conectada:* Corregido el desvío en [BoardCentralTool.kt](file:///i:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/BoardCentralTool.kt) y en los rieles laterales; `SidebarTool.REFERENCES` ahora monta directamente el componente `ReferenceTab` con las tablas de referencia y reglas del sistema de la sesión.
  - *Diario de Bitácora Cronológico:* Se reemplazó el texto provisional por `JournalTab`, permitiendo visualizar el registro cronológico de bitácora y acceder rápidamente a la edición de notas.

- **Entrega 3: Bloques y Consulta — PNJs, Tablas y Mazos:**
  - *Ficha Compartida Unificada (`NpcDetailSheet.kt`):* Creada en `core:ui` y adoptada en `NpcsTab`, `NpcListScreen` y el visor de búsqueda de sesión. Reorganizó la ficha situando estadísticas, recursos de sistema (Choir: Flesh/Armor/Stress/HP) y acciones en la zona superior visible, con retrato táctil ampliable en diálogo modal y reproducción de voz.
  - *Búsqueda en tiempo real y chips de sistema en PNJs (`NpcsTab.kt`):* Buscador dinámico con filtrado instantáneo por nombre y tags, y chips de sistema ("Choir of Flesh", "D&D / d20", etc.).
  - *Visor de Tabla Limpio (`TableDetailSheet.kt`):* Cabecera concisa con fórmula y nombre legible; acciones de administración (Editar / Exportar) agrupadas en menú contextual `MoreVert`, liberando espacio para las entradas de tabla. Botón directo de 1 toque "Registrar en Diario" para asentar tiradas o selecciones manuales.
  - *Catálogo de Tablas (`TablesTab.kt`):* Limpieza de tags técnicos (`.json` eliminado del chip de sistema visible), reemplazo de 3 FABs flotantes apilados por un único FAB con dropdown de administración, y tarjetas compactas con botón directo de tirada rápida.
  - *Bloque de Mesa Sin Vínculo (`TableBlockContent.kt`):* Sustituido el botón deshabilitado "Tirar" por un banner explicativo con botón activo "Elegir tabla" que abre el catálogo para vincularla de inmediato.
  - *Mazos de Juego (`DeckWorkspace.kt`):* Botón explícito "Robar" y flecha de expansión; supresión del `SwipeToDismissBox` dentro del `LazyRow` horizontal de la mano para prevenir descartes involuntarios al hacer scroll.
  - *Historial de Sesión (`SessionHistoryScreen.kt`):* Corregido el rótulo técnico "Carta #null" en eventos de reinicio de mazo.

- **Entrega 4: Registro y Escritura — Notas, Diario y Teclado:**
  - *Teclado móvil sin obstrucción en Notas (`NotesTab.kt`):* Insets dinámicos de teclado (`Modifier.imePadding()`) posicionados en la columna raíz, logrando que la barra de herramientas Markdown flote justo sobre el teclado virtual de Android sin tapar el texto. Scroll fluido y área visible óptima.
  - *Persistencia de estado y rotación:* Uso de `rememberSaveable(stateSaver = TextFieldValue.Saver)` para preservar cursor, selección, texto y scroll al rotar o alternar entre herramientas.
  - *Superficie dual:* Selector visual claro entre "Notas Libres" (editor Markdown) y "Actividad" (lector cronológico de bitácora con copia de 1 toque).
  - *Hojas inferiores al 92%:* En móvil vertical, `ModalBottomSheet` expande a 0.92f de la pantalla con `skipPartiallyExpanded = true` para edición de notas y lectura de diario sin compresión.
  - *Visor Wiki Markdown (`WikiQuickPreviewDialog.kt`):* Diálogo completo de previsualización rápida con renderizado Markdown, tags de categoría e imágenes ampliables, conectado con callbacks `onWikiClick` en la mesa y plan.

- **Entrega 5: Juego Activo — Táctico, Combate, Hexploración y Mythic:**
  - *Eliminación de Solapamiento Top HUD (`SessionTopHudCapsule.kt`):* Reubicado dentro del flujo secuencial de layout de la pantalla en lugar de flotar como overlay absoluto sobre las pestañas de mesa. Chips de Hilos, Combate y Relojes sólo se renderizan cuando existen alertas reales, dejando 100% visible el espacio de pestañas ("Principal", "Exploración", "Editar").
  - *Hotbar Dock Discreto (`SessionHotbarDock.kt` & `SessionViewModel.kt`):* Estado inicial minimizado por defecto (`isHotbarExpanded = false`). La sesión abre con la píldora compacta `⚡ Atajos (4) ^` en la esquina inferior derecha, dejando todo el contenido del tablero visible y accesible.
  - *Protección contra clics accidentales en Combate (`CombatBlockContent.kt`):* Diálogo modal de confirmación `showConfirmEndCombat` antes de finalizar o reiniciar el combate, previniendo toques accidentales sobre el icono de papelera al lado de "Siguiente turno".
  - *Ajuste de offset en Mapa Táctico (`TacticalMapView.kt`):* Margen superior adaptable en teléfonos para que los controles no colisionen con el selector de mapas ni el título. Confirmación modal en `TacticalTokenHud.kt` antes de eliminar tokens.
  - *Mythic Contextual:* Estado vacío con acción directa "Vincular en Ajustes".

- **Entrega 6: Procedimientos y Accesibilidad:**
  - *Barra de herramientas de sesión (`SessionBottomBar.kt`):* Barra inferior horizontal con desplazamiento suave para las 9 herramientas principales de sesión (`Mazos`, `Tablas`, `Combate`, `Notas`, `PNJs`, `Reglas`, `Wiki`, `Plan`, `Mythic`). Permite acceder a cualquier herramienta desde vertical sin requerir la presencia de un bloque en el tablero.
  - *Validación en dispositivo físico:* Compilación Release de 4m 33s, instalación vía ADB y verificación interactiva paso a paso en el Motorola One Fusion real (taps en Tablas, PNJ, Reglas, scroll horizontal de la barra, confirmación de combate).


---

### Rediseño de Layout y Eliminación de Solapamientos en la Mesa de Juego (17 de octubre de 2026)

**Arquitectura de Capas y Resolución de Colisiones en Pantalla Móvil y Desktop:**
- **Coordinación Inteligente Inferior (Hotbar vs TacticalToolbar):**
  - Implementación de alternancia dinámica y exclusión mutua: al expandir la Hotbar de macros/dados (`SessionHotbarDock`), la barra táctica se minimiza automáticamente a una píldora compacta (`Herramientas` con acceso de 1 toque para restaurar).
  - Al seleccionar cualquier herramienta táctica o interactuar con el lienzo del mapa, la Hotbar se contrae automáticamente sin invadir el área de juego.
  - Anclaje coordinado de elevación: cuando la Hotbar está colapsada en modo táctico, se posiciona en `BottomEnd` a `80.dp` de la base, flotando limpiamente por encima de la barra táctica sin pisar los botones de acción ni el banner de resolución de ataque (`TacticalAttackResolutionBanner`).
- **Desduplicación y Pila Vertical Superior (Top Stack):**
  - En modo Táctico (`SessionViewMode.TACTICAL`), la Cápsula HUD superior (`SessionTopHudCapsule`) suprime el módulo de combate duplicado, delegando el tracking de turnos exclusivamente a la `TacticalInitiativeBar` nativa del mapa (con retratos y auto-foco).
  - Reemplazo de anclajes dispersos por una columna unificada (`Column` en `TopCenter` con espaciado uniforme): ahora se apilan de forma orgánica el banner de calibración guiada, el indicador de puntería, los breadcrumbs de mapas anidados, la barra de iniciativa y el mini-HUD del token seleccionado, eliminando el solapamiento visual entre ellos.
  - La cabecera del mapa (`TopStart`) se adaptó con ancho máximo y truncamiento elíptico para evitar invadir la barra de iniciativa en pantallas estrechas.
- **Acceso Limpio al Panel Lateral y Despeje de Canvas:**
  - Se añadió un botón directo en la `TacticalToolbar` principal para abrir/cerrar el Panel Lateral Táctico (dados, destino, notas del DM, índice de salas), eliminando la solapa flotante invasiva en medio del borde derecho en modo vertical/móvil.
  - El mini-panel flotante de notas fijas / pines (`TacticalFloatingPinPanel`) se silencia automáticamente cuando el drawer lateral está abierto en teléfonos, evitando solapamientos y conflictos de gestos táctiles.

---

### Sistema Avanzado de Pines y Dungeon Key en Mapa Táctico (17 de octubre de 2026)

**Pines Tácticos — Presets Rápidos, Drag & Drop y Renderizado Semántico**
- **Presets de Colocación Inmediata a 1 Toque (`TacticalToolbar.kt`):**
  - Al seleccionar la herramienta `PIN`, se despliega una sub-barra contextual animada con 6 presets directos: `ROOM_AUTO` (numeración autoincremental de salas estilo Dungeon Crawl), `TRAP` (Trampa), `TREASURE` (Tesoro), `DOOR_SUBMAP` (Puerta / Escalera), `COMBAT` (Encuentro) y `LORE_NOTE` (Nota).
  - Incluye botón para reiniciar el contador de salas a 1 y toggle visual para ocultar/mostrar pines secretos de DM.
  - Se eliminó la apertura accidental de diálogos al tocar casillas vacías en modos `NAVIGATE` y `SELECT`. Long press abre el diálogo completo de creación/edición.
- **Renderizado Dinámico en Canvas (`TacticalMapCanvas.kt`):**
  - Los pines muestran badges inteligentes con el número de sala o glifo semántico (`⚠`, `★`, `⎅`, `☠`, `i`, `?`).
  - Respeto del estado de exploración (`MapPinExplorationState.DISCOVERED` con halo cian, `EXPLORED` con atenuado sutil, `UNKNOWN`).
  - Distinción visual para pines secretos (borde discontinuo púrpura) y pines anclados (halo dorado).
- **Arrastre Dinámico y Bloqueo de Posición (Drag & Drop + Lock):**
  - En modo `PIN`, los pines desbloqueados pueden ser arrastrados a cualquier celda de la grilla con persistencia instantánea en la base de datos.
  - Indicador de candado en el pin y toggle en el panel para prevenir reubicaciones involuntarias.
- **Notas Fijadas en Pantalla ("Sticky Notes" en `TacticalFloatingPinPanel.kt`):**
  - Se incorporó el botón de chincheta (`PushPin`): permite mantener la tarjeta de la sala o secreto anclada en pantalla incluso mientras el DM interactúa con otras áreas del mapa o tokens.
  - Botón para colapsar la nota fija en una pastilla horizontal compacta en la esquina superior con título y acceso de 1 toque a expandir o cerrar.
- **Pestaña "Dungeon Key" / Índice de Salas en el Panel Lateral (`TacticalSidePanel.kt`):**
  - Nueva pestaña `Pines` que lista todas las habitaciones y puntos de interés del mapa actual.
  - Buscador en tiempo real por texto o descripción, filtros por categoría ("Salas", "Trampas", "Tesoros", "Combate", "Secretos") y métricas de exploración (ej. Exploradas: 3 / 10).
  - Botón de 1 toque para centrar la cámara suavemente sobre el pin en el mapa (`onFocusPinCoordinates`).

---

### Choir of Flesh — Flujo guiado de tablas y ajustes de reglas (17 de octubre de 2026)

**Tablas — Consulta manual, registro y tirada animada**
- En una sesión, abrir una tabla permite leer sus resultados sin tirar. Al tocar una fila se registra la elección en el diario; para rangos se puede indicar el valor del dado y se valida contra la fila seleccionada.
- Las tiradas directas muestran la tabla y animan el desplazamiento hasta la fila elegida. Las tablas de rango conservan el valor exacto obtenido, y no solo el límite inferior de la fila.

**Choir of Flesh — Reglas y persistencia**
- Cotejo de heridas infectadas y reglas de incursión con las ayudas del sistema: una o más heridas menores infectadas producen una herida mayor; se corrigieron rutas de cuadrícula, descansos y efectos automáticos no respaldados por el material.
- Guardar la ficha de una criatura persiste los cambios y sincroniza copias preparadas y encuentros activos.
- Validación: compilación final de módulos de tablas, sesión y APK Debug completada correctamente. Las pruebas de dominio (59) habían pasado en la compilación completa anterior; el último ajuste del índice de fila se verificó con esta compilación.

---
---

### Rediseño de Layout y Resolución de Superposición en la Mesa de Juego (16 de octubre de 2026)

**UI & UX — Optimización de Espacio y Eliminación de Solapamientos en `SessionScreen`**
- **Cápsula Flotante Superior Unificada (`SessionTopHudCapsule.kt`):**
  - Se unificaron los 3 widgets flotantes superiores que colisionaban horizontalmente en pantallas móviles (`SessionThreadsOverlay` en `TopStart`, `SessionCombatFloatingHud` en `TopCenter` y `SessionClocksOverlay` en `TopEnd`) en una única pastilla horizontal centrada compacta con diseño de cristal translúcido.
  - Implementación de **expansión mutuamente exclusiva**: al tocar el chip de Hilos, Combate o Relojes, se despliega únicamente ese panel y se cierran automáticamente los demás, previniendo invasión del área de juego.
  - El chip de combate activo incluye avance rápido de turno en 1 toque directo desde la cabecera.
- **Acordeón Colapsable para Escenas en Solitario (`SoloScenePanel.kt`):**
  - Transformado en un componente tipo acordeón que inicia plegado en una barra delgada de ~40dp (mostrando título de escena, factor de caos y botón selector) liberando el 40% del alto vertical superior que antes asfixiaba los bloques de la mesa.
  - Expansión animada fluida con un toque para acceder a notas de contexto y oráculos de escena.
- **Dock de Atajos Rápidos No Intrusivo (`SessionHotbarDock.kt`):**
  - Al estar colapsada, la pastilla de atajos se ubica en la esquina inferior derecha (`Alignment.BottomEnd`) ocupando solo su ancho mínimo para no bloquear el centro ni las cartas de la mesa.
  - Al expandirse, se despliega a pantalla completa en la base para facilitar el acceso ergonómico con los pulgares.
- **Despliegue Directo:**
  - Compilado APK Release sin minificación (`app-release.apk`) e instalado exitosamente vía ADB por Wi-Fi en el dispositivo Motorola One Fusion.

---

### Modularización Arquitectónica de `:feature:draw` — Fase 3: Modularización de UI en `SessionScreen` (16 de octubre de 2026)

**ARCHITECTURE — Descomposición Modular de la Pantalla Principal de Sesión (`SessionScreen.kt`)**
- **Creación de `SessionTopBar` (`com.deckapp.feature.draw.components.SessionTopBar`):**
  - Encapsula toda la barra superior `TopAppBar`: cronómetro de tiempo transcurrido en sesión, botón de deshacer último robo animado, selector central de modos (Mesa de Juego, Mapa Táctico, Mapa de Hexploración, Oráculo Mythic), asistente de encuadre de escenas, accesos directos al Coro de Carne (Inquebrantables e Incursión), buscador Spotlight (`Ctrl+K`), lanzador de dados, historial y configuración.
  - Alivia ~150 líneas de maquetación en `SessionScreen.kt`.
- **Creación de `SessionDialogsHost` (`com.deckapp.feature.draw.components.SessionDialogsHost`):**
  - Centraliza la orquestación y el ciclo de vida de más de 12 diálogos modales, hojas de detalle y alertas contextuales: previsualizaciones rápidas de NPCs y Lore Wiki, visor de tablas aleatorias `TableDetailSheet`, configuración global de sesión `SessionConfigSheet`, edición de botones Hotbar, asistente de escenas, edición y detonación de Relojes de Tensión, gestión de Hilos y Personajes de Mythic, Command Palette Spotlight, y todos los diálogos y confirmaciones del mapa táctico (creación de pines, hojas de pin, creación/selección/renombrado/eliminado de mapas).
  - Alivia ~320 líneas de diálogos dispersos en `SessionScreen.kt`.
- **Creación de `DeckWorkspace` (`com.deckapp.feature.draw.components.DeckWorkspace`):**
  - Extrae el área de trabajo y cluster de mazos de cartas (`DeckWorkspace` y `DeckClusterItem`) con soporte de swipe-to-dismiss para descarte de cartas, ratios de aspecto personalizados y reversos dinámicos.
  - Alivia ~110 líneas de código privado al fondo del archivo.
- **Impacto y Limpieza en `SessionScreen.kt`:**
  - Reducción drástica del archivo monolítico de **1.380 líneas a 834 líneas** (~40% de reducción).
  - Componentes reutilizables, legibles y desacoplados con responsabilidades claras y ciclo de vida visual ordenado.
- **Validación y Pruebas Unitarias:**
  - Compilación limpia de `:feature:draw:compileDebugKotlin`.
  - Ejecución de la suite completa de pruebas unitarias (`:feature:draw:testDebugUnitTest`): **109 de 109 pruebas aprobadas exitosamente**.

---

### Modularización Arquitectónica de `:feature:draw` — Fase 2: Delegados de Mythic, Spotlight y Relojes/Acciones (16 de octubre de 2026)

**ARCHITECTURE — Desacoplamiento de SessionViewModel en Delegados Especializados (Fase 2)**
- **Creación de `SessionMythicDelegate` (`com.deckapp.feature.draw.delegates`):**
  - Encapsula el Oráculo Mítico (Fate Checks, Factor de Caos, tiradas forzadas, eventos aleatorios), escenas en solitario y diario estructurado (`saveSoloScene`, `closeSoloScene`), gestión de hilos y personajes de trama (`threads` y `characters`), y captura/traspaso de inspiración al diario de sesión.
  - Alivia ~480 líneas de lógica en `SessionViewModel`.
- **Creación de `SessionSpotlightDelegate` (`com.deckapp.feature.draw.delegates`):**
  - Encapsula el Command Palette Spotlight (Sprint 64): búsqueda global asíncrona reactiva con debounce de 150ms sobre tablas aleatorias, PNJs, reglas del sistema SRD, criaturas de encuentros y notas de sesión.
  - Gestiona la conversión fluida de resultados encontrados en criaturas añadidas al combate o fragmentos exportados al diario.
  - Alivia ~250 líneas de búsqueda y orquestación.
- **Creación de `SessionClocksDelegate` (`com.deckapp.feature.draw.delegates`):**
  - Encapsula la gestión completa de Relojes de Tensión (Sprint 61: creación, avance por pulsos, reseteo, detonación y volcado de alertas de impacto al diario), la botonera de acciones rápidas Hotbar (Sprint 59: ejecución polimórfica de dados, oráculos, tablas y cartas) y el Asistente de Encuadre de Escenas (Sprint 60: chequeos d10 de caos, giros de situación y generación estructurada de escenas en Markdown).
  - Alivia ~380 líneas de lógica transversal.
- **Reducción Masiva de `SessionViewModel`:**
  - `SessionViewModel.kt` se reduce de 2.534 líneas iniciales a 1.273 líneas (~50% de reducción de tamaño), manteniendo 100% de compatibilidad binaria y funcional mediante métodos forwarders directos.
- **Validación y Pruebas Unitarias:**
  - Creados tests unitarios exhaustivos para los 3 nuevos delegados:
    - `SessionMythicDelegateTest` (9 tests unitarios).
    - `SessionSpotlightDelegateTest` (8 tests unitarios).
    - `SessionClocksDelegateTest` (7 tests unitarios).
  - Ejecución de la suite completa de pruebas unitarias (`:feature:draw:testDebugUnitTest`): **109 de 109 pruebas aprobadas con 0 fallos**.

---

### Modularización Arquitectónica de `:feature:draw` — Fase 1: Delegados de Cartas y Combate (16 de octubre de 2026)

**ARCHITECTURE — Desacoplamiento de SessionViewModel en Delegados Especializados**
- **Creación de `SessionDeckDelegate` (`com.deckapp.feature.draw.delegates`):**
  - Encapsula toda la lógica de cartas, mazos, descarte, robo aleatorio, robo por palo (`DrawBySuit`), barajado reactivo de pila de descarte (`shufflePileBack`), peek sin mutación, ordenamiento de mano y colapso de clusters Bento.
  - Alivia ~300 líneas de lógica acoplada en `SessionViewModel`.
- **Creación de `SessionCombatDelegate` (`com.deckapp.feature.draw.delegates`):**
  - Encapsula tracker de combate, asaltos, turnos, iniciativa, cálculo de orden determinista, daño, condiciones, participantes temporales de jugadores (PJs), exportación de logs narrativos y reglas de combate del Coro de Carne (*Choir of Flesh* / Inquebrantables).
  - Alivia ~350 líneas de lógica de combate y persistencia de notas en `SessionViewModel`.
- **Compatibilidad 100% hacia atrás en `SessionViewModel`:**
  - Instanciación de ambos delegados en el ViewModel con métodos pasarela (*forwarders*) para preservar íntegra la API pública requerida por `SessionScreen` y tests existentes sin romper llamadas ni contratos.
- **Validación y Pruebas Unitarias:**
  - Creados tests unitarios aislados: `SessionDeckDelegateTest` y `SessionCombatDelegateTest` con MockK y Coroutines Test.
  - Compilación de `:feature:draw:compileDebugKotlin` exitosa.
  - 85 tests unitarios de `:feature:draw:testDebugUnitTest` aprobados al 100%.

---

### Choir of Flesh — Asistente de Juego Guiado: Cierre de Combate, Incursión (8 Nodos) y Asentamiento (16 de octubre de 2026)

**FEATURE — Cierre de Combate Asistido (Aftermath pp. 60-61)**
- `ChoirCombatAftermathUseCase.kt` en `core/domain`:
  - Lógica canónica según el manual: tiradas de salvación de Constitución (CD 10 + heridas menores) para evitar que las heridas menores supuran y se conviertan en heridas mayores.
  - Asistencia médica con chequeo de Inteligencia: reduce la dificultad o evita la supuración según el éxito del médico.
  - Cálculo de Trauma (+1 por cada herida mayor sostenida o supurada) e impacto en la psique del Inquebrantable.
  - Limpieza de heridas temporales de combate y adjudicación atómica de botín, recursos y puntos de experiencia (XP).
- `ChoirCombatAftermathDialog.kt` en `core/ui`:
  - Modal interactivo con tiradas d20 integradas o asistidas.
  - Vista reactiva del pronóstico de salud, desglose de supuración y adjudicación de recompensas.
- Integración en `CombatTab.kt`:
  - Botón "FINALIZAR" de la mesa táctica de combate ahora dispara el flujo guiado de Aftermath para todos los Inquebrantables participantes.
  - Persistencia atómica de las actualizaciones a través de `UpdateChoirCharacterUseCase.applyAftermath` con registro detallado en `CombatLogEntry`.

**FEATURE — Tablero de Incursión Persistente (8 Nodos) & Asentamiento**
- `ChoirIncursion.kt` en `core/model`:
  - Modelos de datos para el tablero de expedición: `ChoirIncursion`, `ChoirIncursionNode`, `ChoirNodeType` (Entrada, Pasaje, Cámara de Carne, Altar, Guarida del Corifeo, etc.), `ChoirNodeState` (Oculto, Revelado, Despejado, Huyendo).
  - Modelo `ChoirSettlement` para la gestión del refugio: moral, defensas, raciones, suministros, instalaciones construidas y tiradas de rumores/eventos d10.
- `ChoirIncursionCodec.kt` en `core/domain`:
  - Serialización segura Base64 delimitada en las notas markdown de la sesión, garantizando persistencia multiplataforma completa sin requerir migraciones complejas de base de datos Room.
- `ChoirIncursionRules.kt` en `core/domain`:
  - Reglas de progresión de la expedición: avance entre nodos, cálculo reactivo de la Canción de la Carne (medidor de resonancia 0..10 que incrementa el peligro y las aberraciones), resolución de despeje o huida, extracción segura al asentamiento, descansos breves y descansos prolongados.
- `ChoirIncursionDialog.kt` en `core/ui`:
  - Diálogo modal con 2 pestañas temáticas:
    1. *Tablero de Incursión*: visualización de los 8 nodos en ruta, estado de activación, medidor visual de la Canción de la Carne y botones de avance/descanso.
    2. *Asentamiento & Refugio*: gestión de métricas del enclave humano (moral, defensas, víveres), registro de instalaciones y lanzador de rumores d10.
- Integración en `SessionScreen.kt` y `SessionViewModel.kt`:
  - Botón de cabecera "Incursión" con icono de brújula (`Icons.Default.Explore`) en sesiones con reglas de Choir of Flesh.
  - Sincronización bidireccional y guardado automático en las notas de la sesión activa.

**VALIDACIÓN Y PRUEBAS**
- 100% de tests unitarios aprobados en `:core:domain:testDebugUnitTest`:
  - `ChoirCombatAftermathUseCaseTest` (evaluación médica, salvaciones de CON, cálculo de trauma y limpieza de heridas).
  - `ChoirIncursionRulesTest` (avance por el grafo de 8 nodos, cálculo de Canción de la Carne y descansos).
- Compilación completa de `:app:assembleDebug` exitosa con Gradle `--gradle-user-home I:\.gradle_home`.
- Instalación y validación en dispositivo físico Motorola One Fusion (`ZE222B5GXX`) vía ADB, con lanzamiento de `MainActivity` verificado mediante capturas de pantalla.


- Se completan Caballo, Sabueso, Penitente, Purificador, Soldado y Superviviente: 30 fichas con retratos WebP existentes.
- Las 30 descripciones, estadísticas, 90 acciones y rasgos se extraen del capítulo V completo del manual local. Se eliminan las conversiones inventadas a PG/CA de las fichas empaquetadas; se conservan los grados dinámicos, números de grupo ♟, alcances, tipos de daño y referencias de página.
- `scripts/integrate-choir-bestiary.py` genera `bestiary.json` y actualiza el paquete de manera reproducible. El generador Kotlin consume este bestiario cotejado, para no restaurar las fichas anteriores.
- Actualización automática de paquetes instalados: incorpora las fichas ausentes, sustituye texto anterior solo si coincide con el original distribuido, conserva notas/retratos personales e incorpora reglas a los encuentros vinculados a PNJs del paquete. No cambia PG actuales ni otros campos de encuentros existentes.
- Sidebar y HUD de sesión comparten controles de heridas mayores/menores, decisión d10 según heridas, objetivo d6, rasgos visibles y consulta de acciones. Estado persistido en las notas del encuentro, compatible con copias y respaldos; cambios registrados en historial y heridas incluidas al copiar el asalto al diario.
- Iniciativa fija de PNJs Choir, intercalada con iniciativas de jugadores. Al añadir un PNJ desde la vista rápida se conservan ficha, imagen e identidad, en lugar de descartarlos por una descripción y valores 10/10.
- Calculador de NO del PJ basado en la tabla dinámica del capítulo III (modificadores −5 a +10). Maestría y Carga se aplican después. Los efectos particulares, excepciones de rasgos, duración, inmunidades y tablas de efectos de heridas se resuelven consultando la ficha; no se ejecutan automáticamente como daño de PG.
- Validación: 360 campos estadísticos y 90 entradas de acciones coinciden con el capítulo V; 30 identidades únicas y extracción reproducible. Pasaron 49 pruebas de dominio y 1 del paquete. APK debug compilada y auditada: JSON idéntico a la fuente cotejada, 30 retratos resueltos, 116 WebP reales, 16 tablas aleatorias y clases nuevas de combate presentes. SHA-256: `cb3ec102c2ffbd5dcc1fde591e2be13786d2c774c158ea74395d2297ef8bd5ba`. No se instaló en el dispositivo en esta entrega.


### Sprint 67 — Ergonomía Visual Bento para Mapas Tácticos y Gestión Segura de Escenarios (16 de octubre de 2026)

**FEATURE — Selector de Mapas Tácticos Estilo Bento (`TacticalMapSelectorSheet`)**
- Rediseño completo del modal de mapas con tarjetas horizontales Bento y miniaturas WebP (`AsyncImage` de Coil con fallback temático de grilla táctica de 80x80dp).
- Badges visuales de estado: indicador `ACTIVO` en verde tonal, subtítulo con dimensiones y escala (`square • 5ft/celda`), y vínculo visible con escenas de la sesión.
- Barra de acciones rápida en cada tarjeta:
  - Cambiar imagen de fondo (`onChangeMapImage` con launcher SAF integrado y pipeline automático a WebP).
  - Renombrar mapa (`onRenameMap`).
  - Vincular con escena de sesión (`onLinkScene`).
  - Eliminar mapa táctico (`onDeleteMap`).
- Botón de cabecera en el Sheet: `+ Nuevo Mapa` para creación rápida.

**FEATURE — Ergonomía de Cabecera y Bloqueo de Eventos en Canvas**
- Reemplazo del menú contextual anticuado por un chip flotante táctico en `TacticalMapView.kt`:
  - Contenedor `Surface` con icono de capas (`Icons.Default.Layers`), nombre del mapa activo y chevron desplegable.
  - Botón de calibración rápida de grilla (`Icons.Default.GridOn`) accesible directamente.
  - Aislamiento de punteros táctiles mediante `Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}` en el Surface padre, evitando que toques en la cabecera caigan accidentalmente en `TacticalMapCanvas` y abran pines de dungeon.
- Estado vacío (`uiState.map == null`): Botón tonal `Elegir Mapa (${uiState.availableMaps.size})` para acceder al selector si ya existen mapas cargados en la sesión.

**FEATURE — Eliminación Segura y Limpieza Física de Huérfanos (`TacticalDeleteMapConfirmDialog`)**
- Diálogo modal de confirmación destructiva con advertencia de Material3: informa la eliminación del mapa, sus tokens y pines vinculados, y la liberación de almacenamiento en disco.
- En `TacticalMapDelegate.kt`:
  - `confirmDeleteMap`: borra la entidad en la base de datos Room (`tacticalMapDao.deleteMap(mapId)` que aplica `CASCADE` a tokens y pines).
  - Limpieza física de disco: verifica si la ruta del mapa eliminado reside en el directorio interno `tactical_maps` y la elimina vía `fileRepository.deleteFile(path)`, evitando la acumulación de archivos WebP huérfanos.
  - Conmutación automática: si el mapa eliminado era el actualmente activo, conmuta de forma transparente al primer mapa restante de `availableMaps` o deja el estado limpio (`null`).
  - `updateMapImage`: al reemplazar el fondo de un mapa, limpia el archivo `.webp` anterior si era interno antes de guardar el nuevo.

**VALIDACIÓN EN HARDWARE FÍSICO (Motorola One Fusion)**
- Compilado e instalado vía `./gradlew installDebug --gradle-user-home I:\.gradle_home`.
- Apertura fluida del `TacticalMapSelectorSheet` desde el chip de cabecera sin falsos toques en el canvas.
- Despliegue del diálogo de confirmación `TacticalDeleteMapConfirmDialog`.
- Eliminación interactiva con feedback por Snackbar (`Mapa '...' eliminado`), transición al estado vacío y recreación instantánea con `+ Crear Mapa Rápido`.

---

### Choir of Flesh — Ilustraciones WebP y carga de tablas en sesiones (16 de octubre de 2026)

**FEATURE — Arte original integrado**
- 116 ilustraciones originales del directorio `Choir_of_Flesh_clean`, convertidas a WebP (calidad 90, resolución máxima 1600 px), con 24 retratos de criaturas y 13 galerías de referencia por capítulo.
- Correspondencia de retratos verificada con los encabezados del capítulo V del manual y hashes SHA-256 de los originales. Los archivos del Vault se leen sin modificarlos.
- Activos en `core/data/src/main/assets/starter_packs/choir_of_flesh/art/`; manifiesto reproducible en `artwork.json` y pipeline en `scripts/integrate-choir-artwork.py` (requiere Pillow y el capítulo V del Vault junto al directorio de origen).
- Las galerías aparecen en Referencias, categoría `Ilustraciones del manual`, y se abren en vista previa. Markdown muestra imágenes locales mediante Coil, con proporción conservada.
- El generador del starter pack conserva los retratos y galerías al regenerar el JSON.

**FIX — Recursos de sistema en sesiones existentes y nuevas**
- `SyncSessionSystemTablesUseCase` conecta los nombres de sistemas con sus identificadores de paquete y añade las tablas correspondientes a la sesión, incluyendo instalaciones del paquete mientras la sesión está abierta.
- Se preservan las tablas añadidas manualmente y se evita duplicar vínculos. Las sesiones `General` no reciben indiscriminadamente todos los paquetes.
- Se permite crear sesiones con un sistema seleccionado sin exigir un mazo de cartas.
- Se cancelan observadores de tablas al cambiar de sesión y se conserva la selección del usuario entre todas las tablas y las tablas de sesión.

**FIX — Actualización e identidad de los datos instalados**
- Al iniciar la app, el paquete instalado recibe retratos, galerías y tablas ausentes sin reemplazar estadísticas, notas, imágenes personalizadas ni contenidos existentes.
- La actualización automática utiliza búsquedas por propiedad exacta del paquete para conservar tablas personalizadas de igual nombre.
- Reinstalar un starter pack actualiza sus entidades conservando IDs y vínculos de sesión; evita el retorno `-1` de `INSERT IGNORE` sobre IDs existentes.
- Editar reglas conserva `sourcePack`. No se requiere migración de esquema.

**VALIDACIÓN**
- 33 pruebas de dominio aprobadas, incluidas 7 nuevas para carga de recursos, idempotencia y cambios de sistema; prueba existente del paquete Choir of Flesh aprobada.
- 116 WebP decodificados y verificados, 24 retratos distintos y 116 enlaces de galería válidos; consultas SQL de propiedad verificadas con tablas de nombres coincidentes.
- Ensamblado final verificado: `BUILD SUCCESSFUL` en `choir-validation-final.log`. APK inspeccionado: 116 WebP reales, 24 retratos, 13 galerías y 116 enlaces válidos a activos incluidos.
- Paridad Desktop: pendiente de trasladar activos y sincronización de tablas de sistema.

---

### Sprint 66 — Optimización de Mapas Tácticos a WebP y Ergonomía del VTT en Pantalla (16 de octubre de 2026)

**FEATURE — Pipeline de Procesamiento y Conversión Automática a WebP**
- Implementado en `FileRepositoryImpl.kt` (`processTacticalMapImage`):
  - Ingesta de cualquier formato de imagen (`image/*`: PNG, JPG, BMP, etc.) mediante `ContentResolver` o ruta local directa.
  - Decodificación con `inJustDecodeBounds` para cálculo adaptativo de `inSampleSize` que previene `OutOfMemoryError` en battlemaps de alta resolución (4K/8K).
  - Escalado proporcional de alta fidelidad con resolución máxima de 2560px.
  - Compresión nativa a WebP Lossy (85% de calidad) con fallback de API para Android 11+ (`Bitmap.CompressFormat.WEBP_LOSSY`).
  - Persistencia permanente en el sandbox privado de la app (`context.filesDir/tactical_maps/{uuid}.webp`), eliminando la vulnerabilidad de pérdida de fondos por revocación de permisos SAF o borrado en galería externa.
- Integración en `TacticalMapDelegate.kt`:
  - En `createNewMap` y `updateMapImage`, el pipeline procesa la imagen de forma asíncrona en hilo de fondo (`NonCancellable + Dispatchers.IO`).
  - Feedback inmediato mediante Snackbar reactivo (`Procesando imagen a WebP...` $\to$ `Imagen de mapa actualizada y optimizada (WebP)`).
- Inyección de dependencias en `SessionViewModel.kt` pasando `FileRepository` al delegado táctico.

**FEATURE — Corrección Ergonómica de UI y Desacoplamiento de Overlays**
- **Eliminación del Solapamiento Superior:**
  - Ajustado el padding superior de la tarjeta del selector/nombre de mapa en `TacticalMapView.kt` a `60.dp`, permitiendo que el tracker de `Hilos & PNJs (Mythic)` y el título del mapa táctico coexistan de manera limpia y accesible sin taparse.
- **Reposicionamiento Adaptativo de la Action Hotbar (`SessionHotbarDock`):**
  - En `SessionScreen.kt`, el dock de atajos rápidos detecta reactivamente el modo táctico (`mainViewMode == SessionViewMode.TACTICAL`) elevándose a `76.dp` de la base. Esto garantiza que todos los botones de la barra de herramientas táctica inferior (Pan/Zoom, Puntero, Regla, AoE, Pines, Niebla, Terreno, Ruta) queden 100% despejados y operables simultáneamente.

**VALIDACIÓN Y PARIDAD**
- Instalado y probado en dispositivo físico real (`Motorola One Fusion` - Android 11) mediante ADB y captura visual completa.
- Compilación validada con éxito mediante Gradle con redirección de caché a `I:\.gradle_home` para sortear limitación de espacio en `J:`.
- *Feature Parity Mandate:* Registrado para sincronizar en `TDAPP-desktop`:
  - Implementación de `processTacticalMapImage` usando `ImageIO` o plugin de WebP en Desktop (`TDAPP-desktop`), almacenando mapas procesados en el subdirectorio de datos de usuario de escritorio.
  - Sincronización del posicionamiento de overlays en Compose Desktop para el modo táctico.

---

### Ajustes y preferencias de uso — 16 de octubre de 2026

**FEATURE — Ajustes organizados y preferencias persistentes**
- Secciones General, Apariencia y lectura, Partidas, Importación e IA, Datos y Acerca de con selector visible al desplazarse.
- Pantalla inicial configurable entre Biblioteca, Sesiones y Referencias; se conserva la navegación activa y la selección se aplica al iniciar nuevamente.
- Tamaño de texto entre 100 % y 130 %, adicional al ajuste de Android, aplicado a Compose y al contenido Markdown.
- Preferencias para mantener encendida la pantalla de la mesa y habilitar/deshabilitar la respuesta háptica de la app; valores iniciales compatibles con el comportamiento anterior.
- Preferencias guardadas en SettingsRepository con StateFlow, sin cambios de esquema de Room.

**FEATURE — Editor de clave Gemini**
- Clave oculta por defecto con acciones para mostrar, guardar y eliminar.
- Prueba explícita de conexión usando el modelo de texto existente, con indicación de consumo de cuota, progreso y tiempo máximo de 30 segundos.
- Estado de clave guardada separado del resultado de la prueba; cancelación e invalidación del resultado al cambiar la clave.

**VALIDACIÓN**
- `:app:assembleDebug`: BUILD SUCCESSFUL con la configuración final.
- `:core:domain:testDebugUnitTest`: validado; 26 pruebas, sin fallos, errores ni omisiones.
- Caché alternativa de compilación en `I:\TTRPG\Visuales\Decks\.codex-settings-gradle`, con dependencias existentes compartidas, porque la unidad J: no tiene espacio suficiente para las transformaciones de Gradle.
- Corregida la referencia de integración `mainViewMode` en el padding de la barra táctica que bloqueaba la compilación.
- Validación estática: `git diff --check`.

---

### Sprint 65 — Sistema Choir of Flesh: Starter Pack Integral, Bestiario Oficial (24 Criaturas), Asistente de Inquebrantable y Desinstalación Limpia (16 de octubre de 2026)

**FEATURE — Starter Pack Reversible "Choir of Flesh" (`choir_of_flesh.json`)**
- Empaquetado completo del sistema TTRPG en solitario *"Choir of Flesh"* (El Coro de la Carne) en formato JSON estandarizado (`core/data/src/main/assets/starter_packs/choir_of_flesh.json`) con codificación UTF-8 pura.
- **10 Reglas Oficiales del Sistema:**
  - *Mecánica Principal:* Pruebas de 1d20 + Modificador de Atributo contra Clase de Dificultad (CD). Sistema de modificadores con rango simétrico de -3 a +3 para puntuaciones de 3 a 18.
  - *Pruebas de Estrés y Angustia:* Efectos del horror cósmico-corporal, acumulación de puntos de angustia y puntos de quiebre.
  - *Combate Mortal y Daño Crítico:* Resolución ágil, heridas graves y penalizadores progresivos.
  - *Dados de Uso (Usage Die):* Mecánica de degradación de suministros y munición (`Ud4` $\to$ `Ud6` $\to$ `Ud8` $\to$ `Ud10` $\to$ `Ud12`) sin contabilidad tediosa.
  - *El Escatón y la Canción de la Carne:* Lore del apocalipsis biológico y la transmutación del mundo.
  - *El Ancla y la Humanidad:* Conexiones afectivas y recuerdos que previenen la asimilación por el Coro.
  - *Los Fragmentos de Gracia:* Reliquias santificadas que ofrecen poderes milagrosos a cambio de riesgo corporal.
  - *Magia y Milagros de Sangre:* Reglas de invocación, sacrificios de PV y manifestaciones aberrantes.
  - *Sanación y Descanso:* Reglas de respiro breve, reposo prolongado y tratamiento de infecciones de carne.
  - *Muerte y Destino Final:* Reglas de agonía, mutación póstuma y asimilación al Coro.
- **4 Tablas de Referencia (19 Filas):**
  - *Modificadores por Puntuación:* 3 (-3), 4-5 (-2), 6-8 (-1), 9-12 (+0), 13-15 (+1), 16-17 (+2), 18 (+3).
  - *Dificultad de Desafíos:* CD 8 (Fácil / Rutinaria), CD 11 (Moderada), CD 14 (Desafiante), CD 17 (Heroica / Crítica), CD 20 (Casi Imposible / Legendaria).
  - *Dados de Uso:* Tipo de dado, tirada de degradación (1-2) y probabilidad de desgaste.
  - *Escala de Angustia:* Efectos acumulativos (1-3 Inquietud, 4-6 Pánico, 7-9 Aberración mental, 10 Ruptura total).
- **16 Tablas Aleatorias Oficiales (162 Entradas):**
  - *Horrores de la Carne (d10)*, *Rumores del Páramo Escarlata (d10)*, *Efectos de la Angustia (d10)*, *Reliquias y Artefactos (d10)*, *Pecados y Máculas (d10)*, *Presagios de la Canción (d10)*, *Encuentros en el Yermo de Hueso (d10)*, *Saqueo de Cadáveres (d10)*, *Mutaciones de la Carne (d10)*, *Revelaciones del Escatón (d10)*, *Facciones del Fin (d10)*, *Lugares Blasfemos (d10)*, *Susurros del Coro (d10)*, *Semillas de Aventura (d10)*, *Armas y Despojos (d12)*, y *Generador de Inquebrantables (10 ocupaciones d10)*.
- **Bestiario Oficial Completo (24 Criaturas):**
  - Todas las criaturas del juego oficial importadas con sus statblocks exhaustivos, PV, CD, Ataques, Acciones Especiales, Lore, Comportamiento en Solitario y Etiquetas de Clasificación (`Aberración`, `Muerto Viviente`, `Cultista`, `Élite`, `Jefe`, `Constructo`, `Monstruosidad`):
    - *Amalgama de Brazos*, *Apostol del Templo Rojo*, *Arpa de Tendones*, *Baluarte de Costillas*, *Campanero Ciego*, *Centinela Osario*, *Ciervo de Piel Erizada*, *Coro Viviente*, *Custodio de Quijadas*, *Danzante de Vísceras*, *Empalador Sacro*, *Enjambre de Dedos*, *Engendro de Pulmón*, *Escultor de Tumores*, *Garganta Hueca*, *Heraldo de la Melodía*, *Hilador de Nervios*, *Madre del Vientre Pétreo*, *Monje del Hábito Purpúreo*, *Niño del Quinto Diente*, *Parásito Melódico*, *Perro Quimérico de Tendones*, *Portador de la Llama Fria*, *Tejedor de Quistes*.

**FEATURE — Soporte Nativo de Criaturas en Starter Packs con Desinstalación Limpia**
- Extensión del motor de paquetes en `ReferenceRepositoryImpl.kt`:
  - `importStarterPack`: importa criaturas definidas en el pack y las vincula con el Tag del paquete (`Choir of Flesh`). Evita duplicados si la criatura ya existe por nombre.
  - `removeStarterPack`: remueve limpiamente las reglas, tablas de referencia, tablas aleatorias y **todas las criaturas vinculadas al Tag del paquete**, eliminando también el tag si ya no tiene dependencias. No deja datos huérfanos ni afecta personajes creados por el jugador.
- Nuevos métodos en `NpcDao`: `getNpcByName(name)`, `getNpcsByTagName(tagName)`, `deleteNpcsByTagName(tagName)`.
- Método en `TagDao`: `getTagByName(name)`.

**FEATURE — Asistente de Creación de Inquebrantables (`ChoirOfFleshCharacterDialog`)**
- Modal interactivo guiado por pasos con estética gótica carmesí y oro (`Color(0xFF8B0000)` / `Color(0xFFFFD700)`), accesible desde `NpcListScreen` y directamente desde la mesa de juego en `SessionScreen`:
  - **Paso 1: Identidad y Ocupación:** 10 ocupaciones del Páramo con bonos de atributo automáticos (Cirujano de Trinchera, Sepulturero del Páramo, Monje Ciego de la Campana, Penitente de la Cadena, etc.) o aleatorización en 1 toque.
  - **Paso 2: Atributos y Modificadores:** Tirada de 3d6 para cada uno de los 6 atributos (Fuerza, Agilidad, Vigor, Intelecto, Voluntad, Presencia) con cálculo reactivo del modificador oficial (-3 a +3).
  - **Paso 3: El Ancla y el Pecado:** Selección o tirada de la persona/recuerdo que mantiene la cordura y la culpa que persigue al personaje.
  - **Paso 4: El Fragmento de Gracia:** Reliquia o don sagrado que le otorga un poder milagroso inicial.
  - **Paso 5: La Misión Inicial:** Meta personal concreta generada a partir de los rumores del páramo.
  - **Paso 6: Equipo y Resumen:** Selección de armamento, equipo de supervivencia y Dados de Uso (`Ud6`, `Ud8`, etc.).
- Cálculo automático de estadísticas vitales:
  - $\text{PV Iniciales} = \text{Vigor} + 10$
  - $\text{Clase de Dificultad (CD) Defensiva} = 10 + \text{Mod. Agilidad}$
  - $\text{Iniciativa} = \text{Mod. Agilidad}$
- Guardado instantáneo como PNJ/Personaje en la base de datos y botón *"Forjar y Anexar al Diario"* que vuelca la ficha completa estructurada en Markdown directamente en las notas de sesión (`dmNotes`) con exclusión mutua `notesMutex`.

**VALIDACIÓN Y PARIDAD**
- 3 pruebas unitarias en `ChoirOfFleshPackTest` verificando la integridad del JSON de starter pack, conteo exacto de 10 reglas, 4 ref tables (19 filas), 16 random tables (162 entradas) y 24 criaturas oficiales completas con PV y CD.
- 5 pruebas unitarias en `ChoirOfFleshCharacterCreationTest` validando cálculo exacto de modificadores (-3 a +3), estadísticas derivadas (PV, CD, Iniciativa) y formateo de ficha Markdown para el diario.
- Compilación y ejecución de tests unitarios `:core:data:testDebugUnitTest` y `:feature:npcs:testDebugUnitTest` 100% aprobadas.
- Ensamble del APK de debug verificado con éxito mediante `./gradlew :app:assembleDebug` (BUILD SUCCESSFUL en 2m 57s).
- *Feature Parity Mandate:* Registrado para sincronizar en `TDAPP-desktop`:
  1. Copia del archivo de starter pack `choir_of_flesh.json` en los assets de desktop.
  2. Métodos de `ReferenceRepositoryImpl` para importación y remoción limpia de criaturas asociadas al tag.
  3. Diálogo Compose Desktop `ChoirOfFleshCharacterDialog` con la misma estética carmesí/oro y flujo de 6 pasos.
  4. Botón "Inquebrantable" en la barra superior de la mesa de juego de desktop.

---

### Sprint 64 — Mesa de Juego: Buscador Rápido "Spotlight" (Command Palette) y Búsqueda Federada (`SessionSpotlightDialog`) (16 de octubre de 2026)

**FEATURE — Buscador Rápido Spotlight y Paleta de Comandos (`SessionSpotlightDialog`)**
- Modal central flotante con estética glassmorphism (`Color(0xF212141C)`), desenfoque de fondo y bordes iluminados, accesible instantáneamente desde cualquier pantalla de la mesa de juego mediante el icono de Lupa en el TopAppBar o a través del atajo de teclado canónico `Ctrl+K` / `Ctrl+F` (optimizado para Desktop y tablets/emuladores con teclado físico).
- **Búsqueda Federada Reactiva con Debounce (150ms):**
  - Consulta simultánea en múltiples fuentes de datos sin congelar la UI:
    - 🎲 **Tablas Aleatorias:** Coincidencias de nombre, categoría y fórmula en `TableRepository`.
    - 👤 **Personajes y PNJs:** Coincidencias de nombre, rol y notas en `NpcRepository`.
    - 📜 **Reglas y Hechizos SRD:** Búsqueda en texto y títulos de `ReferenceRepository`.
    - 🐉 **Monstruos y Criaturas:** Coincidencias en el compendio de encuentros de `EncounterRepository`.
    - 📝 **Diario y Notas:** Búsqueda contextual línea a línea en las notas de sesión (`dmNotes`).
- **Filtro de Categorías por Chips:** Selector horizontal para acotar rápidamente entre `Todos`, `Tablas`, `PNJs`, `Reglas`, `Monstruos` y `Diario` con colores temáticos e iconografía representativa.
- **Vista Previa Rápida (Quick Peek) con Acciones Contextuales en 1 Toque:**
  - Diseño responsivo adaptativo (dos paneles en horizontal / apilado en vertical).
  - Al seleccionar cualquier elemento, muestra su ficha técnica resumida y botones de acción rápida directos:
    - **Tablas:** Botón *"🎲 Tirar en Tabla"* que ejecuta la tirada en tiempo real sin salir del buscador y muestra el resultado con botón *"📋 Al Diario"*.
    - **Monstruos y Criaturas:** Botón *"⚔️ Sumar a Combate"* (añade directamente la criatura al encuentro activo de la sesión con HP, CA y notas) y botón *"📋 Al Diario"*.
    - **PNJs:** Botón *"⚔️ Sumar a Combate"* (lo inyecta con rol y descripción como participante) y *"📋 Al Diario"*.
    - **Reglas del Sistema:** Botón *"📋 Copiar Regla al Diario"* con formato Markdown limpio (título, sistema y descripción).
    - **Notas de Sesión:** Botón *"📋 Copiar Fragmento"* para reusar extractos pasados.

**FEATURE — Extensiones en Repositorio y Arquitectura Domain**
- Agregado método `addCreature(creature: EncounterCreature): Long` en la interfaz `EncounterRepository` y su implementación en `EncounterRepositoryImpl`, permitiendo inyectar criaturas directamente a encuentros en ejecución sin re-persistir todo el bloque.
- Modelos `SpotlightCategory` y `SpotlightItem` (`TableResult`, `NpcResult`, `RuleResult`, `CreatureResult`, `NoteResult`) en `feature/draw/components/spotlight/SpotlightModels.kt`.
- Atajo de teclado global con `Modifier.focusRequester()`, `.focusable()` y `.onKeyEvent()` en `SessionScreen.kt`.

**VALIDACIÓN Y PARIDAD**
- 5 pruebas unitarias en `SessionSpotlightTest` validando asignación de categorías e IDs estables, filtrado por categorías, coincidencia insensible a mayúsculas/minúsculas, y formateo de Markdown para diario; 100% aprobadas.
- Compilación y suite completa de pruebas unitarias `:feature:draw:testDebugUnitTest` (187 tareas) y ensamble `:app:assembleDebug` aprobados sin errores.
- *Feature Parity Mandate:* Registrado para sincronizar en `TDAPP-desktop`: componente `SessionSpotlightDialog`, modelos `SpotlightModels.kt`, atajo `Ctrl+K`/`Ctrl+F`, y métodos del ViewModel `openSpotlight()`, `updateSpotlightQuery()`, `rollSpotlightTable()`, `addSpotlightCreatureToCombat()`, etc.

---

### Sprint 63 — Mesa de Juego: Mini-Tracker de Combate e Iniciativa Flotante (`SessionCombatFloatingHud`) (15 de octubre de 2026)

**FEATURE — Mini-Tracker Flotante de Combate (`SessionCombatFloatingHud`)**
- Pastilla central flotante superior con estética glassmorphism (`Color(0xEB161920)`) anclada en `Alignment.TopCenter` (`padding(top = 10.dp)`), ubicada armónicamente entre Hilos/Amenazas (`TopStart`) y Relojes de Progreso (`TopEnd`), completando la trilogía superior de HUDs tácticos no invasivos de la mesa de juego.
- Solo se muestra de manera reactiva y no intrusiva cuando hay un combate activo con participantes en la sesión.
- **Vista Colapsada (Minimalist Pill):**
  - Indicador de asalto actual (`R{round}`).
  - Nombre del participante que tiene el turno activo, su barra/valor de salud (`{hp}/{maxHp}`) y estado de CA.
  - Botón de 1 toque `▶` para avanzar turno inmediatamente sin abrir menús.
  - Indicador de expansión/colapso para ver la cola completa.
- **Vista Desplegada (Panel de Mando Táctico):**
  - Cabecera con título del encuentro, contador de asalto, botones de *"Copiar Asalto al Diario"*, *"Finalizar Encuentro"* y botón de colapsar.
  - Lista scrollable completa de la cola de iniciativa unificada (Criaturas y Jugadores/PNJs) en orden descendente.
  - Destacado visual con borde dorado del combatiente cuyo turno está en curso.
  - Controles express de daño y curación rápida por participante: `-1`, `-5`, `+5` HP con limitación automática (mínimo 0, máximo HP total).
  - Menú y selector de chips para alternar condiciones SRD 5e (Cegado, Asustado, Envenenado, Inconsciente, etc.) con actualización instantánea.
  - Detección automática de caída a 0 HP marcando al objetivo visualmente como derrotado/inconsciente.

**FEATURE — Extensiones de Modelo y Diario Narrativo**
- Propiedad de extensión unificada `Condition.label` en `core:model/Encounter.kt` para traducciones limpias y consistentes de condiciones de estado en toda la aplicación.
- Método `copyCombatRoundToDiary()` en `SessionViewModel`: vuelca instantáneamente el estado completo de la ronda en Markdown estructurado dentro de las notas de la sesión (`dmNotes`), facilitando la crónica de juego en solitario.

**VALIDACIÓN Y PARIDAD**
- 4 pruebas unitarias en `SessionCombatHudTest` verificando la segregación de participantes unificados, ciclado de turnos e incremento de ronda al dar la vuelta, cálculo y acotación de daño/curación de HP, y formato de resumen Markdown.
- Suite de pruebas de `:feature:draw:testDebugUnitTest` (187 tareas) y compilación `:app:assembleDebug` aprobadas al 100%.
- *Feature Parity Mandate:* Registrado para sincronizar en `TDAPP-desktop`: componente Compose `SessionCombatFloatingHud`, propiedad `isCombatHudExpanded` en `SessionUiState`, y métodos de ViewModel para avance rápido y exportación de ronda.

---

### Sprint 62 — Mesa de Juego: Threats & Threads Tracker Flotante (Hilos de Trama y Lista de PNJs) (15 de octubre de 2026)

**FEATURE — HUD Flotante Interactivo de Hilos y Amenazas (`SessionThreadsOverlay`)**
- Pastilla colapsable con estética glassmorphism (`Color(0xEB161920)`) anclada en la esquina superior izquierda (`Alignment.TopStart`, debajo del app bar: `padding(top = 10.dp, start = 12.dp)`), en perfecto balance simétrico con los Relojes de Tensión (`Alignment.TopEnd`) y el Action Hotbar (`Alignment.BottomCenter`).
- Pastilla cerrada: icono `Bookmark`, contador dinámico de hilos activos (`threads.count { !it.isResolved }`) e indicador visual de apertura/cierre.
- Panel flotante desplegable (`AnimatedVisibility` con deslizamiento vertical suave):
  - Barra superior con título temático, botón `+` para creación rápida y botón de cierre.
  - Selector de pestañas segmentado:
    - 🧵 **Hilos (Threads):** Lista de metas, misiones y misterios abiertos con checkbox interactivo para marcar como resuelto (tachado de texto y feedback háptico).
    - 👤 **PNJs (Characters):** Lista de personajes, amenazas y criaturas clave con notas contextuales y estado.
  - Menú contextual desplegable (tres puntos) en cada elemento: *Editar*, *Enviar a Notas* y *Eliminar*.
  - Estado vacío descriptivo con botón de creación directa para el primer elemento.

**FEATURE — Oráculo Sorteador Mythic d100 Integrado**
- Botón *"🎲 Sortear (d100)"*: Realiza una tirada aleatoria d100 y selecciona uniformemente un hilo o personaje activo de la lista según las reglas canónicas de Mythic GME 2e para eventos inesperados.
- Banner de resultado destacado (`lastRandomMythicPick`) con el elemento sorteado, botón directo *"📋 Copiar al Diario"* y botón de descarte.

**FEATURE — Resumen y Exportación al Diario de Campaña**
- Botón *"📋 Registrar Estado en Diario"*: Inserta automáticamente un bloque estructurado en Markdown en el diario de la sesión (`dmNotes`):
  - Hilos activos en formato checklist (`- [ ] Hilo`).
  - Hilos resueltos (`- [x] Hilo`).
  - Lista de personajes y amenazas con sus notas descriptivas (`- **Nombre** — *Notas*`).

**FEATURE — Auto-Vinculación Transparente de Campaña Mythic**
- Si la sesión de juego no tiene aún una campaña Mythic asociada (`linkedMythicSessionId == null`), el `SessionViewModel` crea y vincula automáticamente una sesión Mythic en segundo plano (`ensureLinkedMythicSessionId()`) al registrar el primer hilo o personaje, garantizando fluidez absoluta sin pantallas de configuración intermedias.

**VALIDACIÓN Y PARIDAD**
- 5 pruebas unitarias en `SessionThreadsTest` validando filtrado de hilos activos, alternancia de estado resuelto (`isResolved`), sorteo aleatorio d100 de hilos y personajes, y formato de resumen Markdown para el diario; 100% aprobadas.
- Compilación y suite de pruebas en `:feature:draw:testDebugUnitTest` aprobadas.
- *Feature Parity Mandate:* Registrado para sincronizar en `TDAPP-desktop`: componentes Compose de `SessionThreadsOverlay`, `ThreadEditDialog`, `MythicCharacterEditDialog`, métodos de ViewModel para oráculo d100 y auto-vinculación de Mythic.

---

### Sprint 61 — Mesa de Juego: Relojes de Tensión y Progreso Flotantes (Progress Clocks Overlay) (15 de octubre de 2026)

**FEATURE — Overlay Flotante Interactivo de Relojes (`SessionClocksOverlay`)**
- Pastilla flotante compacta y colapsable en la esquina superior derecha (`Alignment.TopEnd`, debajo del app bar) con estética glassmorphism (`Color(0xEB161920)`), reborde sutil y sombra de elevación.
- Pastilla cerrada: icono `Schedule`, insignia numérica de relojes activos e indicador de alerta si algún reloj está en estado crítico o completo.
- Panel flotante desplegable (`AnimatedVisibility` suave):
  - Cabecera con título, botón `+` para añadir nuevo reloj y botón para contraer.
  - Lista scrollable de relojes activos en tarjetas temáticas según el color asignado.
  - Reloj interactivo con toque directo para avanzar (+1 tick) con respuesta háptica.
  - Controles de avance rápido: botón `-` (retroceder) y botón `+` (avanzar).
  - Menú contextual desplegable con opciones: Editar, Reiniciar (0/N), Detonar Ahora (100%), Copiar al Diario y Eliminar/Archivar.
  - Estado vacío descriptivo con sugerencias de uso (sigilo, alarmas, veneno, límites temporales).

**FEATURE — Componente Gráfico Circular de Progreso (`CircularClockGraphic`)**
- Renderizado Canvas vectorial que divide el arco circular de forma precisa en 4, 6, 8, 10 o 12 segmentos.
- Animación de color suave (`animateColorAsState`), espaciado angular calibrado y separadores radiales limpios.
- Contador numérico central `progress/segments` con escalado dinámico según el tamaño del componente (44dp, 52dp, 64dp, 76dp).

**FEATURE — Diálogo de Creación y Edición Integral (`ClockEditDialog`)**
- Modal para configurar nombre del reloj o amenaza, selector visual de segmentos discretos (chips de 4, 6, 8, 10, 12), paleta de colores temáticos (Alarma/Peligro `#E53935`, Tensión `#FB8C00`, Sigilo/Veneno `#43A047`, Arcano `#8E24AA`, Tiempo `#1E88E5`, Éter `#00ACC1`), slider de progreso inicial y caja multilínea para redactar la consecuencia al detonar.
- Vista previa en tiempo real del reloj mientras se editan sus propiedades.

**FEATURE — Alerta Dramática de Consecuencia e Impacto (`ClockImpactDialog`)**
- Detonación automática cuando el reloj alcanza el 100% (`progress >= segments`).
- Modal de alta visibilidad con pulso visual en tonos de error/alarma, descripción de la consecuencia del detonante y opciones inmediatas:
  - *"Copiar al Diario"*: Inserta una entrada formateada en Markdown en el diario de la sesión (`dmNotes`) con encabezado formal, bloque de consecuencia y progreso alcanzado.
  - *"Reiniciar (0/N)"*: Devuelve el reloj a 0 para amenazas cíclicas o recurrentes.
  - *"Archivar / Eliminar"*: Remueve el reloj de la lista de amenazas activas.

**DATA & PERSISTENCE — Migración Room v45 $\to$ v46 y Respaldo Global**
- **Room v46:** Modificado `SessionEntity` agregando `@ColumnInfo(defaultValue = "[]") val tensionClocksJson: String = "[]"`.
- Creada `MIGRATION_45_46` en `Migrations.kt` con validación `PRAGMA table_info` y registrada en `MigrationRegistry.kt` y `DeckAppDatabase.kt`.
- `SessionDao`: Agregada consulta `@Query("UPDATE sessions SET tensionClocksJson = :clocksJson WHERE id = :sessionId") suspend fun updateTensionClocks(sessionId: Long, clocksJson: String)`.
- `SessionRepository` y `SessionRepositoryImpl`: Método `suspend fun updateTensionClocks(sessionId: Long, clocks: List<TensionClock>)`.
- `Mappers.kt`: Mapeo bidireccional entre `SessionEntity.tensionClocksJson` y `Session.tensionClocks`.
- `BackupDto.kt` y `BackupRepositoryImpl`: Campo `val tensionClocksJson: String = "[]"` añadido a `SessionBackupDto` con persistencia total en el sistema de copia y restauración de seguridad de DeckApp.

**VALIDACIÓN Y PARIDAD**
- 6 pruebas unitarias en `SessionClocksTest` validando serialización/deserialización Room y Backup, avance y sujeción de progreso (`coerceIn`), restablecimiento, detonación instantánea, y parseo y alternancia de hitos en formato Markdown checklist (`- [ ]`); 100% aprobadas.
- Compilación y suite completa de pruebas en `:feature:draw:testDebugUnitTest` y ensamble del binario global `:app:assembleDebug` 100% exitosos.
- *Feature Parity Mandate:* Registrado para sincronizar en `TDAPP-desktop`: componentes Compose de `SessionClocksOverlay`, `CircularClockGraphic`, `ClockEditDialog`, `ClockImpactDialog`, migración Room v46 y respaldo JSON.

---

### Sprint 60 — Mesa de Juego: Asistente de Encuadre de Escenas Solo RPG (Scene Framing Assistant) (15 de octubre de 2026)

**FEATURE — Asistente de Encuadre Guiado de Escenas (`SceneFramingDialog`)**
- Diálogo modal con estética glassmorphism oscuro (`Color(0xFA161920)`) y asistente secuencial en 3 pasos (+ paso previo de transición inteligente).
- Botón de acceso directo destacado en la barra de herramientas superior de la mesa (`SessionScreen`) y botón de encuadre en el encabezado y estado vacío del planificador de aventuras (`PlanTab`).

**FEATURE — Cierre Inteligente de Escena Previa (Paso 0)**
- Detección automática de escenas activas sin completar al iniciar el encuadre.
- Permite cerrar y resumir la escena previa en 1 toque con su ajuste de balance de Caos (`-1` Control, `0` Neutro, `+1` Descontrol) mediante `CloseSoloSceneUseCase`, o continuar manteniendo ambas abiertas.

**FEATURE — Intención, Expectativa y Chequeo de Caos Mythic (Pasos 1 y 2)**
- Paso 1: Definición de título, lugar, objetivo y expectativa base del personaje.
- Paso 2: Chequeo de escena Mythic (d10 contra el Factor de Caos actual de 1 a 9) con animación de tirada y evaluación instantánea:
  - 🟢 **NORMAL (d10 > Caos):** La escena arranca tal como se esperaba sin sobresaltos.
  - 🟡 **ALTERADA (d10 $\le$ Caos y par):** Giro narrativo contextual automático (urgencia, cambio climático/terreno, presencia inesperada de PNJ u obstáculo imprevisto).
  - 🔴 **INTERRUMPIDA (d10 $\le$ Caos e impar):** La escena esperada no ocurre. Generación automática de un Evento Aleatorio completo:
    - *Foco del Evento:* (ej. Introducir un nuevo PNJ, Moverse hacia un hilo, etc.).
    - *Significado:* Dos palabras clave de Mythic GME 2e (Acción + Tema).

**FEATURE — Situación Adaptada, Vínculos y Registro Automático en Diario (Paso 3)**
- Adaptación en tiempo real del texto de la situación inicial según el resultado del chequeo.
- Vinculación opcional con 1 toque de recursos de la sesión (PNJs clave, Tablas aleatorias o Mazos temáticos).
- Inserción automática de entrada formateada en Markdown en el Diario de la Sesión (`dmNotes`):
  - Encabezado formal `## 🎬 Escena N: {Título}`.
  - Cita destacada con el resultado del chequeo de Caos y el evento/giro.
  - Bloques de Lugar, Objetivo y Situación Inicial.
- Enfoque inmediato de la nueva escena en el Bento Board y tablero de juego (`focusScene`).

**VALIDACIÓN Y PARIDAD**
- 5 pruebas unitarias en `SceneFramingAssistantTest` validando chequeos normales, alterados e interrumpidos, generación de foco/significado y generación de Markdown para diario; 100% aprobadas.
- Compilación y suite completa de pruebas en `:feature:draw:testDebugUnitTest` aprobadas.
- *Feature Parity Mandate:* Registrado para sincronizar en `TDAPP-desktop`: componentes Compose de `SceneFramingDialog`, lógica de evaluación de chequeo de escena y formateador de diario.

---

### Sprint 59 — Mesa de Juego: Action Hotbar & Quick Roll Dock (Barra Rápida de Acciones y Macros) (15 de octubre de 2026)

**FEATURE — Action Hotbar & Quick Roll Dock en Mesa de Juego (`SessionHotbarDock`)**
- Dock flotante horizontal colapsable en el borde inferior de la mesa de juego (`SessionScreen`), diseñado con pastilla sutil glassmorphism (`Color(0xEB161920)`), reborde de 1dp `Color(0x33FFFFFF)` y micro-animación de despliegue/ocultamiento (`AnimatedVisibility`).
- Pastilla de control siempre visible con flecha rotatoria para colapsar y expandir sin obstruir notas ni mapas.
- Fila de chips de macros con soporte para pulsación simple (ejecución inmediata con feedback háptico) y pulsación prolongada (menú de edición o eliminación).

**FEATURE — Tipos de Atajos Soportados (`HotbarActionType`)**
1. 🎲 **Tiradas de Dados personalizadas:** Fórmulas arbitrarias (`1d20`, `1d100`, `1d20+5`, `2d6+3`, etc.) ejecutadas en el evaluador de dados y notificadas en snackbar/historial de tiradas de la sesión.
2. 🔮 **Consultas directas al Oráculo Mythic:** `50/50`, `Casi Seguro (+40)`, `Improbable (-20)` y `Evento Aleatorio` (Foco + Significado de 2 palabras) integrados de forma nativa en la sesión y diario.
3. 📜 **Tirada de Tabla Aleatoria favorita:** Ejecución directa con 1 toque de tablas de botín, encuentros o rumores enlazadas por ID.
4. 🃏 **Robo de Carta:** Extracción rápida de una carta de un mazo seleccionado con animación de descarte/mano.

**FEATURE — Modal de Creación y Edición de Macros (`HotbarActionDialog`)**
- Diálogo modal con 4 pestañas de tipo (Dado, Oráculo, Tabla, Mazo).
- Selector de fórmulas rápidas para dados (`d4`, `d6`, `d8`, `d10`, `d12`, `d20`, `d100`).
- Paleta de 8 colores vibrantes de acento para rápida identificación visual.
- Acciones de Guardar y Eliminar macro.

**FEATURE — Starter Pack y Persistencia Room v44 $\to$ v45 (`MIGRATION_44_45`)**
- Precarga automática de 4 macros por defecto en nuevas sesiones (`defaultHotbarActions()`): `d20`, `d100`, `Oráculo 50/50`, `Evento Aleatorio`.
- Persistencia en `SessionEntity` mediante columna `hotbarActionsJson TEXT NOT NULL DEFAULT '[]'` con migración Room v45 (`MIGRATION_44_45`).
- Respaldado en la copia de seguridad global `FullBackupDto` (`SessionBackupDto.hotbarActionsJson`).

**VALIDACIÓN Y PARIDAD**
- Pruebas unitarias en `SessionHotbarTest` verificando serialización JSON, starter pack por defecto y mutaciones de lista.
- Compilación completa y unit tests de `:feature:draw` aprobados al 100%.
- *Feature Parity Mandate:* Registrado para sincronizar en `TDAPP-desktop`: migración Room v45 (`hotbarActionsJson`), componentes Compose/Desktop de `SessionHotbarDock` y `HotbarActionDialog`, lógica de dispatch de macros en `SessionViewModel`.

---

### Sprint 58 — Mapa Táctico: Mapas Anidados (Map-in-Map), Mini-Panel Flotante, Turn Tracker con Auto-Foco y Spawneo Masivo (15 de octubre de 2026)

**FEATURE — Mapas Anidados (Map-in-Map) y Breadcrumbs Jerárquicos**
- Soporte para sub-mapas tácticos enlazados a pines (`targetMapId` en `TacticalMapPin` y `TacticalMapPinEntity`) con migración Room v43 -> v44 (`MIGRATION_43_44`).
- Barra de migas de pan interactiva superior (`TacticalBreadcrumbBar`): visualiza la jerarquía de exploración (ej. *Mundo > Región > Mazmorra*) con botón para retroceder al mapa superior y badge del mapa actual.
- Selector de mapa de destino en `TacticalPinDialog` con nuevos iconos dedicados para transiciones de mapa (`stairs` y `portal`).
- Navegación bidireccional suave: al hacer click en *"Entrar al mapa"* en un pin enlazado o en una miga de pan superior, la vista conmuta al sub-mapa preservando el historial de navegación en la sesión activa.

**FEATURE — Mini-Panel Flotante In-Situ en Pines (Estilo Atlas VTT)**
- Inspector flotante no invasivo (`TacticalFloatingPinPanel`) con ancho compacto (~340dp) y fondo glassmorphism oscuro posicionado sobre el canvas táctico.
- Renderizado de notas del pin con soporte Markdown y detección de fórmulas de dados en línea (ej. `2d6+3`, `1d20+5`) convertidas a chips de tirada interactiva con un toque.
- Barra de acciones rápidas contextuadas:
  - `[Entrar al Mapa]`: Transición inmediata al sub-mapa táctico enlazado.
  - `[Desplegar Encuentro]`: Spawneo masivo de las criaturas del encuentro en celdas adyacentes no bloqueadas.
  - `[Tirar Tabla]`: Ejecución directa de la tabla aleatoria vinculada.
  - `[Alternar Secreto/Público]`: Modificación rápida de la visibilidad para ocultar o revelar notas y descubrimientos del DM.
  - `[Editar]`: Apertura del diálogo completo para modificar color, icono y metadatos.

**FEATURE — Turn Tracker & Iniciativa en Canvas con Auto-Foco de Cámara**
- Barra de iniciativa táctica superior (`TacticalInitiativeBar`): expone el número de ronda actual, avatares de las criaturas en orden de iniciativa con barras de vida, badges de daño/vida crítica y controles de turno siguiente (`SkipNext`) y anterior (`SkipPrevious`).
- Centrado y auto-foco de cámara fluido: al avanzar o retroceder el turno, o al pulsar directamente sobre el avatar de una criatura en la barra de iniciativa, la cámara del mapa táctico se desplaza automáticamente para enfocar y centrar la celda del token activo (`focusCameraOnTokenId`).

**FEATURE — Spawneo Masivo de Encuentros (Cluster Placement)**
- Algoritmo de colocación en racimo (`spawnEncounterOnMap`): genera y sitúa a todas las criaturas del encuentro en celdas transitables libres alrededor del pin o celda de origen, evitando automáticamente celdas con terreno bloqueante (`WALL`) y tokens preexistentes.
- Vinculación automática al combate activo y refresco en tiempo real del mapa táctico.

**FEATURE — Control de Versiones, Resguardo ante Desastres y Coexistencia de APKs (Estable vs Dev)**
- Coexistencia de variantes en Android: `buildTypes.debug` configurado con `applicationIdSuffix = ".debug"`, `versionNameSuffix = "-DEV"` y nombre visible "DeckApp (Dev)" mediante `app/src/debug/res/values/strings.xml`. Permite instalar y ejecutar ambas apps simultáneamente en el mismo dispositivo sin pisar la base de datos de producción.
- Respaldo local offline inmutable: Integración de `git bundle` en el script automatizado `scripts/checkpoint.ps1` con directorio `.backups/` excluido de git. Genera snapshots binarios del 100% del árbol de Git con verificación criptográfica para recuperación instantánea ante accidentes en GitHub.
- Bóveda de APKs históricas (`.backups/apks/`): Opción `-BuildApk` en `checkpoint.ps1` que compila y archiva ejecutables fechados junto con el catálogo automatizado `CATALOG.md`.
- Instalador híbrido inteligente (`scripts/install-version.ps1`): Detecta dispositivos Android por ADB e instala el APK seleccionado en 1 clic manteniendo partidas (`adb install -r -d`), o abre la carpeta en Windows Explorer si no hay dispositivo conectado.
- Selector interactivo de versiones (`scripts/switch-version.ps1`) y restaurador ante desastres (`scripts/restore-from-backup.ps1`).

**FEATURE — Respaldo Total de Campañas y Mapas Tácticos**
- Motor de backup de la app (`FullBackupDto` y `BackupRepositoryImpl`) ampliado para respaldar e importar de forma íntegra mapas tácticos (`TacticalMapBackupDto`), fichas con elevación y condiciones (`TacticalTokenBackupDto`) y pines con sub-mapas enlazados (`TacticalMapPinBackupDto`).
- Compatibilidad retroactiva garantizada con valores por defecto `= emptyList()` para backups de versiones previas.
- Conteo de mapas en el manifiesto (`BackupCounts.tacticalMaps`) y sincronización con Room DB v44 en `CreateBackupUseCase`.

**VALIDACIÓN Y PARIDAD**
- 6 pruebas unitarias de mapas tácticos en `TacticalAtlasVttFeaturesTest` y 2 pruebas de serialización/compatibilidad en `TacticalMapBackupDtoTest`; todas aprobadas.
- Compilación y suite de pruebas de `:core:data:testDebugUnitTest` y `:feature:draw:testDebugUnitTest` aprobadas al 100%.
- Ensamblado total verificado con `./gradlew :app:assembleDebug` (BUILD SUCCESSFUL en 1m 27s).
- *Feature Parity Mandate:* Registrado para sincronizar en `TDAPP-desktop`: migración Room v44 (`targetMapId`), barra de breadcrumbs de mapa táctico, mini-panel flotante in-situ, algoritmo de cluster placement para encuentros, auto-foco de cámara con el tracker de iniciativa, y exportación/importación de entidades tácticas en `FullBackupDto`.

---

### Sprint 57 — Mapa Táctico: Condiciones, Elevación 3D y Ventaja Inteligente (15 de octubre de 2026)

**FEATURE — Estados y Condiciones en Tokens**
- Micro-badges circulares de condiciones en el arco superior del token en el mapa táctico (`TacticalMapCanvas`), con simbología semántica (⬇ Prone, V Poisoned, C Blinded, 💀 Unconscious, ❄ Stunned, ⚓ Restrained, 🕯 Invisible, etc.). Soporta visualización de hasta 3 badges directos y badge de desbordamiento `+N`.
- Overlay táctico de incapacitación: cruz roja semitransparente con reborde oscuro en el centro del token cuando sus puntos de golpe caen a 0 (`currentHp <= 0`).

**FEATURE — Elevación y Vuelo 3D**
- Campo `elevationFt` integrado en `TacticalToken` y `TacticalTokenEntity` con migración Room 42 -> 43 (`MIGRATION_42_43`).
- Renderizado táctico de altura:
  - Sombra de vuelo elíptica proyectada en el suelo debajo del token, escalada y desenfocada según la altura (`token.elevationFt > 0`).
  - Halo de vuelo cian punteado alrededor del token.
  - Badge flotante en la esquina superior derecha del token con la altura actual (ej. `+10ft`, `+30ft`).
- Control de elevación en `TacticalTokenHud`: selector rápido con accesos directos para *Tierra (0ft)*, `+5ft`, `+10ft`, `+20ft` y botones de microajuste incremental.

**FEATURE — Distancia Euclidiana 3D y Ventaja/Desventaja Inteligente**
- Cálculo de distancia 3D en `CalculateTacticalDistanceUseCase`: hipotenusa euclidiana tridimensional $\sqrt{D_{horiz}^2 + \Delta h^2}$ considerando la diferencia de altura vertical $\Delta h$ entre atacante y defensor.
- Línea de puntería en canvas con etiqueta de distancia 3D y desglose de delta de altura (ej. `36 ft (Δh: 20ft)`).
- Diálogo de ataque táctico (`TacticalAttackDialog`) con sugerencia contextual automática de Ventaja/Desventaja según las condiciones del atacante y defensor (blanco derribado cuerpo a cuerpo vs a distancia, ceguera, invisibilidad, parálisis, etc.).
- Selector de modo de ataque (`Normal`, `Ventaja`, `Desventaja`) que realiza tirada doble de d20 tomando el valor máximo o mínimo según corresponda.
- Banner de resolución (`TacticalAttackResolutionBanner`) enriquecido con etiqueta de ventaja/desventaja y diferencial de elevación 3D.

**VALIDACIÓN Y PARIDAD**
- 5 pruebas unitarias creadas en `TacticalElevationConditionsTest` cubriendo cálculo de distancia 3D euclidiana, distancia en plano, modelo de elevación, y resolución de ataques con ventaja y desventaja; todas aprobadas.
- Compilación y empaquetado total verificado con `./gradlew :app:assembleDebug` (BUILD SUCCESSFUL en 1m 3s).
- *Feature Parity Mandate:* Registrado para sincronizar el cálculo 3D, elevación y badges de condiciones en `TDAPP-desktop`.

---

### Solo roleo — Contexto de escena, preguntas y cierre (15 de octubre de 2026)

**FEATURE — Cabecera de escena en distribución Solo**
- Crear, elegir y editar escenas desde la mesa, con título, lugar, objetivo, situación y pregunta pendiente. La situación se despliega para conservar espacio de lectura.
- Reutiliza las escenas persistentes del planificador. El contexto se guarda como Markdown legible en `Scene.content`; las escenas de texto libre conservan todo su contenido como situación al editarlas.
- Permite preguntar al oráculo desde la cabecera y guardar la consulta completa en el diario de la sesión. Disponible también en una mesa sin bloques.

**FEATURE — Consultas con contexto**
- Campo de pregunta en el bloque Mythic; cada resultado captura pregunta, escena, probabilidad, caos y tirada. El registro Mythic vinculado recibe la pregunta real.
- El envío al diario desde cabecera, bloque, historial y herramientas del editor usa los valores de la tirada original, sin reinterpretarlos con los controles actuales.
- La plantilla del editor sin una consulta previa muestra una respuesta pendiente, evitando insertar un «sí» inventado.

**FEATURE — Cierre y continuidad**
- Cierre con resumen obligatorio, consecuencias y pendientes opcionales, creación opcional de la siguiente escena y ajuste opcional de caos (−1 / igual / +1).
- Guardado del cierre en `Session.dmNotes`, marcado de la escena como completada y creación de su sucesora en una transacción Room. Una escena ya completada rechaza un segundo cierre.
- La siguiente escena recibe los pendientes como pregunta inicial. Los campos existentes y vínculos del planificador se conservan; no se modifica el esquema de la base.
- Serializadas las escrituras del diario durante el cierre para proteger cambios recientes de notas y evitar que una escritura anterior reemplace el cierre.

**VALIDACIÓN**
- 7 pruebas nuevas de contexto multilinea, conservación de escenas libres, captura de la consulta, cierre con/sin sucesora, validaciones y propagación de errores; todas aprobadas.
- 30 pruebas de `:feature:draw:testDebugUnitTest` aprobadas y empaquetado debug correcto.
- Cabecera con altura máxima del 40 % del área disponible y scroll propio para conservar espacio de escritura con teclado u orientación horizontal.
- Verificación visual pendiente: no hay dispositivos ni emuladores conectados.

---

### Mesa de juego — Distribuciones DM / Solo y ancho adaptable (15 de octubre de 2026)

**FEATURE — Protagonista y herramientas ancladas**
- Selector de distribución «Grilla / DM / Solo» por sesión y vista, con selección manual de bloque protagonista. Solo prioriza notas/diario; DM prioriza combate, lugar o notas según los bloques existentes.
- El protagonista se presenta con ancho L al comienzo del área principal. Las herramientas usan una columna lateral de 320 dp cuando el espacio disponible alcanza 900 dp, con scroll independiente; en pantallas más angostas se muestran accesos en una franja inferior que abren el bloque completo.
- Los bloques fijados personalizan las herramientas; si no hay fijados se sugieren hasta tres herramientas según el modo. Cada bloque aparece en una única zona de la mesa.
- Modo edición y carpetas conservan la grilla y sus operaciones de organización. Las preferencias de distribución se mantienen mediante estado guardable de Compose, sin cambios de base de datos ni persistencia garantizada entre aperturas de la app.

**FIX — Columnas según el área real**
- Sustituido el cálculo basado en orientación y ancho total de pantalla por el ancho medido del contenedor, después de descontar paneles laterales y la columna de herramientas.
- Entre una y cuatro columnas, con ancho mínimo objetivo de 240 dp y separación de 10 dp; bloques M ocupan hasta dos columnas, L todo el ancho de su zona.
- Al pasar de franja inferior a columna lateral o entrar en edición, se cierra la herramienta abierta para evitar vistas duplicadas.

**VALIDACIÓN**
- `:feature:draw:testDebugUnitTest :app:assembleDebug --max-workers=1`: BUILD SUCCESSFUL.
- 30 pruebas aprobadas, incluidas 7 nuevas sobre ancho útil, prioridades DM/Solo, selección manual, borrado del protagonista, herramientas fijadas y vistas vacías.
- Revisión visual pendiente: no hay dispositivos ni emuladores conectados.

---

### Sprint 56 — Bento Solo Roleplay: Oráculo Mythic Interactivo y Journaling Rápido (15 de octubre de 2026)

**FEATURE — Bento Mythic Todo-en-Uno (Interactividad Completa)**
- Transformación completa de `MythicBlockContent` en un widget interactivo autónomo de cero fricción sin obligar a cambiar de pantalla o abrir el panel lateral:
  - **Selector de Probabilidades:** Menú contextual desplegable con las 9 probabilidades de Mythic GME 2e (desde *Imposible* hasta *Seguro*).
  - **Factor de Caos Dinámico:** Botones rápidos `+` y `-` para ajustar el factor de caos (rango 1..9) con feedback táctil.
  - **Fate Check d100 en 1 Tap:** Botón principal «Preguntar» que calcula instantáneamente el resultado según la tabla de destino de Mythic (SÍ EXCEPCIONAL, SÍ, NO, NO EXCEPCIONAL), comparando la tirada contra el umbral numérico objetivo.
  - **Detección Automática de Eventos Aleatorios:** Si los dígitos coinciden y son menores o iguales al factor de caos, genera un evento de dos palabras («Acción · Sujeto») de las listas de Mythic.
  - **Generador de Chispas / Eventos:** Botón con icono de rayo (`Icons.Default.Bolt`) para inspirar giros narrativos instantáneos con un solo toque.
  - **Historial Desplegable y Enviar a Notas:** Tarjeta de resultado coloreada semánticamente con botón directo para estampar la respuesta en las notas del DM / Journaling.
  - **Modo Standalone con Auto-Sincronización:** Funciona inmediatamente con valores predeterminados (Caos 5, 50/50) si no hay sesión Mythic vinculada, y sincroniza automáticamente las tiradas con el log oficial de la sesión cuando existe vinculación.

**FEATURE — Bento de Notas: Herramientas de Solo Journaling**
- Barra de herramientas rápidas `[🔮 Oráculo]`, `[🎲 Dado]`, `[⚡ Chispa]` y `[⏰ Hora]` integradas tanto en modo edición (inyección en la posición exacta del cursor) como en modo lectura (chips flotantes inferiores de inserción al final con un solo toque).
- Inserción estructurada con formato Markdown optimizado para lectura y relectura de aventuras en solitario.
- Función de soporte `appendToNotes` en `SessionViewModel` para estampar tiradas y chispas desde cualquier bento sin sobrescribir el texto existente.

**VALIDACIÓN Y PARIDAD**
- Suite unitaria `SessionBoardSoloJournalTest` creada y validada en `:feature:draw:testDebugUnitTest` (BUILD SUCCESSFUL).
- Compilación y empaquetado total verificado con `./gradlew :app:assembleDebug` (BUILD SUCCESSFUL).
- *Feature Parity Mandate:* Registrado para mantener paridad de experiencia en el bento grid de `TDAPP-desktop`.

---

### Bento de tablas — Recuperación de resultados y búsqueda (15 de octubre de 2026)

**FEATURE — Fijar resultados durante la sesión**
- El menú de cada resultado permite fijarlo, también al consultar una tirada anterior. El bloque conserva ese resultado al realizar nuevas tiradas, con acceso a la nueva tirada y opción de volver al resultado más reciente.
- Estado compartido por tabla en el ViewModel de sesión; se conserva al cambiar de pestaña o tamaño mientras vive el ViewModel. No persiste tras cerrar la sesión o reiniciar la app.
- Conectado el estado existente `pinnedTableRolls` y sus callbacks a través de todos los componentes del tablero. Los resúmenes de bloques colapsados reflejan el resultado fijado.

**FEATURE — Consulta y feedback**
- Búsqueda de entradas por varias palabras, ignorando mayúsculas y tildes; contador de coincidencias, limpieza de búsqueda y estado vacío. Se conservan rangos, pesos y dados interactivos.
- Confirmación temporal con icono de verificación al copiar un resultado.

**FIX — Actualización de recursos y compilación**
- Cambiar la tabla vinculada invalida las vistas memorizadas de fórmula, resultado e historial, y cierra las consultas de la tabla anterior.
- Reparaciones mínimas de compilación en el bloque Mythic existente: imports al comienzo del archivo, lectura segura del texto opcional y uso de `saveRoll(MythicRoll)` según la interfaz real del repositorio.

**VALIDACIÓN**
- `:feature:draw:testDebugUnitTest :app:assembleDebug --max-workers=1`: BUILD SUCCESSFUL.
- Ejecución independiente de `TableEntriesSearchTest`: 4 pruebas, 0 fallos, 0 errores. Cobertura de tildes/mayúsculas, varias palabras, búsqueda vacía y fórmulas literales.
- Verificación visual pendiente: ningún dispositivo ni emulador conectado.

---

### Bento de tablas — Lectura y acciones por tamaño (15 de octubre de 2026)

**FEATURE — Resultados legibles durante la sesión**
- S muestra un resumen de dos líneas y tirada rápida; M incluye historial plegado; L ofrece lectura con scroll, hasta cinco tiradas anteriores y consulta de las entradas de la tabla con rangos o pesos según el modo.
- Área de resultado de altura estable, transición entre tiradas y resaltado breve del nuevo resultado. «Ver completo» abre una copia del resultado seleccionado, también desde el historial, con lectura y acciones propias.
- Controles de 48 dp, copia visible y menú contextual para enviar a notas o al combate activo; se conservan los dados interactivos del texto.

**FIX — Datos del bloque conectados**
- La fórmula y el historial ya disponibles en `BoardBlockCard` se transmiten ahora a `TableBlockContent`; antes se perdían en el componente intermedio.
- Se transmite la tabla al bloque para consultar sus entradas sin salir del tablero.

**VALIDACIÓN**
- `./gradlew :app:assembleDebug`: BUILD SUCCESSFUL. Revisión visual en dispositivo pendiente: no había dispositivos ni emuladores conectados.

---

### Sprint 55 — Modo Mesa Híbrida: Panel Lateral Rápido Multitool VTT (15 de octubre de 2026)

**FEATURE — Panel Lateral Rápido Multitool y Adaptabilidad Responsiva**
- Integración del panel multitool `TacticalSidePanel` en el mapa táctico para juego presencial híbrido y gestión integral del DM/Master sin abandonar la pantalla táctica.
- Solapa flotante táctil (`Icons.Default.DashboardCustomize`) en el margen derecho para alternar el panel con un solo toque.
- Comportamiento responsivo inteligente: en pantallas anchas, tablets y orientación horizontal (`maxWidth >= 720.dp`), opera como **pantalla dividida (Side-by-Side)** ajustando el lienzo del mapa táctico; en pantallas móviles verticales opera como **cajón flotante deslizante (Overlay Drawer)** con telón de fondo translúcido.

**FEATURE — 4 Pestañas Multitool Integradas**
- **Notas & Wiki (`NOTES_WIKI`)**: Editor de notas rápidas de sesión con guardado automático y barra de búsqueda en tiempo real que filtra entradas y tarjetas de la Wiki de campaña.
- **Lanzador de Dados (`DICE`)**: Selector interactivo de dados polifacéticos (d4 a d100), contador de dados, modificador numérico rápido (+ / -), evaluación instantánea con `DiceEvaluator` e historial de tiradas recientes con marca de tiempo.
- **Oráculo Mythic GME 2e (`MYTHIC`)**: Fate Check d100 contra tabla de probabilidades (desde *Imposible* hasta *Seguro*), factor de caos ajustable (1..9), respuestas de oráculo contextuales y generador de eventos aleatorios de dos palabras (Acción · Sujeto) basado en las palabras clave oficiales de Mythic.
- **Control de Combate (`COMBAT`)**: Resumen del encuentro activo con combatientes ordenados, barras de salud relativas, CA, selección rápida del token asociado en el mapa y avance directo de rondas y turnos.

**INTEGRATION & TESTING**
- Suite unitaria `TacticalSidePanelTest` implementada en `:feature:draw:testDebugUnitTest`: cobertura de alternancia y estado del panel, cambio entre las 4 pestañas, cálculo y registro de dados, tirada de destino y eventos de Mythic, filtrado de Wiki y avance de turnos de combate.
- Ejecución limpia de `./gradlew :feature:draw:testDebugUnitTest` (13 tests pasando con éxito).
- Verificación completa de compilación y empaquetado del APK debug con `./gradlew :app:assembleDebug` (BUILD SUCCESSFUL).
- *Feature Parity Mandate*: Especificación de arquitectura y componentes preparada para incorporar el panel lateral multitool en `TDAPP-desktop`.

---

### Sprint 54 — Sincronización Táctica Total: Iniciativa, Combate y Ataques (15 de octubre de 2026)

**FEATURE — Tira Flotante de Iniciativa y Halo Luminoso de Turno**
- Tira horizontal flotante `TacticalInitiativeBar` anclada en la parte superior del mapa táctico cuando hay un encuentro activo (`Encounter`): muestra ronda, combatientes ordenados por iniciativa, estado de salud / CA y botón de un toque para avanzar al siguiente turno (`Siguiente Turno`).
- El token en turno activo exhibe en tiempo real un **halo dorado pulsante** en el mapa (`Color(0xFFFFD700)`).
- Al avanzar el turno, se reinicia automáticamente el presupuesto de movimiento (`tokenMovedThisTurnFt`) del combatiente que entra en acción.

**FEATURE — HUD de Token con Acción de Ataque y Puntería Táctica**
- Botón **"Atacar"** (`Icons.Default.AdsClick`) integrado en el HUD flotante del token.
- Al activarse, entra en modo puntería táctica con indicador superior contextual: al tocar un token enemigo en el mapa se dibuja una **línea punteada dinámica de alcance** con badge de distancia en pies y una mira roja sobre el objetivo.

**FEATURE — Resolución Táctica de Ataques y Aplicación de Daño Instantánea**
- Diálogo de ataque táctico `TacticalAttackDialog`: muestra enfrentamiento Atacante vs Defensor, distancia, CA del objetivo, opciones rápidas de armas y campos configurables de bono y dados de daño.
- Banner de resolución `TacticalAttackResolutionBanner`: calcula tirada de d20 + bono vs CA del objetivo, detecta impactos críticos (20) y pifias (1), evalúa dados de daño mediante `DiceEvaluator` y ofrece el botón instantáneo **"Aplicar Daño"** que descuenta automáticamente los PG del token y se sincroniza bidireccionalmente con `EncounterCreature`.

**INTEGRATION & TESTING**
- Suite de pruebas unitarias `TacticalCombatSyncTest` creada con 4 casos de prueba en `:feature:draw:testDebugUnitTest`: resolución de ataque con d20 vs CA, detección de críticos y pifias, aplicación de daño con reducción de PG y sincronización de repositorio, y avance de turno con reinicio de presupuesto de movimiento.
- Todas las pruebas de `:feature:draw:testDebugUnitTest` (7 tests) aprobadas con éxito.
- Empaquetado y compilación verificados con `./gradlew :app:assembleDebug` (BUILD SUCCESSFUL).
- *Feature Parity Mandate*: Especificación lista para trasladar la tira de iniciativa, puntería táctica con línea de alcance y banner de resolución a `TDAPP-desktop`.

---

### Sprint 53 — Restauración y Espacio Dedicado para el Mapa Táctico VTT (15 de octubre de 2026)

**FEATURE — Espacio Dedicado para el Mapa Táctico en la Mesa de Juego**
- Integración en `SessionViewMode.TACTICAL` con botón dedicado en la barra superior de acciones (`Icons.Default.GridOn`), otorgándole su propio espacio de pantalla completa y evitando que el bento del mapa desplace verticalmente o sobrecargue los demás bloques de la mesa.
- Selector multimapa directo en la cabecera del mapa táctico con menú contextual desplegable: cambio rápido de mapa activo, creación de nuevos mapas con calibración de grilla, hoja modal de mapas (`TacticalMapSelectorSheet`), renombrado y eliminación.

**FEATURE — Terreno Dificultoso y Reglas de Movimiento**
- Sub-barra de herramientas para terreno táctico con pinceles para terreno difícil (`DIFFICULT` a +5 ft por celda), muros bloqueantes (`WALL`), zonas de peligro (`HAZARD`) y borrado (`NORMAL`).
- Cálculo reactivo de distancia en tiempo real que integra la rugosidad y coste del terreno tanto en medición libre como al arrastrar tokens y trazar rutas.

**FEATURE — Herramienta de Planificación de Rutas y Presupuesto de Turno**
- Nueva herramienta de ruta paso a paso (`TacticalTool.ROUTE`): trazado de waypoints intermedios al tocar el mapa con indicador visual de trayectoria (nodos numerados, trazos de conexión y badges con distancia acumulada).
- Barra de control de ruta con badge de distancia recorrida, botón para deshacer último waypoint (`Icons.AutoMirrored.Filled.Undo`), botón de confirmación de movimiento con ajuste al centro de celda y cancelación.
- Seguimiento de presupuesto de movimiento por turno (`Mov: X / Y ft`) para tokens, con badge visual y botón de reinicio al iniciar un nuevo turno de combate.

**INTEGRATION & TESTING**
- Suite de pruebas unitarias `TacticalRouteMovementTest` creada y verificada con 3 casos de prueba en `:feature:draw:testDebugUnitTest`: inicialización de ruta al seleccionar token, cálculo reactivo de distancia al añadir/quitar waypoints y confirmación de movimiento con consumo de presupuesto de turno.
- Verificación completa de compilación y empaquetado mediante `./gradlew :app:assembleDebug` (BUILD SUCCESSFUL).
- *Feature Parity Mandate*: Especificación de arquitectura sincronizada para replicar el selector de vista `TACTICAL`, la herramienta `ROUTE` y el cálculo de terreno dificultoso en la versión Desktop (`TDAPP-desktop`).

---

### Sprint 52 — Pérdida de catálogo al actualizar: protección y recuperación (14 de octubre de 2026)

**FIX — Migraciones registradas y actualización no destructiva**
- Identificada la causa: versión de esquema 41 con migraciones registradas solo hasta 37 y `fallbackToDestructiveMigration()` habilitado. Una actualización sin ruta recreaba las tablas y perdía el catálogo.
- Registro único `ALL_MIGRATIONS` incluye las rutas 37→38→39→40→41; eliminado el fallback destructivo. Una versión sin ruta debe fallar conservando la base.
- Corregida la referencia del alias 29→30 y eliminado el borrado incondicional de PNJs en la migración 24→25.
- APK identificado como 0.1.1 (versionCode 2).

**FIX — Restauración y exportación de copias**
- Colecciones y sus referencias incluidas en los DTO, exportación y restauración. Las etiquetas de tablas se insertan después de sus tablas para respetar las claves foráneas.
- La copia de archivos incluye todos los directorios de recursos internos, incluidas portadas y wiki.

**RECOVERY — Material preservado antes de intervenir**
- App detenida sin borrar datos. Snapshot completo de bases, preferencias y archivos internos conservado con SHA-256 en `I:/TTRPG/Visuales/Decks/Deckapp-respaldo-2026-10-14/`.
- Catálogo actual vacío; permanecen 112 imágenes (56 cartas frente/dorso). Preparado un ZIP importable independiente y conservadas las imágenes originales.
- Localizado y copiado `Download/deckapp_backup_20260530.zip`: CRC correcto, 4 mazos, 143 cartas, 299 tablas, 4 PNJs, 3 sesiones, 2 colecciones y otros recursos. Las 112 imágenes actuales corresponden visualmente al mazo NPC Portraits de esa copia.
- La copia antigua carece de dos imágenes referenciadas (una portada de colección y una imagen wiki). Los originales de ambos snapshots se conservan sin modificar.
- Restauración de la copia de mayo pendiente de elegir la fuente de recuperación; no se ha reemplazado el catálogo del usuario.

**INTEGRATION — Validación**
- Preparadas pruebas Android aisladas de migración 37→41 y 40→41, reapertura de base actual, rechazo no destructivo de versión sin ruta, cobertura del registro y restauración de colecciones/etiquetas.
- Compilación y pruebas en curso. Los tests usan un paquete separado y nunca la base de la app del usuario.

---

### Sprint 51 — Revisión en Motorola One Fusion (14 de octubre de 2026)

**FIX — Barras del sistema y ventana de prueba**
- Iconos de las barras de estado y navegación adaptados al tema de la app mediante WindowCompat. En Claro y Pergamino pasan a oscuros; en Oscuro y Pergamino nocturno permanecen claros.
- Ventana de prueba de componentes con descripción más breve y mayor altura de lectura para evitar recortar innecesariamente el botón deshabilitado en el teléfono.

**INTEGRATION — Dispositivo real**
- APK instalado correctamente en Motorola One Fusion con Android 11 y pantalla de 720 × 1600.
- Revisados selector de temas, diálogos Claro/Pergamino, vista previa nocturna, densidad cómoda/compacta y editor de portadas sin guardar datos de prueba.
- `:app:assembleDebug` completado correctamente; APK corregido reinstalado y abierto en el teléfono.
- Verificados visualmente los iconos oscuros sobre Claro/Pergamino y claros sobre Oscuro, y la ventana de ejemplo con todos sus botones visibles. Preferencias de prueba restauradas a Oscuro / Suave / Cómoda.

---

### Sprint 50 — Materiales, emblemas y estilos de campaña (13 de octubre de 2026)

**FEATURE — Identidad de los temas**
- Materiales procedurales cacheados por superficie: grano de carbón en Oscuro, fibras finas en Claro y fibras más largas con matices de envejecido en Pergamino. Se mantiene el control Apagada / Suave / Marcada y la intensidad reducida en áreas de lectura.
- Pergamino nocturno: paleta marrón oscuro, tinta marfil, cobre y tipografía serif. Selector de cuatro temas adaptable al ancho.
- Cabeceras de capítulo con símbolo y separadores propios del tema; iconografía común para tesoro, magia, encuentros, personajes y exploración en tablas y colecciones.
- Acentos Del tema / Bosque / Cobre / Arcano. Estilos de campaña con nombre que guardan y restauran tema, acento, textura y densidad; guardar el mismo nombre actualiza la combinación existente.

**FEATURE — Portadas de colecciones**
- Portada automática temática con marco, emblema y color. Editor con vista previa, selección de seis colores y once emblemas, imagen opcional y opción para volver a la portada automática.
- Fallback de imagen fallida a portada automática; edición accesible sobre fondos claros, oscuros e imágenes.

**FIX — Guardado de colecciones**
- Editar copia los datos existentes para preservar descripción, imagen y fecha de creación. La actualización usa UPDATE, evitando la eliminación en cascada de recursos que podía causar INSERT OR REPLACE.
- Seleccionar una imagen en el editor ya no modifica la colección antes de confirmar.

**INTEGRATION — Validación**
- `:app:assembleDebug` y `:core:data:testDebugUnitTest` completados con éxito. Seis pruebas nuevas de persistencia/regresión aprobadas (cuatro de estilos y dos de colecciones).
- Veinte pares de texto/fondo de Pergamino nocturno y acentos verificados: contraste mínimo 7:1 en colores sólidos base.
- Revisión visual en Android pendiente: sin dispositivo conectado ni AVD configurado.

---

### Sprint 49 — Componentes y lenguaje visual compartidos (13 de octubre de 2026)

**FEATURE — Temas integrados en los controles de la app**
- Paletas Claro y Oscuro completadas con contenedores, colores de texto, superficies inversas, bordes, errores y tintes de elevación; Pergamino incorpora tinte y scrim propios.
- Formas compartidas para campos, botones, tarjetas y diálogos. Pergamino usa esquinas más sobrias; Claro y Oscuro conservan formas redondeadas.
- `DeckCard`, `DeckButton`, `DeckOutlinedButton` y `DeckTextButton` integrados en las pantallas de features y componentes compartidos. Mantienen las interacciones de Material, parámetros explícitos y áreas de acción mínimas de 48 dp; tarjetas con borde fino y elevación discreta según tema.
- Jerarquía tipográfica completa con estilos de lectura comunes y títulos serif en Pergamino.
- Ajustes → Apariencia incorpora una ventana de prueba del tema con tarjeta, campo editable, acción secundaria y estado deshabilitado. El ejemplo no se guarda en las campañas.

**FIX — Consistencia entre temas**
- Tablas y selector de tema usan las formas compartidas. Fondos de papel adoptan los mismos bordes de tarjeta.
- Panel de PNJs de sesión sustituye texto/fondos blancos fijos por colores semánticos para Claro, Oscuro y Pergamino.

**INTEGRATION — Validación**
- Verificados 11 pares de texto/fondo por paleta: contraste mínimo 6.48:1 en Oscuro, 6.43:1 en Claro y 5.49:1 en Pergamino (colores sólidos base).
- `:app:assembleDebug` completado con éxito y verificado nuevamente con los archivos finales; sin errores de formato en los archivos UI modificados. Revisión visual en Android pendiente: sin dispositivo conectado ni AVD configurado.

---

### Sprint 48 — Controles de textura y densidad (13 de octubre de 2026)

**FEATURE — Apariencia configurable de tablas**
- Ajustes → Apariencia incorpora textura Apagada / Suave / Marcada y densidad Cómoda / Compacta, con selección accesible y controles que se acomodan al ancho disponible.
- Vista previa de encuentros con los mismos componentes de cabecera y filas usados en las tablas reales. Responde al tema, textura y densidad actuales.
- Densidad aplicada a filas de consulta y espaciado de tarjetas/listas de tablas; los botones mantienen sus áreas táctiles.

**ARCHITECTURE — Preferencias reactivas**
- `TableAppearance` persiste ambas opciones en SharedPreferences y expone un StateFlow desde SettingsRepository. MainActivity propaga los cambios mediante el tema; valores ausentes o inválidos conservan los valores por defecto Suave / Cómoda.
- La intensidad multiplica la textura según cada superficie, conservando el grano tenue en zonas de lectura. Apagada evita cargar y dibujar la textura.

**INTEGRATION — Validación**
- `:app:assembleDebug` completado con éxito y verificado nuevamente con los archivos finales. Revisión visual en Android pendiente: sin dispositivo conectado ni AVD configurado.

---

### Sprint 47 — Lectura de tablas y refinamiento de Pergamino (13 de octubre de 2026)

**FEATURE — Tablas como páginas de referencia**
- Vista desplegable de entradas en la hoja de tirada: cabecera, rangos monoespaciados, filas alternadas y marca de última tirada para tablas RANGE. WEIGHTED muestra pesos; SEQUENTIAL y MACRO muestran pasos en su orden de ejecución.
- Resultado en panel de lectura con textura tenue, dado destacado y separador de cabecera. Hoja desplazable para consultar entradas, resultados largos e historial en pantallas pequeñas.
- Tarjetas de tablas con papel base, borde fino y acento lateral según etiqueta; estados de selección y tirada conservan colores semánticos. Botones de las tarjetas de biblioteca ampliados a 48 dp y texto de acción adaptado al tema.

**FIX — Consistencia del tema**
- Intensidad de textura reducida por tema y configurable por superficie mediante `textureAlpha` en `PaperCard` / `PaperSurface`.
- Completados los colores de contenedores, superficies inversas y errores de Pergamino; bronce más oscuro para mejorar contraste. Color de texto de respaldo para superficies personalizadas.

**INTEGRATION — Validación**
- `:app:assembleDebug` completado con éxito después de los ajustes finales. Validación visual en Android pendiente: sin dispositivo conectado ni AVD configurado.

---

### Sprint 46 — Tema Pergamino & Textura de Papel para Tablas (13 de octubre de 2026)

**FEATURE — Tema Pergamino y Textura de Fondo TTRPG**
- **Sistema de Temas Tri-Modal (`AppTheme`)**: Soporte en `core:model` para `DARK` (OLED), `LIGHT` y `PARCHMENT` (Pergamino de fantasía medieval).
- **Paleta Pergamino (`DeckParchmentColorScheme`)**: Primario granate/cuero medieval (`#7B1E2B`), acento dorado/latón (`#B8860B`), fondos en papiro/pergamino envejecido (`#F4EEDD` y `#EBE1C8`), y texto en tinta sepia/carbón (`#2B2118`).
- **Tipografía Medieval-Moderna (`DeckParchmentTypography`)**: Títulos y cabeceras en `FontFamily.Serif` evocador de libros de reglas clásicos, combinados con fuentes legibles sin serifas para fórmulas y números de tirada.
- **Textura de Papel Tileable (`bg_paper_texture.webp`)**: Textura ultra-ligera y continua integrada en `core:ui/res/drawable/`.
- **Componentes `PaperCard` y `Modifier.paperBackground()`**: Diseñados para aplicar la textura con modos de fusión adaptables (`BlendMode.Screen` en tema oscuro y `BlendMode.Modulate` en pergamino/claro) sin perder contraste ni legibilidad.
- **Texturización de Tablas**: Aplicado `PaperCard` y fondo texturizado a `TableListItem`, `FeaturedTableCard`, `TableGridItem`, `TableDetailSheet` y al panel de historial de tiradas.
- **Selector de Tema en Ajustes**: Tarjeta interactiva en `SettingsScreen` con vista previa de los 3 temas y persistencia reactiva en `SettingsRepository`.

**FIX — Higiene y Compatibilidad de Compilación**
- **`BackupRepositoryImpl`**: Corregida llamada al constructor de `TableBundleEntity` usando named parameters para solventar incompatibilidad con `imageUrl` y `createdAt`.
- **`LibraryScreen` & `TableGridItem`**: Resueltos imports faltantes de `CircleShape`, `AsyncImage` y orden de launchers de picking de imágenes.

**INTEGRATION — Build & Tests**
- **BUILD SUCCESSFUL** `assembleDebug` y tests unitarios en `:core:domain:test` verificados con éxito (60 tests aprobados).

---

### Sprint 45 — Estabilidad, Higiene Técnica y UX Pro (19 de abril de 2026)

**ARCHITECTURE — Lógica de "Upsert" Inteligente y Estabilidad de Listas**
- **Refactor Sistémico de DAOs**: Eliminado el uso de `OnConflictStrategy.REPLACE` en DAOs críticos (`CardStackDao`, `SessionDao`, `RandomTableDao`, `WikiDao`, `ReferenceDao`). 
- **Preservación de Metadatos**: Implementada lógica de `IGNORE` + `@Update` en los repositorios. Esto garantiza que la fecha de creación (`createdAt`) no se sobrescriba al editar, evitando que los elementos "salten" de posición en las listas ordenadas cronológicamente.
- **Migración de BD**: Bumpeada la base de datos a **Versión 32**. Añadidas columnas `isRadial`, `maxActivitiesPerDay` y `mapNotes` a la tabla `hex_maps`.

**STABILITY — Blindaje del Mapa Hexagonal**
- **Fix de Crash por Zoom**: Corregido error de `maxWidth/maxHeight` negativo al dibujar etiquetas de texto en escalas extremas. Implementada medición de texto manual con `TextLayoutResult` y filtros de coordenadas de seguridad.
- **Optimización de Culling**: Incrementado el margen de renderizado a 3 hexágonos para eliminar parpadeos en los bordes durante el desplazamiento rápido.

**FEATURE — Herramientas Avanzadas de Hexploración**
- **Herramienta "Borrar"**: Añadido pincel de borrado al editor para eliminar hexágonos individuales y crear formas irregulares.
- **Sistema de Coordenadas**: Interruptor visual en el editor para mostrar/ocultar coordenadas axiales en cada hexágono.
- **Indicador de POIs Múltiples**: Nuevo renderizado con borde doble para hexágonos que contienen más de un punto de interés.
- **UX de Creación**: Refactorizado el diálogo de nuevo mapa para soportar entrada de texto fluida (sin bloqueos de borrado) y previsualización de radio/rejilla en tiempo real.

**UX — Editores Adaptables y Gestión de Sesión**
- **Editores "Keyboard-Aware"**: Añadido `imePadding` y soporte de scroll en los editores de PNJs y Encuentros. El teclado ya no tapa los campos inferiores ni los botones de acción.
- **Persistencia de Sesión**: El estado de previsualización de cartas ("Peek") ahora sobrevive a rotaciones de pantalla y reinicios de proceso vía `SavedStateHandle`.
- **Cronómetro Eficiente**: Refactorizado el timer de sesión para ser reactivo y consumir recursos mínimos en segundo plano.

**CLEANUP — Gestión de Almacenamiento "Zero Waste"**
- **Borrado Físico de Imágenes**: Actualizados los repositorios de Mazos, PNJs y Wiki. Ahora, al eliminar un elemento, la aplicación borra automáticamente sus archivos de imagen del almacenamiento interno, evitando la acumulación de datos huérfanos.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — Consolidación total de la infraestructura de datos y UI.

---

### Sprint 44D — Modo Sesión e Integraciones (19 de abril de 2026)

**FEATURE — HexMapSessionScreen: tracking de exploración en vivo**
- **`HexMapSessionScreen`**: canvas en modo SESSION con fog of war activo. Header con día actual y actividades usadas. FAB "+" para nuevo día.
- **`SessionTileSheet`** (`ModalBottomSheet`): chips de estado (Explorado/Reconocido/Mapeado), botones de acción con coste de actividades, notas de jugador visibles post-exploración, POIs con botones "Tirar tabla" y "Encuentro".
- **`StatusChip`**: chip visual con ícono de check cuando el estado está activo.
- **`HexMapSessionViewModel`**: auto-vincula el mapa a la sesión activa al abrir (`LinkHexMapToSessionUseCase`), gestiona día actual con `StartNewHexDayUseCase`, delega acciones a `ExploreHexUseCase`, `ReconnoiterHexUseCase`, `MapHexUseCase`.
- **Integración Sessions**: `SessionRepository.getActiveSession()` inyectado, mapa se vincula automáticamente.
- **Integración Tables**: `RollTableUseCase` invocado desde POI con `tableId`; resultado mostrado en `AlertDialog` con nombre de tabla y texto resuelto.
- **Integración Encounters**: desde POI navega a `EncounterEditorRoute(encounterId)` o a `EncounterListRoute` si no hay encuentro vinculado.
- **Punto de entrada**: "Mapas Hex" agregado al dropdown de `LibraryScreen` (icono `Layers`).
- Fix: smart cast en `poi.tableId`/`poi.encounterId` resuelto con variables locales (propiedad de módulo externo).

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — sprint 44 completo, feature hexploration operativo.

---

### Sprint 44C — Pantallas de Lista y Editor de Hexploración (19 de abril de 2026)

**FEATURE — HexMapListScreen y HexMapEditorScreen funcionales**
- **`HexMapListScreen`**: lista de mapas con `LazyColumn`, `Card` clickable, botón borrar con `AlertDialog` de confirmación, FAB para crear nuevo mapa. Estado vacío con mensaje guía.
- **`CreateHexMapDialog`**: campos de nombre, columnas y filas (3–40) con preview del total de hexes. Creación inmediata navega al editor.
- **`HexMapEditorScreen`**: `HexGridCanvas` en modo DESIGN ocupa el espacio disponible, barra de terreno en la parte inferior, FAB "Iniciar sesión" con navegación a `HexMapSessionRoute`.
- **`TerrainBrushToolbar`**: `LazyRow` con 8 pinceles predefinidos (abierto, difícil, muy difícil, infranqueable, agua, llanura, bosque, montaña). Tap aplica el terreno directamente al hex.
- **`TileDetailSheet`** (`ModalBottomSheet`): long press abre notas DM/jugadores (editables + guardado), lista de POIs del hex con botón borrar, botón agregar POI.
- **`AddPoiDialog`**: nombre, tipo (dropdown con todos los `PoiType`), descripción opcional.
- **`HexMapEditorViewModel`**: `SavedStateHandle["mapId"]` (patrón del proyecto, sin dependencia circular a `:app`). Brush activo aplicado en tap, tile seleccionado en long press.
- Rutas `HexMapListRoute`, `HexMapEditorRoute(mapId)`, `HexMapSessionRoute(mapId)` registradas en `Screen.kt` y `NavGraph.kt`.
- Fix: eliminado `toRoute<HexMapEditorRoute>()` del ViewModel (causaría dependencia circular `:feature → :app`).

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 407 tasks, 16 executed.

---

### Sprint 44B — HexGridCanvas (19 de abril de 2026)

**FEATURE — Canvas de hexágonos con lazy rendering y modos Diseño/Sesión**
- Nuevo módulo `:feature:hexploration` registrado en `settings.gradle.kts` y `app/build.gradle.kts`.
- **`HexGridCanvas`** (`components/HexGridCanvas.kt`): Composable Canvas con coordenadas axiales flat-top.
- **Lazy rendering / viewport culling**: en cada frame se filtran los tiles cuyo centro cae fuera del viewport expandido (+2 hex de margen). Sin cap de tamaño — mapas de cualquier dimensión son viables.
- **Pan + pinch-zoom**: `rememberTransformableState` con zoom entre 0.3× y 4×.
- **Tap / Long press**: `detectTapGestures` convierte coordenadas de pantalla → mundo → axial con `axialRound` para hit-testing preciso.
- **Modo DESIGN**: muestra terreno, indica coste con puntos blancos en tiles difíciles (cost ≥2).
- **Modo SESSION**: fog of war oscuro en tiles no explorados; tint blanco en reconocidos; shimmer dorado en mapeados.
- **POI overlay**: círculo de color según `PoiType` (8 colores semánticos) visible en diseño o cuando el hex está explorado.
- Hex stroke dorado + grosor 3px en tile seleccionado.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 406 tasks, 25 executed.

---

### Sprint 44A — Dominio y Base de Datos de Hexploración (19 de abril de 2026)

**FEATURE — Capa de dominio completa para el feature Hexploración**
- **Modelos** (`core:model`): `HexMap`, `HexTile`, `HexPoi`, `HexDay`, `HexActivityEntry`, `HexActivityType`, `HexStyle`, `PoiType`.
- **Repository interface** (`core:domain`): `HexRepository` con métodos Flow + suspend para mapas, tiles, POIs y días.
- **11 UseCases** en `core:domain/usecase/hex/`: `GetHexMapsUseCase`, `GetHexMapWithTilesUseCase`, `CreateHexMapUseCase` (genera grid de tiles automáticamente), `UpdateHexTileUseCase`, `ExploreHexUseCase`, `ReconnoiterHexUseCase` (costo según `terrainCost`), `MapHexUseCase`, `AddHexPoiUseCase`, `DeleteHexPoiUseCase`, `StartNewHexDayUseCase`, `LogHexActivityUseCase`, `LinkHexMapToSessionUseCase`, `DeleteHexMapUseCase`.
- **Room** (`core:data`): `HexEntities.kt` (4 entidades con FK CASCADE), `HexDao.kt`, `HexMappers.kt` (toDomain/toEntity), `HexRepositoryImpl.kt`.
- **Migración 29→30**: 4 nuevas tablas (`hex_maps`, `hex_tiles`, `hex_pois`, `hex_days`) con índices e integridad referencial.
- **DI**: `HexDao` y `HexRepositoryImpl` registrados en `DataModule.kt`.
- `HexActivityEntry` y `HexActivityType` anotados con `@Serializable` para persistencia JSON en `HexDayEntity.activitiesLog`.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 384 tasks, 71 executed.

---

### Sprint 43 — Resultado de Tirada Inline en Tarjetas de Tabla (18 de abril de 2026)

**FEATURE — Quick Roll con resultado visible sin abrir la tabla**
- Al presionar TIRAR en una tarjeta de tabla, el resultado aparece directamente en la propia tarjeta sin navegar a ningún detalle.
- `TablesUiState` extendido con `quickRollResults: Map<Long, TableRollResult>` — cada tabla mantiene su último resultado independientemente.
- `TablesViewModel.rollTable()` ahora también actualiza `quickRollResults[tableId]`.
- `TablesViewModel.clearQuickRoll(tableId)` elimina el resultado de una tabla específica.
- **`TableGridItem` (biblioteca standalone)**: modo dual con `AnimatedContent (fadeIn/fadeOut)`:
  - *Vista normal*: nombre, descripción, tags y botón TIRAR.
  - *Vista resultado*: fondo secundario, badge con el número de dado, texto resuelto y botón TIRAR DE NUEVO + × para cerrar.
- **`TableListItem` (vista sesión)**: resultado inline expandido debajo del nombre. El dado cambia a icono `Refresh` y el pin se sustituye por `×` mientras hay resultado activo.
- Grid en `TablesTab` refactorizado para usar `TableGridItem` compartido (eliminado el duplicado local más simple).

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — sin errores en feature:tables, feature:draw ni app.

---

### Sprint 42 — Fix BOM en Archivos de Packs (18 de abril de 2026)

**FIX — BOM causaba fallo al instalar todos los packs de tablas**
- Los 5 archivos de packs de tablas (`tables_adventure.json`, `tables_combat_magic.json`, `tables_names.json`, `tables_social.json`, `tables_world.json`) tenían BOM UTF-8 (`EF BB BF`) al inicio.
- `kotlinx.serialization` no tolera BOM: lanzaba `Unexpected JSON token at offset 0: Expected '{', but had '"'` al intentar instalar cualquier pack de tablas.
- El error era silencioso en `loadBundledTablesIfEmpty()` (atrapado por `catch`) pero visible en el nuevo diálogo de packs.
- **Fix 1 (fuente)**: BOM eliminado de los 5 archivos JSON de assets.
- **Fix 2 (defensivo)**: Agregado `.trimStart('\uFEFF')` al leer cualquier asset de pack en `TablesViewModel` y `ReferenceListViewModel`, para tolerar futuros archivos con BOM.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — APK generado con archivos de assets corregidos.

---

### Sprint 41 — Pack Manager en Pantalla de Tablas (18 de abril de 2026)

**FEATURE — Pack Manager nativo en Tablas**
- Eliminada la dependencia de navegar a Referencias para gestionar packs de tablas.
- `TablesViewModel` ahora inyecta `InstallStarterPackUseCase` y `RemoveStarterPackUseCase`, y expone `installTablePack()` / `removeTablePack()` / `setShowPackDialog()`.
- `observeInstalledPacks()`: colecta `getInstalledPackNames()` en tiempo real y filtra solo los packs `tables_*.json`, construyendo la lista de `TablePackInfo` reactiva.
- `TablesUiState` extendido con `showPackDialog` y `availableTablePacks`.
- Nuevo composable `TablePackDialog` en `TableLibraryScreen.kt` (visibilidad `internal`) con estados Instalado / Disponible y acciones Instalar / Actualizar / Quitar.
- `TableLibraryScreen`: botón de pack en TopBar (icono `AutoAwesome`); botón de estado vacío ahora abre el diálogo local.
- `TablesTab` (usado en sesiones): nuevo `SmallFloatingActionButton` terciario con `AutoAwesome`; diálogo de packs también disponible en el contexto de sesión; estado vacío corregido.
- `onExplorePacks` eliminado de `TablesTab` y `TableLibraryScreen` — era un callback de navegación a Referencias que ya no tiene sentido.
- NavGraph y `SessionScreen` actualizados para no pasar el callback obsoleto.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `:feature:tables` + `:feature:draw` + `:app` — sin errores.

---

### Sprint 40 — Corrección de Packs de Tablas Aleatorias (18 de abril de 2026)

**FIX — Pack `tables_loot.json` vacío**
- El archivo `tables_loot.json` existía pero contenía únicamente arrays vacíos — cero tablas y cero entradas.
- Reescrito con **15 tablas completas** y **152 entradas** organizadas en 4 categorías:
  - *Botín*: CR 0-4, CR 5-10, CR 11-16, CR 17+, Gemas, Arte/Antigüedades, Bandidos, Criatura
  - *Magia*: Pociones, Pergaminos, Objetos Mágicos Menores/Moderados/Mayores
  - *Equipamiento*: Armamento Especial
  - *Comercio*: Mercancías Valiosas
- Rango de IDs: tablas 20000–20014, entradas 20000–21409 (sin colisión con otros packs).

**FIX — `subTableId` siempre nulo en `importStarterPack`**
- Bug en `ReferenceRepositoryImpl`: al insertar entradas de tablas aleatorias desde un pack, `subTableId` se fijaba en `null` hardcodeado, ignorando el valor `dto.subTableId`.
- Corregido: ahora se remapea correctamente usando `randomTableIdMap[dto.subTableId]`, preservando las referencias entre sub-tablas dentro del mismo pack.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `:core:data:compileDebugKotlin` — Corrección compilada sin errores.

---

### Sprint 39 — Starter Packs Dinámicos y Resiliencia de Datos (18 de abril de 2026)

**CONTENT — Sistema de Starter Packs**
- **NEW**: Implementado sistema de **Ecosistema de Paquetes**. La app ahora escanea dinámicamente la carpeta `assets/starter_packs` para detectar nuevos contenidos JSON sin necesidad de actualizaciones de código.
- **DATA**: Implementada la **Migración 27 a 28**, añadiendo la columna `sourcePack` a tablas de referencia, reglas y tablas aleatorias para rastrear el origen del contenido.
- **INTELLIGENCE**: Lógica de "Instalación Inteligente" que evita duplicados. El sistema ahora "adopta" y actualiza registros antiguos (huérfanos) basándose en el nombre, unificando la base de datos automáticamente.
- **UX**: Nuevo diálogo de gestión de packs con estados visuales (Instalado/Disponible) y opciones para **Actualizar** o **Quitar** paquetes específicos.
- **EXPANSION**: Creados y expandidos packs exhaustivos para **D&D 5e (2024)** (Maestrías, Estados, Visión) y **Twilight 2000 4th Ed** (Tablas aleatorias de Clima y Encuentros, reglas de Stress).

**STABILITY — Resiliencia y Parches**
- **FIX**: Implementada lógica de **Autocuración de Esquema** en la Migración 28. Detecta y recrea automáticamente la tabla `npcs` si se encuentra en un estado inconsistente (`columns={}`), resolviendo fallos críticos de Room.
- **BACKUP**: `BackupDto` actualizado con valores por defecto y soporte para `sourcePack`, mejorando la compatibilidad con archivos JSON externos simplificados.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — Verificada la integridad de la migración v28 y el motor de escaneo dinámico de assets.

---

### Sprint 38 — Finalización Módulo de Referencias (18 de abril de 2026)

**REFERENCE — Tablas de Consulta y Reglas (Sprint 29-37)**
- **NEW**: Implementado módulo `:feature:reference` para centralizar tablas de reglas, equipo y mecánicas.
- **NEW**: Motor de importación tri-modal: **CSV/TSV**, **Markdown** y **OCR inteligente vía Gemini Vision**.
- **UX**: Grid de consulta interactivo con soporte para `CellExpandDialog` para lectura cómoda de celdas extensas.
- **INTEGRATION**: Integración profunda en `SessionScreen` con un nuevo tab de Referencias pre-filtrado por los sistemas de juego de la sesión activa.
- **CONFIG**: Implementado `GameSystemsSelector` y `SessionConfigSheet` para gestionar dinámicamente los sistemas (D&D 5e, Pathfinder 2e, etc) durante la partida.

**BACKUP — Integración de Referencias**
- **UPDATE**: `FullBackupDto` extendido para incluir todas las tablas de referencia y reglas del sistema.
- **DATA**: Implementada la migración de base de datos **26 a 27** con soporte para FTS4 en tablas de referencia y reglas para búsquedas ultra-rápidas.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — Verificada la integridad de los nuevos flujos de importación y la consistencia del backup.

---

### Sprint 28 — Seguridad Total: Backup & Restore en la Nube (17 de abril de 2026)

**BACKUP — Arquitectura de Persistencia (Portable)**
- **NEW**: Implementado motor de serialización usando `kotlinx.serialization`. Convierte toda la base de datos (mazos, tablas, encuentros, notas) en un archivo JSON agnóstico del schema interno de Room.
- **NEW**: Implementado `BackupRepository` con soporte para transacciones masivas (`withTransaction`), asegurando que la restauración sea atómica y segura.
- **NEW**: `FileRepository` extendido para manejar empaquetado/desempaquetado de ZIPs de biblioteca completa, incluyendo rutas relativas para todas las imágenes.

**UI — Gestión de Biblioteca (Settings)**
- **NEW**: Añadida sección "Copia de Seguridad" en Ajustes.
- **CLOUD**: Integración con Android SAF (Storage Access Framework). El DM ahora puede guardar su backup directamente en **Google Drive, Dropbox o Almacenamiento Local**.
- **SAFETY**: Implementado diálogo de confirmación destructiva antes de restaurar, alertando al usuario sobre el reemplazo total de datos.
- **PROGRESS**: Feedback visual mediante `CircularProgressIndicator` y Snackbars informativos durante todo el proceso de empaquetado y restauración.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — Verificada la integridad de la base de datos tras la inyección del nuevo `BackupDao`.

---

### Sprint 27 — Refactor Core: Importación Robusta y Motor de Combate (17 de abril de 2026)

**IMPORT — Arquitectura y Resiliencia (Sprint 1-3)**
- **NEW**: Implementado `PdfLayoutProcessor` para desacoplar la lógica de extracción de PDFs del UseCase principal. Optimizado para ejecución en `Dispatchers.IO`.
- **ARCHITECTURE**: Centralización total de enums (`DeckImportSource`, `TableImportSource`, `PdfLayoutMode`) en `core:model` para eliminar dependencias circulares y comparaciones de strings.
- **SAFETY**: Implementado **Rollback Automático** en `ImportDeckUseCase`. En caso de fallo, se eliminan los registros parciales de la DB y las imágenes físicas para evitar "datos basura".
- **INTELLIGENCE**: Refactorizado `FilenameParser` para ignorar ruido de escaneo ("Scan_", "IMG_", "Copia de") y soportar múltiples delimitadores (_, -, espacios) con normalización a *Title Case*.
- **VALIDATION**: Añadida validación proactiva de manifiesto en importación de ZIPs para asegurar la integridad antes de procesar.

**ENCOUNTERS — Motor de Combate Determinista (Sprint 1-2)**
- **NEW**: Implementado `CalculateInitiativeOrderUseCase`. Establece un orden de combate inmutable basado en reglas TTRPG (Iniciativa > Bono > Nombre), eliminando saltos visuales en el tracker.
- **LOGIC**: Refactorizado `NextTurnUseCase` para usar el `sortOrder` persistido. Añadido clamping de seguridad para el índice de turno ante borrados accidentales de criaturas.
- **NARRATIVE**: Implementado `ToggleConditionUseCase` con registro automático en el `CombatLog` usando lenguaje natural (ej: "Orco ahora está Aturdido").
- **AUTOMATION**: `ApplyDamageUseCase` ahora automatiza la narrativa del log, registrando caídas inconscientes y recuperaciones de consciencia de forma enriquecida.
- **CLEANUP**: Implementado `CleanupCombatUseCase` para eliminaciones seguras de participantes con recalculo inmediato del orden de iniciativa.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 515 tareas. Verificada la integridad de los nuevos UseCases y su correcta inyección en `SessionViewModel`.

---

### Sprint 26 — Configuración Centralizada, Estabilidad Visual y Reordenamiento (17 de abril de 2026)

**UX/UI — Reordenamiento Persistente (C-1 Extension)**
- **NEW**: Implementado sistema de **Drag & Drop** para la Biblioteca de Mazos y la Biblioteca de Tablas.
- **PERSISTENCE**: Añadido campo `sortOrder` a la base de datos (Entidades `CardStack` y `RandomTable`).
- **MIGRATION**: Implementada la `MIGRATION_25_26` para añadir soporte de ordenamiento sin pérdida de datos.
- **UX**: Integrado `sh.calvin.reorderable` con feedback háptico y visual (elevación 8dp) para una experiencia premium.
- **CONTROL**: Añadido modo "Ordenar" en TopAppBar para evitar movimientos accidentales durante la navegación.

### C-1 — Reordenar recursos y cartas (drag & drop) 🔴 ★★
Actualmente las cartas se muestran en orden `sortOrder`.

**Implementación:** `LazyVerticalGrid` con `sh.calvin.reorderable`.

**Estado:**
- [x] **Biblioteca (Mazos y Tablas)**: Implementado reordenamiento persistente (Sprint 26).
- [ ] **DeckDetail (Cartas individuales)**: Pendiente (Sprint 27).

**UX/UI — Centro de Mando del Mazo (C-2)**
- **NEW**: Implementada la `DeckConfigSheet` centralizada. Consolidada toda la configuración (Metadatos, Visuales, Reglas y Tags) en un panel de control único y premium.
- **FEATURE**: Edición en tiempo real de **Nombre** y **Descripción** del mazo con persistencia inmediata en `CardRepository`.
- **VISUAL**: Integrado el selector de **Imagen de Portada** (Cover) para personalizar la apariencia en la biblioteca.
- **CLEANUP**: Eliminado el diálogo `AddTagDialog` redundante; la gestión de etiquetas ahora es parte integral de la configuración centralizada mediante `FlowRow` y chips interactivos.
- **SAFETY**: Añadida la "Zona de Peligro" con acciones claras de **Archivar** y **Eliminar Mazos** (con diálogo de confirmación).

**VISUAL — Refactor 3D de Mecánica Boca Abajo (B-2/B-3)**
- **NEW**: Implementada rotación 3D real (`graphicsLayer`) con sombreado dinámico basado en el ángulo de giro.
- **ANIMATION**: Migrado a `Animatable` con **Spring** (fricción ajustada) para una respuesta táctil elástica y natural.
- **UX**: Las cartas robadas boca abajo ahora muestran el reverso del mazo correspondiente, manteniendo la intriga para el DM.

**FIX — Estabilización de Importación Markdown**
- **PARSER**: Mejorado el `MarkdownTableParser` con una regex robusta para soportar delimitadores complejos, espacios variables y múltiples columnas sin pérdida de datos.
- **BUG**: Corregido mapeo en `TableImportViewModel` que ignoraba el modo de importación MARKDOWN, permitiendo ahora el flujo completo desde archivos `.md`.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `:feature:deck:compileDebugKotlin` — Verificada integridad de mappers, inyección de dependencias y recursos visuales.

---

### Sprint 18 — Biblioteca de NPCs y Criaturas (17 de abril de 2026)

**ARCHITECTURE — Nuevo Módulo `:feature:npcs`**
- **NEW**: Implementado el módulo independiente `:feature:npcs` para la gestión de la biblioteca persistente.
- **DATA**: Migración de base de datos a la versión **24**. Añadidas tablas `npcs` y `npc_tags`.
- **DATA**: `EncounterCreatureEntity` extendido con `npcId` e `imagePath` para permitir la vinculación con la biblioteca.
- **LOGIC**: Implementados use cases para CRUD de NPCs y guardado local de avatares en `/npcs/`.

**FEATURE — Biblioteca de NPCs Premium**
- **UI**: `NpcListScreen` — Vista de grid visual con previsualización de stats (HP, AC, Iniciativa) y badges de avatars.
- **UI**: `NpcEditorScreen` — Editor completo con selector de imágenes, campos numéricos y soporte **Markdown** para lore y notas.
- **UX**: Sistema de filtrado por Tags para organizar criaturas por tipo, entorno o alineamiento.

**INTEGRATION — Flujo de Trabajo del DM**
- **NEW**: Integrado selector de NPCs en el `EncounterEditorScreen`. Permite añadir múltiples criaturas a un encuentro usando NPCs de la biblioteca como plantillas.
- **UI**: El `CombatTab` ahora muestra los **Avatares** de las criaturas vinculadas, mejorando drásticamente el feedback visual durante el rastreo de iniciativa.
- **INTEGRATION**: Registro en el `NavGraph` principal y acceso desde el menú de la `LibraryScreen`.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 337 tareas. Verificada integridad de mappers, migraciones y Hilt/Dagger.

---

### Sprint 17 — Combat Tracker e Integración de Encuentros (17 de abril de 2026)

**FEATURE — Combat Tracker Premium**
- **NEW**: Implementado `CombatTab` dinámico en `SessionScreen`. Solo aparece cuando hay un encuentro activo.
- **NEW**: Soporte para **Participantes Temporales (PJs)**. El DM puede añadir jugadores a la iniciativa con un solo tap sin persistirlos en la biblioteca de encuentros.
- **UX**: Visualización "Bloodied" (Sangriento) automática cuando una criatura cae por debajo del 50% de HP.
- **UX**: Animaciones de reordenamiento automático en la lista de iniciativa (`animateItem`) y escala visual para el turno activo.
- **LOGIC**: Mejorado `NextTurnUseCase` (ahora en `SessionViewModel`) para ciclar correctamente entre criaturas y jugadores temporales.

**INTEGRATION — Automatización de Notas**
- **NEW**: Al finalizar un combate, se genera un resumen detallado (Rondas, Supervivientes) que se anexa automáticamente a las Notas del DM de la sesión.
- **LOGIC**: `ApplyDamageUseCase` gestiona automáticamente la condición `UNCONSCIOUS` al llegar a 0 HP y la retira al curar.

---

### Sprint 16 — Notas por carta y Portabilidad (ZIP Export v2) (17 de abril de 2026)

**ARCHITECTURE — Componentes Globales**
- **MOVE**: Refactorizado `MarkdownToolbar` y movido a `:core:ui`.
- **FEATURE**: Notas por carta completas con autoguardado y vista previa.
- **INTEGRATION**: Exportación e Importación enriquecida mediante `deck_manifest.json`.

---

### Sprint 15 — Notas de DM: Editor Markdown y UX de Guardado (17 de abril de 2026)

**FEATURE — Editor de Notas Pro**
- **NEW**: Implementada `MarkdownToolbar` con acciones rápidas: Bold, Italic, Link, Listas, Headers (H1/H2), Checkboxes y Código.
- **INTEGRATION**: `NotesTab` refactorizado para usar `TextFieldValue`. Esto permite insertar etiquetas Markdown exactamente en la posición del cursor o envolver el texto seleccionado.
- **UX**: Mejorada la jerarquía visual del editor con contenedores `Surface` redondeados, bordes suaves y tipografía optimizada.

**FEATURE — Feedback de Autoguardado**
- **UX**: Añadido indicador visual de "Guardando…" (CircularProgress + Texto) en la cabecera de la pestaña de notas.
- **UX**: Añadido icono de verificación (Check) una vez persistido el cambio para dar tranquilidad al DM.
- **STATE**: Nueva propiedad `isSavingNotes` en `SessionUiState` controlada desde el ViewModel durante la persistencia en `SessionRepository`.

**FEATURE — Pulido de Notas Rápidas (Quick Notes)**
- **UX**: Rediseñado el `QuickNoteDialog` con un look más premium y mejor feedback ("Se añadirá con un timestamp...").
- **UX**: Foco automático en el editor al abrir el diálogo para permitir escritura inmediata sin toques extra.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** — Verificado que el autoguardado con debounce (800ms) funciona correctamente con la nueva estructura de `TextFieldValue`.

---

### Sprint 24 — Gemini Vision Mode: Path Multimodal para Reconocimiento de Tablas (17 de abril de 2026)

**INTEGRATION — Gemini Vision Mode (Multimodal)**
- **NEW**: Añadido `recognizeTableFromImage(bitmap, apiKey)` a `AiTableRepository` y su implementación en `GeminiAiRepository`. Envía el bitmap recortado directamente a Gemini como entrada multimodal, bypassando ML Kit OCR por completo.
- **NEW**: `RecognizeTableFromImageUseCase` en `:core:domain` — orquesta la llamada al repositorio de IA con visión.
- **NEW**: `TableImportViewModel.recognizeWithVision()` — invoca el use case, popula las entradas y navega al paso MAPPING directamente desde RECOGNITION.
- **UI**: Paso RECOGNITION ahora ofrece dos botones: "OCR" (flujo existente) e "IA Vision" (nuevo path). El botón Vision muestra `CircularProgressIndicator` durante el procesamiento.
- **STATE**: Añadido `isVisionProcessing: Boolean` a `TableImportUiState`.

**UPDATE — Modelo Gemini actualizado a 2.0 Flash**
- Cambiado `gemini-1.5-flash-latest` → `gemini-2.0-flash` en `GeminiAiRepository` para ambos endpoints (texto e imagen).
- `apiVersion` actualizado a `v1beta` (requerido por Gemini 2.0).
- Extraídas constantes `MODEL_NAME` y `API_VERSION` en companion object.
- Refactorizado el parsing de respuesta a función privada `parseResponse()` para eliminar duplicación.

**ARCHITECTURE — Backward compatible**
- El path OCR original no fue modificado. Usuarios sin API key configurada ven el botón Vision pero el error se gestiona mediante el diálogo de error existente.

**INTEGRATION — Build**
- Pendiente verificación con `assembleDebug`.

---

### Sprint 23 — Importación Inteligente: Markdown y Gemini AI (17 de abril de 2026)

**FEATURE — Soporte Nativo de Tablas Markdown (`.md`)**
- **NEW**: Implementado `MarkdownTableParser` para reconocer la sintaxis estándar de tablas `| Header |`.
- **INTEGRATION**: El selector de archivos ahora permite elegir archivos `.md` y los procesa extrayendo las filas de datos ignorando los delimitadores de formato.

**FEATURE — Transcripción Inteligente con Gemini AI**
- **INTEGRATION**: Añadido el SDK de Google AI (`generativeai`).
- **NEW**: Botón "Varita Mágica" en `TableReviewView` que envía el texto (especialmente tras OCR) a Gemini 1.5 Flash.
- **AI**: La IA reconstruye y limpia los datos de la tabla, corrigiendo errores de OCR y re-estructurando columnas desordenadas en un formato JSON estructurado.
- **CONFIG**: Implementada la gestión segura de la API Key en la pantalla de Ajustes, persistida localmente.

**ARCHITECTURE — Settings & Configuration**
- **NEW**: `SettingsRepository` centraliza la gestión de la API Key de Gemini y la calidad de imagen JPEG.
- **FIX**: Resincronizados los ViewModels para usar el nuevo repositorio en lugar de acceso directo a SharedPreferences.

**FIX — Limpieza de Warnings y Estabilización**
- **DEPRECATION**: Reemplazados iconos `List` y `ViewList` por sus versiones `AutoMirrored` para compatibilidad futura y RTL.
- **OPT-IN**: Añadido `@OptIn(FlowPreview::class)` en `LibraryViewModel` para silenciar avisos del operador `debounce`.
- **KAPT**: Configurado `kapt.languageVersion = 1.9` en `gradle.properties` para resolver el aviso de fallback en Kotlin 2.0.
- **BUILD**: Corregidos errores de referencias no resueltas e inyección de dependencias en `TableImportViewModel`.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 313 tareas, sin warnings de código.

---

### Sprint 22 — Estabilización del Build y Refactor de Módulos (16 de abril de 2026)

**FIX — Solución a error de inyección de Dagger Hilt (Missing Binding)**
- **PROBLEM**: El build fallaba con un error de inyección en `TableRepositoryImpl` porque `TableBundleDao` no tenía un método `@Provides` en el módulo de Dagger.
- **SOLUTION**: Añadido `@Provides fun provideTableBundleDao(db: DeckAppDatabase)` a `DatabaseModule` en `DataModule.kt`.
- **FIX**: Detectada y corregida la omisión de `MIGRATION_19_20` en el `databaseBuilder` de Room, asegurando la consistencia entre la versión de la base de datos (20) y las migraciones registradas.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 445 tareas, 21 ejecutadas, sin errores.

---

### Sprint 22 — Estabilización del Build y Refactor de Módulos (16 de abril de 2026)

**ARCHITECTURE — Solución Crítica: KSP Internal Compiler Error**
- **PROBLEM**: El módulo `:feature:tables` sufría un error interno de KSP (NullPointerException) que bloqueaba completamente el build, sin causa aparente en el código fuente.
- **SOLUTION**: Migrado el procesamiento de Hilt en `:feature:tables` de **KSP a Kapt**. Aunque KSP es el estándar moderno, Kapt ofrece la estabilidad necesaria para este módulo específico mientras se investiga la causa raíz del fallo en el compilador.
- **CONSOLIDATION**: Fusionados `TableLibraryViewModel` y `TablesViewModel` en una única unidad lógica. Eliminado `TableImportViewModel` del módulo `:feature:tables` para reducir la complejidad y los puntos de fallo de inyección.

**FIX — Solución al Crash de Inicio (Database Schema Mismatch)**
- **PROBLEM**: El app crasheaba instantáneamente al arrancar debido a un fallo en la validación de Room.
- **CAUSE**: La migración 18→19 creaba un índice para la tabla `recent_files`, pero la entidad `@Entity RecentFileRecord` no lo tenía definido en su código Kotlin.
- **SOLUTION**: Añadido `indices = [Index(value = ["uri"], unique = true)]` a `RecentFileRecord`. Alineados todos los índices manuales con las definiciones de las entidades de Room para asegurar la integridad del esquema.

**FIX — Solución al Crash en Importación (SecurityException)**
- **PROBLEM**: Crash al seleccionar un PDF desde la navegación por carpetas (`No persistable permission grants found`).
- **CAUSE**: Se intentaba llamar a `takePersistableUriPermission` sobre un URI de documento hijo que ya heredaba permisos del árbol (tree URI) de la carpeta madre, lo cual no es permitido por Android.
- **SOLUTION**: Implementado un bloque `try-catch` en `ImportViewModel.onPdfSelected` para manejar de forma segura los permisos de URIs que ya poseen acceso delegado.

**UX/OCR — Mejoras en el Reconocimiento de Tablas**
- **FIX**: Eliminado el residuo de caracteres separadores (como puntos, dos puntos o guiones) que el OCR mantenía en el texto de las filas tras extraer el número de el rango. Mejorada la robustez de la limpieza en `AnalyzeTableImageUseCase`.

**BUILD — Soporte de Alineación 16 KB (Android 15+)**
- **FIX**: Resuelta la advertencia de `Android 16 KB Alignment` en `libmlkit_google_ocr_pipeline.so`.
- **UPDATE**: ML Kit Text Recognition actualizado a `16.0.1`.
- **WORKAROUND**: Añadida dependencia `androidx.camera:camera-core:1.4.2` para forzar utilidades nativas alineadas.
- **CONFIG**: Habilitado `useLegacyPackaging = true` en el bloque de `packaging` para asegurar la correcta alineación de librerías JNI.

**FEAT — Importación Proactiva de Tablas**
- **NEW**: Implementada la navegación por el dispositivo para importación de tablas.
- **NEW**: Soporte para miniaturas de PDFs en el listado de archivos encontrados tras seleccionar una carpeta (Tree URI).
- **UX**: Conectada la interfaz de `SourceSelectionView` con `ActivityResultLauncher` para una experiencia nativa y fluida.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 325 tareas, sin errores.

---

### Sprint 21 — Gestión de Recursos y UX de Nombramiento (15 de abril de 2026)

**FEATURE — Filtrado de Tablas por Sesión**
- `TablesViewModel` ahora soporta `setSession(id)`. Si hay una sesión activa, el tab de tablas solo muestra las tablas "asignadas" por defecto.
- Añadido un interruptor ("Mostrando todas") para navegar rápidamente por la biblioteca completa sin salir del contexto de la sesión.

**UI/UX — Renombrado Prominente en Importación**
- **PROBLEM**: Los usuarios a veces olvidaban cambiar el nombre predeterminado al importar recursos nuevos.
- **SOLUTION**: Los campos de Nombre (Mazos/Tablas) se han movido a la parte superior del paso final de configuración, envuelvos en una `Surface` resaltada (primaryContainer) para asegurar su visibilidad inmediata.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 462 tareas, sin errores.

---

**ARCHITECTURE — Refactor de MazosTab → DeckWorkspace**
- **PROBLEM**: Con múltiples mazos activos, la mano era una única fila horizontal de scroll infinito sin distinción visual entre recursos de diferente origen.
- **SOLUTION**: Reemplazado `MazosTab` (scroll horizontal) por `DeckWorkspace` (lista vertical de clusters), donde cada mazo de la sesión tiene su propia caja Bento colapsable.
- **IMPACT**: Eliminado el scroll horizontal; las cartas se organizan en `FlowRow` que envuelve automáticamente a múltiples filas.

**FEATURE — DeckClusterItem**
- Nuevo componente `DeckClusterItem`: header colapsable (nombre del mazo + "N en mano · M disponibles" + icono ExpandMore/Less), body animado con `AnimatedVisibility`.
- Cuando el cluster no tiene cartas: slot vacío con call-to-action "Selecciona y pulsa ROBAR" que activa el mazo al tocarlo, eliminando la necesidad de la `DeckBar` separada.

**FEATURE — CompactCardItem**
- Nueva variante de carta de 120dp (vs 160dp en SwipeToDiscardCard) para acomodar más cartas por pantalla sin scroll.
- Mantiene swipe-to-discard, tap para detalle, alpha de feedback de arrastre, e indicador de tabla enlazada.

**ARCHITECTURE — SessionViewModel / SessionUiState**
- Nuevos campos: `collapsedDeckIds: Set<Long>` (efímero, no persistido), `handByDeck: Map<Long, List<Card>>` (derivado reactivamente de `hand`).
- Nuevas funciones: `toggleDeckCollapse(stackId)`, `setLastInteractedDeck(stackId)`.

**INTEGRATION — Build**
- `@file:OptIn(ExperimentalLayoutApi::class)` para `FlowRow` (API experimental de Compose).
- **BUILD SUCCESSFUL** `assembleDebug` — 409 tareas, sin errores.

---

### Sprint 20 — Reconocimiento de Tablas: Parsers y Pulido (15 de abril de 2026)

**FEATURE — Multi-columna configurable en `CsvTableParser`**
- Nuevo campo `additionalColumns: List<Int>` en `ParseConfig`. Las columnas adicionales se concatenan al texto principal con ` — `.
- `ParsePreview` expone `columnCount` para que la UI pueda renderizar un selector de columnas.

**FIX — `inferRollFormula` detecta 2d6, 3d6 y otras combinaciones multi-dado**
- Nueva sobrecarga `inferRollFormula(minValue, maxValue)` que reconoce 2d4 (2–8), 2d6 (2–12), 2d10 (2–20) y 3d6 (3–18).
- `ImportTableUseCase.inferFormula()` ahora pasa tanto min como max.
- La sobrecarga de 1 argumento se mantiene por compatibilidad (asume min=1).

**FEATURE — Soporte formato Roll20 en `JsonTableParser`**
- Detecta objetos con campo `"items"` (formato `RollableTable` de Roll20).
- Convierte `weight` en rangos acumulativos: item de weight=3 ocupa 3 resultados consecutivos.

**ARCHITECTURE — `TableParseException` sellada**
- Nuevo archivo `TableParseException.kt` en `:core:domain` con casos: `InvalidJson`, `UnknownFormat`, `EmptyResult`.
- `JsonTableParser` relanza `InvalidJson` y `UnknownFormat` en lugar de `IllegalArgumentException`.
- `ImportTableUseCase` lanza `EmptyResult` cuando JSON o texto plano no producen entradas.

**FIX — Gutters con umbral de votos adaptativo**
- Tablas con menos de 10 líneas usan `minVotes = 1` en `detectColumns`. Las tablas grandes mantienen el 15%.
- Evita descartar columnas opcionales en tablas pequeñas con datos dispersos.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 443 tareas, sin errores.

---

### Sprint 19 — Reconocimiento de Tablas: Robustez del Pipeline (15 de abril de 2026)

**FIX — `groupIntoLines` por centro de línea acumulado**
- **PROBLEM**: La tolerancia usaba la altura del bloque *anterior*, no de la línea en construcción. Un bloque con altura atípica (ej. un dígito grande) desplazaba el threshold para todos los siguientes.
- **SOLUTION**: Se mantienen `lineCenterY` y `lineAvgHeight` como medias incrementales. Cada bloque nuevo se compara contra el centro actual de la línea, no contra el último bloque visto.

**FIX — Modo percentil en `splitLinesIntoTables`**
- Pre-escaneo de todas las líneas para detectar si `maxRoll >= 90` (tabla 01–100).
- En modo percentil, `resetThreshold = 10` en lugar de 2 para evitar partir en la transición "91-00" → "1".

**FEATURE — DPI adaptativo en `loadPdfPage`**
- `renderPageAdaptiveDpi()`: primer render a 800px, mide altura promedio de bloques OCR, luego elige resolución final: 1800px (texto denso), 1200px (normal), 900px (texto grande).
- Evita cargar bitmaps de alta resolución innecesarios en tablas con tipografía grande.

**ARCHITECTURE — `OcrRepositoryImpl` a `@ActivityRetainedScoped`**
- Cambiado de `@Singleton` a `@ActivityRetainedScoped` para liberar el reconocedor ML Kit al salir del flujo de importación.
- **FIX de build**: Extraído a `OcrModule` propio con `@InstallIn(ActivityRetainedComponent::class)`, ya que el scope no es compatible con `SingletonComponent`.

**FEATURE — Stitching con dos modos (`StitchingMode`)**
- Nuevo enum `StitchingMode`: `CONTINUE_RANGES` (desplaza rolls para continuar la secuencia) y `APPEND` (respeta rolls del OCR, solo ajusta `sortOrder`).
- `updateStitchingMode()` expuesto en el ViewModel para que la UI ofrezca el toggle al activar stitching.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 443 tareas, sin errores.

---

### Sprint 18 — Reconocimiento de Tablas: Heurísticas, Confianza y Parsers (15 de abril de 2026)

**FEATURE — Confianza OCR propagada hasta el UiState**
- `buildEntries` calcula `minConfidence` por fila (mínimo de los bloques que la componen).
- `AnalysisResult.lowConfidenceIndices` → `ImportResult.lowConfidenceIndices` → `TableImportUiState.lowConfidenceIndices`.
- Umbral: `CONFIDENCE_THRESHOLD = 0.6f` en `AnalyzeTableImageUseCase`. La UI de revisión puede resaltar estas entradas (implementación visual pendiente).

**FIX — Clustering con promedios globales**
- `clusterBlocks` ahora calcula `avgWidth` y `avgHeight` sobre todos los bloques antes del BFS. Elimina el problema donde un bloque pequeño (ej. "1") generaba un threshold mínimo, impidiendo conectar bloques cercanos.

**FIX — `suggestTablePoints` excluye encabezados y pies de página**
- Filtro: bloques en el 5% superior/inferior de la imagen y textos de ≤3 caracteres se ignoran al calcular el bounding box sugerido.

**FIX — Bug crítico: `expectedTableCount=1` partía la tabla en múltiples**
- **PROBLEM**: `splitLinesIntoTables` aplicaba la heurística de reinicio incluso cuando el usuario declaraba explícitamente 1 tabla.
- **SOLUTION**: Si `expectedCount == 1`, la función devuelve todas las líneas como un único grupo sin evaluar reinicios.

**FEATURE — Detección de formato CSV/JSON por contenido**
- `onFileSelected` asigna `ImportMode.NONE` cuando la extensión no es `.csv`/`.json`.
- `loadFileText` resuelve el modo tras leer el contenido: detecta JSON por prefijo `{`/`[`, CSV por presencia de delimitadores.

**FIX — ENUM_PATTERN ampliado en PlainTextTableParser**
- Cubre ahora `[1-5] texto`, tabulador como separador, y espacio simple sin símbolo de puntuación.

**FIX — Header detection mejorada en CsvTableParser**
- Reconoce keywords TTRPG ("roll", "result", "rango", "encounter", etc.) como señal positiva de cabecera.
- Valida con `RangeParser` antes de aplicar la heurística de dígitos, evitando clasificar `"1d6"` como dato.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** `assembleDebug` — 443 tareas, 28 ejecutadas, sin errores.

---

### Sprint 17 — Reconocimiento de Tablas: Fundamentos del Pipeline OCR (15 de abril de 2026)

**ARCHITECTURE — Jerarquía OCR: TextLine como unidad base**
- **PROBLEM**: `OcrRepositoryImpl` iteraba `TextBlock` y concatenaba líneas con `\n`, forzando a `AnalyzeTableImageUseCase` a re-separar lo que ML Kit ya tenía separado con `splitMultiLineBlocks`.
- **SOLUTION**: Refactorizado `OcrRepositoryImpl` para iterar `block.lines` (nivel `TextLine`). Cada línea produce un `OcrBlock` independiente con bounding box propio.
- **IMPACT**: `splitMultiLineBlocks` pasa a ser safety net en lugar de paso crítico. Bounding boxes más precisos mejoran `groupIntoLines` y `detectColumns`.

**ARCHITECTURE — `OcrBlock.confidence` real + `RectModel` Float**
- `confidence` ahora es el promedio de `TextElement.confidence` por línea en vez de `1.0f` hardcodeado. Habilita filtrado de entradas inciertas en sprints futuros.
- `RectModel` migrado de `Int` a `Float` para preservar precisión al escalar bitmaps. Eliminadas conversiones `.toFloat()` redundantes en `detectColumns`.

**ARCHITECTURE — `OcrRepository` devuelve `Result<List<OcrBlock>>`**
- **PROBLEM**: Fallo silencioso: `catch (e: Exception) { emptyList() }` hacía indistinguible un error de ML Kit de una imagen sin texto.
- **SOLUTION**: Firma cambiada a `Result<List<OcrBlock>>`. Nuevo `OcrException` en `:core:domain`. `ImportTableUseCase.getRawBlocks` usa `getOrThrow()` — el ViewModel ya tenía `try-catch` y distingue el tipo de error.

**FEATURE — Filtro de ruido de separadores**
- `AnalyzeTableImageUseCase` descarta bloques cuyo texto es solo guiones, barras o subrayados (`"———"`, `"_____"`, `"| | |"`). Estos son líneas de tabla que ML Kit convierte en texto y rompían el clustering.

**FIX — Rangos percentiles `00 = 100`**
- `RangeParser` ahora normaliza `"00"` → `100`. `"96-00"` se parsea como `ParsedRange(96, 100)`. Convención estándar en tablas OSR y D&D Basic.

**FIX — BOM UTF-8 en CSV de Excel**
- `CsvTableParser.preview()` y `parse()` aplican `trimStart('\uFEFF')`. El primer campo ya no aparece como `"﻿Roll"` en archivos exportados desde Excel.

---

### Sprint 11 — OCR Avanzado: Segmentación Multilínea y Control por Tabla (15 de abril de 2026)

**FEATURE — Motor OCR Mejorado (`AnalyzeTableImageUseCase`)**
- Implementada `splitMultiLineBlocks`: divide bloques OCR que fusionan filas múltiples (común en layouts con zebra-striping), recuperando números de Roll ocultos.
- Parámetro `expectedTableCount` para que el usuario indique cuántas tablas esperar, ajustando la agresividad del clustering espacial.

**UI/UX — Edición Manual en ReviewStep**
- `BasicTextField` editable para `minRoll` / `maxRoll` directamente en la lista de entradas.
- Botones `ExpandLess`/`ExpandMore` en `EntryRow` para reordenar filas manualmente tras la detección.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — Sin regresión en flujos existentes de CSV, JSON y Texto.

---

### Sprint 16 — Estabilización Crítica: Persistencia y Motor de PDF (15 de abril de 2026)


**ARCHITECTURE — Migración a PdfRenderer Nativo**
- **PROBLEM**: El uso de `pdfium-android` causaba `NoClassDefFoundError: ArrayMap` en dispositivos modernos debido a dependencias obsoletas de Android Support superpuestas con AndroidX.
- **SOLUTION**: Refactorizado `FileRepositoryImpl` para utilizar la API nativa `android.graphics.pdf.PdfRenderer`.
- **IMPACT**: Eliminación de dependencias externas pesadas (`pdfium`, `legacy-support`) y mejora de la estabilidad en los flujos de importación de PDF.

**ARCHITECTURE — Consolidación de Integridad Referencial (MIGRATION 11→12→13)**
- **PROBLEM**: Crone de `SQLiteConstraintException` y fallo de arranque `IllegalStateException` indicando que faltaba una ruta de migración de la versión 11 a la 13.
- **SOLUTION**: Actualizado el esquema Room (versión 13) para incluir `@ForeignKey` con `onDelete = CASCADE` en todas las tablas dependientes.
- **FIX**: Corregida la omisión de `MIGRATION_11_12` y `MIGRATION_12_13` en el `Room.databaseBuilder` dentro de `DataModule.kt`.
- **FIX**: Corregida discrepancia de esquema en `session_deck_refs` (índice faltante) y typo `AUTOINT` en scripts de migración previos.

**INTEGRATION — Build y Calidad**
- **PENDING**: Verificación de flujos de importación de PDF con el nuevo motor nativo y validación de borrado masivo de recursos.

---

### Sprint 11 — Detección de Múltiples Tablas e Importación por Lotes (15 de abril de 2026)

**FEATURE — Clustering Espacial en OCR**
- Implementada la lógica de **clustering espacial** en `AnalyzeTableImageUseCase` para separar múltiples tablas en una misma imagen/página.
- El sistema utiliza un algoritmo de componentes conectados para agrupar bloques de texto próximos, permitiendo detectar tablas independientes.
- Añadida heurística para la **detección automática de títulos** de tablas basada en el contexto del cluster.

**UI/UX — Review Multi-tabla**
- Rediseñada la pantalla `TableImportScreen` para incluir un **selector de pestañas** (`ScrollableTabRow`) cuando se detectan múltiples tablas.
- Permite al usuario revisar, editar y guardar cada tabla de forma independiente dentro del mismo flujo de importación.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — Verificada la compatibilidad de la nueva lógica de OCR con los flujos existentes de CSV y Texto.

---

### Sprint 10 — Estabilización y Gestión de Deuda Técnica (15 de abril de 2026)

**PROBLEM/SOLUTION — Bloqueos de Base de Datos y PDF**
- **DB (MIGRATION 10→11)**: Corregid el fallo de arranque por índices faltantes en `session_deck_refs` y `session_table_refs`. Implementada migración manual para preservar datos.
- **PDFium (AndroidX)**: Solucionado el error `NoClassDefFoundError: ArrayMap` mediante la inclusión de `androidx.legacy:legacy-support-v4` como puente de compatibilidad.

**INTEGRATION — Build**
- **BUILD SUCCESSFUL** — El proyecto vuelve a ser completamente estable.

---

### Sprint 9 — Integración de Cartas y Tablas (15 de abril de 2026)

**ARCHITECTURE — Vínculo de Dominio y Persistencia (MIGRATION 8→9)**
- Implementada la migración de base de datos **8 a 9** para añadir la columna `linkedTableId` a la tabla `cards`.
- Actualizado el modelo de dominio `Card` y los `Mappers` para soportar la asociación opcional con tablas aleatorias.
- Incrementada la versión de la DB a **9** y registrada la migración en `DataModule`.

**FEATURE — Selector de Tablas en Editor de Cartas**
- Añadida la sección **"Acción Vinculada"** en `CardEditorScreen`.
- Implementado un selector modal que carga dinámicamente todas las tablas disponibles de la biblioteca.
- Permite vincular, cambiar o eliminar la asociación de una tabla con la carta de forma persistente.

**FEATURE — Acción Rápida en Sesión (Direct Roll)**
- Las cartas en la `SessionScreen` ahora detectan si tienen una tabla vinculada.
- Añadido un **trigger visual** (icono de dado) en la esquina superior de la carta (cuando está revelada).
- Al pulsar el icono, se dispara `tablesViewModel.rollTable(tableId)`, mostrando el resultado instantáneamente en el panel de detalles.
- Solucionado el problema de *Smart Cast* en propiedades multi-módulo mediante captura en variables locales.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — Verificada la compatibilidad de las migraciones y la integridad de los nuevos componentes UI.
- Actualizada la documentación funcional en `walkthrough.md`.

---

### Sprint 8 — Sistema de Tablas Aleatorias (Fase Avanzada) (15 de abril de 2026)

**ARCHITECTURE — Corrección de Perspectiva (4 Puntos)**
- Implementada la lógica de **Homografía** usando `android.graphics.Matrix.setPolyToPoly`.
- Permite transformar cuadriláteros irregulares (fotos de manuales físicos) en bitmaps rectulares planos para un OCR óptimo.
- UI: Reemplazado `cropRect` por lista de 4 puntos (`Offset`) arrastrables de forma independiente con líneas conectoras y área de recorte visual.

**FEATURE — Soporte Multipage (Stitching) y Flujo Continuo**
- Añadido `isStitchingMode` a `TableImportViewModel`.
- Botón "Añadir página" permite concatenar resultados de diferentes recortes o páginas en una sola tabla lógica antes de guardar.
- El sistema ajusta automáticamente el `sortOrder` y la integridad de los dados al añadir nuevas filas.

**FEATURE — Detección Automática de Columnas y Heurística de Datos**
- El motor de análisis detecta la estructura horizontal de la tabla buscando alineaciones de bloques (gutters) en el eje X.
- Separación automática de múltiples columnas usando el delimitador `|` (ej: `1-10 | Poción | 50 gp`).
- Merge multilínea inteligente: las descripciones largas se unen solo a su columna correspondiente basándose en su posición horizontal.

**FEATURE — Auto-detección de Bordes (Sugerencia OCR)**
- Escaneo silencioso al abrir una página: utiliza los resultados de OCR iniciales para calcular el *bounding box* del contenido textual.
- La UI posiciona automáticamente las 4 esquinas rodeando la tabla detectada para minimizar el ajuste manual.

**INTEGRATION — Estabilidad y Verificación**
- **BUILD SUCCESSFUL** — verificado post-implementación de todas las heurísticas avanzadas.
- Actualizada documentación técnica en `walkthrough.md`.

---

### Sprint 7 — Completar Sistema de Tablas (15 de abril de 2026)

**FEATURE — FAB "TIRAR" real en SessionScreen (tab Tablas)**
- `TablesViewModel` inyectado en `SessionScreen` via `hiltViewModel()` — mismo NavBackStackEntry, misma instancia compartida con `TablesTab`.
- FAB página 2 (Tablas): si `activeTable != null` → tira esa tabla y el resultado aparece en `TableDetailSheet` ya abierto. Si no hay tabla activa → Snackbar "Abre una tabla para usar TIRAR".
- `SessionFab` actualizado: acepta `hasActiveTable`, muestra FAB atenuado (α 0.5) y sublabel "tabla activa" para feedback visual.

**FEATURE — Export de tablas a JSON**
- `ExportTableUseCase` (object en `:core:domain`): serializa `RandomTable` → formato DeckApp JSON v1 (compatible con `JsonTableParser`). Escapa correctamente `\`, `"`, `\n`.
- `TablesViewModel.getExportJson()`: construye JSON desde `activeTable` (ya en memoria con entries completos).
- `TableDetailSheet`: nuevo parámetro `onExport`, nuevo botón de Share en la cabecera.
- `TablesTab`: `rememberLauncherForActivityResult(CreateDocument)` → el usuario elige destino → se escribe el JSON via `ContentResolver`.

**FEATURE — WEIGHTED mode en TableEditorScreen**
- Campo "Fórmula" deshabilitado cuando `rollMode == WEIGHTED` (la fórmula se ignora en ese modo).
- Nota informativa: "Modo Peso: la fórmula se ignora. El resultado se elige por probabilidad relativa."
- `RollTableUseCase.pickEntry` ya manejaba WEIGHTED correctamente — solo faltaba el feedback visual en el editor.

---

### Sprint 6 — Sistema de Importación de Tablas (14 de abril de 2026)

**ARCHITECTURE — Motor de importación multi-fuente**
- `ImportTableUseCase` en `:core:domain` — orquestador universal: OCR, CSV, JSON, texto plano.
- `AnalyzeTableImageUseCase` — heurística de parsing OCR: agrupación por Y, detección de rango/texto, manejo de entradas multilínea.
- `RenderPdfPageUseCase` — envuelve `FileRepository.renderPdfPageToBitmap` para previsualización en el flujo de importación.
- `CsvTableParser` — auto-detección de delimitador, preview de columnas, `ParseConfig` para mapeo manual.
- `JsonTableParser` — compatibilidad con Foundry VTT `RollTable` y formato DeckApp JSON.
- `PlainTextTableParser` — detección de patrones `N. texto`, `- [rango] texto` y listas simples.
- `RangeParser` — motor centralizado: parseo, validación de integridad (huecos/solapamientos), inferencia de fórmula de dado.
- `ReadTextFromUriUseCase` — lectura de texto desde URI SAF vía `FileRepository.readTextFromUri`.

**FEATURE — TableImportScreen (flujo de 5 pasos)**
- Paso 1 (`SOURCE_SELECTION`): 4 cards de origen — Imagen, PDF, Archivo, Pegar texto.
- Paso 2 (`CROP`): canvas interactivo con rectángulo de recorte arrastrable sobre el PDF/imagen; navegación de páginas PDF.
- Paso 3 (`FILE_PREVIEW`): texto raw editable — continuar para CSV configura columnas, para texto plano analiza directamente.
- Paso 4 (`MAPPING`): selección de columna Rango / Texto para CSV con muestra visual de filas.
- Paso 5 (`REVIEW`): entradas editables inline, advertencias de huecos/solapamientos, nombre y categoría de tabla; checkmark en TopBar guarda en Room.

**FEATURE — TableImportViewModel**
- Flujo completo: selección → carga → análisis → revisión → guardado.
- `loadFileText` usa `ReadTextFromUriUseCase` para leer archivos CSV/JSON/TXT desde URI SAF.
- `saveTable()` construye `RandomTable` desde el estado de revisión y persiste via `TableRepository`.
- `savedSuccessfully: Boolean` en `TableImportUiState` — señal para que la pantalla navegue de vuelta.

**INTEGRATION — FileRepository**
- `readTextFromUri(uri: Uri): String` añadido a interfaz e implementado en `FileRepositoryImpl` via `ContentResolver`.

**INTEGRATION — NavGraph**
- `TableImportRoute` ya estaba registrado; `onImportFinished` ahora observado via `LaunchedEffect(savedSuccessfully)`.
- `BUILD SUCCESSFUL` — pendiente de verificación post-refactor.

---

### Sprint 5 — Tablas Aleatorias (14 de abril de 2026)

**ARCHITECTURE — Sistema de tablas aleatorias completo**
- Módulo `:feature:tables` creado desde cero (3 screens, 2 ViewModels, AndroidManifest).
- `AppDatabase` bumpeada a versión 4. Nuevas entidades: `random_tables`, `table_entries`, `table_roll_results`.
- `RandomTable`, `TableEntry`, `TableRollResult`, `TableRollMode` añadidos a `:core:model`.
- `TableRepository` (interface) en `:core:domain`. `TableRepositoryImpl` en `:core:data`.
- `RollTableUseCase` en `:core:domain` — evaluación de dados, resolución de texto inline, sub-tablas 1 nivel.
- `DataModule` actualizado: `RandomTableDao`, `TableRollResultDao`, binding de `TableRepository`.

**FEATURE — RollTableUseCase**
- Regex de fórmulas de dado: `(\d+)d(\d+)([+\-]\d+)?` — soporta `1d6`, `2d8+3`, `1d100`.
- Dados inline en texto: `[1d4+1]` → evaluado y embebido en el resultado final.
- Sub-tabla `@NombreTabla` → lookup por nombre + tirada recursiva (máx. 5 niveles).
- Modos RANGE (por rango) y WEIGHTED (por peso relativo).
- Persiste cada `TableRollResult` en Room antes de retornar.

**FEATURE — TablesTab (tab 1 de SessionScreen)**
- Lista filtrable por categoría (chips) y búsqueda de texto.
- Botón 🎲 en cada item → tirada rápida sin abrir BottomSheet.
- Tap en item → `TableDetailSheet` con resultado animado + historial de los últimos 5 tiros en la sesión.
- FAB para crear nueva tabla (navega a `TableEditorScreen`).

**FEATURE — TableDetailSheet**
- `ModalBottomSheet` con dado animado (valor en cuadrado primary), resultado resuelto.
- Botones "Tirar de nuevo" / "Cerrar".
- Historial por sesión: row compacto con valor de dado, texto y hora.

**FEATURE — TableEditorScreen + TableEditorViewModel**
- Crear y editar tablas: nombre, categoría, descripción, fórmula, modo (RANGE/WEIGHTED).
- Lista editable de entradas con campos min/max (RANGE) o peso (WEIGHTED) + texto.
- Vista previa de tirada desde el editor — guarda temporalmente y tira.
- FAB "Guardar" — valida nombre no vacío antes de persistir.

**INTEGRATION — Bundled tables**
- 9 tablas predefinidas en `app/src/main/assets/tables/bundled_tables.json`.
- `TablesViewModel` las carga al primer lanzamiento si `countBuiltInTables() == 0`.
- Tablas: Encuentros Bosque/Mazmorra, Nombres (humanos M/F + élfico), Clima, Botín Goblin, Crítico/Pifia.

**INTEGRATION — SessionScreen + NavGraph**
- `SessionTabPlaceholder(label = "Tablas")` reemplazado por `TablesTab(sessionId, onCreateTable)`.
- Parámetro `onCreateTable: () -> Unit = {}` añadido a `SessionScreen`.
- Rutas `TablesListRoute` y `TableEditorRoute(tableId: Long = -1L)` añadidas a `Screen.kt` y `NavGraph.kt`.
- `:feature:draw` añadido como dependencia de `:feature:tables` en `build.gradle.kts`.
- **BUILD SUCCESSFUL** en 3m 33s — 407 tareas, sin errores.

---

### Sprint 4 — Visuales: CardAspectRatio, ContentScale.Fit, dots y badge (14 de abril de 2026)

**ARCHITECTURE — Sistema de proporciones de carta (CardAspectRatio)**
- Nuevo enum `CardAspectRatio` en `:core:model`: STANDARD / TAROT / MINI / SQUARE / LANDSCAPE.
- `CardStack.aspectRatio` añadido con default STANDARD. Columna `aspectRatio` en `CardStackEntity`.
- `AppDatabase` bumpeada a versión 3. Mappers actualizados con `runCatching` para migración segura.

**FEATURE — CardThumbnail rediseñado**
- `ContentScale.Fit` reemplaza `Crop` — imagen completa visible, sin recorte.
- Fondo `surfaceVariant` como letterbox cuando la imagen no ocupa todo el espacio.
- Parámetro `aspectRatio: CardAspectRatio` controla el ancho proporcionalmente al alto.
- Dots de caras: `Row` de círculos en borde inferior cuando `card.faces.size > 1`.
  Cara activa = punto grande primary, otras = puntos pequeños semitransparentes.
- Parámetro `showFaceDots: Boolean = true`.

**FEATURE — Badge de conteo en Biblioteca**
- `getTotalCardCount(stackId): Flow<Int>` añadido en `CardDao`, `CardRepository`, `CardRepositoryImpl`.
- `LibraryUiState.deckCardCounts: Map<Long, Int>` — flow combinado de counts por mazo.
- `DeckCoverCard` recibe `cardCount: Int` y muestra `BadgedBox` en la portada.
- `LibraryScreen` pasa `deckCardCounts[deck.id]` a cada `DeckCoverCard`.

**INTEGRATION — DeckDetailScreen + SessionScreen**
- `DeckDetailScreen`: `CardThumbnail` recibe `deck.aspectRatio`.
- `SessionViewModel`: `deckAspectRatios: Map<Long, CardAspectRatio>` cargado del combine existente.
- `SessionScreen`: `SwipeToDiscardCard` recibe `aspectRatio` → pasa a `CardThumbnail`.
- **BUILD SUCCESSFUL** en 48s — sin errores.

---

### SessionScreen refactorizada a workspace multi-tab (14 de abril de 2026)

**ARCHITECTURE — HorizontalPager + TabRow en SessionScreen**
- `SessionScreen` pasa de pantalla monolítica a workspace con `HorizontalPager` de 3 tabs:
  - Tab 0 — Mazos: todo el comportamiento anterior preservado (mano, DeckBar, PileTray)
  - Tab 1 — Tablas: placeholder (`SessionTabPlaceholder`) → se rellena en Sprint 5
  - Tab 2 — Notas: placeholder → Sprint 6
  - Tab 3 — Combate: aparece solo cuando `hasActiveEncounter = true` → Sprint 7
- `MazosTab` extraído como composable privado con la mano, DeckBar y PileTray.
- `SessionFab` extraído como función privada — FAB contextual por tab (ROBAR / TIRAR / NOTA).
- Night Mode overlay elevado a nivel de `Box` raíz — cubre todos los tabs uniformemente.
- `activeTab: Int` y `hasActiveEncounter: Boolean` añadidos a `SessionUiState`.
- `setActiveTab()`, `rollActiveTable()` (stub) y `startQuickNote()` (stub) en `SessionViewModel`.
- Sincronización bidireccional `PagerState ↔ ViewModel` via `LaunchedEffect`.
- **BUILD SUCCESSFUL** — sin errores de compilación, solo warning de `ExperimentalCoroutinesApi` preexistente.

---

### Inicio del Proyecto — Planificación Fundacional (13 de abril de 2026)

**DECISION — Arranque del proyecto**
- App Android nativa para gestión de mazos de cartas TTRPG, orientada a DMs.
- Carpeta base del proyecto: `I:\TTRPG\Visuales\Decks\Deckapp`
- UI en español.
- Vision a largo plazo: suite completa de herramientas para DMs. Fase 1 = mazos.
- Referencia funcional: Foundry VTT Card System (v9+) + módulos Monarch, CCM, Card Hand Mini Toolbar.

**ARCHITECTURE — Stack y estructura de módulos**
- Arquitectura: Multi-módulo Gradle + Clean Architecture (UseCases), igual que TDAPP.
- Módulos: `:core:model`, `:core:domain`, `:core:data`, `:core:ui`,
  `:feature:library`, `:feature:deck`, `:feature:draw`, `:feature:import`, `:feature:session`.
- DI: Hilt. UI: Jetpack Compose + Material 3. DB: Room.
- Event log (`DrawEvent`, append-only) como fuente de verdad de sesión.
  Habilita undo, historial y crash recovery sin código adicional.
- Session-scoped draw state: la sesión tiene copia de trabajo; el deck en biblioteca siempre está limpio.

**DECISION — Modelo de contenido de carta (CardContentMode)**
- Investigación de productos del mercado TTRPG identificó 8 patrones estructurales:
  `IMAGE_ONLY`, `IMAGE_WITH_TEXT`, `REVERSIBLE` (Tarot/Oracle), `TOP_BOTTOM_SPLIT` (Gloomhaven),
  `LEFT_RIGHT_SPLIT`, `FOUR_EDGE_CUES` (Story Engine Deck), `FOUR_QUADRANT`, `DOUBLE_SIDED_FULL` (Arkham Horror LCG).
- Cada mazo tiene `defaultContentMode`; cada carta puede sobreescribir el suyo.
- `CardFace.zones: List<ContentZone>` — número e interpretación de zonas depende del modo.
- `Card.currentRotation: Int` (0/90/180/270) controla qué zona es activa en modos orientación-dependiente.

**DECISION — Import desde PDF**
- Librería PDF: `barteksc/PdfiumAndroid` (wrapper de PDFium, motor de Chrome).
  Más robusto que el `PdfRenderer` nativo de Android.
- Recorte: Kotlin puro + `Bitmap.createBitmap()` para los 4 modos de layout (sin OpenCV en Fase 1).
- 4 modos de layout soportados:
  - Modo A: páginas alternas (frente / dorso / frente / dorso...)
  - Modo B: lado a lado en la misma página
  - Modo C: grilla N×M por página (print-and-play estándar)
  - Modo D: primera mitad PDF = frentes, segunda = dorsos
- UI: preview interactivo con grilla overlay antes de confirmar import.
- Procesamiento en background (WorkManager) — sobrevive a bloqueo de pantalla.
- OpenCV (`zynkware/Document-Scanning-Android-SDK`) reservado para Fase 2: auto-detección de grilla.

**DECISION — Almacenamiento de imágenes**
- Usar SAF (Storage Access Framework) con `takePersistableUriPermission` para acceso a carpetas.
- Imágenes copiadas a almacenamiento interno de la app en el momento del import.
- No guardar paths absolutos externos (se rompen con cambios de SD card o permisos del OS).

**DECISION — Assets de demo**
- `I:\TTRPG\Visuales\Decks\The Ultimate Trove - Assets (Decks)\` (~4,059 archivos, 160 carpetas)
  usado como biblioteca inicial de mazos demo.
- Contiene: Paul Weber, Nord Games, Deck of Many, Tarokka, Pathfinder, Deck of Illusions.

---

---

### Scaffold completo + Primer build exitoso (14 de abril de 2026)

**ARCHITECTURE — Scaffold de todos los módulos**
- Creados todos los feature modules: `:feature:deck`, `:feature:draw`, `:feature:import`, `:feature:session`.
- Cada módulo tiene `build.gradle.kts`, Screen(s), ViewModel(s) iniciales.
- `SessionScreen` implementa wake lock vía `DisposableEffect` + `FLAG_KEEP_SCREEN_ON`.
- `ImportScreen` implementa flujo de 4 fases con preview de PDF (PdfiumAndroid).
- `CardViewScreen` adapta rendering según `CardContentMode` (reversible, FOUR_EDGE_CUES, flip).
- `CardEditorScreen` tiene selector de modo + editores de zonas dinámicos por modo.

**SOLUTION — Errores de build resueltos en iteración**
- `UndoLastActionUseCase`: sintaxis `kotlinx.coroutines.flow.first(it)` → `.first()` con import correcto.
- `android:Theme.Material.NoTitleBar` no existe → corregido a `android:Theme.Material.NoActionBar`.
- PdfiumAndroid v1.9.0 trae `com.android.support:support-compat:26.1.0` que conflictuaba con `androidx.core` → añadido `exclude(group = "com.android.support")`.
- Recursos `mipmap/ic_launcher` faltaban → creados como adaptive icons XML (válidos desde minSdk 26).
- NavGraph pasaba parámetros directos a screens que usan SavedStateHandle → eliminados.
- `DrawCardUseCase.invoke` parámetro `drawMode` llamado como `mode` en SessionViewModel → corregido.
- `SessionViewModel` llamaba `getDrawnCardsForSession()` inexistente → añadido `getDrawnCards()` a CardRepository.
- `ImportDeckUseCase` no existía → creado stub en `:core:domain` con `Result<Long>` como retorno.
- `onCardClick` en SessionScreen era `(Long, Long) -> Unit` pero NavGraph pasaba `(Long) -> Unit` → corregido.

**DECISION — Workflow de desarrollo**
- Ejecutar `:app:assembleDebug` al final de cada sprint para detectar errores antes de la siguiente iteración.

---

---

### Import desde carpeta SAF — Implementación real (14 de abril de 2026)

**SOLUTION — Import de carpeta funcional end-to-end**
- `FileRepository` añadido a `:core:domain` — interfaz para operaciones de archivo (lista imágenes SAF, copia a storage interno, elimina imágenes de mazo).
- `FileRepositoryImpl` en `:core:data` — usa `DocumentsContract` para listar archivos en árbol SAF, `ContentResolver.openInputStream` para copiar. Almacena en `{filesDir}/decks/{deckId}/{fileName}`.
- `FilenameParser` en `:core:domain:util` — parsea `NNN_titulo[_palo].ext` → `CardMetadata(title, value?, suit?)`. Fase 1: el título no puede contener guiones bajos (son separadores de palo).
- `ImportDeckUseCase` actualizado: crea el `CardStack`, procesa la carpeta, crea un `Card + CardFace` por imagen, actualiza la portada del mazo con la primera imagen.
- `DataModule` actualizado: binding `FileRepository → FileRepositoryImpl`.

**ARCHITECTURE — Separación de responsabilidades en import**
- `:core:domain` no tiene `Context` — `FileRepository` es la abstracción limpia.
- `:feature:import` sigue manejando: SAF picker, preview PDF, progreso de UI.
- `:core:domain` maneja: lógica de negocio (qué se importa, cómo se modela).
- `:core:data` maneja: I/O de archivos, queries de Room.
- **PDF import**: queda como TODO Fase 2. `ImportDeckUseCase` reconoce `source="PDF"` pero no procesa. El rendering (PdfiumAndroid) y el recorte (Bitmap.createBitmap) se implementarán en Fase 2.

**DECISION — Convención de nombre de archivo**
- `NNN_titulo[_palo].ext` — Fase 1: sin guiones bajos en el título.
- Ejemplos: `047_dragon.png` → value=47, title="Dragon"; `047_dragon_fuego.png` → suit="Fuego".
- Limitación conocida: títulos con múltiples palabras (`magic_missile`) se interpretan como titulo + palo.

---

### Library, DeckDetail, CardView — Coil file path + Gradle wrapper (14 de abril de 2026)

**SOLUTION — Imágenes internas cargando correctamente con Coil**
- `AsyncImage(model = stringPath)` no funcionaba para rutas absolutas internas (`/data/data/.../files/decks/...`): Coil interpreta los strings como URLs.
- Fix: `AsyncImage(model = File(path))` en `DeckCoverCard`, `CardThumbnail`, `CardViewScreen` y `DeckDetailScreen`.
- Para evitar el warning de smart cast nullable en propiedades de data class, se asigna a una `val` local antes del `null check`.

**SOLUTION — Gradle wrapper funcional desde terminal**
- El proyecto no tenía `gradlew`/`gradlew.bat` ni `gradle-wrapper.jar` (no se habían generado en el scaffold manual).
- Se copió `gradle-wrapper.jar` del proyecto `audio_book_player` (compatible — el jar bootstrapper es agnóstico a la versión; descarga Gradle 8.13 según `gradle-wrapper.properties`).
- Se copiaron los scripts `gradlew` y `gradlew.bat` del mismo proyecto.
- Resultado: `./gradlew :app:assembleDebug` funciona desde la terminal del proyecto.

---

### Fase 1 completa — Loop de juego funcional (14 de abril de 2026)

**SOLUTION — Pile de descarte reactiva**
- Nuevo query en `CardDao.getPiledCards(sessionId)`: cartas con evento DISCARD en esta sesión, después del último RESET, que actualmente tienen `isDrawn = 0`. Query SQL reactivo (Flow) — la pila se actualiza automáticamente.
- `CardRepository` e impl actualizados con `getPiledCards`.
- `SessionViewModel` ahora pobla `uiState.pile` desde este Flow.

**SOLUTION — Reset de mazo**
- `SessionViewModel.resetDeck()`: descarta la mano, llama `cardRepository.resetDeck(stackId)`, registra `DrawAction.RESET` en el event log. El evento RESET es el anchor temporal que limpia la pile en el query SQL.
- Agregado al DropdownMenu de SessionScreen.

**SOLUTION — SessionScreen mejorado**
- `SwipeToDiscardCard`: agrega `graphicsLayer { translationX = offsetX }` — la carta se desliza visualmente hacia la izquierda al hacer swipe. `clickable` para tap a CardViewScreen.
- Haptic fuerte al robar: `LaunchedEffect(uiState.hand.size)` detecta aumento en la mano y dispara `VibrationEffect.createOneShot(80, DEFAULT_AMPLITUDE)`.
- PileTray header completo es clickeable (mejor touch target).

**SOLUTION — CardViewScreen TOP_BOTTOM_SPLIT**
- Extraído `TopBottomZonePanel`: muestra zonas[0] y zonas[1] en dos cards lado a lado (Row), cada una con su etiqueta "Superior" / "Inferior".
- El zone panel ahora usa un `when(face.contentMode)` para despachar el renderizado correcto por modo. `TOP_BOTTOM_SPLIT` va a `TopBottomZonePanel`, el resto al panel genérico.

**Pendiente conocido — Warnings de Room**
- `tagId` en tablas cross-ref no tiene índice → full scan en updates de tags. Aceptable para Fase 1, agregar índices en Fase 2.
- `exportSchema = true` en `DeckAppDatabase` sin `room.schemaLocation` configurado → no genera el archivo JSON de schema. Resolver en Fase 2 aplicando el plugin `androidx.room`.

---

### Fase 2-A — Markwon + Modos de contenido completos (14 de abril de 2026)

**SOLUTION — Markdown rendering con Markwon**
- `MarkdownText` composable en `:core:ui` — wrapper de `AndroidView(TextView)` para Markwon.
- Respeta el color del tema (Material3 `onSurface`) y el tamaño de fuente del `TextStyle` recibido.
- Aplicado en todos los paneles de texto de `CardViewScreen`: `GenericZonePanel`, `TopBottomZonePanel`, `FourQuadrantZonePanel`.

**SOLUTION — FOUR_QUADRANT (Story Engine style)**
- `FourQuadrantOverlay`: overlay 2×2 con `VerticalDivider` + `HorizontalDivider` sobre la imagen de la carta.
- Tap en cuadrante → resalta con indicador de punto y fondo semitransparente.
- `FourQuadrantZonePanel`: muestra el texto del cuadrante seleccionado, con hint "Toca un cuadrante" cuando ninguno está activo.
- Índices: 0=NO, 1=NE, 2=SO, 3=SE.
- Estado local en Screen (`selectedQuadrant`): se resetea al cambiar de carta o cara vía `LaunchedEffect`.

**SOLUTION — DOUBLE_SIDED_FULL (Arkham Horror LCG style)**
- `AnimatedContent(targetState = card.currentFaceIndex)` en el área de imagen con slide horizontal.
- Dirección del slide sigue la lógica: cara siguiente → slide desde la derecha; cara anterior → desde la izquierda.
- El botón "Voltear carta" ya existía — ahora tiene animación.

**ARCHITECTURE — CardViewScreen refactorizada**
- Extraídos componentes privados: `CardFaceImage`, `FourQuadrantOverlay`, `QuadrantCell`, `FourQuadrantZonePanel`, `TopBottomZonePanel`, `GenericZonePanel`.
- El panel de zonas usa `when(face.contentMode)` limpio: `TOP_BOTTOM_SPLIT`, `FOUR_QUADRANT`, y `else` (todos los demás modos).

---

### Sesión multi-mazo — DeckBar + badge en mano (14 de abril de 2026)

**DECISION — Scope multi-mazo**
- La app es exclusivamente para el DM. No hay jugadores en la UI.
- Sistemas como Plot Deck o Tarokka requieren múltiples mazos activos simultáneamente.
- `SessionSetupScreen` ya soportaba multi-mazo en el modelo; el gap era la UI de juego.

**ARCHITECTURE — Selección de mazo activo**
- `SessionUiState` nuevos campos: `selectedDeckId: Long?`, `deckCardCounts: Map<Long, Int>`, `deckNames: Map<Long, String>`.
- `deckCardCounts` es reactivo: usa `flatMapLatest` sobre `getDecksForSession` → `combine` de `getAvailableCount(stackId)` por mazo → mapa stackId→count en tiempo real.
- `CardRepository.getAvailableCount(stackId): Flow<Int>` — nuevo método usando DAO ya existente.
- Auto-selección del primer mazo al cargar la sesión (si `selectedDeckId == null`).

**SOLUTION — DeckBar composable**
- Aparece solo si `deckRefs.size > 1` (sesiones de 1 mazo no lo necesitan).
- `LazyRow` de `FilterChip`s; cada chip muestra nombre + "N disponibles".
- El chip seleccionado se resalta con `primaryContainer`.
- Tap en chip → `viewModel.selectDeck(stackId)`.

**SOLUTION — FAB ROBAR multi-mazo**
- `drawCard()` usa `selectedDeckId` en lugar de `deckRefs.firstOrNull()`.
- Con múltiples mazos, el FAB muestra el nombre del mazo activo debajo de "ROBAR".
- `resetDeck()` ahora resetea **todos** los mazos de la sesión (no solo el primero).

**SOLUTION — Badge de mazo en cartas de la mano**
- En sesiones multi-mazo: `card.stackId` se cruza con `deckNames` para mostrar un badge.
- Overlay `Surface` con `primaryContainer.copy(alpha = 0.9f)` en la base de cada carta.
- En sesiones de 1 mazo: badge oculto (no agrega ruido visual innecesario).

**SOLUTION — TopBar dinámica**
- Con 1 mazo: subtítulo "N en mano" (comportamiento anterior).
- Con múltiples mazos: subtítulo "N en mano  ·  Mazo1 X  ·  Mazo2 Y  ···" (resumen rápido de disponibles).

---

### Tags + búsqueda/filtro en biblioteca (14 de abril de 2026)

**SOLUTION — Búsqueda por nombre en LibraryScreen**
- `LibraryUiState` migrado a `MutableStateFlow` con `searchQuery`, `selectedTagIds`, `allTags`.
- `filteredDecks` como propiedad derivada en el data class — filtrado por nombre (contiene, ignoreCase) y por tags (cualquier tag del mazo está en el set seleccionado).
- `OutlinedTextField` con ícono Search + botón Clear cuando hay texto.
- Estado "Sin resultados" con botón "Limpiar filtros".

**SOLUTION — Filtro por tags como chips en LibraryScreen**
- `LazyRow` de `FilterChip`s debajo del buscador, visible si hay tags.
- `toggleTagFilter(tagId)` actualiza `selectedTagIds: Set<Long>` con toggle on/off.
- Los chips solo aparecen cuando la biblioteca tiene al menos un tag creado.

**SOLUTION — Gestión de tags en DeckDetailScreen**
- Sección de tags con scroll horizontal (encima del hint y del HorizontalDivider).
- `InputChip` por cada tag con botón × (`combinedClickable(onClick = { removeTag(tagId) })`).
- `AssistChip` "+ Tag" abre `AddTagDialog` (AlertDialog con OutlinedTextField).
- `DeckDetailViewModel.addTag(name)`: crea el tag via `saveTag()`, lo añade al deck via `saveStack(deck.copy(tags = ...))`.
- `DeckDetailViewModel.removeTag(tagId)`: filtra tags y llama `saveStack(deck.copy(...))`.
- Tags duplicados ignorados (comparación insensible a mayúsculas).

**PROBLEM/SOLUTION — Crash al borrar mazo (FOREIGN KEY constraint failed)**
- `session_deck_refs.stackId` FK a `card_stacks` no tenía `ON DELETE CASCADE`.
- Al borrar un mazo referenciado en una sesión, SQLite lanzaba `SQLiteConstraintException`.
- Fix: `onDelete = ForeignKey.CASCADE` en `SessionDeckRefEntity.stackId`.
- DB version bumped 1 → 2 con `fallbackToDestructiveMigration()` (dev only).

---

### Borrado de mazos y cartas (14 de abril de 2026)

**DECISION — Features excluidas del scope DM-solo**
- Deal (repartir a jugadores), Pass (pasar carta a jugador), Peek multiusuario: fuera del roadmap.
- La app es exclusivamente para el DM. No hay modelo de jugador en Fase 1/2.

**SOLUTION — Borrado de mazos desde LibraryScreen**
- `DeckCoverCard` recibe `onDelete: (() -> Unit)? = null`. Cuando está presente, muestra botón ⋮ en la fila inferior del card que abre un `DropdownMenu` con "Eliminar mazo" en rojo.
- `LibraryScreen` gestiona `confirmDeleteDeck: CardStack?` como estado local. Cuando no es null, muestra un `AlertDialog` de confirmación.
- `LibraryViewModel.deleteDeck(id)`: primero borra imágenes internas via `fileRepository.deleteImagesForDeck(id)`, luego `cardRepository.deleteStack(id)`. Room elimina en cascada cartas, caras y cross-refs de tags.

**SOLUTION — Borrado de cartas desde DeckDetailScreen**
- `CardThumbnail` envuelto en `Modifier.combinedClickable(onClick=..., onLongClick=...)`.
- Long-press abre `AlertDialog` de confirmación con nombre de la carta.
- `DeckDetailViewModel.deleteCard()` ya existía — solo se conectó a la UI.
- Hint de instrucción visible bajo el contador de cartas: "Mantén presionada una carta para eliminarla".

---

### Fase 2-C — Undo completo + Night Mode (14 de abril de 2026)

**SOLUTION — Undo superficialmente completo**
- `UndoLastActionUseCase` ahora revierte también `DrawAction.DISCARD`: llama `updateCardDrawnState(cardId, isDrawn = true)`, devolviendo la carta a la mano.
- Antes solo revertía DRAW, FLIP, REVERSE, ROTATE. PASS y RESET siguen siendo no revertibles (demasiado estado involucrado).

**SOLUTION — canUndo reactivo en UI**
- `SessionUiState.canUndo: Boolean` derivado de observar `getEventsForSession(sessionId).map { it.isNotEmpty() }`.
- El botón Undo en `SessionScreen` usa `enabled = uiState.canUndo` — deshabilitado cuando no hay nada que deshacer.
- `SessionUiState.snackbarMessage: String?` muestra "Acción deshecha" o "Nada que deshacer" via `SnackbarHost`.

**SOLUTION — Night Mode overlay**
- `SessionUiState.nightMode: Boolean` + `toggleNightMode()` en `SessionViewModel`.
- Toggle desde el `DropdownMenu` de la TopBar con texto dinámico ("Modo nocturno" / "Desactivar modo nocturno").
- Overlay: `Box` con `Color.Black.copy(alpha = 0.55f)` sobre el área de la mano.
- Tap sobre el overlay desactiva el modo nocturno directamente (acceso rápido).
- El tray de descarte queda fuera del overlay (acceso siempre visible).

---

### Fase 2-B — CardEditorScreen completo (14 de abril de 2026)

**SOLUTION — CardEditorScreen funcional end-to-end**
- `CardEditorViewModel` actualizado: inyecta `FileRepository` para copiar imágenes a storage interno.
- Nuevo método `pickFaceImage(faceIndex, uri)`: copia la imagen seleccionada via `GetContent` a `{filesDir}/decks/{deckId}/face_{n}_{timestamp}.jpg`, actualiza `face.imagePath`.
- Nuevo método `updateFaceName(faceIndex, name)`: permite renombrar caras desde la UI.
- Nuevo método `removeFace(faceIndex)`: elimina la cara (guarda siempre mínimo 1) y ajusta `selectedFaceIndex`.
- `isPickingImage: Boolean` en `CardEditorUiState` — muestra `CircularProgressIndicator` en TopBar mientras se copia.

**ARCHITECTURE — Separación imagen vs estado**
- El picker de imagen (`rememberLauncherForActivityResult(GetContent)`) vive en la Screen.
- `pendingPickFaceIndex` como estado local temporal entre el lanzamiento del picker y su callback.
- La copia real de archivos ocurre en el ViewModel via `FileRepository` (no en la Screen).

**SOLUTION — FaceImagePicker composable**
- Si la cara tiene imagen: muestra `AsyncImage` 160dp de alto + dos FAB flotantes ("Cambiar" / "×" para quitar).
- Si no tiene imagen: `OutlinedButton` "Seleccionar imagen".
- `zoneLabelsFor(mode)` función pura extraída para devolver las etiquetas de zona según el modo.

**SOLUTION — Eliminar cara con botón × en tab**
- Botón `IconButton` (18dp) con `Icons.Default.Close` (14dp) integrado dentro del texto del `Tab`.
- Solo visible cuando `faces.size > 1`.

---

### Sprint 3 — Importación y Exportación Completa (ZIP) (14 de abril de 2026)

**SOLUTION — Importación desde ZIP**
- `FileRepository.unzipToTemp(zipUri)`: descomprime archivos en un directorio único dentro de `cacheDir`. Filtra solo extensiones de imagen soportadas.
- `ImportDeckUseCase` actualizado para manejar la fuente `ZIP`: descomprime y luego usa la lógica compartida de importación de imágenes.
- `ImportScreen` & `ImportViewModel`: nueva opción "Archivo ZIP" en la fase de selección.

**SOLUTION — Exportación a ZIP**
- `FileRepository.zipDeckDirectory(deckId, outputUri)`: empaqueta todas las imágenes del directorio interno del mazo en un archivo ZIP directamente en el URI de destino (SAF).
- `ExportDeckToZipUseCase` creado en `:core:domain`.
- `DeckDetailScreen`: ítem "Exportar ZIP" en el menú de opciones.
- `ActivityResultContracts.CreateDocument` integrado para que el usuario elija dónde guardar el archivo exportado.
- Feedback vía `SnackbarHost` para informar éxito o error en la exportación.

**ARCHITECTURE — Reutilización de lógica de importación**
- Refactorizada la lógica de importación de imágenes en `ImportDeckUseCase` hacia el método privado `importFromImageList`, permitiendo que tanto carpetas como archivos ZIP sigan el mismo flujo de parseo de metadatos y copia interna.

---

### Cierre de Sprint 3 — Build estable y Sincronización (14 de abril de 2026)

**INTEGRATION — Build exitoso de fin de sprint**
- Ejecutado `:app:assembleDebug` con éxito. Generado `app-debug.apk` funcional con todas las features de Fase 1 y Fase 2-A/B/C.
- Verificación de regresiones en import ZIP y export ZIP (ambos operativos).
- El event log (`DrawEvent`) persiste correctamente todas las acciones de la sesión multi-mazo.

**ARCHITECTURE — Sincronización de documentación**
- `PLANNING.md` y `DEVLOG.md` actualizados para reflejar el progreso real.
- Fase 1 marcada como 95% completada (solo resta el procesamiento real de PDF).
- Fase 2 marcada como 60% completada.
- Feature de Exportación ZIP adelantada de Fase 3 a Fase 2 (completada).

---

## 📋 Pendientes Iniciales

- [x] Crear proyecto Android Studio con estructura multi-módulo Gradle
- [x] Definir Room schema v1:
      `CardStackEntity`, `CardEntity`, `CardFaceEntity`, `ContentZoneEntity`,
      `DrawEventEntity`, `SessionEntity`, `TagEntity`
- [x] Agregar dependencia `PdfiumAndroid` a `:feature:import`
- [x] Implementar `ImportDeckFromFolderUseCase` en `:core:domain`
- [ ] Implementar `ImportDeckFromPdfUseCase` con los 4 modos de layout
- [x] Smoke test: `LibraryScreen` con mazo hardcodeado
- [x] Verificar SAF + `takePersistableUriPermission` en Android 11+
      (los assets están en almacenamiento externo — requiere folder picker)
- [x] Definir convención de auto-detect desde filename:
      propuesta: `NNN_nombre[_palo].ext` → value=NNN, name="Nombre", suit="Palo"
- [x] Evaluar si Markwon funciona dentro de Compose (Fase 2 pero elegir librería ahora)
- [x] Definir nombre definitivo de la app
- [x] Decidir target API mínima (propuesta: API 26 / Android 8.0)
- [x] **Sprint 6: Notas de DM e Historial de Sesión (2026-04-14)**
    - [x] Implementado Tab de Notas en SessionScreen con auto-guardado y preview Markdown.
    - [x] Implementado `SessionHistoryScreen` con timeline vertical y estadísticas.
    - [x] Añadido timer de sesión en el TopBar de la sesión activa.
    - [x] Migración Room v4 → v5 con campo `dmNotes` en `SessionEntity`.
    - [x] Añadido `DrawAction.PEEK` para registrar revisiones del tope del mazo.
- [x] **Sprint 7: Configuración de Mazos (2026-04-14)**
    - [x] Implementado `DeckConfigSheet` (ModalBottomSheet) para configurar mazos.
    - [x] Añadido soporte para **Robo Boca Abajo** (B-2): Las cartas se inicializan en la cara de dorso al ser robadas.
    - [x] Añadido soporte para **Imagen de Dorso Global** (B-3).
    - [x] Añadida configuración de **Aspect Ratio** y **Draw Mode** desde la UI del mazo.
    - [x] Migración Room v5 → v6 con campos `backImagePath` y `drawFaceDown` en `CardStackEntity`.

---

## 💡 Decisiones de Diseño Anotadas

### Por qué Event Log en lugar de estado mutable
Room guarda el estado actual de las cartas, pero el historial de la sesión se construye
sobre un event log append-only (`DrawEvent`). Ventajas: undo gratis, historial completo,
crash recovery sin lógica adicional. El estado actual es una proyección del log.

### Por qué PdfiumAndroid y no PdfRenderer nativo
`android.graphics.pdf.PdfRenderer` (API 21+) tiene limitaciones severas: solo ARGB_8888,
no soporta PDFs cifrados ni anotaciones, rendimiento limitado en páginas grandes.
`PdfiumAndroid` usa el mismo motor que Chrome (PDFium) y es mucho más robusto con
los PDFs comerciales de mazos TTRPG (Nord Games, Paul Weber, etc. a veces usan features
avanzadas de PDF).

### Por qué Kotlin puro para el recorte (sin OpenCV en Fase 1)
El recorte de cartas en los 4 modos de PDF import es matemático: se divide la página en
rectángulos iguales usando las dimensiones conocidas. No requiere detección de bordes.
Esto mantiene el APK ~40MB más liviano. OpenCV se agrega en Fase 2 solo para el feature
opcional de auto-detección cuando el usuario no conoce el layout del PDF.

## Sprint 12: Gestión Avanzada de Mazos y Mejoras de OCR (2026-04-15)

### Objetivos Completados
- [x] **Duplicar Mazo:** Implementada la clonación completa de mazos, incluyendo la copia física de las imágenes en el almacenamiento interno para evitar dependencias entre copias.
- [x] **Fusionar Mazos:** Capacidad de migrar cartas de un mazo origen a uno destino, remapeando automáticamente las rutas de archivos de imagen.
- [x] **Mejora de Heurística OCR:** Corregido el error de "fusión de filas". El sistema ahora detecta rangos incluso si están concatenados con el texto descriptivo y separa los bloques automáticamente.
- [x] **Estabilidad:** Verificado con build completo (`BUILD SUCCESSFUL`).

### Cambios Técnicos
- **DuplicateDeckUseCase:** Ahora retorna el ID del nuevo mazo para permitir navegación inmediata después de la copia.
- **RangeParser:** Refactorizado para devolver la longitud consumida del string, permitiendo a `AnalyzeTableImageUseCase` dividir bloques de OCR que contienen múltiples datos (rango + descripción).
- **Tolerancia Vertical:** Se aumentó un 15% (de 0.6 a 0.7) en la agrupación de líneas para manejar mejor escaneos de manuales con tipografía variable.

## Sprint 13: Archivado y Restauración de Mazos (2026-04-15)

### Objetivos Completados
- [x] **Visualización de Archivo:** Los mazos archivados ahora muestran un filtro de escala de grises y un icono de "Caja" en el centro de la portada para una distinción inmediata.
- [x] **Modo Solo Lectura:** Al entrar en el detalle de un mazo archivado, se deshabilitan las funciones de edición (Añadir cartas, Tags, Configuración, Duplicación y Fusión) para preservar su estado.
- [x] **Gestión de Sesión:** Deshabilitado el botón de "Añadir a sesión" para mazos archivados desde la biblioteca.
- [x] **Estabilidad:** Build verificado y sin errores.

### Cambios Técnicos
- **DeckCoverCard:** Integración de `ColorFilter.colorMatrix` para el efecto de desactivación visual sin perder la miniatura.
- **DeckDetailScreen:** Implementación de lógica condicional sobre `uiState.deck.isArchived` para restringir el acceso a componentes mutables.
- **LibraryViewModel:** Centralización del estado de visibilidad (Activos vs Archivados) desacoplado de la lógica de persistencia.

---

### Sprint 14: OCR Pro - Revisión Dual y Sensibilidad (15 de abril de 2026)

**FEATURE — Interfaz de Revisión Dual (OCR)**
- Implementado el componente `ZoomableImage` con soporte para gestos (Zoom/Pan/Reset) para previsualización interactiva.
- Rediseño de `ReviewStep` en `TableImportScreen` con layout vertical dividido para validación directa visual vs datos.
- Persistencia de `croppedBitmap` en el `TableImportUiState` para asegurar que la referencia visual se mantenga durante todo el flujo.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — Verificada la integración del nuevo visor interactivo.

---

### Sprint 15: Tablas Recursivas (Sub-tablas) (15 de abril de 2026)

**ARCHITECTURE — Motor de Tirada Recursivo**
- Actualizado `RollTableUseCase` para soportar `subTableId` con recursividad real (hasta 5 niveles).
- Implementada lógica de concatenación de resultados (`Resultado A → Resultado B`) preservando el flujo de sesión.
- Corrección de *Smart Casts* multi-módulo mediante captura en variables locales.

**FEATURE — Table Picker & Editor UX**
- Implementado `TablePickerDialog` en el editor para vincular entradas a cualquier tabla de la biblioteca.
- Nuevo diseño de `EntryRow` con botones de Enlace/Desenlace e indicadores visuales de recursividad.
- Cargada toda la biblioteca de tablas en `TableEditorViewModel` permitiendo búsquedas rápidas durante la vinculación.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — Todas las referencias circulares y de sintaxis resueltas.

---

### Sprint 18: Estabilidad del Mapa e Integridad de Datos (19 de abril de 2026)

**FIX — Crash de Renderizado en HexGrid (maxWidth < 0)**
- Refactorizado `drawTerrainLabel` en `HexGridCanvas.kt` para usar `TextLayoutResult.multiParagraph.paint` directamente con `canvas.translate`.
- Esta solución evita que `DrawScope.drawText` lance una `IllegalArgumentException` cuando las etiquetas están parcialmente fuera de los límites del canvas durante el desplazamiento o zoom.

**FIX — Integridad de Base de Datos (Identity Hash Mismatch)**
- Sincronizadas las entidades Room con las migraciones SQL mediante la adición selectiva de `@ColumnInfo(defaultValue = "...")`.
- Identificados y corregidos campos con valores por defecto inconsistentes entre recreaciones de tablas (especialmente `isPinned`, `confidence` y campos de `hex_maps`).
- Incrementada versión de DB a **33** con una migración robusta (`MIGRATION_32_33`) que verifica y añade columnas faltantes de forma segura mediante `try-catch`.
- Activado `fallbackToDestructiveMigration()` global como salvaguarda final para entornos de desarrollo.

**FEATURE — Mejoras en Modo Simplificado y Aislamiento**
- Corregido bug de inicialización en `SessionViewModel`: el modo simplificado ahora se activa correctamente según las preferencias del usuario.
- **Sesiones desde Cero:** Implementado reset automático de mazos al crear nuevas sesiones en `SessionSetupViewModel`.
- **Aislamiento de Hexcrawl:** El mapa de exploración ahora filtra las cartas "en mano" basándose estrictamente en los eventos de la sesión actual, evitando que aparezcan cartas robadas en otras sesiones.

**INTEGRATION — Build y Estabilidad**
- **BUILD SUCCESSFUL** — APK generado y verificado (v33). App estable en navegación y gestión de recursos.

---

### Sprint 19: Rotación Física Dinámica y Selección de Pistas en Cartas Multi-Opción (19 de octubre de 2026)

**PROBLEM — Cartas de Engine Deck / Deck of Worlds no rotaban**
- Al robar cartas multi-opción (`FOUR_EDGE_CUES`, `TOP_BOTTOM_SPLIT`, `REVERSIBLE`), la rotación inicial siempre se inicializaba en 0° fija.
- En la interfaz gráfica (`CardItem`, `CardThumbnail`, `CardViewScreen`), los componentes `AsyncImage` no aplicaban transformación de rotación física en eje Z (`rotationZ`), por lo que visualmente la carta permanecía inmutable aun cambiando la opción en base de datos.
- Falta de identificadores claros (badges/chips) de la opción o punto cardinal activo en la mano y mesa, y ausencia de control de giro rápido de 1 toque.

**SOLUTION — Robo con Orientación Aleatoria**
- Modificado `DrawCardUseCase`: al robar una carta con `FOUR_EDGE_CUES`, se elige al azar entre 0°, 90°, 180° y 270°; para `TOP_BOTTOM_SPLIT` y `REVERSIBLE`, se elige al azar entre 0° y 180°. Se persiste atómicamente en Room via `UpdateCardStateUseCase(rotation = randomRotation)`.

**SOLUTION — Renderizado y Animación de Rotación Física**
- `CardItem`: implementada rotación animada suave con `animateFloatAsState` y `graphicsLayer { rotationZ = rotationAngle }`. Añadido badge de orientación activa (`N`, `E`, `S`, `O` o `1`, `2`) y botón de giro rápido de 1 toque (`Icons.Default.Refresh`) directamente sobre la carta en mano y mesa.
- `CardThumbnail`: agregada rotación Z y mini-chip en grillas y listas.
- `CardViewScreen`: animada rotación Z de imagen completa con `animateFloatAsState`, chip flotante en la parte superior con orientación y texto de pista, y selector/tirada con dados (d2/d4) que orienta la carta manteniendo visible la ilustración.
- `SessionDeckDelegate` y `SessionViewModel`: implementada función `rotateCard(cardId)` que cicla la orientación según el `contentMode` y emite evento `DrawAction.ROTATE`.
- Conectado el pipeline de eventos en `BoardCentralTool`, `SessionBoardTab`, `SessionBoardPagerContent`, `BoardItemUiContext`, `BoardBlockCard`, `BoardBlockCardAux`, `DeckBlockContent` y `DeckWorkspace`.

**INTEGRATION — Build, Paridad y Dispositivo**
- Sincronizado `MicrosettingDelegate` con `cardRepository.updateCardRotation` para mantener consistencia entre cluster y visor.
- Pruebas unitarias de `:core:domain`, `:feature:draw` y `:feature:deck` ejecutadas con éxito.
- Compilado APK Release (`app-release.apk`) e instalado exitosamente en el dispositivo físico Motorola One Fusion (`ZE222B5GXX`).
- Actualizada documentación de paridad de escritorio en `docs/planning/DESKTOP_PARITY_STORY_ENGINE.md`.

### Selector de recursos de la mesa (21 de octubre de 2026)

- Reorganizado el selector de bloques con búsqueda visible, filtros por tipo en una sola fila y resultados desplazables desde la primera vista.
- Separadas las tablas de la sesión y la biblioteca; se abre en biblioteca para permitir buscar todos los recursos, con las tablas de sesión destacadas.
- Movidas las acciones de creación a un menú compacto; la selección múltiple de tablas mantiene su confirmación fija en la parte inferior.
- Añadido filtro de mazos y acciones explícitas para añadir otros bloques. Corregidos textos visibles del selector.
- Compilación de `:feature:draw:compileDebugKotlin --offline`: correcta.

### Selector de recursos para colecciones grandes (22 de octubre de 2026)

- Incorporados accesos a favoritos y recientes. Los favoritos y los bloques agregados recientemente se guardan localmente; las tiradas recientes también alimentan la vista de tablas recientes.
- Incorporado filtro de colección para tablas y de etiqueta para mazos, usando los metadatos existentes. La lista muestra el total de resultados del tipo elegido y prioriza favoritos y tablas de la sesión.
- Agregada selección múltiple mixta de tablas y mazos con una única barra de confirmación fija. Los mazos ya presentes en la vista se mantienen fuera de los resultados.
- Compilación de `:feature:draw:compileDebugKotlin --offline`: correcta.

### Revisión en dispositivo del selector (22 de octubre de 2026)

- Instalado el APK de depuración en Motorola One Fusion y revisado el selector con la sesión Deckofworlds y 258 recursos disponibles.
- La revisión en pantalla mostró que los filtros de tipo y los accesos Favoritos/Recientes ocupaban demasiado alto. Se compactaron en una sola fila y el alcance/categoría/colección de tablas ahora comparte una fila horizontal desplazable.
- Reinstalación final y apertura de selector verificadas en el teléfono. Para empaquetar se reutilizó la salida compilada de Ajustes porque su recompilación normal falla con referencias no resueltas en `SettingsScreen.kt`.

### Contexto y escala del selector de recursos (22 de octubre de 2026)

- El selector recuerda búsqueda, filtros, alcance de tablas y criterio de orden entre aperturas.
- Añadidos criterios de orden por nombre, recientes y favoritos, combinables con tipo, categoría, colección y búsqueda.
- Los bloques que ya están en la mesa siguen visibles con su vista y carpeta; “Ver” navega a su ubicación. Si hay varias ubicaciones, se indica cuántas.
- Los mazos muestran cuántas cartas quedan disponibles. Los resultados se muestran en tandas de 50 con carga progresiva y el texto de búsqueda se prepara al crear cada entrada.
- Compilación de `:feature:draw:compileDebugKotlin --offline`: correcta.
- Agregado un control para marcar o desmarcar de una vez los recursos seleccionables que están visibles en la lista; excluye los que ya están ubicados en la mesa.
- Añadidos filtros guardados con nombre, etiquetas locales editables, previsualización de tablas/mazos y resultados de ejemplo de tablas.
- La selección múltiple puede abarcar todos los resultados filtrados, marcar recursos como favoritos y elegir la carpeta de destino. Los bloques ya colocados pueden seleccionarse para moverlos a una carpeta, marcarlos favoritos o quitarlos de la vista sin borrar el recurso.
- `SessionViewModel` ahora transmite el grupo destino al delegado del tablero para que las altas en lote respeten la carpeta elegida.
- Compilación de `:feature:draw:compileDebugKotlin --offline` correcta e instalación de depuración completada en Motorola One Fusion.
### Búsqueda y filtros de la biblioteca de NPCs (22 de octubre de 2026)

**Categoría:** `FEATURE / UX / NPCS / SEARCH`

- Se conectó el botón de búsqueda de la biblioteca de NPCs a un campo por nombre y descripción.
- Se agregaron filtros por NPC/monstruo y etiquetas detectadas en los registros existentes, con chips desplazables horizontalmente para pantallas pequeñas.
- Se añadió un estado sin coincidencias con acción para limpiar búsqueda y filtros.
- Verificación: `:feature:npcs:compileDebugKotlin` compiló correctamente.

### Búsqueda global de cartas e importación por lotes de PDF (22 de octubre de 2026)

**Categoría:** `FEATURE / SEARCH / FTS / IMPORT / BATCH`

- El buscador global ahora encuentra cartas por título, notas o texto de caras usando el índice FTS existente y permite abrir la carta encontrada.
- La consulta se cancela y reinicia con debounce al seguir escribiendo; se limpian caracteres de sintaxis FTS y se limita la lista a 100 resultados.
- Se añadió selección múltiple de PDFs. La vista previa usa el primero y la configuración se comparte para la cola; cada PDF genera un mazo, se procesa en secuencia y limita el rango al número de páginas disponible.
- Los errores de importación se presentan por archivo y el resumen informa los mazos/cartas creados.
- Verificación: `:app:compileDebugKotlin --configuration-cache --max-workers=2` completó correctamente. No se probó manualmente la selección múltiple con archivos reales en un dispositivo.
