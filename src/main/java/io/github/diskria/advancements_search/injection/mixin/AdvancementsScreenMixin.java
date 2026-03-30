package io.github.diskria.advancements_search.injection.mixin;

import io.github.diskria.advancements_search.AdvancementsSearchMod;
import io.github.diskria.advancements_search.injection.extension.AdvancementsScreenExtension;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    private static final int FLASHING_COUNT = 5;

    @Unique
    private static final int FLASHING_TICKS_INTERVAL = 3;

    @Unique
    private static final int SEARCH_FIELD_TEXT_LEFT_OFFSET = 2;

    @Unique
    private static final List<AdvancementType> TYPE_PRIORITY = Arrays.asList(
        AdvancementType.TASK,
        AdvancementType.GOAL,
        AdvancementType.CHALLENGE
    );

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
    private int treeWidth;

    @Unique
    private int treeHeight;

    @Unique
    private Identifier flashingAdvancementId;

    @Unique
    private int flashingTickCounter;

    @Unique
    private boolean isFocusedAdvancementClicked;

    protected AdvancementsScreenMixin(Component title) {
        super(title);
    }

    @Override
    public void advancements_search$setFocusedAdvancementWidget(AdvancementWidget widget) {
        focusedAdvancementWidget = widget;
    }

    @Override
    public boolean advancements_search$isSearchActive() {
        return isSearchActive;
    }

    @Override
    public int advancements_search$getTreeWidth() {
        return treeWidth;
    }

    @Override
    public int advancements_search$getTreeHeight() {
        return treeHeight;
    }

    @Override
    public Identifier advancements_search$getFlashingAdvancementId() {
        return flashingAdvancementId;
    }

    @Override
    public boolean advancements_search$isFlashingAtInvisibleState() {
        return flashingTickCounter != 0 && (flashingTickCounter / FLASHING_TICKS_INTERVAL) % 2 == 0;
    }

    @Override
    public void advancements_search$stopFlashing() {
        flashingAdvancementId = null;
        flashingTickCounter = 0;
    }

    @Override
    public void advancements_search$tick() {
        if (flashingTickCounter > 0) {
            flashingTickCounter--;
            if (flashingTickCounter == 0) {
                advancements_search$stopFlashing();
            }
        }
    }

    @Override
    public boolean advancements_search$charTyped(CharacterEvent characterEvent) {
        if (searchBox != null) {
            String oldText = searchBox.getValue();
            if (searchBox.charTyped(characterEvent)) {
                if (!Objects.equals(oldText, searchBox.getValue())) {
                    processSearch();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void advancements_search$resize(int width, int height) {
        if (searchBox != null) {
            String oldText = searchBox.getValue();
            init(width, height);
            searchBox.setValue(oldText);
        }
    }

    @Override
    public void advancements_search$onMouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (isFocusedAdvancementClicked &&
            focusedAdvancementWidget != null &&
            focusedAdvancementWidget.tab == searchTab &&
            mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) {
            Identifier focusedAdvancementId = focusedAdvancementWidget.advancementNode.holder().id();
            for (AdvancementNode advancement : searchResults) {
                if (advancement.holder().id().equals(focusedAdvancementId)) {
                    isSearchActive = false;
                    searchTab.hovered = null;
                    searchTab.fade = 0.0f;
                    flashingAdvancementId = advancement.holder().id();
                    advancements.setSelectedTab(advancement.root().holder(), true);
                    break;
                }
            }
        }
    }

    @Unique
    private @NotNull ArrayList<AdvancementNode> getAdvancements() {
        ArrayList<AdvancementNode> results = new ArrayList<>();
        for (AdvancementHolder advancementEntry : new ArrayList<>(advancements.progress.keySet())) {
            Advancement advancement = advancementEntry.value();
            if (advancement.isRoot()) {
                continue;
            }
            DisplayInfo display = advancementEntry.value().display().orElse(null);
            if (display == null) {
                continue;
            }
            if (display.isHidden()) {
                AdvancementProgress progress = advancements.progress.get(advancementEntry);
                if (progress == null || !progress.isDone()) {
                    continue;
                }
            }
            AdvancementNode advancementNode = advancements.getTree().get(advancementEntry);
            if (advancementNode == null) {
                continue;
            }
            results.add(advancementNode);
        }
        return results;
    }

    @Unique
    private void processSearch() {
        if (searchBox == null) {
            return;
        }
        String query = searchBox.getValue().toLowerCase(Locale.ROOT);
        isSearchActive = !query.isEmpty();
        searchResults.clear();
        if (query.trim().isEmpty()) {
            return;
        }
        for (AdvancementNode advancementNode : getAdvancements()) {
            DisplayInfo display = advancementNode.advancement().display().orElse(null);
            if (display == null) {
                continue;
            }
            String title = display.getTitle().getString().toLowerCase(Locale.ROOT);
            String description = display.getDescription().getString().toLowerCase(Locale.ROOT);
            String iconName = display.getIcon().item().getRegisteredName().toLowerCase(Locale.ROOT);

            if (title.contains(query) || description.contains(query) || iconName.contains(query)) {
                searchResults.add(advancementNode);
            }
        }
        searchResults.sort(Comparator.comparing((advancement) -> advancement.holder().id()));

        searchResults.sort((node, nextNode) -> {
            DisplayInfo display = node.advancement().display().orElse(null);
            DisplayInfo nextDisplay = nextNode.advancement().display().orElse(null);
            if (display == null || nextDisplay == null) {
                return 0;
            }
            int typeIndex = TYPE_PRIORITY.indexOf(display.getType());
            int nextTypeIndex = TYPE_PRIORITY.indexOf(nextDisplay.getType());
            return Integer.compare(typeIndex, nextTypeIndex);
        });
        showSearchResults();
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
        searchTab.hovered = null;
        searchTab.fade = 0.0f;
    }

    @Shadow
    @Final
    private ClientAdvancements advancements;

    @Shadow
    private @Nullable AdvancementTab selectedTab;

    @Shadow
    private int leftPos;

    @Shadow
    private int topPos;

    @Inject(
        method = "extractInside",
        at = @At("TAIL")
    )
    private void startFlashing(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (flashingAdvancementId == null || flashingTickCounter != 0 || selectedTab == null) {
            return;
        }
        for (AdvancementWidget widget : selectedTab.widgets.values()) {
            if (widget.advancementNode.holder().id().equals(flashingAdvancementId)) {
                int centerX = (WIDGET_SIZE - advancements_search$getTreeWidth()) / 2;
                int centerY = (WIDGET_SIZE - advancements_search$getTreeHeight()) / 2;
                selectedTab.scroll(
                    -(selectedTab.scrollX + widget.getX() + TREE_X_OFFSET + centerX),
                    -(selectedTab.scrollY + widget.getY() + centerY)
                );
                flashingTickCounter = FLASHING_COUNT * 2 * FLASHING_TICKS_INTERVAL;
                break;
            }
        }
    }

    @WrapOperation(
        method = "mouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientAdvancements;setSelectedTab(Lnet/minecraft/advancements/AdvancementHolder;Z)V"
        )
    )
    private void closeSearchOnTabChange(
        ClientAdvancements instance, AdvancementHolder selectedTab, boolean tellServer, Operation<Void> original
    ) {
        isSearchActive = false;
        advancements_search$stopFlashing();
        original.call(instance, selectedTab, tellServer);
    }

    @ModifyExpressionValue(
        method = "extractInside",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
            opcode = Opcodes.GETFIELD
        )
    )
    private @Nullable AdvancementTab redirectTabToExtract(AdvancementTab original) {
        if (isSearchActive) {
            return searchTab.widgets.size() > 1 ? searchTab : null;
        }
        return original;
    }

    @ModifyExpressionValue(
        method = "tick",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
            opcode = Opcodes.GETFIELD
        )
    )
    private AdvancementTab redirectTickToSearchTab(AdvancementTab original) {
        return isSearchActive ? searchTab : selectedTab;
    }

    @WrapOperation(
        method = "extractWindow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;extractTab(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIZ)V"
        )
    )
    private void resetTabSelection(AdvancementTab instance, GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY, boolean selected, Operation<Void> original) {
        original.call(instance, graphics, xo, yo, mouseX, mouseY, !isSearchActive && selected);
    }

    @WrapOperation(
        method = "extractWindow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
        )
    )
    private void enableMarqueeTitleOnOverflow(
        GuiGraphicsExtractor graphics, Font font, Component str, int x, int y, int color, boolean dropShadow, Operation<Void> original
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
            original.call(graphics, font, str, x - (int) pos, y, color, dropShadow);
            graphics.disableScissor();
        } else {
            original.call(graphics, font, str, x, y, color, dropShadow);
        }
    }

    @ModifyReceiver(
        method = "extractTooltips",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;extractTooltips(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"
        )
    )
    private AdvancementTab drawWidgetTooltipRedirectTab(
        AdvancementTab original, GuiGraphicsExtractor graphics, int xo, int yo
    ) {
        return isSearchActive ? searchTab : original;
    }

    @ModifyExpressionValue(
        method = "mouseScrolled",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
            opcode = Opcodes.GETFIELD
        )
    )
    private AdvancementTab redirectScrollToSearchTab(AdvancementTab original) {
        return isSearchActive ? searchTab : original;
    }

    @ModifyExpressionValue(
        method = "mouseDragged",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
            opcode = Opcodes.GETFIELD
        )
    )
    private AdvancementTab redirectDragToSearchTab(AdvancementTab original) {
        return isSearchActive ? searchTab : original;
    }

    @Inject(
        method = "init",
        at = @At(value = "TAIL")
    )
    public void initSearchGui(CallbackInfo ci) {
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
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"
        )
    )
    public void getWindowSizes(
        AdvancementsScreen instance, GuiGraphicsExtractor graphics, Operation<Void> original
    ) {
        treeWidth = Math.abs(leftPos * 2 - width) - WINDOW_BORDER_SIZE - WINDOW_BORDER_SIZE;
        treeHeight = Math.abs(topPos * 2 - height) - WINDOW_HEADER_HEIGHT - WINDOW_BORDER_SIZE;
        original.call(instance, graphics);
    }

    @Inject(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;extractWindow(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            shift = At.Shift.AFTER
        )
    )
    public void renderInject(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
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
        int fieldX = leftPos + treeWidth + WINDOW_BORDER_SIZE - SEARCH_FIELD_WIDTH + symmetryFixX;
        int fieldY = topPos + 4;

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            CREATIVE_INVENTORY_TEXTURE,
            fieldX, fieldY,
            SEARCH_FIELD_UV.x, SEARCH_FIELD_UV.y,
            SEARCH_FIELD_WIDTH, SEARCH_FIELD_HEIGHT,
            256, 256
        );

        searchBox.setX(fieldX + SEARCH_FIELD_TEXT_LEFT_OFFSET);
        searchBox.setY(fieldY + SEARCH_FIELD_TEXT_LEFT_OFFSET);
        searchBox.extractRenderState(graphics, mouseX, mouseY, a);
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
                    processSearch();
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
            return;
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
        double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir
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
