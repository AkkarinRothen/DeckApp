package com.deckapp.core.data.backup

import com.deckapp.core.model.backup.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

class ChoirOfFleshPackGenerator {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Test
    fun generateChoirOfFleshPackJson() {
        val rules = listOf(
            SystemRuleBackupDto(
                id = -2001,
                title = "El Escatón (Año 1000 d.C.)",
                content = """
El año 1000 d.C. no trajo el Juicio Final anunciado en las Escrituras, sino el Escatón: dos apocalipsis simultáneos y despiadados que desgarraron la cristiandad.

- **El Coro Celestial**: Cielos abiertos de donde descienden legiones de ángeles cantores cuya armonía perfecta aniquila la voluntad humana, obligando a los penitentes a unirse a un cántico eterno hasta que sus cuerpos se marchitan.
- **La Carne Que Alimenta**: La tierra misma germinó en una masa voraz de tendones, sangre, tumores y bocas balbuceantes que devora la materia orgánica para crear amalgamas vivientes.

Los escasos supervivientes son conocidos como **Inquebrantables**: almas manchadas por un pecado tan profundo que ni el cántico sagrado ni la mutación ciega pueden asimilarlas.
                """.trimIndent(),
                gameSystem = "Choir of Flesh",
                category = "Ambientación",
                isPinned = true,
                sortOrder = 0,
                lastUpdated = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            SystemRuleBackupDto(
                id = -2002,
                title = "Chequeos y Número Objetivo (NO)",
                content = """
Para superar cualquier desafío, peligro o ataque:

**Tirada**: 1d20 + Modificador de Atributo

Si el resultado es igual o superior al **Número Objetivo (NO)**, la acción tiene éxito.

### Dificultades Estándar:
- **Muy Fácil**: NO 5 (80% éxito base)
- **Fácil**: NO 10 (55% éxito base)
- **Promedio**: NO 14 (35% éxito base)
- **Difícil**: NO 16 (25% éxito base)
- **Muy Difícil**: NO 18 (15% éxito base)
- **Casi Imposible**: NO 20 (5% éxito base)

*Un 20 natural siempre es un Éxito Crítico; un 1 natural es una Pifia Desastrosa.*
                """.trimIndent(),
                gameSystem = "Choir of Flesh",
                category = "Mecánicas",
                isPinned = true,
                sortOrder = 1,
                lastUpdated = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            SystemRuleBackupDto(
                id = -2003,
                title = "Modificadores de Maestría y Carga",
                content = """
Las circunstancias tácticas, ventajas de terreno y equipo aplican modificadores directos:

- **Maestría (+1 a +3)**: Ventaja, preparación cuidadosa, herramientas óptimas o flanqueo. Suma +1, +2 o +3 a tu tirada de 1d20.
- **Carga (-1 a -3)**: Oscuridad, terreno resbaladizo, heridas o pánico. Resta 1, 2 o 3 a tu tirada de 1d20.

Los bonos de Maestría y Carga se cancelan entre sí uno a uno.
                """.trimIndent(),
                gameSystem = "Choir of Flesh",
                category = "Mecánicas",
                isPinned = false,
                sortOrder = 2,
                lastUpdated = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            SystemRuleBackupDto(
                id = -2004,
                title = "El Dado de Uso (Usage Die)",
                content = """
Los recursos consumibles (raciones, antorchas, flechas, vendas, aceite) se gestionan mediante el **Dado de Uso (Ud)**:

**Escala**: Ud12 → Ud10 → Ud8 → Ud6 → Ud4 → Agotado

- Tras cada uso o turno de exploración, tira el Dado de Uso.
- Si el resultado es **1 o 2**, el dado desciende un escalón.
- Si obtienes un 1 o 2 en un **Ud4**, el recurso se agota.
                """.trimIndent(),
                gameSystem = "Choir of Flesh",
                category = "Supervivencia",
                isPinned = false,
                sortOrder = 3,
                lastUpdated = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            SystemRuleBackupDto(
                id = -2005,
                title = "Humanidad (HUM)",
                content = """
La Humanidad mide la cordura terrenal y la conexión con el viejo mundo (1 a 20):

- **15 - 20**: Mente lúcida. Pesadillas frecuentes pero sin penalizadores.
- **10 - 14**: -1 Carga en chequeos sociales. El canto del Coro suena tentador.
- **5 - 9**: -2 Carga en chequeos sociales. Aversión al contacto humano.
- **1 - 4**: Animales huyen; niños lloran. Atracción cósmica extrema.
- **0**: **Pérdida Total del Alma**. El personaje se convierte en Penitente o Deshecho.
                """.trimIndent(),
                gameSystem = "Choir of Flesh",
                category = "Psicología",
                isPinned = false,
                sortOrder = 4,
                lastUpdated = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            SystemRuleBackupDto(
                id = -2006,
                title = "Angustia (Distress) y Colapso Mental",
                content = """
El horror genera **Angustia (0 a 10)**.

- Al acumular **10 puntos de Angustia** o fallar un chequeo crítico de Voluntad, sufres un **Colapso Mental** (tira 1d10).
- Tras resolver el Colapso, la Angustia se reduce a la mitad de su puntuación actual.
                """.trimIndent(),
                gameSystem = "Choir of Flesh",
                category = "Psicología",
                isPinned = false,
                sortOrder = 5,
                lastUpdated = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            SystemRuleBackupDto(
                id = -2007,
                title = "Combate, Iniciativa y Táctica",
                content = """
Combate posicional por casillas:

- **Iniciativa**: 1d20 + Bono de Destreza vs Iniciativa del enemigo.
- **Acciones**: 1 Estándar (Atacar, Habilidad) + 1 Movimiento (velocidad en casillas) + 1 Rápida.
- **Defensa (CD)**: Determinada por Armadura y Destreza.
- **Flanqueo**: Otorga +1 de Maestría si un aliado traba al mismo enemigo.
                """.trimIndent(),
                gameSystem = "Choir of Flesh",
                category = "Combate",
                isPinned = false,
                sortOrder = 6,
                lastUpdated = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            SystemRuleBackupDto(
                id = -2008,
                title = "Trauma, Heridas y Condiciones",
                content = """
- **Heridas Menores**: 3 Heridas Menores se convierten en 1 Herida Mayor.
- **Heridas Mayores**: Tira 1d8 en la tabla del tipo de daño infligido.
- **Trauma Permanente**: Secuelas duraderas que marcan el cuerpo y el alma.
                """.trimIndent(),
                gameSystem = "Choir of Flesh",
                category = "Combate",
                isPinned = false,
                sortOrder = 7,
                lastUpdated = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            SystemRuleBackupDto(
                id = -2009,
                title = "El Toque de lo Sobrenatural: Máculas y Mutaciones",
                content = """
- **Mácula de la Carne**: Mutaciones corporales que conceden poder físico pero erosionan la Humanidad.
- **Canto del Coro**: Bendiciones angélicas y clarividencia a cambio de pérdida del libre albedrío.
                """.trimIndent(),
                gameSystem = "Choir of Flesh",
                category = "Sobrenatural",
                isPinned = false,
                sortOrder = 8,
                lastUpdated = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            SystemRuleBackupDto(
                id = -2010,
                title = "Asentamientos y Supervivientes",
                content = """
Gestión del refugio humano:

- **Fases**: Recolección de Recursos (Madera, Piedra, Metal, Comida, Agua), Construcción y Mantenimiento, Eventos de Asentamiento (1d100).
- **Población**: Crecer de Granja a Comuna o Aldea aumenta la capacidad defensiva y los recursos.
                """.trimIndent(),
                gameSystem = "Choir of Flesh",
                category = "Supervivencia",
                isPinned = false,
                sortOrder = 9,
                lastUpdated = 1776300000000L,
                sourcePack = "Choir of Flesh"
            )
        )

        val refTables = listOf(
            ReferenceTableBackupDto(
                id = -2101,
                name = "Modificadores de Atributo",
                description = "Conversión de puntuación a modificador de tirada en Choir of Flesh.",
                gameSystem = "Choir of Flesh",
                category = "Creación",
                columnsJson = """[{"header":"Puntuación","widthWeight":1.0},{"header":"Modificador","widthWeight":1.0}]""",
                isPinned = true,
                sortOrder = 0,
                createdAt = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            ReferenceTableBackupDto(
                id = -2102,
                name = "Números Objetivo y Dificultades",
                description = "Guía de dificultad estándar para chequeos de 1d20.",
                gameSystem = "Choir of Flesh",
                category = "Mecánicas",
                columnsJson = """[{"header":"Dificultad","widthWeight":1.2},{"header":"NO Sugerido","widthWeight":0.8},{"header":"Éxito Base","widthWeight":1.0}]""",
                isPinned = true,
                sortOrder = 1,
                createdAt = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            ReferenceTableBackupDto(
                id = -2103,
                name = "Escala de Humanidad",
                description = "Efectos según la puntuación actual de Humanidad (HUM).",
                gameSystem = "Choir of Flesh",
                category = "Psicología",
                columnsJson = """[{"header":"Puntuación","widthWeight":0.8},{"header":"Efectos Tácticos y Narrativos","widthWeight":2.2}]""",
                isPinned = false,
                sortOrder = 2,
                createdAt = 1776300000000L,
                sourcePack = "Choir of Flesh"
            ),
            ReferenceTableBackupDto(
                id = -2104,
                name = "IA Táctica de Enemigos (Solitario)",
                description = "Determinación del objetivo y acciones de enemigos según su daño.",
                gameSystem = "Choir of Flesh",
                category = "Combate",
                columnsJson = """[{"header":"Condición / Heridas","widthWeight":1.0},{"header":"Acción Predilecta","widthWeight":2.0}]""",
                isPinned = false,
                sortOrder = 3,
                createdAt = 1776300000000L,
                sourcePack = "Choir of Flesh"
            )
        )

        val refRows = listOf(
            ReferenceRowBackupDto(id = -21011, tableId = -2101, cellsJson = """["3 o menos","-3"]""", sortOrder = 0),
            ReferenceRowBackupDto(id = -21012, tableId = -2101, cellsJson = """["4 - 5","-2"]""", sortOrder = 1),
            ReferenceRowBackupDto(id = -21013, tableId = -2101, cellsJson = """["6 - 8","-1"]""", sortOrder = 2),
            ReferenceRowBackupDto(id = -21014, tableId = -2101, cellsJson = """["9 - 12","0"]""", sortOrder = 3),
            ReferenceRowBackupDto(id = -21015, tableId = -2101, cellsJson = """["13 - 15","+1"]""", sortOrder = 4),
            ReferenceRowBackupDto(id = -21016, tableId = -2101, cellsJson = """["16 - 17","+2"]""", sortOrder = 5),
            ReferenceRowBackupDto(id = -21017, tableId = -2101, cellsJson = """["18 o más","+3"]""", sortOrder = 6),

            ReferenceRowBackupDto(id = -21021, tableId = -2102, cellsJson = """["Muy Fácil","5","80%"]""", sortOrder = 0),
            ReferenceRowBackupDto(id = -21022, tableId = -2102, cellsJson = """["Fácil","10","55%"]""", sortOrder = 1),
            ReferenceRowBackupDto(id = -21023, tableId = -2102, cellsJson = """["Promedio","14","35%"]""", sortOrder = 2),
            ReferenceRowBackupDto(id = -21024, tableId = -2102, cellsJson = """["Difícil","16","25%"]""", sortOrder = 3),
            ReferenceRowBackupDto(id = -21025, tableId = -2102, cellsJson = """["Muy Difícil","18","15%"]""", sortOrder = 4),
            ReferenceRowBackupDto(id = -21026, tableId = -2102, cellsJson = """["Casi Imposible","20","5%"]""", sortOrder = 5),

            ReferenceRowBackupDto(id = -21031, tableId = -2103, cellsJson = """["15 - 20","Mente lúcida. Pesadillas frecuentes pero sin penalizadores tácticos."]""", sortOrder = 0),
            ReferenceRowBackupDto(id = -21032, tableId = -2103, cellsJson = """["10 - 14","-1 Carga a chequeos sociales. El canto del Coro sosiega; la Carne no parece tan abominable."]""", sortOrder = 1),
            ReferenceRowBackupDto(id = -21033, tableId = -2103, cellsJson = """["5 - 9","-2 Carga a chequeos sociales. Evitas activamente el contacto con humanos."]""", sortOrder = 2),
            ReferenceRowBackupDto(id = -21034, tableId = -2103, cellsJson = """["1 - 4","Los animales huyen; los niños lloran al tocarte. Deseo febril por la Carne o el Coro."]""", sortOrder = 3),
            ReferenceRowBackupDto(id = -21035, tableId = -2103, cellsJson = """["0","Pérdida de personaje: te conviertes en un Penitente del Coro o un Deshecho de la Carne."]""", sortOrder = 4),

            ReferenceRowBackupDto(id = -21041, tableId = -2104, cellsJson = """["Objetivo de Enemigo (d6)","1-2: Más herido | 3-4: Más cercano | 5-6: Aleatorio"]""", sortOrder = 0),
            ReferenceRowBackupDto(id = -21042, tableId = -2104, cellsJson = """["Sin Heridas Mayores (d10)","1-6: Ataque Estándar | 7-10: Ataque Especial"]""", sortOrder = 1),
            ReferenceRowBackupDto(id = -21043, tableId = -2104, cellsJson = """["1 Herida Mayor (d10)","1-5: Ataque Estándar | 6-8: Ataque Especial | 9-10: Acción de Soporte"]""", sortOrder = 2),
            ReferenceRowBackupDto(id = -21044, tableId = -2104, cellsJson = """["2+ Heridas Mayores (d10)","1-4: Ataque Estándar | 5-6: Ataque Especial | 7-8: Soporte | 9-10: Huir desesperadamente"]""", sortOrder = 3)
        )

        val randTables = mutableListOf<RandomTableBackupDto>()
        val tableEntries = mutableListOf<TableEntryBackupDto>()
        var tableIdCounter = -2201L
        var entryIdCounter = -220101L

        fun addRandomTable(name: String, category: String, desc: String, formula: String, items: List<Pair<IntRange, String>>) {
            val tId = tableIdCounter--
            randTables.add(
                RandomTableBackupDto(
                    id = tId,
                    bundleId = null,
                    name = name,
                    category = category,
                    description = desc,
                    rollFormula = formula,
                    rollMode = "RANGE",
                    isNoRepeat = false,
                    isPinned = true,
                    sourceType = "STARTER_PACK",
                    sourceName = "Choir of Flesh",
                    isBuiltIn = true,
                    sortOrder = Math.abs(tId).toInt(),
                    createdAt = 1776300000000L,
                    sourcePack = "Choir of Flesh"
                )
            )
            items.forEachIndexed { idx, pair ->
                val eId = entryIdCounter--
                tableEntries.add(
                    TableEntryBackupDto(
                        id = eId,
                        tableId = tId,
                        minRoll = pair.first.first,
                        maxRoll = pair.first.last,
                        weight = (pair.first.last - pair.first.first + 1),
                        text = pair.second,
                        sortOrder = idx
                    )
                )
            }
        }

        addRandomTable("El Ancla (Pecado Capital)", "Creación", "El pecado primordial que ancla tu alma a la realidad humana e impide la asimilación.", "1d10", listOf(
            (1..1) to "Orgullo (Soberbia): Te niegas a arrodillarte ante coros de luz o masas de carne. Eres mejor que ellos. Tu ego es un baluarte inexpugnable.",
            (2..2) to "Ira: Un fuego abrasador arde en tus entrañas contra los que destruyeron tu mundo. No puedes sucumbir al éxtasis mientras queden enemigos a los que desmembrar.",
            (3..3) to "Codicia: Deseas poseer, acumular y sobrevivir a cualquier costo. El Coro exige entrega total y la Carne disolución; tu avaricia se niega a soltar ni un átomo.",
            (4..4) to "Lujuria: Un anhelo ferviente por el calor y el tacto carnal genuino del viejo mundo. Rechazas la perfección estéril del canto por el pulso visceral de los sentidos.",
            (5..5) to "Engaño: Tu vida entera es una mentira construida con maestría. El Coro no puede moldear un alma que no es real y la Carne no puede atrapar a un fantasma.",
            (6..6) to "Desesperación: Has visto el vacío y la futilidad. Sin falsas esperanzas que ofrecerte ni temores que infundirte, eres inmune a sus promesas de salvación.",
            (7..7) to "Obsesión: Una única tarea terrenal te consume (entregar una carta, matar a un hombre, cumplir un juramento). El apocalipsis es una mera distracción de tu meta.",
            (8..8) to "Gula: Hambre insaciable por la vida, el dolor y el sabor de la existencia terrena. Te niegas a ser consumido porque estás ocupado consumiendo el mundo.",
            (9..9) to "Apatía: Desapego emocional absoluto. El cielo sangra y la tierra aúlla, pero nada te conmueve. El fin del mundo le sucede a otras personas.",
            (10..10) to "Duda: Escepticismo corrosivo. Cada milagro es un truco y cada cántico un fraude. No puedes ser convertido si te niegas categóricamente a creer."
        ))

        addRandomTable("Fragmento del Viejo Mundo", "Creación", "Un objeto relicario que conserva la memoria de cuando la tierra era humana.", "1d10", listOf(
            (1..1) to "Un Crucifijo Oxidado: Símbolo de la fe silenciosa anterior. Dios no canta; Dios calla, recordándote que la divinidad era paz, no una vorágine celestial.",
            (2..2) to "Un Amuleto Pagano: Piedra de río tallada con una espiral. Recuerda la tierra cuando los dioses pertenecían a los bosques y no al cielo ni a la carroña.",
            (3..3) to "El Dibujo de un Niño: Boceto en pergamino de figuras tomadas de la mano. Un baluarte de inocencia que demuestra que la alegría simple alguna vez existió.",
            (4..4) to "Una Moneda de Plata sin Deslustrar: Con el perfil del Rey Roberto II. Recuerda que la humanidad forjó leyes, comercio y orden frente a las bestias.",
            (5..5) to "Una Página de Platón: Fragmento razonado sobre la existencia. Recuerda que eres un ser pensante, no alimento del foso ni títere del coro.",
            (6..6) to "Una Bolsa de Semillas: Promesa de vida buena y pura esperando ser plantada en tierra féil, lejos de las pústulas de la Carne.",
            (7..7) to "La Llave de una Casa Incendiada: Promesa viva de reconstruir un santuario donde descansar seguro, aunque deba erigirse sobre las cenizas del mundo.",
            (8..8) to "Un Fragmento de Vidriera: Cristal azul y dorado de un santo olvidado. Testimonio de la verdadera historia antes de que los evangelios fueran corrompidos.",
            (9..9) to "Una Flor Seca y sin Olor: Belleza marchita sin diseño divino ni pretensión. Símbolo del mundo apacible que florecía sin exigir adoración.",
            (10..10) to "Un Vial con Agua de Mar: Agua salada de costas deshechas. Recordatorio de los límites del mundo finito frente al apetito infinito de la Carne."
        ))

        addRandomTable("Susurro de Condenación y Misión", "Creación", "Rumores legendarios y misiones desesperadas que empujan al Inquebrantable.", "1d10", listOf(
            (1..1) to "El Santo Silencioso: Buscas a un ermitaño inmune al himno celestial; un vacío viviente en la sinfonía divina que podría ser la clave de la resistencia.",
            (2..2) to "El Cántico Herético: Buscas las estrofas del Contra-Salmo Gnóstico, un rito discordante capaz de quebrar la carne y la fe de los Penitentes.",
            (3..3) to "Tierra Profana: Rumores de un valle o abadía oculta que permanece intacta tanto para el Coro como para la Carne: un santuario del viejo mundo.",
            (4..4) to "El Primer Apóstata: Buscas a la primera alma que eligió la Carne sobre el Coro; un tumor consciente que conoce el secreto de la supervivencia extrema.",
            (5..5) to "El Hijo de los Apocalipsis Gemelos: Buscas al legendario niño nacido de un Penitente del Coro y un Apóstata de la Carne: un puente entre dos horrores.",
            (6..6) to "La Cisterna de las Viejas Plegarias: Una catacumba donde los ecos de un millón de oraciones antiguas amortiguan el canto angélico.",
            (7..7) to "El Nombre Blasfemo: Buscas el nombre original de la Carne Que Alimenta para otorgarle mente y obligarla a negociar o enfocar su hambre.",
            (8..8) to "El Testamento Papal: La encíclica secreta de Silvestre II condenando a los ángeles antes de caer, detallando una debilidad cósmica.",
            (9..9) to "La Campana Negra: Forjada con hierro de estrella caída; su tañido produce una onda de silencio absoluto que quiebra la determinación enemiga.",
            (10..10) to "El Director Divino: Cartas astrales y visiones que señalan la ubicación del ángel Maestro que dirige la sinfonía: tu meta es asesinarlo."
        ))

        addRandomTable("Antigua Ocupación", "Creación", "El oficio que desempeñabas antes del Escatón, con sus talentos y cargas.", "1d10", listOf(
            (1..1) to "Campesino / Siervo: Conoces la tierra y el trabajo agotador. Habilidades: Levantar Cargas (FUE), Robusto (CON), Perspicacia (VOL). Resentimiento hacia nobles.",
            (2..2) to "Clero (Monje/Monja): Letrado y versado en escrituras. Habilidades: Teología (INT), Investigación (INT), Fuerza Interior (VOL). Fe desgarrada por la herejía.",
            (3..3) to "Artesano (Herrero/Albañil): Manos precisas. Habilidades: Fuerza Bruta (FUE), Dedos Ágiles (DES), Regateo (PRE). Angustia al ver construcciones destruidas.",
            (4..4) to "Soldado: Viviste por la espada y el metal. Habilidades: Frenesí (FUE), Golpes Precisos (DES), Entereza (VOL). La violencia es tu único lenguaje.",
            (5..5) to "Noble: Criado para comandar. Habilidades: Tácticas (INT), Diplomacia (VOL), Liderazgo (VOL). Tu título ahora es polvo y ceniza.",
            (6..6) to "Comerciante: Viajero de contratos y caminos. Habilidades: Resistencia (CON), Regateo (PRE), Red de Contactos (PRE). Visión mercantilista del mundo.",
            (7..7) to "Cazador: Vivías en los márgenes agrestes. Habilidades: Emboscador (DES), Sigilo (DES), Naturalismo (INT). Desconfianza extrema de los asentamientos.",
            (8..8) to "Erudito: Hombre de tinta y pergaminos. Habilidades: Maestría del Saber (INT), Enigmas (INT), Introspección (VOL). Mal adaptado al combate físico.",
            (9..9) to "Sanador: Secretos de hierbas y vendas. Habilidades: Resistencia a Enfermedades (CON), Curación (INT), Mente sobre Materia (VOL). Calificado de brujo.",
            (10..10) to "Marginado (Mendigo/Leproso): Ya vivías en el lodo. Habilidades: Robusto (CON), Pies Rápidos (DES), Lectura de Labios (INT). Sin esperanzas que perder."
        ))

        addRandomTable("Colapso Mental", "Psicología", "Efectos desencadenados al alcanzar 10 de Angustia o fallar frente al horror.", "1d10", listOf(
            (1..1) to "Pánico Ciego: Huyes a toda velocidad por la ruta más directa. Si estás acorralado, adoptas posición fetal e Indefenso 1 ronda.",
            (2..2) to "Furia Violenta: Atacas de inmediato a la criatura más cercana (aliada o enemiga) con tu arma más letal. Luego quedas Aturdido (1).",
            (3..3) to "Parálisis por Terror: Caes derribado e inmovilizado. Requiere chequeo Promedio de VOL al inicio de turno o recibir daño para reaccionar.",
            (4..4) to "Mentira Compulsiva: Racionalizas el horror con una mentira desesperada. Intentas convencer con vehemencia a todos de tu fantasía.",
            (5..5) to "Fijación Obsesiva: Te fascinas con un detalle diminuto del horror (una nota, un ojo, un patrón) y no actúas hasta recibir daño.",
            (6..6) to "Nueva Fobia: La fuente del colapso se convierte en fobia permanente; sufres Desventaja al encarar criaturas o entornos similares.",
            (7..7) to "Determinación del Pecador: Te refugias en tu Pecado. Inmune a Asustado y Ventaja temporal al confrontar directamente la fuente de tu angustia.",
            (8..8) to "Catatonía: Colapso total. Desplomado, consciente pero incapaz de moverte o hablar hasta que concluya la escena o te auxilien.",
            (9..9) to "Voz del Coro: Crees comprender una frase sagrada del canto. Ganas 1 punto permanente e imborrable de Angustia y un rumor secreto.",
            (10..10) to "Claridad Aterradora: Comprendes la verdad cósmica. Puedes purgar toda tu Angustia actual, pero la próxima que ganes se duplicará."
        ))

        addRandomTable("Encuentro en Incursión", "Exploración", "Peligros y encuentros al adentrarse en los terrenos devastados.", "1d20", listOf(
            (1..5) to "Apóstatas de la Carne: Bestias de carne viva y tumores. Tira en la tabla de Criaturas de la Carne.",
            (6..10) to "Fieles del Coro: Penitentes y legiones celestiales. Tira en la tabla de Sirvientes del Coro.",
            (11..17) to "Supervivientes del Páramo: Grupo errante o solitario. Tira en la tabla de Supervivientes. Chequeo Promedio de CAR para obtener Rumor.",
            (18..20) to "Purificadores del Coro: Inquisidores fanáticos. Si tienes alguna Mácula, atacan de inmediato sin cuartel."
        ))

        addRandomTable("Criaturas de la Carne", "Encuentros", "Engendros de La Carne Que Alimenta que acechan en las ruinas.", "1d12", listOf(
            (1..1) to "Amalgama: Masa titánica de extremidades cosidas y fauces que arremete en línea recta.",
            (2..2) to "Apóstata: Humano deformado voluntariamente por la Carne, empuñando armas primitivas con astucia.",
            (3..3) to "Tejedor de Huesos: Arácnido colosal hecho de costillas y fémures que dispara agujas óseas.",
            (4..4) to "Bandada de Carroña: Enjambre aéreo de carne alada y ojos vigilantes que desgarran desde las alturas.",
            (5..5) to "Croador: Monstruosidad hinchada con sacos de bilis que vomita pus corrosivo a distancia.",
            (6..6) to "Gusano de Carne: Anélido gigantesco de carne viva que perfora el suelo y devora armaduras.",
            (7..7) to "Rechinador: Bestia bípeda con hileras concéntricas de dientes que trituran metal y hueso.",
            (8..8) to "Fauces del Suelo: Trampa viva en el terreno esperando que una presa pise sus labios ocultos.",
            (9..9) to "Licuador: Masa gelatinosa de enzimas ácidas que disuelve carne y equipo al menor contacto.",
            (10..10) to "Madre de Tumores: Criatura inmóvil que engendra zarcillos y larvas parasitarias continuamente.",
            (11..11) to "Acechador Silencioso: Depredador camaleónico de tendones finos que ataca desde las sombras.",
            (12..12) to "Sabueso Desollado: Cuadrúpedo veloz guiado por el rastro del sudor y la angustia humana."
        ))

        addRandomTable("Sirvientes del Coro", "Encuentros", "Legiones fanáticas y ángeles heraldos del cántico celestial.", "1d12", listOf(
            (1..1) to "Cantor: Penitente con la garganta abierta entonando una armonía que paraliza la voluntad.",
            (2..2) to "Portador del Incensario: Guerrero cubierto de oro que esparce humo alucinógeno sagrado.",
            (3..3) to "Querubín: Rostro angelical de porcelana con alas afiladas como hojas de navaja.",
            (4..4) to "Confesor: Inquisidor con máscara de hierro que arranca confesiones y quiebra mentes.",
            (5..5) to "Flagelante: Penitente con látigos de púas benditas que busca la redención en la sangre.",
            (6..6) to "Heraldo: Comandante angélico con trompeta de bronce que coordina las filas del Coro.",
            (7..7) to "Cruzado Cáscara: Caballero vaciado de humanidad, una armadura pesada guiada por luz divina.",
            (8..8) to "Mater Dolorosa: Estatua viviente que llora plata ardiente y condena a los pecadores.",
            (9..9) to "Pilar de Fe: Monolito ambulante de mármol y alas doradas que aplasta a los heréticos.",
            (10..10) to "Escriba de la Penitencia: Autómata que registra cada pecado y ejecuta castigos de inmediato.",
            (11..11) to "Serafín: Ángel de seis alas de fuego sagrado que desciende con espada flamígera.",
            (12..12) to "Trono: Rueda viviente cubierta de incontables ojos ardientes que proyecta la voluntad divina."
        ))

        addRandomTable("Hitos en Ruinas", "Exploración", "Monumentos y restos de la civilización destruida por el Escatón.", "1d20", listOf(
            (1..1) to "El Molino Silencioso: Molino con la gran rueda atascada de maleza roja. El río fluye espeso y lento.",
            (2..2) to "La Posada del Ahorcado: Letrero con un ahorcado crujiente. Puertas tapiadas desde adentro.",
            (3..3) to "La Mina Colapsada: Entrada derrumbada con vigas astilladas y un golpeteo rítmico en el fondo.",
            (4..4) to "El Patio Olvidado: Barbacana solitaria de un castillo borrado de la faz de la tierra.",
            (5..5) to "La Cantera Llorosa: Paredes de roca que rezuman líquido salado negro; la piedra es gélida.",
            (6..6) to "La Abadía Hundida: Campanario sobresaliendo de un pantano fétido que devoró la iglesia.",
            (7..7) to "El Camino de las Estatuas: Calzada romana con viajeros petrificados en poses de terror.",
            (8..8) to "El Puente de Huesos: Puente sobre el abismo reparado con incontables huesos entrelazados.",
            (9..9) to "El Transbordador Abandonado: Puesto de barquero; la barca amarrada en la orilla opuesta.",
            (10..10) to "La Horca de Tres Brazos: Horca colosal sobre una colina con ocupantes desecados.",
            (11..11) to "La Colonia de Leprosos: Edificio de cuarentena sellado con incontables cruces de peste.",
            (12..12) to "El Acueducto Caído: Arcos colosales derrumbados formando un laberinto en sombras.",
            (13..13) to "El Patio del Curtidor: Fosos químicos pestilentes con pieles estiradas de origen perturbador.",
            (14..14) to "La Mansión Señorial: Cascarón quemado de un noble con alta chimenea solitaria.",
            (15..15) to "El Túnel de Carne: Montaña perforada por un conducto palpitante con vello y zarcillos.",
            (16..16) to "El Sepulcro Profanado: Cripta noble con la losa reventada con violencia desde el interior.",
            (17..17) to "La Columna del Santo: Alta columna donde vivía un eremita, cubierta de runas blasfemas.",
            (18..18) to "La Arboleda del Carnicero: Matadero en el bosque con cadáveres desollados colgados.",
            (19..19) to "El Viejo Fuerte: Empalizada militar podrida con terraplenes cubiertos de moho carmesí.",
            (20..20) to "La Escalera del Peregrino: Escalones vertiginosos tallados en la roca hacia el cielo vacío."
        ))

        addRandomTable("Rasgos Naturales Apocalípticos", "Exploración", "Paisajes mutados y tierras alteradas por fuerzas sobrenaturales.", "1d20", listOf(
            (1..1) to "El Pantano de los Susurros: Ciénaga brumosa donde el viento imita voces humanas suplicantes.",
            (2..2) to "El Bosque de Rostros: Árboles viejos cuya corteza ha crecido formando rostros gritando.",
            (3..3) to "El Río de Sangre: Aguas de color óxido profundo que tiñen de rojo todo lo que tocan.",
            (4..4) to "Las Colinas Anudadas: Hierba gruesa enredada en nudos que hace el avance agotador.",
            (5..5) to "Las Llanuras de Ceniza: Manto gris ceniciento que amortigua todo sonido en el silencio total.",
            (6..6) to "La Espina del Mundo: Picos afilados como cuchillas de hueso auténtico.",
            (7..7) to "El Arrecife de Carne Coralina: Lago con formaciones de carne viva y aspecto de coral.",
            (8..8) to "El Soto de los Susurros: Arboleda laberíntica donde los árboles cambian al no mirarlos.",
            (9..9) to "Las Cuevas Cantoras: Red subterránea donde el viento genera un himno armónico resonante.",
            (10..10) to "El Desierto de Vidrio: Suelo fundido en vidrio negro fracturado en esquirlas cortantes.",
            (11..11) to "El Bosque Petrificado: Madera convertida en piedra dura con hojas de pizarra.",
            (12..12) to "El Barranco Asfixiante: Niebla sulfurosa amarilla que desata ataques de tos violenta.",
            (13..13) to "Los Riscos Dentados: Paredes rocosas que lucen como hileras de colosales molares.",
            (14..14) to "El Valle Fúngico: Hongos gigantes bioluminiscentes que tiñen la noche de luz verdosa.",
            (15..15) to "El Huerto Yermo: Árboles carbonizados que sostienen una única fruta petrificada perfecta.",
            (16..16) to "La Estepa Silenciosa: Pastizal sin aves ni insectos; el silencio es absoluto y desquiciante.",
            (17..17) to "El Delta de Venas: Riachuelos y arroyos que se ramifican imitando el sistema circulatorio.",
            (18..18) to "Los Peñascos Colgantes: Rocas colosales suspendidas en el aire por tendones tensos.",
            (19..19) to "Los Salares: Desierto de sal con costillas colosales de titanes semienterrados.",
            (20..20) to "El Bosque de Hierro: Troncos que han absorbido tanto mineral que su corteza es puro metal oxidado."
        ))

        addRandomTable("Corrupción de la Carne", "Peligros", "Dilemas y tentaciones biológicas en territorio infectado.", "1d6", listOf(
            (1..1) to "El Fruto de Carne: Un fruto dulce late en un tallo. Comerlo restaura D6 Heridas pero añade 1 Mácula.",
            (2..2) to "El Cadáver Susurrante: Escuchar su secreto otorga 1 Rumor (chequeo Promedio VOL o 1 Mácula); destruirlo atrae combate.",
            (3..3) to "Lluvia de Gusanos: Soportarlo pudre D4 raciones (+1 Mácula); guarecerse exige chequeo de DES o quedar apresado.",
            (4..4) to "La Mano que Llama: Ignorarla exige chequeo de VOL; estrecharla otorga una Semilla de Carne pero suma 2 Máculas.",
            (5..5) to "Esporas Asfixiantes: Correr exige chequeo Difícil de CON (+2 Máculas si fallas) o sacrificar suministros valiosos.",
            (6..6) to "El Muro de Ojos: Desafiarlos exige chequeo Difícil de VOL (éxito da Ventaja, fallo +2 Máculas); rodearlo suma 1 Mácula."
        ))

        addRandomTable("Corrupción del Coro", "Peligros", "Dilemas y juicios celestiales frente a la armonía dorada.", "1d6", listOf(
            (1..1) to "El Sendero Armonioso: Camino fácil exige chequeo Promedio de VOL (fallo +1 Mácula); camino escarpado causa +1 Trauma.",
            (2..2) to "La Estatua Llorosa: Recoger lágrimas de plata elimina 1 Angustia pero suma 1 Mácula; romperla añade 1 Angustia.",
            (3..3) to "El Silencio Repentino: Chequeo Promedio de VOL (éxito quita 1 Angustia, fallo +1 Mácula); romperlo atrae al Coro.",
            (4..4) to "Geometría Imposible: Cruzar exige chequeo Difícil de INT (+2 Máculas si fallas) o perder 1 Turno de exploración.",
            (5..5) to "Cuenco de Ofrendas: Ofrendar D4 raciones o un objeto otorga favor; rechazarlo añade 1 Mácula del Coro.",
            (6..6) to "El Verso Viviente: Cumplir penitencia autoinflige +1 Trauma; atravesarlo exige chequeo Difícil de VOL o +2 Máculas."
        ))

        addRandomTable("Heridas: Daño Contundente", "Heridas", "Efectos al sufrir una Herida Mayor por impacto o caída.", "1d8", listOf(
            (1..1) to "Aire Expulsado: Sin aliento; no puedes realizar Acciones Rápidas durante 1 ronda.",
            (2..2) to "Conmoción Leve: Desorientado; sufres -1 Carga en el próximo chequeo.",
            (3..3) to "Empujón Brutal: Sales despedido hacia atrás D4 casillas.",
            (4..4) to "Fisura en Costillas: Dolor agudo al respirar; sufres 1 Herida Menor extra al correr.",
            (5..5) to "Golpe a la Rodilla: Pierdes el equilibrio y caes Derribado de inmediato.",
            (6..6) to "Armadura Abollada: Placas dobladas; tu CD se reduce en 1 hasta reparar el equipo.",
            (7..7) to "Hueso Roto: Extremidad inutilizada; sufres -2 Carga en chequeos con ese miembro.",
            (8..8) to "Trauma Craneal: Caes inconsciente durante 1 ronda o quedas Aturdido (2)."
        ))

        addRandomTable("Heridas: Cortante y Perforante", "Heridas", "Efectos al sufrir una Herida Mayor por armas afiladas o garras.", "1d8", listOf(
            (1..1) to "Corte Superficial: Sangrado molesto; sufres -1 Carga en chequeos de percepción.",
            (2..2) to "Desgarro Muscular: Movimiento reducido a la mitad hasta recibir curación.",
            (3..3) to "Perforación de Armadura: La hoja atraviesa las defensas; CD reducida en 1.",
            (4..4) to "Tendón Cortado: Pierdes el apoyo de la pierna y caes Derribado.",
            (5..5) to "Ojo Cortado: Visión obstruida por sangre; Desventaja en ataques a distancia.",
            (6..6) to "Arteria Tocada: Sangrado profuso; sufres 1 Herida Menor al final de cada turno.",
            (7..7) to "Arma Incrustada: La punta queda clavada en tu cuerpo; requiere Acción Rápida para extraerla.",
            (8..8) to "Desmembramiento / Evisceración: Pérdida de un dedo o herida mortal si no se sutura ya."
        ))

        addRandomTable("Heridas: Fuego y Sagrado", "Heridas", "Efectos al sufrir una Herida Mayor por fuego o fuego divino.", "1d8", listOf(
            (1..1) to "En Llamas: Sufres la condición Ardiendo (1 Herida Menor al inicio de cada turno).",
            (2..2) to "Ampollas Purulentas: El dolor es continuo; sufres -1 Carga en chequeos físicos.",
            (3..3) to "Carne Abrasada: El reflejo del dolor hace que sueltes inmediatamente cualquier objeto empuñado.",
            (4..4) to "Reverberación Sagrada: La luz abrasa el alma; ganas 1 punto de Angustia.",
            (5..5) to "Humo Sofocante: Inhalas cenizas calientes; quedas Aturdido (1).",
            (6..6) to "Pánico Ígneo: Huyes presuroso D4 casillas para sofocar las llamas.",
            (7..7) to "Ignición de Ropas: Ropas y correas quemadas; reduce tu CD en 1.",
            (8..8) to "Estigma Celestial: Quemadura indeleble en forma de sello; atrae a los fanáticos del Coro."
        ))

        addRandomTable("Heridas: Ácido y Carne", "Heridas", "Efectos al sufrir una Herida Mayor por fluidos corrosivos o mutaciones.", "1d8", listOf(
            (1..1) to "Salpicadura Ácida: Vaho abrasivo en los ojos; quedas Cegado durante 1 ronda.",
            (2..2) to "Asimilación Tisular: Zarcillos carnosos se adhieren a tu piel, dejándote Restringido.",
            (3..3) to "Vapores Sofocantes: Inhalas bilis pestilente; quedas Aturdido (1).",
            (4..4) to "Carne Derretida: El ácido disuelve capas vivas; sufres 1 Herida Menor adicional.",
            (5..5) to "Necrosis Progresiva: La ponzoña avanza por la sangre; sufres la condición Envenenado.",
            (6..6) to "Infusión Caótica: Tu carne reacciona con espasmos; cada vez que golpees sufres 1 Herida Menor.",
            (7..7) to "Armadura Disuelta: La protección metálica o de cuero se corroe; CD reducida en 2.",
            (8..8) to "Injerto Parásito: Una boca o tumor con vello queda adherido a tu herida (+1 Mácula de la Carne)."
        ))

        // Verbatim Chapter V profiles, regenerated with scripts/integrate-choir-bestiary.py.
        val bestiaryFile = File("src/main/assets/starter_packs/choir_of_flesh/bestiary.json")
            .takeIf { it.exists() }
            ?: File("core/data/src/main/assets/starter_packs/choir_of_flesh/bestiary.json")
        val bestiary = json.decodeFromString<FullBackupDto>(bestiaryFile.readText(Charsets.UTF_8))
        val npcs = bestiary.npcs

        val artworkFile = File("src/main/assets/starter_packs/choir_of_flesh/artwork.json")
            .takeIf { it.exists() }
            ?: File("core/data/src/main/assets/starter_packs/choir_of_flesh/artwork.json")
        val artwork = json.decodeFromString<FullBackupDto>(artworkFile.readText(Charsets.UTF_8))
        val portraits = artwork.npcs.associate { it.name to it.imagePath }

        val fullBackup = FullBackupDto(
            schemaVersion = 1,
            systemRules = rules.map { rule -> bestiary.systemRules.find { it.title == rule.title } ?: rule } + artwork.systemRules,
            referenceTables = refTables,
            referenceRows = refRows,
            randomTables = randTables,
            tableEntries = tableEntries,
            npcs = npcs.map { it.copy(imagePath = portraits[it.name]) }
        )

        val packFile = File("src/main/assets/starter_packs/choir_of_flesh.json")
        val altPackFile = File("core/data/src/main/assets/starter_packs/choir_of_flesh.json")
        val targetFile = if (packFile.parentFile?.exists() == true) packFile else altPackFile

        targetFile.writeText(json.encodeToString(fullBackup), Charsets.UTF_8)
        println("Generado choir_of_flesh.json con éxito en: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
    }
}
