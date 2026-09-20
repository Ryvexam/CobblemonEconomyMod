package com.cobblemon.economy.commands;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.block.QuestBoardBlock;
import com.cobblemon.economy.questboard.QuestBoardService;
import com.cobblemon.economy.storage.EconomyConfig;
import com.cobblemon.economy.storage.QuestConfig;
import com.cobblemon.economy.storage.QuestNpcConfig;
import com.cobblemon.economy.storage.MonetaryAmount;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.io.File;
import java.util.Optional;

public class EconomyCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cobeco").executes(EconomyCommands::sendHelp));

        registerPublicCurrencyCommand(dispatcher, "balance", "₽", ChatFormatting.GOLD, true);
        registerPublicCurrencyCommand(dispatcher, "bal", "₽", ChatFormatting.GOLD, true);
        registerPublicCurrencyCommand(dispatcher, "pco", " PCo", ChatFormatting.AQUA, false);

        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", StringArgumentType.word())
                    .executes(EconomyCommands::payPlayer))));



        // Admin commands
        dispatcher.register(Commands.literal("eco")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("item").executes(EconomyCommands::giveTagger))
            // Removed redundant subcommand
            .then(Commands.literal("reload").executes(EconomyCommands::reload))
            .then(Commands.literal("skin")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(EconomyCommands::suggestSkins)
                    .executes(EconomyCommands::giveSkinSetter)))
            .then(Commands.literal("shop")
                .then(Commands.literal("list").executes(EconomyCommands::listShops))
                .then(Commands.literal("get")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(EconomyCommands::suggestShopIds)
                        .executes(EconomyCommands::giveShopSetter))))
            .then(Commands.literal("quest")
                .then(Commands.literal("list").executes(EconomyCommands::listQuests)))
            .then(Commands.literal("questnpc")
                .then(Commands.literal("list").executes(EconomyCommands::listQuestNpcs))
                .then(Commands.literal("get")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(EconomyCommands::suggestQuestNpcIds)
                        .executes(EconomyCommands::giveQuestNpcSetter))))
            .then(Commands.literal("questboard")
                .then(Commands.literal("list").executes(EconomyCommands::listQuestNpcs))
                .then(Commands.literal("open")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(EconomyCommands::suggestQuestNpcIds)
                        .executes(EconomyCommands::openQuestBoard)))
                .then(Commands.literal("bind")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(EconomyCommands::suggestQuestNpcIds)
                        .executes(EconomyCommands::bindQuestBoardAtLook)))
                .then(Commands.literal("unbind").executes(EconomyCommands::unbindQuestBoardAtLook))));
    }

    private static CompletableFuture<Suggestions> suggestShopIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(CobblemonEconomy.getConfig().shops.keySet(), builder);
    }

    private static CompletableFuture<Suggestions> suggestSkins(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> skins = new ArrayList<>();
        File modDir = CobblemonEconomy.getModDirectory();
        if (modDir != null) {
            File skinsDir = new File(modDir, "skins");
            if (skinsDir.exists() && skinsDir.isDirectory()) {
                File[] files = skinsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                if (files != null) {
                    for (File f : files) {
                        skins.add(f.getName().substring(0, f.getName().length() - 4));
                    }
                }
            }
        }
        // Also suggest default "shopkeeper"
        skins.add("shopkeeper");
        return SharedSuggestionProvider.suggest(skins, builder);
    }

    private static CompletableFuture<Suggestions> suggestQuestNpcIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        QuestNpcConfig config = CobblemonEconomy.getQuestNpcConfig();
        if (config == null || config.questNpcs == null) {
            return Suggestions.empty();
        }
        return SharedSuggestionProvider.suggest(config.questNpcs.keySet(), builder);
    }

    private static int giveSkinSetter(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String skinName = StringArgumentType.getString(ctx, "name");

        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("cobblemon-economy.item.skin_setter.name", skinName).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("cobblemon-economy.item.skin_setter.lore1").withStyle(ChatFormatting.GRAY),
            Component.translatable("cobblemon-economy.item.skin_setter.lore2", skinName).withStyle(ChatFormatting.GRAY)
        )));
        
        player.getInventory().add(stack);
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.notification.skin_updated").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int giveShopSetter(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String shopId = StringArgumentType.getString(ctx, "id");
        
        if (CobblemonEconomy.getConfig().shops.containsKey(shopId)) {
            // Found by ID directly
        } else {
            // Check translation/display name fallback? No, commands usually use IDs.
            // Just strict ID check as before.
            if (!CobblemonEconomy.getConfig().shops.containsKey(shopId)) {
                ctx.getSource().sendFailure(Component.translatable("cobblemon-economy.command.shop.unknown", shopId));
                return 0;
            }
        }

        ItemStack stack = new ItemStack(Items.NETHER_STAR);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("cobblemon-economy.item.shop_setter.name", shopId).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        
        // Add internal NBT tag for language-agnostic identification
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("ShopSetterId", shopId);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));

        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("cobblemon-economy.item.shop_setter.lore1").withStyle(ChatFormatting.GRAY),
            Component.translatable("cobblemon-economy.item.shop_setter.lore2").withStyle(ChatFormatting.GRAY)
        )));
        
        player.getInventory().add(stack);
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.notification.shopkeeper_set", shopId).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int giveQuestNpcSetter(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String npcId = StringArgumentType.getString(ctx, "id");
        QuestNpcConfig config = CobblemonEconomy.getQuestNpcConfig();
        if (config == null || config.questNpcs == null || !config.questNpcs.containsKey(npcId)) {
            ctx.getSource().sendFailure(Component.translatable("cobblemon-economy.command.quest_npc.unknown", npcId));
            return 0;
        }

        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("cobblemon-economy.item.quest_npc_setter.name", npcId).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("QuestNpcSetterId", npcId);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));

        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("cobblemon-economy.item.quest_npc_setter.lore1").withStyle(ChatFormatting.GRAY),
                Component.translatable("cobblemon-economy.item.quest_npc_setter.lore2", npcId).withStyle(ChatFormatting.GRAY)
        )));

        player.getInventory().add(stack);
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.notification.quest_npc_set", npcId).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int giveTagger(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = new ItemStack(Items.BLAZE_ROD);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("cobblemon-economy.item.tower_tagger.name").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.getInventory().add(stack);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CobblemonEconomy.reloadConfig();
        ctx.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.command.reload.success").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static void registerPublicCurrencyCommand(CommandDispatcher<CommandSourceStack> dispatcher, String label, String symbol, ChatFormatting color, boolean isMain) {
        dispatcher.register(Commands.literal(label)
            .executes(ctx -> getCurrencyBal(ctx, ctx.getSource().getPlayerOrException(), symbol, color, isMain))
            .then(Commands.literal("top").executes(ctx -> sendTop(ctx, symbol, color, isMain)))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> getCurrencyBal(ctx, EntityArgument.getPlayer(ctx, "player"), symbol, color, isMain))
                .then(Commands.literal("add").then(Commands.argument("amount", StringArgumentType.word()).executes(ctx -> modifyCurrency(ctx, label, "give", isMain))))
                .then(Commands.literal("remove").then(Commands.argument("amount", StringArgumentType.word()).executes(ctx -> modifyCurrency(ctx, label, "take", isMain))))
                .then(Commands.literal("set").then(Commands.argument("amount", StringArgumentType.word()).executes(ctx -> modifyCurrency(ctx, label, "set", isMain))))));
    }

    private static int sendTop(CommandContext<CommandSourceStack> context, String symbol, ChatFormatting color, boolean isMain) {
        List<Map.Entry<UUID, BigDecimal>> topList = isMain 
            ? CobblemonEconomy.getEconomyManager().getTopBalance(10) 
            : CobblemonEconomy.getEconomyManager().getTopPco(10);
            
        context.getSource().sendSuccess(() -> Component.translatable(
            isMain ? "cobblemon-economy.command.balance.top.title" : "cobblemon-economy.command.pco.top.title"
        ).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        int rank = 1;
        for (Map.Entry<UUID, BigDecimal> entry : topList) {
            Optional<com.mojang.authlib.GameProfile> profileOpt = context.getSource().getServer().getProfileCache().get(entry.getKey());
            String name = profileOpt.map(com.mojang.authlib.GameProfile::getName).orElse(entry.getKey().toString());
                
            String amount = entry.getValue().stripTrailingZeros().toPlainString() + symbol;
            
            final int currentRank = rank;
            context.getSource().sendSuccess(() -> Component.translatable(
                "cobblemon-economy.command.top.entry",
                currentRank,
                name,
                amount
            ).withStyle(ChatFormatting.YELLOW), false);
            rank++;
        }
        return 1;
    }

    private static int sendHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.bal").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.pco").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.pay").withStyle(ChatFormatting.GRAY), false);

        if (source.hasPermission(2)) {
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.admin_title").withStyle(ChatFormatting.RED), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.reload").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.shop_list").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.shop_get").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.quest_list").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.questnpc_list").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.questnpc_get").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.questboard_list").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.questboard_open").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.questboard_bind").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.questboard_unbind").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.skin").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("cobblemon-economy.command.help.item").withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int openQuestBoard(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String boardId = StringArgumentType.getString(context, "id");
        if (!QuestBoardService.openBoard(player, boardId)) {
            context.getSource().sendFailure(Component.translatable("cobblemon-economy.command.quest_npc.unknown", boardId));
            return 0;
        }
        return 1;
    }

    private static int bindQuestBoardAtLook(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String boardId = StringArgumentType.getString(context, "id");
        HitResult hit = player.pick(6.0, 0.0F, false);
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            context.getSource().sendFailure(Component.translatable("cobblemon-economy.command.questboard.look_block"));
            return 0;
        }
        var bindPos = bhr.getBlockPos();
        var state = player.level().getBlockState(bindPos);
        if (state.getBlock() instanceof QuestBoardBlock) {
            bindPos = QuestBoardBlock.findOrigin(player.level(), bindPos, state);
        }
        String dimension = player.level().dimension().location().toString();
        CobblemonEconomy.bindBoardId(dimension, bindPos.getX(), bindPos.getY(), bindPos.getZ(), boardId);
        context.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.command.questboard.bound", boardId).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int unbindQuestBoardAtLook(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        HitResult hit = player.pick(6.0, 0.0F, false);
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            context.getSource().sendFailure(Component.translatable("cobblemon-economy.command.questboard.look_block"));
            return 0;
        }
        var bindPos = bhr.getBlockPos();
        var state = player.level().getBlockState(bindPos);
        if (state.getBlock() instanceof QuestBoardBlock) {
            bindPos = QuestBoardBlock.findOrigin(player.level(), bindPos, state);
        }
        String dimension = player.level().dimension().location().toString();
        CobblemonEconomy.bindBoardId(dimension, bindPos.getX(), bindPos.getY(), bindPos.getZ(), null);
        context.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.command.questboard.unbound").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int listShops(CommandContext<CommandSourceStack> context) {
        Map<String, EconomyConfig.ShopDefinition> shops = CobblemonEconomy.getConfig().shops;
        context.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.command.shop.list_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (String id : shops.keySet()) {
            context.getSource().sendSuccess(() -> Component.literal("- " + id).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int listQuests(CommandContext<CommandSourceStack> context) {
        QuestConfig config = CobblemonEconomy.getQuestConfig();
        context.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.command.quest.list_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        if (config == null || config.quests == null || config.quests.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.command.quest.empty").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        for (String id : config.quests.keySet()) {
            context.getSource().sendSuccess(() -> Component.literal("- " + id).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int listQuestNpcs(CommandContext<CommandSourceStack> context) {
        QuestNpcConfig config = CobblemonEconomy.getQuestNpcConfig();
        context.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.command.quest_npc.list_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        if (config == null || config.questNpcs == null || config.questNpcs.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.command.quest_npc.empty").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        for (String id : config.questNpcs.keySet()) {
            context.getSource().sendSuccess(() -> Component.literal("- " + id).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int getCurrencyBal(CommandContext<CommandSourceStack> context, ServerPlayer player, String symbol, ChatFormatting color, boolean isMain) {
        CobblemonEconomy.getEconomyManager().updateUsername(player.getUUID(), player.getGameProfile().getName());
        BigDecimal balance = isMain ? CobblemonEconomy.getEconomyManager().getBalance(player.getUUID()) : CobblemonEconomy.getEconomyManager().getPco(player.getUUID());
        Component label = Component.translatable(isMain ? "cobblemon-economy.command.balance.label" : "cobblemon-economy.command.pco.label");
        String display = balance.stripTrailingZeros().toPlainString() + symbol;
        context.getSource().sendSuccess(() -> label.copy().append(Component.literal(display).withStyle(color)), false);
        return 1;
    }

    private static int payPlayer(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer source = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        BigDecimal amount = parseAmount(context);
        if (amount == null || amount.signum() == 0) {
            return 0;
        }
        CobblemonEconomy.getEconomyManager().updateUsername(source.getUUID(), source.getGameProfile().getName());
        CobblemonEconomy.getEconomyManager().updateUsername(target.getUUID(), target.getGameProfile().getName());
        if (source.getUUID().equals(target.getUUID())) return 0;
        if (CobblemonEconomy.getEconomyManager().subtractBalance(source.getUUID(), amount)) {
            CobblemonEconomy.getEconomyManager().addBalance(target.getUUID(), amount);
            source.sendSystemMessage(Component.translatable("cobblemon-economy.command.pay.success", amount.stripTrailingZeros().toPlainString(), target.getName().getString()).withStyle(ChatFormatting.GOLD));
            target.sendSystemMessage(Component.translatable("cobblemon-economy.command.pay.received", amount.stripTrailingZeros().toPlainString(), source.getName().getString()).withStyle(ChatFormatting.GOLD));
            return 1;
        }
        return 0;
    }

    private static int modifyCurrency(CommandContext<CommandSourceStack> context, String label, String action, boolean isMain) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        BigDecimal amount = parseAmount(context);
        if (amount == null) {
            return 0;
        }
        CobblemonEconomy.getEconomyManager().updateUsername(target.getUUID(), target.getGameProfile().getName());
        if (isMain) {
            switch (action) {
                case "give" -> CobblemonEconomy.getEconomyManager().addBalance(target.getUUID(), amount);
                case "take" -> CobblemonEconomy.getEconomyManager().subtractBalance(target.getUUID(), amount);
                case "set" -> CobblemonEconomy.getEconomyManager().setBalance(target.getUUID(), amount);
            }
        } else {
            switch (action) {
                case "give" -> CobblemonEconomy.getEconomyManager().addPco(target.getUUID(), amount);
                case "take" -> CobblemonEconomy.getEconomyManager().subtractPco(target.getUUID(), amount);
                case "set" -> CobblemonEconomy.getEconomyManager().setPco(target.getUUID(), amount);
            }
        }
        context.getSource().sendSuccess(() -> Component.translatable("cobblemon-economy.command.balance.update", target.getName().getString()), true);
        return 1;
    }

    private static BigDecimal parseAmount(CommandContext<CommandSourceStack> context) {
        try {
            return MonetaryAmount.parse(StringArgumentType.getString(context, "amount"));
        } catch (IllegalArgumentException error) {
            context.getSource().sendFailure(Component.translatable("cobblemon-economy.command.amount.invalid"));
            return null;
        }
    }
}
