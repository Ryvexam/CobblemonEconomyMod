package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.networking.OpenShopPayload;
import com.cobblemon.economy.networking.PurchasePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends Screen {
    private final BigDecimal balance;
    private final BigDecimal pco;
    private final String shopType;
    private final List<ShopItem> items = new ArrayList<>();
    
    private final int imageWidth = 256;
    private final int imageHeight = 256;
    
    // Chemin dynamique vers la texture
    private final ResourceLocation backgroundTexture;

    public ShopScreen(BigDecimal balance, BigDecimal pco, String shopType) {
        super(Component.literal("Shop"));
        this.balance = balance;
        this.pco = pco;
        this.shopType = shopType;
        
        // Sélection du dossier en fonction du type de shop
        String folder = shopType.equals("POKE") ? "pokedollar" : "pco";
        // On utilise 0.png par défaut (pas de navigation)
        this.backgroundTexture = ResourceLocation.fromNamespaceAndPath(
            CobblemonEconomy.MOD_ID, 
            "textures/gui/shop/" + folder + "/0.png"
        );
        
        if (shopType.equals("POKE")) {
            items.add(new ShopItem("cobblemon:poke_ball", "Poké Ball", 100, false));
            items.add(new ShopItem("cobblemon:great_ball", "Great Ball", 300, false));
            items.add(new ShopItem("cobblemon:ultra_ball", "Ultra Ball", 600, false));
            items.add(new ShopItem("cobblemon:potion", "Potion", 200, false));
            items.add(new ShopItem("cobblemon:super_potion", "Super Potion", 500, false));
            items.add(new ShopItem("cobblemon:revive", "Rappel", 1500, false));
        } else {
            items.add(new ShopItem("cobblemon:rare_candy", "Super Bonbon", 50, true));
            items.add(new ShopItem("cobblemon:master_ball", "Master Ball", 500, true));
            items.add(new ShopItem("cobblemon:ability_capsule", "Capsule Talent", 150, true));
        }
    }

    @Override
    protected void init() {
        int xStart = (this.width - imageWidth) / 2;
        int yStart = (this.height - imageHeight) / 2;

        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            this.addRenderableWidget(Button.builder(Component.literal("Buy"), button -> {
                ClientPlayNetworking.send(new PurchasePayload(item.id, item.isPco));
            }).bounds(xStart + 185, yStart + 55 + (i * 28), 45, 20).build());
        }
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        
        int xStart = (this.width - imageWidth) / 2;
        int yStart = (this.height - imageHeight) / 2;

        // Utilise la texture spécifique au shop
        graphics.blit(backgroundTexture, xStart, yStart, 0, 0, imageWidth, imageHeight);
        
        String balText = shopType.equals("POKE") ? format(balance) + "₽" : format(pco) + " PCo";
        int color = shopType.equals("POKE") ? 0x55FF55 : 0x55FFFF;
        graphics.drawString(this.font, balText, xStart + 155, yStart + 15, color, false);
        
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            int yPos = yStart + 57 + (i * 28);
            
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item.id)));
            graphics.renderItem(stack, xStart + 28, yPos);
            graphics.drawString(this.font, item.name, xStart + 55, yPos + 4, 0xFFFFFF, false);
            
            String price = item.price + (item.isPco ? " PCo" : "₽");
            graphics.drawString(this.font, price, xStart + 130, yPos + 4, color, false);
        }

        graphics.drawString(this.font, "ryvexam.fr", xStart + 185, yStart + 240, 0x555555, false);

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class ShopItem {
        final String id;
        final String name;
        final int price;
        final boolean isPco;

        ShopItem(String id, String name, int price, boolean isPco) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.isPco = isPco;
        }
    }
}
