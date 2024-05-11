package com.diskree.advancementssearch;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.advancement.Advancement;
import net.minecraft.util.Identifier;

public class AdvancementsSearch implements ClientModInitializer {

    public static final Identifier ADVANCEMENTS_SEARCH_ID =
        new Identifier(BuildConfig.MOD_ID, BuildConfig.MOD_ID + "/root");

    public static boolean isSearch(Advancement root) {
        return root != null && ADVANCEMENTS_SEARCH_ID.equals(root.getId());
    }

    @Override
    public void onInitializeClient() {
    }
}
