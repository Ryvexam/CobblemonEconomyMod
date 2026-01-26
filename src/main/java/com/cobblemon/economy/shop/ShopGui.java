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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import java.math.BigDecimal;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class ShopGui {
    private static final int ITEMS_PER_PAGE = 36; // Slots 9 to 44
    private static final Map<UUID, ResolvedShopSession> ACTIVE_SESSIONS = new HashMap<>();

    private static class ResolvedItem {
        ItemStack templateStack; // Stores item + components
        Item item;
        int price;
        String name;
        int quantity = 1; // Default quantity
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
                    this.templateStack = new ItemStack(this.item);
                } else {
                    this.item = candidates.get(rand.nextInt(candidates.size()));
                    this.templateStack = new ItemStack(this.item);
                    this.name = this.templateStack.getHoverName().getString();
                }

                // Random price +/- 25% ONLY for wildcards
                double multiplier = 0.75 + (rand.nextDouble() * 0.5);
                this.price = (int) Math.round(definition.price * multiplier);
            } else {
                // Try to parse using ItemStringReader to support components [foo=bar]
                try {
                    // Check if ID contains '[' indicating component syntax
                    if (originalId.contains("[")) {
                        var registries = CobblemonEconomy.getGameServer().registryAccess();
                        
                        // Use Reflection with fallback for Intermediary names (Production environment)
                        Class<?> argTypeClass;
                        try {
                            argTypeClass = Class.forName("net.minecraft.command.argument.ItemStackArgumentType");
                        } catch (ClassNotFoundException e) {
                            argTypeClass = Class.forName("net.minecraft.class_2282"); // Intermediary
                        }

                        Class<?> commandRegistryAccessClass;
                        try {
                            commandRegistryAccessClass = Class.forName("net.minecraft.command.CommandRegistryAccess");
                        } catch (ClassNotFoundException e) {
                            commandRegistryAccessClass = Class.forName("net.minecraft.class_7157"); // Intermediary
                        }
                        
                        java.lang.reflect.Method itemStackMethod;
                        try {
                            itemStackMethod = argTypeClass.getMethod("itemStack", commandRegistryAccessClass);
                        } catch (NoSuchMethodException e) {
                            itemStackMethod = argTypeClass.getMethod("method_9774", commandRegistryAccessClass); // Intermediary
                        }
                        Object argTypeInstance = itemStackMethod.invoke(null, registries);
                        
                        java.lang.reflect.Method parseMethod;
                        try {
                            parseMethod = argTypeClass.getMethod("parse", com.mojang.brigadier.StringReader.class);
                        } catch (NoSuchMethodException e) {
                            parseMethod = argTypeClass.getMethod("method_9776", com.mojang.brigadier.StringReader.class); // Intermediary
                        }
                        Object argumentInstance = parseMethod.invoke(argTypeInstance, new com.mojang.brigadier.StringReader(originalId));
                        
                        Class<?> argumentClass;
                        try {
                            argumentClass = Class.forName("net.minecraft.command.argument.ItemStackArgument");
                        } catch (ClassNotFoundException e) {
                            argumentClass = Class.forName("net.minecraft.class_2287"); // Intermediary
                        }

                        java.lang.reflect.Method createStackMethod;
                        try {
                            createStackMethod = argumentClass.getMethod("createStack", int.class, boolean.class);
                        } catch (NoSuchMethodException e) {
                            createStackMethod = argumentClass.getMethod("method_9781", int.class, boolean.class); // Intermediary
                        }
                        
                        this.templateStack = (ItemStack) createStackMethod.invoke(argumentInstance, 1, false);
                        this.item = this.templateStack.getItem();
                    } else {
                        // Standard lookup
                        this.item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(originalId));
                        this.templateStack = new ItemStack(this.item);
                    }
                } catch (Exception e) {
                    CobblemonEconomy.LOGGER.error("Failed to parse item ID: " + originalId, e);
                    this.item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(originalId.split("\\[")[0])); // Fallback to base item
                    this.templateStack = new ItemStack(this.item);
                }

                this.name = definition.name;
                this.price = definition.price;
            }
        }
    }

    private static class ResolvedShopSession {
        final String shopId;
        final List<ResolvedItem> resolvedItems;

        ResolvedShopSession(String shopId, EconomyConfig.ShopDefinition shop) {
            this.shopId = shopId;
            this.resolvedItems = new ArrayList<>();
            if (shop.items != null) {
                for (EconomyConfig.ShopItemDefinition itemDef : shop.items) {
                    if (itemDef != null && itemDef.id != null) {
                        this.resolvedItems.add(new ResolvedItem(itemDef));
                    } else {
                        CobblemonEconomy.LOGGER.warn("Shop '{}' contains a null item or an item with missing ID!", shopId);
                    }
                }
            } else {
                CobblemonEconomy.LOGGER.warn("Shop '{}' has no items list defined!", shopId);
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
        
        String shopTitle = shop.title != null ? shop.title : "Shop";
        String negativeSpace = "\uF804".repeat(21);
        
        gui.setTitle(Component.literal("\uF804" + titleChar)
            .withStyle(style -> 
                style.withFont(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "default"))
                     .withColor(0xFFFFFF)
            )
            .append(Component.literal(negativeSpace)
                .withStyle(style -> style.withFont(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "default"))))
            .append(Component.literal(shopTitle)
                .withStyle(style -> style.withFont(ResourceLocation.withDefaultNamespace("default"))))
        );

        // Add linked shop switch button in slot 8 (top right corner)
        if (shop.linkedShop != null && !shop.linkedShop.isEmpty()) {
            EconomyConfig.ShopDefinition linkedShop = CobblemonEconomy.getConfig().shops.get(shop.linkedShop);
            if (linkedShop != null) {
                String linkedTitle = linkedShop.title != null ? linkedShop.title : "Linked Shop";
                Item switchIcon = linkedShop.isSellShop ? Items.EMERALD : Items.GOLD_INGOT;
                
                gui.setSlot(8, new GuiElementBuilder(switchIcon)
                    .setName(Component.literal("⇄ Switch Shop").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                    .addLoreLine(Component.literal(linkedTitle).withStyle(ChatFormatting.YELLOW))
                    .addLoreLine(Component.empty())
                    .addLoreLine(Component.literal("Click to switch").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                    .setCallback((index, type, action) -> {
                        player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
                        open(player, shop.linkedShop, 0);
                    })
                );
            }
        }

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
                
                Component actionLabel = Component.translatable(
                        isSell ? "cobblemon-economy.shop.sell_action" : "cobblemon-economy.shop.buy_action",
                        isSell ? resolved.quantity : resolved.quantity, // First arg for Sell (Sell X), ignored for Buy? Wait, Buy needs logic.
                        // Actually, let's make it generic.
                        // Buy: "Left: Buy | Middle: x{quantity}"
                        // Sell: "Left: Sell {quantity} | Right: Sell All | Middle: x{quantity}"
                        resolved.quantity // Second arg for Sell (Middle click hint), or first for Buy (Middle click hint)
                );
                
                // Re-doing the logic cleaner:
                if (isSell) {
                     actionLabel = Component.translatable("cobblemon-economy.shop.sell_action", resolved.quantity, resolved.quantity);
                } else {
                     actionLabel = Component.translatable("cobblemon-economy.shop.buy_action", resolved.quantity);
                }

                Component priceLabel = Component.translatable(isSell ? "cobblemon-economy.shop.sell_label" : "cobblemon-economy.shop.price_label");
                ChatFormatting priceColor = isPco ? ChatFormatting.AQUA : ChatFormatting.GREEN;

                // Calculate total price for current quantity
                long totalPrice = (long) resolved.price * resolved.quantity;
                
                ItemStack displayStack = resolved.templateStack.copy();
                displayStack.setCount(resolved.quantity);

                gui.setSlot(slotIndex, new GuiElementBuilder(displayStack)
                    .setName(Component.literal(resolved.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .addLoreLine(priceLabel.copy().withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(totalPrice + (isPco ? " PCo" : "₽")).withStyle(priceColor)))
                    .addLoreLine(Component.literal("x" + resolved.quantity).withStyle(ChatFormatting.WHITE))
                    .addLoreLine(Component.empty())
                    .addLoreLine(actionLabel.copy().withStyle(ChatFormatting.YELLOW))
                    // Removed the separate middle click line, integrated into actionLabel
                    .setCallback((index, type, action) -> {
                        if (type.isMiddle) {
                            // Rotate quantity: 1 -> 2 -> 4 -> 8 -> 16 -> 32 -> 64 -> 1
                            if (resolved.quantity >= 64) resolved.quantity = 1;
                            else resolved.quantity *= 2;
                            
                            // Play sound for feedback
                            player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f);
                        } else {
                            if (isSell) {
                                handleSell(player, resolved, isPco, type.isRight);
                            } else {
                                handlePurchase(player, resolved, isPco);
                            }
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
        BigDecimal price = BigDecimal.valueOf(resolved.price).multiply(BigDecimal.valueOf(resolved.quantity));
        boolean success = isPco ? CobblemonEconomy.getEconomyManager().subtractPco(player.getUUID(), price) : CobblemonEconomy.getEconomyManager().subtractBalance(player.getUUID(), price);

        if (success) {
            ItemStack stackToGive;

            // Check for Minecraft Loot Table (native loot table system)
            // This uses Minecraft's built-in loot table JSON files (e.g., "minecraft:chests/simple_dungeon")
            if (resolved.definition.lootTable != null && !resolved.definition.lootTable.isEmpty()) {
                // Use Minecraft's native loot table system
                // For each quantity, we roll the loot table once
                for (int i = 0; i < resolved.quantity; i++) {
                    try {
                        ServerLevel level = player.serverLevel();
                        ResourceLocation lootTableId = ResourceLocation.parse(resolved.definition.lootTable);
                        ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, lootTableId);

                        LootTable lootTable = level.getServer().reloadableRegistries()
                            .getLootTable(lootTableKey);

                        if (lootTable == LootTable.EMPTY) {
                            CobblemonEconomy.LOGGER.warn("Loot table not found: " + resolved.definition.lootTable);
                            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.lootbox_error").withStyle(ChatFormatting.RED));
                            continue;
                        }

                        // Build loot parameters with player context
                        // Using GIFT context which requires THIS_ENTITY and ORIGIN
                        LootParams lootParams = new LootParams.Builder(level)
                            .withParameter(LootContextParams.THIS_ENTITY, player)
                            .withParameter(LootContextParams.ORIGIN, player.position())
                            .withLuck(player.getLuck())
                            .create(LootContextParamSets.GIFT);

                        // Generate random loot from the table
                        List<ItemStack> loot = lootTable.getRandomItems(lootParams);

                        if (loot.isEmpty()) {
                            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.lootbox_empty").withStyle(ChatFormatting.YELLOW));
                        } else {
                            for (ItemStack lootStack : loot) {
                                if (!player.getInventory().add(lootStack.copy())) {
                                    player.drop(lootStack.copy(), false);
                                }
                                player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.lootbox_open", lootStack.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                            }
                        }
                    } catch (Exception e) {
                        CobblemonEconomy.LOGGER.error("Failed to generate loot from table: " + resolved.definition.lootTable, e);
                        player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.lootbox_error").withStyle(ChatFormatting.RED));
                    }
                }
            }
            // Check for Lootbox/DropTable (simple list of item IDs - legacy/simple approach)
            else if (resolved.definition.dropTable != null && !resolved.definition.dropTable.isEmpty()) {
                // For lootboxes, we give ONE random item per purchase count? Or one bulk?
                // Typically lootboxes are opened one by one.
                // If quantity > 1, we should probably give 'quantity' items.
                // But resolved.definition.dropTable means the item IS a lootbox in concept (or rather, buying it gives the drop).

                // Let's iterate for quantity
                for (int i = 0; i < resolved.quantity; i++) {
                    String randomId = resolved.definition.dropTable.get(new Random().nextInt(resolved.definition.dropTable.size()));
                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(randomId));
                    stackToGive = new ItemStack(item);
                    // Lootbox drops don't usually inherit the NBT of the "crate" item in config,
                    // but the crate itself isn't given.

                    if (!player.getInventory().add(stackToGive)) {
                        player.drop(stackToGive, false);
                    }
                    player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.lootbox_open", stackToGive.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                }

                // We've handled giving items in the loop.
                // Skip the "else" block logic for standard items.
                // Skip the NBT logic below? The NBT logic applies to the "sold item".
                // If dropTable is present, the "sold item" is the drop.
                // But here we might have multiple drops.

                // NOTE: The original code logic was:
                // stackToGive = new ItemStack(item); (random drop)
                // THEN apply NBT from definition to it.
                // THEN give it.

                // If I have a loop, I should probably apply NBT to each drop?
                // But usually dropTable items are raw. NBT in config is usually for the display item or the fixed item.

                // Let's stick to simple logic: If dropTable, repeat logic quantity times.
                // BUT the code structure needs to support "stackToGive" variable which implies single stack.

                // Refactoring handlePurchase to support quantity properly for lootboxes is tricky without changing structure.
                // Let's assume for LOOTBOXES, quantity applies to the number of pulls.

            } else {
                stackToGive = resolved.templateStack.copy();
                stackToGive.setCount(resolved.quantity);

                // Apply NBT (CustomData) if present in definition (1.20.5+ way)
                if (resolved.definition.nbt != null && !resolved.definition.nbt.isEmpty()) {
                    try {
                        CompoundTag nbt = TagParser.parseTag(resolved.definition.nbt);
                        stackToGive.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                    } catch (Exception e) {
                        CobblemonEconomy.LOGGER.error("Failed to parse NBT for item " + resolved.originalId, e);
                    }
                }

                if (!player.getInventory().add(stackToGive)) {
                    player.drop(stackToGive, false);
                }
            }

            player.sendSystemMessage(Component.translatable("cobblemon-economy.shop.purchase_success", resolved.quantity + "x " + resolved.name).withStyle(ChatFormatting.GREEN));
            logTransaction(player, resolved, isPco, false, resolved.quantity, price);

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
            // Sell All (Right Click) vs Sell Quantity (Left Click)
            int amountToSell;
            if (sellAll) {
                amountToSell = Math.min(totalCount, resolved.item.getDefaultMaxStackSize() * 36); // Cap at inventory size? Just totalCount.
                amountToSell = totalCount;
            } else {
                // Sell current quantity selection
                amountToSell = Math.min(totalCount, resolved.quantity);
            }
            
            ItemStack toRemove = resolved.templateStack.copy();
            toRemove.setCount(amountToSell);
            
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
