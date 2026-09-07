# Testing

Candidate `mynx-flora-trades-0.1.0+26.2-canary2.jar` is ACTIVE, not deployed, and runtime untested. Use only the dedicated Matcha Flavoured 26.2 Workbench after an atomic Test Slot assignment.

1. Verify Farmer level 1 and level 2 each retain one persistent choice and visually show 1 Glowcap on the player-input side: 64 Clover or 32 Stone Bud at level 1, then 32 Barley or 32 Windswept Grass at level 2.
2. Save and reload; confirm both selected Farmer flora options persist rather than reroll.
3. Verify Wandering Group 1 shows 1 Glowcap for 16 Glowleaf (`mynx_regions_unexplored:dropleaf`) or 16 Mycotoxic Daisy, and Group 2 shows 1 Glowcap for 16 Cattail or 32 Duckweed.
4. Save and reload; confirm one selection from each Wandering flora group persists. Exercise ordinary Wandering restocking and confirm only the Flora provider follows its existing ORDINARY policy.
5. Place an empty flower pot near an unemployed villager and confirm it claims the pot and becomes `mynx_flora_trades:florist`.
6. Repeat with a representative vanilla potted flower, then a representative Mynx Regions Unexplored potted flower; confirm normal navigation and work-site memory in each case.
7. Confirm all eight Florist offers are immediate, simultaneous, zero-XP, one-level offers of 1 Obol (`minecraft:emerald`) for 8 selected MRU flowers, without progression.
8. Break the claimed pot and confirm normal villager job-site/profession behavior. Smoke native Ribbits trades to ensure they are unchanged.
