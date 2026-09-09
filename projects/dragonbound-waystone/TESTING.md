# Testing

Canary 6 is statically validated but has not been deployed or runtime tested. Its exact artifact is `dragonbound-waystone-0.1.0-canary6.jar`, 61,892 bytes, SHA-256 `64D2BF362A6F9D9D3ABE9C8190B95F90A3D58E2743AC2981D5ACA29585146199`. Its only intended gameplay-visible delta from accepted Canary 5 is shared server-owned vanilla Enderman teleport sound when a valid channel starts, modest portal particles while it remains active, and denser portal bursts at confirmed successful departure and arrival. There is deliberately no completion sound.

The accepted baseline and rollback remain exact `dragonbound-waystone-0.1.0-canary5.jar`, SHA-256 `FF29582CC50F391A3EBB79B5AB1DFF2F8FB7ABB92363BCA46FB8A9608A481AD3`; the user's focused confirmation applies only to those unchanged Canary 5 bytes. No Canary 5 `latest.log` exists, so do not infer that each step below was individually re-observed on Canary 5.

The exact Canary 4 rollback, SHA-256 `2EA97D762EE646F97954ABC6C919ECEA2F337B91096894ADFC13E5AF34982634`, has a user-reported focused runtime pass including the approximately two-second channel and the narrowly gated legacy-default configuration migration. Its matching clean log is 388,269 bytes at SHA-256 `DA8B818CD02FC7A87A52D1C6FB4D62FEB927A1ED0AA996EAAA7C2145125A8A83`. Earlier exact Canary 3 completed all 13 reported channel, geometry, destination, Staff, discovery, and shutdown checks; that predecessor evidence does not substitute for unreported Canary 5 rows.

## Preconditions

1. Test only under explicit runtime-slot ownership in the dedicated Minecraft 26.2 Fabric Workbench. Never use the protected gameplay instance.
2. Verify the exact artifact filename, embedded `dragonbound_waystone` / `0.1.0-canary6` identity, 61,892-byte size, and SHA-256 above before deployment.
3. Use Fabric Loader `0.19.3` or newer and Fabric API `0.156.0+26.2` or newer. Dragonbound has no current Matcha Heart or JEI runtime dependency.
4. Preserve the user's world and configuration state. Use disposable test state for configuration-migration or destructive loss-protection checks.

## Focused current procedure

1. Start the game, enter the intended test world, and place the Dragonbound Waystone. Confirm the four-cuboid End Stone Bricks model, fixed `16 x 3 x 16` gameplay shape, and protected unique-anchor behavior.
2. Confirm exactly the current recipes: Waystone blank / ` E ` / `BFB`, Staff ` P ` / ` F ` / ` S `, and shapeless Ender Pearl plus Dragon's Breath for Imbued Void. Confirm all three appear through vanilla recipe-book and ordinary JEI discovery without a custom JEI category.
3. Main-hand click the Imbued Void once. Confirm exactly one Enderman teleport sound is emitted when the valid channel begins, the default channel is approximately 40 server ticks, modest portal particles surround the player while it remains active, releasing the mouse does not cancel it, exact arrival is at block center and `blockY + 3/16`, and one Pearl is consumed only after confirmed arrival.
4. Cancel a pending channel through movement and accepted damage. Confirm the ambient particles stop immediately, no departure or arrival burst occurs, and no Pearl is consumed. Confirm death, disconnect, dimension change, held-slot or component change, relocation, unsafe state, and an obstructed exact destination fail cleanly without consumption or nearby fallback; rejected starts must remain silent and emit no successful particle sequence.
5. Place a second Waystone and confirm it becomes the sole authority. Break the active anchor in survival, including with a full inventory, and confirm the protected recoverable item path; breaking an older admin-created block must not clear the newer binding.
6. Use the Dragonbound Staff once. Confirm the same single activation sound, ambient particles, confirmed departure and arrival bursts, no second completion sound, the accepted transparent 16 x 16 sprite, the same return rules, success-only 1,200-tick cooldown, and no durability, charge, fuel, or Pearl consumption.
7. In disposable configuration state, verify `configVersion: 1`, the 40-tick default, and the 1,200-tick Staff cooldown. Confirm only the exact complete unversioned Canary 3 default document is migrated; customized, partial, extra-key, and already-versioned documents remain outside that migration.
8. Return to the title screen and quit normally. Review the new `latest.log` for Dragonbound, mixin, recipe, registry, persistence, teleport, resource, or shutdown errors, and record only actions actually observed.

Stop and record `FAIL` or `INCONCLUSIVE` if startup fails, identity or recipes differ, discovery disappears, rejected activation plays the sound, ambient particles persist after cancellation, a completion burst occurs after cancellation, successful bursts are missing or excessive, a second completion sound plays, mouse release cancels the channel, unsafe or obstructed teleport succeeds, success-only consumption or cooldown ordering changes, anchor generations cross-clear, the protected return item can be lost, or relevant errors appear. Dedicated-server behavior remains unvalidated until the user explicitly schedules that separate scope.
