# Testing

**ACTIVE — CONTROLLED VALIDATION PASS — NOT DEPLOYED — C23 RUNTIME FAIL RECORDED — C24 RUNTIME UNTESTED**

Current CSR C24 is `container-slot-reservations-0.1.0-canary24.jar`, 217,178 bytes, SHA-256 `292b1ddb6a9936c128ee05e39b17a9288345318d1bac167b6dc4ba526aafb826`, source `18d8793c78a935b79d7ccdd7a43a1a89124f8b47`, built at `2026-09-17T15:22:33.9227748Z`. Mouse Tweaks remains optional. The exact local Mouse Tweaks 2.31 inspection target is 75,872 bytes with SHA-256 `4592eff38a2e7af3487688e888a637a8cbd4767b46662df94d993a889102effe`; it is test-only and neither bundled nor a runtime dependency.

C23 runtime result: **FAIL** for the focused carried-shulker inbound target. In the supplied normal 26.2 environment with Mouse Tweaks 2.31 and MoreMouseTweaks 2.103.0+26.2 loaded, a drag from an occupied initial ordinary player slot across additional occupied slots did not provide the requested multi-slot collection into the cursor-held shulker. This does not establish whether the initial source alone succeeded. The separate empty-origin carried-shulker-to-inventory direction remains confirmed working; no unrelated result is inferred.

C24 owns only the failed occupied-origin interaction. The normal RMB press consumes an eligible filled player inventory/hotbar source while one supported shulker is on the cursor, immediately sends the whole-stack server-authoritative transaction, and latches inbound collection. `AbstractContainerScreen.mouseDragged` then directly hit-tests every cursor coordinate: each newly entered filled ordinary player slot sends one chained action, an empty player slot is a no-op but updates traversal identity, and leaving the ordinary slot surface permits later re-entry. C24 preserves C23's server source/cursor fingerprint validation, whole-shulker planner, partial remainder, reservation order, nesting rejection, and Creative synchronization. It does not depend on Mouse Tweaks being enabled or armed.

The exact Mouse Tweaks 2.31 bytecode test proves the C23 failure mechanism: a nonempty cursor can arm its RMB state, but `rmbTweakMaybeClickSlot` returns before `clickSlot` for an incompatible occupied target. A cursor-held shulker and arbitrary source stack are inherently incompatible, so C23's helper/`oldSelectedSlot` traversal could not supply later sources. C24 removes that helper mixin, observer, outbound prediction state, and plugin reference. Empty initial slots create no CSR state and keep the established native/Mouse Tweaks outbound path.

Controlled validation used Java 25, Gradle 9.5.1, Loom 1.17.19, and `clean test runGameTest build`. JUnit/contract tests passed, including the exact Mouse Tweaks 2.31 incompatibility proof and C24 screen-drag wiring; all 35 required headless Fabric GameTests passed; marker and release-artifact checks passed. This is controlled validation, not Minecraft desktop runtime evidence.

This remaining matrix is user-directed desktop runtime validation only. It does not authorize Codex to manage, inspect, launch, modify, or deploy any Minecraft profile or gameplay instance.

1. Carry one shulker, RMB-press a filled ordinary player slot, then cross filled cobblestone, logs, bread, and dirt slots. The origin and every newly entered filled slot must collect into the same cursor-held shulker.
2. Cross empty slots during that inbound drag. They must do nothing, but A-to-empty-to-A and A-to-B-to-A must each permit the later genuine re-entry once; repeated callbacks inside A must not repeat a transfer.
3. Verify planner behavior: compatible occupied internal stack first, matching reserved empty second, unreserved empty third; a mismatching reservation is skipped. Check partial capacity leaves the exact source remainder.
4. With a full shulker or illegal nested-container source, the source stays unchanged and the held gesture can still process a later valid source.
5. Change the cursor shulker, source, menu, or context mid-gesture. Both sides must fail closed; RMB release, screen close, menu change, and disconnect must end collection.
6. Regression: carry a populated shulker, RMB-press an **empty** ordinary player slot, and drag across empty slots. The confirmed outbound shulker-to-inventory behavior must remain unchanged.
7. Confirm ordinary non-shulker cursors, Mouse Tweaks absent, Mouse Tweaks RMB disabled, panel actions, reservation editing, selection, tooltip, CCAR integration, and Creative synchronization retain their established behavior.
