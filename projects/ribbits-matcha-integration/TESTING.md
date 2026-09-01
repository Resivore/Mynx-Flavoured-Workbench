# Testing

## Current gate

**PLANNED — NO RUNTIME CANDIDATE**

Implementation has not started. No source, build, artifact, Canary, deployment,
or Minecraft runtime evidence exists for this project. The project now covers
cross-system Ribbits × Matcha behavior rather than only the explorer-map idea.
Do not treat runtime evidence from the canonical Ribbits project or Matcha by
itself as integration evidence.

## Future runtime acceptance procedure

Use only the portions of this procedure that correspond to workstreams actually
implemented by the candidate. Unimplemented conceptual workstreams are not test
requirements. Run after the canonical Ribbits line is runtime-functional, this
project has an exact candidate artifact, and this UUID has explicit Test Slot
ownership. Record exact Minecraft, Fabric Loader, Fabric API, Ribbits, YUNG's
API where applicable, Matcha trade-supplying component or pack, candidate,
resource-pack order, world seed, artifact hashes, and complete client/server
logs.

1. Start with the exact intended Matcha + Ribbits stack. Confirm Matcha's
   authoritative villager-trade set loads cleanly and the candidate does not
   rely on ignored, rejected, duplicate, or unassigned integration resources.
2. For the explorer-map workstream, level several Matcha cartographers through
   the relevant tiers. Confirm a distinct Ribbit Village Explorer Map can
   appear without replacing or renaming Matcha's existing swamp-hut explorer
   map or unrelated cartographer offers.
3. Purchase and follow the Ribbit Village Explorer Map. Confirm its marker and
   terrain lead to the canonical generated Ribbits village, not a swamp hut or
   unrelated structure. Purchase Matcha's swamp-hut map in the same stack and
   confirm it still leads to a vanilla swamp hut. Verify both structures remain
   independently generated and separately identified.
4. For every implemented Matcha-derived Ribbit trade mapping, confirm the offer
   appears only on the intended Ribbit profession and at the documented tier,
   with the intended inputs, outputs, emerald cost, maximum uses, experience,
   price behavior, restocking, and persistence. Verify whether the mapping is
   intentionally frozen or tracks a defined Matcha contract exactly as its
   implementation record specifies.
5. Exercise representative Matcha villagers and Ribbit professions outside the
   intended integration points. Confirm unrelated Matcha offers remain intact,
   unrelated Ribbit-native behavior remains intact, and no trade is duplicated,
   silently replaced, or crowded out beyond the approved design.
6. Check progression and economy interactions across the implemented features:
   repeated trade generation, restocking, offer locking, price changes,
   profession persistence, map purchase, and any relevant compatibility with
   other trade-altering mods in the exact Workbench stack.
7. Save and reload the world, restart the client, and restock affected traders.
   Confirm mapped offers, cartographer progression, purchased maps, map markers,
   destinations, trade uses, and Ribbit profession state persist without
   registry, codec, trade, locate, resource, or map errors.
8. On a dedicated server with matching exact artifacts, exercise every
   implemented integration workstream from a client. Confirm the server owns
   trade and structure-search results and that join, disconnect, reconnect, and
   restart do not desynchronize merchant, Ribbit, map, or progression state.

Pass only when every implemented cross-system behavior matches its documented
contract, existing Matcha and Ribbits behavior outside those boundaries remains
intact, and no relevant trade, map, registry, performance, persistence,
resource, or networking regression is observed. Stop and preserve the exact
stack, seed, entity data, maps, screenshots, and logs on any wrong destination,
missing or replaced offer, unexpected trade drift, failed search, excessive
locate stall, crash, or relevant warning/error. Build, static analysis,
fixtures, generated resources, and GameTests do not constitute Minecraft
runtime validation.
