#!/usr/bin/env python3
"""Reproduce the audit's narrow class and Ribbit-model structure checks.

The probe prints identities and aggregate bone facts only. It never extracts or
writes an input archive and deliberately omits model coordinates and textures.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import struct
import sys
import zipfile
from collections import Counter
from pathlib import Path


EXPECTED_C3_SHA256 = "4f34d743f5fffd8e938c8f5157c630fd85f3b263ac1ae9f96c432cfe51668df2"
EXPECTED_RIBBITS_C9_SHA256 = "e433cd048bc362edae91e2e057c8170d92d110cbe7b9917c105c7336be6543de"
EXPECTED_GECKOLIB_SHA256 = "4bf1c86b4b47aa2c5d84208255695f10d79d609b23d802c995711e64b45cfce0"
EXPECTED_MODEL_MANIFEST_SHA256 = "0d41371da10e5328a803ed960914de54b6bf7f28e19133e2b0bc5305cfa0060c"
DISTINCT_WANDERING_MODEL = "assets/ribbits/geckolib/models/wandering_ribbit.geo.json"


class ProbeError(RuntimeError):
    pass


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ProbeError(message)


def u1(data: bytes, offset: int) -> tuple[int, int]:
    return data[offset], offset + 1


def u2(data: bytes, offset: int) -> tuple[int, int]:
    return struct.unpack_from(">H", data, offset)[0], offset + 2


def u4(data: bytes, offset: int) -> tuple[int, int]:
    return struct.unpack_from(">I", data, offset)[0], offset + 4


def class_super_name(class_bytes: bytes) -> str:
    """Return the direct superclass from one Java class file."""

    magic, offset = u4(class_bytes, 0)
    require(magic == 0xCAFEBABE, "invalid Java class magic")
    offset += 4  # minor and major versions
    cp_count, offset = u2(class_bytes, offset)
    pool: list[object | None] = [None] * cp_count
    index = 1
    while index < cp_count:
        tag, offset = u1(class_bytes, offset)
        if tag == 1:  # UTF-8
            length, offset = u2(class_bytes, offset)
            pool[index] = ("utf8", class_bytes[offset : offset + length].decode("utf-8"))
            offset += length
        elif tag in {3, 4}:  # integer / float
            offset += 4
        elif tag in {5, 6}:  # long / double; two constant-pool slots
            offset += 8
            index += 1
        elif tag == 7:  # class
            name_index, offset = u2(class_bytes, offset)
            pool[index] = ("class", name_index)
        elif tag in {8, 16, 19, 20}:  # string, method type, module, package
            offset += 2
        elif tag in {9, 10, 11, 12, 17, 18}:  # two-u2 constants
            offset += 4
        elif tag == 15:  # method handle
            offset += 3
        else:
            raise ProbeError(f"unsupported class constant-pool tag {tag}")
        index += 1

    offset += 2  # access flags
    _, offset = u2(class_bytes, offset)  # this class
    super_index, _ = u2(class_bytes, offset)
    require(super_index != 0, "class unexpectedly has no direct superclass")
    class_entry = pool[super_index]
    require(isinstance(class_entry, tuple) and class_entry[0] == "class", "bad superclass entry")
    utf8_entry = pool[class_entry[1]]
    require(isinstance(utf8_entry, tuple) and utf8_entry[0] == "utf8", "bad superclass name")
    return str(utf8_entry[1])


def archive_class_super(path: Path, entry: str) -> str:
    with zipfile.ZipFile(path) as archive:
        try:
            class_bytes = archive.read(entry)
        except KeyError as exc:
            raise ProbeError(f"{path.name} lacks {entry}") from exc
    return class_super_name(class_bytes)


def check_source_contracts(project: Path) -> dict[str, str]:
    ribbits = project.parent / "ribbits-26.2"
    renderer = (
        ribbits
        / "common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/render/RibbitRenderer.java"
    ).read_text(encoding="utf-8")
    model = (
        ribbits
        / "common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/model/RibbitModel.java"
    ).read_text(encoding="utf-8")
    registration = (
        ribbits
        / "fabric/src/main/java/com/yungnickyoung/minecraft/ribbits/fabric/client/RibbitsFabricClient.java"
    ).read_text(encoding="utf-8")

    require("extends GeoEntityRenderer<RibbitEntity, R>" in renderer, "RibbitRenderer contract drift")
    require("extends GeoModel<RibbitEntity>" in model, "RibbitModel contract drift")
    require("RibbitRenderer::new" in registration, "Ribbit renderer registration drift")
    require("addRenderData" in renderer and "DT_RIBBIT_DATA" in renderer, "render-data contract drift")
    require("getModelResource(GeoRenderState" in model, "dynamic model contract drift")
    require("getTextureResource(GeoRenderState" in model, "texture contract drift")
    return {
        "renderer": "GeoEntityRenderer",
        "model": "GeoModel",
        "registration": "RibbitRenderer::new",
    }


def check_c3(project: Path) -> dict[str, object]:
    artifact = project.parent / "xaero-entity-icons/artifacts/xaero-emf-entity-icon-compat-0.1.0-canary3.jar"
    digest = sha256(artifact)
    require(digest == EXPECTED_C3_SHA256, "historical C3 artifact hash drift")
    with zipfile.ZipFile(artifact) as archive:
        class_blob = b"".join(
            archive.read(name) for name in archive.namelist() if name.endswith(".class")
        )
    require(b"EMFModelPartRoot" in class_blob, "C3 exact EMF root contract missing")
    require(b"net/minecraft/client/model/geom/ModelPart" in class_blob, "C3 ModelPart contract missing")
    forbidden = [b"geckolib", b"GeoEntityRenderer", b"EntityRenderTracer", b"RadarIconCreator"]
    require(not any(token.lower() in class_blob.lower() for token in forbidden), "C3 scope unexpectedly widened")
    return {
        "filename": artifact.name,
        "bytes": artifact.stat().st_size,
        "sha256": digest,
        "emf_model_part_contract": True,
        "geckolib_or_early_xaero_hook_reference": False,
    }


def check_ribbits_archive(path: Path) -> dict[str, object]:
    digest = sha256(path)
    require(digest == EXPECTED_RIBBITS_C9_SHA256, "Ribbits C9 artifact hash drift")
    renderer_entry = "com/yungnickyoung/minecraft/ribbits/client/render/RibbitRenderer.class"
    model_entry = "com/yungnickyoung/minecraft/ribbits/client/model/RibbitModel.class"
    renderer_super = archive_class_super(path, renderer_entry)
    model_super = archive_class_super(path, model_entry)
    require(renderer_super == "com/geckolib/renderer/GeoEntityRenderer", "unexpected RibbitRenderer superclass")
    require(model_super == "com/geckolib/model/GeoModel", "unexpected RibbitModel superclass")

    cube_counts: Counter[int] = Counter()
    head_count = 0
    model_count = 0
    manifest = hashlib.sha256()
    with zipfile.ZipFile(path) as archive:
        all_geo_entries = sorted(
            name
            for name in archive.namelist()
            if name.startswith("assets/ribbits/") and name.endswith(".geo.json")
        )
        require(len(all_geo_entries) == 42, "expected 42 total Ribbits Geo model entries")
        require(DISTINCT_WANDERING_MODEL in all_geo_entries,
                "distinct Wandering Ribbit model identity is missing")
        entries = [name for name in all_geo_entries if name != DISTINCT_WANDERING_MODEL]
        for entry in entries:
            entry_bytes = archive.read(entry)
            manifest.update(f"{entry}\t{hashlib.sha256(entry_bytes).hexdigest()}\n".encode("utf-8"))
            document = json.loads(entry_bytes)
            geometries = document.get("minecraft:geometry")
            require(isinstance(geometries, list) and geometries, f"{entry}: missing geometry list")
            for geometry in geometries:
                bones = geometry.get("bones")
                require(isinstance(bones, list), f"{entry}: missing bones")
                named = {bone.get("name"): bone for bone in bones if isinstance(bone.get("name"), str)}
                require("main" in named, f"{entry}: missing main bone")
                require("body" in named and named["body"].get("parent") == "main", f"{entry}: body hierarchy drift")
                head_count += sum(1 for name in named if name.casefold() == "head")
                cubes = named["body"].get("cubes", [])
                require(isinstance(cubes, list), f"{entry}: body cubes are not a list")
                cube_counts[len(cubes)] += 1
                model_count += 1

    require(model_count == 41, f"expected 41 model entries, found {model_count}")
    require(head_count == 0, "a canonical head bone now exists; audit conclusion needs review")
    require(cube_counts == Counter({3: 29, 4: 8, 9: 3, 10: 1}), "direct body cube distribution drift")
    require(manifest.hexdigest() == EXPECTED_MODEL_MANIFEST_SHA256, "model-entry manifest drift")
    return {
        "filename": path.name,
        "bytes": path.stat().st_size,
        "sha256": digest,
        "renderer_superclass": renderer_super,
        "model_superclass": model_super,
        "model_entries": model_count,
        "model_entry_manifest_sha256": manifest.hexdigest(),
        "all_have_main_to_body": True,
        "canonical_head_bones": head_count,
        "direct_body_cube_distribution": {str(key): cube_counts[key] for key in sorted(cube_counts)},
    }


def check_geckolib(path: Path) -> dict[str, object]:
    digest = sha256(path)
    require(digest == EXPECTED_GECKOLIB_SHA256, "GeckoLib 5.5.1 artifact hash drift")
    renderer_super = archive_class_super(path, "com/geckolib/renderer/GeoEntityRenderer.class")
    model_super = archive_class_super(path, "com/geckolib/model/GeoModel.class")
    require(
        renderer_super == "net/minecraft/client/renderer/entity/EntityRenderer",
        "GeoEntityRenderer no longer directly extends EntityRenderer",
    )
    require(model_super == "java/lang/Object", "GeoModel superclass drift")
    return {
        "filename": path.name,
        "bytes": path.stat().st_size,
        "sha256": digest,
        "renderer_superclass": renderer_super,
        "model_superclass": model_super,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ribbits-jar", type=Path, help="exact private Ribbits C9 archive (read-only)")
    parser.add_argument("--geckolib-jar", type=Path, help="exact GeckoLib 5.5.1 archive (read-only)")
    args = parser.parse_args()

    project = Path(__file__).resolve().parent
    result: dict[str, object] = {
        "source_contracts": check_source_contracts(project),
        "historical_c3": check_c3(project),
    }
    if args.ribbits_jar:
        result["ribbits_c9"] = check_ribbits_archive(args.ribbits_jar.resolve())
    if args.geckolib_jar:
        result["geckolib_5_5_1"] = check_geckolib(args.geckolib_jar.resolve())
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, json.JSONDecodeError, zipfile.BadZipFile, ProbeError) as exc:
        print(f"audit probe failed: {exc}", file=sys.stderr)
        raise SystemExit(1) from exc
