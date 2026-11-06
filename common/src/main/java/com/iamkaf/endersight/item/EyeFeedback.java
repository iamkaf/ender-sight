package com.iamkaf.endersight.item;

import com.iamkaf.amber.api.billboard.v1.Billboard;
import com.iamkaf.amber.api.billboard.v1.BillboardAnimation;
import com.iamkaf.amber.api.billboard.v1.Billboards;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

/** What an eye shows instead of text: it learns a target, or fails to find one. */
public final class EyeFeedback {
    private static final float ITEM_SCALE = 0.45F;

    private EyeFeedback() {
    }

    /** A ghost of the eye flies from the target back into the player's hand. */
    public static void imprint(ServerPlayer player, Vec3 target, Item eye) {
        Vec3 hand = hand(player);
        Billboards.show(player, Billboard.item(target, eye, ITEM_SCALE)
                .forTicks(16)
                .translateBy(hand.subtract(target), BillboardAnimation.Easing.EASE_OUT_CUBIC)
                .scaleFromTo(0.6D, 1.0D, BillboardAnimation.Easing.EASE_OUT_CUBIC));
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.PORTAL, target.x, target.y, target.z, 24, 0.3D, 0.3D, 0.3D, 0.6D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** The eye lifts from the hand, looks around, and gives up. */
    public static void lost(ServerPlayer player, Item eye) {
        Billboards.show(player, Billboard.item(hand(player), eye, ITEM_SCALE)
                .forTicks(24)
                .translateBy(0.0D, 0.45D, 0.0D, BillboardAnimation.Easing.EASE_OUT_CUBIC)
                .rotateFromTo(new Vec3(0.0D, 0.0D, -30.0D), Vec3.ZERO, BillboardAnimation.Easing.EASE_OUT_ELASTIC)
                .fadeOut(BillboardAnimation.Easing.EASE_IN_QUAD));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.6F, 1.3F);
    }

    private static Vec3 hand(ServerPlayer player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.7D)).add(0.0D, -0.3D, 0.0D);
    }
}
