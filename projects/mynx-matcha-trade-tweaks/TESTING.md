# Testing

Run `python -B projects/mynx-matcha-trade-tweaks/tools/validate_trades.py` from the repository root. Run the Java tests with Java 25 and Gradle 9.5.1. The external Simple Copper Pipes dependency snapshot is optional for reproducing its two item checks.

Compare trades on a fresh villager and newly generated tiers against [the effective matrix](tools/trade_reconciliation.tsv). Verify quantities, both payments, semantic identity, displayed result and restock stock. Exercise the preserved Fisherman tiers, both Leatherworker mushroom blocks, Brush, all three Butcher foods, the Ofuda costs, book exchanges and all equipment families. Visit removed filler tiers and ensure no old or vanilla namespace pool repopulates them.

Load a saved foodChecked villager with one of the publisher's buybacks, a partially used legitimate static trade and recorded XP. Repeat with the excluded foods, a villager with the rejected Heart exchange, an unmarked lookalike and a villager that has never traded. Check results, levels, XP and uses after entity load, restock, chunk reload, world reload and restart. The baked data supplies no new favorite-food offers. The saved-state cleanup runs as each villager loads.

Follow [the coordinated runtime checks](../matcha-flavoured-data/TESTING.md) for pack priority, saved-world and client cases.
