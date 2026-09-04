# Testing

## Current gate

**PLANNED — NO RUNTIME CANDIDATE**

This revision is an audit and architecture plan only. It creates no production
source, deployable artifact, Canary, accepted release, or rollback. The static
audit and the passing Xaero × EMF C3 suite are not Minecraft runtime evidence.
Do not deploy this project or assign its UUID to a testing slot until a later
task produces a fully identified candidate and obtains explicit runtime
ownership.

The historical user observation cannot be bound to an exact Ribbits artifact:
the dedicated 26.2 profile currently contains neither Ribbits nor GeckoLib and
its available logs do not identify either. A future test must record the actual
Ribbits, GeckoLib, Xaero, EMF/ETF, compatibility, Loader/API, and resource-pack
identities before drawing a runtime conclusion.

## Future candidate prerequisites

Before launch, record and hash:

- the candidate compatibility artifact and source checkpoint;
- Ribbits artifact/version and GeckoLib artifact/version;
- Xaero Minimap, nested XaeroLib, Xaero World Map, EMF, ETF, Fabric Loader,
  Fabric API, Minecraft, and Java;
- active resource packs and ordering, especially Fresh Animations and any pack
  with a Ribbits or Xaero icon namespace;
- Xaero radar/category configuration and the exact cache/resource-reload state;
- the dedicated slot/cohort identity and matching launch log.

Stop before launch if any production mixin or bridge version contract differs
from its audited dependency, if the candidate cannot fail closed, if the
Ribbits selector does not recognize the exact model structure, or if this UUID
does not have explicit canonical slot ownership.

## Focused runtime acceptance procedure

Use one controlled world with named adult Ribbits representing Nitwit, Chef,
Farmer, Merchant, and Guard. Keep each within the same known radar range and
category. Capture the minimap result and the ordinary in-world entity render
for every case.

1. Confirm an adult Nitwit at rest yields only the intended face/core with the
   base profession texture, centered and unclipped.
2. Repeat with Chef and Farmer to cover donor-derived direct-body geometry and
   private profession textures. Repeat with Merchant for its larger direct
   cube set and with Guard to prove spear/shield siblings are excluded.
3. Compare one adult and baby of the same profession. Verify the documented
   normalize-or-preserve scale policy and that cache reuse cannot apply an
   unintended adult transform to the baby.
4. Exercise Pride eligibility for a Nitwit. Verify the explicitly chosen Pride
   policy and stable cache discriminator; do not infer this case from date or
   UUID without recording it.
5. Begin and end instrument playing. The head icon must not contain instrument,
   arms, or other accessory geometry and must not create unbounded cache churn.
6. Enter and leave rain/umbrella rendering, covering every structurally distinct
   approved umbrella family. No umbrella or descendant body geometry may enter
   the icon.
7. Change a profession/state where supported, reload resources, save/reload,
   and restart once. Verify valid and failed icon entries invalidate or persist
   only according to the documented key.
8. Force each safe failure control in a test build: unsupported Geo renderer,
   missing selector, unknown structural fingerprint, missing model/texture, and
   ambiguous target. Each must return Xaero's normal dot/name fallback without
   crash, stale icon, blank framebuffer, or whole-body geometry.
9. Run regression controls: one vanilla living entity, one Fresh
   Animations/EMF entity covered by accepted C3, one unsupported non-Ribbit Geo
   entity, one category-excluded entity, and entity heads disabled. The generic
   bridge must not change these paths.
10. Inspect the matching log for mixin/plugin version decisions, bridge
    failures, resource errors, and renderer exceptions. Preserve screenshots,
    config, log, and exact identities for any failure or inconclusive result.

Pass only when every applicable case produces the intended face-only icon and
texture, transition/cache behavior is stable, failures are safe, normal world
rendering is unchanged, and the vanilla/accepted-EMF controls do not regress.
Classify a missing marker separately from a failed head icon: with current
`icons=Always` and `displayNameWhenIconFails=true`, a supported failure should
normally remain a colored dot plus name when labels are permitted.
