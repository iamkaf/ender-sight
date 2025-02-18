package com.iamkaf.endersight;

import com.iamkaf.amber.api.core.AmberMod;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class EnderSight extends AmberMod {
    public static final String MOD_ID = "endersight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnderSight() {
        super(MOD_ID);
    }

    public static void init() {
        LOGGER.info("Ender Sight initialized...");
    }

    /**
     * Creates resource location in the mod namespace with the given path.
     */
    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
