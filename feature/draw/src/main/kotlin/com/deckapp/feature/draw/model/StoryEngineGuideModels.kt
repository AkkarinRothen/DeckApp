package com.deckapp.feature.draw.model

import com.deckapp.core.model.CardContentMode

enum class StoryEngineGuideTab(val title: String) {
    DECK_OF_WORLDS("Deck of Worlds"),
    NEW_STORY_ENGINE("New Story Engine"),
    EXPANSIONS("Expansiones")
}

data class StoryEngineStep(
    val id: String,
    val stepIndex: Int,
    val tab: StoryEngineGuideTab,
    val title: String,
    val subtitle: String,
    val deckQueries: List<String>,
    val contentMode: CardContentMode,
    val description: String,
    val creativeTip: String,
    val sampleQuestions: List<String> = emptyList()
)

object StoryEngineProcedureCatalog {

    val deckOfWorldsSteps = listOf(
        StoryEngineStep(
            id = "dow_region",
            stepIndex = 0,
            tab = StoryEngineGuideTab.DECK_OF_WORLDS,
            title = "Paso 1: Región (Bioma y Terreno)",
            subtitle = "Establece el terreno principal y actúa como núcleo del microescenario",
            deckQueries = listOf("región", "region", "regiones"),
            contentMode = CardContentMode.IMAGE_WITH_TEXT,
            description = "Roba 1 carta de Región y colócala en el centro del tapete o tablero. Esta carta define el bioma, clima y terreno básico (ej: Pantano, Selva, Tundra, Archipiélago). Todas las demás cartas se conectarán a sus bordes.",
            creativeTip = "Consejo: En mundos fantásticos o de ciencia ficción, puedes combinarlo con una carta de expansión de Género para darle matices sobrenaturales.",
            sampleQuestions = listOf(
                "¿Cómo afecta este clima a los viajeros?",
                "¿Qué peligro natural es característico de esta zona?",
                "¿Qué recurso abundante atrae a forasteros?"
            )
        ),
        StoryEngineStep(
            id = "dow_landmark",
            stepIndex = 1,
            tab = StoryEngineGuideTab.DECK_OF_WORLDS,
            title = "Paso 2: Punto de Interés (Hito / Sitio)",
            subtitle = "Añade una estructura, asentamiento o accidente geográfico notable",
            deckQueries = listOf("interés", "interes", "landmark", "puntos"),
            contentMode = CardContentMode.TOP_BOTTOM_SPLIT,
            description = "Roba 1 carta de Punto de Interés y métela bajo uno de los bordes de la carta de Región. Cuenta con 2 opciones (superior e inferior). Puedes girar la carta 180° para alternar la opción activa o tirar 1d2.",
            creativeTip = "Consejo: La opción activa debe asomar por el borde de la Región, creando un mapa modular físico en la mesa.",
            sampleQuestions = listOf(
                "¿Quién construyó o descubrió este hito?",
                "¿Qué función cumple en la actualidad?",
                "¿Por qué es un punto de referencia para las caravanas?"
            )
        ),
        StoryEngineStep(
            id = "dow_namesake",
            stepIndex = 2,
            tab = StoryEngineGuideTab.DECK_OF_WORLDS,
            title = "Paso 3: Nombres y Apelativos",
            subtitle = "Bautiza la región o el punto de interés con prefijos y sufijos",
            deckQueries = listOf("apelativo", "nombre", "namesake", "nombres"),
            contentMode = CardContentMode.FOUR_EDGE_CUES,
            description = "Roba 1 carta de Nombres/Apelativos y colócala en un borde. Tiene 4 pistas cardinales (Sur, Oeste, Norte, Este). La pista activa provee epítetos y nombres propios (ej: 'El Pantano Donde No Cantan Aves', 'La Fosa del Olvido').",
            creativeTip = "Consejo: La pista superior puede servir de prefijo y las tres inferiores de terminación poética.",
            sampleQuestions = listOf(
                "¿Qué leyenda dio origen a este nombre?",
                "¿Cómo llaman los nativos a este lugar frente a cómo lo llaman los mapas oficiales?",
                "¿Qué superstición se asocia a su pronunciación?"
            )
        ),
        StoryEngineStep(
            id = "dow_origin",
            stepIndex = 3,
            tab = StoryEngineGuideTab.DECK_OF_WORLDS,
            title = "Paso 4: Orígenes e Historia (El Pasado)",
            subtitle = "Registra eventos históricos, catástrofes y hechos trascendentales",
            deckQueries = listOf("origen", "orígenes", "historia", "origin"),
            contentMode = CardContentMode.FOUR_EDGE_CUES,
            description = "Roba 1 carta de Origen y métela bajo un borde. Esta carta explica qué ocurrió aquí en tiempos antiguos: una batalla legendaria, la caída de un cometa, una plaga o un pacto arcano.",
            creativeTip = "Consejo: Gira en 90° con 1d4 para explorar distintos matices históricos que enriquecen los restos arqueológicos o ruinas.",
            sampleQuestions = listOf(
                "¿Qué cicatriz física dejó este evento en el terreno?",
                "¿Quiénes fueron los vencedores y quiénes los olvidados?",
                "¿Qué reliquia de esa era sigue oculta aquí?"
            )
        ),
        StoryEngineStep(
            id = "dow_attribute",
            stepIndex = 4,
            tab = StoryEngineGuideTab.DECK_OF_WORLDS,
            title = "Paso 5: Atributos y Cultura (El Presente)",
            subtitle = "Detalla costumbres, habitantes, vida silvestre o anomalías actuales",
            deckQueries = listOf("atributo", "atributos", "cultura", "attribute"),
            contentMode = CardContentMode.FOUR_EDGE_CUES,
            description = "Roba 1 carta de Atributo para definir el estado actual del microsetting: qué facciones operan, qué rituales practican sus habitantes o qué anomalía biológica domina el lugar.",
            creativeTip = "Consejo: Puedes complementar este paso con cartas de la expansión 'Llaves de Cultura' o 'Fragmentos de Lore'.",
            sampleQuestions = listOf(
                "¿Qué costumbre choca inmediatamente con los viajeros extranjeros?",
                "¿Qué facción o grupo controla los accesos en el presente?",
                "¿Qué rumor inquietante circula entre los aldeanos?"
            )
        ),
        StoryEngineStep(
            id = "dow_advent",
            stepIndex = 5,
            tab = StoryEngineGuideTab.DECK_OF_WORLDS,
            title = "Paso 6: Acontecimientos y Crisis (El Futuro)",
            subtitle = "El gancho inminente, dilema o amenaza que desata la aventura",
            deckQueries = listOf("acontecimiento", "acontecimientos", "gancho", "advent"),
            contentMode = CardContentMode.TOP_BOTTOM_SPLIT,
            description = "Roba 1 carta de Acontecimiento. Es el detonante urgente que rompe el equilibrio: una invasión inminente, la rotura de una presa mágica, una rebelión o el despertar de una bestia antigua.",
            creativeTip = "Consejo: Este paso es el punto de partida perfecto para que los PJs tomen decisiones o comiencen una misión.",
            sampleQuestions = listOf(
                "¿Cuánto tiempo queda antes de que la crisis sea irreversible?",
                "¿Qué ganancia o recompensa ofrece intervenir?",
                "¿Quién pide ayuda desesperadamente a los aventureros?"
            )
        )
    )

    val newStoryEngineSteps = listOf(
        StoryEngineStep(
            id = "nse_booster",
            stepIndex = 0,
            tab = StoryEngineGuideTab.NEW_STORY_ENGINE,
            title = "Paso 1: Agente / Potenciador (Quién)",
            subtitle = "El protagonista, antagonista o figura central del conflicto",
            deckQueries = listOf("potenciador", "booster", "potenciadores", "agente"),
            contentMode = CardContentMode.TOP_BOTTOM_SPLIT,
            description = "Roba 1 carta de Potenciadores (Boosters). Te proveerá de arquetipos, roles dramáticos y profesiones singulares (ej: Un Alquimista, Una Condesa, Un Detective, Un Cronoviajero).",
            creativeTip = "Consejo: El Agente no tiene por qué ser heroico; puede ser un antihéroe reacio o un sospechoso.",
            sampleQuestions = listOf(
                "¿Cuál es la habilidad más singular de este personaje?",
                "¿Qué reputación le precede?",
                "¿A quién teme más que a nadie?"
            )
        ),
        StoryEngineStep(
            id = "nse_backstory",
            stepIndex = 1,
            tab = StoryEngineGuideTab.NEW_STORY_ENGINE,
            title = "Paso 2: Trasfondo y Motivación (Por Qué)",
            subtitle = "El motor psicológico, trauma o deuda pendiente que lo impulsa",
            deckQueries = listOf("trasfondo", "backstory", "trasfondos"),
            contentMode = CardContentMode.FOUR_EDGE_CUES,
            description = "Roba 1 carta de Trasfondos. Con 4 orientaciones cardinales, otorga el motivo íntimo del personaje: expiar un crimen pasado, proteger un juramento o vengar a un mentor.",
            creativeTip = "Consejo: Rota la carta en 90° para conectar su trauma directamente con la situación actual de la mesa.",
            sampleQuestions = listOf(
                "¿Qué secreto inconfesable oculta a sus aliados?",
                "¿Qué juró no volver a hacer jamás?",
                "¿Qué precio está dispuesto a pagar por cumplir su meta?"
            )
        ),
        StoryEngineStep(
            id = "nse_mystery",
            stepIndex = 2,
            tab = StoryEngineGuideTab.NEW_STORY_ENGINE,
            title = "Paso 3: Misterio y Conflicto (Qué se Interpone)",
            subtitle = "El enigma, crimen o conspiración en el centro de la trama",
            deckQueries = listOf("misterio", "mystery", "misterios"),
            contentMode = CardContentMode.TOP_BOTTOM_SPLIT,
            description = "Roba 1 carta de Misterios. Plantea el móvil o el enigma central: un robo en una cámara sellada, una identidad falsa o un pacto clandestino que los personajes deben desenredar.",
            creativeTip = "Consejo: Combínalo con pistas del microsetting para anclar el misterio a una región específica.",
            sampleQuestions = listOf(
                "¿Qué pista evidente parece una trampa?",
                "¿Quién es el primer sospechoso inocente?",
                "¿Qué institución poderosa intenta enterrar la verdad?"
            )
        ),
        StoryEngineStep(
            id = "nse_item",
            stepIndex = 3,
            tab = StoryEngineGuideTab.NEW_STORY_ENGINE,
            title = "Paso 4: Objeto Único o Reliquia (Catalizador)",
            subtitle = "El objeto singular o herramienta extraordinaria que altera las reglas",
            deckQueries = listOf("objeto", "unique item", "objetos", "reliquia"),
            contentMode = CardContentMode.FOUR_EDGE_CUES,
            description = "Roba 1 carta de Objetos Únicos. Define un artefacto clave en la disputa: un tomo prohibido, un autómata averiado, una llave estelar o un veneno indetectable.",
            creativeTip = "Consejo: Rota 90° para descubrir las propiedades secundarias o el coste de activar el artefacto.",
            sampleQuestions = listOf(
                "¿Qué efecto imprevisto produce al ser activado?",
                "¿Quién reclama ser su legítimo dueño?",
                "¿Por qué no puede destruirse por medios convencionales?"
            )
        )
    )
}
