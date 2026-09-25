# C12 provider-first masonry verification checklist

Exact current candidate: `interchangeable-block-families-0.1.0-canary12.jar`, embedded version `0.1.0-canary12`, 69,075 bytes, SHA-256 `68ED59C4D1A7542572ED82DF9B2617CEC9BF1C6437304CA7A5AE49AC51E01C1E`, built at `2026-09-25T22:06:44.1468886Z` from source `27eece1bcd30b6e164a50c23103e87a076b0af8e`. Controlled validation used AMC C4 `architectural-material-closure-0.1.0-canary4.jar` (`B7344EC4B53305CDFEB5CFD514E2570B919ECCA5432D6C9ED11B7E1846A90FD5`). Exact C7 remains the accepted baseline.

Use an owner-approved isolated Minecraft 26.2 Fabric environment with Clutter No More and declared providers. AMC is a presence-only runtime dependency: Fabric must accept any installed `architectural_material_closure` version; C12 must not request an AMC version or Canary range.

1. Confirm the catalog reports 166 families, 1,811 unique items, and largest family size 38. Confirm the 17 masonry families contain exactly 646 cells: 409 exact Minecraft/BBB/Macaw provider IDs and 237 AMC C4 gap IDs.
2. For each material/form cell, confirm a real provider form is selected wherever present and AMC supplies only the genuine gap. Check representative buttons/pressure plates, BBB details, Macaw paths/pavings/full blocks/slabs/stairs, and standard/Gothic/arrow-slit/louvered windows.
3. Confirm no provider-owned cell has an AMC duplicate, no approved cell is omitted or repeated, and changing material retains the selected form without crossing material boundaries.
4. Verify recipes retain the canonical first form in each literal C12 family and cleanup removes only non-parent alternatives. Retest component preservation, fail-closed mismatches, Quick Stack Nearby affinity, selector/display behavior, and reload behavior.

Static checks passed. The controlled GameTest harness could not resolve its pre-existing Ribbits support set (yungsapi, trinkets_updated, geckolib, cloth-config2, customportals, and matcha-heart-death compatibility); no support dependency, Minecraft profile, deployment, or manual runtime observation was changed. Stop and report any missing literal item, dependency pin, duplicate membership, canonical-parent drift, cleanup error, or crash.
