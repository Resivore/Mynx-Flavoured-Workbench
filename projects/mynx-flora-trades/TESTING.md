# Testing

Candidate `mynx-flora-trades-0.1.0+26.2-canary3.jar` is ACTIVE, not deployed, and runtime untested. Use only the dedicated Matcha Flavoured 26.2 Workbench after an atomic Test Slot assignment.

1. With a fresh adult unemployed villager and an empty flower pot, verify natural claim, normal pathing, and conversion to `mynx_flora_trades:florist`.
2. Repeat with a vanilla potted flower and a representative Mynx Regions Unexplored potted flower.
3. Save and reload, then break and replace the claimed pot; verify normal job-site release and reacquisition.
4. Verify a second unemployed villager cannot claim the occupied one-capacity pot.
5. Verify all eight Florist offers are immediate, simultaneous, zero-XP, one-level offers of 1 Obol (`minecraft:emerald`) for the selected MRU flowers.
6. Verify Farmer level 1 retains one persistent choice—64 Clover or 32 Stone Bud—for exactly 1 merchant XP, visibly ranks normally, and persists through reload.
7. Verify Farmer level 2 retains one persistent choice—32 Barley or 32 Windswept Grass—for exactly 2 merchant XP and persists through reload.
8. Smoke native gardener behavior; it must remain unchanged.
9. Verify Wandering Group 1 is 1 Glowcap for 16 Glowleaf (`mynx_regions_unexplored:dropleaf`) or 16 Mycotoxic Daisy, and Group 2 is 1 Glowcap for 16 Cattail or 32 Duckweed; provider offers grant no merchant XP.
10. Verify Naturalist and Matcha behavior remain unchanged.

Stop and record FAIL or INCONCLUSIVE on any divergence; do not change a Test Slot, deployment, or protected gameplay profile outside the Test Instance Manager workflow.
