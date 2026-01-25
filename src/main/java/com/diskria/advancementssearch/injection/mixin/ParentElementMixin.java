package com.diskria.advancementssearch.injection.mixin;

import com.diskria.advancementssearch.injection.extension.AdvancementsScreenExtension;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.input.CharInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParentElement.class)
public interface ParentElementMixin {

    @Inject(
            method = "mouseReleased",
            at = @At(value = "HEAD")
    )
    private void onMouseReleasedInAdvancementsScreen(
        Click click, CallbackInfoReturnable<Boolean> cir
    ) {
        if (this instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            advancementsScreenExtension.advancementssearch$onMouseReleased(click);
        }
    }

    @Inject(
            method = "charTyped",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onCharTypedInAdvancementsScreen(CharInput input, CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof AdvancementsScreenExtension advancementsScreenExtension &&
                advancementsScreenExtension.advancementssearch$charTyped(input)
        ) {
            cir.setReturnValue(true);
        }
    }
}
