package com.diskree.advancementssearch.injection.mixin;

import com.diskree.advancementssearch.AdvancementsSearchMod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.text.ClickEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BookScreen.class)
public class BookScreenMixin {

    @WrapOperation(
        method = "handleTextClick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/BookScreen;closeScreen()V"
        )
    )
    private void cancelCloseScreen(
        BookScreen screen,
        Operation<Void> original,
        @Local @NotNull ClickEvent clickEvent
    ) {
        if (clickEvent instanceof ClickEvent.RunCommand(
            String command
        ) && !AdvancementsSearchMod.isModCommand(command)) {
            original.call(screen);
        }
    }
}
