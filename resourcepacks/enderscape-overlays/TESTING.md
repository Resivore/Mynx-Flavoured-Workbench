# Testing

Canary 2 is `ACTIVE` and has not received Minecraft runtime validation. The static generator checks do not establish a runtime result.

In an authorized dedicated Minecraft 26.2 test environment, enable Enderscape 3.0.2+mc26.2, Continuity, Matcha Overlays v37, and this generated pack. Keep this pack above Matcha Overlays so its Enderscape rules have normal resource-pack precedence. Do not use either protected gameplay profile.

Check the following before recording any runtime observation:

- Celestial Cap beside Murublight Cap: only the Celestial Cap side owns the seam; there is no reciprocal Murublight edge and no doubled generic/corruption edge.
- Natural stone chain: inspect Void Shale/Mirestone, Mirestone/Veradite, and Veradite/End Stone. The visible seam owner must descend as `Void Shale > Mirestone > Veradite > End Stone`; Void Shale should preserve its lateral versus end-face texture treatment and registered emissive pixels.
- Seven-material terrain adjacent steps: inspect Void Shale/Celestial Overgrowth, Celestial Overgrowth/Corrupt Overgrowth, Corrupt Overgrowth/Veiled End Stone, Veiled End Stone/Mirestone, Mirestone/Veradite, and Veradite/End Stone. The higher entry in `Void Shale > Celestial Overgrowth > Corrupt Overgrowth > Veiled End Stone > Mirestone > Veradite > End Stone` must own each seam. Keep Celestial grass, Corrupt warped-nylium, and Veiled mycelium transitions top-face-only.
- Non-adjacent priority checks: inspect Void Shale/End Stone, Celestial Overgrowth/Veradite, and Corrupt Overgrowth/End Stone; the higher-ranked material must remain the sole visible owner.
- Veiled Log, Celestial Stem, and Murublight Stem transition only across side bark/stem faces in each axis, never over end-grain.
- Celestial/Murublight planks and bricks retain their singular high-priority Celestial-to-Murublight transition; generic family rules do not stack or reverse it.
- Mirestone/Veradite construction, Kurodite, End Stone construction, ores, emissive overlays, and Void Lachryma source/target pairs retain their existing Canary 1 behavior.

Stop on missing tiles, black/purple textures, opaque mask backgrounds, visible emitter mis-registration, end-grain overlays, or a generic rule visibly overriding a pair-specific corruption seam. Record only observations actually made against the exact archived filename and SHA-256.
