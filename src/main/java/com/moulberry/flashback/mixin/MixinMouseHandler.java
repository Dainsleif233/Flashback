package com.moulberry.flashback.mixin;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.ReplayUI;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    /**
     * Replay editor: cursor is free by default; grab only while left-click
     * is held on the spectator viewport (26.3 uses SDL — not GLFW).
     */
    @Inject(method = "isMouseGrabbed", at=@At("HEAD"), cancellable = true)
    public void isMouseGrabbed(CallbackInfoReturnable<Boolean> cir) {
        if (ReplayUI.isActive()) {
            cir.setReturnValue(ReplayUI.wantsCameraGrab());
        } else if (Flashback.isExporting()) {
            cir.setReturnValue(false);
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
        // In replay, ignore vanilla grab unless camera capture is wanted
        if (ReplayUI.isActive() && !ReplayUI.wantsCameraGrab()) {
            ci.cancel();
        }
    }

    @Inject(method = "releaseMouse", at=@At("HEAD"), cancellable = true)
    public void releaseMouse(CallbackInfo ci) {
        // Always allow release in replay so the editor cursor can appear
        if (Flashback.isExporting()) {
            ci.cancel();
        }
    }

}
