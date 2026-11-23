# Mossy Oak transfer study

Use the stripped log side and the beam to judge highlights. Use the bark side and the shelf recesses to judge shadows. I recommend **darken only**. Both candidates cap the moss at its Ribbits shade. The continuous clamp also lets the moss follow the darkest bark grooves. The bounded ramp runs out of darker colors for 51 of the bark side’s 63 moss texels.

## Side-by-side previews

Run preview_moss_methods.py for the local sheet `comparison-1.png` in test-builds/mossy-oak-method/.

Run preview_moss_methods.py for the local sheet `comparison-2.png` in test-builds/mossy-oak-method/.

Run preview_moss_methods.py for the local sheet `boat-detail.png` in test-builds/mossy-oak-method/.

## How the three treatments work

The Oak and Ribbits Mossy Oak plank pair identifies 63 moss texels per 16×16 tile. The current selector requires changed source pixels with red minus green at most 13. Both prototypes use those same UVs. The shelf repeats the footprint in native tiles and keeps the existing wood-color filter. The boat repeats it on opaque pixels in the entity atlas. Both retain target alpha and copy all unselected texels.

The current footprint treatment scales each source moss channel by target luminance divided by Oak plank luminance at the corresponding reference UV. It rounds channels to integers and caps them at 255. Luminance uses weights 299, 587 and 114 for red, green and blue. A lighter target can therefore make bright yellow-green flecks.

**Darken only** caps the multiplier at 1 before rounding. A bright target keeps the exact Ribbits color. A dark target can carry the moss below the darkest reference olive.

**Bounded ramp** uses the same capped target luminance, then picks the nearest eligible Ribbits olive by luminance. Eligible shades are the original shade and darker entries. Ties favor the darker entry. For this prototype the four original RGB colors serve as the approved palette, from dark to light: #76692F, #86803C, #9D9D4B and #ACA14F. The ramp keeps the pixel-art palette compact, but subtle shadows can round away and deep shadows stop at #76692F.

| Surface | Selected texels | Brightened by uncapped transfer | Desired shadows below ramp floor |
|---|---:|---:|---:|
| Log | 63 | 1 | 51 |
| Log top | 63 | 22 | 2 |
| Stripped log | 63 | 37 | 0 |
| Stripped log top, footprint replay | 63 | 22 | 2 |
| Beam | 63 | 27 | 0 |
| Shelf | 252 | 53 | 109 |
| Boat entity atlas | 771 | 289 | 97 |

The shipped column comes from Mynx Trees C10, BGE C111 and AMC C36. The uncapped replay matches six of seven shipped textures pixel for pixel. **Stripped log top is the exception.** Its shipped art maps the entire Oak end grain through a palette extracted from the treated stripped side. Both prototypes show the requested 63-texel Ribbits footprint on the vanilla stripped top. Differences from its shipped column include placement. Compare the two candidate columns directly to isolate shading.

## Later rollout

Change the gain in AMC’s tools/mossy_oak.py (moss_sheet) and the Mossy Oak branch of tools/doors.py (transfer) while retaining existing UV and protected-art routing. The shelf and boat call moss_sheet through wood_matrix.py and tree_architecture.py. Mirror the gain change in BGE’s PrivateBeamResourceAssembler.mossTransfer. Mynx Trees’ native_wood_resources.py copies three AMC timber textures from its pinned input and separately calls transfer for the stripped top. Refresh that input for a rollout and decide explicitly whether to adopt the demonstrated footprint on the stripped top. Update the pixel-replay verifiers with the chosen method. The owner-authored doors, shutters and Ribbits plank source are outside the rollout.

This is a static texture comparison from main f03a432. The preview generator reads retained source archives and writes private review images locally.

