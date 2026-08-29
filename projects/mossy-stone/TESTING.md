# Testing

C2 (`0.2.0-canary2`) is the exact current candidate. Test only `mossy-stone-0.2.0-canary2.jar`, 27,875 bytes, SHA-256 `0A337771301DFB1165ADED256908677B93B48F756E1C5A8EACFDE5D057178B59`, from legacy implementation commit `2008a418d868139e88bc066672d10f8463d84e0f`.

The frozen legacy checkpoint records Java 25 compilation and 9/9 focused tests passing, followed by deployment of that exact binary and `READY_TO_TEST_VERIFIED`. Migration verification used Temurin Java 25.0.4.1+1, Gradle 9.5.1, and Loom 1.17.19: offline `clean test build --no-daemon` passed the same 9/9 focused tests, and the fresh 27,875-byte deployable reproduced the retained C2 SHA-256 byte-for-byte. No Minecraft process, Test Slot, Test Instance Manager state, or Minecraft instance was accessed during migration. Build and readiness evidence are not runtime validation; C2 remains `RUNTIME_UNTESTED`.

## Runtime preconditions

1. Obtain explicit Test Slot ownership and confirm the exact C2 artifact above before any deployment or replay of the frozen readiness state.
2. Use Minecraft Java 26.2, Fabric Loader 0.19.3 or later, Fabric API 0.157.0+26.2, the exact `clutternomore` 2.0.7+26.2 ShapeMap contract, and the compatible Block Geometry Extensions stack identified by project UUID `4b2342fc-7bdf-5ba6-9f37-d551109d214c`.
3. Keep Regions Unexplored absent. Record only behavior actually observed; do not infer checklist rows from launch, build, static tests, or readiness.

## Focused C2 runtime matrix

1. Confirm C2 loads without Regions Unexplored and produces no missing-registry, model, texture, recipe, loot, or ShapeMap error.
2. Confirm the full block, slab, stairs, wall, BGE Vertical Slab, and BGE Step all display the exact approved Mossy Stone artwork from the `mossy_stone` namespace.
3. Compare full-block and directly owned geometry mining speed, Stone sound, pickaxe/correct-tool requirement, resistance, placement, and ordinary shape behavior with vanilla Stone-family controls.
4. Break the full block without Silk Touch and confirm exactly Mossy Cobblestone drops; repeat with Silk Touch and confirm the Mossy Stone full block drops itself. Preserve normal tool and explosion semantics.
5. Confirm slab, stairs, wall, Vertical Slab, and Step drop their own corresponding geometry rather than degrading into Mossy Cobblestone shapes.
6. Verify Stone plus Moss Block, Stone plus Vine, smelting Mossy Cobblestone, blasting Mossy Cobblestone, and 2x2 Mossy Stone producing four vanilla Mossy Stone Bricks.
7. Verify the ordinary crafting outputs of six slabs, four stairs, and six walls, plus stonecutting outputs of two slabs, one stair, and one wall. Confirm the matching recipe discovery and unlock behavior.
8. Confirm BGE exposes exactly the canonical parent, slab, Vertical Slab, stairs, Step, and wall without duplicate registrations, duplicate recipes, recursive generation, or Mossy Stone ownership of the two BGE-derived shapes.
9. Check placement, state, and waterlogging behavior for each geometry where its owning implementation supports it, then smoke-check representative existing BGE/Nibaru and Interchangeable Block Families behavior for a Mossy Stone-attributable regression.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for any load failure, missing or incorrect artwork, changed Stone-like behavior, wrong loot, recipe/output/unlock mismatch, duplicate or missing derived geometry, ownership conflict, state/waterlogging defect, crash, or relevant log error. Do not promote C2 from static or readiness evidence.

## Retained C1 evidence

Exact C1 is retained at `artifacts/mossy-stone-0.1.0-canary1.jar`, 20,952 bytes, SHA-256 `981F6885DDC998F0D19B15BCBBE869867C67B4392BD82F8AFA368711E7559EE4`, source `2051399c9af3492599fb9be0cd60232b30035969`. It is the older RU-backed predecessor, passed its frozen 5/5 focused static tests, was never deployed, and remains runtime untested. C1 is neither accepted nor a V2 rollback release.
