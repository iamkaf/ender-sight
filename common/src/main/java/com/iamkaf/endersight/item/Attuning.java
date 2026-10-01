package com.iamkaf.endersight.item;

import com.iamkaf.amber.api.event.v1.events.common.BlockEvents;
import com.iamkaf.amber.api.event.v1.events.common.PlayerEvents;
import com.iamkaf.endersight.ModRegistry;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Sneak-using an eye on a block or player binds one eye from the stack to it. */
public final class Attuning {
    private Attuning() {
    }

    public static void init() {
        BlockEvents.BLOCK_INTERACT.register((player, level, hand, hit) -> {
            if (!canAttune(player, hand) || level.getBlockState(hit.getBlockPos()).is(Blocks.END_PORTAL_FRAME)) {
                return InteractionResult.PASS;
            }
            Vec3 target = Vec3.atCenterOf(hit.getBlockPos()).add(0.0D, 0.7D, 0.0D);
            return attune(player, level, hand, Attunement.of(GlobalPos.of(level.dimension(), hit.getBlockPos())), target);
        });
        PlayerEvents.ENTITY_INTERACT.register((player, level, hand, entity) -> {
            if (!canAttune(player, hand) || !(entity instanceof Player target)) {
                return InteractionResult.PASS;
            }
            return attune(player, level, hand, Attunement.of(target), target.getEyePosition());
        });
    }

    private static boolean canAttune(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return player.isShiftKeyDown() && (stack.is(Items.ENDER_EYE)
                || stack.is(ModRegistry.ATTUNED_EYE.get())
                || stack.is(ModRegistry.EVERLASTING_EYE.get()));
    }

    private static InteractionResult attune(Player player, Level level, InteractionHand hand, Attunement attunement, Vec3 target) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);
        // Plain eyes become Attuned Eyes; Attuned and Everlasting Eyes keep their kind.
        ItemStack attuned = held.is(Items.ENDER_EYE) ? new ItemStack(ModRegistry.ATTUNED_EYE.get()) : held.copyWithCount(1);
        attuned.set(ModRegistry.ATTUNEMENT.get(), attunement);
        // Read before adding: Inventory.add empties the stack it was given.
        Item eye = attuned.getItem();
        held.consume(1, player);
        if (held.isEmpty()) {
            player.setItemInHand(hand, attuned);
        } else if (!player.getInventory().add(attuned)) {
            level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), attuned));
        }

        EyeFeedback.imprint(serverPlayer, target, eye);
        return InteractionResult.SUCCESS;
    }
}
