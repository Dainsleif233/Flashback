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

    // Make overlay disappear instantly if inside replay
    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (this.fadeOutStart != -1 && Flashback.isInReplay()) {
            this.minecraft.gui.setOverlay(null);
        }
    }

    // 26.3: force-dismiss loading overlay once a ReplayServer exists so UI can activate
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    public void tick(CallbackInfo ci) {
        if (Flashback.getReplayServer() != null || Flashback.isInReplay()) {
            if (this.minecraft.getSingleplayerServer() instanceof com.moulberry.flashback.playback.ReplayServer) {
                if (this.minecraft.level != null && this.minecraft.player != null) {
                    this.minecraft.gui.setOverlay(null);
                }
            }
        }
    }


}
