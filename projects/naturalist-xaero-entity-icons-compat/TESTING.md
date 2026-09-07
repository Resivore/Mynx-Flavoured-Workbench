# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Canary 1 (`naturalist-xaero-entity-icons-compat-0.1.0-canary1.jar`, SHA-256
`df5a2322b4b6c745fd1fe4b846075629869242b9c6ebb61c3480857f3f3c5de2`, source
`ab199db39ece792fc6b38fa5edf1fe7536e89ba1`) is retained failed provenance.
Its first runtime launch reached Xaero radar-icon resource reload/world entry but aborted before any Naturalist entity observation: its `RadarIconModelPartPrerenderer.renderPart` redirect collided with accepted Xaero × EMF C9's required redirect, producing an `InjectionError` and Xaero render-frame crash. No Naturalist entity icon runtime pass is claimed.

Canary 2 is `naturalist-xaero-entity-icons-compat-0.1.0-canary2.jar`, SHA-256
`ff161f6bcbf5cd056a5e27d7cd56091ab5abfa8e69440c76f2f6ce9ba6e86c50`, source
`e9d0c04ed2ab64b077d1abfb8daaa356c5de895c`. Two clean offline Java 25 builds,
JUnit contract checks, Xaero/Naturalist binary checks, and archive inspection are static evidence only. Both canonical Workbench test slots are occupied at manager revision 117. Do not replace either cohort; deploy this candidate only through a separately authorized serialized transition in the dedicated Matcha Flavoured 26.2 Workbench.

## First Canary 2 runtime gate

With accepted Xaero × EMF C9 and Naturalist C2 both present, start or join a world and allow Xaero radar/icon resources to initialize or reload. Stop immediately unless there is no `@Redirect conflict`, `InjectionError`, `RadarIconModelPartPrerenderer` transformation failure, or Xaero render-frame crash. A clean transformation or client launch is only this smoke gate, not a Naturalist icon runtime pass.

## Runtime matrix

Only after the first Canary 2 gate passes, with exact Naturalist C8, Xaero Minimap, Xaero × EMF Canary 9, and Ribbits × Xaero Canary 5 present, first verify the following native regression controls still use Xaero's normal successful path: Brown Bear (`naturalist:bear`), Bird, Butterfly, Catfish, Caterpillar, Crab, Deer, Firefly, Snake, and Snail.

Then spawn and observe every Canary 1 target: Rhino, Lion, Elephant, Mammoth, Zebra, Giraffe, Hippo, Vulture, Boar, Dragonfly, Anglerfish, Ray, Blobfish, Piranha, Alligator, Bass, Lizard, Lizard Tail, Tortoise, Duck, Starfish, Clam, Giant Isopod, Jellyfish, Whale, Mole, Rat, Black Bear, Tiger, Komodo Dragon, Ostrich, Desert Scorpion, Jungle Scorpion, Great White Shark, Turkey, Capybara, and Hedgehog.

For each living entity, confirm Xaero displays a Naturalist-derived icon rather than the generic yellow marker plus name; confirm the visible renderer-selected model and texture; test adult/baby where Naturalist has separate model classes; test materially distinct state/model or texture branches (including Blobfish, Bass, and variant animals); and resource-reload then re-check the icon. Confirm that duck egg and dirt trail receive no fabricated mob icon.

Stop and record individual `FAIL` or `INCONCLUSIVE` observations with the exact entity ID, adult/baby/state/variant, texture, icon result, and reload result. Also stop for Mixin/classloading errors, a generic marker, wrong texture/geometry, body/limb leakage for headed animals, stale success/FAILED icon after reload, a regression of the ten native controls, an EMF/Ribbits regression, or a changed icon for vanilla/unrelated mod entities. Do not infer a runtime pass from a client launch or static test.
