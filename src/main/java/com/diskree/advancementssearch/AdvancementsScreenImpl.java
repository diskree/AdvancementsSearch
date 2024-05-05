package com.diskree.advancementssearch;

import net.minecraft.client.gui.screen.advancement.AdvancementWidget;

public interface AdvancementsScreenImpl {
    void advancementssearch$setFocusedAdvancementWidget(AdvancementWidget focusedAdvancementWidget);

    boolean advancementssearch$isSearchActive();

    int advancementssearch$getWindowWidth(boolean withBorder);

    int advancementssearch$getWindowHeight(boolean withBorder);
}
