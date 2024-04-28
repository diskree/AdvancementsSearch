package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsSearch;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AdvancementTab.class)
public class AdvancementTabMixin {

    @Shadow
    @Final
    private PlacedAdvancement root;

    @Redirect(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;originX:D",
                    opcode = Opcodes.PUTFIELD
            )
    )
    public void modifyOriginX(@NotNull AdvancementTab instance, double x) {
        instance.originX = AdvancementsSearch.isSearch(root) ? 3 : x;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;originY:D",
                    opcode = Opcodes.PUTFIELD
            )
    )
    public void modifyOriginY(@NotNull AdvancementTab instance, double y) {
        instance.originY = AdvancementsSearch.isSearch(root) ? 0 : y;
    }
}
