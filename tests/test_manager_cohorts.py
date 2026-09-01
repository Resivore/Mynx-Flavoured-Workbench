from __future__ import annotations

import copy
import hashlib
import json
import os
import sqlite3
import tempfile
import unittest
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
    ManagerError,
    PlatformAttestation,
    PhysicalManager,
    _fabric_predicate_matches,
    _windows_processes_using_profile,
)


C57 = "4.2.1-bge.canary57.unified+26.2"
C58 = "4.2.2-bge.canary58.glass-corner-uv+26.2"
COMPATIBLE_LATER = "4.2.9-bge.canary99.compatible+26.2"
PRE_UNIFIED = "4.2.0-bge.canary56.pre-unified+26.2"
UPPER_BOUND = "4.3.0-"
TROWEL_RANGE = ">=4.2.1-bge.canary57.unified+26.2 <4.3.0-"


def descriptor(
    filename: str,
    primary_id: str,
    version: str,
    *,
    provides: tuple[str, ...] = (),
    depends: dict[str, tuple[str, ...]] | None = None,
    project_id: str | None = None,
) -> FabricModDescriptor:
    return FabricModDescriptor(
        path=Path(filename),
        relative_path="mods/" + filename,
        filename=filename,
        primary_id=primary_id,
        provides=provides,
        version=version,
        depends=depends or {},
        managed_project_uuid=("00000000-0000-4000-8000-" + ("1" if project_id else "0") * 12),
        managed_project_id=project_id,
        managed_deployment_id=("00000000-0000-4000-8000-" + ("2" if project_id else "0") * 12),
        managed_artifact_id=("00000000-0000-4000-8000-" + ("3" if project_id else "0") * 12),
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
