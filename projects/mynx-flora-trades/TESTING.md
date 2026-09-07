# Testing

Candidate `mynx-flora-trades-0.1.0+26.2-canary4.jar` is ACTIVE, not deployed, and runtime untested. Canary 3 has one user-reported observation only: a Florist claimed its Flower Pot POI and became functional. It is not an aggregate C3 PASS. Use only the dedicated Matcha Flavoured 26.2 Workbench after an atomic Test Slot assignment.

1. Verify a Florist naturally claims an ordinary Flower Pot.
2. Verify the first offer is 1 Glowcap for 4 Flower Pots.
3. Verify the second offer is 1 Glowcap for 1 Decorated Pot.
4. Verify all eight flower offers follow those two sales, and retain their existing quantities and Obol (`minecraft:emerald`) currency.
5. Verify Florist remains one-level with all offers immediately available and zero merchant XP.
6. Verify existing Farmer Flora offers and their merchant XP remain unchanged.
7. Verify existing Wandering Flora offers remain unchanged.
8. Verify Naturalist and Matcha Wandering behavior remain unchanged.
9. Repeat the existing potted-flower, save/reload, release/reacquisition, and single-capacity Florist checks before assigning any aggregate runtime result.

Stop and record FAIL or INCONCLUSIVE on any divergence; do not change a Test Slot, deployment, or protected gameplay profile outside the Test Instance Manager workflow.
