package com.moulberry.flashback.mixin.playback;

import com.moulberry.flashback.ext.ItemInHandRendererExt;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Set;

/**
 * 26.3: FirstPersonHandsAndItemsRenderer internals changed substantially.
 * Keep only the replay spectating hook; 26.2 shadow fields no longer exist.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MixinItemInHandRenderer implements ItemInHandRendererExt {

    @Override
    public void flashback$renderHandsWithItems(float partialTick, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, AbstractClientPlayer clientPlayer, int i, @Nullable Set<InteractionHand> renderableArms) {
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState != null && editorState.isEntityHidden(clientPlayer)) {
            return;
        }
        // Hand-override path not yet reimplemented for 26.3 renderer API.
    }

}
