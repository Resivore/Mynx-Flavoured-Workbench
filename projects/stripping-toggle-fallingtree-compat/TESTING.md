# Testing

This migration performed no deployment, Minecraft launch, runtime-slot change,
accepted-stack change, or runtime test. The current accepted evidence remains
the frozen client and singleplayer result for exact
`strippingtoggle-fallingtree-compat-0.1.1-canary2.jar`, 11,987 bytes, SHA-256
`D948C23DB8B6C73E764F96422D8359AFB85BA39689352E512FD7B4AA91644846`.
Dedicated-server coverage has not been performed.

## Retained runtime evidence

The user reported that all five focused C2 checks passed and accepted the exact
unchanged artifact:

1. StrippingToggle OFF broke only the targeted log while standing and while
   crouching.
2. StrippingToggle ON allowed normal FallingTree whole-tree behavior while
   crouching.
3. ON also allowed matched loaded-chunk tree comparisons, including standing,
   without the material Canary 1 regression being observed in the focused
   comparison.
4. Reconnecting while OFF kept the next log single-block, and
   `/fallingtree toggle` remained status-only and could not diverge from
   StrippingToggle while ON or OFF.
5. Save and normal quit completed, and the matching log inspection passed the
   focused compatibility scope.

The matching `latest.log` is recorded by provenance only: 388,233 bytes,
SHA-256
`55E2CF519271EECDB14EE7EB0E2841132E37C946888082688D9B9825DCC09911`.
It corroborates exact C2 identity, join, clean leave, and shutdown, but also
contains four server-behind warnings and records `random_tick_speed` being set
to 20 without the preceding value. The elevated tick speed is only a possible
confound for the earlier comparison; the evidence does not prove that C1
caused every stall or that C2 eliminated every stall.

Exact C1,
`strippingtoggle-fallingtree-compat-0.1.0-canary1-authoritative-toggle.jar`,
13,568 bytes, SHA-256
`B5960558C42F6A5597504640A19297670BA83EF7EF79834FA68B6358E4C1AD08`,
remains retained solely as a functional-pass/performance-fail comparison.
Its nine functional checks passed, but actual whole-tree felling was materially
laggier, so C1 is not accepted and is not a rollback release.

## Preconditions for any future regression run

1. Acquire explicit runtime ownership and use only the dedicated Minecraft
   26.2 Fabric Workbench or the separately authorized actual server host. Never
   use the protected gameplay instance.
2. Verify the exact C2 filename, size, SHA-256, embedded mod ID
   `strippingtoggle_fallingtree_compat`, and version `0.1.1-canary2` before
   deployment. Do not substitute a rebuild for the retained accepted binary.
3. Use StrippingToggle `1.2.6+26.2`, FallingTree embedded version `25`, Fabric
   Loader `0.19.3` or newer, and Fabric API `0.157.0+26.2`.
4. Require `config/fallingtree.json` semantic `sneakMode: IGNORE`. The frozen
   accepted file was 1,498 bytes at SHA-256
   `317E6D170D9CB2B709719DAFECAABE6D0FEE06AAB39B4DECA6F96E62497E6C6F`.
5. Preserve the existing accepted stack and all unrelated runtime state. Do not
   enable C1 or another companion build alongside C2.

## Client and singleplayer regression procedure

1. Join the test world with StrippingToggle OFF. Break one matched eligible log
   while standing and another while crouching. Only the targeted log may break
   in either posture.
2. Turn StrippingToggle ON and fell matched eligible trees while crouching and
   standing. FallingTree's normal behavior must run in both postures, subject to
   its unchanged creative, tag, tree, tool, enchantment, permission, durability,
   traversal, drop, leaf, and other non-activation checks.
3. Compare matched loaded-chunk trees with the same axe and tree type against
   the ordinary FallingTree control. Record perceived delay and server-behind
   warnings without assigning a cause from timing alone.
4. Disconnect and reconnect with StrippingToggle OFF and confirm another log
   remains single-block. Repeat after a local-player replacement such as respawn
   or dimension transition and confirm the state is reapplied.
5. Run `/fallingtree toggle` while ON and OFF. It may report the mirrored master
   state, but it must not mutate or diverge from StrippingToggle.
6. Save and quit normally. Inspect the matching log for identity, required
   Mixin, payload, tag, eligibility, tree-breaking, and shutdown errors.

## Still-unperformed dedicated-server procedure

Run this only as a separately authorized server-validation task. The client
must carry exact StrippingToggle and C2; the dedicated server must carry exact
FallingTree and C2 without linking the client-only StrippingToggle class.

1. Start the dedicated server and inspect both logs for environment,
   entrypoint, required-Mixin, payload-registration, and version failures.
2. Join first with StrippingToggle OFF. Confirm the unknown/pre-sync server
   state fails closed and, after synchronization, eligible logs remain
   single-block while standing and crouching.
3. Turn StrippingToggle ON and confirm normal FallingTree behavior while
   standing and crouching, then return OFF and confirm single-block behavior.
4. Disconnect/reconnect, respawn, and change dimension once. Confirm client and
   server remain synchronized after each player replacement and no stale or
   contradictory tag state appears.
5. Run `/fallingtree toggle` in both states and confirm it remains status-only.
6. Leave cleanly and stop the server cleanly. Confirm the companion-owned native
   tag is cleared and inspect both logs before recording any dedicated-server
   result.

Stop and preserve the exact artifacts and matching logs on any startup or
environment-link failure, payload rejection, state divergence, wrong posture
result, command mutation, stale tag, tree-behavior regression, material stall,
crash, or unclean shutdown. After an unclean process kill followed by removing
the companion, clear possible native-tag residue with
`/tag <player> remove fallingtree-disabled`; clean leave and shutdown perform
that cleanup automatically. Do not infer dedicated-server success from the
existing client or singleplayer evidence.
