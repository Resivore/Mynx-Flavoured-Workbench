# Testing

C46 remains runtime-unvalidated. Test the exact retained C46 JAR first without BGE, then with the exact BGE C53 JAR; build, fixture, and GameTest results alone do not authorize promotion.

## Standalone Nibaru regression

1. Launch Minecraft 26.2 with C46 and no BGE installed. Confirm Nibaru loads without missing-dependency or registration errors and that representative slabs, stairs, and walls can be crafted or stonecut, placed, oriented, broken, and picked normally.
2. Exercise representative ordinary stone/wood blocks plus pillar-axis, Glazed-pattern, copper oxidation/wax, leaves, glass, gravity, Honey/Slime, spreadable soil, Dirt Path, and coral families. Confirm each derived block preserves its canonical parent, visual role, copied settings, axis/pattern, transitions, drops, movement, lifecycle, and other specialized material semantics; do not accept collapsed variants.
3. Confirm no Layer geometry is registered by standalone Nibaru and that the absence of BGE does not alter existing Nibaru behavior.

## Paired C46/BGE C53 contract

Deploy C46 (`3281d110f062db62e721d838a35ce915ca73dd41af098a013b52952561ceae7d`) with BGE C53 (`57a4599adb3f4c58ae7b99a0148a38760fdb3da59847ca3ec046379db323b7c8`). Verify BGE resolves each representative Layer through Nibaru's canonical material profile, preserves visual/material/pattern/axis semantics, and consumes the typed DROP_BASE contract so Grass, Mycelium, Podzol, and Dirt Path Layers drop the corresponding Dirt Layer. Confirm BGE remains the geometry owner and does not create a competing material selector.

## Runtime validation gate

Promote neither candidate until the exact pair has been deployed through the controlled two-slot workflow and both projects' documented runtime checks pass. Stop and record a failure or inconclusive result for any crash, missing or duplicate geometry, recursive generation, variant collapse, changed material behavior, bad transition/drop, or standalone BGE dependency.
