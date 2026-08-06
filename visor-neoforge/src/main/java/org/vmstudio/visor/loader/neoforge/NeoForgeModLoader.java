package org.vmstudio.visor.loader.neoforge;


import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.NotNull;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.render.RenderPipelineCallback;
import org.vmstudio.visor.api.client.render.RenderPipelineStage;
import org.vmstudio.visor.api.common.VRException;
import org.vmstudio.visor.api.common.network.VisorChannel;
import org.vmstudio.visor.api.common.network.VisorPayloadToClient;
import org.vmstudio.visor.api.common.network.VisorPayloadToServer;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class NeoForgeModLoader implements ModLoader {
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("visor-neoforge");

    private File configFolder = FMLPaths.CONFIGDIR.get().toFile();

    private final Map<RenderPipelineStage, List<RenderPipelineCallback>> pipelineCallbacks
            = new EnumMap<>(RenderPipelineStage.class);

    private boolean levelStageListenerRegistered = false;

    /**
     * Channels registered before {@link RegisterPayloadHandlersEvent}
     * are buffered here and registered in {@link #onRegisterPayloads}.
     */
    private final List<VisorChannel> pendingChannels = new CopyOnWriteArrayList<>();
    private volatile boolean payloadsRegistered = false;


    @Override
    public File getConfigFolder() {
        return configFolder;
    }

    @Override
    public boolean isModLoaded(@NotNull String id) {
        return FMLLoader.getLoadingModList().getModFileById(id) != null;
    }

    @Override
    public @NotNull String getModVersion(@NotNull String id) {
        if (isModLoaded(VisorAPI.MOD_ID)) {
            return FMLLoader.getLoadingModList()
                    .getModFileById(id).versionString();
        }
        return "no version";
    }

    @Override
    public boolean isDedicatedServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }


    @Override
    public void addToRenderPipeline(@NotNull RenderPipelineStage stage,
                                    @NotNull RenderPipelineCallback callback) {
        pipelineCallbacks
                .computeIfAbsent(stage, k -> new CopyOnWriteArrayList<>())
                .add(callback);

        if (!levelStageListenerRegistered) {
            NeoForge.EVENT_BUS.addListener(this::onRenderLevelStage);
            levelStageListenerRegistered = true;
        }
    }



    @Override
    public boolean enableRenderTargetStencil(@NotNull RenderTarget renderTarget) {
        renderTarget.enableStencil();
        return true;
    }

    @Override
    public double getItemEntityReach(double baseRange, ItemStack itemStack, EquipmentSlot slot) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = LinkedHashMultimap.create();
        itemStack.forEachModifier(EquipmentSlotGroup.bySlot(slot), modifiers::put);
        Collection<AttributeModifier> attributes = modifiers.get(Attributes.ENTITY_INTERACTION_RANGE);
        for (AttributeModifier entry : attributes) {
            if (entry.operation() == AttributeModifier.Operation.ADD_VALUE) {
                baseRange += entry.amount();
            }
        }
        double totalRange = baseRange;
        for (AttributeModifier entry : attributes) {
            if (entry.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                totalRange += baseRange * entry.amount();
            }
        }
        for (AttributeModifier entry : attributes) {
            if (entry.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                totalRange *= 1.0 + entry.amount();
            }
        }
        return totalRange;
    }

    @Override
    public @NotNull List<Class<?>> getClassesAnnotated(@NotNull Class<? extends Annotation> annotation,
                                                       @NotNull String modId,
                                                       @NotNull String packagePath) {
        List<Class<?>> result = new ArrayList<>();
        ModFileInfo info = FMLLoader.getLoadingModList().getModFileById(modId);
        if (info == null) {
            return result;
        }

        ModFileScanData scanData = info.getFile().getScanResult();
        String annotationName = annotation.getName();

        for (var annotationData : scanData.getAnnotations()) {
            String className = annotationData.clazz().getClassName();

            if (!className.startsWith(packagePath)) {
                continue;
            }
            if (!annotationData.annotationType()
                    .getClassName().equals(annotationName)) {
                continue;
            }

            try {
                Class<?> cls = Class.forName(className, false,
                        Thread.currentThread().getContextClassLoader());
                result.add(cls);
            } catch (ClassNotFoundException e) {
                throw new VRException(e);
            }
        }

        return result;

    }

    // ----- NETWORK -----

    /**
     * Fired on the mod bus by {@link VisorMod} (constructor phase), BEFORE
     * {@code ModLoader.registerNetworkChannel} calls arrive (they happen on
     * {@code FMLLoadCompleteEvent}). Channels buffered in
     * {@link #pendingChannels} are registered here, one payload type per channel.
     */
    public void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("visor");
        for (VisorChannel channel : pendingChannels) {
            registerChannel(channel, registrar);
        }
        pendingChannels.clear();
        payloadsRegistered = true;
    }

    @Override
    public void registerNetworkChannel(@NotNull VisorChannel channel) {
        if (!payloadsRegistered) {
            // Before RegisterPayloadHandlersEvent: buffer, will be registered in onRegisterPayloads.
            pendingChannels.add(channel);
        } else {
            // After the event the NeoForge NetworkRegistry is frozen: the payload
            // can no longer be registered, sending will still work for the channel.
            LOGGER.warn(
                    "VisorChannel '{}' registered after RegisterPayloadHandlersEvent: "
                            + "payload handlers will NOT be available for it.",
                    channel.getChannelId());
        }
    }

    private void registerChannel(VisorChannel channel, PayloadRegistrar registrar) {
        ResourceLocation id = channel.getChannelId();
        PayloadRegistrar channelRegistrar = registrar
                .versioned(String.valueOf(channel.getNetworkVersion()));
        StreamCodec<RegistryFriendlyByteBuf, ChannelPayload> codec = channelCodec(id);

        if (channel.hasPacketsToServer()) {
            channelRegistrar.playToServer(new CustomPacketPayload.Type<>(id), codec,
                    (payload, context) -> handleToServer(payload, channel, context));
        }
        if (channel.hasPacketsToClient()) {
            channelRegistrar.playToClient(new CustomPacketPayload.Type<>(id), codec,
                    (payload, context) -> handleToClient(payload, channel, context));
        }
    }

    private static StreamCodec<RegistryFriendlyByteBuf, ChannelPayload> channelCodec(ResourceLocation id) {
        return new StreamCodec<>() {
            @Override
            public ChannelPayload decode(RegistryFriendlyByteBuf buf) {
                // Hand a copy to the channel: the existing VisorPayload serialization
                // (read byte-id + payload) is performed by VisorChannel.handleToServer/ToClient.
                FriendlyByteBuf copy = new FriendlyByteBuf(buf.copy());
                return new ChannelPayload(id, copy);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, ChannelPayload payload) {
                buf.writeBytes(payload.data(), payload.data().readerIndex(), payload.data().readableBytes());
            }
        };
    }

    private void handleToServer(ChannelPayload payload, VisorChannel channel, IPayloadContext context) {
        var player = context.player();
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sender)) {
            return;
        }
        context.enqueueWork(() -> channel.handleToServer(payload.data(), sender,
                p -> PacketDistributor.sendToPlayer(sender, makePayload(channel.getChannelId(), p))));
    }

    private void handleToClient(ChannelPayload payload, VisorChannel channel, IPayloadContext context) {
        context.enqueueWork(() -> channel.handleToClient(payload.data()));
    }

    @Override
    public @NotNull Packet<?> createPacketToClient(@NotNull ResourceLocation channelId,
                                                   @NotNull VisorPayloadToClient payload) {
        return new ChannelPayload(channelId, writePayload(payload)).toVanillaClientbound();
    }

    @Override
    public @NotNull Packet<?> createPacketToServer(@NotNull ResourceLocation channelId,
                                                   @NotNull VisorPayloadToServer payload) {
        return new ChannelPayload(channelId, writePayload(payload)).toVanillaServerbound();
    }

    private static FriendlyByteBuf writePayload(VisorPayloadToClient payload) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buffer);
        return buffer;
    }

    private static FriendlyByteBuf writePayload(VisorPayloadToServer payload) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buffer);
        return buffer;
    }

    private static ChannelPayload makePayload(ResourceLocation channelId, VisorPayloadToClient payload) {
        return new ChannelPayload(channelId, writePayload(payload));
    }

    @Override
    public boolean renderWaterOverlay(Player player, PoseStack mat) {
        return ClientHooks.renderWaterOverlay(player, mat);
    }
    @Override
    public boolean renderFireOverlay(Player player, PoseStack mat) {
        return ClientHooks.renderFireOverlay(player, mat);
    }

    @Override
    public @NotNull LoaderType getType() {
        return LoaderType.FORGE;
    }


    // ----- INNER -----

    private void onRenderLevelStage(RenderLevelStageEvent event) {
        RenderPipelineStage stage = mapForgeStage(event.getStage());
        if (stage == null) return;

        List<RenderPipelineCallback> callbacks = pipelineCallbacks.get(stage);
        if (callbacks == null || callbacks.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        for (RenderPipelineCallback callback : callbacks) {
            callback.render(poseStack, partialTicks);
        }
    }


    private static RenderPipelineStage mapForgeStage(RenderLevelStageEvent.Stage forgeStage) {
         if (forgeStage == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return RenderPipelineStage.AFTER_SOLID;
        }
        if (forgeStage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return RenderPipelineStage.AFTER_TRANSLUCENT;
        }
        if (forgeStage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return RenderPipelineStage.AFTER_WORLD;
        }
        return null;
    }

    /**
     * Payload wrapping an existing VisorChannel. One type per channel
     * ({@code id = channel.getChannelId()}); the {@link StreamCodec} delegates
     * the byte-level serialization to {@code VisorChannel.handleToServer/ToClient}
     * (existing {@code VisorPayload.write/read} contract is untouched).
     */
    private record ChannelPayload(ResourceLocation channelId, FriendlyByteBuf data)
            implements CustomPacketPayload {

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return new Type<>(channelId);
        }
    }
}
