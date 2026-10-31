package com.aicompanion.bot;

import com.aicompanion.bot.compat.TractorCompat;
import com.aicompanion.bot.compat.TrainCompat;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import com.aicompanion.bot.memory.BotMemoryManager;
import com.aicompanion.bot.memory.LearnedRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import java.util.Collections;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.HashSet;
import org.jspecify.annotations.Nullable;
import com.aicompanion.bot.village.HouseBlueprint;
import com.aicompanion.bot.village.CustomBlueprint;
import com.aicompanion.bot.village.VillageManager;
import com.aicompanion.bot.village.ArchitecturalPalette;

public class AiBotPlayer extends ServerPlayer {
    private final String botName;
    private String botRole;
    private final List<String> aliases = new ArrayList<>();
    private @Nullable String activeNickname = null;
    private @Nullable BlockPos guardAnchorPos = null;
    private double guardAnchorRadius = 25.0;

    // Farm configuration
    private @Nullable BlockPos farmOriginPos = null;
    private int farmRadiusX = 10;
    private int farmRadiusZ = 10;
    private final List<String> farmAllowedCrops = new ArrayList<>();
    private boolean farmAllowTill = true;
    private boolean autoCompactWheatToHay = true;
    private int maxSeedsToKeepOnHand = 32;
    private int maxSeedsInChest = 64;
    private boolean autoCompostExcessSeeds = true;

    // Husbandry / Rancher configuration
    private @Nullable BlockPos ranchOriginPos = null;
    private double ranchRadius = 25.0;
    private String ranchAnimal = "cow";
    private String ranchFood = "wheat";
    private int ranchMaxPop = 0;
    private int ranchMinPop = 2;
    private boolean ranchAutoBreed = true;
    private boolean ranchAutoCull = false;
    private boolean ranchAdultsOnly = true;
    private long lastBreedMessageTime = 0;

    // Conductor / Train configuration
    private String conductorDestStation = "";
    private String conductorReturnStation = "";
    private String conductorFreightItem = "";
    private int conductorFreightCount = 0;
    private boolean conductorIsReturning = false;
    private int conductorState = 0;
    private long conductorLastWhistleTime = 0;
    private int conductorArrivedTicks = 0;
    private int conductorTileTarget = 0;
    private @Nullable Vec3 conductorMoveStartPos = null;
    private boolean conductorFollowingPlayer = false;
    private @Nullable ServerPlayer conductorTargetPlayer = null;
    private @Nullable ServerPlayer lastInteractingPlayer = null;
    private String botSubRole = "unemployed";
    private @Nullable BlockPos assignedWorkstationPos = null;
    private int villagerLevel = 1;
    private int villagerXp = 0;
    private final BotVillagerMerchant villagerMerchant = new BotVillagerMerchant(this);
    private int trainBoardCooldownTicks = 0;
    private @Nullable String previousRoleBeforeTrain = null;

    // Tractor configuration
    private boolean tractorMode = false;

    // Global area assignment (e.g. "Compound", "NorthFarm")
    private @Nullable String assignedAreaName = null;
    private int holeEscapeTicks = 0;

    // Base Vanilla-Style A* Pathfinding & Repetitive-Loop Insanity Guard
    private List<BlockPos> currentAStarPath = Collections.emptyList();
    private int currentAStarPathIndex = 0;
    private @Nullable BlockPos lastAStarGoalBlock = null;
    private int lastAStarComputeTick = -999;
    private @Nullable Vec3 insanityCheckPos = null;
    private int insanityStuckTicks = 0;
    private final Map<Long, Integer> unreachableTargetBlacklist = new java.util.HashMap<>();
    private String activePassiveActivity = "";

    public boolean isTargetBlacklisted(BlockPos pos) {
        if (pos == null) return false;
        Integer expiry = this.unreachableTargetBlacklist.get(pos.asLong());
        return expiry != null && this.tickCount < expiry;
    }

    public void invalidatePath() {
        this.currentAStarPath = Collections.emptyList();
        this.currentAStarPathIndex = 0;
        this.lastAStarGoalBlock = null;
        this.lastAStarComputeTick = -999;
        this.activeBypassWaypoint = null;
        this.bypassWaypointTicks = 0;
    }

    public @Nullable String getAssignedAreaName() {
        return this.assignedAreaName;
    }

    public synchronized void setAssignedArea(@Nullable String areaName) {
        this.assignedAreaName = areaName;
    }

    // Lumberjack configuration
    private @Nullable BlockPos lumberOriginPos = null;
    private int lumberRadius = 32;
    private boolean lumberjackAreaActive = false;
    private boolean lumberStoreLogs = true;
    private @Nullable BlockPos targetTreeBase = null;
    private final List<BlockPos> targetTreeLogs = new ArrayList<>();

    public @Nullable BlockPos getLumberOriginPos() { return this.lumberOriginPos; }
    public int getLumberRadius() { return this.lumberRadius; }
    public boolean isLumberjackAreaActive() { return this.lumberjackAreaActive; }
    public boolean isLumberStoreLogs() { return this.lumberStoreLogs; }

    public synchronized void setLumberjackConfig(@Nullable BlockPos origin, int radius, boolean areaActive, boolean storeLogs) {
        this.lumberOriginPos = origin;
        this.lumberRadius = Math.max(8, radius);
        this.lumberjackAreaActive = areaActive;
        this.lumberStoreLogs = storeLogs;
    }

    private final Deque<BotAction> actionQueue = new ArrayDeque<>();
    private @Nullable BotAction currentAction = null;

    // Task execution state
    private int actionProgressCount = 0;
    private int actionTicks = 0;
    private @Nullable BlockPos targetBlockPos = null;
    private int miningTicks = 0;
    private int attackCooldown = 0;
    private int stuckTicks = 0;
    private Vec3 lastCheckedPos = Vec3.ZERO;
    private double bestGoalDistSq = Double.MAX_VALUE;
    private int noProgressTicks = 0;

    // Intelligent Navigation & Dynamic Path Analysis
    private @Nullable Vec3 activeBypassWaypoint = null;
    private int bypassWaypointTicks = 0;
    private @Nullable Vec3 lastMoveTarget = null;
    private double bestMoveTargetDistSq = Double.MAX_VALUE;
    private int moveNoProgressTicks = 0;

    // Doorway & Fence Gate Navigation
    private @Nullable BlockPos lastOpenedDoorPos = null;
    private int doorCloseTimer = 0;
    private @Nullable BlockPos lastOpenedGatePos = null;
    private int gateCloseTimer = 0;

    // Tactical Combat & Defense
    private int shieldBlockTicks = 0;
    private int lastDistressTick = 0;
    private int currentCombatTargetId = -1;
    private int combatTargetEngageTicks = 0;
    private double bestCombatTargetDistSq = Double.MAX_VALUE;
    private final java.util.Map<Integer, Integer> unreachableMobBlacklist = new java.util.HashMap<>();
    private int bowChargeTicks = 0;

    // Tether / Standby & Absolute Command Mode
    private @Nullable BlockPos tetherPos = null;
    private double tetherRadius = 4.0;
    private boolean standingBy = false;
    private boolean absoluteCommandMode = false;

    // Bed, House & Night Shelter configuration
    private @Nullable BlockPos assignedBedPos = null;
    private @Nullable BlockPos assignedHousePos = null;
    private @Nullable String assignedHouseAreaName = null;
    private boolean nightShelterEnabled = true;
    private long lastBreedingTime = 0L;
    private long lastVillagerAmbientSoundTime = 0L;
    private boolean sentryMode = false;
    private @Nullable BlockPos sentryPos = null;

    // Baby Villager configuration
    private boolean isBaby = false;
    private int babyAgeTicks = 0;

    // Village House Construction
    private @Nullable List<HouseBlueprint.VoxelStep> activeHouseQueue = null;
    private int houseBuildCooldown = 0;
    private @Nullable BlockPos currentHouseOrigin = null;

    // Interactive Verbal Excavation & Radius-Constrained Mining
    private boolean awaitingExcavationDimensions = false;
    private @Nullable BlockPos pendingExcavationOrigin = null;
    private net.minecraft.core.Direction pendingExcavationFacing = net.minecraft.core.Direction.NORTH;
    private final List<BlockPos> activeExcavationQueue = new ArrayList<>();
    private @Nullable BlockPos mineZoneCenter = null;
    private int mineZoneRadius = 0;

    // Custom Skin Assignment
    private String customSkinName = "";

    public synchronized String getCustomSkinName() {
        return this.customSkinName;
    }

    public synchronized void setCustomSkinName(String skinName) {
        this.customSkinName = skinName != null ? skinName.trim() : "";
    }

    // Personal Training & Learned Knowledge
    private final List<String> learnedMemories = new ArrayList<>();
    private final List<LearnedRecipe> learnedRecipes = new ArrayList<>();

    public synchronized List<String> getLearnedMemories() { return new ArrayList<>(this.learnedMemories); }
    public synchronized void setLearnedMemories(List<String> memories) {
        this.learnedMemories.clear();
        if (memories != null) {
            for (String m : memories) if (m != null && !m.isBlank()) this.learnedMemories.add(m.trim());
        }
    }
    public synchronized boolean addLearnedMemory(String memory) {
        if (memory == null || memory.isBlank()) return false;
        String clean = memory.trim();
        if (!this.learnedMemories.contains(clean)) {
            this.learnedMemories.add(clean);
            return true;
        }
        return false;
    }
    public synchronized boolean removeLearnedMemory(int index) {
        if (index >= 0 && index < this.learnedMemories.size()) {
            this.learnedMemories.remove(index);
            return true;
        }
        return false;
    }
    public synchronized void clearLearnedMemories() { this.learnedMemories.clear(); }

    public synchronized List<LearnedRecipe> getLearnedRecipes() { return new ArrayList<>(this.learnedRecipes); }
    public synchronized void setLearnedRecipes(List<LearnedRecipe> recipes) {
        this.learnedRecipes.clear();
        if (recipes != null) this.learnedRecipes.addAll(recipes);
    }
    public synchronized void addLearnedRecipe(LearnedRecipe recipe) {
        if (recipe == null || recipe.resultItemId.isBlank()) return;
        this.learnedRecipes.removeIf(r -> r.matches(recipe.resultItemId) || r.name.equalsIgnoreCase(recipe.name));
        this.learnedRecipes.add(recipe);
    }
    public synchronized boolean removeLearnedRecipe(String nameOrItem) {
        if (nameOrItem == null || nameOrItem.isBlank()) return false;
        return this.learnedRecipes.removeIf(r -> r.matches(nameOrItem));
    }
    public synchronized void clearLearnedRecipes() { this.learnedRecipes.clear(); }

    public synchronized boolean isAwaitingExcavationReply() {
        return this.awaitingExcavationDimensions && this.pendingExcavationOrigin != null;
    }

    public AiBotPlayer(ServerLevel level, GameProfile profile, String role) {
        super(level.getServer(), level, profile, ClientInformation.createDefault());
        this.botName = profile.name();
        this.botRole = (role == null || role.isBlank()) ? "All-Around Survival Companion" : role;
        this.connection = new BotNetHandler(level.getServer(), this);
        var stepAttr = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        if (stepAttr != null && stepAttr.getBaseValue() < 1.06) {
            stepAttr.setBaseValue(1.06);
        }
    }

    public synchronized void setBaby(boolean baby, int ageTicks) {
        this.isBaby = baby;
        this.babyAgeTicks = baby ? Math.max(1, ageTicks) : 0;
        var scaleAttr = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(baby ? 0.55 : 1.0);
        }
    }

    public synchronized void setBaby(boolean baby) {
        setBaby(baby, 24000);
    }

    @Override
    public boolean isBaby() {
        return this.isBaby;
    }

    public int getBabyAgeTicks() {
        return this.babyAgeTicks;
    }

    public void setBabyAgeTicks(int ticks) {
        this.babyAgeTicks = ticks;
        if (ticks <= 0 && this.isBaby) {
            growUpToAdult();
        }
    }

    public synchronized void growUpToAdult() {
        this.isBaby = false;
        this.babyAgeTicks = 0;
        var scaleAttr = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(1.0);
        }
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 16, 0.4, 0.4, 0.4, 0.1);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        sayInChat(this.getPlainTextName() + " has grown up into an adult villager!");
        if (this.botRole.toLowerCase().contains("villager")) {
            searchAndClaimWorkstation(48);
        }
        if (this.level().getServer() != null) {
            BotPersistence.saveAll(this.level().getServer());
        }
    }

    public String getBotName() {
        return this.botName;
    }

    public String getBotRole() {
        return this.botRole;
    }

    public synchronized String getBotSubRole() {
        return this.botSubRole != null ? this.botSubRole : "unemployed";
    }

    public synchronized void setBotSubRole(String subRole) {
        String formatted = (subRole == null || subRole.isBlank()) ? "unemployed" : subRole.trim().toLowerCase(Locale.ROOT);
        if (!formatted.equalsIgnoreCase(this.botSubRole)) {
            this.botSubRole = formatted;
            if (this.villagerMerchant != null) {
                this.villagerMerchant.resetOffers();
            }
        }
    }

    public synchronized int getVillagerLevel() {
        return Math.max(1, Math.min(5, this.villagerLevel));
    }

    public synchronized void setVillagerLevel(int level) {
        this.villagerLevel = Math.max(1, Math.min(5, level));
        if (this.villagerMerchant != null) {
            this.villagerMerchant.resetOffers();
        }
    }

    public synchronized int getVillagerXp() {
        return Math.max(0, this.villagerXp);
    }

    public synchronized void setVillagerXp(int xp) {
        this.villagerXp = Math.max(0, xp);
    }

    public String getVillagerLevelName() {
        return switch (getVillagerLevel()) {
            case 1 -> "Novice";
            case 2 -> "Apprentice";
            case 3 -> "Journeyman";
            case 4 -> "Expert";
            case 5 -> "Master";
            default -> "Novice";
        };
    }

    public String getVillagerProfessionTitle() {
        String sub = getBotSubRole();
        if ("unemployed".equalsIgnoreCase(sub) || "none".equalsIgnoreCase(sub)) {
            return "Villager";
        }
        return capitalize(sub);
    }

    public void openVillagerTradeScreen(ServerPlayer player) {
        if (this.isBaby()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.35f);
            this.sayInChat("I'm still just a baby! I can't trade until I grow up!");
            return;
        }

        this.villagerMerchant.setTradingPlayer(player);
        this.villagerMerchant.ensureOffersInitialized();

        String title = this.getBotName() + " - " + getVillagerProfessionTitle() + " (" + getVillagerLevelName() + ")";

        OptionalInt containerId = player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, playerInv, p) -> new net.minecraft.world.inventory.MerchantMenu(id, playerInv, this.villagerMerchant),
                Component.literal(title)
        ));

        if (containerId.isPresent()) {
            player.sendMerchantOffers(containerId.getAsInt(), this.villagerMerchant.getOffers(), this.villagerLevel, this.villagerXp, true, true);
        }
    }

    public synchronized @Nullable BlockPos getAssignedWorkstationPos() {
        return this.assignedWorkstationPos;
    }

    public synchronized void setAssignedWorkstationPos(@Nullable BlockPos pos) {
        this.assignedWorkstationPos = pos;
    }

    public synchronized void setBotRole(String role) {
        if (role != null && !role.isBlank()) {
            String prev = this.botRole;
            this.botRole = role.trim();
            if (!this.botRole.toLowerCase().contains("villager") && prev != null && prev.toLowerCase().contains("villager")) {
                this.assignedWorkstationPos = null;
                this.botSubRole = "none";
            } else if (this.botRole.toLowerCase().contains("villager") && (!prev.toLowerCase().contains("villager") || "none".equals(this.botSubRole))) {
                if ("none".equals(this.botSubRole)) {
                    this.botSubRole = "unemployed";
                }
                if (!this.isBaby()) {
                    searchAndClaimWorkstation(48);
                }
            }
        }
    }

    public synchronized void setFarmArea(@Nullable BlockPos pos, int radiusX, int radiusZ, @Nullable List<String> allowedCrops, boolean allowTill) {
        this.farmOriginPos = pos;
        this.farmRadiusX = Math.max(2, radiusX);
        this.farmRadiusZ = Math.max(2, radiusZ);
        this.farmAllowedCrops.clear();
        if (allowedCrops != null) {
            for (String c : allowedCrops) {
                if (c != null && !c.isBlank()) this.farmAllowedCrops.add(c.trim().toLowerCase(Locale.ROOT));
            }
        }
        this.farmAllowTill = allowTill;
    }

    public synchronized @Nullable BlockPos getFarmOriginPos() { return this.farmOriginPos; }
    public synchronized int getFarmRadiusX() { return this.farmRadiusX; }
    public synchronized int getFarmRadiusZ() { return this.farmRadiusZ; }
    public synchronized List<String> getFarmAllowedCrops() { return new ArrayList<>(this.farmAllowedCrops); }
    public synchronized boolean isFarmAllowTill() { return this.farmAllowTill; }
    public synchronized boolean isAutoCompactWheatToHay() { return this.autoCompactWheatToHay; }
    public synchronized void setAutoCompactWheatToHay(boolean autoCompact) { this.autoCompactWheatToHay = autoCompact; }
    public synchronized int getMaxSeedsToKeepOnHand() { return this.maxSeedsToKeepOnHand; }
    public synchronized void setMaxSeedsToKeepOnHand(int max) { this.maxSeedsToKeepOnHand = Math.max(0, max); }
    public synchronized int getMaxSeedsInChest() { return this.maxSeedsInChest; }
    public synchronized void setMaxSeedsInChest(int max) { this.maxSeedsInChest = Math.max(0, max); }
    public synchronized boolean isAutoCompostExcessSeeds() { return this.autoCompostExcessSeeds; }
    public synchronized void setAutoCompostExcessSeeds(boolean autoCompost) { this.autoCompostExcessSeeds = autoCompost; }

    public synchronized void setRanchConfig(@Nullable BlockPos origin, double radius, String animal, String food,
                                            int maxPop, int minPop, boolean autoBreed, boolean autoCull, boolean adultsOnly) {
        this.ranchOriginPos = origin;
        this.ranchRadius = Math.max(5.0, radius);
        this.ranchAnimal = (animal != null && !animal.isBlank()) ? animal.trim().toLowerCase(Locale.ROOT) : "cow";
        this.ranchFood = (food != null && !food.isBlank()) ? food.trim().toLowerCase(Locale.ROOT) : "wheat";
        this.ranchMaxPop = Math.max(0, maxPop);
        this.ranchMinPop = Math.max(0, minPop);
        this.ranchAutoBreed = autoBreed;
        this.ranchAutoCull = autoCull;
        this.ranchAdultsOnly = adultsOnly;
    }

    public synchronized @Nullable BlockPos getRanchOriginPos() { return this.ranchOriginPos; }
    public synchronized double getRanchRadius() { return this.ranchRadius; }
    public synchronized String getRanchAnimal() { return this.ranchAnimal; }
    public synchronized String getRanchFood() { return this.ranchFood; }
    public synchronized int getRanchMaxPop() { return this.ranchMaxPop; }
    public synchronized int getRanchMinPop() { return this.ranchMinPop; }
    public synchronized boolean isRanchAutoBreed() { return this.ranchAutoBreed; }
    public synchronized boolean isRanchAutoCull() { return this.ranchAutoCull; }
    public synchronized boolean isRanchAdultsOnly() { return this.ranchAdultsOnly; }

    public synchronized void setConductorConfig(String dest, String returnStation, String freightItem, int count, boolean isReturning) {
        this.conductorDestStation = dest != null ? dest.trim() : "";
        this.conductorReturnStation = returnStation != null ? returnStation.trim() : "";
        this.conductorFreightItem = freightItem != null ? freightItem.trim() : "";
        this.conductorFreightCount = Math.max(0, count);
        this.conductorIsReturning = isReturning;
        this.conductorState = 0;
        this.conductorArrivedTicks = 0;
    }

    public synchronized String getConductorDestinationStation() { return this.conductorDestStation; }
    public synchronized String getConductorReturnStation() { return this.conductorReturnStation; }
    public synchronized String getConductorFreightItem() { return this.conductorFreightItem; }
    public synchronized int getConductorFreightCount() { return this.conductorFreightCount; }
    public synchronized boolean isConductorReturning() { return this.conductorIsReturning; }
    public synchronized void setConductorReturning(boolean returning) { this.conductorIsReturning = returning; }

    public synchronized boolean isTractorMode() { return this.tractorMode; }
    public synchronized void setTractorMode(boolean mode) { this.tractorMode = mode; }

    public synchronized void addAlias(String alias) {
        if (alias == null || alias.isBlank()) return;
        String cleaned = alias.trim();
        String lower = cleaned.toLowerCase();
        if (lower.equals(this.botName.toLowerCase())) return;
        for (String existing : this.aliases) {
            if (existing.equalsIgnoreCase(cleaned)) {
                this.activeNickname = existing;
                return;
            }
        }
        this.aliases.add(cleaned);
        this.activeNickname = cleaned;
    }

    public synchronized List<String> getAliases() {
        return new ArrayList<>(this.aliases);
    }

    public synchronized void setAliases(List<String> loadedAliases, @Nullable String loadedNickname) {
        this.aliases.clear();
        if (loadedAliases != null) {
            for (String a : loadedAliases) {
                if (a != null && !a.isBlank() && !a.equalsIgnoreCase(this.botName)) {
                    this.aliases.add(a.trim());
                }
            }
        }
        if (loadedNickname != null && !loadedNickname.isBlank()) {
            this.activeNickname = loadedNickname.trim();
            addAlias(this.activeNickname);
        } else if (!this.aliases.isEmpty()) {
            this.activeNickname = this.aliases.get(this.aliases.size() - 1);
        }
    }

    public synchronized @Nullable String getActiveNickname() {
        return this.activeNickname;
    }

    public synchronized String getChatTag() {
        if (this.activeNickname != null && !this.activeNickname.isBlank() && !this.activeNickname.equalsIgnoreCase(this.botName)) {
            return this.activeNickname + " (" + this.botName + ")";
        }
        return this.botName;
    }

    public synchronized boolean matchesIdentifier(String query) {
        if (query == null || query.isBlank()) return false;
        String q = query.trim();
        if (this.botName.equalsIgnoreCase(q)) return true;
        if (this.activeNickname != null && this.activeNickname.equalsIgnoreCase(q)) return true;
        for (String alias : this.aliases) {
            if (alias.equalsIgnoreCase(q)) return true;
        }
        return false;
    }

    public synchronized boolean containsBotReference(String message) {
        if (message == null || message.isBlank()) return false;
        if (containsWholeWord(message, this.botName)) return true;
        if (this.activeNickname != null && containsWholeWord(message, this.activeNickname)) return true;
        for (String alias : this.aliases) {
            if (containsWholeWord(message, alias)) return true;
        }
        return false;
    }

    public synchronized String stripBotReference(String message) {
        if (message == null || message.isBlank()) return "";
        String text = message;
        if (this.activeNickname != null) {
            text = text.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(this.activeNickname) + "\\b", "");
        }
        text = text.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(this.botName) + "\\b", "");
        for (String alias : this.aliases) {
            text = text.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(alias) + "\\b", "");
        }
        text = text.replaceAll("(?i)\\b(?:bot|hey|please|can\\s+you|just)\\b", "");
        return text.trim();
    }

    public synchronized @Nullable BlockPos getTetherPos() { return this.tetherPos; }
    public synchronized double getTetherRadius() { return this.tetherRadius; }
    public synchronized boolean isStandingBy() { return this.standingBy; }
    public synchronized boolean isAbsoluteCommandMode() { return this.absoluteCommandMode; }

    public synchronized void setTetherPos(@Nullable BlockPos pos) { this.tetherPos = pos; }
    public synchronized void setTetherRadius(double r) { this.tetherRadius = r > 0 ? r : 4.0; }
    public synchronized void setStandingBy(boolean standby) { this.standingBy = standby; }
    public synchronized void setAbsoluteCommandMode(boolean active) { this.absoluteCommandMode = active; }

    public synchronized boolean isSentryMode() { return this.sentryMode; }
    public synchronized @Nullable BlockPos getSentryPos() { return this.sentryPos; }
    public synchronized void setSentryMode(boolean active, @Nullable BlockPos pos) {
        this.sentryMode = active;
        this.sentryPos = pos;
    }

    public synchronized @Nullable BlockPos getAssignedBedPos() { return this.assignedBedPos; }
    public synchronized void setAssignedBedPos(@Nullable BlockPos pos) { this.assignedBedPos = pos; }
    public synchronized @Nullable BlockPos getAssignedHousePos() { return this.assignedHousePos; }
    public synchronized void setAssignedHousePos(@Nullable BlockPos pos) { this.assignedHousePos = pos; }
    public synchronized @Nullable String getAssignedHouseAreaName() { return this.assignedHouseAreaName; }
    public synchronized void setAssignedHouseAreaName(@Nullable String name) { this.assignedHouseAreaName = name; }
    public synchronized boolean isNightShelterEnabled() { return this.nightShelterEnabled; }
    public synchronized void setNightShelterEnabled(boolean enabled) { this.nightShelterEnabled = enabled; }
    public synchronized long getLastBreedingTime() { return this.lastBreedingTime; }
    public synchronized void setLastBreedingTime(long time) { this.lastBreedingTime = time; }

    public synchronized void setAbsoluteFollow(ServerPlayer target) {
        this.actionQueue.clear();
        BotAction followAct = new BotAction("FOLLOW", target.getPlainTextName(), "", 2);
        followAct.distance = 2.5;
        this.currentAction = followAct;
        this.absoluteCommandMode = true;
        this.standingBy = false;
        this.tetherPos = null;
        this.sentryMode = false;
        this.sentryPos = null;
        this.currentCombatTargetId = -1;
        this.combatTargetEngageTicks = 0;
        this.setLastHurtByMob(null);
        if (this.isUsingItem()) this.stopUsingItem();
        this.stopMovement();
        sayInChat("Following you directly, " + target.getPlainTextName() + "! (Absolute Command Mode active - autonomous tasks paused)");
    }

    public synchronized void setAbsoluteTether(BlockPos pos, double radius) {
        this.actionQueue.clear();
        this.currentAction = null;
        this.absoluteCommandMode = true;
        this.standingBy = true;
        this.tetherPos = pos;
        this.tetherRadius = radius > 0 ? radius : 4.0;
        this.sentryMode = false;
        this.sentryPos = null;
        this.currentCombatTargetId = -1;
        this.combatTargetEngageTicks = 0;
        this.setLastHurtByMob(null);
        if (this.isUsingItem()) this.stopUsingItem();
        this.stopMovement();
        sayInChat("Holding position at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") with a " + (int)this.tetherRadius + "m tether. Standing by!");
    }

    public synchronized void setAbsoluteSentry() {
        this.actionQueue.clear();
        this.currentAction = null;
        this.absoluteCommandMode = true;
        this.standingBy = true;
        this.sentryMode = true;
        this.sentryPos = this.blockPosition();
        this.tetherPos = this.blockPosition();
        this.tetherRadius = 2.0;
        this.currentCombatTargetId = -1;
        this.combatTargetEngageTicks = 0;
        this.setLastHurtByMob(null);
        if (this.isUsingItem()) this.stopUsingItem();
        this.stopMovement();
        equipBow();
        sayInChat("Sentry Mode activated at (" + this.getBlockX() + ", " + this.getBlockY() + ", " + this.getBlockZ() + ")! Holding post and sniping all hostiles up to 48m away.");
    }

    public synchronized void setAbsoluteStop() {
        this.actionQueue.clear();
        this.currentAction = null;
        this.absoluteCommandMode = true;
        this.standingBy = true;
        this.sentryMode = false;
        this.sentryPos = null;
        this.currentCombatTargetId = -1;
        this.combatTargetEngageTicks = 0;
        this.setLastHurtByMob(null);
        if (this.isUsingItem()) this.stopUsingItem();
        this.stopMovement();
        sayInChat("Stopped all tasks immediately. Standing by in Absolute Command Mode!");
    }

    public synchronized void resumeAutonomous() {
        this.actionQueue.clear();
        this.currentAction = null;
        this.absoluteCommandMode = false;
        this.standingBy = false;
        this.tetherPos = null;
        this.sentryMode = false;
        this.sentryPos = null;
        if (this.isUsingItem()) this.stopUsingItem();
        sayInChat("Resuming normal role behaviors and autonomous tasks.");
    }

    public synchronized void clearTether() {
        this.tetherPos = null;
        this.standingBy = false;
    }

    public static boolean containsWholeWord(String text, String word) {
        if (text == null || word == null || word.isBlank()) return false;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?<![a-zA-Z0-9_])" + java.util.regex.Pattern.quote(word.trim()) + "(?![a-zA-Z0-9_])",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        return p.matcher(text).find();
    }

    public synchronized boolean extractAndLearnNicknameFromPrompt(String text) {
        if (text == null || text.isBlank()) return false;
        String[] patterns = new String[]{
                "(?:assign\\s+(?:you\\s+)?(?:a\\s+|the\\s+)?nickname(?:\\s+to|\\s+is|:)?)\\s+([a-zA-Z0-9_-]{2,20})",
                "(?:set\\s+(?:your\\s+)?nickname(?:\\s+to|\\s+is|:)?)\\s+([a-zA-Z0-9_-]{2,20})",
                "(?:give\\s+you\\s+(?:the\\s+)?nickname(?:\\s+to|\\s+is|:)?)\\s+([a-zA-Z0-9_-]{2,20})",
                "(?:your\\s+nickname\\s+is(?:\\s+now)?)\\s+([a-zA-Z0-9_-]{2,20})",
                "(?:change\\s+(?:your\\s+)?(?:nick)?name\\s+to)\\s+([a-zA-Z0-9_-]{2,20})",
                "(?:nickname(?:\\s+is|:)?)\\s+([a-zA-Z0-9_-]{2,20})",
                "(?:call\\s+you|name\\s+you|rename\\s+you(?:\\s+to)?|rename\\s+yourself(?:\\s+to)?|your\\s+name\\s+is|you\\s+are\\s+now)\\s+([a-zA-Z0-9_-]{2,20})",
                "(?:respond\\s+to|answer\\s+to)\\s+([a-zA-Z0-9_-]{2,20})"
        };
        Set<String> stopWords = Set.of("from", "now", "on", "me", "here", "there", "please", "the", "a", "an", "to", "that", "this", "assign", "set", "give", "bot", "companion");
        for (String regex : patterns) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) {
                String candidate = m.group(1).trim();
                if (!stopWords.contains(candidate.toLowerCase(Locale.ROOT)) && !candidate.equalsIgnoreCase(this.botName)) {
                    addAlias(candidate);
                    this.activeNickname = candidate;
                    sayInChat("Nickname set to '" + candidate + "'! I will now respond to this name.");
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void setGuardAnchor(@Nullable BlockPos pos, double radius) {
        this.guardAnchorPos = pos;
        this.guardAnchorRadius = Math.max(5.0, radius);
    }

    public synchronized @Nullable BlockPos getGuardAnchorPos() {
        return this.guardAnchorPos;
    }

    public synchronized double getGuardAnchorRadius() {
        return this.guardAnchorRadius;
    }

    public synchronized boolean extractAndLearnAnchorFromPrompt(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        boolean mentionsAnchor = lower.contains("stay here") || lower.contains("stay within")
                || lower.contains("patrol within") || lower.contains("this is home")
                || lower.contains("guard this area") || lower.contains("roam the compound")
                || lower.contains("this bell") || lower.contains("fence boundr") || lower.contains("stay in")
                || lower.contains("inside the fence") || lower.contains("dont stray");

        if (mentionsAnchor) {
            double radius = 25.0;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*(?:blocks?|tiles?|m|meters?)").matcher(lower);
            if (m.find()) {
                try {
                    radius = Double.parseDouble(m.group(1));
                } catch (Exception ignored) {}
            }
            setGuardAnchor(this.blockPosition(), radius);
            return true;
        }
        return false;
    }

    public synchronized void configureFarmForSavedArea(com.aicompanion.bot.area.SavedArea area) {
        this.assignedAreaName = area.name;
        this.botRole = "farmer";
        this.farmOriginPos = BlockPos.containing(area.getCenter());
        int halfW = Math.max(8, (area.getMaxX() - area.getMinX() + 2) / 2);
        int halfL = Math.max(8, (area.getMaxZ() - area.getMinZ() + 2) / 2);
        this.farmRadiusX = halfW;
        this.farmRadiusZ = halfL;
        this.farmAllowTill = true;
        sayInChat("Farm duty set to mapped area '" + area.name + "' (~" + (int) area.getAreaSqMeters() + " m²)! I will systematically tend, harvest, replant, and deposit crops across the entire mapped area.");
    }

    public synchronized boolean extractAndLearnFarmConfigFromPrompt(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("tree") || lower.contains("trees") || lower.contains("wood") || lower.contains("log") || lower.contains("logs")
                || lower.contains("chop") || lower.contains("timber") || lower.contains("lumber")
                || lower.contains("guard") || lower.contains("defend") || lower.contains("protect") || lower.contains("sentry")
                || lower.contains("watch") || lower.contains("patrol") || lower.contains("stand") || lower.contains("stay")
                || lower.contains("hold") || lower.contains("camp") || lower.contains("remain")
                || lower.contains("not farm") || lower.contains("do no farm") || lower.contains("don't farm")
                || lower.contains("dont farm") || lower.contains("no farm") || lower.contains("not a farmer")
                || lower.contains("never farm") || lower.contains("stop farm")) {
            return false;
        }

        // Strip known area names so an area named "wheat_farm" doesn't trigger agricultural farm duty
        String textWithoutAreaNames = lower;
        for (com.aicompanion.bot.area.SavedArea a : BotPersistence.getAllAreas().values()) {
            textWithoutAreaNames = textWithoutAreaNames.replace(a.name.toLowerCase(Locale.ROOT), "");
        }

        boolean mentionsFarm = textWithoutAreaNames.contains("farm") || textWithoutAreaNames.contains("harvest") || textWithoutAreaNames.contains("crop")
                || textWithoutAreaNames.contains("plant") || textWithoutAreaNames.contains("replant") || textWithoutAreaNames.contains("till");
        if (!mentionsFarm) return false;

        com.aicompanion.bot.area.SavedArea foundArea = null;
        for (com.aicompanion.bot.area.SavedArea a : BotPersistence.getAllAreas().values()) {
            if (lower.contains(a.name.toLowerCase(Locale.ROOT))) {
                foundArea = a;
                break;
            }
        }
        if (foundArea == null && this.assignedAreaName != null && !this.assignedAreaName.isBlank()) {
            foundArea = BotPersistence.getArea(this.assignedAreaName);
        }
        if (foundArea == null && speaker != null) {
            for (com.aicompanion.bot.area.SavedArea a : BotPersistence.getAllAreas().values()) {
                if (a.contains(speaker.blockPosition())) {
                    foundArea = a;
                    break;
                }
            }
        }
        if (foundArea == null) {
            for (com.aicompanion.bot.area.SavedArea a : BotPersistence.getAllAreas().values()) {
                if (a.contains(this.blockPosition())) {
                    foundArea = a;
                    break;
                }
            }
        }
        if (foundArea == null && (lower.contains("this area") || lower.contains("the area") || lower.contains("it") || lower.contains("whole area") || lower.contains("area") || lower.contains("zone") || lower.contains("here"))) {
            com.aicompanion.bot.area.SavedArea closest = null;
            double closestDist = 48.0 * 48.0;
            BlockPos ref = speaker != null ? speaker.blockPosition() : this.blockPosition();
            for (com.aicompanion.bot.area.SavedArea a : BotPersistence.getAllAreas().values()) {
                Vec3 c = a.getCenter();
                double d = ref.distToCenterSqr(c.x, c.y, c.z);
                if (d < closestDist) {
                    closestDist = d;
                    closest = a;
                }
            }
            if (closest != null) {
                foundArea = closest;
            }
        }

        if (foundArea != null) {
            if (lower.contains("tractor")) {
                this.tractorMode = true;
            }
            configureFarmForSavedArea(foundArea);
            return true;
        }

        // Check area dimensions: e.g. "10x10", "20x20", "15 by 15", "10 x 10"
        int rx = 5;
        int rz = 5;
        java.util.regex.Matcher areaMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*(?:x|by)\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
        if (areaMatcher.find()) {
            try {
                int w = Integer.parseInt(areaMatcher.group(1));
                int l = Integer.parseInt(areaMatcher.group(2));
                rx = Math.max(2, (w + 1) / 2);
                rz = Math.max(2, (l + 1) / 2);
            } catch (Exception ignored) {}
        } else {
            java.util.regex.Matcher radMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*(?:blocks?|tiles?|area|radius)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
            if (radMatcher.find()) {
                try {
                    int r = Integer.parseInt(radMatcher.group(1));
                    rx = Math.max(2, (r + 1) / 2);
                    rz = rx;
                } catch (Exception ignored) {}
            }
        }

        // Origin: "from this spot", "from here", "this spot", "here"
        BlockPos origin = this.farmOriginPos;
        if (lower.contains("this spot") || lower.contains("from here") || lower.contains("at here") || lower.contains("right here") || origin == null) {
            origin = speaker != null ? speaker.blockPosition() : this.blockPosition();
        }

        // Allowed crops
        List<String> crops = new ArrayList<>();
        if (lower.contains("wheat") || lower.contains("wheet")) crops.add("wheat");
        if (lower.contains("carrot")) crops.add("carrot");
        if (lower.contains("potato") || lower.contains("potatoes")) crops.add("potato");
        if (lower.contains("beetroot") || lower.contains("beet")) crops.add("beetroot");

        boolean allowTill = !lower.contains("dont till") && !lower.contains("no till");

        setFarmArea(origin, rx, rz, crops, allowTill);
        this.botRole = "farmer";
        if (lower.contains("tractor")) {
            this.tractorMode = true;
        }

        String cropDesc = crops.isEmpty() ? "all crops" : String.join(" & ", crops);
        String tractorNote = this.tractorMode ? " (Tractor mode enabled!)" : "";
        sayInChat("Farm duty set! Working " + (rx * 2) + "x" + (rz * 2) + " area from ("
                + origin.getX() + ", " + origin.getY() + ", " + origin.getZ()
                + ") for " + cropDesc + tractorNote + ". I'll harvest 100% mature crops, replant, till dirt, and store in chest.");
        return true;
    }

    public synchronized boolean shouldReverseTowards(Entity loco, Vec3 targetPos) {
        net.minecraft.core.Direction facing = TrainCompat.getFacing(loco);
        double dx = targetPos.x - loco.getX();
        double dz = targetPos.z - loco.getZ();
        double dot = dx * facing.getStepX() + dz * facing.getStepZ();
        return dot < 0;
    }

    public synchronized void dismountFromTrain() {
        Entity loco = TrainCompat.getRiddenLocomotive(this);
        if (loco != null) {
            TrainCompat.stopTrain(loco, this);
            TrainCompat.dismountLocomotive(this);
        } else if (this.getVehicle() != null) {
            this.stopRiding();
        }

        // Clear all active locomotive orders & targets
        this.conductorFollowingPlayer = false;
        this.conductorTargetPlayer = null;
        this.conductorDestStation = "";
        this.conductorReturnStation = "";
        this.conductorTileTarget = 0;
        this.conductorMoveStartPos = null;
        this.conductorState = 0;
        this.conductorArrivedTicks = 0;
        this.clearActions();
        this.trainBoardCooldownTicks = 100; // 5 seconds dismount grace cooldown

        // If bot had a previous role prior to boarding, restore it
        if (this.previousRoleBeforeTrain != null && !this.previousRoleBeforeTrain.isBlank()) {
            this.botRole = this.previousRoleBeforeTrain;
            this.previousRoleBeforeTrain = null;
        } else if (this.botRole.equalsIgnoreCase("conductor") || this.botRole.equalsIgnoreCase("train")) {
            this.botRole = "companion";
        }

        // Step slightly away from the train so the bot isn't trapped in the train hitbox
        Vec3 p = this.position();
        this.teleportTo(p.x + 1.2, p.y, p.z + 1.2);
        this.stopMovement();
    }

    public synchronized boolean handleConductorChatCommand(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();

        Entity loco = TrainCompat.getRiddenLocomotive(this);
        boolean isConductorRole = this.botRole.toLowerCase().contains("conductor") || this.botRole.toLowerCase().contains("train");
        boolean mentionsTrain = lower.contains("train") || lower.contains("locomotive") || lower.contains("rail")
                || lower.contains("railway") || lower.contains("conductor");

        // If not riding a train, not a conductor, and not talking about trains/conductors, this handler does not apply!
        if (loco == null && !isConductorRole && !mentionsTrain) {
            return false;
        }

        // 1. STOP / CANCEL / HALT / BRAKE
        if (lower.equals("stop") || lower.contains("stop the train") || lower.contains("stop train")
                || lower.contains("cancel that") || lower.contains("cancel") || lower.contains("halt")
                || lower.contains("brake") || lower.contains("whoa") || lower.contains("hold on")
                || lower.equals("stay") || lower.contains("stand still")) {
            this.clearActions();
            if (loco != null) {
                TrainCompat.stopTrain(loco, this);
                sayInChat("Train stopped. All locomotive orders cleared!");
            } else {
                stopMovement();
                sayInChat("Orders cleared! Standing by.");
            }
            return true;
        }

        // 2. DISMOUNT / GET OUT / LEAVE TRAIN / GET OFF / EXIT
        if (lower.contains("get out") || lower.contains("dismount") || lower.contains("exit")
                || lower.contains("leave") || lower.contains("get off") || lower.contains("hop off")
                || lower.contains("hop out") || lower.contains("step out") || lower.contains("jump out")) {
            if (loco != null || this.getVehicle() != null) {
                dismountFromTrain();
                sayInChat("Dismounted from the train!");
            } else {
                sayInChat("I'm not in a train right now.");
            }
            return true;
        }

        // 3. COME HERE / COME TO ME / DRIVE TO ME (Player along rail or nearby)
        if (lower.contains("come here") || lower.contains("come to me") || lower.contains("drive to me")
                || lower.contains("drive here") || lower.contains("bring the train to me") || lower.contains("bring train to me")
                || lower.contains("follow me")) {
            if (speaker != null) {
                if (loco != null) {
                    double distSq = loco.distanceToSqr(speaker);
                    if (distSq <= 10000.0) {
                        this.conductorFollowingPlayer = true;
                        this.conductorTargetPlayer = speaker;
                        this.conductorDestStation = "";
                        this.conductorTileTarget = 0;
                        boolean reverse = shouldReverseTowards(loco, speaker.position());
                        TrainCompat.setReversed(loco, reverse);
                        TrainCompat.refuelLocomotive(this, loco);
                        TrainCompat.blowHorn(loco, this);
                        TrainCompat.startTrain(loco, this, 2, reverse);
                        sayInChat("Rolling down the rails to you right now!");
                        return true;
                    } else {
                        dismountFromTrain();
                        sayInChat("You're a bit far from the rail line! Dismounting to come to you on foot.");
                        moveTowards(speaker.position(), true);
                        return true;
                    }
                } else {
                    moveTowards(speaker.position(), true);
                    sayInChat("Coming right over to you!");
                    return true;
                }
            }
        }

        // Check if assigning conductor role
        if (lower.contains("conductor") || lower.contains("train driver") || lower.contains("engineer")) {
            this.botRole = "conductor";
            sayInChat("All aboard! I'm now assigned as Conductor. Say 'move forward', 'ahead 4 tiles', 'reverse', or 'drive to <station>'!");
            return true;
        }

        // Check if nearby locomotive can be mounted
        if (loco == null) {
            Entity nearby = TrainCompat.findNearbyLocomotive(this.level(), this.position(), 24.0);
            if (nearby != null && (isConductorRole || lower.contains("train") || lower.contains("locomotive") || lower.contains("ahead") || lower.contains("forward") || lower.contains("reverse"))) {
                if (this.previousRoleBeforeTrain == null && !isConductorRole) {
                    this.previousRoleBeforeTrain = this.botRole;
                }
                TrainCompat.mountLocomotive(this, nearby);
                loco = nearby;
            }
        }

        if (loco == null && !isConductorRole && !lower.contains("train") && !lower.contains("locomotive")) {
            return false;
        }

        // 4. FORWARD / AHEAD [N TILES/BLOCKS]
        java.util.regex.Matcher fwdMatcher = java.util.regex.Pattern.compile(
                "(?:move\\s+)?(?:ahead|forward|drive\\s+forward|go\\s+ahead)\\s*(?:(\\d+)\\s*(?:tiles?|blocks?|meters?|m)?)?",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
        if (fwdMatcher.find() && !lower.contains("to ") && !lower.contains("towards ")) {
            int tiles = 0;
            String countStr = fwdMatcher.group(1);
            if (countStr != null && !countStr.isBlank()) {
                try { tiles = Integer.parseInt(countStr.trim()); } catch (Exception ignored) {}
            }
            if (loco != null) {
                this.conductorDestStation = "";
                this.conductorFollowingPlayer = false;
                this.conductorTileTarget = tiles;
                this.conductorMoveStartPos = loco.position();
                TrainCompat.setReversed(loco, false);
                TrainCompat.refuelLocomotive(this, loco);
                TrainCompat.startTrain(loco, this, 2, false);
                if (tiles > 0) {
                    sayInChat("Moving ahead " + tiles + " blocks!");
                } else {
                    sayInChat("Moving ahead down the rails.");
                }
                return true;
            }
        }

        // 5. REVERSE / BACK / BACKWARD [N TILES/BLOCKS]
        java.util.regex.Matcher revMatcher = java.util.regex.Pattern.compile(
                "(?:move\\s+)?(?:back(?:ward)?|back\\s+up|reverse)\\s*(?:(\\d+)\\s*(?:tiles?|blocks?|meters?|m)?)?",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
        if (revMatcher.find() && !lower.contains("bring back") && !lower.contains("take it back")) {
            int tiles = 0;
            String countStr = revMatcher.group(1);
            if (countStr != null && !countStr.isBlank()) {
                try { tiles = Integer.parseInt(countStr.trim()); } catch (Exception ignored) {}
            }
            if (loco != null) {
                this.conductorDestStation = "";
                this.conductorFollowingPlayer = false;
                this.conductorTileTarget = tiles;
                this.conductorMoveStartPos = loco.position();
                TrainCompat.setReversed(loco, true);
                TrainCompat.refuelLocomotive(this, loco);
                TrainCompat.startTrain(loco, this, 2, true);
                if (tiles > 0) {
                    sayInChat("Reversing " + tiles + " blocks!");
                } else {
                    sayInChat("Reversing train down the rails.");
                }
                return true;
            }
        }

        // 6. BLOW HORN / WHISTLE
        if (lower.contains("whistle") || lower.contains("horn") || lower.contains("honk")) {
            if (loco != null) {
                TrainCompat.blowHorn(loco, this);
                sayInChat("Choo choo!");
                return true;
            }
        }

        // 7. SPEED CONTROL
        if (lower.contains("faster") || lower.contains("speed up")) {
            if (loco != null) {
                int spd = TrainCompat.getSpeedLevel(loco);
                TrainCompat.setSpeedLevel(loco, Math.min(5, spd + 1));
                sayInChat("Notching up throttle! Speed level " + Math.min(5, spd + 1));
                return true;
            }
        }
        if (lower.contains("slower") || lower.contains("slow down")) {
            if (loco != null) {
                int spd = TrainCompat.getSpeedLevel(loco);
                TrainCompat.setSpeedLevel(loco, Math.max(1, spd - 1));
                sayInChat("Slowing down! Speed level " + Math.max(1, spd - 1));
                return true;
            }
        }

        // 8. DRIVE TO STATION / WAYPOINT
        java.util.regex.Matcher destMatcher = java.util.regex.Pattern.compile(
                "(?:drive|take|run|conduct|go)\\s+(?:the\\s+)?(?:train|locomotive)?\\s*(?:to|towards)\\s+([a-zA-Z0-9_-]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if (destMatcher.find()) {
            String candDest = destMatcher.group(1).trim();
            Set<String> nonStations = Set.of("ahead", "forward", "back", "reverse", "stop", "here", "there", "me", "now", "it", "fast", "slow");
            if (!nonStations.contains(candDest.toLowerCase())) {
                String returnStation = "";
                String freight = "all";
                int count = 64;

                java.util.regex.Matcher retM = java.util.regex.Pattern.compile(
                        "(?:return|bring\\s+back|take\\s+it\\s+back|come\\s+back)\\s+(?:to\\s+)?([a-zA-Z0-9_-]+)",
                        java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
                if (retM.find()) {
                    returnStation = retM.group(1).trim();
                }

                java.util.regex.Matcher frM = java.util.regex.Pattern.compile(
                        "(?:bring|pick\\s*up|load|haul|get|fetch|transport)\\s+(?:back\\s+)?(?:some\\s+)?(?:(\\d+)\\s+)?([a-zA-Z0-9_-]+)",
                        java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
                if (frM.find()) {
                    String countStr = frM.group(1);
                    if (countStr != null) {
                        try { count = Integer.parseInt(countStr); } catch (Exception ignored) {}
                    }
                    String itemStr = frM.group(2).trim();
                    if (!itemStr.equalsIgnoreCase("freight") && !itemStr.equalsIgnoreCase("cargo") && !itemStr.equalsIgnoreCase("it")) {
                        freight = itemStr;
                    }
                }

                BotPersistence.SavedLocation savedLoc = BotPersistence.getLocation(candDest);
                if (savedLoc == null) {
                    sayInChat("I don't have a saved station named '" + candDest + "'. Stand at the station platform and say 'remember this spot as " + candDest + "' first!");
                    this.conductorDestStation = "";
                    return true;
                }

                this.botRole = "conductor";
                setConductorConfig(candDest, returnStation, freight, count, false);
                this.conductorTileTarget = 0;
                this.conductorFollowingPlayer = false;

                if (loco != null) {
                    boolean reverse = shouldReverseTowards(loco, new Vec3(savedLoc.x + 0.5, savedLoc.y + 0.5, savedLoc.z + 0.5));
                    TrainCompat.setReversed(loco, reverse);
                    TrainCompat.refuelLocomotive(this, loco);
                    TrainCompat.blowHorn(loco, this);
                    TrainCompat.startTrain(loco, this, 3, reverse);
                }
                String retDesc = returnStation.isBlank() ? "" : " and return to '" + returnStation + "'";
                sayInChat("Departing for station '" + candDest + "'" + retDesc + "! Throttle open.");
                return true;
            }
        }

        return false;
    }

    public synchronized boolean extractAndLearnHusbandryConfigFromPrompt(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        boolean mentionsHusbandry = lower.contains("breed") || lower.contains("husbandry") || lower.contains("ranch")
                || lower.contains("mate") || lower.contains("cull") || lower.contains("feed") || lower.contains("kill adult");
        if (!mentionsHusbandry) return false;

        // Animal type
        String animal = "cow";
        if (lower.contains("cow")) animal = "cow";
        else if (lower.contains("sheep")) animal = "sheep";
        else if (lower.contains("pig")) animal = "pig";
        else if (lower.contains("chicken")) animal = "chicken";
        else if (lower.contains("horse")) animal = "horse";

        // Food
        String food = "wheat";
        if (lower.contains("wheat") || lower.contains("wheet")) food = "wheat";
        else if (lower.contains("carrot")) food = "carrot";
        else if (lower.contains("potato")) food = "potato";
        else if (lower.contains("seed")) food = "seeds";
        else if (lower.contains("beetroot")) food = "beetroot";
        else {
            if ("pig".equals(animal)) food = "carrot";
            else if ("chicken".equals(animal)) food = "seeds";
        }

        // Max pop (culling threshold): "over 10", "more than 10", "exceeds 10", "cap 10"
        int maxPop = this.ranchMaxPop;
        boolean autoCull = this.ranchAutoCull;
        java.util.regex.Matcher maxMatcher = java.util.regex.Pattern.compile(
                "(?:when\\s+(?:amount|count|number|pop|population)\\s+is\\s+over|over|more\\s+than|exceeds?|limit|cap|max(?:imum)?)\\s+(\\d+)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
        if (maxMatcher.find()) {
            try {
                maxPop = Integer.parseInt(maxMatcher.group(1));
                autoCull = true;
            } catch (Exception ignored) {}
        } else if (lower.contains("cull") || lower.contains("kill")) {
            autoCull = true;
        }

        // Min pop: "never less then 5", "never less than 5", "never under 5", "keep at least 5", "min 5"
        int minPop = this.ranchMinPop;
        java.util.regex.Matcher minMatcher = java.util.regex.Pattern.compile(
                "(?:never\\s+(?:have\\s+)?less\\s+th(?:e|a)n|never\\s+under|keep\\s+at\\s+least|at\\s+least|minimum|min)\\s+(\\d+)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
        if (minMatcher.find()) {
            try {
                minPop = Integer.parseInt(minMatcher.group(1));
            } catch (Exception ignored) {}
        }

        boolean adultsOnly = !lower.contains("kill babies") && !lower.contains("kill baby");

        BlockPos origin = this.ranchOriginPos;
        if (lower.contains("this spot") || lower.contains("from here") || lower.contains("here") || origin == null) {
            origin = speaker != null ? speaker.blockPosition() : this.blockPosition();
        }

        setRanchConfig(origin, 25.0, animal, food, maxPop, minPop, true, autoCull, adultsOnly);
        this.botRole = "husbandry";

        StringBuilder reply = new StringBuilder();
        reply.append("Husbandry duty assigned! Breeding ").append(animal).append("s with ").append(food);
        if (autoCull && maxPop > 0) {
            reply.append(", culling adult ").append(animal).append("s when population exceeds ").append(maxPop)
                 .append(" (preserving at least ").append(minPop).append(" adults, never killing babies)");
        }
        reply.append(". Gathering drops and storing into chest!");
        sayInChat(reply.toString());
        return true;
    }

    public boolean isInsideFarmArea(BlockPos p) {
        if (this.assignedAreaName != null && !this.assignedAreaName.isBlank()) {
            com.aicompanion.bot.area.SavedArea area = BotPersistence.getArea(this.assignedAreaName);
            if (area != null) {
                return area.contains(p);
            }
        }
        if (this.farmOriginPos == null) {
            return this.distanceToSqr(Vec3.atCenterOf(p)) <= 400.0;
        }
        int dx = Math.abs(p.getX() - this.farmOriginPos.getX());
        int dy = Math.abs(p.getY() - this.farmOriginPos.getY());
        int dz = Math.abs(p.getZ() - this.farmOriginPos.getZ());
        return dx <= this.farmRadiusX && dz <= this.farmRadiusZ && dy <= 6;
    }

    public boolean isCropAllowed(net.minecraft.world.level.block.CropBlock crop) {
        if (this.farmAllowedCrops.isEmpty()) return true;
        String name = BuiltInRegistries.BLOCK.getKey(crop).getPath().toLowerCase();
        for (String allowed : this.farmAllowedCrops) {
            if (name.contains(allowed)) return true;
        }
        return false;
    }

    public boolean isSeedAllowed(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
        if (this.farmAllowedCrops.isEmpty()) {
            return id.contains("seed") || id.contains("carrot") || id.contains("potato");
        }
        for (String allowed : this.farmAllowedCrops) {
            if (allowed.contains("wheat") && (id.contains("wheat") || id.contains("seed"))) return true;
            if (allowed.contains("carrot") && id.contains("carrot")) return true;
            if (allowed.contains("potato") && id.contains("potato")) return true;
            if (allowed.contains("beetroot") && (id.contains("beetroot") || id.contains("seed"))) return true;
            if (id.contains(allowed)) return true;
        }
        return false;
    }

    public boolean isInsideRanchArea(Vec3 pos) {
        Vec3 center = this.ranchOriginPos != null ? Vec3.atCenterOf(this.ranchOriginPos) : this.position();
        return pos.distanceToSqr(center) <= (this.ranchRadius * this.ranchRadius);
    }

    public void applyRoleStarterKit() {
        String r = this.botRole.toLowerCase();
        if (r.contains("guard") || r.contains("bodyguard") || r.contains("warrior") || r.contains("combat")) {
            this.getInventory().add(new ItemStack(Items.STONE_SWORD, 1));
            this.getInventory().add(new ItemStack(Items.SHIELD, 1));
            this.getInventory().add(new ItemStack(Items.IRON_HELMET, 1));
            this.autoEquipArmor();
            this.equipBestWeapon();
        } else if (r.contains("farmer") || r.contains("farm")) {
            this.getInventory().add(new ItemStack(Items.IRON_HOE, 1));
            this.getInventory().add(new ItemStack(Items.WHEAT_SEEDS, 32));
            this.getInventory().add(new ItemStack(Items.CARROT, 16));
            this.getInventory().add(new ItemStack(Items.POTATO, 16));
            this.getInventory().add(new ItemStack(Items.BONE_MEAL, 16));
            this.getInventory().add(new ItemStack(Items.BREAD, 8));
        } else if (r.contains("husbandry") || r.contains("ranch") || r.contains("breed")) {
            this.getInventory().add(new ItemStack(Items.IRON_SWORD, 1));
            this.getInventory().add(new ItemStack(Items.WHEAT, 32));
            this.getInventory().add(new ItemStack(Items.CARROT, 16));
            this.getInventory().add(new ItemStack(Items.WHEAT_SEEDS, 32));
            this.getInventory().add(new ItemStack(Items.BREAD, 8));
            this.equipBestWeapon();
        } else if (r.contains("miner") || r.contains("mining")) {
            this.getInventory().add(new ItemStack(Items.STONE_PICKAXE, 1));
            this.getInventory().add(new ItemStack(Items.TORCH, 32));
        } else if (r.contains("lumber") || r.contains("wood")) {
            this.getInventory().add(new ItemStack(Items.STONE_AXE, 1));
            this.getInventory().add(new ItemStack(Items.OAK_SAPLING, 16));
        } else if (r.contains("builder") || r.contains("build")) {
            this.getInventory().add(new ItemStack(Items.COBBLESTONE, 64));
            this.getInventory().add(new ItemStack(Items.OAK_PLANKS, 64));
            this.getInventory().add(new ItemStack(Items.OAK_DOOR, 1));
            this.getInventory().add(new ItemStack(Items.BED.red(), 1));
            this.getInventory().add(new ItemStack(Items.GLASS, 8));
            this.getInventory().add(new ItemStack(Items.TORCH, 16));
            this.getInventory().add(new ItemStack(Items.STONE_AXE, 1));
        } else if (r.contains("courier") || r.contains("helper") || r.contains("collector")) {
            this.getInventory().add(new ItemStack(Items.CHEST, 2));
            this.getInventory().add(new ItemStack(Items.BREAD, 8));
        } else if (r.contains("conductor") || r.contains("train") || r.contains("engineer")) {
            this.getInventory().add(new ItemStack(Items.COAL, 32));
            this.getInventory().add(new ItemStack(Items.CLOCK, 1));
            this.getInventory().add(new ItemStack(Items.COMPASS, 1));
            this.getInventory().add(new ItemStack(Items.IRON_HELMET, 1));
            this.getInventory().add(new ItemStack(Items.BREAD, 16));
            this.autoEquipArmor();
        } else if (r.contains("villager")) {
            this.getInventory().add(new ItemStack(Items.BREAD, 6));
            this.getInventory().add(new ItemStack(Items.WHEAT, 6));
        }
    }

    public String getCurrentTaskSummary() {
        if (currentAction == null && actionQueue.isEmpty()) {
            if (this.isSleeping()) {
                return "Sleeping in bed at (" + this.getBlockX() + ", " + this.getBlockY() + ", " + this.getBlockZ() + ")";
            }
            if (this.standingBy) {
                return "Standing by (stationary)";
            }
            boolean physicallyMoving = this.zza > 0.01f || this.getDeltaMovement().horizontalDistanceSqr() > 0.002;
            if (!this.activePassiveActivity.isBlank()) {
                return this.activePassiveActivity + (physicallyMoving ? " [actively walking]" : " [at position]");
            }
            if (physicallyMoving) {
                return "Autonomous movement (Role: " + this.botRole + ")";
            }
            return "Idle (standing still, waiting for orders)";
        }
        StringBuilder sb = new StringBuilder();
        if (currentAction != null) {
            sb.append(currentAction.toString());
            String norm = currentAction.getNormalizedType();
            if (currentAction.count > 1 && ("MINE".equals(norm) || "EXCAVATE".equals(norm) || "ATTACK".equals(norm))) {
                sb.append(" [").append(actionProgressCount).append("/").append(currentAction.count).append("]");
            }
        }
        if (!actionQueue.isEmpty()) {
            sb.append(" (+" + actionQueue.size() + " queued)");
        }
        return sb.toString();
    }

    public List<BotAction> getRemainingActionsSnapshot() {
        List<BotAction> list = new ArrayList<>();
        if (this.currentAction != null) {
            list.add(this.currentAction);
        }
        list.addAll(this.actionQueue);
        return list;
    }

    public void setActionPlan(List<BotAction> actions) {
        this.clearActions();
        if (actions != null) {
            for (BotAction a : actions) {
                if (a != null && !a.getNormalizedType().isEmpty()) {
                    if ("STOP".equals(a.getNormalizedType())) {
                        this.clearActions();
                        this.standingBy = true;
                        this.stopMovement();
                        return;
                    }
                    this.standingBy = false;
                    this.actionQueue.addLast(a);
                }
            }
        }
        advanceNextAction();
    }

    public void clearActions() {
        if (this.targetBlockPos != null) {
            this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
            this.targetBlockPos = null;
        }
        this.actionQueue.clear();
        this.currentAction = null;
        this.actionProgressCount = 0;
        this.actionTicks = 0;
        this.miningTicks = 0;
        this.activeExcavationQueue.clear();
        this.mineZoneCenter = null;
        this.mineZoneRadius = 0;
        this.conductorDestStation = "";
        this.conductorReturnStation = "";
        this.conductorTileTarget = 0;
        this.conductorMoveStartPos = null;
        this.conductorFollowingPlayer = false;
        this.conductorTargetPlayer = null;
        this.conductorIsReturning = false;
        Entity loco = TrainCompat.getRiddenLocomotive(this);
        if (loco != null) {
            TrainCompat.stopTrain(loco, this);
        }
        this.stopMovement();
    }

    private String lastCompletedActionType = "IDLE";

    public String getCurrentActionTypeForLearning() {
        if (this.currentAction != null) {
            return this.currentAction.getNormalizedType();
        }
        if (this.conductorFollowingPlayer) {
            return "FOLLOW";
        }
        if (this.sentryMode) {
            return "SENTRY";
        }
        if (this.lastCompletedActionType != null && !this.lastCompletedActionType.equals("IDLE")) {
            return this.lastCompletedActionType;
        }
        String r = this.botRole.toLowerCase(Locale.ROOT);
        if (r.contains("farm")) return "FARM_AUTONOMOUS";
        if (r.contains("guard")) return "GUARD_PATROL";
        if (r.contains("villager")) return "VILLAGER_" + this.getBotSubRole().toUpperCase(Locale.ROOT);
        if (r.contains("lumber")) return "LUMBER_AUTONOMOUS";
        if (r.contains("mine")) return "MINE_AUTONOMOUS";
        if (r.contains("husbandry") || r.contains("ranch")) return "RANCH_AUTONOMOUS";
        return "AUTONOMOUS_" + r.toUpperCase(Locale.ROOT);
    }

    private void advanceNextAction() {
        if (this.currentAction != null) {
            this.lastCompletedActionType = this.currentAction.getNormalizedType();
            com.aicompanion.bot.learning.HiveMindBrain.applyReinforcementSignal(this, 3.5f, false);
        }
        if (this.targetBlockPos != null) {
            this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
            this.targetBlockPos = null;
        }
        this.stopMovement();
        this.currentAction = this.actionQueue.pollFirst();
        this.actionProgressCount = 0;
        this.actionTicks = 0;
        this.miningTicks = 0;
        this.stuckTicks = 0;
        this.moveNoProgressTicks = 0;
        this.bestMoveTargetDistSq = Double.MAX_VALUE;
        this.bestGoalDistSq = Double.MAX_VALUE;
        this.noProgressTicks = 0;
    }

    public void sayInChat(String message) {
        if (message == null || message.isBlank()) return;
        Component formatted = Component.literal("[" + this.getChatTag() + "] ")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal(message).withStyle(ChatFormatting.WHITE));
        this.level().getServer().getPlayerList().broadcastSystemMessage(formatted, false);
        GeminiService.recordMemory(this.botName, this.getChatTag() + ": " + message);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) return;

        // Run LivingEntity / Player physics, gravity, item pickup, and food tick
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.doTick();
        this.level().getChunkSource().move(this);

        if (this.isBaby && this.babyAgeTicks > 0) {
            this.babyAgeTicks--;
            if (this.babyAgeTicks <= 0) {
                growUpToAdult();
            }
        }

        if (this.tickCount % 40 == 0) {
            var stepAttr = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
            if (stepAttr != null && stepAttr.getBaseValue() < 1.06) {
                stepAttr.setBaseValue(1.06);
            }
            var scaleAttr = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
            if (scaleAttr != null) {
                double targetScale = this.isBaby ? 0.55 : 1.0;
                if (Math.abs(scaleAttr.getBaseValue() - targetScale) > 0.01) {
                    scaleAttr.setBaseValue(targetScale);
                }
            }
        }

        if (!this.isAlive()) {
            return;
        }

        // Active Fluid Survival: Water Anti-Drowning & Lava / Crude Oil Hazardous Fluid Physics
        if (isInHazardousFluid()) {
            this.setJumping(true);
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, Math.max(motion.y, 0.25), motion.z);
            escapeHazardousFluidToDryLand();
        } else if (this.isInWater()) {
            this.setJumping(true);
            Vec3 motion = this.getDeltaMovement();
            if (this.isEyeInFluid(FluidTags.WATER) || motion.y < 0.04) {
                this.setDeltaMovement(motion.x, Math.max(motion.y, 0.08), motion.z);
            }
            if (this.isEyeInFluid(FluidTags.WATER) || this.getAirSupply() < 200) {
                escapeWaterToDryLand();
            }
        } else {
            // Default jumping to false on dry land every tick so jump state can NEVER get stuck on!
            this.setJumping(false);
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (this.shieldBlockTicks > 0) {
            this.shieldBlockTicks--;
            if (this.shieldBlockTicks <= 0 && this.isUsingItem() && this.getOffhandItem().is(Items.SHIELD)) {
                this.stopUsingItem();
            }
        }

        // Auto-close passed doors and gates (only when fully clear of the opening)
        if (this.lastOpenedGatePos != null) {
            this.gateCloseTimer--;
            Vec3 gateCenter = Vec3.atCenterOf(this.lastOpenedGatePos);
            double distSq = this.distanceToSqr(gateCenter);
            net.minecraft.world.phys.AABB gateBox = new net.minecraft.world.phys.AABB(this.lastOpenedGatePos).inflate(0.25);
            boolean stillInGate = this.getBoundingBox().intersects(gateBox);

            boolean clearedGate = !stillInGate && (distSq > 3.0 || (this.gateCloseTimer <= 0 && distSq > 1.5));
            if (clearedGate) {
                BlockState gs = this.level().getBlockState(this.lastOpenedGatePos);
                if (gs.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock && gs.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN)) {
                    this.level().setBlockAndUpdate(this.lastOpenedGatePos, gs.setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, false));
                    this.level().playSound(null, this.lastOpenedGatePos, net.minecraft.sounds.SoundEvents.FENCE_GATE_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                this.lastOpenedGatePos = null;
            }
        }
        if (this.lastOpenedDoorPos != null) {
            this.doorCloseTimer--;
            Vec3 doorCenter = Vec3.atCenterOf(this.lastOpenedDoorPos);
            double distSq = this.distanceToSqr(doorCenter);
            net.minecraft.world.phys.AABB doorBox = new net.minecraft.world.phys.AABB(this.lastOpenedDoorPos).inflate(0.25);
            boolean stillInDoor = this.getBoundingBox().intersects(doorBox);

            boolean clearedDoor = !stillInDoor && (distSq > 3.0 || (this.doorCloseTimer <= 0 && distSq > 1.5));
            if (clearedDoor) {
                BlockState ds = this.level().getBlockState(this.lastOpenedDoorPos);
                if (ds.getBlock() instanceof DoorBlock door && ds.getValue(DoorBlock.OPEN)) {
                    door.setOpen(this, this.level(), ds, this.lastOpenedDoorPos, false);
                }
                this.lastOpenedDoorPos = null;
            }
        }

        // Periodic survival maintenance every 20 ticks
        if (this.tickCount % 20 == 0) {
            this.unreachableMobBlacklist.entrySet().removeIf(entry -> this.tickCount >= entry.getValue());
            if (BotConfig.AUTO_EAT.getAsBoolean()) {
                tryAutoEat();
            }
            autoEquipArmor();
            vacuumNearbyDrops();
        }

        // Sentry Mode: Hold exact post, 360-degree scan, and snipe all hostiles up to 48m away
        if (this.sentryMode && this.sentryPos != null) {
            Vec3 postCenter = Vec3.atCenterOf(this.sentryPos);
            double distToPostSq = this.distanceToSqr(postCenter);
            if (distToPostSq > 2.25) {
                moveTowards(postCenter, true);
                return;
            }
            Monster sentryThreat = findSentryTarget(48.0);
            if (sentryThreat != null) {
                engageTarget(sentryThreat);
                return;
            } else {
                this.stopMovement();
                equipBow();
                float scanYaw = (this.tickCount * 2.5f) % 360.0f;
                this.setYRot(scanYaw);
                this.setYHeadRot(scanYaw);
                this.setXRot(0.0f);
                return;
            }
        }

        // Tether boundary enforcement: if tethered and outside tether radius, walk back to tether center immediately
        if (this.tetherPos != null) {
            Vec3 tetherCenter = Vec3.atCenterOf(this.tetherPos);
            double distToTetherSq = this.distanceToSqr(tetherCenter);
            if (distToTetherSq > (this.tetherRadius * this.tetherRadius)) {
                if (this.isUsingItem()) this.stopUsingItem();
                this.bowChargeTicks = 0;
                moveTowards(tetherCenter, true);
                return;
            }
        }

        // Auto-defend if attacked by a monster
        if (BotConfig.AUTO_DEFEND.getAsBoolean() && this.getLastHurtByMob() != null && this.getLastHurtByMob().isAlive()) {
            LivingEntity attacker = this.getLastHurtByMob();
            double maxDefendDistSq = hasBow() ? 2304.0 : 64.0;
            if (!(attacker instanceof Player) && this.distanceToSqr(attacker) < maxDefendDistSq && !this.unreachableMobBlacklist.containsKey(attacker.getId())) {
                if (this.tetherPos != null) {
                    double attackerDistToTetherSq = attacker.distanceToSqr(Vec3.atCenterOf(this.tetherPos));
                    if (attackerDistToTetherSq > (this.tetherRadius * this.tetherRadius) && !hasBow()) {
                        // Beyond tether radius and cannot shoot; remain at post
                        this.setLastHurtByMob(null);
                    } else {
                        engageTarget(attacker);
                        return;
                    }
                } else {
                    engageTarget(attacker);
                    return;
                }
            }
        }

        // Execute current action
        if (this.currentAction == null && !this.actionQueue.isEmpty()) {
            advanceNextAction();
        }

        if (this.currentAction != null) {
            this.actionTicks++;
            // Miner role passive: auto-place torch in dark caves while working
            if (this.tickCount % 30 == 0 && this.botRole.toLowerCase().contains("miner")) {
                if (this.level().getMaxLocalRawBrightness(this.blockPosition()) < 7) {
                    placeSpecificBlockFromInventory("torch", this.blockPosition());
                }
            }
            boolean finished = tickCurrentAction(this.currentAction);
            if (finished) {
                GeminiService.recordMemory(this.botName, "[Completed Action: " + this.currentAction + "]");
                advanceNextAction();
            }
        } else {
            if (this.standingBy || this.absoluteCommandMode || this.tetherPos != null) {
                this.stopMovement();
                if (this.tickCount % 20 == 0) {
                    Player nearest = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 8.0, p -> p != this && !(p instanceof AiBotPlayer));
                    if (nearest != null) {
                        lookAtPos(nearest.getEyePosition());
                    }
                }
            } else {
                tickPassiveRoleBehavior();
            }
        }
    }

    private void tickPassiveRoleBehavior() {
        if (this.standingBy || this.absoluteCommandMode || this.tetherPos != null) {
            this.stopMovement();
            return;
        }
        String r = this.botRole.toLowerCase();

        // Night Shelter & Bed Sleeping: Unless told otherwise, seek shelter and bed at night until AM
        boolean isGuardRole = r.contains("guard") || r.contains("bodyguard") || r.contains("warrior");
        boolean shouldSeekShelter = this.nightShelterEnabled && (this.assignedBedPos != null || !isGuardRole);
        if (shouldSeekShelter && (isNightTime() || this.level().isThundering())) {
            if (tickNightShelterAndSleep()) {
                return;
            }
        } else if (this.isSleeping()) {
            this.stopSleepInBed(false, true);
        }

        // 1. Guard / Bodyguard: Automatically patrol compound / anchor or follow player, intercepting nearby monsters (never jumping fences!)
        if (r.contains("guard") || r.contains("bodyguard") || r.contains("warrior")) {
            BlockPos guardCenter = this.guardAnchorPos != null ? this.guardAnchorPos : this.blockPosition();
            double maxDist = this.guardAnchorPos != null ? this.guardAnchorRadius : 24.0;

            // Return inside if drifted outside anchor boundary
            if (this.guardAnchorPos != null && this.distanceToSqr(Vec3.atCenterOf(this.guardAnchorPos)) > (maxDist * maxDist)) {
                moveTowards(Vec3.atCenterOf(this.guardAnchorPos), true);
                return;
            }

            Monster threat = findNearestProvokedThreatNear(guardCenter, maxDist, null);
            if (threat != null) {
                // Ensure monster is on our side of the fence and inside our guard perimeter
                boolean threatInside = this.guardAnchorPos == null || threat.distanceToSqr(Vec3.atCenterOf(this.guardAnchorPos)) <= (maxDist * maxDist);
                boolean fenceBlocked = isLineBlockedByFence(this.position(), threat.position());
                if (threatInside && (!fenceBlocked || hasBow())) {
                    engageTarget(threat);
                    return;
                }
            }

            if (this.guardAnchorPos != null) {
                // Anchored to compound/bell: patrol peacefully within the compound
                double distToAnchorSq = this.distanceToSqr(Vec3.atCenterOf(this.guardAnchorPos));
                if (distToAnchorSq > 16.0) {
                    moveTowards(Vec3.atCenterOf(this.guardAnchorPos), false);
                } else {
                    stopMovement();
                    if (this.tickCount % 60 == 0) {
                        float randomYaw = (this.tickCount * 17) % 360;
                        this.setYRot(randomYaw);
                        this.setYHeadRot(randomYaw);
                    }
                }
                return;
            } else {
                Player nearestHuman = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 48.0, p -> p != this && !(p instanceof AiBotPlayer));
                if (nearestHuman != null) {
                    double distSq = this.distanceToSqr(nearestHuman);
                    if (distSq > 16.0) {
                        moveTowards(nearestHuman.position(), distSq > 36.0);
                    } else {
                        stopMovement();
                        if (this.tickCount % 10 == 0) lookAtPos(nearestHuman.getEyePosition());
                    }
                    return;
                }
            }
        }

        // 2. Farmer: Automatically tend crops, harvest mature only, replant immediately, plant empty farmland, till dirt, and store in chest
        if (r.contains("farmer") || r.contains("farm")) {
            tickPassiveFarmer();
            return;
        }

        // 3. Husbandry / Rancher: Breed livestock, cull excess adults when count > maxPop (preserving minPop), store drops in chest
        if (r.contains("husbandry") || r.contains("ranch") || r.contains("breed") || r.contains("cattle")) {
            tickPassiveRancher();
            return;
        }

        // 3. Courier / Helper: Follow nearest human player and vacuum dropped items within 12m
        if (r.contains("courier") || r.contains("helper") || r.contains("collector")) {
            ItemEntity drop = findNearestDrop(12.0);
            if (drop != null) {
                moveTowards(drop.position(), true);
                return;
            }
            Player nearestHuman = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 32.0, p -> p != this && !(p instanceof AiBotPlayer));
            if (nearestHuman != null && this.distanceToSqr(nearestHuman) > 16.0) {
                moveTowards(nearestHuman.position(), false);
                return;
            }
        }

        // 4. Miner: Auto-place torch if standing in darkness
        if (r.contains("miner") && this.tickCount % 40 == 0) {
            if (this.level().getMaxLocalRawBrightness(this.blockPosition()) < 7) {
                placeSpecificBlockFromInventory("torch", this.blockPosition());
            }
        }

        // 5. Conductor: Drive locomotives, blow horn, haul freight to stations and return
        if (r.contains("conductor") || r.contains("train") || r.contains("engineer") || !this.conductorDestStation.isBlank()) {
            tickPassiveConductor();
            return;
        }

        // 6. Lumberjack: Automatically locate trees in lumber zone, fell whole trees, replant saplings, vacuum drops, and store in chest
        if (r.contains("lumber") || r.contains("wood") || this.lumberjackAreaActive) {
            tickPassiveLumberjack();
            return;
        }

        // 7. Villager: 100% vanilla villager routine (workstation, gathering/breeding, panic/flee, bed)
        if (r.contains("villager")) {
            tickPassiveVillager();
            return;
        }

        this.stopMovement();
        if (this.tickCount % 10 == 0) {
            Player nearest = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 8.0, p -> p != this);
            if (nearest != null) {
                lookAtPos(nearest.getEyePosition());
            }
        }
    }

    private boolean tickCurrentAction(BotAction action) {
        String type = action.getNormalizedType();
        return switch (type) {
            case "STOP" -> {
                if (TrainCompat.getRiddenLocomotive(this) != null || this.getVehicle() != null) {
                    dismountFromTrain();
                }
                clearActions();
                yield true;
            }
            case "DISMOUNT" -> {
                dismountFromTrain();
                sayInChat("Dismounted from the train!");
                yield true;
            }
            case "FOLLOW" -> tickFollowAction(action);
            case "GOTO" -> tickGotoAction(action);
            case "GUARD" -> tickGuardAction(action);
            case "MINE" -> tickMineAction(action);
            case "EXCAVATE" -> tickExcavateAction(action);
            case "FARM" -> tickFarmAction(action);
            case "TILL" -> tickTillAction(action);
            case "BREED" -> tickBreedAction(action);
            case "CULL" -> tickCullAction(action);
            case "HUSBANDRY" -> tickHusbandryAction(action);
            case "CRAFT" -> executeCraftAction(action);
            case "GIVE" -> tickGiveAction(action);
            case "ATTACK" -> tickAttackAction(action);
            case "EQUIP" -> {
                equipBestGear(action.getPrimaryTarget());
                yield true;
            }
            case "PLACE", "BUILD" -> executePlaceAction(action);
            case "BUILD_HOUSE", "BUILD_BLUEPRINT" -> tickHouseBuildingAction();
            case "CHEST_STORE" -> tickChestAction(action, true);
            case "CHEST_TAKE" -> tickChestAction(action, false);
            case "CONDUCT_TRAIN", "DRIVE_TRAIN" -> {
                String dest = action.getPrimaryTarget();
                String freight = action.item != null ? action.item : "all";
                int count = action.count > 0 ? action.count : 64;
                setConductorConfig(dest, "", freight, count, false);
                if (this.previousRoleBeforeTrain == null && !this.botRole.equalsIgnoreCase("conductor")) {
                    this.previousRoleBeforeTrain = this.botRole;
                }
                this.botRole = "conductor";
                tickPassiveConductor();
                yield this.conductorDestStation.isBlank();
            }
            case "DRIVE_TRACTOR", "TRACTOR_FARM" -> {
                this.tractorMode = true;
                this.botRole = "farmer";
                tickPassiveFarmer();
                yield !TractorCompat.isRidingTractor(this);
            }
            case "EAT" -> {
                forceEatFromInventory();
                yield true;
            }
            default -> true;
        };
    }

    private boolean tickFollowAction(BotAction action) {
        ServerPlayer target = findPlayer(action.getPrimaryTarget());
        if (target == null) {
            sayInChat("I can't find " + action.getPrimaryTarget() + " to follow.");
            return true;
        }

        // Defend guarded player from nearby provoked monsters
        if (BotConfig.AUTO_DEFEND.getAsBoolean()) {
            Monster threat = findNearestProvokedThreatNear(target.blockPosition(), hasBow() ? 36.0 : 20.0, target);
            if (threat != null) {
                engageTarget(threat);
                return false;
            }
        }

        double desiredDist = Math.max(2.0, action.distance > 0 ? action.distance : 3.0);
        double distSq = this.distanceToSqr(target);
        if (distSq > 3600.0 && target.level() == this.level()) {
            // Teleport if >60 blocks behind while following
            this.teleportTo(target.getX() + 1.5, target.getY(), target.getZ() + 1.5);
            return false;
        }

        if (distSq > desiredDist * desiredDist) {
            moveTowards(target.position(), distSq > 25.0);
        } else {
            stopMovement();
            lookAtPos(target.getEyePosition());
        }
        return false; // Continuous until told to stop or given a new task
    }

    private boolean tickGotoAction(BotAction action) {
        Vec3 targetVec;
        if (action.x != null && action.y != null && action.z != null) {
            targetVec = new Vec3(action.x + 0.5, action.y, action.z + 0.5);
        } else {
            BotPersistence.SavedLocation savedLoc = BotPersistence.getLocation(action.getPrimaryTarget());
            if (savedLoc != null) {
                targetVec = new Vec3(savedLoc.x + 0.5, savedLoc.y, savedLoc.z + 0.5);
            } else {
                ServerPlayer targetPlayer = findPlayer(action.getPrimaryTarget());
                if (targetPlayer == null) {
                    sayInChat("Couldn't find player or saved location '" + action.getPrimaryTarget() + "'.");
                    return true;
                }
                targetVec = targetPlayer.position();
            }
        }

        double distSq = this.position().distanceToSqr(targetVec);
        boolean isOceanArrival = action.getPrimaryTarget() != null && action.getPrimaryTarget().equalsIgnoreCase("ocean") && this.isInWater();
        if (distSq <= 16.0 || isOceanArrival) {
            stopMovement();
            String destName = action.getPrimaryTarget() != null && !action.getPrimaryTarget().isBlank() ? action.getPrimaryTarget() : "destination";
            sayInChat("Arrived at " + destName + " (" + this.getBlockX() + ", " + this.getBlockY() + ", " + this.getBlockZ() + ")!");
            return true;
        }

        // Progress-based check: never time out on long journeys as long as the bot is making forward progress!
        if (distSq < this.bestGoalDistSq - 1.0) {
            this.bestGoalDistSq = distSq;
            this.noProgressTicks = 0;
        } else {
            this.noProgressTicks++;
        }

        if (this.noProgressTicks > 600) {
            sayInChat("I'm completely blocked from making progress toward my destination.");
            stopMovement();
            return true;
        }
        moveTowards(targetVec, distSq > 16.0);
        return false;
    }

    private boolean tickGuardAction(BotAction action) {
        ServerPlayer guarded = findPlayer(action.getPrimaryTarget());
        BlockPos guardCenter = guarded != null ? guarded.blockPosition() : this.blockPosition();

        Monster threat = findNearestProvokedThreatNear(guardCenter, hasBow() ? 36.0 : 16.0, guarded);
        if (threat != null) {
            engageTarget(threat);
            return false;
        }

        if (guarded != null) {
            double distSq = this.distanceToSqr(guarded);
            if (distSq > 16.0) {
                moveTowards(guarded.position(), distSq > 36.0);
            } else {
                stopMovement();
                lookAtPos(guarded.getEyePosition());
            }
        } else {
            stopMovement();
        }
        return false;
    }

    private boolean tickMineAction(BotAction action) {
        if (this.actionProgressCount >= action.count) {
            stopMovement();
            this.mineZoneCenter = null;
            this.mineZoneRadius = 0;
            sayInChat("Finished mining " + this.actionProgressCount + "x " + action.getPrimaryTarget() + "!");
            return true;
        }

        // First pick up any nearby dropped items
        ItemEntity drop = findNearestDrop(5.0);
        if (drop != null && this.miningTicks == 0) {
            moveTowards(drop.position(), false);
            if (this.distanceToSqr(drop) < 2.5) {
                drop.playerTouch(this);
            }
        }

        // Validate current targetBlockPos
        if (this.targetBlockPos != null) {
            BlockState state = this.level().getBlockState(this.targetBlockPos);
            if (state.isAir() || !matchesBlockQuery(state, action.getPrimaryTarget())) {
                this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
                this.targetBlockPos = null;
                this.miningTicks = 0;
            }
        }

        // Find next matching block if needed
        if (this.targetBlockPos == null) {
            int searchRad = (this.mineZoneCenter != null && this.mineZoneRadius > 0) ? this.mineZoneRadius : BotConfig.SCAN_RADIUS.getAsInt();
            this.targetBlockPos = findNearestMatchingBlock(action.getPrimaryTarget(), searchRad);
            if (this.targetBlockPos == null) {
                stopMovement();
                this.mineZoneCenter = null;
                this.mineZoneRadius = 0;
                if (this.actionProgressCount > 0) {
                    sayInChat("Mined all " + this.actionProgressCount + "x " + action.getPrimaryTarget() + " in the area!");
                } else {
                    sayInChat("I couldn't find any " + action.getPrimaryTarget() + " within " + searchRad + " blocks.");
                }
                return true;
            }
        }

        Vec3 blockCenter = Vec3.atCenterOf(this.targetBlockPos);
        double distSq = this.getEyePosition().distanceToSqr(blockCenter);

        if (distSq > 20.0) {
            // Walk closer to the block
            moveTowards(blockCenter, distSq > 36.0);
            if (this.actionTicks > 500) {
                this.targetBlockPos = null;
                this.actionTicks = 0;
            }
            return false;
        }

        // Within mining reach!
        stopMovement();
        lookAtPos(blockCenter);
        BlockState state = this.level().getBlockState(this.targetBlockPos);
        equipBestToolFor(state);

        this.miningTicks++;
        if (this.miningTicks % 4 == 0) {
            broadcastSwing();
        }

        // Calculate realistic break time (clamped between 6 and 45 ticks for smooth gameplay)
        float destroySpeed = Math.max(1.0f, this.getMainHandItem().getDestroySpeed(state));
        float hardness = Math.max(0.5f, state.getDestroySpeed(this.level(), this.targetBlockPos));
        int ticksRequired = Mth.clamp((int) ((hardness * 20.0f) / destroySpeed), 6, 45);

        int stage = Mth.clamp((this.miningTicks * 10) / ticksRequired, 0, 9);
        this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, stage);

        if (this.miningTicks >= ticksRequired) {
            this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
            BlockPos brokenPos = this.targetBlockPos;
            boolean wasLog = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("log");
            this.level().destroyBlock(brokenPos, true, this);
            // Lumberjack passive: auto-replant sapling if bottom of tree on dirt/grass
            if (wasLog && this.botRole.toLowerCase().contains("lumber")) {
                BlockState below = this.level().getBlockState(brokenPos.below());
                if (below.is(Blocks.DIRT) || below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.PODZOL)) {
                    placeSpecificBlockFromInventory("sapling", brokenPos);
                }
            }
            // Crop auto-replant: replant seed immediately on farmland
            if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) {
                vacuumNearbyDrops();
                BlockState below = this.level().getBlockState(brokenPos.below());
                if (below.is(Blocks.FARMLAND)) {
                    tryReplantCropAt(crop, brokenPos);
                }
            }
            this.targetBlockPos = null;
            this.miningTicks = 0;
            this.actionTicks = 0;
            this.actionProgressCount++;

            // Immediately collect drops spawned at brokenPos
            AABB dropBox = new AABB(brokenPos).inflate(3.5);
            for (ItemEntity itemEntity : this.level().getEntitiesOfClass(ItemEntity.class, dropBox)) {
                if (itemEntity.isAlive()) {
                    ItemStack stack = itemEntity.getItem();
                    if (this.getInventory().add(stack)) {
                        itemEntity.discard();
                    }
                }
            }
        }
        return false;
    }

    private boolean tickExcavateAction(BotAction action) {
        // Skip any already-air, liquid, or unbreakable blocks at the front of the queue
        while (!this.activeExcavationQueue.isEmpty()) {
            BlockPos next = this.activeExcavationQueue.get(0);
            BlockState st = this.level().getBlockState(next);
            if (st.isAir() || !st.getFluidState().isEmpty() || st.getDestroySpeed(this.level(), next) < 0) {
                this.activeExcavationQueue.remove(0);
                this.actionProgressCount++;
                if (this.targetBlockPos != null && this.targetBlockPos.equals(next)) {
                    this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
                    this.targetBlockPos = null;
                    this.miningTicks = 0;
                }
            } else {
                break;
            }
        }

        if (this.activeExcavationQueue.isEmpty()) {
            if (this.targetBlockPos != null) {
                this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
                this.targetBlockPos = null;
            }
            stopMovement();
            vacuumNearbyDrops();
            sayInChat("Finished digging the " + action.getPrimaryTarget() + "! All excavated blocks collected.");
            return true;
        }

        this.targetBlockPos = this.activeExcavationQueue.get(0);
        Vec3 blockCenter = Vec3.atCenterOf(this.targetBlockPos);
        double hdx = blockCenter.x - this.getX();
        double hdz = blockCenter.z - this.getZ();
        double horizDistSq = hdx * hdx + hdz * hdz;

        if (horizDistSq > 20.0) {
            moveTowards(new Vec3(blockCenter.x, this.getY(), blockCenter.z), horizDistSq > 36.0);
            if (this.actionTicks > 200) {
                // Skip unreachable block so excavation never stalls
                this.activeExcavationQueue.remove(0);
                this.targetBlockPos = null;
                this.actionTicks = 0;
                this.miningTicks = 0;
            }
            return false;
        }

        stopMovement();
        lookAtPos(blockCenter);
        BlockState state = this.level().getBlockState(this.targetBlockPos);
        equipBestToolFor(state);

        this.miningTicks++;
        if (this.miningTicks % 4 == 0) {
            broadcastSwing();
        }

        float destroySpeed = Math.max(1.5f, this.getMainHandItem().getDestroySpeed(state));
        float hardness = Math.max(0.4f, state.getDestroySpeed(this.level(), this.targetBlockPos));
        int ticksRequired = Mth.clamp((int) ((hardness * 14.0f) / destroySpeed), 4, 24);

        int stage = Mth.clamp((this.miningTicks * 10) / ticksRequired, 0, 9);
        this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, stage);

        if (this.miningTicks >= ticksRequired) {
            this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
            BlockPos brokenPos = this.targetBlockPos;
            this.level().destroyBlock(brokenPos, true, this);
            this.activeExcavationQueue.remove(0);
            this.targetBlockPos = null;
            this.miningTicks = 0;
            this.actionTicks = 0;
            this.actionProgressCount++;

            AABB dropBox = new AABB(brokenPos).inflate(4.0);
            for (ItemEntity itemEntity : this.level().getEntitiesOfClass(ItemEntity.class, dropBox)) {
                if (itemEntity.isAlive()) {
                    ItemStack stack = itemEntity.getItem();
                    if (this.getInventory().add(stack)) {
                        itemEntity.discard();
                    }
                }
            }
        }
        return false;
    }

    private boolean executeCraftAction(BotAction action) {
        String query = action.getPrimaryTarget().toLowerCase().replace("minecraft:", "").trim();
        int desiredCount = Math.max(1, action.count);
        int craftedTotal = 0;

        for (int i = 0; i < desiredCount; i++) {
            if (craftSingleItemSmart(query)) {
                craftedTotal++;
            } else {
                break;
            }
        }

        if (craftedTotal > 0) {
            sayInChat("Crafted " + craftedTotal + "x " + query + "!");
        } else {
            sayInChat("I don't have enough materials in my inventory to craft " + query + ".");
        }
        return true;
    }

    private boolean craftSingleItemSmart(String query) {
        if (query.contains("plank")) {
            if (consumeMatching(s -> isLog(s), 1)) {
                giveToSelf(new ItemStack(Items.OAK_PLANKS, 4));
                return true;
            }
            return false;
        }
        if (query.contains("stick")) {
            ensurePlanks(2);
            if (consumeMatching(s -> isPlank(s), 2)) {
                giveToSelf(new ItemStack(Items.STICK, 4));
                return true;
            }
            return false;
        }
        if (query.contains("crafting_table") || query.equals("table")) {
            ensurePlanks(4);
            if (consumeMatching(s -> isPlank(s), 4)) {
                giveToSelf(new ItemStack(Items.CRAFTING_TABLE, 1));
                return true;
            }
            return false;
        }
        if (query.contains("torch")) {
            ensureSticks(1);
            if (countMatching(s -> s.is(Items.COAL) || s.is(Items.CHARCOAL)) >= 1 && countMatching(s -> s.is(Items.STICK)) >= 1) {
                consumeMatching(s -> s.is(Items.COAL) || s.is(Items.CHARCOAL), 1);
                consumeMatching(s -> s.is(Items.STICK), 1);
                giveToSelf(new ItemStack(Items.TORCH, 4));
                return true;
            }
            return false;
        }
        if (query.contains("chest")) {
            ensurePlanks(8);
            if (consumeMatching(s -> isPlank(s), 8)) {
                giveToSelf(new ItemStack(Items.CHEST, 1));
                return true;
            }
            return false;
        }
        if (query.contains("furnace")) {
            if (consumeMatching(s -> isCobble(s), 8)) {
                giveToSelf(new ItemStack(Items.FURNACE, 1));
                return true;
            }
            return false;
        }
        if (query.contains("bread")) {
            if (consumeMatching(s -> s.is(Items.WHEAT), 3)) {
                giveToSelf(new ItemStack(Items.BREAD, 1));
                return true;
            }
            return false;
        }
        if (query.contains("hay") || query.contains("bale")) {
            if (consumeMatching(s -> s.is(Items.WHEAT), 9)) {
                giveToSelf(new ItemStack(Items.HAY_BLOCK, 1));
                return true;
            }
            return false;
        }
        if (query.contains("shield")) {
            ensurePlanks(6);
            ensureIronIngots(1);
            if (countMatching(this::isPlank) >= 6 && countMatching(s -> s.is(Items.IRON_INGOT)) >= 1) {
                consumeMatching(this::isPlank, 6);
                consumeMatching(s -> s.is(Items.IRON_INGOT), 1);
                giveToSelf(new ItemStack(Items.SHIELD, 1));
                return true;
            }
            return false;
        }

        // Tools & Weapons: wooden, stone, iron, diamond
        if (query.contains("pickaxe") || query.contains("axe") || query.contains("sword") || query.contains("shovel") || query.contains("hoe")) {
            int sticksNeeded = query.contains("sword") ? 1 : 2;
            int matNeeded = query.contains("pickaxe") || query.contains("axe") ? 3
                    : (query.contains("sword") || query.contains("hoe") ? 2 : 1);

            ensureSticks(sticksNeeded);
            if (countMatching(s -> s.is(Items.STICK)) < sticksNeeded) {
                return false;
            }

            if (query.startsWith("wood")) {
                ensurePlanks(matNeeded);
                if (countMatching(this::isPlank) >= matNeeded) {
                    consumeMatching(this::isPlank, matNeeded);
                    consumeMatching(s -> s.is(Items.STICK), sticksNeeded);
                    Item out = query.contains("pickaxe") ? Items.WOODEN_PICKAXE
                            : query.contains("axe") ? Items.WOODEN_AXE
                            : query.contains("sword") ? Items.WOODEN_SWORD
                            : query.contains("shovel") ? Items.WOODEN_SHOVEL : Items.WOODEN_HOE;
                    giveToSelf(new ItemStack(out, 1));
                    return true;
                }
            } else if (query.startsWith("stone")) {
                if (countMatching(this::isCobble) >= matNeeded) {
                    consumeMatching(this::isCobble, matNeeded);
                    consumeMatching(s -> s.is(Items.STICK), sticksNeeded);
                    Item out = query.contains("pickaxe") ? Items.STONE_PICKAXE
                            : query.contains("axe") ? Items.STONE_AXE
                            : query.contains("sword") ? Items.STONE_SWORD
                            : query.contains("shovel") ? Items.STONE_SHOVEL : Items.STONE_HOE;
                    giveToSelf(new ItemStack(out, 1));
                    return true;
                }
            } else if (query.startsWith("iron")) {
                ensureIronIngots(matNeeded);
                if (countMatching(s -> s.is(Items.IRON_INGOT)) >= matNeeded) {
                    consumeMatching(s -> s.is(Items.IRON_INGOT), matNeeded);
                    consumeMatching(s -> s.is(Items.STICK), sticksNeeded);
                    Item out = query.contains("pickaxe") ? Items.IRON_PICKAXE
                            : query.contains("axe") ? Items.IRON_AXE
                            : query.contains("sword") ? Items.IRON_SWORD
                            : query.contains("shovel") ? Items.IRON_SHOVEL : Items.IRON_HOE;
                    giveToSelf(new ItemStack(out, 1));
                    return true;
                }
            } else if (query.startsWith("diamond")) {
                if (countMatching(s -> s.is(Items.DIAMOND)) >= matNeeded) {
                    consumeMatching(s -> s.is(Items.DIAMOND), matNeeded);
                    consumeMatching(s -> s.is(Items.STICK), sticksNeeded);
                    Item out = query.contains("pickaxe") ? Items.DIAMOND_PICKAXE
                            : query.contains("axe") ? Items.DIAMOND_AXE
                            : query.contains("sword") ? Items.DIAMOND_SWORD
                            : query.contains("shovel") ? Items.DIAMOND_SHOVEL : Items.DIAMOND_HOE;
                    giveToSelf(new ItemStack(out, 1));
                    return true;
                }
            }
        }

        // Armor crafting (iron / diamond)
        if (query.contains("helmet") || query.contains("chestplate") || query.contains("leggings") || query.contains("boots")) {
            int needed = query.contains("helmet") ? 5 : query.contains("chestplate") ? 8 : query.contains("leggings") ? 7 : 4;
            if (query.startsWith("iron")) {
                ensureIronIngots(needed);
                if (countMatching(s -> s.is(Items.IRON_INGOT)) >= needed) {
                    consumeMatching(s -> s.is(Items.IRON_INGOT), needed);
                    Item out = query.contains("helmet") ? Items.IRON_HELMET
                            : query.contains("chestplate") ? Items.IRON_CHESTPLATE
                            : query.contains("leggings") ? Items.IRON_LEGGINGS : Items.IRON_BOOTS;
                    giveToSelf(new ItemStack(out, 1));
                    autoEquipArmor();
                    return true;
                }
            } else if (query.startsWith("diamond")) {
                if (countMatching(s -> s.is(Items.DIAMOND)) >= needed) {
                    consumeMatching(s -> s.is(Items.DIAMOND), needed);
                    Item out = query.contains("helmet") ? Items.DIAMOND_HELMET
                            : query.contains("chestplate") ? Items.DIAMOND_CHESTPLATE
                            : query.contains("leggings") ? Items.DIAMOND_LEGGINGS : Items.DIAMOND_BOOTS;
                    giveToSelf(new ItemStack(out, 1));
                    autoEquipArmor();
                    return true;
                }
            }
        }

        // Check personal and global learned recipes
        List<LearnedRecipe> allKnownRecipes = new ArrayList<>(this.learnedRecipes);
        allKnownRecipes.addAll(BotMemoryManager.getGlobalRecipes());

        for (LearnedRecipe r : allKnownRecipes) {
            if (r.matches(query)) {
                if (canCraftLearnedRecipe(r)) {
                    craftLearnedRecipe(r);
                    return true;
                }
            }
        }

        return false;
    }

    private void ensurePlanks(int needed) {
        while (countMatching(this::isPlank) < needed && countMatching(this::isLog) > 0) {
            if (consumeMatching(this::isLog, 1)) {
                giveToSelf(new ItemStack(Items.OAK_PLANKS, 4));
            } else {
                break;
            }
        }
    }

    private void ensureSticks(int needed) {
        while (countMatching(s -> s.is(Items.STICK)) < needed) {
            ensurePlanks(2);
            if (consumeMatching(this::isPlank, 2)) {
                giveToSelf(new ItemStack(Items.STICK, 4));
            } else {
                break;
            }
        }
    }

    private void ensureIronIngots(int needed) {
        while (countMatching(s -> s.is(Items.IRON_INGOT)) < needed
                && countMatching(s -> s.is(Items.RAW_IRON) || s.is(Items.IRON_ORE) || s.is(Items.DEEPSLATE_IRON_ORE)) > 0) {
            if (consumeMatching(s -> s.is(Items.RAW_IRON) || s.is(Items.IRON_ORE) || s.is(Items.DEEPSLATE_IRON_ORE), 1)) {
                giveToSelf(new ItemStack(Items.IRON_INGOT, 1));
            } else {
                break;
            }
        }
    }

    public int compactWheatInInventory() {
        int wheatCount = countMatching(s -> s.is(Items.WHEAT));
        int blocksToCraft = wheatCount / 9;
        if (blocksToCraft <= 0) return 0;
        int crafted = 0;
        for (int i = 0; i < blocksToCraft; i++) {
            if (consumeMatching(s -> s.is(Items.WHEAT), 9)) {
                giveToSelf(new ItemStack(Items.HAY_BLOCK, 1));
                crafted++;
            } else {
                break;
            }
        }
        return crafted;
    }

    public int uncompactHayInInventory() {
        int hayCount = countMatching(s -> s.is(Items.HAY_BLOCK));
        if (hayCount <= 0) return 0;
        int uncrafted = 0;
        for (int i = 0; i < hayCount; i++) {
            if (consumeMatching(s -> s.is(Items.HAY_BLOCK), 1)) {
                giveToSelf(new ItemStack(Items.WHEAT, 9));
                uncrafted++;
            } else {
                break;
            }
        }
        return uncrafted;
    }

    public boolean canCraftLearnedRecipe(LearnedRecipe recipe) {
        if (recipe == null || recipe.ingredients.isEmpty()) return false;
        for (Map.Entry<String, Integer> ing : recipe.ingredients.entrySet()) {
            int needed = ing.getValue();
            int has = countMatching(s -> isMatchingIngredient(s, ing.getKey()));
            if (has < needed) return false;
        }
        return true;
    }

    public void craftLearnedRecipe(LearnedRecipe recipe) {
        if (recipe == null) return;
        for (Map.Entry<String, Integer> ing : recipe.ingredients.entrySet()) {
            consumeMatching(s -> isMatchingIngredient(s, ing.getKey()), ing.getValue());
        }
        ItemStack result = recipe.createResult();
        if (result != null && !result.isEmpty()) {
            giveToSelf(result);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private boolean isMatchingIngredient(ItemStack stack, String ingredientQuery) {
        if (stack == null || stack.isEmpty() || ingredientQuery == null) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        String clean = ingredientQuery.toLowerCase(Locale.ROOT).replace("minecraft:", "").replace(" ", "_");
        if (id.equals(clean)) return true;
        if (clean.equals("wheat") && id.contains("wheat")) return true;
        if (clean.equals("plank") || clean.equals("planks") || clean.equals("wood")) return isPlank(stack);
        if (clean.equals("stick") && stack.is(Items.STICK)) return true;
        if (clean.equals("iron") || clean.equals("iron_ingot")) return stack.is(Items.IRON_INGOT);
        if (clean.equals("gold") || clean.equals("gold_ingot")) return stack.is(Items.GOLD_INGOT);
        if (clean.equals("diamond") || clean.equals("diamonds")) return stack.is(Items.DIAMOND);
        if (clean.equals("stone") || clean.equals("cobblestone")) return isCobble(stack) || stack.is(Items.STONE);
        if (clean.equals("coal")) return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
        return id.contains(clean) || clean.contains(id);
    }

    private boolean tickGiveAction(BotAction action) {
        ServerPlayer recipient = findPlayer(action.target);
        if (recipient == null) {
            // Fallback to nearest human player
            Player nearest = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 32.0, p -> p != this && !(p instanceof AiBotPlayer));
            if (nearest instanceof ServerPlayer sp) {
                recipient = sp;
            }
        }
        if (recipient == null) {
            sayInChat("I can't find anyone nearby to give items to.");
            return true;
        }

        double distSq = this.distanceToSqr(recipient);
        if (distSq > 9.0) {
            moveTowards(recipient.position(), distSq > 25.0);
            if (this.actionTicks > 400) {
                sayInChat("Couldn't reach " + recipient.getPlainTextName() + " to give items.");
                stopMovement();
                return true;
            }
            return false;
        }

        stopMovement();
        lookAtPos(recipient.getEyePosition());

        String itemQuery = (action.item != null && !action.item.isBlank()) ? action.item : action.target;
        String cleanQuery = itemQuery.toLowerCase().replace("minecraft:", "").trim();
        int remaining = Math.max(1, action.count);
        int droppedTotal = 0;

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase();
            if ("all".equals(cleanQuery) || id.contains(cleanQuery)) {
                int toTake = "all".equals(cleanQuery) ? stack.getCount() : Math.min(remaining, stack.getCount());
                ItemStack split = this.getInventory().removeItem(i, toTake);
                if (!split.isEmpty()) {
                    tossStackTowards(split, recipient.position());
                    droppedTotal += split.getCount();
                    remaining -= split.getCount();
                    if (!"all".equals(cleanQuery) && remaining <= 0) {
                        break;
                    }
                }
            }
        }

        if (droppedTotal > 0) {
            broadcastSwing();
            sayInChat("Here you go, " + recipient.getPlainTextName() + "! Dropped " + droppedTotal + "x " + cleanQuery + ".");
        } else {
            sayInChat("I don't have any " + cleanQuery + " in my inventory.");
        }
        return true;
    }

    private void tossStackTowards(ItemStack stack, Vec3 targetPos) {
        ItemEntity itemEntity = this.createItemStackToDrop(stack, false, true);
        if (itemEntity != null) {
            Vec3 dir = targetPos.add(0, 0.5, 0).subtract(this.getEyePosition()).normalize().scale(0.32);
            itemEntity.setDeltaMovement(dir);
            itemEntity.setDefaultPickUpDelay();
            this.level().addFreshEntity(itemEntity);
        }
    }

    private boolean tickAttackAction(BotAction action) {
        if (this.actionProgressCount >= action.count) {
            stopMovement();
            sayInChat("Finished eliminating " + this.actionProgressCount + "x " + action.getPrimaryTarget() + "!");
            return true;
        }

        String query = action.getPrimaryTarget().toLowerCase().replace("minecraft:", "").trim();
        LivingEntity targetMob = findNearestTargetMob(query, 48.0);
        if (targetMob == null) {
            stopMovement();
            if (this.actionProgressCount > 0) {
                sayInChat("Cleared " + this.actionProgressCount + "x " + query + ", area is clear!");
            } else {
                sayInChat("No " + (query.isEmpty() ? "hostile mobs" : query) + " found nearby.");
            }
            return true;
        }

        boolean wasAlive = targetMob.isAlive();
        engageTarget(targetMob);
        if (wasAlive && !targetMob.isAlive()) {
            this.actionProgressCount++;
        }
        return false;
    }

    private void engageTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            if (this.isUsingItem()) this.stopUsingItem();
            this.currentCombatTargetId = -1;
            this.combatTargetEngageTicks = 0;
            this.bowChargeTicks = 0;
            this.setLastHurtByMob(null);
            return;
        }

        double distSq = this.distanceToSqr(target);

        // Tether constraint enforcement during combat: never wander beyond tether radius!
        if (this.tetherPos != null) {
            Vec3 tetherCenter = Vec3.atCenterOf(this.tetherPos);
            double myDistToTetherSq = this.distanceToSqr(tetherCenter);
            if (myDistToTetherSq > (this.tetherRadius * this.tetherRadius)) {
                if (this.isUsingItem()) this.stopUsingItem();
                this.bowChargeTicks = 0;
                moveTowards(tetherCenter, true);
                return;
            }
            double targetDistToTetherSq = target.distanceToSqr(tetherCenter);
            if (targetDistToTetherSq > (this.tetherRadius * this.tetherRadius)) {
                if (!hasBowAndArrows() || distSq > 625.0) { // 25m max tether archery range
                    if (this.isUsingItem()) this.stopUsingItem();
                    this.currentCombatTargetId = -1;
                    this.combatTargetEngageTicks = 0;
                    this.bowChargeTicks = 0;
                    this.setLastHurtByMob(null);
                    moveTowards(tetherCenter, false);
                    return;
                }
            }
        }

        if (target.getId() != this.currentCombatTargetId) {
            this.currentCombatTargetId = target.getId();
            this.combatTargetEngageTicks = 0;
            this.bestCombatTargetDistSq = distSq;
        } else {
            this.combatTargetEngageTicks++;
            if (distSq < this.bestCombatTargetDistSq - 0.75) {
                this.bestCombatTargetDistSq = distSq;
                this.combatTargetEngageTicks = Math.max(0, this.combatTargetEngageTicks - 8);
            }
        }

        double hdx = target.getX() - this.getX();
        double hdz = target.getZ() - this.getZ();
        double horizDistSq = hdx * hdx + hdz * hdz;
        double vertDiff = Math.abs(target.getY() - this.getY());

        // Timeout or underground spin check: if standing directly above/below a mob without line of sight, or unable to reach after 80 ticks (4s) for melee (or 240 ticks for archers)
        boolean hasBow = hasBow();
        int maxCombatEngageTicks = hasBow ? 240 : 80;
        if ((horizDistSq < 3.5 && vertDiff > 2.5 && !this.hasLineOfSight(target)) || this.combatTargetEngageTicks > maxCombatEngageTicks) {
            this.unreachableMobBlacklist.put(target.getId(), this.tickCount + 200);
            this.currentCombatTargetId = -1;
            this.combatTargetEngageTicks = 0;
            this.bowChargeTicks = 0;
            this.setLastHurtByMob(null);
            if (this.isUsingItem()) this.stopUsingItem();
            stopMovement();
            return;
        }

        lookAtPos(target.getEyePosition());

        // Archery vs Melee Decision & Fence Boundary Enforcement
        boolean fenceBetween = isLineBlockedByFence(this.position(), target.position());
        boolean preferBow = hasBow && (distSq > 9.0 || !hasMeleeWeapon() || fenceBetween || this.sentryMode);

        if (preferBow) {
            if (this.isUsingItem() && this.getOffhandItem().is(Items.SHIELD)) {
                this.stopUsingItem();
            }
            equipBow();
            lookAtPos(target.getEyePosition());

            // Archery positioning
            if (this.sentryMode) {
                if (this.sentryPos != null && this.distanceToSqr(Vec3.atCenterOf(this.sentryPos)) > 1.5) {
                    moveTowards(Vec3.atCenterOf(this.sentryPos), true);
                } else {
                    stopMovement();
                }
            } else if (fenceBetween) {
                // Strict Fence Boundary: never move towards target across a fence! Stay behind perimeter.
                stopMovement();
            } else if (this.tetherPos != null) {
                Vec3 tetherCenter = Vec3.atCenterOf(this.tetherPos);
                if (this.distanceToSqr(tetherCenter) > (this.tetherRadius * this.tetherRadius) * 0.8) {
                    moveTowards(tetherCenter, false);
                } else {
                    stopMovement();
                }
            } else {
                if (distSq < 20.25) {
                    Vec3 away = this.position().subtract(target.position()).normalize().scale(2.0);
                    moveTowards(this.position().add(away), false);
                } else if (distSq > 2304.0) {
                    moveTowards(target.position(), false);
                } else {
                    stopMovement();
                }
            }

            if (!this.isUsingItem()) {
                this.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
                this.bowChargeTicks = 0;
            } else {
                this.bowChargeTicks++;
                if (this.bowChargeTicks >= 20) {
                    if (canShootAtTarget(target)) {
                        this.stopUsingItem();
                        Vec3 eyePos = this.getEyePosition(1.0F);
                        double dx = target.getX() - eyePos.x;
                        double horizDist = Math.sqrt(dx * dx + (target.getZ() - eyePos.z) * (target.getZ() - eyePos.z));
                        double dy = (target.getY() + target.getEyeHeight() * 0.5) - eyePos.y + (horizDist * 0.055 + (horizDist * horizDist * 0.0012));
                        double dz = target.getZ() - eyePos.z;

                        ItemStack arrowStack = getArrowStackForFiring();
                        net.minecraft.world.entity.projectile.arrow.Arrow arrow =
                                new net.minecraft.world.entity.projectile.arrow.Arrow(this.level(), this, arrowStack, this.getMainHandItem());
                        float inaccuracy = this.sentryMode ? 0.3F : 0.8F;
                        arrow.shoot(dx, dy, dz, 2.9F, inaccuracy);
                        this.level().addFreshEntity(arrow);
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                net.minecraft.sounds.SoundEvents.ARROW_SHOOT,
                                net.minecraft.sounds.SoundSource.PLAYERS,
                                1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 1.2F));

                        consumeArrow();
                        this.bowChargeTicks = 0;
                        this.attackCooldown = 10;
                        this.combatTargetEngageTicks = 0;
                    }
                }
            }
            return;
        }

        // If we were drawing a bow and target closed into melee range, stop bow draw
        if (this.isUsingItem() && isBowItem(this.getMainHandItem())) {
            this.stopUsingItem();
            this.bowChargeTicks = 0;
        }

        // Fence boundary check for melee fighters: NEVER cross fences or open gates!
        if (fenceBetween) {
            if (distSq <= 9.0) {
                // Adjacent across fence: hit over the fence
                equipBestMeleeWeapon();
                lookAtPos(target.getEyePosition());
                if (this.attackCooldown <= 0) {
                    broadcastSwing();
                    this.attack(target);
                    this.attackCooldown = 12;
                }
            } else {
                stopMovement();
            }
            return;
        }

        // Sentry mode guard without bow: never leave post
        if (this.sentryMode) {
            if (distSq <= 9.0) {
                equipBestMeleeWeapon();
                lookAtPos(target.getEyePosition());
                if (this.attackCooldown <= 0) {
                    broadcastSwing();
                    this.attack(target);
                    this.attackCooldown = 12;
                }
            } else {
                stopMovement();
            }
            return;
        }

        // 1. Tactical Shield Check (creeper explosions, skeleton bow drawing)
        boolean shouldShield = false;
        if (target instanceof net.minecraft.world.entity.monster.Creeper creeper) {
            if (creeper.getSwellDir() > 0 || distSq < 9.0) {
                shouldShield = true;
            }
        } else if (target.isUsingItem()) {
            shouldShield = true;
        }

        if (shouldShield && this.getOffhandItem().is(Items.SHIELD)) {
            if (!this.isUsingItem()) {
                this.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
            }
            this.shieldBlockTicks = 15;
            moveTowards(target.position(), false);
            return;
        }

        // Drop shield before striking
        if (this.isUsingItem() && this.getOffhandItem().is(Items.SHIELD) && this.shieldBlockTicks <= 0) {
            this.stopUsingItem();
        }

        // 2. Weapon & Range Handling
        equipBestMeleeWeapon();

        if (distSq > 9.5) {
            Vec3 moveGoal = target.position();
            if (this.tetherPos != null && moveGoal.distanceToSqr(Vec3.atCenterOf(this.tetherPos)) > (this.tetherRadius * this.tetherRadius)) {
                stopMovement();
                return;
            }
            // Leverage the mob's own vanilla A* PathNavigation in reverse to route around corners/obstacles!
            if (!this.hasLineOfSight(target) && target instanceof net.minecraft.world.entity.Mob mob) {
                net.minecraft.world.level.pathfinder.Path path = mob.getNavigation().createPath(this.blockPosition(), 0);
                if (path != null && path.getNodeCount() >= 2) {
                    int idx = Math.max(0, path.getNodeCount() - 2);
                    net.minecraft.world.level.pathfinder.Node stepNode = path.getNode(idx);
                    moveGoal = new Vec3(stepNode.x + 0.5, stepNode.y, stepNode.z + 0.5);
                }
            }
            moveTowards(moveGoal, true);
        } else {
            // In melee striking range!
            // Tactical Sprint-Crit: jump right before attack to trigger player-grade critical hits
            if (this.onGround() && this.attackCooldown <= 1) {
                this.jumpFromGround();
                this.setDeltaMovement(this.getDeltaMovement().add(
                        (target.getX() - this.getX()) * 0.15,
                        0.32,
                        (target.getZ() - this.getZ()) * 0.15
                ));
            }

            if (this.attackCooldown <= 0) {
                if (!this.onGround() && this.getDeltaMovement().y < 0) {
                    this.fallDistance = 0.5;
                }
                broadcastSwing();
                this.attack(target);
                this.resetAttackStrengthTicker();
                this.attackCooldown = 11;
                this.combatTargetEngageTicks = 0;
            }
        }
    }

    private boolean executePlaceAction(BotAction action) {
        String target = action.getPrimaryTarget().toLowerCase().replace("minecraft:", "").trim();
        BlockPos origin = this.blockPosition();

        if (target.contains("cottage") || target.contains("house") || target.contains("home")) {
            startHouseConstruction(origin.relative(this.getDirection(), 2), this.getDirection());
            return true;
        }

        if (target.contains("shelter") || target.contains("hut")) {
            int placed = buildQuickShelter(origin);
            if (placed > 0) {
                sayInChat("Built a protective shelter (" + placed + " blocks placed)!");
            } else {
                sayInChat("I need building blocks (cobblestone, planks, or dirt) in my inventory to build a shelter.");
            }
            return true;
        }

        if (target.contains("wall")) {
            int placed = 0;
            BlockPos front = origin.relative(this.getDirection(), 2);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos p = this.getDirection().getAxis() == net.minecraft.core.Direction.Axis.Z
                            ? front.offset(dx, dy, 0)
                            : front.offset(0, dy, dx);
                    if (placeBuildingBlockAt(p)) placed++;
                }
            }
            sayInChat(placed > 0 ? "Built a defensive wall!" : "Out of building blocks.");
            return true;
        }

        if (target.contains("pillar")) {
            BlockPos below = origin;
            this.setPos(this.getX(), this.getY() + 1.1, this.getZ());
            if (placeBuildingBlockAt(below)) {
                sayInChat("Pillared up!");
            }
            return true;
        }

        // Multi-block seed / crop planting onto farmland
        if (target.contains("seed") || target.contains("crop") || target.contains("wheat") || target.contains("carrot") || target.contains("potato") || target.contains("beetroot")) {
            int count = action.count > 0 ? action.count : 32;
            int planted = plantSeedsOnNearbyFarmland(count);
            if (planted > 0) {
                sayInChat("Planted " + planted + "x seeds into the farmland!");
            } else {
                sayInChat("Couldn't find any empty farmland nearby (or I need seeds in my inventory).");
            }
            return true;
        }

        // Single block placement (torch, crafting_table, chest, furnace, etc.)
        BlockPos placePos = origin.relative(this.getDirection(), 1);
        if (!this.level().getBlockState(placePos).isAir()) {
            placePos = origin.above();
        }
        if (placeSpecificBlockFromInventory(target, placePos)) {
            broadcastSwing();
            sayInChat("Placed " + target + " at (" + placePos.getX() + ", " + placePos.getY() + ", " + placePos.getZ() + ").");
        } else {
            sayInChat("I couldn't place " + target + " (check my inventory or space around me).");
        }
        return true;
    }

    private int buildQuickShelter(BlockPos center) {
        int placed = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    boolean isWall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    boolean isRoof = dy == 3;
                    boolean isDoorway = (dx == 0 && dz == 2 && (dy == 0 || dy == 1));
                    if ((isWall || isRoof) && !isDoorway) {
                        if (placeBuildingBlockAt(center.offset(dx, dy, dz))) {
                            placed++;
                        }
                    }
                }
            }
        }
        placeSpecificBlockFromInventory("torch", center.offset(1, 0, 1));
        return placed;
    }

    private boolean placeBuildingBlockAt(BlockPos pos) {
        BlockState targetState = this.level().getBlockState(pos);
        if (!targetState.isAir() && !targetState.canBeReplaced()) {
            return false;
        }
        // Fences and walls are strict boundaries: NEVER replace or place blocks onto or above fences/walls!
        if (isFenceOrWall(targetState) || isFenceOrWall(this.level().getBlockState(pos.below()))) {
            return false;
        }
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                Block b = bi.getBlock();
                String id = BuiltInRegistries.BLOCK.getKey(b).toString();
                if (id.contains("planks") || id.contains("cobblestone") || id.contains("stone") || id.contains("dirt") || id.contains("log") || id.contains("bricks")) {
                    this.level().setBlockAndUpdate(pos, b.defaultBlockState());
                    s.shrink(1);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean placeSpecificBlockFromInventory(String query, BlockPos pos) {
        if (!this.level().getBlockState(pos).isAir() && !this.level().getBlockState(pos).canBeReplaced()) {
            return false;
        }
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
                if (id.contains(query)) {
                    this.level().setBlockAndUpdate(pos, bi.getBlock().defaultBlockState());
                    s.shrink(1);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tickChestAction(BotAction action, boolean store) {
        BlockPos chestPos = store ? findNearestAvailableContainerBlock(16) : findNearestContainerBlock(16);
        if (chestPos == null) {
            sayInChat(store ? "I couldn't find an available chest with free space nearby." : "I couldn't find a chest or barrel nearby.");
            return true;
        }

        double distSq = this.position().distanceToSqr(Vec3.atCenterOf(chestPos));
        if (distSq > 16.0) {
            moveTowards(Vec3.atCenterOf(chestPos), false);
            if (this.actionTicks > 300) {
                stopMovement();
                sayInChat("Couldn't reach the chest.");
                return true;
            }
            return false;
        }

        stopMovement();
        lookAtPos(Vec3.atCenterOf(chestPos));
        Container container = getContainerAt(chestPos);
        if (container == null) {
            sayInChat("That block isn't a valid container.");
            return true;
        }

        String query = action.getPrimaryTarget().toLowerCase().replace("minecraft:", "").trim();
        int moved = 0;

        if (store) {
            for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
                ItemStack s = this.getInventory().getItem(i);
                if (s.isEmpty()) continue;
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
                // Keep main tools/weapons/armor/shields unless storing a specific item
                if (("all".equals(query) || query.isEmpty()) && (id.contains("pickaxe") || id.contains("sword") || id.contains("axe") || id.contains("hoe") || id.contains("shovel") || id.contains("shield") || id.contains("helmet") || id.contains("chestplate") || id.contains("leggings") || id.contains("boots"))) {
                    continue;
                }
                // Keep food rations (e.g. bread, cooked meat, apples) for bot survival
                if (("all".equals(query) || query.isEmpty()) && (id.contains("bread") || id.contains("apple") || id.contains("cooked"))) {
                    int rationCount = countMatching(item -> {
                        String k = BuiltInRegistries.ITEM.getKey(item.getItem()).toString().toLowerCase();
                        return k.contains("bread") || k.contains("apple") || k.contains("cooked");
                    });
                    if (rationCount <= 16) continue;
                }
                // Keep seeds & planting supplies for farmer role
                if (("all".equals(query) || query.isEmpty()) && this.botRole.toLowerCase().contains("farm")) {
                    if (id.contains("seed")) {
                        int seedCount = countMatching(item -> BuiltInRegistries.ITEM.getKey(item.getItem()).toString().contains("seed"));
                        if (seedCount <= 32) continue;
                    }
                    if (id.contains("bone_meal") || id.contains("bonemeal")) {
                        continue;
                    }
                    if (id.contains("carrot")) {
                        int cCount = countMatching(item -> BuiltInRegistries.ITEM.getKey(item.getItem()).toString().contains("carrot"));
                        if (cCount <= 16) continue;
                    }
                    if (id.contains("potato")) {
                        int pCount = countMatching(item -> BuiltInRegistries.ITEM.getKey(item.getItem()).toString().contains("potato"));
                        if (pCount <= 16) continue;
                    }
                }
                // Keep breeding food for husbandry/rancher role
                if (("all".equals(query) || query.isEmpty()) && (this.botRole.toLowerCase().contains("ranch") || this.botRole.toLowerCase().contains("husbandry"))) {
                    if (id.contains(this.ranchFood) || id.contains("wheat") || id.contains("carrot") || id.contains("seed")) {
                        int foodCount = countMatching(item -> BuiltInRegistries.ITEM.getKey(item.getItem()).toString().contains(this.ranchFood));
                        if (foodCount <= 32) continue;
                    }
                }
                boolean matchesStore = "all".equals(query) || query.isEmpty() || id.contains(query);
                if ("drops".equals(query)) {
                    matchesStore = (id.contains("beef") || id.contains("pork") || id.contains("mutton")
                            || id.contains("chicken") || id.contains("leather") || id.contains("wool") || id.contains("feather"))
                            && !id.contains("cooked");
                }
                if (matchesStore) {
                    int countBefore = s.getCount();
                    ItemStack remainder = insertIntoContainer(container, s.copy());
                    int inserted = countBefore - remainder.getCount();
                    if (inserted > 0) {
                        s.shrink(inserted);
                        moved += inserted;
                    }
                }
            }
            if (moved > 0) {
                sayInChat("Stored " + moved + " items into the chest.");
            }
        } else {
            int remaining = Math.max(1, action.count);
            moved = takeItemsFromContainer(container, query, remaining);
            sayInChat("Took " + moved + "x " + query + " from the chest.");
        }
        return true;
    }

    public int takeItemsFromContainer(Container container, String query, int maxCount) {
        String q = query.toLowerCase().replace("minecraft:", "").trim();
        int moved = 0;
        int remaining = maxCount;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
            if ("all".equals(q) || id.contains(q)) {
                int take = Math.min(remaining, s.getCount());
                ItemStack taken = container.removeItem(i, take);
                if (this.getInventory().add(taken)) {
                    moved += take;
                    remaining -= take;
                    if (remaining <= 0) break;
                }
            }
        }
        container.setChanged();
        return moved;
    }

    private ItemStack insertIntoContainer(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                container.setItem(i, stack.copy());
                stack.setCount(0);
                break;
            } else if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int transfer = Math.min(space, stack.getCount());
                slot.grow(transfer);
                stack.shrink(transfer);
            }
        }
        container.setChanged();
        return stack;
    }

    // === FARMING & CROP AUTOMATION ENGINE ===

    private boolean tryReplantCropAt(net.minecraft.world.level.block.CropBlock crop, BlockPos pos) {
        Item needed = Items.WHEAT_SEEDS;
        if (crop == Blocks.CARROTS) needed = Items.CARROT;
        else if (crop == Blocks.POTATOES) needed = Items.POTATO;
        else if (crop == Blocks.BEETROOTS) needed = Items.BEETROOT_SEEDS;

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(needed) && isCropAllowed(crop)) {
                stack.shrink(1);
                this.level().setBlockAndUpdate(pos, crop.getStateForAge(0));
                broadcastSwing();
                return true;
            }
        }
        return false;
    }

    public boolean tryPlantSeedOnFarmland(BlockPos farmlandPos) {
        BlockPos cropPos = farmlandPos.above();
        if (!this.level().getBlockState(cropPos).isAir()) return false;
        if (!this.level().getBlockState(farmlandPos).is(Blocks.FARMLAND)) return false;

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (stack.isEmpty() || !isSeedAllowed(stack)) continue;
            net.minecraft.world.level.block.CropBlock crop = null;
            if (stack.is(Items.WHEAT_SEEDS)) crop = (net.minecraft.world.level.block.CropBlock) Blocks.WHEAT;
            else if (stack.is(Items.CARROT)) crop = (net.minecraft.world.level.block.CropBlock) Blocks.CARROTS;
            else if (stack.is(Items.POTATO)) crop = (net.minecraft.world.level.block.CropBlock) Blocks.POTATOES;
            else if (stack.is(Items.BEETROOT_SEEDS)) crop = (net.minecraft.world.level.block.CropBlock) Blocks.BEETROOTS;

            if (crop != null && isCropAllowed(crop)) {
                stack.shrink(1);
                this.level().setBlockAndUpdate(cropPos, crop.getStateForAge(0));
                broadcastSwing();
                return true;
            }
        }
        return false;
    }

    private int plantSeedsOnNearbyFarmland(int maxCount) {
        BlockPos origin = this.farmOriginPos != null ? this.farmOriginPos : this.blockPosition();
        int planted = 0;
        List<BlockPos> bareFarmland = new ArrayList<>();
        int rx = this.farmOriginPos != null ? this.farmRadiusX : 16;
        int rz = this.farmOriginPos != null ? this.farmRadiusZ : 16;

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (isInsideFarmArea(p) && this.level().getBlockState(p).is(Blocks.FARMLAND) && this.level().getBlockState(p.above()).isAir()) {
                        bareFarmland.add(p.immutable());
                    }
                }
            }
        }
        bareFarmland.sort(Comparator.comparingDouble(p -> this.distanceToSqr(Vec3.atCenterOf(p))));

        for (BlockPos farmPos : bareFarmland) {
            if (planted >= maxCount) break;
            if (tryPlantSeedOnFarmland(farmPos)) {
                planted++;
            }
        }
        return planted;
    }

    private @Nullable BlockPos findNearestMatureCrop(int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        if (this.assignedAreaName != null && !this.assignedAreaName.isBlank()) {
            com.aicompanion.bot.area.SavedArea area = BotPersistence.getArea(this.assignedAreaName);
            if (area != null) {
                int minX = area.getMinX();
                int maxX = area.getMaxX();
                int minZ = area.getMinZ();
                int maxZ = area.getMaxZ();
                int minY = area.minY;
                int maxY = area.maxY;
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (!area.contains(p)) continue;
                            BlockState s = this.level().getBlockState(p);
                            if (s.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop && crop.isMaxAge(s)) {
                                if (isCropAllowed(crop)) {
                                    double d = this.distanceToSqr(Vec3.atCenterOf(p));
                                    if (d < bestDist) {
                                        bestDist = d;
                                        best = p.immutable();
                                    }
                                }
                            }
                        }
                    }
                }
                return best;
            }
        }

        BlockPos center = this.farmOriginPos != null ? this.farmOriginPos : this.blockPosition();
        int rx = this.farmOriginPos != null ? this.farmRadiusX : radius;
        int rz = this.farmOriginPos != null ? this.farmRadiusZ : radius;

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (!isInsideFarmArea(p)) continue;
                    BlockState s = this.level().getBlockState(p);
                    if (s.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop && crop.isMaxAge(s)) {
                        if (isCropAllowed(crop)) {
                            double d = this.distanceToSqr(Vec3.atCenterOf(p));
                            if (d < bestDist) {
                                bestDist = d;
                                best = p.immutable();
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private @Nullable BlockPos findNearestBareFarmland(int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        if (this.assignedAreaName != null && !this.assignedAreaName.isBlank()) {
            com.aicompanion.bot.area.SavedArea area = BotPersistence.getArea(this.assignedAreaName);
            if (area != null) {
                int minX = area.getMinX();
                int maxX = area.getMaxX();
                int minZ = area.getMinZ();
                int maxZ = area.getMaxZ();
                int minY = area.minY;
                int maxY = area.maxY;
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (!area.contains(p)) continue;
                            if (this.level().getBlockState(p).is(Blocks.FARMLAND) && this.level().getBlockState(p.above()).isAir()) {
                                double d = this.distanceToSqr(Vec3.atCenterOf(p));
                                if (d < bestDist) {
                                    bestDist = d;
                                    best = p.immutable();
                                }
                            }
                        }
                    }
                }
                return best;
            }
        }

        BlockPos center = this.farmOriginPos != null ? this.farmOriginPos : this.blockPosition();
        int rx = this.farmOriginPos != null ? this.farmRadiusX : radius;
        int rz = this.farmOriginPos != null ? this.farmRadiusZ : radius;

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (!isInsideFarmArea(p)) continue;
                    if (this.level().getBlockState(p).is(Blocks.FARMLAND) && this.level().getBlockState(p.above()).isAir()) {
                        double d = this.distanceToSqr(Vec3.atCenterOf(p));
                        if (d < bestDist) {
                            bestDist = d;
                            best = p.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private @Nullable BlockPos findNearestTillableDirtInFarmBox() {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        if (this.assignedAreaName != null && !this.assignedAreaName.isBlank()) {
            com.aicompanion.bot.area.SavedArea area = BotPersistence.getArea(this.assignedAreaName);
            if (area != null) {
                int minX = area.getMinX();
                int maxX = area.getMaxX();
                int minZ = area.getMinZ();
                int maxZ = area.getMaxZ();
                int minY = area.minY;
                int maxY = area.maxY;
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (!area.contains(p)) continue;
                            BlockState state = this.level().getBlockState(p);
                            if ((state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.COARSE_DIRT))
                                    && this.level().getBlockState(p.above()).isAir()) {
                                double d = this.distanceToSqr(Vec3.atCenterOf(p));
                                if (d < bestDist) {
                                    bestDist = d;
                                    best = p.immutable();
                                }
                            }
                        }
                    }
                }
                return best;
            }
        }

        if (this.farmOriginPos == null) return null;
        BlockPos origin = this.farmOriginPos;

        for (int dx = -this.farmRadiusX; dx <= this.farmRadiusX; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -this.farmRadiusZ; dz <= this.farmRadiusZ; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (!isInsideFarmArea(p)) continue;
                    BlockState state = this.level().getBlockState(p);
                    if ((state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.COARSE_DIRT))
                            && this.level().getBlockState(p.above()).isAir()) {
                        double d = this.distanceToSqr(Vec3.atCenterOf(p));
                        if (d < bestDist) {
                            bestDist = d;
                            best = p.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean tryTillDirtInFarmBox() {
        if (this.farmOriginPos == null || !this.farmAllowTill) return false;

        ItemStack hoeStack = ItemStack.EMPTY;
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("hoe")) {
                hoeStack = s;
                break;
            }
        }

        if (hoeStack.isEmpty()) {
            BlockPos chestPos = findNearestContainerBlock(20);
            if (chestPos != null && this.level().getBlockEntity(chestPos) instanceof Container c) {
                int taken = takeItemsFromContainer(c, "hoe", 1);
                if (taken > 0) {
                    sayInChat("Grabbed a hoe from the chest to till farm land.");
                }
            }
            return false;
        }

        BlockPos tillTarget = findNearestTillableDirtInFarmBox();
        if (tillTarget == null) return false;

        double distSq = this.distanceToSqr(Vec3.atCenterOf(tillTarget));
        if (distSq > 9.0) {
            moveTowards(Vec3.atCenterOf(tillTarget), false);
            return true;
        }

        stopMovement();
        lookAtPos(Vec3.atCenterOf(tillTarget));
        this.level().setBlockAndUpdate(tillTarget, Blocks.FARMLAND.defaultBlockState());
        this.level().playSound(null, tillTarget, SoundEvents.HOE_TILL.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
        broadcastSwing();

        if (hoeStack.isDamageableItem()) {
            hoeStack.setDamageValue(hoeStack.getDamageValue() + 1);
            if (hoeStack.getDamageValue() >= hoeStack.getMaxDamage()) {
                hoeStack.shrink(1);
            }
        }

        tryPlantSeedOnFarmland(tillTarget);
        return true;
    }

    private boolean hasAnySeeds() {
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && isSeedAllowed(s)) {
                return true;
            }
        }
        return false;
    }

    private boolean tickFarmAction(BotAction action) {
        boolean isAreaFarming = this.assignedAreaName != null && !this.assignedAreaName.isBlank();
        int goal = action.count > 0 ? action.count : (isAreaFarming ? Integer.MAX_VALUE : 20);
        if (this.actionProgressCount >= goal) {
            stopMovement();
            BlockPos chestPos = findNearestAvailableContainerBlock(24);
            if (chestPos != null) {
                depositFarmerCropsIntoChest(chestPos);
            }
            sayInChat("Farm duty complete! Harvested " + this.actionProgressCount + "x crops and replanted seeds.");
            return true;
        }

        BlockPos matureCrop = findNearestMatureCrop(this.farmRadiusX);
        if (matureCrop != null) {
            double distSq = this.position().distanceToSqr(Vec3.atCenterOf(matureCrop));
            if (distSq > 9.0) {
                moveTowards(Vec3.atCenterOf(matureCrop), false);
                return false;
            }
            stopMovement();
            lookAtPos(Vec3.atCenterOf(matureCrop));
            BlockState cropState = this.level().getBlockState(matureCrop);
            if (cropState.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop && crop.isMaxAge(cropState)) {
                this.level().destroyBlock(matureCrop, true, this);
                vacuumNearbyDrops();
                tryReplantCropAt(crop, matureCrop);
                this.actionProgressCount++;
            }
            return false;
        }

        BlockPos bareFarmland = findNearestBareFarmland(this.farmRadiusX);
        if (bareFarmland != null) {
            if (hasAnySeeds()) {
                double distSq = this.position().distanceToSqr(Vec3.atCenterOf(bareFarmland));
                if (distSq > 9.0) {
                    moveTowards(Vec3.atCenterOf(bareFarmland), false);
                    return false;
                }
                stopMovement();
                lookAtPos(Vec3.atCenterOf(bareFarmland));
                tryPlantSeedOnFarmland(bareFarmland);
                return false;
            } else {
                BlockPos chestPos = findNearestContainerBlock(24);
                if (chestPos != null) {
                    Container c = getContainerAt(chestPos);
                    if (c != null) {
                        int taken = takeItemsFromContainer(c, "seed", 32);
                        if (taken == 0) taken = takeItemsFromContainer(c, "carrot", 16);
                        if (taken == 0) taken = takeItemsFromContainer(c, "potato", 16);
                        if (taken > 0) {
                            sayInChat("Grabbed " + taken + "x planting supplies from chest to continue planting.");
                            return false;
                        }
                    }
                }
            }
        }

        if (this.farmAllowTill && tryTillDirtInFarmBox()) {
            return false;
        }

        stopMovement();
        if (getFarmerDepositableCropCount() > 0) {
            BlockPos chestPos = findNearestAvailableContainerBlock(24);
            if (chestPos != null) {
                depositFarmerCropsIntoChest(chestPos);
            }
        }
        if (this.actionProgressCount > 0) {
            sayInChat("Harvested " + this.actionProgressCount + "x mature crops and replanted seeds. No more crops currently mature.");
        } else {
            sayInChat("All crops in the area are still growing. I'll watch over them!");
        }
        return true;
    }

    private boolean tickTillAction(BotAction action) {
        int goal = action.count > 0 ? action.count : 20;
        if (this.actionProgressCount >= goal) {
            sayInChat("Finished tilling " + this.actionProgressCount + " farmland blocks!");
            return true;
        }
        if (!tryTillDirtInFarmBox()) {
            sayInChat("No more dirt to till in my farm zone.");
            return true;
        }
        this.actionProgressCount++;
        return false;
    }

    private int getFarmerDepositableCropCount() {
        int depositable = 0;
        int carrotCount = 0;
        int potatoCount = 0;
        int seedCount = 0;

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(Items.WHEAT) || s.is(Items.HAY_BLOCK) || s.is(Items.BEETROOT) || s.is(Items.MELON_SLICE) || s.is(Items.PUMPKIN)) {
                depositable += s.getCount();
            } else if (s.is(Items.CARROT)) {
                carrotCount += s.getCount();
            } else if (s.is(Items.POTATO)) {
                potatoCount += s.getCount();
            } else if (s.is(Items.WHEAT_SEEDS) || s.is(Items.BEETROOT_SEEDS) || s.is(Items.MELON_SEEDS) || s.is(Items.PUMPKIN_SEEDS)) {
                seedCount += s.getCount();
            }
        }
        depositable += Math.max(0, carrotCount - 16);
        depositable += Math.max(0, potatoCount - 16);
        depositable += Math.max(0, seedCount - this.maxSeedsToKeepOnHand);
        return depositable;
    }

    private void tickPassiveFarmer() {
        if (this.tickCount % 20 != 0) return;

        // Auto-compact wheat to hay bales to save inventory and chest space
        if (this.autoCompactWheatToHay && countMatching(s -> s.is(Items.WHEAT)) >= 9) {
            compactWheatInInventory();
        }

        // JaneMoose Tractor Integration
        if (TractorCompat.isAvailable()) {
            Entity tractor = TractorCompat.getRiddenTractor(this);
            if (tractor != null) {
                TractorCompat.refuelTractorIfLow(this, tractor);
                TractorCompat.ensureAttachmentOperational(tractor, this);

                BlockPos targetBlock = findNearestMatureCrop(this.farmRadiusX);
                if (targetBlock == null && this.farmAllowTill) {
                    targetBlock = findNearestTillableDirtInFarmBox();
                }
                if (targetBlock == null) {
                    targetBlock = findNearestBareFarmland(this.farmRadiusX);
                }

                if (targetBlock != null) {
                    Vec3 targetVec = Vec3.atCenterOf(targetBlock);
                    TractorCompat.steerTractor(this, tractor, targetVec, 0.85);
                    return;
                } else {
                    TractorCompat.stopTractor(this, tractor);
                    if (this.farmOriginPos != null && this.distanceToSqr(Vec3.atCenterOf(this.farmOriginPos)) > 16.0) {
                        TractorCompat.steerTractor(this, tractor, Vec3.atCenterOf(this.farmOriginPos), 0.5);
                    } else {
                        TractorCompat.dismountTractor(this);
                        sayInChat("Farm field completely tended with tractor! Tractor parked.");
                    }
                    return;
                }
            } else {
                Entity nearbyTractor = TractorCompat.findNearbyTractor(this.level(), this.position(), 24.0);
                if (nearbyTractor != null && (this.tractorMode || this.botRole.toLowerCase().contains("tractor"))) {
                    double distSq = this.distanceToSqr(nearbyTractor);
                    if (distSq > 6.0) {
                        moveTowards(nearbyTractor.position(), false);
                        return;
                    } else {
                        TractorCompat.mountTractor(this, nearbyTractor);
                        sayInChat("Mounted the JaneMoose tractor! Beginning mechanized farm work.");
                        return;
                    }
                }
            }
        }

        // 1. Store crops into nearby chest only when there is an actual surplus of harvested crops to deposit (>= 16)
        int depositable = getFarmerDepositableCropCount();
        if (depositable >= 16) {
            BlockPos chestPos = findNearestAvailableContainerBlock(24);
            if (chestPos != null) {
                double distSq = this.distanceToSqr(Vec3.atCenterOf(chestPos));
                if (distSq > 9.0) {
                    moveTowards(Vec3.atCenterOf(chestPos), false);
                    return;
                } else {
                    stopMovement();
                    depositFarmerCropsIntoChest(chestPos);
                    if (getFarmerDepositableCropCount() >= 16) {
                        BlockPos nextChest = findNearestAvailableContainerBlock(24);
                        if (nextChest != null && !nextChest.equals(chestPos)) {
                            moveTowards(Vec3.atCenterOf(nextChest), false);
                        }
                    }
                    return;
                }
            } else if (this.tickCount % 200 == 0) {
                sayInChat("My bags are full of crops and all nearby chests are full! Please place an empty chest.");
            }
        }

        // 2. Compost surplus seeds if bot holds more than maxSeedsToKeepOnHand and composter is nearby
        if (this.autoCompostExcessSeeds) {
            int totalSeeds = countMatching(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString().contains("seed"));
            if (totalSeeds > this.maxSeedsToKeepOnHand) {
                BlockPos composterPos = findNearestComposterBlock(24);
                if (composterPos != null) {
                    double distSq = this.distanceToSqr(Vec3.atCenterOf(composterPos));
                    if (distSq > 9.0) {
                        moveTowards(Vec3.atCenterOf(composterPos), false);
                        return;
                    } else {
                        stopMovement();
                        int composted = compostExcessSeeds(composterPos, this.maxSeedsToKeepOnHand);
                        if (composted > 0) {
                            return;
                        }
                    }
                }
            }
        }

        BlockPos matureCrop = findNearestMatureCrop(this.farmRadiusX);
        if (matureCrop != null) {
            double distSq = this.position().distanceToSqr(Vec3.atCenterOf(matureCrop));
            if (distSq > 9.0) {
                moveTowards(Vec3.atCenterOf(matureCrop), false);
            } else {
                stopMovement();
                lookAtPos(Vec3.atCenterOf(matureCrop));
                BlockState cropState = this.level().getBlockState(matureCrop);
                if (cropState.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop && crop.isMaxAge(cropState)) {
                    this.level().destroyBlock(matureCrop, true, this);
                    vacuumNearbyDrops();
                    tryReplantCropAt(crop, matureCrop);
                }
            }
            return;
        }

        BlockPos bareFarmland = findNearestBareFarmland(this.farmRadiusX);
        if (bareFarmland != null) {
            if (hasAnySeeds()) {
                double distSq = this.position().distanceToSqr(Vec3.atCenterOf(bareFarmland));
                if (distSq > 9.0) {
                    moveTowards(Vec3.atCenterOf(bareFarmland), false);
                } else {
                    stopMovement();
                    lookAtPos(Vec3.atCenterOf(bareFarmland));
                    tryPlantSeedOnFarmland(bareFarmland);
                }
                return;
            } else {
                BlockPos chestPos = findNearestContainerBlock(24);
                if (chestPos != null) {
                    Container c = getContainerAt(chestPos);
                    if (c != null) {
                        int taken = takeItemsFromContainer(c, "seed", 32);
                        if (taken == 0) taken = takeItemsFromContainer(c, "carrot", 16);
                        if (taken == 0) taken = takeItemsFromContainer(c, "potato", 16);
                    }
                }
            }
            return;
        }

        if (this.farmAllowTill && tryTillDirtInFarmBox()) {
            return;
        }

        // Return to farm center if drifted away
        if (this.farmOriginPos != null && this.distanceToSqr(Vec3.atCenterOf(this.farmOriginPos)) > 25.0) {
            moveTowards(Vec3.atCenterOf(this.farmOriginPos), false);
        } else {
            stopMovement();
        }
    }

    private void tickPassiveConductor() {
        if (!TrainCompat.isAvailable()) {
            return;
        }
        if (this.trainBoardCooldownTicks > 0) {
            this.trainBoardCooldownTicks--;
            return;
        }

        // 1. Check if currently riding a locomotive
        Entity loco = TrainCompat.getRiddenLocomotive(this);
        if (loco == null) {
            boolean isConductorRole = this.botRole.toLowerCase().contains("conductor") || this.botRole.toLowerCase().contains("train");
            if (!isConductorRole) {
                return; // Non-conductors must NEVER auto-board!
            }
            // If we have an active train command/target, find nearby locomotive to board
            if (this.conductorFollowingPlayer || !this.conductorDestStation.isBlank() || this.conductorTileTarget > 0) {
                Entity nearbyLoco = TrainCompat.findNearbyLocomotive(this.level(), this.position(), 32.0);
                if (nearbyLoco != null) {
                    double distSq = this.distanceToSqr(nearbyLoco);
                    if (distSq > 9.0) {
                        moveTowards(nearbyLoco.position(), false);
                    } else {
                        stopMovement();
                        TrainCompat.mountLocomotive(this, nearbyLoco);
                        sayInChat("Boarded the locomotive! Conductor ready on the rails.");
                    }
                } else {
                    stopMovement();
                }
            }
            return;
        }

        // 2. We are aboard the locomotive! Maintain fuel
        if (this.tickCount % 40 == 0) {
            TrainCompat.refuelLocomotive(this, loco);
        }

        // Case A: Following player along rails ("come here" / "come to me")
        if (this.conductorFollowingPlayer && this.conductorTargetPlayer != null) {
            ServerPlayer target = this.conductorTargetPlayer;
            double distSq = loco.distanceToSqr(target);
            if (distSq <= 16.0) { // within 4 blocks
                TrainCompat.stopTrain(loco, this);
                TrainCompat.blowHorn(loco, this);
                this.conductorFollowingPlayer = false;
                this.conductorTargetPlayer = null;
                sayInChat("Arrived right in front of you!");
                return;
            }
            if (!TrainCompat.isTrainRunning(loco)) {
                boolean reverse = shouldReverseTowards(loco, target.position());
                TrainCompat.setReversed(loco, reverse);
                TrainCompat.refuelLocomotive(this, loco);
                TrainCompat.startTrain(loco, this, 2, reverse);
            }
            return;
        }

        // Case B: Moving specific tile/block distance (e.g. "move ahead 4 tiles", "back 5 blocks")
        if (this.conductorTileTarget > 0 && this.conductorMoveStartPos != null) {
            double distTraveled = loco.position().distanceTo(this.conductorMoveStartPos);
            if (distTraveled >= this.conductorTileTarget - 0.2) {
                TrainCompat.stopTrain(loco, this);
                TrainCompat.blowHorn(loco, this);
                int target = this.conductorTileTarget;
                this.conductorTileTarget = 0;
                this.conductorMoveStartPos = null;
                sayInChat("Finished moving " + target + " blocks!");
                return;
            }
            if (!TrainCompat.isTrainRunning(loco)) {
                TrainCompat.refuelLocomotive(this, loco);
                TrainCompat.startTrain(loco, this, 2, TrainCompat.isReversed(loco));
            }
            return;
        }

        // Case C: Traveling to destination station
        String targetStationName = this.conductorIsReturning ? this.conductorReturnStation : this.conductorDestStation;
        if (targetStationName.isBlank()) {
            return;
        }

        BotPersistence.SavedLocation station = BotPersistence.getLocation(targetStationName);
        if (station == null) {
            // Unknown station: notify ONCE and clear so it never spams!
            sayInChat("Station '" + targetStationName + "' is unknown. Stand at the station platform and say 'remember this spot as " + targetStationName + "' first.");
            this.conductorDestStation = "";
            this.conductorReturnStation = "";
            this.conductorIsReturning = false;
            TrainCompat.stopTrain(loco, this);
            return;
        }

        Vec3 stationVec = new Vec3(station.x + 0.5, station.y + 0.5, station.z + 0.5);
        double distToStationSq = loco.distanceToSqr(stationVec);

        // Arrived at destination station (within 9 blocks)
        if (distToStationSq <= 81.0) {
            if (TrainCompat.isTrainRunning(loco)) {
                TrainCompat.stopTrain(loco, this);
                TrainCompat.blowHorn(loco, this);
                sayInChat("Arrived at " + (this.conductorIsReturning ? "return station '" + targetStationName + "'" : "station '" + targetStationName + "'") + "! Train stopped.");
                this.conductorArrivedTicks = 0;
            }
            this.conductorArrivedTicks++;

            if (this.conductorArrivedTicks == 20) {
                BlockPos chestPos = findNearestContainerBlock(12);
                if (chestPos != null && this.level().getBlockEntity(chestPos) instanceof Container chest) {
                    List<Entity> carts = TrainCompat.getConnectedCarts(loco);
                    if (!this.conductorIsReturning) {
                        if (this.conductorReturnStation.isBlank()) {
                            int unloaded = TrainCompat.transferFreightTrainToChest(carts, chest, this.conductorFreightItem, this.conductorFreightCount);
                            sayInChat("Unloaded " + unloaded + "x freight into station chest at '" + targetStationName + "'. Delivery complete!");
                            this.conductorDestStation = "";
                        } else {
                            int loaded = TrainCompat.transferFreightChestToTrain(chest, carts, this.conductorFreightItem, this.conductorFreightCount);
                            sayInChat("Loaded " + loaded + "x " + (this.conductorFreightItem.isBlank() ? "freight" : this.conductorFreightItem) + " from station chest. Preparing return trip to '" + this.conductorReturnStation + "'!");
                            this.conductorIsReturning = true;
                            boolean rev = shouldReverseTowards(loco, stationVec);
                            TrainCompat.setReversed(loco, !rev);
                        }
                    } else {
                        int unloaded = TrainCompat.transferFreightTrainToChest(carts, chest, this.conductorFreightItem, this.conductorFreightCount);
                        sayInChat("Returned home to '" + targetStationName + "'! Delivered " + unloaded + "x freight safely into the station chest. Route complete!");
                        this.conductorDestStation = "";
                        this.conductorReturnStation = "";
                        this.conductorIsReturning = false;
                    }
                } else {
                    sayInChat("No station chest found at '" + targetStationName + "'. Route complete!");
                    this.conductorDestStation = "";
                    this.conductorReturnStation = "";
                    this.conductorIsReturning = false;
                }
            }
            return;
        }

        // Underway towards destination
        if (!TrainCompat.isTrainRunning(loco)) {
            boolean reverse = shouldReverseTowards(loco, stationVec);
            TrainCompat.setReversed(loco, reverse);
            TrainCompat.blowHorn(loco, this);
            TrainCompat.refuelLocomotive(this, loco);
            TrainCompat.startTrain(loco, this, 3, reverse);
            sayInChat("All aboard! Train departing for '" + targetStationName + "'.");
            this.conductorLastWhistleTime = this.tickCount;
        } else {
            if (this.tickCount - this.conductorLastWhistleTime > 160) {
                TrainCompat.blowHorn(loco, this);
                this.conductorLastWhistleTime = this.tickCount;
            }
        }
    }

    // === ANIMAL HUSBANDRY & RANCHING ENGINE ===

    public List<Animal> getAllLivingAnimalsOfSpecies(String species) {
        Vec3 center = this.ranchOriginPos != null ? Vec3.atCenterOf(this.ranchOriginPos) : this.position();
        AABB box = new AABB(center.x - this.ranchRadius, center.y - 10, center.z - this.ranchRadius,
                center.x + this.ranchRadius, center.y + 10, center.z + this.ranchRadius);
        String q = (species == null || species.isBlank() || "all".equalsIgnoreCase(species) || "animal".equalsIgnoreCase(species))
                ? "" : species.toLowerCase(Locale.ROOT);

        return this.level().getEntitiesOfClass(Animal.class, box, a -> {
            if (!a.isAlive()) return false;
            if (!isInsideRanchArea(a.position())) return false;
            if (q.isEmpty()) return true;
            String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(a.getType()).getPath().toLowerCase();
            return typeKey.contains(q);
        });
    }

    public ItemStack findBreedingFoodInInventory(Animal animal, String preferredFood) {
        String pref = (preferredFood == null || preferredFood.isBlank()) ? "" : preferredFood.toLowerCase(Locale.ROOT);
        ItemStack backup = ItemStack.EMPTY;

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            if (animal.isFood(s)) {
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().toLowerCase();
                if (!pref.isEmpty() && id.contains(pref)) {
                    return s;
                }
                if (backup.isEmpty()) {
                    backup = s;
                }
            }
        }
        return backup;
    }

    public int takeBreedingFoodFromContainer(Container container, String preferredFood) {
        String q = (preferredFood != null && !preferredFood.isBlank()) ? preferredFood.toLowerCase(Locale.ROOT) : "wheat";
        int taken = takeItemsFromContainer(container, q, 32);
        if (taken == 0 && !"wheat".equals(q)) {
            taken = takeItemsFromContainer(container, "wheat", 32);
        }
        if (taken == 0) {
            taken = takeItemsFromContainer(container, "carrot", 32);
        }
        if (taken == 0) {
            taken = takeItemsFromContainer(container, "seed", 32);
        }
        return taken;
    }

    private int countAnimalDrops() {
        return countMatching(s -> {
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().toLowerCase();
            return id.contains("beef") || id.contains("pork") || id.contains("mutton")
                    || id.contains("chicken") || id.contains("leather") || id.contains("wool") || id.contains("feather");
        });
    }

    private boolean tickBreedAction(BotAction action) {
        String species = action.getPrimaryTarget().isEmpty() ? this.ranchAnimal : action.getPrimaryTarget();
        String food = (action.item != null && !action.item.isBlank()) ? action.item : this.ranchFood;
        List<Animal> animals = getAllLivingAnimalsOfSpecies(species);
        List<Animal> breedable = animals.stream()
                .filter(a -> !a.isBaby() && a.getAge() == 0 && a.canFallInLove() && !a.isInLove())
                .sorted(Comparator.comparingDouble(this::distanceToSqr))
                .toList();

        if (breedable.isEmpty() || this.actionProgressCount >= action.count) {
            stopMovement();
            sayInChat("Breeding complete for " + species + "s! Fed " + this.actionProgressCount + " animals.");
            return true;
        }

        Animal target = breedable.get(0);
        ItemStack foodStack = findBreedingFoodInInventory(target, food);
        if (foodStack.isEmpty()) {
            BlockPos chestPos = findNearestContainerBlock(20);
            if (chestPos != null && this.level().getBlockEntity(chestPos) instanceof Container c) {
                int taken = takeBreedingFoodFromContainer(c, food);
                if (taken > 0) {
                    sayInChat("Grabbed " + taken + "x " + food + " from chest to breed " + species + "s.");
                    return false;
                }
            }
            sayInChat("Out of breeding food (" + food + ") for " + species + "s.");
            return true;
        }

        double distSq = this.distanceToSqr(target);
        if (distSq > 9.0) {
            moveTowards(target.position(), false);
        } else {
            stopMovement();
            lookAtPos(target.position());
            foodStack.shrink(1);
            target.setInLove(this);
            broadcastSwing();
            this.actionProgressCount++;
            sayInChat("Fed a " + species + " to breed (" + this.actionProgressCount + "/" + action.count + ")!");
        }
        return false;
    }

    private boolean tickCullAction(BotAction action) {
        String species = action.getPrimaryTarget().isEmpty() ? this.ranchAnimal : action.getPrimaryTarget();
        int cap = action.count > 0 ? action.count : (this.ranchMaxPop > 0 ? this.ranchMaxPop : 10);
        List<Animal> animals = getAllLivingAnimalsOfSpecies(species);
        int total = animals.size();
        List<Animal> adults = animals.stream().filter(a -> !a.isBaby()).toList();

        if (total <= cap || total <= this.ranchMinPop || adults.size() <= this.ranchMinPop) {
            stopMovement();
            sayInChat("Herd population check complete: " + total + "x " + species + "s present (threshold: " + cap + ", minimum: " + this.ranchMinPop + "). No culling needed.");
            return true;
        }

        Animal victim = adults.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (victim == null || !victim.isAlive()) {
            return true;
        }

        equipBestWeapon();
        double distSq = this.distanceToSqr(victim);
        if (distSq > 6.25) {
            moveTowards(victim.position(), false);
        } else {
            stopMovement();
            lookAtPos(victim.position());
            this.attack(victim);
            broadcastSwing();
            if (!victim.isAlive()) {
                vacuumNearbyDrops();
                this.actionProgressCount++;
                sayInChat("Culled 1x adult " + species + " (remaining herd: " + (total - 1) + ").");
            }
        }
        return false;
    }

    private boolean tickHusbandryAction(BotAction action) {
        tickPassiveRancher();
        this.actionProgressCount++;
        if (this.actionProgressCount >= (action.count > 0 ? action.count : 5)) {
            return true;
        }
        return false;
    }

    private void tickPassiveRancher() {
        if (this.tickCount % 20 != 0) return;

        // 1. Store animal drops into chest if count >= 8
        if (countAnimalDrops() >= 8) {
            BlockPos chestPos = findNearestContainerBlock(20);
            if (chestPos != null) {
                double distSq = this.distanceToSqr(Vec3.atCenterOf(chestPos));
                if (distSq > 9.0) {
                    moveTowards(Vec3.atCenterOf(chestPos), false);
                    return;
                } else {
                    tickChestAction(new BotAction("CHEST_STORE", "drops", "", 64), true);
                    return;
                }
            }
        }

        List<Animal> animals = getAllLivingAnimalsOfSpecies(this.ranchAnimal);
        int totalCount = animals.size();
        List<Animal> adults = animals.stream().filter(a -> !a.isBaby()).toList();

        // 2. Culling check: strictly totalCount > maxPop AND totalCount > minPop AND adults.size() > minPop AND only kill adults
        if (this.ranchAutoCull && this.ranchMaxPop > 0 && totalCount > this.ranchMaxPop && totalCount > this.ranchMinPop && adults.size() > this.ranchMinPop) {
            Animal victim = adults.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (victim != null && victim.isAlive()) {
                equipBestWeapon();
                double distSq = this.distanceToSqr(victim);
                if (distSq > 6.25) {
                    moveTowards(victim.position(), false);
                } else {
                    stopMovement();
                    lookAtPos(victim.position());
                    this.attack(victim);
                    broadcastSwing();
                    if (!victim.isAlive()) {
                        vacuumNearbyDrops();
                        sayInChat("Culled 1x adult " + this.ranchAnimal + " to manage herd size (" + (totalCount - 1) + " remaining, min " + this.ranchMinPop + ").");
                    }
                }
                return;
            }
        }

        // 3. Breeding check
        if (this.ranchAutoBreed) {
            List<Animal> breedable = animals.stream()
                    .filter(a -> !a.isBaby() && a.getAge() == 0 && a.canFallInLove() && !a.isInLove())
                    .sorted(Comparator.comparingDouble(this::distanceToSqr))
                    .toList();

            if (!breedable.isEmpty()) {
                Animal target = breedable.get(0);
                ItemStack foodStack = findBreedingFoodInInventory(target, this.ranchFood);

                if (foodStack.isEmpty()) {
                    BlockPos chestPos = findNearestContainerBlock(20);
                    if (chestPos != null && this.level().getBlockEntity(chestPos) instanceof Container c) {
                        int taken = takeBreedingFoodFromContainer(c, this.ranchFood);
                        if (taken > 0) {
                            sayInChat("Grabbed " + taken + "x " + this.ranchFood + " from chest to breed " + this.ranchAnimal + "s.");
                        }
                    }
                    return;
                }

                double distSq = this.distanceToSqr(target);
                if (distSq > 9.0) {
                    moveTowards(target.position(), false);
                } else {
                    stopMovement();
                    lookAtPos(target.position());
                    foodStack.shrink(1);
                    target.setInLove(this);
                    broadcastSwing();
                    long now = System.currentTimeMillis();
                    if (now - lastBreedMessageTime > 8000) {
                        lastBreedMessageTime = now;
                        sayInChat("Fed a " + this.ranchAnimal + " with " + this.ranchFood + " to breed!");
                    }
                }
                return;
            }
        }

        // 4. Return to ranch origin if drifted far away
        if (this.ranchOriginPos != null && this.distanceToSqr(Vec3.atCenterOf(this.ranchOriginPos)) > 144.0) {
            moveTowards(Vec3.atCenterOf(this.ranchOriginPos), false);
        } else {
            stopMovement();
        }
    }

    // === MOVEMENT, NAVIGATION & UNSTUCK ENGINE ===

    public boolean isOpenDoorOrGate(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN) && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
            return true;
        }
        if (state.getBlock() instanceof DoorBlock && state.hasProperty(DoorBlock.OPEN) && state.getValue(DoorBlock.OPEN)) {
            return true;
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock && state.hasProperty(net.minecraft.world.level.block.FenceGateBlock.OPEN) && state.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN)) {
            return true;
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock && state.hasProperty(net.minecraft.world.level.block.TrapDoorBlock.OPEN) && state.getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN)) {
            return true;
        }
        return false;
    }

    public boolean isHandOpenableDoorOrGate(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.getBlock() instanceof DoorBlock door) {
            return door.type().canOpenByHand();
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
            return true;
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase();
        if (path.contains("gate")) {
            return true;
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock) {
            return !state.is(net.minecraft.world.level.block.Blocks.IRON_TRAPDOOR);
        }
        return false;
    }

    /**
     * Determines the primary passage axis through a doorway, fence gate, or opening.
     * For doors and fence gates, the facing property indicates the orientation:
     * - If facing North or South, passage is along the Z axis (North-South).
     * - If facing East or West, passage is along the X axis (East-West).
     */
    public net.minecraft.core.Direction.Axis getDoorwayPassageAxis(BlockPos pos, BlockState state) {
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).getAxis();
        }
        // Fallback: check adjacent horizontal neighbors to see which axis has solid walls/fences
        boolean hasNorthSouthObstacle = isFenceOrWall(this.level().getBlockState(pos.north())) || isFenceOrWall(this.level().getBlockState(pos.south()))
                || !this.level().getBlockState(pos.north()).isAir() || !this.level().getBlockState(pos.south()).isAir();
        boolean hasEastWestObstacle = isFenceOrWall(this.level().getBlockState(pos.east())) || isFenceOrWall(this.level().getBlockState(pos.west()))
                || !this.level().getBlockState(pos.east()).isAir() || !this.level().getBlockState(pos.west()).isAir();

        if (hasEastWestObstacle && !hasNorthSouthObstacle) {
            return net.minecraft.core.Direction.Axis.Z; // Walls on East/West -> passage is along Z
        }
        if (hasNorthSouthObstacle && !hasEastWestObstacle) {
            return net.minecraft.core.Direction.Axis.X; // Walls on North/South -> passage is along X
        }
        return this.getDirection().getAxis();
    }

    public static boolean isStairBlock(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.getBlock() instanceof net.minecraft.world.level.block.StairBlock) return true;
        if (state.is(net.minecraft.tags.BlockTags.STAIRS)) return true;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(java.util.Locale.ROOT);
        return path.contains("stairs") || path.contains("stair") || path.contains("loft_stairs")
                || path.contains("compact_stairs") || path.contains("bulk_stairs")
                || path.contains("terrace_stairs") || path.contains("skyline_stairs")
                || path.contains("step");
    }

    public static boolean isRailingOrBalustrade(BlockState state) {
        if (state == null || state.isAir()) return false;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(java.util.Locale.ROOT);
        return path.contains("railing") || path.contains("balustrade") || path.contains("balcony")
                || path.contains("handrail") || path.contains("banister") || path.contains("guardrail");
    }

    public static boolean isStairOrPlatform(BlockState state) {
        if (isStairBlock(state)) return true;
        if (state == null || state.isAir()) return false;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(java.util.Locale.ROOT);
        return path.contains("platform");
    }

    public boolean isFenceOrWall(BlockState state) {
        if (state == null || state.isAir()) return false;
        // Never treat stairs, stair platforms, railings, or balconies as impenetrable perimeter fence walls!
        if (isStairOrPlatform(state) || isRailingOrBalustrade(state)) return false;

        if (state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
            return !state.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN);
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("fence_gate") || path.contains("gate")) {
            return !isOpenDoorOrGate(state);
        }
        if (state.is(net.minecraft.tags.BlockTags.FENCES) || state.is(net.minecraft.tags.BlockTags.WALLS)) return true;
        if (state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock || state.getBlock() instanceof net.minecraft.world.level.block.WallBlock) return true;
        return path.contains("fence") || (path.contains("wall") && !path.contains("sign") && !path.contains("torch"));
    }

    public boolean isLineBlockedByFence(Vec3 from, Vec3 to) {
        double dist = from.distanceTo(to);
        if (dist < 0.8) return false;
        int steps = Math.max(2, (int) Math.ceil(dist * 2.0));
        Vec3 diff = to.subtract(from);
        double stepFrac = 1.0 / steps;

        int minY = (int) Math.floor(Math.min(from.y, to.y)) - 2;
        int maxY = (int) Math.ceil(Math.max(from.y, to.y)) + 1;

        for (int i = 1; i < steps; i++) {
            Vec3 sample = from.add(diff.scale(i * stepFrac));
            int blockX = Mth.floor(sample.x);
            int blockZ = Mth.floor(sample.z);
            for (int y = minY; y <= maxY; y++) {
                BlockPos p = new BlockPos(blockX, y, blockZ);
                BlockState st = this.level().getBlockState(p);
                if (isFenceOrWall(st) && !isOpenDoorOrGate(st)) {
                    // Ignore fence/wall blocks if they are directly flanking or part of a walkable stair corridor
                    if (isStairOrPlatform(st) || isRailingOrBalustrade(st)) continue;
                    boolean isBesideStairs = false;
                    for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        if (isStairOrPlatform(this.level().getBlockState(p.relative(d)))) {
                            isBesideStairs = true;
                            break;
                        }
                    }
                    if (isBesideStairs) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canShootAtTarget(LivingEntity target) {
        if (!target.isAlive()) return false;
        if (this.hasLineOfSight(target)) return true;

        Vec3 eyePos = this.getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        HitResult hit = this.level().clip(new ClipContext(eyePos, targetEye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.MISS) return true;
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bhr) {
            BlockState hitState = this.level().getBlockState(bhr.getBlockPos());
            if (isFenceOrWall(hitState)) {
                // Obstruction is a fence or wall; since archer is elevated and fires in an arc, LoS is cleared for archery
                return true;
            }
        }
        return false;
    }

    public @Nullable BlockPos findNearbyFenceOpeningOrGate(BlockPos start, Vec3 goal, int radius) {
        BlockPos bestPos = null;
        double bestScore = Double.MAX_VALUE;

        int minY = Math.max(this.level().getMinY(), start.getY() - 2);
        int maxY = Math.min(this.level().getMaxY(), start.getY() + 3);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos p = new BlockPos(start.getX() + dx, y, start.getZ() + dz);
                    BlockState state = this.level().getBlockState(p);

                    // Candidate 1: Fence Gate (passageway in fence perimeter)
                    if (state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
                        BlockState below = this.level().getBlockState(p.below());
                        BlockState above = this.level().getBlockState(p.above());
                        if (!below.isAir() && (above.isAir() || isPassable(p.above()))) {
                            double distToBot = Math.sqrt(start.distSqr(p));
                            double distToGoal = Math.sqrt(Vec3.atBottomCenterOf(p).distanceToSqr(goal));
                            double score = distToBot + distToGoal * 0.7;
                            if (score < bestScore) {
                                bestScore = score;
                                bestPos = p;
                            }
                        }
                    }
                    // Candidate 2: Existing Opening / Gap in the fence line
                    else if (isPassable(p) && isPassable(p.above())) {
                        BlockState below = this.level().getBlockState(p.below());
                        if (!below.isAir() && !below.canBeReplaced() && !isFenceOrWall(below)) {
                            boolean adjacentToFence = false;
                            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                                if (isFenceOrWall(this.level().getBlockState(p.relative(d)))) {
                                    adjacentToFence = true;
                                    break;
                                }
                            }
                            if (adjacentToFence) {
                                double distToBot = Math.sqrt(start.distSqr(p));
                                double distToGoal = Math.sqrt(Vec3.atBottomCenterOf(p).distanceToSqr(goal));
                                double score = distToBot + distToGoal * 0.85;
                                if (score < bestScore) {
                                    bestScore = score;
                                    bestPos = p;
                                }
                            }
                        }
                    }
                }
            }
        }
        return bestPos;
    }

    public static boolean isHazardousFluid(FluidState fluidState) {
        if (fluidState.isEmpty()) return false;
        if (fluidState.is(FluidTags.LAVA)) return true;
        String id = BuiltInRegistries.FLUID.getKey(fluidState.getType()).getPath().toLowerCase();
        return id.contains("oil") || id.contains("tar") || id.contains("fuel") || id.contains("petroleum")
                || id.contains("diesel") || id.contains("acid") || id.contains("sludge") || id.contains("toxic") || id.contains("poison");
    }

    public static boolean isHazardousBlockOrFluid(BlockState blockState, FluidState fluidState) {
        if (isHazardousFluid(fluidState)) return true;
        if (blockState.is(Blocks.LAVA) || blockState.is(Blocks.MAGMA_BLOCK) || blockState.is(Blocks.FIRE) || blockState.is(Blocks.SOUL_FIRE)) return true;
        String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getPath().toLowerCase();
        return blockId.contains("oil") || blockId.contains("tar") || blockId.contains("fuel") || blockId.contains("petroleum")
                || blockId.contains("diesel") || blockId.contains("acid") || blockId.contains("sludge") || blockId.contains("toxic");
    }

    public boolean isInHazardousFluid() {
        if (this.isInLava()) return true;
        FluidState feetFluid = this.level().getFluidState(this.blockPosition());
        if (isHazardousFluid(feetFluid)) return true;
        FluidState eyeFluid = this.level().getFluidState(BlockPos.containing(this.getEyePosition()));
        if (isHazardousFluid(eyeFluid)) return true;
        BlockState feetBlock = this.level().getBlockState(this.blockPosition());
        return isHazardousBlockOrFluid(feetBlock, feetFluid);
    }

    private final java.util.Set<BlockPos> learnedHazardPits = new java.util.HashSet<>();

    private boolean isDeepPitAt(BlockPos footCandidate) {
        BlockState footState = this.level().getBlockState(footCandidate);
        FluidState footFluid = this.level().getFluidState(footCandidate);
        // Crude oil and hazardous fluids are deadly hazards - treat strictly like lava!
        if (isHazardousBlockOrFluid(footState, footFluid)) {
            return true;
        }

        if (!footState.isAir() && !footState.canBeReplaced()) return false;

        // In Minecraft, drops of 1, 2, or 3 blocks deal ZERO fall damage and are completely safe to step down.
        // footCandidate is at candidate foot level.
        // Check depths 1..4 below footCandidate:
        // depth 1: flat ground (Y - 1)
        // depth 2: 1-block drop down (Y - 2)
        // depth 3: 2-block drop down (Y - 3)
        // depth 4: 3-block drop down (Y - 4)
        for (int depth = 1; depth <= 4; depth++) {
            BlockPos groundCandidate = footCandidate.below(depth);
            BlockState state = this.level().getBlockState(groundCandidate);
            FluidState fluid = this.level().getFluidState(groundCandidate);

            // Hazardous surfaces (crude oil, fuel, lava, magma, fire) are dangerous hazards
            if (isHazardousBlockOrFluid(state, fluid)) {
                return true;
            }

            if (!state.isAir() && !state.canBeReplaced() && !isFenceOrWall(state)) {
                // Solid walkable block found within 3-block drop: safe terrain to walk/drop down!
                return false;
            }
        }

        // 4+ block sheer drop or void: this is a hazardous pit / cliff / ravine!
        return true;
    }

    private boolean isPassable(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        FluidState fluid = this.level().getFluidState(pos);
        // Crude oil and hazardous fluids are completely non-passable (never step into them)
        if (isHazardousBlockOrFluid(state, fluid)) return false;
        if (state.isAir() || state.canBeReplaced()) return true;
        if (isOpenDoorOrGate(state) || isHandOpenableDoorOrGate(state)) return true;
        try {
            if (state.getCollisionShape(this.level(), pos).isEmpty()) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isStandable(BlockPos pos) {
        BlockState belowState = this.level().getBlockState(pos.below());
        FluidState belowFluid = this.level().getFluidState(pos.below());
        if (isHazardousBlockOrFluid(belowState, belowFluid)) return false;
        boolean hasFloor = !belowState.isAir() && !belowState.canBeReplaced() && !isFenceOrWall(belowState) && !isRailingOrBalustrade(belowState);
        return hasFloor && isPassable(pos) && isPassable(pos.above()) && !isDeepPitAt(pos);
    }

    private @Nullable BlockPos findClearBypassAtLateral(BlockPos lateralPos, net.minecraft.core.Direction forwardDir) {
        for (int dy : new int[]{0, 1, -1}) {
            BlockPos p = lateralPos.above(dy);
            if (isStandable(p)) {
                BlockPos forward = p.relative(forwardDir);
                for (int fdy : new int[]{0, 1, -1}) {
                    BlockPos fp = forward.above(fdy);
                    if (isStandable(fp)) {
                        return fp;
                    }
                }
            }
        }
        return null;
    }

    private @Nullable BlockPos findNearbyExitStep(BlockPos feetPos, Vec3 goal) {
        BlockPos bestStep = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos p = feetPos.offset(dx, dy, dz);
                    if (isStandable(p)) {
                        int wallCount = 0;
                        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                            BlockPos adjHeadPos = p.relative(d).above();
                            BlockState adjHeadState = this.level().getBlockState(adjHeadPos);
                            if (!isPassable(adjHeadPos) && !isRailingOrBalustrade(adjHeadState) && !isStairOrPlatform(adjHeadState)) {
                                wallCount++;
                            }
                        }
                        if (wallCount < 2) {
                            double d = Vec3.atCenterOf(p).distanceToSqr(goal);
                            if (d < bestDist) {
                                bestDist = d;
                                bestStep = p;
                            }
                        }
                    }
                }
            }
        }
        return bestStep;
    }

    private boolean isEnclosedInHole() {
        BlockPos feet = this.blockPosition();
        int solidHeadNeighbors = 0;
        int solidFeetNeighbors = 0;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos wallHead = feet.relative(d).above();
            BlockState hs = this.level().getBlockState(wallHead);
            if (!hs.isAir() && !hs.canBeReplaced() && !isFenceOrWall(hs) && !isOpenDoorOrGate(hs) && !isStairOrPlatform(hs) && !isRailingOrBalustrade(hs)) {
                solidHeadNeighbors++;
            }
            BlockPos wallFeet = feet.relative(d);
            BlockState fs = this.level().getBlockState(wallFeet);
            if (!fs.isAir() && !fs.canBeReplaced() && !isFenceOrWall(fs) && !isOpenDoorOrGate(fs) && !isStairOrPlatform(fs) && !isRailingOrBalustrade(fs)) {
                solidFeetNeighbors++;
            }
        }
        return solidHeadNeighbors >= 3 || (solidHeadNeighbors >= 2 && solidFeetNeighbors >= 3);
    }

    private void executeHoleEscape() {
        if (!isEnclosedInHole()) {
            return;
        }
        BlockPos feetPos = this.blockPosition();
        BlockState feetState = this.level().getBlockState(feetPos);
        BlockState belowState = this.level().getBlockState(feetPos.below());
        if (isFenceOrWall(feetState) || isFenceOrWall(belowState) || isStairOrPlatform(feetState) || isStairOrPlatform(belowState)) {
            return; // Strict boundary: Never place blocks on or climb over fences or stairs!
        }
        BlockPos aboveHead = feetPos.above(2);

        // 1. Break block directly above head if obstructed (never fences)
        BlockState aboveHeadState = this.level().getBlockState(aboveHead);
        if (!aboveHeadState.isAir() && !isFenceOrWall(aboveHeadState) && aboveHeadState.getDestroySpeed(this.level(), aboveHead) >= 0 && aboveHeadState.getDestroySpeed(this.level(), aboveHead) < 15.0f) {
            equipBestToolFor(aboveHeadState);
            this.level().destroyBlock(aboveHead, true, this);
            broadcastSwing();
        }

        // 2. Pillar jump straight up and place building block beneath feet
        lookAtPos(new Vec3(this.getX(), this.getY() - 1.0, this.getZ()));
        this.setPos(this.getX(), this.getY() + 1.05, this.getZ());
        this.setDeltaMovement(0, 0.42, 0);

        if (!placeBuildingBlockAt(feetPos)) {
            // Survival fallback: ONLY place dirt if truly in a deep pit and feetPos is air/replaceable (never fences!)
            if ((feetState.isAir() || feetState.canBeReplaced()) && !isFenceOrWall(feetState) && !isFenceOrWall(belowState)) {
                this.level().setBlockAndUpdate(feetPos, Blocks.DIRT.defaultBlockState());
            }
        }

        this.level().playSound(null, feetPos, net.minecraft.sounds.SoundEvents.STONE_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        this.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, feetPos.getX() + 0.5, feetPos.getY() + 1.2, feetPos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);

        // 3. Clear step in front so we can walk forward onto the next level (never fences)
        net.minecraft.core.Direction dir = this.getDirection();
        BlockPos frontHead = feetPos.relative(dir).above(2);
        BlockState frontHeadState = this.level().getBlockState(frontHead);
        if (!frontHeadState.isAir() && !isFenceOrWall(frontHeadState) && frontHeadState.getDestroySpeed(this.level(), frontHead) >= 0 && frontHeadState.getDestroySpeed(this.level(), frontHead) < 15.0f) {
            equipBestToolFor(frontHeadState);
            this.level().destroyBlock(frontHead, true, this);
            broadcastSwing();
        }
        vacuumNearbyDrops();
        this.stuckTicks = 0;
        this.moveNoProgressTicks = 0;
    }

    private void analyzeAndExecuteUnstuckPlan(Vec3 originalGoal) {
        BlockPos feetPos = this.blockPosition();

        // 1. ANALYZE FOR HOLE / PIT / TRENCH:
        int solidHeadNeighbors = 0;
        int solidFeetNeighbors = 0;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos wallHead = feetPos.relative(d).above();
            BlockState hs = this.level().getBlockState(wallHead);
            if (!hs.isAir() && !hs.canBeReplaced() && !isFenceOrWall(hs) && !isStairOrPlatform(hs) && !isRailingOrBalustrade(hs)) {
                solidHeadNeighbors++;
            }
            BlockPos wallFeet = feetPos.relative(d);
            BlockState fs = this.level().getBlockState(wallFeet);
            if (!fs.isAir() && !fs.canBeReplaced() && !isFenceOrWall(fs) && !isStairOrPlatform(fs) && !isRailingOrBalustrade(fs)) {
                solidFeetNeighbors++;
            }
        }

        boolean trappedInHole = solidHeadNeighbors >= 3 || (solidHeadNeighbors >= 2 && solidFeetNeighbors >= 3);
        if (trappedInHole) {
            // Check if an exit ramp or walkable step exists within 2 blocks
            BlockPos rampExit = findNearbyExitStep(feetPos, originalGoal);
            if (rampExit != null) {
                this.activeBypassWaypoint = Vec3.atBottomCenterOf(rampExit);
                this.bypassWaypointTicks = 35;
                this.setJumping(rampExit.getY() > feetPos.getY());
                if (this.onGround() && rampExit.getY() > feetPos.getY()) {
                    this.jumpFromGround();
                }
                this.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, feetPos.getX() + 0.5, feetPos.getY() + 1.2, feetPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
                return;
            }

            // Trapped deep in a pit: clear headroom overhead and pillar jump up!
            BlockPos aboveHead = feetPos.above(2);
            BlockState aboveHeadState = this.level().getBlockState(aboveHead);
            if (!aboveHeadState.isAir() && aboveHeadState.getDestroySpeed(this.level(), aboveHead) >= 0 && aboveHeadState.getDestroySpeed(this.level(), aboveHead) < 15.0f) {
                equipBestToolFor(aboveHeadState);
                this.level().destroyBlock(aboveHead, true, this);
                broadcastSwing();
            }
            executeHoleEscape();
            return;
        }

        // 2. ANALYZE FOR WALL / BUILDING / CLIFF / FENCE OBSTACLE:
        net.minecraft.core.Direction dir = this.getDirection();
        BlockPos frontFeet = feetPos.relative(dir);
        BlockPos frontHead = frontFeet.above();
        BlockState frontFeetState = this.level().getBlockState(frontFeet);
        BlockState frontHeadState = this.level().getBlockState(frontHead);

        // A. DOORWAY & FENCE GATE CHECK:
        BlockState currentFeetState = this.level().getBlockState(feetPos);
        boolean isCurrentInDoorOrGate = (currentFeetState.getBlock() instanceof DoorBlock)
                || (currentFeetState.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock)
                || isOpenDoorOrGate(currentFeetState);
        boolean isDoorInFront = (frontFeetState.getBlock() instanceof DoorBlock) || (frontHeadState.getBlock() instanceof DoorBlock);
        boolean isGateInFront = (frontFeetState.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock);

        if (isCurrentInDoorOrGate) {
            net.minecraft.core.Direction.Axis passageAxis = getDoorwayPassageAxis(feetPos, currentFeetState);
            net.minecraft.core.Direction throughDir = (passageAxis == net.minecraft.core.Direction.Axis.Z)
                    ? ((originalGoal.z >= feetPos.getZ() + 0.5) ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH)
                    : ((originalGoal.x >= feetPos.getX() + 0.5) ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST);
            this.activeBypassWaypoint = Vec3.atBottomCenterOf(feetPos.relative(throughDir, 2));
            this.bypassWaypointTicks = 40;
            return;
        }

        if (isDoorInFront) {
            BlockPos doorPos = (frontFeetState.getBlock() instanceof DoorBlock) ? frontFeet : frontHead;
            BlockState ds = this.level().getBlockState(doorPos);
            if (ds.getBlock() instanceof DoorBlock door && door.type().canOpenByHand() && !ds.getValue(DoorBlock.OPEN)) {
                door.setOpen(this, this.level(), ds, doorPos, true);
                this.lastOpenedDoorPos = doorPos;
                this.doorCloseTimer = 60;
            }
            net.minecraft.core.Direction.Axis passageAxis = getDoorwayPassageAxis(doorPos, ds);
            net.minecraft.core.Direction throughDir = (passageAxis == net.minecraft.core.Direction.Axis.Z)
                    ? ((originalGoal.z >= doorPos.getZ() + 0.5) ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH)
                    : ((originalGoal.x >= doorPos.getX() + 0.5) ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST);
            this.activeBypassWaypoint = Vec3.atBottomCenterOf(doorPos.relative(throughDir, 2));
            this.bypassWaypointTicks = 40;
            return;
        }
        if (isGateInFront) {
            BlockPos gatePos = frontFeet;
            BlockState gs = this.level().getBlockState(gatePos);
            if (gs.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
                if (!gs.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN)) {
                    BlockState openState = gs.setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, true);
                    if (openState.hasProperty(net.minecraft.world.level.block.FenceGateBlock.FACING)) {
                        if (openState.getValue(net.minecraft.world.level.block.FenceGateBlock.FACING) == dir.getOpposite()) {
                            openState = openState.setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, dir);
                        }
                    }
                    this.level().setBlockAndUpdate(gatePos, openState);
                    this.level().playSound(null, gatePos, net.minecraft.sounds.SoundEvents.FENCE_GATE_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                    this.lastOpenedGatePos = gatePos;
                    this.gateCloseTimer = 60;
                }
                net.minecraft.core.Direction.Axis passageAxis = getDoorwayPassageAxis(gatePos, gs);
                net.minecraft.core.Direction throughDir = (passageAxis == net.minecraft.core.Direction.Axis.Z)
                        ? ((originalGoal.z >= gatePos.getZ() + 0.5) ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH)
                        : ((originalGoal.x >= gatePos.getX() + 0.5) ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST);
                this.activeBypassWaypoint = Vec3.atBottomCenterOf(gatePos.relative(throughDir, 2));
                this.bypassWaypointTicks = 40;
                return;
            }
        }

        // B. FENCE / WALL STRICT BOUNDARY CHECK:
        boolean isFenceBarrier = isFenceOrWall(frontFeetState) || isFenceOrWall(frontHeadState);
        if (isFenceBarrier) {
            if (this.currentCombatTargetId != -1 || this.sentryMode || this.standingBy) {
                // In combat, sentry, or standby: never seek gates to leave fence perimeter!
                this.stopMovement();
                return;
            }
            // NEVER pillar jump or place blocks to climb over fences/walls!
            // Seek an existing opening or fence gate along the perimeter.
            BlockPos gateOrOpening = findNearbyFenceOpeningOrGate(feetPos, originalGoal, 24);
            if (gateOrOpening != null) {
                BlockState gs = this.level().getBlockState(gateOrOpening);
                net.minecraft.core.Direction.Axis passageAxis = getDoorwayPassageAxis(gateOrOpening, gs);
                net.minecraft.core.Direction throughDir = (passageAxis == net.minecraft.core.Direction.Axis.Z)
                        ? ((originalGoal.z >= gateOrOpening.getZ() + 0.5) ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH)
                        : ((originalGoal.x >= gateOrOpening.getX() + 0.5) ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST);
                this.activeBypassWaypoint = Vec3.atBottomCenterOf(gateOrOpening.relative(throughDir, 2));
                this.bypassWaypointTicks = 60;
                this.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
                return;
            } else {
                // Strict boundary: stop movement without damaging or jumping fences
                this.stopMovement();
                return;
            }
        }

        // C. STAIRS & STEP CLEARANCE:
        boolean isStairInFront = isStairOrPlatform(frontFeetState) || isStairOrPlatform(frontHeadState);
        boolean isCurrentOnStair = isStairOrPlatform(this.level().getBlockState(feetPos)) || isStairOrPlatform(this.level().getBlockState(feetPos.below()));
        if (isStairInFront || isCurrentOnStair) {
            // We are navigating a staircase! STEP_HEIGHT = 1.06 climbs stairs smoothly without jumping.
            this.setJumping(false);
            this.zza = 1.0f;
            this.xxa = 0.0f;
            this.stuckTicks = 0;
            this.moveNoProgressTicks = 0;
            return;
        }

        boolean isBlockedInFront = (!isPassable(frontFeet) && !isDoorInFront)
                || (!isPassable(frontHead) && !isDoorInFront);

        if (isBlockedInFront || this.horizontalCollision) {
            net.minecraft.core.Direction leftDir = dir.getCounterClockWise();
            net.minecraft.core.Direction rightDir = dir.getClockWise();

            BlockPos bestBypass = null;
            double bestScore = Double.MAX_VALUE;
            String dim = this.level().dimension().identifier().toString();

            // Probe left side
            for (int dist = 1; dist <= 8; dist++) {
                BlockPos probeFeet = feetPos.relative(leftDir, dist);
                boolean lateralBlocked = true;
                for (int dy : new int[]{0, 1, -1}) {
                    if (isStandable(probeFeet.above(dy))) {
                        lateralBlocked = false;
                        break;
                    }
                }
                BlockPos clearFp = findClearBypassAtLateral(probeFeet, dir);
                if (clearFp != null) {
                    double distToGoal = Vec3.atCenterOf(clearFp).distanceToSqr(originalGoal);
                    double learnedBias = com.aicompanion.bot.learning.HiveMindBrain.getNodeScore(dim, clearFp) * 4.0;
                    double score = dist * 2.0 + distToGoal - learnedBias;
                    if (score < bestScore) {
                        bestScore = score;
                        bestBypass = clearFp;
                    }
                    break;
                }
                if (lateralBlocked && dist > 1) {
                    break;
                }
            }

            // Probe right side
            for (int dist = 1; dist <= 8; dist++) {
                BlockPos probeFeet = feetPos.relative(rightDir, dist);
                boolean lateralBlocked = true;
                for (int dy : new int[]{0, 1, -1}) {
                    if (isStandable(probeFeet.above(dy))) {
                        lateralBlocked = false;
                        break;
                    }
                }
                BlockPos clearFp = findClearBypassAtLateral(probeFeet, dir);
                if (clearFp != null) {
                    double distToGoal = Vec3.atCenterOf(clearFp).distanceToSqr(originalGoal);
                    double learnedBias = com.aicompanion.bot.learning.HiveMindBrain.getNodeScore(dim, clearFp) * 4.0;
                    double score = dist * 2.0 + distToGoal - learnedBias;
                    if (score < bestScore) {
                        bestScore = score;
                        bestBypass = clearFp;
                    }
                    break;
                }
                if (lateralBlocked && dist > 1) {
                    break;
                }
            }

            if (bestBypass != null) {
                this.activeBypassWaypoint = Vec3.atBottomCenterOf(bestBypass);
                this.bypassWaypointTicks = 50;
                this.level().sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1.2, this.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
                return;
            }

            // High wall with no side opening: Disengage backwards
            BlockPos backLeft = feetPos.relative(dir.getOpposite(), 2).relative(leftDir, 2);
            if (isStandable(backLeft)) {
                this.activeBypassWaypoint = Vec3.atBottomCenterOf(backLeft);
                this.bypassWaypointTicks = 35;
                return;
            }
            BlockPos backRight = feetPos.relative(dir.getOpposite(), 2).relative(rightDir, 2);
            if (isStandable(backRight)) {
                this.activeBypassWaypoint = Vec3.atBottomCenterOf(backRight);
                this.bypassWaypointTicks = 35;
                return;
            }
        }

        // 3. ANALYZE FOR PIT / CHASM AHEAD:
        if (isDeepPitAt(frontFeet)) {
            net.minecraft.core.Direction leftDir = dir.getCounterClockWise();
            net.minecraft.core.Direction rightDir = dir.getClockWise();
            for (int dist = 1; dist <= 6; dist++) {
                BlockPos leftRim = feetPos.relative(leftDir, dist);
                if (isStandable(leftRim) && !isDeepPitAt(leftRim.relative(dir))) {
                    this.activeBypassWaypoint = Vec3.atBottomCenterOf(leftRim.relative(dir));
                    this.bypassWaypointTicks = 45;
                    return;
                }
                BlockPos rightRim = feetPos.relative(rightDir, dist);
                if (isStandable(rightRim) && !isDeepPitAt(rightRim.relative(dir))) {
                    this.activeBypassWaypoint = Vec3.atBottomCenterOf(rightRim.relative(dir));
                    this.bypassWaypointTicks = 45;
                    return;
                }
            }
        }

        // 4. GENERAL UNSTUCK FALLBACK: Never jump against walls; force fresh A* path calculation and strafe clear!
        this.invalidatePath();
        this.xxa = ((this.tickCount / 10) % 2 == 0) ? 0.85f : -0.85f;
        this.setJumping(false);
    }

    private boolean hasUnobstructedReachToTarget(BlockPos targetPos) {
        if (Math.abs(this.getY() - targetPos.getY()) > 1.5) return false;
        Vec3 from = this.getEyePosition();
        Vec3 to = Vec3.atCenterOf(targetPos).add(0, 0.25, 0);
        BlockHitResult hit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.MISS) return true;
        return hit.getBlockPos().equals(targetPos) || hit.getBlockPos().distManhattan(targetPos) <= 1 && this.level().getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock;
    }

    private void moveTowards(Vec3 target, boolean sprint) {
        // Global Area Perimeter Boundary Enforcement
        if (this.assignedAreaName != null && !this.assignedAreaName.isBlank()) {
            com.aicompanion.bot.area.SavedArea area = BotPersistence.getArea(this.assignedAreaName);
            if (area != null) {
                boolean botInside = area.contains(this.getX(), this.getY(), this.getZ());
                boolean targetInside = area.contains(target.x, target.y, target.z);

                BlockPos targetBlock = BlockPos.containing(target);
                boolean isContainerTarget = getContainerAt(targetBlock) != null;
                boolean isNearPerimeter = Math.abs(target.y - ((area.minY + area.maxY) / 2.0)) <= (area.getHeight() + 10)
                        && target.x >= (area.getMinX() - 4.0) && target.x <= (area.getMaxX() + 4.0)
                        && target.z >= (area.getMinZ() - 4.0) && target.z <= (area.getMaxZ() + 4.0);

                if (!targetInside) {
                    if (isContainerTarget && target.distanceTo(area.getCenter()) <= (Math.max(area.getMaxX() - area.getMinX(), area.getMaxZ() - area.getMinZ()) + 24.0)) {
                        // Allow moving to containers within reach of the area
                    } else if (isNearPerimeter && botInside) {
                        // Allow moving along perimeter edge
                    } else {
                        Vec3 center = area.getCenter();
                        lookAtPos(new Vec3(center.x, center.y + this.getEyeHeight(), center.z));
                        target = center;
                    }
                }
            }
        }

        Vec3 finalDestination = target;
        BlockPos finalGoalBlock = BlockPos.containing(finalDestination);

        // 0. Repetitive-Loop / Insanity Guard: Stop bots from endlessly pushing into corners or walls
        if (this.insanityCheckPos == null || this.position().distanceToSqr(this.insanityCheckPos) > 1.44) {
            this.insanityCheckPos = this.position();
            this.insanityStuckTicks = 0;
        } else {
            this.insanityStuckTicks++;
            if (this.insanityStuckTicks == 25) {
                // Penalize current stuck corner/wall in Hive-Mind and force fresh A* path around the building
                String dim = this.level().dimension().identifier().toString();
                com.aicompanion.bot.learning.HiveMindBrain.recordNodeTraversal(dim, this.blockPosition(), -12.0f, true, false, false, null);
                com.aicompanion.bot.learning.HiveMindBrain.recordNodeTraversal(dim, this.blockPosition().relative(this.getDirection()), -15.0f, true, false, false, null);
                this.invalidatePath();
            } else if (this.insanityStuckTicks >= 90) {
                // Target is completely unreachable or blocked after multiple A* attempts — blacklist for 30s and stop pushing!
                this.unreachableTargetBlacklist.put(finalGoalBlock.asLong(), this.tickCount + 600);
                this.stopMovement();
                return;
            }
        }

        // 1. Line-of-sight Fence Boundary Check: If direct line to target is blocked by fence, route through gate/opening!
        boolean nearGateOrDoor = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockState s = this.level().getBlockState(this.blockPosition().offset(dx, 0, dz));
                if (s.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock || s.getBlock() instanceof DoorBlock || isOpenDoorOrGate(s)) {
                    nearGateOrDoor = true;
                    break;
                }
            }
        }

        if (!nearGateOrDoor && isLineBlockedByFence(this.position(), target)) {
            if (this.currentCombatTargetId != -1 || this.sentryMode || this.standingBy) {
                // In combat, sentry, or standby: never leave fence perimeter!
                this.stopMovement();
                return;
            }
            if (this.activeBypassWaypoint == null) {
                BlockPos gateOrOpening = findNearbyFenceOpeningOrGate(this.blockPosition(), target, 24);
                if (gateOrOpening != null) {
                    BlockState gs = this.level().getBlockState(gateOrOpening);
                    net.minecraft.core.Direction.Axis passageAxis = getDoorwayPassageAxis(gateOrOpening, gs);
                    net.minecraft.core.Direction throughDir = (passageAxis == net.minecraft.core.Direction.Axis.Z)
                            ? ((target.z >= gateOrOpening.getZ() + 0.5) ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH)
                            : ((target.x >= gateOrOpening.getX() + 0.5) ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST);
                    this.activeBypassWaypoint = Vec3.atBottomCenterOf(gateOrOpening.relative(throughDir, 2));
                    this.bypassWaypointTicks = 80;
                }
            }
        }

        // Stagnation & Distance-to-Goal Progress Tracking
        double distToGoalSq = this.position().distanceToSqr(target);
        if (this.lastMoveTarget == null || target.distanceToSqr(this.lastMoveTarget) > 2.25) {
            this.lastMoveTarget = target;
            this.bestMoveTargetDistSq = distToGoalSq;
            this.moveNoProgressTicks = 0;
        } else {
            this.lastMoveTarget = target;
            if (distToGoalSq < this.bestMoveTargetDistSq - 0.2) {
                this.bestMoveTargetDistSq = distToGoalSq;
                this.moveNoProgressTicks = 0;
            } else {
                this.moveNoProgressTicks++;
            }
        }

        // Motionless check adds to stagnation
        if (this.position().distanceToSqr(this.lastCheckedPos) < 0.015) {
            this.moveNoProgressTicks++;
        }
        this.lastCheckedPos = this.position();

        // 1B. BASE VANILLA VILLAGER / MOB 3D VOXEL A* PATHFINDER (with Hive-Mind learned costs layered on top)
        if (this.activeBypassWaypoint == null && distToGoalSq > 1.69) {
            boolean needPathRefresh = this.currentAStarPath.isEmpty()
                    || this.currentAStarPathIndex >= this.currentAStarPath.size()
                    || this.lastAStarGoalBlock == null
                    || !this.lastAStarGoalBlock.closerThan(finalGoalBlock, 1.5)
                    || (this.tickCount - this.lastAStarComputeTick >= 20);

            if (needPathRefresh) {
                this.currentAStarPath = com.aicompanion.bot.navigation.BotPathfinder.findPath(
                        this, this.blockPosition(), finalGoalBlock, 1024);
                this.currentAStarPathIndex = 0;
                this.lastAStarGoalBlock = finalGoalBlock.immutable();
                this.lastAStarComputeTick = this.tickCount;
            }

            // Advance along A* waypoints as the bot steps onto each block
            while (this.currentAStarPathIndex < this.currentAStarPath.size()) {
                BlockPos wp = this.currentAStarPath.get(this.currentAStarPathIndex);
                double dx = (wp.getX() + 0.5) - this.getX();
                double dz = (wp.getZ() + 0.5) - this.getZ();
                double horizSq = dx * dx + dz * dz;
                if (horizSq < 0.36 && Math.abs(wp.getY() - this.getY()) <= 1.6) {
                    this.currentAStarPathIndex++;
                } else {
                    break;
                }
            }

            if (this.currentAStarPathIndex < this.currentAStarPath.size()) {
                BlockPos activeWp = this.currentAStarPath.get(this.currentAStarPathIndex);
                // Proactively open any wooden door or fence gate along the A* path ahead
                for (int lookAhead = 0; lookAhead <= 1 && (this.currentAStarPathIndex + lookAhead) < this.currentAStarPath.size(); lookAhead++) {
                    BlockPos aheadPos = this.currentAStarPath.get(this.currentAStarPathIndex + lookAhead);
                    if (this.distanceToSqr(Vec3.atBottomCenterOf(aheadPos)) < 5.0) {
                        BlockState as = this.level().getBlockState(aheadPos);
                        if (as.getBlock() instanceof DoorBlock door && !as.getValue(DoorBlock.OPEN) && door.type().canOpenByHand()) {
                            door.setOpen(this, this.level(), as, aheadPos, true);
                            this.lastOpenedDoorPos = aheadPos;
                            this.doorCloseTimer = 50;
                        } else if (as.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock && !as.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN)) {
                            this.level().setBlockAndUpdate(aheadPos, as.setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, true));
                            this.lastOpenedGatePos = aheadPos;
                            this.gateCloseTimer = 60;
                        }
                    }
                }
                target = new Vec3(activeWp.getX() + 0.5, activeWp.getY(), activeWp.getZ() + 0.5);
            } else if (this.currentAStarPath.isEmpty() && this.blockPosition().distManhattan(finalGoalBlock) <= 28 && !hasUnobstructedReachToTarget(finalGoalBlock)) {
                // Target is nearby (e.g. inside a locked/doorless building) and A* proved no walkable door path exists!
                if (this.insanityStuckTicks >= 15) {
                    this.unreachableTargetBlacklist.put(finalGoalBlock.asLong(), this.tickCount + 600);
                    this.stopMovement();
                    return;
                }
            }
        }

        // Intermediate Bypass Waypoint Steering
        if (this.activeBypassWaypoint != null) {
            double distToBypassSq = this.position().distanceToSqr(this.activeBypassWaypoint);
            if (distToBypassSq < 1.44 || this.bypassWaypointTicks-- <= 0) {
                this.activeBypassWaypoint = null;
                this.bestMoveTargetDistSq = this.position().distanceToSqr(target);
                this.moveNoProgressTicks = 0;
            } else {
                target = this.activeBypassWaypoint;
            }
        }

        // 2. 1x2 Doorway & Fence Gate Navigation & Lateral Shoulder Alignment:
        BlockPos currentFeet = this.blockPosition();
        net.minecraft.core.Direction dir = this.getDirection();
        BlockPos frontPos = currentFeet.relative(dir);
        BlockState frontState = this.level().getBlockState(frontPos);

        BlockPos doorwayPos = null;
        if (frontState.getBlock() instanceof DoorBlock) {
            doorwayPos = frontPos;
        } else if (this.level().getBlockState(currentFeet).getBlock() instanceof DoorBlock) {
            doorwayPos = currentFeet;
        } else {
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos adj = currentFeet.relative(d);
                if (this.level().getBlockState(adj).getBlock() instanceof DoorBlock) {
                    if (this.distanceToSqr(Vec3.atBottomCenterOf(adj)) < 2.0) {
                        doorwayPos = adj;
                        break;
                    }
                }
            }
        }

        BlockPos gatePos = null;
        if (frontState.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
            gatePos = frontPos;
        } else if (this.level().getBlockState(currentFeet).getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
            gatePos = currentFeet;
        } else {
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos adj = currentFeet.relative(d);
                if (this.level().getBlockState(adj).getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
                    if (this.distanceToSqr(Vec3.atBottomCenterOf(adj)) < 2.5) {
                        gatePos = adj;
                        break;
                    }
                }
            }
        }

        BlockPos activeChokepointPos = doorwayPos != null ? doorwayPos : gatePos;
        net.minecraft.core.Direction chokepointThroughDir = null;

        if (doorwayPos != null) {
            BlockState ds = this.level().getBlockState(doorwayPos);
            if (ds.getBlock() instanceof DoorBlock door) {
                // Open wooden door if closed
                if (!ds.getValue(DoorBlock.OPEN) && door.type().canOpenByHand()) {
                    door.setOpen(this, this.level(), ds, doorwayPos, true);
                    this.lastOpenedDoorPos = doorwayPos;
                    this.doorCloseTimer = 40;
                }
            }
        }

        if (gatePos != null) {
            BlockState gs = this.level().getBlockState(gatePos);
            if (gs.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
                if (!gs.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN)) {
                    BlockState openState = gs.setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, true);
                    if (openState.hasProperty(net.minecraft.world.level.block.FenceGateBlock.FACING)) {
                        if (openState.getValue(net.minecraft.world.level.block.FenceGateBlock.FACING) == dir.getOpposite()) {
                            openState = openState.setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, dir);
                        }
                    }
                    this.level().setBlockAndUpdate(gatePos, openState);
                    this.level().playSound(null, gatePos, net.minecraft.sounds.SoundEvents.FENCE_GATE_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
                    this.lastOpenedGatePos = gatePos;
                    this.gateCloseTimer = 60;
                }
            }
        }

        if (activeChokepointPos != null) {
            BlockState chokepointState = this.level().getBlockState(activeChokepointPos);
            net.minecraft.core.Direction.Axis passageAxis = getDoorwayPassageAxis(activeChokepointPos, chokepointState);
            Vec3 center = Vec3.atBottomCenterOf(activeChokepointPos);
            double distSq = this.distanceToSqr(center);

            if (passageAxis == net.minecraft.core.Direction.Axis.Z) {
                chokepointThroughDir = (target.z >= center.z) ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
                // Pure lateral alignment along X axis (NEVER apply backward drag to Z movement axis!)
                if (distSq < 3.5) {
                    double offX = center.x - this.getX();
                    Vec3 mot = this.getDeltaMovement();
                    this.setDeltaMovement(mot.x + Mth.clamp(offX * 0.35, -0.15, 0.15), mot.y, mot.z);
                }
            } else {
                chokepointThroughDir = (target.x >= center.x) ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
                // Pure lateral alignment along Z axis (NEVER apply backward drag to X movement axis!)
                if (distSq < 3.5) {
                    double offZ = center.z - this.getZ();
                    Vec3 mot = this.getDeltaMovement();
                    this.setDeltaMovement(mot.x, mot.y, mot.z + Mth.clamp(offZ * 0.35, -0.15, 0.15));
                }
            }
        }

        // 4. Stair Corridor Navigation & 1-Block Wide Shoulder Alignment:
        BlockPos currentPos = this.blockPosition();
        BlockPos stairPos = null;
        if (isStairOrPlatform(this.level().getBlockState(currentPos))) {
            stairPos = currentPos;
        } else if (isStairOrPlatform(this.level().getBlockState(currentPos.below()))) {
            stairPos = currentPos.below();
        } else if (isStairOrPlatform(frontState)) {
            stairPos = frontPos;
        } else if (isStairOrPlatform(this.level().getBlockState(frontPos.above()))) {
            stairPos = frontPos.above();
        }

        if (stairPos != null) {
            // Center bot precisely on the stair block centerline so 0.6-width shoulders don't clip 1-block wide railings
            double stairCenterX = stairPos.getX() + 0.5;
            double stairCenterZ = stairPos.getZ() + 0.5;
            double offX = stairCenterX - this.getX();
            double offZ = stairCenterZ - this.getZ();

            Vec3 mot = this.getDeltaMovement();
            if (dir.getAxis() == net.minecraft.core.Direction.Axis.Z) {
                // Moving North/South along stairs -> align X to stair centerline
                this.setDeltaMovement(mot.x + offX * 0.35, mot.y, mot.z);
            } else if (dir.getAxis() == net.minecraft.core.Direction.Axis.X) {
                // Moving East/West along stairs -> align Z to stair centerline
                this.setDeltaMovement(mot.x, mot.y, mot.z + offZ * 0.35);
            } else {
                this.setDeltaMovement(mot.x + offX * 0.25, mot.y, mot.z + offZ * 0.25);
            }
        }

        boolean passingDoorway = false;
        if (activeChokepointPos != null && chokepointThroughDir != null) {
            double distSq = this.distanceToSqr(Vec3.atBottomCenterOf(activeChokepointPos));
            if (distSq < 2.5) {
                passingDoorway = true;
                Vec3 straightAhead = this.position().add(chokepointThroughDir.getStepX() * 2.0, 0, chokepointThroughDir.getStepZ() * 2.0);
                lookAtPos(straightAhead);
            }
        }
        if (!passingDoorway) {
            lookAtPos(new Vec3(target.x, target.y + this.getEyeHeight(), target.z));
        }
        this.setSprinting(sprint);
        this.zza = sprint ? 1.0f : 0.8f;
        this.xxa = 0.0f;

        boolean isExcavating = this.currentAction != null && "EXCAVATE".equals(this.currentAction.getNormalizedType());

        // Deep Pit & Hazard Avoidance: Only check for REAL dangerous drops (4+ block drop into ravine/void or lava/fire)
        // Drops of 1, 2, or 3 blocks are completely safe to step or drop down, dealing zero damage.
        if (!isExcavating && isDeepPitAt(frontPos)) {
            // If target/player is below our elevation, NEVER bridge forward across the sky!
            if (target.y < this.getY() - 0.2) {
                // Steer along the edge or find a safe slope/staircase down toward the player
                BlockPos leftPos = this.blockPosition().relative(dir.getCounterClockWise());
                boolean leftSafe = !isDeepPitAt(leftPos);
                this.zza = 0.2f;
                this.xxa = leftSafe ? 0.9f : -0.9f;
                return;
            }

            // Target is at our elevation or higher across the chasm:
            BlockPos twoAhead = frontPos.relative(dir);
            boolean canJumpGap = this.level().getBlockState(twoAhead).isAir()
                    && !this.level().getBlockState(twoAhead.below()).isAir();
            if (canJumpGap) {
                // 1-block gap: sprint-jump across it
                this.setSprinting(true);
                this.zza = 1.0f;
                this.setJumping(true);
                if (this.onGround()) {
                    this.jumpFromGround();
                }
                return;
            } else {
                // Only place bridge blocks if crossing a narrow (1-2 block) gap toward target
                boolean narrowGap = !this.level().getBlockState(twoAhead.below()).isAir()
                        || !this.level().getBlockState(twoAhead.relative(dir).below()).isAir();
                if (narrowGap && placeBuildingBlockAt(frontPos.below())) {
                    // Successfully bridged narrow pit gap with a block!
                    this.zza = 0.5f;
                    this.xxa = 0.0f;
                    return;
                } else {
                    // Wide ravine or no blocks: steer sideways around the pit instead of placing endless sky bridges
                    BlockPos leftPos = this.blockPosition().relative(dir.getCounterClockWise());
                    boolean leftSafe = !isDeepPitAt(leftPos);
                    this.zza = 0.2f;
                    this.xxa = leftSafe ? 0.9f : -0.9f;
                    return;
                }
            }
        }

        // Barrier & Wall Recognition
        BlockPos headFront = frontPos.above();
        BlockState headState = this.level().getBlockState(headFront);
        boolean isDoorway = (frontState.getBlock() instanceof DoorBlock) || (headState.getBlock() instanceof DoorBlock)
                || (frontState.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock)
                || (this.level().getBlockState(currentFeet).getBlock() instanceof DoorBlock)
                || (this.level().getBlockState(currentFeet).getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock)
                || isOpenDoorOrGate(frontState) || isOpenDoorOrGate(headState)
                || isOpenDoorOrGate(this.level().getBlockState(currentFeet))
                || isOpenDoorOrGate(this.level().getBlockState(currentFeet.above()))
                || activeChokepointPos != null;
        boolean isFenceBarrier = (isFenceOrWall(frontState) || isFenceOrWall(headState)) && !isDoorway;
        boolean isFrontStair = isStairOrPlatform(frontState) || isStairOrPlatform(headState)
                || isStairOrPlatform(this.level().getBlockState(currentFeet))
                || isStairOrPlatform(this.level().getBlockState(currentFeet.below()));
        boolean isTallWall = !frontState.isAir() && !headState.isAir() && !headState.canBeReplaced() && !isFenceBarrier && !isDoorway && !isFrontStair;

        // Jump over 1-block ledges or swim in water/lava/oil (Never jump at 2-block tall solid walls or fences!)
        boolean stepUpPossible = (!frontState.isAir() || isFrontStair)
                && (headState.isAir() || isStairOrPlatform(headState))
                && this.level().getBlockState(headFront.above()).isAir()
                && !isFenceBarrier && !isDoorway;
        boolean wantsToAscend = target.y > this.getY() - 0.2;
        // Hive-Mind Neural Policy & Learned Node Consultation
        String dimKey = this.level().dimension().identifier().toString();
        com.aicompanion.bot.learning.HiveMindBrain.NavNode curNavNode = com.aicompanion.bot.learning.HiveMindBrain.getNode(dimKey, currentFeet);
        float[] sensoryState = com.aicompanion.bot.learning.HiveMindBrain.extractSensoryState(
                this, target, this.moveNoProgressTicks, isFrontStair, stairPos != null, isDoorway, false);
        int neuralAction = com.aicompanion.bot.learning.HiveMindBrain.selectAction(this, sensoryState);

        if ((neuralAction == com.aicompanion.bot.learning.HiveMindBrain.ACT_WALK_CENTERED || sensoryState[23] > 0.3f) && stairPos == null && !isDoorway) {
            double cx = currentFeet.getX() + 0.5 - this.getX();
            double cz = currentFeet.getZ() + 0.5 - this.getZ();
            Vec3 mot = this.getDeltaMovement();
            this.setDeltaMovement(mot.x + cx * 0.25, mot.y, mot.z + cz * 0.25);
        } else if (neuralAction == com.aicompanion.bot.learning.HiveMindBrain.ACT_STRAFE_LEFT && (this.moveNoProgressTicks >= 6 || this.horizontalCollision) && !isFrontStair) {
            this.xxa = 0.65f;
        } else if (neuralAction == com.aicompanion.bot.learning.HiveMindBrain.ACT_STRAFE_RIGHT && (this.moveNoProgressTicks >= 6 || this.horizontalCollision) && !isFrontStair) {
            this.xxa = -0.65f;
        }

        // With STEP_HEIGHT = 1.06, bots step up 1-block ledges and stairs smoothly without jumping.
        // ONLY jump when swimming in water/lava/hazardous fluid!
        boolean shouldJump = this.isInWater() || this.isInLava() || this.isInHazardousFluid();
        this.setJumping(shouldJump);

        if (isFenceBarrier) {
            this.setJumping(false);
            if (this.currentCombatTargetId != -1 || this.sentryMode || this.standingBy) {
                // In combat, sentry, or standby: never leave fence perimeter!
                this.stopMovement();
                return;
            }
            if (this.activeBypassWaypoint == null) {
                BlockPos gateOrOpening = findNearbyFenceOpeningOrGate(this.blockPosition(), target, 24);
                if (gateOrOpening != null) {
                    BlockState gs = this.level().getBlockState(gateOrOpening);
                    net.minecraft.core.Direction.Axis passageAxis = getDoorwayPassageAxis(gateOrOpening, gs);
                    net.minecraft.core.Direction throughDir = (passageAxis == net.minecraft.core.Direction.Axis.Z)
                            ? ((target.z >= gateOrOpening.getZ() + 0.5) ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH)
                            : ((target.x >= gateOrOpening.getX() + 0.5) ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST);
                    this.activeBypassWaypoint = Vec3.atBottomCenterOf(gateOrOpening.relative(throughDir, 2));
                    this.bypassWaypointTicks = 80;
                    return;
                }
            }
            if (this.guardAnchorPos != null) {
                lookAtPos(Vec3.atCenterOf(this.guardAnchorPos));
                this.zza = 0.5f;
                this.xxa = 0.0f;
                return;
            } else {
                // Respect fence boundary: stop!
                this.stopMovement();
                return;
            }
        }

        // Record step & intrinsic reinforcement into the shared Hive-Mind
        boolean madeProgress = this.moveNoProgressTicks == 0;
        boolean gotStuck = !isExcavating && this.moveNoProgressTicks >= 16;
        com.aicompanion.bot.learning.HiveMindBrain.recordBotStep(this, madeProgress, gotStuck, shouldJump);

        // Stuck & Area Analysis Engine
        if (gotStuck) {
            analyzeAndExecuteUnstuckPlan(target);
            this.moveNoProgressTicks = 0;
        }
    }

    public void stopMovement() {
        this.zza = 0.0f;
        this.xxa = 0.0f;
        this.setJumping(false);
        this.setSprinting(false);
        this.activeBypassWaypoint = null;
        this.bypassWaypointTicks = 0;
        this.moveNoProgressTicks = 0;
        this.bestMoveTargetDistSq = Double.MAX_VALUE;
        this.lastMoveTarget = null;
        this.insanityCheckPos = null;
        this.insanityStuckTicks = 0;
    }

    private void lookAtPos(Vec3 target) {
        Vec3 eye = this.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) (-(Mth.atan2(dy, horiz) * (180.0 / Math.PI)));
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);
    }

    private void broadcastSwing() {
        this.level().getChunkSource().sendToTrackingPlayers(this, new ClientboundAnimatePacket(this, 0));
    }

    // === INVENTORY & EQUIPMENT HELPERS ===

    private void equipBestToolFor(BlockState state) {
        int bestSlot = -1;
        float bestSpeed = this.getMainHandItem().getDestroySpeed(state);

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty()) {
                float speed = s.getDestroySpeed(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1) {
            swapToMainHand(bestSlot);
        }
    }

    public boolean isBowItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof net.minecraft.world.item.BowItem || stack.getItem() instanceof net.minecraft.world.item.CrossbowItem) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
        return id.contains("bow");
    }

    public boolean hasBow() {
        if (isBowItem(this.getMainHandItem()) || isBowItem(this.getOffhandItem())) return true;
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            if (isBowItem(this.getInventory().getItem(i))) return true;
        }
        return false;
    }

    public boolean hasArrows() {
        // AI Companions with a bow always have infinite standard arrow reserve fallback
        return true;
    }

    private boolean isArrowItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW);
    }

    public boolean hasBowAndArrows() {
        return hasBow();
    }

    public boolean hasMeleeWeapon() {
        if (isMeleeWeapon(this.getMainHandItem())) return true;
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (isMeleeWeapon(s)) return true;
        }
        return false;
    }

    private boolean isMeleeWeapon(ItemStack s) {
        if (s.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
        return id.contains("sword") || (id.contains("axe") && !id.contains("pickaxe")) || id.contains("mace") || id.contains("trident");
    }

    public void equipBow() {
        if (isBowItem(this.getMainHandItem())) return;
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            if (isBowItem(this.getInventory().getItem(i))) {
                swapToMainHand(i);
                return;
            }
        }
    }

    private void equipBestMeleeWeapon() {
        int bestSlot = -1;
        int bestScore = isMeleeWeapon(this.getMainHandItem()) ? getWeaponScore(this.getMainHandItem()) : 0;
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (isMeleeWeapon(s)) {
                int score = getWeaponScore(s);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
        }
        if (bestSlot != -1) {
            swapToMainHand(bestSlot);
        }
    }

    private ItemStack getArrowStackForFiring() {
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (isArrowItem(s)) return s.copy();
        }
        return new ItemStack(Items.ARROW);
    }

    private void consumeArrow() {
        if (this.isCreative()) return;
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (isArrowItem(s)) {
                s.shrink(1);
                if (s.isEmpty()) {
                    this.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    private void equipBestWeapon() {
        int bestSlot = -1;
        int bestScore = getWeaponScore(this.getMainHandItem());

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            int score = getWeaponScore(s);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            swapToMainHand(bestSlot);
        }
    }

    private int getWeaponScore(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase();
        int tier = id.contains("netherite") ? 50 : id.contains("diamond") ? 40 : id.contains("iron") ? 30 : id.contains("stone") ? 20 : id.contains("wooden") ? 10 : 5;
        if (id.contains("sword")) return tier + 8;
        if (id.contains("axe") && !id.contains("pickaxe")) return tier + 6;
        if (id.contains("trident") || id.contains("mace")) return 45;
        return 1;
    }

    private void equipBestGear(String targetQuery) {
        autoEquipArmor();
        if (targetQuery == null || targetQuery.isBlank() || "best".equalsIgnoreCase(targetQuery)) {
            equipBestWeapon();
            return;
        }
        String q = targetQuery.toLowerCase().replace("minecraft:", "").trim();
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase().contains(q)) {
                swapToMainHand(i);
                return;
            }
        }
    }

    private void autoEquipArmor() {
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            Equippable eq = s.get(DataComponents.EQUIPPABLE);
            if (eq != null) {
                EquipmentSlot slot = eq.slot();
                if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
                    ItemStack current = this.getItemBySlot(slot);
                    if (current.isEmpty() || getArmorTierScore(s) > getArmorTierScore(current)) {
                        ItemStack toEquip = s.split(1);
                        this.setItemSlot(slot, toEquip);
                        if (!current.isEmpty()) {
                            this.getInventory().add(current);
                        }
                    }
                }
            } else if (s.is(Items.SHIELD) && this.getOffhandItem().isEmpty()) {
                this.setItemSlot(EquipmentSlot.OFFHAND, s.split(1));
            }
        }
    }

    private int getArmorTierScore(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
        if (id.contains("netherite")) return 6;
        if (id.contains("diamond")) return 5;
        if (id.contains("iron")) return 4;
        if (id.contains("chainmail") || id.contains("gold")) return 3;
        if (id.contains("copper")) return 2;
        return 1;
    }

    private void swapToMainHand(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 9) {
            this.getInventory().setSelectedSlot(slotIndex);
        } else if (slotIndex >= 9 && slotIndex < this.getInventory().getContainerSize()) {
            int hotbarSlot = this.getInventory().getSelectedSlot();
            ItemStack inHotbar = this.getInventory().getItem(hotbarSlot);
            ItemStack inInv = this.getInventory().getItem(slotIndex);
            this.getInventory().setItem(hotbarSlot, inInv);
            this.getInventory().setItem(slotIndex, inHotbar);
        }
    }

    private void tryAutoEat() {
        if (this.getFoodData().needsFood() || (this.getHealth() < this.getMaxHealth() - 2.0f && this.getFoodData().getFoodLevel() < 20)) {
            forceEatFromInventory();
        }
    }

    public @Nullable BotAction getCurrentAction() {
        return this.currentAction;
    }

    public void forceEatFromInventory() {
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty()) {
                FoodProperties food = s.get(DataComponents.FOOD);
                if (food != null) {
                    this.getFoodData().eat(food);
                    this.heal(2.0f);
                    s.shrink(1);
                    broadcastSwing();
                    return;
                }
            }
        }
    }

    private void vacuumNearbyDrops() {
        AABB box = this.getBoundingBox().inflate(2.5, 1.5, 2.5);
        for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, box)) {
            if (item.isAlive() && !item.hasPickUpDelay()) {
                ItemStack stack = item.getItem();
                if (this.getInventory().add(stack)) {
                    item.discard();
                }
            }
        }
    }

    private boolean isLog(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
        return id.contains("_log") || id.contains("_stem") || id.contains("_wood");
    }

    private boolean isPlank(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
        return id.contains("_planks");
    }

    private boolean isCobble(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
        return id.contains("cobblestone") || id.contains("cobbled_deepslate") || id.contains("blackstone");
    }

    public int countMatching(Predicate<ItemStack> predicate) {
        int total = 0;
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && predicate.test(s)) {
                total += s.getCount();
            }
        }
        return total;
    }

    public boolean consumeMatching(Predicate<ItemStack> predicate, int amount) {
        if (countMatching(predicate) < amount) return false;
        int remaining = amount;
        for (int i = 0; i < this.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && predicate.test(s)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
        return true;
    }

    private void giveToSelf(ItemStack stack) {
        if (!this.getInventory().add(stack)) {
            tossStackTowards(stack, this.position());
        }
    }

    // === WORLD QUERIES ===

    private @Nullable ServerPlayer findPlayer(String name) {
        if (name == null || name.isBlank()) return null;
        for (ServerPlayer p : this.level().players()) {
            if (p != this && p.getPlainTextName().equalsIgnoreCase(name.trim())) {
                return p;
            }
        }
        for (ServerPlayer p : this.level().players()) {
            if (p != this && p.getPlainTextName().toLowerCase().contains(name.trim().toLowerCase())) {
                return p;
            }
        }
        return null;
    }

    private boolean isBlockExposedToAir(BlockPos p) {
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            BlockPos neighbor = p.relative(d);
            BlockState s = this.level().getBlockState(neighbor);
            if (s.isAir() || s.canBeReplaced()) return true;
        }
        return false;
    }

    private @Nullable BlockPos findNearestMatchingBlock(String query, int radius) {
        BlockPos searchCenter = (this.mineZoneCenter != null && this.mineZoneRadius > 0) ? this.mineZoneCenter : this.blockPosition();
        int r = (this.mineZoneCenter != null && this.mineZoneRadius > 0) ? this.mineZoneRadius : radius;

        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int dy = 8; dy >= -8; dy--) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = searchCenter.offset(dx, dy, dz);
                    if (this.mineZoneCenter != null && this.mineZoneCenter.distSqr(pos) > (r * r + 2)) continue;

                    BlockState state = this.level().getBlockState(pos);
                    if (!state.isAir() && matchesBlockQuery(state, query)) {
                        boolean exposed = isBlockExposedToAir(pos);
                        double distFromBot = this.distanceToSqr(Vec3.atCenterOf(pos));
                        double score = distFromBot - (dy * 3.0) + (exposed ? 0.0 : 1000.0);
                        if (score < bestScore) {
                            bestScore = score;
                            best = pos.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean matchesBlockQuery(BlockState state, String query) {
        if (query == null || query.isBlank()) return false;
        String q = query.toLowerCase().replace("minecraft:", "").trim();
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase();
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase();

        // SPECIAL: For crops (wheat, carrots, potatoes, beetroots), NEVER match immature crops!
        if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) {
            if (q.contains("wheat") || q.contains("crop") || q.contains("carrot") || q.contains("potato") || q.contains("beetroot")) {
                return crop.isMaxAge(state);
            }
        }

        if (blockId.equals(q) || path.equals(q)) {
            return true;
        }
        if (q.contains("log") || q.contains("tree") || q.contains("trees") || q.contains("wood") || q.contains("stem")) {
            return path.contains("_log") || path.contains("_stem") || path.contains("_wood") || state.is(net.minecraft.tags.BlockTags.LOGS);
        }
        if (q.equals("stone") || q.equals("cobblestone") || q.equals("cobble")) {
            return path.equals("stone") || path.equals("cobblestone") || path.equals("deepslate")
                    || path.equals("cobbled_deepslate") || path.equals("andesite") || path.equals("diorite")
                    || path.equals("granite") || path.equals("tuff");
        }
        if (q.equals("dirt") || q.equals("grass")) {
            return path.equals("dirt") || path.equals("grass_block") || path.equals("coarse_dirt")
                    || path.equals("podzol") || path.equals("dirt_path") || path.equals("rooted_dirt");
        }
        if (q.equals("sand")) {
            return path.equals("sand") || path.equals("red_sand");
        }
        if (q.equals("gravel")) {
            return path.equals("gravel");
        }
        if (q.equals("iron") || q.contains("iron_ore")) return path.contains("iron_ore");
        if (q.equals("coal") || q.contains("coal_ore")) return path.contains("coal_ore");
        if (q.equals("diamond") || q.contains("diamond_ore")) return path.contains("diamond_ore");
        if (q.equals("gold") || q.contains("gold_ore")) return path.contains("gold_ore");
        if (q.equals("copper") || q.contains("copper_ore")) return path.contains("copper_ore");
        return path.contains(q);
    }

    public @Nullable Container getContainerAt(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock chestBlock) {
            Container c = net.minecraft.world.level.block.ChestBlock.getContainer(chestBlock, state, this.level(), pos, true);
            if (c != null) return c;
        }
        BlockEntity be = this.level().getBlockEntity(pos);
        if (be instanceof Container c) {
            return c;
        }
        return null;
    }

    public boolean containerHasAnyFreeSpace(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty() || slot.getCount() < slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public List<BlockPos> findAllNearbyContainers(int radius, boolean mustHaveSpace) {
        BlockPos origin = this.blockPosition();
        List<BlockPos> results = new ArrayList<>();
        java.util.Set<BlockPos> seenDoubleChestHalves = new java.util.HashSet<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    Container c = getContainerAt(pos);
                    if (c != null) {
                        if (this.level().getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
                            boolean alreadySeen = false;
                            for (BlockPos seen : seenDoubleChestHalves) {
                                if (pos.distManhattan(seen) == 1 && getContainerAt(seen) == c) {
                                    alreadySeen = true;
                                    break;
                                }
                            }
                            if (alreadySeen) continue;
                            seenDoubleChestHalves.add(pos);
                        }

                        if (mustHaveSpace && !containerHasAnyFreeSpace(c)) {
                            continue;
                        }
                        results.add(pos.immutable());
                    }
                }
            }
        }
        results.sort(Comparator.comparingDouble(p -> this.distanceToSqr(Vec3.atCenterOf(p))));
        return results;
    }

    private @Nullable BlockPos findNearestContainerBlock(int radius) {
        List<BlockPos> list = findAllNearbyContainers(radius, false);
        return list.isEmpty() ? null : list.get(0);
    }

    private @Nullable BlockPos findNearestAvailableContainerBlock(int radius) {
        List<BlockPos> list = findAllNearbyContainers(radius, true);
        return list.isEmpty() ? null : list.get(0);
    }

    public int depositFarmerCropsIntoChest(BlockPos chestPos) {
        Container container = getContainerAt(chestPos);
        if (container == null) return 0;
        lookAtPos(Vec3.atCenterOf(chestPos));

        // Auto-compact wheat to hay bales before storing to maximize chest space
        if (this.autoCompactWheatToHay) {
            compactWheatInInventory();
        }

        int totalStored = 0;

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase(Locale.ROOT);

            // Reserve seeds and rations
            if (id.contains("seed")) {
                int seedCount = countMatching(item -> BuiltInRegistries.ITEM.getKey(item.getItem()).toString().contains("seed"));
                if (seedCount <= this.maxSeedsToKeepOnHand) continue;

                int seedsInChest = countItemsInContainer(container, item -> BuiltInRegistries.ITEM.getKey(item.getItem()).toString().contains("seed"));
                int maxCanInsert = Math.max(0, this.maxSeedsInChest - seedsInChest);
                if (maxCanInsert <= 0) continue; // Chest has reached max seeds! Excess seeds stay in inventory for composting.

                int availableToDeposit = seedCount - this.maxSeedsToKeepOnHand;
                int toDeposit = Math.min(s.getCount(), Math.min(maxCanInsert, availableToDeposit));
                if (toDeposit <= 0) continue;

                ItemStack toInsert = s.split(toDeposit);
                int countBefore = toInsert.getCount();
                ItemStack remainder = insertIntoContainer(container, toInsert);
                int inserted = countBefore - remainder.getCount();
                if (!remainder.isEmpty()) {
                    s.grow(remainder.getCount());
                }
                totalStored += inserted;
                if (!containerHasAnyFreeSpace(container)) {
                    break;
                }
                continue;
            }
            if (id.contains("bone_meal") || id.contains("bonemeal")) continue;
            if (id.contains("carrot")) {
                int cCount = countMatching(item -> BuiltInRegistries.ITEM.getKey(item.getItem()).toString().contains("carrot"));
                if (cCount <= 16) continue;
            }
            if (id.contains("potato")) {
                int pCount = countMatching(item -> BuiltInRegistries.ITEM.getKey(item.getItem()).toString().contains("potato"));
                if (pCount <= 16) continue;
            }

            boolean isFarmProduce = id.contains("wheat") || id.contains("hay") || id.contains("carrot") || id.contains("potato")
                    || id.contains("beetroot") || id.contains("melon") || id.contains("pumpkin");

            if (isFarmProduce) {
                int countBefore = s.getCount();
                ItemStack remainder = insertIntoContainer(container, s.copy());
                int inserted = countBefore - remainder.getCount();
                if (inserted > 0) {
                    s.shrink(inserted);
                    totalStored += inserted;
                }
                if (!containerHasAnyFreeSpace(container)) {
                    break;
                }
            }
        }
        if (totalStored > 0) {
            sayInChat("Deposited " + totalStored + "x farm items into the chest.");
        }
        return totalStored;
    }

    public static int countItemsInContainer(Container container, Predicate<ItemStack> predicate) {
        if (container == null) return 0;
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && predicate.test(slot)) {
                total += slot.getCount();
            }
        }
        return total;
    }

    public @Nullable BlockPos findNearestComposterBlock(int radius) {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (this.level().getBlockState(p).is(Blocks.COMPOSTER)) {
                        double d = this.distanceToSqr(Vec3.atCenterOf(p));
                        if (d < bestDistSq) {
                            bestDistSq = d;
                            best = p;
                        }
                    }
                }
            }
        }
        return best;
    }

    public int compostExcessSeeds(BlockPos composterPos, int maxSeedsToKeep) {
        BlockState state = this.level().getBlockState(composterPos);
        if (!state.is(Blocks.COMPOSTER)) return 0;

        int composted = 0;
        if (state.getValue(ComposterBlock.LEVEL) >= 8) {
            state = ComposterBlock.extractProduce(this, state, this.level(), composterPos);
            vacuumNearbyDrops();
        }

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
            if (!id.contains("seed")) continue;

            while (!stack.isEmpty()) {
                int totalSeeds = countMatching(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString().contains("seed"));
                if (totalSeeds <= maxSeedsToKeep) {
                    break;
                }

                if (state.getValue(ComposterBlock.LEVEL) >= 8) {
                    state = ComposterBlock.extractProduce(this, state, this.level(), composterPos);
                    vacuumNearbyDrops();
                }

                int prevCount = stack.getCount();
                state = ComposterBlock.insertItem(this, state, (ServerLevel) this.level(), stack, composterPos);
                int used = prevCount - stack.getCount();
                if (used > 0) {
                    composted += used;
                } else {
                    break;
                }
            }

            int totalSeeds = countMatching(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString().contains("seed"));
            if (totalSeeds <= maxSeedsToKeep) {
                break;
            }
        }

        if (state.getValue(ComposterBlock.LEVEL) >= 8) {
            state = ComposterBlock.extractProduce(this, state, this.level(), composterPos);
            vacuumNearbyDrops();
        }

        if (composted > 0) {
            sayInChat("Composted " + composted + "x surplus seeds into the composter. Retaining " + maxSeedsToKeep + " seeds for planting!");
        }
        return composted;
    }

    public void setLastInteractingPlayer(@Nullable ServerPlayer player) {
        this.lastInteractingPlayer = player;
    }

    public @Nullable ServerPlayer getLastInteractingPlayer() {
        return this.lastInteractingPlayer;
    }

    private boolean isMobReachableAndValid(LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (this.unreachableMobBlacklist.containsKey(target.getId()) && this.tickCount < this.unreachableMobBlacklist.get(target.getId())) return false;

        double vertDiff = Math.abs(target.getY() - this.getY());
        if (vertDiff > 28.0) return false;

        double hdx = target.getX() - this.getX();
        double hdz = target.getZ() - this.getZ();
        double horizDistSq = hdx * hdx + hdz * hdz;

        // If directly above or below the bot through solid floor/ceiling without line of sight, ignore
        if (horizDistSq < 4.0 && vertDiff > 2.2 && !this.hasLineOfSight(target)) {
            return false;
        }

        return true;
    }

    public boolean isProvokedThreat(Monster m, @Nullable ServerPlayer protectedPlayer) {
        if (!isMobReachableAndValid(m)) return false;
        // 1. Is the monster actively targeting this bot?
        if (m.getTarget() == this) return true;
        // 2. Did the monster hurt this bot recently?
        if (this.getLastHurtByMob() == m) return true;
        // 3. Is the monster actively targeting, hurting, or in close proximity to protected player?
        if (protectedPlayer != null) {
            if (m.getTarget() == protectedPlayer) return true;
            if (protectedPlayer.getLastHurtByMob() == m) return true;
            // Any hostile monster within 14 blocks of protected player is an imminent threat
            if (m.distanceToSqr(protectedPlayer) < 196.0) return true;
            if (m instanceof net.minecraft.world.entity.monster.Creeper creeper) {
                if (creeper.getSwellDir() > 0 && creeper.distanceToSqr(protectedPlayer) < 64.0) {
                    return true;
                }
            }
        }
        // 4. Is it a swelling creeper in immediate proximity of the bot?
        if (m instanceof net.minecraft.world.entity.monster.Creeper creeper) {
            if (creeper.getSwellDir() > 0 && creeper.distanceToSqr(this) < 64.0) {
                return true;
            }
        }
        return false;
    }

    public @Nullable Monster findNearestProvokedThreatNear(BlockPos center, double radius, @Nullable ServerPlayer protectedPlayer) {
        AABB box = new AABB(center).inflate(radius, Math.max(24.0, radius * 0.5), radius);
        boolean hasBow = hasBow();
        List<Monster> monsters = this.level().getEntitiesOfClass(Monster.class, box, m -> {
            if (!isProvokedThreat(m, protectedPlayer)) return false;
            if (!hasBow && isLineBlockedByFence(this.position(), m.position())) {
                return false;
            }
            if (hasBow && !canShootAtTarget(m)) {
                return false;
            }
            return true;
        });
        monsters.sort(Comparator.comparingDouble(this::distanceToSqr));
        return monsters.isEmpty() ? null : monsters.get(0);
    }

    private @Nullable Monster findNearestMonsterNear(BlockPos center, double radius) {
        AABB box = new AABB(center).inflate(radius, Math.max(24.0, radius * 0.5), radius);
        boolean hasBow = hasBow();
        List<Monster> monsters = this.level().getEntitiesOfClass(Monster.class, box, m -> {
            if (!isMobReachableAndValid(m)) return false;
            if (!hasBow && isLineBlockedByFence(this.position(), m.position())) {
                return false;
            }
            if (hasBow && !canShootAtTarget(m)) {
                return false;
            }
            return true;
        });
        monsters.sort(Comparator.comparingDouble(this::distanceToSqr));
        return monsters.isEmpty() ? null : monsters.get(0);
    }

    private @Nullable LivingEntity findNearestTargetMob(String query, double radius) {
        double effectiveRadius = Math.max(48.0, radius);
        ServerPlayer commander = this.lastInteractingPlayer;
        if (commander != null && (commander.isRemoved() || commander.level() != this.level())) {
            commander = null;
        }

        // 1. Highest priority: mob directly in commander's crosshairs
        if (commander != null) {
            LivingEntity crossMob = GeminiService.getEntityInCrosshairs(commander, effectiveRadius);
            if (crossMob != null && crossMob.isAlive() && matchesMobQuery(crossMob, query)) {
                return crossMob;
            }
        }

        // 2. High priority: mob directly in commander's front field of view
        if (commander != null) {
            List<LivingEntity> inFront = GeminiService.getEntitiesInFrontCone(commander, effectiveRadius, 0.35);
            for (LivingEntity fe : inFront) {
                if (fe.isAlive() && matchesMobQuery(fe, query)) {
                    return fe;
                }
            }
        }

        // 3. Search in 3D volume around both bot and commander
        AABB box = this.getBoundingBox().inflate(effectiveRadius, 32.0, effectiveRadius);
        List<LivingEntity> list = new ArrayList<>(this.level().getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == this || e instanceof Player || !e.isAlive()) return false;
            if (this.unreachableMobBlacklist.containsKey(e.getId()) && this.tickCount < this.unreachableMobBlacklist.get(e.getId())) return false;
            return matchesMobQuery(e, query);
        }));

        if (commander != null) {
            AABB cmdBox = commander.getBoundingBox().inflate(effectiveRadius, 32.0, effectiveRadius);
            List<LivingEntity> cmdList = this.level().getEntitiesOfClass(LivingEntity.class, cmdBox, e -> {
                if (e == this || e instanceof Player || !e.isAlive()) return false;
                if (this.unreachableMobBlacklist.containsKey(e.getId()) && this.tickCount < this.unreachableMobBlacklist.get(e.getId())) return false;
                return matchesMobQuery(e, query);
            });
            for (LivingEntity e : cmdList) {
                if (!list.contains(e)) {
                    list.add(e);
                }
            }
        }

        if (list.isEmpty()) return null;

        // Sort candidates:
        // Prioritize monsters actively targeting commander or bot, then by distance to bot/commander
        final ServerPlayer finalCommander = commander;
        list.sort((e1, e2) -> {
            boolean threat1 = isProvokedThreatTarget(e1, finalCommander);
            boolean threat2 = isProvokedThreatTarget(e2, finalCommander);
            if (threat1 && !threat2) return -1;
            if (!threat1 && threat2) return 1;

            double d1 = this.distanceToSqr(e1);
            double d2 = this.distanceToSqr(e2);
            if (finalCommander != null) {
                d1 = Math.min(d1, finalCommander.distanceToSqr(e1));
                d2 = Math.min(d2, finalCommander.distanceToSqr(e2));
            }
            return Double.compare(d1, d2);
        });

        return list.get(0);
    }

    private boolean matchesMobQuery(LivingEntity e, String query) {
        if (query.isEmpty() || "hostile".equals(query) || "monsters".equals(query) || "mobs".equals(query)) {
            return e instanceof Monster;
        }
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().toLowerCase();
        return typeId.contains(query);
    }

    private boolean isProvokedThreatTarget(LivingEntity e, @Nullable ServerPlayer protectedPlayer) {
        if (e instanceof net.minecraft.world.entity.Mob mob) {
            if (mob.getTarget() == this) return true;
            if (protectedPlayer != null && mob.getTarget() == protectedPlayer) return true;
        }
        if (this.getLastHurtByMob() == e) return true;
        if (protectedPlayer != null && protectedPlayer.getLastHurtByMob() == e) return true;
        return false;
    }

    public @Nullable Monster findSentryTarget(double radius) {
        AABB box = this.getBoundingBox().inflate(radius, 16.0, radius);
        List<Monster> monsters = this.level().getEntitiesOfClass(Monster.class, box, m -> {
            if (!m.isAlive() || m.isSpectator()) return false;
            if (this.unreachableMobBlacklist.containsKey(m.getId()) && this.tickCount < this.unreachableMobBlacklist.get(m.getId())) {
                return false;
            }
            return canShootAtTarget(m);
        });
        if (monsters.isEmpty()) return null;
        monsters.sort((m1, m2) -> {
            boolean c1 = (m1 instanceof net.minecraft.world.entity.monster.Creeper c && c.getSwellDir() > 0);
            boolean c2 = (m2 instanceof net.minecraft.world.entity.monster.Creeper c && c.getSwellDir() > 0);
            if (c1 && !c2) return -1;
            if (c2 && !c1) return 1;
            return Double.compare(this.distanceToSqr(m1), this.distanceToSqr(m2));
        });
        return monsters.get(0);
    }

    private @Nullable ItemEntity findNearestDrop(double radius) {
        AABB box = this.getBoundingBox().inflate(radius);
        List<ItemEntity> drops = this.level().getEntitiesOfClass(ItemEntity.class, box, e -> {
            if (!e.isAlive()) return false;
            BlockPos pos = e.blockPosition();
            return !isHazardousBlockOrFluid(this.level().getBlockState(pos), this.level().getFluidState(pos));
        });
        drops.sort(Comparator.comparingDouble(this::distanceToSqr));
        return drops.isEmpty() ? null : drops.get(0);
    }

    // === SAFE SERVER PLAYER OVERRIDES ===

    @Override
    public void sendSystemMessage(Component chatComponent, boolean actionBar) {
    }

    @Override
    public void awardStat(Stat<?> stat, int amount) {
    }

    @Override
    public void updateOptions(ClientInformation information) {
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider menuProvider, @Nullable Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
        return OptionalInt.empty();
    }

    @Override
    public void openHorseInventory(AbstractHorse horse, Container container) {
    }

    public void openCompanionGui(ServerPlayer player) {
        net.minecraft.world.inventory.SimpleContainerData data = new net.minecraft.world.inventory.SimpleContainerData(com.aicompanion.bot.menu.BotDashboardMenu.DATA_COUNT);
        data.set(0, (int) (this.getHealth() * 10.0f));
        data.set(1, (int) (this.getMaxHealth() * 10.0f));
        data.set(2, this.getFoodData().getFoodLevel());
        data.set(3, getRoleCode(this.botRole));
        data.set(4, getActionCode(this.currentAction));
        data.set(5, this.actionProgressCount);
        data.set(6, this.currentAction != null ? this.currentAction.count : 0);
        data.set(7, this.assignedAreaName != null ? 1 : 0);

        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (containerId, playerInv, p) -> new com.aicompanion.bot.menu.BotDashboardMenu(containerId, playerInv, this.getInventory(), data, this),
                Component.literal("Companion: " + this.getBotName())
        ));
    }

    private static int getRoleCode(String role) {
        String r = role.toLowerCase();
        if (r.contains("guard") || r.contains("bodyguard")) return 1;
        if (r.contains("farm")) return 2;
        if (r.contains("mine")) return 3;
        if (r.contains("lumber")) return 4;
        if (r.contains("build")) return 5;
        if (r.contains("husbandry") || r.contains("ranch")) return 6;
        if (r.contains("conductor") || r.contains("train")) return 7;
        return 0;
    }

    private static int getActionCode(@Nullable BotAction action) {
        if (action == null) return 0;
        String t = action.getNormalizedType();
        if ("FOLLOW".equals(t)) return 1;
        if ("GUARD".equals(t)) return 2;
        if ("FARM".equals(t)) return 3;
        if ("MINE".equals(t) || "EXCAVATE".equals(t)) return 4;
        if ("BUILD".equals(t) || "BUILD_HOUSE".equals(t) || "BUILD_BLUEPRINT".equals(t)) return 5;
        if ("CONDUCT_TRAIN".equals(t)) return 6;
        return 0;
    }

    @Override
    public void die(DamageSource source) {
        this.clearActions();
        this.dropAllDeathLoot(this.level(), source);
        sayInChat("I died! Respawning safely on dry ground...");
        this.setHealth(this.getMaxHealth());
        this.getFoodData().setFoodLevel(20);
        this.setAirSupply(this.getMaxAirSupply());
        this.clearFire();
        this.setInvulnerableTime(60);
        this.setDeltaMovement(Vec3.ZERO);

        ServerLevel sLevel = (ServerLevel) this.level();
        BlockPos safePos = null;

        // 1. Try near nearest player
        Player nearest = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 256.0, p -> p != this && !(p instanceof AiBotPlayer));
        if (nearest != null) {
            safePos = findSafeDryRespawnPos(sLevel, nearest.blockPosition(), 8);
        }

        // 2. Try near guard anchor or farm origin
        if (safePos == null && this.guardAnchorPos != null) {
            safePos = findSafeDryRespawnPos(sLevel, this.guardAnchorPos, 12);
        }
        if (safePos == null && this.farmOriginPos != null) {
            safePos = findSafeDryRespawnPos(sLevel, this.farmOriginPos, 12);
        }

        // 3. Try near current position (within 24m)
        if (safePos == null) {
            safePos = findSafeDryRespawnPos(sLevel, this.blockPosition(), 24);
        }

        // 4. Fall back to world spawn
        if (safePos == null) {
            safePos = findSafeDryRespawnPos(sLevel, sLevel.getRespawnData().pos(), 16);
        }

        if (safePos != null) {
            this.teleportTo(safePos.getX() + 0.5, safePos.getY() + 0.05, safePos.getZ() + 0.5);
        } else if (nearest != null) {
            this.teleportTo(nearest.getX() + 0.5, nearest.getY() + 0.1, nearest.getZ() + 0.5);
        }
    }

    public void escapeWaterToDryLand() {
        BlockPos current = this.blockPosition();
        BlockPos nearestBank = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = -2; dy <= 4; dy++) {
                    BlockPos p = current.offset(dx, dy, dz);
                    if (this.level().getFluidState(p).isEmpty() && !this.level().getBlockState(p).getCollisionShape(this.level(), p).isEmpty()) {
                        BlockPos feet = p.above();
                        BlockPos head = feet.above();
                        if (this.level().getFluidState(feet).isEmpty() && this.level().getBlockState(feet).getCollisionShape(this.level(), feet).isEmpty()
                                && this.level().getFluidState(head).isEmpty() && this.level().getBlockState(head).getCollisionShape(this.level(), head).isEmpty()) {
                            double d = this.distanceToSqr(Vec3.atCenterOf(feet));
                            if (d < bestDistSq) {
                                bestDistSq = d;
                                nearestBank = feet;
                            }
                        }
                    }
                }
            }
        }

        if (nearestBank != null) {
            Vec3 target = Vec3.atCenterOf(nearestBank);
            lookAtPos(target);
            this.setJumping(true);
            Vec3 m = this.getDeltaMovement();
            Vec3 dir = target.subtract(this.position()).normalize().scale(0.22);
            this.setDeltaMovement(dir.x, Math.max(m.y, 0.15), dir.z);
        } else {
            this.setJumping(true);
            Vec3 m = this.getDeltaMovement();
            this.setDeltaMovement(m.x, Math.max(m.y, 0.18), m.z);
        }
    }

    public void escapeHazardousFluidToDryLand() {
        BlockPos current = this.blockPosition();
        BlockPos nearestBank = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                for (int dy = -2; dy <= 4; dy++) {
                    BlockPos p = current.offset(dx, dy, dz);
                    BlockState groundState = this.level().getBlockState(p);
                    FluidState groundFluid = this.level().getFluidState(p);
                    if (!isHazardousBlockOrFluid(groundState, groundFluid) && !groundState.getCollisionShape(this.level(), p).isEmpty()) {
                        BlockPos feet = p.above();
                        BlockPos head = feet.above();
                        BlockState footState = this.level().getBlockState(feet);
                        FluidState footFluid = this.level().getFluidState(feet);
                        BlockState headState = this.level().getBlockState(head);
                        FluidState headFluid = this.level().getFluidState(head);
                        if (!isHazardousBlockOrFluid(footState, footFluid) && footState.getCollisionShape(this.level(), feet).isEmpty()
                                && !isHazardousBlockOrFluid(headState, headFluid) && headState.getCollisionShape(this.level(), head).isEmpty()) {
                            double d = this.distanceToSqr(Vec3.atCenterOf(feet));
                            if (d < bestDistSq) {
                                bestDistSq = d;
                                nearestBank = feet;
                            }
                        }
                    }
                }
            }
        }

        if (nearestBank != null) {
            Vec3 target = Vec3.atCenterOf(nearestBank);
            lookAtPos(target);
            this.setJumping(true);
            Vec3 m = this.getDeltaMovement();
            Vec3 dir = target.subtract(this.position()).normalize().scale(0.35);
            this.setDeltaMovement(dir.x, Math.max(m.y, 0.28), dir.z);
        } else {
            this.setJumping(true);
            Vec3 m = this.getDeltaMovement();
            this.setDeltaMovement(m.x, Math.max(m.y, 0.25), m.z);
        }
    }

    public static @Nullable BlockPos findSafeDryRespawnPos(ServerLevel level, BlockPos origin, int radius) {
        if (origin == null || level == null) return null;

        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    for (int dy = -4; dy <= 4; dy++) {
                        BlockPos footPos = origin.offset(dx, dy, dz);
                        BlockPos groundPos = footPos.below();
                        BlockPos headPos = footPos.above();

                        FluidState footFluid = level.getFluidState(footPos);
                        FluidState headFluid = level.getFluidState(headPos);
                        if (!footFluid.isEmpty() || !headFluid.isEmpty()) continue;
                        if (isHazardousFluid(footFluid) || isHazardousFluid(headFluid)) continue;

                        BlockState footState = level.getBlockState(footPos);
                        BlockState headState = level.getBlockState(headPos);
                        if (!footState.getCollisionShape(level, footPos).isEmpty() || !headState.getCollisionShape(level, headPos).isEmpty()) continue;
                        if (footState.is(Blocks.LAVA) || footState.is(Blocks.FIRE) || isHazardousBlockOrFluid(footState, footFluid)
                                || footState.is(Blocks.SWEET_BERRY_BUSH) || footState.is(Blocks.WITHER_ROSE) || footState.is(Blocks.POWDER_SNOW)) continue;

                        FluidState groundFluid = level.getFluidState(groundPos);
                        if (!groundFluid.isEmpty() || isHazardousFluid(groundFluid)) continue;

                        BlockState groundState = level.getBlockState(groundPos);
                        if (groundState.getCollisionShape(level, groundPos).isEmpty() && !groundState.isSolid()) continue;
                        if (groundState.is(Blocks.LAVA) || groundState.is(Blocks.MAGMA_BLOCK) || isHazardousBlockOrFluid(groundState, groundFluid)
                                || groundState.is(Blocks.CAMPFIRE) || groundState.is(Blocks.CACTUS)) continue;

                        return footPos;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (isInHazardousFluid()) {
            this.setJumping(true);
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, Math.max(motion.y, 0.28), motion.z);
            escapeHazardousFluidToDryLand();
            return false;
        }
        if (damageSource.is(DamageTypes.DROWN)) {
            this.setJumping(true);
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, Math.max(motion.y, 0.25), motion.z);
            escapeWaterToDryLand();
        }
        // Dynamic Shield Blocking: Check if bot is holding a shield and facing the attacker
        if (this.isBlocking()) {
            Entity directEntity = damageSource.getDirectEntity();
            if (directEntity != null) {
                Vec3 viewVec = this.getViewVector(1.0F);
                Vec3 diff = directEntity.position().vectorTo(this.position()).normalize();
                diff = new Vec3(diff.x, 0.0, diff.z);
                if (diff.dot(viewVec) < 0.0) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.SHIELD_BLOCK.value(),
                            SoundSource.PLAYERS, 1.0f, 1.0f);
                    return false;
                }
            }
        }

        boolean result = super.hurtServer(level, damageSource, amount);
        if (result && damageSource.getEntity() == null) {
            // Environmental hazard damage (fall, cactus, berry bush, fire, magma): penalize in Hive-Mind!
            String dim = this.level().dimension().identifier().toString();
            com.aicompanion.bot.learning.HiveMindBrain.recordNodeTraversal(dim, this.blockPosition(), -10.0f, true, false, false, null);
            com.aicompanion.bot.learning.HiveMindBrain.applyReinforcementSignal(this, -6.0f, false);
        }
        if (result && damageSource.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player)) {
            // Auto-raise shield if available to block consecutive hits
            if (this.getOffhandItem().is(Items.SHIELD)) {
                this.shieldBlockTicks = 20;
                this.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
            }

            // Non-guard workers broadcast distress call to nearby guards
            String role = this.botRole.toLowerCase();
            if (!role.contains("guard") && !role.contains("bodyguard") && !role.contains("warrior") && !role.contains("soldier")) {
                if (this.tickCount - this.lastDistressTick > 100) {
                    this.lastDistressTick = this.tickCount;
                    BotManager.broadcastDistress(this, attacker, this.blockPosition());
                }
            }
        }
        return result;
    }

    public void respondToDistressCall(LivingEntity threat, AiBotPlayer caller) {
        sayInChat("Backup is on the way, " + caller.getBotName() + "! Engaging " + threat.getType().getDescription().getString() + "!");
        this.actionQueue.clear();
        this.actionProgressCount = 0;
        this.actionTicks = 0;
        this.currentAction = new BotAction("ATTACK", threat.getType().getDescription().getString(), null, 1);
        engageTarget(threat);
    }

    public boolean handleBuilderChatCommand(String text, @Nullable ServerPlayer speaker) {
        String lower = text.toLowerCase(Locale.ROOT);
        BlockPos buildSite = this.blockPosition().relative(this.getDirection(), 2);

        // Check for custom saved blueprints in catalog
        for (CustomBlueprint bp : BotPersistence.getAllBlueprints().values()) {
            String bpName = bp.name.toLowerCase(Locale.ROOT);
            if (lower.contains("build " + bpName) || lower.contains("construct " + bpName) || lower.contains(bpName)) {
                startCustomBlueprintConstruction(bp, buildSite, this.getDirection());
                return true;
            }
        }

        if (lower.contains("watchtower") || lower.contains("tower") || lower.contains("bunker")) {
            startCustomConstruction("watchtower", buildSite, this.getDirection());
            return true;
        } else if (lower.contains("barn") || lower.contains("granary") || lower.contains("farmhouse")) {
            startCustomConstruction("barn", buildSite, this.getDirection());
            return true;
        } else if (lower.contains("forge") || lower.contains("blacksmith") || lower.contains("workshop")) {
            startCustomConstruction("forge", buildSite, this.getDirection());
            return true;
        } else if (lower.contains("build a house") || lower.contains("build house") || lower.contains("build home") || lower.contains("build cottage")) {
            startCustomConstruction("cottage", buildSite, this.getDirection());
            return true;
        }
        return false;
    }

    public boolean handleExcavationAndMiningChatCommand(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();

        // 1. Follow-up state when bot previously asked for hole dimensions
        if (this.awaitingExcavationDimensions && this.pendingExcavationOrigin != null) {
            if (lower.equals("cancel") || lower.contains("nevermind") || lower.contains("stop") || lower.contains("abort")) {
                this.awaitingExcavationDimensions = false;
                this.pendingExcavationOrigin = null;
                sayInChat("Excavation cancelled.");
                return true;
            }

            if (lower.contains("start here") || lower.contains("from here") || lower.contains("this spot") || lower.equals("here")) {
                this.pendingExcavationOrigin = resolveExcavationStartPos(speaker);
                this.pendingExcavationFacing = speaker != null ? speaker.getDirection() : this.getDirection();
                spawnExcavationParticles(this.pendingExcavationOrigin);
                int[] dims = parseExcavationDimensions(lower, false);
                if (dims != null) {
                    this.awaitingExcavationDimensions = false;
                    startBoxExcavation(this.pendingExcavationOrigin, this.pendingExcavationFacing, dims[0], dims[1], dims[2]);
                    return true;
                }
                sayInChat("Updated starting point to (" + this.pendingExcavationOrigin.getX() + ", " + this.pendingExcavationOrigin.getY() + ", " + this.pendingExcavationOrigin.getZ() + ")! How wide/long and how deep should I dig from here? (e.g. '5x5 and 4 deep')");
                return true;
            }

            int[] dims = parseExcavationDimensions(lower, true);
            if (dims != null) {
                this.awaitingExcavationDimensions = false;
                startBoxExcavation(this.pendingExcavationOrigin, this.pendingExcavationFacing, dims[0], dims[1], dims[2]);
                return true;
            }
        }

        // 2. Radius-constrained block type mining: e.g. "mine all the stone in this 10 block area"
        java.util.regex.Matcher areaMineMatcher = java.util.regex.Pattern.compile(
                "(?:mine|dig|clear|gather|harvest|break)\\s+(?:all\\s+)?(?:the\\s+)?([a-zA-Z0-9_ ]+?)\\s+(?:in|within|inside|around)\\s+(?:this\\s+|a\\s+|an\\s+)?(?:(\\d+)\\s*(?:blocks?|tiles?|meters?|m)?(?:\\s+(?:area|radius|zone|range|perimeter))?|(?:this|the)?\\s*(?:area|here|spot))",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
        if (areaMineMatcher.find()) {
            String blockQuery = areaMineMatcher.group(1).trim();
            if (!blockQuery.equalsIgnoreCase("hole") && !blockQuery.equalsIgnoreCase("pit") && !blockQuery.equalsIgnoreCase("trench")) {
                int radius = 10;
                String radGroup = areaMineMatcher.group(2);
                if (radGroup != null && !radGroup.isBlank()) {
                    try { radius = Math.min(64, Math.max(2, Integer.parseInt(radGroup.trim()))); } catch (Exception ignored) {}
                }
                BlockPos center = resolveExcavationStartPos(speaker);
                startRadiusMining(center, radius, blockQuery);
                return true;
            }
        }

        // 3. Hole / Pit / Box excavation command: "dig a hole", "dig a hole here", "excavate here", "dig a 10x10x2 box around us"
        boolean mentionsHole = lower.contains("hole") || lower.contains("pit") || lower.contains("trench") || lower.contains("quarry")
                || lower.contains("box") || lower.contains("cube") || lower.contains("room")
                || lower.contains("dig here") || lower.contains("excavate here") || lower.contains("dig right here")
                || (lower.contains("dig") && (lower.contains("deep") || lower.contains("x") || lower.contains("by") || lower.contains("tiles") || lower.contains("blocks")));

        if (mentionsHole && !lower.contains("train") && !lower.contains("conductor")) {
            BlockPos startPos = resolveExcavationStartPos(speaker);
            net.minecraft.core.Direction facing = speaker != null ? speaker.getDirection() : this.getDirection();

            // Check custom starting Y level: e.g. "starting y81", "starting at y 81", "y=81", "y 81", "y81"
            java.util.regex.Matcher yMatcher = java.util.regex.Pattern.compile("(?:starting\\s+(?:at\\s+)?y|at\\s+y|y\\s*=|y)\\s*([+-]?\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
            if (yMatcher.find()) {
                try {
                    int customY = Integer.parseInt(yMatcher.group(1));
                    startPos = new BlockPos(startPos.getX(), customY, startPos.getZ());
                } catch (Exception ignored) {}
            }

            boolean isCentered = lower.contains("around us") || lower.contains("around me") || lower.contains("around here")
                    || lower.contains("in each direction") || lower.contains("in all directions") || lower.contains("centered");
            if (isCentered && speaker != null) {
                startPos = new BlockPos(speaker.getBlockX(), startPos.getY(), speaker.getBlockZ());
            }

            boolean isUpwards = lower.contains("go up") || lower.contains("up ") || lower.contains("upwards")
                    || lower.contains("above") || lower.contains("height") || lower.contains("tall") || lower.contains("raise");

            spawnExcavationParticles(startPos);

            int[] dims = parseExcavationDimensions(lower, false);
            if (dims != null) {
                this.awaitingExcavationDimensions = false;
                startBoxExcavation(startPos, facing, dims[0], dims[1], dims[2], isUpwards, isCentered);
                return true;
            } else {
                this.pendingExcavationOrigin = startPos;
                this.pendingExcavationFacing = facing;
                this.awaitingExcavationDimensions = true;
                moveTowards(new Vec3(startPos.getX() + 0.5, startPos.getY() + 1.0, startPos.getZ() + 0.5), false);
                sayInChat("Marked starting point at (" + startPos.getX() + ", " + startPos.getY() + ", " + startPos.getZ()
                        + ")! How big and how deep should I dig from there? (e.g. '5x5 and 4 deep' or 'width 4 depth 6')");
                return true;
            }
        }

        return false;
    }

    public void startBoxExcavation(BlockPos origin, net.minecraft.core.Direction facing, int width, int length, int depth) {
        startBoxExcavation(origin, facing, width, length, depth, false, false);
    }

    public void startBoxExcavation(BlockPos origin, net.minecraft.core.Direction facing, int width, int length, int height, boolean isUpwards, boolean isCentered) {
        this.clearActions();
        this.activeExcavationQueue.clear();

        net.minecraft.core.Direction right = facing.getClockWise();
        int halfWidth = (width - 1) / 2;
        int halfLength = (length - 1) / 2;

        if (isCentered) {
            int minDy = isUpwards ? 0 : -(height - 1);
            int maxDy = isUpwards ? (height - 1) : 0;

            for (int dy = maxDy; dy >= minDy; dy--) {
                for (int dz = -halfLength; dz <= (length - 1 - halfLength); dz++) {
                    boolean serpentine = ((dz + halfLength) % 2 == 1);
                    int startDx = serpentine ? (width - 1 - halfWidth) : -halfWidth;
                    int endDx = serpentine ? -halfWidth : (width - 1 - halfWidth);
                    int step = serpentine ? -1 : 1;

                    for (int dx = startDx; serpentine ? (dx >= endDx) : (dx <= endDx); dx += step) {
                        BlockPos p = origin.offset(dx, dy, dz);
                        BlockState s = this.level().getBlockState(p);
                        if (!s.isAir() && s.getDestroySpeed(this.level(), p) >= 0) {
                            this.activeExcavationQueue.add(p);
                        }
                    }
                }
            }
        } else {
            if (!isUpwards) {
                for (int dz = 0; dz < length; dz++) {
                    for (int dx = -halfWidth; dx < width - halfWidth; dx++) {
                        BlockPos p = origin.relative(facing, dz).relative(right, dx).above(1);
                        BlockState s = this.level().getBlockState(p);
                        if (!s.isAir() && s.getDestroySpeed(this.level(), p) >= 0) {
                            this.activeExcavationQueue.add(p);
                        }
                    }
                }
            }

            int minDy = isUpwards ? 0 : -(height - 1);
            int maxDy = isUpwards ? (height - 1) : 0;

            for (int dy = maxDy; dy >= minDy; dy--) {
                for (int dz = 0; dz < length; dz++) {
                    boolean serpentine = (dz % 2 == 1);
                    int startDx = serpentine ? (width - 1 - halfWidth) : -halfWidth;
                    int endDx = serpentine ? -halfWidth : (width - 1 - halfWidth);
                    int step = serpentine ? -1 : 1;

                    for (int dx = startDx; serpentine ? (dx >= endDx) : (dx <= endDx); dx += step) {
                        BlockPos p = origin.relative(facing, dz).relative(right, dx).offset(0, dy, 0);
                        BlockState s = this.level().getBlockState(p);
                        if (!s.isAir() && s.getDestroySpeed(this.level(), p) >= 0) {
                            this.activeExcavationQueue.add(p);
                        }
                    }
                }
            }
        }

        String modeDesc = isUpwards ? (height + " high upwards") : (height + " deep");
        String centerDesc = isCentered ? " centered around (" : " from (";
        this.currentAction = new BotAction("EXCAVATE", width + "x" + length + " (" + modeDesc + ")", null, this.activeExcavationQueue.size());
        this.actionProgressCount = 0;
        this.actionTicks = 0;
        sayInChat("Starting systematic excavation of " + width + "x" + length + " box (" + modeDesc + ")" + centerDesc
                + origin.getX() + ", " + origin.getY() + ", " + origin.getZ() + ")! (" + this.activeExcavationQueue.size() + " blocks to dig)");
    }

    public void startRadiusMining(BlockPos center, int radius, String blockQuery) {
        this.clearActions();
        this.mineZoneCenter = center;
        this.mineZoneRadius = radius;

        int totalFound = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState s = this.level().getBlockState(p);
                    if (!s.isAir() && matchesBlockQuery(s, blockQuery)) {
                        totalFound++;
                    }
                }
            }
        }

        if (totalFound == 0) {
            this.mineZoneCenter = null;
            this.mineZoneRadius = 0;
            sayInChat("I scanned the " + radius + "-block area around (" + center.getX() + ", " + center.getY() + ", " + center.getZ()
                    + "), but didn't find any '" + blockQuery + "' to mine.");
            return;
        }

        this.currentAction = new BotAction("MINE", blockQuery, null, totalFound);
        this.actionProgressCount = 0;
        sayInChat("Locked onto " + radius + "-block area at (" + center.getX() + ", " + center.getY() + ", " + center.getZ()
                + ")! Found " + totalFound + "x " + blockQuery + ". Mining every block in this zone!");
    }

    public BlockPos resolveExcavationStartPos(@Nullable ServerPlayer speaker) {
        if (speaker != null) {
            net.minecraft.world.phys.HitResult hit = speaker.pick(7.0, 0.0f, false);
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                return bhr.getBlockPos();
            }
            return speaker.blockPosition().relative(speaker.getDirection(), 1).below();
        }
        return this.blockPosition().relative(this.getDirection(), 1).below();
    }

    private void spawnExcavationParticles(BlockPos pos) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
        }
    }

    public static int @Nullable [] parseExcavationDimensions(String text, boolean lenient) {
        if (text == null || text.isBlank()) return null;
        String s = text.toLowerCase(Locale.ROOT).trim();

        // 1. WxLxD or W by L by D (e.g. "5x5x4" or "5 by 5 by 4")
        java.util.regex.Matcher m3D = java.util.regex.Pattern.compile("(\\d+)\\s*(?:x|by)\\s*(\\d+)\\s*(?:x|by)\\s*(\\d+)").matcher(s);
        if (m3D.find()) {
            try {
                int w = Integer.parseInt(m3D.group(1));
                int l = Integer.parseInt(m3D.group(2));
                int d = Integer.parseInt(m3D.group(3));
                return new int[]{Mth.clamp(w, 1, 32), Mth.clamp(l, 1, 32), Mth.clamp(d, 1, 64)};
            } catch (Exception ignored) {}
        }

        // 2. WxL or W by L with optional depth (e.g. "5x5 and 4 deep", "5 by 5, 4 deep", "5x5")
        java.util.regex.Matcher m2D = java.util.regex.Pattern.compile(
                "(\\d+)\\s*(?:x|by)\\s*(\\d+)(?:.*?(?:and\\s+|depth\\s+)?(\\d+)\\s*(?:blocks?|tiles?|m)?\\s*deep)?").matcher(s);
        if (m2D.find()) {
            try {
                int w = Integer.parseInt(m2D.group(1));
                int l = Integer.parseInt(m2D.group(2));
                int d = 3;
                if (m2D.group(3) != null) {
                    d = Integer.parseInt(m2D.group(3));
                }
                return new int[]{Mth.clamp(w, 1, 32), Mth.clamp(l, 1, 32), Mth.clamp(d, 1, 64)};
            } catch (Exception ignored) {}
        }

        // 3. Named components: width W, length L, depth D (e.g. "width 4 depth 6", "4 wide and 5 deep")
        Integer w = null;
        Integer l = null;
        Integer d = null;

        java.util.regex.Matcher wM = java.util.regex.Pattern.compile("(?:width\\s+|wide\\s+)?(\\d+)\\s*(?:wide|width)").matcher(s);
        if (wM.find()) try { w = Integer.parseInt(wM.group(1)); } catch (Exception ignored) {}

        java.util.regex.Matcher lM = java.util.regex.Pattern.compile("(?:length\\s+|long\\s+)?(\\d+)\\s*(?:long|length)").matcher(s);
        if (lM.find()) try { l = Integer.parseInt(lM.group(1)); } catch (Exception ignored) {}

        java.util.regex.Matcher dM = java.util.regex.Pattern.compile("(?:depth\\s+|deep\\s+)?(\\d+)\\s*(?:deep|depth|down|tall|height)").matcher(s);
        if (dM.find()) try { d = Integer.parseInt(dM.group(1)); } catch (Exception ignored) {}

        if (w != null || l != null || d != null) {
            int finalW = w != null ? w : (l != null ? l : 3);
            int finalL = l != null ? l : finalW;
            int finalD = d != null ? d : 3;
            return new int[]{Mth.clamp(finalW, 1, 32), Mth.clamp(finalL, 1, 32), Mth.clamp(finalD, 1, 64)};
        }

        // 4. If answering conversational prompt leniently: single number "5" or "4 deep"
        if (lenient) {
            java.util.regex.Matcher singleM = java.util.regex.Pattern.compile("\\b(\\d+)\\b").matcher(s);
            if (singleM.find()) {
                try {
                    int val = Integer.parseInt(singleM.group(1));
                    if (s.contains("deep")) {
                        return new int[]{3, 3, Mth.clamp(val, 1, 64)};
                    }
                    return new int[]{Mth.clamp(val, 1, 32), Mth.clamp(val, 1, 32), Mth.clamp(val, 1, 64)};
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    public synchronized boolean handleFarmerChatCommand(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();
        if (lower.contains("tree") || lower.contains("wood") || lower.contains("log") || lower.contains("chop") || lower.contains("lumber")) {
            return false;
        }

        boolean mentionsWheat = lower.contains("wheat") || lower.contains("wheet");
        boolean mentionsHay = lower.contains("hay") || lower.contains("bale");
        boolean mentionsCompost = lower.contains("compost") || lower.contains("fertiliz");
        boolean mentionsSeed = lower.contains("seed");
        boolean mentionsCompact = lower.contains("combine") || lower.contains("craft") || lower.contains("turn")
                || lower.contains("compact") || lower.contains("make") || lower.contains("compress");

        boolean isNegation = lower.contains("stop") || lower.contains("don't") || lower.contains("dont")
                || lower.contains("do not") || lower.contains("never") || lower.contains("disable")
                || lower.contains("no more") || lower.contains("cease") || lower.contains("quit")
                || lower.contains("keep wheat") || lower.contains("keep as wheat") || lower.contains("as wheat")
                || lower.contains("breed with") || lower.contains("need wheat");

        boolean isHayCommand = (mentionsWheat && mentionsHay) || (mentionsHay && mentionsCompact)
                || (mentionsWheat && lower.contains("space")) || lower.contains("combine wheat") || lower.contains("compact wheat")
                || lower.contains("combine the wheat") || lower.contains("wheat into hay")
                || lower.contains("hay bale") || lower.contains("hay bales")
                || (mentionsWheat && (lower.contains("breed") || lower.contains("cow") || lower.contains("sheep")));
        boolean isUncompactCommand = lower.contains("uncraft hay") || lower.contains("turn hay") || lower.contains("convert hay") || lower.contains("hay to wheat") || lower.contains("hay into wheat");
        boolean isCompostCommand = mentionsCompost || (mentionsSeed && (lower.contains("keep") || lower.contains("chest") || lower.contains("hand") || lower.contains("only") || lower.contains("rest")));

        if (!isHayCommand && !isUncompactCommand && !isCompostCommand) {
            return false;
        }

        StringBuilder reply = new StringBuilder();

        // 1. Process wheat to hay compaction request
        if (isHayCommand) {
            if (isNegation) {
                this.autoCompactWheatToHay = false;
                reply.append("Understood! I will STOP combining wheat into hay bales and keep wheat as raw wheat for breeding and farming.");
                int uncrafted = uncompactHayInInventory();
                if (uncrafted > 0) {
                    reply.append(" Uncrafted ").append(uncrafted).append("x hay bales back into ").append(uncrafted * 9).append("x raw wheat!");
                }
            } else {
                this.autoCompactWheatToHay = true;
                int crafted = compactWheatInInventory();
                reply.append("Understood! I will combine wheat into hay bales to save inventory and chest space.");
                if (crafted > 0) {
                    reply.append(" Crafted ").append(crafted).append("x hay bales right now!");
                }
            }
        }

        // 2. Process uncompacting hay bales back into raw wheat
        if (isUncompactCommand && !isHayCommand) {
            int uncrafted = uncompactHayInInventory();
            if (reply.length() > 0) reply.append(" ");
            if (uncrafted > 0) {
                reply.append("Uncrafted ").append(uncrafted).append("x hay bales back into ").append(uncrafted * 9).append("x raw wheat!");
            } else {
                reply.append("No hay bales found in my inventory to uncraft into wheat.");
            }
        }

        // 2. Process seed limits & composting request
        if (isCompostCommand) {
            this.autoCompostExcessSeeds = true;

            // Extract custom limits if numbers are provided
            java.util.regex.Matcher handMatcher = java.util.regex.Pattern.compile("(?:keep|have|hold|leave)\\s+(\\d+)\\s+seeds?\\s+(?:on\\s+hand|in\\s+(?:hand|inventory))").matcher(lower);
            if (handMatcher.find()) {
                try {
                    this.maxSeedsToKeepOnHand = Integer.parseInt(handMatcher.group(1));
                } catch (NumberFormatException ignored) {}
            }

            java.util.regex.Matcher chestMatcher = java.util.regex.Pattern.compile("(?:keep|leave|store|have)\\s+(\\d+)\\s+seeds?\\s+in\\s+(?:the\\s+)?chest").matcher(lower);
            if (chestMatcher.find()) {
                try {
                    this.maxSeedsInChest = Integer.parseInt(chestMatcher.group(1));
                } catch (NumberFormatException ignored) {}
            }

            if (reply.length() > 0) reply.append(" ");
            reply.append("Configured seed limits: keeping ").append(this.maxSeedsToKeepOnHand)
                 .append(" seeds on hand and up to ").append(this.maxSeedsInChest)
                 .append(" in chests, composting the rest.");

            // Immediately compost any surplus seeds if a composter is nearby
            BlockPos composter = findNearestComposterBlock(24);
            if (composter != null) {
                int totalSeeds = countMatching(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString().contains("seed"));
                if (totalSeeds > this.maxSeedsToKeepOnHand) {
                    int composted = compostExcessSeeds(composter, this.maxSeedsToKeepOnHand);
                    if (composted > 0) {
                        reply.append(" Composted ").append(composted).append(" surplus seeds into bone meal right now!");
                    }
                }
            } else {
                reply.append(" (No composter within 24m yet. Place one nearby and I'll route all excess seeds into it!)");
            }
        }

        sayInChat(reply.toString());
        return true;
    }

    public synchronized boolean handleTrainingChatCommand(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();

        // Check training intent keywords
        boolean mentionsLearn = lower.contains("learn") || lower.contains("teach") || lower.contains("remember")
                || lower.contains("train") || lower.contains("memorize") || lower.startsWith("rule:") || lower.startsWith("recipe:");
        boolean mentionsRecipe = lower.contains("recipe") || (lower.contains("craft") && (lower.contains("wheat") || lower.contains("using") || lower.contains("with") || lower.contains("table") || lower.contains("spot")));

        if (!mentionsLearn && !mentionsRecipe) {
            return false;
        }

        boolean isGlobal = lower.contains("global") || lower.contains("for all") || lower.contains("everyone") || lower.contains("all bots");

        // Try parsing recipe first
        LearnedRecipe recipe = BotMemoryManager.parseRecipeFromText(text, isGlobal);
        if (recipe != null) {
            if (isGlobal) {
                BotMemoryManager.addGlobalRecipe(recipe);
            } else {
                this.addLearnedRecipe(recipe);
            }

            // If it's wheat into hay, enable autoCompactWheatToHay automatically
            if (recipe.resultItemId.contains("hay") && recipe.ingredients.containsKey("wheat")) {
                this.autoCompactWheatToHay = true;
                compactWheatInInventory();
            }

            sayInChat("Learned new recipe for '" + recipe.resultItemId + "' (" + recipe.toString() + ")! I will know how to craft this next time (" + (isGlobal ? "Global" : "Personal") + ").");
            return true;
        }

        // If not a recipe, process as a behavioral guideline / memory
        if (mentionsLearn || lower.startsWith("rule:") || lower.startsWith("remember:")) {
            String clean = text.replaceAll("(?i)^(?:bot[,:]?\\s+)?(?:please\\s+)?(?:learn\\s+(?:this[:]?\\s+)?|remember\\s+(?:this[:]?\\s+)?|teach\\s+(?:you[:]?\\s+)?|train\\s+(?:global[:]?\\s+)?|rule[:]?\\s+)", "").trim();
            if (!clean.isBlank()) {
                if (isGlobal) {
                    BotMemoryManager.addGlobalMemory(clean);
                } else {
                    this.addLearnedMemory(clean);
                }
                sayInChat("Recorded to my " + (isGlobal ? "global team" : "personal") + " memory: '" + clean + "'. I'll follow this guideline!");
                return true;
            }
        }

        return false;
    }

    public static boolean isTreeLog(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(net.minecraft.tags.BlockTags.LOGS)) return true;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(Locale.ROOT);
        return path.contains("_log") || path.contains("_stem") || path.contains("_wood");
    }

    public static boolean isTreeLeaves(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(net.minecraft.tags.BlockTags.LEAVES)) return true;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(Locale.ROOT);
        return path.contains("_leaves") || path.contains("wart_block");
    }

    public static boolean isTreeLogItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(net.minecraft.tags.ItemTags.LOGS)) return true;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        return path.contains("_log") || path.contains("_stem") || path.contains("_wood");
    }

    public static Item getMatchingSaplingForLog(BlockState logState) {
        String path = BuiltInRegistries.BLOCK.getKey(logState.getBlock()).getPath().toLowerCase(Locale.ROOT);
        if (path.contains("spruce")) return Items.SPRUCE_SAPLING;
        if (path.contains("birch")) return Items.BIRCH_SAPLING;
        if (path.contains("jungle")) return Items.JUNGLE_SAPLING;
        if (path.contains("acacia")) return Items.ACACIA_SAPLING;
        if (path.contains("dark_oak")) return Items.DARK_OAK_SAPLING;
        if (path.contains("cherry")) return Items.CHERRY_SAPLING;
        if (path.contains("mangrove")) return Items.MANGROVE_PROPAGULE;
        if (path.contains("crimson")) return Items.CRIMSON_FUNGUS;
        if (path.contains("warped")) return Items.WARPED_FUNGUS;
        return Items.OAK_SAPLING;
    }

    public List<BlockPos> findConnectedTreeLogs(BlockPos basePos) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(basePos);
        visited.add(basePos);

        while (!queue.isEmpty() && result.size() < 64) {
            BlockPos curr = queue.poll();
            BlockState s = this.level().getBlockState(curr);
            if (isTreeLog(s)) {
                result.add(curr);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            BlockPos next = curr.offset(dx, dy, dz);
                            if (visited.add(next)) {
                                if (isTreeLog(this.level().getBlockState(next))) {
                                    queue.add(next);
                                }
                            }
                        }
                    }
                }
            }
        }
        result.sort(Comparator.comparingInt(net.minecraft.core.Vec3i::getY));
        return result;
    }

    public @Nullable BlockPos findNearestTreeTrunk(BlockPos center, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int r = 1; r <= radius; r += 2) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r && r > 2) continue;
                    for (int dy = -6; dy <= 12; dy++) {
                        BlockPos p = center.offset(dx, dy, dz);
                        BlockState s = this.level().getBlockState(p);
                        if (isTreeLog(s)) {
                            BlockState below = this.level().getBlockState(p.below());
                            if (!isTreeLog(below)) {
                                double d = this.distanceToSqr(Vec3.atCenterOf(p));
                                if (d < bestDist) {
                                    bestDist = d;
                                    best = p;
                                }
                            }
                        }
                    }
                }
            }
            if (best != null) break;
        }
        return best;
    }

    public void startChoppingTree(BlockPos basePos, @Nullable ServerPlayer speaker) {
        this.clearActions();
        this.targetTreeBase = basePos;
        this.targetTreeLogs.clear();
        this.targetTreeLogs.addAll(findConnectedTreeLogs(basePos));
        if (this.targetTreeLogs.isEmpty()) {
            this.targetTreeLogs.add(basePos);
        }
        this.currentAction = new BotAction("CHOP_TREE", "tree (" + this.targetTreeLogs.size() + " logs)", null, this.targetTreeLogs.size());
        this.actionProgressCount = 0;
        this.actionTicks = 0;
        sayInChat("Cutting down tree at (" + basePos.getX() + ", " + basePos.getY() + ", " + basePos.getZ() + ")! Queued " + this.targetTreeLogs.size() + " logs to chop and replant.");
        chopNextTreeLog();
    }

    public void chopNextTreeLog() {
        if (this.targetTreeLogs.isEmpty()) {
            return;
        }

        while (!this.targetTreeLogs.isEmpty() && !isTreeLog(this.level().getBlockState(this.targetTreeLogs.get(0)))) {
            this.targetTreeLogs.remove(0);
        }

        if (this.targetTreeLogs.isEmpty()) {
            vacuumNearbyDrops();
            this.currentAction = null;
            if (!this.lumberjackAreaActive) {
                sayInChat("Finished felling the tree! Replanted sapling and collected all wood.");
            }
            return;
        }

        BlockPos targetLog = this.targetTreeLogs.get(0);
        Vec3 center = Vec3.atCenterOf(targetLog);
        double distSq = this.distanceToSqr(center);

        if (distSq > 16.0) {
            moveTowards(center, distSq > 36.0);
            return;
        }

        stopMovement();
        lookAtPos(center);
        BlockState state = this.level().getBlockState(targetLog);
        equipBestToolFor(state);

        this.level().destroyBlockProgress(this.getId(), targetLog, (this.miningTicks * 10) / 25);
        this.miningTicks++;

        if (this.miningTicks >= 12) {
            this.level().destroyBlockProgress(this.getId(), targetLog, -1);
            this.miningTicks = 0;
            this.level().destroyBlock(targetLog, true, this);

            if (this.targetTreeBase != null && targetLog.equals(this.targetTreeBase)) {
                BlockState ground = this.level().getBlockState(targetLog.below());
                if (ground.is(Blocks.DIRT) || ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.PODZOL) || ground.is(Blocks.COARSE_DIRT) || ground.is(Blocks.MUD) || ground.is(Blocks.ROOTED_DIRT)) {
                    Item saplingItem = getMatchingSaplingForLog(state);
                    if (saplingItem instanceof BlockItem bi) {
                        this.level().setBlockAndUpdate(targetLog, bi.getBlock().defaultBlockState());
                    }
                }
            }

            vacuumNearbyDrops();
            this.actionProgressCount++;
            this.targetTreeLogs.remove(0);
            if (this.targetTreeLogs.isEmpty()) {
                this.currentAction = null;
                if (!this.lumberjackAreaActive) {
                    sayInChat("Finished felling the tree! Replanted sapling and collected all wood.");
                }
            }
        }
    }

    public int depositLumberDropsIntoChest(BlockPos chestPos) {
        Container container = getContainerAt(chestPos);
        if (container == null) return 0;
        int deposited = 0;

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
            if (path.contains("_log") || path.contains("_stem") || path.contains("_wood") || path.equals("stick")) {
                int count = stack.getCount();
                ItemStack leftover = insertIntoContainer(container, stack);
                int transferred = count - leftover.getCount();
                if (transferred > 0) {
                    this.getInventory().setItem(i, leftover);
                    deposited += transferred;
                }
            }
        }
        return deposited;
    }

    private void tickPassiveLumberjack() {
        if (this.tickCount % 20 != 0) return;

        BlockPos origin = this.lumberOriginPos != null ? this.lumberOriginPos : this.blockPosition();
        int radius = this.lumberRadius > 0 ? this.lumberRadius : 32;

        int logCount = countMatching(s -> isTreeLogItem(s));
        if ((logCount >= 64 || this.getInventory().getFreeSlot() == -1) && this.lumberStoreLogs) {
            BlockPos chestPos = findNearestAvailableContainerBlock(32);
            if (chestPos != null) {
                double distSq = this.distanceToSqr(Vec3.atCenterOf(chestPos));
                if (distSq > 9.0) {
                    moveTowards(Vec3.atCenterOf(chestPos), true);
                    return;
                }
                stopMovement();
                lookAtPos(Vec3.atCenterOf(chestPos));
                int stored = depositLumberDropsIntoChest(chestPos);
                if (stored > 0) {
                    sayInChat("Stored " + stored + "x logs & wood supplies into chest!");
                }
                return;
            }
        }

        ItemEntity drop = findNearestDrop(8.0);
        if (drop != null) {
            moveTowards(drop.position(), false);
            return;
        }

        if (!this.targetTreeLogs.isEmpty()) {
            chopNextTreeLog();
            return;
        }

        BlockPos treeBase = findNearestTreeTrunk(origin, radius);
        if (treeBase != null) {
            List<BlockPos> logs = findConnectedTreeLogs(treeBase);
            if (!logs.isEmpty()) {
                this.targetTreeBase = treeBase;
                this.targetTreeLogs.clear();
                this.targetTreeLogs.addAll(logs);
                chopNextTreeLog();
            }
        }
    }

    public synchronized boolean handleLumberjackChatCommand(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();

        boolean mentionsTree = lower.contains("tree") || lower.contains("trees") || lower.contains("wood")
                || lower.contains("log") || lower.contains("logs") || lower.contains("timber") || lower.contains("lumber");

        if (!mentionsTree && !lower.contains("lumberjack") && !lower.contains("chop")) {
            return false;
        }

        // 1. Single-tree command: "cut this tree down", "chop this tree", "cut tree down", "fell this tree"
        boolean isSingleTree = lower.contains("this tree") || lower.contains("the tree") || lower.contains("cut this")
                || lower.contains("chop this") || lower.contains("cut tree") || lower.contains("chop tree")
                || lower.equals("cut") || lower.equals("chop");

        if (isSingleTree && !lower.contains("all") && !lower.contains("area") && !lower.contains("100")) {
            BlockPos targetBase = null;
            if (speaker != null) {
                net.minecraft.world.phys.HitResult hit = speaker.pick(12.0, 0.0f, false);
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                    BlockPos p = bhr.getBlockPos();
                    BlockState s = this.level().getBlockState(p);
                    if (isTreeLog(s) || isTreeLeaves(s)) {
                        BlockPos curr = p;
                        while (curr.getY() > this.level().getMinY()) {
                            BlockState bState = this.level().getBlockState(curr.below());
                            if (isTreeLog(bState)) {
                                curr = curr.below();
                            } else {
                                break;
                            }
                        }
                        if (isTreeLog(this.level().getBlockState(curr))) {
                            targetBase = curr;
                        }
                    }
                }
            }

            if (targetBase == null) {
                BlockPos ref = speaker != null ? speaker.blockPosition() : this.blockPosition();
                targetBase = findNearestTreeTrunk(ref, 32);
            }

            if (targetBase != null) {
                this.botRole = "lumberjack";
                this.lumberjackAreaActive = false;
                startChoppingTree(targetBase, speaker);
                return true;
            } else {
                sayInChat("I looked around but couldn't find any trees within 32 blocks.");
                return true;
            }
        }

        // 2. Area Lumberjack command
        boolean isAreaLumber = lower.contains("all trees") || lower.contains("all the trees") || lower.contains("area")
                || lower.contains("each direction") || lower.contains("note this spot") || lower.contains("bring back logs")
                || lower.contains("save the logs") || lower.contains("clear trees") || lower.contains("chop trees")
                || lower.contains("lumberjack");

        if (isAreaLumber) {
            int radius = 32;

            java.util.regex.Matcher eachDir = java.util.regex.Pattern.compile("(\\d+)\\s*(?:blocks?|tiles?|m)?\\s*(?:in\\s+each\\s+direction)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
            if (eachDir.find()) {
                try { radius = Math.min(128, Math.max(8, Integer.parseInt(eachDir.group(1)))); } catch (Exception ignored) {}
            } else {
                java.util.regex.Matcher areaMatch = java.util.regex.Pattern.compile("(\\d+)\\s*(?:x|by)\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
                if (areaMatch.find()) {
                    try {
                        int w = Integer.parseInt(areaMatch.group(1));
                        int l = Integer.parseInt(areaMatch.group(2));
                        radius = Math.min(128, Math.max(8, Math.max(w, l) / 2));
                    } catch (Exception ignored) {}
                } else {
                    java.util.regex.Matcher radMatch = java.util.regex.Pattern.compile("(\\d+)\\s*(?:blocks?|tiles?|m|radius|area)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);
                    if (radMatch.find()) {
                        try { radius = Math.min(128, Math.max(8, Integer.parseInt(radMatch.group(1)))); } catch (Exception ignored) {}
                    }
                }
            }

            BlockPos origin = this.lumberOriginPos;
            if (lower.contains("this spot") || lower.contains("from here") || lower.contains("note this spot") || origin == null) {
                origin = speaker != null ? speaker.blockPosition() : this.blockPosition();
            }

            boolean returnLogs = lower.contains("bring back") || lower.contains("save the logs") || lower.contains("store") || lower.contains("chest");

            this.botRole = "lumberjack";
            this.lumberOriginPos = origin;
            this.lumberRadius = radius;
            this.lumberjackAreaActive = true;
            this.lumberStoreLogs = returnLogs;

            sayInChat("Lumberjack duty set! Working " + (radius * 2) + "x" + (radius * 2) + " area from ("
                    + origin.getX() + ", " + origin.getY() + ", " + origin.getZ()
                    + ")! I will systematically chop down all trees, replant saplings, vacuum up drops"
                    + (returnLogs ? ", and store logs in chests." : "."));

            BlockPos tree = findNearestTreeTrunk(origin, radius);
            if (tree != null) {
                startChoppingTree(tree, speaker);
            }
            return true;
        }

        return false;
    }

    public synchronized boolean handleExplorationChatCommand(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();

        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(?:find|locate|search\\s+for|where\\s+is|take\\s+us\\s+to|navigate\\s+to)\\s+(?:us\\s+)?(?:the\\s+)?(?:closest|nearest)?\\s*([a-zA-Z0-9_]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lower);

        if (!m.find()) {
            return false;
        }

        String rawQuery = m.group(1).trim().toLowerCase(Locale.ROOT);
        String biomeQuery = null;
        String prettyName = null;

        if (rawQuery.contains("ocean") || rawQuery.contains("sea") || rawQuery.contains("water")) {
            biomeQuery = "ocean";
            prettyName = "Ocean";
        } else if (rawQuery.contains("desert")) {
            biomeQuery = "desert";
            prettyName = "Desert";
        } else if (rawQuery.contains("plains")) {
            biomeQuery = "plains";
            prettyName = "Plains";
        } else if (rawQuery.contains("forest")) {
            biomeQuery = "forest";
            prettyName = "Forest";
        } else if (rawQuery.contains("jungle")) {
            biomeQuery = "jungle";
            prettyName = "Jungle";
        } else if (rawQuery.contains("swamp")) {
            biomeQuery = "swamp";
            prettyName = "Swamp";
        } else if (rawQuery.contains("mountain") || rawQuery.contains("peak")) {
            biomeQuery = "peaks";
            prettyName = "Mountains";
        } else if (rawQuery.contains("badlands") || rawQuery.contains("mesa")) {
            biomeQuery = "badlands";
            prettyName = "Badlands";
        } else if (rawQuery.contains("cherry")) {
            biomeQuery = "cherry";
            prettyName = "Cherry Grove";
        } else if (rawQuery.contains("taiga") || rawQuery.contains("snow")) {
            biomeQuery = "taiga";
            prettyName = "Taiga";
        }

        if (biomeQuery == null) {
            return false;
        }

        final String targetKey = biomeQuery;
        BlockPos ref = speaker != null ? speaker.blockPosition() : this.blockPosition();

        if (this.level() instanceof ServerLevel sl) {
            com.mojang.datafixers.util.Pair<BlockPos, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> found =
                    sl.findClosestBiome3d(holder -> {
                        String k = holder.unwrapKey().map(key -> key.identifier().getPath().toLowerCase(Locale.ROOT)).orElse("");
                        return k.contains(targetKey);
                    }, ref, 6400, 32, 64);

            if (found != null) {
                BlockPos targetPos = found.getFirst();
                double dist = Math.sqrt(ref.distSqr(targetPos));
                String cardinal = getCardinalDirection(targetPos.getX() - ref.getX(), targetPos.getZ() - ref.getZ());

                BotPersistence.saveLocation(prettyName.toLowerCase(Locale.ROOT), targetPos, this.level().dimension().identifier().toString());
                sayInChat("Found an " + prettyName + " ~" + (int) dist + " blocks away to the " + cardinal + " at ("
                        + targetPos.getX() + ", ~, " + targetPos.getZ() + ")! Starting journey there now.");

                this.clearActions();
                this.currentAction = new BotAction("GOTO", prettyName, null, 1, targetPos.getX(), targetPos.getY(), targetPos.getZ());
                this.actionProgressCount = 0;
                this.actionTicks = 0;
                return true;
            } else {
                sayInChat("I searched up to 6,400 blocks away but couldn't detect any " + prettyName + ".");
                return true;
            }
        }

        return false;
    }

    private static String getCardinalDirection(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        if (angle < 0) angle += 360.0;
        if (angle >= 337.5 || angle < 22.5) return "South";
        if (angle >= 22.5 && angle < 67.5) return "South-West";
        if (angle >= 67.5 && angle < 112.5) return "West";
        if (angle >= 112.5 && angle < 157.5) return "North-West";
        if (angle >= 157.5 && angle < 202.5) return "North";
        if (angle >= 202.5 && angle < 247.5) return "North-East";
        if (angle >= 247.5 && angle < 292.5) return "East";
        return "South-East";
    }

    public void startHouseConstruction(BlockPos origin, net.minecraft.core.Direction facing) {
        startCustomConstruction("cottage", origin, facing);
    }

    public void startCustomBlueprintConstruction(CustomBlueprint blueprint, BlockPos origin, net.minecraft.core.Direction facing) {
        retrieveWorkshopSupplies(origin);
        this.activeHouseQueue = blueprint.toVoxelSteps(origin, facing);
        this.currentHouseOrigin = origin;
        this.currentAction = new BotAction("BUILD_BLUEPRINT", blueprint.name, null, activeHouseQueue.size());
        this.actionProgressCount = 0;
        sayInChat("Retrieved workshop supplies: Constructing blueprint '" + blueprint.name + "' (" + blueprint.sizeX + "x" + blueprint.sizeY + "x" + blueprint.sizeZ + ")! (" + activeHouseQueue.size() + " blocks queued)");
    }

    public void startCustomConstruction(String archetype, BlockPos origin, net.minecraft.core.Direction facing) {
        retrieveWorkshopSupplies(origin);
        ArchitecturalPalette palette = ArchitecturalPalette.resolve((ServerLevel) this.level(), origin, this);
        this.activeHouseQueue = HouseBlueprint.generateBlueprint(archetype, origin, facing, palette);
        this.currentHouseOrigin = origin;
        this.currentAction = new BotAction("BUILD_HOUSE", archetype, null, activeHouseQueue.size());
        this.actionProgressCount = 0;
        sayInChat("Surveyed block registry & workshop chests: Constructing a " + archetype.toUpperCase() + " using " + palette.themeName + "! (" + activeHouseQueue.size() + " blocks queued)");
    }

    private void retrieveWorkshopSupplies(BlockPos origin) {
        int radius = 16;
        for (int x = -radius; x <= radius; x += 4) {
            for (int y = -3; y <= 4; y += 2) {
                for (int z = -radius; z <= radius; z += 4) {
                    BlockPos p = origin.offset(x, y, z);
                    BlockEntity be = this.level().getBlockEntity(p);
                    if (be instanceof Container c) {
                        for (int i = 0; i < c.getContainerSize(); i++) {
                            ItemStack s = c.getItem(i);
                            if (!s.isEmpty()) {
                                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
                                if (id.contains("log") || id.contains("plank") || id.contains("stone") || id.contains("cobble") ||
                                    id.contains("glass") || id.contains("door") || id.contains("roof") || id.contains("window") ||
                                    id.contains("torch") || id.contains("lamp") || id.contains("hay") || id.contains("bed")) {
                                    ItemStack taken = c.removeItem(i, Math.min(s.getCount(), 32));
                                    if (!taken.isEmpty()) {
                                        this.getInventory().add(taken);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Auto-process raw logs into planks if planks are low
        int logCount = countMatching(this::isLog);
        int plankCount = countMatching(this::isPlank);
        if (logCount > 0 && plankCount < 64) {
            int logsToCraft = Math.min(logCount, 16);
            if (consumeMatching(this::isLog, logsToCraft)) {
                this.getInventory().add(new ItemStack(Items.OAK_PLANKS, logsToCraft * 4));
            }
        }
    }

    private boolean tickHouseBuildingAction() {
        if (this.activeHouseQueue == null || this.activeHouseQueue.isEmpty()) {
            this.activeHouseQueue = null;
            // Register completed house in VillageManager
            String compound = this.assignedAreaName != null ? this.assignedAreaName : "Village";
            BlockPos doorPos = this.currentHouseOrigin != null ? this.currentHouseOrigin.relative(this.getDirection(), 1).above(1) : this.blockPosition();
            BlockPos bedPos = this.currentHouseOrigin != null ? this.currentHouseOrigin.relative(this.getDirection().getClockWise(), -2)
                    .relative(this.getDirection().getOpposite(), 6).above(1) : this.blockPosition();
            VillageManager.registerHouse(compound, doorPos, bedPos, null);

            if (this.currentAction != null && "BUILD_BLUEPRINT".equalsIgnoreCase(this.currentAction.type)) {
                sayInChat("Blueprint '" + (this.currentAction.target != null ? this.currentAction.target : "custom") + "' construction complete! Structure registered in village catalog.");
            } else {
                sayInChat("House construction complete! Integrated custom roof, doors, windows, lighting, and bed. Registered in village housing registry!");
            }
            return true;
        }

        if (this.houseBuildCooldown > 0) {
            this.houseBuildCooldown--;
            return false;
        }

        // Take next voxel step
        HouseBlueprint.VoxelStep step = this.activeHouseQueue.remove(0);
        this.actionProgressCount++;

        // Walk close horizontally to the target block if too far (> 5 blocks away horizontally)
        double hdx = (step.pos().getX() + 0.5) - this.getX();
        double hdz = (step.pos().getZ() + 0.5) - this.getZ();
        if (hdx * hdx + hdz * hdz > 25.0) {
            moveTowards(new Vec3(step.pos().getX() + 0.5, this.getY(), step.pos().getZ() + 0.5), false);
            this.activeHouseQueue.add(0, step);
            this.actionProgressCount--;
            return false;
        }

        lookAtPos(Vec3.atCenterOf(step.pos()));

        if (step.isClearAir()) {
            if (!this.level().getBlockState(step.pos()).isAir()) {
                this.level().destroyBlock(step.pos(), false, this);
                broadcastSwing();
                this.houseBuildCooldown = 2;
            } else {
                this.houseBuildCooldown = 0;
            }
        } else {
            this.level().setBlockAndUpdate(step.pos(), step.state());
            broadcastSwing();
            this.level().playSound(null, step.pos(),
                    step.state().getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            this.houseBuildCooldown = 2;
        }

        return false;
    }

    private static final List<String> NEWBORN_BOT_NAMES = List.of(
            "Pip", "Barnaby", "Oliver", "Milo", "Luna", "Jasper", "Theo", "Rosie",
            "Finley", "Toby", "Daisy", "Felix", "Archie", "Willow", "Hazel", "Leo",
            "Gideon", "Clover", "Rowan", "Arlo", "Elliot", "Sage", "Penny", "Gulliver",
            "Finn", "Nora", "Silas", "Freya", "Bramble", "Arthur", "Maisie", "Orson"
    );

    public static String pickRandomAvailableBotName() {
        List<String> shuffled = new ArrayList<>(NEWBORN_BOT_NAMES);
        Collections.shuffle(shuffled);
        for (String name : shuffled) {
            if (BotManager.getBot(name) == null) {
                return name;
            }
        }
        return "Villager_" + (BotManager.getActiveBots().size() + 1);
    }

    public static final Map<Block, String> WORKSTATION_PROFESSIONS = Map.ofEntries(
            Map.entry(Blocks.COMPOSTER, "farmer"),
            Map.entry(Blocks.LECTERN, "librarian"),
            Map.entry(Blocks.BLAST_FURNACE, "armorer"),
            Map.entry(Blocks.SMOKER, "butcher"),
            Map.entry(Blocks.CARTOGRAPHY_TABLE, "cartographer"),
            Map.entry(Blocks.BREWING_STAND, "cleric"),
            Map.entry(Blocks.FLETCHING_TABLE, "fletcher"),
            Map.entry(Blocks.CAULDRON, "leatherworker"),
            Map.entry(Blocks.LOOM, "shepherd"),
            Map.entry(Blocks.SMITHING_TABLE, "weaponsmith"),
            Map.entry(Blocks.STONECUTTER, "mason"),
            Map.entry(Blocks.GRINDSTONE, "toolsmith"),
            Map.entry(Blocks.BARREL, "fisherman")
    );

    public static final Map<String, net.minecraft.sounds.SoundEvent> PROFESSION_WORK_SOUNDS = Map.ofEntries(
            Map.entry("farmer", SoundEvents.VILLAGER_WORK_FARMER),
            Map.entry("librarian", SoundEvents.VILLAGER_WORK_LIBRARIAN),
            Map.entry("armorer", SoundEvents.VILLAGER_WORK_ARMORER),
            Map.entry("butcher", SoundEvents.VILLAGER_WORK_BUTCHER),
            Map.entry("cartographer", SoundEvents.VILLAGER_WORK_CARTOGRAPHER),
            Map.entry("cleric", SoundEvents.VILLAGER_WORK_CLERIC),
            Map.entry("fletcher", SoundEvents.VILLAGER_WORK_FLETCHER),
            Map.entry("leatherworker", SoundEvents.VILLAGER_WORK_LEATHERWORKER),
            Map.entry("shepherd", SoundEvents.VILLAGER_WORK_SHEPHERD),
            Map.entry("weaponsmith", SoundEvents.VILLAGER_WORK_WEAPONSMITH),
            Map.entry("mason", SoundEvents.VILLAGER_WORK_MASON),
            Map.entry("toolsmith", SoundEvents.VILLAGER_WORK_TOOLSMITH),
            Map.entry("fisherman", SoundEvents.VILLAGER_WORK_FISHERMAN)
    );

    public synchronized boolean tickNightShelterAndSleep() {
        if (this.isSleeping()) {
            this.activePassiveActivity = "Sleeping in bed";
            this.stopMovement();
            return true;
        }

        BlockPos targetBed = this.assignedBedPos;
        if (targetBed != null && isTargetBlacklisted(targetBed)) {
            targetBed = null;
        }
        if (targetBed == null) {
            targetBed = findNearestBedBlock(32);
            if (targetBed != null) {
                this.assignedBedPos = targetBed;
            }
        }

        if (targetBed != null) {
            double distSq = this.distanceToSqr(Vec3.atCenterOf(targetBed));
            boolean canReachBedDirectly = distSq <= 4.0 && hasUnobstructedReachToTarget(targetBed);
            if (!canReachBedDirectly) {
                this.activePassiveActivity = "Walking indoors to bed at (" + targetBed.getX() + ", " + targetBed.getY() + ", " + targetBed.getZ() + ")";
                moveTowards(Vec3.atCenterOf(targetBed), false);
                return true;
            } else {
                stopMovement();
                this.activePassiveActivity = "At bed (" + targetBed.getX() + ", " + targetBed.getY() + ", " + targetBed.getZ() + ")";
                lookAtPos(Vec3.atCenterOf(targetBed));
                if (this.tickCount % 20 == 0) {
                    try {
                        boolean ok = this.startSleeping(targetBed);
                        if (ok) {
                            return true;
                        }
                    } catch (Exception ignored) {}
                }
                return true;
            }
        }

        BlockPos targetHouse = this.assignedHousePos;
        if (targetHouse != null && !isTargetBlacklisted(targetHouse)) {
            double distSq = this.distanceToSqr(Vec3.atCenterOf(targetHouse));
            if (distSq > 4.0 || !hasUnobstructedReachToTarget(targetHouse)) {
                this.activePassiveActivity = "Walking inside house at (" + targetHouse.getX() + ", " + targetHouse.getY() + ", " + targetHouse.getZ() + ")";
                moveTowards(Vec3.atCenterOf(targetHouse), false);
            } else {
                this.activePassiveActivity = "Sheltering inside house";
                stopMovement();
            }
            return true;
        }

        BlockPos shelter = findNearestShelterBlock(24);
        if (shelter != null) {
            double distSq = this.distanceToSqr(Vec3.atCenterOf(shelter));
            if (distSq > 3.0) {
                this.activePassiveActivity = "Walking to night shelter at (" + shelter.getX() + ", " + shelter.getY() + ", " + shelter.getZ() + ")";
                moveTowards(Vec3.atCenterOf(shelter), false);
            } else {
                this.activePassiveActivity = "Sheltering indoors for the night";
                stopMovement();
            }
            return true;
        }

        return false;
    }

    public @Nullable BlockPos findNearestBedBlock(int radius) {
        return findNearestBedBlockNear(this.blockPosition(), radius);
    }

    public @Nullable BlockPos findNearestBedBlockNear(BlockPos pos, int radius) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -3, -radius), pos.offset(radius, 3, radius))) {
            if (isTargetBlacklisted(p)) continue;
            BlockState st = this.level().getBlockState(p);
            if (st.getBlock() instanceof BedBlock && st.getValue(BedBlock.PART) == BedPart.HEAD) {
                double d = Vec3.atCenterOf(pos).distanceToSqr(Vec3.atCenterOf(p));
                if (d < bestDistSq) {
                    bestDistSq = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    public @Nullable BlockPos findNearestShelterBlock(int radius) {
        BlockPos center = this.blockPosition();
        for (int r = 1; r <= radius; r += 2) {
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 2, r))) {
                if (this.level().getBlockState(p).isAir() && this.level().getBlockState(p.above()).isAir()) {
                    if (this.level().canSeeSky(p)) continue;
                    BlockPos stand = p.below();
                    if (this.level().getBlockState(stand).isSolid()) {
                        return p.immutable();
                    }
                }
            }
        }
        return null;
    }

    public @Nullable BlockPos findNearestBlockType(Block blockType, int radius) {
        BlockPos center = this.blockPosition();
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            if (this.level().getBlockState(p).is(blockType)) {
                double d = this.distanceToSqr(Vec3.atCenterOf(p));
                if (d < bestDistSq) {
                    bestDistSq = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    public @Nullable BlockPos findNearestWorkstation(int radius) {
        BlockPos center = this.blockPosition();
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            Block b = this.level().getBlockState(p).getBlock();
            if (WORKSTATION_PROFESSIONS.containsKey(b)) {
                double d = this.distanceToSqr(Vec3.atCenterOf(p));
                if (d < bestDistSq) {
                    bestDistSq = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    public @Nullable Monster findNearestHostileThreatForVillager(double maxDist) {
        AABB box = this.getBoundingBox().inflate(maxDist);
        List<Monster> monsters = this.level().getEntitiesOfClass(Monster.class, box, m -> m.isAlive() && !m.isAlliedTo(this));
        Monster closest = null;
        double bestDistSq = maxDist * maxDist;
        for (Monster m : monsters) {
            double d = this.distanceToSqr(m);
            if (d < bestDistSq) {
                bestDistSq = d;
                closest = m;
            }
        }
        return closest;
    }

    public boolean isNightTime() {
        long t = this.level().getOverworldClockTime() % 24000L;
        return (t >= 12542L && t <= 23459L);
    }

    public void wanderAroundHomeOrAnchor(double maxDist) {
        BlockPos center = this.assignedHousePos != null ? this.assignedHousePos
                : (this.guardAnchorPos != null ? this.guardAnchorPos : this.blockPosition());
        double distSq = this.distanceToSqr(Vec3.atCenterOf(center));
        if (distSq > (maxDist * maxDist)) {
            moveTowards(Vec3.atCenterOf(center), false);
            return;
        }

        if (this.tickCount % 60 == 0) {
            double angle = (this.tickCount * 23) % 360;
            double r = 4.0 + (this.tickCount % 5);
            double targetX = center.getX() + Math.cos(Math.toRadians(angle)) * r;
            double targetZ = center.getZ() + Math.sin(Math.toRadians(angle)) * r;
            BlockPos targetPos = new BlockPos((int) targetX, center.getY(), (int) targetZ);
            BlockPos safeStand = (this.level() instanceof ServerLevel sl) ? findSafeDryRespawnPos(sl, targetPos, 3) : null;
            if (safeStand != null) {
                moveTowards(Vec3.atCenterOf(safeStand), false);
            } else {
                moveTowards(Vec3.atCenterOf(targetPos), false);
            }
        }
    }

    private void handleVillagerPanic(Monster threat) {
        this.stopUsingItem();
        BlockPos bell = findNearestBlockType(Blocks.BELL, 16);
        if (bell != null) {
            double distToBellSq = this.distanceToSqr(Vec3.atCenterOf(bell));
            if (distToBellSq > 4.0) {
                moveTowards(Vec3.atCenterOf(bell), true);
            } else {
                stopMovement();
                this.level().playSound(null, bell, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0f, 1.0f);
            }
        } else {
            Vec3 away = this.position().subtract(threat.position()).normalize();
            Vec3 escapeTarget = (this.assignedHousePos != null)
                    ? Vec3.atCenterOf(this.assignedHousePos)
                    : this.position().add(away.scale(12.0));
            moveTowards(escapeTarget, true);
        }

        if (this.tickCount % 40 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase(Locale.ROOT);
    }

    public static @Nullable Block getWorkstationForProfession(String trait) {
        if (trait == null) return null;
        for (var entry : WORKSTATION_PROFESSIONS.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(trait)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean isWorkstationClaimedByOther(BlockPos pos) {
        for (AiBotPlayer other : BotManager.getActiveBots()) {
            if (other != this && other.level() == this.level() && pos.equals(other.getAssignedWorkstationPos())) {
                return true;
            }
        }
        return false;
    }

    public synchronized @Nullable BlockPos findAvailableWorkstation(int radius) {
        BlockPos center = this.assignedHousePos != null ? this.assignedHousePos
                : (this.assignedBedPos != null ? this.assignedBedPos : this.blockPosition());
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -6, -radius), center.offset(radius, 6, radius))) {
            Block b = this.level().getBlockState(p).getBlock();
            if (WORKSTATION_PROFESSIONS.containsKey(b)) {
                if (isWorkstationClaimedByOther(p)) {
                    continue;
                }
                boolean accessible = false;
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    BlockPos adj = p.relative(dir);
                    if (this.level().getBlockState(adj).isAir() || !this.level().getBlockState(adj).isSolid()) {
                        accessible = true;
                        break;
                    }
                }
                if (!accessible) continue;

                double d = this.distanceToSqr(Vec3.atCenterOf(p));
                if (d < bestDistSq) {
                    bestDistSq = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    public synchronized boolean searchAndClaimWorkstation(int radius) {
        if (this.isBaby()) return false;
        BlockPos available = findAvailableWorkstation(radius);
        if (available != null) {
            this.assignedWorkstationPos = available;
            Block b = this.level().getBlockState(available).getBlock();
            String trait = WORKSTATION_PROFESSIONS.getOrDefault(b, "villager");
            this.botSubRole = trait;

            net.minecraft.sounds.SoundEvent snd = PROFESSION_WORK_SOUNDS.getOrDefault(trait, SoundEvents.VILLAGER_YES);
            this.level().playSound(null, available, snd, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, available.getX() + 0.5, available.getY() + 1.2, available.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            }
            sayInChat("I discovered a " + b.getName().getString() + " at (" + available.getX() + ", " + available.getY() + ", " + available.getZ() + ")! I am now taking on the '" + capitalize(trait) + "' profession trait under my Villager role!");
            return true;
        }
        return false;
    }

    private void equipProfessionWorkItem(String trait) {
        Item itemToHold = switch (trait.toLowerCase(Locale.ROOT)) {
            case "farmer" -> Items.WHEAT;
            case "librarian" -> Items.BOOK;
            case "armorer" -> Items.IRON_INGOT;
            case "weaponsmith" -> Items.IRON_SWORD;
            case "toolsmith" -> Items.IRON_PICKAXE;
            case "fletcher" -> Items.BOW;
            case "cleric" -> Items.POTION;
            case "butcher" -> Items.COOKED_BEEF;
            case "fisherman" -> Items.FISHING_ROD;
            case "shepherd" -> Items.SHEARS;
            case "mason" -> Items.STONE_BRICKS;
            case "cartographer" -> Items.MAP;
            case "leatherworker" -> Items.LEATHER;
            default -> null;
        };
        if (itemToHold != null && !this.getMainHandItem().is(itemToHold)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(itemToHold, 1));
        }
    }

    private void tickVillagerWork() {
        if (this.isBaby()) return;

        // Verify existing workstation
        if (this.assignedWorkstationPos != null) {
            BlockState bs = this.level().getBlockState(this.assignedWorkstationPos);
            String trait = WORKSTATION_PROFESSIONS.get(bs.getBlock());
            if (trait == null) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0f, 1.0f);
                sayInChat("My work table at (" + this.assignedWorkstationPos.getX() + ", " + this.assignedWorkstationPos.getY() + ", " + this.assignedWorkstationPos.getZ() + ") was removed! I am now unemployed until I find a new work table.");
                this.assignedWorkstationPos = null;
                this.botSubRole = "unemployed";
            } else if (!trait.equalsIgnoreCase(this.botSubRole)) {
                this.botSubRole = trait;
                sayInChat("My work table was upgraded to a " + bs.getBlock().getName().getString() + "! My sub-role trait is now " + capitalize(trait) + ".");
            }
        }

        // Search for workstation if unemployed
        if (this.assignedWorkstationPos == null) {
            if (this.tickCount % 80 == 0) {
                searchAndClaimWorkstation(48);
            }
            wanderAroundHomeOrAnchor(16.0);
            return;
        }

        BlockPos workstation = this.assignedWorkstationPos;
        double distSq = this.distanceToSqr(Vec3.atCenterOf(workstation));
        if (distSq > 4.0) {
            moveTowards(Vec3.atCenterOf(workstation), false);
        } else {
            stopMovement();
            lookAtPos(Vec3.atCenterOf(workstation));
            equipProfessionWorkItem(this.botSubRole);

            if (this.tickCount % 60 == 0) {
                BlockState bs = this.level().getBlockState(workstation);
                Block b = bs.getBlock();
                String trait = this.botSubRole;
                net.minecraft.sounds.SoundEvent snd = PROFESSION_WORK_SOUNDS.getOrDefault(trait, SoundEvents.VILLAGER_WORK_ARMORER);

                if (b == Blocks.COMPOSTER && this.isAutoCompostExcessSeeds()) {
                    compostExcessSeeds(workstation, this.maxSeedsToKeepOnHand);
                }

                this.level().playSound(null, workstation, snd, SoundSource.BLOCKS, 0.8f, 1.0f);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, workstation.getX() + 0.5, workstation.getY() + 1.1, workstation.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
    }

    private void tickVillagerGatheringAndBreeding() {
        BlockPos bell = findNearestBlockType(Blocks.BELL, 36);
        AiBotPlayer nearestOtherBot = null;
        double bestBotDistSq = 36.0 * 36.0;
        for (AiBotPlayer other : BotManager.getActiveBots()) {
            if (other != this && other.level() == this.level()) {
                double d = this.distanceToSqr(other);
                if (d < bestBotDistSq) {
                    bestBotDistSq = d;
                    nearestOtherBot = other;
                }
            }
        }

        if (bell != null && (nearestOtherBot == null || this.distanceToSqr(Vec3.atCenterOf(bell)) < 16.0)) {
            double distSq = this.distanceToSqr(Vec3.atCenterOf(bell));
            if (distSq > 9.0) {
                moveTowards(Vec3.atCenterOf(bell), false);
            } else {
                stopMovement();
            }
        } else if (nearestOtherBot != null) {
            double distSq = this.distanceToSqr(nearestOtherBot);
            if (distSq > 6.0) {
                moveTowards(nearestOtherBot.position(), false);
            } else {
                stopMovement();
                lookAtPos(nearestOtherBot.getEyePosition());
            }
        } else {
            wanderAroundHomeOrAnchor(16.0);
        }

        if (this.tickCount - this.lastVillagerAmbientSoundTime > 100) {
            this.lastVillagerAmbientSoundTime = this.tickCount;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0f, this.isBaby ? 1.35f : 1.0f);
        }

        if (!this.isBaby && this.tickCount % 40 == 0) {
            checkAndExecuteBreeding(nearestOtherBot);
        }
    }

    private void tickBabyVillagerPlay() {
        this.activePassiveActivity = "Exploring village (Baby Villager)";
        this.setJumping(false);
        // Play tag with nearby friends or wander calmly without erratic jumping
        AiBotPlayer playmate = null;
        double bestDist = 25.0 * 25.0;
        for (AiBotPlayer other : BotManager.getActiveBots()) {
            if (other != this && other.level() == this.level()) {
                double d = this.distanceToSqr(other);
                if (d < bestDist) {
                    bestDist = d;
                    playmate = other;
                }
            }
        }

        if (playmate != null && bestDist > 6.25) {
            moveTowards(playmate.position(), false);
        } else {
            wanderAroundHomeOrAnchor(14.0);
        }

        if (this.tickCount % 80 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0f, 1.35f);
        }
    }

    public @Nullable BlockPos findNearestBed(int radius) {
        if (this.assignedBedPos != null && this.distanceToSqr(Vec3.atCenterOf(this.assignedBedPos)) <= radius * radius) {
            return this.assignedBedPos;
        }
        BlockPos center = this.blockPosition();
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))) {
            if (this.level().getBlockState(p).getBlock() instanceof BedBlock) {
                double d = center.distSqr(p);
                if (d < bestDistSq) {
                    bestDistSq = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    private void tickVillagerReturnHome() {
        BlockPos homeTarget = this.assignedBedPos != null ? this.assignedBedPos : this.assignedHousePos;
        if (homeTarget != null) {
            double distSq = this.distanceToSqr(Vec3.atCenterOf(homeTarget));
            if (distSq > 4.0) {
                moveTowards(Vec3.atCenterOf(homeTarget), false);
            } else {
                stopMovement();
            }
        } else {
            wanderAroundHomeOrAnchor(10.0);
        }
    }

    public synchronized void tickPassiveVillager() {
        Monster threat = findNearestHostileThreatForVillager(12.0);
        if (threat != null) {
            handleVillagerPanic(threat);
            return;
        }

        long dayTime = this.level().getOverworldClockTime() % 24000L;

        if (isNightTime() || this.level().isThundering()) {
            tickNightShelterAndSleep();
            return;
        }

        if (dayTime < 2000L) {
            if (this.tickCount % 80 == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
            wanderAroundHomeOrAnchor(12.0);
            return;
        }

        if (dayTime >= 2000L && dayTime < 9000L) {
            if (this.isBaby) {
                tickBabyVillagerPlay();
            } else {
                tickVillagerWork();
            }
            return;
        }

        if (dayTime >= 9000L && dayTime < 11500L) {
            tickVillagerGatheringAndBreeding();
            return;
        }

        tickVillagerReturnHome();
    }

    public int countBedsIn100x100(BlockPos center) {
        if (!(this.level() instanceof ServerLevel sl)) return 0;
        PoiManager poi = sl.getPoiManager();
        long count = poi.getCountInRange(holder -> holder.is(PoiTypes.HOME), center, 50, PoiManager.Occupancy.ANY);
        return (int) count;
    }

    public int countBotsIn100x100(BlockPos center) {
        int count = 0;
        for (AiBotPlayer b : BotManager.getActiveBots()) {
            if (b.level() == this.level() && Math.abs(b.getBlockX() - center.getX()) <= 50 && Math.abs(b.getBlockZ() - center.getZ()) <= 50) {
                count++;
            }
        }
        return count;
    }

    public int countVillagersIn50Radius(BlockPos center) {
        int count = 0;
        for (AiBotPlayer b : BotManager.getActiveBots()) {
            if (b.level() == this.level() && b.getBotRole().toLowerCase(Locale.ROOT).contains("villager")
                    && !b.isBaby() && b.distanceToSqr(Vec3.atCenterOf(center)) <= (50.0 * 50.0)) {
                count++;
            }
        }
        return count;
    }

    public @Nullable BlockPos findVacantBedIn100x100(BlockPos center) {
        if (!(this.level() instanceof ServerLevel sl)) return null;
        PoiManager poi = sl.getPoiManager();
        Set<BlockPos> claimedBeds = new HashSet<>();
        for (AiBotPlayer bot : BotManager.getActiveBots()) {
            if (bot.getAssignedBedPos() != null) {
                claimedBeds.add(bot.getAssignedBedPos());
            }
        }
        return poi.findAll(holder -> holder.is(PoiTypes.HOME), pos -> !claimedBeds.contains(pos), center, 50, PoiManager.Occupancy.ANY)
                .findFirst().orElse(null);
    }

    public synchronized boolean checkAndExecuteBreeding(@Nullable AiBotPlayer partner) {
        if (!(this.level() instanceof ServerLevel sl)) return false;
        // Must have an adult partner nearby to breed - bots cannot breed by themselves!
        if (partner == null || partner == this || this.isBaby || partner.isBaby()) {
            return false;
        }

        // Must be in proximity to partner
        if (this.distanceToSqr(partner) > (16.0 * 16.0)) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastBreedingTime < 300_000L) {
            return false;
        }
        if (now - partner.getLastBreedingTime() < 300_000L) {
            return false;
        }

        // Both villagers must have hunger at 90% or better (>= 18 / 20)
        if (this.getFoodData().getFoodLevel() < 18 || partner.getFoodData().getFoodLevel() < 18) {
            return false;
        }

        // Minimum of 2 adult villagers in 50-block radius
        int villagersIn50 = countVillagersIn50Radius(this.blockPosition());
        if (villagersIn50 < 2) {
            return false;
        }

        // Number of beds = number of villagers + 1 in 50-block range
        int totalBeds = countBedsIn100x100(this.blockPosition());
        int botCount = countBotsIn100x100(this.blockPosition());
        if (totalBeds < (botCount + 1)) {
            return false;
        }

        BlockPos vacantBed = findVacantBedIn100x100(this.blockPosition());
        if (vacantBed == null) {
            return false;
        }

        // Both villagers must have breeding food (wheat, bread, carrots, potatoes, or beetroots)
        boolean thisHasWheat = countMatching(s -> s.is(Items.WHEAT)) >= 3;
        boolean thisHasBread = countMatching(s -> s.is(Items.BREAD)) >= 3;
        boolean thisHasCarrots = countMatching(s -> s.is(Items.CARROT)) >= 12;
        boolean thisHasPotatoes = countMatching(s -> s.is(Items.POTATO)) >= 12;
        boolean thisHasBeetroots = countMatching(s -> s.is(Items.BEETROOT)) >= 12;
        boolean thisHasFood = thisHasWheat || thisHasBread || thisHasCarrots || thisHasPotatoes || thisHasBeetroots;

        boolean partnerHasWheat = partner.countMatching(s -> s.is(Items.WHEAT)) >= 3;
        boolean partnerHasBread = partner.countMatching(s -> s.is(Items.BREAD)) >= 3;
        boolean partnerHasCarrots = partner.countMatching(s -> s.is(Items.CARROT)) >= 12;
        boolean partnerHasPotatoes = partner.countMatching(s -> s.is(Items.POTATO)) >= 12;
        boolean partnerHasBeetroots = partner.countMatching(s -> s.is(Items.BEETROOT)) >= 12;
        boolean partnerHasFood = partnerHasWheat || partnerHasBread || partnerHasCarrots || partnerHasPotatoes || partnerHasBeetroots;

        if (!thisHasFood || !partnerHasFood) {
            return false;
        }

        if (findNearestHostileThreatForVillager(16.0) != null || partner.findNearestHostileThreatForVillager(16.0) != null) {
            return false;
        }
        if (this.getHealth() < 15.0f || this.isOnFire() || partner.getHealth() < 15.0f || partner.isOnFire()) {
            return false;
        }

        // Consume food from this bot
        if (thisHasWheat) consumeMatching(s -> s.is(Items.WHEAT), 3);
        else if (thisHasBread) consumeMatching(s -> s.is(Items.BREAD), 3);
        else if (thisHasCarrots) consumeMatching(s -> s.is(Items.CARROT), 12);
        else if (thisHasPotatoes) consumeMatching(s -> s.is(Items.POTATO), 12);
        else consumeMatching(s -> s.is(Items.BEETROOT), 12);

        // Consume food from partner bot
        if (partnerHasWheat) partner.consumeMatching(s -> s.is(Items.WHEAT), 3);
        else if (partnerHasBread) partner.consumeMatching(s -> s.is(Items.BREAD), 3);
        else if (partnerHasCarrots) partner.consumeMatching(s -> s.is(Items.CARROT), 12);
        else if (partnerHasPotatoes) partner.consumeMatching(s -> s.is(Items.POTATO), 12);
        else partner.consumeMatching(s -> s.is(Items.BEETROOT), 12);

        sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.2, this.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
        sl.sendParticles(ParticleTypes.HEART, partner.getX(), partner.getY() + 1.2, partner.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
        partner.setLastBreedingTime(now);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0f, 1.0f);

        String babyName = pickRandomAvailableBotName();
        double spawnX = (this.getX() + partner.getX()) * 0.5;
        double spawnY = Math.max(this.getY(), partner.getY());
        double spawnZ = (this.getZ() + partner.getZ()) * 0.5;
        AiBotPlayer baby = BotManager.spawnBot(sl, babyName, "villager", spawnX, spawnY, spawnZ, this.getYRot(), 0.0f);
        baby.setBaby(true);
        baby.setAssignedBedPos(vacantBed);
        baby.setAssignedHousePos(vacantBed);
        VillageManager.registerHouse("Compound", vacantBed, vacantBed, babyName);

        this.lastBreedingTime = now;
        sayInChat("A new baby companion was born into our colony! Welcoming baby " + baby.getPlainTextName() + " to the village (Parents: " + this.getPlainTextName() + " & " + partner.getPlainTextName() + ")!");
        BotPersistence.saveAll(sl.getServer());
        return true;
    }

    public synchronized void applyPlayerReinforcement(float reward, @Nullable String reason) {
        String dim = this.level().dimension().identifier().toString();
        String task = getCurrentActionTypeForLearning();
        int updatedSteps = com.aicompanion.bot.learning.HiveMindBrain.applyReinforcementSignal(this, reward, true);

        if (reward > 0) {
            com.aicompanion.bot.learning.HiveMindBrain.recordNodeTraversal(dim, this.blockPosition(), +15.0f, false, false, false, null);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.15f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0f, 1.0f);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.1, this.getZ(), 14, 0.4, 0.4, 0.4, 0.05);
            }
            int iq = com.aicompanion.bot.learning.HiveMindBrain.getHiveIqScore();
            sayInChat(String.format("Thank you! Shared Hive-Mind reinforced (%+.1f across %d neural steps): locked in my recent route and [%s] task policy! [Hive IQ: %d]",
                    reward, Math.max(1, updatedSteps), task, iq));
        } else if (reward < 0) {
            BlockPos frontObstacle = this.blockPosition().relative(this.getDirection());
            com.aicompanion.bot.learning.HiveMindBrain.recordNodeTraversal(dim, this.blockPosition(), -15.0f, true, false, false, null);
            com.aicompanion.bot.learning.HiveMindBrain.recordNodeTraversal(dim, frontObstacle, -18.0f, true, false, false, null);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0f, 0.9f);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 8, 0.35, 0.35, 0.35, 0.02);
            }
            // Immediately recalculate path or detour away from penalized spot
            if (this.lastMoveTarget != null) {
                analyzeAndExecuteUnstuckPlan(this.lastMoveTarget);
            }
            int iq = com.aicompanion.bot.learning.HiveMindBrain.getHiveIqScore();
            sayInChat(String.format("Understood! Shared Hive-Mind penalized (%+.1f across %d neural steps): marked (%d, %d, %d) & [%s] as a bad route/action and adjusted weights! [Hive IQ: %d]",
                    reward, Math.max(1, updatedSteps), this.getBlockX(), this.getBlockY(), this.getBlockZ(), task, iq));
        }

        if (this.level().getServer() != null) {
            BotPersistence.saveAll(this.level().getServer());
        }
    }

    public synchronized boolean handleReinforcementChatCommand(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String clean = stripBotReference(text).toLowerCase(Locale.ROOT).replaceAll("[,!?:;.]", " ").replaceAll("\\s+", " ").trim();

        // 1. Check for Player Path Demonstration triggers
        if (speaker != null) {
            if (clean.contains("learn this path") || clean.contains("watch how i walk") || clean.contains("record my path")
                    || clean.contains("follow my footsteps") || clean.contains("trace my path") || clean.equals("learn path")) {
                com.aicompanion.bot.learning.HiveMindBrain.startPlayerPathRecording(speaker);
                this.setActionPlan(List.of(new BotAction("FOLLOW", speaker.getPlainTextName(), "", 1)));
                sayInChat("Hive-Mind Path Learning ENABLED! Walk your route (stairs, doors, jumps) and I will record your golden breadcrumbs! Say 'stop learning path' or run /bot learnpath stop when done.");
                return true;
            }
            if (clean.contains("stop learning path") || clean.contains("done showing path") || clean.contains("finish path")
                    || clean.contains("stop recording path")) {
                int recorded = com.aicompanion.bot.learning.HiveMindBrain.stopPlayerPathRecording(speaker);
                sayInChat("Hive-Mind Path Learning COMPLETE! Saved " + recorded + " golden breadcrumb waypoints. All bots will now use this route!");
                return true;
            }
        }

        // 2. Check for Positive Reinforcement ("good job", "good bot", "well done", "nice pathing", etc.)
        boolean isPositive = clean.equals("good job") || clean.equals("good bot") || clean.equals("great job")
                || clean.equals("well done") || clean.equals("nice job") || clean.equals("good work")
                || clean.equals("smart bot") || clean.equals("good boy") || clean.equals("good girl")
                || clean.equals("attaboy") || clean.startsWith("good job ") || clean.startsWith("good bot ")
                || clean.startsWith("great job ") || clean.startsWith("well done ") || clean.startsWith("nice job ")
                || clean.contains("good pathing") || clean.contains("nice pathing") || clean.contains("good route")
                || clean.contains("you did a good job") || clean.contains("you did good") || clean.contains("that was good");

        if (isPositive && !clean.contains("not ") && !clean.contains("bad")) {
            applyPlayerReinforcement(+15.0f, text);
            return true;
        }

        // 3. Check for Negative Reinforcement / Punishment ("bad job", "bad bot", "wrong way", "stop jumping", "use the door", etc.)
        boolean isStopJumpingOrDoor = clean.contains("stop jumping") || clean.contains("quit jumping")
                || clean.contains("dont jump") || clean.contains("don't jump") || clean.contains("stop hopping")
                || clean.contains("why are you jumping") || clean.contains("use the door")
                || clean.contains("go through the door") || clean.contains("stop pushing");

        if (isStopJumpingOrDoor) {
            this.setJumping(false);
            this.invalidatePath();
            applyPlayerReinforcement(-15.0f, text);
            if (clean.contains("stop jumping") || clean.contains("quit jumping") || clean.contains("stop hopping")) {
                this.stopMovement();
                this.standingBy = true;
                sayInChat("Stopped jumping and halted movement! Marked this wall/corner as an obstacle in the Hive-Mind. Tell me 'resume' when you want me to continue.");
            }
            return true;
        }

        boolean isNegative = clean.equals("bad job") || clean.equals("bad bot") || clean.equals("wrong way")
                || clean.equals("bad path") || clean.equals("bad route") || clean.equals("terrible job")
                || clean.equals("bad work") || clean.startsWith("bad job ") || clean.startsWith("bad bot ")
                || clean.startsWith("wrong way") || clean.contains("bad pathing") || clean.contains("wrong path")
                || clean.contains("dont go that way") || clean.contains("don't go that way")
                || clean.contains("stop getting stuck") || clean.contains("you did a bad job")
                || clean.contains("wrong chest");

        if (isNegative) {
            this.setJumping(false);
            this.invalidatePath();
            applyPlayerReinforcement(-15.0f, text);
            return true;
        }

        return false;
    }

    public synchronized boolean handleHousingChatCommand(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();

        boolean mentionsBed = lower.contains("bed");
        boolean mentionsHouse = lower.contains("house") || lower.contains("home") || lower.contains("shelter");
        boolean mentionsAssign = lower.contains("assign") || lower.contains("set") || lower.contains("this is")
                || lower.contains("claim") || lower.contains("your bed") || lower.contains("your house") || lower.contains("your home");

        if (mentionsBed && mentionsAssign) {
            BlockPos targetBed = null;
            if (speaker != null) {
                HitResult hit = speaker.pick(5.0D, 0.0F, false);
                if (hit instanceof BlockHitResult bhr) {
                    BlockPos hitPos = bhr.getBlockPos();
                    if (this.level().getBlockState(hitPos).getBlock() instanceof BedBlock) {
                        targetBed = hitPos;
                    }
                }
                if (targetBed == null) {
                    targetBed = findNearestBedBlockNear(speaker.blockPosition(), 6);
                }
            }
            if (targetBed == null) {
                targetBed = findNearestBedBlock(6);
            }

            if (targetBed != null) {
                this.assignedBedPos = targetBed;
                VillageManager.registerHouse("Compound", targetBed, targetBed, this.botName);
                sayInChat("Understood! Assigned bed at (" + targetBed.getX() + ", " + targetBed.getY() + ", " + targetBed.getZ() + ") as my personal bed. I will return here to sleep every night!");
                return true;
            } else {
                sayInChat("I couldn't find a bed nearby! Look at a bed or stand next to one and tell me again.");
                return true;
            }
        }

        if (mentionsHouse && mentionsAssign) {
            com.aicompanion.bot.area.SavedArea foundArea = null;
            for (com.aicompanion.bot.area.SavedArea a : BotPersistence.getAllAreas().values()) {
                if (lower.contains(a.name.toLowerCase(Locale.ROOT))) {
                    foundArea = a;
                    break;
                }
            }

            if (foundArea != null) {
                this.assignedHouseAreaName = foundArea.name;
                BlockPos areaCenter = new BlockPos((foundArea.getMinX() + foundArea.getMaxX()) / 2, (foundArea.minY + foundArea.maxY) / 2, (foundArea.getMinZ() + foundArea.getMaxZ()) / 2);
                this.assignedHousePos = areaCenter;
                int maxRadius = Math.max(foundArea.getMaxX() - foundArea.getMinX(), foundArea.getMaxZ() - foundArea.getMinZ()) / 2 + 2;
                BlockPos bedInArea = findNearestBedBlockNear(this.assignedHousePos, maxRadius);
                if (bedInArea != null) {
                    this.assignedBedPos = bedInArea;
                }
                sayInChat("Understood! Assigned saved area '" + foundArea.name + "' as my home house. I will seek shelter here!");
                return true;
            } else {
                BlockPos housePos = speaker != null ? speaker.blockPosition() : this.blockPosition();
                this.assignedHousePos = housePos;
                BlockPos bedNear = findNearestBedBlockNear(housePos, 8);
                if (bedNear != null) {
                    this.assignedBedPos = bedNear;
                }
                sayInChat("Understood! Assigned (" + housePos.getX() + ", " + housePos.getY() + ", " + housePos.getZ() + ") as my home house! I will seek shelter here at night.");
                return true;
            }
        }

        if (lower.contains("go to bed") || lower.contains("go to sleep") || lower.equals("sleep") || lower.contains("sleep now")) {
            if (isNightTime() || this.level().isThundering()) {
                sayInChat("Heading to bed now! Goodnight!");
                tickNightShelterAndSleep();
            } else {
                sayInChat("It's daytime right now! I'll head to bed when night falls.");
            }
            return true;
        }

        if (lower.contains("wake up") || lower.contains("get out of bed") || lower.contains("rise and shine")) {
            if (this.isSleeping()) {
                this.stopSleepInBed(false, true);
                sayInChat("I'm awake! Ready for orders.");
            } else {
                sayInChat("I'm already awake!");
            }
            return true;
        }

        if (lower.contains("night watch") || lower.contains("stay awake") || lower.contains("don't sleep") || lower.contains("dont sleep") || lower.contains("no sleep")) {
            this.nightShelterEnabled = false;
            sayInChat("Understood! I will stay awake on night watch and won't seek shelter or bed.");
            return true;
        }

        if (lower.contains("seek shelter") || lower.contains("sleep at night") || lower.contains("use bed")) {
            this.nightShelterEnabled = true;
            sayInChat("Understood! I will seek shelter and sleep in my bed when night falls.");
            return true;
        }

        if (lower.contains("grow up") || lower.contains("become adult") || lower.contains("turn into an adult")) {
            if (this.isBaby) {
                this.growUpToAdult();
            } else {
                sayInChat("I'm already a full-grown adult!");
            }
            return true;
        }

        if (lower.contains("make baby") || lower.contains("turn into baby") || lower.contains("become a baby")) {
            this.setBaby(true);
            sayInChat("Goo goo gaga! I am now a baby companion!");
            return true;
        }

        return false;
    }

    public synchronized boolean handleRoleChatCommand(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();

        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(?:you\\s+are\\s+now\\s+(?:a|an)?|change\\s+role\\s+to|switch\\s+role\\s+to|set\\s+role\\s+(?:to\\s+)?|become\\s+(?:a|an)?|be\\s+(?:a|an)?|role[:]?)\\s+([a-zA-Z0-9_ -]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(lower);

        if (m.find()) {
            String roleRaw = m.group(1).trim();
            roleRaw = roleRaw.replaceAll("\\b(?:bot|please|now|here|thank\\s+you)\\b", "").trim();
            if (roleRaw.isBlank()) return false;

            String newRole = roleRaw;
            if (newRole.contains("villager")) newRole = "villager";
            else if (newRole.contains("farmer") || newRole.contains("farm")) newRole = "farmer";
            else if (newRole.contains("guard") || newRole.contains("bodyguard") || newRole.contains("warrior")) newRole = "guard";
            else if (newRole.contains("miner") || newRole.contains("mine")) newRole = "miner";
            else if (newRole.contains("lumber") || newRole.contains("wood")) newRole = "lumberjack";
            else if (newRole.contains("builder") || newRole.contains("build")) newRole = "builder";
            else if (newRole.contains("ranch") || newRole.contains("husbandry") || newRole.contains("breed")) newRole = "husbandry";
            else if (newRole.contains("conductor") || newRole.contains("train")) newRole = "conductor";
            else if (newRole.contains("courier") || newRole.contains("helper")) newRole = "courier";

            this.setBotRole(newRole);
            this.applyRoleStarterKit();

            String skin = com.aicompanion.bot.skin.BotSkinManager.determineSkinForBot(this.botName, newRole, this.aliases, this.getUUID());
            if (skin != null && !skin.isBlank() && !skin.equals(this.customSkinName)) {
                this.setCustomSkinName(skin);
                net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(new com.aicompanion.bot.skin.SyncBotSkinPayload(this.getUUID(), skin));
            }

            sayInChat("Understood! My role has been changed to '" + newRole + "'. I will now focus on my " + newRole + " duties!");
            return true;
        }
        return false;
    }

    public synchronized boolean handleVillagerChatCommand(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();

        if (lower.contains("find work table") || lower.contains("search for work table") || lower.contains("find a work table")
                || lower.contains("find workstation") || lower.contains("search for workstation") || lower.contains("find a job")
                || lower.contains("get a job") || lower.contains("look for work") || lower.contains("search work table")) {
            if (this.isBaby()) {
                sayInChat("I'm still just a baby villager! I can't take on a profession until I grow up.");
                return true;
            }
            if (searchAndClaimWorkstation(48)) {
                return true;
            } else {
                sayInChat("I searched within 48 blocks but could not find any unclaimed work tables. I will remain unemployed until a work table is placed!");
                return true;
            }
        }

        if (lower.contains("claim this table") || lower.contains("claim this workstation") || lower.contains("work at this table")
                || lower.contains("assign workstation") || lower.contains("this is your workstation") || lower.contains("this is your work table")) {
            if (this.isBaby()) {
                sayInChat("I'm still just a baby villager! I can't take on a profession until I grow up.");
                return true;
            }
            BlockPos targetPos = null;
            if (speaker != null) {
                HitResult hr = speaker.level().clip(new ClipContext(speaker.getEyePosition(), speaker.getEyePosition().add(speaker.getViewVector(1.0F).scale(6.0)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, speaker));
                if (hr.getType() == HitResult.Type.BLOCK && hr instanceof BlockHitResult bhr) {
                    targetPos = bhr.getBlockPos();
                }
            }
            if (targetPos == null) {
                targetPos = this.blockPosition();
            }
            Block b = this.level().getBlockState(targetPos).getBlock();
            String trait = WORKSTATION_PROFESSIONS.get(b);
            if (trait != null) {
                this.setBotRole("villager");
                this.assignedWorkstationPos = targetPos;
                this.botSubRole = trait;
                net.minecraft.sounds.SoundEvent snd = PROFESSION_WORK_SOUNDS.getOrDefault(trait, SoundEvents.VILLAGER_YES);
                this.level().playSound(null, targetPos, snd, SoundSource.BLOCKS, 1.0f, 1.0f);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5, targetPos.getY() + 1.2, targetPos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
                }
                sayInChat("Understood! I claimed the " + b.getName().getString() + " at (" + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ() + ") and took on the '" + capitalize(trait) + "' profession trait under my Villager role!");
                return true;
            } else {
                sayInChat("The block at (" + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ() + ") is a " + b.getName().getString() + ", which is not a recognized villager work table.");
                return true;
            }
        }

        if (lower.contains("clear workstation") || lower.contains("quit job") || lower.contains("become unemployed") || lower.contains("leave workstation")) {
            this.assignedWorkstationPos = null;
            this.botSubRole = "unemployed";
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0f, 1.0f);
            sayInChat("I have released my workstation and am now an unemployed villager.");
            return true;
        }

        return false;
    }

    private static class BotNetHandler extends ServerGamePacketListenerImpl {
        private static final net.minecraft.network.Connection DUMMY_CONNECTION = new BotDummyConnection();

        public BotNetHandler(MinecraftServer server, ServerPlayer player) {
            super(server, DUMMY_CONNECTION, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }

        @Override public void tick() {}
        @Override public void resetPosition() {}
        @Override public void disconnect(Component message) {}
        @Override public void onDisconnect(DisconnectionDetails details) {}
        @Override public void send(Packet<?> packet) {}
        @Override public void send(Packet<?> packet, @Nullable ChannelFutureListener sendListener) {}
        @Override public void teleport(double x, double y, double z, float yaw, float pitch) {}
        @Override public void teleport(PositionMoveRotation posMoveRot, Set<Relative> relatives) {}
        @Override public void ackBlockChangesUpTo(int sequence) {}
        @Override public void sendPlayerChatMessage(PlayerChatMessage message, ChatType.Bound boundChatType) {}
        @Override public void sendDisguisedChatMessage(Component content, ChatType.Bound boundChatType) {}
        @Override public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket packet) {}
        @Override public boolean hasChannel(Identifier payloadId) { return false; }
        @Override public void handlePlayerInput(ServerboundPlayerInputPacket packet) {}
        @Override public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) {}
        @Override public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {}
        @Override public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) {}
        @Override public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) {}
        @Override public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) {}
        @Override public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) {}
        @Override public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) {}
        @Override public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) {}
        @Override public void handleRenameItem(ServerboundRenameItemPacket packet) {}
        @Override public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) {}
        @Override public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) {}
        @Override public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) {}
        @Override public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) {}
        @Override public void handleSelectTrade(ServerboundSelectTradePacket packet) {}
        @Override public void handleEditBook(ServerboundEditBookPacket packet) {}
        @Override public void handleEntityTagQuery(ServerboundEntityTagQueryPacket packet) {}
        @Override public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQueryPacket packet) {}
        @Override public void handleMovePlayer(ServerboundMovePlayerPacket packet) {}
        @Override public void handlePlayerAction(ServerboundPlayerActionPacket packet) {}
        @Override public void handleUseItemOn(ServerboundUseItemOnPacket packet) {}
        @Override public void handleUseItem(ServerboundUseItemPacket packet) {}
        @Override public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) {}
        @Override public void handleResourcePackResponse(ServerboundResourcePackPacket packet) {}
        @Override public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) {}
        @Override public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) {}
        @Override public void handleChat(ServerboundChatPacket packet) {}
        @Override public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) {}
        @Override public void handleInteract(ServerboundInteractPacket packet) {}
        @Override public void handleClientCommand(ServerboundClientCommandPacket packet) {}
        @Override public void handleContainerClose(ServerboundContainerClosePacket packet) {}
        @Override public void handleContainerClick(ServerboundContainerClickPacket packet) {}
        @Override public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) {}
        @Override public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) {}
        @Override public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {}
        @Override public void handleSignUpdate(ServerboundSignUpdatePacket packet) {}
        @Override public void handleKeepAlive(ServerboundKeepAlivePacket packet) {}
        @Override public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {}
        @Override public void handleClientInformation(ServerboundClientInformationPacket packet) {}
        @Override public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {}
        @Override public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) {}
        @Override public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) {}
        @Override public void handleChatCommand(ServerboundChatCommandPacket packet) {}
    }

    private static final class BotDummyConnection extends net.minecraft.network.Connection {
        public BotDummyConnection() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override
        public void setListenerForServerboundHandshake(PacketListener listener) {
        }
    }

    public static class BotVillagerMerchant implements net.minecraft.world.item.trading.Merchant {
        private final AiBotPlayer bot;
        private @Nullable Player tradingPlayer;
        private net.minecraft.world.item.trading.@Nullable MerchantOffers offers;

        public BotVillagerMerchant(AiBotPlayer bot) {
            this.bot = bot;
        }

        public synchronized void resetOffers() {
            this.offers = null;
        }

        @Override
        public void setTradingPlayer(@Nullable Player player) {
            this.tradingPlayer = player;
        }

        @Override
        public @Nullable Player getTradingPlayer() {
            return this.tradingPlayer;
        }

        @Override
        public synchronized net.minecraft.world.item.trading.MerchantOffers getOffers() {
            ensureOffersInitialized();
            return this.offers;
        }

        @Override
        public synchronized void overrideOffers(net.minecraft.world.item.trading.MerchantOffers offers) {
            this.offers = offers;
        }

        @Override
        public synchronized void notifyTrade(net.minecraft.world.item.trading.MerchantOffer offer) {
            offer.increaseUses();
            int xpEarned = offer.getXp();
            if (xpEarned <= 0) xpEarned = 2;
            int currentXp = bot.getVillagerXp() + xpEarned;
            bot.setVillagerXp(currentXp);

            boolean leveledUp = false;
            while (bot.getVillagerLevel() < 5 && bot.getVillagerXp() >= getXpThresholdForLevel(bot.getVillagerLevel() + 1)) {
                int nextLvl = bot.getVillagerLevel() + 1;
                bot.villagerLevel = nextLvl;
                populateOffersForLevel(nextLvl);
                leveledUp = true;
            }

            if (leveledUp) {
                bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                        net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                        net.minecraft.sounds.SoundEvents.VILLAGER_YES,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                if (bot.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            bot.getX(), bot.getY() + 1.2, bot.getZ(), 16, 0.4, 0.4, 0.4, 0.05);
                }
                bot.sayInChat("§aHmm! I leveled up to §6Level " + bot.getVillagerLevel() + " (" + bot.getVillagerLevelName() + ")§a! New trades unlocked!");
                if (this.tradingPlayer instanceof ServerPlayer sp && sp.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu mm) {
                    sp.sendMerchantOffers(mm.containerId, this.offers, bot.getVillagerLevel(), bot.getVillagerXp(), true, true);
                }
            } else {
                bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(),
                        net.minecraft.sounds.SoundEvents.VILLAGER_YES,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                if (bot.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            bot.getX(), bot.getY() + 0.8, bot.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
                }
            }

            if (bot.level().getServer() != null) {
                BotPersistence.saveAll(bot.level().getServer());
            }
        }

        @Override
        public void notifyTradeUpdated(net.minecraft.world.item.ItemStack stack) {
        }

        @Override
        public int getVillagerXp() {
            return bot.getVillagerXp();
        }

        @Override
        public void overrideXp(int xp) {
            bot.setVillagerXp(xp);
        }

        @Override
        public boolean showProgressBar() {
            return true;
        }

        @Override
        public net.minecraft.sounds.SoundEvent getNotifyTradeSound() {
            return net.minecraft.sounds.SoundEvents.VILLAGER_YES;
        }

        @Override
        public boolean isClientSide() {
            return false;
        }

        @Override
        public boolean stillValid(Player player) {
            return player.isAlive() && bot.isAlive() && bot.distanceToSqr(player) <= 64.0;
        }

        @Override
        public boolean canRestock() {
            return true;
        }

        public static int getXpThresholdForLevel(int level) {
            return switch (level) {
                case 2 -> 10;
                case 3 -> 70;
                case 4 -> 150;
                case 5 -> 250;
                default -> 0;
            };
        }

        public synchronized void ensureOffersInitialized() {
            if (this.offers == null) {
                this.offers = new net.minecraft.world.item.trading.MerchantOffers();
                int maxLvl = bot.getVillagerLevel();
                for (int l = 1; l <= maxLvl; l++) {
                    populateOffersForLevel(l);
                }
            }
        }

        private static net.minecraft.world.item.Item getItem(String name) {
            return BuiltInRegistries.ITEM.getValue(Identifier.parse(name));
        }

        private static net.minecraft.world.item.trading.MerchantOffer trade(net.minecraft.world.item.Item costItem, int costCount, net.minecraft.world.item.Item resultItem, int resultCount, int maxUses, int xp) {
            return new net.minecraft.world.item.trading.MerchantOffer(
                    new net.minecraft.world.item.trading.ItemCost(costItem, costCount),
                    new net.minecraft.world.item.ItemStack(resultItem, resultCount),
                    maxUses,
                    xp,
                    0.05f
            );
        }

        private static net.minecraft.world.item.trading.MerchantOffer tradeDual(net.minecraft.world.item.Item costA, int countA, net.minecraft.world.item.Item costB, int countB, net.minecraft.world.item.Item result, int resultCount, int maxUses, int xp) {
            return new net.minecraft.world.item.trading.MerchantOffer(
                    new net.minecraft.world.item.trading.ItemCost(costA, countA),
                    java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(costB, countB)),
                    new net.minecraft.world.item.ItemStack(result, resultCount),
                    maxUses,
                    xp,
                    0.05f
            );
        }

        private void populateOffersForLevel(int level) {
            if (this.offers == null) {
                this.offers = new net.minecraft.world.item.trading.MerchantOffers();
            }
            String prof = bot.getBotSubRole().toLowerCase(Locale.ROOT);
            switch (prof) {
                case "farmer" -> addFarmerTrades(level);
                case "librarian" -> addLibrarianTrades(level);
                case "armorer" -> addArmorerTrades(level);
                case "weaponsmith" -> addWeaponsmithTrades(level);
                case "toolsmith" -> addToolsmithTrades(level);
                case "cleric" -> addClericTrades(level);
                case "fletcher" -> addFletcherTrades(level);
                case "butcher" -> addButcherTrades(level);
                case "fisherman" -> addFishermanTrades(level);
                case "shepherd" -> addShepherdTrades(level);
                case "mason", "stone_mason", "stonemason" -> addMasonTrades(level);
                case "cartographer" -> addCartographerTrades(level);
                case "leatherworker" -> addLeatherworkerTrades(level);
                default -> addUnemployedTrades(level);
            }
        }

        private void addFarmerTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.WHEAT, 20, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.POTATO, 26, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.CARROT, 22, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.BREAD, 6, 16, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.PUMPKIN, 6, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.PUMPKIN_PIE, 4, 12, 5));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.APPLE, 4, 16, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.MELON, 4, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 3, net.minecraft.world.item.Items.COOKIE, 18, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("cake"), 1, 12, 15));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.SUSPICIOUS_STEW, 2, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 3, net.minecraft.world.item.Items.GOLDEN_CARROT, 3, 12, 30));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 4, net.minecraft.world.item.Items.GLISTERING_MELON_SLICE, 3, 12, 30));
                }
            }
        }

        private void addLibrarianTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.PAPER, 24, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("bookshelf"), 3, 12, 1));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.BOOK, 4, 12, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.BOOK, 4, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("lantern"), 1, 12, 5));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 5, net.minecraft.world.item.Items.CLOCK, 1, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("glass"), 4, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 5, net.minecraft.world.item.Items.COMPASS, 1, 12, 20));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 5, net.minecraft.world.item.Items.NAME_TAG, 1, 12, 15));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.LEAD, 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 20, net.minecraft.world.item.Items.DIAMOND, 1, 12, 30));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 3, net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, 2, 12, 30));
                }
            }
        }

        private void addArmorerTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.COAL, 15, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 5, net.minecraft.world.item.Items.IRON_HELMET, 1, 12, 1));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 9, net.minecraft.world.item.Items.IRON_CHESTPLATE, 1, 12, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.IRON_INGOT, 4, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 36, getItem("bell"), 1, 12, 5));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 4, net.minecraft.world.item.Items.IRON_BOOTS, 1, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.LAVA_BUCKET, 1, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.SHIELD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 7, net.minecraft.world.item.Items.IRON_LEGGINGS, 1, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.DIAMOND, 1, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 14, net.minecraft.world.item.Items.DIAMOND_LEGGINGS, 1, 12, 15));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 10, net.minecraft.world.item.Items.DIAMOND_BOOTS, 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 12, net.minecraft.world.item.Items.DIAMOND_HELMET, 1, 12, 30));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 18, net.minecraft.world.item.Items.DIAMOND_CHESTPLATE, 1, 12, 30));
                }
            }
        }

        private void addWeaponsmithTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.COAL, 15, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.IRON_AXE, 1, 12, 1));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 2, net.minecraft.world.item.Items.IRON_SWORD, 1, 12, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.IRON_INGOT, 4, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 36, getItem("bell"), 1, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.FLINT, 24, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 3, net.minecraft.world.item.Items.BOW, 1, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.DIAMOND, 1, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 12, net.minecraft.world.item.Items.DIAMOND_AXE, 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 15, net.minecraft.world.item.Items.DIAMOND_SWORD, 1, 12, 30));
                }
            }
        }

        private void addToolsmithTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.COAL, 15, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.STONE_AXE, 1, 12, 1));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.STONE_PICKAXE, 1, 12, 1));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.STONE_SHOVEL, 1, 12, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.IRON_INGOT, 4, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 36, getItem("bell"), 1, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.FLINT, 30, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 6, net.minecraft.world.item.Items.IRON_PICKAXE, 1, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.DIAMOND, 1, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 12, net.minecraft.world.item.Items.DIAMOND_AXE, 1, 12, 15));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 10, net.minecraft.world.item.Items.DIAMOND_SHOVEL, 1, 12, 15));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 4, net.minecraft.world.item.Items.DIAMOND_HOE, 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 18, net.minecraft.world.item.Items.DIAMOND_PICKAXE, 1, 12, 30));
                }
            }
        }

        private void addClericTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.ROTTEN_FLESH, 32, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.REDSTONE, 4, 16, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.GOLD_INGOT, 15, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.LAPIS_LAZULI, 2, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.RABBIT_FOOT, 9, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 4, getItem("glowstone"), 2, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.GHAST_TEAR, 2, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 5, net.minecraft.world.item.Items.ENDER_PEARL, 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.NETHER_WART, 3, net.minecraft.world.item.Items.EMERALD, 1, 12, 30));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 3, net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, 2, 12, 30));
                }
            }
        }

        private void addFletcherTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.STICK, 32, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.ARROW, 16, 16, 1));
                    this.offers.add(tradeDual(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.GRAVEL, 10, net.minecraft.world.item.Items.FLINT, 10, 16, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.FLINT, 26, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 2, net.minecraft.world.item.Items.BOW, 1, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.STRING, 14, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 3, net.minecraft.world.item.Items.CROSSBOW, 1, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.FEATHER, 24, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 5, getItem("target"), 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 8, net.minecraft.world.item.Items.SPECTRAL_ARROW, 16, 12, 30));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 8, net.minecraft.world.item.Items.CROSSBOW, 1, 12, 30));
                }
            }
        }

        private void addButcherTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.CHICKEN, 14, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.PORKCHOP, 14, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.COOKED_CHICKEN, 5, 16, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.COAL, 15, net.minecraft.world.item.Items.EMERALD, 1, 16, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.COOKED_PORKCHOP, 5, 16, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.BEEF, 10, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.MUTTON, 10, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.RABBIT, 10, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.RABBIT_STEW, 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.SWEET_BERRIES, 10, net.minecraft.world.item.Items.EMERALD, 1, 12, 30));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 2, net.minecraft.world.item.Items.COOKED_BEEF, 6, 12, 30));
                }
            }
        }

        private void addFishermanTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.COD, 20, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(tradeDual(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.COD, 1, net.minecraft.world.item.Items.COOKED_COD, 1, 16, 1));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("campfire"), 1, 16, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.SALMON, 15, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(tradeDual(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.SALMON, 1, net.minecraft.world.item.Items.COOKED_SALMON, 1, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.STRING, 15, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 6, net.minecraft.world.item.Items.FISHING_ROD, 1, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.TROPICAL_FISH, 1, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 3, net.minecraft.world.item.Items.WATER_BUCKET, 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.OAK_BOAT, 1, net.minecraft.world.item.Items.EMERALD, 1, 12, 30));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 8, net.minecraft.world.item.Items.FISHING_ROD, 1, 12, 30));
                }
            }
        }

        private void addShepherdTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(getItem("white_wool"), 18, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 2, net.minecraft.world.item.Items.SHEARS, 1, 16, 1));
                }
                case 2 -> {
                    this.offers.add(trade(getItem("black_wool"), 12, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("white_carpet"), 4, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(getItem("white_wool"), 12, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 3, getItem("white_bed"), 1, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("white_banner"), 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 2, net.minecraft.world.item.Items.PAINTING, 3, 12, 30));
                }
            }
        }

        private void addMasonTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.CLAY_BALL, 10, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("bricks"), 10, 16, 1));
                }
                case 2 -> {
                    this.offers.add(trade(getItem("stone"), 20, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("chiseled_stone_bricks"), 4, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(getItem("granite"), 16, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(getItem("andesite"), 16, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("polished_andesite"), 4, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("terracotta"), 2, 12, 15));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("white_glazed_terracotta"), 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.QUARTZ, 1, net.minecraft.world.item.Items.EMERALD, 1, 12, 30));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("quartz_pillar"), 2, 12, 30));
                }
            }
        }

        private void addCartographerTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.PAPER, 24, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 7, net.minecraft.world.item.Items.MAP, 1, 12, 1));
                }
                case 2 -> {
                    this.offers.add(trade(getItem("glass_pane"), 11, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.MAP, 1, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.COMPASS, 1, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 8, net.minecraft.world.item.Items.COMPASS, 1, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 7, getItem("item_frame"), 1, 12, 15));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 12, net.minecraft.world.item.Items.SPYGLASS, 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 8, getItem("globe_banner_pattern"), 1, 12, 30));
                }
            }
        }

        private void addLeatherworkerTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.LEATHER, 6, net.minecraft.world.item.Items.EMERALD, 1, 16, 2));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 3, net.minecraft.world.item.Items.LEATHER_LEGGINGS, 1, 12, 1));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 7, net.minecraft.world.item.Items.LEATHER_CHESTPLATE, 1, 12, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.FLINT, 26, net.minecraft.world.item.Items.EMERALD, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 5, net.minecraft.world.item.Items.LEATHER_HELMET, 1, 12, 5));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 4, net.minecraft.world.item.Items.LEATHER_BOOTS, 1, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.LEATHER, 4, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 6, net.minecraft.world.item.Items.LEATHER_HORSE_ARMOR, 1, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.TURTLE_SCUTE, 4, net.minecraft.world.item.Items.EMERALD, 1, 12, 20));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 6, net.minecraft.world.item.Items.SADDLE, 1, 12, 30));
                }
            }
        }

        private void addUnemployedTrades(int level) {
            switch (level) {
                case 1 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.BREAD, 6, 16, 1));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.APPLE, 4, 16, 1));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("torch"), 8, 16, 1));
                }
                case 2 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, getItem("oak_sapling"), 4, 12, 5));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 1, net.minecraft.world.item.Items.WHEAT_SEEDS, 4, 12, 5));
                }
                case 3 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 2, net.minecraft.world.item.Items.BUCKET, 1, 12, 10));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 2, net.minecraft.world.item.Items.SHEARS, 1, 12, 10));
                }
                case 4 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 4, net.minecraft.world.item.Items.CLOCK, 1, 12, 15));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 4, net.minecraft.world.item.Items.COMPASS, 1, 12, 15));
                }
                case 5 -> {
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 5, net.minecraft.world.item.Items.LEAD, 1, 12, 30));
                    this.offers.add(trade(net.minecraft.world.item.Items.EMERALD, 6, net.minecraft.world.item.Items.SADDLE, 1, 12, 30));
                }
            }
        }
    }
}
