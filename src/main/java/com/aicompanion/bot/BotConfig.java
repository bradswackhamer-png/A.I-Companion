package com.aicompanion.bot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.neoforge.common.ModConfigSpec;

public class BotConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> GEMINI_API_KEY = BUILDER
            .comment("Default Server Google Gemini API Key (get a free key at https://aistudio.google.com/apikey).")
            .define("geminiApiKey", "AQ.Ab8RN6KlXf2LI67Eq3EJ9byX6D5RvBcrG5retBol3aHWvmlFLw");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> PLAYER_KEY_ENTRIES = BUILDER
            .comment("Per-player Gemini API keys in format 'PlayerName=API_KEY'. Players can set theirs in-game with /bot apikey <key>.")
            .defineListAllowEmpty("playerApiKeys", List.of(), () -> "", obj -> obj instanceof String);

    public static final ModConfigSpec.ConfigValue<String> GEMINI_MODEL = BUILDER
            .comment("Default Gemini model for the server (e.g., gemini-3.5-flash-lite, gemini-3.1-flash-lite, gemma-4-26b-a4b-it)")
            .define("geminiModel", "gemini-3.5-flash-lite");

    public static final ModConfigSpec.IntValue SCAN_RADIUS = BUILDER
            .comment("Radius in blocks around the bot to scan for blocks, ores, chests, and mobs")
            .defineInRange("scanRadius", 16, 6, 32);

    public static final ModConfigSpec.BooleanValue AUTO_DEFEND = BUILDER
            .comment("Whether bots automatically fight back when attacked or when nearby players are attacked by monsters")
            .define("autoDefend", true);

    public static final ModConfigSpec.BooleanValue AUTO_EAT = BUILDER
            .comment("Whether bots automatically eat food from their inventory when hungry or low on health")
            .define("autoEat", true);

    public static final ModConfigSpec.BooleanValue LISTEN_ALL_WHEN_SINGLE_BOT = BUILDER
            .comment("If only 1 bot is active, respond to any chat message even if the bot's name isn't explicitly mentioned")
            .define("listenAllWhenSingleBot", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile String runtimeApiKeyOverride = null;
    private static volatile String runtimeModelOverride = null;
    private static final Map<String, String> PLAYER_KEYS = new ConcurrentHashMap<>();

    private static void syncLoadedPlayerKeys() {
        try {
            List<? extends String> list = PLAYER_KEY_ENTRIES.get();
            if (list != null) {
                for (String entry : list) {
                    int eq = entry.indexOf('=');
                    if (eq > 0 && eq < entry.length() - 1) {
                        String pName = entry.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                        String pKey = entry.substring(eq + 1).trim();
                        if (!pKey.isEmpty()) {
                            PLAYER_KEYS.putIfAbsent(pName, pKey);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static String getApiKey() {
        if (runtimeApiKeyOverride != null && !runtimeApiKeyOverride.isBlank()) {
            return runtimeApiKeyOverride.trim();
        }
        try {
            String cfgKey = GEMINI_API_KEY.get();
            if (cfgKey != null && !cfgKey.isBlank()) {
                return cfgKey.trim();
            }
        } catch (Exception ignored) {
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        return envKey != null ? envKey.trim() : "";
    }

    public static List<String> getCandidateApiKeys(String speakerName) {
        syncLoadedPlayerKeys();
        Set<String> keys = new LinkedHashSet<>();
        if (speakerName != null && !speakerName.isBlank()) {
            String personal = PLAYER_KEYS.get(speakerName.trim().toLowerCase(Locale.ROOT));
            if (personal != null && !personal.isBlank()) {
                keys.add(personal);
            }
        }
        String serverDefault = getApiKey();
        if (!serverDefault.isEmpty()) {
            keys.add(serverDefault);
        }
        for (String pooledKey : PLAYER_KEYS.values()) {
            if (pooledKey != null && !pooledKey.isBlank()) {
                keys.add(pooledKey);
            }
        }
        return new ArrayList<>(keys);
    }

    public static void setPlayerApiKey(String playerName, String key) {
        if (playerName == null || playerName.isBlank() || key == null || key.isBlank()) return;
        syncLoadedPlayerKeys();
        PLAYER_KEYS.put(playerName.trim().toLowerCase(Locale.ROOT), key.trim());
        savePlayerKeysToConfig();
    }

    private static void savePlayerKeysToConfig() {
        try {
            List<String> serialized = new ArrayList<>();
            PLAYER_KEYS.forEach((k, v) -> serialized.add(k + "=" + v));
            PLAYER_KEY_ENTRIES.set(serialized);
            SPEC.save();
        } catch (Exception ignored) {
        }
    }

    public static void setApiKey(String key) {
        runtimeApiKeyOverride = key != null ? key.trim() : "";
        try {
            GEMINI_API_KEY.set(runtimeApiKeyOverride);
            SPEC.save();
        } catch (Exception ignored) {
        }
    }

    public static String getModel() {
        if (runtimeModelOverride != null && !runtimeModelOverride.isBlank()) {
            return runtimeModelOverride.trim();
        }
        try {
            String cfgModel = GEMINI_MODEL.get();
            if (cfgModel != null && !cfgModel.isBlank()) {
                return cfgModel.trim();
            }
        } catch (Exception ignored) {
        }
        return "gemini-3.5-flash-lite";
    }

    public static void setModel(String model) {
        runtimeModelOverride = model != null ? model.trim() : "gemini-3.5-flash-lite";
        try {
            GEMINI_MODEL.set(runtimeModelOverride);
            SPEC.save();
        } catch (Exception ignored) {
        }
    }
}
