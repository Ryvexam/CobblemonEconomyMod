package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenQuestBoardPayload(String boardId, String jsonState) implements CustomPacketPayload {
    public static final Type<OpenQuestBoardPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "open_quest_board"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenQuestBoardPayload> CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, OpenQuestBoardPayload::boardId,
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, OpenQuestBoardPayload::jsonState,
            OpenQuestBoardPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
