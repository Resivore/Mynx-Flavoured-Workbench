"""Layer the Matcha-rendered timber surfaces over the seven live timber sprite IDs."""
from copy import copy
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
import sys
from zipfile import ZipFile

from PIL import Image
from build_foundation_v11 import COMMON, MATCHA, ROOT

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "projects/architectural-material-closure/tools"))
from mossy_oak import RIBBITS_SHA, moss_sheet


def checked(path, digest):
    if sha256(path.read_bytes()).hexdigest() != digest:
        raise ValueError(f"Unexpected source archive {path}")
    return ZipFile(path)


def tile(archive, path):
    return Image.open(BytesIO(archive.read(path))).convert("RGBA")


def build(output):
    client = Path.home()/".gradle/caches/fabric-loom/26.2/minecraft-client.jar"
    rib = COMMON/"projects/ribbits-26.2/artifacts/ribbits-private-reconstruction-4.1.6+26.2-mynx-canary27.jar"
    with (checked(COMMON/"resourcepacks/foundation/artifacts/Foundation v11.zip",
                  "ac31fab940edc2caca1dd9c74bf6158861c267bece7d052a904639d7c9115f36") as base,
          checked(COMMON/MATCHA,"6209783021c358044abedabacee471faff5bd4080437d4e3b5e51963f1804248") as matcha,
          checked(rib, RIBBITS_SHA) as ribbits, ZipFile(client) as mc):
        prefix = "assets/minecraft/textures/block/"
        oak = tile(mc, prefix+"oak_planks.png")
        moss = tile(ribbits,"assets/ribbits/textures/block/mossy_oak_planks.png")
        additions = {}
        for name in ("oak_log","oak_log_top","stripped_oak_log","stripped_oak_log_top"):
            src = prefix + name + ".png"
            # Matcha 1.12 inherits only the stripped side from the client.
            assert (src in matcha.NameToInfo) == (name != "stripped_oak_log")
            target = tile(matcha if src in matcha.NameToInfo else mc, src)
            out = BytesIO()
            moss_sheet(target,oak,moss,()).save(out,format="PNG")
            owners = ["mynx_trees"]
            if name != "stripped_oak_log_top":
                owners.append("architectural_material_closure")
            for ns in owners:
                path = f"assets/{ns}/textures/block/" + name.replace("oak","mossy_oak") + ".png"
                additions[path] = out.getvalue()
        assert not additions.keys() & base.NameToInfo.keys()
        metadata = json.loads(base.read("pack.mcmeta"))
        metadata["pack"]["description"] = "Foundation v12"
        with ZipFile(output,"w") as archive:
            for entry in base.infolist():
                data = (json.dumps(metadata,indent=2)+"\n").encode() if entry.filename=="pack.mcmeta" else base.read(entry)
                archive.writestr(copy(entry),data)
            for path,data in sorted(additions.items()):
                entry = copy(base.getinfo("assets/minecraft/textures/block/birch_planks.png"))
                entry.filename = path
                archive.writestr(entry,data)
        with ZipFile(output) as final:
            assert set(final.namelist()) == set(base.namelist()) | additions.keys()
            assert all(final.read(n) == base.read(n) for n in base.namelist() if n!="pack.mcmeta")
            assert final.testzip() is None


if __name__ == "__main__":
    build(ROOT/"artifacts/Foundation v12.zip")
