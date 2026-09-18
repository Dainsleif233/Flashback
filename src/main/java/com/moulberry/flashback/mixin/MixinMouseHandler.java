package com.moulberry.flashback.mixin;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.ReplayUI;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    /**
     * Replay editor: cursor free by default; grab only while left-click is held
     * on the spectator viewport. Feed Minecraft/SDL mouse events into ImGui.
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
        if (ReplayUI.isActive()) {
            ReplayUI.feedMouseMove((float) x, (float) y);
        }
    }

    @Inject(method = "onButton", at = @At("HEAD"))
    public void onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (!ReplayUI.isActive() || info == null) {
            return;
        }
        // GLFW-style: 1 = press, 0 = release
        boolean down = action != 0;
        ReplayUI.feedMouseButton(info.button(), down);
    }

    @Inject(method = "onScroll", at = @At("HEAD"))
    public void onScroll(long window, double x, double y, CallbackInfo ci) {
        if (ReplayUI.isActive()) {
            ReplayUI.feedMouseWheel((float) x, (float) y);
        }
    }

    @Inject(method = {"onButton", "onScroll", "onMove"}, at = @At("HEAD"), cancellable = true)
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
