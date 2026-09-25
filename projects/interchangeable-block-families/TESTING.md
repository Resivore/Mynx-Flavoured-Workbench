# C11 masonry-closure verification checklist

Exact current candidate: `interchangeable-block-families-0.1.0-canary11.jar`, embedded version `0.1.0-canary11`, 58,912 bytes, SHA-256 `C0DFB4366C59325B7D91A998F6D1A70F4D1FA6DCEB2C6B7F86F9ED165AD1CC79`, built at `2026-09-25T05:26:35.4392877Z` from source `acc3a821`. Controlled validation used AMC C3 `architectural-material-closure-0.1.0-canary3.jar` (`5C74F1C3442D93D0877067195596344A826D118AA894E339DB7E07B1244E7417`). Exact C7 remains the accepted baseline.

Use an owner-approved isolated Minecraft 26.2 Fabric environment with Clutter No More and declared providers. AMC is a presence-only runtime dependency: Fabric must accept any installed `architectural_material_closure` version; C11 must not request C1, C2, C3, or a Canary range.

1. Confirm the catalog reports 233 families, 1,810 unique items, and largest family size 22. Confirm Fabric dependency resolution accepts AMC C3 without an AMC version pin.
2. For each C3 masonry profile, verify the literal Detail family has BBB Column/Urn/Moulding/Fence/Frame; the Building Accessory family has paths, pavings, and parapet; and the Window family has standard, Gothic, arrow-slit, and louvered-shutter forms.
3. Confirm changing material retains the form rather than replacing it with different architecture. No member may cross material profile boundaries.
4. Verify recipes retain the canonical first form in each literal C3 family and cleanup removes only non-parent alternatives. Retest component preservation, fail-closed mismatches, Quick Stack Nearby affinity, and reload behavior.

Static checks passed; no Minecraft deployment, profile access, or manual runtime observation is recorded. Stop and report any missing literal item, dependency pin, duplicate membership, canonical-parent drift, cleanup error, or crash.
