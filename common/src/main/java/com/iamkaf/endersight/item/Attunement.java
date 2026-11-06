package com.iamkaf.endersight.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * What an eye was attuned to: a block position or a player. Exactly one of {@code place} and {@code player} is set;
 * use the factory methods.
 */
public record Attunement(Optional<GlobalPos> place, Optional<UUID> player, String playerName) {
    public static final Codec<Attunement> CODEC = RecordCodecBuilder.<Attunement>create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("place").forGetter(Attunement::place),
            UUIDUtil.CODEC.optionalFieldOf("player").forGetter(Attunement::player),
            Codec.STRING.optionalFieldOf("player_name", "").forGetter(Attunement::playerName)
    ).apply(instance, Attunement::new)).validate(attunement -> attunement.place().isPresent() != attunement.player().isPresent()
            ? DataResult.success(attunement)
            : DataResult.error(() -> "Attunement needs exactly one of place and player"));

    public static final StreamCodec<ByteBuf, Attunement> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), Attunement::place,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Attunement::player,
            ByteBufCodecs.STRING_UTF8, Attunement::playerName,
            Attunement::new
    );

    public static Attunement of(GlobalPos place) {
        return new Attunement(Optional.of(place), Optional.empty(), "");
    }

    public static Attunement of(Player player) {
        return new Attunement(Optional.empty(), Optional.of(player.getUUID()), player.getName().getString());
    }

    /** Where a thrown eye should fly, if its target is online and in the thrower's dimension. */
    public Optional<Vec3> locate(ServerPlayer thrower) {
        if (player.isPresent()) {
            ServerPlayer target = thrower.level().getServer().getPlayerList().getPlayer(player.get());
            return target != null && target.level() == thrower.level() ? Optional.of(target.getEyePosition()) : Optional.empty();
        }
        GlobalPos pos = place.orElseThrow();
        return pos.dimension() == thrower.level().dimension() ? Optional.of(Vec3.atCenterOf(pos.pos())) : Optional.empty();
    }

    public Component description() {
        if (player.isPresent()) {
            return Component.translatable("tooltip.endersight.attuned.player", playerName);
        }
        GlobalPos pos = place.orElseThrow();
        return Component.translatable(
                "tooltip.endersight.attuned.place",
                pos.pos().getX(),
                pos.pos().getY(),
                pos.pos().getZ(),
                pos.dimension().identifier().toString()
        );
    }
}
