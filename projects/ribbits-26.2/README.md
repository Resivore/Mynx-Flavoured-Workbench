# Ribbits 26.2 Port

This directory contains the legally trackable source, build, and test surface for the private Fabric port of Ribbits 4.1.6 to Minecraft Java 26.2. Upstream code is covered by LGPL-3.0; Ribbits textures, sounds, models, data, icons, and other packaged assets are All Rights Reserved and are intentionally absent.

The ported Java and test implementation comes from legacy source checkpoint `0105a1114edcf5eb0306bb60a48ad48d963d187d`; this migration adds build-time identity enforcement and public/private output separation without changing gameplay code. The later retained project head `2a245c05548ec8ef51cdab146c01bb562ce90365` changed only legacy documentation and hash-only private-Canary provenance. The upstream comparison points are Ribbits 4.1.6 revision `b1cde776bc1eeb613cf3e21014d9397b213290e7` and later API reference `9e0065985ef794dae2cccefdf1314c5ac77eeb6e`.

## Current technical checkpoint

The source compiles against Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.157.0+26.2, GeckoLib 5.5.1, Cloth Config 26.2.155, Mod Menu 20.0.1, and the exact external YUNG's API compatibility artifact described below. The port preserves the original entity data and persistence keys, five professions, four instruments, umbrella/pride/model priority, trades, behaviors, maraca/music networking, blocks, vegetation, village/world-generation processors, configuration, and client/server split.

Focused parity work retained these narrow contracts:

- Pending client music and maraca actions queue by entity UUID, replay in order on entity load, and clear on disconnect.
- Merchant menu validity uses exact customer identity; pick results use exact profession identity and fall back to the nitwit egg for unknown professions.
- Regional pride suppression defaults to false, including Cloth Config reset behavior, and supporter-list population starts once from common initialization.
- All eight payload identities/codecs/directions, all six processor codec bindings, and the 25 direct GeckoLib model IDs remain covered; a ninth, registry-aware JUnit suite decodes and round-trips the migrated configured-feature schema against the exact Minecraft 26.2 registries.
- Client render, particle, sound, model, skin, and supporter hooks remain outside main/common initialization and the service-loaded Fabric platform helper.

Two exact-upstream behaviors remain deliberately unchanged: typed eggs replace the default profession after finalization without immediately reassessing goals, and the custom egg spawner branch still targets the vanilla spawner path instead of the generalized 26.2 `Spawner` interface. One Fabric entity-renderer registration deprecation warning also remains.

## Canary 2 worldgen migration

Canary 1 cannot create or duplicate/recreate a Minecraft 26.2 world because five Ribbits configured features still select the removed built-in feature ID `minecraft:random_patch`. Minecraft 26.2 registers no feature under that ID. The exact replacement used here is a singleton `minecraft:sequence` whose placed feature prepends `minecraft:count` and `minecraft:random_offset`; symmetric zero-plateau `minecraft:trapezoid` providers preserve the old horizontal and vertical random-patch distributions, and the original Ribbits inline feature and air predicate remain unchanged and in order.

The assembler applies that conversion only to `ribbits:giant_lilypad_patch`, `ribbits:swamp_daisy_patch`, `ribbits:toadstool_patch`, `ribbits:umbrella_leaf_patch`, and `ribbits:veg_patch`. It rejects a missing, duplicate, extra, stale, partially migrated, or structurally unexpected target before writing any of the five outputs. The manifest and Gradle packaging gate independently require the exact five-ID migration record.

The resulting private candidate is `ribbits-private-reconstruction-4.1.6+26.2-port-canary2.jar`, 3,124,301 bytes, SHA-256 `0AD73B7B61C6EE792EC1745056563641767AFE6811C0FDF2D3C99123C3F289DC`. Two consecutive clean private builds produced that identical size and hash. Against historical Canary 1, both archives contain the same 434 uncompressed file payload paths with no additions or removals; the only changed payloads are the five configured-feature migrations, the `fabric.mod.json` Canary version, and four public text files whose content is identical after newline normalization because current `.gitattributes` requires canonical LF while Canary 1 captured CRLF. Archive ordering and timestamps are normalized after Loom nests the two fixed dependencies, so the new exact artifact identity is reproducible. Canary 1 remains immutable, broken for world creation, and neither accepted nor a rollback candidate.

## Public source-only verification

Supply the exact YUNG's API compatibility JAR through `YUNGS_API_26_2_JAR`. Leave `RIBBITS_PRIVATE_RESOURCES_DIR` and `RIBBITS_PRIVATE_MANIFEST` unset.

```powershell
$env:YUNGS_API_26_2_JAR = 'C:\path\to\YungsApi-26.2-Fabric-6.1.1-compat.1.jar'
Remove-Item Env:RIBBITS_PRIVATE_RESOURCES_DIR -ErrorAction SilentlyContinue
Remove-Item Env:RIBBITS_PRIVATE_MANIFEST -ErrorAction SilentlyContinue
.\gradlew.bat clean test build --offline --no-daemon
```

Required YUNG's API identity:

- Filename: `YungsApi-26.2-Fabric-6.1.1-compat.1.jar`
- SHA-256: `527850C4F061FA9AB327AE0B32A3D26EBC77B51FB86234F5F6B1E3418CA084CA`
- Legacy compatibility revision: `57a89b593ce9410db98b75a350916ae36caf9522`

This command verifies the public source and tests only. Its resource-incomplete archive is deliberately named `ribbits-source-only-4.1.6+26.2-port-canary2.jar`; it is disposable and must not be treated as a runnable Canary.

## Private assembly contract

`tools/private_resource_tools.py` is the only tracked private-assembly mechanism. It contains no protected Ribbits asset or complete source/output PNG bytes. It hash-verifies and stages inputs beneath a repository-ignored `test-builds/private` root, refuses to write into `originals/`, produces a per-file manifest, performs the exact five configured-feature conversions above, and validates both the staged tree and a privately assembled JAR.

Required private inputs are external and read-only:

- `Ribbits-1.21.1-Fabric-4.1.6.jar`, SHA-256 `4CF86564AED393410FB1DBCA3A9CE2425382307655E92BB6B43F3DDCEE5BF731`.
- Minecraft 26.2 merged client JAR, historical identity 37,396,380 bytes and SHA-256 `200D673E028D27DDB22BD2D365FBCB98B55BE4B2043EE52681A8F30812C12CFE`.
- The exact YUNG's API artifact above.

The assembler requires exactly 287 protected Ribbits inputs and produces exactly 308 files. It performs only the pinned migrations required by the 26.2 port: the five configured-feature schema conversions, GeckoLib resource relocation, item definitions, cutout model metadata, recipe/advancement/loot schema changes, locale/config-key normalization, and the authorized temporary spawn-egg derivation. Output trees, manifests, validation reports, previews, and private JARs must remain under ignored local private-build paths and must never be committed.

The CLI exposes three explicit operations:

```text
python tools/private_resource_tools.py assemble --private-root <root> --pristine <ribbits-4.1.6.jar> --minecraft-client <minecraft-26.2.jar> --output <resources> --manifest <manifest.json>
python tools/private_resource_tools.py validate-tree --private-root <root> --resources <resources> --pristine <ribbits-4.1.6.jar> --minecraft-client <minecraft-26.2.jar> --report <tree-report.json>
python tools/private_resource_tools.py validate-jar --private-root <root> --resources <resources> --pristine <ribbits-4.1.6.jar> --minecraft-client <minecraft-26.2.jar> --jar <private.jar> --report <jar-report.json>
```

For a private Gradle assembly, set both `RIBBITS_PRIVATE_RESOURCES_DIR` and `RIBBITS_PRIVATE_MANIFEST` to the exact staged output and its manifest. Gradle rehashes every manifest entry before packaging.

The private output uses the deliberately nonhistorical `ribbits-private-reconstruction-4.1.6+26.2-port-canary2.jar` filename. It is a new candidate, not a renamed or modified Canary 1 binary. Any future rebuild must be independently checked against the exact Canary 2 size and SHA-256 above before being described as the same artifact.

Durable compatibility identities include config file `ribbits-26_2.toml`, translation prefix `text.autoconfig.ribbits-26_2`, and supporter-list user agent `Ribbits/26_2`. The retained structure-processor conversion maps `FACING` to `HORIZONTAL_FACING`; that difference is currently unreachable because all five registered block-replacement processors disable randomized facing and their replacement states do not expose six-way `FACING`.

## Authorized temporary spawn-egg derivation

The Workbench owner's temporary, shared green derivation from Minecraft 26.2's vanilla frog spawn egg continues for private Canary 2. The tool enforces the complete contract without storing either PNG:

- Source entry `assets/minecraft/textures/item/frog_spawn_egg.png`: 16x16, 200 bytes, SHA-256 `23962914851DB6E2F7F346E80BF412D8F6E1435E6CD54B73DD5CCBEC1A50ECCF`.
- Transformation: replace only the indexed PNG `PLTE` RGB bytes; palette indices, pixel positions, alpha, dimensions, and all other PNG chunks remain unchanged.
- Output entry `assets/ribbits/textures/item/ribbit_spawn_egg.png`: 16x16, 200 bytes, SHA-256 `53EB7F9D59457B2CD38A5BF65C10C78AC0721DA2123F37CE2F48C4D3E1DC1ED1`.
- Consumers: nitwit, fisherman, gardener, merchant, and sorcerer spawn eggs, all through `minecraft:item/generated`.

This visual is not exact Ribbits 4.1.6 spawn-egg parity and does not preserve the five original tint-pair distinctions. It changes no item ID, name, registration, profession selection, dispenser/spawn behavior, or entity data.

No protected resource, source/output egg PNG, Minecraft client JAR, pristine Ribbits JAR, private manifest/report, or private Canary JAR belongs in this public repository.
