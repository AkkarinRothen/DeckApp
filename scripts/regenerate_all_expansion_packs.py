"""
Master Regeneration Script for Story Engine & Deck of Worlds Expansions.

Cuts every card cleanly into its individual square card (2x3 grid per sheet, 6 cards/sheet).
Extracts front and duplex back for each card.
Runs OCR on each card's distinct cues (IMAGE_WITH_TEXT, TOP_BOTTOM_SPLIT, FOUR_EDGE_CUES).
Generates Spanish translated starter packs:
  1. core/data/src/main/assets/starter_packs/story_engine_deck_of_worlds_expansions.json (9 decks, 540 cards)
  2. core/data/src/main/assets/starter_packs/new_story_engine.json (4 decks, 300 cards)
"""

import json
import re
import shutil
import sys
from pathlib import Path
import fitz  # PyMuPDF
from PIL import Image
import numpy as np
from rapidocr_onnxruntime import RapidOCR

ROOT = Path(r"i:\TTRPG\Visuales\Decks\Deckapp")
ASSETS = ROOT / "core/data/src/main/assets/starter_packs"
SOURCE_BASE = Path(r"H:\Contenido Personal - Intereses Específicos\download\The Story Engine_ Deck of Worlds_All [2022-06-04]")
SCRATCH = Path(r"C:\Users\Giise\.gemini\antigravity-ide\brain\3591deda-8ce7-4918-a81a-6fdfe790dcc2\scratch")

print("Initializing RapidOCR engine...")
engine = RapidOCR()

# Translation and clean maps
EN_ES_MAP = {
    # Biomes & geography
    "SEA ICE": "Hielo Marino", "MOUNTAIN(S)": "Montaña(s)", "TAIGA": "Taiga", "TUNDRA": "Tundra",
    "GLACIER": "Glaciar", "VILLAGE": "Aldea", "SHELTER": "Refugio", "RIVER": "Río",
    "ICEBERG": "Iceberg", "TOWN": "Pueblo", "POLE": "Polo", "SNOW(S)": "Nieve(s)",
    "FOOTHILL(S)": "Colina(s) Bajas", "AERIE": "Nido / Nidal", "SPA": "Termas", "SPRING": "Manantial",
    "COPSE": "Bosquecillo", "ROAD": "Camino", "HALL": "Salón / Fortaleza", "FIELD": "Campo",
    "PASS": "Paso de Montaña", "CAMP": "Campamento", "MINE": "Mina", "LAKE": "Lago", "CHASM": "Abismo",
    "PENINSULA": "Península", "BAY": "Bahía", "ISLAND(S)": "Isla(s)", "COAST": "Costa",
    "SAND(S)": "Arena(s)", "MESA": "Meseta", "DUNE(S)": "Duna(s)", "DESERT": "Desierto",
    "STEPPE": "Estepa", "GULCH": "Barranco", "MIRAGE": "Espejismo", "CACTI": "Cactus",
    "OASIS": "Oasis", "CITY": "Ciudad", "BADLAND(S)": "Tierras Baldías", "CREEK": "Arroyo",
    "FAULT": "Falla Geológica", "MOUNTAIN": "Montaña", "APIARY": "Colmenar", "RANCH": "Rancho",
    "CRAG": "Risco", "HOODOO(S)": "Hoodoo / Chimenea de Hada", "OUTPOST": "Avanzada",
    # Fantasy & Horror
    "BRIAR(S)": "Zarzal(es)", "KINGDOM": "Reino", "VALE": "Valle",
    "PORTAL": "Portal Mágico", "TAVERN": "Taberna", "TAUERN": "Taberna", "KEEP": "Torreón",
    "APOTHECARY": "Boticario", "CAIRN": "Túmulo / Mojón", "MOUNT": "Monte",
    "FORT": "Fuerte", "MARKET": "Mercado", "SHRINE": "Santuario", "DALE": "Cañada",
    "CASTLE": "Castillo", "FORGE": "Fragua", "CROSSROAD": "Encrucijada", "STAIR": "Escalinata",
    "CATHEDRAL": "Catedral", "OBELISK": "Obelisco",
    "WOOD(S)": "Bosque(s) Umbrío(s)", "PIT(S)": "Fosa(s)", "NECROPOLIS": "Necrópolis",
    "TOMB": "Tumba", "BATTLEFIELD": "Campo de Batalla", "LAIR": "Guarida", "MOOR": "Páramo",
    "SLAUGHTERHOUSE": "Matadero", "CREMATORIUM": "Crematorio", "NEST": "Nido", "ABYSS": "Abismo",
    "SWAMP": "Ciénaga", "ARENA": "Arena / Coliseo", "CABIN": "Cabaña", "WAREHOUSE": "Almacén",
    "RUIN": "Ruina", "LABYRINTH": "Laberinto", "FENCE": "Valla / Cercado", "MANSION": "Mansión",
    "CHAPEL": "Capilla", "CARNIVAL": "Feria / Carnaval", "CARNIUAL": "Feria / Carnaval",
    # SciFi
    "FLOE(S)": "Banquisa(s)", "METROPOLIS": "Metrópolis", "CAPITAL": "Capital Planetaria",
    "STATION": "Estación Espacial", "COLONY": "Colonia", "CRATER": "Cráter", "REACTOR": "Reactor",
    "LAB": "Laboratorio", "HANGAR": "Hangar", "VAULT": "Bóveda", "DOME": "Cúpula",
    # NSE Boosters, Backstories, Mystery, Unique Items
    "AN ALCHEMIST": "Un Alquimista", "A DEMONIAC": "Un Demonólogo", "AN ACROBAT": "Un Acróbata",
    "A CHRONOSHIFTER": "Un Cronoviajero", "A BILLIONAIRE": "Un Multimillonario",
    "A SUPERVILLAIN": "Un Supervillano", "A SUPERUILLAIN": "Un Supervillano",
    "A BIOLOGIST": "Un Biólogo", "A THRILLSEEKER": "Un Temerario",
    "A SUPERHERO": "Un Superhéroe", "A CHAMPION": "Un Campeón",
    "A PHANTASM": "Un Fantasma", "A SCRIBE": "Un Escriba", "A LIGHTHOUSE": "Un Faro",
    "A GARGOYLE": "Una Gárgola", "A WRAITH": "Un Espectro", "A COUNTESS": "Una Condesa",
    "A SAILOR": "Un Marinero", "A NURSE": "Un Enfermero",
    "A GUMSHOE": "Un Detective", "A BUTLER": "Un Mayordomo", "A CLEANER": "Un Limpiador",
    "A DUKE": "Un Duque", "A RINGLEADER": "Un Cabecilla", "AN ANTIQUITIES": "Un Anticuario",
    "A HUSTLER": "Un Estafador", "A FIXER": "Un Arreglador", "A MYSTERY": "Un Autor de Misterio",
    "AN EX-CON": "Un Ex-Convicto", "A RACKETEER": "Un Mafioso", "A MOURNER": "Un Doliente",
    "A SOCIALITE": "Un Miembro de la Alta Sociedad", "A TYCOON": "Un Magnate",
    "A SWINDLER": "Un Timador", "AN EYEWITNESS": "Un Testigo Ocular",
    "WANTS TO": "Desea", "WANTS TO ESCAPE": "Desea Escapar", "WANTS TO PROTECT": "Desea Proteger",
    "WANTS TO DESTROY": "Desea Destruir", "WANTS TO REVEAL": "Desea Revelar",
}

JUNK_RE = re.compile(
    r"\b(peter|chiykowski|story|engine|deck|worlds|expansion|boosters|storyenginedeck|"
    r"deckofworlds|resources|visit|2022|for|more|rlds|tm|the|and|of|card)\b|[©@®™]",
    re.IGNORECASE
)

NOISE_WORDS = {
    "o", "r", "n", "oi", "esources", "boc", "apseui", "ientist", "grau", "obber",
    "ids", "tm", "rlds", "mate", "free", "visit", "resources", "expansions", "new",
    "keeper", "phobe", "the", "and", "of", "in", "to", "for", "deck", "worlds", "card"
}

def clean_and_translate_cue(text: str) -> str:
    words = text.split()
    clean_words = []
    for w in words:
        clean_w = re.sub(r"^[^a-zA-Z\(\)]+|[^a-zA-Z\(\)]+$", "", w)
        if not clean_w:
            continue
        if len(clean_w) == 1 and clean_w.upper() not in ("A", "I"):
            continue
        if clean_w.lower() in NOISE_WORDS:
            continue
        if JUNK_RE.search(clean_w):
            continue
        if re.match(r"^\d+/\d+$", clean_w):
            continue
        clean_words.append(clean_w)
    
    phrase = " ".join(clean_words).strip()
    if not phrase:
        return ""
    
    upper_phrase = phrase.upper()
    if upper_phrase in EN_ES_MAP:
        return f"{phrase} ({EN_ES_MAP[upper_phrase]})"
    
    translated_count = 0
    tokens_es = []
    for token in clean_words:
        up = token.upper()
        if up in EN_ES_MAP:
            tokens_es.append(EN_ES_MAP[up])
            translated_count += 1
        else:
            tokens_es.append(token)
    
    if translated_count > 0:
        combined_es = " · ".join(tokens_es) if len(tokens_es) > 1 else tokens_es[0]
        return f"{phrase} ({combined_es})"
    return phrase

def ocr_img(img: Image.Image) -> str:
    if img.mode != "RGB":
        img = img.convert("RGB")
    res, _ = engine(np.array(img))
    if not res:
        return ""
    return " ".join(r[1].strip() for r in res if r and r[1].strip())

def extract_card_cues(img: Image.Image, mode: str) -> list:
    w, h = img.size
    if mode == "IMAGE_WITH_TEXT":
        crop = img.crop((int(w * 0.05), int(h * 0.68), int(w * 0.95), int(h * 0.95)))
        crop2x = crop.resize((crop.width * 2, crop.height * 2), Image.Resampling.LANCZOS)
        raw = ocr_img(crop2x)
        return [clean_and_translate_cue(raw)]
    elif mode == "TOP_BOTTOM_SPLIT":
        bot = img.crop((int(w * 0.05), int(h * 0.52), int(w * 0.95), h))
        bot2x = bot.resize((bot.width * 2, bot.height * 2), Image.Resampling.LANCZOS)
        top = img.crop((int(w * 0.05), 0, int(w * 0.95), int(h * 0.48))).rotate(180)
        top2x = top.resize((top.width * 2, top.height * 2), Image.Resampling.LANCZOS)
        return [clean_and_translate_cue(ocr_img(bot2x)), clean_and_translate_cue(ocr_img(top2x))]
    elif mode == "FOUR_EDGE_CUES":
        cues = []
        for pil_rot in [0, 270, 180, 90]:
            rot = img.rotate(pil_rot, expand=False)
            rw, rh = rot.size
            edge = rot.crop((int(rw * 0.07), int(rh * 0.70), int(rw * 0.93), rh))
            edge2x = edge.resize((edge.width * 2, edge.height * 2), Image.Resampling.LANCZOS)
            cues.append(clean_and_translate_cue(ocr_img(edge2x)))
        return cues
    return []

def get_mini_expansion_page_info(page_idx: int):
    # For Deck of Worlds 20-page mini expansions (p0..p19, pairs of front/back):
    # p0, p2: Region (12 cards)
    # p4, p6: Landmark (12 cards)
    # p8, p10: Namesake (12 cards)
    # p12: Origin (6 cards)
    # p14, p16: Attribute (12 cards)
    # p18: Advent (6 cards)
    if page_idx in (0, 2):
        return ("Región", "IMAGE_WITH_TEXT", "back_region.webp")
    elif page_idx in (4, 6):
        return ("Punto de Interés", "TOP_BOTTOM_SPLIT", "back_landmark.webp")
    elif page_idx in (8, 10):
        return ("Apelativo", "FOUR_EDGE_CUES", "back_namesake.webp")
    elif page_idx == 12:
        return ("Origen", "FOUR_EDGE_CUES", "back_origin.webp")
    elif page_idx in (14, 16):
        return ("Atributo", "FOUR_EDGE_CUES", "back_attribute.webp")
    elif page_idx == 18:
        return ("Acontecimiento", "TOP_BOTTOM_SPLIT", "back_advent.webp")
    return ("Carta", "TOP_BOTTOM_SPLIT", "back_card.webp")

def get_creative_page_info(pack_key: str, page_idx: int):
    if pack_key == "creative_adventure_prompts":
        return ("Gancho de Aventura", "FOUR_EDGE_CUES", "back_adventure.webp")
    elif pack_key == "creative_culture_keyholes":
        return ("Llave de Cultura", "FOUR_EDGE_CUES", "back_keyhole.webp")
    elif pack_key == "creative_lore_fragments":
        if page_idx < 10:
            return ("Opus de Lore", "FOUR_EDGE_CUES", "back_opus.webp")
        else:
            return ("Florecimiento", "FOUR_EDGE_CUES", "back_flourish.webp")
    return ("Creativo", "FOUR_EDGE_CUES", "back_creative.webp")

def get_nse_page_info(pack_key: str, page_idx: int):
    if pack_key == "nse_boosters":
        if page_idx < 8:
            return ("Agente", "TOP_BOTTOM_SPLIT", "back_agent.webp")
        elif page_idx < 16:
            return ("Motor", "TOP_BOTTOM_SPLIT", "back_engine.webp")
        elif page_idx < 24:
            return ("Ancla", "TOP_BOTTOM_SPLIT", "back_anchor.webp")
        elif page_idx < 32:
            return ("Conflicto", "TOP_BOTTOM_SPLIT", "back_conflict.webp")
        else:
            return ("Aspecto", "FOUR_EDGE_CUES", "back_aspect.webp")
    elif pack_key == "nse_backstories":
        if page_idx < 10:
            return ("Motor de Trasfondo", "FOUR_EDGE_CUES", "back_backstory_engine.webp")
        else:
            return ("Conflicto de Trasfondo", "FOUR_EDGE_CUES", "back_backstory_conflict.webp")
    elif pack_key == "nse_mystery":
        if page_idx < 4:
            return ("Agente", "TOP_BOTTOM_SPLIT", "back_agent.webp")
        elif page_idx < 8:
            return ("Motor", "TOP_BOTTOM_SPLIT", "back_engine.webp")
        elif page_idx < 12:
            return ("Ancla", "TOP_BOTTOM_SPLIT", "back_anchor.webp")
        elif page_idx < 16:
            return ("Conflicto", "TOP_BOTTOM_SPLIT", "back_conflict.webp")
        else:
            return ("Aspecto", "FOUR_EDGE_CUES", "back_aspect.webp")
    elif pack_key == "nse_unique_items":
        if page_idx < 4:
            return ("Ancla", "TOP_BOTTOM_SPLIT", "back_anchor.webp")
        else:
            return ("Aspecto", "FOUR_EDGE_CUES", "back_aspect.webp")
    return ("Story Engine", "TOP_BOTTOM_SPLIT", "back_nse.webp")

EXPANSION_CONFIGS = [
    # 9 Deck of Worlds Expansions
    dict(deck_id=107, pack_key="cartographic_arctic", kind="dow_mini",
         folder="Cartography_Expansion_Arctic_PnP", pdf="Cartography Expansion_Arctic_HQ Cards.pdf",
         prefix="arctic", name_es="Deck of Worlds: Exp. Cartográfica - Ártico",
         desc_es="Regiones heladas, glaciares, tundras y fenómenos polares para el Deck of Worlds.",
         color=-11677985, sort_order=7),
    dict(deck_id=108, pack_key="cartographic_coastlines", kind="dow_mini",
         folder="Cartography_Expansion_Coastlines_PnP", pdf="Cartography Expansion_Coastlines_HQ Cards.pdf",
         prefix="coastlines", name_es="Deck of Worlds: Exp. Cartográfica - Costas",
         desc_es="Penínsulas, acantilados costeros, bahías y archipiélagos.",
         color=-12730524, sort_order=8),
    dict(deck_id=109, pack_key="cartographic_deserts", kind="dow_mini",
         folder="Cartography_Expansion_Deserts_PnP", pdf="Cartography Expansion_Deserts_HQ Cards.pdf",
         prefix="deserts", name_es="Deck of Worlds: Exp. Cartográfica - Desiertos",
         desc_es="Dunas, oasis, cañones áridos y misterios del desierto.",
         color=-2095873, sort_order=9),
    dict(deck_id=110, pack_key="creative_adventure_prompts", kind="dow_creative",
         folder="Creative_Expansion_Adventure_Prompts_PnP", pdf="Creative Expansion_Adventure Prompts_HQ Cards.pdf",
         prefix="adventure", name_es="Deck of Worlds: Exp. Creativa - Ganchos de Aventura",
         desc_es="Detonantes de misiones, crisis locales y giros dramáticos en las 4 orientaciones.",
         color=-5952982, sort_order=10),
    dict(deck_id=111, pack_key="creative_culture_keyholes", kind="dow_creative",
         folder="Creative_Expansion_Culture_Keyholes_PnP", pdf="Creative Expansion_Culture Keyholes_HQ Cards.pdf",
         prefix="culture", name_es="Deck of Worlds: Exp. Creativa - Llaves de Cultura",
         desc_es="Preguntas e indicios culturales, rituales y costumbres de los pueblos.",
         color=-7620206, sort_order=11),
    dict(deck_id=112, pack_key="creative_lore_fragments", kind="dow_creative",
         folder="Creative_Expansion_Lore_Fragments_PnP", pdf="Creative Expansion_Lore Fragments_HQ Cards.pdf",
         prefix="lore", name_es="Deck of Worlds: Exp. Creativa - Fragmentos de Lore",
         desc_es="Mitos, cantos, rumores y fragmentos históricos del microescenario.",
         color=-10066278, sort_order=12),
    dict(deck_id=113, pack_key="genre_fantasy", kind="dow_mini",
         folder="Genre_Expansion_Fantasy_PnP", pdf="Genre Expansion_Fantasy_HQ Cards.pdf",
         prefix="fantasy", name_es="Deck of Worlds: Exp. de Género - Fantasía",
         desc_es="Pistas temáticas de alta fantasía: magia ancestral, monstruos y ruinas arcanas.",
         color=-6736896, sort_order=13),
    dict(deck_id=114, pack_key="genre_horror", kind="dow_mini",
         folder="Genre_Expansion_Horror_PnP", pdf="Genre Expansion_Horror_HQ Cards.pdf",
         prefix="horror", name_es="Deck of Worlds: Exp. de Género - Horror",
         desc_es="Presagios oscuros, cultos siniestros, cementerios y pesadillas cósmicas.",
         color=-14540254, sort_order=14),
    dict(deck_id=115, pack_key="genre_scifi", kind="dow_mini",
         folder="Genre_Expansion_Science_Fiction_PnP", pdf="Genre Expansion_Science Fiction_HQ Cards.pdf",
         prefix="scifi", name_es="Deck of Worlds: Exp. de Género - Ciencia Ficción",
         desc_es="Megaciudades, puertos estelares, tecnología y mundos alienígenas.",
         color=-14483456, sort_order=15),

    # 4 New Story Engine Expansions
    dict(deck_id=201, pack_key="nse_boosters", kind="nse",
         folder="New_Story_Engine_Boosters_PnP", pdf="New Story Engine Boosters_HQ Cards.pdf",
         prefix="nse_booster", name_es="New Story Engine: Potenciadores (Boosters)",
         desc_es="Arquetipos, profesiones y conceptos amplificados para generar personajes y tramas.",
         color=-1158417, sort_order=1),
    dict(deck_id=202, pack_key="nse_backstories", kind="nse",
         folder="New_Story_Engine_Expansion_Backstories_PnP", pdf="New Story Engine Expansion_Backstories_HQ Cards.pdf",
         prefix="nse_backstory", name_es="New Story Engine: Trasfondos (Backstories)",
         desc_es="Orígenes, motivaciones profundas y secretos en 4 direcciones.",
         color=-8825528, sort_order=2),
    dict(deck_id=203, pack_key="nse_mystery", kind="nse",
         folder="New_Story_Engine_Expansion_Mystery_PnP", pdf="New Story Engine Expansion_Mystery_HQ Cards.pdf",
         prefix="nse_mystery", name_es="New Story Engine: Misterios (Mystery)",
         desc_es="Sospechosos, crímenes, conspiraciones y enigmas para novelas negras o aventuras de investigación.",
         color=-10395295, sort_order=3),
    dict(deck_id=204, pack_key="nse_unique_items", kind="nse",
         folder="New_Story_Engine_Expansion_Unique_Items_PnPs", pdf="New Story Engine Expansion_Unique Items_HQ Cards.pdf",
         prefix="nse_item", name_es="New Story Engine: Objetos Únicos (Unique Items)",
         desc_es="Reliquias, artefactos con propiedades singulares y curiosidades narrativas.",
         color=-6973030, sort_order=4),
]

def process_single_expansion(cfg: dict, card_id_counter: list, face_id_counter: list):
    pdf_path = SOURCE_BASE / cfg["folder"] / cfg["pdf"]
    if not pdf_path.exists():
        print(f"Error: {pdf_path} not found!")
        return None

    pack_key = cfg["pack_key"]
    deck_id = cfg["deck_id"]
    prefix = cfg["prefix"]
    kind = cfg["kind"]
    out_cards_dir = ASSETS / pack_key / "cards"
    out_cards_dir.mkdir(parents=True, exist_ok=True)

    doc = fitz.open(str(pdf_path))
    total_pages = len(doc)
    print(f"\nProcessing [{deck_id}] {cfg['name_es']} ({total_pages} pages)...")

    scale = 200 / 72  # 650x650 px
    mat = fitz.Matrix(scale, scale)

    # Saved backs cache per filename
    saved_backs = set()

    cards_dto = []
    card_faces_dto = []
    card_order = 0

    # Iterate front pages (0, 2, 4...)
    for p in range(0, total_pages, 2):
        front_page = doc[p]
        back_page = doc[p + 1] if p + 1 < total_pages else None

        # Determine suit, mode, and back filename for this sheet
        if kind == "dow_mini":
            suit, mode, back_filename = get_mini_expansion_page_info(p)
        elif kind == "dow_creative":
            suit, mode, back_filename = get_creative_page_info(pack_key, p)
        else:
            suit, mode, back_filename = get_nse_page_info(pack_key, p)

        # Ensure the shared back image is saved
        back_file_path = out_cards_dir / back_filename
        if back_page and back_filename not in saved_backs:
            back_rect = fitz.Rect(72, 45, 306, 279)
            pix_back = back_page.get_pixmap(clip=back_rect, matrix=mat, alpha=False)
            img_back = Image.frombytes("RGB", [pix_back.width, pix_back.height], pix_back.samples)
            img_back.save(str(back_file_path), "WEBP", quality=85)
            saved_backs.add(back_filename)

        # 6 cards per sheet: 3 rows x 2 cols
        for r in range(3):
            for c in range(2):
                card_order += 1
                c_id = card_id_counter[0]
                f_id = face_id_counter[0]
                card_id_counter[0] += 1
                face_id_counter[0] += 1

                # Front crop
                front_rect = fitz.Rect(72.0 + c * 234.0, 45.0 + r * 234.0, 72.0 + (c + 1) * 234.0, 45.0 + (r + 1) * 234.0)
                pix_front = front_page.get_pixmap(clip=front_rect, matrix=mat, alpha=False)
                img_front = Image.frombytes("RGB", [pix_front.width, pix_front.height], pix_front.samples)
                
                front_filename = f"{prefix}_{card_order:03d}.webp"
                front_file_path = out_cards_dir / front_filename
                img_front.save(str(front_file_path), "WEBP", quality=85)

                # Extract Cues via OCR
                cues = extract_card_cues(img_front, mode)
                
                # Format zonesJson conforming to List<ContentZone>
                zones_data = [{"text": cue} for cue in cues if cue]
                zones_json = json.dumps(zones_data, ensure_ascii=False)

                # Format title
                if cues and any(c.strip() for c in cues):
                    valid_cues = [c for c in cues if c.strip()]
                    if len(valid_cues) == 1:
                        summary = valid_cues[0][:35]
                    else:
                        summary = f"{valid_cues[0][:20]} / {valid_cues[1][:20]}"
                    card_title = f"{suit} #{card_order:02d}: {summary}"
                else:
                    card_title = f"{suit} #{card_order:02d}"

                front_uri = f"file:///android_asset/starter_packs/{pack_key}/cards/{front_filename}"
                back_uri = f"file:///android_asset/starter_packs/{pack_key}/cards/{back_filename}"

                cards_dto.append({
                    "id": c_id,
                    "stackId": deck_id,
                    "originDeckId": deck_id,
                    "title": card_title,
                    "suit": suit,
                    "value": card_order,
                    "currentFaceIndex": 0,
                    "currentRotation": 0,
                    "isReversed": False,
                    "isDrawn": False,
                    "isRevealed": True,
                    "sortOrder": card_order - 1,
                    "linkedTableId": None,
                    "dmNotes": None,
                    "lastDrawnAt": None
                })

                card_faces_dto.append({
                    "id": f_id,
                    "cardId": c_id,
                    "faceIndex": 0,
                    "name": card_title,
                    "imagePath": front_uri,
                    "contentMode": mode,
                    "zonesJson": zones_json,
                    "reversedImagePath": back_uri
                })

    doc.close()
    print(f"  Extracted {len(cards_dto)} cards for {cfg['name_es']}")

    # Deck definition
    cover_uri = f"file:///android_asset/starter_packs/{pack_key}/cards/{prefix}_001.webp"
    default_back_uri = f"file:///android_asset/starter_packs/{pack_key}/cards/{list(saved_backs)[0]}" if saved_backs else None

    deck_dto = {
        "id": deck_id,
        "name": cfg["name_es"],
        "type": "DECK",
        "description": cfg["desc_es"],
        "coverImagePath": cover_uri,
        "sourceFolderPath": None,
        "defaultContentMode": "FOUR_EDGE_CUES" if "Creativa" in cfg["name_es"] or "Backstories" in cfg["name_es"] else "TOP_BOTTOM_SPLIT",
        "drawMode": "RANDOM",
        "drawFaceDown": False,
        "backImagePath": default_back_uri,
        "displayCount": True,
        "aspectRatio": "SQUARE" if "Deck of Worlds" in cfg["name_es"] else "STANDARD",
        "isArchived": False,
        "sortOrder": cfg["sort_order"],
        "createdAt": 1729250000000 + deck_id * 1000
    }

    return {
        "deck": deck_dto,
        "cards": cards_dto,
        "faces": card_faces_dto
    }

def main():
    print("=== Story Engine & Deck of Worlds Expansions Regenerator ===")
    card_id_counter = [10001]
    face_id_counter = [20001]

    # Shared Tags
    tags_dto = [
        {"id": 1, "name": "Deck of Worlds", "color": -14457476},
        {"id": 2, "name": "Story Engine", "color": -11913386},
        {"id": 3, "name": "Worldbuilding", "color": -7049442}
    ]

    # Existing System Rules
    dow_exp_rule = {
        "id": 101,
        "title": "Deck of Worlds: Guía de Expansiones Cartográficas y de Género",
        "gameSystem": "Deck of Worlds",
        "category": "Reglas de Mundo",
        "content": "# Deck of Worlds: Expansiones Cartográficas, Creativas y de Género\n\nLas expansiones amplían el núcleo del **Deck of Worlds** con cartas temáticas divididas en 6 tipos oficiales (Regiones, Puntos de Interés, Nombres, Orígenes, Atributos y Acontecimientos):\n\n### 1. Expansiones Cartográficas (Biomas Específicos)\n- **Ártico**: Tundras, glaciares, hielo marino y asentamientos boreales.\n- **Costas**: Litorales, acantilados, bahías, archipiélagos y rutas marinas.\n- **Desiertos**: Dunas, oasis, cañones secos y civilizaciones del polvo.\n\n### 2. Expansiones Creativas (Detonantes de Trama)\n- **Ganchos de Aventura**: Crisis locales, misterios y llamadas a la acción.\n- **Llaves de Cultura**: Costumbres, tabúes, festivales e identidades comunitarias.\n- **Fragmentos de Lore**: Mitos, leyendas del pasado y restos de imperios caídos.\n\n### 3. Expansiones de Género (Inyección Temática)\n- **Fantasía**: Magia arcana, ruinas sagradas y órdenes míticas.\n- **Horror**: Pesadillas cósmicas, maldiciones y lugares prohibidos.\n- **Ciencia Ficción**: Megaciudades, puertos estelares y maravillas alienígenas.",
        "isPinned": True,
        "sortOrder": 1,
        "sourcePack": "The Story Engine: Deck of Worlds"
    }

    nse_rule = {
        "id": 201,
        "title": "New Story Engine: Cómo Funciona el Motor de Historias",
        "gameSystem": "Story Engine",
        "category": "Reglas de Narrativa",
        "content": "# New Story Engine: Motor de Historias\n\nEl **New Story Engine** es un generador dinámico de tramas, personajes y conflictos dramáticos:\n\n### Tipos de Cartas y Mazos:\n1. **Potenciadores (Boosters)**: Roles centrales, conceptos dramáticos y giros para personajes.\n2. **Trasfondos (Backstories)**: Motivaciones profundas, traumas, pasados oscuros y deudas pendientes (4 orientaciones).\n3. **Misterios (Mystery)**: Sospechosos, crímenes, móviles y secretos ocultos ideales para intriga y novelas negras.\n4. **Objetos Únicos (Unique Items)**: Artefactos con propiedades extraordinarias y ganchos que impulsan la trama.\n\n### Cómo Usar en Juego:\n- Roba una carta de cada mazo para estructurar una premisa narrativa completa: un Agente con un Trasfondo se enfrenta a un Misterio armado con un Objeto Único.",
        "isPinned": True,
        "sortOrder": 1,
        "sourcePack": "The Story Engine: New Story Engine"
    }

    dow_results = []
    nse_results = []

    for cfg in EXPANSION_CONFIGS:
        res = process_single_expansion(cfg, card_id_counter, face_id_counter)
        if not res:
            continue
        if "Deck of Worlds" in cfg["name_es"]:
            dow_results.append(res)
        else:
            nse_results.append(res)

    # 1. Build story_engine_deck_of_worlds_expansions.json
    dow_decks = [r["deck"] for r in dow_results]
    dow_cards = []
    dow_faces = []
    dow_card_tags = []
    dow_stack_tags = []

    for r in dow_results:
        deck_id = r["deck"]["id"]
        dow_stack_tags.append({"stackId": deck_id, "tagId": 1})
        dow_stack_tags.append({"stackId": deck_id, "tagId": 3})
        for c in r["cards"]:
            dow_cards.append(c)
            dow_card_tags.append({"cardId": c["id"], "tagId": 1})
            dow_card_tags.append({"cardId": c["id"], "tagId": 3})
        for f in r["faces"]:
            dow_faces.append(f)

    dow_pack = {
        "schemaVersion": 1,
        "tags": tags_dto,
        "decks": dow_decks,
        "cards": dow_cards,
        "cardFaces": dow_faces,
        "cardStackTags": dow_stack_tags,
        "cardTags": dow_card_tags,
        "tableBundles": [], "randomTables": [], "tableEntries": [], "randomTableTags": [],
        "sessions": [], "sessionDeckRefs": [], "sessionTableRefs": [], "drawEvents": [],
        "encounters": [], "encounterCreatures": [], "combatLog": [], "npcs": [], "npcTags": [],
        "wikiCategories": [], "wikiEntries": [], "referenceTables": [], "referenceRows": [], "referenceTableTags": [],
        "systemRules": [dow_exp_rule],
        "systemRuleTags": [{"ruleId": 101, "tagId": 1}, {"ruleId": 101, "tagId": 3}],
        "collections": [], "collectionResourceRefs": [], "tacticalMaps": [], "tacticalTokens": [], "tacticalMapPins": []
    }

    dow_pack_path = ASSETS / "story_engine_deck_of_worlds_expansions.json"
    dow_pack_path.write_text(json.dumps(dow_pack, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"\nWritten {dow_pack_path.name}: {len(dow_decks)} decks, {len(dow_cards)} cards, {len(dow_faces)} faces")

    # 2. Build new_story_engine.json
    nse_decks = [r["deck"] for r in nse_results]
    nse_cards = []
    nse_faces = []
    nse_card_tags = []
    nse_stack_tags = []

    for r in nse_results:
        deck_id = r["deck"]["id"]
        nse_stack_tags.append({"stackId": deck_id, "tagId": 2})
        nse_stack_tags.append({"stackId": deck_id, "tagId": 3})
        for c in r["cards"]:
            nse_cards.append(c)
            nse_card_tags.append({"cardId": c["id"], "tagId": 2})
            nse_card_tags.append({"cardId": c["id"], "tagId": 3})
        for f in r["faces"]:
            nse_faces.append(f)

    nse_pack = {
        "schemaVersion": 1,
        "tags": tags_dto,
        "decks": nse_decks,
        "cards": nse_cards,
        "cardFaces": nse_faces,
        "cardStackTags": nse_stack_tags,
        "cardTags": nse_card_tags,
        "tableBundles": [], "randomTables": [], "tableEntries": [], "randomTableTags": [],
        "sessions": [], "sessionDeckRefs": [], "sessionTableRefs": [], "drawEvents": [],
        "encounters": [], "encounterCreatures": [], "combatLog": [], "npcs": [], "npcTags": [],
        "wikiCategories": [], "wikiEntries": [], "referenceTables": [], "referenceRows": [], "referenceTableTags": [],
        "systemRules": [nse_rule],
        "systemRuleTags": [{"ruleId": 201, "tagId": 2}, {"ruleId": 201, "tagId": 3}],
        "collections": [], "collectionResourceRefs": [], "tacticalMaps": [], "tacticalTokens": [], "tacticalMapPins": []
    }

    nse_pack_path = ASSETS / "new_story_engine.json"
    nse_pack_path.write_text(json.dumps(nse_pack, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Written {nse_pack_path.name}: {len(nse_decks)} decks, {len(nse_cards)} cards, {len(nse_faces)} faces")
    print("\nAll expansions regenerated successfully!")

if __name__ == "__main__":
    main()
