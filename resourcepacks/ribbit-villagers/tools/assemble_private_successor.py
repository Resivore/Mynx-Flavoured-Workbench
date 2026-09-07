"""Assemble the private Florist successor without tracking protected Ribbit Villagers artwork."""
from __future__ import annotations

import argparse
import hashlib
import pathlib
import zipfile

SOURCE_PROPERTIES = "assets/minecraft/optifine/cem/villager.properties"
GARDENER_TEXTURE = "assets/minecraft/textures/entity/villager/profession/gardener.png"
FLORIST_TEXTURE = "assets/mynx_flora_trades/textures/entity/villager/profession/florist.png"
VARIANTS = "RIBBIT_VILLAGERS_VARIANTS.txt"
REQUIRED = {
    "RIBBITS_ASSET_NOTICE.txt", VARIANTS, SOURCE_PROPERTIES,
    "assets/minecraft/optifine/cem/villager6.jem",
    "assets/minecraft/optifine/cem/ribbit_hats/gardener.png", GARDENER_TEXTURE,
}


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=pathlib.Path)
    parser.add_argument("output", type=pathlib.Path)
    args = parser.parse_args()
    with zipfile.ZipFile(args.source) as source:
        names = [entry.filename for entry in source.infolist() if not entry.is_dir()]
        if len(names) != len(set(names)) or not REQUIRED.issubset(names):
            raise SystemExit("source is not the verified Ribbit Villagers pack")
        original = {name: source.read(name) for name in names}

    properties = original[SOURCE_PROPERTIES].decode("utf-8")
    if "models.5=6" in properties or "professions.5=" in properties:
        raise SystemExit("source already routes the reserved Gardener model")
    properties += "\n# Mynx Flora Trades — reserved Gardener assets activated as Florist\nmodels.5=6\nprofessions.5=mynx_flora_trades:florist\n"
    variants = original[VARIANTS].decode("utf-8")
    old = """Reserved / inactive:\n  villager6.jem = Ribbits Gardener hat\n  textures/entity/villager/profession/gardener.png = copy of the current farmer outfit\n\nThe gardener model is deliberately not referenced by villager.properties yet.\nWhen a future gardener profession/type has an actual routable identifier, route model 6 to that identifier rather than assigning it to an existing vanilla profession.\n"""
    new = """Active Florist integration:\n  villager6.jem = Ribbits Gardener hat -> mynx_flora_trades:florist\n  assets/mynx_flora_trades/textures/entity/villager/profession/florist.png = exact copy of the reserved gardener/farmer outfit\n\nModel 6 is activated only for the namespaced Mynx Flora Trades Florist profession.\n"""
    if old not in variants:
        raise SystemExit("source variants document does not contain the verified reserved-model block")
    variants = variants.replace(old, new)

    changed = {SOURCE_PROPERTIES: properties.encode("utf-8"), VARIANTS: variants.encode("utf-8"),
               FLORIST_TEXTURE: original[GARDENER_TEXTURE]}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for name in names:
            output.writestr(name, changed.get(name, original[name]))
        output.writestr(FLORIST_TEXTURE, changed[FLORIST_TEXTURE])

    with zipfile.ZipFile(args.output) as output:
        out_names = [entry.filename for entry in output.infolist() if not entry.is_dir()]
        if len(out_names) != len(set(out_names)) or set(out_names) != set(names) | {FLORIST_TEXTURE}:
            raise SystemExit("successor archive has duplicate or unexpected paths")
        for name in names:
            if name not in changed and output.read(name) != original[name]:
                raise SystemExit(f"unrelated protected entry changed: {name}")
        if output.read(FLORIST_TEXTURE) != original[GARDENER_TEXTURE]:
            raise SystemExit("Florist outfit is not the exact reserved source bytes")
    print(f"{args.output.name} {sha256(args.output.read_bytes())}")


if __name__ == "__main__":
    main()
