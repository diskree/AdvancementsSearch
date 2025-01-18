package com.diskree.advancementssearch.injection.mixin;

import com.diskree.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

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
        )
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
        if (screen instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            advancementsScreenExtension.advancementssearch$setFocusedAdvancementWidget(advancementWidget);
        }
    }

    @Inject(
        method = "drawWidgetTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V",
            shift = At.Shift.AFTER
        )
    )
    public void resetFocusedAdvancementWidget(
        DrawContext context,
        int mouseX,
        int mouseY,
        int x,
        int y,
        CallbackInfo ci,
        @Local(ordinal = 0) boolean shouldShowTooltip
    ) {
        if (!shouldShowTooltip && screen instanceof AdvancementsScreenExtension advancementsScreenExtension) {
            advancementsScreenExtension.advancementssearch$setFocusedAdvancementWidget(null);
        }
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V"
        )
    )
    private void cancelBackgroundRenderInSearch(
        DrawContext context,
        Function<Identifier, RenderLayer> renderLayers,
        Identifier sprite,
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
        if (screen instanceof AdvancementsScreenExtension advancementsScreenExtension &&
            !advancementsScreenExtension.advancementssearch$isSearchActive()
        ) {
            original.call(context, renderLayers, sprite, x, y, u, v, width, height, textureWidth, textureHeight);
        }
    }
}
