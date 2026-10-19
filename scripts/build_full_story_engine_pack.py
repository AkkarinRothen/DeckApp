"""Complete, high-fidelity Spanish translation and pack generator for Story Engine: Deck of Worlds.
"""
import json
import re
from pathlib import Path

ROOT = Path(r"i:\TTRPG\Visuales\Decks\Deckapp")
PACK_PATH = ROOT / "core/data/src/main/assets/starter_packs/story_engine_deck_of_worlds.json"
METADATA_PATH = ROOT / "core/data/src/main/assets/starter_packs/deck_of_worlds/cards_metadata.json"
RAW_CUES_PATH = Path(r"C:\Users\Giise\.gemini\antigravity-ide\brain\3591deda-8ce7-4918-a81a-6fdfe790dcc2\scratch\dow_raw_cues.json")

# Complete Spanish translations for Advents (Deck 106: 32 cards, indices 5 to 36)
# Each entry is (Zone 0: Inferior / 0°, Zone 1: Superior / 180°)
ADVENT_TRANSLATIONS = {
    5: (
        "Bajo asedio de una potencia extranjera: un ejército, bombardeo o guerra de propaganda",
        "La fauna muestra conductas anómalas: agresividad súbita, desorientación o frenesí"
    ),
    6: (
        "Un manifiesto revolucionario desata la agitación: exponiendo injusticias, avivando divisiones o revelando secretos de estado",
        "Fiebre por descubrir un mítico sepulcro antiguo: por fervor religioso, saqueo de tesoros o gloria personal"
    ),
    7: (
        "Unas excavaciones recientes desentierran un secreto: un cadáver incorrupto, una cámara acorazada o un túnel prohibido",
        "Refugiados acampan frente a graves adversidades: escasez de víveres, terreno inhóspito o rechazo de los nativos"
    ),
    8: (
        "Los lugareños enferman con extraños síntomas: letargo mortal, excrecencias anómalas o cambios radicales de conducta",
        "Una disputa territorial estalla: compra fraudulenta, reclamo ancestral o límites imprecisos"
    ),
    9: (
        "Proclaman su independencia de sus antiguos gobernantes: una nación soberana, un monarca o un gremio comercial",
        "Una especie invasora devasta el ecosistema: plaga vegetal, enjambre voraz o depredador foráneo"
    ),
    10: (
        "La muerte repentina del líder crea un vacío de poder: a disputar entre herederos, el consejo o sufragio popular",
        "Sismos recurrentes dejan al descubierto algo sepultado: una colmena subterránea, un abismo o una tumba maldita"
    ),
    11: (
        "El grupo dominante reescribe la historia: para ocultar un escándalo reciente, atrocidades del pasado o injusticias actuales",
        "El avistamiento de una criatura insólita conmociona la región: se cree mágica, legendaria o extinta"
    ),
    12: (
        "La hambruna azota y empeora velozmente: por especulación de precios, compras de pánico o motines por comida",
        "Una sociedad secreta celebra un cónclave: para una elección clandestina, sesión de emergencia o gran ritual"
    ),
    13: (
        "La muerte de un icono cultural conmueve a la población: duelo colectivo, pérdida de rumbo o sospechas de magnicidio",
        "El turismo masivo daña el entorno: los locales protestan, lucran o están profundamente divididos"
    ),
    14: (
        "Unas elecciones polarizadas dividen a la sociedad: el voto se fractura por un escándalo, un candidato bufón u odio ideológico",
        "Soldados sin bandera erigen un bastión: sospechosos de ser un ejército extranjero, una milicia rebelde o forajidos"
    ),
    15: (
        "Un colectivo oprimido lucha por sus derechos: igualdad de oportunidades o libertad de expresión cultural",
        "El hallazgo de un recurso valioso desata una fiebre de prospección: un mineral codiciado, combustible o una criatura viva"
    ),
    16: (
        "El desempleo alcanza un récord histórico: desatando protestas sociales, recesión económica o éxodo masivo de trabajadores",
        "Un fenómeno celestial insólito está a punto de manifestarse: un eclipse total, lluvia de meteoros o una supernova"
    ),
    17: (
        "Un avance científico desata una gran controversia: tachado de inmoral, temerario o sacrílego contra la tradición",
        "Un récord mundial está a punto de romperse: el ser vivo más gigantesco, temperatura récord o una hazaña física épica"
    ),
    18: (
        "Se declara la ley marcial: con toque de queda estricto, suspensión de derechos o reclutamiento forzoso",
        "Una extraña formación de nubes no deja de crecer: atribuida a polen venenoso, esporas o un suceso sobrenatural"
    ),
    19: (
        "Un líder populista gana poder con retórica inflamatoria: sembrando conflicto civil, xenofobia o adoctrinamiento",
        "Sonidos estremecedores resuenan en la noche: alaridos lejanos, excavaciones frenéticas o cantos guturales"
    ),
    20: (
        "Una figura preeminente es desenmascarada como un fraude: un prócer fundador, un ídolo cultural o un jerarca religioso",
        "Una sequía devastadora amenaza con consecuencias trágicas: deforestación masiva, extinción de fauna o hambruna"
    ),
    21: (
        "El destino de un prisionero político polariza a la población: un embajador extranjero, un disidente o el antiguo regente",
        "Una nueva comuna se asienta sin autorización: con un mandato de fervor místico, político o vanguardia artística"
    ),
    22: (
        "Desahucios masivos se ejecutan sin piedad: contra una clase desfavorecida, una minoría étnica o un bando opositor",
        "Inundaciones torrenciales anegan la comarca: arruinando cosechas, viviendas y desplazando a la fauna"
    ),
    23: (
        "Una huelga obrera paraliza toda la región: detonada por un accidente fatal, condiciones abusivas o un orador carismático",
        "Una plaga desconocida diezma el ecosistema: tizón en los cultivos, parásito neurológico o esporas virulentas"
    ),
    24: (
        "Estallan disturbios abiertos contra la injusticia: desigualdad asfixiante, tiranía despótica o una ley opresiva",
        "El enfrentamiento del siglo está a punto de celebrarse: duelo a muerte, apuesta colosal o certamen deportivo"
    ),
    25: (
        "Se exigen reparaciones históricas a una antigua potencia colonial: indemnización económica, restitución de tierras o disculpas oficiales",
        "Una gran migración estacional toma un rumbo imprevisto: manadas desbocadas, desvío de ruta o asentamiento permanente"
    ),
    26: (
        "Un profeta asegura hablar en nombre de un nuevo dios: exigiendo devoción ciega, tributos periódicos o reforma de vida",
        "Se descubre una grave contaminación en las aguas: organismos mutantes, putrefacción cadavérica o vertidos tóxicos"
    ),
    27: (
        "Un héroe local resulta ser una farsa: exageró sus hazañas, lleva una vida privada deleznable o es un impostor",
        "El clima se transforma de forma destructiva y súbita: calor abrasador, heladas extremas o tormentas anómalas"
    ),
    28: (
        "Debaten si demoler un símbolo histórico de opresión: un monumento conmemorativo, fortaleza imperial o ley arcaica",
        "El último custodio de una tradición milenaria agoniza: un maestro artesano, un bardo ancestral o el último hablante de una lengua"
    ),
    29: (
        "Una megaconstrucción está al borde del colapso: escasez de materiales, fuga de inversores o negligencia laboral",
        "Una especie crucial se encuentra al filo de la extinción: venerada como sagrada, acosada por cazadores furtivos o sin hábitat"
    ),
    30: (
        "Una reliquia cultural invaluable ha desaparecido: un tratado fundacional, una obra maestra de arte o un botín expoliado",
        "Un erudito respetado lanza un vaticinio perturbador: sobre la ruina económica, una hecatombe climática o fuerzas sobrenaturales"
    ),
    31: (
        "Una ley controvertida suscita violentos debates: tachada de discriminatoria, cortoplacista o dictada por intereses corporativos",
        "El territorio de caza de un superdepredador se desplaza peligrosamente: aproximándose a los ríos, al ganado o a los poblados"
    ),
    32: (
        "Las cloacas se desbordan descontroladamente: con residuos hediondos, alimañas carroñeras o marginados desterrados",
        "La cosecha de toda la temporada corre riesgo de perderse: por plagas voraces, inclemencias del tiempo o sabotaje criminal"
    ),
    33: (
        "Una influyente facción se fractura en dos bandos hostiles: por un cisma teológico, rivalidad dinástica o discordia cultural",
        "Una carrera de altísimo riesgo está a punto de comenzar: a pie, sobre bestias veloces o sobre aguas turbulentas"
    ),
    34: (
        "Un artista rebelde va a revelar una obra incendiaria: rompiendo un tabú ancestral, visibilizando la miseria o condenando al régimen",
        "Los presagios de una antigua profecía empiezan a cumplirse: signos celestiales, fauna desorientada o anomalías atmosféricas"
    ),
    35: (
        "Una manifestación cultural emblemática ha sido prohibida: un dialecto nativo, culto a los ancestros o juego tradicional",
        "Un multitudinario festival abarrotará la región: concierto épico, feria de las artes o bacanal sagrada"
    ),
    36: (
        "Los tesoros de un archivo crucial son tomados como rehenes: una biblioteca histórica, un museo de reliquias o una pinacoteca",
        "Las estrellas se desvanecen una a una del firmamento: por una bruma tóxica, una ilusión óptica o una maldición estelar"
    )
}

# Complete Spanish translations for Origins (Deck 104: 32 cards, indices 3 to 34)
# Each entry is (Zone 0: Sur / 0°, Zone 1: Oeste / 90°, Zone 2: Norte / 180°, Zone 3: Este / 270°)
ORIGIN_TRANSLATIONS = {
    3: (
        "Hogar de un pueblo desaparecido",
        "Fundado por parias y marginados",
        "Último paradero conocido de una reliquia milenaria",
        "Se dice que fue la morada de los dioses"
    ),
    4: (
        "Lugar de un célebre descubrimiento científico",
        "Erigido para cumplir un juramento solemne",
        "Escenario de una legendaria apuesta",
        "Modelado por siglos de conflicto bélico"
    ),
    5: (
        "Lugar de un infame robo histórico",
        "Antiguo centro de comercio e intercambio",
        "Antiguo hábitat de una especie extinta",
        "Transformado por aludes y deslizamientos"
    ),
    6: (
        "Cuna de un movimiento filosófico",
        "Fundado por náufragos o supervivientes",
        "Construido sobre un cementerio antiguo",
        "Escenario de una histórica victoria militar"
    ),
    7: (
        "Sitio de una antigua rebelión aplastada",
        "Fundado por una orden mística errante",
        "Antiguo coto sagrado de caza real",
        "Formado por la caída de un bólido celeste"
    ),
    8: (
        "Escenario de un trágico asedio que duró décadas",
        "Construido como refugio ante un cataclismo",
        "Lugar donde se firmó un armisticio sagrado",
        "Alterado por una erupción volcánica colosal"
    ),
    9: (
        "Antigua colonia penitenciaria olvidada",
        "Fundado por exploradores extraviados",
        "Supuesto lugar de nacimiento de una deidad",
        "Forjado por el curso desviado de un río sagrado"
    ),
    10: (
        "Sitio de un pacto secreto entre imperios",
        "Erigido por antiguos gigantes o constructores míticos",
        "Antiguo centro de alquimia y nigromancia",
        "Moldeado por un fuego que ardió durante un siglo"
    ),
    11: (
        "Lugar de martirio de un héroe o profeta",
        "Fundado como puesto comercial clandestino",
        "Antigua necrópolis de reyes olvidados",
        "Resurgido de las aguas tras un gran terremoto"
    ),
    12: (
        "Escenario de una cruenta guerra de secesión",
        "Fundado por colonos de ultramar",
        "Lugar donde se forjó un artefacto legendario",
        "Transformado por la tala de un bosque milenario"
    ),
    13: (
        "Sitio donde se extinguió el último dragón o coloso",
        "Construido como palacio de verano de un tirano",
        "Antiguo enclave diplomático neutral",
        "Surcado de cavernas excavadas por bestias primitivas"
    ),
    14: (
        "Cuna de una herejía que sacudió el continente",
        "Fundado por desertores de una gran cruzada",
        "Lugar donde reposan las cenizas de una dinastía",
        "Aislado del resto del mundo durante un milenio"
    ),
    15: (
        "Sitio de un misterioso eclipse perpetuo en la antigüedad",
        "Construido para proteger un manantial milagroso",
        "Antiguo bastión fronterizo contra hordas salvajes",
        "Modelado por el deshielo de glaciares ancestrales"
    ),
    16: (
        "Escenario de un juicio histórico que cambió las leyes",
        "Fundado por una cofradía de artesanos proscritos",
        "Lugar de una legendaria batalla naval o fluvial",
        "Consumido en el pasado por un miasma venenoso"
    ),
    17: (
        "Antiguo santuario de peregrinaje prohibido",
        "Construido sobre las ruinas de una civilización sumergida",
        "Sitio donde se avistaron portentos cósmicos",
        "Excavado en la roca viva por generaciones de esclavos"
    ),
    18: (
        "Originalmente una escuela o academia de saber",
        "Alguna vez fue el lecho de un lago ancestral",
        "Fundado por veterano(s) de una guerra olvidada",
        "Supuesto sepulcro de un héroe de baladas populares"
    ),
    19: (
        "Cuna de un estilo arquitectónico revolucionario",
        "Fundado por mercaderes en busca de rutas de especias",
        "Lugar donde se desató una plaga mágica legendaria",
        "Modelado por vientos huracanados incesantes"
    ),
    20: (
        "Escenario de una coronación clandestina",
        "Erigido como trinchera durante una invasión bárbara",
        "Antiguo observatorio de sabios astrónomos",
        "Marcado por el impacto de un cometa sagrado"
    ),
    21: (
        "Sitio de un milagroso acto de salvación comunitaria",
        "Fundado por una secta que buscaba el fin del mundo",
        "Antiguo mercado negro de reliquias y esclavos",
        "Desecado tras el desvío intencionado de sus fuentes"
    ),
    22: (
        "Cuna de un legendario estilo de combate marcial",
        "Fundado para la defensa militar estratégica",
        "Origen de una infame táctica bélica prohibida",
        "Descubierto tras el impacto fortuito de un rayo"
    ),
    23: (
        "Antigua superpotencia militar venida a menos",
        "Fundado por un criminal arrepentido en busca de expiación",
        "Erigido o entregado como tributo de paz entre señores",
        "Modelado por violenta actividad tectónica"
    ),
    24: (
        "Origen de un idioma ancestral hoy en peligro de extinción",
        "Creado por un error de cartografía o cálculo",
        "Adquirido en un trato fraudulento que pasó a la historia",
        "Surgido aparentemente de la noche a la mañana"
    ),
    25: (
        "Territorio escindido de un imperio mayor tras una revuelta",
        "Fundado por una figura borrada deliberadamente de la historia",
        "Escenario de una épica y dolorosa traición",
        "Antaño un desierto árido antes de florecer"
    ),
    26: (
        "Originalmente un campamento estacional provisional",
        "Erigido sobre tierras arrebatadas a sus legítimos dueños",
        "Escenario de una conspiración milenaria secreta",
        "Antiguamente cubierto por un espeso manto de musgo"
    ),
    27: (
        "Sitio de una histórica y célebre protesta popular",
        "Fundado a raíz de una broma o una apuesta temeraria",
        "Intercambiado repetidamente entre naciones en guerra",
        "Antaño praderas y pastizales sin fin"
    ),
    28: (
        "Considerado una de las grandes maravillas del mundo antiguo",
        "Fundado por una compañía itinerante de comediantes y bardos",
        "Su verdadero nombre original se ha perdido en el tiempo",
        "Profundamente modelado por el yugo colonial"
    ),
    29: (
        "Brindó refugio providencial durante un desastre histórico",
        "Modelado por décadas de ocupación militar extranjera",
        "Lugar de un infame y sangriento magnicidio",
        "Sacudido y reconfigurado por un terremoto devastador"
    ),
    30: (
        "Citado en las crónicas como un audaz experimento social",
        "Fundado por un líder carismático recordado con fervor",
        "Vinculado a mitos y leyendas sobre el reino de ultratumba",
        "Alguna vez fue considerado un auténtico paraíso terrenal"
    ),
    31: (
        "Mencionado en leyendas, pero su ubicación exacta permaneció oculta",
        "Fundado por un insólito gremio de artesanos o proscritos",
        "Lugar de un banquete de bodas con sangriento desenlace",
        "Terreno industrial recuperado y devuelto a la naturaleza"
    ),
    32: (
        "Lugar de nacimiento de un célebre mandatario histórico",
        "Diseñado originalmente como una trampa o emboscada mortal",
        "Escenario de un accidente colosal que marcó a generaciones",
        "Antiguo humedal drenado por la mano humana"
    ),
    33: (
        "Cuna de una obra maestra del arte que asombró al mundo",
        "Fundado según la leyenda por un niño visionario",
        "Origen de un enigma milenario que jamás fue resuelto",
        "Modelado por el cráter de una colisión o explosión descomunal"
    ),
    34: (
        "Fue diezmado en el pasado por una peste implacable",
        "Construido íntegramente con dinero manchado de sangre",
        "Escenario de un gigantesco y trágico malentendido histórico",
        "Devastado y transformado para siempre por una tormenta colosal"
    )
}

WORD_MAP = {
    # Regions
    "WOOD(S)": "Bosque(s)", "DESERT": "Desierto", "DELTA": "Delta", "OCEAN": "Océano",
    "SEA": "Mar", "MOUNTAINS": "Montañas", "MOUNTAIN(S)": "Montaña(s)", "TUNDRA": "Tundra",
    "SWAMP": "Pantano", "MARSH": "Ciénaga", "PLAINS": "Llanura", "GRASSLAND": "Pradera",
    "JUNGLE": "Selva", "RAINFOREST": "Bosque lluvioso", "CANYON": "Cañón", "ISLAND(S)": "Isla(s)",
    "VOLCANO": "Volcán", "CAVERNS": "Cavernas", "UNDERGROUND": "Subterráneo", "VALLEY": "Valle",
    "COAST": "Costa", "CLIFFS": "Acantilados", "GLACIER": "Glaciar", "STEPPE": "Estepa",
    "SAVANNA": "Sabana", "RIVER": "Río", "HIGHLANDS": "Tierras altas", "PLATEAU": "Meseta",
    "WASTELAND": "Yermo", "HILLS": "Colinas", "LAKE": "Lago", "REEF": "Arrecife",
    "ATOLL": "Atolón", "MOOR": "Páramo", "CRATER": "Cráter", "WETLANDS": "Humedales",
    "FJORD(S)": "Fiordo(s)", "ARCHIPELAGO": "Archipiélago", "BADLANDS": "Tierras baldías",
    "OASIS": "Oasis", "TAIGA": "Taiga", "ESTUARY": "Estuario", "GORGE": "Garganta",

    # Landmarks
    "WORKSHOP": "Taller", "TREE": "Árbol ancestral", "WATERFALL": "Cascada",
    "ZOO": "Casa de fieras / Zoológico", "STREAM": "Arroyo", "WALL": "Muralla",
    "BRIDGE": "Puente", "TOWER": "Torre", "SHRINE": "Santuario", "MINE": "Mina",
    "INN": "Posada", "TAVERN": "Taberna", "CASTLE": "Castillo", "CITADEL": "Ciudadela",
    "CEMETERY": "Cementerio", "TOMB": "Tumba", "MARKET": "Mercado", "BAZAAR": "Bazar",
    "MONUMENT": "Monumento", "MILL": "Molino", "TEMPLE": "Templo", "LIGHTHOUSE": "Faro",
    "BEACON": "Almenara", "GATE": "Puerta monumental", "PASS": "Paso de montaña",
    "OUTPOST": "Puesto de avanzada", "FORT": "Fuerte", "HARBOR": "Puerto",
    "DOCKS": "Muelles", "LABYRINTH": "Laberinto", "MAZE": "Dédalo", "ARCHIVE": "Archivo",
    "LIBRARY": "Gran biblioteca", "CAVE": "Cueva", "CLIFF": "Acantilado",
    "PALACE": "Palacio", "SANCTUARY": "Santuario", "STATUE": "Estatua colosal",
    "OBELISK": "Obelisco", "RUINS": "Ruinas", "CAMP": "Campamento", "VILLAGE": "Aldea",
    "ARENA": "Arena de combate", "PRISON": "Prisión", "DUNGEON": "Mazmorra",
    "GARDEN": "Jardín botánico", "ORCHARD": "Huerto", "WELL": "Pozo sagrado",
    "FOUNTAIN": "Fuente monumental", "MONASTERY": "Monasterio", "ABBEY": "Abadía",
    "ACADEMY": "Academia", "FOUNDRY": "Fundición", "FORGE": "Forja",
    "GRAVEYARD": "Camposanto", "VAULT": "Cámara acorazada", "SEWERS": "Cloacas",
    "CATACOMBS": "Catacumbas", "BARRACKS": "Cuartel", "OBSERVATORY": "Observatorio",
    "COLOSSUS": "Coloso", "AMPHITHEATER": "Anfiteatro", "MENHIR": "Menhir",
    "MEGALITH": "Megalito", "PYRAMID": "Pirámide", "SHIPWRECK": "Naufragio",
    "SPILLED CARGO": "Cargamento disperso", "QUARRY": "Cantera", "WATCHTOWER": "Atalaya",
    "CARAVAN": "Caravana detenida", "SETTLEMENT": "Asentamiento", "MANOR": "Mansión señorial",
    "ESTATE": "Hacienda", "GROVE": "Arboleda sagrada", "HOT SPRING(S)": "Aguas termales",
    "GEYSER": "Géiser", "FARM": "Granja", "RANCH": "Rancho", "BAKERY": "Panadería",
    "SMITHY": "Herrería", "APOTHECARY": "Boticario / Herbolario", "CLOISTER": "Claustro",
    "ROADSIDE SHRINE": "Altar del camino", "TOWNSHIP": "Pueblo", "MEETING HALL": "Salón comunal",

    # Namesake epithets
    "WINDY": "Ventoso", "WHERE NOTHING GROWS": "Donde nada crece", "CROOKED": "Torcido",
    "UNDER HEAVEN": "Bajo el cielo", "WINKING": "Guiñante", "THAT WATCHES": "Que vigila",
    "UNDAUNTED": "Impertérrito / Valeroso", "WHERE NO BIRD SINGS": "Donde ningún pájaro canta",
    "LAST": "Último", "THAT WENT TOO FAR": "Que fue demasiado lejos", "HIGH": "Alto / Elevado",
    "THAT DREAMS": "Que sueña", "GOD-TOUCHED": "Tocado por los dioses",
    "BEYOND HOPE": "Más allá de la esperanza", "WHISPERING": "Susurrante", "BURNING": "Ardiente",
    "FROZEN": "Congelado", "FORGOTTEN": "Olvidado", "LOST": "Perdido", "HOLLOW": "Hueco",
    "SILENT": "Silencioso", "CRACKED": "Agrietado", "SUNKEN": "Hundido", "SLEEPING": "Durmiente",
    "BOUND": "Atado / Encadenado", "BLEEDING": "Sangrante", "SHROUDED": "Envuelto en sombras",
    "ANCIENT": "Antiguo", "FALLEN": "Caído", "RISING": "Ascendente", "SHINING": "Resplandeciente",
    "DARKENED": "Oscurecido", "BLOODY": "Sanguinolento", "GOLDEN": "Dorado", "SILVER": "Plateado",
    "IRON": "Férreo", "STONE": "Pétreo", "BLESSED": "Bendito", "CURSED": "Maldito",
    "SACRED": "Sagrado", "UNHOLY": "Profano", "WEEPING": "Lloroso", "HOWLING": "Aullante",
    "ROARING": "Rugiente", "SINGING": "Cantarín", "SMILING": "Sonriente", "LAUGHING": "Riente",
    "HUNGRY": "Hambriento", "ENDLESS": "Infinito", "TIMELESS": "Eterno", "RESTLESS": "Inquieto",
    "BROKEN": "Quebrantado", "WITHERED": "Marchito", "TWISTED": "Retorcido", "BARREN": "Estéril",
    "DROWNED": "Ahogado", "BURIED": "Enterrado", "HAUNTED": "Embrujado", "POLLUTED": "Contaminado",

    # Attributes
    "ELABORATE ETIQUETTESYSTEM": "Elaborado protocolo social",
    "ELABORATE ETIQUETTE SYSTEM": "Elaborado protocolo social",
    "SKILLED FORAGERS": "Recolectores experimentados",
    "SITE OF AN AUDITORY PHENOMENON": "Lugar de un fenómeno acústico misterioso",
    "HABITAT OF A RARE INSECT": "Hábitat de una especie rara de insecto",
    "THEOCRATIC GOUERNMENT": "Gobierno teocrático",
    "THEOCRATIC GOVERNMENT": "Gobierno teocrático",
    "HAUEN FOR ARTISTS": "Refugio para artistas",
    "HAVEN FOR ARTISTS": "Refugio para artistas",
    "CONSTANTLY CLOUDY": "Perennemente nublado",
    "PILGRIMAGE DESTINATION": "Destino de peregrinación",
    "UNUSUAL ELECTION PROCESS": "Proceso electoral insólito",
    "HUNTING GROUND": "Coto de caza",
    "KNOWN FOR FOSSIL(S)": "Famoso por sus fósiles",
    "PACIFISTIC": "Sociedad pacifista",
    "NO WRITTEN LANGUAGE": "Sin lenguaje escrito",
    "SHROUDED IN MYTH": "Envuelto en el mito",
    "USED AS A SANCTUARY": "Utilizado como santuario",
    "CULTURALLY SPECIFIC FAMILY STRUCTURE": "Estructura familiar singular",
    "ECCENTRICLEADER": "Líder excéntrico",
    "ECCENTRIC LEADER": "Líder excéntrico",
    "GOOD WALKING TRAILS": "Excelentes senderos de travesía",
    "KNOWN FOR A SPECIFICSALT": "Famoso por una sal medicinal única",
    "KNOWN FOR A SPECIFIC SALT": "Famoso por una sal medicinal única",
    "INCOME INEQUALITY": "Marcada desigualdad de riqueza",
    "ICONICTOWER(S)": "Torre(s) emblemática(s)",
    "ICONIC TOWER(S)": "Torre(s) emblemática(s)",
    "DOTTED WITH GRAUES": "Salpicado de tumbas",
    "DOTTED WITH GRAVES": "Salpicado de tumbas",
    "HABITAT OF A VENOMOUS SPECIES": "Hábitat de una especie venenosa",
    "DISTINCTIUE ARCHITECTURE": "Arquitectura distintiva",
    "DISTINCTIVE ARCHITECTURE": "Arquitectura distintiva",
    "FAMOUSFOR CONFECTIONERY": "Famoso por su repostería tradicional",
    "FAMOUS FOR CONFECTIONERY": "Famoso por su repostería tradicional",
    "PROTECTED CULTURAL SITE": "Sitio cultural protegido",
    "KNOWN FOR POND(S)": "Conocido por sus estanques",
    "FREQUENTLY AT WAR": "Frecuentemente en guerra",
    "LOCATED ABOVEA TUNNEL SYSTEM": "Asentado sobre una red de túneles",
    "LOCATED ABOVE A TUNNEL SYSTEM": "Asentado sobre una red de túneles",
    "POPULAR HOLIDAY SPOT": "Popular destino de vacaciones y retiro",
    "KNOWN FOR A PRECIOUS HERB": "Famoso por una hierba curativa preciosa",
    "WEALTHY": "Región próspera y acaudalada",
    "KNOWN FOR WEAUING": "Célebre por sus tejidos y telares",
    "KNOWN FOR WEAVING": "Célebre por sus tejidos y telares",
    "SWELTERING HOT IN SUMMER": "Calor abrasador en verano",
    "HAUEN FOR OUTCASTS AND EXILES": "Refugio de parias y desterrados",
    "HAVEN FOR OUTCASTS AND EXILES": "Refugio de parias y desterrados",
    "VALUES CREATIVITY": "Rinde culto a la creatividad",
    "KNOWN FOR UNUSUAL PET(S)": "Famoso por criar mascotas insólitas",
    "CONSIDERED SACRED GROUND": "Considerado suelo sagrado",
    "KNOWN FOR ITS FRAGRANCE": "Famoso por su fragancia aromática",
    "FREQUENT CHANGES OF LEADERSHIP": "Frecuentes cambios de gobierno",
    "FAMOUS FOR DUMPLINGS": "Célebre por sus albóndigas / empanadas",
    "COVERED INTREES": "Cubierto de frondosos árboles",
    "COVERED IN TREES": "Cubierto de frondosos árboles",
    "DISTINCTIUE REGIONAL CROP": "Cultivo regional característico",
    "DISTINCTIVE REGIONAL CROP": "Cultivo regional característico",
    "SOCIAL EQUALITY": "Igualdad social arraigada",
    "KNOWN FOR ONE LURID DISTRICT": "Conocido por un sórdido barrio clandestino",
    "STRONG WINDS": "Azotado por fuertes vientos",
    "HOME OF A TITAN OFINDUSTRY": "Hogar de un magnate de la industria",
    "HOME OF A TITAN OF INDUSTRY": "Hogar de un magnate de la industria",
    "VALUES SELF-DETERMINATION": "Valora la autodeterminación individual",
    "CULTURALLYSPECIFIC TATTOO(S)": "Tatuajes corporales tradicionales",
    "CULTURALLY SPECIFIC TATTOO(S)": "Tatuajes corporales tradicionales",
    "SHORT OR LONG GROWING SEASON": "Temporada de cosecha inusualmente corta o larga",
    "HOME OFA RELIGIOUS SORDER": "Sede de una estricta orden monástica",
    "HOME OF A RELIGIOUS ORDER": "Sede de una estricta orden monástica",
    "REGIONALLY SPECIFIC BREAD": "Panadería artesanal tradicional",
    "REGIONALLY SPECIFICSAUCE": "Salsa culinaria típica de la región",
    "REGIONALLY SPECIFIC SAUCE": "Salsa culinaria típica de la región",
    "FESTIUAL SITE": "Sede de festivales multitudinarios",
    "FESTIVAL SITE": "Sede de festivales multitudinarios",
    "LOCATED ON A HILLTOP": "Erigido sobre la cima de una colina",
    "VALUES MENTAL HEALTH": "Prioriza el bienestar mental y espiritual",
    "ORNATE ROOFTOP(S)": "Tejados fastuosos y ornamentados",
    "FAMOUS FORITS UNUSUAL HUE OF SOIL OR STONE": "Famoso por el insólito color de su tierra o roca",
    "FAMOUS FOR ITS UNUSUAL HUE OF SOIL OR STONE": "Famoso por el insólito color de su tierra o roca",
    "KNOWN FOR A SPECIAL BERRY": "Famoso por una baya silvestre única",
    "COMPOSED OFAN UNUSUAL MATERIAL": "Edificado con un material extraordinario",
    "COMPOSED OF AN UNUSUAL MATERIAL": "Edificado con un material extraordinario",
    "KNOWN FOR PASTRIES": "Famoso por su repostería fina",
    "POWERFUL STORMS": "Azotado por tempestades colosales",
    "HOMEOFAN ECCENTRIC HERMIT": "Hogar de un sabio ermitaño excéntrico",
    "HOME OF AN ECCENTRIC HERMIT": "Hogar de un sabio ermitaño excéntrico",
    "VALUES ELDERS": "Veneración y respeto a los ancianos",
    "REGIONAL ACCENT": "Acento y entonación regional inconfundible",
    "KNOWN FOR ORCHARD(S)": "Célebre por sus huertos frutales",
    "HABITAT OF AN ENDANGERED ANIMAL": "Hábitat de un animal en peligro crítico",
    "VALUES UNITY": "Rinde culto a la unidad y la comunidad",
    "DISTINCTIUE FOOTWEAR": "Calzado artesanal característico",
    "DISTINCTIVE FOOTWEAR": "Calzado artesanal característico",
    "KNOWN FOR RUIN(S)": "Famoso por sus ruinas ancestrales",
    "KNOWN FOR REGIONAL LEGUME": "Conocido por una legumbre típica",
    "MULTICULTURAL": "Comunidad abierta y multicultural",
    "KNOWN FOR BOOKMAKING": "Famoso por la imprenta y encuadernación de libros",
    "RICH OR UNUSUAL SOIL": "Tierra excepcionalmente rica y fértil",
    "HOME OF A PRIZED HORSEBREED": "Cuna de una cotizada raza de caballos",
    "HOME OF A PRIZED HORSE BREED": "Cuna de una cotizada raza de caballos",
    "RUN BY MILITARY": "Gobernado bajo junta militar",
    "VEGETARIAN DIET": "Dieta estrictamente vegetariana",
    "BEAUTIFUL GRASSES": "Praderas de hierba deslumbrante",
    "RITUAL SITE": "Sitio de rituales y ceremonias",
    "KNOWNFOR FASHION ACCESSORIES": "Célebre por accesorios de moda y joyas",
    "KNOWN FOR FASHION ACCESSORIES": "Célebre por accesorios de moda y joyas",
    "GENDER EXPECTATIONS UNFAMILIAR TO OUTSIDERS": "Roles de género insólitos para forasteros",
    "ABUNDANT ENERGY SOURCE": "Fuente inagotable de energía",
    "HABITAT OF AN APEXPREDATOR": "Territorio de un feroz superdepredador",
    "HABITAT OF AN APEX PREDATOR": "Territorio de un feroz superdepredador",
    "GOOD HEALTH CARE": "Excelente medicina y sanación tradicional",
    "REGIONALLY SPECIFIC CHEESE": "Queso artesanal curado de la región",
    "REGIONALLY SPECIFIC SPICE": "Especia aromática de gran valor",
    "KNOWN FOR WILDFLOWER(S)": "Famoso por sus flores silvestres",
    "RESTRICTIVE OF FREE SPEECH": "Libertad de expresión severamente censurada",
    "KNOWN FOR PLACES OF WORSHIP": "Famoso por sus templos y santuarios",
    "UNUSUAL MOSS": "Variedad insólita de musgo luminiscente",
    "PRIVATE ESTATE": "Feudo y hacienda privada señorial",
    "KNOWN FOR SPICY FOOD": "Gastronomía famosa por su picante intenso",
    "POPULAR BLOODSPORT": "Deportes sangrientos muy populares",
    "KNOWN FOR HONEY": "Célebre por su miel pura y aromática",
    "REGIONALLY SPECIFIC CATBREED": "Raza felina autóctona de la región",
    "REGIONALLY SPECIFIC CAT BREED": "Raza felina autóctona de la región",
    "UNIQUE EDUCATION SYSTEM": "Sistema pedagógico único e innovador",
    "REGIONAL TABOO": "Fuerte tabú cultural ancestral",
    "GEOLOGICALLY UNSTABLE": "Terreno geológicamente inestable",
    "KNOWN FOR RAREGOURD(S)": "Famoso por el cultivo de calabazas exóticas",
    "KNOWN FOR RARE GOURD(S)": "Famoso por el cultivo de calabazas exóticas",
    "BARTER SYSTEM": "Economía basada en el trueque",
    "KNOWN FOR PROHIBITION": "Rigurosa ley seca y prohibición",
    "ROLLING HILLS": "Ondulantes colinas verdes",
    "STRAY ANIMAL COLONY": "Gran colonia de animales callejeros protegidos",
    "HIGH CRIME RATE": "Elevada criminalidad y forajidos",
    "STRICT SILENCE EXPECTED ATALLTIMES": "Silencio estricto obligatorio en todo momento",
    "STRICT SILENCE EXPECTED AT ALL TIMES": "Silencio estricto obligatorio en todo momento",
    "SOURCE OFA RARE FRUIT": "Origen de una codiciada fruta exótica",
    "SOURCE OF A RARE FRUIT": "Origen de una codiciada fruta exótica",
    "SITE OF NATURE WORSHIP": "Lugar de culto y veneración a la naturaleza",
    "COMPLEX LEGAL SYSTEM": "Sistema judicial laberíntico y complejo",
    "SKILLED SCULPTOR(S)": "Escultores y talladores de renombre",
    "FREEZING COLD IN WINTER": "Frío gélido e implacable en invierno",
    "USED ASA BORDER MARKER": "Utilizado como mojón y límite fronterizo",
    "USED AS A BORDER MARKER": "Utilizado como mojón y límite fronterizo",
    "FAMOUS FOR ARCHIVE(S)": "Famoso por sus archivos y códices",
    "KNOWN FORA": "Famoso por un plato típico tradicional",
    "LOCATED ABOVE": "Situado sobre un canal navegable",
    "HEADQUARTERS OF A CRIME FAMILY": "Sede central de una poderosa mafia familiar",
    "KNOWN FOR BODY NEUTRALITY": "Cultura de neutralidad corporal y aceptación",
    "REGIONALLY SPECIFIC BEVERAGE": "Bebida fermentada típica de la comarca",
    "DISTINCTIUE CARUINGS": "Tallas y grabados artesanales únicos",
    "DISTINCTIVE CARVINGS": "Tallas y grabados artesanales únicos",
    "KNOWN FOR CACTI": "Conocido por sus cactus gigantes",
    "FAMOUS FOR COSMETICS": "Célebre por afeites y cosméticos naturales",
    "BUSTLING MARKET(S)": "Mercados bullangueros y concurridos",
    "RICH IN FOLKLORE": "Rico en leyendas, mitos y folclore",
    "KNOWN FOR MUSHROOM(S)": "Famoso por sus setas y hongos comestibles",
    "UNIQUE SYSTEM OF TIMEKEEPING": "Sistema propio de cómputo del tiempo",
    "KNOWN FOR REVELRY": "Célebre por sus fiestas y juergas desenfrenadas",
    "BEAUTIFUL WINTERS OR SUMMERS": "Inviernos de ensueño y veranos radiantes",
    "KNOWN FOR TEA": "Famoso por sus plantaciones de té selecto",
    "FAMOUS LOCAL DELICACY": "Célebre por un manjar gastronómico autóctono",
    "REGIONALDIALECT": "Dialecto propio con giros incomprensibles",
    "REGIONAL DIALECT": "Dialecto propio con giros incomprensibles",
    "KNOWN FOR HEALING PROPERTIES": "Famoso por sus aguas y propiedades curativas",
    "MASSIVE BIRD COLONY": "Inmensa colonia de aves marinas migratorias",
    "HIGH TAXES": "Tributos y gravámenes asfixiantes",
    "REGIONALLY SPECIFICDYE(S)": "Tintes textiles tradicionales exclusivos",
    "REGIONALLY SPECIFIC DYE(S)": "Tintes textiles tradicionales exclusivos",
    "SOURCE OFA VALUABLE ORE": "Yacimiento de un mineral de altísimo valor",
    "SOURCE OF A VALUABLE ORE": "Yacimiento de un mineral de altísimo valor",
    "ICONIC LAUA FORMATION(S)": "Formaciones de lava volcánica emblemáticas",
    "ICONIC LAVA FORMATION(S)": "Formaciones de lava volcánica emblemáticas",
    "BUSY NIGHTLIFE": "Animada y bulliciosa vida nocturna",
    "PRIZED SOURCE OFMEAT": "Preciada fuente de carne de caza selecta",
    "PRIZED SOURCE OF MEAT": "Preciada fuente de carne de caza selecta",
    "HABITAT OF A SUBTERRANEAN CREATURE": "Hábitat de una mítica criatura subterránea",
    "SOURCE OF NATURALDYE(S)": "Fuente natural de pigmentos y tinturas",
    "SOURCE OF NATURAL DYE(S)": "Fuente natural de pigmentos y tinturas",
    "MULTIPLE WRITING SYSTEMS": "Convivencia de múltiples sistemas de escritura",
    "KNOWN FOR LOUE OF DANCE": "Famoso por su desbordante pasión por la danza",
    "KNOWN FOR LOVE OF DANCE": "Famoso por su desbordante pasión por la danza",
    "LOCATED ON A GEOLOGICAL FAULT": "Asentado sobre una falla geológica activa",
    "HABITAT OF STRANGE SYMBIOTES": "Hábitat de insólitos organismos simbiontes",
    "KNOWN BY MANY NAMES": "Conocido por una miríada de nombres distintos",
    "CULTURALLY SPECIFIC HAIRSTYLE(S)": "Peinados tradicionales y trenzados ceremoniales",
    "BY CANALS": "Rodeado y cruzado por canales navegables",
    "ABUNDANT POISONOUS PLANTS": "Abundancia de plantas y hierbas venenosas",
    "STRICT DIETARY RULES": "Estrictas restricciones y preceptos alimentarios",
    "FREQUENT COUPS": "Frecuentes alzamientos y golpes de estado",
    "FREQUENT EARTHQUAKES": "Temblores y seísmos habituales",
    "USED FOR COMING OFAGE RITUALS": "Escenario de rituales de iniciación a la adultez",
    "USED FOR COMING OF AGE RITUALS": "Escenario de rituales de iniciación a la adultez",
    "KNOWN FOR MUSIC": "Cuna de maestros músicos y juglares",
    "CULTURALLY SPECIFIC EXPRESSION(S) OF GENDER": "Expresiones culturales propias del género",
    "AGGRESSIUE WILDLIFE": "Fauna territorial y agresiva",
    "AGGRESSIVE WILDLIFE": "Fauna territorial y agresiva",
    "FLEETING SEASONAL BLOSSOMS": "Efímera y deslumbrante floración primaveral",
    "KNOWN FOR NEUTRALITY IN ALL CONFLICTS": "Neutralidad inquebrantable en todo conflicto",
    "UNUSUAL FORM OFPUBLIC TRANSPORTATION": "Singular sistema de transporte público",
    "UNUSUAL FORM OF PUBLIC TRANSPORTATION": "Singular sistema de transporte público",
    "UOLCANICALLY ACTIUE": "Zona volcánicamente activa con fumarolas",
    "VOLCANICALLY ACTIVE": "Zona volcánicamente activa con fumarolas",
    "KNOWN FOR MUSICALINSECT(S)": "Famoso por sus insectos de canto melodioso",
    "KNOWN FOR MUSICAL INSECT(S)": "Famoso por sus insectos de canto melodioso",
    "ABSENT LEADER": "Gobernante ausente o desaparecido",
    "KNOWN FOR DECORATED DOORWAY(S)": "Célebre por sus portales y puertas ricamente decoradas",
    "KNOWN FOR RARE WILDLIFE": "Hogar de fauna silvestre exótica e insólita",
    "FAMOUS FOR UINEYARD(S)": "Famoso por sus viñedos y caldos añejos",
    "FAMOUS FOR VINEYARD(S)": "Famoso por sus viñedos y caldos añejos",
    "FAMOUSSPORTS TEAM OR LEAGUE": "Sede de un legendario equipo o liga deportiva",
    "FAMOUS SPORTS TEAM OR LEAGUE": "Sede de un legendario equipo o liga deportiva",
    "PRIZED SOURCE OFDAIRY": "Preciada fuente de lácteos artesanales",
    "PRIZED SOURCE OF DAIRY": "Preciada fuente de lácteos artesanales",
    "KNOWN FORA PLANT THAT ONLY BLOOMS FOR ONEDAY": "Conocido por una flor que brota solo un día al año",
    "KNOWN FOR A PLANT THAT ONLY BLOOMS FOR ONE DAY": "Conocido por una flor que brota solo un día al año",
    "SPIRITUAL RETREAT": "Refugio y retiro para la contemplación espiritual",
    "NUMEROUS GANGS": "Territorio controlado por numerosas bandas rivales",
    "RENOWNED INSTRUMENTMAKER(S)": "Renombrados lutieres y constructores de instrumentos",
    "RENOWNED INSTRUMENT MAKER(S)": "Renombrados lutieres y constructores de instrumentos",
    "LIMITED ACCESS TO CLEAN WATER": "Acceso limitado a fuentes de agua potable",
    "FREQUENT HAILSTORMS": "Frecuentes granizadas de hielo destructor",
    "EXPANSIONIST AGENDA": "Ambiciones bélicas y planes expansionistas",
    "REGIONAL HOLIDAY CELEBRATION": "Colorida fiesta patronal y feria regional",
    "SCENIC VISTA(S)": "Miradores con vistas panorámicas sobrecogedoras",
    "UNUSUAL WILDLIFE ACTIUITY": "Inquietante e inusual actividad de la fauna",
    "UNUSUAL WILDLIFE ACTIVITY": "Inquietante e inusual actividad de la fauna",
    "MULTILINGUAL": "Población políglota y cosmopolita",
    "KNOWN FOR TALKING BIRD(S)": "Famoso por aves capaces de imitar el habla humana",
    "LOCATED ON A WATERWAY": "Situado estratégicamente sobre una arteria fluvial",
    "POPULARCAMPSITE": "Popular punto de parada y acampada de viajeros",
    "POPULAR CAMPSITE": "Popular punto de parada y acampada de viajeros",
    "GAMBLING HUB": "Epicentro de casinos, juegos de azar y apuestas",
    "EVERYONE HATES THE WEATHER": "Clima hostil detestado por todos los habitantes",
    "HABITATOF A GIANT ANIMAL": "Hábitat de una bestia titánica colosal",
    "HABITAT OF A GIANT ANIMAL": "Hábitat de una bestia titánica colosal",
    "VISITED FOR GOOD LUCK": "Destino visitado para atraer la buena fortuna",
    "ORAL HISTORY TRADITION": "Tradición oral viva transmitida por bardos",
    "KNOWN FOR ONE SPECIFIC SUPERSTITION": "Regido por una superstición popular inquebrantable",
    "OLDEST OF ITS KIND": "El más antiguo vestigio de su clase en el mundo",
    "WILDLIFE MATING GROUNDS": "Zona sagrada de apareamiento de la fauna",
    "RELIGIOUSLY DIVERSE": "Gran diversidad de cultos, credos y panteones",
    "KNOWN FOR STREET FOOD": "Famoso por sus puestos de comida callejera",
    "SEASONAL FLOODING": "Crecidas e inundaciones estacionales cíclicas",
    "CARNIUOROUS PLANTS": "Bosquecillos de plantas carnívoras voraces",
    "CARNIVOROUS PLANTS": "Bosquecillos de plantas carnívoras voraces",
}

OF_MAP = {
    "TOWERS": "De las torres", "SHADE": "De la sombra", "MAPS": "De los mapas",
    "MOUTHS": "De las bocas", "SICKLES": "De las hoces", "YOUTH": "De la juventud",
    "PIGS": "De los cerdos", "POETS": "De los poetas", "MEMORY": "De la memoria",
    "BURDENS": "De las cargas", "ORCHARDS": "De los huertos", "LIES": "De las mentiras",
    "INK": "De la tinta", "JADE": "Del jade", "BONES": "De los huesos",
    "ASH": "De la ceniza", "ASHES": "De las cenizas", "BLOOD": "De la sangre",
    "GOLD": "Del oro", "SILVER": "De la plata", "IRON": "Del hierro",
    "STONE": "De la piedra", "GLASS": "Del cristal", "SALT": "De la sal",
    "DUST": "Del polvo", "WIND": "Del viento", "STORMS": "De las tormentas",
    "THUNDER": "Del trueno", "LIGHTNING": "Del relámpago", "FIRE": "Del fuego",
    "FLAME": "De la llama", "SMOKE": "Del humo", "ICE": "Del hielo",
    "FROST": "De la escarcha", "SNOW": "De la nieve", "MIST": "De la niebla",
    "SHADOWS": "De las sombras", "NIGHT": "De la noche", "STARS": "De las estrellas",
    "MOONS": "De las lunas", "SUN": "Del sol", "DAWN": "Del amanecer",
    "DUSK": "Del crepúsculo", "ECHOES": "De los ecos", "WHISPERS": "De los susurros",
    "SILENCE": "Del silencio", "SORROW": "Del pesar", "TEARS": "De las lágrimas",
    "HOPE": "De la esperanza", "DREAMS": "De los sueños", "KINGS": "De los reyes",
    "QUEENS": "De las reinas", "GODS": "De los dioses", "DEMONS": "De los demonios",
    "GIANTS": "De los gigantes", "HEROES": "De los héroes", "SAINTS": "De los santos",
    "THIEVES": "De los ladrones", "CROWS": "De los cuervos", "WOLVES": "De los lobos",
    "SERPENTS": "De las serpientes", "SNAKES": "De las víboras", "BEASTS": "De las bestias",
    "THORNS": "De las espinas", "ROSES": "De las rosas", "OAKS": "De los robles",
    "PINES": "De los pinos", "WILLOWS": "De los sauces", "BELLS": "De las campanas",
    "CHAINS": "De las cadenas", "CROWNS": "De las coronas", "SWORDS": "De las espadas",
    "SHIELDS": "De los escudos", "SHIPS": "De los barcos", "SAILS": "De las velas",
    "COINS": "De las monedas", "MASKS": "De las máscaras", "MIRRORS": "De los espejos",
    "SECRETS": "De los secretos", "VOICES": "De las voces", "GRIEF": "Del duelo",
    "WRATH": "De la ira", "FEAR": "Del temor", "PEACE": "De la paz",
    "WONDERS": "De las maravillas", "RUIN": "De la ruina", "RUST": "Del óxido",
    "SILK": "De la seda", "CLOTH": "De la tela", "ROPE": "De la cuerda",
    "GRAVES": "De las tumbas", "SKULLS": "De las calaveras", "FEATHERS": "De las plumas",
    "WINGS": "De las alas", "HORNS": "De los cuernos", "FANGS": "De los colmillos",
    "SCALES": "De las escamas", "ROOTS": "De las raíces", "LEAVES": "De las hojas",
    "SEEDS": "De las semillas",
}

def translate_cue(text: str) -> str:
    cleaned = text.strip()
    upper = cleaned.upper()
    if upper in WORD_MAP:
        return WORD_MAP[upper]
    
    # Check OF ...
    cleaned_of = re.sub(r"^OF\s*", "", upper)
    if upper.startswith("OF ") or upper.startswith("OF"):
        noun = cleaned_of.strip()
        if noun in OF_MAP:
            return OF_MAP[noun]
        return f"De {noun.capitalize()}"

    return cleaned.capitalize()

def main():
    raw_data = json.loads(RAW_CUES_PATH.read_text(encoding="utf-8"))
    pack = json.loads(PACK_PATH.read_text(encoding="utf-8"))
    metadata = json.loads(METADATA_PATH.read_text(encoding="utf-8"))
    faces_by_card = {face["cardId"]: face for face in pack["cardFaces"]}

    GROUPS = {
        101: ("region", "Región"),
        102: ("landmark", "Punto de Interés"),
        103: ("namesake", "Apelativo"),
        104: ("origin", "Origen"),
        105: ("attribute", "Atributo"),
        106: ("advent", "Acontecimiento"),
    }

    # Generate clean cue mapping
    cues_by_filename = {}

    # Deck 101: Region (32)
    for item in raw_data["region"]:
        fn = item["filename"]
        raw_cues = item["cues"]
        tr = [translate_cue(c) for c in raw_cues]
        cues_by_filename[fn] = tr

    # Deck 102: Landmark (48)
    for item in raw_data["landmark"]:
        fn = item["filename"]
        idx = item["index"]
        raw_cues = item["cues"]
        if fn == "landmark_020.webp":
            tr = ["Cueva", "Gran biblioteca"]
        elif fn == "landmark_038.webp":
            tr = ["Acantilado", "Santuario"]
        elif fn == "landmark_043.webp":
            tr = ["Costa", "Palacio"]
        else:
            tr = [translate_cue(c) for c in raw_cues]
        cues_by_filename[fn] = tr

    # Deck 103: Namesake (48)
    for item in raw_data["namesake"]:
        fn = item["filename"]
        if fn == "namesake_008.webp":
            tr = ["De los cerdos", "De los poetas", "Impertérrito / Valeroso", "Donde ningún pájaro canta"]
        elif fn == "namesake_029.webp":
            tr = ["Del jade", "De la memoria", "Último", "Que fue demasiado lejos"]
        elif fn == "namesake_032.webp":
            tr = ["De los huertos", "De las cargas", "Alto / Elevado", "Que sueña"]
        elif fn == "namesake_035.webp":
            tr = ["De la tinta", "De las mentiras", "Tocado por los dioses", "Más allá de la esperanza"]
        else:
            tr = [translate_cue(c) for c in item["cues"]]
        cues_by_filename[fn] = tr

    # Deck 104: Origin (32)
    for item in raw_data["origin"]:
        fn = item["filename"]
        idx = item["index"]
        if idx in ORIGIN_TRANSLATIONS:
            cues_by_filename[fn] = list(ORIGIN_TRANSLATIONS[idx])
        else:
            cues_by_filename[fn] = [translate_cue(c) for c in item["cues"]]

    # Deck 105: Attribute (48)
    for item in raw_data["attribute"]:
        fn = item["filename"]
        tr = [translate_cue(c) for c in item["cues"]]
        cues_by_filename[fn] = tr

    # Deck 106: Advent (32)
    for item in raw_data["advent"]:
        fn = item["filename"]
        idx = item["index"]
        if idx in ADVENT_TRANSLATIONS:
            cues_by_filename[fn] = list(ADVENT_TRANSLATIONS[idx])
        else:
            cues_by_filename[fn] = [translate_cue(c) for c in item["cues"]]

    # Update metadata
    for ctype, cards in metadata["cards"].items():
        for card in cards:
            fn = card["filename"]
            if fn in cues_by_filename:
                card["translated_cues"] = cues_by_filename[fn]
                card["text"] = " / ".join(cues_by_filename[fn])

    # Update pack cardFaces and card titles
    for card in pack["cards"]:
        face = faces_by_card[card["id"]]
        fn = Path(face["imagePath"]).name
        cues = cues_by_filename.get(fn, [])
        deck_id = card["stackId"]
        prefix_name = GROUPS[deck_id][1]
        order_num = card["sortOrder"] + 1

        # Populate zonesJson with Spanish text
        zones_data = [{"text": cue} for cue in cues]
        face["zonesJson"] = json.dumps(zones_data, ensure_ascii=False)

        # Title formatting: Categoría #XX: [Resumen de opciones]
        if cues:
            if len(cues) == 1:
                title_detail = cues[0]
            elif len(cues) == 2:
                # Truncate long sentences in title for clean display
                p1 = cues[0].split(":")[0][:35]
                p2 = cues[1].split(":")[0][:35]
                title_detail = f"{p1} / {p2}"
            else:
                title_detail = f"{cues[0]} / {cues[1]}"
            card["title"] = f"{prefix_name} #{order_num:02d}: {title_detail}"
        else:
            card["title"] = f"{prefix_name} #{order_num:02d}"

        face["name"] = card["title"]

    PACK_PATH.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    METADATA_PATH.write_text(json.dumps(metadata, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("Full Spanish Story Engine starter pack generated successfully!")

if __name__ == "__main__":
    main()
