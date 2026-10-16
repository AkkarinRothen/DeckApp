"""Convert the supplied book artwork to WebP and update the bundled starter pack.

Usage: python scripts/integrate-choir-artwork.py SOURCE_DIRECTORY
Requires Pillow. Source files are read only; output goes to Android assets.
"""
import hashlib
import json
import re
import sys
import unicodedata
from pathlib import Path

from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "core/data/src/main/assets"
PACK = ASSETS / "starter_packs/choir_of_flesh.json"
ARTWORK = ASSETS / "starter_packs/choir_of_flesh/artwork.json"
URI = "file:///android_asset/"


def normalized(value):
    return "".join(c for c in unicodedata.normalize("NFD", value.lower())
                   if unicodedata.category(c) != "Mn").replace("*", "").strip()


def integrate(source):
    chapter = source.parent / "A Choir of Flesh/Manual/Chapter V/Ch V.md"
    chapter_text = chapter.read_text(encoding="utf-8")
    by_name, by_page = {}, {}
    for heading in re.finditer(r"(?m)^#{1,6} \*\*(.+?)\*\*\s*$", chapter_text):
        images = re.findall(r"!\[.*?\]\((.*?)\)", chapter_text[:heading.start()])
        if not images:
            continue
        local = chapter.parent / images[-1]
        page = int(re.search(r"_page_(\d+)", local.name)[1]) + 168
        candidates = list(source.glob(f"*__page_{page}_Picture_*.jpeg"))
        matches = [p for p in candidates if hashlib.sha256(p.read_bytes()).digest() ==
                   hashlib.sha256(local.read_bytes()).digest()]
        if len(matches) != 1:
            raise ValueError(f"Cannot verify original image for {heading[1]}")
        by_name[normalized(heading[1])] = matches[0]
        by_page[page] = heading[1]

    pack = json.loads(PACK.read_text(encoding="utf-8-sig"))
    aliases = {"croador": "graznador", "escriba de la penitencia": "escriba de penitencia"}
    npc_art = []
    for npc in pack["npcs"]:
        key = normalized(npc["name"])
        image = by_name[aliases.get(key, key)]
        target = ARTWORK.parent / "art" / (image.stem + ".webp")
        npc_art.append({"name": npc["name"], "imagePath": URI + target.relative_to(ASSETS).as_posix()})

    files = sorted(source.glob("*.jpeg"), key=lambda p: (int(re.search(r"page_(\d+)", p.name)[1]), p.name))
    sections = [(8, "Portada y mundo"), (38, "Creación de personajes"),
                (92, "Viajes y exploración"), (167, "Incursiones"),
                (204, "Bestiario"), (999, "Equipo y reliquias")]
    grouped = {name: [] for _, name in sections}
    illustrations = []
    for original in files:
        page = int(re.search(r"page_(\d+)", original.name)[1])
        target = ARTWORK.parent / "art" / (original.stem + ".webp")
        target.parent.mkdir(parents=True, exist_ok=True)
        with Image.open(original) as raw:
            image = ImageOps.exif_transpose(raw).convert("RGB")
            image.thumbnail((1600, 1600), Image.Resampling.LANCZOS)
            image.save(target, "WEBP", quality=90, method=6)
        with Image.open(target) as result:
            assert result.format == "WEBP"
            dimensions = list(result.size)
        uri = URI + target.relative_to(ASSETS).as_posix()
        label = by_page.get(page, "Ilustración del manual")
        section = next(name for limit, name in sections if page <= limit)
        grouped[section].append(f"### {label} · página {page + 1}\n\n![{label}, página {page + 1}]({uri})")
        illustrations.append({"source": original.name, "bookPage": page + 1, "imagePath": uri,
                              "sha256": hashlib.sha256(original.read_bytes()).hexdigest(),
                              "dimensions": dimensions, "bytes": target.stat().st_size})

    galleries = []
    for section, entries in grouped.items():
        for offset in range(0, len(entries), 10):
            number = offset // 10 + 1
            galleries.append({"id": -4000 - len(galleries), "title": f"Galería · {section} · {number}",
                              "content": "Ilustraciones originales del libro, en formato WebP.\n\n" +
                                         "\n\n".join(entries[offset:offset + 10]),
                              "gameSystem": "Choir of Flesh", "category": "Ilustraciones del manual",
                              "sortOrder": 1000 + len(galleries), "sourcePack": "Choir of Flesh"})
    artwork = {"schemaVersion": 1, "npcs": npc_art, "systemRules": galleries, "illustrations": illustrations}
    ARTWORK.write_text(json.dumps(artwork, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    portraits = {npc["name"]: npc["imagePath"] for npc in npc_art}
    for npc in pack["npcs"]:
        npc["imagePath"] = portraits[npc["name"]]
    pack["systemRules"] = [r for r in pack["systemRules"] if r.get("category") != "Ilustraciones del manual"] + galleries
    PACK.write_text(json.dumps(pack, ensure_ascii=False, indent=4) + "\n", encoding="utf-8")
    for item in npc_art:
        assert (ASSETS / item["imagePath"].removeprefix(URI)).is_file()
    print(f"Integrated {len(illustrations)} WebP images, {len(npc_art)} portraits, {len(galleries)} galleries.", flush=True)


if __name__ == "__main__":
    integrate(Path(sys.argv[1]))
