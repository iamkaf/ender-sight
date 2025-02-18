package com.iamkaf.endersight;

import com.iamkaf.amber.api.player.FeedbackHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class EnderSightClient {
    public static final KeyMapping MASTER_TOGGLE = new KeyMapping(
            "key.endersight.endersight",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.endersight"
    );
    public static final int ITEM_CHECK_INTERVAL = 20;
    public static boolean ENDER_SIGHT_ENABLED = false;
    public static boolean ENDER_SIGHT_OVERRIDE = true;
    public static List<Item> ENDER_SIGHT_ITEMS = List.of(Items.ENDER_PEARL, Items.ENDER_EYE);

    public static void onKeybindPressed() {
        EnderSight.LOGGER.info("Ender Sight toggle key pressed.");
        ENDER_SIGHT_OVERRIDE = !ENDER_SIGHT_OVERRIDE;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;

        if (level == null || player == null) {
            return;
        }

        FeedbackHelper.actionBarMessage(
                player,
                Component.literal("Ender Sight: " + (ENDER_SIGHT_OVERRIDE ? "Enabled" : "Disabled"))
        );
    }

    public static void updateEnderSightState() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;

        if (level == null || player == null) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ENDER_SIGHT_ENABLED =
                ENDER_SIGHT_ITEMS.contains(mainHand.getItem()) || ENDER_SIGHT_ITEMS.contains(offHand.getItem());
    }
}
