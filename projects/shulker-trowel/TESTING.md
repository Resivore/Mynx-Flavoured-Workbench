# Testing

## Candidate and evidence boundary

Canonical C7 is the exact private Slot B candidate built from source `f7f61a0c50aa0fb96a8fd4d2cb79ce7ef1a7e62e`:

- deployed private `shulker-trowel-0.1.0-canary6-private.jar`, 42,753 bytes, embedded version `0.1.0-canary6`, SHA-256 `2AA0E986202A9FF92D8B3E9FE496D8EE54F48F34CFF945F4DF28BDC87239DCE3`, deployment `03441945-4b07-48e6-b8ff-993b1f18eeb0`, artifact `dfba959d-aac0-4ad7-bdb6-7efdf00218de`;
- tracked clean `artifacts/shulker-trowel-0.1.0-canary6.jar`, 42,215 bytes, SHA-256 `3FB9228F67B6A0FFDD3F2D75CCD95B77B596D39264D800F8820FC30CFEA5F21F`.

The clean artifact is provenance-only and must not replace the private runtime artifact. Archive comparison found no removed or changed common entry; the private artifact adds only `assets/shulker_trowel/textures/item/trowel.png`, 346 bytes, SHA-256 `D754DBAB87A0FE923016268F0CDDC4A785B022A1184837F9C3CA2BE6732F7789`, from the separately authorized read-only JBT source. The sprite and private JAR remain untracked and non-redistributable.

The Java 25 clean build passed 27/27 focused JUnit tests and 13/13 controlled GameTests against exact BGE C54 and Nibaru C46. Manager revision 37 returned `READY_TO_TEST_VERIFIED / UNTESTED`; Minecraft was not launched for C7. Predecessor C6 booted and co-loaded with BGE C53/C46, but Layer was absent from its selector, so that predecessor result is conservatively `INCONCLUSIVE`; no further pass is inferred. Exact private C5 remains unchanged as accepted and rollback identity, and C7 is not promoted.

## C7 / Private Canary 6 runtime matrix

1. Confirm Slot B contains the exact private C7 identity above and Slot A contains exact BGE C54 `7CD96C8153B2DDCA863A96BCF0BBB04680CA6CE92A63A2086A7155441EC9812A` plus exact Nibaru C46 `3281D110F062DB62E721D838A35CE915CA73DD41AF098A013B52952561CEAE7D`. Keep the clean Canary 6 JAR as provenance-only. Stop on a Loader rejection, startup crash, or relevant initialization error.
2. Exercise the selector and confirm exactly seven modes in order: Full Block, Slab, Stair, Wall, Vertical Slab, Step, Layer. Confirm the original six selections remain stable, Layer follows Step, each icon is the actual resolver-produced representative Oak Planks geometry item, the overlay count/scrolling is dynamic, and selection remains server-authoritative across slot moves and save/reload. Old IDs 0 through 5 must decode unchanged; invalid data must fall back safely.
3. Mandatory economy regression: in survival, hold the actual trowel in the main hand with exactly two Oak Planks in the offhand shulker and select Layer. Confirm the first Oak Layer consumes one plank; compatible same-block growth from thickness 1 through 4 consumes none; a fifth, wrong-face, and incompatible attempt each consume none; and normal survival breaking returns exactly one Oak Planks. Block adjacent valid placement while testing failures.
4. Resolve representative ordinary, pillar/axis, and Glazed Layers, then smoke-check Full Block, Slab, Stair, Wall, Vertical Slab, and Step. Confirm exact log/wood, stripped/unstripped, copper oxidation, and wax variants remain distinct; orientation, waterlogging, normal delegated placement, quantity weighting, no-eligible-source failure, persistence, sounds, and multiplayer/client synchronization remain canonical.
5. Inspect `latest.log` for Loader, catalog, resolver, selector/overlay, placement, payload, synchronization, mixin, registry, or resource errors. Stop and leave C7 unaccepted on any missing/reordered mode, wrong icon/count, mode-authority regression, economy error, adjacent-placement false result, variant collapse, incorrect geometry/state, consumption on failure, crash, or relevant log error. Record only behavior actually observed in Minecraft.
