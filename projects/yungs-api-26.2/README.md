# YUNG's API 26.2 Compatibility

This project maintains the LGPLv3 Fabric build of YUNG's API used by the Minecraft Java 26.2 Mynx stack. Compat.2 preserves the imported YUNG's API 6.1.1 behavior while fixing the enhanced terrain-adaptation iterator race observed when Ribbit Village chunks generated concurrently.

## Current candidate

- Version: `26.2-Fabric-6.1.1-compat.2`
- Mod ID: `yungsapi`
- Implementation checkpoint: `f0be1f9c6c1f3e843a0e44791a650df237836537`
- Artifact: `YungsApi-26.2-Fabric-6.1.1-compat.2.jar`, 1,260,939 bytes, SHA-256 `FF22A6B509BA559988D7A9352DC94AC612C4B099517ACAC7DA7C81322D797ED7`
- State: `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`

The artifact is retained locally under the ignored `artifacts/` directory. It is not accepted or a rollback release, and build or automated fixture success is not Minecraft runtime validation.

## Compatibility correction

The legacy implementation stored mutable piece and junction iterators on each enhanced Beardifier. Concurrent or re-entrant density samples could advance the same cursor between `hasNext()` and `next()`, causing the observed empty-deque `NoSuchElementException` and allowing exhausted state to leak between calls. Legitimately empty filtered inputs were not themselves malformed.

Compat.2 applies the [official upstream correction](https://github.com/YUNG-GANG/YUNGs-API/commit/0196d1800cf071764df7f04be6d5412ad4bc04b9) from commit `0196d1800cf071764df7f04be6d5412ad4bc04b9`: the Beardifier stores lists, and each density call obtains fresh local iterators. Empty inputs therefore contribute neutral density, while nonempty piece and junction calculations retain their established behavior. Focused fixtures exercise empty, nonempty, repeated, and forced-parallel calls.

## Source and license

The maintained subset was imported from exact legacy checkpoint `57a89b593ce9410db98b75a350916ae36caf9522`, whose production source traces to the official [YUNG-GANG/YUNGs-API](https://github.com/YUNG-GANG/YUNGs-API) `26.1.2` branch at checkpoint `fc5af0d11170eda2adfedd3caaa0cea7599ecb03`. The concurrency change is the official upstream three-file fix above. The complete LGPL version 3 license is preserved in `LICENSE` and packaged as `LICENSE_YungsApi` in the artifact.

Only Common, Fabric, the necessary build support, focused downstream-consumer fixtures, and licensing material are maintained here. NeoForge and CurseForge/Modrinth publishing configuration are intentionally excluded from this Fabric-only compatibility project. See `TESTING.md` for the exact automated evidence and pending runtime procedure.
