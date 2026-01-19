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

public class ShopGui {
    private static final int ITEMS_PER_PAGE = 36; // Slots 9 to 44

    public static void open(ServerPlayer player, String shopId) {
        open(player, shopId, 0);
    }

    public static void open(ServerPlayer player, String shopId, int page) {
        EconomyConfig.ShopDefinition shop = CobblemonEconomy.getConfig().shops.get(shopId);
        if (shop == null) {
            shop = CobblemonEconomy.getConfig().shops.get("default_poke");
        }
        if (shop == null) return;

        boolean isPco = "PCO".equals(shop.currency);
        
        // Calcul des pages
        int totalItems = shop.items.size();
        int maxPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (maxPages == 0) maxPages = 1;
        
        // Sécurité page
        if (page < 0) page = 0;
        if (page >= maxPages) page = maxPages - 1;
        
        final int currentPage = page; // Variable finale pour les lambdas

        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < maxPages - 1;

        // Choix de l'image (Background)
        String titleChar;
        if (hasPrev && hasNext) {
            titleChar = isPco ? "\uE005" : "\uE004"; // 2.png
        } else if (hasNext) {
            titleChar = isPco ? "\uE003" : "\uE002"; // 1.png
        } else if (hasPrev) {
            titleChar = isPco ? "\uE007" : "\uE006"; // 3.png
        } else {
            titleChar = isPco ? "\uE001" : "\uE000"; // 0.png
        }

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x6, player, false);
        
        // Titre avec décalage
        String offset = "\uF804"; 
        gui.setTitle(Component.literal(offset + titleChar).withStyle(style -> 
            style.withFont(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "default"))
                 .withColor(0xFFFFFF)
        ));

        // Remplissage de la PREMIÈRE rangée (Slots 0-8)
        // Slot 0 : Retour début (Invisible mais cliquable si page > 0)
        if (hasPrev) {
            gui.setSlot(0, new GuiElementBuilder(Items.AIR)
                .setCallback((index, type, action) -> open(player, shopId, 0))
            );
        } else {
            gui.setSlot(0, new GuiElementBuilder(Items.AIR).setCallback((index, type, action) -> {}));
        }
        
        // Les autres slots de la rangée 0 sont vides
        for (int i = 1; i < 9; i++) {
            gui.setSlot(i, new GuiElementBuilder(Items.AIR).setCallback((index, type, action) -> {}));
        }

        // Remplissage des ITEMS (Slots 9-44)
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int slotIndex = 9 + i;
            int itemIndex = startIndex + i;

            if (itemIndex < endIndex) {
                EconomyConfig.ShopItemDefinition itemDef = shop.items.get(itemIndex);
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemDef.id));
                
                gui.setSlot(slotIndex, new GuiElementBuilder(item)
                    .setName(Component.literal(itemDef.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .addLoreLine(Component.literal("Prix : ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(itemDef.price + (isPco ? " PCo" : "₽")).withStyle(isPco ? ChatFormatting.AQUA : ChatFormatting.GREEN)))
                    .addLoreLine(Component.literal(""))
                    .addLoreLine(Component.literal("▶ Clic pour acheter").withStyle(ChatFormatting.YELLOW))
                    .setCallback((index, type, action) -> {
                        handlePurchase(player, itemDef, isPco);
                        // On réouvre à la même page pour actualiser le solde
                        open(player, shopId, currentPage); 
                    })
                );
            } else {
                // Vide si plus d'items
                gui.setSlot(slotIndex, new GuiElementBuilder(Items.AIR).setCallback((index, type, action) -> {}));
            }
        }

        // DERNIÈRE RANGÉE (Slots 45-53)
        // Gauche (45-48) : Page Précédente
        for (int i = 45; i <= 48; i++) {
            if (hasPrev) {
                final int prevPage = currentPage - 1;
                gui.setSlot(i, new GuiElementBuilder(Items.AIR) // Invisible mais cliquable
                    .setCallback((index, type, action) -> open(player, shopId, prevPage))
                );
            } else {
                gui.setSlot(i, new GuiElementBuilder(Items.AIR).setCallback((index, type, action) -> {}));
            }
        }

        // Milieu (49) : Solde (Tête du joueur)
        BigDecimal balance = isPco ? CobblemonEconomy.getEconomyManager().getPco(player.getUUID()) : CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());
        gui.setSlot(49, new GuiElementBuilder(Items.PLAYER_HEAD)
            .setName(Component.literal("Votre Solde").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
            .setSkullOwner(player.getGameProfile(), player.server)
            .setLore(java.util.List.of(
                Component.literal(balance.stripTrailingZeros().toPlainString() + (isPco ? " PCo" : "₽")).withStyle(ChatFormatting.WHITE),
                Component.literal("Page " + (currentPage + 1) + "/" + maxPages).withStyle(ChatFormatting.GRAY)
            ))
            .setCallback((index, type, action) -> {})
        );

        // Droite (50-53) : Page Suivante
        for (int i = 50; i <= 53; i++) {
            if (hasNext) {
                final int nextPage = currentPage + 1;
                gui.setSlot(i, new GuiElementBuilder(Items.AIR) // Invisible mais cliquable
                    .setCallback((index, type, action) -> open(player, shopId, nextPage))
                );
            } else {
                gui.setSlot(i, new GuiElementBuilder(Items.AIR).setCallback((index, type, action) -> {}));
            }
        }

        gui.open();
    }

    private static void handlePurchase(ServerPlayer player, EconomyConfig.ShopItemDefinition itemDef, boolean isPco) {
        BigDecimal price = BigDecimal.valueOf(itemDef.price);
        boolean success = isPco ? CobblemonEconomy.getEconomyManager().subtractPco(player.getUUID(), price) : CobblemonEconomy.getEconomyManager().subtractBalance(player.getUUID(), price);

        if (success) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemDef.id));
            ItemStack stack = new ItemStack(item);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.sendSystemMessage(Component.literal("✔ Achat réussi : " + itemDef.name).withStyle(ChatFormatting.GREEN));
            
            // Log de la transaction
            logTransaction(player, itemDef, isPco);
        } else {
            player.sendSystemMessage(Component.literal("✖ Solde insuffisant !").withStyle(ChatFormatting.RED));
        }
    }

    private static void logTransaction(ServerPlayer player, EconomyConfig.ShopItemDefinition itemDef, boolean isPco) {
        try {
            java.io.File worldDir = player.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            java.io.File logFile = new java.io.File(new java.io.File(worldDir, "cobblemon-economy"), "transactions.log");
            
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String currency = isPco ? "PCo" : "₽";
            String logEntry = String.format("[%s] PLAYER: %s (%s) | PURCHASE: %s (%s) | PRICE: %d %s\n", 
                timestamp, player.getName().getString(), player.getUUID(), itemDef.name, itemDef.id, itemDef.price, currency);
            
            java.nio.file.Files.writeString(logFile.toPath(), logEntry, 
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException e) {
            CobblemonEconomy.LOGGER.error("Failed to log transaction", e);
        }
    }
}
