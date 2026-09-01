# Testing

## Current accepted release and preserved baselines

Test only this exact current and accepted Matcha Death Rebalance release when
later explicit runtime authority is provided:

- version `0.1.10-canary11`
- file `matcha-heart-death-compat-0.1.10-canary11.jar`
- size `39,105` bytes
- SHA-256 `A0570179F85D32EC6740D9136AD50890B9C797500661CD2ED2325DA5B6B1E4A9`
- release source `8a0002235c6b0921f4d7f6a4cbf73b18ccc421a3`

C11 is `STATIC_PASS / READY_TO_TEST_VERIFIED / RUNTIME_UNTESTED` and is the
accepted Matcha Death Rebalance member of Workbench Stack v11 under the user's
explicit `USER_APPROVED_UNTESTED_PROMOTION`. `READY_TO_TEST_VERIFIED` records
the physically verified accepted-baseline installation; it is not Test Slot
occupancy or Minecraft runtime evidence. Java 25 and Gradle 9.5.1 offline
`clean test build --no-daemon --offline` passed all 44 tests in eight suites
with zero failures, errors, or skips. No runtime PASS is claimed.

Previous accepted C10 remains byte-for-byte unchanged as repository
provenance: version `0.1.9-canary10`, file
`matcha-heart-death-compat-0.1.9-canary10.jar`, 34,751 bytes, SHA-256
`F86442EC69ED8AFB86D52C97DB2E899223C25A659EF7C19F245E51AC642BBC90`, source
`f9c877d14956616bc3f46f3239ac3b512e1ee9c4`. It remains runtime untested.
Runtime-passed C8 remains the exact rollback: version `0.1.7-canary8`, file
`matcha-heart-death-compat-0.1.7-canary8.jar`, 31,176 bytes, SHA-256
`CB36D6917DC61B17F2D8B5CEA0D09CB4C1455E04AACA2C186ECAEBE5E9960AA2`, source
`4707b35150cc4169b2eee68b8683f9dfb27c158e`. Neither result is inherited by C11.

## Expected audiovisual profiles

- Crystal Heart: server-broadcast `minecraft:block.beacon.power_select`, volume
  `0.70`, pitch `1.15`, plus 10 `minecraft:end_rod` particles centered at 65%
  of the player's current bounding-box height with spread X/Y/Z
  `0.22 / 0.30 / 0.22` and speed `0.015`.
- Reinforced Crystal Heart: the same server-broadcast sound, volume `0.75`,
  pitch `0.90`, plus 14 `minecraft:glow` particles at the same pose-relative
  height with spread X/Y/Z `0.30 / 0.35 / 0.30` and speed `0.010`.
- Neither profile uses heart particles. Failed, blocked, off-hand, client-side,
  non-Heart, inventory-movement, and ordinary poisonous-potato paths produce
  neither success sound nor success particles.

## Focused Canary 11 runtime matrix

1. For a later managed-slot run, use the serialized Test Instance Manager only
   after explicit deployment authority and an appropriate slot are available;
   for an explicitly reported external run, bind evidence to the exact identity
   above without fabricating slot history. Do not co-install C10, C8, failed C7,
   or the retired Echo-scarcity artifact. Launch Minecraft Java 26.2 and run
   `/reload`; stop on any relevant startup, recipe, codec, Mixin, loot, JEI, or
   compatibility error.
2. Move, craft, shift-click, store, and off-hand-use a Crystal Heart. Confirm no
   health change, consumption, beacon sound, End Rod burst, or Glow burst.
   Repeat with an ordinary poisonous potato and another unrelated item.
3. From a valid normal-baseline state below the 30-heart cap, use one Crystal
   Heart in the main hand. Confirm exactly one item is consumed and exactly one
   maximum heart is added before one higher beacon tone and one compact 10-count
   End Rod burst appear around the upper torso. Confirm a nearby player receives
   the same single server event and no heart particles appear.
4. Attempt Crystal use below the normal ten-heart baseline and again at the
   30-heart cap. Confirm the existing rejection message and no consumption,
   health change, success sound, or particles.
5. From below the normal ten-heart baseline, use one Reinforced Crystal Heart in
   the main hand. Confirm exactly one item is consumed and exactly one maximum
   heart is restored before one lower beacon tone and one restrained 14-count
   Glow burst appear around the upper torso. Confirm nearby-player receipt and
   no heart particles.
6. Attempt Reinforced use at and above the normal ten-heart baseline. Confirm the
   existing rejection message and no consumption, health change, success sound,
   or particles.
7. Repeat successful uses while standing and while in a short pose such as
   swimming or crawling; the compact burst must remain chest-relative rather
   than floating above the player. Rapid valid uses must produce one feedback
   event per successful state change without client/server duplication.
8. Recheck representative C10 behavior: exact Heart item/model recognition;
   one-heart-per-death loss to the five-heart floor; persistence across
   save/reconnect; Crystal/Reinforced/Resonant recipes; Resonant Favour identity,
   sprite, glint, and JEI exposure; Silk-Touch-only Sculk drops; Ancient City
   Echo availability; advancements; functions; and unrelated Matcha behavior.

Record C11 independently as `PASS`, `FAIL`, or `INCONCLUSIVE`, with only the
behavior actually observed. Its accepted status does not imply a PASS. Stop and
preserve the exact evidence on any state, consumption, duplication, placement,
sound, particle, reload, or regression failure. Do not infer C11 runtime status
from C8, C10, static tests, artifact production, or another project tested in
the same launch.
