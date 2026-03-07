package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuestBoardActionPayload(String boardId, String questId, String action) implements CustomPacketPayload {
    public static final Type<QuestBoardActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "quest_board_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestBoardActionPayload> CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, QuestBoardActionPayload::boardId,
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, QuestBoardActionPayload::questId,
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, QuestBoardActionPayload::action,
            QuestBoardActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
