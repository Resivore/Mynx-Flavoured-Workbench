package dev.resivore.mynxtrees;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
public final class MynxTreesClient implements ClientModInitializer {
    /** Compensates the raw RU color-provider value for the current Iris/Complementary render pipeline. */
    public static final float SILVER_BIRCH_HUE_BASE = 0.12F;
    public static final float SILVER_BIRCH_VALUE = 0.52F;

    public static int golden(int x, int z) {
        float hue=Mth.sin(x/10.0F+Mth.sin(((float)z+x)/50.0F)*3.0F)/75.0F+SILVER_BIRCH_HUE_BASE;
        return java.awt.Color.HSBtoRGB(hue,0.8F,SILVER_BIRCH_VALUE);
    }
    @Override public void onInitializeClient() {
        SilverBirchBaseModels.register();
        BlockColorRegistry.register(List.of(new BlockTintSource() {
            @Override public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) { return golden(pos.getX(),pos.getZ()); }
            @Override public int color(BlockState state) { return golden(0,0); }
        }), MynxTrees.SILVER_LEAVES);
        BlockColorRegistry.register(List.of(BlockTintSources.constant(-1), BlockTintSources.grass()), MynxTrees.SWEET_VIOLETS);
        ParticleProviderRegistry.getInstance().register(MynxTrees.WISTERIA_PARTICLE, FallingLeavesParticle.CherryProvider::new);
    }
}
