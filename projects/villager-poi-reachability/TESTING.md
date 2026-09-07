# Testing

Candidate `villager-poi-reachability-0.1.0+26.2-canary1.jar` is ACTIVE, not deployed, and runtime untested. Use only the dedicated Matcha Flavoured 26.2 Workbench after an atomic Test Slot assignment.

1. Put an empty Flower Pot on a solid adjacent block, one block above an unemployed villager's feet; verify normal Florist claim and the true Flower Pot position.
2. Repeat with a Brewing Stand and verify normal Cleric acquisition.
3. Repeat with a Composter and verify normal Farmer acquisition.
4. Verify the normal-height versions continue to acquire normally.
5. Verify a workstation two blocks above a plain floor is not magically claimable.
6. Verify a raised workstation sealed behind actual obstruction is inaccessible.
7. Verify normal one-villager-per-POI occupancy remains intact.
8. Save/reload and confirm job-site memory retains the true workstation position.
9. Break the raised workstation and confirm normal POI release/invalidation.

Stop and record FAIL or INCONCLUSIVE on a changed normal-height case, a claim through blocked geometry, a fake/downshifted position, a duplicate claim, or any runtime crash. Automated tests are controlled/static evidence, not gameplay runtime evidence.
