package io.github.diskria.advancements_search.injection.mixin;

import io.github.diskria.advancements_search.AdvancementsSearchMod;
import io.github.diskria.advancements_search.injection.extension.AdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {

    @Shadow
    @Final
    public AdvancementTab tab;

    @Shadow
    @Final
    public AdvancementNode advancementNode;

    @Inject(
        method = "extractConnectivity",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void hideConnectivity(
        GuiGraphicsExtractor graphics, int xo, int yo, boolean background, CallbackInfo ci
    ) {
        if (AdvancementsSearchMod.isSearch(tab.getRootNode())) {
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        )
    )
    private void hideWidget(
        GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier location, int x, int y, int width, int height, Operation<Void> original
    ) {
        if (tab.getScreen() instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            Identifier advancementId = advancementsScreenExtension.advancements_search$getFlashingAdvancementId();
            if (!AdvancementsSearchMod.isSearch(tab.getRootNode()) &&
                advancementId != null &&
                advancementId == advancementNode.holder().id() &&
                advancementsScreenExtension.advancements_search$isFlashingAtInvisibleState()
            ) {
                return;
            }
            original.call(instance, renderPipeline, location, x, y, width, height);
        }
    }
}
