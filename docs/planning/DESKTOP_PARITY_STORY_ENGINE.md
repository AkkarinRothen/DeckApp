# Paridad TDAPP-Desktop: The Story Engine — Deck of Worlds & Microsettings

En cumplimiento del **Feature Parity Mandate** (`RULE[user_global]`), este documento detalla los elementos de datos, modelos de dominio, interfaces y lógica de mesa implementados en Android (TDAPP) para su réplica equivalente en la aplicación de escritorio (TDAPP-desktop).

---

## 1. Paquete de Inicio y Assets
- **Archivo JSON:** `core/data/src/main/assets/starter_packs/story_engine_deck_of_worlds.json`
- **Carpeta de Cartas:** `core/data/src/main/assets/starter_packs/deck_of_worlds/cards/*.webp` (246 archivos: 240 cartas + 6 reversos).
- **Sub-mazos (Stacks):**
  1. `101`: *Regiones (Region Cards)* — 40 cartas (Verde/Dorado)
  2. `102`: *Hitos (Landmark Cards)* — 40 cartas (Azul)
  3. `103`: *Nombres (Namesake Cards)* — 40 cartas (Naranja/Ámbar)
  4. `104`: *Orígenes (Origin Cards)* — 40 cartas (Violeta)
  5. `105`: *Atributos (Attribute Cards)* — 40 cartas (Cian/Turquesa)
  6. `106`: *Acontecimientos (Advent Cards)* — 40 cartas (Rojo)
- **Opciones de cartas:** según el mazo, una (`IMAGE_WITH_TEXT`), dos (`TOP_BOTTOM_SPLIT`) o cuatro (`FOUR_EDGE_CUES`); ver `docs/ux/story-engine-card-options.md`.

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
