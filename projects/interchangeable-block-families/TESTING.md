# C7 focused runtime checklist

Exact candidate: `interchangeable-block-families-0.1.0-canary7.jar`, embedded version `0.1.0-canary7`, SHA-256 `599660b27db061926a800bd486f25bb1d8511b6b82b8afd2a369e8189c696b7c`.

Run only in the dedicated Matcha Flavoured 26.2 Workbench after a serialized Test Instance Manager deployment that includes BBB. C7 requires BBB but deliberately has no BBB version predicate; first verify Fabric resolves the intentionally installed `2.0pre4+26.2-pale-oak-dev.3` BBB provider and proceeds to normal mod loading.

1. For Oak and Pale Oak, use CNM's selector in both directions through Trim, Balustrade, Support, and Pallet; repeat one nether wood representative.
2. Confirm each existing Fence/Fence Gate pair still interchanges, and that its matching BBB Frame and Lattice are now selector members. Check Pale Oak specifically.
3. Confirm the Ribbits Mossy Oak fence/gate pair still interchanges and has no BBB Frame or Lattice selector member.
4. For Stone, Blackstone, Deepslate, Nether Brick, Sandstone, Red Sandstone, and Quartz, confirm Column, Urn, Moulding, Fence, and Frame interconvert only within their material family.
5. Confirm Iron Bars, BBB Iron Fence, Iron Chain, and Aurora Iron Chandelier interconvert; confirm the Iron Bars canonical recipe remains available.
6. Restart/reload once and repeat one wood, one stone, and the iron family. Stop and report any missing item, canonical recipe removal, duplicate selector/conversion entry, cross-material match, crash, or reload error.

No manual Minecraft result is recorded. C7 passed controlled Java 25 / Gradle 9.5.1 / Loom 1.17.19 focused catalog validation and production archive packaging; this is not runtime evidence. C7 is not deployed: Slot B still contains older IBF C4. C3 remains the accepted rollback.
