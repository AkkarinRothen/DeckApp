"""Extract all 30 stat blocks verbatim from the local Chapter V export.

Usage: python scripts/integrate-choir-bestiary.py PATH_TO_CHAPTER_V_MD
Original files are read only. No statistics are converted to numeric HP/AC.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "core/data/src/main/assets/starter_packs"


def integrate(chapter):
    text = chapter.read_text(encoding="utf-8")
    pack_path = ASSETS / "choir_of_flesh.json"
    pack = json.loads(pack_path.read_text(encoding="utf-8-sig"))
    folder = ASSETS / "choir_of_flesh"
    legacy = folder / "legacy-bestiary.json"
    if not legacy.exists():
        legacy.write_text(json.dumps({"npcs": pack["npcs"]}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    old = {n["name"]: n for n in pack["npcs"]}
    combat = next(r for r in pack["systemRules"] if r["title"] == "Combate, Iniciativa y Táctica")
    legacy_data = json.loads(legacy.read_text(encoding="utf-8"))
    if not legacy_data.get("systemRules"):
        legacy_data["systemRules"] = [dict(combat)]
        legacy.write_text(json.dumps(legacy_data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    combat["content"] = """Reglas cotejadas con los capítulos III y V del manual.

- **Iniciativa:** los PJs tiran 1d20 + modificador de Iniciativa cada ronda. Los PNJs usan la iniciativa fija de su ficha. Se actúa de mayor a menor; revisa excepciones de rasgos como Astucia Odiosa.
- **Acciones:** acción gratuita, movimiento, reacción y acción estándar. Las reacciones ocurren durante el turno de otra criatura.
- **PNJs:** no hacen chequeos. Los PJs atacan contra el grado dinámico de CD, o se defienden contra el grado de Ataque. Precisión se aplica como Carga a la defensa del PJ.
- **NO dinámico:** usa la tabla del capítulo III con el modificador habitual del PJ. Maestría y Carga se aplican después. Por ejemplo, Promedio con modificador +0 da NO 11; con +2 da NO 13.
- **Heridas:** mayores y menores se registran por separado. Una mayor requiere consultar la tabla de efectos del tipo de daño si la criatura sobrevive. Una menor aumenta Desgaste; los atacantes de un PNJ desgastado reciben Maestría. Respeta inmunidades y excepciones de cada ficha.
- **Decisión enemiga d10:** sin mayores, 1–6 estándar y 7–10 especial; con una mayor, 1–5 estándar, 6–8 especial, 9–10 apoyo; con dos o más, 1–4 estándar, 5–6 especial, 7–8 apoyo, 9–10 huir. Si falta especial usa estándar; si falta apoyo usa especial.
- **Objetivo d6:** 1–2 más herido, 3–4 más cercano, 5–6 aleatorio. Mantén el objetivo hasta que caiga, no pueda atacarse o una acción/razón lógica lo cambie. Si los criterios no resuelven un objetivo, aleatoriza.
- **Número ♟:** multiplica por los personajes del grupo del jugador, incluidos compañeros PNJ. La XP y el botín indicados son por individuo.
- **Dificultad d10:** 1–3 Fácil, 4–8 Promedio, 9–10 Difícil. En solitario ajusta grados, Precisión, XP y tabla de heridas según capítulo V. Si Número es mayor que uno, quita/añade un individuo en Fácil/Difícil en lugar de modificar estadísticas.

Los controles registran heridas y consultan decisiones; los efectos particulares, duración, inmunidades y excepciones se aplican conforme a la ficha."""
    headings = list(re.finditer(r"(?m)^#{1,6} \*\*(.+?)\*\*\s*$", text))
    assert len(headings) == 30
    aliases = {"Graznador": "Croador", "Escriba de Penitencia": "Escriba de la Penitencia"}
    npcs = []
    for i, heading in enumerate(headings):
        name = aliases.get(heading[1], heading[1])
        block = text[heading.end():headings[i+1].start() if i+1 < len(headings) else len(text)]
        block = re.sub(r"!\[.*?\]\(.*?\)", "", block).strip()
        rows = [r for r in block.splitlines() if r.startswith("|")]
        assert len(rows) == 8, name
        cells = lambda row: row.strip("|").split("|")
        stats = dict(zip(cells(rows[0]), cells(rows[2])))
        stats.update(zip(cells(rows[3]), cells(rows[4])))
        stats.update(zip(cells(rows[5]), cells(rows[6])))
        description, details = block.split(rows[0], 1)
        actions = re.findall(r"(?m)^\*\*(Ataque Estándar|Ataque Especial|Acción de Apoyo):\*\* (.+)$", block)
        assert len(actions) == 3, name
        image = re.findall(r"!\[.*?\]\((.*?)\)", text[:heading.start()])[-1]
        page = int(re.search(r"_page_(\d+)", image)[1]) + 168
        images = list((folder / "art").glob(f"*__page_{page}_Picture_1.webp"))
        assert len(images) == 1, name
        notes = "<!-- choir-of-flesh:v2 -->\n### Estadísticas de Choir of Flesh\n"
        notes += "\n".join(f"- **{key}**: {value}" for key, value in stats.items())
        notes += "\n\n" + rows[7] + "\n\n" + block.split(rows[7], 1)[1].strip()
        notes += f"\n\nFuente: manual, capítulo V, página {page + 1}. Los grados son números objetivo dinámicos; no son CA ni PG."
        npc = dict(old.get(name, {"id": -2400-i}))
        npc.update(name=name, description=description.strip(), notes=notes,
                   imagePath="file:///android_asset/starter_packs/choir_of_flesh/art/" + images[0].name,
                   isMonster=True, maxHp=1, currentHp=1, armorClass=0, initiativeBonus=0)
        npcs.append(npc)
    pack["npcs"] = npcs
    pack_path.write_text(json.dumps(pack, ensure_ascii=False, indent=4) + "\n", encoding="utf-8")
    (folder / "bestiary.json").write_text(json.dumps({"npcs": npcs, "systemRules": [combat]}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    artwork_path = folder / "artwork.json"
    artwork = json.loads(artwork_path.read_text(encoding="utf-8"))
    artwork["npcs"] = [{"name": n["name"], "imagePath": n["imagePath"]} for n in npcs]
    artwork_path.write_text(json.dumps(artwork, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Verified 30 profiles, 90 actions and 30 existing WebP portraits.")


if __name__ == "__main__":
    integrate(Path(sys.argv[1]))
