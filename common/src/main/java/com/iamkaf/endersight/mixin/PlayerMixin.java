package com.iamkaf.endersight.mixin;

import com.iamkaf.endersight.ModRegistry;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla only zooms and draws the scope overlay for the vanilla spyglass. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "isScoping", at = @At("RETURN"), cancellable = true)
    private void endersight$scopeWithEnderSpyglass(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!cir.getReturnValueZ() && player.isUsingItem() && player.getUseItem().is(ModRegistry.ENDER_SPYGLASS.get())) {
            cir.setReturnValue(true);
        }
    }
}
