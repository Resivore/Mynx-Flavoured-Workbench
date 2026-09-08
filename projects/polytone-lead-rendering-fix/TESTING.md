# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Canary 1 is `polytone-lead-rendering-fix-0.1.0+26.2-canary1.jar`, embedded version
`0.1.0+26.2-canary1`, SHA-256
`980353b07d27b41bf0b299cc31b827d22498f0f3692e1c7fe321949ac0253714`, source
`5d43ea9309217040186c9103269cdd7779eacd22`. It must be installed alongside Polytone in the dedicated Matcha Flavoured 26.2 Workbench only through a future serialized Test Instance Manager transition. The custom Polytone textured-leash feature is intentionally bypassed: normal Minecraft leash rendering is the expected appearance.

## Runtime matrix

1. With no shader enabled, leash one or more mobs. Confirm a lead is visible with normal vanilla appearance and coloration, with no rainbow or corrupted geometry.
2. Enable the normal Workbench Iris + Complementary shader configuration, leash one or more mobs, and confirm visible normal leads, no rainbow/corrupted coloration, no missing geometry, and no rendering crash.
3. Move each leashed entity through ordinary movement. Confirm attachment, sag, motion, range behavior, and detachment remain normal; this patch must not alter leash gameplay.
4. Check representative unrelated Polytone features used by the stack. They must remain active. The absence of Polytone's custom textured leash is expected and is not a failure.

Stop and record `FAIL` or `INCONCLUSIVE` if Minecraft crashes, a mixin injection fails, a lead disappears, vanilla leash rendering is not reached, rainbow/corrupted geometry remains, leash behavior changes, or unrelated Polytone rendering is disabled. Do not infer a runtime PASS from build, archive, bytecode, or launch-only evidence.
