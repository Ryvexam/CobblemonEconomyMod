package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class ShopkeeperRenderer extends LivingEntityRenderer<LivingEntity, PlayerModel<LivingEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "textures/entity/shopkeeper.png");

    public ShopkeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(LivingEntity entity) {
        return TEXTURE;
    }
}
