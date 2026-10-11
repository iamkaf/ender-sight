package com.iamkaf.endersight.block;

import com.iamkaf.amber.api.functions.v1.PlayerFunctions;
//? if <26.3
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * Watches along its facing and emits redstone while it sees something its mode accepts: 15 when the nearest match is
 * adjacent, fading by one per block to 1 at the edge of its range.
 */
public class WatchersEyeBlock extends DirectionalBlock implements EntityBlock {
    //? if <26.3
    public static final MapCodec<WatchersEyeBlock> CODEC = simpleCodec(WatchersEyeBlock::new);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final EnumProperty<WatchMode> MODE = EnumProperty.create("mode", WatchMode.class);
    public static final int RANGE = 16;
    private static final double VIEW_RADIUS = 2.5D;
    private static final int SCAN_INTERVAL_TICKS = 4;

    public WatchersEyeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWER, 0)
                .setValue(MODE, WatchMode.PLAYERS));
    }

    //? if <26.3 {
    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }
    //?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER, MODE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Like a dispenser, the eye looks back at whoever placed it.
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof WatchersEyeBlockEntity eye) {
            eye.setOwner(player.getUUID());
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, SCAN_INTERVAL_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, SCAN_INTERVAL_TICKS);
        Sighting sighting = look(state, level, pos);
        int power = sighting == null ? 0 : sighting.power();
        if (power == state.getValue(POWER)) {
            return;
        }
        level.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_ALL);
        if (state.getValue(POWER) == 0 && sighting != null) {
            alertOwner(level, pos, sighting);
        }
    }

    private static @Nullable Sighting look(BlockState state, ServerLevel level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        Vec3 forward = Vec3.atLowerCornerOf(facing.getUnitVec3i());
        Vec3 origin = Vec3.atCenterOf(pos).add(forward.scale(0.51D));
        AABB view = new AABB(origin, origin.add(forward.scale(RANGE))).inflate(VIEW_RADIUS);
        WatchMode mode = state.getValue(MODE);

        Sighting nearest = null;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, view, mode::matches)) {
            Vec3 center = entity.getBoundingBox().getCenter();
            Vec3 offset = center.subtract(origin);
            double along = offset.dot(forward);
            if (along < 0.0D || along > RANGE || offset.subtract(forward.scale(along)).length() > VIEW_RADIUS) {
                continue;
            }
            if (nearest != null && along >= nearest.distance()) {
                continue;
            }
            HitResult blocked = level.clip(new ClipContext(origin, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (blocked.getType() == HitResult.Type.MISS) {
                nearest = new Sighting(entity, along);
            }
        }
        return nearest;
    }

    private static void alertOwner(ServerLevel level, BlockPos pos, Sighting sighting) {
        if (!(level.getBlockEntity(pos) instanceof WatchersEyeBlockEntity eye) || !eye.alerts() || eye.owner() == null) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(eye.owner());
        if (owner == null) {
            return;
        }
        PlayerFunctions.sendActionBarMessage(owner, Component.translatable(
                "message.endersight.watchers_eye.alert",
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                sighting.entity().getDisplayName()
        ));
        PlayerFunctions.playSound(owner, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            toggleAlerts(level, pos, player);
        } else {
            WatchMode mode = state.getValue(MODE).next();
            level.setBlock(pos, state.setValue(MODE, mode), Block.UPDATE_ALL);
            PlayerFunctions.sendActionBarMessage(player, Component.translatable("message.endersight.watchers_eye.mode." + mode.getSerializedName()));
        }
        level.playSound(null, pos, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 0.5F, 1.8F);
        return InteractionResult.SUCCESS;
    }

    private static void toggleAlerts(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof WatchersEyeBlockEntity eye)) {
            return;
        }
        if (eye.owner() == null) {
            eye.setOwner(player.getUUID());
        } else if (!eye.owner().equals(player.getUUID())) {
            PlayerFunctions.sendActionBarMessage(player, Component.translatable("message.endersight.watchers_eye.not_owner"));
            return;
        }
        eye.setAlerts(!eye.alerts());
        PlayerFunctions.sendActionBarMessage(player, Component.translatable(
                eye.alerts() ? "message.endersight.watchers_eye.alerts_on" : "message.endersight.watchers_eye.alerts_off"
        ));
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WatchersEyeBlockEntity(pos, state);
    }

    private record Sighting(LivingEntity entity, double distance) {
        int power() {
            return Math.max(1, 15 - (int) Math.floor(Math.max(0.0D, distance - 0.5D)));
        }
    }
}
