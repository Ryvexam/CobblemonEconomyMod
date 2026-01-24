package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ProvideSkinPayload(String skinName, byte[] data) implements CustomPacketPayload {
    public static final Type<ProvideSkinPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "provide_skin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProvideSkinPayload> CODEC = StreamCodec.composite(
        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, ProvideSkinPayload::skinName,
        net.minecraft.network.codec.ByteBufCodecs.byteArray(1048576), ProvideSkinPayload::data, // Max 1MB per skin
        ProvideSkinPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
