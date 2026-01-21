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

public class ShopkeeperRenderer extends LivingEntityRenderer<ShopkeeperEntity, PlayerModel<ShopkeeperEntity>> {
    public static final Logger CLIENT_LOGGER = LoggerFactory.getLogger("cobblemon-economy-client");
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "textures/entity/shopkeeper/shopkeeper.png");
    
    // Cache for external textures
    private static final Map<String, ResourceLocation> EXTERNAL_TEXTURES = new HashMap<>();

    public ShopkeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ShopkeeperEntity entity) {
        String skinName = entity.getSkinName();
        if (skinName == null || skinName.isEmpty() || "shopkeeper".equals(skinName)) {
            return DEFAULT_TEXTURE;
        }

        // Try to load from external skins folder first
        ResourceLocation external = getExternalTexture(skinName);
        if (external != null) {
            return external;
        }

        // Fallback to internal assets
        String cleanName = skinName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "textures/entity/shopkeeper/" + cleanName + ".png");
    }

    private ResourceLocation getExternalTexture(String name) {
        if (EXTERNAL_TEXTURES.containsKey(name)) {
            return EXTERNAL_TEXTURES.get(name);
        }

        File modDir = new File(Minecraft.getInstance().gameDirectory, "config/cobblemon-economy/skins");
        File skinFile = new File(modDir, name + ".png");

        if (skinFile.exists()) {
            try (InputStream is = new FileInputStream(skinFile)) {
                NativeImage image = NativeImage.read(is);
                DynamicTexture texture = new DynamicTexture(image);
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "skins/" + name.toLowerCase());
                
                // Register the dynamic texture
                Minecraft.getInstance().getTextureManager().register(location, texture);
                
                EXTERNAL_TEXTURES.put(name, location);
                CLIENT_LOGGER.info("Loaded external skin: {}", skinFile.getAbsolutePath());
                return location;
            } catch (Exception e) {
                CLIENT_LOGGER.error("Failed to load external skin: " + skinFile.getAbsolutePath(), e);
            }
        }

        return null;
    }
}
