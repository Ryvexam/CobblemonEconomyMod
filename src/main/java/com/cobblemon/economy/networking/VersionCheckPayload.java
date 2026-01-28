package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VersionCheckPayload(String version) implements CustomPacketPayload {
    public static final Type<VersionCheckPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "version_check")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, VersionCheckPayload> CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, VersionCheckPayload::version,
            VersionCheckPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
