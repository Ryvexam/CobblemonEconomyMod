package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.networking.ProvideSkinPayload;
import com.cobblemon.economy.networking.RequestSkinPayload;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

import com.mojang.blaze3d.systems.RenderSystem;

public class ClientNetworkHandler {
    private static final Set<String> REQUESTED_SKINS = new HashSet<>();

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ProvideSkinPayload.TYPE, (payload, context) -> {
            String skinName = payload.skinName();
            byte[] data = payload.data();
            
            context.client().execute(() -> {
                if (data.length == 0) {
                    ShopkeeperRenderer.CLIENT_LOGGER.warn("Server has no data for skin '{}'", skinName);
                    return;
                }

                try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
                    NativeImage image = NativeImage.read(bis);
                    
                    // Sanitize path for ResourceLocation
                    String safeName = skinName.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "skins/" + safeName);

                    // Ensure rendering logic happens on render thread if needed (though execute() is usually main thread)
                    RenderSystem.recordRenderCall(() -> {
                         DynamicTexture texture = new DynamicTexture(image);
                         Minecraft.getInstance().getTextureManager().register(location, texture);
                         ShopkeeperRenderer.registerSyncedSkin(skinName, location);
                         ShopkeeperRenderer.CLIENT_LOGGER.info("Registered skin '{}' at location '{}'", skinName, location);
                    });

                } catch (Exception e) {
                    ShopkeeperRenderer.CLIENT_LOGGER.error("Failed to load skin '{}' from server data", skinName, e);
                }
            });
        });
    }

    public static void requestSkin(String skinName) {
        if (REQUESTED_SKINS.contains(skinName)) return; // Already asked
        REQUESTED_SKINS.add(skinName);
        
        if (ClientPlayNetworking.canSend(RequestSkinPayload.TYPE)) {
            ClientPlayNetworking.send(new RequestSkinPayload(skinName));
            ShopkeeperRenderer.CLIENT_LOGGER.info("Requesting skin '{}' from server...", skinName);
        }
    }
}
