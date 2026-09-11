# Codex Log

## 2026-09-11T21:24:43Z — Create item-pickup audio diagnostic Canary 1
- Revision: 1
- Source checkpoint: `56b3ccd9f9a98bf2ad082405ce917cc32a1c463f`
- Changes: Added a standalone client-only Fabric 26.2 diagnostic that observes only `minecraft:entity.item.pickup`: take-item packet entry, the actual `ClientLevel.playLocalSound(DDD, SoundEvent, SoundSource, FF, boolean)` method, `SoundManager.play`, and `SoundEngine.play`. The engine observer records entry, the first vanilla field-read boundary after all HEAD callbacks, post-`SoundInstance.resolve` concrete sample fields, and natural `PlayResult`. All Mixins are non-cancellable and do not alter arguments, invoke playback, touch CCAR state, or log non-pickup sounds. INIT obtains only the four named optional-mod versions from Fabric Loader metadata.
- Build/static: Temurin Java 25.0.4.1+1, Gradle 9.6.1, and Fabric Loom 1.17.20 clean `test build stageCanaryArtifact --no-daemon` passed. Three focused JUnit checks cover pickup-only qualification, non-mutating/no-CCAR observation architecture, and client-only/no-target-dependency metadata. Release verification confirms required diagnostic classes/resources, exact metadata, runtime-policy attestation, and no foreign or nested implementation.
- Runtime: No Test Instance Manager operation, dedicated Matcha Flavoured 26.2 Workbench mutation, Minecraft launch, user runtime observation, or protected Matcha Flavoured 26.1.2 gameplay-profile access occurred. This release is independently RUNTIME_UNTESTED.
- Artifact: Current embedded `0.1.0-canary1`: `item-pickup-audio-diagnostic-0.1.0-canary1.jar`, 13,392 bytes, SHA-256 `fc67ec7816ab91e941b9df99f54a254705dea8adb07da7c50d8795190d46b094`, source `56b3ccd9f9a98bf2ad082405ce917cc32a1c463f`, CAPABILITY_OR_PROVIDER with no exceptions. The ignored artifact was staged for canonical local retention.
- Result: ACTIVE — CONTROLLED_VALIDATION_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED; no blocker.
- Next state: Retain the exact artifact; deploy only through a supported non-evicting dedicated-Workbench transaction, then collect the single ordinary-pickup trace before diagnosing or changing any sound behavior.
