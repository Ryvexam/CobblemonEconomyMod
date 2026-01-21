package com.cobblemon.economy.shop;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.EconomyConfig;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;

import java.math.BigDecimal;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class ShopGui {
    private static final int ITEMS_PER_PAGE = 36; // Slots 9 to 44
    private static final Map<UUID, ResolvedShopSession> ACTIVE_SESSIONS = new HashMap<>();

    private static class ResolvedItem {
        Item item;
        int price;
        String name;
        final String originalId;
        final EconomyConfig.ShopItemDefinition definition;

        ResolvedItem(EconomyConfig.ShopItemDefinition def) {
            this.definition = def;
            this.originalId = def.id;
            resolve();
        }

        void resolve() {
            Random rand = new Random();
            if (originalId.contains(":*")) {
                String namespace = originalId.split(":")[0];
                List<Item> candidates = BuiltInRegistries.ITEM.stream()
                    .filter(i -> BuiltInRegistries.ITEM.getKey(i).getNamespace().equals(namespace))
                    .toList();
                
                if (candidates.isEmpty()) {
                    this.item = Items.BARRIER;
                    this.name = "Unknown Namespace: " + namespace;
                } else {
                    this.item = candidates.get(rand.nextInt(candidates.size()));
                    this.name = this.item.getName(new ItemStack(this.item)).getString();
                }
            } else {
                this.item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(originalId));
                this.name = definition.name;
            }

            // Random price +/- 25%
            double multiplier = 0.75 + (rand.nextDouble() * 0.5);
            this.price = (int) Math.round(definition.price * multiplier);
        }
    }

    private static class ResolvedShopSession {
        final String shopId;
        final List<ResolvedItem> resolvedItems;

        ResolvedShopSession(String shopId, EconomyConfig.ShopDefinition shop) {
            this.shopId = shopId;
            this.resolvedItems = new ArrayList<>();
            for (EconomyConfig.ShopItemDefinition itemDef : shop.items) {
                this.resolvedItems.add(new ResolvedItem(itemDef));
            }
        }
    }

    public static void open(ServerPlayer player, String shopId) {
        EconomyConfig.ShopDefinition shop = CobblemonEconomy.getConfig().shops.get(shopId);
        if (shop == null) {
            shop = CobblemonEconomy.getConfig().shops.get("default_poke");
        }
        if (shop == null) return;

        // Create a new resolved session for this player open
        ResolvedShopSession session = new ResolvedShopSession(shopId, shop);
        ACTIVE_SESSIONS.put(player.getUUID(), session);
        
        open(player, shopId, 0);
    }

    public static void open(ServerPlayer player, String shopId, int page) {
        ResolvedShopSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null || !session.shopId.equals(shopId)) {
            open(player, shopId);
            return;
        }

        EconomyConfig.ShopDefinition shop = CobblemonEconomy.getConfig().shops.get(shopId);
        if (shop == null) return;

        boolean isPco = "PCO".equals(shop.currency);
        boolean isSell = shop.isSellShop;
        
        int totalItems = session.resolvedItems.size();
        int maxPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (maxPages == 0) maxPages = 1;
        
        if (page < 0) page = 0;
        if (page >= maxPages) page = maxPages - 1;
        
        final int currentPage = page;
        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < maxPages - 1;

        String titleChar;
        if (isSell) {
            if (hasPrev && hasNext) titleChar = isPco ? "\uE00E" : "\uE00A";
            else if (hasNext) titleChar = isPco ? "\uE00D" : "\uE009";
            else if (hasPrev) titleChar = isPco ? "\uE00F" : "\uE00B";
            else titleChar = isPco ? "\uE00C" : "\uE008";
        } else {
            if (hasPrev && hasNext) titleChar = isPco ? "\uE005" : "\uE004";
            else if (hasNext) titleChar = isPco ? "\uE003" : "\uE002";
            else if (hasPrev) titleChar = isPco ? "\uE007" : "\uE006";
            else titleChar = isPco ? "\uE001" : "\uE000";
        }

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x6, player, false);
        gui.setTitle(Component.literal("\uF804" + titleChar).withStyle(style -> 
            style.withFont(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "default"))
                 .withColor(0xFFFFFF)
        ));

        if (hasPrev) {
            gui.setSlot(0, new GuiElementBuilder(Items.AIR)
                .setName(Component.translatable("cobblemon-economy.shop.return_to_start"))
                .setCallback((index, type, action) -> open(player, shopId, 0))
            );
        }

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int slotIndex = 9 + i;
            int itemIndex = startIndex + i;

            if (itemIndex < endIndex) {
                ResolvedItem resolved = session.resolvedItems.get(itemIndex);
                
                Component actionLabel = Component.translatable(isSell ? "cobblemon-economy.shop.sell_action" : "cobblemon-economy.shop.buy_action");
                Component priceLabel = Component.translatable(isSell ? "cobblemon-economy.shop.sell_label" : "cobblemon-economy.shop.price_label");
                ChatFormatting priceColor = isPco ? ChatFormatting.AQUA : ChatFormatting.GREEN;

                gui.setSlot(slotIndex, new GuiElementBuilder(resolved.item)
                    .setName(Component.literal(resolved.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .addLoreLine(priceLabel.copy().withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(resolved.price + (isPco ? " PCo" : "₽")).withStyle(priceColor)))
                    .addLoreLine(Component.empty())
                    .addLoreLine(actionLabel.copy().withStyle(ChatFormatting.YELLOW))
                    .setCallback((index, type, action) -> {
                        if (isSell) {
                            handleSell(player, resolved, isPco, type.isRight);
                        } else {
                            handlePurchase(player, resolved, isPco);
                        }
                        // Refresh GUI
                        open(player, shopId, currentPage); 
                    })
                );
            }
        }

        for (int i = 45; i <= 48; i++) {
            if (hasPrev) {
                final int prevPage = currentPage - 1;
                gui.setSlot(i, new GuiElementBuilder(Items.AIR).setCallback((index, type, action) -> open(player, shopId, prevPage)));
            }
        }

        BigDecimal balance = isPco ? CobblemonEconomy.getEconomyManager().getPco(player.getUUID()) : CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());
        gui.setSlot(49, new GuiElementBuilder(Items.PLAYER_HEAD)
            .setName(Component.translatable("cobblemon-economy.shop.balance_title").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
            .setSkullOwner(player.getGameProfile(), player.server)
            .setLore(List.of(
                Component.literal(balance.stripTrailingZeros().toPlainString() + (isPco ? " PCo" : "₽")).withStyle(ChatFormatting.WHITE),
                Component.translatable("cobblemon-economy.shop.page_info", currentPage + 1, maxPages).withStyle(ChatFormatting.GRAY)
            ))
        );

        for (int i = 50; i <= 53; i++) {
            if (hasNext) {
                final int nextPage = currentPage + 1;
                gui.setSlot(i, new GuiElementBuilder(Items.AIR).setCallback((index, type, action) -> open(player, shopId, nextPage)));
            }
        }

        gui.open();
    }

    private static void handlePurchase(ServerPlayer player, ResolvedItem resolved, boolean isPco) {
        BigDecimal price = BigDecimal.valueOf(resolved.price);
        boolean success = isPco ? CobblemonEconomy.getEconomyManager().subtractPco(player.getUUID(), price) : CobblemonEconomy.getEconomyManager().subtractBalance(player.getUUID(), price);

        if (success) {
            ItemStack stack = new ItemStack(resolved.item);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.purchase_success", resolved.name).withStyle(ChatFormatting.GREEN));
            logTransaction(player, resolved, isPco, false, 1, price);
            
            // Re-resolve the item for the next purchase
            resolved.resolve();
        } else {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.insufficient_balance").withStyle(ChatFormatting.RED));
        }
    }

    private static void handleSell(ServerPlayer player, ResolvedItem resolved, boolean isPco, boolean sellAll) {
        int totalCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(resolved.item)) {
                totalCount += stack.getCount();
            }
        }

        if (totalCount > 0) {
            int amountToSell = sellAll ? Math.min(totalCount, resolved.item.getDefaultMaxStackSize()) : 1;
            ItemStack toRemove = new ItemStack(resolved.item, amountToSell);
            
            if (removeItem(player, toRemove)) {
                BigDecimal totalPrice = BigDecimal.valueOf(resolved.price).multiply(BigDecimal.valueOf(amountToSell));
                if (isPco) CobblemonEconomy.getEconomyManager().addPco(player.getUUID(), totalPrice);
                else CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), totalPrice);
                
                player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.sell_success", amountToSell, resolved.name, totalPrice, (isPco ? " PCo" : "₽")).withStyle(ChatFormatting.GREEN));
                logTransaction(player, resolved, isPco, true, amountToSell, totalPrice);
                
                // Re-resolve the item for the next sale
                resolved.resolve();
            }
        } else {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.no_item_to_sell").withStyle(ChatFormatting.RED));
        }
    }

    private static boolean removeItem(ServerPlayer player, ItemStack toRemove) {
        int remaining = toRemove.getCount();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItem(stack, toRemove)) {
                int count = stack.getCount();
                int taken = Math.min(count, remaining);
                stack.shrink(taken);
                remaining -= taken;
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    private static void logTransaction(ServerPlayer player, ResolvedItem resolved, boolean isPco, boolean isSell, int quantity, BigDecimal totalPrice) {
        try {
            File modDir = CobblemonEconomy.getModDirectory();
            File logFile = new File(modDir, "transactions.log");
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String currency = isPco ? "PCo" : "₽";
            String type = isSell ? "SELL" : "PURCHASE";
            String logEntry = String.format("[%s] TYPE: %s | PLAYER: %s (%s) | ITEM: %s (%s) | QTY: %d | TOTAL: %s %s\n", 
                timestamp, type, player.getName().getString(), player.getUUID(), resolved.name, resolved.originalId, quantity, totalPrice, currency);
            
            Files.writeString(logFile.toPath(), logEntry, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException e) {
            CobblemonEconomy.LOGGER.error("Failed to log transaction", e);
        }
    }
}
