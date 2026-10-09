package com.iamkaf.endersight.mixin;

import com.iamkaf.endersight.item.VeiledEffect;
//? if >=26.3 {
/*import net.minecraft.world.entity.monster.Enderman;
*///?} else {
import net.minecraft.world.entity.monster.EnderMan;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=26.3 {
/*@Mixin(Enderman.class)
*///?} else {
@Mixin(EnderMan.class)
//?}
public abstract class EndermanMixin {
    @Inject(method = "isBeingStaredBy", at = @At("RETURN"), cancellable = true)
    private void endersight$veiledGaze(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && VeiledEffect.hidesGaze((Entity) (Object) this, player)) {
            cir.setReturnValue(false);
        }
    }
}
