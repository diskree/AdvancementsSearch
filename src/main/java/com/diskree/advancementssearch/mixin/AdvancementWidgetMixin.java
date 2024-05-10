package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsScreenImpl;
import com.diskree.advancementssearch.AdvancementsSearch;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.text.*;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {

    @Unique
    private List<OrderedText> searchResultHint;

    @Shadow
    @Final
    private static Identifier TITLE_BOX_TEXTURE;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    public AdvancementTab tab;

    @Shadow
    @Final
    public PlacedAdvancement advancement;

    @Shadow
    @Final
    private int width;

    @Shadow
    @Final
    private int y;

    @Shadow
    protected abstract List<StringVisitable> wrapDescription(Text text, int width);

    @Inject(
            method = "<init>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementWidget;description:Ljava/util/List;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void calculateSearchResultHintWidth(
            @NotNull AdvancementTab tab,
            MinecraftClient client,
            PlacedAdvancement advancement,
            AdvancementDisplay display,
            CallbackInfo ci,
            @Local(ordinal = 3) LocalIntRef maxWidthRef
    ) {
        if (AdvancementsSearch.isSearch(tab.getRoot())) {
            int maxWidth = maxWidthRef.get();
            searchResultHint = Language.getInstance().reorder(wrapDescription(
                    Texts.setStyleIfAbsent(
                            Text.translatable("advancementssearch.search_result_hint").copy(),
                            Style.EMPTY.withColor(Formatting.DARK_GRAY)
                    ),
                    maxWidth
            ));
            for (OrderedText orderedText : searchResultHint) {
                maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(orderedText));
            }
            maxWidthRef.set(maxWidth);
        }
    }

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

    @Inject(
            method = "drawTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIIIIIII)V",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void renderSearchResultHint(
            DrawContext context,
            int originX,
            int originY,
            float alpha,
            int x,
            int y,
            CallbackInfo ci,
            @Local(ordinal = 1) boolean shouldShowOnTop,
            @Local(ordinal = 8) int tooltipX,
            @Local(ordinal = 7) int tooltipY
    ) {
        if (AdvancementsSearch.isSearch(tab.getRoot())) {
            int textureHeight = 32 + searchResultHint.size() * 9;
            int textureY;
            int textY;
            if (shouldShowOnTop) {
                textureY = tooltipY;
                textY = originY + this.y + 9 + 17;
            } else {
                textureY = tooltipY + 26 - textureHeight;
                textY = tooltipY + 26 - textureHeight + 7;
            }
            context.drawGuiTexture(TITLE_BOX_TEXTURE, tooltipX, textureY, width, textureHeight);
            for (int line = 0; line < searchResultHint.size(); ++line) {
                context.drawText(
                        client.textRenderer,
                        searchResultHint.get(line),
                        tooltipX + 5,
                        textY + line * 9,
                        Colors.GRAY,
                        false
                );
            }
        }
    }

    @WrapOperation(
            method = "renderWidgets",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"
            )
    )
    private void highlight(
            DrawContext context,
            Identifier texture,
            int x,
            int y,
            int width,
            int height,
            Operation<Void> original
    ) {
        if (tab.getScreen() instanceof AdvancementsScreenImpl screenImpl) {
            Identifier highlightedAdvancementId = screenImpl.advancementssearch$getHighlightedAdvancementId();
            if (AdvancementsSearch.isSearch(tab.getRoot()) ||
                    highlightedAdvancementId == null ||
                    highlightedAdvancementId != advancement.getAdvancementEntry().id() ||
                    !screenImpl.advancementssearch$isHighlightAtInvisibleState()) {
                original.call(context, texture, x, y, width, height);
            }
        }
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
                    highlightedAdvancementId == advancement.getAdvancementEntry().id()) {
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
