# Codex Log

## 2026-09-20T03:44:16Z — Generate Enderscape Overlays Canary 1

- Revision: 1
- Source checkpoint: `ef9244de7ce6c6d006d73a4d4425e9c3a220554d`
- Changes: Added the declarative relationship manifest and deterministic generator, then generated the first local Enderscape CTM canary. The manifest covers the inspected high-confidence terrain, natural ground-contact, plank, End Stone construction, Mirestone, Veradite, Kurodite, ore, emissive brick, Celestial/Murublight brick and cap, and exact Void Lachryma transition families from Enderscape input `enderscape-fabric-3.0.2+mc26.2.jar` SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b` and Matcha input `Matcha-Overlays-v37.zip` SHA-256 `2642dcea338100f469df905b212423683e83ae7c683c7b9fabfbd2195a2fe802`.
- Build/static: Pinned-input hash checks, PNG decoding, generic plank and ore alpha-equivalence checks, deterministic archive generation, animation metadata preservation, and archive regeneration/byte verification passed. The finalized archive has 748 entries.
- Runtime: RUNTIME_UNTESTED; no Minecraft runtime test, resource-pack deployment, or gameplay profile access occurred.
- Artifact: `enderscape-overlays-0.1.0-canary1.zip`; SHA-256 `4fbd16859792830c7ee040bb757d88805941540389e4ef4d9c67509505a472c3`; finalized `2026-09-20T03:44:16Z`.
- Result: ACTIVE / STATIC_PASS / RUNTIME_UNTESTED.
- Next state: Retain this exact local artifact and perform the targeted manual Continuity checks in TESTING.md before any owner acceptance decision.

## 2026-09-20T14:19:58Z — Generate Enderscape Overlays Canary 2

- Revision: 2
- Source checkpoint: `6d375d74f92aa804c754e28803ad0b93fd664f43`
- Changes: Corrected the Celestial Cap/Murublight Cap seam to the sole high-priority Celestial → Murublight direction; audited the paired plank and brick generic rules so their existing high-priority Void Lachryma directions also cannot stack or reverse. Added verified natural-stone CTMs: Mirestone and Veradite use Matcha v37 `overlays/cobblestone`, while Void Shale uses `overlays/packed_mud` with distinct lateral and end-face properties plus matching emissive companions from the pinned Void Shale blockstate/model assets. Declared and validated the complete directed terrain hierarchy `Void Shale > Celestial Overgrowth > Corrupt Overgrowth > Veiled End Stone > Mirestone > Veradite > End Stone`.
- Build/static: Pinned-input hash checks, exact Cobblestone/Packed Mud 17-tile overlay-donor checks, Void Shale blockstate/model/texture resolution, directed cap and terrain-pair checks, reverse-rule rejection, non-overlapping source/target face coverage, PNG decoding, existing alpha-equivalence checks, and deterministic entry plus byte-identical archive regeneration passed. The finalized archive has 887 entries.
- Runtime: RUNTIME_UNTESTED; no Minecraft runtime test, resource-pack deployment, or gameplay profile access occurred.
- Artifact: `enderscape-overlays-0.1.0-canary2.zip`; SHA-256 `7d6e72e8bba770e88f0fe1d58d25aa3bc9990e16826a355cce0638285415867b`; finalized `2026-09-20T14:19:58Z`.
- Result: ACTIVE / STATIC_PASS / RUNTIME_UNTESTED.
- Next state: Retain this exact local artifact and perform the targeted Canary 2 manual Continuity matrix in TESTING.md before any owner acceptance decision.

## 2026-09-22T05:15:06Z — Generate Enderscape Overlays Canary 3

- Revision: 3
- Source checkpoint: `d82d1eb6394c3254b09b50c77bfeb29e3fc8669e`
- Changes: Replaced the Canary 2 natural-terrain order with the complete directed hierarchy `Celestial Overgrowth > Corrupt Overgrowth > Veiled End Stone > Alluring Magnia > Repulsive Magnia > Mirestone > Veradite > End Stone > Void Shale`. Void Shale is now target-only: its stress-state side/top/bottom source properties, tiles, and emissive source tiles are removed. Inspected pinned Enderscape blockstates/models confirm Alluring and Repulsive Magnia are `cube_all` blocks using their own named textures; their 17-tile overlays preserve that RGB while Matcha v37 Cobblestone provides alpha topology only. The final End Stone to Void Shale pair uses the inspected, hash-pinned local Minecraft 26.2 End Stone RGB with that same topology.
- Build/static: Exact nine-member hierarchy and every sole higher-to-lower owner, reverse-rule rejection, no double overlays, target-only Void Shale, removal of old Void Shale source artifact entries, Magnia RGB/alpha provenance per tile, pinned Enderscape/Minecraft/Matcha inputs, donor-set topology, existing directed corruption/cap checks, PNG decoding, and deterministic archive regeneration passed. Minecraft runtime validation was not performed.
- Runtime: RUNTIME_UNTESTED; no Minecraft runtime test, resource-pack deployment, or gameplay profile access occurred.
- Artifact: `enderscape-overlays-0.1.0-canary3.zip`; SHA-256 `845cec5fb6c957673090c3fa9910399b8834489d5693e581094a966116f74cd9`; finalized `2026-09-22T05:15:06Z`.
- Result: ACTIVE / STATIC_PASS / RUNTIME_UNTESTED.
- Next state: Retain this exact local artifact and perform the targeted Canary 3 manual Continuity matrix in TESTING.md before any owner acceptance decision.
