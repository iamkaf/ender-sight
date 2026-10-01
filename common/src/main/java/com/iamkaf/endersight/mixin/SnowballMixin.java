package com.iamkaf.endersight.mixin;

import com.iamkaf.endersight.item.SeersPearlItem;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Seer's Pearls fly as snowballs carrying the pearl item, so the vanilla projectile and renderer do the work. */
@Mixin(Snowball.class)
public abstract class SnowballMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void endersight$seersPearlImpact(HitResult hit, CallbackInfo ci) {
        SeersPearlItem.onImpact((Snowball) (Object) this, hit);
    }
}
