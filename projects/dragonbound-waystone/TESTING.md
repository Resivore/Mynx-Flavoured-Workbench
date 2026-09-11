# Testing

Canary 10 is statically validated but has not been deployed or runtime tested. Its exact retained artifact is `dragonbound-waystone-0.1.0-canary10.jar`, embedded version `0.1.0-canary10`, 62,619 bytes, SHA-256 `593F7C722218BED7536445432FA903563BD3AA90781FCBECCE1F7647AFEF0DBD`. It preserves Canary 9's corrected four-pixel Waystone base UVs, Canary 8's body-centered portal ramp (one to four particles per server tick), and 30-particle confirmed-arrival burst. Only the shared player-targeted foreground companion changes: eye position plus normalized look direction times 0.55 blocks, Y -0.15; channel ramp three to eight particles with `0.50 / 0.35 / 0.50` spread; confirmed-arrival burst 24 particles with `0.60 / 0.45 / 0.60` spread.

The user reports that Canary 9's player-targeted foreground approach works architecturally but is still too subtle in first person: channel particles and the arrival burst remain difficult to see. That feedback does not establish a Canary 9 runtime `PASS`. Because this world is intended primarily for solo play, Canary 10 deliberately prioritizes first-person visibility over C9's conservative third-person density.

The accepted baseline and rollback remain exact `dragonbound-waystone-0.1.0-canary5.jar`, SHA-256 `FF29582CC50F391A3EBB79B5AB1DFF2F8FB7ABB92363BCA46FB8A9608A481AD3`. The exact Canary 4 rollback, SHA-256 `2EA97D762EE646F97954ABC6C919ECEA2F337B91096894ADFC13E5AF34982634`, retains its focused user-reported runtime pass. Canary 6's user-reported `FAIL` remains limited to its pre-arrival audiovisual timing.

## Preconditions

1. Test only under explicit runtime-slot ownership in the dedicated Minecraft 26.2 Fabric Workbench. Never use the protected gameplay instance.
2. Before deployment, verify the exact filename, embedded `dragonbound_waystone` / `0.1.0-canary10` identity, 62,619-byte size, and SHA-256 above.
3. Use Fabric Loader `0.19.3` or newer and Fabric API `0.156.0+26.2` or newer. Dragonbound has no current Matcha Heart or JEI runtime dependency.
4. Preserve the user's world and configuration state. Use disposable state for configuration-migration or destructive loss-protection checks.

## Focused C10 first-person visual matrix

1. In first person, perform representative valid Imbued Void Pearl and Dragonbound Staff teleports. From the first channel tick, confirm portal particles are plainly visible rather than easy to miss; they must increase noticeably as completion approaches, occupy a useful region around the center/periphery of the view, and still allow the player to see what they are looking at. Look horizontally, upward, and downward and move normally during representative channels: the near-camera foreground effect must remain visible and must not lag far behind.
2. For both items, confirm confirmed arrival produces an immediately obvious, brief foreground portal volume substantially stronger than the ongoing channel; it must not linger as a persistent visual obstruction. Confirm one Enderman sound still occurs only at the confirmed-arrival moment. Confirm Pearl consumption remains success-only. Confirm Staff retains its ordinary one main-hand swing and unchanged success-only cooldown.
3. Cancel channels through movement and accepted damage, and attempt rejected/invalid starts, death, disconnect, dimension/item/slot changes, unsafe state, and obstructed destinations. Confirm body and foreground buildup stop or never begin as appropriate, with no speculative arrival burst, sound, Pearl consumption, or Staff cooldown.
4. In third person, repeat representative Pearl and Staff teleports only to verify nothing is broken, body-centered buildup remains plausible, and there is no pathological giant duplicate cloud. Do not reject C10 merely because its foreground companion is noticeably heavier than C9 if first-person visibility is significantly better.
5. Preserve the accepted checks for exact destination, no nearby fallback, unique anchor/loss protection, one-click mouse-release-independent channeling, recipes/discovery, configuration, Staff combat neutrality, four-pixel base geometry and corrected side UVs, and normal shutdown. Review `latest.log` after normal quit and record only observed behavior.

Stop and record `FAIL` or `INCONCLUSIVE` if foreground effects remain hard to see in first person, obscure normal vision, lose visibility with look direction or normal movement, emit speculative/missing/duplicated success feedback, regress the UV correction, or any preserved behavior differs. Dedicated-server behavior remains a separately scheduled scope.
