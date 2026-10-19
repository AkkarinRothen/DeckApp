"""Extract OCR cues from the 240 Story Engine Deck of Worlds cards.
"""
import json
from pathlib import Path
from PIL import Image, ImageEnhance
from rapidocr_onnxruntime import RapidOCR
import numpy as np

CARDS_DIR = Path(r"i:\TTRPG\Visuales\Decks\Deckapp\core\data\src\main\assets\starter_packs\deck_of_worlds\cards")
METADATA_PATH = Path(r"i:\TTRPG\Visuales\Decks\Deckapp\core\data\src\main\assets\starter_packs\deck_of_worlds\cards_metadata.json")
OUTPUT_RAW = Path(r"C:\Users\Giise\.gemini\antigravity-ide\brain\3591deda-8ce7-4918-a81a-6fdfe790dcc2\scratch\dow_raw_cues.json")

engine = RapidOCR()

def clean_ocr_lines(res):
    if not res:
        return ""
    lines = [r[1].strip() for r in res if r and r[1].strip()]
    return " ".join(lines)

def process_card(filename, ctype, mode, opt_count):
    img_path = CARDS_DIR / filename
    if not img_path.exists():
        return []
    
    img = Image.open(img_path)
    w, h = img.size
    
    # Deck 101: Region (1 option, centered text)
    if mode == "IMAGE_WITH_TEXT":
        # Center crop: y from 35% to 65%, full width
        center = img.crop((int(w * 0.1), int(h * 0.35), int(w * 0.9), int(h * 0.65)))
        center_2x = center.resize((center.width * 2, center.height * 2), Image.Resampling.LANCZOS)
        res, _ = engine(np.array(center_2x))
        txt = clean_ocr_lines(res)
        return [txt]
    
    # Deck 102 & 106: Landmark & Advent (2 options: bottom half = 0°, top half rotated 180° = 180°)
    elif mode == "TOP_BOTTOM_SPLIT":
        # Option 0: Bottom half (0 deg)
        b_crop = img.crop((int(w * 0.05), int(h * 0.5), int(w * 0.95), h))
        b_2x = b_crop.resize((b_crop.width * 2, b_crop.height * 2), Image.Resampling.LANCZOS)
        res_b, _ = engine(np.array(b_2x))
        txt_b = clean_ocr_lines(res_b)
        
        # Option 1: Top half rotated 180 deg
        t_crop = img.crop((int(w * 0.05), 0, int(w * 0.95), int(h * 0.5))).rotate(180)
        t_2x = t_crop.resize((t_crop.width * 2, t_crop.height * 2), Image.Resampling.LANCZOS)
        res_t, _ = engine(np.array(t_2x))
        txt_t = clean_ocr_lines(res_t)
        
        # In bottom-active convention:
        # Zone 0 is bottom (0 deg), Zone 1 is top rotated (180 deg)
        return [txt_b, txt_t]
    
    # Deck 103, 104, 105: Namesake, Origin, Attribute (4 edge cues)
    elif mode == "FOUR_EDGE_CUES":
        # Borde Inferior convention:
        # Zone 0: Sur (0 deg) - already at the bottom edge
        # Zone 1: Oeste (90 deg) - rotates 90 deg clockwise so West moves to the bottom
        # Zone 2: Norte (180 deg) - rotates 180 deg so North moves to the bottom
        # Zone 3: Este (270 deg) - rotates 270 deg so East moves to the bottom
        cues = []
        angles = [0, 90, 180, 270] # In PIL .rotate(), positive degrees is counter-clockwise.
        # To rotate card so edge moves to bottom:
        # Sur is already at bottom (rotation = 0)
        # To bring West to bottom: rotate 90 deg clockwise = 270 deg counter-clockwise (-90)
        # To bring North to bottom: rotate 180 deg
        # To bring East to bottom: rotate 270 deg clockwise = 90 deg counter-clockwise
        pil_rotations = [0, 270, 180, 90]
        
        for rot in pil_rotations:
            rot_img = img.rotate(rot)
            b_edge = rot_img.crop((int(w * 0.1), int(h * 0.72), int(w * 0.9), h))
            b_2x = b_edge.resize((b_edge.width * 2, b_edge.height * 2), Image.Resampling.LANCZOS)
            res, _ = engine(np.array(b_2x))
            cues.append(clean_ocr_lines(res))
        return cues
    
    return []

def main():
    metadata = json.loads(METADATA_PATH.read_text(encoding="utf-8"))
    results = {}
    total = 0
    for ctype, cards in metadata["cards"].items():
        results[ctype] = []
        for card in cards:
            cues = process_card(card["filename"], ctype, card["content_mode"], card["option_count"])
            results[ctype].append({
                "index": card["index"],
                "filename": card["filename"],
                "content_mode": card["content_mode"],
                "cues": cues
            })
            total += 1
            if total % 20 == 0:
                print(f"Processed {total}/240 cards...")
    
    OUTPUT_RAW.write_text(json.dumps(results, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"Finished extracting {total} cards to {OUTPUT_RAW.name}")

if __name__ == "__main__":
    main()
