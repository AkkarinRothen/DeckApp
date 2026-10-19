"""
Master processor for all Story Engine expansion packs.

Converts each expansion PDF to webp card images, runs OCR via RapidOCR,
generates starter pack JSON files with raw OCR text (ready for Spanish translation).

Expansions:
  Deck of Worlds expansions (IDs 107-115):
    107: Cartographic - Arctic
    108: Cartographic - Coastlines
    109: Cartographic - Deserts
    110: Creative - Adventure Prompts
    111: Creative - Culture Keyholes
    112: Creative - Lore Fragments
    113: Genre - Fantasy
    114: Genre - Horror
    115: Genre - Sci-Fi

  New Story Engine expansions (IDs 201-204):
    201: NSE Boosters
    202: NSE Backstories
    203: NSE Mystery
    204: NSE Unique Items

Usage:
    python scripts/process_all_expansions.py
"""

import json, sys, shutil
from pathlib import Path
import fitz  # PyMuPDF
from PIL import Image
import numpy as np
from rapidocr_onnxruntime import RapidOCR

# Paths
ROOT = Path(r"i:\TTRPG\Visuales\Decks\Deckapp")
ASSETS = ROOT / "core/data/src/main/assets/starter_packs"
SOURCE_BASE = Path(
    r"H:\Contenido Personal - Intereses Especificos\download"
    r"\The Story Engine_ Deck of Worlds_All [2022-06-04]"
)
# Workaround for special chars
import os
SOURCE_BASE = None  # Will be set dynamically

def get_source_base():
    candidates = [
        Path(r"H:\Contenido Personal - Intereses Específicos\download\The Story Engine_ Deck of Worlds_All [2022-06-04]"),
    ]
    for c in candidates:
        if c.exists():
            return c
    raise FileNotFoundError("Source base not found")

SCRATCH = Path(
    r"C:\Users\Giise\.gemini\antigravity-ide\brain"
    r"\3591deda-8ce7-4918-a81a-6fdfe790dcc2\scratch"
)

EXPANSIONS = [
    # Cartographic
    dict(deck_id=107, pack_key="cartographic_arctic",
         source_folder="Cartography_Expansion_Arctic_PnP",
         pdf_name="Cartography Expansion_Arctic_HQ Cards.pdf",
         card_prefix="arctic", color_hex=-11677985, sort_order=7,
         card_mode="TOP_BOTTOM_SPLIT",
         name_es="Exp. Cartografica: Artico",
         description_es="Regiones y puntos de interes del artico y tundra glaciar.",
         tag_ids=[1, 3]),
    dict(deck_id=108, pack_key="cartographic_coastlines",
         source_folder="Cartography_Expansion_Coastlines_PnP",
         pdf_name="Cartography Expansion_Coastlines_HQ Cards.pdf",
         card_prefix="coastlines", color_hex=-12730524, sort_order=8,
         card_mode="TOP_BOTTOM_SPLIT",
         name_es="Exp. Cartografica: Costas",
         description_es="Litorales, acantilados y zonas costeras para el Deck of Worlds.",
         tag_ids=[1, 3]),
    dict(deck_id=109, pack_key="cartographic_deserts",
         source_folder="Cartography_Expansion_Deserts_PnP",
         pdf_name="Cartography Expansion_Deserts_HQ Cards.pdf",
         card_prefix="deserts", color_hex=-2095873, sort_order=9,
         card_mode="TOP_BOTTOM_SPLIT",
         name_es="Exp. Cartografica: Desiertos",
         description_es="Dunas, oasis y accidentes deserticos para el Deck of Worlds.",
         tag_ids=[1, 3]),
    # Creative
    dict(deck_id=110, pack_key="creative_adventure_prompts",
         source_folder="Creative_Expansion_Adventure_Prompts_PnP",
         pdf_name="Creative Expansion_Adventure Prompts_HQ Cards.pdf",
         card_prefix="adventure", color_hex=-5952982, sort_order=10,
         card_mode="FOUR_EDGE_CUES",
         name_es="Exp. Creativa: Ganchos de Aventura",
         description_es="Detonantes de aventura, obstaculos y giros narrativos para sesiones dinamicas.",
         tag_ids=[1, 3]),
    dict(deck_id=111, pack_key="creative_culture_keyholes",
         source_folder="Creative_Expansion_Culture_Keyholes_PnP",
         pdf_name="Creative Expansion_Culture Keyholes_HQ Cards.pdf",
         card_prefix="culture", color_hex=-7620206, sort_order=11,
         card_mode="FOUR_EDGE_CUES",
         name_es="Exp. Creativa: Llaves de Cultura",
         description_es="Costumbres, rituales y senas de identidad cultural de cada region.",
         tag_ids=[1, 3]),
    dict(deck_id=112, pack_key="creative_lore_fragments",
         source_folder="Creative_Expansion_Lore_Fragments_PnP",
         pdf_name="Creative Expansion_Lore Fragments_HQ Cards.pdf",
         card_prefix="lore", color_hex=-10066278, sort_order=12,
         card_mode="FOUR_EDGE_CUES",
         name_es="Exp. Creativa: Fragmentos de Lore",
         description_es="Mitos, leyendas, rumores y fragmentos de historia del mundo.",
         tag_ids=[1, 3]),
    # Genre
    dict(deck_id=113, pack_key="genre_fantasy",
         source_folder="Genre_Expansion_Fantasy_PnP",
         pdf_name="Genre Expansion_Fantasy_HQ Cards.pdf",
         card_prefix="fantasy", color_hex=-6736896, sort_order=13,
         card_mode="TOP_BOTTOM_SPLIT",
         name_es="Exp. de Genero: Fantasia",
         description_es="Pistas tematicas de fantasia epica: magia, bestiarios, gremios y artefactos.",
         tag_ids=[1, 3]),
    dict(deck_id=114, pack_key="genre_horror",
         source_folder="Genre_Expansion_Horror_PnP",
         pdf_name="Genre Expansion_Horror_HQ Cards.pdf",
         card_prefix="horror", color_hex=-14540254, sort_order=14,
         card_mode="TOP_BOTTOM_SPLIT",
         name_es="Exp. de Genero: Horror",
         description_es="Pistas para mundos de terror: maldiciones, cultos, criaturas y locura.",
         tag_ids=[1, 3]),
    dict(deck_id=115, pack_key="genre_scifi",
         source_folder="Genre_Expansion_Science_Fiction_PnP",
         pdf_name="Genre Expansion_Science Fiction_HQ Cards.pdf",
         card_prefix="scifi", color_hex=-14483456, sort_order=15,
         card_mode="TOP_BOTTOM_SPLIT",
         name_es="Exp. de Genero: Ciencia Ficcion",
         description_es="Planetas, facciones, tecnologia y xenobiologia para mundos de ciencia ficcion.",
         tag_ids=[1, 3]),
    # New Story Engine
    dict(deck_id=201, pack_key="nse_boosters",
         source_folder="New_Story_Engine_Boosters_PnP",
         pdf_name="New Story Engine Boosters_HQ Cards.pdf",
         card_prefix="nse_booster", color_hex=-1158417, sort_order=1,
         card_mode="TOP_BOTTOM_SPLIT",
         name_es="NSE: Potenciadores",
         description_es="Impulsores narrativos del New Story Engine: momentos dramaticos, giros y tension.",
         tag_ids=[2, 3]),
    dict(deck_id=202, pack_key="nse_backstories",
         source_folder="New_Story_Engine_Expansion_Backstories_PnP",
         pdf_name="New Story Engine Expansion_Backstories_HQ Cards.pdf",
         card_prefix="nse_backstory", color_hex=-8825528, sort_order=2,
         card_mode="FOUR_EDGE_CUES",
         name_es="NSE: Trasfondos",
         description_es="Origenes, traumas y motivaciones para personajes del New Story Engine.",
         tag_ids=[2, 3]),
    dict(deck_id=203, pack_key="nse_mystery",
         source_folder="New_Story_Engine_Expansion_Mystery_PnP",
         pdf_name="New Story Engine Expansion_Mystery_HQ Cards.pdf",
         card_prefix="nse_mystery", color_hex=-10395295, sort_order=3,
         card_mode="TOP_BOTTOM_SPLIT",
         name_es="NSE: Misterios",
         description_es="Conspiraciones, enigmas y secretos oscuros para el New Story Engine.",
         tag_ids=[2, 3]),
    dict(deck_id=204, pack_key="nse_unique_items",
         source_folder="New_Story_Engine_Expansion_Unique_Items_PnPs",
         pdf_name="New Story Engine Expansion_Unique Items_HQ Cards.pdf",
         card_prefix="nse_item", color_hex=-6973030, sort_order=4,
         card_mode="FOUR_EDGE_CUES",
         name_es="NSE: Objetos Unicos",
         description_es="Artefactos, reliquias y objetos extraordinarios del New Story Engine.",
         tag_ids=[2, 3]),
]

print("Loading OCR engine...")
engine = RapidOCR()

def ocr_img(img: Image.Image) -> str:
    if img.mode != "RGB":
        img = img.convert("RGB")
    res, _ = engine(np.array(img))
    if not res:
        return ""
    return " ".join(r[1].strip() for r in res if r and r[1].strip())

def extract_cues(img: Image.Image, mode: str) -> list:
    w, h = img.size
    if mode == "IMAGE_WITH_TEXT":
        crop = img.crop((int(w*0.05), int(h*0.3), int(w*0.95), int(h*0.7)))
        crop2x = crop.resize((crop.width*2, crop.height*2), Image.Resampling.LANCZOS)
        return [ocr_img(crop2x)]
    elif mode == "TOP_BOTTOM_SPLIT":
        bot = img.crop((int(w*0.05), int(h*0.52), int(w*0.95), h))
        bot2x = bot.resize((bot.width*2, bot.height*2), Image.Resampling.LANCZOS)
        top = img.crop((int(w*0.05), 0, int(w*0.95), int(h*0.48))).rotate(180)
        top2x = top.resize((top.width*2, top.height*2), Image.Resampling.LANCZOS)
        return [ocr_img(bot2x), ocr_img(top2x)]
    elif mode == "FOUR_EDGE_CUES":
        cues = []
        for pil_rot in [0, 270, 180, 90]:
            rot = img.rotate(pil_rot, expand=False)
            rw, rh = rot.size
            edge = rot.crop((int(rw*0.07), int(rh*0.70), int(rw*0.93), rh))
            edge2x = edge.resize((edge.width*2, edge.height*2), Image.Resampling.LANCZOS)
            cues.append(ocr_img(edge2x))
        return cues
    return []

def rasterize_pdf(pdf_path: Path, out_dir: Path, prefix: str, dpi: int = 200) -> list:
    out_dir.mkdir(parents=True, exist_ok=True)
    doc = fitz.open(str(pdf_path))
    mat = fitz.Matrix(dpi / 72, dpi / 72)
    paths = []
    for i, page in enumerate(doc):
        pix = page.get_pixmap(matrix=mat, alpha=False)
        out_path = out_dir / f"{prefix}_{i+1:03d}.webp"
        if not out_path.exists():
            pix.save(str(out_path))
        paths.append(out_path)
    doc.close()
    print(f"  Rasterized {len(paths)} pages to {out_dir}")
    return paths

def build_pack_json(exp: dict, raw_cards: list) -> dict:
    deck_id = exp["deck_id"]
    pack_key = exp["pack_key"]
    mode = exp["card_mode"]
    prefix = exp["card_prefix"]
    ts = 1729250000000 + (deck_id * 1000)

    cards_out = []
    for card in raw_cards:
        idx = card["index"]
        filename = card["filename"]
        cues = card["cues_raw"]

        if mode == "IMAGE_WITH_TEXT":
            zones = [{"zoneIndex": 0, "rotation": 0, "label": "Pista",
                      "content": cues[0] if cues else ""}]
        elif mode == "TOP_BOTTOM_SPLIT":
            zones = [
                {"zoneIndex": 0, "rotation": 0, "label": "Inferior",
                 "content": cues[0] if len(cues)>0 else ""},
                {"zoneIndex": 1, "rotation": 180, "label": "Superior",
                 "content": cues[1] if len(cues)>1 else ""},
            ]
        elif mode == "FOUR_EDGE_CUES":
            labels = ["Sur (0deg)", "Oeste (90deg)", "Norte (180deg)", "Este (270deg)"]
            rots = [0, 90, 180, 270]
            zones = [{"zoneIndex": z, "rotation": rots[z], "label": labels[z],
                      "content": cues[z] if z < len(cues) else ""}
                     for z in range(4)]
        else:
            zones = []

        # Build short title from cues
        parts = [c[:35] for c in cues[:2] if c.strip()]
        title = f"#{idx:02d}: {' / '.join(parts)}" if parts else f"#{idx:02d}"

        cards_out.append({
            "id": -(deck_id * 1000 + idx),
            "deckId": deck_id,
            "title": title,
            "contentMode": mode,
            "imageUri": f"file:///android_asset/starter_packs/{pack_key}/cards/{filename}",
            "zonesJson": json.dumps(zones, ensure_ascii=False),
            "isArchived": False,
            "createdAt": ts + idx,
        })

    cover = (f"file:///android_asset/starter_packs/{pack_key}/cards/{raw_cards[0]['filename']}"
             if raw_cards else "")

    deck_entry = {
        "id": deck_id,
        "name": exp["name_es"],
        "type": "DECK",
        "description": exp["description_es"],
        "coverImagePath": cover,
        "sourceFolderPath": None,
        "defaultContentMode": mode,
        "drawMode": "RANDOM",
        "drawFaceDown": False,
        "backImagePath": None,
        "displayCount": True,
        "aspectRatio": "STANDARD",
        "isArchived": False,
        "sortOrder": exp["sort_order"],
        "createdAt": ts,
    }

    # Tags
    tag_defs = {
        1: {"id": 1, "name": "Deck of Worlds", "color": -14457476},
        2: {"id": 2, "name": "Story Engine", "color": -11913386},
        3: {"id": 3, "name": "Worldbuilding", "color": -7049442},
    }
    tags = [tag_defs[t] for t in exp["tag_ids"] if t in tag_defs]

    return {
        "schemaVersion": 1,
        "tags": tags,
        "decks": [deck_entry],
        "cards": cards_out,
    }

def process_expansion(exp: dict, src_base: Path) -> dict:
    pack_key = exp["pack_key"]
    mode = exp["card_mode"]
    prefix = exp["card_prefix"]

    pdf_path = src_base / exp["source_folder"] / exp["pdf_name"]
    if not pdf_path.exists():
        print(f"  WARNING: PDF not found: {pdf_path}")
        return {}

    out_dir = ASSETS / pack_key / "cards"
    print(f"\n{'='*60}")
    print(f"[{exp['deck_id']}] {exp['name_es']} | mode={mode}")
    print(f"  PDF: {pdf_path.name}")

    img_paths = rasterize_pdf(pdf_path, out_dir, prefix, dpi=200)
    total = len(img_paths)

    raw_cards = []
    for i, img_path in enumerate(img_paths, 1):
        img = Image.open(img_path)
        cues = extract_cues(img, mode)
        raw_cards.append({
            "index": i,
            "filename": img_path.name,
            "content_mode": mode,
            "cues_raw": cues,
        })
        if i % 5 == 0 or i == total:
            print(f"  OCR {i}/{total}")

    raw_path = SCRATCH / f"{pack_key}_raw_cues.json"
    raw_path.write_text(json.dumps(raw_cards, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"  Raw OCR -> {raw_path.name}")
    return {"raw_cards": raw_cards}

def main():
    src_base = get_source_base()
    print(f"Source base: {src_base}")

    for exp in EXPANSIONS:
        result = process_expansion(exp, src_base)
        if not result:
            continue

        raw_cards = result["raw_cards"]
        pack_data = build_pack_json(exp, raw_cards)

        # Write individual pack JSON
        pack_dir = ASSETS / exp["pack_key"]
        pack_dir.mkdir(parents=True, exist_ok=True)
        pack_path = pack_dir / f"{exp['pack_key']}.json"
        pack_path.write_text(
            json.dumps(pack_data, indent=2, ensure_ascii=False),
            encoding="utf-8"
        )
        print(f"  Pack JSON -> {pack_path.relative_to(ROOT)}")

    print("\nAll expansions processed!")
    print("Review raw OCR in scratch/*_raw_cues.json, then run translate_expansions.py")

if __name__ == "__main__":
    main()
