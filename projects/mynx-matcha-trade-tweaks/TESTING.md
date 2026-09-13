# Testing

Canary 1 is built and statically checked, but no Minecraft runtime test or
Test Instance Manager deployment has occurred. The current candidate is
`mynx-matcha-trade-tweaks-0.1.0+26.2-canary1.jar`; its exact SHA-256 and source
checkpoint are recorded in `WORKBENCH_STATUS.json` after the build checkpoint.

Use Minecraft Java 26.2 with the accepted Matcha Flavoured 1.12 data pack and
its resource pack, Fabric Loader 0.19.3 or newer, Fabric API 0.157.0 or newer,
and the current Simple Copper Pipes and Copper Hopper providers. Matcha's
resource pack presents `minecraft:beetroot_seeds` as Tomato Seeds, its custom
stone-sword Cleaver, and the component-distinguished goat-horn Clay Fetishes.
The 26.2 local static provider baseline was SimpleCopperPipes 2.1.7 and
Copper Hopper 0.24.0; these are validation inputs, not runtime version pins.

1. In the dedicated **Matcha Flavoured 26.2 Workbench** instance, verify the
   exact Canary filename/SHA-256 and the enabled Matcha and provider identities.
   Stop on any mismatch or pack-loading, Mixin, registry, tag, or trade-set error.
2. Verify `mynx_matcha_trade_tweaks:mynx_matcha_trade_tweaks` is enabled above
   Matcha's built-in data pack. Open fresh villagers at the relevant levels;
   advance them as needed so their offers are generated from the current pack.
3. Check representative ordinary replacements: Armorer pipe/fitting/copper
   hopper with the correct provider items; Farmer direct seed stacks including
   Tomato Seeds; Mason direct stone/glass/dripstone/prismarine stacks, with no
   old bulk spawn-egg or bundle behavior.
4. Check that Armorer Novice has no Composter offer and Butcher levels 1–3 have
   no recipe offers. Confirm their remaining offers load, and that Leatherworker
   Master offers both 16 brown and 16 red mushroom blocks for one emerald each.
5. Check the named-book Librarian inputs, both distinct Clay Fetish inputs, the
   Cleaver's Slaughter/Looting and other components, fish species-specific
   buybacks, log fungus/sapling inputs, and unchanged black-wool fillers.
6. Buy each Farmer egg/calf/piglet and Shepherd lamb offer once. Confirm it
   becomes unavailable, then restock the villager normally and confirm that
   same offer is available again. Confirm no other offer's max uses changed.
7. Record only observed results per trade or a clearly scoped aggregate result,
   including any offer not selected by a randomized pool. A missing expected
   offer, wrong item/count/component, extra removed offer, failed restock, or
   resource-loading error is a FAIL; inability to observe an offer is
   INCONCLUSIVE.

Never use the protected Matcha Flavoured 26.1.2 gameplay instance for this test.
