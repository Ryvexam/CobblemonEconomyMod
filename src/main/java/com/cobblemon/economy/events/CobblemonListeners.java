package com.cobblemon.economy.events;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.npc.NPCBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.quest.QuestService;
import net.minecraft.core.registries.BuiltInRegistries;
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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class CobblemonListeners {
    private static final Map<String, Boolean> preChangeKnowledge = new ConcurrentHashMap<>();
    private static final Set<UUID> activeRaidDensPlayers = ConcurrentHashMap.newKeySet();
    private static volatile boolean listenersRegistered = false;
    private static volatile boolean raidDensApiAvailable = false;
    
    public static void resetListeners() {
        preChangeKnowledge.clear();
        activeRaidDensPlayers.clear();
        listenersRegistered = false;
        raidDensApiAvailable = false;
        CobblemonEconomy.LOGGER.debug("Cobblemon event listeners state reset");
    }

    public static void register() {
        if (listenersRegistered) {
            CobblemonEconomy.LOGGER.debug("Cobblemon event listeners already registered, skipping...");
            return;
        }
        listenersRegistered = true;
        registerRaidDensCompatibility();
        
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
            QuestService.handleCapture(event);

            BigDecimal multiplier = BigDecimal.ONE;
            BigDecimal currentPokemonMult = BigDecimal.ZERO;
            boolean isSpecial = false;
            boolean hadShiny = false;
            boolean hadRadiant = false;
            boolean hadLegendary = false;
            boolean hadParadox = false;

            boolean isShiny = pokemon.getShiny();
            var labels = pokemon.getSpecies().getLabels();
            
            CobblemonEconomy.LOGGER.debug("Capture event - Pokemon: {}, Shiny: {}, Labels: {}", 
                pokemon.getSpecies().getName(), isShiny, labels);

            if (isShiny) {
                currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().shinyMultiplier);
                isSpecial = true;
                hadShiny = true;
                CobblemonEconomy.LOGGER.debug("Shiny detected! Multiplier added: {}", 
                    CobblemonEconomy.getConfig().shinyMultiplier);
            }
            
            if (labels != null) {
                if (labels.contains("radiant")) {
                    currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().radiantMultiplier);
                    isSpecial = true;
                    hadRadiant = true;
                    CobblemonEconomy.LOGGER.debug("Radiant detected! Multiplier added: {}", 
                        CobblemonEconomy.getConfig().radiantMultiplier);
                }
                if (labels.contains("legendary") || labels.contains("mythical")) {
                    currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().legendaryMultiplier);
                    isSpecial = true;
                    hadLegendary = true;
                    CobblemonEconomy.LOGGER.debug("Legendary/Mythical detected! Multiplier added: {}", 
                        CobblemonEconomy.getConfig().legendaryMultiplier);
                }
                if (labels.contains("paradox")) {
                    currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().paradoxMultiplier);
                    isSpecial = true;
                    hadParadox = true;
                    CobblemonEconomy.LOGGER.debug("Paradox detected! Multiplier added: {}", 
                        CobblemonEconomy.getConfig().paradoxMultiplier);
                }
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
            
            CobblemonEconomy.LOGGER.debug("Capture reward calculation - Base: {}, Multiplier: {}, Total: {}, isSpecial: {}", 
                baseReward, multiplier, reward, isSpecial);
            
            if (reward.compareTo(BigDecimal.ZERO) > 0) {
                CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
                
                String formattedReward = reward.stripTrailingZeros().toPlainString();
                
                String translationKey = "cobblemon-economy.event.capture";
                ChatFormatting color = ChatFormatting.GOLD;
                
                if (hadRadiant && hadLegendary) {
                    translationKey = "cobblemon-economy.event.capture.radiant_legendary";
                    color = ChatFormatting.LIGHT_PURPLE;
                } else if (hadRadiant && hadParadox) {
                    translationKey = "cobblemon-economy.event.capture.radiant_paradox";
                    color = ChatFormatting.AQUA;
                } else if (hadShiny && hadLegendary) {
                    translationKey = "cobblemon-economy.event.capture.shiny_legendary";
                    color = ChatFormatting.LIGHT_PURPLE;
                } else if (hadShiny && hadParadox) {
                    translationKey = "cobblemon-economy.event.capture.shiny_paradox";
                    color = ChatFormatting.AQUA;
                } else if (hadLegendary) {
                    translationKey = "cobblemon-economy.event.capture.legendary";
                    color = ChatFormatting.LIGHT_PURPLE;
                } else if (hadRadiant) {
                    translationKey = "cobblemon-economy.event.capture.radiant";
                    color = ChatFormatting.AQUA;
                } else if (hadShiny) {
                    translationKey = "cobblemon-economy.event.capture.shiny";
                    color = ChatFormatting.AQUA;
                } else if (hadParadox) {
                    translationKey = "cobblemon-economy.event.capture.special";
                    color = ChatFormatting.DARK_PURPLE;
                }

                Component message = Component.translatable(translationKey, formattedReward);
                player.sendSystemMessage(message.copy().withStyle(color, ChatFormatting.BOLD));
            } else {
                CobblemonEconomy.LOGGER.warn("No capture reward given - reward amount was zero or negative: {}", reward);
            }

            return kotlin.Unit.INSTANCE;
        });

        // Reward for winning a battle
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            boolean isCombatTower = isBattleTowerBattle(event);
            boolean isRaidDenBattle = isRaidDenBattle(event);

            for (var winner : event.getWinners()) {
                if (winner instanceof PlayerBattleActor playerActor) {
                    ServerPlayer player = playerActor.getEntity();
                    if (player != null) {
                        QuestService.handleBattleVictory(player, isRaidDenBattle, isCombatTower);

                        if (raidDensApiAvailable && isRaidDenBattle) {
                            continue;
                        }

                        CobblemonEconomy.getEconomyManager().updateUsername(player.getUUID(), player.getGameProfile().getName());
                        BigDecimal reward = isRaidDenBattle
                                ? CobblemonEconomy.getConfig().raidDenVictoryReward
                                : CobblemonEconomy.getConfig().battleVictoryReward;
                        String formattedReward = reward.stripTrailingZeros().toPlainString();
                        Component message = Component.empty();
                        boolean hasReward = false;

                        if (reward.compareTo(BigDecimal.ZERO) > 0) {
                             CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
                             message = Component.translatable(
                                     isRaidDenBattle ? "cobblemon-economy.event.raid_victory" : "cobblemon-economy.event.victory",
                                     formattedReward
                             );
                             hasReward = true;
                        }

                        if (isCombatTower) {
                            BigDecimal pcoReward = CobblemonEconomy.getConfig().battleVictoryPcoReward
                                    .add(CobblemonEconomy.getConfig().battleTowerCompletionPcoBonus);
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

        try {
            CobblemonEvents.FOSSIL_REVIVED.subscribe(Priority.NORMAL, event -> {
                ServerPlayer player = event.getPlayer();
                Pokemon pokemon = event.getPokemon();
                if (player == null || pokemon == null) return kotlin.Unit.INSTANCE;
                CobblemonEconomy.getEconomyManager().updateUsername(player.getUUID(), player.getGameProfile().getName());

                BigDecimal multiplier = BigDecimal.ONE;
                BigDecimal currentPokemonMult = BigDecimal.ZERO;
                boolean isSpecial = false;
                boolean hadShiny = false;
                boolean hadRadiant = false;
                boolean hadLegendary = false;
                boolean hadParadox = false;

                boolean isShiny = pokemon.getShiny();
                var labels = pokemon.getSpecies().getLabels();
                
                CobblemonEconomy.LOGGER.debug("Fossil revival event - Pokemon: {}, Shiny: {}, Labels: {}", 
                    pokemon.getSpecies().getName(), isShiny, labels);

                if (isShiny) {
                    currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().shinyMultiplier);
                    isSpecial = true;
                    hadShiny = true;
                    CobblemonEconomy.LOGGER.debug("Shiny fossil detected! Multiplier added: {}", 
                        CobblemonEconomy.getConfig().shinyMultiplier);
                }
                
                if (labels != null) {
                    if (labels.contains("radiant")) {
                        currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().radiantMultiplier);
                        isSpecial = true;
                        hadRadiant = true;
                        CobblemonEconomy.LOGGER.debug("Radiant fossil detected! Multiplier added: {}", 
                            CobblemonEconomy.getConfig().radiantMultiplier);
                    }
                    if (labels.contains("legendary") || labels.contains("mythical")) {
                        currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().legendaryMultiplier);
                        isSpecial = true;
                        hadLegendary = true;
                        CobblemonEconomy.LOGGER.debug("Legendary/Mythical fossil detected! Multiplier added: {}", 
                            CobblemonEconomy.getConfig().legendaryMultiplier);
                    }
                    if (labels.contains("paradox")) {
                        currentPokemonMult = currentPokemonMult.add(CobblemonEconomy.getConfig().paradoxMultiplier);
                        isSpecial = true;
                        hadParadox = true;
                        CobblemonEconomy.LOGGER.debug("Paradox fossil detected! Multiplier added: {}", 
                            CobblemonEconomy.getConfig().paradoxMultiplier);
                    }
                }

                if (isSpecial) {
                    multiplier = currentPokemonMult;
                }

                BigDecimal baseReward = CobblemonEconomy.getConfig().captureReward;
                BigDecimal reward = baseReward.multiply(multiplier);
                
                CobblemonEconomy.LOGGER.debug("Fossil revival reward calculation - Base: {}, Multiplier: {}, Total: {}, isSpecial: {}", 
                    baseReward, multiplier, reward, isSpecial);
                
                if (reward.compareTo(BigDecimal.ZERO) > 0) {
                    CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
                    
                    String formattedReward = reward.stripTrailingZeros().toPlainString();
                    
                    String translationKey = "cobblemon-economy.event.fossil";
                    ChatFormatting color = ChatFormatting.GOLD;
                    
                    if (hadRadiant && hadLegendary) {
                        translationKey = "cobblemon-economy.event.fossil.radiant_legendary";
                        color = ChatFormatting.LIGHT_PURPLE;
                    } else if (hadRadiant && hadParadox) {
                        translationKey = "cobblemon-economy.event.fossil.radiant_paradox";
                        color = ChatFormatting.AQUA;
                    } else if (hadShiny && hadLegendary) {
                        translationKey = "cobblemon-economy.event.fossil.shiny_legendary";
                        color = ChatFormatting.LIGHT_PURPLE;
                    } else if (hadShiny && hadParadox) {
                        translationKey = "cobblemon-economy.event.fossil.shiny_paradox";
                        color = ChatFormatting.AQUA;
                    } else if (hadLegendary) {
                        translationKey = "cobblemon-economy.event.fossil.legendary";
                        color = ChatFormatting.LIGHT_PURPLE;
                    } else if (hadRadiant) {
                        translationKey = "cobblemon-economy.event.fossil.radiant";
                        color = ChatFormatting.AQUA;
                    } else if (hadShiny) {
                        translationKey = "cobblemon-economy.event.fossil.shiny";
                        color = ChatFormatting.AQUA;
                    } else if (hadParadox) {
                        translationKey = "cobblemon-economy.event.fossil.special";
                        color = ChatFormatting.DARK_PURPLE;
                    }

                    Component message = Component.translatable(translationKey, formattedReward);
                    player.sendSystemMessage(message.copy().withStyle(color, ChatFormatting.BOLD));
                } else {
                    CobblemonEconomy.LOGGER.warn("No fossil revival reward given - reward amount was zero or negative: {}", reward);
                }

                return kotlin.Unit.INSTANCE;
            });
        } catch (Throwable t) {
            CobblemonEconomy.LOGGER.warn("Fossil revival event not available on this Cobblemon version.");
        }

        CobblemonEconomy.LOGGER.info("Events registered for Cobblemon 1.7.1 (Pokedex Change Logic + Fossil Rewards)");
    }

    private static void registerRaidDensCompatibility() {
        try {
            Class<?> raidEventsClass = Class.forName("com.necro.raid.dens.common.events.RaidEvents");

            Object raidBattleStart = raidEventsClass.getField("RAID_BATTLE_START").get(null);
            Object raidEnd = raidEventsClass.getField("RAID_END").get(null);

            var subscribeMethod = raidBattleStart.getClass().getMethod("subscribe", Priority.class, Consumer.class);

            Consumer<Object> onRaidBattleStart = event -> {
                ServerPlayer player = asServerPlayer(tryInvoke(event, new String[] {"getPlayer"}));
                if (player != null) {
                    activeRaidDensPlayers.add(player.getUUID());
                }
            };

            Consumer<Object> onRaidEnd = event -> {
                ServerPlayer player = asServerPlayer(tryInvoke(event, new String[] {"getPlayer"}));
                if (player == null) {
                    return;
                }

                activeRaidDensPlayers.remove(player.getUUID());

                Object winValue = tryInvoke(event, new String[] {"isWin"});
                boolean isWin = winValue instanceof Boolean b && b;
                if (!isWin) {
                    return;
                }

                CobblemonEconomy.getEconomyManager().updateUsername(player.getUUID(), player.getGameProfile().getName());
                BigDecimal reward = CobblemonEconomy.getConfig().raidDenVictoryReward;
                if (reward.compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }

                CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), reward);
                String formattedReward = reward.stripTrailingZeros().toPlainString();
                player.sendSystemMessage(Component.translatable("cobblemon-economy.event.raid_victory", formattedReward)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            };

            subscribeMethod.invoke(raidBattleStart, Priority.NORMAL, onRaidBattleStart);
            subscribeMethod.invoke(raidEnd, Priority.NORMAL, onRaidEnd);

            raidDensApiAvailable = true;
            CobblemonEconomy.LOGGER.info("Cobblemon Raid Dens compatibility enabled.");
        } catch (Throwable ignored) {
            raidDensApiAvailable = false;
        }
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

    private static boolean isRaidDenBattle(com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent event) {
        try {
            var battle = event.getBattle();
            if (battle != null && battle.getFormat() != null) {
                String formatMod = battle.getFormat().getMod();
                if (containsRaidKeyword(formatMod)) {
                    return true;
                }

                var ruleSet = battle.getFormat().getRuleSet();
                if (ruleSet != null) {
                    for (String rule : ruleSet) {
                        if (containsRaidKeyword(rule)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return hasRaidActor(event.getWinners()) || hasRaidActor(event.getLosers());
    }

    private static boolean isBattleTowerBattle(com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent event) {
        return hasBattleTowerActor(event.getWinners()) || hasBattleTowerActor(event.getLosers());
    }

    private static boolean hasBattleTowerActor(Iterable<?> actors) {
        if (actors == null) {
            return false;
        }

        for (Object actor : actors) {
            if (actor == null) {
                continue;
            }

            String actorClass = actor.getClass().getName().toLowerCase(Locale.ROOT);
            if (actorClass.contains("battle.tower") || actorClass.contains("battle_tower") || actorClass.contains("battletower")) {
                return true;
            }

            LivingEntity entity = null;
            if (actor instanceof NPCBattleActor npcActor) {
                entity = npcActor.getEntity();
            } else {
                Object resolvedEntity = tryInvoke(actor, new String[] {"getEntity"});
                if (resolvedEntity instanceof LivingEntity living) {
                    entity = living;
                }
            }

            if (entity == null) {
                continue;
            }

            if (entity.getTags().contains("tour_de_combat")) {
                return true;
            }

            for (String tag : entity.getTags()) {
                String lowerTag = tag.toLowerCase(Locale.ROOT);
                if (lowerTag.contains("battle_tower") || lowerTag.contains("battletower")) {
                    return true;
                }
            }

            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (entityId != null) {
                String namespace = entityId.getNamespace().toLowerCase(Locale.ROOT);
                String path = entityId.getPath().toLowerCase(Locale.ROOT);
                if (namespace.contains("battle_tower") || namespace.contains("battletower") || path.contains("battle_tower") || path.contains("battletower")) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasRaidActor(Iterable<?> actors) {
        if (actors == null) {
            return false;
        }

        for (Object actor : actors) {
            if (actor == null) {
                continue;
            }

            String className = actor.getClass().getName().toLowerCase(Locale.ROOT);
            if (containsRaidKeyword(className)) {
                return true;
            }

            Object entity = tryInvoke(actor, new String[] {"getEntity"});
            if (entity instanceof LivingEntity livingEntity) {
                for (String tag : livingEntity.getTags()) {
                    if (containsRaidKeyword(tag)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean containsRaidKeyword(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("raid") || lower.contains("den");
    }

    private static ServerPlayer asServerPlayer(Object value) {
        return value instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private static Object tryInvoke(Object target, String[] methodNames, Object... args) {
        if (target == null) {
            return null;
        }
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
