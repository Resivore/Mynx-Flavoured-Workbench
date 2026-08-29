# Codex Log

## 2026-08-29T20:58:13Z — Migrate frozen FrozenLib Shutdown Compatibility into Mynx Flavoured Workbench
- Revision: 1
- Source checkpoint: `c4a2db96387c5c9dca97769874134bd77e709b04`
- Changes: Imported the maintainable Gradle build, exact source/resources, focused unit test, and sole necessary C1 artifact from frozen legacy checkpoint `f0101f38c446dddaa4226450386402c63597ba7c`. The preserved C1 implementation originated at legacy commit `45fed67116ebcb6d7993d4c2619abe0be02bd9ce` from documented source base `9c000675f7e4cb906f16ca2ca7e122484688b6a3`; no successor or obsolete canary was imported.
- Build/static: Temurin Java 25.0.4+7 and Gradle 9.5.1 `clean test build --no-daemon` passed the focused daemon-worker test with zero failures, errors, or skips. The exact retained 3,576-byte artifact was independently SHA-256 verified; the fresh 3,580-byte verification build had SHA-256 `3af288c1aa3ac9bdda5f73bb1f9a854220da1baeb46e514f37c61ac36e40ecf0` and did not replace the retained JAR.
- Runtime: No deployment or Minecraft runtime check was performed during migration; the protected gameplay instance, dedicated Workbench, runtime slots, accepted stack, and Test Instance Manager state were untouched. Preserved frozen evidence is limited to the exact C1's 2026-08-15 ordinary Quit Game pass, Windows X-close pass, and cape-path log regression pass; visible known-cape rendering remains unobserved, and no dedicated-server validation exists.
- Artifact: Current C1 `frozenlib-shutdown-compat-0.1.0-canary1.jar` SHA-256 `85dda807ca525a559dc1123e986da50348e2f18c63a43eec6bb5c3684cc51476`; accepted and rollback releases remain null because the frozen project classified acceptance as not applicable and retained C1 as permanent infrastructure outside project rollback ownership.
- Result: TESTING — exact C1 remains DEPLOYED with a PARTIAL_RUNTIME_PASS from frozen Minecraft evidence; the build is STATIC_PASS, no formal V2 blocker is active, and visible known-cape rendering remains outstanding.
- Next state: Observe known-cape rendering only when a suitable exact C1 runtime case is available, and repeat both shutdown paths after any future target-version or implementation change.

## 2026-08-29T21:20:18Z — Accept FrozenLib Shutdown Compatibility Canary 1
- Revision: 2
- Source checkpoint: `113eb4a2a24122f64cb1c79a29e99daa762c3b77`
- Changes: User explicitly removed visible known-cape rendering from the acceptance criteria. Promoted the exact retained C1 from TESTING to ACCEPTED, recorded the current C1 as the accepted release, and narrowed `TESTING.md` to the useful future shutdown-regression procedure. No implementation, dependency, artifact, deployment, runtime-slot, or Test Instance Manager changes were made.
- Build/static: No new build was required for this status-only acceptance decision. The retained C1 remains STATIC_PASS with its prior focused test and exact-artifact verification evidence unchanged.
- Runtime: No new Minecraft run was performed. Acceptance is based on the preserved 2026-08-15 ordinary Quit Game pass, Windows title-bar close pass, and cape-path log regression pass. Visible known-cape rendering remains unobserved and is explicitly non-gating; the existing runtime evidence is sufficient for the project's shutdown-compatibility scope, so runtime state is now `RUNTIME_PASS`.
- Artifact: Accepted/current C1 remains `frozenlib-shutdown-compat-0.1.0-canary1.jar` SHA-256 `85dda807ca525a559dc1123e986da50348e2f18c63a43eec6bb5c3684cc51476`, source commit `45fed67116ebcb6d7993d4c2619abe0be02bd9ce`; no rollback artifact is defined.
- Result: ACCEPTED — C1 is the accepted permanent-infrastructure release for exact FrozenLib `2.5.3-mc26.2`.
- Next state: Keep C1 accepted. Repeat the shutdown regression procedure only after a FrozenLib target-version change, compatibility implementation change, or observed shutdown regression.
