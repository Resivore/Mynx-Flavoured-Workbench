# Testing

Current C4 candidate: `matcha-jei-integration-0.3.0-effective-catalog-canary4.jar`, 77,686 bytes, SHA-256 `b2a071b4b9b329fa10ccdfd1a4fddd3808a529f0985ce56f70c98861bedcf8dd`, source checkpoint `af145152e425834f55ba279d9509abf51fc17e7c`. It is `STATIC_PASS`, `NOT_DEPLOYED`, and `RUNTIME_UNTESTED`: no managed deployment, Minecraft launch, or user runtime report has been recorded for this exact release.

The accepted baseline remains exact C3 `matcha-jei-integration-0.2.0-nonrecipe-discovery-canary1.jar`, SHA-256 `1e4941e2353c1696505f243e86eb58f0239dd1144d544897370b021f6748a455`. Its user-reported external aggregate `PASS` is historical evidence for C3 only; it is not C4 evidence. Exact C2 `matcha-jei-integration-0.1.1-subtype-registration-fix-canary.jar`, SHA-256 `4ef3ddde44ec06e2ee852dd64f5f5d1cf25c1f4f908282f854e9f49b2d5fb60d`, remains repository/control rollback provenance and is not a target-local retained rollback.

## C4 client or singleplayer matrix

Under explicit runtime ownership, use a disposable world with Matcha Flavoured 1.12, accepted Frost Protection, JEI 30.18.0.144, and C4.

1. Search both JEI and Creative Search for the Blessing of Demeter. Confirm its custom name, lore, item model, and exact stored enchantment are retained: only `main:freezing_protection` at level III; Frost Walker and vanilla Protection must be absent. Confirm the exact JEI entry opens the real, current vanilla recipe exactly once in its normal JEI category—no duplicate or fabricated recipe.
2. Spot-check Frost Protection III book, Thief's Hood, Iron Lily, Moonshine, Mercurial Curio, a Compass of Foresight, one gem item and trade, one custom fish, one food, one crate, a Potion's Core / Uncharted Expedition identity, and an alternate model. Check each exact identity’s name, lore, model, tooltip/components, and expected JEI relationship or provenance.
3. Recheck the C3 surfaces: Topaz trade and acquisition, one exact-bridge identity such as Apotropaic Arrow or Ofuda, one ordinary Matcha recipe, and a representative component-bearing recipe output. JEI's vanilla categories must remain the sole recipe owner; no duplicate recipe, unrelated data, or fake mechanics may appear.
4. Run `/reload`, open Creative Search and JEI again, then disconnect and reconnect. Confirm both catalogs replace cleanly with no stale, missing, or duplicate Matcha entries, that vanilla cached Search contents are refreshed, and that recipe knowledge stays idempotent.
5. Inspect the session log. Require a normal save and shutdown with no Matcha scanner, networking, Creative Search, JEI subtype, recipe-registration, recipe-award, reload, or client-classloading error.

For the 19 exact-bridge identities, held-stack Uses/Recipes can begin with JEI's broader built-in subtype and show a same-subtype false positive. Selecting the exact Matcha search entry is the intended check; replacing JEI-owned interpreters is outside scope.

## Deferred dedicated-server gate

Start a dedicated server with Matcha Flavoured, Fabric API, and C4 but without JEI. Join and reconnect from a client, confirm currently loaded Matcha recipes are awarded idempotently, and confirm no client-only or JEI classloading failure. This gate remains unperformed.

Record `FAIL` or `INCONCLUSIVE` for any crash, project-owned error, stale/double/missing Creative Search entry, duplicate or fabricated recipe, incorrect exact identity or relationship, unrelated data exposure, reload/reconnect regression, or dedicated-server classloading failure. Do not apply a result to a different artifact, version, hash, or source checkpoint.
