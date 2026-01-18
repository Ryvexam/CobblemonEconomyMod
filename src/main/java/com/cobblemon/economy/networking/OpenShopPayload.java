package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;

public record OpenShopPayload(BigDecimal balance, BigDecimal pco, String shopType) implements CustomPacketPayload {
    public static final Type<OpenShopPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "open_shop"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(BigDecimal::new, BigDecimal::toString), OpenShopPayload::balance,
            ByteBufCodecs.STRING_UTF8.map(BigDecimal::new, BigDecimal::toString), OpenShopPayload::pco,
            ByteBufCodecs.STRING_UTF8, OpenShopPayload::shopType,
            OpenShopPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
