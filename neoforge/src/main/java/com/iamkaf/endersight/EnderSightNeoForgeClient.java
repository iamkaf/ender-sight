package com.iamkaf.endersight;

import com.iamkaf.endersight.client.SpyglassOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** NeoForge draws the scope frame from an item extension, so the Ender Spyglass registers its own there. */
@Mod(value = EnderSight.MOD_ID, dist = Dist.CLIENT)
public final class EnderSightNeoForgeClient {
    public EnderSightNeoForgeClient(IEventBus modBus) {
        modBus.addListener(RegisterClientExtensionsEvent.class, event -> event.registerItem(new IClientItemExtensions() {
            @Override
            public Identifier getScopeOverlayTexture(ItemStack stack) {
                return SpyglassOverlay.SCOPE;
            }
        }, ModRegistry.ENDER_SPYGLASS.get()));
    }
}
