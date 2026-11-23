package dev.resivore.croak;

import com.geckolib.animation.object.EasingType;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.loading.math.MathParser;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import java.util.*;

/** Off-game resource enumeration and bake using the actual GeckoLib loader. */
public final class SoldierBakeCheck {
    public static void main(String[] args) {
        var pack = SoldierPack.load().orElseThrow();
        try (var resources = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(pack))) {
            var loader = new GeckoLibGsonLoader();
            var models = resources.listResources("geckolib/models", id -> true);
            var clips = resources.listResources("geckolib/animations", id -> true);
            var geometry = loader.deserializeGeckoLibModelFile(SoldierPack.MODEL, models.get(SoldierPack.MODEL));
            var model = loader.bakeGeckoLibModelFile(SoldierPack.MODEL, geometry);
            var requiredBones = Set.of("rightleg", "leftleg", "rightarm", "leftarm", "sword", "body");
            if (!model.boneLookup().get().keySet().equals(requiredBones)) throw new AssertionError("Soldier skeleton");
            var animation = loader.deserializeGeckoLibAnimationFile(SoldierPack.ANIMATION, clips.get(SoldierPack.ANIMATION));
            var baked = loader.bakeGeckoLibAnimationsFile(SoldierPack.ANIMATION, animation, MathParser.create());
            var tracks = Map.of("idle", 3, "walk", 5, "agro", 6, "attack", 4, "death", 5);
            tracks.forEach((name, count) -> {
                if (baked.getAnimation(name).boneAnimations().length != count) throw new AssertionError(name);
            });
            var body = Arrays.stream(baked.getAnimation("idle").boneAnimations())
                .filter(bone -> bone.boneName().equals("body")).findFirst().orElseThrow();
            var curves = Arrays.stream(body.positionKeyFrames().yKeyframes())
                .map(frame -> frame.easingType()).toList();
            if (!curves.containsAll(List.of(EasingType.EASE_IN_SINE, EasingType.EASE_OUT_SINE)))
                throw new AssertionError("Original idle easing lost: " + curves);
            resources.getResource(SoldierPack.TEXTURE).orElseThrow();
        }
    }
}
