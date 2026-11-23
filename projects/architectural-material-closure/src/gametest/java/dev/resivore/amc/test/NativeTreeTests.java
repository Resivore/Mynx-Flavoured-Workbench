package dev.resivore.amc.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import java.util.List;

public final class NativeTreeTests {
    @GameTest
    public void nativeFixturesAndVessels(GameTestHelper test) {
        for (String wood : List.of("silver_birch","wisteria")) {
            for (String form : List.of("sign","wall_sign","hanging_sign","wall_hanging_sign","shelf")) {
                Block block=BuiltInRegistries.BLOCK.getOptional(Identifier.parse("mynx_trees:"+wood+"_"+form)).orElseThrow();
                var type=form.equals("shelf") ? BlockEntityTypes.SHELF : form.contains("hanging") ? BlockEntityTypes.HANGING_SIGN : BlockEntityTypes.SIGN;
                test.assertTrue(type.isValid(block.defaultBlockState()),"Block entity rejected "+block);
            }
            for (String form : List.of("frame","lattice","shutter","lantern","plank_window","log_parapet")) {
                var id=Identifier.parse("architectural_material_closure:"+wood+"_"+form);
                var block=BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing "+id));
                test.assertTrue(BuiltInRegistries.ITEM.getOptional(id).orElseThrow()==block.asItem(),"Item mismatch "+id);
            }
            WoodMatrixTests.signPlacements(test,"mynx_trees:"+wood+"_");
            WoodMatrixTests.fixtures(test,"mynx_trees:"+wood+"_","architectural_material_closure:"+wood+"_");
            vessels(test,"mynx_trees",wood);
        }
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ribbits")) vessels(test,"architectural_material_closure","mossy_oak");
        test.succeed();
    }

    private static void vessels(GameTestHelper test, String ns, String wood) {
        for (String kind : List.of("boat","chest_boat")) {
            var id=Identifier.parse(ns+":"+wood+"_"+kind);
            var item=BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
            var entity=BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow().create(test.getLevel(), EntitySpawnReason.COMMAND);
            test.assertTrue(item instanceof BoatItem && entity instanceof AbstractBoat boat && boat.getPickResult().is(item),"Boat drop "+id);
            test.assertTrue((entity instanceof ChestBoat)==kind.equals("chest_boat"),"Wrong hull "+id);
        }
    }
}
