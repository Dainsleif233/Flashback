package com.moulberry.flashback.mixin;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.ReplayUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Shadow
    private boolean mouseGrabbed;

    /**
     * Replay editor: free cursor by default; camera only on viewport left-hold.
     * SDL window handle often does not match event window id, so vanilla onMove
     * early-returns — we feed ImGui and drive camera ourselves.
     */
    @Inject(method = "isMouseGrabbed", at=@At("HEAD"), cancellable = true)
    public void isMouseGrabbed(CallbackInfoReturnable<Boolean> cir) {
        if (ReplayUI.isActive()) {
            cir.setReturnValue(ReplayUI.wantsCameraGrab());
        } else if (Flashback.isExporting()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    public void onMove(long window, double x, double y, double z, double w, CallbackInfo ci) {
        if (!ReplayUI.isActive()) {
            return;
        }
        ReplayUI.feedMouseMove((float) x, (float) y);

        double dx = x - this.xpos;
        double dy = y - this.ypos;
        this.xpos = x;
        this.ypos = y;

        // Camera: turn like vanilla when left-hold is on the spectator viewport
        if (ReplayUI.wantsCameraGrab() && this.minecraft.player != null) {
            float sensitivity = (float) (this.minecraft.options.getMouseSensitivity() * 0.6F + 0.2F);
            double turnX = dx * sensitivity * sensitivity * sensitivity * 8.0;
            double turnY = dy * sensitivity * sensitivity * sensitivity * 8.0;
            this.minecraft.player.turn(turnX, turnY);
        }
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    public void onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (!ReplayUI.isActive() || info == null) {
            return;
        }
        boolean down = action != 0;
        ReplayUI.feedMouseButton(info.button(), down);

        // Let ImGui panels receive clicks; do not forward to the game
        // unless the spectator viewport wants camera grab.
        if (!ReplayUI.wantsCameraGrab() && ReplayUI.getIO() != null && ReplayUI.getIO().getWantCaptureMouse()) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    public void onScroll(long window, double x, double y, CallbackInfo ci) {
        if (!ReplayUI.isActive()) {
            return;
        }
        ReplayUI.feedMouseWheel((float) x, (float) y);
        if (!ReplayUI.wantsCameraGrab() && ReplayUI.getIO() != null && ReplayUI.getIO().getWantCaptureMouse()) {
            ci.cancel();
        }
    }

    @Inject(method = {"onButton", "onScroll"}, at = @At("HEAD"), cancellable = true)
    public void onUseMouse(CallbackInfo ci) {
        if (Flashback.isExporting()) {
            ci.cancel();
        }
    }

    @Inject(method = "grabMouse", at=@At("HEAD"), cancellable = true)
    public void grabMouse(CallbackInfo ci) {
        if (Flashback.isExporting()) {
            ci.cancel();
            return;
        }
        if (ReplayUI.isActive() && !ReplayUI.wantsCameraGrab()) {
            ci.cancel();
        }
    }

    @Inject(method = "releaseMouse", at=@At("HEAD"), cancellable = true)
    public void releaseMouse(CallbackInfo ci) {
        if (Flashback.isExporting()) {
            ci.cancel();
        }
    }

}
