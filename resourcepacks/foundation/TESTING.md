# Testing

Foundation v7 is `ACTIVE` and has static archive/resource validation only. No Minecraft runtime observation has been performed.

In an authorized Minecraft Java 26.2 environment, enable Foundation v7 as the single consolidated Foundation/Matcha pack and retain the exact artifact filename and SHA-256 with any observation.

Check `enderscape:veiled_leaves`, `mynx_trees:silver_birch_leaves`, and `mynx_trees:wisteria_leaves` in an inventory or creative menu. Each preview must visibly retain its normal central leaf cube and add the four angled exterior foliage cards; Silver Birch must retain its constant item tint, while Veiled and Wisteria remain untinted. Place all three leaves and confirm their existing in-world bushy appearance is unchanged.

Check a Matcha-only resource by inspecting a shulker box (`assets/minecraft/textures/entity/shulker/shulker.png`) and confirm its overlay appearance loads without a missing texture.

Check several former collision paths: a lily pad, a birch door, and an item frame must show the Matcha Overlays v37 appearance, because `assets/minecraft/blockstates/lily_pad.json`, `assets/minecraft/textures/block/birch_door_bottom.png`, and `assets/minecraft/textures/item/item_frame.png` are Matcha-wins paths.

Record the exact artifact filename and SHA-256 if any representative asset is missing, flat, wrongly tinted, or resolves to an unexpected source appearance.
