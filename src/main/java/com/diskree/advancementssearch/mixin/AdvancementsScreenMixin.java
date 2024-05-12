package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsScreenImpl;
import com.diskree.advancementssearch.AdvancementsSearch;
import com.diskree.advancementssearch.HighlightType;
import com.diskree.advancementssearch.SearchByType;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen implements AdvancementsScreenImpl {

    @Unique
    private static final Identifier CREATIVE_INVENTORY_TEXTURE =
        new Identifier("textures/gui/container/creative_inventory/tab_item_search.png");

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
    private Advancement searchRootAdvancement;

    @Unique
    private AdvancementTab searchTab;

    @Unique
    private final ArrayList<Advancement> searchResults = new ArrayList<>();

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
    private Advancement highlightedAdvancement;

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
            highlight(searchResults.get(0), highlightType);
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
        for (Advancement advancement : getAdvancements()) {
            if (advancementId.equals(advancement.getId())) {
                highlight(advancement, highlightType);
                break;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (widgetHighlightCounter > 0) {
            widgetHighlightCounter--;
            if (widgetHighlightCounter == 0) {
                advancementssearch$stopHighlight();
            }
        }
        if (searchField != null) {
            searchField.tick();
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
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
    public void resize(MinecraftClient client, int width, int height) {
        if (searchField != null) {
            String oldText = searchField.getText();
            init(client, width, height);
            searchField.setText(oldText);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isFocusedAdvancementClicked &&
            focusedAdvancementWidget != null &&
            focusedAdvancementWidget.tab == searchTab &&
            button == MouseEvent.NOBUTTON
        ) {
            Identifier focusedAdvancementId = focusedAdvancementWidget.advancement.getId();
            for (Advancement advancement : getAdvancements()) {
                if (advancement.getId().equals(focusedAdvancementId)) {
                    highlight(advancement, HighlightType.WIDGET);
                    break;
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Unique
    private @NotNull ArrayList<Advancement> getAdvancements() {
        ArrayList<Advancement> advancements = new ArrayList<>();
        Map<Advancement, AdvancementProgress> progresses = advancementHandler.advancementProgresses;
        for (Advancement advancement : new ArrayList<>(progresses.keySet())) {
            if (advancement == null) {
                continue;
            }
            if (advancement.getParent() == null) {
                continue;
            }
            AdvancementDisplay display = advancement.getDisplay();
            if (display == null) {
                continue;
            }
            if (display.isHidden()) {
                AdvancementProgress progress = progresses.get(advancement);
                if (progress == null || !progress.isDone()) {
                    continue;
                }
            }
            advancements.add(advancement);
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
        for (Advancement advancement : getAdvancements()) {
            AdvancementDisplay display = advancement.getDisplay();
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
                searchResults.add(advancement);
            }
        }
        searchResults.sort(Comparator.comparing(Advancement::getId));

        List<AdvancementFrame> frameOrder = Arrays.asList(
            AdvancementFrame.TASK,
            AdvancementFrame.GOAL,
            AdvancementFrame.CHALLENGE
        );
        searchResults.sort((advancement, nextAdvancement) -> {
            AdvancementDisplay display = advancement.getDisplay();
            AdvancementDisplay nextDisplay = nextAdvancement.getDisplay();
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
        searchTab.addWidget(searchTab.rootWidget, searchRootAdvancement);

        int rowIndex = 0;
        int columnIndex = 0;
        Map<Advancement, AdvancementProgress> progresses = advancementHandler.advancementProgresses;
        Advancement parentAdvancement = searchRootAdvancement;
        for (Advancement searchResult : searchResults) {
            AdvancementDisplay searchResultDisplay = searchResult.getDisplay();
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

            Advancement.Task searchResultAdvancementBuilder = Advancement.Task.create()
                .parent(parentAdvancement)
                .display(searchResultAdvancementDisplay)
                .rewards(searchResult.getRewards());
            searchResultAdvancementBuilder.requirements = searchResult.getRequirements();
            searchResult.getCriteria().forEach(searchResultAdvancementBuilder::criterion);

            Advancement searchResultAdvancement = searchResultAdvancementBuilder.build(searchResult.getId());
            searchTab.addAdvancement(searchResultAdvancement);
            searchTab.widgets.get(searchResultAdvancement).setProgress(progresses.get(searchResultAdvancement));
            if (columnIndex == searchResultsColumnsCount - 1) {
                parentAdvancement = searchRootAdvancement;
                columnIndex = 0;
                rowIndex++;
            } else {
                parentAdvancement = searchResultAdvancement;
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
    private void highlight(@NotNull Advancement advancement, HighlightType type) {
        if (highlightedAdvancement != null) {
            return;
        }
        isSearchActive = false;
        highlightedAdvancement = advancement;
        highlightType = type;
        while (advancement.getParent() != null) {
            advancement = advancement.getParent();
        }
        advancementHandler.selectTab(advancement, true);
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
    private void startHighlight(MatrixStack matrices, int mouseX, int mouseY, int x, int y, CallbackInfo ci) {
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
                highlightedAdvancementId = widget.advancement.getId();
                widgetHighlightCounter = WIDGET_HIGHLIGHT_COUNT * 2 * WIDGET_HIGHLIGHT_TICKS;
                break;
            }
        }
    }

    @Redirect(
        method = "mouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientAdvancementManager;selectTab(Lnet/minecraft/advancement/Advancement;Z)V"
        )
    )
    private void mouseClickedRedirect(
        @NotNull ClientAdvancementManager advancementHandler,
        Advancement tab,
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
        method = "drawWidgets",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;drawBackground(Lnet/minecraft/client/util/math/MatrixStack;IIZ)V"
        )
    )
    private void drawWindowModifyTabSelected(Args args) {
        if (isSearchActive) {
            args.set(3, false);
        }
    }

    @Redirect(
        method = "drawWidgetTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;drawWidgetTooltip(Lnet/minecraft/client/util/math/MatrixStack;IIII)V"
        )
    )
    private void drawWidgetTooltipRedirectTab(
        AdvancementTab selectedTab,
        MatrixStack matrices,
        int mouseX,
        int mouseY,
        int x,
        int y
    ) {
        if (isSearchActive) {
            selectedTab = searchTab;
        }
        selectedTab.drawWidgetTooltip(matrices, mouseX, mouseY, x, y);
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
            Text.of("")
        );
        searchField.setDrawsBackground(false);
        searchField.setEditableColor(Color.WHITE.getRGB());
        searchField.setFocusUnlocked(false);
        children.add(searchField);
        setInitialFocus(searchField);

        if (searchTab == null) {
            AdvancementDisplay searchRootAdvancementDisplay = new AdvancementDisplay(
                ItemStack.EMPTY,
                Text.of(""),
                Text.of(""),
                null,
                AdvancementFrame.TASK,
                false,
                false,
                true
            );
            searchRootAdvancement = Advancement.Task.create()
                .display(searchRootAdvancementDisplay)
                .build(AdvancementsSearch.ADVANCEMENTS_SEARCH_ID);
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

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;renderBackground(Lnet/minecraft/client/util/math/MatrixStack;)V",
            shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void getWindowSizes(
        MatrixStack matrices,
        int mouseX,
        int mouseY,
        float delta,
        CallbackInfo ci,
        int windowX,
        int windowY
    ) {
        this.windowX = windowX;
        this.windowY = windowY;
        treeWidth = Math.abs(windowX * 2 - width) - WINDOW_BORDER_SIZE - WINDOW_BORDER_SIZE;
        treeHeight = Math.abs(windowY * 2 - height) - WINDOW_HEADER_HEIGHT - WINDOW_BORDER_SIZE;
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;drawWidgetTooltip(Lnet/minecraft/client/util/math/MatrixStack;IIII)V",
            shift = At.Shift.BEFORE
        )
    )
    public void renderInject(
        MatrixStack matrices,
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

            if (client != null) {
                client.getTextureManager().bindTexture(CREATIVE_INVENTORY_TEXTURE);
            }
            drawTexture(
                matrices,
                fieldX,
                fieldY,
                SEARCH_FIELD_UV.x,
                SEARCH_FIELD_UV.y,
                SEARCH_FIELD_WIDTH,
                SEARCH_FIELD_HEIGHT
            );

            searchField.setX(fieldX + SEARCH_FIELD_TEXT_LEFT_OFFSET);
            searchField.y = fieldY + SEARCH_FIELD_TEXT_LEFT_OFFSET;
            searchField.render(matrices, mouseX, mouseY, delta);
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
        method = "mouseDragged",
        at = @At(value = "HEAD")
    )
    private void resetFocusedAdvancement(
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
