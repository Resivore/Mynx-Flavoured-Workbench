# Testing

## Current gate

**PLANNED — NO RUNTIME CANDIDATE**

Implementation has not started. No source, build, artifact, Canary, deployment,
or Minecraft runtime evidence exists for this project. Every case below is a
future runtime check and is not passed.

## Future runtime acceptance procedure

1. Establish the exact current Matcha Flavoured 26.2 Frost Protection and
   Blessing of Demeter baselines, including Frost Protection behavior outside
   powdered-snow traversal, the recipe's ingredients and crafting method, and
   the existing Frost Walker II and Frost Protection II result enchantments.
2. Establish ordinary powdered-snow traversal with no Frost Protection
   equipped, plus the corresponding leather-boots traversal baseline.
3. Equip Frost Protection I on only the helmet and verify that it is sufficient
   to prevent the targeted powdered-snow traversal behavior as though the
   player were wearing leather boots.
4. Repeat the same Frost Protection I check with only the chestplate, only the
   leggings, and only the boots enchanted; every individual armor slot must be
   sufficient.
5. Repeat representative individual-slot checks with Frost Protection II and
   Frost Protection III and verify behavior remains consistent with level I.
6. Remove the only Frost Protection-enchanted armor piece while testing and
   verify that normal powdered-snow traversal behavior is restored immediately.
7. Reconfirm that a player with no equipped Frost Protection retains normal
   powdered-snow behavior and that established Frost Protection behavior
   outside this project's targeted traversal change remains unchanged.
8. Craft Blessing of Demeter through its existing recipe and verify that the
   ingredients, arrangement or input contract, and crafting method are
   unchanged and still produce the book successfully.
9. Inspect the resulting book and verify that its complete enchantment list
   contains exactly Frost Protection III.
10. Explicitly verify that the existing Frost Walker II and Frost Protection II
    result enchantments are absent and that no unintended enchantments remain.
11. Verify that ordinary Frost Walker behavior and unrelated Matcha Flavoured
    enchantments, recipes, items, and mechanics remain unchanged.
12. Save and reload the world, then repeat representative armor-slot and crafted
    book checks to verify that the enchantments and behavior persist correctly.
13. If the eventual implementation has server-authoritative or multiplayer
    behavior, repeat representative positive and negative checks on the actual
    server host in a disposable playtest world with two players as appropriate.

Pass only after every applicable case has been observed against the exact
candidate and stack. Stop and record the exact setup as failed or inconclusive
if armor-slot coverage differs, normal behavior is not restored, the crafted
book or recipe contract is wrong, unrelated enchantments regress, or client and
server observations disagree. Do not infer runtime results from static checks,
builds, generated assets, or artifacts.
