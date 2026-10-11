package com.iamkaf.endersight.item;

import com.iamkaf.amber.api.billboard.v1.BillboardAnchor;
import com.iamkaf.endersight.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Thrown like a snowball; where it lands, every living thing nearby is marked for the thrower. */
public class SeersPearlItem extends SnowballItem {
    public static final double RADIUS = 8.0D;
    public static final int DURATION_TICKS = 200;
    private static final double TICKS_PER_BLOCK = 1.5D;

    public SeersPearlItem(Properties properties) {
        super(properties);
    }

    /** Called when any snowball lands; only Seer's Pearls react. */
    public static void onImpact(Snowball snowball, HitResult hit) {
        if (!(snowball.level() instanceof ServerLevel level)
                || !snowball.getItem().is(ModRegistry.SEERS_PEARL.get())
                || !(snowball.getOwner() instanceof ServerPlayer thrower)) {
            return;
        }

        Vec3 center = hit.getLocation();
        Marks.ping(thrower, BillboardAnchor.world(center), 5.0D);
        AABB area = new AABB(center, center).inflate(RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != thrower
                && entity.isAlive()
                && !entity.isSpectator()
                && entity.distanceToSqr(center) <= RADIUS * RADIUS)) {
            // Marks land as a wave spreading out from the impact.
            int delay = (int) Math.round(Math.sqrt(entity.distanceToSqr(center)) * TICKS_PER_BLOCK);
            Marks.show(thrower, Marks.above(entity), DURATION_TICKS, null, delay, false);
            if (entity instanceof ServerPlayer target) {
                Marks.spotted(target, thrower);
            }
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
