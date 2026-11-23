package dev.resivore.croak;

import com.geckolib.cache.GeckoLibResources;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import net.fabricmc.loader.api.FabricLoader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.piglin.*;
import net.minecraft.world.level.Level;
import sun.misc.Unsafe;

/** No world, window, or GPU. Stand-ins stop the real factory at its selected extraction path. */
public final class DispatchFixture {
    private static final class Selected extends Error {
        final boolean soldier;
        Selected(boolean soldier) { this.soldier = soldier; }
    }
    private static class FixturePiglin extends Piglin {
        boolean baby;
        FixturePiglin(EntityType<? extends Piglin> type, Level level) { super(type, level); }
        @Override public EntityType<?> getType() { return EntityTypes.PIGLIN; }
        @Override public boolean isBaby() { return baby; }
        @Override public boolean isDeadOrDying() { throw new Selected(true); }
    }
    private static class FixtureRenderer extends PiglinRenderer {
        FixtureRenderer(EntityRendererProvider.Context c) { super(c, null, null, null, null); }
        @Override public PiglinRenderState createRenderState() { throw new Selected(false); }
    }

    private static <T> T bare(Class<T> type) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return type.cast(((Unsafe)f.get(null)).allocateInstance(type));
    }

    public static void verify(String bundled) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var packs = new ArrayList<PackResources>();
        if (!bundled.isEmpty()) {
            System.clearProperty("croakSoldier.referenceJar");
            // Ask Fabric for its normal always-enabled mod resource packs.
            var method = Class.forName("net.fabricmc.fabric.impl.resource.pack.ModPackResourcesUtil")
                .getMethod("getModResourcePacks", FabricLoader.class, PackType.class, String.class);
            for (Object entry : (List<?>)method.invoke(null, FabricLoader.getInstance(), PackType.CLIENT_RESOURCES, null)) {
                var pack = (PackResources)entry;
                if (pack.getResource(PackType.CLIENT_RESOURCES, SoldierPack.MODEL) != null) packs.add(pack);
            }
        }
        try (var manager = new ReloadableResourceManager(PackType.CLIENT_RESOURCES)) {
            manager.registerReloadListener(new GeckoLibResources());
            manager.createReload(Runnable::run, Runnable::run, CompletableFuture.completedFuture(null), packs).done().join();
            var source = manager.getResource(SoldierPack.MODEL).orElseThrow().source();
            if ((source instanceof SoldierPack) != bundled.isEmpty()) throw new AssertionError("wrong source " + source);
            var context = bare(EntityRendererProvider.Context.class);
            Field rm = EntityRendererProvider.Context.class.getDeclaredField("resourceManager");
            rm.setAccessible(true);
            rm.set(context, manager);
            var renderer = bare(FixtureRenderer.class);
            var piglin = bare(FixturePiglin.class);
            if (FabricLoader.getInstance().getModContainer("croak_soldier_piglins").orElseThrow()
                    .getMetadata().getVersion().getFriendlyString().endsWith("canary1")) {
                verifyC1(context, renderer, piglin);
                return;
            }
            if (!SoldierBinding.resourcesReady(manager)) throw new AssertionError("missing Soldier resources");
            // Invoke the transformed constructor callback; the GPU-backed model set is outside this fixture.
            var setup = Arrays.stream(PiglinRenderer.class.getDeclaredMethods())
                .filter(m -> m.getName().contains("croak$setup")).findFirst().orElseThrow();
            setup.setAccessible(true);
            setup.invoke(renderer, context, null, null, null, null, null);
            var visual = ((SoldierBinding.Owner)renderer).croak$binding().visual();
            if (visual == null) throw new AssertionError("renderer initialization failed");
            expect(renderer, piglin, true);
            piglin.baby = true;
            expect(renderer, piglin, false);
            expect(renderer, bare(PiglinBrute.class), false);
            if (SoldierBinding.Owner.class.isAssignableFrom(ZombifiedPiglinRenderer.class))
                throw new AssertionError("zombified renderer captured");
            // Stop inside the selected render pass, before any GPU or client singleton access.
            var state = visual.createRenderState(visual.getAnimatable(), piglin);
            try {
                renderer.submit(state, new PoseStack() {
                    @Override public void pushPose() {
                        boolean inGeckoPass = StackWalker.getInstance().walk(frames ->
                            frames.anyMatch(frame -> frame.getClassName().equals("com.geckolib.renderer.base.GeoRenderer")
                                && frame.getMethodName().equals("performRenderPass")));
                        throw new Selected(inGeckoPass);
                    }
                }, null, null);
                throw new AssertionError("submission did not enter a render pass");
            } catch (Selected selected) {
                if (!selected.soldier) throw new AssertionError("Soldier state entered vanilla submission");
            }
            System.out.println("Verified transformed reload, renderer creation, adult extraction routing and controls: " + source.getClass());
        }
    }

    // Kept as a control for the owner's failed release. A direct renderer consumer
    // and the standard dispatcher lookup are distinct paths in C1.
    private static void verifyC1(EntityRendererProvider.Context context, PiglinRenderer registered,
                                 FixturePiglin piglin) throws Exception {
        var setup = Arrays.stream(EntityRenderers.class.getDeclaredMethods())
            .filter(m -> m.getName().contains("croak$reload")).findFirst().orElseThrow();
        setup.setAccessible(true);
        setup.invoke(null, context, null);
        var dispatcher = bare(EntityRenderDispatcher.class);
        var map = EntityRenderDispatcher.class.getDeclaredField("renderers");
        map.setAccessible(true);
        map.set(dispatcher, Map.of(EntityTypes.PIGLIN, registered));
        if (!(dispatcher.getRenderer(piglin) instanceof SoldierRenderer))
            throw new AssertionError("C1 standard adult dispatch failed");
        expect(registered, piglin, false);
        System.out.println("C1 initialized; standard dispatcher selects Soldier, direct registered renderer selects vanilla");
    }

    private static void expect(PiglinRenderer renderer, AbstractPiglin entity, boolean soldier) {
        try {
            renderer.createRenderState(entity, 0);
            throw new AssertionError("fixture did not reach either extractor");
        } catch (Selected selected) {
            if (selected.soldier != soldier) throw new AssertionError("incorrect renderer path");
        }
    }
}
