package io.github.diskria.advancements_search.injection.mixin;

import io.github.diskria.advancements_search.AdvancementsSearchMod;
import io.github.diskria.advancements_search.injection.extension.AdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementTab.class)
public class AdvancementTabMixin {

    @Shadow
    @Final
    private AdvancementsScreen screen;

    @Shadow
    @Nullable
    public AdvancementWidget hovered;

    @Shadow
    @Final
    private AdvancementNode rootNode;

    @Inject(
        method = "tick",
        at = @At(value = "TAIL")
    )
    public void updateFocusedAdvancementWidget(int relativeMouseX, int relativeMouseY, CallbackInfo ci) {
        if (screen instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            advancementsScreenExtension.advancements_search$setFocusedAdvancementWidget(hovered);
        }
    }

    @WrapWithCondition(
        method = "extractContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
        )
    )
    private boolean hideTiledBackground(
        GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier texture,
        int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight
    ) {
        return screen instanceof AdvancementsScreenExtension advancementsScreenExtension &&
            !advancementsScreenExtension.advancements_search$isSearchActive();
    }

    @ModifyExpressionValue(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;isMouseOver(IIII)Z")
    )
    public boolean stopFlashingOnHover(boolean original, @Local(name = "widget") AdvancementWidget widget) {
        if (original && screen instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            Identifier advancementId = advancementsScreenExtension.advancements_search$getFlashingAdvancementId();
            if (advancementId != null && !AdvancementsSearchMod.isSearch(rootNode)) {
                if (advancementId != widget.advancementNode.holder().id()) {
                    return false;
                }
                advancementsScreenExtension.advancements_search$stopFlashing();
            }
        }
        return original;
    }
}
