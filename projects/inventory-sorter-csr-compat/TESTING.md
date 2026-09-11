# Inventory Sorter CSR Compatibility — Canary 1

Use only the dedicated Matcha Flavoured 26.2 Workbench through a serialized Test Instance Manager transition. Do not access the protected 26.1.2 profile.

Open a chest supported by Inventory Sorter and run its usual sort action. Confirm each case with an ordinary no-special-slots control first:

- An empty CSR-reserved chest slot stays empty while ordinary stacks on both sides sort.
- A CSR-reserved stack (including adjacent compatible stack counts) remains component- and count-exact and does not merge.
- A bundle and a shulker box stay in their original slots; verify a named shulker with contents still has its exact name and contents.
- Several distributed reserved/bundle/shulker slots remain fixed while ordinary slots sort around them, including with empty space available.
- If the sorter supports the player inventory, sort it with a bundle and a shulker in the main inventory; both stay fixed.
- If supported, repeat on a double chest and verify reservations map to the actual physical half/local slot.

Stop and record `FAIL` if a fixed slot changes, accepts a merge, is used as workspace, or a portable container's contents/components change. Record only observed results; a successful build or GameTest is not desktop runtime evidence.
