# Testing

## Current gate

**ACTIVE — STATIC CANDIDATE; NOT DEPLOYED; RUNTIME UNTESTED**

Canary 1 is a client-only candidate. It must not be treated as accepted or as
Minecraft runtime evidence until this project UUID occupies a canonical Test
Instance Manager slot and the focused matrix below is observed in Minecraft.

- Artifact: `ribbits-xaero-entity-icons-compat-0.1.0-canary1.jar`
- Size: 52,119 bytes
- SHA-256: `dac701cb5bb1ec3d42ed9dc642bb224235b2e1cf1dd84528557ef3ab494f585a`
- Selector: exactly one top-level `main`, exactly one direct child `body`, and
  all and only cubes owned directly by that `body`
- Pose/framing: static `main` and `body` pivots and base rotations are inherited;
  each direct cube keeps its own pivot/rotation; a fixed 180-degree Y turn faces
  the selected geometry forward; transformed selected-cube bounds alone are
  centered at `(32, 32)` and uniformly fit into a 58-pixel span at depth `-450`
- Baby policy: `NORMALIZED_ADULT_GEOMETRY_FIT`; no Ribbits age scale is applied,
  so adult and baby icons use the same deterministic selected-geometry fit
- Cache policy: bounded by entity type, provider and selector versions, resolved
  model and texture resources, profession, baby policy, Pride distinction, and
  reload generation; never by animation time, rain/instrument progress, UUID, or
  position. Reload evicts both successful and `FAILED` Ribbit entries.

Two independent `clean check jar --offline --no-daemon` builds with Java 25,
Gradle 9.5.1, and Loom 1.17.19 produced byte-identical 52,119-byte JARs with the
hash above. The project suite passed 56 tests in 12 suites with zero failures,
errors, or skips, including the exact 80-mod/nested production Minecraft-client
Knot/Mixin application harness. The read-only private-model probe confirmed 42
total C7 Geo models: 41 exact `ribbits:ribbit` models supported by this provider,
plus the separately rendered Wandering Ribbit model that Canary 1 excludes. All
41 supported models retain 156 direct `body` cubes, one exact `main/body` path
per model, and cube counts of 3×29, 4×8, 9×3, and 10×1. The unmodified accepted Xaero × EMF Canary
3 suite separately passed 35 tests in 6 suites with zero failures, errors, or
skips. None of this static or controlled evidence launched Minecraft.

## Exact tested inputs

Canary 1 activates only when the four bold runtime gates match in full. A
same-version replacement, standalone XaeroLib, or changed nested origin must
decline safely.

| Input | Exact tested identity |
| --- | --- |
| **Xaero Minimap** | `26.4.2`; 2,221,925 bytes; SHA-256 `69284892d2eb853c9aefa85a4c9b74232c322da00207994c67ab8aeed8a64048` |
| **nested XaeroLib** | `1.7.1`; `META-INF/jars/xaerolib-fabric-26.2-1.7.1.jar`; 621,485 bytes; SHA-256 `7f4a78dd7e046fea0500fef83b1481d85317c8348d47e947035d7a07efe51065` |
| **GeckoLib Fabric** | `5.5.1`; 703,096 bytes; SHA-256 `4bf1c86b4b47aa2c5d84208255695f10d79d609b23d802c995711e64b45cfce0` |
| **Mynx Ribbits** | `4.1.6+26.2-mynx-canary7`; `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary7.jar`; 3,320,708 bytes; SHA-256 `6b18658c5a68d66623b9a388cc644e2f7a1b864e490b6f8b35d57fcd73a5bf74`; source `a7ddb6afa6ed9cd620f49a28522c7866fdd35f04` |
| patched Trinkets | `4.1.0-beta.3+26.2`; `trinkets-4.1.0-beta.3+26.2-inventory-compat-canary5.jar`; 560,208 bytes; SHA-256 `4c1fa6ac36c0457483fd0d395b99bbd94c9334aad6defece7633bbf0552d1724` |
| Minecraft client | `26.2`; 39,193,383 bytes; SHA-256 `40896ee9f1e2bec3c934daac7e93d41e9e3d9c2f8ae0ca366d52ffbfd1afa290` |
| Fabric Loader | `0.19.3`; 1,976,502 bytes; SHA-256 `73eed8c34bbad0320a2a3cba5346351e822f74f82b3f3c060574068474132958` |
| Fabric API | `0.157.0+26.2`; 2,533,297 bytes; SHA-256 `acb7dc90a0430519c49548074d3fbf6fd81d13063f08f0af344b2a6b08a42620` |
| Xaero World Map | `1.44.2`; 1,473,719 bytes; SHA-256 `d55ef45c559ae0adcf66d894c022f61d9d921629b0c885d04aa00424546a2389` |
| accepted Xaero × EMF companion | `0.1.0-canary3`; 28,351 bytes; SHA-256 `4f34d743f5fffd8e938c8f5157c630fd85f3b263ac1ae9f96c432cfe51668df2`; source `2478f021982c5f26e51ff0579fa2f5dad85ed200` |
| EMF / ETF | EMF `3.2.6`, 587,342 bytes, SHA-256 `876a3e4ffda021a6266df87208f2d9980322cf86223d4fe1e313ca996631f115`; ETF `7.1.1`, 762,131 bytes, SHA-256 `f469bc914302a13a5c767296623df60fb0cc3d4e4a02a77c56541a733ad36e3a` |
| Fresh Animations | `1.10.5`; 645,816 bytes; SHA-256 `cf9f17a2977e171b33cb0b598bc4357dd0383e09c10d5f768ff17c12d0a028ee` |

## Deployment prerequisites

Use only a serialized Test Instance Manager operation against the dedicated
26.2 Workbench. Before launch, give UUID
`7d2e42bd-c917-43bf-99a8-94edc24c5948` sole ownership of Slot A or B, verify the
candidate and four activation-gate identities above byte-for-byte, record the
complete managed cohort and active resource-pack order, and confirm the other
slot is preserved. Stop if any gate differs, the artifact is not the recorded
hash, or canonical slot ownership is absent. Never use the protected 26.1.2
gameplay instance.

## Focused runtime matrix

Use one controlled world with named Ribbits in a fixed radar category/range.
Keep the ordinary in-world render visible for comparison and retain the exact
launch log, config, screenshots, cohort identities, and reload sequence.

1. Check resting adult Nitwit, Chef, Farmer, Merchant, and Guard icons. Every
   icon must use its active texture, stay centered/unclipped, and show every
   direct `body` cube even for the 4-, 9-, and 10-cube variants; no child,
   descendant, sibling, arm, leg, shield, spear, hat, tool, or held item may be
   added.
2. Compare adult and baby of one profession. Both must use the normalized
   selected-geometry fit, without animation- or age-driven cache churn.
3. Exercise Pride eligibility, start/stop instrument playing, and enter/leave
   umbrella/rain states. The resolved model/texture may create a bounded new
   entry, but the selector remains literal `main/body` direct cubes only and
   transient progress must not continuously create entries.
4. Change a visual state where supported, reload resources, save/reload, and
   restart once. Confirm both valid and previously `FAILED` Ribbit entries are
   invalidated and regenerated without stale icons.
5. Force safe failures in an instrumented test cohort: dependency mismatch,
   unsupported renderer, missing/ambiguous/empty selector, invalid transform,
   and missing model or texture. Each must yield Xaero's normal dot/name
   fallback without crash, blank capture, stale icon, or whole-body render.
6. Verify one vanilla entity, one Fresh Animations/EMF entity handled by
   accepted C3, one unsupported non-Ribbit GeckoLib entity, a category-excluded
   entity, and entity heads disabled. Canary 1 must not alter any of these paths.
7. Confirm ordinary Ribbit world rendering and accepted C3 behavior remain
   unchanged, and inspect the log for one activation decision plus bounded
   failure/reload diagnostics rather than per-frame spam.

## Result classification

- **PASS:** every applicable matrix case has the intended direct-cube icon and
  active texture; framing, bounded transitions, reload recovery, and safe
  fallback behave as documented; ordinary Ribbits, vanilla, accepted C3, and
  unsupported-Geo controls do not regress.
- **FAIL:** any selected direct cube is missing or pruned; any child, descendant,
  sibling, whole body, accessory, or wrong texture appears; framing clips or
  drifts; cache growth is unbounded or stale; safe failure crashes/blanks; or a
  control path changes.
- **INCONCLUSIVE:** candidate/dependency/slot identity is uncertain, evidence is
  incomplete, the case cannot be reproduced, or configuration/category/range
  prevents distinguishing a failed icon from a hidden marker. Record only what
  was actually observed.
