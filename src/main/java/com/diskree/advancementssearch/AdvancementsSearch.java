package com.diskree.advancementssearch;

import net.fabricmc.api.ModInitializer;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.util.Identifier;

public class AdvancementsSearch implements ModInitializer {

    public static final Identifier ADVANCEMENTS_SEARCH_ID =
            new Identifier(BuildConfig.MOD_ID, BuildConfig.MOD_ID + "/root");

    public static boolean isSearch(PlacedAdvancement root) {
        return root != null && ADVANCEMENTS_SEARCH_ID.equals(root.getAdvancementEntry().id());
    }

    @Override
    public void onInitialize() {
    }
}
