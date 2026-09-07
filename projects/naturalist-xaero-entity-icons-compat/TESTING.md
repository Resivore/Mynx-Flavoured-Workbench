# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Canary 1 is `naturalist-xaero-entity-icons-compat-0.1.0-canary1.jar`, SHA-256
`df5a2322b4b6c745fd1fe4b846075629869242b9c6ebb61c3480857f3f3c5de2`.
The two clean offline Java 25 builds, JUnit contract checks, Xaero/Naturalist binary checks, and archive inspection are static evidence only. Both canonical Workbench test slots were occupied at manager revision 114. Do not replace either cohort; deploy this candidate only through a separately authorized serialized transition in the dedicated Matcha Flavoured 26.2 Workbench.

## Runtime matrix

With exact Naturalist C8, Xaero Minimap, Xaero × EMF Canary 9, and Ribbits × Xaero Canary 5 present, first verify the following native regression controls still use Xaero's normal successful path: Brown Bear (`naturalist:bear`), Bird, Butterfly, Catfish, Caterpillar, Crab, Deer, Firefly, Snake, and Snail.

Then spawn and observe every Canary 1 target: Rhino, Lion, Elephant, Mammoth, Zebra, Giraffe, Hippo, Vulture, Boar, Dragonfly, Anglerfish, Ray, Blobfish, Piranha, Alligator, Bass, Lizard, Lizard Tail, Tortoise, Duck, Starfish, Clam, Giant Isopod, Jellyfish, Whale, Mole, Rat, Black Bear, Tiger, Komodo Dragon, Ostrich, Desert Scorpion, Jungle Scorpion, Great White Shark, Turkey, Capybara, and Hedgehog.

For each living entity, confirm Xaero displays a Naturalist-derived icon rather than the generic yellow marker plus name; confirm the visible renderer-selected model and texture; test adult/baby where Naturalist has separate model classes; test materially distinct state/model or texture branches (including Blobfish, Bass, and variant animals); and resource-reload then re-check the icon. Confirm that duck egg and dirt trail receive no fabricated mob icon.

Stop and record individual `FAIL` or `INCONCLUSIVE` observations with the exact entity ID, adult/baby/state/variant, texture, icon result, and reload result. Also stop for Mixin/classloading errors, a generic marker, wrong texture/geometry, body/limb leakage for headed animals, stale success/FAILED icon after reload, a regression of the ten native controls, an EMF/Ribbits regression, or a changed icon for vanilla/unrelated mod entities. Do not infer a runtime pass from a client launch or static test.
