"""Extract Choir of Flesh tables from book & aids, generate tactical maps and pins, and update choir_of_flesh.json.
"""
import fitz
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "core/data/src/main/assets"
PACK_PATH = ASSETS / "starter_packs/choir_of_flesh.json"
AIDS_PATH = Path("G:/Mi unidad/4.Juegos de Rol/1.Solo Roleplaying/CHOIR OF FLESH/Choir of Flesh - Sheets & Aids/Choir of Flesh - Sheets & Aids/Tables Aid_clean.pdf")
BOOK_PATH = Path("G:/Mi unidad/4.Juegos de Rol/1.Solo Roleplaying/CHOIR OF FLESH/Choir_of_Flesh_clean.pdf")

def parse_range_table(text, roll_formula):
    """Parses text containing lines like:
       01-05
       Result text...
       or
       1
       Result text...
    """
    lines = [l.strip() for l in text.splitlines() if l.strip()]
    entries = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # Match range like "01-05", "1-2", or single digit "1", "10"
        m = re.match(r"^(\d+)(?:[-–](\d+))?$", line)
        if m and not (len(line) == 4 and int(line) > 1900): # not a year
            min_r = int(m.group(1))
            max_r = int(m.group(2)) if m.group(2) else min_r
            i += 1
            desc_lines = []
            while i < len(lines):
                next_l = lines[i]
                if re.match(r"^(\d+)(?:[-–](\d+))?$", next_l) and not (len(next_l) == 4 and int(next_l) > 1900):
                    break
                if next_l.startswith("D100") or next_l.startswith("D20") or next_l.startswith("D12") or next_l.startswith("D8") or next_l.startswith("D6") or next_l.startswith("D10"):
                    i += 1
                    continue
                desc_lines.append(next_l)
                i += 1
            desc = " ".join(desc_lines).strip()
            if desc:
                entries.append((min_r, max_r, desc))
        else:
            i += 1
    return entries

def main():
    print(f"Opening {PACK_PATH}...")
    pack = json.loads(PACK_PATH.read_text(encoding="utf-8-sig"))
    
    aids_doc = fitz.open(str(AIDS_PATH))
    book_doc = fitz.open(str(BOOK_PATH))

    new_tables = []
    new_entries = []
    
    # 1. Tabla de Botín / Looting Table (P29 of Aids, D100)
    looting_text = aids_doc[28].get_text()
    raw_looting = parse_range_table(looting_text, "1d100")
    print(f"Extracted Looting items: {len(raw_looting)}")
    
    table_id = -2217
    new_tables.append({
        "id": table_id,
        "name": "Tabla de Botín (Looting)",
        "category": "Exploración",
        "description": "Hallazgos y suministros rescatados al saquear las habitaciones del Coro.",
        "rollFormula": "1d100",
        "rollMode": "RANGE",
        "isPinned": True,
        "sourceType": "STARTER_PACK",
        "sourceName": "Choir of Flesh",
        "isBuiltIn": True,
        "sortOrder": 2217,
        "createdAt": 1776300000000,
        "sourcePack": "Choir of Flesh"
    })
    for min_r, max_r, desc in raw_looting:
        new_entries.append({
            "id": -abs(len(new_entries) + 5000),
            "tableId": table_id,
            "minRoll": min_r,
            "maxRoll": max_r,
            "weight": 1,
            "text": desc,
            "sortOrder": min_r
        })

    # 2. Tabla de Rapiña / Scavenging Table (P28 of Aids, D20)
    scav_text = aids_doc[27].get_text()
    raw_scav = parse_range_table(scav_text, "1d20")
    print(f"Extracted Scavenging items: {len(raw_scav)}")
    
    table_id = -2218
    new_tables.append({
        "id": table_id,
        "name": "Tabla de Rapiña (Scavenging)",
        "category": "Exploración",
        "description": "Búsqueda apresurada de recursos en áreas previamente transitadas.",
        "rollFormula": "1d20",
        "rollMode": "RANGE",
        "isPinned": True,
        "sourceType": "STARTER_PACK",
        "sourceName": "Choir of Flesh",
        "isBuiltIn": True,
        "sortOrder": 2218,
        "createdAt": 1776300000000,
        "sourcePack": "Choir of Flesh"
    })
    for min_r, max_r, desc in raw_scav:
        new_entries.append({
            "id": -abs(len(new_entries) + 5000),
            "tableId": table_id,
            "minRoll": min_r,
            "maxRoll": max_r,
            "weight": 1,
            "text": desc,
            "sortOrder": min_r
        })

    # 3. Eventos de Asentamiento / Settlement Events (P2 of Aids, D100)
    settle_text = aids_doc[1].get_text()
    raw_settle = parse_range_table(settle_text, "1d100")
    print(f"Extracted Settlement Events: {len(raw_settle)}")
    
    table_id = -2219
    new_tables.append({
        "id": table_id,
        "name": "Eventos de Asentamiento (Refugio)",
        "category": "Asentamiento",
        "description": "Sucesos que sacuden el refugio durante la fase de asentamiento.",
        "rollFormula": "1d100",
        "rollMode": "RANGE",
        "isPinned": True,
        "sourceType": "STARTER_PACK",
        "sourceName": "Choir of Flesh",
        "isBuiltIn": True,
        "sortOrder": 2219,
        "createdAt": 1776300000000,
        "sourcePack": "Choir of Flesh"
    })
    for min_r, max_r, desc in raw_settle:
        new_entries.append({
            "id": -abs(len(new_entries) + 5000),
            "tableId": table_id,
            "minRoll": min_r,
            "maxRoll": max_r,
            "weight": 1,
            "text": desc,
            "sortOrder": min_r
        })

    # 4. El Precio del Coro / The Choir's Price (P9 of Aids, D100)
    price_text = aids_doc[8].get_text()
    raw_price = parse_range_table(price_text, "1d100")
    print(f"Extracted Choir Price items: {len(raw_price)}")
    
    table_id = -2220
    new_tables.append({
        "id": table_id,
        "name": "El Precio del Coro",
        "category": "Magia y Fe",
        "description": "Consecuencias sobrenaturales y precio del alma al invocar las oraciones del Coro.",
        "rollFormula": "1d100",
        "rollMode": "RANGE",
        "isPinned": True,
        "sourceType": "STARTER_PACK",
        "sourceName": "Choir of Flesh",
        "isBuiltIn": True,
        "sortOrder": 2220,
        "createdAt": 1776300000000,
        "sourcePack": "Choir of Flesh"
    })
    for min_r, max_r, desc in raw_price:
        new_entries.append({
            "id": -abs(len(new_entries) + 5000),
            "tableId": table_id,
            "minRoll": min_r,
            "maxRoll": max_r,
            "weight": 1,
            "text": desc,
            "sortOrder": min_r
        })

    # 5. Dones de los Deshechos / Gifts of the Unmade (Book pages 70-71, D20)
    unmade_text = book_doc[69].get_text() + "\n" + book_doc[70].get_text()
    raw_unmade = parse_range_table(unmade_text, "1d20")
    # Keep only 1 to 20
    raw_unmade = [item for item in raw_unmade if item[0] <= 20 and item[1] <= 20]
    print(f"Extracted Gifts of the Unmade: {len(raw_unmade)}")
    
    table_id = -2221
    new_tables.append({
        "id": table_id,
        "name": "Dones de los Deshechos",
        "category": "Mutaciones",
        "description": "Mutaciones biológicas de La Carne Que Alimenta concedidas o impuestas.",
        "rollFormula": "1d20",
        "rollMode": "RANGE",
        "isPinned": True,
        "sourceType": "STARTER_PACK",
        "sourceName": "Choir of Flesh",
        "isBuiltIn": True,
        "sortOrder": 2221,
        "createdAt": 1776300000000,
        "sourcePack": "Choir of Flesh"
    })
    for min_r, max_r, desc in raw_unmade:
        new_entries.append({
            "id": -abs(len(new_entries) + 5000),
            "tableId": table_id,
            "minRoll": min_r,
            "maxRoll": max_r,
            "weight": 1,
            "text": desc,
            "sortOrder": min_r
        })

    # 6. Complicaciones (D6) (P12 of Aids)
    comp_text = aids_doc[11].get_text()
    raw_comp = parse_range_table(comp_text, "1d6")
    print(f"Extracted Complications: {len(raw_comp)}")
    
    table_id = -2222
    new_tables.append({
        "id": table_id,
        "name": "Complicaciones de Escena",
        "category": "Oráculo",
        "description": "Giros y complicaciones imprevistas durante la exploración o conflicto.",
        "rollFormula": "1d6",
        "rollMode": "RANGE",
        "isPinned": True,
        "sourceType": "STARTER_PACK",
        "sourceName": "Choir of Flesh",
        "isBuiltIn": True,
        "sortOrder": 2222,
        "createdAt": 1776300000000,
        "sourcePack": "Choir of Flesh"
    })
    for min_r, max_r, desc in raw_comp:
        new_entries.append({
            "id": -abs(len(new_entries) + 5000),
            "tableId": table_id,
            "minRoll": min_r,
            "maxRoll": max_r,
            "weight": 1,
            "text": desc,
            "sortOrder": min_r
        })

    # Merge tables and entries without duplicating
    existing_table_ids = {t["id"] for t in pack.get("randomTables", [])}
    for t in new_tables:
        if t["id"] not in existing_table_ids:
            pack["randomTables"].append(t)
    
    existing_entry_ids = {e["id"] for e in pack.get("tableEntries", [])}
    for e in new_entries:
        if e["id"] not in existing_entry_ids:
            pack["tableEntries"].append(e)

    # Tactical Maps
    tactical_maps = [
        {
            "id": -3001,
            "name": "Cuadrícula de Incursión (8 Nodos)",
            "imageUri": "file:///android_asset/starter_packs/choir_of_flesh/maps/incursion_grid.webp",
            "gridType": "NONE",
            "cellSizePx": 100.0,
            "gridOffsetX": 0.0,
            "gridOffsetY": 0.0,
            "gridColorHex": 1090519039,
            "gridOpacity": 0.0,
            "scaleFtPerCell": 10,
            "diagonalRule": "ALTERNATING_5_10_5",
            "sessionId": None,
            "sceneId": None,
            "fogOfWarMaskJson": "",
            "terrainMaskJson": "",
            "areasJson": "[]",
            "createdAt": 1776300000000
        },
        {
            "id": -3002,
            "name": "Cuadrícula de Combate (5×5)",
            "imageUri": "file:///android_asset/starter_packs/choir_of_flesh/maps/combat_grid.webp",
            "gridType": "SQUARE",
            "cellSizePx": 185.0,
            "gridOffsetX": 132.0,
            "gridOffsetY": 426.0,
            "gridColorHex": 1090519039,
            "gridOpacity": 0.35,
            "scaleFtPerCell": 5,
            "diagonalRule": "ALTERNATING_5_10_5",
            "sessionId": None,
            "sceneId": None,
            "fogOfWarMaskJson": "",
            "terrainMaskJson": "",
            "areasJson": "[]",
            "createdAt": 1776300000000
        },
        {
            "id": -3003,
            "name": "Combate Multijugador (10×5)",
            "imageUri": "file:///android_asset/starter_packs/choir_of_flesh/maps/multiplayer_combat_grid.webp",
            "gridType": "SQUARE",
            "cellSizePx": 185.0,
            "gridOffsetX": 267.0,
            "gridOffsetY": 439.0,
            "gridColorHex": 1090519039,
            "gridOpacity": 0.35,
            "scaleFtPerCell": 5,
            "diagonalRule": "ALTERNATING_5_10_5",
            "sessionId": None,
            "sceneId": None,
            "fogOfWarMaskJson": "",
            "terrainMaskJson": "",
            "areasJson": "[]",
            "createdAt": 1776300000000
        }
    ]

    # Pins for Incursion Grid (-3001)
    # Rooms layout:
    # Row 0: Start(cx=280, cy=198), Room 1(cx=842, cy=198), Room 2(cx=1403, cy=198)
    # Row 1: Room 3(cx=280, cy=595), Room 4(cx=842, cy=595), Room 5(cx=1403, cy=595)
    # Row 2: Room 6(cx=280, cy=992), Room 7(cx=842, cy=992), Room 8(cx=1403, cy=992)
    rooms_coords = [
        ("Punto de Partida", 2.80, 1.98, "flag", 0xFF4CAF50, False, "EXPLORED", "Campamento base y acceso seguro a la incursión.", []),
        ("Habitación 1", 8.42, 1.98, "door", 0xFFFFA000, True, "UNKNOWN", "Primera estancia tras el umbral. Registra peligros y saquea si está asegurada.",
         [{"tableId": -2206, "label": "Encuentro"}, {"tableId": -2207, "label": "Criatura"}, {"tableId": -2217, "label": "Botín"}]),
        ("Habitación 2", 14.03, 1.98, "door", 0xFFFFA000, True, "UNKNOWN", "Cámara exterior con restos del viejo mundo.",
         [{"tableId": -2206, "label": "Encuentro"}, {"tableId": -2207, "label": "Criatura"}, {"tableId": -2217, "label": "Botín"}]),
        ("Habitación 3", 2.80, 5.95, "door", 0xFFFFA000, True, "UNKNOWN", "Flanco occidental de la incursión.",
         [{"tableId": -2206, "label": "Encuentro"}, {"tableId": -2207, "label": "Criatura"}, {"tableId": -2217, "label": "Botín"}]),
        ("Habitación 4", 8.42, 5.95, "door", 0xFFFFA000, True, "UNKNOWN", "Encrucijada central de pasillos y ecos de dolor.",
         [{"tableId": -2206, "label": "Encuentro"}, {"tableId": -2207, "label": "Criatura"}, {"tableId": -2217, "label": "Botín"}]),
        ("Habitación 5", 14.03, 5.95, "door", 0xFFFFA000, True, "UNKNOWN", "Ala oriental con paredes pulsantes de carne.",
         [{"tableId": -2206, "label": "Encuentro"}, {"tableId": -2207, "label": "Criatura"}, {"tableId": -2217, "label": "Botín"}]),
        ("Habitación 6", 2.80, 9.92, "door", 0xFFFFA000, True, "UNKNOWN", "Profundidades del suroeste.",
         [{"tableId": -2206, "label": "Encuentro"}, {"tableId": -2207, "label": "Criatura"}, {"tableId": -2217, "label": "Botín"}]),
        ("Habitación 7", 8.42, 9.92, "door", 0xFFFFA000, True, "UNKNOWN", "Antesala del horror definitivo.",
         [{"tableId": -2206, "label": "Encuentro"}, {"tableId": -2207, "label": "Criatura"}, {"tableId": -2217, "label": "Botín"}]),
        ("Habitación 8 (Núcleo de Incursión)", 14.03, 9.92, "skull", 0xFFF44336, True, "UNKNOWN", "Núcleo de la incursión: abominación mayor, reliquia sagrada o salida final.",
         [{"tableId": -2206, "label": "Encuentro"}, {"tableId": -2207, "label": "Criatura"}, {"tableId": -2217, "label": "Botín"}])
    ]

    tactical_pins = []
    for idx, (title, cx, cy, icon, color, is_sec, exp_state, desc, links) in enumerate(rooms_coords):
        tactical_pins.append({
            "id": -3100 - idx,
            "mapId": -3001,
            "cellX": float(cx),
            "cellY": float(cy),
            "title": title,
            "description": desc,
            "iconName": icon,
            "colorHex": color,
            "isSecret": 1 if is_sec else 0,
            "explorationState": exp_state,
            "targetTableId": links[0]["tableId"] if links else None,
            "targetEncounterId": None,
            "targetWikiEntryId": None,
            "targetMapId": None,
            "discoveriesJson": "[]",
            "tableLinksJson": json.dumps(links, ensure_ascii=False)
        })

    pack["tacticalMaps"] = tactical_maps
    pack["tacticalMapPins"] = tactical_pins

    PACK_PATH.write_text(json.dumps(pack, ensure_ascii=False, indent=4) + "\n", encoding="utf-8")
    print(f"Successfully updated {PACK_PATH}:")
    print(f"  - Random Tables: {len(pack['randomTables'])}")
    print(f"  - Table Entries: {len(pack['tableEntries'])}")
    print(f"  - Tactical Maps: {len(pack['tacticalMaps'])}")
    print(f"  - Tactical Pins: {len(pack['tacticalMapPins'])}")

if __name__ == "__main__":
    main()
