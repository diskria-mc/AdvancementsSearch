package com.diskria.advancementssearch.injection.mixin;

import com.diskria.advancementssearch.AdvancementsSearchMod;
import com.diskria.advancementssearch.HighlightType;
import com.diskria.advancementssearch.SearchByType;
import com.diskria.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancements.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
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
import java.util.*;
import java.util.List;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen implements AdvancementsScreenExtension {

    @Unique
    private static final Identifier CREATIVE_INVENTORY_TEXTURE =
        Identifier.withDefaultNamespace("textures/gui/container/creative_inventory/tab_item_search.png");

    @Unique
    private static final Component SEARCH_TITLE = Component.translatable("gui.recipebook.search_hint");

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
    private EditBox searchBox;

    @Unique
    private AdvancementNode searchRootAdvancement;

    @Unique
    private AdvancementTab searchTab;

    @Unique
    private final ArrayList<AdvancementNode> searchResults = new ArrayList<>();

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
    private AdvancementNode highlightedAdvancement;

    @Unique
    private Identifier highlightedAdvancementId;

    @Unique
    private HighlightType highlightType;

    @Unique
    private int widgetHighlightCounter;

    @Unique
    private boolean isFocusedAdvancementClicked;

    protected AdvancementsScreenMixin(Component title) {
        super(title);
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
        searchBox.setValue(query);
        isSearchActive = !query.isEmpty();
        showSearchResults();
    }

    @Override
    public void advancementssearch$highlightAdvancement(Identifier advancementId, HighlightType highlightType) {
        for (AdvancementNode advancement : getAdvancements(false)) {
            if (advancementId.equals(advancement.holder().id())) {
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
    public boolean advancementssearch$charTyped(CharacterEvent characterEvent) {
        if (searchBox != null) {
            String oldText = searchBox.getValue();
            if (searchBox.charTyped(characterEvent)) {
                if (!Objects.equals(oldText, searchBox.getValue())) {
                    searchByUser();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void advancementssearch$resize(int width, int height) {
        if (searchBox != null) {
            String oldText = searchBox.getValue();
            init(width, height);
            searchBox.setValue(oldText);
        }
    }

    @Override
    public void advancementssearch$onMouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (isFocusedAdvancementClicked &&
            focusedAdvancementWidget != null &&
            focusedAdvancementWidget.tab == searchTab &&
            mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) {
            Identifier focusedAdvancementId = focusedAdvancementWidget.advancementNode.holder().id();
            for (AdvancementNode advancement : getAdvancements(true)) {
                if (advancement.holder().id().equals(focusedAdvancementId)) {
                    highlight(advancement, HighlightType.OBTAINED_STATUS);
                    break;
                }
            }
        }
    }

    @Unique
    private @NotNull ArrayList<AdvancementNode> getAdvancements(boolean shouldExcludeRoots) {
        ArrayList<AdvancementNode> advancements = new ArrayList<>();
        AdvancementTree advancementManager = this.advancements.getTree();
        Map<AdvancementHolder, AdvancementProgress> progresses = this.advancements.progress;
        for (AdvancementHolder advancementEntry : new ArrayList<>(progresses.keySet())) {
            if (advancementEntry == null) {
                continue;
            }
            Advancement advancement = advancementEntry.value();
            if (shouldExcludeRoots && advancement.isRoot()) {
                continue;
            }
            DisplayInfo display = advancementEntry.value().display().orElse(null);
            if (display == null) {
                continue;
            }
            if (display.isHidden()) {
                AdvancementProgress progress = progresses.get(advancementEntry);
                if (progress == null || !progress.isDone()) {
                    continue;
                }
            }
            AdvancementNode advancementNode = advancementManager.get(advancementEntry);
            if (advancementNode == null) {
                continue;
            }
            advancements.add(advancementNode);
        }
        return advancements;
    }

    @Unique
    private void searchByUser() {
        if (searchBox == null) {
            return;
        }
        String query = searchBox.getValue();
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
        for (AdvancementNode advancementNode : getAdvancements(true)) {
            DisplayInfo display = advancementNode.advancement().display().orElse(null);
            if (display == null) {
                continue;
            }
            String title = display.getTitle().getString().toLowerCase(Locale.ROOT);
            String description = display.getDescription().getString().toLowerCase(Locale.ROOT);
            String iconName = display.getIcon().item().getRegisteredName().toLowerCase(Locale.ROOT);

            if ((checkEverywhere || searchByType == SearchByType.TITLE) && title.contains(query) ||
                (checkEverywhere || searchByType == SearchByType.DESCRIPTION) && description.contains(query) ||
                (checkEverywhere || searchByType == SearchByType.ICON) && iconName.contains(query)
            ) {
                searchResults.add(advancementNode);
            }
        }
        searchResults.sort(Comparator.comparing((advancement) -> advancement.holder().id()));

        List<AdvancementType> frameOrder = Arrays.asList(
            AdvancementType.TASK,
            AdvancementType.GOAL,
            AdvancementType.CHALLENGE
        );
        searchResults.sort((prevNode, nextNode) -> {
            DisplayInfo display = prevNode.advancement().display().orElse(null);
            DisplayInfo nextDisplay = nextNode.advancement().display().orElse(null);
            if (display == null || nextDisplay == null) {
                return 0;
            }
            int frameIndex = frameOrder.indexOf(display.getType());
            int nextFrameIndex = frameOrder.indexOf(nextDisplay.getType());
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
        searchTab.addWidget(searchTab.root, searchRootAdvancement.holder());

        int rowIndex = 0;
        int columnIndex = 0;
        Map<AdvancementHolder, AdvancementProgress> progresses = advancements.progress;
        AdvancementNode rootAdvancement = new AdvancementNode(searchRootAdvancement.holder(), null);
        AdvancementNode parentAdvancementNode = rootAdvancement;
        for (AdvancementNode searchResult : searchResults) {
            DisplayInfo searchResultDisplay = searchResult.advancement().display().orElse(null);
            if (searchResultDisplay == null) {
                continue;
            }
            DisplayInfo searchResultAdvancementDisplay = new DisplayInfo(
                searchResultDisplay.getIcon(),
                searchResultDisplay.getTitle(),
                searchResultDisplay.getDescription(),
                searchResultDisplay.getBackground(),
                searchResultDisplay.getType(),
                searchResultDisplay.shouldShowToast(),
                searchResultDisplay.shouldAnnounceChat(),
                searchResultDisplay.isHidden()
            );
            searchResultAdvancementDisplay.setLocation(columnIndex, rowIndex);

            Advancement.Builder searchResultAdvancementBuilder = Advancement.Builder.advancement()
                .parent(parentAdvancementNode.holder())
                .display(searchResultAdvancementDisplay)
                .rewards(searchResult.advancement().rewards())
                .requirements(searchResult.advancement().requirements());
            searchResult.advancement().criteria().forEach(searchResultAdvancementBuilder::addCriterion);
            if (searchResult.advancement().sendsTelemetryEvent()) {
                searchResultAdvancementBuilder = searchResultAdvancementBuilder.sendsTelemetryEvent();
            }
            AdvancementHolder searchResultAdvancementEntry =
                searchResultAdvancementBuilder.build(searchResult.holder().id());
            AdvancementNode searchResultAdvancementNode =
                new AdvancementNode(searchResultAdvancementEntry, parentAdvancementNode);

            searchTab.addAdvancement(searchResultAdvancementNode);
            searchTab.widgets.get(searchResultAdvancementEntry)
                .setProgress(progresses.get(searchResultAdvancementEntry));
            if (columnIndex == searchResultsColumnsCount - 1) {
                parentAdvancementNode = rootAdvancement;
                columnIndex = 0;
                rowIndex++;
            } else {
                parentAdvancementNode = new AdvancementNode(
                    searchResultAdvancementEntry,
                    searchResultAdvancementNode
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
        searchTab.minX = Integer.MAX_VALUE;
        searchTab.minY = Integer.MAX_VALUE;
        searchTab.maxX = Integer.MIN_VALUE;
        searchTab.maxY = Integer.MIN_VALUE;
        searchTab.scrollX = searchResultsOriginX;
        searchTab.scrollY = 0;
        searchTab.centered = true;
        for (AdvancementWidget widget : searchTab.widgets.values()) {
            widget.parent = null;
            widget.children.clear();
        }
        searchTab.widgets.clear();
    }

    @Unique
    private void highlight(@NotNull AdvancementNode advancement, HighlightType type) {
        if (highlightedAdvancement != null) {
            return;
        }
        isSearchActive = false;
        highlightedAdvancement = advancement;
        highlightType = type;
        advancements.setSelectedTab(advancement.root().holder(), true);
    }

    @Shadow
    @Final
    private ClientAdvancements advancements;

    @Shadow
    private @Nullable AdvancementTab selectedTab;

    @Inject(
        method = "extractInside",
        at = @At("TAIL")
    )
    private void startHighlight(GuiGraphicsExtractor graphics, int xo, int yo, CallbackInfo ci) {
        if (highlightedAdvancement == null || selectedTab == null) {
            return;
        }
        for (AdvancementWidget widget : selectedTab.widgets.values()) {
            if (widget.advancementNode == highlightedAdvancement) {
                int centerX = (WIDGET_SIZE - advancementssearch$getTreeWidth()) / 2;
                int centerY = (WIDGET_SIZE - advancementssearch$getTreeHeight()) / 2;
                selectedTab.scroll(
                    -(selectedTab.scrollX + widget.getX() + TREE_X_OFFSET + centerX),
                    -(selectedTab.scrollY + widget.getY() + centerY)
                );
                highlightedAdvancement = null;
                highlightedAdvancementId = widget.advancementNode.holder().id();
                widgetHighlightCounter = WIDGET_HIGHLIGHT_COUNT * 2 * WIDGET_HIGHLIGHT_TICKS;
                break;
            }
        }
    }

    @Redirect(
        method = "mouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientAdvancements;setSelectedTab(Lnet/minecraft/advancements/AdvancementHolder;Z)V"
        )
    )
    private void mouseClickedRedirect(
        ClientAdvancements clientAdvancements, AdvancementHolder selectedTab, boolean tellServer
    ) {
        isSearchActive = false;
        advancementssearch$stopHighlight();
        clientAdvancements.setSelectedTab(selectedTab, true);
    }

    @Redirect(
        method = "extractInside",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
            opcode = Opcodes.GETFIELD
        )
    )
    private @Nullable AdvancementTab drawAdvancementTreeInject(AdvancementsScreen screen) {
        return !isSearchActive ? selectedTab : searchTab.widgets.size() > 1 ? searchTab : null;
    }

    @ModifyArgs(
        method = "extractWindow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;extractTab(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIZ)V"
        )
    )
    private void drawWindowModifyTabSelected(Args args) {
        if (isSearchActive) {
            args.set(5, false);
        }
    }

    @Redirect(
        method = "extractWindow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
        )
    )
    private void modifyWindowTitleRender(
        GuiGraphicsExtractor graphics, Font font, Component str, int x, int y, int color, boolean dropShadow
    ) {
        if (isSearchActive) {
            str = SEARCH_TITLE;
        }
        int rightEdgeX = x + treeWidth - SEARCH_FIELD_WIDTH - 3;
        int textWidth = font.width(str);
        int availableWidth = rightEdgeX - x;

        if (textWidth > availableWidth) {
            int bottomY = y + font.lineHeight;
            int excessWidth = textWidth - availableWidth;
            double time = Util.getMillis() / 1000.0;
            double period = Math.max((double) excessWidth * 0.5, 3);
            double alpha = Math.sin(Math.PI / 2 * Math.cos(Math.PI * 2 * time / period)) / 2 + 0.5;
            double pos = Mth.lerp(alpha, 0, excessWidth);

            graphics.enableScissor(x, y, rightEdgeX, bottomY);
            graphics.text(font, str, x - (int) pos, y, color, dropShadow);
            graphics.disableScissor();
        } else {
            graphics.text(font, str, x, y, color, dropShadow);
        }
    }

    @ModifyReceiver(
        method = "extractTooltips",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;extractTooltips(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V"
        )
    )
    private AdvancementTab drawWidgetTooltipRedirectTab(
        AdvancementTab original,
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        int x,
        int y
    ) {
        return isSearchActive ? searchTab : original;
    }

    @Redirect(
        method = "mouseScrolled",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
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
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
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
        searchBox = new EditBox(
            font,
            0,
            0,
            SEARCH_FIELD_WIDTH - SEARCH_FIELD_TEXT_LEFT_OFFSET - 8,
            font.lineHeight,
            Component.translatable("itemGroup.search")
        );
        searchBox.setMaxLength(50);
        searchBox.setBordered(false);
        searchBox.setTextColor(-1);
        searchBox.setCanLoseFocus(false);
        addWidget(searchBox);
        setInitialFocus(searchBox);

        if (searchTab == null) {
            DisplayInfo searchRootAdvancementDisplay = new DisplayInfo(
                ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.BARRIER)),
                Component.empty(),
                Component.empty(),
                Optional.empty(),
                AdvancementType.TASK,
                false,
                false,
                true
            );
            searchRootAdvancement = new AdvancementNode(
                Advancement.Builder
                    .recipeAdvancement()
                    .display(searchRootAdvancementDisplay)
                    .build(AdvancementsSearchMod.ADVANCEMENTS_SEARCH_ID),
                null
            );
            AdvancementsScreen advancementsScreen = (AdvancementsScreen) (Object) this;
            if (minecraft != null) {
                searchTab = new AdvancementTab(
                    minecraft,
                    advancementsScreen,
                    AdvancementTabType.ABOVE,
                    0,
                    searchRootAdvancement,
                    searchRootAdvancementDisplay
                );
            }
        }
    }

    @WrapOperation(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"
        )
    )
    public void getWindowSizes(
        AdvancementsScreen screen,
        GuiGraphicsExtractor context,
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
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;extractWindow(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V",
            shift = At.Shift.AFTER
        )
    )
    public void renderInject(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (searchBox == null) {
            return;
        }
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

        graphics.blit(
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

        searchBox.setX(fieldX + SEARCH_FIELD_TEXT_LEFT_OFFSET);
        searchBox.setY(fieldY + SEARCH_FIELD_TEXT_LEFT_OFFSET);
        searchBox.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Inject(
        method = "keyPressed",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void keyPressedInject(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (searchBox != null) {
            String oldText = searchBox.getValue();
            if (searchBox.keyPressed(event)) {
                if (!Objects.equals(oldText, searchBox.getValue())) {
                    searchByUser();
                }
                cir.setReturnValue(true);
            }
            if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(
        method = "mouseClicked",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void mouseClickedInject(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (searchBox != null && searchBox.mouseClicked(event, doubleClick)) {
            isSearchActive = !searchBox.getValue().isEmpty();
            cir.setReturnValue(true);
        }
        isFocusedAdvancementClicked = focusedAdvancementWidget != null &&
            focusedAdvancementWidget.tab == searchTab &&
            event.button() == MouseEvent.NOBUTTON;
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
        MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir
    ) {
        isFocusedAdvancementClicked = false;
    }
}
