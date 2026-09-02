# Mynx Ribbits 26.2

This directory maintains the legally trackable source, build, test, and private-assembly tooling for the private Minecraft Java 26.2 downstream Ribbits build used by the fixed Mynx/Matcha stack. Upstream behavior remains the default; every intentional Mynx departure is recorded in `MYNX_DEVIATIONS.md`.

Upstream code is covered by LGPL-3.0. Ribbits resources and the approved Guard Ribbits and Useful Ribbits donor visuals retain their original rights status. Protected resources, donor-derived outputs, private manifests/reports, pristine or donor JARs, and the complete private candidate are ignored and must not be published or redistributed.

## Current candidate

- Version: `4.1.6+26.2-mynx-canary1`
- Implementation checkpoint: `3e9b81f2c2464811caf593a6451e6ff1c9136955`
- Private artifact: `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary1.jar`, 3,163,214 bytes, SHA-256 `EC2946D299BCDA22FFA393CE310760D1F963ED1D28377A86F45DA22E64869443`
- Source-only artifact: `ribbits-source-only-4.1.6+26.2-mynx-canary1.jar`, 1,100,732 bytes, SHA-256 `9810B58958F6E0D06A1A391E483781364275AEFCDA7481F2A4B70FF3F3A308D7`
- State: `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`

Two independent clean private assemblies from the implementation checkpoint produced the same filename, size, SHA-256, and complete JAR bytes. The source-only artifact contains no protected or donor-derived assets and is deliberately non-runnable.

The exact faithful-port baseline remains historical private Canary 2: `ribbits-private-reconstruction-4.1.6+26.2-port-canary2.jar`, 3,124,301 bytes, SHA-256 `0AD73B7B61C6EE792EC1745056563641767AFE6811C0FDF2D3C99123C3F289DC`, source checkpoint `efe1970d2447aea4913e67f55c0c6b83cc36c5bb`. It was not renamed, overwritten, accepted, or designated as rollback.

## Canary 1 scope

Canary 1 fixes the two private chest-loot codec failures, adds Chef, Farmer, Prospector, and Guard as visual variants of `RibbitEntity`, retains `ribbits:nitwit` with the display name Musician, and replaces implicit registry-wide village selection with an explicit ordered equal-weight pool of Musician, Gardener, Fisherman, Merchant, Chef, Farmer, Prospector, and Guard. Sorcerer remains registered and available through its typed egg and commands but is absent from natural village assignment.

Profession identity continues to serialize only by stable registry ID. The existing five professions retain their exact model IDs and shared texture. New professions supply private model/texture identities, initialize without instruments, and receive no donor AI or behavior. The rendering contract uses deterministic composite textures and twelve private profession-specific umbrella models so each new profession keeps its accessories through all three existing rain/umbrella variants.

Merchant's six definitions and Fisherman's seven-definition/four-random-offer behavior remain unchanged. The existing restock implementation remains byte-exact to authoritative `main`. No redesigned trade economy, strict daily restocking, Chef progression, Sorcerer progression, witch-hut work, explorer maps, Matcha integration, or Custom Portals integration is present.

## Public source-only build

Supply the exact YUNG's API compatibility JAR through `YUNGS_API_26_2_JAR`; leave the two private-resource variables unset.

```powershell
$env:YUNGS_API_26_2_JAR = 'C:\path\to\YungsApi-26.2-Fabric-6.1.1-compat.1.jar'
Remove-Item Env:RIBBITS_PRIVATE_RESOURCES_DIR -ErrorAction SilentlyContinue
Remove-Item Env:RIBBITS_PRIVATE_MANIFEST -ErrorAction SilentlyContinue
.\gradlew.bat clean test build --offline --no-daemon
```

Required dependency identity: `YungsApi-26.2-Fabric-6.1.1-compat.1.jar`, 1,266,121 bytes, SHA-256 `527850C4F061FA9AB327AE0B32A3D26EBC77B51FB86234F5F6B1E3418CA084CA`. The build verifies the resulting source-only archive contains no protected or donor-derived payload.

## Private assembly contract

`tools/private_resource_tools.py` is the only tracked private assembler. Inputs remain external and read-only:

- `Ribbits-1.21.1-Fabric-4.1.6.jar` — SHA-256 `4CF86564AED393410FB1DBCA3A9CE2425382307655E92BB6B43F3DDCEE5BF731`
- Minecraft 26.2 merged client JAR — 37,396,380 bytes; SHA-256 `200D673E028D27DDB22BD2D365FBCB98B55BE4B2043EE52681A8F30812C12CFE`
- `GuardRibbits-1.20.1-Fabric-1.0.4.jar` — 166,896 bytes; SHA-256 `52F1E184DC12CF1E29BC224AB5A640B8EA5875AA9F46067C0907C6A45B7D1869`
- `useful_ribbits-1.0.2-forge-1.20.1.jar` — 416,439 bytes; SHA-256 `2B56007A985B162477113BB2EA1776D9CE2CE602886EA21A88D8B2500D2DED5D`

The donor JARs must be exact direct members of the resolved authoritative checkout's `originals/mods` directory. The tool rejects renamed, substituted, symlinked, missing, duplicate, malformed, changed, or out-of-root inputs and rehashes both donors before and after assembly. It reads only the eight allowlisted visual members documented in `MYNX_DEVIATIONS.md`, records every derived output, and never copies either donor archive into the output.

The private root must be beneath an ignored `test-builds/private` boundary. The explicit operations are:

```text
python tools/private_resource_tools.py assemble --private-root <private-root> --pristine <ribbits.jar> --minecraft-client <minecraft.jar> --originals-root <authoritative-originals> --guard-donor <exact-guard.jar> --useful-donor <exact-useful.jar> --output <resources> --manifest <manifest.json>
python tools/private_resource_tools.py validate-tree --private-root <private-root> --resources <resources> --pristine <ribbits.jar> --minecraft-client <minecraft.jar> --originals-root <authoritative-originals> --guard-donor <exact-guard.jar> --useful-donor <exact-useful.jar> --report <tree-report.json>
python tools/private_resource_tools.py validate-jar --private-root <private-root> --resources <resources> --pristine <ribbits.jar> --jar <private.jar> --minecraft-client <minecraft.jar> --originals-root <authoritative-originals> --guard-donor <exact-guard.jar> --useful-donor <exact-useful.jar> --report <jar-report.json>
```

The exact assembly contains 336 resource files: 245 strict JSON, 29 NBT, 18 OGG, and 44 PNG files. It includes 41 direct GeckoLib models, 24 item definitions, nine typed spawn eggs, and exactly 20 donor-derived model/texture outputs. The manifest becomes eligible only after the final donor rehash and is atomically published; Gradle independently validates the complete donor set, all allowlists and output provenance, every staged file hash, loot and structure migrations, resource counts, and the exact private-domain archive inventory.

For the private Gradle build, set `RIBBITS_PRIVATE_RESOURCES_DIR` and `RIBBITS_PRIVATE_MANIFEST` to that exact staged tree and eligible manifest, then run the same clean offline build. Keep the complete private artifact, donor-derived resources, private trees, manifests, reports, and previews ignored and untracked.

The exact archive-diff boundary, donor member allowlist, rendering decisions, compatibility repairs, and deferred roadmap are recorded in `MYNX_DEVIATIONS.md`. Runtime work must use the separate procedure in `TESTING.md` and must never infer a runtime pass from these static results.
