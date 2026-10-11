package com.iamkaf.endersight.item;

import com.iamkaf.endersight.ModRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Eaten for the Veiled effect. The effect is applied here rather than as a consume effect because consumable
 * components are built before the effect registry is guaranteed to be populated.
 */
public class EnderVeilItem extends Item {
    public EnderVeilItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            entity.addEffect(new MobEffectInstance(ModRegistry.veiled(), VeiledEffect.DURATION_TICKS));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
