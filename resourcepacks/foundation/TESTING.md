# Foundation verification

## Canary 8 / v12 Mossy Oak

Enable Foundation v12 above Matcha Flavoured 1.12. Place raw and stripped logs on three axes, both six-sided wood blocks, BGE timber shapes and AMC log parapets. Inspect their inventory models. Compare against the four Foundation surfaces in the final preview. Disable Foundation, reload, and repeat with the standalone timber. The stripped side uses Matcha’s vanilla fallback in both stacks. Check existing plank overlays at corners and confirm ordinary Oak keeps its original appearance.

## Canary 7 material comparison

Use Foundation v11 above Matcha Flavoured 1.12. Compare ordinary Birch, Goop-derived Silver Birch and muted Wisteria side by side. Inspect planks, logs, doors, boats, signs, shelves, BBB beams and Macaw and Dramatic Doors designs. Check the Birch bark overlay and the straight edges, corners and isolated transitions of all three plank overlays. Examine plank/beam geometry with Continuity active. Compare with Foundation disabled to check that the private mod assets render independently.

The two rounded Silver Birch log ends belong to Mynx Trees. Changing ordinary Birch's pack priority must not change them. Recheck the existing Red Sandstone, Nether and End surfaces as described below.

The following surfaces were introduced in Canary 6. Use the current pack for regression checks. In an owner-directed Minecraft Java 26.2 session, give Foundation higher priority than Matcha Flavoured 1.12. Foundation includes the Matcha Overlays v37 resources. The native catalog below was checked against BBB Canary 9 (dev.7) and Macaw's Windows 2.4.2. Use the pack stack's connected-texture renderer for the overlay checks and AMC for its six additional forms.

## Cut Red Sandstone transitions

1. Build a vertical wall of Smooth Red Sandstone. Viewed head-on, place a column of Cut Red Sandstone immediately to the right of the smooth faces. Inspect the one-pixel edge on the smooth side and follow it across a horizontal block joint. Its grain and reddish-brown shades should line up with the Matcha cut side artwork.
2. Extend the cut blocks across the bottom of the smooth area to form an inside corner. Then remove the bottom cut row and stop the cut column above a row of smooth blocks to form an outside corner. The original mask is asymmetric. Check the right edge and the open space at the turn against the static comparison sheet.
3. Repeat on the four side orientations and with bottom, top and double Smooth Red Sandstone slabs. Record which slab faces receive an overlay with the installed renderer. Top and bottom faces fall outside this rule. Inspect ordinary Cut Sandstone next to Smooth Sandstone as an unchanged control.

From the canonical checkout, this command recreates the nearest-neighbor comparison, rule/tile inventory and exact archive diff. The sheet uses the archived PNGs in static side-face layouts. Pass `--continuity <path-to-continuity-3.0.1+26.2.jar>` to include the six built-in rule exclusions in the inventory.

```sh
python -B resourcepacks/foundation/tools/build_foundation_v10.py --reports .tmp/foundation-v10
```

## Native Red Sandstone blocks

Compare every inventory preview and placed form with Matcha Cut Red Sandstone under the same lighting. Look for its reddish-brown material throughout the carving, including narrow edges, recessed shadows and lit caps. Inspect all sides and break a sample of each form to check its stone particles.

| Native block ID | Placed checks |
| --- | --- |
| `bbb:red_sandstone_column` | Inspect single and joined columns on all three axes. Compare the cap, flutes and four layer variants across a joint. |
| `bbb:red_sandstone_fence` | Check the post, connected rails and filled joins from both sides. |
| `bbb:red_sandstone_frame` | Join vertical, horizontal, diagonal and corner pieces. Break the vertical piece and compare its particles with the other frame pieces. |
| `bbb:red_sandstone_moulding` | Inspect straight, inner and outer corners on both halves, with and without dentils. Check the highlight on each tooth. |
| `bbb:red_sandstone_urn` | Inspect the rim, body and foot on empty and soiled urns. The stone changes color and the soil retains its dirt artwork. |
| `mcwwindows:red_sandstone_window` | Build a resizable window at least three blocks wide and high. Inspect both axes, mirrored outer pieces, and the glass-only center. |
| `mcwwindows:red_sandstone_window2` | Rotate the rectangular window and inspect closed, locked, open-left and open-right states. |
| `mcwwindows:red_sandstone_four_window` | Check all four panes, both opening directions, the locked state and the frame around each pane. |
| `mcwwindows:red_sandstone_pane_window` | Stack the panes and check base, above, below and middle parts across closed, locked and both open states. |

The Macaw frames keep their original grain with Matcha's color mapping. Check glass transparency through each opening. Compare a regular Sandstone window and BBB Sandstone carving as controls.

To reproduce the retained v9 native-provider checks with Python and Pillow, run the following from a checkout retaining the inputs. The three reports list every selector and parent chain, the pixel counts and shade mapping for each sprite, and the SHA-256 of every changed v9 ZIP member.

```sh
python -B resourcepacks/foundation/tools/build_foundation_v9.py --reports .tmp/foundation-v9
```

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

Record the Foundation filename and SHA-256, provider versions, pack order, CTM renderer, locale, affected block and pose with each runtime observation.
