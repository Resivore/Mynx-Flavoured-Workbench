# Testing

This first canary is `ACTIVE` and has not received Minecraft runtime validation. The static generator checks do not establish a runtime result.

In an authorized dedicated Minecraft 26.2 test environment, enable Enderscape 3.0.2+mc26.2, Continuity, Matcha Overlays v37, and this generated pack. Keep this pack above Matcha Overlays so its Enderscape rules have normal resource-pack precedence. Do not use either protected gameplay profile.

Check the following before recording any runtime observation:

- Celestial Overgrowth, Veiled End Stone, and Corrupt Overgrowth transition onto their verified End Stone or Mirestone substrates; Celestial and Corrupt Paths act only as receivers.
- Veiled Log, Celestial Stem, and Murublight Stem transition only across side bark/stem faces in each axis, never over end-grain.
- Veiled, Celestial, and Murublight planks join with one another; the Celestial-to-Murublight seam is visually distinct rather than a stack of generic overlays.
- Mirestone, Veradite, Kurodite, End Stone construction, Celestial/Murublight brick, and cube-cap families retain their decorative target faces while source transitions align at adjacency seams.
- Nebulite and Mirestone Nebulite Ore keep visible and emissive edge pixels registered on End Stone and Mirestone respectively. Also inspect both Shadoline ore substrates.
- Void Lachryma source/target pairs for Veradite/Kurodite and Celestial/Murublight show the high-priority pair transition for the implemented base, brick, plank, stem, and cap pairs.

Stop on missing tiles, black/purple textures, opaque mask backgrounds, visible emitter mis-registration, end-grain overlays, or a generic rule visibly overriding a pair-specific corruption seam. Record only observations actually made against the exact archived filename and SHA-256.
