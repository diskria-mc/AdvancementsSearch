package com.diskria.advancementssearch.injection.extension;

import com.diskria.advancementssearch.HighlightType;
import com.diskria.advancementssearch.SearchByType;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.input.CharInput;
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

    boolean advancementssearch$charTyped(CharInput input);

    void advancementssearch$resize(int width, int height);

    void advancementssearch$onMouseReleased(Click click);
}
