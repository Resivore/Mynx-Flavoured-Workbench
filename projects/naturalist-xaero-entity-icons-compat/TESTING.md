# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Canary 2 is retained failed external-runtime provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary2.jar`, SHA-256
`ff161f6bcbf5cd056a5e27d7cd56091ab5abfa8e69440c76f2f6ce9ba6e86c50`, source
`e9d0c04ed2ab64b077d1abfb8daaa356c5de895c`. Its Xaero × EMF C9 resource-reload/world-entry smoke gate passed: the C1 competing redirect failure did not recur and the Naturalist matrix could be inspected. C2 nevertheless failed the observed entity matrix; it must not be promoted or described as runtime-passing.

Canary 3 is retained user-reported external-runtime **FAIL** provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary3.jar`, SHA-256
`510b205ce889ca92f31cf20a0eff078348598e3d6fef3cfeba15dc9363977d7b`, source
`831297c74ee4829563476b64d75e24d2342148f9`. Its observed failures were snout-only Alligator, unrecognizable Anglerfish, angled/cropped Great White Shark, oversized Brown Bear and Tiger, Clam side profile, ear-like Boar, nose plus neck Zebra, label-only Ray/Bass/Giant Isopod/Hedgehog/Vulture/Tortoise/Starfish/Lizard/Mole, blank Piranha, and worsened angled Scorpions (including a Desert Scorpion without its abdomen). Do not describe C3 as runtime-untested or runtime-passing.

Canary 4 is `naturalist-xaero-entity-icons-compat-0.1.0-canary4.jar`, SHA-256
`9aa6869ed1579655c1639206cd2225119523b3c12423f0bce84c40a0f3658c19`, source
`10ba9e25956934b740818aa01391775d165c1f98`. Two clean offline Java 25 / Loom 1.17.19 builds, 18 JUnit contract/source/binary checks, and client-only archive inspection are static evidence only. C4 is not deployed: canonical manager revision 117 retains unrelated BGE C60 in Slot A and IBF C4 in Slot B. Do not displace either cohort; deploy C4 only through a future serialized free-slot transition in the dedicated Matcha Flavoured 26.2 Workbench.

## C4 runtime matrix

With exact Naturalist C8, Xaero Minimap, accepted Xaero × EMF C9, and Ribbits × Xaero C5, start or join a world and trigger Xaero resource initialization/reload. Require no redirect conflict, `InjectionError`, transformation failure, or Xaero render-frame crash; this is only the coexistence smoke gate.

Then verify a visible, recognizable Naturalist-derived icon rather than a generic label/marker for Ray, Bass (normal/medium/large), Giant Isopod (rolled and unrolled), Hedgehog (rolled and unrolled), Vulture (adult/baby), Tortoise (adult/baby), Starfish, Lizard, Mole, and Piranha. Piranha must never retain a blank successful icon: a failed capture must return Xaero's normal fallback. Repeat after resource reload.

Verify recognizable corrected geometry without unrelated body/limb leakage for Alligator (snout plus immediate eye plane, not broad neck/body), Boar (face/head, not ears), Duck, Komodo Dragon (recognizable side-face), Rat, and Zebra (face-only, no long neck or nose overshoot). Verify Anglerfish has a compact recognizable side/profile; Great White Shark is fully framed and cleanly side-on; Clam is top-down at approximately C3 size; Desert Scorpion includes its abdomen; both scorpions are top-down and recognizable; and Starfish is top-down with all legs visible.

Verify sensible Xaero icon scale for Blobfish, Clam, Dragonfly, Lion, Hippo, Mammoth, Elephant, Rhino, Great White Shark, Desert Scorpion, Jungle Scorpion, Ostrich, Tiger, Starfish, Whale, Zebra, and Brown Bear. Brown Bear must retain Xaero's working native model/texture capture while becoming sensibly sized; Tiger must be reduced without an unrelated presentation change.

Regression controls: Capybara remains good; Lizard Tail, Black Bear, and Turkey retain their C2 behavior; Bird, Butterfly, Catfish, Caterpillar, Crab, Deer, Firefly, Snake, and Snail remain unmodified native Xaero captures. Confirm Duck Egg and Dirt Trail remain excluded. Re-check all observations after resource reload so neither stale successful nor stale failed cache entries mask the result.

Stop and record exact entity ID, age/state/variant, icon result, texture result, reload result, and any compatibility crash. Do not infer a runtime PASS from a client launch or this static validation.
