package com.diskree.advancementssearch.injection.mixin;

import com.diskree.advancementssearch.AdvancementsSearchMod;
import com.diskree.advancementssearch.HighlightType;
import com.diskree.advancementssearch.SearchByType;
import com.diskree.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancement.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen implements AdvancementsScreenExtension {

    @Unique
    private static final Identifier CREATIVE_INVENTORY_TEXTURE =
        Identifier.ofVanilla("textures/gui/container/creative_inventory/tab_item_search.png");

    @Unique
    private static final Text SEARCH_TITLE = Text.translatable("gui.recipebook.search_hint");

    @Unique
    private static final Point SEARCH_FIELD_UV = new Point(80, 4);

    @Unique
    private static final int SEARCH_FIELD_WIDTH = 90;

    @Unique
    private static final int SEARCH_FIELD_HEIGHT = 12;

    @Unique
    private static final int WINDOW_BORDER_SIZE = 9;

    @Unique
    private static final int WINDOW_HEADER_HEIGHT = 18;

    @Unique
    private static final int WIDGET_SIZE = 26;

    @Unique
    private static final int TREE_X_OFFSET = 3;

    @Unique
    private static final int WIDGET_HIGHLIGHT_COUNT = 5;

    @Unique
    private static final int WIDGET_HIGHLIGHT_TICKS = 3;

    @Unique
    private static final int SEARCH_FIELD_TEXT_LEFT_OFFSET = 2;

    @Unique
    private TextFieldWidget searchField;

    @Unique
    private PlacedAdvancement searchRootAdvancement;

    @Unique
    private AdvancementTab searchTab;

    @Unique
    private final ArrayList<PlacedAdvancement> searchResults = new ArrayList<>();

    @Unique
    private boolean isSearchActive;

    @Unique
    private int searchResultsColumnsCount;

    @Unique
    private int searchResultsOriginX;

    @Unique
    private AdvancementWidget focusedAdvancementWidget;

    @Unique
    private int windowX;

    @Unique
    private int windowY;

    @Unique
    private int treeWidth;

    @Unique
    private int treeHeight;

    @Unique
    private PlacedAdvancement highlightedAdvancement;

    @Unique
    private Identifier highlightedAdvancementId;

    @Unique
    private HighlightType highlightType;

    @Unique
    private int widgetHighlightCounter;

    @Unique
    private boolean isFocusedAdvancementClicked;

    public AdvancementsScreenMixin() {
        super(null);
    }

    @Override
    public void advancementssearch$setFocusedAdvancementWidget(AdvancementWidget focusedAdvancementWidget) {
        this.focusedAdvancementWidget = focusedAdvancementWidget;
    }

    @Override
    public boolean advancementssearch$isSearchActive() {
        return isSearchActive;
    }

    @Override
    public int advancementssearch$getTreeWidth() {
        return treeWidth;
    }

    @Override
    public int advancementssearch$getTreeHeight() {
        return treeHeight;
    }

    @Override
    public Identifier advancementssearch$getHighlightedAdvancementId() {
        return highlightedAdvancementId;
    }

    @Override
    public HighlightType advancementssearch$getHighlightType() {
        return highlightType;
    }

    @Override
    public boolean advancementssearch$isHighlightAtInvisibleState() {
        return widgetHighlightCounter != 0 && (widgetHighlightCounter / WIDGET_HIGHLIGHT_TICKS) % 2 == 0;
    }

    @Override
    public void advancementssearch$stopHighlight() {
        highlightedAdvancementId = null;
        highlightType = null;
        widgetHighlightCounter = 0;
    }

    @Override
    public void advancementssearch$search(
        String query,
        SearchByType searchByType,
        boolean autoHighlightSingle,
        HighlightType highlightType
    ) {
        searchInternal(query, searchByType);
        if (autoHighlightSingle && searchResults.size() == 1) {
            highlight(searchResults.getFirst(), highlightType);
            searchResults.clear();
            return;
        }
        query = SearchByType.addMaskToQuery(query, searchByType);
        searchField.setText(query);
        isSearchActive = !query.isEmpty();
        showSearchResults();
    }

    @Override
    public void advancementssearch$highlightAdvancement(Identifier advancementId, HighlightType highlightType) {
        for (PlacedAdvancement advancement : getAdvancements(false)) {
            if (advancementId.equals(advancement.getAdvancementEntry().id())) {
                highlight(advancement, highlightType);
                break;
            }
        }
    }

    @Override
    public void advancementssearch$tick() {
        if (widgetHighlightCounter > 0) {
            widgetHighlightCounter--;
            if (widgetHighlightCounter == 0) {
                advancementssearch$stopHighlight();
            }
        }
    }

    @Override
    public boolean advancementssearch$charTyped(char chr, int modifiers) {
        if (searchField != null) {
            String oldText = searchField.getText();
            if (searchField.charTyped(chr, modifiers)) {
                if (!Objects.equals(oldText, searchField.getText())) {
                    searchByUser();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void advancementssearch$resize(MinecraftClient client, int width, int height) {
        if (searchField != null) {
            String oldText = searchField.getText();
            init(client, width, height);
            searchField.setText(oldText);
        }
    }

    @Override
    public void advancementssearch$onMouseReleased(double mouseX, double mouseY, int button) {
        if (isFocusedAdvancementClicked &&
            focusedAdvancementWidget != null &&
            focusedAdvancementWidget.tab == searchTab &&
            button == GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) {
            Identifier focusedAdvancementId = focusedAdvancementWidget.advancement.getAdvancementEntry().id();
            for (PlacedAdvancement advancement : getAdvancements(true)) {
                if (advancement.getAdvancementEntry().id().equals(focusedAdvancementId)) {
                    highlight(advancement, HighlightType.WIDGET);
                    break;
                }
            }
        }
    }

    @Unique
    private @NotNull ArrayList<PlacedAdvancement> getAdvancements(boolean shouldExcludeRoots) {
        ArrayList<PlacedAdvancement> advancements = new ArrayList<>();
        AdvancementManager advancementManager = advancementHandler.getManager();
        Map<AdvancementEntry, AdvancementProgress> progresses = advancementHandler.advancementProgresses;
        for (AdvancementEntry advancementEntry : new ArrayList<>(progresses.keySet())) {
            if (advancementEntry == null) {
                continue;
            }
            Advancement advancement = advancementEntry.value();
            if (shouldExcludeRoots && advancement.isRoot()) {
                continue;
            }
            AdvancementDisplay display = advancementEntry.value().display().orElse(null);
            if (display == null) {
                continue;
            }
            if (display.isHidden()) {
                AdvancementProgress progress = progresses.get(advancementEntry);
                if (progress == null || !progress.isDone()) {
                    continue;
                }
            }
            PlacedAdvancement placedAdvancement = advancementManager.get(advancementEntry);
            if (placedAdvancement == null) {
                continue;
            }
            PlacedAdvancement rootAdvancement = placedAdvancement.getRoot();
            if (rootAdvancement == null) {
                continue;
            }
            advancements.add(placedAdvancement);
        }
        return advancements;
    }

    @Unique
    private void searchByUser() {
        if (searchField == null) {
            return;
        }
        String query = searchField.getText();
        isSearchActive = !query.isEmpty();
        searchInternal(SearchByType.getQueryWithoutMask(query), SearchByType.findByMask(query));
        showSearchResults();
    }

    @Unique
    private void searchInternal(String query, SearchByType searchByType) {
        query = query.toLowerCase(Locale.ROOT);
        searchResults.clear();
        if (query.trim().isEmpty()) {
            return;
        }
        boolean checkEverywhere = searchByType == SearchByType.EVERYWHERE;
        for (PlacedAdvancement placedAdvancement : getAdvancements(true)) {
            AdvancementDisplay display = placedAdvancement.getAdvancement().display().orElse(null);
            if (display == null) {
                continue;
            }
            String title = display.getTitle().getString().toLowerCase(Locale.ROOT);
            String description = display.getDescription().getString().toLowerCase(Locale.ROOT);
            String iconName = display.getIcon().getItem().getName().getString().toLowerCase(Locale.ROOT);

            if ((checkEverywhere || searchByType == SearchByType.TITLE) && title.contains(query) ||
                (checkEverywhere || searchByType == SearchByType.DESCRIPTION) && description.contains(query) ||
                (checkEverywhere || searchByType == SearchByType.ICON) && iconName.contains(query)
            ) {
                searchResults.add(placedAdvancement);
            }
        }
        searchResults.sort(Comparator.comparing((advancement) -> advancement.getAdvancementEntry().id()));

        List<AdvancementFrame> frameOrder = Arrays.asList(
            AdvancementFrame.TASK,
            AdvancementFrame.GOAL,
            AdvancementFrame.CHALLENGE
        );
        searchResults.sort((advancement, nextAdvancement) -> {
            AdvancementDisplay display = advancement.getAdvancement().display().orElse(null);
            AdvancementDisplay nextDisplay = nextAdvancement.getAdvancement().display().orElse(null);
            if (display == null || nextDisplay == null) {
                return 0;
            }
            int frameIndex = frameOrder.indexOf(display.getFrame());
            int nextFrameIndex = frameOrder.indexOf(nextDisplay.getFrame());
            return Integer.compare(frameIndex, nextFrameIndex);
        });
    }

    @Unique
    private void showSearchResults() {
        if (searchTab == null) {
            return;
        }
        resetSearchTab();
        if (searchResults.isEmpty()) {
            return;
        }
        searchTab.addWidget(searchTab.rootWidget, searchRootAdvancement.getAdvancementEntry());

        int rowIndex = 0;
        int columnIndex = 0;
        Map<AdvancementEntry, AdvancementProgress> progresses = advancementHandler.advancementProgresses;
        PlacedAdvancement rootAdvancement = new PlacedAdvancement(searchRootAdvancement.getAdvancementEntry(), null);
        PlacedAdvancement parentPlacedAdvancement = rootAdvancement;
        for (PlacedAdvancement searchResult : searchResults) {
            AdvancementDisplay searchResultDisplay = searchResult.getAdvancement().display().orElse(null);
            if (searchResultDisplay == null) {
                continue;
            }
            AdvancementDisplay searchResultAdvancementDisplay = new AdvancementDisplay(
                searchResultDisplay.getIcon(),
                searchResultDisplay.getTitle(),
                searchResultDisplay.getDescription(),
                searchResultDisplay.getBackground(),
                searchResultDisplay.getFrame(),
                searchResultDisplay.shouldShowToast(),
                searchResultDisplay.shouldAnnounceToChat(),
                searchResultDisplay.isHidden()
            );
            searchResultAdvancementDisplay.setPos(columnIndex, rowIndex);

            Advancement.Builder searchResultAdvancementBuilder = Advancement.Builder.create()
                .parent(parentPlacedAdvancement.getAdvancementEntry())
                .display(searchResultAdvancementDisplay)
                .rewards(searchResult.getAdvancement().rewards())
                .requirements(searchResult.getAdvancement().requirements());
            searchResult.getAdvancement().criteria().forEach(searchResultAdvancementBuilder::criterion);
            if (searchResult.getAdvancement().sendsTelemetryEvent()) {
                searchResultAdvancementBuilder = searchResultAdvancementBuilder.sendsTelemetryEvent();
            }
            AdvancementEntry searchResultAdvancementEntry =
                searchResultAdvancementBuilder.build(searchResult.getAdvancementEntry().id());
            PlacedAdvancement searchResultPlacedAdvancement =
                new PlacedAdvancement(searchResultAdvancementEntry, parentPlacedAdvancement);

            searchTab.addAdvancement(searchResultPlacedAdvancement);
            searchTab.widgets.get(searchResultAdvancementEntry)
                .setProgress(progresses.get(searchResultAdvancementEntry));
            if (columnIndex == searchResultsColumnsCount - 1) {
                parentPlacedAdvancement = rootAdvancement;
                columnIndex = 0;
                rowIndex++;
            } else {
                parentPlacedAdvancement = new PlacedAdvancement(
                    searchResultAdvancementEntry,
                    searchResultPlacedAdvancement
                );
                columnIndex++;
            }
        }
    }

    @Unique
    private void resetSearchTab() {
        if (searchTab == null) {
            return;
        }
        searchTab.minPanX = Integer.MAX_VALUE;
        searchTab.minPanY = Integer.MAX_VALUE;
        searchTab.maxPanX = Integer.MIN_VALUE;
        searchTab.maxPanY = Integer.MIN_VALUE;
        searchTab.originX = searchResultsOriginX;
        searchTab.originY = 0;
        searchTab.initialized = true;
        for (AdvancementWidget widget : searchTab.widgets.values()) {
            widget.parent = null;
            widget.children.clear();
        }
        searchTab.widgets.clear();
    }

    @Unique
    private void highlight(@NotNull PlacedAdvancement advancement, HighlightType type) {
        if (highlightedAdvancement != null) {
            return;
        }
        isSearchActive = false;
        highlightedAdvancement = advancement;
        highlightType = type;
        advancementHandler.selectTab(advancement.getRoot().getAdvancementEntry(), true);
    }

    @Shadow
    @Final
    private ClientAdvancementManager advancementHandler;

    @Shadow
    private @Nullable AdvancementTab selectedTab;

    @Inject(
        method = "drawAdvancementTree",
        at = @At("TAIL")
    )
    private void startHighlight(DrawContext context, int x, int y, CallbackInfo ci) {
        if (highlightedAdvancement == null || selectedTab == null) {
            return;
        }
        for (AdvancementWidget widget : selectedTab.widgets.values()) {
            if (widget != null && widget.advancement == highlightedAdvancement) {
                int centerX = (WIDGET_SIZE - advancementssearch$getTreeWidth()) / 2;
                int centerY = (WIDGET_SIZE - advancementssearch$getTreeHeight()) / 2;
                selectedTab.move(
                    -(selectedTab.originX + widget.getX() + TREE_X_OFFSET + centerX),
                    -(selectedTab.originY + widget.getY() + centerY)
                );
                highlightedAdvancement = null;
                highlightedAdvancementId = widget.advancement.getAdvancementEntry().id();
                widgetHighlightCounter = WIDGET_HIGHLIGHT_COUNT * 2 * WIDGET_HIGHLIGHT_TICKS;
                break;
            }
        }
    }

    @Redirect(
        method = "mouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientAdvancementManager;selectTab(Lnet/minecraft/advancement/AdvancementEntry;Z)V"
        )
    )
    private void mouseClickedRedirect(
        @NotNull ClientAdvancementManager advancementHandler,
        AdvancementEntry tab,
        boolean local
    ) {
        isSearchActive = false;
        advancementssearch$stopHighlight();
        advancementHandler.selectTab(tab, true);
    }

    @Redirect(
        method = "drawAdvancementTree",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;",
            opcode = Opcodes.GETFIELD
        )
    )
    private @Nullable AdvancementTab drawAdvancementTreeInject(AdvancementsScreen screen) {
        return !isSearchActive ? selectedTab : searchTab.widgets.size() > 1 ? searchTab : null;
    }

    @ModifyArgs(
        method = "drawWindow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;drawBackground(Lnet/minecraft/client/gui/DrawContext;IIZ)V"
        )
    )
    private void drawWindowModifyTabSelected(Args args) {
        if (isSearchActive) {
            args.set(3, false);
        }
    }

    @Redirect(
        method = "drawWindow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"
        )
    )
    private void modifyWindowTitleRender(
        DrawContext context,
        TextRenderer textRenderer,
        Text text,
        int x,
        int y,
        int color,
        boolean shadow
    ) {
        if (isSearchActive) {
            text = SEARCH_TITLE;
        }
        int rightEdgeX = x + treeWidth - SEARCH_FIELD_WIDTH - 3;
        int textWidth = textRenderer.getWidth(text);
        int availableWidth = rightEdgeX - x;

        if (textWidth > availableWidth) {
            int bottomY = y + textRenderer.fontHeight;

            int excessWidth = textWidth - availableWidth;
            double timeInSeconds = Util.getMeasuringTimeMs() / 1000.0;
            double adjustmentFactor = Math.max((double) excessWidth * 0.5, 3);
            double oscillation =
                Math.sin(Math.PI / 2 * Math.cos(Math.PI * 2 * timeInSeconds / adjustmentFactor)) / 2 + 0.5;
            double offset = MathHelper.lerp(oscillation, 0, excessWidth);

            context.enableScissor(x, y, rightEdgeX, bottomY);
            context.drawText(textRenderer, text, x - (int) offset, y, color, shadow);
            context.disableScissor();
        } else {
            context.drawText(textRenderer, text, x, y, color, shadow);
        }
    }

    @Redirect(
        method = "drawWidgetTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;drawWidgetTooltip(Lnet/minecraft/client/gui/DrawContext;IIII)V"
        )
    )
    private void drawWidgetTooltipRedirectTab(
        AdvancementTab selectedTab,
        DrawContext context,
        int mouseX,
        int mouseY,
        int x,
        int y
    ) {
        if (isSearchActive) {
            selectedTab = searchTab;
        }
        selectedTab.drawWidgetTooltip(context, mouseX, mouseY, x, y);
    }

    @Redirect(
        method = "mouseScrolled",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;",
            opcode = Opcodes.GETFIELD
        )
    )
    private AdvancementTab mouseScrolledRedirect(AdvancementsScreen screen) {
        return isSearchActive ? searchTab : selectedTab;
    }

    @Redirect(
        method = "mouseDragged",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;",
            opcode = Opcodes.GETFIELD
        )
    )
    private AdvancementTab mouseDraggedRedirect(AdvancementsScreen screen) {
        return isSearchActive ? searchTab : selectedTab;
    }

    @Inject(
        method = "init",
        at = @At(value = "TAIL")
    )
    public void initInject(CallbackInfo ci) {
        searchField = new TextFieldWidget(
            textRenderer,
            0,
            0,
            SEARCH_FIELD_WIDTH - SEARCH_FIELD_TEXT_LEFT_OFFSET - 8,
            textRenderer.fontHeight,
            ScreenTexts.EMPTY
        );
        searchField.setDrawsBackground(false);
        searchField.setEditableColor(Colors.WHITE);
        searchField.setFocusUnlocked(false);
        addSelectableChild(searchField);
        setInitialFocus(searchField);

        if (searchTab == null) {
            AdvancementDisplay searchRootAdvancementDisplay = new AdvancementDisplay(
                ItemStack.EMPTY,
                Text.empty(),
                Text.empty(),
                Optional.empty(),
                AdvancementFrame.TASK,
                false,
                false,
                true
            );
            searchRootAdvancement = new PlacedAdvancement(
                Advancement.Builder
                    .createUntelemetered()
                    .display(searchRootAdvancementDisplay)
                    .build(AdvancementsSearchMod.ADVANCEMENTS_SEARCH_ID),
                null
            );
            AdvancementsScreen advancementsScreen = (AdvancementsScreen) (Object) this;
            if (client != null) {
                searchTab = new AdvancementTab(
                    client,
                    advancementsScreen,
                    null,
                    0,
                    searchRootAdvancement,
                    searchRootAdvancementDisplay
                );
            }
        }
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;drawAdvancementTree(Lnet/minecraft/client/gui/DrawContext;II)V"
        )
    )
    public void getWindowSizes(
        AdvancementsScreen screen,
        DrawContext context,
        int x,
        int y,
        Operation<Void> original
    ) {
        windowX = x;
        windowY = y;
        treeWidth = Math.abs(windowX * 2 - width) - WINDOW_BORDER_SIZE - WINDOW_BORDER_SIZE;
        treeHeight = Math.abs(windowY * 2 - height) - WINDOW_HEADER_HEIGHT - WINDOW_BORDER_SIZE;
        original.call(screen, context, x, y);
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;drawWindow(Lnet/minecraft/client/gui/DrawContext;II)V",
            shift = At.Shift.AFTER
        )
    )
    public void renderInject(
        DrawContext context,
        int mouseX,
        int mouseY,
        float delta,
        CallbackInfo ci
    ) {
        if (searchField != null) {
            int frameOffset = 1;
            int frameContainerWidth = frameOffset + WIDGET_SIZE + frameOffset;
            int columnsCount = treeWidth / frameContainerWidth;
            int rowWidth = frameContainerWidth * columnsCount;
            int horizontalOffset = treeWidth - rowWidth - TREE_X_OFFSET;
            int originX = horizontalOffset / 2;
            if (searchResultsColumnsCount != columnsCount || searchResultsOriginX != originX) {
                searchResultsColumnsCount = columnsCount;
                searchResultsOriginX = originX;
                if (isSearchActive) {
                    showSearchResults();
                }
            }

            int symmetryFixX = 1;
            int fieldX = windowX + treeWidth + WINDOW_BORDER_SIZE - SEARCH_FIELD_WIDTH + symmetryFixX;
            int fieldY = windowY + 4;

            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                CREATIVE_INVENTORY_TEXTURE,
                fieldX,
                fieldY,
                SEARCH_FIELD_UV.x,
                SEARCH_FIELD_UV.y,
                SEARCH_FIELD_WIDTH,
                SEARCH_FIELD_HEIGHT,
                256,
                256
            );

            searchField.setX(fieldX + SEARCH_FIELD_TEXT_LEFT_OFFSET);
            searchField.setY(fieldY + SEARCH_FIELD_TEXT_LEFT_OFFSET);
            searchField.render(context, mouseX, mouseY, delta);
        }
    }

    @Inject(
        method = "keyPressed",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void keyPressedInject(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (searchField != null) {
            String oldText = searchField.getText();
            if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
                if (!Objects.equals(oldText, searchField.getText())) {
                    searchByUser();
                }
                cir.setReturnValue(true);
            }
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(
        method = "mouseClicked",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void mouseClickedInject(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (searchField != null && searchField.mouseClicked(mouseX, mouseY, button)) {
            isSearchActive = !searchField.getText().isEmpty();
            cir.setReturnValue(true);
        }
        isFocusedAdvancementClicked = focusedAdvancementWidget != null &&
            focusedAdvancementWidget.tab == searchTab &&
            button == MouseEvent.NOBUTTON;
    }

    @Inject(
        method = "mouseScrolled",
        at = @At(value = "HEAD")
    )
    private void resetFocusedAdvancementOnScroll(
        double mouseX,
        double mouseY,
        double horizontalAmount,
        double verticalAmount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        isFocusedAdvancementClicked = false;
    }

    @Inject(
        method = "mouseDragged",
        at = @At(value = "HEAD")
    )
    private void resetFocusedAdvancementOnDrag(
        double mouseX,
        double mouseY,
        int button,
        double deltaX,
        double deltaY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        isFocusedAdvancementClicked = false;
    }
}
