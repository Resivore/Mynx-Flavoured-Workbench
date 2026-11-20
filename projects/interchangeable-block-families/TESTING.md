# C18 verification checklist

Use the exact current JAR, SHA-256, and source checkpoint in [WORKBENCH_STATUS.json](WORKBENCH_STATUS.json).

For the AMC-present controlled run, add `"-Pamc_reference_jar=../architectural-material-closure/artifacts/AMC C14.jar"` to the Gradle invocation. Repeat the full invocation with that property omitted. With AMC C14, expect 190 families and 1,447 unique items. Without AMC, expect 181 families and 1,397 unique items. The largest family has 22 members in both runs.

The normal controlled tasks are `test runGameTest build stageCanaryArtifact`. Use the restored C9 dependencies: exact accepted Nibaru C45 and BGE C52, BBB Enderscape dev.7, Dramatic Doors C9, Ribbits C17, Quick Stack Nearby 0.4.0 with compatibility C6, and the audited external providers wired in `build.gradle`. Ribbits support is supplied through `ribbits_runtime_reference_jars`: YUNG's API compat.2, Trinkets Updated compatibility C5, GeckoLib 5.5.1, Cloth Config 26.2.155, Custom Portals C2, and Matcha Heart compatibility C11. Supply `matcha_food_resource_root` from the tracked Matcha Flavoured 1.12 data resource root. Enderscape uses the existing Lithostitched 1.7.13 and Trim Patcher 2.1 controlled inputs. These external inputs remain outside the production IBF archive. The exact BBB dependency predicate remains necessary because dev.7 contains the audited Enderscape package and its version is not ordered Fabric semantic versioning; the contract test rejects incompatible dev.6 before registry startup.

### Mud Bricks selector checks

With AMC C14 installed, start with a named stack of seven of each canonical item. Cycle through its selector in the order below and return to the parent. Check the name, count and components at every step.

| Canonical item | Alternates in order |
|---|---|
| AMC Mud Bricks Button | AMC Pressure Plate, Parapet, Louvered Shutter |
| AMC Mud Bricks Column | AMC Urn, Moulding, Fence, Frame, then Macaw Mud Brick Arrow Slit |
| AMC Mud Bricks Window | AMC Window 2, Four Window, Pane Window, then Macaw Mud Brick Gothic Window |

Check that the three parent crafting recipes and the Column stonecutting recipe remain available. Check cleanup of the alternate crafting recipes and the Urn, Moulding and Fence stonecutting recipes. Open the separate Mud Brick Path selector from `mcwpaths:mud_brick_running_bond_path` and check its original eleven Paths and Pavings.

Repeat without AMC. Neither Macaw item should expose a Mud Bricks selector, and both should remain independently craftable. Repeat the selector and recipe checks after a reload.

### Existing family checks

1. With AMC C14 loaded, repeat the following for Andesite, Diorite, and Granite. Use a named stack of seven and switch through each selector, checking count and components after every change and on return to the first item.

   | Selector | Expected order |
   |---|---|
   | AMC Button | AMC Button, AMC Pressure Plate, Macaw Parapet, Macaw Louvered Shutter |
   | AMC Column | AMC Column, Urn, Moulding, Fence, Frame, Arrow Slit (all AMC) |
   | Macaw Window | Macaw Window, Window 2, Four Window, Pane Window, AMC Gothic Window |
   | Macaw Running Bond Path | Running Bond, Strewn Rocky, Windmill Weave, Flagstone, Crystal Floor paths, followed by Diamond, Basket Weave, Square, Honeycomb, Clover, Dumble pavings |

   Open each selector from an alternate as well. Check that the AMC Button recipe and both AMC Column recipes (crafting and stonecutting) remain available. Check that the alternate crafting and stonecutting recipes are removed.

2. In both configurations, cycle the BBB Stone Column through BBB Stone Urn, Moulding, Fence, Frame, then Macaw Stone Brick Arrow Slit. Check that Cobblestone Arrow Slit keeps its independent recipe. With AMC absent, open each of the three Macaw window selectors and verify the original four windows. The material parapets and louvered shutters should keep their own recipes with no accessory selector. Verify that the C16 Stone Button family still has Button, Pressure Plate, AMC Parapet, AMC Louvered Shutter with AMC present, and the vanilla pair when absent.
3. Recheck the existing Macaw ordinary and Dramatic Doors tall-door families, trapdoors, windows/shutters, the 77 audited Macaw thin Paths and 78 Pavings, Aurora lanterns, Ribbits, BBB, vanilla fences, bars/chains, and accessories. Confirm wood shutters/louvered shutters occur only with their Button-parent wood family; Stone Button is separate from Stone thin Paths; and the Nether Brick arrow slit still belongs to its original BBB detail family. Providers retain native placement, collision, redstone, recipe costs, drops, and copper behavior. CNM retains switching and recipe/equivalence ownership. Macaw path blocks and patterned full-block/slab/stair geometry remain outside IBF.
4. Exercise all 15 literal display-fixture triples: Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Pale Oak, Bamboo, Crimson, Warped, Veiled, Celestial, and Murublight. Each uses Sign as canonical, then Hanging Sign and Shelf. Wall Sign and Wall Hanging Sign placement blocks and ordinary geometry remain outside these triples.
5. Verify Veiled, Celestial, and Murublight Fence/Fence Gate/BBB Frame/Lattice families; their three separate BBB Trim/Balustrade/Support/Pallet detail families; the seven Enderscape Button/Pressure Plate pairs; and Shadoline Bars/Chain. Confirm the original C7/C8 families and canonical parents remain intact without cross-material merging.
6. After reload, check canonical acquisition and recipe cleanup. The controlled recipe corpus removes 1,167 alternate recipes with AMC and 1,114 without it. Both runs retain every canonical recipe. Stone Running Bond Path remains obtainable as the Stone Path parent, while Stone Strewn Rocky Path is removed. Five Enderscape `void_lachryma` conversions remain provider-owned at CNM's custom-recipe boundary. Recheck a pre-existing Macaw and BBB canonical recipe and the separate accepted Nibaru/BGE component.
7. Transfer component-bearing stacks through a vanilla, Enderscape, and BBB family; preserve exact count and component patch. Mismatched components and incompatible targets must fail closed. Check Quick Stack Nearby affinity and the selector viewport/transfer behavior for a 22-member family.
8. Restart or reload once, repeat representative C7, C8, and C9 families, and report only observed missing members, duplicate membership, canonical-parent changes, cross-family matches, recipe drift, crashes, or reload errors against the exact release identity.

This checklist is lifecycle-neutral and does not authorize access to a Minecraft testing profile. Runtime evidence and acceptance require separate owner direction.
