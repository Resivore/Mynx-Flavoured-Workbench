# Testing

## Current gate

**CANARY 1 RETAINED — READY FOR CONTROLLED DEPLOYMENT; NOT DEPLOYED / RUNTIME UNTESTED**

Use exact `matcha-frost-protection-0.1.0-canary1.jar`, SHA-256
`ba7384dc0a325338b9a5922d868de7c4e9242947f3d8372c6cf9684328d92bf6`.
The clean Java 25 / Gradle 9.5.1 build passed all 13 focused tests against
Minecraft 26.2 and exact Matcha Flavoured 1.12. This is static evidence only.
The candidate has not been deployed, launched, or gameplay-tested.

## Runtime acceptance procedure

1. Under explicit Test Slot ownership, deploy the exact retained JAR through
   the Test Instance Manager and complete its normal readiness verification.
   Stop if the deployed hash differs or the other slot cannot be preserved.
2. Launch the exact Minecraft 26.2 Matcha stack, run `/reload`, and confirm no
   relevant registry, recipe, Mixin, or resource-reload error appears.
3. Establish the local baselines: ordinary armor with no Frost Protection must
   sink into powdered snow, while leather boots must provide the normal solid
   surface when walking without descending.
4. Prepare one armor item for each of helmet, chestplate, leggings, and boots
   with `main:freezing_protection` directly present at levels I, II, and III.
   Matcha intentionally supports normal acquisition only on chest armor, so
   provision off-slot test items through direct item-component commands or an
   equivalent controlled method; do not alter Matcha's supported-items tag.
5. Test all twelve single-piece slot/level combinations. Each must provide the
   same solid powdered-snow walking surface as leather boots. Deliberately
   descending must still work like leather boots; this Canary does not add a
   broader `entityInside` or freezing bypass.
6. For every slot, remove the only qualifying piece and walk onto adjacent
   fresh powdered snow. Ordinary traversal must return immediately. Reconfirm
   the all-zero case with no Frost Protection equipped.
7. Equip ordinary Frost Walker without Frost Protection. Powdered-snow
   traversal must remain ordinary, and Frost Walker's normal water behavior
   must remain unchanged.
8. Recheck Matcha's existing Frost Protection behavior: freezing damage
   protection remains level-scaled, and the separate chest-slot Frost
   Protection III frozen-water exemption remains unchanged.
9. Craft `blessings:frost_walker_frost_protection` with the existing shaped
   pattern and inputs. The method, ingredients, arrangement, item name, lore,
   model, count, and enchanted-book identity must remain unchanged.
10. Inspect the complete crafted enchantment set. It must be exactly Frost
    Protection III: `main:freezing_protection` level 3, with Frost Walker absent,
    Frost Protection II absent, and no additional enchantment.
11. Save and reload, then repeat representative traversal, removal, ordinary
    Frost Walker, and crafted-book checks. Confirm unrelated Matcha recipes and
    content still load normally.
12. If server-authoritative validation is required, repeat representative
    positive and negative cases on the actual server host in a disposable
    playtest world with two players.

Pass only after every applicable case is observed against this exact artifact
and stack. Stop and record the exact setup as failed or inconclusive if slot or
level behavior differs, ordinary behavior is not restored, deliberate descent
is blocked, the recipe contract/result is wrong, unrelated behavior regresses,
or client and server observations disagree. Do not infer runtime results from
the static suite, build, or retained artifact.
