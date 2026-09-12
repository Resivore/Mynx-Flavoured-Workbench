# Testing

## Canary 1 runtime procedure

Use only `macaws-trapdoors-moreculling-compat-0.1.0-canary1.jar`, embedded
version `0.1.0-canary1`, SHA-256
`d6ffd125e082978b594ca01c075bb706ddf7f5696dee1e7d15e357cbbfdb4af5`.
The controlled providers are `mcw-trapdoors-1.1.5-mc26.2fabric.jar` at
`6411cb0ff6c6cc4312deed48c3ca69cdbcbae72e80ff4ed0269f2d0a78acd32d`
and `moreculling-fabric-26.2-1.8.1.jar` at
`ea04505496e4d35a8c94199884b6fafa69057efe50f2096d2988c11163d49122`.
Use only the dedicated Matcha Flavoured 26.2 Workbench through the serialized
Test Instance Manager. Never access the protected 26.1.2 gameplay profile.

1. Verify the dedicated profile and deploy Canary 1 as a member of a managed
   test-slot cohort without removing the other slot or any retained companion.
   Confirm `CURRENT_RELEASE_DEPLOYED / READY_TO_TEST_VERIFIED` before launch.
2. In a disposable Creative test world, build a white-concrete test surface at
   least three blocks thick so any missing adjacent face reads as an obvious
   dark or terrain-colored X-ray hole. Use representative
   `mcwtrpdoors:oak_ranch_trapdoor` and `mcwtrpdoors:oak_bark_trapdoor` blocks.
3. For each representative, test a closed bottom trapdoor on a solid floor, a
   closed top trapdoor against a solid ceiling, and an open vertical trapdoor
   against a solid wall. Include floor, wall, and ceiling contact where the
   model's genuine openings or glass/open arrangement exposes the neighboring
   block face.
4. Inspect every placement straight on and at shallow/grazing angles from all
   useful sides. Look through the model's transparent/open regions and along
   every solid contact edge.
5. Confirm the openings remain visibly transparent/open, the adjacent solid
   faces remain fully rendered, no dark X-ray/light hole appears, and ranch and
   bark geometry/textures otherwise match their original appearance.
6. Open and close both representatives by hand and with redstone. Re-place
   bottom/top/wall orientations, walk/collide against them, break them, confirm
   normal drops, craft or inspect their recipes, and listen for unchanged
   placement/open/close/break sounds.
7. Place at least one unrelated Macaw trapdoor such as
   `mcwtrpdoors:oak_barn_trapdoor` or `mcwtrpdoors:oak_glass_trapdoor` beside
   the same surface and confirm its appearance and behavior remain unchanged.
8. With the existing IBF candidate present, switch into and out of representative
   ranch/bark trapdoors through the normal IBF interaction. Confirm the exact
   `mcwtrpdoors:*` block/item identity remains recognized and the post-switch
   ranch/bark rendering remains correct.
9. Exit cleanly and inspect `latest.log` for compatibility initialization,
   Fabric dependency, Mixin, renderer, or resource errors before recording a
   result for this project only.

Pass only if all visual, interaction, unrelated-trapdoor, and IBF checks above
are observed on the exact deployed canary. Stop and record `FAIL` for a
reproduced X-ray hole, changed ranch/bark appearance, unrelated trapdoor change,
or behavior regression. Record `INCONCLUSIVE` for drifted hashes/versions,
incomplete orientations/angles, ambiguous lighting, missing IBF availability,
startup/resource errors, or inability to bind observations to the exact managed
deployment. Do not infer a runtime pass from compilation, provider/API checks,
manager readiness, or startup alone.

