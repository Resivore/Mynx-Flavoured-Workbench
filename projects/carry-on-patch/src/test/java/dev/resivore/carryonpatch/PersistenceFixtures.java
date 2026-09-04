package dev.resivore.carryonpatch;

import java.lang.reflect.*;
import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.resivore.carryonpatch.common.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;
import org.chermew.grabandgo.duck.GrabCarrier;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Executed inside production Knot, using actual patched upstream callbacks and tracked fields.
 * No world, server, network connection or user save is opened. */
public final class PersistenceFixtures {
    public static class FixturePlayer extends Player {
        public FixturePlayer() { super(null, new GameProfile(UUID.randomUUID(), "fixture")); }
        @Override public GameType gameMode() { return GameType.SURVIVAL; }
        @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {}
        @Override public UUID getUUID() { return new UUID(0, 42); }
    }

    private static Player player() throws Exception {
        // Avoid constructing a world/inventory. Define real upstream tracked fields normally.
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field uf = unsafeType.getDeclaredField("theUnsafe"); uf.setAccessible(true);
        Object unsafe = uf.get(null);
        Player p = (Player) unsafeType.getMethod("allocateInstance", Class.class).invoke(unsafe, FixturePlayer.class);
        SynchedEntityData.Builder builder = new SynchedEntityData.Builder(p);
        Map<String,Object> base = Map.of("DATA_SHARED_FLAGS_ID", (byte)0, "DATA_AIR_SUPPLY_ID", 300,
                "DATA_CUSTOM_NAME_VISIBLE", false, "DATA_CUSTOM_NAME", Optional.empty(), "DATA_SILENT", false,
                "DATA_NO_GRAVITY", false, "DATA_POSE", net.minecraft.world.entity.Pose.STANDING, "DATA_TICKS_FROZEN", 0);
        for (var entry : base.entrySet()) {
            Field f=Entity.class.getDeclaredField(entry.getKey()); f.setAccessible(true);
            builder.define((EntityDataAccessor)f.get(null), entry.getValue());
        }
        Method define = Player.class.getDeclaredMethod("defineSynchedData", SynchedEntityData.Builder.class);
        define.setAccessible(true); define.invoke(p, builder);
        Field data = Entity.class.getDeclaredField("entityData"); data.setAccessible(true); data.set(p, builder.build());
        return p;
    }

    private static Method callback(String suffix) {
        return Arrays.stream(Player.class.getDeclaredMethods()).filter(m -> m.getName().endsWith(suffix))
                .reduce((a,b) -> { throw new AssertionError("duplicate callback"); }).orElseThrow();
    }

    static Player load(CompoundTag saved) throws Exception {
        Player p = player();
        Method method = callback("grabandgo$readAdditionalSaveData"); method.setAccessible(true);
        ProblemReporter.Collector problems = new ProblemReporter.Collector();
        method.invoke(p, TagValueInput.create(problems, RegistryAccess.EMPTY, saved), new CallbackInfo("fixture", false));
        require(problems.isEmpty(), "typed load warnings: " + problems.getReport());
        return p;
    }

    static CompoundTag save(Player p) throws Exception {
        Method method = callback("grabandgo$addAdditionalSaveData"); method.setAccessible(true);
        ProblemReporter.Collector problems = new ProblemReporter.Collector();
        TagValueOutput out = TagValueOutput.createWithContext(problems, RegistryAccess.EMPTY);
        method.invoke(p, out, new CallbackInfo("fixture", false));
        require(problems.isEmpty(), "save errors"); return out.buildResult();
    }

    public static void run() throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        CompoundTag payload = TagParser.parseCompoundFully("{Type:entity,EntityTypeId:'minecraft:pig',EntityData:{id:'minecraft:pig',UUID:[I;1,2,3,4],CustomName:'fixture',unknown:{b:1b,s:2s,i:3,l:4L,f:5.0f,d:6.0d,ba:[B;1b,2b],ia:[I;3,4],la:[L;5L,6L],list:[{x:7s}]}}}");
        for (Tag raw : List.of(payload, StringTag.valueOf(payload.toString()))) {
            CompoundTag disk = new CompoundTag(); disk.put(CarryState.DATA, raw.copy()); disk.putBoolean(CarryState.FLAG, false);
            for (int n=0;n<8;n++) {
                Player p = load(disk); GrabCarrier c = (GrabCarrier)p;
                require(c.grabandgo$isCarrying(), "stale false flag was not reconciled");
                require(payload.equals(c.grabandgo$getCarriedData()), "complete/type-preserving payload mismatch");
                require(p.getEntityData().getNonDefaultValues() != null, "reconnect metadata missing");
                Player client=player();client.getEntityData().assignValues(p.getEntityData().getNonDefaultValues());
                require(((GrabCarrier)client).grabandgo$isCarrying() && payload.equals(((GrabCarrier)client).grabandgo$getCarriedData()), "initial tracking metadata did not restore client");
                disk=save(p); require(payload.equals(disk.get(CarryState.DATA)), "canonical compound changed");
            }
        }
        for (Tag raw : List.of(StringTag.valueOf(""), new CompoundTag())) {
            CompoundTag disk = new CompoundTag(); disk.put(CarryState.DATA, raw);
            require(!((GrabCarrier)load(disk)).grabandgo$isCarrying(), "empty state became occupied");
        }
        require(!((GrabCarrier)load(new CompoundTag())).grabandgo$isCarrying(), "absent state became occupied");
        for (Tag raw : List.of(StringTag.valueOf("{broken"), IntTag.valueOf(73),
                TagParser.parseCompoundFully("{Type:unknown,opaque:[L;9L]}"), new CompoundTag())) {
            CompoundTag disk = new CompoundTag(); disk.put(CarryState.DATA, raw.copy()); disk.putBoolean(CarryState.FLAG, true);
            for (int n=0;n<5;n++) {
                Player p=load(disk); GrabCarrier c=(GrabCarrier)p;
                c.grabandgo$clearCarried(); c.grabandgo$setCarriedData(new CompoundTag()); c.grabandgo$setCarrying(false);
                require(c.grabandgo$isCarrying(), "preserved state was cleared");
                disk=save(p); require(raw.equals(disk.get(CarryState.DATA)), "malformed tag lost or changed");
                require(disk.getString(CarryState.WARNED).isPresent(), "warning not remembered");
            }
        }
        CompoundTag disk=new CompoundTag(); disk.put(CarryState.DATA,payload); disk.putBoolean(CarryState.FLAG,true);
        Player p=load(disk); GrabCarrier c=(GrabCarrier)p; CarryState state=((CarryStateAccess)p).carryOnPatch$state();
        require(state.begin(),"begin failed"); c.grabandgo$clearCarried();
        require(c.grabandgo$isCarrying(),"in-flight clear allowed");
        state.retryable(); require(payload.equals(save(p).get(CarryState.DATA)),"failed attempt lost payload");
        require(state.begin(),"retry failed"); state.consumed(); c.grabandgo$clearCarried(); c.grabandgo$clearCarried();
        require(!((GrabCarrier)load(save(p))).grabandgo$isCarrying(),"consumed payload resurrected");
        Player client=load(disk);client.getEntityData().assignValues(p.getEntityData().packDirty());
        require(!((GrabCarrier)client).grabandgo$isCarrying() && ((GrabCarrier)client).grabandgo$getCarriedData().isEmpty(), "consumption metadata failed to clear client");
        p=load(disk); state=((CarryStateAccess)p).carryOnPatch$state(); state.begin(); state.uncertain();
        p=load(save(p)); require(!((CarryStateAccess)p).carryOnPatch$state().begin(),"partial placement retry unlocked on reload");
        PlacementFixtures.run();
        System.out.println("Patched upstream persistence: legacy STRING/COMPOUND, canonical rounds, unknown typed fields, malformed retention, flags, metadata, consumption and partial-effect lock PASS");
    }

    private static void require(boolean value, String message) { if(!value) throw new AssertionError(message); }
}
