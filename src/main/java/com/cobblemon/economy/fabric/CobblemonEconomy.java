package com.cobblemon.economy.fabric;

import com.cobblemon.economy.commands.EconomyCommands;
import com.cobblemon.economy.entity.ShopkeeperEntity;
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
import java.util.Optional;

public class CobblemonEconomy implements ModInitializer {
    public static final String MOD_ID = "cobblemon-economy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static EconomyManager economyManager;
    private static EconomyConfig config;
    private static MinecraftServer gameServer;

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
        LOGGER.info("Initializing Cobblemon Economy (Ryvexam Edition)");
        
        PayloadTypeRegistry.playC2S().register(PurchasePayload.TYPE, PurchasePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenShopPayload.TYPE, OpenShopPayload.CODEC);

        FabricDefaultAttributeRegistry.register(SHOPKEEPER, ShopkeeperEntity.createAttributes());

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(content -> {
            content.accept(SHOPKEEPER_SPAWN_EGG);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            gameServer = server;
            File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
            File modDir = new File(worldDir, "cobblemon-economy");
            if (!modDir.exists()) modDir.mkdirs();
            config = EconomyConfig.load(new File(modDir, "config.json"));
            economyManager = new EconomyManager(new File(modDir, "economy.db"));
        });

        ServerPlayNetworking.registerGlobalReceiver(PurchasePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handlePurchase(player, payload.itemId(), payload.isPco()));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> EconomyCommands.register(dispatcher));

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(entity instanceof ShopkeeperEntity shopkeeper)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            Component customName = stack.get(DataComponents.CUSTOM_NAME);

            // Détection du Shop Setter
            if (customName != null && customName.getString().startsWith("Shop Setter: ")) {
                String shopId = customName.getString().replace("Shop Setter: ", "");
                if (config.shops.containsKey(shopId)) {
                    shopkeeper.setShopId(shopId);
                    player.sendSystemMessage(Component.literal("Boutique du marchand réglée sur : " + shopId).withStyle(ChatFormatting.GREEN));
                    
                    // SUPPRESSION DU SETTER APRES UTILISATION
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendSystemMessage(Component.literal("Boutique '" + shopId + "' introuvable dans la config !").withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }
            }

            // Détection du Tower Tagger
            if (customName != null && customName.getString().equals("Tower Tagger")) {
                if (shopkeeper.getTags().contains("tour_de_combat")) {
                    shopkeeper.removeTag("tour_de_combat");
                    player.sendSystemMessage(Component.literal("Tag supprimé : tour_de_combat").withStyle(ChatFormatting.RED));
                } else {
                    shopkeeper.addTag("tour_de_combat");
                    player.sendSystemMessage(Component.literal("Tag ajouté : tour_de_combat").withStyle(ChatFormatting.AQUA));
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        CobblemonListeners.register();
    }

    private void handlePurchase(ServerPlayer player, String itemId, boolean isPco) {
        Optional<EconomyConfig.ShopItemDefinition> itemDef = config.shops.values().stream()
            .flatMap(shop -> shop.items.stream())
            .filter(item -> item.id.equals(itemId))
            .findFirst();

        if (itemDef.isEmpty()) return;

        BigDecimal bPrice = BigDecimal.valueOf(itemDef.get().price);
        boolean success = isPco ? economyManager.subtractPco(player.getUUID(), bPrice) : economyManager.subtractBalance(player.getUUID(), bPrice);

        if (success) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            player.getInventory().add(new ItemStack(item));
            player.sendSystemMessage(Component.literal("✔ Achat réussi !").withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.literal("✖ Pas assez d'argent !").withStyle(ChatFormatting.RED));
        }
    }

    public static void reloadConfig() {
        if (gameServer != null) {
            File worldDir = gameServer.getWorldPath(LevelResource.ROOT).toFile();
            config = EconomyConfig.load(new File(new File(worldDir, "cobblemon-economy"), "config.json"));
        }
    }

    public static EconomyManager getEconomyManager() { return economyManager; }
    public static EconomyConfig getConfig() { return config; }
    public static MinecraftServer getGameServer() { return gameServer; }
}
