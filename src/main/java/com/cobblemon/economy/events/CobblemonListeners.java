package com.cobblemon.economy.events;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.npc.NPCBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;

import java.math.BigDecimal;
import java.util.List;

public class CobblemonListeners {
    public static void register() {
        // Reward for Pokedex updates (First catch of a species)
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(Priority.NORMAL, event -> {
            // Check if the change is marking a Pokemon as CAUGHT
            if (event.getKnowledge() == PokedexEntryProgress.CAUGHT) {
                ServerPlayer player = CobblemonEconomy.getGameServer().getPlayerList().getPlayer(event.getPlayerUUID());
                if (player == null) return kotlin.Unit.INSTANCE;

                BigDecimal reward = CobblemonEconomy.getConfig().newDiscoveryReward;
                CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);

                player.sendSystemMessage(Component.translatable("cobblemon-economy.event.discovery.title")
                    .append(Component.translatable("cobblemon-economy.event.discovery.reward", reward + "₽").withStyle(ChatFormatting.GOLD)));
            }
            return kotlin.Unit.INSTANCE;
        });

        // Reward for winning a battle
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            boolean isCombatTower = false;
            BigDecimal multiplier = BigDecimal.ONE;

            for (var loser : event.getLosers()) {
                if (loser instanceof NPCBattleActor npcActor) {
                    LivingEntity entity = npcActor.getEntity();
                    if (entity != null && entity.getTags().contains("tour_de_combat")) {
                        isCombatTower = true;
                    }
                }

                // Check for special pokemon in the losing side
                for (var pokemonStack : loser.getPokemonList()) {
                    var pokemon = pokemonStack.getOriginalPokemon();
                    if (pokemon == null) continue;

                    if (pokemon.getShiny()) {
                        multiplier = multiplier.max(CobblemonEconomy.getConfig().shinyMultiplier);
                    }
                    
                    // Check for Legendary/Mythical/Paradox using labels
                    var labels = pokemon.getSpecies().getLabels();
                    if (labels.contains("legendary") || labels.contains("mythical")) {
                        multiplier = multiplier.max(CobblemonEconomy.getConfig().legendaryMultiplier);
                    }
                    if (labels.contains("paradox")) {
                        multiplier = multiplier.max(CobblemonEconomy.getConfig().paradoxMultiplier);
                    }
                }
            }

            for (var winner : event.getWinners()) {
                if (winner instanceof PlayerBattleActor playerActor) {
                    ServerPlayer player = playerActor.getEntity();
                    if (player != null) {
                        BigDecimal reward = CobblemonEconomy.getConfig().battleVictoryReward.multiply(multiplier);
                        CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
                        
                        Component message = Component.translatable("cobblemon-economy.event.victory", reward.stripTrailingZeros().toPlainString() + "₽");

                        if (isCombatTower) {
                            BigDecimal pcoReward = CobblemonEconomy.getConfig().battleVictoryPcoReward;
                            CobblemonEconomy.getEconomyManager().addPco(player.getUUID(), pcoReward);
                            message = message.copy().append(Component.translatable("cobblemon-economy.event.victory_pco", pcoReward));
                        }
                        
                        if (multiplier.compareTo(BigDecimal.ONE) > 0) {
                            player.sendSystemMessage(Component.literal("★ BONUS X" + multiplier.stripTrailingZeros().toPlainString() + " ★").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                        }

                        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.GOLD));
                    }
                }
            }
            return kotlin.Unit.INSTANCE;
        });

        CobblemonEconomy.LOGGER.info("Events registered for Cobblemon 1.7.1 (Pokedex Change Logic)");
    }
}
