package com.iamkaf.endersight.neoforge;

import com.iamkaf.endersight.EnderSight;
import net.neoforged.fml.common.Mod;

@Mod(EnderSight.MOD_ID)
public final class EnderSightNeoForge {
    public EnderSightNeoForge() {
        // Run our common setup.
        EnderSight.init();
    }
}
