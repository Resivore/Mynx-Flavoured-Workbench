# Testing

C6 (`0.1.0-canary6`) is the exact current candidate: `slab-decorations-0.1.0-canary6.jar`, 51,315 bytes, SHA-256 `289330c35e79469d147bc8243f6ff69cb2e2f673e65055e6297219bb24219863`, source checkpoint `b589e1e52825e94a41fec2932c14c5a4ba07d791`. It is `ACTIVE / CONTROLLED_VALIDATION_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED` and unaccepted. C5 remains the failed predecessor: `0.1.0-canary5`, `slab-decorations-0.1.0-canary5.jar`, SHA-256 `8de706eb59bdafd5c594f5375be6485b9edb7b30b2b2017b2c8eaa44129fe480`, source checkpoint `b589e1e52825e94a41fec2932c14c5a4ba07d791`.

C6 uses BGE C70's exact Stone profile source (`minecraft:stone_slab`) and the optional Sodium `LevelSlice` client snapshot bridge. Automated validation did not render a Minecraft client or load RU; its Java 25 build passed 5 focused JUnit tests and 30 headless server GameTests. The temporary, hash-verified Terrain Slabs comparison input was not retained in `originals/`.

## Runtime matrix

1. RU Tassel and Clover on supported bottom slabs: rendered model, outline, and targeting are each exactly `-0.5 Y`.
2. A vanilla flower or grass on a bottom slab remains exactly `-0.5 Y`.
3. Hanging Roots and Spore Blossom under top slabs are exactly `+0.5 Y`.
4. Cave Vines/glow berries beneath Stone top slabs succeed whenever full Stone succeeds; verify growth and berries.
5. Weeping Vines beneath a valid top slab preserve placement, growth, and `+0.5 Y` representation.
6. RU Dropleaf beneath the matching Stone top slab preserves placement, downward growth, one anchor, and `+0.5 Y` representation.
7. Bottom/double ceiling supports and top/double upward supports remain unshifted; waterlogged, unsupported, and foreign geometry remain rejected.
8. Save/chunk/world reload preserves attachment and representation.

Stop and record only observed `PASS`, `FAIL`, or `INCONCLUSIVE` behavior for this exact C6 identity. C5 user-reported failures were: Tassel and Clover appeared about `+0.5` block too high on bottom slabs; ceiling foliage appeared about `0.5` block too low under top slabs; Cave Vines and RU Dropleaf could use full Stone but not the corresponding Stone slab. No broader RU conclusion is inferred.
