package com.cobblemon.economy.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.Util;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;

public class ShopkeeperEntity extends PathfinderMob {
    private String shopId = "default_poke";
    private static final EntityDataAccessor<String> SKIN_NAME = SynchedEntityData.defineId(ShopkeeperEntity.class, EntityDataSerializers.STRING);

    public ShopkeeperEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        // this.setInvulnerable(true); // Handled dynamically in hurt()
    }
    
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Allow Creative players and /kill command
        if (source.isCreativePlayer() || source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) {
            return super.hurt(source, amount);
        }
        
        // Check YAWP flag
        Boolean allowed = com.cobblemon.economy.compat.CompatHandler.checkYawpAttack(this, source.getEntity());
        
        if (allowed != null && allowed) {
             return super.hurt(source, amount);
        }
        
        return false; // Default invulnerable
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 6.0F));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        // No push
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        // Always try to face the player if spawned by egg, command, or event (likely egg usage)
        if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND || reason == MobSpawnType.EVENT) {
            Player nearestPlayer = level.getNearestPlayer(this, 10.0);
            if (nearestPlayer != null) {
                // Calculate angle to look at the player
                double d0 = nearestPlayer.getX() - this.getX();
                double d1 = nearestPlayer.getZ() - this.getZ();
                float f = (float)(net.minecraft.util.Mth.atan2(d1, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                
                this.setYRot(f);
                this.setYBodyRot(f);
                this.setYHeadRot(f);
                this.setXRot(0.0f); // Ensure head is level
                this.yRotO = f;
                this.yBodyRotO = f;
                this.yHeadRotO = f;
            }
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_NAME, "shopkeeper"); // Valeur par défaut : "shopkeeper"
    }

    public String getSkinName() {
        return this.entityData.get(SKIN_NAME);
    }

    public void setSkinName(String name) {
        this.entityData.set(SKIN_NAME, name);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
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
        
        // Sanitize: Ensure CustomNameVisible is off if it was accidentally enabled by previous versions
        if (this.isCustomNameVisible()) {
            this.setCustomNameVisible(false);
        }
        
        // Remove default name if it was persisted
        if (this.hasCustomName() && this.getCustomName().getString().equalsIgnoreCase("shopkeeper")) {
            this.setCustomName(null);
        }
    }
}
