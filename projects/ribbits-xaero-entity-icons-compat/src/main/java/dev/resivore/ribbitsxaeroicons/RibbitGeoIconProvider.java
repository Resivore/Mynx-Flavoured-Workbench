package dev.resivore.ribbitsxaeroicons;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.yungnickyoung.minecraft.ribbits.data.RibbitData;
import com.yungnickyoung.minecraft.ribbits.module.DataTicketModule;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;
import xaero.lib.client.graphics.XaeroBufferProvider;

/** Exact Ribbits opt-in provider; no other GeckoLib entity is enabled in Canary 1. */
public final class RibbitGeoIconProvider implements GeoIconProvider {
    public static final String ENTITY_TYPE = "ribbits:ribbit";
    public static final String PROVIDER_ID = "ribbits-gecko-provider-v1";
    public static final String SELECTOR_VERSION = "main-body-direct-cubes-v1";
    public static final String BABY_POLICY = "NORMALIZED_ADULT_GEOMETRY_FIT";
    private static final String RENDERER_CLASS =
            "com.yungnickyoung.minecraft.ribbits.client.render.RibbitRenderer";
    private static final String MODEL_CLASS =
            "com.yungnickyoung.minecraft.ribbits.client.model.RibbitModel";
    private static final String UNRESOLVED = "<unresolved>";

    public RibbitGeoIconProvider() {
    }

    public static boolean owns(String entityType) {
        return ENTITY_TYPE.equals(entityType);
    }

    public static boolean owns(Entity entity) {
        return entity != null && owns(EntityTypeIdentity.of(entity));
    }

    public boolean supports(String entityType, boolean geoRenderer, boolean upstreamHandled) {
        return owns(entityType) && geoRenderer && !upstreamHandled;
    }

    @Override
    public boolean supports(
            Entity entity,
            EntityRenderer<?, ?> renderer,
            EntityRenderState renderState,
            boolean upstreamHandled) {
        if (entity == null || renderer == null || renderState == null) {
            return false;
        }
        String entityType = EntityTypeIdentity.of(entity);
        boolean exactGeoRenderer = renderer instanceof GeoEntityRenderer<?, ?>
                && RENDERER_CLASS.equals(renderer.getClass().getName())
                && renderState instanceof GeoRenderState;
        return supports(entityType, exactGeoRenderer, upstreamHandled);
    }

    @Override
    public CacheIdentity cacheIdentity(
            Entity entity, EntityRenderer<?, ?> renderer, EntityRenderState renderState) {
        try {
            return resolveBasic(entity, renderer, renderState).identity();
        } catch (Throwable failure) {
            GeoIconLog.failure("identity", failure);
            return unresolvedIdentity();
        }
    }

    @Override
    public boolean prerender(
            MinimapElementGraphics graphics,
            EntityRenderer<?, ?> renderer,
            EntityRenderState renderState,
            Entity entity,
            RadarIconCreator.Parameters parameters) {
        if (!(parameters.variant instanceof RibbitCacheVariant variant)) {
            GeoIconLog.failure("variant", "Xaero parameters do not contain the Ribbit cache record");
            return false;
        }

        final Prepared prepared;
        try {
            RenderSystem.assertOnRenderThread();
            prepared = prepare(entity, renderer, renderState);
            if (!variant.identity().equals(prepared.basic().identity())) {
                GeoIconLog.failure("identity-drift",
                        "render-time model or texture differs from the cached variant");
                return false;
            }
        } catch (Throwable failure) {
            GeoIconLog.failure("prepare", failure);
            return false;
        }

        PoseStack pose = graphics.pose();
        XaeroBufferProvider buffers = graphics.getBufferSource();
        RenderType[] activeType = new RenderType[1];
        pose.pushPose();
        return RenderStateGuard.run(
                () -> {
                    Minecraft.getInstance().gameRenderer.lighting()
                            .setupFor(com.mojang.blaze3d.platform.Lighting.Entry.ITEMS_FLAT);
                    prepared.plan().applyFraming(pose, parameters.scale);
                    activeType[0] = prepared.renderType();
                    var consumer = buffers.getBuffer(activeType[0]);
                    for (GeoCube cube : prepared.plan().cubes()) {
                        pose.pushPose();
                        try {
                            cube.render(
                                    pose,
                                    consumer,
                                    LightCoordsUtil.FULL_BRIGHT,
                                    OverlayTexture.NO_OVERLAY,
                                    0xFFFFFFFF);
                        } finally {
                            pose.popPose();
                        }
                    }
                },
                () -> restoreRenderState(buffers, activeType[0], pose),
                failure -> GeoIconLog.failure("draw", failure));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Prepared prepare(
            Entity entity, EntityRenderer<?, ?> renderer, EntityRenderState renderState) {
        BasicResolution basic = resolveBasic(entity, renderer, renderState);
        GeoModel model = basic.model();
        BakedGeoModel baked = model.getBakedModel(basic.modelId());
        require(baked != null && !baked.isMissingno(), "active baked model is missing");
        require(Minecraft.getInstance().getResourceManager()
                        .getResource(basic.textureId()).isPresent(),
                "active texture resource is missing");

        GeoBone[] topLevel = baked.topLevelBones();
        require(topLevel != null, "baked model has no top-level bone array");
        var selection = RibbitHeadSelector.select(Arrays.stream(topLevel)
                        .map(GeoBoneView::new)
                        .toList())
                .orElseThrow(() -> new IllegalArgumentException(
                        "model does not have one exact top-level main/direct body selection"));
        GeoBone main = ((GeoBoneView) selection.main()).bone();
        GeoBone body = ((GeoBoneView) selection.body()).bone();
        require(body instanceof CuboidGeoBone, "direct body is not a cuboid bone");
        CuboidGeoBone cuboidBody = (CuboidGeoBone) body;
        require(cuboidBody.cubes != null && cuboidBody.cubes.length > 0,
                "direct body cube array is empty");
        GeometryPlan plan = GeometryPlan.create(main, cuboidBody, cuboidBody.cubes);

        GeoEntityRenderer geoRenderer = basic.renderer();
        RenderType renderType = geoRenderer.getRenderType(renderState, basic.textureId());
        require(renderType != null, "Ribbit renderer returned no cutout render type");
        return new Prepared(basic, plan, renderType);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BasicResolution resolveBasic(
            Entity entity, EntityRenderer<?, ?> renderer, EntityRenderState renderState) {
        require(entity != null && renderer != null && renderState != null,
                "entity, renderer, or populated render state is absent");
        require(owns(entity), "entity is not ribbits:ribbit");
        require(renderer instanceof GeoEntityRenderer<?, ?>
                        && RENDERER_CLASS.equals(renderer.getClass().getName()),
                "renderer is not the exact supported Ribbit GeoEntityRenderer");
        require(renderState instanceof GeoRenderState,
                "render state does not implement the GeckoLib state contract");
        GeoRenderState geoState = (GeoRenderState) renderState;
        requirePopulatedTickets(geoState);

        GeoEntityRenderer geoRenderer = (GeoEntityRenderer) renderer;
        GeoModel model = geoRenderer.getGeoModel();
        require(model != null && MODEL_CLASS.equals(model.getClass().getName()),
                "renderer does not expose the exact supported Ribbit model");
        Identifier modelId = (Identifier) model.getModelResource(geoState);
        Identifier textureId = (Identifier) model.getTextureResource(geoState);
        require(modelId != null && textureId != null,
                "active Ribbit model or texture resource is absent");

        RibbitData data = geoState.getGeckolibData(DataTicketModule.DT_RIBBIT_DATA);
        require(data != null && data.getProfession() != null
                        && data.getProfession().id() != null,
                "Ribbit profession identity is absent");
        boolean pride = Boolean.TRUE.equals(
                geoState.getGeckolibData(DataTicketModule.DT_IS_PRIDE_RIBBIT));
        CacheIdentity identity = new CacheIdentity(
                ENTITY_TYPE,
                PROVIDER_ID,
                SELECTOR_VERSION,
                modelId.toString(),
                textureId.toString(),
                data.getProfession().id().toString(),
                BABY_POLICY,
                pride,
                ReloadGeneration.current());
        return new BasicResolution(geoRenderer, model, modelId, textureId, identity);
    }

    private static void requirePopulatedTickets(GeoRenderState state) {
        require(state.hasGeckolibData(DataTicketModule.DT_RIBBIT_DATA),
                "Ribbit data ticket is absent");
        require(state.hasGeckolibData(DataTicketModule.DT_PLAYING_INSTRUMENT),
                "instrument-state ticket is absent");
        require(state.hasGeckolibData(DataTicketModule.DT_UMBRELLA_FALLING),
                "umbrella-state ticket is absent");
        require(state.hasGeckolibData(DataTicketModule.DT_IN_RAIN),
                "rain-state ticket is absent");
        require(state.hasGeckolibData(DataTicketModule.DT_IS_PRIDE_RIBBIT),
                "Pride-state ticket is absent");
    }

    private static CacheIdentity unresolvedIdentity() {
        return new CacheIdentity(
                ENTITY_TYPE,
                PROVIDER_ID,
                SELECTOR_VERSION,
                UNRESOLVED,
                UNRESOLVED,
                UNRESOLVED,
                BABY_POLICY,
                false,
                ReloadGeneration.current());
    }

    private static void restoreRenderState(
            XaeroBufferProvider buffers, RenderType renderType, PoseStack pose) throws Throwable {
        Throwable failure = null;
        try {
            if (renderType != null) {
                buffers.endBatch(renderType);
            }
        } catch (Throwable thrown) {
            failure = thrown;
        }
        try {
            pose.popPose();
        } catch (Throwable thrown) {
            if (failure == null) {
                failure = thrown;
            } else {
                failure.addSuppressed(thrown);
            }
        }
        try {
            MinimapRendererHelper.restoreDefaultShaderBlendState();
        } catch (Throwable thrown) {
            if (failure == null) {
                failure = thrown;
            } else {
                failure.addSuppressed(thrown);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private record BasicResolution(
            GeoEntityRenderer<?, ?> renderer,
            GeoModel<?> model,
            Identifier modelId,
            Identifier textureId,
            CacheIdentity identity) {
    }

    private record Prepared(BasicResolution basic, GeometryPlan plan, RenderType renderType) {
    }

    private static final class GeoBoneView implements RibbitHeadSelector.BoneView<GeoCube> {
        private final GeoBone bone;

        private GeoBoneView(GeoBone bone) {
            this.bone = Objects.requireNonNull(bone, "bone");
        }

        GeoBone bone() {
            return bone;
        }

        @Override
        public String name() {
            return bone.name();
        }

        @Override
        public RibbitHeadSelector.BoneView<GeoCube> parent() {
            return bone.parent() == null ? null : new GeoBoneView(bone.parent());
        }

        @Override
        public List<? extends RibbitHeadSelector.BoneView<GeoCube>> children() {
            GeoBone[] children = bone.children();
            if (children == null) {
                return null;
            }
            return Arrays.stream(children).map(GeoBoneView::new).toList();
        }

        @Override
        public List<GeoCube> directCubes() {
            if (!(bone instanceof CuboidGeoBone cuboid) || cuboid.cubes == null) {
                return List.of();
            }
            return Arrays.asList(cuboid.cubes);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof GeoBoneView view && bone == view.bone;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(bone);
        }
    }

    private static final class EntityTypeIdentity {
        private EntityTypeIdentity() {
        }

        static String of(Entity entity) {
            Identifier id = net.minecraft.world.entity.EntityType.getKey(entity.getType());
            return id == null ? "" : id.toString();
        }
    }
}
