# Testing

Current `xaero-emf-entity-icon-compat-0.1.0-canary7.jar` is **ACTIVE /
STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED**. It is unaccepted and has no
rollback. Identity: 42,172 bytes, SHA-256
`b70d04024ee22440d573c8b6682c8097e2eda34f5f6e15b171c401db09943ee`; source
checkpoint is `736bc9553528bd31ffc18fb2504423886665de92`.

Two independent Java 25 / Gradle 9.5.1 / Loom 1.17.19 clean offline `check jar`
builds produced byte-identical C7 files. All 45 tests in nine suites passed,
as did the client-only allowlist, CRC and duplicate-entry scan (28 entries;
no duplicate or nested JAR entries), and the combined 80-mod production
Minecraft 26.2 Knot/Mixin application with C7, Xaero 26.4.2, EMF 3.2.6, ETF
7.1.1, Ribbits, GeckoLib and the recorded companion cohort. No Minecraft game
was launched. These are static/classloading results, not runtime observations.

## C6 external runtime evidence reconciled for C7

The user supplied these observations for exact C6 SHA-256
`f4e989342fd767b219c1b1c617845562d5db0966fe0e508db62f5b4cd06a89f3`:
axolotl, sniffer, iron golem, wolf, bat, parrot, witch and ravager visibly
work. Frog, allay and vex instead show Xaero's yellow generic entity square
plus name. All user-tested custom hatted villagers show that same fallback, at
minimum farmer, butcher, cleric and mason. Farmer is specifically a regression:
it worked under C5 and falls back under C6. No result is invented for untested
villager professions.

Xaero's exact manager control flow reads a per-variant cache before it may call
the icon creator. C6 replaced `FAILED` with `null` at that cache getter even
when Xaero had not granted prerendering. The manager then returned `null`,
leaving `FAILED` stored while C6 had already spent the sole retry; later calls
returned FAILED and Xaero selected the generic fallback. C7 moves the narrow
override to the manager's exact cache read: an observed EMF entity keeps FAILED
until `canPrerender` is true, then gets one recreation attempt under the same
key. C7 logs deferred/retried cache state and the final manager result; it does
not claim that a generated, cached or nonempty icon is visibly correct.

## C7 manual runtime matrix

Under separately authorized Test Instance Manager ownership only, install the
exact C7 hash above, clear/reload relevant Xaero resources, and record effective
resource-pack order and model identities. For every owned target, **PASS** is a
correctly textured and framed head icon on the minimap. Xaero's yellow generic
entity square plus name is **FAIL**. Do not promote from this procedure without
actual observations.

| Cases | Required observation |
| --- | --- |
| Preserve: axolotl; sniffer; iron golem; wolf; bat; parrot; witch; ravager | Still display the intended correctly framed head icon with no body/limb/held-item contamination. |
| Fix: frog; allay; vex | Replace the yellow fallback with the intended head icon. Confirm `FAILED_RETRY_DEFERRED_NO_PRERENDER` never consumes the retry and `FAILED_RETRY_AT_PRERENDER` occurs at most once per variant/generation. |
| Fix: farmer; butcher; cleric; mason | Replace the yellow fallback with a head icon including valid head-local/nose-attached hat geometry; exclude body and arms. Farmer is the C5-to-C6 regression control. |
| Other effective hatted villager model, if present | Apply the same structural head/headwear boundary; record its actual profession/model identity and result. The inspected Fresh Animations base pack has a shared villager head/headwear/nose structure, not additional profession-specific JEM files. |
| Controls: normal villager; sheep; horse; sea turtle; creeper; vanilla non-EMF entity | Preserve ordinary Xaero behavior; C7 must neither hijack normal icons nor alter unrelated entities. |
| World rendering and reload | Fresh Animations/EMF world rendering remains intact. Reload removes relevant icon cache entries; no persistent or repeated retry loop occurs. |

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for any yellow fallback,
blank/invisible icon, wrong texture or framing, headwear/body contamination,
ordinary-icon regression, EMF/Fresh Animations world-rendering regression,
Mixin error, or cache behavior outside the bounded result lifecycle.
