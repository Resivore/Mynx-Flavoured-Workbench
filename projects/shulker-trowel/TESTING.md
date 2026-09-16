# Shulker Trowel C11 accepted runtime evidence and regression procedure

**ACCEPTED — CONTROLLED_VALIDATION_PASS — NOT_DEPLOYED — USER-REPORTED EXTERNAL RUNTIME_PASS**

Exact accepted private C11: `shulker-trowel-0.1.0-canary10-private.jar`, 50,375 bytes, SHA-256 `e286d0ab90fcf4aa5d59b5250ef6a6bf8bf52266143d85c749915d53ad11f57a`, embedded version `0.1.0-canary10`, source `adfa2dee4426fed91d4ad224136cd16b318ce2ec`. The user explicitly reported an aggregate Minecraft runtime **PASS** and accepted this exact identity. No row-level observations were supplied, so none are inferred. This evidence is external/user-reported: no managed deployment, Test Slot operation, profile access, or Minecraft launch by Codex is claimed; deployment remains `NOT_DEPLOYED`. Exact C10 / Private Canary 9 is now the rollback/previous-known-good release. Do not access the protected Matcha Flavoured 26.1.2 profile.

## A. Offhand shulker placement

1. Put an empty vanilla shulker in OFF_HAND and right-click a normal placement surface. Nothing places.
2. Crouch and repeat. Nothing places.
3. Repeat with a filled shulker and a dyed shulker. Confirm the stack count, contents, custom name, and components are unchanged; there is no placement sound, particle, block entity, advancement, or placement statistic.

## B. Target interactions

1. With an offhand shulker, right-click a chest or barrel, then crouch-right-click it.
2. Test a replaceable target and another ordinary placement-valid surface.
3. Confirm the offhand shulker never places. Record ordinary target-interaction behavior exactly as observed; this candidate does not claim to change it.

## C. Main-hand control

1. Move the same filled/named shulker to MAIN_HAND and place it normally, including while crouching.
2. Confirm its contents and custom data survive placement and normal breaking/pickup remains vanilla.

## D. Quick Right-Click 1.9

1. Install exact `quickrightclick-26.2.0-1.9.jar` (mod ID `quickrightclick`, version `1.9`, SHA-256 `87e77365919532bd38a004235c58e1220bbcbae680d8a3d27ed2ce2ddbd7831b`).
2. Hold a shulker in MAIN_HAND and OFF_HAND in turn, then trigger its former quick-right-click path. Confirm no Quick Right-Click shulker UI/temporary world behavior occurs.
3. Verify Quick Right-Click still works for at least a bed and one non-shulker table/chest feature.
4. Repeat the normal C11 offhand-placement checks. The optional compatibility seam must be inert when Quick Right-Click is absent.

## E. CSR and Shulker Trowel

1. Confirm CSR's carried-shulker tooltip/interface is unchanged.
2. Hold the Trowel in MAIN_HAND and a material palette shulker in OFF_HAND. Place several materials and geometries, including a crouched build where applicable.
3. Confirm material selection and Survival consumption still use the actual offhand `DataComponents.CONTAINER`, while that palette shulker itself never places.

Retain this procedure for a future regression test of this exact accepted identity or an explicitly identified successor. Automated checks and a successful launch are not runtime PASS. Record only observed Minecraft behavior against the tested exact identity.
