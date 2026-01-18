package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.util.List;

public record OpenShopPayload(
    BigDecimal balance, 
    BigDecimal pco, 
    String title, 
    String currency, 
    List<ShopItemData> items
) implements CustomPacketPayload {
    public static final Type<OpenShopPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "open_shop"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(BigDecimal::new, BigDecimal::toString), OpenShopPayload::balance,
            ByteBufCodecs.STRING_UTF8.map(BigDecimal::new, BigDecimal::toString), OpenShopPayload::pco,
            ByteBufCodecs.STRING_UTF8, OpenShopPayload::title,
            ByteBufCodecs.STRING_UTF8, OpenShopPayload::currency,
            ShopItemData.CODEC.apply(ByteBufCodecs.list()), OpenShopPayload::items,
            OpenShopPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ShopItemData(String id, String name, int price) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ShopItemData> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShopItemData::id,
                ByteBufCodecs.STRING_UTF8, ShopItemData::name,
                ByteBufCodecs.VAR_INT, ShopItemData::price,
                ShopItemData::new
        );
    }
}
