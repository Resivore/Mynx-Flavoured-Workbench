# Testing

Exact `0.1.0-canary4` is the current and accepted client/singleplayer release. The migration performed no deployment or Minecraft runtime check. Use this procedure only for a controlled revalidation of the exact retained C4 JAR or for the still-unperformed dedicated-server coverage.

1. Confirm the loaded artifact is `inherent-3x3-inventory-crafting-0.1.0-canary4.jar` with SHA-256 `6E797291DD6C68F5AFE65F91177AD2653F7C4A99E10BD20E078FAFD7AAF6CB33`, then reach the title screen, enter a world, and open the survival inventory.
2. Confirm the final upper-panel layout: input interiors at x `77,95,113` and y `8,26,44`, result at `(152,27)`, recipe-book button at `(100,62)`, and no overlap with player, equipment, Trash, or Inventory Extended slots.
3. Craft representative 1×1, 2×2, true 3×3 shaped, shapeless, repeated, and remainder/container recipes manually; confirm normal output, consumption, remainders, and recipe awards.
4. Exercise recipe-book one-click, maximum, and replacement placement and JEI player-recipe transfer; confirm all nine cells are filled in row-major order and the result updates normally.
5. Shift-click from every crafting input and the result. Confirm world pickups, inventory moves, external-container quick moves, offhand routing, carried-container routing, and Trinkets operations never use crafting cells as destinations.
6. Verify all Inventory Extended rows, hotbar, armor, offhand, Simple Trash Slot, Trinkets slots, and Creative-to-Survival switching remain usable and correctly ordered.
7. Close and reopen the inventory, save and reload, disconnect and rejoin, and die and respawn with marked ingredients; confirm there is no loss, duplication, unexpected drop, stale result, or ghost stack.
8. Use an ordinary crafting table manually, through its recipe book, and through JEI; confirm `CraftingMenu` behavior remains unchanged.
9. Review the log for mixin application failures, layout assertions, packet errors, or recipe-transfer errors and confirm a normal shutdown.
10. For dedicated-server coverage, repeat the applicable crafting, routing, relog, and lifecycle cases as a remote client and record that result separately; no dedicated-server pass is currently claimed.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if the game crashes, slot construction differs between client and server, any non-recipe route inserts into a crafting cell, ingredients are lost or duplicated, Inventory Extended/Trash/Trinkets ordering regresses, the final GUI overlaps, or crafting tables change. Do not replace or repromote the accepted C4 artifact without an explicit new project task.
