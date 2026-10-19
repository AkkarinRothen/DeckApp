# Paridad TDAPP-Desktop: Gestión Dinámica de Mazos en Sesión Activa

En cumplimiento del **Feature Parity Mandate** (`RULE[user_global]`), este documento especifica la arquitectura, interfaces y lógica de control implementadas en Android (TDAPP) para su implementación equivalente en la aplicación de escritorio (TDAPP-desktop).

---

## 1. Problema Resuelto
Previamente, la selección de mazos vinculados a una sesión se definía únicamente en la pantalla previa de creación/configuración inicial de la partida. Una vez en el tapete de juego, no existía una vía directa para añadir nuevos mazos (por ejemplo, al introducir cartas de ayuda, tablas convertidas a cartas, o expansiones como *The Story Engine: Deck of Worlds*), ni para retirar mazos que ya no se utilizaban sin reiniciar la sesión.

---

## 2. Componentes UI a Implementar en Desktop

### A. Modal / Drawer de Selección de Mazos (`SessionDeckPickerSheet`)
- **Barra de búsqueda reactiva:** Filtrado instantáneo por nombre de mazo y por nombre de etiquetas (`Tag`).
- **Listado de Mazos disponibles:**
  - Miniatura del mazo (usando su `coverImagePath` o primera carta con aspecto correspondiente).
  - Nombre del mazo y cantidad de cartas disponibles (`$cardCount cartas`).
  - Badge o chip con la etiqueta principal (`primaryTag`).
  - Switch / Checkbox interactivo para activar o desactivar el mazo en la sesión.
- **Badge y Salvaguarda de Cartas Activas en Mesa / Mano:**
  - Indicador numérico si hay cartas actualmente robadas en mano o en el tapete provenientes de ese mazo (`$cardsInHand en mesa/mano`).
  - Al intentar desmarcar el mazo si tiene cartas activas, se muestra un diálogo de confirmación:
    > *"¿Desactivar mazo con cartas activas?"*
    > *"Hay X cartas de este mazo actualmente en mesa o en tu mano. Si lo desactivas de la sesión, estas cartas serán enviadas a la pila de descarte para evitar inconsistencias."*
    > Botones: `Cancelar` y `Desactivar y Descartar`.
- **Acceso Directo a Starter Packs:**
  - Botón o icono en la cabecera del modal para abrir directamente el catálogo de Paquetes de Inicio (`Starter Packs`), permitiendo al usuario instalar un nuevo pack y tener sus mazos inmediatamente disponibles en el listado sin abandonar la partida.

### B. Barra Lateral de Mazos (`DeckWorkspace`)
- **Cabecera de Mazos:** Mostrar fila superior con el título `"Mazos en juego (N)"` y botón de texto `"Gestionar"` o icono de edición que abre el modal selector.
- **Empty State:** Si la sesión no tiene ningún mazo activo, mostrar una tarjeta centrada con icono de cartas y botón de acción destacado: `"+ Añadir Mazos a la Mesa"`.

### C. Hoja / Pestaña de Configuración de Sesión (`SessionConfigSheet`)
- Nueva sección interactiva `"Mazos en la Mesa"`:
  - Tarjeta de resumen que muestra el conteo de mazos activos (`X mazos vinculados`) y lista resumida de sus nombres.
  - Botón `"Gestionar Mazos"` que invoca el selector modal.

---

## 3. Lógica de Estado y Repositorio (`SessionViewModel` / Session Controller)

- **Propiedades de Estado:**
  - `showDeckPicker: Boolean` (control de apertura del modal).
  - `deckCardCounts: Map<Long, Int>` (cartas disponibles por mazo).
  - `cardsInHandByDeck: Map<Long, List<Card>>` (cartas en mano/mesa agrupadas por stackId).
- **Acciones:**
  - `toggleDeckInSession(deckId: Long)`:
    - Si el mazo está activo: elimina la referencia `SessionDeckRef` de la sesión. Si tiene cartas en mano, las descarta.
    - Si el mazo no está activo: inserta un nuevo `SessionDeckRef(sessionId, stackId, isCollapsed = false, sortOrder)`.
  - `openStarterPackDialog()`: escanea los paquetes de inicio y abre el diálogo de instalación.
