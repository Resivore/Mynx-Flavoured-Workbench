# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Canary 4 is `polytone-lead-rendering-fix-0.1.0+26.2-canary4.jar`, embedded version
`0.1.0+26.2-canary4`, SHA-256
`9ba918cb2634f2736b7ca930c840b761b8c76151576e3c7a63b9f62c60055ce1`, source
`ad3407895d37e3919dcf36efa727347358aa19e2`. Install it alongside Polytone only through a future
serialized Test Instance Manager transition in the dedicated Matcha Flavoured 26.2 Workbench. Do
not touch the protected Matcha Flavoured 26.1.2 gameplay profile. C4 keeps Polytone's textured
leash bypass and pairs an Iris ENTITY-format contract with Minecraft's exact native `TRIANGLE_STRIP`
topology; C2's `entitySolid`/`QUADS` route is not used. C4 preserves C3 geometry and samples
Polytone 26.2-6.3.1's `minecraft:textures/entity/lead.png` (SHA-256
`be48270065a58e1e89031d75038ed79b149e736936c28b9f7beb49b0204be59f`) with a white `(1,1,1)`
vertex multiplier: texture supplies base brown, lightmap supplies environment light, and the retained
segment-derived normal supplies entity/shader directional lighting. It intentionally does not apply
C3's old untextured `(0.5,0.4,0.3)` tint or alternating `0.7` multiplier.

## Runtime matrix

1. Attach a lead to a fence and stand where the visible leash-knot material and long rope can be compared simultaneously. PASS requires the long rope to be in the same general tan/brown material family, without C3's substantial dark-brown mismatch; exact pixel identity is not required for small normal-orientation shading differences.
2. In direct sunlight, confirm the rope is neither near-black/dark chocolate while the knot is light tan nor washed-out white. It must have no rainbow/corruption, grayscale-only color, or solid black rope.
3. Repeat in shade. Both rope and fence knot should darken naturally while retaining comparable brown material appearance.
4. Move and rotate the camera around the rope. Confirm stable color with no sudden normal-driven blackening, and one continuous narrow rope with normal sag: no C2-style polygons, disconnected shards, ribbon sheets, or jumping geometry.
5. Move the leashed entity and/or player. Confirm C3's smooth continuity, normal sag, correct attachment points, range, and detachment, with no stretched remnants.
6. Test the normal Workbench Iris + Complementary configuration, then disable shaders and repeat the material/color and geometry checks. C4 must not be a shader-only correction.

Stop and record `FAIL` or `INCONCLUSIVE` if the rope remains substantially darker than the fence lead material, becomes incorrectly white, the primitive topology appears corrupt, the rope is grayscale or black, rainbow coloration returns, geometry disappears/disconnects, camera movement exposes major artifacts, lighting is grossly incorrect, mechanics change, a mixin injection fails, or unrelated rendering changes. Build, artifact, bytecode, and launch-smoke evidence are not visual runtime validation.
