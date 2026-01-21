package com.cobblemon.economy.fabric;

import com.cobblemon.economy.commands.EconomyCommands;
import com.cobblemon.economy.entity.ShopkeeperEntity;
import com.cobblemon.economy.events.CobblemonListeners;
import com.cobblemon.economy.storage.EconomyConfig;
import com.cobblemon.economy.storage.EconomyManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
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
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SpawnEggItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class CobblemonEconomy implements ModInitializer {
    public static final String MOD_ID = "cobblemon-economy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static EconomyManager economyManager;
    private static EconomyConfig config;
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

    @Override
    public void onInitialize() {
        LOGGER.info("Starting Cobblemon Economy (Common Init)...");
        
        // skins Dir stays global for convenience
        File globalDir = FabricLoader.getInstance().getConfigDir().resolve("cobblemon-economy").toFile();
        File skinsDir = new File(globalDir, "skins");
        if (!skinsDir.exists()) skinsDir.mkdirs();

        FabricDefaultAttributeRegistry.register(SHOPKEEPER, ShopkeeperEntity.createAttributes());

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(content -> {
            content.accept(SHOPKEEPER_SPAWN_EGG);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            gameServer = server;
            
            // Back to world directory for mod data
            modDirectory = server.getWorldPath(LevelResource.ROOT).resolve("cobblemon-economy").toFile();
            if (!modDirectory.exists()) modDirectory.mkdirs();

            config = EconomyConfig.load(new File(modDirectory, "config.json"));
            economyManager = new EconomyManager(new File(modDirectory, "economy.db"));
            
            CobblemonListeners.register();
            LOGGER.info("Cobblemon Economy (Server Init) - DONE");
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> EconomyCommands.register(dispatcher));

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(entity instanceof ShopkeeperEntity shopkeeper)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            
            if (!stack.isEmpty() && stack.getCount() > 0) {
                Component customNameComp = stack.get(DataComponents.CUSTOM_NAME);
                if (customNameComp != null) {
                    String customName = customNameComp.getString();

                    if (stack.is(Items.NETHER_STAR) && customName.startsWith("Shop Setter: ")) {
                        if (player instanceof ServerPlayer serverPlayer) {
                            if (serverPlayer.getCooldowns().isOnCooldown(stack.getItem())) return InteractionResult.FAIL;
                            serverPlayer.getCooldowns().addCooldown(stack.getItem(), 20);
                        }

                        String shopId = customName.replace("Shop Setter: ", "");
                        EconomyConfig.ShopDefinition shopDef = config.shops.get(shopId);
                        if (shopDef != null) {
                            shopkeeper.setShopId(shopId);
                            if (shopDef.skin != null && !shopDef.skin.isEmpty()) {
                                shopkeeper.setSkinName(shopDef.skin);
                            }
                            
                            player.sendSystemMessage(Component.translatable("cobblemon-economy.notification.shopkeeper_set", shopId).withStyle(ChatFormatting.GREEN));
                            if (!player.getAbilities().instabuild) {
                                stack.shrink(1);
                                if (player instanceof ServerPlayer sp) sp.containerMenu.broadcastChanges();
                            }
                            return InteractionResult.SUCCESS;
                        }
                    }

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
                com.cobblemon.economy.shop.ShopGui.open(serverPlayer, shopkeeper.getShopId());
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        LOGGER.info("Cobblemon Economy (Common Init) - DONE");
    }

    public static void reloadConfig() {
        if (modDirectory != null) {
            config = EconomyConfig.load(new File(modDirectory, "config.json"));
        }
    }

    public static EconomyManager getEconomyManager() { return economyManager; }
    public static EconomyConfig getConfig() { return config; }
    public static MinecraftServer getGameServer() { return gameServer; }
    public static File getModDirectory() { return modDirectory; }
}
