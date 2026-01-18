package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.networking.OpenShopPayload;
import com.cobblemon.economy.networking.PurchasePayload;
import com.cobblemon.economy.shop.ShopScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.inventory.Slot;

import java.math.BigDecimal;
import java.util.List;

public class ShopScreen extends AbstractContainerScreen<ShopScreenHandler> {
    private final BigDecimal balance;
    private final BigDecimal pco;
    private final String shopTitle;
    private final String currencyType;
    private final List<OpenShopPayload.ShopItemData> itemData;
    private final ResourceLocation backgroundTexture;

    public ShopScreen(ShopScreenHandler handler, Inventory inventory, OpenShopPayload payload) {
        super(handler, inventory, Component.literal("Shop"));
        this.balance = payload.balance();
        this.pco = payload.pco();
        this.shopTitle = payload.title();
        this.currencyType = payload.currency();
        this.itemData = payload.items();
        
        this.imageWidth = 256;
        this.imageHeight = 256;
        this.inventoryLabelY = 1000;
        this.titleLabelY = 1000;

        String folder = currencyType.equals("POKE") ? "pokedollar" : "pco";
        this.backgroundTexture = ResourceLocation.fromNamespaceAndPath(
            CobblemonEconomy.MOD_ID, 
            "textures/gui/shop/" + folder + "/0.png"
        );

        // Remplir les slots du shop avec les objets et leurs lores
        for (int i = 0; i < itemData.size(); i++) {
            OpenShopPayload.ShopItemData data = itemData.get(i);
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.id())));
            
            String priceLabel = data.price() + (currencyType.equals("PCO") ? " PCo" : "₽");
            ChatFormatting color = currencyType.equals("PCO") ? ChatFormatting.AQUA : ChatFormatting.GOLD;
            
            stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Prix : ").withStyle(ChatFormatting.GRAY).append(Component.literal(priceLabel).withStyle(color)),
                Component.literal(""),
                Component.literal("▶ Clic gauche pour acheter").withStyle(ChatFormatting.YELLOW)
            )));
            
            this.getMenu().getSlot(i).set(stack);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Clic gauche
            for (int i = 0; i < itemData.size(); i++) {
                Slot slot = this.getMenu().getSlot(i);
                // Vérifier si la souris est sur l'un des 6 slots du shop
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    ClientPlayNetworking.send(new PurchasePayload(itemData.get(i).id(), currencyType.equals("PCO")));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(backgroundTexture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);

        // Titre
        graphics.drawCenteredString(this.font, shopTitle.replace("⚔️", "").replace("⭐", "").trim(), this.leftPos + 128, this.topPos + 35, 0xFFFFFF);
        
        // Solde
        String balText = currencyType.equals("POKE") ? format(balance) + "₽" : format(pco) + " PCo";
        int color = currencyType.equals("POKE") ? 0x55FF55 : 0x55FFFF;
        graphics.drawString(this.font, balText, this.leftPos + 155, this.topPos + 15, color, false);
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
