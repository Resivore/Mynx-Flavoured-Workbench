# Testing

## Current gate

**C5 NOT DEPLOYED — STATIC PASS — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Canary 2 is retained failed external-runtime provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary2.jar`, SHA-256
`ff161f6bcbf5cd056a5e27d7cd56091ab5abfa8e69440c76f2f6ce9ba6e86c50`, source
`e9d0c04ed2ab64b077d1abfb8daaa356c5de895c`. Its Xaero × EMF C9 resource-reload/world-entry smoke gate passed: the C1 competing redirect failure did not recur and the Naturalist matrix could be inspected. C2 nevertheless failed the observed entity matrix; it must not be promoted or described as runtime-passing.

Canary 3 is retained user-reported external-runtime **FAIL** provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary3.jar`, SHA-256
`510b205ce889ca92f31cf20a0eff078348598e3d6fef3cfeba15dc9363977d7b`, source
`831297c74ee4829563476b64d75e24d2342148f9`. Its observed failures were snout-only Alligator, unrecognizable Anglerfish, angled/cropped Great White Shark, oversized Brown Bear and Tiger, Clam side profile, ear-like Boar, nose plus neck Zebra, label-only Ray/Bass/Giant Isopod/Hedgehog/Vulture/Tortoise/Starfish/Lizard/Mole, blank Piranha, and worsened angled Scorpions (including a Desert Scorpion without its abdomen). Do not describe C3 as runtime-untested or runtime-passing.

Canary 4 is retained user-reported external-runtime **FAIL** provenance:
`naturalist-xaero-entity-icons-compat-0.1.0-canary4.jar`, SHA-256
`9aa6869ed1579655c1639206cd2225119523b3c12423f0bce84c40a0f3658c19`, source
`10ba9e25956934b740818aa01391775d165c1f98`. The reported good controls are perfect Boar and the improved Great White Shark profile. C4 still had an oversized/bottom-clipped Great White, huge unrecognizable Anglerfish, oversized Brown Bear, label-only Ray/Bass/Clam/Jellyfish/Giant Isopod/Hedgehog/Zebra/Vulture/Tortoise/Starfish/Lizard/Mole, blank Piranha, and effectively front-on Jungle/Desert Scorpions. It is not runtime-passing or runtime-untested.

Canary 5 is `naturalist-xaero-entity-icons-compat-0.1.0-canary5.jar`, embedded version
`0.1.0-canary5`, SHA-256
`81e5d2283ee54de6436cd9456e372e30ca4389b7bd053ccf264c2561bc545c66`, source
`653d2698ddfeb5df5cf794002e5314795b6e0627`. Two independent clean offline Java 25 / Loom 1.17.19 builds, focused JUnit contract/source/trace/binary checks, and client-only archive inspection passed with byte-identical artifacts. This is static evidence only. C5 is not deployed: canonical manager revision 117 retains unrelated BGE C60 in Slot A and IBF C4 in Slot B. Do not displace either cohort; retain C5 until a serialized free-slot transition is authorized.

## C5 manual runtime checklist

With exact Naturalist C8, Xaero Minimap, accepted Xaero × EMF C9, and Ribbits × Xaero C5, start or join a world and trigger Xaero resource initialization/reload. Require no redirect conflict, `InjectionError`, transformation failure, or Xaero render-frame crash; this is only the coexistence smoke gate.

First verify Boar exactly retains its C4 face/head selection, trace behavior, orientation, scale, and presentation. Verify Great White Shark remains C4 side/profile with the whole silhouette framed (no bottom clipping), and Brown Bear remains Xaero-native with its normal texture/model capture while smaller.

Then verify a visible, recognizable Naturalist-derived icon rather than a generic label/marker for Ray, Bass (normal/medium/large), Clam, Jellyfish, Giant Isopod (rolled and unrolled), Hedgehog (rolled and unrolled), Zebra, Vulture (adult/baby), Tortoise (adult/baby), Starfish, Lizard, and Mole. Zebra must remain face-only. Clam and Starfish must be top-down, with all Starfish arms visible. Repeat after resource reload.

Verify Anglerfish is a compact side/profile head/face icon with jaw and lure, rather than a huge full body. Piranha must render a recognizable icon; deliberately malformed/empty capture must instead leave Xaero's normal label/fallback available and never cache a blank success. Desert and Jungle Scorpions must both be explicit top-down compact silhouettes with claws, legs, body, and tail; Desert must retain its abdomen.

Verify the remaining changed scales are sensible: Great White Shark, Brown Bear, Anglerfish, Clam, Giant Isopod, Jellyfish, Desert Scorpion, Jungle Scorpion, and Starfish. Brown Bear must retain Xaero's working native model/texture capture; no changed entity may become a label-only or blank cached icon after reload.

Regression controls: Capybara remains good; Lizard Tail, Black Bear, and Turkey retain their C2 behavior; Bird, Butterfly, Catfish, Caterpillar, Crab, Deer, Firefly, Snake, and Snail remain unmodified native Xaero captures. Confirm Duck Egg and Dirt Trail remain excluded. Re-check all observations after resource reload so neither stale successful nor stale failed cache entries mask the result.

Stop and record exact entity ID, age/state/variant, icon result, texture result, reload result, and any compatibility crash. Do not infer a runtime PASS from a client launch or this static validation.
