# Testing

Wearable Lanterns Canary 6 (`0.1.0-canary6`) is the current candidate: `wearable-lanterns-0.1.0-canary6.jar`, 14,001 bytes, SHA-256 `e6ae0178676dc8f12b6e5bb2ded0986d8e17edd4a3eeb026f862b4666ef72591`, finalized at `2026-09-21T19:36:04.0577202Z` from source checkpoint `ce2a1551358d372c7dee34c770fe95e81759b401`. It is `CONTROLLED_VALIDATION_PASS` and `RUNTIME_UNTESTED`.

Accepted Canary 5 remains unchanged and accepted: `wearable-lanterns-0.1.0-canary5.jar`, 13,920 bytes, SHA-256 `0d05d2af86dfb8f41e7cb7c0dde75aed681cfb657fa2cb8de4a42f40344dd0e6`, source checkpoint `413c2dad2f5acf7285311c31921493ef5650eed5`. Canary 6 is not accepted without a later explicit owner decision.

## Observed controlled evidence

- Java 25 / Gradle 9.5.1 / Loom 1.17.19 offline `clean test build` passed 22 focused JUnit tests with zero failures, errors, or skips. Those tests bind the exact seven-value tag, all five provider entries as `required: false`, optional metadata, actual-ItemStack renderer configuration, unchanged Iris selection/evaluation rules, unchanged slot/menu boundaries, exact provider-archive hashes and inventory-model chains, and the absence of provider code/assets or substitute luminance logic.
- The absent-provider controlled server run passed all 3 required GameTests. None of the five provider IDs was registered, the optional tag loaded cleanly, and vanilla Lantern/Soul Lantern remained accepted in the amount-one `legs/lantern` slot.
- The fixture-present controlled server run passed all 3 required GameTests. Plain test items registered under the five audited registry IDs resolved into the public tag; every item passed the real registered predicate and `TrinketSlot.canInsert`, occupied the actual slot, retained the same `ItemStack` object, and was returned unchanged by Trinkets `allEquipped(false)`. This proves implementation-agnostic tag/slot/native-scan behavior; it is not a claim that the real provider mods were launched.
- The finalized JAR scan found no Enderscape, Ribbits, or Aurora assets, data, classes, nested JARs, fixed-brightness implementation, or GameTest code. Compared by uncompressed entry content with exact accepted Canary 5, the entry set is identical and all 11 unaffected entries are byte-identical; only `fabric.mod.json` (candidate version) and `data/trinkets/tags/item/legs/lantern.json` differ. This preserves the renderer, slot/entity/menu data, GUI sprite/lang, production classes, LambDynamicLights boundary, and Iris mixin/config/bridge bytes.
- No Minecraft gameplay profile was inspected or manipulated. No manual, client, shader, Inventory Extended, or actual-provider runtime result was observed, so runtime remains untested.

## Provider audit references

| Approved inventory item | Exact inspected provider baseline | Actual inventory model | Native/default-state luminance reference |
| --- | --- | --- | --- |
| `enderscape:void_lantern` | Enderscape `3.0.2`, SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b` | Provider item definition to `enderscape:item/void_lantern`, generated model and provider texture | 12 |
| `enderscape:bulb_lantern` | Same archive | Provider item definition to `enderscape:item/bulb_lantern`, generated model and provider texture | 15, inherited from Lantern properties |
| `ribbits:swamp_lantern` | Ribbits `4.1.6+26.2-mynx-canary27`, SHA-256 `088c4b7e88c432d6275395e29273597cf42575f59350fe8b4f48c46e6df7f9fd` | Provider item definition to `ribbits:item/swamp_lantern`, generated model and provider texture | 15, state-independent |
| `auroraslanterns:amethyst_lantern` | Aurora's Lanterns `2.1.1+26.2`, SHA-256 `e0f8fe41c5ada5746de8db256eca8d2d6854c08b35fb6c83fad0ef3d66158a8a` | Provider item definition to `auroraslanterns:item/amethyst_lantern`, generated model and provider texture | 14 |
| `auroraslanterns:redstone_lantern` | Same archive | Provider item definition to `auroraslanterns:item/redstone_lantern`, generated model and provider texture | Default state 7; provider state semantics can resolve unlit to 0 |

Archive registry/source inspection confirmed that these are ordinary provider `BlockItem` registrations with the exact IDs above. Enderscape End Lamp/Blinklamp, Aurora chandeliers/wall-placement blocks, BBB wooden lanterns, and every other light remain outside the tag. Neither provider assets nor brightness values are copied into Wearable Lanterns.

LambDynamicLights 4.12.2 continues to enumerate Trinkets `allEquipped(false)` and pass the returned real stack to its ordinary item/block luminance manager. Iris `1.11.2+mc26.2` continues to evaluate the selected real stack's item identity, emission, and color; its block-item path starts from provider default state and applies any stack block-state component before emission evaluation. Wearable Lanterns adds no provider-specific light source or substitute.

## Future user-directed runtime matrix

Use only a later user-authorized test environment. Do not inspect or manipulate any protected or retired Minecraft profile.

1. With no provider mods installed, confirm clean startup, the exact two vanilla entries remain wearable, and no missing-tag/provider error occurs.
2. With each audited provider present, separately place each of the five approved real items into `legs/lantern`; confirm excluded provider lights remain rejected.
3. For all seven supported items, verify the dedicated slot remains amount one, stays at its accepted inventory position, survives relog/dimension changes, and preserves Trinkets equip/quick-move/drop/synchronization behavior with and without Inventory Extended.
4. Confirm the worn render uses each actual provider inventory model/resource-pack override; look specifically for accidental vanilla-model substitution or copied texture behavior.
5. Under LambDynamicLights, compare equipped versus held copies of each item and confirm native differences are retained, especially Void Lantern 12, Amethyst Lantern 14, and Redstone Lantern's provider/default-state semantics. Stop on duplicate, stuck, or normalized-to-15 light.
6. Under exact audited Iris plus the intended shader pack, re-run empty/non-light/equal/brighter real-offhand cases. Confirm Iris retains actual item identity/emission/color, equal or brighter real offhand wins, and no gameplay hand or inventory state changes.
7. Recheck vanilla Lantern and Soul Lantern behavior against accepted Canary 5, including render, LDL, Iris, menu position, and Inventory Extended transfers.

Record exact mod/shader versions and only the behavior actually observed. Stop and preserve logs/settings if startup fails, the actual offhand changes, item loss/duplication occurs, a light sticks after unequip, a duplicate dynamic-light source appears, world/server light changes, or any accepted slot/menu/rendering behavior regresses. Do not mark a runtime pass or accept Canary 6 until that evidence is explicitly supplied.
