package com.iamkaf.endersight;

import net.fabricmc.api.ModInitializer;

public final class EnderSightFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        EnderSight.init();
    }
}
