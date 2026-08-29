package dev.resivore.mapmarkerextension.compat.worldmap;

import dev.resivore.mapmarkerextension.client.MapDecorationSprite;
import dev.resivore.mapmarkerextension.client.MapMarkerTargetRepository;
import dev.resivore.mapmarkerextension.core.MapMarkerTarget;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.lib.client.graphics.util.ImmediateRenderUtil;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

public final class XaeroWorldMapMarkerRenderer extends ElementRenderer<
    MapMarkerTarget,
    XaeroWorldMapMarkerRenderer.Context,
    XaeroWorldMapMarkerRenderer
> {
    public XaeroWorldMapMarkerRenderer(MapMarkerTargetRepository targets) {
        super(new Context(), new Provider(targets), new Reader());
    }

    @Override
    public void preRender(
        ElementRenderInfo renderInfo,
        XaeroBufferProvider buffers,
        MultiTextureRenderTypeRendererProvider rendererProvider,
        boolean shadow
    ) {
    }

    @Override
    public void postRender(
        ElementRenderInfo renderInfo,
        XaeroBufferProvider buffers,
        MultiTextureRenderTypeRendererProvider rendererProvider,
        boolean shadow
    ) {
    }

    @Override
    public void renderElementShadow(
        MapMarkerTarget target,
        boolean highlighted,
        float optionalScale,
        double partialX,
        double partialY,
        ElementRenderInfo renderInfo,
        MapElementGraphics graphics,
        XaeroBufferProvider buffers,
        MultiTextureRenderTypeRendererProvider rendererProvider
    ) {
    }

    @Override
    public boolean renderElement(
        MapMarkerTarget target,
        boolean highlighted,
        double optionalDepth,
        float optionalScale,
        double partialX,
        double partialY,
        ElementRenderInfo renderInfo,
        MapElementGraphics graphics,
        XaeroBufferProvider buffers,
        MultiTextureRenderTypeRendererProvider rendererProvider
    ) {
        if (renderInfo.mapDimension == null
            || !target.dimensionId().equals(renderInfo.mapDimension.identifier().toString())) {
            return false;
        }
        graphics.pose().translate(partialX, partialY, optionalDepth);
        graphics.pose().scale(optionalScale * 2.0F, optionalScale * 2.0F, 1.0F);
        blitVerticallyCorrectedSprite(
            graphics,
            MapDecorationSprite.get(target.decorationAssetId())
        );
        return true;
    }

    private static void blitVerticallyCorrectedSprite(
        MapElementGraphics graphics,
        TextureAtlasSprite sprite
    ) {
        AbstractTexture atlasTexture = Minecraft.getInstance()
            .getTextureManager()
            .getTexture(sprite.atlasLocation());
        if (atlasTexture == null) {
            return;
        }

        graphics.flush();
        ImmediateRenderUtil.texturedRect(
            graphics.pose(),
            -4.0F,
            -4.0F,
            sprite.getU0(),
            sprite.getV1(),
            8.0F,
            8.0F,
            sprite.getU1(),
            sprite.getV0(),
            1.0F,
            RenderPipelines.GUI_TEXTURED,
            new PreparedRenderType.Texture(
                "Sampler0",
                atlasTexture.getTextureView(),
                atlasTexture.getSampler()
            )
        );
    }

    @Override
    public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
        return location == ElementRenderLocation.WORLD_MAP && !shadow;
    }

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public boolean shouldBeDimScaled() {
        return false;
    }

    static final class Context {
        private Iterator<MapMarkerTarget> iterator = java.util.Collections.emptyIterator();
    }

    private static final class Provider extends ElementRenderProvider<MapMarkerTarget, Context> {
        private final MapMarkerTargetRepository targets;

        private Provider(MapMarkerTargetRepository targets) {
            this.targets = targets;
        }

        @Override
        public void begin(ElementRenderLocation location, Context context) {
            context.iterator = targets.snapshot().iterator();
        }

        @Override
        public boolean hasNext(ElementRenderLocation location, Context context) {
            return context.iterator.hasNext();
        }

        @Override
        public MapMarkerTarget getNext(ElementRenderLocation location, Context context) {
            return context.iterator.next();
        }

        @Override
        public void end(ElementRenderLocation location, Context context) {
            context.iterator = java.util.Collections.emptyIterator();
        }
    }

    private static final class Reader
        extends ElementReader<MapMarkerTarget, Context, XaeroWorldMapMarkerRenderer> {

        @Override public boolean isHidden(MapMarkerTarget target, Context context) { return false; }
        @Override public double getRenderX(MapMarkerTarget target, Context context, float partialTicks) { return target.renderX(); }
        @Override public double getRenderZ(MapMarkerTarget target, Context context, float partialTicks) { return target.renderZ(); }
        @Override public int getInteractionBoxLeft(MapMarkerTarget target, Context context, float partialTicks) { return -4; }
        @Override public int getInteractionBoxRight(MapMarkerTarget target, Context context, float partialTicks) { return 4; }
        @Override public int getInteractionBoxTop(MapMarkerTarget target, Context context, float partialTicks) { return -4; }
        @Override public int getInteractionBoxBottom(MapMarkerTarget target, Context context, float partialTicks) { return 4; }
        @Override public int getRenderBoxLeft(MapMarkerTarget target, Context context, float partialTicks) { return -4; }
        @Override public int getRenderBoxRight(MapMarkerTarget target, Context context, float partialTicks) { return 4; }
        @Override public int getRenderBoxTop(MapMarkerTarget target, Context context, float partialTicks) { return -4; }
        @Override public int getRenderBoxBottom(MapMarkerTarget target, Context context, float partialTicks) { return 4; }
        @Override public int getLeftSideLength(MapMarkerTarget target, Minecraft minecraft) { return 0; }
        @Override public String getMenuName(MapMarkerTarget target) { return ""; }
        @Override public String getFilterName(MapMarkerTarget target) { return ""; }
        @Override public int getMenuTextFillLeftPadding(MapMarkerTarget target) { return 0; }
        @Override public int getRightClickTitleBackgroundColor(MapMarkerTarget target) { return 0; }
        @Override public boolean shouldScaleBoxWithOptionalScale() { return true; }
    }
}
