package com.aicompanion.bot;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BotManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, AiBotPlayer> ACTIVE_BOTS = new LinkedHashMap<>();
    private static volatile boolean isServerStopping = false;

    public static void setServerStopping(boolean stopping) {
        isServerStopping = stopping;
    }

    public static boolean isServerStopping() {
        return isServerStopping;
    }

    public static synchronized Collection<AiBotPlayer> getActiveBots() {
        return new ArrayList<>(ACTIVE_BOTS.values());
    }

    public static synchronized @Nullable AiBotPlayer getBot(String name) {
        if (name == null) return null;
        String key = name.toLowerCase(Locale.ROOT).trim();
        AiBotPlayer direct = ACTIVE_BOTS.get(key);
        if (direct != null) return direct;
        for (AiBotPlayer bot : ACTIVE_BOTS.values()) {
            if (bot.matchesIdentifier(key)) {
                return bot;
            }
        }
        return null;
    }

    public static synchronized AiBotPlayer spawnBot(ServerLevel level, String name, String role, double x, double y, double z, float yaw, float pitch) {
        String key = name.toLowerCase(Locale.ROOT).trim();
        if (ACTIVE_BOTS.containsKey(key)) {
            removeBot(name, false);
        }

        UUID uuid = UUID.nameUUIDFromBytes(("AiBotCompanion:" + key).getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(uuid, name);

        AiBotPlayer bot = new AiBotPlayer(level, profile, role);
        bot.setPos(x, y, z);
        bot.setYRot(yaw);
        bot.setYHeadRot(yaw);
        bot.setXRot(pitch);
        bot.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        bot.setHealth(bot.getMaxHealth());

        // New bot villagers start out as adorable baby bot villagers
        if (role != null && role.equalsIgnoreCase("villager")) {
            bot.setBaby(true);
        }

        // Give starter rations and role-specific kit
        bot.getInventory().add(new ItemStack(Items.BREAD, 8));
        bot.applyRoleStarterKit();

        String assignedSkin = com.aicompanion.bot.skin.BotSkinManager.determineSkinForBot(name, role, bot.getAliases(), bot.getUUID());
        bot.setCustomSkinName(assignedSkin);

        BotPersistence.markBotCreated();
        registerBotInWorldAndPlayerList(level, bot);
        ACTIVE_BOTS.put(key, bot);
        level.setChunkForced(bot.blockPosition().getX() >> 4, bot.blockPosition().getZ() >> 4, true);

        if (!bot.getCustomSkinName().isBlank()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(new com.aicompanion.bot.skin.SyncBotSkinPayload(bot.getUUID(), bot.getCustomSkinName()));
        }

        BotPersistence.saveAll(level.getServer());

        if (BotConfig.getCandidateApiKeys(null).isEmpty()) {
            bot.sayInChat("Hey! I'm online (" + bot.getBotRole() + ") in Offline Tactical Mode. Give me orders in chat, or type /bot apikey for full Gemini conversational AI!");
        } else {
            bot.sayInChat("Hey! I'm online and ready to help (" + bot.getBotRole() + "). Mention my name in chat or use /bot ask " + name + " <request>!");
        }
        return bot;
    }

    public static synchronized AiBotPlayer spawnBotFromSave(ServerLevel level, BotPersistence.SavedBotData data) {
        String key = data.name.toLowerCase(Locale.ROOT).trim();
        if (ACTIVE_BOTS.containsKey(key)) {
            removeBot(data.name, false);
        }

        UUID uuid = UUID.nameUUIDFromBytes(("AiBotCompanion:" + key).getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(uuid, data.name);

        AiBotPlayer bot = new AiBotPlayer(level, profile, data.role);
        bot.setPos(data.x, data.y, data.z);
        bot.setYRot(data.yaw);
        bot.setYHeadRot(data.yaw);
        bot.setXRot(data.pitch);
        bot.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        bot.setHealth(Math.max(1.0f, data.health));
        bot.getFoodData().setFoodLevel(data.foodLevel);
        bot.setBaby(data.isBaby, data.babyAgeTicks);

        if (data.inventory != null) {
            for (BotPersistence.SavedItemStack s : data.inventory) {
                ItemStack stack = BotPersistence.resolveStack(s.itemId, s.count);
                if (stack.isEmpty()) continue;
                if (s.equipSlot != null && !s.equipSlot.isEmpty()) {
                    for (EquipmentSlot eq : EquipmentSlot.values()) {
                        if (eq.getName().equalsIgnoreCase(s.equipSlot)) {
                            bot.setItemSlot(eq, stack);
                            break;
                        }
                    }
                } else if (s.slot >= 0 && s.slot < bot.getInventory().getContainerSize()) {
                    bot.getInventory().setItem(s.slot, stack);
                } else {
                    bot.getInventory().add(stack);
                }
            }
        }

        BotPersistence.markBotCreated();
        registerBotInWorldAndPlayerList(level, bot);
        ACTIVE_BOTS.put(key, bot);
        level.setChunkForced(bot.blockPosition().getX() >> 4, bot.blockPosition().getZ() >> 4, true);

        if (data.skinName != null && !data.skinName.isBlank()) {
            bot.setCustomSkinName(data.skinName);
        } else {
            String assignedSkin = com.aicompanion.bot.skin.BotSkinManager.determineSkinForBot(data.name, data.role, data.aliases, bot.getUUID());
            bot.setCustomSkinName(assignedSkin);
        }
        if (!bot.getCustomSkinName().isBlank()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(new com.aicompanion.bot.skin.SyncBotSkinPayload(bot.getUUID(), bot.getCustomSkinName()));
        }

        return bot;
    }

    @SuppressWarnings("unchecked")
    private static void registerBotInWorldAndPlayerList(ServerLevel level, AiBotPlayer bot) {
        MinecraftServer server = level.getServer();
        PlayerList playerList = server.getPlayerList();

        // Add to PlayerList.players & playersByUUID so JourneyMap world radar & minimap show the bot as a real player icon!
        try {
            for (Field f : PlayerList.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(playerList);
                if (val instanceof List<?> list && f.getName().equals("players")) {
                    List<ServerPlayer> pList = (List<ServerPlayer>) list;
                    if (!pList.contains(bot)) {
                        pList.add(bot);
                    }
                } else if (val instanceof Map<?, ?> map && f.getName().equals("playersByUUID")) {
                    Map<UUID, ServerPlayer> uuidMap = (Map<UUID, ServerPlayer>) map;
                    uuidMap.put(bot.getUUID(), bot);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not register bot in PlayerList fields for map radar", e);
        }

        ClientboundPlayerInfoUpdatePacket infoPacket = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(bot));
        playerList.broadcastAll(infoPacket);
        level.addNewPlayer(bot);
    }

    @SuppressWarnings("unchecked")
    private static void unregisterBotFromPlayerList(PlayerList playerList, AiBotPlayer bot) {
        try {
            for (Field f : PlayerList.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(playerList);
                if (val instanceof List<?> list && f.getName().equals("players")) {
                    list.remove(bot);
                } else if (val instanceof Map<?, ?> map && f.getName().equals("playersByUUID")) {
                    map.remove(bot.getUUID());
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized boolean removeBot(String name) {
        return removeBot(name, true);
    }

    public static synchronized boolean removeBot(String name, boolean saveChanges) {
        if (name == null) return false;
        AiBotPlayer target = getBot(name);
        if (target == null) return false;
        String key = target.getBotName().toLowerCase(Locale.ROOT).trim();
        AiBotPlayer bot = ACTIVE_BOTS.remove(key);
        if (bot == null) return false;

        bot.clearActions();
        ServerLevel level = bot.level();
        level.setChunkForced(bot.blockPosition().getX() >> 4, bot.blockPosition().getZ() >> 4, false);
        MinecraftServer server = level.getServer();
        unregisterBotFromPlayerList(server.getPlayerList(), bot);
        level.removePlayerImmediately(bot, Entity.RemovalReason.DISCARDED);
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(bot.getUUID())));
        if (ACTIVE_BOTS.isEmpty()) {
            BotPersistence.markExplicitlyCleared();
        }
        if (saveChanges) {
            BotPersistence.saveAll(server);
        }
        return true;
    }

    public static synchronized int removeAllBots() {
        BotPersistence.markExplicitlyCleared();
        List<String> names = new ArrayList<>(ACTIVE_BOTS.keySet());
        int count = 0;
        for (String name : names) {
            if (removeBot(name, true)) count++;
        }
        return count;
    }

    public static synchronized void despawnForShutdownWithoutWipingSave(MinecraftServer server) {
        isServerStopping = true;
        BotPersistence.saveAll(server);
    }

    public static synchronized void onPlayerLoggedIn(ServerPlayer joiningPlayer) {
        if (joiningPlayer instanceof AiBotPlayer) return;
        if (ACTIVE_BOTS.isEmpty()) return;

        List<ServerPlayer> botsAsPlayers = new ArrayList<>(ACTIVE_BOTS.values());
        ClientboundPlayerInfoUpdatePacket packet = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(botsAsPlayers);
        joiningPlayer.connection.send(packet);

        for (AiBotPlayer bot : ACTIVE_BOTS.values()) {
            if (!bot.getCustomSkinName().isBlank()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(joiningPlayer, new com.aicompanion.bot.skin.SyncBotSkinPayload(bot.getUUID(), bot.getCustomSkinName()));
            }
        }

        for (AiBotPlayer bot : ACTIVE_BOTS.values()) {
            if (bot.level() == joiningPlayer.level()) {
                bot.level().getChunk(bot.blockPosition());
                bot.level().getChunkSource().move(bot);

                // If joining player is in proximity, immediately send entity spawn packets
                double distSq = bot.distanceToSqr(joiningPlayer);
                if (distSq <= 16384.0) { // within 128 blocks
                    joiningPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(bot, 0, bot.blockPosition()));
                    java.util.List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> nonDefault = bot.getEntityData().getNonDefaultValues();
                    if (nonDefault != null && !nonDefault.isEmpty()) {
                        joiningPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(bot.getId(), nonDefault));
                    }
                    joiningPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundRotateHeadPacket(bot, (byte)(bot.getYHeadRot() * 256.0F / 360.0F)));

                    List<com.mojang.datafixers.util.Pair<EquipmentSlot, ItemStack>> eqList = new ArrayList<>();
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        ItemStack item = bot.getItemBySlot(slot);
                        if (!item.isEmpty()) {
                            eqList.add(com.mojang.datafixers.util.Pair.of(slot, item));
                        }
                    }
                    if (!eqList.isEmpty()) {
                        joiningPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(bot.getId(), eqList));
                    }
                }
            }
        }
    }

    public static void handleChatMessage(ServerPlayer speaker, String rawMessage) {
        if (speaker instanceof AiBotPlayer) return;
        if (rawMessage == null || rawMessage.isBlank()) return;

        Collection<AiBotPlayer> bots = getActiveBots();
        if (bots.isEmpty()) return;

        String lower = rawMessage.toLowerCase(Locale.ROOT).trim();

        // Check if player is teaching a location in chat: "remember this spot as Town" / "save location Town"
        java.util.regex.Matcher rememberMatcher = java.util.regex.Pattern
                .compile("(?:remember|save)\\s+(?:this\\s+(?:spot|place|location)\\s+as|location|waypoint)\\s+([a-zA-Z0-9_-]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(rawMessage);
        if (rememberMatcher.find()) {
            String locName = rememberMatcher.group(1);
            BotPersistence.saveLocation(locName, speaker.blockPosition(), speaker.level().dimension().identifier().toString());
            BotPersistence.saveAll(speaker.level().getServer());
            AiBotPlayer nearest = findNearestBot(speaker, bots);
            if (nearest != null) {
                nearest.sayInChat("Got it! I saved '" + locName + "' at ("
                        + speaker.getBlockX() + ", " + speaker.getBlockY() + ", " + speaker.getBlockZ() + ") in persistent memory.");
            }
            return;
        }

        List<AiBotPlayer> targets = new ArrayList<>();

        if (lower.startsWith("bots") || lower.startsWith("@bots") || lower.startsWith("everyone") || lower.contains("all bots")) {
            targets.addAll(bots);
        } else {
            for (AiBotPlayer bot : bots) {
                if (bot.containsBotReference(rawMessage)) {
                    targets.add(bot);
                }
            }
        }

        if (targets.isEmpty()) {
            for (AiBotPlayer bot : bots) {
                if (bot.isAwaitingExcavationReply()) {
                    targets.add(bot);
                }
            }
        }

        if (targets.isEmpty()) {
            if (lower.startsWith("bot ") || lower.startsWith("bot,") || lower.startsWith("@bot") || lower.startsWith("hey bot")) {
                AiBotPlayer nearest = findNearestBot(speaker, bots);
                if (nearest != null) targets.add(nearest);
            } else if (lower.contains("nickname") || lower.contains("rename") || lower.contains("call you") || lower.contains("name you")) {
                AiBotPlayer nearest = findNearestBot(speaker, bots);
                if (nearest != null) targets.add(nearest);
            } else if (lower.contains("tree") || lower.contains("trees") || lower.contains("wood") || lower.contains("log") || lower.contains("logs") || lower.contains("chop") || lower.contains("timber") || lower.contains("lumber")) {
                AiBotPlayer nearestLumber = null;
                for (AiBotPlayer b : bots) {
                    if (b.getBotRole().toLowerCase().contains("lumber") || b.getBotRole().toLowerCase().contains("wood") || b.getBotName().toLowerCase().contains("lumber")) {
                        nearestLumber = b;
                        break;
                    }
                }
                AiBotPlayer candidate = nearestLumber != null ? nearestLumber : findNearestBot(speaker, bots);
                if (candidate != null) targets.add(candidate);
            } else if (lower.contains("wheat") || lower.contains("hay") || lower.contains("compost") || lower.contains("crop") || lower.contains("seed")) {
                AiBotPlayer nearestFarmer = null;
                for (AiBotPlayer b : bots) {
                    if (b.getBotRole().toLowerCase().contains("farm")) {
                        nearestFarmer = b;
                        break;
                    }
                }
                AiBotPlayer candidate = nearestFarmer != null ? nearestFarmer : findNearestBot(speaker, bots);
                if (candidate != null) targets.add(candidate);
            } else if (lower.contains("good job") || lower.contains("bad job") || lower.contains("good bot")
                    || lower.contains("bad bot") || lower.contains("well done") || lower.contains("nice job")
                    || lower.contains("wrong way") || lower.contains("bad path") || lower.contains("good path")
                    || lower.contains("nice path") || lower.contains("learn this path") || lower.contains("watch how i walk")
                    || lower.contains("stop learning path") || lower.contains("done showing path")) {
                AiBotPlayer nearest = findNearestBot(speaker, bots);
                if (nearest != null) targets.add(nearest);
            } else if (bots.size() == 1 && BotConfig.LISTEN_ALL_WHEN_SINGLE_BOT.getAsBoolean()) {
                targets.add(bots.iterator().next());
            }
        }

        for (AiBotPlayer targetBot : targets) {
            dispatchRequestToBot(targetBot, speaker, rawMessage);
        }
    }

    public static @Nullable AiBotPlayer findNearestBot(ServerPlayer speaker, Collection<AiBotPlayer> bots) {
        AiBotPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (AiBotPlayer b : bots) {
            if (b.level() == speaker.level()) {
                double d = b.distanceToSqr(speaker);
                if (d < bestDist) {
                    bestDist = d;
                    best = b;
                }
            }
        }
        return best != null ? best : (bots.isEmpty() ? null : bots.iterator().next());
    }

    public static void dispatchRequestToBot(AiBotPlayer bot, @Nullable ServerPlayer speaker, String requestText) {
        MinecraftServer server = bot.level().getServer();
        if (!server.isSameThread()) {
            server.execute(() -> dispatchRequestToBot(bot, speaker, requestText));
            return;
        }
        String speakerName = speaker != null ? speaker.getPlainTextName() : "Admin";
        bot.setLastInteractingPlayer(speaker);

        String rawLower = requestText.toLowerCase(Locale.ROOT).trim();
        boolean hasICommand = rawLower.startsWith("i command") || rawLower.contains(" i command");

        String cmdClean = bot.stripBotReference(requestText).toLowerCase().replaceAll("[,!?:;.]", " ").replaceAll("\\s+", " ").trim();

        // 1. If player explicitly prefixed request with "i command":
        if (hasICommand) {
            String orderPart = rawLower.replaceAll(".*\\bi\\s+command(?:\\s+you)?(?:\\s+to)?(?:\\s+that)?[:]?", "").trim();
            String orderClean = bot.stripBotReference(orderPart).toLowerCase().replaceAll("[,!?:;.]", " ").replaceAll("\\s+", " ").trim();

            if (orderClean.equals("stop") || orderClean.equals("halt") || orderClean.equals("cancel")
                    || orderClean.equals("pause") || orderClean.equals("freeze") || orderClean.equals("stop it")
                    || orderClean.equals("stop all") || orderClean.equals("standby")) {
                bot.setAbsoluteStop();
                GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
                BotPersistence.saveAll(server);
                return;
            }

            if (speaker != null && (orderClean.equals("come here") || orderClean.equals("come") || orderClean.equals("come to me")
                    || orderClean.equals("follow me") || orderClean.equals("follow") || orderClean.equals("to me")
                    || orderClean.equals("regroup") || orderClean.equals("on me") || orderClean.startsWith("come here")
                    || orderClean.startsWith("follow me") || orderClean.equals("get over here"))) {
                bot.setAbsoluteFollow(speaker);
                GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
                BotPersistence.saveAll(server);
                return;
            }

            if (orderClean.equals("stay here") || orderClean.equals("stand here") || orderClean.equals("stay")
                    || orderClean.equals("stand") || orderClean.equals("wait here") || orderClean.equals("hold position")
                    || orderClean.equals("hold ground") || orderClean.equals("hold here") || orderClean.equals("camp here")
                    || orderClean.equals("stand by") || orderClean.equals("wait")) {
                bot.setAbsoluteTether(bot.blockPosition(), 4.0);
                GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
                BotPersistence.saveAll(server);
                return;
            }

            if (orderClean.equals("sentry") || orderClean.equals("sentry mode") || orderClean.equals("stand sentry")
                    || orderClean.equals("guard sentry") || orderClean.equals("sentry duty") || orderClean.equals("sniper mode")
                    || orderClean.equals("be a sentry") || orderClean.equals("hold sentry") || orderClean.equals("start sentry")) {
                bot.setAbsoluteSentry();
                GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
                BotPersistence.saveAll(server);
                return;
            }

            // Other tasks prefixed with "i command" place the bot in Absolute Command Mode
            bot.setAbsoluteCommandMode(true);
        } else {
            // Normal (Non-Absolute) Controls:
            // Immediate Stop: only triggers on standalone stop/halt/cancel words (NOT "stop combining...", "stop farming...")
            if (cmdClean.equals("stop") || cmdClean.equals("halt") || cmdClean.equals("cancel")
                    || cmdClean.equals("pause") || cmdClean.equals("freeze") || cmdClean.equals("stop it")
                    || cmdClean.equals("stop all") || cmdClean.equals("stop current task") || cmdClean.equals("stop working")) {
                bot.clearActions();
                bot.stopMovement();
                bot.setStandingBy(false);
                bot.setAbsoluteCommandMode(false);
                bot.sayInChat("Stopped current tasks.");
                GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
                BotPersistence.saveAll(server);
                return;
            }

            // Normal Follow (without locking into Absolute Mode)
            if (speaker != null && (cmdClean.equals("come here") || cmdClean.equals("come") || cmdClean.equals("come to me")
                    || cmdClean.equals("follow me") || cmdClean.equals("follow") || cmdClean.equals("to me")
                    || cmdClean.equals("regroup") || cmdClean.equals("on me") || cmdClean.startsWith("come here")
                    || cmdClean.startsWith("follow me") || cmdClean.equals("get over here"))) {
                bot.clearActions();
                BotAction followAct = new BotAction("FOLLOW", speaker.getPlainTextName(), "", 2);
                followAct.distance = 2.5;
                bot.setActionPlan(List.of(followAct));
                bot.setAbsoluteCommandMode(false);
                bot.setStandingBy(false);
                bot.setTetherPos(null);
                bot.setSentryMode(false, null);
                bot.sayInChat("Following you, " + speaker.getPlainTextName() + "!");
                GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
                BotPersistence.saveAll(server);
                return;
            }

            // Normal Hold / Stay / Tether
            if (cmdClean.equals("stay here") || cmdClean.equals("stand here") || cmdClean.equals("stay")
                    || cmdClean.equals("stand") || cmdClean.equals("wait here") || cmdClean.equals("hold position")
                    || cmdClean.equals("hold ground") || cmdClean.equals("hold here") || cmdClean.equals("camp here")
                    || cmdClean.equals("standby") || cmdClean.equals("stand by") || cmdClean.equals("wait")) {
                bot.clearActions();
                bot.setAbsoluteCommandMode(false);
                bot.setStandingBy(true);
                bot.setTetherPos(bot.blockPosition());
                bot.setTetherRadius(4.0);
                bot.sayInChat("Holding position at (" + bot.getBlockX() + ", " + bot.getBlockY() + ", " + bot.getBlockZ() + ") with a 4m tether.");
                GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
                BotPersistence.saveAll(server);
                return;
            }

            // Normal Sentry Mode
            if (cmdClean.equals("sentry") || cmdClean.equals("sentry mode") || cmdClean.equals("stand sentry")
                    || cmdClean.equals("guard sentry") || cmdClean.equals("sentry duty") || cmdClean.equals("sniper mode")
                    || cmdClean.equals("be a sentry") || cmdClean.equals("hold sentry") || cmdClean.equals("start sentry")) {
                bot.clearActions();
                bot.setAbsoluteCommandMode(false);
                bot.setStandingBy(true);
                bot.setSentryMode(true, bot.blockPosition());
                bot.setTetherPos(bot.blockPosition());
                bot.setTetherRadius(2.0);
                bot.equipBow();
                bot.sayInChat("Sentry Mode activated at (" + bot.getBlockX() + ", " + bot.getBlockY() + ", " + bot.getBlockZ() + ")!");
                GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
                BotPersistence.saveAll(server);
                return;
            }
        }

        // Resume Autonomous: "resume", "continue", "back to work", "dismiss", "free roam"
        if (cmdClean.equals("resume") || cmdClean.equals("continue") || cmdClean.equals("back to work")
                || cmdClean.equals("dismiss") || cmdClean.equals("free roam") || cmdClean.equals("carry on")) {
            bot.resumeAutonomous();
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleReinforcementChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleHousingChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleVillagerChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleRoleChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleConductorChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleTrainingChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleBuilderChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleExcavationAndMiningChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleLumberjackChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleExplorationChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.handleFarmerChatCommand(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        // Check if player mentions any saved global area (e.g. Wheat_Farm, Base, Compound):
        com.aicompanion.bot.area.SavedArea mentionedArea = null;
        for (com.aicompanion.bot.area.SavedArea a : BotPersistence.getAllAreas().values()) {
            if (cmdClean.contains(a.name.toLowerCase(Locale.ROOT)) || requestText.toLowerCase(Locale.ROOT).contains(a.name.toLowerCase(Locale.ROOT))) {
                mentionedArea = a;
                break;
            }
        }

        if (mentionedArea != null) {
            String lowerReq = requestText.toLowerCase(Locale.ROOT);
            boolean isGuardOrStand = lowerReq.contains("guard") || lowerReq.contains("defend")
                    || lowerReq.contains("protect") || lowerReq.contains("sentry") || lowerReq.contains("watch")
                    || lowerReq.contains("patrol") || lowerReq.contains("stand") || lowerReq.contains("stay")
                    || lowerReq.contains("hold") || lowerReq.contains("camp") || lowerReq.contains("remain")
                    || lowerReq.contains("not farm") || lowerReq.contains("do no farm") || lowerReq.contains("don't farm")
                    || lowerReq.contains("dont farm") || lowerReq.contains("not a farmer") || lowerReq.contains("no farm")
                    || lowerReq.contains("stop farm");

            boolean isExplicitFarm = (lowerReq.contains("farm ") || lowerReq.contains("harvest ") || lowerReq.contains("tend ")
                    || lowerReq.startsWith("farm") || lowerReq.startsWith("harvest") || lowerReq.startsWith("tend"))
                    && !isGuardOrStand;

            if (isGuardOrStand) {
                bot.setAssignedArea(mentionedArea.name);
                if (!bot.getBotRole().toLowerCase().contains("guard") && !bot.getBotRole().toLowerCase().contains("warrior")) {
                    bot.setBotRole("guard");
                }
                BlockPos anchorPos = (speaker != null && mentionedArea.contains(speaker.blockPosition())) ? speaker.blockPosition() : bot.blockPosition();
                double anchorRadius = Math.max(mentionedArea.getMaxX() - mentionedArea.getMinX(), mentionedArea.getMaxZ() - mentionedArea.getMinZ()) / 2.0;
                bot.setGuardAnchor(anchorPos, anchorRadius);

                if (lowerReq.contains("stay") || lowerReq.contains("stand") || lowerReq.contains("hold")) {
                    bot.setAbsoluteTether(anchorPos, Math.max(4.0, Math.min(anchorRadius, 16.0)));
                }

                bot.sayInChat("Understood! Standing guard in '" + mentionedArea.name + "' (" + mentionedArea.type + "). I am on guard duty and will defend the area without farming!");
                GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
                BotPersistence.saveAll(server);
                return;
            } else if (isExplicitFarm) {
                bot.configureFarmForSavedArea(mentionedArea);
                GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
                BotPersistence.saveAll(server);
                return;
            }
        }

        if (bot.extractAndLearnNicknameFromPrompt(requestText)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        if (bot.extractAndLearnFarmConfigFromPrompt(requestText, speaker)) {
            GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
            BotPersistence.saveAll(server);
            return;
        }

        boolean learnedAnchor = bot.extractAndLearnAnchorFromPrompt(requestText);
        boolean learnedHusbandry = bot.extractAndLearnHusbandryConfigFromPrompt(requestText, speaker);
        GeminiService.recordMemory(bot.getBotName(), speakerName + ": " + requestText);
        if (learnedAnchor || learnedHusbandry) {
            BotPersistence.saveAll(server);
        }

        String telemetryPrompt = GeminiService.captureTelemetry(bot, speaker, requestText);

        if (speaker != null) {
            speaker.sendSystemMessage(Component.literal("[" + bot.getChatTag() + " is thinking...]")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }

        GeminiService.askGeminiAsync(telemetryPrompt, speakerName, requestText).thenAccept(plan -> {
            server.execute(() -> {
                if (bot.isRemoved()) return;
                if (plan.reply != null && !plan.reply.isBlank()) {
                    bot.sayInChat(plan.reply);
                }
                if (plan.actions != null && !plan.actions.isEmpty()) {
                    bot.setActionPlan(plan.actions);
                }
                BotPersistence.saveAll(server);
            });
        });
    }

    public static void broadcastDistress(AiBotPlayer victim, net.minecraft.world.entity.LivingEntity threat, BlockPos pos) {
        String threatName = threat.getType().getDescription().getString();
        victim.sayInChat("[Distress] Under attack by " + threatName + " at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")! Requesting backup!");

        for (AiBotPlayer bot : ACTIVE_BOTS.values()) {
            if (bot == victim) continue;
            if (bot.level() != victim.level()) continue;
            if (bot.distanceToSqr(victim) > 48.0 * 48.0) continue;

            String role = bot.getBotRole().toLowerCase();
            if (role.contains("guard") || role.contains("bodyguard") || role.contains("warrior") || role.contains("soldier")) {
                bot.respondToDistressCall(threat, victim);
            }
        }
    }
}
