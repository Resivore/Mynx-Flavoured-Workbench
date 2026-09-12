# Testing

**ACTIVE — CONTROLLED VALIDATION PASS — NOT DEPLOYED — RUNTIME UNTESTED**

Use only explicit Test Instance Manager ownership of the dedicated Matcha Flavoured 26.2 Workbench; never access the protected Matcha Flavoured 26.1.2 gameplay profile. Offhand C8 is `offhand-shift-click-qol-0.3.4-routed-pickup-audio-fallback-canary1.jar` (SHA-256 `7ca386bea50eb2ece489da4cccc262a13d45b82171042f39efaea0a91e73e357`, source `5aa03f560824b1944d634e91a6dcf04a479250f7`). C7 remains accepted and rollback provenance.

1. With CCAR absent, fully custom-handle a matching ground pickup through selected main hand, occupied hotbar, occupied offhand, empty hotbar, and ordinary inventory. Each pickup must have exactly one normal-volume `entity.item.pickup` pop with natural Minecraft 26.2 pitch variation.
2. Create a partial custom route that vanilla finishes. Expect vanilla's one pop only, with no fallback duplicate.
3. Use full/no-space destinations and a full inventory. When nothing is acquired, expect no sound; otherwise retain exactly one ordinary vanilla pop.
4. Rapidly repeat pickups and split across eligible destinations. Verify one cue per `ItemEntity.playerTouch`, exact source/remainder/item counts, no loss or duplication, and unchanged destination priority.
5. Verify QUICK_MOVE, menu interactions, Inventory Extended, foreign-slot exclusion, components, save/reload, and pickup delay are unchanged and never create a pickup-audio fallback.
6. Reinstall/pair current CCAR C18 (`carried-container-auto-routing-0.3.13-routed-pickup-audio-fallback-canary1.jar`, SHA-256 `7c04136c0f2e852e209dc8130424a04f8db6ffc22dd7b482ff64971d7bd80c4c`). Offhand must yield ground-pickup routing and emit no competing cue; CCAR alone supplies one appropriate normal or lower-pitched cue.

Stop and preserve logs/world state for a missing, doubled, fixed-pitch, wrong-volume, or wrong-pitch pop; altered routing, count/remainder mismatch, loss/duplication, unexpected foreign-slot route, menu failure, mixin/startup error, or persistence regression. Do not promote C8 or infer runtime PASS from build or static tests.
