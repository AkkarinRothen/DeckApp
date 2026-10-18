# Iconografía de DeckApp

El icono de Android usa una carta doble y un dado facetado en cobre, tinta y pergamino. Tiene capas adaptables para los lanzadores circular y normal, más una silueta monocroma para los lanzadores temáticos de Android. Los recursos están en `app/src/main/res/drawable/ic_launcher_*.xml` y `app/src/main/res/mipmap-anydpi-v26/`.

Los accesos principales comparten un conjunto de vectores de línea en `core/ui/src/main/kotlin/com/deckapp/core/ui/icons/DeckIcons.kt`. Cubren biblioteca, sesiones, búsqueda, mesa, mapa táctico, exploración, diario, dado, tablas, guía, oráculo y los temas. Se usan en la navegación principal, la barra de herramientas de la mesa, los bloques y atajos pertinentes. Al usarlos mediante `Icon`, toman el color del tema activo y escalan sin depender de la tipografía o de un emoji instalado en el teléfono.

Los marcadores del mapa tienen un conjunto propio en `feature/draw/src/main/kotlin/com/deckapp/feature/draw/tactical/TacticalPinIcons.kt`: información, puerta, trampa, tesoro, peligro, secreto, escalera y portal. Conservan las claves guardadas en los mapas existentes; se usan en el selector y en las fichas del pin. En el lienzo, los números de sala y las etiquetas breves personalizadas siguen como texto, mientras que los tipos de pin se trazan con geometría vectorial.

Se reemplazaron los símbolos Unicode decorativos de los controles visibles del diario de hexploración, las notas rápidas de mesa y los resultados de dados. Los símbolos que forman parte de entradas históricas del diario siguen siendo texto del contenido. Los iconos universales de acciones como cerrar, volver, añadir y ajustes conservan su forma reconocible de Material.
