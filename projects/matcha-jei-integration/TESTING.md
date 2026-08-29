# Testing

Test only the exact retained C3 artifact `matcha-jei-integration-0.2.0-nonrecipe-discovery-canary1.jar`, SHA-256 `1e4941e2353c1696505f243e86eb58f0239dd1144d544897370b021f6748a455`. It remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; build, fixtures, and artifact checks do not authorize promotion.

## C3 client and singleplayer matrix

1. Under explicit runtime ownership, launch C3 with Matcha Flavoured 1.12, JEI 30.18.0.144, and the cumulative accepted stack in a disposable world. Confirm there is no Matcha integration, networking, subtype, duplicate-recipe, or classloading error.
2. Search Topaz and open Uses. Confirm Expert Toolsmith `14 Emerald + 1 Topaz → Topaz Earrings` and Master Toolsmith `1 Topaz → 10 Emerald`, with the correct levels and exact component-bearing stacks.
3. Open Topaz Earrings recipes/acquisition and Topaz acquisition. Confirm the trade relationships and `Possible Ancient City chest loot; chance and conditions vary` provenance.
4. Spot-check Opal, Ruby, Amber, one custom fish, the cold-cow crate, Titanium Compass, Fox Pelt, and Compound Bow. Confirm the displayed relationships use their exact Matcha identities and that Compound Bow identifies the full-durability representative while noting that acquired durability varies.
5. Select Apotropaic Arrow and one Ofuda directly from JEI search. Confirm each exact entry opens only its matching Matcha relationship and retains the real tooltip and components.
6. Recheck an ordinary Matcha recipe and representative component-bearing recipe outputs. Confirm JEI's vanilla categories remain the sole recipe owner, no duplicate recipe is introduced, and the accepted 315-output behavior remains intact.
7. Run `/reload`, then disconnect and reconnect. Confirm the Matcha categories replace cleanly without duplicate entries, unrelated vanilla data remains absent, recipe knowledge persists idempotently, and no project-owned error appears.
8. Inspect the session log and require a normal save and shutdown with no Matcha subtype, recipe-registration, recipe-award, networking, reload, or common-initializer client-classloading error.

For the 19 exact-bridge identities, Uses/Recipes from a held inventory stack can begin with JEI's broader built-in subtype and show a same-subtype false positive. Selecting the exact Matcha search entry is the intended check; replacing JEI-owned interpreters is outside scope.

## Deferred dedicated-server gate

Start a dedicated server with Matcha Flavoured, Fabric API, and this companion but without JEI. Join and reconnect from a client, confirm currently loaded Matcha recipes are awarded idempotently, and confirm there is no client-only or JEI classloading failure. This gate remains explicitly unperformed.

Stop and leave C3 unaccepted for any crash, project-owned error, duplicate/fabricated recipe, incorrect exact identity or relationship, unrelated data exposure, reload/reconnect regression, or dedicated-server classloading failure.
