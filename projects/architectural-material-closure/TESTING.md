# C2 local masonry-detail verification

Exact current candidate: `architectural-material-closure-0.1.0-canary2.jar`,
5,775,402 bytes, SHA-256
`5DCEB2F48B8328DCF7E1E9435D3C2716EB1719AEA408B8BBB1B273E02B77E0D0`,
built at `2026-09-25T04:47:09.0532696Z` from source checkpoint
`028c7490480883e3b9cf6a26c37f980adfe601d8`. It is `STATIC_PASS /
RUNTIME_UNTESTED`; no deployment or runtime result is recorded.

## Controlled static checks

From the repository root with Java 25:

```powershell
& projects/building-but-better/gradlew.bat -p projects/architectural-material-closure test check --no-daemon
```

The focused contracts pin the 40 owned IDs in their literal material/form
order and reject unapproved material expansion. C2's local generation task
requires the exact private BBB reference JAR, derives the authoritative model
and blockstate topology plus materialized texture treatment only into ignored
build output, and the production-JAR check verifies the generated item models
and rejects foreign classes. Do not distribute this local derivative JAR.

## Manual C2 matrix

Use an owner-approved isolated Minecraft 26.2 Fabric environment. Do not
interpret installing the JAR, reaching the title screen, or static validation
as a pass.

1. Confirm the exact filename and SHA-256 before launch. Verify the eight
   material groups each expose Column, Urn, Moulding, Fence, and Frame exactly
   once in Creative Building Blocks and Creative Search.
2. Place, break, and pick-block every form for one ordinary stone material,
   Mossy Stone Brick, Cobbled Deepslate, Mud Brick, and Dark Prismarine. Check
   the BBB-equivalent connected/stateful model, own self-drop, native
   hardness/sound baseline, and pickaxe behavior.
3. Verify each Fence connects as a fence and each Frame connects as an
   iron-bars-style frame without replacing a Minecraft, Macaw, or BBB item.
4. At a stonecutter, verify every form consumes the matching native vanilla
   material only. Confirm no recipe creates a provider-namespaced duplicate.
5. With IBF present after its separately released literal catalog integration,
   exercise the five-form family for each material and confirm no member crosses
   material or source-component boundary.

Stop and report a failure or inconclusive result on any missing/duplicate item,
provider replacement, invalid recipe, incorrect self-drop, bad connection,
crash, or cross-material interchangeability.
