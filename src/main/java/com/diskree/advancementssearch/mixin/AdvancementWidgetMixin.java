package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsScreenImpl;
import com.diskree.advancementssearch.AdvancementsSearch;
import com.diskree.advancementssearch.HighlightType;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementObtainedStatus;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {

    @Shadow
    @Final
    public AdvancementTab tab;

    @Shadow
    @Final
    public PlacedAdvancement advancement;

    @Inject(
        method = "renderLines",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void cancelLinesRenderInSearch(
        DrawContext context,
        int x,
        int y,
        boolean border,
        CallbackInfo ci
    ) {
        if (AdvancementsSearch.isSearch(tab.getRoot())) {
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "renderWidgets",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"
        )
    )
    private void highlightWidget(
        DrawContext context,
        Function<Identifier, RenderLayer> renderLayers,
        Identifier sprite,
        int x,
        int y,
        int width,
        int height,
        Operation<Void> original
    ) {
        if (tab.getScreen() instanceof AdvancementsScreenImpl screenImpl) {
            Identifier highlightedAdvancementId = screenImpl.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearch.isSearch(tab.getRoot()) &&
                highlightedAdvancementId != null &&
                highlightedAdvancementId == advancement.getAdvancementEntry().id() &&
                screenImpl.advancementssearch$getHighlightType() == HighlightType.WIDGET &&
                screenImpl.advancementssearch$isHighlightAtInvisibleState()
            ) {
                return;
            }
            original.call(context, renderLayers, sprite, x, y, width, height);
        }
    }

    @Redirect(
        method = "renderWidgets",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementObtainedStatus;getFrameTexture(Lnet/minecraft/advancement/AdvancementFrame;)Lnet/minecraft/util/Identifier;"
        )
    )
    private @Nullable Identifier highlightObtainedStatus(AdvancementObtainedStatus status, AdvancementFrame frame) {
        if (tab.getScreen() instanceof AdvancementsScreenImpl screenImpl) {
            Identifier highlightedAdvancementId = screenImpl.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearch.isSearch(tab.getRoot()) &&
                highlightedAdvancementId != null &&
                highlightedAdvancementId == advancement.getAdvancementEntry().id() &&
                screenImpl.advancementssearch$getHighlightType() == HighlightType.OBTAINED_STATUS &&
                screenImpl.advancementssearch$isHighlightAtInvisibleState()
            ) {
                status = status == AdvancementObtainedStatus.OBTAINED ?
                    AdvancementObtainedStatus.UNOBTAINED : AdvancementObtainedStatus.OBTAINED;
            }
        }
        return status.getFrameTexture(frame);
    }

    @Inject(
        method = "drawTooltip",
        at = @At(value = "HEAD")
    )
    public void checkHighlight(
        DrawContext context,
        int originX,
        int originY,
        float alpha,
        int x,
        int y,
        CallbackInfo ci
    ) {
        if (tab.getScreen() instanceof AdvancementsScreenImpl screenImpl) {
            Identifier highlightedAdvancementId = screenImpl.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearch.isSearch(tab.getRoot()) &&
                highlightedAdvancementId != null &&
                highlightedAdvancementId == advancement.getAdvancementEntry().id()
            ) {
                screenImpl.advancementssearch$stopHighlight();
            }
        }
    }

    @ModifyReturnValue(
        method = "shouldRender",
        at = @At(value = "TAIL")
    )
    public boolean cancelTooltipRender(boolean original) {
        if (original && tab.getScreen() instanceof AdvancementsScreenImpl screenImpl) {
            Identifier highlightedAdvancementId = screenImpl.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearch.isSearch(tab.getRoot()) && highlightedAdvancementId != null) {
                return highlightedAdvancementId == advancement.getAdvancementEntry().id();
            }
        }
        return original;
    }
}
