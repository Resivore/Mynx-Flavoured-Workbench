# C13 BGE-boundary verification checklist

Current candidate: `interchangeable-block-families-0.1.0-canary13.jar`, 70,830 bytes, SHA-256 `9FBE4627414CCC0A4EBA33C8912244FB55D47F6BE4B6ECAC1752F699F37974AB`, finalized `2026-09-27T01:36:09.9004633Z` from `1811c86b2b2221836deb299ebd11e32427e86269`. C7 remains the exact accepted baseline. C13 is `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`.

Controlled Java 25 validation used AMC C5 and BGE C102. The full unit/static/archive suite and all 26 GameTests passed. It proves 166 families, 1,607 unique members, largest family size 26, and 17 masonry families with exactly 442 eligible cells (253 provider; 189 AMC). All 204 patterned full/slab/stair cells are literal BGE-owned exclusions. The exact existing C102 AMC nine-role component is wholly outside IBF; a genuine cross-family component still fails closed.

In an owner-approved isolated Minecraft 26.2 environment with AMC C5 and BGE C102, confirm patterned geometry has one BGE/AMC nine-role family and is not joined to an IBF masonry family. Then smoke-test a real IBF family, recipe cleanup, selection, and transfers. Report any boundary collision, missing member, duplicate membership, recipe regression, or crash. This checklist is lifecycle-neutral and does not authorize Minecraft testing-profile access.
