# Testing

Canary 9 is statically validated but has not been deployed or runtime tested. Its exact artifact is `dragonbound-waystone-0.1.0-canary9.jar`, embedded version `0.1.0-canary9`, 62,633 bytes, SHA-256 `9635C870959C7ED2FB84758DE47079C3F986CF3DDD9AAE80417EFD59A1A4341B`. It retains Canary 8's world-visible body-centered channel ramp (one to four portal particles per server tick) and 30-particle confirmed-arrival burst. It corrects only the four vertical base UVs to `[0, 12, 16, 16]` and sends a small player-targeted companion stream at the player's eye position plus their normalized look vector times 0.70 blocks, lowered 0.15 blocks: one to two channel particles per tick and an eight-particle arrival burst.

The user reported limited Canary 8 visual observations: its four-pixel base geometry was correct but the side UVs still behaved as two pixels tall; its portal effects looked good in third person; and its channeling/success effects were barely visible or effectively invisible in first person. These observations do not establish a full Canary 8 runtime `PASS`.

The user runtime-tested exact Canary 6 and reported `FAIL` limited to audiovisual timing: the Enderman sound occurred when channeling began and portal particles appeared during the lead-up, before the player teleported. The user did not report a failure of its channel or teleport mechanics. Canary 6 is therefore not runtime-untested; its exact artifact is retained for provenance only and Canary 5 remains the accepted baseline and rollback.

The accepted baseline and rollback remain exact `dragonbound-waystone-0.1.0-canary5.jar`, SHA-256 `FF29582CC50F391A3EBB79B5AB1DFF2F8FB7ABB92363BCA46FB8A9608A481AD3`; the user's focused confirmation applies only to those unchanged Canary 5 bytes. No Canary 5 `latest.log` exists, so do not infer that each step below was individually re-observed on Canary 5.

The exact Canary 4 rollback, SHA-256 `2EA97D762EE646F97954ABC6C919ECEA2F337B91096894ADFC13E5AF34982634`, has a user-reported focused runtime pass including the approximately two-second channel and the narrowly gated legacy-default configuration migration. Its matching clean log is 388,269 bytes at SHA-256 `DA8B818CD02FC7A87A52D1C6FB4D62FEB927A1ED0AA996EAAA7C2145125A8A83`. Earlier exact Canary 3 completed all 13 reported channel, geometry, destination, Staff, discovery, and shutdown checks; that predecessor evidence does not substitute for unreported Canary 5 rows.

## Preconditions

1. Test only under explicit runtime-slot ownership in the dedicated Minecraft 26.2 Fabric Workbench. Never use the protected gameplay instance.
2. Verify the exact artifact filename, embedded `dragonbound_waystone` / `0.1.0-canary9` identity, 62,633-byte size, and SHA-256 above before deployment.
3. Use Fabric Loader `0.19.3` or newer and Fabric API `0.156.0+26.2` or newer. Dragonbound has no current Matcha Heart or JEI runtime dependency.
4. Preserve the user's world and configuration state. Use disposable test state for configuration-migration or destructive loss-protection checks.

## Focused C9 visual matrix

1. Start the game, enter the intended test world, and place the Dragonbound Waystone. In world and the relevant inventory/model contexts, confirm its base remains four pixels tall (`16 x 4 x 16`), all four End Stone Bricks side faces map cleanly across the full four-pixel height with no two-pixel stretching/cropping, and the top, bottom, raised plate, and rims are unchanged. Confirm ordinary placement, orientation, interaction, and protected unique-anchor behavior remain normal.
2. Confirm exactly the current recipes: Waystone blank / ` E ` / `BFB`, Staff ` P ` / ` F ` / ` S `, and shapeless Ender Pearl plus Dragon's Breath for Imbued Void. Confirm all three appear through vanilla recipe-book and ordinary JEI discovery without a custom JEI category.
3. In first person, test both the Imbued Void Pearl and Dragonbound Staff. For each: confirm particles are visible from the beginning but subtle, become more noticeable toward completion, frame rather than substantially obscure the crosshair, and successful arrival has an obvious but non-flashing foreground burst. Confirm one Enderman sound remains timed after confirmed arrival only. Confirm Pearl success-only consumption; for Staff, also confirm its ordinary one main-hand swing and unchanged success-only cooldown.
4. In third person, repeat representative Pearl and Staff teleports. Confirm the existing body-centered buildup and natural 30-particle arrival burst remain good, while the player-targeted companion creates no doubled or excessive observer cloud.
5. Cancel a pending channel through movement and accepted damage. Confirm its body and foreground buildup stop immediately, with no arrival sound/burst or Pearl consumption. Confirm rejected starts, death, disconnect, dimension/item/slot changes, unsafe state, and obstructed destinations do not emit speculative success particles.
6. Preserve the accepted checks for exact destination, no nearby fallback, unique anchor/loss protection, one-click mouse-release-independent channeling, recipes/discovery, configuration, Staff combat neutrality, and normal shutdown. Review `latest.log` after normal quit and record only observed behavior.

Stop and record `FAIL` or `INCONCLUSIVE` if first-person effects remain hard to see, obscure the view, third-person effects become doubled/excessive, the Enderman sound occurs before confirmed arrival or after failure, arrival feedback is speculative/missing/duplicated, the base side UVs remain stretched/cropped, or any preserved behavior differs. Dedicated-server behavior remains unvalidated until the user explicitly schedules that separate scope.
