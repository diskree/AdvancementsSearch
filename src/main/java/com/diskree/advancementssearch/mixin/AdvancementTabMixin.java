package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsScreenImpl;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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

import java.awt.*;

@Mixin(AdvancementTab.class)
public class AdvancementTabMixin {

    @Shadow
    @Final
    private AdvancementsScreen screen;

    @Inject(
        method = "drawWidgetTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementWidget;drawTooltip(IIFII)V",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void saveFocusedAdvancementWidget(
        int mouseX,
        int mouseY,
        int x,
        int y,
        CallbackInfo ci,
        @Local(ordinal = 0) AdvancementWidget advancementWidget
    ) {
        if (screen instanceof AdvancementsScreenImpl screenImpl) {
            screenImpl.advancementssearch$setFocusedAdvancementWidget(advancementWidget);
        }
    }

    @Inject(
        method = "drawWidgetTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;popMatrix()V",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void resetFocusedAdvancementWidget(
        int mouseX,
        int mouseY,
        int x,
        int y,
        CallbackInfo ci,
        @Local(ordinal = 0) boolean shouldShowTooltip
    ) {
        if (!shouldShowTooltip && screen instanceof AdvancementsScreenImpl screenImpl) {
            screenImpl.advancementssearch$setFocusedAdvancementWidget(null);
        }
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;blit(IIFFIIII)V"
        )
    )
    private void cancelBackgroundRenderInSearch(
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
        if (screen instanceof AdvancementsScreenImpl screenImpl && !screenImpl.advancementssearch$isSearchActive()) {
            original.call(x, y, u, v, width, height, textureWidth, textureHeight);
        }
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementWidget;renderLines(IIZ)V",
            shift = At.Shift.BEFORE,
            ordinal = 0
        )
    )
    public void drawBlackBackgroundInSearch(CallbackInfo ci) {
        if (screen instanceof AdvancementsScreenImpl screenImpl && screenImpl.advancementssearch$isSearchActive()) {
            AdvancementTab.fill(
                0,
                0,
                screenImpl.advancementssearch$getTreeWidth(),
                screenImpl.advancementssearch$getTreeHeight(),
                Color.BLACK.getRGB()
            );
        }
    }
}
