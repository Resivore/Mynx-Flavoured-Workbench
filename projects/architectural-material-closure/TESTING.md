# C1 masonry-detail verification

Exact current candidate: `architectural-material-closure-0.1.0-canary1.jar`,
116,558 bytes, SHA-256
`49AE754AFDA6A1FAD959F11E1C51027A5B2DBC00CB5108CF69A70D3264C63999`,
built at `2026-09-25T02:51:55.4416033Z` from source checkpoint
`f69ff26b35b5898384f991ae06722de1c1099b2f`. It is `STATIC_PASS /
RUNTIME_UNTESTED`; no deployment or runtime result is recorded.

## Controlled static checks

From the repository root with Java 25:

```powershell
& projects/building-but-better/gradlew.bat -p projects/architectural-material-closure test check --no-daemon
```

The focused contracts pin the 40 owned IDs in their literal material/form
order, reject provider namespaces and unapproved Blackstone/Quartz expansion,
and the production-JAR check verifies all 40 generated item models exist and
no foreign classes are packaged.

## Manual C1 matrix

Use an owner-approved isolated Minecraft 26.2 Fabric environment. Do not
interpret installing the JAR, reaching the title screen, or static validation
as a pass.

1. Confirm the exact filename and SHA-256 before launch. Verify the eight
   material groups each expose Column, Urn, Moulding, Fence, and Frame exactly
   once in Creative Building Blocks and Creative Search.
2. Place, break, and pick-block every form for one ordinary stone material,
   Mossy Stone Brick, Cobbled Deepslate, Mud Brick, and Dark Prismarine. Check
   the original model, own self-drop, native hardness/sound baseline, and
   pickaxe behavior.
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
