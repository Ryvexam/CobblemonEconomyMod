package com.cobblemon.economy.entity;

import com.cobblemon.economy.shop.ShopGui;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.block.entity.SkullBlockEntity;

public class ShopkeeperEntity extends PathfinderMob {
    private String shopId = "default_poke";
    private static final EntityDataAccessor<String> SKIN_NAME = SynchedEntityData.defineId(ShopkeeperEntity.class, EntityDataSerializers.STRING);
    private GameProfile cachedProfile = null;

    public ShopkeeperEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_NAME, "");
    }

    public String getSkinName() {
        return this.entityData.get(SKIN_NAME);
    }

    public void setSkinName(String name) {
        this.entityData.set(SKIN_NAME, name);
        this.cachedProfile = null; // Invalider le cache pour forcer le rechargement
    }

    public GameProfile getGameProfile() {
        String name = getSkinName();
        if (name == null || name.isEmpty()) return null;

        if (cachedProfile == null || !cachedProfile.getName().equals(name)) {
            cachedProfile = new GameProfile(null, name);
        }
        return cachedProfile;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("ShopId", shopId);
        nbt.putString("SkinName", getSkinName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("ShopId")) {
            this.shopId = nbt.getString("ShopId");
        }
        if (nbt.contains("SkinName")) {
            setSkinName(nbt.getString("SkinName"));
        }
    }
}
