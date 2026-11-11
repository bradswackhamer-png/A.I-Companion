package com.aicompanion.bot.village;

import com.aicompanion.bot.AiBotPlayer;
import com.aicompanion.bot.BotConfig;
import com.aicompanion.bot.BotManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages village growth, housing inventory, bed assignments, and village diagnostics.
 * Enables companions to autonomously construct houses, register beds,
 * accommodate new recruits/apprentices as the colony expands, and generate village status reports.
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

    public static class VillageStatusReport {
        public BlockPos center;
        public int scanRadius;
        public @Nullable BlockPos bellPos;
        public double bellDistance;
        public int totalBeds;
        public int claimedBeds;
        public int vacantBeds;
        public int adultVillagers;
        public int babyVillagers;
        public int readyToBreedVillagers;
        public final List<CompanionVillagerDetail> companionVillagers = new ArrayList<>();
        public final List<VanillaVillagerDetail> vanillaVillagers = new ArrayList<>();
        public String breedingVerdict = "";

        public String formatChatSummary(String botName) {
            StringBuilder sb = new StringBuilder();
            sb.append("Village Report around my post (").append(scanRadius).append("m radius): ");
            if (bellPos != null) {
                sb.append(String.format("Town bell active at (%d, %d, %d) [%.0fm away]! ", bellPos.getX(), bellPos.getY(), bellPos.getZ(), bellDistance));
            } else {
                sb.append("No town bell detected nearby. ");
            }
            sb.append(String.format("Beds: %d total (%d claimed, %d vacant). ", totalBeds, claimedBeds, vacantBeds));
            sb.append(String.format("Villagers: %d companion(s), %d vanilla (%d ready to breed). ",
                    companionVillagers.size(), vanillaVillagers.size(), readyToBreedVillagers));
            sb.append(breedingVerdict);
            return sb.toString();
        }
    }

    public static class CompanionVillagerDetail {
        public String name;
        public String role;
        public String profession;
        public boolean isBaby;
        public int babyMinsRemaining;
        public float health;
        public float maxHealth;
        public int foodLevel;
        public int bread;
        public int wheat;
        public int carrot;
        public int potato;
        public int beetroot;
        public @Nullable BlockPos bedPos;
        public @Nullable BlockPos workstationPos;
        public String workstationName;
        public boolean isReadyToBreed;
    }

    public static class VanillaVillagerDetail {
        public String profession;
        public boolean isBaby;
        public BlockPos pos;
        public double distance;
        public int bread;
        public int carrot;
        public int potato;
        public int beetroot;
        public boolean isReadyToBreed;
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

    /**
     * Scans around the center position within {@code scanRadius} blocks to compile
     * a comprehensive village diagnostics report (bell detection, beds, villager food/hunger,
     * and breeding status).
     */
    public static VillageStatusReport generateReport(ServerLevel level, BlockPos center, int scanRadius) {
        VillageStatusReport report = new VillageStatusReport();
        report.center = center;
        report.scanRadius = scanRadius;

        PoiManager poi = level.getPoiManager();

        // 1. Detect town bell
        BlockPos bellPos = poi.find(holder -> holder.is(PoiTypes.MEETING), p -> true, center, scanRadius, PoiManager.Occupancy.ANY)
                .orElse(null);
        if (bellPos == null) {
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-scanRadius, -16, -scanRadius), center.offset(scanRadius, 16, scanRadius))) {
                if (level.getBlockState(p).is(Blocks.BELL)) {
                    bellPos = p.immutable();
                    break;
                }
            }
        }
        if (bellPos != null) {
            report.bellPos = bellPos;
            report.bellDistance = Math.sqrt(center.distSqr(bellPos));
        }

        // 2. Detect beds
        Set<BlockPos> uniqueBedHeads = new HashSet<>();
        poi.findAll(holder -> holder.is(PoiTypes.HOME), p -> true, center, scanRadius, PoiManager.Occupancy.ANY)
                .forEach(p -> uniqueBedHeads.add(AiBotPlayer.getBedHeadPos(level, p)));

        for (VillageHouse house : HOUSES.values()) {
            if (house.bedPos != null && center.distSqr(house.bedPos) <= (long) scanRadius * scanRadius) {
                uniqueBedHeads.add(AiBotPlayer.getBedHeadPos(level, house.bedPos));
            }
        }

        // 3. Scan Companion Villagers
        Set<BlockPos> claimedBedHeads = new HashSet<>();
        for (AiBotPlayer bot : BotManager.getActiveBots()) {
            if (bot.level() == level && center.distSqr(bot.blockPosition()) <= (long) scanRadius * scanRadius) {
                if (bot.getAssignedBedPos() != null) {
                    BlockPos bedHead = AiBotPlayer.getBedHeadPos(level, bot.getAssignedBedPos());
                    claimedBedHeads.add(bedHead);
                    uniqueBedHeads.add(bedHead);
                }

                CompanionVillagerDetail detail = new CompanionVillagerDetail();
                detail.name = bot.getPlainTextName();
                detail.role = bot.getBotRole();
                detail.profession = AiBotPlayer.capitalize(bot.getBotSubRole());
                detail.isBaby = bot.isBaby();
                detail.babyMinsRemaining = bot.isBaby() ? Math.max(1, (bot.getBabyAgeTicks() + 1199) / 1200) : 0;
                detail.health = bot.getHealth();
                detail.maxHealth = bot.getMaxHealth();
                detail.foodLevel = bot.getFoodData().getFoodLevel();

                detail.bread = bot.countMatching(s -> s.is(Items.BREAD));
                detail.wheat = bot.countMatching(s -> s.is(Items.WHEAT));
                detail.carrot = bot.countMatching(s -> s.is(Items.CARROT));
                detail.potato = bot.countMatching(s -> s.is(Items.POTATO));
                detail.beetroot = bot.countMatching(s -> s.is(Items.BEETROOT));

                detail.bedPos = bot.getAssignedBedPos();
                detail.workstationPos = bot.getAssignedWorkstationPos();
                if (detail.workstationPos != null) {
                    detail.workstationName = level.getBlockState(detail.workstationPos).getBlock().getName().getString();
                } else {
                    detail.workstationName = "None";
                }

                boolean hasBreedingFood = detail.bread >= 3 || detail.wheat >= 3
                        || detail.carrot >= 12 || detail.potato >= 12 || detail.beetroot >= 12;
                detail.isReadyToBreed = !detail.isBaby && detail.foodLevel >= 18 && hasBreedingFood;

                if (detail.isBaby) {
                    report.babyVillagers++;
                } else {
                    report.adultVillagers++;
                }
                if (detail.isReadyToBreed) {
                    report.readyToBreedVillagers++;
                }

                report.companionVillagers.add(detail);
            }
        }

        // 4. Scan Vanilla Villagers
        AABB searchBox = new AABB(center).inflate(scanRadius);
        List<Villager> vanillaList = level.getEntitiesOfClass(Villager.class, searchBox);
        for (Villager v : vanillaList) {
            VanillaVillagerDetail vDetail = new VanillaVillagerDetail();
            vDetail.profession = v.getVillagerData().profession().value().name().getString();
            vDetail.isBaby = v.isBaby();
            vDetail.pos = v.blockPosition();
            vDetail.distance = Math.sqrt(center.distSqr(vDetail.pos));

            for (int i = 0; i < v.getInventory().getContainerSize(); i++) {
                ItemStack stack = v.getInventory().getItem(i);
                if (stack.is(Items.BREAD)) vDetail.bread += stack.getCount();
                else if (stack.is(Items.CARROT)) vDetail.carrot += stack.getCount();
                else if (stack.is(Items.POTATO)) vDetail.potato += stack.getCount();
                else if (stack.is(Items.BEETROOT)) vDetail.beetroot += stack.getCount();
            }

            vDetail.isReadyToBreed = !vDetail.isBaby && (vDetail.bread >= 3 || vDetail.carrot >= 12 || vDetail.potato >= 12 || vDetail.beetroot >= 12);
            if (vDetail.isBaby) {
                report.babyVillagers++;
            } else {
                report.adultVillagers++;
            }
            if (vDetail.isReadyToBreed) {
                report.readyToBreedVillagers++;
            }
            report.vanillaVillagers.add(vDetail);
        }

        report.totalBeds = uniqueBedHeads.size();
        report.claimedBeds = Math.min(report.totalBeds, claimedBedHeads.size());
        report.vacantBeds = Math.max(0, report.totalBeds - report.claimedBeds);

        int totalVillagers = report.adultVillagers + report.babyVillagers;
        int bedsRequired = totalVillagers + 1;

        if (report.totalBeds < bedsRequired) {
            report.breedingVerdict = "INSUFFICIENT BEDS (Colony has " + report.totalBeds + " beds for " + totalVillagers + " villagers; need " + bedsRequired + " beds for next baby).";
        } else if (report.vacantBeds <= 0) {
            report.breedingVerdict = "NO VACANT BEDS (All beds are occupied. Place more beds to allow breeding).";
        } else if (report.readyToBreedVillagers < 2) {
            report.breedingVerdict = "NEED FOOD (" + report.readyToBreedVillagers + " of 2 required adults ready; feed villagers bread/carrots/potatoes).";
        } else {
            report.breedingVerdict = "READY TO BREED! (" + report.readyToBreedVillagers + " adults ready with food & " + report.vacantBeds + " vacant bed(s) available).";
        }

        return report;
    }

    public static List<Component> formatRichReport(VillageStatusReport report) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("==================== VILLAGE STATUS REPORT ====================")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        // Town Bell
        if (report.bellPos != null) {
            lines.add(Component.literal("Town Bell: ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(String.format("Found at (%d, %d, %d) [%.1fm away] (Active Village Center)",
                            report.bellPos.getX(), report.bellPos.getY(), report.bellPos.getZ(), report.bellDistance)).withStyle(ChatFormatting.GREEN)));
        } else {
            lines.add(Component.literal("Town Bell: ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("None detected within range (villagers gather at houses)").withStyle(ChatFormatting.RED)));
        }

        // Bed summary
        int totalVillagers = report.adultVillagers + report.babyVillagers;
        ChatFormatting bedColor = report.vacantBeds > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        lines.add(Component.literal(String.format("Beds: %d total (%d claimed, %d vacant) | Needed for breeding: %d",
                report.totalBeds, report.claimedBeds, report.vacantBeds, totalVillagers + 1)).withStyle(bedColor));

        // Villager roster
        lines.add(Component.literal(String.format("Villagers (%d total: %d Adults, %d Babies, %d Ready to Breed):",
                totalVillagers, report.adultVillagers, report.babyVillagers, report.readyToBreedVillagers)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        for (CompanionVillagerDetail c : report.companionVillagers) {
            StringBuilder desc = new StringBuilder();
            desc.append(" • ").append(c.name).append(" [").append(c.role);
            if (!c.profession.isBlank() && !"None".equalsIgnoreCase(c.profession)) {
                desc.append(" / ").append(c.profession);
            }
            desc.append("]: ");
            if (c.isBaby) {
                desc.append(String.format("Stage: Baby (~%dm left) | ", c.babyMinsRemaining));
            }
            desc.append(String.format("HP: %.0f/%.0f | Hunger: %d/20", c.health, c.maxHealth, c.foodLevel));
            desc.append(String.format(" | Food: %d bread, %d wheat, %d carrot, %d potato", c.bread, c.wheat, c.carrot, c.potato));
            if (c.bedPos != null) {
                desc.append(String.format(" | Bed: (%d, %d, %d)", c.bedPos.getX(), c.bedPos.getY(), c.bedPos.getZ()));
            } else {
                desc.append(" | Bed: None");
            }
            if (c.workstationPos != null) {
                desc.append(" | Work: ").append(c.workstationName);
            }
            if (c.isReadyToBreed) {
                desc.append(" [READY TO BREED]");
            }
            ChatFormatting cColor = c.isReadyToBreed ? ChatFormatting.GREEN : (c.isBaby ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
            lines.add(Component.literal(desc.toString()).withStyle(cColor));
        }

        for (VanillaVillagerDetail v : report.vanillaVillagers) {
            String vDesc = String.format(" • Vanilla Villager [%s]: Distance: %.1fm | Food: %d bread, %d carrot, %d potato%s",
                    v.profession, v.distance, v.bread, v.carrot, v.potato, v.isReadyToBreed ? " [READY TO BREED]" : "");
            lines.add(Component.literal(vDesc).withStyle(v.isReadyToBreed ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }

        // Breeding Verdict
        ChatFormatting vColor = report.breedingVerdict.startsWith("READY") ? ChatFormatting.GREEN : ChatFormatting.GOLD;
        lines.add(Component.literal("Breeding Verdict: " + report.breedingVerdict).withStyle(vColor, ChatFormatting.BOLD));

        return lines;
    }

    public static List<Component> formatBotDebug(AiBotPlayer bot) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("==================== " + bot.getChatTag() + " DIAGNOSTICS ====================")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        // 1. AI Engine & Learning
        boolean forceOffline = BotConfig.isForceOffline();
        String aiEngine = forceOffline ? "Offline Tactical Mode" : ("Cloud Gemini (" + BotConfig.getModel() + ")");
        int hiveIq = com.aicompanion.bot.learning.HiveMindBrain.getHiveIqScore();
        lines.add(Component.literal(String.format("AI Engine: %s | Hive IQ: %d | Absolute Mode: %b",
                aiEngine, hiveIq, bot.isAbsoluteCommandMode())).withStyle(ChatFormatting.YELLOW));

        // 2. Physical & Vitals
        lines.add(Component.literal(String.format("Vitals: HP: %.1f/%.1f | Hunger: %d/20 (Sat: %.1f) | Pos: (%d, %d, %d) | Facing: %s",
                bot.getHealth(), bot.getMaxHealth(), bot.getFoodData().getFoodLevel(), bot.getFoodData().getSaturationLevel(),
                bot.getBlockX(), bot.getBlockY(), bot.getBlockZ(), bot.getDirection().getName())).withStyle(ChatFormatting.GREEN));

        // 3. Role & Stage
        String stageStr = bot.isBaby() ? String.format("Baby (~%d min until adult)", Math.max(1, (bot.getBabyAgeTicks() + 1199) / 1200)) : "Adult";
        lines.add(Component.literal(String.format("Role: %s | Profession: %s | Stage: %s",
                bot.getBotRole(), AiBotPlayer.capitalize(bot.getBotSubRole()), stageStr)).withStyle(ChatFormatting.AQUA));

        // 4. Current Tasks & Movement
        lines.add(Component.literal("Task: " + bot.getCurrentTaskSummary()).withStyle(ChatFormatting.WHITE));
        if (bot.getCurrentAction() != null) {
            lines.add(Component.literal("Action Queue: " + bot.getCurrentAction().type + " -> Target: " + bot.getCurrentAction().getPrimaryTarget())
                    .withStyle(ChatFormatting.GRAY));
        }

        // 5. Navigation & Pathfinder
        var nav = bot.getVanillaNavigator();
        Vec3 lastMoveTarget = bot.getLastMoveTarget();
        String moveTargetStr = lastMoveTarget != null
                ? String.format("(%.1f, %.1f, %.1f) [%.1fm away]", lastMoveTarget.x, lastMoveTarget.y, lastMoveTarget.z, Math.sqrt(bot.distanceToSqr(lastMoveTarget)))
                : "None";
        BlockPos pathGoal = nav.getLastTargetBlock();
        String pathGoalStr = pathGoal != null ? String.format("(%d, %d, %d)", pathGoal.getX(), pathGoal.getY(), pathGoal.getZ()) : "None";

        lines.add(Component.literal(String.format("Navigation: Active: %b | Goal: %s | MoveTarget: %s",
                nav.isNavigating(), pathGoalStr, moveTargetStr)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal(String.format("Stuck Tracker: StuckTicks: %d | InsanityStuck: %d | Unreachable: %d | VanillaStuck: %d",
                bot.getStuckTicks(), bot.getInsanityStuckTicks(), nav.getUnreachableTicks(), nav.getConsecutiveVanillaStuckCount()))
                .withStyle(bot.getStuckTicks() > 0 ? ChatFormatting.RED : ChatFormatting.GRAY));

        // 6. Water & Environmental
        lines.add(Component.literal(String.format("Environment: InWater: %b | AllowWater: %b | NightShelter: %b | Sleeping: %b",
                bot.isInWater(), bot.isAllowWaterEntry(), bot.isNightShelterEnabled(), bot.isSleeping())).withStyle(ChatFormatting.YELLOW));

        // 7. Village, Bed & Bell
        BlockPos bed = bot.getAssignedBedPos();
        String bedStr = bed != null ? String.format("(%d, %d, %d)", bed.getX(), bed.getY(), bed.getZ()) : "None";
        BlockPos workstation = bot.getAssignedWorkstationPos();
        String wsStr = workstation != null
                ? bot.level().getBlockState(workstation).getBlock().getName().getString() + " at (" + workstation.getX() + ", " + workstation.getY() + ", " + workstation.getZ() + ")"
                : "None";
        BlockPos bell = bot.findNearestBlockType(Blocks.BELL, 48);
        String bellStr = bell != null
                ? String.format("Found at (%d, %d, %d) [%.1fm away]", bell.getX(), bell.getY(), bell.getZ(), Math.sqrt(bot.distanceToSqr(Vec3.atCenterOf(bell))))
                : "None in 48m";

        lines.add(Component.literal(String.format("Village State: Bed: %s | Workstation: %s | Bell: %s",
                bedStr, wsStr, bellStr)).withStyle(ChatFormatting.LIGHT_PURPLE));

        // 8. Food & Breeding Readiness
        int bread = bot.countMatching(s -> s.is(Items.BREAD));
        int wheat = bot.countMatching(s -> s.is(Items.WHEAT));
        int carrot = bot.countMatching(s -> s.is(Items.CARROT));
        int potato = bot.countMatching(s -> s.is(Items.POTATO));
        int beetroot = bot.countMatching(s -> s.is(Items.BEETROOT));
        boolean readyToBreed = !bot.isBaby() && bot.getFoodData().getFoodLevel() >= 18
                && (bread >= 3 || wheat >= 3 || carrot >= 12 || potato >= 12 || beetroot >= 12);

        lines.add(Component.literal(String.format("Breeding Food: Bread: %d, Wheat: %d, Carrot: %d, Potato: %d, Beetroot: %d | Ready: %b",
                bread, wheat, carrot, potato, beetroot, readyToBreed)).withStyle(readyToBreed ? ChatFormatting.GREEN : ChatFormatting.GOLD));

        return lines;
    }
}
