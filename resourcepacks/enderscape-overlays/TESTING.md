# Testing

Canary 3 is `ACTIVE` and has not received Minecraft runtime validation. The static generator checks do not establish a runtime result.

In an authorized dedicated Minecraft 26.2 test environment, enable Enderscape 3.0.2+mc26.2, Continuity, Matcha Overlays v37, and this generated pack. Keep this pack above Matcha Overlays so its Enderscape rules have normal resource-pack precedence. Do not use either protected gameplay profile.

Check the following before recording any runtime observation:

- Celestial Cap beside Murublight Cap: only the Celestial Cap side owns the seam; there is no reciprocal Murublight edge and no doubled generic/corruption edge.
- Nine-material terrain adjacent steps: inspect Celestial Overgrowth/Corrupt Overgrowth, Corrupt Overgrowth/Veiled End Stone, Veiled End Stone/Alluring Magnia, Alluring Magnia/Repulsive Magnia, Repulsive Magnia/Mirestone, Mirestone/Veradite, Veradite/End Stone, and End Stone/Void Shale. The higher entry in `Celestial Overgrowth > Corrupt Overgrowth > Veiled End Stone > Alluring Magnia > Repulsive Magnia > Mirestone > Veradite > End Stone > Void Shale` must own each seam. Keep Celestial grass, Corrupt warped-nylium, and Veiled mycelium transitions top-face-only.
- Magnia topology and Void Shale target behavior: inspect Alluring Magnia and Repulsive Magnia beside lower terrain blocks, including Void Shale. Each Magnia seam should retain its own Enderscape material art with only a Cobblestone-style alpha edge; Void Shale must not place an overlay onto any terrain material.
- Non-adjacent priority checks: inspect Celestial Overgrowth/Void Shale, Veiled End Stone/Mirestone, Alluring Magnia/End Stone, and Repulsive Magnia/Void Shale; the higher-ranked material must remain the sole visible owner.
- Veiled Log, Celestial Stem, and Murublight Stem transition only across side bark/stem faces in each axis, never over end-grain.
- Celestial/Murublight planks and bricks retain their singular high-priority Celestial-to-Murublight transition; generic family rules do not stack or reverse it.
- Mirestone/Veradite construction, Kurodite, End Stone construction, ores, emissive overlays, and Void Lachryma source/target pairs retain their existing Canary 2 behavior.

Stop on missing tiles, black/purple textures, opaque mask backgrounds, visible emitter mis-registration, end-grain overlays, or a generic rule visibly overriding a pair-specific corruption seam. Record only observations actually made against the exact archived filename and SHA-256.
