package com.diskree.advancementssearch.mixin;

import com.diskree.advancementssearch.AdvancementsSearch;
import net.minecraft.advancement.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementTabType;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
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
public abstract class AdvancementsScreenMixin extends Screen {

    @Unique
    private static final Identifier CREATIVE_INVENTORY_TEXTURE = new Identifier("textures/gui/container/creative_inventory/tab_item_search.png");

    @Unique
    private static final Point SEARCH_FIELD_UV = new Point(80, 4);

    @Unique
    private static final int SEARCH_FIELD_WIDTH = 90;

    @Unique
    private static final int SEARCH_FIELD_HEIGHT = 12;

    @Unique
    private TextFieldWidget searchField;

    @Unique
    private PlacedAdvancement searchRootAdvancement;

    @Unique
    private AdvancementTab searchTab;

    @Unique
    private boolean isSearchActive;

    @Unique
    private int windowWidth;

    @Shadow
    @Final
    private ClientAdvancementManager advancementHandler;

    @Shadow
    private @Nullable AdvancementTab selectedTab;

    @Shadow
    public abstract boolean mouseClicked(double mouseX, double mouseY, int button);

    public AdvancementsScreenMixin() {
        super(null);
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
            if (!searchField.getText().isEmpty()) {
                search();
            }
        }
    }

    @Unique
    private void search() {
        if (client == null || client.player == null || searchField == null) {
            return;
        }
        String query = searchField.getText().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            isSearchActive = false;
            return;
        }
        isSearchActive = true;
        for (AdvancementWidget widget : searchTab.widgets.values()) {
            widget.parent = null;
            widget.children.clear();
        }
        searchTab.widgets.clear();

        searchTab.minPanX = Integer.MAX_VALUE;
        searchTab.minPanY = Integer.MAX_VALUE;
        searchTab.maxPanX = Integer.MIN_VALUE;
        searchTab.maxPanY = Integer.MIN_VALUE;
        searchTab.initialized = false;

        searchTab.addWidget(searchTab.rootWidget, searchRootAdvancement.getAdvancementEntry());

        int rowIndex = 0;
        int columnIndex = 0;
        AdvancementManager advancementManager = advancementHandler.getManager();
        Map<AdvancementEntry, AdvancementProgress> progresses = client.player.networkHandler.getAdvancementHandler().advancementProgresses;
        ArrayList<PlacedAdvancement> searchResults = new ArrayList<>();
        for (AdvancementEntry advancementEntry : new ArrayList<>(progresses.keySet())) {
            if (advancementEntry == null) {
                continue;
            }
            Advancement advancement = advancementEntry.value();
            if (advancement.isRoot()) {
                continue;
            }
            AdvancementDisplay display = advancementEntry.value().display().orElse(null);
            if (display == null || display.isHidden()) {
                continue;
            }
            PlacedAdvancement placedAdvancement = advancementManager.get(advancementEntry);
            if (placedAdvancement == null) {
                continue;
            }
            PlacedAdvancement rootAdvancement = placedAdvancement.getRoot();
            if (rootAdvancement == null) {
                continue;
            }
            String title = display.getTitle().getString().toLowerCase(Locale.ROOT);
            String description = display.getDescription().getString().toLowerCase(Locale.ROOT);
            if (title.contains(query) || description.contains(query)) {
                searchResults.add(placedAdvancement);
            }
        }

        List<AdvancementFrame> frameOrder = Arrays.asList(AdvancementFrame.TASK, AdvancementFrame.GOAL, AdvancementFrame.CHALLENGE);
        searchResults.sort(Comparator.comparing((advancement) -> advancement.getAdvancementEntry().id()));
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

        PlacedAdvancement rootAdvancement = new PlacedAdvancement(searchRootAdvancement.getAdvancementEntry(), null);
        PlacedAdvancement parentPlacedAdvancement = rootAdvancement;
        int frameContainerWidth = 28;
        int treeWidth = windowWidth - 9 * 2;
        int columnsCount = treeWidth / frameContainerWidth;
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
            if (columnIndex == columnsCount - 1) {
                parentPlacedAdvancement = rootAdvancement;
                columnIndex = 0;
                rowIndex++;
            } else {
                parentPlacedAdvancement = new PlacedAdvancement(searchResultAdvancementEntry, searchResultPlacedAdvancement);
                columnIndex++;
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
        instance.selectTab(tab, true);
    }

    @Inject(
            method = "drawAdvancementTree",
            at = @At("HEAD"),
            cancellable = true
    )
    private void drawAdvancementTreeInject(DrawContext context, int mouseX, int mouseY, int x, int y, CallbackInfo ci) {
        if (isSearchActive && searchTab.widgets.size() > 1) {
            searchTab.render(context, x + 9, y + 18);
            ci.cancel();
        }
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

    @Redirect(
            method = "drawAdvancementTree",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screen/advancement/AdvancementTab;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private @Nullable AdvancementTab drawAdvancementTreeRedirect(AdvancementsScreen instance) {
        return isSearchActive && searchTab.widgets.size() <= 1 ? null : selectedTab;
    }

    @Inject(method = "init", at = @At(value = "RETURN"))
    public void initInject(CallbackInfo ci) {
        searchField = new TextFieldWidget(
                textRenderer,
                SEARCH_FIELD_WIDTH - 8,
                SEARCH_FIELD_HEIGHT - 2,
                ScreenTexts.EMPTY
        );
        searchField.setFocusUnlocked(false);
        searchField.setDrawsBackground(false);
        //noinspection DataFlowIssue
        searchField.setEditableColor(Formatting.WHITE.getColorValue());
        searchField.setMaxLength(50);
        addSelectableChild(searchField);
        setInitialFocus(searchField);

        AdvancementDisplay searchRootAdvancementDisplay = new AdvancementDisplay(
                ItemStack.EMPTY,
                Text.empty(),
                Text.empty(),
                Optional.of(new Identifier("textures/block/" + Registries.BLOCK.getId(Blocks.BLACK_CONCRETE).getPath() + ".png")),
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
            searchTab = new AdvancementTab(client, advancementsScreen, AdvancementTabType.RIGHT, 0, searchRootAdvancement, searchRootAdvancementDisplay);
        }
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
            windowWidth = Math.abs(i * 2 - width);
            int fieldX = i + windowWidth - 8 - SEARCH_FIELD_WIDTH;
            int fieldY = j + 4;
            int textPadding = 2;

            context.drawTexture(CREATIVE_INVENTORY_TEXTURE, fieldX, fieldY, SEARCH_FIELD_UV.x, SEARCH_FIELD_UV.y, SEARCH_FIELD_WIDTH, SEARCH_FIELD_HEIGHT);
            searchField.setX(fieldX + textPadding);
            searchField.setY(fieldY + textPadding);
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
            cir.setReturnValue(true);
        }
    }
}
