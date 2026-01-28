package com.cobblemon.economy.events;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.Cobblemon;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.Comparator;

public class CobblemonListeners {
    private static final Map<String, Boolean> preChangeKnowledge = new ConcurrentHashMap<>();

    public static void register() {
        // Capture existing status before change
        CobblemonEvents.POKEDEX_DATA_CHANGED_PRE.subscribe(Priority.NORMAL, event -> {
            try {
                if (event.getKnowledge() == PokedexEntryProgress.CAUGHT) {
                    var speciesRecord = event.getRecord().getSpeciesDexRecord();
                    // Store if it was ALREADY caught
                    boolean wasCaught = speciesRecord.getKnowledge() == PokedexEntryProgress.CAUGHT;
                    String key = event.getPlayerUUID().toString() + ":" + speciesRecord.getId().toString();
                    preChangeKnowledge.put(key, wasCaught);
                }
            } catch (Exception e) {
                CobblemonEconomy.LOGGER.error("Error in Pokedex PRE event", e);
            }
            return kotlin.Unit.INSTANCE;
        });

        // Reward for Pokedex updates (First catch of a species)
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(Priority.NORMAL, event -> {
            // Check if the change is marking a Pokemon as CAUGHT
            if (event.getKnowledge() == PokedexEntryProgress.CAUGHT) {
                // Verify against PRE cache to ensure it's a new catch
                var speciesRecord = event.getRecord().getSpeciesDexRecord();
                String key = event.getPlayerUUID().toString() + ":" + speciesRecord.getId().toString();
                Boolean wasCaught = preChangeKnowledge.remove(key); // Remove to clean up

                // If we don't know the previous state, assume it might be a re-trigger (safer to deny if unsure? or allow?)
                // Actually, if PRE didn't fire (unlikely), we might give duplicate.
                // But generally PRE fires.
                // If wasCaught is TRUE, return immediately.
                if (Boolean.TRUE.equals(wasCaught)) {
                    return kotlin.Unit.INSTANCE;
                }

                ServerPlayer player = CobblemonEconomy.getGameServer().getPlayerList().getPlayer(event.getPlayerUUID());
                if (player == null) return kotlin.Unit.INSTANCE;
                CobblemonEconomy.getEconomyManager().updateUsername(player.getUUID(), player.getGameProfile().getName());

                int uniqueCount = getUniqueCaptureCount(player);
                if (uniqueCount >= 0) {
                    CobblemonEconomy.getEconomyManager().setCaptureCount(player.getUUID(), uniqueCount);
                } else {
                    uniqueCount = CobblemonEconomy.getEconomyManager().incrementUniqueCapture(player.getUUID());
                }

                handleCaptureMilestones(player, uniqueCount);
 
                // Check if this species is a legendary, mythical or paradox to avoid double rewards
                var species = com.cobblemon.mod.common.api.pokemon.PokemonSpecies.INSTANCE.getByIdentifier(speciesRecord.getId());
                if (species != null) {
                    var labels = species.getLabels();
                    if (labels.contains("legendary") || labels.contains("mythical") || labels.contains("paradox")) {
                        return kotlin.Unit.INSTANCE;
                    }
                }
                
                // For Pokedex discovery, we only give the base reward as it's for the species
                BigDecimal reward = CobblemonEconomy.getConfig().newDiscoveryReward;
                
                if (reward.compareTo(BigDecimal.ZERO) > 0) {
                    CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
                    String formattedReward = reward.stripTrailingZeros().toPlainString();
                    player.sendSystemMessage(Component.translatable("cobblemon-economy.event.discovery.title")
                        .append(Component.translatable("cobblemon-economy.event.discovery.reward", formattedReward).withStyle(ChatFormatting.GOLD)));
                }
            }
            return kotlin.Unit.INSTANCE;
        });

        // Reward for capturing a pokemon
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            ServerPlayer player = event.getPlayer();
            Pokemon pokemon = event.getPokemon();
            if (player == null || pokemon == null) return kotlin.Unit.INSTANCE;
            CobblemonEconomy.getEconomyManager().updateUsername(player.getUUID(), player.getGameProfile().getName());

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

            if (!isSpecial) {
                try {
                    // Check Pokedex to see if already caught
                    var pokedex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
                    var speciesIdentifier = pokemon.getSpecies().getResourceIdentifier();
                    boolean isCaught = pokedex.getHighestKnowledgeForSpecies(speciesIdentifier) == PokedexEntryProgress.CAUGHT;
                    
                    if (isCaught) {
                        return kotlin.Unit.INSTANCE;
                    }
                } catch (Exception e) {
                    CobblemonEconomy.LOGGER.error("Failed to check pokedex status in capture event", e);
                }
            }

            if (isSpecial) {
                multiplier = currentPokemonMult;
            }

            BigDecimal baseReward = CobblemonEconomy.getConfig().captureReward;
            BigDecimal reward = baseReward.multiply(multiplier);
            
            if (reward.compareTo(BigDecimal.ZERO) > 0) {
                CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
                
                String formattedReward = reward.stripTrailingZeros().toPlainString();
                
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
                        CobblemonEconomy.getEconomyManager().updateUsername(player.getUUID(), player.getGameProfile().getName());
                        BigDecimal reward = CobblemonEconomy.getConfig().battleVictoryReward;
                        String formattedReward = reward.stripTrailingZeros().toPlainString();
                        Component message = Component.empty();
                        boolean hasReward = false;

                        if (reward.compareTo(BigDecimal.ZERO) > 0) {
                             CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
                             message = Component.translatable("cobblemon-economy.event.victory", formattedReward);
                             hasReward = true;
                        }

                        if (isCombatTower) {
                            BigDecimal pcoReward = CobblemonEconomy.getConfig().battleVictoryPcoReward;
                            if (pcoReward.compareTo(BigDecimal.ZERO) > 0) {
                                CobblemonEconomy.getEconomyManager().addPco(player.getUUID(), pcoReward);
                                if (hasReward) {
                                     message = message.copy().append(Component.translatable("cobblemon-economy.event.victory_pco", pcoReward));
                                } else {
                                     // Just PCO message logic if needed, or reuse event key if it supports partial
                                     // Assuming event.victory expects {0} arg. If no poke reward, we might need a different message or just append PCO
                                     // For simplicity, let's just append to empty if no poke reward
                                     message = Component.translatable("cobblemon-economy.event.victory_pco", pcoReward);
                                }
                                hasReward = true;
                            }
                        }
                        
                        if (hasReward) {
                            player.sendSystemMessage(message.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                        }
                    }
                }
            }
            return kotlin.Unit.INSTANCE;
        });

        CobblemonEconomy.LOGGER.info("Events registered for Cobblemon 1.7.1 (Pokedex Change Logic)");
    }

    private static void handleCaptureMilestones(ServerPlayer player, int uniqueCount) {
        if (uniqueCount <= 0) {
            return;
        }
        Map<String, BigDecimal> milestones = CobblemonEconomy.getConfig().captureMilestones;
        if (milestones == null || milestones.isEmpty()) {
            return;
        }

        List<Integer> ordered = new ArrayList<>();
        for (String key : milestones.keySet()) {
            try {
                ordered.add(Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
            }
        }
        ordered.sort(Comparator.naturalOrder());

        for (Integer milestone : ordered) {
            if (milestone == null || milestone <= 0) {
                continue;
            }
            if (uniqueCount < milestone) {
                continue;
            }
            BigDecimal reward = milestones.get(String.valueOf(milestone));
            if (reward == null || reward.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            boolean claimed = CobblemonEconomy.getEconomyManager().claimCaptureMilestone(player.getUUID(), milestone);
            if (!claimed) {
                continue;
            }
            CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
            String formattedReward = reward.stripTrailingZeros().toPlainString();
            player.sendSystemMessage(Component.translatable(
                    "cobblemon-economy.event.milestone",
                    milestone,
                    formattedReward
            ).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    private static int getUniqueCaptureCount(ServerPlayer player) {
        try {
            Object pokedex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
            Object records = tryInvoke(pokedex, new String[] {"getSpeciesRecords", "getAllSpeciesRecords", "getEntries", "getDexRecords"});
            if (records == null) {
                return -1;
            }

            int count = 0;
            if (records instanceof Map<?, ?> map) {
                for (Object record : map.values()) {
                    if (isCaughtRecord(record)) {
                        count++;
                    }
                }
                return count;
            }
            if (records instanceof Iterable<?> iterable) {
                for (Object record : iterable) {
                    if (isCaughtRecord(record)) {
                        count++;
                    }
                }
                return count;
            }
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.debug("Failed to compute unique capture count", e);
        }
        return -1;
    }

    private static boolean isCaughtRecord(Object record) {
        if (record == null) {
            return false;
        }
        Object speciesRecord = tryInvoke(record, new String[] {"getSpeciesDexRecord"});
        Object target = speciesRecord != null ? speciesRecord : record;
        Object knowledge = tryInvoke(target, new String[] {"getKnowledge", "getHighestKnowledge"});
        return knowledge == PokedexEntryProgress.CAUGHT;
    }

    private static Object tryInvoke(Object target, String[] methodNames, Object... args) {
        for (String name : methodNames) {
            try {
                Class<?>[] params = new Class<?>[args.length];
                for (int i = 0; i < args.length; i++) {
                    params[i] = args[i].getClass();
                }
                return target.getClass().getMethod(name, params).invoke(target, args);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
