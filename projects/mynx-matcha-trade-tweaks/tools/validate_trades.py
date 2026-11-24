"""Validate the generated trade matrix and optionally check its external item provider."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import subprocess
import sys
import zipfile


def verify_dependency_snapshot(snapshot: Path, errors: list[str]) -> None:
    """Prove non-vanilla result IDs against a local, read-only dependency ZIP.

    The snapshot is optional because it is untracked. Hashes identify validation
    inputs only; they are not runtime version requirements.
    """
    expected = {
        "mods/SimpleCopperPipes-mc26.2-2.1.7.jar": (
            "222b7e4116d52784c1f495b8ff86ff4849fcbef55cf9ddd6bf87ffe4c4c30484",
            {
                "simple_copper_pipes:waxed_copper_pipe": "data/simple_copper_pipes/recipe/waxed_copper_pipe_from_honeycomb.json",
                "simple_copper_pipes:waxed_copper_fitting": "data/simple_copper_pipes/recipe/waxed_copper_fitting_from_honeycomb.json",
            },
        ),
    }
    with zipfile.ZipFile(snapshot) as archive:
        for jar_name, (expected_hash, recipes) in expected.items():
            try:
                jar_bytes = archive.read(jar_name)
            except KeyError:
                errors.append(f"dependency snapshot lacks {jar_name}")
                continue
            actual_hash = hashlib.sha256(jar_bytes).hexdigest()
            if actual_hash != expected_hash:
                errors.append(f"dependency snapshot hash changed for {jar_name}: {actual_hash}")
            with zipfile.ZipFile(io.BytesIO(jar_bytes)) as jar:
                for item_id, recipe_path in recipes.items():
                    namespace, path = item_id.split(":", 1)
                    asset_path = f"assets/{namespace}/items/{path}.json"
                    if asset_path not in jar.namelist():
                        errors.append(f"dependency item asset absent: {item_id}")
                    try:
                        recipe = json.loads(jar.read(recipe_path))
                    except (KeyError, ValueError) as exc:
                        errors.append(f"dependency item recipe invalid for {item_id}: {exc}")
                        continue
                    if recipe.get("result", {}).get("id") != item_id:
                        errors.append(f"dependency recipe does not produce {item_id}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write-report", action="store_true")
    parser.add_argument("--dependency-zip", type=Path)
    args = parser.parse_args()
    generator = Path(__file__).resolve().parents[1] / "generate_overlay.py"
    subprocess.run([sys.executable, "-B", str(generator), *(["--write"] if args.write_report else [])], check=True)
    if args.dependency_zip:
        errors = []
        verify_dependency_snapshot(args.dependency_zip, errors)
        if errors:
            raise SystemExit("\n".join(errors))
