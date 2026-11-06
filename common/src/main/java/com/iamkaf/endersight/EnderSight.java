package com.iamkaf.endersight;

import com.iamkaf.amber.api.core.v2.AmberInitializer;
import com.iamkaf.endersight.item.Attuning;
import com.iamkaf.endersight.item.Marks;
import net.minecraft.resources.Identifier;

public final class EnderSight {
    public static final String MOD_ID = "endersight";

    private EnderSight() {
    }

    public static void init() {
        AmberInitializer.initialize(MOD_ID);
        ModRegistry.init();
        Attuning.init();
        Marks.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
