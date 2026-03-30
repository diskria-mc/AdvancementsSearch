package io.github.diskria.advancements_search.injection.extension;

import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public interface AdvancementsScreenExtension {
    void advancements_search$setFocusedAdvancementWidget(AdvancementWidget widget);

    boolean advancements_search$isSearchActive();

    int advancements_search$getTreeWidth();

    int advancements_search$getTreeHeight();

    Identifier advancements_search$getFlashingAdvancementId();

    boolean advancements_search$isFlashingAtInvisibleState();

    void advancements_search$stopFlashing();

    void advancements_search$tick();

    boolean advancements_search$charTyped(CharacterEvent characterEvent);

    void advancements_search$resize(int width, int height);

    void advancements_search$onMouseReleased(MouseButtonEvent mouseButtonEvent);
}
