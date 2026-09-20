#!/usr/bin/env python3
"""Build and verify the locally retained Enderscape Overlays derivative pack."""

from __future__ import annotations

import argparse
import binascii
import hashlib
import json
import pathlib
import struct
import sys
import zipfile
import zlib
from datetime import datetime


TILE_COUNT = 17
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
FIXED_ZIP_TIME = (1980, 1, 1, 0, 0, 0)
ALL_FACES = frozenset(("bottom", "top", "north", "south", "east", "west"))
FACE_SETS = {
    "all": ALL_FACES,
    "sides": frozenset(("north", "south", "east", "west")),
    "bottom": frozenset(("bottom",)),
    "top": frozenset(("top",)),
    "north": frozenset(("north",)),
    "south": frozenset(("south",)),
    "east": frozenset(("east",)),
    "west": frozenset(("west",)),
}


class GenerationError(RuntimeError):
    pass


def repository_root(path: pathlib.Path) -> pathlib.Path:
    for candidate in (path, *path.parents):
        if (candidate / ".git").exists() and (candidate / "resourcepacks").exists():
            return candidate
    raise GenerationError("could not locate repository root")


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_manifest(path: pathlib.Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        manifest = json.load(handle)
    if manifest.get("schema_version") != 1:
        raise GenerationError("unsupported relationship manifest schema")
    if not manifest.get("relationships") or not manifest.get("templates"):
        raise GenerationError("manifest has no relationships or templates")
    return manifest


def verified_input(root: pathlib.Path, input_spec: dict, label: str) -> pathlib.Path:
    path = root / input_spec["path"]
    if not path.is_file():
        raise GenerationError(f"missing {label} input: {path}")
    actual = sha256(path)
    if actual != input_spec["sha256"]:
        raise GenerationError(f"{label} SHA-256 changed: {actual}")
    return path


def png_rgba(data: bytes, label: str) -> tuple[int, int, bytes]:
    if not data.startswith(PNG_SIGNATURE):
        raise GenerationError(f"{label} is not PNG")
    pos = len(PNG_SIGNATURE)
    width = height = bit_depth = color_type = None
    palette = transparency = None
    payload = bytearray()
    while pos < len(data):
        if pos + 12 > len(data):
            raise GenerationError(f"truncated PNG chunk in {label}")
        size = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        body_end = pos + 8 + size
        if body_end + 4 > len(data):
            raise GenerationError(f"truncated PNG data in {label}")
        body = data[pos + 8:body_end]
        if binascii.crc32(kind + body) & 0xFFFFFFFF != struct.unpack(">I", data[body_end:body_end + 4])[0]:
            raise GenerationError(f"bad PNG CRC in {label}")
        pos = body_end + 4
        if kind == b"IHDR":
            width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(">IIBBBBB", body)
            if compression or filtering or interlace:
                raise GenerationError(f"{label} must be a non-interlaced PNG")
        elif kind == b"PLTE":
            palette = body
        elif kind == b"tRNS":
            transparency = body
        elif kind == b"IDAT":
            payload.extend(body)
        elif kind == b"IEND":
            break
    if width is None or color_type not in {0, 2, 3, 4, 6}:
        raise GenerationError(f"unsupported PNG format in {label}")
    valid_depths = {0: {1, 2, 4, 8}, 2: {8}, 3: {1, 2, 4, 8}, 4: {8}, 6: {8}}
    if bit_depth not in valid_depths[color_type]:
        raise GenerationError(f"unsupported PNG bit depth in {label}")
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
    raw = zlib.decompress(bytes(payload))
    bits_per_pixel = channels * bit_depth
    stride = (width * bits_per_pixel + 7) // 8
    filter_bytes = max(1, (bits_per_pixel + 7) // 8)
    expected = height * (stride + 1)
    if len(raw) != expected:
        raise GenerationError(f"unexpected PNG scanline length in {label}")
    rows: list[bytes] = []
    offset = 0
    previous = bytearray(stride)
    for _ in range(height):
        filter_type = raw[offset]
        offset += 1
        current = bytearray(raw[offset:offset + stride])
        offset += stride
        for index in range(stride):
            left = current[index - filter_bytes] if index >= filter_bytes else 0
            above = previous[index]
            upper_left = previous[index - filter_bytes] if index >= filter_bytes else 0
            if filter_type == 1:
                current[index] = (current[index] + left) & 255
            elif filter_type == 2:
                current[index] = (current[index] + above) & 255
            elif filter_type == 3:
                current[index] = (current[index] + ((left + above) // 2)) & 255
            elif filter_type == 4:
                predictor = left + above - upper_left
                distances = (abs(predictor - left), abs(predictor - above), abs(predictor - upper_left))
                current[index] = (current[index] + (left, above, upper_left)[distances.index(min(distances))]) & 255
            elif filter_type != 0:
                raise GenerationError(f"unsupported PNG filter in {label}")
        rows.append(bytes(current))
        previous = current
    rgba = bytearray()
    for row in rows:
        for pixel_index in range(width):
            if bit_depth < 8:
                packed_index = pixel_index * bit_depth
                sample = (row[packed_index // 8] >> (8 - bit_depth - packed_index % 8)) & ((1 << bit_depth) - 1)
            else:
                sample = row[pixel_index * channels]
            if color_type == 0:
                gray = sample if bit_depth == 8 else round(sample * 255 / ((1 << bit_depth) - 1))
                rgba.extend((gray, gray, gray, 255))
            elif color_type == 2:
                index = pixel_index * channels
                rgba.extend((*row[index:index + 3], 255))
            elif color_type == 3:
                palette_index = sample * 3
                if palette is None or palette_index + 3 > len(palette):
                    raise GenerationError(f"invalid palette in {label}")
                alpha = transparency[sample] if transparency and sample < len(transparency) else 255
                rgba.extend((*palette[palette_index:palette_index + 3], alpha))
            elif color_type == 4:
                index = pixel_index * channels
                rgba.extend((row[index], row[index], row[index], row[index + 1]))
            else:
                index = pixel_index * channels
                rgba.extend(row[index:index + 4])
    return width, height, bytes(rgba)


def png_encode(width: int, height: int, rgba: bytes) -> bytes:
    if len(rgba) != width * height * 4:
        raise GenerationError("invalid RGBA output length")
    scanlines = b"".join(b"\0" + rgba[row * width * 4:(row + 1) * width * 4] for row in range(height))
    def chunk(kind: bytes, body: bytes) -> bytes:
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", binascii.crc32(kind + body) & 0xFFFFFFFF)
    return PNG_SIGNATURE + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(scanlines, 9)) + chunk(b"IEND", b"")


def read_entry(archive: zipfile.ZipFile, name: str) -> bytes:
    try:
        return archive.read(name)
    except KeyError as error:
        raise GenerationError(f"required source entry missing: {name}") from error


def donor_entry(template: dict, tile: int, prefix: str) -> str:
    return f"{prefix}{template['donor']}/{tile}.png"


def alpha(data: bytes) -> bytes:
    return data[3::4]


def check_alpha_equivalence(manifest: dict, matcha: zipfile.ZipFile) -> None:
    prefix = manifest["inputs"]["matcha_overlays"]["tile_prefix"]
    for check in manifest["alpha_equivalence_checks"]:
        reference = None
        for member in check["members"]:
            current = []
            for tile in range(TILE_COUNT):
                _, _, pixels = png_rgba(read_entry(matcha, f"{prefix}{member}/{tile}.png"), f"{member}/{tile}.png")
                current.append(alpha(pixels))
            if reference is None:
                reference = current
            elif current != reference:
                raise GenerationError(f"Matcha alpha topology differs for {check['name']}: {member}")


def check_required_donor_sets(manifest: dict, matcha: zipfile.ZipFile) -> None:
    """Verify the named Matcha donor sets before using their alpha masks."""
    prefix = manifest["inputs"]["matcha_overlays"]["tile_prefix"]
    for check in manifest.get("required_donor_sets", []):
        donor = check["donor"]
        properties_path = f"{prefix}{donor}/{check['properties']}"
        properties_text = read_entry(matcha, properties_path).decode("utf-8")
        property_lines = {line.rstrip("\r") for line in properties_text.splitlines()}
        if "method=overlay" not in property_lines or "tiles=0-16" not in property_lines:
            raise GenerationError(f"Matcha donor set is not a usable 17-tile overlay: {donor}")
        for tile in range(TILE_COUNT):
            _, _, pixels = png_rgba(read_entry(matcha, f"{prefix}{donor}/{tile}.png"), f"{donor}/{tile}.png")
            mask = alpha(pixels)
            if not any(mask) or all(value == 255 for value in mask):
                raise GenerationError(f"Matcha donor alpha is unusable: {donor}/{tile}.png")


def check_asset_evidence(manifest: dict, enderscape: zipfile.ZipFile) -> None:
    """Keep block-ID/model/texture evidence tied to the pinned Enderscape JAR."""
    for check in manifest.get("asset_evidence_checks", []):
        blockstate_path = check["blockstate"]
        blockstate = json.loads(read_entry(enderscape, blockstate_path))
        for state, model in check["variants"].items():
            actual = blockstate.get("variants", {}).get(state, {}).get("model")
            if actual != model:
                raise GenerationError(
                    f"{check['block_id']} does not resolve {state} to {model}: {actual!r}"
                )
        for model_check in check["models"]:
            model = json.loads(read_entry(enderscape, model_check["path"]))
            if model.get("parent") != model_check["parent"]:
                raise GenerationError(f"unexpected parent for {model_check['path']}")
            if model.get("textures") != model_check["textures"]:
                raise GenerationError(f"unexpected texture map for {model_check['path']}")
            if "face_textures" in model_check:
                try:
                    actual_faces = {
                        face: definition["texture"]
                        for face, definition in model["elements"][0]["faces"].items()
                    }
                except (KeyError, IndexError, TypeError) as error:
                    raise GenerationError(f"missing face evidence in {model_check['path']}") from error
                if actual_faces != model_check["face_textures"]:
                    raise GenerationError(f"unexpected face textures for {model_check['path']}")
        for texture_path in check["required_textures"]:
            width, height, _ = png_rgba(read_entry(enderscape, texture_path), texture_path)
            if width <= 0 or height <= 0:
                raise GenerationError(f"empty required texture: {texture_path}")


def relationship_faces(relationship: dict, template: dict) -> frozenset[str]:
    value = relationship.get("faces_override", template["faces"])
    if value is None:
        return ALL_FACES
    try:
        return FACE_SETS[value]
    except KeyError as error:
        raise GenerationError(f"unsupported faces setting for {relationship['id']}: {value!r}") from error


def pair_relationships(manifest: dict, source: str, target: str) -> list[dict]:
    return [
        relationship
        for relationship in manifest["relationships"]
        if relationship["source_blocks"] == [source] and target in relationship["target_blocks"]
    ]


def check_declared_exclusions(manifest: dict, by_id: dict[str, dict]) -> None:
    for relationship in manifest["relationships"]:
        for excluded_id in relationship.get("excludes", []):
            excluded = by_id.get(excluded_id)
            if excluded is None:
                raise GenerationError(f"{relationship['id']} excludes unknown relationship {excluded_id}")
            if excluded["source_blocks"] != relationship["source_blocks"]:
                raise GenerationError(f"{relationship['id']} exclusion {excluded_id} has a different source")
            overlap = set(relationship["target_blocks"]) & set(excluded["target_blocks"])
            if overlap:
                joined = ", ".join(sorted(overlap))
                raise GenerationError(
                    f"{relationship['id']} still overlaps excluded relationship {excluded_id}: {joined}"
                )


def check_no_double_overlays(manifest: dict) -> None:
    """A source/target pair may partition faces, but it may never stack rules."""
    pair_faces: dict[tuple[str, str], list[tuple[str, frozenset[str]]]] = {}
    for relationship in manifest["relationships"]:
        template = manifest["templates"][relationship["template"]]
        if len(relationship["source_blocks"]) != 1:
            raise GenerationError(f"{relationship['id']} must have exactly one source block")
        source = relationship["source_blocks"][0]
        faces = relationship_faces(relationship, template)
        for target in relationship["target_blocks"]:
            key = (source, target)
            for other_id, other_faces in pair_faces.get(key, []):
                if faces & other_faces:
                    raise GenerationError(
                        f"duplicate effective overlay for {source} -> {target}: "
                        f"{other_id} and {relationship['id']} overlap on {', '.join(sorted(faces & other_faces))}"
                    )
            pair_faces.setdefault(key, []).append((relationship["id"], faces))


def check_directed_pair_checks(manifest: dict) -> None:
    for check in manifest.get("directed_pair_checks", []):
        source = check["source"]
        target = check["target"]
        actual = {relationship["id"] for relationship in pair_relationships(manifest, source, target)}
        expected = set(check["relationship_ids"])
        if actual != expected:
            raise GenerationError(
                f"{check['id']} has wrong owner rules: expected {sorted(expected)}, got {sorted(actual)}"
            )
        reverse = pair_relationships(manifest, target, source)
        if reverse:
            raise GenerationError(
                f"{check['id']} has reverse owner rules: {[relationship['id'] for relationship in reverse]}"
            )


def check_priority_chains(manifest: dict) -> None:
    for chain in manifest.get("priority_chains", []):
        members = chain["members"]
        source_rule_ids = chain["source_rule_ids"]
        if len(members) != len(set(members)):
            raise GenerationError(f"{chain['id']} has duplicate priority-chain members")
        for index, source in enumerate(members[:-1]):
            expected = set(source_rule_ids[source])
            for target in members[index + 1:]:
                actual = {relationship["id"] for relationship in pair_relationships(manifest, source, target)}
                if actual != expected:
                    raise GenerationError(
                        f"{chain['id']} lacks its sole directed owner for {source} -> {target}: "
                        f"expected {sorted(expected)}, got {sorted(actual)}"
                    )
                reverse = pair_relationships(manifest, target, source)
                if reverse:
                    raise GenerationError(
                        f"{chain['id']} has a reverse rule for {target} -> {source}: "
                        f"{[relationship['id'] for relationship in reverse]}"
                    )


def validate_relationship_semantics(manifest: dict) -> None:
    by_id = {relationship["id"]: relationship for relationship in manifest["relationships"]}
    if len(by_id) != len(manifest["relationships"]):
        raise GenerationError("relationship IDs must be unique")
    for relationship in manifest["relationships"]:
        if relationship["template"] not in manifest["templates"]:
            raise GenerationError(f"unknown template for {relationship['id']}")
    check_declared_exclusions(manifest, by_id)
    check_no_double_overlays(manifest)
    check_directed_pair_checks(manifest)
    check_priority_chains(manifest)


def properties(relationship: dict, template: dict) -> bytes:
    source = relationship["source_blocks"]
    if len(source) != 1:
        raise GenerationError(f"{relationship['id']} must have exactly one source block")
    lines = [f"method={template['method']}", "tiles=0-16", f"matchBlocks={' '.join(relationship['target_blocks'])}"]
    if template["connect"]:
        lines.append(f"connect={template['connect']}")
    faces = relationship.get("faces_override", template["faces"])
    if faces:
        lines.append(f"faces={faces}")
    lines.append(f"connectBlocks={source[0]}")
    if template["tint"]:
        lines.extend(("tintIndex=0", f"tintBlock={source[0]}"))
    lines.append(f"layer={template['layer']}")
    if relationship.get("priority") == "high":
        lines.append("prioritize=true")
    return ("\n".join(lines) + "\n").encode("utf-8")


def combine(material: bytes, donor: bytes, label: str) -> tuple[int, int, bytes]:
    width, height, material_rgba = png_rgba(material, f"material {label}")
    donor_width, donor_height, donor_rgba = png_rgba(donor, f"donor {label}")
    output_width, output_height = max(width, donor_width), max(height, donor_height)
    if output_width % width or output_width % donor_width or output_height % height or output_height % donor_height:
        raise GenerationError(f"incompatible animation dimensions for {label}: {width}x{height} vs {donor_width}x{donor_height}")
    result = bytearray(output_width * output_height * 4)
    for y in range(output_height):
        for x in range(output_width):
            output_index = (y * output_width + x) * 4
            material_index = ((y % height) * width + x % width) * 4
            donor_index = ((y % donor_height) * donor_width + x % donor_width) * 4
            result[output_index:output_index + 3] = material_rgba[material_index:material_index + 3]
            result[output_index + 3] = donor_rgba[donor_index + 3]
    return output_width, output_height, bytes(result)


def optional_metadata(archive: zipfile.ZipFile, image_entry: str) -> bytes | None:
    try:
        data = archive.read(f"{image_entry}.mcmeta")
    except KeyError:
        return None
    try:
        json.loads(data)
    except json.JSONDecodeError as error:
        raise GenerationError(f"invalid animation metadata: {image_entry}.mcmeta") from error
    return data


def merged_metadata(material: bytes | None, donor: bytes | None, label: str) -> bytes | None:
    if material and donor and material != donor:
        raise GenerationError(f"incompatible animation metadata for {label}")
    return material or donor


def generated_entries(root: pathlib.Path, manifest: dict, source_commit: str, built_at: str) -> dict[str, bytes]:
    validate_relationship_semantics(manifest)
    enderscape_path = verified_input(root, manifest["inputs"]["enderscape"], "Enderscape")
    matcha_path = verified_input(root, manifest["inputs"]["matcha_overlays"], "Matcha Overlays")
    project = manifest["project"]
    entries: dict[str, bytes] = {}
    with zipfile.ZipFile(enderscape_path) as enderscape, zipfile.ZipFile(matcha_path) as matcha:
        check_alpha_equivalence(manifest, matcha)
        check_required_donor_sets(manifest, matcha)
        check_asset_evidence(manifest, enderscape)
        texture_prefix = manifest["inputs"]["enderscape"]["texture_prefix"]
        tile_prefix = manifest["inputs"]["matcha_overlays"]["tile_prefix"]
        for relationship in manifest["relationships"]:
            template = manifest["templates"][relationship["template"]]
            folder = f"assets/enderscape/optifine/ctm/enderscape_overlays/{relationship['id']}"
            entries[f"{folder}/{relationship['id']}.properties"] = properties(relationship, template)
            for tile in range(TILE_COUNT):
                donor_path = donor_entry(template, tile, tile_prefix)
                material_path = f"{texture_prefix}{relationship['material']}"
                donor = read_entry(matcha, donor_path)
                material = read_entry(enderscape, material_path)
                width, height, pixels = combine(material, donor, f"{relationship['id']}/{tile}")
                entries[f"{folder}/{tile}.png"] = png_encode(width, height, pixels)
                metadata = merged_metadata(optional_metadata(enderscape, material_path), optional_metadata(matcha, donor_path), f"{relationship['id']}/{tile}")
                if metadata:
                    entries[f"{folder}/{tile}.png.mcmeta"] = metadata
                if relationship.get("emissive_material"):
                    emissive_path = f"{texture_prefix}{relationship['emissive_material']}"
                    emissive = read_entry(enderscape, emissive_path)
                    width, height, pixels = combine(emissive, donor, f"{relationship['id']}/{tile}_e")
                    entries[f"{folder}/{tile}_e.png"] = png_encode(width, height, pixels)
                    metadata = merged_metadata(optional_metadata(enderscape, emissive_path), optional_metadata(matcha, donor_path), f"{relationship['id']}/{tile}_e")
                    if metadata:
                        entries[f"{folder}/{tile}_e.png.mcmeta"] = metadata
    source_identity = {
        "built_at": built_at,
        "generated_by": "tools/generate_enderscape_overlays.py",
        "inputs": {
            "enderscape": {"filename": enderscape_path.name, "license": manifest["inputs"]["enderscape"]["license"], "sha256": manifest["inputs"]["enderscape"]["sha256"]},
            "matcha_overlays": {"filename": matcha_path.name, "license": manifest["inputs"]["matcha_overlays"]["license"], "sha256": manifest["inputs"]["matcha_overlays"]["sha256"]}
        },
        "relationship_manifest_sha256": hashlib.sha256(json.dumps(manifest, sort_keys=True, separators=(",", ":")).encode("utf-8")).hexdigest(),
        "source_commit": source_commit,
        "version": project["version"]
    }
    entries["pack.mcmeta"] = (json.dumps({"pack": {"description": f"{project['name']} {project['version']}", **project["pack_format"]}}, indent=2) + "\n").encode("utf-8")
    entries["assets/minecraft/optifine/emissive.properties"] = b"suffix.emissive=_e\n"
    entries["source_identity.json"] = (json.dumps(source_identity, indent=2, sort_keys=True) + "\n").encode("utf-8")
    entries["NOTICE.md"] = (
        "Enderscape Overlays is a generated, locally retained CTM resource pack.\n\n"
        "Generated tiles combine Enderscape 3.0.2+mc26.2 texture RGB with alpha/mask topology from Matcha Overlays v37. "
        "Enderscape declares MIT in fabric.mod.json. Matcha Overlays v37 supplies CC BY-NC 4.0 in its LICENSE. "
        "The generated tiles are adapted material and may be shared only under the applicable non-commercial attribution terms. "
        "License: https://creativecommons.org/licenses/by-nc/4.0/\n\n"
        "This pack does not contain either source archive. Exact input filenames and hashes are recorded in source_identity.json.\n"
    ).encode("utf-8")
    return entries


def write_archive(path: pathlib.Path, entries: dict[str, bytes]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9, strict_timestamps=True) as archive:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, FIXED_ZIP_TIME)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            info.create_system = 3
            archive.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)


def validate_built_at(value: str) -> None:
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as error:
        raise GenerationError("--built-at must be RFC 3339 UTC") from error
    if parsed.tzinfo is None or value[-1:] != "Z":
        raise GenerationError("--built-at must be UTC and end in Z")


def build(args: argparse.Namespace) -> None:
    root = repository_root(args.manifest.resolve())
    manifest = read_manifest(args.manifest)
    validate_built_at(args.built_at)
    if len(args.source_commit) != 40 or any(character not in "0123456789abcdef" for character in args.source_commit):
        raise GenerationError("--source-commit must be a lowercase 40-character Git commit")
    entries = generated_entries(root, manifest, args.source_commit, args.built_at)
    write_archive(args.artifact, entries)
    print(f"built {args.artifact} ({len(entries)} entries, sha256 {sha256(args.artifact)})")


def verify(args: argparse.Namespace) -> None:
    root = repository_root(args.manifest.resolve())
    manifest = read_manifest(args.manifest)
    if not args.artifact.is_file():
        raise GenerationError(f"artifact is missing: {args.artifact}")
    with zipfile.ZipFile(args.artifact) as archive:
        names = archive.namelist()
        if names != sorted(names) or len(names) != len(set(names)):
            raise GenerationError("artifact entries are not unique, sorted, and deterministic")
        source = json.loads(read_entry(archive, "source_identity.json"))
        expected = generated_entries(root, manifest, source["source_commit"], source["built_at"])
        if set(names) != set(expected):
            raise GenerationError("artifact entry set does not match the manifest")
        for name, value in expected.items():
            if read_entry(archive, name) != value:
                raise GenerationError(f"artifact entry differs from deterministic generation: {name}")
    print(f"verified {args.artifact} (sha256 {sha256(args.artifact)})")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    for command in ("build", "verify"):
        subparser = commands.add_parser(command)
        subparser.add_argument("--manifest", type=pathlib.Path, required=True)
        subparser.add_argument("--artifact", type=pathlib.Path, required=True)
        if command == "build":
            subparser.add_argument("--source-commit", required=True)
            subparser.add_argument("--built-at", required=True)
    args = parser.parse_args()
    try:
        if args.command == "build":
            build(args)
        else:
            verify(args)
    except (GenerationError, OSError, zipfile.BadZipFile, json.JSONDecodeError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
