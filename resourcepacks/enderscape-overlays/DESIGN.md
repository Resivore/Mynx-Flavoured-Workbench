# Enderscape Overlays design

This resource pack is generated from two pinned local inputs. It is deliberately a relationship table, not a hand-maintained set of CTM images: for every relationship the generator reads a Matcha v37 tile's alpha channel, combines it with the exact Enderscape material texture RGB, and writes the 17-tile `overlay` set plus its local Continuity-compatible properties file.

The exact Enderscape input is `enderscape-fabric-3.0.2+mc26.2.jar` (`9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`), which declares MIT in `fabric.mod.json`. The exact Matcha input is `Matcha-Overlays-v37.zip` (`2642dcea338100f469df905b212423683e83ae7c683c7b9fabfbd2195a2fe802`), whose `LICENSE` is CC BY-NC 4.0. The repository therefore contains the generator and manifest only; the derivative ZIP is ignored, locally retained, and includes attribution plus a CC BY-NC notice. Neither source archive is copied or changed.

The input inspection establishes these concrete constraints:

- `celestial_overgrowth_0.json` and `veiled_end_stone_0.json` retain `minecraft:block/end_stone`; `corrupt_overgrowth_0.json` retains `enderscape:block/mirestone`.
- `celestial_cap.json` and `murublight_cap.json` are `minecraft:block/cube_all`; both receive the mushroom-cap template. Their Void Lachryma relationship is singular and directed: Celestial Cap owns the seam with Murublight Cap. The generic cap rules are self-only, so neither an opposite rule nor a stacked generic rule can apply.
- Column models for Veiled Log, Celestial Stem, and Murublight Stem use `cube_column`. Their rules use `faces=sides`, preserving the Matcha log/stem semantics and excluding end-grain under all axes.
- Matcha v37 has alpha-equivalent generic ore sets and generic plank sets. The generator verifies those equivalence claims before every build.
- The Enderscape Void Lachryma recipes establish Veradite → Kurodite, corresponding brick conversions, and Celestial → Murublight plank, brick, stem, and cap conversions. The direct target rules use the existing Matcha weathered-copper topology and `prioritize=true`; the generic relations explicitly list those high-priority seams as exclusions.

Veiled End Stone uses the mycelium donor rather than warped nylium. Both are top-face terrain templates, but v37's mycelium topology is the structurally appropriate subdued veiling transition; warped nylium is reserved for the explicitly corrupt terrain relationship.

Canary 2 declares the natural-terrain seam order as `Void Shale > Celestial Overgrowth > Corrupt Overgrowth > Veiled End Stone > Mirestone > Veradite > End Stone`. Every cross-material edge is generated only from the higher-ranked source; static checks reject an opposite source or overlapping generic/specific property rule. Mirestone and Veradite are verified `cube_all` materials and use Matcha v37's `overlays/cobblestone` alpha set. Void Shale is verified from its pinned blockstate and four stress-model variants: its lateral faces use `void_shale_side` and its upper/lower end faces use `void_shale_end`, each with Matcha's `overlays/packed_mud` alpha set and matching emissive companion tiles. This preserves its model's non-cube-all face semantics.

Slabs, stairs, walls, BGE geometry, decorative/chiseled sources, stripped wood, leaves, lamps, Magnia, Drift Jelly, Shadoline construction, and Purpur-related families are intentionally deferred. No claim about their CTM surface behavior is made by this canary.
