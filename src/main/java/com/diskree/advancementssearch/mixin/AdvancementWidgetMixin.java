package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsSearch;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementWidget.class)
public class AdvancementWidgetMixin {

    @Shadow
    @Final
    private AdvancementTab tab;

    @Shadow
    @Nullable
    public AdvancementWidget parent;

    @Inject(
            method = "renderLines",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void renderLinesInject(DrawContext context, int x, int y, boolean border, CallbackInfo ci) {
        if (tab.getRoot() != null && AdvancementsSearch.ADVANCEMENTS_SEARCH_ID.equals(tab.getRoot().getAdvancementEntry().id())) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "renderWidgets",
            at = @At(value = "HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    public int modifyWidgetX(int x) {
        if (tab.getRoot() != null && AdvancementsSearch.ADVANCEMENTS_SEARCH_ID.equals(tab.getRoot().getAdvancementEntry().id()) && parent == null) {
            x -= 2;
        }
        return x;
    }
}
