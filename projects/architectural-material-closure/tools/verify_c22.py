"""Regression checks against the exact retained C22 archive."""
import hashlib
import json
from zipfile import ZipFile

from verify_masonry import NS


def compare_c22(current, previous_path):
    assert hashlib.sha256(previous_path.read_bytes()).hexdigest() == "ced8e122ac4bad974e41dc93dfcad3c643940e77cf0ae83dedf93ebfd33a391e"
    with ZipFile(previous_path) as previous:
        old = {e.filename for e in previous.infolist() if not e.is_dir()}
        new = {e.filename for e in current.infolist() if not e.is_dir()}
        assert len(old) == 1881 and old <= new
        added = new - old
        ids = {f.split("/")[-1][:-5] for f in added if f.startswith(f"assets/{NS}/blockstates/")}
        items = {f.split("/")[-1][:-5] for f in added if f.startswith(f"assets/{NS}/items/")}
        assert ids == items and len(ids) == 120
        for material in ("tuff", "cinnabar", "sulfur", "smooth_basalt"):
            assert sum(name.startswith(material + "_") for name in ids) == 30
            sheets = [current.read(f"assets/{NS}/textures/block/{material}_{pattern}.png")
                      for pattern in ("running_bond", "flagstone", "windmill_weave", "crystal_floor")]
            assert len(set(sheets)) == 4
        changed = [f for f in old if previous.read(f) != current.read(f)]
        for f in changed:
            before, after = (json.loads(z.read(f)) for z in (previous, current))
            if f == "fabric.mod.json":
                assert before.pop("version") == "0.1.0-canary22" and after.pop("version") == "0.1.0-canary23"
                assert after["entrypoints"]["main"].pop() == "dev.resivore.amc.BaseStoneFamilies"
                assert after == before
            elif f == f"assets/{NS}/lang/en_us.json":
                assert before.items() <= after.items()
                assert after.keys() - before.keys() == {"block." + NS + "." + name for name in ids}
            else:
                assert "/tags/" in f and set(before) == set(after) == {"replace", "values"}, f
                assert before["replace"] == after["replace"] == False
                assert after["values"][:len(before["values"])] == before["values"]
                assert len(after["values"]) == len(set(after["values"]))
                assert set(after["values"][len(before["values"]):]) <= {NS + ":" + name for name in ids}
        print(f"C22 comparison: {len(old)-len(changed)} byte-identical, {len(changed)} additive metadata entries, "
              f"{len(added)} new entries and {len(ids)} new pairs.")
