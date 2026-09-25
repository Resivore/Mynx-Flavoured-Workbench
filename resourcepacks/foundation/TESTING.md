# Testing

Foundation v5 is `ACTIVE` and has static archive/resource validation only. No Minecraft runtime observation has been performed.

In an authorized Minecraft Java 26.2 environment with Foundation v5 above the source packs, inspect the Creative inventory or give each exact item:

- `enderscape:veiled_leaves` must use the existing veiled bushy geometry and remain untinted.
- `mynx_trees:silver_birch_leaves` must use the existing Silver Birch bushy geometry with its normal item tint.
- `mynx_trees:wisteria_leaves` must use the existing Wisteria bushy geometry and remain untinted.

Place each leaf block and confirm its in-world Foundation v4 bushy appearance is unchanged. Record the exact artifact filename and SHA-256 if an inventory preview is flat, has a missing model/texture, uses the wrong texture, or has unexpected tinting.
