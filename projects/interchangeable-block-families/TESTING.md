# Testing

Test only exact current C4 `interchangeable-block-families-0.1.0-canary4.jar`, 51,171 bytes, SHA-256 `8f805d0cc6cf65599c8fab190be2aa023006a9bd990bcdd2bf0c45f62f1a2968`. C4 remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; controlled tests, GameTests, a build, and byte-identical artifact reproduction do not authorize promotion or constitute in-game runtime validation.

Exact accepted/live C3 remains the effective rollback: `interchangeable-block-families-0.1.0-canary3.jar`, 47,456 bytes, SHA-256 `d0150aea777dd7837d0c9b4f5cf41ce45f510c60ef416fb7001d06a36a6b0e87`.

## Current controlled regression coverage

- The Java 25 build must pass all 36 JUnit checks in 10 suites and all 19 required Fabric GameTests against the exact pinned provider and accepted integration identities.
- The catalog must remain exactly 133 disjoint families, 1,120 unique item IDs, and a 22-member maximum; the accepted 96-family/944-member subset must retain serialization SHA-256 `be066c1bf5524c54eba4558d801142efbbeed6ccee651cfa390d7b40d86b17bf`, and full C4 must retain `b97003b8046a517bcb44f5ee146661b934a9de2df42e808453260d5436c27634`.
- Provider partition, copper-finish isolation, component-patch equality, transfer preservation, disabled ShapeMap behavior, Quick Stack Nearby affinity, door-only loot scope, UI viewport, Nibaru/BGE isolation, and recipe cleanup must remain passing.
- The 1,478-recipe fixture must expect 984 non-parent removals, zero parent removals, and zero surviving literal rewrites; the Macaw Paths partition must remain 77 admitted thin paths and 239 excluded forms.
- A fresh remapped production JAR must remain byte-identical to retained C4 and must not bundle GameTest, Clutter No More, or Macaw provider classes. Run `tools/Verify-RetainedArtifacts.ps1` to verify both retained release identities.

## C4 runtime procedure

Under explicit deployment ownership, use the cumulative accepted stack with exact accepted C3 replaced only in an experimental slot by exact C4. Record only behavior actually observed.

1. Switch Iron Bars to Iron Chain and back, place both, and confirm their native placement and behavior remain provider-owned.
2. Exercise all eight copper bars/chain pairs. Confirm each chain switches only with the same oxidation and wax finish, and verify vanilla weathering, waxing, scraping, and lightning behavior remains intact.
3. Cycle the seven-member Oak accessory family and the three-member partial Bamboo family; confirm no Spruce or unrelated form appears.
4. Cycle Stone and Andesite representatives, including every admitted thin path and the Andesite parapet; confirm no full block, slab, stair, paving, or path block enters the selector.
5. Check Polished Blackstone, Gold, and Iron weighted-plate/rod families; confirm the separate Iron Bars/Chain family never joins the iron accessory family.
6. Acquire canonical parents and switch to non-parent forms. Confirm every family remains obtainable, including aged unwaxed copper-bar parents through vanilla weathering.
7. Confirm equal-component members switch and merge through Clutter No More and Quick Stack Nearby while differently named, lored, or otherwise patched stacks remain distinct and preserve their component patch during transfer.
8. Perform one accepted alternate-door direct break and one support-loss control; each must still yield exactly one matching design item.

Stop and leave C4 unaccepted for any artifact filename, version, or SHA-256 mismatch; missing or cross-linked family; collapsed copper/source variant; component loss; incorrect recipe removal; duplicate door drop; provider behavior regression; selector overflow; crash; or relevant ShapeMap, Mixin, recipe, or transfer error.
