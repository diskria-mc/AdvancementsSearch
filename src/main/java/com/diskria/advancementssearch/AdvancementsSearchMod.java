package com.diskria.advancementssearch;

import com.diskria.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class AdvancementsSearchMod implements ClientModInitializer {

    public static final Identifier ADVANCEMENTS_SEARCH_ID =
            Identifier.of(BuildConfig.MOD_ID, BuildConfig.MOD_ID + "/root");

    public static boolean isSearch(PlacedAdvancement root) {
        return root != null && ADVANCEMENTS_SEARCH_ID.equals(root.getAdvancementEntry().id());
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal(BuildConfig.MOD_ID)
                        .then(literal("search")
                                .then(argument("query", StringArgumentType.string())
                                        .then(argument("by", StringArgumentType.word())
                                                .suggests(new SearchByTypeSuggestionProvider())
                                                .then(argument("autoHighlightSingle", BoolArgumentType.bool())
                                                        .then(argument("highlightType", StringArgumentType.word())
                                                                .suggests(new HighlightTypeSuggestionProvider())
                                                                .executes(context -> search(
                                                                        context.getSource().getClient(),
                                                                        StringArgumentType.getString(context, "query"),
                                                                        SearchByType.map(StringArgumentType.getString(context, "by")),
                                                                        BoolArgumentType.getBool(context, "autoHighlightSingle"),
                                                                        HighlightType.map(StringArgumentType.getString(context, "highlightType")))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(literal("highlight")
                                .then(argument("advancementId", IdentifierArgumentType.identifier())
                                        .then(argument("highlightType", StringArgumentType.word())
                                                .suggests(new HighlightTypeSuggestionProvider())
                                                .executes(context -> highlight(
                                                        context.getSource().getClient(),
                                                        context.getArgument("advancementId", Identifier.class),
                                                        HighlightType.map(StringArgumentType.getString(context, "highlightType")))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    public static class SearchByTypeSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(
                CommandContext<FabricClientCommandSource> context,
                SuggestionsBuilder builder
        ) {
            for (SearchByType type : SearchByType.values()) {
                builder.suggest(type.name().toLowerCase(Locale.ROOT));
            }
            return builder.buildFuture();
        }
    }

    public static class HighlightTypeSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(
                CommandContext<FabricClientCommandSource> context,
                SuggestionsBuilder builder
        ) {
            for (HighlightType type : HighlightType.values()) {
                builder.suggest(type.name().toLowerCase(Locale.ROOT));
            }
            return builder.buildFuture();
        }
    }

    private int search(
            @NotNull MinecraftClient client,
            String query,
            SearchByType searchByType,
            boolean autoHighlightSingle,
            HighlightType highlightType
    ) {
        if (client.player != null) {
            AdvancementsScreen screen = new AdvancementsScreen(client.player.networkHandler.getAdvancementHandler());
            client.setScreen(screen);
            if (client.currentScreen instanceof AdvancementsScreenExtension advancementsScreenExtension) {
                advancementsScreenExtension.advancementssearch$search(
                        query,
                        searchByType,
                        autoHighlightSingle,
                        highlightType
                );
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int highlight(
            @NotNull MinecraftClient client,
            Identifier advancementId,
            HighlightType highlightType
    ) {
        if (client.player != null) {
            AdvancementsScreen screen = new AdvancementsScreen(client.player.networkHandler.getAdvancementHandler());
            client.setScreen(screen);
            if (client.currentScreen instanceof AdvancementsScreenExtension advancementsScreenExtension) {
                advancementsScreenExtension.advancementssearch$highlightAdvancement(advancementId, highlightType);
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
