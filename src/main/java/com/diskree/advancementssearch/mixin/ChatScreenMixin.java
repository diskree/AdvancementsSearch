package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsSearch;
import net.minecraft.client.gui.screen.ChatScreen;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(
        method = "sendMessage",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendCommand(Ljava/lang/String;Lnet/minecraft/text/Text;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void cancelCloseScreen(
        @NotNull String chatText,
        boolean addToHistory,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (AdvancementsSearch.isModCommand(chatText)) {
            cir.setReturnValue(false);
        }
    }
}
