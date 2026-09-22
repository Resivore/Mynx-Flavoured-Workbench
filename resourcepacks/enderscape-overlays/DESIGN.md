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

Canary 3 declares the complete natural-terrain seam order as `Celestial Overgrowth > Corrupt Overgrowth > Veiled End Stone > Alluring Magnia > Repulsive Magnia > Mirestone > Veradite > End Stone > Void Shale`. Every cross-material edge is generated only from the higher-ranked source; static checks require the sole declared owner for every pair, reject reverse rules and double overlays, and make Void Shale target-only. Void Shale's Canary 2 stress-face source properties, tiles, and emissive source handling are removed entirely. Mirestone, Veradite, Alluring Magnia, Repulsive Magnia, and the required native End Stone edge use Matcha v37's `overlays/cobblestone` alpha set. The Magnia blocks were inspected in the pinned Enderscape JAR as `minecraft:block/cube_all` models; their generated tiles preserve their own `alluring_magnia` or `repulsive_magnia` RGB while the donor provides alpha topology only. The native End Stone source is likewise inspected and hash-pinned from the local Minecraft 26.2 client because it is above target-only Void Shale.

Slabs, stairs, walls, BGE geometry, decorative/chiseled sources, stripped wood, leaves, lamps, Magnia derivatives, Drift Jelly, Shadoline construction, and Purpur-related families are intentionally deferred. No claim about their CTM surface behavior is made by this canary.
