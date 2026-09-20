# Enderscape 3.0.2+mc26.2 material-behavior audit (C84)

Audit input: the retained `originals/mods/enderscape-fabric-3.0.2+mc26.2.jar`
(SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`).
The classifications below apply to BGE's differently shaped derivatives, never
to Enderscape's canonical block. `ofFullCopy` retains properties callbacks, but
does not turn an arbitrary BGE geometry into the provider's concrete block
subclass; that distinction is intentional and tested.

| Source | Provider behavior | Classification | C84 disposition |
| --- | --- | --- | --- |
| `veiled_log`, `veiled_wood`, `celestial_stem`, `celestial_hyphae`, `murublight_stem`, `murublight_hyphae`, `shadoline_pillar`, `dusk_purpur_pillar` | rotated-pillar material state | B | Existing BGE axis profiles retain material `AXIS` independently of the role's slab/stair/wall/etc. state and preserve side/end resources. |
| `chiseled_end_stone`, `cracked_end_stone_bricks`, `chiseled_purpur`, `chiseled_shadoline`, `chiseled_veradite`, `chiseled_mirestone`, `cracked_mirestone_bricks`, `chiseled_kurodite`, `chiseled_dusk_purpur` | ordinary block | A | No provider material state or overriding behavior relevant to a changed geometry was found; copied properties are sufficient. |
| `end_lamp` | ordinary lit block | A | Its fixed properties callback has no state dependency, so `ofFullCopy` safely preserves the material light contract. |
| `nebulite_block` | `NebuliteBlock` | C | C84 gives all eight BGE roles `HasMagniaPowerSignal`; the exact source's fixed signal is 15. It has no material blockstate property, so it intentionally creates no state-model variants. |
| `alluring_magnia`, `repulsive_magnia` | `MagniaBlock` | C | C84 adds the exact `power` material state, fixed polarity, placement/neighbor power recomputation, analog output, matching-polarity signal, and Magnia interfaces without consuming geometry-owned state. |
| `blistered_magnia` | `BlisteredMagniaBlock` | C | C84 adds `polarity` (`NONE` default), canonical neighbor selection, delayed reevaluation, 0/14 light and dynamic map-color callbacks, Magnia interfaces/signals, canonical transition sounds, and state-specific resources to every generated role. |
| `blinklamp` | `BlinklampBlock` | C | C84 adds `luminance` (default 7), copied dynamic light/map-color callbacks, neighbor-delayed recomputation, transition sounds, and all eight resource-state variants. |
| `veiled_leaves` | `VeiledLeavesBlock` | B | Existing BGE leaf geometry preserves `distance`, `persistent`, waterlogging, decay scheduling, and source tint as material state while retaining each role's geometry. |
| `drift_jelly_block` | `DriftJellyBlock` | B | Existing translucent slime-inset geometry provides BGE's bounce/fall material adaptation while retaining the provider visual/render-layer contract. |
| `celestial_cap`, `murublight_cap` | `ChanterelleCapBlock` | D | The provider overrides `fallOn` for the original cap block. BGE has no cap-specific collision surface or fall-reaction bridge; applying the original callback to Slab/Stair/Wall/etc. would falsely make a source-cap rule appear geometry-independent. Derived forms retain copied physical properties and visuals only; they do not claim the cap's custom landing behavior. |
| `void_shale` | `VoidShaleBlock` | D | The provider's private `natural`, `iteration`, and `stress` state drives source-identity-bound idle-entity shattering, attachment checks, ticking, sounds, and break reaction. It cannot be projected onto arbitrary geometry without a dedicated event/router and shape-specific attachment definition. Derived forms do not claim Void Shale shattering behavior. |
| `veiled_end_stone`, `celestial_overgrowth`, `corrupt_overgrowth`, `celestial_path`, `corrupt_path` | `AbstractOvergrowthBlock` subclasses | D | These classes own directional placement, path-shaped collision, air/survival scheduling, flatten-item interaction, random-tick transformation, bonemeal, and source/path transitions. Those contracts are tied to the provider's full/path geometry and cannot be truthfully overlaid on BGE's independent role state. BGE retains only the explicitly catalogued side/top/bottom or lowered-path material surface contract; it does not claim the provider interaction/transformation behavior. |

`D` is an explicit non-inheritance boundary, not an implicit fallback: the
canonical Enderscape block remains provider-owned and is the only form that
advertises the listed source-specific behavior. C84's GameTests construct every
admitted family with the exact provider JAR, exercise every C bridge, and prove
that every C-bridge copied callback has its required derived state.
