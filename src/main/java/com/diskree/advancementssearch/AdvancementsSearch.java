package com.diskree.advancementssearch;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class AdvancementsSearch implements ClientModInitializer {

    public static final Identifier ADVANCEMENTS_SEARCH_ID =
            new Identifier(BuildConfig.MOD_ID, BuildConfig.MOD_ID + "/root");

    public static boolean isSearch(PlacedAdvancement root) {
        return root != null && ADVANCEMENTS_SEARCH_ID.equals(root.getAdvancementEntry().id());
    }

    public static boolean isModCommand(@NotNull String command) {
        return command.startsWith("/" + BuildConfig.MOD_ID + " ");
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal(BuildConfig.MOD_ID)
                        .then(literal("search").then(argument("query", StringArgumentType.string()).executes(
                                context -> search(
                                        context.getSource().getClient(),
                                        StringArgumentType.getString(context, "query")
                                )
                        )))
                        .then(literal("open").then(argument("identifier", IdentifierArgumentType.identifier()).executes(
                                context -> openAdvancement(
                                        context.getSource().getClient(),
                                        context.getArgument("identifier", Identifier.class)
                                )
                        )))
                )
        );
    }

    private int search(@NotNull MinecraftClient client, String query) {
        if (client.player != null) {
            AdvancementsScreen screen = new AdvancementsScreen(client.player.networkHandler.getAdvancementHandler());
            client.setScreen(screen);
            if (client.currentScreen instanceof AdvancementsScreenImpl screenImpl) {
                screenImpl.advancementssearch$search(query);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int openAdvancement(@NotNull MinecraftClient client, Identifier identifier) {
        if (client.player != null) {
            AdvancementsScreen screen = new AdvancementsScreen(client.player.networkHandler.getAdvancementHandler());
            client.setScreen(screen);
            if (client.currentScreen instanceof AdvancementsScreenImpl screenImpl) {
                screenImpl.advancementssearch$openAdvancement(identifier);
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
