package com.diskree.advancementssearch.injection.extension;

import com.diskree.advancementssearch.HighlightType;
import com.diskree.advancementssearch.SearchByType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.util.Identifier;

public interface AdvancementsScreenExtension {
    void advancementssearch$setFocusedAdvancementWidget(AdvancementWidget focusedAdvancementWidget);

    boolean advancementssearch$isSearchActive();

    int advancementssearch$getTreeWidth();

    int advancementssearch$getTreeHeight();

    Identifier advancementssearch$getHighlightedAdvancementId();

    HighlightType advancementssearch$getHighlightType();

    boolean advancementssearch$isHighlightAtInvisibleState();

    void advancementssearch$stopHighlight();

    void advancementssearch$search(
        String query,
        SearchByType searchByType,
        boolean autoHighlightSingle,
        HighlightType highlightType
    );

    void advancementssearch$highlightAdvancement(Identifier advancementId, HighlightType highlightType);

    void advancementssearch$tick();

    boolean advancementssearch$charTyped(char chr, int modifiers);

    void advancementssearch$resize(MinecraftClient client, int width, int height);

    void advancementssearch$onMouseReleased(double mouseX, double mouseY, int button);
}
