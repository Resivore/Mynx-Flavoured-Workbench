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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    private static final int GATE_SEARCH_RADIUS = SHEEP_RADIUS + 4;
    private static final int GATE_FLOOD_LIMIT = 1200;
    private static final int GATE_CROSS_TIMEOUT = 100;
    private static final int SCAN_LOG_INTERVAL = 200;

    private WorkCoordinator() {}

    public static void tick(Villager villager, ServerLevel level) {
        ResourceKey<VillagerProfession> profession = villager.getVillagerData().profession().unwrapKey().orElse(null);
        Profile profile = profile(profession);
        State state = STATES.get(villager);
        if (state != null && state.pendingExit != null) restorePendingExit(villager, level, state);
        if (profile == null) {
            if (state != null) {
                if (state.eligible || state.sheep != null || state.bank != null || state.propSlot.current() != null)
                    cancel(villager, state, "profession changed");
                cleanupGateExit(villager, level, state);
            }
            return;
        }
        if (state == null) { state = new State(); STATES.put(villager, state); }
        if (state.eligible && state.profession != profession) cancel(villager, state, "profession changed");
        SiteCheck siteCheck = claimedSite(villager, level, profile);
        List<String> interruptions = new ArrayList<>();
        if (!villager.isAlive()) interruptions.add("villager dead");
        if (villager.isBaby()) interruptions.add("villager is a baby");
        if (villager.isSleeping()) interruptions.add("villager sleeping");
        if (villager.isTrading()) interruptions.add("villager trading with player");
        if (villager.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)) interruptions.add("breed target present");
        if (profession != VillagerProfession.SHEPHERD && profession != VillagerProfession.FISHERMAN
                && villager.getBrain().hasMemoryValue(MemoryModuleType.INTERACTION_TARGET))
            interruptions.add("interaction target present during ambient work");
        if (!villager.getBrain().isActive(Activity.WORK)) interruptions.add("vanilla WORK activity inactive");
        if (siteCheck.reason() != null) interruptions.add(siteCheck.reason());
        if (!interruptions.isEmpty()) {
            if (state.eligible || state.sheep != null || state.bank != null || state.propSlot.current() != null)
                cancel(villager, state, String.join("; ", interruptions));
            cleanupGateExit(villager, level, state);
            return;
        }
        BlockPos site = siteCheck.site();
        if ((profession == VillagerProfession.SHEPHERD || profession == VillagerProfession.FISHERMAN)
                && (!state.eligible || !site.equals(state.site))) {
            if (state.eligible) cancel(villager, state, "claimed site changed from " + state.site + " to " + site);
            state.eligible = true;
            state.site = site;
            state.profession = profession;
            log(villager, "custom WORK eligible; claimed site={} interactionTarget={} (non-blocking)",
                    site, interactionTarget(villager));
        }
        if (profession == VillagerProfession.SHEPHERD) shepherd(villager, level, site, state);
        else if (profession == VillagerProfession.FISHERMAN) fisherman(villager, level, site, state);
        else ambient(villager, site, state);
    }

    private static SiteCheck claimedSite(Villager villager, ServerLevel level, Profile profile) {
        GlobalPos claim = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null);
        if (claim == null) return new SiteCheck(null, "claimed job site absent");
        if (!claim.dimension().equals(level.dimension()))
            return new SiteCheck(null, "claimed job site in wrong dimension: " + claim.dimension());
        BlockPos site = claim.pos();
        if (site.distSqr(villager.blockPosition()) > 24 * 24)
            return new SiteCheck(null, "claimed job site out of range: " + site);
        if (!level.hasChunkAt(site)) return new SiteCheck(null, "claimed job site chunk unavailable: " + site);
        if (!level.getBlockState(site).is(profile.block))
            return new SiteCheck(null, "workstation block mismatch at " + site + ": expected " + profile.block
                    + ", found " + level.getBlockState(site).getBlock());
        if (!level.getPoiManager().getType(site).map(type -> type.is(profile.poi)).orElse(false))
            return new SiteCheck(null, "workstation POI mismatch at " + site + ": expected " + profile.poi
                    + ", found " + level.getPoiManager().getType(site).orElse(null));
        return new SiteCheck(site, null);
    }

    private static String interactionTarget(Villager villager) {
        // Vanilla's 26.2 WORK package sets this memory when looking at a nearby player.
        // The memory alone does not mean the villager is trading or should abandon work.
        return villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET)
                .map(target -> target.getType() + "#" + target.getId()).orElse("none");
    }

    private static void shepherd(Villager villager, ServerLevel level, BlockPos loom, State state) {
        SimpleContainer owned = ((OwnedOutput)villager).villagerWork$ownedOutput();
        if (state.gateRoute != null && state.gateRoute.stage == GateRouteRules.Stage.CANCELLED) {
            retryGateRestoration(villager, level, state);
            return;
        }
        if (state.clearPropAt != 0) {
            if (villager.tickCount < state.clearPropAt) return;
            clearProp(villager);
            state.clearPropAt = 0;
        }
        if (state.gateRoute != null && isGateExitStage(state.gateRoute.stage)
                && tickGateReturn(villager, level, loom, state)) return;
        if (containsWool(owned)) {
            if (state.gateRoute == null && villager.tickCount % 20 == 0)
                depositWool(villager, level, loom, owned, state);
            if (villager.distanceToSqr(Vec3.atCenterOf(loom)) > 4 * 4) {
                if (villager.tickCount % 20 == 0) moveNearSite(villager, level, loom, 0.6);
                return;
            }
            if (villager.tickCount % 20 == 0) depositWool(villager, level, loom, owned, state);
            return;
        }
        Sheep target = state.sheep;
        String targetInvalid = target == null ? null : sheepInvalidReason(target, villager, loom, state);
        if (targetInvalid != null) {
            clearSheep(villager, state, targetInvalid);
            target = null;
        }
        if (state.gateRoute != null && isGateExitStage(state.gateRoute.stage)) return;
        if (target == null && villager.tickCount >= state.cooldown
                && (villager.tickCount + villager.getId()) % SCAN_INTERVAL == 0) {
            List<Sheep> sheep = level.getEntitiesOfClass(Sheep.class,
                    new AABB(loom).inflate(SHEEP_RADIUS, 4, SHEEP_RADIUS), Sheep::isAlive);
            sheep.sort(Comparator.comparingDouble((Sheep candidate) -> villager.distanceToSqr(candidate))
                    .thenComparingInt(Sheep::getId));
            int baby = 0, notShearable = 0, noCapacity = 0, noInteraction = 0, noPath = 0, examined = 0;
            boolean describeAttempts = villager.tickCount >= state.nextScanLog;
            for (Sheep candidate : sheep) {
                examined++;
                if (candidate.isBaby()) { baby++; continue; }
                if (!candidate.readyForShearing()) { notShearable++; continue; }
                if (candidate.distanceToSqr(villager) > 18 * 18) { noInteraction++; continue; }
                if (!OutputStorage.fits(owned, new ItemStack(woolFor(candidate.getColor())), 3)) { noCapacity++; continue; }
                List<BlockPos> positions = sheepPositions(candidate, villager, level);
                if (positions.isEmpty()) { noInteraction++; continue; }
                List<String> attempts = new ArrayList<>();
                for (BlockPos position : positions) {
                    if (validSheepInteraction(villager, candidate, position, level)) {
                        state.sheep = candidate;
                        state.sheepRequested = position;
                        state.sheepTarget = position;
                        state.navigationDeadline = villager.tickCount + NAVIGATION_TIMEOUT;
                        state.nextSheepPathAt = villager.tickCount + 10;
                        state.repositioningSheep = false;
                        state.telegraphRestarts = 0;
                        target = candidate;
                        attempts.add("requested=" + position + " reason=alreadyPhysicallyArrived");
                        log(villager, "selected sheep id={} uuid={} at {} requested={} endpoint={} already physically arrived; navigation unnecessary",
                                candidate.getId(), candidate.getUUID(), candidate.blockPosition(), position, position);
                        break;
                    }
                    SheepPathAttempt attempt = trySheepPath(villager, candidate, position, level);
                    attempts.add(attempt.summary());
                    if (!attempt.accepted()) continue;
                    state.sheep = candidate;
                    state.sheepRequested = position;
                    state.sheepTarget = attempt.endpoint();
                    state.navigationDeadline = villager.tickCount + NAVIGATION_TIMEOUT;
                    state.nextSheepPathAt = villager.tickCount + 10;
                    state.repositioningSheep = false;
                    state.telegraphRestarts = 0;
                    state.customNavigation = true;
                    state.customPath = attempt.path();
                    target = candidate;
                    log(villager, "selected sheep id={} uuid={} at {} requested={} endpoint={} canReach={} navigation started moveTo={}",
                            candidate.getId(), candidate.getUUID(), candidate.blockPosition(), position,
                            attempt.endpoint(), attempt.canReach(), attempt.moveAccepted());
                    break;
                }
                if (target == null) {
                    GateRoute route = findGateRoute(villager, level, loom, candidate, positions, attempts, describeAttempts);
                    if (route != null) {
                        state.sheep = candidate;
                        state.sheepRequested = route.interaction;
                        state.sheepTarget = route.interaction;
                        state.gateRoute = route;
                        state.navigationDeadline = villager.tickCount + NAVIGATION_TIMEOUT;
                        state.nextSheepPathAt = villager.tickCount + 10;
                        state.repositioningSheep = false;
                        state.telegraphRestarts = 0;
                        target = candidate;
                        log(villager, "target sheep selected id={} uuid={} sheepPos={} interaction={} via gate={}",
                                candidate.getId(), candidate.getUUID(), candidate.blockPosition(), route.interaction, route.gate);
                    }
                }
                if (describeAttempts || target == candidate)
                    log(villager, "sheep route sheepId={} sheepPos={} interactionPositions={} attempts={}",
                            candidate.getId(), candidate.blockPosition(), positions.size(), attempts);
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
        boolean outputAvailable = OutputStorage.fits(owned, new ItemStack(woolFor(target.getColor())), 3);
        if (!outputAvailable) {
            clearSheep(villager, state, "no safe wool output capacity");
            return;
        }
        villager.getLookControl().setLookAt(target);
        if (state.gateRoute != null && tickGateEntry(villager, level, state)) return;
        double sheepDistance = Math.sqrt(villager.distanceToSqr(target));
        boolean footingSafe = standingIsClear(level, villager, villager.blockPosition());
        boolean clearLine = clearShearLine(level, villager.position().add(0, 0.9, 0), target);

        if (state.shearAt == 0) {
            if (state.repositioningSheep && sheepDistance > ShearActionRules.RECOVERY_DISTANCE) {
                clearSheep(villager, state, "sheep moved beyond bounded recovery distance=" + sheepDistance);
                return;
            }
            if (!validSheepInteraction(villager, target, state.sheepTarget, level)) {
                if (villager.tickCount > state.navigationDeadline) {
                    clearSheep(villager, state, state.repositioningSheep
                            ? "same target genuinely unreachable after bounded repositioning"
                            : "interaction navigation timed out before close approach");
                    return;
                }
                if (villager.tickCount >= state.nextSheepPathAt)
                    repositionSameSheep(villager, target, level, state,
                            state.repositioningSheep ? "recoverable movement" : "final close approach");
                return;
            }

            ShearActionRules.Decision approach = shearDecision(ShearActionRules.Phase.APPROACH,
                    target, villager, loom, state, outputAvailable, sheepDistance, footingSafe, clearLine);
            if (approach.action() != ShearActionRules.Action.START_TELEGRAPH) {
                if (approach.action() == ShearActionRules.Action.ABANDON)
                    clearSheep(villager, state, "close approach abandoned: " + approach.reason());
                else restartSheepApproach(villager, target, level, state, approach.reason());
                return;
            }

            stopCustomNavigation(villager, state);
            if (state.gateRoute != null && state.gateRoute.stage == GateRouteRules.Stage.APPROACH_SHEEP)
                state.gateRoute.stage = GateRouteRules.advance(state.gateRoute.stage, true);
            if (!showProp(villager, state, Items.SHEARS)) {
                clearSheep(villager, state, "temporary shears prop overlay failure");
                return;
            }
            state.shearAt = villager.tickCount + TELEGRAPH_TICKS;
            log(villager, "final approach reached sheep id={} requested={} endpoint={} villagerPos={} distance={} clearLine={}; telegraph {} shearAt={}",
                    target.getId(), state.sheepRequested, state.sheepTarget, villager.position(),
                    sheepDistance, clearLine, state.telegraphRestarts == 0 ? "started" : "restarted",
                    state.shearAt);
            state.repositioningSheep = false;
            return;
        }

        boolean shearsOverlayMissing = state.propSlot.current() == null;
        if (!showProp(villager, state, Items.SHEARS)) {
            clearSheep(villager, state, "true external hand conflict or prop overlay failure during telegraph");
            return;
        }
        if (shearsOverlayMissing) {
            state.telegraphRestarts++;
            state.shearAt = villager.tickCount + TELEGRAPH_TICKS;
            log(villager, "synthetic shears overlay restored after save/interruption; same-target telegraph restarted shearAt={}",
                    state.shearAt);
            return;
        }

        ShearActionRules.Phase phase = villager.tickCount < state.shearAt
                ? ShearActionRules.Phase.TELEGRAPH : ShearActionRules.Phase.FINAL_VALIDATION;
        ShearActionRules.Decision decision = shearDecision(phase, target, villager, loom, state,
                outputAvailable, sheepDistance, footingSafe, clearLine);
        if (decision.action() == ShearActionRules.Action.ABANDON) {
            clearSheep(villager, state, "shearing action abandoned: " + decision.reason());
            return;
        }
        if (decision.action() == ShearActionRules.Action.REPOSITION) {
            restartSheepApproach(villager, target, level, state, decision.reason());
            return;
        }
        if (decision.action() == ShearActionRules.Action.CONTINUE_TELEGRAPH) return;
        if (decision.action() != ShearActionRules.Action.SHEAR) {
            clearSheep(villager, state, "unexpected shearing decision=" + decision);
            return;
        }

        log(villager, "final pre-shear validation sheep id={} distance={} clearLine={} footingSafe={} readyForShearing={}",
                target.getId(), sheepDistance, clearLine, footingSafe, target.readyForShearing());
        villager.swing(InteractionHand.MAIN_HAND);
        int before = woolCount(owned);
        boolean sheared = ShearCapture.shear(target, level, owned);
        int captured = woolCount(owned) - before;
        log(villager, "actual shear call completed sheep id={} at={} stateChanged={} captured={} ownedTotal={}",
                target.getId(), target.blockPosition(), sheared, captured, woolCount(owned));
        if (!sheared) {
            clearSheep(villager, state, "shear call did not change the final-validated sheep state");
            return;
        }
        if (state.gateRoute != null) {
            state.gateRoute.stage = GateRouteRules.advance(GateRouteRules.Stage.SHEAR, true);
            state.gateRoute.deadline = villager.tickCount + NAVIGATION_TIMEOUT;
            log(villager, "returning to gate with wool gate={} sheepId={}", state.gateRoute.gate, target.getId());
        } else log(villager, "returning to loom with wool loom={}", loom);
        state.sheep = null;
        state.sheepRequested = null;
        state.sheepTarget = null;
        state.shearAt = 0;
        state.repositioningSheep = false;
        state.telegraphRestarts = 0;
        state.cooldown = villager.tickCount + 60;
        state.clearPropAt = villager.tickCount + 5;
    }

    /** The closed gate is never made pathfindable globally: only its two real sides are explored. */
    private static GateRoute findGateRoute(Villager villager, ServerLevel level, BlockPos loom,
                                           Sheep sheep, List<BlockPos> positions, List<String> directAttempts,
                                           boolean describeAttempts) {
        log(villager, "direct route blocked sheepId={} sheepPos={} interactionPoints={} attempts={}; gate search started barrierToSheep={}",
                sheep.getId(), sheep.blockPosition(), positions, directAttempts,
                directAttempts.stream().anyMatch(attempt -> attempt.contains("barrierToSheep=true")));
        List<GateRouteRules.Candidate> choices = new ArrayList<>();
        List<String> examined = new ArrayList<>();
        int count = 0;
        BlockPos low = loom.offset(-GATE_SEARCH_RADIUS, -4, -GATE_SEARCH_RADIUS);
        BlockPos high = loom.offset(GATE_SEARCH_RADIUS, 4, GATE_SEARCH_RADIUS);
        for (BlockPos mutable : BlockPos.betweenClosed(low, high)) {
            BlockPos gate = mutable.immutable();
            if (!level.hasChunkAt(gate)) continue;
            BlockState gateState = level.getBlockState(gate);
            if (!(gateState.getBlock() instanceof FenceGateBlock)) continue;
            count++;
            Direction facing = gateState.getValue(FenceGateBlock.FACING);
            BlockPos a = gate.relative(facing);
            BlockPos b = gate.relative(facing.getOpposite());
            if (!gatePassageHasRoom(level, villager, gate)) {
                examined.add(gate + ":unsafe/unloaded passage");
                continue;
            }
            for (int side = 0; side < 2; side++) {
                BlockPos near = side == 0 ? a : b;
                BlockPos far = side == 0 ? b : a;
                if (!standingIsClear(level, villager, near)) {
                    examined.add(gate + ":invalid near side=" + near);
                    continue;
                }
                if (!standingIsClear(level, villager, far)) {
                    examined.add(gate + ":invalid far side=" + far);
                    continue;
                }
                Path nearPath = villager.getNavigation().createPath(near, 0);
                boolean nearReachable = atBlock(villager, near) || exactEndpoint(nearPath, near);
                if (!nearReachable) {
                    examined.add(gate + ":no reachable near side=" + near + " endpoint=" + pathEndpoint(nearPath));
                    continue;
                }
                for (BlockPos interaction : positions) {
                    int farCost = walkingDistance(level, villager, far, interaction, loom);
                    boolean connects = farCost >= 0;
                    if (!connects) continue;
                    double nearCost = nearPath == null ? 0 : nearPath.getNodeCount();
                    choices.add(new GateRouteRules.Candidate(gate, near, far, interaction,
                            nearCost, farCost, true, true));
                }
                if (choices.stream().noneMatch(choice -> choice.gate().equals(gate) && choice.near().equals(near)))
                    examined.add(gate + ":far side does not connect to sheep=" + sheep.getId() + " side=" + far);
            }
        }
        GateRouteRules.Candidate selected = GateRouteRules.select(false, choices).orElse(null);
        if (selected == null) {
            if (describeAttempts || count > 0)
                log(villager, "gate search rejected sheepId={} gatesExamined={} reasons={} result=no relevant gate exists",
                        sheep.getId(), count, examined);
            return null;
        }
        BlockState state = level.getBlockState(selected.gate());
        GateRoute route = new GateRoute(selected, loom, state.getBlock(), state.getValue(FenceGateBlock.FACING),
                state.getValue(FenceGateBlock.OPEN), villager.tickCount + NAVIGATION_TIMEOUT);
        log(villager, "gate route selected sheepId={} sheepPos={} interaction={} gate={} facing={} initialOpen={} near={} far={} nearPathCost={} verifiedFarPathCost={} gatesExamined={} rejected={}",
                sheep.getId(), sheep.blockPosition(), route.interaction, route.gate, route.facing,
                route.initiallyOpen, route.near, route.far, selected.approachCost(), selected.sheepCost(), count, examined);
        return route;
    }

    private static int walkingDistance(ServerLevel level, Villager villager, BlockPos start,
                                       BlockPos target, BlockPos loom) {
        if (!standingIsClear(level, villager, start) || !standingIsClear(level, villager, target)) return -1;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        int distance = 0;
        while (!queue.isEmpty() && seen.size() <= GATE_FLOOD_LIMIT) {
            int breadth = queue.size();
            for (int i = 0; i < breadth; i++) {
                BlockPos current = queue.removeFirst();
                if (current.equals(target)) return distance;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockPos next = current.relative(direction);
                        if (Math.abs(next.getX() - loom.getX()) > GATE_SEARCH_RADIUS
                                || Math.abs(next.getZ() - loom.getZ()) > GATE_SEARCH_RADIUS
                                || Math.abs(next.getY() - loom.getY()) > 4 || seen.contains(next)) continue;
                        if (!standingIsClear(level, villager, next)) continue;
                        seen.add(next);
                        queue.addLast(next);
                }
            }
            distance++;
        }
        return -1;
    }

    private static boolean tickGateEntry(Villager villager, ServerLevel level, State state) {
        GateRoute route = state.gateRoute;
        if (route == null || route.stage == GateRouteRules.Stage.APPROACH_SHEEP
                || route.stage == GateRouteRules.Stage.SHEAR || isGateExitStage(route.stage)) return false;
        if (!sameGate(level, route)) { clearSheep(villager, state, "gate changed/disappeared during entry"); return true; }
        if (villager.tickCount > route.deadline && route.stage != GateRouteRules.Stage.CLOSE_ENTRY) {
            clearSheep(villager, state, "gate entry navigation/crossing timeout"); return true;
        }
        if (route.stage == GateRouteRules.Stage.APPROACH_ENTRY) {
            if (atBlock(villager, route.near)) {
                stopCustomNavigation(villager, state);
                route.stage = GateRouteRules.advance(route.stage, true);
                log(villager, "arrived at gate={} near={} sheepId={}", route.gate, route.near, state.sheep.getId());
            } else {
                if (route.lastPathAt == 0 || villager.tickCount - route.lastPathAt >= 20) {
                    if (!startGatePath(villager, state, route.near)) {
                        clearSheep(villager, state, "no reachable near side after gate selection gate=" + route.gate);
                        return true;
                    }
                    route.lastPathAt = villager.tickCount;
                    if (!route.approachLogged) {
                        log(villager, "approaching gate={} near={} sheepId={}", route.gate, route.near, state.sheep.getId());
                        route.approachLogged = true;
                    }
                }
                return true;
            }
        }
        if (route.stage == GateRouteRules.Stage.OPEN_ENTRY) {
            if (!atBlock(villager, route.near) || villager.distanceToSqr(Vec3.atCenterOf(route.gate)) > 2.5 * 2.5) {
                clearSheep(villager, state, "moved out of gate interaction range before entry");
                return true;
            }
            if (!openGate(villager, level, route, "entry")) {
                clearSheep(villager, state, "gate could not be opened for entry");
                return true;
            }
            route.stage = GateRouteRules.advance(route.stage, true);
            route.deadline = villager.tickCount + GATE_CROSS_TIMEOUT;
            route.lastPathAt = villager.tickCount;
            if (!startGatePath(villager, state, route.far)) {
                clearSheep(villager, state, "opened gate but no post-open path to far side=" + route.far);
                return true;
            }
            log(villager, "crossing gate={} toward far={} sheepId={}", route.gate, route.far, state.sheep.getId());
            return true;
        }
        if (route.stage == GateRouteRules.Stage.CROSS_ENTRY) {
            if (!level.getBlockState(route.gate).getValue(FenceGateBlock.OPEN)) {
                if (route.ownsOpen) invalidateGateOwnership(level, route, "closed-externally");
                clearSheep(villager, state, "gate closed externally before entry crossing");
                return true;
            }
            if (crossedTo(villager, level, route.far, route.gate)) {
                stopCustomNavigation(villager, state);
                route.stage = GateRouteRules.advance(route.stage, true);
                route.deadline = villager.tickCount + 40;
                log(villager, "gate crossed inward gate={} far={}", route.gate, route.far);
            } else {
                if (villager.tickCount - route.lastPathAt >= 20) {
                    if (!startGatePath(villager, state, route.far)) {
                        clearSheep(villager, state, "opened gate but post-open path lost");
                        return true;
                    }
                    route.lastPathAt = villager.tickCount;
                }
                return true;
            }
        }
        if (route.stage == GateRouteRules.Stage.CLOSE_ENTRY) {
            if (!finishGateClosure(villager, level, route, false)) return true;
            route.stage = GateRouteRules.advance(route.stage, true);
            route.deadline = villager.tickCount + NAVIGATION_TIMEOUT;
            state.navigationDeadline = route.deadline;
            log(villager, "continuing to target sheep id={} interaction={} after gate={}",
                    state.sheep.getId(), route.interaction, route.gate);
        }
        return route.stage != GateRouteRules.Stage.APPROACH_SHEEP;
    }

    private static boolean tickGateReturn(Villager villager, ServerLevel level, BlockPos loom, State state) {
        GateRoute route = state.gateRoute;
        if (route == null) return false;
        if (!sameGate(level, route)) {
            log(villager, "gate changed/disappeared during exit gate={}; no forced passage", route.gate);
            if (route.ownsOpen) invalidateGateOwnership(level, route, "external-change");
            state.gateRoute = null;
            return false;
        }
        if (villager.tickCount > route.deadline && route.stage != GateRouteRules.Stage.CLOSE_EXIT) {
            log(villager, "gate exit navigation/crossing timeout gate={} stage={}", route.gate, route.stage);
            safeRestoreGate(villager, level, route, "exit timeout");
            if (isOnFarSide(villager, route)) {
                route.stage = GateRouteRules.Stage.APPROACH_EXIT;
                route.deadline = villager.tickCount + NAVIGATION_TIMEOUT;
                route.lastPathAt = 0;
                return true;
            }
            if (route.ownsOpen) {
                route.stage = GateRouteRules.Stage.CANCELLED;
                route.lastPathAt = villager.tickCount - 20;
                return true;
            }
            state.gateRoute = null;
            return false;
        }
        if (route.stage == GateRouteRules.Stage.APPROACH_EXIT) {
            if (atBlock(villager, route.far)) {
                stopCustomNavigation(villager, state);
                route.stage = GateRouteRules.advance(route.stage, true);
                log(villager, "arrived at exit gate={} inside={}", route.gate, route.far);
            } else {
                if (route.lastPathAt == 0 || villager.tickCount - route.lastPathAt >= 20) {
                    if (!startGatePath(villager, state, route.far)) {
                        log(villager, "no path to known exit gate={} inside={}", route.gate, route.far);
                        route.lastPathAt = villager.tickCount;
                        return true;
                    }
                    route.lastPathAt = villager.tickCount;
                    if (!route.exitLogged) {
                        log(villager, "returning to gate with wool={} gate={} inside={}",
                                containsWool(((OwnedOutput)villager).villagerWork$ownedOutput()), route.gate, route.far);
                        route.exitLogged = true;
                    }
                }
                return true;
            }
        }
        if (route.stage == GateRouteRules.Stage.OPEN_EXIT) {
            if (!atBlock(villager, route.far) || villager.distanceToSqr(Vec3.atCenterOf(route.gate)) > 2.5 * 2.5) {
                log(villager, "exit gate interaction range lost gate={}", route.gate);
                route.stage = GateRouteRules.Stage.APPROACH_EXIT;
                route.lastPathAt = 0;
                return true;
            }
            if (route.externalHold && !level.getBlockState(route.gate).getValue(FenceGateBlock.OPEN))
                return true; // Another actor repeatedly closed it; wait for that actor to open it.
            if (!openGate(villager, level, route, "exit")) {
                log(villager, "gate could not be opened for exit gate={}", route.gate);
                route.stage = GateRouteRules.Stage.APPROACH_EXIT;
                route.lastPathAt = 0;
                return true;
            }
            route.stage = GateRouteRules.advance(route.stage, true);
            route.deadline = villager.tickCount + GATE_CROSS_TIMEOUT;
            route.lastPathAt = villager.tickCount;
            if (!startGatePath(villager, state, route.near)) {
                log(villager, "opened gate but no post-open exit path gate={} outside={}", route.gate, route.near);
                return true;
            }
            return true;
        }
        if (route.stage == GateRouteRules.Stage.CROSS_EXIT) {
            if (!level.getBlockState(route.gate).getValue(FenceGateBlock.OPEN)) {
                if (route.ownsOpen) invalidateGateOwnership(level, route, "closed-externally");
                route.externalChanges++;
                route.externalHold = route.externalChanges >= 2;
                route.stage = GateRouteRules.Stage.APPROACH_EXIT;
                route.deadline = villager.tickCount + NAVIGATION_TIMEOUT;
                route.lastPathAt = 0;
                log(villager, "gate closed externally before outward crossing gate={} changes={} holdUntilExternallyOpened={}",
                        route.gate, route.externalChanges, route.externalHold);
                return true;
            }
            if (crossedTo(villager, level, route.near, route.gate)) {
                stopCustomNavigation(villager, state);
                route.stage = GateRouteRules.advance(route.stage, true);
                route.deadline = villager.tickCount + 40;
                log(villager, "crossed gate outward gate={} outside={}", route.gate, route.near);
            } else {
                if (villager.tickCount - route.lastPathAt >= 20) {
                    if (!startGatePath(villager, state, route.near)) {
                        log(villager, "post-open exit path lost gate={}", route.gate);
                        route.lastPathAt = villager.tickCount;
                        return true;
                    }
                    route.lastPathAt = villager.tickCount;
                }
                return true;
            }
        }
        if (route.stage == GateRouteRules.Stage.CLOSE_EXIT) {
            if (!finishGateClosure(villager, level, route, true)) return true;
            route.stage = GateRouteRules.advance(route.stage, true);
            log(villager, "returning to loom={} after gate exit={}", loom, route.gate);
            state.gateRoute = null;
        }
        return false;
    }

    private static boolean isGateExitStage(GateRouteRules.Stage stage) {
        return stage == GateRouteRules.Stage.APPROACH_EXIT || stage == GateRouteRules.Stage.OPEN_EXIT
                || stage == GateRouteRules.Stage.CROSS_EXIT || stage == GateRouteRules.Stage.CLOSE_EXIT;
    }

    private static void cleanupGateExit(Villager villager, ServerLevel level, State state) {
        if (state.gateRoute != null && state.gateRoute.stage == GateRouteRules.Stage.CANCELLED) {
            retryGateRestoration(villager, level, state);
            return;
        }
        if (state.gateRoute == null || !isGateExitStage(state.gateRoute.stage) || !villager.isAlive()
                || villager.isSleeping() || villager.isTrading()
                || villager.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)) return;
        // Finishing an already-started gate crossing is safety cleanup, not new work or a new sheep scan.
        tickGateReturn(villager, level, state.gateRoute.loom, state);
    }

    /** Save only an exit obligation; target sheep and synthetic props never survive a reload. */
    public static void saveGateExit(Villager villager, ValueOutput output) {
        State state = STATES.get(villager);
        if (state == null || state.gateRoute == null || !(villager.level() instanceof ServerLevel level)) return;
        GateRoute route = state.gateRoute;
        if (!isOnFarSide(villager, route) && !villager.getBoundingBox().intersects(new AABB(route.gate))) {
            safeRestoreGate(villager, level, route, "saving outside pen");
            if (!route.ownsOpen) return;
            route.stage = GateRouteRules.Stage.CANCELLED;
        }
        output.putLong("VillagerWorkExitGate", route.gate.asLong());
        output.putLong("VillagerWorkExitNear", route.near.asLong());
        output.putLong("VillagerWorkExitFar", route.far.asLong());
        output.putLong("VillagerWorkExitLoom", route.loom.asLong());
        output.putString("VillagerWorkExitFacing", route.facing.name());
        output.putBoolean("VillagerWorkExitInitiallyOpen", route.initiallyOpen);
        output.putBoolean("VillagerWorkExitOwnsOpen", route.ownsOpen);
        output.putBoolean("VillagerWorkExitRestoreOnly", route.stage == GateRouteRules.Stage.CANCELLED
                || !isOnFarSide(villager, route));
        output.putString("VillagerWorkExitRouteId", route.routeId.toString());
    }

    public static void loadGateExit(Villager villager, ValueInput input) {
        var gate = input.getLong("VillagerWorkExitGate");
        if (gate.isEmpty()) return;
        String facingName = input.getStringOr("VillagerWorkExitFacing", "");
        Direction facing;
        try { facing = Direction.valueOf(facingName); }
        catch (IllegalArgumentException exception) { return; }
        UUID routeId;
        try { routeId = UUID.fromString(input.getStringOr("VillagerWorkExitRouteId", UUID.randomUUID().toString())); }
        catch (IllegalArgumentException exception) { routeId = UUID.randomUUID(); }
        State state = STATES.computeIfAbsent(villager, ignored -> new State());
        state.pendingExit = new PendingExit(BlockPos.of(gate.get()),
                BlockPos.of(input.getLongOr("VillagerWorkExitNear", gate.get())),
                BlockPos.of(input.getLongOr("VillagerWorkExitFar", gate.get())),
                BlockPos.of(input.getLongOr("VillagerWorkExitLoom", gate.get())), facing,
                input.getBooleanOr("VillagerWorkExitInitiallyOpen", false),
                input.getBooleanOr("VillagerWorkExitOwnsOpen", false),
                input.getBooleanOr("VillagerWorkExitRestoreOnly", false), routeId);
        if (villager.level() instanceof ServerLevel level) restorePendingExit(villager, level, state);
    }

    private static void restorePendingExit(Villager villager, ServerLevel level, State state) {
        PendingExit saved = state.pendingExit;
        if (!level.hasChunkAt(saved.gate)) return;
        state.pendingExit = null;
        BlockState blockState = level.getBlockState(saved.gate);
        if (!(blockState.getBlock() instanceof FenceGateBlock)
                || blockState.getValue(FenceGateBlock.FACING) != saved.facing
                || !saved.near.equals(saved.gate.relative(saved.facing))
                && !saved.near.equals(saved.gate.relative(saved.facing.getOpposite()))
                || !saved.far.equals(saved.gate.relative(saved.facing))
                && !saved.far.equals(saved.gate.relative(saved.facing.getOpposite()))
                || saved.near.equals(saved.far)) {
            log(villager, "saved gate exit discarded: gate changed or sides invalid gate={}", saved.gate);
            return;
        }
        GateRouteRules.Candidate candidate = new GateRouteRules.Candidate(saved.gate, saved.near,
                saved.far, saved.far, 0, 0, true, true);
        GateRoute route = new GateRoute(candidate, saved.loom, blockState.getBlock(), saved.facing,
                saved.initiallyOpen, villager.tickCount + NAVIGATION_TIMEOUT, saved.routeId);
        route.ownsOpen = saved.ownsOpen && blockState.getValue(FenceGateBlock.OPEN)
                && LivestockGateBlocker.activate(level, route.gate, route.owner(villager));
        route.stage = saved.restoreOnly || !isOnFarSide(villager, route)
                ? GateRouteRules.Stage.CANCELLED : GateRouteRules.Stage.APPROACH_EXIT;
        if (route.stage == GateRouteRules.Stage.CANCELLED && !route.ownsOpen) return;
        if (route.stage == GateRouteRules.Stage.CANCELLED) {
            route.lastPathAt = villager.tickCount - 20;
            state.gateRoute = route;
            retryGateRestoration(villager, level, state);
            return;
        }
        state.gateRoute = route;
        log(villager, "resumed saved gate exit gate={} inside={} outside={} ownedOpen={}",
                route.gate, route.far, route.near, route.ownsOpen);
    }

    public static void releaseGate(Villager villager) {
        State state = STATES.get(villager);
        if (state == null) return;
        if (state.gateRoute != null && villager.level() instanceof ServerLevel level) {
            safeRestoreGate(villager, level, state.gateRoute, "villager removed/converted");
            if (state.gateRoute.ownsOpen)
                relinquishGateOwnership(villager, level, state.gateRoute, "villager-removed/converted");
        }
        state.gateRoute = null;
        state.pendingExit = null;
        stopCustomNavigation(villager, state);
        clearProp(villager);
    }

    /** Chunk unloads have already saved/restored the hand; permanent removals relinquish leases. */
    public static void onRemoval(Villager villager) {
        Entity.RemovalReason reason = villager.getRemovalReason();
        clearProp(villager);
        if (reason == null || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK
                || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) return;
        releaseGate(villager);
        STATES.remove(villager);
    }

    private static boolean startGatePath(Villager villager, State state, BlockPos goal) {
        if (atBlock(villager, goal)) return true;
        Path path = villager.getNavigation().createPath(goal, 0);
        if (!exactEndpoint(path, goal) || !villager.getNavigation().moveTo(path, 0.6)) return false;
        state.customNavigation = true;
        state.customPath = path;
        return true;
    }

    private static boolean exactEndpoint(Path path, BlockPos goal) {
        return path != null && path.getNodeCount() > 0 && goal.equals(path.getEndNode().asBlockPos());
    }

    private static BlockPos pathEndpoint(Path path) {
        return path == null || path.getNodeCount() == 0 ? null : path.getEndNode().asBlockPos();
    }

    private static boolean atBlock(Villager villager, BlockPos feet) {
        return villager.blockPosition().equals(feet)
                && villager.distanceToSqr(Vec3.atBottomCenterOf(feet)) <= 1.2 * 1.2;
    }

    private static boolean crossedTo(Villager villager, ServerLevel level,
                                     BlockPos destination, BlockPos gate) {
        if (!atBlock(villager, destination) || !level.hasChunkAt(gate)) return false;
        BlockState state = level.getBlockState(gate);
        if (!(state.getBlock() instanceof FenceGateBlock)) return false;
        VoxelShape closedShape = state.setValue(FenceGateBlock.OPEN, false)
                .getCollisionShape(level, gate);
        return !intersectsShape(villager, closedShape, gate);
    }

    private static boolean gatePassageHasRoom(ServerLevel level, Villager villager, BlockPos gate) {
        if (!level.hasChunkAt(gate) || !level.hasChunkAt(gate.above()) || !level.hasChunkAt(gate.below())) return false;
        return level.getBlockState(gate).getBlock() instanceof FenceGateBlock
                && level.getBlockState(gate.above()).getCollisionShape(level, gate.above()).isEmpty()
                && level.getFluidState(gate).isEmpty() && level.getFluidState(gate.above()).isEmpty()
                && level.getBlockState(gate.below()).isFaceSturdy(level, gate.below(), Direction.UP);
    }

    private static boolean sameGate(ServerLevel level, GateRoute route) {
        return level.hasChunkAt(route.gate) && level.getBlockState(route.gate).getBlock() == route.block
                && level.getBlockState(route.gate).getValue(FenceGateBlock.FACING) == route.facing;
    }

    private static boolean openGate(Villager villager, ServerLevel level, GateRoute route, String phase) {
        if (!sameGate(level, route)) return false;
        BlockState before = level.getBlockState(route.gate);
        if (before.getValue(FenceGateBlock.OPEN)) {
            if (route.ownsOpen && !LivestockGateBlocker.validate(level, route.gate)) {
                route.ownsOpen = false;
                log(villager, "owned gate blocker invalidated before {} traversal gate={}", phase, route.gate);
            }
            if (!route.ownsOpen && LivestockGateBlocker.joinActive(level, route.gate, route.owner(villager))) {
                route.ownsOpen = true;
                log(villager, "joined active VWR gate blocker for {} traversal gate={}", phase, route.gate);
            }
            log(villager, "gate already open for {} gate={}; routineOwned={}", phase, route.gate, route.ownsOpen);
            return true;
        }
        if (!level.setBlock(route.gate, before.setValue(FenceGateBlock.OPEN, true), 2)) return false;
        // This specific closed-to-open mutation is ours even when the gate happened to be open
        // when the route was first selected and somebody closed it in the meantime.
        route.ownsOpen = GateRouteRules.ownsOpenTransition(false, true);
        if (route.ownsOpen && !LivestockGateBlocker.activate(level, route.gate, route.owner(villager))) {
            BlockState opened = level.getBlockState(route.gate);
            if (sameGate(level, route) && opened.getValue(FenceGateBlock.OPEN))
                level.setBlock(route.gate, opened.setValue(FenceGateBlock.OPEN, false), 2);
            route.ownsOpen = false;
            log(villager, "gate blocker activation failed; rolled back owned open gate={} phase={}", route.gate, phase);
            return false;
        }
        level.playSound(null, route.gate, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(villager, GameEvent.BLOCK_OPEN, route.gate);
        log(villager, "opened gate for {} gate={} routineOwned={}", phase, route.gate, route.ownsOpen);
        return true;
    }

    private static boolean finishGateClosure(Villager villager, ServerLevel level, GateRoute route, boolean exit) {
        if (!route.ownsOpen) return true;
        if (!sameGate(level, route)) {
            log(villager, "gate closure unsafe: gate changed/disappeared gate={}", route.gate);
            invalidateGateOwnership(level, route, "external-change");
            return true;
        }
        BlockState before = level.getBlockState(route.gate);
        BlockState closed = before.setValue(FenceGateBlock.OPEN, false);
        boolean powered = before.getValue(FenceGateBlock.POWERED) || level.hasNeighborSignal(route.gate);
        boolean passageClear = closedGateCollisionClear(level, route.gate, closed, villager);
        boolean physicallyCleared = !intersectsShape(villager, closed.getCollisionShape(level, route.gate), route.gate);
        boolean otherOwners = LivestockGateBlocker.hasOtherOwners(level, route.gate, route.owner(villager));
        GateRouteRules.ClosurePlan closure = GateRouteRules.planClosure(route.ownsOpen, true,
                before.getValue(FenceGateBlock.OPEN), false, powered, passageClear,
                physicallyCleared, otherOwners);
        if (closure != GateRouteRules.ClosurePlan.ATTEMPT_CLOSE) {
            if (closure == GateRouteRules.ClosurePlan.RELEASE_EXTERNAL) {
                log(villager, "gate closure unsafe gate={} powered={} passageClear={} physicallyCleared={} open={} phase={}",
                        route.gate, powered, passageClear, physicallyCleared,
                        before.getValue(FenceGateBlock.OPEN), exit ? "exit" : "entry");
                String reason = powered ? "powered" : "closed-externally";
                invalidateGateOwnership(level, route, reason);
                return true;
            }
            if (closure == GateRouteRules.ClosurePlan.RELEASE_SHARED) {
                log(villager, "shared VWR gate remains open for another owner gate={} phase={}",
                        route.gate, exit ? "exit" : "entry");
                relinquishGateOwnership(villager, level, route, "shared-owner-complete");
                return true;
            }
            if (closure == GateRouteRules.ClosurePlan.COMPLETE_UNOWNED) return true;
            // An occupied closed-gate collision plane is not a successful timeout: retain both
            // the closure obligation and livestock blocker until a later safe close.
            if (villager.tickCount >= route.nextClosureLog) {
                log(villager, "owned gate closure waiting for collision plane to clear gate={} passageClear={} physicallyCleared={} phase={}",
                        route.gate, passageClear, physicallyCleared, exit ? "exit" : "entry");
                route.nextClosureLog = villager.tickCount + 40;
            }
            return false;
        }
        if (level.setBlock(route.gate, closed, 2)) {
            level.playSound(null, route.gate, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(villager, GameEvent.BLOCK_CLOSE, route.gate);
            log(villager, "closed gate after {} gate={}", exit ? "exit" : "entry", route.gate);
            invalidateGateOwnership(level, route, "closed/restored");
            return true;
        }
        if (villager.tickCount >= route.nextClosureLog) {
            log(villager, "owned gate closure setBlock failed; retaining blocker and retrying gate={}", route.gate);
            route.nextClosureLog = villager.tickCount + 40;
        }
        return false;
    }

    /** Only the prospective closed gate's thin collision plane can make closure unsafe. */
    private static boolean closedGateCollisionClear(ServerLevel level, BlockPos gate,
                                                    BlockState closed, Villager villager) {
        VoxelShape shape = closed.getCollisionShape(level, gate);
        if (shape.isEmpty()) return true;
        AABB bounds = shape.bounds().move(gate.getX(), gate.getY(), gate.getZ());
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                        entity -> entity.isAlive() && entity != villager)
                .stream().noneMatch(entity -> intersectsShape(entity, shape, gate));
    }

    private static boolean intersectsShape(Entity entity, VoxelShape shape, BlockPos pos) {
        if (shape.isEmpty()) return false;
        return shape.toAabbs().stream()
                .map(box -> box.move(pos.getX(), pos.getY(), pos.getZ()))
                .anyMatch(box -> box.intersects(entity.getBoundingBox()));
    }

    private static void relinquishGateOwnership(Villager villager, ServerLevel level,
                                                GateRoute route, String reason) {
        LivestockGateBlocker.release(level, route.gate, route.owner(villager), reason);
        route.ownsOpen = false;
    }

    private static void invalidateGateOwnership(ServerLevel level, GateRoute route, String reason) {
        LivestockGateBlocker.invalidateGate(level, route.gate, reason);
        route.ownsOpen = false;
    }

    private static void safeRestoreGate(Villager villager, ServerLevel level, GateRoute route, String reason) {
        if (!route.ownsOpen) return;
        log(villager, "safe gate restoration requested gate={} reason={}", route.gate, reason);
        finishGateClosure(villager, level, route, false);
    }

    private static void retryGateRestoration(Villager villager, ServerLevel level, State state) {
        GateRoute route = state.gateRoute;
        if (route == null || route.stage != GateRouteRules.Stage.CANCELLED) return;
        if (!route.ownsOpen || !sameGate(level, route)) {
            if (route.ownsOpen) invalidateGateOwnership(level, route, "external-change");
            state.gateRoute = null;
            return;
        }
        if (villager.tickCount - route.lastPathAt < 20) return;
        route.lastPathAt = villager.tickCount;
        if (finishGateClosure(villager, level, route, false)) state.gateRoute = null;
    }

    private static List<BlockPos> sheepPositions(Sheep sheep, Villager villager, ServerLevel level) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos origin = sheep.blockPosition();
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;
            BlockPos feet = origin.offset(dx, dy, dz);
            if (standingIsClear(level, villager, feet)
                    && Vec3.atBottomCenterOf(feet).distanceToSqr(sheep.position())
                    <= ShearActionRules.IDEAL_APPROACH_DISTANCE * ShearActionRules.IDEAL_APPROACH_DISTANCE
                    && clearShearLine(level, Vec3.atBottomCenterOf(feet).add(0, 0.9, 0), sheep))
                positions.add(feet);
        }
        positions.sort(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(villager.blockPosition()))
                .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        return positions;
    }

    private static SheepPathAttempt trySheepPath(Villager villager, Sheep sheep,
                                                  BlockPos requested, ServerLevel level) {
        Path path = villager.getNavigation().createPath(requested, 0);
        if (path == null) return new SheepPathAttempt(null, requested, null, null, null, null, "pathNull");
        BlockPos reportedTarget = path.getTarget();
        boolean canReach = path.canReach();
        BlockPos endpoint = path.getNodeCount() == 0 ? null : path.getEndNode().asBlockPos();
        if (endpoint == null)
            return new SheepPathAttempt(path, requested, reportedTarget, null, canReach, null, "emptyPath");
        boolean endpointLineClear = clearShearLine(level,
                Vec3.atBottomCenterOf(endpoint).add(0, 0.9, 0), sheep);
        // A partial vanilla path can end at a usable neighboring node. The endpoint, not the
        // reported target, is what the villager can actually approach. World checks below still
        // require a safe standing block and a clear shear line from that exact endpoint.
        if (!SheepPathEndpoint.nearRequested(requested, endpoint))
            return new SheepPathAttempt(path, requested, reportedTarget, endpoint, canReach, null,
                    "endpointOutsideTolerance; barrierToSheep=" + !endpointLineClear);
        if (!standingIsClear(level, villager, endpoint))
            return new SheepPathAttempt(path, requested, reportedTarget, endpoint, canReach, null,
                    "endpointNotWalkable");
        if (Vec3.atBottomCenterOf(endpoint).distanceToSqr(sheep.position())
                > ShearActionRules.IDEAL_APPROACH_DISTANCE * ShearActionRules.IDEAL_APPROACH_DISTANCE)
            return new SheepPathAttempt(path, requested, reportedTarget, endpoint, canReach, null,
                    "endpointTooFarFromSheepForTelegraph");
        if (!endpointLineClear)
            return new SheepPathAttempt(path, requested, reportedTarget, endpoint, canReach, null,
                    "endpointShearLineBlocked; barrierToSheep=true");
        boolean moved = villager.getNavigation().moveTo(path, 0.6);
        return new SheepPathAttempt(path, requested, reportedTarget, endpoint, canReach, moved,
                moved ? "accepted" : "moveToRejected");
    }

    private record SheepPathAttempt(Path path, BlockPos requested, BlockPos reportedTarget,
                                    BlockPos endpoint, Boolean canReach, Boolean moveAccepted, String reason) {
        boolean accepted() { return "accepted".equals(reason); }

        String summary() {
            String requestedToTarget = reportedTarget == null ? "n/a"
                    : String.format("%.2f", Math.sqrt(requested.distSqr(reportedTarget)));
            String requestedToEndpoint = endpoint == null ? "n/a"
                    : String.format("%.2f", Math.sqrt(requested.distSqr(endpoint)));
            return "requested=" + requested + " createPath=" + (path == null ? "null" : "present")
                    + " pathNull=" + (path == null) + " canReach=" + canReach
                    + " reportedTarget=" + reportedTarget + " endpoint=" + endpoint
                    + " targetDelta=" + requestedToTarget + " endpointDelta=" + requestedToEndpoint
                    + " nodes=" + (path == null ? 0 : path.getNodeCount())
                    + " moveTo=" + moveAccepted + " reason=" + reason;
        }
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
        return feet != null && sheep.readyForShearing()
                && standingIsClear(level, villager, feet)
                && standingIsClear(level, villager, villager.blockPosition())
                && villager.blockPosition().equals(feet)
                && villager.distanceToSqr(Vec3.atBottomCenterOf(feet)) <= 1.0
                && villager.distanceToSqr(sheep)
                <= ShearActionRules.IDEAL_APPROACH_DISTANCE * ShearActionRules.IDEAL_APPROACH_DISTANCE
                && clearShearLine(level, villager.position().add(0, 0.9, 0), sheep);
    }

    private static String sheepInvalidReason(Sheep sheep, Villager villager, BlockPos loom, State state) {
        if (!sheep.isAlive()) return "selected sheep died";
        if (sheep.isBaby()) return "selected sheep is a baby";
        if (!sheep.readyForShearing()) return "selected sheep is no longer shearable (possibly sheared externally)";
        if (sheep.distanceToSqr(Vec3.atCenterOf(loom)) > 16 * 16)
            return "selected sheep left the bounded routine area around the claimed loom";
        if (state.gateRoute == null && sheep.distanceToSqr(villager) > 18 * 18)
            return "selected sheep moved beyond the direct-task locality bound";
        return null;
    }

    private static ShearActionRules.Decision shearDecision(ShearActionRules.Phase phase,
                                                            Sheep sheep, Villager villager,
                                                            BlockPos loom, State state,
                                                            boolean outputAvailable,
                                                            double distance, boolean footingSafe,
                                                            boolean clearLine) {
        boolean local = sheep.distanceToSqr(Vec3.atCenterOf(loom)) <= 16 * 16
                && (state.gateRoute != null || sheep.distanceToSqr(villager) <= 18 * 18);
        return ShearActionRules.decide(phase,
                new ShearActionRules.Eligibility(villager.getBrain().isActive(Activity.WORK),
                        sheep.isAlive(), !sheep.isBaby(), sheep.readyForShearing(), local, outputAvailable),
                new ShearActionRules.Geometry(distance, footingSafe, clearLine));
    }

    /** Repaths toward the retained sheep only; it never performs another gate discovery scan. */
    private static boolean repositionSameSheep(Villager villager, Sheep sheep,
                                               ServerLevel level, State state, String reason) {
        state.nextSheepPathAt = villager.tickCount + 10;
        stopCustomNavigation(villager, state);
        List<BlockPos> nearby = sheepPositions(sheep, villager, level);
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        if (state.sheepRequested != null && nearby.contains(state.sheepRequested))
            positions.add(state.sheepRequested);
        positions.addAll(nearby);
        List<String> attempts = new ArrayList<>();
        for (BlockPos position : positions) {
            if (validSheepInteraction(villager, sheep, position, level)) {
                state.sheepRequested = position;
                state.sheepTarget = position;
                log(villager, "repositioned same target sheep id={} reason={} interaction={} already arrived; gate route retained={}",
                        sheep.getId(), reason, position, state.gateRoute != null);
                return true;
            }
            SheepPathAttempt attempt = trySheepPath(villager, sheep, position, level);
            attempts.add(attempt.summary());
            if (!attempt.accepted()) continue;
            state.sheepRequested = position;
            state.sheepTarget = attempt.endpoint();
            state.customNavigation = true;
            state.customPath = attempt.path();
            log(villager, "repositioning same target sheep id={} reason={} interaction={} endpoint={} distance={} without gate rescan",
                    sheep.getId(), reason, position, attempt.endpoint(), Math.sqrt(villager.distanceToSqr(sheep)));
            return true;
        }
        if (villager.tickCount >= state.nextRepositionLog) {
            log(villager, "same target sheep reposition pending id={} reason={} positions={} attempts={}",
                    sheep.getId(), reason, positions.size(), attempts);
            state.nextRepositionLog = villager.tickCount + 40;
        }
        return false;
    }

    private static void restartSheepApproach(Villager villager, Sheep sheep, ServerLevel level,
                                             State state, ShearActionRules.Reason reason) {
        log(villager, "sheep moved or geometry changed during telegraph id={} reason={} distance={}; repositioning same target and retaining gate={}",
                sheep.getId(), reason, Math.sqrt(villager.distanceToSqr(sheep)),
                state.gateRoute == null ? "none" : state.gateRoute.gate);
        state.shearAt = 0;
        state.clearPropAt = 0;
        state.repositioningSheep = true;
        state.telegraphRestarts++;
        state.navigationDeadline = villager.tickCount + NAVIGATION_TIMEOUT;
        state.nextSheepPathAt = villager.tickCount;
        clearProp(villager);
        repositionSameSheep(villager, sheep, level, state, reason.toString());
    }

    private static void clearSheep(Villager villager, State state, String reason) {
        if (state.sheep != null) log(villager, "shepherd cancelled sheep id={} interaction={}: {}",
                state.sheep.getId(), state.sheepTarget, reason);
        stopCustomNavigation(villager, state);
        if (state.gateRoute != null) {
            GateRoute route = state.gateRoute;
            if (villager.level() instanceof ServerLevel level)
                safeRestoreGate(villager, level, route, reason);
            if (isOnFarSide(villager, route) && sameGate((ServerLevel)villager.level(), route)) {
                route.stage = GateRouteRules.Stage.APPROACH_EXIT;
                route.deadline = villager.tickCount + NAVIGATION_TIMEOUT;
                route.lastPathAt = 0;
                log(villager, "selected sheep invalid inside pen; returning through known gate={} reason={}",
                        route.gate, reason);
            } else if (route.ownsOpen) {
                route.stage = GateRouteRules.cancel(route.stage);
                route.lastPathAt = villager.tickCount - 20;
                log(villager, "gate restoration pending until passage clears gate={} reason={}", route.gate, reason);
            } else state.gateRoute = null;
        }
        state.sheep = null;
        state.sheepRequested = null;
        state.sheepTarget = null;
        state.shearAt = 0;
        state.clearPropAt = 0;
        state.repositioningSheep = false;
        state.telegraphRestarts = 0;
        state.nextSheepPathAt = 0;
        clearProp(villager);
    }

    private static boolean isOnFarSide(Villager villager, GateRoute route) {
        Vec3 center = Vec3.atCenterOf(route.gate);
        Vec3 far = Vec3.atCenterOf(route.far).subtract(center);
        Vec3 position = villager.position().subtract(center);
        return position.x * far.x + position.z * far.z > 0.55;
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
                if (inserted > 0) presentFishDeposit(villager, level, site);
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
            state.fishingPhase = FishingRodLifecycle.Phase.FLOAT_ACTIVE;
            holdFishingPosition(villager);
            villager.getLookControl().setLookAt(Vec3.atCenterOf(state.water));
            if (villager.tickCount >= state.catchAt) {
                state.fishingPhase = FishingRodLifecycle.Phase.RETRIEVING;
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
                state.fishingPhase = FishingRodLifecycle.afterRetrieve(containsFish(owned));
            }
            return;
        }
        if (containsFish(owned)) {
            state.fishingPhase = FishingRodLifecycle.Phase.RETURNING_TO_BARREL;
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
        if (state.bank == null) {
            state.fishingPhase = FishingRodLifecycle.Phase.IDLE;
            return;
        }
        if (!openWater(level, state.water) || !standingIsClear(level, villager, state.bank)) {
            cancel(villager, state, "selected water or bank became invalid");
            return;
        }
        if (villager.tickCount > state.navigationDeadline && state.rodAt == 0) {
            cancel(villager, state, "bank navigation timed out");
            return;
        }
        if (villager.distanceToSqr(Vec3.atBottomCenterOf(state.bank)) > 1.1 * 1.1) {
            state.fishingPhase = FishingRodLifecycle.Phase.NAVIGATING_TO_BANK;
            if (state.rodAt != 0) {
                // Ordinary villager steering can nudge a valid Fisherman off the exact bank after
                // the rod telegraph. Keep the same vetted bank/water pair and retry the telegraph.
                state.rodAt = 0;
                state.navigationDeadline = villager.tickCount + NAVIGATION_TIMEOUT;
                log(villager, "moved slightly away from bank={} before cast; repositioning and restarting cast telegraph",
                        state.bank);
            }
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
        holdFishingPosition(villager);
        villager.getLookControl().setLookAt(Vec3.atCenterOf(state.water));
        if (state.rodAt == 0) {
            state.fishingPhase = FishingRodLifecycle.Phase.CAST_TELEGRAPH;
            state.rodAt = villager.tickCount + 8;
            log(villager, "arrived bank={} facing water={}; cast telegraph begins castAt={}", state.bank, state.water, state.rodAt);
            return;
        }
        if (villager.tickCount < state.rodAt) return;
        villager.swing(InteractionHand.MAIN_HAND);
        state.fishingPhase = FishingRodLifecycle.Phase.FLOAT_ACTIVE;
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
            state.fishingPhase = FishingRodLifecycle.Phase.NAVIGATING_TO_BANK;
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

    /** Gives a successful claimed-barrel transfer one compact, server-authoritative interaction cue. */
    private static void presentFishDeposit(Villager villager, ServerLevel level, BlockPos barrel) {
        villager.getLookControl().setLookAt(Vec3.atCenterOf(barrel));
        villager.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, barrel, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.75f, 1.0f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, barrel.getX() + 0.5, barrel.getY() + 0.7,
                barrel.getZ() + 0.5, 5, 0.22, 0.20, 0.22, 0.01);
        level.gameEvent(villager, GameEvent.BLOCK_OPEN, barrel);
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

    /** A live bobber is the Fisherman's committed action: do not let ambient navigation wander it. */
    private static void holdFishingPosition(Villager villager) {
        villager.getNavigation().stop();
        Vec3 velocity = villager.getDeltaMovement();
        if (velocity.x != 0.0 || velocity.z != 0.0)
            villager.setDeltaMovement(0.0, velocity.y, 0.0);
    }

    private static boolean showProp(Villager villager, State state, Item item) {
        // Keep a stable transaction reference for this entire invocation.  setItemInHand may run
        // vanilla callbacks which can clear/suspend the state, so never dereference the mutable
        // slot again after changing equipment.
        TemporaryHandProp prop = state.propSlot.current();
        if (prop != null && prop.intendedItem() != item) {
            clearProp(villager, state, prop);
            prop = state.propSlot.current();
            if (prop != null && prop.intendedItem() != item) return false;
        }
        ItemStack held = villager.getItemInHand(InteractionHand.MAIN_HAND);
        float dropChance = villager.getDropChances().byEquipment(EquipmentSlot.MAINHAND);
        if (prop == null) {
            prop = state.propSlot.beginIfAbsent(villager.getUUID(), UUID.randomUUID(), item, held, dropChance);
            if (prop.intendedItem() != item) return false;
            TemporaryHandProp.Overlay overlay = prop.overlay();
            applyHandState(villager, overlay.stack(), overlay.dropChance());
            log(villager, "synthetic {} overlay started; original main-hand state preserved stack={} dropChance={} action={}",
                    propName(item), prop.originalState().stack(), prop.originalState().dropChance(), prop.actionId());
            return state.propSlot.isCurrent(prop) && (state.propSlot.isSuspended(prop)
                    || prop.owns(villager.getItemInHand(InteractionHand.MAIN_HAND)));
        }

        boolean wasSuspended = state.propSlot.isSuspended(prop);
        TemporaryHandProp.Reconcile reconcile = wasSuspended
                ? prop.resume(held, dropChance)
                : prop.reconcile(held, dropChance);
        if (reconcile.kind() == TemporaryHandProp.ReconcileKind.FOREIGN_VWR_PRESENTATION) {
            log(villager, "foreign/stale VWR hand presentation quarantined before synthetic {} overlay stack={}",
                    propName(item), held);
        }
        if (reconcile.applyOverlay() && state.propSlot.isCurrent(prop)) {
            applyHandState(villager, reconcile.overlay().stack(), reconcile.overlay().dropChance());
            if (wasSuspended && state.propSlot.resumeIfCurrent(prop)) {
                log(villager, "synthetic {} overlay restored after save interruption action={}",
                        propName(item), prop.actionId());
            } else if ((reconcile.kind() == TemporaryHandProp.ReconcileKind.EXTERNAL_CLEAR_REBASED
                    || reconcile.kind() == TemporaryHandProp.ReconcileKind.EXTERNAL_REPLACEMENT_REBASED)
                    && villager.tickCount >= state.nextPropConflictLog) {
                log(villager, "synthetic {} unexpectedly replaced/cleared kind={}; latest legitimate main-hand state preserved stack={} dropChance={}; overlay reapplied",
                        propName(item), reconcile.kind(), reconcile.restorationState().stack(),
                        reconcile.restorationState().dropChance());
                state.nextPropConflictLog = villager.tickCount + 100;
            }
        }
        return state.propSlot.isCurrent(prop) && (state.propSlot.isSuspended(prop)
                || prop.owns(villager.getItemInHand(InteractionHand.MAIN_HAND)));
    }

    public static void clearProp(Villager villager) {
        State state = STATES.get(villager);
        if (state == null) return;
        clearProp(villager, state, state.propSlot.current());
    }

    /** Removes the synthetic stack before serialization but retains its action identity in RAM. */
    public static void suspendPropForSave(Villager villager) {
        State state = STATES.get(villager);
        if (state == null) return;
        TemporaryHandProp prop = state.propSlot.current();
        if (prop == null || state.propSlot.isSuspended(prop)) return;
        ItemStack held = villager.getItemInHand(InteractionHand.MAIN_HAND);
        float dropChance = villager.getDropChances().byEquipment(EquipmentSlot.MAINHAND);
        TemporaryHandProp.Restore restore = prop.restore(held, dropChance);
        if (restore.applyRestoration() && state.propSlot.isCurrent(prop))
            applyHandState(villager, restore.handState().stack(), restore.handState().dropChance());
        if (state.propSlot.suspendIfCurrent(prop)) {
            log(villager, "synthetic {} overlay suspended for save action={} cleanup={} restorationApplied={} mainHand={} dropChance={}",
                    propName(prop.intendedItem()), prop.actionId(), restore.kind(), restore.applyRestoration(),
                    villager.getItemInHand(InteractionHand.MAIN_HAND),
                    villager.getDropChances().byEquipment(EquipmentSlot.MAINHAND));
        }
    }

    private static void clearProp(Villager villager, State state, TemporaryHandProp prop) {
        if (prop == null || !state.propSlot.isCurrent(prop)) return;
        ItemStack held = villager.getItemInHand(InteractionHand.MAIN_HAND);
        float dropChance = villager.getDropChances().byEquipment(EquipmentSlot.MAINHAND);
        TemporaryHandProp.Restore restore = prop.restore(held, dropChance);
        if (restore.applyRestoration() && state.propSlot.isCurrent(prop))
            applyHandState(villager, restore.handState().stack(), restore.handState().dropChance());
        if (!state.propSlot.clearIfCurrent(prop)) return;
        log(villager, "synthetic {} overlay cleanup result={} restorationApplied={} mainHand={} dropChance={} action={}",
                propName(prop.intendedItem()), restore.kind(), restore.applyRestoration(),
                villager.getItemInHand(InteractionHand.MAIN_HAND),
                villager.getDropChances().byEquipment(EquipmentSlot.MAINHAND), prop.actionId());
    }

    private static void applyHandState(Villager villager, ItemStack stack, float dropChance) {
        villager.setItemInHand(InteractionHand.MAIN_HAND, stack);
        villager.setDropChance(EquipmentSlot.MAINHAND, dropChance);
    }

    private static String propName(Item item) {
        return item == Items.SHEARS ? "shears" : item == Items.FISHING_ROD ? "fishing rod" : item.toString();
    }

    private static void cancel(Villager villager, State state, String reason) {
        if (state.eligible || state.sheep != null || state.bank != null || state.floatEntity != null)
            log(villager, "custom WORK cancelled: {} site={} sheep={} bank={} water={} float={} interactionTarget={}",
                    reason, state.site, state.sheep == null ? "none" : state.sheep.getId(), state.bank,
                    state.water, state.floatEntity == null ? "none" : state.floatEntity.getId(),
                    interactionTarget(villager));
        if (state.floatEntity != null) state.floatEntity.discard();
        stopCustomNavigation(villager, state);
        if (state.gateRoute != null && villager.level() instanceof ServerLevel level) {
            GateRoute route = state.gateRoute;
            safeRestoreGate(villager, level, route, reason);
            if (villager.isAlive() && sameGate(level, route) && isOnFarSide(villager, route)) {
                route.stage = GateRouteRules.Stage.APPROACH_EXIT;
                route.deadline = villager.tickCount + NAVIGATION_TIMEOUT;
                route.lastPathAt = 0;
            } else if (route.ownsOpen && sameGate(level, route)) {
                route.stage = GateRouteRules.cancel(route.stage);
                route.lastPathAt = villager.tickCount - 20;
            } else state.gateRoute = null;
        }
        state.floatEntity = null;
        state.water = null;
        state.bank = null;
        state.sheep = null;
        state.sheepRequested = null;
        state.sheepTarget = null;
        state.shearAt = 0;
        state.clearPropAt = 0;
        state.repositioningSheep = false;
        state.telegraphRestarts = 0;
        state.nextSheepPathAt = 0;
        state.rodAt = 0;
        state.fishingPhase = FishingRodLifecycle.Phase.CANCELLED;
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
    private record SiteCheck(BlockPos site, String reason) {}
    private record PendingExit(BlockPos gate, BlockPos near, BlockPos far, BlockPos loom,
                               Direction facing, boolean initiallyOpen, boolean ownsOpen,
                               boolean restoreOnly, UUID routeId) {}
    private static final class GateRoute {
        final BlockPos gate;
        final BlockPos near;
        final BlockPos far;
        final BlockPos interaction;
        final BlockPos loom;
        final Block block;
        final Direction facing;
        final boolean initiallyOpen;
        final UUID routeId;
        GateRouteRules.Stage stage = GateRouteRules.Stage.APPROACH_ENTRY;
        int deadline;
        int lastPathAt;
        int nextClosureLog;
        boolean ownsOpen;
        boolean approachLogged;
        boolean exitLogged;
        int externalChanges;
        boolean externalHold;

        GateRoute(GateRouteRules.Candidate candidate, BlockPos loom, Block block, Direction facing,
                   boolean initiallyOpen, int deadline) {
            this(candidate, loom, block, facing, initiallyOpen, deadline, UUID.randomUUID());
        }

        GateRoute(GateRouteRules.Candidate candidate, BlockPos loom, Block block, Direction facing,
                  boolean initiallyOpen, int deadline, UUID routeId) {
            this.gate = candidate.gate();
            this.near = candidate.near();
            this.far = candidate.far();
            this.interaction = candidate.interaction();
            this.loom = loom;
            this.block = block;
            this.facing = facing;
            this.initiallyOpen = initiallyOpen;
            this.deadline = deadline;
            this.routeId = routeId;
        }

        LivestockGateBlocker.Owner owner(Villager villager) {
            return new LivestockGateBlocker.Owner(villager.getUUID(), routeId);
        }
    }
    private static final class State {
        Sheep sheep;
        BlockPos sheepRequested;
        BlockPos sheepTarget;
        GateRoute gateRoute;
        PendingExit pendingExit;
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
        int nextSheepPathAt;
        int nextRepositionLog;
        int cooldown;
        int telegraphRestarts;
        FishingRodLifecycle.Phase fishingPhase = FishingRodLifecycle.Phase.IDLE;
        final TemporaryHandProp.Slot propSlot = new TemporaryHandProp.Slot();
        int nextPropConflictLog;
        boolean eligible;
        boolean repositioningSheep;
        boolean customNavigation;
        Path customPath;
    }
}
