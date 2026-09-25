# C4 local masonry-closure verification

Exact current candidate: `architectural-material-closure-0.1.0-canary4.jar`, 1,771,750 bytes, SHA-256 `B7344EC4B53305CDFEB5CFD514E2570B919ECCA5432D6C9ED11B7E1846A90FD5`, built at `2026-09-25T20:37:28.9453931Z` from source checkpoint `8995d572`. It is `STATIC_PASS / RUNTIME_UNTESTED`. This private derivative JAR is local only and must not be distributed.

From the repository root with Java 25:

```powershell
& projects/building-but-better/gradlew.bat -p projects/architectural-material-closure test check --no-daemon
```

Use only an owner-approved isolated Minecraft 26.2 Fabric environment with BBB, Macaw Windows, Macaw Paths, and AMC. Test every clumped masonry profile: Stone, Andesite, Diorite, Granite, Brick, Mossy Stone Brick, Cobbled Deepslate, Deepslate, Mud Brick, Polished Blackstone, Prismarine Bricks, Dark Prismarine, Sandstone, Red Sandstone, Quartz, Nether Brick, and End Stone Brick.

1. Confirm no existing Minecraft/BBB/Macaw cell is duplicated and all 237 AMC-owned gaps appear once: native Button/Pressure Plate, BBB Column/Urn/Moulding/Fence/Frame, five thin paths, six pavings, four engraved patterns with matching slabs/stairs, four standard windows, parapet, Gothic window, arrow slit, and louvered shutter.
2. Verify placed forms retain provider state behavior: native button/pressure-plate redstone states, connected BBB fence/frame, engraved-pattern states, facing Dumble Paving, window extension/open states, parapet facing, Gothic/arrow multi-block states, shutter hinge/open state, and thin path collision.
3. Compare representative forms with BBB/Macaw originals: silhouette, negative space, proportions, and texture treatment must remain recognizable while only material changes.
4. With IBF C11 present, confirm material changes preserve architectural form within each literal C4 family and do not cross profile boundaries.

Stop and report any missing item, provider replacement, invalid recipe/self-drop, incorrect state transition, bad model/texture, crash, or cross-material family.
