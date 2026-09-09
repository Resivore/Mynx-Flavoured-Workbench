# Testing

## Current gate

**C18 NOT DEPLOYED — STATIC PASS — RUNTIME UNTESTED — CLAM-ONLY CACHE-TO-RENDER DIAGNOSTIC REQUIRED — NOT READY FOR PROMOTION**

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

Canary 9 is retained USER-REPORTED / EXTERNAL RUNTIME **FAIL** provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary9.jar`, embedded `0.1.0-canary9`, SHA-256 `c29601bf5c5a7b185cd54d87bf5681b23f742277fb6bd9b5ec82f426120af60d`, source `1f7ee394ce78cd4a61dee8ac012843fc4acb521b`. Its exact observation was: **Brown Bear icon remained the same oversized size as before**. C9 forced the pre-existing Brown Bear Xaero raster/cache result to regenerate; that did not change the displayed icon. Do not infer any other entity result from this focused test.

Canary 10 is retained static/unrun predecessor provenance: `naturalist-xaero-entity-icons-compat-0.1.0-canary10.jar`, embedded `0.1.0-canary10`, SHA-256 `9dbc94795b485a8d76cdde876587f98068c25deffc95ef81152b0f373817caaa`, source `4eca6c1defeec412cdd8b97a0aad4abc9d2927c2`. Its cache-refresh hypothesis is not repeated in C11.

## C11 retained runtime failure

Exact C11 (`0.1.0-canary11`, `47bf6fa9a9019b972b52e185424c488954af665f94ddbf57ea3856992174306a`) is USER-REPORTED / EXTERNAL RUNTIME FAIL. Brown Bear remained oversized and normally oriented; the temporary 90-degree diagnostic rotation did not appear. The supplied first-request log was `canPrerender=true`, entity-cache MISS, non-null final `XaeroIcon`, `nativeHook=false`, and `nativeRendered=false`, followed by an entity-cache HIT. This specifically disproves the Brown Bear `RadarIconModelPrerenderer.renderModel` presentation path only.

## C12 reconciled runtime evidence

Exact C12 (`0.1.0-canary12`, `78be16149ce6892f25e02047cf50e6f07070b45092b7a55ebd1b9336ce8bb489`) is USER-REPORTED / EXTERNAL RUNTIME evidence. It loaded as `naturalist_xaero_entity_icons_compat 0.1.0-canary12`; Brown Bear remained oversized, as expected because C12 had no size correction. Its focused cache-miss evidence was: `canPrerender=true`; entity-cache MISS; `RadarIconCreator#create` received `xaero.hud.minimap.radar.icon.definition.form.sprite.RadarIconSpriteForm` and `naturalist:textures/entity/bear/bear.png`; creator returned a non-null icon; Xaero cached it; and the following entity-cache HIT returned the non-null cached icon. C12 did not reach either observed model-form or model-part seam. This identifies the native sprite prerenderer, rather than the model prerender bridge, as the effective Brown Bear presentation path.

## C13 retained runtime failure

Exact C13 (`0.1.0-canary13`, `1077ee5b564c8f018e2205fcfce0c48c0d01deff57a77f2068d210c5ebfa05aa`, source `6de714a962caf32ee823ffec5f837cf43db07046`) is USER-REPORTED / EXTERNAL RUNTIME **FAIL**. Brown Bear is no longer oversized, but it is extremely small—effectively a tiny brown dot on the minimap. Orientation/presentation otherwise remain intact. This is not icon-generation failure: it runtime-proves that the `naturalist:bear` → `RadarIconCreator#create` → `RadarIconSpriteForm` → Brown-Bear-specific scale → `XaeroIcon` seam controls displayed size. C13's `0.12F` cap is therefore far too small and must not be accepted or described as runtime-passing.

## C14 retained runtime failure

Exact C14 (`naturalist-xaero-entity-icons-compat-0.1.0-canary14.jar`, embedded `0.1.0-canary14`, SHA-256 `01a2348b061237f45df6a015d57693c31d92cf79dd204fb7d2b39b8f3ab6f1ea`, source `6fe8dd0f82cbaa7378b1c00a0e326ca316414ae9`) is USER-REPORTED / EXTERNAL RUNTIME **FAIL**. Brown Bear is substantially larger than C13's microscopic brown dot; its correct icon is visible and recognizable, orientation remains correct, and the sprite-path scale intervention is clearly functioning. It is nevertheless noticeably too small compared with nearby normal entity icons. C14 is a final-Brown-Bear-sizing failure and must not be accepted/promoted.

## C15 reconciled runtime result

Exact C15 (`naturalist-xaero-entity-icons-compat-0.1.0-canary15.jar`, embedded `0.1.0-canary15`, SHA-256 `97f79910675e71610651bd871ac6aad89ccd5e63794350b8c659c44cbbcab7b7`, source `53fec53d07daabdd8315ac9e25a755291832eb75`) is USER-REPORTED / EXTERNAL RUNTIME mixed evidence, not a full-project pass. Brown Bear **PASSed** the requested correction: at `0.65F`, it is recognizable, correctly oriented, no longer oversized, and no longer too small. Its exact `naturalist:bear` → `RadarIconSpriteForm` → Brown-Bear-specific creator-scale path is frozen for C16. Clam **FAILed** as label-only: Xaero did not produce or accept its model icon; this is capture-route failure, not visual scale evidence.

## C16 reconciled runtime failure

Exact C16 (`naturalist-xaero-entity-icons-compat-0.1.0-canary16.jar`, embedded `0.1.0-canary16`, SHA-256 `6928f24ee1a9e453afd2023368021e45412569beee75efa9e77443d881238122`, source `1b2357fad4270f632a93842141aa4ad2723ede16`) is USER-REPORTED / EXTERNAL RUNTIME **FAIL**. Clam remained label-only with no model icon. This disproves only the C16 attempt to recover icon creation by restoring C7's model-root source and `bottom` trace at the smaller `0.20F` scale; it supplies no orientation, recognizability, clipping, or scale result.

## C17 reconciled runtime failure

Exact C17 (`naturalist-xaero-entity-icons-compat-0.1.0-canary17.jar`, embedded `0.1.0-canary17`, SHA-256 `e4f3bb9a15cd747ff431a6d5f2edc9575bdb2d38f9ad86102346720d3f0dd418`, source `549dfeae60d83616fadea5a32e6a568fc2fbd5e0`) is USER-REPORTED / EXTERNAL RUNTIME **FAIL**. Its supplied runtime identity confirms `naturalist_xaero_entity_icons_compat 0.1.0-canary17`; Clam nevertheless remained label-only. C17 is diagnostic-only and retained the `0.20F` Clam scale. It did not test `0.28F`, so this result is capture-route failure only and supplies no scale, orientation, clipping, or recognizability conclusion.

## C18 focused runtime procedure

With exact Naturalist C8, Xaero Minimap 26.4.2, EMF 3.2.6, generic Xaero × EMF C9, and exact C18, reload resources once and request only Clam through a normal cache MISS followed by a cache HIT. C18 keeps C17/C16's `ClamModel` source path `""`, trace path `bottom`, normalized detached `top`/`bottom`/`hinge` assembly, top-down orientation, and `0.20F` scale unchanged. Brown Bear remains frozen at its proven native `naturalist:bear` → `RadarIconSpriteForm` `0.65F` path.

Collect the one-time `NaturalistXaero ClamCapture` log sequence: request/canPrerender; initial entity-cache HIT or MISS; on MISS, selected `RadarIconCreator#create` form and texture, creator result, model-form entry, model-part traversal, and cache write; then native rendered-part count/model class, contract source/trace/children/scale, explicit trace binding, bridge render destination before/after plus adapter/selected registration, any caught bridge exception, and final manager icon result. Report only those logs and whether Clam is label-only or a real icon; do not retune based on it. Do not assess or alter Brown Bear, Starfish, either Scorpion, Giant Isopod, Zebra, Great White Shark, Piranha, Bass, Ray, Hedgehog, or another Naturalist entity. Require no relevant `InjectionError`, mixin transformation failure, or Xaero render-frame crash.
