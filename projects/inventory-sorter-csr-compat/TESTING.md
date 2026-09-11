# Inventory Sorter CSR Compatibility — Canary 2

Use only the dedicated Matcha Flavoured 26.2 Workbench through a serialized Test Instance Manager transition. Do not access the protected 26.1.2 profile.

For each case, first use an ordinary no-reservation inventory as a control. CSR-reserved *physical slots* are fixed; every unreserved outer shulker box and bundle is sortable under Inventory Sorter's normal ItemStack ordering.

- Sort an unreserved empty and then filled shulker placed at an obviously unsorted position. It must move normally while its exact contents, custom name, dye/type, and every component remain intact.
- Sort an unreserved empty and then filled bundle. The outer bundle must move normally; its exact contents, name, variant/components, and any CSR-owned identity data must remain intact.
- Put a partially filled bundle beside loose compatible stacks, sort, and confirm the loose stacks remain outer inventory stacks. The bundle contents must neither increase nor be extracted/reordered.
- Repeat with several bundles and with mixed bundles/shulkers. All unreserved outer containers may reorder; none may receive, lose, merge, or reconstruct contents.
- Reserve a physical slot holding an ordinary stack, a shulker, and a bundle in separate trials. Sort each time: the exact reserved slot and full stack must remain unchanged; it must not receive merges or be workspace. Also verify an empty reserved slot remains empty.
- Use a mixed layout of ordinary stacks, a movable bundle, a CSR-reserved slot, a movable shulker, and ordinary stacks. Only the reserved slot may remain fixed. Repeat with distributed reservations and, if supported, a double container to verify physical-half/local-slot mapping.
- In the player main inventory, sort with shulkers only: normal Inventory Sorter sorting must occur. Then sort with one and several bundles: outer bundles and shulkers must sort normally, while no loose item is inserted into any bundle.
- If the matching CSR specific-container-identity canary is available, verify unreserved identity-bearing shulkers/bundles move while retaining their complete identity/components. Then place each in its correctly reserved slot and confirm that slot remains untouched.

Stop and record `FAIL` if any CSR-reserved slot changes, if a portable container’s components or contents change, or if sorting inserts loose items into a bundle. Record only observed desktop runtime results; build, GameTests, and launch evidence are not desktop runtime evidence.
