# Testing

The exact retained C1 has a frozen partial runtime pass: ordinary Quit Game, Windows title-bar close, and cape-path log checks passed on 2026-08-15. Visible known-cape rendering remains unobserved. This migration performed no Minecraft runtime test.

Use this procedure only with `frozenlib-shutdown-compat-0.1.0-canary1.jar` SHA-256 `85dda807ca525a559dc1123e986da50348e2f18c63a43eec6bb5c3684cc51476` and exact FrozenLib `2.5.3-mc26.2`.

## Shutdown and cape regression procedure

1. Launch the client, enter a world, and wait for ordinary initialization and network activity.
2. Leave through Minecraft's normal Quit Game flow. Confirm world and I/O saving complete, the process exits promptly, no Java process lingers, and no post-main shutdown watchdog or crash report appears.
3. Relaunch, enter a world, and close the client with the Windows title-bar X. Confirm saving and cleanup complete and the process exits promptly without the prior watchdog behavior or a lingering Java process.
4. In both `latest.log` files, confirm the required compatibility mixin loaded and FrozenLib cape initialization produced no new target, cape, HTTPS, JSON, or executor exception.
5. When a known FrozenLib cape is available, confirm it renders normally. This is the only outstanding C1 runtime observation.

Stop and record the run as failed or inconclusive if the process lingers, the shutdown watchdog or a crash report appears, saving or cleanup does not complete, the required mixin target fails, a new cape/network/executor exception appears, or a known cape fails to render. Preserve the affected log and crash report. Do not infer runtime success from compilation, the focused unit test, or JAR production.
