# Testing

## Current gate

**C6 NOT DEPLOYED — STATIC PASS — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

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

Canary 5 is retained user-reported/external runtime **FAIL** provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary5.jar`, embedded version `0.1.0-canary5`, SHA-256 `81e5d2283ee54de6436cd9456e372e30ca4389b7bd053ccf264c2561bc545c66`, source `653d2698ddfeb5df5cf794002e5314795b6e0627`. Its reported good results were Mole, Vulture, Tortoise, Lizard, Anglerfish, Great White Shark size/profile, Ray, Piranha, Jellyfish, and Giant Isopod (rolled/unrolled). The reported failures were bottom-clipped Great White; unrecognizable Jungle Scorpion; label-only Desert Scorpion, Starfish, and Clam; nose-only Zebra; wrong Bass presentation; and oversized Brown Bear, Jellyfish, Giant Isopod, Piranha, and Ray. Do not describe C5 as runtime-untested or runtime-passing.

Canary 6 is `naturalist-xaero-entity-icons-compat-0.1.0-canary6.jar`, embedded version `0.1.0-canary6`, SHA-256 `abb9e16ef84007dfd2136ea11cd8803c87cd74ab9dbc04edee8a9c8f0a2337f1`, source `1b9483c118c2809e86d95fefa857c5e57c8f708c`. A clean offline Java 25 / Loom 1.17.19 `check stageCanaryArtifact` build, focused JUnit contract/source/trace/binary checks, and client-only archive inspection passed. This is static evidence only. C6 is not deployed: canonical manager revision 117 retains unrelated BGE C60 in Slot A and IBF C4 in Slot B. Do not displace either cohort.

## C6 manual runtime checklist

With exact Naturalist C8, Xaero Minimap, accepted Xaero × EMF C9, and Ribbits × Xaero C5, start or join a world and trigger Xaero resource initialization/reload. Require no redirect conflict, `InjectionError`, transformation failure, or Xaero render-frame crash; this is only the coexistence smoke gate.

First verify Mole, Vulture, Tortoise, Lizard, Anglerfish, Boar, Capybara, and the unchanged native Xaero controls retain their C5-good behavior. Verify Great White Shark retains its C5 scale and side/profile while its complete lower silhouette is framed. Verify Brown Bear remains Xaero-native with its normal texture/model capture and has a decisively smaller icon; do not compare it to a custom partial capture.

Then verify a visible, recognizable Naturalist-derived icon rather than a generic label/marker for Ray, Bass (normal/medium/large), Clam, Jellyfish, Giant Isopod (rolled and unrolled), Hedgehog (rolled and unrolled), Zebra (adult/baby), Vulture (adult/baby), Tortoise (adult/baby), Starfish, Lizard, and Mole. Zebra must be a side profile of face, ears, muzzle, and only a compact upper-neck silhouette. Bass must be a side/profile fish in every size, with Large Bass retaining a compact whole silhouette. Clam must be top-down. Starfish uses the narrow model fallback because Xaero's trace-gated model capture has no item-sprite result seam; confirm it is a recognizable top-down entity icon and never a blank cached success. Repeat after resource reload.

Verify Anglerfish is a compact side/profile head/face icon with jaw and lure, rather than a huge full body. Piranha must render a recognizable icon; deliberately malformed/empty capture must instead leave Xaero's normal label/fallback available and never cache a blank success. Desert and Jungle Scorpions must both be explicit top-down compact silhouettes with claws, legs, body, and tail; Desert must retain its abdomen. Neither may fall back to a label.

Verify C6's scale-only changes are materially smaller and visually consistent for Brown Bear, Ray, Piranha, Jellyfish, and Giant Isopod (both states). Brown Bear must retain Xaero's working native model/texture capture; no changed entity may become a label-only or blank cached icon after reload.

Regression controls: Capybara remains good; Lizard Tail, Black Bear, and Turkey retain their C2 behavior; Bird, Butterfly, Catfish, Caterpillar, Crab, Deer, Firefly, Snake, and Snail remain unmodified native Xaero captures. Confirm Duck Egg and Dirt Trail remain excluded. Re-check all observations after resource reload so neither stale successful nor stale failed cache entries mask the result.

Stop and record exact entity ID, age/state/variant, icon result, texture result, reload result, and any compatibility crash. Do not infer a runtime PASS from a client launch or this static validation.
