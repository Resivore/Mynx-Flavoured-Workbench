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

## 2026-09-25T04:47:09Z — Architectural Material Closure Canary 2
- Revision: 2
- Source checkpoint: `028c7490480883e3b9cf6a26c37f980adfe601d8`
- Changes: Replaced C1's vanilla column, urn, moulding, fence, and frame approximations with BBB's stateful form classes. The tracked generator verifies the exact local BBB input and emits its derived blockstate/model topology plus material-tinted texture treatment only under ignored `build/`; the private derivative JAR remains ignored and is not published.
- Build/static: Java 25, Gradle 9.5.1, and Fabric Loom 1.17.19 completed `test check --no-daemon`. The C1 literal-ID boundary remains covered; production-JAR isolation continues to reject foreign classes.
- Runtime: No Minecraft deployment, launch, testing-profile access, server-state mutation, or manual runtime observation occurred.
- Artifact: Current/unaccepted C2 is `architectural-material-closure-0.1.0-canary2.jar`, 5,775,402 bytes, SHA-256 `5dceb2f48b8328dcf7e1e9435d3c2716eb1719aea408b8bbb1b273e02b77e0d0`, built at `2026-09-25T04:47:09.0532696Z` from source checkpoint `028c7490480883e3b9cf6a26c37f980adfe601d8`.
- Result: ACTIVE — `STATIC_PASS / RUNTIME_UNTESTED`; private local derivation is owner-authorized, while derived provider payloads remain untracked and unpublishable.
- Next state: Extend the same local-only materialization pipeline to the remaining audited Macaw masonry vocabulary, add the resulting literal families to IBF, and obtain owner-supplied runtime observations before acceptance.

## 2026-09-25T05:20:36Z — Complete Architectural Material Closure Canary 3

- Revision: 3
- Source checkpoint: `acc3a821da106dc58fd08e72741da026be291192`
- Changes: Completed the owner-authorized local-only BBB/Macaw materialization matrix across 17 clumped masonry profiles. C3 supplies faithful BBB Column/Urn/Moulding/Fence/Frame forms plus five Macaw thin paths, six pavings, four standard windows, parapet, Gothic window, arrow slit, and louvered shutter forms. The tracked generator follows exact provider blockstate/model references recursively and writes retextured protected closures only beneath ignored build/artifact paths; no protected provider bytes are tracked or published.
- Build/static: Java 25, Gradle 9.5.1, and Fabric Loom 1.17.19 passed `test check --no-daemon`; production isolation verifies 408 generated item models and no foreign classes.
- Runtime: No Minecraft deployment, testing-profile access, or manual runtime observation occurred.
- Artifact: Current/unaccepted C3 is `architectural-material-closure-0.1.0-canary3.jar`, 1,031,310 bytes, SHA-256 `5C74F1C3442D93D0877067195596344A826D118AA894E339DB7E07B1244E7417`, built at `2026-09-25T05:20:36.1241545Z` from source checkpoint `acc3a821`.
- Result: ACTIVE — `STATIC_PASS / RUNTIME_UNTESTED`; private derived resources and JAR remain local/ignored.
- Next state: Obtain owner-supplied runtime observations against exact C3 bytes before acceptance or a successor.

## 2026-09-25T20:37:28Z — Complete Architectural Material Closure Canary 4

- Revision: 4
- Source checkpoint: `8995d572ef5317779e49a896e6ca663b128a6508`
- Changes: Replaced C3's rectangular local registry with the fixed provider-first 646-cell audit: all 409 Minecraft/BBB/Macaw-owned cells remain unregistered by AMC and exactly 237 genuine gaps are supplied. Corrected behavior fidelity to use native Minecraft controls, Macaw `ConnectedWindow`, `EngravedBlock`, and `FacingPathBlock` where the provider uses them. The local-only resource generator now closes concrete provider models and textures recursively, derives native control model hierarchies, and rejects unresolved AMC or declared external references.
- Build/static: Java 25, Gradle 9.5.1, and Fabric Loom 1.17.19 completed `test check stageCanaryArtifact --no-daemon --rerun-tasks`. The C4 check proves 646 approved cells, 409 provider-owned cells, 237 AMC cells, exact generated item/blockstate coverage, resource closure, and no foreign production classes. This is static evidence only.
- Runtime: No Minecraft deployment, testing-profile access, or manual runtime observation occurred.
- Artifact: Current/unaccepted C4 is `architectural-material-closure-0.1.0-canary4.jar`, 1,771,750 bytes, SHA-256 `B7344EC4B53305CDFEB5CFD514E2570B919ECCA5432D6C9ED11B7E1846A90FD5`, built at `2026-09-25T20:37:28.9453931Z` from source checkpoint `8995d572ef5317779e49a896e6ca663b128a6508`.
- Result: ACTIVE — `STATIC_PASS / RUNTIME_UNTESTED`; private derived resources and JAR remain local and retained without publishing protected payloads.
- Next state: Obtain owner-supplied runtime observations against the exact C4 bytes before acceptance or a successor.
