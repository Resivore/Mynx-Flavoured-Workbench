package dev.resivore.croak;

import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Run the fixture inside the same transformed class loader as the packaged candidate. */
public final class ProductionReloadCheck {
    public static void main(String[] args) throws Exception {
        Knot knot = new Knot(EnvType.CLIENT);
        ClassLoader loader = knot.init(new String[0]);
        knot.addToClassPath(Path.of(ProductionReloadCheck.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        Class.forName("dev.resivore.croak.DispatchFixture", true, loader)
            .getMethod("verify", String.class).invoke(null, args[0]);
    }
}
