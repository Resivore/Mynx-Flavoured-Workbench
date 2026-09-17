# Testing

C7 (`0.1.0-canary7`) is the exact current candidate: `slab-decorations-0.1.0-canary7.jar`, 51,319 bytes, SHA-256 `b4e6bd4891f85906009911a699c8359fc2b52e690dc9b7ef70e30a1ed88272ff`, source checkpoint `36ee39bed00507a1632155ac3813842aba49f145`. It is `ACTIVE / CONTROLLED_VALIDATION_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED` and unaccepted. C6 is the exact runtime-tested predecessor: `0.1.0-canary6`, `slab-decorations-0.1.0-canary6.jar`, SHA-256 `289330c35e79469d147bc8243f6ff69cb2e2f673e65055e6297219bb24219863`.

C6 runtime evidence is deliberately narrow: cave vines could grow from Stone slabs and ordinary non-berry segments had the intended slab-relative position; when glow berries appeared, the berry-bearing visual showed an apparent `0.5`-block vertical gap/displacement. C7 leaves the offset calculation and non-berry behavior unchanged, but applies the final model wrapper to the lit `berries=true` model path as well. Automated validation did not render a Minecraft client or load RU; Java 25 validation passed 5 focused JUnit tests and 30 headless server GameTests. The temporary hash-verified comparison inputs were not retained in `originals/`.

## Runtime matrix

1. RU Tassel and Clover on supported bottom slabs: rendered model, outline, and targeting are each exactly `-0.5 Y`.
2. A vanilla flower or grass on a bottom slab remains exactly `-0.5 Y`.
3. Hanging Roots and Spore Blossom under top slabs are exactly `+0.5 Y`.
4. Cave Vines beneath Stone top slabs retain their correct slab-relative position before berries, while growing glow berries, and after harvesting/removing berries; verify both head and plant segments.
5. Weeping Vines beneath a valid top slab preserve placement, growth, and `+0.5 Y` representation.
6. RU Dropleaf beneath the matching Stone top slab preserves placement, downward growth, one anchor, and `+0.5 Y` representation.
7. Bottom/double ceiling supports and top/double upward supports remain unshifted; waterlogged, unsupported, and foreign geometry remain rejected.
8. Save/chunk/world reload preserves attachment and representation.

Stop and record only observed `PASS`, `FAIL`, or `INCONCLUSIVE` behavior for this exact C7 identity. C6's berry-only cave-vine visual failure is predecessor evidence, not C7 runtime validation. C5 user-reported failures were: Tassel and Clover appeared about `+0.5` block too high on bottom slabs; ceiling foliage appeared about `0.5` block too low under top slabs; Cave Vines and RU Dropleaf could use full Stone but not the corresponding Stone slab. No broader RU conclusion is inferred.
