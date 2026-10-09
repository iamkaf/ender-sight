package com.iamkaf.endersight.mixin;

import com.iamkaf.endersight.client.SpyglassOverlay;
import net.minecraft.client.Minecraft;
//? if >=26.2 {
/*import net.minecraft.client.gui.Hud;
*///?} else {
import net.minecraft.client.gui.Gui;
//?}
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Swaps the scope frame while the Ender Spyglass is in use. Fabric and Forge only: NeoForge reads the frame from an item
 * extension instead of this field.
 */
//? if >=26.2 {
/*@Mixin(Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
public abstract class SpyglassScopeTextureMixin {
    @Shadow
    @Final
    private static Identifier SPYGLASS_SCOPE_LOCATION;

    /** ASM's GETSTATIC opcode; ASM itself isn't on the common compile classpath. */
    private static final int GETSTATIC = 178;

    //? if >=26.2 {
    /*@Redirect(method = "extractSpyglassOverlay", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Hud;SPYGLASS_SCOPE_LOCATION:Lnet/minecraft/resources/Identifier;", opcode = GETSTATIC))
    *///?} else if >=26.1 {
    @Redirect(method = "extractSpyglassOverlay", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;SPYGLASS_SCOPE_LOCATION:Lnet/minecraft/resources/Identifier;", opcode = GETSTATIC))
    //?} else {
    /*@Redirect(method = "renderSpyglassOverlay", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;SPYGLASS_SCOPE_LOCATION:Lnet/minecraft/resources/Identifier;", opcode = GETSTATIC))
    *///?}
    private Identifier endersight$scopeTexture() {
        return SpyglassOverlay.isScoping(Minecraft.getInstance().player) ? SpyglassOverlay.SCOPE : SPYGLASS_SCOPE_LOCATION;
    }
}
