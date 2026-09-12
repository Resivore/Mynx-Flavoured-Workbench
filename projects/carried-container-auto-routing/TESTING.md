# Testing

**ACTIVE — CONTROLLED VALIDATION PASS — NOT DEPLOYED — RUNTIME UNTESTED**

Test only under explicit Test Instance Manager ownership in the dedicated Matcha Flavoured 26.2 Workbench. Never open or alter the protected Matcha Flavoured 26.1.2 gameplay profile. CCAR C18 is `carried-container-auto-routing-0.3.13-routed-pickup-audio-fallback-canary1.jar` (SHA-256 `7c04136c0f2e852e209dc8130424a04f8db6ffc22dd7b482ff64971d7bd80c4c`, source `5aa03f560824b1944d634e91a6dcf04a479250f7`); pair it with Offhand C8 `offhand-shift-click-qol-0.3.4-routed-pickup-audio-fallback-canary1.jar` (SHA-256 `7ca386bea50eb2ece489da4cccc262a13d45b82171042f39efaea0a91e73e357`).

1. Fully custom-handle an ordinary ground pickup. Expect exactly one normal-volume `entity.item.pickup` pop with natural vanilla pitch variation.
2. Repeat into ordinary selected-main-hand, hotbar, offhand, and inventory destinations. Expect exactly one normal pop per `ItemEntity.playerTouch`, never one per destination or moved segment.
3. Fully route matching items into an unlocked qualifying carried shulker and then a bundle. Expect exactly one normal-volume pop using the same natural variation at a clearly lower `0.80` pitch multiplier.
4. Partially route a matching stack into a carried container while vanilla handles the remainder. Expect exactly one lower-pitched pop, never a second normal fallback.
5. With locked, full, or nonqualifying carriers, verify unchanged routing and one ordinary vanilla pop only when the item is otherwise acquired. With a full inventory and no acquisition, expect no pop.
6. Perform rapid repeated pickups and multi-carrier splits. Expect one cue per pickup event, with no double cues, loss, duplication, or source/remainder drift.
7. Recheck exact counts, carrier contents, CSR reservations, locks, components, effective capacity, Inventory Extended slots, main-hand/hotbar/offhand priority, pickup delay, and save/reload behavior.
8. Run CCAR and Offhand C8 together. Offhand must yield its ground-pickup path and CCAR must be the sole cue owner.
9. Run Offhand C8 without CCAR using its project procedure; its routing must remain behavior-equivalent except for the restored normal local pickup audio.

Stop and preserve exact logs/world state on any missing, doubled, fixed-pitch, wrong-volume, or wrong-pitch cue; any routing order, item-count, carrier, reservation, lock, menu, QUICK_MOVE, Inventory Extended, duplication/loss, or startup regression. Do not infer runtime PASS from this document, a build, or GameTests.
