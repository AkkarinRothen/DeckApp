"""Align the Deck of Worlds starter pack with the options visible on its cards.

The original image extraction crossed five deck boundaries. This script keeps
card IDs and image paths stable while correcting their stack and option layout.
It never guesses/transcribes the wording printed on the images.
"""

import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "core/data/src/main/assets/starter_packs/story_engine_deck_of_worlds.json"
METADATA = ROOT / "core/data/src/main/assets/starter_packs/deck_of_worlds/cards_metadata.json"

GROUPS = {
    101: ("region", "Región", "IMAGE_WITH_TEXT", 1),
    102: ("landmark", "Punto de interés", "TOP_BOTTOM_SPLIT", 2),
    103: ("namesake", "Nombre y apelativo", "FOUR_EDGE_CUES", 4),
    104: ("origin", "Origen e historia", "FOUR_EDGE_CUES", 4),
    105: ("attribute", "Atributo y cultura", "FOUR_EDGE_CUES", 4),
    106: ("advent", "Acontecimiento y gancho", "TOP_BOTTOM_SPLIT", 2),
}


def target_for(stem: str) -> int:
    kind, number = stem.rsplit("_", 1)
    index = int(number)
    if kind == "region" or (kind == "landmark" and index <= 2):
        return 101
    if kind == "landmark" or (kind == "namesake" and index <= 2):
        return 102
    if kind == "namesake" or (kind == "origin" and index <= 2):
        return 103
    if kind == "origin" and index <= 34:
        return 104
    if kind == "attribute" or (kind == "origin" and index >= 35) or (kind == "advent" and index <= 4):
        return 105
    if kind == "advent":
        return 106
    raise ValueError(stem)


def main() -> None:
    pack = json.loads(PACK.read_text(encoding="utf-8"))
    metadata = json.loads(METADATA.read_text(encoding="utf-8"))
    faces_by_card = {face["cardId"]: face for face in pack["cardFaces"]}
    counts = Counter()
    ordered_cards = {deck_id: [] for deck_id in GROUPS}
    ordered_metadata = {prefix: [] for prefix, _, _, _ in GROUPS.values()}
    metadata_by_stem = {
        Path(entry["filename"]).stem: entry
        for entries in metadata["cards"].values()
        for entry in entries
    }

    for card in pack["cards"]:
        face = faces_by_card[card["id"]]
        stem = Path(face["imagePath"]).stem
        deck_id = target_for(stem)
        prefix, suit, mode, option_count = GROUPS[deck_id]
        card["stackId"] = deck_id
        card["originDeckId"] = deck_id
        card["suit"] = suit
        face["contentMode"] = mode
        face["zonesJson"] = json.dumps(
            [{"text": ""} for _ in range(option_count)], ensure_ascii=False, separators=(",", ":")
        )
        ordered_cards[deck_id].append(card)
        counts[deck_id] += 1

        ordered_metadata[prefix].append({
            **metadata_by_stem[stem],
            "option_count": option_count,
            "content_mode": mode,
        })

    assert len(faces_by_card) == len(pack["cards"]) == 240
    assert dict(counts) == {101: 32, 102: 48, 103: 48, 104: 32, 105: 48, 106: 32}

    for deck in pack["decks"]:
        deck_id = deck["id"]
        if deck_id in GROUPS:
            deck["defaultContentMode"] = GROUPS[deck_id][2]
            for order, card in enumerate(ordered_cards[deck_id]):
                card["sortOrder"] = order
                card["title"] = f"{GROUPS[deck_id][1]} #{order + 1:02d}"
                faces_by_card[card["id"]]["name"] = card["title"]

    for deck_id, (prefix, _, _, _) in GROUPS.items():
        metadata["card_counts"][prefix] = counts[deck_id]
        metadata["cards"][prefix] = ordered_metadata[prefix]

    PACK.write_text(json.dumps(pack, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    METADATA.write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(dict(counts))


if __name__ == "__main__":
    main()
