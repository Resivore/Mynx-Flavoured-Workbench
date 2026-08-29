# Testing

This project has no project-owned compatibility artifact. Its preserved visual
behavior is Simple Trash Slot 1.0.4's built-in `H2` placement: frame origin
`(151,61)` and click/slot origin `(152,62)` in the accepted Matcha inventory
layout. Do not add a coordinate mixin, duplicate positioning layer, or logical
slot change to reproduce that placement.

The frozen legacy record retains the user's 2026-08-24 confirmation that the
approved visual/resource-pack scope was complete. It does not contain a
project-local artifact or a separately reported result for every check below.
This migration performed no Minecraft launch, deployment, or runtime check.

Current `main` records the accepted layout authority as Inherent 3×3 Inventory
Crafting UUID `357cd94a-4ab2-54c9-ac6c-4049fea08bb9`, exact Canary 4 SHA-256
`6e797291dd6c68f5afe65f91177ad2653f7c4a99e10bd20e078fafd7aaf6cb33`.
Its canonical legacy source commit is
`34e8de533e804ab4993b7502d7c5afbec6ebc8ca`. That artifact remains owned by
its own project and is not copied here.

## Focused visual regression procedure

Use the exact external Simple Trash Slot reference
`simple_trash_slot-1.0.4+26.1.2-fabric.jar` (mod ID `simple_trash_slot`, version
`1.0.4`, SHA-256
`81790940da606f732a565adca2b4ec9fa0e8e0015cb1cdc1c4e009506244739b`)
with its `H2` placement selected.

1. Open Survival Inventory and confirm the Matcha-styled frame at `(151,61)`
   agrees with the actual click target at `(152,62)`.
2. Check empty, occupied, hover, and highlight presentation; confirm the slot
   reads as part of the Matcha inventory and the normal trash action is
   unchanged.
3. Confirm no overlap or click-target conflict with the crafting result and
   recipe-book control, Inventory Extended rows, or Trinkets slots when those
   elements are present.
4. Repeat at representative GUI scales and in Creative -> Survival Inventory
   if Simple Trash Slot is present there.
5. Reload resources, reopen the inventory, and inspect the log for relevant
   GUI, sprite, resource, mixin, or menu errors.

Stop and record a failure or inconclusive result if the artwork and click
target disagree, the trash action changes, another control overlaps the slot,
the built-in placement is not honored, or relevant errors appear. Reopen this
project only for a newly demonstrated visual defect; do not infer a need for a
custom compatibility layer from this migration.
