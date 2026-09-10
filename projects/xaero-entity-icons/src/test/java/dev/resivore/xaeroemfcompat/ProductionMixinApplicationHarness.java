package dev.resivore.xaeroemfcompat;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;

/** Applies the packaged C11 mixins to exact production classes without launching Minecraft. */
public final class ProductionMixinApplicationHarness {
    private ProductionMixinApplicationHarness() {
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 3, "expected Canary 11, Xaero, and EMF paths");
        Path patch = Path.of(args[0]).toRealPath();
        Path xaero = Path.of(args[1]).toRealPath();
        Path emf = Path.of(args[2]).toRealPath();

        ClassLoader loader = new Knot(EnvType.CLIENT).init(new String[0]);
        requireResourceFrom(loader, "xaero_emf_entity_icon_compat.mixins.json", patch);

        requireSingleHandler(loadFrom(loader,
                "xaero.hud.minimap.radar.icon.RadarIconManager", xaero),
                "xaeroEmf$scaleTargetedPresentation");
        requireSingleHandler(loadFrom(loader,
                "xaero.hud.minimap.radar.icon.creator.render.form.model."
                        + "RadarIconModelFormPrerenderer", xaero),
                "xaeroEmf$applyTargetedUpscale");
        requireSingleHandler(loadFrom(loader,
                "xaero.hud.minimap.radar.icon.creator.render.form.model."
                        + "RadarIconModelPrerenderer", xaero),
                "xaeroEmfEntityIconCompat$renderRelocatedHead");
        Class<?> partPrerenderer = loadFrom(loader,
                "xaero.hud.minimap.radar.icon.creator.render.form.model.part."
                        + "RadarIconModelPartPrerenderer", xaero);
        requireSingleHandler(partPrerenderer,
                "xaeroEmfEntityIconCompat$resolveAdapterTrace");
        requireSingleHandler(partPrerenderer,
                "xaeroEmfEntityIconCompat$renderCanonicalFrameAdapter");
        requireSingleHandler(loadFrom(loader,
                "traben.entity_model_features.models.parts.EMFModelPart", emf),
                "xaeroEmfEntityIconCompat$detectModelPart");

        System.out.println("Production Minecraft 26.2 Knot/Mixin application passed for "
                + patch.getFileName());
    }

    private static Class<?> loadFrom(
            ClassLoader loader, String className, Path expectedJar) throws Exception {
        Class<?> type = Class.forName(className, false, loader);
        Path actual = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toRealPath();
        require(actual.equals(expectedJar), className + " loaded from " + actual);
        return type;
    }

    private static void requireSingleHandler(Class<?> type, String name) {
        long count = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().contains(name))
                .filter(method -> !method.getName().contains("lambda$"))
                .count();
        require(count == 1, name + " applied " + count + " times to " + type.getName());
    }

    private static void requireResourceFrom(
            ClassLoader loader, String resourceName, Path expectedJar) throws Exception {
        URL resource = loader.getResource(resourceName);
        require(resource != null && resource.openConnection() instanceof JarURLConnection,
                "missing packaged " + resourceName);
        Path actual = Path.of(((JarURLConnection) resource.openConnection())
                .getJarFileURL().toURI()).toRealPath();
        require(actual.equals(expectedJar), resourceName + " loaded from " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
