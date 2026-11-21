"""Validate C28 additions against finalized C27, the native substrates and provider art."""
import hashlib
import json
import subprocess
import sys
from contextlib import ExitStack
from pathlib import Path
from zipfile import ZipFile

from verify_c26 import compare_family_additions


if __name__ == "__main__":
    if not __debug__:
        sys.exit("Verification requires assertions enabled")
    current, *references, c27 = map(Path, sys.argv[1:])
    assert hashlib.sha256(c27.read_bytes()).hexdigest() == "4eaaebe4de7bb9927c474dff1821863a7bfae931610c11787fac7926fd4e10ce"
    subprocess.run([sys.executable, "-B", Path(__file__).with_name("verify_c27.py"), c27, *references], check=True)
    families = (
        ("calcite", "minecraft", "calcite", "calcite", "calcite",
         "bf4d6a2d7e044fb5e17ba86b86e24df4f031e75df600bc0fa6aabb5377644cb9"),
        ("shadoline_block", "enderscape", "shadoline_block", "shadoline_block_slab", "shadoline_block",
         "27395c8f856c49a3ccf28e1480778a97187098eeb4d7724e81fcf67e2975178a"),
    )
    with ExitStack() as stack:
        jar, previous, client, end, bbb, paths = [stack.enter_context(ZipFile(path)) for path in
                (current, c27, references[1], references[6], references[2], references[3])]
        for material, ns, base, slab, sprite, digest in families:
            provider = client if ns == "minecraft" else end
            model = json.loads(provider.read(f"assets/{ns}/models/block/{base}.json"))
            assert model == {"parent": "minecraft:block/cube_all", "textures": {"all": f"{ns}:block/{sprite}"}}
        slab = json.loads(end.read("assets/enderscape/models/block/shadoline_block_slab.json"))
        assert slab["textures"] == dict.fromkeys(("top", "side", "bottom"), "enderscape:block/shadoline_block")
        compare_family_additions(jar, previous, client, end, bbb, paths, families, 28, 4769)
