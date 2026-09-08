# C6 runtime result / C7 metadata correction

Exact failed C6 candidate: `interchangeable-block-families-0.1.0-canary6.jar`, embedded version `0.1.0-canary6`, SHA-256 `a086f3c3c9cf7524803fe8b9b4693976cb1da5a211e552bb629bed4092b906f6`.

## C6 observed runtime result

C6 is a runtime FAIL at Fabric Loader startup with the controlled BBB Pale Oak provider `bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.3.jar`. C6 packaged `bbb >=2.0pre4+26.2`; Fabric's version ordering treats the installed `-pale-oak-dev.3` build as a prerelease below that release floor, so Loader rejects the otherwise intended provider before Minecraft launches.

This is an IBF metadata defect, not evidence that BBB itself is functionally incompatible. Preserve the exact C6 artifact and SHA-256 as failed provenance; do not rebuild or overwrite C6.

## C7 source correction

C7 source checkpoint `6fe21c62d7147dfee77017b33f982a9e148c19ea` advances the embedded/project version to `0.1.0-canary7` and changes only the BBB runtime predicate from `>=2.0pre4+26.2` to `*`. BBB remains a required dependency, but IBF no longer constrains its version. No C7 artifact has been built, retained, deployed, or runtime-tested by this repository-only correction.

Before any C7 runtime test, build from the C7 source, retain the resulting artifact under a new C7 filename/hash, and inspect its packaged `fabric.mod.json` to confirm `"bbb": "*"` and embedded version `0.1.0-canary7`.

Then run only in the dedicated Matcha Flavoured 26.2 Workbench after a serialized Test Instance Manager deployment:

1. Confirm Fabric Loader accepts the installed BBB provider without a BBB version incompatibility.
2. For Oak and Pale Oak, use CNM's selector in both directions through Trim, Balustrade, Support, and Pallet; repeat one nether wood representative.
3. Confirm each existing Fence/Fence Gate pair still interchanges, and that its matching BBB Frame and Lattice are selector members. Check Pale Oak specifically.
4. Confirm the Ribbits Mossy Oak fence/gate pair still interchanges and has no BBB Frame or Lattice selector member.
5. For Stone, Blackstone, Deepslate, Nether Brick, Sandstone, Red Sandstone, and Quartz, confirm Column, Urn, Moulding, Fence, and Frame interconvert only within their material family.
6. Confirm Iron Bars, BBB Iron Fence, Iron Chain, and Aurora Iron Chandelier interconvert; confirm the Iron Bars canonical recipe remains available.
7. Restart/reload once and repeat one wood, one stone, and the iron family. Stop and report any missing item, canonical recipe removal, duplicate selector/conversion entry, cross-material match, crash, reload error, or renewed loader incompatibility.

C3 remains the accepted rollback. The older C4 Slot B deployment is unchanged by this correction.
