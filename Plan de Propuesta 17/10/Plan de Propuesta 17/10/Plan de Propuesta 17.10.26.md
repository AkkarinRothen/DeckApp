# Plan de mejora de la mesa de juego

Fecha: 17 de octubre de 2026. Estado: propuesta preparada después de recorrer la interfaz real y revisar su implementación. Este trabajo agrega documentación y evidencias; no implementa los cambios propuestos.

## 1. Objetivo y diagnóstico

La mesa reúne muchas herramientas útiles, pero hoy el espacio, los accesos y los visores no siguen una estructura común. Parte de la dificultad viene de controles superpuestos; otra parte, de rutas incompletas y estados vacíos que no permiten continuar. Cambiar colores o reducir márgenes por sí solo dejaría esos problemas pendientes.

El objetivo es que una partida pueda avanzar desde la mesa con tres operaciones claras: **consultar un recurso, resolver una acción y registrar lo ocurrido**. La mesa debe conservar el contexto al pasar de una operación a otra.

Orden recomendado:

1. Recuperar espacio y asegurar que todas las herramientas sean accesibles.
2. Corregir rutas y estados vacíos que bloquean el uso.
3. Unificar visores, registro y comportamiento de los bloques.
4. Mejorar los flujos de escenas, combate, mapas y sistemas concretos.
5. Pulir densidad, accesibilidad y presentación visual.

## 2. Cómo se hizo la revisión

Dispositivo: Motorola One Fusion, Android conectado por ADB. Resolución física 720 × 1600, densidad 280 y escala de fuente 1,1. Se recorrieron vertical y horizontal. La app instalada se identifica como com.deckapp.ttrpg, versión 0.1.1, actualizada el 17/10/2026 a las 12:23:50.

Se abrieron paneles, formularios, menús y modos de distribución; se desplazaron listas y se enfocó el editor para observar el teclado. No se lanzaron dados, no se seleccionaron resultados de tabla, no se crearon personajes o escenas y no se modificaron puntos de vida ni posiciones de tokens. Se restauraron la distribución Grilla, la vista Principal, la herramienta de desplazamiento del mapa y la rotación automática.

La sesión observada tiene 39 PNJs, tablas de Choir of Flesh y mazos vinculados. No tenía combate activo, mapa de hexploración vinculado ni crónica Mythic vinculada; los mazos consultados no tenían cartas en mano. Estas condiciones limitan qué flujos se pudieron comprobar sin alterar la partida.

| Área | Alcance real de la revisión |
| --- | --- |
| Mesa y personalización | Se probaron Grilla, Solo y DM, cambio de vista, entrada y salida de edición, catálogo de bloques e Inspiración. |
| Tablas | Se abrió el catálogo, se expandió el panel, se consultó una tabla y su lista manual. No se ejecutaron tiradas ni registros. |
| Notas | Se abrió el panel y se enfocó el editor con teclado, sin introducir texto. |
| PNJs | Se abrió el listado y la ficha de Acechador Silencioso; se buscó Caballo en la búsqueda de sesión. |
| Combate | Se probó el estado sin encuentro y su acceso. El combate activo se revisó en código, no se inició uno nuevo. |
| Mapa táctico | Se abrió el mapa existente, se seleccionó un token y se consultó su inspector. No se movió ni editó. |
| Hexploración | Se comprobó el bloque y el destino sin mapa vinculado. La exploración activa requiere otra prueba con datos. |
| Hilos, relojes y Atajos | Se abrieron los paneles vacíos y se expandió Atajos. No se crearon recursos ni ejecutaron macros. |
| Escenas y Choir | Se abrió el primer paso del encuadre, el creador de Inquebrantable y el asistente de Incursión en horizontal. Se cancelaron los formularios. |
| Búsqueda, dados, historial y configuración | Se usaron búsqueda y filtros básicos, el lanzador sin tirar, el historial existente y la configuración sin modificar vínculos. |
| Wiki | Se abrió el panel vacío y se revisó su implementación. |
| Diario, Referencias y Plan | La navegación presenta restricciones. Se revisaron las conexiones en código; no se da por probado un diario operativo ni un flujo de referencias completo. |

Las observaciones del dispositivo describen la app instalada. Los hallazgos de código describen el checkout actual, que contiene cambios previos sin confirmar; no se presupone que ambos sean idénticos en todos los detalles.

## 3. Evidencias principales

Las capturas de este directorio documentan el recorrido. Estas son las más relevantes:

| Evidencia | Hallazgo |
| --- | --- |
| [Mesa inicial](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/01-mesa-inicial.png) | Hilos/Relojes cubre las vistas de mesa; Atajos cubre el bloque de combate; la cabecera corta acciones de Choir. |
| [Tablas expandidas](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/03-tablas-expandida.png) | Filtros, favoritos y acciones flotantes compiten con la lista; aparece el nombre técnico del archivo JSON. |
| [Consulta manual de tabla](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/05-tabla-manual.png) | Cabecera y acciones dejan poco espacio para entradas largas; hay desplazamientos internos. |
| [Notas con teclado](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/08-notas-teclado.png) | El teclado tapa los controles de formato. |
| [Combate sin encuentro](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/10-combate-sin-encuentro.png) | Abrir Tracker conduce a una hoja vacía. |
| [Ficha de PNJ](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/12-pnj-ficha.png) | Descripción extensa, marcadores Markdown visibles y estadísticas relegadas debajo. |
| [Hexploración sin vínculo](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/14-hexploracion.png) | Pide vincular un mapa sin ofrecer la acción en el mismo lugar. |
| [Mapa táctico](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/15-mapa-tactico.png) | La cápsula tapa el selector de mapas; las herramientas usan iconos sin rótulo visible. |
| [Inspector de token](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/16-token-seleccionado.png) | Cubre el token, parte el indicador de HP y corta opciones laterales. |
| [Catálogo de bloques](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/24-catalogo-expandido.png) | Mucho espacio para filtros y organización antes de los recursos; hay texto con codificación dañada. |
| [Mesa horizontal](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/27-horizontal-estable.png) | Queda visible casi solo el encabezado de una tarjeta; las pestañas laterales no caben. |
| [Búsqueda de Caballo](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/34-busqueda-caballo.png) | El recurso existe, pero su visor difiere de la ficha abierta desde PNJs. |
| [Historial](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/50-historial-vertical.png) | Los filtros ocupan gran parte de la pantalla; eventos de reinicio muestran “Carta #null”. |
| [Distribución Solo](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/52-distribucion-solo.png) | Se destaca Notas, pero los accesos a herramientas secundarias no resultan visibles en el recorrido. |
| [Distribución DM](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/53-distribucion-dm.png) | Elige Combate como protagonista aunque no haya combate activo. |
| [Mazos en vertical](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/29-mazos-vertical.png) | Nombres extensos y acción principal representada por un “+”. La mano está vacía. |
| [Wiki vacía](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/32-wiki-vertical.png) | Búsqueda sin explicación del estado ni acción de creación o vínculo. |
| [Dados en horizontal](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/39-dados-expandido.png) | Incluso expandido, el botón Lanzar queda comprimido y su texto cortado. |
| [Encuadre de escena](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/40-encuadre-escena.png) | Flujo por pasos aprovechable; la etiqueta del último paso se parte en poco ancho. |
| [Creación de Inquebrantable](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/41-choir-inquebrantable.png) | El acceso de cabecera conduce directamente a un formulario de creación. |
| [Incursión horizontal](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/42-incursion-horizontal.png) | Cabecera, pestañas y botones dejan casi fuera del área visible la grilla y sus acciones. |
| [Configuración](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/46-configuracion-expandida.png) | Reúne nombre, vínculos y sistemas en una hoja extensa; es un destino que debe poder alcanzarse en vertical. |
| [Inspiración](/I:/TTRPG/Visuales/Decks/Deckapp/docs/ux/mesa-2026-10-17/evidencias/54-inspiracion.png) | Mantiene estados útiles, pero su vacío debe explicar cómo conseguir el primer resultado. |

## 4. Estructura de pantalla propuesta

### Teléfono en vertical

- **Cabecera compacta:** volver, nombre abreviado de sesión, selector de Mesa/Táctico/Exploración/Oráculo y menú contextual. Las acciones de sistema viven en ese menú o en un acceso “Sistema”.
- **Contexto opcional:** una sola franja con escena actual y avisos de combate/relojes/hilos. El detalle se abre al tocar. La franja participa del layout y no cubre el contenido.
- **Área de trabajo flexible:** bloques o visor principal; ocupa el espacio restante y recuerda su posición.
- **Barra de sesión:** Herramientas, Dados, Nota y Buscar, con personalización de favoritos. Las herramientas restantes están en un selector con texto y búsqueda.
- La navegación general Librería/Sesiones/Buscar/Ajustes se retira de la sesión activa; la salida de la mesa sigue disponible en la cabecera.

### Teléfono en horizontal

- Usar la altura disponible además del ancho para decidir el layout. Girar el teléfono no debe activar automáticamente una disposición de escritorio.
- Cabecera de una fila, contexto condensado y herramientas accesibles mediante menú o rail compacto desplazable.
- Abrir como máximo un inspector a la vez cuando el ancho restante no alcanza para leer la mesa. En poca altura, preferir un visor completo que pueda volver al contexto anterior.
- Los paneles laterales simultáneos quedan para ventanas suficientemente grandes y con un ancho mínimo garantizado para el contenido central.

### Ventanas amplias

- Conservar la mesa al centro y un panel auxiliar redimensionable. Permitir dos auxiliares solo cuando el centro siga siendo útil.
- La lógica se basa en restricciones reales de ancho y alto, no únicamente en orientación.
- Los mismos recursos y acciones deben estar disponibles en todos los tamaños.

## 5. Plan por área

### A. Estructura y navegación — P0, esfuerzo medio

**Problemas:** desborde de la cabecera, overlays sobre contenido, herramientas difíciles de alcanzar en vertical, pérdida de altura en horizontal y paneles laterales con medidas fijas.

**Cambios:**

1. Centralizar en SessionScreen la distribución de cabecera, contexto, contenido e instrumentos de sesión.
2. Ocultar la navegación general en la ruta de sesión desde NavGraph.
3. Integrar un selector común de herramientas. Evaluar SessionBottomBar ya existente antes de crear otro componente; no colocar su lista completa y cuatro acciones fijas en una sola fila estrecha.
4. Reubicar SessionTopHudCapsule dentro del flujo de layout. Mostrar contadores y alertas relevantes; el detalle usa el mismo contenedor de paneles que las demás herramientas.
5. Unificar Atajos con la barra de sesión o darle espacio reservado. Evitar una segunda barra flotante permanente.
6. Corregir el contrato de tamaño de BentoSidebar y su overlay: su Box exterior ocupa todo el espacio e ignora el modifier recibido, mientras SessionScreen intenta usarlo dentro de filas con anchos fijos.
7. Hacer desplazables y accesibles los selectores laterales; los nombres dibujados en Canvas deben tener semántica equivalente.

**Aceptación:** todas las herramientas se abren en vertical sin necesitar un bloque; no hay títulos o botones críticos cortados con fuente 1,1; ninguna barra tapa controles; horizontal permite leer y operar contenido; volver cierra primero el panel actual y devuelve el contexto.

### B. Vistas, distribución y edición de bloques — P1, esfuerzo medio

**Problemas:** se mezclan vistas Principal/Exploración/Narrativa con distribución Grilla/DM/Solo; la edición muestra muchas opciones; los vacíos ocupan más que su información; DM prioriza un combate inactivo.

**Cambios:**

1. Separar visualmente “Espacio de trabajo” de “Distribución”. Proponer “Panel principal” en lugar de “Protagonista”.
2. Conservar Grilla/DM/Solo y la personalización actual durante la transición; aclarar qué cambia cada opción antes de renombrarlas o migrarlas.
3. En teléfono, usar una columna de bloques compactos; grilla de dos columnas solo cuando ambos bloques sigan siendo legibles.
4. Definir tres presentaciones comunes: resumen plegado, uso rápido y detalle. El bloque de mesa muestra la información y acción más frecuentes; la edición completa vive en el visor.
5. Sustituir vacíos grandes por una fila breve y una acción útil: “Elegir tabla”, “Preparar encuentro”, “Vincular mapa”.
6. Mantener accesibles los bloques secundarios en Solo y DM; reproducir el caso observado de herramientas no visibles y corregirlo antes de tomar esos modos como predeterminados.
7. Elegir el panel principal según actividad real y elección explícita; no favorecer un encuentro inexistente.
8. En edición: arrastre, mover arriba/abajo, tamaño, título, vínculo y grupo dentro de un menú consistente. Mantener una vista previa del resultado.
9. Simplificar Agregar bloque: primero tipo, después recurso. Carpetas inteligentes y organización avanzada pasan a un apartado secundario.

**Aceptación:** cambiar distribución conserva todos los bloques y sus vínculos; los secundarios se alcanzan sin salir del modo; un bloque vacío ocupa una altura breve; se puede reordenar sin arrastre; rotar no reinicia la personalización ni el contexto de lectura.

### C. Tablas y modo manual guiado — P1, esfuerzo medio

**Problemas:** paneles apilados, cabeceras extensas, favoritos grandes, etiquetas técnicas y experiencias diferentes según se entre desde Tablas, búsqueda o mesa.

**Cambios:**

1. Usar un solo visor de tabla desde catálogo, bloque, búsqueda y atajo. En teléfono, abrirlo con espacio suficiente para leer entradas largas.
2. Cabecera breve con nombre y fórmula; edición/exportación pasan al menú. Favorecer lectura sobre administración durante la partida.
3. Presentar claramente “Consultar / Elegir resultado” y “Tirar”. Conservar el modo manual ya integrado.
4. Mostrar entradas con columna de rango estrecha y texto amplio, un único desplazamiento principal y resultado seleccionado reconocible.
5. Para resultados de rango, permitir ingresar el dado exacto con teclado numérico, validación local y retorno a la tabla sin perder posición.
6. Animar el desplazamiento hacia un resultado ya resuelto, sin hacer tiradas adicionales para la animación. Respetar reducción de movimiento.
7. Usar “Choir of Flesh” como nombre de sistema; eliminar el nombre del archivo JSON del filtro visible.
8. Compactar favoritos y agrupar creación/importación/packs en una sola acción de administración; evitar tres botones flotantes cubriendo filas.
9. En el bloque sin vínculo, explicar el estado y ofrecer “Elegir tabla” en vez de un botón Tirar deshabilitado sin explicación.

**Aceptación:** una elección manual y una tirada automática producen cada una un solo registro; muestran tabla, dado exacto y resultado; abrir y cerrar no registra nada; el mismo recurso tiene el mismo comportamiento desde cualquier entrada; una animación cancelada no cambia el resultado.

### D. Notas, diario e historial — P0 para acceso y teclado; P1 para integración, esfuerzo medio/grande

**Problemas:** “Notas”, “Bitácora”, “Diario” e “Historial” no explican destinos distintos; Diario queda provisional; el teclado tapa formato; historial muestra “Carta #null” en eventos que no corresponden a una carta.

**Cambios:**

1. Dar a la sesión un acceso “Registro” con dos superficies claras: **Notas libres** y **Actividad**. Conservar los datos actuales y sus destinos de persistencia.
2. Resolver el Diario antes de ofrecerlo como herramienta operativa: NavGraph no registra un conector journal y SessionScreen muestra un texto provisional como fallback.
3. Primera entrega: reutilizar las notas y el historial existentes dentro de una navegación común. No es necesario migrar toda la persistencia para corregir la experiencia inicial.
4. Segunda entrega: evaluar entradas estructuradas con origen, escena, recurso y marca de tiempo. Definir migración e identificación de entradas antes de cambiar el modelo.
5. Mover el formato de notas por encima del teclado mediante insets correctos. Usar visor completo al escribir en poca altura.
6. Mantener borrador, selección, scroll y estado de guardado al abrir un recurso o rotar. Diferenciar Guardando, Guardado y Error con acción de reintento.
7. Unificar el punto de registro desde tablas, dados, búsqueda, cartas, escenas y relojes. Auditar duplicados entre automatismos y botones de copia antes de ampliar los registros.
8. Compactar filtros de historial en un botón con resumen; mostrar actividad útil desde la primera pantalla. Representar reinicios por mazo y acción, sin fabricar referencias de carta nulas.
9. Formatear duraciones largas en horas y minutos; aclarar si el contador representa tiempo transcurrido o tiempo efectivo de juego.

**Aceptación:** todo botón “Al diario” conduce a un destino visible; cada acción aparece una sola vez; el teclado no tapa editor, formato ni retorno; no hay “null” visibles; abrir un recurso desde el registro conserva el punto de lectura al volver.

### E. Mazos y cartas — P1, esfuerzo medio

**Problemas:** listado de mazos con nombres largos y botón “+” poco explícito. La mano estaba vacía; las interacciones sobre cartas se revisaron en DeckWorkspace y requieren una prueba posterior con cartas.

**Cambios:**

1. Separar visualmente selector de mazo, mano actual y descarte. Mostrar disponibles y última acción en un resumen compacto.
2. Usar “Robar” como acción visible y un indicador claro de expandir/plegar; evitar que el usuario tenga que interpretar un “+”.
3. Añadir búsqueda o favoritos cuando haya muchos mazos. Conservar títulos completos en el detalle.
4. En vertical, miniaturas legibles y visor ampliable; en horizontal, mano desplazable con detalle auxiliar cuando quepa.
5. Distinguir gesto de navegar por la mano de descartar. Como el código descarta al deslizar, ofrecer feedback y deshacer si se mantiene ese gesto.
6. Unificar revelar, guardar como inspiración y registrar con los controles equivalentes de tablas.

**Aceptación:** con 1, 10 y 30 cartas se puede identificar, ampliar y volver sin perder posición; desplazar la mano no descarta accidentalmente; no se roba una carta al intentar expandir un mazo; las acciones respetan cartas ocultas y formatos de imagen.

### F. PNJs y fichas — P1, esfuerzo medio

**Problemas:** lista de 39 PNJs sin búsqueda visible en su panel, Markdown sin renderizar en descripción y ficha estrecha con narrativa antes de estadísticas; el visor de búsqueda es distinto.

**Cambios:**

1. Agregar búsqueda y filtros de sistema, tipo y vinculados a la escena; distinguir “Disponibles” de “Activos” para no llamar activos a todos los PNJs de la biblioteca.
2. Crear una ficha compartida para bloque, listado, búsqueda y combate: imagen ampliable, identidad, resumen mecánico y acciones.
3. Pestañas o secciones plegables para Acciones, Descripción y Notas. Durante combate abrir primero lo mecánico.
4. Renderizar o limpiar Markdown de las descripciones. Conservar texto íntegro en detalle, no en una alerta estrecha.
5. Usar estadísticas y controles del sistema de la criatura; mantener el modelo específico de Choir en todos los puntos de entrada.
6. Ofrecer “Añadir al encuentro”, “Fijar en mesa” y “Registrar” con nombres consistentes y destino explícito.

**Aceptación:** Caballo y Acechador muestran la misma ficha desde búsqueda y PNJs; la imagen se puede ver completa; sus acciones se consultan sin atravesar primero varios párrafos; no se muestran atributos genéricos irrelevantes para su sistema.

### G. Combate — P0 para estado vacío; P1 para uso activo, esfuerzo medio/grande

**Problemas:** Abrir Tracker sin encuentro abre una hoja vacía. Existen bloque, HUD, panel de combate e iniciativa táctica: hay que asignar una función clara a cada uno.

**Cambios:**

1. Estado inicial con “Preparar encuentro”, “Elegir encuentro” y, cuando corresponda, “Añadir participantes”. Nunca abrir una superficie vacía.
2. Bloque de mesa: ronda, turno actual, siguiente y acceso al detalle. El HUD queda reducido a aviso y avance; la edición de participantes vive en un único inspector.
3. En táctico, usar su iniciativa como representación principal; evitar otro controlador de turnos simultáneo.
4. Inspector de participante: nombre, recursos del sistema, condiciones y acciones; descripción y ajustes quedan plegados.
5. Distinguir aplicar daño, curar y cambiar un valor. Ofrecer deshacer para cambios frecuentes y separar finalizar combate de acciones ordinarias.
6. Para Choir, agrupar las acciones y recursos existentes en una secuencia legible; exponer qué efecto se va a aplicar y qué decisión sigue siendo manual.
7. Al terminar, resumen y enlace al procedimiento de consecuencias ya integrado.

**Aceptación:** el combate puede prepararse desde la mesa; turno, ronda y recursos coinciden en bloque, panel y mapa; los controles específicos de Choir están disponibles sin introducir automatismos de reglas nuevos; salir del inspector no pierde la selección.

### H. Mapa táctico — P1, esfuerzo medio/grande

**Problemas:** inspector sobre el token, controles cortados, acciones de eliminar/cerrar muy juntas y selección de mapa cubierta por el HUD.

**Cambios:**

1. Reubicar selector de mapa en una cabecera común libre de superposiciones.
2. Inspector inferior en teléfono con resumen inicial; panel lateral solo si queda un lienzo útil. Al abrirlo, mantener el token seleccionado visible en el área libre.
3. Primera fila de inspector: nombre, recurso principal y condiciones. Elevación, tamaño y ajustes pasan a “Más”.
4. Agrupar herramientas por Selección, Medición y Edición; mostrar el nombre de la herramienta activa y una indicación breve de su gesto.
5. Coordinar Atajos, herramientas y pines para que solo un panel detallado esté abierto cuando falte espacio. Conservar el cierre automático que ya existe entre Atajos y herramientas tácticas.
6. Separar eliminación de las acciones más frecuentes y ofrecer recuperación apropiada.
7. Dar una alternativa en lista a tokens y pines dibujados en Canvas para accesibilidad y selección precisa.

**Aceptación:** token e inspector son visibles al mismo tiempo; no se corta HP ni los controles esenciales; pan/zoom no se confunde con mover token; la selección permanece al volver de una ficha; las herramientas pueden usarse con una mano y tienen etiquetas accesibles.

### I. Hexploración — P0 para vínculo y estado; P1 para actividad, esfuerzo medio

**Problemas:** el bloque muestra “Mapa activo” aunque el destino pide vincular uno, sin acción directa.

**Cambios:**

1. Derivar el estado visible del vínculo real. Sin mapa: “Vincular mapa” y “Crear mapa”.
2. Con mapa: mostrar ubicación, día y próxima acción; el mini resumen abre el mismo mapa que su bloque.
3. Mantener el hexágono seleccionado al consultar un POI, regla o ficha.
4. Dar a viaje, exploración y campamento un panel contextual, evitando añadir otra capa flotante sobre el mapa.
5. Ofrecer registrar una decisión manual y ejecutar procedimientos asistidos como acciones distintas y comprensibles.

**Aceptación:** el usuario puede resolver la falta de vínculo desde el propio vacío; nombre, estado y destino coinciden; el recorrido con mapa vinculado se valida expresamente en una sesión de prueba.

### J. Escenas, Plan y Mythic — P1/P2, esfuerzo medio/grande

**Problemas:** hay acciones relacionadas repartidas entre Escena, Plan, Solo y Oráculo; los accesos a entradas Wiki en varios contextos están sin implementar. Mythic no estaba vinculado en la sesión observada.

**Cambios:**

1. Usar una identidad común de escena en Plan, cabecera de Solo y registro. Mostrar título, objetivo y estado actual; completar/cerrar requiere un resultado comprensible.
2. Mantener el encuadre por pasos, con progreso legible en teléfono, borrador y regreso al paso anterior.
3. En Solo, mantener escritura o recurso principal al centro y oráculo/tablas al alcance; no esconder las herramientas secundarias.
4. En Mythic sin vínculo, ofrecer vincular o crear desde el estado vacío. Explicar qué comparte el vínculo.
5. Unificar la consulta y su resultado entre atajo, bloque y centro Mythic; registrar una vez y permitir retomarla en la escena.
6. Conectar los enlaces Wiki y la navegación de los contextos que hoy tienen callbacks vacíos.

**Aceptación:** elegir una escena actualiza las representaciones relacionadas; consultar un PNJ o regla devuelve al paso de escena anterior; la consulta de oráculo conserva pregunta y resultado; los enlaces visibles tienen destino real.

### K. Hilos, relojes, Atajos e Inspiración — P1, esfuerzo pequeño/medio

**Cambios:**

1. Mostrar contadores útiles en contexto: pendientes, completados y alertas; el vacío no ocupa una cápsula permanente si no aporta una acción frecuente.
2. Abrir detalles mediante el mismo sistema de paneles, con un solo panel expandido en teléfono.
3. En relojes, separar avanzar de editar y permitir revertir un toque. Mantener visibles segmento actual, total y consecuencia.
4. En Atajos, explicar el efecto antes de configurar: fórmula/recurso y si se registra automáticamente. Personalización mediante una acción visible, además de pulsación larga.
5. En Inspiración, conservar Pendientes/Usadas/Descartadas; aclarar “Guardar resultado” cuando no hay candidatos y permitir ir al recurso de origen.
6. Diferenciar guardar para usar después de registrar en el diario, con feedback que indique dónde quedó cada elemento.

**Aceptación:** no hay tres controladores equivalentes para la misma acción; marcar, usar y descartar inspiración conserva su origen; un reloj no avanza al intentar editarlo; la macro muestra un resultado recuperable.

### L. Choir of Flesh y otros sistemas — P1/P2, esfuerzo medio/grande

**Problemas:** los botones particulares desbordan la cabecera; “Inquebrantable” abre directamente creación; Incursión tiene contenido denso en poca altura.

**Cambios:**

1. Crear un acceso contextual “Sistema: Choir of Flesh” con Personajes, Procedimientos, Incursión, Refugio, Tablas y Reglas.
2. Personajes primero ofrece existentes y “Crear”; el creador sigue disponible como flujo independiente.
3. Separar el estado de campaña del estado de combate; elegir el personaje vinculado explícitamente cuando el procedimiento lo necesite.
4. Para Incursión, dedicar superficie útil a grilla y nodo seleccionado. En poca altura, usar páginas de contenido y una barra de acciones breve; evitar cabecera y pestañas que dejan la grilla fuera de vista.
5. Presentar cada procedimiento como pasos: requisito, decisión/tirada, consecuencia y registro. Mantener entrada manual junto al asistente.
6. Mostrar las imágenes WebP existentes en fichas y referencias, con ampliación sin recorte del arte.
7. Las mejoras de layout reutilizan las reglas ya integradas; toda ampliación mecánica requiere su cotejo específico con el material del sistema.

**Aceptación:** desde cualquier tamaño se llega a las herramientas de Choir; el personaje elegido se conserva; nodo y acciones se pueden leer en vertical y horizontal; cerrar un formulario incompleto no guarda cambios silenciosos.

### M. Referencias, Wiki y documentación — P0 para conexión; P1 para visor, esfuerzo medio

**Problemas:** Referencia dirige a Plan; se importó ReferenceTab pero no se monta en ese recorrido; el panel Wiki vacío presenta solo la búsqueda.

**Cambios:**

1. Dar a Referencias un destino propio con reglas y tablas del sistema de la sesión. Corregir el mapeo del bloque y los callbacks de navegación.
2. En Wiki vacía, explicar el estado y ofrecer crear o vincular contenido; al buscar sin resultados, mantener la búsqueda y dar una salida útil.
3. Reutilizar un visor de texto largo con encabezado breve, búsqueda dentro de la página, enlaces y favoritos.
4. Priorizar recursos de sesión; ampliar a biblioteca mediante un filtro explícito.
5. Conservar historial de navegación entre vínculos y scroll; permitir fijar una regla en mesa sin copiar su contenido.

**Aceptación:** tocar Referencia abre referencias; el recurso aparece filtrado por sistema; todo enlace visible abre su destino o explica por qué no está disponible; una consulta puede cerrarse y retomarse sin perder posición.

### N. Estilo, accesibilidad y consistencia — transversal, esfuerzo pequeño/medio

- Conservar la identidad oscura y los títulos del tema; usar tipografía de lectura para párrafos largos y controles.
- Reducir textura bajo tablas y texto extenso. Usar color de acento para acción/estado, acompañado por texto o icono.
- Unificar márgenes, alturas de cabecera, botones, separadores y menús de bloque; no reducir tipografía para encajar un control desbordado.
- Objetivos táctiles de al menos 48 dp, etiquetas accesibles, orden de foco y alternativas para arrastre y Canvas.
- Corregir textos con codificación dañada, acentos, “PNJs/NPCs”, “Tracker/Rastreador”, “Rodillo/Lanzador” y nombres técnicos filtrados a la interfaz.
- Soportar fuente ampliada y reducción de movimiento; explicar por qué una acción está deshabilitada.

### O. Dados y fórmulas — P1, esfuerzo pequeño/medio

**Problemas:** el lanzador arranca a media altura; en horizontal incluso expandido comprime el botón Lanzar hasta cortar su texto. El diálogo se cerró al girar en el recorrido; NavGraph conserva su apertura con remember, no como estado restaurable.

**Cambios:**

1. Mostrar fórmula, edición y Lanzar sin desplazamiento obligatorio. En poca altura, resultado compacto y controles en filas adaptables.
2. Conservar los controles simples de cantidad/caras/modificador y ofrecer fórmulas o favoritos sin añadir un panel siempre abierto.
3. Unificar el resultado y su historial entre lanzador, bloque de dados y Atajos. Exponer si el resultado ya se registró.
4. En modo manual, permitir ingresar un total físico con origen explícito. No atribuirle un desglose aleatorio que no fue observado.
5. Conservar fórmula, resultado y apertura al rotar; incluir etiquetas accesibles para los botones “+” y “−”.

**Aceptación:** Lanzar siempre tiene altura táctil y texto legible; cambiar el tamaño de pantalla no pierde la fórmula; ingresar un resultado físico no lanza dados nuevos; el mismo resultado se puede recuperar desde Actividad.

### P. Búsqueda de sesión — P1, esfuerzo medio

**Problemas:** la consulta vacía ofrece tablas de otros sistemas; el diálogo divide lista y detalle aunque haya poca altura y usa un texto de ayuda largo con Ctrl+K en teléfono.

**Cambios:**

1. Priorizar recursos vinculados y del sistema de la sesión. Mostrar un filtro explícito “En esta sesión / Toda la biblioteca”.
2. En teléfono, lista primero y detalle al abrir un resultado; en ventana amplia, mantener lista y detalle simultáneos.
3. Abrir los mismos visores de tabla, PNJ y regla que usa la mesa, con acciones y registro compartidos.
4. Añadir acciones de navegación a herramientas para recuperar accesos desde la búsqueda sin depender de bloques personalizados.
5. Mantener consulta, filtro y scroll al volver; diferenciar cargando, sin resultados y error. Reservar la ayuda de teclado para dispositivos con teclado.

**Aceptación:** buscar Caballo llega a su ficha completa; una búsqueda de sesión no mezcla recursos globales sin indicarlo; se puede consultar una tabla manualmente desde el resultado; cerrar el visor vuelve a la misma búsqueda.

### Q. Configuración de sesión — P2, esfuerzo pequeño/medio

**Cambios:**

1. Organizar en General, Recursos vinculados y Presentación. Nombre y sistemas quedan en General; mapa/Mythic en Recursos; herramientas favoritas y densidad en Presentación.
2. Hacer accesible esta configuración desde la cabecera compacta en cualquier tamaño, sin depender de rotar el teléfono.
3. Explicar qué cambia al vincular un mapa o una crónica; mostrar el recurso actual y acceso a él.
4. Aclarar el alcance de “Reiniciar paneles flotantes” y distinguir ese ajuste de reiniciar recursos o la partida. No mezclar acciones de recuperación visual con cambios sobre datos de juego.
5. Mostrar qué opciones se guardan al instante y qué formularios requieren Guardar; evitar controles equivalentes con comportamientos distintos.

**Aceptación:** configurar un recurso devuelve a la misma sesión y no borra personalización; los vínculos se reflejan en sus bloques; el usuario entiende el alcance de un reinicio antes de ejecutarlo.

## 6. Entregas y dependencias

Esfuerzo pequeño: cambio local sobre un componente. Medio: varios componentes reutilizados y navegación. Grande: integración de estado, destinos o persistencia. Es una estimación relativa; no implica horas de entrega.

| Entrega | Trabajo concreto | Dependencias | Condición para cerrar |
| --- | --- | --- | --- |
| 1. Mesa utilizable | Cabecera adaptable, barra de sesión, contexto sin overlays, navegación por herramientas, paneles por ancho/alto y retirada de navegación general dentro de la sesión. | Revisar NavGraph, SessionScreen y BentoSidebar juntos. | Todas las herramientas son alcanzables en ambos giros; el área de trabajo puede leerse y ningún control tapa otro. |
| 2. Salidas a estados vacíos | Combate sin encuentro, mapa sin vínculo, tabla sin recurso, Wiki vacía; conexión real de Referencias y reemplazo del Diario provisional. | Entrega 1. | Ninguna acción visible termina en una hoja vacía o destino equivocado. |
| 3. Bloques y consulta | Densidad de tarjetas, catálogo de bloques, secundarios de Solo/DM, visor de tablas, fichas PNJ y visor de reglas compartidos. | Entregas 1–2. | Recurso consistente desde búsqueda, bloque y catálogo; no se pierden configuraciones actuales. |
| 4. Registro y escritura | Editor con teclado, navegación Notas/Actividad, filtros de historial, representaciones de eventos y punto único de registro. | Visores y destinos comunes. | Una acción produce una entrada recuperable; no se muestran valores nulos; borradores y scroll se conservan. |
| 5. Juego activo | Combate específico del sistema, inspector táctico, mano de cartas, mapa vinculado y flujos de escena. | Entregas 1–4 y sesión de prueba con datos. | Casos completos de juego sin pérdida de contexto, acciones duplicadas ni desincronización. |
| 6. Procedimientos y acabado | Centro de sistema Choir, Incursión/Refugio adaptables, oráculo vinculado, Inspiración, accesibilidad y estilo consistente. | Estructura y registro estables. | Procedimientos legibles y recuperables; navegación y controles accesibles con fuente ampliada. |

**Primer lote recomendado:** entregas 1 y 2, divididas en cambios pequeños y revisables. No comenzaría con una reescritura global ni con otra capa de botones flotantes. El primer lote debe entregar una mesa donde las funciones existentes puedan alcanzarse y usarse.

## 7. Verificación de cada entrega

### Matriz de pantallas

- Motorola actual: vertical y horizontal, fuente 1,1.
- Teléfono estrecho: aproximadamente 360 dp de ancho, con fuente 1,3 y 1,5 para detectar cortes.
- Ventana baja y teclado abierto: notas, búsqueda, título de escena e ingreso de dado manual.
- Ventana amplia: un inspector y dos paneles solo si queda suficiente contenido central.

### Casos de recorrido

1. Mesa → herramienta → recurso → volver al mismo bloque y scroll.
2. Bloque sin configurar → elegir recurso → volver con estado correcto.
3. Tabla → consultar → elegir resultado físico → verificar un registro; repetir con tirada automática y animación reducida.
4. Notas → escribir borrador → abrir regla → volver → rotar → confirmar conservación del borrador.
5. PNJ → ficha → añadir a encuentro de prueba → abrirlo en mapa → consultar acción → volver.
6. Mazo → robar en sesión de prueba → ampliar carta → revelar/descartar/deshacer → comprobar mano y actividad.
7. Escena → consulta → +-inspiración → usar resultado → registrar y cerrar escena.
8. Mapa sin vínculo → vincular en sesión de prueba → visitar un hexágono → consultar POI → volver a la selección.
9. Choir → elegir personaje → consultar procedimiento → completar paso manual → registrar resultado coherente.
10. Editar mesa con grupos y bloques fijados → cambiar distribución → rotar/reabrir → comprobar orden, vínculos y tamaños.

Las pruebas automatizadas deben cubrir decisiones de navegación, estados vacíos y la unicidad del registro. La densidad visual, teclado, zonas táctiles y Canvas requieren además revisión en dispositivo. No ampliar la batería de reglas por cambios puramente visuales si esas reglas no se modifican.

### Límites de esta auditoría

No se validaron una mano con cartas, combate activo, hexploración con mapa, crónica Mythic vinculada ni guardado de formularios. Tampoco se ejecutó la animación de tirada o el registro manual para evitar añadir actividad a la sesión del usuario. Deben incluirse en una sesión de prueba antes de afirmar que esas mejoras están verificadas de extremo a extremo.

## 8. Mapa de implementación

| Grupo | Archivos principales |
| --- | --- |
| Estructura y navegación | [NavGraph.kt](/I:/TTRPG/Visuales/Decks/Deckapp/app/src/main/kotlin/com/deckapp/navigation/NavGraph.kt:115), [SessionScreen.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/SessionScreen.kt:195), [BentoSidebar.kt](/I:/TTRPG/Visuales/Decks/Deckapp/core/ui/src/main/kotlin/com/deckapp/core/ui/components/BentoSidebar.kt:98). |
| Cabecera y herramientas | [SessionTopBar.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/SessionTopBar.kt), [SessionBottomBar.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/SessionBottomBar.kt), [SessionTopHudCapsule.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/SessionTopHudCapsule.kt). |
| Distribución y bloques | [BoardWorkspaceLayout.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/BoardWorkspaceLayout.kt), [BoardWorkspacePolicy.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/BoardWorkspacePolicy.kt), [SessionBoardBlockPickerSheet.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/SessionBoardBlockPickerSheet.kt). |
| Rutas de recursos y referencias | [BoardCentralTool.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/session/BoardCentralTool.kt:101), [ReferenceTab.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/reference/src/main/kotlin/com/deckapp/feature/reference/ReferenceTab.kt). |
| Visores y registro | [TableDetailSheet.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/tables/src/main/kotlin/com/deckapp/feature/tables/TableDetailSheet.kt), [SessionDialogsHost.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/SessionDialogsHost.kt), [AddQuickNoteUseCase.kt](/I:/TTRPG/Visuales/Decks/Deckapp/core/domain/src/main/kotlin/com/deckapp/core/domain/usecase/AddQuickNoteUseCase.kt), [SessionHistoryScreen.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/session/src/main/kotlin/com/deckapp/feature/session/SessionHistoryScreen.kt:406). |
| Mazos, Wiki y Plan | [DeckWorkspace.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/DeckWorkspace.kt), [WikiTab.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/WikiTab.kt), [PlanTab.kt](/I:/TTRPG/Visuales/Decks/Deckapp/feature/draw/src/main/kotlin/com/deckapp/feature/draw/components/PlanTab.kt). |
| Táctico y Choir | Componentes de feature/draw/tactical, [ChoirIncursionDialog.kt](/I:/TTRPG/Visuales/Decks/Deckapp/core/ui/src/main/kotlin/com/deckapp/core/ui/components/ChoirIncursionDialog.kt), diálogos de creación y fichas ya integrados. |

Este mapa permite implementar el plan sobre los componentes actuales, respetando los cambios previos del proyecto y evitando reconstruir funciones que ya existen.
