package com.aicompanion.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public class GeminiService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Gemini-AI-Bot-Worker");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .executor(EXECUTOR)
            .build();

    private static final Map<String, Deque<String>> BOT_MEMORY = new HashMap<>();

    private static final String SYSTEM_INSTRUCTION = """
            You are an intelligent, autonomous Minecraft AI Companion player on a multiplayer NeoForge 26.3 server.
            You play alongside real human players as a helpful, proactive teammate.
            You MUST respond strictly with a single JSON object matching this schema:
            {
              "thought": "Your step-by-step tactical reasoning based on your inventory, health, nearby blocks/entities, and the player's request.",
              "reply": "A concise, natural, friendly in-game chat message to the player (1-2 sentences max).",
              "actions": [
                {
                  "type": "ACTION_TYPE",
                  "target": "block_id, item_id, mob_type, structure_type, or player_name",
                  "item": "item_id (used for GIVE)",
                  "count": 1,
                  "x": null,
                  "y": null,
                  "z": null,
                  "distance": 3.0
                }
              ]
            }

            AVAILABLE ACTION TYPES (you may chain multiple actions in sequence in the "actions" array):
            1. "FOLLOW" -> Continuously follow a player and defend them. Set "target" to the player's name, "distance" to 3.0.
            2. "GOTO" -> Walk/jump/swim to a player or coordinates. Set "target" to player name OR set "x", "y", "z" integers.
            3. "GUARD" -> Guard a player or your current spot, aggressively hunting any hostile mobs nearby. Set "target" to player name (or "here").
            4. "MINE" -> Find, mine, and pick up blocks. Set "target" to the block name or category (e.g. "log", "oak_log", "stone", "cobblestone", "coal_ore", "iron_ore", "diamond_ore", "sand", "dirt", or a full modded block ID like "modid:custom_ore"), and "count" to how many blocks to mine.
            5. "CRAFT" -> Craft items using your inventory. Set "target" to the item name (e.g. "oak_planks", "stick", "crafting_table", "wooden_pickaxe", "stone_pickaxe", "stone_sword", "stone_axe", "iron_pickaxe", "iron_sword", "torch", "chest", "furnace", "bread", "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots", "shield") and "count" to how many to craft. (Note: The CRAFT engine automatically crafts intermediate planks/sticks from logs if you have logs in your inventory!).
            6. "GIVE" -> Walk to a player and toss items to them. Set "target" to the player's name, "item" to the item name (or "all" to give all gathered items), and "count" to the amount.
            7. "ATTACK" -> Hunt and kill nearby mobs. Set "target" to "hostile" (for all monsters), or a specific mob name (e.g. "zombie", "skeleton", "creeper", "spider", "cow", "pig", "sheep", "chicken"), and "count" to how many to kill.
            8. "EQUIP" -> Equip gear from inventory. Set "target" to "best" (automatically equips best armor, weapon, and offhand shield/torches) or a specific item name.
            9. "PLACE" -> Place a block or build a structure near your position. Set "target" to "shelter" (builds a protective 5x5 survival shelter), "wall", "platform", "pillar", "torch", "crafting_table", "chest", or "furnace".
            10. "CHEST_STORE" -> Deposit gathered resources into the nearest chest. Set "target" to "all" or a specific item name.
            11. "CHEST_TAKE" -> Take items from the nearest chest. Set "target" to the item name and "count" to the amount.
            12. "FARM" -> Systematically tend your assigned farm area: harvest ONLY 100% fully grown mature crops, immediately replant matching seeds into farmland, plant empty farmland, till untilled dirt with a hoe, and store excess crops into a nearby chest while always keeping a supply of seeds in inventory. Set "target" to "all", "wheat", "carrots", "potatoes", or "beetroots", and "count" to the number of crops (e.g. 20).
            13. "TILL" -> Till dirt/grass in your assigned farm zone using a hoe.
            14. "BREED" -> Feed livestock that are ready to mate. Set "target" to animal ("cows", "sheep", "pigs", "chickens"), "item" to breeding food ("wheat", "carrots", "seeds").
            15. "CULL" -> Cull excess adult animals when herd size exceeds threshold. Set "target" to animal ("cows"), "count" to max herd size. Strictly never kills babies and preserves minimum herd size.
            16. "HUSBANDRY" -> Complete livestock management: breed animals when ready, cull excess adults if herd size exceeds limit, and deposit meat/leather/wool into nearby chest. Set "target" to animal ("cows"), "item" to food ("wheat"), "count" to max herd size.
            17. "EAT" -> Eat food from your inventory immediately.
            18. "STOP" -> Stop all current tasks and stand still.

            CRITICAL INTELLIGENCE RULES:
            - Multi-step planning: If a player asks for something you don't currently have (e.g., "bring me a stone pickaxe" or "get me 10 iron ore"), check your inventory first!
              * If you need to mine stone or coal_ore and don't have a pickaxe, first MINE 3 "log", then CRAFT 1 "wooden_pickaxe", then MINE the stone!
              * If you need to mine iron_ore and don't have a stone/iron/diamond pickaxe, make sure you get a stone_pickaxe first!
              * If the player says "bring me...", "give me...", or "get me...", ALWAYS append a "GIVE" action at the end of your "actions" array with "target" set to the requesting player's name!
            - If the player just asks a question (e.g. "what's in your inventory?", "where are you?", "how's your health?"), answer accurately in "reply" using the LIVE TELEMETRY below and leave "actions": [] empty (so you don't interrupt your current task unless asked).
            - Keep "reply" natural, in-character, and concise.
            """;

    public static void recordMemory(String botName, String line) {
        synchronized (BOT_MEMORY) {
            Deque<String> mem = BOT_MEMORY.computeIfAbsent(botName.toLowerCase(), k -> new ArrayDeque<>());
            mem.addLast(line);
            while (mem.size() > 25) {
                mem.removeFirst();
            }
        }
    }

    public static void setMemory(String botName, List<String> lines) {
        synchronized (BOT_MEMORY) {
            Deque<String> mem = BOT_MEMORY.computeIfAbsent(botName.toLowerCase(), k -> new ArrayDeque<>());
            mem.clear();
            if (lines != null) {
                for (String l : lines) {
                    mem.addLast(l);
                }
            }
        }
    }

    public static List<String> getMemory(String botName) {
        synchronized (BOT_MEMORY) {
            Deque<String> mem = BOT_MEMORY.get(botName.toLowerCase());
            return mem == null ? List.of() : new ArrayList<>(mem);
        }
    }

    public static String captureTelemetry(AiBotPlayer bot, ServerPlayer speaker, String userMessage) {
        StringBuilder sb = new StringBuilder();
        ServerLevel level = bot.level();
        BlockPos botPos = bot.blockPosition();

        sb.append("=== YOUR BOT STATUS ===\n");
        sb.append("Name: ").append(bot.getBotName()).append(" (Role: ").append(bot.getBotRole()).append(")\n");
        if (!bot.getAliases().isEmpty()) {
            sb.append("Active Nickname / Aliases you also respond to: ").append(String.join(", ", bot.getAliases())).append("\n");
        }
        sb.append(String.format("Position: (%d, %d, %d) in %s\n",
                botPos.getX(), botPos.getY(), botPos.getZ(), level.dimension().identifier()));
        sb.append(String.format("Health: %.1f / %.1f | Hunger: %d / 20\n",
                bot.getHealth(), bot.getMaxHealth(), bot.getFoodData().getFoodLevel()));
        sb.append("Current Task: ").append(bot.getCurrentTaskSummary()).append("\n");

        if (bot.getFarmOriginPos() != null) {
            sb.append(String.format("Assigned Farm Zone: Origin=(%d, %d, %d), Size=%dx%d, Allowed Crops=%s, Till Dirt=%b\n",
                    bot.getFarmOriginPos().getX(), bot.getFarmOriginPos().getY(), bot.getFarmOriginPos().getZ(),
                    bot.getFarmRadiusX() * 2, bot.getFarmRadiusZ() * 2,
                    bot.getFarmAllowedCrops().isEmpty() ? "ALL" : String.join(", ", bot.getFarmAllowedCrops()),
                    bot.isFarmAllowTill()));
        }
        if (bot.getRanchOriginPos() != null || bot.getBotRole().toLowerCase().contains("husbandry") || bot.getBotRole().toLowerCase().contains("ranch")) {
            sb.append(String.format("Assigned Husbandry Duty: Animal=%s, Breeding Food=%s, Max Cap=%d, Min Preserved=%d, AutoCull=%b, AdultsOnly=%b\n",
                    bot.getRanchAnimal(), bot.getRanchFood(), bot.getRanchMaxPop(), bot.getRanchMinPop(),
                    bot.isRanchAutoCull(), bot.isRanchAdultsOnly()));
        }

        // Equipped items
        sb.append("Equipped: MainHand=").append(formatStack(bot.getMainHandItem()))
                .append(", OffHand=").append(formatStack(bot.getOffhandItem()))
                .append(", Armor=[")
                .append(formatStack(bot.getItemBySlot(EquipmentSlot.HEAD))).append(", ")
                .append(formatStack(bot.getItemBySlot(EquipmentSlot.CHEST))).append(", ")
                .append(formatStack(bot.getItemBySlot(EquipmentSlot.LEGS))).append(", ")
                .append(formatStack(bot.getItemBySlot(EquipmentSlot.FEET))).append("]\n");

        // Inventory summary
        Map<String, Integer> itemCounts = new HashMap<>();
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            ItemStack s = bot.getInventory().getItem(i);
            if (!s.isEmpty()) {
                Identifier id = BuiltInRegistries.ITEM.getKey(s.getItem());
                itemCounts.merge(id.toString(), s.getCount(), Integer::sum);
            }
        }
        if (itemCounts.isEmpty()) {
            sb.append("Inventory: EMPTY\n");
        } else {
            sb.append("Inventory Contents: ");
            List<String> entries = new ArrayList<>();
            itemCounts.forEach((k, v) -> entries.add(k + " x" + v));
            entries.sort(String::compareTo);
            sb.append(String.join(", ", entries)).append("\n");
        }

        // Speaker info
        if (speaker != null) {
            BlockPos spPos = speaker.blockPosition();
            double dist = Math.sqrt(bot.distanceToSqr(speaker));
            sb.append("\n=== SPEAKER INFO ===\n");
            sb.append(String.format("Speaker Name: %s | Position: (%d, %d, %d) | Distance: %.1f blocks | Holding: %s\n",
                    speaker.getPlainTextName(), spPos.getX(), spPos.getY(), spPos.getZ(), dist, formatStack(speaker.getMainHandItem())));
        }

        // Other players & bots
        sb.append("\n=== PLAYERS & BOTS ON SERVER ===\n");
        for (ServerPlayer p : level.players()) {
            if (p == bot) continue;
            String tag = (p instanceof AiBotPlayer otherBot) ? "[AI Bot - " + otherBot.getCurrentTaskSummary() + "]" : "[Human Player]";
            sb.append(String.format("- %s %s at (%d, %d, %d) (%.1fm away)\n",
                    p.getPlainTextName(), tag, p.getBlockX(), p.getBlockY(), p.getBlockZ(), Math.sqrt(bot.distanceToSqr(p))));
        }

        // Nearby blocks scan
        int radius = BotConfig.SCAN_RADIUS.getAsInt();
        Map<String, Integer> blockCounts = new HashMap<>();
        Map<String, BlockPos> nearestBlockPos = new HashMap<>();
        Map<String, Double> nearestBlockDist = new HashMap<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -8; dy <= 10; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = botPos.offset(dx, dy, dz);
                    if (!level.isLoaded(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    Block block = state.getBlock();
                    if (block == Blocks.BEDROCK || block == Blocks.WATER || block == Blocks.GRASS_BLOCK || block == Blocks.SHORT_GRASS) {
                        continue;
                    }
                    Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                    String key = id.toString();
                    blockCounts.merge(key, 1, Integer::sum);
                    double d2 = botPos.distSqr(pos);
                    if (!nearestBlockDist.containsKey(key) || d2 < nearestBlockDist.get(key)) {
                        nearestBlockDist.put(key, d2);
                        nearestBlockPos.put(key, pos.immutable());
                    }
                }
            }
        }

        sb.append("\n=== NEARBY NOTABLE BLOCKS (within ").append(radius).append("m) ===\n");
        if (blockCounts.isEmpty()) {
            sb.append("None detected.\n");
        } else {
            List<String> blockSummaries = new ArrayList<>();
            blockCounts.forEach((key, count) -> {
                BlockPos np = nearestBlockPos.get(key);
                blockSummaries.add(String.format("%s (x%d, nearest at %d,%d,%d)", key, count, np.getX(), np.getY(), np.getZ()));
            });
            blockSummaries.sort(Comparator.comparingDouble(s -> {
                String k = s.substring(0, s.indexOf(" ("));
                // Prioritize ores, logs, tables, chests
                boolean important = k.contains("ore") || k.contains("log") || k.contains("wood")
                        || k.contains("chest") || k.contains("table") || k.contains("furnace") || k.contains("bed");
                return (important ? 0.0 : 1000.0) + nearestBlockDist.getOrDefault(k, 999.0);
            }));
            int limit = Math.min(25, blockSummaries.size());
            sb.append(String.join("; ", blockSummaries.subList(0, limit))).append("\n");
        }

        // Nearby entities
        AABB box = new AABB(botPos).inflate(24.0, 12.0, 24.0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != bot && !(e instanceof ServerPlayer) && e.isAlive());
        sb.append("\n=== NEARBY MOBS & ENTITIES (within 24m) ===\n");
        if (entities.isEmpty()) {
            sb.append("None nearby.\n");
        } else {
            entities.sort(Comparator.comparingDouble(bot::distanceToSqr));
            int limit = Math.min(15, entities.size());
            for (int i = 0; i < limit; i++) {
                LivingEntity e = entities.get(i);
                Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                String hostility = (e instanceof Monster) ? "[HOSTILE]" : "[PASSIVE/NEUTRAL]";
                sb.append(String.format("- %s %s at (%d, %d, %d) dist=%.1fm HP=%.1f\n",
                        typeId, hostility, e.getBlockX(), e.getBlockY(), e.getBlockZ(), Math.sqrt(bot.distanceToSqr(e)), e.getHealth()));
            }
        }

        // Saved locations / waypoints
        Map<String, BotPersistence.SavedLocation> locations = BotPersistence.getAllLocations();
        if (!locations.isEmpty()) {
            sb.append("\n=== SAVED LOCATIONS / WAYPOINTS ===\n");
            locations.values().forEach(loc ->
                    sb.append(String.format("- %s: (%d, %d, %d)\n", loc.name, loc.x, loc.y, loc.z)));
        }

        // Recent conversation memory
        List<String> mem = getMemory(bot.getBotName());
        if (!mem.isEmpty()) {
            sb.append("\n=== RECENT CONVERSATION & TASK MEMORY ===\n");
            for (String m : mem) {
                sb.append(m).append("\n");
            }
        }

        sb.append("\n=== NEW REQUEST FROM ").append(speaker != null ? speaker.getPlainTextName() : "Player").append(" ===\n");
        sb.append(userMessage).append("\n");

        return sb.toString();
    }

    private static String formatStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "none";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
    }

    public static CompletableFuture<BotAction.GeminiPlanResponse> askGeminiAsync(String telemetryPrompt, String speakerName, String rawRequest) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> apiKeys = BotConfig.getCandidateApiKeys(speakerName);
            if (apiKeys.isEmpty()) {
                return buildLocalFallbackPlan(speakerName, rawRequest);
            }

            String primaryModel = BotConfig.getModel();
            List<String> candidateModels = new ArrayList<>();
            candidateModels.add(primaryModel);
            for (String fallback : List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemma-4-26b-a4b-it", "gemini-3.6-flash", "gemini-3.5-flash")) {
                if (!candidateModels.contains(fallback)) {
                    candidateModels.add(fallback);
                }
            }

            int lastStatus = 0;
            for (String apiKey : apiKeys) {
                for (String model : candidateModels) {
                    boolean isGemma = model.startsWith("gemma");
                    JsonObject root = new JsonObject();

                    if (!isGemma) {
                        JsonObject sysInst = new JsonObject();
                        JsonArray sysParts = new JsonArray();
                        JsonObject sysPart = new JsonObject();
                        sysPart.addProperty("text", SYSTEM_INSTRUCTION);
                        sysParts.add(sysPart);
                        sysInst.add("parts", sysParts);
                        root.add("systemInstruction", sysInst);
                    }

                    JsonArray contents = new JsonArray();
                    JsonObject userContent = new JsonObject();
                    userContent.addProperty("role", "user");
                    JsonArray userParts = new JsonArray();
                    JsonObject userPart = new JsonObject();
                    userPart.addProperty("text", isGemma ? (SYSTEM_INSTRUCTION + "\n\n" + telemetryPrompt) : telemetryPrompt);
                    userParts.add(userPart);
                    userContent.add("parts", userParts);
                    contents.add(userContent);
                    root.add("contents", contents);

                    JsonObject genConfig = new JsonObject();
                    genConfig.addProperty("temperature", 0.25);
                    if (!isGemma) {
                        genConfig.addProperty("responseMimeType", "application/json");
                    }
                    root.add("generationConfig", genConfig);

                    String requestBody = GSON.toJson(root);
                    String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Content-Type", "application/json")
                                .header("x-goog-api-key", apiKey)
                                .timeout(Duration.ofSeconds(10))
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .build();

                        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                        lastStatus = response.statusCode();
                        if (lastStatus != 200) {
                            LOGGER.warn("Gemini model {} returned HTTP {}, trying next fallback...", model, lastStatus);
                            continue;
                        }

                        JsonObject respJson = JsonParser.parseString(response.body()).getAsJsonObject();
                        JsonArray candidates = respJson.getAsJsonArray("candidates");
                        if (candidates == null || candidates.isEmpty()) {
                            continue;
                        }

                        JsonObject contentObj = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                        JsonArray parts = contentObj.getAsJsonArray("parts");
                        String rawJsonText = parts.get(0).getAsJsonObject().get("text").getAsString().trim();

                        // Extract JSON object if wrapped in markdown or text
                        int firstBrace = rawJsonText.indexOf('{');
                        int lastBrace = rawJsonText.lastIndexOf('}');
                        if (firstBrace >= 0 && lastBrace > firstBrace) {
                            rawJsonText = rawJsonText.substring(firstBrace, lastBrace + 1);
                        }

                        BotAction.GeminiPlanResponse plan = GSON.fromJson(rawJsonText, BotAction.GeminiPlanResponse.class);
                        if (plan != null) {
                            if (plan.actions == null) {
                                plan.actions = List.of();
                            }
                            return plan;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Gemini model {} timed out or failed ({}), trying next fallback...", model, e.getMessage());
                    }
                }
            }

            LOGGER.warn("All cloud models busy (last HTTP {}), executing smart local tactical parser.", lastStatus);
            return buildLocalFallbackPlan(speakerName, rawRequest);
        }, EXECUTOR);
    }

    private static BotAction.GeminiPlanResponse buildLocalFallbackPlan(String speakerName, String rawRequest) {
        BotAction.GeminiPlanResponse res = new BotAction.GeminiPlanResponse();
        List<BotAction> actions = new ArrayList<>();
        String lower = rawRequest == null ? "" : rawRequest.toLowerCase().trim();

        int count = 1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d+)\\b").matcher(lower);
        if (m.find()) {
            try {
                count = Math.min(256, Math.max(1, Integer.parseInt(m.group(1))));
            } catch (NumberFormatException ignored) {
            }
        } else if (lower.contains("some") || lower.contains("bunch") || lower.contains("logs") || lower.contains("trees")) {
            count = 10;
        }

        if (lower.contains("stop") || lower.contains("stay") || lower.contains("wait") || lower.contains("cancel")) {
            actions.add(new BotAction("STOP", "", "", 1));
            res.reply = "Stopping right here and standing by!";
        } else if (lower.contains("follow") || lower.contains("come with")) {
            actions.add(new BotAction("FOLLOW", speakerName, "", 1));
            res.reply = "On my way! Following you, " + speakerName + ".";
        } else if (lower.contains("come here") || lower.contains("come to me") || lower.contains("go to me")) {
            actions.add(new BotAction("GOTO", speakerName, "", 1));
            res.reply = "Heading over to you now, " + speakerName + "!";
        } else if (lower.contains("go to ") || lower.contains("head to ") || lower.contains("walk to ") || lower.contains("return to ")) {
            String dest = speakerName;
            for (BotPersistence.SavedLocation loc : BotPersistence.getAllLocations().values()) {
                if (lower.contains(loc.name.toLowerCase())) {
                    BotAction gotoLoc = new BotAction("GOTO", loc.name, "", 1);
                    gotoLoc.x = loc.x;
                    gotoLoc.y = loc.y;
                    gotoLoc.z = loc.z;
                    actions.add(gotoLoc);
                    res.reply = "Heading to " + loc.name + " at (" + loc.x + ", " + loc.y + ", " + loc.z + ")!";
                    res.actions = actions;
                    return res;
                }
            }
            actions.add(new BotAction("GOTO", dest, "", 1));
            res.reply = "On my way!";
        } else if (lower.contains("guard") || lower.contains("protect") || lower.contains("defend")) {
            actions.add(new BotAction("GUARD", speakerName, "", 1));
            res.reply = "I've got your back, " + speakerName + "! Guarding you from any threats.";
        } else if (lower.contains("shelter") || lower.contains("house") || lower.contains("hut")) {
            actions.add(new BotAction("PLACE", "shelter", "", 1));
            res.reply = "Building a protective shelter right here!";
        } else if (lower.contains("breed") || lower.contains("husbandry") || lower.contains("ranch") || lower.contains("mate")) {
            String animal = "cow";
            if (lower.contains("sheep")) animal = "sheep";
            else if (lower.contains("pig")) animal = "pig";
            else if (lower.contains("chicken")) animal = "chicken";
            String food = "wheat";
            if (lower.contains("carrot")) food = "carrot";
            else if (lower.contains("potato")) food = "potato";
            else if (lower.contains("seed")) food = "seeds";
            actions.add(new BotAction("HUSBANDRY", animal, food, count > 1 ? count : 10));
            res.reply = "On it! Breeding " + animal + "s with " + food + ", managing population, and storing drops in chest.";
        } else if (lower.contains("farm") || lower.contains("harvest") || lower.contains("replant") || lower.contains("plant")) {
            String crop = "wheat";
            if (lower.contains("carrot")) crop = "carrots";
            else if (lower.contains("potato")) crop = "potatoes";
            else if (lower.contains("beetroot")) crop = "beetroots";
            actions.add(new BotAction("FARM", crop, "", count > 1 ? count : 20));
            res.reply = "On it! Harvesting mature " + crop + " only, replanting, tending farmland, and storing in chest.";
        } else if (lower.contains("till")) {
            actions.add(new BotAction("TILL", "farmland", "hoe", count > 1 ? count : 20));
            res.reply = "Tilling dirt into farmland with my hoe!";
        } else if (lower.contains("mine") || lower.contains("chop") || lower.contains("gather") || lower.contains("dig") || lower.contains("collect") || lower.contains("get")) {
            String target = "log";
            if (lower.contains("iron")) target = "iron_ore";
            else if (lower.contains("coal")) target = "coal_ore";
            else if (lower.contains("diamond")) target = "diamond_ore";
            else if (lower.contains("gold")) target = "gold_ore";
            else if (lower.contains("copper")) target = "copper_ore";
            else if (lower.contains("cobble") || lower.contains("stone")) target = "stone";
            else if (lower.contains("dirt")) target = "dirt";
            else if (lower.contains("sand")) target = "sand";
            actions.add(new BotAction("MINE", target, "", count));
            if (lower.contains("bring") || lower.contains("give")) {
                actions.add(new BotAction("GIVE", speakerName, target, count));
            }
            res.reply = "On it! Mining " + count + "x " + target + " for you.";
        } else if (lower.contains("craft") || lower.contains("make")) {
            String item = "wooden_pickaxe";
            if (lower.contains("stone pickaxe") || lower.contains("stone_pickaxe")) item = "stone_pickaxe";
            else if (lower.contains("iron pickaxe") || lower.contains("iron_pickaxe")) item = "iron_pickaxe";
            else if (lower.contains("stone sword") || lower.contains("stone_sword")) item = "stone_sword";
            else if (lower.contains("iron sword") || lower.contains("iron_sword")) item = "iron_sword";
            else if (lower.contains("torch")) item = "torch";
            else if (lower.contains("plank")) item = "oak_planks";
            else if (lower.contains("stick")) item = "stick";
            else if (lower.contains("chest")) item = "chest";
            else if (lower.contains("furnace")) item = "furnace";
            else if (lower.contains("table")) item = "crafting_table";
            actions.add(new BotAction("CRAFT", item, item, count));
            if (lower.contains("bring") || lower.contains("give")) {
                actions.add(new BotAction("GIVE", speakerName, item, count));
            }
            res.reply = "Crafting " + count + "x " + item + "!";
        } else if (lower.contains("give") || lower.contains("drop") || lower.contains("toss")) {
            String item = "all";
            if (lower.contains("log") || lower.contains("wood")) item = "log";
            else if (lower.contains("cobble") || lower.contains("stone")) item = "cobblestone";
            else if (lower.contains("iron")) item = "iron";
            else if (lower.contains("coal")) item = "coal";
            actions.add(new BotAction("GIVE", speakerName, item, count == 1 ? 64 : count));
            res.reply = "Bringing you my " + item + "!";
        } else if (lower.contains("attack") || lower.contains("kill") || lower.contains("fight") || lower.contains("hunt")) {
            String mob = "hostile";
            if (lower.contains("zombie")) mob = "zombie";
            else if (lower.contains("skeleton")) mob = "skeleton";
            else if (lower.contains("creeper")) mob = "creeper";
            else if (lower.contains("spider")) mob = "spider";
            else if (lower.contains("cow")) mob = "cow";
            else if (lower.contains("pig")) mob = "pig";
            else if (lower.contains("sheep")) mob = "sheep";
            else if (lower.contains("chicken")) mob = "chicken";
            actions.add(new BotAction("ATTACK", mob, "", Math.max(3, count)));
            res.reply = "Hunting nearby " + mob + "!";
        } else if (lower.contains("chest") || lower.contains("store") || lower.contains("deposit")) {
            actions.add(new BotAction("CHEST_STORE", "all", "", 1));
            res.reply = "Storing my gathered items into the nearest chest!";
        } else {
            res.reply = "I'm ready for orders! Tell me to follow you, guard you, mine logs/stone/ores, craft gear, attack mobs, or build a shelter.";
        }
        res.actions = actions;
        return res;
    }
}
