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

    public static final net.neoforged.neoforge.registries.DeferredRegister.Items ITEMS =
            net.neoforged.neoforge.registries.DeferredRegister.createItems(MODID);

    public static final net.neoforged.neoforge.registries.DeferredItem<com.aicompanion.bot.area.SurveyorWandItem> SURVEYOR_WAND =
            ITEMS.registerItem("surveyor_wand", com.aicompanion.bot.area.SurveyorWandItem::new, p -> p.stacksTo(1));

    public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.aicompanion.bot.menu.BotDashboardMenu>> BOT_DASHBOARD_MENU =
            MENUS.register("bot_dashboard", () -> new net.minecraft.world.inventory.MenuType<>(com.aicompanion.bot.menu.BotDashboardMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.item.CreativeModeTab> CREATIVE_MODE_TABS =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.CreativeModeTab, net.minecraft.world.item.CreativeModeTab> COMPANION_TAB =
            CREATIVE_MODE_TABS.register("companion_tab", () -> net.minecraft.world.item.CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.aicompanion"))
                    .icon(() -> new net.minecraft.world.item.ItemStack(SURVEYOR_WAND.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(SURVEYOR_WAND.get());
                    }).build());

    public AiCompanionMod(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) {
            com.aicompanion.bot.client.AiCompanionClient.init(modEventBus);
        }
        modContainer.registerConfig(ModConfig.Type.LOCAL, BotConfig.SPEC);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Gemini AI Companion Bot Mod (NeoForge 26.3.0.48-beta) initialized!");
    }

    private void addCreative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SURVEYOR_WAND.get());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        BotCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onEntityInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof AiBotPlayer bot && event.getEntity() instanceof ServerPlayer player) {
            if (!player.level().isClientSide()) {
                bot.openCompanionGui(player);
            }
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
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
        if (BotManager.isServerStopping() || !BotPersistence.hasLoadedFromDisk()) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel.dimension() == Level.OVERWORLD) {
            BotPersistence.saveAll(serverLevel.getServer());
        }
    }
}
