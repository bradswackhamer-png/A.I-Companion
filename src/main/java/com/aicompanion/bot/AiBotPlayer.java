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
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    // Tractor configuration
    private boolean tractorMode = false;

    // Global area assignment (e.g. "Compound", "NorthFarm")
    private @Nullable String assignedAreaName = null;
    private int holeEscapeTicks = 0;

    public @Nullable String getAssignedAreaName() {
        return this.assignedAreaName;
    }

    public synchronized void setAssignedArea(@Nullable String areaName) {
        this.assignedAreaName = areaName;
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

    // Tactical Combat & Defense
    private int shieldBlockTicks = 0;
    private int lastDistressTick = 0;
    private int currentCombatTargetId = -1;
    private int combatTargetEngageTicks = 0;
    private double bestCombatTargetDistSq = Double.MAX_VALUE;
    private final java.util.Map<Integer, Integer> unreachableMobBlacklist = new java.util.HashMap<>();

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

    public synchronized boolean isAwaitingExcavationReply() {
        return this.awaitingExcavationDimensions && this.pendingExcavationOrigin != null;
    }

    public AiBotPlayer(ServerLevel level, GameProfile profile, String role) {
        super(level.getServer(), level, profile, ClientInformation.createDefault());
        this.botName = profile.name();
        this.botRole = (role == null || role.isBlank()) ? "All-Around Survival Companion" : role;
        this.connection = new BotNetHandler(level.getServer(), this);
    }

    public String getBotName() {
        return this.botName;
    }

    public String getBotRole() {
        return this.botRole;
    }

    public synchronized void setBotRole(String role) {
        if (role != null && !role.isBlank()) {
            this.botRole = role.trim();
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
                "(?:call\\s+you|name\\s+you|rename\\s+you(?:\\s+to)?|rename\\s+yourself(?:\\s+to)?|your\\s+name\\s+is|you\\s+are\\s+now)\\s+([a-zA-Z0-9_-]{2,20})",
                "(?:respond\\s+to|answer\\s+to)\\s+([a-zA-Z0-9_-]{2,20})"
        };
        Set<String> stopWords = Set.of("from", "now", "on", "me", "here", "there", "please", "the", "a", "an", "to", "that", "this");
        boolean learned = false;
        for (String regex : patterns) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
            while (m.find()) {
                String candidate = m.group(1).trim();
                if (!stopWords.contains(candidate.toLowerCase()) && !candidate.equalsIgnoreCase(this.botName)) {
                    addAlias(candidate);
                    learned = true;
                }
            }
        }
        return learned;
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

    public synchronized boolean extractAndLearnFarmConfigFromPrompt(String text, @Nullable ServerPlayer speaker) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        boolean mentionsFarm = lower.contains("farm") || lower.contains("harvest") || lower.contains("crop")
                || lower.contains("plant") || lower.contains("replant") || lower.contains("till");
        if (!mentionsFarm) return false;

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

        // 2. DISMOUNT / GET OUT / LEAVE TRAIN
        if (lower.contains("get out") || lower.contains("dismount") || lower.contains("exit the train") || lower.contains("leave the train")) {
            if (loco != null) {
                TrainCompat.stopTrain(loco, this);
                TrainCompat.dismountLocomotive(this);
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
                        TrainCompat.stopTrain(loco, this);
                        TrainCompat.dismountLocomotive(this);
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
        if (this.farmOriginPos == null) {
            return this.distanceToSqr(Vec3.atCenterOf(p)) <= 400.0;
        }
        int dx = Math.abs(p.getX() - this.farmOriginPos.getX());
        int dy = Math.abs(p.getY() - this.farmOriginPos.getY());
        int dz = Math.abs(p.getZ() - this.farmOriginPos.getZ());
        return dx <= this.farmRadiusX && dz <= this.farmRadiusZ && dy <= 3;
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
        }
    }

    public String getCurrentTaskSummary() {
        if (currentAction == null && actionQueue.isEmpty()) {
            return "Idle (waiting for orders)";
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
                        return;
                    }
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

    private void advanceNextAction() {
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

        if (!this.isAlive()) {
            return;
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

        // Periodic survival maintenance every 20 ticks
        if (this.tickCount % 20 == 0) {
            this.unreachableMobBlacklist.entrySet().removeIf(entry -> this.tickCount >= entry.getValue());
            if (BotConfig.AUTO_EAT.getAsBoolean()) {
                tryAutoEat();
            }
            autoEquipArmor();
            vacuumNearbyDrops();
        }

        // Auto-defend if attacked by a monster
        if (BotConfig.AUTO_DEFEND.getAsBoolean() && this.getLastHurtByMob() != null && this.getLastHurtByMob().isAlive()) {
            LivingEntity attacker = this.getLastHurtByMob();
            if (!(attacker instanceof Player) && this.distanceToSqr(attacker) < 64.0 && !this.unreachableMobBlacklist.containsKey(attacker.getId())) {
                engageTarget(attacker);
                return;
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
            tickPassiveRoleBehavior();
        }
    }

    private void tickPassiveRoleBehavior() {
        String r = this.botRole.toLowerCase();

        // 1. Guard / Bodyguard: Automatically patrol compound / anchor or follow player, intercepting nearby monsters (never jumping fences!)
        if (r.contains("guard") || r.contains("bodyguard") || r.contains("warrior")) {
            BlockPos guardCenter = this.guardAnchorPos != null ? this.guardAnchorPos : this.blockPosition();
            double maxDist = this.guardAnchorPos != null ? this.guardAnchorRadius : 24.0;

            // Return inside if drifted outside anchor boundary
            if (this.guardAnchorPos != null && this.distanceToSqr(Vec3.atCenterOf(this.guardAnchorPos)) > (maxDist * maxDist)) {
                moveTowards(Vec3.atCenterOf(this.guardAnchorPos), true);
                return;
            }

            Monster threat = findNearestMonsterNear(guardCenter, maxDist);
            if (threat != null) {
                // Ensure monster is on our side of the fence and inside our guard perimeter
                boolean threatInside = this.guardAnchorPos == null || threat.distanceToSqr(Vec3.atCenterOf(this.guardAnchorPos)) <= (maxDist * maxDist);
                if (threatInside && !isLineBlockedByFence(this.getEyePosition(), threat.getEyePosition())) {
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
                clearActions();
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

        // Defend guarded player from nearby monsters
        if (BotConfig.AUTO_DEFEND.getAsBoolean()) {
            Monster threat = findNearestMonsterNear(target.blockPosition(), 10.0);
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
        if (distSq <= 4.5) {
            stopMovement();
            sayInChat("Arrived at destination!");
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

        Monster threat = findNearestMonsterNear(guardCenter, 16.0);
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
        LivingEntity targetMob = findNearestTargetMob(query, 24.0);
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
        double distSq = this.distanceToSqr(target);
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

        // Timeout or underground spin check: if standing directly above/below a mob without line of sight, or unable to reach after 80 ticks (4s)
        if ((horizDistSq < 3.5 && vertDiff > 2.5 && !this.hasLineOfSight(target)) || this.combatTargetEngageTicks > 80) {
            this.unreachableMobBlacklist.put(target.getId(), this.tickCount + 200);
            this.currentCombatTargetId = -1;
            this.combatTargetEngageTicks = 0;
            this.setLastHurtByMob(null);
            stopMovement();
            return;
        }

        lookAtPos(target.getEyePosition());

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
        equipBestWeapon();

        if (distSq > 9.5) {
            Vec3 moveGoal = target.position();
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
        if (!this.level().getBlockState(pos).isAir() && !this.level().getBlockState(pos).canBeReplaced()) {
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
        BlockPos chestPos = findNearestContainerBlock(16);
        if (chestPos == null) {
            sayInChat("I couldn't find a chest or barrel nearby.");
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
        BlockEntity be = this.level().getBlockEntity(chestPos);
        if (!(be instanceof Container container)) {
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
        BlockPos center = this.farmOriginPos != null ? this.farmOriginPos : this.blockPosition();
        int rx = this.farmOriginPos != null ? this.farmRadiusX : radius;
        int rz = this.farmOriginPos != null ? this.farmRadiusZ : radius;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
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
        BlockPos center = this.farmOriginPos != null ? this.farmOriginPos : this.blockPosition();
        int rx = this.farmOriginPos != null ? this.farmRadiusX : radius;
        int rz = this.farmOriginPos != null ? this.farmRadiusZ : radius;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
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
        if (this.farmOriginPos == null) return null;
        BlockPos origin = this.farmOriginPos;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -this.farmRadiusX; dx <= this.farmRadiusX; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -this.farmRadiusZ; dz <= this.farmRadiusZ; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
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
        int goal = action.count > 0 ? action.count : 20;
        if (this.actionProgressCount >= goal) {
            stopMovement();
            BlockPos chestPos = findNearestContainerBlock(16);
            if (chestPos != null) {
                tickChestAction(new BotAction("CHEST_STORE", "all", "", 64), true);
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
                BlockPos chestPos = findNearestContainerBlock(20);
                if (chestPos != null && this.level().getBlockEntity(chestPos) instanceof Container c) {
                    int taken = takeItemsFromContainer(c, "seed", 32);
                    if (taken > 0) {
                        sayInChat("Grabbed " + taken + "x seeds from chest to continue planting.");
                        return false;
                    }
                }
            }
        }

        if (this.farmAllowTill && tryTillDirtInFarmBox()) {
            return false;
        }

        stopMovement();
        if (getFarmerDepositableCropCount() > 0) {
            BlockPos chestPos = findNearestContainerBlock(16);
            if (chestPos != null) {
                tickChestAction(new BotAction("CHEST_STORE", "all", "", 64), true);
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
            if (s.is(Items.WHEAT) || s.is(Items.BEETROOT) || s.is(Items.MELON_SLICE) || s.is(Items.PUMPKIN)) {
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
        depositable += Math.max(0, seedCount - 32);
        return depositable;
    }

    private void tickPassiveFarmer() {
        if (this.tickCount % 20 != 0) return;

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
            BlockPos chestPos = findNearestContainerBlock(20);
            if (chestPos != null) {
                double distSq = this.distanceToSqr(Vec3.atCenterOf(chestPos));
                if (distSq > 9.0) {
                    moveTowards(Vec3.atCenterOf(chestPos), false);
                    return;
                } else {
                    stopMovement();
                    tickChestAction(new BotAction("CHEST_STORE", "all", "", 64), true);
                    return;
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
                BlockPos chestPos = findNearestContainerBlock(20);
                if (chestPos != null && this.level().getBlockEntity(chestPos) instanceof Container c) {
                    takeItemsFromContainer(c, "seed", 32);
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

        // 1. Check if currently riding a locomotive
        Entity loco = TrainCompat.getRiddenLocomotive(this);
        if (loco == null) {
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

    public boolean isFenceOrWall(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(net.minecraft.tags.BlockTags.FENCES) || state.is(net.minecraft.tags.BlockTags.WALLS)) return true;
        if (state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock || state.getBlock() instanceof net.minecraft.world.level.block.WallBlock) return true;
        if (state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock && !state.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN)) return true;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase();
        return path.contains("fence") || (path.contains("wall") && !path.contains("sign") && !path.contains("torch"));
    }

    public boolean isLineBlockedByFence(Vec3 from, Vec3 to) {
        double dist = from.distanceTo(to);
        if (dist < 1.0) return false;
        int steps = (int) Math.ceil(dist);
        Vec3 diff = to.subtract(from).normalize();
        for (int i = 1; i < steps; i++) {
            BlockPos p = BlockPos.containing(from.add(diff.scale(i)));
            if (isFenceOrWall(this.level().getBlockState(p))) {
                return true;
            }
        }
        return false;
    }

    private final java.util.Set<BlockPos> learnedHazardPits = new java.util.HashSet<>();

    private boolean isDeepPitAt(BlockPos footCandidate) {
        BlockState footState = this.level().getBlockState(footCandidate);
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
            if (!state.isAir() && !state.canBeReplaced() && !isFenceOrWall(state)) {
                // Hazardous surfaces (lava, magma, fire) are dangerous hazards
                if (state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                    return true;
                }
                // Solid walkable block found within 3-block drop: safe terrain to walk/drop down!
                return false;
            }
            if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE)) {
                return true;
            }
        }

        // 4+ block sheer drop or void: this is a hazardous pit / cliff / ravine!
        return true;
    }

    private boolean isPassable(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private boolean isStandable(BlockPos pos) {
        BlockState belowState = this.level().getBlockState(pos.below());
        boolean hasFloor = !belowState.isAir() && !belowState.canBeReplaced() && !isFenceOrWall(belowState);
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
                            if (!isPassable(p.relative(d).above())) {
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
            if (!hs.isAir() && !hs.canBeReplaced() && !isFenceOrWall(hs)) {
                solidHeadNeighbors++;
            }
            BlockPos wallFeet = feet.relative(d);
            BlockState fs = this.level().getBlockState(wallFeet);
            if (!fs.isAir() && !fs.canBeReplaced() && !isFenceOrWall(fs)) {
                solidFeetNeighbors++;
            }
        }
        return solidHeadNeighbors >= 3 || (solidHeadNeighbors >= 2 && solidFeetNeighbors >= 3);
    }

    private void executeHoleEscape() {
        BlockPos feetPos = this.blockPosition();
        BlockPos aboveHead = feetPos.above(2);

        // 1. Break block directly above head if obstructed
        BlockState aboveHeadState = this.level().getBlockState(aboveHead);
        if (!aboveHeadState.isAir() && aboveHeadState.getDestroySpeed(this.level(), aboveHead) >= 0 && aboveHeadState.getDestroySpeed(this.level(), aboveHead) < 15.0f) {
            equipBestToolFor(aboveHeadState);
            this.level().destroyBlock(aboveHead, true, this);
            broadcastSwing();
        }

        // 2. Pillar jump straight up and place building block beneath feet
        lookAtPos(new Vec3(this.getX(), this.getY() - 1.0, this.getZ()));
        this.setPos(this.getX(), this.getY() + 1.05, this.getZ());
        this.setDeltaMovement(0, 0.42, 0);

        if (!placeBuildingBlockAt(feetPos)) {
            // Survival fallback: place dirt so companion is never permanently trapped
            this.level().setBlockAndUpdate(feetPos, Blocks.DIRT.defaultBlockState());
        }

        this.level().playSound(null, feetPos, net.minecraft.sounds.SoundEvents.STONE_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        this.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, feetPos.getX() + 0.5, feetPos.getY() + 1.2, feetPos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);

        // 3. Clear step in front so we can walk forward onto the next level
        net.minecraft.core.Direction dir = this.getDirection();
        BlockPos frontHead = feetPos.relative(dir).above(2);
        BlockState frontHeadState = this.level().getBlockState(frontHead);
        if (!frontHeadState.isAir() && frontHeadState.getDestroySpeed(this.level(), frontHead) >= 0 && frontHeadState.getDestroySpeed(this.level(), frontHead) < 15.0f) {
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
            if (!hs.isAir() && !hs.canBeReplaced() && !isFenceOrWall(hs)) {
                solidHeadNeighbors++;
            }
            BlockPos wallFeet = feetPos.relative(d);
            BlockState fs = this.level().getBlockState(wallFeet);
            if (!fs.isAir() && !fs.canBeReplaced() && !isFenceOrWall(fs)) {
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

        // 2. ANALYZE FOR WALL / BUILDING / CLIFF OBSTACLE:
        net.minecraft.core.Direction dir = this.getDirection();
        BlockPos frontFeet = feetPos.relative(dir);
        BlockPos frontHead = frontFeet.above();
        BlockState frontFeetState = this.level().getBlockState(frontFeet);
        BlockState frontHeadState = this.level().getBlockState(frontHead);

        boolean isBlockedInFront = (!frontFeetState.isAir() && !frontFeetState.canBeReplaced())
                || (!frontHeadState.isAir() && !frontHeadState.canBeReplaced())
                || isFenceOrWall(frontFeetState);

        if (isBlockedInFront || this.horizontalCollision) {
            net.minecraft.core.Direction leftDir = dir.getCounterClockWise();
            net.minecraft.core.Direction rightDir = dir.getClockWise();

            BlockPos bestBypass = null;
            double bestScore = Double.MAX_VALUE;

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
                    double score = dist * 2.0 + distToGoal;
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
                    double score = dist * 2.0 + distToGoal;
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

            // If lateral search found no way around within 8 blocks:
            // Check if it's a 1-2 block barrier (e.g. fence or low wall) and target is elevated or on the other side
            BlockState headAboveState = this.level().getBlockState(frontHead.above());
            boolean isLowBarrier = headAboveState.isAir() || headAboveState.canBeReplaced();
            if (isLowBarrier && originalGoal.y >= this.getY() - 0.5) {
                executeHoleEscape(); // Pillar over the low barrier
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

        // 4. GENERAL UNSTUCK FALLBACK:
        this.xxa = ((this.tickCount / 10) % 2 == 0) ? 0.9f : -0.9f;
        this.setJumping(true);
        if (this.onGround()) {
            this.jumpFromGround();
        }
    }

    private void moveTowards(Vec3 target, boolean sprint) {
        // Global Area Perimeter Boundary Enforcement
        if (this.assignedAreaName != null && !this.assignedAreaName.isBlank()) {
            com.aicompanion.bot.area.SavedArea area = BotPersistence.getArea(this.assignedAreaName);
            if (area != null) {
                boolean botInside = area.contains(this.getX(), this.getY(), this.getZ());
                boolean targetInside = area.contains(target.x, target.y, target.z);
                if (!botInside || !targetInside) {
                    Vec3 center = area.getCenter();
                    lookAtPos(new Vec3(center.x, center.y + this.getEyeHeight(), center.z));
                    target = center;
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

        lookAtPos(new Vec3(target.x, target.y + this.getEyeHeight(), target.z));
        this.setSprinting(sprint);
        this.zza = sprint ? 1.0f : 0.8f;
        this.xxa = 0.0f;

        net.minecraft.core.Direction dir = this.getDirection();
        BlockPos frontPos = this.blockPosition().relative(dir);
        BlockState frontState = this.level().getBlockState(frontPos);

        // Open wooden doors & fence gates in front of bot
        if (frontState.getBlock() instanceof DoorBlock door && !frontState.getValue(DoorBlock.OPEN)) {
            door.setOpen(this, this.level(), frontState, frontPos, true);
        } else if (frontState.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock && !frontState.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN)) {
            if (this.guardAnchorPos == null) {
                this.level().setBlockAndUpdate(frontPos, frontState.setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, true));
            }
        }

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
        boolean isFenceBarrier = isFenceOrWall(frontState) || isFenceOrWall(headState);
        boolean isTallWall = !frontState.isAir() && !headState.isAir() && !headState.canBeReplaced() && !isFenceBarrier;

        // Jump over 1-block ledges or swim in water/lava (Never jump at 2-block tall solid walls!)
        // Only jump onto an elevated block if target is at or above our elevation, or if physically obstructed
        boolean stepUpPossible = !frontState.isAir() && headState.isAir() && this.level().getBlockState(headFront.above()).isAir() && !isFenceBarrier;
        boolean wantsToAscend = target.y > this.getY() - 0.2;
        boolean shouldJump = ((stepUpPossible && (wantsToAscend || this.horizontalCollision))
                || (this.horizontalCollision && !isTallWall)
                || this.isInWater() || this.isInLava()) && !isFenceBarrier;
        if (!shouldJump && target.y > this.getY() + 0.5 && !frontState.isAir() && !isTallWall && !isFenceBarrier) {
            shouldJump = true;
        }
        this.setJumping(shouldJump);
        if (shouldJump && this.onGround()) {
            this.jumpFromGround();
        }

        if (isFenceBarrier) {
            this.setJumping(false);
            if (this.guardAnchorPos != null) {
                // Anchored inside compound: turn around and steer toward anchor center
                lookAtPos(Vec3.atCenterOf(this.guardAnchorPos));
                this.zza = 0.5f;
                this.xxa = 0.0f;
                return;
            } else {
                // Steer sideways along the fence
                this.xxa = ((this.tickCount / 20) % 2 == 0) ? 0.8f : -0.8f;
                this.zza = 0.2f;
            }
        }

        // Stuck & Area Analysis Engine
        if (!isExcavating && this.moveNoProgressTicks >= 16) {
            analyzeAndExecuteUnstuckPlan(target);
            this.moveNoProgressTicks = 0;
        }
    }

    private void stopMovement() {
        this.zza = 0.0f;
        this.xxa = 0.0f;
        this.setJumping(false);
        this.setSprinting(false);
        this.activeBypassWaypoint = null;
        this.bypassWaypointTicks = 0;
        this.moveNoProgressTicks = 0;
        this.bestMoveTargetDistSq = Double.MAX_VALUE;
        this.lastMoveTarget = null;
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

    private int countMatching(Predicate<ItemStack> predicate) {
        int total = 0;
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && predicate.test(s)) {
                total += s.getCount();
            }
        }
        return total;
    }

    private boolean consumeMatching(Predicate<ItemStack> predicate, int amount) {
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
        if (q.equals("wood") || q.equals("tree") || q.equals("trees") || q.equals("logs") || q.equals("log")) {
            return path.contains("_log") || path.contains("_stem") || path.contains("_wood");
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

    private @Nullable BlockPos findNearestContainerBlock(int radius) {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (this.level().getBlockEntity(pos) instanceof Container) {
                        double d = origin.distSqr(pos);
                        if (d < bestDist) {
                            bestDist = d;
                            best = pos.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean isMobReachableAndValid(LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (this.unreachableMobBlacklist.containsKey(target.getId())) return false;

        double vertDiff = Math.abs(target.getY() - this.getY());
        if (vertDiff > 6.0) return false;

        double hdx = target.getX() - this.getX();
        double hdz = target.getZ() - this.getZ();
        double horizDistSq = hdx * hdx + hdz * hdz;

        // If directly above or below the bot through solid floor/ceiling without line of sight, ignore
        if (horizDistSq < 4.0 && vertDiff > 2.2 && !this.hasLineOfSight(target)) {
            return false;
        }

        // Direct line of sight is always valid
        if (this.hasLineOfSight(target)) {
            return true;
        }

        // If out of line of sight, check if the mob's own vanilla A* PathNavigation can reach us
        if (target instanceof net.minecraft.world.entity.Mob mob) {
            net.minecraft.world.level.pathfinder.Path path = mob.getNavigation().createPath(this.blockPosition(), 1);
            return path != null && path.canReach();
        }

        return false;
    }

    private @Nullable Monster findNearestMonsterNear(BlockPos center, double radius) {
        AABB box = new AABB(center).inflate(radius, 6.0, radius);
        List<Monster> monsters = this.level().getEntitiesOfClass(Monster.class, box, m -> {
            if (!isMobReachableAndValid(m)) return false;
            // Ignore monsters separated by fences/walls
            if (isLineBlockedByFence(this.getEyePosition(), m.getEyePosition())) {
                return false;
            }
            return true;
        });
        monsters.sort(Comparator.comparingDouble(this::distanceToSqr));
        return monsters.isEmpty() ? null : monsters.get(0);
    }

    private @Nullable LivingEntity findNearestTargetMob(String query, double radius) {
        AABB box = this.getBoundingBox().inflate(radius, 6.0, radius);
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == this || e instanceof Player || !isMobReachableAndValid(e)) return false;
            if (this.guardAnchorPos != null && isLineBlockedByFence(this.getEyePosition(), e.getEyePosition())) {
                return false;
            }
            if (query.isEmpty() || "hostile".equals(query) || "monsters".equals(query) || "mobs".equals(query)) {
                return e instanceof Monster;
            }
            String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().toLowerCase();
            return typeId.contains(query);
        });
        list.sort(Comparator.comparingDouble(this::distanceToSqr));
        return list.isEmpty() ? null : list.get(0);
    }

    private @Nullable ItemEntity findNearestDrop(double radius) {
        AABB box = this.getBoundingBox().inflate(radius);
        List<ItemEntity> drops = this.level().getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive);
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
        sayInChat("I died! Respawning near players...");
        this.setHealth(this.getMaxHealth());
        this.getFoodData().setFoodLevel(20);
        Player nearest = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 256.0, p -> p != this && !(p instanceof AiBotPlayer));
        if (nearest != null) {
            this.teleportTo(nearest.getX() + 1.5, nearest.getY(), nearest.getZ() + 1.5);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
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

        // 3. Hole / Pit excavation command: "dig a hole", "dig a hole here", "excavate here", "dig a 5x5 hole 4 deep here"
        boolean mentionsHole = lower.contains("hole") || lower.contains("pit") || lower.contains("trench") || lower.contains("quarry")
                || lower.contains("dig here") || lower.contains("excavate here") || lower.contains("dig right here")
                || (lower.contains("dig") && (lower.contains("deep") || lower.contains("x") || lower.contains("by")));

        if (mentionsHole && !lower.contains("train") && !lower.contains("conductor")) {
            BlockPos startPos = resolveExcavationStartPos(speaker);
            net.minecraft.core.Direction facing = speaker != null ? speaker.getDirection() : this.getDirection();
            spawnExcavationParticles(startPos);

            int[] dims = parseExcavationDimensions(lower, false);
            if (dims != null) {
                this.awaitingExcavationDimensions = false;
                startBoxExcavation(startPos, facing, dims[0], dims[1], dims[2]);
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
        this.clearActions();
        this.activeExcavationQueue.clear();

        net.minecraft.core.Direction right = facing.getClockWise();
        int halfWidth = (width - 1) / 2;

        // Build top-down excavation queue (surface down to pit floor)
        // First clear surface obstruction layer (dy = 1) if solid
        for (int dz = 0; dz < length; dz++) {
            for (int dx = -halfWidth; dx < width - halfWidth; dx++) {
                BlockPos p = origin.relative(facing, dz).relative(right, dx).above(1);
                BlockState s = this.level().getBlockState(p);
                if (!s.isAir() && s.getDestroySpeed(this.level(), p) >= 0) {
                    this.activeExcavationQueue.add(p);
                }
            }
        }

        // Layer-by-layer from surface (dy = 0) down to -(depth - 1)
        for (int dy = 0; dy > -depth; dy--) {
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

        this.currentAction = new BotAction("EXCAVATE", width + "x" + length + " (" + depth + " deep)", null, this.activeExcavationQueue.size());
        this.actionProgressCount = 0;
        sayInChat("Starting systematic excavation of " + width + "x" + length + " hole (" + depth + " deep) from ("
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
}
