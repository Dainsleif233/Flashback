package com.moulberry.flashback.mixin.visuals;

import com.moulberry.flashback.Flashback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public class MixinLoadingOverlay {

    @Shadow private long fadeOutStart;

    @Shadow @Final private Minecraft minecraft;

    // Dismiss only when the replay client already has a world
    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if ((Flashback.isInReplay() || Flashback.getReplayServer() != null) && this.minecraft.level != null) {
            this.minecraft.gui.setOverlay(null);
        }
    }


}
