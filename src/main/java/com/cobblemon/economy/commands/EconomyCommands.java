package com.cobblemon.economy.commands;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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

public class EconomyCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /balance or /bal
        registerCurrencyCommand(dispatcher, "balance", "₽", ChatFormatting.GOLD, true);
        registerCurrencyCommand(dispatcher, "bal", "₽", ChatFormatting.GOLD, true);

        // /pco (Points de Combat)
        registerCurrencyCommand(dispatcher, "pco", " PCo", ChatFormatting.AQUA, false);

        // /pay <player> <amount>
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                    .executes(EconomyCommands::payPlayer))));

        // Admin commands: /eco item | /eco reload
        dispatcher.register(Commands.literal("eco")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("item")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack stack = new ItemStack(Items.BLAZE_ROD);
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal("Tower Tagger").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                    stack.set(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal("Clic droit sur un PNJ").withStyle(ChatFormatting.GRAY),
                        Component.literal("pour activer le tag Tour de Combat.").withStyle(ChatFormatting.GRAY)
                    )));
                    player.getInventory().add(stack);
                    return 1;
                }))
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    CobblemonEconomy.reloadConfig();
                    ctx.getSource().sendSuccess(() -> Component.literal("Configuration rechargée !").withStyle(ChatFormatting.GREEN), true);
                    return 1;
                })));
    }

    private static void registerCurrencyCommand(CommandDispatcher<CommandSourceStack> dispatcher, String label, String symbol, ChatFormatting color, boolean isMain) {
        dispatcher.register(Commands.literal(label)
            .executes(ctx -> getCurrencyBal(ctx, ctx.getSource().getPlayerOrException(), symbol, color, isMain))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> getCurrencyBal(ctx, EntityArgument.getPlayer(ctx, "player"), symbol, color, isMain))
                .then(Commands.literal("add")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> modifyCurrency(ctx, label, "give", isMain))))
                .then(Commands.literal("remove")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> modifyCurrency(ctx, label, "take", isMain))))
                .then(Commands.literal("set")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> modifyCurrency(ctx, label, "set", isMain))))));
    }

    private static int getCurrencyBal(CommandContext<CommandSourceStack> context, ServerPlayer player, String symbol, ChatFormatting color, boolean isMain) {
        BigDecimal balance = isMain ? 
            CobblemonEconomy.getEconomyManager().getBalance(player.getUUID()) :
            CobblemonEconomy.getEconomyManager().getPco(player.getUUID());
            
        String label = isMain ? "Pokedollars : " : "PCOs : ";
        // formatage propre ici aussi
        String display = balance.stripTrailingZeros().toPlainString() + symbol;

        context.getSource().sendSuccess(() -> Component.literal(label)
            .append(Component.literal(display).withStyle(color)), false);
        return 1;
    }

    private static int payPlayer(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer source = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        BigDecimal amount = BigDecimal.valueOf(DoubleArgumentType.getDouble(context, "amount"));

        if (source.getUUID().equals(target.getUUID())) {
            context.getSource().sendFailure(Component.literal("Vous ne pouvez pas vous payer vous-même !"));
            return 0;
        }

        if (CobblemonEconomy.getEconomyManager().subtractBalance(source.getUUID(), amount)) {
            CobblemonEconomy.getEconomyManager().addBalance(target.getUUID(), amount);
            source.sendSystemMessage(Component.literal("Vous avez payé " + amount.stripTrailingZeros().toPlainString() + "₽ à " + target.getName().getString()).withStyle(ChatFormatting.GOLD));
            target.sendSystemMessage(Component.literal("Vous avez reçu " + amount.stripTrailingZeros().toPlainString() + "₽ de " + source.getName().getString()).withStyle(ChatFormatting.GOLD));
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Fonds insuffisants !"));
            return 0;
        }
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

        context.getSource().sendSuccess(() -> Component.literal("Solde mis à jour pour " + target.getName().getString()), true);
        return 1;
    }
}
