package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** A bounded server-side overlay that runs only during vanilla WORK activity. */
public final class WorkCoordinator {
    private static final Map<Villager, State> STATES = new WeakHashMap<>();
    private static final int SCAN_INTERVAL = 60;
    private static final int SHEEP_RADIUS = 12;
    private static final int FISH_RADIUS = 8;

    private WorkCoordinator() {}

    public static void tick(Villager villager, ServerLevel level) {
        ResourceKey<VillagerProfession> profession = villager.getVillagerData().profession().unwrapKey().orElse(null);
        Profile profile = profile(profession);
        State state = STATES.get(villager);
        if (profile == null) { if (state != null) cancel(villager, state); return; }
        if (state == null) { state = new State(); STATES.put(villager, state); }
        BlockPos site = claimedSite(villager, level, profile);
        if (site == null || villager.isBaby() || !villager.isAlive() || villager.isTrading() || villager.isSleeping()
                || !villager.getBrain().isActive(Activity.WORK)
                || villager.getBrain().hasMemoryValue(MemoryModuleType.INTERACTION_TARGET)
                || villager.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)) {
            cancel(villager, state);
            return;
        }
        if (profession == VillagerProfession.SHEPHERD) shepherd(villager, level, site, state);
        else if (profession == VillagerProfession.FISHERMAN) fisherman(villager, level, site, state);
        else ambient(villager, site, state);
    }

    private static BlockPos claimedSite(Villager villager, ServerLevel level, Profile profile) {
        if (profile == null) return null;
        GlobalPos claim = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null);
        if (claim == null || !claim.dimension().equals(level.dimension())) return null;
        BlockPos site = claim.pos();
        if (site.distSqr(villager.blockPosition()) > 24 * 24 || !level.hasChunkAt(site)) return null;
        if (!level.getBlockState(site).is(profile.block)) return null;
        if (!level.getPoiManager().getType(site).map(type -> type.is(profile.poi)).orElse(false)) return null;
        return site;
    }

    private static void shepherd(Villager villager, ServerLevel level, BlockPos loom, State state) {
        SimpleContainer owned = ((OwnedOutput)villager).villagerWork$ownedOutput();
        if (villager.tickCount % 20 == 0 && containsWool(owned)) depositWool(villager, level, loom, owned);
        if (containsWool(owned) && villager.distanceToSqr(Vec3.atCenterOf(loom)) > 4 * 4) {
            state.sheep = null;
            clearProp(villager);
            if (villager.tickCount % 20 == 0) moveNearSite(villager, level, loom, 0.6);
            return;
        }
        Sheep target = state.sheep;
        if (target != null && (!target.isAlive() || !target.readyForShearing()
                || target.distanceToSqr(Vec3.atCenterOf(loom)) > 16 * 16 || target.distanceToSqr(villager) > 18 * 18)) {
            state.sheep = null;
            target = null;
            clearProp(villager);
        }
        if (target == null && villager.tickCount >= state.cooldown
                && (villager.tickCount + villager.getId()) % SCAN_INTERVAL == 0) {
            List<Sheep> sheep = level.getEntitiesOfClass(Sheep.class,
                    new AABB(loom).inflate(SHEEP_RADIUS, 4, SHEEP_RADIUS),
                    animal -> animal.isAlive() && animal.readyForShearing() && !animal.isBaby()
                            && animal.distanceToSqr(villager) <= 18 * 18);
            sheep.sort(Comparator.comparingDouble(villager::distanceToSqr));
            for (Sheep candidate : sheep) {
                if (!OutputStorage.fits(owned, new ItemStack(woolFor(candidate.getColor())), 3)) continue;
                Path path = villager.getNavigation().createPath(candidate, 0);
                if (path != null && path.canReach()) { state.sheep = candidate; target = candidate; break; }
            }
        }
        if (target == null) { clearProp(villager); return; }
        if (!OutputStorage.fits(owned, new ItemStack(woolFor(target.getColor())), 3)) {
            state.sheep = null;
            clearProp(villager);
            return;
        }
        showProp(villager, state, Items.SHEARS);
        villager.getLookControl().setLookAt(target);
        if (villager.distanceToSqr(target) > 2.5 * 2.5) {
            if (villager.tickCount % 20 == 0) villager.getNavigation().moveTo(target, 0.6);
            return;
        }
        villager.getNavigation().stop();
        villager.swing(InteractionHand.MAIN_HAND);
        if (target.readyForShearing()) ShearCapture.shear(target, level, owned);
        state.sheep = null;
        state.cooldown = villager.tickCount + 60;
        clearProp(villager);
    }

    private static void depositWool(Villager villager, ServerLevel level, BlockPos loom, SimpleContainer owned) {
        if (villager.distanceToSqr(Vec3.atCenterOf(loom)) > 4 * 4) return;
        List<BarrelBlockEntity> barrels = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos pos = loom.relative(direction);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(Blocks.BARREL)) continue;
            if (!(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) || barrel.getLootTable() != null) continue;
            barrels.add(barrel);
        }
        // Ordered directions make ties deterministic; matching stacks win before any empty slot.
        for (int i = 0; i < owned.getContainerSize(); i++) {
            ItemStack stack = owned.getItem(i);
            if (stack.isEmpty() || !stack.is(net.minecraft.tags.ItemTags.WOOL)) continue;
            int moved = OutputStorage.insertAcross(barrels, stack, stack.getCount());
            if (moved > 0) owned.removeItem(i, moved);
        }
    }

    private static void fisherman(Villager villager, ServerLevel level, BlockPos site, State state) {
        SimpleContainer owned = ((OwnedOutput)villager).villagerWork$ownedOutput();
        if (villager.tickCount % 20 == 0 && villager.distanceToSqr(Vec3.atCenterOf(site)) <= 4 * 4) {
            BarrelBlockEntity barrel = claimedBarrel(level, site);
            if (barrel != null) deposit(owned, barrel, false);
        }
        if (state.floatEntity != null) {
            if (state.floatEntity.isRemoved() || state.water == null || !level.hasChunkAt(state.water)
                    || !level.getFluidState(state.water).is(FluidTags.WATER)) { cancel(villager, state); return; }
            showProp(villager, state, Items.FISHING_ROD);
            villager.getLookControl().setLookAt(Vec3.atCenterOf(state.water));
            if (villager.tickCount >= state.catchAt) {
                villager.swing(InteractionHand.MAIN_HAND);
                level.sendParticles(ParticleTypes.SPLASH, state.floatEntity.getX(), state.floatEntity.getY(),
                        state.floatEntity.getZ(), 8, 0.15, 0.05, 0.15, 0.03);
                level.playSound(null, state.floatEntity.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH,
                        SoundSource.NEUTRAL, 0.6f, 1.0f);
                state.floatEntity.discard();
                state.floatEntity = null;
                ItemStack caught = rollFish(level, state.water, villager);
                if (!caught.isEmpty() && isFish(caught) && OutputStorage.fits(owned, caught, caught.getCount()))
                    OutputStorage.insert(owned, caught, caught.getCount());
                state.water = null;
                state.bank = null;
                state.cooldown = villager.tickCount + 100;
                clearProp(villager);
            }
            return;
        }
        if (containsFish(owned)) {
            if (villager.distanceToSqr(Vec3.atCenterOf(site)) > 3 * 3 && villager.tickCount % 20 == 0)
                moveNearSite(villager, level, site, 0.6);
            return;
        }
        if (villager.tickCount < state.cooldown) return;
        if (!canHoldAnyFish(owned)) return;
        if (state.bank == null && (villager.tickCount + villager.getId()) % SCAN_INTERVAL == 0)
            locateWater(villager, level, site, state);
        if (state.bank == null) return;
        if (villager.distanceToSqr(Vec3.atCenterOf(state.bank)) > 1.8 * 1.8) {
            if (villager.tickCount % 20 == 0)
                villager.getNavigation().moveTo(state.bank.getX() + 0.5, state.bank.getY(), state.bank.getZ() + 0.5, 0.6);
            return;
        }
        if (!level.hasChunkAt(state.water) || !level.getFluidState(state.water).is(FluidTags.WATER)) { state.bank = null; state.water = null; return; }
        villager.getNavigation().stop();
        villager.getLookControl().setLookAt(Vec3.atCenterOf(state.water));
        showProp(villager, state, Items.FISHING_ROD);
        villager.swing(InteractionHand.MAIN_HAND);
        FishingFloat bobber = new FishingFloat(level, villager, state.water);
        if (level.addFreshEntity(bobber)) {
            state.floatEntity = bobber;
            state.catchAt = villager.tickCount + 120 + villager.getRandom().nextInt(180);
            level.playSound(null, villager.blockPosition(), SoundEvents.FISHING_BOBBER_THROW,
                    SoundSource.NEUTRAL, 0.5f, 1.0f);
        } else { clearProp(villager); state.bank = null; state.water = null; }
    }

    private static void locateWater(Villager villager, ServerLevel level, BlockPos site, State state) {
        List<Bank> banks = new ArrayList<>();
        for (int dx = -FISH_RADIUS; dx <= FISH_RADIUS; dx++)
            for (int dz = -FISH_RADIUS; dz <= FISH_RADIUS; dz++)
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos water = site.offset(dx, dy, dz);
                    if (!level.hasChunkAt(water) || !level.getFluidState(water).is(FluidTags.WATER)
                            || !level.getBlockState(water.above()).isAir()) continue;
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        BlockPos bank = water.relative(side);
                        if (!level.hasChunkAt(bank) || !level.getBlockState(bank).isAir()
                                || !level.getBlockState(bank.above()).isAir()
                                || !level.getBlockState(bank.below()).isSolidRender()) continue;
                        banks.add(new Bank(bank, water));
                    }
                }
        banks.sort(Comparator.comparingDouble(b -> b.bank.distSqr(villager.blockPosition())));
        for (Bank candidate : banks) {
            Path path = villager.getNavigation().createPath(candidate.bank, 0);
            if (path != null && path.canReach()) {
                state.bank = candidate.bank;
                state.water = candidate.water;
                return;
            }
        }
    }

    private static ItemStack rollFish(ServerLevel level, BlockPos water, Villager villager) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(water))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .withParameter(LootContextParams.THIS_ENTITY, villager)
                .create(LootContextParamSets.FISHING);
        for (ItemStack stack : level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING_FISH).getRandomItems(params)) {
            if (isFish(stack)) { ItemStack one = stack.copy(); one.setCount(1); return one; }
        }
        return ItemStack.EMPTY;
    }

    private static boolean canHoldAnyFish(SimpleContainer output) {
        // Reserve a genuinely empty slot so any one fish, including a datapack component variant, fits.
        for (int i = 0; i < output.getContainerSize(); i++)
            if (output.getItem(i).isEmpty()) return true;
        return false;
    }

    private static boolean isFish(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);
    }

    private static boolean containsFish(SimpleContainer output) {
        for (int i = 0; i < output.getContainerSize(); i++) if (isFish(output.getItem(i))) return true;
        return false;
    }

    private static boolean containsWool(SimpleContainer output) {
        for (int i = 0; i < output.getContainerSize(); i++)
            if (output.getItem(i).is(net.minecraft.tags.ItemTags.WOOL)) return true;
        return false;
    }

    private static BarrelBlockEntity claimedBarrel(ServerLevel level, BlockPos site) {
        if (!level.hasChunkAt(site) || !level.getBlockState(site).is(Blocks.BARREL)) return null;
        if (level.getBlockEntity(site) instanceof BarrelBlockEntity barrel && barrel.getLootTable() == null) return barrel;
        return null;
    }

    private static void deposit(SimpleContainer owned, BarrelBlockEntity barrel, boolean wool) {
        for (int i = 0; i < owned.getContainerSize(); i++) {
            ItemStack stack = owned.getItem(i);
            if (stack.isEmpty() || (wool ? !stack.is(net.minecraft.tags.ItemTags.WOOL) : !isFish(stack))) continue;
            int inserted = OutputStorage.insert(barrel, stack, stack.getCount());
            if (inserted > 0) owned.removeItem(i, inserted);
        }
    }

    private static void ambient(Villager villager, BlockPos site, State state) {
        if (villager.distanceToSqr(Vec3.atCenterOf(site)) > 3 * 3) {
            if (villager.tickCount % 60 == 0 && villager.level() instanceof ServerLevel level)
                moveNearSite(villager, level, site, 0.5);
            return;
        }
        if ((villager.tickCount + villager.getId()) % 100 == 0) {
            villager.getLookControl().setLookAt(Vec3.atCenterOf(site));
            villager.swing(InteractionHand.MAIN_HAND);
            villager.playWorkSound();
        }
    }

    private static Profile profile(ResourceKey<VillagerProfession> profession) {
        if (profession == null) return null;
        if (profession == VillagerProfession.SHEPHERD) return new Profile(PoiTypes.SHEPHERD, Blocks.LOOM);
        if (profession == VillagerProfession.FISHERMAN) return new Profile(PoiTypes.FISHERMAN, Blocks.BARREL);
        if (profession == VillagerProfession.ARMORER) return new Profile(PoiTypes.ARMORER, Blocks.BLAST_FURNACE);
        if (profession == VillagerProfession.BUTCHER) return new Profile(PoiTypes.BUTCHER, Blocks.SMOKER);
        if (profession == VillagerProfession.CARTOGRAPHER) return new Profile(PoiTypes.CARTOGRAPHER, Blocks.CARTOGRAPHY_TABLE);
        if (profession == VillagerProfession.CLERIC) return new Profile(PoiTypes.CLERIC, Blocks.BREWING_STAND);
        if (profession == VillagerProfession.FLETCHER) return new Profile(PoiTypes.FLETCHER, Blocks.FLETCHING_TABLE);
        if (profession == VillagerProfession.LEATHERWORKER) return new Profile(PoiTypes.LEATHERWORKER, Blocks.CAULDRON);
        if (profession == VillagerProfession.LIBRARIAN) return new Profile(PoiTypes.LIBRARIAN, Blocks.LECTERN);
        if (profession == VillagerProfession.MASON) return new Profile(PoiTypes.MASON, Blocks.STONECUTTER);
        if (profession == VillagerProfession.TOOLSMITH) return new Profile(PoiTypes.TOOLSMITH, Blocks.SMITHING_TABLE);
        if (profession == VillagerProfession.WEAPONSMITH) return new Profile(PoiTypes.WEAPONSMITH, Blocks.GRINDSTONE);
        return null; // Farmer, nitwit, and unemployed villagers retain vanilla behavior.
    }

    private static Item woolFor(DyeColor color) {
        return Blocks.WOOL.pick(color).asItem();
    }

    private static void moveNearSite(Villager villager, ServerLevel level, BlockPos site, double speed) {
        List<BlockPos> standing = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos pos = site.offset(0, dy, 0).relative(side);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.below()).isSolidRender()) standing.add(pos);
        }
        standing.sort(Comparator.comparingDouble(pos -> pos.distSqr(villager.blockPosition())));
        for (BlockPos pos : standing) {
            Path path = villager.getNavigation().createPath(pos, 0);
            if (path != null && path.canReach() && villager.getNavigation().moveTo(path, speed)) return;
        }
    }

    private static void showProp(Villager villager, State state, Item item) {
        if (state.prop != null) return;
        if (!villager.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return;
        state.prop = new ItemStack(item);
        villager.setItemInHand(InteractionHand.MAIN_HAND, state.prop);
    }

    public static void clearProp(Villager villager) {
        State state = STATES.get(villager);
        if (state == null || state.prop == null) return;
        if (villager.getItemInHand(InteractionHand.MAIN_HAND) == state.prop)
            villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        state.prop = null;
    }

    private static void cancel(Villager villager, State state) {
        if (state.floatEntity != null) state.floatEntity.discard();
        state.floatEntity = null;
        state.water = null;
        state.bank = null;
        state.sheep = null;
        clearProp(villager);
    }

    private record Profile(ResourceKey<PoiType> poi, Block block) {}
    private record Bank(BlockPos bank, BlockPos water) {}
    private static final class State {
        Sheep sheep;
        BlockPos bank;
        BlockPos water;
        FishingFloat floatEntity;
        int catchAt;
        int cooldown;
        ItemStack prop;
    }
}
