package com.iamkaf.endersight.item;

import com.iamkaf.endersight.ModRegistry;
import com.iamkaf.endersight.mixin.EyeOfEnderAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * An Eye of Ender that flies to its attunement, or to the nearest stronghold when it has none. Everlasting eyes always
 * drop back instead of sometimes shattering.
 */
public class SightEyeItem extends Item {
    private static final int LOST_COOLDOWN_TICKS = 20;
    private final boolean everlasting;

    public SightEyeItem(Properties properties, boolean everlasting) {
        super(properties);
        this.everlasting = everlasting;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        ItemStack stack = player.getItemInHand(hand);
        Attunement attunement = stack.get(ModRegistry.ATTUNEMENT.get());
        Optional<Vec3> target;
        if (attunement != null) {
            target = attunement.locate(serverPlayer);
        } else {
            BlockPos stronghold = serverLevel.findNearestMapStructure(
                    StructureTags.EYE_OF_ENDER_LOCATED,
                    player.blockPosition(),
                    100,
                    false
            );
            target = Optional.ofNullable(stronghold).map(Vec3::atLowerCornerOf);
        }
        if (target.isEmpty()) {
            EyeFeedback.lost(serverPlayer, this);
            player.getCooldowns().addCooldown(stack, LOST_COOLDOWN_TICKS);
            return InteractionResult.FAIL;
        }

        EyeOfEnder eye = new EyeOfEnder(level, player.getX(), player.getY(0.5), player.getZ());
        eye.setItem(stack);
        eye.signalTo(target.get());
        if (everlasting) {
            ((EyeOfEnderAccessor) eye).endersight$setSurviveAfterDeath(true);
        }
        level.gameEvent(GameEvent.PROJECTILE_SHOOT, eye.position(), GameEvent.Context.of(player));
        level.addFreshEntity(eye);

        float pitch = Mth.lerp(level.getRandom().nextFloat(), 0.33F, 0.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, pitch);
        stack.consume(1, player);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> builder,
            TooltipFlag flag
    ) {
        Attunement attunement = stack.get(ModRegistry.ATTUNEMENT.get());
        if (attunement != null) {
            builder.accept(attunement.description());
        }
    }
}
