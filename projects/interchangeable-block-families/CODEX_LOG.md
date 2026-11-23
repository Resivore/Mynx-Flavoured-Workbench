# Codex Log

## 2026-08-29T22:42:11Z — Migrate frozen Interchangeable Block Families into Mynx Flavoured Workbench
- Revision: 1
- Source checkpoint: `5597f2a8f9a65950b55c501ee2b004b30e8443ae`
- Changes: Migrated the exact maintainable C4 implementation from legacy branch snapshot `1173318ecd5a35636fa0f391b397543093aa7adf`, reconciled with frozen status authority `f0101f38c446dddaa4226450386402c63597ba7c`, preserved immutable UUID `ee8a20f8-d0a8-515c-9a74-3b19f238227d`, normalized only V2 artifact/reference and build-launcher paths, and retained exact current C4 plus accepted/effective-rollback C3. Legacy README, BUILD_PROVENANCE, PROVIDER_AUDIT, C1/C2 artifacts and matrices, older canary tools, global notes, manager records, snapshots, branches, worktrees, and Git history were not imported.
- Build/static: Forty frozen source, fixture, and build-metadata files matched the C4 snapshot; Temurin Java 25.0.4+7, Gradle 9.5.1, and Fabric Loom 1.17.19 `clean test build --no-daemon` passed 36/36 JUnit checks in 10 suites and the lifecycle-bound 19/19 Fabric GameTests. The fresh 51,171-byte production JAR was byte-identical to retained C4, both retained artifacts passed filename, size, SHA-256, mod ID/version, and package-isolation verification, and build/reference changes did not alter production implementation behavior. These are controlled automated results, not in-game validation.
- Runtime: No Test Slot or accepted-baseline mutation, deployment, dedicated Workbench or protected-profile access, or manual/in-game runtime test occurred. Gradle's isolated GameTest harness is recorded only as controlled validation; it creates no deployment or runtime evidence. Exact C4 remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; only exact C3 retains its prior aggregate focused runtime pass and accepted/live classification.
- Artifact: Current C4 is `interchangeable-block-families-0.1.0-canary4.jar`, 51,171 bytes, SHA-256 `8f805d0cc6cf65599c8fab190be2aa023006a9bd990bcdd2bf0c45f62f1a2968`, source `9f5a72fff4e8370cdf031006f6658399550f609f`, retained at `4d70fa059177ccf8b0af867c2d07c33ab236a0a9`; accepted/rollback C3 is `interchangeable-block-families-0.1.0-canary3.jar`, 47,456 bytes, SHA-256 `d0150aea777dd7837d0c9b4f5cf41ce45f510c60ef416fb7001d06a36a6b0e87`, source `34796a9af6b2da235c3ff9d0e3338f8e6af0e137`, retained/deployed at `ce470e026a1882e427b39ac7acd2d8b2bca45f58` and promoted byte-identically at `b71932fb4c917edfa2c162600b01a4c1f2983dfe`.
- Result: ACTIVE — current C4 remains `CONTROLLED_VALIDATION_PASS`, `NOT_DEPLOYED`, and `RUNTIME_UNTESTED`; exact accepted/live C3 remains the rollback; no blocker is recorded. Mynx repository and Sheet synchronization initialize this previously absent UUID at revision 1 while frozen legacy revision 4 remains provenance only.
- Next state: Under explicit runtime ownership, deploy exact C4 into one experimental slot alongside the cumulative accepted stack and complete only the focused C4 procedure; preserve exact accepted C3 until C4 receives an independently reported result and explicit promotion.

## 2026-09-07T03:57:38.772589Z — Deploy exact C4 to Slot B after clock-skew recovery
- Revision: 2
- Source checkpoint: `29ed69c35fe1b34bc8ba99a645593471005a3bdc`
- Changes: Added the missing embedded C4 version binding to the canonical release record and atomically replaced the independent QSN C9 Slot B cohort with exact C4. C3 remains the accepted disabled rollback; C4 is deployment `573a027d-5579-4a2b-aefd-a4c57a6ee34a`, artifact `3d2a3524-6242-43b1-b0a8-c3a6e592087e`.
- Build/static: The focused manager regression suite passed 73 tests, including revision-110 backward-clock coverage. The manager preflight reverified the exact C4 filename, SHA-256, embedded Fabric version, current-manifest identity, and enabled dependency graph.
- Runtime: No Minecraft client launched and no runtime observation was recorded. The manager physically installed C4 and verified it `READY_TO_TEST_VERIFIED` at the actual observed UTC `2026-09-07T03:57:38.772589Z`; prior state `updated_at` remained `2026-09-07T04:02:01Z` as the logical watermark because the host clock was behind it. This is timing metadata, not fabricated runtime evidence.
- Artifact: Current C4 remains `interchangeable-block-families-0.1.0-canary4.jar`, 51,171 bytes, SHA-256 `8f805d0cc6cf65599c8fab190be2aa023006a9bd990bcdd2bf0c45f62f1a2968`, source `9f5a72fff4e8370cdf031006f6658399550f609f`; C3 remains exact accepted rollback.
- Result: TESTING / CONTROLLED_VALIDATION_PASS / READY_TO_TEST_VERIFIED / RUNTIME_UNTESTED. Manager revision 111 / Stack v27, state SHA-256 `e1c91ad96695cbdeface9554f33079484b775b674a863aef0892ba5a53edc8f9`, physical inventory SHA-256 `dafdb93c1f970c3775571cdfffe84b8d6f95432e3f36dd158c2d376d62f24c84`; 44 managed files verified. Protected gameplay profile was not accessed.
- Next state: Run only the focused C4 runtime procedure against this exact Slot B artifact, record only supplied observations, and preserve C3 until an explicit independent PASS supports promotion.

## 2026-09-07T21:18:28.4948692Z — Preserve C5 Aurora, Ribbits, and paving source after recipe-cleanup regression
- Revision: 3
- Source checkpoint: `e86b80dd6c6be66d43789679c6563059f26c64bd`
- Changes: Reconciled preserved C5 source with current `origin/main`; bound all C5 validation metadata to exact Dramatic Doors C8 (internal C8 / filename canary7) `dramaticdoors-1.20.1-3.3.3+26.2-workbench-canary7.jar` SHA-256 `43c3efd19619a59a10957b5893adce081766f653d8cccb13f38bfbaacec2cefb`; added nine literal Aurora chandelier members, the Ribbits Mossy Oak fence/gate family, and audited Macaw Paths pavings. Added a runtime assertion that canonical ShapeMap parents are never also inverse-map alternates.
- Build/static: Java 25 / Gradle 9.5.1 / Fabric Loom 1.17.19 JUnit validation passed before the GameTest-only parent assertion; the full C8 dependency harness compiled and loaded 82 Fabric mods, 3,802 recipes, 2,359 advancements, and the exact 134-family / 1,209-member catalog. Dramatic Doors C8 completed reload without the prior immutable-map, advancement-reload, or Mixin failure.
- Runtime: No Test Instance Manager action, dedicated Workbench change, protected-profile access, or manual/in-game observation occurred. The full controlled GameTest ran 18 IBF checks: 17 passed and `recipeCleanupRemovesNewNonParentsAndRetainsCanonicalAcquisition` failed because `minecraft:iron_bars` was absent after CNM cleanup. With CNM `recipe_fixes=false`, canonical Iron Bars was present and the expected non-parent cleanup check failed instead; with cleanup enabled, the resolved ShapeMap still proved Iron Bars is a parent, not an alternate. This is controlled validation only, not runtime evidence.
- Artifact: No C5 artifact was produced, retained, deployed, or promoted. The older C4 Slot B artifact remains independently deployed; accepted C3 remains the exact rollback.
- Result: TESTING / CONTROLLED_VALIDATION_FAIL / NOT_DEPLOYED / RUNTIME_UNTESTED for repository-current C5. TESTING is retained solely because immutable project UUID `ee8a20f8-d0a8-515c-9a74-3b19f238227d` remains represented by the untouched older C4 Slot B deployment; C5 itself is not deployed.
- Next state: Diagnose and correct the narrow CNM/IBF recipe-cleanup interaction without transferring CNM cleanup ownership to IBF, then rerun the full exact C8 dependency GameTest before creating a C5 artifact.

## 2026-09-07T22:10:38.5106842Z — Finalize C5 canonical-parent cleanup compatibility
- Revision: 4
- Source checkpoint: `e07724cfe756e56b228f85cdaa153e56c29ec75e`
- Changes: Inspected unchanged local CNM 2.0.7 bytecode and confirmed its cleanup match includes the resolved parent in `ShapeMap.getShapes(result)`, causing a canonical parent-result recipe to be discarded. Added one IBF mixin at that cleanup call site: audited IBF canonical parents are excluded only from that match set, while IBF alternates and non-IBF CNM families remain unchanged. Added runtime proof that `minecraft:iron_bars` and every listed canonical acquisition survive, while Iron Chain, C5 alternates, and an unrelated CNM alternate remain removed. C5's packaged dependencies now declare capability/provider floors; exact validation bytes remain recorded here rather than pinned as runtime requirements. The test-only GameTest input can mount canonical Matcha data required by the pinned Ribbits provider without bundling it in C5.
- Build/static: Clean Java 25 / Gradle 9.5.1 / Fabric Loom 1.17.19 validation passed: 36/36 JUnit checks and a production build. Exact Dramatic Doors C8 `dramaticdoors-1.20.1-3.3.3+26.2-workbench-canary7.jar` SHA-256 `43c3efd19619a59a10957b5893adce081766f653d8cccb13f38bfbaacec2cefb` loaded with the pinned provider chain; the full IBF GameTest suite passed 18/18. The prior C5 17/18 Iron Bars failure remains preserved in revision 3.
- Runtime: No Minecraft client/manual observation has occurred. C5 is retained but not yet deployed; C4 Slot B remains unchanged at this administrative checkpoint.
- Artifact: C5 `interchangeable-block-families-0.1.0-canary5.jar`, 53,955 bytes, SHA-256 `f9086f493df13142ffceb306caff1231d424ad6cca49f357195f0eccd0f44d0a`, embedded version `0.1.0-canary5`, source `e07724cfe756e56b228f85cdaa153e56c29ec75e`.
- Result: TESTING / CONTROLLED_VALIDATION_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED. C3 remains accepted rollback; C5 must replace only C4 in Slot B through the serialized manager before manual testing.
- Next state: Retain C5 canonically, deploy its exact bytes to Slot B while preserving C4 rollback provenance and Slot A, then perform only the focused C5 checklist.

## 2026-09-08T01:30:02.3514110Z — Add BBB detail families Canary 6
- Revision: 5
- Source checkpoint: `414553afd5872bfdb295c7c2142141069e5cb552`
- Changes: Used current BBB main as the literal provider authority (BBB Pale Oak C3, `bbb-fabric-26.2-2.0pre4+26.2-pale-oak-dev.3.jar`, SHA-256 `d0928b9316e3f6b323151fac73df3be1056aba05b56d4909b419cc15c09fc4d0`). Added 12 four-member BBB Trim/Balustrade/Support/Pallet wood families including Pale Oak; extended the existing 12 vanilla Fence/Fence Gate families in place with their matching BBB Frame/Lattice; preserved Ribbits Mossy Oak as its original two real members; added seven five-member BBB Stone/Blackstone/Deepslate/Nether Brick/Sandstone/Red Sandstone/Quartz detail families; and added BBB Iron Fence to the existing Iron Bars/Iron Chain/Aurora Iron Chandelier family. The literal catalog is now 153 disjoint families and 1,317 unique members; no provider IDs were inferred or fabricated.
- Build/static: BBB's clean Java 25 / Gradle 9.5.1 / Loom 1.17.20 production build produced the exact current provider JAR above. IBF's clean Java 25 / Gradle 9.5.1 / Loom 1.17.19 focused suite passed 17/17 catalog tests, including exact families, one canonical Fence/Fence Gate family per wood, Mossy Oak exclusions, exact seven stone sets, existing iron-family extension, uniqueness, and stable catalog serialization. Production archive inspection confirmed `interchangeable_block_families` version `0.1.0-canary6`, runtime `bbb >=2.0pre4+26.2`, 30 entries, and no bundled BBB, CNM, or GameTest classes. The historical full provider/GameTest harness was not run from this clean worktree because its pinned Aurora provider artifact is unavailable locally; this is a validation limitation, not runtime evidence.
- Runtime: No Minecraft launch, managed deployment, slot mutation, or profile access occurred. Both experimental slots are occupied; older IBF C4 remains the independent Slot B member. C6 also requires BBB in the managed dependency graph, which is not currently installed there, so no safe serialized C6 replacement preflight exists. C6 is `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; project lifecycle remains `TESTING` solely because the immutable project UUID is represented by older C4 in Slot B.
- Artifact: Current retained ignored artifact is `interchangeable-block-families-0.1.0-canary6.jar`, 55,427 bytes, SHA-256 `a086f3c3c9cf7524803fe8b9b4693976cb1da5a211e552bb629bed4092b906f6`, built from source checkpoint `414553afd5872bfdb295c7c2142141069e5cb552`. C3 remains accepted rollback.
- Result: TESTING / CONTROLLED_VALIDATION_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED. The focused runtime checklist now covers representative wood including Pale Oak, fence/gate/frame/lattice behavior, Mossy Oak exclusion, all seven stone families, iron, duplicate outputs, and reload.
- Next state: Under explicit runtime ownership, make BBB available as a compatible managed provider and atomically replace only IBF's existing Slot B member with exact C6; then complete the focused manual checklist before any promotion.

## 2026-09-08T02:22:33.4982785Z — Remove IBF BBB version restriction Canary 7
- Revision: 6
- Source checkpoint: `9ad4600e62808e5e976392a69b495a2fe4b5d47c`
- Changes: Created metadata-only C7 from current C6. The packaged `fabric.mod.json` retains `bbb` under `depends` but changes its predicate from `>=2.0pre4+26.2` to Fabric's unrestricted `*`; no BBB artifact, IBF catalog, or IBF runtime implementation changed. Canonical dependency metadata now distinguishes the required any-version BBB capability from the exact pale-oak development JAR used only as a controlled-validation input.
- Build/static: Clean Java 25 / Gradle 9.5.1 / Fabric Loom 1.17.19 `clean test build --no-daemon` compiled and packaged C7, but the full JUnit task reported 39/40 passing: unchanged `RecipeImpactContractTest.recipeImpactFixtureIsBoundToTheExactExpandedCatalog` fails because its C5 expected catalog SHA-256 `46FA910A483BDDA19EAEE2F4D95F6363AC9F838B4510E5A9525E3A12B818EC53` predates the C6 catalog and computes `D656991C5E93F2EE4E24055A0CBB433B1C7F0CFB8FF6E78CEA98185E8C519C26`. This pre-existing C6 fixture inconsistency is outside the metadata-only change and was not altered. The focused C6/C7 catalog suite passed 17/17. Final archive inspection confirms `interchangeable_block_families` version `0.1.0-canary7`, `depends.bbb` exactly `*`, 30 entries, and no bundled BBB, CNM, or GameTest classes. Fabric Loader 0.19.3 `VersionPredicate.parse("*")` accepted both `2.0pre4+26.2` and `2.0pre4+26.2-pale-oak-dev.3`.
- Runtime: No Minecraft launch, Test Instance Manager action, deployment, slot mutation, or profile access occurred. C7 remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; lifecycle remains `TESTING` solely because the immutable project UUID is still represented by older C4 in Slot B. The protected Matcha Flavoured 26.1.2 gameplay instance was not accessed.
- Artifact: Current retained ignored artifact is `interchangeable-block-families-0.1.0-canary7.jar`, 55,422 bytes, SHA-256 `599660b27db061926a800bd486f25bb1d8511b6b82b8afd2a369e8189c696b7c`, built from source checkpoint `9ad4600e62808e5e976392a69b495a2fe4b5d47c`. C3 remains the accepted rollback.
- Result: TESTING / CONTROLLED_VALIDATION_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED. The full-suite fixture failure is recorded separately above and no Minecraft runtime outcome is inferred.
- Next state: Under explicit runtime ownership, atomically replace only IBF's existing Slot B member with exact C7 while preserving Slot A, confirm Fabric accepts the installed BBB provider and proceeds past dependency resolution, then complete the focused manual checklist before any promotion.

## 2026-09-14T08:09:29Z — Owner-directed lifecycle acceptance

- Revision: 7
- Source checkpoint: `f511f435c623883c5ee1474e9227acb73fb2df68`
- Changes: Changed lifecycle from TESTING to ACCEPTED and bound the unchanged exact current release as accepted by project-owner direction; historical testing records remain intact.
- Build/static: Preserved the existing CONTROLLED_VALIDATION_PASS classification; no build or artifact substitution was performed.
- Runtime: No Minecraft runtime testing was performed by Codex for this administrative transition. The project owner's external/gameplay testing authorized acceptance, but no new PASS or other runtime result was recorded or inferred; the existing RUNTIME_UNTESTED classification is unchanged.
- Artifact: Current release identity is unchanged: version C7 (0.1.0-canary7), filename interchangeable-block-families-0.1.0-canary7.jar, SHA-256 599660b27db061926a800bd486f25bb1d8511b6b82b8afd2a369e8189c696b7c, source 9ad4600e62808e5e976392a69b495a2fe4b5d47c.
- Result: ACCEPTED — owner-directed acceptance without fabricated runtime evidence.
- Next state: Preserve the accepted release and historical evidence. A future successor may use TESTING while awaiting or receiving user runtime validation; no Workbench slot or profile allocation is required.

## 2026-09-21T19:30:02.3302694Z — Add literal display fixtures and Enderscape families Canary 8

- Revision: 8
- Source checkpoint: `745714aadb669409a172ec7a4e0cacb9a6dcf98f`
- Changes: Audited the actual Minecraft 26.2 merged registry JAR and exact Enderscape 3.0.2 provider rather than deriving names. Added one `DISPLAY_FIXTURE` architecture and the complete eligible literal set of 12 vanilla plus Veiled, Celestial, and Murublight Sign/Hanging Sign/Shelf triples with Sign canonical. Added the exact three Enderscape Fence/Fence Gate pairs, seven Button/Pressure Plate pairs, and Shadoline Bars/Chain pair through the existing `FENCE_GATE`, `BUILDING_ACCESSORY`, and `BAR_CHAIN` architectures. Wall-sign placement blocks, Enderscape doors/trapdoors, and ordinary BGE geometry remain excluded. All accepted C7 families and canonical parents are unchanged; the exact C7 artifact remains the accepted baseline.
- Build/static: Java 25, Gradle 9.5.1, and Loom 1.17.19 clean validation passed 50/50 JUnit tests and all 25 required Fabric GameTests. The controlled server loaded 85 mods, the exact 179-family / 1,384-member catalog, 4,712 recipes, and 3,012 advancements. The catalog digest is `3407A182B2C41E8E05D19CF7BA5CCE400E622EDB93AF8B3610E23C36B3874CC7`; filtering only approved C8 additions reproduces accepted C7 digest `D656991C5E93F2EE4E24055A0CBB433B1C7F0CFB8FF6E78CEA98185E8C519C26`. Tests prove the exact 15 display triples, three Enderscape fence families, seven accessories, Shadoline pair, 30 wall-block exclusions, uniqueness, unchanged predecessor catalog, component/transfer fail-closed behavior, QSN affinity, viewport behavior, and recipe policy. Of 76 provider recipes touching the 67 new members, all 30 canonical-result recipes remain; 41 ordinary alternate-result crafting recipes are removed; five Enderscape `void_lachryma` alternate conversions remain unchanged at CNM's existing custom-recipe boundary. Production isolation found 30 entries, 19 IBF production classes, no nested JARs, and no bundled Enderscape, CNM, BBB, Minecraft, or GameTest provider classes.
- Runtime: No manual Minecraft test, deployment, testing-profile inspection or mutation, server-release mutation, or owner acceptance occurred. The Fabric GameTest harness is controlled automated evidence only; C8 remains `RUNTIME_UNTESTED`.
- Artifact: Current/unaccepted C8 is `interchangeable-block-families-0.1.0-canary8.jar`, embedded version `0.1.0-canary8`, 56,854 bytes, SHA-256 `496335c83a8fa990d0ac7267c5920cf8918598be16af3e5f3b1a0cfe8816d043`, built at `2026-09-21T19:28:10.1759522Z` from source checkpoint `745714aadb669409a172ec7a4e0cacb9a6dcf98f`. Exact Enderscape input was `enderscape-fabric-3.0.2+mc26.2.jar`, 104,162,822 bytes, SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`; the audited Minecraft 26.2 merged JAR was 37,399,549 bytes, SHA-256 `e29fddd54a12fdbb7a0ccc899ad7ce6b5167f5cbcef7c305ee3069ed7a4cc1ef`. Exact C7 remains accepted unchanged at 55,422 bytes and SHA-256 `599660b27db061926a800bd486f25bb1d8511b6b82b8afd2a369e8189c696b7c`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED. C8 is the current candidate; exact C7 remains accepted until explicit owner direction.
- Next state: Perform only the focused C8 verification in an owner-approved isolated environment, record only supplied observations, and preserve C7 until explicit acceptance.

## 2026-09-22T02:08:38.0352945Z — Extend BBB Enderscape families Canary 9

- Revision: 9
- Source checkpoint: `464b8b334581c17277aa1512af12017c51355345`
- Changes: Used only the exact BBB Enderscape provider `bbb-fabric-26.2-2.0pre4+26.2-enderscape-dev.7.jar`, 1,363,575 bytes, SHA-256 `57ddb5dfe62f2eb9f4a2ce22fbeeb5cce4386bbd93aab3f7df0dd8e6d19ddaf0`. Extended the existing Veiled, Celestial, and Murublight Enderscape Fence/Fence Gate families in place with matching BBB Frame/Lattice members, and added exactly three BBB Trim/Balustrade/Support/Pallet detail families. The literal catalog is now 182 disjoint families and 1,402 unique members. BBB Wall, Beam, Beam Stairs, Beam Slab, and Lantern remain excluded. Fabric metadata requires exactly `=2.0pre4+26.2-enderscape-dev.7`: an equality predicate is used because this provider version is not Fabric Extended SemVer and C9 needs its audited package rather than an ordered range.
- Build/static: Clean Java 25 / Gradle 9.5.1 / Fabric Loom 1.17.19 validation passed all 56 JUnit tests, all 26 required Fabric GameTests, and production-JAR isolation. The controlled server loaded 85 mods, 4,751 recipes, 3,039 advancements, and the exact 182-family / 1,402-member catalog. The C9 catalog digest is `0FF094610F07F1D93E6B95F95AC61F6011BB10423D77CE34548CF973B23CEEA7`; filtering C9 additions reproduces the unchanged C8 digest `7804116884B0682F3444DAC0C3560DAA8173E0C2A8EC820EE69BDF016CD3540E`, while filtering all C8/C9 additions reproduces accepted C7 digest `D656991C5E93F2EE4E24055A0CBB433B1C7F0CFB8FF6E78CEA98185E8C519C26`. Of 94 provider recipes touching 85 C8/C9 added members, 33 canonical-result recipes remain, 56 ordinary alternate-result crafting recipes are removed, and five Enderscape `void_lachryma` conversions remain provider-owned at CNM's custom-recipe boundary. Production isolation found 30 entries, 19 IBF production classes, no nested JARs, and no bundled provider or GameTest classes.
- Runtime: No manual Minecraft test, deployment, testing-profile inspection or mutation, server-release mutation, or owner acceptance occurred. The Fabric GameTest harness is controlled automated evidence only; C9 remains `RUNTIME_UNTESTED`.
- Artifact: Current/unaccepted C9 is `interchangeable-block-families-0.1.0-canary9.jar`, embedded version `0.1.0-canary9`, 57,103 bytes, SHA-256 `0e3d487170870d1e7e60c34d9bde250aa0999074cb4c0b6851e2d80f9864d25e`, built at `2026-09-22T02:08:09.1416977Z` from source checkpoint `464b8b334581c17277aa1512af12017c51355345`. Exact C7 remains accepted unchanged at 55,422 bytes and SHA-256 `599660b27db061926a800bd486f25bb1d8511b6b82b8afd2a369e8189c696b7c`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED. C9 is the current candidate; exact C7 remains accepted until explicit owner direction.
- Next state: Perform only the focused C9 verification in an owner-approved isolated environment, record only supplied observations, and preserve C7 until explicit acceptance.

## 2026-09-25T02:58:25Z — Integrate AMC masonry details as Canary 10

- Revision: 10
- Source checkpoint: `f69ff26b35b5898384f991ae06722de1c1099b2f`
- Changes: Added exact AMC C1 as an external required provider and introduced a neutral `MASONRY_DETAIL` category rather than presenting AMC blocks as BBB-owned. C10 adds exactly eight literal Column/Urn/Moulding/Fence/Frame families—Andesite, Diorite, Granite, Brick, Mossy Stone Brick, Cobbled Deepslate, Mud Brick, and Dark Prismarine—for 40 new disjoint catalog members. Existing C7/C8/C9 families and their canonical parents are retained; no provider namespace is inferred or modified. C10's recipe-cleanup fixture includes all 40 native-material AMC stonecutting recipes, retaining only the Column canonical recipe in each family and removing the four alternate-form results.
- Build/static: Java 25, Gradle 9.5.1, and Fabric Loom 1.17.19 passed all 58 focused JUnit catalog, exact-AMC-artifact, and assembled-cleanup contracts. Production-JAR isolation passed. The broader GameTest launch resolved no test because the pre-existing Ribbits support set lacks yungsapi, trinkets_updated, geckolib, cloth-config2, customportals, and matcha_heart_death_compat; no provider, runtime profile, or test support was changed to work around that unrelated setup failure.
- Runtime: No Minecraft deployment, launch, testing-profile access, server-release mutation, or manual runtime observation occurred. C10 remains runtime untested.
- Artifact: Current/unaccepted C10 is `interchangeable-block-families-0.1.0-canary10.jar`, 57,827 bytes, SHA-256 `D4A82BE49CBA715A73A0C949B8165B0CB4660EB33DE8AA794C75B85780190EC3`, built at `2026-09-25T02:58:12.5820093Z` from source checkpoint `f69ff26b35b5898384f991ae06722de1c1099b2f`. Its exact AMC C1 provider is `architectural-material-closure-0.1.0-canary1.jar`, SHA-256 `49AE754AFDA6A1FAD959F11E1C51027A5B2DBC00CB5108CF69A70D3264C63999`.
- Result: ACTIVE — C10 is `STATIC_PASS / RUNTIME_UNTESTED`; exact C7 remains the accepted baseline.
- Next state: Repair the unrelated controlled GameTest support wiring only under its owning scope, then execute the C10 manual matrix against these exact C1/C10 bytes before acceptance or a successor.

## 2026-09-25T05:26:35Z — Integrate AMC masonry closure as Canary 11

- Revision: 11
- Source checkpoint: `acc3a821da106dc58fd08e72741da026be291192`
- Changes: Added literal C3 masonry Detail, Window, and Building Accessory families across all 17 audited clumped profiles (233 families / 1,810 unique members). C11 integrates BBB details, paths/pavings/parapets, standard/Gothic/arrow-slit windows, and louvered shutters. It changes Fabric's AMC requirement from the incorrect C1 equality pin to `architectural_material_closure: "*"`; the exact C3 filename/SHA/version remain controlled-validation evidence only.
- Build/static: Java 25, Gradle 9.5.1, and Fabric Loom 1.17.19 passed focused C11 catalog, artifact, dependency, and recipe-cleanup contracts. The controlled C3 artifact has 408 item/blockstate/loot entries; recipe cleanup retains canonical first forms and removes 357 C3 non-parent alternatives.
- Runtime: No Minecraft deployment, testing-profile access, or manual runtime observation occurred.
- Artifact: Current/unaccepted C11 is `interchangeable-block-families-0.1.0-canary11.jar`, 58,912 bytes, SHA-256 `C0DFB4366C59325B7D91A998F6D1A70F4D1FA6DCEB2C6B7F86F9ED165AD1CC79`, built at `2026-09-25T05:26:35.4392877Z` from source checkpoint `acc3a821`.
- Result: ACTIVE — `STATIC_PASS / RUNTIME_UNTESTED`; exact C7 remains accepted pending owner decision.
- Next state: Obtain owner-supplied runtime observations against exact C3/C11 bytes before acceptance or a successor.

## 2026-09-25T22:06:44.1468886Z — Finalize provider-first AMC masonry matrix as Canary 12

- Revision: 12
- Source checkpoint: `27eece1bcd30b6e164a50c23103e87a076b0af8e`
- Changes: Replaced C11's rectangular AMC-only masonry catalog with 17 literal C12 masonry families. The completed matrix has 646 exact cells: 409 Minecraft, BBB, Macaw Paths, or Macaw Windows provider IDs and 237 AMC C4 gap IDs. C12 removes every duplicate AMC stand-in, rehomes the affected legacy BBB/window/accessory shells so every cell has exactly one owner, preserves the unrestricted Fabric dependency `architectural_material_closure: "*"`, and retains accepted C7 unchanged.
- Build/static: Java 25 / Gradle 9.5.1 / Fabric Loom 1.17.19 passed all 57 JUnit checks, including exact C4 SHA/resource validation, provider-first 646/409/237 coverage, BBB regressions, catalog/recipe contracts, and production-JAR isolation. The assembled cleanup corpus removes 1,541 non-parent results with no parent removal or literal rewrite.
- Runtime: No Minecraft profile, deployment, server-release record, or manual runtime observation was touched. The controlled GameTest launch is blocked before tests by the pre-existing Ribbits support requirements yungsapi, trinkets_updated, geckolib, cloth-config2, customportals, and matcha-heart-death compatibility; no dependency was changed to work around it.
- Artifact: Current retained C12 is `interchangeable-block-families-0.1.0-canary12.jar`, 69,075 bytes, SHA-256 `68ed59c4d1a7542572ed82df9b2617cec9bf1c6437304ca7a5ae49ac51e01c1e`, built at `2026-09-25T22:06:44.1468886Z` from the preceding source checkpoint above. Controlled provider evidence is AMC C4 `architectural-material-closure-0.1.0-canary4.jar`, SHA-256 `b7344ec4b53305cdfeb5cfd514e2570b919ecca5432d6c9ed11b7e1846a90fd5`.
- Result: ACTIVE / STATIC_PASS / RUNTIME_UNTESTED. Exact C7 remains the accepted baseline pending owner decision.
- Next state: Under explicit owner runtime direction, test the exact C12/C4 bytes in an isolated environment and record only supplied observations; retain C7 unchanged until an owner acceptance decision.

## 2026-09-27T01:36:09.9004633Z — Finalize Interchangeable Block Families Canary 13

- Revision: 13
- Source checkpoint: `1811c86b2b2221836deb299ebd11e32427e86269`
- Changes: Excluded the exact literal 204 BGE-owned patterned full/slab/stair cells (156 Macaw, 48 AMC) from all 17 masonry profiles without inference or material-specific exceptions. C13 retains 442 eligible masonry members (253 provider, 189 AMC), 1,607 total unique members, and largest family size 26; C7 remains accepted unchanged.
- Build/static: Java 25 unit/catalog/recipe/archive checks passed. Controlled C5/C102/C13 GameTests passed all 26 tests, proving the exact existing AMC/BGE nine-role component is wholly outside IBF while a genuine cross-family component still fails closed.
- Runtime: No Minecraft profile, world, deployment, or manual runtime observation was touched. Owner-supplied C12 collision evidence remains tied to its reported prior release context.
- Artifact: Current retained C13 is `interchangeable-block-families-0.1.0-canary13.jar`, 70,830 bytes, SHA-256 `9FBE4627414CCC0A4EBA33C8912244FB55D47F6BE4B6ECAC1752F699F37974AB`, finalized at `2026-09-27T01:36:09.9004633Z` from the preceding checkpoint above.
- Result: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`.
- Next state: Obtain owner-supplied runtime observations against the exact coordinated C5/C102/C13 release set before acceptance or a successor.

## 2026-09-28T04:08:36.351893Z — Finalize IBF C14 with complete C9 catalog semantics

- Revision: 14
- Source checkpoint: `f7c642529c9aa9d2657c7d03e346706da423a351`
- Changes: Removed the entire C10-C13 AMC integration layer and its catalog, dependency, fixture, recipe, boundary, and compatibility machinery. Restored actual C9 source 464b8b334581c17277aa1512af12017c51355345, retaining every C7/C8/C9 family and canonical parent. Corrected stale manifest prose to reflect actual C9 code: 77 thin Paths plus 78 Pavings remain legitimate catalog members.
- Build/static: Java 25 test/runGameTest/build/stageCanaryArtifact passed using the exact restored non-AMC C9 inputs: 56/56 JUnit checks across 13 suites, 26/26 required controlled GameTests, production isolation, 182 families, 1,402 unique members, maximum 22. Catalog serialization SHA-256 0ff094610f07f1d93e6b95f95ac61f6011bb10423d77ce34548cf973b23ceea7 matches C9. All 30 archive entry names and 19 production classes match C9 byte-for-byte; only Fabric embedded version differs. Fresh BBB dev.7 exception checks passed and reject incompatible dev.6.
- Runtime: No Minecraft runtime result was recorded and no testing profile was inspected or modified.
- Artifact: `IBF C14.jar`, 57,104 bytes, SHA-256 `d17176029929232ea650a187936b1e4582c615c36287a1a4ee9a07c466f913bc`, finalized at `2026-09-28T04:02:47.1977917Z` from the preceding coherent implementation/artifact checkpoint above. Provider artifacts remain external and originals inputs were read-only. Required artifact retention verified the exact filename and SHA-256 in the primary checkout.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED. Manifest revision 13 to 14; exact accepted C7 and rollback C3 identities and retained bytes remain unchanged.
- Next state: Under owner-directed verification, exercise the retained C7/C8/C9 families against exact C14 bytes using TESTING.md; acceptance remains an explicit owner decision.

## 2026-09-28T15:06:34.0328687Z — Finalize IBF C15 family-catalog restructuring

- Revision: 15
- Source checkpoint: `4c02ac0bb38e1f27d5a7454c2a1d9a1a7c52c711`
- Changes: Reworked the literal audit from exact Minecraft, Macaw's Paths 1.1.1, Macaw's Windows 2.4.2, BBB dev.7, and Enderscape provider inventories. Button-parent wood families now own their exact shutters/louvered shutters, Stone Button and Stone thin Path/Paving are separate, no Button-less accessory family is promoted, thin Paths exclude parapets and patterned geometry, BBB Nether Brick appends only its exact arrow slit, and matching Stone/Blackstone Gothic windows are appended without collapsing window families.
- Build/static: Java 25 / Gradle 9.5.1 / Fabric Loom 1.17.19 passed all 54 focused unit/static/catalog/recipe checks, production archive isolation, and artifact staging. Controlled validation loaded 85 mods with the documented test-only Ribbits support set and passed all 26 required Fabric GameTests. The catalog is 181 families, 1,396 unique members, largest family size 22; its SHA-256 serialization is `4b753028b9820913d068901d8b3ba93f13bad6dab6c95aa312ba70f2b1c4b8b5`. The assembled cleanup corpus removes 1,113 non-parent results and zero canonical-parent results.
- Runtime: No manual Minecraft runtime result was recorded and no testing profile was inspected or modified.
- Artifact: `IBF C15.jar`, 57,017 bytes, SHA-256 `e3764ad9463c9b3eb210b2889bcd22fed20f6e7d49a1302c86466d979adb2c1c`, finalized at `2026-09-28T15:06:34.0328687Z` from the preceding coherent implementation/artifact checkpoint above. Provider artifacts remain external and originals inputs were read-only.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED. Manifest revision 14 to 15; exact accepted C7 and rollback C3 identities and retained bytes remain unchanged.
- Next state: Under owner-directed verification, exercise the C15 Button, Path/Paving, BBB masonry, and Gothic-window families against these exact bytes; acceptance remains an explicit owner decision.

## 2026-11-20T17:20:29.9150057Z — IBF C16 adds the AMC Stone accessories

- Revision: 16
- Source checkpoint: `459efd9c47e1c778df7d4d0807793f52c33626e3`
- Changes: Extended the Stone Button family in place, in order, with AMC Stone Parapet and Stone Louvered Shutter after the vanilla pressure plate. Both IDs were verified against AMC C9 registrations. Absent AMC, the original two-member selector resolves.
- Build/static: Compared the ordered catalog with C15. Only the Stone Button row changed, with 181 families, 1,398 unique members when AMC is present, and a maximum size of 22. Full catalog SHA-256 `099c8eaf1ec9049a487f69139fb5e4ab42bdfbd76ec8b82acca97e289cea044a`. All 54 JUnit checks and production archive isolation passed. All 27 controlled Fabric GameTests passed with AMC C9 and again without AMC, including canonical recipe retention, alternate cleanup, collision checks, and a named-stack selector round trip.
- Runtime: Manual Minecraft runtime untested.
- Artifact: `IBF C16.jar`, embedded version `0.1.0-canary16`, 57,290 bytes, SHA-256 `da10a26e33c1f8c560ef866799afbdc84185de0313b95a4aa25749fd62d606b3`, finalized at `2026-11-20T17:20:29.9150057Z`. Required retention verified the exact filename and SHA-256 in the primary checkout.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner-directed selector and reload checks in TESTING.md.

## 2026-11-20T18:24:24.6574017Z — IBF C17 integrates AMC C12 and Macaw Stone Brick Arrow Slit

- Revision: 17
- Source checkpoint: `947a2743c089d83c29f9c67b1a6dc95370e0a028`
- Changes: Added three AMC Button-parent accessories and three AMC Column-parent masonry selectors, appended the matching AMC Gothic Window to each Macaw window selector, and appended Macaw Stone Brick Arrow Slit to BBB Stone Column. All C16 keys and parents retain their relative order. The three path families and all unrelated catalog rows match C16.
- Build/static: Java 25, Gradle 9.5.1 and Loom 1.17.19 passed 54 JUnit checks and 28 controlled GameTests in each configuration. With AMC C12, IBF resolved 187 families / 1,432 items. Without AMC, 181 families / 1,397 items resolved, and all six standalone Macaw parapets and shutters kept their recipes. The cleanup corpus removed 1,152 and 1,114 alternate recipes respectively, with zero canonical removals. Selector transfers preserved stack counts and component patches. Full catalog SHA-256 `65afd8ca3b6a59d59fa039a4b971d160ca47455af94631b80a64d59fc695e6e9`. Production archive isolation and staging passed.
- Runtime: Manual Minecraft runtime verification is pending owner testing.
- Artifact: `IBF C17.jar`, embedded version `0.1.0-canary17`, 58,219 bytes, SHA-256 `415ec62d9b13578affc1b25f432bd738e82d67aa787ad384920be09d1b0e3547`, finalized at `2026-11-20T18:24:24.6574017Z`. Retention verified the filename and SHA-256 in the primary checkout.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner-directed selector, recipe and reload verification using TESTING.md.

## 2026-11-20T18:56:55.1148578Z — IBF C18 adds the three Mud Bricks selectors

- Revision: 18
- Source checkpoint: `1330559eb7de6a15066c320794f6ce72667fe5be`
- Changes: Registered the 13 AMC C14 Mud Bricks items in Button, Column, and Window families. Macaw's `mud_brick_arrow_slit` completes the Column family and `mud_brick_gothic` completes the Window family. The ordered catalog excluding those three keys reproduces the C17 digest, including the separate Mud Brick Paths and Pavings.
- Build/static: Java 25, Gradle 9.5.1 and Loom 1.17.19 passed 55 JUnit checks, 29 controlled Fabric GameTests, production archive isolation and build with AMC C14 and again without AMC. The provider-present run resolved 190 families and 1,447 items. The absent run resolved 181 families and 1,397 items, with both Macaw items independently craftable. Named stacks preserved count and component patches through all three selectors. The assembled recipe corpus removed 1,167 alternate recipes with AMC and 1,114 without, with zero canonical removals. Full catalog SHA-256 `d92138f9362a8cbb8bf7b9d220390072b7639866da1c9bf50d7010ff4abb9575`.
- Runtime: Manual Minecraft runtime verification is pending owner testing.
- Artifact: `IBF C18.jar`, embedded version `0.1.0-canary18`, 58,461 bytes, SHA-256 `3054759b8358fea4449a1cf2de91d665c7a915c103589b03844e22a1c0f02083`, finalized at `2026-11-20T18:56:55.1148578Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner-directed Mud Bricks selector, recipe and reload checks in TESTING.md.

## 2026-11-20T19:14:27.0959986Z — IBF C19 integrates the AMC C15 Prismarine families

- Revision: 19
- Source checkpoint: `09733780ce98035fdbf23942be2d3b6af1d8455b`
- Changes: Added Button and Column families for Prismarine Bricks and Dark Prismarine, and the eleven-member Prismarine Bricks Running Bond Path family. Exactly 27 AMC items join four Macaw parapets and arrow slits. Excluding the five new keys reproduces the complete ordered C18 catalog.
- Build/static: Java 25, Gradle 9.5.1 and Loom 1.17.19 passed 55 JUnit checks and 30 controlled GameTests in each configuration. AMC C15 resolved 195 families and 1,478 items. Without AMC, 181 families and 1,397 items resolved and the four Macaw items kept their recipes. Transfers preserved stack counts and components. The recipe corpus removed 1,199 alternate recipes with AMC and 1,114 without, with zero canonical removals. The four excluded patterned full blocks placed independently and retained their stonecutting recipes. Production isolation and build passed. Catalog SHA-256 `b1c74a1432ecf8ac0c229ad1a964f0c67d7dc4eae80fb04eed2d6094cacd2c05`.
- Runtime: Manual Minecraft runtime verification is pending owner testing.
- Artifact: `IBF C19.jar`, embedded version `0.1.0-canary19`, 58,915 bytes, SHA-256 `875708c20ce13b77a87adbeb5ecac8c963a8691015c3ed16b4bcd48eceecb1d5`, finalized at `2026-11-20T19:14:27.0959986Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner-directed Prismarine selector, stonecutting and reload checks in TESTING.md.

## 2026-11-20T19:25:24.4245247Z — IBF C20 integrates AMC C16 Sandstone and Red Sandstone

- Revision: 20
- Source checkpoint: `c616755f6760467931149f56c123958a57d390a2`
- Changes: Added two four-member AMC Button selectors and appended one AMC Gothic Window and one AMC Arrow Slit per material to the existing Macaw Window and BBB Column selectors. All twelve AMC items have unique membership. Both Macaw Path definitions match the C19 source bytes. Removing only the C20 additions reproduces the ordered C19 catalog digest.
- Build/static: Java 25, Gradle 9.5.1 and Loom 1.17.19 passed 55 JUnit tests and 31 controlled Fabric GameTests in each configuration. Exact AMC C16 resolved 197 families and 1,490 items. Without AMC, 181 families and 1,397 items resolved, with the original Window and Column selectors functional. Named seven-item stacks retained their components through selector round trips. The audited recipe corpus removed 1,209 alternate recipes with AMC and 1,114 without, with zero canonical removals. Production JAR isolation and build passed. Catalog SHA-256 `288dc62fce86619038830c050bd5d6530aff4c3a2525bafe93ca16215d9bf8f9`.
- Runtime: Manual Minecraft runtime verification is pending owner testing.
- Artifact: `IBF C20.jar`, embedded version `0.1.0-canary20`, 59,076 bytes, SHA-256 `90a423755d4a0bb31ca636ffa04c2bacf76a627362003fc1134a4e5262203bb3`, finalized at `2026-11-20T19:25:24.4245247Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner-directed Sandstone selector, recipe and reload checks in TESTING.md.

## 2026-11-20T21:50:09.2970545Z — IBF C21 integrates AMC C19 Deepslate, Quartz and Nether Bricks

- Revision: 21
- Source checkpoint: `c3a884b2ace9c998cf4f7747c91407bebd4b886d`
- Changes: Added three Button families, two eleven-member Paths/Pavings families, and one Nether Bricks Window family. Extended the existing Deepslate and Quartz Macaw Window and BBB Column families. All 42 intended AMC items and the Macaw Nether Brick Gothic Window resolve once. The eight patterned full blocks remain independent. Removing the C21 additions reproduces the entire ordered C20 catalog, including both Deepslate path families and the Nether Brick BBB family.
- Build/static: Exact AMC C19 and AMC-absent runs each passed 55 JUnit tests, 32 controlled Fabric GameTests, production archive isolation and build. With AMC, 203 families and 1,533 items resolved. Without AMC, 181 families and 1,397 items resolved and the unparented Macaw Gothic Window kept its recipe. Selector round trips preserved stack counts and component patches. The eight excluded full blocks placed independently and retained their recipes. The audited corpus removed 1,246 alternate recipes with AMC and 1,114 without, with zero canonical removals. Ordered catalog SHA-256 `9700ae6b2d7dbde923a0d3c5c28422c450bab5eb73686e23da83695a3cbde159`.
- Runtime: Owner-directed manual Minecraft runtime verification and acceptance are pending.
- Artifact: `IBF C21.jar`, embedded version `0.1.0-canary21`, 59,677 bytes, SHA-256 `f4b2fe43299533e0ae5db1f3aa8693c092eedd1edc2f2c6ce5bcac1e75797442`, finalized at `2026-11-20T21:50:09.2970545Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner-directed selector, independent patterned block and reload checks in TESTING.md.

## 2026-11-20T22:21:07.8387300Z — IBF C22 integrates End Stone Bricks and the Polished Blackstone shutter

- Revision: 22
- Source checkpoint: `72135957b1f155e1dea6ba3b8aeeb06bb89342c9`
- Changes: Added three AMC-parented End Stone Bricks families (Column, Window and Running Bond Path). Appended the AMC parapet and shutter to Enderscape's Polished End Stone Button family, and the AMC shutter to the existing Polished Blackstone family. Exactly 23 AMC items have unique membership. The four patterned full blocks remain independent. Removing only the C22 additions reproduces the complete ordered C21 digest.
- Build/static: Exact AMC C21 and AMC-absent runs each passed 55 JUnit tests, 33 controlled Fabric GameTests, archive isolation and build. Both used the standard provider set and full Matcha fixture, with Extended Block Shapes absent. With AMC, 206 families and 1,558 items resolved. Without AMC, 181 families and 1,397 items resolved, including both original Button selectors, and both Macaw items kept their standalone recipes. Named seven-item stacks retained count and components through selector round trips. All four full blocks retained independent recipes and placed successfully. The audited corpus removed 1,271 alternate recipes with AMC and 1,114 without, with zero canonical removals. Catalog SHA-256 `01b6dd6ff7b4466963ddd5b6962a4eb3ed0a3bb25153b5229378eefe5920231e`.
- Runtime: Owner-directed manual Minecraft verification and acceptance are pending.
- Artifact: `IBF C22.jar`, embedded version `0.1.0-canary22`, 60,019 bytes, SHA-256 `98aaf408316570067b02ba43810c69e5f02a478e5c36df64b0f1d053ca25aaf1`, finalized at `2026-11-20T22:21:07.8387300Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner-directed End Stone Bricks and Polished Blackstone checks in TESTING.md.

## 2026-11-20T22:39:01.2320214Z — IBF C23 integrates AMC Mossy Stone Bricks

- Revision: 23
- Source checkpoint: `cee53653a5dc02c163b37a1eff1cc8fabf9e746e`
- Changes: Added three selectors with the AMC Mossy Stone Bricks Button, Column and Window as canonical parents. The fifteen items each have one membership. Removing just the three keys reproduces the complete ordered C22 catalog digest, including the separate eleven-member Macaw Mossy Stone Paths/Pavings family.
- Build/static: Exact AMC C22 and AMC-absent runs each passed 55 JUnit tests, 34 controlled Fabric GameTests, archive isolation and build. With AMC, 209 families and 1,573 items resolved. Without AMC, 181 families and 1,397 items resolved. Named stacks of seven kept their counts and component patches through forward and reverse transfers. The Macaw path selector operated in both runs. The audited recipe corpus removed 1,286 alternate recipes with AMC and 1,114 without, with zero canonical removals. The three new parent crafting recipes and Column stonecutting recipe survived cleanup. Catalog SHA-256 `71e85a472c26c030e24439fedce4ecbdbe538667fe126f91c9d1b86f89b0198b`.
- Runtime: Owner-directed manual Minecraft verification and acceptance are pending.
- Artifact: `IBF C23.jar`, embedded version `0.1.0-canary23`, 60,261 bytes, SHA-256 `6879a263b8f33b9dd0a8a689e615b6c75c87844dce5a4393e5f8a18622d68a4a`, finalized at `2026-11-20T22:39:01.2320214Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner-directed Mossy Stone Bricks selector, recipe and reload checks in TESTING.md.

## 2026-11-21T01:48:44.0304125Z — IBF C24 integrates four AMC base materials

- Revision: 24
- Source checkpoint: `88b851aa264971db24346ffd311379fd095da3fe`
- Changes: Added four literal selectors each for Tuff, Cinnabar, Sulfur and Smooth Basalt. The AMC Button, Column, Window and Running Bond Path are the parents. All 104 members resolved once. Removing the sixteen additions from the compiled C24 catalog reproduced the finalized C23 catalog SHA-256 `71e85a472c26c030e24439fedce4ecbdbe538667fe126f91c9d1b86f89b0198b`. The ordered C24 catalog SHA-256 is `bc0038de836e6208b7ac9fa863cbbe53349ee464ddd98be9f2acd9936c13aad4`.
- Build/static: Exact AMC C23 and AMC-absent builds each passed 55 JUnit tests, 35 controlled Fabric GameTests and production archive isolation. With AMC, 225 families and 1,677 items resolved. Named stacks of seven preserved their count and component patches in both directions. All sixteen independent patterned full blocks retained their recipes and Building Blocks tab entries and placed with their own IDs. Without AMC, 181 families and 1,397 items resolved. The assembled recipe audits recorded 1,386 alternate removals with AMC and 1,114 without it, and zero canonical removals in both runs.
- Runtime: Owner-directed Minecraft verification is pending.
- Artifact: `IBF C24.jar`, embedded version `0.1.0-canary24`, 61,757 bytes, SHA-256 `e3940a3244847793e50b2a2c14209fa62320f46fffcfdf6d2332335e80652a96`, finalized at `2026-11-21T01:48:44.0304125Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner verification of the four materials in TESTING.md and an explicit acceptance decision.

## 2026-11-21T02:15:04.5045397Z — IBF C25 integrates three Enderscape-backed AMC materials

- Revision: 25
- Source checkpoint: `a290ae301866d304cc6e53f4c63101be99482c5e`
- Changes: Added Veradite, Mirestone and Kurodite Column, Window and Running Bond Path selectors. Appended each AMC Parapet and Louvered Shutter after its native polished Enderscape Button and Pressure Plate. All 72 new members resolved once. Removing nine families and the six appended members from the compiled C25 catalog reproduced the exact C24 digest `bc0038de836e6208b7ac9fa863cbbe53349ee464ddd98be9f2acd9936c13aad4`. The C25 catalog SHA-256 is `4f2e6c7c9bdbf32d7539763e3b49402c8648955b1291314e992a31cf7a6a30fe`.
- Build/static: With Enderscape 3.0.2, the exact AMC C24 and AMC-absent runs each passed 55 JUnit tests, 36 controlled Fabric GameTests, archive isolation and build. With AMC, 234 families and 1,749 items resolved. Named stacks of seven preserved their counts and component patches in forward and reverse transfers. All twelve independent patterned full blocks retained recipes, creative entries and placement. Without AMC, 181 families and 1,397 items resolved, including the original three control pairs. The assembled audit included all 96 recipes in AMC's Enderscape pack and recorded 1,458 alternate removals with AMC and 1,114 without it. Both runs retained all canonical recipes. A separate controlled launch with Enderscape disabled stopped in Fabric dependency resolution before IBF initialized.
- Runtime: Owner-directed Minecraft verification and acceptance are pending.
- Artifact: `IBF C25.jar`, embedded version `0.1.0-canary25`, 62,805 bytes, SHA-256 `8a90fe8d0ee2d0cc40f6bf7c62968adefe53caba0475d89fced5d92bf11c6066`, finalized at `2026-11-21T02:15:04.5045397Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner checks of polished controls, transfers, recipes and independent full blocks in TESTING.md.

## 2026-11-21T02:59:08.0890460Z — IBF C26 integrates AMC Purpur, Dusk Purpur and Resin

- Revision: 26
- Source checkpoint: `7cc0e44064e1109d6417726e120247b493549cd8`
- Changes: Added four selectors per material with the AMC Button, Column, Window and Running Bond Path as canonical parents. All 78 members resolved exactly once. Removing only the twelve new keys from the compiled C26 catalog reproduced the C25 catalog digest `4f2e6c7c9bdbf32d7539763e3b49402c8648955b1291314e992a31cf7a6a30fe`. The C26 catalog SHA-256 is `755bd84010e39e36a61eaa10ab715c953607396834b054f2285a5faa82a02539`.
- Build/static: With Enderscape 3.0.2, both exact AMC C26 and AMC-absent builds passed 55 JUnit tests, 37 controlled Fabric GameTests and archive isolation. With AMC, 246 families and 1,827 items resolved. Named stacks of seven preserved count and component patches in both directions. All twelve independent patterned full blocks retained recipes, creative entries and placement. The six provider slabs and stairs remained outside IBF and the vanilla Resin Clump recipe survived. Without AMC, 181 families and 1,397 items resolved. The recipe audit removed 1,533 alternate recipes with AMC and 1,114 without it, with zero canonical removals. The 102 new provider recipes account for 75 new alternate removals. With AMC present and Enderscape disabled, Fabric rejected the missing required provider before IBF initialized.
- Runtime: Owner gameplay verification and acceptance are pending.
- Artifact: `IBF C26.jar`, embedded version `0.1.0-canary26`, 63,979 bytes, SHA-256 `e5d919c43278f4329bef54dd3360fe6e990e262398c49c70e898bb607f062bb0`, finalized at `2026-11-21T02:59:08.0890460Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner checks of selector transfers, independent patterned blocks and recipes in TESTING.md.

## 2026-11-21T18:51:54.5313930Z — IBF C27 integrates Calcite and Shadoline Block

- Revision: 27
- Source checkpoint: `7e0dac1aae79d1f0e9f063f4eaf3252524c73e3f`
- Changes: Added four AMC-parented selectors per material with 52 unique members. Enderscape is optional with its existing minimum supported version. Families supplied by Enderscape or its AMC and BBB packs resolve only when the provider is present. Removing the eight new keys from the compiled C27 catalog reproduced the complete ordered C26 digest `755bd84010e39e36a61eaa10ab715c953607396834b054f2285a5faa82a02539`. C27 catalog SHA-256 `20056131e05bcbdde38cac00d13ed49023d626619ddef26fb8831fee8db872b6`.
- Build/static: The 55 JUnit checks and production archive validation passed. With Enderscape 3.0.2, the exact AMC C28 and AMC-absent builds each passed 38 controlled GameTests, resolving 254 families / 1,879 items and 181 / 1,397 respectively. The assembled recipe audit removed 1,583 alternates with AMC and 1,114 without it, with zero canonical removals. The 68 new provider recipes comprise 50 suppressed alternates, 10 retained parent recipes and eight independent full-block recipes. With Enderscape omitted, the dedicated registry and ShapeMap GameTest passed with AMC (220 / 1,704) and without AMC (164 / 1,348). It checked the complete active graph, forward and reverse transfers of named stacks of seven, independent pattern recipes, creative entries and placement for each available material, and the orphaned End Stone Bricks accessories' independent recipes with AMC alone.
- Runtime: Owner gameplay verification and acceptance are pending.
- Artifact: `IBF C27.jar`, embedded version `0.1.0-canary27`, 65,137 bytes, SHA-256 `5336cc6faf594fcf7262d19ea75bac10440223bc179008f9e9631d255eeb0d3a`, finalized at `2026-11-21T18:51:54.5313930Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner checks of selector transfers, independent patterns and optional-provider behavior in TESTING.md.

## 2026-11-22T01:51:03Z — IBF C28 Mossy Oak

- Revision: 28
- Source checkpoint: `89523164812b77f71a02ff07e2e286e9b6d55eff`
- Changes: Preserved the Ribbits Fence parent and Fence Gate first alternate, then appended AMC Frame and Lattice. Added AMC Trim, Balustrade, Support and Pallet in order to the wood BBB detail category. Removing these catalog changes reproduces the complete C27 digest. The C28 catalog digest is 6926571dc498310dd9c5e88f3bcfd854ff6c7d13486970f61529c476d7dcc60e.
- Build/static: Java 25 build, 55 JUnit checks and production archive isolation passed. With Enderscape, 38/38 Fabric GameTests passed both with and without AMC. The focused Enderscape-absent GameTest passed both ways. The assembled recipe corpus contained 2,189 recipes with AMC and 1,572 without it. CNM suppressed 1,588 and 1,114 alternate recipes respectively, with zero canonical recipe removals. Checked ordered selectors, unique membership and round trips preserving a named stack of seven.
- Runtime: Owner gameplay verification and acceptance pending.
- Artifact: `IBF C28.jar`, 65,515 bytes, SHA-256 `de912d4753f69cbb6cf53e371659bfa91b57392ae48eec868da367d6b735f3c5`, built_at `2026-11-22T01:45:17.1216990Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Exercise both Mossy Oak selectors in-world and verify their recipes and named stack transfers.

## 2026-11-23T00:18:33Z — IBF C29 integrates the AMC Macaw wood doors

- Revision: 29
- Source checkpoint: `b7729409df7456e48989c0081d9c17d1551d051d`
- Changes: Veiled, Celestial, Murublight and Mossy Oak each retain their native door as parent and gain 22 AMC C30 designs. The four two-high selectors account for 92 distinct members. Removing them reproduces the compiled C28 catalog (SHA-256 `6926571dc498310dd9c5e88f3bcfd854ff6c7d13486970f61529c476d7dcc60e`).
- Build/static: The 55 JUnit checks, production build and archive isolation passed. With Enderscape 3.0.2, AMC C30 and AMC-absent runs each passed 38 server GameTests (259 families / 1,977 items and 181 / 1,397). Without Enderscape, both focused GameTests passed (222 / 1,733 with AMC, 164 / 1,348 without). Native doors without AMC retain their recipes and have no selector. Mossy Oak retains all 23 members with AMC alone. All 88 door alternatives preserved names, counts and component patches on forward and reverse transfers. All 88 alternate recipes were suppressed, while four native crafting recipes and the Murublight conversion survived. The assembled cleanup removed 1,676 alternates with AMC and 1,114 without, with zero canonical removals or surviving literal ingredient rewrites. The isolated graphical client exercised all 23 positions of all four selectors through the held-item overlay and 12-icon inventory tooltip at 320 scaled pixels.
- Runtime: Owner gameplay verification and acceptance are pending.
- Artifact: `IBF C29.jar` (embedded `0.1.0-canary29`, 67,091 bytes), SHA-256 `b5ef2765538d49ce46b8d51b97873026e03143350819ebbe82c8cf44f1ab4f97`, built at `2026-11-23T00:09:12.2693718Z`.
- Result: ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED.
- Next state: Owner checks of GUI scale, placement, drops and reload behavior in TESTING.md.
