package com.moulberry.flashback.mixin.visuals;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.visuals.WorldRenderHook;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Shadow @Final private LevelTargetBundle targets;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addAlwaysOnTopPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;)V", shift = At.Shift.BEFORE), require = 0)
    public void renderLevelPost(GraphicsResourceAllocator resourceAllocator, boolean bl, CameraRenderState cameraState,
        GpuBufferSlice terrainFog, Vector4f fogColor, boolean bl2, boolean bl3,
        CallbackInfo ci, @Local FrameGraphBuilder frameGraphBuilder
    ) {
        if (!Flashback.isInReplay()) {
            return;
        }

        FramePass framePass = frameGraphBuilder.addPass("flashback_mod_pass");
        this.targets.main = framePass.readsAndWrites(this.targets.main);
        // 26.3: LevelTargetBundle no longer exposes translucent/itemEntity/particles fields
        if (!this.targets.transmittance.isEmpty()) {
            for (int i = 0; i < this.targets.transmittance.size(); i++) {
                this.targets.transmittance.set(i, framePass.readsAndWrites(this.targets.transmittance.get(i)));
            }
        }
        if (this.targets.accumulate != null) {
            this.targets.accumulate = framePass.readsAndWrites(this.targets.accumulate);
        }
        if (this.targets.entityOutline != null) {
            this.targets.entityOutline = framePass.readsAndWrites(this.targets.entityOutline);
        }
        framePass.executes(() -> {
            PoseStack poseStack = new PoseStack();
            if (cameraState != null && cameraState.viewRotationMatrix != null) {
                poseStack.mulPose(cameraState.viewRotationMatrix);
            }

            // Set model view stack to identity
            var modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewStack.identity();

            WorldRenderHook.renderHook(poseStack, cameraState);

            // Pop model view stack
            modelViewStack.popMatrix();
        });
    }

}
