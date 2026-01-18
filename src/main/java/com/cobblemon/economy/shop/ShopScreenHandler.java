package com.cobblemon.economy.shop;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ShopScreenHandler extends AbstractContainerMenu {
    private final Container shopContainer;

    public ShopScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(6));
    }

    public ShopScreenHandler(int syncId, Inventory playerInventory, Container shopContainer) {
        super(null, syncId);
        this.shopContainer = shopContainer;

        // 1. Slots de la Boutique (Top)
        // Positionnés pour s'aligner avec les lignes de ton UI
        for (int i = 0; i < 6; i++) {
            this.addSlot(new Slot(shopContainer, i, 30, 58 + (i * 28)) {
                @Override
                public boolean mayPickup(Player player) {
                    return false; 
                }
            });
        }

        // 2. Slots de l'Inventaire (Standard Minecraft Chest)
        // X = 48 (8 + 40 de marge pour centrer 176 dans 256)
        // Y = 140 (Standard pour un coffre 9x6)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 48 + j * 18, 140 + i * 18));
            }
        }

        // 3. Slots de la Hotbar (Standard Minecraft Chest)
        // Y = 198 (Standard)
        for (int j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInventory, j, 48 + j * 18, 198));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
