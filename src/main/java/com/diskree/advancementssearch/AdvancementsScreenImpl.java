package com.diskree.advancementssearch;

import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.util.Identifier;

public interface AdvancementsScreenImpl {

    void advancementssearch$setFocusedAdvancementWidget(AdvancementWidget focusedAdvancementWidget);

    boolean advancementssearch$isSearchActive();

    int advancementssearch$getWindowWidth(boolean withBorder);

    int advancementssearch$getWindowHeight(boolean withBorder);

    Identifier advancementssearch$getHighlightedAdvancementId();

    boolean advancementssearch$isHighlightAtInvisibleState();

    void advancementssearch$stopHighlight();
}
