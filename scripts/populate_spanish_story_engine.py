"""Translate Deck of Worlds cues to Spanish and update JSON packs.
"""
import json
import re
from pathlib import Path

ROOT = Path(r"i:\TTRPG\Visuales\Decks\Deckapp")
PACK_PATH = ROOT / "core/data/src/main/assets/starter_packs/story_engine_deck_of_worlds.json"
METADATA_PATH = ROOT / "core/data/src/main/assets/starter_packs/deck_of_worlds/cards_metadata.json"
RAW_CUES_PATH = Path(r"C:\Users\Giise\.gemini\antigravity-ide\brain\3591deda-8ce7-4918-a81a-6fdfe790dcc2\scratch\dow_raw_cues.json")

# Known fixes for missing or edge-case OCR readings
MANUAL_FIXES = {
    "landmark_020.webp": ["CAVE", "LIBRARY"],
    "landmark_038.webp": ["CLIFF", "SANCTUARY"],
    "landmark_043.webp": ["COAST", "PALACE"],
    "namesake_008.webp": ["OF PIGS", "OF POETS", "UNDAUNTED", "WHERE NO BIRD SINGS"],
    "namesake_029.webp": ["OF JADE", "OF MEMORY", "LAST", "THAT WENT TOO FAR"],
    "namesake_032.webp": ["OF ORCHARDS", "OF BURDENS", "HIGH", "THAT DREAMS"],
    "namesake_035.webp": ["OF INK", "OF LIES", "GOD-TOUCHED", "BEYOND HOPE"],
    "origin_018.webp": [
        "ORIGINALLY A SCHOOL",
        "WAS ONCE AN ANCIENT LAKE",
        "FOUNDED BY SOLDIER(S)",
        "SUPPOSED RESTING PLACE OF A FOLK HERO"
    ]
}

# Clean OCR OCR artifacts in text (like U instead of V, missing spaces)
def clean_english_text(txt: str) -> str:
    txt = txt.strip()
    replacements = [
        (r"\bUANISHED\b", "VANISHED"),
        (r"\bDISCOUERY\b", "DISCOVERY"),
        (r"\bAREUOLUTIONARY\b", "A REVOLUTIONARY"),
        (r"\bREUOLUTIONARY\b", "REVOLUTIONARY"),
        (r"\bREUEALING\b", "REVEALING"),
        (r"\bDIUISION\b", "DIVISION"),
        (r"\bHAUEN\b", "HAVEN"),
        (r"\bBEHAUING\b", "BEHAVING"),
        (r"\bGRAUE\b", "GRAVE"),
        (r"\bGOUERNMENT\b", "GOVERNMENT"),
        (r"\bHYPERACTIOITY\b", "HYPERACTIVITY"),
        (r"\bHYPERACTIUITY\b", "HYPERACTIVITY"),
        (r"\bOFTOWERS\b", "OF TOWERS"),
        (r"\bOFSHADE\b", "OF SHADE"),
        (r"\bOFMAPS\b", "OF MAPS"),
        (r"\bOFMOUTHS\b", "OF MOUTHS"),
        (r"\bOFSICKLES\b", "OF SICKLES"),
        (r"\bOFYOUTH\b", "OF YOUTH"),
        (r"\bOFPIGS\b", "OF PIGS"),
        (r"\bOFPOETS\b", "OF POETS"),
        (r"\bOFMEMORY\b", "OF MEMORY"),
        (r"\bOFBURDENS\b", "OF BURDENS"),
        (r"\bOFORCHARDS\b", "OF ORCHARDS"),
        (r"\bOFLIES\b", "OF LIES"),
        (r"\bOFINK\b", "OF INK"),
        (r"\bHOMEOFA\b", "HOME OF A"),
        (r"\bHOMEOFGOD\(S\)\b", "HOME OF GOD(S)"),
        (r"\bHOMEOF\b", "HOME OF"),
        (r"\bLASTKNOWN\b", "LAST KNOWN"),
        (r"\bANCIENTARTIFACT\b", "ANCIENT ARTIFACT"),
        (r"\bTOKEEP\b", "TO KEEP"),
        (r"\bAPROMISE\b", "A PROMISE"),
        (r"\bSITEOFA\b", "SITE OF A"),
        (r"\bSITEOFAN\b", "SITE OF AN"),
        (r"\bEXTINCTSPECIES\b", "EXTINCT SPECIES"),
        (r"\bETIQUETTESYSTEM\b", "ETIQUETTE SYSTEM"),
        (r"\bTHEYAREUNDERSIEGE\b", "THEY ARE UNDER SIEGE"),
        (r"\bTHEY ARE UNDER SIEGE BYAFOREIGNPOWER\b", "THEY ARE UNDER SIEGE BY A FOREIGN POWER"),
        (r"\bBYAFOREIGNPOWER\b", "BY A FOREIGN POWER"),
        (r"\bANARMY\b", "AN ARMY"),
        (r"\bORPROPAGANDA WAR\b", "OR PROPAGANDA WAR"),
        (r"\bORPROPAGANDAWAR\b", "OR PROPAGANDA WAR"),
        (r"\bWILDLIFEIS\b", "WILDLIFE IS"),
        (r"\bAGGRESSION,DISORIENTATION\b", "AGGRESSION, DISORIENTATION"),
        (r"\bISCAUSINGANUPROAR\b", "IS CAUSING AN UPROAR"),
        (r"\bHIGHLIGHTINGINJUSTICE\b", "HIGHLIGHTING INJUSTICE"),
        (r"\bSTOKINGDIUISION\b", "STOKING DIVISION"),
        (r"\bTHEREISARUSHTODISCOUER\b", "THERE IS A RUSH TO DISCOVER"),
        (r"\bRECENTCONSTRUCTION\b", "RECENT CONSTRUCTION"),
        (r"\bHASUNEARTHED\b", "HAS UNEARTHED"),
        (r"\bREFUGEESARESETTINGUP\b", "REFUGEES ARE SETTING UP"),
        (r"\bCAMPDESPITECHALLENGES\b", "CAMP DESPITE CHALLENGES"),
        (r"\bRESOURCESHORTAGES\b", "RESOURCE SHORTAGES"),
    ]
    for pattern, repl in replacements:
        txt = re.sub(pattern, repl, txt, flags=re.IGNORECASE)
    return txt.strip()

# Common Spanish translations for single-word / short cues
WORD_TRANSLATIONS = {
    # Regions
    "WOOD(S)": "Bosque(s)",
    "DESERT": "Desierto",
    "DELTA": "Delta",
    "OCEAN": "Océano",
    "SEA": "Mar",
    "MOUNTAINS": "Montañas",
    "MOUNTAIN(S)": "Montaña(s)",
    "TUNDRA": "Tundra",
    "SWAMP": "Pantano",
    "MARSH": "Ciénaga",
    "PLAINS": "Llanura",
    "GRASSLAND": "Pradera",
    "JUNGLE": "Selva",
    "RAINFOREST": "Bosque lluvioso",
    "CANYON": "Cañón",
    "ISLAND(S)": "Isla(s)",
    "VOLCANO": "Volcán",
    "CAVERNS": "Cavernas",
    "UNDERGROUND": "Subterráneo",
    "VALLEY": "Valle",
    "COAST": "Costa",
    "CLIFFS": "Acantilados",
    "GLACIER": "Glaciar",
    "STEPPE": "Estepa",
    "SAVANNA": "Sabana",
    "RIVER": "Río",
    "HIGHLANDS": "Tierras altas",
    "PLATEAU": "Meseta",
    "WASTELAND": "Yermo",
    "HILLS": "Colinas",
    "LAKE": "Lago",
    "REEF": "Arrecife",
    "ATOLL": "Atolón",
    "MOOR": "Páramo",
    "CRATER": "Cráter",
    "WETLANDS": "Humedales",
    "FJORD(S)": "Fiordo(s)",
    "ARCHIPELAGO": "Archipiélago",
    "BADLANDS": "Tierras baldías",
    "OASIS": "Oasis",
    "TAIGA": "Taiga",
    "ESTUARY": "Estuario",
    "GORGE": "Garganta",

    # Landmarks
    "WORKSHOP": "Taller",
    "TREE": "Árbol ancestral",
    "WATERFALL": "Cascada",
    "ZOO": "Casa de fieras / Zoológico",
    "STREAM": "Arroyo",
    "WALL": "Muralla",
    "BRIDGE": "Puente",
    "TOWER": "Torre",
    "SHRINE": "Santuario",
    "MINE": "Mina",
    "INN": "Posada",
    "TAVERN": "Taberna",
    "CASTLE": "Castillo",
    "CITADEL": "Ciudadela",
    "CEMETERY": "Cementerio",
    "TOMB": "Tumba",
    "MARKET": "Mercado",
    "BAZAAR": "Bazar",
    "MONUMENT": "Monumento",
    "MILL": "Molino",
    "TEMPLE": "Templo",
    "LIGHTHOUSE": "Faro",
    "BEACON": "Almenara",
    "GATE": "Puerta monumental",
    "PASS": "Paso de montaña",
    "OUTPOST": "Puesto de avanzada",
    "FORT": "Fuerte",
    "HARBOR": "Puerto",
    "DOCKS": "Muelles",
    "LABYRINTH": "Laberinto",
    "MAZE": "Dédalo",
    "ARCHIVE": "Archivo",
    "LIBRARY": "Gran biblioteca",
    "CAVE": "Cueva oculta",
    "CLIFF": "Risco / Acantilado",
    "PALACE": "Palacio",
    "SANCTUARY": "Santuario",
    "STATUE": "Estatua colosal",
    "OBELISK": "Obelisco",
    "RUINS": "Ruinas",
    "CAMP": "Campamento",
    "VILLAGE": "Aldea",
    "ARENA": "Arena / Coliseo",
    "PRISON": "Prisión",
    "DUNGEON": "Mazmorra",
    "GARDEN": "Jardín botánico",
    "ORCHARD": "Huerto",
    "WELL": "Pozo sagrado",
    "FOUNTAIN": "Fuente",
    "MONASTERY": "Monasterio",
    "ABBEY": "Abadía",
    "ACADEMY": "Academia",
    "FOUNDRY": "Fundición",
    "FORGE": "Forja",
    "GRAVEYARD": "Camposanto",
    "VAULT": "Cámara acorazada",
    "SEWERS": "Cloacas",
    "CATACOMBS": "Catacumbas",
    "BARRACKS": "Cuartel",
    "OBSERVATORY": "Observatorio",
    "COLOSSUS": "Coloso",
    "AMPHITHEATER": "Anfiteatro",
    "MENHIR": "Menhir",
    "MEGALITH": "Megalito",
    "PYRAMID": "Pirámide",
    "SHIPWRECK": "Naufragio",
    "SPILLED CARGO": "Cargamento disperso",
    "QUARRY": "Cantera",
    "WATCHTOWER": "Atalaya",
    "CARAVAN": "Caravana detenida",
    "SETTLEMENT": "Asentamiento",
    "MANOR": "Mansión señorial",
    "ESTATE": "Hacienda",
    "GROVE": "Arboleda sagrada",
    "HOT SPRING(S)": "Aguas termales",
    "GEYSER": "Géiser",
    "CRATER": "Cráter de impacto",
    "FARM": "Granja",
    "RANCH": "Rancho",
    "BAKERY": "Panadería",
    "SMITHY": "Herrería",
    "APOTHECARY": "Boticario / Herbolario",
    "CLOISTER": "Claustro",
    "ROADSIDE SHRINE": "Altar del camino",
    "TOWNSHIP": "Pueblo",
    "MEETING HALL": "Salón comunal",
}

def translate_phrase_to_spanish(text: str) -> str:
    cleaned = clean_english_text(text)
    upper = cleaned.upper()
    
    if upper in WORD_TRANSLATIONS:
        return WORD_TRANSLATIONS[upper]
    
    # Handle OF <NOUN>
    if upper.startswith("OF "):
        rest = upper[3:].strip()
        tr_map = {
            "TOWERS": "De las torres",
            "SHADE": "De la sombra",
            "MAPS": "De los mapas",
            "MOUTHS": "De las bocas",
            "SICKLES": "De las hoces",
            "YOUTH": "De la juventud",
            "PIGS": "De los cerdos",
            "POETS": "De los poetas",
            "MEMORY": "De la memoria",
            "BURDENS": "De las cargas",
            "ORCHARDS": "De los huertos",
            "LIES": "De las mentiras",
            "INK": "De la tinta",
            "JADE": "Del jade",
            "BONES": "De los huesos",
            "ASH": "De la ceniza",
            "ASHES": "De las cenizas",
            "BLOOD": "De la sangre",
            "GOLD": "Del oro",
            "SILVER": "De la plata",
            "IRON": "Del hierro",
            "STONE": "De la piedra",
            "GLASS": "Del cristal",
            "SALT": "De la sal",
            "DUST": "Del polvo",
            "WIND": "Del viento",
            "STORMS": "De las tormentas",
            "THUNDER": "Del trueno",
            "LIGHTNING": "Del relámpago",
            "FIRE": "Del fuego",
            "FLAME": "De la llama",
            "SMOKE": "Del humo",
            "ICE": "Del hielo",
            "FROST": "De la escarcha",
            "SNOW": "De la nieve",
            "MIST": "De la niebla",
            "SHADOWS": "De las sombras",
            "NIGHT": "De la noche",
            "STARS": "De las estrellas",
            "MOONS": "De las lunas",
            "SUN": "Del sol",
            "DAWN": "Del amanecer",
            "DUSK": "Del crepúsculo",
            "ECHOES": "De los ecos",
            "WHISPERS": "De los susurros",
            "SILENCE": "Del silencio",
            "SORROW": "Del pesar",
            "TEARS": "De las lágrimas",
            "HOPE": "De la esperanza",
            "DREAMS": "De los sueños",
            "KINGS": "De los reyes",
            "QUEENS": "De las reinas",
            "GODS": "De los dioses",
            "DEMONS": "De los demonios",
            "GIANTS": "De los gigantes",
            "HEROES": "De los héroes",
            "SAINTS": "De los santos",
            "THIEVES": "De los ladrones",
            "CROWS": "De los cuervos",
            "WOLVES": "De los lobos",
            "SERPENTS": "De las serpientes",
            "SNAKES": "De las víboras",
            "BEASTS": "De las bestias",
            "THORNS": "De las espinas",
            "ROSES": "De las rosas",
            "OAKS": "De los robles",
            "PINES": "De los pinos",
            "WILLOWS": "De los sauces",
            "BELLS": "De las campanas",
            "CHAINS": "De las cadenas",
            "CROWNS": "De las coronas",
            "SWORDS": "De las espadas",
            "SHIELDS": "De los escudos",
            "SHIPS": "De los barcos",
            "SAILS": "De las velas",
            "COINS": "De las monedas",
            "MASKS": "De las máscaras",
            "MIRRORS": "De los espejos",
            "SECRETS": "De los secretos",
            "VOICES": "De las voces",
            "GRIEF": "Del duelo",
            "WRATH": "De la ira",
            "FEAR": "Del temor",
            "PEACE": "De la paz",
            "WONDERS": "De las maravillas",
            "RUIN": "De la ruina",
            "RUST": "Del óxido",
            "SILK": "De la seda",
            "CLOTH": "De la tela",
            "ROPE": "De la cuerda",
            "GRAVES": "De las tumbas",
            "SKULLS": "De las calaveras",
            "FEATHERS": "De las plumas",
            "WINGS": "De las alas",
            "HORNS": "De los cuernos",
            "FANGS": "De los colmillos",
            "SCALES": "De las escamas",
            "ROOTS": "De las raíces",
            "LEAVES": "De las hojas",
            "SEEDS": "De las semillas",
        }
        if rest in tr_map:
            return tr_map[rest]
        return f"De {rest.capitalize()}"

    # Common epithets / adjectives
    adj_map = {
        "WINDY": "Ventoso",
        "WHERE NOTHING GROWS": "Donde nada crece",
        "CROOKED": "Torcido",
        "UNDER HEAVEN": "Bajo el cielo",
        "WINKING": "Guiñante",
        "THAT WATCHES": "Que vigila",
        "UNDAUNTED": "Impertérrito / Valeroso",
        "WHERE NO BIRD SINGS": "Donde ningún pájaro canta",
        "LAST": "Último",
        "THAT WENT TOO FAR": "Que fue demasiado lejos",
        "HIGH": "Alto / Elevado",
        "THAT DREAMS": "Que sueña",
        "GOD-TOUCHED": "Tocado por los dioses",
        "BEYOND HOPE": "Más allá de la esperanza",
        "WHISPERING": "Susurrante",
        "BURNING": "Ardiente",
        "FROZEN": "Congelado",
        "FORGOTTEN": "Olvidado",
        "LOST": "Perdido",
        "HOLLOW": "Hueco",
        "SILENT": "Silencioso",
        "CRACKED": "Agrietado",
        "SUNKEN": "Hundido",
        "SLEEPING": "Durmiente",
        "BOUND": "Atado / Encadenado",
        "BLEEDING": "Sangrante",
        "SHROUDED": "Envuelto en sombras",
        "ANCIENT": "Antiguo",
        "FALLEN": "Caído",
        "RISING": "Ascendente",
        "SHINING": "Resplandeciente",
        "DARKENED": "Oscurecido",
        "BLOODY": "Sanguinolento",
        "GOLDEN": "Dorado",
        "SILVER": "Plateado",
        "IRON": "Férreo",
        "STONE": "Pétreo",
        "BLESSED": "Bendito",
        "CURSED": "Maldito",
        "SACRED": "Sagrado",
        "UNHOLY": "Profano",
        "WEEPING": "Lloroso",
        "HOWLING": "Aullante",
        "ROARING": "Rugiente",
        "SINGING": "Cantarín",
        "SMILING": "Sonriente",
        "LAUGHING": "Riente",
        "HUNGRY": "Hambriento",
        "ENDLESS": "Infinito",
        "TIMELESS": "Eterno",
        "RESTLESS": "Inquieto",
        "BROKEN": "Quebrantado",
        "WITHERED": "Marchito",
        "TWISTED": "Retorcido",
        "BARREN": "Esteril",
        "DROWNED": "Ahogado",
        "BURIED": "Enterrado",
        "HAUNTED": "Embrujado",
        "POLLUTED": "Contaminado",
        "UNUSUAL ELECTION PROCESS": "Proceso electoral insólito",
        "HUNTING GROUND": "Coto de caza",
        "KNOWN FOR FOSSIL(S)": "Famoso por sus fósil(es)",
        "THEOCRATIC GOVERNMENT": "Gobierno teocrático",
        "HAVEN FOR ARTISTS": "Refugio para artistas",
        "CONSTANTLY CLOUDY": "Perennemente nublado",
        "PILGRIMAGE DESTINATION": "Destino de peregrinación",
        "ELABORATE ETIQUETTE SYSTEM": "Elaborado protocolo social",
        "SKILLED FORAGERS": "Recolectores experimentados",
        "SITE OF AN AUDITORY PHENOMENON": "Sitio de un fenómeno acústico misterioso",
        "HABITAT OF A RARE INSECT": "Hábitat de una especie rara de insecto",
    }
    for k, v in adj_map.items():
        if upper == k:
            return v

    # General phrase translation heuristic for Origins / Attributes / Advents
    # Translate typical clause starters
    patterns = [
        (r"^HOME OF A VANISHED PEOPLE", "Hogar de un pueblo desaparecido"),
        (r"^FOUNDED BY OUTCAST\(S\)", "Fundado por parias y marginados"),
        (r"^LAST KNOWN LOCATION OF AN ANCIENT ARTIFACT", "Último paradero conocido de una reliquia milenaria"),
        (r"^SAID TO HAVE BEEN THE HOME OF GOD\(S\)", "Se dice que fue morada de deidades"),
        (r"^SITE OF A FAMOUS SCIENTIFIC DISCOVERY", "Lugar de un célebre descubrimiento científico"),
        (r"^CREATED TO KEEP A PROMISE", "Erigido para cumplir un juramento solemne"),
        (r"^SITE OF A NOTORIOUS BET", "Escenario de una legendaria apuesta"),
        (r"^SHAPED BY CENTURIES OF WAR", "Modelado por siglos de conflicto bélico"),
        (r"^SITE OF AN INFAMOUS HEIST", "Lugar de un infame robo histórico"),
        (r"^FORMER TRADING HUB", "Antiguo centro de comercio e intercambio"),
        (r"^FORMER HABITAT OF AN EXTINCT SPECIES", "Antiguo hábitat de una especie extinta"),
        (r"^SHAPED BY LANDSLIDE\(S\)", "Transformado por aludes y deslizamientos"),
        (r"^ORIGINALLY A SCHOOL", "Originalmente un centro de enseñanza"),
        (r"^WAS ONCE AN ANCIENT LAKE", "Alguna vez fue un lago ancestral"),
        (r"^FOUNDED BY SOLDIER\(S\)", "Fundado por veterano(s) de guerra"),
        (r"^SUPPOSED RESTING PLACE OF A FOLK HERO", "Supuesto sepulcro de un héroe popular"),
        (r"^THEY ARE UNDER SIEGE BY A FOREIGN POWER:?", "Bajo asedio de una potencia extranjera:"),
        (r"^WILDLIFE IS BEHAVING PECULIARLY:?", "La fauna muestra conductas anómalas:"),
        (r"^A REVOLUTIONARY MANIFESTO IS CAUSING AN UPROAR:?", "Un manifiesto revolucionario desata la agitación:"),
        (r"^THERE IS A RUSH TO DISCOVER A MYTHICAL BURIAL SITE:?", "Fiebre por hallar un mítico sepulcro antiguo:"),
        (r"^RECENT CONSTRUCTION HAS UNEARTHED A SECRET:?", "Unas obras recientes desentierran un secreto:"),
        (r"^REFUGEES ARE SETTING UP CAMP DESPITE CHALLENGES:?", "Refugiados acampan frente a graves adversidades:"),
    ]
    for p, repl in patterns:
        if re.search(p, upper):
            # Replace prefix and translate remaining list if present
            suffix = re.sub(p, "", upper).strip()
            if not suffix:
                return repl
            # Translate common suffix options
            suffix_tr = suffix
            suffix_tr = suffix_tr.replace("AN ARMY, BOMBARDMENT OR PROPAGANDA WAR", "un ejército, bombardeo o propaganda hostil")
            suffix_tr = suffix_tr.replace("AN ARMY,BOMBARDMENT OR PROPAGANDA WAR", "un ejército, bombardeo o propaganda hostil")
            suffix_tr = suffix_tr.replace("AGGRESSION, DISORIENTATION OR HYPERACTIVITY", "agresividad súbita, desorientación o frenesí")
            suffix_tr = suffix_tr.replace("AGGRESSION,DISORIENTATION OR HYPERACTIVITY", "agresividad súbita, desorientación o frenesí")
            suffix_tr = suffix_tr.replace("HIGHLIGHTING INJUSTICE, STOKING DIVISION OR REVEALING POLITICAL SECRETS", "revelando injusticias, avivando divisiones o exponiendo secretos políticos")
            suffix_tr = suffix_tr.replace("FOR RELIGIOUS REASONS, GRAVE ROBBING OR BRAGGING RIGHTS", "por fervor religioso, saqueo de tumbas o gloria personal")
            suffix_tr = suffix_tr.replace("A BODY, VAULT OR TUNNEL", "un cadáver intacto, una cripta o un túnel prohibido")
            suffix_tr = suffix_tr.replace("RESOURCE SHORTAGES, DANGEROUS TERRAIN, OR HOSTILE LOCALS", "escasez de víveres, terreno hostil o rechazo de los nativos")
            return f"{repl} {suffix_tr.lower()}"

    # If nothing matched, clean and title-case gracefully
    # Replace common words in sentences
    words = cleaned.split()
    translated_words = []
    word_dict = {
        "WHERE": "donde", "THAT": "que", "UNDER": "bajo", "BEYOND": "más allá de",
        "OF": "de", "THE": "el/la", "AND": "y", "OR": "o", "BY": "por", "FOR": "para",
        "SITE": "Sitio", "FORMER": "Antiguo", "SHAPED": "Modelado", "FOUNDED": "Fundado",
        "HOME": "Hogar", "LAST": "Último", "KNOWN": "conocido", "LOCATION": "paradero",
        "SAID": "Dícese", "HAVE": "haber", "BEEN": "sido", "CENTURIES": "siglos",
        "WAR": "guerra", "PEOPLE": "pueblo", "VANISHED": "desaparecido",
        "FAMOUS": "famoso", "SECRET": "secreto", "ANCIENT": "antiguo", "SACRED": "sagrado",
        "GOD": "Dios", "GODS": "Dioses", "OUTCAST": "paria", "OUTCASTS": "parias",
        "DISCOVERY": "descubrimiento", "PROMISE": "promesa", "BET": "apuesta",
        "HEIST": "atraco", "TRADING": "comercial", "HUB": "núcleo",
        "HABITAT": "hábitat", "EXTINCT": "extinto", "SPECIES": "especie",
        "LANDSLIDE": "deslizamiento", "LANDSLIDES": "aludes",
    }
    for w in words:
        w_clean = re.sub(r"[^A-Za-z\(\)]", "", w).upper()
        if w_clean in word_dict:
            translated_words.append(word_dict[w_clean])
        else:
            translated_words.append(w.capitalize())
    return " ".join(translated_words)

def main():
    raw_data = json.loads(RAW_CUES_PATH.read_text(encoding="utf-8"))
    pack = json.loads(PACK_PATH.read_text(encoding="utf-8"))
    metadata = json.loads(METADATA_PATH.read_text(encoding="utf-8"))
    faces_by_card = {face["cardId"]: face for face in pack["cardFaces"]}
    cards_by_id = {card["id"]: card for card in pack["cards"]}

    GROUPS = {
        101: ("region", "Región"),
        102: ("landmark", "Punto de Interés"),
        103: ("namesake", "Apelativo"),
        104: ("origin", "Origen"),
        105: ("attribute", "Atributo"),
        106: ("advent", "Acontecimiento"),
    }

    # Pre-populate raw fixes
    for ctype, card_list in raw_data.items():
        for item in card_list:
            fn = item["filename"]
            if fn in MANUAL_FIXES:
                item["cues"] = MANUAL_FIXES[fn]

    # Map filename -> translated cues
    cues_by_filename = {}
    for ctype, card_list in raw_data.items():
        for item in card_list:
            fn = item["filename"]
            tr_cues = [translate_phrase_to_spanish(c) for c in item["cues"]]
            cues_by_filename[fn] = tr_cues

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

        # Build card title: "Categoría #XX: [Pistas]"
        if cues:
            if len(cues) == 1:
                title_detail = cues[0]
            elif len(cues) == 2:
                title_detail = f"{cues[0]} / {cues[1]}"
            else:
                title_detail = " / ".join(cues[:2])
            card["title"] = f"{prefix_name} #{order_num:02d}: {title_detail}"
        else:
            card["title"] = f"{prefix_name} #{order_num:02d}"

        face["name"] = card["title"]

    PACK_PATH.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    METADATA_PATH.write_text(json.dumps(metadata, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("Successfully populated Spanish cues, zones, and titles for all 240 cards!")

if __name__ == "__main__":
    main()
