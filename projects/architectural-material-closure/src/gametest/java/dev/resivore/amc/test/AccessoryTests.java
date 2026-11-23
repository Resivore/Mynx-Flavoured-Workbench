package dev.resivore.amc.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class AccessoryTests {
    private static final String AMC="architectural_material_closure";
    private static final List<String> WOODS=List.of("veiled","celestial","murublight","mossy_oak");
    private static List<String> forms(String wood) {
        var result=new ArrayList<String>();
        if (wood.equals("mossy_oak")) result.addAll(List.of("button","pressure_plate"));
        result.addAll(List.of("planks_path",wood.equals("celestial")||wood.equals("murublight")?"stem_parapet":"log_parapet",
                "plank_parapet","blinds","curtain_rod","shutter","louvered_shutter"));
        return result;
    }
    private static boolean present(String wood) {return FabricLoader.getInstance().isModLoaded(wood.equals("mossy_oak")?"ribbits":"enderscape");}
    private static Identifier id(String wood,String form) {return Identifier.fromNamespaceAndPath(AMC,wood+"_"+form);}
    static void registry(GameTestHelper h,Set<Identifier> ids) {
        for (var wood:WOODS) for (var form:forms(wood)) {
            var key=id(wood,form);
            if (present(wood)) h.assertTrue(ids.add(key),"duplicate "+key);
            boolean recipe=present(wood)&&(!key.getPath().equals("mossy_oak_shutter")||FabricLoader.getInstance().isModLoaded("mcwtrpdoors"));
            h.assertTrue(h.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE,key)).isPresent()==recipe,"recipe "+key);
            for (var provider:List.of("ribbits","mcwwindows","mcwpaths","enderscape"))
                h.assertTrue(!BuiltInRegistries.BLOCK.containsKey(Identifier.fromNamespaceAndPath(provider,key.getPath())),"provider owns "+key);
        }
    }
    @GameTest public void materials(GameTestHelper h) {
        for (var wood:WOODS) {
            if (!present(wood)) continue;
            for (var form:forms(wood)) {
                String ns=switch(form) {case "planks_path" -> "mcwpaths";case "button","pressure_plate" -> "minecraft";default -> "mcwwindows";};
                var ref=Identifier.fromNamespaceAndPath(ns,(form.equals("stem_parapet")?"warped_":"oak_")+form);
                var block=BuiltInRegistries.BLOCK.getOptional(id(wood,form)).orElseThrow();
                BambooTests.checkForm(h,BuiltInRegistries.BLOCK.getOptional(ref).orElseThrow(),block,form,false);
                if (form.equals("blinds")) BambooTests.checkBlinds(h,block);
                if (form.endsWith("parapet")) {
                    var pos=new BlockPos(1,1,1);
                    h.setBlock(pos,block);
                    h.setBlock(pos.above(),Blocks.FLOWER_POT);
                    h.assertTrue(h.getBlockState(pos).getValue(net.kikoz.mcwwindows.objects.Parapet.FLOWER)==net.kikoz.mcwwindows.objects.Parapet.Flower.FLOWER,"support "+wood);
                    h.setBlock(pos.above(),Blocks.AIR);
                }
                if (form.endsWith("shutter")) {
                    var pos=new BlockPos(1,1,1);
                    h.setBlock(pos,block);h.setBlock(pos.above(),block);
                    boolean initial=h.getBlockState(pos).getValue(BlockStateProperties.OPEN);
                    h.useBlock(pos);
                    h.assertTrue(h.getBlockState(pos).getValue(BlockStateProperties.OPEN)!=initial
                            && h.getBlockState(pos.above()).getValue(BlockStateProperties.OPEN)!=initial,"stack "+wood+form);
                    h.setBlock(pos.above(),Blocks.AIR);
                }
                h.setBlock(new BlockPos(1,1,1),Blocks.AIR);
                grid(h,wood,form);
            }
        }
        h.succeed();
    }
    private static void grid(GameTestHelper h,String wood,String form) {
        boolean moss=wood.equals("mossy_oak");
        if (moss&&form.equals("shutter")&&!FabricLoader.getInstance().isModLoaded("mcwtrpdoors")) return;
        String ns=(moss?"ribbits:":"enderscape:")+wood;
        String slab=ns+(moss?"_planks_slab":"_slab");
        String log=moss?slab:ns+(wood.equals("veiled")?"_log":"_stem");
        String pattern=switch(form) {
            case "button" -> "P";case "pressure_plate" -> "PP";case "planks_path" -> "L";
            case "log_parapet","stem_parapet" -> "SSSSGS";case "plank_parapet" -> "SSSSPS";case "blinds" -> "LLLSSSSSS";
            case "curtain_rod" -> "SGS";case "shutter" -> "TTT";default -> "LLL";};
        int width=form.endsWith("shutter")?1:Math.min(3,pattern.length());
        var stacks=new ArrayList<ItemStack>();
        for(char c:pattern.toCharArray()){
            String ingredient=switch(c){case 'P' -> ns+"_planks";case 'S' -> "minecraft:stick";case 'L' -> slab;case 'G' -> log;
                default -> moss?AMC+":mossy_oak_classic_trapdoor":ns+"_trapdoor";};
            stacks.add(BuiltInRegistries.ITEM.getOptional(Identifier.parse(ingredient)).orElseThrow().getDefaultInstance());
        }
        var input=CraftingInput.of(width,pattern.length()/width,stacks);
        var selected=h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();
        int count=form.endsWith("parapet")||form.equals("curtain_rod")?5:form.equals("button")||form.equals("pressure_plate")?1:3;
        h.assertTrue(selected.id().identifier().equals(id(wood,form)) && selected.value().assemble(input).getCount()==count,"craft "+wood+form+" got "+selected.id());
    }
    @GameTest(maxTicks=55) public void woodenControls(GameTestHelper h) {
        if (!present("mossy_oak")) {h.succeed();return;}
        var button=new BlockPos(1,1,1);
        var plate=button.east();
        h.setBlock(button.below(),Blocks.STONE);h.setBlock(plate.below(),Blocks.STONE);
        h.setBlock(button,BuiltInRegistries.BLOCK.getValue(id("mossy_oak","button")).defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR));
        h.setBlock(plate,BuiltInRegistries.BLOCK.getValue(id("mossy_oak","pressure_plate")));
        h.useBlock(button);
        h.spawnItem(Items.STICK,Vec3.atBottomCenterOf(plate));
        h.runAfterDelay(4,()->h.assertBlockProperty(plate,BlockStateProperties.POWERED,true));
        h.runAfterDelay(29,()->h.assertBlockProperty(button,BlockStateProperties.POWERED,true));
        h.runAfterDelay(30,()->{h.assertBlockProperty(button,BlockStateProperties.POWERED,false);h.succeed();});
    }
}
