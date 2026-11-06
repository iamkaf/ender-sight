package com.iamkaf.endersight.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** What a Watcher's Eye reacts to. Cycled by using the block. */
public enum WatchMode implements StringRepresentable {
    PLAYERS("players"),
    HOSTILE("hostile"),
    ALL("all");

    private final String name;

    WatchMode(String name) {
        this.name = name;
    }

    public boolean matches(LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        return switch (this) {
            case PLAYERS -> entity instanceof Player;
            case HOSTILE -> entity instanceof Enemy;
            case ALL -> !(entity instanceof ArmorStand);
        };
    }

    public WatchMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
