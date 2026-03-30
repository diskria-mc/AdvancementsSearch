package io.github.diskria.advancements_search.injection.mixin;

import io.github.diskria.advancements_search.injection.extension.AdvancementsScreenExtension;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin {

    @Inject(
            method = "mouseReleased",
            at = @At(value = "HEAD")
    )
    private void onMouseReleasedInAdvancementsScreen(
        MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir
    ) {
        if (this instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            advancementsScreenExtension.advancements_search$onMouseReleased(event);
        }
    }

    @Inject(
            method = "charTyped",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onCharTypedInAdvancementsScreen(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof AdvancementsScreenExtension advancementsScreenExtension &&
                advancementsScreenExtension.advancements_search$charTyped(event)
        ) {
            cir.setReturnValue(true);
        }
    }
}
