package com.iamkaf.endersight.mixin;

import com.iamkaf.endersight.EnderSightClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract EntityType<?> getType();

    @Shadow
    public abstract Level level();

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void endersight$isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (endersight$useEnderSight()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void endersight$getTeamColor(CallbackInfoReturnable<Integer> cir) {
        if (endersight$useEnderSight()) {
            // 11141290 = ChatFormatting.DARK_PURPLE
            cir.setReturnValue(11141290);
        }
    }

    @Unique
    private boolean endersight$useEnderSight() {
        return getType().equals(EntityType.ENDERMAN) && level().isClientSide() && EnderSightClient.ENDER_SIGHT_ENABLED && EnderSightClient.ENDER_SIGHT_OVERRIDE;
    }
}
