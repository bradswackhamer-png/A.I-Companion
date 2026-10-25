package com.aicompanion.bot.skin;

import com.aicompanion.bot.AiCompanionMod;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncBotSkinPayload(UUID botUuid, String skinName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncBotSkinPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AiCompanionMod.MODID, "sync_bot_skin"));

    public static final StreamCodec<FriendlyByteBuf, SyncBotSkinPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, SyncBotSkinPayload::botUuid,
                    ByteBufCodecs.STRING_UTF8, SyncBotSkinPayload::skinName,
                    SyncBotSkinPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
