package com.iamkaf.endersight.item;

import com.iamkaf.amber.api.billboard.v1.Billboard;
import com.iamkaf.amber.api.billboard.v1.BillboardAnimation;
import com.iamkaf.amber.api.billboard.v1.Billboards;
import com.iamkaf.endersight.EnderSight;
import com.iamkaf.endersight.ModRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** Endermen ignore the gaze of a veiled player, and the player sees each ignored stare blink shut. */
public class VeiledEffect extends MobEffect {
    public static final int DURATION_TICKS = 3600;
    private static final Identifier BLINK_TEXTURE = EnderSight.id("textures/billboard/veil_eye.png");
    private static final int BLINK_TICKS = 16;
    /** Endermen re-check a stare every tick, so each one blinks at most this often. */
    private static final int BLINK_INTERVAL_TICKS = 60;
    private static final Map<Entity, Long> LAST_BLINK = new WeakHashMap<>();

    public VeiledEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B4FB8);
    }

    /** Whether the enderman should ignore this player's stare. Shows the blink when it does. */
    public static boolean hidesGaze(Entity enderman, Player player) {
        if (!player.hasEffect(ModRegistry.veiled())) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            long now = enderman.level().getGameTime();
            Long last = LAST_BLINK.get(enderman);
            if (last == null || now - last >= BLINK_INTERVAL_TICKS) {
                LAST_BLINK.put(enderman, now);
                blink(serverPlayer, enderman);
            }
        }
        return true;
    }

    private static void blink(ServerPlayer viewer, Entity enderman) {
        Billboard eye = Billboard.texture(Vec3.ZERO, BLINK_TEXTURE, 0.7F, 0.7F)
                .boundTo(enderman, new Vec3(0.0D, enderman.getBbHeight() + 0.5D, 0.0D))
                .forTicks(BLINK_TICKS)
                .scaleFromTo(new Vec3(1.0D, 1.0D, 1.0D), new Vec3(1.0D, 0.05D, 1.0D), BillboardAnimation.Easing.EASE_IN_CUBIC);
        Billboards.show(viewer, eye);
    }
}
