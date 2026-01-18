package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PurchasePayload(String itemId, boolean isPco) implements CustomPacketPayload {
    public static final Type<PurchasePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "purchase"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PurchasePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PurchasePayload::itemId,
            ByteBufCodecs.BOOL, PurchasePayload::isPco,
            PurchasePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
