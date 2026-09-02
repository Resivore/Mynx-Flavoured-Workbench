# Testing

Canary 1 is the exact current candidate in Test Slot A and remains independently `RUNTIME_UNTESTED`. Test only `artifacts/container-slot-reservations-0.1.0-canary1.jar`, 63,388 bytes, SHA-256 `4E7F0A470A387BE76189D0A5E1AE8C614D17EC8B24A6774B400838CC234C4532`, from source checkpoint `e0adc6b9302392440f2e5654c1e4031916b0c996`. Manager revision 67 identifies it as deployment `c0fdfff1-831a-4081-93a3-e596d7fc928d`, artifact `c3bc9365-5664-4148-9200-3292307f9bce`, under `Container Slot Reservations - Canary 1`, with `CURRENT_RELEASE_DEPLOYED` and `READY_TO_TEST_VERIFIED`.

The deployed artifact is the unchanged retained C1 identity; it was not rebuilt, repackaged, relabeled, or given successor-only dependency-policy metadata. Its existing verification remains `STATIC_PASS`: Temurin Java 25.0.4.1 and Gradle 9.5.1 `clean test runGameTest build --offline --no-daemon` passed 28/28 JUnit tests and all 10 required Fabric GameTests, and a clean LF export reproduced the exact retained JAR byte-for-byte. Those are build, contract, serialization, and server GameTest results, not Minecraft gameplay/runtime validation. Minecraft was not launched for this transition.

A normal Windows checkout had previously produced a semantically equivalent but byte-distinct verification JAR because three JSON resources used CRLF instead of the retained artifact's LF; all other 50 JAR entries matched. That output was discarded and never replaced the exact retained artifact. Stop if the manager no longer reports revision 67 or a later canonical revision preserving the exact deployment ID, artifact ID, filename, SHA-256, version, and source checkpoint above.

## Runtime handoff

Run this matrix only under explicit runtime ownership in the dedicated Minecraft 26.2 Workbench, while exact C1 remains verified in Slot A alongside the current accepted baseline and the independent Slot B candidate. Do not touch the protected gameplay profile, rebuild or substitute a same-version JAR, infer evidence from automated tests, or mutate Slot B.

1. In a single chest, reserve an occupied slot and a cursor-derived empty slot, clear both forms, close and reopen, and confirm the exact indices and component-aware templates persist.
2. Repeat the reserve, clear, close, and reopen checks in a trapped chest and a barrel.
3. In a double chest, reserve slots in both physical halves; reopen from either half, break one half, verify the survivor retains only its physical reservations, re-form the double chest, and confirm reservations did not migrate between halves.
4. Reserve placed shulker slots, close and reopen, break and carry the shulker, relog, place it again, and confirm the exact local indices persist.
5. Repeat the shulker round trip with an undyed and a colored shulker carrying physical contents, a custom name, and other meaningful components; confirm color, contents, name, unrelated components, and reservations all remain intact.
6. Confirm an empty reservation shows a faded count-free ghost, project outline, and reservation tooltip at the live slot coordinates; confirm an occupied reserved slot shows only its marker.
7. Manually insert a matching stack into an empty reserved slot, then try an item with the same registry carrier but different meaningful components; accept only the exact component identity without moving, deleting, or duplicating the rejected stack.
8. Shift-click matching and component-distinct items and confirm vanilla destination order is unchanged except that mismatched reserved empty slots are skipped.
9. Insert matching and component-distinct items with a vanilla hopper and confirm vanilla ordering and cooldown remain unchanged; extract manually and by hopper until empty and confirm the reservation remains.
10. Verify reservations across chunk unload/reload, world save/reload, and a clean server restart.
11. With two players viewing one supported container, change reservations from each side and confirm server-authoritative convergence without stale ghosts or unauthorized slot changes.
12. With the intended inventory UI stack enabled, repeat representative display, manual-placement, and QUICK_MOVE checks and confirm player, crafting, result, equipment, trinket, trash, fake, and foreign slots remain ineligible while expanded live coordinates stay aligned.
13. Exercise rapid reserve, replace, clear, insert, and extract actions and confirm no duplication, deletion, detached-copy writeback, or client/server desynchronization.
14. Exit through a clean shutdown and inspect the complete log for project-attributable mixin, codec, component, networking, persistence, or rendering errors.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if the artifact or dependency identities differ, readiness verification fails, an unsupported owner becomes eligible, physical-half ownership changes, a component-distinct item is admitted, insertion order changes beyond the reservation predicate, persistence or synchronization fails, any item or unrelated component is lost, or a project-attributable error occurs. Do not accept Canary 1 without a controlled pass of the applicable matrix.
