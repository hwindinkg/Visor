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
        // RegisterPayloadHandlersEvent is fired on the mod bus during the
        // construction phase, before channels arrive (FMLLoadCompleteEvent).
        // NeoForgeModLoader buffers them and registers in onRegisterPayloads.
        NeoForgeModLoader loader = (NeoForgeModLoader) ModLoader.get();
        modEventBus.addListener(loader::onRegisterPayloads);

        modEventBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(final FMLLoadCompleteEvent event){
        event.enqueueWork(AddonManagerImpl::register);
    }


}
