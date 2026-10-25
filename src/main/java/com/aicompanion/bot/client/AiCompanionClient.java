package com.aicompanion.bot.client;

import com.aicompanion.bot.AiCompanionMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class AiCompanionClient {

    public static final KeyMapping.Category COMPANION_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(AiCompanionMod.MODID, "controls"));

    // Default key 'B' (GLFW key code 66)
    public static final KeyMapping KEY_OPEN_GUI =
            new KeyMapping("key.aicompanion.open_gui", 66, COMPANION_CATEGORY);

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(AiCompanionClient::onRegisterMenuScreens);
        modEventBus.addListener(AiCompanionClient::onRegisterKeyMappings);
        modEventBus.addListener(AiCompanionClient::onClientSetup);
        modEventBus.addListener(AiCompanionClient::onRegisterClientPayloads);
        modEventBus.addListener(BotClientSkinManager::onRegisterRenderStateModifiers);
        NeoForge.EVENT_BUS.addListener(AiCompanionClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(AiCompanionClient::onRenderNameTag);
    }

    private static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(BotClientSkinManager::loadAllSkins);
    }

    private static void onRegisterClientPayloads(net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent event) {
        event.register(com.aicompanion.bot.skin.SyncBotSkinPayload.TYPE, (payload, context) -> {
            BotClientSkinManager.setBotSkin(payload.botUuid(), payload.skinName());
        });
    }

    private static void onRenderNameTag(RenderNameTagEvent.DoRender event) {
        EntityRenderState state = event.getEntityRenderState();
        if (state.nameTagAttachment != null && !state.isDiscrete) {
            event.setCanceled(true);
            int offset = (state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState avatarState && avatarState.showExtraEars) ? -10 : 0;
            event.getSubmitNodeCollector().submitNameTag(
                    event.getPoseStack(),
                    state.nameTagAttachment,
                    offset,
                    event.getContent(),
                    false, // Disable seeThrough emission pass so shaders don't bloom nametags into glowing white boxes
                    state.lightCoords,
                    event.getCameraRenderState()
            );
        }
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register((MenuType<com.aicompanion.bot.menu.BotDashboardMenu>) AiCompanionMod.BOT_DASHBOARD_MENU.get(), BotDashboardScreen::new);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_OPEN_GUI);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        while (KEY_OPEN_GUI.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.connection != null) {
                mc.player.connection.sendCommand("bot gui");
            }
        }
    }
}
