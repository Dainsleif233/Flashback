package com.moulberry.flashback.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.ext.WindowExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class MixinWindow implements WindowExt {

    @Shadow private int framebufferWidth;
    @Shadow private int framebufferHeight;

    @Shadow private int width;
    @Shadow private int height;

    @Shadow private int guiScale;

    @Shadow private int guiScaledWidth;

    @Shadow private int guiScaledHeight;

    @Shadow
    private int windowedWidth;

    @Shadow
    @Final
    private long handle;

    @Shadow
    public abstract int getWidth();

    @Shadow
    public abstract int getHeight();

    @Shadow
    @Final
    private WindowEventHandler eventHandler;

    @Unique
    private int overrideFramebufferWidth = -1;
    @Unique
    private int overrideFramebufferHeight = -1;
    /** True SDL/window metrics captured before any Flashback override. */
    @Unique
    private int realFramebufferWidth = -1;
    @Unique
    private int realFramebufferHeight = -1;
    @Unique
    private int realWindowWidth = -1;
    @Unique
    private int realWindowHeight = -1;

    @Unique
    private float calculateWidthScaleFactor() {
        int realFb = this.realFramebufferWidth > 0 ? this.realFramebufferWidth : this.framebufferWidth;
        int realW = this.realWindowWidth > 0 ? this.realWindowWidth : this.width;
        if (realW <= 0) {
            return 1f;
        }
        return Math.max(1 / 8f, Math.min(8f, (float) realFb / realW));
    }

    @Unique
    private float calculateHeightScaleFactor() {
        int realFb = this.realFramebufferHeight > 0 ? this.realFramebufferHeight : this.framebufferHeight;
        int realH = this.realWindowHeight > 0 ? this.realWindowHeight : this.height;
        if (realH <= 0) {
            return 1f;
        }
        return Math.max(1 / 8f, Math.min(8f, (float) realFb / realH));
    }

    @Override
    public void flashback$updateScaledFramebuffer(boolean callFramebufferSizeChanged) {
        // Capture true window metrics while no override is active so scale factors stay stable
        if (this.overrideFramebufferWidth == -1 && this.framebufferWidth > 0 && this.width > 0) {
            this.realFramebufferWidth = this.framebufferWidth;
            this.realFramebufferHeight = this.framebufferHeight;
            this.realWindowWidth = this.width;
            this.realWindowHeight = this.height;
        }

        int lastWidth = this.overrideFramebufferWidth;
        int lastHeight = this.overrideFramebufferHeight;

        int newWidth = -1;
        int newHeight = -1;
        if (Flashback.EXPORT_JOB != null && Flashback.EXPORT_JOB.shouldChangeFramebufferSize()) {
            newWidth = Flashback.EXPORT_JOB.getWidth();
            newHeight = Flashback.EXPORT_JOB.getHeight();
        } else if (ReplayUI.shouldModifyViewport()) {
            newWidth = ReplayUI.getNewGameWidth(this.calculateWidthScaleFactor());
            newHeight = ReplayUI.getNewGameHeight(this.calculateHeightScaleFactor());
        }

        // Ignore tiny dock-layout jitter to avoid per-frame framebuffer thrash
        if (lastWidth > 0 && newWidth > 0
                && Math.abs(newWidth - lastWidth) <= 2
                && Math.abs(newHeight - lastHeight) <= 2) {
            newWidth = lastWidth;
            newHeight = lastHeight;
        }

        this.overrideFramebufferWidth = newWidth;
        this.overrideFramebufferHeight = newHeight;

        boolean sizeChanged = lastWidth != this.overrideFramebufferWidth || lastHeight != this.overrideFramebufferHeight;

        if (newWidth > 0 && newHeight > 0) {
            this.framebufferWidth = newWidth;
            this.framebufferHeight = newHeight;
        } else if (this.realFramebufferWidth > 0 && this.realFramebufferHeight > 0) {
            // Restore real size when override is cleared
            if (this.framebufferWidth != this.realFramebufferWidth || this.framebufferHeight != this.realFramebufferHeight) {
                this.framebufferWidth = this.realFramebufferWidth;
                this.framebufferHeight = this.realFramebufferHeight;
                sizeChanged = true;
            }
        }

        if (callFramebufferSizeChanged && sizeChanged) {
            this.eventHandler.framebufferSizeChanged();
        }
    }

    @Inject(method = "getWidth", at=@At("HEAD"), cancellable = true)
    public void getWidth(CallbackInfoReturnable<Integer> cir) {
        int width = this.overrideFramebufferWidth;
        if (width != -1) {
            cir.setReturnValue(width);
        }
    }

    @Inject(method = "getHeight", at=@At("HEAD"), cancellable = true)
    public void getHeight(CallbackInfoReturnable<Integer> cir) {
        int height = this.overrideFramebufferHeight;
        if (height != -1) {
            cir.setReturnValue(height);
        }
    }

    @Inject(method = "getScreenWidth", at=@At("HEAD"), cancellable = true)
    public void getScreenWidth(CallbackInfoReturnable<Integer> cir) {
        if (ReplayUI.shouldModifyViewport()) {
            cir.setReturnValue(ReplayUI.getNewGameWidth(1));
        }
    }

    @Inject(method = "getScreenHeight", at=@At("HEAD"), cancellable = true)
    public void getScreenHeight(CallbackInfoReturnable<Integer> cir) {
        if (ReplayUI.shouldModifyViewport()) {
            cir.setReturnValue(ReplayUI.getNewGameHeight(1));
        }
    }

    @Inject(method = "onResize", at=@At("HEAD"), cancellable = true)
    public void onResize(int width, int height, CallbackInfo ci) {
        // 26.3: Window.onResize(int,int) — no window-handle argument
        // Drop cached real metrics so the next update recaptures from the new size
        if (this.overrideFramebufferWidth == -1) {
            this.realFramebufferWidth = -1;
            this.realFramebufferHeight = -1;
            this.realWindowWidth = -1;
            this.realWindowHeight = -1;
        }
    }

    @Inject(method = "calculateScale", at=@At("HEAD"), cancellable = true)
    public void calculateScale(int scale, boolean forceEven, CallbackInfoReturnable<Integer> cir) {
        if (Flashback.EXPORT_JOB != null && Flashback.EXPORT_JOB.shouldChangeFramebufferSize()) {
            int fbw = Flashback.EXPORT_JOB.getWidth();
            int fbh = Flashback.EXPORT_JOB.getHeight();

            int j = 1;
            while (j != scale && j < fbw && j < fbh && fbw / (j + 1) >= 320 && fbh / (j + 1) >= 240) {
                j++;
            }
            if (forceEven && j % 2 != 0) {
                j++;
            }
            cir.setReturnValue(j);
        } else if (ReplayUI.shouldModifyViewport()) {
            // Use real (pre-override) framebuffer metrics for GUI scale
            int realFbW = this.realFramebufferWidth > 0 ? this.realFramebufferWidth : this.framebufferWidth;
            int realFbH = this.realFramebufferHeight > 0 ? this.realFramebufferHeight : this.framebufferHeight;

            int j = 1;
            while (j != scale && j < realFbW && j < realFbH && realFbW / (j + 1) >= 320 && realFbH / (j + 1) >= 240) {
                j++;
            }
            if (forceEven && j % 2 != 0) {
                j++;
            }
            cir.setReturnValue(j);
        }
    }

    @Inject(method = "setGuiScale", at=@At("HEAD"), cancellable = true)
    public void setGuiScale(int scale, CallbackInfo ci) {
        if (Flashback.EXPORT_JOB != null && Flashback.EXPORT_JOB.shouldChangeFramebufferSize()) {
            int fbw = Flashback.EXPORT_JOB.getWidth();
            int fbh = Flashback.EXPORT_JOB.getHeight();

            this.guiScale = scale;
            int i = (int)((double)fbw / scale);
            this.guiScaledWidth = (double)fbw / scale > (double)i ? i + 1 : i;
            int j = (int)((double)fbh / scale);
            this.guiScaledHeight = (double)fbh / scale > (double)j ? j + 1 : j;

            ci.cancel();
        } else if (ReplayUI.shouldModifyViewport()) {
            int fbw = ReplayUI.getNewGameWidth(this.calculateWidthScaleFactor());
            int fbh = ReplayUI.getNewGameHeight(this.calculateHeightScaleFactor());

            this.guiScale = scale;
            int i = (int)((double)fbw / scale);
            this.guiScaledWidth = (double)fbw / scale > (double)i ? i + 1 : i;
            int j = (int)((double)fbh / scale);
            this.guiScaledHeight = (double)fbh / scale > (double)j ? j + 1 : j;

            ci.cancel();
        }
    }

}
