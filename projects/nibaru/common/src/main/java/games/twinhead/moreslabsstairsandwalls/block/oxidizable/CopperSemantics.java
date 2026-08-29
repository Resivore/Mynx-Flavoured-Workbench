package games.twinhead.moreslabsstairsandwalls.block.oxidizable;

import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.Function;

/** Authoritative copper material transitions, independent of the geometry carrying them. */
public final class CopperSemantics {
    private CopperSemantics() {}

    public static Optional<NibaruMaterialProfile> target(NibaruMaterialProfile source,
            MaterialTransition.Type type) {
        return source.transition(type).flatMap(edge -> NibaruMaterialProfiles.fromFamily(edge.target()));
    }

    public static Optional<BlockState> transition(BlockState source, NibaruMaterialProfile profile,
            MaterialTransition.Type type, Function<NibaruMaterialProfile, Optional<Block>> geometryResolver) {
        return target(profile, type).flatMap(geometryResolver)
                .map(block -> block.withPropertiesOf(source));
    }

    public static Optional<Block> nativeGeometry(Block source, NibaruMaterialProfile target) {
        NibaruMaterialProfile current = NibaruMaterialProfiles.fromBlock(source).orElse(null);
        if (current == null) return Optional.empty();
        if (current.nativeSlab().orElse(null) == source) return target.nativeSlab();
        if (current.nativeStair().orElse(null) == source) return target.nativeStair();
        if (current.nativeWall().orElse(null) == source) return target.nativeWall();
        return Optional.empty();
    }

    public static InteractionResult interact(BlockState state, NibaruMaterialProfile profile, ItemStack stack,
            Level level, BlockPos pos, Player player, InteractionHand hand,
            Function<NibaruMaterialProfile, Optional<Block>> geometryResolver) {
        MaterialTransition.Type type;
        if (stack.getItem() instanceof HoneycombItem && !profile.waxed()) type = MaterialTransition.Type.WAXED;
        else if (stack.getItem() instanceof AxeItem) type = profile.waxed()
                ? MaterialTransition.Type.UNWAXED : MaterialTransition.Type.PREVIOUS_OXIDATION;
        else return InteractionResult.TRY_WITH_EMPTY_HAND;

        Optional<BlockState> target = transition(state, profile, type, geometryResolver);
        if (target.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, target.get());
            if (!player.isCreative()) {
                if (stack.getItem() instanceof HoneycombItem) stack.shrink(1);
                else stack.hurtAndBreak(1, player,
                        hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
        } else if (type == MaterialTransition.Type.WAXED) {
            ParticleUtils.spawnParticlesOnBlockFaces(level, pos, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
            level.playSound(player, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (type == MaterialTransition.Type.UNWAXED) {
            ParticleUtils.spawnParticlesOnBlockFaces(level, pos, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
            level.playSound(player, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            ParticleUtils.spawnParticlesOnBlockFaces(level, pos, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
            level.playSound(player, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
}
