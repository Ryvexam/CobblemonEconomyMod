package com.cobblemon.economy.events;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.npc.NPCBattleActor;
import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;

import java.math.BigDecimal;

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

                player.sendSystemMessage(Component.literal("§bNouvelle Découverte ! §rVous avez reçu ")
                    .append(Component.literal(reward + "₽").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" pour votre capture !")));
            }
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
                        
                        String message = "Victoire ! Vous avez reçu " + reward + "₽";

                        if (isCombatTower) {
                            BigDecimal pcoReward = CobblemonEconomy.getConfig().battleVictoryPcoReward;
                            CobblemonEconomy.getEconomyManager().addPco(player.getUUID(), pcoReward);
                            message += " et " + pcoReward + " PCo";
                        }
                        
                        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GOLD));
                    }
                }
            }
            return kotlin.Unit.INSTANCE;
        });

        CobblemonEconomy.LOGGER.info("Events registered for Cobblemon 1.7.1 (Pokedex Change Logic)");
    }
}
