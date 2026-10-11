package com.iamkaf.endersight.mixin;

import com.iamkaf.endersight.client.SpyglassOverlay;
//? if >=26.2 {
/*import net.minecraft.client.gui.Hud;
*///?} else {
import net.minecraft.client.gui.Gui;
//?}
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the reticle over the scope while the Ender Spyglass is in use. */
//? if >=26.2 {
/*@Mixin(Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
public abstract class SpyglassOverlayMixin {
    //? if >=26.1 {
    @Inject(method = "extractSpyglassOverlay", at = @At("TAIL"))
    private void endersight$drawReticle(GuiGraphicsExtractor graphics, float scale, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "renderSpyglassOverlay", at = @At("TAIL"))
    private void endersight$drawReticle(GuiGraphics graphics, float scale, CallbackInfo ci) {*/
    //?}
        SpyglassOverlay.draw(graphics);
    }
}
