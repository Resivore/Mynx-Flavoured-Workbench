# Testing

Exact `matcha-noxious-redstone-0.2.1+mc26.2.jar` is accepted for permanent
Workbench-stack membership, but its gameplay/runtime result remains
**RUNTIME_UNTESTED**. Acceptance and the verified artifact identity are not a
runtime pass.

## Preconditions

1. Test only under explicit runtime-slot ownership in the dedicated Minecraft
   26.2 Fabric Workbench. Never use the protected gameplay instance.
2. Acquire the private artifact only from
   `Resivore/Minecraft-26.2-Workbench@7b98587352e318cfa07801ad708bf164ac432903:projects/test-instance-manager/accepted-artifacts/matcha-noxious-redstone-0.2.1+mc26.2.jar`.
   Verify the exact filename, 5,520-byte size, SHA-256
   `31E6F1FAE99BFC787ED91FABC33E8B7CF258F1DEDA4DBE0F000E693EAE3D7C3D`,
   mod ID `matcha_noxious_redstone`, version `0.2.1`, and client environment
   before deployment.
3. Use Fabric Loader `0.19.3` or newer, Java 25 or newer, and Minecraft
   `>=26.2 <26.3`. Record the complete enabled stack and matching log.

## Remaining focused runtime checks

1. Launch once without Particle Interactions. Confirm the exact
   `0.2.1 loaded: density-compensated noxious redstone active` message and no
   missing-target, class-loading, or Mixin error.
2. Repeatedly power visible redstone dust. Confirm ordinary red dust particles
   are replaced by sparse native noxious-gas particles, with no double emission,
   while redstone signal behavior and unrelated particle types remain unchanged.
3. Launch separately with the intended `eg_particle_interactions` version.
   Exercise both its continuous `redstone_dust` effect and a
   `redstone_interaction` burst. Confirm both become native noxious gas, the
   continuous effect is approximately one emission per 16 attempts, the
   interaction effect is approximately one per six attempts, and neither
   original redstone effect remains visible.
4. Return to the title screen and quit normally. Review `latest.log` for
   Matcha Noxious Redstone, Particle Interactions, Mixin, particle, resource,
   or shutdown errors and record only behavior actually observed.

Stop and record `FAIL` or `INCONCLUSIVE` if either launch fails, the exact
identity differs, red dust remains or doubles, noxious gas is absent, unrelated
particles or redstone behavior change, either optional integration path fails,
or relevant errors appear in the log.
