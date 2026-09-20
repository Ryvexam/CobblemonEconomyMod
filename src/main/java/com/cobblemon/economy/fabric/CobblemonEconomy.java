package com.cobblemon.economy.fabric;

import com.cobblemon.economy.commands.EconomyCommands;
import com.cobblemon.economy.block.QuestBoardBlock;
import com.cobblemon.economy.compat.CompatHandler;
import com.cobblemon.economy.compat.tab.TabIntegration;
import com.cobblemon.economy.entity.ShopkeeperEntity;
import com.cobblemon.economy.events.CobblemonListeners;
import com.cobblemon.economy.questboard.QuestBoardBindings;
import com.cobblemon.economy.questboard.QuestBoardService;
import com.cobblemon.economy.quest.QuestManager;
import com.cobblemon.economy.storage.EconomyConfig;
import com.cobblemon.economy.storage.EconomyManager;
import com.cobblemon.economy.storage.QuestConfig;
import com.cobblemon.economy.storage.QuestNpcConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import com.cobblemon.economy.util.PerformanceProfiler;

public class CobblemonEconomy implements ModInitializer {
    public static final String MOD_ID = "cobblemon-economy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static EconomyManager economyManager;
    private static QuestManager questManager;
    private static EconomyConfig config;
    private static QuestConfig questConfig;
    private static QuestNpcConfig questNpcConfig;
    private static QuestBoardBindings questBoardBindings;
    private static MinecraftServer gameServer;
    private static File modDirectory;

    public static final EntityType<ShopkeeperEntity> SHOPKEEPER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shopkeeper"),
            EntityType.Builder.of(ShopkeeperEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .build("shopkeeper")
    );

    public static final Item SHOPKEEPER_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shopkeeper_spawn_egg"),
            new SpawnEggItem(SHOPKEEPER, 0xEEBA10, 0xFFFFFF, new Item.Properties())
    );

    public static final Item TRAINER_HEAD = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "trainer_head"),
            new Item(new Item.Properties())
    );

    public static final Block QUEST_BOARD_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "quest_board"),
            new QuestBoardBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    public static final Item QUEST_BOARD_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "quest_board"),
            new BlockItem(QUEST_BOARD_BLOCK, new Item.Properties())
    );

    public static final CreativeModeTab COBECO_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "cobeco"),
            FabricItemGroup.builder()
                    .title(Component.literal("CobEco"))
                    .icon(() -> new ItemStack(QUEST_BOARD_ITEM))
                    .displayItems((parameters, output) -> {
                        output.accept(QUEST_BOARD_ITEM);
                        output.accept(SHOPKEEPER_SPAWN_EGG);
                    })
                    .build()
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Starting Cobblemon Economy (Common Init)...");
        
        CompatHandler.init();
        
        FabricDefaultAttributeRegistry.register(SHOPKEEPER, ShopkeeperEntity.createAttributes());
        com.cobblemon.economy.networking.NetworkHandler.register();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(content -> {
            content.accept(SHOPKEEPER_SPAWN_EGG);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(content -> {
            content.accept(QUEST_BOARD_ITEM);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            gameServer = server;
            
            // Per-world configuration path: world/config/cobblemon-economy/
            // Fix: Normalize path to avoid './' issues
            Path worldPath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            modDirectory = worldPath.resolve("config").resolve("cobblemon-economy").toFile();
            
            if (!modDirectory.exists()) {
                boolean created = modDirectory.mkdirs();
                if (!created) LOGGER.error("Failed to create config directory: " + modDirectory.getAbsolutePath());
            }

            // Create skins directory inside the world config directory
            File skinsDir = new File(modDirectory, "skins");
            if (!skinsDir.exists()) skinsDir.mkdirs();

            config = EconomyConfig.load(new File(modDirectory, "config.json"), new File(modDirectory, "shops.json"));
            PerformanceProfiler.configure(config);
            questConfig = QuestConfig.load(new File(modDirectory, "quests.json"));
            questNpcConfig = QuestNpcConfig.load(new File(modDirectory, "quest_npcs.json"));
            questBoardBindings = QuestBoardBindings.load(new File(modDirectory, "quest_boards_bindings.json"));
            economyManager = new EconomyManager(new File(modDirectory, "economy.db"));
            questManager = new QuestManager(new File(modDirectory, "quests.db"));
            
            CobblemonListeners.register();
            LOGGER.info("Cobblemon Economy (Server Init) - DONE");
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CobblemonListeners.resetListeners();
            LOGGER.info("Cobblemon Economy (Server Stop) - Listeners reset");
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TabIntegration.register();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (economyManager != null && player != null) {
                economyManager.getBalance(player.getUUID());
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> EconomyCommands.register(dispatcher));

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(entity instanceof ShopkeeperEntity shopkeeper)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            
            if (!stack.isEmpty() && stack.getCount() > 0) {
                // 1. Check Shop Setter (Nether Star)
                if (stack.is(Items.NETHER_STAR)) {
                    // Try to read custom data (language-agnostic)
                    net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                    String shopId = null;
                    
                    if (customData != null && customData.contains("ShopSetterId")) {
                        shopId = customData.copyTag().getString("ShopSetterId");
                    } else {
                        // Fallback to Name-based check (Legacy support)
                        Component customNameComp = stack.get(DataComponents.CUSTOM_NAME);
                        if (customNameComp != null) {
                            String customName = customNameComp.getString();
                            if (customName.startsWith("Shop Setter: ")) {
                                shopId = customName.replace("Shop Setter: ", "");
                            }
                        }
                    }

                    if (shopId != null) {
                        if (player instanceof ServerPlayer serverPlayer) {
                            if (serverPlayer.getCooldowns().isOnCooldown(stack.getItem())) return InteractionResult.FAIL;
                            serverPlayer.getCooldowns().addCooldown(stack.getItem(), 20);
                        }

                        EconomyConfig.ShopDefinition shopDef = config.shops.get(shopId);
                        if (shopDef != null) {
                            shopkeeper.setShopId(shopId);
                            shopkeeper.setNpcRole("SHOP");
                            shopkeeper.setQuestNpcId("");
                            if (shopDef.skin != null && !shopDef.skin.isEmpty()) {
                                shopkeeper.setSkinName(shopDef.skin);
                            }
                            shopkeeper.setSkinModel(shopDef.skinModel);
                            
                            player.sendSystemMessage(Component.translatable("cobblemon-economy.notification.shopkeeper_set", shopId).withStyle(ChatFormatting.GREEN));
                            if (!player.getAbilities().instabuild) {
                                stack.shrink(1);
                                if (player instanceof ServerPlayer sp) sp.containerMenu.broadcastChanges();
                            }
                            return InteractionResult.SUCCESS;
                        }
                    }
                }

                net.minecraft.world.item.component.CustomData questSetterData = stack.get(DataComponents.CUSTOM_DATA);
                String questNpcId = null;
                if (questSetterData != null && questSetterData.contains("QuestNpcSetterId")) {
                    questNpcId = questSetterData.copyTag().getString("QuestNpcSetterId");
                }

                if (questNpcId != null && !questNpcId.isBlank()) {
                    QuestNpcConfig.QuestNpcDefinition npcDefinition = questNpcConfig != null ? questNpcConfig.questNpcs.get(questNpcId) : null;
                    if (npcDefinition != null) {
                        shopkeeper.setNpcRole("QUEST");
                        shopkeeper.setQuestNpcId(questNpcId);
                        if (npcDefinition.skin != null && !npcDefinition.skin.isBlank()) {
                            shopkeeper.setSkinName(npcDefinition.skin);
                        }
                        shopkeeper.setSkinModel(npcDefinition.skinModel);
                        // Keep head name hidden; quest NPC display name is shown inside GUI.
                        shopkeeper.setCustomName(null);
                        player.sendSystemMessage(Component.translatable("cobblemon-economy.notification.quest_npc_set", questNpcId).withStyle(ChatFormatting.GREEN));
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            if (player instanceof ServerPlayer sp) sp.containerMenu.broadcastChanges();
                        }
                        return InteractionResult.SUCCESS;
                    }
                }

                if (stack.is(Items.NAME_TAG)) {
                    if (!player.hasPermissions(2)) {
                        player.sendSystemMessage(Component.translatable("cobblemon-economy.notification.op_only").withStyle(ChatFormatting.RED));
                        return InteractionResult.FAIL;
                    }
                    // Allow OPs to name it (will be handled by vanilla if sneaking)
                    if (player.isShiftKeyDown()) {
                        return InteractionResult.PASS;
                    }
                }

                Component customNameComp = stack.get(DataComponents.CUSTOM_NAME);
                if (customNameComp != null) {
                    String customName = customNameComp.getString();

                    if (stack.is(Items.PLAYER_HEAD) && customName.startsWith("Skin Setter: ")) {
                        if (player instanceof ServerPlayer serverPlayer) {
                            if (serverPlayer.getCooldowns().isOnCooldown(stack.getItem())) return InteractionResult.FAIL;
                            serverPlayer.getCooldowns().addCooldown(stack.getItem(), 20);
                        }

                        String skinName = customName.replace("Skin Setter: ", "");
                        shopkeeper.setSkinName(skinName);
                        player.sendSystemMessage(Component.translatable("cobblemon-economy.notification.skin_updated").withStyle(ChatFormatting.GREEN));
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            if (player instanceof ServerPlayer sp) sp.containerMenu.broadcastChanges();
                        }
                        return InteractionResult.SUCCESS;
                    }

                    if (stack.is(Items.BLAZE_ROD) && customName.equals("Tower Tagger")) {
                        if (player instanceof ServerPlayer serverPlayer) {
                            if (serverPlayer.getCooldowns().isOnCooldown(stack.getItem())) return InteractionResult.FAIL;
                            serverPlayer.getCooldowns().addCooldown(stack.getItem(), 20);
                        }
                        if (shopkeeper.getTags().contains("tour_de_combat")) {
                            shopkeeper.removeTag("tour_de_combat");
                            player.sendSystemMessage(Component.translatable("cobblemon-economy.notification.tag_removed").withStyle(ChatFormatting.RED));
                        } else {
                            shopkeeper.addTag("tour_de_combat");
                            player.sendSystemMessage(Component.translatable("cobblemon-economy.notification.tag_added").withStyle(ChatFormatting.AQUA));
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }

            if (player instanceof ServerPlayer serverPlayer && !player.isShiftKeyDown()) {
                if (shopkeeper.isQuestNpc() && shopkeeper.getQuestNpcId() != null && !shopkeeper.getQuestNpcId().isBlank()) {
                    QuestBoardService.openBoard(serverPlayer, shopkeeper.getQuestNpcId());
                } else {
                    com.cobblemon.economy.shop.ShopGui.open(serverPlayer, shopkeeper.getShopId());
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(hitResult instanceof BlockHitResult bhr)) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            String blockId = BuiltInRegistries.BLOCK.getKey(world.getBlockState(bhr.getBlockPos()).getBlock()).toString();
            String boardId = resolveBoardId(world, bhr.getBlockPos());
            if (boardId == null && !blockId.equals(MOD_ID + ":quest_board")) {
                return InteractionResult.PASS;
            }
            if (boardId == null) {
                boardId = getDefaultBoardId();
            }

            if (boardId == null || boardId.isBlank()) {
                return InteractionResult.PASS;
            }

            boolean opened = QuestBoardService.openBoard(serverPlayer, boardId);
            return opened ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });

        LOGGER.info("Cobblemon Economy (Common Init) - DONE");
    }

    public static void reloadConfig() {
        if (modDirectory != null) {
            config = EconomyConfig.load(new File(modDirectory, "config.json"), new File(modDirectory, "shops.json"));
            PerformanceProfiler.configure(config);
            questConfig = QuestConfig.load(new File(modDirectory, "quests.json"));
            questNpcConfig = QuestNpcConfig.load(new File(modDirectory, "quest_npcs.json"));
            questBoardBindings = QuestBoardBindings.load(new File(modDirectory, "quest_boards_bindings.json"));
        }
    }

    public static EconomyManager getEconomyManager() { return economyManager; }
    public static QuestManager getQuestManager() { return questManager; }
    public static EconomyConfig getConfig() { return config; }
    public static QuestConfig getQuestConfig() { return questConfig; }
    public static QuestNpcConfig getQuestNpcConfig() { return questNpcConfig; }
    public static QuestBoardBindings getQuestBoardBindings() { return questBoardBindings; }
    public static MinecraftServer getGameServer() { return gameServer; }
    public static File getModDirectory() { return modDirectory; }

    public static String resolveBoardId(String dimension, int x, int y, int z) {
        if (questBoardBindings == null || questBoardBindings.bindings == null) {
            return null;
        }
        return questBoardBindings.bindings.get(QuestBoardBindings.key(dimension, x, y, z));
    }

    public static String resolveBoardId(net.minecraft.world.level.Level world, BlockPos pos) {
        String direct = resolveBoardId(world.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
        if (direct != null) {
            return direct;
        }
        var state = world.getBlockState(pos);
        if (state.getBlock() instanceof QuestBoardBlock) {
            BlockPos origin = QuestBoardBlock.findOrigin(world, pos, state);
            return resolveBoardId(world.dimension().location().toString(), origin.getX(), origin.getY(), origin.getZ());
        }
        return null;
    }

    public static void bindBoardId(String dimension, int x, int y, int z, String boardId) {
        if (modDirectory == null) {
            return;
        }
        if (questBoardBindings == null) {
            questBoardBindings = QuestBoardBindings.load(new File(modDirectory, "quest_boards_bindings.json"));
        }
        String key = QuestBoardBindings.key(dimension, x, y, z);
        if (boardId == null || boardId.isBlank()) {
            questBoardBindings.bindings.remove(key);
        } else {
            questBoardBindings.bindings.put(key, boardId);
        }
        QuestBoardBindings.save(new File(modDirectory, "quest_boards_bindings.json"), questBoardBindings);
    }

    private static String getDefaultBoardId() {
        if (questNpcConfig == null || questNpcConfig.questNpcs == null || questNpcConfig.questNpcs.isEmpty()) {
            return null;
        }
        return questNpcConfig.questNpcs.keySet().iterator().next();
    }

}
