package dev.resivore.xaeroemfcompat;

import net.minecraft.client.model.geom.ModelPart;
import xaero.common.core.XaeroMinimapCore;

import java.util.Objects;

/** Forwards exactly the model-part identity and color Xaero already captures. */
public final class ModelPartDetectionBridge {
    private ModelPartDetectionBridge() {
    }

    public static void forwardToXaero(ModelPart part, int color) {
        forward(part, color, XaeroMinimapCore::onEntityIconsModelPartRenderDetection);
    }

    static <T> void forward(T part, int color, DetectionSink<? super T> sink) {
        Objects.requireNonNull(part, "part");
        Objects.requireNonNull(sink, "sink").detect(part, color);
    }

    @FunctionalInterface
    interface DetectionSink<T> {
        void detect(T part, int color);
    }
}
