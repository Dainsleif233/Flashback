package com.moulberry.flashback.mixin.client;

import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.moulberry.flashback.Flashback;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 26.3 pipeline shader preload can miss mod assets via ResourceManager.
 * Fallback-read flashback shaders from the mod jar / resourcepacks folder.
 */
@Mixin(targets = "net.minecraft.client.renderer.GameRenderer$1")
public class MixinGameRendererShaderSource {

    @Inject(
        method = "getShader(Lnet/minecraft/resources/Identifier;Lcom/mojang/renderpearl/api/pipeline/ShaderType;)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void flashback$loadShaderFromClasspath(Identifier id, ShaderType type, CallbackInfoReturnable<String> cir) {
        if (id == null || type == null || !"flashback".equals(id.getNamespace())) {
            return;
        }

        Identifier file = null;
        try {
            file = type.idConverter().idToFile(id);
        } catch (Throwable ignored) {}

        String pathSuffix = id.getPath(); // e.g. core/imgui_b3d
        String ext = (type == ShaderType.FRAGMENT) ? ".fsh" : ".vsh";
        if (file != null) {
            pathSuffix = file.getPath();
            if (pathSuffix.startsWith("shaders/")) {
                pathSuffix = pathSuffix.substring("shaders/".length());
            }
        }

        String[] classpathCandidates = new String[] {
            "assets/flashback/shaders/" + pathSuffix,
            "assets/flashback/shaders/core/" + id.getPath() + ext,
            "assets/flashback/shaders/" + id.getPath() + ext
        };
        ClassLoader cl = Flashback.class.getClassLoader();
        for (String path : classpathCandidates) {
            try (InputStream in = cl.getResourceAsStream(path)) {
                if (in != null) {
                    cir.setReturnValue(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    return;
                }
            } catch (Throwable ignored) {}
        }

        try {
            Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            Path[] diskCandidates = new Path[] {
                gameDir.resolve("config/flashback/shaders").resolve(pathSuffix),
                gameDir.resolve("resourcepacks/flashback_shaders/assets/flashback/shaders/core/" + id.getPath() + ext),
                gameDir.resolve("resourcepacks/flashback_shaders/assets/flashback/shaders/" + pathSuffix)
            };
            for (Path p : diskCandidates) {
                if (Files.isRegularFile(p)) {
                    cir.setReturnValue(Files.readString(p));
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

}
