package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsScreenImpl;
import com.diskree.advancementssearch.AdvancementsSearch;
import com.diskree.advancementssearch.DiceButton;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancement.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
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
import net.minecraft.util.Formatting;
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
    private TextFieldWidget searchField;

    @Unique
    private DiceButton randomAdvancementButton;

    @Unique
    private PlacedAdvancement searchRootAdvancement;

    @Unique
    private AdvancementTab searchTab;

    @Unique
    private ArrayList<PlacedAdvancement> searchResults;

    @Unique
    private boolean isSearchActive;

    @Unique
    private int searchResultsColumnsCount;

    @Unique
    private int searchResultsOriginX;

    @Unique
    private AdvancementWidget focusedAdvancementWidget;

    @Unique
    private int windowWidth;

    @Unique
    private int windowHeight;

    @Unique
    private PlacedAdvancement targetAdvancement;

    @Unique
    private Identifier highlightedAdvancementId;

    @Unique
    private int widgetHighlightCounter;

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
    public int advancementssearch$getWindowWidth(boolean withBorder) {
        return withBorder ? windowWidth : windowWidth - WINDOW_BORDER_SIZE - WINDOW_BORDER_SIZE;
    }

    @Override
    public int advancementssearch$getWindowHeight(boolean withBorder) {
        return withBorder ? windowHeight : windowHeight - WINDOW_HEADER_HEIGHT - WINDOW_BORDER_SIZE;
    }

    @Override
    public Identifier advancementssearch$getHighlightedAdvancementId() {
        return highlightedAdvancementId;
    }

    @Override
    public boolean advancementssearch$isHighlightAtInvisibleState() {
        return widgetHighlightCounter != 0 && (widgetHighlightCounter / WIDGET_HIGHLIGHT_TICKS) % 2 == 0;
    }

    @Override
    public void advancementssearch$stopHighlight() {
        highlightedAdvancementId = null;
        widgetHighlightCounter = 0;
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
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchField != null) {
            String oldText = searchField.getText();
            if (searchField.charTyped(chr, modifiers)) {
                if (!Objects.equals(oldText, searchField.getText())) {
                    search();
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

    @Unique
    private @NotNull ArrayList<PlacedAdvancement> getAdvancements() {
        if (client == null || client.player == null) {
            return new ArrayList<>();
        }
        ArrayList<PlacedAdvancement> advancements = new ArrayList<>();
        AdvancementManager advancementManager = advancementHandler.getManager();
        Map<AdvancementEntry, AdvancementProgress> progresses = client.player.networkHandler.getAdvancementHandler().advancementProgresses;
        for (AdvancementEntry advancementEntry : new ArrayList<>(progresses.keySet())) {
            if (advancementEntry == null) {
                continue;
            }
            Advancement advancement = advancementEntry.value();
            if (advancement.isRoot()) {
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
    private void search() {
        if (client == null || client.player == null || searchField == null) {
            return;
        }
        checkSearchActive();
        String query = searchField.getText().toLowerCase(Locale.ROOT);

        searchResults = new ArrayList<>();
        for (PlacedAdvancement placedAdvancement : getAdvancements()) {
            AdvancementDisplay display = placedAdvancement.getAdvancement().display().orElse(null);
            if (display == null) {
                continue;
            }
            String title = display.getTitle().getString().toLowerCase(Locale.ROOT);
            String description = display.getDescription().getString().toLowerCase(Locale.ROOT);

            if (title.contains(query) || description.contains(query)) {
                searchResults.add(placedAdvancement);
            }
        }
        searchResults.sort(Comparator.comparing((advancement) -> advancement.getAdvancementEntry().id()));

        List<AdvancementFrame> frameOrder = Arrays.asList(
                AdvancementFrame.TASK,
                AdvancementFrame.GOAL,
                AdvancementFrame.CHALLENGE
        );
        searchResults.sort((advancement1, advancement2) -> {
            AdvancementDisplay display1 = advancement1.getAdvancement().display().orElse(null);
            AdvancementDisplay display2 = advancement2.getAdvancement().display().orElse(null);
            if (display1 == null || display2 == null) {
                return 0;
            }
            int index1 = frameOrder.indexOf(display1.getFrame());
            int index2 = frameOrder.indexOf(display2.getFrame());
            return Integer.compare(index1, index2);
        });
        if (searchResults.isEmpty()) {
            resetSearchTab();
            return;
        }
        updateSearchResults();
    }

    @Unique
    private void updateSearchResults() {
        if (client == null || client.player == null || searchTab == null) {
            return;
        }
        resetSearchTab();
        searchTab.addWidget(searchTab.rootWidget, searchRootAdvancement.getAdvancementEntry());

        int rowIndex = 0;
        int columnIndex = 0;
        Map<AdvancementEntry, AdvancementProgress> progresses = client.player.networkHandler.getAdvancementHandler().advancementProgresses;
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
            AdvancementEntry searchResultAdvancementEntry = searchResultAdvancementBuilder.build(searchResult.getAdvancementEntry().id());
            PlacedAdvancement searchResultPlacedAdvancement = new PlacedAdvancement(searchResultAdvancementEntry, parentPlacedAdvancement);

            searchTab.addAdvancement(searchResultPlacedAdvancement);
            searchTab.widgets.get(searchResultAdvancementEntry).setProgress(progresses.get(searchResultAdvancementEntry));
            if (columnIndex == searchResultsColumnsCount - 1) {
                parentPlacedAdvancement = rootAdvancement;
                columnIndex = 0;
                rowIndex++;
            } else {
                parentPlacedAdvancement = new PlacedAdvancement(searchResultAdvancementEntry, searchResultPlacedAdvancement);
                columnIndex++;
            }
        }
    }

    @Unique
    private void resetSearchTab() {
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
    private void checkSearchActive() {
        String query = searchField.getText().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            isSearchActive = false;
            return;
        }
        isSearchActive = true;
    }

    @Unique
    private boolean openAdvancement(@NotNull PlacedAdvancement placedAdvancement) {
        if (targetAdvancement != null) {
            return false;
        }
        isSearchActive = false;
        targetAdvancement = placedAdvancement;
        advancementHandler.selectTab(placedAdvancement.getRoot().getAdvancementEntry(), true);
        return true;
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
    private void jumpToTargetAdvancement(DrawContext context, int mouseX, int mouseY, int x, int y, CallbackInfo ci) {
        if (targetAdvancement != null && selectedTab != null) {
            for (AdvancementWidget widget : selectedTab.widgets.values()) {
                if (widget != null && widget.advancement == targetAdvancement) {
                    selectedTab.move(
                            -(selectedTab.originX + widget.getX() + TREE_X_OFFSET + (double) WIDGET_SIZE / 2 - ((double) advancementssearch$getWindowWidth(false)) / 2),
                            -(selectedTab.originY + widget.getY() + (double) WIDGET_SIZE / 2 - ((double) advancementssearch$getWindowHeight(false)) / 2)
                    );
                    targetAdvancement = null;
                    highlightedAdvancementId = widget.advancement.getAdvancementEntry().id();
                    widgetHighlightCounter = WIDGET_HIGHLIGHT_COUNT * 2 * WIDGET_HIGHLIGHT_TICKS;
                    break;
                }
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
    private void mouseClickedRedirect(@NotNull ClientAdvancementManager instance, AdvancementEntry tab, boolean local) {
        isSearchActive = false;
        advancementssearch$stopHighlight();
        instance.selectTab(tab, true);
    }

    @Redirect(
            method = "drawAdvancementTree",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private @Nullable AdvancementTab drawAdvancementTreeInject(AdvancementsScreen instance) {
        return !isSearchActive ? selectedTab : searchTab.widgets.size() > 1 ? searchTab : null;
    }

    @ModifyArgs(
            method = "drawAdvancementTree",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V",
                    ordinal = 0
            )
    )
    private void drawAdvancementTreeModifyText(Args args) {
        if (isSearchActive) {
            args.set(1, Text.translatable("advancementssearch.advancements_not_found"));
        }
    }

    @WrapOperation(
            method = "drawAdvancementTree",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V",
                    ordinal = 1
            )
    )
    private void cancelSadLabelRenderInSearch(DrawContext instance, TextRenderer textRenderer, Text text, int centerX, int y, int color, Operation<Void> original) {
        if (!isSearchActive) {
            original.call(instance, textRenderer, text, centerX, y, color);
        }
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
            method = "drawWidgetTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;drawWidgetTooltip(Lnet/minecraft/client/gui/DrawContext;IIII)V"
            )
    )
    private void drawWidgetTooltipRedirectTab(AdvancementTab instance, DrawContext context, int mouseX, int mouseY, int x, int y) {
        if (isSearchActive) {
            instance = searchTab;
        }
        instance.drawWidgetTooltip(context, mouseX, mouseY, x, y);
    }

    @Redirect(
            method = "mouseScrolled",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private AdvancementTab mouseScrolledRedirect(AdvancementsScreen instance) {
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
    private AdvancementTab mouseDraggedRedirect(AdvancementsScreen instance) {
        return isSearchActive ? searchTab : selectedTab;
    }

    @Inject(method = "init", at = @At(value = "TAIL"))
    public void initInject(CallbackInfo ci) {
        searchField = new TextFieldWidget(textRenderer, 0, 0, ScreenTexts.EMPTY);
        searchField.setDrawsBackground(false);
        searchField.setEditableColor(Formatting.WHITE.getColorValue());
        searchField.setMaxLength(50);
        setInitialFocus(searchField);

        randomAdvancementButton = new DiceButton(() -> {
            if (client == null || client.player == null) {
                return false;
            }
            Map<AdvancementEntry, AdvancementProgress> progresses = client.player.networkHandler.getAdvancementHandler().advancementProgresses;
            ArrayList<PlacedAdvancement> notObtainedAdvancements = new ArrayList<>();
            for (PlacedAdvancement placedAdvancement : getAdvancements()) {
                if (!progresses.get(placedAdvancement.getAdvancementEntry()).isDone()) {
                    notObtainedAdvancements.add(placedAdvancement);
                }
            }
            if (!notObtainedAdvancements.isEmpty()) {
                return openAdvancement(notObtainedAdvancements.get(client.player.getRandom().nextInt(notObtainedAdvancements.size())));
            }
            return false;
        });

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
                            .build(AdvancementsSearch.ADVANCEMENTS_SEARCH_ID),
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

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void getWindowSizes(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci, int i, int j) {
        windowWidth = Math.abs(i * 2 - width);
        windowHeight = Math.abs(j * 2 - height);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;drawWindow(Lnet/minecraft/client/gui/DrawContext;II)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void renderInject(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci, int i, int j) {
        if (searchField != null) {
            int frameOffset = 1;
            int frameContainerWidth = frameOffset + WIDGET_SIZE + frameOffset;
            int treeWidth = advancementssearch$getWindowWidth(false);
            int columnsCount = treeWidth / frameContainerWidth;
            int rowWidth = frameContainerWidth * columnsCount;
            int horizontalOffset = treeWidth - rowWidth - TREE_X_OFFSET;
            int originX = horizontalOffset / 2;
            if (searchResultsColumnsCount != columnsCount || searchResultsOriginX != originX) {
                searchResultsColumnsCount = columnsCount;
                searchResultsOriginX = originX;
                if (isSearchActive) {
                    updateSearchResults();
                }
            }

            int symmetryFixX = 1;
            int fieldX = i + windowWidth - WINDOW_BORDER_SIZE - SEARCH_FIELD_WIDTH + symmetryFixX;
            int fieldY = j + 4;

            context.drawTexture(CREATIVE_INVENTORY_TEXTURE, fieldX, fieldY, SEARCH_FIELD_UV.x, SEARCH_FIELD_UV.y, SEARCH_FIELD_WIDTH, SEARCH_FIELD_HEIGHT);

            int leftTextOffset = 2;
            int rightTextOffset = 8;
            searchField.setX(fieldX + leftTextOffset);
            searchField.setY(fieldY + leftTextOffset);
            searchField.setWidth(SEARCH_FIELD_WIDTH - leftTextOffset - rightTextOffset);
            searchField.setHeight(SEARCH_FIELD_HEIGHT - leftTextOffset);
            searchField.render(context, mouseX, mouseY, delta);

            randomAdvancementButton.setX(fieldX - randomAdvancementButton.getWidth() - 3);
            randomAdvancementButton.setY(fieldY);
            randomAdvancementButton.render(context, mouseX, mouseY, delta);
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
                    search();
                }
                cir.setReturnValue(true);
            }
            if (searchField.isFocused() && searchField.isVisible() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
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
            checkSearchActive();
            cir.setReturnValue(true);
        }
        if (randomAdvancementButton != null && randomAdvancementButton.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
        if (focusedAdvancementWidget != null &&
                focusedAdvancementWidget.tab != null &&
                focusedAdvancementWidget.tab == searchTab &&
                button == 1
        ) {
            for (PlacedAdvancement placedAdvancement : getAdvancements()) {
                if (placedAdvancement.getAdvancementEntry().id().equals(focusedAdvancementWidget.advancement.getAdvancementEntry().id())) {
                    openAdvancement(placedAdvancement);
                    cir.setReturnValue(true);
                    break;
                }
            }
        }
    }
}
