package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsScreenImpl;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
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
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void resetFocusedAdvancementWidget(
            DrawContext context,
            int mouseX,
            int mouseY,
            int x,
            int y,
            CallbackInfo ci,
            @Local(ordinal = 0) boolean bl
    ) {
        if (!bl) {
            ((AdvancementsScreenImpl) screen).advancementssearch$setFocusedAdvancementWidget(null);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIFFIIII)V"
            )
    )
    private void cancelBackgroundRenderInSearch(
            DrawContext instance,
            Identifier texture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight,
            Operation<Void> original
    ) {
        if (!((AdvancementsScreenImpl) screen).advancementssearch$isSearchActive()) {
            original.call(instance, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        }
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementWidget;renderLines(Lnet/minecraft/client/gui/DrawContext;IIZ)V",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            )
    )
    public void drawBlackBackgroundInSearch(DrawContext context, int x, int y, CallbackInfo ci) {
        AdvancementsScreenImpl screenImpl = (AdvancementsScreenImpl) screen;
        if (screenImpl.advancementssearch$isSearchActive()) {
            context.fill(
                    0,
                    0,
                    screenImpl.advancementssearch$getWindowWidth(false),
                    screenImpl.advancementssearch$getWindowHeight(false),
                    Colors.BLACK
            );
        }
    }
}
