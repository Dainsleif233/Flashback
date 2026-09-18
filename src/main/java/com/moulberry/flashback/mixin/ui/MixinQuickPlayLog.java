package com.moulberry.flashback.mixin.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.quickplay.QuickPlayLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Version-isolated launcher instances may pass a locked game directory as the
 * quick-play log path, which floods the log with file-lock errors. Quick-play
 * logging is optional; skip it.
 */
@Mixin(QuickPlayLog.class)
public class MixinQuickPlayLog {

    @Inject(method = "log(Lnet/minecraft/client/Minecraft;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void flashback$skipQuickPlayLog(Minecraft minecraft, CallbackInfo ci) {
        ci.cancel();
    }

}
