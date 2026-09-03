from __future__ import annotations

import copy
import hashlib
import io
import json
import os
import sqlite3
import stat
import tempfile
import unittest
import warnings
import zipfile
from contextlib import closing
from pathlib import Path
from unittest.mock import patch

from tests.test_instance_manager import (
    ManagerFixture,
    artifact,
    candidate_declaration,
    stable_uuid,
    tree_snapshot,
    unit,
)
from tools.test_instance_manager.manager import (
    FabricModDescriptor,
    ManagedArtifact,
    ManagerError,
    PlatformAttestation,
    PhysicalManager,
    _NestedJarTraversalBudget,
    _enforce_runtime_dependency_policy,
    _fabric_predicate_matches,
    _runtime_dependency_predicate_restrictions,
    _read_fabric_descriptor_tree,
    _read_fabric_manifest_from_archive,
    _windows_processes_using_profile,
)
from tools.runtime_slots import migrate_runtime_state, validate_runtime_state


C57 = "4.2.1-bge.canary57.unified+26.2"
C58 = "4.2.2-bge.canary58.glass-corner-uv+26.2"
COMPATIBLE_LATER = "4.2.9-bge.canary99.compatible+26.2"
PRE_UNIFIED = "4.2.0-bge.canary56.pre-unified+26.2"
UPPER_BOUND = "4.3.0-"
TROWEL_RANGE = ">=4.2.1-bge.canary57.unified+26.2 <4.3.0-"
_ENVIRONMENT_UNSET = object()


def descriptor(
    filename: str,
    primary_id: str,
    version: str,
    *,
    provides: tuple[str, ...] = (),
    depends: dict[str, tuple[str, ...]] | None = None,
    recommends: dict[str, tuple[str, ...]] | None = None,
    suggests: dict[str, tuple[str, ...]] | None = None,
    project_id: str | None = None,
    root_container_sha256: str | None = None,
) -> FabricModDescriptor:
    return FabricModDescriptor(
        path=Path(filename),
        relative_path="mods/" + filename,
        filename=filename,
        primary_id=primary_id,
        provides=provides,
        version=version,
        depends=depends or {},
        recommends=recommends or {},
        suggests=suggests or {},
        managed_project_uuid=("00000000-0000-4000-8000-" + ("1" if project_id else "0") * 12),
        managed_project_id=project_id,
        managed_deployment_id=("00000000-0000-4000-8000-" + ("2" if project_id else "0") * 12),
        managed_artifact_id=("00000000-0000-4000-8000-" + ("3" if project_id else "0") * 12),
        root_container_filename=filename,
        root_container_sha256=root_container_sha256,
    )


def write_dependency_mod(
    path: Path,
    primary_id: str,
    version: str,
    *,
    provides: tuple[str, ...] = (),
    depends: dict[str, str | list[str]] | None = None,
) -> str:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr(
            "fabric.mod.json",
            json.dumps(
                {
                    "schemaVersion": 1,
                    "id": primary_id,
                    "version": version,
                    "name": primary_id,
                    "provides": list(provides),
                    "depends": depends or {},
                }
            ),
        )
        archive.writestr("fixture.txt", version)
    return hashlib.sha256(path.read_bytes()).hexdigest()


class AcceptedCompanionFixture:
    """QSN-shaped accepted multi-artifact unit with one manifest-bound slot artifact."""

    def __init__(self, root: Path, *, compat_depends_on_upstream: bool = True):
        self.fixture = ManagerFixture(root)
        fixture = self.fixture
        self.project_uuid = stable_uuid("project:quick-stack-nearby-compat")
        self.version = "0.1.0-canary6"
        self.source_commit = "3c8cc5917da9fd016e57a969955fa7c6e3b08661"
        self.core_name = "quick-stack-nearby-0.4.0.jar"
        self.compat_name = "quick-stack-nearby-compat-0.1.0-canary6.jar"

        core_hash = write_dependency_mod(
            fixture.mods / self.core_name,
            "quick-stack-nearby",
            "0.4.0",
        )
        compat_source = fixture.repository / "artifacts" / self.compat_name
        compat_hash = write_dependency_mod(
            compat_source,
            "quick_stack_nearby_compat",
            self.version,
            depends=(
                {"quick-stack-nearby": "=0.4.0"}
                if compat_depends_on_upstream
                else None
            ),
        )
        (fixture.mods / self.compat_name).write_bytes(compat_source.read_bytes())
        self.core_bytes = (fixture.mods / self.core_name).read_bytes()
        self.compat_bytes = compat_source.read_bytes()

        self.core_artifact = artifact(
            self.core_name,
            "quick-stack-nearby",
            core_hash,
        )
        self.compat_artifact = artifact(
            self.compat_name,
            "quick_stack_nearby_compat",
            compat_hash,
            source_type="REPOSITORY",
            source_path="artifacts/" + self.compat_name,
        )
        self.accepted_unit = unit(
            "quick-stack-nearby-compat",
            self.version,
            self.core_artifact,
            project_uuid=self.project_uuid,
        )
        self.accepted_unit["source_commit"] = self.source_commit
        self.accepted_unit["artifacts"].append(copy.deepcopy(self.compat_artifact))

        self.slot_unit = copy.deepcopy(self.accepted_unit)
        self.slot_unit["deployment_id"] = stable_uuid("qsn-regression-deployment")
        slot_artifact = copy.deepcopy(self.compat_artifact)
        slot_artifact["artifact_id"] = stable_uuid("qsn-regression-artifact")
        self.slot_unit["artifacts"] = [slot_artifact]

        fixture.project_index[self.project_uuid] = "quick-stack-nearby-compat"
        fixture.project_display_names[self.project_uuid] = "Quick Stack Nearby Compatibility"
        state = migrate_runtime_state(fixture.state, fixture.project_index)
        state["accepted_baseline"]["members"].append(
            {
                "unit": copy.deepcopy(self.accepted_unit),
                "accepted_at": "2026-08-29T12:00:00Z",
            }
        )
        state["accepted_baseline"]["provenance"]["accepted_artifact_count"] += 2
        validate_runtime_state(state, fixture.project_index)
        fixture.state = state
        fixture.state_path.write_text(
            json.dumps(state, indent=2) + "\n",
            encoding="utf-8",
        )
        fixture.manager.project_index = copy.deepcopy(fixture.project_index)
        fixture.manager.project_display_names = copy.deepcopy(
            fixture.project_display_names
        )
        fixture.manager.adopt(dry_run=False)

    @staticmethod
    def _member_declaration(member: dict) -> dict:
        return candidate_declaration(
            member["unit"],
            member["replaces_accepted_deployment_id"],
            member.get("dependency_overrides"),
            member.get("accepted_companion_artifacts"),
        )

    def operation(
        self,
        *,
        companion_artifacts: list[dict] | None | object = ...,
        slot_unit: dict | None = None,
    ) -> dict:
        state = self.fixture.repository_state()
        slot_a = state["slots"]["A"]
        assert slot_a is not None
        candidate = candidate_declaration(
            slot_unit or self.slot_unit,
            self.accepted_unit["deployment_id"],
        )
        if companion_artifacts is ...:
            candidate["accepted_companion_artifacts"] = [
                copy.deepcopy(self.core_artifact)
            ]
        elif companion_artifacts is not None:
            candidate["accepted_companion_artifacts"] = copy.deepcopy(
                companion_artifacts
            )
        return {
            "type": "DEPLOY_PROFILE",
            "slots": {
                "A": {
                    "members": [
                        self._member_declaration(member)
                        for member in slot_a["members"]
                    ]
                },
                "B": {"candidate": candidate},
            },
        }

    def successor_slot_unit(self) -> dict:
        version = "0.1.0-canary7"
        name = "quick-stack-nearby-compat-0.1.0-canary7.jar"
        source = self.fixture.repository / "artifacts" / name
        sha256 = write_dependency_mod(
            source,
            "quick_stack_nearby_compat",
            version,
            depends={"quick-stack-nearby": ">=0.4.0"},
        )
        return unit(
            "quick-stack-nearby-compat",
            version,
            artifact(
                name,
                "quick_stack_nearby_compat",
                sha256,
                source_type="REPOSITORY",
                source_path="artifacts/" + name,
            ),
            project_uuid=self.project_uuid,
        )

    def deploy(
        self,
        *,
        at: str = "2099-01-01T00:00:30Z",
        slot_unit: dict | None = None,
    ) -> dict:
        state = self.fixture.repository_state()
        return self.fixture.manager.transition(
            operation=self.operation(slot_unit=slot_unit),
            expected_revision=state["revision"],
            at=at,
            dry_run=False,
        )


def fabric_mod_jar_bytes(
    primary_id: str,
    version: str,
    *,
    provides: tuple[str, ...] = (),
    depends: dict[str, str | list[str]] | None = None,
    recommends: dict[str, str | list[str]] | None = None,
    suggests: dict[str, str | list[str]] | None = None,
    declared_paths: tuple[str, ...] = (),
    nested_entries: tuple[tuple[str, bytes], ...] = (),
    payload: bytes | None = None,
    environment: object = _ENVIRONMENT_UNSET,
) -> bytes:
    """Build deterministic Fabric fixture bytes, including declared children."""

    output = io.BytesIO()
    manifest: dict[str, object] = {
        "schemaVersion": 1,
        "id": primary_id,
        "version": version,
        "name": primary_id,
        "provides": list(provides),
        "depends": depends or {},
        "jars": [{"file": item} for item in declared_paths],
    }
    if environment is not _ENVIRONMENT_UNSET:
        manifest["environment"] = environment
    if recommends is not None:
        manifest["recommends"] = recommends
    if suggests is not None:
        manifest["suggests"] = suggests
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as archive:
        archive.writestr(
            "fabric.mod.json",
            json.dumps(
                manifest,
                sort_keys=True,
            ),
        )
        archive.writestr("fixture.bin", payload if payload is not None else version.encode("utf-8"))
        for member_path, member_bytes in nested_entries:
            archive.writestr(member_path, member_bytes)
    return output.getvalue()


def managed_fixture_artifact(path: Path, primary_id: str, version: str) -> ManagedArtifact:
    sha256 = hashlib.sha256(path.read_bytes()).hexdigest()
    return ManagedArtifact(
        deployment_id=stable_uuid("nested-deployment:" + primary_id),
        artifact_id=stable_uuid("nested-artifact:" + sha256),
        project_uuid=stable_uuid("nested-project:" + primary_id),
        project_id=primary_id + "-project",
        filename=path.name,
        sha256=sha256,
        mod_id=primary_id,
        ownership_mod_ids=(primary_id,),
        relative_path="mods/" + path.name,
        active=True,
        source={"type": "ADOPTED_TARGET", "path": "mods/" + path.name},
        expected_fabric_version=version,
    )


def install_fake_modrinth_launch_authority(
    fixture: ManagerFixture,
    *,
    game_version: str = "26.2",
    loader_version: str = "0.19.3",
    install_stage: str = "installed",
    content_set_status: str = "available",
    metadata_id: str | None = None,
    metadata_loader_coordinates: list[str] | None = None,
    required_java_major: int = 25,
    configured_java_version: str = "25.0.2",
    probed_java_specification: str = "25",
    probed_java_version: str = "25.0.2",
) -> dict[str, object]:
    """Create isolated Modrinth launch authority for the exact fixture target."""

    app_root = fixture.target.parent.parent
    database = app_root / "app.db"
    java_home = app_root / "meta" / "java_versions" / "fixture-java"
    selected_java = java_home / "bin" / ("javaw.exe" if os.name == "nt" else "java")
    probe_java = selected_java.with_name("java.exe") if os.name == "nt" else selected_java
    probe_java.parent.mkdir(parents=True, exist_ok=True)
    selected_java.write_bytes(b"fixture javaw")
    if probe_java != selected_java:
        probe_java.write_bytes(b"fixture java")

    instance_id = bytes.fromhex("00112233445566778899aabbccddeeff")
    content_set_id = bytes.fromhex("ffeeddccbbaa99887766554433221100")
    decoy_instance_id = bytes.fromhex("11111111111111111111111111111111")
    decoy_content_set_id = bytes.fromhex("22222222222222222222222222222222")
    with closing(sqlite3.connect(database)) as connection:
        connection.executescript(
            """
            CREATE TABLE instances (
                id BLOB PRIMARY KEY,
                path TEXT,
                install_stage TEXT,
                applied_content_set_id BLOB
            );
            CREATE TABLE instance_content_sets (
                id BLOB PRIMARY KEY,
                instance_id BLOB,
                status TEXT,
                game_version TEXT,
                loader TEXT,
                loader_version TEXT,
                modified TEXT
            );
            CREATE TABLE instance_launch_overrides (
                instance_id BLOB PRIMARY KEY,
                overrides BLOB
            );
            CREATE TABLE java_versions (
                major_version INTEGER,
                full_version TEXT,
                path TEXT
            );
            """
        )
        connection.execute(
            "INSERT INTO instances VALUES (?, ?, ?, ?)",
            (instance_id, fixture.target.name, install_stage, content_set_id),
        )
        connection.execute(
            "INSERT INTO instance_content_sets VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                content_set_id,
                instance_id,
                content_set_status,
                game_version,
                "fabric",
                loader_version,
                "2099-01-01T00:00:00Z",
            ),
        )
        connection.execute(
            "INSERT INTO instance_launch_overrides VALUES (?, jsonb(?))",
            (instance_id, json.dumps({"java_path": None})),
        )
        # This malformed unrelated row proves that production code selects
        # only the exact configured target rather than enumerating profiles.
        connection.execute(
            "INSERT INTO instances VALUES (?, ?, ?, ?)",
            (decoy_instance_id, "Unrelated Fixture", "broken", decoy_content_set_id),
        )
        connection.execute(
            "INSERT INTO instance_content_sets VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                decoy_content_set_id,
                decoy_instance_id,
                "broken",
                "not-a-version",
                "unknown",
                "latest",
                "2099-01-01T00:00:00Z",
            ),
        )
        connection.execute(
            "INSERT INTO java_versions VALUES (?, ?, ?)",
            (required_java_major, configured_java_version, str(selected_java)),
        )
        connection.commit()

    version_id = f"{game_version}-{loader_version}"
    metadata_path = app_root / "meta" / "versions" / version_id / f"{version_id}.json"
    metadata_path.parent.mkdir(parents=True, exist_ok=True)
    loader_coordinates = (
        [f"net.fabricmc:fabric-loader:{loader_version}"]
        if metadata_loader_coordinates is None
        else metadata_loader_coordinates
    )
    metadata_path.write_text(
        json.dumps(
            {
                "id": version_id if metadata_id is None else metadata_id,
                "javaVersion": {"majorVersion": required_java_major},
                "libraries": [
                    {"name": coordinate}
                    for coordinate in [*loader_coordinates, "org.example:unrelated:1.0.0"]
                ],
            },
            sort_keys=True,
        ),
        encoding="utf-8",
    )
    probe_calls: list[Path] = []

    def probe(executable: Path) -> dict[str, str]:
        probe_calls.append(executable)
        return {
            "java.specification.version": probed_java_specification,
            "java.version": probed_java_version,
            "java.home": str(java_home),
        }

    fixture.manager = PhysicalManager.from_config(
        fixture.config_path,
        project_index=fixture.project_index,
        project_display_names=fixture.project_display_names,
        java_property_probe=probe,
    )
    return {
        "app_root": app_root,
        "database": database,
        "metadata_path": metadata_path,
        "selected_java": selected_java,
        "probe_java": probe_java,
        "probe_calls": probe_calls,
        "java_home": java_home,
    }


def set_fake_java_override(authority: dict[str, object], path: Path) -> None:
    """Point the exact fixture instance at one explicit Java executable."""

    with closing(sqlite3.connect(authority["database"])) as connection:
        connection.execute(
            "UPDATE instance_launch_overrides SET overrides = jsonb(?)",
            (json.dumps({"java_path": str(path)}),),
        )
        connection.commit()


def platform_candidate_operation(fixture: ManagerFixture) -> tuple[dict, dict]:
    version = "0.1.7-canary8"
    filename = f"matcha-heart-death-compat-{version}.jar"
    source = fixture.repository / "artifacts" / filename
    sha256 = write_dependency_mod(
        source,
        "matcha_heart_death_compat",
        version,
        depends={
            "minecraft": "=26.2",
            "fabricloader": ">=0.19.0 <0.20.0-",
            "java": ">=25",
        },
    )
    candidate = unit(
        "matcha-heart-death-compat",
        version,
        artifact(
            filename,
            "matcha_heart_death_compat",
            sha256,
            source_type="REPOSITORY",
            source_path="artifacts/" + filename,
        ),
        project_uuid=fixture.heart_uuid,
    )
    mossy, _ = fixture.add_mossy_repository_candidate()
    return candidate, {
        "type": "DEPLOY_PROFILE",
        "slots": {
            "A": {
                "members": [
                    candidate_declaration(candidate, fixture.c5["deployment_id"]),
                    candidate_declaration(mossy),
                ]
            },
            "B": None,
        },
    }


class FabricPredicateTests(unittest.TestCase):
    def test_trowel_bounded_range_matches_fabric_prerelease_ordering(self) -> None:
        cases = {
            C57: True,
            C58: True,
            COMPATIBLE_LATER: True,
            PRE_UNIFIED: False,
            UPPER_BOUND: False,
            "4.3.0-bge.canary1.breaking+26.2": False,
        }
        for version, expected in cases.items():
            with self.subTest(version=version):
                self.assertEqual(expected, _fabric_predicate_matches(version, TROWEL_RANGE))

    def test_unknown_or_invalid_predicate_syntax_fails_closed(self) -> None:
        # Fabric deliberately falls back to exact StringVersion matching for
        # strings such as `1.x.2`; only forms rejected by Loader are errors.
        for predicate in (">", "<nonsense", ">=", ">=1.x"):
            with self.subTest(predicate=predicate):
                with self.assertRaises(ManagerError):
                    _fabric_predicate_matches(C58, predicate)

    def test_loader_four_component_wildcard_uses_exact_next_component_bounds(self) -> None:
        cases = {
            "1.2.3": True,
            "1.2.3.0": True,
            "1.2.3.99": True,
            "1.2.4-": False,
            "1.2.4-alpha": False,
            "1.3.0": False,
            "1.2.2.99": False,
        }
        for version, expected in cases.items():
            with self.subTest(version=version):
                self.assertEqual(expected, _fabric_predicate_matches(version, "1.2.3.x"))

    def test_loader_longer_component_wildcard_is_supported(self) -> None:
        self.assertTrue(_fabric_predicate_matches("1.2.3.4.99", "1.2.3.4.x"))
        self.assertFalse(_fabric_predicate_matches("1.2.3.5-", "1.2.3.4.x"))


class RuntimeDependencyPolicyTests(unittest.TestCase):
    @staticmethod
    def policy(*exceptions: dict[str, object]) -> dict[str, object]:
        return {
            "contract": "CAPABILITY_OR_PROVIDER",
            "exceptions": list(exceptions),
        }

    def test_capability_alias_and_genuine_minimum_floors_are_allowed(self) -> None:
        slab = descriptor(
            "slab-c3.jar",
            "slab_decorations",
            "0.1.0-canary3",
            depends={
                "minecraft": ("=26.2",),
                "fabricloader": (">=0.19.3",),
                "fabric-api": (">=0.157.0",),
                "java": (">=25",),
                "more_slabs_stairs_and_walls": ("*",),
            },
            suggests={
                "clutternomore": ("*",),
                "cnm_terrain_slabs_compat": ("*",),
            },
            project_id="slab-decorations",
        )
        result = _enforce_runtime_dependency_policy(
            [slab],
            self.policy(),
            release_label="slab-decorations/slab-c3.jar",
        )
        self.assertEqual("CURRENT_RELEASE_POLICY_ENFORCED", result["classification"])
        self.assertEqual(7, result["checked_predicate_count"])
        self.assertEqual([], result["accepted_exceptions"])

    def test_exact_canary_build_metadata_ceiling_and_family_ranges_need_evidence(self) -> None:
        cases = {
            "=" + C58: {"CANARY_SPECIFIC_VERSION", "EXACT_VERSION_PIN", "SEMANTIC_BUILD_METADATA"},
            "<5.0.0-": {"UPPER_BOUND"},
            "2.0.x": {"EXACT_VERSION_PIN", "VERSION_FAMILY_CEILING"},
        }
        for predicate, expected in cases.items():
            with self.subTest(predicate=predicate):
                self.assertEqual(
                    expected,
                    set(_runtime_dependency_predicate_restrictions("provider_api", predicate)),
                )
                consumer = descriptor(
                    "consumer.jar",
                    "consumer",
                    "1.0.0",
                    depends={"provider_api": (predicate,)},
                    project_id="consumer",
                )
                with self.assertRaisesRegex(ManagerError, "runtime dependency policy violation"):
                    _enforce_runtime_dependency_policy(
                        [consumer],
                        self.policy(),
                        release_label="consumer/consumer.jar",
                    )

    def test_exact_exception_requires_matching_nonstale_regression_evidence(self) -> None:
        predicate = "<5.0.0-"
        exception = {
            "consumer_id": "consumer",
            "relationship": "depends",
            "dependency_id": "provider_api",
            "predicate": predicate,
            "reason": "Provider 5 removed the API used by this release.",
            "regression_evidence": ["GameTest provider-5-api-removal fails before initialization."],
        }
        consumer = descriptor(
            "consumer.jar",
            "consumer",
            "1.0.0",
            depends={"provider_api": (predicate,)},
            project_id="consumer",
        )
        result = _enforce_runtime_dependency_policy(
            [consumer],
            self.policy(exception),
            release_label="consumer/consumer.jar",
        )
        self.assertEqual(1, len(result["accepted_exceptions"]))

        stale = copy.deepcopy(exception)
        stale["predicate"] = "<4.0.0-"
        with self.assertRaisesRegex(ManagerError, "violation"):
            _enforce_runtime_dependency_policy(
                [consumer],
                self.policy(stale),
                release_label="consumer/consumer.jar",
            )

    def test_declared_nested_metadata_is_inside_policy_scope(self) -> None:
        nested = fabric_mod_jar_bytes(
            "nested_consumer",
            "1.0.0",
            depends={"provider_api": "=1.2.3"},
        )
        outer = fabric_mod_jar_bytes(
            "outer_consumer",
            "1.0.0",
            declared_paths=("META-INF/jars/nested.jar",),
            nested_entries=(("META-INF/jars/nested.jar", nested),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "outer.jar"
            path.write_bytes(outer)
            descriptors = _read_fabric_descriptor_tree(path)
        with self.assertRaisesRegex(
            ManagerError,
            r"nested_consumer.*outer\.jar!/META-INF/jars/nested\.jar",
        ):
            _enforce_runtime_dependency_policy(
                descriptors,
                self.policy(),
                release_label="outer/outer.jar",
            )

    def test_packaged_optional_relationships_are_parsed_and_enforced(self) -> None:
        payload = fabric_mod_jar_bytes(
            "consumer",
            "1.0.0",
            recommends={"recommended_api": ">=1.0.0"},
            suggests={"optional_provider": "=2.0.0+tested"},
        )
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "consumer.jar"
            path.write_bytes(payload)
            descriptors = _read_fabric_descriptor_tree(path)
        self.assertEqual(
            {"optional_provider": ("=2.0.0+tested",)},
            descriptors[0].suggests,
        )
        with self.assertRaisesRegex(ManagerError, r"optional_provider.*suggests"):
            _enforce_runtime_dependency_policy(
                descriptors,
                self.policy(),
                release_label="consumer/consumer.jar",
            )

    def test_manager_binds_policy_only_to_exact_current_filename_and_hash(self) -> None:
        sha256 = "a" * 64
        current = descriptor(
            "slab-c3.jar",
            "slab_decorations",
            "0.1.0-canary3",
            depends={"more_slabs_stairs_and_walls": ("*",)},
            project_id="slab-decorations",
            root_container_sha256=sha256,
        )
        manifest = {
            "identity": {"project_id": "slab-decorations"},
            "state": {
                "releases": {
                    "current": {
                        "version": "0.1.0-canary3",
                        "artifact": {"filename": "slab-c3.jar", "sha256": sha256},
                        "source_commit": "f" * 40,
                        "runtime_dependency_policy": self.policy(),
                    }
                }
            },
        }

        class Catalog:
            repository_statuses = {
                current.managed_project_uuid: (
                    Path("projects/slab-decorations/WORKBENCH_STATUS.json"),
                    manifest,
                )
            }

        report = PhysicalManager._runtime_dependency_policy_report(Catalog(), [current])  # type: ignore[arg-type]
        self.assertEqual("RUNTIME_DEPENDENCY_POLICY_VERIFIED", report["status"])
        self.assertEqual(1, len(report["enforced_artifacts"]))
        self.assertEqual([], report["grandfathered_artifacts"])

    def test_shared_nested_bytes_are_enforced_for_each_owning_root_release(self) -> None:
        nested = fabric_mod_jar_bytes(
            "shared_nested_consumer",
            "1.0.0",
            depends={"provider_api": "=1.2.3"},
        )
        exception = {
            "consumer_id": "shared_nested_consumer",
            "relationship": "depends",
            "dependency_id": "provider_api",
            "predicate": "=1.2.3",
            "reason": "The fixture intentionally models one demonstrated exact API contract.",
            "regression_evidence": ["The provider-1.2.4 fixture fails its focused initialization check."],
        }

        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            descriptor_sets: list[FabricModDescriptor] = []
            statuses: dict[str, tuple[Path, dict[str, object]]] = {}
            for index, project_id in enumerate(("outer-a", "outer-b"), start=1):
                filename = project_id + ".jar"
                payload = fabric_mod_jar_bytes(
                    project_id.replace("-", "_"),
                    "1.0.0",
                    declared_paths=("META-INF/jars/shared.jar",),
                    nested_entries=(("META-INF/jars/shared.jar", nested),),
                )
                path = root / filename
                path.write_bytes(payload)
                sha256 = hashlib.sha256(payload).hexdigest()
                project_uuid = f"00000000-0000-4000-8000-{index:012d}"
                managed = ManagedArtifact(
                    deployment_id=f"10000000-0000-4000-8000-{index:012d}",
                    artifact_id=f"20000000-0000-4000-8000-{index:012d}",
                    project_uuid=project_uuid,
                    project_id=project_id,
                    filename=filename,
                    sha256=sha256,
                    mod_id=project_id.replace("-", "_"),
                    ownership_mod_ids=(project_id.replace("-", "_"),),
                    relative_path="mods/" + filename,
                    active=True,
                    source={"type": "REPOSITORY", "path": filename},
                )
                descriptor_sets.extend(_read_fabric_descriptor_tree(path, artifact=managed))
                statuses[project_uuid] = (
                    Path("projects") / project_id / "WORKBENCH_STATUS.json",
                    {
                        "identity": {"project_id": project_id},
                        "state": {
                            "releases": {
                                "current": {
                                    "version": "1.0.0",
                                    "artifact": {"filename": filename, "sha256": sha256},
                                    "source_commit": "f" * 40,
                                    "runtime_dependency_policy": self.policy(exception),
                                }
                            }
                        },
                    },
                )

        class Catalog:
            repository_statuses = statuses

        report = PhysicalManager._runtime_dependency_policy_report(  # type: ignore[arg-type]
            Catalog(),
            descriptor_sets,
        )
        self.assertEqual(2, len(report["enforced_artifacts"]))
        self.assertEqual([2, 2], [item["descriptor_count"] for item in report["enforced_artifacts"]])
        self.assertEqual(
            ["shared_nested_consumer", "shared_nested_consumer"],
            [
                item["accepted_exceptions"][0]["consumer_id"]
                for item in report["enforced_artifacts"]
            ],
        )


class FabricDependencyGraphTests(unittest.TestCase):
    def setUp(self) -> None:
        self.bge_c58 = descriptor(
            "bge-c58.jar",
            "cnm_terrain_slabs_compat",
            C58,
            provides=("more_slabs_stairs_and_walls",),
            project_id="block-geometry-extensions",
        )
        self.trowel = descriptor(
            "trowel-canary9.jar",
            "shulker_trowel",
            "0.1.0-canary9",
            depends={"cnm_terrain_slabs_compat": (TROWEL_RANGE,)},
            project_id="shulker-trowel",
        )
        self.managed_ids = {
            "cnm_terrain_slabs_compat",
            "more_slabs_stairs_and_walls",
            "shulker_trowel",
        }

    def resolve(self, *items: FabricModDescriptor) -> dict:
        return PhysicalManager._resolve_dependency_graph(  # type: ignore[arg-type]
            None,
            items,
            managed_ownership_ids=set(self.managed_ids),
            phase="TEST",
        )

    def test_bounded_trowel_dependency_resolves_exact_c58_alias_owner(self) -> None:
        report = self.resolve(self.bge_c58, self.trowel)
        resolution = report["resolutions"][0]
        self.assertEqual("FABRIC_DEPENDENCY_GRAPH_VERIFIED", report["status"])
        self.assertEqual("RESOLVED_MANAGED_PROVIDER", resolution["classification"])
        self.assertEqual(C58, resolution["provider"]["version"])
        self.assertEqual(
            ["more_slabs_stairs_and_walls"],
            resolution["provider"]["provides"],
        )

    def test_flexible_stable_alias_accepts_a_newer_unified_provider(self) -> None:
        slab = descriptor(
            "slab-c3.jar",
            "slab_decorations",
            "0.1.0-canary3",
            depends={"more_slabs_stairs_and_walls": ("*",)},
            project_id="slab-decorations",
        )
        report = self.resolve(self.bge_c58, slab)
        resolution = report["resolutions"][0]
        self.assertEqual("more_slabs_stairs_and_walls", resolution["dependency_id"])
        self.assertEqual(C58, resolution["provider"]["version"])
        self.assertEqual("cnm_terrain_slabs_compat", resolution["provider"]["primary_id"])

    def test_metadata_array_predicates_are_or_not_and(self) -> None:
        trowel = descriptor(
            "trowel-or.jar",
            "shulker_trowel",
            "0.1.0-canary9",
            depends={"cnm_terrain_slabs_compat": ("=0.0.0-never", TROWEL_RANGE)},
            project_id="shulker-trowel",
        )
        resolution = self.resolve(self.bge_c58, trowel)["resolutions"][0]
        self.assertEqual([TROWEL_RANGE], resolution["matched_predicates"])

    def test_exact_c57_trowel_rejects_c58_and_names_companion(self) -> None:
        stale = descriptor(
            "trowel-canary8.jar",
            "shulker_trowel",
            "0.1.0-canary8",
            depends={"cnm_terrain_slabs_compat": ("=" + C57,)},
            project_id="shulker-trowel",
        )
        with self.assertRaisesRegex(
            ManagerError,
            r"shulker-trowel@0\.1\.0-canary8.*cnm_terrain_slabs_compat.*same atomic cohort",
        ):
            self.resolve(self.bge_c58, stale)

    def test_missing_managed_provider_fails_closed(self) -> None:
        with self.assertRaisesRegex(ManagerError, "missing managed Fabric dependency.*companion"):
            self.resolve(self.trowel)

    def test_missing_external_hard_dependency_also_fails_closed(self) -> None:
        consumer = descriptor(
            "external-consumer.jar",
            "external_consumer",
            "1.0.0",
            depends={"fabric-api": (">=0.100.0",)},
            project_id="external-consumer",
        )
        with self.assertRaisesRegex(
            ManagerError,
            r"missing external Fabric dependency.*fabric-api.*no exact enabled provider",
        ):
            self.resolve(consumer)

    def test_physically_present_unmanaged_external_provider_is_resolved(self) -> None:
        consumer = descriptor(
            "external-consumer.jar",
            "external_consumer",
            "1.0.0",
            depends={"fabric-api": (">=0.100.0",)},
            project_id="external-consumer",
        )
        fabric_api = descriptor("fabric-api.jar", "fabric-api", "0.120.0+26.2")
        resolution = self.resolve(consumer, fabric_api)["resolutions"][0]
        self.assertEqual("RESOLVED_EXTERNAL_ENABLED_PROVIDER", resolution["classification"])
        self.assertEqual("0.120.0+26.2", resolution["provider"]["version"])

    def test_unrelated_external_duplicate_is_observed_but_does_not_block(self) -> None:
        sodium_stable = descriptor(
            "sodium-stable.jar",
            "sodium",
            "0.9.1+mc26.2",
        )
        sodium_alpha = descriptor(
            "sodium-alpha.jar",
            "sodium",
            "0.9.2-alpha.4+mc26.2",
        )
        report = self.resolve(self.bge_c58, self.trowel, sodium_stable, sodium_alpha)
        groups = report["observed_out_of_scope_duplicate_ownership_groups"]
        self.assertEqual(1, len(groups))
        self.assertEqual("sodium", groups[0]["ownership_id"])
        self.assertEqual(
            "OBSERVED_EXTERNAL_DUPLICATE_NOT_EVALUATED",
            groups[0]["classification"],
        )
        self.assertEqual("EXCLUDED_FROM_PROVIDER_SET", groups[0]["provider_resolution"])
        self.assertEqual(
            {"sodium-stable.jar", "sodium-alpha.jar"},
            {owner["filename"] for owner in groups[0]["owners"]},
        )
        self.assertEqual(
            ["cnm_terrain_slabs_compat"],
            [item["dependency_id"] for item in report["resolutions"]],
        )

    def test_unrelated_external_duplicate_is_visible_in_dry_run_and_verify_receipts(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = ManagerFixture(Path(temporary))
            write_dependency_mod(
                fixture.mods / "sodium-stable.jar",
                "sodium",
                "0.9.1+mc26.2",
            )
            write_dependency_mod(
                fixture.mods / "sodium-alpha.jar",
                "sodium",
                "0.9.2-alpha.4+mc26.2",
            )

            dry_run = fixture.manager.adopt(dry_run=True)
            dry_groups = dry_run["dependency_resolution"][
                "observed_out_of_scope_duplicate_ownership_groups"
            ]
            self.assertEqual(["sodium"], [item["ownership_id"] for item in dry_groups])

            fixture.manager.adopt(dry_run=False)
            verified = fixture.manager.verify()
            verify_groups = verified["fabric_dependency_graph"][
                "observed_out_of_scope_duplicate_ownership_groups"
            ]
            self.assertEqual(dry_groups, verify_groups)
            self.assertEqual("PHYSICAL_STATE_VERIFIED", verified["status"])

    def test_duplicate_external_provider_required_by_managed_consumer_blocks(self) -> None:
        consumer = descriptor(
            "managed-consumer.jar",
            "managed_consumer",
            "1.0.0",
            depends={"fabric-api": (">=0.100.0",)},
            project_id="managed-consumer",
        )
        first = descriptor("fabric-api-first.jar", "fabric-api", "0.120.0+26.2")
        second = descriptor("fabric-api-second.jar", "fabric-api", "0.121.0+26.2")
        with self.assertRaisesRegex(
            ManagerError,
            r"duplicate enabled Fabric ownership fabric-api:.*first.*second",
        ):
            self.resolve(consumer, first, second)

    def test_duplicate_managed_ownership_blocks_even_when_not_a_dependency(self) -> None:
        first = descriptor(
            "managed-first.jar",
            "managed_duplicate",
            "1.0.0",
            project_id="managed-first",
        )
        second = descriptor(
            "managed-second.jar",
            "managed_duplicate",
            "2.0.0",
            project_id="managed-second",
        )
        with self.assertRaisesRegex(
            ManagerError,
            r"duplicate enabled Fabric ownership managed_duplicate:.*managed-first.*managed-second",
        ):
            self.resolve(first, second)

    def test_c57_and_c58_duplicate_provider_fails_closed(self) -> None:
        bge_c57 = descriptor(
            "bge-c57.jar",
            "cnm_terrain_slabs_compat",
            C57,
            provides=("more_slabs_stairs_and_walls",),
            project_id="block-geometry-extensions",
        )
        with self.assertRaisesRegex(ManagerError, "duplicate enabled Fabric ownership cnm_terrain_slabs_compat"):
            self.resolve(bge_c57, self.bge_c58, self.trowel)

    def test_standalone_nibaru_beside_unified_bge_fails_closed(self) -> None:
        standalone = descriptor(
            "nibaru.jar",
            "more_slabs_stairs_and_walls",
            "1.0.0",
        )
        with self.assertRaisesRegex(ManagerError, "duplicate enabled Fabric ownership more_slabs_stairs_and_walls"):
            self.resolve(self.bge_c58, standalone, self.trowel)

    def test_platform_dependency_without_exact_attestation_fails_closed(self) -> None:
        consumer = descriptor(
            "builtin-consumer.jar",
            "builtin_consumer",
            "1.0.0",
            depends={"minecraft": ("=26.2",)},
            project_id="builtin-consumer",
        )
        with self.assertRaisesRegex(
            ManagerError,
            r"missing external Fabric dependency.*minecraft.*no exact enabled provider",
        ):
            self.resolve(consumer)


class DeclaredNestedFabricJarTests(unittest.TestCase):
    @staticmethod
    def raw_control_manifest_jar(primary_id: str) -> bytes:
        manifest = (
            b'{"schemaVersion":1,"id":"'
            + primary_id.encode("ascii")
            + b'","version":"1.0.0","name":"raw control",'
            b'"description":"line one\nline two"}'
        )
        output = io.BytesIO()
        with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as archive:
            archive.writestr("fabric.mod.json", manifest)
        return output.getvalue()

    def write_outer(
        self,
        directory: Path,
        outer_bytes: bytes,
        *,
        primary_id: str = "trinkets_updated",
        version: str = "4.1.0-beta.3+26.2",
    ) -> tuple[Path, ManagedArtifact]:
        path = directory / "managed-outer.jar"
        path.write_bytes(outer_bytes)
        return path, managed_fixture_artifact(path, primary_id, version)

    @staticmethod
    def resolve(
        descriptors: tuple[FabricModDescriptor, ...],
        managed_id: str = "trinkets_updated",
    ) -> dict:
        return PhysicalManager._resolve_dependency_graph(  # type: ignore[arg-type]
            None,
            descriptors,
            managed_ownership_ids={managed_id},
            phase="TEST",
        )

    def test_recursive_nested_provider_resolves_with_exact_provenance_receipt(self) -> None:
        event_bytes = fabric_mod_jar_bytes("yumi_commons_event", "2.0.0")
        event_path = "META-INF/jars/yumi-commons-event-2.0.0.jar"
        core_bytes = fabric_mod_jar_bytes(
            "yumi_mc_core",
            "1.1.1+26.2",
            depends={"yumi_commons_event": "~2.0.0"},
            declared_paths=(event_path,),
            nested_entries=((event_path, event_bytes),),
        )
        core_path = "META-INF/jars/yumi-mc-foundation-1.1.1+26.2.jar"
        outer_bytes = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            depends={"yumi_mc_core": ">=1.1.0+26.2"},
            declared_paths=(core_path,),
            nested_entries=((core_path, core_bytes),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer_bytes)
            descriptors = _read_fabric_descriptor_tree(
                path,
                relative_path="mods/managed-outer.jar",
                artifact=artifact_record,
            )

        self.assertEqual(
            ["trinkets_updated", "yumi_mc_core", "yumi_commons_event"],
            [item.primary_id for item in descriptors],
        )
        self.assertEqual(
            {artifact_record.project_id},
            {item.managed_project_id for item in descriptors},
        )
        self.assertEqual([0, 1, 2], [len(item.nested_chain) for item in descriptors])
        self.assertEqual(hashlib.sha256(outer_bytes).hexdigest(), descriptors[0].root_container_sha256)
        self.assertEqual(hashlib.sha256(core_bytes).hexdigest(), descriptors[1].nested_chain[-1].member_sha256)
        self.assertEqual(hashlib.sha256(event_bytes).hexdigest(), descriptors[2].nested_chain[-1].member_sha256)

        report = self.resolve(descriptors)
        self.assertEqual("FABRIC_DEPENDENCY_GRAPH_VERIFIED", report["status"])
        self.assertEqual(1, report["enabled_fabric_jar_count"])
        self.assertEqual(3, report["enabled_fabric_descriptor_count"])
        self.assertEqual(2, report["declared_nested_fabric_descriptor_count"])
        self.assertEqual(1, len(report["enabled_fabric_jars"]))
        self.assertEqual(3, len(report["enabled_fabric_descriptors"]))
        resolutions = {item["dependency_id"]: item for item in report["resolutions"]}
        self.assertEqual("1.1.1+26.2", resolutions["yumi_mc_core"]["provider"]["version"])
        self.assertEqual("2.0.0", resolutions["yumi_commons_event"]["provider"]["version"])
        self.assertEqual(
            "DECLARED_NESTED_FABRIC_JAR",
            resolutions["yumi_mc_core"]["provider"]["provenance"]["classification"],
        )
        self.assertEqual(
            2,
            resolutions["yumi_commons_event"]["provider"]["provenance"]["nested_depth"],
        )
        json.dumps(report)

    def test_loader_valid_raw_control_character_parses_in_root_and_nested_manifests(self) -> None:
        nested_bytes = self.raw_control_manifest_jar("raw_nested")
        member_path = "META-INF/jars/raw-nested.jar"
        outer_bytes = fabric_mod_jar_bytes(
            "raw_root",
            "1.0.0",
            depends={"raw_nested": "=1.0.0"},
            declared_paths=(member_path,),
            nested_entries=((member_path, nested_bytes),),
        )
        # Exercise the same leniency in the root descriptor as well.
        with zipfile.ZipFile(io.BytesIO(outer_bytes), "r") as source:
            entries = [(entry.filename, source.read(entry)) for entry in source.infolist()]
        root_manifest = json.loads(next(data for name, data in entries if name == "fabric.mod.json"))
        root_manifest["description"] = "line one\nline two"
        rewritten = io.BytesIO()
        with zipfile.ZipFile(rewritten, "w", compression=zipfile.ZIP_STORED) as archive:
            for name, data in entries:
                if name == "fabric.mod.json":
                    data = json.dumps(root_manifest, sort_keys=True).encode("utf-8").replace(
                        b"line one\\nline two", b"line one\nline two"
                    )
                archive.writestr(name, data)

        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(
                Path(temporary), rewritten.getvalue(), primary_id="raw_root", version="1.0.0"
            )
            descriptors = _read_fabric_descriptor_tree(
                path,
                relative_path="mods/raw-root.jar",
                artifact=artifact_record,
            )

        self.assertEqual(["raw_root", "raw_nested"], [item.primary_id for item in descriptors])
        self.assertEqual("1.0.0", self.resolve(descriptors, "raw_root")["resolutions"][0]["provider"]["version"])

    def test_loader_leniency_does_not_accept_structurally_invalid_manifest(self) -> None:
        output = io.BytesIO()
        with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as archive:
            archive.writestr(
                "fabric.mod.json",
                b'{"schemaVersion":1,"id":"broken","version":"1.0.0",}',
            )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(
                Path(temporary), output.getvalue(), primary_id="broken", version="1.0.0"
            )
            with self.assertRaisesRegex(ManagerError, "cannot read Fabric manifest"):
                _read_fabric_descriptor_tree(
                    path,
                    relative_path="mods/broken.jar",
                    artifact=artifact_record,
                )

    def test_default_and_client_environments_are_client_eligible(self) -> None:
        provider = fabric_mod_jar_bytes("nested_provider", "1.0.0")
        member_path = "META-INF/jars/provider.jar"
        cases = (
            ("default", _ENVIRONMENT_UNSET, "*"),
            ("empty", "", "*"),
            ("client", "client", "client"),
            ("uppercase-client", "CLIENT", "client"),
        )
        for label, raw_environment, expected_environment in cases:
            with self.subTest(label=label), tempfile.TemporaryDirectory() as temporary:
                kwargs = {} if raw_environment is _ENVIRONMENT_UNSET else {"environment": raw_environment}
                outer = fabric_mod_jar_bytes(
                    "trinkets_updated",
                    "4.1.0-beta.3+26.2",
                    declared_paths=(member_path,),
                    nested_entries=((member_path, provider),),
                    **kwargs,
                )
                path, artifact_record = self.write_outer(Path(temporary), outer)
                descriptors = _read_fabric_descriptor_tree(path, artifact=artifact_record)
            self.assertEqual(2, len(descriptors))
            self.assertEqual(expected_environment, descriptors[0].environment)
            self.assertTrue(all(item.environment_eligible for item in descriptors))

    def test_server_root_is_receipted_but_does_not_traverse_declared_children(self) -> None:
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=("META-INF/jars/missing.jar",),
            environment="SERVER",
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer)
            descriptors = _read_fabric_descriptor_tree(
                path,
                relative_path="mods/managed-outer.jar",
                artifact=artifact_record,
            )
        self.assertEqual(1, len(descriptors))
        self.assertEqual("server", descriptors[0].environment)
        self.assertFalse(descriptors[0].environment_eligible)

        report = self.resolve(descriptors)
        self.assertEqual(1, report["enabled_fabric_jar_count"])
        self.assertEqual(0, report["enabled_fabric_descriptor_count"])
        self.assertEqual(1, report["environment_excluded_fabric_descriptor_count"])
        excluded = report["environment_excluded_fabric_descriptors"][0]
        self.assertEqual("EXCLUDED_BY_FABRIC_ENVIRONMENT", excluded["classification"])
        self.assertEqual("server", excluded["environment"])
        self.assertEqual("mods/managed-outer.jar", excluded["provenance"]["root_container"]["path"])

    def test_server_nested_candidate_cannot_satisfy_client_dependency_or_traverse_children(self) -> None:
        member_path = "META-INF/jars/server-provider.jar"
        server_provider = fabric_mod_jar_bytes(
            "server_provider",
            "1.0.0",
            declared_paths=("META-INF/jars/missing-grandchild.jar",),
            environment="server",
        )
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=(member_path,),
            nested_entries=((member_path, server_provider),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer)
            descriptors = _read_fabric_descriptor_tree(
                path,
                relative_path="mods/managed-outer.jar",
                artifact=artifact_record,
            )
        self.assertEqual(["trinkets_updated", "server_provider"], [item.primary_id for item in descriptors])
        report = self.resolve(descriptors)
        self.assertEqual(1, report["enabled_fabric_descriptor_count"])
        self.assertEqual(1, report["environment_excluded_fabric_descriptor_count"])
        self.assertEqual(
            "mods/managed-outer.jar!/META-INF/jars/server-provider.jar",
            report["environment_excluded_fabric_descriptors"][0]["path"],
        )

        consumer = descriptor(
            "consumer.jar",
            "managed_consumer",
            "1.0.0",
            depends={"server_provider": ("*",)},
            project_id="managed-consumer",
        )
        with self.assertRaisesRegex(
            ManagerError,
            r"missing external Fabric dependency.*server_provider.*no exact enabled provider",
        ):
            PhysicalManager._resolve_dependency_graph(  # type: ignore[arg-type]
                None,
                (*descriptors, consumer),
                managed_ownership_ids={"trinkets_updated", "managed_consumer"},
                phase="TEST",
            )

    def test_invalid_environment_values_fail_closed(self) -> None:
        for raw_environment in ("dedicated", 7, None):
            with self.subTest(raw_environment=raw_environment), tempfile.TemporaryDirectory() as temporary:
                outer = fabric_mod_jar_bytes(
                    "trinkets_updated",
                    "4.1.0-beta.3+26.2",
                    environment=raw_environment,
                )
                path, artifact_record = self.write_outer(Path(temporary), outer)
                with self.assertRaisesRegex(ManagerError, "fabric.mod.json environment"):
                    _read_fabric_descriptor_tree(path, artifact=artifact_record)

    def test_missing_declared_nested_member_fails_closed(self) -> None:
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=("META-INF/jars/missing.jar",),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer)
            with self.assertRaisesRegex(ManagerError, "declared nested JAR member is missing"):
                _read_fabric_descriptor_tree(path, artifact=artifact_record)

    def test_malformed_declared_nested_member_fails_closed(self) -> None:
        member_path = "META-INF/jars/broken.jar"
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=(member_path,),
            nested_entries=((member_path, b"not a ZIP archive"),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer)
            with self.assertRaisesRegex(ManagerError, "declared nested JAR member is malformed"):
                _read_fabric_descriptor_tree(path, artifact=artifact_record)

    def test_only_a_missing_root_descriptor_is_classified_as_non_fabric(self) -> None:
        non_fabric_output = io.BytesIO()
        with zipfile.ZipFile(non_fabric_output, "w", compression=zipfile.ZIP_STORED) as archive:
            archive.writestr("library.class", b"fixture")
        non_fabric_bytes = non_fabric_output.getvalue()
        member_path = "META-INF/jars/plain-library.jar"
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=(member_path,),
            nested_entries=((member_path, non_fabric_bytes),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            plain_path = directory / "plain.jar"
            plain_path.write_bytes(non_fabric_bytes)
            with self.assertRaises(ManagerError) as root_error:
                _read_fabric_descriptor_tree(plain_path)
            self.assertEqual("_NonFabricRootError", type(root_error.exception).__name__)

            path, artifact_record = self.write_outer(directory, outer)
            with self.assertRaises(ManagerError) as nested_error:
                _read_fabric_descriptor_tree(path, artifact=artifact_record)
            self.assertIs(type(nested_error.exception), ManagerError)
            self.assertIn("root fabric.mod.json", str(nested_error.exception))

    def test_unsafe_or_loader_ignored_nested_paths_fail_closed(self) -> None:
        unsafe_paths = (
            "../escape.jar",
            "nested/../escape.jar",
            "nested\\escape.jar",
            "/absolute.jar",
            "C:drive-relative.jar",
            "META-INF/jars/provider.JAR",
            "META-INF//jars/provider.jar",
        )
        for member_path in unsafe_paths:
            with self.subTest(member_path=member_path), tempfile.TemporaryDirectory() as temporary:
                outer = fabric_mod_jar_bytes(
                    "trinkets_updated",
                    "4.1.0-beta.3+26.2",
                    declared_paths=(member_path,),
                )
                path, artifact_record = self.write_outer(Path(temporary), outer)
                with self.assertRaisesRegex(
                    ManagerError,
                    r"(?:unsafe nested JAR path|safe non-empty ZIP member path)",
                ):
                    _read_fabric_descriptor_tree(path, artifact=artifact_record)

    def test_different_nested_versions_select_loader_higher_candidate(self) -> None:
        first_path = "META-INF/jars/provider-one.jar"
        second_path = "META-INF/jars/provider-two.jar"
        first = fabric_mod_jar_bytes("nested_provider", "1.0.0")
        second = fabric_mod_jar_bytes("nested_provider", "2.0.0")
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=(first_path, second_path),
            nested_entries=((first_path, first), (second_path, second)),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer)
            descriptors = _read_fabric_descriptor_tree(path, artifact=artifact_record)
        report = self.resolve(descriptors)
        selection = report["candidate_selection_groups"][0]
        self.assertEqual("nested_provider", selection["ownership_id"])
        self.assertEqual("2.0.0", selection["selected_candidate"]["full_version"])
        self.assertEqual(
            [("1.0.0", "LOWER_VERSION")],
            [
                (item["full_version"], item["inactive_reason"])
                for item in selection["inactive_alternatives"]
            ],
        )
        self.assertEqual(3, report["discovered_fabric_descriptor_count"])
        self.assertEqual(2, report["selected_fabric_descriptor_count"])

    def test_fabric_api_base_build_variants_select_cr_compass_parent_priority(self) -> None:
        versions = (
            ("cr_compass", "cr-compass.jar", "2.0.4+ece0632333"),
            ("fabric_api", "fabric-api.jar", "2.0.4+ece063239e"),
            ("modmenu", "modmenu.jar", "2.0.4+ece063239c"),
        )
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            discovered: list[FabricModDescriptor] = []
            for parent_id, filename, version in versions:
                member_path = f"META-INF/jars/fabric-api-base-{version}.jar"
                nested = fabric_mod_jar_bytes("fabric_api_base", version)
                outer = fabric_mod_jar_bytes(
                    parent_id,
                    "1.0.0",
                    declared_paths=(member_path,),
                    nested_entries=((member_path, nested),),
                )
                path = directory / filename
                path.write_bytes(outer)
                discovered.extend(
                    _read_fabric_descriptor_tree(
                        path,
                        relative_path="mods/" + filename,
                    )
                )

        consumer = descriptor(
            "managed-consumer.jar",
            "managed_consumer",
            "1.0.0",
            depends={"fabric_api_base": ("=2.0.4",)},
            project_id="managed-consumer",
        )
        report = PhysicalManager._resolve_dependency_graph(  # type: ignore[arg-type]
            None,
            (*discovered, consumer),
            managed_ownership_ids={"managed_consumer"},
            phase="TEST",
        )
        selection = next(
            item
            for item in report["candidate_selection_groups"]
            if item["ownership_id"] == "fabric_api_base"
        )
        selected = selection["selected_candidate"]
        self.assertEqual("2.0.4+ece0632333", selected["full_version"])
        self.assertEqual("cr_compass", selected["priority_parents"][0]["primary_id"])
        self.assertEqual([2, 0, 4], selected["semantic_precedence_key"]["components"])
        self.assertTrue(selected["semantic_precedence_key"]["build_metadata_ignored"])
        self.assertEqual(3, len(selection["discovered_candidates"]))
        self.assertEqual(
            {"2.0.4+ece063239e", "2.0.4+ece063239c"},
            {item["full_version"] for item in selection["inactive_alternatives"]},
        )
        self.assertEqual(
            {"LOWER_PRIORITY_PARENT"},
            {item["inactive_reason"] for item in selection["inactive_alternatives"]},
        )
        resolution = next(
            item for item in report["resolutions"] if item["dependency_id"] == "fabric_api_base"
        )
        self.assertEqual("2.0.4+ece0632333", resolution["provider"]["version"])
        self.assertEqual(
            "mods/cr-compass.jar",
            resolution["provider"]["provenance"]["root_container"]["path"],
        )

    def test_unique_external_root_shadows_higher_nested_candidate(self) -> None:
        nested_version = "9.0.0+nested"
        member_path = "META-INF/jars/fabric-api-base-nested.jar"
        outer = fabric_mod_jar_bytes(
            "external_parent",
            "1.0.0",
            declared_paths=(member_path,),
            nested_entries=((member_path, fabric_mod_jar_bytes("fabric_api_base", nested_version)),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "external-parent.jar"
            path.write_bytes(outer)
            nested_descriptors = _read_fabric_descriptor_tree(
                path,
                relative_path="mods/external-parent.jar",
            )
        direct = descriptor("fabric-api-base-direct.jar", "fabric_api_base", "2.0.4+direct")
        consumer = descriptor(
            "managed-consumer.jar",
            "managed_consumer",
            "1.0.0",
            depends={"fabric_api_base": ("*",)},
            project_id="managed-consumer",
        )
        report = PhysicalManager._resolve_dependency_graph(  # type: ignore[arg-type]
            None,
            (*nested_descriptors, direct, consumer),
            managed_ownership_ids={"managed_consumer"},
            phase="TEST",
        )
        selection = report["candidate_selection_groups"][0]
        self.assertEqual("2.0.4+direct", selection["selected_candidate"]["full_version"])
        self.assertTrue(selection["selected_candidate"]["root_candidate"])
        self.assertEqual(
            [(nested_version, "ROOT_SHADOWED_NESTED")],
            [
                (item["full_version"], item["inactive_reason"])
                for item in selection["inactive_alternatives"]
            ],
        )

    def test_equal_priority_nested_candidates_with_distinct_tied_roots_fail_closed(self) -> None:
        variants = (
            ("first-parent.jar", "2.0.4+build-a"),
            ("second-parent.jar", "2.0.4+build-b"),
        )
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            discovered: list[FabricModDescriptor] = []
            for filename, version in variants:
                member_path = f"META-INF/jars/base-{version}.jar"
                outer = fabric_mod_jar_bytes(
                    "tied_parent",
                    "1.0.0",
                    declared_paths=(member_path,),
                    nested_entries=((member_path, fabric_mod_jar_bytes("fabric_api_base", version)),),
                    payload=filename.encode("utf-8"),
                )
                path = directory / filename
                path.write_bytes(outer)
                discovered.extend(
                    _read_fabric_descriptor_tree(path, relative_path="mods/" + filename)
                )
        consumer = descriptor(
            "managed-consumer.jar",
            "managed_consumer",
            "1.0.0",
            depends={"fabric_api_base": ("=2.0.4",)},
            project_id="managed-consumer",
        )
        with self.assertRaisesRegex(
            ManagerError,
            r"ambiguous equal-priority external nested Fabric candidates.*fabric_api_base.*build metadata",
        ):
            PhysicalManager._resolve_dependency_graph(  # type: ignore[arg-type]
                None,
                (*discovered, consumer),
                managed_ownership_ids={"managed_consumer"},
                phase="TEST",
            )

    def test_byte_identical_nested_candidate_is_deduplicated_with_all_chains(self) -> None:
        first_path = "META-INF/jars/provider-one.jar"
        second_path = "META-INF/jars/provider-two.jar"
        provider = fabric_mod_jar_bytes("nested_provider", "1.0.0")
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=(first_path, second_path),
            nested_entries=((first_path, provider), (second_path, provider)),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer)
            descriptors = _read_fabric_descriptor_tree(path, artifact=artifact_record)
        self.assertEqual(2, len(descriptors))
        nested = descriptors[1]
        self.assertEqual(1, len(nested.alternate_nested_chains))
        provenance = nested.provenance_record()
        self.assertEqual(2, len(provenance["nested_chains"]))
        self.resolve(descriptors)

    def test_identical_nested_bytes_across_managed_roots_fail_with_all_origins(self) -> None:
        member_path = "META-INF/jars/shared-provider.jar"
        provider = fabric_mod_jar_bytes("nested_provider", "1.0.0")
        first_outer = fabric_mod_jar_bytes(
            "managed_first",
            "1.0.0",
            declared_paths=(member_path,),
            nested_entries=((member_path, provider),),
        )
        second_outer = fabric_mod_jar_bytes(
            "managed_second",
            "1.0.0",
            declared_paths=(member_path,),
            nested_entries=((member_path, provider),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            first_path = directory / "first.jar"
            second_path = directory / "second.jar"
            first_path.write_bytes(first_outer)
            second_path.write_bytes(second_outer)
            first_descriptors = _read_fabric_descriptor_tree(
                first_path,
                relative_path="mods/first.jar",
                artifact=managed_fixture_artifact(first_path, "managed_first", "1.0.0"),
            )
            second_descriptors = _read_fabric_descriptor_tree(
                second_path,
                relative_path="mods/second.jar",
                artifact=managed_fixture_artifact(second_path, "managed_second", "1.0.0"),
            )
        with self.assertRaisesRegex(
            ManagerError,
            "ambiguous managed root ownership",
        ) as captured:
            PhysicalManager._resolve_dependency_graph(  # type: ignore[arg-type]
                None,
                (*first_descriptors, *second_descriptors),
                managed_ownership_ids={"managed_first", "managed_second"},
                phase="TEST",
            )
        message = str(captured.exception)
        self.assertIn("mods/first.jar!/META-INF/jars/shared-provider.jar", message)
        self.assertIn("mods/second.jar!/META-INF/jars/shared-provider.jar", message)
        self.assertLess(message.index("mods/first.jar"), message.index("mods/second.jar"))

    def test_identical_nested_bytes_across_external_roots_are_one_provider_with_all_origins(self) -> None:
        member_path = "META-INF/jars/shared-provider.jar"
        provider = fabric_mod_jar_bytes("nested_provider", "1.0.0")
        first_outer = fabric_mod_jar_bytes(
            "external_first",
            "1.0.0",
            declared_paths=(member_path,),
            nested_entries=((member_path, provider),),
        )
        second_outer = fabric_mod_jar_bytes(
            "external_second",
            "1.0.0",
            declared_paths=(member_path,),
            nested_entries=((member_path, provider),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            first_path = directory / "first.jar"
            second_path = directory / "second.jar"
            first_path.write_bytes(first_outer)
            second_path.write_bytes(second_outer)
            first_descriptors = _read_fabric_descriptor_tree(
                first_path,
                relative_path="mods/first.jar",
            )
            second_descriptors = _read_fabric_descriptor_tree(
                second_path,
                relative_path="mods/second.jar",
            )
        consumer = descriptor(
            "consumer.jar",
            "managed_consumer",
            "1.0.0",
            depends={"nested_provider": ("=1.0.0",)},
            project_id="managed-consumer",
        )
        report = PhysicalManager._resolve_dependency_graph(  # type: ignore[arg-type]
            None,
            (*first_descriptors, *second_descriptors, consumer),
            managed_ownership_ids={"managed_consumer"},
            phase="TEST",
        )
        resolution = next(item for item in report["resolutions"] if item["dependency_id"] == "nested_provider")
        self.assertEqual("RESOLVED_EXTERNAL_ENABLED_PROVIDER", resolution["classification"])
        origins = resolution["provider"]["provenance"]["origins"]
        self.assertEqual(2, len(origins))
        self.assertEqual(
            ["mods/first.jar", "mods/second.jar"],
            [item["root_container"]["path"] for item in origins],
        )

    def test_duplicate_declaration_and_duplicate_archive_path_fail_closed(self) -> None:
        member_path = "META-INF/jars/provider.jar"
        provider = fabric_mod_jar_bytes("nested_provider", "1.0.0")
        duplicate_declaration = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=(member_path, member_path),
            nested_entries=((member_path, provider),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), duplicate_declaration)
            with self.assertRaisesRegex(ManagerError, "duplicate declared nested JAR path"):
                _read_fabric_descriptor_tree(path, artifact=artifact_record)

        output = io.BytesIO()
        manifest = {
            "schemaVersion": 1,
            "id": "trinkets_updated",
            "version": "4.1.0-beta.3+26.2",
            "jars": [{"file": member_path}],
        }
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as archive:
                archive.writestr("fabric.mod.json", json.dumps(manifest))
                archive.writestr(member_path, provider)
                archive.writestr(member_path, provider)
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), output.getvalue())
            with self.assertRaisesRegex(ManagerError, "duplicate ZIP entry path"):
                _read_fabric_descriptor_tree(path, artifact=artifact_record)

    def test_nested_depth_and_size_guards_fail_closed(self) -> None:
        child = fabric_mod_jar_bytes("depth-five", "1.0.0")
        for depth in range(4, 0, -1):
            member_path = f"META-INF/jars/depth-{depth + 1}.jar"
            child = fabric_mod_jar_bytes(
                f"depth-{depth}",
                "1.0.0",
                declared_paths=(member_path,),
                nested_entries=((member_path, child),),
            )
        root_member = "META-INF/jars/depth-1.jar"
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=(root_member,),
            nested_entries=((root_member, child),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer)
            with self.assertRaisesRegex(ManagerError, "nested JAR depth exceeds"):
                _read_fabric_descriptor_tree(path, artifact=artifact_record)

        provider = fabric_mod_jar_bytes("large-provider", "1.0.0", payload=b"x" * 256)
        member_path = "META-INF/jars/large-provider.jar"
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=(member_path,),
            nested_entries=((member_path, provider),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer)
            with patch(
                "tools.test_instance_manager.manager._MAX_NESTED_JAR_ENTRY_SIZE",
                len(provider) - 1,
            ):
                with self.assertRaisesRegex(ManagerError, "declared nested JAR member exceeds"):
                    _read_fabric_descriptor_tree(path, artifact=artifact_record)

    def test_nonregular_nested_member_and_nested_platform_claim_fail_closed(self) -> None:
        member_path = "META-INF/jars/link.jar"
        provider = fabric_mod_jar_bytes("nested_provider", "1.0.0")
        output = io.BytesIO()
        manifest = {
            "schemaVersion": 1,
            "id": "trinkets_updated",
            "version": "4.1.0-beta.3+26.2",
            "jars": [{"file": member_path}],
        }
        with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as archive:
            archive.writestr("fabric.mod.json", json.dumps(manifest))
            link_entry = zipfile.ZipInfo(member_path)
            link_entry.create_system = 3
            link_entry.external_attr = (stat.S_IFLNK | 0o777) << 16
            archive.writestr(link_entry, provider)
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), output.getvalue())
            with self.assertRaisesRegex(ManagerError, "not a regular file"):
                _read_fabric_descriptor_tree(path, artifact=artifact_record)

        platform_provider = fabric_mod_jar_bytes("minecraft", "26.2")
        platform_path = "META-INF/jars/fake-platform.jar"
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=(platform_path,),
            nested_entries=((platform_path, platform_provider),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer)
            with self.assertRaisesRegex(ManagerError, "cannot claim Fabric platform IDs"):
                _read_fabric_descriptor_tree(path, artifact=artifact_record)

    def test_raw_zip_entry_names_fail_closed_before_lookup(self) -> None:
        root_bytes = fabric_mod_jar_bytes("trinkets_updated", "4.1.0-beta.3+26.2")
        mutations = (
            ("orig-name mismatch", lambda item: setattr(item, "orig_filename", "other-name")),
            ("NUL", lambda item: setattr(item, "filename", item.filename + "\x00shadow")),
        )
        for label, mutate in mutations:
            with self.subTest(label=label), zipfile.ZipFile(io.BytesIO(root_bytes)) as archive:
                mutate(archive.infolist()[0])
                with self.assertRaisesRegex(ManagerError, "unsafe or normalized raw name"):
                    _read_fabric_manifest_from_archive(
                        archive,
                        label,
                        budget=_NestedJarTraversalBudget(),
                    )

        with zipfile.ZipFile(io.BytesIO(root_bytes)) as archive:
            entry = archive.getinfo("fixture.bin")
            entry.orig_filename = "META-INF\\jars\\shadow.jar"
            entry.filename = entry.orig_filename
            with self.assertRaisesRegex(ManagerError, "unsafe or normalized raw name"):
                _read_fabric_manifest_from_archive(
                    archive,
                    "backslash fixture",
                    budget=_NestedJarTraversalBudget(),
                )

    def test_manifest_and_nested_member_compressed_size_caps_fail_closed(self) -> None:
        root_bytes = fabric_mod_jar_bytes("trinkets_updated", "4.1.0-beta.3+26.2")
        with zipfile.ZipFile(io.BytesIO(root_bytes)) as archive:
            manifest_compressed_size = archive.getinfo("fabric.mod.json").compress_size
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), root_bytes)
            with patch(
                "tools.test_instance_manager.manager._MAX_FABRIC_MANIFEST_COMPRESSED_SIZE",
                manifest_compressed_size - 1,
            ):
                with self.assertRaisesRegex(ManagerError, "ZIP member compressed size exceeds"):
                    _read_fabric_descriptor_tree(path, artifact=artifact_record)

        provider = fabric_mod_jar_bytes("nested_provider", "1.0.0")
        member_path = "META-INF/jars/provider.jar"
        outer = fabric_mod_jar_bytes(
            "trinkets_updated",
            "4.1.0-beta.3+26.2",
            declared_paths=(member_path,),
            nested_entries=((member_path, provider),),
        )
        with tempfile.TemporaryDirectory() as temporary:
            path, artifact_record = self.write_outer(Path(temporary), outer)
            with patch(
                "tools.test_instance_manager.manager._MAX_NESTED_JAR_ENTRY_COMPRESSED_SIZE",
                len(provider) - 1,
            ):
                with self.assertRaisesRegex(ManagerError, "ZIP member compressed size exceeds"):
                    _read_fabric_descriptor_tree(path, artifact=artifact_record)

    def test_shared_budget_spans_root_manifests_and_nested_counts(self) -> None:
        first = fabric_mod_jar_bytes("managed_first", "1.0.0")
        second = fabric_mod_jar_bytes("managed_second", "1.0.0")
        with zipfile.ZipFile(io.BytesIO(first)) as archive:
            first_manifest = archive.getinfo("fabric.mod.json")
            first_expanded = first_manifest.file_size
            first_compressed = first_manifest.compress_size
        with zipfile.ZipFile(io.BytesIO(second)) as archive:
            second_manifest = archive.getinfo("fabric.mod.json")
            second_expanded = second_manifest.file_size
            second_compressed = second_manifest.compress_size
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            first_path = directory / "first.jar"
            second_path = directory / "second.jar"
            first_path.write_bytes(first)
            second_path.write_bytes(second)
            aggregate_cases = (
                (
                    "expanded",
                    "tools.test_instance_manager.manager._MAX_NESTED_JAR_AGGREGATE_SIZE",
                    first_expanded + second_expanded - 1,
                    "aggregate expanded size",
                ),
                (
                    "compressed",
                    "tools.test_instance_manager.manager._MAX_NESTED_JAR_AGGREGATE_COMPRESSED_SIZE",
                    first_compressed + second_compressed - 1,
                    "aggregate compressed size",
                ),
            )
            for label, constant, limit, message in aggregate_cases:
                with self.subTest(label=label), patch(constant, limit):
                    budget = _NestedJarTraversalBudget()
                    _read_fabric_descriptor_tree(first_path, budget=budget)
                    with self.assertRaisesRegex(ManagerError, message):
                        _read_fabric_descriptor_tree(second_path, budget=budget)

        provider = fabric_mod_jar_bytes("nested_provider", "1.0.0")
        member_path = "META-INF/jars/provider.jar"
        outers = (
            fabric_mod_jar_bytes(
                "managed_first",
                "1.0.0",
                declared_paths=(member_path,),
                nested_entries=((member_path, provider),),
            ),
            fabric_mod_jar_bytes(
                "managed_second",
                "1.0.0",
                declared_paths=(member_path,),
                nested_entries=((member_path, provider),),
            ),
        )
        with tempfile.TemporaryDirectory() as temporary:
            paths = []
            for index, outer in enumerate(outers):
                path = Path(temporary) / f"root-{index}.jar"
                path.write_bytes(outer)
                paths.append(path)
            with patch("tools.test_instance_manager.manager._MAX_NESTED_JAR_COUNT", 1):
                budget = _NestedJarTraversalBudget()
                _read_fabric_descriptor_tree(paths[0], budget=budget)
                with self.assertRaisesRegex(ManagerError, "count exceeds 1 across the enabled graph"):
                    _read_fabric_descriptor_tree(paths[1], budget=budget)


class FabricPlatformAttestationTests(unittest.TestCase):
    def fixture(self, temporary: str) -> ManagerFixture:
        profiles = Path(temporary) / "ModrinthApp" / "profiles"
        profiles.mkdir(parents=True)
        return ManagerFixture(profiles)

    def test_exact_modrinth_authority_resolves_all_builtins_and_receipts_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = self.fixture(temporary)
            authority = install_fake_modrinth_launch_authority(fixture)
            before = tree_snapshot(authority["app_root"])
            attestation = fixture.manager._attest_platform_providers(
                {"minecraft", "fabricloader", "java"}
            )
            self.assertIsNotNone(attestation)
            self.assertEqual(before, tree_snapshot(authority["app_root"]))
            self.assertEqual(
                [authority["probe_java"]],
                authority["probe_calls"],
            )
            self.assertEqual(
                {"minecraft": "26.2", "fabricloader": "0.19.3", "java": "25"},
                {item.mod_id: item.version for item in attestation.providers},
            )
            self.assertTrue(
                attestation.evidence["target"]["instance_id"].startswith("hex:")
            )
            self.assertEqual(
                64,
                len(attestation.evidence["java"]["selected_executable_sha256"]),
            )

            consumer = descriptor(
                "builtin-consumer.jar",
                "builtin_consumer",
                "1.0.0",
                depends={
                    "minecraft": ("=26.2",),
                    "fabricloader": (">=0.19.0 <0.20.0-",),
                    "java": (">=25",),
                },
                project_id="builtin-consumer",
            )
            report = fixture.manager._resolve_dependency_graph(
                (consumer,),
                managed_ownership_ids={"builtin_consumer"},
                phase="TEST",
                platform_attestation=attestation,
            )
            self.assertEqual(
                {"fabricloader", "java", "minecraft"},
                {item["dependency_id"] for item in report["resolutions"]},
            )
            self.assertTrue(
                all(
                    item["classification"] == "RESOLVED_ATTESTED_PLATFORM_PROVIDER"
                    and item["satisfied"]
                    and item["matched_predicates"]
                    for item in report["resolutions"]
                )
            )
            self.assertEqual(
                attestation.fingerprint,
                report["platform_attestation"]["fingerprint"],
            )
            json.dumps(report)

    def test_builtin_predicate_mismatch_and_duplicate_jar_ownership_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = self.fixture(temporary)
            install_fake_modrinth_launch_authority(fixture)
            attestation = fixture.manager._attest_platform_providers({"minecraft"})
            incompatible = descriptor(
                "incompatible.jar",
                "incompatible_consumer",
                "1.0.0",
                depends={"minecraft": ("=26.1",)},
                project_id="incompatible-consumer",
            )
            with self.assertRaisesRegex(
                ManagerError,
                r"minecraft \['=26\.1'\].*minecraft@26\.2.*dedicated-profile launch provider",
            ):
                fixture.manager._resolve_dependency_graph(
                    (incompatible,),
                    managed_ownership_ids={"incompatible_consumer"},
                    phase="TEST",
                    platform_attestation=attestation,
                )

            forged_builtin = descriptor(
                "forged-minecraft.jar",
                "minecraft",
                "26.2",
                project_id="forged-minecraft",
            )
            with self.assertRaisesRegex(
                ManagerError,
                "duplicate enabled Fabric ownership minecraft: Fabric builtin provider",
            ):
                fixture.manager._resolve_dependency_graph(
                    (forged_builtin,),
                    managed_ownership_ids={"minecraft"},
                    phase="TEST",
                    platform_attestation=attestation,
                )

    def test_integrated_dry_run_apply_and_verify_reattest_exact_platform_versions(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = self.fixture(temporary)
            authority = install_fake_modrinth_launch_authority(fixture)
            fixture.manager.adopt(dry_run=False)
            _candidate, operation = platform_candidate_operation(fixture)
            before = fixture.repository_state()

            dry_run = fixture.manager.transition(
                operation=operation,
                expected_revision=before["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )
            platform_resolutions = [
                item
                for item in dry_run["dependency_resolution"]["resolutions"]
                if item["classification"] == "RESOLVED_ATTESTED_PLATFORM_PROVIDER"
            ]
            self.assertEqual(3, len(platform_resolutions))
            self.assertEqual(
                {"minecraft": "26.2", "fabricloader": "0.19.3", "java": "25"},
                {
                    item["dependency_id"]: item["provider"]["version"]
                    for item in platform_resolutions
                },
            )

            applied = fixture.manager.transition(
                operation=operation,
                expected_revision=before["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=False,
            )
            verified = fixture.manager.verify()
            self.assertEqual("FABRIC_PLATFORM_PROVIDERS_ATTESTED", applied["dependency_resolution"]["platform_attestation"]["status"])
            self.assertEqual("PHYSICAL_STATE_VERIFIED", verified["status"])
            self.assertEqual(
                "FABRIC_PLATFORM_PROVIDERS_ATTESTED",
                verified["fabric_dependency_graph"]["platform_attestation"]["status"],
            )
            self.assertTrue(authority["probe_calls"])
            self.assertTrue(all(path == authority["probe_java"] for path in authority["probe_calls"]))

    def test_launch_authority_mismatches_fail_closed(self) -> None:
        cases = (
            (
                "stale content set",
                {"content_set_status": "stale"},
                "applied content set is not available",
            ),
            (
                "unresolved loader alias",
                {"loader_version": "latest"},
                "unresolved alias",
            ),
            (
                "cached metadata id drift",
                {"metadata_id": "wrong-id"},
                "metadata identity",
            ),
            (
                "cached loader coordinate drift",
                {"metadata_loader_coordinates": ["net.fabricmc:fabric-loader:0.19.2"]},
                "cached Fabric Loader coordinate",
            ),
            (
                "selected java full version drift",
                {"probed_java_version": "25.0.3"},
                "configured Java version identity",
            ),
            (
                "java major drift",
                {"probed_java_specification": "24"},
                "disagrees with required major 25",
            ),
            (
                "unsafe game version authority",
                {"game_version": "release/candidate"},
                "not an exact release semantic version",
            ),
        )
        for label, options, pattern in cases:
            with self.subTest(label=label), tempfile.TemporaryDirectory() as temporary:
                fixture = self.fixture(temporary)
                authority = install_fake_modrinth_launch_authority(fixture, **options)
                before = tree_snapshot(authority["app_root"])
                with self.assertRaisesRegex(ManagerError, pattern):
                    fixture.manager._attest_platform_providers({"minecraft"})
                self.assertEqual(before, tree_snapshot(authority["app_root"]))

    def test_exact_target_row_must_be_unique(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = self.fixture(temporary)
            authority = install_fake_modrinth_launch_authority(fixture)
            with closing(sqlite3.connect(authority["database"])) as connection:
                duplicate_instance = bytes.fromhex("33333333333333333333333333333333")
                duplicate_set = bytes.fromhex("44444444444444444444444444444444")
                connection.execute(
                    "INSERT INTO instances VALUES (?, ?, ?, ?)",
                    (duplicate_instance, fixture.target.name, "installed", duplicate_set),
                )
                connection.execute(
                    "INSERT INTO instance_content_sets VALUES (?, ?, ?, ?, ?, ?, ?)",
                    (
                        duplicate_set,
                        duplicate_instance,
                        "available",
                        "26.2",
                        "fabric",
                        "0.19.3",
                        "2099-01-01T00:00:00Z",
                    ),
                )
                connection.commit()
            with self.assertRaisesRegex(ManagerError, "matched 2 applied content sets"):
                fixture.manager._attest_platform_providers({"java"})

    def test_arbitrary_launch_override_executable_is_never_probed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = self.fixture(temporary)
            authority = install_fake_modrinth_launch_authority(fixture)
            arbitrary = authority["app_root"] / "meta" / "java_versions" / "fixture-java" / "bin" / "launcher.exe"
            arbitrary.write_bytes(b"not a Java launcher")
            set_fake_java_override(authority, arbitrary)

            with self.assertRaisesRegex(ManagerError, "selected Java executable must be"):
                fixture.manager._attest_platform_providers({"java"})
            self.assertEqual([], authority["probe_calls"])

    def test_protected_profile_launch_override_is_rejected_before_access_or_probe(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = self.fixture(temporary)
            authority = install_fake_modrinth_launch_authority(fixture)
            protected_before = tree_snapshot(fixture.protected)
            protected_java = fixture.protected / "runtime" / "bin" / ("java.exe" if os.name == "nt" else "java")
            set_fake_java_override(authority, protected_java)

            with self.assertRaisesRegex(ManagerError, "protected gameplay profile"):
                fixture.manager._attest_platform_providers({"java"})
            self.assertEqual([], authority["probe_calls"])
            self.assertEqual(protected_before, tree_snapshot(fixture.protected))

    def test_reparse_traversal_launch_override_is_never_probed_when_supported(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = self.fixture(temporary)
            authority = install_fake_modrinth_launch_authority(fixture)
            linked_home = authority["app_root"] / "linked-java"
            try:
                linked_home.symlink_to(authority["java_home"], target_is_directory=True)
            except (NotImplementedError, OSError) as exc:
                self.skipTest(f"directory symlinks are unavailable: {exc}")
            set_fake_java_override(
                authority,
                linked_home / "bin" / ("java.exe" if os.name == "nt" else "java"),
            )

            with self.assertRaisesRegex(ManagerError, "symlink, junction, or reparse point"):
                fixture.manager._attest_platform_providers({"java"})
            self.assertEqual([], authority["probe_calls"])

    def test_under_lock_launch_authority_drift_fails_before_transaction(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = self.fixture(temporary)
            install_fake_modrinth_launch_authority(fixture)
            fixture.manager.adopt(dry_run=False)
            _candidate, operation = platform_candidate_operation(fixture)
            baseline = fixture.manager._attest_platform_providers(
                {"minecraft", "fabricloader", "java"}
            )
            drifted = PlatformAttestation(
                providers=baseline.providers,
                evidence={**baseline.evidence, "fixture_drift": True},
                fingerprint="f" * 64,
            )
            before_state = fixture.repository_state()
            before_tree = tree_snapshot(fixture.root)
            stages: list[str] = []
            with patch.object(
                fixture.manager,
                "_attest_platform_providers",
                side_effect=(baseline, drifted),
            ), self.assertRaisesRegex(ManagerError, "changed between planning and the under-lock pre-mutation check"):
                fixture.manager.transition(
                    operation=operation,
                    expected_revision=before_state["revision"],
                    at="2099-01-01T00:00:30Z",
                    dry_run=False,
                    failure_injector=stages.append,
                )
            self.assertEqual([], stages)
            self.assertEqual(before_state, fixture.repository_state())
            self.assertEqual(before_tree, tree_snapshot(fixture.root))

    def test_post_write_launch_authority_drift_rolls_back_exact_profile_preimage(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = self.fixture(temporary)
            authority = install_fake_modrinth_launch_authority(fixture)
            fixture.manager.adopt(dry_run=False)
            candidate, operation = platform_candidate_operation(fixture)
            before_state = fixture.repository_state()
            before_tree = tree_snapshot(fixture.root)
            reached_post_write = False

            def drift_after_physical_apply(stage: str) -> None:
                nonlocal reached_post_write
                if stage != "after_physical_apply":
                    return
                reached_post_write = True
                metadata_path = authority["metadata_path"]
                metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
                metadata["fixture_nonsemantic_drift"] = True
                metadata_path.write_text(json.dumps(metadata, sort_keys=True), encoding="utf-8")

            with self.assertRaisesRegex(ManagerError, "changed.*post-write verification"):
                fixture.manager.transition(
                    operation=operation,
                    expected_revision=before_state["revision"],
                    at="2099-01-01T00:00:30Z",
                    dry_run=False,
                    failure_injector=drift_after_physical_apply,
                )
            self.assertTrue(reached_post_write)
            self.assertEqual(before_state, fixture.repository_state())
            self.assertEqual(before_tree, tree_snapshot(fixture.root))
            self.assertFalse((fixture.mods / candidate["artifacts"][0]["filename"]).exists())

    def test_final_commit_boundary_launch_authority_drift_rolls_back_exact_preimage(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            fixture = self.fixture(temporary)
            authority = install_fake_modrinth_launch_authority(fixture)
            fixture.manager.adopt(dry_run=False)
            candidate, operation = platform_candidate_operation(fixture)
            before_state = fixture.repository_state()
            before_tree = tree_snapshot(fixture.root)
            reached_final_boundary = False

            def drift_after_post_verify(stage: str) -> None:
                nonlocal reached_final_boundary
                if stage != "after_post_verify":
                    return
                reached_final_boundary = True
                metadata_path = authority["metadata_path"]
                metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
                metadata["fixture_final_boundary_drift"] = True
                metadata_path.write_text(json.dumps(metadata, sort_keys=True), encoding="utf-8")

            with self.assertRaisesRegex(ManagerError, "changed.*final post-verification commit boundary"):
                fixture.manager.transition(
                    operation=operation,
                    expected_revision=before_state["revision"],
                    at="2099-01-01T00:00:30Z",
                    dry_run=False,
                    failure_injector=drift_after_post_verify,
                )
            self.assertTrue(reached_final_boundary)
            self.assertEqual(before_state, fixture.repository_state())
            self.assertEqual(before_tree, tree_snapshot(fixture.root))
            self.assertFalse((fixture.mods / candidate["artifacts"][0]["filename"]).exists())


class DependencyCohortTransitionTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.fixture = ManagerFixture(Path(self.temp.name))
        self.bge_uuid = stable_uuid("project:block-geometry-extensions")
        self.trowel_uuid = stable_uuid("project:shulker-trowel")
        self.fixture.project_index.update(
            {
                self.bge_uuid: "block-geometry-extensions",
                self.trowel_uuid: "shulker-trowel",
            }
        )
        self.fixture.project_display_names.update(
            {
                self.bge_uuid: "Block Geometry Extensions",
                self.trowel_uuid: "Shulker Trowel",
            }
        )
        self.fixture.manager.project_index = copy.deepcopy(self.fixture.project_index)
        self.fixture.manager.project_display_names = copy.deepcopy(self.fixture.project_display_names)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def candidate(
        self,
        project_id: str,
        project_uuid: str,
        primary_id: str,
        version: str,
        *,
        provides: tuple[str, ...] = (),
        depends: dict[str, str | list[str]] | None = None,
    ) -> dict:
        filename = f"{project_id}-{version}.jar"
        source = self.fixture.repository / "artifacts" / filename
        sha256 = write_dependency_mod(
            source,
            primary_id,
            version,
            provides=provides,
            depends=depends,
        )
        return unit(
            project_id,
            version,
            artifact(
                filename,
                primary_id,
                sha256,
                ownership_mod_ids=[primary_id, *provides],
                source_type="REPOSITORY",
                source_path="artifacts/" + filename,
            ),
            project_uuid=project_uuid,
        )

    def provider(self, version: str) -> dict:
        return self.candidate(
            "block-geometry-extensions",
            self.bge_uuid,
            "cnm_terrain_slabs_compat",
            version,
            provides=("more_slabs_stairs_and_walls",),
        )

    def trowel(self, version: str, predicate: str) -> dict:
        return self.candidate(
            "shulker-trowel",
            self.trowel_uuid,
            "shulker_trowel",
            version,
            depends={"cnm_terrain_slabs_compat": predicate},
        )

    @staticmethod
    def deploy_operation(provider: dict, companion: dict) -> dict:
        return {
            "type": "DEPLOY_PROFILE",
            "slots": {
                "A": {"members": [candidate_declaration(provider), candidate_declaration(companion)]},
                "B": None,
            },
        }

    def test_exact_c57_pin_rejects_c58_in_real_dry_run_without_mutation(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c58 = self.provider(C58)
        operation = self.deploy_operation(
            c58,
            self.trowel("0.1.0-canary8", "=" + C57),
        )
        before_state = self.fixture.repository_state()
        before_tree = tree_snapshot(self.fixture.root)

        with self.assertRaisesRegex(
            ManagerError,
            r"shulker-trowel@0\.1\.0-canary8.*cnm_terrain_slabs_compat.*same atomic cohort",
        ):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=before_state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )

        self.assertEqual(before_state, self.fixture.repository_state())
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))
        self.assertFalse((self.fixture.mods / c58["artifacts"][0]["filename"]).exists())

    def test_bounded_companion_allows_atomic_later_provider_update(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c58 = self.provider(C58)
        companion = self.trowel("0.1.0-canary9", TROWEL_RANGE)
        state = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=self.deploy_operation(c58, companion),
            expected_revision=state["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        later = self.provider(COMPATIBLE_LATER)
        state = self.fixture.repository_state()
        before_companion = copy.deepcopy(state["slots"]["A"]["members"][1])
        result = self.fixture.manager.transition(
            operation={
                "type": "UPDATE_SLOT",
                "slot": "A",
                "candidate": candidate_declaration(later),
            },
            expected_revision=state["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=False,
        )

        after = self.fixture.repository_state()
        self.assertEqual(COMPATIBLE_LATER, after["slots"]["A"]["members"][0]["unit"]["version"])
        self.assertEqual(before_companion, after["slots"]["A"]["members"][1])
        resolution = next(
            item
            for item in result["dependency_resolution"]["resolutions"]
            if item["consumer"]["project_id"] == "shulker-trowel"
        )
        self.assertEqual(COMPATIBLE_LATER, resolution["provider"]["version"])
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

    def test_upper_bound_provider_update_fails_without_mutation_unless_companion_is_replaced(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c58 = self.provider(C58)
        companion = self.trowel("0.1.0-canary9", TROWEL_RANGE)
        state = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=self.deploy_operation(c58, companion),
            expected_revision=state["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        breaking = self.provider("4.3.0-bge.canary1.breaking+26.2")
        state = self.fixture.repository_state()
        before_tree = tree_snapshot(self.fixture.root)
        with self.assertRaisesRegex(ManagerError, r"shulker-trowel@0\.1\.0-canary9.*does not satisfy"):
            self.fixture.manager.transition(
                operation={
                    "type": "UPDATE_SLOT",
                    "slot": "A",
                    "candidate": candidate_declaration(breaking),
                },
                expected_revision=state["revision"],
                at="2099-01-01T00:00:31Z",
                dry_run=True,
            )
        self.assertEqual(state, self.fixture.repository_state())
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))

        compatible = self.trowel("0.1.0-canary10", ">=4.3.0- <5.0.0-")
        dry_run = self.fixture.manager.transition(
            operation={
                "type": "UPDATE_SLOT",
                "slot": "A",
                "members": [candidate_declaration(breaking), candidate_declaration(compatible)],
            },
            expected_revision=state["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=True,
        )
        self.assertEqual("FABRIC_DEPENDENCY_GRAPH_VERIFIED", dry_run["dependency_resolution"]["status"])


class AcceptedCompanionPassthroughTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.qsn = AcceptedCompanionFixture(Path(self.temp.name))
        self.fixture = self.qsn.fixture

    def tearDown(self) -> None:
        self.temp.cleanup()

    def test_accepted_qsn_artifact_can_regress_with_required_upstream_passthrough(self) -> None:
        before = self.fixture.repository_state()
        before_a = copy.deepcopy(before["slots"]["A"])
        before_tree = tree_snapshot(self.fixture.root)
        operation = self.qsn.operation()

        dry_run = self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=True,
        )
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))
        after_b = dry_run["slot_changes"]["B"]["after"]["members"][0]
        self.assertEqual(
            [self.qsn.core_artifact],
            after_b["accepted_companion_artifacts"],
        )
        projected_b = dry_run["title_projection"]["slots"]["B"]
        self.assertEqual(
            [self.qsn.core_artifact],
            projected_b["accepted_companion_passthrough"]["artifacts"],
        )
        self.assertNotIn("quick-stack-nearby-0.4.0", projected_b["line"])
        passthrough = dry_run["dependency_resolution"][
            "accepted_companion_passthrough"
        ]
        self.assertEqual("ACCEPTED_COMPANION_PASSTHROUGH_VERIFIED", passthrough["status"])
        self.assertEqual(1, passthrough["companion_count"])
        self.assertEqual(
            self.qsn.core_artifact["artifact_id"],
            passthrough["companions"][0]["artifact"]["artifact_id"],
        )

        self.qsn.deploy()
        deployed = self.fixture.repository_state()
        self.assertEqual(before_a, deployed["slots"]["A"])
        self.assertEqual(
            "UNTESTED",
            deployed["slots"]["B"]["members"][0]["runtime_result"][
                "classification"
            ],
        )
        self.assertEqual(
            self.qsn.core_bytes,
            (self.fixture.mods / self.qsn.core_name).read_bytes(),
        )
        self.assertEqual(
            self.qsn.compat_bytes,
            (self.fixture.mods / self.qsn.compat_name).read_bytes(),
        )
        self.assertEqual(
            self.qsn.compat_bytes,
            (self.fixture.mods / (self.qsn.compat_name + ".disabled")).read_bytes(),
        )

        receipt = self.fixture.manager.verify()
        self.assertEqual("PHYSICAL_STATE_VERIFIED", receipt["status"])
        slot_b = receipt["slots"]["B"]
        self.assertEqual(1, len(slot_b["artifacts"]))
        self.assertEqual(
            self.qsn.slot_unit["artifacts"][0]["artifact_id"],
            slot_b["artifacts"][0]["artifact_id"],
        )
        companion = slot_b["accepted_companion_passthrough"]["companions"][0]
        self.assertEqual(
            "ACCEPTED_BASELINE_COMPANION_PASSTHROUGH",
            companion["classification"],
        )
        self.assertEqual(
            self.qsn.core_artifact,
            companion["accepted_artifact_identity"],
        )
        self.assertEqual("ACTIVE", companion["physical_artifact"]["disposition"])
        enabled_ids = [
            item["primary_id"]
            for item in receipt["fabric_dependency_graph"]["enabled_fabric_jars"]
        ]
        self.assertEqual(1, enabled_ids.count("quick-stack-nearby"))
        self.assertEqual(1, enabled_ids.count("quick_stack_nearby_compat"))

    def test_successor_can_retain_required_upstream_passthrough(self) -> None:
        before = self.fixture.repository_state()
        accepted_before = copy.deepcopy(
            next(
                member
                for member in before["accepted_baseline"]["members"]
                if member["unit"]["project_uuid"] == self.qsn.project_uuid
            )
        )
        successor = self.qsn.successor_slot_unit()
        operation = self.qsn.operation(slot_unit=successor)

        dry_run = self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=True,
        )
        after_b = dry_run["slot_changes"]["B"]["after"]["members"][0]
        self.assertEqual(successor["deployment_id"], after_b["deployment_id"])
        self.assertEqual(successor["version"], after_b["version"])
        self.assertEqual(
            [self.qsn.core_artifact],
            after_b["accepted_companion_artifacts"],
        )
        self.assertEqual(
            "ACCEPTED_COMPANION_PASSTHROUGH_VERIFIED",
            dry_run["dependency_resolution"]["accepted_companion_passthrough"]["status"],
        )

        self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        deployed = self.fixture.repository_state()
        accepted_after = next(
            member
            for member in deployed["accepted_baseline"]["members"]
            if member["unit"]["project_uuid"] == self.qsn.project_uuid
        )
        self.assertEqual(accepted_before, accepted_after)
        self.assertEqual(
            "UNTESTED",
            deployed["slots"]["B"]["members"][0]["runtime_result"]["classification"],
        )
        receipt = self.fixture.manager.verify()
        self.assertEqual("PHYSICAL_STATE_VERIFIED", receipt["status"])
        self.assertEqual(
            successor["artifacts"][0]["artifact_id"],
            receipt["slots"]["B"]["artifacts"][0]["artifact_id"],
        )
        companion = receipt["slots"]["B"]["accepted_companion_passthrough"]["companions"][0]
        self.assertEqual(self.qsn.core_artifact, companion["accepted_artifact_identity"])

    def test_successor_passthrough_rejects_wrong_role_or_nonrepository_artifact(self) -> None:
        state = self.fixture.repository_state()
        successor = self.qsn.successor_slot_unit()
        cases = []

        wrong_role = copy.deepcopy(successor)
        wrong_role["artifacts"][0]["ownership_keys"] = ["mod:not_quick_stack_compat"]
        cases.append((wrong_role, "successor passthrough must unambiguously replace"))

        adopted = copy.deepcopy(successor)
        adopted["artifacts"][0]["source"] = {
            "type": "ADOPTED_TARGET",
            "path": "mods/" + adopted["artifacts"][0]["filename"],
        }
        cases.append((adopted, "successor passthrough requires repository-backed"))

        for candidate, message in cases:
            with self.subTest(message=message):
                with self.assertRaisesRegex(ManagerError, message):
                    self.fixture.manager.transition(
                        operation=self.qsn.operation(slot_unit=candidate),
                        expected_revision=state["revision"],
                        at="2099-01-01T00:00:30Z",
                        dry_run=True,
                    )

    def test_successor_passthrough_rejects_mixed_release_identity(self) -> None:
        state = self.fixture.repository_state()
        new_version_only = self.qsn.successor_slot_unit()
        new_version_only["source_commit"] = self.qsn.accepted_unit["source_commit"]
        new_source_only = self.qsn.successor_slot_unit()
        new_source_only["version"] = self.qsn.accepted_unit["version"]
        for candidate in (new_version_only, new_source_only):
            with self.subTest(
                version=candidate["version"],
                source_commit=candidate["source_commit"],
            ):
                with self.assertRaisesRegex(
                    ManagerError,
                    "accepted companion passthrough cannot mix accepted and successor release identity",
                ):
                    self.fixture.manager.transition(
                        operation=self.qsn.operation(slot_unit=candidate),
                        expected_revision=state["revision"],
                        at="2099-01-01T00:00:30Z",
                        dry_run=True,
                    )

    def test_successor_passthrough_promotion_remains_fail_closed(self) -> None:
        successor = self.qsn.successor_slot_unit()
        self.qsn.deploy(at="2099-01-01T00:00:30Z", slot_unit=successor)
        deployed = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation={
                "type": "RECORD_RESULT",
                "slot": "B",
                "classification": "PASS",
                "evidence": {"passed": ["successor fixture pass"], "failed": []},
            },
            expected_revision=deployed["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=False,
        )
        passed = self.fixture.repository_state()
        with self.assertRaisesRegex(
            ManagerError,
            "accepted companion passthrough promotion must reconcile byte-identically",
        ):
            self.fixture.manager.transition(
                operation={"type": "PROMOTE_SLOT", "slot": "B"},
                expected_revision=passed["revision"],
                at="2099-01-01T00:00:32Z",
                dry_run=True,
            )

    def test_missing_required_companion_fails_without_mutation(self) -> None:
        state = self.fixture.repository_state()
        before = tree_snapshot(self.fixture.root)
        with self.assertRaisesRegex(ManagerError, "missing managed Fabric dependency"):
            self.fixture.manager.transition(
                operation=self.qsn.operation(companion_artifacts=None),
                expected_revision=state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )
        self.assertEqual(before, tree_snapshot(self.fixture.root))

    def test_altered_companion_descriptor_is_rejected(self) -> None:
        altered = copy.deepcopy(self.qsn.core_artifact)
        altered["sha256"] = "0" * 64
        state = self.fixture.repository_state()
        before = tree_snapshot(self.fixture.root)
        with self.assertRaisesRegex(ManagerError, "must exactly match the accepted companion"):
            self.fixture.manager.transition(
                operation=self.qsn.operation(companion_artifacts=[altered]),
                expected_revision=state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )
        self.assertEqual(before, tree_snapshot(self.fixture.root))

    def test_foreign_project_companion_is_rejected(self) -> None:
        state = self.fixture.repository_state()
        before = tree_snapshot(self.fixture.root)
        foreign = copy.deepcopy(self.fixture.base["artifacts"][0])
        with self.assertRaisesRegex(ManagerError, "same accepted project unit"):
            self.fixture.manager.transition(
                operation=self.qsn.operation(companion_artifacts=[foreign]),
                expected_revision=state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )
        self.assertEqual(before, tree_snapshot(self.fixture.root))

    def test_duplicate_enabled_owner_of_companion_id_is_rejected(self) -> None:
        write_dependency_mod(
            self.fixture.mods / "unmanaged-qsn-duplicate.jar",
            "quick-stack-nearby",
            "0.4.0",
        )
        state = self.fixture.repository_state()
        before = tree_snapshot(self.fixture.root)
        with self.assertRaisesRegex(
            ManagerError,
            "unmanaged conflicting mod artifact .* owns quick-stack-nearby",
        ):
            self.fixture.manager.transition(
                operation=self.qsn.operation(),
                expected_revision=state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )
        self.assertEqual(before, tree_snapshot(self.fixture.root))

    def test_unnecessary_companion_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            qsn = AcceptedCompanionFixture(
                Path(temporary),
                compat_depends_on_upstream=False,
            )
            state = qsn.fixture.repository_state()
            before = tree_snapshot(qsn.fixture.root)
            with self.assertRaisesRegex(ManagerError, "unnecessary accepted companion"):
                qsn.fixture.manager.transition(
                    operation=qsn.operation(),
                    expected_revision=state["revision"],
                    at="2099-01-01T00:00:30Z",
                    dry_run=True,
                )
            self.assertEqual(before, tree_snapshot(qsn.fixture.root))

    def test_failed_transition_rolls_back_passthrough_bytes_and_receipts(self) -> None:
        state = self.fixture.repository_state()
        before = tree_snapshot(self.fixture.root)

        def fail_after_physical_apply(stage: str) -> None:
            if stage == "after_physical_apply":
                raise RuntimeError("accepted companion rollback fixture")

        with self.assertRaisesRegex(ManagerError, "rolled back"):
            self.fixture.manager.transition(
                operation=self.qsn.operation(),
                expected_revision=state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=False,
                failure_injector=fail_after_physical_apply,
            )
        self.assertEqual(before, tree_snapshot(self.fixture.root))
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

    def test_slot_clear_restores_original_complete_accepted_unit(self) -> None:
        accepted_before = copy.deepcopy(
            next(
                member
                for member in self.fixture.repository_state()["accepted_baseline"][
                    "members"
                ]
                if member["unit"]["project_uuid"] == self.qsn.project_uuid
            )
        )
        self.qsn.deploy()
        deployed = self.fixture.repository_state()
        slot_a_before = copy.deepcopy(deployed["slots"]["A"])
        self.fixture.manager.transition(
            operation={"type": "REMOVE_SLOT", "slot": "B"},
            expected_revision=deployed["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=False,
        )
        cleared = self.fixture.repository_state()
        accepted_after = next(
            member
            for member in cleared["accepted_baseline"]["members"]
            if member["unit"]["project_uuid"] == self.qsn.project_uuid
        )
        self.assertEqual(accepted_before, accepted_after)
        self.assertEqual(slot_a_before, cleared["slots"]["A"])
        self.assertIsNone(cleared["slots"]["B"])
        self.assertEqual(
            self.qsn.core_bytes,
            (self.fixture.mods / self.qsn.core_name).read_bytes(),
        )
        self.assertEqual(
            self.qsn.compat_bytes,
            (self.fixture.mods / self.qsn.compat_name).read_bytes(),
        )
        self.assertFalse(
            (self.fixture.mods / (self.qsn.compat_name + ".disabled")).exists()
        )
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

    def test_byte_identical_promotion_reconciles_original_accepted_identity(self) -> None:
        before = self.fixture.repository_state()
        stack_revision = before["accepted_baseline"]["revision"]
        accepted_before = copy.deepcopy(
            next(
                member
                for member in before["accepted_baseline"]["members"]
                if member["unit"]["project_uuid"] == self.qsn.project_uuid
            )
        )
        self.qsn.deploy()
        deployed = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation={
                "type": "RECORD_RESULT",
                "slot": "B",
                "classification": "PASS",
                "evidence": {"passed": ["fixture pass"], "failed": []},
            },
            expected_revision=deployed["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=False,
        )
        passed = self.fixture.repository_state()
        slot_a_before = copy.deepcopy(passed["slots"]["A"])
        self.fixture.manager.transition(
            operation={"type": "PROMOTE_SLOT", "slot": "B"},
            expected_revision=passed["revision"],
            at="2099-01-01T00:00:32Z",
            dry_run=False,
        )
        reconciled = self.fixture.repository_state()
        accepted_after = next(
            member
            for member in reconciled["accepted_baseline"]["members"]
            if member["unit"]["project_uuid"] == self.qsn.project_uuid
        )
        self.assertEqual(accepted_before, accepted_after)
        self.assertEqual(stack_revision, reconciled["accepted_baseline"]["revision"])
        self.assertEqual(slot_a_before, reconciled["slots"]["A"])
        self.assertIsNone(reconciled["slots"]["B"])
        self.assertFalse(
            (self.fixture.mods / (self.qsn.compat_name + ".disabled")).exists()
        )
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])


class AtomicCohortManagerTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.fixture = ManagerFixture(Path(self.temp.name))

    def tearDown(self) -> None:
        self.temp.cleanup()

    def operation(self) -> dict:
        c8, _ = self.fixture.add_repository_candidate()
        c3, _ = self.fixture.add_mossy_repository_candidate()
        return {
            "type": "DEPLOY_PROFILE",
            "slots": {
                "A": {
                    "members": [
                        candidate_declaration(c8, self.fixture.c5["deployment_id"]),
                        candidate_declaration(c3),
                    ]
                },
                "B": None,
            },
        }

    @staticmethod
    def legacy_projection_shape(projection: dict) -> dict:
        legacy = copy.deepcopy(projection)
        for label in ("A", "B"):
            slot = legacy["slots"][label]
            if slot["occupied"]:
                del slot["members"]
                del slot["version"]
        return legacy

    def install_revision_61_legacy_projection_preimage(self) -> tuple[dict, dict]:
        state = self.fixture.repository_state()
        state["revision"] = 60
        self.fixture.state_path.write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")
        self.fixture.manager.adopt(dry_run=False)

        active = self.fixture.repository_state()
        self.assertEqual(1, active["schema_version"])
        self.assertEqual(61, active["revision"])
        current = self.fixture.manager._title_projection(active)
        legacy = self.legacy_projection_shape(current)
        ledger = json.loads(self.fixture.manager.ledger_path.read_text(encoding="utf-8"))
        ledger["title_projection"] = copy.deepcopy(legacy)
        self.fixture.manager.ledger_path.write_text(
            json.dumps(ledger, indent=2) + "\n",
            encoding="utf-8",
        )
        self.fixture.manager.title_projection_path.write_text(
            json.dumps(legacy, indent=2) + "\n",
            encoding="utf-8",
        )
        return active, legacy

    def test_exact_revision_61_legacy_projection_dry_run_and_apply_migrate_atomically(self) -> None:
        active, legacy = self.install_revision_61_legacy_projection_preimage()
        verified_legacy = self.fixture.manager.verify()
        self.assertEqual("PHYSICAL_STATE_VERIFIED", verified_legacy["status"])
        self.assertEqual("LEGACY_MIGRATION_REQUIRED", verified_legacy["title_display"]["status"])
        self.assertEqual(
            "LEGACY_MIGRATION_REQUIRED",
            verified_legacy["title_display"]["projection"]["status"],
        )
        for label in ("A", "B"):
            self.assertNotIn("members", legacy["slots"][label])
            self.assertNotIn("version", legacy["slots"][label])

        operation = self.operation()
        before_dry_run = tree_snapshot(self.fixture.root)
        dry_run = self.fixture.manager.transition(
            operation=operation,
            expected_revision=61,
            at="2099-01-01T00:00:30Z",
            dry_run=True,
        )
        self.assertEqual(before_dry_run, tree_snapshot(self.fixture.root))
        self.assertEqual(62, dry_run["target_state_revision"])
        self.assertEqual(2, len(dry_run["title_projection"]["slots"]["A"]["members"]))
        self.assertTrue(
            all("version" in member for member in dry_run["title_projection"]["slots"]["A"]["members"])
        )

        applied = self.fixture.manager.transition(
            operation=operation,
            expected_revision=61,
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        migrated = self.fixture.repository_state()
        self.assertFalse(applied["dry_run"])
        self.assertEqual(2, migrated["schema_version"])
        self.assertEqual(62, migrated["revision"])
        final_projection = json.loads(self.fixture.manager.title_projection_path.read_text(encoding="utf-8"))
        final_ledger = json.loads(self.fixture.manager.ledger_path.read_text(encoding="utf-8"))
        self.assertEqual(self.fixture.manager._title_projection(migrated), final_projection)
        self.assertEqual(final_projection, final_ledger["title_projection"])
        self.assertEqual(2, len(final_projection["slots"]["A"]["members"]))
        self.assertEqual("SYNCHRONIZED", self.fixture.manager.verify()["title_display"]["status"])

    def test_schema_v1_legacy_projection_tamper_fails_before_transition(self) -> None:
        active, legacy = self.install_revision_61_legacy_projection_preimage()
        operation = self.operation()
        before = tree_snapshot(self.fixture.root)

        tampered_physical = copy.deepcopy(legacy)
        tampered_physical["slots"]["A"]["canary"] += 1
        self.fixture.manager.title_projection_path.write_text(
            json.dumps(tampered_physical, indent=2) + "\n",
            encoding="utf-8",
        )
        with self.assertRaisesRegex(
            ManagerError,
            "V2 title display projection does not match canonical runtime state",
        ):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=active["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )
        self.assertEqual(before["repository/runtime-state.json"], self.fixture.state_path.read_bytes())

        self.fixture.manager.title_projection_path.write_text(
            json.dumps(legacy, indent=2) + "\n",
            encoding="utf-8",
        )
        ledger = json.loads(self.fixture.manager.ledger_path.read_text(encoding="utf-8"))
        ledger["title_projection"]["slots"]["B"]["project_display_name"] = "Tampered"
        self.fixture.manager.ledger_path.write_text(
            json.dumps(ledger, indent=2) + "\n",
            encoding="utf-8",
        )
        with self.assertRaisesRegex(
            ManagerError,
            "target-local ledger title projection does not match its runtime state",
        ):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=active["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )
        self.assertEqual(before["repository/runtime-state.json"], self.fixture.state_path.read_bytes())

    def test_schema_v2_rejects_an_otherwise_exact_legacy_shaped_projection(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        candidate, _ = self.fixture.add_repository_candidate()
        before = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation={
                "type": "DEPLOY_PROFILE",
                "slots": {
                    "A": {
                        "members": [
                            candidate_declaration(candidate, self.fixture.c5["deployment_id"]),
                        ]
                    },
                    "B": None,
                },
            },
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        state = self.fixture.repository_state()
        self.assertEqual(2, state["schema_version"])
        current = self.fixture.manager._title_projection(state)
        legacy_shaped = self.legacy_projection_shape(current)
        ledger = json.loads(self.fixture.manager.ledger_path.read_text(encoding="utf-8"))
        ledger["title_projection"] = copy.deepcopy(legacy_shaped)
        self.fixture.manager.ledger_path.write_text(
            json.dumps(ledger, indent=2) + "\n",
            encoding="utf-8",
        )
        with self.assertRaisesRegex(
            ManagerError,
            "target-local ledger title projection does not match its runtime state",
        ):
            self.fixture.manager.verify()

        ledger["title_projection"] = copy.deepcopy(current)
        self.fixture.manager.ledger_path.write_text(
            json.dumps(ledger, indent=2) + "\n",
            encoding="utf-8",
        )
        self.fixture.manager.title_projection_path.write_text(
            json.dumps(legacy_shaped, indent=2) + "\n",
            encoding="utf-8",
        )
        with self.assertRaisesRegex(
            ManagerError,
            "V2 title display projection does not match canonical runtime state",
        ):
            self.fixture.manager.verify()

    def test_one_revision_atomic_deploy_finalizes_shared_ready_cohort(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation = self.operation()
        before = self.fixture.repository_state()
        dry_run = self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=True,
        )
        self.assertEqual(2, len(dry_run["slot_changes"]["A"]["after"]["members"]))
        self.assertIsNone(dry_run["slot_changes"]["B"]["after"])
        self.assertEqual("FABRIC_DEPENDENCY_GRAPH_VERIFIED", dry_run["dependency_resolution"]["status"])
        preimages = dry_run["displaced_member_runtime_result_preimages"]
        self.assertEqual(
            {self.fixture.c7["deployment_id"], self.fixture.mossy["deployment_id"]},
            {item["deployment_id"] for item in preimages},
        )
        self.assertTrue(all(item["runtime_result"]["classification"] == "FAIL" for item in preimages))
        self.assertTrue(all(item["runtime_result"]["evidence"]["failed"] for item in preimages))

        applied = self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        after = self.fixture.repository_state()
        self.assertEqual(before["revision"] + 1, after["revision"])
        self.assertEqual("READY_TO_TEST_VERIFIED", after["slots"]["A"]["deployment"]["state"])
        self.assertEqual(2, len(after["slots"]["A"]["members"]))
        self.assertIsNone(after["slots"]["B"])
        self.assertEqual("READY_TO_TEST_VERIFIED", applied["finalization"]["target_deployment_state"])

        receipt = self.fixture.manager.verify()
        self.assertEqual("PHYSICAL_STATE_VERIFIED", receipt["status"])
        self.assertEqual(2, receipt["slots"]["A"]["cohort_member_count"])
        self.assertEqual(
            {"matcha-heart-death-compat", "mossy-stone"},
            {member["project_id"] for member in receipt["slots"]["A"]["members"]},
        )
        for member in receipt["slots"]["A"]["members"]:
            self.assertEqual("A", member["slot"])
            self.assertEqual("READY_TO_TEST_VERIFIED", member["deployment_state"])
            self.assertEqual("UNTESTED", member["runtime_result"])
            self.assertEqual("CURRENT_RELEASE_DEPLOYED", member["current_release_comparison"]["classification"])
            self.assertEqual(1, len(member["artifacts"]))
            artifact_record = member["artifacts"][0]
            self.assertEqual(member["deployment_id"], artifact_record["deployment_id"])
            self.assertEqual(member["project_uuid"], artifact_record["project_uuid"])
            self.assertEqual(member["version"], artifact_record["expected_fabric_version"])
            self.assertEqual(member["version"], artifact_record["embedded_fabric_version"])
            self.assertEqual(64, len(artifact_record["sha256"]))
            self.assertTrue(artifact_record["filename"].endswith(".jar"))
        self.assertEqual(
            "Slot A: Matcha Death Rebalance - Canary 8 + Mossy Stone - Canary 3",
            receipt["title_display"]["lines"][1],
        )
        self.assertIsNone(receipt["slots"]["B"])

    def test_deploy_profile_rejects_declared_version_that_differs_from_embedded_version_before_mutation(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation = self.operation()
        operation["slots"]["A"]["members"][0]["unit"]["version"] = "0.1.7-canary999"
        before_state = self.fixture.repository_state()
        before_tree = tree_snapshot(self.fixture.root)

        with self.assertRaisesRegex(
            ManagerError,
            r"embedded Fabric version mismatch.*expected slot member version 0\.1\.7-canary999.*0\.1\.7-canary8",
        ):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=before_state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )

        self.assertEqual(before_state, self.fixture.repository_state())
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))

    def test_post_deployment_verification_rejects_slot_version_descriptor_mismatch(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation = self.operation()
        before = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )

        state = self.fixture.repository_state()
        state["slots"]["A"]["members"][0]["unit"]["version"] = "0.1.7-canary999"
        self.fixture.state_path.write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")
        ledger = json.loads(self.fixture.manager.ledger_path.read_text(encoding="utf-8"))
        ledger["runtime_state"] = copy.deepcopy(state)
        ledger["state_digest"] = self.fixture.manager._title_projection(state)["state_digest"]
        ledger["title_projection"] = self.fixture.manager._title_projection(state)
        self.fixture.manager.ledger_path.write_text(json.dumps(ledger, indent=2) + "\n", encoding="utf-8")
        self.fixture.manager.title_projection_path.write_text(
            json.dumps(self.fixture.manager._title_projection(state), indent=2) + "\n",
            encoding="utf-8",
        )

        with self.assertRaisesRegex(ManagerError, "embedded Fabric version mismatch"):
            self.fixture.manager.verify()

    def test_member_update_preserves_and_receipts_unchanged_companion_result(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation = self.operation()
        before = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        cohort = self.fixture.repository_state()["slots"]["A"]["members"]
        mossy_member = next(
            member for member in cohort if member["unit"]["project_uuid"] == self.fixture.mossy_uuid
        )
        state = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation={
                "type": "RECORD_RESULT",
                "slot": "A",
                "project_uuid": mossy_member["unit"]["project_uuid"],
                "classification": "PASS",
                "evidence": {"passed": ["companion result survives"], "failed": []},
            },
            expected_revision=state["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=False,
        )
        successor, _ = self.fixture.add_repository_candidate("0.1.8-canary9")
        state = self.fixture.repository_state()
        dry_run = self.fixture.manager.transition(
            operation={
                "type": "UPDATE_SLOT",
                "slot": "A",
                "candidate": candidate_declaration(successor, self.fixture.c5["deployment_id"]),
            },
            expected_revision=state["revision"],
            at="2099-01-01T00:00:32Z",
            dry_run=True,
        )
        preserved = dry_run["preserved_companion_runtime_results"]
        mossy_receipt = next(item for item in preserved if item["project_uuid"] == self.fixture.mossy_uuid)
        self.assertEqual("PASS", mossy_receipt["runtime_result"]["classification"])
        self.assertEqual(
            ["companion result survives"],
            mossy_receipt["runtime_result"]["evidence"]["passed"],
        )
        displaced = dry_run["displaced_member_runtime_result_preimages"]
        self.assertEqual(1, len(displaced))
        self.assertEqual("REPLACED", displaced[0]["disposition"])

    def test_cohort_write_failure_restores_every_member_and_state(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation = self.operation()
        before_state = self.fixture.repository_state()
        before_tree = tree_snapshot(self.fixture.root)

        def fail_after_first_write(stage: str) -> None:
            if stage == "after_write_1":
                raise RuntimeError("cohort fixture failure")

        with self.assertRaisesRegex(ManagerError, "rolled back"):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=before_state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=False,
                failure_injector=fail_after_first_write,
            )
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))
        self.assertEqual(before_state, self.fixture.repository_state())

    def test_clearing_cohort_restores_accepted_predecessor_and_preserves_one_member_other_slot(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c8, _ = self.fixture.add_repository_candidate()
        c3, _ = self.fixture.add_mossy_repository_candidate()
        gamma_uuid = stable_uuid("project:gamma-one-member")
        self.fixture.project_index[gamma_uuid] = "gamma-one-member"
        self.fixture.project_display_names[gamma_uuid] = "Gamma One Member"
        self.fixture.manager.project_index = copy.deepcopy(self.fixture.project_index)
        self.fixture.manager.project_display_names = copy.deepcopy(self.fixture.project_display_names)
        gamma_version = "0.1.0-canary1"
        gamma_name = f"gamma-one-member-{gamma_version}.jar"
        gamma_source = self.fixture.repository / "artifacts" / gamma_name
        gamma_hash = write_dependency_mod(gamma_source, "gamma_one_member", gamma_version)
        gamma = unit(
            "gamma-one-member",
            gamma_version,
            artifact(
                gamma_name,
                "gamma_one_member",
                gamma_hash,
                source_type="REPOSITORY",
                source_path="artifacts/" + gamma_name,
            ),
            project_uuid=gamma_uuid,
        )
        operation = {
            "type": "DEPLOY_PROFILE",
            "slots": {
                "A": {
                    "members": [
                        candidate_declaration(c8, self.fixture.c5["deployment_id"]),
                        candidate_declaration(c3),
                    ]
                },
                "B": {"candidate": candidate_declaration(gamma)},
            },
        }
        state = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=operation,
            expected_revision=state["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        deployed = self.fixture.repository_state()
        before_b = copy.deepcopy(deployed["slots"]["B"])
        gamma_bytes = (self.fixture.mods / gamma_name).read_bytes()
        c5_name = self.fixture.c5["artifacts"][0]["filename"]
        c8_name = c8["artifacts"][0]["filename"]
        self.assertTrue((self.fixture.mods / (c5_name + ".disabled")).is_file())
        self.assertFalse((self.fixture.mods / c5_name).exists())

        self.fixture.manager.transition(
            operation={"type": "REMOVE_SLOT", "slot": "A"},
            expected_revision=deployed["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=False,
        )

        cleared = self.fixture.repository_state()
        self.assertIsNone(cleared["slots"]["A"])
        self.assertEqual(before_b, cleared["slots"]["B"])
        self.assertEqual(self.fixture.c5_bytes, (self.fixture.mods / c5_name).read_bytes())
        self.assertFalse((self.fixture.mods / (c5_name + ".disabled")).exists())
        self.assertFalse((self.fixture.mods / c8_name).exists())
        self.assertEqual(gamma_bytes, (self.fixture.mods / gamma_name).read_bytes())
        receipt = self.fixture.manager.verify()
        self.assertEqual("PHYSICAL_STATE_VERIFIED", receipt["status"])
        self.assertEqual(1, receipt["slots"]["B"]["cohort_member_count"])
        self.assertEqual("gamma-one-member", receipt["slots"]["B"]["project_id"])


class ProfileUseGuardTests(unittest.TestCase):
    def test_operator_shell_that_mentions_profile_is_not_minecraft_use(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            profile = Path(temporary) / "Matcha Flavoured 26.2 Workbench"
            profile.mkdir()
            evidence = [
                {
                    "ProcessId": 101,
                    "Name": "pwsh.exe",
                    "ExecutablePath": "C:\\Program Files\\PowerShell\\7\\pwsh.exe",
                    "CommandLine": f'pwsh.exe -Command "inspect {profile}"',
                },
                {
                    "ProcessId": 102,
                    "Name": "javaw.exe",
                    "ExecutablePath": "C:\\Java\\bin\\javaw.exe",
                    "CommandLine": "javaw.exe -jar unrelated.jar",
                },
            ]
            completed = unittest.mock.Mock(returncode=0, stdout=json.dumps(evidence), stderr="")
            with patch("tools.test_instance_manager.manager.os.name", "nt"), patch(
                "tools.test_instance_manager.manager.subprocess.run", return_value=completed
            ):
                self.assertEqual([], _windows_processes_using_profile(profile))

    def test_profile_named_java_process_is_detected(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            profile = Path(temporary) / "Matcha Flavoured 26.2 Workbench"
            profile.mkdir()
            evidence = {
                "ProcessId": 4242,
                "Name": "javaw.exe",
                "ExecutablePath": "C:\\Java\\bin\\javaw.exe",
                "CommandLine": f'javaw.exe --gameDir "{profile}"',
            }
            completed = unittest.mock.Mock(returncode=0, stdout=json.dumps(evidence), stderr="")
            with patch("tools.test_instance_manager.manager.os.name", "nt"), patch(
                "tools.test_instance_manager.manager.subprocess.run", return_value=completed
            ):
                self.assertEqual(4242, _windows_processes_using_profile(profile)[0]["pid"])

    def test_named_profile_process_blocks_before_lock_or_other_mutation(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary) / "ModrinthApp" / "profiles"
            root.mkdir(parents=True)
            fixture = ManagerFixture(root)
            before = tree_snapshot(root)
            blocker = [{"pid": 4242, "name": "javaw.exe", "command_line": str(fixture.target)}]
            with patch(
                "tools.test_instance_manager.manager._windows_processes_using_profile",
                return_value=blocker,
            ):
                with self.assertRaisesRegex(ManagerError, r"actively in use.*javaw\.exe \(PID 4242\)"):
                    fixture.manager.adopt(dry_run=False)
            self.assertEqual(before, tree_snapshot(root))
            self.assertFalse(fixture.manager.lock_path.exists())


class CurrentReleaseComparisonTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.fixture = ManagerFixture(Path(self.temp.name))

    def tearDown(self) -> None:
        self.temp.cleanup()

    def test_older_slot_release_is_never_reported_as_current(self) -> None:
        unit = self.fixture.c7
        artifact = unit["artifacts"][0]
        current = {
            "identity": {"project_id": unit["project_id"]},
            "state": {
                "releases": {
                    "current": {
                        "version": "C8",
                        "source_commit": "f" * 40,
                        "artifact": {
                            "filename": "newer-current.jar",
                            "sha256": "e" * 64,
                        },
                    }
                }
            },
        }
        result = self.fixture.manager._current_release_comparison(
            unit,
            deployment_state="READY_TO_TEST_VERIFIED",
            statuses={unit["project_uuid"]: (Path("WORKBENCH_STATUS.json"), current)},
        )
        self.assertEqual("OLDER_RELEASE_DEPLOYED", result["classification"])
        self.assertFalse(result["slot_matches_current_release"])
        self.assertNotEqual(artifact["filename"], result["current_release"]["artifacts"][0]["filename"])

    def test_exact_current_identity_not_yet_deployed_is_explicit(self) -> None:
        unit = self.fixture.c7
        artifact = unit["artifacts"][0]
        current = {
            "identity": {"project_id": unit["project_id"]},
            "state": {
                "releases": {
                    "current": {
                        "version": "C7",
                        "source_commit": unit["source_commit"],
                        "artifact": {
                            "filename": artifact["filename"],
                            "sha256": artifact["sha256"],
                        },
                    }
                }
            },
        }
        result = self.fixture.manager._current_release_comparison(
            unit,
            deployment_state="NOT_DEPLOYED",
            statuses={unit["project_uuid"]: (Path("WORKBENCH_STATUS.json"), current)},
        )
        self.assertEqual("CURRENT_RELEASE_NOT_DEPLOYED", result["classification"])
        self.assertTrue(result["slot_matches_current_release"])


if __name__ == "__main__":
    unittest.main()
