package org.vmstudio.visor.loader.neoforge;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.core.common.addon.AddonManagerImpl;

@Mod(VisorAPI.MOD_ID)
public class VisorMod {

    public VisorMod(final IEventBus modEventBus){
        // Real NeoForge 1.21.1 order (CommonModLoader.finish): FMLLoadCompleteEvent
        // fires FIRST — registerNetworkChannel calls land there and are buffered in
        // NeoForgeModLoader.pendingChannels. RegisterPayloadHandlersEvent fires
        // AFTER it, draining the buffer in onRegisterPayloads.
        NeoForgeModLoader loader = (NeoForgeModLoader) ModLoader.get();
        modEventBus.addListener(loader::onRegisterPayloads);

        modEventBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(final FMLLoadCompleteEvent event){
        event.enqueueWork(AddonManagerImpl::register);
    }


}
