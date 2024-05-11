package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsScreenImpl;
import com.diskree.advancementssearch.AdvancementsSearch;
import com.diskree.advancementssearch.HighlightType;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancement.Advancement;
import net.minecraft.client.gui.screen.advancement.AdvancementObtainedStatus;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {

    @Shadow
    @Final
    public AdvancementTab tab;

    @Shadow
    @Final
    public Advancement advancement;

    @Inject(
        method = "method_2323",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void cancelLinesRenderInSearch(
        int x,
        int y,
        boolean border,
        CallbackInfo ci
    ) {
        if (AdvancementsSearch.isSearch(tab.root)) {
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "method_2325",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementWidget;blit(IIIIII)V"
        )
    )
    private void highlightWidget(
        AdvancementWidget widget,
        int x,
        int y,
        int u,
        int v,
        int width,
        int height,
        Operation<Void> original
    ) {
        if (tab.field_2687 instanceof AdvancementsScreenImpl screenImpl) {
            Identifier highlightedAdvancementId = screenImpl.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearch.isSearch(tab.root) &&
                highlightedAdvancementId != null &&
                highlightedAdvancementId == advancement.getId() &&
                screenImpl.advancementssearch$getHighlightType() == HighlightType.WIDGET &&
                screenImpl.advancementssearch$isHighlightAtInvisibleState()
            ) {
                return;
            }
            original.call(widget, x, y, u, v, width, height);
        }
    }

    @Redirect(
        method = "method_2325",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementObtainedStatus;getSpriteIndex()I"
        )
    )
    private int highlightObtainedStatus(AdvancementObtainedStatus status) {
        if (tab.field_2687 instanceof AdvancementsScreenImpl screenImpl) {
            Identifier highlightedAdvancementId = screenImpl.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearch.isSearch(tab.root) &&
                highlightedAdvancementId != null &&
                highlightedAdvancementId == advancement.getId() &&
                screenImpl.advancementssearch$getHighlightType() == HighlightType.OBTAINED_STATUS &&
                screenImpl.advancementssearch$isHighlightAtInvisibleState()
            ) {
                status = status == AdvancementObtainedStatus.OBTAINED ?
                    AdvancementObtainedStatus.UNOBTAINED : AdvancementObtainedStatus.OBTAINED;
            }
        }
        return status.getSpriteIndex();
    }

    @Inject(
        method = "method_2331",
        at = @At(value = "HEAD")
    )
    public void checkHighlight(
        int originX,
        int originY,
        float alpha,
        int x,
        int y,
        CallbackInfo ci
    ) {
        if (tab.field_2687 instanceof AdvancementsScreenImpl screenImpl) {
            Identifier highlightedAdvancementId = screenImpl.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearch.isSearch(tab.root) &&
                highlightedAdvancementId != null &&
                highlightedAdvancementId == advancement.getId()
            ) {
                screenImpl.advancementssearch$stopHighlight();
            }
        }
    }

    @ModifyReturnValue(
        method = "method_2329",
        at = @At(value = "TAIL")
    )
    public boolean cancelTooltipRender(boolean original) {
        if (original && tab.field_2687 instanceof AdvancementsScreenImpl screenImpl) {
            Identifier highlightedAdvancementId = screenImpl.advancementssearch$getHighlightedAdvancementId();
            if (!AdvancementsSearch.isSearch(tab.root) && highlightedAdvancementId != null) {
                return highlightedAdvancementId == advancement.getId();
            }
        }
        return original;
    }
}
