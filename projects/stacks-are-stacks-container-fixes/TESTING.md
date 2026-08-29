# Testing

Canary 1 is the current unaccepted candidate and remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`. Migration verification preserved all 11 source/build/test blobs and the retained JAR byte-for-byte from the coherent legacy checkpoints, then passed all 9 focused tests in 3 suites with Temurin Java 25.0.4.1+1, Gradle 9.5.1, and Fabric Loom 1.17.19. Those results are build/static evidence, not Minecraft runtime evidence.

The fresh verification build produced a 3,683-byte JAR at SHA-256 `a42f0dbb5e99425e289201eed9fdb9379c775882dd8f7758baaaed2430fb45f3`, so it did not replace the exact retained 3,677-byte artifact. All class files, `fabric.mod.json`, and the JAR manifest matched byte-for-byte; the sole entry difference was CRLF rather than LF line endings in the otherwise identical mixin JSON. The retained `stacks-are-stacks-container-fixes-0.1.0-canary1.jar` at SHA-256 `ab3634c31c2f3b231908cf392ed5ada074e624e3dfba45b5707d8b38958e1eed` remains authoritative.

Legacy records preserve two pre-fix observations: a chest retained three saddles while the client rendered the slot as one with no `3`, and normally non-stackable stacked contents disappeared from the Easy Shulker Boxes tooltip preview. These are historical defect observations only; Canary 1 was never deployed or launched and has no runtime pass.

## Runtime handoff

Run this matrix only after a Test Slot owner deploys the exact retained Canary 1 alongside exact Stacks Are Stacks `2.1.2-1.26.2`. The legacy representative configuration was `StackSize=64`, `AffectAll=false`, `Exclude=[]`, and `ConsumableFix=true`; Easy Shulker Boxes `26.2.3` with embedded Item Interactions `26.2.2` is needed only for the preview case.

1. Launch and connect normally; confirm both mods initialize without a mixin, invoker, or client-start error.
2. Obtain one stack of three saddles, put it in a chest, close and reopen the chest, and confirm the slot visibly renders `3` while retaining the actual count.
3. Move, split, merge, and shift-click that saddle stack; confirm visible and actual counts remain correct through each operation and another close/reopen cycle.
4. Put three saddles and another eligible normally non-stackable item in a shulker, hover it through the Easy Shulker Boxes preview, and confirm every item icon and applicable count is visible.
5. Place three stone as a vanilla-stackable control in the same chest and shulker preview; confirm its icon and count behavior is unchanged.
6. If the test configuration defines an excluded or otherwise ineligible item, confirm this add-on does not make it stackable or change its client presentation.
7. Sanity-check player-inventory display and reconnect once; confirm the aligned client holder maximums remain stable.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if the exact dependency/candidate identities cannot be established, startup fails, a retained count is visually clamped or lost, a shulker entry remains blank, an ineligible item changes behavior, the vanilla control regresses, or reconnecting destabilizes the result. Do not promote Canary 1 without a controlled pass of every applicable case.
