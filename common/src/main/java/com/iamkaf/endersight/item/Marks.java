package com.iamkaf.endersight.item;

import com.iamkaf.amber.api.billboard.v1.Billboard;
import com.iamkaf.amber.api.billboard.v1.BillboardAnchor;
import com.iamkaf.amber.api.billboard.v1.BillboardAnimation.Easing;
import com.iamkaf.amber.api.billboard.v1.Billboards;
import com.iamkaf.amber.api.event.v1.events.common.ServerTickEvents;
import com.iamkaf.amber.api.functions.v1.PlayerFunctions;
import com.iamkaf.endersight.EnderSight;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Through-wall markers shared by Ender Marks and Seer's Pearls. A mark arrives in two phases under one identity: it
 * drops onto its target with a ring ping, then holds steady until it fades at the end of its life.
 */
public final class Marks {
    private static final Identifier MARKER = EnderSight.id("textures/billboard/ender_mark.png");
    private static final Identifier RING = EnderSight.id("textures/billboard/mark_ring.png");
    private static final Identifier WATCHER = EnderSight.id("textures/billboard/veil_eye.png");
    private static final int SPOTTED_TICKS = 60;
    private static final float SIZE = 1.0F;
    private static final int ARRIVAL_TICKS = 12;
    private static final double DROP = 1.2D;
    private static final double LABEL_DROP = 0.7D;
    private static final double ENTITY_CLEARANCE = 0.8D;
    /** Marks shown later, such as the hold phase after an arrival. Only touched on the server thread. */
    private static final List<Scheduled> SCHEDULED = new ArrayList<>();

    private Marks() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(Marks::tick);
    }

    /** The point above an entity's head where its mark sits. */
    public static BillboardAnchor above(Entity entity) {
        return BillboardAnchor.entity(entity, new Vec3(0.0D, entity.getBbHeight() + ENTITY_CLEARANCE, 0.0D));
    }

    /**
     * Shows a mark to one viewer after {@code delayTicks}. The label, when present, hangs under the marker once it
     * has landed. {@code ping} adds a ring as the mark lands; leave it off when a larger ping already leads.
     */
    public static void show(
            ServerPlayer viewer,
            BillboardAnchor anchor,
            int ticks,
            @Nullable Component label,
            int delayTicks,
            boolean ping
    ) {
        schedule(viewer, delayTicks, () -> {
            UUID id = UUID.randomUUID();
            Billboards.show(viewer, marker(anchor, id, ARRIVAL_TICKS)
                    .translateFromTo(new Vec3(0.0D, DROP, 0.0D), Vec3.ZERO, Easing.EASE_OUT_BACK)
                    .scaleFromTo(0.6D, 1.0D, Easing.EASE_OUT_BACK)
                    .fadeIn(Easing.EASE_OUT_CUBIC));
            if (ping) {
                ping(viewer, anchor, 2.2D);
            }
            schedule(viewer, ARRIVAL_TICKS, () -> {
                int hold = ticks - ARRIVAL_TICKS;
                // The ease-in fade keeps the mark solid for most of its life and lets it go at the end.
                Billboards.show(viewer, marker(anchor, id, hold).fadeOut(Easing.EASE_IN_CUBIC));
                if (label != null) {
                    Billboards.show(viewer, Billboard.text(Vec3.ZERO, label, 0.02F, 0xFFE0C8FF)
                            .anchoredTo(lowered(anchor, LABEL_DROP))
                            .visibleThroughWalls()
                            .forTicks(hold)
                            .fadeOut(Easing.EASE_IN_CUBIC));
                }
            });
        });
    }

    /**
     * Tells a marked player who is watching: an eye blinks open over the marker's head, visible through walls, with an
     * Enderman's stare.
     */
    public static void spotted(ServerPlayer target, Entity watcher) {
        Billboards.show(target, Billboard.texture(Vec3.ZERO, WATCHER, 0.8F, 0.8F)
                .anchoredTo(above(watcher))
                .visibleThroughWalls()
                .forTicks(SPOTTED_TICKS)
                .scaleFromTo(new Vec3(1.0D, 0.05D, 1.0D), new Vec3(1.0D, 1.0D, 1.0D), Easing.EASE_OUT_BACK)
                .fadeOut(Easing.EASE_IN_CUBIC));
        PlayerFunctions.playSound(target, SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    /** A ring that ripples out from a point, as a mark lands or a scan begins. */
    public static void ping(ServerPlayer viewer, BillboardAnchor anchor, double reach) {
        Billboards.show(viewer, Billboard.texture(Vec3.ZERO, RING, SIZE, SIZE)
                .anchoredTo(anchor)
                .visibleThroughWalls()
                .forTicks(14)
                .scaleFromTo(0.3D, reach, Easing.EASE_OUT_CUBIC)
                .fadeOut(Easing.EASE_IN_QUAD));
    }

    private static Billboard marker(BillboardAnchor anchor, UUID id, int ticks) {
        return Billboard.texture(Vec3.ZERO, MARKER, SIZE, SIZE)
                .anchoredTo(anchor)
                .identifiedBy(id)
                .visibleThroughWalls()
                .forTicks(ticks);
    }

    private static BillboardAnchor lowered(BillboardAnchor anchor, double by) {
        Vec3 down = new Vec3(0.0D, -by, 0.0D);
        return switch (anchor) {
            case BillboardAnchor.World world -> BillboardAnchor.world(world.position().add(down));
            case BillboardAnchor.Entity entity -> new BillboardAnchor.Entity(entity.entityId(), entity.offset().add(down));
        };
    }

    private static void schedule(ServerPlayer viewer, int delayTicks, Runnable action) {
        if (delayTicks <= 0) {
            action.run();
        } else {
            SCHEDULED.add(new Scheduled(viewer, delayTicks, action));
        }
    }

    private static void tick() {
        if (SCHEDULED.isEmpty()) {
            return;
        }
        List<Runnable> due = new ArrayList<>();
        SCHEDULED.removeIf(scheduled -> {
            // A viewer who left, or whose server stopped, no longer needs the mark.
            if (scheduled.viewer.hasDisconnected()) {
                return true;
            }
            if (--scheduled.ticksLeft > 0) {
                return false;
            }
            due.add(scheduled.action);
            return true;
        });
        // Run after the sweep: actions may schedule follow-ups.
        due.forEach(Runnable::run);
    }

    private static final class Scheduled {
        private final ServerPlayer viewer;
        private int ticksLeft;
        private final Runnable action;

        private Scheduled(ServerPlayer viewer, int ticksLeft, Runnable action) {
            this.viewer = viewer;
            this.ticksLeft = ticksLeft;
            this.action = action;
        }
    }
}
