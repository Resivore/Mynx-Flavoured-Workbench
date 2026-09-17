package dev.resivore.slabdecorations;

import dev.resivore.slabdecorations.mixin.GrowingPlantBlockAccessor;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.BigDripleafStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CactusFlowerBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Optional;

/** Resolves exact BGE-owned native horizontal slabs and directional foliage attachments. */
public final class NibaruHorizontalSurface {
    public static final double BOTTOM_OFFSET = -0.5D;
    public static final double CEILING_TOP_OFFSET = 0.5D;

    private NibaruHorizontalSurface() {
    }

    /** Returns an exact native horizontal candidate before projected survival is evaluated. */
    public static Optional<Surface> candidate(BlockState plantState, BlockGetter level, BlockPos plantPos) {
        Attachment attachment = attachment(plantState, level, plantPos).orElse(null);
        if (attachment == null) return Optional.empty();

        BlockPos supportPos = attachment.supportPos();
        BlockState supportState = level.getBlockState(supportPos);
        if (!(supportState.getBlock() instanceof SlabBlock)
                || !supportState.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            return Optional.empty();
        }

        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(supportState.getBlock()).orElse(null);
        Block exactHorizontalSource = profile == null ? null : profile.nativeSlab()
                .orElseGet(() -> profile.effectiveSlabSource().orElse(null));
        if (exactHorizontalSource != supportState.getBlock()) {
            return Optional.empty();
        }

        return Optional.of(new Surface(profile, supportState, supportPos,
                supportState.getValue(BlockStateProperties.SLAB_TYPE), attachment));
    }

    /**
     * Returns a usable attachment only when the complete projected vanilla result can be evaluated.
     * Client render snapshots are paired with their ClientLevel by the model wrapper.
     */
    public static Optional<Surface> supporting(BlockState plantState, BlockGetter level, BlockPos plantPos) {
        Surface surface = candidate(plantState, level, plantPos).orElse(null);
        if (surface == null) return Optional.empty();
        if (!(level instanceof LevelReader reader)
                || !CanonicalSurvivalProjection.evaluate(plantState, reader, plantPos, surface)) {
            return Optional.empty();
        }
        return Optional.of(surface);
    }

    /**
     * Seeds resolution with the immutable state currently being rendered, then uses the owning
     * level for every nonlocal root, anchor, support, and projected-survival read. Render terrain
     * snapshots are intentionally bounded and cannot authoritatively resolve long columns.
     */
    public static Optional<Surface> supporting(
            BlockState plantState,
            BlockGetter blockView,
            LevelReader environment,
            BlockPos plantPos) {
        Surface surface = candidate(plantState, environment, plantPos).orElse(null);
        if (surface == null) return Optional.empty();
        return CanonicalSurvivalProjection.evaluate(plantState, environment, plantPos, surface)
                ? Optional.of(surface)
                : Optional.empty();
    }

    public static double visibleOffset(BlockState plantState, BlockGetter level, BlockPos plantPos) {
        Surface surface = supporting(plantState, level, plantPos).orElse(null);
        return surface == null ? 0.0D : surface.offset();
    }

    public static double visibleOffset(
            BlockState plantState,
            BlockGetter blockView,
            LevelReader environment,
            BlockPos plantPos) {
        Surface surface = supporting(plantState, blockView, environment, plantPos).orElse(null);
        return surface == null ? 0.0D : surface.offset();
    }

    /** Upward-facing surface plane within the support block. */
    public static double surfaceHeight(SlabType type) {
        return type == SlabType.BOTTOM ? 0.5D : 1.0D;
    }

    /** Downward-facing attachment plane within the support block. */
    public static double ceilingHeight(SlabType type) {
        return type == SlabType.TOP ? 0.5D : 0.0D;
    }

    static Optional<Attachment> attachment(BlockState state, BlockGetter level, BlockPos pos) {
        PlantFamilyEligibility.Family family = PlantFamilyEligibility.family(state).orElse(null);
        if (family == null) return Optional.empty();

        return switch (family) {
            case UPWARD_VEGETATION, SURFACE_FOLIAGE -> mossyCarpetAttachment(state, level, pos);
            case DOUBLE_HEIGHT_VEGETATION -> doublePlantAttachment(state, level, pos);
            case DRIPLEAF_COLUMN -> dripleafAttachment(state, level, pos);
            case CEILING_FOLIAGE -> Optional.of(
                    new Attachment(pos, state, AttachmentOrientation.CEILING));
            case UPWARD_GROWING_COLUMN, DOWNWARD_GROWING_COLUMN ->
                    growingPlantAttachment(state, level, pos);
            case SUGAR_CANE_COLUMN -> sugarCaneAttachment(state, level, pos);
            case BAMBOO_COLUMN -> bambooAttachment(state, level, pos);
            case CACTUS_COLUMN -> cactusAttachment(state, level, pos);
            case HANGING_MOSS_COLUMN -> hangingMossAttachment(state, level, pos);
        };
    }

    private static Optional<Attachment> doublePlantAttachment(
            BlockState state,
            BlockGetter level,
            BlockPos pos) {
        if (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) return Optional.empty();
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            return Optional.of(new Attachment(pos, state, AttachmentOrientation.UPWARD));
        }

        BlockPos lowerPos = pos.below();
        BlockState lower = level.getBlockState(lowerPos);
        if (lower.is(state.getBlock())
                && lower.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && lower.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            return Optional.of(new Attachment(lowerPos, lower, AttachmentOrientation.UPWARD));
        }
        return Optional.empty();
    }

    private static Optional<Attachment> mossyCarpetAttachment(
            BlockState state,
            BlockGetter level,
            BlockPos pos) {
        if (!(state.getBlock() instanceof MossyCarpetBlock)) {
            return Optional.of(new Attachment(pos, state, AttachmentOrientation.UPWARD));
        }
        if (!state.hasProperty(MossyCarpetBlock.BASE)) return Optional.empty();
        if (state.getValue(MossyCarpetBlock.BASE)) {
            return Optional.of(new Attachment(pos, state, AttachmentOrientation.UPWARD));
        }

        BlockPos lowerPos = pos.below();
        BlockState lower = level.getBlockState(lowerPos);
        if (lower.getBlock() instanceof MossyCarpetBlock
                && lower.is(state.getBlock())
                && lower.hasProperty(MossyCarpetBlock.BASE)
                && lower.getValue(MossyCarpetBlock.BASE)) {
            return Optional.of(new Attachment(lowerPos, lower, AttachmentOrientation.UPWARD));
        }
        return Optional.empty();
    }

    private static Optional<Attachment> dripleafAttachment(
            BlockState state,
            BlockGetter level,
            BlockPos pos) {
        if (!(state.getBlock() instanceof BigDripleafBlock)
                && !(state.getBlock() instanceof BigDripleafStemBlock)) {
            return Optional.empty();
        }

        BlockPos rootPos = pos;
        BlockState rootState = state;
        while (!level.isOutsideBuildHeight(rootPos.getY() - 1)) {
            BlockPos belowPos = rootPos.below();
            BlockState below = level.getBlockState(belowPos);
            if (!(below.getBlock() instanceof BigDripleafBlock)
                    && !(below.getBlock() instanceof BigDripleafStemBlock)) break;
            rootPos = belowPos;
            rootState = below;
        }
        return Optional.of(new Attachment(rootPos, rootState, AttachmentOrientation.UPWARD));
    }

    private static Optional<Attachment> sugarCaneAttachment(
            BlockState state,
            BlockGetter level,
            BlockPos pos) {
        if (!(state.getBlock() instanceof SugarCaneBlock)) return Optional.empty();
        return upwardColumnAttachment(state, level, pos,
                candidate -> candidate.getBlock() instanceof SugarCaneBlock);
    }

    private static Optional<Attachment> bambooAttachment(
            BlockState state,
            BlockGetter level,
            BlockPos pos) {
        if (!isBamboo(state)) return Optional.empty();
        return upwardColumnAttachment(state, level, pos, NibaruHorizontalSurface::isBamboo);
    }

    private static boolean isBamboo(BlockState state) {
        return state.getBlock() instanceof BambooSaplingBlock
                || state.getBlock() instanceof BambooStalkBlock;
    }

    private static Optional<Attachment> cactusAttachment(
            BlockState state,
            BlockGetter level,
            BlockPos pos) {
        if (!isCactus(state)) return Optional.empty();
        return upwardColumnAttachment(state, level, pos, NibaruHorizontalSurface::isCactus);
    }

    private static boolean isCactus(BlockState state) {
        return state.getBlock() instanceof CactusBlock
                || state.getBlock() instanceof CactusFlowerBlock;
    }

    private static Optional<Attachment> upwardColumnAttachment(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            java.util.function.Predicate<BlockState> member) {
        BlockPos rootPos = pos;
        BlockState rootState = state;
        while (!level.isOutsideBuildHeight(rootPos.getY() - 1)) {
            BlockPos belowPos = rootPos.below();
            BlockState below = level.getBlockState(belowPos);
            if (!member.test(below)) break;
            rootPos = belowPos;
            rootState = below;
        }
        return Optional.of(new Attachment(rootPos, rootState, AttachmentOrientation.UPWARD));
    }

    private static Optional<Attachment> growingPlantAttachment(
            BlockState state,
            BlockGetter level,
            BlockPos pos) {
        GrowingPlantContract contract = growingPlantContract(state).orElse(null);
        if (contract == null) return Optional.empty();

        BlockPos anchorPos = pos;
        BlockState anchorState = state;
        Direction towardAnchor = contract.growthDirection().getOpposite();
        while (!level.isOutsideBuildHeight(anchorPos.relative(towardAnchor).getY())) {
            BlockPos nextPos = anchorPos.relative(towardAnchor);
            BlockState next = level.getBlockState(nextPos);
            GrowingPlantContract nextContract = growingPlantContract(next).orElse(null);
            if (nextContract == null || !contract.equals(nextContract)
                    || !contract.contains(next)) break;
            anchorPos = nextPos;
            anchorState = next;
        }
        AttachmentOrientation orientation = contract.growthDirection() == Direction.UP
                ? AttachmentOrientation.UPWARD
                : AttachmentOrientation.CEILING;
        return Optional.of(new Attachment(anchorPos, anchorState, orientation));
    }

    private static Optional<GrowingPlantContract> growingPlantContract(BlockState state) {
        if (!(state.getBlock() instanceof GrowingPlantBlock)
                || !(state.getBlock() instanceof GrowingPlantBlockAccessor accessor)) {
            return Optional.empty();
        }
        Direction growthDirection = accessor.slabDecorations$getGrowthDirection();
        if (growthDirection != Direction.UP && growthDirection != Direction.DOWN) {
            return Optional.empty();
        }
        Block head = accessor.slabDecorations$invokeGetHeadBlock();
        Block body = accessor.slabDecorations$invokeGetBodyBlock();
        if (head == null || body == null || (!state.is(head) && !state.is(body))) {
            return Optional.empty();
        }
        return Optional.of(new GrowingPlantContract(head, body, growthDirection));
    }

    private static Optional<Attachment> hangingMossAttachment(
            BlockState state,
            BlockGetter level,
            BlockPos pos) {
        if (!(state.getBlock() instanceof HangingMossBlock)) return Optional.empty();
        BlockPos anchorPos = pos;
        BlockState anchorState = state;
        while (!level.isOutsideBuildHeight(anchorPos.getY() + 1)) {
            BlockPos abovePos = anchorPos.above();
            BlockState above = level.getBlockState(abovePos);
            if (!above.is(state.getBlock())) break;
            anchorPos = abovePos;
            anchorState = above;
        }
        return Optional.of(new Attachment(anchorPos, anchorState, AttachmentOrientation.CEILING));
    }

    record Attachment(BlockPos pos, BlockState state, AttachmentOrientation orientation) {
        BlockPos supportPos() {
            return pos.relative(orientation.supportDirection());
        }
    }

    private record GrowingPlantContract(Block head, Block body, Direction growthDirection) {
        boolean contains(BlockState state) {
            return state.is(head) || state.is(body);
        }
    }

    public enum AttachmentOrientation {
        UPWARD(Direction.DOWN),
        CEILING(Direction.UP);

        private final Direction supportDirection;

        AttachmentOrientation(Direction supportDirection) {
            this.supportDirection = supportDirection;
        }

        Direction supportDirection() {
            return supportDirection;
        }

        double height(SlabType type) {
            return this == UPWARD ? surfaceHeight(type) : ceilingHeight(type);
        }

        double offset(SlabType type) {
            if (this == UPWARD) return type == SlabType.BOTTOM ? BOTTOM_OFFSET : 0.0D;
            return type == SlabType.TOP ? CEILING_TOP_OFFSET : 0.0D;
        }
    }

    public record Surface(
            NibaruMaterialProfile profile,
            BlockState supportState,
            BlockPos supportPos,
            SlabType type,
            Attachment attachment) {

        public BlockState canonicalParentState() {
            return profile.canonicalParent().withPropertiesOf(supportState);
        }

        public AttachmentOrientation orientation() {
            return attachment.orientation();
        }

        public double height() {
            return attachment.orientation().height(type);
        }

        public double offset() {
            return attachment.orientation().offset(type);
        }

        public boolean waterlogged() {
            return supportState.hasProperty(BlockStateProperties.WATERLOGGED)
                    && supportState.getValue(BlockStateProperties.WATERLOGGED);
        }
    }
}
