# Codex Log

## 2026-09-07T03:15:00Z — Assemble private Florist successor v2
- Revision: 1
- Source checkpoint: `18ec89dcfbc627a6adc003dc9d735788cdd2013a`
- Changes: Added a deterministic private-only assembler that validates the exact Ribbit Villagers source archive, preserves all non-target bytes, activates reserved model 6 for `mynx_flora_trades:florist`, and routes the matching existing Gardener outfit as the Florist outfit.
- Build/static: Source-entry validation and archive assembly passed.
- Runtime: RUNTIME_UNTESTED; no resource-pack deployment or gameplay profile was touched.
- Artifact: `ribbit-villagers-florist-private-v2.zip`; SHA-256 `331ee9d0857aded60f7720ee7b32bdec3e339e5f4cc7300f6c61f7926b7b0453`.
- Result: ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED.
- Next state: Retain the exact private archive, publish the integrated revision from main through the gated Sheet workflow, then validate the Florist route in the dedicated Workbench.
