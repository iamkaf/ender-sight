package com.iamkaf.endersight.client;

import com.iamkaf.amber.api.functions.v1.ClientFunctions;
import com.iamkaf.endersight.EnderSight;
import com.iamkaf.endersight.ModRegistry;
import com.iamkaf.endersight.item.EnderSpyglassItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The Ender Spyglass scope: its own frame, plus an eye reticle that opens once letting go would teleport. The label
 * under it says how far the jump is, or why it can't happen.
 */
public final class SpyglassOverlay {
    public static final Identifier SCOPE = EnderSight.id("textures/misc/ender_spyglass_scope.png");
    private static final Identifier OPEN = EnderSight.id("textures/misc/ender_spyglass_reticle_open.png");
    private static final Identifier CHARGING = EnderSight.id("textures/misc/ender_spyglass_reticle_charging.png");
    private static final Identifier CLOSED = EnderSight.id("textures/misc/ender_spyglass_reticle_closed.png");
    private static final int READY_COLOR = 0xFF7FE8D4;
    private static final int WAITING_COLOR = 0xFFC9B6E4;
    private static final int BLOCKED_COLOR = 0xFFE98AA8;

    private SpyglassOverlay() {
    }

    public static boolean isScoping(@Nullable LocalPlayer player) {
        return player != null && player.isUsingItem() && player.getUseItem().is(ModRegistry.ENDER_SPYGLASS.get());
    }

    //? if >=26.1 {
    public static void draw(GuiGraphicsExtractor graphics) {
    //?} else {
    /*public static void draw(GuiGraphics graphics) {*/
    //?}
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!isScoping(player)) {
            return;
        }

        Identifier reticle;
        Component label;
        int color;
        EnderSpyglassItem.Aim aim = EnderSpyglassItem.aim(player);
        if (aim instanceof EnderSpyglassItem.Aim.Ready ready) {
            int blocks = (int) Math.round(ready.distance());
            if (!EnderSpyglassItem.canAfford(player)) {
                reticle = CLOSED;
                label = Component.translatable("overlay.endersight.ender_spyglass.no_pearl");
                color = BLOCKED_COLOR;
            } else if (player.getTicksUsingItem() < EnderSpyglassItem.MIN_SCOPE_TICKS
                    || player.getCooldowns().isOnCooldown(player.getUseItem())) {
                reticle = CHARGING;
                label = Component.translatable("overlay.endersight.ender_spyglass.distance", blocks);
                color = WAITING_COLOR;
            } else {
                reticle = OPEN;
                label = Component.translatable("overlay.endersight.ender_spyglass.distance", blocks);
                color = READY_COLOR;
            }
        } else if (aim instanceof EnderSpyglassItem.Aim.NoRoom) {
            reticle = CLOSED;
            label = Component.translatable("overlay.endersight.ender_spyglass.no_room");
            color = BLOCKED_COLOR;
        } else {
            reticle = CLOSED;
            label = Component.translatable("overlay.endersight.ender_spyglass.no_target");
            color = WAITING_COLOR;
        }

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        // Above the crosshair, which vanilla draws on top of the scope.
        graphics.blit(RenderPipelines.GUI_TEXTURED, reticle, centerX - 8, centerY - 26, 0.0F, 0.0F, 16, 16, 16, 16);
        Font font = minecraft.font;
        ClientFunctions.renderText(graphics, font, label, centerX - font.width(label) / 2, centerY + 12, color);
    }
}
