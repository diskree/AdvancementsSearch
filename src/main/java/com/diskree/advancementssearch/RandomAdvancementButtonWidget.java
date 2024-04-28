package com.diskree.advancementssearch;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class RandomAdvancementButtonWidget extends ButtonWidget {

    private final int textureWidth = 12;
    private final int textureHeight = 12;
    private final Identifier texture = new Identifier(BuildConfig.MOD_ID, "icon/random_advancement");
    private final Identifier hoveredTexture = new Identifier(BuildConfig.MOD_ID, "icon/random_advancement_hovered");

    public RandomAdvancementButtonWidget(ButtonWidget.PressAction onPress) {
        super(0, 0, 0, 0, ScreenTexts.EMPTY, button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(
                                SoundEvents.ENTITY_FISHING_BOBBER_THROW,
                                0.5F,
                                0.4F / (client.player.getRandom().nextFloat() * 0.4F + 0.8F)
                        )
                );
            }
            onPress.onPress(button);
        }, DEFAULT_NARRATION_SUPPLIER);
        width = textureWidth;
        height = textureHeight;
        setTooltip(Tooltip.of(Text.translatable("advancementssearch.random_advancement_tooltip")));
    }

    @Override
    public void renderWidget(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX() + getWidth() / 2 - textureWidth / 2;
        int y = getY() + getHeight() / 2 - textureHeight / 2;
        context.drawGuiTexture(hovered ? hoveredTexture : texture, x, y, textureWidth, textureHeight);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
    }
}
