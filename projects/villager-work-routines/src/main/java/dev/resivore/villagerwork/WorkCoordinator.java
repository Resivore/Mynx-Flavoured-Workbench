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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** A bounded server-side overlay that runs only during vanilla WORK activity. */
public final class WorkCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagerWorkRoutines");
    private static final Map<Villager, State> STATES = new WeakHashMap<>();
    private static final int SCAN_INTERVAL = 60;
    private static final int SHEEP_RADIUS = 12;
    private static final int FISH_RADIUS = 8;
    private static final int TELEGRAPH_TICKS = 14;
    private static final int NAVIGATION_TIMEOUT = 160;
    private static final int SCAN_LOG_INTERVAL = 200;

    private WorkCoordinator() {}

    public static void tick(Villager villager, ServerLevel level) {
        ResourceKey<VillagerProfession> profession = villager.getVillagerData().profession().unwrapKey().orElse(null);
        Profile profile = profile(profession);
        State state = STATES.get(villager);
        if (profile == null) { if (state != null) cancel(villager, state, "profession changed"); return; }
        if (state == null) { state = new State(); STATES.put(villager, state); }
        BlockPos site = claimedSite(villager, level, profile);
        if (site == null || villager.isBaby() || !villager.isAlive() || villager.isTrading() || villager.isSleeping()
                || !villager.getBrain().isActive(Activity.WORK)
                || villager.getBrain().hasMemoryValue(MemoryModuleType.INTERACTION_TARGET)
                || villager.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)) {
            cancel(villager, state, site == null ? "claim or workstation unavailable" : "WORK eligibility interrupted");
            return;
        }
        if (state.eligible && state.profession != profession) cancel(villager, state, "profession changed");
        if ((profession == VillagerProfession.SHEPHERD || profession == VillagerProfession.FISHERMAN)
                && (!state.eligible || !site.equals(state.site))) {
            if (state.eligible) cancel(villager, state, "claimed site changed");
            state.eligible = true;
            state.site = site;
            state.profession = profession;
            log(villager, "custom WORK eligible; claimed site={}", site);
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
        if (villager.tickCount % 20 == 0 && containsWool(owned)) depositWool(villager, level, loom, owned, state);
        if (state.clearPropAt != 0) {
            if (villager.tickCount < state.clearPropAt) return;
            clearProp(villager);
            state.clearPropAt = 0;
        }
        if (containsWool(owned) && villager.distanceToSqr(Vec3.atCenterOf(loom)) > 4 * 4) {
            clearSheep(villager, state, "returning to loom with wool");
            if (villager.tickCount % 20 == 0) moveNearSite(villager, level, loom, 0.6);
            return;
        }
        Sheep target = state.sheep;
        if (target != null && (!target.isAlive() || target.isBaby() || !target.readyForShearing()
                || target.distanceToSqr(Vec3.atCenterOf(loom)) > 16 * 16 || target.distanceToSqr(villager) > 18 * 18)) {
            clearSheep(villager, state, "selected sheep no longer eligible or local");
            target = null;
        }
        if (target == null && villager.tickCount >= state.cooldown
                && (villager.tickCount + villager.getId()) % SCAN_INTERVAL == 0) {
            List<Sheep> sheep = level.getEntitiesOfClass(Sheep.class,
                    new AABB(loom).inflate(SHEEP_RADIUS, 4, SHEEP_RADIUS), Sheep::isAlive);
            sheep.sort(Comparator.comparingDouble((Sheep candidate) -> villager.distanceToSqr(candidate))
                    .thenComparingInt(Sheep::getId));
            int baby = 0, notShearable = 0, noCapacity = 0, noInteraction = 0, noPath = 0, examined = 0;
            for (Sheep candidate : sheep) {
                examined++;
                if (candidate.isBaby()) { baby++; continue; }
                if (!candidate.readyForShearing()) { notShearable++; continue; }
                if (candidate.distanceToSqr(villager) > 18 * 18) { noInteraction++; continue; }
                if (!OutputStorage.fits(owned, new ItemStack(woolFor(candidate.getColor())), 3)) { noCapacity++; continue; }
                List<BlockPos> positions = sheepPositions(candidate, villager, level);
                if (positions.isEmpty()) { noInteraction++; continue; }
                for (BlockPos position : positions) {
                    Path path = villager.getNavigation().createPath(position, 0);
                    if (path == null || !path.canReach() || !position.equals(path.getTarget())) continue;
                    if (!villager.getNavigation().moveTo(path, 0.6)) continue;
                    state.sheep = candidate;
                    state.sheepTarget = position;
                    state.navigationDeadline = villager.tickCount + NAVIGATION_TIMEOUT;
                    state.customNavigation = true;
                    state.customPath = path;
                    target = candidate;
                    log(villager, "selected sheep id={} uuid={} at {} interaction={} path reachable; navigation started",
                            candidate.getId(), candidate.getUUID(), candidate.blockPosition(), position);
                    break;
                }
                if (target != null) break;
                noPath++;
            }
            if (villager.tickCount >= state.nextScanLog || target != null) {
                log(villager, "sheep scan loom={} found={} examined={} baby={} notShearable={} noCapacity={} noValidInteraction={} noPath={} selected={}",
                        loom, sheep.size(), examined, baby, notShearable, noCapacity, noInteraction, noPath,
                        target == null ? "none" : target.getId());
                state.nextScanLog = villager.tickCount + SCAN_LOG_INTERVAL;
            }
        }
        if (target == null) return;
        if (!OutputStorage.fits(owned, new ItemStack(woolFor(target.getColor())), 3)) {
            clearSheep(villager, state, "no safe wool output capacity");
            return;
        }
        villager.getLookControl().setLookAt(target);
        if (villager.tickCount > state.navigationDeadline && state.shearAt == 0) {
            clearSheep(villager, state, "interaction navigation timed out");
            return;
        }
        if (!validSheepInteraction(villager, target, state.sheepTarget, level)) {
            if (state.shearAt != 0) { clearSheep(villager, state, "interaction became blocked or invalid during action"); return; }
            if (villager.tickCount % 20 == 0) {
                Path path = villager.getNavigation().createPath(state.sheepTarget, 0);
                if (path == null || !path.canReach() || !state.sheepTarget.equals(path.getTarget())
                        || !villager.getNavigation().moveTo(path, 0.6)) {
                    clearSheep(villager, state, "interaction path lost");
                } else state.customPath = path;
            }
            return;
        }
        stopCustomNavigation(villager, state);
        if (state.shearAt == 0) {
            if (!showProp(villager, state, Items.SHEARS)) { clearSheep(villager, state, "main hand occupied; shears cannot be shown"); return; }
            state.shearAt = villager.tickCount + TELEGRAPH_TICKS;
            log(villager, "arrived at sheep id={} interaction={}; shears equipped, action begins; shearAt={}",
                    target.getId(), state.sheepTarget, state.shearAt);
            return;
        }
        boolean shearsWereCleared = state.prop == null;
        if (!showProp(villager, state, Items.SHEARS)) {
            clearSheep(villager, state, "shears lost during action");
            return;
        }
        if (shearsWereCleared) {
            state.shearAt = villager.tickCount + TELEGRAPH_TICKS;
            log(villager, "shears restored after save/interruption; action phase restarted shearAt={}", state.shearAt);
            return;
        }
        if (villager.tickCount < state.shearAt) return;
        log(villager, "actual shear fired sheep id={} at {}", target.getId(), target.blockPosition());
        villager.swing(InteractionHand.MAIN_HAND);
        int before = woolCount(owned);
        ShearCapture.shear(target, level, owned);
        log(villager, "shear captured wool count={} ownedTotal={}", woolCount(owned) - before, woolCount(owned));
        state.sheep = null;
        state.sheepTarget = null;
        state.shearAt = 0;
        state.cooldown = villager.tickCount + 60;
        state.clearPropAt = villager.tickCount + 5;
    }

    private static List<BlockPos> sheepPositions(Sheep sheep, Villager villager, ServerLevel level) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos origin = sheep.blockPosition();
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;
            BlockPos feet = origin.offset(dx, dy, dz);
            if (standingIsClear(level, villager, feet)
                    && Vec3.atCenterOf(feet).distanceToSqr(sheep.position()) <= 2.4 * 2.4
                    && clearShearLine(level, Vec3.atBottomCenterOf(feet).add(0, 0.9, 0), sheep))
                positions.add(feet);
        }
        positions.sort(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(villager.blockPosition()))
                .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        return positions;
    }

    private static boolean standingIsClear(ServerLevel level, Villager villager, BlockPos feet) {
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(feet.above()) || !level.hasChunkAt(feet.below())) return false;
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                || !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()
                || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) return false;
        AABB box = villager.getBoundingBox().move(feet.getX() + 0.5 - villager.getX(),
                feet.getY() - villager.getY(), feet.getZ() + 0.5 - villager.getZ());
        return level.noCollision(villager, box);
    }

    private static boolean clearShearLine(ServerLevel level, Vec3 from, Sheep sheep) {
        Vec3 to = sheep.position().add(0, 0.7, 0);
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, sheep)).getType() == HitResult.Type.MISS;
    }

    private static boolean validSheepInteraction(Villager villager, Sheep sheep, BlockPos feet, ServerLevel level) {
        return feet != null && villager.blockPosition().equals(feet) && standingIsClear(level, villager, feet)
                && villager.distanceToSqr(Vec3.atBottomCenterOf(feet)) <= 1.3 * 1.3
                && villager.distanceToSqr(sheep) <= 2.4 * 2.4
                && clearShearLine(level, villager.position().add(0, 0.9, 0), sheep);
    }

    private static void clearSheep(Villager villager, State state, String reason) {
        if (state.sheep != null) log(villager, "shepherd cancelled sheep id={} interaction={}: {}",
                state.sheep.getId(), state.sheepTarget, reason);
        stopCustomNavigation(villager, state);
        state.sheep = null;
        state.sheepTarget = null;
        state.shearAt = 0;
        state.clearPropAt = 0;
        clearProp(villager);
    }

    private static int woolCount(SimpleContainer owned) {
        int count = 0;
        for (int i = 0; i < owned.getContainerSize(); i++)
            if (owned.getItem(i).is(net.minecraft.tags.ItemTags.WOOL)) count += owned.getItem(i).getCount();
        return count;
    }

    private static void depositWool(Villager villager, ServerLevel level, BlockPos loom, SimpleContainer owned, State state) {
        if (villager.distanceToSqr(Vec3.atCenterOf(loom)) > 4 * 4) return;
        int before = woolCount(owned);
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
        int deposited = before - woolCount(owned);
        if (deposited > 0 || villager.tickCount >= state.nextDepositLog) {
            log(villager, "wool deposit loom={} adjacentBarrels={} barrelPositions={} attempted={} inserted={} retained={}",
                    loom, barrels.size(), barrels.stream().map(BarrelBlockEntity::getBlockPos).toList(),
                    before, deposited, woolCount(owned));
            state.nextDepositLog = villager.tickCount + SCAN_LOG_INTERVAL;
        }
    }

    private static void fisherman(Villager villager, ServerLevel level, BlockPos site, State state) {
        SimpleContainer owned = ((OwnedOutput)villager).villagerWork$ownedOutput();
        if (villager.tickCount % 20 == 0 && villager.distanceToSqr(Vec3.atCenterOf(site)) <= 4 * 4) {
            BarrelBlockEntity barrel = claimedBarrel(level, site);
            if (barrel != null && containsFish(owned)) {
                int attempted = fishCount(owned);
                int inserted = deposit(owned, barrel, false);
                if (inserted > 0 || villager.tickCount >= state.nextDepositLog) {
                    log(villager, "fish deposit claimedBarrel={} attempted={} inserted={} retained={}",
                            site, attempted, inserted, fishCount(owned));
                    state.nextDepositLog = villager.tickCount + SCAN_LOG_INTERVAL;
                }
            }
        }
        if (state.floatEntity != null) {
            if (state.floatEntity.isRemoved() || state.water == null || !level.hasChunkAt(state.water)
                    || !openWater(level, state.water)) { cancel(villager, state, "float lost or water vanished"); return; }
            if (!showProp(villager, state, Items.FISHING_ROD)) { cancel(villager, state, "rod cannot remain equipped"); return; }
            villager.getLookControl().setLookAt(Vec3.atCenterOf(state.water));
            if (villager.tickCount >= state.catchAt) {
                log(villager, "retrieve float={} water={} scheduledAt={}", state.floatEntity.getId(), state.water, state.catchAt);
                villager.swing(InteractionHand.MAIN_HAND);
                level.sendParticles(ParticleTypes.SPLASH, state.floatEntity.getX(), state.floatEntity.getY(),
                        state.floatEntity.getZ(), 8, 0.15, 0.05, 0.15, 0.03);
                level.playSound(null, state.floatEntity.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH,
                        SoundSource.NEUTRAL, 0.6f, 1.0f);
                state.floatEntity.discard();
                state.floatEntity = null;
                ItemStack caught = rollFish(level, state.water, villager);
                int inserted = 0;
                if (!caught.isEmpty() && isFish(caught) && OutputStorage.fits(owned, caught, caught.getCount()))
                    inserted = OutputStorage.insert(owned, caught, caught.getCount());
                log(villager, "fish result={} count={} insertedIntoOwned={} retainedTotal={}",
                        caught.isEmpty() ? "none" : caught.getItem(), caught.getCount(), inserted, fishCount(owned));
                state.water = null;
                state.bank = null;
                state.rodAt = 0;
                state.cooldown = villager.tickCount + 100;
                clearProp(villager);
            }
            return;
        }
        if (containsFish(owned)) {
            if (villager.distanceToSqr(Vec3.atCenterOf(site)) > 3 * 3 && villager.tickCount % 20 == 0) {
                if (state.nextReturnLog <= villager.tickCount) {
                    log(villager, "returning with fish={} to claimed barrel={}", fishCount(owned), site);
                    state.nextReturnLog = villager.tickCount + SCAN_LOG_INTERVAL;
                }
                moveNearSite(villager, level, site, 0.6);
            }
            return;
        }
        if (villager.tickCount < state.cooldown) return;
        if (!canHoldAnyFish(owned)) {
            if (villager.tickCount >= state.nextScanLog) {
                log(villager, "fishing paused: no safe owned-output capacity");
                state.nextScanLog = villager.tickCount + SCAN_LOG_INTERVAL;
            }
            return;
        }
        if (state.bank == null && (villager.tickCount + villager.getId()) % SCAN_INTERVAL == 0)
            locateWater(villager, level, site, state);
        if (state.bank == null) return;
        if (!openWater(level, state.water) || !standingIsClear(level, villager, state.bank)) {
            cancel(villager, state, "selected water or bank became invalid");
            return;
        }
        if (villager.tickCount > state.navigationDeadline && state.rodAt == 0) {
            cancel(villager, state, "bank navigation timed out");
            return;
        }
        if (!villager.blockPosition().equals(state.bank)
                || villager.distanceToSqr(Vec3.atBottomCenterOf(state.bank)) > 1.1 * 1.1) {
            if (state.rodAt != 0) { cancel(villager, state, "moved away from bank before cast"); return; }
            if (villager.tickCount % 20 == 0) {
                Path path = villager.getNavigation().createPath(state.bank, 0);
                if (path == null || !path.canReach() || !state.bank.equals(path.getTarget())
                        || !villager.getNavigation().moveTo(path, 0.6))
                    cancel(villager, state, "bank path lost");
                else state.customPath = path;
            }
            return;
        }
        stopCustomNavigation(villager, state);
        villager.getLookControl().setLookAt(Vec3.atCenterOf(state.water));
        boolean rodWasCleared = state.rodAt != 0 && state.prop == null;
        if (!showProp(villager, state, Items.FISHING_ROD)) { cancel(villager, state, "main hand occupied; rod cannot be shown"); return; }
        if (rodWasCleared) {
            state.rodAt = villager.tickCount + 8;
            log(villager, "rod restored after save/interruption; cast phase restarted castAt={}", state.rodAt);
            return;
        }
        if (state.rodAt == 0) {
            state.rodAt = villager.tickCount + 8;
            log(villager, "arrived bank={} facing water={}; rod equipped, castAt={}", state.bank, state.water, state.rodAt);
            return;
        }
        if (villager.tickCount < state.rodAt) return;
        villager.swing(InteractionHand.MAIN_HAND);
        FishingFloat bobber = new FishingFloat(level, villager, state.water);
        if (level.addFreshEntity(bobber)) {
            state.floatEntity = bobber;
            state.catchAt = villager.tickCount + 120 + villager.getRandom().nextInt(180);
            log(villager, "cast water={} bank={} floatEntity={} spawned; retrieveAt={}",
                    state.water, state.bank, bobber.getId(), state.catchAt);
            level.playSound(null, villager.blockPosition(), SoundEvents.FISHING_BOBBER_THROW,
                    SoundSource.NEUTRAL, 0.5f, 1.0f);
        } else cancel(villager, state, "float entity spawn failed");
    }

    private static void locateWater(Villager villager, ServerLevel level, BlockPos site, State state) {
        List<ShorelineCandidates.Candidate> banks = new ArrayList<>();
        int[] rejected = new int[3]; // unloaded, obstructed feet/head, unsupported ground
        ShorelineCandidates.Geometry geometry = new ShorelineCandidates.Geometry() {
            @Override public boolean loaded(BlockPos pos) {
                if (level.hasChunkAt(pos)) return true;
                rejected[0]++;
                return false;
            }
            @Override public boolean clear(BlockPos pos) {
                if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getFluidState(pos).isEmpty()) return true;
                rejected[1]++;
                return false;
            }
            @Override public boolean supports(BlockPos pos) {
                if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) return true;
                rejected[2]++;
                return false;
            }
        };
        int waterFound = 0, openWater = 0, noOpenWater = 0, noStanding = 0, bodyBlocked = 0, noPath = 0;
        for (int dx = -FISH_RADIUS; dx <= FISH_RADIUS; dx++)
            for (int dz = -FISH_RADIUS; dz <= FISH_RADIUS; dz++)
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos water = site.offset(dx, dy, dz);
                    if (!level.hasChunkAt(water) || !level.hasChunkAt(water.above())) { rejected[0]++; continue; }
                    if (!level.getFluidState(water).is(FluidTags.WATER)) continue;
                    waterFound++;
                    if (!openWater(level, water)) { noOpenWater++; continue; }
                    openWater++;
                    List<ShorelineCandidates.Candidate> found = ShorelineCandidates.adjacentTo(water, geometry);
                    if (found.isEmpty()) noStanding++;
                    banks.addAll(found);
                }
        banks.sort(ShorelineCandidates.nearestTo(villager.blockPosition(), site));
        for (ShorelineCandidates.Candidate candidate : banks) {
            if (!standingIsClear(level, villager, candidate.feet())) { bodyBlocked++; continue; }
            Path path = villager.getNavigation().createPath(candidate.feet(), 0);
            if (path == null || !path.canReach() || !candidate.feet().equals(path.getTarget())
                    || !villager.getNavigation().moveTo(path, 0.6)) { noPath++; continue; }
            state.bank = candidate.feet();
            state.water = candidate.water();
            state.navigationDeadline = villager.tickCount + NAVIGATION_TIMEOUT;
            state.customNavigation = true;
            state.customPath = path;
            log(villager, "selected water={} bank={} path reachable; navigation started", state.water, state.bank);
            break;
        }
        if (villager.tickCount >= state.nextScanLog || state.bank != null) {
            log(villager, "water/bank scan claimedBarrel={} waterFound={} openWater={} noOpenWater={} noStanding={} bankCandidates={} unsafeOrUnloaded={} obstructed={} noSupport={} bodyBlocked={} noPath={} selected={}",
                    site, waterFound, openWater, noOpenWater, noStanding, banks.size(), rejected[0], rejected[1], rejected[2], bodyBlocked, noPath,
                    state.bank == null ? "none" : state.bank);
            state.nextScanLog = villager.tickCount + SCAN_LOG_INTERVAL;
        }
    }

    private static boolean openWater(ServerLevel level, BlockPos water) {
        return water != null && level.hasChunkAt(water) && level.hasChunkAt(water.above())
                && level.getFluidState(water).is(FluidTags.WATER) && level.getFluidState(water).isSource()
                && level.getBlockState(water).getCollisionShape(level, water).isEmpty()
                && level.getFluidState(water.above()).isEmpty()
                && level.getBlockState(water.above()).getCollisionShape(level, water.above()).isEmpty();
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

    private static int deposit(SimpleContainer owned, BarrelBlockEntity barrel, boolean wool) {
        int total = 0;
        for (int i = 0; i < owned.getContainerSize(); i++) {
            ItemStack stack = owned.getItem(i);
            if (stack.isEmpty() || (wool ? !stack.is(net.minecraft.tags.ItemTags.WOOL) : !isFish(stack))) continue;
            int inserted = OutputStorage.insert(barrel, stack, stack.getCount());
            if (inserted > 0) { owned.removeItem(i, inserted); total += inserted; }
        }
        return total;
    }

    private static int fishCount(SimpleContainer owned) {
        int count = 0;
        for (int i = 0; i < owned.getContainerSize(); i++)
            if (isFish(owned.getItem(i))) count += owned.getItem(i).getCount();
        return count;
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

    private static boolean showProp(Villager villager, State state, Item item) {
        if (state.prop != null) return villager.getItemInHand(InteractionHand.MAIN_HAND) == state.prop;
        if (!villager.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return false;
        state.prop = new ItemStack(item);
        villager.setItemInHand(InteractionHand.MAIN_HAND, state.prop);
        return true;
    }

    public static void clearProp(Villager villager) {
        State state = STATES.get(villager);
        if (state == null || state.prop == null) return;
        if (villager.getItemInHand(InteractionHand.MAIN_HAND) == state.prop)
            villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        state.prop = null;
    }

    private static void cancel(Villager villager, State state, String reason) {
        if (state.eligible || state.sheep != null || state.bank != null || state.floatEntity != null)
            log(villager, "custom WORK cancelled: {} site={} sheep={} bank={} water={} float={}",
                    reason, state.site, state.sheep == null ? "none" : state.sheep.getId(), state.bank,
                    state.water, state.floatEntity == null ? "none" : state.floatEntity.getId());
        if (state.floatEntity != null) state.floatEntity.discard();
        stopCustomNavigation(villager, state);
        state.floatEntity = null;
        state.water = null;
        state.bank = null;
        state.sheep = null;
        state.sheepTarget = null;
        state.shearAt = 0;
        state.clearPropAt = 0;
        state.rodAt = 0;
        state.eligible = false;
        state.site = null;
        state.profession = null;
        clearProp(villager);
    }

    private static void log(Villager villager, String message, Object... args) {
        LOGGER.info("[VillagerWorkRoutines] villager id={} uuid={} profession={} " + message,
                prepend(villager, args));
    }

    private static void stopCustomNavigation(Villager villager, State state) {
        if (state.customNavigation && villager.getNavigation().getPath() == state.customPath)
            villager.getNavigation().stop();
        state.customNavigation = false;
        state.customPath = null;
    }

    private static Object[] prepend(Villager villager, Object[] args) {
        Object[] all = new Object[args.length + 3];
        all[0] = villager.getId();
        all[1] = villager.getUUID();
        all[2] = villager.getVillagerData().profession().unwrapKey().orElse(null);
        System.arraycopy(args, 0, all, 3, args.length);
        return all;
    }

    private record Profile(ResourceKey<PoiType> poi, Block block) {}
    private static final class State {
        Sheep sheep;
        BlockPos sheepTarget;
        BlockPos bank;
        BlockPos water;
        BlockPos site;
        ResourceKey<VillagerProfession> profession;
        FishingFloat floatEntity;
        int catchAt;
        int shearAt;
        int clearPropAt;
        int rodAt;
        int navigationDeadline;
        int nextScanLog;
        int nextDepositLog;
        int nextReturnLog;
        int cooldown;
        ItemStack prop;
        boolean eligible;
        boolean customNavigation;
        Path customPath;
    }
}
