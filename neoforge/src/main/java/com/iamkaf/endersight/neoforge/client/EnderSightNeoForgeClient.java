package com.iamkaf.endersight.neoforge.client;

import com.iamkaf.amber.api.level.LevelHelper;
import com.iamkaf.endersight.EnderSight;
import com.iamkaf.endersight.EnderSightClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = EnderSight.MOD_ID, dist = Dist.CLIENT)
public class EnderSightNeoForgeClient {
    public EnderSightNeoForgeClient(ModContainer modContainer) {
        IEventBus modBus = modContainer.getEventBus();
        IEventBus gameBus = NeoForge.EVENT_BUS;

        assert modBus != null;
        modBus.addListener(EnderSightNeoForgeClient::onRegisterKeyMappingsEvent);
        gameBus.addListener(EnderSightNeoForgeClient::onClientTickEvent);
    }

    public static void onRegisterKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        event.register(EnderSightClient.MASTER_TOGGLE);
    }

    public static void onClientTickEvent(ClientTickEvent.Post event) {
        while (EnderSightClient.MASTER_TOGGLE.consumeClick()) {
            EnderSightClient.onKeybindPressed();
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        LevelHelper.runEveryXTicks(
                level, EnderSightClient.ITEM_CHECK_INTERVAL, (time) -> {
                    EnderSightClient.updateEnderSightState();
                }
        );
    }
}
