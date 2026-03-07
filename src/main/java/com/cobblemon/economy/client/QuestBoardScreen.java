package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.questboard.QuestBoardState;
import com.cobblemon.economy.networking.QuestBoardActionPayload;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget;
import com.cobblemon.mod.common.pokemon.Pokemon;
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
    private static final int[] QUEST_SLOT_X = {98, 153, 203, 98, 153, 208};
    private static final int[] QUEST_SLOT_Y = {32, 32, 32, 89, 89, 89};
    private static final int DETAIL_X = 14;
    private static final int DETAIL_W = 78;
    private static final int HEADER_Y = 8;
    private static final int HEADER_ICON_X = 24;
    private static final int HEADER_TITLE_X = 30;
    private static final int HEADER_RIGHT_MARGIN = 8;
    private static final int TIER_X = 26;
    private static final int TIER_Y = 32;
    private static final int REWARD_SLOT_X = 26;
    private static final int REWARD_SLOT_Y = 124;
    private static final float TEXT_DETAIL_SCALE = 0.75f;
    private static final int SELECTED_PREVIEW_ITEM_X = 49;
    private static final int SELECTED_PREVIEW_ITEM_Y = 107;
    private static final float SELECTED_PREVIEW_ITEM_SCALE = 1.9f;
    private static final float CARD_PREVIEW_ITEM_SCALE = 1.8f;
    private static final float CARD_PREVIEW_MODEL_SCALE = 0.80f;
    private static final float SELECTED_PREVIEW_MODEL_SCALE = 1.10f;
    private static final int COLOR_HEADER_TEXT = 0xF7F2E5;
    private static final int COLOR_TITLE = 0xEABF4A;
    private static final int COLOR_BODY_TEXT = 0xE5E1D7;
    private static final int COLOR_MUTED_TEXT = 0xBEB7A7;
    private static final int COLOR_ALERT_TEXT = 0xE79AA4;
    private static final int COLOR_REWARD_TITLE = 0x6FD7C6;
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

    public QuestBoardScreen(String boardId, QuestBoardState state) {
        super(Component.translatable("cobblemon-economy.quest.gui.npc", state != null ? state.boardName : boardId));
        this.boardId = boardId;
        this.state = state == null ? new QuestBoardState() : state;
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
        buildModelWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        ticksElapsed++;
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
                : createModelWidget(selected.previewSpecies, selected.previewShiny, left + 38, top + 82, 54, 54, SELECTED_PREVIEW_MODEL_SCALE);
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
        guiGraphics.drawString(this.font, title, left + HEADER_TITLE_X, top + HEADER_Y, COLOR_HEADER_TEXT, false);
        guiGraphics.drawString(this.font, refresh, refreshX, top + HEADER_Y, COLOR_HEADER_TEXT, false);

        int hoveredQuestIndex = -1;
        for (int i = 0; i < Math.min(QUEST_SLOT_X.length, state.quests.size()); i++) {
            QuestBoardState.QuestCard card = state.quests.get(i);
            int sx = left + QUEST_SLOT_X[i];
            int sy = top + QUEST_SLOT_Y[i];
            if (inside(mouseX, mouseY, sx, sy, 50, 50)) {
                hoveredQuestIndex = i;
            }
            if (i == selectedIndex) {
                guiGraphics.fill(sx - 2, sy - 2, sx + 52, sy - 1, 0xBFD8C097);
                guiGraphics.fill(sx - 2, sy + 51, sx + 52, sy + 52, 0xBFD8C097);
                guiGraphics.fill(sx - 2, sy - 2, sx - 1, sy + 52, 0xBFD8C097);
                guiGraphics.fill(sx + 51, sy - 2, sx + 52, sy + 52, 0xBFD8C097);
                guiGraphics.fill(sx, sy, sx + 50, sy + 50, 0x222C2115);
                guiGraphics.drawString(this.font, ">", sx + 54, sy + 20 + selectPointerOffsetY, 0xFFE7D29F, false);
            }
            if (i < cardWidgets.size() && cardWidgets.get(i) != null) {
                cardWidgets.get(i).render(guiGraphics, mouseX, mouseY, partialTick);
            } else {
                renderCardPreviewItem(guiGraphics, card, sx, sy);
            }
        }

        QuestBoardState.QuestCard selected = getSelectedQuest();
        if (selected != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + DETAIL_X, top + 43, 0);
            guiGraphics.pose().scale(TEXT_DETAIL_SCALE, TEXT_DETAIL_SCALE, 1.0f);
            
            int drawW = (int) (DETAIL_W / TEXT_DETAIL_SCALE);
            int cursorY = 0;
            
            guiGraphics.drawString(this.font, ellipsize(selected.questName, drawW), 0, cursorY, COLOR_TITLE, false);
            cursorY += 12;
            
            String statusKey = selected.status == null ? "available" : selected.status.toLowerCase(Locale.ROOT);
            guiGraphics.drawString(this.font, ellipsize(Component.translatable("cobblemon-economy.quest.status." + statusKey).getString(), drawW), 0, cursorY, COLOR_BODY_TEXT, false);
            cursorY += 12;

            guiGraphics.drawString(this.font, ellipsize(selected.progressSummary == null ? "" : selected.progressSummary, drawW), 0, cursorY, COLOR_BODY_TEXT, false);
            cursorY += 12;

            if (selected.timeRemainingMs > 0) {
                guiGraphics.drawString(this.font, ellipsize(Component.translatable("cobblemon-economy.quest.time_remaining", formatDuration(selected.timeRemainingMs)).getString(), drawW), 0, cursorY, COLOR_ALERT_TEXT, false);
                cursorY += 12;
            } else if (selected.cooldownRemainingMs > 0) {
                guiGraphics.drawString(this.font, ellipsize(Component.translatable("cobblemon-economy.quest.cooldown_remaining", formatDuration(selected.cooldownRemainingMs)).getString(), drawW), 0, cursorY, COLOR_ALERT_TEXT, false);
                cursorY += 12;
            }

            if (selected.objectives != null && !selected.objectives.isEmpty()) {
                guiGraphics.drawString(this.font, ellipsize(Component.translatable("cobblemon-economy.quest.hover_objectives").getString(), drawW), 0, cursorY, COLOR_MUTED_TEXT, false);
            }
            
            guiGraphics.pose().popPose();

            ResourceLocation tierTexture = resolveTierTexture(selected.rewardPokedollars);
            blitNearest(guiGraphics, tierTexture, left + TIER_X, top + TIER_Y, 0, 0, 68, 11, 68, 11);

            String rewardText = Component.translatable("cobblemon-economy.quest.reward.pokedollars.short", selected.rewardPokedollars).getString();
            int rewardW = this.font.width(rewardText);
            guiGraphics.drawString(this.font, rewardText, left + REWARD_SLOT_X + (68 - rewardW) / 2, top + REWARD_SLOT_Y + 2, COLOR_BODY_TEXT, false);
        }

        if (selectedModelWidget != null) {
            selectedModelWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        } else if (selected != null) {
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
            int hintY = top + GUI_H + 6;
            if (hintY + 9 > this.height) {
                hintY = Math.max(4, top - 12);
            }
            String hintText = ellipsize(interactionHint, GUI_W - 4);
            int hintX = left + (GUI_W - this.font.width(hintText)) / 2;
            guiGraphics.drawString(this.font, hintText, hintX, hintY, COLOR_BODY_TEXT, true);
        }

        if (System.currentTimeMillis() < cancelConfirmUntil) {
            guiGraphics.drawString(this.font, ellipsize(Component.translatable("cobblemon-economy.quest.cancel_confirm").getString(), 126), left + 153, top + 136, COLOR_ALERT_TEXT, false);
        }

        if (hoveredQuestIndex >= 0 && hoveredQuestIndex < state.quests.size()) {
            renderQuestTooltip(guiGraphics, state.quests.get(hoveredQuestIndex), mouseX, mouseY);
        } else if (selected != null && inside(mouseX, mouseY, left + DETAIL_X, top + 43, DETAIL_W, 60)) {
            renderQuestTooltip(guiGraphics, selected, mouseX, mouseY);
        }
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
        return new ModelWidget(x, y, w, h, pokemon.asRenderablePokemon(), scale, 0.0f, 0.0, false, true);
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
        guiGraphics.pose().translate(slotX + 11, slotY + 11, 0);
        guiGraphics.pose().scale(CARD_PREVIEW_ITEM_SCALE, CARD_PREVIEW_ITEM_SCALE, 1.0f);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private void renderSelectedPreviewItem(GuiGraphics guiGraphics, QuestBoardState.QuestCard card, int left, int top) {
        Item item = resolveItem(card != null ? card.previewItem : null);
        ItemStack stack = new ItemStack(item);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left + SELECTED_PREVIEW_ITEM_X, top + SELECTED_PREVIEW_ITEM_Y, 0);
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
