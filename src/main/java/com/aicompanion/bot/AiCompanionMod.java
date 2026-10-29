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
        modEventBus.addListener(this::registerPayloadHandlers);
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

    private void registerPayloadHandlers(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        event.registrar("1.2.0")
                .playToClient(com.aicompanion.bot.skin.SyncBotSkinPayload.TYPE, com.aicompanion.bot.skin.SyncBotSkinPayload.STREAM_CODEC);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        BotCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onEntityInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof AiBotPlayer bot && event.getEntity() instanceof ServerPlayer player) {
            if (!player.level().isClientSide()) {
                net.minecraft.world.item.ItemStack held = player.getItemInHand(event.getHand());
                if (held.is(net.minecraft.world.item.Items.WRITTEN_BOOK) || held.is(net.minecraft.world.item.Items.WRITABLE_BOOK)) {
                    boolean isGlobal = player.isShiftKeyDown();
                    com.aicompanion.bot.memory.BotMemoryManager.StudyResult res =
                            com.aicompanion.bot.memory.BotMemoryManager.studyTrainingBook(held, bot, isGlobal);
                    bot.sayInChat("Studied your training book '" + res.title() + "'! Learned "
                            + res.newGuidelines() + " guidelines and " + res.newRecipes() + " recipes (" + (res.global() ? "Global" : "Personal") + ").");
                    BotPersistence.saveAll(player.level().getServer());
                    event.setCanceled(true);
                    event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                    return;
                }
                if (bot.isBaby() && (held.is(net.minecraft.world.item.Items.WHEAT) || held.is(net.minecraft.world.item.Items.BREAD)
                        || held.is(net.minecraft.world.item.Items.CARROT) || held.is(net.minecraft.world.item.Items.POTATO))) {
                    if (!player.isCreative()) {
                        held.shrink(1);
                    }
                    bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                            net.minecraft.sounds.SoundEvents.VILLAGER_YES,
                            net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.35f);
                    if (bot.level() instanceof ServerLevel sl) {
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, bot.getX(), bot.getY() + 0.6, bot.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
                    }
                    bot.setBabyAgeTicks(Math.max(0, bot.getBabyAgeTicks() - 2400));
                    int minutesRemaining = Math.max(1, (bot.getBabyAgeTicks() + 1199) / 1200);
                    bot.sayInChat("Yum! Thank you! I am growing bigger (~" + minutesRemaining + "m until adult)!");
                    BotPersistence.saveAll(player.level().getServer());
                    event.setCanceled(true);
                    event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                    return;
                }
                if (bot.getBotRole().toLowerCase().contains("villager") && (player.isShiftKeyDown() || player.isCrouching())) {
                    if (bot.isBaby()) {
                        bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                                net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.35f);
                        bot.sayInChat("I'm still just a baby! I can't trade until I grow up!");
                    } else {
                        bot.openVillagerTradeScreen(player);
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                    return;
                }
                if (bot.getBotRole().toLowerCase().contains("villager") && held.is(net.minecraft.world.item.Items.EMERALD)) {
                    if (bot.isBaby()) {
                        bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                                net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.35f);
                        bot.sayInChat("I'm still just a baby! I can't trade emeralds until I grow up!");
                        event.setCanceled(true);
                        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                        return;
                    }
                    if (!player.isCreative()) {
                        held.shrink(1);
                    }
                    bot.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, 1));
                    String sub = bot.getBotSubRole().toLowerCase(java.util.Locale.ROOT);
                    net.minecraft.world.item.ItemStack tradeReward;
                    String tradeDesc;

                    switch (sub) {
                        case "librarian" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK, 2);
                            tradeDesc = "2 Books";
                        }
                        case "armorer" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 2);
                            tradeDesc = "2 Iron Ingots";
                        }
                        case "weaponsmith" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD, 1);
                            tradeDesc = "1 Iron Sword";
                        }
                        case "toolsmith" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE, 1);
                            tradeDesc = "1 Iron Pickaxe";
                        }
                        case "fletcher" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 16);
                            tradeDesc = "16 Arrows";
                        }
                        case "cleric" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE, 4);
                            tradeDesc = "4 Redstone Dust";
                        }
                        case "butcher" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_BEEF, 4);
                            tradeDesc = "4 Cooked Beef";
                        }
                        case "fisherman" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_COD, 6);
                            tradeDesc = "6 Cooked Cod";
                        }
                        case "farmer" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 6);
                            tradeDesc = "6 Bread";
                        }
                        case "shepherd" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("white_wool")), 4);
                            tradeDesc = "4 White Wool";
                        }
                        case "mason" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_BRICKS, 8);
                            tradeDesc = "8 Stone Bricks";
                        }
                        case "cartographer" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MAP, 1);
                            tradeDesc = "1 Empty Map";
                        }
                        case "leatherworker" -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER, 4);
                            tradeDesc = "4 Leather";
                        }
                        default -> {
                            tradeReward = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 4);
                            tradeDesc = "4 Bread";
                        }
                    }

                    player.getInventory().add(tradeReward);
                    bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                            net.minecraft.sounds.SoundEvents.VILLAGER_YES,
                            net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                    String profLabel = "unemployed".equalsIgnoreCase(sub) || "none".equalsIgnoreCase(sub) ? "Villager" : AiBotPlayer.capitalize(sub);
                    bot.sayInChat("Hmm! Pleasure trading with you, " + player.getPlainTextName() + "! [" + profLabel + " Trade: 1 Emerald -> " + tradeDesc + "]");
                    BotPersistence.saveAll(player.level().getServer());
                    event.setCanceled(true);
                    event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                    return;
                }
                if (bot.getBotRole().toLowerCase().contains("villager")) {
                    bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                            net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT,
                            net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                }
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
    public void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player instanceof AiBotPlayer)) {
                com.aicompanion.bot.learning.HiveMindBrain.tickPlayerPathRecording(player);
            }
        }
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
