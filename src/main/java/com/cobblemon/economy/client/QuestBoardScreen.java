package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.questboard.QuestBoardState;
import com.cobblemon.economy.networking.QuestBoardActionPayload;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Mth;

import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuestBoardScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "textures/gui/quest/quest_board.png");
    private static final ResourceLocation FAVICON = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "textures/gui/quest/quest_favicon.png");
    private static final ResourceLocation TIER_1 = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "textures/gui/quest/quest_tier1.png");
    private static final ResourceLocation TIER_2 = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "textures/gui/quest/quest_tier2.png");
    private static final ResourceLocation TIER_3 = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "textures/gui/quest/quest_tier3.png");
    private static final int TEX_W = 296;
    private static final int TEX_H = 162;
    private static final int GUI_W = 296;
    private static final int GUI_H = 162;
    private static final int[] QUEST_SLOT_X = {98, 153, 208, 98, 153, 208};
    private static final int[] QUEST_SLOT_Y = {32, 32, 32, 89, 89, 89};
    private static final int DETAIL_PANEL_X = 26;
    private static final int DETAIL_PANEL_Y = 39;
    private static final int DETAIL_PANEL_W = 69;
    private static final int DETAIL_PANEL_H = 84;
    private static final int HEADER_Y = 8;
    private static final int HEADER_ICON_X = 24;
    private static final int HEADER_TITLE_X = 30;
    private static final int HEADER_RIGHT_MARGIN = 8;
    private static final int TIER_X = 39;
    private static final int TIER_Y = 31;
    private static final float TIER_DISPLAY_SCALE = 0.75f;
    private static final int REWARD_SLOT_X = 26;
    private static final int REWARD_SLOT_Y = 124;
    private static final int SELECTED_PREVIEW_ITEM_X = 51;
    private static final int SELECTED_PREVIEW_ITEM_Y = 93;
    private static final int REWARD_SLOT_W = 68;
    private static final float DETAIL_TEXT_SCALE = 0.75f;
    private static final float SELECTED_PREVIEW_ITEM_SCALE = 1.15f;
    private static final float CARD_PREVIEW_ITEM_SCALE = 2.0f;
    private static final float CARD_PREVIEW_MODEL_SCALE = 0.80f;
    private static final float SELECTED_PREVIEW_MODEL_SCALE = 0.75f;
    private static final int COLOR_HEADER_TEXT = 0xF7F2E5;
    private static final int COLOR_TITLE = 0xEABF4A;
    private static final int COLOR_BODY_TEXT = 0xE5E1D7;
    private static final int COLOR_MUTED_TEXT = 0xBEB7A7;
    private static final int COLOR_ALERT_TEXT = 0xE79AA4;
    private static final int COLOR_REWARD_TEXT = 0x5D4A2D;
    private static final int COLOR_AVAILABLE = 0xEABF4A;
    private static final int COLOR_ACTIVE = 0x82B5D8;
    private static final int COLOR_CLAIMABLE = 0x8AD49A;
    private static final int COLOR_COMPLETED = 0xBEB7A7;
    private static final int COLOR_LOCKED = 0xD17F86;
    private static final int COLOR_COOLDOWN = 0xB7A4D8;
    private static final long DOUBLE_CLICK_MS = 350L;

    private final String boardId;
    private final QuestBoardState state;
    private int selectedIndex = 0;
    private long cancelConfirmUntil = 0L;
    private ModelWidget selectedModelWidget;
    private final List<ModelWidget> cardWidgets = new ArrayList<>();
    private int ticksElapsed = 0;
    private int selectPointerOffsetY = 0;
    private boolean selectPointerIncrement = false;
    private long lastLeftQuestClickAt = 0L;
    private long lastRightQuestClickAt = 0L;
    private int lastLeftQuestIndex = -1;
    private int lastRightQuestIndex = -1;
    private long lastTickAt = 0L;
    private boolean refreshQueued = false;

    public QuestBoardScreen(String boardId, QuestBoardState state) {
        super(Component.translatable("cobblemon-economy.quest.gui.npc", state != null ? state.boardName : boardId));
        this.boardId = boardId;
        this.state = state == null ? new QuestBoardState() : state;
    }

    public boolean isSameBoard(String otherBoardId) {
        return this.boardId != null && this.boardId.equals(otherBoardId);
    }

    public void copySelectionFrom(QuestBoardScreen other) {
        if (other == null) {
            return;
        }

        String selectedQuestId = other.getSelectedQuestId();
        if (selectedQuestId != null && this.state.quests != null) {
            for (int i = 0; i < this.state.quests.size(); i++) {
                QuestBoardState.QuestCard card = this.state.quests.get(i);
                if (card != null && selectedQuestId.equals(card.questId)) {
                    this.selectedIndex = i;
                    return;
                }
            }
        }

        this.selectedIndex = Mth.clamp(other.selectedIndex, 0, Math.max(0, this.state.quests.size() - 1));
    }

    @Override
    protected void init() {
        super.init();
        ensureNearest(BACKGROUND);
        ensureNearest(FAVICON);
        ensureNearest(TIER_1);
        ensureNearest(TIER_2);
        ensureNearest(TIER_3);
        this.selectedIndex = Mth.clamp(this.selectedIndex, 0, Math.max(0, this.state.quests.size() - 1));
        this.cancelConfirmUntil = 0L;
        this.lastTickAt = System.currentTimeMillis();
        this.refreshQueued = false;
        buildModelWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        long now = System.currentTimeMillis();
        long deltaMs = this.lastTickAt <= 0L ? 0L : Math.min(1000L, Math.max(0L, now - this.lastTickAt));
        this.lastTickAt = now;
        ticksElapsed++;
        if (!refreshQueued && tickDynamicState(deltaMs)) {
            requestRefresh();
        }
        int speed = 3;
        if (ticksElapsed % (2 * speed) == 0) {
            selectPointerIncrement = !selectPointerIncrement;
        }
        if (ticksElapsed % speed == 0) {
            selectPointerOffsetY += selectPointerIncrement ? 1 : -1;
            selectPointerOffsetY = Mth.clamp(selectPointerOffsetY, -1, 1);
        }
    }

    private void buildModelWidgets() {
        this.cardWidgets.clear();
        int left = left();
        int top = top();

        for (int i = 0; i < Math.min(QUEST_SLOT_X.length, state.quests.size()); i++) {
            QuestBoardState.QuestCard card = state.quests.get(i);
            ModelWidget widget = usesPokemonPreview(card)
                    ? createModelWidget(card.previewSpecies, card.previewShiny, left + QUEST_SLOT_X[i] - 2, top + QUEST_SLOT_Y[i], 54, 54, CARD_PREVIEW_MODEL_SCALE)
                    : null;
            cardWidgets.add(widget);
        }

        QuestBoardState.QuestCard selected = getSelectedQuest();
        selectedModelWidget = selected == null || !usesPokemonPreview(selected)
                ? null
                : createModelWidget(selected.previewSpecies, selected.previewShiny, left + 39, top + 93, 42, 29, SELECTED_PREVIEW_MODEL_SCALE);
    }

    private void onPrimaryAction() {
        QuestBoardState.QuestCard selected = getSelectedQuest();
        if (selected == null) {
            return;
        }

        String status = selected.status == null ? "" : selected.status.toUpperCase(Locale.ROOT);
        if ("AVAILABLE".equals(status)) {
            sendAction(selected.questId, "ACCEPT");
            return;
        }
        if ("CLAIMABLE".equals(status)) {
            sendAction(selected.questId, "CLAIM");
        }
    }

    private void onCancelAction() {
        QuestBoardState.QuestCard selected = getSelectedQuest();
        if (selected == null) {
            return;
        }
        String status = selected.status == null ? "" : selected.status.toUpperCase(Locale.ROOT);
        if ("ACTIVE".equals(status)) {
            sendAction(selected.questId, "CANCEL");
            cancelConfirmUntil = 0L;
        }
    }

    private void sendAction(String questId, String action) {
        if (ClientPlayNetworking.canSend(QuestBoardActionPayload.TYPE)) {
            this.refreshQueued = true;
            ClientPlayNetworking.send(new QuestBoardActionPayload(boardId, questId, action));
        }
    }

    private QuestBoardState.QuestCard getSelectedQuest() {
        if (state.quests == null || state.quests.isEmpty()) {
            return null;
        }
        if (selectedIndex < 0 || selectedIndex >= state.quests.size()) {
            selectedIndex = 0;
        }
        return state.quests.get(selectedIndex);
    }

    private String getSelectedQuestId() {
        QuestBoardState.QuestCard selected = getSelectedQuest();
        return selected != null ? selected.questId : null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xAA000000);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = left();
        int top = top();
        guiGraphics.fill(left, top, left + GUI_W, top + GUI_H, 0xFF1A120A);
        blitNearest(guiGraphics, BACKGROUND, left, top, 0, 0, GUI_W, GUI_H, TEX_W, TEX_H);
        blitNearest(guiGraphics, FAVICON, left + HEADER_ICON_X, top + HEADER_Y + 1, 0, 0, 7, 7, 7, 7);

        String refresh = formatDuration(this.state.rotationRemainingMs);
        int refreshWidth = this.font.width(refresh);
        int refreshX = left + GUI_W - HEADER_RIGHT_MARGIN - refreshWidth;
        int titleMaxWidth = Math.max(40, refreshX - (left + HEADER_TITLE_X) - 6);
        String title = ellipsize(this.state.boardName == null ? this.boardId : this.state.boardName, titleMaxWidth);
        guiGraphics.drawString(this.font, title, left + HEADER_TITLE_X, top + HEADER_Y, COLOR_HEADER_TEXT, true);
        guiGraphics.drawString(this.font, refresh, refreshX, top + HEADER_Y, COLOR_HEADER_TEXT, true);

        int hoveredQuestIndex = -1;
        for (int i = 0; i < Math.min(QUEST_SLOT_X.length, state.quests.size()); i++) {
            QuestBoardState.QuestCard card = state.quests.get(i);
            int sx = left + QUEST_SLOT_X[i];
            int sy = top + QUEST_SLOT_Y[i];
            if (inside(mouseX, mouseY, sx, sy, 50, 50)) {
                hoveredQuestIndex = i;
            }
            renderCardSurface(guiGraphics, card, sx, sy);
            if (i == selectedIndex) {
                int selectionColor = statusColor(card);
                guiGraphics.fill(sx - 2, sy - 2, sx + 52, sy - 1, selectionColor);
                guiGraphics.fill(sx - 2, sy + 51, sx + 52, sy + 52, selectionColor);
                guiGraphics.fill(sx - 2, sy - 2, sx - 1, sy + 52, selectionColor);
                guiGraphics.fill(sx + 51, sy - 2, sx + 52, sy + 52, selectionColor);
                guiGraphics.fill(sx, sy, sx + 50, sy + 50, 0x222C2115);
                guiGraphics.drawString(this.font, ">", sx + 54, sy + 20 + selectPointerOffsetY, 0xFFE7D29F, false);
            } else if (hoveredQuestIndex == i) {
                int hoverColor = statusColor(card);
                guiGraphics.fill(sx - 1, sy - 1, sx + 51, sy, hoverColor);
                guiGraphics.fill(sx - 1, sy + 50, sx + 51, sy + 51, hoverColor);
                guiGraphics.fill(sx - 1, sy - 1, sx, sy + 51, hoverColor);
                guiGraphics.fill(sx + 50, sy - 1, sx + 51, sy + 51, hoverColor);
            }
            if (i < cardWidgets.size() && cardWidgets.get(i) != null) {
                cardWidgets.get(i).render(guiGraphics, mouseX, mouseY, partialTick);
            } else {
                renderCardPreviewItem(guiGraphics, card, sx, sy);
            }
            renderCardStateMarker(guiGraphics, card, sx, sy);
        }

        QuestBoardState.QuestCard selected = getSelectedQuest();
        if (selected != null) {
            guiGraphics.fill(left + DETAIL_PANEL_X, top + DETAIL_PANEL_Y,
                    left + DETAIL_PANEL_X + DETAIL_PANEL_W, top + DETAIL_PANEL_Y + DETAIL_PANEL_H, 0x7A24170E);
            guiGraphics.fill(left + DETAIL_PANEL_X, top + DETAIL_PANEL_Y,
                    left + DETAIL_PANEL_X + DETAIL_PANEL_W, top + DETAIL_PANEL_Y + 1, 0xB8D1B07A);
            guiGraphics.fill(left + DETAIL_PANEL_X, top + DETAIL_PANEL_Y + DETAIL_PANEL_H - 1,
                    left + DETAIL_PANEL_X + DETAIL_PANEL_W, top + DETAIL_PANEL_Y + DETAIL_PANEL_H, 0x7A8A6A42);

            drawDetailText(guiGraphics, ellipsize(selected.questName, scaledDetailWidth()), left, top + 44, COLOR_TITLE);
            String statusKey = selected.status == null ? "available" : selected.status.toLowerCase(Locale.ROOT);
            drawDetailText(guiGraphics, ellipsize(Component.translatable("cobblemon-economy.quest.status." + statusKey).getString(), scaledDetailWidth()), left, top + 56, statusColor(selected));
            drawDetailText(guiGraphics, ellipsize(selected.progressSummary == null ? "" : selected.progressSummary, scaledDetailWidth()), left, top + 68, COLOR_BODY_TEXT);

            String timerText = detailTimerText(selected);
            if (!timerText.isBlank()) {
                drawDetailText(guiGraphics, ellipsize(timerText, scaledDetailWidth()), left, top + 80, COLOR_ALERT_TEXT);
            }

            ResourceLocation tierTexture = resolveTierTexture(selected.rewardPokedollars);
            renderTierStars(guiGraphics, tierTexture, left, top);

            renderRewardLines(guiGraphics, selected, left, top);
        }

        if (selectedModelWidget != null) {
            renderSelectedPreviewFrame(guiGraphics, left, top);
            selectedModelWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        } else if (selected != null) {
            renderSelectedPreviewFrame(guiGraphics, left, top);
            renderSelectedPreviewItem(guiGraphics, selected, left, top);
        }

        String interactionHint = null;
        if (selected != null) {
            String status = selected.status == null ? "" : selected.status.toUpperCase(Locale.ROOT);
            interactionHint = switch (status) {
                case "AVAILABLE", "CLAIMABLE" -> Component.translatable("cobblemon-economy.quest.hint.double_left").getString();
                case "ACTIVE" -> Component.translatable("cobblemon-economy.quest.hint.double_right").getString();
                default -> Component.translatable("cobblemon-economy.quest.hint.select_card").getString();
            };
        }

        if (interactionHint != null && !interactionHint.isBlank()) {
            int hintY = top + GUI_H - 10;
            String hintText = ellipsize(interactionHint, GUI_W - 4);
            int hintX = left + (GUI_W - this.font.width(hintText)) / 2;
            guiGraphics.fill(left + 10, hintY - 2, left + GUI_W - 10, hintY + 9, 0xA51E140D);
            guiGraphics.drawString(this.font, hintText, hintX, hintY, COLOR_BODY_TEXT, true);
        }

        if (System.currentTimeMillis() < cancelConfirmUntil) {
            drawDetailText(guiGraphics, ellipsize(Component.translatable("cobblemon-economy.quest.cancel_confirm").getString(), scaledDetailWidth()), left, top + 112, COLOR_ALERT_TEXT);
        }

        if (hoveredQuestIndex >= 0 && hoveredQuestIndex < state.quests.size()) {
            renderQuestTooltip(guiGraphics, state.quests.get(hoveredQuestIndex), mouseX, mouseY);
        } else if (selected != null && inside(mouseX, mouseY, left + DETAIL_PANEL_X, top + DETAIL_PANEL_Y, DETAIL_PANEL_W, DETAIL_PANEL_H)) {
            renderQuestTooltip(guiGraphics, selected, mouseX, mouseY);
        }
    }

    private boolean tickDynamicState(long deltaMs) {
        if (deltaMs <= 0L) {
            return false;
        }

        boolean needsRefresh = advanceTimer(this.state.rotationRemainingMs, value -> this.state.rotationRemainingMs = value, deltaMs);
        if (this.state.quests == null) {
            return needsRefresh;
        }

        for (QuestBoardState.QuestCard card : this.state.quests) {
            if (card == null) {
                continue;
            }
            needsRefresh |= advanceTimer(card.timeRemainingMs, value -> card.timeRemainingMs = value, deltaMs);
            needsRefresh |= advanceTimer(card.cooldownRemainingMs, value -> card.cooldownRemainingMs = value, deltaMs);
        }
        return needsRefresh;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = left();
        int top = top();

        if (button == 0 || button == 1) {
            for (int i = 0; i < Math.min(QUEST_SLOT_X.length, state.quests.size()); i++) {
                int sx = left + QUEST_SLOT_X[i];
                int sy = top + QUEST_SLOT_Y[i];
                if (!inside(mouseX, mouseY, sx, sy, 50, 50)) {
                    continue;
                }

                this.selectedIndex = i;
                buildModelWidgets();

                long now = System.currentTimeMillis();
                if (button == 0) {
                    boolean isDouble = lastLeftQuestIndex == i && (now - lastLeftQuestClickAt) <= DOUBLE_CLICK_MS;
                    lastLeftQuestClickAt = now;
                    lastLeftQuestIndex = i;
                    if (isDouble) {
                        onPrimaryAction();
                    }
                } else {
                    boolean isDouble = lastRightQuestIndex == i && (now - lastRightQuestClickAt) <= DOUBLE_CLICK_MS;
                    lastRightQuestClickAt = now;
                    lastRightQuestIndex = i;
                    if (isDouble) {
                        onCancelAction();
                    } else {
                        QuestBoardState.QuestCard selected = getSelectedQuest();
                        String status = selected != null && selected.status != null ? selected.status.toUpperCase(Locale.ROOT) : "";
                        cancelConfirmUntil = "ACTIVE".equals(status) ? now + DOUBLE_CLICK_MS : 0L;
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private ModelWidget createModelWidget(String speciesId, boolean shiny, int x, int y, int w, int h, float scale) {
        Pokemon pokemon = buildPokemon(speciesId, shiny);
        if (pokemon == null) {
            return null;
        }
        RenderablePokemon renderablePokemon = pokemon.asRenderablePokemon();
        try {
            Constructor<ModelWidget> modernConstructor = ModelWidget.class.getConstructor(
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    RenderablePokemon.class,
                    float.class,
                    float.class,
                    double.class,
                    boolean.class,
                    boolean.class,
                    int.class
            );
            return modernConstructor.newInstance(x, y, w, h, renderablePokemon, scale, 0.0f, 0.0, false, true, 15);
        } catch (NoSuchMethodException ignored) {
            try {
                Constructor<ModelWidget> legacyConstructor = ModelWidget.class.getConstructor(
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        RenderablePokemon.class,
                        float.class,
                        float.class,
                        double.class,
                        boolean.class,
                        boolean.class
                );
                return legacyConstructor.newInstance(x, y, w, h, renderablePokemon, scale, 0.0f, 0.0, false, true);
            } catch (ReflectiveOperationException | SecurityException e) {
                CobblemonEconomy.LOGGER.error("Unable to create the Cobblemon model widget", e);
                return null;
            }
        } catch (ReflectiveOperationException | SecurityException e) {
            CobblemonEconomy.LOGGER.error("Unable to create the Cobblemon model widget", e);
            return null;
        }
    }

    private Pokemon buildPokemon(String speciesId, boolean shiny) {
        if (speciesId == null || speciesId.isBlank()) {
            return null;
        }

        String normalized = normalizeToken(speciesId);
        Species species = null;
        ResourceLocation rl = ResourceLocation.tryParse(normalized);
        if (rl != null) {
            species = PokemonSpecies.getByIdentifier(rl);
        }
        if (species == null) {
            String shortId = normalized;
            int idx = normalized.indexOf(':');
            if (idx >= 0 && idx + 1 < normalized.length()) {
                shortId = normalized.substring(idx + 1);
            }
            species = PokemonSpecies.getByName(shortId);
        }
        if (species == null) {
            return null;
        }

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setShiny(shiny);
        return pokemon;
    }

    private Item resolveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Items.BARRIER;
        }

        String normalized = normalizeToken(itemId);
        ResourceLocation rl = ResourceLocation.tryParse(normalized);
        if (rl != null) {
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item != null && item != Items.AIR) {
                return item;
            }
        }

        String shortId = normalized;
        int idx = normalized.indexOf(':');
        if (idx >= 0 && idx + 1 < normalized.length()) {
            shortId = normalized.substring(idx + 1);
        }
        ResourceLocation cobblemon = ResourceLocation.fromNamespaceAndPath("cobblemon", shortId);
        Item item = BuiltInRegistries.ITEM.get(cobblemon);
        if (item != null && item != Items.AIR) {
            return item;
        }

        ResourceLocation minecraft = ResourceLocation.fromNamespaceAndPath("minecraft", shortId);
        item = BuiltInRegistries.ITEM.get(minecraft);
        return item != null && item != Items.AIR ? item : Items.BARRIER;
    }

    private void renderCardStateMarker(GuiGraphics guiGraphics, QuestBoardState.QuestCard card, int slotX, int slotY) {
        int color = statusColor(card);
        guiGraphics.fill(slotX + 42, slotY + 42, slotX + 48, slotY + 48, 0xB51A110A);
        guiGraphics.fill(slotX + 43, slotY + 43, slotX + 47, slotY + 47, color);
    }

    private void renderCardSurface(GuiGraphics guiGraphics, QuestBoardState.QuestCard card, int slotX, int slotY) {
        int color = statusColor(card);
        guiGraphics.fill(slotX, slotY, slotX + 50, slotY + 50, 0x6B24170E);
        guiGraphics.fill(slotX + 2, slotY + 2, slotX + 48, slotY + 3, 0x5ADEC695);
        guiGraphics.fill(slotX + 2, slotY + 47, slotX + 48, slotY + 48, 0x7A1A110A);
        guiGraphics.fill(slotX + 2, slotY + 47, slotX + 48, slotY + 49, color);
    }

    private void renderSelectedPreviewFrame(GuiGraphics guiGraphics, int left, int top) {
        int x = left + 34;
        int y = top + 91;
        int width = 52;
        int height = 32;
        guiGraphics.fill(x, y, x + width, y + height, 0x8C24170E);
        guiGraphics.fill(x, y, x + width, y + 1, 0xB8D1B07A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0x7A8A6A42);
        guiGraphics.fill(x, y, x + 1, y + height, 0x7A8A6A42);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0x7A8A6A42);
    }

    private int statusColor(QuestBoardState.QuestCard card) {
        String status = card == null || card.status == null
                ? "AVAILABLE"
                : card.status.toUpperCase(Locale.ROOT);
        return switch (status) {
            case "ACTIVE" -> COLOR_ACTIVE;
            case "CLAIMABLE" -> COLOR_CLAIMABLE;
            case "COMPLETED" -> COLOR_COMPLETED;
            case "LOCKED" -> COLOR_LOCKED;
            case "ON_COOLDOWN" -> COLOR_COOLDOWN;
            default -> COLOR_AVAILABLE;
        };
    }

    private int scaledDetailWidth() {
        return Math.max(1, Math.round((DETAIL_PANEL_W - 4) / DETAIL_TEXT_SCALE));
    }

    private void drawDetailText(GuiGraphics guiGraphics, String text, int left, int y, int color) {
        int renderedWidth = Math.round(this.font.width(text) * DETAIL_TEXT_SCALE);
        int x = left + DETAIL_PANEL_X + Math.max(0, (DETAIL_PANEL_W - renderedWidth) / 2);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(DETAIL_TEXT_SCALE, DETAIL_TEXT_SCALE, 1.0f);
        guiGraphics.drawString(this.font, text, 0, 0, color, true);
        guiGraphics.pose().popPose();
    }

    private boolean usesPokemonPreview(QuestBoardState.QuestCard card) {
        if (card == null) {
            return false;
        }
        String kind = card.previewKind == null ? "" : card.previewKind.toUpperCase(Locale.ROOT);
        if (!"POKEMON".equals(kind) && !"CAPTURE".equals(kind)) {
            return false;
        }
        return card.previewSpecies != null && !card.previewSpecies.isBlank();
    }

    private void renderCardPreviewItem(GuiGraphics guiGraphics, QuestBoardState.QuestCard card, int slotX, int slotY) {
        Item item = resolveItem(card != null ? card.previewItem : null);
        ItemStack stack = new ItemStack(item);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(slotX + 9, slotY + 9, 0);
        guiGraphics.pose().scale(CARD_PREVIEW_ITEM_SCALE, CARD_PREVIEW_ITEM_SCALE, 1.0f);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private void renderSelectedPreviewItem(GuiGraphics guiGraphics, QuestBoardState.QuestCard card, int left, int top) {
        Item item = resolveItem(card != null ? card.previewItem : null);
        ItemStack stack = new ItemStack(item);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left + SELECTED_PREVIEW_ITEM_X - 1, top + SELECTED_PREVIEW_ITEM_Y - 1, 0);
        guiGraphics.pose().scale(SELECTED_PREVIEW_ITEM_SCALE, SELECTED_PREVIEW_ITEM_SCALE, 1.0f);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private String normalizeToken(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private ResourceLocation resolveTierTexture(BigDecimal reward) {
        if (reward == null) {
            return TIER_1;
        }
        if (reward.compareTo(new BigDecimal("10000")) >= 0) {
            return TIER_3;
        }
        if (reward.compareTo(new BigDecimal("5000")) >= 0) {
            return TIER_2;
        }
        return TIER_1;
    }

    private void blitNearest(GuiGraphics guiGraphics,
                             ResourceLocation texture,
                             int x,
                             int y,
                             int u,
                             int v,
                             int width,
                             int height,
                             int textureWidth,
                             int textureHeight) {
        ensureNearest(texture);
        guiGraphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    private void renderTierStars(GuiGraphics guiGraphics, ResourceLocation texture, int left, int top) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left + TIER_X + 21, top + TIER_Y + 6, 0);
        guiGraphics.pose().scale(TIER_DISPLAY_SCALE, TIER_DISPLAY_SCALE, 1.0f);
        blitNearest(guiGraphics, texture, -21, -6, 0, 0, 42, 12, 42, 12);
        guiGraphics.pose().popPose();
    }

    private void ensureNearest(ResourceLocation texture) {
        if (this.minecraft == null) {
            return;
        }
        AbstractTexture nativeTexture = this.minecraft.getTextureManager().getTexture(texture);
        nativeTexture.setFilter(false, false);
    }

    private int left() {
        return (this.width - GUI_W) / 2;
    }

    private int top() {
        return (this.height - GUI_H) / 2;
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        }
        if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }

    private String ellipsize(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int available = Math.max(0, maxWidth - this.font.width(suffix));
        String clipped = this.font.plainSubstrByWidth(text, available);
        return clipped.isEmpty() ? suffix : clipped + suffix;
    }

    private String detailTimerText(QuestBoardState.QuestCard card) {
        if (card == null) {
            return "";
        }
        if (card.timeRemainingMs > 0L) {
            return formatDuration(card.timeRemainingMs);
        }
        if (card.cooldownRemainingMs > 0L) {
            return formatDuration(card.cooldownRemainingMs);
        }
        return "";
    }

    private void renderRewardLines(GuiGraphics guiGraphics, QuestBoardState.QuestCard card, int left, int top) {
        List<Component> rewardLines = buildRewardLines(card);
        if (rewardLines.isEmpty()) {
            return;
        }

        int startY = top + REWARD_SLOT_Y + (rewardLines.size() > 1 ? -3 : 2);
        for (int i = 0; i < rewardLines.size(); i++) {
            String text = ellipsize(rewardLines.get(i).getString(), REWARD_SLOT_W - 4);
            int rewardW = this.font.width(text);
            guiGraphics.drawString(this.font, text, left + REWARD_SLOT_X + Math.max(0, (REWARD_SLOT_W - rewardW) / 2), startY + (i * 10), COLOR_REWARD_TEXT, false);
        }
    }

    private List<Component> buildRewardLines(QuestBoardState.QuestCard card) {
        List<Component> lines = new ArrayList<>();
        if (card == null) {
            return lines;
        }

        if (card.rewardPokedollars != null && card.rewardPokedollars.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(Component.translatable("cobblemon-economy.quest.reward.pokedollars.short", formatAmount(card.rewardPokedollars)));
        }
        if (card.rewardPco != null && card.rewardPco.compareTo(BigDecimal.ZERO) > 0 && lines.size() < 2) {
            lines.add(Component.translatable("cobblemon-economy.quest.reward.pco.short", formatAmount(card.rewardPco)));
        }
        if (card.hasCommandRewards && lines.size() < 2) {
            lines.add(Component.translatable("cobblemon-economy.quest.reward.commands.short"));
        }
        if (lines.isEmpty()) {
            lines.add(Component.literal("-"));
        }
        return lines;
    }

    private boolean advanceTimer(long currentValue, java.util.function.LongConsumer updater, long deltaMs) {
        if (currentValue <= 0L) {
            return false;
        }
        long nextValue = Math.max(0L, currentValue - deltaMs);
        updater.accept(nextValue);
        return currentValue > 0L && nextValue == 0L;
    }

    private void requestRefresh() {
        if (!ClientPlayNetworking.canSend(QuestBoardActionPayload.TYPE)) {
            return;
        }
        this.refreshQueued = true;
        ClientPlayNetworking.send(new QuestBoardActionPayload(boardId, "", "REFRESH"));
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }

    private void renderQuestTooltip(GuiGraphics guiGraphics, QuestBoardState.QuestCard card, int mouseX, int mouseY) {
        if (card == null) {
            return;
        }

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(card.questName == null ? "Quest" : card.questName).withStyle(ChatFormatting.GOLD));

        String statusKey = card.status == null ? "available" : card.status.toLowerCase(Locale.ROOT);
        tooltip.add(Component.translatable("cobblemon-economy.quest.status." + statusKey).withStyle(ChatFormatting.GRAY));

        if (card.progressSummary != null && !card.progressSummary.isBlank()) {
            tooltip.add(Component.literal(card.progressSummary).withStyle(ChatFormatting.WHITE));
        }

        if (card.objectives != null && !card.objectives.isEmpty()) {
            for (String objective : card.objectives) {
                if (objective == null || objective.isBlank()) {
                    continue;
                }
                tooltip.add(Component.literal("- " + objective).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        boolean hasPokedollars = card.rewardPokedollars != null && card.rewardPokedollars.compareTo(BigDecimal.ZERO) > 0;
        boolean hasPco = card.rewardPco != null && card.rewardPco.compareTo(BigDecimal.ZERO) > 0;
        if (hasPokedollars || hasPco) {
            tooltip.add(Component.translatable("cobblemon-economy.quest.rewards_title").withStyle(ChatFormatting.AQUA));
            if (hasPokedollars) {
                tooltip.add(Component.translatable("cobblemon-economy.quest.reward.pokedollars", formatAmount(card.rewardPokedollars)).withStyle(ChatFormatting.WHITE));
            }
            if (hasPco) {
                tooltip.add(Component.translatable("cobblemon-economy.quest.reward.pco", formatAmount(card.rewardPco)).withStyle(ChatFormatting.WHITE));
            }
        }
        if (card.hasCommandRewards) {
            if (!hasPokedollars && !hasPco) {
                tooltip.add(Component.translatable("cobblemon-economy.quest.rewards_title").withStyle(ChatFormatting.AQUA));
            }
            tooltip.add(Component.translatable("cobblemon-economy.quest.reward.commands").withStyle(ChatFormatting.WHITE));
        }

        List<FormattedCharSequence> lines = tooltip.stream().map(Component::getVisualOrderText).toList();
        guiGraphics.renderTooltip(this.font, lines, mouseX, mouseY);
    }

    @Override
    public void renderBlurredBackground(float delta) {
    }

    @Override
    public void renderMenuBackground(GuiGraphics guiGraphics) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
