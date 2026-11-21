# Testing Foundation v8

Use `Foundation v8.zip`, Canary 4, SHA-256 `6ff2d732ab196204e0cfe7d1057953b5ce0d0ac09b10d476a234966913f36f92`. In an owner-directed Minecraft Java 26.2 session, give Foundation higher priority than Matcha Flavoured 1.12. Foundation includes the Matcha Overlays v37 resources. Use the pack stack's connected-texture renderer for the overlay checks and AMC with its window provider for the six forms.

## Vanilla materials and connected edges

1. Compare Netherrack, Nether Bricks and End Stone Bricks with their vanilla 26.2 inventory previews and placed faces. Look for the red Netherrack grain, the dark red Nether Bricks courses and the pale End Stone Bricks courses.
2. Place Netherrack against a wall of ordinary stone. Test a straight edge, an inner corner, an outer corner and an isolated junction. Inspect the top and sides. The connected fringe should follow each boundary with red Netherrack artwork and transparent gaps. Repeat with End Stone Bricks against stone to see its pale brick edge.
3. Place End Stone Bricks beside Nether Bricks. This pairing exercises one of the existing overlays that can appear on ordinary Nether Bricks. Compare nearby cracked and red Nether Bricks with the previous stack as controls.
4. In US, UK, Canadian and Australian English, check the base item tooltip for **End Stone Bricks**. Check **End Stone Brick Slab**, **End Stone Brick Stairs** and **End Stone Brick Wall**. Confirm a separate Matcha translation still appears.

## AMC Red Sandstone

Use the six Red Sandstone forms in the inventory and in the world. Place Matcha Red Sandstone beside them to compare side grain, top grain and the exposed underside. Matcha uses the vanilla bottom sprite. The shutter uses the top sprite for both caps.

| Form | What to inspect |
| --- | --- |
| Button | Floor, wall and ceiling mounts, pressed and released. Check the face grain rotates with the button. |
| Pressure Plate | Raised and powered poses. Compare top, side and visible underside with the source block. |
| Parapet | Rotate the parapet and check its support variant. Look across the top and along the side. |
| Gothic Window | Single and stacked sections on both axes. Check the narrow frame, glass and open-state iron bars. |
| Arrow Slit | Single, lower, middle and upper sections on rotated walls. Check stone faces around the opening and exposed caps. |
| Louvered Shutter | Both hinges and open/closed poses. Look for five lit slats with recessed shadows, visible Matcha top grain, clean edges and matching caps. |

## Retained v7 checks

Check the cube and four exterior foliage cards in the inventory previews for Veiled, Silver Birch and Wisteria Leaves. Silver Birch retains its constant tint. Check all three placed bushy models, the shulker-box artwork, and the merged Matcha appearance of a lily pad, birch door and item frame.

Record the Foundation filename and SHA-256, AMC version, pack order, CTM renderer, locale, affected block and pose with each runtime observation.
