package com.diskria.advancementssearch.injection.mixin;

import com.diskria.advancementssearch.AdvancementsSearchMod;
import com.diskria.advancementssearch.HighlightType;
import com.diskria.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
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
    public void cancelLinesRenderInSearch(
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
    private void highlightWidget(
        GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier location, int x, int y, int width, int height, Operation<Void> original
    ) {
        if (tab.getScreen() instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            Identifier advancementId = advancementsScreenExtension.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearchMod.isSearch(tab.getRootNode()) &&
                advancementId != null &&
                advancementId == advancementNode.holder().id() &&
                advancementsScreenExtension.advancementssearch$getHighlightType() == HighlightType.WIDGET &&
                advancementsScreenExtension.advancementssearch$isHighlightAtInvisibleState()
            ) {
                return;
            }
            original.call(instance, renderPipeline, location, x, y, width, height);
        }
    }

    @ModifyReceiver(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidgetType;frameSprite(Lnet/minecraft/advancements/AdvancementType;)Lnet/minecraft/resources/Identifier;"
        )
    )
    private AdvancementWidgetType highlightObtainedStatus(AdvancementWidgetType original, AdvancementType type) {
        if (tab.getScreen() instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            Identifier advancementId = advancementsScreenExtension.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearchMod.isSearch(tab.getRootNode()) &&
                advancementId != null &&
                advancementId == advancementNode.holder().id() &&
                advancementsScreenExtension.advancementssearch$getHighlightType() == HighlightType.OBTAINED_STATUS &&
                advancementsScreenExtension.advancementssearch$isHighlightAtInvisibleState()
            ) {
                return original == AdvancementWidgetType.OBTAINED ?
                    AdvancementWidgetType.UNOBTAINED : AdvancementWidgetType.OBTAINED;
            }
        }
        return original;
    }

    @ModifyReturnValue(
        method = "isMouseOver",
        at = @At(value = "TAIL")
    )
    public boolean cancelTooltipRender(boolean original) {
        if (original && tab.getScreen() instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            Identifier advancementId = advancementsScreenExtension.advancementssearch$getHighlightedAdvancementId();
            if (advancementId != null && !AdvancementsSearchMod.isSearch(tab.getRootNode())) {
                if (advancementId == advancementNode.holder().id()) {
                    advancementsScreenExtension.advancementssearch$stopHighlight();
                    return true;
                }
                return false;
            }
        }
        return original;
    }
}
