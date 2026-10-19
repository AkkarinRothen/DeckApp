# Paridad TDAPP-Desktop: The Story Engine — Deck of Worlds & Microsettings

En cumplimiento del **Feature Parity Mandate** (`RULE[user_global]`), este documento detalla los elementos de datos, modelos de dominio, interfaces y lógica de mesa implementados en Android (TDAPP) para su réplica equivalente en la aplicación de escritorio (TDAPP-desktop).

---

## 1. Paquete de Inicio y Assets
- **Archivo JSON:** `core/data/src/main/assets/starter_packs/story_engine_deck_of_worlds.json`
- **Carpeta de Cartas:** `core/data/src/main/assets/starter_packs/deck_of_worlds/cards/*.webp` (246 archivos: 240 cartas + 6 reversos).
- **Sub-mazos (Stacks) Rebalanceados (240 cartas totales):**
  1. `101`: *Regiones (Region Cards)* — 32 cartas (`IMAGE_WITH_TEXT`, 1 opción centrada, Verde)
  2. `102`: *Puntos de Interés (Landmark Cards)* — 48 cartas (`TOP_BOTTOM_SPLIT`, 2 opciones, Marrón/Cobre)
  3. `103`: *Nombres y Apelativos (Namesake Cards)* — 48 cartas (`FOUR_EDGE_CUES`, 4 opciones, Ámbar)
  4. `104`: *Orígenes e Historia (Origin Cards)* — 32 cartas (`FOUR_EDGE_CUES`, 4 opciones, Púrpura)
  5. `105`: *Atributos y Cultura (Attribute Cards)* — 48 cartas (`FOUR_EDGE_CUES`, 4 opciones, Cerceta/Teal)
  6. `106`: *Acontecimientos y Ganchos (Advent Cards)* — 32 cartas (`TOP_BOTTOM_SPLIT`, 2 opciones, Magenta/Rosa)
- **Traducción y Zonas:** Todas las 240 cartas cuentan con sus 704 zonas traducidas al español en `zonesJson` y títulos temáticos enriquecidos en `story_engine_deck_of_worlds.json` y `cards_metadata.json`.

---

## 2. Modelo de Dominio (`Microsetting.kt`)
- `MicrosettingSlotType`: `REGION`, `LANDMARK`, `NAMESAKE`, `ORIGIN`, `ATTRIBUTE`, `ADVENT`.
- `MicrosettingCardItem(slotType, cardId, cardTitle, cardImagePath, rotationDegrees, activeCue, fallbackText)`.
- `MicrosettingCluster(id, title, cards: Map<MicrosettingSlotType, MicrosettingCardItem>, createdAt)`.
- Serializador/deserializador `MicrosettingCodec`:
  - `encode(cluster): String` -> JSON empaquetado en `SessionBoardItem.customData`.
  - `decode(customData): MicrosettingCluster`.
  - `formatAsNotes(cluster): String` -> genera el hook narrativo para notas / diario del director.

---

## 3. Elemento de Tablero de Sesión (`SessionBoardItemType.MICROSETTING`)
- Añadir el enum `MICROSETTING` a los tipos de ítems de sesión en el tablero.
- Renderizar un bloque interactivo con 6 cartas dispuestas en cuadrícula (2 columnas o 6 slots), cada una con su miniatura rotada según `rotationDegrees`, selector de pista activa y acciones para rotar 90° o redibujar carta individual.

---

## 4. Diálogo / Modal Generador (`MicrosettingBuilderDialog`)
- Robo aleatorio de 1 carta por cada sub-mazo instalado.
- Visualización interactiva con brújula de 4 direcciones para ciclar la pista activa de cada carta.
- Botón "Guardar en el Tablero" (crea un `SessionBoardItem` tipo `MICROSETTING`).
- Botón "Copiar / Enviar a Notas del DM" con síntesis de texto.

---

## 5. Bloques de Mazos en Mesa & Selector de Pistas/Tiradas
- **Visor de Opciones:** Selector en visor de carta para cartas tipo `TOP_BOTTOM_SPLIT` (2 opciones, tirada d2) y `FOUR_EDGE_CUES` (4 opciones cardinales, tirada d4).
- **Semántica de Barajado:** Botones separados en bloque de mazo para barajar cartas disponibles restantes (`shuffleDeckOnly`) versus recuperar descartes y barajar mazo completo.
- **Acceso a Visor:** Clic en cualquier carta de la mano en el bloque del tablero abre el visor de carta completo para inspección, rotación y toma de notas.
- **Diálogos de Confirmación:** Confirmaciones modales con recuento explícito para "Descartar mano" y "Reiniciar mazo".

---

## 6. Rotación Física y Selección de Pistas en Mano, Tablero y Visor
- **Convención de Orientación Activa (Borde Inferior):**
  - La pista activa siempre queda orientada en posición de lectura derecha en el **borde inferior** (Sur / Abajo).
  - `FOUR_EDGE_CUES`: 0° = Sur, 90° = Oeste, 180° = Norte, 270° = Este.
  - `TOP_BOTTOM_SPLIT`: 0° = Inferior, 180° = Superior.
  - `IMAGE_WITH_TEXT`: 0° = Centro.
- **Robo con Orientación Aleatoria (`DrawCardUseCase`):**
  - Al robar una carta con múltiples opciones (`FOUR_EDGE_CUES` -> [0°, 90°, 180°, 270°]; `TOP_BOTTOM_SPLIT` -> [0°, 180°]), se asigna una rotación aleatoria inicial y se persiste en `Card.currentRotation`.
- **Rotación Física en Render (`CardItem` & `CardThumbnail`):**
  - La imagen se rota físicamente mediante `graphicsLayer { rotationZ = rotationAngle }` con animación de muelle (spring).
  - Chip identificador de opción activa visible en mano y mesa (`S`, `O`, `N`, `E` o `1`, `2`).
  - Botón de rotación rápida de 1 toque sobre la carta para ciclar la orientación inmediatamente.
- **Pipeline de Mesa y Workspace:**
  - `SessionViewModel.rotateCard(cardId)` avanza 90° o 180° según el `contentMode` y emite evento `DrawAction.ROTATE`.
  - Conectado a través de `BoardCentralTool`, `SessionBoardTab`, `SessionBoardPagerContent`, `BoardItemUiContext`, `BoardBlockCard`, `BoardBlockCardAux`, `DeckBlockContent` y `DeckWorkspace`.
- **Visor Completo (`CardViewScreen`):**
  - Rotación 2D sobre eje Z animada con `animateFloatAsState`.
  - Chip flotante en la parte superior con orientación y texto de la pista activa en español.
  - Diálogo de selección y tirada de dados (d2/d4) que orienta y mantiene visible la ilustración de la carta.

---

## 7. Paquetes Maestros de Expansión (Deck of Worlds & New Story Engine)

En cumplimiento de la decisión de empaquetado del usuario, se crearon **2 paquetes maestros oficiales** (`FullBackupDto`):

### A. `story_engine_deck_of_worlds_expansions.json` (9 Mazos, 90 Cartas, 1 Regla Oficial)
- **Expansiones Cartográficas (`TOP_BOTTOM_SPLIT`, 2 zonas 0°/180°):**
  - `107`: *Ártico* (10 cartas, frente + reverso)
  - `108`: *Costas* (10 cartas, frente + reverso)
  - `109`: *Desiertos* (10 cartas, frente + reverso)
- **Expansiones Creativas (`FOUR_EDGE_CUES`, 4 zonas 0°/90°/180°/270°):**
  - `110`: *Ganchos de Aventura* (10 cartas, frente + reverso)
  - `111`: *Llaves de Cultura* (10 cartas, frente + reverso)
  - `112`: *Fragmentos de Lore* (10 cartas, frente + reverso)
- **Expansiones de Género (`TOP_BOTTOM_SPLIT`, 2 zonas 0°/180°):**
  - `113`: *Fantasía* (10 cartas, frente + reverso)
  - `114`: *Horror* (10 cartas, frente + reverso)
  - `115`: *Ciencia Ficción* (10 cartas, frente + reverso)
- **Regla de Mundo Incluida:** `101`: *Deck of Worlds: Guía de Expansiones Cartográficas y de Género*.

### B. `new_story_engine.json` (4 Mazos, 50 Cartas, 1 Regla Oficial)
- `201`: *Potenciadores (Boosters)* — 20 cartas (`TOP_BOTTOM_SPLIT`, 2 zonas, frente + reverso).
- `202`: *Trasfondos (Backstories)* — 10 cartas (`FOUR_EDGE_CUES`, 4 zonas, frente + reverso).
- `203`: *Misterios (Mystery)* — 10 cartas (`TOP_BOTTOM_SPLIT`, 2 zonas, frente + reverso).
- `204`: *Objetos Únicos (Unique Items)* — 10 cartas (`FOUR_EDGE_CUES`, 4 zonas, frente + reverso).
- **Regla de Narrativa Incluida:** `201`: *New Story Engine: Cómo Funciona el Motor de Historias*.

### Integración en UI y Paridad:
- Los archivos `.json` se ubican en `assets/starter_packs/` para descubrimiento automático vía `AssetManager.list("starter_packs")`.
- Se registraron los nombres amigables en `TablesViewModel`, `SessionViewModel`, `ReferenceTabViewModel`, `ReferenceListViewModel` y `TablesTab`.
- Preservación de cartas de usuario en `ReferenceRepositoryImpl`: las re-importaciones preservan cartas en mano/sesiones basadas en `imagePath`.

