# Plan priorizado de mejoras para DeckApp

**Fecha:** 21 de octubre de 2026  
**Estado:** propuesta de implementación  
**Base:** auditoría UX de la mesa del 17/10/2026, roadmap y DEVLOG del proyecto.

## Objetivo

Reducir la fricción al dirigir una partida desde el teléfono, asegurar que cada herramienta conduzca a una acción útil y hacer más confiable la preparación y conservación de campañas. Las mejoras se ordenan por impacto en el uso real y dependencias, no por cantidad de funciones.

## Criterios de prioridad

- **P0 — Bloqueante:** impide encontrar o completar una acción frecuente durante la partida.
- **P1 — Importante:** reduce pasos, inconsistencias o errores de uso.
- **P2 — Expansión:** aporta valor en casos específicos después de estabilizar los flujos principales.
- **XS/S/M/L:** esfuerzo orientativo por entrega; debe confirmarse al dividir tareas técnicas.

## Fase 0 — Alinear el plan con el estado actual

**Prioridad:** P0 · **Esfuerzo:** XS

El roadmap general tiene secciones fechadas en abril, mientras que DEVLOG y la auditoría UX llegan a octubre. Antes de abrir tareas, reconciliar el estado de implementación para no volver a planificar funciones ya terminadas.

- Marcar como completados los elementos que DEVLOG confirma, incluyendo historial, notas, búsqueda global y backup/restauración, verificando su estado actual en la app.
- Mantener como referencia principal de UX el recorrido documentado en `docs/ux/mesa-2026-10-17/PLAN.md`.
- Crear tareas a partir de los problemas observados y registrar dependencias y criterios de aceptación.

**Cierre:** cada iniciativa seleccionada tiene un estado vigente, una ubicación de implementación y una persona responsable.

## Fase 1 — Hacer usable la mesa en todos los tamaños

**Prioridad:** P0 · **Esfuerzo:** M/L

Resolver navegación, espacio y controles que hoy dificultan encontrar herramientas en vertical u operar en horizontal. Agrupar la cabecera, el contexto de la sesión, el contenido y los accesos a herramientas en un layout que responda al espacio disponible.

- Evitar que HUD, Atajos, barras o paneles tapen controles y contenido.
- Dar acceso a todas las herramientas en teléfono vertical y horizontal; conservar contexto al abrir y cerrar paneles.
- Corregir la navegación general dentro de una sesión y ofrecer un selector de herramientas consistente.
- Hacer que fuente ampliada y objetivos táctiles sigan siendo utilizables.

**Aceptación:** no hay acciones o títulos esenciales cortados; ningún panel tapa un control necesario; abrir/cerrar una herramienta conserva búsqueda, selección y posición; todos los accesos están disponibles en vertical y horizontal.

## Fase 2 — Cerrar rutas incompletas y estados vacíos

**Prioridad:** P0 · **Esfuerzo:** M

Convertir los destinos que hoy terminan en hojas vacías o mensajes provisionales en flujos que permiten seguir jugando. Reutilizar los modelos y registros ya implementados.

- En combate sin encuentro, ofrecer preparar o elegir un encuentro y añadir participantes.
- En Hexploración sin mapa vinculado, ofrecer vincular o crear el mapa desde el mismo estado vacío.
- Resolver el acceso al Diario y las referencias pendientes desde la mesa.
- En tablas sin vínculo, mostrar “Elegir tabla”; en Wiki vacía, explicar cómo crear o vincular contenido.
- Unificar Notas y Actividad bajo un acceso de Registro, haciendo visibles las funciones existentes.
- Corregir presentación del historial para que acciones sin carta no muestren referencias nulas.

**Aceptación:** todo estado vacío explica qué falta y ofrece una siguiente acción; ningún acceso principal conduce a una superficie sin contenido ni salida clara.

## Fase 3 — Unificar consulta y registro durante la partida

**Prioridad:** P1 · **Esfuerzo:** M/L

Hacer que los recursos se consulten y registren de la misma manera sin importar desde dónde se abran.

- Usar visores consistentes para tablas, PNJs, reglas y resultados de búsqueda.
- Priorizar recursos vinculados a la sesión y distinguirlos de toda la biblioteca.
- Simplificar filtros del historial y conservar el punto de lectura al volver de un recurso.
- Asegurar que dados, tablas, cartas, escenas y relojes aparezcan una sola vez en Actividad.
- Mejorar la ficha de PNJ para mostrar primero identidad, datos mecánicos y acciones; renderizar la descripción de forma legible.
- Ajustar notas y formularios al teclado y a cambios de orientación, con estados de guardado claros.

**Aceptación:** un mismo recurso tiene comportamiento consistente desde mesa, biblioteca y búsqueda; cada acción queda registrada una vez; al volver se conservan consulta y posición.

## Fase 4 — Preparación, biblioteca y protección de datos

**Prioridad:** P1 · **Esfuerzo:** M/L

Reducir trabajo repetido al preparar campañas y reforzar la recuperación de contenido.

- Mejorar importación PDF con plantillas reutilizables y rango de páginas; después, evaluar la cola de importación múltiple.
- Añadir validación de espacio y manejo recuperable de errores en importaciones largas.
- Completar exportación del mazo desde su detalle y revisar el flujo de backup/restauración existente con una campaña de muestra.
- Mejorar búsqueda y filtros de biblioteca para bibliotecas grandes; las colecciones avanzadas quedan sujetas a validar su necesidad.
- Priorizar miniaturas livianas solo si las mediciones muestran lentitud o consumo relevante.

**Aceptación:** un error de importación no deja contenido parcial sin explicación; el usuario puede recuperar la campaña desde un backup comprobado; buscar y abrir recursos no pierde el contexto.

## Fase 5 — Expansiones opcionales

**Prioridad:** P2 · **Esfuerzo:** L

Abordar una vez resueltos los problemas principales de mesa y estabilidad, eligiendo según uso real de campañas.

- Vista de mesa compartida en navegador local.
- Exportación a formatos de Foundry VTT u otros VTT objetivo.
- Tirada Tarokka con posiciones nombradas y revelado individual.
- Generadores y automatizaciones de contenido, como cartas de botín con probabilidades.

Antes de iniciar una integración, definir el caso principal, permisos de acceso, formato de datos y un prototipo pequeño.

## Secuencia recomendada de entregas

| Entrega | Alcance | Prioridad | Estimación |
|---|---|---:|---:|
| 0 | Verificar estado real y actualizar roadmap | P0 | XS |
| 1 | Navegación y layout de mesa adaptable | P0 | M/L |
| 2 | Estados vacíos y accesos incompletos | P0 | M |
| 3 | Visores, registro y búsqueda dentro de sesión | P1 | M/L |
| 4 | Importación, biblioteca y recuperación | P1 | M/L |
| 5 | Integraciones y modos especializados | P2 | L |

Las entregas 1 y 2 deben poder publicarse por partes. La fase 3 depende de tener definidos los accesos y contenedores comunes de la mesa; la fase 4 puede avanzar en paralelo si no cambia navegación compartida.

## Verificación por entrega

- Probar teléfono vertical y horizontal, además de una ventana amplia si está disponible.
- Repetir flujos con fuente ampliada y teclado abierto.
- Verificar estados vacío, cargando, error y contenido real.
- Confirmar que navegación de ida y vuelta conserva selección, filtros y desplazamiento.
- Para datos: probar persistencia tras cerrar y volver a abrir la sesión; para backups, exportar y restaurar en una base de prueba.
- Revisar que los textos estén claros, sin nombres internos ni valores `null` visibles.

## Fuera de la primera prioridad

La generación de contenido por IA, comandos de voz, servidor VTT e importación masiva automática pueden aportar valor, pero implican más dependencias y decisiones. Se recomienda reevaluarlas después de las primeras cuatro entregas, con evidencia de uso.

## Documentos relacionados

- `docs/ux/mesa-2026-10-17/PLAN.md` — hallazgos, diseño propuesto y criterios detallados de la mesa.
- `docs/planning/ROADMAP.md` — backlog histórico del producto; actualizarlo junto con la Fase 0.
- `DEVLOG.md` — historial reciente de implementación y decisiones técnicas.
