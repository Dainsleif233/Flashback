package com.moulberry.flashback;

import com.mojang.blaze3d.platform.Window;

public class WindowSizeTracker {

    /**
     * 26.3 windows are SDL-backed. Do not query GLFW on the window handle —
     * it returns bogus sizes (e.g. 32x32) and causes framebuffer resize jitter.
     * Prefer the values already maintained by the Minecraft Window class.
     */

    public static int getWidth(Window window) {
        if (window == null) {
            return 1;
        }
        int w = window.framebufferWidth;
        return w > 0 ? w : Math.max(1, window.getWidth());
    }

    public static int getHeight(Window window) {
        if (window == null) {
            return 1;
        }
        int h = window.framebufferHeight;
        return h > 0 ? h : Math.max(1, window.getHeight());
    }

}
