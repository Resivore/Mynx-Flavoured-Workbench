package com.crispytwig.naturalist;

import com.crispytwig.naturalist.client.model.*;
import com.crispytwig.naturalist.client.model.item.NaturalistItemModelProperties;

import com.crispytwig.naturalist.client.gui.screens.ElephantInventoryScreen;
import com.crispytwig.naturalist.client.renderer.*;
import com.crispytwig.naturalist.registry.NaturalistBlockEntities;
import com.crispytwig.naturalist.registry.NaturalistEntityTypes;
import com.crispytwig.naturalist.registry.NaturalistMenus;
import com.crispytwig.naturalist.registry.NaturalistRegistry;
import com.crispytwig.naturalist.server.item.KnapsackItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

@Environment(EnvType.CLIENT)
public final class NaturalistClient {
    private NaturalistClient() {
    }

    @FunctionalInterface
    public interface LayerRegistrar {
        void register(ModelLayerLocation location, Supplier<LayerDefinition> definition);
    }

    public static void registerLayerDefinitions(LayerRegistrar r) {
        r.register(AlligatorBabyModel.LAYER_LOCATION, AlligatorBabyModel::createBodyLayer);
        r.register(AlligatorModel.LAYER_LOCATION, AlligatorModel::createBodyLayer);
        r.register(AnglerfishModel.LAYER_LOCATION, AnglerfishModel::createBodyLayer);
        r.register(BassModel.LAYER_LOCATION, BassModel::createBodyLayer);
        r.register(MediumBassModel.LAYER_LOCATION, MediumBassModel::createBodyLayer);
        r.register(LargeBassModel.LAYER_LOCATION, LargeBassModel::createBodyLayer);
        r.register(BearBabyModel.LAYER_LOCATION, BearBabyModel::createBodyLayer);
        r.register(BearModel.LAYER_LOCATION, BearModel::createBodyLayer);
        r.register(BirdBabyModel.LAYER_LOCATION, BirdBabyModel::createBodyLayer);
        r.register(BirdModel.LAYER_LOCATION, BirdModel::createBodyLayer);
        r.register(BlackBearBabyModel.LAYER_LOCATION, BlackBearBabyModel::createBodyLayer);
        r.register(BlackBearModel.LAYER_LOCATION, BlackBearModel::createBodyLayer);
        r.register(BlobfishGrayModel.LAYER_LOCATION, BlobfishGrayModel::createBodyLayer);
        r.register(BlobfishPinkModel.LAYER_LOCATION, BlobfishPinkModel::createBodyLayer);
        r.register(BoarBabyModel.LAYER_LOCATION, BoarBabyModel::createBodyLayer);
        r.register(BoarModel.LAYER_LOCATION, BoarModel::createBodyLayer);
        r.register(ButterflyModel.LAYER_LOCATION, ButterflyModel::createBodyLayer);
        r.register(CapybaraBabyModel.LAYER_LOCATION, CapybaraBabyModel::createBodyLayer);
        r.register(CapybaraModel.LAYER_LOCATION, CapybaraModel::createBodyLayer);
        r.register(CaterpillarModel.LAYER_LOCATION, CaterpillarModel::createBodyLayer);
        r.register(CatfishModel.LAYER_LOCATION, CatfishModel::createBodyLayer);
        r.register(ClamModel.LAYER_LOCATION, ClamModel::createBodyLayer);
        r.register(CrabBabyModel.LAYER_LOCATION, CrabBabyModel::createBodyLayer);
        r.register(HedgehogModel.LAYER_LOCATION, HedgehogModel::createBodyLayer);
        r.register(CrabModel.LAYER_LOCATION, CrabModel::createBodyLayer);
        r.register(DeerBabyModel.LAYER_LOCATION, DeerBabyModel::createBodyLayer);
        r.register(DeerModel.LAYER_LOCATION, DeerModel::createBodyLayer);
        r.register(DesertScorpionModel.LAYER_LOCATION, DesertScorpionModel::createBodyLayer);
        r.register(DirtTrailModel.LAYER_LOCATION, DirtTrailModel::createBodyLayer);
        r.register(DragonflyModel.LAYER_LOCATION, DragonflyModel::createBodyLayer);
        r.register(DuckBabyModel.LAYER_LOCATION, DuckBabyModel::createBodyLayer);
        r.register(DuckModel.LAYER_LOCATION, DuckModel::createBodyLayer);
        r.register(ElephantBabyModel.LAYER_LOCATION, ElephantBabyModel::createBodyLayer);
        r.register(ElephantModel.LAYER_LOCATION, ElephantModel::createBodyLayer);
        r.register(FireflyBabyModel.LAYER_LOCATION, FireflyBabyModel::createBodyLayer);
        r.register(FireflyModel.LAYER_LOCATION, FireflyModel::createBodyLayer);
        r.register(GiantIsopodModel.LAYER_LOCATION, GiantIsopodModel::createBodyLayer);
        r.register(GiraffeBabyModel.LAYER_LOCATION, GiraffeBabyModel::createBodyLayer);
        r.register(GiraffeModel.LAYER_LOCATION, GiraffeModel::createBodyLayer);
        r.register(GreatWhiteSharkModel.LAYER_LOCATION, GreatWhiteSharkModel::createBodyLayer);
        r.register(HippoBabyModel.LAYER_LOCATION, HippoBabyModel::createBodyLayer);
        r.register(HippoModel.LAYER_LOCATION, HippoModel::createBodyLayer);
        r.register(JellyfishModel.LAYER_LOCATION, JellyfishModel::createBodyLayer);
        r.register(JungleScorpionModel.LAYER_LOCATION, JungleScorpionModel::createBodyLayer);
        r.register(KomodoDragonModel.LAYER_LOCATION, KomodoDragonModel::createBodyLayer);
        r.register(LionBabyModel.LAYER_LOCATION, LionBabyModel::createBodyLayer);
        r.register(LionModel.LAYER_LOCATION, LionModel::createBodyLayer);
        r.register(LizardModel.LAYER_LOCATION, LizardModel::createBodyLayer);
        r.register(LizardTailModel.LAYER_LOCATION, LizardTailModel::createBodyLayer);
        r.register(MammothBabyModel.LAYER_LOCATION, MammothBabyModel::createBodyLayer);
        r.register(MammothModel.LAYER_LOCATION, MammothModel::createBodyLayer);
        r.register(MoleModel.LAYER_LOCATION, MoleModel::createBodyLayer);
        r.register(OstrichBabyModel.LAYER_LOCATION, OstrichBabyModel::createBodyLayer);
        r.register(OstrichModel.LAYER_LOCATION, OstrichModel::createBodyLayer);
        r.register(PiranhaModel.LAYER_LOCATION, PiranhaModel::createBodyLayer);
        r.register(RatModel.LAYER_LOCATION, RatModel::createBodyLayer);
        r.register(RayModel.LAYER_LOCATION, RayModel::createBodyLayer);
        r.register(RhinoModel.LAYER_LOCATION, RhinoModel::createBodyLayer);
        r.register(SnailModel.LAYER_LOCATION, SnailModel::createBodyLayer);
        r.register(SnakeModel.LAYER_LOCATION, SnakeModel::createBodyLayer);
        r.register(StarfishModel.LAYER_LOCATION, StarfishModel::createBodyLayer);
        r.register(TigerBabyModel.LAYER_LOCATION, TigerBabyModel::createBodyLayer);
        r.register(TigerModel.LAYER_LOCATION, TigerModel::createBodyLayer);
        r.register(TortoiseBabyModel.LAYER_LOCATION, TortoiseBabyModel::createBodyLayer);
        r.register(TortoiseModel.LAYER_LOCATION, TortoiseModel::createBodyLayer);
        r.register(TurkeyModel.LAYER_LOCATION, TurkeyModel::createBodyLayer);
        r.register(VultureBabyModel.LAYER_LOCATION, VultureBabyModel::createBodyLayer);
        r.register(VultureModel.LAYER_LOCATION, VultureModel::createBodyLayer);
        r.register(WhaleBabyModel.LAYER_LOCATION, WhaleBabyModel::createBodyLayer);
        r.register(WhaleModel.LAYER_LOCATION, WhaleModel::createBodyLayer);
        r.register(ZebraBabyModel.LAYER_LOCATION, ZebraBabyModel::createBodyLayer);
        r.register(ZebraModel.LAYER_LOCATION, ZebraModel::createBodyLayer);
    }

    @FunctionalInterface
    public interface RendererRegistrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    public static void registerRenderers(RendererRegistrar r) {
        r.register(NaturalistEntityTypes.SNAIL.get(), SnailRenderer::new);
        r.register(NaturalistEntityTypes.BEAR.get(), BearRenderer::new);
        r.register(NaturalistEntityTypes.BUTTERFLY.get(), ButterflyRenderer::new);
        r.register(NaturalistEntityTypes.FIREFLY.get(), FireflyRenderer::new);
        r.register(NaturalistEntityTypes.SNAKE.get(), SnakeRenderer::new);
        r.register(NaturalistEntityTypes.CRAB.get(), CrabRenderer::new);
        r.register(NaturalistEntityTypes.DEER.get(), DeerRenderer::new);
        r.register(NaturalistEntityTypes.BIRD.get(), BirdRenderer::new);
        r.register(NaturalistEntityTypes.CATERPILLAR.get(), CaterpillarRenderer::new);
        r.register(NaturalistEntityTypes.RHINO.get(), RhinoRenderer::new);
        r.register(NaturalistEntityTypes.LION.get(), LionRenderer::new);
        r.register(NaturalistEntityTypes.ELEPHANT.get(), ElephantRenderer::new);
        r.register(NaturalistEntityTypes.MAMMOTH.get(), MammothRenderer::new);
        r.register(NaturalistEntityTypes.ZEBRA.get(), ZebraRenderer::new);
        r.register(NaturalistEntityTypes.GIRAFFE.get(), GiraffeRenderer::new);
        r.register(NaturalistEntityTypes.HIPPO.get(), HippoRenderer::new);
        r.register(NaturalistEntityTypes.VULTURE.get(), VultureRenderer::new);
        r.register(NaturalistEntityTypes.BOAR.get(), BoarRenderer::new);
        r.register(NaturalistEntityTypes.DRAGONFLY.get(), DragonflyRenderer::new);
        r.register(NaturalistEntityTypes.CATFISH.get(), CatfishRenderer::new);
        r.register(NaturalistEntityTypes.ALLIGATOR.get(), AlligatorRenderer::new);
        r.register(NaturalistEntityTypes.BASS.get(), BassRenderer::new);
        r.register(NaturalistEntityTypes.LIZARD.get(), LizardRenderer::new);
        r.register(NaturalistEntityTypes.LIZARD_TAIL.get(), LizardTailRenderer::new);
        r.register(NaturalistEntityTypes.TORTOISE.get(), TortoiseRenderer::new);
        r.register(NaturalistEntityTypes.DUCK.get(), DuckRenderer::new);
        r.register(NaturalistEntityTypes.DUCK_EGG.get(), ThrownItemRenderer::new);
        r.register(NaturalistEntityTypes.STARFISH.get(), StarfishRenderer::new);
        r.register(NaturalistEntityTypes.CLAM.get(), ClamRenderer::new);
        r.register(NaturalistEntityTypes.GIANT_ISOPOD.get(), GiantIsopodRenderer::new);
        r.register(NaturalistEntityTypes.JELLYFISH.get(), JellyfishRenderer::new);
        r.register(NaturalistEntityTypes.ANGLERFISH.get(), AnglerfishRenderer::new);
        r.register(NaturalistEntityTypes.RAY.get(), RayRenderer::new);
        r.register(NaturalistEntityTypes.BLOBFISH.get(), BlobfishRenderer::new);
        r.register(NaturalistEntityTypes.PIRANHA.get(), PiranhaRenderer::new);
        r.register(NaturalistEntityTypes.WHALE.get(), WhaleRenderer::new);
        r.register(NaturalistEntityTypes.MOLE.get(), MoleRenderer::new);
        r.register(NaturalistEntityTypes.DIRT_TRAIL.get(), DirtTrailRenderer::new);
        r.register(NaturalistEntityTypes.RAT.get(), RatRenderer::new);
        r.register(NaturalistEntityTypes.BLACK_BEAR.get(), BlackBearRenderer::new);
        r.register(NaturalistEntityTypes.TIGER.get(), TigerRenderer::new);
        r.register(NaturalistEntityTypes.KOMODO_DRAGON.get(), KomodoDragonRenderer::new);
        r.register(NaturalistEntityTypes.OSTRICH.get(), OstrichRenderer::new);
        r.register(NaturalistEntityTypes.DESERT_SCORPION.get(), DesertScorpionRenderer::new);
        r.register(NaturalistEntityTypes.JUNGLE_SCORPION.get(), JungleScorpionRenderer::new);
        r.register(NaturalistEntityTypes.GREAT_WHITE_SHARK.get(), GreatWhiteSharkRenderer::new);
        r.register(NaturalistEntityTypes.TURKEY.get(), TurkeyRenderer::new);
        r.register(NaturalistEntityTypes.CAPYBARA.get(), CapybaraRenderer::new);
        r.register(NaturalistEntityTypes.HEDGEHOG.get(), HedgehogRenderer::new);
    }

    @FunctionalInterface
    public interface MenuScreenRegistrar {
        <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> factory);
    }

    public static void registerMenuScreens(MenuScreenRegistrar r) {
        r.register(NaturalistMenus.ELEPHANT.get(), ElephantInventoryScreen::new);
    }

    public static void registerItemProperties() {
        NaturalistItemModelProperties.Property color = (stack, level, entity, seed) -> {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            return customData != null ? customData.copyTag().getIntOr("Color", 0) / 15.0F : 0.0F;
        };
        NaturalistItemModelProperties.register(
                NaturalistRegistry.SNAIL.get(), Identifier.withDefaultNamespace("color"), color);
        NaturalistItemModelProperties.register(NaturalistRegistry.KNAPSACK.get(),
                Naturalist.location("filled"),
                (stack, level, entity, seed) -> KnapsackItem.isFilled(stack) ? 1.0F : 0.0F);
    }
}
