# Testing

Current C6 candidate: `matcha-jei-integration-0.3.2-canonical-food-discovery-canary6.jar`, 83,567 bytes, SHA-256 `5a0cd106589513b59faa6daeceb03133da71f22051356a33767342ab2ade088e`, source checkpoint `e016421530f1f1b47ff43632fb69cd73d56f5bbd`. It is `STATIC_PASS`, `NOT_DEPLOYED`, and `RUNTIME_UNTESTED`: no managed deployment, Minecraft launch, or user runtime result has been recorded for this exact release.

Exact predecessor C4 `matcha-jei-integration-0.3.0-effective-catalog-canary4.jar`, SHA-256 `b2a071b4b9b329fa10ccdfd1a4fddd3808a529f0985ce56f70c98861bedcf8dd`, source `af145152e425834f55ba279d9509abf51fc17e7c`, has a user-reported external runtime `FAIL`: with C4 active, global Creative Search was severely incomplete and could not find ordinary vanilla items including Crafting Table. That result belongs only to C4. C5 was a safe-search successor and remains `RUNTIME_UNTESTED`; this C6 request is not evidence that C5 failed.

The accepted baseline remains exact C3 `matcha-jei-integration-0.2.0-nonrecipe-discovery-canary1.jar`, SHA-256 `1e4941e2353c1696505f243e86eb58f0239dd1144d544897370b021f6748a455`. Its user-reported external aggregate `PASS` remains historical evidence for C3 only. Exact C2 remains repository/control rollback provenance.

## C6 client or singleplayer matrix

Under explicit runtime ownership, use a disposable world with Matcha Flavoured 1.12, accepted Frost Protection, JEI 30.18.0.144 when available, C6, and the same unrelated mods that produce the existing Polymer duplicate-tab warnings.

### A. Canonical food discovery

1. In Creative Search, search several Matcha-modified ordinary foods, including bread, baked potato, cooked beef, cooked cod, dried kelp, golden apple, and golden carrot.
2. Require only the Matcha health-bearing identity for each replacement, never its visually ordinary plain/default counterpart.
3. Hover every result. Require the intended Matcha health information: its food/consumable effects and associated tooltip information must be present.
4. Check a custom-model food sharing a base item (for example cooked pufferfish) remains a distinct Matcha identity rather than being collapsed with cooked cod.

### B. Search safety and unrelated identities

1. Search `crafting table`, several ordinary foods with no Matcha replacement, ordinary items from different vanilla tabs, and representative unrelated-mod items from the Polymer-warning tabs.
2. Require every control item to remain searchable. C6 must not rebuild, clear, or reconstruct global Creative Search.
3. Confirm Matcha component variants that are not a plain/default counterpart remain distinct: representative potions, suspicious stew, enchanted books, and another exact Matcha custom-model identity are useful controls.

### C. JEI

1. If JEI previously showed both the plain/default and Matcha food identities, search the same affected foods. Require the canonical Matcha identity to remain visible and the plain/default identity to be absent from the ingredient list.
2. Confirm a representative custom-model Matcha food and unrelated component variants remain visible.
3. Confirm the Blessing of Demeter exact entry opens its actual current vanilla recipe exactly once in the normal JEI category—no duplicate or fabricated recipe.
4. Recheck ordinary vanilla JEI searching plus representative C3 trade/acquisition surfaces. JEI's vanilla categories must remain the sole recipe owner.

### D. Lifecycle and logs

1. Run `/reload`, reopen Creative Search and JEI, and repeat A through C.
2. Disconnect and reconnect, then repeat those checks.
3. Require no stale or duplicate Matcha entry, no missing unrelated identity, and restoration of a default identity if its synchronized Matcha replacement is no longer present.
4. Check logs for new Creative or JEI errors. The existing Polymer duplicate-item-group warnings may remain only if they are the same pre-existing nonfatal condition.

## Deferred dedicated-server gate

Start a dedicated server with Matcha Flavoured, Fabric API, and C6 but without JEI. Join and reconnect from a client, confirm loaded Matcha recipes are awarded idempotently, and confirm no client-only or JEI classloading failure. This gate remains unperformed.

Record `FAIL` or `INCONCLUSIVE` for any crash, project-owned error, loss of a vanilla or unrelated Search entry, stale/double/missing Matcha entry, duplicate or fabricated recipe, incorrect exact identity or relationship, `/reload`/reconnect regression, or dedicated-server classloading failure. Do not apply a result to a different artifact, version, hash, or source checkpoint.
