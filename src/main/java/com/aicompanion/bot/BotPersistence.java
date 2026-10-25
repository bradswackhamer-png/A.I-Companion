package com.aicompanion.bot;

import com.aicompanion.bot.area.SavedArea;
import com.aicompanion.bot.memory.BotMemoryManager;
import com.aicompanion.bot.memory.LearnedRecipe;
import com.aicompanion.bot.village.CustomBlueprint;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public class BotPersistence {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Persistent named locations (e.g., "town", "base", "mine") shared across bots
    private static final Map<String, SavedLocation> SAVED_LOCATIONS = new LinkedHashMap<>();

    private static volatile boolean hasLoadedFromDisk = false;
    private static volatile boolean hasExplicitlyClearedAllBots = false;

    public static void markExplicitlyCleared() {
        hasExplicitlyClearedAllBots = true;
    }

    public static void markBotCreated() {
        hasExplicitlyClearedAllBots = false;
    }

    public static boolean hasLoadedFromDisk() {
        return hasLoadedFromDisk;
    }

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
        public String dimension = "minecraft:overworld";
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
        public boolean autoCompactWheatToHay = true;
        public int maxSeedsToKeepOnHand = 32;
        public int maxSeedsInChest = 64;
        public boolean autoCompostExcessSeeds = true;

        // Lumberjack configuration
        public Integer lumberOriginX = null;
        public Integer lumberOriginY = null;
        public Integer lumberOriginZ = null;
        public int lumberRadius = 32;
        public boolean lumberjackAreaActive = false;
        public boolean lumberStoreLogs = true;

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

        // Conductor / Train & Tractor configuration
        public String conductorDest = "";
        public String conductorReturn = "";
        public String conductorFreight = "";
        public int conductorCount = 0;
        public boolean conductorReturning = false;
        public boolean tractorMode = false;

        public String assignedArea = "";
        public String skinName = "";

        public List<SavedItemStack> inventory = new ArrayList<>();
        public List<String> memory = new ArrayList<>();
        public List<String> learnedMemories = new ArrayList<>();
        public List<LearnedRecipe> learnedRecipes = new ArrayList<>();
        public List<BotAction> activeActions = new ArrayList<>();
    }

    public static class PersistentStore {
        public List<SavedBotData> bots = new ArrayList<>();
        public Map<String, SavedLocation> locations = new LinkedHashMap<>();
        public Map<String, SavedArea> areas = new LinkedHashMap<>();
        public Map<String, CustomBlueprint> blueprints = new LinkedHashMap<>();
        public List<String> globalMemories = new ArrayList<>();
        public List<LearnedRecipe> globalRecipes = new ArrayList<>();
    }

    // Persistent custom architectural blueprints captured from world
    private static final Map<String, CustomBlueprint> SAVED_BLUEPRINTS = new LinkedHashMap<>();

    public static synchronized void saveBlueprint(CustomBlueprint blueprint) {
        if (blueprint == null || blueprint.name == null || blueprint.name.isBlank()) return;
        String key = blueprint.name.trim().toLowerCase(Locale.ROOT);
        SAVED_BLUEPRINTS.put(key, blueprint);
    }

    public static synchronized CustomBlueprint getBlueprint(String name) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (SAVED_BLUEPRINTS.containsKey(key)) {
            return SAVED_BLUEPRINTS.get(key);
        }
        for (Map.Entry<String, CustomBlueprint> entry : SAVED_BLUEPRINTS.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static synchronized Map<String, CustomBlueprint> getAllBlueprints() {
        return new LinkedHashMap<>(SAVED_BLUEPRINTS);
    }

    public static synchronized boolean removeBlueprint(String name) {
        if (name == null || name.isBlank()) return false;
        String key = name.trim().toLowerCase(Locale.ROOT);
        return SAVED_BLUEPRINTS.remove(key) != null;
    }

    // Persistent global areas (e.g. "Compound", "Base", "NorthFarm")
    private static final Map<String, SavedArea> SAVED_AREAS = new LinkedHashMap<>();

    public static synchronized void saveArea(SavedArea area) {
        if (area == null || area.name == null || area.name.isBlank()) return;
        String key = area.name.trim().toLowerCase(Locale.ROOT);
        SAVED_AREAS.put(key, area);
    }

    public static synchronized SavedArea getArea(String name) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (SAVED_AREAS.containsKey(key)) {
            return SAVED_AREAS.get(key);
        }
        for (Map.Entry<String, SavedArea> entry : SAVED_AREAS.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static synchronized Map<String, SavedArea> getAllAreas() {
        return new LinkedHashMap<>(SAVED_AREAS);
    }

    public static synchronized boolean removeArea(String name) {
        if (name == null || name.isBlank()) return false;
        String key = name.trim().toLowerCase(Locale.ROOT);
        return SAVED_AREAS.remove(key) != null;
    }

    public static synchronized List<SavedArea> getAreasContaining(double x, double y, double z, String dimension) {
        List<SavedArea> list = new ArrayList<>();
        for (SavedArea area : SAVED_AREAS.values()) {
            if (dimension == null || area.dimension.equalsIgnoreCase(dimension)) {
                if (area.contains(x, y, z)) {
                    list.add(area);
                }
            }
        }
        return list;
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
        if (!hasLoadedFromDisk) {
            LOGGER.warn("Skipping BotPersistence.saveAll: world bots have not finished loading from disk yet.");
            return;
        }

        if (BotManager.getActiveBots().isEmpty() && !hasExplicitlyClearedAllBots) {
            LOGGER.warn("Skipping BotPersistence.saveAll: active bot list is empty and bots were not explicitly removed by user command.");
            return;
        }

        try {
            PersistentStore store = new PersistentStore();
            store.locations.putAll(SAVED_LOCATIONS);
            store.areas.putAll(SAVED_AREAS);
            store.blueprints.putAll(SAVED_BLUEPRINTS);

            for (AiBotPlayer bot : BotManager.getActiveBots()) {
                SavedBotData data = new SavedBotData();
                data.name = bot.getBotName();
                data.nickname = bot.getActiveNickname() != null ? bot.getActiveNickname() : "";
                data.aliases = bot.getAliases();
                data.role = bot.getBotRole();
                data.dimension = bot.level().dimension().identifier().toString();
                data.assignedArea = bot.getAssignedAreaName() != null ? bot.getAssignedAreaName() : "";
                data.skinName = bot.getCustomSkinName();
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
                data.autoCompactWheatToHay = bot.isAutoCompactWheatToHay();
                data.maxSeedsToKeepOnHand = bot.getMaxSeedsToKeepOnHand();
                data.maxSeedsInChest = bot.getMaxSeedsInChest();
                data.autoCompostExcessSeeds = bot.isAutoCompostExcessSeeds();

                if (bot.getLumberOriginPos() != null) {
                    data.lumberOriginX = bot.getLumberOriginPos().getX();
                    data.lumberOriginY = bot.getLumberOriginPos().getY();
                    data.lumberOriginZ = bot.getLumberOriginPos().getZ();
                }
                data.lumberRadius = bot.getLumberRadius();
                data.lumberjackAreaActive = bot.isLumberjackAreaActive();
                data.lumberStoreLogs = bot.isLumberStoreLogs();

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
                data.conductorDest = bot.getConductorDestinationStation();
                data.conductorReturn = bot.getConductorReturnStation();
                data.conductorFreight = bot.getConductorFreightItem();
                data.conductorCount = bot.getConductorFreightCount();
                data.conductorReturning = bot.isConductorReturning();
                data.tractorMode = bot.isTractorMode();
                data.memory = GeminiService.getMemory(bot.getBotName());
                data.learnedMemories = bot.getLearnedMemories();
                data.learnedRecipes = bot.getLearnedRecipes();
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

            store.globalMemories = BotMemoryManager.getGlobalMemories();
            store.globalRecipes = BotMemoryManager.getGlobalRecipes();

            File file = getSaveFile(server);
            File bakFile = new File(file.getParentFile(), "aicompanion_memory.json.bak");
            File tmpFile = new File(file.getParentFile(), "aicompanion_memory.json.tmp");

            try (FileWriter writer = new FileWriter(tmpFile, StandardCharsets.UTF_8)) {
                GSON.toJson(store, writer);
            }

            if (file.exists() && file.length() > 0) {
                try {
                    java.nio.file.Files.copy(file.toPath(), bakFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {}
            }

            try {
                java.nio.file.Files.move(tmpFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception e) {
                java.nio.file.Files.move(tmpFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save AI bot persistent memory", e);
        }
    }

    public static synchronized void loadAll(MinecraftServer server) {
        File file = getSaveFile(server);
        File bakFile = new File(file.getParentFile(), "aicompanion_memory.json.bak");

        File targetToRead = file;
        if (!targetToRead.exists() || targetToRead.length() == 0) {
            if (bakFile.exists() && bakFile.length() > 50) {
                LOGGER.info("Primary bot memory file missing or empty, falling back to backup file: {}", bakFile.getName());
                targetToRead = bakFile;
            } else {
                hasLoadedFromDisk = true;
                return;
            }
        }

        try (FileReader reader = new FileReader(targetToRead, StandardCharsets.UTF_8)) {
            PersistentStore store = GSON.fromJson(reader, PersistentStore.class);
            if (store == null && targetToRead != bakFile && bakFile.exists() && bakFile.length() > 50) {
                try (FileReader bakReader = new FileReader(bakFile, StandardCharsets.UTF_8)) {
                    store = GSON.fromJson(bakReader, PersistentStore.class);
                }
            }
            if (store == null) {
                hasLoadedFromDisk = true;
                return;
            }

            // If primary file has 0 bots but backup has bots, restore from backup
            if ((store.bots == null || store.bots.isEmpty()) && targetToRead != bakFile && bakFile.exists() && bakFile.length() > 50) {
                try (FileReader bakReader = new FileReader(bakFile, StandardCharsets.UTF_8)) {
                    PersistentStore bakStore = GSON.fromJson(bakReader, PersistentStore.class);
                    if (bakStore != null && bakStore.bots != null && !bakStore.bots.isEmpty()) {
                        LOGGER.warn("Primary memory file had 0 bots but backup has {} bots. Restoring from backup!", bakStore.bots.size());
                        store = bakStore;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Could not read backup file", e);
                }
            }

            hasLoadedFromDisk = true;

            SAVED_LOCATIONS.clear();
            if (store.locations != null) {
                SAVED_LOCATIONS.putAll(store.locations);
            }

            SAVED_AREAS.clear();
            if (store.areas != null) {
                SAVED_AREAS.putAll(store.areas);
                for (SavedArea area : SAVED_AREAS.values()) {
                    area.computeAABB();
                }
            }

            SAVED_BLUEPRINTS.clear();
            if (store.blueprints != null) {
                SAVED_BLUEPRINTS.putAll(store.blueprints);
            }

            if (store.globalMemories != null) {
                BotMemoryManager.setGlobalMemories(store.globalMemories);
            }
            if (store.globalRecipes != null) {
                BotMemoryManager.setGlobalRecipes(store.globalRecipes);
            }

            if (store.bots != null) {
                for (SavedBotData data : store.bots) {
                    if (data.name == null || data.name.isBlank()) continue;
                    ServerLevel targetLevel = server.overworld();
                    if (data.dimension != null && !data.dimension.isBlank()) {
                        try {
                            Identifier dimId = Identifier.parse(data.dimension);
                            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);
                            ServerLevel lvl = server.getLevel(dimKey);
                            if (lvl != null) {
                                targetLevel = lvl;
                            }
                        } catch (Exception ignored) {}
                    }
                    targetLevel.getChunk(BlockPos.containing(data.x, data.y, data.z));
                    AiBotPlayer bot = BotManager.spawnBotFromSave(targetLevel, data);
                    bot.setAliases(data.aliases, data.nickname);
                    if (data.assignedArea != null && !data.assignedArea.isBlank()) {
                        bot.setAssignedArea(data.assignedArea);
                    }
                    if (data.anchorX != null && data.anchorY != null && data.anchorZ != null) {
                        bot.setGuardAnchor(new BlockPos(data.anchorX, data.anchorY, data.anchorZ), data.anchorRadius);
                    }
                    if (data.farmOriginX != null && data.farmOriginY != null && data.farmOriginZ != null) {
                        bot.setFarmArea(new BlockPos(data.farmOriginX, data.farmOriginY, data.farmOriginZ),
                                data.farmRadiusX, data.farmRadiusZ, data.farmAllowedCrops, data.farmAllowTill);
                    }
                    bot.setAutoCompactWheatToHay(data.autoCompactWheatToHay);
                    bot.setMaxSeedsToKeepOnHand(data.maxSeedsToKeepOnHand > 0 ? data.maxSeedsToKeepOnHand : 32);
                    bot.setMaxSeedsInChest(data.maxSeedsInChest > 0 ? data.maxSeedsInChest : 64);
                    bot.setAutoCompostExcessSeeds(data.autoCompostExcessSeeds);
                    if (data.lumberOriginX != null && data.lumberOriginY != null && data.lumberOriginZ != null) {
                        bot.setLumberjackConfig(new BlockPos(data.lumberOriginX, data.lumberOriginY, data.lumberOriginZ),
                                data.lumberRadius, data.lumberjackAreaActive, data.lumberStoreLogs);
                    }
                    if (data.ranchOriginX != null && data.ranchOriginY != null && data.ranchOriginZ != null) {
                        bot.setRanchConfig(new BlockPos(data.ranchOriginX, data.ranchOriginY, data.ranchOriginZ),
                                data.ranchRadius, data.ranchAnimal, data.ranchFood,
                                data.ranchMaxPop, data.ranchMinPop, data.ranchAutoBreed,
                                data.ranchAutoCull, data.ranchAdultsOnly);
                    }
                    bot.setConductorConfig(data.conductorDest, data.conductorReturn, data.conductorFreight, data.conductorCount, data.conductorReturning);
                    bot.setTractorMode(data.tractorMode);
                    if (data.memory != null) {
                        GeminiService.setMemory(bot.getBotName(), data.memory);
                        for (String memLine : data.memory) {
                            if (AiBotPlayer.containsWholeWord(memLine, bot.getBotName())) {
                                bot.extractAndLearnNicknameFromPrompt(memLine);
                            }
                        }
                    }
                    if (data.learnedMemories != null) {
                        bot.setLearnedMemories(data.learnedMemories);
                    }
                    if (data.learnedRecipes != null) {
                        bot.setLearnedRecipes(data.learnedRecipes);
                    }
                    if (data.activeActions != null && !data.activeActions.isEmpty()) {
                        bot.setActionPlan(data.activeActions);
                    }
                }
                LOGGER.info("Restored {} AI Companion bot(s), {} location(s), and {} global area(s) from persistent memory.",
                        store.bots.size(), SAVED_LOCATIONS.size(), SAVED_AREAS.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load AI bot persistent memory", e);
            hasLoadedFromDisk = true;
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
