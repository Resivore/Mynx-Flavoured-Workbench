package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.*;
import dev.resivore.slotreservations.network.*;
import fuzs.iteminteractions.common.api.v2.client.gui.screens.inventory.tooltip.ClientItemContentsTooltip;
import fuzs.iteminteractions.common.api.v2.world.inventory.tooltip.ItemContentsTooltip;
import fuzs.iteminteractions.common.api.v2.world.item.storage.ContainerStorage;
import fuzs.iteminteractions.common.api.v2.world.item.storage.ItemStorageHolder;
import fuzs.iteminteractions.common.impl.client.gui.screens.inventory.tooltip.CollapsibleClientTooltipComponent;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import com.mojang.blaze3d.platform.Window;
import org.joml.Matrix3x2fStack;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Exact transformed screen/editor/native tooltip classes; only platform services and drawing sink are mocked. */
class TooltipRenderLifecycleTest {
    static MockedStatic<FabricLoader> loaderStatic;
    MockedStatic<Minecraft> minecraftStatic;
    MockedStatic<ClientPlayNetworking> networking;
    MockedStatic<ClientTooltipComponent> tooltipFactory;
    MockedStatic<net.minecraft.client.gui.screens.Screen> textFactory;
    Minecraft minecraft;
    Gui gui;
    MouseHandler mouse;
    Window window;
    Font font;
    AbstractContainerMenu menu;
    LocalPlayer player;
    AbstractContainerScreen<?> screen;
    ContainerStorage storage;
    ItemStack host;
    Slot hostSlot;
    ItemContentsTooltip lastImage;
    ClientItemContentsTooltip lastClient;
    GuiGraphicsExtractor graphics;
    final List<NestedReservationActionPayload> packets = new ArrayList<>();
    double pointerX, pointerY;
    int queries;
    boolean factoryCopy = true;

    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        var frozen = MappedRegistry.class.getDeclaredField("frozen"); frozen.setAccessible(true);
        frozen.setBoolean(BuiltInRegistries.DATA_COMPONENT_TYPE, false);
        try { ModComponents.initialize(); } finally { frozen.setBoolean(BuiltInRegistries.DATA_COMPONENT_TYPE, true); }
        for (Item item : List.of(Items.SHULKER_BOX, Items.STONE, Items.DIRT))
            if (!item.builtInRegistryHolder().areComponentsBound()) item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        loaderStatic = mockStatic(FabricLoader.class);
        FabricLoader loader = mock(FabricLoader.class);
        loaderStatic.when(FabricLoader::getInstance).thenReturn(loader);
        when(loader.isModLoaded(anyString())).thenReturn(true);
    }
    @AfterAll static void closeLoader() { loaderStatic.close(); }

    @BeforeEach void setup() throws Exception {
        NestedTooltipEditor.beginFrame(); NestedTooltipEditor.beginFrame(); finishFrame();
        minecraft = mock(Minecraft.class); gui = mock(Gui.class); mouse = mock(MouseHandler.class);
        window = mock(Window.class); font = mock(Font.class);
        minecraftStatic = mockStatic(Minecraft.class); minecraftStatic.when(Minecraft::getInstance).thenReturn(minecraft);
        set(minecraft, "gui", gui); set(minecraft, "mouseHandler", mouse); set(minecraft, "font", font);
        player = mock(LocalPlayer.class); set(minecraft, "player", player);
        when(minecraft.getWindow()).thenReturn(window);
        var atlases = mock(net.minecraft.client.resources.model.sprite.AtlasManager.class);
        when(minecraft.getAtlasManager()).thenReturn(atlases);
        when(atlases.getAtlasOrThrow(any())).thenReturn(mock(net.minecraft.client.renderer.texture.TextureAtlas.class));
        when(mouse.getScaledXPos(window)).thenAnswer(a -> pointerX);
        when(mouse.getScaledYPos(window)).thenAnswer(a -> pointerY);
        when(window.getGuiScaledWidth()).thenReturn(640); when(window.getGuiScaledHeight()).thenReturn(360);
        when(player.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        menu = new TestMenu(); player.containerMenu = menu;
        screen = mock(AbstractContainerScreen.class, CALLS_REAL_METHODS);
        set(screen, "menu", menu); set(screen, "font", font); set(screen, "minecraft", minecraft);
        when(gui.screen()).thenReturn(screen);
        textFactory = mockStatic(net.minecraft.client.gui.screens.Screen.class, CALLS_REAL_METHODS);
        textFactory.when(() -> net.minecraft.client.gui.screens.Screen.getTooltipFromItem(any(), any())).thenReturn(List.of());
        host = spy(new ItemStack(Items.SHULKER_BOX)); hostSlot = menu.slots.get(0); hostSlot.set(host);
        menu.slots.get(1).set(host.copy());
        set(screen, "hoveredSlot", hostSlot);
        storage = mock(ContainerStorage.class, CALLS_REAL_METHODS);
        set(storage, "inventoryWidth", 9); set(storage, "inventoryHeight", 3);
        doReturn(-1).when(storage).getSelectedItem(any(), any());
        doAnswer(a -> {
            queries++;
            ItemStack copiedSource = factoryCopy ? host.copy() : host;
            if (factoryCopy) assertNotSame(host, copiedSource);
            var image = new ItemStorageHolder(storage).getTooltipImage(copiedSource, player).orElse(Optional.empty());
            lastImage = image.map(value -> (ItemContentsTooltip)value).orElse(null);
            return image;
        }).when(host).getTooltipImage();
        tooltipFactory = mockStatic(ClientTooltipComponent.class, CALLS_REAL_METHODS);
        tooltipFactory.when(() -> ClientTooltipComponent.create(any(TooltipComponent.class))).thenAnswer(a -> {
            lastClient = new ClientItemContentsTooltip(a.getArgument(0));
            return new CollapsibleClientTooltipComponent(lastClient);
        });
        networking = mockStatic(ClientPlayNetworking.class);
        networking.when(() -> ClientPlayNetworking.canSend(NestedReservationActionPayload.TYPE)).thenReturn(true);
        networking.when(() -> ClientPlayNetworking.send(any())).thenAnswer(a -> { packets.add(a.getArgument(0)); return null; });
        graphics = graphics();
        fuzs.iteminteractions.common.impl.ItemInteractions.CLIENT.itemStorageTooltip = fuzs.iteminteractions.common.impl.config.ItemStorageTooltip.ALWAYS;
    }
    @AfterEach void cleanup() {
        if (networking != null) networking.close(); if (tooltipFactory != null) tooltipFactory.close();
        if (textFactory != null) textFactory.close(); if (minecraftStatic != null) minecraftStatic.close();
    }

    GuiGraphicsExtractor graphics() throws Exception {
        GuiGraphicsExtractor result = mock(GuiGraphicsExtractor.class, CALLS_REAL_METHODS);
        set(result, "minecraft", minecraft); set(result, "pose", new Matrix3x2fStack(16));
        // Keep native deferred scheduling and final positioning. Replace only resource/GPU submissions.
        doNothing().when(result).nextStratum();
        doNothing().when(result).blitSprite(any(), any(net.minecraft.resources.Identifier.class), anyInt(), anyInt(), anyInt(), anyInt());
        doNothing().when(result).blitSprite(any(), any(net.minecraft.resources.Identifier.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        doNothing().when(result).item(any(ItemStack.class), anyInt(), anyInt(), anyInt());
        doNothing().when(result).itemDecorations(any(), any(), anyInt(), anyInt());
        doNothing().when(result).text(any(Font.class), anyString(), anyInt(), anyInt(), anyInt(), anyBoolean());
        return result;
    }
    void frame(int x, int y, boolean outer) throws Exception {
        pointerX=x; pointerY=y; set(screen, "hoveredSlot", outer ? hostSlot : null);
        if (hasLogicalBoundary()) NestedTooltipEditor.beginFrame();
        new GuiGraphicsExtractor(minecraft, null, x, y); // Native main extractor constructor/delegation.
        graphics=graphics();
        invoke(screen, "extractTooltip", new Class[]{GuiGraphicsExtractor.class,int.class,int.class}, graphics,x,y);
        // Native image extraction is deferred until AFTER scheduling returns and the scope has closed.
        graphics.extractDeferredElements(x,y,0);
        finishFrame();
        // Installed Sodium constructs this after GUI extraction, just before GuiRenderer.render().
        new GuiGraphicsExtractor(minecraft, null, x, y);
    }
    static boolean hasLogicalBoundary() { try { NestedTooltipEditor.class.getMethod("endFrame"); return true; } catch (NoSuchMethodException e) { return false; } }
    static void finishFrame() throws Exception { if (hasLogicalBoundary()) NestedTooltipEditor.class.getMethod("endFrame").invoke(null); }
    TooltipGrid grid() {
        var nativeBackground = mockingDetails(graphics).getInvocations().stream().filter(i -> i.getMethod().getName().equals("blitSprite")
                && i.getArguments().length == 7 && ClientItemContentsTooltip.CONTAINER_SPRITE.equals(i.getArgument(1))).findFirst().orElseThrow();
        return new TooltipGrid((int)nativeBackground.getArgument(2)+7,(int)nativeBackground.getArgument(3)+7);
    }
    @Test void equalCopiedFactoryBindsExactHostAcrossTwoFrames() throws Exception {
        ReservationStore.setData(host, ReservationData.EMPTY.with(1,new ItemStack(Items.STONE)));
        frame(300,160,true);
        assertNotNull(((TooltipSourceAccess)(Object)lastImage).containerSlotReservations$getHost());
        assertSame(hostSlot, ((TooltipSourceAccess)(Object)lastImage).containerSlotReservations$getHost().slot());
        var grid = grid(); frame(grid.x()+9,grid.y()+9,false);
        assertSame(hostSlot, ((TooltipSourceAccess)(Object)lastImage).containerSlotReservations$getHost().slot());
        assertTrue(NestedTooltipEditor.hovered(lastClient)>=0);
    }
    @Test void sodiumAuxiliaryConstructorCannotDestroySameObjectHostBetweenFrames() throws Exception {
        factoryCopy=false;
        ReservationStore.setData(host,ReservationData.EMPTY.with(1,new ItemStack(Items.STONE)));
        frame(300,160,true); var grid=grid(); var first=lastClient;
        frame(grid.x()+9,grid.y()+9,false);
        assertNotSame(first,lastClient,"The next frame must resubmit the exact host despite Sodium's auxiliary constructor");
        assertEquals(0,NestedTooltipEditor.hovered(lastClient));
    }
    void select(int slot) throws Exception {
        frame(300,160,true); TooltipGrid before=grid();
        frame(before.x()+18*(slot%9)+9,before.y()+18*(slot/9)+9,false);
        assertEquals(before,grid(),"Retained tooltip must remain anchored");
        assertEquals(slot,NestedTooltipEditor.hovered(lastClient));
    }
    void contents(int occupied, int reserved) {
        var entries=NonNullList.withSize(27,ItemStack.EMPTY);
        if(occupied>=0) entries.set(occupied,new ItemStack(Items.STONE));
        host.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(entries));
        ReservationStore.setData(host,reserved<0?ReservationData.EMPTY:ReservationData.EMPTY.with(reserved,new ItemStack(Items.DIRT)));
    }
    void assertNativeHighlight(int slot,TooltipGrid grid) {
        for(var sprite:List.of("container/slot_highlight_back","container/slot_highlight_front")) {
            long submissions=mockingDetails(graphics).getInvocations().stream().filter(i -> i.getMethod().getName().equals("blitSprite")
                    && i.getArguments().length==6 && i.getArgument(1).toString().equals("minecraft:"+sprite)
                    && (int)i.getArgument(2)==grid.x()+18*(slot%9)-3 && (int)i.getArgument(3)==grid.y()+18*(slot/9)-3).count();
            assertEquals(1,submissions,"Native back/front submission for logical cell "+slot);
        }
        long physicalSubmissions=mockingDetails(graphics).getInvocations().stream().filter(i -> i.getMethod().getName().equals("item")
                && i.getArguments().length==4 && (int)i.getArgument(3)==slot
                && ((ItemStack)i.getArgument(0)).isEmpty()==lastImage.itemList().get(slot).isEmpty()).count();
        assertEquals(1,physicalSubmissions,"Both boolean handlers must prevent duplicate selected-cell drawing");
    }
    @ParameterizedTest @CsvSource({"1,0","1,1","1,2","1,3","1,4","2,0","2,1","2,2","2,3","2,4","3,0","3,4"})
    void all27CellsAtScalesAndScreenEdges(int scale,int edge) throws Exception {
        int width=960/scale,height=720/scale;
        when(window.getGuiScaledWidth()).thenReturn(width); when(window.getGuiScaledHeight()).thenReturn(height);
        int anchorX=switch(edge){case 1->1;case 2->width-2;default->width/2;};
        int anchorY=switch(edge){case 3->1;case 4->height-2;default->height/2;};
        for(boolean physicallyEmpty:List.of(true,false)) {
            contents(physicallyEmpty?-1:0,1);
            NestedTooltipEditor.beginFrame(); NestedTooltipEditor.beginFrame(); finishFrame();
            frame(anchorX,anchorY,true); TooltipGrid initial=grid();
            for(int slot=0;slot<27;slot++) {
                frame(initial.x()+18*(slot%9)+9,initial.y()+18*(slot/9)+9,false);
                assertEquals(initial,grid()); assertEquals(slot,NestedTooltipEditor.hovered(lastClient));
                assertNativeHighlight(slot,initial);
                assertSame(hostSlot,((TooltipSourceAccess)(Object)lastImage).containerSlotReservations$getHost().slot());
                assertEquals(27,lastImage.itemList().size());
                if(physicallyEmpty) assertTrue(lastImage.itemList().stream().allMatch(ItemStack::isEmpty));
            }
        }
    }
    @ParameterizedTest @CsvSource({"true,false,false,SLOT_STACK","true,true,false,SLOT_STACK","false,true,false,CLEAR_EMPTY", "false,true,true,CARRIED_STACK","false,false,true,CARRIED_STACK","false,false,false,NONE"})
    void exactNestedActions(boolean occupied,boolean reserved,boolean cursor,String action) throws Exception {
        int slot=8; contents(occupied?slot:0,reserved?slot:1);
        if(cursor) menu.setCarried(new ItemStack(Items.DIRT));
        select(slot); assertNativeHighlight(slot,grid());
        ItemStack before=host.copy(); int beforeQueries=queries;
        if(action.equals("NONE")) { assertEquals(Boolean.FALSE,NestedTooltipEditor.send()); assertTrue(packets.isEmpty()); }
        else {
            assertEquals(Boolean.TRUE,NestedTooltipEditor.send()); assertEquals(1,packets.size());
            var packet=packets.getFirst(); assertEquals(menu.containerId,packet.menuId()); assertEquals(hostSlot.index,packet.hostSlot());
            assertEquals(slot,packet.nestedSlot()); assertEquals(ReservationActionPayload.Source.valueOf(action),packet.source());
        }
        assertTrue(ItemStack.matches(before,host),"Client action must only predict acknowledgement, never mutate host");
        assertEquals(beforeQueries,queries);
    }
    @ParameterizedTest @ValueSource(strings={"screen","menu","menuId","state","slot","moved","count","pointer","frame","tooltip","collapsed","closed","size"})
    void staleStateNeverTargetsACell(String stale) throws Exception {
        contents(0,1); select(1); var old=lastClient;
        switch(stale) {
            case "screen" -> when(gui.screen()).thenReturn(mock(AbstractContainerScreen.class));
            case "menu" -> player.containerMenu=new TestMenu();
            case "menuId" -> set(menu,"containerId",72);
            case "state" -> menu.incrementStateId();
            case "slot" -> menu.slots.set(hostSlot.index,new Slot(hostSlot.container,hostSlot.getContainerSlot(),0,0));
            case "moved" -> hostSlot.set(ItemStack.EMPTY);
            case "count" -> host.setCount(2);
            case "pointer" -> pointerX+=1;
            case "frame" -> NestedTooltipEditor.beginFrame();
            case "tooltip" -> old=new ClientItemContentsTooltip(lastImage);
            case "collapsed" -> { fuzs.iteminteractions.common.impl.ItemInteractions.CLIENT.itemStorageTooltip=fuzs.iteminteractions.common.impl.config.ItemStorageTooltip.NEVER; frame((int)pointerX,(int)pointerY,false); }
            case "closed" -> frame(630,350,false);
            case "size" -> { when(window.getGuiScaledWidth()).thenReturn(320); frame((int)pointerX,(int)pointerY,false); }
        }
        assertEquals(-1,NestedTooltipEditor.hovered(old));
        if(!stale.equals("tooltip")) assertNull(NestedTooltipEditor.send());
        assertTrue(packets.isEmpty());
    }
    @Test void duplicateIdenticalShulkersNeverRedirectTheHost() throws Exception {
        contents(0,1); menu.slots.get(1).set(host.copy()); select(1);
        assertSame(hostSlot,((TooltipSourceAccess)(Object)lastImage).containerSlotReservations$getHost().slot());
        set(screen,"hoveredSlot",menu.slots.get(1));
        assertEquals(Boolean.TRUE,NestedTooltipEditor.send());
        assertEquals(0,packets.getFirst().hostSlot(),"Nested grid retains priority over an underlying outer slot");
    }
    @Test void finalReservationClearRestoresNativeNoTooltip() throws Exception {
        contents(-1,1); select(1); assertEquals(Boolean.TRUE,NestedTooltipEditor.send());
        ReservationStore.setData(host,ReservationData.EMPTY); menu.incrementStateId();
        frame((int)pointerX,(int)pointerY,false);
        assertNull(lastImage); assertEquals(-1,NestedTooltipEditor.hovered(lastClient)); assertNull(NestedTooltipEditor.send());
    }
    @Test void repeatedImageQueriesHaveOneScopeAndNoLeakedBinding() throws Exception {
        contents(0,1); menu.setCarried(new ItemStack(Items.DIRT)); frame(300,160,true);
        assertEquals(2,queries,"Native cursor-present path probes visibility then schedules a second image");
        assertNotNull(((TooltipSourceAccess)(Object)lastImage).containerSlotReservations$getHost());
        assertNull(NestedTooltipEditor.capture(host.copy()),"Scheduling scope ends before deferred extraction");
    }
    @Test void exactCellCornersAndRowBoundariesUseFinalNativeGeometry() throws Exception {
        contents(0,1); frame(300,160,true); var grid=grid();
        for(int slot=0;slot<27;slot++) for(int dx:List.of(0,17)) for(int dy:List.of(0,17)) {
            frame(grid.x()+18*(slot%9)+dx,grid.y()+18*(slot/9)+dy,false);
            assertEquals(slot,NestedTooltipEditor.hovered(lastClient)); assertEquals(grid,grid());
        }
        // The border remains a retention bridge, never an actionable fake cell.
        frame(grid.x()-1,grid.y()+9,false); assertEquals(-1,NestedTooltipEditor.hovered(lastClient)); assertNull(NestedTooltipEditor.send());
    }
    @Test void changedOuterSlotOutsideGridDoesNotRetainPreviousHost() throws Exception {
        contents(0,1); frame(300,160,true); var old=lastClient;
        NestedTooltipEditor.beginFrame(); pointerX=298; pointerY=158;
        set(screen,"hoveredSlot",menu.slots.get(1));
        assertFalse(NestedTooltipEditor.retainPreview(screen,graphics,298,158));
        finishFrame(); assertEquals(-1,NestedTooltipEditor.hovered(old));
    }
    @Test void multicountHostCannotAcquireBinding() throws Exception {
        contents(0,1); host.setCount(2); frame(300,160,true);
        assertTrue(lastImage==null || ((TooltipSourceAccess)(Object)lastImage).containerSlotReservations$getHost()==null);
        assertEquals(-1,NestedTooltipEditor.hovered(lastClient)); assertNull(NestedTooltipEditor.send());
    }
    @Test void oldTooltipCannotRepublishAStaleFrameBinding() throws Exception {
        contents(0,1); select(1); var obsolete=lastClient; var initial=grid();
        frame(initial.x()+9,initial.y()+9,false);
        obsolete.extractImage(font,initial.x()-7,initial.y()-7,176,68,graphics);
        assertEquals(-1,NestedTooltipEditor.hovered(obsolete)); assertEquals(0,NestedTooltipEditor.hovered(lastClient));
    }
    @Test void occupiedExactTogglePredictsClearWithoutChangingClientContents() throws Exception {
        contents(8,8); ReservationStore.setData(host,ReservationData.EMPTY.with(8,new ItemStack(Items.STONE)));
        select(8); assertEquals(Boolean.TRUE,NestedTooltipEditor.send());
        var expected=NestedTooltipEditor.class.getDeclaredField("expectedReply"); expected.setAccessible(true);
        assertTrue(ReservationStore.getData((ItemStack)expected.get(null)).isEmpty());
        assertFalse(ReservationStore.getData(host).isEmpty());
        assertEquals(Boolean.FALSE,NestedTooltipEditor.send(),"Wait for server acknowledgement");
    }
    @Test void installedSodiumHookProvesTheAuxiliaryConstructorOrder() throws Exception {
        var path=java.nio.file.Path.of(System.getProperty("sodiumConsoleJar"));
        var bytes=java.nio.file.Files.readAllBytes(path);
        assertEquals("a6d1ced177a1c57147e8c1c8043650d5f93e1f2824c9952f3fb67ae05633a0a7",
                java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
        try(var zip=new java.util.zip.ZipFile(path.toFile())) {
            var node=new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(zip.getInputStream(zip.getEntry(
                    "net/caffeinemc/mods/sodium/mixin/features/gui/hooks/console/GameRendererMixin.class"))).accept(node,0);
            var onRender=node.methods.stream().filter(m->m.name.equals("onRender")).findFirst().orElseThrow();
            var inject=onRender.visibleAnnotations.stream().filter(a->a.desc.endsWith("/Inject;")).findFirst().orElseThrow();
            assertEquals(List.of("render"),inject.values.get(inject.values.indexOf("method")+1));
            var at=(org.objectweb.asm.tree.AnnotationNode)((List<?>)inject.values.get(inject.values.indexOf("at")+1)).getFirst();
            assertEquals("Lnet/minecraft/client/gui/render/GuiRenderer;render()V",at.values.get(at.values.indexOf("target")+1));
            int ctors=0;
            for(var i:onRender.instructions) if(i instanceof org.objectweb.asm.tree.MethodInsnNode call
                    && call.owner.equals("net/minecraft/client/gui/GuiGraphicsExtractor") && call.name.equals("<init>")) {
                assertEquals("(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;II)V",call.desc); ctors++;
            }
            assertEquals(1,ctors);
        }
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void realPlayerInventoryAndSharedChestHosts(boolean playerInventory) throws Exception {
        var inventory=new net.minecraft.world.entity.player.Inventory(player,new net.minecraft.world.entity.EntityEquipment());
        menu=playerInventory?new InventoryMenu(inventory,true,player):ChestMenu.threeRows(71,inventory,new SimpleContainer(27));
        player.containerMenu=menu; set(screen,"menu",menu);
        hostSlot=menu.slots.get(playerInventory?9:0); hostSlot.set(host); contents(0,1);
        select(26); assertEquals(Boolean.FALSE,NestedTooltipEditor.send());
        assertSame(hostSlot,((TooltipSourceAccess)(Object)lastImage).containerSlotReservations$getHost().slot());
    }
    static Object invoke(Object instance,String name,Class<?>[] signature,Object... args) throws Exception {
        Class<?> type=instance.getClass();
        while(type!=null) { try { Method method=type.getDeclaredMethod(name,signature); method.setAccessible(true); return method.invoke(instance,args); }
            catch(NoSuchMethodException e) {type=type.getSuperclass();} }
        throw new NoSuchMethodException(name);
    }
    static void set(Object instance,String name,Object value) throws Exception {
        Class<?> type=instance.getClass();
        while(type!=null) { try { Field field=type.getDeclaredField(name); field.setAccessible(true); field.set(instance,value); return; }
            catch(NoSuchFieldException e) {type=type.getSuperclass();} }
        throw new NoSuchFieldException(name);
    }
    static class TestMenu extends AbstractContainerMenu {
        TestMenu() { super(MenuType.GENERIC_9x3,42); var owner=new SimpleContainer(27); for(int i=0;i<27;i++) addSlot(new Slot(owner,i,8+18*(i%9),18+18*(i/9))); }
        public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
        public boolean stillValid(Player player) { return true; }
    }
}
