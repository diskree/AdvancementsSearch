package com.diskree.advancementssearch;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiceButton extends ButtonWidget {

    private static final ArrayList<Identifier> TEXTURES = new ArrayList<>(List.of(
            new Identifier(BuildConfig.MOD_ID, "icon/dice_1"),
            new Identifier(BuildConfig.MOD_ID, "icon/dice_2"),
            new Identifier(BuildConfig.MOD_ID, "icon/dice_3"),
            new Identifier(BuildConfig.MOD_ID, "icon/dice_4"),
            new Identifier(BuildConfig.MOD_ID, "icon/dice_5"),
            new Identifier(BuildConfig.MOD_ID, "icon/dice_6")
    ));
    private static Identifier currentTexture;

    private final int textureWidth = 12;
    private final int textureHeight = 12;

    public DiceButton(RollAction action) {
        super(0, 0, 0, 0, ScreenTexts.EMPTY, button -> {
            if (action.onRoll()) {
                reRoll();
            }
        }, DEFAULT_NARRATION_SUPPLIER);
        width = textureWidth;
        height = textureHeight;
        setTooltip(Tooltip.of(Text.translatable("advancementssearch.random_advancement_tooltip")));
        reRoll();
    }

    @Override
    public void renderWidget(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX() + getWidth() / 2 - textureWidth / 2;
        int y = getY() + getHeight() / 2 - textureHeight / 2;

        if (currentTexture != null) {
            context.drawGuiTexture(currentTexture, x, y, textureWidth, textureHeight);
        }
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
    }

    private static void reRoll() {
        Collections.shuffle(TEXTURES);
        if (currentTexture != null) {
            int currentIndex = TEXTURES.indexOf(currentTexture);
            if (currentIndex == 0) {
                Collections.swap(TEXTURES, currentIndex, TEXTURES.size() - 1);
            }
        }
        currentTexture = TEXTURES.get(0);
    }

    @Environment(value= EnvType.CLIENT)
    public interface RollAction {
        boolean onRoll();
    }
}
