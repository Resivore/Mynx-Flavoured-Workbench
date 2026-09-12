# BGE C68 runtime procedure

Current candidate: `cnm-nibaru-integration-4.2.12-bge.canary68.layer-axis-uv+26.2.jar`, SHA-256 `823cfe9f9fcc32c1cf73e1ddfdfc6489efda0753df64679723df56204a4bf732`, embedded version `4.2.12-bge.canary68.layer-axis-uv+26.2`, source checkpoint `9bc80ee2538a39b9fa15606611dc050a1cf6bff2`. Exact C58 remains accepted; C62 and C64 remain external runtime-failed provenance.

C68 is `NOT_DEPLOYED` and `RUNTIME_UNTESTED`. Its clean Java 25 build, archive checks, and 107/107 Minecraft 26.2 GameTests are static evidence only. The project lifecycle remains `TESTING` solely because its immutable UUID is assigned to an older Slot A cohort; that does not deploy or ready C68. Use only the dedicated Matcha Flavoured 26.2 Workbench. Never use or modify the protected Matcha Flavoured 26.1.2 profile.

## Manual checks

1. Place an Oak Log Layer at one-layer thickness and inspect every exposed edge.
2. Repeat at two and three layers; optionally confirm the four-layer full block remains normal.
3. Rotate/place the Layer on different block faces, covering all six placement facings.
4. Test material axes X, Y, and Z if obtainable through the normal BGE interaction/state system.
5. Confirm bark grain is neither stretched nor compressed on any thin face.
6. Confirm end-grain faces still use the end texture and remain correctly oriented.
7. Confirm an ordinary non-axis material such as Oak Planks appears unchanged. Record only actually observed `PASS`, `FAIL`, or `INCONCLUSIVE` evidence.
