# Codex Log

## 2026-09-25T02:52:27Z — Create Architectural Material Closure Canary 1
- Revision: 1
- Source checkpoint: `f69ff26b35b5898384f991ae06722de1c1099b2f`
- Changes: Created AMC as an additive Fabric content project and implemented exactly 40 original masonry-detail blocks: Column, Urn, Moulding, Fence, and Frame for Andesite, Diorite, Granite, Brick, Mossy Stone Brick, Cobbled Deepslate, Mud Brick, and Dark Prismarine. The literal scope follows the audited Macaw Paths profiles where BBB supplied no corresponding five-detail family. C1 generates its own models, item definitions, self-drops, stonecutting recipes, advancements, pickaxe tag, and language entries using only vanilla base textures; no third-party content is copied or bundled. AMC contributes no ShapeMap/equivalence implementation; IBF remains that sole authority.
- Build/static: Java 25, Gradle 9.5.1, and Fabric Loom 1.17.19 completed `test check --no-daemon` with both focused contract tests passing and production-JAR isolation verifying the 40 generated item models and no foreign classes. This is static evidence only.
- Runtime: No Minecraft deployment, launch, testing-profile access, server-state mutation, or manual runtime observation occurred. C1 remains runtime untested.
- Artifact: Current/unaccepted C1 is `architectural-material-closure-0.1.0-canary1.jar`, 116,558 bytes, SHA-256 `49AE754AFDA6A1FAD959F11E1C51027A5B2DBC00CB5108CF69A70D3264C63999`, built at `2026-09-25T02:51:55.4416033Z` from source checkpoint `f69ff26b35b5898384f991ae06722de1c1099b2f`.
- Result: ACTIVE — C1 is `STATIC_PASS / RUNTIME_UNTESTED`, with no accepted or rollback release and no blocker.
- Next state: Release a separately audited IBF literal catalog integration before relying on C1 members for switching, then obtain owner-supplied runtime observations against this exact artifact before acceptance or a successor.
