package com.aicompanion.bot.village;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages village growth, housing inventory, and bed assignments.
 * Enables companions to autonomously construct houses, register beds,
 * and accommodate new recruits/apprentices as the colony expands.
 */
public class VillageManager {

    public static class VillageHouse {
        public String id;
        public String compoundArea;
        public BlockPos doorPos;
        public BlockPos bedPos;
        public @Nullable String ownerBotName;
        public long completedTime;

        public VillageHouse(String id, String compoundArea, BlockPos doorPos, BlockPos bedPos, @Nullable String ownerBotName) {
            this.id = id;
            this.compoundArea = compoundArea;
            this.doorPos = doorPos;
            this.bedPos = bedPos;
            this.ownerBotName = ownerBotName;
            this.completedTime = System.currentTimeMillis();
        }
    }

    private static final Map<String, VillageHouse> HOUSES = new ConcurrentHashMap<>();

    public static void registerHouse(String compoundArea, BlockPos doorPos, BlockPos bedPos, @Nullable String owner) {
        String id = "house_" + doorPos.getX() + "_" + doorPos.getY() + "_" + doorPos.getZ();
        VillageHouse house = new VillageHouse(id, compoundArea, doorPos, bedPos, owner);
        HOUSES.put(id, house);
    }

    public static List<VillageHouse> getHousesInCompound(String compoundArea) {
        List<VillageHouse> list = new ArrayList<>();
        for (VillageHouse h : HOUSES.values()) {
            if (h.compoundArea.equalsIgnoreCase(compoundArea)) {
                list.add(h);
            }
        }
        return list;
    }

    public static int getTotalBeds(String compoundArea) {
        return getHousesInCompound(compoundArea).size();
    }

    public static int getVacantBeds(String compoundArea) {
        int count = 0;
        for (VillageHouse h : getHousesInCompound(compoundArea)) {
            if (h.ownerBotName == null || h.ownerBotName.isBlank()) {
                count++;
            }
        }
        return count;
    }

    public static @Nullable BlockPos claimVacantBed(String compoundArea, String botName) {
        for (VillageHouse h : getHousesInCompound(compoundArea)) {
            if (h.ownerBotName == null || h.ownerBotName.isBlank()) {
                h.ownerBotName = botName;
                return h.bedPos;
            }
        }
        return null;
    }

    public static Map<String, VillageHouse> getAllHouses() {
        return Collections.unmodifiableMap(HOUSES);
    }

    public static void clear() {
        HOUSES.clear();
    }
}
