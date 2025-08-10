package com.diskria.advancementssearch.injection.mixin;

import com.diskria.advancementssearch.AdvancementsSearchMod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    @WrapOperation(
        method = "keyPressed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V",
            ordinal = 1
        )
    )
    private void cancelCloseScreen(MinecraftClient client, Screen screen, Operation<Void> original) {
        if (!AdvancementsSearchMod.isModCommand(chatField.getText())) {
            original.call(client, screen);
        }
    }
}
