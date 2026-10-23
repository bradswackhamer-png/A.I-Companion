package com.aicompanion.bot;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

@Mod(AiCompanionMod.MODID)
public class AiCompanionMod {
    public static final String MODID = "aicompanion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AiCompanionMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.LOCAL, BotConfig.SPEC);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Gemini AI Companion Bot Mod (NeoForge 26.3.0.48-beta) initialized!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        BotCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String rawText = event.getRawText();
        // ServerChatEvent fires on the Netty network thread in 26.3; always schedule world access onto the main ServerThread!
        player.level().getServer().execute(() -> BotManager.handleChatMessage(player, rawText));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BotManager.onPlayerLoggedIn(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        BotManager.setServerStopping(false);
        BotPersistence.loadAll(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        BotManager.setServerStopping(true);
        BotPersistence.saveAll(event.getServer());
    }

    @SubscribeEvent
    public void onLevelSave(LevelEvent.Save event) {
        if (BotManager.isServerStopping()) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel.dimension() == Level.OVERWORLD) {
            BotPersistence.saveAll(serverLevel.getServer());
        }
    }
}
