# Testing

## Current gate

**PLANNED — NO RUNTIME CANDIDATE**

Implementation has not started. No source, build, artifact, Canary, deployment,
or Minecraft runtime evidence exists for this project. Do not treat the
Ribbits port's source/static evidence as explorer-map compatibility evidence.

## Future runtime acceptance procedure

Run this procedure only after the Ribbits 26.2 port has a runtime-functional
candidate, this project has produced an exact candidate artifact, and this UUID
has explicit Test Slot ownership. Record the exact Minecraft, Fabric Loader,
Fabric API, Ribbits, YUNG's API, Matcha trade-supplying component, candidate,
resource-pack order, world seed, artifact hashes, and complete client/server
logs.

1. In a disposable new world, confirm the canonical Ribbits village and vanilla
   swamp hut are both enabled as distinct structures. Verify their exact
   registry or structure-tag identities with current Minecraft commands or
   equivalent server-side evidence.
2. Level several ordinary cartographers through the relevant tiers. Confirm the
   Ribbit Village Explorer Map offer can appear without replacing or renaming
   Matcha's existing swamp-hut map or unrelated vanilla offers.
3. Check the Ribbits offer's display name, destination marker, inputs, emerald
   cost, tier, maximum uses, experience, restocking, and trade persistence.
   Confirm the presentation is distinct and does not use copied protected
   Ribbits resources.
4. Purchase and follow a Ribbit Village Explorer Map. Confirm its marker and
   terrain lead to a generated Ribbits village, not a swamp hut or another
   structure, and that the located village's Ribbit entities and structure
   content remain owned by the base Ribbits project.
5. Purchase and follow Matcha's swamp-hut explorer map in the same stack.
   Confirm it still leads to a vanilla swamp hut and never resolves to a
   Ribbits village.
6. Repeat the two purchases from another cartographer and, where practical, a
   second seed. Confirm nearest-target selection is stable, maps remain usable
   after travel, and one offer does not crowd the other out of the intended
   trade pool.
7. Save and reload the world, restart the client, and restock the cartographers.
   Confirm both map items, markers, destinations, trade uses, and villager
   progression persist without registry, codec, trade, locate, or map errors.
8. On a dedicated server with matching exact artifacts, purchase both maps and
   follow both markers from a client. Confirm the server owns structure search
   and trade results, and that joining, disconnecting, and reconnecting do not
   desynchronize map or merchant state.

Pass only when the dedicated Ribbits map consistently locates the canonical
Ribbits village, the existing swamp-hut map consistently locates a vanilla
swamp hut, both structures continue to generate independently, and no trade,
map, registry, performance, persistence, resource, or networking regression is
observed. Stop and preserve the exact stack, seed, villager data, maps,
screenshots, and logs on any wrong destination, missing offer, replaced offer,
failed search, excessive locate stall, crash, or relevant warning/error. Build,
static analysis, fixtures, generated resources, and GameTests do not constitute
Minecraft runtime validation.
