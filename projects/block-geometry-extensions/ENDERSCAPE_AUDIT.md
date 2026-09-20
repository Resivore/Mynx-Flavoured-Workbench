# Enderscape 3.0.2+mc26.2 material-behavior audit (C86)

Audit input: the retained `originals/mods/enderscape-fabric-3.0.2+mc26.2.jar`
(SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`).
The classifications below apply to BGE's differently shaped derivatives, never
to Enderscape's canonical block. `ofFullCopy` retains properties callbacks, but
does not turn an arbitrary BGE geometry into the provider's concrete block
subclass; that distinction is intentional and tested.

C85 verifies the exact canonical Veiled Leaves resource form: `assets/enderscape/items/veiled_leaves.json`
contains only a `minecraft:model` reference to `enderscape:block/veiled_leaves`, with no item `tints`
array. Its block model inherits `minecraft:block/leaves` and supplies the provider block-model tint route.
Generated BGE items therefore copy an explicit canonical item tint array when one exists, but accept this
model-only form and retain the generated block/source-provider tint path when it does not. C86 additionally
classifies the five DirectionalBlock canonical `facing` states shared with BGE Layer geometry: a full Layer
projects that exact facing into its canonical source rather than resetting it to the source default.

| Source | Provider behavior | Classification | C86 disposition |
| --- | --- | --- | --- |
| `veiled_log`, `veiled_wood`, `celestial_stem`, `celestial_hyphae`, `murublight_stem`, `murublight_hyphae`, `shadoline_pillar`, `dusk_purpur_pillar` | rotated-pillar material state | B | Existing BGE axis profiles retain material `AXIS` independently of the role's slab/stair/wall/etc. state and preserve side/end resources. |
| `chiseled_end_stone`, `cracked_end_stone_bricks`, `chiseled_purpur`, `chiseled_shadoline`, `chiseled_veradite`, `chiseled_mirestone`, `cracked_mirestone_bricks`, `chiseled_kurodite`, `chiseled_dusk_purpur` | ordinary block | A | No provider material state or overriding behavior relevant to a changed geometry was found; copied properties are sufficient. |
| `end_lamp` | ordinary lit block | A | Its fixed properties callback has no state dependency, so `ofFullCopy` safely preserves the material light contract. |
| `nebulite_block` | `NebuliteBlock` | C | Retains C84's `HasMagniaPowerSignal` bridge on all eight BGE roles; the exact source's fixed signal is 15. It has no material blockstate property, so it intentionally creates no state-model variants. |
| `alluring_magnia`, `repulsive_magnia` | `MagniaBlock` | C | Retains C84's exact `power` material state, fixed polarity, placement/neighbor power recomputation, analog output, matching-polarity signal, and Magnia interfaces without consuming geometry-owned state. |
| `blistered_magnia` | `BlisteredMagniaBlock` | C | Retains C84's `polarity` (`NONE` default), canonical neighbor selection, delayed reevaluation, 0/14 light and dynamic map-color callbacks, Magnia interfaces/signals, canonical transition sounds, and state-specific resources to every generated role. |
| `blinklamp` | `BlinklampBlock` | C | Retains C84's `luminance` (default 7), copied dynamic light/map-color callbacks, neighbor-delayed recomputation, transition sounds, and all eight resource-state variants. |
| `veiled_leaves` | `VeiledLeavesBlock` | B | Existing BGE leaf geometry preserves `distance`, `persistent`, waterlogging, decay scheduling, and source tint as material state while retaining each role's geometry. |
| `drift_jelly_block` | `DriftJellyBlock` | B | Existing translucent slime-inset geometry provides BGE's bounce/fall material adaptation while retaining the provider visual/render-layer contract. |
| `celestial_cap`, `murublight_cap` | `ChanterelleCapBlock` | D | The provider overrides `fallOn` for the original cap block. BGE has no cap-specific collision surface or fall-reaction bridge; applying the original callback to Slab/Stair/Wall/etc. would falsely make a source-cap rule appear geometry-independent. Derived forms retain copied physical properties and visuals only; they do not claim the cap's custom landing behavior. |
| `void_shale` | `VoidShaleBlock` | D | The provider's private `natural`, `iteration`, and `stress` state drives source-identity-bound idle-entity shattering, attachment checks, ticking, sounds, and break reaction. It cannot be projected onto arbitrary geometry without a dedicated event/router and shape-specific attachment definition. Derived forms do not claim Void Shale shattering behavior. |
| `veiled_end_stone`, `celestial_overgrowth`, `corrupt_overgrowth`, `celestial_path`, `corrupt_path` | `AbstractOvergrowthBlock` subclasses | D | These classes inherit `DirectionalBlock` and its six-way canonical `facing`, which is the same property instance used by BGE Layer exposed-face geometry. C86 preserves that value only when a Layer completes to the canonical source. Directional placement, path-shaped collision, air/survival scheduling, flatten-item interaction, random-tick transformation, bonemeal, and source/path transitions remain provider-only behavior; BGE retains only its catalogued surface contract. |

`D` is an explicit non-inheritance boundary, not an implicit fallback: the
canonical Enderscape block remains provider-owned and is the only form that
advertises the listed source-specific behavior. C86's controlled GameTests keep
the exact provider dependency and use a concrete DirectionalBlock carrier for
each affected source ID to prove that the shared global `facing` is classified
and projected only on full-Layer canonicalization; existing state-bridge
coverage remains intact.
