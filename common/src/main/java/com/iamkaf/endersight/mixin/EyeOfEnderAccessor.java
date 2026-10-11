package com.iamkaf.endersight.mixin;

import net.minecraft.world.entity.projectile.EyeOfEnder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EyeOfEnder.class)
public interface EyeOfEnderAccessor {
    @Accessor("surviveAfterDeath")
    void endersight$setSurviveAfterDeath(boolean survive);
}
