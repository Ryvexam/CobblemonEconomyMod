package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import com.cobblemon.economy.entity.ShopkeeperEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import java.util.concurrent.CompletableFuture;

public class ShopkeeperRenderer extends LivingEntityRenderer<ShopkeeperEntity, PlayerModel<ShopkeeperEntity>> {
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "textures/entity/shopkeeper.png");

    public ShopkeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ShopkeeperEntity entity) {
        String skinName = entity.getSkinName();
        if (skinName == null || skinName.isEmpty()) {
            return DEFAULT_TEXTURE;
        }

        GameProfile profile = entity.getGameProfile();
        if (profile != null) {
            // Si le profil a déjà des propriétés (textures), on l'utilise direct
            if (!profile.getProperties().isEmpty()) {
                return Minecraft.getInstance().getSkinManager().getInsecureSkin(profile).texture();
            }
            
            // Sinon, on tente de le mettre à jour (chargement asynchrone simulé par le SkinManager)
            // L'astuce est d'utiliser le TileEntitySkull logic si possible, ou de laisser le jeu charger.
            // Ici, on retourne le skin insecure qui tente de résoudre le nom.
            return Minecraft.getInstance().getSkinManager().getInsecureSkin(profile).texture();
        }
        
        return DEFAULT_TEXTURE;
    }
}
