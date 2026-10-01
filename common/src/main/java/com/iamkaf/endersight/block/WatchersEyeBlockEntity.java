package com.iamkaf.endersight.block;

import com.iamkaf.endersight.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Remembers who placed the eye and whether they want alerts. */
public class WatchersEyeBlockEntity extends BlockEntity {
    private @Nullable UUID owner;
    private boolean alerts = true;

    public WatchersEyeBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.WATCHERS_EYE_BLOCK_ENTITY.get(), pos, state);
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public boolean alerts() {
        return alerts;
    }

    public void setAlerts(boolean alerts) {
        this.alerts = alerts;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        alerts = input.getBooleanOr("alerts", true);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("owner", UUIDUtil.CODEC, owner);
        output.putBoolean("alerts", alerts);
    }
}
