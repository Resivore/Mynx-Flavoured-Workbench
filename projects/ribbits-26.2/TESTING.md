# Testing

Ribbits private Canary 2 is `NOT_DEPLOYED / RUNTIME_UNTESTED`. Use this procedure only during a future explicitly authorized Workbench runtime task after independently verifying the exact Git-ignored artifact `projects/ribbits-26.2/artifacts/ribbits-private-reconstruction-4.1.6+26.2-port-canary2.jar`, 3,124,301 bytes, SHA-256 `0AD73B7B61C6EE792EC1745056563641767AFE6811C0FDF2D3C99123C3F289DC`, and exact Git-ignored dependency `projects/ribbits-26.2/artifacts/YungsApi-26.2-Fabric-6.1.1-compat.1.jar`, 1,266,121 bytes, SHA-256 `527850C4F061FA9AB327AE0B32A3D26EBC77B51FB86234F5F6B1E3418CA084CA`. Historical Canary 1 is broken for world creation and must not be used as a rollback candidate. The public `ribbits-source-only-4.1.6+26.2-port-canary2.jar` is resource-incomplete and disposable; never deploy or classify it as Canary 2.

## Focused runtime matrix

1. Create a new disposable world and require successful datapack/registry loading and world entry.
2. Duplicate or recreate a disposable world and require successful loading and world entry.
3. Save, exit, and reload the disposable world; also change dimensions once and confirm the save remains loadable.
4. Review the complete client and server logs and confirm there is no configured-feature registry or codec error, no unbound `ribbits:*_patch` feature, and no mixin, model, resource, or payload blocker.
5. Locate or generate Ribbits village/worldgen and verify the giant lilypad, swamp daisy, toadstool, umbrella leaf, and general vegetation patches generate; exercise the village processors, blocks, items, tags, recipes, advancements, and sorcerer loot behavior.
6. Exercise all five professions and their expected goals, trades, work, home, crop-watering, fishing, music, buff, and interaction behavior without crashes or incorrect profession fallback.
7. Confirm Ribbit profession, instrument, umbrella, pride, and supporter state persist across save/reload; verify models, profession skins, umbrellas, animations, sounds, particles, maraca playback, supporter hats, and client disconnect/reload cleanup.
8. Verify all five distinctly named profession spawn eggs share the authorized temporary green texture while preserving the correct spawned profession, dispenser behavior, persistence, and pick-result identity.
9. On the actual server host during an explicitly scheduled server playtest, start another disposable world, join with a client, and verify common/client separation plus all music, maraca, and supporter payload directions without disconnects or desynchronization.
10. Save and restart once more, repeat the core entity and world-generation checks, and review the final client and server logs before assigning any runtime classification.

Stop and record the independent slot result as failed or inconclusive if exact artifact or dependency identity differs, required private resources are missing, world create/duplicate/reload fails, any registry/codec/mixin/model/payload error appears, any of the five patches is absent or malformed, state fails to persist, client/server behavior desynchronizes, or the game crashes. Do not promote or call Canary 2 runtime validated from build, archive, codec, registry, or other static evidence alone.
