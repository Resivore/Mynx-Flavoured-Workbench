# Testing

## Canary 1 diagnostic procedure

Use only exact `item-pickup-audio-diagnostic-0.1.0-canary1.jar`, embedded
version `0.1.0-canary1`, SHA-256
`fc67ec7816ab91e941b9df99f54a254705dea8adb07da7c50d8795190d46b094`.
It is a temporary client-side observer, not a sound fix. Use only the dedicated
Matcha Flavoured 26.2 Workbench; never access the protected 26.1.2 profile.

1. Install this exact JAR by the supported dedicated-Workbench mechanism alongside
   the current test environment, without evicting any existing member.
2. Launch the dedicated 26.2 Workbench and confirm one `[PickupAudioDiag][INIT]`
   line reports Minecraft plus the selected loaded-mod versions or `not-loaded`.
3. Enter a world, drop one ordinary item, pick it up normally once, then exit.
4. Inspect only `[PickupAudioDiag]` lines for the same numeric sequence. A normal
   synchronous trace is ordered `PACKET`, `LOCAL_SOUND`, `MANAGER`,
   `ENGINE_ENTER`, `ENGINE_SURVIVED_HEAD`, `RESOLVED`, and `ENGINE_RETURN`.

`PACKET` includes item entity, collector, amount, sequence, thread, and monotonic
elapsed time. `LOCAL_SOUND` includes the exact event, category, volume, pitch,
coordinates, and delay flag. Manager and engine records include event, category,
volume, pitch, attenuation, relative state, coordinates, and thread. `RESOLVED`
adds the concrete sample location/path/type and stream/attenuation metadata;
`ENGINE_RETURN` reports the natural 26.2 `SoundEngine.PlayResult`.

Interpret a missing checkpoint narrowly:

- No `PACKET`: the client did not receive the expected take-item packet.
- `PACKET` but no `LOCAL_SOUND`: the path stops in or before vanilla's take-item
  local-sound request.
- `LOCAL_SOUND` but no `MANAGER`: it stops before the sound manager facade.
- `MANAGER` but no `ENGINE_ENTER`: it stops at or before sound-engine entry.
- `ENGINE_ENTER` but no `ENGINE_SURVIVED_HEAD` and no `ENGINE_RETURN`: an
  interception after the entry observer prevented the first vanilla instruction.
- `ENGINE_SURVIVED_HEAD` but no `RESOLVED`, with `ENGINE_RETURN=NOT_STARTED`:
  vanilla's initial engine eligibility checks stopped it; this is not evidence of
  a HEAD cancellation.
- `RESOLVED` and `ENGINE_RETURN`: event resolution and natural engine processing
  occurred. If the pop remains inaudible, investigate the later channel/device
  layer from this exact boundary rather than changing a mod speculatively.

Stop and record an inconclusive result if the INIT versions or JAR identity drift,
any Mixin fails during startup, lines do not share a safe sequence, unrelated
sounds log per-sound lines, or ordinary pickup behavior changes. Do not infer a
runtime pass from startup or this static build.
