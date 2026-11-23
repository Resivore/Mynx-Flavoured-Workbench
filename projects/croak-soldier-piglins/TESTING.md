# Croak Soldier Piglins verification

Canary 1 is client-only. The build uses Java 25, Gradle 9.5.1, and the Minecraft 26.2 Fabric toolchain. From this directory, run `gradle build`. The off-game bake check reads the Workbench reference by default. Use `-PcroaksJar=<absolute path>` for a different reference location or `-PgeckolibJar=<absolute path>` to compile against a local Fabric 26.2 GeckoLib build.

## Owner-directed visual check

Use the client JAR with Fabric 26.2, GeckoLib, and the owner's usual EMF/ETF configuration. Pass one JVM argument pointing to the existing archive, for example `"-DcroakSoldier.referenceJar=C:\References\croaks-2.0.0-neoforge-1.21.1.jar"`. The archive is opened read-only on client resource reload. An absent setting, unreadable file, or mismatching Soldier resource should leave piglins on their normal renderer.

- Compare an ordinary adult with the original Soldier, checking front/back orientation, unit scale, foot contact, translucent texture, built-in left-hand sword, and idle breathing. Walk around two piglins in different states to check independent animation progress.
- Observe calm walking, aggressive pursuit, a melee swing, and death. Verify the geometry follows the source clips, the attack overlays locomotion once per swing, and there is no extra vanilla death roll. Vanilla entity removal limits the visible portion of the 1.5-second death clip.
- Compare an adult beside a baby piglin, a brute, and adult/baby zombified piglins. Only the ordinary adult should change. Check EMF/ETF models on other entities before and after a resource reload.
- Check bartering, collisions, damage, and hitboxes. Actual equipped items and armor are outside C1. The Soldier's modeled sword remains visible regardless of inventory.
- Reload resources while adults are visible, then repeat with a missing reference on a separate launch. Record exact client mod versions, artifact identity, observations, and any client errors.
