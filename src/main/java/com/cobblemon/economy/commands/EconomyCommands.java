package com.cobblemon.economy.commands;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.EconomyConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class EconomyCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cobeco").executes(EconomyCommands::sendHelp));

        registerPublicCurrencyCommand(dispatcher, "balance", "₽", ChatFormatting.GOLD, true);
        registerPublicCurrencyCommand(dispatcher, "bal", "₽", ChatFormatting.GOLD, true);
        registerPublicCurrencyCommand(dispatcher, "pco", " PCo", ChatFormatting.AQUA, false);

        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                    .executes(EconomyCommands::payPlayer))));

        // Admin commands
        dispatcher.register(Commands.literal("eco")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("item").executes(EconomyCommands::giveTagger))
            .then(Commands.literal("reload").executes(EconomyCommands::reload))
            .then(Commands.literal("skin")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(EconomyCommands::giveSkinSetter)))
            .then(Commands.literal("shop")
                .then(Commands.literal("list").executes(EconomyCommands::listShops))
                .then(Commands.literal("get")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .executes(EconomyCommands::giveShopSetter)))));
    }

    private static int giveSkinSetter(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String skinName = StringArgumentType.getString(ctx, "name");

        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Skin Setter: " + skinName).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("Clic droit sur un marchand").withStyle(ChatFormatting.GRAY),
            Component.literal("pour lui donner le skin de : " + skinName).withStyle(ChatFormatting.GRAY)
        )));
        
        player.getInventory().add(stack);
        ctx.getSource().sendSuccess(() -> Component.literal("Vous avez reçu le Skin Setter pour : " + skinName).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int giveShopSetter(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String shopId = StringArgumentType.getString(ctx, "id");
        
        if (!CobblemonEconomy.getConfig().shops.containsKey(shopId)) {
            ctx.getSource().sendFailure(Component.literal("Boutique '" + shopId + "' inconnue !"));
            return 0;
        }

        ItemStack stack = new ItemStack(Items.NETHER_STAR);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Shop Setter: " + shopId).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("Clic droit sur un marchand").withStyle(ChatFormatting.GRAY),
            Component.literal("pour lui assigner ce shop.").withStyle(ChatFormatting.GRAY)
        )));
        
        player.getInventory().add(stack);
        ctx.getSource().sendSuccess(() -> Component.literal("Vous avez reçu le Setter pour : " + shopId).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int giveTagger(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = new ItemStack(Items.BLAZE_ROD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Tower Tagger").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.getInventory().add(stack);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CobblemonEconomy.reloadConfig();
        ctx.getSource().sendSuccess(() -> Component.literal("Configuration rechargée !").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static void registerPublicCurrencyCommand(CommandDispatcher<CommandSourceStack> dispatcher, String label, String symbol, ChatFormatting color, boolean isMain) {
        dispatcher.register(Commands.literal(label)
            .executes(ctx -> getCurrencyBal(ctx, ctx.getSource().getPlayerOrException(), symbol, color, isMain))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> getCurrencyBal(ctx, EntityArgument.getPlayer(ctx, "player"), symbol, color, isMain))
                .then(Commands.literal("add").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01)).executes(ctx -> modifyCurrency(ctx, label, "give", isMain))))
                .then(Commands.literal("remove").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01)).executes(ctx -> modifyCurrency(ctx, label, "take", isMain))))
                .then(Commands.literal("set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(ctx -> modifyCurrency(ctx, label, "set", isMain))))));
    }

    private static int sendHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("--- Cobblemon Economy ---").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("/bal - Voir son argent").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("/pco - Voir ses points").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("/pay <joueur> <montant> - Payer").withStyle(ChatFormatting.GRAY), false);
        if (source.hasPermission(2)) {
            source.sendSuccess(() -> Component.literal("\n[Admin Commands]").withStyle(ChatFormatting.RED), false);
            source.sendSuccess(() -> Component.literal("/eco reload - Recharger config").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("/eco shop list - Lister les IDs").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("/eco shop get <id> - Objet de config").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("/eco item - Tower Tagger").withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int listShops(CommandContext<CommandSourceStack> context) {
        Map<String, EconomyConfig.ShopDefinition> shops = CobblemonEconomy.getConfig().shops;
        context.getSource().sendSuccess(() -> Component.literal("--- Liste des Boutiques ---").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (String id : shops.keySet()) {
            context.getSource().sendSuccess(() -> Component.literal("- " + id).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int getCurrencyBal(CommandContext<CommandSourceStack> context, ServerPlayer player, String symbol, ChatFormatting color, boolean isMain) {
        BigDecimal balance = isMain ? CobblemonEconomy.getEconomyManager().getBalance(player.getUUID()) : CobblemonEconomy.getEconomyManager().getPco(player.getUUID());
        String label = isMain ? "Pokedollars : " : "PCOs : ";
        String display = balance.stripTrailingZeros().toPlainString() + symbol;
        context.getSource().sendSuccess(() -> Component.literal(label).append(Component.literal(display).withStyle(color)), false);
        return 1;
    }

    private static int payPlayer(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer source = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        BigDecimal amount = BigDecimal.valueOf(DoubleArgumentType.getDouble(context, "amount"));
        if (source.getUUID().equals(target.getUUID())) return 0;
        if (CobblemonEconomy.getEconomyManager().subtractBalance(source.getUUID(), amount)) {
            CobblemonEconomy.getEconomyManager().addBalance(target.getUUID(), amount);
            source.sendSystemMessage(Component.literal("Payé " + amount.stripTrailingZeros().toPlainString() + "₽ à " + target.getName().getString()).withStyle(ChatFormatting.GOLD));
            target.sendSystemMessage(Component.literal("Reçu " + amount.stripTrailingZeros().toPlainString() + "₽ de " + source.getName().getString()).withStyle(ChatFormatting.GOLD));
            return 1;
        }
        return 0;
    }

    private static int modifyCurrency(CommandContext<CommandSourceStack> context, String label, String action, boolean isMain) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        BigDecimal amount = BigDecimal.valueOf(DoubleArgumentType.getDouble(context, "amount"));
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
        context.getSource().sendSuccess(() -> Component.literal("Solde de " + target.getName().getString() + " mis à jour."), true);
        return 1;
    }
}
