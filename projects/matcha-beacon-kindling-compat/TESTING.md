# Testing

Canary 1 is the current candidate and remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`. Test only the exact retained `matcha-beacon-kindling-compat-0.1.0-canary1.jar`, 47,044 bytes, SHA-256 `B343DD5F000913731A9E256126F18ADF085C7E364645CE9F9940692D693E1E0E`, with Matcha Flavoured 1.12 at SHA-256 `6209783021C358044ABEDABACEE471FAFF5BD4080437D4E3B5E51963F1804248`.

The migration verification used Java 25.0.4.1 and Gradle 9.5.1 for a clean build, passed all 26 focused tests in five suites with zero failures, errors, or skips, and reproduced the retained JAR byte-for-byte. The suite covers pure lifecycle transitions, SavedData codecs and exact-identity invariants, required hook registration, the seven-function allowlist, the pinned Matcha archive contract, and the absence of `data/main` overrides. These are source/build/static results, not Minecraft runtime validation.

## Runtime handoff

Run this matrix only under explicit Test Slot ownership in a disposable client or server world. Use two players for the independence and offline-visit checks, and do not infer any behavior that was not directly observed.

1. Place Beacon Kindling and confirm Matcha's signal campfire, particles, sounds, approach message, and periodic marker particle. After ten active minutes, require exactly one Matcha-constructed trader with Matcha trades; after five active visit minutes, require the departure effects/message, extinguished campfire, exact marker removal, and cleared owner lock.
2. Log out the owner partway through approach for longer than the nominal remaining time while another player observes the area. Confirm no trader arrives offline, then relog and verify the persisted summon resumes from its remaining active duration.
3. Unload the beacon chunk during approach, wait past the nominal remaining duration, then reload it. Confirm the mod did not force-load the chunk or create a phantom/immediate trader and that the exact valid beacon resumes normally.
4. Save and restart during approach. Confirm the same owner, marker UUID, dimension, position, phase, and remaining timer resume without a duplicate or stale lock.
5. Log out the owner during the five-minute visit while another player keeps the area observable. Confirm departure pauses, then relog and verify only the exact persisted trader and beacon complete their remaining visit and cleanup.
6. In a disposable pre-patch state, give one player Matcha's `SummonedTrader` tag and timer score without a companion SavedData record. Confirm join recovery clears only that player's stale state and later neutralizes only unowned legacy marker/trader UUIDs.
7. In separate attempts, extinguish and break the exact campfire during approach. Confirm the lost-sight behavior, exact-owner cleanup, no trader creation, and successful reuse of Beacon Kindling afterward.
8. Run two well-separated summons, preferably across dimensions, then pause one by logout or chunk unload. Confirm the other independently approaches, arrives, and departs without changing the paused record; resume and complete the paused summon separately.
9. During a tracked approach, invoke or schedule each intercepted legacy function path and run `/reload`. Confirm nested, top-level, and scheduled legacy calls cannot clear tracked state or create a duplicate, while the guarded arrival boundary still creates exactly one trader.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for any Mixin application failure, duplicate or wrong-owner trader, nearest/global cleanup, timer progress while paused, persistence loss, forced chunk loading, wrong Matcha item/recipe/assets/advancement/messages/effects/trader/trades, broad function interception, unrelated Matcha regression, or relevant startup/reload/shutdown error. Do not promote Canary 1 without a controlled pass of the applicable matrix.
