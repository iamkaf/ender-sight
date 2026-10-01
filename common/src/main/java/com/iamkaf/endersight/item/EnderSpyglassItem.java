package com.iamkaf.endersight.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
//? if >=26.2 {
/*import net.minecraft.world.entity.EntityTypes;
*///?} else {
import net.minecraft.world.entity.EntityType;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A spyglass that teleports the player to the block face they were scoping when they let go. */
public class EnderSpyglassItem extends SpyglassItem {
    public static final double RANGE = 64.0D;
    /** Brief taps release without teleporting, so a quick look never spends a pearl. */
    public static final int MIN_SCOPE_TICKS = 10;
    public static final int COOLDOWN_TICKS = 40;

    public EnderSpyglassItem(Properties properties) {
        super(properties);
    }

    /** Where letting go would take the player. Shared by the server teleport and the client scope overlay. */
    public sealed interface Aim {
        record Ready(Vec3 destination, double distance) implements Aim {
        }

        record NoTarget() implements Aim {
        }

        record NoRoom() implements Aim {
        }
    }

    public static Aim aim(Player player) {
        HitResult hit = player.pick(RANGE, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return new Aim.NoTarget();
        }
        Vec3 destination = Vec3.atBottomCenterOf(blockHit.getBlockPos().relative(blockHit.getDirection()));
        AABB landing = player.getBoundingBox().move(destination.subtract(player.position()));
        if (!player.level().noCollision(player, landing)) {
            return new Aim.NoRoom();
        }
        return new Aim.Ready(destination, player.position().distanceTo(destination));
    }

    public static boolean canAfford(Player player) {
        return player.getAbilities().instabuild || player.getInventory().contains(stack -> stack.is(Items.ENDER_PEARL));
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTicks) {
        boolean released = super.releaseUsing(stack, level, user, remainingTicks);
        if (user instanceof ServerPlayer player && getUseDuration(stack, user) - remainingTicks >= MIN_SCOPE_TICKS) {
            blink(player, stack);
        }
        return released;
    }

    private static void blink(ServerPlayer player, ItemStack spyglass) {
        if (player.getCooldowns().isOnCooldown(spyglass)
                || !(aim(player) instanceof Aim.Ready ready)
                || !canAfford(player)) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            consumePearl(player.getInventory());
        }

        ServerLevel level = (ServerLevel) player.level();
        Vec3 destination = ready.destination();
        // Same landing rules as a thrown ender pearl.
        if (level.getRandom().nextFloat() < 0.05F && level.isSpawningMonsters()) {
            //? if >=26.2 {
            /*Endermite endermite = EntityTypes.ENDERMITE.create(level, EntitySpawnReason.TRIGGERED);
            *///?} else {
            Endermite endermite = EntityType.ENDERMITE.create(level, EntitySpawnReason.TRIGGERED);
            //?}
            if (endermite != null) {
                endermite.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                level.addFreshEntity(endermite);
            }
        }
        player.teleportTo(destination.x, destination.y, destination.z);
        player.resetFallDistance();
        player.hurtServer(level, player.damageSources().enderPearl(), 5.0F);
        level.playSound(null, destination.x, destination.y, destination.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.getCooldowns().addCooldown(spyglass, COOLDOWN_TICKS);
    }

    private static void consumePearl(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.ENDER_PEARL)) {
                stack.shrink(1);
                return;
            }
        }
    }
}
