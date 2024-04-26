package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsSearch;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementTab.class)
public class AdvancementTabMixin {

    @Shadow
    @Final
    private PlacedAdvancement root;

    @Inject(method = "drawBackground", at = @At(value = "HEAD"), cancellable = true)
    public void drawBackgroundInject(DrawContext context, int x, int y, boolean selected, CallbackInfo ci) {
        if (root != null && AdvancementsSearch.ADVANCEMENTS_SEARCH_ID.equals(root.getAdvancementEntry().id())) {
            ci.cancel();
        }
    }

    @Inject(method = "drawIcon", at = @At(value = "HEAD"), cancellable = true)
    public void drawIconInject(DrawContext context, int x, int y, CallbackInfo ci) {
        if (root != null && AdvancementsSearch.ADVANCEMENTS_SEARCH_ID.equals(root.getAdvancementEntry().id())) {
            ci.cancel();
        }
    }

    @Inject(method = "isClickOnTab", at = @At(value = "HEAD"), cancellable = true)
    public void isClickOnTabInject(int screenX, int screenY, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (root != null && AdvancementsSearch.ADVANCEMENTS_SEARCH_ID.equals(root.getAdvancementEntry().id())) {
            cir.setReturnValue(false);
        }
    }
}
