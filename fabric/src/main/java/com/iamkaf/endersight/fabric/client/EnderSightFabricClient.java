package com.iamkaf.endersight.fabric.client;

import com.iamkaf.amber.api.level.LevelHelper;
import com.iamkaf.endersight.EnderSightClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public final class EnderSightFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(EnderSightClient.MASTER_TOGGLE);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (EnderSightClient.MASTER_TOGGLE.consumeClick()) {
                EnderSightClient.onKeybindPressed();
            }

            Level level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            LevelHelper.runEveryXTicks(level, EnderSightClient.ITEM_CHECK_INTERVAL, (time) -> {
                EnderSightClient.updateEnderSightState();
            });
        });
    }
}
