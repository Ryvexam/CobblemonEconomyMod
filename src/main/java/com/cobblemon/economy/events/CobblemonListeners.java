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

                // For Pokedex discovery, we only give the base reward as it's for the species
                BigDecimal reward = CobblemonEconomy.getConfig().newDiscoveryReward;
                CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);

                String formattedReward = reward.stripTrailingZeros().toPlainString() + "₽";
                player.sendSystemMessage(Component.translatable("cobblemon-economy.event.discovery.title")
                    .append(Component.translatable("cobblemon-economy.event.discovery.reward", formattedReward).withStyle(ChatFormatting.GOLD)));
            }
            return kotlin.Unit.INSTANCE;
        });

        // Reward for capturing a pokemon
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            ServerPlayer player = event.getPlayer();
            Pokemon pokemon = event.getPokemon();
            if (player == null || pokemon == null) return kotlin.Unit.INSTANCE;

            BigDecimal multiplier = BigDecimal.ONE;
            BigDecimal currentPokemonMult = BigDecimal.ZERO;
            boolean isSpecial = false;
            boolean hadShiny = false;
            boolean hadLegendary = false;
            boolean hadParadox = false;

            if (pokemon.getShiny()) {
                currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().shinyMultiplier);
                isSpecial = true;
                hadShiny = true;
            }
            
            var labels = pokemon.getSpecies().getLabels();
            if (labels.contains("legendary") || labels.contains("mythical")) {
                currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().legendaryMultiplier);
                isSpecial = true;
                hadLegendary = true;
            }
            if (labels.contains("paradox")) {
                currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().paradoxMultiplier);
                isSpecial = true;
                hadParadox = true;
            }

            if (isSpecial) {
                multiplier = currentPokemonMult;
            }

            BigDecimal reward = CobblemonEconomy.getConfig().battleVictoryReward.multiply(multiplier);
            CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
            
            String formattedReward = reward.stripTrailingZeros().toPlainString() + "₽";
            
            String translationKey = "cobblemon-economy.event.capture";
            ChatFormatting color = ChatFormatting.GOLD;
            
            if (hadShiny && hadLegendary) {
                translationKey = "cobblemon-economy.event.capture.shiny_legendary";
                color = ChatFormatting.LIGHT_PURPLE;
            } else if (hadShiny && hadParadox) {
                translationKey = "cobblemon-economy.event.capture.shiny_paradox";
                color = ChatFormatting.AQUA;
            } else if (hadLegendary) {
                translationKey = "cobblemon-economy.event.capture.legendary";
                color = ChatFormatting.LIGHT_PURPLE;
            } else if (hadShiny) {
                translationKey = "cobblemon-economy.event.capture.shiny";
                color = ChatFormatting.AQUA;
            } else if (hadParadox) {
                translationKey = "cobblemon-economy.event.capture.special";
                color = ChatFormatting.DARK_PURPLE;
            }

            Component message = Component.translatable(translationKey, formattedReward);
            player.sendSystemMessage(message.copy().withStyle(color, ChatFormatting.BOLD));

            return kotlin.Unit.INSTANCE;
        });

        // Reward for winning a battle
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            boolean isCombatTower = false;

            for (var loser : event.getLosers()) {
                if (loser instanceof NPCBattleActor npcActor) {
                    LivingEntity entity = npcActor.getEntity();
                    if (entity != null && entity.getTags().contains("tour_de_combat")) {
                        isCombatTower = true;
                        break;
                    }
                }
            }

            for (var winner : event.getWinners()) {
                if (winner instanceof PlayerBattleActor playerActor) {
                    ServerPlayer player = playerActor.getEntity();
                    if (player != null) {
                        BigDecimal reward = CobblemonEconomy.getConfig().battleVictoryReward;
                        CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
                        
                        String formattedReward = reward.stripTrailingZeros().toPlainString() + "₽";
                        Component message = Component.translatable("cobblemon-economy.event.victory", formattedReward);

                        if (isCombatTower) {
                            BigDecimal pcoReward = CobblemonEconomy.getConfig().battleVictoryPcoReward;
                            CobblemonEconomy.getEconomyManager().addPco(player.getUUID(), pcoReward);
                            message = message.copy().append(Component.translatable("cobblemon-economy.event.victory_pco", pcoReward));
                        }
                        
                        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    }
                }
            }
            return kotlin.Unit.INSTANCE;
        });

        CobblemonEconomy.LOGGER.info("Events registered for Cobblemon 1.7.1 (Pokedex Change Logic)");
    }
}
