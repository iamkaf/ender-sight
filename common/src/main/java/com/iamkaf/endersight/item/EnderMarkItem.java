package com.iamkaf.endersight.item;

import com.iamkaf.amber.api.functions.v1.PlayerFunctions;
import com.iamkaf.amber.api.billboard.v1.BillboardAnchor;
import com.iamkaf.endersight.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Marks what the player is looking at for themselves and nearby players who carry Ender Marks. */
public class EnderMarkItem extends Item {
    public static final double RANGE = 64.0D;
    public static final double SHARE_RANGE = 128.0D;
    public static final int BLOCK_DURATION_TICKS = 600;
    /** Mobs and players carry a mark half as long as a place does. */
    public static final int ENTITY_DURATION_TICKS = 300;
    public static final int COOLDOWN_TICKS = 100;

    public EnderMarkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getViewVector(1.0F).scale(RANGE);
        BlockHitResult blockHit = level.clip(new ClipContext(eye, eye.add(reach), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? eye.add(reach) : blockHit.getLocation();
        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eye, end, searchBox, entity -> !entity.isSpectator() && entity.isPickable(), eye.distanceToSqr(end)
        );

        Component label = player.getName();
        Vec3 markPosition;
        Entity markedEntity = null;
        if (entityHit != null) {
            markedEntity = entityHit.getEntity();
            markPosition = markedEntity.position();
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            markPosition = Vec3.atBottomCenterOf(blockHit.getBlockPos()).add(0.0D, 1.6D, 0.0D);
        } else {
            PlayerFunctions.sendActionBarMessage(player, Component.translatable("message.endersight.ender_mark.no_target"));
            return InteractionResult.FAIL;
        }

        int duration = markedEntity != null ? ENTITY_DURATION_TICKS : BLOCK_DURATION_TICKS;
        BillboardAnchor anchor = markedEntity != null ? Marks.above(markedEntity) : BillboardAnchor.world(markPosition);
        for (ServerPlayer viewer : serverLevel.players()) {
            if (viewer != serverPlayer && (viewer.distanceToSqr(markPosition) > SHARE_RANGE * SHARE_RANGE || !carriesMarks(viewer))) {
                continue;
            }
            Marks.show(viewer, anchor, duration, label, 0, true);
        }
        if (markedEntity instanceof ServerPlayer target) {
            Marks.spotted(target, player);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean carriesMarks(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModRegistry.ENDER_MARK.get()));
    }
}
