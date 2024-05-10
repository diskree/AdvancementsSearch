package com.diskree.advancementssearch;

import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.util.Identifier;

public interface AdvancementsScreenImpl {

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
}
