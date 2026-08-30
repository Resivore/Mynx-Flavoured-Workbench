# Testing

Wearable Lanterns is PLANNED and has no implementation, build, artifact, or runtime candidate. Every case below is a future runtime acceptance target; no case has passed.

## Future runtime acceptance procedure

1. Open the survival inventory and hover the vanilla leggings slot. Confirm that the dedicated lantern slot reveals immediately to its left, the normal belt slot remains independently available to the right, and the visual order is lantern, leggings, belt. Repeat with the belt provider present and absent; if standalone belt absence is supported, confirm that the lantern remains on the left without replacing Trinkets' normal hover/group behavior.
2. Equip and unequip one regular lantern through direct slot interaction. Confirm that the slot accepts exactly one item, preserves the normal belt contents, and returns the lantern without loss or duplication.
3. Repeat direct equip and unequip with a soul lantern, then confirm that representative invalid blocks, arbitrary items, and non-lantern Trinkets are rejected.
4. Shift-click regular and soul lanterns from Inventory Extended's upper rows, row six, and physical hotbar into the lantern slot; shift-click them back out and confirm ordinary storage-before-hotbar routing, no use of offhand/equipment as storage, correct stack remainder behavior, and no fixed-boundary failures.
5. Repeat direct and shift-click cases in the creative inventory. Confirm correct native Trinkets wrappers, icon, hover/reveal, packet handling, and return to the exact survival slot without ghost items or rejected valid indices.
6. Equip a lantern and belt item simultaneously, move each independently, and confirm that neither slot overwrites, displaces, validates for, or renders as the other.
7. Relog and restart the client, then change dimensions. Confirm exact lantern-slot persistence and prompt synchronization for the local player and a representative observed remote player where multiplayer testing is available.
8. Test death with keepInventory disabled and enabled. Confirm normal Trinkets drop/keep behavior, no duplication or loss, and independence from the belt item; record the exact gamerule and observed result rather than inferring it.
9. In third-person, the inventory player preview, and another-player view, confirm that the actual equipped ItemStack renders at the intended hip/waist placement, follows player motion acceptably, coexists with leggings and other armor, and does not render when the stack is absent or equipped in another Trinkets slot.
10. Repeat representative standing, walking, sprinting, crouching, swimming, elytra, and armor cases with the normal player model and with Fresh Animations/entity-model changes where applicable. Confirm stable body-part attachment, acceptable clipping, and no assumptions about a replacement model hierarchy.
11. Apply Matcha Flavoured and a small diagnostic resource-pack override to the regular and soul lantern item models/textures. Confirm that worn rendering follows the active resource-defined ItemStack model without project-owned copies of vanilla lantern textures.
12. With LambDynamicLights 4.12.2+26.2 present, confirm that regular and soul lantern luminance is derived through the provider's item/block rules, follows the player smoothly, updates across dimensions and relog, and disappears promptly on unequip, death, or slot removal. Repeat without LambDynamicLights and confirm that equip, persistence, and visual rendering still work without errors.
13. Distinguish client dynamic light from actual block/server light: verify that no lantern block, light block, world-state mutation, server luminance, or persistent environmental light is created by wearing the item.

Stop and preserve the exact state and logs on any crash, disconnect, item loss or duplication, invalid-item acceptance, belt/lantern collision, fixed-index failure, creative ghost slot, persistence or synchronization failure, stuck dynamic light, world-light mutation, or material rendering regression. Do not infer a runtime result from this audit, data inspection, decompilation, compilation, static tests, or artifact creation.
