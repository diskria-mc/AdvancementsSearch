package com.diskria.advancementssearch.injection.extension;

import com.diskria.advancementssearch.HighlightType;
import com.diskria.advancementssearch.SearchByType;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

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

    boolean advancementssearch$charTyped(CharacterEvent characterEvent);

    void advancementssearch$resize(int width, int height);

    void advancementssearch$onMouseReleased(MouseButtonEvent mouseButtonEvent);
}
