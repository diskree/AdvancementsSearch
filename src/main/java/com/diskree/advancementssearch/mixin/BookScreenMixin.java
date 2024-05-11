package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsSearch;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
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
            target = "Lnet/minecraft/client/MinecraftClient;openScreen(Lnet/minecraft/client/gui/screen/Screen;)V"
        )
    )
    private void cancelCloseScreen(
        MinecraftClient client,
        Screen screen,
        Operation<Void> original,
        @Local @NotNull ClickEvent clickEvent
    ) {
        if (!AdvancementsSearch.isModCommand(clickEvent.getValue())) {
            original.call(client, screen);
        }
    }
}
