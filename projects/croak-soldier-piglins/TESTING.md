# Croak Soldier Piglins verification

Canary 2 is client-only. The build uses Java 25, Gradle 9.5.1, and the Minecraft 26.2 Fabric toolchain. From this directory, run `gradle build`. The off-game bake check reads the Workbench reference by default. Use `-PcroaksJar=<absolute path>` for a different reference location or `-PgeckolibJar=<absolute path>` to compile against a local Fabric 26.2 GeckoLib build.

## Owner-directed visual check

Use the retained C2 private JAR in place of C1 with Fabric 26.2, GeckoLib, and the usual EMF/ETF configuration. The private JAR supplies its own Soldier assets and needs no Croaks JVM argument. Use one Croak Soldier Piglins JAR at a time. For a code-only build, pass `"-DcroakSoldier.referenceJar=<absolute path to the Croaks reference JAR>"`. Resource reload reads that archive without modifying it.

For the first adult in view, look for `[Croak Soldier]` lines for the resource source, renderer initialization, successful adult extraction, and first submission. Record the last stage reached and any subsequent exception alongside the visual observation. Messages distinguish a missing property on the code-only build, a rejected external archive, a missing resource, a missing GeckoLib bake, failed initialization, an unexpected adult renderer or vanilla state, and extraction/submission errors.

- Compare an ordinary adult with the original Soldier, checking front/back orientation, unit scale, foot contact, translucent texture, built-in left-hand sword, and idle breathing. Walk around two piglins in different states to check independent animation progress.
- Observe calm walking, aggressive pursuit, a melee swing, and death. Verify the geometry follows the source clips, the attack overlays locomotion once per swing, and there is no extra vanilla death roll. Vanilla entity removal limits the visible portion of the 1.5-second death clip.
- Compare an adult beside a baby piglin, a brute, and adult/baby zombified piglins. Only the ordinary adult should change. Check EMF/ETF models on other entities before and after a resource reload.
- Check bartering, collisions, damage, and hitboxes. Actual equipped items and armor are outside C2. The Soldier's modeled sword remains visible regardless of inventory.
- Reload resources while adults are visible, then check the code-only build with a missing reference on a separate launch for the expected vanilla fallback. Record exact client mod versions, artifact identity, observations, and any client errors.


## Off-game reproduction

`gradle privateTestJar` builds the ignored local drop-in from the owner-supplied reference. The public code-only JAR is checked separately. The private build includes only the three Soldier resources plus the upstream license notice and credits.

Run `checkProductionReload` with the Gradle properties `geckolibJar`, `emfJar`, `etfJar`, and `fabricApiJar` pointing to the corresponding Fabric 26.2 dependency JARs. Add `-PprivateCandidate=true` for the no-argument mod-resource route, or set `candidateJar` to the retained C1 artifact to reproduce the dispatcher versus direct-renderer control. This fixture transforms production classes and stops before drawing a frame.
