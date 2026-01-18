package com.cobblemon.economy.fabric;

import com.cobblemon.economy.commands.EconomyCommands;
import com.cobblemon.economy.entity.PcoShopkeeperEntity;
import com.cobblemon.economy.entity.PokeShopkeeperEntity;
import com.cobblemon.economy.events.CobblemonListeners;
import com.cobblemon.economy.networking.OpenShopPayload;
import com.cobblemon.economy.networking.PurchasePayload;
import com.cobblemon.economy.storage.EconomyConfig;
import com.cobblemon.economy.storage.EconomyManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SpawnEggItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;

public class CobblemonEconomy implements ModInitializer {
    public static final String MOD_ID = "cobblemon-economy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static EconomyManager economyManager;
    private static EconomyConfig config;
    private static MinecraftServer gameServer;

    public static final EntityType<PokeShopkeeperEntity> POKE_SHOPKEEPER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "poke_shopkeeper"),
            EntityType.Builder.of(PokeShopkeeperEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .build("poke_shopkeeper")
    );

    public static final EntityType<PcoShopkeeperEntity> PCO_SHOPKEEPER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "pco_shopkeeper"),
            EntityType.Builder.of(PcoShopkeeperEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .build("pco_shopkeeper")
    );

    public static final Item POKE_SHOPKEEPER_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "poke_shopkeeper_spawn_egg"),
            new SpawnEggItem(POKE_SHOPKEEPER, 0xEEBA10, 0xFFFFFF, new Item.Properties())
    );

    public static final Item PCO_SHOPKEEPER_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "pco_shopkeeper_spawn_egg"),
            new SpawnEggItem(PCO_SHOPKEEPER, 0x10EBAE, 0x000000, new Item.Properties())
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Cobblemon Economy (Ryvexam Edition)");
        
        config = EconomyConfig.load();

        // Register Payloads for Server
        PayloadTypeRegistry.playC2S().register(PurchasePayload.TYPE, PurchasePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenShopPayload.TYPE, OpenShopPayload.CODEC);

        FabricDefaultAttributeRegistry.register(POKE_SHOPKEEPER, PokeShopkeeperEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PCO_SHOPKEEPER, PcoShopkeeperEntity.createAttributes());

        // Add to Creative Tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(content -> {
            content.accept(POKE_SHOPKEEPER_SPAWN_EGG);
            content.accept(PCO_SHOPKEEPER_SPAWN_EGG);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            gameServer = server;
            File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
            File dbFile = new File(worldDir, "cobblemon-economy.db");
            economyManager = new EconomyManager(dbFile);
        });

        // Handle purchases from client
        ServerPlayNetworking.registerGlobalReceiver(PurchasePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            String itemId = payload.itemId();
            boolean isPco = payload.isPco();

            context.server().execute(() -> {
                handlePurchase(player, itemId, isPco);
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            EconomyCommands.register(dispatcher);
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(entity instanceof LivingEntity living)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (stack.get(DataComponents.CUSTOM_NAME) != null && 
                stack.get(DataComponents.CUSTOM_NAME).getString().equals("Tower Tagger")) {
                
                if (living.getTags().contains("tour_de_combat")) {
                    living.removeTag("tour_de_combat");
                    player.sendSystemMessage(Component.literal("Tag supprimé : tour_de_combat").withStyle(ChatFormatting.RED));
                } else {
                    living.addTag("tour_de_combat");
                    player.sendSystemMessage(Component.literal("Tag ajouté : tour_de_combat").withStyle(ChatFormatting.AQUA));
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        CobblemonListeners.register();
    }

    private void handlePurchase(ServerPlayer player, String itemId, boolean isPco) {
        int price = 0;
        if (itemId.equals("cobblemon:poke_ball")) price = 100;
        else if (itemId.equals("cobblemon:great_ball")) price = 300;
        else if (itemId.equals("cobblemon:ultra_ball")) price = 600;
        else if (itemId.equals("cobblemon:potion")) price = 200;
        else if (itemId.equals("cobblemon:rare_candy")) price = 50;
        else if (itemId.equals("cobblemon:master_ball")) price = 500;
        else if (itemId.equals("cobblemon:ability_capsule")) price = 150;
        else if (itemId.equals("cobblemon:super_potion")) price = 500;
        else if (itemId.equals("cobblemon:revive")) price = 1500;

        BigDecimal bPrice = BigDecimal.valueOf(price);
        boolean success = isPco ? 
            economyManager.subtractPco(player.getUUID(), bPrice) :
            economyManager.subtractBalance(player.getUUID(), bPrice);

        if (success) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            player.getInventory().add(new ItemStack(item));
            player.sendSystemMessage(Component.literal("✔ Achat réussi !").withStyle(ChatFormatting.GREEN));
            
            // Re-send updated balance to refresh the screen
            ServerPlayNetworking.send(player, new OpenShopPayload(
                economyManager.getBalance(player.getUUID()),
                economyManager.getPco(player.getUUID()),
                isPco ? "PCO" : "POKE"
            ));
        } else {
            player.sendSystemMessage(Component.literal("✖ Pas assez d'argent !").withStyle(ChatFormatting.RED));
        }
    }

    public static void reloadConfig() {
        config = EconomyConfig.load();
    }

    public static EconomyManager getEconomyManager() { return economyManager; }
    public static EconomyConfig getConfig() { return config; }
    public static MinecraftServer getGameServer() { return gameServer; }
}
