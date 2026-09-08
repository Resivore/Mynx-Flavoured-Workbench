# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Canary 2 is `polytone-lead-rendering-fix-0.1.0+26.2-canary2.jar`, embedded version
`0.1.0+26.2-canary2`, SHA-256
`c3e3e63df63e5671ce1dd5f205813e6a4fc814e06f6f2ffc93bcdb21e63a57d3`, source
`eb2093ee22b70549f29e619e168f7ea191a5258b`. Install it alongside Polytone only through a future
serialized Test Instance Manager transition in the dedicated Matcha Flavoured 26.2 Workbench. Do
not touch the protected Matcha Flavoured 26.1.2 gameplay profile. C2 retains the Polytone textured
leash bypass and supplies the native 26.2 rope geometry, brown color factors, and light interpolation
through an entity-format buffer for Iris compatibility.

## Runtime matrix

1. With no shader enabled, leash a mob. Confirm that the rope is visible, normally brown, not rainbow/corrupt, and not grayscale.
2. Enable the normal Workbench Iris + Complementary shader configuration and repeat. Confirm a visibly brown rope, no rainbow/corruption, no grayscale result, and no missing geometry.
3. In bright daylight and shade/darker lighting, confirm ordinary brightness changes while the rope retains a brown hue rather than collapsing to neutral gray.
4. Move the leashed mob. Confirm normal attachment, sag, movement, range, and detachment; test a fence/leash-knot case and a directly held lead where practical.
5. Confirm representative unrelated Polytone functionality remains available. C2 must not affect generic entity rendering.

Stop and record `FAIL` or `INCONCLUSIVE` if rainbow coloration returns, the rope remains grayscale, geometry disappears, lighting becomes flat/arbitrary, leash behavior changes, a mixin injection fails, or unrelated Polytone/entity rendering changes. Build, archive, bytecode, and launch smoke evidence are not visual runtime validation.
