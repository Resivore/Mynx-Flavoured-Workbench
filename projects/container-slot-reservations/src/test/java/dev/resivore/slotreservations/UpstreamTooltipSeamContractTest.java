package dev.resivore.slotreservations;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.BlendFactor;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UpstreamTooltipSeamContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String OUTER_SHA256 =
            "66f803ae8796a0cdd7086e175fe8b96846515710b8b98b9aebc79d52ea2b0c38";
    private static final String NESTED_ENTRY =
            "META-INF/jars/iteminteractions-fabric-26.2.2.jar";
    private static final String NESTED_SHA256 =
            "1745adf294134817986e46bf554a5a6613837602b660e15a15c50bdc2e0a293c";

    @Test
    void exactEasyShulkerArtifactCarriesTheAuditedItemInteractionsBuild() throws IOException {
        Path artifact = testedArtifact();
        assertEquals(639_115L, Files.size(artifact));
        assertEquals(OUTER_SHA256, sha256(Files.readAllBytes(artifact)));

        try (ZipFile outer = new ZipFile(artifact.toFile())) {
            String metadata = text(outer, "fabric.mod.json");
            assertTrue(metadata.contains("\"id\": \"easyshulkerboxes\""));
            assertTrue(metadata.contains("\"version\": \"26.2.3\""));
            assertTrue(metadata.contains(NESTED_ENTRY));

            byte[] nested = bytes(outer, NESTED_ENTRY);
            assertEquals(196_177, nested.length);
            assertEquals(NESTED_SHA256, sha256(nested));

            try (ZipInputStream itemInteractions = new ZipInputStream(new ByteArrayInputStream(nested))) {
                byte[] itemMetadata = bytes(itemInteractions, "fabric.mod.json");
                String itemMetadataText = new String(itemMetadata, StandardCharsets.UTF_8);
                assertTrue(itemMetadataText.contains("\"id\": \"iteminteractions\""));
                assertTrue(itemMetadataText.contains("\"version\": \"26.2.2\""));
            }
        }
    }

    @Test
    void requiredTooltipBytecodeSeamMatchesTheExactSlotLocalOverlayContract() throws IOException {
        byte[] nested;
        try (ZipFile outer = new ZipFile(testedArtifact().toFile())) {
            nested = bytes(outer, NESTED_ENTRY);
        }

        Set<Member> itemContents = members(nested,
                "fuzs/iteminteractions/common/api/v2/world/inventory/tooltip/ItemContentsTooltip.class");
        assertTrue(itemContents.contains(new Member(
                "<init>",
                "(Lnet/minecraft/core/NonNullList;IIII)V",
                Opcodes.ACC_PUBLIC
        )));
        assertTrue(itemContents.contains(new Member(
                "itemList",
                "()Lnet/minecraft/core/NonNullList;",
                Opcodes.ACC_PUBLIC
        )));

        String clientClass =
                "fuzs/iteminteractions/common/api/v2/client/gui/screens/inventory/tooltip/ClientItemContentsTooltip.class";
        Set<Member> clientContents = members(nested, clientClass);
        assertTrue(clientContents.contains(new Member(
                "<init>",
                "(Lfuzs/iteminteractions/common/api/v2/world/inventory/tooltip/ItemContentsTooltip;)V",
                Opcodes.ACC_PUBLIC
        )));
        assertTrue(clientContents.contains(new Member(
                "itemList",
                "Lnet/minecraft/core/NonNullList;",
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL
        )));
        assertTrue(clientContents.contains(new Member(
                "gridWidth",
                "I",
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL
        )));
        assertTrue(clientContents.contains(new Member(
                "gridHeight",
                "I",
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL
        )));
        assertTrue(clientContents.contains(new Member(
                "extractSlot",
                "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphicsExtractor;III)V",
                Opcodes.ACC_PRIVATE
        )));

        Invocations clientCalls = invocations(nested, clientClass, "extractSlot",
                "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphicsExtractor;III)V");
        assertTrue(clientCalls.calls.contains(new Invocation(
                "net/minecraft/client/gui/GuiGraphicsExtractor",
                "item",
                "(Lnet/minecraft/world/item/ItemStack;III)V",
                Opcodes.INVOKEVIRTUAL,
                false
        )));
        assertTrue(clientCalls.calls.contains(new Invocation(
                "net/minecraft/client/gui/GuiGraphicsExtractor",
                "itemDecorations",
                "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
                Opcodes.INVOKEVIRTUAL,
                false
        )));

        String slotHighlightDescriptor =
                "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V";
        Invocation backHighlight = new Invocation(
                "fuzs/iteminteractions/common/impl/config/SlotHighlight",
                "blitBackSprite",
                slotHighlightDescriptor,
                Opcodes.INVOKEVIRTUAL,
                false
        );
        Invocation slotContents = new Invocation(
                "fuzs/iteminteractions/common/api/v2/client/gui/screens/inventory/tooltip/ClientItemContentsTooltip",
                "extractSlot",
                "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphicsExtractor;III)V",
                Opcodes.INVOKEVIRTUAL,
                false
        );
        Invocation frontHighlight = new Invocation(
                "fuzs/iteminteractions/common/impl/config/SlotHighlight",
                "blitFrontSprite",
                slotHighlightDescriptor,
                Opcodes.INVOKEVIRTUAL,
                false
        );
        Invocations highlightedSlotCalls = invocations(
                nested,
                clientClass,
                "extractHighlightSlotContents",
                "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIZ)V"
        );
        assertTrue(highlightedSlotCalls.calls.indexOf(backHighlight)
                        < highlightedSlotCalls.calls.indexOf(slotContents));
        assertTrue(highlightedSlotCalls.calls.indexOf(slotContents)
                        < highlightedSlotCalls.calls.indexOf(frontHighlight),
                "A TAIL injection in extractSlot stays beneath Item Interactions' front highlight");

        String storageClass =
                "fuzs/iteminteractions/common/api/v2/world/item/storage/ContainerStorage.class";
        Set<Member> storageMembers = members(nested, storageClass);
        assertTrue(storageMembers.contains(new Member(
                "createTooltipImageComponent",
                "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/NonNullList;)Lnet/minecraft/world/inventory/tooltip/TooltipComponent;",
                Opcodes.ACC_PUBLIC
        )), "The source-stack factory seam must remain a public instance method");
        Invocations storageCalls = invocations(
                nested,
                storageClass,
                "createTooltipImageComponent",
                "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/NonNullList;)Lnet/minecraft/world/inventory/tooltip/TooltipComponent;"
        );
        assertTrue(storageCalls.calls.contains(new Invocation(
                "fuzs/iteminteractions/common/api/v2/world/inventory/tooltip/ItemContentsTooltip",
                "<init>",
                "(Lnet/minecraft/core/NonNullList;IIII)V",
                Opcodes.INVOKESPECIAL,
                false
        )));
    }

    @Test
    void optionalPseudoMixinHandlersMatchTheAuditedTargetsWithoutHardFuzsLinks()
            throws IOException {
        String config = Files.readString(ROOT.resolve(
                "src/main/resources/container_slot_reservations.client.mixins.json"
        ));
        String[] optionalMixins = {
                "ClientItemContentsTooltipMixin",
                "ContainerStorageTooltipMixin",
                "ItemContentsTooltipSourceMixin"
        };
        for (String mixin : optionalMixins) {
            assertTrue(config.contains('"' + mixin + '"'),
                    "Optional mixin must remain in Fabric's client-only mixin list: " + mixin);
            byte[] mixinClass = classpathEntry(
                    "dev/resivore/slotreservations/mixin/client/" + mixin + ".class"
            );
            assertTrue(classAnnotations(mixinClass).contains(
                    "Lorg/spongepowered/asm/mixin/Pseudo;"
            ));
            assertNoHardFuzsTypeLinks(mixinClass, mixin);
        }

        Set<Member> storageMixin = members(classpathEntry(
                "dev/resivore/slotreservations/mixin/client/ContainerStorageTooltipMixin.class"
        ));
        assertTrue(storageMixin.contains(new Member(
                "containerSlotReservations$captureTooltipSource",
                "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/NonNullList;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",
                Opcodes.ACC_PRIVATE
        )));

        Set<Member> clientMixin = members(classpathEntry(
                "dev/resivore/slotreservations/mixin/client/ClientItemContentsTooltipMixin.class"
        ));
        assertTrue(clientMixin.contains(new Member(
                "containerSlotReservations$captureSource",
                "(Ljava/lang/Object;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
                Opcodes.ACC_PRIVATE
        )));
        assertTrue(clientMixin.contains(new Member(
                "containerSlotReservations$extractReservationOverlay",
                "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIILorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
                Opcodes.ACC_PRIVATE
        )));
    }

    @Test
    void exactMinecraftGhostBlitSeamsRequireOneExplicitAlphaConversion() throws IOException {
        String texturedShader = new String(
                classpathEntry("assets/minecraft/shaders/core/position_tex_color.fsh"),
                StandardCharsets.UTF_8
        );
        assertTrue(texturedShader.contains(
                "vec4 color = texture(Sampler0, texCoord0) * vertexColor;"
        ));
        assertTrue(texturedShader.contains("fragColor = color * ColorModulator;"));
        assertFalse(texturedShader.contains("color.rgb *= color.a"),
                "If the shader begins premultiplying vertex tint, the ghost color contract changes");

        assertEquals(BlendFactor.ONE,
                BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA.color().sourceFactor());
        assertEquals(BlendFactor.ONE_MINUS_SRC_ALPHA,
                BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA.color().destFactor());
        assertEquals(BlendFactor.ONE,
                BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA.alpha().sourceFactor());
        assertEquals(BlendFactor.ONE_MINUS_SRC_ALPHA,
                BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA.alpha().destFactor());
        assertEquals(BlendFactor.SRC_ALPHA,
                BlendFunction.TRANSLUCENT.color().sourceFactor());
        assertEquals(BlendFactor.ONE_MINUS_SRC_ALPHA,
                BlendFunction.TRANSLUCENT.color().destFactor());

        String guiRenderer = "net/minecraft/client/gui/render/GuiRenderer.class";
        String atlasDescriptor =
                "(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;)V";
        byte[] guiRendererClass = classpathEntry(guiRenderer);
        assertTrue(members(guiRendererClass).contains(new Member(
                "submitBlitFromItemAtlas",
                atlasDescriptor,
                Opcodes.ACC_PRIVATE
        )));
        MethodCode atlasBlit = methodCode(
                guiRendererClass,
                "submitBlitFromItemAtlas",
                atlasDescriptor
        );
        assertEquals(1, atlasBlit.negativeOneConstants);
        assertTrue(atlasBlit.fields.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "net/minecraft/client/renderer/RenderPipelines",
                "GUI_TEXTURED_PREMULTIPLIED_ALPHA",
                "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        )));
        assertTrue(atlasBlit.fields.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "com/mojang/blaze3d/textures/FilterMode",
                "NEAREST",
                "Lcom/mojang/blaze3d/textures/FilterMode;"
        )));
        assertTrue(atlasBlit.calls.contains(new Invocation(
                "net/minecraft/client/gui/render/TextureSetup",
                "singleTexture",
                "(Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;)Lnet/minecraft/client/gui/render/TextureSetup;",
                Opcodes.INVOKESTATIC,
                false
        )));

        MethodCode transparentClear = methodCode(guiRendererClass, "<clinit>", "()V");
        assertEquals(1, transparentClear.floatZeroConstants,
                "GuiRenderer.CLEAR_COLOR must continue to be constructed from exactly zero");
        assertTrue(transparentClear.calls.contains(new Invocation(
                "org/joml/Vector4f",
                "<init>",
                "(F)V",
                Opcodes.INVOKESPECIAL,
                false
        )));
        assertTrue(transparentClear.fields.contains(new FieldAccess(
                Opcodes.PUTSTATIC,
                "net/minecraft/client/gui/render/GuiRenderer",
                "CLEAR_COLOR",
                "Lorg/joml/Vector4fc;"
        )));

        MethodCode guiDraw = methodCode(guiRendererClass, "draw", "()V");
        assertTrue(guiDraw.calls.contains(new Invocation(
                "net/minecraft/client/renderer/DynamicUniforms",
                "writeTransform",
                "(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;",
                Opcodes.INVOKEVIRTUAL,
                false
        )), "The GUI final pass must retain its neutral default ColorModulator overload");
        byte[] dynamicUniforms = classpathEntry(
                "net/minecraft/client/renderer/DynamicUniforms.class"
        );
        MethodCode defaultTransform = methodCode(
                dynamicUniforms,
                "writeTransform",
                "(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
        );
        assertTrue(defaultTransform.fields.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "net/minecraft/client/renderer/DynamicUniforms",
                "WHITE",
                "Lorg/joml/Vector4fc;"
        )));
        MethodCode uniformConstants = methodCode(dynamicUniforms, "<clinit>", "()V");
        assertEquals(4, uniformConstants.floatOneConstants,
                "The default GUI ColorModulator must remain neutral RGBA white");
        assertTrue(uniformConstants.calls.contains(new Invocation(
                "org/joml/Vector4f",
                "<init>",
                "(FFFF)V",
                Opcodes.INVOKESPECIAL,
                false
        )));

        String pipRenderer =
                "net/minecraft/client/gui/render/pip/PictureInPictureRenderer.class";
        String pipDescriptor =
                "(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;)V";
        byte[] pipRendererClass = classpathEntry(pipRenderer);
        assertTrue(members(pipRendererClass).contains(new Member(
                "blitTexture",
                pipDescriptor,
                Opcodes.ACC_PROTECTED
        )));
        MethodCode pipBlit = methodCode(pipRendererClass, "blitTexture", pipDescriptor);
        assertEquals(1, pipBlit.negativeOneConstants);
        assertTrue(pipBlit.fields.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "net/minecraft/client/renderer/RenderPipelines",
                "GUI_TEXTURED_PREMULTIPLIED_ALPHA",
                "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        )));
        assertTrue(pipBlit.fields.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "com/mojang/blaze3d/textures/FilterMode",
                "NEAREST",
                "Lcom/mojang/blaze3d/textures/FilterMode;"
        )));
        assertTrue(pipBlit.calls.contains(new Invocation(
                "net/minecraft/client/gui/render/TextureSetup",
                "singleTexture",
                "(Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;)Lnet/minecraft/client/gui/render/TextureSetup;",
                Opcodes.INVOKESTATIC,
                false
        )));

        MethodCode atlasAllocation = methodCode(
                classpathEntry("net/minecraft/client/gui/render/GuiItemAtlas.class"),
                "<init>",
                "(Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;II)V"
        );
        assertTrue(atlasAllocation.fields.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "com/mojang/blaze3d/GpuFormat",
                "RGBA8_UNORM",
                "Lcom/mojang/blaze3d/GpuFormat;"
        )));
        MethodCode atlasRender = methodCode(
                classpathEntry("net/minecraft/client/gui/render/GuiItemAtlas.class"),
                "drawToSlot",
                "(IIZLnet/minecraft/client/renderer/item/ItemStackRenderState;)V"
        );
        assertTrue(atlasRender.fields.contains(new FieldAccess(
                Opcodes.PUTSTATIC,
                "com/mojang/blaze3d/systems/RenderSystem",
                "outputColorTextureOverride",
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"
        )));
        assertTrue(atlasRender.calls.stream().anyMatch(call ->
                call.owner().equals("com/mojang/blaze3d/systems/CommandEncoder")
                        && call.name().equals("clearColorAndDepthTextures")));
        assertTrue(atlasRender.calls.stream().anyMatch(call ->
                call.owner().equals("net/minecraft/client/renderer/feature/FeatureRenderDispatcher")
                        && call.name().equals("renderAllFeatures")));

        MethodCode pipAllocation = methodCode(
                pipRendererClass,
                "prepareTexturesAndProjection",
                "(ZII)V"
        );
        assertTrue(pipAllocation.fields.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "com/mojang/blaze3d/GpuFormat",
                "RGBA8_UNORM",
                "Lcom/mojang/blaze3d/GpuFormat;"
        )));
        assertTrue(pipAllocation.fields.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "net/minecraft/client/gui/render/GuiRenderer",
                "CLEAR_COLOR",
                "Lorg/joml/Vector4fc;"
        )));
        assertTrue(pipAllocation.calls.stream().anyMatch(call ->
                call.owner().equals("com/mojang/blaze3d/systems/CommandEncoder")
                        && call.name().equals("clearColorAndDepthTextures")));

        MethodCode pipPrepare = methodCode(
                pipRendererClass,
                "prepare",
                "(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;I)V"
        );
        assertTrue(pipPrepare.fields.contains(new FieldAccess(
                Opcodes.PUTSTATIC,
                "com/mojang/blaze3d/systems/RenderSystem",
                "outputColorTextureOverride",
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"
        )));
        assertTrue(pipPrepare.calls.stream().anyMatch(call -> call.name().equals("renderToTexture")));

        MethodCode vertices = methodCode(
                classpathEntry("net/minecraft/client/renderer/state/gui/BlitRenderState.class"),
                "buildVertices",
                "(Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"
        );
        assertEquals(4, vertices.calls.stream().filter(call ->
                call.owner().equals("com/mojang/blaze3d/vertex/VertexConsumer")
                        && call.name().equals("setColor")
                        && call.descriptor().equals("(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
                .count());
        MethodCode packedColor = methodCode(
                classpathEntry("com/mojang/blaze3d/vertex/BufferBuilder.class"),
                "putRgba",
                "(JI)V"
        );
        assertEquals(List.of(new Invocation(
                "net/minecraft/util/ARGB",
                "toABGR",
                "(I)I",
                Opcodes.INVOKESTATIC,
                false
        )), packedColor.calls.stream().filter(call ->
                call.owner().equals("net/minecraft/util/ARGB")).toList(),
                "Packed vertex color must only be channel-reordered, never premultiplied");

        Set<Member> atlasMixin = members(classpathEntry(
                "dev/resivore/slotreservations/mixin/client/GuiRendererGhostMixin.class"
        ));
        assertTrue(atlasMixin.contains(new Member(
                "containerSlotReservations$submitAlphaOnlyGhost",
                "(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
                Opcodes.ACC_PRIVATE
        )));
        MethodCode atlasMixinHandler = methodCode(
                classpathEntry("dev/resivore/slotreservations/mixin/client/GuiRendererGhostMixin.class"),
                "containerSlotReservations$submitAlphaOnlyGhost",
                "(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"
        );
        assertTrue(atlasMixinHandler.calls.stream().anyMatch(call ->
                call.owner().equals("dev/resivore/slotreservations/client/GhostItemRenderPipeline")
                        && call.name().equals("blit")));
        assertTrue(atlasMixinHandler.calls.stream().noneMatch(call ->
                call.name().equals("fill") || call.name().equals("clearColorTexture")
                        || call.name().equals("clearColorAndDepthTextures")));
        Set<Member> pipMixin = members(classpathEntry(
                "dev/resivore/slotreservations/mixin/client/PictureInPictureRendererGhostMixin.class"
        ));
        assertTrue(pipMixin.contains(new Member(
                "containerSlotReservations$submitOversizedAlphaOnlyGhost",
                "(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
                Opcodes.ACC_PRIVATE
        )));
        MethodCode pipMixinHandler = methodCode(
                classpathEntry("dev/resivore/slotreservations/mixin/client/PictureInPictureRendererGhostMixin.class"),
                "containerSlotReservations$submitOversizedAlphaOnlyGhost",
                "(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"
        );
        assertTrue(pipMixinHandler.calls.stream().anyMatch(call ->
                call.owner().equals("dev/resivore/slotreservations/client/GhostItemRenderPipeline")
                        && call.name().equals("blit")));
        assertTrue(pipMixinHandler.calls.stream().noneMatch(call ->
                call.name().equals("fill") || call.name().equals("clearColorTexture")
                        || call.name().equals("clearColorAndDepthTextures")));

        String ghostShader = new String(classpathEntry(
                "assets/container_slot_reservations/shaders/core/ghost_item_alpha.fsh"
        ), StandardCharsets.UTF_8);
        assertTrue(ghostShader.contains("float opacity = vertexColor.a;"));
        assertTrue(ghostShader.contains(
                "vec4 ghost = vec4(item.rgb * opacity, item.a * opacity);"
        ));
        assertFalse(ghostShader.contains("vertexColor.rgb"));

        MethodCode customPipeline = methodCode(classpathEntry(
                "dev/resivore/slotreservations/client/GhostItemRenderPipeline.class"
        ), "<clinit>", "()V");
        assertTrue(customPipeline.fields.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "net/minecraft/client/renderer/RenderPipelines",
                "GUI_TEXTURED_SNIPPET",
                "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Snippet;"
        )));
        assertTrue(customPipeline.fields.contains(new FieldAccess(
                Opcodes.GETSTATIC,
                "com/mojang/blaze3d/pipeline/BlendFunction",
                "TRANSLUCENT_PREMULTIPLIED_ALPHA",
                "Lcom/mojang/blaze3d/pipeline/BlendFunction;"
        )));
        assertTrue(customPipeline.calls.contains(new Invocation(
                "net/minecraft/client/renderer/RenderPipelines",
                "register",
                "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)Lcom/mojang/blaze3d/pipeline/RenderPipeline;",
                Opcodes.INVOKESTATIC,
                false
        )));
    }

    private static Path testedArtifact() {
        String configured = System.getProperty("easyShulkerBoxesJar", "");
        assertFalse(configured.isBlank(),
                "The exact Easy Shulker Boxes audit input was not found; set -PeasyShulkerBoxesJar=<path>");
        Path artifact = Path.of(configured);
        assertTrue(Files.isRegularFile(artifact), "Missing upstream audit artifact: " + artifact);
        return artifact;
    }

    private static Set<Member> members(byte[] nestedJar, String classEntry) throws IOException {
        return members(nestedEntry(nestedJar, classEntry));
    }

    private static Set<Member> members(byte[] classBytes) {
        Set<Member> members = new HashSet<>();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                members.add(new Member(name, descriptor, access));
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                members.add(new Member(name, descriptor, access));
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return members;
    }

    private static MethodCode methodCode(
            byte[] classBytes,
            String methodName,
            String methodDescriptor
    ) {
        MethodCode result = new MethodCode();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!methodName.equals(name) || !methodDescriptor.equals(descriptor)) {
                    return null;
                }
                result.found = true;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name,
                                               String descriptor) {
                        result.fields.add(new FieldAccess(opcode, owner, name, descriptor));
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                                                String descriptor, boolean isInterface) {
                        result.calls.add(new Invocation(
                                owner,
                                name,
                                descriptor,
                                opcode,
                                isInterface
                        ));
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ICONST_M1) {
                            result.negativeOneConstants++;
                        } else if (opcode == Opcodes.FCONST_0) {
                            result.floatZeroConstants++;
                        } else if (opcode == Opcodes.FCONST_1) {
                            result.floatOneConstants++;
                        }
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (Integer.valueOf(-1).equals(value)) {
                            result.negativeOneConstants++;
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertTrue(result.found, "Required Minecraft method seam is missing: "
                + methodName + methodDescriptor);
        return result;
    }

    private static Invocations invocations(
            byte[] nestedJar,
            String classEntry,
            String methodName,
            String methodDescriptor
    ) throws IOException {
        byte[] classBytes = nestedEntry(nestedJar, classEntry);
        Invocations result = new Invocations();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!methodName.equals(name) || !methodDescriptor.equals(descriptor)) {
                    return null;
                }
                result.found = true;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                                                String descriptor, boolean isInterface) {
                        result.calls.add(new Invocation(
                                owner,
                                name,
                                descriptor,
                                opcode,
                                isInterface
                        ));
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertTrue(result.found, "Required upstream method seam is missing: "
                + methodName + methodDescriptor);
        return result;
    }

    private static byte[] classpathEntry(String name) throws IOException {
        try (InputStream input = UpstreamTooltipSeamContractTest.class
                .getClassLoader()
                .getResourceAsStream(name)) {
            assertNotNull(input, "Missing compiled classpath entry: " + name);
            return input.readAllBytes();
        }
    }

    private static Set<String> classAnnotations(byte[] classBytes) {
        Set<String> annotations = new HashSet<>();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                annotations.add(descriptor);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return annotations;
    }

    private static void assertNoHardFuzsTypeLinks(byte[] classBytes, String mixinName) {
        Set<String> references = hardTypeReferences(classBytes);
        Set<String> fuzsReferences = new HashSet<>();
        references.stream()
                .filter(reference -> reference.contains("fuzs/"))
                .forEach(fuzsReferences::add);
        assertTrue(fuzsReferences.isEmpty(),
                "Optional mixin has hard Fuzs linkage instead of string targets: "
                        + mixinName + " -> " + fuzsReferences);
    }

    /** Collects JVM type links while deliberately ignoring soft target names in annotations. */
    private static Set<String> hardTypeReferences(byte[] classBytes) {
        Set<String> references = new HashSet<>();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                addInternalName(references, superName);
                if (interfaces != null) {
                    for (String implemented : interfaces) {
                        addInternalName(references, implemented);
                    }
                }
                addSignature(references, signature);
            }

            @Override
            public void visitOuterClass(String owner, String name, String descriptor) {
                addInternalName(references, owner);
                addDescriptor(references, descriptor);
            }

            @Override
            public RecordComponentVisitor visitRecordComponent(
                    String name,
                    String descriptor,
                    String signature
            ) {
                addDescriptor(references, descriptor);
                addSignature(references, signature);
                return null;
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                addDescriptor(references, descriptor);
                addSignature(references, signature);
                if (value instanceof Type type) {
                    addType(references, type);
                }
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                addDescriptor(references, descriptor);
                addSignature(references, signature);
                if (exceptions != null) {
                    for (String exception : exceptions) {
                        addInternalName(references, exception);
                    }
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        addInternalName(references, type);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name,
                                               String descriptor) {
                        addInternalName(references, owner);
                        addDescriptor(references, descriptor);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name,
                                                String descriptor, boolean isInterface) {
                        addInternalName(references, owner);
                        addDescriptor(references, descriptor);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(
                            String name,
                            String descriptor,
                            Handle bootstrapMethodHandle,
                            Object... bootstrapMethodArguments
                    ) {
                        addDescriptor(references, descriptor);
                        addHandle(references, bootstrapMethodHandle);
                        for (Object argument : bootstrapMethodArguments) {
                            if (argument instanceof Type type) {
                                addType(references, type);
                            } else if (argument instanceof Handle handle) {
                                addHandle(references, handle);
                            }
                        }
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Type type) {
                            addType(references, type);
                        } else if (value instanceof Handle handle) {
                            addHandle(references, handle);
                        }
                    }

                    @Override
                    public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
                        addDescriptor(references, descriptor);
                    }

                    @Override
                    public void visitTryCatchBlock(
                            org.objectweb.asm.Label start,
                            org.objectweb.asm.Label end,
                            org.objectweb.asm.Label handler,
                            String type
                    ) {
                        addInternalName(references, type);
                    }

                    @Override
                    public void visitLocalVariable(
                            String name,
                            String descriptor,
                            String signature,
                            org.objectweb.asm.Label start,
                            org.objectweb.asm.Label end,
                            int index
                    ) {
                        addDescriptor(references, descriptor);
                        addSignature(references, signature);
                    }
                };
            }
        }, 0);
        return references;
    }

    private static void addHandle(Set<String> references, Handle handle) {
        addInternalName(references, handle.getOwner());
        addDescriptor(references, handle.getDesc());
    }

    private static void addDescriptor(Set<String> references, String descriptor) {
        if (descriptor == null) {
            return;
        }
        addType(references, descriptor.startsWith("(")
                ? Type.getMethodType(descriptor)
                : Type.getType(descriptor));
    }

    private static void addType(Set<String> references, Type type) {
        switch (type.getSort()) {
            case Type.ARRAY -> addType(references, type.getElementType());
            case Type.OBJECT -> addInternalName(references, type.getInternalName());
            case Type.METHOD -> {
                addType(references, type.getReturnType());
                for (Type argument : type.getArgumentTypes()) {
                    addType(references, argument);
                }
            }
            default -> {
            }
        }
    }

    private static void addInternalName(Set<String> references, String name) {
        if (name != null) {
            references.add(name);
        }
    }

    private static void addSignature(Set<String> references, String signature) {
        if (signature != null && signature.contains("fuzs/")) {
            references.add(signature);
        }
    }

    private static byte[] nestedEntry(byte[] nestedJar, String name) throws IOException {
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(nestedJar))) {
            return bytes(input, name);
        }
    }

    private static String text(ZipFile zip, String name) throws IOException {
        return new String(bytes(zip, name), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        assertNotNull(entry, "Missing archive entry: " + name);
        try (var input = zip.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static byte[] bytes(ZipInputStream input, String name) throws IOException {
        for (ZipEntry entry; (entry = input.getNextEntry()) != null; ) {
            if (name.equals(entry.getName())) {
                return input.readAllBytes();
            }
        }
        throw new AssertionError("Missing nested archive entry: " + name);
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private record Member(String name, String descriptor, int access) {
    }

    private record Invocation(
            String owner,
            String name,
            String descriptor,
            int opcode,
            boolean isInterface
    ) {
    }

    private record FieldAccess(int opcode, String owner, String name, String descriptor) {
    }

    private static final class Invocations {
        private boolean found;
        private final List<Invocation> calls = new ArrayList<>();
    }

    private static final class MethodCode {
        private boolean found;
        private int negativeOneConstants;
        private int floatZeroConstants;
        private int floatOneConstants;
        private final List<FieldAccess> fields = new ArrayList<>();
        private final List<Invocation> calls = new ArrayList<>();
    }
}
