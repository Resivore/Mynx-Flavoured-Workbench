package dev.resivore.carryonpatch;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import dev.resivore.carryonpatch.common.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.chermew.grabandgo.duck.GrabCarrier;
import org.objectweb.asm.*;

/** In-memory Level test double; actual patched upstream placement methods and Minecraft NBT
 * decoders run, but no Minecraft server, world save, or profile is started. */
public final class PlacementFixtures {
    private static final Map<UUID,Entity> ENTITIES = new HashMap<>();
    private static BlockState placed = Blocks.AIR.defaultBlockState();
    private static BlockEntity blockEntity;
    private static boolean reject, explode, collide;
    private static int additions;
    private static Method entityMethod, blockMethod;

    public static RegistryAccess world$registryAccess() { return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY); }
    public static FeatureFlagSet world$enabledFeatures() { return FeatureFlags.DEFAULT_FLAGS; }
    public static WorldBorder world$getWorldBorder() { return new WorldBorder(); }
    public static boolean world$mayInteract(Entity player, BlockPos pos) { return true; }
    public static boolean world$noCollision(Entity entity) { return !collide; }
    public static BlockState world$getBlockState(BlockPos pos) { return placed; }
    public static Entity world$getEntity(UUID uuid) { return ENTITIES.get(uuid); }
    public static Entity world$getEntityInAnyDimension(UUID uuid) { return ENTITIES.get(uuid); }
    public static boolean world$addFreshEntity(Entity entity) {
        additions++;
        if (reject) return false;
        ENTITIES.put(entity.getUUID(),entity);
        if (explode) throw new IllegalStateException("fixture side effect");
        return true;
    }
    public static boolean world$setBlock(BlockPos pos, BlockState value, int flags) {
        if (reject) return false;
        placed=value;
        if(explode) throw new IllegalStateException("fixture partial block placement");
        return true;
    }
    public static void world$setBlockEntity(BlockEntity value) { blockEntity=value; }
    public static BlockEntity world$getBlockEntity(BlockPos pos) { return blockEntity; }
    public static void world$sendBlockUpdated(BlockPos pos, BlockState before, BlockState after, int flags) {}
    public static void world$playSound(Entity source, double x, double y, double z,
            net.minecraft.sounds.SoundEvent event, net.minecraft.sounds.SoundSource category, float volume, float pitch) {}

    private static Level world() throws Exception {
        String name="dev/resivore/carryonpatch/FixtureLevel";
        ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);
        out.visit(Opcodes.V25,Opcodes.ACC_PUBLIC,name,null,"net/minecraft/world/level/Level",null);
        for(Method hook:PlacementFixtures.class.getDeclaredMethods()) {
            if(!hook.getName().startsWith("world$"))continue;
            String desc=org.objectweb.asm.Type.getMethodDescriptor(hook);
            MethodVisitor mv=out.visitMethod(Opcodes.ACC_PUBLIC,hook.getName().substring(6),desc,null,null);
            mv.visitCode(); int slot=1;
            for(var type:org.objectweb.asm.Type.getArgumentTypes(desc)) {
                mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD),slot); slot+=type.getSize();
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,"dev/resivore/carryonpatch/PlacementFixtures",hook.getName(),desc,false);
            mv.visitInsn(org.objectweb.asm.Type.getReturnType(desc).getOpcode(Opcodes.IRETURN));
            mv.visitMaxs(0,0);mv.visitEnd();
        }
        out.visitEnd(); Class<?> cls=MethodHandles.lookup().defineClass(out.toByteArray());
        Class<?> u=Class.forName("sun.misc.Unsafe"); Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);
        return (Level)u.getMethod("allocateInstance",Class.class).invoke(f.get(null),cls);
    }

    private static Player carrying(CompoundTag payload) throws Exception {
        CompoundTag disk=new CompoundTag(); disk.put(CarryState.DATA,payload);disk.putBoolean(CarryState.FLAG,true);
        return PersistenceFixtures.load(disk);
    }
    private static InteractionResult place(Player p,Level level,boolean block) throws Exception {
        return (InteractionResult)(block?blockMethod:entityMethod).invoke(null,p,level,BlockPos.ZERO,((GrabCarrier)p).grabandgo$getCarriedData());
    }
    private static void retained(Player p,CompoundTag expected) throws Exception {
        require(expected.equals(PersistenceFixtures.save(p).get(CarryState.DATA)),"failed placement changed payload");
        require(((GrabCarrier)p).grabandgo$isCarrying(),"failed placement cleared flag");
    }
    private static void require(boolean v,String m) { if(!v)throw new AssertionError(m); }

    public static void run() throws Exception {
        Class<?> handler=Class.forName("org.chermew.grabandgo.event.GrabHandler");
        entityMethod=handler.getDeclaredMethod("placeEntity",Player.class,Level.class,BlockPos.class,CompoundTag.class);
        blockMethod=handler.getDeclaredMethod("placeBlock",Player.class,Level.class,BlockPos.class,CompoundTag.class);
        entityMethod.setAccessible(true);blockMethod.setAccessible(true);
        // Standalone bootstrap does not run the data-pack component binding phase.
        // Bind only the two fixture items with ordinary common components.
        net.minecraft.world.item.Items.DIAMOND.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponents.COMMON_ITEM_COMPONENTS);
        net.minecraft.world.item.Items.APPLE.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponents.COMMON_ITEM_COMPONENTS);
        Level level=world();
        CompoundTag unknown=TagParser.parseCompoundFully("{Type:entity,EntityTypeId:'missing:mob',EntityData:{id:'missing:mob',UUID:[I;1,2,3,4],unknown:[B;2b]}}");
        Player p=carrying(unknown); require(place(p,level,false)==InteractionResult.FAIL,"unknown entity accepted");retained(p,unknown);
        CompoundTag sheep=TagParser.parseCompoundFully("{Type:entity,EntityTypeId:'minecraft:sheep',EntityData:{id:'minecraft:sheep',UUID:[I;1,2,3,4],Color:5b,Age:-24000}}");
        collide=true;p=carrying(sheep);
        require(place(p,level,false)==InteractionResult.FAIL,"blocked mob accepted");retained(p,sheep);collide=false;
        reject=true; p=carrying(sheep);
        require(place(p,level,false)==InteractionResult.FAIL,"rejected spawn accepted");retained(p,sheep);
        require(additions==1,"actual Minecraft entity decoder did not reach insertion");
        reject=false;
        require(place(p,level,false)==InteractionResult.SUCCESS,"valid entity retry failed");
        require(ENTITIES.size()==1,"entity not added exactly once");
        var restored=(net.minecraft.world.entity.animal.sheep.Sheep)ENTITIES.values().iterator().next();
        require(restored.getColor()==net.minecraft.world.item.DyeColor.LIME && restored.getAge()==-24000,"sheep properties lost");
        require(place(p,level,false)==InteractionResult.FAIL,"repeated placement succeeded");
        require(place(PersistenceFixtures.load(PersistenceFixtures.save(p)),level,false)==InteractionResult.FAIL,"reload resurrected entity");
        p=carrying(sheep);require(place(p,level,false)==InteractionResult.FAIL,"UUID conflict accepted");retained(p,sheep);
        ENTITIES.clear(); explode=true;p=carrying(sheep);
        require(place(p,level,false)==InteractionResult.FAIL,"partial side effect accepted");retained(p,sheep);
        int attempts=additions;explode=false;
        p=PersistenceFixtures.load(PersistenceFixtures.save(p));
        require(place(p,level,false)==InteractionResult.FAIL && additions==attempts,"partial side effect retried after reload");
        ENTITIES.clear();
        CompoundTag container=TagParser.parseCompoundFully("{Type:block,BlockId:'minecraft:barrel',BlockState:{Name:'minecraft:barrel',Properties:{facing:up,open:'false'}},BlockEntityData:{id:'minecraft:barrel',x:8,y:9,z:10,Items:[{Slot:0b,id:'minecraft:diamond',count:7},{Slot:13b,id:'minecraft:apple',count:3}]}}");
        reject=true;p=carrying(container);require(place(p,level,true)==InteractionResult.FAIL,"rejected block accepted");retained(p,container);
        reject=false;require(place(p,level,true)==InteractionResult.SUCCESS,"container retry failed");
        require(blockEntity!=null && placed.getBlock()==Blocks.BARREL,"container missing");
        var inventory=(net.minecraft.world.Container)blockEntity;
        require(inventory.getItem(0).getCount()==7 && inventory.getItem(0).is(net.minecraft.world.item.Items.DIAMOND),"diamonds lost");
        require(inventory.getItem(13).getCount()==3 && inventory.getItem(13).is(net.minecraft.world.item.Items.APPLE),"apples lost");
        require(place(p,level,true)==InteractionResult.FAIL,"container duplicated");
        placed=Blocks.AIR.defaultBlockState();blockEntity=null;
        CompoundTag absentBlock=container.copy();absentBlock.putString("BlockId","missing:block");
        p=carrying(absentBlock);require(place(p,level,true)==InteractionResult.FAIL,"unknown block accepted");retained(p,absentBlock);
        CompoundTag broken=container.copy();broken.getCompoundOrEmpty("BlockEntityData").putString("Items","not a list");
        p=carrying(broken);require(place(p,level,true)==InteractionResult.FAIL,"invalid container decoding accepted");retained(p,broken);
        p=PersistenceFixtures.load(PersistenceFixtures.save(p));require(!((CarryStateAccess)p).carryOnPatch$state().begin(),"decoder rejection not retained");
        explode=true;p=carrying(container);require(place(p,level,true)==InteractionResult.FAIL,"partial block accepted");retained(p,container);
        explode=false;p=PersistenceFixtures.load(PersistenceFixtures.save(p));
        require(!((CarryStateAccess)p).carryOnPatch$state().begin(),"partial block lock lost");
        System.out.println("Patched upstream placement: missing IDs, rejected spawn/block, valid retry, UUID conflict, exact-once consumption/reload, partial-effect locks PASS");
    }
}
