package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*

data class ChoirProcedureInfo(
    val id: ChoirProcedure, val title: String, val sheet: Int,
    val reference: String, val summary: String, val guided: Boolean = false
)

data class ChoirGuideAction(val id: String, val label: String)
data class ChoirGuideStep(
    val id: String, val title: String, val instruction: String, val next: String,
    val sheet: Int, val reference: String,
    val actions: List<ChoirGuideAction>, val die: Int? = null,
    val tables: List<String> = emptyList(), val isCounter: Boolean = false
)

/** Spanish paraphrases of the six supplied Gameplay Procedures sheets. Not a new ruleset. */
object ChoirProcedureCatalog {
    val procedures = listOf(
        ChoirProcedureInfo(ChoirProcedure.EXPLORATION, "Exploración exterior", 5, "Ayuda 5 · manual pp. 100–102", "Llegadas, encuentros, acciones y turnos diarios.", true),
        ChoirProcedureInfo(ChoirProcedure.INCURSION_SETUP, "Preparar una incursión", 6, "Ayuda 6 · manual p. 111", "Rumores, extracción, peligros y cinco puntos de interés.", true),
        ChoirProcedureInfo(ChoirProcedure.INCURSION, "Explorar una incursión", 6, "Ayuda 6", "Quince acciones, costes y extracción.", true),
        ChoirProcedureInfo(ChoirProcedure.ANGUISH, "Prueba de Angustia", 3, "Ayuda 3 · manual pp. 47–49", "Interrumpe el procedimiento actual y después permite retomarlo.", true),
        ChoirProcedureInfo(ChoirProcedure.CORRUPTION, "Áreas de corrupción", 5, "Ayuda 5 · manual pp. 103–105", "Fuente, tres turnos de travesía, Mancha y consecuencias."),
        ChoirProcedureInfo(ChoirProcedure.CHECKS, "Pruebas estáticas y dinámicas", 3, "Ayuda 3 · manual p. 43", "Pruebas dinámicas por defecto y comparación con el objetivo."),
        ChoirProcedureInfo(ChoirProcedure.COMBAT, "Combate", 3, "Ayudas 3–4 · manual pp. 51, 172–174", "Preparación, iniciativa por ronda, activación, objetivo y acción."),
        ChoirProcedureInfo(ChoirProcedure.AFTERMATH, "Heridas, Vigor y después del combate", 4, "Ayuda 4 · manual p. 61", "Reacción inmediata, consecuencias, Trauma y tratamiento."),
        ChoirProcedureInfo(ChoirProcedure.SETTLEMENT, "Fase de asentamiento", 2, "Ayuda 2", "Los seis pasos del regreso, en orden."),
        ChoirProcedureInfo(ChoirProcedure.CHARACTER, "Crear personaje", 1, "Ayuda 1 · manual pp. 13–29", "Atributos, valores derivados, elecciones y trasfondo."),
        ChoirProcedureInfo(ChoirProcedure.SETTLEMENT_SETUP, "Crear asentamiento", 2, "Ayuda 2 · manual pp. 36–37", "Granja inicial, recursos, defensa, nombre y ciudadano notable.")
    )
    fun info(id: ChoirProcedure) = procedures.first { it.id == id }
    fun initial(id: ChoirProcedure): String = when (id) {
        ChoirProcedure.EXPLORATION -> "arrival"
        ChoirProcedure.INCURSION_SETUP -> "rumors"
        ChoirProcedure.INCURSION -> "action"
        else -> "reference"
    }
    private fun a(id: String, text: String) = ChoirGuideAction(id, text)
    private fun step(id: String, title: String, text: String, next: String, page: Int,
                     actions: List<ChoirGuideAction>, die: Int? = null,
                     tables: List<String> = emptyList(), ref: String = "Ayuda $page", counter: Boolean = false) =
        ChoirGuideStep(id, title, text, next, page, ref, actions, die, tables, counter)

    fun current(memory: ChoirGuideMemory): ChoirGuideStep? {
        memory.anguishChecks.firstOrNull()?.let { check ->
            return if (check.roll == null) step("anguish", "Prueba de Angustia · ${check.characterName}",
                "Angustia actual: ${check.characterId?.let { memory.observedAnguish[it] } ?: check.anguish}. Tira 1d20. Un resultado igual o menor exige resolver un Colapso. La ficha no se modifica al tirar.",
                "Resolver el Colapso si corresponde y retomar el paso anterior.", 3,
                listOf(a("resolve_anguish", "Confirmar resultado")), 20, ref = "Ayuda 3 · manual pp. 47–49")
            else step("breakdown", "Resolver Colapso · ${check.characterName}",
                "El d20 fue ${check.roll}, igual o menor que ${check.anguish}. Consulta el Colapso y resuelve sus efectos en la ficha; esta guía no los aplica automáticamente.",
                "Retomar el procedimiento que estaba en curso.", 3,
                listOf(a("breakdown_done", "Colapso resuelto; continuar")), tables = listOf("Colapso Mental"), ref = "Ayuda 3 · manual p. 49")
        }
        val p = memory.progress
        val active = p.active ?: return null
        val id = p.steps[active] ?: initial(active)
        return when (active) {
            ChoirProcedure.EXPLORATION -> when (id) {
                "arrival" -> step(id, "Comprobar encuentro al entrar", "Cada entrada en un punto exige 1d10, aunque ya lo hubieras visitado. Con 1–5, consulta Encuentros; con 6–10, elige la siguiente acción.",
                    "Encuentro, si corresponde; después elegir una acción.", 5,
                    listOf(a("arrival_roll", "Confirmar d10"), a("at_action", "Ya estoy en el punto; elegir acción")), 10, ref = "Ayuda 5 · manual p. 102")
                "encounter" -> step(id, "Resolver el encuentro", "Consulta la tabla de Encuentros. Resuelve la situación; si hay combate, completa también sus consecuencias antes de volver. Esta llegada exterior no gasta acciones de incursión.",
                    "Elegir una acción de exploración.", 5, listOf(a("encounter_done", "Encuentro resuelto")), tables = listOf("Encuentros de exploración exterior"), ref = "Ayuda 5 · manual p. 102")
                "action" -> step(id, "Elegir acción de exploración", "Un día tiene cuatro turnos. Viajar, cazar/recolectar, descansar o reunir rumores ocupa un turno. El contador se actualiza al completar la acción.",
                    "Resolver la acción elegida y revisar necesidades al terminar el día.", 5,
                    listOf(a("travel", "Viajar"), a("forage", "Cazar / recolectar"), a("rest", "Descansar"), a("rumors", "Reunir rumores")))
                "destination" -> step(id, "Elegir destino del viaje", "Puedes ir a un punto conocido o generar uno conectado con 1d6: 1–3 hito en ruinas, 4 rasgo natural, 5 área de corrupción y 6 bifurcación. Generar no realiza una tirada de encuentro todavía.",
                    "Describir el destino, llegar y comprobar el encuentro.", 5,
                    listOf(a("known", "Llegué a un punto conocido"), a("new_point", "Confirmar d6 de nuevo punto")), 6, ref = "Ayuda 5 · manual pp. 100–101")
                "landmark" -> step(id, "Describir el hito en ruinas", "Consulta la tabla de Hitos en Ruinas y anota el destino. Después confirma la llegada: se consume un turno y se comprueba encuentro.",
                    "Comprobar encuentro al entrar.", 5, listOf(a("arrived", "Llegué al punto")), tables = listOf("Hitos en Ruinas"), ref = "Ayuda 5 · manual p. 100")
                "natural" -> step(id, "Describir el rasgo natural", "Consulta Rasgos Naturales, anota el destino y confirma la llegada.", "Comprobar encuentro al entrar.", 5,
                    listOf(a("arrived", "Llegué al punto")), tables = listOf("Rasgos Naturales Apocalípticos"), ref = "Ayuda 5 · manual p. 101")
                "fork" -> step(id, "Generar los dos caminos", "Una bifurcación genera dos caminos con sus propios puntos, no un punto adicional. Resuelve ambos destinos con las referencias y anota el camino elegido. No se gasta otro turno por cada tirada de generación.",
                    "Llegar al destino elegido y comprobar encuentro; si hay corrupción, resolver su travesía.", 5,
                    listOf(a("arrived", "Llegué al destino elegido"), a("corruption", "El camino cruza corrupción")))
                "corruption" -> step(id, "Atravesar el área de corrupción", "La travesía tiene un coste fijo de tres turnos, que sustituye el turno ordinario de este viaje. Determina la fuente, sigue la Mancha (0–5), resuelve los eventos y consulta la consecuencia final. Revisa comida y sueño si cambia el día. La ayuda contiene indicaciones distintas sobre la frecuencia de eventos: coteja el manual antes de resolverlos.",
                    "Registrar tres turnos de travesía y comprobar encuentro al alcanzar el destino.", 5,
                    listOf(a("corruption_done", "Travesía y necesidades resueltas")), ref = "Ayuda 5 · manual pp. 103–105")
                "forage" -> step(id, "Cazar y recolectar", "Resuelve una prueba media de INT con la regla de pruebas dinámicas. Si tienes éxito obtienes 1d4+1 raciones. Aquí marcas el resultado; no se inventa un objetivo numérico.",
                    "Con éxito, determinar las raciones; con fallo, completar el turno.", 5,
                    listOf(a("forage_success", "Tuve éxito"), a("forage_fail", "Fallé")), ref = "Ayudas 3 y 5 · manual p. 43")
                "rations" -> step(id, "Anotar las raciones encontradas", "Tira 1d4 y suma 1. Anota las raciones en la ficha o inventario antes de confirmar. La guía registra el resultado y el turno; no edita el inventario.",
                    "Elegir otra acción o revisar las necesidades del día.", 5,
                    listOf(a("rations_done", "Ya anoté las raciones")), 4)
                "rest" -> step(id, "Resolver descanso y equipo", "Un turno permite dormir, recuperarte o gestionar el inventario. Dormir al menos seis horas al día evita Fatigado. Aplica la recuperación correspondiente con su regla; este paso no cura Trauma ni Angustia por sí solo.",
                    "Completar el turno y revisar las necesidades del día cuando corresponda.", 5, listOf(a("rest_done", "Descanso resuelto")))
                "rumors" -> step(id, "Reunir rumores", "Resuelve una prueba media de PRE: con éxito ganas dos rumores; con fallo ganas uno. Anótalos en la ficha antes de confirmar.",
                    "Completar el turno de exploración.", 5, listOf(a("rumors_success", "Éxito: anoté 2 rumores"), a("rumors_fail", "Fallo: anoté 1 rumor")), ref = "Ayudas 3 y 5 · manual p. 43")
                "day_end" -> step(id, "Revisar comida y sueño", "Finalizaste los cuatro turnos del día. Cada criatura debe comer al menos una ración y dormir al menos seis horas. Revisa las condiciones Hambriento y Fatigado si corresponde y aplica sus efectos manualmente.",
                    "Comenzar el día siguiente y elegir acción.", 5, listOf(a("day_done", "Necesidades y condiciones revisadas")))
                else -> null
            }
            ChoirProcedure.INCURSION_SETUP -> when (id) {
                "rumors" -> step(id, "Generar una localización", "Gasta 10 rumores para generar una localización. El descuento se realiza en tu ficha; confirma aquí cuando esté hecho.",
                    "Preparar el trazado con la hoja de Incursion Grid.", 6, listOf(a("rumors_spent", "Ya gasté 10 rumores")))
                "layout" -> step(id, "Preparar la grilla", "Usa Incursion Grid para generar el trazado de la localización. Puedes abrir el tablero de incursión desde aquí y volver sin perder el paso.",
                    "Tirar 1d8 para situar la zona de extracción.", 6, listOf(a("layout_done", "Trazado preparado")))
                "extraction" -> step(id, "Situar la extracción", "Tira 1d8 y marca esa zona de extracción en tu grilla. El resultado queda guardado en la guía.",
                    "Situar dos áreas distintas de terreno peligroso.", 6, listOf(a("extraction", "Confirmar zona de extracción")), 8)
                "hazard_one", "hazard_two" -> step(id, "Situar terreno peligroso ${p.hazardousNodes.size + 1}/2", "Tira 1d8. Las dos áreas peligrosas deben ser diferentes entre sí. La extracción puede compartir zona: la ayuda no lo prohíbe.",
                    if (id == "hazard_one") "Situar la segunda área peligrosa." else "Preparar cinco puntos de interés.", 6,
                    listOf(a("hazard", "Confirmar área peligrosa")), 8, ref = "Ayuda 6 · manual p. 111")
                "poi" -> step(id, "Preparar punto de interés ${p.pointsPrepared + 1}/5", "Consulta Puntos de Interés y colócalos en orden: de arriba abajo y de izquierda a derecha. Anota cada resultado en la grilla antes de confirmarlo.",
                    if (p.pointsPrepared < 4) "Preparar el siguiente punto de interés." else "Iniciar con 15 acciones de incursión.", 6,
                    listOf(a("poi", "Punto de interés colocado")))
                "ready" -> if (p.incursionPrepared) step(id, "Incursión ya iniciada", "Esta preparación ya inició una incursión. Puedes retomarla con sus ${p.incursionActions} acciones restantes. Para otra localización usa Reiniciar este procedimiento y completa una preparación nueva.",
                    "Retomar el paso guardado de la incursión.", 6, listOf(a("continue_incursion", "Continuar incursión existente")))
                    else step(id, "Incursión preparada", "Comprueba extracción ${p.extractionNode}, terrenos peligrosos ${p.hazardousNodes.joinToString()} y los cinco puntos de interés. Iniciar establece 15 acciones para esta incursión.",
                    "Elegir la primera acción de incursión.", 6, listOf(a("begin_incursion", "Iniciar incursión · 15 acciones")))
                else -> null
            }
            ChoirProcedure.INCURSION -> when (id) {
                "action" -> step(id, "Elegir acción de incursión", if (!p.incursionPrepared)
                    "Completa la preparación antes de gastar acciones. Para una incursión ya comenzada puedes cargar manualmente las acciones restantes."
                    else "Moverse entre puntos, combatir, registrar un punto o recoger su recompensa cuesta 1 acción cada uno. Otras oportunidades pueden tener un coste propio. Con 0 acciones antes de alcanzar extracción, la ayuda indica la muerte: comprueba la situación antes de continuar.",
                    "Resolver la acción y volver a esta elección, o completar la extracción.", 6,
                    if (!p.incursionPrepared) listOf(a("prepare", "Preparar nueva incursión"), a("resume", "Cargar acciones restantes"))
                    else listOf(a("move", "Moverse · 1 acción"), a("fight", "Combatir · 1 acción"), a("scavenge", "Registrar punto · 1 acción"), a("loot", "Recoger recompensa · 1 acción"), a("opportunity", "Otra oportunidad"), a("extract", "Estoy en la zona de extracción")))
                "resume" -> step(id, "Retomar una incursión existente", "Introduce las acciones que realmente te quedan (0–15). No se descuentan rumores ni se regenera el mapa. Anota la zona de extracción en la grilla si aún no está marcada.",
                    "Elegir acción con el contador restaurado.", 6, listOf(a("resume_actions", "Confirmar acciones restantes")), 15, counter = true)
                "opportunity" -> step(id, "Coste de la oportunidad", "Introduce el coste indicado por el punto de interés (0–15). La guía no deduce ese coste del nombre de la oportunidad.",
                    "Descontar el coste y resolver la oportunidad.", 6, listOf(a("opportunity_cost", "Confirmar coste")), 15, counter = true)
                "resolve_move" -> step(id, "Resolver la llegada al punto", "El coste del movimiento ya se descontó. Actualiza tu posición en la grilla. Si entraste en terreno peligroso, consulta su tabla antes de continuar. Resuelve el contenido del punto.",
                    "Elegir otra acción de incursión.", 6, listOf(a("action_done", "Llegada y terreno resueltos")), ref = "Ayuda 6 · manual p. 111")
                "resolve_fight" -> step(id, "Resolver el combate y sus consecuencias", "El coste de combatir ya se descontó una vez, no una vez por ronda. Sigue el procedimiento de Combate y, al terminar, Heridas y Trauma. La prueba de Angustia puede interrumpir este paso.",
                    "Volver a elegir acción de incursión.", 4, listOf(a("action_done", "Combate y consecuencias resueltos")), ref = "Ayudas 3–4 y 6 · manual p. 61")
                "resolve_scavenge", "resolve_loot", "resolve_opportunity" -> step(id, "Resolver la acción del punto", "El coste ya se descontó. Resuelve el registro, la recompensa u oportunidad según el punto de interés y anota los cambios en tu ficha o inventario. Registrar y recoger recompensa son acciones distintas.",
                    "Elegir otra acción de incursión.", 6, listOf(a("action_done", "Acción resuelta y anotada")))
                "extraction" -> step(id, "Confirmar extracción", "Confirma que alcanzaste la zona de extracción. Si se agotaron las acciones antes de llegar, resuelve la consecuencia indicada en la ayuda. Este botón no concede botín, recuperación ni bonificaciones.",
                    "Consultar la fase de asentamiento al regresar.", 6, listOf(a("extracted", "Llegué y completé la extracción")))
                "finished" -> step(id, "Incursión finalizada", "El regreso activa los seis pasos de la fase de asentamiento. Consulta la ayuda 2. Para otra incursión inicia una preparación nueva desde el índice.",
                    "Escala, mantenimiento, descontento/renombre, recursos, mejoras y evento.", 2, emptyList())
                else -> null
            }
            else -> {
                val info = info(active)
                step("reference", info.title, referenceText(active), "Puedes volver al procedimiento que estabas siguiendo desde el índice.", info.sheet, emptyList(), ref = info.reference)
            }
        }
    }

    fun referenceText(id: ChoirProcedure): String = when (id) {
        ChoirProcedure.CHARACTER -> "1. Seis tiradas de 4d6, descartando el menor; asignar atributos y modificadores.\n2. Angustia 0, Humanidad 20; carga igual a FUE; Defensa: DES + armadura; iniciativa: DES; Vigor: CON + VOL; movimiento 2.\n3. Elegir competencias, dote y habilidades. Desventajas y dones según sus opciones.\n4. Resolver pecado, fragmento, susurro y ocupación. Nombre y vínculo son opcionales.\nConsulta la hoja original para las elecciones y excepciones completas."
        ChoirProcedure.SETTLEMENT_SETUP -> "Granja inicial: población 5, comida 10, agua 10; 1d4 de madera, piedra, metal, tesoro y exóticos. Defensa 12; mejora inicial: choza grande. Determina nombre y ciudadano notable (manual pp. 36–37)."
        ChoirProcedure.SETTLEMENT -> "Al regresar de una incursión, en orden:\n1. Determinar escala.\n2. Pagar mantenimiento: población y estructuras.\n3. Resolver descontento y renombre.\n4. Recolectar recursos.\n5. Construir mejoras.\n6. Un evento aleatorio (1d100).\nLos costes y efectos están en la hoja original. Este índice no los descuenta automáticamente."
        ChoirProcedure.CHECKS -> "Dinámicas (por defecto): atributo y modificadores determinan el objetivo según dificultad en la tabla de p. 43. Tira 1d20, aplica Maestría/Carga al resultado y compara.\nEstáticas: 1d20 + atributo y modificadores frente al objetivo. Igualar o superar el objetivo es éxito."
        ChoirProcedure.COMBAT -> "Preparación: dificultad (p. 174), sorpresa, terreno (p. 51) y despliegue.\nAl inicio de cada ronda: iniciativa de los PJs frente a la iniciativa enemiga.\nEnemigos: activar de abajo arriba y de oeste a este; determinar objetivo (p. 172) y conducta según heridas (p. 173).\nAtaque de PJ: prueba frente a Defensa. Ataque enemigo: el PJ tira defensa frente a Ataque; el enemigo no tira para impactar.\nConsulta también la ayuda 4 para heridas y reacción con Vigor."
        ChoirProcedure.AFTERMATH -> "Durante combate: al recibir una herida, puedes reaccionar inmediatamente gastando 1 Vigor para reducir una mayor a menor o anular una menor.\nMayor: marcar pista y, si sigue con vida, consultar la tabla de lesión del tipo de daño. Menor: marcar y sumar Desgaste.\nDespués: cada mayor sufrida aporta 1 Trauma; resolver tratamiento con suministro médico y prueba media de INT. Si falla o faltan suministros, prueba media de CON; si falla y hubo alguna menor, +1 Trauma total adicional. Limpiar pistas y consultar Trauma (p. 61).\nLa ayuda deja ambiguo el margen exacto de 4 al resolver heridas: coteja el manual antes de automatizarlo."
        ChoirProcedure.CORRUPTION -> "1. Determinar fuente (p. 103).\n2. Seguir Mancha, de 0 a 5.\n3. Atravesar el área en tres turnos de exploración y resolver eventos (pp. 104–105).\n4. Consultar la consecuencia final de Mancha al salir (p. 103).\nLa hoja indica tanto una frecuencia por área como tres eventos al terminar: consulta el manual para resolver esa diferencia."
        ChoirProcedure.ANGUISH -> "Cada vez que un PJ gana Angustia, prueba 1d20 contra su Angustia actual. Igual o menor exige Colapso (p. 49). La guía puede interrumpir tu paso actual y retomarlo después."
        else -> info(id).summary
    }
}
