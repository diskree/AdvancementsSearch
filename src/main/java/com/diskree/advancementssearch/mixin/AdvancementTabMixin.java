package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsScreenImpl;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AdvancementTab.class)
public class AdvancementTabMixin {

    @Shadow
    @Final
    private AdvancementsScreen screen;

    @Inject(
            method = "drawWidgetTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementWidget;drawTooltip(Lnet/minecraft/client/gui/DrawContext;IIFII)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void saveFocusedAdvancementWidget(
            DrawContext context,
            int mouseX,
            int mouseY,
            int x,
            int y,
            CallbackInfo ci,
            @Local(ordinal = 0) AdvancementWidget advancementWidget
    ) {
        ((AdvancementsScreenImpl) screen).advancementssearch$setFocusedAdvancementWidget(advancementWidget);
    }

    @Inject(
            method = "drawWidgetTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    public void resetFocusedAdvancementWidget(DrawContext context, int mouseX, int mouseY, int x, int y, CallbackInfo ci, @Local(ordinal = 0) boolean bl) {
        if (!bl) {
            ((AdvancementsScreenImpl) screen).advancementssearch$setFocusedAdvancementWidget(null);
        }
    }
}
