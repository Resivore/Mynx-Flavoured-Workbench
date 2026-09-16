# Testing

Current C5 candidate: `matcha-jei-integration-0.3.1-safe-search-canary5.jar`, 79,558 bytes, SHA-256 `70b46cb64610b883d0981514672e49a1c700062bad18800dc51e409f579a9e4b`, source checkpoint `1924b7aeccbc3e561d91cd4b4447cb3277d5a67e`. It is `STATIC_PASS`, `NOT_DEPLOYED`, and `RUNTIME_UNTESTED`: no managed deployment, Minecraft launch, or user runtime result has been recorded for this exact release.

Exact predecessor C4 `matcha-jei-integration-0.3.0-effective-catalog-canary4.jar`, SHA-256 `b2a071b4b9b329fa10ccdfd1a4fddd3808a529f0985ce56f70c98861bedcf8dd`, source `af145152e425834f55ba279d9509abf51fc17e7c`, has a user-reported external runtime `FAIL`. With C4 active, global Creative Search was severely incomplete and could not find ordinary vanilla items including Crafting Table. The same pack emits pre-existing Polymer duplicate-entry errors for several category tabs, but the user's comparison establishes that those errors are nonfatal and Creative Search works when C4 is absent. This `FAIL` belongs only to C4.

The accepted baseline remains exact C3 `matcha-jei-integration-0.2.0-nonrecipe-discovery-canary1.jar`, SHA-256 `1e4941e2353c1696505f243e86eb58f0239dd1144d544897370b021f6748a455`. Its user-reported external aggregate `PASS` remains historical evidence for C3 only. Exact C2 `matcha-jei-integration-0.1.1-subtype-registration-fix-canary.jar`, SHA-256 `4ef3ddde44ec06e2ee852dd64f5f5d1cf25c1f4f908282f854e9f49b2d5fb60d`, remains repository/control rollback provenance and is not a target-local retained rollback.

## C5 client or singleplayer matrix

Under explicit runtime ownership, use a disposable world with Matcha Flavoured 1.12, accepted Frost Protection, JEI 30.18.0.144, C5, and the same unrelated mods that produce the existing Polymer duplicate-tab warnings.

### A. Baseline Creative Search

1. Search `crafting table` and require the vanilla Crafting Table.
2. Search several other ordinary vanilla items from different tabs.
3. Search representative unrelated-mod items from the tabs that currently produce Polymer duplicate warnings.
4. Require all of those entries to remain present. Matcha must not clear, replace, or reconstruct the complete global Search collection.

### B. Matcha exact catalog

1. Search Creative Search for the Blessing of Demeter. Confirm its custom name, lore, item model, and authoritative C4-effective stored enchantment identity: only `main:freezing_protection` at level III; Frost Walker and vanilla Protection must be absent.
2. Spot-check Frost Protection III book, Thief's Hood, Iron Lily, Moonshine, Mercurial Curio, a Compass of Foresight, one gem item and trade, one custom fish, one food, one crate, a Potion's Core / Uncharted Expedition identity, and an alternate model.
3. Confirm each exact identity appears once with its expected components. No stale predecessor identity or duplicate Matcha Search insertion is acceptable.

### C. JEI

1. Confirm the Blessing of Demeter exact entry appears and opens its actual current vanilla recipe exactly once in the normal JEI category—no duplicate or fabricated recipe.
2. Recheck ordinary vanilla JEI searching and representative Matcha C3 surfaces: Topaz trade and acquisition, one exact-bridge identity such as Apotropaic Arrow or Ofuda, one ordinary Matcha recipe, and a representative component-bearing recipe output.
3. Require JEI's vanilla categories to remain the sole recipe owner, all exact ingredient identities to remain available, and no broad ingredient disappearance.

For the 19 exact-bridge identities, held-stack Uses/Recipes can begin with JEI's broader built-in subtype and show a same-subtype false positive. Selecting the exact Matcha search entry is the intended check; replacing JEI-owned interpreters remains outside scope.

### D. Lifecycle

1. Join the world and complete A through C.
2. Run `/reload`, reopen Creative Search and JEI, and repeat the representative checks.
3. Disconnect and reconnect, reopen both interfaces, and repeat them again.
4. Require no stale, duplicated, or missing Matcha entry and no loss of vanilla or unrelated-mod entries after any transition.

### E. Logs

The existing Polymer duplicate-item-group warnings may remain only if they are the same pre-existing nonfatal condition. Require no new Matcha-owned Creative Search exception, no Matcha-triggered global Search rebuild, no global Search collapse, and a normal save and shutdown without Matcha scanner, networking, JEI subtype, recipe-registration, recipe-award, reload, or client-classloading errors.

## Deferred dedicated-server gate

Start a dedicated server with Matcha Flavoured, Fabric API, and C5 but without JEI. Join and reconnect from a client, confirm currently loaded Matcha recipes are awarded idempotently, and confirm no client-only or JEI classloading failure. This gate remains unperformed.

Record `FAIL` or `INCONCLUSIVE` for any crash, project-owned error, loss of a vanilla or unrelated Search entry, stale/double/missing Matcha entry, duplicate or fabricated recipe, incorrect exact identity or relationship, unrelated data exposure, reload/reconnect regression, or dedicated-server classloading failure. Do not apply a result to a different artifact, version, hash, or source checkpoint.
