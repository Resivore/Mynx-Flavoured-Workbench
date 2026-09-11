# Testing

Canary 12 is statically validated but has not been deployed or runtime tested. Its exact retained artifact is `dragonbound-waystone-0.1.0-canary12.jar`, embedded version `0.1.0-canary12`, 63,354 bytes, SHA-256 `6F621B16652BAF9CAAD89BA923B8792EBC0BA08706A9F507FD17AABFC3121AD6`. It preserves the C11 feet-level origin: `player.position() + (0, 0.05, 0)`, a shallow Y `plus or minus 0.05` spread, 0.65-block channel radius, 0.75-block confirmed-arrival radius, and player-targeted count-zero packets. C11 user testing established that ordinary `minecraft:portal` still visibly falls despite its positive packet Y; that focused direction failure is not a full C11 runtime PASS.

Only the player-targeted foreground type changes in C12: it is now vanilla `minecraft:reverse_portal` with speed `1.0`, Y motion `0.06-0.12`, and X/Z drift within `plus or minus 0.035`. Exact 26.2 mapped client inspection shows that `ReversePortalParticle` receives the count-zero vector unchanged at speed one and, on each live tick, adds `motion * age / lifetime` without gravity. Its 60-61 tick lifetime means every positive-Y particle rises on every live tick. The C11 foreground channel ramp remains three to eight and its confirmed-arrival burst remains 24. Body-centered third-person support remains ordinary `minecraft:portal`: one to four channel particles and a 30-particle confirmed-arrival burst.

The accepted baseline and rollback remain exact `dragonbound-waystone-0.1.0-canary5.jar`, SHA-256 `FF29582CC50F391A3EBB79B5AB1DFF2F8FB7ABB92363BCA46FB8A9608A481AD3`.

## Preconditions

1. Test only under explicit runtime-slot ownership in the dedicated Minecraft 26.2 Fabric Workbench. Never use the protected gameplay instance.
2. Before deployment, verify the exact filename, embedded `dragonbound_waystone` / `0.1.0-canary12` identity, 63,354-byte size, and SHA-256 above.
3. Use Fabric Loader `0.19.3` or newer and Fabric API `0.156.0+26.2` or newer. Dragonbound has no current Matcha Heart or JEI runtime dependency.

## Focused C12 first-person checklist

1. On a normal full block, use both an Imbued Void Pearl and Dragonbound Staff in first person. From the first channel tick, particles must originate around the feet, immediately move upward through the lower view, continue through/past the camera region without falling or arcing down, and ramp from three to eight as completion approaches. Rotate the camera and move during channeling: the source must remain ground/body-relative, not look-relative.
2. After a confirmed arrival with each item, confirm the stronger 24-particle reverse-portal burst starts around the destination feet and rises upward. Confirm one Enderman teleport sound remains synchronized with that confirmed arrival, Pearl consumption remains success-only, and the Staff retains its ordinary one main-hand swing and success-only cooldown.
3. Cancel channels through movement and accepted damage, and attempt rejected/invalid starts. No success burst or success sound may occur unless teleport is confirmed.
4. In third person, verify only that the ordinary body-centered portal effects remain intact and the targeted reverse-portal stream creates no pathological spam.

Stop and record `FAIL` or `INCONCLUSIVE` if particles appear to fall from above, do not visibly travel upward from the feet, become camera-relative, arc back down, cause a speculative or missing success effect, or regress any frozen behavior. Do not claim static validation as a Minecraft runtime PASS.
