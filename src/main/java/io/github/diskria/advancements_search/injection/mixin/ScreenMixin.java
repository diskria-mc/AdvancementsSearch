package io.github.diskria.advancements_search.injection.mixin;

import io.github.diskria.advancements_search.injection.extension.AdvancementsScreenExtension;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(
            method = "tick",
            at = @At(value = "HEAD")
    )
    private void tickInAdvancementsScreen(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            advancementsScreenExtension.advancements_search$tick();
        }
    }

    @Inject(
            method = "resize",
            at = @At(value = "HEAD")
    )
    private void resizeInAdvancementsScreen(int width, int height, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            advancementsScreenExtension.advancements_search$resize(width, height);
        }
    }
}
