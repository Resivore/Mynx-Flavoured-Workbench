package dev.resivore.mapmarkerextension.compat.minimap;

import dev.resivore.mapmarkerextension.client.MapDecorationSprite;
import dev.resivore.mapmarkerextension.client.MapMarkerTargetRepository;
import dev.resivore.mapmarkerextension.core.MapMarkerTarget;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.lib.client.graphics.XaeroBufferProvider;

public final class XaeroMinimapMarkerRenderer
    extends MinimapElementRenderer<MapMarkerTarget, XaeroMinimapMarkerRenderer.Context> {

    public XaeroMinimapMarkerRenderer(MapMarkerTargetRepository targets) {
        super(new Reader(), new Provider(targets), new Context());
    }

    @Override
    public boolean renderElement(
        MapMarkerTarget target,
        boolean highlighted,
        boolean outOfBounds,
        double optionalDepth,
        float optionalScale,
        double partialX,
        double partialY,
        MinimapElementRenderInfo renderInfo,
        MinimapElementGraphics graphics,
        XaeroBufferProvider buffers
    ) {
        if (outOfBounds || renderInfo.mapDimension == null
            || !target.dimensionId().equals(renderInfo.mapDimension.identifier().toString())) {
            return false;
        }
        graphics.pose().translate(partialX, partialY, optionalDepth);
        graphics.pose().scale(optionalScale, optionalScale, 1.0F);
        graphics.blit(
            MapDecorationSprite.get(target.decorationAssetId()),
            -4,
            -4,
            8,
            8,
            RenderPipelines.GUI_TEXTURED
        );
        return true;
    }

    @Override
    public void preRender(
        MinimapElementRenderInfo renderInfo,
        XaeroBufferProvider buffers,
        MultiTextureRenderTypeRendererProvider multiTextureRenderers
    ) {
    }

    @Override
    public void postRender(
        MinimapElementRenderInfo renderInfo,
        XaeroBufferProvider buffers,
        MultiTextureRenderTypeRendererProvider multiTextureRenderers
    ) {
    }

    @Override
    public boolean shouldRender(MinimapElementRenderLocation location) {
        return location == MinimapElementRenderLocation.OVER_MINIMAP;
    }

    @Override
    public int getOrder() {
        return 300;
    }

    static final class Context {
        private Iterator<MapMarkerTarget> iterator = java.util.Collections.emptyIterator();
    }

    private static final class Provider
        extends MinimapElementRenderProvider<MapMarkerTarget, Context> {
        private final MapMarkerTargetRepository targets;

        private Provider(MapMarkerTargetRepository targets) {
            this.targets = targets;
        }

        @Override
        public void begin(MinimapElementRenderLocation location, Context context) {
            context.iterator = targets.snapshot().iterator();
        }

        @Override
        public boolean hasNext(MinimapElementRenderLocation location, Context context) {
            return context.iterator.hasNext();
        }

        @Override
        public MapMarkerTarget getNext(MinimapElementRenderLocation location, Context context) {
            return context.iterator.next();
        }

        @Override
        public void end(MinimapElementRenderLocation location, Context context) {
            context.iterator = java.util.Collections.emptyIterator();
        }
    }

    private static final class Reader extends MinimapElementReader<MapMarkerTarget, Context> {
        @Override public boolean isHidden(MapMarkerTarget target, Context context) { return false; }
        @Override public double getRenderX(MapMarkerTarget target, Context context, float partialTicks) { return target.renderX(); }
        @Override public double getRenderY(MapMarkerTarget target, Context context, float partialTicks) { return 64.0D; }
        @Override public double getRenderZ(MapMarkerTarget target, Context context, float partialTicks) { return target.renderZ(); }

        @Override
        public double getCoordinateScale(
            MapMarkerTarget target,
            Context context,
            MinimapElementRenderInfo renderInfo
        ) {
            return renderInfo.backgroundCoordinateScale;
        }

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
