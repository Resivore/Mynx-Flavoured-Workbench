# Testing

Canary 5 is `ACTIVE / RUNTIME_UNTESTED`. Controlled tests prove canonical appearance,
Continuity seam compatibility, bounded diagnostics, and contact decisions; they do not prove
Minecraft-client visual output. Use Minecraft Java 26.2, Fabric Loader 0.19.3+, Continuity
`3.0.1+26.2` exactly, and a BGE build satisfying the declared range. Disable shaders. Do not
generate a large test area: C5 preserves independent diagnostic budgets specifically so these
small arrangements remain useful.

At startup capture `[BGE-CTM DIAG] STARTUP`. It reports independent defaults:
`appearanceCap=100`, `regularCap=200`, `ruleSelectionCap=100`, `overlayCap=100`, and
`overlayBaselineCap=10`. Each category deduplicates and suppresses independently. Set any cap
with `-Dbge_ctm.diagnostics.<name>=N` (clamped to 1–300), or disable all diagnostics with
`-Dbge_ctm.diagnostics.disable=true`.

## Test A — regular clear-glass CTM

Place only these side-by-side `UP`-face controls, then stop and save `latest.log`:

1. full clear glass ↔ full clear glass;
2. top clear-glass slab ↔ top clear-glass slab;
3. top clear-glass slab ↔ full clear glass;
4. top clear-glass slab ↔ bottom clear-glass slab (negative).

For #2 and #3, provide managed `RULE_SELECTION` and `REGULAR` lines. `RULE_SELECTION
reason=NO_PROCESSOR` means no matching Continuity processor was selected for that emitted sprite;
`PROCESSOR_SELECTED` plus `REGULAR reason=UPSTREAM_REJECT` means a selected rule/predicate
rejected it; `FINAL_CONNECT` or `STATE_FALLBACK_CONTACT_OK` is a controlled geometry allowance,
not visual proof. The negative must remain `NON_COPLANAR`/`FINAL_VETO` when it reaches contact.

## Test B — proven podzol double-slab case

Use the same directional arrangement for all three sources: full grass receiver, full podzol
baseline, then `more_slabs_stairs_and_walls:podzol_slab[type=double,waterlogged=false]`.

Capture the corresponding `APPEARANCE` and managed `OVERLAY` lines. The double slab must report
`carrier=ORDINARY_SLAB`, canonical `minecraft:podzol[snowy=...]`, and `reason=PROJECTED`; it must
not report `UNMAPPABLE_CANONICAL_STATE`. If the full-podzol baseline has `FINAL_OVERLAY`, the
double slab should reach equivalent native semantic evaluation (`nativeFull=true`) before the
existing real-contact decision. Supply any `CONNECT_BLOCKS_REJECT`, `NATIVE_SEMANTIC_REJECT`, or
final reason verbatim.

## Test C — managed partial source

Only after Test B, repeat its known-positive directional relationship with a podzol `TOP` slab,
then a `BOTTOM` slab (and optionally one Layer). Capture `OVERLAY` lines. A supported partial
source may show `PARTIAL_SOURCE_PROMOTED`, but it must still pass native semantics and coplanar
contact; this confirms C4's partial-source promotion remains bounded. The bottom/recessed control
must not be inferred positive merely because its canonical appearance is podzol.

Send the visual result, exact states/positions/face, shader and pack state, and all relevant
`[BGE-CTM DIAG]` lines. Do not report an aggregate pass/fail in place of those observations.
