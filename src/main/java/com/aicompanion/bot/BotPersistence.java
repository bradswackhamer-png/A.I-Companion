package com.aicompanion.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public class BotPersistence {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Persistent named locations (e.g., "town", "base", "mine") shared across bots
    private static final Map<String, SavedLocation> SAVED_LOCATIONS = new LinkedHashMap<>();

    public static class SavedLocation {
        public String name = "";
        public int x;
        public int y;
        public int z;
        public String dimension = "minecraft:overworld";

        public SavedLocation() {}

        public SavedLocation(String name, int x, int y, int z, String dimension) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }
    }

    public static class SavedItemStack {
        public String itemId = "";
        public int count = 1;
        public int slot = -1;
        public String equipSlot = "";

        public SavedItemStack() {}

        public SavedItemStack(String itemId, int count, int slot, String equipSlot) {
            this.itemId = itemId;
            this.count = count;
            this.slot = slot;
            this.equipSlot = equipSlot;
        }
    }

    public static class SavedBotData {
        public String name = "";
        public String nickname = "";
        public List<String> aliases = new ArrayList<>();
        public String role = "All-Around Survival Companion";
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public float health = 20.0f;
        public int foodLevel = 20;
        public Integer anchorX = null;
        public Integer anchorY = null;
        public Integer anchorZ = null;
        public double anchorRadius = 25.0;

        // Farmer configuration
        public Integer farmOriginX = null;
        public Integer farmOriginY = null;
        public Integer farmOriginZ = null;
        public int farmRadiusX = 10;
        public int farmRadiusZ = 10;
        public List<String> farmAllowedCrops = new ArrayList<>();
        public boolean farmAllowTill = true;

        // Rancher / Husbandry configuration
        public Integer ranchOriginX = null;
        public Integer ranchOriginY = null;
        public Integer ranchOriginZ = null;
        public double ranchRadius = 20.0;
        public String ranchAnimal = "cow";
        public String ranchFood = "wheat";
        public int ranchMaxPop = 0;
        public int ranchMinPop = 2;
        public boolean ranchAutoBreed = true;
        public boolean ranchAutoCull = false;
        public boolean ranchAdultsOnly = true;

        public List<SavedItemStack> inventory = new ArrayList<>();
        public List<String> memory = new ArrayList<>();
        public List<BotAction> activeActions = new ArrayList<>();
    }

    public static class PersistentStore {
        public List<SavedBotData> bots = new ArrayList<>();
        public Map<String, SavedLocation> locations = new LinkedHashMap<>();
    }

    public static synchronized void saveLocation(String name, BlockPos pos, String dimension) {
        if (name == null || name.isBlank()) return;
        String key = name.trim().toLowerCase(Locale.ROOT);
        SAVED_LOCATIONS.put(key, new SavedLocation(name.trim(), pos.getX(), pos.getY(), pos.getZ(), dimension));
    }

    public static synchronized SavedLocation getLocation(String name) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (SAVED_LOCATIONS.containsKey(key)) {
            return SAVED_LOCATIONS.get(key);
        }
        for (Map.Entry<String, SavedLocation> entry : SAVED_LOCATIONS.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static synchronized Map<String, SavedLocation> getAllLocations() {
        return new LinkedHashMap<>(SAVED_LOCATIONS);
    }

    private static File getSaveFile(MinecraftServer server) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        return worldDir.resolve("aicompanion_memory.json").toFile();
    }

    public static synchronized void saveAll(MinecraftServer server) {
        try {
            PersistentStore store = new PersistentStore();
            store.locations.putAll(SAVED_LOCATIONS);

            for (AiBotPlayer bot : BotManager.getActiveBots()) {
                SavedBotData data = new SavedBotData();
                data.name = bot.getBotName();
                data.nickname = bot.getActiveNickname() != null ? bot.getActiveNickname() : "";
                data.aliases = bot.getAliases();
                data.role = bot.getBotRole();
                data.x = bot.getX();
                data.y = bot.getY();
                data.z = bot.getZ();
                data.yaw = bot.getYRot();
                data.pitch = bot.getXRot();
                data.health = bot.getHealth();
                data.foodLevel = bot.getFoodData().getFoodLevel();
                if (bot.getGuardAnchorPos() != null) {
                    data.anchorX = bot.getGuardAnchorPos().getX();
                    data.anchorY = bot.getGuardAnchorPos().getY();
                    data.anchorZ = bot.getGuardAnchorPos().getZ();
                    data.anchorRadius = bot.getGuardAnchorRadius();
                }
                if (bot.getFarmOriginPos() != null) {
                    data.farmOriginX = bot.getFarmOriginPos().getX();
                    data.farmOriginY = bot.getFarmOriginPos().getY();
                    data.farmOriginZ = bot.getFarmOriginPos().getZ();
                }
                data.farmRadiusX = bot.getFarmRadiusX();
                data.farmRadiusZ = bot.getFarmRadiusZ();
                data.farmAllowedCrops = bot.getFarmAllowedCrops();
                data.farmAllowTill = bot.isFarmAllowTill();

                if (bot.getRanchOriginPos() != null) {
                    data.ranchOriginX = bot.getRanchOriginPos().getX();
                    data.ranchOriginY = bot.getRanchOriginPos().getY();
                    data.ranchOriginZ = bot.getRanchOriginPos().getZ();
                }
                data.ranchRadius = bot.getRanchRadius();
                data.ranchAnimal = bot.getRanchAnimal();
                data.ranchFood = bot.getRanchFood();
                data.ranchMaxPop = bot.getRanchMaxPop();
                data.ranchMinPop = bot.getRanchMinPop();
                data.ranchAutoBreed = bot.isRanchAutoBreed();
                data.ranchAutoCull = bot.isRanchAutoCull();
                data.ranchAdultsOnly = bot.isRanchAdultsOnly();
                data.memory = GeminiService.getMemory(bot.getBotName());
                data.activeActions = bot.getRemainingActionsSnapshot();

                for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
                    ItemStack s = bot.getInventory().getItem(i);
                    if (!s.isEmpty()) {
                        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                        data.inventory.add(new SavedItemStack(id, s.getCount(), i, ""));
                    }
                }
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND}) {
                    ItemStack s = bot.getItemBySlot(slot);
                    if (!s.isEmpty()) {
                        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                        data.inventory.add(new SavedItemStack(id, s.getCount(), -1, slot.getName()));
                    }
                }
                store.bots.add(data);
            }

            File file = getSaveFile(server);
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(store, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save AI bot persistent memory", e);
        }
    }

    public static synchronized void loadAll(MinecraftServer server) {
        File file = getSaveFile(server);
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            PersistentStore store = GSON.fromJson(reader, PersistentStore.class);
            if (store == null) return;

            SAVED_LOCATIONS.clear();
            if (store.locations != null) {
                SAVED_LOCATIONS.putAll(store.locations);
            }

            if (store.bots != null) {
                ServerLevel overworld = server.overworld();
                for (SavedBotData data : store.bots) {
                    if (data.name == null || data.name.isBlank()) continue;
                    AiBotPlayer bot = BotManager.spawnBotFromSave(overworld, data);
                    bot.setAliases(data.aliases, data.nickname);
                    if (data.anchorX != null && data.anchorY != null && data.anchorZ != null) {
                        bot.setGuardAnchor(new BlockPos(data.anchorX, data.anchorY, data.anchorZ), data.anchorRadius);
                    }
                    if (data.farmOriginX != null && data.farmOriginY != null && data.farmOriginZ != null) {
                        bot.setFarmArea(new BlockPos(data.farmOriginX, data.farmOriginY, data.farmOriginZ),
                                data.farmRadiusX, data.farmRadiusZ, data.farmAllowedCrops, data.farmAllowTill);
                    }
                    if (data.ranchOriginX != null && data.ranchOriginY != null && data.ranchOriginZ != null) {
                        bot.setRanchConfig(new BlockPos(data.ranchOriginX, data.ranchOriginY, data.ranchOriginZ),
                                data.ranchRadius, data.ranchAnimal, data.ranchFood,
                                data.ranchMaxPop, data.ranchMinPop, data.ranchAutoBreed,
                                data.ranchAutoCull, data.ranchAdultsOnly);
                    }
                    if (data.memory != null) {
                        GeminiService.setMemory(bot.getBotName(), data.memory);
                        for (String memLine : data.memory) {
                            if (AiBotPlayer.containsWholeWord(memLine, bot.getBotName())) {
                                bot.extractAndLearnNicknameFromPrompt(memLine);
                            }
                        }
                    }
                    if (data.activeActions != null && !data.activeActions.isEmpty()) {
                        bot.setActionPlan(data.activeActions);
                    }
                }
                LOGGER.info("Restored {} AI Companion bot(s) and {} saved location(s) from persistent memory.",
                        store.bots.size(), SAVED_LOCATIONS.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load AI bot persistent memory", e);
        }
    }

    public static ItemStack resolveStack(String itemId, int count) {
        try {
            Identifier id = Identifier.parse(itemId);
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (item != Items.AIR) {
                    return new ItemStack(item, Math.max(1, count));
                }
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }
}
