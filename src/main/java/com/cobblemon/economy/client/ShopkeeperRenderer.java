package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cobblemon.economy.entity.ShopkeeperEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.storage.LevelResource;

public class ShopkeeperRenderer extends LivingEntityRenderer<ShopkeeperEntity, PlayerModel<ShopkeeperEntity>> {
    public static final Logger CLIENT_LOGGER = LoggerFactory.getLogger("cobblemon-economy-client");
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "textures/entity/shopkeeper/shopkeeper.png");
    
    // Cache for external textures
    private static final Map<String, ResourceLocation> EXTERNAL_TEXTURES = new HashMap<>();

    public ShopkeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    // Public method for network handler to register skins
    public static void registerSyncedSkin(String name, ResourceLocation location) {
        EXTERNAL_TEXTURES.put(name, location);
    }

    @Override
    public ResourceLocation getTextureLocation(ShopkeeperEntity entity) {
        String skinName = entity.getSkinName();
        if (skinName == null || skinName.isEmpty() || "shopkeeper".equals(skinName)) {
            return DEFAULT_TEXTURE;
        }

        // 1. Check Cache (includes synced skins)
        if (EXTERNAL_TEXTURES.containsKey(skinName)) {
            return EXTERNAL_TEXTURES.get(skinName);
        }

        // 2. Try Local File (Singleplayer / Dev)
        ResourceLocation local = loadLocalSkin(skinName);
        if (local != null) return local;

        // 3. Request from Server (for Multiplayer)
        ClientNetworkHandler.requestSkin(skinName);
        
        // Return default while waiting for network response
        return DEFAULT_TEXTURE;
    }

    private ResourceLocation loadLocalSkin(String name) {
        File modDir;
        
        // In Singleplayer, prioritize the world-specific config
        if (Minecraft.getInstance().hasSingleplayerServer()) {
             try {
                 modDir = Minecraft.getInstance().getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve("config/cobblemon-economy/skins").toFile();
             } catch (Exception e) {
                 modDir = new File(Minecraft.getInstance().gameDirectory, "config/cobblemon-economy/skins");
             }
        } else {
             // Multiplayer client doesn't have access to server files, but might have local copies in .minecraft/config/
             modDir = new File(Minecraft.getInstance().gameDirectory, "config/cobblemon-economy/skins");
        }

        File skinFile = new File(modDir, name + ".png");

        if (skinFile.exists()) {
            try (InputStream is = new FileInputStream(skinFile)) {
                NativeImage image = NativeImage.read(is);
                DynamicTexture texture = new DynamicTexture(image);
                String safeName = name.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "skins/" + safeName);
                
                Minecraft.getInstance().getTextureManager().register(location, texture);
                
                EXTERNAL_TEXTURES.put(name, location);
                CLIENT_LOGGER.info("Loaded local skin: {}", skinFile.getAbsolutePath());
                return location;
            } catch (Exception e) {
                CLIENT_LOGGER.error("Failed to load local skin: " + skinFile.getAbsolutePath(), e);
            }
        }
        return null;
    }
}
