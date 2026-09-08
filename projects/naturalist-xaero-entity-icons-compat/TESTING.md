# Testing

## Current gate

**C10 NOT DEPLOYED — STATIC PASS — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

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

Canary 6 is retained USER-REPORTED / EXTERNAL RUNTIME **FAIL** provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary6.jar`, embedded version `0.1.0-canary6`, SHA-256 `abb9e16ef84007dfd2136ea11cd8803c87cd74ab9dbc04edee8a9c8f0a2337f1`, source `1b9483c118c2809e86d95fefa857c5e57c8f708c`. Giant Isopod, Zebra, Great White Shark, Piranha, and Bass were good. Ray, Brown Bear, and Hedgehog were too large. Clam, Starfish, and Desert Scorpion were label-only, and Jungle Scorpion was an unrecognizable tiny horizontal fragment. C6 was not manager-deployed: canonical manager revision 117 retains unrelated BGE C60 in Slot A and IBF C4 in Slot B.

Canary 7 is retained USER-REPORTED / EXTERNAL RUNTIME **FAIL** provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary7.jar`, embedded version `0.1.0-canary7`, SHA-256 `c4f2c6a3658f73a853623356deb94708a082f65265cdeeed9b970c57e9106cb2`, source `871b2f5c987b58d88deca3f3725207813c761d14`. Giant Isopod, Zebra, Great White Shark, Piranha, Bass, Ray, and Hedgehog were good. Clam retained the correct top-down direction but was too large and top-clipped; Jungle Scorpion, Desert Scorpion, and Starfish were label-only. C7 was external/user-reported rather than manager-deployed: canonical manager revision 117 retains unrelated BGE C60 in Slot A and IBF C4 in Slot B.

Canary 8 is retained USER-REPORTED / EXTERNAL RUNTIME **FAIL** provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary8.jar`, embedded `0.1.0-canary8`, SHA-256 `dc26b12fe87d60849fb7a99a0687b70e895ce3d6064dbd0f2ddff69d9f1cbb70`, source `479c36d1b3cd4f5c0839a7ba3aa2d8b0c053dde0`. The latest user report is that the canary wholly failed overall and Brown Bear remained too large. No other C8 entity result is recorded or inferred. C8 had no manager deployment: canonical manager revision 117 retains unrelated BGE C60 in Slot A and IBF C4 in Slot B.

Canary 9 is retained user-reported/external runtime **FAIL** provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary9.jar`, embedded `0.1.0-canary9`, SHA-256 `c29601bf5c5a7b185cd54d87bf5681b23f742277fb6bd9b5ec82f426120af60d`, source `1f7ee394ce78cd4a61dee8ac012843fc4acb521b`. The world loaded and the player joined, then Xaero radar initialization crashed because Naturalist C9 and generic Xaero × EMF C9 both redirected the same `RadarIconEntityCache#get` call. The generic required `xaeroEmf$retryFailedAtActualPrerender` injection was displaced after Naturalist's redirect won; do not classify C9 as runtime-untested or passing.

Canary 10 is `naturalist-xaero-entity-icons-compat-0.1.0-canary10.jar`, embedded `0.1.0-canary10`, SHA-256 `9dbc94795b485a8d76cdde876587f98068c25deffc95ef81152b0f373817caaa`, source `4eca6c1defeec412cdd8b97a0aad4abc9d2927c2`. Offline Java 25 / Loom 1.17.19 `clean check stageCanaryArtifact` passed focused cache freshness, Native Bear behavior, existing Naturalist contracts, Xaero bytecode seams, client-only archive checks, and a production Fabric Knot/Mixin harness using Xaero 26.4.2, EMF 3.2.6, ETF 7.1.1, Naturalist C8, and generic Xaero × EMF C9. It is static evidence only. Both manager slots are occupied by unrelated BGE C60 and IBF C4, so C10 is not deployed and remains runtime-untested.

## C10 Brown Bear coexistence runtime checklist

With exact Naturalist C8, Xaero Minimap 26.4.2, EMF 3.2.6, generic Xaero × EMF C9, and C10, start or join an existing world and trigger Xaero resource initialization/reload. Require no redirect conflict, `InjectionError`, transformation failure, or Xaero render-frame crash.

Verify only Brown Bear for this canary: its final displayed Xaero radar icon must be materially smaller, still use Xaero's native Brown Bear model/texture capture rather than a custom fallback or head-only capture, and remain correct after resource reload. The initial cached entry may be retried only once when Xaero can prerender it; no-prerender cache reads remain unchanged. Require no crash, redirect conflict, injection failure, mixin transformation failure, or Xaero render-frame crash.

Verify no competing redirect, `InjectionError` for `xaeroEmf$retryFailedAtActualPrerender`, mixin transformation failure, or Xaero render-frame crash occurs with generic Xaero × EMF C9. Deliberately malformed/empty capture must leave Xaero's normal fallback available and never cache a blank success.

Do not retune or classify Clam, Starfish, either Scorpion, Ray, Hedgehog, Bass, Zebra, Giant Isopod, Great White Shark, or any other Naturalist target from this focused Brown Bear test. Record only actual Brown Bear observations with its state/variant, native texture/model result, initial display result, post-reload result, and any compatibility failure.

Stop and record exact entity ID, age/state/variant, icon result, texture result, reload result, and any compatibility crash. Do not infer a runtime PASS from a client launch or this static validation.
